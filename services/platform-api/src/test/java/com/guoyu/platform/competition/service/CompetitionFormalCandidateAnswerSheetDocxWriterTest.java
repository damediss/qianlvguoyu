package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.competition.service.CompetitionFormalCandidateAnswerSheetDocxWriter.AnswerSheetDocument;
import com.guoyu.platform.competition.service.CompetitionFormalCandidateAnswerSheetDocxWriter.AnswerSheetDocument.Option;
import com.guoyu.platform.competition.service.CompetitionFormalCandidateAnswerSheetDocxWriter.AnswerSheetDocument.Question;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.xmlbeans.XmlCursor;
import org.junit.jupiter.api.Test;

class CompetitionFormalCandidateAnswerSheetDocxWriterTest {

  private final CompetitionFormalCandidateAnswerSheetDocxWriter writer =
      new CompetitionFormalCandidateAnswerSheetDocxWriter();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void fileNameUsesCandidateNameAndDisambiguatesDuplicates() {
    assertThat(CompetitionFormalCandidateAnswerSheetDocxWriter.docxFileName("张三", "QL001"))
        .isEqualTo("张三.docx");
    assertThat(CompetitionFormalCandidateAnswerSheetDocxWriter.docxFileName(" 张三/李? ", "QL001"))
        .isEqualTo("张三李.docx");
    assertThat(CompetitionFormalCandidateAnswerSheetDocxWriter.docxFileName(" ", "QL009"))
        .isEqualTo("QL009.docx");
    assertThat(CompetitionFormalCandidateAnswerSheetDocxWriter.docxFileName(null, null))
        .isEqualTo("答题卡.docx");

    Set<String> used = new LinkedHashSet<>();
    assertThat(CompetitionFormalCandidateAnswerSheetDocxWriter.uniqueDocxFileName("张三", "QL001", used))
        .isEqualTo("张三.docx");
    assertThat(CompetitionFormalCandidateAnswerSheetDocxWriter.uniqueDocxFileName("张三", "QL002", used))
        .isEqualTo("张三-QL002.docx");
    assertThat(CompetitionFormalCandidateAnswerSheetDocxWriter.uniqueDocxFileName("张三", "QL002", used))
        .isEqualTo("张三-2.docx");
  }

  @Test
  void writesOneAnswerSheetWithChoiceAndFillAnswers() throws Exception {
    AnswerSheetDocument document = new AnswerSheetDocument(
        "张三",
        "QL001",
        new BigDecimal("85.00"),
        new BigDecimal("100"),
        List.of(
            new Question(
                1,
                "single",
                "<p>阳极效应的原因是什么？</p>",
                true,
                true,
                new BigDecimal("2.00"),
                new BigDecimal("2"),
                objectMapper.readTree("{\"selectedOptionIds\":[\"101:A\"]}"),
                List.of(
                    new Option("101:A", "A", "氧化铝浓度过低"),
                    new Option("101:B", "B", "温度过高"))),
            new Question(
                2,
                "fill",
                "电解温度一般控制在____。",
                true,
                false,
                BigDecimal.ZERO,
                new BigDecimal("2"),
                objectMapper.readTree("{\"blanks\":[\"950℃\",\"\"]}"),
                List.of())),
        "测试实操成绩+仿真成绩导入",
        "挤压修模工",
        "测试单位",
        "450101199001011234");

    byte[] bytes = writer.write(document);

    String text = documentText(bytes);
    assertThat(text).contains("姓     名  张三");
    assertThat(text).contains("单 位 名 称  测试单位");
    assertThat(text).contains("身 份 证 号 码  450101199001011234");
    assertThat(text).contains("测试实操成绩+仿真成绩导入");
    assertThat(text).contains("挤压修模工理论笔答");
    assertThat(text).contains("一、单选题(2分)");
    assertThat(text).contains("1.A");
    assertThat(text).contains("四、填空题(2分)");
    assertThat(text).contains("2.950℃");
    assertThat(text).contains("85");
    assertThat(text).doesNotContain("金属轧制工实操笔答");
  }

  /**
   * 回归：单题大题题量超过模板占位段时走重建路径（2026-10-13 生产事故——
   * 旧实现溢出插入触发 POI insertNewParagraph 的 XmlAnyTypeImpl→CTP 强转 500）。
   * 断言 40 题全部落卡且顺序正确、后续题型分区完好。
   */
  @Test
  void writesAllQuestionsWhenSectionExceedsTemplateSlots() throws Exception {
    List<Question> questions = new ArrayList<>();
    for (int index = 1; index <= 40; index++) {
      questions.add(new Question(
          index,
          "single",
          "<p>第" + index + "题题干</p>",
          true,
          true,
          new BigDecimal("2.00"),
          new BigDecimal("2"),
          objectMapper.readTree("{\"selectedOptionIds\":[\"1" + String.format("%02d", index) + ":A\"]}"),
          List.of(
              new Option("1" + String.format("%02d", index) + ":A", "A", "选项A"),
              new Option("1" + String.format("%02d", index) + ":B", "B", "选项B"))));
    }
    AnswerSheetDocument document = new AnswerSheetDocument(
        "溢出考生",
        "QL099",
        new BigDecimal("80"),
        new BigDecimal("100"),
        questions,
        "赛事",
        "工种",
        "单位",
        "450101199001011234");

    byte[] bytes = writer.write(document);

    String text = documentText(bytes);
    assertThat(text).contains("一、单选题");
    assertThat(text).contains("共40题");
    for (int index = 1; index <= 40; index++) {
      assertThat(text).contains(index + ". 第" + index + "题题干");
    }
    assertThat(text.indexOf("1. 第1题题干"))
        .isLessThan(text.indexOf("20. 第20题题干"))
        .isLessThan(text.indexOf("40. 第40题题干"));
    assertThat(text).contains("二、多选题");
    assertThat(text).contains("三、判断题");
    assertThat(text).contains("考生答案汇总");
  }

  private static String documentText(byte[] bytes) throws Exception {
    try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
      StringBuilder text = new StringBuilder();
      XmlCursor cursor = document.getDocument().newCursor();
      cursor.selectPath(
          "declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' .//w:t");
      while (cursor.toNextSelection()) {
        text.append(cursor.getTextValue());
      }
      cursor.dispose();
      document.getFooterList().forEach(footer -> footer.getParagraphs()
          .forEach(paragraph -> text.append(paragraph.getText())));
      return text.toString();
    }
  }
}
