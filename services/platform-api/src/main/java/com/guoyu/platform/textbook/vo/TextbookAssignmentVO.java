package com.guoyu.platform.textbook.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 章分配 VO（方案 §6.1 C3/C4 响应、C2/W1 内嵌）。ID 均字符串下发。 */
public record TextbookAssignmentVO(
    String chapterId,
    String writerId,
    String writerName,
    String reviewerId,
    String reviewerName,
    LocalDate dueDate,
    String instruction,
    boolean active,
    String taskStatus,
    Integer assignmentVersion,
    Integer sectionCount,
    Integer approvedCount,
    Integer submittedCount,
    Integer returnedCount,
    Integer draftCount,
    Integer untouchedCount,
    String assignedByName,
    LocalDateTime assignedAt) {
}
