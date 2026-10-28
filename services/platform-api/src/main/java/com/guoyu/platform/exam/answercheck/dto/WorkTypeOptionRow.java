package com.guoyu.platform.exam.answercheck.dto;

import lombok.Getter;
import lombok.Setter;

/** 工种下拉项：完整父路径在服务层拼接，解决同名职业/工种歧义。 */
@Getter
@Setter
public class WorkTypeOptionRow {
  private Integer id;
  private String title;
  private Integer parentId;
  private String seriesNo;
}
