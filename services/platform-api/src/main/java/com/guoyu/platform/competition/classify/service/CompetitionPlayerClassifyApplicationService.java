package com.guoyu.platform.competition.classify.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.classify.dto.CompetitionPlayerClassifyCreateReq;
import com.guoyu.platform.competition.classify.entity.CompetitionPlayerClassifyTaskEntity;
import com.guoyu.platform.competition.classify.mapper.CompetitionPlayerClassifyTaskMapper;
import com.guoyu.platform.competition.classify.vo.CompetitionPlayerClassifyAssignmentVO;
import com.guoyu.platform.competition.classify.vo.CompetitionPlayerClassifyTaskVO;
import com.guoyu.platform.competition.service.CompetitionStageApplicationService;
import com.guoyu.platform.competition.service.CompetitionStagePaperAssignmentApplicationService;
import com.guoyu.platform.competition.vo.CompetitionStagePaperAssignmentPlayerVO;
import com.guoyu.platform.competition.vo.CompetitionStageVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionPlayerClassifyApplicationService {

  private static final String PRACTICAL = "practical";
  private static final String CLASSIFIED = "classified";
  private static final int MAX_SELECTED_PLAYER_COUNT = 5000;
  private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();
  private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};
  private static final TypeReference<List<CompetitionPlayerClassifyAssignmentVO>> ASSIGNMENTS =
      new TypeReference<>() {};

  private final CompetitionStageApplicationService stageService;
  private final CompetitionStagePaperAssignmentApplicationService stagePlayerService;
  private final CompetitionPlayerClassifyTaskMapper taskMapper;
  private final CompetitionPlayerClassifyWorkbookExportService workbookExportService;
  private final ObjectMapper objectMapper;

  public CompetitionPlayerClassifyApplicationService(
      CompetitionStageApplicationService stageService,
      CompetitionStagePaperAssignmentApplicationService stagePlayerService,
      CompetitionPlayerClassifyTaskMapper taskMapper,
      CompetitionPlayerClassifyWorkbookExportService workbookExportService,
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
  public PageResponse<CompetitionPlayerClassifyTaskVO> page(Long stageId, long page, long pageSize) {
    stageService.assertCanManage(stageId);
    Page<CompetitionPlayerClassifyTaskEntity> result = taskMapper.selectPage(
        new Page<>(page, pageSize),
        new LambdaQueryWrapper<CompetitionPlayerClassifyTaskEntity>()
            .eq(CompetitionPlayerClassifyTaskEntity::getStageId, stageId)
            .orderByDesc(CompetitionPlayerClassifyTaskEntity::getCreatedAt));
    if (result == null) {
      log.warn("Player classify task page returned null, stageId={}", stageId);
      return PageResponse.empty(page, pageSize);
    }
    List<CompetitionPlayerClassifyTaskVO> items = result.getRecords() == null
        ? List.of()
        : result.getRecords().stream().map(entity -> toVO(entity, false)).toList();
    return new PageResponse<>(items, result.getCurrent(), result.getSize(), result.getTotal());
  }

  @Transactional(readOnly = true)
  public CompetitionPlayerClassifyTaskVO get(Long taskId) {
    CompetitionPlayerClassifyTaskEntity entity = taskMapper.selectById(taskId);
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "排序分类不存在");
    }
    stageService.assertCanManage(entity.getStageId());
    return toVO(entity, true);
  }

  @Transactional(readOnly = true)
  public byte[] exportExcel(Long taskId) {
    CompetitionPlayerClassifyTaskVO task = get(taskId);
    return workbookExportService.export(task.stageName(), task.assignments());
  }

  @Transactional
  public CompetitionPlayerClassifyTaskVO classify(
      Long stageId, CompetitionPlayerClassifyCreateReq req) {
    CompetitionStageVO stage = stageService.assertCanManage(stageId);
    if (req == null || req.typeNames() == null || req.typeNames().isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请至少添加一个分类");
    }
    if (req.contestantProfileIds() == null || req.contestantProfileIds().isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择参加分类的选手");
    }
    List<String> typeNames = normalizeTypeNames(req.typeNames());
    List<Long> selectedIds = List.copyOf(req.contestantProfileIds());
    validateSelectedIdentities(stageId, selectedIds);
    if (typeNames.size() > selectedIds.size()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "分类数量不能超过选手人数，否则会出现没有选手的分类");
    }

    List<Long> shuffled = new ArrayList<>(selectedIds);
    Collections.shuffle(shuffled, RANDOM);
    List<Integer> sizes = CompetitionPlayerClassifyPlan.balancedSizes(shuffled.size(), typeNames.size());
    Map<Long, CompetitionStagePaperAssignmentPlayerVO> candidateById = candidateMap(stageId);
    List<CompetitionPlayerClassifyAssignmentVO> assignments = new ArrayList<>(shuffled.size());
    int cursor = 0;
    for (int typeIndex = 0; typeIndex < typeNames.size(); typeIndex++) {
      int size = sizes.get(typeIndex);
      for (int order = 1; order <= size; order++) {
        Long profileId = shuffled.get(cursor++);
        CompetitionStagePaperAssignmentPlayerVO candidate = candidateById.get(profileId);
        assignments.add(new CompetitionPlayerClassifyAssignmentVO(
            String.valueOf(profileId),
            candidate == null ? null : candidate.contestantNo(),
            candidate == null ? null : candidate.name(),
            candidate == null || candidate.orgName() == null || candidate.orgName().isBlank()
                ? "未填写参赛公司" : candidate.orgName().trim(),
            typeIndex + 1,
            typeNames.get(typeIndex),
            order));
      }
    }

    PlatformUserPrincipal principal = currentUser();
    LocalDateTime now = LocalDateTime.now();
    CompetitionPlayerClassifyTaskEntity entity = new CompetitionPlayerClassifyTaskEntity();
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
    entity.setTypeCount(typeNames.size());
    entity.setPlayerCount(selectedIds.size());
    entity.setStatus(CLASSIFIED);
    entity.setTypeNamesJson(writeJson(typeNames));
    entity.setSelectedProfileIdsJson(writeJson(selectedIds.stream().map(String::valueOf).toList()));
    entity.setResultSnapshotJson(writeJson(assignments));
    if (taskMapper.insert(entity) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "排序分类保存失败");
    }
    return toVO(entity, true);
  }

  @Transactional
  public void delete(Long taskId) {
    CompetitionPlayerClassifyTaskEntity entity = taskMapper.selectOne(
        new LambdaQueryWrapper<CompetitionPlayerClassifyTaskEntity>()
            .eq(CompetitionPlayerClassifyTaskEntity::getId, taskId)
            .last("FOR UPDATE"));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "排序分类不存在");
    }
    stageService.assertCanManage(entity.getStageId());
    if (taskMapper.deleteById(taskId) != 1) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "排序分类不存在");
    }
  }

  private void validateSelectedIdentities(Long stageId, List<Long> contestantProfileIds) {
    if (contestantProfileIds.size() > MAX_SELECTED_PLAYER_COUNT) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "单次最多选择 5000 名选手");
    }
    Set<Long> selectedIds = new HashSet<>(contestantProfileIds);
    if (selectedIds.size() != contestantProfileIds.size()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "选手列表中存在重复选手");
    }
    if (!candidateMap(stageId).keySet().containsAll(selectedIds)) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "所选选手中包含不属于当前赛段的选手，请刷新名单后重试");
    }
  }

  private List<String> normalizeTypeNames(List<String> typeNames) {
    LinkedHashSet<String> unique = new LinkedHashSet<>();
    for (String typeName : typeNames) {
      if (typeName == null || typeName.isBlank()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "分类名称不能为空");
      }
      String normalized = typeName.trim();
      if (normalized.length() > 32) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "分类名称不能超过 32 个字");
      }
      if (!unique.add(normalized)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "分类名称不能重复：" + normalized);
      }
    }
    if (unique.size() > 20) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "分类最多 20 个");
    }
    return List.copyOf(unique);
  }

  private Map<Long, CompetitionStagePaperAssignmentPlayerVO> candidateMap(Long stageId) {
    List<CompetitionStagePaperAssignmentPlayerVO> candidates =
        stagePlayerService.visualization(stageId, PRACTICAL);
    Map<Long, CompetitionStagePaperAssignmentPlayerVO> result = new HashMap<>();
    if (candidates == null) {
      return result;
    }
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

  private CompetitionPlayerClassifyTaskVO toVO(
      CompetitionPlayerClassifyTaskEntity entity, boolean includeAssignments) {
    List<String> typeNames = readTypeNames(entity.getTypeNamesJson());
    List<CompetitionPlayerClassifyAssignmentVO> assignments = includeAssignments
        ? readAssignments(entity.getResultSnapshotJson())
        : List.of();
    List<Integer> typeSizes = List.of();
    Integer playerCount = entity.getPlayerCount();
    Integer typeCount = entity.getTypeCount();
    if (playerCount != null && typeCount != null && typeCount >= 1 && typeCount <= playerCount) {
      typeSizes = CompetitionPlayerClassifyPlan.balancedSizes(playerCount, typeCount);
    }
    return new CompetitionPlayerClassifyTaskVO(
        String.valueOf(entity.getId()),
        String.valueOf(entity.getEventId()),
        String.valueOf(entity.getStageId()),
        entity.getStageName(),
        typeCount,
        playerCount,
        entity.getStatus(),
        entity.getCreatedAt(),
        typeNames,
        typeSizes,
        assignments);
  }

  private List<String> readTypeNames(String json) {
    if (json == null || json.isBlank()) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "排序分类数据损坏");
    }
    try {
      List<String> names = objectMapper.readValue(json, STRING_LIST);
      return names == null ? List.of() : names;
    } catch (JsonProcessingException ex) {
      log.error("Failed to read player classify type names");
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "排序分类数据损坏");
    }
  }

  private List<CompetitionPlayerClassifyAssignmentVO> readAssignments(String json) {
    if (json == null || json.isBlank()) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "排序分类结果不完整");
    }
    try {
      List<CompetitionPlayerClassifyAssignmentVO> assignments =
          objectMapper.readValue(json, ASSIGNMENTS);
      if (assignments == null || assignments.isEmpty()) {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "排序分类结果不完整");
      }
      return assignments;
    } catch (JsonProcessingException ex) {
      log.error("Failed to read player classify assignments");
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "排序分类结果不完整");
    }
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      log.error("Failed to serialize player classify task data");
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "排序分类数据保存失败");
    }
  }

  private PlatformUserPrincipal currentUser() {
    PlatformUserPrincipal principal = SecurityContext.currentUser();
    if (principal == null || principal.getTenantId() == null || principal.getUserId() == null) {
      log.warn("Player classify rejected because the current user is missing");
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前用户身份无效");
    }
    return principal;
  }
}
