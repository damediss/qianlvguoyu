package com.guoyu.platform.competition.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guoyu.platform.competition.domain.CompetitionPracticalScoringPolicy.ActorRole;
import com.guoyu.platform.competition.domain.CompetitionPracticalScoringPolicy.AvoidanceRule;
import com.guoyu.platform.competition.domain.CompetitionPracticalScoringPolicy.ScoreRevision;
import com.guoyu.platform.competition.enums.CompetitionPracticalScoreStatus;
import com.guoyu.platform.competition.enums.CompetitionUnlockRequestStatus;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompetitionPracticalScoringPolicyTest {

  private final CompetitionPracticalScoringPolicy policy =
      new CompetitionPracticalScoringPolicy();

  @Test
  void submissionLocksScoreAndDirectModificationIsRejected() {
    ScoreRevision assigned =
        new ScoreRevision(1, CompetitionPracticalScoreStatus.ASSIGNED, null, true);
    ScoreRevision locked = policy.submitAndLock(assigned, new BigDecimal("88.5"));

    assertThat(locked.status()).isEqualTo(CompetitionPracticalScoreStatus.SUBMITTED_LOCKED);
    assertThatThrownBy(() -> policy.saveScore(locked, new BigDecimal("90")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("不可直接修改");
  }

  @Test
  void approvedUnlockCreatesTraceableNewRevisionWhichCanLockAgain() {
    ScoreRevision original = policy.submitAndLock(
        new ScoreRevision(1, CompetitionPracticalScoreStatus.ASSIGNED, null, true),
        new BigDecimal("88.5"));
    var request = policy.requestUnlock(original, ActorRole.HEAD_REFEREE, 1, "录入证据有误");
    var approved = policy.decideUnlock(request, ActorRole.SYSTEM_ADMIN, true);
    var revisions = policy.createRescoreRevision(original, approved);
    ScoreRevision relocked =
        policy.submitAndLock(revisions.newRevision(), new BigDecimal("89.5"));

    assertThat(approved.status()).isEqualTo(CompetitionUnlockRequestStatus.APPROVED);
    assertThat(revisions.archivedRevision().revisionNo()).isEqualTo(1);
    assertThat(revisions.archivedRevision().score()).isEqualByComparingTo("88.5");
    assertThat(revisions.archivedRevision().currentRevision()).isFalse();
    assertThat(revisions.newRevision().revisionNo()).isEqualTo(2);
    assertThat(relocked.status()).isEqualTo(CompetitionPracticalScoreStatus.SUBMITTED_LOCKED);
    assertThat(relocked.score()).isEqualByComparingTo("89.5");
  }

  @Test
  void refereeCannotUnlockAndUnapprovedRequestCannotCreateRevision() {
    ScoreRevision locked = policy.submitAndLock(
        new ScoreRevision(1, CompetitionPracticalScoreStatus.ASSIGNED, null, true),
        new BigDecimal("88"));

    assertThatThrownBy(() ->
        policy.requestUnlock(locked, ActorRole.REFEREE, 1, "想修改"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("主裁判长");
    var pending = policy.requestUnlock(locked, ActorRole.HEAD_REFEREE, 1, "录入有误");
    assertThatThrownBy(() -> policy.createRescoreRevision(locked, pending))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("未批准");
  }

  @Test
  void activeAvoidanceRejectsScoringOperation() {
    assertThatThrownBy(() -> policy.assertScoringAllowed(
        101L, 202L, List.of(new AvoidanceRule(101L, 202L, true))))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("禁评");
  }
}
