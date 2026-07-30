package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.competition.dto.CompetitionContestantWorkTypeConfirmReq;
import com.guoyu.platform.competition.dto.CompetitionEventWorkTypeCreateReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventOrgEntity;
import com.guoyu.platform.competition.mapper.CompetitionEventOrgMapper;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.UserMapper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CompetitionWorkTypeApplicationScopeTest {

  private final CompetitionEventService eventService =
      Mockito.mock(CompetitionEventService.class);
  private final CompetitionEventWorkTypeDomainService eventWorkTypeDomainService =
      Mockito.mock(CompetitionEventWorkTypeDomainService.class);
  private final CompetitionContestantWorkTypeDomainService contestantDomainService =
      Mockito.mock(CompetitionContestantWorkTypeDomainService.class);
  private final ExamWorkTypeMapper examWorkTypeMapper =
      Mockito.mock(ExamWorkTypeMapper.class);
  private final CompetitionEventOrgMapper eventOrgMapper =
      Mockito.mock(CompetitionEventOrgMapper.class);
  private final UserMapper userMapper = Mockito.mock(UserMapper.class);
  private final CompetitionEventWorkTypeApplicationService eventWorkTypeService =
      new CompetitionEventWorkTypeApplicationService(
          eventService, eventWorkTypeDomainService, examWorkTypeMapper,
          eventOrgMapper, userMapper);
  private final CompetitionContestantWorkTypeApplicationService contestantService =
      new CompetitionContestantWorkTypeApplicationService(
          eventService, contestantDomainService, eventOrgMapper, userMapper);

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void platformParticipantCanReadWorkTypesButCannotManageEvenIfAuthorityWasGranted() {
    authenticateEnterpriseUser();
    when(eventService.getById(1L, 200L)).thenReturn(platformEvent());
    when(userMapper.selectOne(any())).thenReturn(user(3001L));
    when(eventOrgMapper.selectOne(any())).thenReturn(activeRelation(3001L));
    when(eventWorkTypeDomainService.list(1L, 200L)).thenReturn(List.of());

    assertThat(eventWorkTypeService.list(200L)).isEmpty();
    verify(eventWorkTypeDomainService).list(1L, 200L);

    assertThatThrownBy(() -> eventWorkTypeService.create(
        200L, new CompetitionEventWorkTypeCreateReq(100L, "焊工", "active", 1)))
        .isInstanceOf(BusinessException.class)
        .hasMessage("平台赛事工种仅平台管理员可管理");
    verify(eventWorkTypeDomainService, never()).create(any(), any(), any());
  }

  @Test
  void platformParticipantReconciliationAndConfirmationCarryCurrentOrganizationScope() {
    authenticateEnterpriseUser();
    when(eventService.getById(1L, 200L)).thenReturn(platformEvent());
    when(userMapper.selectOne(any())).thenReturn(user(3001L));
    when(eventOrgMapper.selectOne(any())).thenReturn(activeRelation(3001L));
    when(contestantDomainService.reconcile(1L, 200L, 3001L)).thenReturn(List.of());

    assertThat(contestantService.reconcile(200L)).isEmpty();
    verify(contestantDomainService).reconcile(1L, 200L, 3001L);

    CompetitionContestantWorkTypeConfirmReq req =
        new CompetitionContestantWorkTypeConfirmReq(10L, "企业复核");
    contestantService.confirm(200L, 21L, req);
    verify(contestantDomainService).confirm(1L, 200L, 21L, 1000L, req, 3001L);
  }

  private void authenticateEnterpriseUser() {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        1000L, 1L, "enterprise-admin", "enterprise-admin", "business",
        List.of("ENTERPRISE_ADMIN"),
        List.of(
            "competition:event-work-type:view",
            "competition:event-work-type:manage"),
        "test@example.com");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(
            principal, "token", principal.getAuthorities()));
  }

  private CompetitionEventEntity platformEvent() {
    CompetitionEventEntity event = new CompetitionEventEntity();
    event.setId(200L);
    event.setTenantId(1L);
    event.setScope("platform");
    event.setStatus("draft");
    return event;
  }

  private UserEntity user(Long orgId) {
    UserEntity user = new UserEntity();
    user.setId(1000L);
    user.setTenantId(1L);
    user.setOrgId(orgId);
    user.setDeleted(0);
    return user;
  }

  private CompetitionEventOrgEntity activeRelation(Long orgId) {
    CompetitionEventOrgEntity relation = new CompetitionEventOrgEntity();
    relation.setTenantId(1L);
    relation.setEventId(200L);
    relation.setOrgId(orgId);
    relation.setStatus("active");
    relation.setDeleted(0);
    return relation;
  }
}
