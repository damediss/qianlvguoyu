package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_candidate_exam_session")
public class CertificationCandidateExamSessionEntity extends BaseEntity {
  private Long taskId;
  private Long candidateId;
  private Long examSessionId;
  private String participationStatus;
  private String collectionStatus;
  private Long collectionSourceSessionId;
  private String exemptReason;
  private String remark;
}
