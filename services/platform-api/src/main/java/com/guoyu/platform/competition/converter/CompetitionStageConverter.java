package com.guoyu.platform.competition.converter;

import com.guoyu.platform.competition.dto.CompetitionStageCreateReq;
import com.guoyu.platform.competition.dto.CompetitionStageUpdateReq;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.vo.CompetitionStageVO;

public final class CompetitionStageConverter {

  private CompetitionStageConverter() {
  }

  public static CompetitionStageEntity toEntity(CompetitionStageCreateReq source) {
    if (source == null) {
      return null;
    }
    CompetitionStageEntity target = new CompetitionStageEntity();
    target.setName(source.name());
    target.setStageType(source.stageType());
    target.setSortOrder(source.sortOrder());
    target.setStartAt(source.startAt());
    target.setEndAt(source.endAt());
    target.setDescription(source.description());
    return target;
  }

  public static void applyUpdate(CompetitionStageEntity target, CompetitionStageUpdateReq source) {
    if (target == null || source == null) {
      return;
    }
    target.setName(source.name());
    target.setStageType(source.stageType());
    if (source.sortOrder() != null) {
      target.setSortOrder(source.sortOrder());
    }
    target.setStartAt(source.startAt());
    target.setEndAt(source.endAt());
    if (source.status() != null && !source.status().isBlank()) {
      target.setStatus(source.status());
    }
    target.setDescription(source.description());
  }

  public static CompetitionStageVO toVO(CompetitionStageEntity source) {
    if (source == null) {
      return null;
    }
    CompetitionStageVO target = new CompetitionStageVO();
    target.setId(source.getId());
    target.setEventId(source.getEventId());
    target.setName(source.getName());
    target.setStageType(source.getStageType());
    target.setSortOrder(source.getSortOrder());
    target.setStartAt(source.getStartAt());
    target.setEndAt(source.getEndAt());
    target.setStatus(source.getStatus());
    target.setDescription(source.getDescription());
    return target;
  }
}
