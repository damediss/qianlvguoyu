package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.competition.dto.CompetitionTaskTemplateCreateReq;
import com.guoyu.platform.competition.dto.CompetitionTaskTemplateUpdateReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.entity.CompetitionTaskTemplateEntity;
import com.guoyu.platform.system.mapper.UserMapper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CompetitionTaskTemplateApplicationServiceTest {

  private final CompetitionEventService eventService = Mockito.mock(CompetitionEventService.class);
  private final CompetitionStageService stageService = Mockito.mock(CompetitionStageService.class);
  private final CompetitionTaskTemplateService templateService = Mockito.mock(CompetitionTaskTemplateService.class);
  private final UserMapper userMapper = Mockito.mock(UserMapper.class);
  private final CompetitionTaskTemplateApplicationService service =
      new CompetitionTaskTemplateApplicationService(eventService, stageService, templateService, userMapper);

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void publishedEventCannotCreateTaskTemplate() {
    authenticate();
    when(stageService.getById(1L, 5001L)).thenReturn(stage());
    when(eventService.getById(1L, 1001L)).thenReturn(event("published"));

    assertThatThrownBy(() -> service.create(5001L, createRequest()))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .hasMessage("只有草稿状态赛事允许修改任务模板");
    verify(templateService, never()).create(any(), any(), any());
  }

  @Test
  void publishedEventCannotUpdateTaskTemplate() {
    authenticate();
    CompetitionTaskTemplateEntity template = new CompetitionTaskTemplateEntity();
    template.setId(6001L);
    template.setTenantId(1L);
    template.setStageId(5001L);
    when(templateService.getById(1L, 6001L)).thenReturn(template);
    when(stageService.getById(1L, 5001L)).thenReturn(stage());
    when(eventService.getById(1L, 1001L)).thenReturn(event("published"));

    assertThatThrownBy(() -> service.update(6001L, updateRequest()))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .hasMessage("只有草稿状态赛事允许修改任务模板");
    verify(templateService, never()).update(any(), any(), any());
  }

  private void authenticate() {
    var principal = new com.guoyu.platform.security.PlatformUserPrincipal(
        1000L, 1L, "admin", "Admin", "business", List.of("PLATFORM_ADMIN"),
        List.of("competition:task-template:manage"), "test@example.com");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
  }

  private CompetitionStageEntity stage() {
    CompetitionStageEntity stage = new CompetitionStageEntity();
    stage.setId(5001L);
    stage.setTenantId(1L);
    stage.setEventId(1001L);
    return stage;
  }

  private CompetitionEventEntity event(String status) {
    CompetitionEventEntity event = new CompetitionEventEntity();
    event.setId(1001L);
    event.setTenantId(1L);
    event.setScope("platform");
    event.setStatus(status);
    return event;
  }

  private CompetitionTaskTemplateCreateReq createRequest() {
    return new CompetitionTaskTemplateCreateReq(
        "notice_ack", "Notice", null, null, 0, null, null, 1);
  }

  private CompetitionTaskTemplateUpdateReq updateRequest() {
    return new CompetitionTaskTemplateUpdateReq(
        "notice_ack", "Updated", null, null, 0, null, null, 1);
  }
}
