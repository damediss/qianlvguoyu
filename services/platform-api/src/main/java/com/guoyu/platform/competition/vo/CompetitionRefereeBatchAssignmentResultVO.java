package com.guoyu.platform.competition.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.ArrayList;
import java.util.List;

public class CompetitionRefereeBatchAssignmentResultVO {

  @JsonSerialize(using = ToStringSerializer.class)
  private Long stageId;
  @JsonSerialize(contentUsing = ToStringSerializer.class)
  private List<Long> contestantMemberIds = new ArrayList<>();
  private int updatedCount;

  public Long getStageId() { return stageId; }
  public void setStageId(Long stageId) { this.stageId = stageId; }
  public List<Long> getContestantMemberIds() { return contestantMemberIds; }
  public void setContestantMemberIds(List<Long> contestantMemberIds) {
    this.contestantMemberIds = contestantMemberIds == null
        ? new ArrayList<>() : new ArrayList<>(contestantMemberIds);
  }
  public int getUpdatedCount() { return updatedCount; }
  public void setUpdatedCount(int updatedCount) { this.updatedCount = updatedCount; }
}
