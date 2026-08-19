package com.guoyu.platform.certification.collection.export.service;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 职业技能等级认定座次签到表 PDF 固定模板。
 *
 * <p>版式对齐现有座次签到表：A4 横向、页眉 + 3 列 x 3 行考生格，每页固定 9 名考生。
 * 所有格子、图片框和文字行均使用固定坐标，不由内容自动撑开或分页。</p>
 */
@Slf4j
public final class CollectionPdfTemplate {

  private static final float PAGE_WIDTH = PDRectangle.A4.getHeight();
  private static final float PAGE_HEIGHT = PDRectangle.A4.getWidth();
  private static final float MARGIN = 14f;
  private static final float HEADER_HEIGHT = 58f;
  private static final float GRID_TOP = PAGE_HEIGHT - MARGIN - HEADER_HEIGHT;
  private static final float GRID_BOTTOM = MARGIN;
  private static final float BLOCK_WIDTH = (PAGE_WIDTH - MARGIN * 2) / 3f;
  private static final float BLOCK_HEIGHT = (GRID_TOP - GRID_BOTTOM) / 3f;
  private static final float INFO_HEIGHT = 73f;
  private static final float LABEL_HEIGHT = 21f;
  private static final float INNER_PADDING = 2.5f;
  private static final float BORDER_WIDTH = 0.65f;
  private static final int COLUMNS = 3;
  private static final int ROWS = 3;
  private static final int CANDIDATES_PER_PAGE = COLUMNS * ROWS;
  private static final String EMPTY_IMAGE_TEXT = "暂无采集图片";

  private CollectionPdfTemplate() {
  }

  public static byte[] render(PdfTask task, List<PdfCandidate> candidates, String configuredFontPath)
      throws IOException {
    if (task == null || candidates == null || candidates.isEmpty()) {
      throw new IllegalArgumentException("PDF 模板缺少任务或考生数据");
    }
    try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      PDFont font = loadChineseFont(document, configuredFontPath);
      document.getDocumentInformation().setTitle("职业技能等级认定座次签到表");
      document.getDocumentInformation().setSubject("实操信息采集打印版");
      int pageCount = (candidates.size() + CANDIDATES_PER_PAGE - 1) / CANDIDATES_PER_PAGE;
      for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
        PDPage page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
        document.addPage(page);
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
          drawPageHeader(content, font, task);
          for (int slot = 0; slot < CANDIDATES_PER_PAGE; slot++) {
            int candidateIndex = pageIndex * CANDIDATES_PER_PAGE + slot;
            PdfCandidate candidate = candidateIndex < candidates.size() ? candidates.get(candidateIndex) : null;
            int row = slot / COLUMNS;
            int column = slot % COLUMNS;
            float x = MARGIN + column * BLOCK_WIDTH;
            float y = GRID_TOP - (row + 1) * BLOCK_HEIGHT;
            drawCandidateBlock(content, font, document, candidate, task, x, y);
          }
        }
      }
      document.save(output);
      return output.toByteArray();
    }
  }

  private static void drawPageHeader(PDPageContentStream content, PDFont font, PdfTask task)
      throws IOException {
    float top = PAGE_HEIGHT - MARGIN;
    drawCenteredText(content, font, "职业技能等级认定座次签到表", 18f, PAGE_WIDTH / 2f, top - 19f);
    String firstLine = "考场地址：" + safe(task.examSite()) + "    考试时间：" + safe(task.examTime());
    String secondLine = "考试任务：" + safe(task.taskName()) + "    工种/等级：" + safe(task.workTypeLevel());
    drawText(content, font, firstLine, 8.5f, MARGIN + 2f, top - 35f, PAGE_WIDTH - MARGIN * 2 - 4f);
    drawText(content, font, secondLine, 7.5f, MARGIN + 2f, top - 49f, PAGE_WIDTH - MARGIN * 2 - 4f);
    content.setLineWidth(BORDER_WIDTH);
    content.moveTo(MARGIN, GRID_TOP);
    content.lineTo(PAGE_WIDTH - MARGIN, GRID_TOP);
    content.stroke();
  }

  private static void drawCandidateBlock(
      PDPageContentStream content,
      PDFont font,
      PDDocument document,
      PdfCandidate candidate,
      PdfTask task,
      float x,
      float y
  ) throws IOException {
    float subWidth = BLOCK_WIDTH / 3f;
    float imageHeight = BLOCK_HEIGHT - INFO_HEIGHT;
    content.setLineWidth(BORDER_WIDTH);
    content.addRect(x, y, BLOCK_WIDTH, BLOCK_HEIGHT);
    content.stroke();
    content.moveTo(x, y + INFO_HEIGHT);
    content.lineTo(x + BLOCK_WIDTH, y + INFO_HEIGHT);
    content.stroke();
    for (int col = 1; col < 3; col++) {
      content.moveTo(x + subWidth * col, y + INFO_HEIGHT);
      content.lineTo(x + subWidth * col, y + BLOCK_HEIGHT);
      content.stroke();
    }

    if (candidate != null) {
      drawFixedImage(content, font, document, candidate.avatar(),
          x + INNER_PADDING, y + INFO_HEIGHT + INNER_PADDING,
          subWidth - INNER_PADDING * 2, imageHeight - INNER_PADDING * 2, true);
      drawFixedImage(content, font, document, candidate.checkInFingerprint(),
          x + subWidth + INNER_PADDING, y + INFO_HEIGHT + INNER_PADDING,
          subWidth - INNER_PADDING * 2, imageHeight - INNER_PADDING * 2, true);
    }
    drawSignatureColumn(content, font, document,
        candidate == null ? null : candidate.checkInSignature(),
        candidate == null ? null : candidate.submitSignature(),
        x + subWidth * 2, y + INFO_HEIGHT, imageHeight, candidate != null);

    float rowHeight = INFO_HEIGHT / 3f;
    for (int row = 1; row < 3; row++) {
      content.moveTo(x, y + rowHeight * row);
      content.lineTo(x + BLOCK_WIDTH, y + rowHeight * row);
      content.stroke();
    }
    content.moveTo(x + subWidth, y);
    content.lineTo(x + subWidth, y + INFO_HEIGHT);
    content.stroke();
    if (candidate == null) {
      return;
    }
    float valueCenterX = x + subWidth * 2f;
    drawCenteredText(content, font, safe(candidate.name()), 8f,
        x + subWidth / 2f, y + rowHeight * 2 + 7f);
    drawCenteredText(content, font, safe(task.examType()), 8f,
        valueCenterX, y + rowHeight * 2 + 7f);
    drawCenteredText(content, font, "证件号", 7f,
        x + subWidth / 2f, y + rowHeight + 7f);
    drawCenteredText(content, font, safe(candidate.idCardNo()), 6.6f,
        valueCenterX, y + rowHeight + 7f);
    drawCenteredText(content, font, "准考证", 7f,
        x + subWidth / 2f, y + 7f);
    drawCenteredText(content, font, safe(candidate.admissionNo()), 6.6f,
        valueCenterX, y + 7f);
  }

  private static void drawSignatureColumn(
      PDPageContentStream content,
      PDFont font,
      PDDocument document,
      byte[] checkInImageBytes,
      byte[] submitImageBytes,
      float x,
      float imageAreaY,
      float imageAreaHeight,
      boolean showPlaceholder
  ) throws IOException {
    float halfHeight = imageAreaHeight / 2f;
    float columnWidth = BLOCK_WIDTH / 3f;
    float imageWidth = columnWidth - INNER_PADDING * 2;
    float imageHeight = halfHeight - LABEL_HEIGHT - INNER_PADDING * 2;
    fillLabelCell(content, "考场签到", x,
        imageAreaY + imageAreaHeight - LABEL_HEIGHT, columnWidth, LABEL_HEIGHT, font);
    drawFixedImage(content, font, document, checkInImageBytes, x + INNER_PADDING,
        imageAreaY + halfHeight + INNER_PADDING, imageWidth, imageHeight, showPlaceholder);
    fillLabelCell(content, "交卷确认", x,
        imageAreaY + halfHeight - LABEL_HEIGHT, columnWidth, LABEL_HEIGHT, font);
    drawFixedImage(content, font, document, submitImageBytes, x + INNER_PADDING,
        imageAreaY + INNER_PADDING, imageWidth, imageHeight, showPlaceholder);
  }

  private static void fillLabelCell(
      PDPageContentStream content,
      String label,
      float x,
      float y,
      float width,
      float height,
      PDFont font
  ) throws IOException {
    if (!StringUtils.hasText(label)) {
      return;
    }
    content.setNonStrokingColor(0.88f, 0.89f, 0.91f);
    content.addRect(x, y, width, height);
    content.fill();
    content.setNonStrokingColor(0f, 0f, 0f);
    content.setLineWidth(BORDER_WIDTH);
    content.addRect(x, y, width, height);
    content.stroke();
    drawCenteredText(content, font, label, 8f, x + width / 2f, y + 6f);
  }

  private static void drawFixedImage(
      PDPageContentStream content,
      PDFont font,
      PDDocument document,
      byte[] imageBytes,
      float x,
      float y,
      float width,
      float height,
      boolean showPlaceholder
  ) throws IOException {
    if (imageBytes == null || imageBytes.length == 0) {
      if (showPlaceholder) {
        drawCenteredText(content, font, EMPTY_IMAGE_TEXT, 5.4f, x + width / 2f, y + height / 2f - 2f);
      }
      return;
    }
    try {
      BufferedImage source = ImageIO.read(new java.io.ByteArrayInputStream(imageBytes));
      if (source == null || source.getWidth() <= 0 || source.getHeight() <= 0) {
        drawCenteredText(content, font, EMPTY_IMAGE_TEXT, 5.4f, x + width / 2f, y + height / 2f - 2f);
        return;
      }
      PDImageXObject image = PDImageXObject.createFromByteArray(document, imageBytes, "collection-image");
      float scale = Math.min(width / image.getWidth(), height / image.getHeight());
      float drawWidth = image.getWidth() * scale;
      float drawHeight = image.getHeight() * scale;
      content.drawImage(image, x + (width - drawWidth) / 2f, y + (height - drawHeight) / 2f,
          drawWidth, drawHeight);
    } catch (IOException | RuntimeException ex) {
      drawCenteredText(content, font, EMPTY_IMAGE_TEXT, 5.4f, x + width / 2f, y + height / 2f - 2f);
    }
  }

  private static void drawText(
      PDPageContentStream content,
      PDFont font,
      String value,
      float size,
      float x,
      float y,
      float maxWidth
  ) throws IOException {
    String text = truncate(value, font, size, maxWidth);
    content.beginText();
    content.setFont(font, size);
    content.newLineAtOffset(x, y);
    content.showText(text);
    content.endText();
  }

  private static void drawCenteredText(
      PDPageContentStream content,
      PDFont font,
      String value,
      float size,
      float centerX,
      float y
  ) throws IOException {
    String text = safe(value);
    float width = font.getStringWidth(text) / 1000f * size;
    drawText(content, font, text, size, centerX - width / 2f, y, width + 1f);
  }

  private static String truncate(String value, PDFont font, float size, float maxWidth) throws IOException {
    String text = safe(value).replace('\n', ' ').replace('\r', ' ');
    if (font.getStringWidth(text) / 1000f * size <= maxWidth) {
      return text;
    }
    String suffix = "…";
    int end = text.length();
    while (end > 0 && font.getStringWidth(text.substring(0, end) + suffix) / 1000f * size > maxWidth) {
      end--;
    }
    return end == 0 ? suffix : text.substring(0, end) + suffix;
  }

  private static PDFont loadChineseFont(PDDocument document, String configuredFontPath) throws IOException {
    if (StringUtils.hasText(configuredFontPath)) {
      Path path = Path.of(configuredFontPath.trim());
      if (!Files.isRegularFile(path)) {
        throw new IOException("配置的 PDF 中文字体不存在：" + path);
      }
      return loadFontFile(document, path);
    }
    String[] systemFonts = {
        "C:/Windows/Fonts/simhei.ttf",
        "C:/Windows/Fonts/msyh.ttc",
        "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
        "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
        "/usr/share/fonts/noto-cjk/NotoSansCJK-Regular.ttc"
    };
    IOException lastFailure = null;
    for (String font : systemFonts) {
      Path path = Path.of(font);
      if (Files.isRegularFile(path)) {
        try {
          return loadFontFile(document, path);
        } catch (IOException ex) {
          lastFailure = ex;
        }
      }
    }
    throw new IOException(
        "未找到可嵌入的 TrueType 中文字体，请安装 fonts-wqy-zenhei 或配置 "
            + "platform.certification.collection.pdf-font-path 指向 .ttf/.ttc 文件",
        lastFailure);
  }

  private static PDFont loadFontFile(PDDocument document, Path path) throws IOException {
    if (isTrueTypeCollection(path)) {
      try (InputStream input = Files.newInputStream(path);
          TrueTypeCollection collection = new TrueTypeCollection(input)) {
        PDFont[] loadedFont = new PDFont[1];
        IOException[] lastFailure = new IOException[1];
        collection.processAllFonts(font -> {
          if (loadedFont[0] != null) {
            return;
          }
          try {
            loadedFont[0] = PDType0Font.load(document, font, true);
          } catch (IOException ex) {
            lastFailure[0] = ex;
          }
        });
        if (loadedFont[0] != null) {
          return loadedFont[0];
        }
        throw new IOException("TrueType 字体集合中没有可嵌入的字体：" + path, lastFailure[0]);
      }
    }
    try (InputStream input = Files.newInputStream(path)) {
      return PDType0Font.load(document, input, true);
    }
  }

  private static boolean isTrueTypeCollection(Path path) throws IOException {
    try (InputStream input = new BufferedInputStream(Files.newInputStream(path))) {
      byte[] signature = input.readNBytes(4);
      return Arrays.equals(signature, new byte[] {'t', 't', 'c', 'f'});
    }
  }

  private static String safe(String value) {
    return StringUtils.hasText(value) ? value.trim() : "-";
  }

  public record PdfTask(String taskName, String examTime, String examSite, String workTypeLevel, String examType) {
  }

  public record PdfCandidate(
      String name,
      String idCardNo,
      String admissionNo,
      String workType,
      String level,
      String status,
      byte[] avatar,
      byte[] checkInFingerprint,
      byte[] checkInSignature,
      byte[] submitFingerprint,
      byte[] submitSignature
  ) {
  }
}
