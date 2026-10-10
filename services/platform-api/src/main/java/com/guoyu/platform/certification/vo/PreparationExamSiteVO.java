package com.guoyu.platform.certification.vo;

import com.guoyu.platform.certification.entity.CertificationExamSiteEntity;

public record PreparationExamSiteVO(
    Long id,
    String siteName,
    String siteType,
    String address,
    Integer seatCount,
    String filingNo,
    String contactName,
    String contactPhone,
    String status,
    String remark
) {
  public static PreparationExamSiteVO from(CertificationExamSiteEntity entity) {
    return new PreparationExamSiteVO(
        entity.getId(),
        entity.getSiteName(),
        entity.getSiteType(),
        entity.getAddress(),
        entity.getSeatCount(),
        entity.getFilingNo(),
        entity.getContactName(),
        entity.getContactPhone(),
        entity.getStatus(),
        entity.getRemark()
    );
  }
}
