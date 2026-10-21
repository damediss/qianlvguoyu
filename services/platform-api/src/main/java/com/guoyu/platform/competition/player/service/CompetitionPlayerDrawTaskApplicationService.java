package com.guoyu.platform.competition.player.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.draw.entity.CompetitionPlayerDrawAssignmentEntity;
import com.guoyu.platform.competition.draw.entity.CompetitionPlayerDrawTaskEntity;
import com.guoyu.platform.competition.draw.mapper.CompetitionPlayerDrawAssignmentMapper;
import com.guoyu.platform.competition.draw.mapper.CompetitionPlayerDrawTaskMapper;
import com.guoyu.platform.competition.draw.service.CompetitionPlayerSelfDraw;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.mapper.CompetitionEventMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeLevelMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageMapper;
import com.guoyu.platform.competition.player.security.CompetitionPlayerPrincipal;
import com.guoyu.platform.competition.player.security.CompetitionPlayerSecurityContext;
import com.guoyu.platform.competition.player.vo.CompetitionPlayerDrawTaskItemVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.security.SecureRandom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionPlayerDrawTaskApplicationService {

  private static final String DISTRIBUTED = "distributed";
  private static final String DRAWING = "drawing";
  private static final String DRAWN = "drawn";
  private static final String PENDING = "pending";
  private static final SecureRandom RANDOM = new SecureRandom();

  private final CompetitionPlayerDrawAssignmentMapper assignmentMapper;
  private final CompetitionPlayerDrawTaskMapper taskMapper;
  private final CompetitionEventMapper eventMapper;
  private final CompetitionStageMapper stageMapper;
  private final CompetitionEventWorkTypeMapper workTypeMapper;
  private final CompetitionEventWorkTypeLevelMapper levelMapper;

  public CompetitionPlayerDrawTaskApplicationService(
      CompetitionPlayerDrawAssignmentMapper assignmentMapper,
      CompetitionPlayerDrawTaskMapper taskMapper,
      CompetitionEventMapper eventMapper,
      CompetitionStageMapper stageMapper,
      CompetitionEventWorkTypeMapper workTypeMapper,
      CompetitionEventWorkTypeLevelMapper levelMapper) {
    this.assignmentMapper = assignmentMapper;
    this.taskMapper = taskMapper;
    this.eventMapper = eventMapper;
    this.stageMapper = stageMapper;
    this.workTypeMapper = workTypeMapper;
    this.levelMapper = levelMapper;
  }

  @Transactional(readOnly = true)
  public List<CompetitionPlayerDrawTaskItemVO> listMine() {
    CompetitionPlayerPrincipal player = currentPlayer();
    List<CompetitionPlayerDrawAssignmentEntity> assignments = assignmentMapper.selectList(
        new LambdaQueryWrapper<CompetitionPlayerDrawAssignmentEntity>()
            .eq(CompetitionPlayerDrawAssignmentEntity::getPlayerId, player.getPlayerId())
            .orderByDesc(CompetitionPlayerDrawAssignmentEntity::getCreatedAt)
            .orderByDesc(CompetitionPlayerDrawAssignmentEntity::getId));
    if (assignments == null || assignments.isEmpty()) {
      return List.of();
    }
    Metadata metadata = metadata(assignments);
    List<CompetitionPlayerDrawTaskItemVO> result = new ArrayList<>();
    for (CompetitionPlayerDrawAssignmentEntity assignment : assignments) {
      CompetitionPlayerDrawTaskEntity task = metadata.tasks().get(assignment.getTaskId());
      if (task != null) {
        result.add(toVO(assignment, task, metadata));
      }
    }
    return List.copyOf(result);
  }

  @Transactional
  public CompetitionPlayerDrawTaskItemVO draw(Long taskId) {
    CompetitionPlayerPrincipal player = currentPlayer();
    CompetitionPlayerDrawTaskEntity task = taskMapper.selectOne(
        new LambdaQueryWrapper<CompetitionPlayerDrawTaskEntity>()
            .eq(CompetitionPlayerDrawTaskEntity::getId, taskId)
            .last("FOR UPDATE"));
    if (task == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "抽签任务不存在");
    }
    CompetitionPlayerDrawAssignmentEntity mine =
        assignmentMapper.selectForUpdateByTaskAndPlayer(taskId, player.getPlayerId());
    if (mine == null) {
      log.warn("Competition player attempted inaccessible draw task, playerId={}, taskId={}",
          player.getPlayerId(), taskId);
      throw new BusinessException(ErrorCode.NOT_FOUND, "抽签任务不存在或未分发给当前选手");
    }
    if (mine.getAppearanceOrder() != null) {
      return toVO(mine, task, metadata(List.of(mine)));
    }
    if (!DISTRIBUTED.equals(task.getStatus()) && !DRAWING.equals(task.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前抽签任务不可执行");
    }

    List<CompetitionPlayerDrawAssignmentEntity> assignments = assignmentMapper.selectByTaskId(taskId);
    if (assignments == null || assignments.size() != task.getPlayerCount()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "抽签任务选手数据不完整");
    }
    Set<Integer> used = new HashSet<>();
    for (CompetitionPlayerDrawAssignmentEntity assignment : assignments) {
      if (assignment.getAppearanceOrder() != null && !used.add(assignment.getAppearanceOrder())) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "抽签号码数据异常");
      }
    }
    final int picked;
    try {
      picked = CompetitionPlayerSelfDraw.pick(assignments.size(), used, RANDOM);
    } catch (IllegalStateException ex) {
      throw new BusinessException(ErrorCode.CONFLICT, "本任务已完成全部抽签");
    }

    LocalDateTime now = LocalDateTime.now();
    mine.setAppearanceOrder(picked);
    mine.setDrawStatus(DRAWN);
    mine.setDrawnAt(now);
    mine.setUpdatedBy(player.getPlayerId());
    mine.setUpdatedAt(now);
    if (assignmentMapper.updateById(mine) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "抽签结果保存失败");
    }

    int drawnCount = used.size() + 1;
    task.setDrawnCount(drawnCount);
    task.setStatus(drawnCount == assignments.size() ? DRAWN : DRAWING);
    task.setUpdatedBy(player.getPlayerId());
    task.setUpdatedAt(now);
    if (taskMapper.updateById(task) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "抽签进度保存失败");
    }
    log.info("Competition player completed draw, playerId={}, taskId={}, appearanceOrder={}",
        player.getPlayerId(), taskId, picked);
    return toVO(mine, task, metadata(List.of(mine)));
  }

  private Metadata metadata(List<CompetitionPlayerDrawAssignmentEntity> assignments) {
    List<Long> taskIds = assignments.stream().map(CompetitionPlayerDrawAssignmentEntity::getTaskId)
        .filter(java.util.Objects::nonNull).distinct().toList();
    Map<Long, CompetitionPlayerDrawTaskEntity> tasks = index(
        taskIds.isEmpty() ? List.of() : taskMapper.selectBatchIds(taskIds));
    List<Long> eventIds = tasks.values().stream().map(CompetitionPlayerDrawTaskEntity::getEventId)
        .filter(java.util.Objects::nonNull).distinct().toList();
    List<Long> stageIds = tasks.values().stream().map(CompetitionPlayerDrawTaskEntity::getStageId)
        .filter(java.util.Objects::nonNull).distinct().toList();
    Map<Long, CompetitionEventEntity> events = index(
        eventIds.isEmpty() ? List.of() : eventMapper.selectBatchIds(eventIds));
    Map<Long, CompetitionStageEntity> stages = index(
        stageIds.isEmpty() ? List.of() : stageMapper.selectBatchIds(stageIds));
    List<Long> workTypeIds = stages.values().stream().map(CompetitionStageEntity::getEventWorkTypeId)
        .filter(java.util.Objects::nonNull).distinct().toList();
    Map<Long, CompetitionEventWorkTypeEntity> workTypes = index(
        workTypeIds.isEmpty() ? List.of() : workTypeMapper.selectBatchIds(workTypeIds));
    List<CompetitionEventWorkTypeLevelEntity> levels = eventIds.isEmpty()
        ? List.of()
        : levelMapper.selectList(new LambdaQueryWrapper<CompetitionEventWorkTypeLevelEntity>()
            .in(CompetitionEventWorkTypeLevelEntity::getEventId, eventIds));
    Map<String, CompetitionEventWorkTypeLevelEntity> levelByScope = new HashMap<>();
    if (levels != null) {
      for (CompetitionEventWorkTypeLevelEntity level : levels) {
        levelByScope.put(levelKey(level.getEventId(), level.getEventWorkTypeId(), level.getLevelCode()), level);
      }
    }
    return new Metadata(tasks, events, stages, workTypes, levelByScope);
  }

  private CompetitionPlayerDrawTaskItemVO toVO(
      CompetitionPlayerDrawAssignmentEntity assignment,
      CompetitionPlayerDrawTaskEntity task,
      Metadata metadata) {
    CompetitionEventEntity event = metadata.events().get(task.getEventId());
    CompetitionStageEntity stage = metadata.stages().get(task.getStageId());
    CompetitionEventWorkTypeEntity workType = stage == null
        ? null : metadata.workTypes().get(stage.getEventWorkTypeId());
    CompetitionEventWorkTypeLevelEntity level = stage == null ? null : metadata.levels().get(
        levelKey(task.getEventId(), stage.getEventWorkTypeId(), stage.getEventWorkTypeLevelCode()));
    return new CompetitionPlayerDrawTaskItemVO(
        String.valueOf(task.getId()), String.valueOf(task.getEventId()),
        event == null ? null : event.getName(), String.valueOf(task.getStageId()), task.getStageName(),
        workType == null ? null : workType.getDisplayName(),
        level == null ? stage == null ? null : stage.getEventWorkTypeLevelCode() : level.getLevelName(),
        assignment.getAppearanceOrder() == null ? PENDING : DRAWN,
        assignment.getAppearanceOrder(), assignment.getDrawnAt(), task.getCreatedAt());
  }

  private String levelKey(Long eventId, Long workTypeId, String levelCode) {
    return String.valueOf(eventId) + ':' + workTypeId + ':' + levelCode;
  }

  private <T extends com.guoyu.platform.common.domain.BaseEntity> Map<Long, T> index(List<T> rows) {
    Map<Long, T> result = new HashMap<>();
    if (rows != null) {
      for (T row : rows) {
        if (row != null && row.getId() != null) result.put(row.getId(), row);
      }
    }
    return result;
  }

  private CompetitionPlayerPrincipal currentPlayer() {
    try {
      return CompetitionPlayerSecurityContext.currentPlayer();
    } catch (IllegalStateException ex) {
      log.warn("Competition player draw request has no authenticated player context");
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "选手登录已失效");
    }
  }

  private record Metadata(
      Map<Long, CompetitionPlayerDrawTaskEntity> tasks,
      Map<Long, CompetitionEventEntity> events,
      Map<Long, CompetitionStageEntity> stages,
      Map<Long, CompetitionEventWorkTypeEntity> workTypes,
      Map<String, CompetitionEventWorkTypeLevelEntity> levels) {
  }
}
