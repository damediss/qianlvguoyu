package com.guoyu.platform.textbook.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

/**
 * 教材节审定时间线（textbook_section_review_record）：submit/approve/return/reopen。
 * created_by 即操作人、created_at 即操作时间；comment 在 return/reopen 必填。
 */
@TableName("textbook_section_review_record")
public class TextbookSectionReviewRecordEntity extends BaseEntity {

  private Long contentId;
  private Long sectionId;
  /** 对应第几次提交 */
  private Integer submitRevision;
  /** submit/approve/return/reopen */
  private String action;
  private String fromStatus;
  private String toStatus;
  private String comment;

  public Long getContentId() {
    return contentId;
  }

  public void setContentId(Long contentId) {
    this.contentId = contentId;
  }

  public Long getSectionId() {
    return sectionId;
  }

  public void setSectionId(Long sectionId) {
    this.sectionId = sectionId;
  }

  public Integer getSubmitRevision() {
    return submitRevision;
  }

  public void setSubmitRevision(Integer submitRevision) {
    this.submitRevision = submitRevision;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getFromStatus() {
    return fromStatus;
  }

  public void setFromStatus(String fromStatus) {
    this.fromStatus = fromStatus;
  }

  public String getToStatus() {
    return toStatus;
  }

  public void setToStatus(String toStatus) {
    this.toStatus = toStatus;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }
}
