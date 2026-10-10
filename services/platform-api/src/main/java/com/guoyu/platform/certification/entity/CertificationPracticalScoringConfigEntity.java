package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_practical_scoring_config")
public class CertificationPracticalScoringConfigEntity extends BaseEntity {
  private Long taskId;
  private Long scopeId;
  private Long candidateId;
  private Long workTypeId;
  private String workTypeName;
  private String levelCode;
  private String levelName;
  private LocalDate examDate;
  private Long paperId;
  private Long groupId;
  private String status;
}
