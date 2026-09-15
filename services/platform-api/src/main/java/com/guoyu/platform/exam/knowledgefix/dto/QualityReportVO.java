package com.guoyu.platform.exam.knowledgefix.dto;

import java.math.BigDecimal;

/** 质量报告（自动质量信号，方案 §4.4） */
public record QualityReportVO(
    Long batchNo,
    Integer total,
    Integer classified,
    Integer applyCnt,
    Integer keepCnt,
    Integer flagCnt,
    Integer failed,
    BigDecimal selfConsistencyRate,
    String status,
    /** 未分类行数（FIX-1.3 口径：total - done） */
    Integer unclassified) {
}
