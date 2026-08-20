package com.guoyu.platform.trainingdepartment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.trainingdepartment.service.RensheScoreRosterParseService.ParsedRoster;
import com.guoyu.platform.trainingdepartment.service.RensheScoreRosterParseService.ParsedRow;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class RensheScoreRosterParseServiceTest {

  private final RensheScoreRosterParseService service = new RensheScoreRosterParseService();

  @Test
  void parseRensheRoster_readsMergedScoreHeaderAndPlanCode() throws Exception {
    ParsedRoster roster = service.parse(rensheBytes());
    assertThat(roster.layout()).isEqualTo(RensheScoreRosterParseService.LAYOUT_RENSHE);
    assertThat(roster.planName()).isEqualTo("20250726百色皓海炭极生产工二级技师-正考");
    assertThat(roster.planCode()).isEqualTo("250717S0018450010030016");
    assertThat(roster.examTimeText()).contains("2025-07-26");
    assertThat(roster.rows()).hasSize(3);

    ParsedRow first = roster.rows().get(0);
    assertThat(first.name()).isEqualTo("陈朝永");
    assertThat(first.idCard()).isEqualTo("452631197706194818");
    assertThat(first.ticketNo()).startsWith("250718");
    assertThat(first.jobName()).isEqualTo("炭极生产工");
    assertThat(first.levelName()).isEqualTo("二级技师");
    assertThat(first.theory()).isEqualByComparingTo("76");
    assertThat(first.skill()).isEqualByComparingTo("79.40");
    assertThat(first.comprehensive()).isEqualByComparingTo("79.50");
    assertThat(first.performance()).isNull();

    ParsedRow emptyPerf = roster.rows().get(1);
    assertThat(emptyPerf.name()).isEqualTo("李四");
    assertThat(emptyPerf.comprehensive()).isNull();
    assertThat(emptyPerf.theory()).isEqualByComparingTo("70");

    ParsedRow invalid = roster.rows().get(2);
    assertThat(invalid.name()).isEqualTo("王五");
    assertThat(invalid.idCard()).isNull();
    assertThat(invalid.idCardRaw()).isEqualTo("110101900101123");
  }

  @Test
  void parseFlatTemplate() throws Exception {
    try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = wb.createSheet();
      Row h = sheet.createRow(0);
      h.createCell(0).setCellValue("身份证号");
      h.createCell(1).setCellValue("姓名");
      h.createCell(2).setCellValue("综合");
      h.createCell(3).setCellValue("理论");
      h.createCell(4).setCellValue("技能");
      Row r = sheet.createRow(1);
      r.createCell(0).setCellValue("110101199001011234");
      r.createCell(1).setCellValue("张三");
      r.createCell(2).setCellValue(80);
      r.createCell(3).setCellValue(75);
      r.createCell(4).setCellValue(85);
      wb.write(out);
      ParsedRoster roster = service.parse(out.toByteArray());
      assertThat(roster.layout()).isEqualTo(RensheScoreRosterParseService.LAYOUT_FLAT);
      assertThat(roster.rows()).hasSize(1);
      assertThat(roster.rows().get(0).comprehensive()).isEqualByComparingTo("80.00");
      assertThat(roster.rows().get(0).skill()).isEqualByComparingTo("85.00");
    }
  }

  @Test
  void parseScore_dashIsEmpty() {
    assertThat(RensheScoreRosterParseService.parseScore("—")).isNull();
    assertThat(RensheScoreRosterParseService.parseScore("/")).isNull();
    assertThat(RensheScoreRosterParseService.parseScore("无")).isNull();
    assertThat(RensheScoreRosterParseService.parseScore("79.4")).isEqualByComparingTo("79.40");
  }

  @Test
  void normalizeIdCard_rejects15Digit() {
    assertThat(RensheScoreRosterParseService.normalizeIdCard("110101900101123")).isNull();
    assertThat(RensheScoreRosterParseService.normalizeIdCard("452631197706194818"))
        .isEqualTo("452631197706194818");
    assertThat(RensheScoreRosterParseService.normalizeIdCard("45263119770619481x"))
        .isEqualTo("45263119770619481X");
  }

  @Test
  void missingHeaderThrows() throws Exception {
    try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = wb.createSheet();
      Row r = sheet.createRow(0);
      r.createCell(0).setCellValue("不是成绩单");
      wb.write(out);
      assertThatThrownBy(() -> service.parse(out.toByteArray()))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("未识别到");
    }
  }

  private static byte[] rensheBytes() throws Exception {
    try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = wb.createSheet("成绩花名册");
      sheet.createRow(0).createCell(0).setCellValue("职业技能等级认定成绩花名册");
      Row meta1 = sheet.createRow(1);
      meta1.createCell(0).setCellValue("计划名称");
      meta1.createCell(1).setCellValue("20250726百色皓海炭极生产工二级技师-正考");
      meta1.createCell(4).setCellValue("计划编码");
      meta1.createCell(5).setCellValue("250717S0018450010030016");
      Row meta2 = sheet.createRow(2);
      meta2.createCell(0).setCellValue("考试时间：2025-07-26 08:00 - 2025-07-27 17:30");

      Row header = sheet.createRow(4);
      header.createCell(0).setCellValue("序号");
      header.createCell(1).setCellValue("准考证号");
      header.createCell(2).setCellValue("姓名");
      header.createCell(3).setCellValue("性别");
      header.createCell(4).setCellValue("证件号码");
      header.createCell(5).setCellValue("机构名称");
      header.createCell(6).setCellValue("职业工种");
      header.createCell(7).setCellValue("技能等级");
      header.createCell(8).setCellValue("考核成绩");
      sheet.addMergedRegion(new CellRangeAddress(4, 4, 8, 11));

      Row sub = sheet.createRow(5);
      sub.createCell(8).setCellValue("理论");
      sub.createCell(9).setCellValue("技能");
      sub.createCell(10).setCellValue("综合");
      sub.createCell(11).setCellValue("工作业绩");

      Row r1 = sheet.createRow(6);
      r1.createCell(0).setCellValue(1);
      r1.createCell(1).setCellValue("2507184501001");
      r1.createCell(2).setCellValue("陈朝永");
      r1.createCell(3).setCellValue("男");
      r1.createCell(4).setCellValue("452631197706194818");
      r1.createCell(5).setCellValue("某培训机构");
      r1.createCell(6).setCellValue("炭极生产工");
      r1.createCell(7).setCellValue("二级技师");
      r1.createCell(8).setCellValue(76);
      r1.createCell(9).setCellValue(79.4);
      r1.createCell(10).setCellValue(79.5);
      r1.createCell(11).setCellValue("—");

      Row r2 = sheet.createRow(7);
      r2.createCell(0).setCellValue(2);
      r2.createCell(1).setCellValue("2507184501002");
      r2.createCell(2).setCellValue("李四");
      r2.createCell(4).setCellValue("110101199001011234");
      r2.createCell(6).setCellValue("炭极生产工");
      r2.createCell(7).setCellValue("二级技师");
      r2.createCell(8).setCellValue(70);
      r2.createCell(9).setCellValue(72);
      r2.createCell(10).setCellValue("—");
      r2.createCell(11).setCellValue("/");

      Row r3 = sheet.createRow(8);
      r3.createCell(2).setCellValue("王五");
      r3.createCell(4).setCellValue("110101900101123");
      r3.createCell(8).setCellValue(60);
      r3.createCell(10).setCellValue(60);

      wb.write(out);
      return out.toByteArray();
    }
  }
}
