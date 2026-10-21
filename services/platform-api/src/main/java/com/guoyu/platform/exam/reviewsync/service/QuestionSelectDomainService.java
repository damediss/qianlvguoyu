package com.guoyu.platform.exam.reviewsync.service;

import com.guoyu.platform.exam.reviewsync.dto.ReviewKnowledgeNode;
import com.guoyu.platform.exam.reviewsync.dto.ReviewQuestion;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 题目选择（§5.3 冗余裁剪 / §5.6 自组模考卷）。纯逻辑可单测。
 */
public final class QuestionSelectDomainService {

  private QuestionSelectDomainService() {
  }

  /** 题型权重（§5.3：多选 3、单选 2、判断 1，其他 1） */
  static int typeWeight(String type) {
    return switch (type == null ? "" : type) {
      case "multi" -> 3;
      case "single" -> 2;
      default -> 1;
    };
  }

  /** 题型排序权（保底选题时优先：多选 > 单选 > 判断 > 其他） */
  private static int typeRank(String type) {
    return switch (type == null ? "" : type) {
      case "multi" -> 4;
      case "single" -> 3;
      case "judge" -> 2;
      default -> 1;
    };
  }

  /** 重要度排序权（X > Y > Z > 空） */
  private static int importantRank(String important) {
    return switch (important == null ? "" : important) {
      case "X" -> 3;
      case "Y" -> 2;
      case "Z" -> 1;
      default -> 0;
    };
  }

  // ------------------------------------------------------------------
  // §5.3 题库冗余裁剪
  // ------------------------------------------------------------------

  /**
   * 从题目全集选出 target 题，知识点（L3）覆盖率 100%。
   *
   * @param questions 题目全集
   * @param tree      知识树（取 L3 重要度）
   * @param weights   权重（L3 归一化）
   * @param target    必做题数（target >= questions.size() 时全选）
   * @return 必做题 id 有序列表
   */
  public static List<Long> trimQuestionBank(List<ReviewQuestion> questions, List<ReviewKnowledgeNode> tree,
      WeightResolver.ResolvedWeights weights, int target) {
    if (target >= questions.size()) {
      return questions.stream().map(ReviewQuestion::id).toList();
    }
    Map<Long, ReviewKnowledgeNode> nodeById = new HashMap<>();
    for (ReviewKnowledgeNode node : tree) {
      nodeById.put(node.id(), node);
    }
    // 按 L3 分组
    Map<Long, List<ReviewQuestion>> byL3 = new LinkedHashMap<>();
    for (ReviewQuestion q : questions) {
      if (q.knowledgeId() != null) {
        byL3.computeIfAbsent(q.knowledgeId(), k -> new ArrayList<>()).add(q);
      }
    }

    List<ReviewQuestion> selected = new ArrayList<>();
    // ① 每个 L3 先取 1 题（L3 按重要度 X>Y>Z 优先保证；组内 多选>单选>判断>其他、难度中等优先）
    List<Long> l3Ordered = new ArrayList<>(byL3.keySet());
    l3Ordered.sort(Comparator.comparingInt((Long l3) -> {
      ReviewKnowledgeNode node = nodeById.get(l3);
      return node != null ? importantRank(node.important()) : 0;
    }).reversed());
    for (Long l3 : l3Ordered) {
      List<ReviewQuestion> group = new ArrayList<>(byL3.get(l3));
      group.sort(Comparator
          .comparingInt((ReviewQuestion q) -> typeRank(q.type()))
          .thenComparingInt(q -> q.difficulty() != null && q.difficulty() == 2 ? 1 : 0)
          .reversed());
      selected.add(group.get(0));
      group.remove(0);
      byL3.put(l3, group);
    }

    // ② 剩余名额按"L3 归一化权重 × 题型权重"加权轮转填满
    List<ReviewQuestion> rest = new ArrayList<>();
    for (List<ReviewQuestion> group : byL3.values()) {
      rest.addAll(group);
    }
    rest.sort(Comparator
        .comparingDouble((ReviewQuestion q) -> weights.weightOfL3(q.knowledgeId()) * typeWeight(q.type()))
        .reversed());
    int remaining = target - selected.size();
    for (int i = 0; i < Math.min(remaining, rest.size()); i++) {
      selected.add(rest.get(i));
    }

    List<Long> ids = new ArrayList<>();
    for (ReviewQuestion q : selected) {
      ids.add(q.id());
    }
    return ids;
  }

  /**
   * §5.3 第 4 条：中断补救"删尾部偏题"——在已选池内按 Z 重要度 → cal/answer/essay 题型 → 难度最高 排序删除。
   *
   * @param selectedIds 已选必做池
   * @param byId        题目全集 id → 题
   * @param nodeById    L3 节点
   * @param deleteCount 删除量
   * @return 被删除的题 id（调用方从必做池剔除）
   */
  public static List<Long> pickTailQuestionsToDrop(List<Long> selectedIds, Map<Long, ReviewQuestion> byId,
      Map<Long, ReviewKnowledgeNode> nodeById, int deleteCount) {
    List<ReviewQuestion> candidates = new ArrayList<>();
    for (Long id : selectedIds) {
      ReviewQuestion q = byId.get(id);
      if (q != null) {
        candidates.add(q);
      }
    }
    candidates.sort(Comparator
        .comparingInt((ReviewQuestion q) -> {
          ReviewKnowledgeNode node = q.knowledgeId() != null ? nodeById.get(q.knowledgeId()) : null;
          return node != null ? importantRank(node.important()) : 0;
        })
        .thenComparingInt(q -> isComplexType(q.type()) ? 0 : 1)
        .thenComparingInt(q -> q.difficulty() != null ? -q.difficulty() : 0));
    List<Long> dropped = new ArrayList<>();
    for (int i = 0; i < Math.min(deleteCount, candidates.size()); i++) {
      dropped.add(candidates.get(i).id());
    }
    return dropped;
  }

  private static boolean isComplexType(String type) {
    return "cal".equals(type) || "answer".equals(type) || "essay".equals(type);
  }

  // ------------------------------------------------------------------
  // §5.6 自组模考卷
  // ------------------------------------------------------------------

  /**
   * 按权重从必做池组卷。
   *
   * @param pool           必做池
   * @param weights        权重（L1 归一化 + l1ByL3）
   * @param mockSize       题量
   * @param wrongRateByL3  学员各 L3 错题率（0~1，无数据 L3 不在 map 中），用于 ×1.5 加权
   * @param random         随机源（测试可注入固定种子）
   * @return 卷题 id 有序列表（已随机排序，脱离题库顺序）
   */
  public static List<Long> composeMockPaper(List<ReviewQuestion> pool, WeightResolver.ResolvedWeights weights,
      int mockSize, Map<Long, Double> wrongRateByL3, Random random) {
    if (pool.size() <= mockSize) {
      List<Long> all = new ArrayList<>(pool.stream().map(ReviewQuestion::id).toList());
      Collections.shuffle(all, random);
      return all;
    }
    // 按 L1 分组
    Map<Long, List<ReviewQuestion>> byL1 = new LinkedHashMap<>();
    for (ReviewQuestion q : pool) {
      Long l1 = weights.l1ByL3().get(q.knowledgeId());
      if (l1 != null) {
        byL1.computeIfAbsent(l1, k -> new ArrayList<>()).add(q);
      }
    }
    // L1 归一化权重 → 大配额（water-filling：题量不足的 L1 差额再分配）
    Map<Long, Integer> quota = allocateQuota(weights.l1Weights(), byL1, mockSize);

    List<ReviewQuestion> selected = new ArrayList<>();
    for (Map.Entry<Long, Integer> entry : quota.entrySet()) {
      List<ReviewQuestion> group = new ArrayList<>(byL1.get(entry.getKey()));
      // 组内：错题率 ×1.5 加权 + 随机。
      // 抖动值必须先抽好存表再排序——比较器内联抽 random 会因键不确定违反排序契约
      // （生产实证：TimSort mergeHi 抛 Comparison method violates its general contract!）
      Map<Long, Double> jitterByQid = new HashMap<>();
      for (ReviewQuestion q : group) {
        jitterByQid.put(q.id(), random.nextDouble());
      }
      group.sort(Comparator.comparingDouble((ReviewQuestion q) -> {
        double wrongRate = wrongRateByL3.getOrDefault(q.knowledgeId(), 0.0);
        return -(1.0 + 1.5 * wrongRate) * jitterByQid.get(q.id());
      }));
      for (int i = 0; i < Math.min(entry.getValue(), group.size()); i++) {
        selected.add(group.get(i));
      }
    }
    // 题型约束：多选不少于 20%（不足则从未选中的多选题里补换）
    enforceMultiRatio(selected, pool, mockSize, random);

    List<Long> ids = new ArrayList<>();
    for (ReviewQuestion q : selected) {
      ids.add(q.id());
    }
    Collections.shuffle(ids, random);
    return ids;
  }

  /** L1 权重 → 整数配额（water-filling：先按权重取整并封顶题量，剩余名额按权重序轮询补齐） */
  static Map<Long, Integer> allocateQuota(Map<Long, Double> l1Weights, Map<Long, List<ReviewQuestion>> byL1,
      int mockSize) {
    Map<Long, Integer> quota = new LinkedHashMap<>();
    int assigned = 0;
    // 第一轮：权重取整 + 封顶各自题量
    for (Map.Entry<Long, Double> entry : l1Weights.entrySet()) {
      List<ReviewQuestion> group = byL1.get(entry.getKey());
      if (group == null || group.isEmpty()) {
        continue;
      }
      int q = Math.min((int) Math.floor(entry.getValue() * mockSize), group.size());
      quota.put(entry.getKey(), q);
      assigned += q;
    }
    // 第二轮：剩余名额（取整损耗 + 封顶盈余）按权重序轮询补齐到有题 L1
    List<Long> byWeight = new ArrayList<>(quota.keySet());
    byWeight.sort((a, b) -> Double.compare(l1Weights.getOrDefault(b, 0.0), l1Weights.getOrDefault(a, 0.0)));
    int remaining = mockSize - assigned;
    int guard = 0;
    while (remaining > 0 && guard++ < mockSize * 4 + 16) {
      boolean progressed = false;
      for (Long l1 : byWeight) {
        if (remaining <= 0) {
          break;
        }
        int current = quota.get(l1);
        int size = byL1.get(l1) != null ? byL1.get(l1).size() : 0;
        if (current < size) {
          quota.put(l1, current + 1);
          remaining--;
          progressed = true;
        }
      }
      if (!progressed) {
        break;
      }
    }
    return quota;
  }

  /** 多选比例不足 20% 时，用池内未选中的多选题替换非多选末位题 */
  private static void enforceMultiRatio(List<ReviewQuestion> selected, List<ReviewQuestion> pool, int mockSize,
      Random random) {
    long multiCount = selected.stream().filter(q -> "multi".equals(q.type())).count();
    int required = (int) Math.ceil(mockSize * 0.2);
    if (multiCount >= required) {
      return;
    }
    List<ReviewQuestion> poolMulti = new ArrayList<>();
    for (ReviewQuestion q : pool) {
      if ("multi".equals(q.type()) && selected.stream().noneMatch(s -> s.id() == q.id())) {
        poolMulti.add(q);
      }
    }
    Collections.shuffle(poolMulti, random);
    int need = required - (int) multiCount;
    for (int i = selected.size() - 1; i >= 0 && need > 0 && !poolMulti.isEmpty(); i--) {
      if (!"multi".equals(selected.get(i).type())) {
        selected.set(i, poolMulti.remove(0));
        need--;
      }
    }
  }

  /**
   * 模考卷主观题数量上限裁剪（《简答题作答与AI判分方案》Q2-A：同步交卷的墙钟预算约束）。
   *
   * <p>裁剪规则：客观题（single/multi/judge）永不裁；主观题超过 maxSubjective 时，
   * 按题型优先级裁——cal/answer/essay 复杂主观题（与 §5.3-4 pickTailQuestionsToDrop 的
   * "偏题"口径一致）优先裁，其余主观题按组卷原顺序裁尾部（组卷已按 L1 配额+错题率加权
   * +随机定序）。被裁名额不回补客观题：入卷题量相应减少，调用方按实际入卷题重算总分/
   * 及格分（passScore=total×0.6 同比缩小，口径自洽）。
   *
   * @param questionIds    组卷产出的卷题 id（有序）
   * @param byId           题 id → 轻量元数据（取 type 用）
   * @param maxSubjective  主观题上限（sys_dict ai_review_mock_subjective_max）
   * @return 裁剪后的卷题 id（保持组卷原顺序）
   */
  public static List<Long> capSubjectiveCount(List<Long> questionIds, Map<Long, ReviewQuestion> byId,
      int maxSubjective) {
    List<Long> subjective = new ArrayList<>();
    for (Long id : questionIds) {
      ReviewQuestion q = byId.get(id);
      if (q != null && AiGradeApplicationService.isSubjective(q.type())) {
        subjective.add(id);
      }
    }
    if (subjective.size() <= maxSubjective) {
      return questionIds;
    }
    // 裁 drop 个：cal/answer/essay 复杂主观题排前（优先裁）；同档内按组卷原顺序从尾部裁
    // （组卷已按 L1 配额+错题率加权+随机定序，保留更靠前的入选题）
    Map<Long, Integer> orderIndex = new HashMap<>();
    for (int i = 0; i < subjective.size(); i++) {
      orderIndex.put(subjective.get(i), i);
    }
    List<Long> dropOrder = new ArrayList<>(subjective);
    dropOrder.sort(Comparator
        .comparingInt((Long id) -> {
          ReviewQuestion q = byId.get(id);
          return q != null && isComplexType(q.type()) ? 0 : 1;
        })
        .thenComparing(Comparator.comparingInt(orderIndex::get).reversed()));
    Set<Long> dropped = new HashSet<>(dropOrder.subList(0, subjective.size() - maxSubjective));
    List<Long> kept = new ArrayList<>(questionIds.size() - dropped.size());
    for (Long id : questionIds) {
      if (!dropped.contains(id)) {
        kept.add(id);
      }
    }
    return kept;
  }

  /** 题型自然分布（组卷说明/快照用） */
  public static Map<String, Integer> typeDistribution(List<ReviewQuestion> questions) {
    Map<String, Integer> dist = new HashMap<>();
    for (ReviewQuestion q : questions) {
      dist.merge(q.type() == null ? "" : q.type(), 1, Integer::sum);
    }
    return dist;
  }

  /** 分值合计 */
  public static BigDecimal totalScore(List<ReviewQuestion> questions, Map<Long, ReviewQuestion> byId, List<Long> ids) {
    BigDecimal total = BigDecimal.ZERO;
    for (Long id : ids) {
      ReviewQuestion q = byId.get(id);
      if (q != null && q.score() != null) {
        total = total.add(q.score());
      }
    }
    return total;
  }
}
