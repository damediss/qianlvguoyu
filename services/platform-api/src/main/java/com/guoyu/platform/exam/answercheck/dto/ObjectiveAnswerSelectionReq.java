package com.guoyu.platform.exam.answercheck.dto;

import java.util.List;

/**
 * 选题请求（方案 §7.3）。
 *
 * @param selectionMode ids / filter
 * @param detailIds     ids 模式必填并去重；filter 模式必须为空
 * @param scope         filter 模式必填；ids 模式可选，若提供则同时套用
 */
public record ObjectiveAnswerSelectionReq(
    String selectionMode,
    List<Integer> detailIds,
    ObjectiveAnswerScope scope) {

  public static final String MODE_IDS = "ids";
  public static final String MODE_FILTER = "filter";
}
