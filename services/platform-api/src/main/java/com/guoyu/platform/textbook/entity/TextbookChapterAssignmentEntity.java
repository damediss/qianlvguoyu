package com.guoyu.platform.textbook.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 教材章分配（textbook_chapter_assignment）：每章一条当前记录 + 列表查询进度缓存。
 *
 * <p>task_status 与各 *_count 是冗余缓存，唯一事实源是节正文状态；
 * 由 TextbookProgressService.refreshChapterSummary 在写操作同事务内统一重算。
 * active=0 表示已取消（复用本行重新分配），deleted 仅在章节被重建移除时置位。
 */
@TableName("textbook_chapter_assignment")
public class TextbookChapterAssignmentEntity extends BaseEntity {

  private Long projectId;
  private Long chapterId;
  private Long writerId;
  private Long reviewerId;
  private LocalDate dueDate;
  private String instruction;
  /** 1 有效，0 已取消；不是 deleted */
  private Integer active;
  /** assigned/writing/reviewing/returned/approved */
  private String taskStatus;
  private Integer sectionCount;
  private Integer approvedCount;
  private Integer submittedCount;
  private Integer returnedCount;
  private Integer draftCount;
  private Long assignedBy;
  private LocalDateTime assignedAt;
  /** 仅改派/取消递增，进度刷新不递增 */
  private Integer assignmentVersion;

  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

  public Long getChapterId() {
    return chapterId;
  }

  public void setChapterId(Long chapterId) {
    this.chapterId = chapterId;
  }

  public Long getWriterId() {
    return writerId;
  }

  public void setWriterId(Long writerId) {
    this.writerId = writerId;
  }

  public Long getReviewerId() {
    return reviewerId;
  }

  public void setReviewerId(Long reviewerId) {
    this.reviewerId = reviewerId;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public void setDueDate(LocalDate dueDate) {
    this.dueDate = dueDate;
  }

  public String getInstruction() {
    return instruction;
  }

  public void setInstruction(String instruction) {
    this.instruction = instruction;
  }

  public Integer getActive() {
    return active;
  }

  public void setActive(Integer active) {
    this.active = active;
  }

  public String getTaskStatus() {
    return taskStatus;
  }

  public void setTaskStatus(String taskStatus) {
    this.taskStatus = taskStatus;
  }

  public Integer getSectionCount() {
    return sectionCount;
  }

  public void setSectionCount(Integer sectionCount) {
    this.sectionCount = sectionCount;
  }

  public Integer getApprovedCount() {
    return approvedCount;
  }

  public void setApprovedCount(Integer approvedCount) {
    this.approvedCount = approvedCount;
  }

  public Integer getSubmittedCount() {
    return submittedCount;
  }

  public void setSubmittedCount(Integer submittedCount) {
    this.submittedCount = submittedCount;
  }

  public Integer getReturnedCount() {
    return returnedCount;
  }

  public void setReturnedCount(Integer returnedCount) {
    this.returnedCount = returnedCount;
  }

  public Integer getDraftCount() {
    return draftCount;
  }

  public void setDraftCount(Integer draftCount) {
    this.draftCount = draftCount;
  }

  public Long getAssignedBy() {
    return assignedBy;
  }

  public void setAssignedBy(Long assignedBy) {
    this.assignedBy = assignedBy;
  }

  public LocalDateTime getAssignedAt() {
    return assignedAt;
  }

  public void setAssignedAt(LocalDateTime assignedAt) {
    this.assignedAt = assignedAt;
  }

  public Integer getAssignmentVersion() {
    return assignmentVersion;
  }

  public void setAssignmentVersion(Integer assignmentVersion) {
    this.assignmentVersion = assignmentVersion;
  }
}
