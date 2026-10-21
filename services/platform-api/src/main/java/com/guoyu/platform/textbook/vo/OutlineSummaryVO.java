package com.guoyu.platform.textbook.vo;

import java.util.List;

/**
 * 合计校验：章合计 = 总字数 是否平衡；不平衡列出章节（提示不阻塞导出）。
 */
public record OutlineSummaryVO(
    long chapterSum,
    long totalWords,
    boolean balanced,
    List<String> unbalancedChapters
) {
}
