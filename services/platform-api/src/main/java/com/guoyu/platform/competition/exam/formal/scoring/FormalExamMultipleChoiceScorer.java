package com.guoyu.platform.competition.exam.formal.scoring;

import com.guoyu.platform.competition.entity.CompetitionFormalCandidateAnswerEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamQuestionSnapshotEntity;
import org.springframework.stereotype.Component;

@Component
public class FormalExamMultipleChoiceScorer implements FormalExamQuestionScorer {

  private final FormalExamChoiceScoringSupport support;

  public FormalExamMultipleChoiceScorer(FormalExamChoiceScoringSupport support) {
    this.support = support;
  }

  @Override
  public String questionType() {
    return "multi";
  }

  @Override
  public FormalExamQuestionScoreOutcome score(
      CompetitionFormalExamQuestionSnapshotEntity question,
      CompetitionFormalCandidateAnswerEntity answer) {
    return support.scoreSet(question, answer, 2, Integer.MAX_VALUE);
  }
}
