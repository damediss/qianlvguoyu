package com.guoyu.platform.exam.reviewsync.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.guoyu.platform.integration.php.dto.PhpKnowledgeNode;
import com.guoyu.platform.integration.php.dto.PhpQuestion;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** §10.1 裁剪（覆盖率 100%）与组卷（题型比例、无重复、配额加权） */
class QuestionSelectDomainServiceTest {

  private static List<PhpKnowledgeNode> tree(int l3Count) {
    List<PhpKnowledgeNode> nodes = new ArrayList<>();
    nodes.add(new PhpKnowledgeNode(1, null, "相关知识", 0, null));
    nodes.add(new PhpKnowledgeNode(10, 1L, "生产操作", 1, null));
    for (int i = 0; i < l3Count; i++) {
      nodes.add(new PhpKnowledgeNode(100 + i, 10L, "L2-" + i, 2, null));
      nodes.add(new PhpKnowledgeNode(1000 + i, (long) (100 + i), "L3-" + i, 3, i % 3 == 0 ? "X" : "Z"));
    }
    return nodes;
  }

  private static List<PhpQuestion> questions(int l3Count, int perL3) {
    List<PhpQuestion> list = new ArrayList<>();
    long id = 1;
    String[] types = {"single", "multi", "judge"};
    for (int l3 = 0; l3 < l3Count; l3++) {
      for (int i = 0; i < perL3; i++) {
        list.add(new PhpQuestion(id++, 1000L + l3, types[i % 3], 1 + (i % 3), BigDecimal.ONE));
      }
    }
    return list;
  }

  private static WeightResolver.ResolvedWeights uniformWeights(List<PhpKnowledgeNode> tree,
      List<PhpQuestion> questions) {
    return WeightResolver.resolve(new com.guoyu.platform.integration.php.dto.PhpCourseWeights(null, List.of()),
        tree, questions);
  }

  @Test
  void trimKeeps100PercentKnowledgeCoverage() {
    List<PhpKnowledgeNode> tree = tree(20);
    List<PhpQuestion> questions = questions(20, 150); // 3000 题 20 个 L3
    WeightResolver.ResolvedWeights weights = uniformWeights(tree, questions);

    List<Long> selected = QuestionSelectDomainService.trimQuestionBank(questions, tree, weights, 1200);

    assertThat(selected).hasSize(1200);
    // 覆盖率 100%：20 个 L3 每个至少 1 题
    Map<Long, PhpQuestion> byId = new HashMap<>();
    questions.forEach(q -> byId.put(q.id(), q));
    Set<Long> coveredL3 = new HashSet<>();
    selected.forEach(id -> coveredL3.add(byId.get(id).knowledgeId()));
    assertThat(coveredL3).hasSize(20);
    // 无重复
    assertThat(new HashSet<>(selected)).hasSize(1200);
  }

  @Test
  void trimWithTargetLargerThanPoolSelectsAll() {
    List<PhpKnowledgeNode> tree = tree(5);
    List<PhpQuestion> questions = questions(5, 10);
    List<Long> selected = QuestionSelectDomainService.trimQuestionBank(
        questions, tree, uniformWeights(tree, questions), 100);
    assertThat(selected).hasSize(50);
  }

  @Test
  void mockPaperHasNoDuplicatesAndEnforcesMultiRatio() {
    List<PhpKnowledgeNode> tree = tree(10);
    // 构造题型偏斜：多选极少（每 L3 只有 1 道多选）
    List<PhpQuestion> questions = new ArrayList<>();
    long id = 1;
    for (int l3 = 0; l3 < 10; l3++) {
      for (int i = 0; i < 19; i++) {
        questions.add(new PhpQuestion(id++, 1000L + l3, "single", 2, BigDecimal.ONE));
      }
      questions.add(new PhpQuestion(id++, 1000L + l3, "multi", 2, BigDecimal.ONE));
    }
    WeightResolver.ResolvedWeights weights = uniformWeights(tree, questions);

    List<Long> paper = QuestionSelectDomainService.composeMockPaper(
        questions, weights, 100, Map.of(), new Random(42));

    assertThat(new HashSet<>(paper)).hasSize(100);
    Map<Long, PhpQuestion> byId = new HashMap<>();
    questions.forEach(q -> byId.put(q.id(), q));
    long multi = paper.stream().filter(qid -> "multi".equals(byId.get(qid).type())).count();
    assertThat(multi).isGreaterThanOrEqualTo(10); // 池内多选仅 10 道，全部用上（比例兜底逻辑不虚构题）
  }

  @Test
  void allocateQuotaWaterFillsWhenCategoryLacksQuestions() {
    Map<Long, Double> weights = Map.of(1L, 0.8, 2L, 0.2);
    Map<Long, List<PhpQuestion>> byL1 = Map.of(
        1L, List.of(new PhpQuestion(1, 1000L, "single", 2, BigDecimal.ONE)), // 只有 1 题
        2L, List.of(
            new PhpQuestion(2, 1001L, "single", 2, BigDecimal.ONE),
            new PhpQuestion(3, 1001L, "single", 2, BigDecimal.ONE),
            new PhpQuestion(4, 1001L, "single", 2, BigDecimal.ONE)));
    Map<Long, Integer> quota = QuestionSelectDomainService.allocateQuota(weights, byL1, 4);
    assertThat(quota.get(1L)).isEqualTo(1); // 封顶其题量
    assertThat(quota.get(2L)).isEqualTo(3); // 差额让渡
    assertThat(quota.values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(4);
  }

  @Test
  void tailDropPrefersLowImportanceAndComplexTypes() {
    List<PhpKnowledgeNode> tree = List.of(
        new PhpKnowledgeNode(1, null, "R", 0, null),
        new PhpKnowledgeNode(10, 1L, "L1", 1, null),
        new PhpKnowledgeNode(100, 10L, "L2", 2, null),
        new PhpKnowledgeNode(1000, 100L, "重点L3", 3, "X"),
        new PhpKnowledgeNode(1001, 100L, "偏门L3", 3, "Z"));
    Map<Long, PhpKnowledgeNode> nodeById = new HashMap<>();
    tree.forEach(n -> nodeById.put(n.id(), n));
    Map<Long, PhpQuestion> byId = Map.of(
        1L, new PhpQuestion(1, 1000L, "single", 1, BigDecimal.ONE),
        2L, new PhpQuestion(2, 1001L, "cal", 5, BigDecimal.ONE),
        3L, new PhpQuestion(3, 1001L, "judge", 1, BigDecimal.ONE));
    List<Long> dropped = QuestionSelectDomainService.pickTailQuestionsToDrop(
        List.of(1L, 2L, 3L), byId, nodeById, 1);
    // Z 重要度 + cal 题型 + 最高难度 → 先删 2 号
    assertThat(dropped).containsExactly(2L);
  }
}
