package com.guoyu.platform.knowledge.vo;

import java.time.LocalDateTime;
import java.util.List;

public record KnowledgeSpaceVO(
    Long id,
    Long tenantId,
    String code,
    String name,
    String scopeType,
    Long ownerUserId,
    String categoryCode,
    String description,
    String status,
    List<String> visibleRoleCodes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
