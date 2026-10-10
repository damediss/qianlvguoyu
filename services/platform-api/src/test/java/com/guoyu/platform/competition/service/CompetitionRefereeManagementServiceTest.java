package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.competition.dto.CompetitionRefereeGroupUpsertReq;
import com.guoyu.platform.competition.entity.CompetitionRefereeGroupEntity;
import com.guoyu.platform.competition.entity.CompetitionRefereeGroupMemberEntity;
import com.guoyu.platform.competition.mapper.CompetitionEventMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventMemberMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalAssignmentMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalSessionMapper;
import com.guoyu.platform.competition.mapper.CompetitionRefereeAssignmentMapper;
import com.guoyu.platform.competition.mapper.CompetitionRefereeGroupMapper;
import com.guoyu.platform.competition.mapper.CompetitionRefereeGroupMemberMapper;
import com.guoyu.platform.competition.mapper.CompetitionRefereeProfileMapper;
import com.guoyu.platform.competition.mapper.CompetitionRefereeQualificationMapper;
import com.guoyu.platform.competition.mapper.CompetitionRefereeQueryRow;
import com.guoyu.platform.competition.mapper.CompetitionScoreSourceMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageMapper;
import com.guoyu.platform.competition.mapper.CompetitionStagePaperAssignmentMapper;
import com.guoyu.platform.competition.player.domain.CompetitionPlayerIdentityDomainService;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CompetitionRefereeManagementServiceTest {

  private static final Long TENANT_ID = 10159L;
  private static final Long USER_ID = 1000L;

  private final CompetitionEventWorkTypeDomainService eventWorkTypeService =
      Mockito.mock(CompetitionEventWorkTypeDomainService.class);
  private final CompetitionEventWorkTypeLevelService eventWorkTypeLevelService =
      Mockito.mock(CompetitionEventWorkTypeLevelService.class);
  private final CompetitionStageApplicationService stageApplicationService =
      Mockito.mock(CompetitionStageApplicationService.class);
  private final CompetitionRefereeProfileMapper profileMapper =
      Mockito.mock(CompetitionRefereeProfileMapper.class);
  private final CompetitionRefereeQualificationMapper qualificationMapper =
      Mockito.mock(CompetitionRefereeQualificationMapper.class);
  private final CompetitionEventMemberMapper memberMapper =
      Mockito.mock(CompetitionEventMemberMapper.class);
  private final CompetitionRefereeAssignmentMapper assignmentMapper =
      Mockito.mock(CompetitionRefereeAssignmentMapper.class);
  private final CompetitionRefereeGroupMapper groupMapper =
      Mockito.mock(CompetitionRefereeGroupMapper.class);
  private final CompetitionRefereeGroupMemberMapper groupMemberMapper =
      Mockito.mock(CompetitionRefereeGroupMemberMapper.class);
  private final CompetitionStageMapper stageMapper =
      Mockito.mock(CompetitionStageMapper.class);
  private final CompetitionStagePaperAssignmentMapper stagePaperAssignmentMapper =
      Mockito.mock(CompetitionStagePaperAssignmentMapper.class);
  private final CompetitionEventMapper eventMapper =
      Mockito.mock(CompetitionEventMapper.class);
  private final CompetitionPracticalAssignmentMapper practicalAssignmentMapper =
      Mockito.mock(CompetitionPracticalAssignmentMapper.class);
  private final CompetitionPracticalSessionMapper practicalSessionMapper =
      Mockito.mock(CompetitionPracticalSessionMapper.class);
  private final CompetitionScoreSourceMapper scoreSourceMapper =
      Mockito.mock(CompetitionScoreSourceMapper.class);
  private final CompetitionPlayerIdentityDomainService identityService =
      Mockito.mock(CompetitionPlayerIdentityDomainService.class);

  private final CompetitionRefereeManagementService service = new CompetitionRefereeManagementService(
      eventWorkTypeService, eventWorkTypeLevelService, stageApplicationService, profileMapper,
      qualificationMapper, memberMapper, assignmentMapper, groupMapper, groupMemberMapper,
      stageMapper, stagePaperAssignmentMapper, eventMapper, practicalAssignmentMapper,
      practicalSessionMapper, scoreSourceMapper, identityService);

  @BeforeEach
  void authenticate() {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        USER_ID, TENANT_ID, "admin", "管理员", "platform",
        List.of("PLATFORM_ADMIN"), List.of("competition:referee-group:manage"), null);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
    when(groupMapper.selectList(any())).thenReturn(List.of());
    when(groupMapper.insert(any(CompetitionRefereeGroupEntity.class))).thenReturn(1);
    when(groupMemberMapper.selectList(any())).thenReturn(List.of());
    when(groupMemberMapper.insert(any(CompetitionRefereeGroupMemberEntity.class))).thenReturn(1);
    when(assignmentMapper.selectList(any())).thenReturn(List.of());
    when(profileMapper.selectRows(TENANT_ID, null, null, null, null, null)).thenReturn(List.of(
        referee(1L, "张三"), referee(2L, "李四"), referee(3L, "王五")));
  }

  @AfterEach
  void clearAuthentication() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void createGroupRejectsRefereeAlreadyInAnotherGroup() {
    CompetitionRefereeGroupMemberEntity occupied = new CompetitionRefereeGroupMemberEntity();
    occupied.setGroupId(88L);
    occupied.setRefereeProfileId(1L);
    occupied.setStatus("active");
    occupied.setDeleted(0);
    when(groupMemberMapper.selectList(any())).thenReturn(List.of(occupied));
    CompetitionRefereeGroupEntity other = new CompetitionRefereeGroupEntity();
    other.setId(88L);
    other.setTenantId(TENANT_ID);
    other.setGroupName("第一裁判小组");
    other.setStatus("confirmed");
    other.setDeleted(0);
    when(groupMapper.selectById(88L)).thenReturn(other);

    assertThatThrownBy(() -> service.createGroup(new CompetitionRefereeGroupUpsertReq(
        "第二裁判小组", List.of(1L, 2L, 3L), 1L)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("一名裁判只能进入一个裁判小组");
  }

  @Test
  void createGroupSucceedsWhenRefereesAreFree() {
    var result = service.createGroup(new CompetitionRefereeGroupUpsertReq(
        "第一裁判小组", List.of(1L, 2L, 3L), 1L));

    assertThat(result.getGroupName()).isEqualTo("第一裁判小组");
    assertThat(result.getStatus()).isEqualTo("draft");
    assertThat(result.getBoundTasks()).isEmpty();
    assertThat(result.getReleasable()).isFalse();
  }

  @Test
  void releaseGroupRequiresCompletedStageTasks() {
    CompetitionRefereeGroupEntity group = new CompetitionRefereeGroupEntity();
    group.setId(10L);
    group.setTenantId(TENANT_ID);
    group.setGroupName("第一裁判小组");
    group.setStatus("confirmed");
    group.setDeleted(0);
    when(groupMapper.selectById(10L)).thenReturn(group);

    assertThatThrownBy(() -> service.releaseGroup(10L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("尚未绑定赛段任务");
  }

  private CompetitionRefereeQueryRow referee(Long profileId, String name) {
    CompetitionRefereeQueryRow row = new CompetitionRefereeQueryRow();
    row.setProfileId(profileId);
    row.setName(name);
    row.setStatus("active");
    return row;
  }
}
