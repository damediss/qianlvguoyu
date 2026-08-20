package com.guoyu.platform.trainingdepartment.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
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
  void applyFormsReplaceClassAppointmentPlaceholders() throws Exception {
    Map<String, String> vars = new LinkedHashMap<>();
    vars.put("req_work_type", "电解槽操作工");
    vars.put("req_work_level", "高级技师/一级");
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
        assertThat(xml).as(rel).contains("高级技师/一级");
        assertThat(xml).as(rel).doesNotContain("${req_work_type}");
        assertThat(xml).as(rel).doesNotContain("${req_work_level}");
      }
    }
  }
}
