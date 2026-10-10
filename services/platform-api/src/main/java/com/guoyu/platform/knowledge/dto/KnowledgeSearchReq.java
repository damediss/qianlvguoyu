package com.guoyu.platform.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record KnowledgeSearchReq(
    @NotBlank @Size(max = 512) String query,
    List<Long> spaceIds,
    @Size(max = 64) String categoryCode,
    Integer topK
) {
}
