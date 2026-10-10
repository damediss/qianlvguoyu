package com.guoyu.platform.competition.sorting.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** 选手归类与按组抽签。每个组抽到一个工位编号。 */
final class CompetitionPlayerSortingPlan {

  static final String MISSING_ORG = "未填写参赛公司";

  private CompetitionPlayerSortingPlan() {
  }

  record MemberSeed(String contestantProfileId, String orgName) {
  }

  record CompanyGroup(int groupNo, String orgName, List<String> profileIds, String groupName) {
  }

  /** 把已有组号随机映射为 1..N 的出场顺序。先出现在返回值里的旧组号成为出场顺序 1。 */
  static Map<Integer, Integer> shuffleGroupAppearance(List<Integer> groupNos, Random random) {
    LinkedHashSet<Integer> unique = new LinkedHashSet<>();
    if (groupNos != null) {
      for (Integer groupNo : groupNos) {
        if (groupNo != null) {
          unique.add(groupNo);
        }
      }
    }
    if (unique.isEmpty()) {
      throw new IllegalArgumentException("no groups to shuffle");
    }
    List<Integer> shuffled = new ArrayList<>(unique);
    Collections.shuffle(shuffled, random);
    Map<Integer, Integer> oldToNew = new LinkedHashMap<>();
    for (int index = 0; index < shuffled.size(); index++) {
      oldToNew.put(shuffled.get(index), index + 1);
    }
    return oldToNew;
  }

  static String normalizeOrg(String orgName) {
    if (orgName == null || orgName.isBlank()) {
      return MISSING_ORG;
    }
    return orgName.trim();
  }

  /** 公司首次出现的顺序即组号，组内选手保持原选择顺序。 */
  static List<CompanyGroup> groupByCompany(List<MemberSeed> selectedInOrder) {
    Map<String, List<String>> byOrg = new LinkedHashMap<>();
    for (MemberSeed seed : selectedInOrder) {
      if (seed == null || seed.contestantProfileId() == null || seed.contestantProfileId().isBlank()) {
        continue;
      }
      byOrg.computeIfAbsent(normalizeOrg(seed.orgName()), key -> new ArrayList<>())
          .add(seed.contestantProfileId());
    }
    List<CompanyGroup> groups = new ArrayList<>();
    int groupNo = 1;
    for (Map.Entry<String, List<String>> entry : byOrg.entrySet()) {
      groups.add(new CompanyGroup(groupNo++, entry.getKey(), List.copyOf(entry.getValue()), entry.getKey()));
    }
    return List.copyOf(groups);
  }

  /** 从尚未抽中的排序号里均匀抽取一个。最后一名只剩一个号时仍然由调用方点击触发。 */
  static int drawUnusedOrder(int groupSize, java.util.Set<Integer> used, Random random) {
    if (groupSize < 1 || random == null) {
      throw new IllegalArgumentException("group size and random are required");
    }
    List<Integer> remaining = new ArrayList<>();
    for (int order = 1; order <= groupSize; order++) {
      if (used == null || !used.contains(order)) {
        remaining.add(order);
      }
    }
    if (remaining.isEmpty()) {
      throw new IllegalArgumentException("no order number remains");
    }
    return remaining.get(random.nextInt(remaining.size()));
  }

  /** 从尚未分配的工位编号里抽取一个。 */
  static String drawUnusedCode(List<String> codes, java.util.Set<String> used, Random random) {
    if (codes == null || codes.isEmpty() || random == null) {
      throw new IllegalArgumentException("codes and random are required");
    }
    List<String> remaining = new ArrayList<>();
    for (String code : codes) {
      if (used == null || !used.contains(code)) {
        remaining.add(code);
      }
    }
    if (remaining.isEmpty()) {
      throw new IllegalArgumentException("no workstation code remains");
    }
    return remaining.get(random.nextInt(remaining.size()));
  }

  enum WorkstationDrawGate {
    READY,
    NEED_GROUP_ORDER,
    WAIT_FOR_REMAINING_GROUPS,
    ALL_ASSIGNED
  }

  record WorkstationDrawChoice(WorkstationDrawGate gate, Integer groupNo) {
  }

  /**
   * 只给已经完成组内抽签、且尚未分配工位的小组抽工位。
   * 多个小组都可抽时，按组号从小到大取下一组，已抽出的工位不再进入候选。
   */
  static WorkstationDrawChoice chooseWorkstationGroup(
      List<Integer> groupNosInOrder,
      Set<Integer> orderCompleted,
      Set<Integer> workstationAssigned) {
    if (groupNosInOrder == null || groupNosInOrder.isEmpty()) {
      throw new IllegalArgumentException("groups are required");
    }
    Integer next = null;
    boolean anyIncomplete = false;
    boolean anyCompleted = false;
    for (Integer groupNo : groupNosInOrder) {
      if (groupNo == null) {
        continue;
      }
      boolean completed = orderCompleted != null && orderCompleted.contains(groupNo);
      boolean assigned = workstationAssigned != null && workstationAssigned.contains(groupNo);
      if (!completed) {
        anyIncomplete = true;
        continue;
      }
      anyCompleted = true;
      if (!assigned && next == null) {
        next = groupNo;
      }
    }
    if (next != null) {
      return new WorkstationDrawChoice(WorkstationDrawGate.READY, next);
    }
    if (anyIncomplete) {
      return new WorkstationDrawChoice(
          anyCompleted ? WorkstationDrawGate.WAIT_FOR_REMAINING_GROUPS : WorkstationDrawGate.NEED_GROUP_ORDER,
          null);
    }
    return new WorkstationDrawChoice(WorkstationDrawGate.ALL_ASSIGNED, null);
  }

  /** 每个组无放回抽取 1 个工位编号。编号数量不能少于组数。 */
  static List<String> drawForGroups(int groupCount, List<String> codes, Random random) {
    if (groupCount < 1) {
      throw new IllegalArgumentException("group count must be positive");
    }
    if (codes == null || codes.size() < groupCount) {
      throw new IllegalArgumentException("workstation codes are fewer than the groups");
    }
    if (random == null) {
      throw new IllegalArgumentException("random must not be null");
    }
    List<String> pool = new ArrayList<>(codes);
    Collections.shuffle(pool, random);
    return List.copyOf(pool.subList(0, groupCount));
  }
}
