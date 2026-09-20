package com.guoyu.platform.competition.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 理论组卷的确定性分配与抽题算法。
 *
 * <p>该组件不访问数据库，也不依赖请求线程上下文。预检和正式生成必须使用相同的
 * {@link #allocate(List, List, List, long)}，这样两条链路的三级配额、规则桶矩阵和
 * 候选题联合匹配口径保持一致。</p>
 */
public final class CompetitionTheoryMakeExamAllocator {

  private static final BigDecimal ONE = BigDecimal.ONE;
  private static final BigDecimal FRACTION_TOLERANCE = new BigDecimal("0.005");
  private static final BigDecimal PERCENT_TOLERANCE = new BigDecimal("0.5");

  /** 组卷明细的一行规则；index 是输入列表索引，不能用题型/排序号代替。 */
  public record RuleInput(
      int index,
      String questionType,
      int questionCount,
      BigDecimal point,
      Integer difficultyStart,
      Integer difficultyEnd,
      int sortOrder) {
  }

  /** 参与理论分配的 nodeLevel=3 三级考评范围。 */
  public record TopicInput(
      Long id,
      int nodeLevel,
      String path,
      String fullCode,
      BigDecimal weight,
      int sortOrder) {
  }

  /** 已通过外层赛事/工种/等级/题型/启用状态过滤的候选题。 */
  public record CandidateInput(
      Long questionId,
      Long topicId,
      String questionType,
      Integer difficulty) {
  }

  /** 实际选中的题目与规则桶、三级范围的归属。 */
  public record Selection(Long questionId, int ruleIndex, Long topicId, int ordinal) {
  }

  public record CellAllocation(
      int ruleIndex,
      int sortOrder,
      String questionType,
      BigDecimal point,
      Integer difficultyStart,
      Integer difficultyEnd,
      int required,
      int available,
      int assigned,
      int shortage) {
  }

  public record TopicAllocation(
      Long blueprintNodeId,
      String path,
      BigDecimal weight,
      int sortOrder,
      int questionCount,
      int available,
      int assigned,
      int shortage,
      List<CellAllocation> details) {
  }

  public record RuleAllocation(
      int ruleIndex,
      int sortOrder,
      String questionType,
      BigDecimal point,
      Integer difficultyStart,
      Integer difficultyEnd,
      int required,
      int available,
      int assigned,
      int shortage) {
  }

  public record AllocationResult(
      boolean ready,
      int totalQuestions,
      List<TopicAllocation> topics,
      List<RuleAllocation> details,
      List<Selection> selections,
      List<String> warnings,
      List<String> errors) {
  }

  /**
   * Calculates the complete theory paper allocation and tries to match every requested slot to
   * a distinct candidate question. Structural errors throw {@link IllegalArgumentException};
   * shortages are returned in the result so precheck can render precise diagnostics.
   */
  public AllocationResult allocate(
      List<RuleInput> rules,
      List<TopicInput> topics,
      List<CandidateInput> candidates,
      long randomSeed) {
    List<RuleInput> orderedRules = orderedRules(rules);
    List<TopicWeight> weightedTopics = orderedTopics(topics);
    int totalQuestions = totalQuestions(orderedRules);
    Map<Long, Integer> topicIndexById = new HashMap<>();
    for (int index = 0; index < weightedTopics.size(); index++) {
      Long id = weightedTopics.get(index).topic().id();
      if (topicIndexById.put(id, index) != null) {
        throw new IllegalArgumentException("理论三级考评范围ID不能重复：" + id);
      }
    }

    int[] topicQuotas = largestRemainderTopicQuotas(weightedTopics, totalQuestions);
    int[][] matrix = allocateMatrix(orderedRules, weightedTopics, topicQuotas);
    List<CandidateInput> uniqueCandidates = uniqueCandidates(candidates, topicIndexById);
    List<CellWork> cells = new ArrayList<>();
    Map<CellKey, CellWork> cellByKey = new HashMap<>();
    for (int rulePosition = 0; rulePosition < orderedRules.size(); rulePosition++) {
      RuleInput rule = orderedRules.get(rulePosition);
      for (int topicPosition = 0; topicPosition < weightedTopics.size(); topicPosition++) {
        int required = matrix[rulePosition][topicPosition];
        CellWork cell = new CellWork(rule, weightedTopics.get(topicPosition).topic(), topicPosition, required);
        cell.candidateIds().addAll(matchingCandidateIds(uniqueCandidates, rule, cell.topic().id()));
        cells.add(cell);
        cellByKey.put(new CellKey(rule.index(), cell.topic().id()), cell);
      }
    }

    List<SlotWork> slots = new ArrayList<>();
    for (CellWork cell : cells) {
      for (int ordinal = 1; ordinal <= cell.required(); ordinal++) {
        slots.add(new SlotWork(cell, ordinal, new ArrayList<>(cell.candidateIds())));
      }
    }
    randomizeCandidateOrder(slots, uniqueCandidates, randomSeed);
    slots.sort(Comparator
        .comparingInt((SlotWork slot) -> slot.candidateIds().size())
        .thenComparingInt(slot -> slot.cell().topicPosition())
        .thenComparingInt(slot -> slot.cell().rule().sortOrder())
        .thenComparingInt(slot -> slot.cell().rule().index())
        .thenComparingInt(SlotWork::ordinal));

    Map<Long, Integer> candidateToSlot = new HashMap<>();
    for (int slotIndex = 0; slotIndex < slots.size(); slotIndex++) {
      tryMatch(slotIndex, slots, candidateToSlot, new HashSet<>());
    }

    Map<Integer, Long> matchedCandidateBySlot = new HashMap<>();
    for (Map.Entry<Long, Integer> entry : candidateToSlot.entrySet()) {
      matchedCandidateBySlot.put(entry.getValue(), entry.getKey());
    }
    Map<CellKey, Integer> assignedByCell = new HashMap<>();
    Map<Integer, Integer> assignedByRule = new HashMap<>();
    Map<Long, Integer> assignedByTopic = new HashMap<>();
    List<Selection> selections = new ArrayList<>();
    for (int slotIndex = 0; slotIndex < slots.size(); slotIndex++) {
      Long questionId = matchedCandidateBySlot.get(slotIndex);
      if (questionId == null) continue;
      SlotWork slot = slots.get(slotIndex);
      CellWork cell = slot.cell();
      CellKey key = new CellKey(cell.rule().index(), cell.topic().id());
      assignedByCell.merge(key, 1, Integer::sum);
      assignedByRule.merge(cell.rule().index(), 1, Integer::sum);
      assignedByTopic.merge(cell.topic().id(), 1, Integer::sum);
      selections.add(new Selection(questionId, cell.rule().index(), cell.topic().id(), slot.ordinal()));
    }
    selections.sort(Comparator
        .comparingInt(Selection::ruleIndex)
        .thenComparingInt(Selection::ordinal)
        .thenComparing(Selection::questionId));

    List<String> errors = new ArrayList<>();
    List<RuleAllocation> detailAllocations = new ArrayList<>();
    for (RuleInput rule : orderedRules) {
      int available = (int) uniqueCandidates.stream()
          .filter(candidate -> matchesRule(candidate, rule))
          .count();
      int assigned = assignedByRule.getOrDefault(rule.index(), 0);
      int shortage = Math.max(0, rule.questionCount() - assigned);
      detailAllocations.add(new RuleAllocation(rule.index(), rule.sortOrder(), rule.questionType(), rule.point(),
          rule.difficultyStart(), rule.difficultyEnd(), rule.questionCount(), available, assigned, shortage));
    }

    List<TopicAllocation> topicAllocations = new ArrayList<>();
    for (int topicPosition = 0; topicPosition < weightedTopics.size(); topicPosition++) {
      TopicInput topic = weightedTopics.get(topicPosition).topic();
      List<CellAllocation> topicCells = new ArrayList<>();
      for (RuleInput rule : orderedRules) {
        CellWork cell = cellByKey.get(new CellKey(rule.index(), topic.id()));
        int required = cell == null ? 0 : cell.required();
        int available = cell == null ? 0 : cell.candidateIds().size();
        int assigned = assignedByCell.getOrDefault(new CellKey(rule.index(), topic.id()), 0);
        int shortage = Math.max(0, required - assigned);
        topicCells.add(new CellAllocation(rule.index(), rule.sortOrder(), rule.questionType(), rule.point(),
            rule.difficultyStart(), rule.difficultyEnd(), required, available, assigned, shortage));
        if (shortage > 0) errors.add(shortageMessage(topic, rule, required, available, shortage));
      }
      int assigned = assignedByTopic.getOrDefault(topic.id(), 0);
      int shortage = Math.max(0, topicQuotas[topicPosition] - assigned);
      topicAllocations.add(new TopicAllocation(topic.id(), topic.path(), weightedTopics.get(topicPosition).weight(),
          topic.sortOrder(), topicQuotas[topicPosition], topicAvailable(uniqueCandidates, topic.id()), assigned,
          shortage, topicCells));
    }

    // A rule-level shortage can be caused by overlapping cells even when no individual cell has a
    // local shortage. Report it as well, while preserving the more useful cell-level diagnostics.
    for (RuleAllocation allocation : detailAllocations) {
      if (allocation.shortage() > 0 && errors.stream().noneMatch(error -> error.contains(ruleDescription(allocation)))) {
        errors.add(ruleDescription(allocation) + "：需要" + allocation.required() + "题，可用"
            + allocation.available() + "题，缺少" + allocation.shortage() + "题");
      }
    }

    List<String> warnings = new ArrayList<>();
    for (int index = 0; index < weightedTopics.size(); index++) {
      TopicInput topic = weightedTopics.get(index).topic();
      if (weightedTopics.get(index).weight().signum() > 0 && topicQuotas[index] == 0) {
        warnings.add(topic.path() + "权重为" + percent(weightedTopics.get(index).weight())
            + "，因整卷题量较少按比例取整后自动分配0题，本次不强制补足1题");
      }
    }

    return new AllocationResult(errors.isEmpty(), totalQuestions, topicAllocations, detailAllocations,
        selections, warnings, errors);
  }

  private List<RuleInput> orderedRules(List<RuleInput> rules) {
    if (rules == null || rules.isEmpty()) throw new IllegalArgumentException("至少需要配置一条理论组卷规则");
    List<RuleInput> result = new ArrayList<>();
    Set<Integer> indexes = new HashSet<>();
    for (int index = 0; index < rules.size(); index++) {
      RuleInput rule = rules.get(index);
      if (rule == null) throw new IllegalArgumentException("理论组卷规则不能为空");
      if (rule.index() < 0 || !indexes.add(rule.index())) {
        throw new IllegalArgumentException("理论组卷规则索引必须唯一且不能为负数");
      }
      if (rule.questionType() == null || rule.questionType().isBlank()) {
        throw new IllegalArgumentException("理论组卷规则题型不能为空");
      }
      if (rule.questionCount() <= 0) throw new IllegalArgumentException("理论组卷规则题量必须为正数");
      if (rule.point() == null || rule.point().signum() <= 0) throw new IllegalArgumentException("理论组卷规则单题分值必须为正数");
      result.add(rule);
    }
    result.sort(Comparator.comparingInt(RuleInput::sortOrder).thenComparingInt(RuleInput::index));
    return result;
  }

  private List<TopicWeight> orderedTopics(List<TopicInput> topics) {
    if (topics == null || topics.isEmpty()) throw new IllegalArgumentException("细目表没有nodeLevel=3三级考评范围");
    List<TopicInput> ordered = new ArrayList<>();
    for (TopicInput topic : topics) {
      if (topic == null || topic.id() == null) throw new IllegalArgumentException("理论三级考评范围缺少ID");
      if (topic.nodeLevel() != 3) throw new IllegalArgumentException("理论组卷权重节点必须是nodeLevel=3三级考评范围");
      if (topic.weight() == null) {
        throw new IllegalArgumentException("理论三级考评范围“" + safePath(topic.path()) + "”缺少明确权重");
      }
      if (topic.weight().signum() < 0) {
        throw new IllegalArgumentException("理论三级考评范围“" + safePath(topic.path()) + "”权重不能为负数");
      }
      ordered.add(topic);
    }
    ordered.sort(Comparator.comparingInt(TopicInput::sortOrder)
        .thenComparing(topic -> safe(topic.fullCode()))
        .thenComparing(TopicInput::id));
    BigDecimal total = ordered.stream().map(TopicInput::weight).reduce(BigDecimal.ZERO, BigDecimal::add);
    boolean percentageInput = total.compareTo(new BigDecimal("1.5")) > 0;
    BigDecimal expected = percentageInput ? new BigDecimal("100") : ONE;
    BigDecimal tolerance = percentageInput ? PERCENT_TOLERANCE : FRACTION_TOLERANCE;
    if (total.subtract(expected).abs().compareTo(tolerance) > 0) {
      throw new IllegalArgumentException("理论三级考评范围权重合计" + total + "不等于100%（允许0.5%容差）");
    }
    List<TopicWeight> result = new ArrayList<>();
    for (TopicInput topic : ordered) {
      BigDecimal normalized = percentageInput ? topic.weight().movePointLeft(2) : topic.weight();
      result.add(new TopicWeight(topic, normalized));
    }
    return result;
  }

  private int totalQuestions(List<RuleInput> rules) {
    long total = 0;
    for (RuleInput rule : rules) total += rule.questionCount();
    if (total <= 0 || total > Integer.MAX_VALUE) throw new IllegalArgumentException("理论组卷总题量无效");
    return (int) total;
  }

  private int[] largestRemainderTopicQuotas(List<TopicWeight> topics, int totalQuestions) {
    int[] quotas = new int[topics.size()];
    List<BigDecimal> remainders = new ArrayList<>();
    int assigned = 0;
    for (int index = 0; index < topics.size(); index++) {
      BigDecimal exact = BigDecimal.valueOf(totalQuestions).multiply(topics.get(index).weight());
      int base = exact.setScale(0, RoundingMode.FLOOR).intValueExact();
      quotas[index] = base;
      assigned += base;
      remainders.add(exact.subtract(BigDecimal.valueOf(base)));
    }
    List<Integer> order = new ArrayList<>();
    for (int index = 0; index < topics.size(); index++) order.add(index);
    order.sort((left, right) -> {
      int remainderCompare = remainders.get(right).compareTo(remainders.get(left));
      return remainderCompare != 0 ? remainderCompare : Integer.compare(left, right);
    });
    for (int cursor = 0; cursor < totalQuestions - assigned; cursor++) quotas[order.get(cursor)]++;
    return quotas;
  }

  private int[][] allocateMatrix(List<RuleInput> rules, List<TopicWeight> topics, int[] topicQuotas) {
    int[][] matrix = new int[rules.size()][topics.size()];
    int[] rowRemaining = new int[rules.size()];
    int[] columnRemaining = new int[topics.size()];
    BigDecimal[][] remainders = new BigDecimal[rules.size()][topics.size()];
    for (int row = 0; row < rules.size(); row++) {
      int rowFloorSum = 0;
      for (int column = 0; column < topics.size(); column++) {
        BigDecimal exact = BigDecimal.valueOf(rules.get(row).questionCount()).multiply(topics.get(column).weight());
        int base = exact.setScale(0, RoundingMode.FLOOR).intValueExact();
        matrix[row][column] = base;
        rowFloorSum += base;
        remainders[row][column] = exact.subtract(BigDecimal.valueOf(base));
      }
      rowRemaining[row] = rules.get(row).questionCount() - rowFloorSum;
    }
    for (int column = 0; column < topics.size(); column++) {
      int columnFloorSum = 0;
      for (int row = 0; row < rules.size(); row++) columnFloorSum += matrix[row][column];
      columnRemaining[column] = topicQuotas[column] - columnFloorSum;
      if (columnRemaining[column] < 0) {
        throw new IllegalArgumentException("理论三级范围配额矩阵列配额无法守恒");
      }
    }
    int remaining = 0;
    for (int value : rowRemaining) remaining += value;
    int columnRemainingTotal = 0;
    for (int value : columnRemaining) columnRemainingTotal += value;
    if (remaining != columnRemainingTotal) throw new IllegalArgumentException("理论组卷配额矩阵行列总量无法守恒");
    for (int count = 0; count < remaining; count++) {
      int bestRow = -1;
      int bestColumn = -1;
      for (int row = 0; row < rules.size(); row++) {
        if (rowRemaining[row] <= 0) continue;
        for (int column = 0; column < topics.size(); column++) {
          if (columnRemaining[column] <= 0) continue;
          if (bestRow < 0 || remainders[row][column].compareTo(remainders[bestRow][bestColumn]) > 0) {
            bestRow = row;
            bestColumn = column;
          }
        }
      }
      if (bestRow < 0) throw new IllegalArgumentException("理论组卷配额矩阵无法满足行列配额");
      matrix[bestRow][bestColumn]++;
      rowRemaining[bestRow]--;
      columnRemaining[bestColumn]--;
    }
    return matrix;
  }

  private List<CandidateInput> uniqueCandidates(List<CandidateInput> candidates, Map<Long, Integer> topicIndexById) {
    Map<Long, CandidateInput> byId = new LinkedHashMap<>();
    if (candidates != null) {
      for (CandidateInput candidate : candidates) {
        if (candidate == null || candidate.questionId() == null || candidate.topicId() == null
            || !topicIndexById.containsKey(candidate.topicId()) || candidate.questionType() == null) continue;
        byId.putIfAbsent(candidate.questionId(), candidate);
      }
    }
    List<CandidateInput> result = new ArrayList<>(byId.values());
    result.sort(Comparator.comparing(CandidateInput::questionId));
    return result;
  }

  private List<Long> matchingCandidateIds(List<CandidateInput> candidates, RuleInput rule, Long topicId) {
    List<Long> result = new ArrayList<>();
    for (CandidateInput candidate : candidates) {
      if (topicId.equals(candidate.topicId()) && matchesRule(candidate, rule)) result.add(candidate.questionId());
    }
    return result;
  }

  private boolean matchesRule(CandidateInput candidate, RuleInput rule) {
    if (!rule.questionType().equalsIgnoreCase(candidate.questionType())) return false;
    Integer difficulty = candidate.difficulty();
    return (rule.difficultyStart() == null || difficulty != null && difficulty >= rule.difficultyStart())
        && (rule.difficultyEnd() == null || difficulty != null && difficulty <= rule.difficultyEnd());
  }

  private int topicAvailable(List<CandidateInput> candidates, Long topicId) {
    return (int) candidates.stream().filter(candidate -> topicId.equals(candidate.topicId())).count();
  }

  private void randomizeCandidateOrder(List<SlotWork> slots, List<CandidateInput> candidates, long seed) {
    List<Long> ids = new ArrayList<>(candidates.stream().map(CandidateInput::questionId).toList());
    Collections.shuffle(ids, new java.util.Random(seed));
    Map<Long, Integer> rank = new HashMap<>();
    for (int index = 0; index < ids.size(); index++) rank.put(ids.get(index), index);
    for (SlotWork slot : slots) slot.candidateIds().sort(Comparator.comparingInt(rank::get));
  }

  private boolean tryMatch(
      int slotIndex,
      List<SlotWork> slots,
      Map<Long, Integer> candidateToSlot,
      Set<Long> visitedCandidates) {
    for (Long candidateId : slots.get(slotIndex).candidateIds()) {
      if (!visitedCandidates.add(candidateId)) continue;
      Integer occupiedSlot = candidateToSlot.get(candidateId);
      if (occupiedSlot == null || tryMatch(occupiedSlot, slots, candidateToSlot, visitedCandidates)) {
        candidateToSlot.put(candidateId, slotIndex);
        return true;
      }
    }
    return false;
  }

  private String shortageMessage(TopicInput topic, RuleInput rule, int required, int available, int shortage) {
    return safePath(topic.path()) + " / " + typeLabel(rule.questionType()) + " / "
        + difficultyLabel(rule) + "：需要" + required + "题，可用" + available + "题，缺少" + shortage + "题";
  }

  private String ruleDescription(RuleAllocation allocation) {
    return "组卷规则" + allocation.sortOrder() + " / " + typeLabel(allocation.questionType()) + " / "
        + difficultyLabel(allocation.difficultyStart(), allocation.difficultyEnd());
  }

  private String difficultyLabel(RuleInput rule) {
    return difficultyLabel(rule.difficultyStart(), rule.difficultyEnd());
  }

  private String difficultyLabel(Integer start, Integer end) {
    if (start == null && end == null) return "不限难度";
    if (start != null && end != null) return "难度" + start + "-" + end;
    if (start != null) return "难度" + start + "以上";
    return "难度" + end + "以下";
  }

  private String typeLabel(String type) {
    if (type == null) return "未知题型";
    return switch (type.toLowerCase(Locale.ROOT)) {
      case "single" -> "单选题";
      case "multi" -> "多选题";
      case "judge" -> "判断题";
      case "fill" -> "填空题";
      default -> type;
    };
  }

  private String percent(BigDecimal value) {
    return value.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString() + "%";
  }

  private String safePath(String path) {
    return path == null || path.isBlank() ? "未命名三级考评范围" : path;
  }

  private String safe(String value) {
    return value == null ? "" : value;
  }

  private record TopicWeight(TopicInput topic, BigDecimal weight) {
  }

  private record CellKey(int ruleIndex, Long topicId) {
  }

  private static final class CellWork {
    private final RuleInput rule;
    private final TopicInput topic;
    private final int topicPosition;
    private final int required;
    private final List<Long> candidateIds = new ArrayList<>();

    private CellWork(RuleInput rule, TopicInput topic, int topicPosition, int required) {
      this.rule = rule;
      this.topic = topic;
      this.topicPosition = topicPosition;
      this.required = required;
    }

    private RuleInput rule() { return rule; }
    private TopicInput topic() { return topic; }
    private int topicPosition() { return topicPosition; }
    private int required() { return required; }
    private List<Long> candidateIds() { return candidateIds; }
  }

  private record SlotWork(CellWork cell, int ordinal, List<Long> candidateIds) {
  }
}
