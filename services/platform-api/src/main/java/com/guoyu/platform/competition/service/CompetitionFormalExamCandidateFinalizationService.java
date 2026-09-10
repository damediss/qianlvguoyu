package com.guoyu.platform.competition.service;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateExamEntity;
import com.guoyu.platform.competition.enums.CompetitionFormalCandidateExamStatus;
import com.guoyu.platform.competition.exam.formal.FormalExamErrorCodes;
import com.guoyu.platform.competition.exam.realtime.FormalExamRealtimePublisher;
import com.guoyu.platform.competition.mapper.CompetitionFormalAnsweredCountRow;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateAnswerMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateExamMapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class CompetitionFormalExamCandidateFinalizationService {

  private final CompetitionFormalCandidateExamMapper candidateExamMapper;
  private final CompetitionFormalCandidateAnswerMapper answerMapper;
  private FormalExamRealtimePublisher realtimePublisher;

  public CompetitionFormalExamCandidateFinalizationService(
      CompetitionFormalCandidateExamMapper candidateExamMapper,
      CompetitionFormalCandidateAnswerMapper answerMapper) {
    this.candidateExamMapper = candidateExamMapper;
    this.answerMapper = answerMapper;
  }

  @Autowired(required = false)
  public void setRealtimePublisher(FormalExamRealtimePublisher realtimePublisher) {
    this.realtimePublisher = realtimePublisher;
  }

  @Transactional
  public CompetitionFormalCandidateExamEntity autoSubmitIfRunning(
      Long candidateExamId, LocalDateTime deadlineTime, LocalDateTime updatedAt) {
    return finalizeIfRunning(
        candidateExamId,
        CompetitionFormalCandidateExamStatus.AUTO_SUBMITTED,
        FormalExamErrorCodes.SUBMIT_TYPE_AUTO,
        deadlineTime,
        updatedAt,
        null,
        null,
        null);
  }

  @Transactional
  public int autoSubmitRunningChunk(
      Long examSessionId, LocalDateTime deadlineTime, LocalDateTime updatedAt, int limit) {
    if (examSessionId == null || limit <= 0) {
      return 0;
    }
    List<Long> runningIds = candidateExamMapper.selectIdsBySessionAndStatusLimit(
        examSessionId, CompetitionFormalCandidateExamStatus.RUNNING.getCode(), limit);
    if (runningIds == null || runningIds.isEmpty()) {
      return 0;
    }
    Map<Long, Integer> answeredByCandidate = new HashMap<>();
    List<CompetitionFormalAnsweredCountRow> counted =
        answerMapper.countAnsweredByCandidateIds(runningIds);
    if (counted != null) {
      for (CompetitionFormalAnsweredCountRow row : counted) {
        if (row != null && row.getCandidateExamId() != null) {
          answeredByCandidate.put(row.getCandidateExamId(), row.totalOrZero());
        }
      }
    }
    int converted = 0;
    LocalDateTime submitTime = deadlineTime == null ? updatedAt : deadlineTime;
    for (Long candidateExamId : runningIds) {
      if (candidateExamId == null) {
        continue;
      }
      int answeredCount = answeredByCandidate.getOrDefault(candidateExamId, 0);
      int updated = candidateExamMapper.markSubmittedIfRunning(
          candidateExamId,
          CompetitionFormalCandidateExamStatus.RUNNING.getCode(),
          CompetitionFormalCandidateExamStatus.AUTO_SUBMITTED.getCode(),
          submitTime,
          FormalExamErrorCodes.SUBMIT_TYPE_AUTO,
          answeredCount,
          updatedAt == null ? submitTime : updatedAt,
          null);
      if (updated == 1) {
        converted++;
        CompetitionFormalCandidateExamEntity latest = candidateExamMapper.selectById(candidateExamId);
        publishCandidateStatus(latest, CompetitionFormalCandidateExamStatus.RUNNING.getCode());
      }
    }
    if (converted > 0) {
      log.warn("Formal exam auto-submit chunk, sessionId={}, converted={}, scanned={}",
          examSessionId, converted, runningIds.size());
    }
    return converted;
  }

  @Transactional
  public CompetitionFormalCandidateExamEntity forceSubmitIfRunning(
      Long candidateExamId,
      LocalDateTime submitTime,
      Long operatorId,
      String reason) {
    if (!StringUtils.hasText(reason) || reason.trim().length() > 500) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "强制交卷原因长度为 1～500 字");
    }
    return finalizeIfRunning(
        candidateExamId,
        CompetitionFormalCandidateExamStatus.FORCED_SUBMITTED,
        FormalExamErrorCodes.SUBMIT_TYPE_FORCED,
        submitTime,
        submitTime,
        operatorId,
        reason.trim(),
        operatorId);
  }

  @Transactional
  public CompetitionFormalCandidateExamEntity markAbsentIfWaitingEnter(
      Long candidateExamId, LocalDateTime absentTime, LocalDateTime updatedAt) {
    if (candidateExamId == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "选手考试实例不能为空");
    }
    if (absentTime == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "缺考确认时间不能为空");
    }
    CompetitionFormalCandidateExamEntity locked = candidateExamMapper.selectByIdForUpdate(candidateExamId);
    if (locked == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "选手考试实例不存在");
    }
    CompetitionFormalCandidateExamStatus current =
        CompetitionFormalCandidateExamStatus.fromCode(locked.getStatus());
    if (current == CompetitionFormalCandidateExamStatus.ABSENT) {
      return locked;
    }
    if (current != CompetitionFormalCandidateExamStatus.WAITING_ENTER) {
      return locked;
    }
    int updated = candidateExamMapper.markAbsentIfWaitingEnter(
        candidateExamId,
        CompetitionFormalCandidateExamStatus.WAITING_ENTER.getCode(),
        CompetitionFormalCandidateExamStatus.ABSENT.getCode(),
        absentTime,
        FormalExamErrorCodes.ABSENT_REASON_DEADLINE_NOT_ENTERED,
        updatedAt == null ? absentTime : updatedAt);
    if (updated == 1) {
      CompetitionFormalCandidateExamEntity latest = candidateExamMapper.selectById(candidateExamId);
      if (latest == null) {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "缺考归档后未能读取考试实例");
      }
      log.warn("Formal exam candidate marked absent, candidateExamId={}, absentTime={}",
          candidateExamId, absentTime);
      publishCandidateStatus(latest, CompetitionFormalCandidateExamStatus.WAITING_ENTER.getCode());
      return latest;
    }
    CompetitionFormalCandidateExamEntity latest = candidateExamMapper.selectByIdForUpdate(candidateExamId);
    if (latest == null) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "缺考归档后未能读取考试实例");
    }
    return latest;
  }

  private CompetitionFormalCandidateExamEntity finalizeIfRunning(
      Long candidateExamId,
      CompetitionFormalCandidateExamStatus toStatus,
      String submitType,
      LocalDateTime submitTime,
      LocalDateTime updatedAt,
      Long updatedBy,
      String forceReason,
      Long forceSubmittedBy) {
    if (candidateExamId == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "选手考试实例不能为空");
    }
    CompetitionFormalCandidateExamEntity locked = candidateExamMapper.selectByIdForUpdate(candidateExamId);
    if (locked == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "选手考试实例不存在");
    }
    CompetitionFormalCandidateExamStatus current =
        CompetitionFormalCandidateExamStatus.fromCode(locked.getStatus());
    if (current != null && current.isTerminal()) {
      return locked;
    }
    if (current != CompetitionFormalCandidateExamStatus.RUNNING) {
      return locked;
    }
    int answeredCount = answerMapper.countAnswered(candidateExamId);
    int updated;
    if (toStatus == CompetitionFormalCandidateExamStatus.FORCED_SUBMITTED) {
      updated = candidateExamMapper.markForcedIfRunning(
          candidateExamId,
          CompetitionFormalCandidateExamStatus.RUNNING.getCode(),
          toStatus.getCode(),
          submitTime,
          submitType,
          answeredCount,
          forceReason,
          forceSubmittedBy,
          updatedAt,
          updatedBy);
    } else {
      updated = candidateExamMapper.markSubmittedIfRunning(
          candidateExamId,
          CompetitionFormalCandidateExamStatus.RUNNING.getCode(),
          toStatus.getCode(),
          submitTime,
          submitType,
          answeredCount,
          updatedAt,
          updatedBy);
    }
    if (updated == 1) {
      CompetitionFormalCandidateExamEntity latest = candidateExamMapper.selectById(candidateExamId);
      if (latest == null) {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "交卷后未能读取考试实例");
      }
      log.warn("Formal exam candidate finalized, candidateExamId={}, status={}, submitType={}, answered={}",
          candidateExamId, toStatus.getCode(), submitType, answeredCount);
      publishCandidateStatus(latest, CompetitionFormalCandidateExamStatus.RUNNING.getCode());
      return latest;
    }
    CompetitionFormalCandidateExamEntity latest = candidateExamMapper.selectByIdForUpdate(candidateExamId);
    if (latest == null) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "交卷后未能读取考试实例");
    }
    return latest;
  }

  private void publishCandidateStatus(CompetitionFormalCandidateExamEntity entity, String previousStatus) {
    if (realtimePublisher == null || entity == null) {
      return;
    }
    realtimePublisher.candidateStatus(
        entity.getTenantId(),
        entity.getExamSessionId(),
        entity.getId(),
        null,
        null,
        previousStatus,
        entity.getStatus());
  }
}
