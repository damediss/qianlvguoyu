package com.guoyu.platform.certification.vo;

public record CertificationArchiveCandidateMaterialVO(
    Long id,
    Long archiveCandidateId,
    String materialType,
    String materialGroup,
    String fileName,
    Long fileSize,
    String fileHash,
    String contentType
) {
}
