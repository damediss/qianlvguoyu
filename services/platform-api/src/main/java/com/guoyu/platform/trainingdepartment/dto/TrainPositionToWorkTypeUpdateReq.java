package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** train_position_to_work_type 更新请求 */
public class TrainPositionToWorkTypeUpdateReq {

  @Size(max = 255)
  /** 岗位名称 */
  private String positionName;

  @Size(max = 255)
  /** 工种ID */
  private Long workTypeId;
  public String getPositionName() { return positionName; }
  public void setPositionName(String positionName) { this.positionName = positionName; }
  public Long getWorkTypeId() { return workTypeId; }
  public void setWorkTypeId(Long workTypeId) { this.workTypeId = workTypeId; }
}
