package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_task_equipment")
public class CertificationTaskEquipmentEntity extends BaseEntity {
  private Long taskId;
  private Long equipmentId;
  private String equipmentName;
  private String equipmentNo;
  private Integer equipmentQuantity;
  private String status;
  private String remark;
  private Integer sortOrder;
}
