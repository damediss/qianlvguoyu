package com.guoyu.platform.trainingdepartment.support;

import com.guoyu.platform.trainingdepartment.service.BaiduOcrService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;
import java.util.concurrent.Semaphore;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 证书 PDF 无文本层时：渲染首页 → 百度通用 OCR → 文本。 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RensheSkillCertOcrSupport {

  /** 限制并发，避免百度 QPS 打满后整体更慢 */
  private static final Semaphore OCR_SLOTS = new Semaphore(4);

  private final BaiduOcrService baiduOcrService;

  public boolean isEnabled() {
    return baiduOcrService.isEnabled();
  }

  /** 对 PDF 首页做 OCR，成功返回识别文本，否则 null。 */
  public String ocrPdfFirstPage(byte[] pdfBytes) {
    if (!isEnabled() || pdfBytes == null || pdfBytes.length == 0) {
      return null;
    }
    try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
      return ocrFirstPage(doc);
    } catch (Exception ex) {
      log.warn("证书 PDF OCR 失败", ex);
      return null;
    }
  }

  /** 复用已打开的 PDDocument，避免二次加载。 */
  public String ocrFirstPage(PDDocument doc) {
    if (!isEnabled() || doc == null) {
      return null;
    }
    boolean acquired = false;
    try {
      OCR_SLOTS.acquire();
      acquired = true;
      byte[] jpeg = renderFirstPageJpeg(doc, 100);
      if (jpeg == null || jpeg.length == 0) {
        return null;
      }
      // 先走通用基础版（更快）；硬字段仍缺时再高精度
      String text = baiduOcrService.recognizeGeneralTextFast(jpeg);
      if (StringUtils.hasText(text)) {
        RensheSkillCertPdfParser.Parsed probe = RensheSkillCertPdfParser.parseText(text, "ocr.jpg");
        if (!RensheSkillCertPdfParser.needsOcr(probe)) {
          return text;
        }
      }
      String accurate = baiduOcrService.recognizeGeneralText(jpeg);
      return StringUtils.hasText(accurate) ? accurate : text;
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      return null;
    } catch (Exception ex) {
      log.warn("证书 PDF OCR 失败", ex);
      return null;
    } finally {
      if (acquired) {
        OCR_SLOTS.release();
      }
    }
  }

  static byte[] renderFirstPageJpeg(PDDocument doc, int dpi) throws Exception {
    if (doc.getNumberOfPages() < 1) {
      return null;
    }
    PDFRenderer renderer = new PDFRenderer(doc);
    BufferedImage image = renderer.renderImageWithDPI(0, dpi, ImageType.RGB);
    return toJpeg(image, 0.72f);
  }

  private static byte[] toJpeg(BufferedImage image, float quality) throws Exception {
    Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
    if (!writers.hasNext()) {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ImageIO.write(image, "jpg", bos);
      return bos.toByteArray();
    }
    ImageWriter writer = writers.next();
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (ImageOutputStream ios = ImageIO.createImageOutputStream(bos)) {
      writer.setOutput(ios);
      ImageWriteParam param = writer.getDefaultWriteParam();
      if (param.canWriteCompressed()) {
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);
      }
      writer.write(null, new IIOImage(image, null, null), param);
    } finally {
      writer.dispose();
    }
    return bos.toByteArray();
  }
}
