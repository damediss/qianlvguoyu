package com.guoyu.platform.certification.vo;

import java.time.LocalDateTime;

public record CertificationArchiveMaterialVO(
    Long id,
    Long archiveId,
    String materialType,
    String materialGroup,
    String materialName,
    String fileName,
    Long fileSize,
    String fileHash,
    String contentType,
    String fileStatus,
    Long uploadedBy,
    LocalDateTime uploadedAt
) {
}
