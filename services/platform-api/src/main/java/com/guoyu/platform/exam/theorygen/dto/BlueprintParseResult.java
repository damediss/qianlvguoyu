package com.guoyu.platform.exam.theorygen.dto;

import java.util.ArrayList;
import java.util.List;

/** 细目表 xlsx 解析结果 */
public class BlueprintParseResult {

  private String fileName;
  private List<NodeRow> rows = new ArrayList<>();
  private List<String> warnings = new ArrayList<>();

  /** 行数据：父级以 parentKey 关联（解析期临时键，入库时换 id） */
  public static class NodeRow {
    private int nodeLevel;
    private String code;
    private String name;
    private java.math.BigDecimal weight;
    private String importance;
    private String relatedKnowledge;
    private String xyzStat;
    private String parentKey;
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
    public String getImportance() { return importance; }
    public void setImportance(String importance) { this.importance = importance; }
    public String getRelatedKnowledge() { return relatedKnowledge; }
    public void setRelatedKnowledge(String relatedKnowledge) { this.relatedKnowledge = relatedKnowledge; }
    public String getXyzStat() { return xyzStat; }
    public void setXyzStat(String xyzStat) { this.xyzStat = xyzStat; }
    public String getParentKey() { return parentKey; }
    public void setParentKey(String parentKey) { this.parentKey = parentKey; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public int getRowIndex() { return rowIndex; }
    public void setRowIndex(int rowIndex) { this.rowIndex = rowIndex; }
    public int getParentIndex() { return parentIndex; }
    public void setParentIndex(int parentIndex) { this.parentIndex = parentIndex; }
  }

  public String getFileName() { return fileName; }
  public void setFileName(String fileName) { this.fileName = fileName; }
  public List<NodeRow> getRows() { return rows; }
  public void setRows(List<NodeRow> rows) { this.rows = rows; }
  public List<String> getWarnings() { return warnings; }
  public void setWarnings(List<String> warnings) { this.warnings = warnings; }
}
