package com.guoyu.platform.exam.skillgen.dto;

import com.guoyu.platform.exam.skillgen.entity.SkillBlueprintEntity;
import com.guoyu.platform.exam.skillgen.service.SkillBlueprintApplicationService;
import java.time.LocalDateTime;

/** 操作技能细目表批次 VO */
public record SkillBlueprintVO(
    Long id,
    Integer workTypeId,
    String workTypeName,
    Integer level,
    String levelName,
    String title,
    String sourceFileName,
    Long sourceFileId,
    Integer pointCount,
    String xyzRatio,
    Integer weightCheck,
    String checkReport,
    String status,
    LocalDateTime createdAt) {

  public static SkillBlueprintVO from(SkillBlueprintEntity e, String workTypeName) {
    String levelName = e.getLevel() != null && e.getLevel() >= 1 && e.getLevel() <= 5
        ? SkillBlueprintApplicationService.LEVEL_NAMES[e.getLevel()] : "";
    return new SkillBlueprintVO(e.getId(), e.getWorkTypeId(), workTypeName, e.getLevel(), levelName,
        e.getTitle(), e.getSourceFileName(), e.getSourceFileId(), e.getPointCount(),
        e.getXyzRatio(), e.getWeightCheck(), e.getCheckReport(), e.getStatus(), e.getCreatedAt());
  }
}
