package com.guoyu.platform.exam.standard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.exam.standard.entity.ExamStandardNodeEntity;
import com.guoyu.platform.exam.standard.service.StandardLlmPoint;
import com.guoyu.platform.exam.standard.service.StandardLlmSummarizeService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * B5/B6：LLM 输出确定性护栏（方案 §6.2/§6.3、§13.1）。
 */
class StandardLlmGuardrailTest {

  /** B5a：非法 JSON 抛异常（调用方重试，最终失败走原文兜底） */
  @Test
  void b5_invalidJsonThrows() {
    assertThrows(IllegalArgumentException.class,
        () -> StandardLlmSummarizeService.parsePointsResponse("这不是JSON", 15, false));
    assertThrows(IllegalArgumentException.class,
        () -> StandardLlmSummarizeService.parsePointsResponse("", 15, false));
    assertThrows(IllegalArgumentException.class,
        () -> StandardLlmSummarizeService.parsePointsResponse("{\"wrong\":[]}", 15, false));
  }

  /** B5b：markdown 围栏兼容 */
  @Test
  void b5_markdownFenceStripped() {
    String content = "```json\n{\"points\":[{\"title\":\"工匠精神的基本内涵\",\"important\":\"X\"}]}\n```";
    StandardLlmPoint.ParseResult result =
        StandardLlmSummarizeService.parsePointsResponse(content, 15, false);
    assertEquals(1, result.getPoints().size());
    assertEquals("工匠精神的基本内涵", result.getPoints().get(0).title());
  }

  /** B5c：非法 important 默认 X + 警告 */
  @Test
  void b5_invalidImportantDefaultsX() {
    String content = "{\"points\":[{\"title\":\"知识点甲\",\"important\":\"核心\"},"
        + "{\"title\":\"知识点乙\",\"important\":\"y\"}]}";
    StandardLlmPoint.ParseResult result =
        StandardLlmSummarizeService.parsePointsResponse(content, 15, false);
    assertEquals(2, result.getPoints().size());
    assertEquals("X", result.getPoints().get(0).important(), "非法值默认 X");
    assertEquals("Y", result.getPoints().get(1).important(), "小写归一为大写");
    assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("非法 important")));
  }

  /** B5d：理论通道 >15 截断 + 警告 */
  @Test
  void b5_theoryTruncateAt15() {
    StringBuilder sb = new StringBuilder("{\"points\":[");
    for (int i = 0; i < 20; i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append("{\"title\":\"知识点").append(i).append("\",\"important\":\"X\"}");
    }
    sb.append("]}");
    StandardLlmPoint.ParseResult result =
        StandardLlmSummarizeService.parsePointsResponse(sb.toString(), 15, false);
    assertEquals(15, result.getPoints().size());
    assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("截断")));
  }

  /** B5e：技能通道上限 = 原文条数 × 2 */
  @Test
  void b5_skillTruncateByFactor() {
    StringBuilder sb = new StringBuilder("{\"points\":[");
    for (int i = 0; i < 9; i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append("{\"title\":\"操作").append(i).append("\",\"important\":\"X\"}");
    }
    sb.append("]}");
    // 原文 3 条 → 上限 6
    StandardLlmPoint.ParseResult result =
        StandardLlmSummarizeService.parsePointsResponse(sb.toString(), 6, true);
    assertEquals(6, result.getPoints().size());
    assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("截断")));
  }

  /** B6：技能考评点标题以"能"开头 → 警告（模型应去"能"字句式） */
  @Test
  void b6_skillTitleStartsWithNengWarns() {
    String content = "{\"points\":[{\"title\":\"能查出风门损坏\",\"important\":\"X\"},"
        + "{\"title\":\"检查风筒的损坏状况\",\"important\":\"Y\"}]}";
    StandardLlmPoint.ParseResult result =
        StandardLlmSummarizeService.parsePointsResponse(content, 10, true);
    assertEquals(2, result.getPoints().size(), "不丢条目");
    assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("能")),
        "应以警告提示'能'字开头");
    // 理论通道不报该警告
    StandardLlmPoint.ParseResult theory =
        StandardLlmSummarizeService.parsePointsResponse(content, 10, false);
    assertTrue(theory.getWarnings().isEmpty());
  }

  /** B2：max_tokens 按通道自适应（理论按目标点数，技能按素材条数） */
  @Test
  void b2_adaptiveMaxTokens() {
    // 理论：目标点数 × 60 + 256，下限 1024、封顶 4096
    assertEquals(1024, StandardLlmSummarizeService.computeTheoryTokens(1));
    assertEquals(1024, StandardLlmSummarizeService.computeTheoryTokens(6));
    assertEquals(1216, StandardLlmSummarizeService.computeTheoryTokens(16), "16 点 → 1216");
    assertEquals(2656, StandardLlmSummarizeService.computeTheoryTokens(40), "40 点 → 2656");
    // 技能：素材条数 × 40 + 256，封顶 2048
    assertEquals(296, StandardLlmSummarizeService.computeMaxTokens(List.of("a"), true));
    assertEquals(416, StandardLlmSummarizeService.computeMaxTokens(List.of("a", "b", "c", "d"), true));
    assertEquals(2048, StandardLlmSummarizeService.computeMaxTokens(
        List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
            "11", "12", "13", "14", "15", "16", "17", "18", "19", "20",
            "21", "22", "23", "24", "25", "26", "27", "28", "29", "30",
            "31", "32", "33", "34", "35", "36", "37", "38", "39", "40",
            "41", "42", "43", "44", "45"), true), "45 条素材封顶 2048");
  }

  /** 弹窗参数：importants 规范化（null=全档；去重排序；空集/非法 → 400） */
  @Test
  void summarize_normalizeImportants() {
    assertEquals(List.of("X", "Y", "Z"), StandardLlmSummarizeService.normalizeImportants(null));
    assertEquals(List.of("X", "Y"),
        StandardLlmSummarizeService.normalizeImportants(List.of("y", "X", "x")));
    assertThrows(BusinessException.class,
        () -> StandardLlmSummarizeService.normalizeImportants(List.of()));
    assertThrows(BusinessException.class,
        () -> StandardLlmSummarizeService.normalizeImportants(List.of("W")));
  }

  /** 理论通道数量分配：全表 200 点按小节权重分配（拍板 2026-09，优先级高于材料条数规则） */
  @Test
  void summarize_allocateTheoryTargets() {
    // L1a 权重 0.5：两个小节材料 3:1 → 100 点按 75/25 分；L1b 权重 0.5：一个小节 → 100
    ExamStandardNodeEntity l1a = l1(1L, new java.math.BigDecimal("0.50"));
    ExamStandardNodeEntity l1b = l1(2L, new java.math.BigDecimal("0.50"));
    ExamStandardNodeEntity s1 = l2(11L, 1L, null);
    ExamStandardNodeEntity s2 = l2(12L, 1L, null);
    ExamStandardNodeEntity s3 = l2(13L, 2L, null);
    Map<Long, Integer> targets = StandardLlmSummarizeService.allocateTheoryTargets(
        List.of(l1a, l1b), List.of(s1, s2, s3),
        s -> s.getId() == 11L ? 3 : 1, 200);
    assertEquals(75, targets.get(11L), "L1a 材料 3/4 → 75 点");
    assertEquals(25, targets.get(12L), "L1a 材料 1/4 → 25 点");
    assertEquals(100, targets.get(13L), "L1b 独生子节 → 100 点");
    assertEquals(200, targets.values().stream().mapToInt(Integer::intValue).sum(), "合计严格 200");
  }

  /** 手工小节权重优先于材料占比；超出 L1 预算时挤占其他小节 */
  @Test
  void summarize_allocateManualWeightFirst() {
    ExamStandardNodeEntity l1a = l1(1L, new java.math.BigDecimal("0.10"));
    ExamStandardNodeEntity manual = l2(11L, 1L, new java.math.BigDecimal("0.05")); // 手工 5% → 10 点
    ExamStandardNodeEntity auto = l2(12L, 1L, null);                               // 剩余 10 点
    Map<Long, Integer> targets = StandardLlmSummarizeService.allocateTheoryTargets(
        List.of(l1a), List.of(manual, auto), s -> 5, 200);
    assertEquals(10, targets.get(11L), "手工权重 0.05 × 200 = 10 点");
    assertEquals(10, targets.get(12L), "L1 预算 20 点 − 手工 10 点 = 10 点");
  }

  /** L1 无权重时按材料条数在全表剩余预算内兜底；权重极小小节保底 1 点 */
  @Test
  void summarize_allocateFallbackAndFloor() {
    ExamStandardNodeEntity weighted = l1(1L, new java.math.BigDecimal("0.80")); // 消费 160 点
    ExamStandardNodeEntity noWeight = l1(2L, null);                              // 兜底池 40 点
    ExamStandardNodeEntity s1 = l2(11L, 1L, null);
    ExamStandardNodeEntity s2 = l2(12L, 2L, null); // 材料 3
    ExamStandardNodeEntity s3 = l2(13L, 2L, null); // 材料 1
    Map<Long, Integer> targets = StandardLlmSummarizeService.allocateTheoryTargets(
        List.of(weighted, noWeight), List.of(s1, s2, s3),
        s -> s.getId() == 12L ? 3 : 1, 200);
    assertEquals(160, targets.get(11L), "L1 权重 0.8 × 200 = 160 点");
    assertEquals(30, targets.get(12L), "兜底池 40 × 3/4 = 30 点");
    assertEquals(10, targets.get(13L), "兜底池 40 × 1/4 = 10 点");
    assertEquals(200, targets.values().stream().mapToInt(Integer::intValue).sum(), "合计严格 200");

    // 保底：L1 权重 0.001 → 0.2 点 → 取整 0 → 保底 1
    ExamStandardNodeEntity tiny = l1(3L, new java.math.BigDecimal("0.001"));
    ExamStandardNodeEntity tinyLeaf = l2(14L, 3L, null);
    Map<Long, Integer> floored = StandardLlmSummarizeService.allocateTheoryTargets(
        List.of(tiny), List.of(tinyLeaf), s -> 1, 200);
    assertEquals(1, floored.get(14L), "权重极小小节保底 1 点");
  }

  /** 国标数量下限：二/一级（levelCode 3/4）按 120 点分配 */
  @Test
  void summarize_allocateSeniorLevel120() {
    ExamStandardNodeEntity l1a = l1(1L, new java.math.BigDecimal("0.50"));
    ExamStandardNodeEntity l1b = l1(2L, new java.math.BigDecimal("0.50"));
    ExamStandardNodeEntity s1 = l2(11L, 1L, null);
    ExamStandardNodeEntity s2 = l2(12L, 2L, null);
    Map<Long, Integer> targets = StandardLlmSummarizeService.allocateTheoryTargets(
        List.of(l1a, l1b), List.of(s1, s2), s -> 1, 120);
    assertEquals(60, targets.get(11L));
    assertEquals(60, targets.get(12L));
    assertEquals(120, targets.values().stream().mapToInt(Integer::intValue).sum());
    // 等级映射：五/四/三级 200，二/一级 120
    assertEquals(200, StandardLlmSummarizeService.theoryTotalTarget(0));
    assertEquals(200, StandardLlmSummarizeService.theoryTotalTarget(2));
    assertEquals(120, StandardLlmSummarizeService.theoryTotalTarget(3));
    assertEquals(120, StandardLlmSummarizeService.theoryTotalTarget(4));
  }

  /** 同小节相同/相近考评点自动合并：规范化后相等或互为子串，保留首个 */
  @Test
  void summarize_dedupSectionPoints() {
    List<StandardLlmPoint> points = List.of(
        new StandardLlmPoint("磨矿浓度计算方法", "X"),
        new StandardLlmPoint("磨矿浓度 计算方法", "Y"),  // 仅差空格 → 规范化相等合并
        new StandardLlmPoint("矿浆pH值测定", "Z"),      // 独立保留
        new StandardLlmPoint("矿浆pH值测定方法", "X")); // 包含第 3 条 → 相近合并
    List<StandardLlmPoint> out = StandardLlmSummarizeService.dedupSectionPoints(points);
    assertEquals(2, out.size());
    assertEquals("磨矿浓度计算方法", out.get(0).title());
    assertEquals("矿浆pH值测定", out.get(1).title());
  }

  /** 数量兜底补点过滤：与已有考评点相同/相近的丢弃，其余保留 */
  @Test
  void summarize_filterTopupAgainstExisting() {
    List<StandardLlmPoint> points = List.of(
        new StandardLlmPoint("磨矿浓度计算", "X"),      // 被已有「磨矿浓度计算方法」包含 → 丢弃
        new StandardLlmPoint("磨机衬板更换周期", "Y"));  // 独立 → 保留
    List<StandardLlmPoint> out = StandardLlmSummarizeService.filterAgainstExisting(points,
        List.of("磨矿浓度计算方法", "矿浆pH值测定"));
    assertEquals(1, out.size());
    assertEquals("磨机衬板更换周期", out.get(0).title());
  }

  private static ExamStandardNodeEntity l1(Long id, java.math.BigDecimal weight) {
    ExamStandardNodeEntity n = new ExamStandardNodeEntity();
    n.setId(id);
    n.setNodeLevel(1);
    n.setWeight(weight);
    return n;
  }

  private static ExamStandardNodeEntity l2(Long id, Long parentId, java.math.BigDecimal weight) {
    ExamStandardNodeEntity n = new ExamStandardNodeEntity();
    n.setId(id);
    n.setParentId(parentId);
    n.setNodeLevel(2);
    n.setWeight(weight);
    return n;
  }

  /** 弹窗参数：重要程度规则文案——子集限定取值，全档保留三档说明 */
  @Test
  void summarize_theoryImportantRule() {
    String subset = StandardLlmSummarizeService.theoryImportantRule(List.of("X", "Y"));
    assertTrue(subset.contains("仅允许取值 X、Y"), subset);
    assertTrue(subset.contains("X=核心必考") && !subset.contains("Z=一般了解"), subset);
    String full = StandardLlmSummarizeService.theoryImportantRule(List.of("X", "Y", "Z"));
    assertTrue(full.contains("Z=一般了解") && !full.contains("仅允许"), full);
  }

  /** 弹窗参数：越档产出归入最低允许档，不丢考评点 */
  @Test
  void summarize_clampImportants() {
    List<StandardLlmPoint> points = List.of(
        new StandardLlmPoint("甲", "X"), new StandardLlmPoint("乙", "Z"),
        new StandardLlmPoint("丙", "Y"));
    List<StandardLlmPoint> clamped =
        StandardLlmSummarizeService.clampImportants(points, List.of("X", "Y"));
    assertEquals(3, clamped.size());
    assertEquals("X", clamped.get(0).important());
    assertEquals("Y", clamped.get(1).important(), "Z 越档归入最低允许档 Y");
    assertEquals("Y", clamped.get(2).important());
    // 仅选 X 时全部归 X
    List<StandardLlmPoint> onlyX =
        StandardLlmSummarizeService.clampImportants(points, List.of("X"));
    assertTrue(onlyX.stream().allMatch(p -> "X".equals(p.important())));
  }

  // ───────── 技能表 X 占比护栏（2026-09-16 拍板：严格按 X 数/总数 ≥ 85%，允许全部归 X） ─────────

  private static ExamStandardNodeEntity skillLeaf(long id, String important, String source,
      String reviewStatus) {
    ExamStandardNodeEntity n = new ExamStandardNodeEntity();
    n.setId(id);
    n.setImportant(important);
    n.setSource(source);
    n.setReviewStatus(reviewStatus);
    return n;
  }

  /** G1：X 占比已达标（17/20=85%）→ 不翻 */
  @Test
  void g1_skillXRatioAlreadyCompliant() {
    List<ExamStandardNodeEntity> leaves = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      leaves.add(skillLeaf(i, i < 17 ? "X" : "Y", "llm", "pending"));
    }
    assertTrue(StandardLlmSummarizeService.pickSkillFlipToX(leaves).isEmpty());
  }

  /** G2：占比不足 → 从树序末尾往前翻，跳过 X 叶 */
  @Test
  void g2_skillXRatioFlipsFromTreeEnd() {
    List<ExamStandardNodeEntity> leaves = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      boolean y = i == 5 || i == 10 || i == 15 || i == 16 || i == 17 || i == 18;
      leaves.add(skillLeaf(i, y ? "Y" : "X", "llm", "pending"));
    }
    // needX=17，xCount=14，缺 3 → 末尾往前：18、17、16（19 是 X 跳过）
    List<ExamStandardNodeEntity> flip = StandardLlmSummarizeService.pickSkillFlipToX(leaves);
    assertEquals(3, flip.size());
    assertEquals(18L, flip.get(0).getId());
    assertEquals(17L, flip.get(1).getId());
    assertEquals(16L, flip.get(2).getId());
  }

  /** G3：小叶表可全部归 X（⌈4×0.85⌉=4） */
  @Test
  void g3_skillXRatioMayFlipAllToX() {
    List<ExamStandardNodeEntity> leaves = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      leaves.add(skillLeaf(i, "Y", "llm", "pending"));
    }
    assertEquals(4, StandardLlmSummarizeService.pickSkillFlipToX(leaves).size());
  }

  /** G4：人工/已审叶不翻——翻不够就只返回可翻的（缺口由调用方告警） */
  @Test
  void g4_skillXRatioSkipsManualAndReviewed() {
    List<ExamStandardNodeEntity> leaves = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      leaves.add(skillLeaf(i, i < 15 ? "X" : "Y",
          (i == 17) ? "manual" : "llm",
          (i >= 16) ? "approved" : "pending"));
    }
    // needX=17，xCount=15，缺 2；可翻的 pending 非 X 只有 15 号叶（16-18 已审/人工，19 已审）
    List<ExamStandardNodeEntity> flip = StandardLlmSummarizeService.pickSkillFlipToX(leaves);
    assertEquals(1, flip.size());
    assertEquals(15L, flip.get(0).getId());
  }
}
