package com.guoyu.platform.competition.domain;

import com.guoyu.platform.competition.enums.CompetitionPracticalScoreStatus;
import com.guoyu.platform.competition.enums.CompetitionUnlockRequestStatus;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;

public final class CompetitionPracticalScoringPolicy {

  public ScoreRevision saveScore(ScoreRevision revision, BigDecimal score) {
    requireEditable(revision);
    validateScore(score);
    return new ScoreRevision(
        revision.revisionNo(),
        CompetitionPracticalScoreStatus.DRAFT,
        score,
        revision.currentRevision());
  }

  public ScoreRevision submitAndLock(ScoreRevision revision, BigDecimal score) {
    requireEditable(revision);
    validateScore(score);
    return new ScoreRevision(
        revision.revisionNo(),
        CompetitionPracticalScoreStatus.SUBMITTED_LOCKED,
        score,
        revision.currentRevision());
  }

  public UnlockRequest requestUnlock(
      ScoreRevision revision, ActorRole actorRole, int requestNo, String reason) {
    if (actorRole != ActorRole.HEAD_REFEREE) {
      throw new IllegalStateException("仅主裁判长可发起解锁申请");
    }
    if (revision == null
        || revision.status() != CompetitionPracticalScoreStatus.SUBMITTED_LOCKED
        || !revision.currentRevision()) {
      throw new IllegalStateException("仅当前已锁定评分可申请解锁");
    }
    if (requestNo <= 0 || reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("解锁申请编号和原因不能为空");
    }
    return new UnlockRequest(
        requestNo,
        revision.revisionNo(),
        CompetitionUnlockRequestStatus.PENDING,
        reason.trim());
  }

  public UnlockRequest decideUnlock(
      UnlockRequest request, ActorRole actorRole, boolean approved) {
    if (actorRole != ActorRole.SYSTEM_ADMIN) {
      throw new IllegalStateException("仅系统管理员可审批解锁申请");
    }
    if (request == null || request.status() != CompetitionUnlockRequestStatus.PENDING) {
      throw new IllegalStateException("仅待审批解锁申请可处理");
    }
    return new UnlockRequest(
        request.requestNo(),
        request.targetRevisionNo(),
        approved
            ? CompetitionUnlockRequestStatus.APPROVED
            : CompetitionUnlockRequestStatus.REJECTED,
        request.reason());
  }

  public RescoreRevision createRescoreRevision(
      ScoreRevision original, UnlockRequest approvedRequest) {
    if (original == null
        || original.status() != CompetitionPracticalScoreStatus.SUBMITTED_LOCKED
        || !original.currentRevision()) {
      throw new IllegalStateException("新修订必须源自当前已锁定评分");
    }
    if (approvedRequest == null
        || approvedRequest.status() != CompetitionUnlockRequestStatus.APPROVED
        || approvedRequest.targetRevisionNo() != original.revisionNo()) {
      throw new IllegalStateException("未批准解锁不得创建有效重评分修订");
    }
    ScoreRevision archived = new ScoreRevision(
        original.revisionNo(), original.status(), original.score(), false);
    ScoreRevision rescore = new ScoreRevision(
        original.revisionNo() + 1,
        CompetitionPracticalScoreStatus.UNLOCKED_FOR_RESCORE,
        null,
        true);
    return new RescoreRevision(archived, rescore);
  }

  public void assertScoringAllowed(
      Long refereeMemberId,
      Long contestantMemberId,
      Collection<AvoidanceRule> avoidanceRules) {
    Collection<AvoidanceRule> rules =
        avoidanceRules == null ? Collections.emptyList() : avoidanceRules;
    boolean forbidden = rules.stream().anyMatch(rule ->
        rule.active()
            && refereeMemberId != null
            && refereeMemberId.equals(rule.refereeMemberId())
            && contestantMemberId != null
            && contestantMemberId.equals(rule.contestantMemberId()));
    if (forbidden) {
      throw new IllegalStateException("裁判与选手存在生效禁评关系");
    }
  }

  private void requireEditable(ScoreRevision revision) {
    if (revision == null || !revision.currentRevision()) {
      throw new IllegalStateException("仅当前评分修订可编辑");
    }
    if (revision.status() != CompetitionPracticalScoreStatus.ASSIGNED
        && revision.status() != CompetitionPracticalScoreStatus.DRAFT
        && revision.status() != CompetitionPracticalScoreStatus.UNLOCKED_FOR_RESCORE) {
      throw new IllegalStateException("已锁定、回避或作废评分不可直接修改");
    }
  }

  private void validateScore(BigDecimal score) {
    if (score == null
        || score.compareTo(BigDecimal.ZERO) < 0
        || score.compareTo(new BigDecimal("100")) > 0) {
      throw new IllegalArgumentException("实操评分必须在0到100之间");
    }
  }

  public record ScoreRevision(
      int revisionNo,
      CompetitionPracticalScoreStatus status,
      BigDecimal score,
      boolean currentRevision) {

    public ScoreRevision {
      if (revisionNo <= 0 || status == null) {
        throw new IllegalArgumentException("评分修订号和状态必须有效");
      }
    }
  }

  public record UnlockRequest(
      int requestNo,
      int targetRevisionNo,
      CompetitionUnlockRequestStatus status,
      String reason) {
  }

  public record RescoreRevision(
      ScoreRevision archivedRevision,
      ScoreRevision newRevision) {
  }

  public record AvoidanceRule(
      Long refereeMemberId,
      Long contestantMemberId,
      boolean active) {
  }

  public enum ActorRole {
    REFEREE,
    HEAD_REFEREE,
    SYSTEM_ADMIN
  }
}
