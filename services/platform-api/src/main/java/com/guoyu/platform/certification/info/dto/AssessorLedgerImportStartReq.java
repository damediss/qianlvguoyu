package com.guoyu.platform.certification.info.dto;

import jakarta.validation.constraints.NotNull;

public record AssessorLedgerImportStartReq(@NotNull Long fileId) {
}
