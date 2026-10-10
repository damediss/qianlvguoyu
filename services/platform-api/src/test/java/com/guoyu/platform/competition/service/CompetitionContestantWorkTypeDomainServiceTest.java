package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.competition.dto.CompetitionContestantWorkTypeConfirmReq;
import com.guoyu.platform.competition.entity.CompetitionContestantProfileEntity;
import com.guoyu.platform.competition.entity.CompetitionEventMemberEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionRegistrationEntity;
import com.guoyu.platform.competition.mapper.CompetitionContestantProfileMapper;
import com.guoyu.platform.competition.mapper.CompetitionContestantWorkTypeRow;
import com.guoyu.platform.competition.mapper.CompetitionEventMemberMapper;
import com.guoyu.platform.competition.mapper.CompetitionRegistrationMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CompetitionContestantWorkTypeDomainServiceTest {

  private final CompetitionContestantProfileMapper profileMapper =
      Mockito.mock(CompetitionContestantProfileMapper.class);
  private final CompetitionEventMemberMapper memberMapper =
      Mockito.mock(CompetitionEventMemberMapper.class);
  private final CompetitionRegistrationMapper registrationMapper =
      Mockito.mock(CompetitionRegistrationMapper.class);
  private final CompetitionEventWorkTypeDomainService eventWorkTypeService =
      Mockito.mock(CompetitionEventWorkTypeDomainService.class);
  private final CompetitionContestantWorkTypeDomainService service =
      new CompetitionContestantWorkTypeDomainService(
          profileMapper, memberMapper, registrationMapper, eventWorkTypeService);

  @Test
  void uniqueExactMatchIsCandidateWhileZeroAndMultipleMatchesRemainUnresolved() {
    CompetitionEventWorkTypeEntity welding = workType(10L, 100L, "焊工赛项");
    CompetitionEventWorkTypeEntity electricianOne = workType(11L, 101L, "电工");
    CompetitionEventWorkTypeEntity electricianTwo = workType(12L, 102L, "电工");
    when(eventWorkTypeService.list(1L, 200L))
        .thenReturn(List.of(welding, electricianOne, electricianTwo));
    when(eventWorkTypeService.findActiveSourceName(1L, welding.getWorkTypeId()))
        .thenReturn("焊工");
    when(profileMapper.selectWorkTypeRowsByEvent(1L, 200L, 3001L))
        .thenReturn(List.of(
            row(1L, 21L, "焊工"),
            row(2L, 22L, "钳工"),
            row(3L, 23L, "电工")));

    var result = service.reconcile(1L, 200L, 3001L);

    assertThat(result).hasSize(3);
    assertThat(result.get(0).getMatchStatus()).isEqualTo("matched");
    assertThat(result.get(0).isConfirmed()).isFalse();
    assertThat(result.get(0).getCandidates()).hasSize(1);
    assertThat(result.get(1).getMatchStatus()).isEqualTo("unmatched");
    assertThat(result.get(1).getCandidates()).isEmpty();
    assertThat(result.get(2).getMatchStatus()).isEqualTo("conflict");
    assertThat(result.get(2).getCandidates()).hasSize(2);
  }

  @Test
  void manualConfirmationPersistsStableEventWorkType() {
    CompetitionEventMemberEntity member = member(21L, 200L);
    CompetitionContestantProfileEntity profile = profile(31L, 21L);
    CompetitionEventWorkTypeEntity eventWorkType = workType(10L, 200L, "焊工");
    when(memberMapper.selectOne(any())).thenReturn(member);
    when(profileMapper.selectList(any())).thenReturn(List.of(profile));
    when(eventWorkTypeService.getById(1L, 10L)).thenReturn(eventWorkType);
    when(profileMapper.updateById(any(CompetitionContestantProfileEntity.class))).thenReturn(1);

    var result = service.confirm(
        1L, 200L, 21L, 99L,
        new CompetitionContestantWorkTypeConfirmReq(10L, "现场复核"), null);

    assertThat(result.isConfirmed()).isTrue();
    assertThat(profile.getEventWorkTypeId()).isEqualTo(10L);
    assertThat(profile.getWorkTypeMatchStatus()).isEqualTo("matched");
    assertThat(profile.getWorkTypeMatchNote()).isEqualTo("现场复核");
    assertThat(profile.getUpdatedBy()).isEqualTo(99L);
  }

  @Test
  void rejectsWorkTypeFromAnotherEvent() {
    when(memberMapper.selectOne(any())).thenReturn(member(21L, 200L));
    when(profileMapper.selectList(any())).thenReturn(List.of(profile(31L, 21L)));
    when(eventWorkTypeService.getById(1L, 10L))
        .thenReturn(workType(10L, 999L, "焊工"));

    assertThatThrownBy(() -> service.confirm(
        1L, 200L, 21L, 99L,
        new CompetitionContestantWorkTypeConfirmReq(10L, null), null))
        .isInstanceOf(BusinessException.class)
        .hasMessage("赛事工种无效或不属于当前赛事");
    verify(profileMapper, never()).updateById(any(CompetitionContestantProfileEntity.class));
  }

  @Test
  void enterpriseReconciliationPassesOrganizationScopeToMapper() {
    when(profileMapper.selectWorkTypeRowsByEvent(1L, 200L, 3001L))
        .thenReturn(List.of());
    when(eventWorkTypeService.list(1L, 200L)).thenReturn(List.of());

    assertThat(service.reconcile(1L, 200L, 3001L)).isEmpty();

    verify(profileMapper).selectWorkTypeRowsByEvent(1L, 200L, 3001L);
  }

  @Test
  void rejectsManualConfirmationForAnotherOrganizationContestant() {
    CompetitionContestantProfileEntity profile = profile(31L, 21L);
    profile.setRegistrationId(41L);
    when(memberMapper.selectOne(any())).thenReturn(member(21L, 200L));
    when(profileMapper.selectList(any())).thenReturn(List.of(profile));
    when(registrationMapper.selectOne(any())).thenReturn(null);

    assertThatThrownBy(() -> service.confirm(
        1L, 200L, 21L, 99L,
        new CompetitionContestantWorkTypeConfirmReq(10L, null), 3001L))
        .isInstanceOf(BusinessException.class)
        .hasMessage("无权确认其他企业选手工种");

    verify(eventWorkTypeService, never()).getById(any(), any());
    verify(profileMapper, never()).updateById(any(CompetitionContestantProfileEntity.class));
  }

  @Test
  void confirmTargetsUnmatchedProfileWhenMemberAlreadyHasAnotherItem() {
    CompetitionContestantProfileEntity matched = profile(31L, 21L);
    matched.setEventWorkTypeId(10L);
    matched.setWorkTypeMatchStatus("matched");
    CompetitionContestantProfileEntity unmatched = profile(32L, 21L);
    unmatched.setRegistrationId(42L);
    when(memberMapper.selectOne(any())).thenReturn(member(21L, 200L));
    when(profileMapper.selectList(any())).thenReturn(List.of(matched, unmatched));
    when(eventWorkTypeService.getById(1L, 11L)).thenReturn(workType(11L, 200L, "电工"));
    when(profileMapper.updateById(any(CompetitionContestantProfileEntity.class))).thenReturn(1);

    var result = service.confirm(
        1L, 200L, 21L, 99L,
        new CompetitionContestantWorkTypeConfirmReq(11L, "第二赛项"), null);

    assertThat(result.getProfileId()).isEqualTo(32L);
    assertThat(unmatched.getEventWorkTypeId()).isEqualTo(11L);
    assertThat(matched.getEventWorkTypeId()).isEqualTo(10L);
  }

  private CompetitionContestantWorkTypeRow row(Long id, Long memberId, String rosterText) {
    CompetitionContestantWorkTypeRow row = new CompetitionContestantWorkTypeRow();
    row.setProfileId(id);
    row.setContestantMemberId(memberId);
    row.setRosterWorkType(rosterText);
    row.setWorkTypeMatchStatus("unmatched");
    return row;
  }

  private CompetitionEventWorkTypeEntity workType(Long id, Long eventId, String name) {
    CompetitionEventWorkTypeEntity entity = new CompetitionEventWorkTypeEntity();
    entity.setId(id);
    entity.setTenantId(1L);
    entity.setEventId(eventId);
    entity.setWorkTypeId(id + 1000);
    entity.setDisplayName(name);
    entity.setStatus("active");
    entity.setDeleted(0);
    return entity;
  }

  private CompetitionEventMemberEntity member(Long id, Long eventId) {
    CompetitionEventMemberEntity entity = new CompetitionEventMemberEntity();
    entity.setId(id);
    entity.setTenantId(1L);
    entity.setEventId(eventId);
    entity.setRoleType("contestant");
    entity.setStatus("active");
    entity.setDeleted(0);
    return entity;
  }

  private CompetitionContestantProfileEntity profile(Long id, Long memberId) {
    CompetitionContestantProfileEntity entity = new CompetitionContestantProfileEntity();
    entity.setId(id);
    entity.setTenantId(1L);
    entity.setMemberId(memberId);
    entity.setWorkTypeMatchStatus("unmatched");
    entity.setDeleted(0);
    return entity;
  }
}
