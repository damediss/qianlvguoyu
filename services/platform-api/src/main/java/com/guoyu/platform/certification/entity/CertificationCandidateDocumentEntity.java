package com.guoyu.platform.certification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("certification_candidate_document")
public class CertificationCandidateDocumentEntity extends BaseEntity {
  private Long taskId;
  private Long candidateId;
  private Long templateId;
  private String docCode;
  private String docName;
  private Boolean required;
  private Long fileId;
  private Long fileRefId;
  private String fileName;
  private String fileType;
  private Integer versionNo;
  private String status;
  private LocalDateTime uploadedAt;
  private Long uploadedBy;
  private LocalDateTime reviewedAt;
  private Long reviewedBy;
  private String reviewComment;
  private String remark;
}
