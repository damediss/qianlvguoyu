package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

public record CompetitionEventOrgCreateReq(
    Long orgId,
    @Size(max = 128) String orgName,
    @Min(0) Integer quota,
    @Min(0) @Max(1) Integer importPermission
) {

  @AssertTrue(message = "请输入企业名称")
  public boolean hasOrganization() {
    return orgId != null || (orgName != null && !orgName.trim().isEmpty());
  }
}
