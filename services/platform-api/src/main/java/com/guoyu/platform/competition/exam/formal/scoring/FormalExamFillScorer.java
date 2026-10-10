package com.guoyu.platform.competition.exam.formal.scoring;

import com.guoyu.platform.competition.entity.CompetitionFormalCandidateAnswerEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamQuestionSnapshotEntity;
import org.springframework.stereotype.Component;

@Component
public class FormalExamFillScorer implements FormalExamQuestionScorer {

  private final FormalExamChoiceScoringSupport support;

  public FormalExamFillScorer(FormalExamChoiceScoringSupport support) {
    this.support = support;
  }

  @Override
  public String questionType() {
    return "fill";
  }

  @Override
  public FormalExamQuestionScoreOutcome score(
      CompetitionFormalExamQuestionSnapshotEntity question,
      CompetitionFormalCandidateAnswerEntity answer) {
    return support.scoreFill(question, answer);
  }
}
