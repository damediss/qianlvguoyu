package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompetitionFormalExamPlayerLoginReq(
    @NotBlank @Size(max = 32) String account,
    @NotBlank @Size(min = 6, max = 64) String password,
    @Size(max = 32) String sessionId,
    @Size(max = 64) String deviceId) {

  public CompetitionFormalExamPlayerLoginReq(String account, String password) {
    this(account, password, null, null);
  }
}
