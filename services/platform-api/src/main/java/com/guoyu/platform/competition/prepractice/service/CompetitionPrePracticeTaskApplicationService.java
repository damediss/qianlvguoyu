package com.guoyu.platform.competition.prepractice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeStageEntity;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.mapper.CompetitionEventMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeLevelMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeStageMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageMapper;
import com.guoyu.platform.competition.prepractice.dto.CompetitionPrePracticeQuestionCountReq;
import com.guoyu.platform.competition.prepractice.dto.CompetitionPrePracticeQuestionTypeQuotaReq;
import com.guoyu.platform.competition.prepractice.dto.CompetitionPrePracticeSourceScopeReq;
import com.guoyu.platform.competition.prepractice.dto.CompetitionPrePracticeAvailableTaskQueryReq;
import com.guoyu.platform.competition.prepractice.dto.CompetitionPrePracticeTaskCreateReq;
import com.guoyu.platform.competition.prepractice.dto.CompetitionPrePracticeTaskQueryReq;
import com.guoyu.platform.competition.prepractice.entity.CompetitionPrePracticeTaskEntity;
import com.guoyu.platform.competition.prepractice.entity.CompetitionPrePracticeTaskQuestionEntity;
import com.guoyu.platform.competition.prepractice.mapper.CompetitionPrePracticeTaskMapper;
import com.guoyu.platform.competition.prepractice.mapper.CompetitionPrePracticeTaskQuestionMapper;
import com.guoyu.platform.competition.prepractice.vo.CompetitionPrePracticeQuestionTypeCountVO;
import com.guoyu.platform.competition.prepractice.vo.CompetitionPrePracticeSourceScopeVO;
import com.guoyu.platform.competition.prepractice.vo.CompetitionPrePracticeTaskVO;
import com.guoyu.platform.exam.dto.ExamPracticeCourseScope;
import com.guoyu.platform.exam.dto.ExamPracticeQuestionResourceRow;
import com.guoyu.platform.exam.reviewsync.support.QuestionTypes;
import com.guoyu.platform.exam.service.ExamPracticeResourceApplicationService;
import com.guoyu.platform.exam.zhenti.service.ZhentiLevel;
import com.guoyu.platform.security.SecurityContext;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CompetitionPrePracticeTaskApplicationService {

  private final CompetitionEventMapper eventMapper;
  private final CompetitionEventWorkTypeMapper eventWorkTypeMapper;
  private final CompetitionEventWorkTypeLevelMapper eventWorkTypeLevelMapper;
  private final CompetitionEventWorkTypeStageMapper eventWorkTypeStageMapper;
  private final CompetitionStageMapper stageMapper;
  private final CompetitionPrePracticeTaskMapper taskMapper;
  private final CompetitionPrePracticeTaskQuestionMapper taskQuestionMapper;
  private final ExamPracticeResourceApplicationService resourceService;
  private final ObjectMapper objectMapper;

  public CompetitionPrePracticeTaskApplicationService(
      CompetitionEventMapper eventMapper,
      CompetitionEventWorkTypeMapper eventWorkTypeMapper,
      CompetitionEventWorkTypeLevelMapper eventWorkTypeLevelMapper,
      CompetitionEventWorkTypeStageMapper eventWorkTypeStageMapper,
      CompetitionStageMapper stageMapper,
      CompetitionPrePracticeTaskMapper taskMapper,
      CompetitionPrePracticeTaskQuestionMapper taskQuestionMapper,
      ExamPracticeResourceApplicationService resourceService,
      ObjectMapper objectMapper) {
    this.eventMapper = eventMapper;
    this.eventWorkTypeMapper = eventWorkTypeMapper;
    this.eventWorkTypeLevelMapper = eventWorkTypeLevelMapper;
    this.eventWorkTypeStageMapper = eventWorkTypeStageMapper;
    this.stageMapper = stageMapper;
    this.taskMapper = taskMapper;
    this.taskQuestionMapper = taskQuestionMapper;
    this.resourceService = resourceService;
    this.objectMapper = objectMapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<CompetitionPrePracticeTaskVO> page(CompetitionPrePracticeTaskQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<CompetitionPrePracticeTaskEntity> wrapper =
        new LambdaQueryWrapper<CompetitionPrePracticeTaskEntity>()
            .eq(CompetitionPrePracticeTaskEntity::getTenantId, tenantId)
            .eq(req.getEventId() != null, CompetitionPrePracticeTaskEntity::getEventId, req.getEventId())
            .orderByDesc(CompetitionPrePracticeTaskEntity::getCreatedAt)
            .orderByDesc(CompetitionPrePracticeTaskEntity::getId);
    Page<CompetitionPrePracticeTaskEntity> page = taskMapper.selectPage(
        new Page<>(req.getPage(), req.getPageSize()), wrapper);
    List<CompetitionPrePracticeTaskVO> items = page.getRecords() == null
        ? List.of()
        : page.getRecords().stream().filter(Objects::nonNull).map(this::toVO).toList();
    return new PageResponse<>(items, page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public PageResponse<CompetitionPrePracticeTaskVO> availableForPush(
      CompetitionPrePracticeAvailableTaskQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Page<CompetitionPrePracticeTaskEntity> page = taskMapper.selectPage(
        new Page<>(req.getPage(), req.getPageSize()),
        new LambdaQueryWrapper<CompetitionPrePracticeTaskEntity>()
            .eq(CompetitionPrePracticeTaskEntity::getTenantId, tenantId)
            .eq(CompetitionPrePracticeTaskEntity::getEventId, req.getEventId())
            .eq(CompetitionPrePracticeTaskEntity::getEventWorkTypeId, req.getEventWorkTypeId())
            .eq(CompetitionPrePracticeTaskEntity::getEventWorkTypeLevelCode, req.getLevelCode())
            .eq(CompetitionPrePracticeTaskEntity::getStatus, "configured")
            .orderByDesc(CompetitionPrePracticeTaskEntity::getCreatedAt)
            .orderByDesc(CompetitionPrePracticeTaskEntity::getId));
    List<CompetitionPrePracticeTaskVO> items = page.getRecords() == null
        ? List.of() : page.getRecords().stream().map(this::toVO).toList();
    return new PageResponse<>(items, page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public List<CompetitionPrePracticeQuestionTypeCountVO> questionCounts(
      CompetitionPrePracticeQuestionCountReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    List<ExamPracticeCourseScope> scopes = courseScopes(req.sourceScope(), tenantId);
    Map<String, Long> counts = resourceService.countAvailable(tenantId, scopes);
    return QuestionTypes.SCOPE_TYPES.stream()
        .map(type -> new CompetitionPrePracticeQuestionTypeCountVO(
            type, QuestionTypes.toLabel(type), 0, counts.getOrDefault(type, 0L)))
        .toList();
  }

  @Transactional
  public CompetitionPrePracticeTaskVO create(CompetitionPrePracticeTaskCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    List<ExamPracticeCourseScope> scopes = courseScopes(req.sourceScope(), tenantId);
    TargetScope target = requireTargetScope(tenantId, req);
    Map<String, Long> availableCounts = resourceService.countAvailable(tenantId, scopes);
    List<CompetitionPrePracticeQuestionTypeCountVO> configuredCounts =
        validateQuotas(req.questionTypeQuotas(), availableCounts);
    int questionCount = configuredCounts.stream()
        .mapToInt(CompetitionPrePracticeQuestionTypeCountVO::questionCount).sum();
    if (questionCount <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请至少为一个题型配置1道试题");
    }

    List<Integer> sourceWorkTypeIds = req.sourceScope().sourceWorkTypeIds().stream()
        .map(this::toIntWorkTypeId).distinct().toList();
    Map<Integer, String> sourceWorkTypeNames = resourceService.loadWorkTypeNames(tenantId, sourceWorkTypeIds);
    if (sourceWorkTypeNames.size() != sourceWorkTypeIds.size()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "试题资源工种不存在或已停用，请刷新后重试");
    }
    List<CompetitionPrePracticeSourceScopeVO> sourceScope = buildSourceScope(
        req.sourceScope(), sourceWorkTypeNames);

    CompetitionPrePracticeTaskEntity task = new CompetitionPrePracticeTaskEntity();
    task.setTenantId(tenantId);
    task.setEventId(target.event().getId());
    task.setEventNameSnapshot(target.event().getName());
    task.setEventWorkTypeId(target.workType().getId());
    task.setEventWorkTypeNameSnapshot(target.workType().getDisplayName());
    task.setEventWorkTypeLevelCode(target.level().getLevelCode());
    task.setEventWorkTypeLevelNameSnapshot(target.level().getLevelName());
    task.setStageId(target.stage().getId());
    task.setStageNameSnapshot(target.stage().getName());
    task.setTitle(req.title().trim());
    task.setSourceScopeJson(writeJson(sourceScope));
    task.setQuestionTypeCountsJson(writeJson(configuredCounts));
    task.setQuestionCount(questionCount);
    task.setStatus("configured");
    task.setDeliveryStatus("not_pushed");
    taskMapper.insert(task);

    int sortOrder = 0;
    for (CompetitionPrePracticeQuestionTypeCountVO quota : configuredCounts) {
      if (quota.questionCount() == null || quota.questionCount() <= 0) {
        continue;
      }
      List<ExamPracticeQuestionResourceRow> sampled = resourceService.sample(
          tenantId, scopes, quota.questionType(), quota.questionCount(), quota.availableCount());
      if (sampled.size() != quota.questionCount()) {
        throw new BusinessException(ErrorCode.CONFLICT, "课程试题资源发生变化，请刷新数量后重新生成");
      }
      for (ExamPracticeQuestionResourceRow question : sampled) {
        if (question == null || question.getQuestionId() == null
            || question.getSourceCourseId() == null || question.getSourceWorkTypeId() == null
            || question.getSourceLevel() == null) {
          throw new BusinessException(ErrorCode.CONFLICT, "课程试题资源信息不完整，请刷新后重试");
        }
        CompetitionPrePracticeTaskQuestionEntity taskQuestion = new CompetitionPrePracticeTaskQuestionEntity();
        taskQuestion.setTenantId(tenantId);
        taskQuestion.setTaskId(task.getId());
        taskQuestion.setSourceQuestionId(question.getQuestionId());
        taskQuestion.setSourceCourseId(question.getSourceCourseId());
        taskQuestion.setSourceWorkTypeId(question.getSourceWorkTypeId());
        taskQuestion.setSourceLevel(question.getSourceLevel());
        taskQuestion.setQuestionType(quota.questionType());
        taskQuestion.setSortOrder(++sortOrder);
        taskQuestion.setQuestionSnapshotJson(writeJson(new PrePracticeQuestionSnapshot(
            question.getQuestionId().toString(), question.getQuestionType(), question.getContent(),
            question.getAnswer(), question.getResolve(), question.getAudio(), question.getVideo(),
            question.getDifficulty(), question.getScore(), question.getKnowledgeId(),
            question.getSourceCourseId(), question.getSourceWorkTypeId(), question.getSourceLevel())));
        taskQuestionMapper.insert(taskQuestion);
      }
    }
    return toVO(task);
  }

  private List<ExamPracticeCourseScope> courseScopes(
      CompetitionPrePracticeSourceScopeReq sourceScope,
      Long tenantId) {
    if (sourceScope == null || sourceScope.sourceWorkTypeIds() == null
        || sourceScope.sourceLevelCodes() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择试题资源工种和等级");
    }
    if (new HashSet<>(sourceScope.sourceWorkTypeIds()).size() != sourceScope.sourceWorkTypeIds().size()
        || new HashSet<>(sourceScope.sourceLevelCodes()).size() != sourceScope.sourceLevelCodes().size()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "试题资源范围中存在重复工种或等级");
    }
    List<Integer> workTypeIds = sourceScope.sourceWorkTypeIds().stream()
        .map(this::toIntWorkTypeId).distinct().toList();
    Map<Integer, String> workTypeNames = resourceService.loadWorkTypeNames(tenantId, workTypeIds);
    if (workTypeNames.size() != workTypeIds.size()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "试题资源工种不存在或已停用，请刷新后重试");
    }
    return ExamPracticeResourceApplicationService.toCourseScopes(
        workTypeIds, sourceScope.sourceLevelCodes());
  }

  private List<CompetitionPrePracticeQuestionTypeCountVO> validateQuotas(
      List<CompetitionPrePracticeQuestionTypeQuotaReq> quotas,
      Map<String, Long> availableCounts) {
    if (quotas == null || quotas.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请配置至少一个题型");
    }
    Set<String> types = new HashSet<>();
    List<CompetitionPrePracticeQuestionTypeCountVO> result = new ArrayList<>();
    for (CompetitionPrePracticeQuestionTypeQuotaReq quota : quotas) {
      String type = QuestionTypes.normalize(quota.questionType());
      Integer count = quota.questionCount();
      if (!QuestionTypes.SCOPE_TYPES.contains(type)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "试题题型不在可选范围内");
      }
      if (!types.add(type)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "同一题型不能重复配置");
      }
      if (count == null || count < 0) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "题型数量不能小于0");
      }
      long available = availableCounts.getOrDefault(type, 0L);
      if (count > available) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
            QuestionTypes.toLabel(type) + "配置数量超过当前可用数量，请刷新后重试");
      }
      result.add(new CompetitionPrePracticeQuestionTypeCountVO(
          type, QuestionTypes.toLabel(type), count, available));
    }
    return result;
  }

  private TargetScope requireTargetScope(Long tenantId, CompetitionPrePracticeTaskCreateReq req) {
    CompetitionEventEntity event = eventMapper.selectOne(
        new LambdaQueryWrapper<CompetitionEventEntity>()
            .eq(CompetitionEventEntity::getTenantId, tenantId)
            .eq(CompetitionEventEntity::getId, req.eventId()));
    if (event == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "赛事不存在或无权访问");
    }
    CompetitionEventWorkTypeEntity workType = eventWorkTypeMapper.selectOne(
        new LambdaQueryWrapper<CompetitionEventWorkTypeEntity>()
            .eq(CompetitionEventWorkTypeEntity::getTenantId, tenantId)
            .eq(CompetitionEventWorkTypeEntity::getEventId, req.eventId())
            .eq(CompetitionEventWorkTypeEntity::getId, req.eventWorkTypeId())
            .eq(CompetitionEventWorkTypeEntity::getStatus, "active"));
    if (workType == null) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "当前赛事工种不存在或已停用");
    }
    String levelCode = req.eventWorkTypeLevelCode().trim();
    CompetitionEventWorkTypeLevelEntity level = eventWorkTypeLevelMapper.selectOne(
        new LambdaQueryWrapper<CompetitionEventWorkTypeLevelEntity>()
            .eq(CompetitionEventWorkTypeLevelEntity::getTenantId, tenantId)
            .eq(CompetitionEventWorkTypeLevelEntity::getEventId, req.eventId())
            .eq(CompetitionEventWorkTypeLevelEntity::getEventWorkTypeId, req.eventWorkTypeId())
            .eq(CompetitionEventWorkTypeLevelEntity::getLevelCode, levelCode));
    if (level == null) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "所选等级未配置在当前赛事工种下");
    }
    CompetitionStageEntity stage = stageMapper.selectOne(
        new LambdaQueryWrapper<CompetitionStageEntity>()
            .eq(CompetitionStageEntity::getTenantId, tenantId)
            .eq(CompetitionStageEntity::getEventId, req.eventId())
            .eq(CompetitionStageEntity::getEventWorkTypeId, req.eventWorkTypeId())
            .eq(CompetitionStageEntity::getEventWorkTypeLevelCode, levelCode)
            .eq(CompetitionStageEntity::getId, req.stageId()));
    if (stage == null) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "所选赛段不属于当前赛事、工种和等级");
    }
    CompetitionEventWorkTypeStageEntity stageConfig = eventWorkTypeStageMapper.selectOne(
        new LambdaQueryWrapper<CompetitionEventWorkTypeStageEntity>()
            .eq(CompetitionEventWorkTypeStageEntity::getTenantId, tenantId)
            .eq(CompetitionEventWorkTypeStageEntity::getEventId, req.eventId())
            .eq(CompetitionEventWorkTypeStageEntity::getEventWorkTypeId, req.eventWorkTypeId())
            .eq(CompetitionEventWorkTypeStageEntity::getEventWorkTypeLevelCode, levelCode)
            .eq(CompetitionEventWorkTypeStageEntity::getStageId, req.stageId())
            .eq(CompetitionEventWorkTypeStageEntity::getEnabled, 1));
    if (stageConfig == null) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "所选赛段未启用，请返回赛事赛段设置检查");
    }
    if (!StringUtils.hasText(event.getName()) || !StringUtils.hasText(workType.getDisplayName())
        || !StringUtils.hasText(level.getLevelName()) || !StringUtils.hasText(stage.getName())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "当前赛事范围信息不完整，请先补全后再生成任务");
    }
    return new TargetScope(event, workType, level, stage);
  }

  private List<CompetitionPrePracticeSourceScopeVO> buildSourceScope(
      CompetitionPrePracticeSourceScopeReq request,
      Map<Integer, String> workTypeNames) {
    List<CompetitionPrePracticeSourceScopeVO> result = new ArrayList<>();
    for (Long rawWorkTypeId : request.sourceWorkTypeIds()) {
      Integer workTypeId = toIntWorkTypeId(rawWorkTypeId);
      for (Integer grade : request.sourceLevelCodes()) {
        if (grade == null || grade < 1 || grade > 5) {
          throw new BusinessException(ErrorCode.BAD_REQUEST, "课程题库等级必须是1至5级");
        }
        result.add(new CompetitionPrePracticeSourceScopeVO(
            String.valueOf(workTypeId), workTypeNames.get(workTypeId), grade,
            ZhentiLevel.labelOf(5 - grade)));
      }
    }
    return result;
  }

  private CompetitionPrePracticeTaskVO toVO(CompetitionPrePracticeTaskEntity entity) {
    return new CompetitionPrePracticeTaskVO(
        id(entity.getId()), id(entity.getEventId()), entity.getEventNameSnapshot(),
        id(entity.getEventWorkTypeId()), entity.getEventWorkTypeNameSnapshot(),
        entity.getEventWorkTypeLevelCode(), entity.getEventWorkTypeLevelNameSnapshot(),
        id(entity.getStageId()), entity.getStageNameSnapshot(), entity.getTitle(),
        readJson(entity.getSourceScopeJson()), readJson(entity.getQuestionTypeCountsJson()),
        entity.getQuestionCount(), entity.getStatus(), entity.getDeliveryStatus(), entity.getCreatedAt());
  }

  private JsonNode readJson(String json) {
    try {
      return StringUtils.hasText(json) ? objectMapper.readTree(json) : objectMapper.createArrayNode();
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("赛前练习任务配置数据无法读取", exception);
    }
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("赛前练习任务配置数据无法保存", exception);
    }
  }

  private Integer toIntWorkTypeId(Long workTypeId) {
    if (workTypeId == null || workTypeId <= 0 || workTypeId > Integer.MAX_VALUE) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "试题资源工种编号无效");
    }
    return workTypeId.intValue();
  }

  private static String id(Long value) {
    return value == null ? null : value.toString();
  }

  private record TargetScope(
      CompetitionEventEntity event,
      CompetitionEventWorkTypeEntity workType,
      CompetitionEventWorkTypeLevelEntity level,
      CompetitionStageEntity stage) {
  }

  private record PrePracticeQuestionSnapshot(
      String questionId,
      String questionType,
      String content,
      String answer,
      String resolve,
      String audio,
      String video,
      Integer difficulty,
      java.math.BigDecimal score,
      Integer knowledgeId,
      Integer courseId,
      Integer workTypeId,
      Integer courseLevel) {
  }
}
