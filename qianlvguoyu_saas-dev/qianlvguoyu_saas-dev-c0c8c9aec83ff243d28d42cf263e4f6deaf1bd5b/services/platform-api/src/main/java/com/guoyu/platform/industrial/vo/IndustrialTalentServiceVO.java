package com.guoyu.platform.industrial.vo;

import com.guoyu.platform.industrial.entity.IndustrialTalentServiceEntity;

public record IndustrialTalentServiceVO(
    String id,
    String title,
    String source,
    String date
) {

  public static IndustrialTalentServiceVO from(IndustrialTalentServiceEntity entity) {
    if (entity == null) {
      return null;
    }
    return new IndustrialTalentServiceVO(
        entity.getId() == null ? null : String.valueOf(entity.getId()),
        entity.getTitle(),
        entity.getSource(),
        entity.getServiceDate()
    );
  }
}
