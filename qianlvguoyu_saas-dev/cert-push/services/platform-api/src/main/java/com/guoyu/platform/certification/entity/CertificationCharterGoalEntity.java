package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_charter_goal")
public class CertificationCharterGoalEntity extends BaseEntity {

  private Long taskId;
  private Long documentId;
  private String goalName;
  private String goalDescription;
  private LocalDate targetDate;
  private Boolean completed;
  private Integer sortOrder;
}
