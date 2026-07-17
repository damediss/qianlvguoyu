package com.guoyu.platform.competition.converter;

import com.guoyu.platform.competition.dto.CompetitionEventDraftReq;
import com.guoyu.platform.competition.dto.CompetitionEventCreateReq;
import com.guoyu.platform.competition.dto.CompetitionEventUpdateReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.vo.CompetitionEventVO;

/** Explicit conversion boundary; entities are not exposed by the API layer. */
public final class CompetitionEventConverter {

  private CompetitionEventConverter() {
  }

  public static CompetitionEventEntity toEntity(CompetitionEventDraftReq source) {
    if (source == null) {
      return null;
    }
    CompetitionEventEntity target = new CompetitionEventEntity();
    target.setName(source.getName());
    target.setCode(source.getCode());
    target.setScope(source.getScope());
    target.setOwnerOrgId(source.getOwnerOrgId());
    target.setRegistrationAuditRequired(source.getRegistrationAuditRequired());
    target.setStartAt(source.getStartAt());
    target.setEndAt(source.getEndAt());
    target.setDescription(source.getDescription());
    return target;
  }

  public static CompetitionEventEntity toEntity(CompetitionEventCreateReq source) {
    if (source == null) {
      return null;
    }
    CompetitionEventEntity target = new CompetitionEventEntity();
    target.setName(source.name());
    target.setCode(source.code());
    target.setScope(source.scope());
    target.setRegistrationAuditRequired(source.registrationAuditRequired());
    target.setStartAt(source.startAt());
    target.setEndAt(source.endAt());
    target.setDescription(source.description());
    return target;
  }

  public static void applyUpdate(CompetitionEventEntity target, CompetitionEventUpdateReq source) {
    if (target == null || source == null) {
      return;
    }
    target.setName(source.name());
    if (source.registrationAuditRequired() != null) {
      target.setRegistrationAuditRequired(source.registrationAuditRequired());
    }
    target.setStartAt(source.startAt());
    target.setEndAt(source.endAt());
    target.setDescription(source.description());
  }

  public static CompetitionEventVO toVO(CompetitionEventEntity source) {
    if (source == null) {
      return null;
    }
    CompetitionEventVO target = new CompetitionEventVO();
    target.setId(source.getId());
    target.setName(source.getName());
    target.setCode(source.getCode());
    target.setScope(source.getScope());
    target.setOwnerOrgId(source.getOwnerOrgId());
    target.setStatus(source.getStatus());
    target.setRegistrationAuditRequired(source.getRegistrationAuditRequired());
    target.setStartAt(source.getStartAt());
    target.setEndAt(source.getEndAt());
    target.setDescription(source.getDescription());
    target.setCreatedBy(source.getCreatedBy());
    target.setCreatedAt(source.getCreatedAt());
    target.setUpdatedBy(source.getUpdatedBy());
    target.setUpdatedAt(source.getUpdatedAt());
    return target;
  }
}
