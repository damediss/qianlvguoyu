package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 跟班作业题目快照 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("train_follow_task_question")
public class TrainFollowTaskQuestionEntity extends BaseEntity {

  private Long taskId;
  private Integer sortNumber;
  /** single / multi / judge */
  private String questionType;
  private String stem;
  private String optionsJson;
  private String answerJson;
  private BigDecimal score;
  private Long sourceExamDetailId;
  private String analysis;
}
