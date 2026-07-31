package com.guoyu.platform.trainingdepartment.support;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 身份证图二次收紧：按比例四周内收，去掉百度 card_image / 历史图多余背景边。
 * 配置：platform.baidu.idcard-crop-inset-ratio（默认 0.04）。
 */
@Component
@Slf4j
public class IdCardImageInsetCropper {

  private final double insetRatio;

  public IdCardImageInsetCropper(
      @Value("${platform.baidu.idcard-crop-inset-ratio:0.04}") double insetRatio) {
    this.insetRatio = Math.max(0d, Math.min(0.2d, insetRatio));
  }

  public double insetRatio() {
    return insetRatio;
  }

  /** 对磁盘图片原地收紧；失败则保留原文件。 */
  public void tightenInPlace(Path imagePath) {
    if (imagePath == null || insetRatio <= 0d || !Files.isRegularFile(imagePath)) {
      return;
    }
    try {
      byte[] raw = Files.readAllBytes(imagePath);
      byte[] tightened = tightenBytes(raw);
      if (tightened != null && tightened.length > 0 && tightened != raw) {
        Files.write(imagePath, tightened);
      }
    } catch (Exception e) {
      log.warn("身份证导出二次裁剪失败 path={}", imagePath, e);
    }
  }

  /** 对图片字节做内收；无需裁剪或失败时返回原数组。 */
  public byte[] tightenBytes(byte[] raw) {
    if (raw == null || raw.length == 0 || insetRatio <= 0d) {
      return raw;
    }
    try {
      BufferedImage src = ImageIO.read(new ByteArrayInputStream(raw));
      if (src == null) {
        return raw;
      }
      int[] box = applyInset(0, 0, src.getWidth(), src.getHeight(), src.getWidth(), src.getHeight());
      if (box[2] >= src.getWidth() && box[3] >= src.getHeight()) {
        return raw;
      }
      return encodeJpeg(src.getSubimage(box[0], box[1], box[2], box[3]));
    } catch (Exception e) {
      log.warn("身份证字节二次裁剪失败", e);
      return raw;
    }
  }

  /**
   * 按百度 card_location 在原图上裁切并内收。
   * @return JPEG 字节；失败返回 null
   */
  public byte[] cropFromOriginal(Path localFile, int left, int top, int width, int height) {
    if (localFile == null || !Files.isRegularFile(localFile) || width <= 0 || height <= 0) {
      return null;
    }
    try {
      BufferedImage src = ImageIO.read(localFile.toFile());
      if (src == null) {
        return null;
      }
      int[] box = applyInset(left, top, width, height, src.getWidth(), src.getHeight());
      return encodeJpeg(src.getSubimage(box[0], box[1], box[2], box[3]));
    } catch (Exception e) {
      log.warn("身份证原图定位裁剪失败 path={}", localFile, e);
      return null;
    }
  }

  public int[] applyInset(int left, int top, int width, int height, int imgW, int imgH) {
    int insetX = (int) Math.round(width * insetRatio);
    int insetY = (int) Math.round(height * insetRatio);
    if (insetRatio > 0d) {
      insetX = Math.max(1, insetX);
      insetY = Math.max(1, insetY);
    }
    int x = Math.max(0, left + insetX);
    int y = Math.max(0, top + insetY);
    int w = Math.max(1, width - insetX * 2);
    int h = Math.max(1, height - insetY * 2);
    if (x + w > imgW) {
      w = Math.max(1, imgW - x);
    }
    if (y + h > imgH) {
      h = Math.max(1, imgH - y);
    }
    return new int[]{x, y, w, h};
  }

  private static byte[] encodeJpeg(BufferedImage image) throws Exception {
    BufferedImage rgb = image;
    if (image.getType() != BufferedImage.TYPE_INT_RGB) {
      rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
      Graphics2D g = rgb.createGraphics();
      g.drawImage(image, 0, 0, null);
      g.dispose();
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(rgb, "jpg", baos);
    return baos.toByteArray();
  }
}
