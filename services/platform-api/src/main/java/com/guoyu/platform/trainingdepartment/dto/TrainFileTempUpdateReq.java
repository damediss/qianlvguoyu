package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** train_file_temp 更新请求 */
public class TrainFileTempUpdateReq {

  @Size(max = 255)
  /** 模板类型 */
  private String type;

  @Size(max = 255)
  /** 模板名称 */
  private String title;

  @Size(max = 255)
  /** 城市/地区 */
  private String city;

  @Size(max = 255)
  /** 源文件路径 */
  private String pathSource;
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getCity() { return city; }
  public void setCity(String city) { this.city = city; }
  public String getPathSource() { return pathSource; }
  public void setPathSource(String pathSource) { this.pathSource = pathSource; }
}
