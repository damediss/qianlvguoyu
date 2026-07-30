package com.guoyu.platform.certification.dto;

import jakarta.validation.constraints.Size;

public record CertificationDocumentReq(
    @Size(max = 4000) String contentText,
    String contentJson,
    @Size(max = 1000) String remark
) {
}
