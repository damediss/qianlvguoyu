package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("train_follow_survey_response")
public class TrainFollowSurveyResponseEntity extends BaseEntity {
  private Long classId;
  private String bizDate;
  private String respondentName;
  private BigDecimal score;
  private String commentText;
  /** admin / app / import */
  private String source;
}
