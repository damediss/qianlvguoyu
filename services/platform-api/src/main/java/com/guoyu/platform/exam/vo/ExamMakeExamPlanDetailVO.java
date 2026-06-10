package com.guoyu.platform.exam.vo;

import com.guoyu.platform.exam.entity.ExamMakeExamPlanDetailEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ExamMakeExamPlanDetailVO {

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
  private Integer knowledgeId;
  private Integer score;

  public static ExamMakeExamPlanDetailVO from(ExamMakeExamPlanDetailEntity entity) {
    ExamMakeExamPlanDetailVO vo = new ExamMakeExamPlanDetailVO();
    vo.setId(entity.getId());
    vo.setMakeId(entity.getMakeId());
    vo.setCourseId(entity.getCourseId());
    vo.setType(entity.getType());
    vo.setNum(entity.getNum());
    vo.setPoint(entity.getPoint());
    vo.setStartPoint(entity.getStartPoint());
    vo.setEndPoint(entity.getEndPoint());
    vo.setDifficultyStart(entity.getDifficultyStart());
    vo.setDifficultyEnd(entity.getDifficultyEnd());
    vo.setTotalPoint(entity.getTotalPoint());
    vo.setUserId(entity.getUserId());
    vo.setStatus(entity.getStatus());
    vo.setSortNumber(entity.getSortNumber());
    vo.setTenantId(entity.getTenantId());
    vo.setCreateTime(entity.getCreateTime());
    vo.setKnowledgeId(entity.getKnowledgeId());
    vo.setScore(entity.getScore());
    return vo;
  }

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
