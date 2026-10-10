package com.guoyu.platform.knowledge.vo;

import java.time.LocalDateTime;

public record KnowledgeDocumentVO(
    Long id,
    Long tenantId,
    Long spaceId,
    Long fileId,
    String title,
    String categoryCode,
    String sourceType,
    String accessScope,
    String sensitiveLevel,
    String parserStatus,
    String indexStatus,
    Integer chunkCount,
    String textSha256,
    String errorMessage,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
