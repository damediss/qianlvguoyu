package com.guoyu.platform.exam.standard.parser.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 权重表（理论知识权重表 / 技能要求权重表 共用）。
 */
public class StandardWeightTable {

  public enum Kind {
    THEORY, SKILL
  }

  private Kind kind;
  private final List<StandardWeightRow> rows = new ArrayList<>();

  public Kind getKind() {
    return kind;
  }

  public void setKind(Kind kind) {
    this.kind = kind;
  }

  public List<StandardWeightRow> getRows() {
    return rows;
  }

  /** 按项目名（归一化后）查找行：先精确匹配；未命中再用宽松归一化兜底（剥枚举编号/尾注，见 normalizeMatchKey） */
  public StandardWeightRow findRow(String normalizedItem) {
    for (StandardWeightRow row : rows) {
      if (DocxTextUtil.normalizeItemName(row.getItem()).equals(normalizedItem)) {
        return row;
      }
    }
    String loose = DocxTextUtil.normalizeMatchKey(normalizedItem);
    if (loose.isEmpty()) {
      return null;
    }
    for (StandardWeightRow row : rows) {
      if (DocxTextUtil.normalizeMatchKey(row.getItem()).equals(loose)) {
        return row;
      }
    }
    return null;
  }
}
