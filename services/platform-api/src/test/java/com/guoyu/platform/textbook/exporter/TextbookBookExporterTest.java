package com.guoyu.platform.textbook.exporter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.textbook.entity.TextbookProjectEntity;
import com.guoyu.platform.textbook.exporter.TextbookBookExporter.BookChapter;
import com.guoyu.platform.textbook.exporter.TextbookBookExporter.BookPart;
import com.guoyu.platform.textbook.exporter.TextbookBookExporter.BookSection;
import com.guoyu.platform.textbook.exporter.TextbookOutlineExporter.ExportFile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 教材整册导出（方案 §12.1-TextbookBookExporterTest）：
 * 篇章节顺序与 Heading 层级、节内标题不污染导航、review 状态标记、
 * final 校验拒绝、内部图片嵌入、坏图片 final 报错、文件名口径。
 */
class TextbookBookExporterTest {

  private static final byte[] TINY_PNG = tinyPng();

  private static byte[] tinyPng() {
    try {
      BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
      Graphics2D g = image.createGraphics();
      g.setColor(Color.RED);
      g.fillRect(0, 0, 4, 4);
      g.dispose();
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      ImageIO.write(image, "png", out);
      return out.toByteArray();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private TextbookProjectEntity project() {
    TextbookProjectEntity project = new TextbookProjectEntity();
    project.setWorkTypeTitle("铝电解工");
    project.setDirection("铝电解槽操作工");
    project.setLevelCode(5);
    project.setStandardVersion("2020年版");
    project.setTotalWords(100000);
    return project;
  }

  private List<BookPart> sampleParts() {
    BookSection s1 = new BookSection(9101L, "更换阳极", "2.1", "approved",
        "## 节内小标题\n\n操作步骤正文。\n\n![示意图](textbook-asset://9901)\n");
    BookSection s2 = new BookSection(9102L, "残极检查", "2.2", "submitted",
        "待审定正文。");
    BookSection empty = new BookSection(9103L, "未开始节", "2.3", null, null);
    BookChapter chapter = new BookChapter(9001L, "铝电解槽操作", "2",
        List.of(s1, s2, empty));
    return List.of(new BookPart("第一部分 职业功能", List.of(chapter)));
  }

  private static List<String> paragraphTexts(XWPFDocument doc) {
    List<String> texts = new ArrayList<>();
    for (XWPFParagraph paragraph : doc.getParagraphs()) {
      texts.add(paragraph.getText());
    }
    return texts;
  }

  @Test
  @DisplayName("review：篇章节顺序 + Heading1~3 层级 + 节内标题不污染导航 + 状态标记/未编写")
  void reviewExportStructure() throws Exception {
    TextbookBookExporter exporter = new TextbookBookExporter(true, id -> TINY_PNG);
    ExportFile file = exporter.exportBook(project(), sampleParts());

    assertTrue(file.fileName().startsWith("教材正文_铝电解工_铝电解槽操作工_五级-初级工_review_"));
    assertTrue(file.fileName().endsWith(".docx"));

    try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(file.content()))) {
      List<String> texts = paragraphTexts(doc);
      int partIdx = indexContaining(texts, "第一部分 职业功能");
      int chapterIdx = indexContaining(texts, "2 铝电解槽操作");
      int sectionIdx = indexContaining(texts, "2.1 更换阳极");
      int innerIdx = indexContaining(texts, "节内小标题");
      assertTrue(partIdx >= 0 && chapterIdx > partIdx && sectionIdx > chapterIdx
          && innerIdx > sectionIdx, "篇→章→节→节内标题顺序不符");

      // Heading 层级：篇=Heading1 章=Heading2 节=Heading3（3 个节各一个）；
      // 节内标题不得用 Heading1~3
      List<String> headingStyles = new ArrayList<>();
      for (XWPFParagraph paragraph : doc.getParagraphs()) {
        if (paragraph.getStyle() != null && paragraph.getStyle().startsWith("Heading")) {
          headingStyles.add(paragraph.getStyle() + ":" + paragraph.getText());
        }
      }
      assertEquals(5, headingStyles.size(), "只有篇/章/各节标题进入三级导航，节内标题不得污染");
      assertTrue(headingStyles.get(0).startsWith("Heading1"));
      assertTrue(headingStyles.get(1).startsWith("Heading2"));
      for (int i = 2; i < 5; i++) {
        assertTrue(headingStyles.get(i).startsWith("Heading3"),
            "节标题应为 Heading3：" + headingStyles.get(i));
      }
      assertTrue(headingStyles.stream().noneMatch(s -> s.contains("节内小标题")));

      // review 标记：submitted 节标【状态：待审定】；空节插 [本节未编写]
      assertTrue(texts.stream().anyMatch(t -> t.contains("【状态：待审定】")));
      assertTrue(texts.stream().anyMatch(t -> t.contains("[本节未编写]")));
      assertTrue(texts.stream().anyMatch(t -> t.contains("编写审阅稿")));

      // 图片嵌入 + 链接文字（本文无外链，仅确认图片存在）
      boolean hasPicture = doc.getParagraphs().stream()
          .flatMap(p -> p.getRuns().stream())
          .anyMatch(run -> !run.getEmbeddedPictures().isEmpty());
      assertTrue(hasPicture, "内部图片应嵌入 docx");
    }
  }

  @Test
  @DisplayName("final：非全 approved / 有占位 / 坏图片 → 明确 409")
  void finalExportGuards() {
    TextbookBookExporter exporter = new TextbookBookExporter(false, id -> TINY_PNG);
    BusinessException notApproved = assertThrows(BusinessException.class,
        () -> exporter.exportBook(project(), sampleParts()));
    assertTrue(notApproved.getMessage().contains("残极检查"));

    BookSection approvedWithPlaceholder = new BookSection(9101L, "更换阳极", "2.1",
        "approved", "正文含 [待专家补充] 占位");
    BookPart part = new BookPart(null, List.of(
        new BookChapter(9001L, "章", "2", List.of(approvedWithPlaceholder))));
    BusinessException placeholder = assertThrows(BusinessException.class,
        () -> new TextbookBookExporter(false, id -> TINY_PNG).exportBook(project(),
            List.of(part)));
    assertTrue(placeholder.getMessage().contains("待专家补充"));

    BookSection approved = new BookSection(9101L, "更换阳极", "2.1", "approved",
        "![图](textbook-asset://9901)");
    BookPart badImagePart = new BookPart(null, List.of(
        new BookChapter(9001L, "章", "2", List.of(approved))));
    BusinessException badImage = assertThrows(BusinessException.class,
        () -> new TextbookBookExporter(false, id -> null).exportBook(project(),
            List.of(badImagePart)));
    assertTrue(badImage.getMessage().contains("无法读取"), "final 坏图片必须明确报错");
  }

  @Test
  @DisplayName("final：全部 approved 无占位 → 导出成功且不含状态标记")
  void finalExportSucceeds() throws Exception {
    BookSection approved = new BookSection(9101L, "更换阳极", "2.1", "approved",
        "## 小标题\n\n完成正文。");
    BookPart part = new BookPart("第一部分", List.of(
        new BookChapter(9001L, "铝电解槽操作", "2", List.of(approved))));
    ExportFile file = new TextbookBookExporter(false, id -> null)
        .exportBook(project(), List.of(part));
    assertTrue(file.fileName().contains("_final_"));
    try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(file.content()))) {
      List<String> texts = paragraphTexts(doc);
      assertTrue(texts.stream().noneMatch(t -> t.contains("【状态：")));
      assertTrue(texts.stream().anyMatch(t -> t.contains("完成正文")));
    }
  }

  @Test
  @DisplayName("单节/单章文件名口径（§9.3）")
  void sectionAndChapterFileNames() {
    BookSection section = new BookSection(9101L, "更换阳极", "2.1", "approved", "正文");
    BookChapter chapter = new BookChapter(9001L, "铝电解槽操作", "2", List.of(section));
    TextbookProjectEntity project = project();
    Pattern ts = Pattern.compile("_\\d{12}\\.docx$");
    ExportFile sectionFile = new TextbookBookExporter(true, id -> null)
        .exportSection(project, chapter, section);
    assertTrue(sectionFile.fileName().startsWith("教材节_铝电解工_铝电解槽操作_更换阳极_"));
    assertTrue(ts.matcher(sectionFile.fileName()).find());
    ExportFile chapterFile = new TextbookBookExporter(true, id -> null)
        .exportChapter(project, chapter);
    assertTrue(chapterFile.fileName().startsWith("教材章_铝电解工_铝电解槽操作_"));
    assertTrue(chapterFile.content().length > 100);
  }

  private static int indexContaining(List<String> texts, String needle) {
    for (int i = 0; i < texts.size(); i++) {
      if (texts.get(i) != null && texts.get(i).contains(needle)) {
        return i;
      }
    }
    return -1;
  }
}
