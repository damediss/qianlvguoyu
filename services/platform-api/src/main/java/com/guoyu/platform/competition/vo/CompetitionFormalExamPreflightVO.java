package com.guoyu.platform.competition.vo;

import java.util.List;

public record CompetitionFormalExamPreflightVO(
    boolean ready,
    List<CompetitionFormalExamPreflightCheckVO> checks) {
}
