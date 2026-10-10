package com.guoyu.platform.competition.converter;

import com.guoyu.platform.competition.dto.CompetitionEventWorkTypeLevelItemReq;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.vo.CompetitionEventWorkTypeLevelVO;

public final class CompetitionEventWorkTypeLevelConverter {

  private CompetitionEventWorkTypeLevelConverter() {
  }

  public static void apply(
      CompetitionEventWorkTypeLevelEntity target,
      CompetitionEventWorkTypeLevelItemReq source) {
    target.setLevelCode(source.levelCode().trim());
    target.setLevelName(source.levelName().trim());
    target.setSourceLevelCode(source.sourceLevelCode());
    target.setSourceLevelText(source.sourceLevelText());
    target.setReconciliationStatus(
        source.reconciliationStatus() == null || source.reconciliationStatus().isBlank()
            ? "matched" : source.reconciliationStatus());
  }

  public static CompetitionEventWorkTypeLevelVO toVO(CompetitionEventWorkTypeLevelEntity source) {
    if (source == null) {
      return null;
    }
    CompetitionEventWorkTypeLevelVO target = new CompetitionEventWorkTypeLevelVO();
    target.setId(source.getId());
    target.setEventId(source.getEventId());
    target.setEventWorkTypeId(source.getEventWorkTypeId());
    target.setLevelCode(source.getLevelCode());
    target.setLevelName(source.getLevelName());
    target.setSourceLevelCode(source.getSourceLevelCode());
    target.setSourceLevelText(source.getSourceLevelText());
    target.setReconciliationStatus(source.getReconciliationStatus());
    target.setStagePlanStatus(source.getStagePlanStatus());
    target.setStagePlanConfirmedBy(source.getStagePlanConfirmedBy());
    target.setStagePlanConfirmedAt(source.getStagePlanConfirmedAt());
    target.setConfirmedAt(source.getConfirmedAt());
    target.setConfirmedBy(source.getConfirmedBy());
    return target;
  }
}
