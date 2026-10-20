package com.guoyu.platform.exam.zhenti.service;

/**
 * scope_key 生成（方案 v2.1 §4.3，Q4 已拍板：普通列 + 单一工具类）。
 *
 * <p>有共享组：work:{workTypeId}:{level}；无共享组（降级私有）：exam:{examId}。
 * 全项目唯一实现点，写入 exam_zhenti.scope_key 与按 key 去重都必须走这里。
 */
public final class ZhentiScopeKey {

  private static final String WORK_PREFIX = "work:";
  private static final String EXAM_PREFIX = "exam:";

  private ZhentiScopeKey() {
  }

  public static String of(ZhentiScope scope) {
    return WORK_PREFIX + scope.workTypeId() + ":" + scope.level();
  }

  public static String ofExam(Long examId) {
    return EXAM_PREFIX + examId;
  }

  /** 上传/入库统一入口：有 scope 用 work 键，无 scope 降级为计划私有键。 */
  public static String ofScopeOrExam(ZhentiScope scope, Long examId) {
    return scope == null ? ofExam(examId) : of(scope);
  }

  /** 共享组展示标签：「工种 · 四级/中级工」。 */
  public static String label(ZhentiScope scope, String workTypeTitle) {
    return workTypeTitle + " · " + ZhentiLevel.labelOf(scope.level());
  }
}
