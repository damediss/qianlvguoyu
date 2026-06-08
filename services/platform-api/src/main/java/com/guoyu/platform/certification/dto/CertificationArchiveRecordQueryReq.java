package com.guoyu.platform.certification.dto;

import com.guoyu.platform.common.api.PageQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CertificationArchiveRecordQueryReq extends PageQuery {
  private String keyword;
  private String archiveStatus;
}
