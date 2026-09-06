package com.guoyu.platform.exam.theorygen.dto;

import com.guoyu.platform.exam.theorygen.entity.TheoryBlueprintEntity;
import com.guoyu.platform.exam.theorygen.service.TheoryBlueprintApplicationService;
import java.time.LocalDateTime;

/** 细目表批次 VO */
public record BlueprintVO(
    Long id,
    Integer workTypeId,
    String workTypeName,
    Integer level,
    String levelName,
    Integer courseId,
    String title,
    String sourceFileName,
    Integer pointCount,
    String xyzRatio,
    Integer weightCheck,
    String checkReport,
    String status,
    LocalDateTime createdAt) {

  public static BlueprintVO from(TheoryBlueprintEntity e, String workTypeName) {
    String levelName = e.getLevel() != null && e.getLevel() >= 1 && e.getLevel() <= 5
        ? TheoryBlueprintApplicationService.LEVEL_NAMES[e.getLevel()] : "";
    return new BlueprintVO(e.getId(), e.getWorkTypeId(), workTypeName, e.getLevel(), levelName,
        e.getCourseId(), e.getTitle(), e.getSourceFileName(), e.getPointCount(),
        e.getXyzRatio(), e.getWeightCheck(), e.getCheckReport(), e.getStatus(), e.getCreatedAt());
  }
}
