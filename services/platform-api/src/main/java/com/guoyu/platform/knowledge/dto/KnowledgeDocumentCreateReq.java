package com.guoyu.platform.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record KnowledgeDocumentCreateReq(
    @NotNull Long fileId,
    @NotBlank @Size(max = 255) String title,
    @Size(max = 64) String categoryCode,
    @Size(max = 32) String sourceType,
    @Size(max = 32) String accessScope,
    @Size(max = 32) String sensitiveLevel,
    List<Long> visibleUserIds,
    List<@Size(max = 64) String> visibleRoleCodes,
    String rawText
) {
}
