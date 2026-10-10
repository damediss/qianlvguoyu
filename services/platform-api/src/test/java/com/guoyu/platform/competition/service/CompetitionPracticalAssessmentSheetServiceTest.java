package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CompetitionPracticalAssessmentSheetServiceTest {

  @Test
  void assessmentSheetCodeUsesPaperSnapshotIdentityWhenPaperCodesRepeat() {
    assertThat(CompetitionPracticalAssessmentSheetService.buildSheetCode("A", 1001L))
        .isEqualTo("A-KP-1001");
    assertThat(CompetitionPracticalAssessmentSheetService.buildSheetCode("A", 1002L))
        .isEqualTo("A-KP-1002");
  }

  @Test
  void missingPaperCodeStillProducesDeterministicDistinctCode() {
    assertThat(CompetitionPracticalAssessmentSheetService.buildSheetCode("", 1001L))
        .isEqualTo("1001-KP-1001");
  }
}
