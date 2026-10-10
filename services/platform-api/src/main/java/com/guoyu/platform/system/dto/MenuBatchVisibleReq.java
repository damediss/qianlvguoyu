package com.guoyu.platform.system.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record MenuBatchVisibleReq(
    @NotEmpty @Size(max = 200) List<Long> ids,
    @NotNull Integer visible
) {
}
