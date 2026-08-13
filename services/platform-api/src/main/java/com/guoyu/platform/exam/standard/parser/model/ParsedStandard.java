package com.guoyu.platform.exam.standard.parser.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * docx 解析产物：职业元信息 + 基本要求树（等级无关）+ 各等级工作要求 + 双权重表 + 警告。
 */
public class ParsedStandard {

  public static final String DEFAULT_DIRECTION = "默认";

  private String workTypeTitle;
  private String seriesNo;
  private String version;
  /** 方向列表；无方向工种为单元素 ["默认"] */
  private final List<String> directions = new ArrayList<>();
  /** 1.4 职业技能等级：方向 → 允许等级代码列表（权威约束）；未解析到为空 */
  private final Map<String, List<Integer>> directionLevels = new LinkedHashMap<>();
  /** 基本要求 L1 列表（2.x），含 L2 children 与理论素材；等级无关 */
  private final List<StandardNodeDoc> basicSections = new ArrayList<>();
  private final List<StandardLevelDoc> levels = new ArrayList<>();
  /** 理论知识权重表（文档级，行内按 等级|方向 取值） */
  private StandardWeightTable theoryWeights;
  /** 技能要求权重表（文档级，可能为 null——个别标准只有理论权重表） */
  private StandardWeightTable skillWeights;
  private final List<String> parseWarnings = new ArrayList<>();
  /** 统计：识别到的工作要求表格数 */
  private int workTableCount;
  /** 统计：识别到的权重表格数 */
  private int weightTableCount;
  /** 统计：文档表格总数 */
  private int totalTableCount;

  public String getWorkTypeTitle() {
    return workTypeTitle;
  }

  public void setWorkTypeTitle(String workTypeTitle) {
    this.workTypeTitle = workTypeTitle;
  }

  public String getSeriesNo() {
    return seriesNo;
  }

  public void setSeriesNo(String seriesNo) {
    this.seriesNo = seriesNo;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public List<String> getDirections() {
    return directions;
  }

  public Map<String, List<Integer>> getDirectionLevels() {
    return directionLevels;
  }

  public List<StandardNodeDoc> getBasicSections() {
    return basicSections;
  }

  public List<StandardLevelDoc> getLevels() {
    return levels;
  }

  public StandardWeightTable getTheoryWeights() {
    return theoryWeights;
  }

  public void setTheoryWeights(StandardWeightTable theoryWeights) {
    this.theoryWeights = theoryWeights;
  }

  public StandardWeightTable getSkillWeights() {
    return skillWeights;
  }

  public void setSkillWeights(StandardWeightTable skillWeights) {
    this.skillWeights = skillWeights;
  }

  public List<String> getParseWarnings() {
    return parseWarnings;
  }

  public StandardLevelDoc levelOf(int levelCode) {
    for (StandardLevelDoc l : levels) {
      if (l.getLevelCode() == levelCode) {
        return l;
      }
    }
    return null;
  }

  public int getWorkTableCount() {
    return workTableCount;
  }

  public void setWorkTableCount(int workTableCount) {
    this.workTableCount = workTableCount;
  }

  public int getWeightTableCount() {
    return weightTableCount;
  }

  public void setWeightTableCount(int weightTableCount) {
    this.weightTableCount = weightTableCount;
  }

  public int getTotalTableCount() {
    return totalTableCount;
  }

  public void setTotalTableCount(int totalTableCount) {
    this.totalTableCount = totalTableCount;
  }
}
