package com.guoyu.platform.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("task_examsub_detail")
public class ExamTaskExamsubDetailEntity {

  @TableId(type = IdType.AUTO)
  private Integer id;

  private Integer taskId;
  private String zhishidian;
  private String content;
  private String answer;
  private Integer isJisuan;
  private String nanyicd;
  private Integer isblod;
  private String pic;
  private String examType;
  private Long knowledgeId;
  private String scorePoints;
  private String scoringStandard;
  private Long aiBatchId;
  private Integer source;
  private Integer tenantId;

  @TableLogic(value = "0", delval = "1")
  private Integer deleted;

  private LocalDateTime createTime;
  private LocalDateTime updatedAt;
  private Integer createdBy;
  private Integer updatedBy;

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public Integer getTaskId() { return taskId; }
  public void setTaskId(Integer taskId) { this.taskId = taskId; }
  public String getZhishidian() { return zhishidian; }
  public void setZhishidian(String zhishidian) { this.zhishidian = zhishidian; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public String getAnswer() { return answer; }
  public void setAnswer(String answer) { this.answer = answer; }
  public Integer getIsJisuan() { return isJisuan; }
  public void setIsJisuan(Integer isJisuan) { this.isJisuan = isJisuan; }
  public String getNanyicd() { return nanyicd; }
  public void setNanyicd(String nanyicd) { this.nanyicd = nanyicd; }
  public Integer getIsblod() { return isblod; }
  public void setIsblod(Integer isblod) { this.isblod = isblod; }
  public String getExamType() { return examType; }
  public void setExamType(String examType) { this.examType = examType; }
  public Long getKnowledgeId() { return knowledgeId; }
  public void setKnowledgeId(Long knowledgeId) { this.knowledgeId = knowledgeId; }
  public String getScorePoints() { return scorePoints; }
  public void setScorePoints(String scorePoints) { this.scorePoints = scorePoints; }
  public String getScoringStandard() { return scoringStandard; }
  public void setScoringStandard(String scoringStandard) { this.scoringStandard = scoringStandard; }
  public Long getAiBatchId() { return aiBatchId; }
  public void setAiBatchId(Long aiBatchId) { this.aiBatchId = aiBatchId; }
  public Integer getSource() { return source; }
  public void setSource(Integer source) { this.source = source; }
  public String getPic() { return pic; }
  public void setPic(String pic) { this.pic = pic; }
  public Integer getTenantId() { return tenantId; }
  public void setTenantId(Integer tenantId) { this.tenantId = tenantId; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
  public Integer getCreatedBy() { return createdBy; }
  public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }
  public Integer getUpdatedBy() { return updatedBy; }
  public void setUpdatedBy(Integer updatedBy) { this.updatedBy = updatedBy; }
}
