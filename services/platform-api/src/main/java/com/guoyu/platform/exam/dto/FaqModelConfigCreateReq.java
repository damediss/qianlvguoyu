package com.guoyu.platform.exam.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 创建 LLM 模型配置请求 */
public record FaqModelConfigCreateReq(
    @NotBlank @Size(max = 64) String modelCode,
    @NotBlank @Size(max = 128) String modelName,
    @NotBlank @Size(max = 512) String baseUrl,
    @NotBlank @Size(max = 512) String apiKey,
    @NotBlank @Size(max = 32) String apiFormat,
    @NotBlank @Size(max = 128) String modelId,
    @DecimalMin("0.0") @DecimalMax("2.0") java.math.BigDecimal temperature,
    @Min(100) @Max(32000) Integer maxTokens,
    String extraConfig,
    @Min(1) @Max(20) Integer jevConcurrency,
    @Min(5) @Max(120) Integer jevReadTimeoutSeconds,
    @NotNull Integer status
) {
}
