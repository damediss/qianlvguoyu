package com.guoyu.platform.certification.defense;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation;

class DefenseSummaryExportServiceTest {

  private final DefenseSummaryExportService service = new DefenseSummaryExportService();
  private static final byte[] THESIS_PNG = Base64.getDecoder().decode(
      "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");
  private static final byte[] PERFORMANCE_PNG = Base64.getDecoder().decode(
      "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");

  @Test
  void scoreSheetTemplatesAreValidDocx() throws Exception {
    for (String path : List.of(
        DefenseScoreSheetDocxWriter.THESIS_TEMPLATE,
        DefenseScoreSheetDocxWriter.PERFORMANCE_TEMPLATE
    )) {
      try (XWPFDocument document = DefenseSummaryExportService.openTemplate(path)) {
        assertThat(document.getTables()).isNotEmpty();
      }
    }
  }

  @Test
  void exportFillsOfficialSummaryTemplate() throws Exception {
    DefenseScoringModels.ProgressView progress = progress();

    var result = service.export(progress);

    assertThat(result.filename()).contains("技师、高级技师评价综合评定科目评审成绩汇总表");
    assertThat(result.filename()).endsWith(".docx");
    try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(result.data()))) {
      assertLandscapeA4(document);
      String allText = document.getParagraphs().stream().map(XWPFParagraph::getText).reduce("", (a, b) -> a + b);
      assertThat(allText).contains("评审项目：铝电解槽操作工认定");
      assertThat(allText).contains("评审时间：2026年8月13日");
      assertThat(allText).contains("评审地点：广西百色广投银海铝业有限责任公司");
      assertThat(allText).contains("评审职业：铝电解工");
      assertThat(allText).contains("专家甲");
      assertThat(allText).contains("专家乙");
      assertThat(allText).contains("专家丙");
      XWPFTable table = document.getTables().get(0);
      assertThat(table.getNumberOfRows()).isEqualTo(4);
      XWPFTableRow first = table.getRow(2);
      assertThat(cell(first, 2)).isEqualTo("张三");
      assertThat(cell(first, 7)).isEqualTo("80");
      assertThat(cell(first, 12)).isEqualTo("80");
      XWPFTableRow second = table.getRow(3);
      assertThat(cell(second, 2)).isEqualTo("李四");
      assertThat(cell(second, 12)).isEqualTo("不合格");
    }
  }

  @Test
  void exportInsertsThesisSignaturesAndAssessorSheets() throws Exception {
    DefenseScoringModels.ProgressView progress = progress();
    DefenseScoringModels.ScoreSheetView thesis = new DefenseScoringModels.ScoreSheetView(
        DefenseScoreCatalog.SHEET_THESIS,
        DefenseScoreCatalog.STATUS_SUBMITTED,
        new DefenseScoringModels.ThesisPayload(
            "槽控实践总结",
            List.of(false, false, false, false),
            "",
            List.of(15, 15, 15, 15, 15, 15, 10),
            List.of("问题甲", "问题乙", "问题丙"),
            List.of(25, 20, 25, 20)
        ),
        null,
        new BigDecimal("100.00"),
        new BigDecimal("90.00"),
        null,
        new BigDecimal("94.00"),
        false,
        101L,
        "a".repeat(64),
        "专家甲",
        null
    );
    DefenseScoringModels.ScoreSheetView performance = new DefenseScoringModels.ScoreSheetView(
        DefenseScoreCatalog.SHEET_PERFORMANCE,
        DefenseScoreCatalog.STATUS_SUBMITTED,
        null,
        DefenseScoreCatalog.emptyPerformance(),
        null,
        null,
        new BigDecimal("80.00"),
        new BigDecimal("80.00"),
        false,
        201L,
        "b".repeat(64),
        "专家甲",
        null
    );
    List<DefenseSummaryExportService.ExpertSignImage> signs = List.of(
        new DefenseSummaryExportService.ExpertSignImage(1L, "专家甲", THESIS_PNG),
        new DefenseSummaryExportService.ExpertSignImage(2L, "专家乙", THESIS_PNG),
        new DefenseSummaryExportService.ExpertSignImage(3L, "专家丙", THESIS_PNG)
    );
    List<DefenseSummaryExportService.AssessorSheetExport> sheets = List.of(
        new DefenseSummaryExportService.AssessorSheetExport(
            "张三", "2608064501032300010", "铝电解工", "一级/高级技师", "专家甲",
            thesis, performance, THESIS_PNG, PERFORMANCE_PNG),
        new DefenseSummaryExportService.AssessorSheetExport(
            "张三", "2608064501032300010", "铝电解工", "一级/高级技师", "专家乙",
            thesis, performance, THESIS_PNG, PERFORMANCE_PNG),
        new DefenseSummaryExportService.AssessorSheetExport(
            "张三", "2608064501032300010", "铝电解工", "一级/高级技师", "专家丙",
            thesis, performance, THESIS_PNG, PERFORMANCE_PNG)
    );

    var result = service.export(progress, signs, sheets);

    assertThat(result.filename()).endsWith(".zip");
    Map<String, byte[]> files = unzip(result.data());
    assertThat(files.keySet()).hasSize(7);
    assertThat(files.keySet()).filteredOn(name -> name.contains("汇总表")).hasSize(1);
    assertThat(files.keySet()).filteredOn(name -> name.contains("论文")).hasSize(3);
    assertThat(files.keySet()).filteredOn(name -> name.contains("业绩评估")).hasSize(3);

    byte[] summaryBytes = files.entrySet().stream()
        .filter(entry -> entry.getKey().contains("汇总表"))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElseThrow();
    try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(summaryBytes))) {
      XWPFParagraph expertParagraph = document.getParagraphs().stream()
          .filter(paragraph -> paragraph.getText() != null && paragraph.getText().startsWith("评审专家"))
          .findFirst()
          .orElseThrow();
      List<byte[]> footerPictures = expertParagraph.getRuns().stream()
          .map(XWPFRun::getEmbeddedPictures)
          .flatMap(List::stream)
          .map(picture -> picture.getPictureData().getData())
          .toList();
      assertThat(footerPictures).hasSize(3);
      assertThat(footerPictures).allMatch(bytes -> java.util.Arrays.equals(bytes, THESIS_PNG));
      assertThat(document.getTables().size()).isEqualTo(1);
      assertLandscapeA4(document);
    }

    byte[] thesisBytes = files.entrySet().stream()
        .filter(entry -> entry.getKey().contains("论文") && entry.getKey().contains("专家甲"))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElseThrow();
    try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(thesisBytes))) {
      StringBuilder all = new StringBuilder();
      document.getParagraphs().forEach(paragraph -> all.append(paragraph.getText()));
      document.getTables().forEach(table -> table.getRows().forEach(row ->
          row.getTableCells().forEach(cell -> all.append(cell.getText()))));
      assertThat(all.toString()).contains("技术总结（论文）答辩评审评分表");
      assertThat(all.toString()).contains("申报职业：铝电解工  级别：一级/高级技师");
      assertThat(all.toString()).contains("槽控实践总结");
      assertThat(all.toString()).contains("问题甲");
      assertThat(all.toString()).contains("否定说明：");
      assertThat(all.toString()).contains("1.口有严重抄袭行为或内容违背社会主义价值观。");
      assertThat(document.getTables().get(0).getWidth()).isEqualTo(8328);
      XWPFParagraph title = firstBodyParagraph(document);
      assertThat(title.getRuns().get(0).getFontSize()).isEqualTo(16);
      assertThat(title.getRuns().get(0).isBold()).isTrue();
      assertCenteredSignatureOnly(document.getTables().get(0), 24, THESIS_PNG);
      assertPortraitA4(document);
    }

    byte[] performanceBytes = files.entrySet().stream()
        .filter(entry -> entry.getKey().contains("业绩评估") && entry.getKey().contains("专家甲"))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElseThrow();
    try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(performanceBytes))) {
      StringBuilder all = new StringBuilder();
      document.getParagraphs().forEach(paragraph -> all.append(paragraph.getText()));
      document.getTables().forEach(table -> table.getRows().forEach(row ->
          row.getTableCells().forEach(cell -> all.append(cell.getText()))));
      assertThat(all.toString()).contains("技师、高级技师业绩评估报告评分表");
      assertThat(all.toString()).contains("申报职业（工种）：铝电解工       级别：一级/高级技师");
      assertThat(all.toString()).contains("履行岗位职责能力");
      assertThat(all.toString()).contains("备注：每一项业绩有佐证材料，无佐证材料视为无效，不得分");
      XWPFParagraph title = firstBodyParagraph(document);
      assertThat(title.getRuns().get(0).getFontSize()).isEqualTo(14);
      assertThat(title.getRuns().get(0).isBold()).isTrue();
      assertCenteredSignatureOnly(document.getTables().get(0), 12, PERFORMANCE_PNG);
      assertPortraitA4(document);
    }
  }

  private static DefenseScoringModels.ProgressView progress() {
    return new DefenseScoringModels.ProgressView(
        5L,
        "20260804BSYHL",
        "铝电解槽操作工认定",
        "铝电解工",
        "一级/高级技师",
        "2026年8月13日",
        "广西百色广投银海铝业有限责任公司",
        "ready_to_complete",
        "configured",
        "答辩组",
        2,
        2,
        List.of(
            new DefenseScoringModels.ExpertView(1L, "专家甲", true),
            new DefenseScoringModels.ExpertView(2L, "专家乙", false),
            new DefenseScoringModels.ExpertView(3L, "专家丙", false)
        ),
        List.of(
            candidate(11L, "张三", "2608064501032300010", "452630198809204378", "90", "80", "70", "80.00", "100", "80", "60", "80.00", "80.00", false),
            candidate(12L, "李四", "2608064501032300011", "452630198608144911", "60", "60", "60", "60.00", "50", "50", "50", "50.00", null, true)
        )
    );
  }

  private static DefenseScoringModels.CandidateProgressView candidate(
      Long id,
      String name,
      String ticket,
      String idCard,
      String t1,
      String t2,
      String t3,
      String tAvg,
      String p1,
      String p2,
      String p3,
      String pAvg,
      String finalScore,
      boolean rejected
  ) {
    return new DefenseScoringModels.CandidateProgressView(
        id,
        ticket,
        ticket,
        name,
        idCard,
        idCard,
        "铝电解工",
        "一级/高级技师",
        true,
        "completed",
        List.of(decimal(t1), decimal(t2), decimal(t3)),
        decimal(tAvg),
        List.of(decimal(p1), decimal(p2), decimal(p3)),
        decimal(pAvg),
        decimal(finalScore),
        rejected,
        List.of()
    );
  }

  private static BigDecimal decimal(String value) {
    return value == null ? null : new BigDecimal(value);
  }

  private static Map<String, byte[]> unzip(byte[] data) throws Exception {
    Map<String, byte[]> files = new LinkedHashMap<>();
    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(data), StandardCharsets.UTF_8)) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        files.put(entry.getName(), zip.readAllBytes());
      }
    }
    return files;
  }

  private static String cell(XWPFTableRow row, int index) {
    return row.getTableCells().get(index).getText().trim();
  }

  private static void assertCenteredSignatureOnly(XWPFTable table, int rowIndex, byte[] expectedPng) {
    XWPFTableCell cell = table.getRow(rowIndex).getCell(1);
    assertThat(cell.getText().trim()).isEmpty();
    List<byte[]> pictures = cell.getParagraphs().stream()
        .flatMap(paragraph -> paragraph.getRuns().stream())
        .flatMap(run -> run.getEmbeddedPictures().stream())
        .map(picture -> picture.getPictureData().getData())
        .toList();
    assertThat(pictures).hasSize(1);
    assertThat(pictures.get(0)).isEqualTo(expectedPng);
    assertThat(cell.getParagraphs().get(0).getAlignment()).isEqualTo(ParagraphAlignment.CENTER);
    assertThat(cell.getVerticalAlignment()).isEqualTo(XWPFTableCell.XWPFVertAlign.CENTER);
  }

  private static void assertLandscapeA4(XWPFDocument document) {
    CTPageSz pageSize = document.getDocument().getBody().getSectPr().getPgSz();
    assertThat(pageSize.getOrient()).isEqualTo(STPageOrientation.LANDSCAPE);
    assertThat(pageTwips(pageSize.getW())).isEqualTo(16838L);
    assertThat(pageTwips(pageSize.getH())).isEqualTo(11906L);
  }

  private static void assertPortraitA4(XWPFDocument document) {
    CTPageSz pageSize = document.getDocument().getBody().getSectPr().getPgSz();
    if (pageSize.getOrient() != null) {
      assertThat(pageSize.getOrient()).isEqualTo(STPageOrientation.PORTRAIT);
    }
    assertThat(pageTwips(pageSize.getW())).isEqualTo(11906L);
    assertThat(pageTwips(pageSize.getH())).isEqualTo(16838L);
    var margin = document.getDocument().getBody().getSectPr().getPgMar();
    assertThat(pageTwips(margin.getTop())).isEqualTo(1440L);
    assertThat(pageTwips(margin.getLeft())).isEqualTo(1800L);
  }

  private static XWPFParagraph firstBodyParagraph(XWPFDocument document) {
    return document.getBodyElements().stream()
        .filter(XWPFParagraph.class::isInstance)
        .map(XWPFParagraph.class::cast)
        .findFirst()
        .orElseThrow();
  }

  private static long pageTwips(Object value) {
    if (value instanceof Number number) {
      return number.longValue();
    }
    return new BigDecimal(String.valueOf(value)).longValue();
  }
}
