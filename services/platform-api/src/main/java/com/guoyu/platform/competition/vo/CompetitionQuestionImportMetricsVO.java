package com.guoyu.platform.competition.vo;

import java.util.List;

/** 竞赛试题导入分段耗时。批次数按多值 INSERT 计，不是逐行 Mapper.insert。 */
public record CompetitionQuestionImportMetricsVO(
    long totalMs,
    long excelParseMs,
    long validationMs,
    long persistMs,
    long commitMs,
    int questionBatchCount,
    int bindingBatchCount,
    int persistSqlCount,
    int totalSqlCount,
    List<Long> batchWriteMs,
    Long avgBatchWriteMs
) {}
