package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 培训班级创建请求 */
public class TrainInfoClassCreateReq {

  /** 开班信息ID */
  private Long trainInfoId;

  @NotBlank
  @Size(max = 50)
  private String title;

  /** 人数 */
  private Integer nums;

  private Long companyId;

  private Long workTypeId;

  /** 开始日期 */
  @Size(max = 10)
  private String dateStart;

  /** 结束日期 */
  @Size(max = 10)
  private String dateEnd;

  /** 添加时间 */
  private Integer addtime;

  /** 文件模板 */
  @Size(max = 100)
  private String fileTemps;

  /** 课程表文件 */
  @Size(max = 255)
  private String kcbiaoFile;

  public Long getTrainInfoId() {
    return trainInfoId;
  }

  public void setTrainInfoId(Long trainInfoId) {
    this.trainInfoId = trainInfoId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Integer getNums() {
    return nums;
  }

  public void setNums(Integer nums) {
    this.nums = nums;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public void setCompanyId(Long companyId) {
    this.companyId = companyId;
  }

  public Long getWorkTypeId() {
    return workTypeId;
  }

  public void setWorkTypeId(Long workTypeId) {
    this.workTypeId = workTypeId;
  }

  public String getDateStart() {
    return dateStart;
  }

  public void setDateStart(String dateStart) {
    this.dateStart = dateStart;
  }

  public String getDateEnd() {
    return dateEnd;
  }

  public void setDateEnd(String dateEnd) {
    this.dateEnd = dateEnd;
  }

  public Integer getAddtime() {
    return addtime;
  }

  public void setAddtime(Integer addtime) {
    this.addtime = addtime;
  }

  public String getFileTemps() {
    return fileTemps;
  }

  public void setFileTemps(String fileTemps) {
    this.fileTemps = fileTemps;
  }

  public String getKcbiaoFile() {
    return kcbiaoFile;
  }

  public void setKcbiaoFile(String kcbiaoFile) {
    this.kcbiaoFile = kcbiaoFile;
  }

}
