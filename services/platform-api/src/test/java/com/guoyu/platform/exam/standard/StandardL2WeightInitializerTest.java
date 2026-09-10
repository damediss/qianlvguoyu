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

  private StandardTreeDraft l2(String key, String parent, String knowledge, String titleRaw) {
    return new StandardTreeDraft(key, parent, 2, 1, "theory", key, titleRaw, null, null,
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

  @Test
  void lawSectionCappedAtFivePercentAndExcessRedistributed() {
    // 矿井通风工场景：L1=0.25（25 单位）；矿山安全 3 条、机械基础 1 条、法律法规 10 条
    // 初始 5/2/18 → 法律法规钳到 5，释放 13 按 3:1 再分（10/3）→ 15/5/5，合计仍 = L1
    List<StandardTreeDraft> drafts = new ArrayList<>();
    drafts.add(l1("A-B", new BigDecimal("0.2500")));
    drafts.add(l2("A-B-A", "A-B", "1. 甲\n2. 乙\n3. 丙", "矿山安全基础知识"));
    drafts.add(l2("A-B-B", "A-B", "1. 丁", "机械基础知识"));
    drafts.add(l2("A-B-C", "A-B",
        "1. 劳动法\n2. 劳动合同法\n3. 矿山安全法\n4. 职业病防治法\n5. 安全生产法\n6. 矿山安全法实施条例"
            + "\n7. 煤矿安全规程\n8. 金属非金属矿山安全规程\n9. 尾矿库安全规程\n10. 行业标准",
        "相关法律法规知识"));

    Map<String, BigDecimal> weights = StandardL2WeightInitializer.allocate(drafts);

    assertEquals(0, new BigDecimal("0.05").compareTo(weights.get("0|默认|A-B-C")), "法律法规钳制到 5%");
    assertEquals(0, new BigDecimal("0.15").compareTo(weights.get("0|默认|A-B-A")), "释放权重按比例再分给兄弟");
    assertEquals(0, new BigDecimal("0.05").compareTo(weights.get("0|默认|A-B-B")));
    assertEquals(0, new BigDecimal("0.25").compareTo(sum(weights)), "L2 合计仍严格等于 L1 权重");
  }

  @Test
  void lawSectionUnderCapKeepsShare() {
    // L1=0.15（15 单位）；其它 3 条、法律法规 1 条 → 11/4，法律法规 ≤5% 不触发钳制
    List<StandardTreeDraft> drafts = new ArrayList<>();
    drafts.add(l1("A-B", new BigDecimal("0.1500")));
    drafts.add(l2("A-B-A", "A-B", "1. 甲\n2. 乙\n3. 丙", "矿山安全基础知识"));
    drafts.add(l2("A-B-B", "A-B", "1. 劳动法", "相关法律法规知识"));

    Map<String, BigDecimal> weights = StandardL2WeightInitializer.allocate(drafts);

    assertEquals(0, new BigDecimal("0.11").compareTo(weights.get("0|默认|A-B-A")));
    assertEquals(0, new BigDecimal("0.04").compareTo(weights.get("0|默认|A-B-B")));
    assertEquals(0, new BigDecimal("0.15").compareTo(sum(weights)));
  }

  @Test
  void lawSectionTitleVariantAlsoCapped() {
    // "法律、法规相关知识"（顿号变体）同样命中上限；L1=0.25，法律法规 10 条、兄弟 1 条
    List<StandardTreeDraft> drafts = new ArrayList<>();
    drafts.add(l1("A-B", new BigDecimal("0.2500")));
    drafts.add(l2("A-B-A", "A-B",
        "1. 劳动法\n2. 劳动合同法\n3. 矿山安全法\n4. 职业病防治法\n5. 安全生产法\n6. 矿山安全法实施条例"
            + "\n7. 煤矿安全规程\n8. 金属非金属矿山安全规程\n9. 尾矿库安全规程\n10. 行业标准",
        "法律、法规相关知识"));
    drafts.add(l2("A-B-B", "A-B", "1. 甲", "机械基础知识"));

    Map<String, BigDecimal> weights = StandardL2WeightInitializer.allocate(drafts);

    assertEquals(0, new BigDecimal("0.05").compareTo(weights.get("0|默认|A-B-A")), "顿号变体也钳制到 5%");
    assertEquals(0, new BigDecimal("0.20").compareTo(weights.get("0|默认|A-B-B")));
    assertEquals(0, new BigDecimal("0.25").compareTo(sum(weights)));
  }

  @Test
  void lawSectionWithoutSiblingsDropsExcess() {
    // 法律法规独占 L1=0.25 → 钳到 0.05，无兄弟可再分（合计 < L1，退化场景接受）
    List<StandardTreeDraft> drafts = new ArrayList<>();
    drafts.add(l1("A-B", new BigDecimal("0.2500")));
    drafts.add(l2("A-B-A", "A-B", "1. 劳动法\n2. 安全生产法", "相关法律法规知识"));

    Map<String, BigDecimal> weights = StandardL2WeightInitializer.allocate(drafts);

    assertEquals(0, new BigDecimal("0.05").compareTo(weights.get("0|默认|A-B-A")));
    assertEquals(1, weights.size());
  }
}
