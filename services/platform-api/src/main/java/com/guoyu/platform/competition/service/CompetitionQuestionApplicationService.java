package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.competition.dto.CompetitionPracticalQuestionDetailReq;
import com.guoyu.platform.competition.dto.CompetitionPracticalAssessmentContentReq;
import com.guoyu.platform.competition.dto.CompetitionPracticalAssessmentElementReq;
import com.guoyu.platform.competition.dto.CompetitionPracticalScoreItemReq;
import com.guoyu.platform.competition.dto.CompetitionQuestionCreateReq;
import com.guoyu.platform.competition.dto.CompetitionQuestionQueryReq;
import com.guoyu.platform.competition.dto.CompetitionQuestionUpdateReq;
import com.guoyu.platform.competition.dto.CompetitionQuestionBlueprintBindingReq;
import com.guoyu.platform.competition.dto.CompetitionQuestionBlueprintBatchBindingReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalQuestionDetailEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalQuestionAssessmentContentEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalQuestionScoreItemEntity;
import com.guoyu.platform.competition.entity.CompetitionQuestionEntity;
import com.guoyu.platform.competition.entity.CompetitionQuestionImportBatchEntity;
import com.guoyu.platform.competition.entity.CompetitionQuestionBlueprintEntity;
import com.guoyu.platform.competition.entity.CompetitionQuestionBlueprintNodeEntity;
import com.guoyu.platform.competition.entity.CompetitionQuestionBlueprintBindingEntity;
import com.guoyu.platform.competition.mapper.CompetitionEventMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeLevelMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalQuestionDetailMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalQuestionAssessmentContentMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalQuestionScoreItemMapper;
import com.guoyu.platform.competition.mapper.CompetitionQuestionImportBatchMapper;
import com.guoyu.platform.competition.mapper.CompetitionQuestionMapper;
import com.guoyu.platform.competition.mapper.CompetitionQuestionBlueprintBindingMapper;
import com.guoyu.platform.competition.mapper.CompetitionQuestionBlueprintNodeMapper;
import com.guoyu.platform.competition.vo.CompetitionPracticalScoreItemVO;
import com.guoyu.platform.competition.vo.CompetitionPracticalAssessmentContentVO;
import com.guoyu.platform.competition.vo.CompetitionPracticalAssessmentElementVO;
import com.guoyu.platform.competition.vo.CompetitionPracticalQuestionDetailVO;
import com.guoyu.platform.competition.vo.CompetitionQuestionImportBatchVO;
import com.guoyu.platform.competition.vo.CompetitionQuestionVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.Collections;
import java.text.Normalizer;
import java.util.concurrent.RejectedExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class CompetitionQuestionApplicationService {

  private final CompetitionQuestionMapper questionMapper;
  private final CompetitionPracticalQuestionDetailMapper practicalDetailMapper;
  private final CompetitionPracticalQuestionAssessmentContentMapper assessmentContentMapper;
  private final CompetitionPracticalQuestionScoreItemMapper scoreItemMapper;
  private final CompetitionQuestionImportBatchMapper batchMapper;
  private final CompetitionEventMapper eventMapper;
  private final CompetitionEventWorkTypeMapper workTypeMapper;
  private final CompetitionEventWorkTypeLevelMapper levelMapper;
  private final CompetitionQuestionWorkbookParser parser;
  private final CompetitionQuestionImportPersistenceService persistenceService;
  private final CompetitionQuestionBlueprintApplicationService blueprintService;
  private final CompetitionQuestionBlueprintNodeMapper blueprintNodeMapper;
  private final CompetitionQuestionBlueprintBindingMapper blueprintBindingMapper;
  private final ThreadPoolTaskExecutor importExecutor;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private static final Safelist PRACTICAL_HTML = Safelist.relaxed()
      .addTags("u", "s")
      .addProtocols("a", "href", "http", "https", "mailto")
      .removeTags("img");

  @Autowired
  public CompetitionQuestionApplicationService(
      CompetitionQuestionMapper questionMapper,
      CompetitionPracticalQuestionDetailMapper practicalDetailMapper,
      CompetitionPracticalQuestionAssessmentContentMapper assessmentContentMapper,
      CompetitionPracticalQuestionScoreItemMapper scoreItemMapper,
      CompetitionQuestionImportBatchMapper batchMapper,
      CompetitionEventMapper eventMapper,
      CompetitionEventWorkTypeMapper workTypeMapper,
      CompetitionEventWorkTypeLevelMapper levelMapper,
      CompetitionQuestionImportPersistenceService persistenceService,
      @Qualifier("competitionQuestionImportExecutor") ThreadPoolTaskExecutor importExecutor,
      CompetitionQuestionBlueprintApplicationService blueprintService,
      CompetitionQuestionBlueprintNodeMapper blueprintNodeMapper,
      CompetitionQuestionBlueprintBindingMapper blueprintBindingMapper) {
    this.questionMapper = questionMapper;
    this.practicalDetailMapper = practicalDetailMapper;
    this.assessmentContentMapper = assessmentContentMapper;
    this.scoreItemMapper = scoreItemMapper;
    this.batchMapper = batchMapper;
    this.eventMapper = eventMapper;
    this.workTypeMapper = workTypeMapper;
    this.levelMapper = levelMapper;
    this.parser = new CompetitionQuestionWorkbookParser();
    this.persistenceService = persistenceService;
    this.importExecutor = importExecutor;
    this.blueprintService = blueprintService;
    this.blueprintNodeMapper = blueprintNodeMapper;
    this.blueprintBindingMapper = blueprintBindingMapper;
  }

  /** Source-compatible constructor retained for existing unit-test and extension callers. */
  public CompetitionQuestionApplicationService(
      CompetitionQuestionMapper questionMapper,
      CompetitionPracticalQuestionDetailMapper practicalDetailMapper,
      CompetitionPracticalQuestionScoreItemMapper scoreItemMapper,
      CompetitionQuestionImportBatchMapper batchMapper,
      CompetitionEventMapper eventMapper,
      CompetitionEventWorkTypeMapper workTypeMapper,
      CompetitionEventWorkTypeLevelMapper levelMapper,
      CompetitionQuestionImportPersistenceService persistenceService,
      ThreadPoolTaskExecutor importExecutor) {
    this(questionMapper, practicalDetailMapper, null, scoreItemMapper, batchMapper, eventMapper,
        workTypeMapper, levelMapper, persistenceService, importExecutor);
  }

  /** Full constructor retained for callers that need to inject the structured-content mapper. */
  public CompetitionQuestionApplicationService(
      CompetitionQuestionMapper questionMapper,
      CompetitionPracticalQuestionDetailMapper practicalDetailMapper,
      CompetitionPracticalQuestionAssessmentContentMapper assessmentContentMapper,
      CompetitionPracticalQuestionScoreItemMapper scoreItemMapper,
      CompetitionQuestionImportBatchMapper batchMapper,
      CompetitionEventMapper eventMapper,
      CompetitionEventWorkTypeMapper workTypeMapper,
      CompetitionEventWorkTypeLevelMapper levelMapper,
      CompetitionQuestionImportPersistenceService persistenceService,
      ThreadPoolTaskExecutor importExecutor) {
    this(questionMapper, practicalDetailMapper, assessmentContentMapper, scoreItemMapper, batchMapper,
        eventMapper, workTypeMapper, levelMapper, persistenceService, importExecutor, null, null, null);
  }

  @Transactional(readOnly = true)
  public PageResponse<CompetitionQuestionVO> page(Long eventId, CompetitionQuestionQueryReq req) {
    PlatformUserPrincipal principal = currentUser();
    Integer status = req.getStatus();
    validateScope(principal.getTenantId(), eventId, req.getEventWorkTypeId(), req.getEventWorkTypeLevelId());
    Long displayBlueprintId = req.getBlueprintId();
    if (displayBlueprintId == null && blueprintService != null) {
      try {
        displayBlueprintId = blueprintService.findActive(principal.getTenantId(), eventId,
            req.getEventWorkTypeId(), req.getEventWorkTypeLevelId(), req.getContentType()).getId();
      } catch (BusinessException ignored) {
        // Historical/unbound questions remain listable even before a blueprint is activated.
      }
    }
    LambdaQueryWrapper<CompetitionQuestionEntity> wrapper = new LambdaQueryWrapper<CompetitionQuestionEntity>()
        .eq(CompetitionQuestionEntity::getTenantId, principal.getTenantId())
        .eq(CompetitionQuestionEntity::getEventId, eventId)
        .eq(req.getEventWorkTypeId() != null, CompetitionQuestionEntity::getEventWorkTypeId, req.getEventWorkTypeId())
        .eq(req.getEventWorkTypeLevelId() != null, CompetitionQuestionEntity::getEventWorkTypeLevelId, req.getEventWorkTypeLevelId())
        .eq(StringUtils.hasText(req.getContentType()), CompetitionQuestionEntity::getContentType, req.getContentType())
        .eq(StringUtils.hasText(req.getQuestionType()), CompetitionQuestionEntity::getQuestionType, req.getQuestionType())
        // A blueprint id is also used to calculate binding status for the
        // default list.  Only an explicit “bound”/node filter should restrict
        // the rows; otherwise historical unbound questions must remain visible.
        .inSql(req.getBlueprintId() != null
                && ("bound".equalsIgnoreCase(req.getBindingStatus()) || req.getBlueprintNodeId() != null),
            CompetitionQuestionEntity::getId,
            req.getBlueprintId() == null ? "SELECT 0" : "SELECT question_id FROM competition_question_blueprint_binding"
                + " WHERE tenant_id = " + principal.getTenantId() + " AND blueprint_id = " + req.getBlueprintId()
                + " AND deleted = 0")
        .inSql(req.getBlueprintNodeId() != null,
            CompetitionQuestionEntity::getId,
            req.getBlueprintNodeId() == null ? "SELECT 0" : "SELECT question_id FROM competition_question_blueprint_binding"
                + " WHERE tenant_id = " + principal.getTenantId() + " AND blueprint_node_id = " + req.getBlueprintNodeId()
                + " AND deleted = 0")
        .notInSql("unbound".equalsIgnoreCase(req.getBindingStatus()) && req.getBlueprintId() != null,
            CompetitionQuestionEntity::getId,
            "SELECT question_id FROM competition_question_blueprint_binding WHERE tenant_id = "
                + principal.getTenantId() + " AND blueprint_id = " + req.getBlueprintId() + " AND deleted = 0")
        .eq(status != null, CompetitionQuestionEntity::getStatus, Integer.valueOf(1).equals(status) ? "enabled" : "disabled")
        .like(StringUtils.hasText(req.getKeyword()), CompetitionQuestionEntity::getStem, req.getKeyword())
        .orderByAsc(CompetitionQuestionEntity::getSortOrder)
        .orderByAsc(CompetitionQuestionEntity::getId);
    Page<CompetitionQuestionEntity> result = questionMapper.selectPage(
        new Page<>(req.getPage(), req.getPageSize()), wrapper);
    if (result == null) {
      log.warn("Competition question mapper returned null page, tenantId={}, eventId={}",
          principal.getTenantId(), eventId);
      result = new Page<>(req.getPage(), req.getPageSize());
    }
    List<CompetitionQuestionVO> items = new ArrayList<>();
    if (result.getRecords() != null) {
      // The practical plan editor needs the immutable duration/total-score metadata in
      // order to populate its read-only fields when a question is selected.  Theory rows
      // remain lightweight; practical pages are bounded by the normal page size and may
      // include their structured detail and score elements.
      boolean includePracticalDetail = "practical".equalsIgnoreCase(req.getContentType());
      for (CompetitionQuestionEntity entity : result.getRecords()) {
        CompetitionQuestionBlueprintBindingEntity binding = null;
        if (displayBlueprintId != null && blueprintBindingMapper != null) {
          List<CompetitionQuestionBlueprintBindingEntity> rows = blueprintBindingMapper.selectByQuestionAndBlueprint(
              principal.getTenantId(), entity.getId(), displayBlueprintId);
          if (rows != null && !rows.isEmpty()) binding = rows.get(0);
        }
        items.add(toVO(principal.getTenantId(), entity, includePracticalDetail, binding));
      }
    }
    return new PageResponse<>(items, result.getCurrent(), result.getSize(), result.getTotal());
  }

  @Transactional(readOnly = true)
  public CompetitionQuestionVO get(Long eventId, Long questionId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionQuestionEntity entity = findQuestion(principal.getTenantId(), eventId, questionId);
    CompetitionQuestionBlueprintBindingEntity binding = null;
    if (blueprintService != null && blueprintBindingMapper != null) {
      try {
        Long blueprintId = blueprintService.findActive(principal.getTenantId(), eventId,
            entity.getEventWorkTypeId(), entity.getEventWorkTypeLevelId(), entity.getContentType()).getId();
        List<CompetitionQuestionBlueprintBindingEntity> rows = blueprintBindingMapper.selectByQuestionAndBlueprint(
            principal.getTenantId(), entity.getId(), blueprintId);
        if (rows != null && !rows.isEmpty()) binding = rows.get(0);
      } catch (BusinessException ignored) { }
    }
    return toVO(principal.getTenantId(), entity, true, binding);
  }

  @Transactional
  public CompetitionQuestionVO create(Long eventId, CompetitionQuestionCreateReq req) {
    PlatformUserPrincipal principal = currentUser();
    lockEvent(principal.getTenantId(), eventId);
    String contentType = normalize(req.contentType());
    String questionType = normalize(req.questionType());
    Integer sortOrder = nextSortOrder(principal.getTenantId(), eventId, req.eventWorkTypeId(),
        req.eventWorkTypeLevelId(), contentType, questionType);
    CompetitionPracticalQuestionDetailReq practicalDetail = normalizePracticalDetail(
        req.practicalDetail(), req.scoreItems());
    if (blueprintService != null) {
      // New questions are part of the current blueprint contract.  Existing
      // historical unbound rows remain editable through update(), but a new
      // row cannot be created outside an active theory/practical version.
      blueprintService.findActive(principal.getTenantId(), eventId, req.eventWorkTypeId(),
          req.eventWorkTypeLevelId(), contentType);
      if (req.blueprintId() == null || req.blueprintNodeId() == null) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "当前范围已启用细目表，新建题目必须选择末级考评点");
      }
    }
    validateQuestion(principal.getTenantId(), eventId, req.eventWorkTypeId(), req.eventWorkTypeLevelId(),
        contentType, questionType, sortOrder, practicalDetail,
        null, req.referenceAnswer(), req.fillAnswer(), req.optionsJson());
    CompetitionQuestionEntity entity = new CompetitionQuestionEntity();
    entity.setId(IdWorker.getId());
    entity.setTenantId(principal.getTenantId());
    entity.setEventId(eventId);
    entity.setEventWorkTypeId(req.eventWorkTypeId());
    entity.setEventWorkTypeLevelId(req.eventWorkTypeLevelId());
    entity.setContentType(contentType);
    entity.setQuestionType(questionType);
    entity.setSortOrder(sortOrder);
    // `score` is retained as a legacy non-null column.  Practical questions now
    // derive their authoritative score from the structured detail; theory
    // questions keep a neutral legacy value because the make-plan point owns
    // their runtime score.
    entity.setScore("practical".equals(contentType) && practicalDetail != null
        ? practicalDetail.totalScore() : java.math.BigDecimal.ONE);
    entity.setStem(req.stem());
    entity.setReferenceAnswer(req.referenceAnswer());
    entity.setFillAnswer(req.fillAnswer());
    entity.setOptionsJson(req.optionsJson());
    entity.setDifficulty(req.difficulty());
    entity.setStatus("disabled".equalsIgnoreCase(req.status()) ? "disabled" : "enabled");
    entity.setCreatedBy(principal.getUserId());
    entity.setUpdatedBy(principal.getUserId());
    entity.setDeleted(0);
    if (questionMapper.insert(entity) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "竞赛试题保存失败");
    }
    CompetitionQuestionBlueprintBindingEntity binding = null;
    if (req.blueprintId() != null || req.blueprintNodeId() != null) {
      if (req.blueprintId() == null || req.blueprintNodeId() == null) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "新建题目绑定细目表时必须同时选择版本和末级考评点");
      }
      bindBlueprint(eventId, entity.getId(),
          new CompetitionQuestionBlueprintBindingReq(req.blueprintId(), req.blueprintNodeId()));
      binding = findBinding(principal.getTenantId(), entity.getId(), req.blueprintId());
    }
    savePracticalChildren(principal.getTenantId(), principal.getUserId(), entity.getId(), practicalDetail);
    return toVO(principal.getTenantId(), entity, true, binding);
  }

  @Transactional
  public CompetitionQuestionVO update(Long eventId, Long questionId, CompetitionQuestionUpdateReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionQuestionEntity entity = findQuestion(principal.getTenantId(), eventId, questionId);
    String oldContentType = entity.getContentType();
    Long workTypeId = req.eventWorkTypeId() != null ? req.eventWorkTypeId() : entity.getEventWorkTypeId();
    Long levelId = req.eventWorkTypeLevelId() != null ? req.eventWorkTypeLevelId() : entity.getEventWorkTypeLevelId();
    String contentType = StringUtils.hasText(req.contentType()) ? req.contentType() : entity.getContentType();
    String questionType = StringUtils.hasText(req.questionType()) ? req.questionType() : entity.getQuestionType();
    Integer sequence = entity.getSortOrder();
    String referenceAnswer = req.referenceAnswer() != null
        ? req.referenceAnswer() : entity.getReferenceAnswer();
    String fillAnswer = req.fillAnswer() != null ? req.fillAnswer() : entity.getFillAnswer();
    String optionsJson = req.optionsJson() != null ? req.optionsJson() : entity.getOptionsJson();
    CompetitionPracticalQuestionDetailReq practicalDetail = normalizePracticalDetail(
        req.practicalDetail(), req.scoreItems());
    validateQuestion(principal.getTenantId(), eventId, workTypeId, levelId, contentType, questionType, sequence,
        practicalDetail, null, entity.getContentType().equals(contentType),
        referenceAnswer, fillAnswer, optionsJson);
    ensureSequenceAvailable(principal.getTenantId(), eventId, workTypeId, levelId, contentType, questionType, sequence, questionId);
    entity.setEventWorkTypeId(workTypeId);
    entity.setEventWorkTypeLevelId(levelId);
    entity.setContentType(contentType.toLowerCase(Locale.ROOT));
    entity.setQuestionType(questionType.toLowerCase(Locale.ROOT));
    entity.setSortOrder(sequence);
    if ("practical".equalsIgnoreCase(contentType) && practicalDetail != null) {
      entity.setScore(practicalDetail.totalScore());
    }
    if (req.stem() != null) entity.setStem(req.stem());
    if (req.referenceAnswer() != null) entity.setReferenceAnswer(req.referenceAnswer());
    if (req.fillAnswer() != null) entity.setFillAnswer(req.fillAnswer());
    if (req.optionsJson() != null) entity.setOptionsJson(req.optionsJson());
    if (req.difficulty() != null) entity.setDifficulty(req.difficulty());
    if (req.status() != null) entity.setStatus("disabled".equalsIgnoreCase(req.status()) ? "disabled" : "enabled");
    entity.setUpdatedBy(principal.getUserId());
    if (questionMapper.updateById(entity) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "竞赛试题已发生变化，请刷新后重试");
    }
    if ("practical".equalsIgnoreCase(contentType)) {
      replacePracticalChildren(principal.getTenantId(), principal.getUserId(), entity.getId(), practicalDetail);
    } else if ("practical".equalsIgnoreCase(oldContentType)) {
      deletePracticalChildren(principal.getTenantId(), entity.getId());
    }
    CompetitionQuestionBlueprintBindingEntity binding = null;
    if (req.blueprintId() != null || req.blueprintNodeId() != null) {
      if (req.blueprintId() == null || req.blueprintNodeId() == null) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "修改题目绑定细目表时必须同时选择版本和末级考评点");
      }
      // Binding is deliberately performed after the question update inside the
      // same transaction.  A historical unbound question can therefore be
      // promoted into the active blueprint without creating a second question.
      bindBlueprint(eventId, questionId,
          new CompetitionQuestionBlueprintBindingReq(req.blueprintId(), req.blueprintNodeId()));
      binding = findBinding(principal.getTenantId(), questionId, req.blueprintId());
    } else if (blueprintService != null && blueprintBindingMapper != null) {
      // Editing a historical/unbound row does not implicitly bind it, while
      // editing an already-bound row must not make the response look unbound.
      try {
        Long activeBlueprintId = blueprintService.findActive(principal.getTenantId(), eventId,
            entity.getEventWorkTypeId(), entity.getEventWorkTypeLevelId(), entity.getContentType()).getId();
        binding = findBinding(principal.getTenantId(), questionId, activeBlueprintId);
      } catch (BusinessException ignored) {
        // No active version is a valid legacy state; the question remains editable.
      }
    }
    return toVO(principal.getTenantId(), entity, true, binding);
  }

  @Transactional
  public CompetitionQuestionVO bindBlueprint(Long eventId, Long questionId,
      CompetitionQuestionBlueprintBindingReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionQuestionEntity question = findQuestion(principal.getTenantId(), eventId, questionId);
    if (blueprintService == null || blueprintNodeMapper == null || blueprintBindingMapper == null) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "竞赛细目表绑定服务未就绪");
    }
    CompetitionQuestionBlueprintEntity blueprint = blueprintService.requireActive(principal.getTenantId(),
        req.blueprintId(), eventId, question.getEventWorkTypeId(), question.getEventWorkTypeLevelId(),
        question.getContentType());
    List<CompetitionQuestionBlueprintNodeEntity> nodes = blueprintNodeMapper.selectByBlueprint(
        principal.getTenantId(), blueprint.getId());
    Set<Long> parentIds = nodes == null ? Set.of() : nodes.stream()
        .map(CompetitionQuestionBlueprintNodeEntity::getParentId)
        .filter(parent -> parent != null && parent != 0L).collect(java.util.stream.Collectors.toSet());
    CompetitionQuestionBlueprintNodeEntity node = nodes == null ? null : nodes.stream()
        .filter(item -> req.blueprintNodeId().equals(item.getId())).findFirst().orElse(null);
    if (node == null || parentIds.contains(node.getId())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "只能绑定细目表末级考评点");
    }
    blueprintBindingMapper.delete(new LambdaQueryWrapper<CompetitionQuestionBlueprintBindingEntity>()
        .eq(CompetitionQuestionBlueprintBindingEntity::getTenantId, principal.getTenantId())
        .eq(CompetitionQuestionBlueprintBindingEntity::getQuestionId, questionId)
        .eq(CompetitionQuestionBlueprintBindingEntity::getBlueprintId, blueprint.getId()));
    CompetitionQuestionBlueprintBindingEntity binding = new CompetitionQuestionBlueprintBindingEntity();
    binding.setId(IdWorker.getId()); binding.setTenantId(principal.getTenantId());
    binding.setQuestionId(questionId); binding.setBlueprintId(blueprint.getId());
    binding.setBlueprintNodeId(node.getId()); binding.setMatchMode("manual");
    binding.setCreatedBy(principal.getUserId()); binding.setUpdatedBy(principal.getUserId()); binding.setDeleted(0);
    if (blueprintBindingMapper.insert(binding) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "题目考评点绑定保存失败");
    }
    return toVO(principal.getTenantId(), question, true, binding);
  }

  private CompetitionQuestionBlueprintBindingEntity findBinding(Long tenantId, Long questionId,
      Long blueprintId) {
    if (blueprintBindingMapper == null || blueprintId == null) return null;
    List<CompetitionQuestionBlueprintBindingEntity> rows = blueprintBindingMapper
        .selectByQuestionAndBlueprint(tenantId, questionId, blueprintId);
    return rows == null || rows.isEmpty() ? null : rows.get(0);
  }

  @Transactional
  public List<CompetitionQuestionVO> bindBlueprintBatch(Long eventId,
      CompetitionQuestionBlueprintBatchBindingReq req) {
    if (req.questionIds() == null || req.questionIds().isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "至少选择一条历史试题");
    }
    List<CompetitionQuestionVO> result = new ArrayList<>();
    for (Long questionId : req.questionIds()) {
      if (questionId == null) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "历史试题编号不能为空");
      }
      result.add(bindBlueprint(eventId, questionId,
          new CompetitionQuestionBlueprintBindingReq(req.blueprintId(), req.blueprintNodeId())));
    }
    return result;
  }

  @Transactional
  public void delete(Long eventId, Long questionId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionQuestionEntity entity = findQuestion(principal.getTenantId(), eventId, questionId);
    if (questionMapper.deleteById(entity.getId()) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "竞赛试题删除失败");
    }
    deletePracticalChildren(principal.getTenantId(), questionId);
  }

  @Transactional
  public CompetitionQuestionImportBatchVO importFile(
      Long eventId, Long eventWorkTypeId, Long eventWorkTypeLevelId, String contentType, MultipartFile file) {
    return importFile(eventId, eventWorkTypeId, eventWorkTypeLevelId, contentType, null, null, file);
  }

  @Transactional
  public CompetitionQuestionImportBatchVO importFile(
      Long eventId, Long eventWorkTypeId, Long eventWorkTypeLevelId, String contentType,
      Long blueprintId, String selectedSheet, MultipartFile file) {
    PlatformUserPrincipal principal = currentUser();
    validateScope(principal.getTenantId(), eventId, eventWorkTypeId, eventWorkTypeLevelId);
    if (!"theory".equalsIgnoreCase(contentType) && !"practical".equalsIgnoreCase(contentType)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "contentType 必须是 theory 或 practical");
    }
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择要上传的 Excel 文件");
    }
    if (blueprintId != null) {
      if (blueprintService == null) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "竞赛细目表服务未就绪");
      }
      blueprintService.requireActive(principal.getTenantId(), blueprintId, eventId, eventWorkTypeId,
          eventWorkTypeLevelId, contentType);
    }
    Path tempFile;
    try {
      String suffix = file.getOriginalFilename() != null && file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".xls")
          ? ".xls" : ".xlsx";
      tempFile = Files.createTempFile("competition-question-", suffix);
      file.transferTo(tempFile);
    } catch (IOException ex) {
      log.error("Failed to stage competition question import file", ex);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导入文件暂存失败");
    }
    CompetitionQuestionImportBatchEntity batch = new CompetitionQuestionImportBatchEntity();
    batch.setId(IdWorker.getId());
    batch.setTenantId(principal.getTenantId());
    batch.setEventId(eventId);
    batch.setEventWorkTypeId(eventWorkTypeId);
    batch.setEventWorkTypeLevelId(eventWorkTypeLevelId);
    batch.setContentType(contentType.toLowerCase(Locale.ROOT));
    batch.setBlueprintId(blueprintId);
    batch.setOriginalFilename(StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "题库导入.xlsx");
    batch.setSelectedSheet(StringUtils.hasText(selectedSheet) ? selectedSheet : null);
    batch.setTempFilePath(tempFile.toString());
    batch.setStatus("pending");
    batch.setTotalCount(0);
    batch.setSuccessCount(0);
    batch.setFailureCount(0);
    batch.setCreatedBy(principal.getUserId());
    batch.setUpdatedBy(principal.getUserId());
    batch.setDeleted(0);
    if (batchMapper.insert(batch) != 1) {
      deleteQuietly(tempFile);
      throw new BusinessException(ErrorCode.CONFLICT, "导入批次创建失败");
    }
    try {
      Long tenantId = principal.getTenantId();
      Long userId = principal.getUserId();
      importExecutor.execute(() -> processImport(batch.getId(), tenantId, userId));
    } catch (RejectedExecutionException ex) {
      batch.setStatus("failed");
      batch.setFailureCount(1);
      batch.setErrorDetail("[\"导入任务繁忙，请稍后重试\"]");
      batch.setFinishedAt(LocalDateTime.now());
      batch.setTempFilePath(null);
      batchMapper.updateById(batch);
      deleteQuietly(tempFile);
      throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "导入任务繁忙，请稍后重试");
    }
    return toBatchVO(batch);
  }

  @Transactional(readOnly = true)
  public CompetitionQuestionImportBatchVO getImportBatch(Long eventId, Long batchId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionQuestionImportBatchEntity batch = batchMapper.selectOne(new LambdaQueryWrapper<CompetitionQuestionImportBatchEntity>()
        .eq(CompetitionQuestionImportBatchEntity::getTenantId, principal.getTenantId())
        .eq(CompetitionQuestionImportBatchEntity::getEventId, eventId)
        .eq(CompetitionQuestionImportBatchEntity::getId, batchId)
        .eq(CompetitionQuestionImportBatchEntity::getDeleted, 0));
    if (batch == null) throw new BusinessException(ErrorCode.NOT_FOUND, "导入批次不存在或不属于当前赛事");
    return toBatchVO(batch);
  }

  public byte[] template(String contentType) {
    if (!"theory".equalsIgnoreCase(contentType) && !"practical".equalsIgnoreCase(contentType)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "contentType 必须是 theory 或 practical");
    }
    try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("竞赛试题");
      String[] headers = "theory".equalsIgnoreCase(contentType)
          ? new String[] {"序号", "题型", "题干", "参考答案", "回答", "选项A", "选项B", "选项C", "选项D", "选项E", "选项F", "选项G", "选项H", "难度", "考评点完整编码", "考评点名称", "层级路径"}
          : new String[] {"序号", "题型", "题干", "考试时间（分钟）", "评分差值", "试题总分", "评分方式", "考核要求", "考场准备", "考生准备", "否定项", "试题材料", "考核内容序号", "考核内容", "评分项序号", "评分要素", "是否配分", "配分", "评分标准", "最大扣分", "步长", "考评点完整编码", "考评点名称", "层级路径"};
      Row header = sheet.createRow(0);
      for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
      Row example = sheet.createRow(1);
      if ("theory".equalsIgnoreCase(contentType)) {
        example.createCell(0).setCellValue(1);
        example.createCell(1).setCellValue("单选题");
        example.createCell(2).setCellValue("示例题干");
        example.createCell(3).setCellValue("A");
        example.createCell(5).setCellValue("选项A");
        example.createCell(6).setCellValue("选项B");
        example.createCell(14).setCellValue("A-1-1-1");
        example.createCell(15).setCellValue("示例考评点");
        example.createCell(16).setCellValue("一级/二级/三级/示例考评点");
      } else {
        example.createCell(0).setCellValue(1);
        example.createCell(1).setCellValue("实操题");
        example.createCell(2).setCellValue("示例实操题");
        example.createCell(3).setCellValue(60);
        example.createCell(4).setCellValue(10);
        example.createCell(5).setCellValue(100);
        example.createCell(6).setCellValue("扣分制");
        example.createCell(7).setCellValue("考核要求");
        example.createCell(12).setCellValue(1);
        example.createCell(13).setCellValue("考核内容");
        example.createCell(14).setCellValue(1);
        example.createCell(15).setCellValue("评分要素");
        example.createCell(16).setCellValue("是");
        example.createCell(17).setCellValue(100);
        example.createCell(18).setCellValue("评分标准");
        example.createCell(19).setCellValue(100);
        example.createCell(20).setCellValue(1);
        example.createCell(21).setCellValue("A-1-1-1");
        example.createCell(22).setCellValue("示例考评点");
        example.createCell(23).setCellValue("一级/二级/示例考评点");
      }
      workbook.write(output);
      return output.toByteArray();
    } catch (IOException ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "模板生成失败");
    }
  }

  private void processImport(Long batchId, Long tenantId, Long userId) {
    TenantContextHolder.setTenantId(tenantId);
    CompetitionQuestionImportBatchEntity batch = null;
    try {
      batch = batchMapper.selectOne(new LambdaQueryWrapper<CompetitionQuestionImportBatchEntity>()
          .eq(CompetitionQuestionImportBatchEntity::getTenantId, tenantId)
          .eq(CompetitionQuestionImportBatchEntity::getId, batchId)
          .eq(CompetitionQuestionImportBatchEntity::getDeleted, 0));
      if (batch == null) return;
      batch.setStatus("processing");
      batch.setStartedAt(LocalDateTime.now());
      batch.setUpdatedBy(userId);
      batchMapper.updateById(batch);
      CompetitionQuestionWorkbookParser.ParsedWorkbook parsed = parser.parse(Path.of(batch.getTempFilePath()),
          batch.getContentType(), batch.getSelectedSheet());
      batch.setTotalCount(parsed.questions().size());
      if (!StringUtils.hasText(batch.getSelectedSheet()) && StringUtils.hasText(parsed.selectedSheet())) {
        batch.setSelectedSheet(parsed.selectedSheet());
      }
      validateImportConflicts(tenantId, batch, parsed.questions());
      List<CompetitionQuestionImportPersistenceService.Binding> bindings = resolveBlueprintBindings(
          tenantId, batch, parsed.questions());
      persistenceService.persist(tenantId, userId, batch.getEventId(), batch.getEventWorkTypeId(),
          batch.getEventWorkTypeLevelId(), batch.getContentType(), batch.getId(), parsed.questions(), bindings);
      batch.setStatus("succeeded");
      batch.setSuccessCount(parsed.questions().size());
      batch.setFailureCount(0);
      batch.setFinishedAt(LocalDateTime.now());
      batch.setUpdatedBy(userId);
      batchMapper.updateById(batch);
      log.info("Competition question import succeeded, batchId={}, count={}, tenantId={}", batchId, parsed.questions().size(), tenantId);
    } catch (Exception ex) {
      log.error("Competition question import failed, batchId={}, tenantId={}", batchId, tenantId, ex);
      if (batch == null) {
        batch = new CompetitionQuestionImportBatchEntity();
        batch.setId(batchId);
        batch.setTenantId(tenantId);
      }
      batch.setStatus("failed");
      batch.setFailureCount(batch.getTotalCount() == null || batch.getTotalCount() == 0 ? 1 : batch.getTotalCount());
      batch.setErrorDetail("[\"" + jsonEscape(ex.getMessage() == null ? "导入失败" : ex.getMessage()) + "\"]");
      batch.setFinishedAt(LocalDateTime.now());
      batch.setUpdatedBy(userId);
      if (batch.getId() != null) batchMapper.updateById(batch);
    } finally {
      if (batch != null && StringUtils.hasText(batch.getTempFilePath())) {
        deleteQuietly(Path.of(batch.getTempFilePath()));
        batch.setTempFilePath(null);
        batch.setUpdatedBy(userId);
        batchMapper.updateById(batch);
      }
      TenantContextHolder.clear();
    }
  }

  private void validateImportConflicts(Long tenantId, CompetitionQuestionImportBatchEntity batch,
      List<CompetitionQuestionWorkbookParser.ParsedQuestion> questions) {
    Set<String> seen = new HashSet<>();
    for (CompetitionQuestionWorkbookParser.ParsedQuestion question : questions) {
      String key = question.questionType() + ":" + question.sortOrder();
      if (!seen.add(key)) throw new BusinessException(ErrorCode.CONFLICT, "文件内存在重复范围+题型+序号: " + key);
      validateParsedQuestion(question, batch.getContentType());
    }
    List<CompetitionQuestionEntity> existing = questionMapper.selectList(new LambdaQueryWrapper<CompetitionQuestionEntity>()
        .eq(CompetitionQuestionEntity::getTenantId, tenantId)
        .eq(CompetitionQuestionEntity::getEventId, batch.getEventId())
        .eq(CompetitionQuestionEntity::getEventWorkTypeId, batch.getEventWorkTypeId())
        .eq(CompetitionQuestionEntity::getEventWorkTypeLevelId, batch.getEventWorkTypeLevelId())
        .eq(CompetitionQuestionEntity::getContentType, batch.getContentType())
        .eq(CompetitionQuestionEntity::getDeleted, 0));
    if (existing != null) {
      for (CompetitionQuestionEntity row : existing) {
        String key = row.getQuestionType() + ":" + row.getSortOrder();
        if (seen.contains(key)) throw new BusinessException(ErrorCode.CONFLICT, "数据库中已存在范围+题型+序号: " + key);
      }
    }
  }

  private void validateParsedQuestion(CompetitionQuestionWorkbookParser.ParsedQuestion question, String contentType) {
    if (question.sortOrder() == null || question.sortOrder() <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "题目序号必须为正数");
    }
    if ("practical".equals(contentType)) {
      if (question.scoreItems().isEmpty()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "实操题至少需要一个评分项（序号" + question.sortOrder() + "）");
      }
      // The current structured workbook is preferred.  The historical nine-column
      // score-record layout is also supported and is marked by the parser so its
      // unambiguous score rows can be mapped into the new nested structure.
      if (!question.structuredTemplate() && !question.legacyPracticalLayout()
          && (question.durationMinutes() == null || question.scoreDifference() == null
              || question.totalScore() == null || !StringUtils.hasText(question.scoringMethod()))) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            "旧版实操模板缺少必填基础字段：考试时间、评分差值、试题总分、评分方式，请下载最新模板后重试");
      }
      if (question.structuredTemplate() || question.legacyPracticalLayout()) {
        if (question.durationMinutes() == null || question.durationMinutes() <= 0
            || question.scoreDifference() == null || question.scoreDifference().signum() < 0
            || question.totalScore() == null || question.totalScore().signum() <= 0
            || question.totalScore().compareTo(new java.math.BigDecimal("100")) > 0
            || question.scoreDifference().compareTo(question.totalScore()) > 0) {
          throw new BusinessException(ErrorCode.BAD_REQUEST, "实操模板的考试时间、评分差值或试题总分无效且不能为空");
        }
        String method = normalizeScoringMethod(question.scoringMethod());
        if (method == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "评分方式只能填写加分制或扣分制");
        if (question.assessmentContents() == null || question.assessmentContents().isEmpty()) {
          throw new BusinessException(ErrorCode.BAD_REQUEST, "实操模板至少需要一个考核内容");
        }
        Set<Integer> contentOrders = new HashSet<>();
        for (CompetitionQuestionWorkbookParser.ParsedAssessmentContent content : question.assessmentContents()) {
          if (content == null || content.sortOrder() == null || content.sortOrder() <= 0
              || !contentOrders.add(content.sortOrder()) || !hasHtmlText(content.contentHtml())
              || content.elements() == null || content.elements().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "实操模板考核内容序号必须唯一且富文本不能为空");
          }
          Set<Integer> elementOrders = new HashSet<>();
          for (CompetitionQuestionWorkbookParser.ParsedScoreItem element : content.elements()) {
            if (element == null || element.itemNo() == null || element.itemNo() <= 0
                || !elementOrders.add(element.itemNo()) || !hasHtmlText(element.elementHtml())
                || !hasHtmlText(element.criterion())) {
              throw new BusinessException(ErrorCode.BAD_REQUEST, "实操模板评分要素序号、要素内容和评分标准不能为空");
            }
          }
        }
        java.math.BigDecimal allocatedTotal = java.math.BigDecimal.ZERO;
        for (CompetitionQuestionWorkbookParser.ParsedScoreItem item : question.scoreItems()) {
          if (item.allocationEnabled()) {
            if (item.maxScore() == null || item.maxScore().signum() <= 0 || item.maxScore().compareTo(new java.math.BigDecimal("100")) > 0) {
              throw new BusinessException(ErrorCode.BAD_REQUEST, "配分要素最大配分必须大于0且不超过100分");
            }
            allocatedTotal = allocatedTotal.add(item.maxScore());
            if ("deductive".equals(method)) {
              if (item.maxDeduction() == null || item.deductionStep() == null || item.maxDeduction().compareTo(item.maxScore()) > 0
                  || item.deductionStep().signum() <= 0 || item.maxDeduction().remainder(item.deductionStep()).signum() != 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "扣分制评分项的最大扣分和步长无效");
              }
            } else if (item.maxDeduction() != null || item.deductionStep() != null) {
              throw new BusinessException(ErrorCode.BAD_REQUEST, "加分制评分项不能填写最大扣分和步长");
            }
          } else if (item.maxScore() != null || item.maxDeduction() != null || item.deductionStep() != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不配分要素不能填写任何分值");
          }
        }
        if (allocatedTotal.compareTo(question.totalScore()) != 0) {
          throw new BusinessException(ErrorCode.BAD_REQUEST, "配分要素合计必须等于试题总分");
        }
        return;
      }
      Set<Integer> itemNos = new HashSet<>();
      for (CompetitionQuestionWorkbookParser.ParsedScoreItem item : question.scoreItems()) {
        if (item.itemNo() == null || item.itemNo() <= 0 || !itemNos.add(item.itemNo())
            || item.maxScore() == null || item.maxScore().signum() <= 0
            || item.maxDeduction() == null || item.maxDeduction().signum() < 0
            || item.deductionStep() == null || item.maxDeduction().compareTo(item.maxScore()) > 0
            || item.deductionStep().signum() <= 0) {
          throw new BusinessException(ErrorCode.BAD_REQUEST, "实操题评分项编号、分值、扣分上限或扣分步长无效（序号" + question.sortOrder() + "）");
        }
      }
    }
  }

  private void validateQuestion(Long tenantId, Long eventId, Long workTypeId, Long levelId,
      String contentType, String questionType, Integer sortOrder,
      CompetitionPracticalQuestionDetailReq detail, List<CompetitionPracticalScoreItemReq> ignoredLegacyItems,
      String referenceAnswer, String fillAnswer, String optionsJson) {
    validateQuestion(tenantId, eventId, workTypeId, levelId, contentType, questionType, sortOrder,
        detail, ignoredLegacyItems, true, referenceAnswer, fillAnswer, optionsJson);
  }

  /**
   * Resolves every imported question to one leaf node before persistence.  This
   * is intentionally all-or-nothing: a missing or ambiguous point rejects the
   * complete batch and therefore cannot leave a partially bound question bank.
   */
  private List<CompetitionQuestionImportPersistenceService.Binding> resolveBlueprintBindings(
      Long tenantId, CompetitionQuestionImportBatchEntity batch,
      List<CompetitionQuestionWorkbookParser.ParsedQuestion> questions) {
    if (batch.getBlueprintId() == null) return List.of();
    if (blueprintNodeMapper == null || blueprintService == null) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "竞赛细目表绑定服务未就绪");
    }
    CompetitionQuestionBlueprintEntity blueprint = blueprintService.requireActive(tenantId, batch.getBlueprintId(),
        batch.getEventId(), batch.getEventWorkTypeId(), batch.getEventWorkTypeLevelId(), batch.getContentType());
    List<CompetitionQuestionBlueprintNodeEntity> nodes = blueprintNodeMapper.selectByBlueprint(tenantId, blueprint.getId());
    if (nodes == null || nodes.isEmpty()) throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "细目表没有可绑定的考评点");
    Set<Long> parentIds = new HashSet<>();
    for (CompetitionQuestionBlueprintNodeEntity node : nodes) {
      if (node.getParentId() != null && node.getParentId() != 0L) parentIds.add(node.getParentId());
    }
    List<CompetitionQuestionBlueprintNodeEntity> leaves = nodes.stream()
        .filter(node -> node != null && node.getId() != null && !parentIds.contains(node.getId()))
        .toList();
    Map<String, List<CompetitionQuestionBlueprintNodeEntity>> byCode = new LinkedHashMap<>();
    Map<String, List<CompetitionQuestionBlueprintNodeEntity>> byName = new LinkedHashMap<>();
    Map<Long, CompetitionQuestionBlueprintNodeEntity> byId = new HashMap<>();
    for (CompetitionQuestionBlueprintNodeEntity node : leaves) {
      byId.put(node.getId(), node);
      byCode.computeIfAbsent(normalizeMatch(node.getFullCode()), ignored -> new ArrayList<>()).add(node);
      byName.computeIfAbsent(normalizeMatch(node.getName()), ignored -> new ArrayList<>()).add(node);
    }
    Map<Long, String> pathById = buildNodePaths(nodes);
    List<CompetitionQuestionImportPersistenceService.Binding> result = new ArrayList<>();
    for (int index = 0; index < questions.size(); index++) {
      CompetitionQuestionWorkbookParser.ParsedQuestion question = questions.get(index);
      String code = normalizeMatch(question.blueprintFullCode());
      String pointName = normalizeMatch(question.blueprintPointName());
      String path = normalizePath(question.blueprintPath());
      if (code.isBlank() && pointName.isBlank()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            "第" + (index + 2) + "行缺少考评点完整编码/考评点名称，无法导入未绑定题目");
      }
      List<CompetitionQuestionBlueprintNodeEntity> candidates = code.isBlank()
          ? new ArrayList<>(byName.getOrDefault(pointName, List.of()))
          : new ArrayList<>(byCode.getOrDefault(code, List.of()));
      if (!path.isBlank()) {
        candidates.removeIf(node -> !pathMatches(path, normalizePath(pathById.get(node.getId()))));
      } else if ("theory".equalsIgnoreCase(batch.getContentType())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            "第" + (index + 2) + "行缺少理论考评点层级路径，无法进行精确匹配");
      }
      if (!pointName.isBlank()) candidates.removeIf(node -> !pointName.equals(normalizeMatch(node.getName())));
      if (candidates.size() != 1) {
        String candidateText = candidates.stream().map(node -> node.getFullCode() + " " + node.getName())
            .limit(10).reduce((left, right) -> left + "、" + right).orElse("无候选");
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            "第" + (index + 2) + "行考评点匹配" + (candidates.isEmpty() ? "失败" : "存在多义")
                + "，原值=" + (StringUtils.hasText(question.blueprintPointName()) ? question.blueprintPointName() : question.blueprintFullCode())
                + "，候选=" + candidateText);
      }
      CompetitionQuestionBlueprintNodeEntity node = candidates.get(0);
      String matchMode = !code.isBlank() ? "full_code" : (!path.isBlank() ? "path_exact" : "point_name_exact");
      result.add(new CompetitionQuestionImportPersistenceService.Binding(index, blueprint.getId(), node.getId(), matchMode));
    }
    return result;
  }

  private Map<Long, String> buildNodePaths(List<CompetitionQuestionBlueprintNodeEntity> nodes) {
    Map<Long, CompetitionQuestionBlueprintNodeEntity> byId = new HashMap<>();
    for (CompetitionQuestionBlueprintNodeEntity node : nodes) byId.put(node.getId(), node);
    Map<Long, String> result = new HashMap<>();
    for (CompetitionQuestionBlueprintNodeEntity node : nodes) {
      List<String> names = new ArrayList<>();
      Set<Long> guard = new HashSet<>();
      CompetitionQuestionBlueprintNodeEntity current = node;
      while (current != null && guard.add(current.getId())) {
        if (StringUtils.hasText(current.getName())) names.add(current.getName());
        Long parent = current.getParentId();
        current = parent == null || parent == 0L ? null : byId.get(parent);
      }
      Collections.reverse(names);
      result.put(node.getId(), String.join("/", names));
    }
    return result;
  }

  private boolean pathMatches(String supplied, String actual) {
    // Paths are part of the import contract.  After Unicode/whitespace and
    // separator normalization, a suffix match would make two different
    // hierarchy roots indistinguishable and could bind to the wrong point.
    return supplied.equals(actual);
  }

  private String normalizePath(String value) {
    String normalized = normalizeMatch(value);
    return normalized.replace('>', '/').replace('＞', '/').replace('\\', '/').replaceAll("/+", "/")
        .replaceAll("^/|/$", "");
  }

  private String normalizeMatch(String value) {
    if (value == null) return "";
    return Normalizer.normalize(value, Normalizer.Form.NFKC).replace('\u200b', ' ')
        .trim().replaceAll("\\s+", "");
  }

  private void validateQuestion(Long tenantId, Long eventId, Long workTypeId, Long levelId,
      String contentType, String questionType, Integer sortOrder,
      CompetitionPracticalQuestionDetailReq detail, List<CompetitionPracticalScoreItemReq> ignoredLegacyItems,
      boolean sameContentType, String referenceAnswer, String fillAnswer, String optionsJson) {
    validateScope(tenantId, eventId, workTypeId, levelId);
    if (!StringUtils.hasText(contentType) || !StringUtils.hasText(questionType)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "题目类型不能为空");
    }
    String normalizedContentType = contentType.toLowerCase(Locale.ROOT);
    String normalizedQuestionType = questionType.toLowerCase(Locale.ROOT);
    if (("theory".equals(normalizedContentType) && !Set.of("single", "multi", "judge", "fill").contains(normalizedQuestionType))
        || ("practical".equals(normalizedContentType) && !"practical".equals(normalizedQuestionType))) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "题型与理论/实操类型不匹配");
    }
    if (sortOrder == null || sortOrder <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "序号必须为正数");
    }
    if ("practical".equals(normalizedContentType)) {
      validatePracticalDetail(detail);
    } else {
      validateTheoryAnswer(normalizedQuestionType, referenceAnswer, fillAnswer, optionsJson);
      if (detail != null || (ignoredLegacyItems != null && !ignoredLegacyItems.isEmpty())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "理论题不能提交实操详情或评分项");
      }
    }
  }

  private CompetitionPracticalQuestionDetailReq normalizePracticalDetail(
      CompetitionPracticalQuestionDetailReq detail, List<CompetitionPracticalScoreItemReq> legacyItems) {
    boolean hasNewStructure = detail != null && (
        detail.assessmentContents() != null
            || detail.durationMinutes() != null
            || detail.scoreDifference() != null
            || detail.totalScore() != null
            || StringUtils.hasText(detail.scoringMethod())
            || StringUtils.hasText(detail.assessmentRequirementsHtml())
            || StringUtils.hasText(detail.examRoomPrepareHtml())
            || StringUtils.hasText(detail.candidatePrepareHtml())
            || StringUtils.hasText(detail.negativeItemsHtml())
            || StringUtils.hasText(detail.questionMaterialsHtml()));
    if (hasNewStructure && legacyItems != null && !legacyItems.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "不能同时提交结构化评分表和旧版评分项");
    }
    if (detail == null && (legacyItems == null || legacyItems.isEmpty())) return null;
    if (detail != null && detail.assessmentContents() != null && !detail.assessmentContents().isEmpty()) {
      return sanitizePracticalDetail(detail);
    }
    if (legacyItems == null || legacyItems.isEmpty()) {
      return detail == null ? null : sanitizePracticalDetail(detail);
    }
    List<CompetitionPracticalAssessmentElementReq> elements = new ArrayList<>();
    java.math.BigDecimal total = java.math.BigDecimal.ZERO;
    int order = 1;
    for (CompetitionPracticalScoreItemReq item : legacyItems) {
      if (item == null) continue;
      java.math.BigDecimal maxScore = item.maxScore();
      if (maxScore != null) total = total.add(maxScore);
      String element = item.itemName();
      String standard = item.criterion();
      elements.add(new CompetitionPracticalAssessmentElementReq(
          item.itemNo() == null ? order : item.itemNo(),
          element,
          true,
          maxScore,
          StringUtils.hasText(standard) ? standard : element,
          item.maxDeduction(),
          item.deductionStep()));
      order++;
    }
    String assessment = detail == null ? null : firstNonBlank(
        detail.assessmentRequirementsHtml(), detail.operationContent());
    String room = detail == null ? null : firstNonBlank(
        detail.examRoomPrepareHtml(), detail.equipmentPrepare());
    String candidate = detail == null ? null : firstNonBlank(
        detail.candidatePrepareHtml(), detail.studentPrepare());
    CompetitionPracticalQuestionDetailReq converted = new CompetitionPracticalQuestionDetailReq(
        detail == null || detail.durationMinutes() == null ? 60 : detail.durationMinutes(),
        detail == null || detail.scoreDifference() == null ? new java.math.BigDecimal("10") : detail.scoreDifference(),
        detail == null || detail.totalScore() == null ? total : detail.totalScore(),
        detail == null || !StringUtils.hasText(detail.scoringMethod()) ? "deductive" : detail.scoringMethod(),
        assessment, room, candidate,
        detail == null ? null : detail.negativeItemsHtml(),
        detail == null ? null : detail.questionMaterialsHtml(),
        List.of(new CompetitionPracticalAssessmentContentReq(
            1, StringUtils.hasText(assessment) ? assessment : "<p>默认考核内容</p>", elements)),
        null, null, null);
    return sanitizePracticalDetail(converted);
  }

  private CompetitionPracticalQuestionDetailReq sanitizePracticalDetail(
      CompetitionPracticalQuestionDetailReq detail) {
    if (detail == null) return null;
    List<CompetitionPracticalAssessmentContentReq> contents = new ArrayList<>();
    if (detail.assessmentContents() != null) {
      for (CompetitionPracticalAssessmentContentReq content : detail.assessmentContents()) {
        if (content == null) continue;
        List<CompetitionPracticalAssessmentElementReq> elements = new ArrayList<>();
        if (content.elements() != null) {
          for (CompetitionPracticalAssessmentElementReq element : content.elements()) {
            if (element == null) continue;
            elements.add(new CompetitionPracticalAssessmentElementReq(
                element.elementOrder(), sanitizeHtml(element.elementHtml()), element.allocationEnabled(),
                element.maxScore(), sanitizeHtml(element.standardHtml()), element.maxDeduction(),
                element.deductionStep()));
          }
        }
        contents.add(new CompetitionPracticalAssessmentContentReq(
            content.sortOrder(), sanitizeHtml(content.contentHtml()), elements));
      }
    }
    return new CompetitionPracticalQuestionDetailReq(
        detail.durationMinutes(), detail.scoreDifference(), detail.totalScore(),
        normalizeScoringMethod(detail.scoringMethod()), sanitizeHtml(firstNonBlank(
            detail.assessmentRequirementsHtml(), detail.operationContent())),
        sanitizeHtml(firstNonBlank(detail.examRoomPrepareHtml(), detail.equipmentPrepare())),
        sanitizeHtml(firstNonBlank(detail.candidatePrepareHtml(), detail.studentPrepare())),
        sanitizeHtml(detail.negativeItemsHtml()), sanitizeHtml(detail.questionMaterialsHtml()), contents,
        null, null, null);
  }

  private void validatePracticalDetail(CompetitionPracticalQuestionDetailReq detail) {
    if (detail == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "实操题必须填写结构化详情");
    if (detail.durationMinutes() == null || detail.durationMinutes() <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "考试时间必须为正整数分钟");
    }
    if (detail.totalScore() == null || detail.totalScore().signum() <= 0
        || detail.totalScore().compareTo(new java.math.BigDecimal("100")) > 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "试题总分必须大于0且不超过100分");
    }
    if (detail.scoreDifference() == null || detail.scoreDifference().signum() < 0
        || detail.scoreDifference().compareTo(detail.totalScore()) > 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "评分差值必须在0到试题总分之间");
    }
    String method = normalizeScoringMethod(detail.scoringMethod());
    if (method == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "评分方式只能是加分制或扣分制");
    if (detail.assessmentContents() == null || detail.assessmentContents().isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "至少需要一个考核内容");
    }
    Set<Integer> contentOrders = new HashSet<>();
    java.math.BigDecimal allocatedTotal = java.math.BigDecimal.ZERO;
    for (CompetitionPracticalAssessmentContentReq content : detail.assessmentContents()) {
      if (content == null || content.sortOrder() == null || content.sortOrder() <= 0
          || !contentOrders.add(content.sortOrder()) || !hasHtmlText(content.contentHtml())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "考核内容序号必须唯一且内容不能为空");
      }
      if (content.elements() == null || content.elements().isEmpty()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "每个考核内容至少需要一个评分要素");
      }
      Set<Integer> elementOrders = new HashSet<>();
      for (CompetitionPracticalAssessmentElementReq element : content.elements()) {
        if (element == null || element.elementOrder() == null || element.elementOrder() <= 0
            || !elementOrders.add(element.elementOrder()) || !hasHtmlText(element.elementHtml())
            || !hasHtmlText(element.standardHtml()) || element.allocationEnabled() == null) {
          throw new BusinessException(ErrorCode.BAD_REQUEST, "评分要素序号、要素内容和评分标准不能为空");
        }
        boolean allocated = Boolean.TRUE.equals(element.allocationEnabled());
        if (!allocated) {
          if (element.maxScore() != null || element.maxDeduction() != null || element.deductionStep() != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不配分要素不能填写分值或扣分规则");
          }
          continue;
        }
        if (element.maxScore() == null || element.maxScore().signum() <= 0
            || element.maxScore().compareTo(new java.math.BigDecimal("100")) > 0) {
          throw new BusinessException(ErrorCode.BAD_REQUEST, "最大配分必须大于0且不超过100分");
        }
        allocatedTotal = allocatedTotal.add(element.maxScore());
        if ("deductive".equals(method)) {
          if (element.maxDeduction() == null || element.maxDeduction().signum() < 0
              || element.maxDeduction().compareTo(element.maxScore()) > 0
              || element.deductionStep() == null || element.deductionStep().signum() <= 0
              || element.maxDeduction().remainder(element.deductionStep()).signum() != 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "扣分制的最大扣分和步长设置无效");
          }
        } else if (element.maxDeduction() != null || element.deductionStep() != null) {
          throw new BusinessException(ErrorCode.BAD_REQUEST, "加分制不需要填写最大扣分和步长");
        }
      }
    }
    if (allocatedTotal.compareTo(detail.totalScore()) != 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "配分要素合计 " + allocatedTotal + " 必须等于试题总分 " + detail.totalScore());
    }
  }

  private String normalizeScoringMethod(String value) {
    if (value == null) return null;
    return switch (value.trim().toLowerCase(Locale.ROOT)) {
      case "additive", "add", "加分", "加分制" -> "additive";
      case "deductive", "deduct", "扣分", "扣分制" -> "deductive";
      default -> null;
    };
  }

  private String sanitizeHtml(String value) {
    if (!StringUtils.hasText(value)) return null;
    return Jsoup.clean(value, PRACTICAL_HTML);
  }

  /** The legacy alias is a VARCHAR(255); structured HTML is authoritative. */
  private String legacyItemName(String html) {
    String text = Jsoup.parse(html).text();
    return text.length() <= 255 ? text : text.substring(0, 255);
  }

  private boolean hasHtmlText(String value) {
    if (!StringUtils.hasText(value)) return false;
    // Import validation must use the same cleaned representation that is stored.
    // Otherwise a payload containing only stripped tags (for example a script
    // element) would pass the required-field check and be persisted as empty.
    String cleaned = Jsoup.clean(value, PRACTICAL_HTML);
    return StringUtils.hasText(Jsoup.parse(cleaned).text());
  }

  private String firstNonBlank(String... values) {
    if (values != null) for (String value : values) if (StringUtils.hasText(value)) return value;
    return null;
  }

  private void validateTheoryAnswer(
      String questionType, String referenceAnswer, String fillAnswer, String optionsJson) {
    if ("fill".equals(questionType)) {
      if (!StringUtils.hasText(fillAnswer) && !StringUtils.hasText(referenceAnswer)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "填空题必须填写回答或参考答案");
      }
      return;
    }
    if (!StringUtils.hasText(referenceAnswer)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "选择题和判断题必须填写参考答案");
    }
    if ("judge".equals(questionType)) {
      String answer = referenceAnswer.trim().toUpperCase(Locale.ROOT);
      if (!Set.of("T", "F").contains(answer)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "判断题参考答案只能是 T 或 F");
      }
      return;
    }
    JsonNode options;
    try {
      options = objectMapper.readTree(optionsJson);
    } catch (JsonProcessingException | IllegalArgumentException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "理论题选项必须是有效JSON对象");
    }
    if (options == null || !options.isObject() || options.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "理论题必须配置选项A-H");
    }
    Set<String> available = new HashSet<>();
    options.fieldNames().forEachRemaining(key -> available.add(key.toUpperCase(Locale.ROOT)));
    String letters = referenceAnswer.toUpperCase(Locale.ROOT).replaceAll("[^A-H]", "");
    if (letters.isBlank() || ("single".equals(questionType) && letters.length() != 1)
        || ("multi".equals(questionType) && letters.length() < 2)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "答案必须是有效的A-H选项");
    }
    Set<String> selected = new HashSet<>();
    for (int index = 0; index < letters.length(); index++) {
      String key = String.valueOf(letters.charAt(index));
      if (!available.contains(key) || !selected.add(key)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "参考答案选项不存在或重复");
      }
    }
  }

  private void ensureSequenceAvailable(Long tenantId, Long eventId, Long workTypeId, Long levelId,
      String contentType, String questionType, Integer sortOrder, Long exceptId) {
    LambdaQueryWrapper<CompetitionQuestionEntity> wrapper = new LambdaQueryWrapper<CompetitionQuestionEntity>()
        .eq(CompetitionQuestionEntity::getTenantId, tenantId)
        .eq(CompetitionQuestionEntity::getEventId, eventId)
        .eq(CompetitionQuestionEntity::getEventWorkTypeId, workTypeId)
        .eq(CompetitionQuestionEntity::getEventWorkTypeLevelId, levelId)
        .eq(CompetitionQuestionEntity::getContentType, contentType.toLowerCase(Locale.ROOT))
        .eq(CompetitionQuestionEntity::getQuestionType, questionType.toLowerCase(Locale.ROOT))
        .eq(CompetitionQuestionEntity::getSortOrder, sortOrder)
        .eq(CompetitionQuestionEntity::getDeleted, 0)
        .ne(exceptId != null, CompetitionQuestionEntity::getId, exceptId);
    if (questionMapper.selectCount(wrapper) > 0) throw new BusinessException(ErrorCode.CONFLICT, "同范围+题型+序号的竞赛试题已存在");
  }

  private Integer nextSortOrder(Long tenantId, Long eventId, Long workTypeId, Long levelId,
      String contentType, String questionType) {
    if (!StringUtils.hasText(contentType) || !StringUtils.hasText(questionType)) return 1;
    // A question number identifies its position in the complete scope, not a position within
    // each individual question type.  This keeps single, multi, judge and fill creation order
    // contiguous (1, 2, 3, ...).
    Integer currentMax = questionMapper.selectMaxSortOrder(
        tenantId, eventId, workTypeId, levelId, contentType, questionType);
    if (currentMax == null) return 1;
    if (currentMax >= Integer.MAX_VALUE) {
      throw new BusinessException(ErrorCode.CONFLICT, "竞赛试题序号已达到最大值，无法继续新增");
    }
    return Math.max(currentMax, 0) + 1;
  }

  private void savePracticalChildren(Long tenantId, Long userId, Long questionId,
      CompetitionPracticalQuestionDetailReq detail) {
    if (detail == null) return;
    CompetitionPracticalQuestionDetailEntity detailEntity = new CompetitionPracticalQuestionDetailEntity();
    detailEntity.setId(IdWorker.getId());
    detailEntity.setTenantId(tenantId);
    detailEntity.setQuestionId(questionId);
    detailEntity.setDurationMinutes(detail.durationMinutes());
    detailEntity.setScoreDifference(detail.scoreDifference());
    detailEntity.setTotalScore(detail.totalScore());
    detailEntity.setScoringMethod(normalizeScoringMethod(detail.scoringMethod()));
    detailEntity.setAssessmentRequirementsHtml(detail.assessmentRequirementsHtml());
    detailEntity.setExamRoomPrepareHtml(detail.examRoomPrepareHtml());
    detailEntity.setCandidatePrepareHtml(detail.candidatePrepareHtml());
    detailEntity.setNegativeItemsHtml(detail.negativeItemsHtml());
    detailEntity.setQuestionMaterialsHtml(detail.questionMaterialsHtml());
    // Keep the old aliases synchronized so already deployed readers continue to work.
    detailEntity.setOperationContent(detail.assessmentRequirementsHtml());
    detailEntity.setEquipmentPrepare(detail.examRoomPrepareHtml());
    detailEntity.setStudentPrepare(detail.candidatePrepareHtml());
    detailEntity.setCreatedBy(userId);
    detailEntity.setUpdatedBy(userId);
    detailEntity.setDeleted(0);
    if (practicalDetailMapper.insert(detailEntity) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "实操题详情保存失败");
    }
    int itemNo = 1;
    List<CompetitionPracticalAssessmentContentReq> contents = detail.assessmentContents() == null
        ? List.of() : detail.assessmentContents();
    for (CompetitionPracticalAssessmentContentReq content : contents) {
      CompetitionPracticalQuestionAssessmentContentEntity contentEntity =
          new CompetitionPracticalQuestionAssessmentContentEntity();
      contentEntity.setId(IdWorker.getId());
      contentEntity.setTenantId(tenantId);
      contentEntity.setQuestionId(questionId);
      contentEntity.setSortOrder(content.sortOrder());
      contentEntity.setContentHtml(content.contentHtml());
      contentEntity.setCreatedBy(userId);
      contentEntity.setUpdatedBy(userId);
      contentEntity.setDeleted(0);
      if (assessmentContentMapper != null && assessmentContentMapper.insert(contentEntity) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "实操考核内容保存失败");
      }
      List<CompetitionPracticalAssessmentElementReq> elements = content.elements() == null
          ? List.of() : content.elements();
      for (CompetitionPracticalAssessmentElementReq element : elements) {
        CompetitionPracticalQuestionScoreItemEntity entity = new CompetitionPracticalQuestionScoreItemEntity();
        entity.setId(IdWorker.getId());
        entity.setTenantId(tenantId);
        entity.setQuestionId(questionId);
        // The source-compatible constructor used by legacy extensions may not
        // have the structured-content mapper.  Keep those callers functional
        // by retaining the flat score item with a null content_id; production
        // Spring wiring always supplies the mapper and persists the group.
        entity.setContentId(assessmentContentMapper == null ? null : contentEntity.getId());
        entity.setElementOrder(element.elementOrder());
        entity.setItemNo(itemNo++);
        entity.setElementHtml(element.elementHtml());
        entity.setAllocationEnabled(Boolean.TRUE.equals(element.allocationEnabled()) ? 1 : 0);
        entity.setItemName(legacyItemName(element.elementHtml()));
        entity.setStandardHtml(element.standardHtml());
        entity.setCriterion(Jsoup.parse(element.standardHtml()).text());
        entity.setMaxScore(Boolean.TRUE.equals(element.allocationEnabled()) ? element.maxScore() : null);
        entity.setMaxDeduction(Boolean.TRUE.equals(element.allocationEnabled()) ? element.maxDeduction() : null);
        entity.setDeductionStep(Boolean.TRUE.equals(element.allocationEnabled()) ? element.deductionStep() : null);
        entity.setStatus("enabled");
        entity.setCreatedBy(userId);
        entity.setUpdatedBy(userId);
        entity.setDeleted(0);
        if (scoreItemMapper.insert(entity) != 1) {
          throw new BusinessException(ErrorCode.CONFLICT, "实操评分项保存失败");
        }
      }
    }
  }

  private void replacePracticalChildren(Long tenantId, Long userId, Long questionId,
      CompetitionPracticalQuestionDetailReq detail) {
    deletePracticalChildren(tenantId, questionId);
    savePracticalChildren(tenantId, userId, questionId, detail);
  }

  private void deletePracticalChildren(Long tenantId, Long questionId) {
    if (assessmentContentMapper != null) {
      assessmentContentMapper.delete(new LambdaQueryWrapper<CompetitionPracticalQuestionAssessmentContentEntity>()
          .eq(CompetitionPracticalQuestionAssessmentContentEntity::getTenantId, tenantId)
          .eq(CompetitionPracticalQuestionAssessmentContentEntity::getQuestionId, questionId));
    }
    practicalDetailMapper.delete(new LambdaQueryWrapper<CompetitionPracticalQuestionDetailEntity>()
        .eq(CompetitionPracticalQuestionDetailEntity::getTenantId, tenantId)
        .eq(CompetitionPracticalQuestionDetailEntity::getQuestionId, questionId));
    scoreItemMapper.delete(new LambdaQueryWrapper<CompetitionPracticalQuestionScoreItemEntity>()
        .eq(CompetitionPracticalQuestionScoreItemEntity::getTenantId, tenantId)
        .eq(CompetitionPracticalQuestionScoreItemEntity::getQuestionId, questionId));
  }

  private CompetitionQuestionEntity findQuestion(Long tenantId, Long eventId, Long questionId) {
    CompetitionQuestionEntity entity = questionMapper.selectOne(new LambdaQueryWrapper<CompetitionQuestionEntity>()
        .eq(CompetitionQuestionEntity::getTenantId, tenantId)
        .eq(CompetitionQuestionEntity::getEventId, eventId)
        .eq(CompetitionQuestionEntity::getId, questionId)
        .eq(CompetitionQuestionEntity::getDeleted, 0));
    if (entity == null) throw new BusinessException(ErrorCode.NOT_FOUND, "竞赛试题不存在");
    return entity;
  }

  private void validateScope(Long tenantId, Long eventId, Long workTypeId, Long levelId) {
    CompetitionEventEntity event = eventMapper.selectOne(new LambdaQueryWrapper<CompetitionEventEntity>()
        .eq(CompetitionEventEntity::getTenantId, tenantId).eq(CompetitionEventEntity::getId, eventId)
        .eq(CompetitionEventEntity::getDeleted, 0));
    if (event == null) throw new BusinessException(ErrorCode.NOT_FOUND, "赛事不存在");
    if (workTypeId == null || levelId == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择赛事、工种和等级范围");
    CompetitionEventWorkTypeEntity workType = workTypeMapper.selectOne(new LambdaQueryWrapper<CompetitionEventWorkTypeEntity>()
        .eq(CompetitionEventWorkTypeEntity::getTenantId, tenantId).eq(CompetitionEventWorkTypeEntity::getId, workTypeId)
        .eq(CompetitionEventWorkTypeEntity::getEventId, eventId).eq(CompetitionEventWorkTypeEntity::getDeleted, 0));
    if (workType == null) throw new BusinessException(ErrorCode.NOT_FOUND, "赛事工种不存在");
    CompetitionEventWorkTypeLevelEntity level = levelMapper.selectOne(new LambdaQueryWrapper<CompetitionEventWorkTypeLevelEntity>()
        .eq(CompetitionEventWorkTypeLevelEntity::getTenantId, tenantId).eq(CompetitionEventWorkTypeLevelEntity::getId, levelId)
        .eq(CompetitionEventWorkTypeLevelEntity::getEventId, eventId).eq(CompetitionEventWorkTypeLevelEntity::getEventWorkTypeId, workTypeId)
        .eq(CompetitionEventWorkTypeLevelEntity::getDeleted, 0));
    if (level == null) throw new BusinessException(ErrorCode.NOT_FOUND, "赛事工种等级不存在");
  }

  private void lockEvent(Long tenantId, Long eventId) {
    CompetitionEventEntity event = eventMapper.selectForUpdate(tenantId, eventId);
    if (event == null) throw new BusinessException(ErrorCode.NOT_FOUND, "赛事不存在");
  }

  private CompetitionQuestionVO toVO(Long tenantId, CompetitionQuestionEntity entity, boolean includeChildren,
      CompetitionQuestionBlueprintBindingEntity binding) {
    CompetitionPracticalQuestionDetailEntity detail = includeChildren
        ? practicalDetailMapper.selectByQuestion(tenantId, entity.getId()) : null;
    List<CompetitionPracticalQuestionScoreItemEntity> scoreItems = includeChildren
        ? scoreItemMapper.selectByQuestion(tenantId, entity.getId()) : List.of();
    List<CompetitionPracticalQuestionAssessmentContentEntity> contents = includeChildren
        && assessmentContentMapper != null
        ? assessmentContentMapper.selectByQuestion(tenantId, entity.getId()) : List.of();
    List<CompetitionPracticalScoreItemVO> itemVOs = new ArrayList<>();
    if (scoreItems != null) {
      for (CompetitionPracticalQuestionScoreItemEntity item : scoreItems) {
        itemVOs.add(new CompetitionPracticalScoreItemVO(String.valueOf(item.getId()), item.getItemNo(),
            item.getItemName(), item.getCriterion(), item.getMaxScore(), item.getMaxDeduction(),
            item.getDeductionStep(), item.getStatus(), item.getElementHtml(),
            Integer.valueOf(1).equals(item.getAllocationEnabled()), item.getStandardHtml(),
            item.getContentId() == null ? null : String.valueOf(item.getContentId()), item.getElementOrder()));
      }
    }
    CompetitionPracticalQuestionDetailVO practicalDetail = toPracticalDetailVO(detail, contents, scoreItems);
    String blueprintFullCode = null;
    String blueprintPath = null;
    if (binding != null && blueprintNodeMapper != null) {
      List<CompetitionQuestionBlueprintNodeEntity> nodes = blueprintNodeMapper.selectByBlueprint(
          tenantId, binding.getBlueprintId());
      if (nodes != null) {
        for (CompetitionQuestionBlueprintNodeEntity node : nodes) {
          if (binding.getBlueprintNodeId().equals(node.getId())) {
            blueprintFullCode = node.getFullCode();
            blueprintPath = buildNodePaths(nodes).get(node.getId());
            break;
          }
        }
      }
    }
    return new CompetitionQuestionVO(String.valueOf(entity.getId()), String.valueOf(entity.getEventId()),
        String.valueOf(entity.getEventWorkTypeId()), String.valueOf(entity.getEventWorkTypeLevelId()), entity.getContentType(),
        entity.getQuestionType(), entity.getSortOrder(), entity.getStem(), entity.getReferenceAnswer(),
        entity.getFillAnswer(), entity.getOptionsJson(), entity.getDifficulty(), entity.getStatus(),
        entity.getSourceBatchId() == null ? null : String.valueOf(entity.getSourceBatchId()),
        binding == null ? null : String.valueOf(binding.getBlueprintId()),
        binding == null ? null : String.valueOf(binding.getBlueprintNodeId()),
        blueprintFullCode,
        blueprintPath,
        binding == null ? "unbound" : "bound",
        detail == null ? null : detail.getOperationContent(), detail == null ? null : detail.getStudentPrepare(),
        detail == null ? null : detail.getEquipmentPrepare(), itemVOs, practicalDetail);
  }

  private CompetitionPracticalQuestionDetailVO toPracticalDetailVO(
      CompetitionPracticalQuestionDetailEntity detail,
      List<CompetitionPracticalQuestionAssessmentContentEntity> contents,
      List<CompetitionPracticalQuestionScoreItemEntity> scoreItems) {
    if (detail == null) return null;
    Map<Long, List<CompetitionPracticalQuestionScoreItemEntity>> itemsByContent = new LinkedHashMap<>();
    if (scoreItems != null) {
      for (CompetitionPracticalQuestionScoreItemEntity item : scoreItems) {
        itemsByContent.computeIfAbsent(item.getContentId(), ignored -> new ArrayList<>()).add(item);
      }
    }
    List<CompetitionPracticalAssessmentContentVO> contentVOs = new ArrayList<>();
    if (contents != null) {
      for (CompetitionPracticalQuestionAssessmentContentEntity content : contents) {
        List<CompetitionPracticalQuestionScoreItemEntity> items = itemsByContent.getOrDefault(
            content.getId(), new ArrayList<>());
        items.sort(Comparator.comparing(CompetitionPracticalQuestionScoreItemEntity::getElementOrder,
            Comparator.nullsLast(Integer::compareTo)).thenComparing(CompetitionPracticalQuestionScoreItemEntity::getId));
        java.math.BigDecimal total = items.stream()
            .filter(item -> Integer.valueOf(1).equals(item.getAllocationEnabled()))
            .map(CompetitionPracticalQuestionScoreItemEntity::getMaxScore)
            .filter(value -> value != null)
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        List<CompetitionPracticalAssessmentElementVO> elementVOs = new ArrayList<>();
        for (CompetitionPracticalQuestionScoreItemEntity item : items) {
          elementVOs.add(new CompetitionPracticalAssessmentElementVO(
              String.valueOf(item.getId()), item.getItemNo(), item.getElementOrder(),
              sanitizeHtml(firstNonBlank(item.getElementHtml(), item.getItemName())),
              Integer.valueOf(1).equals(item.getAllocationEnabled()), item.getMaxScore(),
              sanitizeHtml(firstNonBlank(item.getStandardHtml(), item.getCriterion())), item.getMaxDeduction(),
              item.getDeductionStep(), item.getStatus()));
        }
        contentVOs.add(new CompetitionPracticalAssessmentContentVO(
            String.valueOf(content.getId()), content.getSortOrder(), sanitizeHtml(content.getContentHtml()), total, elementVOs));
      }
    }
    if (contentVOs.isEmpty() && scoreItems != null && !scoreItems.isEmpty()) {
      // Read compatibility for rows created before the structured-content migration.
      java.math.BigDecimal total = scoreItems.stream().map(CompetitionPracticalQuestionScoreItemEntity::getMaxScore)
          .filter(value -> value != null).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
      List<CompetitionPracticalAssessmentElementVO> elements = new ArrayList<>();
      for (CompetitionPracticalQuestionScoreItemEntity item : scoreItems) {
        elements.add(new CompetitionPracticalAssessmentElementVO(String.valueOf(item.getId()), item.getItemNo(),
            item.getElementOrder() == null ? item.getItemNo() : item.getElementOrder(),
            sanitizeHtml(firstNonBlank(item.getElementHtml(), item.getItemName())),
            !Integer.valueOf(0).equals(item.getAllocationEnabled()), item.getMaxScore(),
            sanitizeHtml(firstNonBlank(item.getStandardHtml(), item.getCriterion())), item.getMaxDeduction(),
            item.getDeductionStep(), item.getStatus()));
      }
      contentVOs.add(new CompetitionPracticalAssessmentContentVO(
          String.valueOf(scoreItems.get(0).getQuestionId()), 1, "<p>默认考核内容</p>", total, elements));
    }
    return new CompetitionPracticalQuestionDetailVO(
        detail.getDurationMinutes(), detail.getScoreDifference(), detail.getTotalScore(),
        normalizeScoringMethod(detail.getScoringMethod()),
        sanitizeHtml(firstNonBlank(detail.getAssessmentRequirementsHtml(), detail.getOperationContent())),
        sanitizeHtml(firstNonBlank(detail.getExamRoomPrepareHtml(), detail.getEquipmentPrepare())),
        sanitizeHtml(firstNonBlank(detail.getCandidatePrepareHtml(), detail.getStudentPrepare())),
        sanitizeHtml(detail.getNegativeItemsHtml()), sanitizeHtml(detail.getQuestionMaterialsHtml()), contentVOs);
  }

  private CompetitionQuestionImportBatchVO toBatchVO(CompetitionQuestionImportBatchEntity batch) {
    return new CompetitionQuestionImportBatchVO(String.valueOf(batch.getId()), String.valueOf(batch.getEventId()),
        String.valueOf(batch.getEventWorkTypeId()), String.valueOf(batch.getEventWorkTypeLevelId()), batch.getContentType(),
        batch.getBlueprintId() == null ? null : String.valueOf(batch.getBlueprintId()),
        batch.getOriginalFilename(), batch.getSelectedSheet(), batch.getStatus(), batch.getTotalCount(), batch.getSuccessCount(), batch.getFailureCount(),
        batch.getErrorDetail(), batch.getStartedAt(), batch.getFinishedAt());
  }

  private PlatformUserPrincipal currentUser() {
    try {
      return SecurityContext.currentUser();
    } catch (IllegalStateException ex) {
      log.warn("Competition question request has no authenticated platform user");
      throw ex;
    }
  }

  private String normalize(String value) {
    return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
  }

  private void deleteQuietly(Path path) {
    try { Files.deleteIfExists(path); } catch (IOException ex) { log.warn("Failed to delete temporary import file {}", path); }
  }

  private String jsonEscape(String text) {
    return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n");
  }
}
