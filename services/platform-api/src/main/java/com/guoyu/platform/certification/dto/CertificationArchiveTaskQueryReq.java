package com.guoyu.platform.certification.dto;

import com.guoyu.platform.common.api.PageQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CertificationArchiveTaskQueryReq extends PageQuery {
  private String keyword;
  private String status;
  private String currentPhase;
  private String archiveStatus;
}
