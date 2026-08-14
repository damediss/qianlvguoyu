package com.guoyu.platform.exam.standard.vo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 解析任务 VO（id/fileId 以字符串下发——坑#24）。
 */
public record StandardTaskVO(
    String id,
    String workTypeTitle,
    String seriesNo,
    String version,
    String fileId,
    String status,
    List<String> directions,
    List<Map<String, Object>> levels,
    Integer leafTotal,
    Integer leafPending,
    String parseWarnings,
    String llmModelId,
    LocalDateTime llmFinishedAt,
    LocalDateTime createdAt,
    Map<String, List<Integer>> directionLevels) {
}
