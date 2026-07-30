package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_material_template")
public class CertificationMaterialTemplateEntity extends BaseEntity {
  private String templateScope;
  private String itemCode;
  private String itemName;
  private String itemGroup;
  private Boolean required;
  private String requiredRule;
  private Long templateFileId;
  private String acceptFileTypes;
  private Integer maxFileSizeMb;
  private Boolean enabled;
  private Integer sortOrder;
  private String remark;
}
