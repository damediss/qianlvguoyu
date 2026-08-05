package com.guoyu.platform.competition.converter;

import com.guoyu.platform.competition.entity.CompetitionRosterImportEntity;
import com.guoyu.platform.competition.entity.CompetitionRosterItemEntity;
import com.guoyu.platform.competition.vo.CompetitionRosterImportDetailVO;
import com.guoyu.platform.competition.vo.CompetitionRosterImportVO;
import com.guoyu.platform.competition.vo.CompetitionRosterItemVO;

public final class CompetitionRosterImportConverter {

  private CompetitionRosterImportConverter() {
  }

  public static CompetitionRosterItemVO toItemVO(CompetitionRosterItemEntity source) {
    if (source == null) {
      return null;
    }
    CompetitionRosterItemVO target = new CompetitionRosterItemVO();
    target.setId(source.getId());
    target.setBatchId(source.getBatchId());
    target.setEmployeeNo(source.getEmployeeNo());
    target.setName(source.getName());
    target.setWorkType(source.getWorkType());
    target.setLevel(source.getLevel());
    target.setValidationStatus(source.getValidationStatus());
    target.setErrorMessage(source.getErrorMessage());
    return target;
  }

  public static CompetitionRosterImportDetailVO toDetailVO(CompetitionRosterImportEntity source) {
    if (source == null) {
      return null;
    }
    CompetitionRosterImportDetailVO target = new CompetitionRosterImportDetailVO();
    target.setId(source.getId());
    target.setBatchId(source.getId());
    target.setEventId(source.getEventId());
    target.setOrgId(source.getOrgId());
    target.setEventWorkTypeId(source.getEventWorkTypeId());
    target.setBatchNo(source.getBatchNo());
    target.setSourceFileId(source.getSourceFileId());
    target.setStatus(source.getStatus());
    target.setTotalCount(source.getTotalCount());
    target.setValidCount(source.getValidCount());
    target.setInvalidCount(source.getInvalidCount());
    target.setConfirmedBy(source.getConfirmedBy());
    target.setConfirmedAt(source.getConfirmedAt());
    target.setSummary(source.getSummary());
    target.setCreatedBy(source.getCreatedBy());
    target.setCreatedAt(source.getCreatedAt());
    return target;
  }

  public static CompetitionRosterImportVO toVO(CompetitionRosterImportEntity source, String orgName) {
    if (source == null) return null;
    CompetitionRosterImportVO target = new CompetitionRosterImportVO();
    target.setId(source.getId());
    target.setBatchId(source.getId());
    target.setBatchNo(source.getBatchNo());
    target.setEventId(source.getEventId());
    target.setOrgId(source.getOrgId());
    target.setEventWorkTypeId(source.getEventWorkTypeId());
    target.setOrgName(orgName);
    target.setSourceFileId(source.getSourceFileId());
    target.setStatus(source.getStatus());
    target.setTotalCount(source.getTotalCount());
    target.setValidCount(source.getValidCount());
    target.setInvalidCount(source.getInvalidCount());
    target.setConfirmedBy(source.getConfirmedBy());
    target.setConfirmedAt(source.getConfirmedAt());
    target.setCreatedBy(source.getCreatedBy());
    target.setCreatedAt(source.getCreatedAt());
    return target;
  }
}
