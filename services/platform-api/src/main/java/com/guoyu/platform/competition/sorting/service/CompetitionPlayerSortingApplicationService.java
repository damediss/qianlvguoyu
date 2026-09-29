package com.guoyu.platform.competition.sorting.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.service.CompetitionStageApplicationService;
import com.guoyu.platform.competition.service.CompetitionStagePaperAssignmentApplicationService;
import com.guoyu.platform.competition.sorting.dto.CompetitionPlayerSortingTaskCreateReq;
import com.guoyu.platform.competition.sorting.dto.CompetitionPlayerSortingTaskSelectionReq;
import com.guoyu.platform.competition.sorting.entity.CompetitionPlayerSortingTaskEntity;
import com.guoyu.platform.competition.sorting.mapper.CompetitionPlayerSortingTaskMapper;
import com.guoyu.platform.competition.sorting.vo.CompetitionPlayerSortingAssignmentVO;
import com.guoyu.platform.competition.sorting.vo.CompetitionPlayerSortingTaskVO;
import com.guoyu.platform.competition.vo.CompetitionStagePaperAssignmentPlayerVO;
import com.guoyu.platform.competition.vo.CompetitionStageVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionPlayerSortingApplicationService {

  private static final String PRACTICAL = "practical";
  private static final String CREATED = "created";
  private static final String SORTED = "sorted";
  private static final int MAX_SELECTED_PLAYER_COUNT = 5000;
  private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();
  private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};
  private static final TypeReference<List<CompetitionPlayerSortingAssignmentVO>> ASSIGNMENTS =
      new TypeReference<>() {};

  private final CompetitionStageApplicationService stageService;
  private final CompetitionStagePaperAssignmentApplicationService stagePlayerService;
  private final CompetitionPlayerSortingTaskMapper taskMapper;
  private final CompetitionPlayerSortingWorkbookExportService workbookExportService;
  private final ObjectMapper objectMapper;

  public CompetitionPlayerSortingApplicationService(
      CompetitionStageApplicationService stageService,
      CompetitionStagePaperAssignmentApplicationService stagePlayerService,
      CompetitionPlayerSortingTaskMapper taskMapper,
      CompetitionPlayerSortingWorkbookExportService workbookExportService,
      ObjectMapper objectMapper) {
    this.stageService = stageService;
    this.stagePlayerService = stagePlayerService;
    this.taskMapper = taskMapper;
    this.workbookExportService = workbookExportService;
    this.objectMapper = objectMapper;
  }

  @Transactional(readOnly = true)
  public List<CompetitionStagePaperAssignmentPlayerVO> candidates(Long stageId) {
    stageService.assertCanManage(stageId);
    List<CompetitionStagePaperAssignmentPlayerVO> candidates =
        stagePlayerService.visualization(stageId, PRACTICAL);
    return candidates == null ? List.of() : candidates;
  }

  @Transactional(readOnly = true)
  public PageResponse<CompetitionPlayerSortingTaskVO> page(
      Long stageId, long page, long pageSize) {
    stageService.assertCanManage(stageId);
    Page<CompetitionPlayerSortingTaskEntity> result = taskMapper.selectPage(
        new Page<>(page, pageSize),
        new LambdaQueryWrapper<CompetitionPlayerSortingTaskEntity>()
            .eq(CompetitionPlayerSortingTaskEntity::getStageId, stageId)
            .orderByDesc(CompetitionPlayerSortingTaskEntity::getCreatedAt));
    if (result == null) {
      log.warn("Player sorting task page returned null, stageId={}", stageId);
      return PageResponse.empty(page, pageSize);
    }
    List<CompetitionPlayerSortingTaskVO> items = result.getRecords() == null
        ? List.of()
        : result.getRecords().stream().map(entity -> toVO(entity, List.of())).toList();
    return new PageResponse<>(items, result.getCurrent(), result.getSize(), result.getTotal());
  }

  @Transactional
  public CompetitionPlayerSortingTaskVO create(
      Long stageId, CompetitionPlayerSortingTaskCreateReq req) {
    CompetitionStageVO stage = stageService.assertCanManage(stageId);
    validateConfiguration(req);
    if (!req.contestantProfileIds().isEmpty()) {
      validateSelectedContestants(stageId, req.contestantProfileIds(),
          req.groupCount(), req.playersPerGroup());
    }
    Set<Long> selectedIds = new HashSet<>(req.contestantProfileIds());

    PlatformUserPrincipal principal = currentUser();
    LocalDateTime now = LocalDateTime.now();
    CompetitionPlayerSortingTaskEntity entity = new CompetitionPlayerSortingTaskEntity();
    entity.setId(IdWorker.getId());
    entity.setTenantId(principal.getTenantId());
    entity.setCreatedBy(principal.getUserId());
    entity.setUpdatedBy(principal.getUserId());
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);
    entity.setDeleted(0);
    entity.setEventId(stage.getEventId());
    entity.setStageId(stageId);
    entity.setStageName(stage.getName());
    entity.setGroupCount(req.groupCount());
    entity.setWorkstationCount(req.workstationCount());
    entity.setPlayersPerGroup(req.playersPerGroup());
    entity.setPlayerCount(selectedIds.size());
    entity.setStatus(CREATED);
    entity.setSelectedProfileIdsJson(writeJson(
        req.contestantProfileIds().stream().map(String::valueOf).toList()));
    int inserted = taskMapper.insert(entity);
    if (inserted != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "排序任务创建失败");
    }
    return toVO(entity, List.of());
  }

  @Transactional
  public void delete(Long taskId) {
    CompetitionPlayerSortingTaskEntity entity = taskMapper.selectOne(
        new LambdaQueryWrapper<CompetitionPlayerSortingTaskEntity>()
            .eq(CompetitionPlayerSortingTaskEntity::getId, taskId)
            .last("FOR UPDATE"));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "排序任务不存在");
    }
    stageService.assertCanManage(entity.getStageId());
    if (!CREATED.equals(entity.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "已完成随机排序的任务不能删除");
    }
    if (taskMapper.deleteById(taskId) != 1) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "排序任务不存在");
    }
  }

  @Transactional
  public CompetitionPlayerSortingTaskVO selectContestants(
      Long taskId, CompetitionPlayerSortingTaskSelectionReq req) {
    CompetitionPlayerSortingTaskEntity entity = requireTask(taskId);
    stageService.assertCanManage(entity.getStageId());
    if (!CREATED.equals(entity.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "已完成随机排序的任务不能修改选手");
    }
    if (req == null || req.contestantProfileIds() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择参加排序的选手");
    }
    validateSelectedContestants(entity.getStageId(), req.contestantProfileIds(),
        entity.getGroupCount(), entity.getPlayersPerGroup());

    PlatformUserPrincipal principal = currentUser();
    entity.setSelectedProfileIdsJson(writeJson(
        req.contestantProfileIds().stream().map(String::valueOf).toList()));
    entity.setPlayerCount(req.contestantProfileIds().size());
    entity.setUpdatedBy(principal.getUserId());
    entity.setUpdatedAt(LocalDateTime.now());
    if (taskMapper.updateById(entity) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "排序任务选手保存失败");
    }
    return toVO(entity, List.of());
  }

  @Transactional
  public CompetitionPlayerSortingTaskVO randomize(Long taskId) {
    CompetitionPlayerSortingTaskEntity entity = requireTask(taskId);
    CompetitionStageVO stage = stageService.assertCanManage(entity.getStageId());
    if (!stage.getEventId().equals(entity.getEventId())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "排序任务与赛段不匹配");
    }
    if (!CREATED.equals(entity.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "该排序任务已完成抽签，请新建任务重新抽签");
    }

    List<Long> selectedIds = readSelectedIds(entity.getSelectedProfileIdsJson());
    int maximumPlayerCount = Math.min(
        Math.multiplyExact(entity.getGroupCount(), entity.getPlayersPerGroup()),
        MAX_SELECTED_PLAYER_COUNT);
    if (selectedIds.size() != entity.getPlayerCount()
        || selectedIds.size() < entity.getGroupCount()
        || selectedIds.size() > maximumPlayerCount) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "排序任务选手人数不符合配置，请重新选择选手");
    }
    Map<Long, CompetitionStagePaperAssignmentPlayerVO> candidateById =
        candidateMap(entity.getStageId());
    List<CompetitionStagePaperAssignmentPlayerVO> selected = new ArrayList<>();
    for (Long selectedId : selectedIds) {
      CompetitionStagePaperAssignmentPlayerVO candidate = candidateById.get(selectedId);
      if (candidate == null) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
            "有选手已不属于当前赛段，请重新创建排序任务");
      }
      selected.add(candidate);
    }

    Map<Integer, String> workstationByGroup = assignWorkstations(
        entity.getGroupCount(), entity.getWorkstationCount());
    Collections.shuffle(selected, RANDOM);
    List<CompetitionPlayerSortingAssignmentVO> assignments = new ArrayList<>(selected.size());
    int baseGroupSize = selected.size() / entity.getGroupCount();
    int largerGroupCount = selected.size() % entity.getGroupCount();
    int selectedIndex = 0;
    for (int groupNo = 1; groupNo <= entity.getGroupCount(); groupNo++) {
      int groupSize = baseGroupSize + (groupNo <= largerGroupCount ? 1 : 0);
      for (int groupOrder = 1; groupOrder <= groupSize; groupOrder++) {
        CompetitionStagePaperAssignmentPlayerVO candidate = selected.get(selectedIndex++);
        assignments.add(new CompetitionPlayerSortingAssignmentVO(
            candidate.contestantProfileId(), candidate.contestantNo(), candidate.name(),
            groupNo, groupOrder, workstationByGroup.get(groupNo)));
      }
    }

    PlatformUserPrincipal principal = currentUser();
    entity.setResultSnapshotJson(writeJson(assignments));
    entity.setStatus(SORTED);
    entity.setUpdatedBy(principal.getUserId());
    entity.setUpdatedAt(LocalDateTime.now());
    if (taskMapper.updateById(entity) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "随机排序结果保存失败");
    }
    return toVO(entity, assignments);
  }

  @Transactional(readOnly = true)
  public CompetitionPlayerSortingTaskVO get(Long taskId) {
    CompetitionPlayerSortingTaskEntity entity = requireTask(taskId);
    stageService.assertCanManage(entity.getStageId());
    return toVO(entity, readAssignments(entity.getResultSnapshotJson()));
  }

  @Transactional(readOnly = true)
  public byte[] exportExcel(Long taskId) {
    CompetitionPlayerSortingTaskEntity entity = requireTask(taskId);
    stageService.assertCanManage(entity.getStageId());
    if (!SORTED.equals(entity.getStatus())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "请先完成随机排序再导出名单");
    }
    List<CompetitionPlayerSortingAssignmentVO> assignments =
        readAssignments(entity.getResultSnapshotJson());
    if (assignments.size() != entity.getPlayerCount()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "排序结果不完整，无法导出名单");
    }
    return workbookExportService.export(entity.getStageName(), assignments, entity.getGroupCount());
  }

  private void validateConfiguration(CompetitionPlayerSortingTaskCreateReq req) {
    if (req == null || req.groupCount() == null || req.workstationCount() == null
        || req.playersPerGroup() == null || req.contestantProfileIds() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "排序任务配置不完整");
    }
    if (req.groupCount() < 1 || req.workstationCount() < 1 || req.playersPerGroup() < 1) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "组数、工位数和每组人数上限必须大于零");
    }
    if (req.workstationCount() < req.groupCount()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "工位数量不能少于组数，每组至少需要一个工位");
    }
  }

  private Map<Long, CompetitionStagePaperAssignmentPlayerVO> candidateMap(Long stageId) {
    List<CompetitionStagePaperAssignmentPlayerVO> candidates = candidates(stageId);
    Map<Long, CompetitionStagePaperAssignmentPlayerVO> result = new HashMap<>();
    for (CompetitionStagePaperAssignmentPlayerVO candidate : candidates) {
      if (candidate == null || candidate.contestantProfileId() == null) {
        continue;
      }
      try {
        result.put(Long.valueOf(candidate.contestantProfileId()), candidate);
      } catch (NumberFormatException ex) {
        log.warn("Ignoring stage candidate with an invalid profile ID, stageId={}", stageId);
      }
    }
    return result;
  }

  private void validateSelectedContestants(Long stageId, List<Long> contestantProfileIds,
      int groupCount, int playersPerGroup) {
    int minimumCount = groupCount;
    int maximumCount = Math.min(
        Math.multiplyExact(groupCount, playersPerGroup), MAX_SELECTED_PLAYER_COUNT);
    if (contestantProfileIds.size() < minimumCount || contestantProfileIds.size() > maximumCount) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "已选人数需在 " + minimumCount + " 至 " + maximumCount + " 人之间（每组至少 1 人，"
              + "每组最多 " + playersPerGroup + " 人），当前已选 " + contestantProfileIds.size() + " 人");
    }
    Set<Long> selectedIds = new HashSet<>(contestantProfileIds);
    if (selectedIds.size() != contestantProfileIds.size()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "选手列表中存在重复选手");
    }
    Map<Long, CompetitionStagePaperAssignmentPlayerVO> candidateById = candidateMap(stageId);
    if (!candidateById.keySet().containsAll(selectedIds)) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "所选选手中包含不属于当前赛段的选手，请刷新名单后重试");
    }
  }

  private Map<Integer, String> assignWorkstations(int groupCount, int workstationCount) {
    Map<Integer, String> result = new HashMap<>();
    int perGroup = workstationCount / groupCount;
    int remainder = workstationCount % groupCount;
    int nextStation = 1;
    for (int groupNo = 1; groupNo <= groupCount; groupNo++) {
      int assignedCount = perGroup + (groupNo <= remainder ? 1 : 0);
      List<String> stations = IntStream.range(nextStation, nextStation + assignedCount)
          .mapToObj(number -> number + "号工位")
          .toList();
      result.put(groupNo, String.join("、", stations));
      nextStation += assignedCount;
    }
    return result;
  }

  private CompetitionPlayerSortingTaskEntity requireTask(Long taskId) {
    CompetitionPlayerSortingTaskEntity entity = taskMapper.selectById(taskId);
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "排序任务不存在");
    }
    return entity;
  }

  private List<Long> readSelectedIds(String json) {
    try {
      return objectMapper.readValue(json, STRING_LIST).stream().map(Long::valueOf).toList();
    } catch (Exception ex) {
      log.error("Failed to read player sorting task selected IDs");
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "排序任务选手数据损坏");
    }
  }

  private List<CompetitionPlayerSortingAssignmentVO> readAssignments(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      List<CompetitionPlayerSortingAssignmentVO> result =
          objectMapper.readValue(json, ASSIGNMENTS);
      return result == null ? List.of() : result;
    } catch (JsonProcessingException ex) {
      log.error("Failed to read player sorting task result snapshot");
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "排序结果数据损坏");
    }
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      log.error("Failed to serialize player sorting task data");
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "排序任务数据保存失败");
    }
  }

  private CompetitionPlayerSortingTaskVO toVO(
      CompetitionPlayerSortingTaskEntity entity,
      List<CompetitionPlayerSortingAssignmentVO> assignments) {
    return new CompetitionPlayerSortingTaskVO(
        String.valueOf(entity.getId()), String.valueOf(entity.getEventId()),
        String.valueOf(entity.getStageId()), entity.getStageName(), entity.getGroupCount(),
        entity.getWorkstationCount(), entity.getPlayersPerGroup(), entity.getPlayerCount(),
        entity.getStatus(), entity.getCreatedAt(), assignments);
  }

  private PlatformUserPrincipal currentUser() {
    PlatformUserPrincipal principal = SecurityContext.currentUser();
    if (principal == null || principal.getTenantId() == null || principal.getUserId() == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前用户身份无效");
    }
    return principal;
  }
}
