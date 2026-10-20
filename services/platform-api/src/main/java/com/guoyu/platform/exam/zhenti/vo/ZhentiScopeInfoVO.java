package com.guoyu.platform.exam.zhenti.vo;

import java.util.List;

/**
 * 考试计划的共享组信息（管理端提示条 / 上传弹窗选择项，方案 v2.1 §7）。
 *
 * @param scopeEnabled 平台开关（platform.zhenti.scope-enabled）
 * @param unscoped     无有效共享组（真题仅本计划可见，上传走私有口径）
 * @param scopes       全部共享组（label 含工种名与等级中文，planCount 为该组下活跃计划数）
 */
public record ZhentiScopeInfoVO(
    boolean scopeEnabled,
    boolean unscoped,
    List<Item> scopes
) {

  public record Item(Long workTypeId, int level, String label, long planCount) {
  }
}
