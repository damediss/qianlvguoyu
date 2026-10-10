package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/** 批量分配班级请求 */
public class AssignClassReq {

  /** 目标班级ID */
  @NotNull
  /** 班级ID */
  private Long classId;

  /** 用户ID列表 */
  @NotNull
  /** 用户ID列表 */
  private List<Long> userIds;

  public Long getClassId() {
    return classId;
  }

  public void setClassId(Long classId) {
    this.classId = classId;
  }

  public List<Long> getUserIds() {
    return userIds;
  }

  public void setUserIds(List<Long> userIds) {
    this.userIds = userIds;
  }
}
