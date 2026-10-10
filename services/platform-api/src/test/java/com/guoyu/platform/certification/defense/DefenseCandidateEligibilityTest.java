package com.guoyu.platform.certification.defense;

import static org.assertj.core.api.Assertions.assertThat;

import com.guoyu.platform.certification.entity.CertificationCandidateEntity;
import org.junit.jupiter.api.Test;

class DefenseCandidateEligibilityTest {

  @Test
  void acceptsTechnicianAndSeniorTechnicianCodes() {
    assertThat(DefenseCandidateEligibility.isEligibleLevel("technician", "技师")).isTrue();
    assertThat(DefenseCandidateEligibility.isEligibleLevel("senior_technician", "高级技师")).isTrue();
    assertThat(DefenseCandidateEligibility.isEligibleLevel("TECHNICIAN", null)).isTrue();
    assertThat(DefenseCandidateEligibility.isEligibleLevel("3", null)).isTrue();
    assertThat(DefenseCandidateEligibility.isEligibleLevel("4", null)).isTrue();
  }

  @Test
  void acceptsNationalLevelNames() {
    assertThat(DefenseCandidateEligibility.isEligibleLevel(null, "二级/技师")).isTrue();
    assertThat(DefenseCandidateEligibility.isEligibleLevel(null, "一级/高级技师")).isTrue();
    assertThat(DefenseCandidateEligibility.isEligibleLevel(null, "技师")).isTrue();
    assertThat(DefenseCandidateEligibility.isEligibleLevel(null, "高级技师")).isTrue();
  }

  @Test
  void rejectsLowerSkillLevels() {
    assertThat(DefenseCandidateEligibility.isEligibleLevel("junior", "初级")).isFalse();
    assertThat(DefenseCandidateEligibility.isEligibleLevel("intermediate", "中级")).isFalse();
    assertThat(DefenseCandidateEligibility.isEligibleLevel("senior", "高级")).isFalse();
    assertThat(DefenseCandidateEligibility.isEligibleLevel(null, "高级工")).isFalse();
    assertThat(DefenseCandidateEligibility.isEligibleLevel(null, "三级/高级工")).isFalse();
    assertThat(DefenseCandidateEligibility.isEligibleLevel(null, null)).isFalse();
  }

  @Test
  void candidateWithoutLevelIsNotEligible() {
    CertificationCandidateEntity candidate = new CertificationCandidateEntity();
    candidate.setStatus("active");
    assertThat(DefenseCandidateEligibility.isEligible(candidate)).isFalse();
  }
}
