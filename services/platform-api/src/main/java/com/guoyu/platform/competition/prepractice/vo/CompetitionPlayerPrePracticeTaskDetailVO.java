package com.guoyu.platform.competition.prepractice.vo;

import java.util.List;

public record CompetitionPlayerPrePracticeTaskDetailVO(
    CompetitionPlayerPrePracticeTaskVO task,
    Integer answeredCount,
    List<CompetitionPlayerPrePracticeQuestionVO> questions) {
}
