package com.guoyu.platform.knowledge.vo;

import java.time.LocalDateTime;

public record KnowledgeCitationVO(
    Long chunkId,
    Long tenantId,
    Long spaceId,
    String spaceName,
    Long documentId,
    String documentTitle,
    Long fileId,
    String heading,
    Integer pageNo,
    Integer chunkNo,
    String content,
    LocalDateTime createdAt
) {
}
