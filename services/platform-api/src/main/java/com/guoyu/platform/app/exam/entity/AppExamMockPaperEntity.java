package com.guoyu.platform.app.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** App 端生成的模拟考卷（一次组卷/一次错题重练/一次随机练习 都对应一条） */
@TableName("exam_mock_paper")
public class AppExamMockPaperEntity {

  @TableId(type = IdType.AUTO)
  private Integer id;

  private Long userId;
  private Long tenantId;
  private String title;
  private String questionIds;
  private Integer totalCount;
  private Integer totalPoint;
  private Integer passPoint;
  private Integer timeLimit;
  private String mode;
  private Integer score;
  private Integer rightCount;
  private String status;
  private LocalDateTime createTime;
  private LocalDateTime finishTime;

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getQuestionIds() { return questionIds; }
  public void setQuestionIds(String questionIds) { this.questionIds = questionIds; }
  public Integer getTotalCount() { return totalCount; }
  public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
  public Integer getTotalPoint() { return totalPoint; }
  public void setTotalPoint(Integer totalPoint) { this.totalPoint = totalPoint; }
  public Integer getPassPoint() { return passPoint; }
  public void setPassPoint(Integer passPoint) { this.passPoint = passPoint; }
  public Integer getTimeLimit() { return timeLimit; }
  public void setTimeLimit(Integer timeLimit) { this.timeLimit = timeLimit; }
  public String getMode() { return mode; }
  public void setMode(String mode) { this.mode = mode; }
  public Integer getScore() { return score; }
  public void setScore(Integer score) { this.score = score; }
  public Integer getRightCount() { return rightCount; }
  public void setRightCount(Integer rightCount) { this.rightCount = rightCount; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public LocalDateTime getFinishTime() { return finishTime; }
  public void setFinishTime(LocalDateTime finishTime) { this.finishTime = finishTime; }
}
