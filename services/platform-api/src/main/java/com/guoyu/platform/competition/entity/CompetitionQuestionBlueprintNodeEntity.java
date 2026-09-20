package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;

/** 竞赛细目表层级节点及末级考评点。 */
@TableName("competition_question_blueprint_node")
public class CompetitionQuestionBlueprintNodeEntity extends BaseEntity {

  private Long blueprintId;
  private Long parentId;
  private Integer nodeLevel;
  private String code;
  private String fullCode;
  private String name;
  private BigDecimal weight;
  private String selectMode;
  private String skillRequirement;
  private String relatedKnowledge;
  private String importance;
  private String xyzStat;
  private Integer questionQuota;
  private Integer sortOrder;
  public Long getBlueprintId() { return blueprintId; }
  public void setBlueprintId(Long blueprintId) { this.blueprintId = blueprintId; }
  public Long getParentId() { return parentId; }
  public void setParentId(Long parentId) { this.parentId = parentId; }
  public Integer getNodeLevel() { return nodeLevel; }
  public void setNodeLevel(Integer nodeLevel) { this.nodeLevel = nodeLevel; }
  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  public String getFullCode() { return fullCode; }
  public void setFullCode(String fullCode) { this.fullCode = fullCode; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public BigDecimal getWeight() { return weight; }
  public void setWeight(BigDecimal weight) { this.weight = weight; }
  public String getSelectMode() { return selectMode; }
  public void setSelectMode(String selectMode) { this.selectMode = selectMode; }
  public String getSkillRequirement() { return skillRequirement; }
  public void setSkillRequirement(String skillRequirement) { this.skillRequirement = skillRequirement; }
  public String getRelatedKnowledge() { return relatedKnowledge; }
  public void setRelatedKnowledge(String relatedKnowledge) { this.relatedKnowledge = relatedKnowledge; }
  public String getImportance() { return importance; }
  public void setImportance(String importance) { this.importance = importance; }
  public String getXyzStat() { return xyzStat; }
  public void setXyzStat(String xyzStat) { this.xyzStat = xyzStat; }
  public Integer getQuestionQuota() { return questionQuota; }
  public void setQuestionQuota(Integer questionQuota) { this.questionQuota = questionQuota; }
  public Integer getSortOrder() { return sortOrder; }
  public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
