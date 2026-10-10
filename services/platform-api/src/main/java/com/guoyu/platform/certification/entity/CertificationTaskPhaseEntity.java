package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_task_phase")
public class CertificationTaskPhaseEntity extends BaseEntity {

  private Long taskId;
  private String phaseCode;
  private String phaseName;
  private Integer phaseOrder;
  private String status;
  private LocalDateTime startedAt;
  private LocalDateTime completedAt;
  private Long completedBy;
  private String remark;
}
