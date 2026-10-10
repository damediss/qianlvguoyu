package com.guoyu.platform.exam.reviewsync.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** AI 同步学灰度白名单（D8，方案 §15.2），对应 ai_review_gray_whitelist */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_review_gray_whitelist")
public class AiReviewGrayWhitelistEntity extends BaseEntity {

  /** 范围类型：exam=按考试/工种（train_exam_list.id）；user=按学员（platform_user.id） */
  private String scopeType;
  /** 范围 ID：PHP 库 train_exam_list.id 或 platform_user.id */
  private Long scopeId;
  private String remark;
}
