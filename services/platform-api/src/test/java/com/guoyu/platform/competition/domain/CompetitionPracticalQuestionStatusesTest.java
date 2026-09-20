package com.guoyu.platform.competition.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.guoyu.platform.competition.entity.CompetitionPracticalScoreEntryEntity;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompetitionPracticalQuestionStatusesTest {
  @Test
  void openedSheetWithoutSubmitStaysScoring() {
    CompetitionPracticalScoreEntryEntity entry = new CompetitionPracticalScoreEntryEntity();
    entry.setStatus("scoring");
    assertThat(CompetitionPracticalQuestionStatuses.ofQuestion("draft", List.of(entry)))
        .isEqualTo("scoring");
    assertThat(CompetitionPracticalQuestionStatuses.ofQuestion("draft", List.of()))
        .isEqualTo("scoring");
  }

  @Test
  void submittedEntriesAndLockedSheetAreSubmitted() {
    CompetitionPracticalScoreEntryEntity entry = new CompetitionPracticalScoreEntryEntity();
    entry.setStatus("submitted");
    assertThat(CompetitionPracticalQuestionStatuses.ofQuestion("draft", List.of(entry)))
        .isEqualTo("submitted");
    assertThat(CompetitionPracticalQuestionStatuses.ofQuestion("submitted_locked", List.of()))
        .isEqualTo("submitted");
  }
}
