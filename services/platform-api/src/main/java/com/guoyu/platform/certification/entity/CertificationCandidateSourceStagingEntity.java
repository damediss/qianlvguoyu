package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_candidate_source_staging")
public class CertificationCandidateSourceStagingEntity extends BaseEntity {

  private Long taskId;
  private Long taskScopeId;
  private Long candidateId;
  private Long importBatchId;
  private String sourceType;
  private String externalRefTable;
  private Long externalRefId;
  private Long signUpId;
  private String promoteStatus;
  private String promoteMessage;
  private LocalDateTime promotedAt;

  private String candidateName;
  private String gender;
  private LocalDate birthDate;
  private String education;
  private LocalDate graduateDate;
  private String school;
  private String major;
  private String graduationCertificateNo;
  private String nation;
  private String politicalStatus;
  private String idCardNo;
  private String phone;
  private String homeAddress;

  private String companyName;
  private String positionName;
  private String companyAddress;
  private String zipCode;
  private String companyCreditCode;
  private String industrialPark;

  private LocalDate workStartDate;
  private LocalDate entryDate;
  private LocalDate laborContractStartDate;
  private LocalDate laborContractEndDate;
  private String responsibleName;
  private String responsiblePhone;
  private String originalWorkTypeName;
  private String originalLevelName;
  private String originalCertNo;
  private Long applyWorkTypeId;
  private String applyWorkTypeName;
  private Long certWorkTypeId;
  private String certWorkTypeName;
  private String workDirection;
  private String workTypeCode;
  private String applyLevelCode;
  private String applyLevelName;
  private String certLevelName;
  private String applyWorkYears;
  private String applyCondition;
  private String workResume;

  private BigDecimal theoryScore;
  private BigDecimal practicalScore;
  private String trainingSummary;

  private String photoUrl;
  private String photoAltUrl;
  private String idCardFrontUrl;
  private String idCardBackUrl;
  private String originalCertImageUrl;
  private String educationCertImageUrl;

  private Long platformUserId;
  private Long userId;
  private String rawPayload;
  private String remark;
}
