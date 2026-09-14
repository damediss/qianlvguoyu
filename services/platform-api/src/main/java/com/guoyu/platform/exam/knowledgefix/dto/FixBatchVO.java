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
    String failReason) {

  public static FixBatchVO from(ExamKnowledgeFixBatchEntity e) {
    return new FixBatchVO(e.getId(), e.getBatchNo(), e.getTotal(), e.getDone(), e.getFailed(),
        e.getApplyCnt(), e.getKeepCnt(), e.getFlagCnt(), e.getSelfConsistencyRate(),
        e.getStatus(), e.getFailReason());
  }
}
