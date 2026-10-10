package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.competition.dto.CompetitionRegistrationQueryReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventOrgEntity;
import com.guoyu.platform.competition.mapper.CompetitionEventOrgMapper;
import com.guoyu.platform.competition.mapper.CompetitionRegistrationQueryRow;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(OutputCaptureExtension.class)
class CompetitionRegistrationApplicationServiceTest {

  private final CompetitionRegistrationService registrationService =
      Mockito.mock(CompetitionRegistrationService.class);
  private final CompetitionEventService eventService = Mockito.mock(CompetitionEventService.class);
  private final CompetitionEventOrgMapper eventOrgMapper = Mockito.mock(CompetitionEventOrgMapper.class);
  private final OrgMapper orgMapper = Mockito.mock(OrgMapper.class);
  private final UserMapper userMapper = Mockito.mock(UserMapper.class);
  private final CompetitionRegistrationApplicationService service =
      new CompetitionRegistrationApplicationService(
          registrationService, eventService, eventOrgMapper, orgMapper, userMapper);

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void rejectsEnterpriseUserQueryingAnotherOrganization() {
    authenticateEnterpriseUser();
    when(userMapper.selectOne(any())).thenReturn(user(3001L));
    CompetitionRegistrationQueryReq req = new CompetitionRegistrationQueryReq();
    req.setOrgId(4001L);

    assertThatThrownBy(() -> service.page(req))
        .isInstanceOf(BusinessException.class)
        .hasMessage("无权查询其他企业的参赛资格");
    verify(registrationService, never()).page(any(), any());
  }

  @Test
  void rejectsEnterpriseUserWithoutActivePlatformEventParticipation(CapturedOutput output) {
    authenticateEnterpriseUser();
    when(userMapper.selectOne(any())).thenReturn(user(3001L));
    when(orgMapper.selectOne(any())).thenReturn(org(3001L));
    when(eventService.getById(1L, 1001L)).thenReturn(event(1001L));
    when(eventOrgMapper.selectOne(any())).thenReturn(null);
    CompetitionRegistrationQueryReq req = new CompetitionRegistrationQueryReq();
    req.setEventId(1001L);

    assertThatThrownBy(() -> service.page(req))
        .isInstanceOf(BusinessException.class)
        .hasMessage("当前企业不是有效赛事参与企业");
    org.assertj.core.api.Assertions.assertThat(output).contains(
        "Registration event access denied because active organization relation is missing, "
            + "userId=2000, tenantId=1, eventId=1001, orgId=3001");
    verify(registrationService, never()).page(any(), any());
  }

  @Test
  void logsAndRejectsEnterpriseInternalEventOutsideOwnerOrganization(CapturedOutput output) {
    authenticateEnterpriseUser();
    when(userMapper.selectOne(any())).thenReturn(user(3001L));
    when(orgMapper.selectOne(any())).thenReturn(org(3001L));
    CompetitionEventEntity event = event(1001L);
    event.setScope("enterprise_internal");
    event.setOwnerOrgId(4001L);
    when(eventService.getById(1L, 1001L)).thenReturn(event);
    CompetitionRegistrationQueryReq req = new CompetitionRegistrationQueryReq();
    req.setEventId(1001L);

    assertThatThrownBy(() -> service.page(req))
        .isInstanceOf(BusinessException.class)
        .hasMessage("无权查询该赛事的参赛资格");
    org.assertj.core.api.Assertions.assertThat(output).contains(
        "Registration event access denied for enterprise-internal event, "
            + "userId=2000, tenantId=1, eventId=1001, orgId=3001");
    verify(registrationService, never()).page(any(), any());
  }

  @Test
  void scopesPlatformEventQueryToActiveEnterpriseParticipation() {
    authenticateEnterpriseUser();
    when(userMapper.selectOne(any())).thenReturn(user(3001L));
    when(orgMapper.selectOne(any())).thenReturn(org(3001L));
    when(eventService.getById(1L, 1001L)).thenReturn(event(1001L));
    CompetitionEventOrgEntity relation = new CompetitionEventOrgEntity();
    relation.setEventId(1001L);
    relation.setOrgId(3001L);
    relation.setStatus("active");
    when(eventOrgMapper.selectOne(any())).thenReturn(relation);
    when(registrationService.page(any(), any())).thenReturn(new Page<CompetitionRegistrationQueryRow>(1, 20));
    CompetitionRegistrationQueryReq req = new CompetitionRegistrationQueryReq();
    req.setEventId(1001L);

    service.page(req);

    org.assertj.core.api.Assertions.assertThat(req.getOrgId()).isEqualTo(3001L);
    verify(registrationService).page(1L, req);
  }

  private void authenticateEnterpriseUser() {
    var principal = new com.guoyu.platform.security.PlatformUserPrincipal(
        2000L, 1L, "enterprise-admin", "Enterprise Admin", "business",
        List.of("ENTERPRISE_ADMIN"), List.of("competition:registration:view"), null);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
  }

  private UserEntity user(Long orgId) {
    UserEntity user = new UserEntity();
    user.setId(2000L);
    user.setTenantId(1L);
    user.setOrgId(orgId);
    return user;
  }

  private OrgEntity org(Long orgId) {
    OrgEntity org = new OrgEntity();
    org.setId(orgId);
    org.setTenantId(1L);
    return org;
  }

  private CompetitionEventEntity event(Long eventId) {
    CompetitionEventEntity event = new CompetitionEventEntity();
    event.setId(eventId);
    event.setTenantId(1L);
    event.setScope("platform");
    event.setStatus("draft");
    return event;
  }
}
