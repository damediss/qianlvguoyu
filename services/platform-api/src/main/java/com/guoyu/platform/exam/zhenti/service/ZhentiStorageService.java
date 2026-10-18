package com.guoyu.platform.exam.zhenti.service;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.FileStorage;
import com.guoyu.platform.file.TencentCosFileStorage;
import com.guoyu.platform.file.support.FileLocalPathResolver;
import com.qcloud.cos.model.COSObject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 真题文件存储（复用 FileStorage，深模块；方案 §2.3）。
 *
 * <p>读取不走上游 HTTP 自回环：本地磁盘直接开流，COS 走 SDK 取对象；两者都不可用才 404。
 * 任何日志与出参都不打印 file_key 全量（只留对象键尾段便于排查）。
 */
@Service
@Slf4j
public class ZhentiStorageService {

  private final FileStorage storage;
  private final FileLocalPathResolver localPathResolver;

  public ZhentiStorageService(FileStorage storage, FileLocalPathResolver localPathResolver) {
    this.storage = storage;
    this.localPathResolver = localPathResolver;
  }

  /** 落库对象键固定为 zhenti/{examId}/{md5}.pdf（方案 §2.3）。 */
  public String putPdf(Long examId, String md5, byte[] bytes) {
    String objectKey = "zhenti/" + examId + "/" + md5 + ".pdf";
    storage.put(objectKey, new ByteArrayInputStream(bytes), bytes.length, "application/pdf");
    return objectKey;
  }

  /** 异步导入的 zip 暂存键；处理完成后 best-effort 删除。 */
  public String putImportZip(Long tenantId, Long importId, byte[] bytes) {
    String objectKey = "zhenti/import/" + tenantId + "/" + importId + ".zip";
    storage.put(objectKey, new ByteArrayInputStream(bytes), bytes.length, "application/zip");
    return objectKey;
  }

  /** 异步导入 zip 落存储（流式，避免把 200MB 压缩包整体读进内存）。 */
  public String putImportZip(Long tenantId, Long importId, Path source) {
    String objectKey = "zhenti/import/" + tenantId + "/" + importId + ".zip";
    try (InputStream in = Files.newInputStream(source)) {
      storage.put(objectKey, in, Files.size(source), "application/zip");
      return objectKey;
    } catch (IOException ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导入包暂存失败，请稍后重试");
    }
  }

  public void delete(String objectKey) {
    if (objectKey == null || objectKey.isBlank()) {
      return;
    }
    storage.delete(objectKey);
  }

  /** 打开文件流；调用方负责关闭。 */
  public InputStream open(String objectKey) {
    Optional<Path> local = localPathResolver.resolveExisting(objectKey);
    if (local.isPresent()) {
      try {
        return Files.newInputStream(local.get());
      } catch (IOException ex) {
        log.warn("[ZhentiStorage] 本地文件读取失败 keyTail={} err={}", tail(objectKey), ex.toString());
      }
    }
    InputStream cos = tryOpenCos(objectKey);
    if (cos != null) {
      return cos;
    }
    log.warn("[ZhentiStorage] 文件内容缺失 keyTail={}", tail(objectKey));
    throw new BusinessException(ErrorCode.NOT_FOUND, "文件不存在或已被清理");
  }

  private InputStream tryOpenCos(String objectKey) {
    if (!(storage instanceof TencentCosFileStorage cosStorage)) {
      return null;
    }
    try {
      COSObject object = cosStorage.getObject(objectKey);
      return object.getObjectContent();
    } catch (Exception ex) {
      log.warn("[ZhentiStorage] COS 读取失败 keyTail={} err={}", tail(objectKey), ex.toString());
      return null;
    }
  }

  private static String tail(String key) {
    if (key == null) {
      return "null";
    }
    return key.length() <= 24 ? key : "..." + key.substring(key.length() - 24);
  }
}
