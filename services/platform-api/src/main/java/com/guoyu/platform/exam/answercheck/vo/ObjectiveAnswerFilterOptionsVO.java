package com.guoyu.platform.exam.answercheck.vo;

import java.util.List;

/** 筛选项（方案 §5.2）：只返回经知识点→课程关联后确实有题的工种。 */
public record ObjectiveAnswerFilterOptionsVO(
    List<WorkTypeOption> workTypes,
    List<LevelOption> levels,
    List<SourceOption> sources,
    List<TypeOption> types) {

  /** path 为完整父路径，解决同名职业/工种歧义。 */
  public record WorkTypeOption(Long id, String title, String path) {
  }

  public record LevelOption(Integer code, String label) {
  }

  public record SourceOption(Integer value, String label) {
  }

  public record TypeOption(String value, String label) {
  }
}
