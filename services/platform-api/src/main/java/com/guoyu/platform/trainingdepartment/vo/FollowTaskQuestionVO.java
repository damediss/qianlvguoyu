package com.guoyu.platform.trainingdepartment.vo;

import java.math.BigDecimal;

/** 题目 VO；paper 不含 answer/analysis，result 可含 */
public record FollowTaskQuestionVO(
    String id,
    Integer sortNumber,
    String questionType,
    String stem,
    String optionsJson,
    BigDecimal score,
    String answerJson,
    String analysis
) {}
