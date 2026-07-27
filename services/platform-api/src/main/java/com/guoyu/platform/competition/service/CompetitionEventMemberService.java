package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.entity.CompetitionContestantProfileEntity;
import com.guoyu.platform.competition.entity.CompetitionEventMemberEntity;
import com.guoyu.platform.competition.enums.CompetitionEventMemberRole;
import com.guoyu.platform.competition.mapper.CompetitionContestantProfileMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventMemberMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class CompetitionEventMemberService {

  private final CompetitionEventMemberMapper memberMapper;
  private final CompetitionContestantProfileMapper contestantProfileMapper;

  public CompetitionEventMemberService(
      CompetitionEventMemberMapper memberMapper,
      CompetitionContestantProfileMapper contestantProfileMapper) {
    this.memberMapper = memberMapper;
    this.contestantProfileMapper = contestantProfileMapper;
  }

  public CompetitionEventMemberEntity createContestantMember(
      Long tenantId, Long eventId, Long personId, Long operatorUserId, LocalDateTime joinedAt) {
    CompetitionEventMemberEntity existing = memberMapper.selectOne(
        new LambdaQueryWrapper<CompetitionEventMemberEntity>()
            .eq(CompetitionEventMemberEntity::getTenantId, tenantId)
            .eq(CompetitionEventMemberEntity::getEventId, eventId)
            .eq(CompetitionEventMemberEntity::getPersonId, personId)
            .eq(CompetitionEventMemberEntity::getRoleType, CompetitionEventMemberRole.CONTESTANT.getCode())
            .eq(CompetitionEventMemberEntity::getDeleted, 0));
    if (existing != null) {
      throw new BusinessException(ErrorCode.CONFLICT, "该人员已是当前赛事选手");
    }

    CompetitionEventMemberEntity member = new CompetitionEventMemberEntity();
    member.setTenantId(tenantId);
    member.setEventId(eventId);
    member.setPersonId(personId);
    member.setRoleType(CompetitionEventMemberRole.CONTESTANT.getCode());
    member.setStatus("active");
    member.setJoinedAt(joinedAt);
    member.setCreatedBy(operatorUserId);
    member.setUpdatedBy(operatorUserId);
    member.setDeleted(0);
    try {
      if (memberMapper.insert(member) != 1 || member.getId() == null) {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建赛事选手成员失败");
      }
      return member;
    } catch (DuplicateKeyException ex) {
      throw new BusinessException(ErrorCode.CONFLICT, "该人员已是当前赛事选手");
    }
  }

  public void createContestantProfile(Long tenantId, Long memberId, Long registrationId, Long operatorUserId) {
    CompetitionContestantProfileEntity profile = new CompetitionContestantProfileEntity();
    profile.setTenantId(tenantId);
    profile.setMemberId(memberId);
    profile.setRegistrationId(registrationId);
    profile.setPromotionStatus("pending");
    profile.setTotalScore(BigDecimal.ZERO);
    profile.setCreatedBy(operatorUserId);
    profile.setUpdatedBy(operatorUserId);
    profile.setDeleted(0);
    if (contestantProfileMapper.insert(profile) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建选手角色扩展失败");
    }
  }
}
