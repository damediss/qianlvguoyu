package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeStageEntity;
import com.guoyu.platform.competition.entity.CompetitionStageContentEntity;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeLevelMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeStageMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageContentMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageMapper;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CompetitionEventPublishReadinessServiceTest {

  private final CompetitionEventWorkTypeMapper workTypeMapper =
      Mockito.mock(CompetitionEventWorkTypeMapper.class);
  private final CompetitionEventWorkTypeLevelMapper levelMapper =
      Mockito.mock(CompetitionEventWorkTypeLevelMapper.class);
  private final CompetitionStageMapper stageMapper = Mockito.mock(CompetitionStageMapper.class);
  private final CompetitionStageContentMapper contentMapper =
      Mockito.mock(CompetitionStageContentMapper.class);
  private final CompetitionEventWorkTypeStageMapper workTypeStageMapper =
      Mockito.mock(CompetitionEventWorkTypeStageMapper.class);
  private final CompetitionEventPublishReadinessService service =
      new CompetitionEventPublishReadinessService(
          workTypeMapper, levelMapper, stageMapper, contentMapper, workTypeStageMapper);

  @Test
  void basicEventIsReadyWithoutWorkTypesOrStages() {
    CompetitionEventEntity event = event();
    when(workTypeMapper.selectList(any())).thenReturn(List.of());

    var readiness = service.evaluate(1L, 1000L, event, principal());

    assertThat(readiness.ready()).isTrue();
    assertThat(readiness.activeWorkTypeCount()).isZero();
    assertThat(readiness.selectedLevelCount()).isZero();
    assertThat(readiness.scopes()).isEmpty();
  }

  @Test
  void unconfirmedScopeDoesNotBlockEventPublishReadiness() {
    CompetitionEventEntity event = event();
    CompetitionEventWorkTypeEntity workType = workType();
    CompetitionEventWorkTypeLevelEntity level = level("5", "draft");
    when(workTypeMapper.selectList(any())).thenReturn(List.of(workType));
    when(levelMapper.selectList(any())).thenReturn(List.of(level));
    when(stageMapper.selectList(any())).thenReturn(List.of(stage()));
    when(contentMapper.selectList(any())).thenReturn(List.of(content()));
    when(workTypeStageMapper.selectList(any())).thenReturn(List.of(stageConfig()));

    var readiness = service.evaluate(1L, 1000L, event, principal());

    assertThat(readiness.ready()).isTrue();
    assertThat(readiness.scopes()).singleElement().satisfies(scope ->
        assertThat(scope.stagePlanStatus()).isEqualTo("draft"));
    assertThat(event.getStatus()).isEqualTo("draft");
    assertThat(event.getStagePlanStatus()).isNull();
  }

  @Test
  void scopeConfirmationStillRequiresCompleteStageConfiguration() {
    CompetitionEventEntity event = event();
    CompetitionEventWorkTypeEntity workType = workType();
    CompetitionEventWorkTypeLevelEntity level = level("5", "draft");
    when(stageMapper.selectList(any())).thenReturn(List.of());

    var readiness = service.evaluateForScopeConfirmation(1L, event, workType, level);

    assertThat(readiness.ready()).isFalse();
    assertThat(readiness.checks()).anyMatch(check ->
        "SCOPE_STAGES_MISSING".equals(check.code()) && !check.passed());
  }

  @Test
  void scopeConfirmationAllowsSimulationWhenScoresAreImported() {
    CompetitionEventEntity event = event();
    CompetitionEventWorkTypeEntity workType = workType();
    CompetitionEventWorkTypeLevelEntity level = level("5", "draft");
    when(stageMapper.selectList(any())).thenReturn(List.of(stage()));
    when(contentMapper.selectList(any())).thenReturn(List.of(
        content("theory", "40.00"),
        content("simulation", "30.00"),
        content("practical", "30.00")));
    when(workTypeStageMapper.selectList(any())).thenReturn(List.of(stageConfig()));

    var readiness = service.evaluateForScopeConfirmation(1L, event, workType, level);

    assertThat(readiness.ready()).isTrue();
    assertThat(readiness.checks()).anyMatch(check ->
        "SIMULATION_PROVIDER_UNAVAILABLE".equals(check.code()) && check.passed());
  }

  @Test
  void scopeConfirmationRequiresEventDatesAfterEarlyPublish() {
    CompetitionEventEntity event = event();
    event.setStartAt(null);
    event.setEndAt(null);
    CompetitionEventWorkTypeEntity workType = workType();
    CompetitionEventWorkTypeLevelEntity level = level("5", "draft");
    when(stageMapper.selectList(any())).thenReturn(List.of(stage()));
    when(contentMapper.selectList(any())).thenReturn(List.of(content()));
    when(workTypeStageMapper.selectList(any())).thenReturn(List.of(stageConfig()));

    var readiness = service.evaluateForScopeConfirmation(1L, event, workType, level);

    assertThat(readiness.ready()).isFalse();
    assertThat(readiness.checks()).anyMatch(check ->
        "EVENT_TIME_REQUIRED_FOR_CONFIRM".equals(check.code()) && !check.passed());
  }

  private CompetitionEventEntity event() {
    CompetitionEventEntity event = new CompetitionEventEntity();
    event.setId(1001L);
    event.setTenantId(1L);
    event.setName("赛事");
    event.setCode("EVENT-1001");
    event.setScope("platform");
    event.setRegistrationAuditRequired(0);
    event.setStartAt(LocalDateTime.of(2026, 8, 1, 9, 0));
    event.setEndAt(LocalDateTime.of(2026, 8, 2, 18, 0));
    event.setStatus("draft");
    event.setDeleted(0);
    return event;
  }

  private CompetitionEventWorkTypeEntity workType() {
    CompetitionEventWorkTypeEntity workType = new CompetitionEventWorkTypeEntity();
    workType.setId(2001L);
    workType.setEventId(1001L);
    workType.setWorkTypeId(3001L);
    workType.setDisplayName("焊工");
    workType.setStatus("active");
    workType.setSortOrder(1);
    return workType;
  }

  private CompetitionEventWorkTypeLevelEntity level(String code, String status) {
    CompetitionEventWorkTypeLevelEntity level = new CompetitionEventWorkTypeLevelEntity();
    level.setId(4001L);
    level.setEventId(1001L);
    level.setEventWorkTypeId(2001L);
    level.setLevelCode(code);
    level.setLevelName("初级工");
    level.setStagePlanStatus(status);
    level.setDeleted(0);
    return level;
  }

  private CompetitionStageEntity stage() {
    CompetitionStageEntity stage = new CompetitionStageEntity();
    stage.setId(5001L);
    stage.setEventId(1001L);
    stage.setEventWorkTypeId(2001L);
    stage.setEventWorkTypeLevelCode("5");
    stage.setStageType("preliminary");
    stage.setName("初赛");
    stage.setSortOrder(20);
    stage.setStartAt(LocalDateTime.of(2026, 8, 1, 10, 0));
    stage.setEndAt(LocalDateTime.of(2026, 8, 1, 12, 0));
    stage.setStatus("draft");
    return stage;
  }

  private CompetitionStageContentEntity content() {
    return content("theory", "100.00");
  }

  private CompetitionStageContentEntity content(String type, String weight) {
    CompetitionStageContentEntity content = new CompetitionStageContentEntity();
    content.setEventId(1001L);
    content.setStageId(5001L);
    content.setContentType(type);
    content.setStatus("enabled");
    content.setWeight(new BigDecimal(weight));
    content.setDeleted(0);
    return content;
  }

  private CompetitionEventWorkTypeStageEntity stageConfig() {
    CompetitionEventWorkTypeStageEntity config = new CompetitionEventWorkTypeStageEntity();
    config.setId(7001L);
    config.setEventId(1001L);
    config.setEventWorkTypeId(2001L);
    config.setEventWorkTypeLevelCode("5");
    config.setStageId(5001L);
    config.setEnabled(1);
    config.setPromotionRuleType("PERCENTAGE");
    config.setPromotionPercentage(50);
    config.setDeleted(0);
    return config;
  }

  private PlatformUserPrincipal principal() {
    return new PlatformUserPrincipal(1000L, 1L, "admin", "admin", "business",
        List.of("PLATFORM_ADMIN"), List.of("competition:event:publish"), "test@example.com");
  }
}
