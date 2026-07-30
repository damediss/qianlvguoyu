package com.guoyu.platform.certification.info.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_info_audit_record")
public class CertificationInfoAuditRecordEntity extends BaseEntity {
  private String ownerType;
  private Long ownerId;
  private String action;
  private String beforeStatus;
  private String afterStatus;
  private Long operatorId;
  private String operatorName;
  private String comment;
  private String snapshotJson;
  private String processInstanceId;
}
