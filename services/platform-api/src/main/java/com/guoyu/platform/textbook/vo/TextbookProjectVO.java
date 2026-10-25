package com.guoyu.platform.textbook.vo;

import java.time.LocalDateTime;

/**
 * 教材工程详情（雪花 id 以字符串下发）。
 *
 * @param parseWarnings 解析告警 JSON 数组字符串（原样透传 DB 列，前端 JSON.parse）
 */
public record TextbookProjectVO(
    String id,
    String workTypeTitle,
    String seriesNo,
    String standardVersion,
    String fileId,
    String fileName,
    String direction,
    Integer levelCode,
    java.util.List<Integer> functionNos,
    Integer trainingHours,
    Integer wordsPerHour,
    Integer totalWords,
    String status,
    String parseWarnings,
    Integer chapterCount,
    Integer sectionCount,
    Integer pointCount,
    /** 二期编写协同列表/详情批量字段（方案 §10.2；页面进度、删除文案与结构锁提示） */
    Integer assignedChapterCount,
    Integer approvedSectionCount,
    Boolean hasContent,
    Boolean structureLocked,
    OutlineSummaryVO summary,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
