package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.mapper.CompetitionPlayerQueryRow;
import com.guoyu.platform.competition.mapper.CompetitionRegistrationMapper;
import com.guoyu.platform.competition.player.domain.CompetitionPlayerIdentityDomainService;
import com.guoyu.platform.competition.vo.CompetitionPlayerVO;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CompetitionPlayerManagementServiceTest {

  private final CompetitionEventService eventService = Mockito.mock(CompetitionEventService.class);
  private final com.guoyu.platform.competition.mapper.CompetitionEventOrgMapper eventOrgMapper =
      Mockito.mock(com.guoyu.platform.competition.mapper.CompetitionEventOrgMapper.class);
  private final CompetitionEventWorkTypeDomainService eventWorkTypeService =
      Mockito.mock(CompetitionEventWorkTypeDomainService.class);
  private final CompetitionRegistrationMapper registrationMapper =
      Mockito.mock(CompetitionRegistrationMapper.class);
  private final com.guoyu.platform.competition.mapper.CompetitionRosterItemMapper rosterItemMapper =
      Mockito.mock(com.guoyu.platform.competition.mapper.CompetitionRosterItemMapper.class);
  private final com.guoyu.platform.competition.mapper.CompetitionContestantProfileMapper profileMapper =
      Mockito.mock(com.guoyu.platform.competition.mapper.CompetitionContestantProfileMapper.class);
  private final com.guoyu.platform.competition.mapper.CompetitionEventMemberMapper memberMapper =
      Mockito.mock(com.guoyu.platform.competition.mapper.CompetitionEventMemberMapper.class);
  private final CompetitionRosterImportService rosterImportService =
      Mockito.mock(CompetitionRosterImportService.class);
  private final CompetitionRegistrationService registrationService =
      Mockito.mock(CompetitionRegistrationService.class);
  private final CompetitionPlayerIdentityDomainService playerIdentityService =
      Mockito.mock(CompetitionPlayerIdentityDomainService.class);
  private final CompetitionContestantScopeChangeGuard scopeChangeGuard =
      Mockito.mock(CompetitionContestantScopeChangeGuard.class);
  private final OrgMapper orgMapper = Mockito.mock(OrgMapper.class);
  private final UserMapper userMapper = Mockito.mock(UserMapper.class);
  private final CompetitionPlayerManagementService service = new CompetitionPlayerManagementService(
      eventService,
      eventOrgMapper,
      eventWorkTypeService,
      registrationMapper,
      rosterItemMapper,
      profileMapper,
      memberMapper,
      rosterImportService,
      registrationService,
      playerIdentityService,
      scopeChangeGuard,
      orgMapper,
      userMapper);

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void keepsListValuesMaskedButReturnsFullValuesForAuthorizedEditDetails() {
    authenticateSuperAdmin();
    CompetitionPlayerQueryRow row = playerRow();
    when(eventService.getById(1L, 1001L)).thenReturn(event());
    when(orgMapper.selectOne(any())).thenReturn(org());
    when(registrationMapper.selectPlayerByRegistrationId(1L, 5001L)).thenReturn(row);

    CompetitionPlayerVO editDetail = service.getForEdit(5001L);

    assertThat(editDetail.getIdCardNo()).isEqualTo("11010519491231002X");
    assertThat(editDetail.getMobile()).isEqualTo("13800138001");

    Page<CompetitionPlayerQueryRow> page = new Page<>(1, 10);
    page.setRecords(List.of(row));
    page.setTotal(1);
    when(registrationMapper.selectPlayerPage(
        any(Page.class),
        eq(1L),
        eq(1001L),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        isNull())).thenReturn(page);

    var query = new com.guoyu.platform.competition.dto.CompetitionPlayerQueryReq();
    query.setEventId(1001L);
    CompetitionPlayerVO listValue = service.page(query).items().get(0);

    assertThat(listValue.getIdCardNo()).isEqualTo("110********002X");
    assertThat(listValue.getMobile()).isEqualTo("138****8001");
  }

  private void authenticateSuperAdmin() {
    var principal = new com.guoyu.platform.security.PlatformUserPrincipal(
        1000L,
        1L,
        "admin",
        "平台管理员",
        "admin",
        List.of("PLATFORM_ADMIN"),
        List.of("competition:player:view", "competition:player:update"),
        "test@example.com");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
  }

  private CompetitionPlayerQueryRow playerRow() {
    CompetitionPlayerQueryRow row = new CompetitionPlayerQueryRow();
    row.setRegistrationId(5001L);
    row.setEventId(1001L);
    row.setEventName("2026 技能竞赛");
    row.setOrgId(3001L);
    row.setOrgName("示例企业");
    row.setPlayerId(7001L);
    row.setMemberId(8001L);
    row.setEventWorkTypeId(9001L);
    row.setEventWorkTypeName("电工");
    row.setName("张三");
    row.setIdCardNo("11010519491231002X");
    row.setMobile("13800138001");
    row.setLevel("高级");
    row.setStatus("eligible_unactivated");
    return row;
  }

  private CompetitionEventEntity event() {
    CompetitionEventEntity event = new CompetitionEventEntity();
    event.setId(1001L);
    event.setTenantId(1L);
    return event;
  }

  private OrgEntity org() {
    OrgEntity org = new OrgEntity();
    org.setId(3001L);
    org.setTenantId(1L);
    org.setDeleted(0);
    return org;
  }
}
