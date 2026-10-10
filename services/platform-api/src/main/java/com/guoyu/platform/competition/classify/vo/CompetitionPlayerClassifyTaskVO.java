package com.guoyu.platform.competition.classify.vo;

import java.time.LocalDateTime;
import java.util.List;

public record CompetitionPlayerClassifyTaskVO(
    String id,
    String eventId,
    String stageId,
    String stageName,
    Integer typeCount,
    Integer playerCount,
    String status,
    LocalDateTime createdAt,
    List<String> typeNames,
    List<Integer> typeSizes,
    List<CompetitionPlayerClassifyAssignmentVO> assignments) {
}
