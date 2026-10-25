package com.guoyu.platform.textbook.vo;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.time.LocalDateTime;

/** 我的任务 VO（方案 §6.2）：AssignmentVO 平铺 + 任务列表字段。 */
public record TextbookTaskVO(
    @JsonUnwrapped TextbookAssignmentVO assignment,
    String projectId,
    String workTypeTitle,
    String direction,
    Integer levelCode,
    String chapterTitle,
    String chapterSourceNo,
    String projectStatus,
    LocalDateTime latestSectionUpdatedAt,
    Integer actualWords,
    Integer targetWords,
    boolean overdue) {
}
