package com.guoyu.platform.competition.service;

import com.guoyu.platform.competition.entity.CompetitionFormalExamSessionEntity;
import com.guoyu.platform.competition.enums.CompetitionFormalCandidateExamStatus;
import com.guoyu.platform.competition.enums.CompetitionFormalExamSessionStatus;
import com.guoyu.platform.competition.exam.formal.FormalExamSessionFinishRules;
import com.guoyu.platform.competition.exam.formal.FormalExamSessionFinishRules.CandidateCounts;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateExamMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamSessionMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalStatusCountRow;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionFormalExamSessionFinishService {

  private final CompetitionFormalExamSessionMapper sessionMapper;
  private final CompetitionFormalCandidateExamMapper candidateExamMapper;

  public CompetitionFormalExamSessionFinishService(
      CompetitionFormalExamSessionMapper sessionMapper,
      CompetitionFormalCandidateExamMapper candidateExamMapper) {
    this.sessionMapper = sessionMapper;
    this.candidateExamMapper = candidateExamMapper;
  }

  @Transactional
  public boolean finishIfEligible(Long sessionId, LocalDateTime deadline, LocalDateTime now) {
    if (sessionId == null || deadline == null || now == null) {
      return false;
    }
    CompetitionFormalExamSessionEntity locked = sessionMapper.selectByIdForUpdate(sessionId);
    if (locked == null) {
      return false;
    }
    if (CompetitionFormalExamSessionStatus.FINISHED.getCode().equals(locked.getStatus())) {
      return false;
    }
    if (!CompetitionFormalExamSessionStatus.RUNNING.getCode().equals(locked.getStatus())) {
      return false;
    }
    if (now.isBefore(deadline)) {
      return false;
    }
    CandidateCounts counts = countsOf(candidateExamMapper.countStatusByExamSessionId(sessionId));
    if (!FormalExamSessionFinishRules.finishEligible(locked, counts, now)) {
      if (counts.ready() > 0) {
        log.error(
            "Formal exam session cannot finish because READY candidates remain, sessionId={}, readyCount={}",
            sessionId, counts.ready());
      }
      if (counts.total() != (locked.getCandidateTotal() == null ? -1 : locked.getCandidateTotal())) {
        log.error(
            "Formal exam session cannot finish because candidate totals mismatch, sessionId={}, recorded={}, actual={}",
            sessionId, locked.getCandidateTotal(), counts.total());
      }
      return false;
    }
    int updated = sessionMapper.markFinishedIfRunning(
        sessionId,
        CompetitionFormalExamSessionStatus.RUNNING.getCode(),
        CompetitionFormalExamSessionStatus.FINISHED.getCode(),
        deadline,
        sessionId,
        now);
    if (updated == 1) {
      log.warn(
          "Formal exam session finished, sessionId={}, actualEndTime={}, occupancyToken={}",
          sessionId, deadline, sessionId);
      return true;
    }
    return false;
  }

  private static CandidateCounts countsOf(List<CompetitionFormalStatusCountRow> rows) {
    int waiting = 0;
    int ready = 0;
    int running = 0;
    int submitted = 0;
    int autoSubmitted = 0;
    int forcedSubmitted = 0;
    int absent = 0;
    int total = 0;
    if (rows != null) {
      for (CompetitionFormalStatusCountRow row : rows) {
        if (row == null) {
          continue;
        }
        int count = row.totalOrZero();
        total += count;
        CompetitionFormalCandidateExamStatus status =
            CompetitionFormalCandidateExamStatus.fromCode(row.getStatus());
        if (status == CompetitionFormalCandidateExamStatus.WAITING_ENTER) {
          waiting += count;
        } else if (status == CompetitionFormalCandidateExamStatus.READY) {
          ready += count;
        } else if (status == CompetitionFormalCandidateExamStatus.RUNNING) {
          running += count;
        } else if (status == CompetitionFormalCandidateExamStatus.SUBMITTED) {
          submitted += count;
        } else if (status == CompetitionFormalCandidateExamStatus.AUTO_SUBMITTED) {
          autoSubmitted += count;
        } else if (status == CompetitionFormalCandidateExamStatus.FORCED_SUBMITTED) {
          forcedSubmitted += count;
        } else if (status == CompetitionFormalCandidateExamStatus.ABSENT) {
          absent += count;
        }
      }
    }
    return new CandidateCounts(
        waiting, ready, running, submitted, autoSubmitted, forcedSubmitted, absent, total);
  }
}
