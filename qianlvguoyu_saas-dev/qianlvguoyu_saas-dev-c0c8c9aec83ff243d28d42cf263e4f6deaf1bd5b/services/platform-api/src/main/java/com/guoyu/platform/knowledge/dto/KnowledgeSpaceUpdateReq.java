package com.guoyu.platform.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record KnowledgeSpaceUpdateReq(
    @NotBlank @Size(max = 128) String name,
    @Size(max = 64) String categoryCode,
    @Size(max = 512) String description,
    @NotBlank @Size(max = 32) String status,
    List<@Size(max = 64) String> visibleRoleCodes
) {
}
