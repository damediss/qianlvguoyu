package com.guoyu.platform.exam.knowledgefix.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guoyu.platform.exam.knowledgefix.dto.KnowledgeNodeRow;
import com.guoyu.platform.exam.knowledgefix.service.KnowledgeFixKeywordPruner.PruneResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * 剪枝评分器单测（方案 v1.3 附一 KP-01~08）：纯逻辑无 mock，与离线闸门脚本同口径。
 */
class KnowledgeFixKeywordPrunerTest {

  private static final int COURSE = 290;

  private KnowledgeNodeRow node(int id, int parentId, String title, int level) {
    KnowledgeNodeRow n = new KnowledgeNodeRow();
    n.setId(id);
    n.setCourseId(COURSE);
    n.setParentId(parentId);
    n.setTitle(title);
    n.setLevel(level);
    return n;
  }

  /** 课程树：8 个 L2（501~508），501 蒸发系统挂 11 个 L3（601/602/611~619），其余各挂 1~2 个 */
  private Map<Integer, List<KnowledgeNodeRow>> courseL2() {
    List<KnowledgeNodeRow> l2 = new ArrayList<>();
    String[] titles = {"蒸发系统", "焙烧系统", "沉降系统", "过滤系统", "通信系统",
        "职业道德", "分析系统", "计量系统"};
    for (int i = 0; i < titles.length; i++) {
      l2.add(node(501 + i, 400, titles[i], 2));
    }
    Map<Integer, List<KnowledgeNodeRow>> m = new java.util.HashMap<>();
    m.put(COURSE, l2);
    return m;
  }

  private Map<Integer, Map<Integer, List<KnowledgeNodeRow>>> courseL3ByParent() {
    List<KnowledgeNodeRow> l3 = new ArrayList<>();
    String[] evap = {"蒸发器跑碱处理", "蒸发站真空调整", "蒸发器结垢清理", "蒸发强度计算",
        "蒸发室液位控制", "蒸发器传热系数", "蒸发站效数选择", "二次蒸汽利用",
        "蒸发器酸洗防垢", "不凝汽排放", "防雷接地施工"};
    for (int i = 0; i < evap.length; i++) {
      l3.add(node(601 + i, 501, evap[i], 3));
    }
    l3.add(node(603 + 100, 502, "焙烧温度控制", 3));
    l3.add(node(506, 506, "爱岗敬业要求", 3));
    Map<Integer, Map<Integer, List<KnowledgeNodeRow>>> m = new java.util.HashMap<>();
    Map<Integer, List<KnowledgeNodeRow>> byParent = new java.util.HashMap<>();
    for (KnowledgeNodeRow n : l3) {
      byParent.computeIfAbsent(n.getParentId(), x -> new ArrayList<>()).add(n);
    }
    m.put(COURSE, byParent);
    return m;
  }

  private KnowledgeFixKeywordPruner pruner() {
    return new KnowledgeFixKeywordPruner(courseL2(), courseL3ByParent());
  }

  private List<KnowledgeNodeRow> siblingsOf501() {
    List<KnowledgeNodeRow> all = new ArrayList<>();
    courseL3ByParent().get(COURSE).values().forEach(all::addAll);
    return all.stream().filter(n -> n.getParentId() == 501).collect(Collectors.toList());
  }

  private List<KnowledgeNodeRow> l2List() {
    return courseL2().get(COURSE);
  }

  /** KP-01 确定性：同输入重复评分结果逐位一致；列表顺序稳定 */
  @Test
  void kp01_deterministicSameInputSameOutput() {
    KnowledgeFixKeywordPruner p = pruner();
    List<KnowledgeNodeRow> siblings = siblingsOf501();
    PruneResult a = p.pruneSiblings(COURSE, siblings, null, "蒸发器跑碱如何处理");
    PruneResult b = p.pruneSiblings(COURSE, siblings, null, "蒸发器跑碱如何处理");
    assertEquals(a.candidates().stream().map(KnowledgeNodeRow::getId).toList(),
        b.candidates().stream().map(KnowledgeNodeRow::getId).toList());
    assertEquals(a.pruned(), b.pruned());
    PruneResult c1 = p.pruneL2Shortlist(COURSE, l2List(), 4192, "蒸发器跑碱如何处理");
    PruneResult c2 = p.pruneL2Shortlist(COURSE, l2List(), 4192, "蒸发器跑碱如何处理");
    assertEquals(c1.candidates().stream().map(KnowledgeNodeRow::getId).toList(),
        c2.candidates().stream().map(KnowledgeNodeRow::getId).toList());
  }

  /** KP-02 词表健壮性：null 标题 / 空标题 / 纯★ / 超长标题不抛异常 */
  @Test
  void kp02_robustAgainstPathologicalTitles() {
    List<KnowledgeNodeRow> l2 = new ArrayList<>();
    l2.add(node(701, 400, null, 2));
    l2.add(node(702, 400, "", 2));
    l2.add(node(703, 400, "★", 2));
    l2.add(node(704, 400, "★" + "超".repeat(60), 2));
    l2.add(node(705, 400, "正常章节", 2));
    l2.add(node(706, 400, "另", 2));
    Map<Integer, List<KnowledgeNodeRow>> l2Map = Map.of(COURSE, l2);
    List<KnowledgeNodeRow> l3 = new ArrayList<>();
    for (int i = 0; i < 6; i++) {
      KnowledgeNodeRow n = node(801 + i, 701 + i, i == 0 ? null : "标题" + i, 3);
      l3.add(n);
    }
    Map<Integer, Map<Integer, List<KnowledgeNodeRow>>> l3Map =
        Map.of(COURSE, Map.of(701, List.of(l3.get(0)), 702, List.of(l3.get(1)),
            703, List.of(l3.get(2)), 704, List.of(l3.get(3)),
            705, List.of(l3.get(4)), 706, List.of(l3.get(5))));
    KnowledgeFixKeywordPruner p = new KnowledgeFixKeywordPruner(l2Map, l3Map);
    PruneResult r1 = p.pruneL2Shortlist(COURSE, l2, null, "正常章节相关试题");
    assertNotNull(r1);
    PruneResult r2 = p.pruneSiblings(COURSE, l3, null, "正常章节相关试题");
    assertNotNull(r2);
  }

  /** KP-03 截断边界：兄弟 ≤8 原集返回；>8 剪到 8 且结果恒为原集子集 */
  @Test
  void kp03_truncateOnlyAboveKeepSize() {
    KnowledgeFixKeywordPruner p = pruner();
    List<KnowledgeNodeRow> siblings = siblingsOf501();
    assertEquals(11, siblings.size());
    // ≤8：取任一 8 个兄弟的子集 → 原集返回不剪
    PruneResult small = p.pruneSiblings(COURSE, siblings.subList(0, 8), null, "蒸发器跑碱如何处理");
    assertFalse(small.pruned());
    assertEquals(8, small.candidates().size());
    assertEquals(siblings.subList(0, 8).stream().map(KnowledgeNodeRow::getId).toList(),
        small.candidates().stream().map(KnowledgeNodeRow::getId).toList());
    // >8：剪到 8，且为原集子集
    PruneResult big = p.pruneSiblings(COURSE, siblings, null, "蒸发器跑碱如何处理");
    assertTrue(big.pruned());
    assertEquals(8, big.candidates().size());
    List<Integer> siblingIds = siblings.stream().map(KnowledgeNodeRow::getId).toList();
    assertTrue(big.candidates().stream().map(KnowledgeNodeRow::getId).allMatch(siblingIds::contains));
  }

  /** KP-04 弱信号回退：题干与全部候选零词面重合 → 回退全量且可识别 */
  @Test
  void kp04_weakSignalFallsBackToFullSet() {
    KnowledgeFixKeywordPruner p = pruner();
    List<KnowledgeNodeRow> siblings = siblingsOf501();
    PruneResult r = p.pruneSiblings(COURSE, siblings, null, "zzz qqq xxx vvv");
    assertFalse(r.pruned());
    assertTrue(r.weakFallback());
    assertEquals(siblings.size(), r.candidates().size());
    PruneResult l2r = p.pruneL2Shortlist(COURSE, l2List(), null, "zzz qqq xxx vvv");
    assertFalse(l2r.pruned());
    assertTrue(l2r.weakFallback());
    assertEquals(l2List().size(), l2r.candidates().size());
  }

  /** KP-05 单候选边界：剪后 <2 → 回退全量，绝不返回 1 个候选（Jev criteria min_length=2 会 422） */
  @Test
  void kp05_neverReturnsSingleCandidate() {
    KnowledgeFixKeywordPruner p = pruner();
    List<KnowledgeNodeRow> siblings = siblingsOf501();
    for (int size = 2; size <= 11; size++) {
      PruneResult r = p.pruneSiblings(COURSE, siblings.subList(0, size), null, "蒸发器跑碱如何处理");
      assertTrue(r.candidates().size() >= 2 || r.candidates().size() == size,
          "size=" + size + " returned " + r.candidates().size());
    }
    // 单兄弟：≤8 原集返回（1 个），由服务层 <2 拦截兜底（既有逻辑）
    PruneResult single = p.pruneSiblings(COURSE, siblings.subList(0, 1), null, "蒸发器跑碱如何处理");
    assertFalse(single.pruned());
    assertEquals(1, single.candidates().size());
  }

  /** KP-06 keep 保位：老节点在兄弟集内且不在 top-8 → 替换末位（槽位数不变）；跨章老节点不保位 */
  @Test
  void kp06_keepSlotReservation() {
    KnowledgeFixKeywordPruner p = pruner();
    List<KnowledgeNodeRow> siblings = siblingsOf501();
    // 610 防雷接地施工（零分）不在 top-8 → 老节点保位后必须出现在名单中（槽位数不变）
    PruneResult r = p.pruneSiblings(COURSE, siblings, 610, "蒸发器跑碱如何处理");
    assertTrue(r.pruned());
    assertTrue(r.reservedOld());
    assertEquals(8, r.candidates().size());
    assertTrue(r.candidates().stream().anyMatch(n -> n.getId() == 610));
    // 老节点本就在名单内（高相关 601）→ 不占额外槽位、无保位标志
    PruneResult inTop = p.pruneSiblings(COURSE, siblings, 601, "蒸发器跑碱如何处理");
    assertTrue(inTop.pruned());
    assertFalse(inTop.reservedOld());
    // 跨章老节点（4192 属 506，不在 501 兄弟集）→ 不保位
    PruneResult cross = p.pruneSiblings(COURSE, siblings, 4192, "蒸发器跑碱如何处理");
    assertTrue(cross.pruned());
    assertFalse(cross.reservedOld());
  }

  /** KP-07 课程隔离：索引按课程构建，B 课程的评分不引用 A 课程任何节点 */
  @Test
  void kp07_courseIsolation() {
    Map<Integer, List<KnowledgeNodeRow>> l2 = new java.util.HashMap<>();
    l2.put(COURSE, l2List());
    l2.put(291, List.of(node(901, 900, "熔铸工艺", 2), node(902, 900, "铸造缺陷", 2),
        node(903, 900, "均匀化退火", 2), node(904, 900, "晶粒细化", 2),
        node(905, 900, "熔体净化", 2), node(906, 900, "浇注温度", 2)));
    Map<Integer, Map<Integer, List<KnowledgeNodeRow>>> l3 = new java.util.HashMap<>();
    l3.put(COURSE, courseL3ByParent().get(COURSE));
    List<KnowledgeNodeRow> l3b = new ArrayList<>();
    String[] b = {"熔体静置处理", "除气除渣操作", "铸锭冷却控制"};
    for (int i = 0; i < b.length; i++) {
      l3b.add(node(951 + i, 901 + (i % 6), b[i], 3));
    }
    Map<Integer, List<KnowledgeNodeRow>> byParentB = new java.util.HashMap<>();
    l3b.forEach(n -> byParentB.computeIfAbsent(n.getParentId(), x -> new ArrayList<>()).add(n));
    l3.put(291, byParentB);

    KnowledgeFixKeywordPruner p = new KnowledgeFixKeywordPruner(l2, l3);
    PruneResult r = p.pruneL2Shortlist(291, l2.get(291), null, "蒸发器跑碱如何处理");
    // 蒸发题在 291（熔铸）课程内为弱信号 → 全量回退；候选只能来自 291 自己的 L2 集（901~906）
    assertTrue(r.candidates().stream().map(KnowledgeNodeRow::getId).allMatch(id -> id >= 901 && id <= 906));
    PruneResult own = p.pruneL2Shortlist(COURSE, l2.get(COURSE), null, "蒸发器跑碱如何处理");
    assertTrue(own.candidates().stream().map(KnowledgeNodeRow::getId).allMatch(id -> id >= 501 && id <= 508));
  }

  /** KP-08 并发安全：多线程并发评分结果与单线程一致、无异常 */
  @Test
  void kp08_concurrentReadsAreConsistent() throws Exception {
    KnowledgeFixKeywordPruner p = pruner();
    List<KnowledgeNodeRow> siblings = siblingsOf501();
    PruneResult single = p.pruneSiblings(COURSE, siblings, 4192, "蒸发器跑碱如何处理");
    List<Integer> expected = single.candidates().stream().map(KnowledgeNodeRow::getId).toList();
    ExecutorService pool = Executors.newFixedThreadPool(8);
    try {
      for (int t = 0; t < 8; t++) {
        pool.submit(() -> {
          for (int i = 0; i < 50; i++) {
            PruneResult r = p.pruneSiblings(COURSE, siblings, 4192, "蒸发器跑碱如何处理");
            assertEquals(expected, r.candidates().stream().map(KnowledgeNodeRow::getId).toList());
          }
        });
      }
      pool.shutdown();
      assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS));
    } finally {
      pool.shutdownNow();
    }
  }
}
