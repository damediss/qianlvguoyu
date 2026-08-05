package com.guoyu.platform.competition.service;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.entity.CompetitionContestantProfileEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryAttemptEntity;
import com.guoyu.platform.competition.mapper.CompetitionScoreSourceMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryAttemptMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryCandidateAssignmentMapper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Blocker contract for contestant scope changes. The caller must already hold the profile row lock.
 */
@Service
@Slf4j
public class CompetitionContestantScopeChangeGuard {

  private final CompetitionTheoryAttemptMapper attemptMapper;
  private final CompetitionTheoryCandidateAssignmentMapper assignmentMapper;
  private final CompetitionScoreSourceMapper scoreSourceMapper;

  public CompetitionContestantScopeChangeGuard(
      CompetitionTheoryAttemptMapper attemptMapper,
      CompetitionTheoryCandidateAssignmentMapper assignmentMapper,
      CompetitionScoreSourceMapper scoreSourceMapper) {
    this.attemptMapper = attemptMapper;
    this.assignmentMapper = assignmentMapper;
    this.scoreSourceMapper = scoreSourceMapper;
  }

  public void assertChangeAllowed(
      Long tenantId, Long eventId, CompetitionContestantProfileEntity profile) {
    List<CompetitionTheoryAttemptEntity> attempts = attemptMapper.selectNonTerminalForUpdateByProfile(
        tenantId, profile.getId());
    if (attempts != null && !attempts.isEmpty()) {
      reject("non-terminal attempt", tenantId, eventId, profile.getId());
    }
    if (assignmentMapper.countScopeChangeBlockers(tenantId, eventId, profile.getId()) > 0) {
      reject("draft or published assignment", tenantId, eventId, profile.getId());
    }
    if (scoreSourceMapper.countScopeChangeBlockers(
        tenantId, eventId, profile.getEventWorkTypeId(), profile.getMemberId()) > 0) {
      reject("score source", tenantId, eventId, profile.getId());
    }
  }

  private void reject(String blocker, Long tenantId, Long eventId, Long profileId) {
    log.warn("Competition contestant scope change rejected by blocker={}, tenantId={}, eventId={}, profileId={}",
        blocker, tenantId, eventId, profileId);
    throw new BusinessException(ErrorCode.CONFLICT, "选手已有考试或成绩业务数据，不能重新匹配工种");
  }
}
