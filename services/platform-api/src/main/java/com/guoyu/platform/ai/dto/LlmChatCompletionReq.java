package com.guoyu.platform.ai.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * LLM 代理接口请求体 — OpenAI 兼容 Chat Completion。
 * messages 用 JsonNode 接收，后端原样透传给 LLM。
 */
public record LlmChatCompletionReq(
    @NotBlank(message = "modelCode 不能为空")
    String modelCode,

    @NotNull(message = "messages 不能为空")
    JsonNode messages,

    BigDecimal temperature,
    Integer maxTokens,
    String thinking,

    /**
     * DeepSeek 推理强度：low/high/max（thinking 对象内字段，仅 type=enabled 时生效）。
     * 传了用请求值，否则用 exam_faq_model_config.reasoning_effort（默认 high）。
     */
    String reasoningEffort,

    /**
     * 是否请求 JSON Output（仅 DeepSeek 生效）。
     * true 时后端会对 DeepSeek 自动附加 {@code response_format: {"type": "json_object"}}。
     */
    Boolean responseFormatJson
) {
}
