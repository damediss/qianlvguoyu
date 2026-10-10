package com.guoyu.platform.competition.converter;

import com.guoyu.platform.competition.entity.CompetitionEventOrgEntity;
import com.guoyu.platform.competition.vo.CompetitionEventOrgVO;

public final class CompetitionEventOrgConverter {

  private CompetitionEventOrgConverter() {
  }

  public static CompetitionEventOrgVO toVO(CompetitionEventOrgEntity source, String orgName) {
    return toVO(source, null, orgName);
  }

  public static CompetitionEventOrgVO toVO(CompetitionEventOrgEntity source, String orgCode, String orgName) {
    if (source == null) {
      return null;
    }
    CompetitionEventOrgVO target = new CompetitionEventOrgVO();
    target.setId(source.getId());
    target.setEventId(source.getEventId());
    target.setOrgId(source.getOrgId());
    target.setOrgName(orgName);
    target.setOrgCode(orgCode);
    target.setQuota(source.getQuota());
    target.setStatus(source.getStatus());
    target.setImportPermission(source.getImportPermission());
    target.setCreatedAt(source.getCreatedAt());
    return target;
  }
}
