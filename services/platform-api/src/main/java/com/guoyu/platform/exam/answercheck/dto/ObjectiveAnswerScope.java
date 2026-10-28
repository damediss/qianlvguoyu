package com.guoyu.platform.exam.answercheck.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 选题范围（方案 §7.1）。types 为空按三种题型全集；存在未知值直接 400。
 */
@Getter
@Setter
public class ObjectiveAnswerScope {
  private Integer workTypeId;
  private Integer levelCode;
  private Integer source;
  private List<String> types;

  public static ObjectiveAnswerScope empty() {
    ObjectiveAnswerScope scope = new ObjectiveAnswerScope();
    scope.setTypes(List.of("single", "multi", "judge"));
    return scope;
  }
}
