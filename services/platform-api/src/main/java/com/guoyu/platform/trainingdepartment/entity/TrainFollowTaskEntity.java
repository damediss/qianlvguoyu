package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 跟班作业/随堂练任务 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("train_follow_task")
public class TrainFollowTaskEntity extends BaseEntity {

  private Long classId;
  private Long trainInfoId;
  private Long scheduleId;
  private String bizDate;
  /** homework / practice */
  private String taskType;
  private String title;
  private String description;
  private LocalDateTime deadlineAt;
  /** draft / published / closed */
  private String status;
  private BigDecimal passScore;
  /** submitted / score */
  private String passRule;
  private Integer showAnalysisAfterSubmit;
  private LocalDateTime publishedAt;
}
