package com.guoyu.platform.exam.zhenti.service;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.zhenti.vo.ZhentiImportFileResult;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * zip 解包与逐条导入（方案 §2.4 / §2.5）。
 *
 * <p>安全边界：拒绝目录穿越与绝对路径条目（整包 400）、条目数 &gt;100（413）、
 * 单文件非法/超限计入失败明细但不中断整包。
 */
@Service
@Slf4j
public class ZhentiZipProcessor {

  private static final Charset GBK = Charset.forName("GBK");

  private final ZhentiIngestService ingestService;

  public ZhentiZipProcessor(ZhentiIngestService ingestService) {
    this.ingestService = ingestService;
  }

  /** 先登记条目：数量与安全性在写库前一次性判定。 */
  public ZipInspection inspect(Path zipPath) {
    try (ZipFile zip = open(zipPath)) {
      List<String> names = new ArrayList<>();
      Enumeration<? extends ZipEntry> entries = zip.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        if (entry.isDirectory()) {
          continue;
        }
        assertSafeEntry(entry.getName());
        names.add(entry.getName());
        if (names.size() > ZhentiUploadValidator.MAX_ZIP_ENTRIES) {
          throw new BusinessException(ErrorCode.PAYLOAD_TOO_LARGE,
              "zip 内文件不能超过 " + ZhentiUploadValidator.MAX_ZIP_ENTRIES + " 个");
        }
      }
      if (names.isEmpty()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "zip 内没有可导入的文件");
      }
      return new ZipInspection(names);
    } catch (BusinessException ex) {
      throw ex;
    } catch (IOException | IllegalArgumentException ex) {
      log.warn("[ZhentiImport] zip 解析失败: {}", ex.toString());
      throw new BusinessException(ErrorCode.BAD_REQUEST, "zip 文件无法解析，请检查压缩包");
    }
  }

  /**
   * 逐条导入。
   *
   * @param scope      共享组（null = 计划私有口径），整包统一，方案 v2.1 §3.4
   * @param onFinished 每处理完一个文件回调（用于异步进度落库）
   */
  public List<ZhentiImportFileResult> process(
      Path zipPath,
      Long tenantId,
      Long examId,
      Long operatorId,
      ZhentiScope scope,
      IntConsumer onFinished) {
    List<ZhentiImportFileResult> results = new ArrayList<>();
    try (ZipFile zip = open(zipPath)) {
      Enumeration<? extends ZipEntry> entries = zip.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        if (entry.isDirectory()) {
          continue;
        }
        String displayName = displayName(entry.getName());
        results.add(importOne(zip, entry, displayName, tenantId, examId, operatorId, scope));
        if (onFinished != null) {
          onFinished.accept(results.size());
        }
      }
      return results;
    } catch (BusinessException ex) {
      throw ex;
    } catch (IOException ex) {
      log.warn("[ZhentiImport] zip 读取中断: {}", ex.toString());
      throw new BusinessException(ErrorCode.BAD_REQUEST, "zip 文件无法解析，请检查压缩包");
    }
  }

  private ZhentiImportFileResult importOne(
      ZipFile zip,
      ZipEntry entry,
      String displayName,
      Long tenantId,
      Long examId,
      Long operatorId,
      ZhentiScope scope) {
    if (!displayName.toLowerCase().endsWith(".pdf")) {
      return ZhentiImportFileResult.failure(displayName, "文件类型非法，仅支持 PDF");
    }
    try (InputStream in = zip.getInputStream(entry)) {
      byte[] content = in.readAllBytes();
      var entity = ingestService.ingest(
          tenantId, examId, operatorId, displayName, content, null, null, scope);
      if (entity.getMetaPending() != null && entity.getMetaPending() == 1) {
        return ZhentiImportFileResult.pending(
            displayName, entity.getId(), entity.getTitle(), entity.getYear(),
            "无法从文件名解析年份或工种等级，请补录标题与年份");
      }
      return ZhentiImportFileResult.success(
          displayName, entity.getId(), entity.getTitle(), entity.getYear());
    } catch (BusinessException ex) {
      return ZhentiImportFileResult.failure(displayName, ex.getMessage());
    } catch (IOException ex) {
      log.warn("[ZhentiImport] 条目读取失败 name={} err={}", displayName, ex.toString());
      return ZhentiImportFileResult.failure(displayName, "文件读取失败");
    } catch (RuntimeException ex) {
      log.warn("[ZhentiImport] 条目导入异常 name={} err={}", displayName, ex.toString());
      return ZhentiImportFileResult.failure(displayName, "导入失败，请稍后重试");
    }
  }

  private ZipFile open(Path zipPath) throws IOException {
    try {
      return new ZipFile(zipPath.toFile(), StandardCharsets.UTF_8);
    } catch (ZipException | IllegalArgumentException ex) {
      // 中文 Windows 压缩包常见 GBK 文件名：UTF-8 解码失败时回退
      return new ZipFile(zipPath.toFile(), GBK);
    }
  }

  private static void assertSafeEntry(String name) {
    String normalized = name.replace('\\', '/');
    boolean absolute = normalized.startsWith("/") || normalized.matches("^[A-Za-z]:.*");
    boolean traversal = false;
    for (String segment : normalized.split("/")) {
      if ("..".equals(segment)) {
        traversal = true;
        break;
      }
    }
    if (absolute || traversal) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "zip 内含非法路径条目：" + name);
    }
  }

  private static String displayName(String entryName) {
    String normalized = entryName.replace('\\', '/');
    int slash = normalized.lastIndexOf('/');
    return slash >= 0 ? normalized.substring(slash + 1) : normalized;
  }

  /** 条目登记结果。 */
  public record ZipInspection(List<String> entryNames) {
    public int total() {
      return entryNames.size();
    }
  }
}
