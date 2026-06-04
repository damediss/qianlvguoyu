package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_assessor_task_score")
public class CertificationAssessorTaskScoreEntity extends BaseEntity {
  private Long assessorTaskId;
  private Long taskId;
  private Integer stepNo;
  private String stepName;
  private BigDecimal maxScore;
  private BigDecimal score;
  private String pointsJson;
  private String remark;
}
