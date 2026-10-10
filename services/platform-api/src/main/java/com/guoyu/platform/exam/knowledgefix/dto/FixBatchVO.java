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
    /** Jev 通道分类行数（§8.5 统计） */
    Integer jevClassified,
    /** 走 LLM 全流程兜底行数 */
    Integer llmFallback,
    /** Jev 行平均置信度（信息项，不卡门禁） */
    BigDecimal jevAvgConfidence,
    String status,
    String failReason,
    /** 未分类行数（FIX-1.3 口径：total - done，即未获得模型结论的行；failed 为尝试失败次数，另计） */
    Integer unclassified,
    /** 累计 LLM 用量（FIX-10/v1.7；进度接口填入，提交接口为 null） */
    FixBatchUsageVO usage) {

  public static FixBatchVO from(ExamKnowledgeFixBatchEntity e) {
    return from(e, null);
  }

  public static FixBatchVO from(ExamKnowledgeFixBatchEntity e, FixBatchUsageVO usage) {
    int total = e.getTotal() == null ? 0 : e.getTotal();
    int done = e.getDone() == null ? 0 : e.getDone();
    return new FixBatchVO(e.getId(), e.getBatchNo(), e.getTotal(), e.getDone(), e.getFailed(),
        e.getApplyCnt(), e.getKeepCnt(), e.getFlagCnt(), e.getSelfConsistencyRate(),
        e.getJevClassified(), e.getLlmFallback(), e.getJevAvgConfidence(),
        e.getStatus(), e.getFailReason(), Math.max(0, total - done), usage);
  }
}
