package com.guoyu.platform.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("exam_make_exam_plan_detail")
public class ExamMakeExamPlanDetailEntity {

  @TableId(type = IdType.AUTO)
  private Integer id;

  private Integer makeId;
  private Integer courseId;
  private String type;
  private Integer num;
  private BigDecimal point;
  private Integer startPoint;
  private Integer endPoint;
  private Integer difficultyStart;
  private Integer difficultyEnd;
  private BigDecimal totalPoint;
  private Integer userId;
  private Integer status;
  private Integer sortNumber;
  private Integer tenantId;
  private LocalDateTime createTime;

  @TableField(exist = false)
  private Integer knowledgeId;
  @TableField(exist = false)
  private Integer score;

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public Integer getMakeId() { return makeId; }
  public void setMakeId(Integer makeId) { this.makeId = makeId; }
  public Integer getCourseId() { return courseId; }
  public void setCourseId(Integer courseId) { this.courseId = courseId; }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public Integer getNum() { return num; }
  public void setNum(Integer num) { this.num = num; }
  public BigDecimal getPoint() { return point; }
  public void setPoint(BigDecimal point) { this.point = point; }
  public Integer getStartPoint() { return startPoint; }
  public void setStartPoint(Integer startPoint) { this.startPoint = startPoint; }
  public Integer getEndPoint() { return endPoint; }
  public void setEndPoint(Integer endPoint) { this.endPoint = endPoint; }
  public Integer getDifficultyStart() { return difficultyStart; }
  public void setDifficultyStart(Integer difficultyStart) { this.difficultyStart = difficultyStart; }
  public Integer getDifficultyEnd() { return difficultyEnd; }
  public void setDifficultyEnd(Integer difficultyEnd) { this.difficultyEnd = difficultyEnd; }
  public BigDecimal getTotalPoint() { return totalPoint; }
  public void setTotalPoint(BigDecimal totalPoint) { this.totalPoint = totalPoint; }
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
  public Integer getKnowledgeId() { return knowledgeId; }
  public void setKnowledgeId(Integer knowledgeId) { this.knowledgeId = knowledgeId; }
  public Integer getScore() { return score; }
  public void setScore(Integer score) { this.score = score; }
}
