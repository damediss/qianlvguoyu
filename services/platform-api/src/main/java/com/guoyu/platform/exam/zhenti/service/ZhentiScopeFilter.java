package com.guoyu.platform.exam.zhenti.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.guoyu.platform.exam.zhenti.entity.ExamZhentiEntity;
import java.util.List;

/**
 * 真题可见性过滤（方案 v2.1 §3.4）——A1/D1/years 共用的一份实现。
 *
 * <p>有 scope：命中任一共享组，或“未归类旧数据”（scope 列为空且 exam_id 属于本计划，迁移期不倒退）；
 * 无 scope：退化为原 exam_id 私有口径。
 * 条件用 and(...) 整体包住，避免与 title/year 等过滤串联成错误 SQL。
 */
public final class ZhentiScopeFilter {

  private ZhentiScopeFilter() {
  }

  public static void apply(LambdaQueryWrapper<ExamZhentiEntity> wrapper, Long examId,
      List<ZhentiScope> scopes) {
    if (scopes.isEmpty()) {
      wrapper.eq(ExamZhentiEntity::getExamId, examId);
      return;
    }
    applyInternal(wrapper, examId, scopes);
  }

  public static void apply(QueryWrapper<ExamZhentiEntity> wrapper, Long examId,
      List<ZhentiScope> scopes) {
    if (scopes.isEmpty()) {
      wrapper.eq("exam_id", examId);
      return;
    }
    applyInternal(wrapper, examId, scopes);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void applyInternal(com.baomidou.mybatisplus.core.conditions.AbstractWrapper wrapper,
      Long examId, List<ZhentiScope> scopes) {
    wrapper.and(w -> {
      com.baomidou.mybatisplus.core.conditions.AbstractWrapper inner =
          (com.baomidou.mybatisplus.core.conditions.AbstractWrapper) w;
      for (int i = 0; i < scopes.size(); i++) {
        if (i > 0) {
          inner.or();
        }
        ZhentiScope scope = scopes.get(i);
        inner.apply("(work_type_id = {0} AND level_code = {1})",
            scope.workTypeId(), scope.level());
      }
      inner.or();
      inner.apply("(work_type_id IS NULL AND level_code IS NULL AND exam_id = {0})", examId);
    });
  }
}
