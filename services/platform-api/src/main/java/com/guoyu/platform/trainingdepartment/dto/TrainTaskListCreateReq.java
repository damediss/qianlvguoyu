package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 知识点任务列表创建请求 */
public class TrainTaskListCreateReq {

  /** 工种ID */
  private Long workTypeId;

  /** 课程ID */
  private Long courseId;

  /** 知识点ID */
  private Long knowledgeId;

  @NotBlank
  @Size(max = 255)
  private String title;

  /** 类型 */
  @Size(max = 255)
  private String type;

  @Size(max = 2000)
  private String content;

  /** 学习时长 */
  private BigDecimal getLearnTime;

  /** 最低分钟数 */
  private Integer minMinute;

  /** 是否必修 */
  private Integer isRequired;

  private Long userId;

  private Integer sortNumber;

  /** 分数 */
  private Integer score;

  /** 获得积分 */
  private BigDecimal getPoint;

  /** 审核状态 */
  private Integer checkStatus;

  /** 终审状态 */
  private Integer finalCheckStatus;

  /** 审核时间 */
  private LocalDateTime checkTime;

  /** 审核人ID */
  private Long checkUserId;

  /** 审核人用户名 */
  @Size(max = 255)
  private String checkUsername;

  /** 终审时间 */
  private LocalDateTime finalCheckTime;

  /** 终审人ID */
  private Long finalCheckUserId;

  /** 终审人用户名 */
  @Size(max = 255)
  private String finalCheckUsername;

  public Long getWorkTypeId() {
    return workTypeId;
  }

  public void setWorkTypeId(Long workTypeId) {
    this.workTypeId = workTypeId;
  }

  public Long getCourseId() {
    return courseId;
  }

  public void setCourseId(Long courseId) {
    this.courseId = courseId;
  }

  public Long getKnowledgeId() {
    return knowledgeId;
  }

  public void setKnowledgeId(Long knowledgeId) {
    this.knowledgeId = knowledgeId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public BigDecimal getGetLearnTime() {
    return getLearnTime;
  }

  public void setGetLearnTime(BigDecimal getLearnTime) {
    this.getLearnTime = getLearnTime;
  }

  public Integer getMinMinute() {
    return minMinute;
  }

  public void setMinMinute(Integer minMinute) {
    this.minMinute = minMinute;
  }

  public Integer getIsRequired() {
    return isRequired;
  }

  public void setIsRequired(Integer isRequired) {
    this.isRequired = isRequired;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Integer getSortNumber() {
    return sortNumber;
  }

  public void setSortNumber(Integer sortNumber) {
    this.sortNumber = sortNumber;
  }

  public Integer getScore() {
    return score;
  }

  public void setScore(Integer score) {
    this.score = score;
  }

  public BigDecimal getGetPoint() {
    return getPoint;
  }

  public void setGetPoint(BigDecimal getPoint) {
    this.getPoint = getPoint;
  }

  public Integer getCheckStatus() {
    return checkStatus;
  }

  public void setCheckStatus(Integer checkStatus) {
    this.checkStatus = checkStatus;
  }

  public Integer getFinalCheckStatus() {
    return finalCheckStatus;
  }

  public void setFinalCheckStatus(Integer finalCheckStatus) {
    this.finalCheckStatus = finalCheckStatus;
  }

  public LocalDateTime getCheckTime() {
    return checkTime;
  }

  public void setCheckTime(LocalDateTime checkTime) {
    this.checkTime = checkTime;
  }

  public Long getCheckUserId() {
    return checkUserId;
  }

  public void setCheckUserId(Long checkUserId) {
    this.checkUserId = checkUserId;
  }

  public String getCheckUsername() {
    return checkUsername;
  }

  public void setCheckUsername(String checkUsername) {
    this.checkUsername = checkUsername;
  }

  public LocalDateTime getFinalCheckTime() {
    return finalCheckTime;
  }

  public void setFinalCheckTime(LocalDateTime finalCheckTime) {
    this.finalCheckTime = finalCheckTime;
  }

  public Long getFinalCheckUserId() {
    return finalCheckUserId;
  }

  public void setFinalCheckUserId(Long finalCheckUserId) {
    this.finalCheckUserId = finalCheckUserId;
  }

  public String getFinalCheckUsername() {
    return finalCheckUsername;
  }

  public void setFinalCheckUsername(String finalCheckUsername) {
    this.finalCheckUsername = finalCheckUsername;
  }

}
