package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.dto.CompetitionContestantWorkTypeConfirmReq;
import com.guoyu.platform.competition.entity.CompetitionContestantProfileEntity;
import com.guoyu.platform.competition.entity.CompetitionEventMemberEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionRegistrationEntity;
import com.guoyu.platform.competition.enums.CompetitionEventMemberRole;
import com.guoyu.platform.competition.mapper.CompetitionContestantProfileMapper;
import com.guoyu.platform.competition.mapper.CompetitionContestantWorkTypeRow;
import com.guoyu.platform.competition.mapper.CompetitionEventMemberMapper;
import com.guoyu.platform.competition.mapper.CompetitionRegistrationMapper;
import com.guoyu.platform.competition.vo.CompetitionContestantWorkTypeReconciliationVO;
import com.guoyu.platform.competition.vo.CompetitionWorkTypeCandidateVO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class CompetitionContestantWorkTypeDomainService {

  private static final String MATCHED = "matched";
  private static final String UNMATCHED = "unmatched";
  private static final String CONFLICT = "conflict";
  private static final String ACTIVE = "active";

  private final CompetitionContestantProfileMapper profileMapper;
  private final CompetitionEventMemberMapper memberMapper;
  private final CompetitionRegistrationMapper registrationMapper;
  private final CompetitionEventWorkTypeDomainService eventWorkTypeService;

  public CompetitionContestantWorkTypeDomainService(
      CompetitionContestantProfileMapper profileMapper,
      CompetitionEventMemberMapper memberMapper,
      CompetitionRegistrationMapper registrationMapper,
      CompetitionEventWorkTypeDomainService eventWorkTypeService) {
    this.profileMapper = profileMapper;
    this.memberMapper = memberMapper;
    this.registrationMapper = registrationMapper;
    this.eventWorkTypeService = eventWorkTypeService;
  }

  @Transactional(readOnly = true)
  public List<CompetitionContestantWorkTypeReconciliationVO> reconcile(
      Long tenantId,
      Long eventId,
      Long orgId) {
    List<CompetitionContestantWorkTypeRow> rows =
        profileMapper.selectWorkTypeRowsByEvent(tenantId, eventId, orgId);
    if (rows == null) {
      log.warn("Contestant profile mapper returned null reconciliation rows, tenantId={}, eventId={}",
          tenantId, eventId);
      return new ArrayList<>();
    }

    List<CompetitionEventWorkTypeEntity> activeWorkTypes = eventWorkTypeService
        .list(tenantId, eventId)
        .stream()
        .filter(item -> ACTIVE.equals(item.getStatus()))
        .toList();
    Map<Long, CompetitionEventWorkTypeEntity> workTypesById = new HashMap<>();
    activeWorkTypes.forEach(item -> workTypesById.put(item.getId(), item));

    List<CompetitionContestantWorkTypeReconciliationVO> result = new ArrayList<>();
    for (CompetitionContestantWorkTypeRow row : rows) {
      result.add(reconcileOne(row, activeWorkTypes, workTypesById));
    }
    return result;
  }

  @Transactional
  public CompetitionContestantWorkTypeReconciliationVO confirm(
      Long tenantId,
      Long eventId,
      Long contestantMemberId,
      Long operatorUserId,
      CompetitionContestantWorkTypeConfirmReq req,
      Long orgId) {
    CompetitionEventMemberEntity member = memberMapper.selectOne(
        new LambdaQueryWrapper<CompetitionEventMemberEntity>()
            .eq(CompetitionEventMemberEntity::getTenantId, tenantId)
            .eq(CompetitionEventMemberEntity::getId, contestantMemberId)
            .eq(CompetitionEventMemberEntity::getRoleType,
                CompetitionEventMemberRole.CONTESTANT.getCode())
            .eq(CompetitionEventMemberEntity::getDeleted, 0));
    if (member == null || !Objects.equals(eventId, member.getEventId())) {
      log.warn("Contestant work type confirmation crossed event scope, tenantId={}, eventId={}, memberId={}",
          tenantId, eventId, contestantMemberId);
      throw new BusinessException(ErrorCode.NOT_FOUND, "赛事选手不存在");
    }
    if (!ACTIVE.equals(member.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "仅有效赛事选手可确认工种");
    }

    CompetitionContestantProfileEntity profile = profileMapper.selectOne(
        new LambdaQueryWrapper<CompetitionContestantProfileEntity>()
            .eq(CompetitionContestantProfileEntity::getTenantId, tenantId)
            .eq(CompetitionContestantProfileEntity::getMemberId, contestantMemberId)
            .eq(CompetitionContestantProfileEntity::getDeleted, 0));
    if (profile == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "赛事选手扩展不存在");
    }
    if (orgId != null) {
      var registration = registrationMapper.selectOne(
          new LambdaQueryWrapper<CompetitionRegistrationEntity>()
              .eq(CompetitionRegistrationEntity::getTenantId, tenantId)
              .eq(CompetitionRegistrationEntity::getId, profile.getRegistrationId())
              .eq(CompetitionRegistrationEntity::getEventId, eventId)
              .eq(CompetitionRegistrationEntity::getOrgId, orgId)
              .eq(CompetitionRegistrationEntity::getDeleted, 0));
      if (registration == null) {
        log.warn("Contestant work type confirmation crossed organization scope, tenantId={}, eventId={}, memberId={}, orgId={}",
            tenantId, eventId, contestantMemberId, orgId);
        throw new BusinessException(ErrorCode.FORBIDDEN, "无权确认其他企业选手工种");
      }
    }

    CompetitionEventWorkTypeEntity eventWorkType =
        eventWorkTypeService.getById(tenantId, req.eventWorkTypeId());
    if (!Objects.equals(eventId, eventWorkType.getEventId())
        || !ACTIVE.equals(eventWorkType.getStatus())) {
      log.warn("Contestant work type confirmation used another event work type, tenantId={}, eventId={}, memberId={}, eventWorkTypeId={}",
          tenantId, eventId, contestantMemberId, req.eventWorkTypeId());
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "赛事工种无效或不属于当前赛事");
    }

    profile.setEventWorkTypeId(eventWorkType.getId());
    profile.setWorkTypeMatchStatus(MATCHED);
    profile.setWorkTypeMatchNote(
        StringUtils.hasText(req.note()) ? req.note().trim() : "人工确认");
    profile.setUpdatedBy(operatorUserId);
    if (profileMapper.updateById(profile) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "选手工种归属已发生变化，请刷新后重试");
    }

    CompetitionContestantWorkTypeReconciliationVO result =
        new CompetitionContestantWorkTypeReconciliationVO();
    result.setProfileId(profile.getId());
    result.setContestantMemberId(profile.getMemberId());
    result.setRegistrationId(profile.getRegistrationId());
    result.setEventWorkTypeId(eventWorkType.getId());
    result.setMatchStatus(MATCHED);
    result.setConfirmed(true);
    result.setNote(profile.getWorkTypeMatchNote());
    result.setCandidates(List.of(toCandidate(eventWorkType)));
    return result;
  }

  private CompetitionContestantWorkTypeReconciliationVO reconcileOne(
      CompetitionContestantWorkTypeRow row,
      List<CompetitionEventWorkTypeEntity> activeWorkTypes,
      Map<Long, CompetitionEventWorkTypeEntity> workTypesById) {
    CompetitionContestantWorkTypeReconciliationVO result =
        new CompetitionContestantWorkTypeReconciliationVO();
    result.setProfileId(row.getProfileId());
    result.setContestantMemberId(row.getContestantMemberId());
    result.setRegistrationId(row.getRegistrationId());
    result.setRosterWorkType(row.getRosterWorkType());
    result.setNote(row.getWorkTypeMatchNote());

    if (row.getEventWorkTypeId() != null) {
      CompetitionEventWorkTypeEntity confirmed = workTypesById.get(row.getEventWorkTypeId());
      if (confirmed != null && MATCHED.equals(row.getWorkTypeMatchStatus())) {
        result.setEventWorkTypeId(confirmed.getId());
        result.setMatchStatus(MATCHED);
        result.setConfirmed(true);
        result.setCandidates(List.of(toCandidate(confirmed)));
        return result;
      }
      result.setMatchStatus(CONFLICT);
      result.setConfirmed(false);
      result.setNote("已记录的赛事工种无效或不属于当前赛事");
      return result;
    }

    List<CompetitionWorkTypeCandidateVO> candidates = activeWorkTypes.stream()
        .filter(item -> exactRosterTextMatch(
            item.getTenantId(), row.getRosterWorkType(), item))
        .map(this::toCandidate)
        .toList();
    result.setCandidates(candidates);
    result.setConfirmed(false);
    if (candidates.size() == 1) {
      result.setMatchStatus(MATCHED);
      result.setNote("名单工种文本唯一精确命中，待人工确认");
    } else if (candidates.isEmpty()) {
      result.setMatchStatus(UNMATCHED);
      result.setNote("名单工种文本未命中赛事工种");
    } else {
      result.setMatchStatus(CONFLICT);
      result.setNote("名单工种文本命中多个赛事工种，禁止自动猜测");
    }
    return result;
  }

  private CompetitionWorkTypeCandidateVO toCandidate(
      CompetitionEventWorkTypeEntity eventWorkType) {
    CompetitionWorkTypeCandidateVO candidate = new CompetitionWorkTypeCandidateVO();
    candidate.setEventWorkTypeId(eventWorkType.getId());
    candidate.setWorkTypeId(eventWorkType.getWorkTypeId());
    candidate.setDisplayName(eventWorkType.getDisplayName());
    return candidate;
  }

  private boolean exactRosterTextMatch(
      Long tenantId,
      String rosterWorkType,
      CompetitionEventWorkTypeEntity eventWorkType) {
    if (Objects.equals(rosterWorkType, eventWorkType.getDisplayName())) {
      return true;
    }
    String sourceName =
        eventWorkTypeService.findActiveSourceName(tenantId, eventWorkType.getWorkTypeId());
    return Objects.equals(rosterWorkType, sourceName);
  }

}
