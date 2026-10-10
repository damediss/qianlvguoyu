package com.guoyu.platform.certification.collection.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CollectionFormExportRequest(Long examSessionId, @NotEmpty List<Long> candidateIds) {
}
