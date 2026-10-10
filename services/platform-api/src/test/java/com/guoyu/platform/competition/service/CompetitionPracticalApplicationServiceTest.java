package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.guoyu.platform.competition.dto.CompetitionPracticalPaperBindingReq;
import com.guoyu.platform.competition.entity.CompetitionPracticalPaperBindingEntity;
import com.guoyu.platform.competition.mapper.CompetitionPracticalPaperBindingMapper;
import com.guoyu.platform.competition.vo.CompetitionMakeExamSnapshotVO;
import com.guoyu.platform.competition.vo.CompetitionPracticalPaperBindingVO;
import com.guoyu.platform.competition.vo.CompetitionStageVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CompetitionPracticalApplicationServiceTest {
  private final CompetitionStageApplicationService stageService =
      Mockito.mock(CompetitionStageApplicationService.class);
  private final CompetitionStageExecutionPlanService planService =
      Mockito.mock(CompetitionStageExecutionPlanService.class);
  private final CompetitionPracticalRuleService ruleService =
      Mockito.mock(CompetitionPracticalRuleService.class);
  private final CompetitionPracticalExecutionService executionService =
      Mockito.mock(CompetitionPracticalExecutionService.class);
  private final CompetitionMakeExamPlanApplicationService makeExamPlanService =
      Mockito.mock(CompetitionMakeExamPlanApplicationService.class);
  private final CompetitionPracticalPaperBindingMapper paperBindingMapper =
      Mockito.mock(CompetitionPracticalPaperBindingMapper.class);
  private final CompetitionPracticalApplicationService service =
      new CompetitionPracticalApplicationService(
          stageService, planService, ruleService, executionService,
          makeExamPlanService, paperBindingMapper);

  @BeforeEach
  void authenticate() {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        1000L, 10159L, "admin", "管理员", "platform",
        List.of("PLATFORM_ADMIN"), List.of("competition:stage-plan:manage"), null);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
  }

  @AfterEach
  void clearAuthentication() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void bindsPublishedPaperToStageWithoutPracticalRule() {
    when(stageService.assertCanManageStagePlan(10L)).thenReturn(stage());
    givenPublishedSnapshot();
    when(paperBindingMapper.selectOne(any())).thenReturn(null);
    when(paperBindingMapper.insert(any(CompetitionPracticalPaperBindingEntity.class))).thenReturn(1);

    CompetitionPracticalPaperBindingVO result = service.bindPaperToStage(
        10L, new CompetitionPracticalPaperBindingReq(30L, 40L, 500L));

    assertThat(result.eventId()).isEqualTo("20");
    assertThat(result.stageId()).isEqualTo("10");
    assertThat(result.eventWorkTypeId()).isEqualTo("30");
    assertThat(result.eventWorkTypeLevelId()).isEqualTo("40");
    assertThat(result.paperSnapshotId()).isEqualTo("500");
    verify(paperBindingMapper).insert(any(CompetitionPracticalPaperBindingEntity.class));
    verifyNoInteractions(ruleService);
  }

  @Test
  void returnsExistingBindingWhenPaperIsClickedAgain() {
    when(stageService.assertCanManageStagePlan(10L)).thenReturn(stage());
    givenPublishedSnapshot();
    CompetitionPracticalPaperBindingEntity existing = new CompetitionPracticalPaperBindingEntity();
    existing.setId(900L);
    existing.setEventId(20L);
    existing.setStageId(10L);
    existing.setEventWorkTypeId(30L);
    existing.setEventWorkTypeLevelId(40L);
    existing.setPaperSnapshotId(500L);
    when(paperBindingMapper.selectOne(any())).thenReturn(existing);

    CompetitionPracticalPaperBindingVO result = service.bindPaperToStage(
        10L, new CompetitionPracticalPaperBindingReq(30L, 40L, 500L));

    assertThat(result.id()).isEqualTo("900");
    assertThat(result.paperSnapshotId()).isEqualTo("500");
    verify(paperBindingMapper, never()).insert(any(CompetitionPracticalPaperBindingEntity.class));
    verifyNoInteractions(ruleService);
  }

  private void givenPublishedSnapshot() {
    when(makeExamPlanService.requirePublishedSnapshot(
        eq(20L), eq(500L), eq("practical"), eq(30L), eq(40L)))
        .thenReturn(new CompetitionMakeExamSnapshotVO(
            "500", "600", "20", "30", "40", "practical", "P1", 1,
            "实操蓝图 P1", 90, 1, new BigDecimal("100"), new BigDecimal("60"),
            "published", List.of()));
  }

  private CompetitionStageVO stage() {
    CompetitionStageVO stage = new CompetitionStageVO();
    stage.setId(10L);
    stage.setEventId(20L);
    return stage;
  }
}
