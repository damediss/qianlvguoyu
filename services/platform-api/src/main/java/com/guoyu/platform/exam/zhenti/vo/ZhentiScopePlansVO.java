package com.guoyu.platform.exam.zhenti.vo;

import java.util.List;

/**
 * 「真题共享范围」页数据（方案 v2.1 §7）：考试计划 + 现有工种绑定（含失效标记）+ 课程选项。
 */
public record ZhentiScopePlansVO(
    List<Item> records,
    long total,
    long page,
    long size,
    List<CourseOption> courses
) {

  public record Item(
      String examId,
      String title,
      List<Binding> bindings,
      boolean noBinding,
      boolean multiBinding,
      boolean hasInvalid
  ) {
  }

  /** 一条工种绑定；valid=false 表示工种不存在或已删除（需改绑课程修复）。 */
  public record Binding(
      Long workTypeId,
      String workTypeTitle,
      Integer level,
      String levelLabel,
      Long courseId,
      String courseTitle,
      boolean valid
  ) {
  }

  public record CourseOption(Long id, String title, Long workTypeId, Integer level) {
  }
}
