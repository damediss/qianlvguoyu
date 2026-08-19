package com.guoyu.platform.certification.defense;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;

class DefenseSummaryExportServiceTest {

  private final DefenseSummaryExportService service = new DefenseSummaryExportService();

  @Test
  void exportFillsOfficialSummaryTemplate() throws Exception {
    DefenseScoringModels.ProgressView progress = new DefenseScoringModels.ProgressView(
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

    var result = service.export(progress);

    assertThat(result.filename()).contains("技师、高级技师评价综合评定科目评审成绩汇总表");
    assertThat(result.filename()).endsWith(".docx");
    try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(result.data()))) {
      String allText = document.getParagraphs().stream().map(XWPFParagraph::getText).reduce("", (a, b) -> a + b);
      assertThat(allText).contains("评审项目：铝电解槽操作工认定");
      assertThat(allText).contains("评审时间：2026年8月13日");
      assertThat(allText).contains("评审地点：广西百色广投银海铝业有限责任公司");
      assertThat(allText).contains("评审职业：铝电解工");
      assertThat(allText).contains("专家甲、专家乙、专家丙");
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

  private static String cell(XWPFTableRow row, int index) {
    return row.getTableCells().get(index).getText().trim();
  }
}
