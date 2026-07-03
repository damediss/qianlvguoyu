package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_practical_candidate_result")
public class CertificationPracticalCandidateResultEntity extends BaseEntity {
  private Long taskId;
  private Long candidateId;
  private Integer questionCount;
  private Integer completedQuestionCount;
  private BigDecimal totalScore;
  private String status;
  private LocalDateTime calculatedAt;
}
