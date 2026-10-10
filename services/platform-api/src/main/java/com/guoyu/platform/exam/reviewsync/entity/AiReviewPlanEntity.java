package com.guoyu.platform.exam.reviewsync.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** AI 复习计划主表（学员 × 考试计划唯一），对应 ai_review_plan */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_review_plan")
public class AiReviewPlanEntity extends BaseEntity {

  /** 学员（PHP platform_user.id） */
  private Long platformUserId;
  /** 培训考试计划 train_exam_list.id */
  private Long examId;
  /** 工种（题目范围快照） */
  private Long workTypeId;
  /** 等级快照 */
  private String level;
  /** 科目快照 */
  private Long courseId;
  /** 题库来源快照（train_exam_list.source） */
  private Integer source;
  /** 权重快照 JSON：[{knowledgeId,title,rate}]（计划生成时固化，§5.7） */
  private String weightSnapshot;
  /** 权重来源：standard/custom/count */
  private String weightSource;
  /** 考试日期（最近未来日） */
  private LocalDate examDate;
  /** 复习开始日 */
  private LocalDate startDate;
  private Integer totalDays;
  private Integer stage1Days;
  private Integer stage2Days;
  private Integer stage3Days;
  /** 题目全集题数 */
  private Integer totalQuestions;
  /** 冗余裁剪后必做题数 */
  private Integer targetQuestions;
  /** 必做池题 id 列表 JSON（排题输入快照） */
  private String mustDoIds;
  /** 被裁剪/中断补救放弃的题 id JSON */
  private String droppedQuestionIds;
  /** 是否触发裁剪 */
  private Integer trimmed;
  private Integer dailyNewCap;
  private Integer dailyTotalCap;
  /** 当前阶段 1/2/3 */
  private Integer currentStage;
  /** 当前第几天（从 1 起） */
  private Integer currentDayIndex;
  /** 最后活跃日（有作答即更新） */
  private LocalDate lastActiveDate;
  /** 中断次数 */
  private Integer interruptCount;
  /** active/completed/abandoned */
  private String status;
}
