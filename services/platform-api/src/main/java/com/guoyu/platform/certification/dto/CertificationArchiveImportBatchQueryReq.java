package com.guoyu.platform.certification.dto;

import com.guoyu.platform.common.api.PageQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CertificationArchiveImportBatchQueryReq extends PageQuery {
  private Long archiveId;
  private String status;
  private String keyword;
}
