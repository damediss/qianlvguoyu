package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.dto.CompetitionEventUpdateReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventOrgEntity;
import com.guoyu.platform.competition.mapper.CompetitionEventMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventOrgMapper;
import com.guoyu.platform.competition.vo.CompetitionEventPublishResultVO;
import com.guoyu.platform.competition.vo.CompetitionEventPublishReadinessVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CompetitionEventServiceTest {

  private final CompetitionEventMapper eventMapper = Mockito.mock(CompetitionEventMapper.class);
  private final CompetitionEventOrgMapper eventOrgMapper = Mockito.mock(CompetitionEventOrgMapper.class);
  private final OrgMapper orgMapper = Mockito.mock(OrgMapper.class);
  private final UserMapper userMapper = Mockito.mock(UserMapper.class);
  private final CompetitionEventService service = new CompetitionEventService(
      eventMapper, eventOrgMapper, orgMapper, userMapper);

  @Test
  void publishesPlatformEventAfterAllPrerequisitesPass() {
    CompetitionEventEntity event = event("platform", null);
    when(eventMapper.selectForUpdate(1L, 1001L)).thenReturn(event);
    stubValidConfiguration();
    when(eventMapper.updateById(event)).thenReturn(1);

    CompetitionEventPublishResultVO result = service.publish(
        1L, 1000L, 1001L, principal(1000L, "admin", "PLATFORM_ADMIN"));

    assertThat(result.getEventId()).isEqualTo(1001L);
    assertThat(result.getPreviousStatus()).isEqualTo("draft");
    assertThat(result.getStatus()).isEqualTo("published");
    assertThat(event.getPlayerEntryCode()).isNotBlank();
    assertThat(event.getStatus()).isEqualTo("published");
    assertThat(event.getUpdatedBy()).isEqualTo(1000L);
    assertThat(event.getUpdatedAt()).isNotNull();
    verify(eventMapper).updateById(event);
  }

  @Test
  void publishesEnterpriseInternalEventOnlyForOwnerScope() {
    CompetitionEventEntity event = event("enterprise_internal", 3001L);
    when(eventMapper.selectForUpdate(1L, 1001L)).thenReturn(event);
    stubValidConfiguration();
    UserEntity user = user(1000L, 3001L);
    when(userMapper.selectOne(any())).thenReturn(user);
    when(eventMapper.updateById(event)).thenReturn(1);

    CompetitionEventPublishResultVO result = service.publish(
        1L, 1000L, 1001L, principal(1000L, "enterprise-admin", "ENTERPRISE_ADMIN"));

    assertThat(result.getStatus()).isEqualTo("published");
    verify(eventMapper).updateById(event);
  }

  @Test
  void publishKeepsStageArrangementDraftForLaterPlayerAndStageSetup() {
    CompetitionEventPublishReadinessService readinessService =
        Mockito.mock(CompetitionEventPublishReadinessService.class);
    CompetitionEventService strictService = new CompetitionEventService(
        eventMapper, eventOrgMapper, orgMapper, userMapper, readinessService);
    CompetitionEventEntity event = event("platform", null);
    event.setStagePlanStatus("draft");
    when(eventMapper.selectForUpdate(1L, 1001L)).thenReturn(event);
    when(readinessService.evaluate(any(), any(), any(), any())).thenReturn(
        new CompetitionEventPublishReadinessVO(
            1001L, "COMP-1001", "Competition", "draft", "platform",
            event.getStartAt(), event.getEndAt(), true, 0, 0, 0, 0, List.of(), List.of()));
    when(eventMapper.updateById(event)).thenReturn(1);

    strictService.publish(1L, 1000L, 1001L,
        principal(1000L, "admin", "PLATFORM_ADMIN"));

    assertThat(event.getStagePlanStatus()).isEqualTo("draft");
    assertThat(event.getStagePlanConfirmedBy()).isNull();
    assertThat(event.getStagePlanConfirmedAt()).isNull();
    assertThat(event.getStatus()).isEqualTo("published");
    assertThat(event.getPlayerEntryCode()).isNotBlank();
  }

  @Test
  void rejectsInvalidEventTimeRangeWithoutChangingEvent() {
    CompetitionEventEntity event = event("platform", null);
    event.setEndAt(event.getStartAt().minusMinutes(1));
    when(eventMapper.selectForUpdate(1L, 1001L)).thenReturn(event);

    assertThatThrownBy(() -> service.publish(
        1L, 1000L, 1001L, principal(1000L, "admin", "PLATFORM_ADMIN")))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .extracting("code")
        .isEqualTo(ErrorCode.UNPROCESSABLE_ENTITY);
    assertThat(event.getStatus()).isEqualTo("draft");
    verify(eventMapper, never()).updateById(any(CompetitionEventEntity.class));
  }

  @Test
  void publishesPlatformEventWithoutParticipantOrganizations() {
    CompetitionEventEntity event = event("platform", null);
    when(eventMapper.selectForUpdate(1L, 1001L)).thenReturn(event);
    when(eventOrgMapper.selectList(any())).thenReturn(List.of());
    when(eventMapper.updateById(event)).thenReturn(1);

    CompetitionEventPublishResultVO result = service.publish(
        1L, 1000L, 1001L, principal(1000L, "admin", "PLATFORM_ADMIN"));

    assertThat(result.getStatus()).isEqualTo("published");
    assertThat(event.getPlayerEntryCode()).isNotBlank();
    verify(eventMapper).updateById(event);
  }

  @Test
  void publishesBasicInformationWithoutEventDates() {
    CompetitionEventEntity event = event("platform", null);
    event.setStartAt(null);
    event.setEndAt(null);
    when(eventMapper.selectForUpdate(1L, 1001L)).thenReturn(event);
    when(eventMapper.updateById(event)).thenReturn(1);

    CompetitionEventPublishResultVO result = service.publish(
        1L, 1000L, 1001L, principal(1000L, "admin", "PLATFORM_ADMIN"));

    assertThat(result.getStatus()).isEqualTo("published");
    verify(eventMapper).updateById(event);
  }

  @Test
  void rejectsPlatformEventOutsideNonAdminOrganizationScope() {
    CompetitionEventEntity event = event("platform", null);
    when(eventMapper.selectForUpdate(1L, 1001L)).thenReturn(event);
    stubValidConfiguration();
    when(userMapper.selectOne(any())).thenReturn(user(1000L, 4001L));

    assertThatThrownBy(() -> service.publish(
        1L, 1000L, 1001L, principal(1000L, "enterprise-admin", "ENTERPRISE_ADMIN")))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .extracting("code")
        .isEqualTo(ErrorCode.FORBIDDEN);
    verify(eventMapper, never()).updateById(any(CompetitionEventEntity.class));
  }

  @Test
  void rejectsEnterpriseEventWhenOwnerOrganizationIsInvalid() {
    CompetitionEventEntity event = event("enterprise_internal", 3001L);
    when(eventMapper.selectForUpdate(1L, 1001L)).thenReturn(event);
    when(userMapper.selectOne(any())).thenReturn(user(1000L, 3001L));
    when(orgMapper.selectOne(any())).thenReturn(null);

    assertThatThrownBy(() -> service.publish(
        1L, 1000L, 1001L, principal(1000L, "enterprise-admin", "ENTERPRISE_ADMIN")))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .extracting("code")
        .isEqualTo(ErrorCode.FORBIDDEN);
    verify(eventMapper, never()).updateById(any(CompetitionEventEntity.class));
  }

  @Test
  void rejectsInvalidEventStatesAndMissingEvents() {
    for (String status : List.of("published", "closed", "archived")) {
      CompetitionEventEntity event = event("platform", null);
      event.setStatus(status);
      when(eventMapper.selectForUpdate(1L, 1001L)).thenReturn(event);
      assertThatThrownBy(() -> service.publish(
          1L, 1000L, 1001L, principal(1000L, "admin", "PLATFORM_ADMIN")))
          .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
          .extracting("code")
          .isEqualTo(ErrorCode.CONFLICT);
    }

    when(eventMapper.selectForUpdate(1L, 9001L)).thenReturn(null);
    assertThatThrownBy(() -> service.publish(
        1L, 1000L, 9001L, principal(1000L, "admin", "PLATFORM_ADMIN")))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .extracting("code")
        .isEqualTo(ErrorCode.NOT_FOUND);
  }

  @Test
  void secondPublishAttemptConflictsAfterFirstTransactionChangesStatus() {
    CompetitionEventEntity event = event("platform", null);
    when(eventMapper.selectForUpdate(1L, 1001L)).thenReturn(event);
    stubValidConfiguration();
    when(eventMapper.updateById(event)).thenAnswer(invocation -> {
      event.setStatus("published");
      return 1;
    });

    service.publish(1L, 1000L, 1001L, principal(1000L, "admin", "PLATFORM_ADMIN"));

    assertThatThrownBy(() -> service.publish(
        1L, 1000L, 1001L, principal(1000L, "admin", "PLATFORM_ADMIN")))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .extracting("code")
        .isEqualTo(ErrorCode.CONFLICT);
    verify(eventMapper, org.mockito.Mockito.times(1)).updateById(event);
  }

  @Test
  void allowsUpdatingPublishedEventUntilStagePlanIsConfirmed() {
    CompetitionEventEntity event = event("platform", null);
    event.setStatus("published");
    event.setStagePlanStatus("draft");
    when(eventMapper.selectForUpdate(1L, 1001L)).thenReturn(event);
    when(eventMapper.updateById(event)).thenReturn(1);

    CompetitionEventEntity updated = service.updateDraft(1L, 1001L,
        new CompetitionEventUpdateReq("Updated", 0, event.getStartAt(), event.getEndAt(), "补充说明"));

    assertThat(updated.getName()).isEqualTo("Updated");
    verify(eventMapper).updateById(event);
  }

  @Test
  void rejectsUpdatingEventAfterStagePlanIsConfirmed() {
    CompetitionEventEntity event = event("platform", null);
    event.setStatus("published");
    event.setStagePlanStatus("confirmed");
    when(eventMapper.selectForUpdate(1L, 1001L)).thenReturn(event);

    assertThatThrownBy(() -> service.updateDraft(1L, 1001L,
        new CompetitionEventUpdateReq("Updated", 0, event.getStartAt(), event.getEndAt(), null)))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .extracting("code")
        .isEqualTo(ErrorCode.CONFLICT);
    verify(eventMapper).selectForUpdate(1L, 1001L);
    verify(eventMapper, never()).updateById(any(CompetitionEventEntity.class));
  }

  private void stubValidConfiguration() {
    when(eventOrgMapper.selectList(any())).thenReturn(List.of(relation(3001L, "active")));
    when(eventOrgMapper.selectOne(any())).thenReturn(relation(3001L, "active"));
    when(orgMapper.selectOne(any())).thenReturn(org(3001L));
  }

  private CompetitionEventEntity event(String scope, Long ownerOrgId) {
    CompetitionEventEntity event = new CompetitionEventEntity();
    event.setId(1001L);
    event.setTenantId(1L);
    event.setName("Competition");
    event.setCode("COMP-1001");
    event.setScope(scope);
    event.setOwnerOrgId(ownerOrgId);
    event.setStatus("draft");
    event.setRegistrationAuditRequired(0);
    event.setStartAt(LocalDateTime.of(2026, 8, 1, 9, 0));
    event.setEndAt(LocalDateTime.of(2026, 8, 2, 18, 0));
    event.setDeleted(0);
    return event;
  }

  private CompetitionEventOrgEntity relation(Long orgId, String status) {
    CompetitionEventOrgEntity relation = new CompetitionEventOrgEntity();
    relation.setId(7001L);
    relation.setTenantId(1L);
    relation.setEventId(1001L);
    relation.setOrgId(orgId);
    relation.setStatus(status);
    relation.setDeleted(0);
    return relation;
  }

  private OrgEntity org(Long id) {
    OrgEntity org = new OrgEntity();
    org.setId(id);
    org.setTenantId(1L);
    org.setDeleted(0);
    return org;
  }

  private UserEntity user(Long id, Long orgId) {
    UserEntity user = new UserEntity();
    user.setId(id);
    user.setTenantId(1L);
    user.setOrgId(orgId);
    user.setDeleted(0);
    return user;
  }

  private PlatformUserPrincipal principal(Long userId, String username, String role) {
    return new PlatformUserPrincipal(userId, 1L, username, username, "business",
        List.of(role), List.of("competition:event:publish"), "test@example.com");
  }
}
