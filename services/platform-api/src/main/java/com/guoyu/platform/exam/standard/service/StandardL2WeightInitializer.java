package com.guoyu.platform.exam.standard.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 解析建档时 L2 考评权重初始化。
 *
 * <p>国标权重表只到「中类/职业功能」（树中 L1）粒度，L2 小节没有权重数据。本类在 staging 建树时按相关知识小点条数，把 L1 权重用最大余数法预分摊到其下 L2 小节（合计严格等于 L1 权重，凑整到 0.01），便于树编辑页预览/微调；用户仍可在树编辑页手工改权重。导出与 AI 概括点数分配会把它当作已填写的权重优先采用。
 */
public final class StandardL2WeightInitializer {

  private StandardL2WeightInitializer() {
  }

  /**
   * @return treeMapKey(level|direction|nodeKey) → 预分配的 L2 权重；L1 无权重或其下无小节时不填
   */
  public static Map<String, BigDecimal> allocate(List<StandardTreeDraft> drafts) {
    Map<String, BigDecimal> result = new HashMap<>();
    for (StandardTreeDraft l1 : drafts) {
      if (l1.nodeLevel() != 1 || l1.weight() == null || l1.weight().signum() <= 0) {
        continue;
      }
      List<StandardTreeDraft> children = new ArrayList<>();
      for (StandardTreeDraft d : drafts) {
        if (d.nodeLevel() == 2 && Objects.equals(d.parentNodeKey(), l1.nodeKey())
            && d.levelCode() == l1.levelCode() && Objects.equals(d.direction(), l1.direction())) {
          children.add(d);
        }
      }
      if (children.isEmpty()) {
        continue;
      }
      // 单位换算：0.01 → 1 个单位（权重 0.25 → 25 个单位）
      int totalUnits = l1.weight().setScale(2, RoundingMode.HALF_UP).movePointRight(2).intValue();
      if (totalUnits <= 0) {
        continue;
      }
      int n = children.size();
      int[] counts = new int[n];
      for (int i = 0; i < n; i++) {
        counts[i] = materials(children.get(i).relatedKnowledge());
      }
      int[] units = largestRemainder(totalUnits, counts);
      for (int i = 0; i < n; i++) {
        if (units[i] > 0) {
          String key = StandardParseApplicationService.treeMapKey(children.get(i).levelCode(),
              children.get(i).direction(), children.get(i).nodeKey());
          result.put(key, new BigDecimal(units[i]).movePointLeft(2));
        }
      }
    }
    return result;
  }

  /** 相关知识小点条数：按行拆分、剥离行首编号（1. / 1.1.1 / （1）），空行不计；空小节按 1 计避免除零 */
  private static int materials(String raw) {
    if (raw == null || raw.isBlank()) {
      return 1;
    }
    int count = 0;
    for (String line : raw.split("\n")) {
      String stripped = line.strip()
          .replaceAll("^(\\d+(\\.\\d+)*[.、]|[（(]\\d+[）)])\\s*", "");
      if (!stripped.isBlank()) {
        count++;
      }
    }
    return Math.max(count, 1);
  }

  /** 最大余数法：把 totalUnits 个单位按 counts 比例分配，保证合计严格等于 totalUnits */
  private static int[] largestRemainder(int totalUnits, int[] counts) {
    int n = counts.length;
    int[] result = new int[n];
    int totalCount = 0;
    for (int c : counts) {
      totalCount += c;
    }
    if (totalCount <= 0) {
      // 兜底均分（materials 保底 1，理论不会走到）
      for (int i = 0; i < n; i++) {
        result[i] = totalUnits / n + (i < totalUnits % n ? 1 : 0);
      }
      return result;
    }
    int allocated = 0;
    double[] remainders = new double[n];
    for (int i = 0; i < n; i++) {
      double exact = (double) totalUnits * counts[i] / totalCount;
      result[i] = (int) Math.floor(exact);
      remainders[i] = exact - result[i];
      allocated += result[i];
    }
    // 余数大者优先各补 1 个单位；小数份额之和 < n，leftover 必然 < n，一轮即可凑满
    int leftover = totalUnits - allocated;
    while (leftover > 0) {
      int best = -1;
      double bestRem = -1;
      for (int i = 0; i < n; i++) {
        if (remainders[i] > bestRem) {
          bestRem = remainders[i];
          best = i;
        }
      }
      result[best]++;
      remainders[best] = -1;
      leftover--;
    }
    return result;
  }
}