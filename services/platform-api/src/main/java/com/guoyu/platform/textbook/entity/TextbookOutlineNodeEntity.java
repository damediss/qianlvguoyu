package com.guoyu.platform.textbook.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;

/**
 * 教材大纲树（textbook_outline）：篇/章/节/点 四级。
 */
@TableName("textbook_outline")
public class TextbookOutlineNodeEntity extends BaseEntity {

  private Long projectId;
  /** 0=根（篇的父） */
  private Long parentId;
  /** 1=篇 2=章 3=节 4=点 */
  private Integer nodeLevel;
  /** 点级：theory/skill；篇章节需写回 NULL（updateById 默认忽略 null，显式 ALWAYS） */
  @TableField(updateStrategy = FieldStrategy.ALWAYS)
  private String dimension;
  /** 标准原始编号（2.2.1 / 1.1），溯源用，不随编辑变 */
  private String sourceNo;
  private String titleRaw;
  private String title;
  /** 权重%（章级） */
  private BigDecimal weight;
  /** 目标字数（章/节级） */
  private Integer targetWords;
  /** 字数被人工调整=1（重算配比时保留） */
  private Integer wordsManual;
  private Integer sortNo;

  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

  public Long getParentId() {
    return parentId;
  }

  public void setParentId(Long parentId) {
    this.parentId = parentId;
  }

  public Integer getNodeLevel() {
    return nodeLevel;
  }

  public void setNodeLevel(Integer nodeLevel) {
    this.nodeLevel = nodeLevel;
  }

  public String getDimension() {
    return dimension;
  }

  public void setDimension(String dimension) {
    this.dimension = dimension;
  }

  public String getSourceNo() {
    return sourceNo;
  }

  public void setSourceNo(String sourceNo) {
    this.sourceNo = sourceNo;
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

  public BigDecimal getWeight() {
    return weight;
  }

  public void setWeight(BigDecimal weight) {
    this.weight = weight;
  }

  public Integer getTargetWords() {
    return targetWords;
  }

  public void setTargetWords(Integer targetWords) {
    this.targetWords = targetWords;
  }

  public Integer getWordsManual() {
    return wordsManual;
  }

  public void setWordsManual(Integer wordsManual) {
    this.wordsManual = wordsManual;
  }

  public Integer getSortNo() {
    return sortNo;
  }

  public void setSortNo(Integer sortNo) {
    this.sortNo = sortNo;
  }
}
