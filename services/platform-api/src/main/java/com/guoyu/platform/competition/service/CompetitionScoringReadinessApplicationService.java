package com.guoyu.platform.competition.service;

import com.guoyu.platform.competition.vo.CompetitionScoringReadinessVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionScoringReadinessApplicationService {

  private final CompetitionScoringReadinessDomainService readinessDomainService;
  private final CompetitionScoreRuleApplicationService scoreRuleApplicationService;

  public CompetitionScoringReadinessApplicationService(
      CompetitionScoringReadinessDomainService readinessDomainService,
      CompetitionScoreRuleApplicationService scoreRuleApplicationService) {
    this.readinessDomainService = readinessDomainService;
    this.scoreRuleApplicationService = scoreRuleApplicationService;
  }

  @Transactional(readOnly = true)
  public CompetitionScoringReadinessVO check(
      Long eventId, Long stageId, Long eventWorkTypeId) {
    PlatformUserPrincipal principal = currentUser();
    scoreRuleApplicationService.accessibleEvent(principal, eventId);
    return readinessDomainService.check(
        principal.getTenantId(), eventId, stageId, eventWorkTypeId);
  }

  private PlatformUserPrincipal currentUser() {
    try {
      return SecurityContext.currentUser();
    } catch (IllegalStateException ex) {
      log.warn("Competition scoring readiness request has no authenticated platform user");
      throw ex;
    }
  }
}
