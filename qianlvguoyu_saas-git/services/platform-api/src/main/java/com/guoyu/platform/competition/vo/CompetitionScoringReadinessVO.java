package com.guoyu.platform.competition.vo;

import java.util.ArrayList;
import java.util.List;

public class CompetitionScoringReadinessVO {

  private Long eventId;
  private Long stageId;
  private Long eventWorkTypeId;
  private boolean ready;
  private Long publishedScoreRuleVersionId;
  private List<CompetitionScoringReadinessCheckVO> checks = new ArrayList<>();

  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long stageId) { this.stageId = stageId; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public boolean isReady() { return ready; }
  public void setReady(boolean ready) { this.ready = ready; }
  public Long getPublishedScoreRuleVersionId() { return publishedScoreRuleVersionId; }
  public void setPublishedScoreRuleVersionId(Long publishedScoreRuleVersionId) {
    this.publishedScoreRuleVersionId = publishedScoreRuleVersionId;
  }
  public List<CompetitionScoringReadinessCheckVO> getChecks() { return checks; }
  public void setChecks(List<CompetitionScoringReadinessCheckVO> checks) {
    this.checks = checks == null ? new ArrayList<>() : new ArrayList<>(checks);
  }
}
