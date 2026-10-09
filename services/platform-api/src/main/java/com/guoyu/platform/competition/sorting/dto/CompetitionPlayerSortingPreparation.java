package com.guoyu.platform.competition.sorting.dto;

import java.util.List;

/** 选手按公司归类后，由业务人员手动成组并命名，再保存组内角色。 */
public record CompetitionPlayerSortingPreparation(
    List<String> roleOptions,
    List<Member> members) {

  public record Member(
      String contestantProfileId,
      String orgName,
      String role,
      Integer groupNo,
      String groupName) {
  }
}
