package com.guoyu.platform.competition.vo;
import java.util.List;
public record CompetitionStageSettlementPreflightVO(Long planId, boolean ready, List<CompetitionStageSettlementPreflightCheckVO> checks) { }
