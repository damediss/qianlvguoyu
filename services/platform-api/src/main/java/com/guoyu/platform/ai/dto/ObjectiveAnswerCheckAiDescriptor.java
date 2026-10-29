package com.guoyu.platform.ai.dto;

/**
 * AI 服务描述：业务侧只拿非敏感连接配置与凭据就绪布尔值，绝不返回 apiKey（方案 v1.5 §6/§9.1）。
 *
 * @param modelCode             模型配置编码
 * @param modelId               请求 model 字段
 * @param baseUrl               SystemOne baseUrl（非密钥）
 * @param credentialsReady      环境变量真实密钥是否可用；DB 占位值不算就绪
 * @param jevConcurrency        批次可固化的 JEV 派发并发
 * @param jevReadTimeoutSeconds 批次可固化的 JEV 读超时
 */
public record ObjectiveAnswerCheckAiDescriptor(
    String modelCode,
    String modelId,
    String baseUrl,
    boolean credentialsReady,
    int jevConcurrency,
    int jevReadTimeoutSeconds) {

  public static ObjectiveAnswerCheckAiDescriptor unavailable() {
    return new ObjectiveAnswerCheckAiDescriptor(null, null, null, false, 5, 30);
  }
}
