package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** train_base_data 创建请求 */
public class TrainBaseDataCreateReq {

  @Size(max = 255)
  /** 配置键 */
  private String dataKey;

  @Size(max = 255)
  /** 配置值 */
  private String dataValue;

  @Size(max = 255)
  /** 数据类型 */
  private String dataType;

  @Size(max = 255)
  /** 描述 */
  private String description;

  public String getDataKey() {
    return dataKey;
  }

  public void setDataKey(String dataKey) {
    this.dataKey = dataKey;
  }

  public String getDataValue() {
    return dataValue;
  }

  public void setDataValue(String dataValue) {
    this.dataValue = dataValue;
  }

  public String getDataType() {
    return dataType;
  }

  public void setDataType(String dataType) {
    this.dataType = dataType;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
