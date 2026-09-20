package com.guoyu.platform.courseware.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/** AI 课件项目 cw_project */
@Getter
@Setter
@TableName("cw_project")
public class CwProjectEntity extends BaseEntity {

  public static final String STATUS_DRAFT = "draft";
  public static final String STATUS_PARSING = "parsing";
  public static final String STATUS_DIGESTING = "digesting";
  public static final String STATUS_OUTLINE_GENERATING = "outline_generating";
  public static final String STATUS_OUTLINE_READY = "outline_ready";
  public static final String STATUS_GENERATING = "generating";
  public static final String STATUS_READY = "ready";
  public static final String STATUS_PARTIAL = "partial";
  public static final String STATUS_FAILED = "failed";
  public static final String STATUS_CANCELLED = "cancelled";

  private String title;
  private Long sourceFileId;
  private String audience;
  private String contentDensity;
  private String themeId;
  private String status;
  private Integer totalPages;
  private Integer readyPages;
  private Integer failedPages;
  private String error;
}
