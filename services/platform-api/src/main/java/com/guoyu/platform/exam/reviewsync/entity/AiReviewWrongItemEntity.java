package com.guoyu.platform.exam.reviewsync.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 错题本（三级淘汰），对应 ai_review_wrong_item */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_review_wrong_item")
public class AiReviewWrongItemEntity extends BaseEntity {

  private Long planId;
  private Long platformUserId;
  private Long questionId;
  /** 知识点（L3） */
  private Long knowledgeId;
  /** red/yellow/green */
  private String cardLevel;
  /** 二次错误加星（顽固盲区） */
  private Integer star;
  /** 连续答对计数 */
  private Integer consecutiveCorrect;
  /** 最近一次答对日期（黄牌间隔>3天判定） */
  private LocalDate lastCorrectDate;
  private Integer wrongCount;
  private Integer correctCount;
  private LocalDateTime firstWrongTime;
  private LocalDateTime lastWrongTime;
  /** 是否已移出（绿牌=1） */
  private Integer removed;
  private LocalDateTime removeTime;
}
