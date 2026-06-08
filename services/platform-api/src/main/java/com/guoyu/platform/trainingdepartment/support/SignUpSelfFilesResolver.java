package com.guoyu.platform.trainingdepartment.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpSelfEntity;
import com.guoyu.platform.trainingdepartment.vo.TrainSignUpSelfFileVO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 解析 train_sign_up_self.files JSON 并生成可访问 URL（对齐 PHP selflist） */
@Component
public class SignUpSelfFilesResolver {

  private final String cdnPath;
  private final String cdnUrl;
  private final ObjectMapper objectMapper;

  public SignUpSelfFilesResolver(
      @Value("${platform.cdn.path:./data/cdn}") String cdnPath,
      @Value("${platform.cdn.url:http://localhost:8080/api/exam/files}") String cdnUrl,
      ObjectMapper objectMapper) {
    this.cdnPath = cdnPath;
    this.cdnUrl = cdnUrl.endsWith("/") ? cdnUrl.substring(0, cdnUrl.length() - 1) : cdnUrl;
    this.objectMapper = objectMapper;
  }

  public List<TrainSignUpSelfFileVO> resolveFiles(String filesJson) {
    if (!StringUtils.hasText(filesJson)) {
      return List.of();
    }
    try {
      List<String> paths = objectMapper.readValue(filesJson, new TypeReference<List<String>>() {});
      List<TrainSignUpSelfFileVO> result = new ArrayList<>();
      for (String path : paths) {
        if (!StringUtils.hasText(path)) {
          continue;
        }
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        Path file = Paths.get(cdnPath, normalized);
        if (Files.exists(file)) {
          result.add(new TrainSignUpSelfFileVO(cdnUrl + "/" + normalized));
        }
      }
      return result;
    } catch (Exception ignored) {
      return List.of();
    }
  }

  public String appendFilePath(TrainSignUpSelfEntity signUp, String storedPath) {
    List<String> files = new ArrayList<>();
    if (StringUtils.hasText(signUp.getFiles())) {
      try {
        files.addAll(objectMapper.readValue(signUp.getFiles(), new TypeReference<List<String>>() {}));
      } catch (Exception ignored) {
        files = new ArrayList<>();
      }
    }
    files.add(storedPath);
    try {
      return objectMapper.writeValueAsString(files);
    } catch (Exception e) {
      throw new IllegalStateException("更新方案附件失败", e);
    }
  }

  public String publicUrl(String relativePath) {
    String normalized = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
    return cdnUrl + "/" + normalized;
  }
}
