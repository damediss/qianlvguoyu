package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 师资库证照附件 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("train_teacher_bank_file")
public class TrainTeacherBankFileEntity extends BaseEntity {

  private Long teacherId;
  private String fileType;
  private Long fileId;
  private String legacyUrl;
  private String certNo;
  private Integer sortNo;
  private String remark;
}
