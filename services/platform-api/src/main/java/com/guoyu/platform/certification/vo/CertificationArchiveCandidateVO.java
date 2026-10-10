package com.guoyu.platform.certification.vo;

import java.util.List;

public record CertificationArchiveCandidateVO(
    Long id,
    Long archiveId,
    String candidateNo,
    String name,
    String idCard,
    String maskedIdCard,
    String folderName,
    List<CertificationArchiveCandidateMaterialVO> materials
) {
}
