package com.guoyu.platform.exam.standard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guoyu.platform.exam.standard.parser.DocxStandardParser;
import com.guoyu.platform.exam.standard.parser.model.ParsedStandard;
import com.guoyu.platform.exam.standard.service.NodeKeyGenerator;
import com.guoyu.platform.exam.standard.service.StandardL2WeightInitializer;
import com.guoyu.platform.exam.standard.service.StandardTreeBuilder;
import com.guoyu.platform.exam.standard.service.StandardTreeDraft;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * B 组（规则引擎）：B1 编码序号器 / B2 理论权重分配 / B3 技能比重（方案 §13.1）。
 */
class StandardTreeBuilderTest {

  private static ParsedStandard kuangshan;

  @BeforeAll
  static void parse() throws Exception {
    try (InputStream in = StandardTreeBuilderTest.class
        .getResourceAsStream("/standard/矿山救护工.docx")) {
      kuangshan = new DocxStandardParser().parse(in);
    }
  }

  /** B1：编码序号器 */
  @Test
  void b1_nodeKeyGenerator() {
    assertEquals("A", NodeKeyGenerator.letters(0));
    assertEquals("B", NodeKeyGenerator.letters(1));
    assertEquals("Z", NodeKeyGenerator.letters(25));
    // 超 26 进位 AA/AB（选其一并文档化：本实现采用进位制）
    assertEquals("AA", NodeKeyGenerator.letters(26));
    assertEquals("AB", NodeKeyGenerator.letters(27));
    assertEquals("BA", NodeKeyGenerator.letters(52));

    assertEquals("A-A", NodeKeyGenerator.childKey("A", 0));
    assertEquals("A-A-B", NodeKeyGenerator.childKey("A-A", 1));
    assertEquals("A-A-A-001", NodeKeyGenerator.leafKey("A-A-A", 0));
    assertEquals("B-C-B-014", NodeKeyGenerator.leafKey("B-C-B", 13));
    assertEquals("职业道德A-A", NodeKeyGenerator.titleWithKey("职业道德", "A-A"));
  }

  /** B2：理论权重分配（样例对拍：基本要求 0.25 → 职业道德 0.05 / 基础知识 0.20） */
  @Test
  void b2_theoryWeightAllocation() {
    List<StandardTreeDraft> drafts = new StandardTreeBuilder().build(kuangshan);
    // 5 等级 × 1 方向
    StandardTreeDraft a = find(drafts, 0, "默认", "A");
    assertEquals(0, a.weight().compareTo(new BigDecimal("0.2500")), "基本要求权重 0.25");
    StandardTreeDraft aa = find(drafts, 0, "默认", "A-A");
    assertEquals("职业道德A-A", aa.title());
    assertEquals(0, aa.weight().compareTo(new BigDecimal("0.0500")), "职业道德 0.05");
    StandardTreeDraft ab = find(drafts, 0, "默认", "A-B");
    assertEquals("基础知识A-B", ab.title());
    assertEquals(0, ab.weight().compareTo(new BigDecimal("0.2000")), "基础知识 0.20");

    StandardTreeDraft b = find(drafts, 0, "默认", "B");
    assertEquals(0, b.weight().compareTo(new BigDecimal("0.7500")), "工作要求 0.75");
    StandardTreeDraft ba = find(drafts, 0, "默认", "B-A");
    assertEquals("事故预防B-A", ba.title());
    assertEquals(0, ba.weight().compareTo(new BigDecimal("0.2000")), "事故预防 0.20");
    assertEquals(0, find(drafts, 0, "默认", "B-B").weight().compareTo(new BigDecimal("0.3000")));
    assertEquals(0, find(drafts, 0, "默认", "B-C").weight().compareTo(new BigDecimal("0.2500")));

    // 编码序列与样例一致：A-A-A 职业道德基本知识、A-A-B 职业守则
    assertEquals("职业道德基本知识A-A-A", find(drafts, 0, "默认", "A-A-A").title());
    assertEquals("职业守则A-A-B", find(drafts, 0, "默认", "A-A-B").title());
    // 2.1.1 职业道德基本知识：源标准空小节，解析期已按固定口径补齐相关知识，不再标记 needLlm
    assertFalse(find(drafts, 0, "默认", "A-A-A").needLlm());
    assertEquals("1.工匠精神培养\n2.安全意识培养\n3.职业道德素养\n4.团队协作精神",
        find(drafts, 0, "默认", "A-A-A").relatedKnowledge());

    // 技师（level 3）：培训指导 0.10 出现在 B 支
    StandardTreeDraft bd = find(drafts, 3, "默认", "B-D");
    assertEquals("培训指导B-D", bd.title());
    assertEquals(0, bd.weight().compareTo(new BigDecimal("0.1000")), "技师培训指导 0.10");
    // 初级 B 支无培训指导（权重 — → 不在树中……初级工作要求表本就无该职业功能）
    assertTrue(drafts.stream().noneMatch(d -> d.levelCode() == 0 && d.title().contains("培训指导")));

    // L2 权重解析期留空（导出时按叶数比例计算）
    assertNull(find(drafts, 0, "默认", "A-A-A").weight());
  }

  /** B3：技能比重（一级=技能权重表值 0.15/0.15/0.70） */
  @Test
  void b3_skillWeights() {
    assertEquals(0, StandardTreeBuilder.skillWeightOf(kuangshan, "事故预防", 0, "默认")
        .compareTo(new BigDecimal("0.1500")));
    assertEquals(0, StandardTreeBuilder.skillWeightOf(kuangshan, "事故救援", 0, "默认")
        .compareTo(new BigDecimal("0.1500")));
    assertEquals(0, StandardTreeBuilder.skillWeightOf(kuangshan, "装备操作", 0, "默认")
        .compareTo(new BigDecimal("0.7000")));
    // 初级培训指导 — → null
    assertNull(StandardTreeBuilder.skillWeightOf(kuangshan, "培训指导", 0, "默认"));
    // 技师培训指导 10 → 0.10
    assertEquals(0, StandardTreeBuilder.skillWeightOf(kuangshan, "培训指导", 3, "默认")
        .compareTo(new BigDecimal("0.1000")));
  }

  /** B5：矿井通风工——职业功能编号无分隔符直写（"1业前准备"），工作要求权重经宽松匹配落位 */
  @Test
  void b5_kuangjingTongfengWorkWeightsMatchedLoosely() throws Exception {
    ParsedStandard std;
    try (InputStream in = StandardTreeBuilderTest.class
        .getResourceAsStream("/standard/矿井通风工.docx")) {
      assertNotNull(in, "测试资源缺失：矿井通风工.docx");
      std = new DocxStandardParser().parse(in);
    }
    List<StandardTreeDraft> drafts = new StandardTreeBuilder().build(std);

    // 五级：业前准备 0.20 / 业中操作 0.30 / 业后整理 0.20，工作要求合计 0.70
    assertEquals(0, find(drafts, 0, "默认", "B-A").weight().compareTo(new BigDecimal("0.2000")),
        "五级业前准备 0.20（宽松匹配权重表「业前准备」行）");
    assertEquals(0, find(drafts, 0, "默认", "B-B").weight().compareTo(new BigDecimal("0.3000")));
    assertEquals(0, find(drafts, 0, "默认", "B-C").weight().compareTo(new BigDecimal("0.2000")));
    assertEquals(0, find(drafts, 0, "默认", "B").weight().compareTo(new BigDecimal("0.7000")),
        "五级工作要求合计 0.70");
    // 五级工作表本无"技术管理和培训"职业功能，树中不应出现
    assertTrue(drafts.stream().noneMatch(
        d -> d.levelCode() == 0 && d.title().contains("技术管理和培训")));

    // 二级/技师：技术管理和培训 0.10 落位；工作要求合计 0.80
    assertEquals(0, find(drafts, 3, "默认", "B-A").weight().compareTo(new BigDecimal("0.2000")));
    assertEquals(0, find(drafts, 3, "默认", "B-B").weight().compareTo(new BigDecimal("0.2500")));
    assertEquals(0, find(drafts, 3, "默认", "B-C").weight().compareTo(new BigDecimal("0.2500")));
    assertEquals(0, find(drafts, 3, "默认", "B-D").weight().compareTo(new BigDecimal("0.1000")),
        "二级技术管理和培训 0.10");
    assertEquals(0, find(drafts, 3, "默认", "B").weight().compareTo(new BigDecimal("0.8000")));
  }

  /** B6：矿井通风工——法律法规小节初始权重钳制 5%，同 L1 的 L2 合计仍等于 L1 权重 */
  @Test
  void b6_lawSectionCappedAtFivePercent() throws Exception {
    ParsedStandard std;
    try (InputStream in = StandardTreeBuilderTest.class
        .getResourceAsStream("/standard/矿井通风工.docx")) {
      assertNotNull(in, "测试资源缺失：矿井通风工.docx");
      std = new DocxStandardParser().parse(in);
    }
    List<StandardTreeDraft> drafts = new StandardTreeBuilder().build(std);
    Map<String, BigDecimal> prefill = StandardL2WeightInitializer.allocate(drafts);

    // 五级：基础知识 L1=0.25，其下法律法规小节（10 部法规条目）预填 ≤0.05，L2 合计仍 0.25
    StandardTreeDraft lawL2 = drafts.stream()
        .filter(d -> d.levelCode() == 0 && "默认".equals(d.direction())
            && "A-B".equals(d.parentNodeKey()) && d.titleRaw() != null
            && d.titleRaw().contains("法律法规"))
        .findFirst()
        .orElseThrow(() -> new AssertionError("缺少法律法规小节"));
    BigDecimal lawWeight = prefill.get("0|默认|" + lawL2.nodeKey());
    assertNotNull(lawWeight, "法律法规小节应有预填权重");
    assertTrue(lawWeight.compareTo(new BigDecimal("0.05")) <= 0,
        "法律法规初始权重应 ≤0.05，实际=" + lawWeight);
    BigDecimal sumL2 = prefill.entrySet().stream()
        .filter(e -> e.getKey().startsWith("0|默认|A-B-"))
        .map(Map.Entry::getValue)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertEquals(0, new BigDecimal("0.25").compareTo(sumL2), "基础知识 L2 合计应等于 L1 权重 0.25");

    // 二级/技师：基础知识 L1=0.15，同样钳制且合计不变
    BigDecimal lawWeight3 = prefill.get("3|默认|" + lawL2.nodeKey());
    assertNotNull(lawWeight3);
    assertTrue(lawWeight3.compareTo(new BigDecimal("0.05")) <= 0,
        "二级法律法规初始权重应 ≤0.05，实际=" + lawWeight3);
    BigDecimal sumL2Level3 = prefill.entrySet().stream()
        .filter(e -> e.getKey().startsWith("3|默认|A-B-"))
        .map(Map.Entry::getValue)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertEquals(0, new BigDecimal("0.15").compareTo(sumL2Level3), "二级基础知识 L2 合计应等于 0.15");
  }

  private StandardTreeDraft find(List<StandardTreeDraft> drafts, int levelCode, String direction,
      String nodeKey) {
    return drafts.stream()
        .filter(d -> d.levelCode() == levelCode && d.direction().equals(direction)
            && d.nodeKey().equals(nodeKey))
        .findFirst()
        .orElseThrow(() -> new AssertionError("缺少节点：" + levelCode + "/" + direction + "/" + nodeKey));
  }

  /** 树完整性：每个非根节点的 parentNodeKey 都存在（同树内） */
  @Test
  void b2_treeIntegrity() {
    List<StandardTreeDraft> drafts = new StandardTreeBuilder().build(kuangshan);
    for (StandardTreeDraft d : drafts) {
      assertNotNull(d.title());
      if (d.parentNodeKey() != null) {
        find(drafts, d.levelCode(), d.direction(), d.parentNodeKey());
      }
    }
  }

  /** B4：1.4 职业技能等级为权威约束——焊工 4 工种只有 16 个有效组合（气焊工无技师/高级技师） */
  @Test
  void b4_directionLevelConstraints() throws Exception {
    try (InputStream in = StandardTreeBuilderTest.class
        .getResourceAsStream("/standard/焊工标准.docx")) {
      assertNotNull(in, "测试资源缺失：焊工标准.docx");
      ParsedStandard han = new DocxStandardParser().parse(in);

      // 1.4 工种等级范围解析
      Map<String, List<Integer>> dl = han.getDirectionLevels();
      assertEquals(List.of(0, 1, 2, 3, 4), dl.get("电焊工"));
      assertEquals(List.of(0, 1, 2), dl.get("气焊工"), "气焊工仅 五/四/三 级");
      assertEquals(List.of(0, 1, 2, 3), dl.get("钎焊工"), "钎焊工到技师");
      assertEquals(List.of(0, 1, 2, 3, 4), dl.get("焊接设备操作工"));

      // 构建后有效组合恰好 16 个（5+3+4+5），无效组合不产生任何节点
      List<StandardTreeDraft> drafts = new StandardTreeBuilder().build(han);
      Set<String> combos = drafts.stream()
          .map(d -> d.levelCode() + "|" + d.direction())
          .collect(Collectors.toSet());
      assertEquals(17, combos.size(), "焊工有效等级×工种组合应为 17（电5+气3+钎4+焊5），实际组合：" + combos);
      assertFalse(combos.contains("3|气焊工"), "气焊工不应有技师树（含 A 支）");
      assertFalse(combos.contains("4|气焊工"), "气焊工不应有高级技师树（含 A 支）");
      assertFalse(combos.contains("4|钎焊工"), "钎焊工不应有高级技师树（含 A 支）");
      assertTrue(combos.contains("0|气焊工"));
      assertTrue(combos.contains("2|气焊工"));
      assertTrue(combos.contains("3|钎焊工"));
      assertTrue(combos.contains("4|电焊工"));
      assertTrue(combos.contains("4|焊接设备操作工"));
    }
  }
}
