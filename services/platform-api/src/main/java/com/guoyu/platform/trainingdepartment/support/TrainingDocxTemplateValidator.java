package com.guoyu.platform.trainingdepartment.support;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 校验开班申请表 Word 是否含 PhpWord 占位符 */
@Component
public class TrainingDocxTemplateValidator {

  private static final List<String> REQUIRED_ANY = List.of("pxdw", "company", "worktype", "wktype");
  private static final Pattern BROKEN_MACRO_PATTERN =
      Pattern.compile("\\$(?:\\{|[^\\{$]*>\\{)[^}$]*\\}");

  private final TrainingStoredFileResolver fileResolver;

  public TrainingDocxTemplateValidator(TrainingStoredFileResolver fileResolver) {
    this.fileResolver = fileResolver;
  }

  public void validateKaiBanApplication(String pathUsed, String pathSource) {
    String storedPath = StringUtils.hasText(pathUsed) ? pathUsed : pathSource;
    if (!StringUtils.hasText(storedPath)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传开班申请表 Word 模板");
    }
    byte[] bytes = readStoredFile(storedPath);
    if (!hasKaiBanMacros(bytes)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "开班申请表须上传含 Word 变量的文档（如 ${pxdw}、${company}、${worktype}）。"
              + "请在 Word 对应位置插入占位符后上传到「文件」；也可另传「设置变量后的Word」覆盖。");
    }
  }

  public boolean hasKaiBanMacros(byte[] docxBytes) {
    if (docxBytes == null || docxBytes.length == 0) {
      return false;
    }
    try {
      String xml = extractWordXml(docxBytes);
      if (!StringUtils.hasText(xml)) {
        return false;
      }
      xml = fixBrokenMacros(xml);
      for (String key : REQUIRED_ANY) {
        if (xml.contains("${" + key + "}") || xml.contains("$" + key + "$")) {
          return true;
        }
      }
      return false;
    } catch (IOException ex) {
      return false;
    }
  }

  private byte[] readStoredFile(String storedPath) {
    try {
      Path local = fileResolver.resolveLocalPath(storedPath);
      return Files.readAllBytes(local);
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "无法读取 Word 模板: " + ex.getMessage());
    }
  }

  static String extractWordXml(byte[] docxBytes) throws IOException {
    StringBuilder merged = new StringBuilder();
    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(docxBytes))) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        String name = entry.getName();
        if (!isWordPart(name)) {
          continue;
        }
        merged.append(new String(zis.readAllBytes(), StandardCharsets.UTF_8));
      }
    }
    return merged.toString();
  }

  static boolean isWordPart(String name) {
    return "word/document.xml".equals(name)
        || (name.startsWith("word/header") && name.endsWith(".xml"))
        || (name.startsWith("word/footer") && name.endsWith(".xml"));
  }

  /** 对齐 PHP PhpWord TemplateProcessor::fixBrokenMacros */
  static String fixBrokenMacros(String documentPart) {
    Matcher matcher = BROKEN_MACRO_PATTERN.matcher(documentPart);
    StringBuffer buffer = new StringBuffer();
    while (matcher.find()) {
      String fixed = matcher.group().replaceAll("<[^>]+>", "");
      matcher.appendReplacement(buffer, Matcher.quoteReplacement(fixed));
    }
    matcher.appendTail(buffer);
    return buffer.toString();
  }
}
