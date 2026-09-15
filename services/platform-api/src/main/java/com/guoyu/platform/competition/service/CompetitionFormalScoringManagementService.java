package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageQuery;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateResultEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateExamEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamSessionEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalScoringTaskEntity;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateResultMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalScoringTaskMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateExamMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamSessionMapper;
import com.guoyu.platform.competition.vo.CompetitionFormalScoringTaskVO;
import com.guoyu.platform.competition.vo.CompetitionFormalResultRevisionVO;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompetitionFormalScoringManagementService {
  private final CompetitionFormalScoringTaskMapper taskMapper;
  private final CompetitionFormalCandidateResultMapper resultMapper;
  private final CompetitionFormalScoringTaskService taskService;
  private final CompetitionFormalCandidateExamMapper candidateMapper;
  private final CompetitionFormalExamSessionMapper sessionMapper;

  public CompetitionFormalScoringManagementService(CompetitionFormalScoringTaskMapper taskMapper,
      CompetitionFormalCandidateResultMapper resultMapper,
      CompetitionFormalScoringTaskService taskService,
      CompetitionFormalCandidateExamMapper candidateMapper,
      CompetitionFormalExamSessionMapper sessionMapper) {
    this.taskMapper = taskMapper;
    this.resultMapper = resultMapper;
    this.taskService = taskService;
    this.candidateMapper = candidateMapper;
    this.sessionMapper = sessionMapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<CompetitionFormalScoringTaskVO> tasks(Long sessionId, Long eventId,
      Long stageId, Long eventWorkTypeId, Long eventWorkTypeLevelId, String status,
      PageQuery query) {
    List<Long> sessionIds = null;
    if (sessionId == null && eventId != null) {
      List<CompetitionFormalExamSessionEntity> sessions = sessionMapper.selectList(
          new LambdaQueryWrapper<CompetitionFormalExamSessionEntity>()
              .eq(CompetitionFormalExamSessionEntity::getEventId, eventId)
              .eq(stageId != null, CompetitionFormalExamSessionEntity::getStageId, stageId)
              .eq(eventWorkTypeId != null,
                  CompetitionFormalExamSessionEntity::getEventWorkTypeId, eventWorkTypeId)
              .eq(eventWorkTypeLevelId != null,
                  CompetitionFormalExamSessionEntity::getEventWorkTypeLevelId,
                  eventWorkTypeLevelId)
              .eq(CompetitionFormalExamSessionEntity::getDeleted, 0));
      sessionIds = sessions == null ? List.of()
          : sessions.stream().map(CompetitionFormalExamSessionEntity::getId).toList();
      if (sessionIds.isEmpty()) {
        return PageResponse.empty(query.getPage(), query.getPageSize());
      }
    }
    Page<CompetitionFormalScoringTaskEntity> page = new Page<>(query.getPage(), query.getPageSize());
    List<Long> scopedSessionIds = sessionIds;
    Page<CompetitionFormalScoringTaskEntity> result = taskMapper.selectPage(page,
        new LambdaQueryWrapper<CompetitionFormalScoringTaskEntity>()
            .eq(sessionId != null, CompetitionFormalScoringTaskEntity::getExamSessionId, sessionId)
            .in(sessionId == null && scopedSessionIds != null,
                CompetitionFormalScoringTaskEntity::getExamSessionId, scopedSessionIds)
            .eq(status != null && !status.isBlank(), CompetitionFormalScoringTaskEntity::getTaskStatus, status)
            .eq(CompetitionFormalScoringTaskEntity::getDeleted, 0)
            .orderByDesc(CompetitionFormalScoringTaskEntity::getCreatedAt));
    List<CompetitionFormalScoringTaskVO> items = result.getRecords() == null ? List.of()
        : result.getRecords().stream().map(this::toVO).toList();
    return new PageResponse<>(items, result.getCurrent(), result.getSize(), result.getTotal());
  }

  @Transactional(readOnly = true)
  public List<CompetitionFormalResultRevisionVO> history(Long candidateExamId) {
    List<CompetitionFormalCandidateResultEntity> rows =
        resultMapper.selectHistoryByCandidateExamId(candidateExamId);
    return rows == null ? List.of() : rows.stream().map(row ->
        new CompetitionFormalResultRevisionVO(String.valueOf(row.getId()),
            String.valueOf(row.getExamSessionId()), String.valueOf(row.getCandidateExamId()),
            row.getRevisionNo(), Integer.valueOf(1).equals(row.getCurrentFlag()),
            row.getResultStatus(), row.getTotalScore(), row.getFullScore(),
            row.getScoreVersion(), row.getCorrectionReason(), row.getScoredTime())).toList();
  }

  @Transactional
  public CompetitionFormalScoringTaskVO retry(Long taskId, Long userId, String reason) {
    return toVO(taskService.retryFailed(taskId, userId, reason));
  }

  @Transactional
  public CompetitionFormalScoringTaskVO rescore(Long candidateExamId, Long userId, String reason) {
    List<CompetitionFormalCandidateResultEntity> history =
        resultMapper.selectHistoryByCandidateExamId(candidateExamId);
    int revision = history == null || history.isEmpty() ? 1
        : history.stream().map(CompetitionFormalCandidateResultEntity::getRevisionNo)
            .filter(java.util.Objects::nonNull).max(Integer::compareTo).orElse(0) + 1;
    if (revision <= 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "首次判分应通过交卷任务触发");
    }
    return toVO(taskService.enqueueRevision(candidateExamId, revision, reason, userId));
  }

  private CompetitionFormalScoringTaskVO toVO(CompetitionFormalScoringTaskEntity task) {
    CompetitionFormalCandidateExamEntity candidate = candidateMapper.selectById(task.getCandidateExamId());
    return new CompetitionFormalScoringTaskVO(String.valueOf(task.getId()),
        String.valueOf(task.getExamSessionId()), String.valueOf(task.getCandidateExamId()),
        candidate == null || candidate.getContestantMemberId() == null
            ? null : String.valueOf(candidate.getContestantMemberId()),
        task.getResultRevisionNo(), candidate == null ? null : candidate.getCandidateNo(),
        candidate == null ? null : candidate.getCandidateName(),
        task.getTaskStatus(), task.getAttemptCount(),
        task.getNextAttemptAt(), task.getLeaseUntil(), task.getLastError(),
        task.getRequestedReason(), task.getStartedAt(), task.getCompletedAt(),
        candidate == null ? null : candidate.getStatus());
  }
}
