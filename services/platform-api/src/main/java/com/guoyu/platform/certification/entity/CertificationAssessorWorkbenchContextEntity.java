package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_assessor_workbench_context")
public class CertificationAssessorWorkbenchContextEntity extends BaseEntity {

  private Long leaderAssessorId;
  private Long activeTaskId;
}
