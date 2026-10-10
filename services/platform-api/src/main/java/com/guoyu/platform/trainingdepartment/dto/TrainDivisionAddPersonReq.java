package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 草稿班内手填加人（姓名必填，身份证强烈建议）。 */
public class TrainDivisionAddPersonReq {

  @NotBlank
  @Size(max = 64)
  private String personName;
  @Size(max = 32)
  private String idCard;
  @Size(max = 32)
  private String phone;
  @Size(max = 64)
  private String jobName;
  @Size(max = 64)
  private String levelName;
  @Size(max = 128)
  private String workshop;
  @Size(max = 64)
  private String shiftTeam;

  public String getPersonName() { return personName; }
  public void setPersonName(String personName) { this.personName = personName; }
  public String getIdCard() { return idCard; }
  public void setIdCard(String idCard) { this.idCard = idCard; }
  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }
  public String getJobName() { return jobName; }
  public void setJobName(String jobName) { this.jobName = jobName; }
  public String getLevelName() { return levelName; }
  public void setLevelName(String levelName) { this.levelName = levelName; }
  public String getWorkshop() { return workshop; }
  public void setWorkshop(String workshop) { this.workshop = workshop; }
  public String getShiftTeam() { return shiftTeam; }
  public void setShiftTeam(String shiftTeam) { this.shiftTeam = shiftTeam; }
}
