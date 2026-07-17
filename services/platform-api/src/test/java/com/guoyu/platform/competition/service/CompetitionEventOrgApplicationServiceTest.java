package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.competition.dto.CompetitionEventOrgCreateReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventOrgEntity;
import com.guoyu.platform.competition.enums.CompetitionEventScope;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CompetitionEventOrgApplicationServiceTest {

  private final CompetitionEventService eventService = Mockito.mock(CompetitionEventService.class);
  private final CompetitionEventOrgService eventOrgService = Mockito.mock(CompetitionEventOrgService.class);
  private final OrgMapper orgMapper = Mockito.mock(OrgMapper.class);
  private final UserMapper userMapper = Mockito.mock(UserMapper.class);
  private final CompetitionEventOrgApplicationService service = new CompetitionEventOrgApplicationService(
      eventService, eventOrgService, orgMapper, userMapper);

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void addsOrganizationToPlatformEvent() {
    authenticate(1000L, 1L, "admin", List.of("PLATFORM_ADMIN"));
    when(eventService.getById(1L, 1001L)).thenReturn(event(1001L, CompetitionEventScope.PLATFORM.getCode(), null));
    when(orgMapper.selectOne(any())).thenReturn(org(3001L, 1L));
    when(eventOrgService.create(1L, 1001L, new CompetitionEventOrgCreateReq(3001L, 20, 1)))
        .thenReturn(relation(2001L, 1001L, 3001L));

    service.create(1001L, new CompetitionEventOrgCreateReq(3001L, 20, 1));

    verify(eventOrgService).create(1L, 1001L, new CompetitionEventOrgCreateReq(3001L, 20, 1));
  }

  @Test
  void rejectsOrganizationFromAnotherTenant() {
    authenticate(1000L, 1L, "admin", List.of("PLATFORM_ADMIN"));
    when(eventService.getById(1L, 1001L)).thenReturn(event(1001L, CompetitionEventScope.PLATFORM.getCode(), null));
    when(orgMapper.selectOne(any())).thenReturn(null);

    assertThatThrownBy(() -> service.create(1001L, new CompetitionEventOrgCreateReq(9001L, null, null)))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .hasMessage("参与企业不存在或不属于当前租户");
    verify(eventOrgService, never()).create(any(), any(), any());
  }

  @Test
  void rejectsAddingOrganizationToEnterpriseInternalEvent() {
    authenticate(1000L, 1L, "enterprise-admin", List.of("ENTERPRISE_ADMIN"));
    when(eventService.getById(1L, 1001L)).thenReturn(event(1001L,
        CompetitionEventScope.ENTERPRISE_INTERNAL.getCode(), 3001L));
    UserEntity user = new UserEntity();
    user.setOrgId(3001L);
    when(userMapper.selectOne(any())).thenReturn(user);

    assertThatThrownBy(() -> service.create(1001L, new CompetitionEventOrgCreateReq(4001L, null, null)))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .hasMessage("企业内部赛事不能配置其他企业");
    verify(eventOrgService, never()).create(any(), any(), any());
  }

  @Test
  void rejectsAccessToEnterpriseEventOutsideCurrentOrganization() {
    authenticate(1000L, 1L, "enterprise-admin", List.of("ENTERPRISE_ADMIN"));
    when(eventService.getById(1L, 1001L)).thenReturn(event(1001L,
        CompetitionEventScope.ENTERPRISE_INTERNAL.getCode(), 4001L));
    UserEntity user = new UserEntity();
    user.setOrgId(3001L);
    when(userMapper.selectOne(any())).thenReturn(user);

    assertThatThrownBy(() -> service.page(1001L, new com.guoyu.platform.competition.dto.CompetitionEventOrgQueryReq()))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .hasMessage("无权访问该赛事");
  }

  private void authenticate(Long userId, Long tenantId, String username, List<String> roles) {
    var principal = new com.guoyu.platform.security.PlatformUserPrincipal(
        userId, tenantId, username, username, "business", roles,
        List.of("competition:event-org:manage"), "test@example.com");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
  }

  private CompetitionEventEntity event(Long id, String scope, Long ownerOrgId) {
    CompetitionEventEntity event = new CompetitionEventEntity();
    event.setId(id);
    event.setTenantId(1L);
    event.setScope(scope);
    event.setOwnerOrgId(ownerOrgId);
    return event;
  }

  private OrgEntity org(Long id, Long tenantId) {
    OrgEntity org = new OrgEntity();
    org.setId(id);
    org.setTenantId(tenantId);
    org.setName("Company " + id);
    org.setDeleted(0);
    return org;
  }

  private CompetitionEventOrgEntity relation(Long id, Long eventId, Long orgId) {
    CompetitionEventOrgEntity relation = new CompetitionEventOrgEntity();
    relation.setId(id);
    relation.setTenantId(1L);
    relation.setEventId(eventId);
    relation.setOrgId(orgId);
    relation.setStatus("invited");
    relation.setImportPermission(1);
    return relation;
  }
}
