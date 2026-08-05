package com.guoyu.platform.competition.vo;

import java.time.LocalDateTime;
import java.util.List;

public record CompetitionStageRosterSnapshotVO(
    Long id,
    Long planId,
    Long eventId,
    Long stageId,
    Long eventWorkTypeId,
    Long eventWorkTypeLevelId,
    Integer snapshotVersion,
    String sourceType,
    Long sourcePromotionSnapshotId,
    String status,
    LocalDateTime generatedAt,
    LocalDateTime publishedAt,
    List<CompetitionStageRosterItemVO> items) { }
