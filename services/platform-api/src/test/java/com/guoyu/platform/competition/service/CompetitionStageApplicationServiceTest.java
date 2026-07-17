package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.competition.dto.CompetitionStageCreateReq;
import com.guoyu.platform.competition.dto.CompetitionStageUpdateReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.enums.CompetitionEventScope;
import com.guoyu.platform.competition.enums.CompetitionEventStatus;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CompetitionStageApplicationServiceTest {

  private final CompetitionEventService eventService = Mockito.mock(CompetitionEventService.class);
  private final CompetitionStageService stageService = Mockito.mock(CompetitionStageService.class);
  private final UserMapper userMapper = Mockito.mock(UserMapper.class);
  private final CompetitionStageApplicationService service = new CompetitionStageApplicationService(
      eventService, stageService, userMapper);

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void createsStageForDraftPlatformEvent() {
    authenticate(1000L, 1L, "admin", List.of("PLATFORM_ADMIN"));
    when(eventService.getById(1L, 1001L)).thenReturn(event(1001L, "platform", null, "draft"));
    CompetitionStageEntity created = stage(5001L, 1001L);
    when(stageService.create(1L, 1001L, new CompetitionStageCreateReq(
        "Preparation", "preparation", 1, null, null, null))).thenReturn(created);

    service.create(1001L, new CompetitionStageCreateReq(
        "Preparation", "preparation", 1, null, null, null));

    verify(stageService).create(1L, 1001L, new CompetitionStageCreateReq(
        "Preparation", "preparation", 1, null, null, null));
  }

  @Test
  void updatesStageOnlyWhenEventIsDraft() {
    authenticate(1000L, 1L, "admin", List.of("PLATFORM_ADMIN"));
    when(stageService.getById(1L, 5001L)).thenReturn(stage(5001L, 1001L));
    when(eventService.getById(1L, 1001L)).thenReturn(event(1001L, "platform", null, "draft"));
    CompetitionStageUpdateReq req = new CompetitionStageUpdateReq(
        "Updated", "preparation", 2, null, null, null, null);
    when(stageService.update(1L, 5001L, req)).thenReturn(stage(5001L, 1001L));

    service.update(5001L, req);

    verify(stageService).update(1L, 5001L, req);
  }

  @Test
  void rejectsStageModificationWhenEventIsPublished() {
    authenticate(1000L, 1L, "admin", List.of("PLATFORM_ADMIN"));
    when(stageService.getById(1L, 5001L)).thenReturn(stage(5001L, 1001L));
    when(eventService.getById(1L, 1001L)).thenReturn(event(1001L, "platform", null, "published"));

    assertThatThrownBy(() -> service.update(5001L,
        new CompetitionStageUpdateReq("Updated", "preparation", 2, null, null, null, null)))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .hasMessage("只有草稿状态赛事允许修改赛段");
    verify(stageService, never()).update(any(), any(), any());
  }

  @Test
  void rejectsEnterpriseEventOutsideOwnerOrganization() {
    authenticate(1000L, 1L, "enterprise-admin", List.of("ENTERPRISE_ADMIN"));
    when(eventService.getById(1L, 1001L)).thenReturn(event(1001L,
        CompetitionEventScope.ENTERPRISE_INTERNAL.getCode(), 4001L, "draft"));
    UserEntity user = new UserEntity();
    user.setOrgId(3001L);
    when(userMapper.selectOne(any())).thenReturn(user);

    assertThatThrownBy(() -> service.create(1001L,
        new CompetitionStageCreateReq("Preparation", "preparation", 1, null, null, null)))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .hasMessage("无权访问该赛事");
    verify(stageService, never()).create(any(), any(), any());
  }

  @Test
  void rejectsCrossTenantStageAccess() {
    authenticate(1000L, 1L, "admin", List.of("PLATFORM_ADMIN"));
    when(stageService.getById(1L, 9001L)).thenThrow(
        new com.guoyu.platform.common.exception.BusinessException("404", "赛段不存在"));

    assertThatThrownBy(() -> service.update(9001L,
        new CompetitionStageUpdateReq("Updated", "preparation", 1, null, null, null, null)))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .hasMessage("赛段不存在");
    verify(eventService, never()).getById(any(), any());
  }

  private void authenticate(Long userId, Long tenantId, String username, List<String> roles) {
    var principal = new com.guoyu.platform.security.PlatformUserPrincipal(
        userId, tenantId, username, username, "business", roles,
        List.of("competition:event:view", "competition:event:update"), "test@example.com");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
  }

  private CompetitionEventEntity event(Long id, String scope, Long ownerOrgId, String status) {
    CompetitionEventEntity event = new CompetitionEventEntity();
    event.setId(id);
    event.setTenantId(1L);
    event.setScope(scope);
    event.setOwnerOrgId(ownerOrgId);
    event.setStatus(status);
    return event;
  }

  private CompetitionStageEntity stage(Long id, Long eventId) {
    CompetitionStageEntity stage = new CompetitionStageEntity();
    stage.setId(id);
    stage.setTenantId(1L);
    stage.setEventId(eventId);
    stage.setName("Preparation");
    stage.setStageType("preparation");
    stage.setStatus("draft");
    return stage;
  }
}
