package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_task_scope")
public class CertificationTaskScopeEntity extends BaseEntity {

  private Long taskId;
  private String certificationType;
  private String workTypeName;
  private String levelCode;
  private String levelName;
  private Integer plannedPeopleCount;
  private Integer sortOrder;
  private String remark;
}

