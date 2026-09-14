package com.guoyu.platform.trainingdepartment.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class SignUpPeopleDefaultTemplatePackServiceTest {

  @Test
  void classLevelOneMapsToSeniorTechnician() {
    assertThat(SignUpPeopleDefaultTemplatePackService.classLevelToReqIndex(1)).isEqualTo(4);
    assertThat(SignUpPeopleDefaultTemplatePackService.classLevelToReqIndex(5)).isEqualTo(0);
  }

  @Test
  void unsetPersonReqLevelFallsBackToClassLevelOne() {
    Integer req = SignUpPeopleDefaultTemplatePackService.resolveReqLevelIndex(
        0, null, null, 88L, 1);
    assertThat(req).isEqualTo(4);
  }

  @Test
  void copiedClassWorkTypeWithDefaultZeroStillUsesClassLevel() {
    Integer req = SignUpPeopleDefaultTemplatePackService.resolveReqLevelIndex(
        0, 88L, "电解槽操作工", 88L, 1);
    assertThat(req).isEqualTo(4);
  }

  @Test
  void classEncodingStoredAsReqLevelIsConverted() {
    Integer req = SignUpPeopleDefaultTemplatePackService.resolveReqLevelIndex(
        1, 88L, "电解槽操作工", 88L, 1);
    assertThat(req).isEqualTo(4);
  }

  @Test
  void explicitDifferentWorkTypeKeepsLevelZero() {
    Integer req = SignUpPeopleDefaultTemplatePackService.resolveReqLevelIndex(
        0, 99L, "焊工", 88L, 1);
    assertThat(req).isEqualTo(0);
  }

  @Test
  void commentRegisterChecksPromoteWhenHasPreviousCert() throws Exception {
    assertThat(SignUpPeopleDefaultTemplatePackService.hasPreviousLevelCertificate(null)).isFalse();
    TrainSignUpPeopleSelfEntity none = new TrainSignUpPeopleSelfEntity();
    assertThat(SignUpPeopleDefaultTemplatePackService.hasPreviousLevelCertificate(none)).isFalse();
    TrainSignUpPeopleSelfEntity withPaper = new TrainSignUpPeopleSelfEntity();
    withPaper.setPaperNo("ABC123");
    assertThat(SignUpPeopleDefaultTemplatePackService.hasPreviousLevelCertificate(withPaper)).isTrue();

    ClassPathResource resource = new ClassPathResource("template/职业技能评价登记表（2025新）.docx");
    try (InputStream in = resource.getInputStream()) {
      byte[] raw = in.readAllBytes();
      byte[] promote = SignUpPeopleDefaultTemplatePackService.applyCommentRegisterEvalChecks(raw, true);
      String promoteXml = TrainingDocxTemplateValidator.extractWordXml(promote);
      assertThat(promoteXml).contains("☑晋级评价");
      assertThat(promoteXml).contains("□初次评价");
      assertThat(promoteXml).doesNotContain("☑初次评价");

      byte[] first = SignUpPeopleDefaultTemplatePackService.applyCommentRegisterEvalChecks(raw, false);
      String firstXml = TrainingDocxTemplateValidator.extractWordXml(first);
      assertThat(firstXml).contains("☑初次评价");
      assertThat(firstXml).contains("□晋级评价");
      assertThat(firstXml).doesNotContain("☑晋级评价");
    }
  }

  @Test
  void inferZipFromPingguoAddress() {
    assertThat(RegionZipCodeSupport.inferFromAddresses("广西平果县新安镇布思村陆家屯33号"))
        .isEqualTo("531405");
    assertThat(RegionZipCodeSupport.inferFromAddresses("广西平果市铝城大道左一巷135号"))
        .isEqualTo("531400");
    assertThat(RegionZipCodeSupport.normalizeZip("531400")).isEqualTo("531400");
    assertThat(RegionZipCodeSupport.normalizeZip("邮编531400")).isEqualTo("531400");
  }

  @Test
  void resolveCompanyNamePrefersOrgOverPlanTitleStoredOnPerson() {
    String name = SignUpPeopleDefaultTemplatePackService.resolveCompanyName(
        "2026年强强碳素自费认定材料收集",
        "广西强强碳素股份有限公司",
        "广西强强碳素股份有限公司",
        "2026年强强碳素自费认定材料收集");
    assertThat(name).isEqualTo("广西强强碳素股份有限公司");
  }

  @Test
  void resolveCompanyNameFallsBackToWorkExpWhenOrgMissing() {
    String name = SignUpPeopleDefaultTemplatePackService.resolveCompanyName(
        "2026年强强碳素自费认定材料收集",
        "",
        "广西强强碳素股份有限公司",
        "2026年强强碳素自费认定材料收集");
    assertThat(name).isEqualTo("广西强强碳素股份有限公司");
  }

  @Test
  void resolveCompanyNameSkipsCollectionPlanTitle() {
    assertThat(SignUpPeopleDefaultTemplatePackService.looksLikeCollectionPlanTitle(
        "2026年强强碳素自费认定材料收集", "2026年强强碳素自费认定材料收集")).isTrue();
    String name = SignUpPeopleDefaultTemplatePackService.resolveCompanyName(
        "2026年强强碳素自费认定材料收集",
        "",
        "",
        "2026年强强碳素自费认定材料收集");
    assertThat(name).isEmpty();
  }

  @Test
  void optionalCurrentLevelBlankWhenDefaultZeroWithoutEvidence() {
    TrainSignUpPeopleSelfEntity person = new TrainSignUpPeopleSelfEntity();
    person.setLevel("0");
    assertThat(SignUpPeopleDefaultTemplatePackService.optionalCurrentLevelLabel(person)).isEmpty();
  }

  @Test
  void levelApplyFormFilledTextUsesSongTiXiaoSi() throws Exception {
    Map<String, String> vars = new LinkedHashMap<>();
    vars.put("name", "张三");
    vars.put("req_work_type", "电解槽操作工");
    vars.put("req_work_level", "四级/中级工");
    vars.put("graduate_date", "2022-06-01");
    ClassPathResource resource = new ClassPathResource("template/职业技能等级认定申请表.docx");
    try (InputStream in = resource.getInputStream()) {
      byte[] replaced = TrainingDocxVariableReplacer.replaceAll(in.readAllBytes(), vars);
      try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(replaced))) {
        SignUpPeopleDefaultTemplatePackService.applySongTiXiaoSiToLevelApplyForm(doc);
        boolean sawSong = false;
        boolean sawXiaoSi = false;
        for (XWPFTable table : doc.getTables()) {
          for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
              for (XWPFParagraph p : cell.getParagraphs()) {
                for (XWPFRun run : p.getRuns()) {
                  if ("张三".equals(run.text()) || "电解槽操作工".equals(run.text())) {
                    assertThat(run.getFontFamily()).isEqualTo("宋体");
                    assertThat(run.getFontSize()).isEqualTo(12);
                    sawSong = true;
                    sawXiaoSi = true;
                  }
                }
              }
            }
          }
        }
        assertThat(sawSong).isTrue();
        assertThat(sawXiaoSi).isTrue();
      }
    }
  }

  @Test
  void applyFormsReplaceManyPlaceholders() throws Exception {
    Map<String, String> vars = new LinkedHashMap<>();
    vars.put("req_work_type", "电解槽操作工");
    vars.put("req_work_level", "四级/中级工");
    vars.put("graduate_date", "2022-06-01");
    vars.put("com_phone", "17777611474");
    vars.put("com_address", "平果市");
    vars.put("com_zipcode", "531400");
    vars.put("workExp", "2019-04-19至今，在广西强强碳素从事人工装卸工作");
    vars.put("work_type", "");
    vars.put("work_type_level", "");
    ClassPathResource resource = new ClassPathResource("template/职业技能等级认定申请表.docx");
    try (InputStream in = resource.getInputStream()) {
      byte[] out = TrainingDocxVariableReplacer.replaceAll(in.readAllBytes(), vars);
      String xml = TrainingDocxTemplateValidator.extractWordXml(out);
      assertThat(xml).contains("电解槽操作工");
      assertThat(xml).contains("四级/中级工");
      assertThat(xml).contains("2022-06-01");
      assertThat(xml).contains("17777611474");
      assertThat(xml).contains("平果市");
      assertThat(xml).contains("2019-04-19至今");
      assertThat(xml).doesNotContain("${graduate_date}");
      assertThat(xml).doesNotContain("${com_phone}");
      assertThat(xml).doesNotContain("${workExp}");
      assertThat(xml).doesNotContain("${req_work_type}");
    }
  }

  @Test
  void applyFormsReplaceClassAppointmentPlaceholders() throws Exception {
    Map<String, String> vars = new LinkedHashMap<>();
    vars.put("req_work_type", "电解槽操作工");
    vars.put("req_work_level", "一级/高级技师");
    String[] templates = {
        "template/职业技能等级认定申请表.docx",
        "template/职业技能评价登记表（2025新）.docx",
        "template/广西壮族自治区职业技能鉴定登记表.docx"
    };
    for (String rel : templates) {
      ClassPathResource resource = new ClassPathResource(rel);
      assertThat(resource.exists()).as(rel).isTrue();
      try (InputStream in = resource.getInputStream()) {
        byte[] out = TrainingDocxVariableReplacer.replaceAll(in.readAllBytes(), vars);
        String xml = TrainingDocxTemplateValidator.extractWordXml(out);
        assertThat(xml).as(rel).contains("电解槽操作工");
        assertThat(xml).as(rel).contains("一级/高级技师");
        assertThat(xml).as(rel).doesNotContain("${req_work_type}");
        assertThat(xml).as(rel).doesNotContain("${req_work_level}");
      }
    }
  }
}
