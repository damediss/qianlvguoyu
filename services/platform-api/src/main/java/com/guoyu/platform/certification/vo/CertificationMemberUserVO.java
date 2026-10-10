package com.guoyu.platform.certification.vo;

/** Registered no-review certification administrators available for project membership. */
public record CertificationMemberUserVO(
    String id,
    String username,
    String displayName,
    String phone
) {
}
