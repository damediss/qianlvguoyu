package com.guoyu.platform.certification.vo;

public record CertificationArchiveCheckItemVO(
    String code,
    String name,
    boolean passed,
    String message
) {
}
