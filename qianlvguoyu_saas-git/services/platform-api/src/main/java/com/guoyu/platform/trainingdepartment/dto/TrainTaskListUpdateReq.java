package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** 知识点任务列表更新请求 */
public class TrainTaskListUpdateReq {

  /** 工种ID */
  private Long workTypeId;

  /** 课程ID */
  private Long courseId;

  /** 知识点ID */
  private Long knowledgeId;

  @NotBlank
  @Size(max = 255)
  /** 标题 */
  private String title;

  /** 类型 */
  @Size(max = 255)
  /** 类型 */
  private String type;

  @Size(max = 2000)
  /** 内容 */
  private String content;

  /** 学习时长 */
  private BigDecimal getLearnTime;

  /** 最低分钟数 */
  private Integer minMinute;

  /** 是否必修 */
  private Integer isRequired;

  /** 排序号 */
  private Integer sortNumber;

  /** 分数 */
  private Integer score;

  /** 获得积分 */
  private BigDecimal getPoint;

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

}
