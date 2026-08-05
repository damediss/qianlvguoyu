package com.guoyu.platform.exam.reviewsync.service;

import com.guoyu.platform.integration.php.dto.PhpCourseWeights;
import com.guoyu.platform.integration.php.dto.PhpKnowledgeNode;
import com.guoyu.platform.integration.php.dto.PhpQuestion;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 权重解析器（§5.7）：type=3 国标 → type=0 人工 → 题量占比兜底。
 *
 * <p>职责：
 * <ol>
 *   <li>构建 L3 → L1 映射（沿 parent 链爬到 level=1）；</li>
 *   <li>L1 归一化权重（Σrate 可能 ≠100，按 rate_i/Σ 归一；无题 L1 剔除后重新归一）；</li>
 *   <li>L1→L3 细分（L1 内按 L3 题量占比）；</li>
 *   <li>无配置时按 L1 题量占比兜底（weightSource="count"，命中记 warn 由调用方处理）。</li>
 * </ol>
 * 纯逻辑，全部静态方法，可单测。
 */
public final class WeightResolver {

  private WeightResolver() {
  }

  /**
   * @param l1Weights      L1 归一化权重（key=L1 knowledgeId，value=0~1，Σ=1）
   * @param l3Weights      L3 归一化权重（key=L3 knowledgeId，value=0~1，Σ=1，仅含有题的 L3）
   * @param l1ByL3         L3 → L1 映射
   * @param weightSource   standard/custom/count
   * @param l1Titles       L1 id → 标题（快照/展示用）
   */
  public record ResolvedWeights(
      Map<Long, Double> l1Weights,
      Map<Long, Double> l3Weights,
      Map<Long, Long> l1ByL3,
      String weightSource,
      Map<Long, String> l1Titles) {

    /** 某 L3 的归一化权重（未配置 L1 下的 L3 为 0） */
    public double weightOfL3(long l3Id) {
      return l3Weights.getOrDefault(l3Id, 0.0);
    }
  }

  /**
   * @param courseWeights PHP 侧权重配置（可为空）
   * @param tree          课程知识结构全树
   * @param questions     题目全集（必做池候选）
   */
  public static ResolvedWeights resolve(PhpCourseWeights courseWeights, List<PhpKnowledgeNode> tree,
      List<PhpQuestion> questions) {
    Map<Long, PhpKnowledgeNode> nodeById = new HashMap<>();
    for (PhpKnowledgeNode node : tree) {
      nodeById.put(node.id(), node);
    }

    // L3 → L1 映射 + 每 L3 题数
    Map<Long, Long> l1ByL3 = new HashMap<>();
    Map<Long, Long> questionCountByL3 = new HashMap<>();
    for (PhpQuestion question : questions) {
      Long l3 = question.knowledgeId();
      if (l3 == null) {
        continue;
      }
      questionCountByL3.merge(l3, 1L, Long::sum);
      l1ByL3.computeIfAbsent(l3, k -> findL1(nodeById, l3));
    }

    // 每 L1 题数与标题
    Map<Long, Long> questionCountByL1 = new HashMap<>();
    Map<Long, String> l1Titles = new LinkedHashMap<>();
    for (Map.Entry<Long, Long> entry : l1ByL3.entrySet()) {
      Long l1 = entry.getValue();
      if (l1 == null) {
        continue;
      }
      questionCountByL1.merge(l1, questionCountByL3.get(entry.getKey()), Long::sum);
      PhpKnowledgeNode l1Node = nodeById.get(l1);
      l1Titles.putIfAbsent(l1, l1Node != null ? l1Node.title() : String.valueOf(l1));
    }

    // L1 权重：配置优先，兜底题量占比
    Map<Long, Double> l1Weights;
    String weightSource;
    if (courseWeights != null && !courseWeights.isEmpty()) {
      weightSource = courseWeights.sourceType() != null ? courseWeights.sourceType() : "custom";
      // 配置可能挂在任意层级（人工配置实测有挂 L2 的）：统一归并到 L1 祖先节点
      Map<Long, Integer> configured = new LinkedHashMap<>();
      for (PhpCourseWeights.WeightItem item : courseWeights.items()) {
        Long l1 = findL1(nodeById, item.knowledgeId());
        if (l1 != null) {
          configured.merge(l1, item.rate(), Integer::sum);
          l1Titles.putIfAbsent(l1, item.title());
        }
      }
      l1Weights = normalize(configured, questionCountByL1);
      if (l1Weights.isEmpty()) {
        // 配置节点与题目 L1 完全错位：等价于无配置，按题量占比兜底
        weightSource = "count";
        l1Weights = countProportion(questionCountByL1);
      }
    } else {
      weightSource = "count";
      l1Weights = countProportion(questionCountByL1);
    }

    // L1→L3 细分：L1 内按 L3 题量占比
    Map<Long, Double> l3Weights = new HashMap<>();
    for (Map.Entry<Long, Long> entry : l1ByL3.entrySet()) {
      long l3 = entry.getKey();
      Long l1 = entry.getValue();
      if (l1 == null) {
        continue;
      }
      double l1Weight = l1Weights.getOrDefault(l1, 0.0);
      if (l1Weight <= 0) {
        continue;
      }
      long l1Total = questionCountByL1.getOrDefault(l1, 0L);
      if (l1Total <= 0) {
        continue;
      }
      l3Weights.put(l3, l1Weight * questionCountByL3.get(l3) / (double) l1Total);
    }

    return new ResolvedWeights(l1Weights, l3Weights, l1ByL3, weightSource, l1Titles);
  }

  /** 沿 parent 链爬到 level=1 的节点；爬不到返回 null（数据异常容忍） */
  private static Long findL1(Map<Long, PhpKnowledgeNode> nodeById, long startId) {
    Long current = startId;
    int guard = 0;
    while (current != null && guard++ < 10) {
      PhpKnowledgeNode node = nodeById.get(current);
      if (node == null) {
        return null;
      }
      if (node.level() != null && node.level() == 1) {
        return node.id();
      }
      current = node.parentId();
    }
    return null;
  }

  /** 题量占比兜底：L1 权重 = 该 L1 题数 / 总题数 */
  private static Map<Long, Double> countProportion(Map<Long, Long> questionCountByL1) {
    Map<Long, Integer> counts = new LinkedHashMap<>();
    for (Map.Entry<Long, Long> entry : questionCountByL1.entrySet()) {
      counts.put(entry.getKey(), entry.getValue().intValue());
    }
    return normalize(counts, questionCountByL1);
  }

  /**
   * 归一化：只保留"有题的 L1"，rate_i / Σrate。
   * 配置中有权重但无题的 L1 被剔除（其权重按比例让渡给其余项）。
   */
  private static Map<Long, Double> normalize(Map<Long, Integer> rawRates, Map<Long, Long> questionCountByL1) {
    long total = 0;
    Map<Long, Integer> effective = new LinkedHashMap<>();
    for (Map.Entry<Long, Integer> entry : rawRates.entrySet()) {
      if (entry.getValue() == null || entry.getValue() <= 0) {
        continue;
      }
      // 无题的 L1 不参与（除非全部都没题——调用方不会出现此情形，防御保留）
      if (questionCountByL1.getOrDefault(entry.getKey(), 0L) <= 0) {
        continue;
      }
      effective.put(entry.getKey(), entry.getValue());
      total += entry.getValue();
    }
    Map<Long, Double> normalized = new LinkedHashMap<>();
    if (total <= 0) {
      return normalized;
    }
    for (Map.Entry<Long, Integer> entry : effective.entrySet()) {
      normalized.put(entry.getKey(), entry.getValue() / (double) total);
    }
    return normalized;
  }

  /** 权重快照序列化（写入 ai_review_plan.weight_snapshot） */
  public static List<Map<String, Object>> toSnapshot(ResolvedWeights weights) {
    List<Map<String, Object>> snapshot = new ArrayList<>();
    for (Map.Entry<Long, Double> entry : weights.l1Weights().entrySet()) {
      Map<String, Object> item = new HashMap<>();
      item.put("knowledgeId", entry.getKey());
      item.put("title", weights.l1Titles().get(entry.getKey()));
      item.put("rate", Math.round(entry.getValue() * 1000) / 10.0); // 百分比，保留 1 位小数
      snapshot.add(item);
    }
    return snapshot;
  }
}
