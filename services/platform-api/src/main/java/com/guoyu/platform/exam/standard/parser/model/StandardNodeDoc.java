package com.guoyu.platform.exam.standard.parser.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 骨架节点（解析期 POJO）。
 * 基本要求段：level 1=L1（2.x），level 2=L2（2.x.y）；
 * 工作要求表：level 1=职业功能，level 2=工作内容。
 */
public class StandardNodeDoc {

  private int level;
  /** 原始编号，如 "2.1" / "1" / "1.1" */
  private String number;
  /** 纯标题（不含编号与编码后缀） */
  private String title;
  /** 理论叶素材（相关知识要求编号项 / 基本要求（n）项） */
  private final List<String> theoryMaterials = new ArrayList<>();
  /** 技能叶素材（技能要求编号项，"能……"句式） */
  private final List<String> skillMaterials = new ArrayList<>();
  /** L2 相关知识原文（理论细目表"相关知识"列） */
  private String relatedKnowledgeRaw;
  /** L2 技能要求原文（技能细目表"技能要求"列） */
  private String skillRaw;
  /** 无列举项，需 LLM 概括补叶 */
  private boolean needLlm;
  private final List<StandardNodeDoc> children = new ArrayList<>();

  public int getLevel() {
    return level;
  }

  public void setLevel(int level) {
    this.level = level;
  }

  public String getNumber() {
    return number;
  }

  public void setNumber(String number) {
    this.number = number;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public List<String> getTheoryMaterials() {
    return theoryMaterials;
  }

  public List<String> getSkillMaterials() {
    return skillMaterials;
  }

  public String getRelatedKnowledgeRaw() {
    return relatedKnowledgeRaw;
  }

  public void setRelatedKnowledgeRaw(String relatedKnowledgeRaw) {
    this.relatedKnowledgeRaw = relatedKnowledgeRaw;
  }

  public String getSkillRaw() {
    return skillRaw;
  }

  public void setSkillRaw(String skillRaw) {
    this.skillRaw = skillRaw;
  }

  public boolean isNeedLlm() {
    return needLlm;
  }

  public void setNeedLlm(boolean needLlm) {
    this.needLlm = needLlm;
  }

  public List<StandardNodeDoc> getChildren() {
    return children;
  }
}
