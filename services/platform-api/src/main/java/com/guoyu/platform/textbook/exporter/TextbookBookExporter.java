package com.guoyu.platform.textbook.exporter;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.textbook.domain.TextbookMarkdownSupport;
import com.guoyu.platform.textbook.entity.TextbookProjectEntity;
import com.guoyu.platform.textbook.exporter.TextbookOutlineExporter.ExportFile;
import com.guoyu.platform.textbook.service.TextbookSectionContentApplicationService;
import com.vladsch.flexmark.ast.BlockQuote;
import com.vladsch.flexmark.ast.BulletList;
import com.vladsch.flexmark.ast.Code;
import com.vladsch.flexmark.ast.Emphasis;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.HardLineBreak;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.ast.IndentedCodeBlock;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.ast.ListBlock;
import com.vladsch.flexmark.ast.ListItem;
import com.vladsch.flexmark.ast.OrderedList;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.SoftLineBreak;
import com.vladsch.flexmark.ast.StrongEmphasis;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.ext.tables.TableBlock;
import com.vladsch.flexmark.ext.tables.TableBody;
import com.vladsch.flexmark.ext.tables.TableCell;
import com.vladsch.flexmark.ext.tables.TableRow;
import com.vladsch.flexmark.util.ast.Node;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STHighlightColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 教材正文 docx 合成导出（方案 §9，E1-E4）：POI XWPF，复用一期 ExportFile 与 Controller
 * 下载响应口径。篇→Heading1、章→Heading2、节→Heading3；节内 Markdown h1~h6 一律映射为
 * 「节内标题」自定义形态（加粗、字号递减、无 outline 级别），不得污染篇/章/节三级导航。
 * review 模式空节插 [本节未编写]、非 approved 标状态、占位黄色高亮；final 模式再次
 * 后端校验全部 approved 且无占位，坏图片明确报错并指出节名。
 */
public class TextbookBookExporter {

  private static final Logger log = LoggerFactory.getLogger(TextbookBookExporter.class);
  private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
  private static final Pattern ASSET_URI =
      Pattern.compile("^" + TextbookMarkdownSupport.ASSET_URI_PREFIX + "(\\d{1,19})$");
  /** 版心最大宽度 15cm（EMU：1cm = 360000） */
  private static final int MAX_IMAGE_WIDTH_EMU = 15 * 360000;

  /** 导出数据（由应用服务按 outline 顺序装配）。 */
  public record BookSection(Long id, String title, String sourceNo, String status,
                            String contentMd) {
  }

  public record BookChapter(Long id, String title, String sourceNo, List<BookSection> sections) {
  }

  public record BookPart(String title, List<BookChapter> chapters) {
  }

  /** 资产字节解析器：教材侧已完成归属校验与内部受控读取。返回 null 表示读取失败。 */
  public interface AssetBytesResolver {
    byte[] resolve(Long assetId);
  }

  private final boolean reviewMode;
  private final AssetBytesResolver assets;
  private final XWPFDocument doc = new XWPFDocument();

  public TextbookBookExporter(boolean reviewMode, AssetBytesResolver assets) {
    this.reviewMode = reviewMode;
    this.assets = assets;
  }

  // ============================================================
  // 入口
  // ============================================================

  public ExportFile exportBook(TextbookProjectEntity project, List<BookPart> parts) {
    if (!reviewMode) {
      verifyFinal(parts);
    }
    renderCover(project, "教材正文");
    renderParts(parts);
    return buildFile(bookFileName(project));
  }

  public ExportFile exportChapter(TextbookProjectEntity project, BookChapter chapter) {
    if (!reviewMode) {
      verifyFinal(List.of(new BookPart(null, List.of(chapter))));
    }
    renderCover(project, "教材章");
    renderHeading(chapterTitle(chapter), 2);
    renderSections(chapter.sections());
    return buildFile("教材章_" + sanitize(project.getWorkTypeTitle()) + "_"
        + sanitize(chapter.title()) + "_" + LocalDateTime.now().format(TS) + ".docx");
  }

  public ExportFile exportSection(TextbookProjectEntity project, BookChapter chapter,
      BookSection section) {
    if (!reviewMode) {
      verifyFinal(List.of(new BookPart(null, List.of(chapter))));
    }
    renderCover(project, "教材节");
    renderHeading(chapterTitle(chapter), 2);
    renderSection(section);
    return buildFile("教材节_" + sanitize(project.getWorkTypeTitle()) + "_"
        + sanitize(chapter.title()) + "_" + sanitize(section.title()) + "_"
        + LocalDateTime.now().format(TS) + ".docx");
  }

  private String chapterTitle(BookChapter chapter) {
    return chapter.sourceNo() == null || chapter.sourceNo().isEmpty()
        ? chapter.title() : chapter.sourceNo() + " " + chapter.title();
  }

  // ============================================================
  // final 再校验（不信任前端按钮状态，方案 §9.1-5）
  // ============================================================

  private void verifyFinal(List<BookPart> parts) {
    for (BookPart part : parts) {
      for (BookChapter chapter : part.chapters()) {
        for (BookSection section : chapter.sections()) {
          if (!TextbookSectionContentApplicationService.STATUS_APPROVED
              .equals(section.status())) {
            throw new BusinessException(ErrorCode.CONFLICT,
                "final 导出要求全部节已通过：「" + section.title() + "」当前状态："
                    + (section.status() == null ? "未编写" : section.status()));
          }
          if (section.contentMd() != null
              && section.contentMd().contains(TextbookMarkdownSupport.PLACEHOLDER)) {
            throw new BusinessException(ErrorCode.CONFLICT,
                "final 导出禁止含 [待专家补充] 占位：「" + section.title() + "」");
          }
        }
      }
    }
  }

  // ============================================================
  // 渲染
  // ============================================================

  private void renderCover(TextbookProjectEntity project, String kind) {
    XWPFParagraph title = doc.createParagraph();
    title.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
    XWPFRun run = title.createRun();
    run.setFontFamily("黑体");
    run.setFontSize(22);
    run.setBold(true);
    run.setText(sanitize(project.getWorkTypeTitle()) + "-" + sanitize(project.getDirection())
        + "（" + TextbookOutlineExporter.levelLabel(project.getLevelCode()) + "）");
    XWPFParagraph meta = doc.createParagraph();
    meta.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
    XWPFRun metaRun = meta.createRun();
    metaRun.setFontSize(11);
    String line = (project.getStandardVersion() == null ? "" : project.getStandardVersion() + "　")
        + "导出时间 " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    if (reviewMode) {
      line += "　编写审阅稿";
    }
    metaRun.setText(line);
  }

  private void renderParts(List<BookPart> parts) {
    for (BookPart part : parts) {
      if (part.title() != null) {
        renderHeading(part.title(), 1);
      }
      for (BookChapter chapter : part.chapters()) {
        renderHeading(chapterTitle(chapter), 2);
        renderSections(chapter.sections());
      }
    }
  }

  private void renderSections(List<BookSection> sections) {
    for (BookSection section : sections) {
      renderSection(section);
    }
  }

  private void renderSection(BookSection section) {
    renderHeading(section.sourceNo() == null || section.sourceNo().isEmpty()
        ? section.title() : section.sourceNo() + " " + section.title(), 3);
    if (reviewMode && !TextbookSectionContentApplicationService.STATUS_APPROVED
        .equals(section.status())) {
      XWPFParagraph marker = doc.createParagraph();
      XWPFRun markerRun = marker.createRun();
      markerRun.setFontSize(10);
      markerRun.setItalic(true);
      markerRun.setText("【状态：" + statusLabel(section.status()) + "】");
    }
    if (section.contentMd() == null || section.contentMd().isBlank()) {
      if (reviewMode) {
        XWPFParagraph empty = doc.createParagraph();
        XWPFRun emptyRun = empty.createRun();
        emptyRun.setItalic(true);
        emptyRun.setText("[本节未编写]");
      }
      return;
    }
    renderMarkdown(section);
  }

  private void renderHeading(String text, int level) {
    XWPFParagraph paragraph = doc.createParagraph();
    paragraph.setStyle("Heading" + level);
    XWPFRun run = paragraph.createRun();
    run.setFontFamily("黑体");
    run.setBold(true);
    run.setFontSize(switch (level) {
      case 1 -> 18;
      case 2 -> 16;
      default -> 14;
    });
    run.setText(text);
  }

  private void renderMarkdown(BookSection section) {
    Node root = TextbookMarkdownSupport.parse(section.contentMd());
    renderBlocks(root, 0, false, section);
  }

  private void renderBlocks(Node parent, int indent, boolean quote, BookSection section) {
    int bulletIndex = 1;
    for (Node node : parent.getChildren()) {
      if (node instanceof Heading heading) {
        renderInnerHeading(heading);
      } else if (node instanceof Paragraph paragraph) {
        renderInlineParagraph(paragraph, indent, quote, null);
      } else if (node instanceof BulletList || node instanceof OrderedList ordered) {
        renderList((ListBlock) node, indent, section);
      } else if (node instanceof BlockQuote) {
        renderBlocks(node, indent + 1, true, section);
      } else if (node instanceof FencedCodeBlock fenced) {
        renderCodeBlock(fenced.getContentChars().toString());
      } else if (node instanceof IndentedCodeBlock indented) {
        renderCodeBlock(indented.getContentChars().toString());
      } else if (node instanceof TableBlock table) {
        renderTable(table);
      } else {
        renderBlocks(node, indent, quote, section);
      }
      bulletIndex++;
    }
  }

  private void renderList(ListBlock list, int indent, BookSection section) {
    int index = 1;
    for (Node child : list.getChildren()) {
      if (!(child instanceof ListItem item)) {
        continue;
      }
      String prefix = list instanceof OrderedList ? (index + ". ") : "• ";
      for (Node block : item.getChildren()) {
        if (block instanceof Paragraph paragraph) {
          renderInlineParagraph(paragraph, indent + 1, false, prefix);
        } else if (block instanceof Heading heading) {
          renderInnerHeading(heading);
        } else {
          renderBlocks(block, indent + 1, false, section);
        }
      }
      index++;
    }
  }

  /** 节内标题：加粗、字号按级递减；不使用 Word Heading 样式、不进三级导航（§9.2）。 */
  private void renderInnerHeading(Heading heading) {
    XWPFParagraph paragraph = doc.createParagraph();
    XWPFRun run = paragraph.createRun();
    run.setBold(true);
    run.setFontFamily("黑体");
    run.setFontSize(switch (Math.min(Math.max(heading.getLevel(), 1), 6)) {
      case 1 -> 14;
      case 2 -> 13;
      case 3 -> 12;
      case 4 -> 12;
      case 5 -> 11;
      default -> 11;
    });
    appendInlineRuns(run, heading, false);
  }

  private void renderInlineParagraph(Node paragraph, int indent, boolean quote, String prefix) {
    XWPFParagraph out = doc.createParagraph();
    out.setSpacingBetween(1.5);
    if (quote) {
      // 引用：左缩进 + 斜体灰字（§9.2：引用左缩进）
      out.setIndentationLeft((Math.max(indent, 1)) * 720);
    } else if (indent > 0) {
      out.setIndentationLeft(indent * 480);
    }
    XWPFRun run = out.createRun();
    run.setFontFamily("宋体");
    run.setFontSize(12);
    run.setItalic(quote);
    if (quote) {
      run.setColor("666666");
    }
    if (prefix != null) {
      run.setText(prefix);
    }
    appendInlineRuns(run, paragraph, quote);
  }

  private void renderCodeBlock(String code) {
    for (String line : code.split("\n", -1)) {
      XWPFParagraph paragraph = doc.createParagraph();
      XWPFRun run = paragraph.createRun();
      run.setFontFamily("Courier New");
      run.setFontSize(10);
      run.setText(line);
    }
  }

  private void renderTable(TableBlock table) {
    XWPFTable xtable = doc.createTable();
    boolean firstRow = true;
    for (Node child : table.getChildren()) {
      if (!(child instanceof TableRow row)) {
        if (child instanceof TableBody body) {
          for (Node bodyRow : body.getChildren()) {
            if (bodyRow instanceof TableRow r) {
              appendTableRow(xtable, r, firstRow);
              firstRow = false;
            }
          }
        }
        continue;
      }
      appendTableRow(xtable, row, firstRow);
      firstRow = false;
    }
  }

  private void appendTableRow(XWPFTable xtable, TableRow row, boolean header) {
    XWPFTableRow xrow = xtable.createRow();
    int cellIndex = 0;
    for (Node cellNode : row.getChildren()) {
      if (!(cellNode instanceof TableCell)) {
        continue;
      }
      while (xrow.getTableCells().size() <= cellIndex) {
        xrow.addNewTableCell();
      }
      StringBuilder sb = new StringBuilder();
      collectText(cellNode, sb);
      var cell = xrow.getCell(cellIndex);
      cell.setText(header ? sb.toString() : sb.toString());
      if (header) {
        cell.getParagraphs().forEach(p -> p.getRuns().forEach(r -> r.setBold(true)));
      }
      cellIndex++;
    }
  }

  private void collectText(Node node, StringBuilder sb) {
    for (Node child : node.getChildren()) {
      if (child instanceof Text text) {
        sb.append(text.getChars());
      } else if (child instanceof Code code) {
        sb.append(code.getText());
      } else if (child instanceof SoftLineBreak || child instanceof HardLineBreak) {
        sb.append(' ');
      } else {
        collectText(child, sb);
      }
    }
  }

  /** 行内渲染：粗斜体、行内代码、链接（文字（URL））、图片（受控内嵌）、占位高亮。 */
  private void appendInlineRuns(XWPFRun baseRun, Node node, boolean bold) {
    for (Node child : node.getChildren()) {
      if (child instanceof Text text) {
        appendTextWithPlaceholderHighlight(baseRun, text.getChars().toString(), bold);
      } else if (child instanceof Code code) {
        XWPFRun run = sameFormatNewRun(baseRun);
        run.setFontFamily("Courier New");
        run.setText(code.getText().toString());
      } else if (child instanceof Emphasis || child instanceof StrongEmphasis) {
        appendInlineRuns(baseRun, child, true);
      } else if (child instanceof SoftLineBreak || child instanceof HardLineBreak) {
        baseRun.addBreak();
      } else if (child instanceof Link link) {
        StringBuilder sb = new StringBuilder();
        collectText(link, sb);
        String url = link.getUrl().toString();
        appendTextWithPlaceholderHighlight(baseRun, sb.toString().isBlank() ? url
            : sb + "（" + url + "）", bold);
      } else if (child instanceof Image image) {
        renderImage(baseRun, image.getUrl().toString());
      } else {
        appendInlineRuns(baseRun, child, bold);
      }
    }
  }

  private void appendTextWithPlaceholderHighlight(XWPFRun baseRun, String text, boolean bold) {
    if (text == null || text.isEmpty()) {
      return;
    }
    String placeholder = TextbookMarkdownSupport.PLACEHOLDER;
    int idx;
    int from = 0;
    while ((idx = text.indexOf(placeholder, from)) >= 0) {
      if (idx > from) {
        XWPFRun run = sameFormatNewRun(baseRun);
        run.setBold(bold);
        run.setText(text.substring(from, idx));
      }
      XWPFRun highlight = sameFormatNewRun(baseRun);
      highlight.setBold(bold);
      highlight.setText(placeholder);
      highlight.getCTR().addNewRPr().addNewHighlight().setVal(STHighlightColor.YELLOW);
      from = idx + placeholder.length();
    }
    if (from < text.length()) {
      XWPFRun run = sameFormatNewRun(baseRun);
      run.setBold(bold);
      run.setText(text.substring(from));
    }
  }

  private XWPFRun sameFormatNewRun(XWPFRun baseRun) {
    XWPFParagraph paragraph = baseRun.getParagraph();
    XWPFRun run = paragraph.createRun();
    run.setFontFamily(baseRun.getFontFamily());
    run.setFontSize(baseRun.getFontSize());
    return run;
  }

  /** 图片按原始比例缩放，版心最大 15cm；review 无法读取显示占位，final 报错指出节名（§9.2）。 */
  private void renderImage(XWPFRun baseRun, String uri) {
    Matcher matcher = ASSET_URI.matcher(uri == null ? "" : uri.trim());
    if (!matcher.matches()) {
      appendTextWithPlaceholderHighlight(baseRun, "[无效图片引用]", false);
      return;
    }
    Long assetId = Long.parseLong(matcher.group(1));
    byte[] bytes = assets == null ? null : assets.resolve(assetId);
    if (bytes == null || bytes.length == 0) {
      if (reviewMode) {
        appendTextWithPlaceholderHighlight(baseRun, "[图片无法读取 assetId=" + assetId + "]", false);
        log.warn("[教材导出] review 模式图片不可读：assetId={}", assetId);
        return;
      }
      throw new BusinessException(ErrorCode.CONFLICT, "final 导出失败：图片 assetId=" + assetId
          + " 无法读取，请检查教材资产");
    }
    // Word docx 不支持 webp 嵌入：明确失败路径（review 占位、final 报错），不产出坏图
    if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F'
        && bytes[3] == 'F' && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B'
        && bytes[11] == 'P') {
      if (reviewMode) {
        appendTextWithPlaceholderHighlight(baseRun,
            "[webp 图片暂不支持嵌入 docx assetId=" + assetId + "]", false);
        return;
      }
      throw new BusinessException(ErrorCode.CONFLICT,
          "final 导出失败：webp 图片不受 Word 支持（assetId=" + assetId + "），请改用 png/jpeg");
    }
    try {
      BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
      int widthEmu = MAX_IMAGE_WIDTH_EMU;
      int heightEmu;
      if (image != null && image.getWidth() > 0 && image.getHeight() > 0) {
        double ratio = (double) image.getHeight() / image.getWidth();
        heightEmu = (int) (widthEmu * ratio);
      } else {
        heightEmu = widthEmu;
      }
      String name = "asset-" + assetId;
      try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
        baseRun.addPicture(in, pictureType(bytes), name, widthEmu, heightEmu);
      }
    } catch (Exception e) {
      if (reviewMode) {
        appendTextWithPlaceholderHighlight(baseRun, "[图片嵌入失败 assetId=" + assetId + "]", false);
        return;
      }
      throw new BusinessException(ErrorCode.CONFLICT,
          "final 导出失败：图片 assetId=" + assetId + " 嵌入异常：" + e.getMessage());
    }
  }

  private static int pictureType(byte[] bytes) {
    if (bytes.length >= 4 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P') {
      return XWPFDocument.PICTURE_TYPE_PNG;
    }
    return XWPFDocument.PICTURE_TYPE_JPEG;
  }

  private ExportFile buildFile(String fileName) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      doc.write(out);
      doc.close();
      return new ExportFile(fileName,
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
          out.toByteArray());
    } catch (IOException e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "docx 导出失败：" + e.getMessage());
    }
  }

  private String bookFileName(TextbookProjectEntity project) {
    return "教材正文_" + sanitize(project.getWorkTypeTitle()) + "_"
        + sanitize(project.getDirection()) + "_"
        + TextbookOutlineExporter.levelLabel(project.getLevelCode()).replace("/", "-") + "_"
        + (reviewMode ? "review" : "final") + "_" + LocalDateTime.now().format(TS) + ".docx";
  }

  private static String sanitize(String raw) {
    return raw == null ? "" : raw.replaceAll("[\\\\/:*?\"<>|\\s]+", "");
  }

  private static String statusLabel(String status) {
    if (status == null) {
      return "未编写";
    }
    return switch (status) {
      case "draft" -> "草稿";
      case "submitted" -> "待审定";
      case "returned" -> "已打回";
      case "approved" -> "已通过";
      default -> status;
    };
  }
}
