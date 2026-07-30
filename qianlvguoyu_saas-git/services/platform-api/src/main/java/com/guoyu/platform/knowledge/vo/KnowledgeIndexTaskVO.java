package com.guoyu.platform.knowledge.vo;

import java.time.LocalDateTime;

public record KnowledgeIndexTaskVO(
    Long id,
    Long tenantId,
    Long spaceId,
    Long documentId,
    String taskType,
    String status,
    Integer retryCount,
    String errorMessage,
    LocalDateTime startedAt,
    LocalDateTime finishedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
