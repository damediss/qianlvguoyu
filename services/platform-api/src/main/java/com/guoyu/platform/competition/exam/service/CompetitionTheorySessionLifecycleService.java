package com.guoyu.platform.competition.exam.service;

import com.guoyu.platform.competition.mapper.CompetitionTheoryExamSessionMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Keeps session state transitions in short, independent database transactions. */
@Service
public class CompetitionTheorySessionLifecycleService {

  private final CompetitionTheoryExamSessionMapper sessionMapper;

  public CompetitionTheorySessionLifecycleService(
      CompetitionTheoryExamSessionMapper sessionMapper) {
    this.sessionMapper = sessionMapper;
  }

  @Transactional
  public void openIfDue(Long sessionId, LocalDateTime now) {
    sessionMapper.openIfDue(sessionId, now);
  }

  @Transactional
  public void closeIfDue(Long sessionId, LocalDateTime now) {
    sessionMapper.closeIfDue(sessionId, now);
  }
}
