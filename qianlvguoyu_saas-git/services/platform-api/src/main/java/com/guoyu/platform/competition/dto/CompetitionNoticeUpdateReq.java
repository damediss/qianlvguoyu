package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompetitionNoticeUpdateReq(
    @NotBlank @Size(max = 256) String title,
    @Size(max = 100000) String content,
    String contentType,
    @Min(1) Integer version
) {
  public CompetitionNoticeUpdateReq(String title, String content, Integer version) {
    this(title, content, null, version);
  }
}
