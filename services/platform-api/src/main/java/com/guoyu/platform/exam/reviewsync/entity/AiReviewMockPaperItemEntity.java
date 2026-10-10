package com.guoyu.platform.exam.reviewsync.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 模考卷题明细，对应 ai_review_mock_paper_item */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_review_mock_paper_item")
public class AiReviewMockPaperItemEntity extends BaseEntity {

  private Long paperId;
  private Long questionId;
  private String questionType;
  /** 题序（随机，脱离题库顺序） */
  private Integer sortNumber;
  private BigDecimal score;
}
