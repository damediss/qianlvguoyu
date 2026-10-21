package com.guoyu.platform.competition.player.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.guoyu.platform.competition.draw.entity.CompetitionPlayerDrawAssignmentEntity;
import com.guoyu.platform.competition.draw.entity.CompetitionPlayerDrawTaskEntity;
import com.guoyu.platform.competition.draw.mapper.CompetitionPlayerDrawAssignmentMapper;
import com.guoyu.platform.competition.draw.mapper.CompetitionPlayerDrawTaskMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeLevelMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageMapper;
import com.guoyu.platform.competition.player.security.CompetitionPlayerPrincipal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CompetitionPlayerDrawTaskApplicationServiceTest {

  private final CompetitionPlayerDrawAssignmentMapper assignmentMapper =
      Mockito.mock(CompetitionPlayerDrawAssignmentMapper.class);
  private final CompetitionPlayerDrawTaskMapper taskMapper =
      Mockito.mock(CompetitionPlayerDrawTaskMapper.class);
  private final CompetitionEventMapper eventMapper = Mockito.mock(CompetitionEventMapper.class);
  private final CompetitionStageMapper stageMapper = Mockito.mock(CompetitionStageMapper.class);
  private final CompetitionEventWorkTypeMapper workTypeMapper =
      Mockito.mock(CompetitionEventWorkTypeMapper.class);
  private final CompetitionEventWorkTypeLevelMapper levelMapper =
      Mockito.mock(CompetitionEventWorkTypeLevelMapper.class);
  private final CompetitionPlayerDrawTaskApplicationService service =
      new CompetitionPlayerDrawTaskApplicationService(
          assignmentMapper, taskMapper, eventMapper, stageMapper, workTypeMapper, levelMapper);

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void repeatedDrawReturnsOriginalNumberWithoutWritingAgain() {
    login();
    CompetitionPlayerDrawTaskEntity task = task("drawing");
    CompetitionPlayerDrawAssignmentEntity mine = assignment(9001L, 7);
    when(taskMapper.selectOne(ArgumentMatchers.<Wrapper<CompetitionPlayerDrawTaskEntity>>any()))
        .thenReturn(task);
    when(assignmentMapper.selectForUpdateByTaskAndPlayer(3001L, 9001L)).thenReturn(mine);
    when(taskMapper.selectBatchIds(List.of(3001L))).thenReturn(List.of(task));

    var result = service.draw(3001L);

    assertThat(result.appearanceOrder()).isEqualTo(7);
    verify(assignmentMapper, never()).updateById(mine);
    verify(taskMapper, never()).updateById(task);
  }

  @Test
  void drawUsesTheOnlyRemainingNumberAndCompletesTask() {
    login();
    CompetitionPlayerDrawTaskEntity task = task("distributed");
    task.setPlayerCount(2);
    CompetitionPlayerDrawAssignmentEntity mine = assignment(9001L, null);
    CompetitionPlayerDrawAssignmentEntity other = assignment(9002L, 1);
    when(taskMapper.selectOne(ArgumentMatchers.<Wrapper<CompetitionPlayerDrawTaskEntity>>any()))
        .thenReturn(task);
    when(assignmentMapper.selectForUpdateByTaskAndPlayer(3001L, 9001L)).thenReturn(mine);
    when(assignmentMapper.selectByTaskId(3001L)).thenReturn(List.of(mine, other));
    when(assignmentMapper.updateById(mine)).thenReturn(1);
    when(taskMapper.updateById(task)).thenReturn(1);
    when(taskMapper.selectBatchIds(List.of(3001L))).thenReturn(List.of(task));

    var result = service.draw(3001L);

    assertThat(result.appearanceOrder()).isEqualTo(2);
    assertThat(task.getStatus()).isEqualTo("drawn");
    assertThat(task.getDrawnCount()).isEqualTo(2);
  }

  @Test
  void unauthenticatedPlayerCannotReadTasks() {
    assertThatThrownBy(service::listMine).hasMessage("选手登录已失效");
  }

  private void login() {
    CompetitionPlayerPrincipal principal = new CompetitionPlayerPrincipal(9001L, 10159L);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
  }

  private CompetitionPlayerDrawTaskEntity task(String status) {
    CompetitionPlayerDrawTaskEntity task = new CompetitionPlayerDrawTaskEntity();
    task.setId(3001L);
    task.setTenantId(10159L);
    task.setEventId(1001L);
    task.setStageId(2001L);
    task.setStageName("决赛");
    task.setPlayerCount(10);
    task.setDrawnCount(1);
    task.setStatus(status);
    task.setCreatedAt(LocalDateTime.now());
    return task;
  }

  private CompetitionPlayerDrawAssignmentEntity assignment(Long playerId, Integer order) {
    CompetitionPlayerDrawAssignmentEntity assignment = new CompetitionPlayerDrawAssignmentEntity();
    assignment.setId(playerId + 10000);
    assignment.setTenantId(10159L);
    assignment.setTaskId(3001L);
    assignment.setPlayerId(playerId);
    assignment.setContestantProfileId(playerId + 20000);
    assignment.setPlayerName("选手" + playerId);
    assignment.setDrawStatus(order == null ? "pending" : "drawn");
    assignment.setAppearanceOrder(order);
    assignment.setCreatedAt(LocalDateTime.now());
    return assignment;
  }
}
