package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_task_assessor_group")
public class CertificationTaskAssessorGroupEntity extends BaseEntity {
  private Long taskId;
  private String groupName;
  private Long leaderAssessorId;
  private String workTypeName;
  private String status;
  private String remark;
}
