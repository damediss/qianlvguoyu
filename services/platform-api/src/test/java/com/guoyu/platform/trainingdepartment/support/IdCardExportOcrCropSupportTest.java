package com.guoyu.platform.trainingdepartment.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.trainingdepartment.service.BaiduOcrService;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

class IdCardExportOcrCropSupportTest {

  @TempDir
  Path tempDir;

  @Test
  void applyOcrCropResultUsesCardLocation() throws Exception {
    Path image = tempDir.resolve("front.jpg");
    writeSolidJpeg(image, 400, 300, Color.LIGHT_GRAY);

    BaiduOcrService baidu = Mockito.mock(BaiduOcrService.class);
    IdCardImageInsetCropper cropper = new IdCardImageInsetCropper(0.04);
    IdCardExportOcrCropSupport support = new IdCardExportOcrCropSupport(baidu, cropper);

    Map<String, Object> loc = new LinkedHashMap<>();
    loc.put("left", 40);
    loc.put("top", 30);
    loc.put("width", 320);
    loc.put("height", 200);
    Map<String, Object> ocr = new LinkedHashMap<>();
    ocr.put("card_location", loc);

    assertThat(support.applyOcrCropResult(image, ocr)).isTrue();

    BufferedImage out = ImageIO.read(image.toFile());
    assertThat(out).isNotNull();
    // 裁切后应明显小于原图
    assertThat(out.getWidth()).isLessThan(400);
    assertThat(out.getHeight()).isLessThan(300);
  }

  @Test
  void cropIdCardInPlaceFallsBackToTightenWhenOcrDisabled() throws Exception {
    Path image = tempDir.resolve("back.jpg");
    writeSolidJpeg(image, 200, 120, Color.WHITE);
    long before = Files.size(image);

    BaiduOcrService baidu = Mockito.mock(BaiduOcrService.class);
    when(baidu.isEnabled()).thenReturn(false);
    IdCardImageInsetCropper cropper = new IdCardImageInsetCropper(0.04);
    IdCardExportOcrCropSupport support = new IdCardExportOcrCropSupport(baidu, cropper);

    support.cropIdCardInPlace(image, "back");

    assertThat(Files.isRegularFile(image)).isTrue();
    assertThat(Files.size(image)).isPositive();
    // 内收后文件仍存在（尺寸可能因 JPEG 重编码变化）
    assertThat(Files.size(image)).isNotEqualTo(0L);
    assertThat(before).isPositive();
  }

  @Test
  void cropIdCardInPlaceCallsBaiduWhenEnabled() throws Exception {
    Path image = tempDir.resolve("front2.jpg");
    writeSolidJpeg(image, 400, 300, Color.GRAY);

    BaiduOcrService baidu = Mockito.mock(BaiduOcrService.class);
    when(baidu.isEnabled()).thenReturn(true);
    Map<String, Object> loc = new LinkedHashMap<>();
    loc.put("left", 50);
    loc.put("top", 40);
    loc.put("width", 300);
    loc.put("height", 180);
    Map<String, Object> ocr = new LinkedHashMap<>();
    ocr.put("card_location", loc);
    when(baidu.recognizeIdCard(anyString(), isNull(), eq("front"))).thenReturn(ocr);

    IdCardExportOcrCropSupport support =
        new IdCardExportOcrCropSupport(baidu, new IdCardImageInsetCropper(0.04));
    support.cropIdCardInPlace(image, "front");

    verify(baidu).recognizeIdCard(anyString(), isNull(), eq("front"));
    BufferedImage out = ImageIO.read(image.toFile());
    assertThat(out.getWidth()).isLessThan(400);
  }

  private static void writeSolidJpeg(Path path, int w, int h, Color color) throws Exception {
    BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = img.createGraphics();
    g.setColor(color);
    g.fillRect(0, 0, w, h);
    g.dispose();
    ImageIO.write(img, "jpg", path.toFile());
  }
}
