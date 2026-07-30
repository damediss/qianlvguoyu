package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.guoyu.platform.competition.entity.CompetitionContestantProfileEntity;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventMemberEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionRegistrationEntity;
import com.guoyu.platform.competition.entity.CompetitionScoreRuleEntity;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.mapper.CompetitionContestantProfileMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventMemberMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
import com.guoyu.platform.competition.mapper.CompetitionRegistrationMapper;
import com.guoyu.platform.competition.mapper.CompetitionScoreRuleMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageMapper;
import com.guoyu.platform.competition.vo.CompetitionScoringReadinessCheckVO;
import com.guoyu.platform.competition.vo.CompetitionScoringReadinessVO;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CompetitionScoringReadinessDomainServiceTest {

  private CompetitionEventMapper eventMapper;
  private CompetitionStageMapper stageMapper;
  private CompetitionEventWorkTypeMapper workTypeMapper;
  private CompetitionScoreRuleMapper ruleMapper;
  private CompetitionContestantProfileMapper profileMapper;
  private CompetitionEventMemberMapper memberMapper;
  private CompetitionRegistrationMapper registrationMapper;
  private CompetitionScoringReadinessDomainService service;

  @BeforeEach
  void setUp() {
    eventMapper = Mockito.mock(CompetitionEventMapper.class);
    stageMapper = Mockito.mock(CompetitionStageMapper.class);
    workTypeMapper = Mockito.mock(CompetitionEventWorkTypeMapper.class);
    ruleMapper = Mockito.mock(CompetitionScoreRuleMapper.class);
    profileMapper = Mockito.mock(CompetitionContestantProfileMapper.class);
    memberMapper = Mockito.mock(CompetitionEventMemberMapper.class);
    registrationMapper = Mockito.mock(CompetitionRegistrationMapper.class);
    CompetitionScoreRuleDomainService ruleDomainService =
        new CompetitionScoreRuleDomainService(
            ruleMapper, eventMapper, stageMapper, workTypeMapper);
    service = new CompetitionScoringReadinessDomainService(
        eventMapper, stageMapper, workTypeMapper, ruleMapper, profileMapper,
        memberMapper, registrationMapper, ruleDomainService);

    when(eventMapper.selectOne(any())).thenReturn(event("published"));
    when(stageMapper.selectOne(any())).thenReturn(stage());
    when(workTypeMapper.selectOne(any())).thenReturn(workType());
    when(ruleMapper.selectList(any())).thenReturn(List.of(rule()));
    when(profileMapper.selectList(any())).thenReturn(List.of());
    when(registrationMapper.selectList(any())).thenReturn(List.of());
  }

  @Test
  void identifiesMissingStageWorkTypeAndUnpublishedRuleSeparately() {
    CompetitionScoringReadinessVO missingStage = service.check(1L, 1001L, null, 3001L);
    assertThat(checks(missingStage).get("STAGE_MISSING").passed()).isFalse();
    assertThat(missingStage.isReady()).isFalse();

    CompetitionScoringReadinessVO missingWorkType = service.check(1L, 1001L, 2001L, null);
    assertThat(checks(missingWorkType).get("EVENT_WORK_TYPE_MISSING").passed()).isFalse();
    assertThat(missingWorkType.isReady()).isFalse();

    when(ruleMapper.selectList(any())).thenReturn(List.of());
    CompetitionScoringReadinessVO unpublished = service.check(1L, 1001L, 2001L, 3001L);
    assertThat(checks(unpublished).get("SCORE_RULE_UNPUBLISHED").passed()).isFalse();
    assertThat(unpublished.getPublishedScoreRuleVersionId()).isNull();
  }

  @Test
  void eventMustBePublishedAndPublishedRuleMustRemainValid() {
    when(eventMapper.selectOne(any())).thenReturn(event("draft"));
    CompetitionScoringReadinessVO draftEvent = service.check(1L, 1001L, 2001L, 3001L);
    assertThat(checks(draftEvent).get("EVENT_NOT_READY").passed()).isFalse();

    CompetitionScoreRuleEntity invalidRule = rule();
    invalidRule.setTheoryWeight(new BigDecimal("40"));
    invalidRule.setSimulationPracticalWeight(new BigDecimal("50"));
    when(eventMapper.selectOne(any())).thenReturn(event("published"));
    when(ruleMapper.selectList(any())).thenReturn(List.of(invalidRule));
    CompetitionScoringReadinessVO invalid = service.check(1L, 1001L, 2001L, 3001L);
    assertThat(checks(invalid).get("SCORE_RULE_INVALID").passed()).isFalse();
    assertThat(invalid.getPublishedScoreRuleVersionId()).isNull();
  }

  @Test
  void identifiesUnresolvedContestantWorkType() {
    CompetitionContestantProfileEntity profile = profile("unmatched");
    CompetitionRegistrationEntity registration = registration();
    when(profileMapper.selectList(any())).thenReturn(List.of(profile), List.of(profile));
    when(registrationMapper.selectList(any())).thenReturn(List.of(registration), List.of(registration));
    when(memberMapper.selectList(any())).thenReturn(List.of(member("active")));

    CompetitionScoringReadinessVO result = service.check(1L, 1001L, 2001L, 3001L);

    assertThat(checks(result).get("CONTESTANT_WORK_TYPE_UNRESOLVED").passed()).isFalse();
    assertThat(result.isReady()).isFalse();
  }

  @Test
  void identifiesIneligibleContestantMember() {
    CompetitionContestantProfileEntity profile = profile("matched");
    CompetitionRegistrationEntity registration = registration();
    when(profileMapper.selectList(any())).thenReturn(List.of(profile), List.of(profile));
    when(registrationMapper.selectList(any())).thenReturn(List.of(registration), List.of(registration));
    when(memberMapper.selectList(any())).thenReturn(List.of(member("inactive")));

    CompetitionScoringReadinessVO result = service.check(1L, 1001L, 2001L, 3001L);

    assertThat(checks(result).get("CONTESTANT_WORK_TYPE_UNRESOLVED").passed()).isTrue();
    assertThat(checks(result).get("CONTESTANT_INELIGIBLE").passed()).isFalse();
    assertThat(result.isReady()).isFalse();
  }

  @Test
  void eligibleRegistrationWithoutMemberIdIsAlsoIneligible() {
    CompetitionRegistrationEntity registration = registration();
    registration.setMemberId(null);
    when(registrationMapper.selectList(any()))
        .thenReturn(List.of(registration), List.of(registration));

    CompetitionScoringReadinessVO result = service.check(1L, 1001L, 2001L, 3001L);

    assertThat(checks(result).get("CONTESTANT_WORK_TYPE_UNRESOLVED").passed()).isFalse();
    assertThat(checks(result).get("CONTESTANT_INELIGIBLE").passed()).isFalse();
  }

  @Test
  void eligibleRegistrationWithoutProfileIsAlsoIneligible() {
    CompetitionRegistrationEntity registration = registration();
    when(registrationMapper.selectList(any()))
        .thenReturn(List.of(registration), List.of(registration));
    when(profileMapper.selectList(any())).thenReturn(List.of());
    when(memberMapper.selectList(any())).thenReturn(List.of(member("active")));

    CompetitionScoringReadinessVO result = service.check(1L, 1001L, 2001L, 3001L);

    assertThat(checks(result).get("CONTESTANT_WORK_TYPE_UNRESOLVED").passed()).isFalse();
    assertThat(checks(result).get("CONTESTANT_INELIGIBLE").passed()).isFalse();
  }

  @Test
  void completeEligibleRegistrationForAnotherWorkTypeDoesNotFailSelectedScope() {
    CompetitionContestantProfileEntity otherProfile = profile("matched");
    otherProfile.setEventWorkTypeId(3999L);
    CompetitionRegistrationEntity registration = registration();
    when(profileMapper.selectList(any())).thenReturn(List.of(), List.of(otherProfile));
    when(registrationMapper.selectList(any()))
        .thenReturn(List.of(registration), List.of(registration));
    when(memberMapper.selectList(any())).thenReturn(List.of(member("active")));

    CompetitionScoringReadinessVO result = service.check(1L, 1001L, 2001L, 3001L);

    assertThat(checks(result).get("CONTESTANT_WORK_TYPE_UNRESOLVED").passed()).isTrue();
    assertThat(checks(result).get("CONTESTANT_INELIGIBLE").passed()).isTrue();
    assertThat(result.isReady()).isTrue();
  }

  @Test
  void completeScopeIsReadyAndReturnsPublishedRuleVersionId() {
    CompetitionContestantProfileEntity profile = profile("matched");
    CompetitionRegistrationEntity registration = registration();
    when(profileMapper.selectList(any())).thenReturn(List.of(profile), List.of(profile));
    when(registrationMapper.selectList(any())).thenReturn(List.of(registration), List.of(registration));
    when(memberMapper.selectList(any())).thenReturn(List.of(member("active")));

    CompetitionScoringReadinessVO result = service.check(1L, 1001L, 2001L, 3001L);

    assertThat(result.isReady()).isTrue();
    assertThat(result.getPublishedScoreRuleVersionId()).isEqualTo(4001L);
    assertThat(result.getChecks()).hasSize(7);
    assertThat(result.getChecks()).allMatch(CompetitionScoringReadinessCheckVO::passed);
  }

  private Map<String, CompetitionScoringReadinessCheckVO> checks(
      CompetitionScoringReadinessVO result) {
    return result.getChecks().stream().collect(Collectors.toMap(
        CompetitionScoringReadinessCheckVO::code, Function.identity()));
  }

  private CompetitionEventEntity event(String status) {
    CompetitionEventEntity entity = new CompetitionEventEntity();
    entity.setId(1001L);
    entity.setTenantId(1L);
    entity.setStatus(status);
    return entity;
  }

  private CompetitionStageEntity stage() {
    CompetitionStageEntity entity = new CompetitionStageEntity();
    entity.setId(2001L);
    entity.setTenantId(1L);
    entity.setEventId(1001L);
    entity.setStatus("active");
    return entity;
  }

  private CompetitionEventWorkTypeEntity workType() {
    CompetitionEventWorkTypeEntity entity = new CompetitionEventWorkTypeEntity();
    entity.setId(3001L);
    entity.setTenantId(1L);
    entity.setEventId(1001L);
    entity.setStatus("active");
    return entity;
  }

  private CompetitionScoreRuleEntity rule() {
    CompetitionScoreRuleEntity entity = new CompetitionScoreRuleEntity();
    entity.setId(4001L);
    entity.setTenantId(1L);
    entity.setEventId(1001L);
    entity.setStageId(2001L);
    entity.setEventWorkTypeId(3001L);
    entity.setScoringSystem("B");
    entity.setTheoryTemplate("THEORY_90");
    entity.setTheoryWeight(new BigDecimal("40"));
    entity.setSimulationPracticalWeight(new BigDecimal("60"));
    entity.setStatus("published");
    entity.setRuleVersion(1);
    return entity;
  }

  private CompetitionContestantProfileEntity profile(String matchStatus) {
    CompetitionContestantProfileEntity entity = new CompetitionContestantProfileEntity();
    entity.setId(7001L);
    entity.setTenantId(1L);
    entity.setMemberId(5001L);
    entity.setRegistrationId(6001L);
    entity.setEventWorkTypeId(3001L);
    entity.setWorkTypeMatchStatus(matchStatus);
    entity.setDeleted(0);
    return entity;
  }

  private CompetitionEventMemberEntity member(String status) {
    CompetitionEventMemberEntity entity = new CompetitionEventMemberEntity();
    entity.setId(5001L);
    entity.setTenantId(1L);
    entity.setEventId(1001L);
    entity.setRoleType("contestant");
    entity.setStatus(status);
    entity.setDeleted(0);
    return entity;
  }

  private CompetitionRegistrationEntity registration() {
    CompetitionRegistrationEntity entity = new CompetitionRegistrationEntity();
    entity.setId(6001L);
    entity.setTenantId(1L);
    entity.setEventId(1001L);
    entity.setMemberId(5001L);
    entity.setStatus("eligible_activated");
    entity.setDeleted(0);
    return entity;
  }
}
