package com.guoyu.platform.exam.standard.vo;

import com.guoyu.platform.exam.standard.entity.ExamStandardNodeEntity;
import java.math.BigDecimal;

/**
 * 树节点 VO（id 以字符串下发，避免雪花 ID 超 JS Number 安全整数——坑#24）。
 */
public record StandardNodeVO(
    String id,
    String taskId,
    String parentId,
    String nodeKey,
    Integer nodeLevel,
    Integer sortNumber,
    String dimension,
    String titleRaw,
    String title,
    String important,
    BigDecimal weight,
    String relatedKnowledge,
    String skillRequirement,
    String source,
    String reviewStatus,
    String direction,
    Integer levelCode,
    Integer needLlm) {

  public static StandardNodeVO from(ExamStandardNodeEntity e) {
    return new StandardNodeVO(
        e.getId() == null ? null : String.valueOf(e.getId()),
        e.getTaskId() == null ? null : String.valueOf(e.getTaskId()),
        e.getParentId() == null ? null : String.valueOf(e.getParentId()),
        e.getNodeKey(),
        e.getNodeLevel(),
        e.getSortNumber(),
        e.getDimension(),
        e.getTitleRaw(),
        e.getTitle(),
        e.getImportant(),
        e.getWeight(),
        e.getRelatedKnowledge(),
        e.getSkillRequirement(),
        e.getSource(),
        e.getReviewStatus(),
        e.getDirection(),
        e.getLevelCode(),
        e.getNeedLlm());
  }
}
