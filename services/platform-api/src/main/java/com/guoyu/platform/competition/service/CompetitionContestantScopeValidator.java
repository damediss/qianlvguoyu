package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.competition.entity.CompetitionContestantProfileEntity;
import com.guoyu.platform.competition.entity.CompetitionEventMemberEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryAttemptEntity;
import com.guoyu.platform.competition.enums.CompetitionEventMemberRole;
import com.guoyu.platform.competition.mapper.CompetitionContestantProfileMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventMemberMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryAttemptMapper;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Centralizes contestant scope checks and the profile-first locking contract. */
@Service
@Slf4j
public class CompetitionContestantScopeValidator {

  public enum UpdateIntent {
    PARTICIPATION,
    CLEANUP_ONLY
  }

  private final CompetitionContestantProfileMapper profileMapper;
  private final CompetitionEventMemberMapper memberMapper;
  private final CompetitionTheoryAttemptMapper attemptMapper;

  /** Kept for focused unit tests that only exercise the read-only validator. */
  public CompetitionContestantScopeValidator(
      CompetitionContestantProfileMapper profileMapper,
      CompetitionEventMemberMapper memberMapper) {
    this(profileMapper, memberMapper, null);
  }

  @Autowired
  public CompetitionContestantScopeValidator(
      CompetitionContestantProfileMapper profileMapper,
      CompetitionEventMemberMapper memberMapper,
      CompetitionTheoryAttemptMapper attemptMapper) {
    this.profileMapper = profileMapper;
    this.memberMapper = memberMapper;
    this.attemptMapper = attemptMapper;
  }

  /** Read-only lookup. It intentionally does not acquire a row lock. */
  public void validate(
      Long tenantId,
      Long eventId,
      Long stageId,
      Long eventWorkTypeId,
      Long contestantMemberId) {
    CompetitionEventMemberEntity member = findMember(
        tenantId, eventId, contestantMemberId);
    CompetitionContestantProfileEntity profile = profileMapper.selectOne(
        new LambdaQueryWrapper<CompetitionContestantProfileEntity>()
            .eq(CompetitionContestantProfileEntity::getTenantId, tenantId)
            .eq(CompetitionContestantProfileEntity::getMemberId, contestantMemberId)
            .eq(eventWorkTypeId != null, CompetitionContestantProfileEntity::getEventWorkTypeId,
                eventWorkTypeId)
            .eq(CompetitionContestantProfileEntity::getWorkTypeMatchStatus, "matched")
            .eq(CompetitionContestantProfileEntity::getDeleted, 0));
    validateCommon(tenantId, eventId, stageId, eventWorkTypeId, contestantMemberId,
        member, profile, false, UpdateIntent.PARTICIPATION);
  }

  /** Locks the shared contestant profile before any participant-side write. */
  public CompetitionContestantScope validateForUpdate(
      Long tenantId,
      Long eventId,
      Long stageId,
      Long eventWorkTypeId,
      Long contestantMemberId) {
    return validateForUpdate(
        tenantId, eventId, stageId, eventWorkTypeId, contestantMemberId,
        UpdateIntent.PARTICIPATION);
  }

  /**
   * Locks the profile and applies either the strict stage contract or cleanup-only semantics.
   * No caller may duplicate the stage check after this method returns.
   */
  public CompetitionContestantScope validateForUpdate(
      Long tenantId,
      Long eventId,
      Long stageId,
      Long eventWorkTypeId,
      Long contestantMemberId,
      UpdateIntent intent) {
    requireTenantContext(tenantId);
    CompetitionContestantProfileEntity profile = eventWorkTypeId == null
        ? profileMapper.selectForUpdateByTenantAndMember(tenantId, contestantMemberId)
        : profileMapper.selectForUpdateByTenantMemberAndWorkType(
            tenantId, contestantMemberId, eventWorkTypeId);
    CompetitionEventMemberEntity member = findMember(tenantId, eventId, contestantMemberId);
    validateCommon(tenantId, eventId, stageId, eventWorkTypeId, contestantMemberId,
        member, profile, true, intent);
    if (intent == UpdateIntent.PARTICIPATION) {
      ensureNoConcurrentStageAttempt(tenantId, stageId, profile);
    }
    return new CompetitionContestantScope(member, profile);
  }

  /** Shared lock primitive for scope changes and future stage transfers. */
  public CompetitionContestantScope lockForScopeChange(
      Long tenantId, Long eventId, Long contestantMemberId) {
    requireTenantContext(tenantId);
    CompetitionContestantProfileEntity profile = profileMapper.selectForUpdateByTenantAndMember(
        tenantId, contestantMemberId);
    CompetitionEventMemberEntity member = findMember(tenantId, eventId, contestantMemberId);
    validateCommon(tenantId, eventId, null, profile == null ? null : profile.getEventWorkTypeId(),
        contestantMemberId, member, profile, true, UpdateIntent.CLEANUP_ONLY);
    return new CompetitionContestantScope(member, profile);
  }

  private CompetitionEventMemberEntity findMember(
      Long tenantId, Long eventId, Long contestantMemberId) {
    return memberMapper.selectOne(
        new LambdaQueryWrapper<CompetitionEventMemberEntity>()
            .eq(CompetitionEventMemberEntity::getTenantId, tenantId)
            .eq(CompetitionEventMemberEntity::getId, contestantMemberId)
            .eq(CompetitionEventMemberEntity::getEventId, eventId)
            .eq(CompetitionEventMemberEntity::getRoleType,
                CompetitionEventMemberRole.CONTESTANT.getCode())
            .eq(CompetitionEventMemberEntity::getStatus, "active")
            .eq(CompetitionEventMemberEntity::getDeleted, 0));
  }

  private void validateCommon(
      Long tenantId,
      Long eventId,
      Long stageId,
      Long eventWorkTypeId,
      Long contestantMemberId,
      CompetitionEventMemberEntity member,
      CompetitionContestantProfileEntity profile,
      boolean locked,
      UpdateIntent intent) {
    if (member == null) {
      reject("member is not active in event", "选手不属于当前赛事有效参赛成员",
          tenantId, eventId, stageId, eventWorkTypeId, contestantMemberId);
    }
    if (profile == null || !Objects.equals(profile.getMemberId(), contestantMemberId)
        || !"matched".equals(profile.getWorkTypeMatchStatus())) {
      reject("matched profile is missing", "选手尚未报名当前赛项或工种尚未完成匹配",
          tenantId, eventId, stageId, eventWorkTypeId, contestantMemberId);
    }
    if (eventWorkTypeId != null && !Objects.equals(profile.getEventWorkTypeId(), eventWorkTypeId)) {
      reject("event work type mismatch", "选手已归属其他赛事工种",
          tenantId, eventId, stageId, eventWorkTypeId, contestantMemberId);
    }
    if (intent == UpdateIntent.PARTICIPATION && profile.getCurrentStageId() != null
        && !Objects.equals(profile.getCurrentStageId(), stageId)) {
      reject("stage mismatch", "选手已归属其他赛段，不能处理当前赛段成绩",
          tenantId, eventId, stageId, eventWorkTypeId, contestantMemberId);
    }
    if (locked && !Objects.equals(profile.getTenantId(), tenantId)) {
      reject("profile tenant mismatch", "选手赛事范围不匹配",
          tenantId, eventId, stageId, eventWorkTypeId, contestantMemberId);
    }
  }

  private void ensureNoConcurrentStageAttempt(
      Long tenantId, Long requestedStageId, CompetitionContestantProfileEntity profile) {
    if (attemptMapper == null || requestedStageId == null || profile.getCurrentStageId() != null) {
      return;
    }
    List<CompetitionTheoryAttemptEntity> attempts = attemptMapper.selectNonTerminalForUpdateByProfile(
        tenantId, profile.getId());
    if (attempts == null) {
      return;
    }
    boolean differentStageExists = attempts.stream()
        .filter(item -> item != null && item.getStageId() != null)
        .anyMatch(item -> !Objects.equals(item.getStageId(), requestedStageId));
    if (differentStageExists) {
      log.warn("Competition contestant has non-terminal attempt in another stage, tenantId={}, profileId={}, stageId={}",
          tenantId, profile.getId(), requestedStageId);
      throw new BusinessException(ErrorCode.CONFLICT, "选手已有其他赛段进行中的考试记录");
    }
  }

  private void requireTenantContext(Long tenantId) {
    Long contextTenantId = TenantContextHolder.getTenantId();
    if (contextTenantId == null || !Objects.equals(contextTenantId, tenantId)) {
      log.warn("Competition contestant scope tenant context mismatch, contextTenantId={}, tenantId={}",
          contextTenantId, tenantId);
      throw new BusinessException(ErrorCode.FORBIDDEN, "赛事租户上下文不匹配");
    }
  }

  private void reject(
      String reason, String message, Long tenantId, Long eventId, Long stageId,
      Long eventWorkTypeId, Long contestantMemberId) {
    log.warn("Competition contestant scope rejected: {}, tenantId={}, eventId={}, stageId={}, eventWorkTypeId={}, contestantMemberId={}",
        reason, tenantId, eventId, stageId, eventWorkTypeId, contestantMemberId);
    throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, message);
  }
}
