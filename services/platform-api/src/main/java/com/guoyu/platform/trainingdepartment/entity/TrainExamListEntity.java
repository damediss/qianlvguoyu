package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 考试计划
 */
@TableName("train_exam_list")
public class TrainExamListEntity {

  /**  主键ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 标题 */
  private String title;
  /** 等级 */
  private String level;
  /** 培训ID */
  private Long trainId;
  /** 组织ID */
  private Long orgId;
  /** 组织名称 */
  private String orgName;
  /** 0线上 1线下 */
  private Integer type;
  /** 0内部 1外部 */
  private Integer source;
  /** 监考人ID */
  private Long invigilateUserId;
  /** 监考人 */
  private String invigilateUsername;
  /** 考评人ID */
  private Long evaluateUserId;
  /** 考评人 */
  private String evaluateUsername;
  /** 督导员ID */
  private Long watcherUserId;
  /** 督导员 */
  private String watcherUsername;
  /** 考务人员ID */
  private Long staffUserId;
  /** 考务人员 */
  private String staffUsername;
  /** 命题人员ID */
  private Long propositionUserId;
  /** 命题人员 */
  private String propositionUsername;
  /** 阅卷人员ID */
  private Long markingUserId;
  /** 阅卷人员 */
  private String markingUsername;
  /** 试卷ID */
  private Long examId;
  /** 考试日期 */
  private String examDate;
  /** 0截止日期 1时间段 */
  private Integer examDateMode;
  /** 考试日期列表 */
  private String examDateList;
  /** 考试时间(分钟) */
  private Integer examTime;
  /** 是否需要先完成游戏学 */
  private Integer needFinishAdventure;
  /** 是否需要先完成常规学 */
  private Integer needFinishNormal;
  /** 用户ID */
  private Long userId;
  /** 排序号 */
  private Integer sortNumber;
  /** 租户ID */
  private Long tenantId;
  /** 机构ID */
  private Long jigouId;
  /** 创建时间 */
  private LocalDateTime createTime;

  /**  逻辑删除标记 0-未删除 1-已删除 */
  @TableLogic
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
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
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public Long getJigouId() { return jigouId; }
  public void setJigouId(Long jigouId) { this.jigouId = jigouId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
