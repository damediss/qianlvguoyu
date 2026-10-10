package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** train_exam_room_list 创建请求 */
public class TrainExamRoomListCreateReq {

  @Size(max = 255)
  /** 组织ID */
  private Long orgId;

  @Size(max = 255)
  /** 组织名称 */
  private String orgName;

  @Size(max = 255)
  /** 考场类型 */
  private String type;

  @Size(max = 255)
  /** 考场名称 */
  private String roomName;

  @Size(max = 255)
  /** 位置 */
  private String position;

  @Size(max = 255)
  /** 最大容纳人数 */
  private Integer maxNum;

  public Long getOrgId() {
    return orgId;
  }

  public void setOrgId(Long orgId) {
    this.orgId = orgId;
  }

  public String getOrgName() {
    return orgName;
  }

  public void setOrgName(String orgName) {
    this.orgName = orgName;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getRoomName() {
    return roomName;
  }

  public void setRoomName(String roomName) {
    this.roomName = roomName;
  }

  public String getPosition() {
    return position;
  }

  public void setPosition(String position) {
    this.position = position;
  }

  public Integer getMaxNum() {
    return maxNum;
  }

  public void setMaxNum(Integer maxNum) {
    this.maxNum = maxNum;
  }
}
