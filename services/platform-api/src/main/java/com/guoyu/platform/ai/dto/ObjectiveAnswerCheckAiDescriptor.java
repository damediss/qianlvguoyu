package com.guoyu.platform.ai.dto;

/** AI 服务描述：只暴露模型名与凭据就绪布尔值，绝不返回 baseUrl/apiKey（方案 §6）。 */
public record ObjectiveAnswerCheckAiDescriptor(String modelId, boolean credentialsReady) {
}
