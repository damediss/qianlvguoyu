package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.competition.entity.CompetitionContestantProfileEntity;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventMemberEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionRegistrationEntity;
import com.guoyu.platform.competition.entity.CompetitionScoreRuleEntity;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.enums.CompetitionEventMemberRole;
import com.guoyu.platform.competition.enums.CompetitionEventStatus;
import com.guoyu.platform.competition.enums.CompetitionScoreRuleStatus;
import com.guoyu.platform.competition.enums.CompetitionStageStatus;
import com.guoyu.platform.competition.mapper.CompetitionContestantProfileMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventMemberMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
import com.guoyu.platform.competition.mapper.CompetitionRegistrationMapper;
import com.guoyu.platform.competition.mapper.CompetitionScoreRuleMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageMapper;
import com.guoyu.platform.competition.vo.CompetitionScoringReadinessCheckVO;
import com.guoyu.platform.competition.vo.CompetitionScoringReadinessVO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionScoringReadinessDomainService {

  private static final Set<String> ELIGIBLE_STATUSES =
      Set.of("eligible_unactivated", "eligible_activated");

  private final CompetitionEventMapper eventMapper;
  private final CompetitionStageMapper stageMapper;
  private final CompetitionEventWorkTypeMapper eventWorkTypeMapper;
  private final CompetitionScoreRuleMapper scoreRuleMapper;
  private final CompetitionContestantProfileMapper profileMapper;
  private final CompetitionEventMemberMapper memberMapper;
  private final CompetitionRegistrationMapper registrationMapper;
  private final CompetitionScoreRuleDomainService scoreRuleDomainService;

  public CompetitionScoringReadinessDomainService(
      CompetitionEventMapper eventMapper,
      CompetitionStageMapper stageMapper,
      CompetitionEventWorkTypeMapper eventWorkTypeMapper,
      CompetitionScoreRuleMapper scoreRuleMapper,
      CompetitionContestantProfileMapper profileMapper,
      CompetitionEventMemberMapper memberMapper,
      CompetitionRegistrationMapper registrationMapper,
      CompetitionScoreRuleDomainService scoreRuleDomainService) {
    this.eventMapper = eventMapper;
    this.stageMapper = stageMapper;
    this.eventWorkTypeMapper = eventWorkTypeMapper;
    this.scoreRuleMapper = scoreRuleMapper;
    this.profileMapper = profileMapper;
    this.memberMapper = memberMapper;
    this.registrationMapper = registrationMapper;
    this.scoreRuleDomainService = scoreRuleDomainService;
  }

  @Transactional(readOnly = true)
  public CompetitionScoringReadinessVO check(
      Long tenantId, Long eventId, Long stageId, Long eventWorkTypeId) {
    return check(tenantId, eventId, stageId, eventWorkTypeId, null);
  }

  @Transactional(readOnly = true)
  public CompetitionScoringReadinessVO check(
      Long tenantId,
      Long eventId,
      Long stageId,
      Long eventWorkTypeId,
      Long eventWorkTypeLevelId) {
    List<CompetitionScoringReadinessCheckVO> checks = new ArrayList<>();

    CompetitionEventEntity event = findEvent(tenantId, eventId);
    boolean eventReady = event != null
        && CompetitionEventStatus.PUBLISHED.getCode().equals(event.getStatus());
    checks.add(check(
        "EVENT_NOT_READY", eventReady,
        eventReady ? "赛事已发布" : "赛事不存在或尚未发布"));

    CompetitionStageEntity stage = findStage(tenantId, eventId, stageId);
    boolean stageReady = stage != null && isScoringStageReady(stage.getStatus());
    checks.add(check(
        "STAGE_MISSING", stageReady,
        stageReady ? "赛段有效" : "未指定当前赛事的有效赛段"));

    CompetitionEventWorkTypeEntity eventWorkType =
        findEventWorkType(tenantId, eventId, eventWorkTypeId);
    boolean eventWorkTypeReady =
        eventWorkType != null && "active".equals(eventWorkType.getStatus());
    checks.add(check(
        "EVENT_WORK_TYPE_MISSING", eventWorkTypeReady,
        eventWorkTypeReady ? "赛事工种有效" : "未指定当前赛事的有效赛事工种"));

    List<CompetitionScoreRuleEntity> publishedRules =
        stageReady && eventWorkTypeReady
            ? findPublishedRules(tenantId, eventId, stageId, eventWorkTypeId, eventWorkTypeLevelId)
            : Collections.emptyList();
    boolean uniquePublishedRule = publishedRules.size() == 1;
    checks.add(check(
        "SCORE_RULE_UNPUBLISHED", uniquePublishedRule,
        uniquePublishedRule ? "存在唯一已发布评分规则" : publishedRuleMessage(publishedRules.size())));

    CompetitionScoreRuleEntity publishedRule =
        uniquePublishedRule ? publishedRules.get(0) : null;
    boolean validRule = publishedRule == null || scoreRuleDomainService.isValidRule(publishedRule);
    checks.add(check(
        "SCORE_RULE_INVALID", validRule,
        publishedRule == null
            ? "评分规则发布后校验配置"
            : validRule ? "已发布评分规则配置有效" : "已发布评分规则模板或权重不合法"));

    CandidateChecks candidateChecks = eventWorkTypeReady
        ? checkCandidates(tenantId, eventId, stageId, eventWorkTypeId, eventWorkTypeLevelId)
        : CandidateChecks.notApplicable();
    checks.add(check(
        "CONTESTANT_WORK_TYPE_UNRESOLVED",
        candidateChecks.workTypesResolved(),
        candidateChecks.workTypeMessage()));
    checks.add(check(
        "CONTESTANT_INELIGIBLE",
        candidateChecks.allCandidatesEligible(),
        candidateChecks.eligibilityMessage()));

    CompetitionScoringReadinessVO result = new CompetitionScoringReadinessVO();
    result.setEventId(eventId);
    result.setStageId(stageId);
    result.setEventWorkTypeId(eventWorkTypeId);
    result.setEventWorkTypeLevelId(eventWorkTypeLevelId);
    result.setChecks(checks);
    result.setReady(checks.stream().allMatch(CompetitionScoringReadinessCheckVO::passed));
    if (uniquePublishedRule && validRule) {
      result.setPublishedScoreRuleVersionId(publishedRule.getId());
    }
    return result;
  }

  private CandidateChecks checkCandidates(
      Long tenantId,
      Long eventId,
      Long stageId,
      Long eventWorkTypeId,
      Long eventWorkTypeLevelId) {
    List<CompetitionContestantProfileEntity> scopeProfiles = safeList(
        profileMapper.selectList(new LambdaQueryWrapper<CompetitionContestantProfileEntity>()
            .eq(CompetitionContestantProfileEntity::getTenantId, tenantId)
            .eq(CompetitionContestantProfileEntity::getEventWorkTypeId, eventWorkTypeId)
            .eq(eventWorkTypeLevelId != null,
                CompetitionContestantProfileEntity::getEventWorkTypeLevelId, eventWorkTypeLevelId)
            .and(query -> query.isNull(CompetitionContestantProfileEntity::getCurrentStageId)
                .or()
                .eq(CompetitionContestantProfileEntity::getCurrentStageId, stageId))
            .eq(CompetitionContestantProfileEntity::getDeleted, 0)),
        "scope profiles", tenantId, eventId);

    List<CompetitionRegistrationEntity> eligibleRegistrations = safeList(
        registrationMapper.selectList(new LambdaQueryWrapper<CompetitionRegistrationEntity>()
            .eq(CompetitionRegistrationEntity::getTenantId, tenantId)
            .eq(CompetitionRegistrationEntity::getEventId, eventId)
            .in(CompetitionRegistrationEntity::getStatus, ELIGIBLE_STATUSES)
            .eq(CompetitionRegistrationEntity::getDeleted, 0)),
        "eligible registrations", tenantId, eventId);

    Set<Long> memberIds = new HashSet<>();
    Set<Long> registrationIds = new HashSet<>();
    scopeProfiles.forEach(profile -> {
      if (profile.getMemberId() != null) {
        memberIds.add(profile.getMemberId());
      }
      if (profile.getRegistrationId() != null) {
        registrationIds.add(profile.getRegistrationId());
      }
    });
    eligibleRegistrations.forEach(registration -> {
      if (registration.getMemberId() != null) {
        memberIds.add(registration.getMemberId());
      }
      if (registration.getId() != null) {
        registrationIds.add(registration.getId());
      }
    });

    List<CompetitionContestantProfileEntity> relatedProfiles =
        findRelatedProfiles(tenantId, memberIds, registrationIds);
    Map<Long, List<CompetitionContestantProfileEntity>> profilesByMember =
        relatedProfiles.stream()
            .filter(profile -> profile.getMemberId() != null)
            .collect(Collectors.groupingBy(CompetitionContestantProfileEntity::getMemberId));
    Map<Long, List<CompetitionContestantProfileEntity>> profilesByRegistration =
        relatedProfiles.stream()
            .filter(profile -> profile.getRegistrationId() != null)
            .collect(Collectors.groupingBy(CompetitionContestantProfileEntity::getRegistrationId));

    Map<Long, CompetitionEventMemberEntity> membersById =
        findMembers(tenantId, memberIds).stream()
            .filter(member -> member.getId() != null)
            .collect(Collectors.toMap(
                CompetitionEventMemberEntity::getId, Function.identity(), (left, right) -> left));
    // competition_contestant_profile has no event_id of its own. Keep readiness
    // anchored to the event member before evaluating the candidate flags, so a
    // stale profile from another event cannot contaminate this scoring scope.
    List<CompetitionContestantProfileEntity> eventScopeProfiles = scopeProfiles.stream()
        .filter(profile -> {
          CompetitionEventMemberEntity member = membersById.get(profile.getMemberId());
          return member != null && Objects.equals(member.getEventId(), eventId);
        })
        .toList();
    boolean unresolvedEligibleRegistration = eligibleRegistrations.stream()
        .anyMatch(registration -> !hasResolvedProfile(
            registration, profilesByMember, profilesByRegistration));
    boolean unresolvedScopeProfile = eventScopeProfiles.stream().anyMatch(profile ->
        profile.getEventWorkTypeId() == null
            || !"matched".equals(profile.getWorkTypeMatchStatus()));
    boolean workTypesResolved = !unresolvedEligibleRegistration && !unresolvedScopeProfile;

    Map<Long, CompetitionRegistrationEntity> registrationsById =
        findRegistrations(tenantId, registrationIds).stream()
            .filter(registration -> registration.getId() != null)
            .collect(Collectors.toMap(
                CompetitionRegistrationEntity::getId, Function.identity(), (left, right) -> left));

    boolean allScopeProfilesEligible = eventScopeProfiles.stream().allMatch(profile ->
        isEligibleCandidate(eventId, profile, membersById, registrationsById));
    Set<Long> scopeMemberIds = eventScopeProfiles.stream()
        .map(CompetitionContestantProfileEntity::getMemberId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    Set<Long> scopeRegistrationIds = eventScopeProfiles.stream()
        .map(CompetitionContestantProfileEntity::getRegistrationId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    boolean allEligibleRegistrationsStructured = eligibleRegistrations.stream().allMatch(
        registration -> {
          boolean currentScope = scopeMemberIds.contains(registration.getMemberId())
              || scopeRegistrationIds.contains(registration.getId());
          if (!currentScope && hasResolvedProfile(
              registration, profilesByMember, profilesByRegistration)) {
            return true;
          }
          return isEligibleRegistrationStructure(
              eventId, registration, profilesByMember, profilesByRegistration, membersById);
        });
    boolean allCandidatesEligible =
        allScopeProfilesEligible && allEligibleRegistrationsStructured;
    return new CandidateChecks(
        workTypesResolved,
        allCandidatesEligible,
        workTypesResolved
            ? "当前赛事有效资格选手均已明确匹配赛事工种"
            : "存在未匹配、冲突或缺少赛事工种归属的有效资格选手",
        allCandidatesEligible
            ? "当前赛事工种候选选手资格和成员关系均有效"
            : "存在成员状态、角色、赛事或资格不一致的评分候选选手");
  }

  private boolean hasResolvedProfile(
      CompetitionRegistrationEntity registration,
      Map<Long, List<CompetitionContestantProfileEntity>> profilesByMember,
      Map<Long, List<CompetitionContestantProfileEntity>> profilesByRegistration) {
    if (registration.getMemberId() == null) {
      return false;
    }
    Map<Long, CompetitionContestantProfileEntity> candidates = new HashMap<>();
    profilesByMember.getOrDefault(registration.getMemberId(), Collections.emptyList())
        .forEach(profile -> candidates.put(profile.getId(), profile));
    profilesByRegistration.getOrDefault(registration.getId(), Collections.emptyList())
        .forEach(profile -> candidates.put(profile.getId(), profile));
    return candidates.size() == 1 && candidates.values().stream().allMatch(profile ->
        Objects.equals(profile.getMemberId(), registration.getMemberId())
            && Objects.equals(profile.getRegistrationId(), registration.getId())
            && profile.getEventWorkTypeId() != null
            && "matched".equals(profile.getWorkTypeMatchStatus()));
  }

  private boolean isEligibleCandidate(
      Long eventId,
      CompetitionContestantProfileEntity profile,
      Map<Long, CompetitionEventMemberEntity> membersById,
      Map<Long, CompetitionRegistrationEntity> registrationsById) {
    CompetitionEventMemberEntity member = membersById.get(profile.getMemberId());
    CompetitionRegistrationEntity registration = registrationsById.get(profile.getRegistrationId());
    return member != null
        && Objects.equals(member.getEventId(), eventId)
        && CompetitionEventMemberRole.CONTESTANT.getCode().equals(member.getRoleType())
        && "active".equals(member.getStatus())
        && registration != null
        && Objects.equals(registration.getEventId(), eventId)
        && ELIGIBLE_STATUSES.contains(registration.getStatus())
        && Objects.equals(registration.getMemberId(), profile.getMemberId());
  }

  private boolean isEligibleRegistrationStructure(
      Long eventId,
      CompetitionRegistrationEntity registration,
      Map<Long, List<CompetitionContestantProfileEntity>> profilesByMember,
      Map<Long, List<CompetitionContestantProfileEntity>> profilesByRegistration,
      Map<Long, CompetitionEventMemberEntity> membersById) {
    if (registration.getMemberId() == null) {
      return false;
    }
    Map<Long, CompetitionContestantProfileEntity> candidates = new HashMap<>();
    profilesByMember.getOrDefault(registration.getMemberId(), Collections.emptyList())
        .forEach(profile -> candidates.put(profile.getId(), profile));
    profilesByRegistration.getOrDefault(registration.getId(), Collections.emptyList())
        .forEach(profile -> candidates.put(profile.getId(), profile));
    if (candidates.size() != 1) {
      return false;
    }
    CompetitionContestantProfileEntity profile = candidates.values().iterator().next();
    CompetitionEventMemberEntity member = membersById.get(registration.getMemberId());
    return Objects.equals(profile.getMemberId(), registration.getMemberId())
        && Objects.equals(profile.getRegistrationId(), registration.getId())
        && member != null
        && Objects.equals(member.getEventId(), eventId)
        && CompetitionEventMemberRole.CONTESTANT.getCode().equals(member.getRoleType())
        && "active".equals(member.getStatus());
  }

  private CompetitionEventEntity findEvent(Long tenantId, Long eventId) {
    if (eventId == null) {
      return null;
    }
    return eventMapper.selectOne(new LambdaQueryWrapper<CompetitionEventEntity>()
        .eq(CompetitionEventEntity::getTenantId, tenantId)
        .eq(CompetitionEventEntity::getId, eventId));
  }

  private CompetitionStageEntity findStage(Long tenantId, Long eventId, Long stageId) {
    if (stageId == null) {
      return null;
    }
    return stageMapper.selectOne(new LambdaQueryWrapper<CompetitionStageEntity>()
        .eq(CompetitionStageEntity::getTenantId, tenantId)
        .eq(CompetitionStageEntity::getId, stageId)
        .eq(CompetitionStageEntity::getEventId, eventId));
  }

  private CompetitionEventWorkTypeEntity findEventWorkType(
      Long tenantId, Long eventId, Long eventWorkTypeId) {
    if (eventWorkTypeId == null) {
      return null;
    }
    return eventWorkTypeMapper.selectOne(
        new LambdaQueryWrapper<CompetitionEventWorkTypeEntity>()
            .eq(CompetitionEventWorkTypeEntity::getTenantId, tenantId)
            .eq(CompetitionEventWorkTypeEntity::getId, eventWorkTypeId)
            .eq(CompetitionEventWorkTypeEntity::getEventId, eventId));
  }

  private List<CompetitionScoreRuleEntity> findPublishedRules(
      Long tenantId,
      Long eventId,
      Long stageId,
      Long eventWorkTypeId,
      Long eventWorkTypeLevelId) {
    return safeList(scoreRuleMapper.selectList(
        new LambdaQueryWrapper<CompetitionScoreRuleEntity>()
            .eq(CompetitionScoreRuleEntity::getTenantId, tenantId)
            .eq(CompetitionScoreRuleEntity::getEventId, eventId)
            .eq(CompetitionScoreRuleEntity::getStageId, stageId)
            .eq(CompetitionScoreRuleEntity::getEventWorkTypeId, eventWorkTypeId)
            .eq(eventWorkTypeLevelId != null,
                CompetitionScoreRuleEntity::getEventWorkTypeLevelId, eventWorkTypeLevelId)
            .eq(CompetitionScoreRuleEntity::getStatus, CompetitionScoreRuleStatus.PUBLISHED.getCode())
            .eq(CompetitionScoreRuleEntity::getDeleted, 0)),
        "published score rules", tenantId, eventId);
  }

  private boolean isScoringStageReady(String status) {
    return "active".equals(status)
        || CompetitionStageStatus.READY_LOCKED.getCode().equals(status)
        || CompetitionStageStatus.IN_PROGRESS.getCode().equals(status)
        || CompetitionStageStatus.CLOSED.getCode().equals(status)
        || CompetitionStageStatus.SETTLED.getCode().equals(status)
        || CompetitionStageStatus.RESULT_PUBLISHED.getCode().equals(status);
  }

  private List<CompetitionContestantProfileEntity> findRelatedProfiles(
      Long tenantId, Set<Long> memberIds, Set<Long> registrationIds) {
    if (memberIds.isEmpty() && registrationIds.isEmpty()) {
      return Collections.emptyList();
    }
    LambdaQueryWrapper<CompetitionContestantProfileEntity> wrapper =
        new LambdaQueryWrapper<CompetitionContestantProfileEntity>()
            .eq(CompetitionContestantProfileEntity::getTenantId, tenantId)
            .eq(CompetitionContestantProfileEntity::getDeleted, 0)
            .and(query -> {
              if (!memberIds.isEmpty()) {
                query.in(CompetitionContestantProfileEntity::getMemberId, memberIds);
              }
              if (!registrationIds.isEmpty()) {
                if (!memberIds.isEmpty()) {
                  query.or();
                }
                query.in(CompetitionContestantProfileEntity::getRegistrationId, registrationIds);
              }
            });
    return safeList(profileMapper.selectList(wrapper), "related profiles", tenantId, null);
  }

  private List<CompetitionEventMemberEntity> findMembers(Long tenantId, Set<Long> memberIds) {
    if (memberIds.isEmpty()) {
      return Collections.emptyList();
    }
    return safeList(memberMapper.selectList(
        new LambdaQueryWrapper<CompetitionEventMemberEntity>()
            .eq(CompetitionEventMemberEntity::getTenantId, tenantId)
            .in(CompetitionEventMemberEntity::getId, memberIds)
            .eq(CompetitionEventMemberEntity::getDeleted, 0)),
        "members", tenantId, null);
  }

  private List<CompetitionRegistrationEntity> findRegistrations(
      Long tenantId, Set<Long> registrationIds) {
    if (registrationIds.isEmpty()) {
      return Collections.emptyList();
    }
    return safeList(registrationMapper.selectList(
        new LambdaQueryWrapper<CompetitionRegistrationEntity>()
            .eq(CompetitionRegistrationEntity::getTenantId, tenantId)
            .in(CompetitionRegistrationEntity::getId, registrationIds)
            .eq(CompetitionRegistrationEntity::getDeleted, 0)),
        "registrations", tenantId, null);
  }

  private <T> List<T> safeList(List<T> values, String source, Long tenantId, Long eventId) {
    if (values != null) {
      return values;
    }
    log.warn("Competition readiness mapper returned null {}, tenantId={}, eventId={}",
        source, tenantId, eventId);
    return Collections.emptyList();
  }

  private CompetitionScoringReadinessCheckVO check(
      String code, boolean passed, String message) {
    return new CompetitionScoringReadinessCheckVO(code, passed, message);
  }

  private String publishedRuleMessage(int count) {
    return count == 0 ? "当前范围没有已发布评分规则" : "当前范围存在多个已发布评分规则";
  }

  private record CandidateChecks(
      boolean workTypesResolved,
      boolean allCandidatesEligible,
      String workTypeMessage,
      String eligibilityMessage) {

    private static CandidateChecks notApplicable() {
      return new CandidateChecks(
          true, true, "赛事工种有效后检查选手归属", "赛事工种有效后检查选手资格");
    }
  }
}
