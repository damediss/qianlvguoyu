package com.guoyu.platform.exam.answercheck.dto;

import lombok.Getter;
import lombok.Setter;

/** 结果明细过滤条件（页面统计卡、复核队列、导出共用）。 */
@Getter
@Setter
public class ObjectiveAnswerItemFilter {
  private String resultCode;
  private String contentQuality;
  private Boolean needsReview;
  private String reviewStatus;
  private String questionType;
  private String keyword;
}
