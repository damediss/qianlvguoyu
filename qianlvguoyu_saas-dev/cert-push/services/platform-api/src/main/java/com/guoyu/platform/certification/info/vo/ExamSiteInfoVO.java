package com.guoyu.platform.certification.info.vo;

public record ExamSiteInfoVO(
    Long id,
    String siteName,
    String siteType,
    String address,
    Integer seatCount,
    String filingNo,
    String contactName,
    String maskedContactPhone,
    String equipmentCondition,
    String materialsDescription,
    String auditStatus,
    String status,
    String remark
) {
}
