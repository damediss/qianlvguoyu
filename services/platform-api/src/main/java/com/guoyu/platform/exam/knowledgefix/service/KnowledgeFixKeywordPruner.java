package com.guoyu.platform.exam.knowledgefix.service;

import com.guoyu.platform.exam.knowledgefix.dto.KnowledgeNodeRow;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 知识归属修正·关键词剪枝器（方案 v1.3 §7.2 附一/附四，§13 步骤 11）。
 *
 * <p>离线闸门实证（附四）：stage1 L2 证据聚合 top5 召回 77.2%（228 课 71.8% ≥ 60% 闸门线）、
 * stage2 兄弟内 top5 召回 93.6% / top8 96.7%——候选集收窄使 Jev maxP 下限从 1/N 机械抬升，
 * 是 JEV 复活的必要条件（非充分，通过率仍须小批次实测）。
 *
 * <p>评分口径与离线脚本一致（零手工规则）：节点标题切短语词（分隔符切分、2~14 字）+ 字符 bigram，
 * 课程内 IDF 加权；短语命中以 bigram 预过滤再确认；短语权重 idf×min(len/4,2)、bigram 权重 idf×0.25。
 * 同分 tie-break 固定为 node_id 升序（KP-01 确定性）。
 *
 * <p>线程模型（KP-08）：索引按课程在批次启动时构建一次，内部结构构建后只读
 * （不可变 Map + 构造完成安全发布），Worker 并发调用 {@link #pruneSiblings} / {@link #pruneL2Shortlist} 安全。
 *
 * <p>边界语义：
 * <ul>
 *   <li>KP-03：候选集 ≤K 时原集返回（不剪）；</li>
 *   <li>KP-04：题干与全部候选标题零词面重合（全零分）→ 弱信号回退全量，可识别；</li>
 *   <li>KP-05/21：剪后候选 &lt;2 → 回退全量（Jev criteria min_length=2 会 422，不发请求）；</li>
 *   <li>KP-06/20：keep 保位——老节点（stage2）或老 L2（stage1）不在名单内时强制替换末位，
 *       槽位数不变；keep 语义不被剪枝消灭（漏挂无害、错挂是污染，保位防结构性偏 apply）。</li>
 * </ul>
 */
public class KnowledgeFixKeywordPruner {

  /** stage2 兄弟集保留数（附四：top5 召回 93.6% / top8 96.7%，K=8 兼顾召回与 maxP 抬升） */
  public static final int STAGE2_KEEP = 8;
  /** stage1 L2 短名单保留数（附四：整体 top5 召回 77.2%） */
  public static final int STAGE1_KEEP = 5;

  /** 剪枝结果：candidates=剪后候选（顺序即 criteria 顺序）；pruned=false 表示原集返回；weakFallback=弱信号回退；reservedOld=keep 保位生效 */
  public record PruneResult(List<KnowledgeNodeRow> candidates, boolean pruned,
      boolean weakFallback, boolean reservedOld) {
  }

  private final Map<Integer, CourseIndex> courseIndexes;

  public KnowledgeFixKeywordPruner(Map<Integer, List<KnowledgeNodeRow>> courseL2,
      Map<Integer, Map<Integer, List<KnowledgeNodeRow>>> courseL3ByParent) {
    Map<Integer, CourseIndex> idx = new HashMap<>();
    for (Map.Entry<Integer, List<KnowledgeNodeRow>> e : courseL2.entrySet()) {
      List<KnowledgeNodeRow> l3 = new ArrayList<>();
      Map<Integer, List<KnowledgeNodeRow>> byParent = courseL3ByParent.getOrDefault(e.getKey(), Map.of());
      for (List<KnowledgeNodeRow> children : byParent.values()) {
        l3.addAll(children);
      }
      idx.put(e.getKey(), new CourseIndex(e.getValue(), l3));
    }
    // 只有 L3 没有 L2 的异常课程也建索引（stage2 兜底可用）
    for (Map.Entry<Integer, Map<Integer, List<KnowledgeNodeRow>>> e : courseL3ByParent.entrySet()) {
      if (!idx.containsKey(e.getKey())) {
        List<KnowledgeNodeRow> l3 = new ArrayList<>();
        for (List<KnowledgeNodeRow> children : e.getValue().values()) {
          l3.addAll(children);
        }
        idx.put(e.getKey(), new CourseIndex(List.of(), l3));
      }
    }
    this.courseIndexes = Map.copyOf(idx);
  }

  /**
   * stage2 剪枝：所选 L2 的兄弟鉴定点按关键词分取 top-{@value STAGE2_KEEP}（KP-03/04/05/06）。
   *
   * @param siblings       所选 L2 下的全部存活 L3（调用前已有非空校验）
   * @param oldKnowledgeId 当老节点在本兄弟集内且不在 top-K 时强制保留末位（keep 语义保位）
   */
  public PruneResult pruneSiblings(Integer courseId, List<KnowledgeNodeRow> siblings,
      Integer oldKnowledgeId, String questionText) {
    if (siblings == null || siblings.size() <= STAGE2_KEEP) {
      return new PruneResult(siblings, false, false, false);
    }
    CourseIndex idx = courseIndexes.get(courseId);
    if (idx == null) {
      return new PruneResult(siblings, false, false, false);
    }
    Map<Integer, Double> scores = idx.scoreL3(questionText);
    if (allZero(scores)) {
      return new PruneResult(siblings, false, true, false);
    }
    List<KnowledgeNodeRow> top = topK(siblings, scores, STAGE2_KEEP);
    boolean reserved = reserveOld(top, siblings, oldKnowledgeId, STAGE2_KEEP);
    if (top.size() < 2) {
      return new PruneResult(siblings, false, false, false);
    }
    return new PruneResult(top, true, false, reserved);
  }

  /**
   * stage1 宽剪：L2 候选按其子节点最高分（L3 证据聚合）取 top-{@value STAGE1_KEEP}（KP-18~21）。
   * 仅 JEV 通道调用（附一硬约束 1）；LLM 全流程 stage1 恒全量。
   *
   * @param oldKnowledgeId 当老节点所在 L2 不在 top-K 时强制替换末位（候选数不变，KP-20）
   */
  public PruneResult pruneL2Shortlist(Integer courseId, List<KnowledgeNodeRow> l2List,
      Integer oldKnowledgeId, String questionText) {
    if (l2List == null || l2List.size() <= STAGE1_KEEP) {
      return new PruneResult(l2List, false, false, false);
    }
    CourseIndex idx = courseIndexes.get(courseId);
    if (idx == null) {
      return new PruneResult(l2List, false, false, false);
    }
    Map<Integer, Double> l3Scores = idx.scoreL3(questionText);
    Map<Integer, Double> l2Scores = new HashMap<>();
    for (KnowledgeNodeRow l2 : l2List) {
      double max = 0;
      for (KnowledgeNodeRow child : idx.childrenOf(l2.getId())) {
        max = Math.max(max, l3Scores.getOrDefault(child.getId(), 0.0));
      }
      l2Scores.put(l2.getId(), max);
    }
    if (allZero(l2Scores)) {
      return new PruneResult(l2List, false, true, false);
    }
    List<KnowledgeNodeRow> top = topK(l2List, l2Scores, STAGE1_KEEP);
    Integer oldL2Id = resolveOldL2(courseId, oldKnowledgeId);
    boolean reserved = false;
    if (oldL2Id != null) {
      boolean inTop = top.stream().anyMatch(n -> n.getId() != null && n.getId().equals(oldL2Id));
      if (!inTop) {
        KnowledgeNodeRow oldL2 = idx.l2ById(oldL2Id);
        if (oldL2 != null) {
          top.set(top.size() - 1, oldL2);
          reserved = true;
        }
      }
    }
    if (top.size() < 2) {
      return new PruneResult(l2List, false, false, false);
    }
    return new PruneResult(top, true, false, reserved);
  }

  /** 老节点归位到 L2：本身是 L2 → 自身；是 L3 → 其父 L2；不在本课程树 → null（跨章错挂不保位） */
  private Integer resolveOldL2(Integer courseId, Integer oldKnowledgeId) {
    if (oldKnowledgeId == null) {
      return null;
    }
    CourseIndex idx = courseIndexes.get(courseId);
    if (idx == null) {
      return null;
    }
    if (idx.l2ById(oldKnowledgeId) != null) {
      return oldKnowledgeId;
    }
    KnowledgeNodeRow l3 = idx.l3ById(oldKnowledgeId);
    return l3 == null ? null : l3.getParentId();
  }

  /** keep 保位：old 在候选池内且不在名单内 → 替换末位（槽位数不变）；已在名单内不占额外槽位 */
  private boolean reserveOld(List<KnowledgeNodeRow> top, List<KnowledgeNodeRow> pool,
      Integer oldId, int k) {
    if (oldId == null) {
      return false;
    }
    boolean oldInPool = pool.stream().anyMatch(n -> n.getId() != null && n.getId().equals(oldId));
    if (!oldInPool) {
      return false;
    }
    boolean oldInTop = top.stream().anyMatch(n -> n.getId() != null && n.getId().equals(oldId));
    if (oldInTop) {
      return false;
    }
    KnowledgeNodeRow oldNode = pool.stream()
        .filter(n -> n.getId() != null && n.getId().equals(oldId)).findFirst().orElse(null);
    if (oldNode == null) {
      return false;
    }
    top.set(Math.min(k, top.size()) - 1, oldNode);
    return true;
  }

  private static boolean allZero(Map<Integer, Double> scores) {
    return scores.values().stream().allMatch(v -> v == 0.0);
  }

  /** 按 (-score, id 升序) 排序取前 K（确定性 tie-break，KP-01）；零分候选按序补位 */
  private static List<KnowledgeNodeRow> topK(List<KnowledgeNodeRow> pool,
      Map<Integer, Double> scores, int k) {
    List<KnowledgeNodeRow> sorted = new ArrayList<>(pool);
    sorted.sort(Comparator
        .comparingDouble((KnowledgeNodeRow n) -> -scores.getOrDefault(n.getId(), 0.0))
        .thenComparing(n -> n.getId() == null ? Integer.MAX_VALUE : n.getId()));
    return new ArrayList<>(sorted.subList(0, Math.min(k, sorted.size())));
  }

  /** 单课程评分索引（构建后只读） */
  private static final class CourseIndex {

    private final List<KnowledgeNodeRow> l2List;
    private final Map<Integer, KnowledgeNodeRow> l2ById;
    private final Map<Integer, KnowledgeNodeRow> l3ById;
    private final Map<Integer, List<KnowledgeNodeRow>> childrenByL2;
    private final TermIndex l3Terms;

    CourseIndex(List<KnowledgeNodeRow> l2List, List<KnowledgeNodeRow> l3List) {
      this.l2List = List.copyOf(l2List);
      Map<Integer, KnowledgeNodeRow> l2Map = new HashMap<>();
      for (KnowledgeNodeRow n : this.l2List) {
        l2Map.put(n.getId(), n);
      }
      this.l2ById = Map.copyOf(l2Map);
      Map<Integer, KnowledgeNodeRow> l3Map = new HashMap<>();
      Map<Integer, List<KnowledgeNodeRow>> children = new HashMap<>();
      for (KnowledgeNodeRow n : l3List) {
        l3Map.put(n.getId(), n);
        children.computeIfAbsent(n.getParentId(), x -> new ArrayList<>()).add(n);
      }
      this.l3ById = Map.copyOf(l3Map);
      Map<Integer, List<KnowledgeNodeRow>> childrenCopy = new HashMap<>();
      for (Map.Entry<Integer, List<KnowledgeNodeRow>> e : children.entrySet()) {
        childrenCopy.put(e.getKey(), List.copyOf(e.getValue()));
      }
      this.childrenByL2 = Map.copyOf(childrenCopy);
      this.l3Terms = new TermIndex(l3List);
    }

    List<KnowledgeNodeRow> childrenOf(Integer l2Id) {
      return childrenByL2.getOrDefault(l2Id, List.of());
    }

    KnowledgeNodeRow l2ById(Integer id) {
      return l2ById.get(id);
    }

    KnowledgeNodeRow l3ById(Integer id) {
      return l3ById.get(id);
    }

    Map<Integer, Double> scoreL3(String text) {
      return l3Terms.score(text);
    }
  }

  /** 课程内倒排评分索引（构建后只读）：短语 + bigram，IDF 在本课程候选标题集合上计算 */
  private static final class TermIndex {

    private final Map<String, Double> phraseIdf;
    private final Map<String, Double> bigramIdf;
    private final Map<String, List<Integer>> phraseNodes;
    private final Map<String, List<Integer>> bigramNodes;
    private final Map<String, java.util.Set<String>> phraseBigrams;
    private final List<String> phrases;
    private final Map<Integer, List<String>> nodeBigrams;

    TermIndex(List<KnowledgeNodeRow> nodes) {
      Map<String, Counter> dfP = new HashMap<>();
      Map<String, Counter> dfB = new HashMap<>();
      Map<Integer, List<String>> np = new HashMap<>();
      Map<Integer, List<String>> nb = new HashMap<>();
      for (KnowledgeNodeRow n : nodes) {
        List<String> ps = new ArrayList<>();
        List<String> bs = new ArrayList<>();
        extractTerms(n.getTitle() == null ? "" : n.getTitle(), ps, bs);
        np.put(n.getId(), ps);
        nb.put(n.getId(), bs);
        for (String p : ps) {
          dfP.computeIfAbsent(p, x -> new Counter()).inc();
        }
        for (String b : bs) {
          dfB.computeIfAbsent(b, x -> new Counter()).inc();
        }
      }
      int total = Math.max(nodes.size(), 1);
      Map<String, Double> pidf = new HashMap<>();
      for (Map.Entry<String, Counter> e : dfP.entrySet()) {
        pidf.put(e.getKey(), Math.log(1 + total / (1.0 + e.getValue().value)) + 0.5);
      }
      Map<String, Double> bidf = new HashMap<>();
      for (Map.Entry<String, Counter> e : dfB.entrySet()) {
        bidf.put(e.getKey(), Math.log(1 + total / (1.0 + e.getValue().value)));
      }
      this.phraseIdf = Map.copyOf(pidf);
      this.bigramIdf = Map.copyOf(bidf);
      Map<String, List<Integer>> p2n = new HashMap<>();
      Map<String, java.util.Set<String>> pbg = new HashMap<>();
      for (Map.Entry<Integer, List<String>> e : np.entrySet()) {
        for (String p : e.getValue()) {
          p2n.computeIfAbsent(p, x -> new ArrayList<>()).add(e.getKey());
          pbg.computeIfAbsent(p, x -> bigramSetOf(p));
        }
      }
      this.phraseNodes = deepCopy(p2n);
      this.phraseBigrams = deepCopySets(pbg);
      Map<String, List<Integer>> b2n = new HashMap<>();
      for (Map.Entry<Integer, List<String>> e : nb.entrySet()) {
        for (String b : e.getValue()) {
          b2n.computeIfAbsent(b, x -> new ArrayList<>()).add(e.getKey());
        }
      }
      this.bigramNodes = deepCopy(b2n);
      this.phrases = List.copyOf(pidf.keySet());
      this.nodeBigrams = deepCopy(nb);
    }

    Map<Integer, Double> score(String text) {
      if (text == null || text.isEmpty() || phraseIdf.isEmpty()) {
        return Map.of();
      }
      java.util.Set<String> textBigrams = bigramSetOf(text);
      Map<Integer, Double> scores = new HashMap<>();
      List<String> hitPhrases = new ArrayList<>();
      for (String p : phrases) {
        if (text.contains(p) && textBigrams.containsAll(phraseBigrams.get(p))) {
          hitPhrases.add(p);
        }
      }
      java.util.Set<String> covered = new java.util.HashSet<>();
      for (String p : hitPhrases) {
        double w = phraseIdf.get(p) * Math.min(p.length() / 4.0, 2.0);
        for (Integer node : phraseNodes.getOrDefault(p, List.of())) {
          scores.merge(node, w, Double::sum);
        }
        covered.addAll(phraseBigrams.get(p));
      }
      for (Map.Entry<String, Double> e : bigramIdf.entrySet()) {
        String b = e.getKey();
        if (covered.contains(b) || !textBigrams.contains(b)) {
          continue;
        }
        for (Integer node : bigramNodes.getOrDefault(b, List.of())) {
          if (nodeBigrams.getOrDefault(node, List.of()).contains(b)) {
            scores.merge(node, e.getValue() * 0.25, Double::sum);
          }
        }
      }
      return scores;
    }

    private static java.util.Set<String> bigramSetOf(String s) {
      java.util.Set<String> set = new java.util.HashSet<>();
      for (int i = 0; i < s.length() - 1; i++) {
        set.add(s.substring(i, i + 2));
      }
      return set;
    }

    private static void extractTerms(String title, List<String> phrases, List<String> bigrams) {
      for (String s : title.split("[、，,；;：:（）()\\[\\]【】/／\\s★.。·\\-—]+")) {
        if (s.length() >= 2 && s.length() <= 14) {
          phrases.add(s);
        }
      }
      for (int i = 0; i < title.length() - 1; i++) {
        bigrams.add(title.substring(i, i + 2));
      }
    }

    private static <K, V> Map<K, List<V>> deepCopy(Map<K, List<V>> src) {
      Map<K, List<V>> copy = new LinkedHashMap<>();
      for (Map.Entry<K, List<V>> e : src.entrySet()) {
        copy.put(e.getKey(), List.copyOf(e.getValue()));
      }
      return Map.copyOf(copy);
    }

    private static Map<String, java.util.Set<String>> deepCopySets(
        Map<String, java.util.Set<String>> src) {
      Map<String, java.util.Set<String>> copy = new LinkedHashMap<>();
      for (Map.Entry<String, java.util.Set<String>> e : src.entrySet()) {
        copy.put(e.getKey(), Set.copyOf(e.getValue()));
      }
      return Map.copyOf(copy);
    }

    /** 可变 int 计数器（构建期用） */
    private static final class Counter {

      private int value;

      void inc() {
        value++;
      }

      int value() {
        return value;
      }
    }
  }
}
