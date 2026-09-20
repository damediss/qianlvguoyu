package com.guoyu.platform.competition.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CompetitionPracticalMakeExamAllocatorTest {

  private final CompetitionPracticalMakeExamAllocator allocator = new CompetitionPracticalMakeExamAllocator();

  @Test
  void normalizesRequiredUnitsAndKeepsQuestionQuotaOutOfQuestionCount() {
    var nodes = List.of(
        node(1L, null, "范围", BigDecimal.ONE, null, 1),
        node(2L, 1L, "设备", new BigDecimal("0.40"), "全部必考", 1),
        node(3L, 2L, "设备点1", null, null, 1),
        node(4L, 2L, "设备点2", null, null, 2),
        node(5L, 1L, "流程", new BigDecimal("0.60"), "必考", 2),
        node(6L, 5L, "流程点", null, null, 1));
    var result = allocator.allocate(new CompetitionPracticalMakeExamAllocator.AllocationRequest(
        99L, nodes,
        List.of(binding(101L, 99L, 3L), binding(102L, 99L, 6L)),
        List.of(candidate(101L), candidate(102L)), new BigDecimal("100"), 1, 7L));

    assertThat(result.ready()).isTrue();
    assertThat(result.questionCount()).isEqualTo(2);
    assertThat(result.units()).extracting(CompetitionPracticalMakeExamAllocator.UnitAllocation::targetScore)
        .containsExactly(new BigDecimal("40.00"), new BigDecimal("60.00"));
    assertThat(result.units()).allSatisfy(unit -> assertThat(unit.candidateQuestionCount()).isEqualTo(1));
  }

  @Test
  void usesFourteenMandatoryRangeWeightsAsAbsoluteWholePaperWeights() {
    List<CompetitionPracticalMakeExamAllocator.NodeInput> nodes = new ArrayList<>();
    List<CompetitionPracticalMakeExamAllocator.BindingInput> bindings = new ArrayList<>();
    List<CompetitionPracticalMakeExamAllocator.CandidateInput> candidates = new ArrayList<>();
    nodes.add(node(1L, null, "技能操作", BigDecimal.ONE, null, 1));
    nodes.add(node(10L, 1L, "生产准备", new BigDecimal("0.20"), null, 1));
    nodes.add(node(20L, 1L, "配矿", new BigDecimal("0.05"), null, 2));
    nodes.add(node(30L, 1L, "氢氧化铝焙烧", new BigDecimal("0.55"), null, 3));
    nodes.add(node(40L, 1L, "设备维护与保养", new BigDecimal("0.10"), null, 4));
    nodes.add(node(50L, 1L, "安全生产", new BigDecimal("0.10"), null, 5));

    addRequiredUnit(nodes, bindings, candidates, 101L, 10L, "设备准备", "0.08", 3, 1);
    addRequiredUnit(nodes, bindings, candidates, 102L, 10L, "流程准备", "0.04", 2, 2);
    addRequiredUnit(nodes, bindings, candidates, 103L, 10L, "物料准备", "0.04", 1, 3);
    addRequiredUnit(nodes, bindings, candidates, 104L, 10L, "其他准备", "0.04", 1, 4);
    addRequiredUnit(nodes, bindings, candidates, 201L, 20L, "卸车", "0.01", 1, 1);
    addRequiredUnit(nodes, bindings, candidates, 202L, 20L, "碎矿", "0.02", 1, 2);
    addRequiredUnit(nodes, bindings, candidates, 203L, 20L, "均化", "0.02", 1, 3);
    addRequiredUnit(nodes, bindings, candidates, 301L, 30L, "烘炉与准备", "0.10", 1, 1);
    addRequiredUnit(nodes, bindings, candidates, 302L, 30L, "生产运行操作", "0.15", 1, 2);
    addRequiredUnit(nodes, bindings, candidates, 303L, 30L, "故障判断与处理", "0.30", 1, 3);
    addRequiredUnit(nodes, bindings, candidates, 401L, 40L, "设备维护", "0.06", 1, 1);
    addRequiredUnit(nodes, bindings, candidates, 402L, 40L, "设备检修", "0.04", 1, 2);
    addRequiredUnit(nodes, bindings, candidates, 501L, 50L, "作业环境安全监管", "0.05", 1, 1);
    addRequiredUnit(nodes, bindings, candidates, 502L, 50L, "生产作业安全监管", "0.05", 1, 2);

    var result = allocator.allocate(new CompetitionPracticalMakeExamAllocator.AllocationRequest(
        99L, nodes, bindings, candidates, new BigDecimal("100"), 1, 20260917L));

    assertThat(result.ready()).isTrue();
    assertThat(result.questionCount()).isEqualTo(14);
    assertThat(result.units()).allSatisfy(unit -> {
      assertThat(unit.selectionMode()).isEqualTo("REQUIRED");
      assertThat(unit.selectedQuestionId()).isNotNull();
    });
    assertThat(result.units().stream().map(CompetitionPracticalMakeExamAllocator.UnitAllocation::targetScore)
        .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("100.00");
    assertThat(result.units()).filteredOn(unit -> unit.structurePath().endsWith("设备准备"))
        .singleElement().satisfies(unit -> {
          assertThat(unit.targetScore()).isEqualByComparingTo("8.00");
          assertThat(unit.candidateQuestionCount()).isEqualTo(3);
        });
    assertThat(result.units()).filteredOn(unit -> unit.structurePath().endsWith("流程准备"))
        .singleElement().satisfies(unit -> {
          assertThat(unit.targetScore()).isEqualByComparingTo("4.00");
          assertThat(unit.candidateQuestionCount()).isEqualTo(2);
        });
  }

  @Test
  void createsFourSlotsWhenTopLevelRangesAreRequiredAndChildrenChooseOne() {
    List<CompetitionPracticalMakeExamAllocator.NodeInput> nodes = new ArrayList<>();
    List<CompetitionPracticalMakeExamAllocator.BindingInput> bindings = new ArrayList<>();
    List<CompetitionPracticalMakeExamAllocator.CandidateInput> candidates = new ArrayList<>();
    long[] rangeIds = {100L, 200L, 300L, 400L};
    String[] rangeNames = {"A 作业环境确认", "B 生产监控管理", "C 生产运行总结", "D 技术管理和培训指导"};
    String[] rangeWeights = {"0.10", "0.40", "0.40", "0.10"};
    int[] optionalCounts = {2, 3, 2, 3};
    for (int rangeIndex = 0; rangeIndex < rangeIds.length; rangeIndex++) {
      long rangeId = rangeIds[rangeIndex];
      nodes.add(node(rangeId, null, rangeNames[rangeIndex],
          new BigDecimal(rangeWeights[rangeIndex]), "必考", rangeIndex + 1));
      for (int optionIndex = 1; optionIndex <= optionalCounts[rangeIndex]; optionIndex++) {
        long optionId = rangeId + optionIndex;
        long pointId = optionId * 100;
        long questionId = optionId * 1000;
        nodes.add(node(optionId, rangeId, "二级候选" + optionIndex,
            new BigDecimal(rangeWeights[rangeIndex]), "任选一项", optionIndex));
        nodes.add(node(pointId, optionId, "考评点", null, null, 1));
        bindings.add(binding(questionId, 99L, pointId));
        candidates.add(candidate(questionId));
      }
    }

    var result = allocator.allocate(new CompetitionPracticalMakeExamAllocator.AllocationRequest(
        99L, nodes, bindings, candidates, new BigDecimal("100"), 1, 20260918L));

    assertThat(result.ready()).isTrue();
    assertThat(result.questionCount()).isEqualTo(4);
    assertThat(result.units()).extracting(CompetitionPracticalMakeExamAllocator.UnitAllocation::selectionMode)
        .containsOnly("CHOOSE_ONE");
    assertThat(result.units()).extracting(CompetitionPracticalMakeExamAllocator.UnitAllocation::targetScore)
        .containsExactly(new BigDecimal("10.00"), new BigDecimal("40.00"),
            new BigDecimal("40.00"), new BigDecimal("10.00"));
    assertThat(result.units()).extracting(unit -> unit.candidateNodes().size())
        .containsExactly(2, 3, 2, 3);
    assertThat(result.units()).allSatisfy(unit -> assertThat(unit.selectedQuestionId()).isNotNull());
  }

  @Test
  void groupsOptionalSiblingsByExactParentAndUsesOneInheritedWeight() {
    var nodes = List.of(
        node(1L, null, "范围", BigDecimal.ONE, null, 1),
        node(2L, 1L, "任选A", new BigDecimal("0.30"), "任选一项", 1),
        node(3L, 2L, "点A", null, null, 1),
        node(4L, 1L, "任选B", new BigDecimal("0.70"), "任选1项", 2),
        node(5L, 4L, "点B", null, null, 1));
    var result = allocator.allocate(new CompetitionPracticalMakeExamAllocator.AllocationRequest(
        99L, nodes, List.of(binding(201L, 99L, 3L), binding(202L, 99L, 5L)),
        List.of(candidate(201L), candidate(202L)), new BigDecimal("100"), 1, 11L));

    assertThat(result.ready()).isTrue();
    assertThat(result.questionCount()).isEqualTo(1);
    assertThat(result.units().get(0).selectionMode()).isEqualTo("CHOOSE_ONE");
    assertThat(result.units().get(0).scoreWeight()).isEqualByComparingTo("1");
    assertThat(result.units().get(0).candidateNodes()).hasSize(2)
        .allSatisfy(node -> assertThat(node.scoreWeight()).isEqualByComparingTo("1"));
    assertThat(result.units().get(0).selectedQuestionId()).isIn(201L, 202L);
  }

  @Test
  void doesNotMixOptionalSiblingsUnderDifferentParents() {
    var nodes = List.of(
        node(1L, null, "范围", BigDecimal.ONE, null, 1),
        node(2L, 1L, "父A", new BigDecimal("0.5"), null, 1),
        node(3L, 2L, "A任选", BigDecimal.ONE, "选考", 1),
        node(4L, 3L, "A点", null, null, 1),
        node(5L, 1L, "父B", new BigDecimal("0.5"), null, 2),
        node(6L, 5L, "B任选", BigDecimal.ONE, "选考", 1),
        node(7L, 6L, "B点", null, null, 1));
    var result = allocator.allocate(new CompetitionPracticalMakeExamAllocator.AllocationRequest(
        99L, nodes, List.of(binding(301L, 99L, 4L), binding(302L, 99L, 7L)),
        List.of(candidate(301L), candidate(302L)), new BigDecimal("100"), 1, 13L));
    assertThat(result.ready()).isTrue();
    assertThat(result.questionCount()).isEqualTo(2);
    assertThat(result.units()).extracting(CompetitionPracticalMakeExamAllocator.UnitAllocation::selectionGroup)
        .containsExactly("CHOOSE_ONE:2", "CHOOSE_ONE:5");
    assertThat(result.units()).extracting(CompetitionPracticalMakeExamAllocator.UnitAllocation::targetScore)
        .containsExactly(new BigDecimal("50.00"), new BigDecimal("50.00"));
  }

  @Test
  void rejectsBlankOrUnknownSelectionModeInsteadOfGuessingRequired() {
    var nodes = List.of(
        node(1L, null, "范围", BigDecimal.ONE, null, 1),
        node(2L, 1L, "待配置结构", BigDecimal.ONE, "以后再说", 1),
        node(3L, 2L, "考评点", null, null, 1));
    var result = allocator.allocate(new CompetitionPracticalMakeExamAllocator.AllocationRequest(
        nodes, List.of(binding(401L, null, 3L)), List.of(candidate(401L)), new BigDecimal("100"), 1, 17L));

    assertThat(result.ready()).isFalse();
    assertThat(result.errors()).anyMatch(error -> error.contains("无法识别"));
  }

  @Test
  void supportsSelectableLeafStructuresWithoutAssumingAThirdLevelTopic() {
    var nodes = List.of(
        node(1L, null, "业务范围", BigDecimal.ONE, null, 1),
        node(2L, 1L, "必考末级点", new BigDecimal("0.40"), "全部必考", 1),
        node(3L, 1L, "任选末级点", new BigDecimal("0.60"), "任一", 2));
    var result = allocator.allocate(new CompetitionPracticalMakeExamAllocator.AllocationRequest(
        99L, nodes, List.of(binding(451L, 99L, 2L), binding(452L, 99L, 3L)),
        List.of(candidate(451L), candidate(452L)), new BigDecimal("100"), 1, 18L));

    assertThat(result.ready()).isTrue();
    assertThat(result.questionCount()).isEqualTo(2);
    assertThat(result.units()).extracting(CompetitionPracticalMakeExamAllocator.UnitAllocation::selectionMode)
        .containsExactlyInAnyOrder("REQUIRED", "CHOOSE_ONE");
  }

  @Test
  void reportsInvalidSourceScoreAndDoesNotCreateAnEmptyFinalUnit() {
    var nodes = List.of(
        node(1L, null, "范围", BigDecimal.ONE, null, 1),
        node(2L, 1L, "必考", BigDecimal.ONE, "必考", 1),
        node(3L, 2L, "点", null, null, 1));
    var result = allocator.allocate(new CompetitionPracticalMakeExamAllocator.AllocationRequest(
        99L, nodes, List.of(binding(461L, 99L, 3L)),
        List.of(new CompetitionPracticalMakeExamAllocator.CandidateInput(461L, BigDecimal.ZERO, 60)),
        new BigDecimal("100"), 1, 21L));

    assertThat(result.ready()).isFalse();
    assertThat(result.warnings()).anyMatch(warning -> warning.contains("原始总分"));
    assertThat(result.errors()).anyMatch(error -> error.contains("无法"));
  }

  @Test
  void preservesDeterminismAndPrefersNoCrossPaperRepeats() {
    var nodes = List.of(
        node(1L, null, "范围", BigDecimal.ONE, null, 1),
        node(2L, 1L, "必考", BigDecimal.ONE, "必考", 1),
        node(3L, 2L, "点", null, null, 1));
    var bindings = List.of(binding(501L, 99L, 3L), binding(502L, 99L, 3L));
    var candidates = List.of(candidate(501L), candidate(502L));
    var request = new CompetitionPracticalMakeExamAllocator.AllocationRequest(
        99L, nodes, bindings, candidates, new BigDecimal("100"), 1, 19L);
    var first = allocator.allocate(request);
    var second = allocator.allocate(request);
    var next = allocator.allocate(request.withPrevious(Set.of(first.units().get(0).selectedQuestionId())));

    assertThat(first.selectedQuestionIds()).isEqualTo(second.selectedQuestionIds());
    assertThat(next.units().get(0).selectedQuestionId()).isNotEqualTo(first.units().get(0).selectedQuestionId());
  }

  @Test
  void warnsAndFallsBackWhenCrossPaperDeduplicationIsImpossible() {
    var nodes = List.of(
        node(1L, null, "范围", BigDecimal.ONE, null, 1),
        node(2L, 1L, "必考", BigDecimal.ONE, "必考", 1),
        node(3L, 2L, "点", null, null, 1));
    var result = allocator.allocate(new CompetitionPracticalMakeExamAllocator.AllocationRequest(
        99L, nodes, List.of(binding(601L, 99L, 3L)), List.of(candidate(601L)),
        new BigDecimal("100"), 2, 23L, 60, Set.of(601L)));

    assertThat(result.ready()).isTrue();
    assertThat(result.warnings()).anyMatch(warning -> warning.contains("套卷之间完全去重"));
    assertThat(result.referenceDurationMinutes()).isEqualTo(60);
  }

  @Test
  void allocatesExactCentsAndScalesFromSelectedSourceTotal() {
    var nodes = List.of(
        node(1L, null, "范围", BigDecimal.ONE, null, 1),
        node(2L, 1L, "A", new BigDecimal("0.333333"), "必考", 1),
        node(3L, 2L, "点A", null, null, 1),
        node(4L, 1L, "B", new BigDecimal("0.333333"), "必考", 2),
        node(5L, 4L, "点B", null, null, 1),
        node(6L, 1L, "C", new BigDecimal("0.333334"), "必考", 3),
        node(7L, 6L, "点C", null, null, 1));
    var result = allocator.allocate(new CompetitionPracticalMakeExamAllocator.AllocationRequest(
        99L, nodes, List.of(binding(701L, 99L, 3L), binding(702L, 99L, 5L), binding(703L, 99L, 7L)),
        List.of(new CompetitionPracticalMakeExamAllocator.CandidateInput(701L, new BigDecimal("50"), 10),
            new CompetitionPracticalMakeExamAllocator.CandidateInput(702L, new BigDecimal("25"), 20),
            new CompetitionPracticalMakeExamAllocator.CandidateInput(703L, new BigDecimal("100"), 30)),
        new BigDecimal("100"), 1, 29L));

    assertThat(result.ready()).isTrue();
    assertThat(result.units()).extracting(CompetitionPracticalMakeExamAllocator.UnitAllocation::targetScore)
        .containsExactly(new BigDecimal("33.33"), new BigDecimal("33.33"), new BigDecimal("33.34"));
    assertThat(result.units()).allSatisfy(unit -> assertThat(unit.scoreScale()).isNotNull());
    assertThat(result.units()).extracting(CompetitionPracticalMakeExamAllocator.UnitAllocation::referenceDurationMinutes)
        .containsExactly(10, 20, 30);
  }

  private CompetitionPracticalMakeExamAllocator.NodeInput node(Long id, Long parentId, String name,
      BigDecimal weight, String selectMode, int sortOrder) {
    return new CompetitionPracticalMakeExamAllocator.NodeInput(id, parentId, 3,
        String.valueOf(id), String.valueOf(id), name, weight, selectMode, sortOrder);
  }

  private CompetitionPracticalMakeExamAllocator.BindingInput binding(Long questionId, Long blueprintId,
      Long nodeId) {
    return new CompetitionPracticalMakeExamAllocator.BindingInput(questionId, blueprintId, nodeId);
  }

  private CompetitionPracticalMakeExamAllocator.CandidateInput candidate(Long questionId) {
    return new CompetitionPracticalMakeExamAllocator.CandidateInput(questionId, new BigDecimal("100"), 60);
  }

  private void addRequiredUnit(List<CompetitionPracticalMakeExamAllocator.NodeInput> nodes,
      List<CompetitionPracticalMakeExamAllocator.BindingInput> bindings,
      List<CompetitionPracticalMakeExamAllocator.CandidateInput> candidates, Long unitId,
      Long parentId, String name, String weight, int candidateCount, int sortOrder) {
    nodes.add(node(unitId, parentId, name, new BigDecimal(weight), "必考", sortOrder));
    for (int index = 1; index <= candidateCount; index++) {
      long leafId = unitId * 100 + index;
      long questionId = unitId * 1000 + index;
      nodes.add(node(leafId, unitId, name + "考评点" + index, null, null, index));
      bindings.add(binding(questionId, 99L, leafId));
      candidates.add(candidate(questionId));
    }
  }
}
