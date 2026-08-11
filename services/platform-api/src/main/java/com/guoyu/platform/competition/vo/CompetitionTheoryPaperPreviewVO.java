package com.guoyu.platform.competition.vo;

import com.guoyu.platform.competition.entity.CompetitionTheoryPaperSnapshotEntity;
import java.math.BigDecimal;
import java.util.List;

public record CompetitionTheoryPaperPreviewVO(
    Long id,
    String paperCode,
    Integer versionNo,
    String sourceTitle,
    Integer durationMinutes,
    Integer questionCount,
    BigDecimal totalScore,
    List<CompetitionTheoryPaperPreviewQuestionVO> questions
) {

  public static CompetitionTheoryPaperPreviewVO from(
      CompetitionTheoryPaperSnapshotEntity snapshot,
      List<CompetitionTheoryPaperPreviewQuestionVO> questions) {
    return new CompetitionTheoryPaperPreviewVO(
        snapshot.getId(),
        snapshot.getPaperCode(),
        snapshot.getVersionNo(),
        snapshot.getSourceTitle(),
        snapshot.getDurationMinutes(),
        snapshot.getQuestionCount(),
        snapshot.getTotalScore(),
        questions);
  }
}
