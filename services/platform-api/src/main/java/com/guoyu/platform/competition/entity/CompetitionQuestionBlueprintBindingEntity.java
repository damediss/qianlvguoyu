package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

/** 试题在某个细目表版本中的考评点绑定，支持跨版本精确复用。 */
@TableName("competition_question_blueprint_binding")
public class CompetitionQuestionBlueprintBindingEntity extends BaseEntity {

  private Long questionId;
  private Long blueprintId;
  private Long blueprintNodeId;
  private String matchMode;
  private Long sourceBatchId;
  public Long getQuestionId() { return questionId; }
  public void setQuestionId(Long questionId) { this.questionId = questionId; }
  public Long getBlueprintId() { return blueprintId; }
  public void setBlueprintId(Long blueprintId) { this.blueprintId = blueprintId; }
  public Long getBlueprintNodeId() { return blueprintNodeId; }
  public void setBlueprintNodeId(Long blueprintNodeId) { this.blueprintNodeId = blueprintNodeId; }
  public String getMatchMode() { return matchMode; }
  public void setMatchMode(String matchMode) { this.matchMode = matchMode; }
  public Long getSourceBatchId() { return sourceBatchId; }
  public void setSourceBatchId(Long sourceBatchId) { this.sourceBatchId = sourceBatchId; }
}
