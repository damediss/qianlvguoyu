package com.guoyu.platform.competition.vo;

import java.util.List;

public record CompetitionFormalExamPlayerPaperVO(
    CompetitionFormalExamPlayerPaperSessionVO session,
    CompetitionFormalExamPlayerPaperCandidateVO candidate,
    CompetitionFormalExamPlayerPaperMetaVO paper,
    List<CompetitionFormalExamPlayerQuestionVO> questions,
    List<CompetitionFormalExamPlayerSavedAnswerVO> answers) {
}
