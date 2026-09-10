package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;
import java.util.List;

public record CompetitionFormalExamPlayerQuestionVO(
    String questionSnapshotId,
    Integer order,
    String type,
    String stem,
    BigDecimal score,
    List<CompetitionFormalExamPlayerOptionVO> options) {
}
