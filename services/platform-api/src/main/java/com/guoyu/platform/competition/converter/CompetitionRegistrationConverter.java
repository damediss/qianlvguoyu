package com.guoyu.platform.competition.converter;

import com.guoyu.platform.competition.mapper.CompetitionRegistrationQueryRow;
import com.guoyu.platform.competition.vo.CompetitionRegistrationVO;

public final class CompetitionRegistrationConverter {

  private CompetitionRegistrationConverter() {
  }

  public static CompetitionRegistrationVO toVO(CompetitionRegistrationQueryRow source) {
    if (source == null) {
      return null;
    }
    CompetitionRegistrationVO target = new CompetitionRegistrationVO();
    target.setRegistrationId(source.getRegistrationId());
    target.setEventId(source.getEventId());
    target.setEventName(source.getEventName());
    target.setOrgId(source.getOrgId());
    target.setOrgName(source.getOrgName());
    target.setRosterItemId(source.getRosterItemId());
    target.setEmployeeNo(source.getEmployeeNo());
    target.setName(source.getName());
    target.setWorkType(source.getWorkType());
    target.setLevel(source.getLevel());
    target.setSource(source.getSource());
    target.setStatus(source.getStatus());
    target.setAuditReason(source.getAuditReason());
    target.setAuditedBy(source.getAuditedBy());
    target.setAuditedAt(source.getAuditedAt());
    target.setCreatedAt(source.getCreatedAt());
    return target;
  }
}
