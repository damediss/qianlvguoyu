package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 培训班级表
 */
@TableName("train_info_class")
public class TrainInfoClassEntity {

  /**  主键ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 开班信息ID */
  private Long trainInfoId;
  /** 班级标题 */
  private String title;
  /** 人数 */
  private Integer nums;
  /** 企业ID */
  private Long companyId;
  /** 工种ID */
  private Long workTypeId;
  /** 工种等级 1-5 */
  private Integer workTypeLevel;
  /** 开始日期 */
  private String dateStart;
  /** 结束日期 */
  private String dateEnd;
  /** 添加时间 */
  private Integer addtime;
  /** 文件模板 */
  private String fileTemps;
  /** 课表文件 */
  private String kcbiaoFile;
  /** 企业确认状态 0待确认 1已确认 2已驳回 */
  private Integer companyConfirmStatus;
  private LocalDateTime companyConfirmTime;
  private Long companyConfirmUserId;
  private String companyConfirmUsername;
  private String companyRejectReason;

  /**  逻辑删除标记 0-未删除 1-已删除 */
  @TableLogic
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getTrainInfoId() { return trainInfoId; }
  public void setTrainInfoId(Long trainInfoId) { this.trainInfoId = trainInfoId; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public Integer getNums() { return nums; }
  public void setNums(Integer nums) { this.nums = nums; }
  public Long getCompanyId() { return companyId; }
  public void setCompanyId(Long companyId) { this.companyId = companyId; }
  public Long getWorkTypeId() { return workTypeId; }
  public void setWorkTypeId(Long workTypeId) { this.workTypeId = workTypeId; }
  public Integer getWorkTypeLevel() { return workTypeLevel; }
  public void setWorkTypeLevel(Integer workTypeLevel) { this.workTypeLevel = workTypeLevel; }
  public String getDateStart() { return dateStart; }
  public void setDateStart(String dateStart) { this.dateStart = dateStart; }
  public String getDateEnd() { return dateEnd; }
  public void setDateEnd(String dateEnd) { this.dateEnd = dateEnd; }
  public Integer getAddtime() { return addtime; }
  public void setAddtime(Integer addtime) { this.addtime = addtime; }
  public String getFileTemps() { return fileTemps; }
  public void setFileTemps(String fileTemps) { this.fileTemps = fileTemps; }
  public String getKcbiaoFile() { return kcbiaoFile; }
  public void setKcbiaoFile(String kcbiaoFile) { this.kcbiaoFile = kcbiaoFile; }
  public Integer getCompanyConfirmStatus() { return companyConfirmStatus; }
  public void setCompanyConfirmStatus(Integer companyConfirmStatus) { this.companyConfirmStatus = companyConfirmStatus; }
  public LocalDateTime getCompanyConfirmTime() { return companyConfirmTime; }
  public void setCompanyConfirmTime(LocalDateTime companyConfirmTime) { this.companyConfirmTime = companyConfirmTime; }
  public Long getCompanyConfirmUserId() { return companyConfirmUserId; }
  public void setCompanyConfirmUserId(Long companyConfirmUserId) { this.companyConfirmUserId = companyConfirmUserId; }
  public String getCompanyConfirmUsername() { return companyConfirmUsername; }
  public void setCompanyConfirmUsername(String companyConfirmUsername) { this.companyConfirmUsername = companyConfirmUsername; }
  public String getCompanyRejectReason() { return companyRejectReason; }
  public void setCompanyRejectReason(String companyRejectReason) { this.companyRejectReason = companyRejectReason; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
