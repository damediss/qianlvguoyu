package com.guoyu.platform.trainingdepartment.vo;

import java.util.List;

/** 同步历史信息收集结果 */
public record SignUpPeopleHistorySyncResultVO(
    int total,
    int merged,
    int skippedNoHistory,
    int skippedNoIdCard,
    int unchanged,
    List<String> messages
) {}
