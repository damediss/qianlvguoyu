package com.guoyu.platform.competition.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

/**
 * 竞赛实操自动组卷的唯一规则入口。
 *
 * <p>该类只接收已经通过赛事范围、启用状态和租户过滤的输入，不访问数据库，也不依赖
 * {@code SecurityContext}。预检、结构预览、正式生成和多套试卷生成都必须调用同一个
 * {@link #allocate(AllocationRequest)}，从而保证选择组、分值、去重和随机种子的口径一致。</p>
 */
public final class CompetitionPracticalMakeExamAllocator {

  private static final BigDecimal ONE = BigDecimal.ONE;
  private static final BigDecimal WEIGHT_TOLERANCE = new BigDecimal("0.0005");
  private static final BigDecimal SCORE_ZERO = BigDecimal.ZERO.setScale(2);
  private static final long GOLDEN_RATIO = 0x9E3779B97F4A7C15L;

  /** 归一化后的结构节点输入；nodeLevel 只用于展示，算法不依赖固定层级。 */
  public record NodeInput(
      Long id,
      Long parentId,
      Integer nodeLevel,
      String code,
      String fullCode,
      String name,
      BigDecimal weight,
      String selectMode,
      Integer sortOrder) {
  }

  /** 当前蓝图版本中的题目-末级考评点绑定。 */
  public record BindingInput(
      Long questionId,
      Long blueprintId,
      Long blueprintNodeId,
      boolean active) {

    public BindingInput(Long questionId, Long blueprintId, Long blueprintNodeId) {
      this(questionId, blueprintId, blueprintNodeId, true);
    }
  }

  /** 已通过外层题库查询过滤的实操题候选。 */
  public record CandidateInput(
      Long questionId,
      BigDecimal sourceTotalScore,
      Integer durationMinutes,
      boolean enabled) {

    public CandidateInput(Long questionId, BigDecimal sourceTotalScore, Integer durationMinutes) {
      this(questionId, sourceTotalScore, durationMinutes, true);
    }
  }

  /** 分配请求。previouslySelectedQuestionIds 用于多套试卷优先跨套去重。 */
  public record AllocationRequest(
      Long blueprintId,
      List<NodeInput> nodes,
      List<BindingInput> bindings,
      List<CandidateInput> candidates,
      BigDecimal totalScore,
      Integer paperSequence,
      long randomSeed,
      Integer plannedDurationMinutes,
      Set<Long> previouslySelectedQuestionIds) {

    public AllocationRequest(
        List<NodeInput> nodes,
        List<BindingInput> bindings,
        List<CandidateInput> candidates,
        BigDecimal totalScore,
        int paperSequence,
        long randomSeed) {
      this(null, nodes, bindings, candidates, totalScore, paperSequence, randomSeed, null, Set.of());
    }

    public AllocationRequest(
        Long blueprintId,
        List<NodeInput> nodes,
        List<BindingInput> bindings,
        List<CandidateInput> candidates,
        BigDecimal totalScore,
        int paperSequence,
        long randomSeed) {
      this(blueprintId, nodes, bindings, candidates, totalScore, paperSequence, randomSeed, null, Set.of());
    }

    public AllocationRequest withPrevious(Set<Long> previous) {
      return new AllocationRequest(blueprintId, nodes, bindings, candidates, totalScore, paperSequence,
          randomSeed, plannedDurationMinutes, previous == null ? Set.of() : previous);
    }

    public AllocationRequest withPlannedDuration(Integer durationMinutes) {
      return new AllocationRequest(blueprintId, nodes, bindings, candidates, totalScore, paperSequence,
          randomSeed, durationMinutes, previouslySelectedQuestionIds);
    }
  }

  /** 供前端和审计使用的候选结构预览。 */
  public record CandidateNode(
      Long blueprintNodeId,
      String path,
      BigDecimal scoreWeight,
      List<Long> assessmentPointIds,
      int candidateQuestionCount,
      List<Long> candidateQuestionIds,
      boolean selected) {
  }

  /** 一条最终实操单元；一个 REQUIRED 节点或一个 CHOOSE_ONE 选择组对应一条。 */
  public record UnitAllocation(
      String selectionGroup,
      String selectionMode,
      Long structureNodeId,
      String structurePath,
      BigDecimal scoreWeight,
      BigDecimal targetScore,
      int candidateQuestionCount,
      List<CandidateNode> candidateNodes,
      Long selectedQuestionId,
      Long selectedAssessmentPointId,
      BigDecimal sourceTotalScore,
      BigDecimal scoreScale,
      Integer referenceDurationMinutes) {
  }

  public record AllocationResult(
      boolean ready,
      int questionCount,
      BigDecimal totalScore,
      int referenceDurationMinutes,
      List<UnitAllocation> units,
      List<String> warnings,
      List<String> errors) {

    public List<Long> selectedQuestionIds() {
      return units.stream().map(UnitAllocation::selectedQuestionId)
          .filter(java.util.Objects::nonNull).toList();
    }
  }

  /**
   * 对任意深度的蓝图完成结构归一化、选择组归并、候选匹配和整分值分配。
   */
  public AllocationResult allocate(AllocationRequest request) {
    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();
    if (request == null) {
      return failed("实操自动组卷请求不能为空");
    }
    if (request.totalScore() == null || request.totalScore().signum() <= 0) {
      errors.add("实操整卷总分必须为正数");
    } else {
      validateScoreScale(request.totalScore(), "实操整卷总分", errors);
    }
    if (request.paperSequence() == null || request.paperSequence() <= 0) {
      errors.add("实操套卷序号必须为正数");
    }

    Tree tree = buildTree(request.nodes(), errors);
    List<Structure> structures = tree == null ? List.of() : normalizeStructures(tree, errors);
    Map<Long, List<BindingInput>> bindingsByQuestion = bindingsByQuestion(request, tree, warnings);
    Map<Long, CandidateInput> candidatesById = uniqueCandidates(request.candidates(), warnings);
    Map<Long, List<QuestionChoice>> choicesByStructure = buildChoices(
        structures, bindingsByQuestion, candidatesById, tree, warnings);

    List<GroupPlan> groups = buildGroups(structures, choicesByStructure, request, errors);
    if (groups.isEmpty() && errors.isEmpty()) {
      errors.add("实操细目表没有可参与组卷的结构节点");
    }
    if (!errors.isEmpty()) {
      return resultWithPreview(request, groups, choicesByStructure, errors, warnings);
    }

    Set<Long> previous = request.previouslySelectedQuestionIds() == null
        ? Set.of() : new HashSet<>(request.previouslySelectedQuestionIds());
    long selectionSeed = mix(request.randomSeed(), "paper:" + request.paperSequence());
    MatchOutcome match = chooseGroupsAndMatch(groups, previous, selectionSeed, true);
    if (match == null) {
      match = chooseGroupsAndMatch(groups, previous, selectionSeed, false);
      if (match != null && !previous.isEmpty()) {
        warnings.add("题库不足以在套卷之间完全去重，本套卷已按确定性规则复用历史题目");
      }
    }
    if (match == null) {
      errors.add(unmatchedMessage(groups, previous, selectionSeed));
      return resultWithPreview(request, groups, choicesByStructure, errors, warnings);
    }

    List<BigDecimal> weights = groups.stream().map(GroupPlan::scoreWeight).toList();
    List<BigDecimal> targetScores = allocateScores(request.totalScore(), weights);
    List<UnitAllocation> units = new ArrayList<>();
    int referenceDuration = 0;
    for (int index = 0; index < groups.size(); index++) {
      GroupPlan group = match.groups().get(index);
      QuestionChoice choice = match.choiceByGroup().get(group.selectionGroup());
      BigDecimal targetScore = targetScores.get(index);
      BigDecimal sourceTotal = choice == null ? null : choice.sourceTotalScore();
      BigDecimal scale = sourceTotal == null || sourceTotal.signum() <= 0
          ? null : targetScore.divide(sourceTotal, 8, RoundingMode.HALF_UP);
      Integer duration = choice == null ? null : choice.durationMinutes();
      if (duration != null) {
        try {
          referenceDuration = Math.addExact(referenceDuration, duration);
        } catch (ArithmeticException ex) {
          errors.add("实操题参考时长合计溢出");
        }
      }
      units.add(toUnitAllocation(group, choice, targetScore));
    }
    if (request.plannedDurationMinutes() != null
        && referenceDuration > request.plannedDurationMinutes()) {
      warnings.add("所选实操题参考时长合计" + referenceDuration + "分钟，超过计划时长"
          + request.plannedDurationMinutes() + "分钟");
    }
    if (targetScores.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
        .compareTo(request.totalScore().setScale(2, RoundingMode.UNNECESSARY)) != 0) {
      errors.add("实操单元分值分配未能守恒到计划总分");
    }
    return new AllocationResult(errors.isEmpty(), groups.size(), request.totalScore(), referenceDuration,
        units, warnings, errors);
  }

  private AllocationResult failed(String error) {
    return new AllocationResult(false, 0, null, 0, List.of(), List.of(), List.of(error));
  }

  private void validateScoreScale(BigDecimal score, String label, List<String> errors) {
    try {
      score.setScale(2, RoundingMode.UNNECESSARY);
    } catch (ArithmeticException ex) {
      errors.add(label + "最多保留两位小数");
    }
  }

  private Tree buildTree(List<NodeInput> inputs, List<String> errors) {
    if (inputs == null || inputs.isEmpty()) {
      errors.add("实操细目表没有节点");
      return null;
    }
    Map<Long, Node> byId = new LinkedHashMap<>();
    for (NodeInput input : inputs) {
      if (input == null || input.id() == null) {
        errors.add("实操细目表节点缺少ID");
        continue;
      }
      if (byId.containsKey(input.id())) {
        errors.add("实操细目表节点ID重复：" + input.id());
        continue;
      }
      if (input.name() == null || input.name().isBlank()) {
        errors.add("实操细目表节点" + input.id() + "缺少名称");
      }
      if (input.weight() != null && input.weight().signum() < 0) {
        errors.add("实操结构“" + safe(input.name()) + "”权重不能为负数");
      }
      byId.put(input.id(), new Node(input));
    }
    Map<Long, List<Node>> children = new HashMap<>();
    List<Node> roots = new ArrayList<>();
    for (Node node : byId.values()) {
      Long parentId = zeroAsNull(node.input.parentId());
      if (parentId == null) {
        roots.add(node);
      } else {
        Node parent = byId.get(parentId);
        if (parent == null) {
          errors.add("实操节点“" + safe(node.input.name()) + "”的父节点不存在");
        } else {
          node.parent = parent;
          children.computeIfAbsent(parent.id(), ignored -> new ArrayList<>()).add(node);
        }
      }
    }
    if (roots.isEmpty()) errors.add("实操细目表没有根节点");
    Comparator<Node> order = nodeComparator();
    roots.sort(order);
    for (List<Node> rows : children.values()) rows.sort(order);
    for (Node node : byId.values()) node.children.addAll(children.getOrDefault(node.id(), List.of()));
    Set<Long> visiting = new HashSet<>();
    Set<Long> visited = new HashSet<>();
    for (Node node : byId.values()) {
      if (!visited.contains(node.id())) detectCycle(node, visiting, visited, errors);
    }
    return new Tree(byId, roots);
  }

  private void detectCycle(Node node, Set<Long> visiting, Set<Long> visited, List<String> errors) {
    if (visited.contains(node.id())) return;
    if (!visiting.add(node.id())) {
      errors.add("实操细目表层级存在循环引用：" + node.id());
      return;
    }
    for (Node child : node.children) detectCycle(child, visiting, visited, errors);
    visiting.remove(node.id());
    visited.add(node.id());
  }

  private List<Structure> normalizeStructures(Tree tree, List<String> errors) {
    // Validate every explicitly populated mode before frontier discovery.  A
    // mode on a non-frontier ancestor can be intentionally superseded by
    // deeper modes (the standard workbook does this), but an unknown value is
    // still a malformed participating-structure declaration and must not be
    // silently treated as REQUIRED.
    for (Node node : tree.byId.values()) {
      if (node.input.selectMode() != null && !node.input.selectMode().isBlank()
          && parseSelectionMode(node.input.selectMode()) == null) {
        errors.add("实操参与组卷结构“" + path(node) + "”的选考方式“"
            + node.input.selectMode() + "”无法识别，请明确填写必考、全部必考、任选一项或选考");
      }
    }
    Map<Long, Boolean> explicitDescendant = new HashMap<>();
    for (Node root : tree.roots) hasExplicitDescendant(root, explicitDescendant);
    List<Node> frontiers = new ArrayList<>();
    for (Node root : tree.roots) collectFrontier(root, explicitDescendant, frontiers, errors);
    frontiers.sort(nodeComparator());
    List<Structure> result = new ArrayList<>();
    for (Node node : frontiers) {
      SelectionMode mode = parseSelectionMode(node.input.selectMode());
      if (mode == null) {
        String text = node.input.selectMode() == null || node.input.selectMode().isBlank()
            ? "空白" : "“" + node.input.selectMode() + "”";
        errors.add("实操参与组卷结构“" + path(node) + "”的选考方式" + text
            + "无法识别，请明确填写必考、全部必考、任选一项或选考");
      }
      List<Node> leaves = leaves(node);
      if (leaves.isEmpty()) {
        errors.add("实操结构“" + path(node) + "”没有有效末级考评点");
      }
      result.add(new Structure(node, mode, path(node), leaves, pathWeight(node)));
    }
    return result;
  }

  private boolean hasExplicitDescendant(Node node, Map<Long, Boolean> memo) {
    if (!node.children.isEmpty()) {
      boolean value = false;
      for (Node child : node.children) {
        SelectionMode childMode = parseSelectionMode(child.input.selectMode());
        boolean childHasExplicit = childMode != null || hasExplicitDescendant(child, memo);
        value = value || childHasExplicit;
      }
      memo.put(node.id(), value);
      return value;
    }
    memo.put(node.id(), false);
    return false;
  }

  private void collectFrontier(Node node, Map<Long, Boolean> explicitDescendant,
      List<Node> result, List<String> errors) {
    if (node.children.isEmpty()) {
      // A leaf can itself be a selectable structure.  This is useful for
      // custom blueprints whose assessment point is also the business unit.
      if (parseSelectionMode(node.input.selectMode()) != null) result.add(node);
      return;
    }
    if (Boolean.TRUE.equals(explicitDescendant.get(node.id()))) {
      for (Node child : node.children) {
        if (!child.children.isEmpty()) {
          collectFrontier(child, explicitDescendant, result, errors);
        } else if (parseSelectionMode(child.input.selectMode()) != null) {
          result.add(child);
        }
      }
      return;
    }
    result.add(node);
  }

  private List<Node> leaves(Node node) {
    List<Node> result = new ArrayList<>();
    collectLeaves(node, result);
    result.sort(nodeComparator());
    return result;
  }

  private void collectLeaves(Node node, List<Node> result) {
    if (node.children.isEmpty()) {
      result.add(node);
      return;
    }
    for (Node child : node.children) collectLeaves(child, result);
  }

  private BigDecimal pathWeight(Node node) {
    BigDecimal result = ONE;
    Set<Long> seen = new HashSet<>();
    Node current = node;
    while (current != null && seen.add(current.id())) {
      BigDecimal value = normalizeWeight(current.input.weight());
      if (value != null) result = result.multiply(value);
      current = current.parent;
    }
    return result;
  }

  private Map<Long, List<BindingInput>> bindingsByQuestion(AllocationRequest request, Tree tree,
      List<String> warnings) {
    Map<Long, List<BindingInput>> result = new HashMap<>();
    if (request.bindings() == null) return result;
    for (BindingInput binding : request.bindings()) {
      if (binding == null || binding.questionId() == null || binding.blueprintNodeId() == null
          || !binding.active() || request.blueprintId() != null
          && !request.blueprintId().equals(binding.blueprintId())) continue;
      if (tree == null || !tree.byId.containsKey(binding.blueprintNodeId())
          || !tree.byId.get(binding.blueprintNodeId()).children.isEmpty()) {
        warnings.add("题目" + binding.questionId() + "绑定的不是当前细目表有效末级考评点，已排除");
        continue;
      }
      result.computeIfAbsent(binding.questionId(), ignored -> new ArrayList<>()).add(binding);
    }
    for (List<BindingInput> rows : result.values()) {
      rows.sort(Comparator.comparing(BindingInput::blueprintNodeId));
    }
    return result;
  }

  private Map<Long, CandidateInput> uniqueCandidates(List<CandidateInput> candidates,
      List<String> warnings) {
    Map<Long, CandidateInput> result = new TreeMap<>();
    if (candidates == null) return result;
    for (CandidateInput candidate : candidates) {
      if (candidate == null || candidate.questionId() == null || !candidate.enabled()) continue;
      if (result.putIfAbsent(candidate.questionId(), candidate) != null) {
        warnings.add("题目" + candidate.questionId() + "存在重复候选记录，已去重");
      }
    }
    return result;
  }

  private Map<Long, List<QuestionChoice>> buildChoices(List<Structure> structures,
      Map<Long, List<BindingInput>> bindingsByQuestion, Map<Long, CandidateInput> candidatesById,
      Tree tree, List<String> warnings) {
    Map<Long, List<QuestionChoice>> result = new HashMap<>();
    for (Structure structure : structures) {
      Set<Long> leafIds = structure.leaves.stream().map(Node::id).collect(java.util.stream.Collectors.toSet());
      List<QuestionChoice> choices = new ArrayList<>();
      for (Map.Entry<Long, List<BindingInput>> entry : bindingsByQuestion.entrySet()) {
        CandidateInput candidate = candidatesById.get(entry.getKey());
        if (candidate == null) continue;
        for (BindingInput binding : entry.getValue()) {
          if (!leafIds.contains(binding.blueprintNodeId())) continue;
          if (candidate.sourceTotalScore() == null || candidate.sourceTotalScore().signum() <= 0) {
            warnings.add("实操题" + candidate.questionId() + "原始总分为空或不为正，已排除");
            break;
          }
          if (candidate.durationMinutes() == null || candidate.durationMinutes() <= 0) {
            warnings.add("实操题" + candidate.questionId() + "参考时长为空或不为正，已排除");
            break;
          }
          choices.add(new QuestionChoice(candidate.questionId(), binding.blueprintNodeId(),
              structure.node.id(), candidate.sourceTotalScore(), candidate.durationMinutes()));
          break;
        }
      }
      choices.sort(Comparator.comparing(QuestionChoice::questionId)
          .thenComparing(QuestionChoice::assessmentPointId));
      List<QuestionChoice> unique = uniqueChoices(choices);
      structure.choices = unique;
      result.put(structure.node.id(), unique);
    }
    return result;
  }

  private List<QuestionChoice> uniqueChoices(List<QuestionChoice> choices) {
    Map<Long, QuestionChoice> byQuestion = new LinkedHashMap<>();
    for (QuestionChoice choice : choices) byQuestion.putIfAbsent(choice.questionId(), choice);
    return new ArrayList<>(byQuestion.values());
  }

  private List<GroupPlan> buildGroups(List<Structure> structures,
      Map<Long, List<QuestionChoice>> choicesByStructure, AllocationRequest request,
      List<String> errors) {
    Map<String, List<Structure>> chooseGroups = new LinkedHashMap<>();
    List<GroupPlan> result = new ArrayList<>();
    for (Structure structure : structures) {
      if (structure.mode == SelectionMode.REQUIRED) {
        // 标准竞赛实操细目表会同时保存上级业务范围的汇总权重，以及
        // 当前必考考评范围自身的整卷绝对权重。必考单元必须直接使用
        // 当前节点权重，不能再与父级汇总权重相乘，否则 8% 会被错误
        // 计算成“上级占比 × 8%”。
        BigDecimal requiredWeight = declaredWeight(structure.node);
        result.add(new GroupPlan("REQUIRED:" + structure.node.id(), "REQUIRED", structure.node.id(),
            structure.path, requiredWeight, List.of(structure), choicesByStructure
                .getOrDefault(structure.node.id(), List.of())));
      } else if (structure.mode == SelectionMode.CHOOSE_ONE) {
        Long parentId = structure.node.parent == null ? 0L : structure.node.parent.id();
        chooseGroups.computeIfAbsent("CHOOSE_ONE:" + parentId, ignored -> new ArrayList<>()).add(structure);
      }
    }
    for (Map.Entry<String, List<Structure>> entry : chooseGroups.entrySet()) {
      List<Structure> candidates = entry.getValue();
      candidates.sort(Comparator.comparing(structure -> structure.path));
      Node groupParent = candidates.get(0).node.parent;
      // “一级必考、二级任选一项”表示一级范围只产生一个题位，二级只是
      // 该题位的候选范围。选择组使用父级必考范围的整卷权重，不能把
      // 2/3 个二级候选的相同权重相加，否则 A=10% 会被算成 20%。
      BigDecimal groupWeight = groupParent != null
          && parseSelectionMode(groupParent.input.selectMode()) == SelectionMode.REQUIRED
          ? declaredWeight(groupParent) : BigDecimal.ZERO;
      if (groupWeight.signum() <= 0) {
        groupWeight = candidates.stream().map(structure -> structure.pathWeight)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
      }
      if (groupWeight.signum() <= 0) {
        errors.add("实操选择组“" + entry.getKey() + "”的有效权重必须为正数");
      }
      result.add(new GroupPlan(entry.getKey(), "CHOOSE_ONE", groupParent == null
          ? null : groupParent.id(), groupParent == null ? candidates.get(0).path : path(groupParent),
          groupWeight, candidates,
          List.of()));
    }
    result.sort(Comparator.comparing(GroupPlan::selectionGroup));
    for (GroupPlan group : result) {
      if (group.scoreWeight.signum() <= 0) {
        errors.add("实操最终组卷单元“" + group.structurePath + "”的有效权重必须为正数");
      }
    }
    BigDecimal total = result.stream().map(GroupPlan::scoreWeight).reduce(BigDecimal.ZERO, BigDecimal::add);
    if (total.signum() <= 0 || total.subtract(ONE).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
      errors.add("实操最终选择单元有效权重合计" + total + "，必须等于100%");
    }
    if (total.signum() > 0) {
      for (GroupPlan group : result) group.scoreWeight = group.scoreWeight.divide(total, 12, RoundingMode.HALF_UP);
    }
    return result;
  }

  private MatchOutcome chooseGroupsAndMatch(List<GroupPlan> groups, Set<Long> previous,
      long seed, boolean avoidPrevious) {
    Map<String, Structure> chosen = new LinkedHashMap<>();
    MatchOutcome outcome = chooseGroupAt(groups, 0, chosen, previous, seed, avoidPrevious);
    return outcome;
  }

  private MatchOutcome chooseGroupAt(List<GroupPlan> groups, int index, Map<String, Structure> chosen,
      Set<Long> previous, long seed, boolean avoidPrevious) {
    if (index >= groups.size()) {
      List<GroupPlan> effective = new ArrayList<>();
      for (GroupPlan group : groups) {
        if ("REQUIRED".equals(group.selectionMode)) {
          effective.add(group.withChosen(group.candidates.get(0), group.candidates.get(0).choices));
        } else {
          Structure selected = chosen.get(group.selectionGroup);
          if (selected == null) return null;
          effective.add(group.withChosen(selected, selected.choices));
        }
      }
      return match(effective, previous, seed, avoidPrevious);
    }
    GroupPlan group = groups.get(index);
    if ("REQUIRED".equals(group.selectionMode)) {
      return chooseGroupAt(groups, index + 1, chosen, previous, seed, avoidPrevious);
    }
    List<Structure> options = new ArrayList<>(group.candidates);
    options.sort(Comparator.comparing(structure -> structure.path));
    Random random = new Random(mix(seed, group.selectionGroup));
    java.util.Collections.shuffle(options, random);
    for (Structure option : options) {
      if (availableChoices(option.choices, previous, avoidPrevious).isEmpty()
          && !availableChoices(option.choices, Set.of(), false).isEmpty()) continue;
      chosen.put(group.selectionGroup, option);
      MatchOutcome outcome = chooseGroupAt(groups, index + 1, chosen, previous, seed, avoidPrevious);
      if (outcome != null) return outcome;
    }
    chosen.remove(group.selectionGroup);
    return null;
  }

  private MatchOutcome match(List<GroupPlan> groups, Set<Long> previous, long seed,
      boolean avoidPrevious) {
    List<Slot> slots = new ArrayList<>();
    for (GroupPlan group : groups) {
      List<QuestionChoice> options = availableChoices(group.choices, previous, avoidPrevious);
      if (options.isEmpty()) return null;
      options.sort(Comparator.comparing(QuestionChoice::questionId)
          .thenComparing(QuestionChoice::assessmentPointId));
      java.util.Collections.shuffle(options, new Random(mix(seed, group.selectionGroup)));
      slots.add(new Slot(group, options));
    }
    slots.sort(Comparator.comparingInt((Slot slot) -> slot.choices.size())
        .thenComparing(slot -> slot.group.selectionGroup));
    Map<Long, Integer> questionOwner = new HashMap<>();
    Map<Integer, QuestionChoice> chosenBySlot = new HashMap<>();
    for (int index = 0; index < slots.size(); index++) {
      if (!matchSlot(index, slots, questionOwner, chosenBySlot, new HashSet<>())) return null;
    }
    Map<String, QuestionChoice> selected = new HashMap<>();
    Map<String, GroupPlan> byGroup = new HashMap<>();
    for (int index = 0; index < slots.size(); index++) {
      GroupPlan group = slots.get(index).group;
      byGroup.put(group.selectionGroup, group);
      selected.put(group.selectionGroup, chosenBySlot.get(index));
    }
    List<GroupPlan> ordered = new ArrayList<>();
    for (GroupPlan group : groups) ordered.add(byGroup.get(group.selectionGroup));
    return new MatchOutcome(ordered, selected);
  }

  private boolean matchSlot(int slotIndex, List<Slot> slots, Map<Long, Integer> questionOwner,
      Map<Integer, QuestionChoice> chosenBySlot, Set<Long> visitedQuestions) {
    Slot slot = slots.get(slotIndex);
    for (QuestionChoice choice : slot.choices) {
      if (!visitedQuestions.add(choice.questionId())) continue;
      Integer owner = questionOwner.get(choice.questionId());
      if (owner == null || matchSlot(owner, slots, questionOwner, chosenBySlot, visitedQuestions)) {
        questionOwner.put(choice.questionId(), slotIndex);
        chosenBySlot.put(slotIndex, choice);
        return true;
      }
    }
    return false;
  }

  private List<QuestionChoice> availableChoices(List<QuestionChoice> choices, Set<Long> previous,
      boolean avoidPrevious) {
    if (choices == null) return List.of();
    List<QuestionChoice> result = new ArrayList<>();
    for (QuestionChoice choice : choices) {
      if (!avoidPrevious || previous == null || !previous.contains(choice.questionId())) result.add(choice);
    }
    return result;
  }

  private String unmatchedMessage(List<GroupPlan> groups, Set<Long> previous, long seed) {
    StringBuilder result = new StringBuilder("实操题库无法为每个最终组卷单元匹配一条完整且不重复的实操题：");
    boolean first = true;
    for (GroupPlan group : groups) {
      if (!first) result.append("；");
      first = false;
      int all = uniqueChoices(group.choices).size();
      int fresh = availableChoices(group.choices, previous, true).size();
      result.append(group.structurePath).append("需要1题，可用").append(all).append("题");
      if (!previous.isEmpty() && fresh < all) result.append("，跨套未复用可用").append(fresh).append("题");
    }
    return result.toString();
  }

  private List<BigDecimal> allocateScores(BigDecimal totalScore, List<BigDecimal> weights) {
    long cents = totalScore.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact();
    List<Long> base = new ArrayList<>();
    List<BigDecimal> remainders = new ArrayList<>();
    long assigned = 0;
    for (BigDecimal weight : weights) {
      BigDecimal exact = BigDecimal.valueOf(cents).multiply(weight);
      long floor = exact.setScale(0, RoundingMode.FLOOR).longValueExact();
      base.add(floor);
      remainders.add(exact.subtract(BigDecimal.valueOf(floor)));
      assigned += floor;
    }
    List<Integer> order = new ArrayList<>();
    for (int index = 0; index < weights.size(); index++) order.add(index);
    order.sort((left, right) -> {
      int compare = remainders.get(right).compareTo(remainders.get(left));
      return compare != 0 ? compare : Integer.compare(left, right);
    });
    for (long cursor = 0; cursor < cents - assigned; cursor++) {
      int index = order.get(Math.toIntExact(cursor));
      base.set(index, base.get(index) + 1);
    }
    List<BigDecimal> result = new ArrayList<>();
    for (Long value : base) result.add(BigDecimal.valueOf(value, 2));
    return result;
  }

  private AllocationResult resultWithPreview(AllocationRequest request, List<GroupPlan> groups,
      Map<Long, List<QuestionChoice>> choicesByStructure, List<String> errors, List<String> warnings) {
    List<UnitAllocation> units = new ArrayList<>();
    for (GroupPlan group : groups) {
      Structure selected = "REQUIRED".equals(group.selectionMode) ? group.candidates.get(0) : null;
      List<CandidateNode> nodes = new ArrayList<>();
      for (Structure candidate : group.candidates) {
        List<QuestionChoice> choices = choicesByStructure.getOrDefault(candidate.node.id(), List.of());
        nodes.add(candidateNode(candidate, choices, group.scoreWeight,
            selected != null && selected.node.id().equals(candidate.node.id())));
      }
      units.add(new UnitAllocation(group.selectionGroup, group.selectionMode,
          selected == null ? group.structureNodeId : selected.node.id(),
          selected == null ? group.structurePath : selected.path,
          group.scoreWeight, null, selected == null ? 0 : selected.choices.size(), nodes,
          null, null, null, null, null));
    }
    return new AllocationResult(false, groups.size(), request.totalScore(), 0, units, warnings, errors);
  }

  private UnitAllocation toUnitAllocation(GroupPlan group, QuestionChoice selected,
      BigDecimal targetScore) {
    List<CandidateNode> nodes = new ArrayList<>();
    for (Structure candidate : group.candidates) {
      nodes.add(candidateNode(candidate, candidate.choices, group.scoreWeight,
          selected != null && candidate.node.id().equals(selected.structureId())));
    }
    return new UnitAllocation(group.selectionGroup, group.selectionMode,
        group.chosenNode == null ? group.structureNodeId : group.chosenNode.node.id(),
        group.chosenNode == null ? group.structurePath : group.chosenNode.path,
        group.scoreWeight, targetScore, selected == null ? 0 : group.chosenNode.choices.size(), nodes,
        selected == null ? null : selected.questionId(), selected == null ? null : selected.assessmentPointId(),
        selected == null ? null : selected.sourceTotalScore(), selected == null ? null
            : targetScore.divide(selected.sourceTotalScore(), 8, RoundingMode.HALF_UP),
        selected == null ? null : selected.durationMinutes());
  }

  private CandidateNode candidateNode(Structure candidate, List<QuestionChoice> choices,
      BigDecimal groupWeight, boolean selected) {
    List<Long> pointIds = candidate.leaves.stream().map(Node::id).sorted().toList();
    List<Long> questionIds = choices.stream().map(QuestionChoice::questionId).distinct().sorted().toList();
    return new CandidateNode(candidate.node.id(), candidate.path, groupWeight, pointIds,
        questionIds.size(), questionIds, selected);
  }

  private SelectionMode parseSelectionMode(String value) {
    if (value == null) return null;
    String normalized = value.replace(" ", "").replace("　", "")
        .replace("（", "(").replace("）", ")").trim().toLowerCase(Locale.ROOT);
    if (normalized.isBlank()) return null;
    if (Set.of("必考", "全部必考", "required", "require", "mandatory", "必选", "全部").contains(normalized)) {
      return SelectionMode.REQUIRED;
    }
    if (Set.of("任选一项", "任选1项", "任一", "任选", "选考", "optional", "chooseone",
        "choose_one", "choose-one", "任意", "任意一项").contains(normalized)) {
      return SelectionMode.CHOOSE_ONE;
    }
    return null;
  }

  private BigDecimal normalizeWeight(BigDecimal value) {
    if (value == null) return ONE;
    if (value.compareTo(ONE) > 0 && value.compareTo(new BigDecimal("100")) <= 0) {
      return value.movePointLeft(2);
    }
    return value;
  }

  private BigDecimal declaredWeight(Node node) {
    if (node == null || node.input.weight() == null) return BigDecimal.ZERO;
    return normalizeWeight(node.input.weight());
  }

  private Comparator<Node> nodeComparator() {
    return Comparator.comparingInt((Node node) -> node.input.sortOrder() == null
            ? Integer.MAX_VALUE : node.input.sortOrder())
        .thenComparing(node -> safe(node.input.fullCode()))
        .thenComparing(node -> safe(node.input.code()))
        .thenComparing(Node::id);
  }

  private String path(Node node) {
    List<String> parts = new ArrayList<>();
    Set<Long> seen = new HashSet<>();
    Node current = node;
    while (current != null && seen.add(current.id())) {
      parts.add(safe(current.input.name()));
      current = current.parent;
    }
    java.util.Collections.reverse(parts);
    return String.join(" / ", parts);
  }

  private String safe(String value) {
    return value == null || value.isBlank() ? "未命名节点" : value.trim();
  }

  private Long zeroAsNull(Long value) {
    return value == null || value == 0L ? null : value;
  }

  private long mix(long seed, String value) {
    long result = seed ^ GOLDEN_RATIO;
    if (value != null) for (int index = 0; index < value.length(); index++) result = result * 31 + value.charAt(index);
    result ^= result >>> 33;
    result *= 0xff51afd7ed558ccdl;
    result ^= result >>> 33;
    return result;
  }

  private record Tree(Map<Long, Node> byId, List<Node> roots) {
  }

  private static final class Node {
    private final NodeInput input;
    private final List<Node> children = new ArrayList<>();
    private Node parent;

    private Node(NodeInput input) {
      this.input = input;
    }

    private Long id() {
      return input.id();
    }
  }

  private enum SelectionMode {
    REQUIRED,
    CHOOSE_ONE
  }

  private static final class Structure {
    private final Node node;
    private final SelectionMode mode;
    private final String path;
    private final List<Node> leaves;
    private final BigDecimal pathWeight;
    private List<QuestionChoice> choices = List.of();

    private Structure(Node node, SelectionMode mode, String path, List<Node> leaves, BigDecimal pathWeight) {
      this.node = node;
      this.mode = mode;
      this.path = path;
      this.leaves = leaves;
      this.pathWeight = pathWeight;
    }
  }

  private static final class GroupPlan {
    private final String selectionGroup;
    private final String selectionMode;
    private final Long structureNodeId;
    private final String structurePath;
    private BigDecimal scoreWeight;
    private final List<Structure> candidates;
    private final List<QuestionChoice> choices;
    private Structure chosenNode;

    private GroupPlan(String selectionGroup, String selectionMode, Long structureNodeId,
        String structurePath, BigDecimal scoreWeight, List<Structure> candidates,
        List<QuestionChoice> choices) {
      this.selectionGroup = selectionGroup;
      this.selectionMode = selectionMode;
      this.structureNodeId = structureNodeId;
      this.structurePath = structurePath;
      this.scoreWeight = scoreWeight;
      this.candidates = candidates;
      this.choices = choices;
    }

    private GroupPlan withChosen(Structure node, List<QuestionChoice> selectedChoices) {
      GroupPlan copy = new GroupPlan(selectionGroup, selectionMode, structureNodeId, structurePath,
          scoreWeight, candidates, selectedChoices);
      copy.chosenNode = node;
      return copy;
    }

    private String selectionGroup() { return selectionGroup; }
    private String selectionMode() { return selectionMode; }
    private Long structureNodeId() { return structureNodeId; }
    private String structurePath() { return structurePath; }
    private BigDecimal scoreWeight() { return scoreWeight; }
  }

  private record QuestionChoice(
      Long questionId,
      Long assessmentPointId,
      Long structureId,
      BigDecimal sourceTotalScore,
      Integer durationMinutes) {
  }

  private record Slot(GroupPlan group, List<QuestionChoice> choices) {
  }

  private record MatchOutcome(List<GroupPlan> groups, Map<String, QuestionChoice> choiceByGroup) {
  }
}
