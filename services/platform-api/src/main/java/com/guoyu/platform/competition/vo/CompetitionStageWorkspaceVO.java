package com.guoyu.platform.competition.vo;

import java.util.List;

public record CompetitionStageWorkspaceVO(
    CompetitionStageVO stage,
    List<CompetitionStageContentVO> contents,
    CompetitionStageReadinessVO readiness) {
}
