package com.guoyu.platform.competition.converter;

import com.guoyu.platform.competition.entity.CompetitionScoreRuleEntity;
import com.guoyu.platform.competition.vo.CompetitionScoreRuleVO;

public final class CompetitionScoreRuleConverter {

  private CompetitionScoreRuleConverter() {
  }

  public static CompetitionScoreRuleVO toVO(CompetitionScoreRuleEntity entity) {
    if (entity == null) {
      return null;
    }
    CompetitionScoreRuleVO vo = new CompetitionScoreRuleVO();
    vo.setId(entity.getId());
    vo.setEventId(entity.getEventId());
    vo.setStageId(entity.getStageId());
    vo.setEventWorkTypeId(entity.getEventWorkTypeId());
    vo.setEventWorkTypeLevelId(entity.getEventWorkTypeLevelId());
    vo.setScoringSystem(entity.getScoringSystem());
    vo.setTheoryTemplate(entity.getTheoryTemplate());
    vo.setTheoryWeight(entity.getTheoryWeight());
    vo.setSimulationPracticalWeight(entity.getSimulationPracticalWeight());
    vo.setSimulationWeight(entity.getSimulationWeight());
    vo.setPracticalWeight(entity.getPracticalWeight());
    vo.setStatus(entity.getStatus());
    vo.setRuleVersion(entity.getRuleVersion());
    vo.setPublishedAt(entity.getPublishedAt());
    vo.setPublishedBy(entity.getPublishedBy());
    vo.setCreatedAt(entity.getCreatedAt());
    vo.setUpdatedAt(entity.getUpdatedAt());
    return vo;
  }
}
