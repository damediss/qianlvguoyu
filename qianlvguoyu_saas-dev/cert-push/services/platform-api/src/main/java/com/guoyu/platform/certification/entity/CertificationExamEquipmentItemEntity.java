package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_exam_equipment_item")
public class CertificationExamEquipmentItemEntity extends BaseEntity {
  private Long taskId;
  private Long templateId;
  private String itemCode;
  private String itemName;
  private Long examSiteId;
  private String examSiteName;
  private String modelSpec;
  private Integer required;
  private Integer expectedQuantity;
  private Integer actualQuantity;
  private String unit;
  private String responsiblePerson;
  private String status;
  private String checkResult;
  private String exceptionDescription;
  private String exceptionStatus;
  private String checkedByName;
  private LocalDateTime checkedAt;
  private Long attachmentFileId;
  private String attachmentFileName;
  private String reviewComment;
  private Integer locked;
  private String remark;
}
