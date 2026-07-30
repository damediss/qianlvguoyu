package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;

@TableName("competition_contestant_profile")
public class CompetitionContestantProfileEntity extends BaseEntity {

  private Long memberId;
  private Long registrationId;
  private String contestantNo;
  private String groupCode;
  private Long currentStageId;
  private Long eventWorkTypeId;
  private String workTypeMatchStatus;
  private String workTypeMatchNote;
  private String promotionStatus;
  private BigDecimal totalScore;

  public Long getMemberId() { return memberId; }
  public void setMemberId(Long memberId) { this.memberId = memberId; }
  public Long getRegistrationId() { return registrationId; }
  public void setRegistrationId(Long registrationId) { this.registrationId = registrationId; }
  public String getContestantNo() { return contestantNo; }
  public void setContestantNo(String contestantNo) { this.contestantNo = contestantNo; }
  public String getGroupCode() { return groupCode; }
  public void setGroupCode(String groupCode) { this.groupCode = groupCode; }
  public Long getCurrentStageId() { return currentStageId; }
  public void setCurrentStageId(Long currentStageId) { this.currentStageId = currentStageId; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public String getWorkTypeMatchStatus() { return workTypeMatchStatus; }
  public void setWorkTypeMatchStatus(String workTypeMatchStatus) {
    this.workTypeMatchStatus = workTypeMatchStatus;
  }
  public String getWorkTypeMatchNote() { return workTypeMatchNote; }
  public void setWorkTypeMatchNote(String workTypeMatchNote) { this.workTypeMatchNote = workTypeMatchNote; }
  public String getPromotionStatus() { return promotionStatus; }
  public void setPromotionStatus(String promotionStatus) { this.promotionStatus = promotionStatus; }
  public BigDecimal getTotalScore() { return totalScore; }
  public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
}
