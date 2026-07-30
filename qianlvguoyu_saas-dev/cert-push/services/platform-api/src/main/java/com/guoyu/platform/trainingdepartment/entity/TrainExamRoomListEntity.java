package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * train_exam_room_list
 */
@TableName("train_exam_room_list")
public class TrainExamRoomListEntity {

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 组织ID */
  private Long orgId;

  /** 组织名称 */
  private String orgName;

  /** 考场类型 */
  private String type;

  /** 考场名称 */
  private String roomName;

  /** 位置 */
  private String position;

  /** 最大容纳人数 */
  private Integer maxNum;

  /** 租户ID */
  private Long tenantId;
  /** 创建时间 */
  private LocalDateTime createTime;
  /** 更新时间 */
  private LocalDateTime updateTime;

  /** 逻辑删除 0-未删除 1-已删除 */
  @TableLogic
  private Integer deleted;
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getOrgId() { return orgId; }
  public void setOrgId(Long orgId) { this.orgId = orgId; }
  public String getOrgName() { return orgName; }
  public void setOrgName(String orgName) { this.orgName = orgName; }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public String getRoomName() { return roomName; }
  public void setRoomName(String roomName) { this.roomName = roomName; }
  public String getPosition() { return position; }
  public void setPosition(String position) { this.position = position; }
  public Integer getMaxNum() { return maxNum; }
  public void setMaxNum(Integer maxNum) { this.maxNum = maxNum; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public LocalDateTime getUpdateTime() { return updateTime; }
  public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
