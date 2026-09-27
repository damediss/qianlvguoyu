package com.guoyu.platform.ai.dto;

import java.util.Map;

/**
 * Jev 决策结果（bocha-jev-v1 / TypeSafe SystemOne choice 题，实测 2026-09-27）：
 * choice = criteria 的 key 原样回传；confidence = maxP（metadata.confidence_definition
 * = "maximum candidate probability"，路由到 TypeSafe 侧时口径切换为分布形状推导——
 * 调用方须把 confidence_definition 落库，方案 §4-F2）。
 *
 * @param choice               选中选项（= criteria key，如 nodeId 字符串）
 * @param confidence           校准置信度（0~1）
 * @param probabilities        完整概率分布（实测含全部 criteria key）
 * @param inputTokens          输入 token（实测 usage.input_tokens）
 * @param outputTokens         输出 token（实测恒 0，输出免费）
 * @param confidenceDefinition 响应 metadata.confidence_definition（审计落库，§8.8）
 * @param responseModel        响应 model 字段（双服务商路由漂移监控，§9 风险 3）
 * @param durationMs           端到端耗时（含网络+网关；实测 1.0s@3 选项 ~ 5.6s@255 选项）
 * @param usageEstimated       响应缺 usage 时按字符估算的防御分支标记（§11.1）
 */
public record JevDecisionResult(
    String choice,
    double confidence,
    Map<String, Double> probabilities,
    long inputTokens,
    long outputTokens,
    String confidenceDefinition,
    String responseModel,
    int durationMs,
    boolean usageEstimated) {
}
