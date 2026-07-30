package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.competition.dto.CompetitionScoreRuleCreateReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventOrgEntity;
import com.guoyu.platform.competition.enums.CompetitionEventScope;
import com.guoyu.platform.competition.mapper.CompetitionEventOrgMapper;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.UserMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CompetitionScoreRuleApplicationServiceTest {

  private final CompetitionScoreRuleDomainService domainService =
      Mockito.mock(CompetitionScoreRuleDomainService.class);
  private final CompetitionEventService eventService = Mockito.mock(CompetitionEventService.class);
  private final CompetitionEventOrgMapper eventOrgMapper =
      Mockito.mock(CompetitionEventOrgMapper.class);
  private final UserMapper userMapper = Mockito.mock(UserMapper.class);
  private final CompetitionScoreRuleApplicationService service =
      new CompetitionScoreRuleApplicationService(
          domainService, eventService, eventOrgMapper, userMapper);

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void activePlatformParticipantMayReadButCannotManagePlatformRule() {
    authenticate("enterprise-admin", List.of("ENTERPRISE_ADMIN"));
    when(eventService.getById(1L, 1001L))
        .thenReturn(event(CompetitionEventScope.PLATFORM.getCode(), null));
    when(userMapper.selectOne(any())).thenReturn(user(3001L));
    when(eventOrgMapper.selectOne(any())).thenReturn(activeRelation(3001L));
    when(domainService.list(1L, 1001L, null, null)).thenReturn(List.of());

    assertThat(service.list(1001L, null, null)).isEmpty();

    assertThatThrownBy(() -> service.create(1001L, request()))
        .isInstanceOfSatisfying(BusinessException.class,
            ex -> assertThat(ex.getCode()).isEqualTo("403"))
        .hasMessage("仅平台管理员可管理平台赛事评分规则");
    verify(domainService, never()).create(any(), any(), any(), any());
  }

  @Test
  void enterpriseInternalOwnerMayManageRule() {
    authenticate("enterprise-admin", List.of("ENTERPRISE_ADMIN"));
    when(eventService.getById(1L, 1001L))
        .thenReturn(event(CompetitionEventScope.ENTERPRISE_INTERNAL.getCode(), 3001L));
    when(userMapper.selectOne(any())).thenReturn(user(3001L));

    service.create(1001L, request());

    verify(domainService).create(1L, 9L, 1001L, request());
  }

  @Test
  void platformAdminMayManagePlatformRule() {
    authenticate("platform-admin", List.of("PLATFORM_ADMIN"));
    when(eventService.getById(1L, 1001L))
        .thenReturn(event(CompetitionEventScope.PLATFORM.getCode(), null));

    service.create(1001L, request());

    verify(domainService).create(1L, 9L, 1001L, request());
  }

  private void authenticate(String username, List<String> roles) {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        9L, 1L, username, username, "business", roles,
        List.of("competition:score-rule:view", "competition:score-rule:manage"),
        "test@example.com");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
  }

  private CompetitionEventEntity event(String scope, Long ownerOrgId) {
    CompetitionEventEntity entity = new CompetitionEventEntity();
    entity.setId(1001L);
    entity.setTenantId(1L);
    entity.setScope(scope);
    entity.setOwnerOrgId(ownerOrgId);
    entity.setStatus("published");
    return entity;
  }

  private UserEntity user(Long orgId) {
    UserEntity entity = new UserEntity();
    entity.setId(9L);
    entity.setTenantId(1L);
    entity.setOrgId(orgId);
    entity.setDeleted(0);
    return entity;
  }

  private CompetitionEventOrgEntity activeRelation(Long orgId) {
    CompetitionEventOrgEntity entity = new CompetitionEventOrgEntity();
    entity.setTenantId(1L);
    entity.setEventId(1001L);
    entity.setOrgId(orgId);
    entity.setStatus("active");
    entity.setDeleted(0);
    return entity;
  }

  private CompetitionScoreRuleCreateReq request() {
    return new CompetitionScoreRuleCreateReq(
        2001L, 3001L, "B", "THEORY_90",
        new BigDecimal("40"), new BigDecimal("60"));
  }
}
