package com.guoyu.platform.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("exam_paper_detail")
public class ExamPaperDetailEntity {

  @TableId(type = IdType.AUTO)
  private Integer id;

  private Integer paperId;
  private Integer examId;
  private String type;
  private Integer diff;
  private String content;
  private String answer;
  private BigDecimal score;
  private String resolve;
  private Integer status;
  private Integer sortNumber;
  private Integer tenantId;
  private LocalDateTime createTime;

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public Integer getPaperId() { return paperId; }
  public void setPaperId(Integer paperId) { this.paperId = paperId; }
  public Integer getExamId() { return examId; }
  public void setExamId(Integer examId) { this.examId = examId; }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public Integer getDiff() { return diff; }
  public void setDiff(Integer diff) { this.diff = diff; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public String getAnswer() { return answer; }
  public void setAnswer(String answer) { this.answer = answer; }
  public BigDecimal getScore() { return score; }
  public void setScore(BigDecimal score) { this.score = score; }
  public String getResolve() { return resolve; }
  public void setResolve(String resolve) { this.resolve = resolve; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public Integer getTenantId() { return tenantId; }
  public void setTenantId(Integer tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
