package com.guoyu.platform.competition.vo;

import java.util.List;

public record CompetitionStageReadinessVO(
    Long stageId,
    boolean ready,
    List<CompetitionStageReadinessCheckVO> checks) {
}
