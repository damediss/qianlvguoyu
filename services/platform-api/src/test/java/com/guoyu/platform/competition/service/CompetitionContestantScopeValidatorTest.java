package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.competition.entity.CompetitionContestantProfileEntity;
import com.guoyu.platform.competition.entity.CompetitionEventMemberEntity;
import com.guoyu.platform.competition.mapper.CompetitionContestantProfileMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventMemberMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CompetitionContestantScopeValidatorTest {
  private static final Long TENANT_ID = 1L;
  private static final Long EVENT_ID = 100L;
  private static final Long STAGE_A_ID = 200L;
  private static final Long STAGE_B_ID = 201L;
  private static final Long EVENT_WORK_TYPE_ID = 300L;
  private static final Long OTHER_EVENT_WORK_TYPE_ID = 301L;
  private static final Long CONTESTANT_MEMBER_ID = 400L;

  private CompetitionEventMemberMapper memberMapper;
  private CompetitionContestantProfileMapper profileMapper;
  private CompetitionContestantScopeValidator validator;

  @BeforeEach
  void setUp() {
    memberMapper = Mockito.mock(CompetitionEventMemberMapper.class);
    profileMapper = Mockito.mock(CompetitionContestantProfileMapper.class);
    validator = new CompetitionContestantScopeValidator(profileMapper, memberMapper);
    when(memberMapper.selectOne(any())).thenReturn(member());
  }

  @Test
  void contestantAssignedToAnotherStageIsRejectedWithoutWrites() {
    when(profileMapper.selectOne(any())).thenReturn(profile(STAGE_B_ID, EVENT_WORK_TYPE_ID));

    assertThatThrownBy(() -> validate(STAGE_A_ID, EVENT_WORK_TYPE_ID))
        .isInstanceOf(BusinessException.class)
        .hasMessage("选手已归属其他赛段，不能处理当前赛段成绩");

    verify(memberMapper).selectOne(any());
    verify(profileMapper).selectOne(any());
    verifyNoMoreInteractions(memberMapper, profileMapper);
  }

  @Test
  void contestantAssignedToAnotherWorkTypeIsRejected() {
    when(profileMapper.selectOne(any())).thenReturn(profile(null, OTHER_EVENT_WORK_TYPE_ID));

    assertThatThrownBy(() -> validate(STAGE_A_ID, EVENT_WORK_TYPE_ID))
        .isInstanceOf(BusinessException.class)
        .hasMessage("选手已归属其他赛事工种");
  }

  @Test
  void contestantWithoutStageAssignmentRemainsCompatible() {
    when(profileMapper.selectOne(any())).thenReturn(profile(null, EVENT_WORK_TYPE_ID));

    assertThatCode(() -> validate(STAGE_A_ID, EVENT_WORK_TYPE_ID)).doesNotThrowAnyException();
  }

  @Test
  void contestantAssignedToRequestedStageRemainsCompatible() {
    when(profileMapper.selectOne(any())).thenReturn(profile(STAGE_A_ID, EVENT_WORK_TYPE_ID));

    assertThatCode(() -> validate(STAGE_A_ID, EVENT_WORK_TYPE_ID)).doesNotThrowAnyException();
  }

  private void validate(Long stageId, Long eventWorkTypeId) {
    validator.validate(TENANT_ID, EVENT_ID, stageId, eventWorkTypeId, CONTESTANT_MEMBER_ID);
  }

  private CompetitionEventMemberEntity member() {
    CompetitionEventMemberEntity member = new CompetitionEventMemberEntity();
    member.setId(CONTESTANT_MEMBER_ID);
    member.setTenantId(TENANT_ID);
    member.setEventId(EVENT_ID);
    member.setRoleType("contestant");
    member.setStatus("active");
    member.setDeleted(0);
    return member;
  }

  private CompetitionContestantProfileEntity profile(Long stageId, Long eventWorkTypeId) {
    CompetitionContestantProfileEntity profile = new CompetitionContestantProfileEntity();
    profile.setId(500L);
    profile.setTenantId(TENANT_ID);
    profile.setMemberId(CONTESTANT_MEMBER_ID);
    profile.setEventWorkTypeId(eventWorkTypeId);
    profile.setCurrentStageId(stageId);
    profile.setWorkTypeMatchStatus("matched");
    profile.setDeleted(0);
    return profile;
  }
}
