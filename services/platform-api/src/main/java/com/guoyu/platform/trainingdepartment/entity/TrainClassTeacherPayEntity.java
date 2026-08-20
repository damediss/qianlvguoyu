package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 跟班本班教师课酬（元/天） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("train_class_teacher_pay")
public class TrainClassTeacherPayEntity extends BaseEntity {

  private Long classId;
  private String teacherName;
  private String teachForm;
  private BigDecimal ratePerDay;
  private String remark;
}
