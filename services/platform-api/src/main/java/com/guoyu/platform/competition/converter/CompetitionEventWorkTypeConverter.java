package com.guoyu.platform.competition.converter;

import com.guoyu.platform.competition.dto.CompetitionEventWorkTypeCreateReq;
import com.guoyu.platform.competition.dto.CompetitionEventWorkTypeUpdateReq;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.vo.CompetitionEventWorkTypeVO;

public final class CompetitionEventWorkTypeConverter {

  private CompetitionEventWorkTypeConverter() {
  }

  public static CompetitionEventWorkTypeEntity toEntity(CompetitionEventWorkTypeCreateReq source) {
    if (source == null) {
      return null;
    }
    CompetitionEventWorkTypeEntity target = new CompetitionEventWorkTypeEntity();
    target.setWorkTypeId(source.workTypeId());
    target.setDisplayName(source.displayName());
    target.setStatus(source.status());
    target.setSortOrder(source.sortOrder());
    target.setStagePlanStatus("draft");
    return target;
  }

  public static void applyUpdate(
      CompetitionEventWorkTypeEntity target,
      CompetitionEventWorkTypeUpdateReq source) {
    if (target == null || source == null) {
      return;
    }
    target.setWorkTypeId(source.workTypeId());
    target.setDisplayName(source.displayName());
    if (source.status() != null) {
      target.setStatus(source.status());
    }
    if (source.sortOrder() != null) {
      target.setSortOrder(source.sortOrder());
    }
  }

  public static CompetitionEventWorkTypeVO toVO(
      CompetitionEventWorkTypeEntity source,
      String workTypeName) {
    if (source == null) {
      return null;
    }
    CompetitionEventWorkTypeVO target = new CompetitionEventWorkTypeVO();
    target.setId(source.getId());
    target.setEventId(source.getEventId());
    target.setWorkTypeId(source.getWorkTypeId());
    target.setWorkTypeName(workTypeName);
    target.setDisplayName(source.getDisplayName());
    target.setStatus(source.getStatus());
    target.setSortOrder(source.getSortOrder());
    target.setStagePlanStatus(source.getStagePlanStatus());
    target.setStagePlanConfirmedBy(source.getStagePlanConfirmedBy());
    target.setStagePlanConfirmedAt(source.getStagePlanConfirmedAt());
    return target;
  }
}
