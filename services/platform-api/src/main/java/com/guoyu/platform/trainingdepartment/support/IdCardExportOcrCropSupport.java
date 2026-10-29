package com.guoyu.platform.trainingdepartment.support;

import com.guoyu.platform.trainingdepartment.service.BaiduOcrService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 默认模板导出：对身份证正/反面补一次百度 OCR 证件板裁剪（与 App 上传口径一致），
 * 失败时回退为四周比例内收。
 */
@Component
@Slf4j
public class IdCardExportOcrCropSupport {

  private final BaiduOcrService baiduOcrService;
  private final IdCardImageInsetCropper idCardCropper;

  public IdCardExportOcrCropSupport(
      BaiduOcrService baiduOcrService,
      IdCardImageInsetCropper idCardCropper) {
    this.baiduOcrService = baiduOcrService;
    this.idCardCropper = idCardCropper;
  }

  /**
   * @param imagePath 已拷贝到导出目录的正/反面图
   * @param side {@code front} 或 {@code back}
   */
  public void cropIdCardInPlace(Path imagePath, String side) {
    if (imagePath == null || !Files.isRegularFile(imagePath)) {
      return;
    }
    String normalizedSide = "back".equalsIgnoreCase(side) ? "back" : "front";
    boolean cropped = false;
    try {
      if (baiduOcrService.isEnabled()) {
        Map<String, Object> ocr = baiduOcrService.recognizeIdCard(
            imagePath.toAbsolutePath().toString(), null, normalizedSide);
        cropped = applyOcrCropResult(imagePath, ocr);
        if (cropped) {
          log.info("默认模板导出 OCR 证件裁剪成功 side={} path={}", normalizedSide, imagePath.getFileName());
        }
      }
    } catch (Exception e) {
      log.warn("默认模板导出 OCR 证件裁剪异常 side={} path={}", normalizedSide, imagePath.getFileName(), e);
    }
    if (!cropped) {
      idCardCropper.tightenInPlace(imagePath);
    }
  }

  /** 将 OCR 的 card_location / card_image 写回原文件；成功返回 true。 */
  @SuppressWarnings("unchecked")
  boolean applyOcrCropResult(Path imagePath, Map<String, Object> ocr) throws Exception {
    if (ocr == null || ocr.isEmpty() || imagePath == null || !Files.isRegularFile(imagePath)) {
      return false;
    }
    byte[] bytes = null;
    Object locationObj = ocr.get("card_location");
    if (locationObj instanceof Map<?, ?> loc) {
      bytes = idCardCropper.cropFromOriginal(
          imagePath,
          toInt(loc.get("left")),
          toInt(loc.get("top")),
          toInt(loc.get("width")),
          toInt(loc.get("height")));
    }
    if (bytes == null) {
      Object rawB64 = ocr.get("card_image_base64");
      if (rawB64 instanceof String cardImageBase64 && StringUtils.hasText(cardImageBase64)) {
        String pure = cardImageBase64;
        int comma = cardImageBase64.indexOf(',');
        if (cardImageBase64.startsWith("data:") && comma > 0) {
          pure = cardImageBase64.substring(comma + 1);
        }
        byte[] decoded = Base64.getDecoder().decode(pure);
        bytes = idCardCropper.tightenBytes(decoded);
      }
    }
    if (bytes == null || bytes.length == 0) {
      return false;
    }
    Files.write(imagePath, bytes);
    return true;
  }

  private static int toInt(Object v) {
    if (v instanceof Number n) {
      return n.intValue();
    }
    if (v == null) {
      return 0;
    }
    try {
      return Integer.parseInt(v.toString());
    } catch (NumberFormatException e) {
      return 0;
    }
  }
}
