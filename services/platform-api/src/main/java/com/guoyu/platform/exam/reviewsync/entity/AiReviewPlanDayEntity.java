package com.guoyu.platform.exam.reviewsync.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 每日任务，对应 ai_review_plan_day */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_review_plan_day")
public class AiReviewPlanDayEntity extends BaseEntity {

  private Long planId;
  /** 第几天 */
  private Integer dayIndex;
  /** 计划日期 */
  private LocalDate planDate;
  /** 1/2/3 */
  private Integer stage;
  /** 今日新题 id 有序列表 JSON */
  private String newQuestionIds;
  /** 今日复盘题 id 有序列表 JSON（红牌置顶、加星优先） */
  private String reviewQuestionIds;
  /** 阶段3 模考卷 id */
  private Long mockPaperId;
  private Integer newTarget;
  private Integer reviewTarget;
  private Integer newDone;
  private Integer reviewDone;
  private Integer correctCount;
  private Integer answerCount;
  /** pending/in_progress/done/skipped/abandoned */
  private String status;
  /** 当日策略说明（中断补救文案等，展示给学员） */
  private String strategyNote;
}
