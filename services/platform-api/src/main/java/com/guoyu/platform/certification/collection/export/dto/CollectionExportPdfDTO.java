package com.guoyu.platform.certification.collection.export.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** 实操信息采集表固定版式 PDF 导出请求。 */
public record CollectionExportPdfDTO(
    @NotNull Long examTaskId,
    Long examSessionId,
    @NotEmpty List<Long> candidateIds
) {
}
