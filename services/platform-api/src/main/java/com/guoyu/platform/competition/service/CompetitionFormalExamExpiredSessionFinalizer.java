package com.guoyu.platform.competition.service;

import com.guoyu.platform.competition.entity.CompetitionFormalExamSessionEntity;
import com.guoyu.platform.competition.enums.CompetitionFormalCandidateExamStatus;
import com.guoyu.platform.competition.exam.formal.FormalExamErrorCodes;
import com.guoyu.platform.competition.exam.formal.FormalExamSessionFinishRules;
import com.guoyu.platform.competition.exam.observability.FormalExamMetrics;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateExamMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CompetitionFormalExamExpiredSessionFinalizer {

  private final CompetitionFormalCandidateExamMapper candidateExamMapper;
  private final CompetitionFormalExamCandidateFinalizationService finalizationService;
  private final CompetitionFormalExamSessionFinishService finishService;
  private final int autoSubmitChunkSize;
  private final int autoSubmitMaxChunks;
  private final FormalExamMetrics metrics;

  public CompetitionFormalExamExpiredSessionFinalizer(
      CompetitionFormalCandidateExamMapper candidateExamMapper,
      CompetitionFormalExamCandidateFinalizationService finalizationService,
      CompetitionFormalExamSessionFinishService finishService,
      CompetitionFormalExamScoringService scoringService) {
    this(candidateExamMapper, finalizationService, finishService, 100, 5, null);
  }

  @Autowired
  public CompetitionFormalExamExpiredSessionFinalizer(
      CompetitionFormalCandidateExamMapper candidateExamMapper,
      CompetitionFormalExamCandidateFinalizationService finalizationService,
      CompetitionFormalExamSessionFinishService finishService,
      @Value("${platform.competition.formal-exam.auto-submit.chunk-size:100}") int autoSubmitChunkSize,
      @Value("${platform.competition.formal-exam.auto-submit.max-chunks-per-tick:5}") int autoSubmitMaxChunks,
      ObjectProvider<FormalExamMetrics> metrics) {
    this.candidateExamMapper = candidateExamMapper;
    this.finalizationService = finalizationService;
    this.finishService = finishService;
    this.autoSubmitChunkSize = Math.max(1, autoSubmitChunkSize);
    this.autoSubmitMaxChunks = Math.max(1, autoSubmitMaxChunks);
    this.metrics = metrics == null ? null : metrics.getIfAvailable();
  }

  public void finalizeExpiredSession(CompetitionFormalExamSessionEntity session, LocalDateTime now) {
    if (session == null || session.getId() == null || now == null) {
      return;
    }
    LocalDateTime deadline = FormalExamSessionFinishRules.deadlineTime(session);
    if (deadline == null || now.isBefore(deadline)) {
      return;
    }
    Instant started = Instant.now();
    int absent = markWaitingAbsent(session.getId(), deadline, now);
    int autoSubmitted = autoSubmitRunning(session.getId(), deadline, now);
    int ready = candidateExamMapper.countBySessionAndStatus(
        session.getId(), CompetitionFormalCandidateExamStatus.READY.getCode());
    if (ready > 0) {
      log.error(
          "Formal exam runtime anomaly: session running but candidates still ready, sessionId={}, readyCount={}",
          session.getId(), ready);
    }
    finishService.finishIfEligible(session.getId(), deadline, now);
    if (metrics != null) {
      metrics.recordFinalize("expire", absent + autoSubmitted, Duration.between(started, Instant.now()));
    }
  }

  private int autoSubmitRunning(Long sessionId, LocalDateTime deadline, LocalDateTime now) {
    int converted = 0;
    for (int chunk = 0; chunk < autoSubmitMaxChunks; chunk++) {
      int moved = finalizationService.autoSubmitRunningChunk(
          sessionId, deadline, now, autoSubmitChunkSize);
      converted += moved;
      if (moved < autoSubmitChunkSize) {
        break;
      }
    }
    return converted;
  }

  private int markWaitingAbsent(Long sessionId, LocalDateTime deadline, LocalDateTime now) {
    int updated = candidateExamMapper.markAbsentIfWaitingEnterBySession(
        sessionId,
        CompetitionFormalCandidateExamStatus.WAITING_ENTER.getCode(),
        CompetitionFormalCandidateExamStatus.ABSENT.getCode(),
        deadline,
        FormalExamErrorCodes.ABSENT_REASON_DEADLINE_NOT_ENTERED,
        now);
    if (updated > 0) {
      log.warn("Formal exam waiting candidates marked absent, sessionId={}, count={}",
          sessionId, updated);
    }
    return Math.max(0, updated);
  }
}
