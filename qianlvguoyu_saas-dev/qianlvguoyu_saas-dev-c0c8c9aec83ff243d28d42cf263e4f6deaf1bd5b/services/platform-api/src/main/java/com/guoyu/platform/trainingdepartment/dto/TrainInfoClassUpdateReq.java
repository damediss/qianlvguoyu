package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 培训班级更新请求 */
public class TrainInfoClassUpdateReq {

  @NotBlank
  @Size(max = 50)
  /** 标题 */
  private String title;

  /** 人数 */
  private Integer nums;

  /** 开始日期 */
  @Size(max = 10)
  private String dateStart;

  /** 结束日期 */
  @Size(max = 10)
  private String dateEnd;

  /** 文件模板 */
  @Size(max = 100)
  private String fileTemps;

  /** 课程表文件 */
  @Size(max = 255)
  private String kcbiaoFile;

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
