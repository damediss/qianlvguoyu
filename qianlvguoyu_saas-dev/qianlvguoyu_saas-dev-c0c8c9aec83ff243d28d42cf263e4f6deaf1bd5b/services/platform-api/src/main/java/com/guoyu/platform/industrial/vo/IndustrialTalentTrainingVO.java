package com.guoyu.platform.industrial.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.guoyu.platform.industrial.entity.IndustrialTalentTrainingEntity;

public record IndustrialTalentTrainingVO(
    String id,
    String name,
    String level,
    @JsonProperty("work_type") String workType,
    String zhiwei,
    String education
) {

  public static IndustrialTalentTrainingVO from(IndustrialTalentTrainingEntity entity) {
    if (entity == null) {
      return null;
    }
    return new IndustrialTalentTrainingVO(
        entity.getId() == null ? null : String.valueOf(entity.getId()),
        entity.getName(),
        entity.getLevel(),
        entity.getWorkType(),
        entity.getZhiwei(),
        entity.getEducation()
    );
  }
}
