package com.guoyu.platform.certification.info.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_exam_equipment")
public class CertificationInfoExamEquipmentEntity extends BaseEntity {
  private String equipmentType;
  private String assetNo;
  private Integer equipmentQuantity;
  private Long siteId;
  private String siteName;
  private String equipmentModel;
  private LocalDate calibrationValidUntil;
  private String equipmentStatus;
  private Long equipmentPhotoFileId;
  private String equipmentPhotoFileName;
  private String materialsDescription;
  private String auditStatus;
  private LocalDateTime submittedAt;
  private Long submittedBy;
  private LocalDateTime reviewedAt;
  private Long reviewedBy;
  private String reviewerName;
  private String rejectReason;
  private LocalDateTime disabledAt;
  private Long disabledBy;
  private String disableReason;
  private Integer referencedCount;
  private String remark;
}
