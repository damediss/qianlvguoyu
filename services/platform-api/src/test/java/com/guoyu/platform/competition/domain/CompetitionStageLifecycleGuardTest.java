package com.guoyu.platform.competition.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.mapper.CompetitionStageExecutionPlanMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageRosterItemMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageRosterSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageSettlementItemMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageSettlementMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CompetitionStageLifecycleGuardTest {

  private final CompetitionStageMapper stageMapper = Mockito.mock(CompetitionStageMapper.class);
  private final CompetitionStageExecutionPlanMapper planMapper = Mockito.mock(CompetitionStageExecutionPlanMapper.class);
  private final CompetitionStageRosterSnapshotMapper rosterSnapshotMapper = Mockito.mock(CompetitionStageRosterSnapshotMapper.class);
  private final CompetitionStageRosterItemMapper rosterItemMapper = Mockito.mock(CompetitionStageRosterItemMapper.class);
  private final CompetitionStageSettlementMapper settlementMapper = Mockito.mock(CompetitionStageSettlementMapper.class);
  private final CompetitionStageSettlementItemMapper settlementItemMapper = Mockito.mock(CompetitionStageSettlementItemMapper.class);
  private final CompetitionStageLifecycleGuard guard = new CompetitionStageLifecycleGuard(
      stageMapper, planMapper, rosterSnapshotMapper, rosterItemMapper, settlementMapper, settlementItemMapper);

  @Test
  void interactiveWritesRejectClosedStage() {
    CompetitionStageEntity stage = stage("closed", LocalDateTime.now().minusHours(2), LocalDateTime.now().plusHours(1));
    when(stageMapper.selectOne(any())).thenReturn(stage);

    assertThatThrownBy(() -> guard.requireInteractiveWrite(1L, 10L))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void interactiveWritesRejectAfterEndAtEvenWhenStatusIsInProgress() {
    CompetitionStageEntity stage = stage("in_progress", LocalDateTime.now().minusHours(2), LocalDateTime.now().minusMinutes(1));
    when(stageMapper.selectOne(any())).thenReturn(stage);

    assertThatThrownBy(() -> guard.requireInteractiveWrite(1L, 10L))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void allocationWritableAllowsDraftConfiguringAndReadyLocked() {
    assertThatCode(() -> guard.requireStageAllocationWritable(
        stage("draft", LocalDateTime.now(), LocalDateTime.now().plusHours(1))))
        .doesNotThrowAnyException();
    assertThatCode(() -> guard.requireStageAllocationWritable(
        stage("configuring", LocalDateTime.now(), LocalDateTime.now().plusHours(1))))
        .doesNotThrowAnyException();
    assertThatCode(() -> guard.requireStageAllocationWritable(
        stage("ready_locked", LocalDateTime.now(), LocalDateTime.now().plusHours(1))))
        .doesNotThrowAnyException();
  }

  @Test
  void allocationWritableRejectsStartedOrClosedStages() {
    assertThatThrownBy(() -> guard.requireStageAllocationWritable(
        stage("in_progress", LocalDateTime.now(), LocalDateTime.now().plusHours(1))))
        .isInstanceOf(BusinessException.class)
        .hasMessage("当前赛段不允许调整分场或裁判分配");
    assertThatThrownBy(() -> guard.requireStageAllocationWritable(
        stage("closed", LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1))))
        .isInstanceOf(BusinessException.class)
        .hasMessage("当前赛段不允许调整分场或裁判分配");
  }

  private CompetitionStageEntity stage(String status, LocalDateTime startAt, LocalDateTime endAt) {
    CompetitionStageEntity stage = new CompetitionStageEntity();
    stage.setId(10L);
    stage.setTenantId(1L);
    stage.setEventId(20L);
    stage.setStatus(status);
    stage.setStartAt(startAt);
    stage.setEndAt(endAt);
    stage.setDeleted(0);
    return stage;
  }
}
