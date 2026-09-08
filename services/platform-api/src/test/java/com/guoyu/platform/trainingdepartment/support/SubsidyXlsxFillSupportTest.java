package com.guoyu.platform.trainingdepartment.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class SubsidyXlsxFillSupportTest {

  @Test
  void replacesScalarPlaceholdersInXlsx() throws Exception {
    byte[] template;
    try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      Sheet sheet = wb.createSheet();
      Row row = sheet.createRow(0);
      row.createCell(0).setCellValue("企业：${company}");
      row.createCell(1).setCellValue("工种：$worktype$");
      wb.write(bos);
      template = bos.toByteArray();
    }

    Map<String, String> vars = new LinkedHashMap<>();
    vars.put("company", "千铝工投");
    vars.put("worktype", "电工");
    byte[] out = TrainingXlsxVariableReplacer.replaceAll(template, vars);

    try (Workbook wb = WorkbookFactory.create(new java.io.ByteArrayInputStream(out))) {
      Sheet sheet = wb.getSheetAt(0);
      assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("企业：千铝工投");
      assertThat(sheet.getRow(0).getCell(1).getStringCellValue()).isEqualTo("工种：电工");
    }
    assertThat(TrainingXlsxVariableReplacer.extractKeys(template))
        .containsExactlyInAnyOrder("company", "worktype");
  }

  @Test
  void fillsRosterByPrototypeRow() throws Exception {
    byte[] template;
    try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      Sheet sheet = wb.createSheet();
      sheet.createRow(0).createCell(0).setCellValue("班级：${class_title}");
      Row proto = sheet.createRow(1);
      proto.createCell(0).setCellValue("${seq}");
      proto.createCell(1).setCellValue("${name}");
      proto.createCell(2).setCellValue("${idCard}");
      wb.write(bos);
      template = bos.toByteArray();
    }

    List<Map<String, String>> students =
        List.of(
            Map.of("seq", "1", "name", "张三", "idCard", "110101199001011234"),
            Map.of("seq", "2", "name", "李四", "idCard", "110101199002021234"));
    Map<String, String> classVars = Map.of("class_title", "电工一班");

    SubsidyXlsxRosterFiller.FillResult result =
        SubsidyXlsxRosterFiller.fill(template, students, classVars);
    assertThat(result.mode()).isEqualTo("placeholder_row");
    assertThat(result.studentCount()).isEqualTo(2);

    try (Workbook wb = WorkbookFactory.create(new java.io.ByteArrayInputStream(result.bytes()))) {
      Sheet sheet = wb.getSheetAt(0);
      assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("班级：电工一班");
      assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("张三");
      assertThat(sheet.getRow(2).getCell(1).getStringCellValue()).isEqualTo("李四");
      assertThat(sheet.getRow(2).getCell(0).getStringCellValue()).isEqualTo("2");
    }
  }

  @Test
  void fillsRosterByChineseHeader() throws Exception {
    byte[] template;
    try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      Sheet sheet = wb.createSheet();
      Row header = sheet.createRow(0);
      header.createCell(0).setCellValue("序号");
      header.createCell(1).setCellValue("姓名");
      header.createCell(2).setCellValue("身份证号码");
      header.createCell(3).setCellValue("联系电话");
      sheet.createRow(1); // data start
      wb.write(bos);
      template = bos.toByteArray();
    }

    List<Map<String, String>> students =
        List.of(
            Map.of("seq", "1", "name", "王五", "idCard", "220101199003031234", "phone", "13800000000"));
    SubsidyXlsxRosterFiller.FillResult result =
        SubsidyXlsxRosterFiller.fill(template, students, Map.of("company", "测试企业"));
    assertThat(result.mode()).isEqualTo("header_map");

    try (Workbook wb = WorkbookFactory.create(new java.io.ByteArrayInputStream(result.bytes()))) {
      Sheet sheet = wb.getSheetAt(0);
      assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("王五");
      assertThat(sheet.getRow(1).getCell(2).getStringCellValue()).isEqualTo("220101199003031234");
      assertThat(sheet.getRow(1).getCell(3).getStringCellValue()).isEqualTo("13800000000");
    }
  }

  @Test
  void fillsRosterByChineseHeaderWithSubsidyColumns() throws Exception {
    byte[] template;
    try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      Sheet sheet = wb.createSheet();
      Row header = sheet.createRow(0);
      header.createCell(0).setCellValue("序号");
      header.createCell(1).setCellValue("姓名");
      header.createCell(2).setCellValue("培训对象");
      header.createCell(3).setCellValue("班期");
      header.createCell(4).setCellValue("培训起止时间");
      header.createCell(5).setCellValue("发证时间");
      header.createCell(6).setCellValue("培训补贴");
      header.createCell(7).setCellValue("培训学校名称");
      header.createCell(8).setCellValue("就业情况\n工作单位及岗位");
      sheet.createRow(1);
      wb.write(bos);
      template = bos.toByteArray();
    }

    List<Map<String, String>> students =
        List.of(
            Map.ofEntries(
                Map.entry("seq", "1"),
                Map.entry("name", "黄克昌"),
                Map.entry("trainTarget", "岗位技能提升"),
                Map.entry("banqi", "电工一班"),
                Map.entry("pxtime", "2025-01-01 ~ 2025-03-01"),
                Map.entry("issueDate", "2025-06-01"),
                Map.entry("perPersonAmount", "1000"),
                Map.entry("trainSchool", "千铝工投"),
                Map.entry("employment", "某某公司 / 电工")));
    SubsidyXlsxRosterFiller.FillResult result =
        SubsidyXlsxRosterFiller.fill(template, students, Map.of("company", "某某公司"));
    assertThat(result.mode()).isEqualTo("header_map");

    try (Workbook wb = WorkbookFactory.create(new java.io.ByteArrayInputStream(result.bytes()))) {
      Sheet sheet = wb.getSheetAt(0);
      Row row = sheet.getRow(1);
      assertThat(row.getCell(1).getStringCellValue()).isEqualTo("黄克昌");
      assertThat(row.getCell(2).getStringCellValue()).isEqualTo("岗位技能提升");
      assertThat(row.getCell(3).getStringCellValue()).isEqualTo("电工一班");
      assertThat(row.getCell(4).getStringCellValue()).isEqualTo("2025-01-01 ~ 2025-03-01");
      assertThat(row.getCell(5).getStringCellValue()).isEqualTo("2025-06-01");
      assertThat(row.getCell(6).getStringCellValue()).isEqualTo("1000");
      assertThat(row.getCell(7).getStringCellValue()).isEqualTo("千铝工投");
      assertThat(row.getCell(8).getStringCellValue()).isEqualTo("某某公司 / 电工");
    }
  }

  @Test
  void fillsApplicationFormByChineseLabels() throws Exception {
    byte[] template;
    try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      Sheet sheet = wb.createSheet();
      sheet.createRow(0).createCell(0).setCellValue("培训机构职业培训补贴申请表");
      Row date = sheet.createRow(1);
      date.createCell(0).setCellValue("申报日期 ：2025年 月 日");
      Row r2 = sheet.createRow(2);
      r2.createCell(0).setCellValue("培训机构名称");
      r2.createCell(1).setCellValue("");
      r2.createCell(2).setCellValue("性质");
      r2.createCell(3).setCellValue("");
      Row r3 = sheet.createRow(3);
      r3.createCell(0).setCellValue("经办人姓名");
      r3.createCell(1).setCellValue("");
      r3.createCell(2).setCellValue("联系电话");
      r3.createCell(3).setCellValue("");
      Row r4 = sheet.createRow(4);
      r4.createCell(0).setCellValue("本次培训班次");
      r4.createCell(1).setCellValue("");
      Row r5 = sheet.createRow(5);
      r5.createCell(0).setCellValue("企业组织培训企业名称");
      r5.createCell(1).setCellValue("");
      Row r6 = sheet.createRow(6);
      r6.createCell(0).setCellValue("补贴总人数（人）");
      r6.createCell(1).setCellValue("");
      r6.createCell(2).setCellValue("补贴总金额（元）");
      r6.createCell(3).setCellValue("");
      wb.write(bos);
      template = bos.toByteArray();
    }

    Map<String, String> vars = new LinkedHashMap<>();
    vars.put("pxdw", "千铝工投职业培训学校");
    vars.put("fzr", "张三");
    vars.put("fzrtel", "13800001111");
    vars.put("class_title", "电工高级班");
    vars.put("company", "某某有限公司");
    vars.put("nums", "12");
    vars.put("amount", "36000");
    vars.put("today", "2025年09月08日");

    assertThat(SubsidyXlsxLabelFiller.detectMatchedKeys(template))
        .contains("pxdw", "fzr", "company", "nums", "amount");

    SubsidyXlsxLabelFiller.FillResult result = SubsidyXlsxLabelFiller.fill(template, vars);
    assertThat(result.matchedCount()).isGreaterThanOrEqualTo(6);

    try (Workbook wb = WorkbookFactory.create(new java.io.ByteArrayInputStream(result.bytes()))) {
      Sheet sheet = wb.getSheetAt(0);
      assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).contains("2025年09月08日");
      assertThat(sheet.getRow(2).getCell(1).getStringCellValue()).isEqualTo("千铝工投职业培训学校");
      assertThat(sheet.getRow(3).getCell(1).getStringCellValue()).isEqualTo("张三");
      assertThat(sheet.getRow(3).getCell(3).getStringCellValue()).isEqualTo("13800001111");
      assertThat(sheet.getRow(4).getCell(1).getStringCellValue()).isEqualTo("电工高级班");
      assertThat(sheet.getRow(5).getCell(1).getStringCellValue()).isEqualTo("某某有限公司");
      assertThat(sheet.getRow(6).getCell(1).getStringCellValue()).isEqualTo("12");
      assertThat(sheet.getRow(6).getCell(3).getStringCellValue()).isEqualTo("36000");
    }
  }

  @Test
  void doesNotWriteValueIntoWideMergedBlankRow() throws Exception {
    byte[] template;
    try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      Sheet sheet = wb.createSheet();
      Row label = sheet.createRow(0);
      label.createCell(0).setCellValue("补贴总人数（人）");
      label.createCell(1).setCellValue(""); // 正确值格
      Row blank = sheet.createRow(1);
      blank.createCell(0).setCellValue("");
      sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, 10)); // 通栏空行
      wb.write(bos);
      template = bos.toByteArray();
    }

    Map<String, String> vars = Map.of("nums", "9");
    SubsidyXlsxLabelFiller.FillResult result = SubsidyXlsxLabelFiller.fill(template, vars);
    try (Workbook wb = WorkbookFactory.create(new java.io.ByteArrayInputStream(result.bytes()))) {
      Sheet sheet = wb.getSheetAt(0);
      assertThat(sheet.getRow(0).getCell(1).getStringCellValue()).isEqualTo("9");
      String wide = sheet.getRow(1).getCell(0) == null ? "" : sheet.getRow(1).getCell(0).getStringCellValue();
      assertThat(wide).isNotEqualTo("9");
    }
  }

  @Test
  void doesNotPutTotalIntoUrbanUnemployedRow() throws Exception {
    byte[] template;
    try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      Sheet sheet = wb.createSheet();
      Row r0 = sheet.createRow(0);
      r0.createCell(0).setCellValue("补贴总人数（人）");
      r0.createCell(1).setCellValue("");
      Row r1 = sheet.createRow(1);
      r1.createCell(0).setCellValue("各类人员享受培训补贴人数");
      Row r2 = sheet.createRow(2);
      r2.createCell(0).setCellValue("城镇登记\n失业人员\n数");
      r2.createCell(1).setCellValue("");
      Row r3 = sheet.createRow(3);
      r3.createCell(0).setCellValue("农村转移\n就业劳动");
      r3.createCell(1).setCellValue("");
      Row r4 = sheet.createRow(4);
      r4.createCell(0).setCellValue("企业在职员工");
      r4.createCell(1).setCellValue("");
      wb.write(bos);
      template = bos.toByteArray();
    }

    Map<String, String> vars = new LinkedHashMap<>();
    vars.put("nums", "25");
    vars.put("count_enterprise_employee", "25");
    vars.put("count_urban_unemployed", "0");
    vars.put("count_rural_transfer", "0");
    vars.put("zero", "0");

    SubsidyXlsxLabelFiller.FillResult result = SubsidyXlsxLabelFiller.fill(template, vars);
    try (Workbook wb = WorkbookFactory.create(new java.io.ByteArrayInputStream(result.bytes()))) {
      Sheet sheet = wb.getSheetAt(0);
      assertThat(sheet.getRow(0).getCell(1).getStringCellValue()).isEqualTo("25");
      assertThat(sheet.getRow(2).getCell(1).getStringCellValue()).isEqualTo("0");
      assertThat(sheet.getRow(3).getCell(1).getStringCellValue()).isEqualTo("0");
      assertThat(sheet.getRow(4).getCell(1).getStringCellValue()).isEqualTo("25");
    }
  }
}
