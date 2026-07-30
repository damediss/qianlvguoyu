package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_practical_question_result")
public class CertificationPracticalQuestionResultEntity extends BaseEntity {
  private Long taskId;
  private Long candidateId;
  private Long paperId;
  private Long paperQuestionId;
  private Integer submittedCount;
  private BigDecimal averageScore;
  private BigDecimal finalScore;
  private String status;
  private LocalDateTime calculatedAt;
}
