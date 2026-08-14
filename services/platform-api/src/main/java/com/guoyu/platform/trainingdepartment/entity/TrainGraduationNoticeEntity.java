package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("train_graduation_notice")
public class TrainGraduationNoticeEntity extends BaseEntity {
  private Long classId;
  private String channel;
  private String title;
  private String body;
  private Integer totalCount;
  private Integer successCount;
  private Integer failCount;
}
