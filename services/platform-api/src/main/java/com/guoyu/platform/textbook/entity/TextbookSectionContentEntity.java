package com.guoyu.platform.textbook.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

/**
 * 教材节当前正文（textbook_section_content）：每节一条。
 *
 * <p>content_status 状态机：draft → submitted → approved / returned（方案 §3.2）。
 * version 为显式乐观锁（baseVersion 条件更新），不依赖 MP @Version 插件。
 * submitted_by/reviewed_by 在重新提交时清空（方案 §5.2），写回 NULL 需 ALWAYS 策略。
 */
@TableName("textbook_section_content")
public class TextbookSectionContentEntity extends BaseEntity {

  private Long projectId;
  private Long chapterId;
  private Long sectionId;
  private String contentMd;
  /** draft/submitted/approved/returned */
  private String contentStatus;
  private Integer wordCount;
  private String checkReportJson;
  private Integer version;
  private Long submittedBy;
  private LocalDateTime submittedAt;
  @TableField(updateStrategy = FieldStrategy.ALWAYS)
  private Long reviewedBy;
  @TableField(updateStrategy = FieldStrategy.ALWAYS)
  private LocalDateTime reviewedAt;

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

  public Long getSectionId() {
    return sectionId;
  }

  public void setSectionId(Long sectionId) {
    this.sectionId = sectionId;
  }

  public String getContentMd() {
    return contentMd;
  }

  public void setContentMd(String contentMd) {
    this.contentMd = contentMd;
  }

  public String getContentStatus() {
    return contentStatus;
  }

  public void setContentStatus(String contentStatus) {
    this.contentStatus = contentStatus;
  }

  public Integer getWordCount() {
    return wordCount;
  }

  public void setWordCount(Integer wordCount) {
    this.wordCount = wordCount;
  }

  public String getCheckReportJson() {
    return checkReportJson;
  }

  public void setCheckReportJson(String checkReportJson) {
    this.checkReportJson = checkReportJson;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public Long getSubmittedBy() {
    return submittedBy;
  }

  public void setSubmittedBy(Long submittedBy) {
    this.submittedBy = submittedBy;
  }

  public LocalDateTime getSubmittedAt() {
    return submittedAt;
  }

  public void setSubmittedAt(LocalDateTime submittedAt) {
    this.submittedAt = submittedAt;
  }

  public Long getReviewedBy() {
    return reviewedBy;
  }

  public void setReviewedBy(Long reviewedBy) {
    this.reviewedBy = reviewedBy;
  }

  public LocalDateTime getReviewedAt() {
    return reviewedAt;
  }

  public void setReviewedAt(LocalDateTime reviewedAt) {
    this.reviewedAt = reviewedAt;
  }
}
