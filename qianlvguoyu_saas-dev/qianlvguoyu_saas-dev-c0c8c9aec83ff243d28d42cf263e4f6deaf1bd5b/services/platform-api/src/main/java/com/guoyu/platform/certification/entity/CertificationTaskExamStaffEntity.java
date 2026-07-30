package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_task_exam_staff")
public class CertificationTaskExamStaffEntity extends BaseEntity {
  private Long taskId;
  private Long staffId;
  private String staffName;
  private String phone;
  private String maskedPhone;
  private String remark;
  private String status;
  private String taskRole;
  private String roleName;
  private Integer isLeader;
  private Integer sortOrder;
}
