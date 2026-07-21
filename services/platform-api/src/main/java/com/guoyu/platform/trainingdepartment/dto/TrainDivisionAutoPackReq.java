package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TrainDivisionAutoPackReq {

  @NotNull
  private Long planId;
  @NotNull
  private Long trainInfoId;
  /**
   * by_job_level（工种×等级，默认）| by_shift_team | by_job_level_slot（高级时段）| by_roster_order
   */
  private String strategy = "by_job_level";
  @Min(1)
  @Max(200)
  private Integer classMax = 50;
  @Size(max = 80)
  private String classNamePrefix;
  private Boolean submitForCompanyConfirm = true;
  private Boolean excludeNoIdCard = false;
  /** 剔除当月几乎无可上课日（全夜/全休）的人；排班仅过滤，不参与分组 */
  private Boolean useShiftFilter = true;
  /** 缺工种或缺等级时进未分入（默认 true） */
  private Boolean requireJobAndLevel = true;
  @Min(2000)
  @Max(2100)
  private Integer year;
  @Min(1)
  @Max(12)
  private Integer month;
  /** roster 为空时自动解析名单 */
  private Boolean autoParseRoster = true;

  public Long getPlanId() { return planId; }
  public void setPlanId(Long planId) { this.planId = planId; }
  public Long getTrainInfoId() { return trainInfoId; }
  public void setTrainInfoId(Long trainInfoId) { this.trainInfoId = trainInfoId; }
  public String getStrategy() { return strategy; }
  public void setStrategy(String strategy) { this.strategy = strategy; }
  public Integer getClassMax() { return classMax; }
  public void setClassMax(Integer classMax) { this.classMax = classMax; }
  public String getClassNamePrefix() { return classNamePrefix; }
  public void setClassNamePrefix(String classNamePrefix) { this.classNamePrefix = classNamePrefix; }
  public Boolean getSubmitForCompanyConfirm() { return submitForCompanyConfirm; }
  public void setSubmitForCompanyConfirm(Boolean submitForCompanyConfirm) {
    this.submitForCompanyConfirm = submitForCompanyConfirm;
  }
  public Boolean getExcludeNoIdCard() { return excludeNoIdCard; }
  public void setExcludeNoIdCard(Boolean excludeNoIdCard) { this.excludeNoIdCard = excludeNoIdCard; }
  public Boolean getUseShiftFilter() { return useShiftFilter; }
  public void setUseShiftFilter(Boolean useShiftFilter) { this.useShiftFilter = useShiftFilter; }
  public Boolean getRequireJobAndLevel() { return requireJobAndLevel; }
  public void setRequireJobAndLevel(Boolean requireJobAndLevel) {
    this.requireJobAndLevel = requireJobAndLevel;
  }
  public Integer getYear() { return year; }
  public void setYear(Integer year) { this.year = year; }
  public Integer getMonth() { return month; }
  public void setMonth(Integer month) { this.month = month; }
  public Boolean getAutoParseRoster() { return autoParseRoster; }
  public void setAutoParseRoster(Boolean autoParseRoster) { this.autoParseRoster = autoParseRoster; }
}
