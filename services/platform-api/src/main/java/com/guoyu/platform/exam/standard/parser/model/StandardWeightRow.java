package com.guoyu.platform.exam.standard.parser.model;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 权重表单行：category（基本要求/相关知识要求/技能要求）+ item（项目名）+ 每「等级|方向」的百分比值。
 * 值为 null 表示该等级×方向无此项目（docx 中的 "—" 或空白）。
 */
public class StandardWeightRow {

  private String category;
  private String item;
  /** key = levelCode + "|" + direction（无方向工种 direction="默认"） */
  private final Map<String, BigDecimal> values = new LinkedHashMap<>();

  public static String keyOf(int levelCode, String direction) {
    return levelCode + "|" + direction;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getItem() {
    return item;
  }

  public void setItem(String item) {
    this.item = item;
  }

  public Map<String, BigDecimal> getValues() {
    return values;
  }

  public BigDecimal valueAt(int levelCode, String direction) {
    return values.get(keyOf(levelCode, direction));
  }
}
