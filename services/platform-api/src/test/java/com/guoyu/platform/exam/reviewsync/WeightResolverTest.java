package com.guoyu.platform.exam.reviewsync.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.guoyu.platform.exam.reviewsync.dto.ReviewCourseWeights;
import com.guoyu.platform.exam.reviewsync.dto.ReviewKnowledgeNode;
import com.guoyu.platform.exam.reviewsync.dto.ReviewQuestion;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** §10.1 权重回退链：type=3 命中 / 仅 type=0 / 全无配置三态；归一化；L1→L3 细分 */
class WeightResolverTest {

  /** 构造树：L0(1,2) → L1(11 职业道德,12 基础知识,13 生产操作) → L2(21,22,23) → L3(31,32,33) */
  private static List<ReviewKnowledgeNode> tree() {
    return List.of(
        new ReviewKnowledgeNode(1, null, "基本要求", 0, null),
        new ReviewKnowledgeNode(2, null, "相关知识", 0, null),
        new ReviewKnowledgeNode(11, 1L, "职业道德", 1, null),
        new ReviewKnowledgeNode(12, 1L, "基础知识", 1, null),
        new ReviewKnowledgeNode(13, 2L, "生产操作", 1, null),
        new ReviewKnowledgeNode(21, 11L, "职业道德基本知识", 2, null),
        new ReviewKnowledgeNode(22, 12L, "化学基础", 2, null),
        new ReviewKnowledgeNode(23, 13L, "工艺操作", 2, null),
        new ReviewKnowledgeNode(31, 21L, "工匠精神", 3, "X"),
        new ReviewKnowledgeNode(32, 22L, "铝的性质", 3, "Y"),
        new ReviewKnowledgeNode(33, 23L, "焙烧启动", 3, "Z"));
  }

  private static List<ReviewQuestion> questions(int... countsByL3) {
    // countsByL3[0] 道挂在 L3=31，[1] 道挂 32，[2] 道挂 33
    List<ReviewQuestion> list = new java.util.ArrayList<>();
    long id = 1;
    long[] l3s = {31, 32, 33};
    for (int k = 0; k < countsByL3.length; k++) {
      for (int i = 0; i < countsByL3[k]; i++) {
        list.add(new ReviewQuestion(id++, l3s[k], "single", 2, BigDecimal.ONE));
      }
    }
    return list;
  }

  @Test
  void standardWeightsAreNormalizedAndDistributedToL3() {
    ReviewCourseWeights configured = new ReviewCourseWeights("standard", List.of(
        new ReviewCourseWeights.WeightItem(11, "职业道德", 5),
        new ReviewCourseWeights.WeightItem(12, "基础知识", 35),
        new ReviewCourseWeights.WeightItem(13, "生产操作", 40)));
    WeightResolver.ResolvedWeights resolved = WeightResolver.resolve(configured, tree(), questions(10, 10, 10));

    assertThat(resolved.weightSource()).isEqualTo("standard");
    // Σrate=80 → 归一化 5/80, 35/80, 40/80
    assertThat(resolved.l1Weights().get(11L)).isEqualTo(5 / 80.0);
    assertThat(resolved.l1Weights().get(12L)).isEqualTo(35 / 80.0);
    assertThat(resolved.l1Weights().get(13L)).isEqualTo(40 / 80.0);
    // L3 均分 L1 权重（各 L1 下只有 1 个有题 L3）
    assertThat(resolved.weightOfL3(31)).isEqualTo(5 / 80.0);
    assertThat(resolved.weightOfL3(33)).isEqualTo(40 / 80.0);
    assertThat(resolved.l1ByL3().get(31L)).isEqualTo(11L);
    assertThat(resolved.l1ByL3().get(33L)).isEqualTo(13L);
  }

  @Test
  void emptyConfigFallsBackToQuestionCountProportion() {
    WeightResolver.ResolvedWeights resolved = WeightResolver.resolve(
        new ReviewCourseWeights(null, List.of()), tree(), questions(49, 19, 32));

    assertThat(resolved.weightSource()).isEqualTo("count");
    assertThat(resolved.l1Weights().get(11L)).isEqualTo(0.49);
    assertThat(resolved.l1Weights().get(12L)).isEqualTo(0.19);
    assertThat(resolved.l1Weights().get(13L)).isEqualTo(0.32);
  }

  @Test
  void l1WithoutQuestionsIsExcludedFromNormalization() {
    ReviewCourseWeights configured = new ReviewCourseWeights("standard", List.of(
        new ReviewCourseWeights.WeightItem(11, "职业道德", 50),
        new ReviewCourseWeights.WeightItem(12, "基础知识", 50),
        new ReviewCourseWeights.WeightItem(99, "无题项目", 100)));
    WeightResolver.ResolvedWeights resolved = WeightResolver.resolve(configured, tree(), questions(5, 5, 0));

    // 无题的 99 被剔除，11/12 按 50:50 归一
    assertThat(resolved.l1Weights()).containsOnlyKeys(11L, 12L);
    assertThat(resolved.l1Weights().get(11L)).isEqualTo(0.5);
    assertThat(resolved.weightOfL3(33)).isEqualTo(0.0);
  }

  @Test
  void configAttachedAtL2IsMappedToL1Ancestor() {
    // 人工配置实测会挂 L2（course 283 案例）：rate=100 挂 L2=21 → 归并到 L1=11
    ReviewCourseWeights configured = new ReviewCourseWeights("custom", List.of(
        new ReviewCourseWeights.WeightItem(21, "职业道德基本知识", 100)));
    WeightResolver.ResolvedWeights resolved = WeightResolver.resolve(configured, tree(), questions(10, 10, 10));
    assertThat(resolved.weightSource()).isEqualTo("custom");
    assertThat(resolved.l1Weights().get(11L)).isEqualTo(1.0);
    assertThat(resolved.weightOfL3(31)).isEqualTo(1.0);
  }

  @Test
  void configWithTotalMismatchFallsBackToCountProportion() {
    // 配置节点与题目 L1 完全错位（有权重但全部无题）：等价无配置，按题量兜底
    ReviewCourseWeights configured = new ReviewCourseWeights("custom", List.of(
        new ReviewCourseWeights.WeightItem(99, "不存在的项目", 100)));
    WeightResolver.ResolvedWeights resolved = WeightResolver.resolve(configured, tree(), questions(60, 30, 10));
    assertThat(resolved.weightSource()).isEqualTo("count");
    assertThat(resolved.l1Weights().get(11L)).isEqualTo(0.6);
  }

  @Test
  void snapshotSerializesPercentages() {
    ReviewCourseWeights configured = new ReviewCourseWeights("standard", List.of(
        new ReviewCourseWeights.WeightItem(11, "职业道德", 25),
        new ReviewCourseWeights.WeightItem(12, "基础知识", 75)));
    WeightResolver.ResolvedWeights resolved = WeightResolver.resolve(configured, tree(), questions(5, 5, 0));
    List<Map<String, Object>> snapshot = WeightResolver.toSnapshot(resolved);
    assertThat(snapshot).hasSize(2);
    assertThat(snapshot.get(0).get("rate")).isEqualTo(25.0);
    assertThat(snapshot.get(1).get("title")).isEqualTo("基础知识");
  }
}
