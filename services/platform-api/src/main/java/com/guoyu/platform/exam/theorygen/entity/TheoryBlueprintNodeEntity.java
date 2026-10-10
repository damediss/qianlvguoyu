package com.guoyu.platform.exam.theorygen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 理论知识细目表节点 exam_theory_blueprint_node */
@TableName("exam_theory_blueprint_node")
public class TheoryBlueprintNodeEntity {

  @TableId(type = IdType.INPUT)
  private Long id;

  private Long tenantId;
  private Long blueprintId;
  private Long parentId;
  private Integer nodeLevel;
  private String code;
  private String fullCode;
  private String name;
  private BigDecimal weight;
  private String importance;
  private String relatedKnowledge;
  private String xyzStat;
  private Long knowledgeId;
  private Integer sortOrder;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private Long createdBy;
  private Long updatedBy;

  @TableLogic(value = "0", delval = "1")
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
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
  public String getImportance() { return importance; }
  public void setImportance(String importance) { this.importance = importance; }
  public String getRelatedKnowledge() { return relatedKnowledge; }
  public void setRelatedKnowledge(String relatedKnowledge) { this.relatedKnowledge = relatedKnowledge; }
  public String getXyzStat() { return xyzStat; }
  public void setXyzStat(String xyzStat) { this.xyzStat = xyzStat; }
  public Long getKnowledgeId() { return knowledgeId; }
  public void setKnowledgeId(Long knowledgeId) { this.knowledgeId = knowledgeId; }
  public Integer getSortOrder() { return sortOrder; }
  public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
  public Long getCreatedBy() { return createdBy; }
  public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
  public Long getUpdatedBy() { return updatedBy; }
  public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
