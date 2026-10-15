package com.guoyu.platform.exam.reviewsync.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 中断日志（每次触发中断补救写一行；D 案 7 天窗口与回归率指标数据源），对应 ai_review_interrupt_log */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_review_interrupt_log")
public class AiReviewInterruptLogEntity extends BaseEntity {

  private Long planId;
  private Long platformUserId;
  /** A/B/C/D */
  private String interruptCase;
  /** 本次间隔天数 */
  private Integer gapDays;
  /** 中断时阶段 */
  private Integer stageAtInterrupt;
}
