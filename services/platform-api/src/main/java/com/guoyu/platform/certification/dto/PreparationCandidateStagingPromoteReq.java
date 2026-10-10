package com.guoyu.platform.certification.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record PreparationCandidateStagingPromoteReq(
    @NotEmpty List<Long> stagingIds
) {
}
