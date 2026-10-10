package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_task")
public class CertificationTaskEntity extends BaseEntity {

  private String taskNo;
  private String taskName;
  private String taskManagerName;
  private String companyName;
  private String certificationType;
  private Integer plannedPeopleCount;
  private LocalDate planStartDate;
  private LocalDate planEndDate;
  private LocalDate examDate;
  private String sourceType;
  private String sourceName;
  private String currentPhase;
  private String status;
  private Integer documentCompletedCount;
  private Integer documentTotalCount;
  private Integer phaseCompletedCount;
  private Integer phaseTotalCount;
  private LocalDateTime lastBusinessUpdatedAt;
  private String remark;
}
