package com.guoyu.platform.competition.service;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/** 提供竞赛理论/实操细目表导入模板。模板文件随应用版本发布，避免前后端字段漂移。 */
@Service
@Slf4j
public class CompetitionQuestionBlueprintTemplateService {

  private static final String XLSX_CONTENT_TYPE =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  public TemplateFile get(String contentType) {
    String normalized = normalize(contentType);
    String path;
    String filename;
    if ("theory".equals(normalized)) {
      path = "templates/competition-theory-blueprint-template.xlsx";
      filename = "竞赛理论细目表导入模板.xlsx";
    } else {
      path = "templates/competition-practical-blueprint-template.xlsx";
      filename = "竞赛实操细目表导入模板.xlsx";
    }
    ClassPathResource resource = new ClassPathResource(path);
    if (!resource.exists()) {
      log.error("Competition blueprint template is missing, contentType={}, path={}", normalized, path);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "细目表导入模板不存在");
    }
    try (InputStream input = resource.getInputStream()) {
      byte[] content = input.readAllBytes();
      if (content.length < 4 || content[0] != 'P' || content[1] != 'K') {
        log.error("Competition blueprint template is not a valid XLSX, contentType={}, path={}",
            normalized, path);
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "细目表导入模板无效");
      }
      return new TemplateFile(filename, XLSX_CONTENT_TYPE, content);
    } catch (IOException ex) {
      log.error("Failed to read competition blueprint template, contentType={}, path={}",
          normalized, path, ex);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "细目表导入模板读取失败");
    }
  }

  private String normalize(String contentType) {
    if ("theory".equalsIgnoreCase(contentType) || "practical".equalsIgnoreCase(contentType)) {
      return contentType.toLowerCase(java.util.Locale.ROOT);
    }
    throw new BusinessException(ErrorCode.BAD_REQUEST, "contentType 必须是 theory 或 practical");
  }

  public record TemplateFile(String filename, String contentType, byte[] content) {
  }
}
