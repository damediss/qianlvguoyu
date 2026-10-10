package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.dto.CompetitionTheoryPaperImportReq;
import com.guoyu.platform.competition.dto.CompetitionTheoryCompetitionPaperImportReq;
import com.guoyu.platform.competition.dto.CompetitionTheoryRequirementUpsertReq;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPaperQuestionEntity;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPaperSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionScoreRuleEntity;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryPaperQuestionEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryPaperSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryRequirementEntity;
import com.guoyu.platform.competition.enums.CompetitionScoreRuleStatus;
import com.guoyu.platform.competition.enums.CompetitionTheoryTemplate;
import com.guoyu.platform.competition.mapper.CompetitionTheoryPaperQuestionMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryPaperSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryRequirementMapper;
import com.guoyu.platform.competition.service.CompetitionTheoryAnswerSnapshotAdapter.ParsedAnswer;
import com.guoyu.platform.competition.service.CompetitionTheoryPaperSourceAdapter.SourcePaper;
import com.guoyu.platform.competition.service.CompetitionTheoryPaperSourceAdapter.SourceQuestion;
import com.guoyu.platform.competition.vo.CompetitionTheoryCourseSourceVO;
import com.guoyu.platform.competition.vo.CompetitionTheoryPaperPreviewQuestionVO;
import com.guoyu.platform.competition.vo.CompetitionTheoryPaperPreviewVO;
import com.guoyu.platform.competition.vo.CompetitionTheoryPaperSnapshotVO;
import com.guoyu.platform.competition.vo.CompetitionTheoryPaperSourceVO;
import com.guoyu.platform.competition.vo.CompetitionTheoryRequirementVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class CompetitionTheoryPaperApplicationService {

  private static final String PUBLISHED = "published";
  private static final String EXACT_MATCH = "exact_match";
  private static final Set<String> JUDGE_TRUE_TOKENS =
      Set.of("正确", "对", "true", "t", "1", "是", "√", "yes", "y");
  private static final Set<String> JUDGE_FALSE_TOKENS =
      Set.of("错误", "错", "false", "f", "0", "否", "×", "x", "no", "n");

  private final CompetitionScoreRuleDomainService scoreRuleDomainService;
  private final CompetitionScoreRuleApplicationService scoreRuleApplicationService;
  private final CompetitionStageService stageService;
  private final CompetitionEventWorkTypeDomainService eventWorkTypeDomainService;
  private final CompetitionTheoryRequirementMapper requirementMapper;
  private final CompetitionTheoryPaperSnapshotMapper snapshotMapper;
  private final CompetitionTheoryPaperQuestionMapper questionMapper;
  private final CompetitionTheoryPaperSourceAdapter sourceAdapter;
  private final CompetitionTheoryCompetitionPaperSourceAdapter competitionSourceAdapter;
  private final CompetitionTheoryAnswerSnapshotAdapter answerAdapter;
  private final ObjectMapper objectMapper;

  public CompetitionTheoryPaperApplicationService(
      CompetitionScoreRuleDomainService scoreRuleDomainService,
      CompetitionScoreRuleApplicationService scoreRuleApplicationService,
      CompetitionStageService stageService,
      CompetitionEventWorkTypeDomainService eventWorkTypeDomainService,
      CompetitionTheoryRequirementMapper requirementMapper,
      CompetitionTheoryPaperSnapshotMapper snapshotMapper,
      CompetitionTheoryPaperQuestionMapper questionMapper,
      CompetitionTheoryPaperSourceAdapter sourceAdapter,
      CompetitionTheoryAnswerSnapshotAdapter answerAdapter) {
    this(scoreRuleDomainService, scoreRuleApplicationService, stageService,
        eventWorkTypeDomainService, requirementMapper, snapshotMapper, questionMapper,
        sourceAdapter, answerAdapter, null);
  }

  @Autowired
  public CompetitionTheoryPaperApplicationService(
      CompetitionScoreRuleDomainService scoreRuleDomainService,
      CompetitionScoreRuleApplicationService scoreRuleApplicationService,
      CompetitionStageService stageService,
      CompetitionEventWorkTypeDomainService eventWorkTypeDomainService,
      CompetitionTheoryRequirementMapper requirementMapper,
      CompetitionTheoryPaperSnapshotMapper snapshotMapper,
      CompetitionTheoryPaperQuestionMapper questionMapper,
      CompetitionTheoryPaperSourceAdapter sourceAdapter,
      CompetitionTheoryAnswerSnapshotAdapter answerAdapter,
      CompetitionTheoryCompetitionPaperSourceAdapter competitionSourceAdapter) {
    this.scoreRuleDomainService = scoreRuleDomainService;
    this.scoreRuleApplicationService = scoreRuleApplicationService;
    this.stageService = stageService;
    this.eventWorkTypeDomainService = eventWorkTypeDomainService;
    this.requirementMapper = requirementMapper;
    this.snapshotMapper = snapshotMapper;
    this.questionMapper = questionMapper;
    this.sourceAdapter = sourceAdapter;
    this.competitionSourceAdapter = competitionSourceAdapter;
    this.answerAdapter = answerAdapter;
    this.objectMapper = new ObjectMapper();
  }

  @Transactional(readOnly = true)
  public CompetitionTheoryRequirementVO getRequirement(Long ruleId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionScoreRuleEntity rule =
        scoreRuleDomainService.getById(principal.getTenantId(), ruleId);
    scoreRuleApplicationService.accessibleEvent(principal, rule.getEventId());
    return CompetitionTheoryRequirementVO.from(findRequirement(principal.getTenantId(), ruleId));
  }

  @Transactional
  public CompetitionTheoryRequirementVO upsertRequirement(
      Long ruleId, CompetitionTheoryRequirementUpsertReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionScoreRuleEntity rule =
        scoreRuleDomainService.getById(principal.getTenantId(), ruleId);
    scoreRuleApplicationService.manageableEvent(principal, rule.getEventId());
    if (!CompetitionScoreRuleStatus.DRAFT.getCode().equals(rule.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "只有草稿评分规则可以修改理论考试要求");
    }
    validateRequirement(rule, req);
    CompetitionTheoryRequirementEntity entity =
        findRequirement(principal.getTenantId(), ruleId);
    if (entity == null) {
      entity = new CompetitionTheoryRequirementEntity();
      entity.setTenantId(principal.getTenantId());
      entity.setEventId(rule.getEventId());
      entity.setStageId(rule.getStageId());
      entity.setEventWorkTypeId(rule.getEventWorkTypeId());
      entity.setScoreRuleId(ruleId);
      entity.setCreatedBy(principal.getUserId());
      entity.setDeleted(0);
    }
    applyRequirement(entity, req);
    entity.setUpdatedBy(principal.getUserId());
    try {
      int affected = entity.getId() == null
          ? requirementMapper.insert(entity)
          : requirementMapper.updateById(entity);
      if (affected != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "理论考试要求已发生变化，请刷新后重试");
      }
    } catch (DuplicateKeyException ex) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前评分规则已经存在理论考试要求");
    }
    return CompetitionTheoryRequirementVO.from(entity);
  }

  @Transactional(readOnly = true)
  public List<CompetitionTheoryCourseSourceVO> listCourses(
      Long eventId, Long eventWorkTypeId) {
    PlatformUserPrincipal principal = currentUser();
    scoreRuleApplicationService.accessibleEvent(principal, eventId);
    CompetitionEventWorkTypeEntity eventWorkType =
        requireEventWorkType(principal.getTenantId(), eventId, eventWorkTypeId);
    return sourceAdapter.listCourses(principal.getTenantId(), eventWorkType.getWorkTypeId());
  }

  @Transactional(readOnly = true)
  public List<CompetitionTheoryPaperSourceVO> listSourcePapers(
      Long eventId, Long eventWorkTypeId, Long courseId) {
    PlatformUserPrincipal principal = currentUser();
    scoreRuleApplicationService.accessibleEvent(principal, eventId);
    CompetitionEventWorkTypeEntity eventWorkType =
        requireEventWorkType(principal.getTenantId(), eventId, eventWorkTypeId);
    return sourceAdapter.listCompletedPapers(
        principal.getTenantId(), eventWorkType.getWorkTypeId(), courseId);
  }

  @Transactional(readOnly = true)
  public List<CompetitionTheoryPaperSnapshotVO> listSnapshots(
      Long eventId, Long stageId, Long eventWorkTypeId, Long eventWorkTypeLevelId,
      Long scoreRuleId) {
    PlatformUserPrincipal principal = currentUser();
    scoreRuleApplicationService.accessibleEvent(principal, eventId);
    LambdaQueryWrapper<CompetitionTheoryPaperSnapshotEntity> wrapper =
        new LambdaQueryWrapper<CompetitionTheoryPaperSnapshotEntity>()
            .eq(CompetitionTheoryPaperSnapshotEntity::getTenantId, principal.getTenantId())
            .eq(CompetitionTheoryPaperSnapshotEntity::getEventId, eventId)
            .orderByAsc(CompetitionTheoryPaperSnapshotEntity::getPaperCode)
            .orderByDesc(CompetitionTheoryPaperSnapshotEntity::getVersionNo);
    if (stageId != null) {
      wrapper.eq(CompetitionTheoryPaperSnapshotEntity::getStageId, stageId);
    }
    if (eventWorkTypeId != null) {
      wrapper.eq(CompetitionTheoryPaperSnapshotEntity::getEventWorkTypeId, eventWorkTypeId);
    }
    if (eventWorkTypeLevelId != null) {
      wrapper.eq(CompetitionTheoryPaperSnapshotEntity::getEventWorkTypeLevelId,
          eventWorkTypeLevelId);
    }
    if (scoreRuleId != null) {
      wrapper.eq(CompetitionTheoryPaperSnapshotEntity::getScoreRuleId, scoreRuleId);
    }
    List<CompetitionTheoryPaperSnapshotEntity> records = snapshotMapper.selectList(wrapper);
    if (records == null) {
      log.warn("Theory snapshot mapper returned null list, tenantId={}, eventId={}",
          principal.getTenantId(), eventId);
      return Collections.emptyList();
    }
    return records.stream().map(CompetitionTheoryPaperSnapshotVO::from).toList();
  }

  @Transactional(readOnly = true)
  public CompetitionTheoryPaperPreviewVO previewSnapshot(Long eventId, Long snapshotId) {
    PlatformUserPrincipal principal = currentUser();
    scoreRuleApplicationService.accessibleEvent(principal, eventId);
    CompetitionTheoryPaperSnapshotEntity snapshot = snapshotMapper.selectOne(
        new LambdaQueryWrapper<CompetitionTheoryPaperSnapshotEntity>()
            .eq(CompetitionTheoryPaperSnapshotEntity::getTenantId, principal.getTenantId())
            .eq(CompetitionTheoryPaperSnapshotEntity::getEventId, eventId)
            .eq(CompetitionTheoryPaperSnapshotEntity::getId, snapshotId)
            .eq(CompetitionTheoryPaperSnapshotEntity::getDeleted, 0));
    if (snapshot == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "试卷快照不存在或不属于当前赛事");
    }
    List<CompetitionTheoryPaperQuestionEntity> questions = questionMapper.selectByPaper(snapshotId);
    if (questions == null) {
      log.warn("Theory paper question mapper returned null list, tenantId={}, eventId={}, snapshotId={}",
          principal.getTenantId(), eventId, snapshotId);
      questions = Collections.emptyList();
    }
    List<CompetitionTheoryPaperPreviewQuestionVO> previewQuestions = questions.stream()
        .map(question -> new CompetitionTheoryPaperPreviewQuestionVO(
            question.getSortOrder(), question.getContent(), question.getQuestionType(),
            question.getScore()))
        .toList();
    return CompetitionTheoryPaperPreviewVO.from(snapshot, previewQuestions);
  }

  @Transactional
  public List<CompetitionTheoryPaperSnapshotVO> importPair(
      Long eventId, CompetitionTheoryPaperImportReq req) {
    PlatformUserPrincipal principal = currentUser();
    scoreRuleApplicationService.manageableEvent(principal, eventId);
    CompetitionScoreRuleEntity rule =
        resolveImportScope(principal, eventId, req);
    if (!Objects.equals(eventId, rule.getEventId())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "评分规则不属于当前赛事");
    }
    if (!CompetitionScoreRuleStatus.PUBLISHED.getCode().equals(rule.getStatus())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "只能为已发布评分规则导入试卷");
    }
    CompetitionEventWorkTypeEntity eventWorkType =
        requireEventWorkType(principal.getTenantId(), eventId, rule.getEventWorkTypeId());
    if (Objects.equals(req.paperAId(), req.paperBId())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "A卷和B卷必须选择不同课程试卷");
    }
    SourcePaper paperA = sourceAdapter.loadCompletedPaper(
        principal.getTenantId(), eventWorkType.getWorkTypeId(), req.courseId(), req.paperAId());
    SourcePaper paperB = sourceAdapter.loadCompletedPaper(
        principal.getTenantId(), eventWorkType.getWorkTypeId(), req.courseId(), req.paperBId());
    if (Objects.equals(paperA.planId(), paperB.planId())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "A卷和B卷必须来自两个独立组卷计划");
    }
    PreparedPaper preparedA = preparePaper(paperA);
    PreparedPaper preparedB = preparePaper(paperB);
    requireEquivalent(preparedA, preparedB);
    LocalDateTime now = LocalDateTime.now();
    List<CompetitionTheoryPaperSnapshotVO> result = new ArrayList<>();
    result.add(CompetitionTheoryPaperSnapshotVO.from(
        persistSnapshot(principal, rule, "A", preparedA, now)));
    result.add(CompetitionTheoryPaperSnapshotVO.from(
        persistSnapshot(principal, rule, "B", preparedB, now)));
    return result;
  }

  /**
   * Binds one or more published competition make-exam snapshots to a stage.
   *
   * <p>Each source snapshot belongs to one independent make-exam plan. The stage assigns the
   * execution codes (P1, P2, ...) so A/B and other variants are a stage concern rather than a
   * concern of the make-exam plan.</p>
   */
  @Transactional
  public List<CompetitionTheoryPaperSnapshotVO> importCompetitionPapers(
      Long eventId, CompetitionTheoryCompetitionPaperImportReq req) {
    PlatformUserPrincipal principal = currentUser();
    scoreRuleApplicationService.manageableEvent(principal, eventId);
    CompetitionStageEntity stage = stageService.getById(principal.getTenantId(), req.stageId());
    if (!Objects.equals(eventId, stage.getEventId())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "赛段不属于当前赛事");
    }
    requireEventWorkType(principal.getTenantId(), eventId, req.eventWorkTypeId());
    LinkedHashSet<Long> distinctSnapshotIds = new LinkedHashSet<>(req.paperSnapshotIds());
    if (distinctSnapshotIds.size() != req.paperSnapshotIds().size()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "不能重复选择同一竞赛组卷快照");
    }
    List<CompetitionTheoryCompetitionPaperSourceAdapter.SourcePaper> papers = new ArrayList<>();
    for (Long snapshotId : distinctSnapshotIds) {
      papers.add(competitionSourceAdapter.loadPublishedPaper(
          principal.getTenantId(), eventId, req.eventWorkTypeId(),
          req.eventWorkTypeLevelId(), snapshotId));
    }
    LocalDateTime now = LocalDateTime.now();
    snapshotMapper.deprecatePublishedCompetitionSnapshots(
        principal.getTenantId(), eventId, req.stageId(), req.eventWorkTypeId(),
        req.eventWorkTypeLevelId(),
        principal.getUserId());
    List<CompetitionTheoryPaperSnapshotVO> result = new ArrayList<>();
    int index = 1;
    for (CompetitionTheoryCompetitionPaperSourceAdapter.SourcePaper paper : papers) {
      result.add(CompetitionTheoryPaperSnapshotVO.from(
          persistCompetitionSnapshot(principal, eventId, req.stageId(), req.eventWorkTypeId(),
              req.eventWorkTypeLevelId(), "P" + index++, paper, now)));
    }
    return result;
  }

  private CompetitionTheoryPaperSnapshotEntity persistCompetitionSnapshot(
      PlatformUserPrincipal principal,
      Long eventId,
      Long stageId,
      Long eventWorkTypeId,
      Long eventWorkTypeLevelId,
      String paperCode,
      CompetitionTheoryCompetitionPaperSourceAdapter.SourcePaper source,
      LocalDateTime now) {
    Integer maxVersion = snapshotMapper.selectMaxVersion(
        principal.getTenantId(), eventId, stageId, eventWorkTypeId, null, paperCode);
    CompetitionMakeExamPaperSnapshotEntity sourceSnapshot = source.snapshot();
    CompetitionTheoryPaperSnapshotEntity snapshot = new CompetitionTheoryPaperSnapshotEntity();
    snapshot.setTenantId(principal.getTenantId());
    snapshot.setEventId(eventId);
    snapshot.setStageId(stageId);
    snapshot.setEventWorkTypeId(eventWorkTypeId);
    snapshot.setEventWorkTypeLevelId(eventWorkTypeLevelId);
    snapshot.setScoreRuleId(null);
    snapshot.setRequirementId(null);
    snapshot.setPaperCode(paperCode);
    snapshot.setVersionNo(maxVersion == null ? 1 : maxVersion + 1);
    snapshot.setSourceCourseId(null);
    snapshot.setSourcePlanId(sourceSnapshot.getPlanId());
    snapshot.setSourcePaperId(null);
    snapshot.setSourceCompetitionPaperSnapshotId(sourceSnapshot.getId());
    snapshot.setSourceTitle(resolveTitle(sourceSnapshot.getTitle(), paperCode));
    snapshot.setSourceHash(sourceSnapshot.getSourceHash());
    snapshot.setDurationMinutes(sourceSnapshot.getDurationMinutes());
    snapshot.setQuestionCount(source.questions().size());
    snapshot.setTotalScore(sourceSnapshot.getTotalScore());
    snapshot.setPassScore(sourceSnapshot.getPassScore());
    snapshot.setStatus(PUBLISHED);
    snapshot.setImportedAt(now);
    snapshot.setImportedBy(principal.getUserId());
    snapshot.setPublishedAt(now);
    snapshot.setPublishedBy(principal.getUserId());
    snapshot.setCreatedBy(principal.getUserId());
    snapshot.setUpdatedBy(principal.getUserId());
    snapshot.setDeleted(0);
    try {
      if (snapshotMapper.insert(snapshot) != 1) {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建竞赛理论执行快照失败");
      }
      int sortOrder = 0;
      for (CompetitionMakeExamPaperQuestionEntity sourceQuestion : source.questions()) {
        CompetitionTheoryPaperQuestionEntity question = toCompetitionQuestionEntity(
            principal, snapshot.getId(), ++sortOrder, sourceQuestion);
        if (questionMapper.insert(question) != 1) {
          throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存竞赛理论执行题目快照失败");
        }
      }
      return snapshot;
    } catch (DuplicateKeyException ex) {
      throw new BusinessException(ErrorCode.CONFLICT, "竞赛理论执行快照版本已变化，请重试");
    }
  }

  private CompetitionTheoryPaperQuestionEntity toCompetitionQuestionEntity(
      PlatformUserPrincipal principal,
      Long snapshotId,
      int sortOrder,
      CompetitionMakeExamPaperQuestionEntity source) {
    String type = source.getQuestionType() == null
        ? "" : source.getQuestionType().trim().toLowerCase(Locale.ROOT);
    if (!List.of("single", "multi", "judge", "fill").contains(type)) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "竞赛理论快照包含不支持的题型");
    }
    CompetitionTheoryPaperQuestionEntity question = new CompetitionTheoryPaperQuestionEntity();
    question.setTenantId(principal.getTenantId());
    question.setPaperSnapshotId(snapshotId);
    question.setSourceQuestionId(source.getSourceQuestionId());
    question.setSortOrder(sortOrder);
    question.setQuestionType(type);
    question.setContent(source.getStem());
    String executionOptionsJson = toExecutionOptionsJson(type, source.getOptionsJson());
    question.setOptionsJson(executionOptionsJson);
    question.setCorrectAnswersJson(toExecutionAnswersJson(type, source, executionOptionsJson));
    question.setFillAnswer("fill".equals(type) ? firstNonBlank(
        source.getFillAnswer(), source.getReferenceAnswer()) : null);
    question.setScore(source.getScore());
    question.setResolveContent(null);
    question.setDifficulty(source.getDifficulty());
    question.setScoringRuleCode(EXACT_MATCH);
    question.setScoringRuleVersion(1);
    question.setCreatedBy(principal.getUserId());
    question.setUpdatedBy(principal.getUserId());
    question.setDeleted(0);
    return question;
  }

  private String toExecutionOptionsJson(String optionsJson) {
    return toExecutionOptionsJson(null, optionsJson);
  }

  /**
   * Converts the make-exam option payload to the execution snapshot shape.
   *
   * <p>Questions created in the independent competition question bank store choice options as an
   * object and judge answers as T/F without storing an option object.  The execution tables,
   * however, require an array of keyed options.  Canonicalising both object and legacy array
   * payloads here keeps the answer keys and the candidate-facing option IDs in sync.</p>
   */
  private String toExecutionOptionsJson(String questionType, String optionsJson) {
    try {
      ArrayNode options = objectMapper.createArrayNode();
      if (StringUtils.hasText(optionsJson)) {
        JsonNode root = objectMapper.readTree(optionsJson);
        if (root != null && root.isObject()) {
          root.fields().forEachRemaining(entry -> {
            String key = normalizeExecutionOptionKey(entry.getKey());
            if (!StringUtils.hasText(key)) {
              return;
            }
            ObjectNode option = objectMapper.createObjectNode();
            option.put("key", key);
            option.put("content", optionContent(entry.getValue()));
            options.add(option);
          });
        } else if (root != null && root.isArray()) {
          int index = 0;
          Set<String> usedKeys = new java.util.HashSet<>();
          for (JsonNode node : root) {
            if (node == null || node.isNull()) {
              continue;
            }
            String key = normalizeExecutionOptionKey(existingOptionKey(node));
            if (!StringUtils.hasText(key) || !usedKeys.add(key)) {
              key = generatedOptionKey(index, usedKeys);
            }
            usedKeys.add(key);
            ObjectNode option = objectMapper.createObjectNode();
            option.put("key", key);
            option.put("content", optionContent(node));
            options.add(option);
            index++;
          }
        }
      }
      if (options.isEmpty() && "judge".equalsIgnoreCase(questionType)) {
        addDefaultJudgeOptions(options);
      }
      return objectMapper.writeValueAsString(options);
    } catch (JsonProcessingException | IllegalArgumentException ex) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "竞赛理论快照选项格式异常");
    }
  }

  private void addDefaultJudgeOptions(ArrayNode options) {
    ObjectNode truth = objectMapper.createObjectNode();
    truth.put("key", "T");
    truth.put("content", "正确");
    options.add(truth);
    ObjectNode falsity = objectMapper.createObjectNode();
    falsity.put("key", "F");
    falsity.put("content", "错误");
    options.add(falsity);
  }

  private String existingOptionKey(JsonNode node) {
    if (node == null || !node.isObject()) {
      return null;
    }
    for (String field : List.of("key", "optionKey", "optionId", "id")) {
      String value = node.path(field).asText(null);
      if (StringUtils.hasText(value)) {
        return value.trim();
      }
    }
    return null;
  }

  private String normalizeExecutionOptionKey(String key) {
    if (!StringUtils.hasText(key)) {
      return null;
    }
    String trimmed = key.trim();
    return trimmed.matches("[a-hA-H]") ? trimmed.toUpperCase(Locale.ROOT) : trimmed;
  }

  private String generatedOptionKey(int index, Set<String> usedKeys) {
    String candidate = index < 26 ? String.valueOf((char) ('A' + index)) : "i" + index;
    while (usedKeys.contains(candidate)) {
      index++;
      candidate = index < 26 ? String.valueOf((char) ('A' + index)) : "i" + index;
    }
    return candidate;
  }

  private String optionContent(JsonNode node) {
    if (node == null || node.isNull()) {
      return "";
    }
    if (!node.isObject()) {
      return node.asText("");
    }
    for (String field : List.of("content", "text", "label", "value")) {
      JsonNode value = node.get(field);
      if (value != null && !value.isNull()) {
        return value.asText("");
      }
    }
    return "";
  }

  private String toExecutionAnswersJson(
      String type, CompetitionMakeExamPaperQuestionEntity source, String executionOptionsJson) {
    String answer = "fill".equals(type)
        ? firstNonBlank(source.getFillAnswer(), source.getReferenceAnswer())
        : firstNonBlank(source.getReferenceAnswer(), source.getFillAnswer());
    if (answer == null) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "竞赛理论快照缺少参考答案");
    }
    ArrayNode answers = objectMapper.createArrayNode();
    if ("fill".equals(type)) {
      answers.add(answer);
    } else if ("judge".equals(type)) {
      String normalized = answer.trim().toLowerCase(Locale.ROOT);
      JsonNode options = readExecutionOptions(executionOptionsJson);
      String key = null;
      if (normalized.matches("[a-h]")) {
        key = normalized.toUpperCase(Locale.ROOT);
        if (!containsOptionKey(options, key)) {
          throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "竞赛判断题答案选项不存在");
        }
      } else if (normalized.matches("[tf]")
          && containsOptionKey(options, normalized.toUpperCase(Locale.ROOT))) {
        // Preserve an explicitly keyed T/F option set when one was supplied by an importer.
        key = normalized.toUpperCase(Locale.ROOT);
      } else {
        key = judgeOptionKey(options, normalized);
      }
      if (key == null) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "竞赛判断题答案无法匹配选项");
      }
      answers.add(key);
    } else {
      String letters = answer.toUpperCase(Locale.ROOT).replaceAll("[^A-H]", "");
      if (letters.isBlank()) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "竞赛选择题参考答案格式异常");
      }
      Set<String> selected = new LinkedHashSet<>();
      for (int index = 0; index < letters.length(); index++) {
        String key = String.valueOf(letters.charAt(index));
        if (!containsJsonOption(executionOptionsJson, key)) {
          throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "竞赛选择题答案选项不存在");
        }
        if (selected.add(key)) {
          answers.add(key);
        }
      }
    }
    try {
      return objectMapper.writeValueAsString(answers);
    } catch (JsonProcessingException ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "序列化竞赛理论答案快照失败");
    }
  }

  private JsonNode readExecutionOptions(String optionsJson) {
    try {
      return objectMapper.readTree(optionsJson);
    } catch (JsonProcessingException | IllegalArgumentException ex) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "竞赛判断题选项格式异常");
    }
  }

  private boolean containsOptionKey(JsonNode options, String key) {
    if (options == null || !options.isArray() || !StringUtils.hasText(key)) {
      return false;
    }
    for (JsonNode option : options) {
      if (key.equalsIgnoreCase(option.path("key").asText(""))) {
        return true;
      }
    }
    return false;
  }

  private String judgeOptionKey(JsonNode options, String normalizedAnswer) {
    Boolean expected = judgeValue(normalizedAnswer);
    if (expected == null || options == null || !options.isArray()) {
      return null;
    }
    for (JsonNode option : options) {
      Boolean actual = judgeValue(option.path("content").asText(""));
      if (expected.equals(actual)) {
        String key = option.path("key").asText(null);
        if (StringUtils.hasText(key)) {
          return key.trim();
        }
      }
    }
    return null;
  }

  private Boolean judgeValue(String value) {
    String normalized = value == null
        ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    if (!StringUtils.hasText(normalized)) {
      return null;
    }
    if (JUDGE_FALSE_TOKENS.contains(normalized)
        || normalized.contains("错误")
        || normalized.contains("不正确")
        || normalized.contains("不对")) {
      return Boolean.FALSE;
    }
    if (JUDGE_TRUE_TOKENS.contains(normalized)
        || normalized.contains("正确")
        || "对".equals(normalized)
        || "是".equals(normalized)) {
      return Boolean.TRUE;
    }
    return null;
  }

  private boolean containsJsonOption(String optionsJson, String key) {
    try {
      JsonNode root = objectMapper.readTree(toExecutionOptionsJson(optionsJson));
      return containsOptionKey(root, key);
    } catch (JsonProcessingException | IllegalArgumentException ex) {
      return false;
    }
  }

  private String firstNonBlank(String first, String second) {
    if (StringUtils.hasText(first)) return first.trim();
    return StringUtils.hasText(second) ? second.trim() : null;
  }

  private CompetitionTheoryPaperSnapshotEntity persistSnapshot(
      PlatformUserPrincipal principal,
      CompetitionScoreRuleEntity rule,
      String paperCode,
      PreparedPaper prepared,
      LocalDateTime now) {
    Integer maxVersion = snapshotMapper.selectMaxVersion(
        principal.getTenantId(),
        rule.getEventId(),
        rule.getStageId(),
        rule.getEventWorkTypeId(),
        rule.getId(),
        paperCode);
    snapshotMapper.deprecatePublished(
        principal.getTenantId(),
        rule.getEventId(),
        rule.getStageId(),
        rule.getEventWorkTypeId(),
        rule.getId(),
        paperCode,
        principal.getUserId());
    CompetitionTheoryPaperSnapshotEntity snapshot =
        new CompetitionTheoryPaperSnapshotEntity();
    snapshot.setTenantId(principal.getTenantId());
    snapshot.setEventId(rule.getEventId());
    snapshot.setStageId(rule.getStageId());
    snapshot.setEventWorkTypeId(rule.getEventWorkTypeId());
    snapshot.setScoreRuleId(rule.getId());
    // 课程组卷计划已经携带时长、分值和题型要求，竞赛侧不再重复创建理论要求。
    snapshot.setRequirementId(null);
    snapshot.setPaperCode(paperCode);
    snapshot.setVersionNo(maxVersion == null ? 1 : maxVersion + 1);
    snapshot.setSourceCourseId(prepared.source().courseId());
    snapshot.setSourcePlanId(prepared.source().planId());
    snapshot.setSourcePaperId(prepared.source().paperId());
    snapshot.setSourceTitle(resolveTitle(prepared.source().title(), paperCode));
    snapshot.setSourceHash(prepared.sourceHash());
    snapshot.setDurationMinutes(prepared.source().durationMinutes());
    snapshot.setQuestionCount(prepared.questions().size());
    snapshot.setTotalScore(prepared.source().totalScore());
    snapshot.setPassScore(prepared.source().passScore());
    snapshot.setStatus(PUBLISHED);
    snapshot.setImportedAt(now);
    snapshot.setImportedBy(principal.getUserId());
    snapshot.setPublishedAt(now);
    snapshot.setPublishedBy(principal.getUserId());
    snapshot.setCreatedBy(principal.getUserId());
    snapshot.setUpdatedBy(principal.getUserId());
    snapshot.setDeleted(0);
    try {
      if (snapshotMapper.insert(snapshot) != 1) {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建竞赛试卷快照失败");
      }
      int sortOrder = 0;
      for (PreparedQuestion preparedQuestion : prepared.questions()) {
        sortOrder++;
        CompetitionTheoryPaperQuestionEntity question =
            toQuestionEntity(principal, snapshot.getId(), sortOrder, preparedQuestion);
        if (questionMapper.insert(question) != 1) {
          throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存竞赛试卷题目快照失败");
        }
      }
      return snapshot;
    } catch (DuplicateKeyException ex) {
      throw new BusinessException(ErrorCode.CONFLICT, "竞赛试卷版本已变化，请重试");
    }
  }

  private CompetitionTheoryPaperQuestionEntity toQuestionEntity(
      PlatformUserPrincipal principal,
      Long snapshotId,
      int sortOrder,
      PreparedQuestion prepared) {
    SourceQuestion source = prepared.source();
    CompetitionTheoryPaperQuestionEntity entity =
        new CompetitionTheoryPaperQuestionEntity();
    entity.setTenantId(principal.getTenantId());
    entity.setPaperSnapshotId(snapshotId);
    entity.setSourceQuestionId(source.sourceQuestionId());
    entity.setSortOrder(sortOrder);
    entity.setQuestionType(prepared.answer().normalizedType());
    entity.setContent(source.content());
    entity.setOptionsJson(prepared.answer().optionsJson());
    entity.setCorrectAnswersJson(prepared.answer().correctAnswersJson());
    entity.setScore(source.score());
    entity.setResolveContent(source.resolveContent());
    entity.setDifficulty(source.difficulty());
    entity.setScoringRuleCode(EXACT_MATCH);
    entity.setScoringRuleVersion(1);
    entity.setSourceOrgId(source.sourceOrgId());
    entity.setSourceOrgName(source.sourceOrgName());
    entity.setSourceAuthorId(source.sourceAuthorId());
    entity.setSourceUpdatedAt(source.sourceUpdatedAt());
    entity.setCreatedBy(principal.getUserId());
    entity.setUpdatedBy(principal.getUserId());
    entity.setDeleted(0);
    return entity;
  }

  private PreparedPaper preparePaper(SourcePaper source) {
    List<PreparedQuestion> questions = new ArrayList<>();
    for (SourceQuestion sourceQuestion : source.questions()) {
      ParsedAnswer parsed = answerAdapter.parse(
          sourceQuestion.type(), sourceQuestion.content(), sourceQuestion.answerJson());
      switch (parsed.normalizedType()) {
        case "single", "multi", "judge" -> {
          // 题型、分值和总分以课程试卷为准，不再与竞赛侧理论要求二次比对。
        }
        default -> throw new IllegalStateException("Unexpected objective question type");
      }
      questions.add(new PreparedQuestion(sourceQuestion, parsed));
    }
    return new PreparedPaper(source, questions, sourceHash(source, questions));
  }

  private void requireEquivalent(PreparedPaper first, PreparedPaper second) {
    requirePairEqual(first.source().durationMinutes(), second.source().durationMinutes(), "A/B考试时长");
    requirePairDecimal(first.source().totalScore(), second.source().totalScore(), "A/B总分");
    requirePairDecimal(first.source().passScore(), second.source().passScore(), "A/B及格分");
    requirePairEqual(first.questions().size(), second.questions().size(), "A/B题目总数");
  }

  private String sourceHash(SourcePaper source, List<PreparedQuestion> questions) {
    StringBuilder value = new StringBuilder()
        .append(source.paperId()).append('|')
        .append(source.planId()).append('|')
        .append(source.courseId()).append('|')
        .append(source.durationMinutes()).append('|')
        .append(source.totalScore()).append('|')
        .append(source.passScore());
    for (PreparedQuestion question : questions) {
      value.append('\n')
          .append(question.source().sourceQuestionId()).append('|')
          .append(question.answer().normalizedType()).append('|')
          .append(question.source().content()).append('|')
          .append(question.answer().optionsJson()).append('|')
          .append(question.answer().correctAnswersJson()).append('|')
          .append(question.source().score());
    }
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(value.toString().getBytes(StandardCharsets.UTF_8));
      StringBuilder hash = new StringBuilder();
      for (byte current : digest) {
        hash.append(String.format("%02x", current));
      }
      return hash.toString();
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is unavailable", ex);
    }
  }

  private void validateRequirement(
      CompetitionScoreRuleEntity rule, CompetitionTheoryRequirementUpsertReq req) {
    String template = req.templateCode().trim().toUpperCase(Locale.ROOT);
    if (!CompetitionTheoryTemplate.supports(template)) {
      throw new BusinessException(
          ErrorCode.UNPROCESSABLE_ENTITY, "理论模板仅支持THEORY_90、THEORY_120或CUSTOM");
    }
    if (!template.equals(rule.getTheoryTemplate())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "理论要求模板必须与评分规则一致");
    }
    if (req.singleCount() + req.multiCount() + req.judgeCount() <= 0) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "理论考试题目总数必须大于0");
    }
    requirePositiveScore(req.singleCount(), req.singleScore(), "单选题");
    requirePositiveScore(req.multiCount(), req.multiScore(), "多选题");
    requirePositiveScore(req.judgeCount(), req.judgeScore(), "判断题");
    BigDecimal calculated = req.singleScore().multiply(BigDecimal.valueOf(req.singleCount()))
        .add(req.multiScore().multiply(BigDecimal.valueOf(req.multiCount())))
        .add(req.judgeScore().multiply(BigDecimal.valueOf(req.judgeCount())));
    requireDecimal(req.totalScore(), calculated, "理论考试总分");
    if (req.passScore().compareTo(req.totalScore()) > 0) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "及格分不能高于总分");
    }
  }

  private void applyRequirement(
      CompetitionTheoryRequirementEntity entity, CompetitionTheoryRequirementUpsertReq req) {
    entity.setTemplateCode(req.templateCode().trim().toUpperCase(Locale.ROOT));
    entity.setDurationMinutes(req.durationMinutes());
    entity.setSingleCount(req.singleCount());
    entity.setSingleScore(req.singleScore());
    entity.setMultiCount(req.multiCount());
    entity.setMultiScore(req.multiScore());
    entity.setJudgeCount(req.judgeCount());
    entity.setJudgeScore(req.judgeScore());
    entity.setTotalScore(req.totalScore());
    entity.setPassScore(req.passScore());
  }

  private CompetitionScoreRuleEntity resolveImportScope(
      PlatformUserPrincipal principal,
      Long eventId,
      CompetitionTheoryPaperImportReq req) {
    CompetitionStageEntity stage =
        stageService.getById(principal.getTenantId(), req.stageId());
    if (!Objects.equals(eventId, stage.getEventId())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "Competition stage does not belong to the current event");
    }
    requireEventWorkType(principal.getTenantId(), eventId, req.eventWorkTypeId());
    CompetitionScoreRuleEntity scope = new CompetitionScoreRuleEntity();
    scope.setEventId(eventId);
    scope.setStageId(req.stageId());
    scope.setEventWorkTypeId(req.eventWorkTypeId());
    scope.setStatus(PUBLISHED);
    return scope;
  }

  private CompetitionEventWorkTypeEntity requireEventWorkType(
      Long tenantId, Long eventId, Long eventWorkTypeId) {
    CompetitionEventWorkTypeEntity eventWorkType =
        eventWorkTypeDomainService.getById(tenantId, eventWorkTypeId);
    if (!Objects.equals(eventId, eventWorkType.getEventId())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "赛事工种不属于当前赛事");
    }
    return eventWorkType;
  }

  private CompetitionTheoryRequirementEntity findRequirement(Long tenantId, Long ruleId) {
    return requirementMapper.selectOne(
        new LambdaQueryWrapper<CompetitionTheoryRequirementEntity>()
            .eq(CompetitionTheoryRequirementEntity::getTenantId, tenantId)
            .eq(CompetitionTheoryRequirementEntity::getScoreRuleId, ruleId)
            .eq(CompetitionTheoryRequirementEntity::getDeleted, 0));
  }

  private void requirePositiveScore(Integer count, BigDecimal score, String name) {
    if (count > 0 && score.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, name + "单题分值必须大于0");
    }
  }

  private void requireEqual(Object expected, Object actual, String name) {
    if (!Objects.equals(expected, actual)) {
      throw new BusinessException(
          ErrorCode.UNPROCESSABLE_ENTITY, name + "不符合竞赛理论考试要求");
    }
  }

  private void requireDecimal(BigDecimal expected, BigDecimal actual, String name) {
    if (expected == null || actual == null || expected.compareTo(actual) != 0) {
      throw new BusinessException(
          ErrorCode.UNPROCESSABLE_ENTITY, name + "不符合竞赛理论考试要求");
    }
  }

  private void requirePairEqual(Object expected, Object actual, String name) {
    if (!Objects.equals(expected, actual)) {
      throw new BusinessException(
          ErrorCode.UNPROCESSABLE_ENTITY, name + "不一致");
    }
  }

  private void requirePairDecimal(BigDecimal expected, BigDecimal actual, String name) {
    if (expected == null || actual == null || expected.compareTo(actual) != 0) {
      throw new BusinessException(
          ErrorCode.UNPROCESSABLE_ENTITY, name + "不一致");
    }
  }

  private String resolveTitle(String sourceTitle, String paperCode) {
    return StringUtils.hasText(sourceTitle) ? sourceTitle.trim() : paperCode + "卷";
  }

  private PlatformUserPrincipal currentUser() {
    try {
      return SecurityContext.currentUser();
    } catch (IllegalStateException ex) {
      log.warn("Competition theory paper request has no authenticated platform user");
      throw ex;
    }
  }

  private record PreparedPaper(
      SourcePaper source,
      List<PreparedQuestion> questions,
      String sourceHash
  ) {
  }

  private record PreparedQuestion(
      SourceQuestion source,
      ParsedAnswer answer
  ) {
  }
}
