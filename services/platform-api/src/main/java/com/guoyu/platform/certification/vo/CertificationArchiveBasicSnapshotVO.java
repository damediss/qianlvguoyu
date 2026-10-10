package com.guoyu.platform.certification.vo;

import java.time.LocalDateTime;

public record CertificationArchiveBasicSnapshotVO(
    Long id,
    Long archiveId,
    LocalDateTime completedAt,
    Integer candidateCount,
    String taskJson,
    String scopeJson,
    String examStaffJson,
    String deviceJson,
    String roomJson,
    String evaluatorJson,
    LocalDateTime createdAt
) {
}
