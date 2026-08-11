package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 跟班作业学员任务态 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("train_follow_task_item")
public class TrainFollowTaskItemEntity extends BaseEntity {

  private Long taskId;
  private Long peopleId;
  private String userName;
  /** pending / submitted / graded */
  private String status;
  private BigDecimal score;
  private Integer passed;
  private LocalDateTime submittedAt;
  private LocalDateTime gradedAt;
  private Long gradedBy;

  @TableField("comment_text")
  private String comment;

  /** 首次打开试卷时间 */
  private LocalDateTime startedAt;

  /** 答题时长（秒）= submitted_at - started_at */
  private Integer durationSeconds;
}
