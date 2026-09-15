package com.guoyu.platform.exam.knowledgefix.dto;

import com.guoyu.platform.exam.knowledgefix.entity.ExamKnowledgeFixBatchEntity;
import java.math.BigDecimal;

/** 分类批次视图 */
public record FixBatchVO(
    Long id,
    Long batchNo,
    Integer total,
    Integer done,
    Integer failed,
    Integer applyCnt,
    Integer keepCnt,
    Integer flagCnt,
    BigDecimal selfConsistencyRate,
    String status,
    String failReason,
    /** 未分类行数（FIX-1.3 口径：total - done，即未获得模型结论的行；failed 为尝试失败次数，另计） */
    Integer unclassified) {

  public static FixBatchVO from(ExamKnowledgeFixBatchEntity e) {
    int total = e.getTotal() == null ? 0 : e.getTotal();
    int done = e.getDone() == null ? 0 : e.getDone();
    return new FixBatchVO(e.getId(), e.getBatchNo(), e.getTotal(), e.getDone(), e.getFailed(),
        e.getApplyCnt(), e.getKeepCnt(), e.getFlagCnt(), e.getSelfConsistencyRate(),
        e.getStatus(), e.getFailReason(), Math.max(0, total - done));
  }
}
