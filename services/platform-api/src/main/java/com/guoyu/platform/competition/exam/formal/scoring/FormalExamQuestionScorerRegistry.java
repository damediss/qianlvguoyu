package com.guoyu.platform.competition.exam.formal.scoring;

import com.guoyu.platform.competition.entity.CompetitionFormalCandidateAnswerEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamQuestionSnapshotEntity;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class FormalExamQuestionScorerRegistry {

  private final Map<String, FormalExamQuestionScorer> scorers;

  public FormalExamQuestionScorerRegistry(List<FormalExamQuestionScorer> scorers) {
    this.scorers = scorers.stream().collect(Collectors.toUnmodifiableMap(
        FormalExamQuestionScorer::questionType, Function.identity()));
  }

  public FormalExamQuestionScoreOutcome score(
      CompetitionFormalExamQuestionSnapshotEntity question,
      CompetitionFormalCandidateAnswerEntity answer) {
    if (question == null || question.getId() == null) {
      throw new FormalExamScoringException("正式题目快照不存在");
    }
    String type = FormalExamChoiceScoringSupport.normalizeType(question.getQuestionType());
    FormalExamQuestionScorer scorer = scorers.get(type);
    if (scorer == null) {
      throw new FormalExamScoringException(
          "未知题型无法自动评分: " + question.getQuestionType(),
          question.getExamSessionId(), null, question.getId());
    }
    return scorer.score(question, answer);
  }
}
