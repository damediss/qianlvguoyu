package com.guoyu.platform.industrial.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProjectResourceVO(
    String id,
    String type,
    String date,
    String name,
    String state,
    String source,
    String company,
    String preview,
    @JsonProperty("file_type") String fileType
) {
}
