package com.guoyu.platform.trainingdepartment.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.guoyu.platform.trainingdepartment.support.TeacherCertTextParser.ParsedCerts;
import org.junit.jupiter.api.Test;

class TeacherCertTextParserTest {

  @Test
  void parseTeacherQualGlued() {
    ParsedCerts p = TeacherCertTextParser.parse("教师资格证20074500171100449");
    assertThat(p.teacherQualNo()).isEqualTo("20074500171100449");
  }

  @Test
  void parseMultiLineMixed() {
    ParsedCerts p =
        TeacherCertTextParser.parse(
            "职业技能等级证书\nS000045001009232005054\n职称证书\nGX22023009130");
    assertThat(p.skillCertNo()).contains("S000045001009232005054");
    assertThat(p.titleCertNo()).isEqualTo("GX22023009130");
  }
}
