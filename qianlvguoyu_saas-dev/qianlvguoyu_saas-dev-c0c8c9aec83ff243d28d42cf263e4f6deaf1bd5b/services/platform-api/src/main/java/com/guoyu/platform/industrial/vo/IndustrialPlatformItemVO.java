package com.guoyu.platform.industrial.vo;

import com.guoyu.platform.industrial.entity.IndustrialPlatformItemEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record IndustrialPlatformItemVO(
    String id,
    String category,
    String company,
    String name,
    Integer nums,
    LocalDate date,
    String state,
    Long tenantId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

  public static IndustrialPlatformItemVO from(IndustrialPlatformItemEntity entity) {
    return new IndustrialPlatformItemVO(
        String.valueOf(entity.getId()),
        entity.getCategory(),
        entity.getCompany(),
        entity.getName(),
        entity.getNums(),
        entity.getItemDate(),
        entity.getState(),
        entity.getTenantId(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
