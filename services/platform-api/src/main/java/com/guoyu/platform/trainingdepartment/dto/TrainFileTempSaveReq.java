package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;

/** 材料模板保存（对齐 PHP file-temp-save） */
public class TrainFileTempSaveReq {

  private Long id;
  /** 0=认定 1=培训 */
  private Integer type = 1;
  private String city;
  @NotBlank
  private String title;
  @NotBlank
  private String pathSource;
  private String pathUsed;
  private String ext;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Integer getType() { return type; }
  public void setType(Integer type) { this.type = type; }
  public String getCity() { return city; }
  public void setCity(String city) { this.city = city; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getPathSource() { return pathSource; }
  public void setPathSource(String pathSource) { this.pathSource = pathSource; }
  public String getPathUsed() { return pathUsed; }
  public void setPathUsed(String pathUsed) { this.pathUsed = pathUsed; }
  public String getExt() { return ext; }
  public void setExt(String ext) { this.ext = ext; }
}
