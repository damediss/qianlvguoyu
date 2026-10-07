package com.guoyu.platform.exam.reviewsync.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.guoyu.platform.exam.reviewsync.dto.ReviewKnowledgeNode;
import com.guoyu.platform.exam.reviewsync.dto.ReviewQuestion;
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

  private static List<ReviewKnowledgeNode> tree(int l3Count) {
    List<ReviewKnowledgeNode> nodes = new ArrayList<>();
    nodes.add(new ReviewKnowledgeNode(1, null, "相关知识", 0, null));
    nodes.add(new ReviewKnowledgeNode(10, 1L, "生产操作", 1, null));
    for (int i = 0; i < l3Count; i++) {
      nodes.add(new ReviewKnowledgeNode(100 + i, 10L, "L2-" + i, 2, null));
      nodes.add(new ReviewKnowledgeNode(1000 + i, (long) (100 + i), "L3-" + i, 3, i % 3 == 0 ? "X" : "Z"));
    }
    return nodes;
  }

  private static List<ReviewQuestion> questions(int l3Count, int perL3) {
    List<ReviewQuestion> list = new ArrayList<>();
    long id = 1;
    String[] types = {"single", "multi", "judge"};
    for (int l3 = 0; l3 < l3Count; l3++) {
      for (int i = 0; i < perL3; i++) {
        list.add(new ReviewQuestion(id++, 1000L + l3, types[i % 3], 1 + (i % 3), BigDecimal.ONE));
      }
    }
    return list;
  }

  private static WeightResolver.ResolvedWeights uniformWeights(List<ReviewKnowledgeNode> tree,
      List<ReviewQuestion> questions) {
    return WeightResolver.resolve(new com.guoyu.platform.exam.reviewsync.dto.ReviewCourseWeights(null, List.of()),
        tree, questions);
  }

  @Test
  void trimKeeps100PercentKnowledgeCoverage() {
    List<ReviewKnowledgeNode> tree = tree(20);
    List<ReviewQuestion> questions = questions(20, 150); // 3000 题 20 个 L3
    WeightResolver.ResolvedWeights weights = uniformWeights(tree, questions);

    List<Long> selected = QuestionSelectDomainService.trimQuestionBank(questions, tree, weights, 1200);

    assertThat(selected).hasSize(1200);
    // 覆盖率 100%：20 个 L3 每个至少 1 题
    Map<Long, ReviewQuestion> byId = new HashMap<>();
    questions.forEach(q -> byId.put(q.id(), q));
    Set<Long> coveredL3 = new HashSet<>();
    selected.forEach(id -> coveredL3.add(byId.get(id).knowledgeId()));
    assertThat(coveredL3).hasSize(20);
    // 无重复
    assertThat(new HashSet<>(selected)).hasSize(1200);
  }

  @Test
  void trimWithTargetLargerThanPoolSelectsAll() {
    List<ReviewKnowledgeNode> tree = tree(5);
    List<ReviewQuestion> questions = questions(5, 10);
    List<Long> selected = QuestionSelectDomainService.trimQuestionBank(
        questions, tree, uniformWeights(tree, questions), 100);
    assertThat(selected).hasSize(50);
  }

  @Test
  void mockPaperHasNoDuplicatesAndEnforcesMultiRatio() {
    List<ReviewKnowledgeNode> tree = tree(10);
    // 构造题型偏斜：多选极少（每 L3 只有 1 道多选）
    List<ReviewQuestion> questions = new ArrayList<>();
    long id = 1;
    for (int l3 = 0; l3 < 10; l3++) {
      for (int i = 0; i < 19; i++) {
        questions.add(new ReviewQuestion(id++, 1000L + l3, "single", 2, BigDecimal.ONE));
      }
      questions.add(new ReviewQuestion(id++, 1000L + l3, "multi", 2, BigDecimal.ONE));
    }
    WeightResolver.ResolvedWeights weights = uniformWeights(tree, questions);

    List<Long> paper = QuestionSelectDomainService.composeMockPaper(
        questions, weights, 100, Map.of(), new Random(42));

    assertThat(new HashSet<>(paper)).hasSize(100);
    Map<Long, ReviewQuestion> byId = new HashMap<>();
    questions.forEach(q -> byId.put(q.id(), q));
    long multi = paper.stream().filter(qid -> "multi".equals(byId.get(qid).type())).count();
    assertThat(multi).isGreaterThanOrEqualTo(10); // 池内多选仅 10 道，全部用上（比例兜底逻辑不虚构题）
  }

  @Test
  void allocateQuotaWaterFillsWhenCategoryLacksQuestions() {
    Map<Long, Double> weights = Map.of(1L, 0.8, 2L, 0.2);
    Map<Long, List<ReviewQuestion>> byL1 = Map.of(
        1L, List.of(new ReviewQuestion(1, 1000L, "single", 2, BigDecimal.ONE)), // 只有 1 题
        2L, List.of(
            new ReviewQuestion(2, 1001L, "single", 2, BigDecimal.ONE),
            new ReviewQuestion(3, 1001L, "single", 2, BigDecimal.ONE),
            new ReviewQuestion(4, 1001L, "single", 2, BigDecimal.ONE)));
    Map<Long, Integer> quota = QuestionSelectDomainService.allocateQuota(weights, byL1, 4);
    assertThat(quota.get(1L)).isEqualTo(1); // 封顶其题量
    assertThat(quota.get(2L)).isEqualTo(3); // 差额让渡
    assertThat(quota.values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(4);
  }

  @Test
  void tailDropPrefersLowImportanceAndComplexTypes() {
    List<ReviewKnowledgeNode> tree = List.of(
        new ReviewKnowledgeNode(1, null, "R", 0, null),
        new ReviewKnowledgeNode(10, 1L, "L1", 1, null),
        new ReviewKnowledgeNode(100, 10L, "L2", 2, null),
        new ReviewKnowledgeNode(1000, 100L, "重点L3", 3, "X"),
        new ReviewKnowledgeNode(1001, 100L, "偏门L3", 3, "Z"));
    Map<Long, ReviewKnowledgeNode> nodeById = new HashMap<>();
    tree.forEach(n -> nodeById.put(n.id(), n));
    Map<Long, ReviewQuestion> byId = Map.of(
        1L, new ReviewQuestion(1, 1000L, "single", 1, BigDecimal.ONE),
        2L, new ReviewQuestion(2, 1001L, "cal", 5, BigDecimal.ONE),
        3L, new ReviewQuestion(3, 1001L, "judge", 1, BigDecimal.ONE));
    List<Long> dropped = QuestionSelectDomainService.pickTailQuestionsToDrop(
        List.of(1L, 2L, 3L), byId, nodeById, 1);
    // Z 重要度 + cal 题型 + 最高难度 → 先删 2 号
    assertThat(dropped).containsExactly(2L);
  }

  // ------------------------------------------------------------------
  // 模考卷主观题数量上限（《简答题作答与AI判分方案》Q2-A）
  // ------------------------------------------------------------------

  @Test
  void capSubjectiveCountUntouchedWhenUnderLimit() {
    Map<Long, ReviewQuestion> byId = Map.of(
        1L, new ReviewQuestion(1, 1000L, "single", 1, BigDecimal.ONE),
        2L, new ReviewQuestion(2, 1000L, "answer", 1, BigDecimal.ONE));
    List<Long> ids = List.of(1L, 2L);
    assertThat(QuestionSelectDomainService.capSubjectiveCount(ids, byId, 5)).isSameAs(ids);
  }

  @Test
  void capSubjectiveCountDropsComplexSubjectiveFirstAndKeepsObjectives() {
    // 3 客观 + 6 主观（cal/answer/essay 复杂档 + analysis/fill/short），上限 5 → 裁 1 个复杂档
    Map<Long, ReviewQuestion> byId = Map.of(
        1L, new ReviewQuestion(1, 1000L, "single", 1, BigDecimal.ONE),
        2L, new ReviewQuestion(2, 1000L, "multi", 1, BigDecimal.ONE),
        3L, new ReviewQuestion(3, 1000L, "judge", 1, BigDecimal.ONE),
        4L, new ReviewQuestion(4, 1000L, "cal", 1, BigDecimal.ONE),
        5L, new ReviewQuestion(5, 1000L, "answer", 1, BigDecimal.ONE),
        6L, new ReviewQuestion(6, 1000L, "essay", 1, BigDecimal.ONE),
        7L, new ReviewQuestion(7, 1000L, "analysis", 1, BigDecimal.ONE),
        8L, new ReviewQuestion(8, 1000L, "fill", 1, BigDecimal.ONE),
        9L, new ReviewQuestion(9, 1000L, "short", 1, BigDecimal.ONE));
    List<Long> ids = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L);

    List<Long> kept = QuestionSelectDomainService.capSubjectiveCount(ids, byId, 5);

    // 复杂档 cal/answer/essay 优先裁、同档内从尾部裁（essay 先出局）；客观题全保留，组卷原顺序不变
    assertThat(kept).containsExactly(1L, 2L, 3L, 4L, 5L, 7L, 8L, 9L);
    // 上限再收紧到 3：复杂档 3 题全部裁掉，保留 analysis/fill/short
    assertThat(QuestionSelectDomainService.capSubjectiveCount(ids, byId, 3))
        .containsExactly(1L, 2L, 3L, 7L, 8L, 9L);
  }

  @Test
  void capSubjectiveCountDropsTailWithinSamePriority() {
    // 同档（非复杂）主观题超上限：按组卷原顺序裁尾部
    Map<Long, ReviewQuestion> byId = new HashMap<>();
    for (long i = 1; i <= 6; i++) {
      byId.put(i, new ReviewQuestion(i, 1000L, "analysis", 1, BigDecimal.ONE));
    }
    List<Long> kept = QuestionSelectDomainService.capSubjectiveCount(
        List.of(1L, 2L, 3L, 4L, 5L, 6L), byId, 5);
    assertThat(kept).containsExactly(1L, 2L, 3L, 4L, 5L);
  }
}
