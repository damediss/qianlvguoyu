package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.guoyu.platform.competition.dto.*;
import com.guoyu.platform.competition.entity.*;
import com.guoyu.platform.competition.mapper.*;
import com.guoyu.platform.competition.vo.CompetitionStageVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.util.*;
import java.util.stream.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CompetitionStagePaperAssignmentApplicationServiceTest {
  private final CompetitionStageApplicationService stages = mock(CompetitionStageApplicationService.class);
  private final CompetitionEventService events = mock(CompetitionEventService.class);
  private final CompetitionEventWorkTypeMapper workTypes = mock(CompetitionEventWorkTypeMapper.class);
  private final CompetitionEventWorkTypeLevelMapper levels = mock(CompetitionEventWorkTypeLevelMapper.class);
  private final CompetitionTheoryPaperSnapshotMapper papers = mock(CompetitionTheoryPaperSnapshotMapper.class);
  private final CompetitionStagePaperAssignmentMapper assignments = mock(CompetitionStagePaperAssignmentMapper.class);
  private final CompetitionFormalExamSessionMapper formalSessions =
      mock(CompetitionFormalExamSessionMapper.class);
  private final CompetitionPracticalPaperBindingMapper practicalBindings =
      mock(CompetitionPracticalPaperBindingMapper.class);
  private final CompetitionMakeExamPaperSnapshotMapper practicalSnapshots =
      mock(CompetitionMakeExamPaperSnapshotMapper.class);
  private final CompetitionStagePaperAssignmentApplicationService service =
      new CompetitionStagePaperAssignmentApplicationService(
          stages, events, workTypes, levels, papers, assignments, formalSessions,
          practicalBindings, practicalSnapshots);

  @BeforeEach void setup() {
    var principal = new PlatformUserPrincipal(1000L, 1L, "admin", "Admin", "platform", List.of(), List.of(), null);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    var stage = new CompetitionStageVO();
    stage.setId(10L); stage.setEventId(20L); stage.setEventWorkTypeId(30L); stage.setEventWorkTypeLevelCode("3");
    when(stages.assertCanManage(10L)).thenReturn(stage);
    var event = new CompetitionEventEntity(); event.setId(20L);
    when(events.getById(1L, 20L)).thenReturn(event);
    when(events.lockForUpdate(1L, 20L)).thenReturn(event);
    var workType = new CompetitionEventWorkTypeEntity(); workType.setId(30L);
    when(workTypes.selectOne(any())).thenReturn(workType);
    var level = new CompetitionEventWorkTypeLevelEntity(); level.setId(40L); level.setLevelCode("3");
    when(levels.selectOne(any())).thenReturn(level);
    when(assignments.selectByScopeForUpdate(1L, 20L, 10L, 30L, 40L, "theory"))
        .thenReturn(List.of());
    when(assignments.insert(any(CompetitionStagePaperAssignmentEntity.class))).thenReturn(1);
    when(assignments.updateById(any(CompetitionStagePaperAssignmentEntity.class))).thenReturn(1);
    when(formalSessions.selectLatestByStage(1L, 10L)).thenReturn(null);
  }

  @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

  private void candidates(int count) {
    var rows = IntStream.range(0, count).mapToObj(index -> {
      var row = new CompetitionStagePaperAssignmentPlayerRow();
      row.setContestantProfileId(100L + index); row.setContestantMemberId(200L + index);
      row.setRegistrationId(300L + index); return row;
    }).toList();
    when(assignments.selectEligiblePlayers(eq(1L), eq(20L), eq(10L), eq(30L), eq(40L),
        anyList(), isNull(), isNull(), isNull(), isNull(), eq("theory"))).thenReturn(rows);
  }

  private void practicalCandidates(int count) {
    var rows = IntStream.range(0, count).mapToObj(index -> {
      var row = new CompetitionStagePaperAssignmentPlayerRow();
      row.setContestantProfileId(100L + index); row.setContestantMemberId(200L + index);
      row.setRegistrationId(300L + index); return row;
    }).toList();
    when(assignments.selectEligiblePlayers(eq(1L), eq(20L), eq(10L), eq(30L), eq(40L),
        anyList(), isNull(), isNull(), isNull(), isNull(), eq("practical"))).thenReturn(rows);
  }

  private void papers(int count) {
    when(papers.selectList(any())).thenReturn(IntStream.range(0, count).mapToObj(index -> {
      var paper = new CompetitionTheoryPaperSnapshotEntity(); paper.setId(500L + index);
      paper.setPaperCode("P" + index); return paper;
    }).toList());
  }

  @Test void balancesUnevenPlayerCountAcrossEveryConfiguredPaper() {
    candidates(123); papers(5);
    service.autoAllocate(10L);
    var capture = ArgumentCaptor.forClass(CompetitionStagePaperAssignmentEntity.class);
    verify(assignments, times(123)).insert(capture.capture());
    var saved = capture.getAllValues();
    assertThat(saved).extracting(CompetitionStagePaperAssignmentEntity::getContestantProfileId).doesNotHaveDuplicates();
    var counts = saved.stream().collect(Collectors.groupingBy(
        CompetitionStagePaperAssignmentEntity::getPaperSnapshotId, Collectors.counting()));
    assertThat(counts).hasSize(5);
    assertThat(Collections.max(counts.values()) - Collections.min(counts.values())).isLessThanOrEqualTo(1L);
  }

  @Test void rejectsPaperOutsideCurrentStageWithoutWriting() {
    candidates(1); papers(2);
    assertThatThrownBy(() -> service.save(10L, new CompetitionStagePaperAssignmentBatchReq(
        List.of(new CompetitionStagePaperAssignmentItemReq(100L, 999L)))))
        .hasMessage("只能分配当前赛段已发布的试卷");
    verify(assignments, never()).insert(any(CompetitionStagePaperAssignmentEntity.class));
    verify(assignments, never()).updateById(any(CompetitionStagePaperAssignmentEntity.class));
  }

  @Test void clearingPaperReturnsConfirmedAssignmentToDraft() {
    candidates(1); papers(2);
    var existing = new CompetitionStagePaperAssignmentEntity();
    existing.setId(700L); existing.setContestantProfileId(100L); existing.setPaperSnapshotId(500L);
    existing.setStatus("confirmed"); existing.setConfirmedBy(1000L);
    when(assignments.selectByScopeForUpdate(1L, 20L, 10L, 30L, 40L, "theory"))
        .thenReturn(List.of(existing));
    service.save(10L, new CompetitionStagePaperAssignmentBatchReq(
        List.of(new CompetitionStagePaperAssignmentItemReq(100L, null))));
    verify(assignments).updateById(existing);
    assertThat(existing.getPaperSnapshotId()).isNull();
    assertThat(existing.getConfirmedBy()).isNull();
    assertThat(existing.getStatus()).isEqualTo("draft");
  }

  @Test void refusesConfirmationWithUnassignedPlayers() {
    candidates(1); papers(2);
    assertThatThrownBy(() -> service.confirm(10L, new CompetitionStagePaperAssignmentBatchReq(List.of())))
        .hasMessage("仍有未分配试卷的选手，不能确认配置");
  }

  @Test void readyFormalExamLocksAllAssignmentWrites() {
    candidates(1); papers(2);
    var runtime = new CompetitionFormalExamSessionEntity();
    runtime.setId(9001L);
    runtime.setStatus("ready");
    when(formalSessions.selectLatestByStage(1L, 10L)).thenReturn(runtime);
    assertThat(service.summary(10L).formalExamLocked()).isTrue();
    assertThat(service.summary(10L).formalExamSessionId()).isEqualTo("9001");
    assertThatThrownBy(() -> service.autoAllocate(10L)).hasMessageContaining("已锁定");
    assertThatThrownBy(() -> service.assign(10L, 100L, new CompetitionStagePaperAssignmentChangeReq(500L)))
        .hasMessageContaining("已锁定");
    assertThatThrownBy(() -> service.batchAssign(10L, new CompetitionStagePaperAssignmentBatchReq(
        List.of(new CompetitionStagePaperAssignmentItemReq(100L, 500L)))))
        .hasMessageContaining("已锁定");
    assertThatThrownBy(() -> service.save(10L, new CompetitionStagePaperAssignmentBatchReq(
        List.of(new CompetitionStagePaperAssignmentItemReq(100L, 500L)))))
        .hasMessageContaining("已锁定");
    assertThatThrownBy(() -> service.confirm(10L, new CompetitionStagePaperAssignmentBatchReq(List.of())))
        .hasMessageContaining("已锁定");
    verify(assignments, never()).insert(any(CompetitionStagePaperAssignmentEntity.class));
    verify(assignments, never()).updateById(any(CompetitionStagePaperAssignmentEntity.class));
  }

  @Test void finishedFormalExamAlsoLocksAssignmentWrites() {
    candidates(1); papers(2);
    var runtime = new CompetitionFormalExamSessionEntity();
    runtime.setId(9002L);
    runtime.setStatus("finished");
    when(formalSessions.selectLatestByStage(1L, 10L)).thenReturn(runtime);
    assertThatThrownBy(() -> service.autoAllocate(10L)).hasMessageContaining("已锁定");
    verify(assignments, never()).insert(any(CompetitionStagePaperAssignmentEntity.class));
  }

  @Test void practicalAssignmentUsesStageBoundPublishedPracticalPapers() {
    practicalCandidates(1);
    var binding = new CompetitionPracticalPaperBindingEntity();
    binding.setId(800L);
    binding.setTenantId(1L);
    binding.setEventId(20L);
    binding.setStageId(10L);
    binding.setEventWorkTypeId(30L);
    binding.setEventWorkTypeLevelId(40L);
    binding.setPaperSnapshotId(900L);
    when(practicalBindings.selectList(any())).thenReturn(List.of(binding));

    var paper = new CompetitionMakeExamPaperSnapshotEntity();
    paper.setId(900L);
    paper.setEventId(20L);
    paper.setEventWorkTypeId(30L);
    paper.setEventWorkTypeLevelId(40L);
    paper.setContentType("practical");
    paper.setPaperCode("P");
    paper.setVersionNo(1);
    paper.setTitle("实操试卷");
    paper.setStatus("published");
    when(practicalSnapshots.selectList(any())).thenReturn(List.of(paper));
    when(assignments.selectByScopeForUpdate(1L, 20L, 10L, 30L, 40L, "practical"))
        .thenReturn(List.of());
    when(formalSessions.selectLatestByStageAndExamType(1L, 10L, "practical"))
        .thenReturn(null);

    var summary = service.summary(10L, "practical");

    assertThat(summary.paperCount()).isEqualTo(1);
    assertThat(summary.papers()).singleElement()
        .satisfies(item -> {
          assertThat(item.paperCode()).isEqualTo("P");
          assertThat(item.title()).isEqualTo("实操试卷");
        });
    service.autoAllocate(10L, "practical");
    var capture = ArgumentCaptor.forClass(CompetitionStagePaperAssignmentEntity.class);
    verify(assignments).insert(capture.capture());
    assertThat(capture.getValue().getContentType()).isEqualTo("practical");
    verify(papers, never()).selectList(any());
  }
}
