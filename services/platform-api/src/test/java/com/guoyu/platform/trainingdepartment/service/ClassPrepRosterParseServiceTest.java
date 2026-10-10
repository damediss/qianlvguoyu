package com.guoyu.platform.trainingdepartment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.trainingdepartment.service.ClassPrepRosterParseService.ContractDates;
import com.guoyu.platform.trainingdepartment.service.ClassPrepRosterParseService.ParsedRoster;
import com.guoyu.platform.trainingdepartment.service.ClassPrepRosterParseService.ParsedRow;
import java.io.ByteArrayOutputStream;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class ClassPrepRosterParseServiceTest {

  private final ClassPrepRosterParseService service = new ClassPrepRosterParseService();

  @Test
  void parseApprenticeRoster_skipsMergedHeaderAndFooter() throws Exception {
    ParsedRoster roster = service.parse(apprenticeBytes());
    assertThat(roster.company()).isEqualTo("测试碳素有限公司");
    assertThat(roster.trainingOrg()).isEqualTo("测试职业技术学院");
    assertThat(roster.classPeriod()).contains("第3班");
    assertThat(roster.rows()).hasSize(2);

    ParsedRow first = roster.rows().get(0);
    assertThat(first.name()).isEqualTo("张三");
    assertThat(first.gender()).isEqualTo("男");
    assertThat(first.nation()).isEqualTo("壮族");
    assertThat(first.education()).isEqualTo("大学专科");
    assertThat(first.political()).isEqualTo("群众");
    assertThat(first.idCard()).isEqualTo("45260119850606511X");
    assertThat(first.phone()).isEqualTo("15977634123");
    assertThat(first.socialSecurity()).isEqualTo("453108966564");
    assertThat(first.workDateStart()).isEqualTo("2019-3-1");
    assertThat(first.workDateEnd()).isNull();
    assertThat(first.personCategory()).isEqualTo("企业职工-在岗农民工");

    ParsedRow second = roster.rows().get(1);
    assertThat(second.name()).isEqualTo("李四");
    assertThat(second.workDateStart()).isEqualTo("2022-7-1");
    assertThat(second.workDateEnd()).isEqualTo("2025-6-30");
    assertThat(second.remark()).isEqualTo("备注甲");
  }

  @Test
  void parseContract_openEndedAndRange() {
    ContractDates open = ClassPrepRosterParseService.parseContract("2019-3-1至无固定期限");
    assertThat(open.start()).isEqualTo("2019-3-1");
    assertThat(open.end()).isNull();
    assertThat(open.workTimeFallback()).isNull();

    ContractDates range = ClassPrepRosterParseService.parseContract("2022-7-1至2025-6-30");
    assertThat(range.start()).isEqualTo("2022-7-1");
    assertThat(range.end()).isEqualTo("2025-6-30");
  }

  @Test
  void normalizeIdCard_uppercasesX() {
    assertThat(ClassPrepRosterParseService.normalizeIdCard("45260119850606511x"))
        .isEqualTo("45260119850606511X");
  }

  @Test
  void missingApprenticeHeaderThrows() throws Exception {
    try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = wb.createSheet();
      Row h = sheet.createRow(0);
      h.createCell(0).setCellValue("姓名");
      h.createCell(1).setCellValue("身份证号");
      h.createCell(2).setCellValue("手机号");
      Row r = sheet.createRow(1);
      r.createCell(0).setCellValue("张三");
      r.createCell(1).setCellValue("110101199001011234");
      r.createCell(2).setCellValue("13800138000");
      wb.write(out);
      assertThatThrownBy(() -> service.parse(out.toByteArray()))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("未识别到学徒制花名册表头");
    }
  }

  private static byte[] apprenticeBytes() throws Exception {
    try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = wb.createSheet("Sheet1");
      sheet.addMergedRegion(new CellRangeAddress(0, 1, 0, 12));
      sheet.createRow(0).createCell(0).setCellValue("新型学徒制培训学员花名册");

      Row meta = sheet.createRow(2);
      meta.createCell(0).setCellValue("企业：");
      meta.createCell(2).setCellValue("测试碳素有限公司");
      meta.createCell(6).setCellValue("合作培训机构：");
      meta.createCell(7).setCellValue("测试职业技术学院");
      meta.createCell(8).setCellValue("班期：2024年市本级第3班（炭极生产工高级工）");
      sheet.createRow(3).createCell(8).setCellValue("班期编号：");

      Row header = sheet.createRow(4);
      header.createCell(0).setCellValue("序号");
      header.createCell(1).setCellValue("姓名");
      header.createCell(2).setCellValue("性别");
      header.createCell(3).setCellValue("民族");
      header.createCell(4).setCellValue("学历");
      header.createCell(5).setCellValue("政治面貌");
      header.createCell(6).setCellValue("身份证号码");
      header.createCell(7).setCellValue("家庭住址");
      header.createCell(8).setCellValue("联系电话");
      header.createCell(9).setCellValue("社保号");
      header.createCell(10).setCellValue("签订劳动合同时间(起、止）");
      header.createCell(11).setCellValue("培训人员类别");
      header.createCell(12).setCellValue("备注");
      for (int c = 0; c <= 12; c++) {
        sheet.addMergedRegion(new CellRangeAddress(4, 5, c, c));
      }

      Row r1 = sheet.createRow(6);
      r1.createCell(0).setCellValue(1);
      r1.createCell(1).setCellValue("张三");
      r1.createCell(2).setCellValue("男");
      r1.createCell(3).setCellValue("壮族");
      r1.createCell(4).setCellValue("大学专科");
      r1.createCell(5).setCellValue("群众");
      r1.createCell(6).setCellValue("45260119850606511x");
      r1.createCell(7).setCellValue("广西某路1号");
      r1.createCell(8).setCellValue("15977634123");
      r1.createCell(9).setCellValue("453108966564");
      r1.createCell(10).setCellValue("2019-3-1至无固定期限");
      r1.createCell(11).setCellValue("企业职工-在岗农民工");

      Row r2 = sheet.createRow(7);
      r2.createCell(0).setCellValue(2);
      r2.createCell(1).setCellValue("李四");
      r2.createCell(2).setCellValue("女");
      r2.createCell(3).setCellValue("汉族");
      r2.createCell(4).setCellValue("高中");
      r2.createCell(5).setCellValue("群众");
      r2.createCell(6).setCellValue("452625197509211474");
      r2.createCell(7).setCellValue("广西某路2号");
      r2.createCell(8).setCellValue("15777650405");
      r2.createCell(9).setCellValue("453103588519");
      r2.createCell(10).setCellValue("2022-7-1至2025-6-30");
      r2.createCell(11).setCellValue("企业职工-在岗农民工");
      r2.createCell(12).setCellValue("备注甲");

      Row footer = sheet.createRow(8);
      footer.createCell(0).setCellValue("女性11人，占总人数28%");
      sheet.addMergedRegion(new CellRangeAddress(8, 8, 0, 12));

      wb.write(out);
      return out.toByteArray();
    }
  }
}
