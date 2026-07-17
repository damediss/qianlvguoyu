package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.dto.CompetitionRegistrationAuditRequest;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventOrgEntity;
import com.guoyu.platform.competition.entity.CompetitionRegistrationEntity;
import com.guoyu.platform.competition.entity.CompetitionRosterImportEntity;
import com.guoyu.platform.competition.entity.CompetitionRosterItemEntity;
import com.guoyu.platform.competition.mapper.CompetitionEventMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventOrgMapper;
import com.guoyu.platform.competition.mapper.CompetitionRegistrationMapper;
import com.guoyu.platform.competition.mapper.CompetitionRosterImportMapper;
import com.guoyu.platform.competition.mapper.CompetitionRosterItemMapper;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CompetitionRegistrationServiceTest {

  private final CompetitionEventMapper eventMapper = Mockito.mock(CompetitionEventMapper.class);
  private final CompetitionEventOrgMapper eventOrgMapper = Mockito.mock(CompetitionEventOrgMapper.class);
  private final CompetitionRosterImportMapper importMapper = Mockito.mock(CompetitionRosterImportMapper.class);
  private final CompetitionRosterItemMapper itemMapper = Mockito.mock(CompetitionRosterItemMapper.class);
  private final CompetitionRegistrationMapper registrationMapper = Mockito.mock(CompetitionRegistrationMapper.class);
  private final OrgMapper orgMapper = Mockito.mock(OrgMapper.class);
  private final UserMapper userMapper = Mockito.mock(UserMapper.class);
  private final CompetitionRegistrationService service = new CompetitionRegistrationService(
      eventMapper, eventOrgMapper, importMapper, itemMapper, registrationMapper, orgMapper, userMapper);

  @Test
  void confirmsBatchAndGeneratesPendingAuditRegistrations() {
    when(importMapper.selectForUpdate(1L, 7001L)).thenReturn(batch("pending_confirm"));
    when(eventMapper.selectForUpdate(1L, 1001L)).thenReturn(event(1));
    when(eventOrgMapper.selectForUpdate(1L, 1001L, 3001L)).thenReturn(participation(10, 1, null));
    when(itemMapper.selectList(any())).thenReturn(List.of(item(9001L, "E001")));
    when(registrationMapper.selectOne(any())).thenReturn(null);
    when(registrationMapper.existsEffectiveByEmployeeNo(1L, 1001L, 3001L, "E001"))
        .thenReturn(false);
    when(registrationMapper.countEffectiveByEventOrg(1L, 1001L, 3001L)).thenReturn(0L);

    CompetitionRosterImportEntity result = service.confirmAndGenerate(1L, 7001L, 1000L);

    assertThat(result.getStatus()).isEqualTo("confirmed");
    verify(registrationMapper).insert(any(CompetitionRegistrationEntity.class));
    verify(importMapper).updateById(result);
  }

  @Test
  void confirmsBatchWithoutAuditAsEligibleUnactivated() {
    when(importMapper.selectForUpdate(1L, 7001L)).thenReturn(batch("pending_confirm"));
    when(eventMapper.selectForUpdate(1L, 1001L)).thenReturn(event(0));
    when(eventOrgMapper.selectForUpdate(1L, 1001L, 3001L)).thenReturn(participation(10, 1, null));
    when(itemMapper.selectList(any())).thenReturn(List.of(item(9001L, "E001")));
    when(registrationMapper.selectOne(any())).thenReturn(null);
    when(registrationMapper.existsEffectiveByEmployeeNo(1L, 1001L, 3001L, "E001"))
        .thenReturn(false);
    when(registrationMapper.countEffectiveByEventOrg(1L, 1001L, 3001L)).thenReturn(0L);

    service.confirmAndGenerate(1L, 7001L, 1000L);

    var captor = org.mockito.ArgumentCaptor.forClass(CompetitionRegistrationEntity.class);
    verify(registrationMapper).insert(captor.capture());
    assertThat(captor.getValue().getStatus()).isEqualTo("eligible_unactivated");
    assertThat(captor.getValue().getSource()).isEqualTo("enterprise_import");
  }

  @Test
  void rejectsDuplicateRosterItemWithoutChangingBatch() {
    when(importMapper.selectForUpdate(1L, 7001L)).thenReturn(batch("pending_confirm"));
    when(eventMapper.selectForUpdate(1L, 1001L)).thenReturn(event(0));
    when(eventOrgMapper.selectForUpdate(1L, 1001L, 3001L)).thenReturn(participation(10, 1, null));
    when(itemMapper.selectList(any())).thenReturn(List.of(item(9001L, "E001")));
    CompetitionRegistrationEntity existing = new CompetitionRegistrationEntity();
    when(registrationMapper.selectOne(any())).thenReturn(existing);

    assertThatThrownBy(() -> service.confirmAndGenerate(1L, 7001L, 1000L))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .hasMessage("rosterItem 已存在参赛资格");
    verify(registrationMapper, never()).insert(any(CompetitionRegistrationEntity.class));
    verify(importMapper, never()).updateById(any(CompetitionRosterImportEntity.class));
  }

  @Test
  void rejectsWhenQuotaWouldBeExceeded() {
    when(importMapper.selectForUpdate(1L, 7001L)).thenReturn(batch("pending_confirm"));
    when(eventMapper.selectForUpdate(1L, 1001L)).thenReturn(event(0));
    when(eventOrgMapper.selectForUpdate(1L, 1001L, 3001L)).thenReturn(participation(10, 1, 1));
    when(itemMapper.selectList(any())).thenReturn(List.of(item(9001L, "E001"), item(9002L, "E002")));
    when(registrationMapper.selectOne(any())).thenReturn(null);
    when(registrationMapper.existsEffectiveByEmployeeNo(any(), any(), any(), any())).thenReturn(false);
    when(registrationMapper.countEffectiveByEventOrg(1L, 1001L, 3001L)).thenReturn(0L);

    assertThatThrownBy(() -> service.confirmAndGenerate(1L, 7001L, 1000L))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .hasMessage("赛事参与企业名额不足，当前名额=1");
    verify(registrationMapper, never()).insert(any(CompetitionRegistrationEntity.class));
  }

  @Test
  void rejectsAlreadyConfirmedBatch() {
    when(importMapper.selectForUpdate(1L, 7001L)).thenReturn(batch("confirmed"));

    assertThatThrownBy(() -> service.confirmAndGenerate(1L, 7001L, 1000L))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .hasMessage("名单导入批次已确认，不能重复确认");
  }

  @Test
  void rejectsBatchWithoutValidItemsAndDoesNotUpdateBatch() {
    when(importMapper.selectForUpdate(1L, 7001L)).thenReturn(batch("pending_confirm"));
    when(eventMapper.selectForUpdate(1L, 1001L)).thenReturn(event(0));
    when(eventOrgMapper.selectForUpdate(1L, 1001L, 3001L)).thenReturn(participation(10, 1, null));
    when(itemMapper.selectList(any())).thenReturn(List.of());

    assertThatThrownBy(() -> service.confirmAndGenerate(1L, 7001L, 1000L))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class);
    verify(registrationMapper, never()).insert(any(CompetitionRegistrationEntity.class));
    verify(importMapper, never()).updateById(any(CompetitionRosterImportEntity.class));
  }

  @Test
  void rejectsWhenImportPermissionIsRevoked() {
    when(importMapper.selectForUpdate(1L, 7001L)).thenReturn(batch("pending_confirm"));
    when(eventMapper.selectForUpdate(1L, 1001L)).thenReturn(event(0));
    when(eventOrgMapper.selectForUpdate(1L, 1001L, 3001L)).thenReturn(participation(10, 0, null));

    assertThatThrownBy(() -> service.confirmAndGenerate(1L, 7001L, 1000L))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class);
    verify(itemMapper, never()).selectList(any());
    verify(registrationMapper, never()).insert(any(CompetitionRegistrationEntity.class));
  }

  @Test
  void rejectsEnterpriseInternalBatchForNonOwnerOrganization() {
    when(importMapper.selectForUpdate(1L, 7001L)).thenReturn(batch("pending_confirm"));
    CompetitionEventEntity internalEvent = event(0);
    internalEvent.setScope("enterprise_internal");
    internalEvent.setOwnerOrgId(9999L);
    when(eventMapper.selectForUpdate(1L, 1001L)).thenReturn(internalEvent);

    assertThatThrownBy(() -> service.confirmAndGenerate(1L, 7001L, 1000L))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class);
    verify(eventOrgMapper, never()).selectForUpdate(any(), any(), any());
    verify(registrationMapper, never()).insert(any(CompetitionRegistrationEntity.class));
  }

  @Test
  void rejectsEmployeeNoAlreadyRegisteredInSameEventAndOrganization() {
    when(importMapper.selectForUpdate(1L, 7001L)).thenReturn(batch("pending_confirm"));
    when(eventMapper.selectForUpdate(1L, 1001L)).thenReturn(event(0));
    when(eventOrgMapper.selectForUpdate(1L, 1001L, 3001L)).thenReturn(participation(10, 1, null));
    when(itemMapper.selectList(any())).thenReturn(List.of(item(9001L, "E001")));
    when(registrationMapper.selectOne(any())).thenReturn(null);
    when(registrationMapper.existsEffectiveByEmployeeNo(1L, 1001L, 3001L, "E001"))
        .thenReturn(true);

    assertThatThrownBy(() -> service.confirmAndGenerate(1L, 7001L, 1000L))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class);
    verify(registrationMapper, never()).insert(any(CompetitionRegistrationEntity.class));
    verify(importMapper, never()).updateById(any(CompetitionRosterImportEntity.class));
  }

  @Test
  void approvesPendingRegistrationAndTrimsOptionalReason() {
    CompetitionRegistrationEntity registration = registration("pending_audit");
    stubAuditDependencies(registration);
    CompetitionRegistrationAuditRequest request = request("approve", "  符合赛事要求  ");

    var result = service.audit(1L, 9101L, request, principal(1000L, "PLATFORM_ADMIN"));

    assertThat(result.getBeforeStatus()).isEqualTo("pending_audit");
    assertThat(result.getStatus()).isEqualTo("eligible_unactivated");
    assertThat(result.getAction()).isEqualTo("approve");
    assertThat(registration.getAuditReason()).isEqualTo("符合赛事要求");
    assertThat(registration.getAuditedBy()).isEqualTo(1000L);
    assertThat(registration.getAuditedAt()).isNotNull();
    assertThat(registration.getUpdatedBy()).isEqualTo(1000L);
    assertThat(registration.getUpdatedAt()).isNotNull();
    verify(registrationMapper).updateById(registration);
  }

  @Test
  void requiresReasonForSupplementAndStoresIt() {
    CompetitionRegistrationEntity registration = registration("pending_audit");
    stubAuditDependencies(registration);

    assertThatThrownBy(() -> service.audit(1L, 9101L, request("supplement", "  "),
        principal(1000L, "PLATFORM_ADMIN")))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .extracting("code")
        .isEqualTo(ErrorCode.UNPROCESSABLE_ENTITY);
    verify(registrationMapper, never()).updateById(any(CompetitionRegistrationEntity.class));

    CompetitionRegistrationAuditRequest validRequest = request("supplement", "补充材料");
    var result = service.audit(1L, 9101L, validRequest, principal(1000L, "PLATFORM_ADMIN"));
    assertThat(result.getStatus()).isEqualTo("supplement_required");
    assertThat(registration.getAuditReason()).isEqualTo("补充材料");
  }

  @Test
  void rejectsPendingRegistrationWithRequiredReason() {
    CompetitionRegistrationEntity registration = registration("pending_audit");
    stubAuditDependencies(registration);

    var result = service.audit(1L, 9101L, request("reject", "不符合报名条件"),
        principal(1000L, "PLATFORM_ADMIN"));

    assertThat(result.getStatus()).isEqualTo("rejected");
    assertThat(registration.getAuditReason()).isEqualTo("不符合报名条件");
  }

  @Test
  void rejectsNonPendingRegistrationWithConflict() {
    CompetitionRegistrationEntity registration = registration("eligible_unactivated");
    stubAuditDependencies(registration);

    assertThatThrownBy(() -> service.audit(1L, 9101L, request("reject", "再次驳回"),
        principal(1000L, "PLATFORM_ADMIN")))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .extracting("code")
        .isEqualTo(ErrorCode.CONFLICT);
    verify(registrationMapper, never()).updateById(any(CompetitionRegistrationEntity.class));
  }

  @Test
  void rejectsEveryNonPendingRegistrationState() {
    for (String status : List.of(
        "eligible_unactivated", "supplement_required", "rejected", "withdrawn", "expired",
        "imported_pending_confirm")) {
      CompetitionRegistrationEntity registration = registration(status);
      stubAuditDependencies(registration);

      assertThatThrownBy(() -> service.audit(1L, 9101L, request("approve", null),
          principal(1000L, "PLATFORM_ADMIN")))
          .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
          .extracting("code")
          .isEqualTo(ErrorCode.CONFLICT);
    }
    verify(registrationMapper, never()).updateById(any(CompetitionRegistrationEntity.class));
  }

  @Test
  void rejectsClosedEventAndCrossOrganizationUser() {
    CompetitionRegistrationEntity registration = registration("pending_audit");
    stubAuditDependencies(registration);
    CompetitionEventEntity event = event(1);
    event.setStatus("closed");
    when(eventMapper.selectForUpdate(1L, 1001L)).thenReturn(event);
    assertThatThrownBy(() -> service.audit(1L, 9101L, request("approve", null),
        principal(1000L, "PLATFORM_ADMIN")))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .extracting("code")
        .isEqualTo(ErrorCode.UNPROCESSABLE_ENTITY);

    event.setStatus("draft");
    PlatformUserPrincipal enterpriseUser = principal(2000L, "ENTERPRISE_ADMIN");
    UserEntity user = new UserEntity();
    user.setTenantId(1L);
    user.setId(2000L);
    user.setOrgId(4001L);
    when(userMapper.selectOne(any())).thenReturn(user);
    assertThatThrownBy(() -> service.audit(1L, 9101L, request("approve", null), enterpriseUser))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .extracting("code")
        .isEqualTo(ErrorCode.FORBIDDEN);
  }

  @Test
  void validatesActionReasonLengthAndMissingAssociation() {
    assertThatThrownBy(() -> service.audit(1L, 9101L, request("invalid", null),
        principal(1000L, "PLATFORM_ADMIN")))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .extracting("code")
        .isEqualTo(ErrorCode.UNPROCESSABLE_ENTITY);

    assertThatThrownBy(() -> service.audit(1L, 9101L, request("reject", "x".repeat(1001)),
        principal(1000L, "PLATFORM_ADMIN")))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .extracting("code")
        .isEqualTo(ErrorCode.UNPROCESSABLE_ENTITY);

    CompetitionRegistrationEntity registration = registration("pending_audit");
    when(registrationMapper.selectForUpdate(1L, 9101L)).thenReturn(registration);
    when(eventMapper.selectForUpdate(1L, 1001L)).thenReturn(event(1));
    when(itemMapper.selectOne(any())).thenReturn(null);
    assertThatThrownBy(() -> service.audit(1L, 9101L, request("approve", null),
        principal(1000L, "PLATFORM_ADMIN")))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .extracting("code")
        .isEqualTo(ErrorCode.NOT_FOUND);
  }

  @Test
  void secondAuditAfterFirstSuccessfulAuditConflicts() {
    CompetitionRegistrationEntity registration = registration("pending_audit");
    stubAuditDependencies(registration);

    service.audit(1L, 9101L, request("approve", null), principal(1000L, "PLATFORM_ADMIN"));
    assertThatThrownBy(() -> service.audit(1L, 9101L, request("reject", "不能重复审核"),
        principal(1000L, "PLATFORM_ADMIN")))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .extracting("code")
        .isEqualTo(ErrorCode.CONFLICT);
    verify(registrationMapper).updateById(registration);
  }

  private CompetitionRosterImportEntity batch(String status) {
    CompetitionRosterImportEntity batch = new CompetitionRosterImportEntity();
    batch.setId(7001L);
    batch.setTenantId(1L);
    batch.setEventId(1001L);
    batch.setOrgId(3001L);
    batch.setStatus(status);
    return batch;
  }

  private CompetitionEventEntity event(int registrationAuditRequired) {
    CompetitionEventEntity event = new CompetitionEventEntity();
    event.setId(1001L);
    event.setTenantId(1L);
    event.setScope("platform");
    event.setStatus("draft");
    event.setRegistrationAuditRequired(registrationAuditRequired);
    return event;
  }

  private CompetitionRegistrationEntity registration(String status) {
    CompetitionRegistrationEntity registration = new CompetitionRegistrationEntity();
    registration.setId(9101L);
    registration.setTenantId(1L);
    registration.setEventId(1001L);
    registration.setOrgId(3001L);
    registration.setRosterItemId(9001L);
    registration.setStatus(status);
    return registration;
  }

  private CompetitionRegistrationAuditRequest request(String action, String reason) {
    CompetitionRegistrationAuditRequest request = new CompetitionRegistrationAuditRequest();
    request.setAction(action);
    request.setReason(reason);
    return request;
  }

  private PlatformUserPrincipal principal(Long userId, String role) {
    return new PlatformUserPrincipal(userId, 1L, "user" + userId, "User", "admin",
        List.of(role), List.of("competition:registration:audit"), null);
  }

  private void stubAuditDependencies(CompetitionRegistrationEntity registration) {
    when(registrationMapper.selectForUpdate(1L, registration.getId())).thenReturn(registration);
    when(eventMapper.selectForUpdate(1L, registration.getEventId())).thenReturn(event(1));
    when(itemMapper.selectOne(any())).thenReturn(item(9001L, "E001"));
    when(importMapper.selectOne(any())).thenReturn(batch("confirmed"));
    OrgEntity org = new OrgEntity();
    org.setId(3001L);
    org.setTenantId(1L);
    when(orgMapper.selectOne(any())).thenReturn(org);
    when(eventOrgMapper.selectForUpdate(1L, 1001L, 3001L)).thenReturn(participation(10, 1, null));
  }

  private CompetitionEventOrgEntity participation(long quota, int importPermission, Integer ignored) {
    CompetitionEventOrgEntity relation = new CompetitionEventOrgEntity();
    relation.setTenantId(1L);
    relation.setEventId(1001L);
    relation.setOrgId(3001L);
    relation.setStatus("active");
    relation.setQuota(ignored == null ? null : ignored);
    relation.setImportPermission(importPermission);
    return relation;
  }

  private CompetitionRosterItemEntity item(Long id, String employeeNo) {
    CompetitionRosterItemEntity item = new CompetitionRosterItemEntity();
    item.setId(id);
    item.setTenantId(1L);
    item.setBatchId(7001L);
    item.setEmployeeNo(employeeNo);
    item.setName("Name");
    item.setValidationStatus("valid");
    return item;
  }
}
