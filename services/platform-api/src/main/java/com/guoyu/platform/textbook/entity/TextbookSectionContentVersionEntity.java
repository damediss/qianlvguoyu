package com.guoyu.platform.textbook.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

/**
 * 教材节正文快照（textbook_section_content_version）：不可变。
 *
 * <p>snapshot_type=submit（提交审定时）或 before_restore（恢复历史前先保存当前内容）。
 * snapshot_no 每 content 从 1 递增、全类型共用；submit_revision 仅 submit 快照有值。
 */
@TableName("textbook_section_content_version")
public class TextbookSectionContentVersionEntity extends BaseEntity {

  private Long contentId;
  private Long sectionId;
  private Integer snapshotNo;
  /** 仅 submit 快照有值：第 1/2/3… 次提交 */
  private Integer submitRevision;
  /** submit / before_restore */
  private String snapshotType;
  private String contentMd;
  private Integer wordCount;

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

  public Integer getSnapshotNo() {
    return snapshotNo;
  }

  public void setSnapshotNo(Integer snapshotNo) {
    this.snapshotNo = snapshotNo;
  }

  public Integer getSubmitRevision() {
    return submitRevision;
  }

  public void setSubmitRevision(Integer submitRevision) {
    this.submitRevision = submitRevision;
  }

  public String getSnapshotType() {
    return snapshotType;
  }

  public void setSnapshotType(String snapshotType) {
    this.snapshotType = snapshotType;
  }

  public String getContentMd() {
    return contentMd;
  }

  public void setContentMd(String contentMd) {
    this.contentMd = contentMd;
  }

  public Integer getWordCount() {
    return wordCount;
  }

  public void setWordCount(Integer wordCount) {
    this.wordCount = wordCount;
  }
}
