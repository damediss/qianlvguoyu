package com.guoyu.platform.competition.domain;

import com.guoyu.platform.competition.entity.CompetitionPracticalScoreEntryEntity;
import java.util.List;

public final class CompetitionPracticalQuestionStatuses {
  public static final String UNOPENED = "unopened";
  public static final String SCORING = "scoring";
  public static final String SUBMITTED = "submitted";
  public static final String RETURNED = "returned";

  private CompetitionPracticalQuestionStatuses() { }

  public static String ofQuestion(
      String sheetStatus, List<CompetitionPracticalScoreEntryEntity> questionEntries) {
    if (sheetStatus == null) {
      return UNOPENED;
    }
    if ("submitted_locked".equals(sheetStatus)) {
      return SUBMITTED;
    }
    List<CompetitionPracticalScoreEntryEntity> rows =
        questionEntries == null ? List.of() : questionEntries;
    boolean submitted = !rows.isEmpty() && rows.stream().allMatch(CompetitionPracticalQuestionStatuses::submitted);
    if ("unlocked_for_rescore".equals(sheetStatus) && !submitted) {
      return RETURNED;
    }
    return submitted ? SUBMITTED : SCORING;
  }

  public static boolean submitted(CompetitionPracticalScoreEntryEntity entry) {
    return entry != null && SUBMITTED.equals(entry.getStatus());
  }
}
