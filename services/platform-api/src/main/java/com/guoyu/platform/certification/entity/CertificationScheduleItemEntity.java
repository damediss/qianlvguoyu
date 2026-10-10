package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_schedule_item")
public class CertificationScheduleItemEntity extends BaseEntity {

  private Long taskId;
  private Long documentId;
  private Integer dayNo;
  private LocalDate planDate;
  private LocalDate startDate;
  private LocalDate endDate;
  private String itemName;
  private String itemDescription;
  private Boolean required;
  private Boolean completed;
  private Long externalContactMemberId;
  private Long certificationOwnerMemberId;
  private Integer sortOrder;
  private String remark;
}
