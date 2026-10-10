package com.guoyu.platform.certification.collection.vo;

/**
 * 扫码匹配当前考生结果。
 *
 * <ul>
 *   <li>{@code alreadyCompleted} 为 true 时表示考生已完成采集，前端应提示而非静默覆盖。</li>
 *   <li>{@code currentCandidate} 为匹配后（或已完成）考生的最新采集详情。</li>
 * </ul>
 */
public record ScanMatchResultVO(
    boolean alreadyCompleted,
    CandidateCollectionDetailVO currentCandidate,
    String message
) {
}
