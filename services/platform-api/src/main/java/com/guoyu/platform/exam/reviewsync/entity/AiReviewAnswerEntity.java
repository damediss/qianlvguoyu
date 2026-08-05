package com.guoyu.platform.exam.reviewsync.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 作答记录（主记录），对应 ai_review_answer */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_review_answer")
public class AiReviewAnswerEntity extends BaseEntity {

  private Long planId;
  /** 每日任务 id（模考可空） */
  private Long dayId;
  private Long platformUserId;
  /** 冗余：train_exam_list.id（回写 PHP exam_result.exam_plan_id 用） */
  private Long examPlanId;
  /** exam_exam_detail.id */
  private Long questionId;
  /** 冗余：题目分值（回写 get_score 用） */
  private java.math.BigDecimal questionScore;
  private Long knowledgeId;
  /** single/multi/judge/... */
  private String questionType;
  /** new/review/mock */
  private String roundType;
  /** 作答内容（单/判：选项内容；多选：内容数组 JSON） */
  private String userAnswer;
  private Integer isCorrect;
  private Integer useTimeSec;
  /** 该题第几次作答 */
  private Integer attemptSeq;
  /** 回写 PHP exam_result：0 未回写 1 已回写 2 失败待补偿 */
  private Integer phpSynced;
  private LocalDateTime phpSyncTime;
}
