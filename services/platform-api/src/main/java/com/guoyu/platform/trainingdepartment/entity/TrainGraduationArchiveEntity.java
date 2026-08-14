package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("train_graduation_archive")
public class TrainGraduationArchiveEntity extends BaseEntity {
  private Long classId;
  private Long fileId;
  private String filePath;
  private Integer versionNo;
  private String remark;
}
