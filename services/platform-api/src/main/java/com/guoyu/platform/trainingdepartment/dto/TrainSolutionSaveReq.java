package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** 培训方案保存请求（含经费明细） */
public class TrainSolutionSaveReq {

  private Long id;

  @NotBlank
  private String title;

  @NotNull
  private Long companyId;

  private String companyName;

  @NotNull
  private Long traininfoId;

  private String filePath;

  /** 经费明细列表（全量替换） */
  private List<TrainSolutionBadgeItem> badgeList;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public Long getCompanyId() { return companyId; }
  public void setCompanyId(Long companyId) { this.companyId = companyId; }
  public String getCompanyName() { return companyName; }
  public void setCompanyName(String companyName) { this.companyName = companyName; }
  public Long getTraininfoId() { return traininfoId; }
  public void setTraininfoId(Long traininfoId) { this.traininfoId = traininfoId; }
  public String getFilePath() { return filePath; }
  public void setFilePath(String filePath) { this.filePath = filePath; }
  public List<TrainSolutionBadgeItem> getBadgeList() { return badgeList; }
  public void setBadgeList(List<TrainSolutionBadgeItem> badgeList) { this.badgeList = badgeList; }
}
