package com.guoyu.platform.exam.answercheck.dto;

import lombok.Getter;
import lombok.Setter;

/** 试题分页/快照行：只带业务字段，不带 org/user 等外发禁止字段。 */
@Getter
@Setter
public class ObjectiveAnswerScopeRow {
  private Integer detailId;
  private String questionType;
  private String content;
  private String answer;
  private String resolveText;
  private Integer source;
  private String audio;
  private String video;
  private Integer knowledgeId;
  private String knowledgeTitle;
  private Integer courseId;
  private String courseTitle;
  private Integer courseLevel;
  private Integer workTypeId;
  private String workTypeTitle;
}
