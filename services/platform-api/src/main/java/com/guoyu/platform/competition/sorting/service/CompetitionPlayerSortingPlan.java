package com.guoyu.platform.competition.sorting.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** 选手归类与按组抽签。每个组抽到一个工位编号。 */
final class CompetitionPlayerSortingPlan {

  static final String MISSING_ORG = "未填写参赛公司";

  private CompetitionPlayerSortingPlan() {
  }

  record MemberSeed(String contestantProfileId, String orgName) {
  }

  record CompanyGroup(int groupNo, String orgName, List<String> profileIds, String groupName) {
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
