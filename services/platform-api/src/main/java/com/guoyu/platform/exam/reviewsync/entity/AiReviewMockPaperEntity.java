package com.guoyu.platform.exam.reviewsync.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 自组模考卷，对应 ai_review_mock_paper */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_review_mock_paper")
public class AiReviewMockPaperEntity extends BaseEntity {

  private Long planId;
  /** 第几套 */
  private Integer paperNo;
  private Integer questionCount;
  private BigDecimal totalScore;
  /** 及格分（total×0.6） */
  private BigDecimal passScore;
  private Integer timeLimitMin;
  /** ready/done */
  private String status;
  private BigDecimal score;
  private Integer correctCount;
}
