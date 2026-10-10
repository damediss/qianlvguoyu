package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.competition.dto.CompetitionRosterPrecheckReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventOrgEntity;
import com.guoyu.platform.competition.entity.CompetitionRosterImportEntity;
import com.guoyu.platform.competition.entity.CompetitionRosterItemEntity;
import com.guoyu.platform.file.entity.FileEntity;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.competition.mapper.CompetitionEventOrgMapper;
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

class CompetitionRosterImportApplicationServiceTest {

  private final CompetitionEventService eventService = Mockito.mock(CompetitionEventService.class);
  private final CompetitionEventOrgMapper eventOrgMapper = Mockito.mock(CompetitionEventOrgMapper.class);
  private final CompetitionRosterImportService importService = Mockito.mock(CompetitionRosterImportService.class);
  private final CompetitionRegistrationService registrationService = Mockito.mock(CompetitionRegistrationService.class);
  private final FileApplicationService fileService = Mockito.mock(FileApplicationService.class);
  private final OrgMapper orgMapper = Mockito.mock(OrgMapper.class);
  private final UserMapper userMapper = Mockito.mock(UserMapper.class);
  private final CompetitionRosterImportApplicationService service =
      new CompetitionRosterImportApplicationService(
          eventService, eventOrgMapper, importService, registrationService, fileService, orgMapper, userMapper);

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void participatingOrganizationCanPrecheck() {
    authenticate(1000L, 1L, "enterprise-admin");
    when(userMapper.selectOne(any())).thenReturn(user(3001L));
    when(orgMapper.selectOne(any())).thenReturn(org(3001L, 1L));
    when(eventService.getById(1L, 1001L)).thenReturn(event(1001L, "platform", null));
    when(eventOrgMapper.selectOne(any())).thenReturn(relation(1001L, 3001L));
    when(fileService.getExistingFile(1L, 8001L)).thenReturn(file(8001L, 1L));
    when(fileService.readFileBytesById(8001L)).thenReturn(new byte[]{1, 2, 3});
    CompetitionRosterImportEntity batch = batch(7001L, 1001L, 3001L);
    when(importService.precheck(1L, 1001L, 3001L, 8001L, new byte[]{1, 2, 3})).thenReturn(batch);
    when(importService.pageItems(any(), any(), any())).thenReturn(new Page<>(1, 20));

    service.precheck(1001L, new CompetitionRosterPrecheckReq(8001L, 3001L));

    verify(importService).precheck(1L, 1001L, 3001L, 8001L, new byte[]{1, 2, 3});
  }

  @Test
  void nonParticipatingOrganizationCannotPrecheck() {
    authenticate(1000L, 1L, "enterprise-admin");
    when(userMapper.selectOne(any())).thenReturn(user(3001L));
    when(orgMapper.selectOne(any())).thenReturn(org(3001L, 1L));
    when(eventService.getById(1L, 1001L)).thenReturn(event(1001L, "platform", null));
    when(eventOrgMapper.selectOne(any())).thenReturn(null);

    assertThatThrownBy(() -> service.precheck(1001L, new CompetitionRosterPrecheckReq(8001L, 3001L)))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .hasMessage("当前企业不是赛事参与企业");
    verify(fileService, never()).getExistingFile(any(), any());
  }

  @Test
  void crossTenantOrganizationFails() {
    authenticate(1000L, 1L, "enterprise-admin");
    when(userMapper.selectOne(any())).thenReturn(user(3001L));
    when(orgMapper.selectOne(any())).thenReturn(null);

    assertThatThrownBy(() -> service.precheck(1001L, new CompetitionRosterPrecheckReq(8001L, 3001L)))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .hasMessage("企业不存在或不属于当前租户");
    verify(eventService, never()).getById(any(), any());
  }

  @Test
  void nonExcelFileCannotEnterPrecheck() {
    authenticate(1000L, 1L, "enterprise-admin");
    when(userMapper.selectOne(any())).thenReturn(user(3001L));
    when(orgMapper.selectOne(any())).thenReturn(org(3001L, 1L));
    when(eventService.getById(1L, 1001L)).thenReturn(event(1001L, "platform", null));
    when(eventOrgMapper.selectOne(any())).thenReturn(relation(1001L, 3001L));
    FileEntity file = file(8001L, 1L);
    file.setOriginalName("roster.csv");
    when(fileService.getExistingFile(1L, 8001L)).thenReturn(file);

    assertThatThrownBy(() -> service.precheck(1001L, new CompetitionRosterPrecheckReq(8001L, 3001L)))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .hasMessage("名单文件仅支持 .xlsx 或 .xls");
    verify(fileService, never()).readFileBytesById(any());
  }

  @Test
  void enterpriseInternalEventOnlyAllowsOwnerOrganization() {
    authenticate(1000L, 1L, "enterprise-admin");
    when(userMapper.selectOne(any())).thenReturn(user(3001L));
    when(orgMapper.selectOne(any())).thenReturn(org(3001L, 1L));
    when(eventService.getById(1L, 1001L)).thenReturn(event(1001L, "enterprise_internal", 4001L));

    assertThatThrownBy(() -> service.precheck(1001L, new CompetitionRosterPrecheckReq(8001L, 3001L)))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .hasMessage("无权访问该赛事");
  }

  private void authenticate(Long userId, Long tenantId, String username) {
    var principal = new com.guoyu.platform.security.PlatformUserPrincipal(
        userId, tenantId, username, username, "business",
        List.of("ENTERPRISE_ADMIN"), List.of("competition:roster:import"), "test@example.com");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
  }

  private UserEntity user(Long orgId) {
    UserEntity user = new UserEntity();
    user.setOrgId(orgId);
    return user;
  }

  private OrgEntity org(Long id, Long tenantId) {
    OrgEntity org = new OrgEntity();
    org.setId(id);
    org.setTenantId(tenantId);
    org.setDeleted(0);
    return org;
  }

  private CompetitionEventEntity event(Long id, String scope, Long ownerOrgId) {
    CompetitionEventEntity event = new CompetitionEventEntity();
    event.setId(id);
    event.setTenantId(1L);
    event.setScope(scope);
    event.setOwnerOrgId(ownerOrgId);
    return event;
  }

  private CompetitionEventOrgEntity relation(Long eventId, Long orgId) {
    CompetitionEventOrgEntity relation = new CompetitionEventOrgEntity();
    relation.setEventId(eventId);
    relation.setOrgId(orgId);
    relation.setStatus("active");
    return relation;
  }

  private FileEntity file(Long id, Long tenantId) {
    FileEntity file = new FileEntity();
    file.setId(id);
    file.setTenantId(tenantId);
    file.setStatus("uploaded");
    file.setOriginalName("roster.xlsx");
    return file;
  }

  private CompetitionRosterImportEntity batch(Long id, Long eventId, Long orgId) {
    CompetitionRosterImportEntity batch = new CompetitionRosterImportEntity();
    batch.setId(id);
    batch.setTenantId(1L);
    batch.setEventId(eventId);
    batch.setOrgId(orgId);
    batch.setStatus("pending_confirm");
    return batch;
  }
}
