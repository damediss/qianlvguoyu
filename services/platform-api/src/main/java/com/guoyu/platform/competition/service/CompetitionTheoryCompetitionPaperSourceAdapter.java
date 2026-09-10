package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPaperQuestionEntity;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPaperSnapshotEntity;
import com.guoyu.platform.competition.mapper.CompetitionMakeExamPaperQuestionMapper;
import com.guoyu.platform.competition.mapper.CompetitionMakeExamPaperSnapshotMapper;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads the independent competition make-exam snapshots used by the competition execution chain.
 *
 * <p>The legacy {@link CompetitionTheoryPaperSourceAdapter} deliberately remains course-backed
 * for historical imports. New competition paper imports use this adapter and therefore never
 * query the course question or course make-exam tables.</p>
 */
@Component
@Slf4j
public class CompetitionTheoryCompetitionPaperSourceAdapter {

  private final CompetitionMakeExamPaperSnapshotMapper snapshotMapper;
  private final CompetitionMakeExamPaperQuestionMapper questionMapper;

  public CompetitionTheoryCompetitionPaperSourceAdapter(
      CompetitionMakeExamPaperSnapshotMapper snapshotMapper,
      CompetitionMakeExamPaperQuestionMapper questionMapper) {
    this.snapshotMapper = snapshotMapper;
    this.questionMapper = questionMapper;
  }

  @Transactional(readOnly = true)
  public SourcePaper loadPublishedPaper(
      Long tenantId,
      Long eventId,
      Long eventWorkTypeId,
      Long eventWorkTypeLevelId,
      Long paperSnapshotId) {
    CompetitionMakeExamPaperSnapshotEntity snapshot = snapshotMapper.selectOne(
        new LambdaQueryWrapper<CompetitionMakeExamPaperSnapshotEntity>()
            .eq(CompetitionMakeExamPaperSnapshotEntity::getTenantId, tenantId)
            .eq(CompetitionMakeExamPaperSnapshotEntity::getId, paperSnapshotId)
            .eq(CompetitionMakeExamPaperSnapshotEntity::getEventId, eventId)
            .eq(CompetitionMakeExamPaperSnapshotEntity::getEventWorkTypeId, eventWorkTypeId)
            .eq(CompetitionMakeExamPaperSnapshotEntity::getEventWorkTypeLevelId,
                eventWorkTypeLevelId)
            .eq(CompetitionMakeExamPaperSnapshotEntity::getContentType, "theory")
            .eq(CompetitionMakeExamPaperSnapshotEntity::getStatus, "published")
            .eq(CompetitionMakeExamPaperSnapshotEntity::getDeleted, 0));
    if (snapshot == null) {
      throw new BusinessException(
          ErrorCode.UNPROCESSABLE_ENTITY, "竞赛组卷快照未发布或与当前赛事工种等级不匹配");
    }
    List<CompetitionMakeExamPaperQuestionEntity> questions =
        questionMapper.selectByPaper(tenantId, snapshot.getId());
    if (questions == null || questions.isEmpty()) {
      log.warn("Competition paper snapshot has no questions, tenantId={}, snapshotId={}",
          tenantId, paperSnapshotId);
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "竞赛组卷快照没有可用题目");
    }
    return new SourcePaper(snapshot, Collections.unmodifiableList(questions));
  }

  public record SourcePaper(
      CompetitionMakeExamPaperSnapshotEntity snapshot,
      List<CompetitionMakeExamPaperQuestionEntity> questions) {
  }
}
