package com.guoyu.platform.exam.zhenti.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 「真题共享范围」页批量保存（方案 v2.1 §7，Q5 已拍板保留该页）。
 *
 * <p>编辑模型 = 课程绑定：只提交 courseIdList，由 saveWorkTypeList 重新派生工种/等级，
 * 不直接改 work_type_id（避免派生关系漂移）。
 */
public record ZhentiScopeBatchSaveReq(@NotEmpty @Valid List<Item> items) {

  public record Item(
      @NotNull Long examId,
      @NotNull List<Long> courseIdList) {
  }
}
