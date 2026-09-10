package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 补贴材料填数模板库 */
@Getter
@Setter
@TableName("train_subsidy_material_template")
public class TrainSubsidyMaterialTemplateEntity {

  @TableId(type = IdType.AUTO)
  private Long id;

  /** platform / tenant / class */
  private String scope;
  private Long tenantId;
  private Long classId;
  private String materialType;
  private String slotCode;
  private String regionCode;
  private String trainMode;
  private Integer policyYear;
  private String title;
  private String fileFormat;
  private Long templateFileId;
  private String templateFilePath;
  private String fieldSchemaJson;
  private Integer parseVersion;
  /** draft / active / retired */
  private String status;
  private String remark;
  private Long createdBy;
  private Long updatedBy;

  @TableLogic
  private Integer deleted;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
