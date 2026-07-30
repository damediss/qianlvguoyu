package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_exam_material_item")
public class CertificationExamMaterialItemEntity extends BaseEntity {
  private Long taskId;
  private Long templateId;
  private String itemCode;
  private String itemName;
  private Integer required;
  private Integer requireFile;
  private Integer expectedQuantity;
  private Integer actualQuantity;
  private String unit;
  private String responsiblePerson;
  private Long fileId;
  private String fileName;
  private Integer versionNo;
  private String status;
  private String importedByName;
  private LocalDateTime importedAt;
  private String reviewedByName;
  private LocalDateTime reviewedAt;
  private String reviewComment;
  private Integer locked;
  private String remark;
}
