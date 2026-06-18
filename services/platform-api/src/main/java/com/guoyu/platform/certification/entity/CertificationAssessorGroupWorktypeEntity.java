package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_assessor_group_worktype")
public class CertificationAssessorGroupWorktypeEntity extends BaseEntity {
  private Long groupId;
  private Long scopeId;
  private String workTypeName;
  private String levelCode;
  private String levelName;
  private Integer sortOrder;
}
