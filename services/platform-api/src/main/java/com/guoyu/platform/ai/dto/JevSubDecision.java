package com.guoyu.platform.ai.dto;

import java.util.Map;

/**
 * 单个子问题判定结果。
 *
 * @param margin   p(choice) - max(p(其他候选))；最高并列时为 0
 * @param top1     最高概率（仅展示）
 * @param top2     次高概率（仅展示）
 */
public record JevSubDecision(
    String choice,
    double confidence,
    Map<String, Double> probabilities,
    double margin,
    double top1,
    double top2) {
}
