package com.guoyu.platform.trainingdepartment.support;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 学员证件照 URL，对齐 PHP YuntrainController::classstudentlistAction
 */
@Component
public class TrainingIdCardHeaderResolver {

  private final String cdnPath;
  private final String cdnUrl;

  public TrainingIdCardHeaderResolver(
      @Value("${platform.cdn.path:./data/cdn}") String cdnPath,
      @Value("${platform.cdn.url:http://localhost:8080/api/exam/files}") String cdnUrl) {
    this.cdnPath = cdnPath;
    this.cdnUrl = cdnUrl.endsWith("/") ? cdnUrl.substring(0, cdnUrl.length() - 1) : cdnUrl;
  }

  public String resolveHeaderUrl(String idCard) {
    if (!StringUtils.hasText(idCard)) {
      return "";
    }
    String id = idCard.trim();
    Path root = Paths.get(cdnPath).toAbsolutePath().normalize();

    Path oldFull = root.resolve("idCard/old/headerOrigin").resolve(id + "_full.png");
    if (Files.exists(oldFull)) {
      return cdnUrl + "/idCard/old/headerOrigin/" + id + "_full.png";
    }
    Path header2 = root.resolve("idCard/headerOrigin").resolve(id + "_header2.jpg");
    if (Files.exists(header2)) {
      return cdnUrl + "/idCard/headerOrigin/" + id + "_header2.jpg";
    }
    Path header = root.resolve("idCard/headerOrigin").resolve(id + "_header.jpg");
    if (Files.exists(header)) {
      return cdnUrl + "/idCard/headerOrigin/" + id + "_header.jpg";
    }
    return "";
  }
}
