package com.guoyu.platform.courseware.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/** AI 课件 LLM 调用审计 cw_llm_log(LLM 规范 #10) */
@Getter
@Setter
@TableName("cw_llm_log")
public class CwLlmLogEntity extends BaseEntity {

  public static final String STAGE_DIGEST = "digest";
  public static final String STAGE_OUTLINE = "outline";
  public static final String STAGE_SLIDE = "slide";

  private Long projectId;
  private Long slideId;
  private String stage;
  private String modelCode;
  private String modelId;
  private String promptDigest;
  private Integer promptTokens;
  private Integer completionTokens;
  private Integer totalTokens;
  private Integer cachedTokens;
  private Long durationMs;
  private Integer success;
  private String error;
  private String traceId;
}
