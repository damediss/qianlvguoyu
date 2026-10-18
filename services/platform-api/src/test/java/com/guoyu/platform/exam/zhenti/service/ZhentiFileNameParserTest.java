package com.guoyu.platform.exam.zhenti.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ZhentiFileNameParserTest {

  private final ZhentiFileNameParser parser = new ZhentiFileNameParser();

  @Test
  @DisplayName("详规样例：工种（别名）等级科目-年份 全解析")
  void parsesSampleFileName() {
    var parsed = parser.parse("铝电解工（铝及铝合金熔铸工）五级理论科目-2024.pdf");
    assertThat(parsed.title()).isEqualTo("铝电解工（铝及铝合金熔铸工）五级理论科目");
    assertThat(parsed.year()).isEqualTo("2024");
    assertThat(parsed.subject()).isEqualTo("理论");
    assertThat(parsed.levelParsed()).isTrue();
    assertThat(parsed.metaPending()).isFalse();
  }

  @Test
  @DisplayName("无年份 → metaPending")
  void missingYearIsPending() {
    var parsed = parser.parse("铝电解工（铝及铝合金熔铸工）五级理论科目.pdf");
    assertThat(parsed.year()).isNull();
    assertThat(parsed.title()).isEqualTo("铝电解工（铝及铝合金熔铸工）五级理论科目");
    assertThat(parsed.metaPending()).isTrue();
  }

  @Test
  @DisplayName("无科目关键词 → 仍解析工种等级，不 pending")
  void missingSubjectStillParsesLevel() {
    var parsed = parser.parse("铝电解工（铝及铝合金熔铸工）三级-2023.pdf");
    assertThat(parsed.subject()).isNull();
    assertThat(parsed.year()).isEqualTo("2023");
    assertThat(parsed.levelParsed()).isTrue();
    assertThat(parsed.metaPending()).isFalse();
  }

  @Test
  @DisplayName("分隔符清理：前导【年份】与尾部分隔符")
  void cleansSeparators() {
    var parsed = parser.parse("【2024】-铝电解工-理论_.pdf");
    assertThat(parsed.year()).isEqualTo("2024");
    assertThat(parsed.title()).isEqualTo("铝电解工-理论");
    // 无「工种（别名）等级」→ 待完善
    assertThat(parsed.metaPending()).isTrue();
  }

  @Test
  @DisplayName("纯英文文件名 → year 为空 + pending")
  void englishFileNameIsPending() {
    var parsed = parser.parse("exam_paper_final.pdf");
    assertThat(parsed.year()).isNull();
    assertThat(parsed.title()).isEqualTo("exam_paper_final");
    assertThat(parsed.metaPending()).isTrue();
  }

  @Test
  @DisplayName("只有年份 2024.pdf")
  void onlyYear() {
    var parsed = parser.parse("2024.pdf");
    assertThat(parsed.year()).isEqualTo("2024");
    assertThat(parsed.title()).isEqualTo("2024");
    assertThat(parsed.levelParsed()).isFalse();
    assertThat(parsed.metaPending()).isTrue();
  }

  @Test
  @DisplayName("半年份 2024上 保留在标题中，不误删")
  void halfYearKeepsTitle() {
    var parsed = parser.parse("2024上.pdf");
    assertThat(parsed.year()).isEqualTo("2024");
    assertThat(parsed.title()).isEqualTo("2024上");
    assertThat(parsed.metaPending()).isTrue();
  }

  @Test
  @DisplayName("超长标题截断到 200 字")
  void truncatesLongTitle() {
    String longName = "铝".repeat(300) + ".pdf";
    var parsed = parser.parse(longName);
    assertThat(parsed.title()).hasSize(200);
  }

  @Test
  @DisplayName("带路径的文件名只取末段")
  void stripsPath() {
    var parsed = parser.parse("C:\\真题\\铝电解工-2024.pdf");
    assertThat(parsed.year()).isEqualTo("2024");
    assertThat(parsed.title()).isEqualTo("铝电解工");
  }
}
