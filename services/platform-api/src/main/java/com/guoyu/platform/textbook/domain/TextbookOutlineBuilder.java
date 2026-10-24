package com.guoyu.platform.textbook.domain;

import com.guoyu.platform.exam.standard.parser.model.DocxTextUtil;
import com.guoyu.platform.exam.standard.parser.model.ParsedStandard;
import com.guoyu.platform.exam.standard.parser.model.StandardLevelDoc;
import com.guoyu.platform.exam.standard.parser.model.StandardNodeDoc;
import com.guoyu.platform.exam.standard.parser.model.StandardWeightRow;
import com.guoyu.platform.exam.standard.parser.model.StandardWeightTable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 教材骨架生成（设计 §2.4-③，纯规则）。
 *
 * <ul>
 *   <li>篇：固定两个 L1（"基本要求"/"工作要求"）；</li>
 *   <li>章：基础篇=2.x；专业篇=该等级经方向过滤后的职业功能（L1），章权重取理论知识权重表对应行；</li>
 *   <li>权重为 null（—）或无权重表行的项目不生章（实测：三级无"工作准备"、二级无"设备管理"）；</li>
 *   <li>节=工作内容（L2）；点=theory/skill 素材逐条，source_no 记标准编号（L2 编号.条目序）；</li>
 *   <li>方向权重列缺失时回退"默认"列（铝电解工权重表按等级列、无方向行）。</li>
 * </ul>
 *
 * <p>产出 {@link OutlineNodeDraft} 扁平列表（parentIndex 关联），字数配比由
 * {@link TextbookWordAllocator} 负责，id/parentId 由 ApplicationService 落库时分配。
 */
public final class TextbookOutlineBuilder {

  /** 骨架节点草稿（不含 id；parentIndex 指向返回列表内的下标，-1=顶层篇） */
  public record OutlineNodeDraft(
      int nodeLevel,
      int parentIndex,
      String sourceNo,
      String titleRaw,
      String title,
      String dimension,
      BigDecimal weight,
      int sortNo
  ) {
  }

  /** 骨架 + 生成期告警（不生章原因等） */
  public record BuildResult(List<OutlineNodeDraft> nodes, List<String> warnings) {
  }

  private TextbookOutlineBuilder() {
  }

  /**
   * @param levelCode 解析器口径（0=五级 … 4=一级）
   * @param functionNos 方向过滤后的职业功能序号（1 起）；单方向工种传全部序号
   */
  public static BuildResult build(ParsedStandard parsed, String direction, int levelCode,
      List<Integer> functionNos) {
    List<OutlineNodeDraft> nodes = new ArrayList<>();
    List<String> warnings = new ArrayList<>();
    StandardLevelDoc level = parsed.levelOf(levelCode);
    if (level == null) {
      warnings.add("WARN: 标准中不存在该等级节（levelCode=" + levelCode + "），骨架为空");
      return new BuildResult(nodes, warnings);
    }

    // 篇一：基本要求（等级无关）
    int basicPart = part(nodes, "基本要求", "2", 1);
    int sort = 0;
    for (StandardNodeDoc l1 : parsed.getBasicSections()) {
      BigDecimal weight = chapterWeight(parsed.getTheoryWeights(), l1.getTitle(), levelCode,
          direction);
      if (weight == null) {
        warnings.add("WARN: 权重表无「" + l1.getTitle() + "」行（或该等级为 —），不生章");
        continue;
      }
      int chapter = chapter(nodes, basicPart, l1.getNumber(), l1.getTitle(), weight, ++sort);
      int sectionSort = 0;
      for (StandardNodeDoc l2 : l1.getChildren()) {
        int section = section(nodes, chapter, l2.getNumber(), l2.getTitle(), ++sectionSort);
        addPoints(nodes, section, l2, "theory", 0);
      }
    }

    // 篇二：工作要求（方向过滤后的职业功能）
    int workPart = part(nodes, "工作要求", "3", 2);
    sort = 0;
    for (StandardNodeDoc func : level.getWorkFunctions()) {
      Integer no = parseFunctionNo(func.getNumber());
      if (no == null || !functionNos.contains(no)) {
        continue;
      }
      BigDecimal weight = chapterWeight(parsed.getTheoryWeights(), func.getTitle(), levelCode,
          direction);
      if (weight == null) {
        warnings.add("WARN: 职业功能「" + func.getTitle() + "」权重为 —（或权重表无此行），不生章");
        continue;
      }
      int chapter = chapter(nodes, workPart, func.getNumber(), func.getTitle(), weight, ++sort);
      int sectionSort = 0;
      for (StandardNodeDoc l2 : func.getChildren()) {
        int section = section(nodes, chapter, l2.getNumber(), l2.getTitle(), ++sectionSort);
        int nextSort = addPoints(nodes, section, l2, "theory", 0);
        addPoints(nodes, section, l2, "skill", nextSort);
      }
    }
    return new BuildResult(nodes, warnings);
  }

  /** 章权重：先按方向列取，缺失回退"默认"列（无方向工种权重表只有默认列） */
  static BigDecimal chapterWeight(StandardWeightTable theoryWeights, String itemTitle,
      int levelCode, String direction) {
    if (theoryWeights == null) {
      return null;
    }
    StandardWeightRow row = theoryWeights.findRow(DocxTextUtil.normalizeItemName(itemTitle));
    if (row == null) {
      return null;
    }
    BigDecimal value = row.valueAt(levelCode, direction);
    if (value == null && !ParsedStandard.DEFAULT_DIRECTION.equals(direction)) {
      value = row.valueAt(levelCode, ParsedStandard.DEFAULT_DIRECTION);
    }
    return value;
  }

  /** 工作要求 L1 编号 → 职业功能序号（"1"→1）；非纯数字编号返回 null */
  public static Integer parseFunctionNo(String number) {
    if (number == null) {
      return null;
    }
    try {
      return Integer.valueOf(number.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static int part(List<OutlineNodeDraft> nodes, String title, String sourceNo, int sortNo) {
    nodes.add(new OutlineNodeDraft(1, -1, sourceNo, title, title, null, null, sortNo));
    return nodes.size() - 1;
  }

  private static int chapter(List<OutlineNodeDraft> nodes, int partIndex, String sourceNo,
      String title, BigDecimal weight, int sortNo) {
    nodes.add(new OutlineNodeDraft(2, partIndex, sourceNo, title, title, null, weight, sortNo));
    return nodes.size() - 1;
  }

  private static int section(List<OutlineNodeDraft> nodes, int chapterIndex, String sourceNo,
      String title, int sortNo) {
    nodes.add(new OutlineNodeDraft(3, chapterIndex, sourceNo, title, title, null, null, sortNo));
    return nodes.size() - 1;
  }

  /** 素材逐条成点；source_no = 节编号.条目序（理论/技能各自从 1 编号，与标准条目编号一致），sortNo 从 sortBase+1 起 */
  private static int addPoints(List<OutlineNodeDraft> nodes, int sectionIndex,
      StandardNodeDoc l2, String dimension, int sortBase) {
    List<String> materials = "theory".equals(dimension)
        ? l2.getTheoryMaterials() : l2.getSkillMaterials();
    int seq = 0;
    int sort = sortBase;
    for (String material : materials) {
      String text = material == null ? "" : material.trim();
      if (text.isEmpty()) {
        continue;
      }
      seq++;
      sort++;
      String sourceNo = (l2.getNumber() == null ? "" : l2.getNumber()) + "." + seq;
      nodes.add(new OutlineNodeDraft(4, sectionIndex, sourceNo, text, text, dimension, null, sort));
    }
    return sort;
  }
}
