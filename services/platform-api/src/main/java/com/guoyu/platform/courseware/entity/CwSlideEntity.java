package com.guoyu.platform.courseware.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/** AI 课件生成页 cw_slide */
@Getter
@Setter
@TableName("cw_slide")
public class CwSlideEntity extends BaseEntity {

  public static final String STATUS_PENDING = "pending";
  public static final String STATUS_GENERATING = "generating";
  public static final String STATUS_READY = "ready";
  public static final String STATUS_FAILED = "failed";
  public static final String STATUS_CANCELLED = "cancelled";

  private Long projectId;
  private Long outlinePageId;
  private Integer position;
  private String layoutId;
  /** JSON: [{slot_id,type,text?/items?/header?/rows?/value?/label?/note?}] */
  private String blocks;
  private String speakerNotes;
  /** JSON: [{severity,code,slotId,message}] */
  private String issues;
  private String status;
  private String error;
  private Integer revision;
}
