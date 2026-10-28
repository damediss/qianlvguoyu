package com.guoyu.platform.exam.answercheck.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 选题 SQL 查询条件（分页与创建快照共用，避免两套筛选逻辑）。
 */
@Getter
@Setter
public class ObjectiveAnswerScopeQuery {
  private Integer workTypeId;
  private Integer levelCode;
  private Integer source;
  private java.util.List<String> types;
  private java.util.List<Integer> detailIds;
}
