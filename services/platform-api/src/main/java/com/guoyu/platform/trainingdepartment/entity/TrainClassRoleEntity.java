package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 班级岗位角色，对应 train_class_role */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("train_class_role")
public class TrainClassRoleEntity extends BaseEntity {

  private Long classId;
  /** head_teacher / follow_staff / lead_teacher / assistant / monitor */
  private String roleCode;
  /** platform_user / people / teacher_bank */
  private String subjectType;
  private Long subjectId;
  private Long platformUserId;
  private Long peopleId;
  private String displayName;
  private String phone;
  private LocalDate startDate;
  private LocalDate endDate;
  /** active / ended */
  private String status;
}
