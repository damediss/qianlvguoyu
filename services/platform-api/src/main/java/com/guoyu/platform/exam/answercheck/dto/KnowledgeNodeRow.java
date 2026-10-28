package com.guoyu.platform.exam.answercheck.dto;

import lombok.Getter;
import lombok.Setter;

/** 知识点节点（构建“父/子/叶”路径时只在服务层走内存）。 */
@Getter
@Setter
public class KnowledgeNodeRow {
  private Integer id;
  private String title;
  private Integer parentId;
  private Integer courseId;
}
