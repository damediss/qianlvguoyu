package com.guoyu.platform.certification.mapper;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CandidateDocRow {
  private Long id;
  private Long candidateId;
  private Long tenantId;
  private Long taskId;
  private Long templateId;
  private String docCode;
  private String docName;
  private Boolean required;
}
