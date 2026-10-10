package com.guoyu.platform.certification.dto;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class CertificationDashboardQueryReq {

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate startDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate endDate;

  private String dateMode;
  private String currentPhase;
  private String status;
  private String certificationType;
  private String workTypeName;
  private String levelName;

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public void setEndDate(LocalDate endDate) {
    this.endDate = endDate;
  }

  public String getDateMode() {
    return dateMode;
  }

  public void setDateMode(String dateMode) {
    this.dateMode = dateMode;
  }

  public String getCurrentPhase() {
    return currentPhase;
  }

  public void setCurrentPhase(String currentPhase) {
    this.currentPhase = currentPhase;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getCertificationType() {
    return certificationType;
  }

  public void setCertificationType(String certificationType) {
    this.certificationType = certificationType;
  }

  public String getWorkTypeName() {
    return workTypeName;
  }

  public void setWorkTypeName(String workTypeName) {
    this.workTypeName = workTypeName;
  }

  public String getLevelName() {
    return levelName;
  }

  public void setLevelName(String levelName) {
    this.levelName = levelName;
  }
}
