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
  /** 达标课时（班级统一值，考勤列表展示，仅展示不参与完成率计算） */
  private java.math.BigDecimal passHours;
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
  /** 单位联系人（工作年限证明等） */
  private String companyContactName;
  /** 单位联系方式 */
  private String companyContactPhone;
  /** 1=跟班已手工保存课表，重导开班 Excel 需确认 */
  private Integer scheduleManualEdited;
  /** 最近一次从 kcbiao_file 导入时间 */
  private LocalDateTime scheduleImportedAt;
  /** 企业确认状态 0待确认 1已确认 2已驳回 */
  private Integer companyConfirmStatus;
  private LocalDateTime companyConfirmTime;
  private Long companyConfirmUserId;
  private String companyConfirmUsername;
  private String companyRejectReason;

  /** pending / partial / done */
  private String graduateStatus;
  private LocalDateTime graduateAt;
  private Long certificateTempId;

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
  public java.math.BigDecimal getPassHours() { return passHours; }
  public void setPassHours(java.math.BigDecimal passHours) { this.passHours = passHours; }
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
  public String getCompanyContactName() { return companyContactName; }
  public void setCompanyContactName(String companyContactName) { this.companyContactName = companyContactName; }
  public String getCompanyContactPhone() { return companyContactPhone; }
  public void setCompanyContactPhone(String companyContactPhone) { this.companyContactPhone = companyContactPhone; }
  public Integer getScheduleManualEdited() { return scheduleManualEdited; }
  public void setScheduleManualEdited(Integer scheduleManualEdited) {
    this.scheduleManualEdited = scheduleManualEdited;
  }
  public LocalDateTime getScheduleImportedAt() { return scheduleImportedAt; }
  public void setScheduleImportedAt(LocalDateTime scheduleImportedAt) {
    this.scheduleImportedAt = scheduleImportedAt;
  }
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
  public String getGraduateStatus() { return graduateStatus; }
  public void setGraduateStatus(String graduateStatus) { this.graduateStatus = graduateStatus; }
  public LocalDateTime getGraduateAt() { return graduateAt; }
  public void setGraduateAt(LocalDateTime graduateAt) { this.graduateAt = graduateAt; }
  public Long getCertificateTempId() { return certificateTempId; }
  public void setCertificateTempId(Long certificateTempId) { this.certificateTempId = certificateTempId; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
