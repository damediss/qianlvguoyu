package com.guoyu.platform.competition.sorting.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

/** 选手按公司归类后，由业务人员手动成组并命名，再随机排定各组出场顺序。 */
public record CompetitionPlayerSortingPreparation(
    List<String> roleOptions,
    List<Member> members,
    Boolean groupOrderShuffled) {

  @JsonIgnore
  public CompetitionPlayerSortingPreparation(List<String> roleOptions, List<Member> members) {
    this(roleOptions, members, null);
  }

  public record Member(
      String contestantProfileId,
      String orgName,
      String role,
      Integer groupNo,
      String groupName,
      Integer drawOrder) {
  }
}
