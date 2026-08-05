package com.guoyu.platform.competition.vo;

import java.util.List;

public record CompetitionStagePlanPreflightVO(
    Long planId,
    boolean readyToPublish,
    List<CompetitionStagePlanPreflightCheckVO> checks) { }
