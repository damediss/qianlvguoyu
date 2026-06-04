package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_task_assessor")
public class CertificationTaskAssessorEntity extends BaseEntity {
  private Long taskId;
  private Long groupId;
  private Long assessorId;
  private String assessorName;
  private String certificateNo;
  private String taskRole;
  private String roleName;
  private String workTypeScope;
  private LocalDate validUntil;
  private String workStatus;
  private Integer sortOrder;
  private String remark;
}
