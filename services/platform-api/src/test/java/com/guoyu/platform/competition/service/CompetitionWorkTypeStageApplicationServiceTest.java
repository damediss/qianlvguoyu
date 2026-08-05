package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.competition.dto.CompetitionWorkTypeStageConfigReq;
import com.guoyu.platform.competition.dto.CompetitionWorkTypeStageReplaceReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeStageEntity;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeStageMapper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CompetitionWorkTypeStageApplicationServiceTest {

  private final CompetitionEventService eventService = Mockito.mock(CompetitionEventService.class);
  private final CompetitionEventWorkTypeApplicationService workTypeService =
      Mockito.mock(CompetitionEventWorkTypeApplicationService.class);
  private final CompetitionStageService stageService = Mockito.mock(CompetitionStageService.class);
  private final CompetitionEventWorkTypeStageMapper mapper =
      Mockito.mock(CompetitionEventWorkTypeStageMapper.class);
  private final CompetitionEventWorkTypeMapper workTypeMapper =
      Mockito.mock(CompetitionEventWorkTypeMapper.class);
  private final CompetitionWorkTypeStageApplicationService service =
      new CompetitionWorkTypeStageApplicationService(
          eventService, workTypeService, stageService, mapper, workTypeMapper);

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void draftWorkTypeCanBeEditedWhileAnotherWorkTypeUsesItsOwnConfirmedScope() {
    authenticate();
    CompetitionEventEntity event = event("draft", null);
    CompetitionEventWorkTypeEntity currentWorkType = workType(20L, "draft");
    CompetitionStageEntity stage = stage(30L);
    when(eventService.getById(1L, 10L)).thenReturn(event);
    when(workTypeMapper.selectOne(any())).thenReturn(currentWorkType);
    when(stageService.list(1L, 10L)).thenReturn(List.of(stage));
    when(mapper.selectOne(any())).thenReturn(null);
    when(mapper.selectList(any())).thenReturn(List.of());
    Mockito.doReturn(1).when(mapper).insert(any(CompetitionEventWorkTypeStageEntity.class));

    List<?> result = service.replace(10L, 20L, new CompetitionWorkTypeStageReplaceReq(List.of(
        new CompetitionWorkTypeStageConfigReq(30L, true, 1, "PERCENTAGE", 50, null))));

    assertThat(result).hasSize(1);
    verify(mapper).insert(any(CompetitionEventWorkTypeStageEntity.class));
  }

  @Test
  void legacyEventLevelConfirmationStillLocksWorkTypeEditing() {
    authenticate();
    CompetitionEventEntity event = event("draft", "confirmed");
    when(eventService.getById(1L, 10L)).thenReturn(event);
    when(workTypeMapper.selectOne(any())).thenReturn(workType(20L, "draft"));

    assertThatThrownBy(() -> service.replace(10L, 20L, new CompetitionWorkTypeStageReplaceReq(List.of(
        new CompetitionWorkTypeStageConfigReq(30L, true, 1, "PERCENTAGE", 50, null)))))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class);

    verify(stageService, never()).list(any(), any());
  }

  private void authenticate() {
    var principal = new com.guoyu.platform.security.PlatformUserPrincipal(
        1000L, 1L, "admin", "admin", "business", List.of("PLATFORM_ADMIN"),
        List.of("competition:event-work-type:manage"), "test@example.com");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
  }

  private CompetitionEventEntity event(String status, String stagePlanStatus) {
    CompetitionEventEntity event = new CompetitionEventEntity();
    event.setId(10L);
    event.setTenantId(1L);
    event.setScope("platform");
    event.setStatus(status);
    event.setStagePlanStatus(stagePlanStatus);
    return event;
  }

  private CompetitionEventWorkTypeEntity workType(Long id, String stagePlanStatus) {
    CompetitionEventWorkTypeEntity workType = new CompetitionEventWorkTypeEntity();
    workType.setId(id);
    workType.setTenantId(1L);
    workType.setEventId(10L);
    workType.setWorkTypeId(100L);
    workType.setStagePlanStatus(stagePlanStatus);
    return workType;
  }

  private CompetitionStageEntity stage(Long id) {
    CompetitionStageEntity stage = new CompetitionStageEntity();
    stage.setId(id);
    stage.setTenantId(1L);
    stage.setEventId(10L);
    stage.setStageType("preliminary");
    stage.setName("初赛");
    stage.setSortOrder(1);
    return stage;
  }
}
