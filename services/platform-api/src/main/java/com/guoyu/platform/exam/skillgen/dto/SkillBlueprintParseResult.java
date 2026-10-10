package com.guoyu.platform.exam.skillgen.dto;

import java.util.ArrayList;
import java.util.List;

/** 操作技能细目表 xlsx 解析结果 */
public class SkillBlueprintParseResult {

  private String fileName;
  private String workTypeTitle;
  private List<NodeRow> rows = new ArrayList<>();
  private List<String> warnings = new ArrayList<>();

  /** 行数据：父级以行索引关联（解析期临时结构，入库时换 id） */
  public static class NodeRow {
    private int nodeLevel;
    private String code;
    private String name;
    private java.math.BigDecimal weight;
    private String selectMode;
    private String skillRequirement;
    private String importance;
    private Integer questionQuota;
    private String xyzStat;
    private int sortOrder;
    /** 解析期行索引（0 起，全 rows 序），供父 id 映射 */
    private int rowIndex = -1;
    /** 父行索引（-1=根） */
    private int parentIndex = -1;

    public int getNodeLevel() { return nodeLevel; }
    public void setNodeLevel(int nodeLevel) { this.nodeLevel = nodeLevel; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public java.math.BigDecimal getWeight() { return weight; }
    public void setWeight(java.math.BigDecimal weight) { this.weight = weight; }
    public String getSelectMode() { return selectMode; }
    public void setSelectMode(String selectMode) { this.selectMode = selectMode; }
    public String getSkillRequirement() { return skillRequirement; }
    public void setSkillRequirement(String skillRequirement) { this.skillRequirement = skillRequirement; }
    public String getImportance() { return importance; }
    public void setImportance(String importance) { this.importance = importance; }
    public Integer getQuestionQuota() { return questionQuota; }
    public void setQuestionQuota(Integer questionQuota) { this.questionQuota = questionQuota; }
    public String getXyzStat() { return xyzStat; }
    public void setXyzStat(String xyzStat) { this.xyzStat = xyzStat; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public int getRowIndex() { return rowIndex; }
    public void setRowIndex(int rowIndex) { this.rowIndex = rowIndex; }
    public int getParentIndex() { return parentIndex; }
    public void setParentIndex(int parentIndex) { this.parentIndex = parentIndex; }
  }

  public String getFileName() { return fileName; }
  public void setFileName(String fileName) { this.fileName = fileName; }
  public String getWorkTypeTitle() { return workTypeTitle; }
  public void setWorkTypeTitle(String workTypeTitle) { this.workTypeTitle = workTypeTitle; }
  public List<NodeRow> getRows() { return rows; }
  public void setRows(List<NodeRow> rows) { this.rows = rows; }
  public List<String> getWarnings() { return warnings; }
  public void setWarnings(List<String> warnings) { this.warnings = warnings; }
}
