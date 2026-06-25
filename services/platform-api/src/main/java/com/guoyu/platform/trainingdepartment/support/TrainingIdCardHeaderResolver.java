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

  /** 班级学员列表证件照：本地文件 > 用户详情 > 报名人员 > 标准 CDN 路径 */
  public String resolveForStudent(String idCard, String detailHeader, String signUpHeader) {
    String fromFile = resolveHeaderUrl(idCard);
    if (StringUtils.hasText(fromFile)) {
      return fromFile;
    }
    String fromDetail = resolveStoredHeader(detailHeader);
    if (StringUtils.hasText(fromDetail)) {
      return fromDetail;
    }
    String fromSignUp = resolveStoredHeader(signUpHeader);
    if (StringUtils.hasText(fromSignUp)) {
      return fromSignUp;
    }
    return resolveCanonicalHeaderUrl(idCard);
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

  /** 将 DB 存储值转为可访问的完整 URL */
  public String resolveStoredHeader(String stored) {
    if (!StringUtils.hasText(stored) || isInvalidImageStored(stored)) {
      return "";
    }
    if (stored.startsWith("http://") || stored.startsWith("https://")) {
      return stored;
    }
    if (stored.startsWith("/api/")) {
      return stored;
    }
    String normalized = stored.startsWith("/") ? stored.substring(1) : stored;
    Path rel = Paths.get(cdnPath, normalized);
    if (Files.exists(rel)) {
      return cdnUrl + "/" + normalized;
    }
    return cdnUrl + "/" + normalized;
  }

  /** 本地无文件时仍返回 PHP 对齐的标准 CDN 路径（供前端直链加载） */
  private String resolveCanonicalHeaderUrl(String idCard) {
    if (!StringUtils.hasText(idCard)) {
      return "";
    }
    String id = idCard.trim();
    return cdnUrl + "/idCard/headerOrigin/" + id + "_header2.jpg";
  }

  private boolean isInvalidImageStored(String path) {
    String t = path.trim();
    return t.startsWith("process=image")
        || t.startsWith("x-oss-process=")
        || t.startsWith("?x-oss-process=")
        || t.matches("^(image/)?(resize|quality|crop).*");
  }
}
