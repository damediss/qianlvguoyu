package com.guoyu.platform.exam.standard.parser.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 单等级文档块：3.x 节（五级/初级工 … 一级/高级技师）的工作要求与双权重表。
 */
public class StandardLevelDoc {

  /** 0=五级/初级 … 4=一级/高级技师 */
  private int levelCode;
  /** 如 "五级/初级工" */
  private String levelName;
  /** 工作要求 L1（职业功能）列表，含 L2（工作内容）children */
  private final List<StandardNodeDoc> workFunctions = new ArrayList<>();

  public int getLevelCode() {
    return levelCode;
  }

  public void setLevelCode(int levelCode) {
    this.levelCode = levelCode;
  }

  public String getLevelName() {
    return levelName;
  }

  public void setLevelName(String levelName) {
    this.levelName = levelName;
  }

  public List<StandardNodeDoc> getWorkFunctions() {
    return workFunctions;
  }
}
