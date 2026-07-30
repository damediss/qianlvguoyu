package com.guoyu.platform.exam.dto;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record ExamTaskXimuImportReq(
    @NotNull Integer workTypeId,
    @NotNull Integer level,
    @NotNull Integer types
) {
}
