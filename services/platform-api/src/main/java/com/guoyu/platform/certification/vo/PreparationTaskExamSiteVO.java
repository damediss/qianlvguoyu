package com.guoyu.platform.certification.vo;

import com.guoyu.platform.certification.entity.CertificationTaskExamSiteEntity;
import java.time.LocalDate;

public record PreparationTaskExamSiteVO(
    Long id,
    Long taskId,
    Long siteId,
    String siteName,
    String siteType,
    String address,
    Integer seatCount,
    LocalDate examDate,
    String startTime,
    String endTime,
    String examType,
    Integer arrangedCount,
    String contactName,
    String contactPhone,
    String status
) {
  public static PreparationTaskExamSiteVO from(CertificationTaskExamSiteEntity entity) {
    return new PreparationTaskExamSiteVO(
        entity.getId(),
        entity.getTaskId(),
        entity.getSiteId(),
        entity.getSiteName(),
        entity.getSiteType(),
        entity.getAddress(),
        entity.getSeatCount(),
        entity.getExamDate(),
        entity.getStartTime(),
        entity.getEndTime(),
        entity.getExamType(),
        entity.getArrangedCount(),
        entity.getContactName(),
        entity.getContactPhone(),
        entity.getStatus()
    );
  }
}
