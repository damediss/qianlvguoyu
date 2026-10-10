package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_task_member")
public class CertificationTaskMemberEntity extends BaseEntity {

  private Long taskId;
  private Long documentId;
  private Long userId;
  private String memberName;
  private String roleCode;
  private String roleName;
  private String departmentName;
  private String responsibility;
  private LocalDate startDate;
  private LocalDate endDate;
  private String inputFrequency;
  private BigDecimal workloadPercent;
  private String phone;
  private String maskedPhone;
  private Boolean coreRole;
  private Integer sortOrder;
  private String remark;
}
