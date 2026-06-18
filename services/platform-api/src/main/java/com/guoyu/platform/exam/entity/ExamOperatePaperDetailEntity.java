package com.guoyu.platform.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("exam_operate_paper_detail")
public class ExamOperatePaperDetailEntity {

  @TableId(type = IdType.AUTO)
  private Integer id;

  private Integer paperId;
  private String title;
  private String studentPrepare;
  private String content;
  private BigDecimal score;
  private Integer weight;
  private String equipPrepare;
  private String examContent;
  private String scoringTemplateJson;
  private Integer userId;
  private Integer status;
  private Integer sortNumber;
  private Integer tenantId;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public Integer getPaperId() { return paperId; }
  public void setPaperId(Integer paperId) { this.paperId = paperId; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getStudentPrepare() { return studentPrepare; }
  public void setStudentPrepare(String studentPrepare) { this.studentPrepare = studentPrepare; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public BigDecimal getScore() { return score; }
  public void setScore(BigDecimal score) { this.score = score; }
  public Integer getWeight() { return weight; }
  public void setWeight(Integer weight) { this.weight = weight; }
  public String getEquipPrepare() { return equipPrepare; }
  public void setEquipPrepare(String equipPrepare) { this.equipPrepare = equipPrepare; }
  public String getExamContent() { return examContent; }
  public void setExamContent(String examContent) { this.examContent = examContent; }
  public String getScoringTemplateJson() { return scoringTemplateJson; }
  public void setScoringTemplateJson(String scoringTemplateJson) { this.scoringTemplateJson = scoringTemplateJson; }
  public Integer getUserId() { return userId; }
  public void setUserId(Integer userId) { this.userId = userId; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public Integer getTenantId() { return tenantId; }
  public void setTenantId(Integer tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public LocalDateTime getUpdateTime() { return updateTime; }
  public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
