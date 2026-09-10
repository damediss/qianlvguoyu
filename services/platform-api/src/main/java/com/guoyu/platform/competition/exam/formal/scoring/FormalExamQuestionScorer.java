package com.guoyu.platform.competition.exam.formal.scoring;

import com.guoyu.platform.competition.entity.CompetitionFormalCandidateAnswerEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamQuestionSnapshotEntity;

public interface FormalExamQuestionScorer {

  String questionType();

  FormalExamQuestionScoreOutcome score(
      CompetitionFormalExamQuestionSnapshotEntity question,
      CompetitionFormalCandidateAnswerEntity answer);
}
