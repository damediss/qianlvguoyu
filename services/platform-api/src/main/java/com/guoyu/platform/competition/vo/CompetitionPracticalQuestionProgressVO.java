package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;
import java.util.List;

public record CompetitionPracticalQuestionProgressVO(
    String paperQuestionId,
    String status,
    BigDecimal score,
    List<CompetitionPracticalJudgeScoreEntryVO> entries) { }
