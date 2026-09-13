package com.guoyu.platform.exam.knowledgefix.dto;

import java.math.BigDecimal;

/** 待分类行（修正清单行 + 题干/答案/解析联查） */
public class PendingFixRow {
  private Long rowId;
  private Integer detailId;
  private Integer oldKnowledgeId;
  private Integer courseId;
  private String content;
  private String answer;
  private String resolve;
  private String currentStatus;
  private BigDecimal currentConfidence;

  public Long getRowId() { return rowId; }
  public void setRowId(Long rowId) { this.rowId = rowId; }
  public Integer getDetailId() { return detailId; }
  public void setDetailId(Integer detailId) { this.detailId = detailId; }
  public Integer getOldKnowledgeId() { return oldKnowledgeId; }
  public void setOldKnowledgeId(Integer oldKnowledgeId) { this.oldKnowledgeId = oldKnowledgeId; }
  public Integer getCourseId() { return courseId; }
  public void setCourseId(Integer courseId) { this.courseId = courseId; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public String getAnswer() { return answer; }
  public void setAnswer(String answer) { this.answer = answer; }
  public String getResolve() { return resolve; }
  public void setResolve(String resolve) { this.resolve = resolve; }
  public String getCurrentStatus() { return currentStatus; }
  public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }
  public BigDecimal getCurrentConfidence() { return currentConfidence; }
  public void setCurrentConfidence(BigDecimal currentConfidence) { this.currentConfidence = currentConfidence; }
}
