package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.competition.domain.CompetitionTheoryScheduleAllocator;
import com.guoyu.platform.competition.dto.CompetitionTheoryRoomUpsertReq;
import com.guoyu.platform.competition.dto.CompetitionTheoryScheduleCreateReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionScoreRuleEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryExamRoomEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryScheduleVersionEntity;
import com.guoyu.platform.competition.mapper.CompetitionTheoryAssignmentAdjustmentMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryCandidateAssignmentMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryExamRoomMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryExamSessionMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryPaperSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryScheduleVersionMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheorySessionRoomMapper;
import com.guoyu.platform.competition.messaging.mapper.CompetitionPlayerMessageMapper;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CompetitionTheoryScheduleApplicationServiceTest {

  private final CompetitionScoreRuleApplicationService scoreRuleApplicationService =
      Mockito.mock(CompetitionScoreRuleApplicationService.class);
  private final CompetitionScoreRuleDomainService scoreRuleDomainService =
      Mockito.mock(CompetitionScoreRuleDomainService.class);
  private final CompetitionTheoryScheduleVersionMapper versionMapper =
      Mockito.mock(CompetitionTheoryScheduleVersionMapper.class);
  private final CompetitionTheoryExamRoomMapper roomMapper =
      Mockito.mock(CompetitionTheoryExamRoomMapper.class);
  private final CompetitionTheoryExamSessionMapper sessionMapper =
      Mockito.mock(CompetitionTheoryExamSessionMapper.class);
  private final CompetitionTheorySessionRoomMapper sessionRoomMapper =
      Mockito.mock(CompetitionTheorySessionRoomMapper.class);
  private final CompetitionTheoryCandidateAssignmentMapper assignmentMapper =
      Mockito.mock(CompetitionTheoryCandidateAssignmentMapper.class);
  private final CompetitionTheoryAssignmentAdjustmentMapper adjustmentMapper =
      Mockito.mock(CompetitionTheoryAssignmentAdjustmentMapper.class);
  private final CompetitionTheoryPaperSnapshotMapper paperSnapshotMapper =
      Mockito.mock(CompetitionTheoryPaperSnapshotMapper.class);
  private final CompetitionPlayerMessageMapper playerMessageMapper =
      Mockito.mock(CompetitionPlayerMessageMapper.class);
  private final CompetitionTheoryScheduleApplicationService service =
      new CompetitionTheoryScheduleApplicationService(
          scoreRuleApplicationService, scoreRuleDomainService, versionMapper, roomMapper,
          sessionMapper, sessionRoomMapper, assignmentMapper, adjustmentMapper,
          paperSnapshotMapper, new CompetitionTheoryScheduleAllocator(), playerMessageMapper);

  @BeforeEach
  void setUp() {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        9L, 1L, "admin", "admin", "business", List.of("PLATFORM_ADMIN"),
        List.of("competition:theory-schedule:view", "competition:theory-schedule:manage",
            "competition:theory-schedule:publish"),
        "admin@example.com");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
    when(scoreRuleApplicationService.manageableEvent(any(), eq(1001L)))
        .thenReturn(event());
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void refusesScheduleVersionForMismatchedPublishedRuleScope() {
    CompetitionScoreRuleEntity rule = publishedRule();
    rule.setEventWorkTypeId(9999L);
    when(scoreRuleDomainService.getById(1L, 4001L)).thenReturn(rule);

    assertThatThrownBy(() -> service.createVersion(
        1001L, new CompetitionTheoryScheduleCreateReq(2001L, 3001L, 4001L)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("已发布评分规则");
    verify(versionMapper, never()).insert(any(CompetitionTheoryScheduleVersionEntity.class));
  }

  @Test
  void publishedScheduleLocksAllManageOperations() {
    CompetitionTheoryScheduleVersionEntity version = version("published");
    when(versionMapper.selectForUpdate(1L, 5001L)).thenReturn(version);

    assertThatThrownBy(() -> service.randomAllocate(5001L))
        .isInstanceOf(BusinessException.class)
        .hasMessage("已发布或已撤销安排不可修改");
    verify(assignmentMapper, never()).insert(
        any(com.guoyu.platform.competition.entity.CompetitionTheoryCandidateAssignmentEntity.class));
  }

  @Test
  void roomReferencedByPublishedArrangementCannotBeChanged() {
    CompetitionTheoryExamRoomEntity room = new CompetitionTheoryExamRoomEntity();
    room.setId(6001L);
    room.setTenantId(1L);
    room.setEventId(1001L);
    room.setName("第一考场");
    room.setAddress("旧地址");
    room.setCapacity(20);
    room.setStatus("enabled");
    when(roomMapper.selectOne(any())).thenReturn(room);
    when(roomMapper.countPublishedReferences(1L, 6001L)).thenReturn(1L);

    assertThatThrownBy(() -> service.updateRoom(6001L,
        new CompetitionTheoryRoomUpsertReq("第一考场", "新地址", 30, "enabled", null)))
        .isInstanceOf(BusinessException.class)
        .hasMessage("已发布安排引用的考场不可修改");
    verify(roomMapper, never()).updateById(any(CompetitionTheoryExamRoomEntity.class));
  }

  private CompetitionScoreRuleEntity publishedRule() {
    CompetitionScoreRuleEntity rule = new CompetitionScoreRuleEntity();
    rule.setId(4001L);
    rule.setTenantId(1L);
    rule.setEventId(1001L);
    rule.setStageId(2001L);
    rule.setEventWorkTypeId(3001L);
    rule.setStatus("published");
    return rule;
  }

  private CompetitionTheoryScheduleVersionEntity version(String status) {
    CompetitionTheoryScheduleVersionEntity version =
        new CompetitionTheoryScheduleVersionEntity();
    version.setId(5001L);
    version.setTenantId(1L);
    version.setEventId(1001L);
    version.setStageId(2001L);
    version.setEventWorkTypeId(3001L);
    version.setScoreRuleId(4001L);
    version.setStatus(status);
    return version;
  }

  private CompetitionEventEntity event() {
    CompetitionEventEntity event = new CompetitionEventEntity();
    event.setId(1001L);
    event.setTenantId(1L);
    event.setStatus("published");
    return event;
  }
}
