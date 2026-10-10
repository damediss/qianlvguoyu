package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_stage_final_podium")
public class CompetitionStageFinalPodiumEntity extends BaseEntity {
  private Long settlementId;
  private Long planId;
  private Long eventId;
  private Long stageId;
  private Long firstContestantMemberId;
  private Long secondContestantMemberId;
  private Long thirdContestantMemberId;
  private String specialExplanation;
  private String status;
  private Long confirmedBy;
  private LocalDateTime confirmedAt;
  private Long publishedBy;
  private LocalDateTime publishedAt;

  public Long getSettlementId() { return settlementId; }
  public void setSettlementId(Long value) { settlementId = value; }
  public Long getPlanId() { return planId; }
  public void setPlanId(Long value) { planId = value; }
  public Long getEventId() { return eventId; }
  public void setEventId(Long value) { eventId = value; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long value) { stageId = value; }
  public Long getFirstContestantMemberId() { return firstContestantMemberId; }
  public void setFirstContestantMemberId(Long value) { firstContestantMemberId = value; }
  public Long getSecondContestantMemberId() { return secondContestantMemberId; }
  public void setSecondContestantMemberId(Long value) { secondContestantMemberId = value; }
  public Long getThirdContestantMemberId() { return thirdContestantMemberId; }
  public void setThirdContestantMemberId(Long value) { thirdContestantMemberId = value; }
  public String getSpecialExplanation() { return specialExplanation; }
  public void setSpecialExplanation(String value) { specialExplanation = value; }
  public String getStatus() { return status; }
  public void setStatus(String value) { status = value; }
  public Long getConfirmedBy() { return confirmedBy; }
  public void setConfirmedBy(Long value) { confirmedBy = value; }
  public LocalDateTime getConfirmedAt() { return confirmedAt; }
  public void setConfirmedAt(LocalDateTime value) { confirmedAt = value; }
  public Long getPublishedBy() { return publishedBy; }
  public void setPublishedBy(Long value) { publishedBy = value; }
  public LocalDateTime getPublishedAt() { return publishedAt; }
  public void setPublishedAt(LocalDateTime value) { publishedAt = value; }
}
