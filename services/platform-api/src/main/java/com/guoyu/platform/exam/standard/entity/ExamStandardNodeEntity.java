package com.guoyu.platform.exam.standard.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;

/**
 * 职业技能标准树节点草稿（staging），对应 exam_standard_node 表。
 */
@TableName("exam_standard_node")
public class ExamStandardNodeEntity extends BaseEntity {

  private Long taskId;
  private Long parentId;
  private String nodeKey;
  private Integer nodeLevel;
  private Integer sortNumber;
  /** theory/skill；骨架节点（L0~L2）恒为 theory */
  private String dimension;
  private String titleRaw;
  private String title;
  /** X/Y/Z（仅 L3） */
  private String important;
  private BigDecimal weight;
  private String relatedKnowledge;
  private String skillRequirement;
  /** extract/llm/manual */
  private String source;
  /** pending/approved/modified */
  private String reviewStatus;
  private String direction;
  private Integer levelCode;
  private Integer needLlm;

  public Long getTaskId() {
    return taskId;
  }

  public void setTaskId(Long taskId) {
    this.taskId = taskId;
  }

  public Long getParentId() {
    return parentId;
  }

  public void setParentId(Long parentId) {
    this.parentId = parentId;
  }

  public String getNodeKey() {
    return nodeKey;
  }

  public void setNodeKey(String nodeKey) {
    this.nodeKey = nodeKey;
  }

  public Integer getNodeLevel() {
    return nodeLevel;
  }

  public void setNodeLevel(Integer nodeLevel) {
    this.nodeLevel = nodeLevel;
  }

  public Integer getSortNumber() {
    return sortNumber;
  }

  public void setSortNumber(Integer sortNumber) {
    this.sortNumber = sortNumber;
  }

  public String getDimension() {
    return dimension;
  }

  public void setDimension(String dimension) {
    this.dimension = dimension;
  }

  public String getTitleRaw() {
    return titleRaw;
  }

  public void setTitleRaw(String titleRaw) {
    this.titleRaw = titleRaw;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getImportant() {
    return important;
  }

  public void setImportant(String important) {
    this.important = important;
  }

  public BigDecimal getWeight() {
    return weight;
  }

  public void setWeight(BigDecimal weight) {
    this.weight = weight;
  }

  public String getRelatedKnowledge() {
    return relatedKnowledge;
  }

  public void setRelatedKnowledge(String relatedKnowledge) {
    this.relatedKnowledge = relatedKnowledge;
  }

  public String getSkillRequirement() {
    return skillRequirement;
  }

  public void setSkillRequirement(String skillRequirement) {
    this.skillRequirement = skillRequirement;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getReviewStatus() {
    return reviewStatus;
  }

  public void setReviewStatus(String reviewStatus) {
    this.reviewStatus = reviewStatus;
  }

  public String getDirection() {
    return direction;
  }

  public void setDirection(String direction) {
    this.direction = direction;
  }

  public Integer getLevelCode() {
    return levelCode;
  }

  public void setLevelCode(Integer levelCode) {
    this.levelCode = levelCode;
  }

  public Integer getNeedLlm() {
    return needLlm;
  }

  public void setNeedLlm(Integer needLlm) {
    this.needLlm = needLlm;
  }
}
