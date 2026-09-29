package com.guoyu.platform.competition.vo;

import java.util.List;

public record CompetitionPracticalExecutionConfigValidationVO(
    boolean valid, List<String> errors, String message) { }
