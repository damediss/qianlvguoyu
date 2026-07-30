package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** 考试计划创建请求 */
public class TrainExamListCreateReq {

  @Size(max = 255)
  /** 标题 */
  private String title;

  @Size(max = 255)
  /** 等级 */
  private String level;

  /** 培训ID */
  private Long trainId;
  /** 组织ID */
  private Long orgId;

  @Size(max = 255)
  /** 组织名称 */
  private String orgName;

  /** 0线上 1线下 */
  private Integer type;

  /** 0内部 1外部 */
  private Integer source;

  private Long invigilateUserId;

  @Size(max = 255)
  private String invigilateUsername;

  private Long evaluateUserId;

  @Size(max = 255)
  private String evaluateUsername;

  private Long watcherUserId;

  @Size(max = 255)
  private String watcherUsername;

  private Long staffUserId;

  @Size(max = 255)
  private String staffUsername;

  private Long propositionUserId;

  @Size(max = 255)
  private String propositionUsername;

  private Long markingUserId;

  @Size(max = 255)
  private String markingUsername;

  private Long examId;

  @Size(max = 20)
  private String examDate;

  /** 0截止日期 1时间段 */
  private Integer examDateMode;

  private String examDateList;

  /** 考试时间(分钟) */
  private Integer examTime;

  private Integer needFinishAdventure;
  private Integer needFinishNormal;
  private Long jigouId;
  /** 排序号 */
  private Integer sortNumber;

  /** 课程ID列表 */
  private java.util.List<Long> courseIdList;

  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getLevel() { return level; }
  public void setLevel(String level) { this.level = level; }
  public Long getTrainId() { return trainId; }
  public void setTrainId(Long trainId) { this.trainId = trainId; }
  public Long getOrgId() { return orgId; }
  public void setOrgId(Long orgId) { this.orgId = orgId; }
  public String getOrgName() { return orgName; }
  public void setOrgName(String orgName) { this.orgName = orgName; }
  public Integer getType() { return type; }
  public void setType(Integer type) { this.type = type; }
  public Integer getSource() { return source; }
  public void setSource(Integer source) { this.source = source; }
  public Long getInvigilateUserId() { return invigilateUserId; }
  public void setInvigilateUserId(Long invigilateUserId) { this.invigilateUserId = invigilateUserId; }
  public String getInvigilateUsername() { return invigilateUsername; }
  public void setInvigilateUsername(String invigilateUsername) { this.invigilateUsername = invigilateUsername; }
  public Long getEvaluateUserId() { return evaluateUserId; }
  public void setEvaluateUserId(Long evaluateUserId) { this.evaluateUserId = evaluateUserId; }
  public String getEvaluateUsername() { return evaluateUsername; }
  public void setEvaluateUsername(String evaluateUsername) { this.evaluateUsername = evaluateUsername; }
  public Long getWatcherUserId() { return watcherUserId; }
  public void setWatcherUserId(Long watcherUserId) { this.watcherUserId = watcherUserId; }
  public String getWatcherUsername() { return watcherUsername; }
  public void setWatcherUsername(String watcherUsername) { this.watcherUsername = watcherUsername; }
  public Long getStaffUserId() { return staffUserId; }
  public void setStaffUserId(Long staffUserId) { this.staffUserId = staffUserId; }
  public String getStaffUsername() { return staffUsername; }
  public void setStaffUsername(String staffUsername) { this.staffUsername = staffUsername; }
  public Long getPropositionUserId() { return propositionUserId; }
  public void setPropositionUserId(Long propositionUserId) { this.propositionUserId = propositionUserId; }
  public String getPropositionUsername() { return propositionUsername; }
  public void setPropositionUsername(String propositionUsername) { this.propositionUsername = propositionUsername; }
  public Long getMarkingUserId() { return markingUserId; }
  public void setMarkingUserId(Long markingUserId) { this.markingUserId = markingUserId; }
  public String getMarkingUsername() { return markingUsername; }
  public void setMarkingUsername(String markingUsername) { this.markingUsername = markingUsername; }
  public Long getExamId() { return examId; }
  public void setExamId(Long examId) { this.examId = examId; }
  public String getExamDate() { return examDate; }
  public void setExamDate(String examDate) { this.examDate = examDate; }
  public Integer getExamDateMode() { return examDateMode; }
  public void setExamDateMode(Integer examDateMode) { this.examDateMode = examDateMode; }
  public String getExamDateList() { return examDateList; }
  public void setExamDateList(String examDateList) { this.examDateList = examDateList; }
  public Integer getExamTime() { return examTime; }
  public void setExamTime(Integer examTime) { this.examTime = examTime; }
  public Integer getNeedFinishAdventure() { return needFinishAdventure; }
  public void setNeedFinishAdventure(Integer needFinishAdventure) { this.needFinishAdventure = needFinishAdventure; }
  public Integer getNeedFinishNormal() { return needFinishNormal; }
  public void setNeedFinishNormal(Integer needFinishNormal) { this.needFinishNormal = needFinishNormal; }
  public Long getJigouId() { return jigouId; }
  public void setJigouId(Long jigouId) { this.jigouId = jigouId; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public java.util.List<Long> getCourseIdList() { return courseIdList; }
  public void setCourseIdList(java.util.List<Long> courseIdList) { this.courseIdList = courseIdList; }
}
