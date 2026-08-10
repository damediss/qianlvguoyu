package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 跟班作业作答明细 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("train_follow_task_answer")
public class TrainFollowTaskAnswerEntity extends BaseEntity {

  private Long taskId;
  private Long itemId;
  private Long questionId;
  private Long peopleId;
  private String answerJson;
  private Integer isCorrect;
  private BigDecimal score;
}
