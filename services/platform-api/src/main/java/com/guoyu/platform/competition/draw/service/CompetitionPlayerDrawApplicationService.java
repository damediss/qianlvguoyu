package com.guoyu.platform.competition.draw.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.util.SensitiveMaskUtils;
import com.guoyu.platform.competition.draw.dto.CompetitionPlayerDrawTaskSelectionReq;
import com.guoyu.platform.competition.draw.entity.CompetitionPlayerDrawTaskEntity;
import com.guoyu.platform.competition.draw.mapper.CompetitionPlayerDrawTaskMapper;
import com.guoyu.platform.competition.draw.service.CompetitionPlayerDrawSession.DrawStep;
import com.guoyu.platform.competition.draw.vo.CompetitionPlayerDrawSlot;
import com.guoyu.platform.competition.draw.vo.CompetitionPlayerDrawTaskVO;
import com.guoyu.platform.competition.entity.CompetitionPlayerIdentityEntity;
import com.guoyu.platform.competition.mapper.CompetitionPlayerIdentityMapper;
import com.guoyu.platform.competition.service.CompetitionStageApplicationService;
import com.guoyu.platform.competition.service.CompetitionStagePaperAssignmentApplicationService;
import com.guoyu.platform.competition.vo.CompetitionStagePaperAssignmentPlayerVO;
import com.guoyu.platform.competition.vo.CompetitionStageVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionPlayerDrawApplicationService {

  private static final String PRACTICAL = "practical";
  private static final String CREATED = "created";
  private static final String ORDERED = "ordered";
  private static final String DRAWING = "drawing";
  private static final String DRAWN = "drawn";
  private static final int MAX_SELECTED_PLAYER_COUNT = 5000;
  private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();
  private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};
  private static final TypeReference<List<CompetitionPlayerDrawSlot>> SLOTS = new TypeReference<>() {};

  private final CompetitionStageApplicationService stageService;
  private final CompetitionStagePaperAssignmentApplicationService stagePlayerService;
  private final CompetitionPlayerDrawTaskMapper taskMapper;
  private final CompetitionPlayerDrawWorkbookExportService workbookExportService;
  private final CompetitionPlayerIdentityMapper identityMapper;
  private final ObjectMapper objectMapper;

  public CompetitionPlayerDrawApplicationService(
      CompetitionStageApplicationService stageService,
      CompetitionStagePaperAssignmentApplicationService stagePlayerService,
      CompetitionPlayerDrawTaskMapper taskMapper,
      CompetitionPlayerDrawWorkbookExportService workbookExportService,
      CompetitionPlayerIdentityMapper identityMapper,
      ObjectMapper objectMapper) {
    this.stageService = stageService;
    this.stagePlayerService = stagePlayerService;
    this.taskMapper = taskMapper;
    this.workbookExportService = workbookExportService;
    this.identityMapper = identityMapper;
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
  public PageResponse<CompetitionPlayerDrawTaskVO> page(Long stageId, long page, long pageSize) {
    stageService.assertCanManage(stageId);
    Page<CompetitionPlayerDrawTaskEntity> result = taskMapper.selectPage(
        new Page<>(page, pageSize),
        new LambdaQueryWrapper<CompetitionPlayerDrawTaskEntity>()
            .eq(CompetitionPlayerDrawTaskEntity::getStageId, stageId)
            .orderByDesc(CompetitionPlayerDrawTaskEntity::getCreatedAt));
    if (result == null) {
      log.warn("Player draw task page returned null, stageId={}", stageId);
      return PageResponse.empty(page, pageSize);
    }
    List<CompetitionPlayerDrawTaskVO> items = result.getRecords() == null
        ? List.of()
        : result.getRecords().stream().map(entity -> toVO(entity, List.of())).toList();
    return new PageResponse<>(items, result.getCurrent(), result.getSize(), result.getTotal());
  }

  @Transactional
  public CompetitionPlayerDrawTaskVO create(Long stageId) {
    CompetitionStageVO stage = stageService.assertCanManage(stageId);
    PlatformUserPrincipal principal = currentUser();
    LocalDateTime now = LocalDateTime.now();
    CompetitionPlayerDrawTaskEntity entity = new CompetitionPlayerDrawTaskEntity();
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
    entity.setPlayerCount(0);
    entity.setDrawnCount(0);
    entity.setStatus(CREATED);
    entity.setSelectedProfileIdsJson(writeJson(List.of()));
    int inserted = taskMapper.insert(entity);
    if (inserted != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "抽签任务创建失败");
    }
    return toVO(entity, List.of());
  }

  @Transactional
  public void delete(Long taskId) {
    CompetitionPlayerDrawTaskEntity entity = lockTask(taskId);
    stageService.assertCanManage(entity.getStageId());
    if (DRAWING.equals(entity.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "抽签进行中的任务不能删除，请完成全部抽签后再删除");
    }
    if (taskMapper.deleteById(taskId) != 1) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "抽签任务不存在");
    }
  }

  @Transactional
  public CompetitionPlayerDrawTaskVO selectContestants(
      Long taskId, CompetitionPlayerDrawTaskSelectionReq req) {
    CompetitionPlayerDrawTaskEntity entity = lockTask(taskId);
    stageService.assertCanManage(entity.getStageId());
    if (!CREATED.equals(entity.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "已生成抽签顺序的任务不能修改选手");
    }
    if (req == null || req.contestantProfileIds() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择参加抽签的选手");
    }
    validateSelectedContestants(entity.getStageId(), req.contestantProfileIds());
    PlatformUserPrincipal principal = currentUser();
    entity.setSelectedProfileIdsJson(writeJson(
        req.contestantProfileIds().stream().map(String::valueOf).toList()));
    entity.setPlayerCount(req.contestantProfileIds().size());
    entity.setDrawnCount(0);
    entity.setUpdatedBy(principal.getUserId());
    entity.setUpdatedAt(LocalDateTime.now());
    if (taskMapper.updateById(entity) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "抽签任务选手保存失败");
    }
    return toVO(entity, List.of());
  }

  @Transactional
  public CompetitionPlayerDrawTaskVO generateDrawOrder(Long taskId) {
    CompetitionPlayerDrawTaskEntity entity = lockTask(taskId);
    CompetitionStageVO stage = stageService.assertCanManage(entity.getStageId());
    assertSameEvent(entity, stage);
    if (!CREATED.equals(entity.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "抽签顺序已生成，请继续逐个抽签或新建任务");
    }
    List<Long> selectedIds = readSelectedIds(entity.getSelectedProfileIdsJson());
    if (selectedIds.isEmpty() || selectedIds.size() != entity.getPlayerCount()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "请先选择参加抽签的选手");
    }
    List<CompetitionPlayerDrawSlot> seeds = new ArrayList<>(selectedIds.size());
    Map<Long, CompetitionStagePaperAssignmentPlayerVO> candidateById =
        candidateMap(entity.getStageId());
    for (Long selectedId : selectedIds) {
      CompetitionStagePaperAssignmentPlayerVO candidate = candidateById.get(selectedId);
      if (candidate == null) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
            "有选手已不属于当前赛段，请重新创建抽签任务");
      }
      seeds.add(new CompetitionPlayerDrawSlot(
          candidate.contestantProfileId(), candidate.contestantNo(), candidate.name(),
          candidate.orgName(), null, 0, null));
    }
    List<CompetitionPlayerDrawSlot> ordered =
        CompetitionPlayerDrawSession.createDrawOrder(seeds, RANDOM);
    return persistSlots(entity, ordered, ORDERED, 0);
  }

  @Transactional
  public CompetitionPlayerDrawTaskVO drawNext(Long taskId) {
    CompetitionPlayerDrawTaskEntity entity = lockTask(taskId);
    CompetitionStageVO stage = stageService.assertCanManage(entity.getStageId());
    assertSameEvent(entity, stage);
    if (DRAWN.equals(entity.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "全部选手已完成出场抽签");
    }
    if (!ORDERED.equals(entity.getStatus()) && !DRAWING.equals(entity.getStatus())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "请先生成抽签顺序，再逐个抽签");
    }
    List<CompetitionPlayerDrawSlot> slots = readSlots(entity.getResultSnapshotJson());
    validateSnapshot(entity, slots);
    assertSlotsStillInStage(entity.getStageId(), slots);
    DrawStep step;
    try {
      step = CompetitionPlayerDrawSession.drawNext(slots, RANDOM);
    } catch (IllegalStateException ex) {
      log.warn("Player draw task has no remaining lot, taskId={}", taskId);
      throw new BusinessException(ErrorCode.CONFLICT, "全部选手已完成出场抽签");
    }
    int drawnCount = (int) step.slots().stream()
        .filter(slot -> slot.appearanceOrder() != null)
        .count();
    String status = step.complete() ? DRAWN : DRAWING;
    return persistSlots(entity, step.slots(), status, drawnCount);
  }

  @Transactional(readOnly = true)
  public CompetitionPlayerDrawTaskVO get(Long taskId) {
    CompetitionPlayerDrawTaskEntity entity = requireTask(taskId);
    stageService.assertCanManage(entity.getStageId());
    return toVO(entity, withMobile(entity.getStageId(), readSlots(entity.getResultSnapshotJson())));
  }

  @Transactional(readOnly = true)
  public byte[] exportExcel(Long taskId) {
    CompetitionPlayerDrawTaskEntity entity = requireTask(taskId);
    stageService.assertCanManage(entity.getStageId());
    if (!DRAWN.equals(entity.getStatus())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "请先完成全部选手的出场抽签再导出名单");
    }
    List<CompetitionPlayerDrawSlot> slots = withMobile(entity.getStageId(),
        readSlots(entity.getResultSnapshotJson()));
    if (slots.size() != entity.getPlayerCount()
        || slots.stream().anyMatch(slot -> slot.appearanceOrder() == null)) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "抽签结果不完整，无法导出名单");
    }
    return workbookExportService.export(entity.getStageName(), slots);
  }

  private CompetitionPlayerDrawTaskVO persistSlots(
      CompetitionPlayerDrawTaskEntity entity,
      List<CompetitionPlayerDrawSlot> slots,
      String status,
      int drawnCount) {
    PlatformUserPrincipal principal = currentUser();
    entity.setResultSnapshotJson(writeJson(slots));
    entity.setStatus(status);
    entity.setDrawnCount(drawnCount);
    entity.setPlayerCount(slots.size());
    entity.setUpdatedBy(principal.getUserId());
    entity.setUpdatedAt(LocalDateTime.now());
    if (taskMapper.updateById(entity) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "抽签结果保存失败");
    }
    return toVO(entity, withMobile(entity.getStageId(), slots));
  }

  private void assertSameEvent(CompetitionPlayerDrawTaskEntity entity, CompetitionStageVO stage) {
    if (stage.getEventId() == null || !stage.getEventId().equals(entity.getEventId())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "抽签任务与赛段不匹配");
    }
  }

  private void assertSlotsStillInStage(Long stageId, List<CompetitionPlayerDrawSlot> slots) {
    Map<Long, CompetitionStagePaperAssignmentPlayerVO> candidateById = candidateMap(stageId);
    for (CompetitionPlayerDrawSlot slot : slots) {
      Long profileId = parseId(slot.contestantProfileId());
      if (profileId == null || !candidateById.containsKey(profileId)) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
            "有选手已不属于当前赛段，无法继续抽签");
      }
    }
  }

  private void validateSnapshot(
      CompetitionPlayerDrawTaskEntity entity, List<CompetitionPlayerDrawSlot> slots) {
    if (slots.size() != entity.getPlayerCount()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "抽签顺序不完整，请重新创建抽签任务");
    }
    Set<Integer> drawOrders = new HashSet<>();
    Set<Integer> appearanceOrders = new HashSet<>();
    Set<String> profileIds = new HashSet<>();
    int drawn = 0;
    for (CompetitionPlayerDrawSlot slot : slots) {
      if (slot.contestantProfileId() == null || !profileIds.add(slot.contestantProfileId())) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "抽签顺序中存在重复选手");
      }
      if (slot.drawOrder() < 1 || slot.drawOrder() > slots.size()
          || !drawOrders.add(slot.drawOrder())) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "抽签顺序数据损坏");
      }
      if (slot.appearanceOrder() != null) {
        if (slot.appearanceOrder() < 1 || slot.appearanceOrder() > slots.size()
            || !appearanceOrders.add(slot.appearanceOrder())) {
          throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "出场顺序数据损坏");
        }
        drawn++;
      }
    }
    if (entity.getDrawnCount() == null || entity.getDrawnCount() != drawn) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "抽签进度与结果不一致");
    }
  }

  private void validateSelectedContestants(Long stageId, List<Long> contestantProfileIds) {
    if (contestantProfileIds.isEmpty() || contestantProfileIds.size() > MAX_SELECTED_PLAYER_COUNT) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "请选择 1 至 " + MAX_SELECTED_PLAYER_COUNT + " 名选手，当前已选 "
              + contestantProfileIds.size() + " 人");
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

  private Map<Long, CompetitionStagePaperAssignmentPlayerVO> candidateMap(Long stageId) {
    Map<Long, CompetitionStagePaperAssignmentPlayerVO> result = new HashMap<>();
    for (CompetitionStagePaperAssignmentPlayerVO candidate : candidates(stageId)) {
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

  private List<CompetitionPlayerDrawSlot> withMobile(
      Long stageId, List<CompetitionPlayerDrawSlot> slots) {
    if (slots == null || slots.isEmpty()) {
      return slots == null ? List.of() : slots;
    }
    Set<Long> profileIds = new HashSet<>();
    for (CompetitionPlayerDrawSlot slot : slots) {
      Long profileId = parseId(slot.contestantProfileId());
      if (profileId != null) {
        profileIds.add(profileId);
      }
    }
    Map<Long, String> mobileByProfileId = maskedMobileByProfileId(candidates(stageId), profileIds);
    if (mobileByProfileId.isEmpty()) {
      return slots;
    }
    List<CompetitionPlayerDrawSlot> result = new ArrayList<>(slots.size());
    for (CompetitionPlayerDrawSlot slot : slots) {
      Long profileId = parseId(slot.contestantProfileId());
      result.add(new CompetitionPlayerDrawSlot(
          slot.contestantProfileId(), slot.contestantNo(), slot.name(), slot.orgName(),
          profileId == null ? null : mobileByProfileId.get(profileId),
          slot.drawOrder(), slot.appearanceOrder()));
    }
    return result;
  }

  private Map<Long, String> maskedMobileByProfileId(
      List<CompetitionStagePaperAssignmentPlayerVO> candidates, Set<Long> profileIds) {
    if (candidates == null || candidates.isEmpty() || profileIds == null || profileIds.isEmpty()) {
      return Map.of();
    }
    Map<Long, Long> playerIdByProfileId = new HashMap<>();
    for (CompetitionStagePaperAssignmentPlayerVO candidate : candidates) {
      if (candidate == null || candidate.contestantProfileId() == null || candidate.playerId() == null) {
        continue;
      }
      Long profileId = parseId(candidate.contestantProfileId());
      if (profileId == null || !profileIds.contains(profileId)) {
        continue;
      }
      Long playerId = parseId(candidate.playerId());
      if (playerId != null) {
        playerIdByProfileId.put(profileId, playerId);
      }
    }
    if (playerIdByProfileId.isEmpty()) {
      return Map.of();
    }
    List<Long> playerIds = playerIdByProfileId.values().stream().distinct().toList();
    Map<Long, String> mobileByPlayerId = new HashMap<>();
    final int chunk = 500;
    for (int i = 0; i < playerIds.size(); i += chunk) {
      List<Long> part = playerIds.subList(i, Math.min(i + chunk, playerIds.size()));
      List<CompetitionPlayerIdentityEntity> rows = identityMapper.selectBatchIds(part);
      if (rows == null) {
        continue;
      }
      for (CompetitionPlayerIdentityEntity row : rows) {
        if (row != null && row.getId() != null
            && row.getMobile() != null && !row.getMobile().isBlank()) {
          mobileByPlayerId.put(row.getId(), SensitiveMaskUtils.maskPhone(row.getMobile()));
        }
      }
    }
    Map<Long, String> result = new HashMap<>();
    for (Map.Entry<Long, Long> entry : playerIdByProfileId.entrySet()) {
      String mobile = mobileByPlayerId.get(entry.getValue());
      if (mobile != null) {
        result.put(entry.getKey(), mobile);
      }
    }
    return result;
  }

  private Long parseId(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Long.valueOf(value);
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private CompetitionPlayerDrawTaskEntity requireTask(Long taskId) {
    CompetitionPlayerDrawTaskEntity entity = taskMapper.selectById(taskId);
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "抽签任务不存在");
    }
    return entity;
  }

  private CompetitionPlayerDrawTaskEntity lockTask(Long taskId) {
    CompetitionPlayerDrawTaskEntity entity = taskMapper.selectOne(
        new LambdaQueryWrapper<CompetitionPlayerDrawTaskEntity>()
            .eq(CompetitionPlayerDrawTaskEntity::getId, taskId)
            .last("FOR UPDATE"));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "抽签任务不存在");
    }
    return entity;
  }

  private List<Long> readSelectedIds(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      List<String> values = objectMapper.readValue(json, STRING_LIST);
      if (values == null) {
        return List.of();
      }
      return values.stream().map(Long::valueOf).toList();
    } catch (Exception ex) {
      log.error("Failed to read player draw task selected IDs");
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "抽签任务选手数据损坏");
    }
  }

  private List<CompetitionPlayerDrawSlot> readSlots(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      List<CompetitionPlayerDrawSlot> result = objectMapper.readValue(json, SLOTS);
      return result == null ? List.of() : result;
    } catch (JsonProcessingException ex) {
      log.error("Failed to read player draw task result snapshot");
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "抽签结果数据损坏");
    }
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      log.error("Failed to serialize player draw task data");
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "抽签任务数据保存失败");
    }
  }

  private CompetitionPlayerDrawTaskVO toVO(
      CompetitionPlayerDrawTaskEntity entity, List<CompetitionPlayerDrawSlot> slots) {
    return new CompetitionPlayerDrawTaskVO(
        String.valueOf(entity.getId()), String.valueOf(entity.getEventId()),
        String.valueOf(entity.getStageId()), entity.getStageName(), entity.getPlayerCount(),
        entity.getDrawnCount(), entity.getStatus(), entity.getCreatedAt(),
        slots == null ? List.of() : slots);
  }

  private PlatformUserPrincipal currentUser() {
    PlatformUserPrincipal principal = SecurityContext.currentUser();
    if (principal == null || principal.getTenantId() == null || principal.getUserId() == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前用户身份无效");
    }
    return principal;
  }
}
