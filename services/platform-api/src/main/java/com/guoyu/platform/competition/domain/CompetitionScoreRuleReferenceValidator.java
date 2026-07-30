package com.guoyu.platform.competition.domain;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.service.CompetitionScoringReadinessDomainService;
import com.guoyu.platform.competition.vo.CompetitionScoringReadinessVO;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class CompetitionScoreRuleReferenceValidator {

  private final CompetitionScoringReadinessDomainService readinessDomainService;

  public CompetitionScoreRuleReferenceValidator(
      CompetitionScoringReadinessDomainService readinessDomainService) {
    this.readinessDomainService = readinessDomainService;
  }

  public void validatePublishedRule(
      Long tenantId,
      Long eventId,
      Long stageId,
      Long eventWorkTypeId,
      Long scoreRuleId) {
    CompetitionScoringReadinessVO readiness =
        readinessDomainService.check(tenantId, eventId, stageId, eventWorkTypeId);
    if (readiness == null
        || !readiness.isReady()
        || !Objects.equals(readiness.getPublishedScoreRuleVersionId(), scoreRuleId)) {
      throw new BusinessException(
          ErrorCode.UNPROCESSABLE_ENTITY,
          "成绩范围未就绪或评分规则不是当前范围唯一已发布版本");
    }
  }
}
