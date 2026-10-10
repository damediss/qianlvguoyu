package com.guoyu.platform.trainingdepartment.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.trainingdepartment.support.SignUpPeopleSummaryExcelSupport.RosterPerson;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

class SignUpPeopleSummaryExcelSupportTest {

  private final SignUpPeopleSummaryExcelSupport support =
      new SignUpPeopleSummaryExcelSupport(new ObjectMapper());

  @Test
  void peopleRosterFileName_usesClassTitleAndSignUpId() {
    assertThat(SignUpPeopleSummaryExcelSupport.peopleRosterFileName("2026电工中级1班", 90029L))
        .isEqualTo("2026电工中级1班-报名人员汇总表-90029.xlsx");
  }

  @Test
  void write_matchesDesktopTemplateHeadersAndFills25Columns() throws Exception {
    RosterPerson person = new RosterPerson();
    person.name = "张三";
    person.idCardNumber = "110101199001011234";
    person.phone = "13800000000";
    person.age = 36;
    person.industrialPark = "经开区";
    person.workTypeName = "电工";
    person.level = "1";
    person.education = "大专";
    person.titleType = "工程师";
    person.titleLevel = "中级";
    person.school = "某职业技术学院";
    person.major = "电气";
    person.schoolNo = "NO-1";
    person.inSchoolDate = "2016-09-01";
    person.graduateDate = "2019-06-30";
    person.workTime = "2019-07";
    person.createTime = LocalDateTime.of(2026, 9, 1, 10, 0, 0);
    person.declarationCondition = "工作年限";
    person.workExp = "[{\"startDate\":\"2019-07\",\"endDate\":\"至今\",\"company\":\"测试公司\",\"position\":\"电工\"}]";
    person.address = "某路1号";
    person.workDateStart = "2019-07-01";
    person.idCardFront = "/f1";
    person.idCardBack = "/f2";
    person.idCardHeader = "/f3";
    person.schoolPaper = "/p";
    person.contactFirstPage = "/c1";
    person.contactExpirePage = "/c2";
    person.contactSignPage = "/c3";

    byte[] bytes = support.write("示例企业", List.of(person));
    try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
      Sheet sheet = wb.getSheet("Sheet0");
      Row header = sheet.getRow(0);
      for (int i = 0; i < SignUpPeopleSummaryExcelSupport.HEADERS.size(); i++) {
        assertThat(header.getCell(i).getStringCellValue())
            .isEqualTo(SignUpPeopleSummaryExcelSupport.HEADERS.get(i));
      }
      assertThat(header.getLastCellNum()).isEqualTo((short) 25);

      Row data = sheet.getRow(1);
      assertThat(data.getCell(0).getStringCellValue()).isEqualTo("张三");
      assertThat(data.getCell(1).getStringCellValue()).isEqualTo("110101199001011234");
      assertThat(data.getCell(2).getStringCellValue()).isEqualTo("13800000000");
      assertThat(data.getCell(3).getStringCellValue()).isEqualTo("36");
      assertThat(data.getCell(4).getStringCellValue()).isEqualTo("示例企业");
      assertThat(data.getCell(5).getStringCellValue()).isEqualTo("经开区");
      assertThat(data.getCell(6).getStringCellValue()).isEqualTo("电工");
      assertThat(data.getCell(7).getStringCellValue()).isEqualTo("四级/中级工");
      assertThat(data.getCell(8).getStringCellValue()).isEqualTo("大专");
      assertThat(data.getCell(9).getStringCellValue()).isEqualTo("工程师");
      assertThat(data.getCell(10).getStringCellValue()).isEqualTo("中级");
      assertThat(data.getCell(11).getStringCellValue()).isEqualTo("某职业技术学院");
      assertThat(data.getCell(12).getStringCellValue()).isEqualTo("电气");
      assertThat(data.getCell(13).getStringCellValue()).isEqualTo("NO-1");
      assertThat(data.getCell(14).getStringCellValue()).isEqualTo("2016-09-01");
      assertThat(data.getCell(15).getStringCellValue()).isEqualTo("2019-06-30");
      assertThat(data.getCell(16).getStringCellValue()).isEqualTo("2019-07");
      assertThat(data.getCell(17).getStringCellValue()).isEqualTo("2026-09-01 10:00:00");
      assertThat(data.getCell(18).getStringCellValue()).isEqualTo("工作年限");
      assertThat(data.getCell(19).getStringCellValue()).contains("2019-07至至今,在测试公司从事电工工作");
      assertThat(data.getCell(20).getStringCellValue()).isEqualTo("某路1号");
      assertThat(data.getCell(21).getStringCellValue()).isEmpty();
      assertThat(data.getCell(22).getStringCellValue()).isEmpty();
      assertThat(data.getCell(23).getStringCellValue()).isEmpty();
      assertThat(data.getCell(24).getStringCellValue()).isEmpty();
    }
  }

  @Test
  void missingTips_listsRequiredEmptyFieldsLikePhp() {
    RosterPerson person = new RosterPerson();
    person.education = "本科";
    person.workTypePaperHas = 1;
    List<String> tips = support.missingTips(person);
    assertThat(tips).contains(
        "身份证正面", "身份证反面", "证件照", "毕业院校", "专业", "毕业证照片", "毕业证编号",
        "入学日期", "毕业日期", "从业日期", "本公司入职日期", "上一等级证书", "职称",
        "工作经历", "劳动合同第一页", "劳动合同期限页", "劳动合同盖章页");
  }
}
