package com.guoyu.platform.courseware.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/** AI 课件知识点清单 cw_knowledge_point(基本要求/工作要求逐 x.y.z 知识点) */
@Getter
@Setter
@TableName("cw_knowledge_point")
public class CwKnowledgePointEntity extends BaseEntity {

  public static final String CATEGORY_BASIC = "basic";
  public static final String CATEGORY_WORK = "work";

  private Long projectId;
  private String category;
  private String levelName;
  private String jobFunction;
  private String jobContent;
  private String code;
  private String title;
  private String skillText;
  private String knowledgeText;
  private String sourceRef;
  private Integer sortNo;
}
