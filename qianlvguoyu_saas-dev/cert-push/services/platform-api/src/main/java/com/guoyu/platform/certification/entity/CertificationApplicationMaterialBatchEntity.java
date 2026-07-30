package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_application_material_batch")
public class CertificationApplicationMaterialBatchEntity extends BaseEntity {
  private Long taskId;
  private String batchNo;
  private String batchType;
  private String generateScope;
  private Integer candidateCount;
  private Integer successCount;
  private Integer failedCount;
  private String status;
  private Long packageFileId;
  private String packageFileName;
  private String processInstanceId;
  private LocalDateTime submittedAt;
  private LocalDateTime approvedAt;
  private String rejectReason;
  private String remark;
}
