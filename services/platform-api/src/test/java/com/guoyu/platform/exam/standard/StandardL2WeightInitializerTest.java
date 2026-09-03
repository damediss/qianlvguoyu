package com.guoyu.platform.exam.standard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * StandardL2WeightInitializer 单测：按知识小点条数比例分摊、最大余数法凑整、合计严格等于 L1 权重。
 */
class StandardL2WeightInitializerTest {

  private StandardTreeDraft l1(String key, BigDecimal weight) {
    return new StandardTreeDraft(key, "A", 1, 1, "theory", key, key, null, weight,
        null, null, "extract", "默认", 0, false);
  }

  private StandardTreeDraft l2(String key, String parent, String knowledge) {
    return new StandardTreeDraft(key, parent, 2, 1, "theory", key, key, null, null,
        knowledge, null, "extract", "默认", 0, false);
  }

  private static BigDecimal sum(Map<String, BigDecimal> weights) {
    return weights.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  @Test
  void allocatesByMaterialCount() {
    // L1=0.25（25 个 0.01 单位）；小节 A 3 条、B 1 条、C 1 条 → 15/5/5
    List<StandardTreeDraft> drafts = new ArrayList<>();
    drafts.add(l1("A-A", new BigDecimal("0.2500")));
    drafts.add(l2("A-A-A", "A-A", "1. 甲\n2. 乙\n3. 丙"));
    drafts.add(l2("A-A-B", "A-A", "1. 丁"));
    drafts.add(l2("A-A-C", "A-A", "1. 戊"));

    Map<String, BigDecimal> weights = StandardL2WeightInitializer.allocate(drafts);

    assertEquals(0, new BigDecimal("0.15").compareTo(weights.get("0|默认|A-A-A")));
    assertEquals(0, new BigDecimal("0.05").compareTo(weights.get("0|默认|A-A-B")));
    assertEquals(0, new BigDecimal("0.05").compareTo(weights.get("0|默认|A-A-C")));
    assertEquals(0, new BigDecimal("0.25").compareTo(sum(weights)));
  }

  @Test
  void largestRemainderFillsTiesInOrder() {
    // L1=0.25，3 个小节各 1 条：25 单位按 1:1:1 → 9/8/8（余数并列时排前者先补）
    List<StandardTreeDraft> drafts = new ArrayList<>();
    drafts.add(l1("A-A", new BigDecimal("0.2500")));
    drafts.add(l2("A-A-A", "A-A", "1. 甲"));
    drafts.add(l2("A-A-B", "A-A", "1. 乙"));
    drafts.add(l2("A-A-C", "A-A", "1. 丙"));

    Map<String, BigDecimal> weights = StandardL2WeightInitializer.allocate(drafts);

    assertEquals(0, new BigDecimal("0.09").compareTo(weights.get("0|默认|A-A-A")));
    assertEquals(0, new BigDecimal("0.08").compareTo(weights.get("0|默认|A-A-B")));
    assertEquals(0, new BigDecimal("0.08").compareTo(weights.get("0|默认|A-A-C")));
    assertEquals(0, new BigDecimal("0.25").compareTo(sum(weights)));
  }

  @Test
  void skipsWhenL1HasNoWeight() {
    // L1 无权重（权重表缺失降级）→ 不预填，保持导出时按叶数兜底
    List<StandardTreeDraft> drafts = new ArrayList<>();
    drafts.add(l1("A-A", null));
    drafts.add(l2("A-A-A", "A-A", "1. 甲"));

    assertTrue(StandardL2WeightInitializer.allocate(drafts).isEmpty());
  }

  @Test
  void emptySectionCountsAsOneMaterial() {
    // L1=0.10；小节 A 3 条、B 空小节（按 1 条计）→ 7.5/2.5 → 8/2（余数并列先补 A）
    List<StandardTreeDraft> drafts = new ArrayList<>();
    drafts.add(l1("A-A", new BigDecimal("0.1000")));
    drafts.add(l2("A-A-A", "A-A", "1. 甲\n2. 乙\n3. 丙"));
    drafts.add(l2("A-A-B", "A-A", ""));

    Map<String, BigDecimal> weights = StandardL2WeightInitializer.allocate(drafts);

    assertEquals(0, new BigDecimal("0.08").compareTo(weights.get("0|默认|A-A-A")));
    assertEquals(0, new BigDecimal("0.02").compareTo(weights.get("0|默认|A-A-B")));
    assertEquals(0, new BigDecimal("0.10").compareTo(sum(weights)));
  }

  @Test
  void ignoresChildrenOfOtherLevelsOrDirections() {
    // 同 nodeKey 但不同 levelCode/direction 的草稿不算本 L1 的小节
    List<StandardTreeDraft> drafts = new ArrayList<>();
    drafts.add(l1("A-A", new BigDecimal("0.0500")));
    drafts.add(l2("A-A-A", "A-A", "1. 甲"));
    drafts.add(new StandardTreeDraft("A-A-B", "A-A", 2, 2, "theory", "x", "x", null, null,
        "1. 乙", null, "extract", "默认", 1, false));

    Map<String, BigDecimal> weights = StandardL2WeightInitializer.allocate(drafts);

    assertEquals(1, weights.size());
    assertEquals(0, new BigDecimal("0.05").compareTo(weights.get("0|默认|A-A-A")));
  }
}
