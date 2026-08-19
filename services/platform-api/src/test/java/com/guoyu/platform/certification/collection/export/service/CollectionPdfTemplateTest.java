package com.guoyu.platform.certification.collection.export.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class CollectionPdfTemplateTest {

  @Test
  void rendersTenCandidatesIntoFixedA4PagesAndKeepsEmptyImageSlotStable() throws Exception {
    List<CollectionPdfTemplate.PdfCandidate> candidates = new ArrayList<>();
    for (int i = 1; i <= 10; i++) {
      byte[] image = png(i % 2 == 0 ? Color.BLUE : Color.DARK_GRAY, 80 + i * 3, 50 + i * 2);
      candidates.add(new CollectionPdfTemplate.PdfCandidate(
          "考生" + String.format("%02d", i),
          "11010119900101" + String.format("%04d", i),
          "202600" + String.format("%04d", i),
          "铝电解工",
          "三级/高级工",
          "COMPLETED",
          image,
          image,
          i == 10 ? null : image,
          image,
          i == 10 ? null : image));
    }
    byte[] output = CollectionPdfTemplate.render(
        new CollectionPdfTemplate.PdfTask("PDF 固定版式测试任务", "2026-07-14 09:00-11:00", "一号考场（测试地址）",
            "铝电解工 / 三级", "理论"),
        candidates,
        "");
    Path outputPath = Path.of("outputs/practical-collection-qa/generated-collection-form-test.pdf");
    Files.createDirectories(outputPath.getParent());
    Files.write(outputPath, output);

    try (PDDocument document = Loader.loadPDF(output)) {
      assertThat(document.getNumberOfPages()).isEqualTo(2);
      assertThat(document.getPage(0).getMediaBox().getWidth()).isCloseTo(841.89f,
          org.assertj.core.data.Offset.offset(0.01f));
      assertThat(document.getPage(0).getMediaBox().getHeight()).isCloseTo(595.28f,
          org.assertj.core.data.Offset.offset(0.01f));
      String text = new PDFTextStripper().getText(document);
      assertThat(text).contains("PDF 固定版式测试任务", "考生01", "考生10", "暂无采集图片");
      assertThat(text).contains("考场地址：一号考场（测试地址）");
      assertThat(text).contains("考试时间：2026-07-14 09:00-11:00");
      assertThat(text).contains("理论");
    }
  }

  private byte[] png(Color color, int width, int height) throws Exception {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    graphics.setColor(Color.WHITE);
    graphics.fillRect(0, 0, width, height);
    graphics.setColor(color);
    graphics.fillRect(5, 5, Math.max(1, width - 10), Math.max(1, height - 10));
    graphics.dispose();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    ImageIO.write(image, "PNG", output);
    return output.toByteArray();
  }
}
