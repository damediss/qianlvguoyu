package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guoyu.platform.common.exception.BusinessException;
import java.io.ByteArrayInputStream;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

class CompetitionQuestionBlueprintTemplateServiceTest {

  private final CompetitionQuestionBlueprintTemplateService service =
      new CompetitionQuestionBlueprintTemplateService();

  @Test
  void providesTheoryTemplateBasedOnTheBusinessQuestionHierarchy() throws Exception {
    var template = service.get("theory");

    assertThat(template.filename()).isEqualTo("竞赛理论细目表导入模板.xlsx");
    assertThat(template.content()).startsWith((byte) 'P', (byte) 'K');
    try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(template.content()))) {
      var row = workbook.getSheetAt(0).getRow(2);
      assertThat(row.getCell(0).getStringCellValue()).contains("考评范围一级");
      assertThat(row.getCell(2).getStringCellValue()).contains("考评范围二级");
      assertThat(row.getCell(8).getStringCellValue()).contains("鉴定点");
      assertThat(row.getCell(9).getStringCellValue()).isEqualTo("重要程度");
      assertThat(row.getCell(10).getStringCellValue()).isEqualTo("试题量");
    }
  }

  @Test
  void providesPracticalTemplateBasedOnTheBusinessAssessmentDetailTable() throws Exception {
    var template = service.get("practical");

    assertThat(template.filename()).isEqualTo("竞赛实操细目表导入模板.xlsx");
    try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(template.content()))) {
      var sheet = workbook.getSheetAt(0);
      assertThat(sheet.getRow(2).getCell(0).getStringCellValue()).isEqualTo("考评范围一级");
      assertThat(sheet.getRow(2).getCell(4).getStringCellValue()).isEqualTo("考评范围二级");
      assertThat(sheet.getRow(2).getCell(9).getStringCellValue()).isEqualTo("考评点");
      assertThat(sheet.getRow(3).getCell(6).getStringCellValue()).isEqualTo("技能要求");
      assertThat(sheet.getRow(3).getCell(12).getStringCellValue()).isEqualTo("试题量");
    }
  }

  @Test
  void rejectsUnsupportedTemplateType() {
    assertThatThrownBy(() -> service.get("score"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("contentType");
  }
}
