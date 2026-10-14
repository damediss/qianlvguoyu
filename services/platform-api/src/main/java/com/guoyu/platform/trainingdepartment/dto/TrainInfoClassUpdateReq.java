package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** 培训班级更新请求 */
public class TrainInfoClassUpdateReq {

  @NotBlank
  @Size(max = 50)
  /** 标题 */
  private String title;

  /** 人数 */
  private Integer nums;

  /** 达标课时：不传表示不修改；传则校验（最多一位小数）并更新 */
  @DecimalMin(value = "0", message = "达标课时不能为负数")
  @Digits(integer = 4, fraction = 1, message = "达标课时最多输入一位小数")
  private BigDecimal passHours;

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

  /** 单位联系人 */
  @Size(max = 64)
  private String companyContactName;

  /** 单位联系方式 */
  @Size(max = 32)
  private String companyContactPhone;

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

  public BigDecimal getPassHours() {
    return passHours;
  }

  public void setPassHours(BigDecimal passHours) {
    this.passHours = passHours;
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

  public String getCompanyContactName() {
    return companyContactName;
  }

  public void setCompanyContactName(String companyContactName) {
    this.companyContactName = companyContactName;
  }

  public String getCompanyContactPhone() {
    return companyContactPhone;
  }

  public void setCompanyContactPhone(String companyContactPhone) {
    this.companyContactPhone = companyContactPhone;
  }

}
