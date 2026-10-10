package com.guoyu.platform.exam.standard.service;

import com.guoyu.platform.exam.standard.parser.model.DocxTextUtil;
import com.guoyu.platform.exam.standard.parser.model.ParsedStandard;
import com.guoyu.platform.exam.standard.parser.model.StandardLevelDoc;
import com.guoyu.platform.exam.standard.parser.model.StandardNodeDoc;
import com.guoyu.platform.exam.standard.parser.model.StandardWeightRow;
import com.guoyu.platform.exam.standard.parser.model.StandardWeightTable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 骨架树构建器（方案 §2.2 / §8）：ParsedStandard → 每「等级×方向」一棵 staging 树。
 *
 * <ul>
 *   <li>基本要求 A 段（职业道德/基础知识）对所有方向共享；</li>
 *   <li>工作要求 B 段的职业功能按理论权重矩阵的「等级×方向」归属过滤（值非空即属于该方向）；</li>
 *   <li>L0/L1 权重取权重表项目级值（百分比 ÷100，4 位小数）；L2 权重导出时按叶数比例计算；</li>
 *   <li>权重表缺失时所有骨架节点权重留空，页面提示手工填写（§12 降级）。</li>
 * </ul>
 */
public class StandardTreeBuilder {

  /**
   * @return 全量节点草稿（含所有 等级×方向），parentNodeKey 串起父子关系
   */
  public List<StandardTreeDraft> build(ParsedStandard standard) {
    List<StandardTreeDraft> drafts = new ArrayList<>();
    for (StandardLevelDoc level : standard.getLevels()) {
      for (String direction : standard.getDirections()) {
        buildTree(standard, level, direction, drafts);
      }
    }
    return drafts;
  }

  private void buildTree(ParsedStandard standard, StandardLevelDoc level, String direction,
      List<StandardTreeDraft> drafts) {
    int levelCode = level.getLevelCode();
    StandardWeightTable theory = standard.getTheoryWeights();

    // ---- 0) 权威约束：1.4 职业技能等级（方向 → 允许等级列表），不适用整棵跳过 ----
    Map<String, List<Integer>> directionLevels = standard.getDirectionLevels();
    if (!directionLevels.isEmpty()) {
      List<Integer> allowed = directionLevels.get(direction);
      if (allowed != null) {
        if (!allowed.contains(levelCode)) {
          standard.getParseWarnings().add("WARN: 按 1.4 职业技能等级，" + direction + " 不设 "
              + level.getLevelName() + "，跳过建树");
          return;
        }
      } else {
        standard.getParseWarnings().add("WARN: 1.4 未列出工种 " + direction
            + " 的等级范围，按权重表兜底判断");
      }
    }

    // ---- 0.5) 计算 A 支权重与 B 支成员，权重表兜底判断须在建 A 支之前 ----
    BigDecimal aWeight = weightOf(theory, "职业道德", levelCode, direction);
    aWeight = sum(aWeight, weightOf(theory, "基础知识", levelCode, direction));
    List<StandardNodeDoc> memberFunctions = new ArrayList<>();
    for (StandardNodeDoc func : level.getWorkFunctions()) {
      if (isMember(theory, func.getTitle(), levelCode, direction)) {
        memberFunctions.add(func);
      }
    }
    // 方向不适用于该等级（如焊工二级/一级无气焊工）：整棵树跳过（含 A 支）
    if (theory != null && aWeight == null && memberFunctions.isEmpty()) {
      standard.getParseWarnings().add("WARN: 等级×方向无权重数据，跳过建树：" + level.getLevelName()
          + " / " + direction);
      return;
    }

    // ---- L0 A：基本要求 ----
    drafts.add(new StandardTreeDraft("A", null, 0, 1, "theory",
        NodeKeyGenerator.titleWithKey("基本要求", "A"), "基本要求", null, aWeight,
        null, null, "extract", direction, levelCode, false));

    // ---- A 段 L1/L2（基本要求各方向共享） ----
    int l1Index = 0;
    for (StandardNodeDoc basicL1 : standard.getBasicSections()) {
      String l1Key = NodeKeyGenerator.childKey("A", l1Index);
      BigDecimal l1Weight = weightOf(theory, basicL1.getTitle(), levelCode, direction);
      drafts.add(new StandardTreeDraft(l1Key, "A", 1, l1Index + 1, "theory",
          NodeKeyGenerator.titleWithKey(basicL1.getTitle(), l1Key), basicL1.getTitle(), null,
          l1Weight, null, null, "extract", direction, levelCode, false));
      int l2Index = 0;
      for (StandardNodeDoc basicL2 : basicL1.getChildren()) {
        String l2Key = NodeKeyGenerator.childKey(l1Key, l2Index);
        drafts.add(new StandardTreeDraft(l2Key, l1Key, 2, l2Index + 1, "theory",
            NodeKeyGenerator.titleWithKey(basicL2.getTitle(), l2Key), basicL2.getTitle(), null,
            null, basicL2.getRelatedKnowledgeRaw(), null, "extract", direction, levelCode,
            basicL2.isNeedLlm()));
        l2Index++;
      }
      l1Index++;
    }

    // ---- L0 B：工作要求（职业功能按 等级×方向 归属过滤） ----
    BigDecimal bWeight = null;
    for (StandardNodeDoc func : memberFunctions) {
      bWeight = sum(bWeight, weightOf(theory, func.getTitle(), levelCode, direction));
    }
    drafts.add(new StandardTreeDraft("B", null, 0, 2, "theory",
        NodeKeyGenerator.titleWithKey("工作要求", "B"), "工作要求", null, bWeight,
        null, null, "extract", direction, levelCode, false));

    int fIndex = 0;
    for (StandardNodeDoc func : memberFunctions) {
      String fKey = NodeKeyGenerator.childKey("B", fIndex);
      BigDecimal fWeight = weightOf(theory, func.getTitle(), levelCode, direction);
      drafts.add(new StandardTreeDraft(fKey, "B", 1, fIndex + 1, "theory",
          NodeKeyGenerator.titleWithKey(func.getTitle(), fKey), func.getTitle(), null,
          fWeight, null, null, "extract", direction, levelCode, false));
      int cIndex = 0;
      for (StandardNodeDoc content : func.getChildren()) {
        String cKey = NodeKeyGenerator.childKey(fKey, cIndex);
        drafts.add(new StandardTreeDraft(cKey, fKey, 2, cIndex + 1, "theory",
            NodeKeyGenerator.titleWithKey(content.getTitle(), cKey), content.getTitle(), null,
            null, content.getRelatedKnowledgeRaw(), content.getSkillRaw(), "extract",
            direction, levelCode, content.isNeedLlm()));
        cIndex++;
      }
      fIndex++;
    }
  }

  /** 职业功能是否属于该 等级×方向：权重表存在时以值非空为准；权重表缺失/无此行时默认属于 */
  private boolean isMember(StandardWeightTable theory, String funcTitle, int levelCode,
      String direction) {
    if (theory == null) {
      return true;
    }
    StandardWeightRow row = theory.findRow(DocxTextUtil.normalizeItemName(funcTitle));
    if (row == null) {
      return true;
    }
    return valueAtWithDefaultFallback(row, levelCode, direction) != null;
  }

  /** 权重表取值：百分比 → 小数（25 → 0.2500）；无表/无行/— 返回 null */
  public static BigDecimal weightOf(StandardWeightTable table, String item, int levelCode,
      String direction) {
    if (table == null) {
      return null;
    }
    StandardWeightRow row = table.findRow(DocxTextUtil.normalizeItemName(item));
    if (row == null) {
      return null;
    }
    BigDecimal percent = valueAtWithDefaultFallback(row, levelCode, direction);
    if (percent == null) {
      return null;
    }
    return percent.movePointLeft(2).setScale(4, RoundingMode.HALF_UP);
  }

  /**
   * 按 等级×方向 取值，方向键不存在（非 "—" 显式空值）时回退「默认」方向：
   * 兼容方向行未识别/方向被污染的历史数据；方向键存在但值为 null（docx "—"）时不回退。
   */
  private static BigDecimal valueAtWithDefaultFallback(StandardWeightRow row, int levelCode,
      String direction) {
    String key = StandardWeightRow.keyOf(levelCode, direction);
    if (row.getValues().containsKey(key)) {
      return row.getValues().get(key);
    }
    if (!ParsedStandard.DEFAULT_DIRECTION.equals(direction)) {
      return row.getValues().get(StandardWeightRow.keyOf(levelCode, ParsedStandard.DEFAULT_DIRECTION));
    }
    return null;
  }

  /** 技能权重表取值（技能细目表一级比重） */
  public static BigDecimal skillWeightOf(ParsedStandard standard, String funcTitle, int levelCode,
      String direction) {
    return weightOf(standard.getSkillWeights(), funcTitle, levelCode, direction);
  }

  private static BigDecimal sum(BigDecimal a, BigDecimal b) {
    if (a == null) {
      return b;
    }
    if (b == null) {
      return a;
    }
    return a.add(b).setScale(4, RoundingMode.HALF_UP);
  }
}
