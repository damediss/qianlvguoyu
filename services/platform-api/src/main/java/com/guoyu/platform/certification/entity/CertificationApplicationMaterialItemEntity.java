package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_application_material_item")
public class CertificationApplicationMaterialItemEntity extends BaseEntity {
  private Long taskId;
  private Long batchId;
  private Long candidateId;
  private Long templateId;
  private String itemCode;
  private String itemName;
  private Boolean required;
  private Long fileId;
  private String fileName;
  private Integer versionNo;
  private String status;
  private String remark;
}
