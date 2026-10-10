package com.guoyu.platform.courseware.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/** AI 课件大纲页 cw_outline_page(章→页两级) */
@Getter
@Setter
@TableName("cw_outline_page")
public class CwOutlinePageEntity extends BaseEntity {

  public static final String ROLE_COVER = "cover";
  public static final String ROLE_TOC = "toc";
  public static final String ROLE_SECTION = "section";
  public static final String ROLE_CONTENT = "content";
  public static final String ROLE_REVIEW = "review";

  private Long projectId;
  private Integer chapterNo;
  private String chapterTitle;
  private Integer position;
  private String title;
  private String objective;
  /** JSON: ["要点1","要点2"] */
  private String keyPoints;
  private String pageRole;
  private String layoutId;
  /** JSON: ["S1:2","S1:3"] */
  private String sourceRefs;

  /** 知识点 ID(仅知识点页非空,cw_knowledge_point.id) */
  private Long knowledgePointId;
}
