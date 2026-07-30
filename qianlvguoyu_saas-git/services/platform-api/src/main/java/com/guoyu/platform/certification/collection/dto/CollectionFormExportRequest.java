package com.guoyu.platform.certification.collection.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CollectionFormExportRequest(@NotEmpty List<Long> candidateIds) {
}
