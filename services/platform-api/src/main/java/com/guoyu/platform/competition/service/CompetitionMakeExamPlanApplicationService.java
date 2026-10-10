package com.guoyu.platform.competition.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.dto.CompetitionMakeExamPlanDetailReq;
import com.guoyu.platform.competition.dto.CompetitionMakeExamPlanQueryReq;
import com.guoyu.platform.competition.dto.CompetitionMakeExamPlanUpsertReq;
import com.guoyu.platform.competition.dto.CompetitionMakeExamPlanTopicRuleReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPaperQuestionEntity;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPaperSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPlanDetailEntity;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPlanEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalQuestionDetailEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalQuestionScoreItemEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalQuestionAssessmentContentEntity;
import com.guoyu.platform.competition.entity.CompetitionQuestionEntity;
import com.guoyu.platform.competition.entity.CompetitionQuestionBlueprintEntity;
import com.guoyu.platform.competition.entity.CompetitionQuestionBlueprintNodeEntity;
import com.guoyu.platform.competition.entity.CompetitionQuestionBlueprintBindingEntity;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPlanTopicRuleEntity;
import com.guoyu.platform.competition.domain.CompetitionPracticalMakeExamAllocator;
import com.guoyu.platform.competition.domain.CompetitionTheoryMakeExamAllocator;
import com.guoyu.platform.competition.mapper.CompetitionEventMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeLevelMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
import com.guoyu.platform.competition.mapper.CompetitionMakeExamPaperQuestionMapper;
import com.guoyu.platform.competition.mapper.CompetitionMakeExamPaperSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionMakeExamPlanDetailMapper;
import com.guoyu.platform.competition.mapper.CompetitionMakeExamPlanMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalQuestionDetailMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalQuestionScoreItemMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalQuestionAssessmentContentMapper;
import com.guoyu.platform.competition.mapper.CompetitionQuestionMapper;
import com.guoyu.platform.competition.mapper.CompetitionQuestionBlueprintBindingMapper;
import com.guoyu.platform.competition.mapper.CompetitionQuestionBlueprintNodeMapper;
import com.guoyu.platform.competition.mapper.CompetitionMakeExamPlanTopicRuleMapper;
import com.guoyu.platform.competition.vo.CompetitionMakeExamPlanDetailVO;
import com.guoyu.platform.competition.vo.CompetitionMakeExamPlanVO;
import com.guoyu.platform.competition.vo.CompetitionMakeExamPlanTopicRuleVO;
import com.guoyu.platform.competition.vo.CompetitionMakeExamSnapshotQuestionVO;
import com.guoyu.platform.competition.vo.CompetitionMakeExamSnapshotVO;
import com.guoyu.platform.competition.vo.CompetitionMakeExamPlanPrecheckVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

@Service
@Slf4j
public class CompetitionMakeExamPlanApplicationService {

  private static final String PURPOSE_EXAM_PLAN = "EXAM_PLAN";
  private static final String PURPOSE_PRACTICAL_SCORING_SCHEME = "PRACTICAL_SCORING_SCHEME";

  private final CompetitionMakeExamPlanMapper planMapper;
  private final CompetitionMakeExamPlanDetailMapper detailMapper;
  private final CompetitionMakeExamPaperSnapshotMapper snapshotMapper;
  private final CompetitionMakeExamPaperQuestionMapper snapshotQuestionMapper;
  private final CompetitionQuestionMapper questionMapper;
  private final CompetitionPracticalQuestionDetailMapper practicalDetailMapper;
  private final CompetitionPracticalQuestionScoreItemMapper scoreItemMapper;
  private final CompetitionPracticalQuestionAssessmentContentMapper assessmentContentMapper;
  private final CompetitionEventMapper eventMapper;
  private final CompetitionEventWorkTypeMapper workTypeMapper;
  private final CompetitionEventWorkTypeLevelMapper levelMapper;
  private final CompetitionQuestionBlueprintApplicationService blueprintService;
  private final CompetitionQuestionBlueprintNodeMapper blueprintNodeMapper;
  private final CompetitionQuestionBlueprintBindingMapper blueprintBindingMapper;
  private final CompetitionMakeExamPlanTopicRuleMapper topicRuleMapper;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final CompetitionTheoryMakeExamAllocator theoryAllocator = new CompetitionTheoryMakeExamAllocator();
  private final CompetitionPracticalMakeExamAllocator practicalAllocator = new CompetitionPracticalMakeExamAllocator();

  public CompetitionMakeExamPlanApplicationService(
      CompetitionMakeExamPlanMapper planMapper,
      CompetitionMakeExamPlanDetailMapper detailMapper,
      CompetitionMakeExamPaperSnapshotMapper snapshotMapper,
      CompetitionMakeExamPaperQuestionMapper snapshotQuestionMapper,
      CompetitionQuestionMapper questionMapper,
      CompetitionPracticalQuestionDetailMapper practicalDetailMapper,
      CompetitionPracticalQuestionScoreItemMapper scoreItemMapper,
      CompetitionEventMapper eventMapper,
      CompetitionEventWorkTypeMapper workTypeMapper,
      CompetitionEventWorkTypeLevelMapper levelMapper) {
    this(planMapper, detailMapper, snapshotMapper, snapshotQuestionMapper, questionMapper,
        practicalDetailMapper, scoreItemMapper, null, eventMapper, workTypeMapper, levelMapper,
        null, null, null, null);
  }

  @Autowired
  public CompetitionMakeExamPlanApplicationService(
      CompetitionMakeExamPlanMapper planMapper,
      CompetitionMakeExamPlanDetailMapper detailMapper,
      CompetitionMakeExamPaperSnapshotMapper snapshotMapper,
      CompetitionMakeExamPaperQuestionMapper snapshotQuestionMapper,
      CompetitionQuestionMapper questionMapper,
      CompetitionPracticalQuestionDetailMapper practicalDetailMapper,
      CompetitionPracticalQuestionScoreItemMapper scoreItemMapper,
      CompetitionPracticalQuestionAssessmentContentMapper assessmentContentMapper,
      CompetitionEventMapper eventMapper,
      CompetitionEventWorkTypeMapper workTypeMapper,
      CompetitionEventWorkTypeLevelMapper levelMapper,
      CompetitionQuestionBlueprintApplicationService blueprintService,
      CompetitionQuestionBlueprintNodeMapper blueprintNodeMapper,
      CompetitionQuestionBlueprintBindingMapper blueprintBindingMapper,
      CompetitionMakeExamPlanTopicRuleMapper topicRuleMapper) {
    this.planMapper = planMapper;
    this.detailMapper = detailMapper;
    this.snapshotMapper = snapshotMapper;
    this.snapshotQuestionMapper = snapshotQuestionMapper;
    this.questionMapper = questionMapper;
    this.practicalDetailMapper = practicalDetailMapper;
    this.scoreItemMapper = scoreItemMapper;
    this.assessmentContentMapper = assessmentContentMapper;
    this.eventMapper = eventMapper;
    this.workTypeMapper = workTypeMapper;
    this.levelMapper = levelMapper;
    this.blueprintService = blueprintService;
    this.blueprintNodeMapper = blueprintNodeMapper;
    this.blueprintBindingMapper = blueprintBindingMapper;
    this.topicRuleMapper = topicRuleMapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<CompetitionMakeExamPlanVO> page(Long eventId, CompetitionMakeExamPlanQueryReq req) {
    PlatformUserPrincipal principal = currentUser();
    requireEvent(principal.getTenantId(), eventId);
    LambdaQueryWrapper<CompetitionMakeExamPlanEntity> wrapper = new LambdaQueryWrapper<CompetitionMakeExamPlanEntity>()
        .eq(CompetitionMakeExamPlanEntity::getTenantId, principal.getTenantId())
        .eq(CompetitionMakeExamPlanEntity::getEventId, eventId)
        .eq(req.getEventWorkTypeId() != null, CompetitionMakeExamPlanEntity::getEventWorkTypeId, req.getEventWorkTypeId())
        .eq(req.getEventWorkTypeLevelId() != null, CompetitionMakeExamPlanEntity::getEventWorkTypeLevelId, req.getEventWorkTypeLevelId())
        .eq(StringUtils.hasText(req.getContentType()), CompetitionMakeExamPlanEntity::getContentType, req.getContentType())
        .eq(StringUtils.hasText(req.getStatus()), CompetitionMakeExamPlanEntity::getStatus, req.getStatus())
        .like(StringUtils.hasText(req.getKeyword()), CompetitionMakeExamPlanEntity::getTitle, req.getKeyword())
        .orderByDesc(CompetitionMakeExamPlanEntity::getUpdatedAt)
        .orderByDesc(CompetitionMakeExamPlanEntity::getId);
    if (Boolean.TRUE.equals(req.getSimpleMode())) {
      wrapper.isNull(CompetitionMakeExamPlanEntity::getBlueprintId);
      if ("practical".equalsIgnoreCase(req.getContentType())) {
        wrapper.isNull(CompetitionMakeExamPlanEntity::getPracticalQuestionId);
        wrapper.eq(CompetitionMakeExamPlanEntity::getPlanPurpose, PURPOSE_PRACTICAL_SCORING_SCHEME);
      }
    } else if (Boolean.FALSE.equals(req.getSimpleMode())) {
      wrapper.and(condition -> condition.isNotNull(CompetitionMakeExamPlanEntity::getBlueprintId)
          .or().isNotNull(CompetitionMakeExamPlanEntity::getPracticalQuestionId));
    }
    Page<CompetitionMakeExamPlanEntity> result = planMapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    if (result == null) {
      log.warn("Competition make-plan mapper returned null page, tenantId={}, eventId={}",
          principal.getTenantId(), eventId);
      result = new Page<>(req.getPage(), req.getPageSize());
    }
    List<CompetitionMakeExamPlanVO> items = new ArrayList<>();
    if (result.getRecords() != null) {
      for (CompetitionMakeExamPlanEntity entity : result.getRecords()) items.add(toPlanVO(principal.getTenantId(), entity));
    }
    return new PageResponse<>(items, result.getCurrent(), result.getSize(), result.getTotal());
  }

  @Transactional(readOnly = true)
  public CompetitionMakeExamPlanVO get(Long eventId, Long planId) {
    PlatformUserPrincipal principal = currentUser();
    return toPlanVO(principal.getTenantId(), findPlan(principal.getTenantId(), eventId, planId));
  }

  @Transactional(readOnly = true)
  public CompetitionMakeExamPlanPrecheckVO precheck(Long eventId, CompetitionMakeExamPlanUpsertReq req) {
    PlatformUserPrincipal principal = currentUser();
    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();
    try {
      // A precheck may be used while editing a historical draft. Its blueprint is
      // already locked by the draft, so it is valid even after a newer version was
      // activated for the same event scope.
      validatePlan(principal.getTenantId(), eventId, req, true);
    } catch (BusinessException ex) {
      errors.add(ex.getMessage());
    }
    if (isPracticalScoringSchemeRequest(req)) {
      int selectedCount = req.practicalQuestionIds() == null ? 0 : req.practicalQuestionIds().size();
      return new CompetitionMakeExamPlanPrecheckVO(errors.isEmpty(), selectedCount,
          List.of(), List.of(), errors, List.of(), 0, List.of(), List.of(), selectedCount,
          req.durationMinutes());
    }
    CompetitionMakeExamPlanEntity probe = new CompetitionMakeExamPlanEntity();
    probe.setEventId(eventId); probe.setEventWorkTypeId(req.eventWorkTypeId()); probe.setEventWorkTypeLevelId(req.eventWorkTypeLevelId());
    probe.setContentType(normalizeContentType(req.contentType())); probe.setBlueprintId(req.blueprintId());
    List<CompetitionQuestionEntity> candidates = sortCandidateQuestions(
        questionMapper.selectList(questionScope(principal.getTenantId(), probe)));
    List<CompetitionMakeExamPlanPrecheckVO.TopicItem> topics = new ArrayList<>();
    List<CompetitionMakeExamPlanPrecheckVO.TheoryAllocationItem> theoryAllocations = new ArrayList<>();
    Integer unboundQuestionCount = 0;
    String contentType = probe.getContentType();
    boolean simplePractical = "practical".equals(contentType)
        && req.blueprintId() == null && req.practicalQuestionId() == null;
    if (simplePractical && req.details() != null && req.details().size() == 1
        && req.details().get(0) != null && req.details().get(0).point() != null) {
      int practicalCandidateCount = (int) candidates.stream()
          .filter(candidate -> candidate != null
              && "practical".equalsIgnoreCase(candidate.getQuestionType()))
          .count();
      candidates = validSimplePracticalCandidates(principal.getTenantId(), candidates,
          req.details().get(0).point());
      if (candidates.size() < practicalCandidateCount) {
        warnings.add("有" + (practicalCandidateCount - candidates.size())
            + "道实操题缺少可用于当前分值配置的有效评分项，已排除");
      }
    }
    if (req.blueprintId() != null && blueprintNodeMapper != null && "theory".equals(contentType)) {
      try {
        BlueprintTopology topology = loadTopology(principal.getTenantId(), req.blueprintId());
        Map<Long, CompetitionQuestionBlueprintBindingEntity> bindingMap = bindingByQuestion(
            principal.getTenantId(), req.blueprintId());
        List<CompetitionQuestionEntity> allScopeQuestions = questionMapper.selectList(
            questionScope(principal.getTenantId(), copyPlanWithoutBlueprint(probe)));
        allScopeQuestions = allScopeQuestions == null ? new ArrayList<>() : allScopeQuestions;
        List<CompetitionQuestionEntity> validCandidates = validTheoryCandidates(
            candidates, topology, bindingMap, req.blueprintId());
        unboundQuestionCount = countUnusableTheoryQuestions(allScopeQuestions, topology, bindingMap,
            req.blueprintId());
        candidates = validCandidates;
        CompetitionTheoryMakeExamAllocator.AllocationResult allocation = allocateTheory(
            topology, req.details(), candidates, bindingMap, 0L);
        errors.addAll(allocation.errors());
        warnings.addAll(allocation.warnings());
        for (CompetitionTheoryMakeExamAllocator.TopicAllocation topic : allocation.topics()) {
          List<CompetitionMakeExamPlanPrecheckVO.DetailAllocationItem> cells = new ArrayList<>();
          for (CompetitionTheoryMakeExamAllocator.CellAllocation cell : topic.details()) {
            cells.add(new CompetitionMakeExamPlanPrecheckVO.DetailAllocationItem(cell.ruleIndex(),
                cell.sortOrder(), cell.questionType(), cell.point(), cell.difficultyStart(),
                cell.difficultyEnd(), cell.required(), cell.available(), cell.assigned(), cell.shortage()));
          }
          topics.add(new CompetitionMakeExamPlanPrecheckVO.TopicItem(
              String.valueOf(topic.blueprintNodeId()), topic.path(), topic.weight(), topic.sortOrder(),
              topic.questionCount(), topic.available(), topic.shortage(), cells));
          theoryAllocations.add(new CompetitionMakeExamPlanPrecheckVO.TheoryAllocationItem(
              String.valueOf(topic.blueprintNodeId()), topic.path(), topic.weight(), topic.sortOrder(),
              topic.questionCount(), topic.available(), topic.assigned(), topic.shortage(), cells));
        }
        List<CompetitionMakeExamPlanPrecheckVO.DetailItem> detailItems = new ArrayList<>();
        for (CompetitionTheoryMakeExamAllocator.RuleAllocation detail : allocation.details()) {
          detailItems.add(new CompetitionMakeExamPlanPrecheckVO.DetailItem(detail.ruleIndex(), detail.sortOrder(),
              detail.questionType(), detail.required(), detail.available(), detail.shortage()));
        }
        if (unboundQuestionCount > 0) {
          warnings.add("当前赛事工种等级下有" + unboundQuestionCount
              + "道题未绑定到当前细目表的有效末级考评点，已排除");
        }
        return new CompetitionMakeExamPlanPrecheckVO(errors.isEmpty() && allocation.ready(),
            candidates.size(), topics, detailItems, errors, theoryAllocations, unboundQuestionCount, warnings);
      } catch (BusinessException ex) {
        errors.add(ex.getMessage());
      } catch (IllegalArgumentException ex) {
        errors.add(ex.getMessage() == null ? "理论组卷自动分配失败" : ex.getMessage());
      }
    }
    if (req.blueprintId() != null && blueprintNodeMapper != null && "practical".equals(contentType)) {
      try {
        BlueprintTopology topology = loadTopology(principal.getTenantId(), req.blueprintId());
        // Preview the same first paper that formal generation will create.  In
        // particular, do not use the un-mixed base seed here: generation mixes
        // the base seed with the paper sequence before calling the allocator.
        long baseSeed = req.randomSeed() == null ? practicalSeed(req) : req.randomSeed();
        int paperCount = req.paperCount() == null ? 1 : req.paperCount();
        if (paperCount <= 0 || paperCount > 50) paperCount = 1;
        List<CompetitionPracticalMakeExamAllocator.CandidateInput> candidateInputs =
            practicalCandidateInputs(principal.getTenantId(), candidates);
        appendPracticalCandidateWarnings(candidateInputs, warnings);
        Set<Long> usedAcrossPapers = new HashSet<>();
        CompetitionPracticalMakeExamAllocator.AllocationResult allocation = null;
        for (int paper = 1; paper <= paperCount; paper++) {
          CompetitionPracticalMakeExamAllocator.AllocationResult paperAllocation = allocatePractical(
              principal.getTenantId(), req.blueprintId(), topology, candidateInputs, req.totalScore(),
              req.durationMinutes(), paper, mixSeed(baseSeed, paper), usedAcrossPapers);
          if (allocation == null) allocation = paperAllocation;
          errors.addAll(paperAllocation.errors());
          warnings.addAll(paperAllocation.warnings());
          if (!paperAllocation.ready()) break;
          usedAcrossPapers.addAll(paperAllocation.selectedQuestionIds());
        }
        if (allocation == null) {
          allocation = new CompetitionPracticalMakeExamAllocator.AllocationResult(
              false, 0, req.totalScore(), 0, List.of(), List.of(), List.of("实操自动组卷预检未生成分配结果"));
        }
        List<CompetitionMakeExamPlanPrecheckVO.PracticalAllocationItem> practicalAllocations =
            toPracticalPrecheck(allocation);
        for (CompetitionMakeExamPlanPrecheckVO.PracticalAllocationItem item : practicalAllocations) {
          topics.add(new CompetitionMakeExamPlanPrecheckVO.TopicItem(item.blueprintNodeId(), item.path(),
              item.scoreWeight(), null, 1, item.candidateQuestionCount(),
              item.candidateQuestionCount() > 0 ? 0 : 1, List.of()));
        }
        return new CompetitionMakeExamPlanPrecheckVO(errors.isEmpty() && allocation.ready(),
            candidates.size(), topics, List.of(), errors, List.of(), 0, warnings,
            practicalAllocations, allocation.questionCount(), allocation.referenceDurationMinutes());
      } catch (BusinessException ex) {
        errors.add(ex.getMessage());
      } catch (IllegalArgumentException ex) {
        errors.add(ex.getMessage() == null ? "实操自动组卷预检失败" : ex.getMessage());
      }
      return new CompetitionMakeExamPlanPrecheckVO(false, candidates.size(), topics, List.of(), errors,
          List.of(), 0, warnings, List.of(), 0, 0);
    }
    List<CompetitionMakeExamPlanPrecheckVO.DetailItem> detailItems = new ArrayList<>();
    if (theoryAllocations.isEmpty() && req.details() != null) {
      Set<Long> usedQuestionIds = new HashSet<>();
      Random precheckRandom = new Random(effectiveRequestSeed(req));
      List<CompetitionMakeExamPlanDetailReq> orderedDetails = new ArrayList<>(req.details());
      orderedDetails.sort(Comparator.comparingInt(detail ->
          detail == null || detail.sortOrder() == null ? Integer.MAX_VALUE : detail.sortOrder()));
      for (CompetitionMakeExamPlanDetailReq detail : orderedDetails) {
        if (detail == null || detail.questionType() == null || detail.questionCount() == null) continue;
        List<CompetitionQuestionEntity> availableQuestions = new ArrayList<>();
        for (CompetitionQuestionEntity candidate : candidates) {
          if (candidate == null || candidate.getId() == null || usedQuestionIds.contains(candidate.getId())
              || !detail.questionType().equalsIgnoreCase(candidate.getQuestionType())
              || !matchesDifficulty(candidate, detail)) {
            continue;
          }
          availableQuestions.add(candidate);
        }
        Collections.shuffle(availableQuestions, precheckRandom);
        int available = availableQuestions.size();
        detailItems.add(new CompetitionMakeExamPlanPrecheckVO.DetailItem(detail.questionType(), detail.questionCount(), available,
            Math.max(0, detail.questionCount() - available)));
        for (int index = 0; index < Math.min(detail.questionCount(), available); index++) {
          usedQuestionIds.add(availableQuestions.get(index).getId());
        }
      }
    }
    if (unboundQuestionCount > 0) {
      warnings.add("当前赛事工种等级下有" + unboundQuestionCount + "道题未绑定到当前细目表的有效末级考评点，已排除");
    }
    boolean ready = errors.isEmpty()
        && topics.stream().allMatch(item -> item.shortage() == 0)
        && detailItems.stream().allMatch(item -> item.shortage() == 0);
    return new CompetitionMakeExamPlanPrecheckVO(ready, candidates.size(), topics, detailItems, errors,
        theoryAllocations, unboundQuestionCount, warnings);
  }

  @Transactional
  public CompetitionMakeExamPlanVO create(Long eventId, CompetitionMakeExamPlanUpsertReq req) {
    PlatformUserPrincipal principal = currentUser();
    validatePlan(principal.getTenantId(), eventId, req, false);
    CompetitionMakeExamPlanEntity plan = new CompetitionMakeExamPlanEntity();
    plan.setId(IdWorker.getId());
    plan.setTenantId(principal.getTenantId());
    plan.setEventId(eventId);
    plan.setEventWorkTypeId(req.eventWorkTypeId());
    plan.setEventWorkTypeLevelId(req.eventWorkTypeLevelId());
    plan.setBlueprintId(req.blueprintId());
    plan.setPracticalQuestionId(req.practicalQuestionId());
    String contentType = normalizeContentType(req.contentType());
    plan.setContentType(contentType);
    plan.setPlanPurpose(isPracticalScoringSchemeRequest(req)
        ? PURPOSE_PRACTICAL_SCORING_SCHEME : PURPOSE_EXAM_PLAN);
    plan.setTitle(req.title());
    plan.setExamType(req.examType());
    plan.setDurationMinutes(req.durationMinutes());
    plan.setTotalScore(req.totalScore());
    plan.setPassScore(req.passScore());
    plan.setPaperCount(normalizePaperCount(req, contentType));
    plan.setRandomSeed(effectiveRequestSeed(req));
    plan.setPlanVersion(nextPlanVersion(principal.getTenantId(), eventId,
        req.eventWorkTypeId(), req.eventWorkTypeLevelId(), contentType, plan.getPlanPurpose()));
    plan.setStatus("draft");
    plan.setComments(req.comments());
    plan.setCreatedBy(principal.getUserId());
    plan.setUpdatedBy(principal.getUserId());
    plan.setDeleted(0);
    if (planMapper.insert(plan) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "竞赛组卷计划保存失败");
    }
    if (!isPracticalScoringSchemeRequest(req) && !isBlueprintPractical(contentType, req.blueprintId())) {
      saveDetails(principal.getTenantId(), principal.getUserId(), plan.getId(), req.details());
    }
    generateDraftSnapshots(principal.getTenantId(), principal.getUserId(), plan, req.details(),
        req.practicalQuestionIds());
    return toPlanVO(principal.getTenantId(), plan);
  }

  @Transactional
  public CompetitionMakeExamPlanVO update(Long eventId, Long planId, CompetitionMakeExamPlanUpsertReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionMakeExamPlanEntity plan = findPlan(principal.getTenantId(), eventId, planId);
    if (!"draft".equals(plan.getStatus())) throw new BusinessException(ErrorCode.CONFLICT, "已发布组卷计划不可直接修改，请新建版本");
    CompetitionMakeExamPlanUpsertReq effectiveReq = lockDraftBlueprint(plan, req);
    boolean existingScoringScheme = PURPOSE_PRACTICAL_SCORING_SCHEME.equals(plan.getPlanPurpose());
    if (existingScoringScheme != isPracticalScoringSchemeRequest(effectiveReq)) {
      throw new BusinessException(ErrorCode.CONFLICT, "评分方案和普通组卷计划不能互相转换，请新建对应类型的计划");
    }
    validatePlan(principal.getTenantId(), eventId, effectiveReq, true);
    plan.setEventWorkTypeId(effectiveReq.eventWorkTypeId());
    plan.setEventWorkTypeLevelId(effectiveReq.eventWorkTypeLevelId());
    plan.setBlueprintId(effectiveReq.blueprintId());
    plan.setPracticalQuestionId(effectiveReq.practicalQuestionId());
    String contentType = normalizeContentType(effectiveReq.contentType());
    plan.setContentType(contentType);
    plan.setPlanPurpose(existingScoringScheme ? PURPOSE_PRACTICAL_SCORING_SCHEME : PURPOSE_EXAM_PLAN);
    plan.setTitle(effectiveReq.title());
    plan.setExamType(effectiveReq.examType());
    plan.setDurationMinutes(effectiveReq.durationMinutes());
    plan.setTotalScore(effectiveReq.totalScore());
    plan.setPassScore(effectiveReq.passScore());
    plan.setPaperCount(normalizePaperCount(effectiveReq, contentType));
    plan.setRandomSeed(effectiveRequestSeed(effectiveReq));
    plan.setPlanVersion(nextPlanVersion(principal.getTenantId(), eventId,
        req.eventWorkTypeId(), req.eventWorkTypeLevelId(), contentType, plan.getPlanPurpose()));
    plan.setComments(effectiveReq.comments());
    plan.setUpdatedBy(principal.getUserId());
    if (planMapper.updateById(plan) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "竞赛组卷计划已发生变化，请刷新后重试");
    }
    detailMapper.delete(new LambdaQueryWrapper<CompetitionMakeExamPlanDetailEntity>()
        .eq(CompetitionMakeExamPlanDetailEntity::getTenantId, principal.getTenantId())
        .eq(CompetitionMakeExamPlanDetailEntity::getPlanId, planId));
    if (topicRuleMapper != null) {
      topicRuleMapper.delete(new LambdaQueryWrapper<CompetitionMakeExamPlanTopicRuleEntity>()
          .eq(CompetitionMakeExamPlanTopicRuleEntity::getTenantId, principal.getTenantId())
          .eq(CompetitionMakeExamPlanTopicRuleEntity::getPlanId, planId));
    }
    snapshotMapper.update(null, new LambdaUpdateWrapper<CompetitionMakeExamPaperSnapshotEntity>()
        .eq(CompetitionMakeExamPaperSnapshotEntity::getTenantId, principal.getTenantId())
        .eq(CompetitionMakeExamPaperSnapshotEntity::getPlanId, planId)
        .eq(CompetitionMakeExamPaperSnapshotEntity::getStatus, "draft")
        .set(CompetitionMakeExamPaperSnapshotEntity::getStatus, "deprecated")
        .set(CompetitionMakeExamPaperSnapshotEntity::getUpdatedBy, principal.getUserId())
        .set(CompetitionMakeExamPaperSnapshotEntity::getUpdatedAt, LocalDateTime.now()));
    if (!isPracticalScoringSchemeRequest(effectiveReq)
        && !isBlueprintPractical(contentType, effectiveReq.blueprintId())) {
      saveDetails(principal.getTenantId(), principal.getUserId(), plan.getId(), effectiveReq.details());
    }
    generateDraftSnapshots(principal.getTenantId(), principal.getUserId(), plan, effectiveReq.details(),
        effectiveReq.practicalQuestionIds());
    return toPlanVO(principal.getTenantId(), plan);
  }

  @Transactional
  public void delete(Long eventId, Long planId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionMakeExamPlanEntity plan = findPlan(principal.getTenantId(), eventId, planId);
    if ("published".equals(plan.getStatus())) throw new BusinessException(ErrorCode.CONFLICT, "已发布组卷计划不可删除");
    planMapper.deleteById(plan.getId());
  }

  @Transactional
  public CompetitionMakeExamPlanVO publish(Long eventId, Long planId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionMakeExamPlanEntity plan = findPlan(principal.getTenantId(), eventId, planId);
    if (!"draft".equals(plan.getStatus())) throw new BusinessException(ErrorCode.CONFLICT, "只有草稿组卷计划可以发布");
    List<CompetitionMakeExamPaperSnapshotEntity> snapshots = snapshotMapper.selectByPlan(principal.getTenantId(), planId);
    int expectedDrafts = "practical".equalsIgnoreCase(plan.getContentType())
        ? (plan.getPaperCount() == null ? 1 : plan.getPaperCount()) : 1;
    if (snapshots == null || plan.getPaperCount() == null
        || snapshots.stream().filter(s -> "draft".equals(s.getStatus())).count() != expectedDrafts) {
      throw new BusinessException(ErrorCode.CONFLICT, "组卷计划缺少完整草稿快照，无法发布");
    }
    for (CompetitionMakeExamPaperSnapshotEntity snapshot : snapshots) {
      if ("draft".equals(snapshot.getStatus())) {
        validateSnapshotScore(principal.getTenantId(), snapshot, plan.getTotalScore());
      }
    }
    plan.setStatus("published");
    plan.setUpdatedBy(principal.getUserId());
    if (planMapper.updateById(plan) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "竞赛组卷计划发布失败");
    }
    if (snapshots != null) {
      for (CompetitionMakeExamPaperSnapshotEntity snapshot : snapshots) {
        if ("draft".equals(snapshot.getStatus())) {
          snapshot.setStatus("published");
          snapshot.setUpdatedBy(principal.getUserId());
          if (snapshotMapper.updateById(snapshot) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "竞赛试卷快照发布失败");
          }
        }
      }
    }
    return toPlanVO(principal.getTenantId(), plan);
  }

  @Transactional(readOnly = true)
  public List<CompetitionMakeExamSnapshotVO> listSnapshots(Long eventId, Long planId) {
    PlatformUserPrincipal principal = currentUser();
    findPlan(principal.getTenantId(), eventId, planId);
    List<CompetitionMakeExamPaperSnapshotEntity> snapshots = snapshotMapper.selectByPlan(principal.getTenantId(), planId);
    List<CompetitionMakeExamSnapshotVO> result = new ArrayList<>();
    if (snapshots != null) for (CompetitionMakeExamPaperSnapshotEntity snapshot : snapshots) result.add(toSnapshotVO(principal.getTenantId(), snapshot));
    return result;
  }

  /**
   * Lists all published one-paper snapshots available to a stage. A stage may select snapshots
   * from multiple independent plans to form A/B or other paper variants.
   */
  @Transactional(readOnly = true)
  public List<CompetitionMakeExamSnapshotVO> listPublishedSnapshots(
      Long eventId, Long eventWorkTypeId, Long eventWorkTypeLevelId, String contentType) {
    PlatformUserPrincipal principal = currentUser();
    requireScope(principal.getTenantId(), eventId, eventWorkTypeId, eventWorkTypeLevelId);
    List<CompetitionMakeExamPaperSnapshotEntity> snapshots = snapshotMapper.selectList(
        new LambdaQueryWrapper<CompetitionMakeExamPaperSnapshotEntity>()
            .eq(CompetitionMakeExamPaperSnapshotEntity::getTenantId, principal.getTenantId())
            .eq(CompetitionMakeExamPaperSnapshotEntity::getEventId, eventId)
            .eq(CompetitionMakeExamPaperSnapshotEntity::getEventWorkTypeId, eventWorkTypeId)
            .eq(CompetitionMakeExamPaperSnapshotEntity::getEventWorkTypeLevelId,
                eventWorkTypeLevelId)
            .eq(CompetitionMakeExamPaperSnapshotEntity::getContentType,
                normalizeContentType(contentType))
            .eq(CompetitionMakeExamPaperSnapshotEntity::getStatus, "published")
            .eq(CompetitionMakeExamPaperSnapshotEntity::getDeleted, 0)
            .orderByDesc(CompetitionMakeExamPaperSnapshotEntity::getUpdatedAt)
            .orderByDesc(CompetitionMakeExamPaperSnapshotEntity::getId));
    List<CompetitionMakeExamSnapshotVO> result = new ArrayList<>();
    if (snapshots != null) {
      for (CompetitionMakeExamPaperSnapshotEntity snapshot : snapshots) {
        CompetitionMakeExamPlanEntity owner = planMapper.selectOne(
            new LambdaQueryWrapper<CompetitionMakeExamPlanEntity>()
                .eq(CompetitionMakeExamPlanEntity::getTenantId, principal.getTenantId())
                .eq(CompetitionMakeExamPlanEntity::getId, snapshot.getPlanId())
                .eq(CompetitionMakeExamPlanEntity::getDeleted, 0));
        if (owner != null && !PURPOSE_PRACTICAL_SCORING_SCHEME.equals(owner.getPlanPurpose())) {
          result.add(toSnapshotVO(principal.getTenantId(), snapshot));
        }
      }
    }
    return result;
  }

  @Transactional(readOnly = true)
  public CompetitionMakeExamSnapshotVO previewSnapshot(Long eventId, Long snapshotId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionMakeExamPaperSnapshotEntity snapshot = snapshotMapper.selectOne(new LambdaQueryWrapper<CompetitionMakeExamPaperSnapshotEntity>()
        .eq(CompetitionMakeExamPaperSnapshotEntity::getTenantId, principal.getTenantId())
        .eq(CompetitionMakeExamPaperSnapshotEntity::getEventId, eventId)
        .eq(CompetitionMakeExamPaperSnapshotEntity::getId, snapshotId)
        .eq(CompetitionMakeExamPaperSnapshotEntity::getDeleted, 0));
    if (snapshot == null) throw new BusinessException(ErrorCode.NOT_FOUND, "竞赛试卷快照不存在");
    return toSnapshotVO(principal.getTenantId(), snapshot);
  }

  /**
   * Returns a published snapshot after checking the complete execution scope. This is shared by
   * the theory and practical competition execution adapters so a snapshot from another event,
   * level, content type, or draft plan cannot be bound to a stage.
   */
  @Transactional(readOnly = true)
  public CompetitionMakeExamSnapshotVO requirePublishedSnapshot(
      Long eventId,
      Long snapshotId,
      String contentType,
      Long eventWorkTypeId,
      Long eventWorkTypeLevelId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionMakeExamPaperSnapshotEntity snapshot = snapshotMapper.selectOne(
        new LambdaQueryWrapper<CompetitionMakeExamPaperSnapshotEntity>()
            .eq(CompetitionMakeExamPaperSnapshotEntity::getTenantId, principal.getTenantId())
            .eq(CompetitionMakeExamPaperSnapshotEntity::getId, snapshotId)
            .eq(CompetitionMakeExamPaperSnapshotEntity::getEventId, eventId)
            .eq(CompetitionMakeExamPaperSnapshotEntity::getEventWorkTypeId, eventWorkTypeId)
            .eq(CompetitionMakeExamPaperSnapshotEntity::getEventWorkTypeLevelId,
                eventWorkTypeLevelId)
            .eq(CompetitionMakeExamPaperSnapshotEntity::getContentType,
                normalizeContentType(contentType))
            .eq(CompetitionMakeExamPaperSnapshotEntity::getStatus, "published")
            .eq(CompetitionMakeExamPaperSnapshotEntity::getDeleted, 0));
    if (snapshot == null) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "竞赛组卷快照未发布或与当前赛事工种等级不匹配");
    }
    CompetitionMakeExamPlanEntity owner = planMapper.selectOne(
        new LambdaQueryWrapper<CompetitionMakeExamPlanEntity>()
            .eq(CompetitionMakeExamPlanEntity::getTenantId, principal.getTenantId())
            .eq(CompetitionMakeExamPlanEntity::getId, snapshot.getPlanId())
            .eq(CompetitionMakeExamPlanEntity::getStatus, "published")
            .eq(CompetitionMakeExamPlanEntity::getDeleted, 0));
    if (owner == null || PURPOSE_PRACTICAL_SCORING_SCHEME.equals(owner.getPlanPurpose())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "实操评分方案不能通过普通试卷绑定入口使用");
    }
    return toSnapshotVO(principal.getTenantId(), snapshot);
  }

  private void generateDraftSnapshots(Long tenantId, Long userId, CompetitionMakeExamPlanEntity plan,
      List<CompetitionMakeExamPlanDetailReq> details, List<Long> practicalQuestionIds) {
    if (blueprintService != null && plan.getBlueprintId() != null) {
      blueprintService.persistInheritedBindings(tenantId, plan.getBlueprintId(), userId);
    }
    if (PURPOSE_PRACTICAL_SCORING_SCHEME.equals(plan.getPlanPurpose())) {
      List<SelectedQuestion> selectedQuestions = selectedPracticalScoringSchemeQuestions(
          tenantId, plan.getEventId(), plan.getEventWorkTypeId(), plan.getEventWorkTypeLevelId(),
          practicalQuestionIds);
      persistSnapshot(tenantId, userId, plan, "P",
          Long.toString(plan.getRandomSeed() == null ? 0L : plan.getRandomSeed()), selectedQuestions);
      return;
    }
    List<CompetitionQuestionEntity> candidates = sortCandidateQuestions(
        questionMapper.selectList(questionScope(tenantId, plan)));
    if ("practical".equals(plan.getContentType()) && plan.getBlueprintId() != null) {
      generatePracticalBlueprintSnapshots(tenantId, userId, plan, candidates);
      return;
    }

    long randomSeedValue = plan.getRandomSeed() == null
        ? UUID.randomUUID().getMostSignificantBits() : plan.getRandomSeed();
    String randomSeed = Long.toString(randomSeedValue);
    Random random = new Random(randomSeedValue);
    List<SelectedQuestion> selectedQuestions;
    if ("theory".equals(plan.getContentType()) && plan.getBlueprintId() != null) {
      selectedQuestions = selectTheoryByTopics(tenantId, plan, candidates, details, randomSeedValue);
    } else if ("practical".equals(plan.getContentType()) && plan.getPracticalQuestionId() != null) {
      CompetitionQuestionEntity selected = candidates.stream()
          .filter(candidate -> candidate != null && candidate.getId() != null
              && candidate.getId().equals(plan.getPracticalQuestionId()))
          .findFirst()
          .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT,
              "指定的实操题不存在、已停用或不属于当前赛事工种等级"));
      CompetitionPracticalQuestionDetailEntity detail = practicalDetailMapper.selectByQuestion(tenantId, selected.getId());
      if (detail == null || detail.getTotalScore() == null || detail.getDurationMinutes() == null) {
        throw new BusinessException(ErrorCode.CONFLICT, "指定实操题缺少完整的总分或考试时间配置");
      }
      selectedQuestions = List.of(new SelectedQuestion(selected, detail.getTotalScore(), null, null,
          null, null, detail.getTotalScore(), BigDecimal.ONE));
    } else if ("practical".equals(plan.getContentType())) {
      selectedQuestions = selectSimplePracticalQuestions(tenantId, candidates, details, random);
    } else {
      selectedQuestions = selectQuestionsForDetails(candidates, details, random);
    }
    persistSnapshot(tenantId, userId, plan, "P", randomSeed, selectedQuestions);
    if ("theory".equals(plan.getContentType()) && plan.getBlueprintId() != null) {
      saveAutoTheoryTopicRules(tenantId, userId, plan.getId(), plan.getBlueprintId(), selectedQuestions);
    }
  }

  private void generatePracticalBlueprintSnapshots(Long tenantId, Long userId,
      CompetitionMakeExamPlanEntity plan, List<CompetitionQuestionEntity> candidates) {
    BlueprintTopology topology = loadTopology(tenantId, plan.getBlueprintId());
    List<CompetitionQuestionBlueprintBindingEntity> bindingRows = safeBindings(tenantId, plan.getBlueprintId());
    List<CompetitionPracticalMakeExamAllocator.NodeInput> nodes = practicalNodeInputs(topology.nodes());
    List<CompetitionPracticalMakeExamAllocator.BindingInput> bindings = bindingRows.stream()
        .filter(row -> row != null)
        .map(row -> new CompetitionPracticalMakeExamAllocator.BindingInput(row.getQuestionId(),
            row.getBlueprintId(), row.getBlueprintNodeId(), row.getDeleted() == null || row.getDeleted() == 0))
        .toList();
    List<CompetitionPracticalMakeExamAllocator.CandidateInput> candidateInputs = practicalCandidateInputs(
        tenantId, candidates);
    long baseSeed = plan.getRandomSeed() == null
        ? UUID.randomUUID().getMostSignificantBits() : plan.getRandomSeed();
    Set<Long> usedAcrossPapers = new HashSet<>();
    CompetitionPracticalMakeExamAllocator.AllocationResult firstAllocation = null;
    int paperCount = plan.getPaperCount() == null ? 1 : plan.getPaperCount();
    for (int paper = 1; paper <= paperCount; paper++) {
      long paperSeed = mixSeed(baseSeed, paper);
      CompetitionPracticalMakeExamAllocator.AllocationRequest request =
          new CompetitionPracticalMakeExamAllocator.AllocationRequest(plan.getBlueprintId(), nodes, bindings,
              candidateInputs, plan.getTotalScore(), paper, paperSeed, plan.getDurationMinutes(), usedAcrossPapers);
      CompetitionPracticalMakeExamAllocator.AllocationResult allocation = practicalAllocator.allocate(request);
      if (!allocation.ready()) {
        throw new BusinessException(ErrorCode.CONFLICT, String.join("；", allocation.errors()));
      }
      List<SelectedQuestion> selectedQuestions = selectedPracticalQuestions(plan, candidates, topology, allocation,
          bindingRows);
      persistSnapshot(tenantId, userId, plan, paperCount == 1 ? "P" : "P" + paper,
          Long.toString(paperSeed), selectedQuestions);
      selectedQuestions.stream().map(selected -> selected.question().getId()).forEach(usedAcrossPapers::add);
      if (firstAllocation == null) firstAllocation = allocation;
    }
    if (firstAllocation != null) {
      saveAutoPracticalTopicRules(tenantId, userId, plan.getId(), firstAllocation);
    }
  }

  private void persistSnapshot(Long tenantId, Long userId, CompetitionMakeExamPlanEntity plan,
      String paperCode, String randomSeed, List<SelectedQuestion> selectedQuestions) {
    BigDecimal actualTotalScore = sumAssignedScores(selectedQuestions);
    if (actualTotalScore.compareTo(plan.getTotalScore()) != 0) {
      throw new BusinessException(ErrorCode.CONFLICT,
          "组卷规则合计分值 " + actualTotalScore + " 不等于计划总分 " + plan.getTotalScore());
    }
    CompetitionMakeExamPaperSnapshotEntity snapshot = new CompetitionMakeExamPaperSnapshotEntity();
    snapshot.setId(IdWorker.getId());
    snapshot.setTenantId(tenantId);
    snapshot.setPlanId(plan.getId());
    snapshot.setBlueprintId(plan.getBlueprintId());
    snapshot.setEventId(plan.getEventId());
    snapshot.setEventWorkTypeId(plan.getEventWorkTypeId());
    snapshot.setEventWorkTypeLevelId(plan.getEventWorkTypeLevelId());
    snapshot.setContentType(plan.getContentType());
    snapshot.setPaperCode(paperCode);
    snapshot.setVersionNo(plan.getPlanVersion());
    snapshot.setTitle("P".equals(paperCode) ? plan.getTitle() + "（试卷）"
        : plan.getTitle() + "（" + paperCode + "卷）");
    snapshot.setDurationMinutes(plan.getDurationMinutes());
    snapshot.setQuestionCount(selectedQuestions.size());
    snapshot.setTotalScore(actualTotalScore);
    snapshot.setPassScore(plan.getPassScore());
    snapshot.setStatus("draft");
    snapshot.setRandomSeed(randomSeed);
    snapshot.setSourceHash(hashQuestionIds(selectedQuestions));
    snapshot.setCreatedBy(userId);
    snapshot.setUpdatedBy(userId);
    snapshot.setDeleted(0);
    if (snapshotMapper.insert(snapshot) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "竞赛试卷快照保存失败");
    }
    int order = 1;
    for (SelectedQuestion selected : selectedQuestions) {
      saveSnapshotQuestion(tenantId, userId, snapshot.getId(), order++, selected);
    }
  }

  private List<SelectedQuestion> selectedPracticalQuestions(CompetitionMakeExamPlanEntity plan,
      List<CompetitionQuestionEntity> candidates, BlueprintTopology topology,
      CompetitionPracticalMakeExamAllocator.AllocationResult allocation,
      List<CompetitionQuestionBlueprintBindingEntity> bindingRows) {
    Map<Long, CompetitionQuestionEntity> questionById = new HashMap<>();
    for (CompetitionQuestionEntity candidate : candidates) {
      if (candidate != null && candidate.getId() != null) questionById.put(candidate.getId(), candidate);
    }
    Map<Long, List<CompetitionQuestionBlueprintBindingEntity>> bindingsByQuestion = new HashMap<>();
    for (CompetitionQuestionBlueprintBindingEntity binding : bindingRows) {
      if (binding != null && binding.getQuestionId() != null
          && (binding.getDeleted() == null || binding.getDeleted() == 0)) {
        bindingsByQuestion.computeIfAbsent(binding.getQuestionId(), ignored -> new ArrayList<>()).add(binding);
      }
    }
    List<SelectedQuestion> result = new ArrayList<>();
    for (CompetitionPracticalMakeExamAllocator.UnitAllocation unit : allocation.units()) {
      CompetitionQuestionEntity question = questionById.get(unit.selectedQuestionId());
      if (question == null) {
        throw new BusinessException(ErrorCode.CONFLICT, "实操自动组卷结果中的题目已不在当前候选范围");
      }
      CompetitionQuestionBlueprintBindingEntity binding = bindingsByQuestion
          .getOrDefault(question.getId(), List.of()).stream()
          .filter(item -> unit.selectedAssessmentPointId() == null
              || unit.selectedAssessmentPointId().equals(item.getBlueprintNodeId()))
          .findFirst().orElse(null);
      if (binding == null) {
        throw new BusinessException(ErrorCode.CONFLICT, "实操自动组卷结果缺少有效末级考评点绑定");
      }
      result.add(selected(question, unit.targetScore(), plan.getBlueprintId(), binding,
          topology.pathById(), topology.fullCodeById(), unit.sourceTotalScore(), unit.scoreScale()));
    }
    return result;
  }

  private List<CompetitionPracticalMakeExamAllocator.NodeInput> practicalNodeInputs(
      List<CompetitionQuestionBlueprintNodeEntity> nodes) {
    List<CompetitionPracticalMakeExamAllocator.NodeInput> result = new ArrayList<>();
    if (nodes != null) {
      for (CompetitionQuestionBlueprintNodeEntity node : nodes) {
        if (node == null) continue;
        result.add(new CompetitionPracticalMakeExamAllocator.NodeInput(node.getId(), node.getParentId(),
            node.getNodeLevel(), node.getCode(), node.getFullCode(), node.getName(), node.getWeight(),
            node.getSelectMode(), node.getSortOrder()));
      }
    }
    return result;
  }

  private List<CompetitionPracticalMakeExamAllocator.CandidateInput> practicalCandidateInputs(
      Long tenantId, List<CompetitionQuestionEntity> candidates) {
    List<CompetitionPracticalMakeExamAllocator.CandidateInput> result = new ArrayList<>();
    if (candidates == null) return result;
    for (CompetitionQuestionEntity candidate : candidates) {
      if (candidate == null || candidate.getId() == null) continue;
      CompetitionPracticalQuestionDetailEntity detail = practicalDetailMapper == null ? null
          : practicalDetailMapper.selectByQuestion(tenantId, candidate.getId());
      boolean baseConfigured = detail != null && detail.getDurationMinutes() != null
          && detail.getDurationMinutes() > 0 && detail.getTotalScore() != null
          && detail.getTotalScore().signum() > 0;
      // Keep candidates with an invalid base total/time enabled so the common
      // allocator can report the precise source-data warning.  Once those base
      // values are present, an invalid scoring-item structure is not a
      // complete practical question and must not be selected at all.
      boolean enabled = !baseConfigured || validPracticalScoringShape(tenantId, candidate.getId(), detail);
      result.add(new CompetitionPracticalMakeExamAllocator.CandidateInput(candidate.getId(),
          detail == null ? null : detail.getTotalScore(), detail == null ? null : detail.getDurationMinutes(),
          enabled));
    }
    return result;
  }

  private void appendPracticalCandidateWarnings(
      List<CompetitionPracticalMakeExamAllocator.CandidateInput> candidates,
      List<String> warnings) {
    if (candidates == null || warnings == null) return;
    for (CompetitionPracticalMakeExamAllocator.CandidateInput candidate : candidates) {
      if (candidate == null || candidate.questionId() == null || candidate.enabled()
          || candidate.sourceTotalScore() == null || candidate.sourceTotalScore().signum() <= 0
          || candidate.durationMinutes() == null || candidate.durationMinutes() <= 0) continue;
      warnings.add("实操题" + candidate.questionId() + "评分项结构不完整或分值不守恒，已排除");
    }
  }

  private boolean validPracticalScoringShape(Long tenantId, Long questionId,
      CompetitionPracticalQuestionDetailEntity detail) {
    if (detail == null || scoreItemMapper == null) return false;
    String scoringMethod = normalizeSnapshotScoringMethod(detail.getScoringMethod());
    if (scoringMethod == null) return false;
    List<CompetitionPracticalQuestionScoreItemEntity> items = scoreItemMapper.selectByQuestion(tenantId, questionId);
    if (items == null || items.isEmpty()) return false;
    Set<Integer> itemNos = new HashSet<>();
    BigDecimal ordinaryTotal = BigDecimal.ZERO;
    int ordinaryCount = 0;
    for (CompetitionPracticalQuestionScoreItemEntity item : items) {
      if (item == null || item.getItemNo() == null || item.getItemNo() <= 0
          || !itemNos.add(item.getItemNo())) return false;
      if (Integer.valueOf(0).equals(item.getAllocationEnabled())) {
        if (item.getMaxScore() != null || item.getMaxDeduction() != null || item.getDeductionStep() != null) {
          return false;
        }
        continue;
      }
      if (item.getMaxScore() == null || item.getMaxScore().signum() <= 0) return false;
      if ("deductive".equals(scoringMethod)) {
        if (item.getMaxDeduction() == null || item.getMaxDeduction().signum() < 0
            || item.getMaxDeduction().compareTo(item.getMaxScore()) > 0
            || item.getDeductionStep() == null || item.getDeductionStep().signum() <= 0
            || item.getMaxDeduction().remainder(item.getDeductionStep()).signum() != 0) return false;
      } else if (item.getMaxDeduction() != null || item.getDeductionStep() != null) {
        return false;
      }
      ordinaryTotal = ordinaryTotal.add(item.getMaxScore());
      ordinaryCount++;
    }
    return ordinaryCount > 0 && ordinaryTotal.compareTo(detail.getTotalScore()) == 0;
  }

  private long mixSeed(long seed, int paperSequence) {
    long value = seed ^ (0x9E3779B97F4A7C15L * paperSequence);
    value ^= value >>> 33;
    value *= 0xff51afd7ed558ccdl;
    value ^= value >>> 33;
    return value;
  }

  private CompetitionPracticalMakeExamAllocator.AllocationResult allocatePractical(
      Long tenantId,
      Long blueprintId,
      BlueprintTopology topology,
      List<CompetitionPracticalMakeExamAllocator.CandidateInput> candidates,
      BigDecimal totalScore,
      Integer plannedDurationMinutes,
      int paperSequence,
      long randomSeed,
      Set<Long> previouslySelectedQuestionIds) {
    List<CompetitionQuestionBlueprintBindingEntity> bindingRows = safeBindings(tenantId, blueprintId);
    List<CompetitionPracticalMakeExamAllocator.BindingInput> bindings = bindingRows.stream()
        .filter(row -> row != null)
        .map(row -> new CompetitionPracticalMakeExamAllocator.BindingInput(row.getQuestionId(),
            row.getBlueprintId(), row.getBlueprintNodeId(), row.getDeleted() == null || row.getDeleted() == 0))
        .toList();
    return practicalAllocator.allocate(new CompetitionPracticalMakeExamAllocator.AllocationRequest(
        blueprintId, practicalNodeInputs(topology.nodes()), bindings,
        candidates, totalScore, paperSequence, randomSeed,
        plannedDurationMinutes, previouslySelectedQuestionIds));
  }

  private List<CompetitionMakeExamPlanPrecheckVO.PracticalAllocationItem> toPracticalPrecheck(
      CompetitionPracticalMakeExamAllocator.AllocationResult allocation) {
    List<CompetitionMakeExamPlanPrecheckVO.PracticalAllocationItem> result = new ArrayList<>();
    for (CompetitionPracticalMakeExamAllocator.UnitAllocation unit : allocation.units()) {
      List<CompetitionMakeExamPlanPrecheckVO.PracticalCandidateNode> candidateNodes = new ArrayList<>();
      for (CompetitionPracticalMakeExamAllocator.CandidateNode candidate : unit.candidateNodes()) {
        candidateNodes.add(new CompetitionMakeExamPlanPrecheckVO.PracticalCandidateNode(
            stringId(candidate.blueprintNodeId()), candidate.path(), candidate.scoreWeight(),
            candidate.assessmentPointIds().stream().map(this::stringId).toList(),
            candidate.candidateQuestionCount(), candidate.candidateQuestionIds().stream().map(this::stringId).toList(),
            candidate.selected()));
      }
      result.add(new CompetitionMakeExamPlanPrecheckVO.PracticalAllocationItem(
          unit.selectionGroup(), unit.selectionMode(), stringId(unit.structureNodeId()), unit.structurePath(),
          unit.scoreWeight(), unit.targetScore(), unit.candidateQuestionCount(), candidateNodes,
          stringId(unit.selectedQuestionId()), stringId(unit.selectedAssessmentPointId()),
          unit.sourceTotalScore(), unit.scoreScale(), unit.referenceDurationMinutes()));
    }
    return result;
  }

  private String stringId(Long value) {
    return value == null ? null : String.valueOf(value);
  }

  private List<SelectedQuestion> selectTheoryByTopics(Long tenantId, CompetitionMakeExamPlanEntity plan,
      List<CompetitionQuestionEntity> candidates, List<CompetitionMakeExamPlanDetailReq> details,
      long randomSeed) {
    BlueprintTopology topology = loadTopology(tenantId, plan.getBlueprintId());
    Map<Long, CompetitionQuestionBlueprintBindingEntity> bindings = bindingByQuestion(tenantId, plan.getBlueprintId());
    List<CompetitionQuestionEntity> validCandidates = validTheoryCandidates(candidates, topology, bindings,
        plan.getBlueprintId());
    CompetitionTheoryMakeExamAllocator.AllocationResult allocation = allocateTheory(
        topology, details, validCandidates, bindings, randomSeed);
    if (!allocation.ready()) {
      throw new BusinessException(ErrorCode.CONFLICT, String.join("；", allocation.errors()));
    }
    Map<Long, CompetitionQuestionEntity> questionById = new HashMap<>();
    for (CompetitionQuestionEntity candidate : validCandidates) questionById.put(candidate.getId(), candidate);
    List<SelectedQuestion> selected = new ArrayList<>();
    for (CompetitionTheoryMakeExamAllocator.Selection selection : allocation.selections()) {
      CompetitionQuestionEntity question = questionById.get(selection.questionId());
      if (question == null || selection.ruleIndex() < 0 || selection.ruleIndex() >= details.size()) {
        throw new BusinessException(ErrorCode.CONFLICT, "理论组卷自动抽题结果无法对应组卷明细");
      }
      CompetitionQuestionBlueprintBindingEntity binding = bindings.get(question.getId());
      CompetitionMakeExamPlanDetailReq detail = details.get(selection.ruleIndex());
      selected.add(selected(question, detail.point(), plan.getBlueprintId(), binding,
          topology.pathById(), topology.fullCodeById()));
    }
    return selected;
  }

  private CompetitionTheoryMakeExamAllocator.AllocationResult allocateTheory(
      BlueprintTopology topology,
      List<CompetitionMakeExamPlanDetailReq> details,
      List<CompetitionQuestionEntity> candidates,
      Map<Long, CompetitionQuestionBlueprintBindingEntity> bindings,
      long randomSeed) {
    List<CompetitionTheoryMakeExamAllocator.RuleInput> rules = new ArrayList<>();
    if (details != null) {
      for (int index = 0; index < details.size(); index++) {
        CompetitionMakeExamPlanDetailReq detail = details.get(index);
        if (detail == null) continue;
        rules.add(new CompetitionTheoryMakeExamAllocator.RuleInput(index, detail.questionType(),
            detail.questionCount() == null ? 0 : detail.questionCount(), detail.point(),
            detail.difficultyStart(), detail.difficultyEnd(), detail.sortOrder() == null ? index + 1 : detail.sortOrder()));
      }
    }
    List<CompetitionTheoryMakeExamAllocator.TopicInput> topics = new ArrayList<>();
    for (int index = 0; index < topology.theoryTopicNodes().size(); index++) {
      CompetitionQuestionBlueprintNodeEntity topic = topology.theoryTopicNodes().get(index);
      topics.add(new CompetitionTheoryMakeExamAllocator.TopicInput(topic.getId(),
          topic.getNodeLevel() == null ? 0 : topic.getNodeLevel(), topology.pathById().get(topic.getId()),
          topic.getFullCode(), topic.getWeight(), index + 1));
    }
    List<CompetitionTheoryMakeExamAllocator.CandidateInput> candidateInputs = new ArrayList<>();
    if (candidates != null) {
      for (CompetitionQuestionEntity candidate : candidates) {
        if (candidate == null || candidate.getId() == null) continue;
        CompetitionQuestionBlueprintBindingEntity binding = bindings.get(candidate.getId());
        Long topicId = binding == null ? null : topology.theoryTopicForPoint().get(binding.getBlueprintNodeId());
        if (topicId == null) continue;
        candidateInputs.add(new CompetitionTheoryMakeExamAllocator.CandidateInput(candidate.getId(), topicId,
            candidate.getQuestionType(), candidate.getDifficulty()));
      }
    }
    return theoryAllocator.allocate(rules, topics, candidateInputs, randomSeed);
  }

  private List<CompetitionQuestionEntity> validTheoryCandidates(
      List<CompetitionQuestionEntity> candidates,
      BlueprintTopology topology,
      Map<Long, CompetitionQuestionBlueprintBindingEntity> bindings,
      Long blueprintId) {
    List<CompetitionQuestionEntity> result = new ArrayList<>();
    Set<Long> seen = new HashSet<>();
    if (candidates == null) return result;
    for (CompetitionQuestionEntity candidate : candidates) {
      if (candidate == null || candidate.getId() == null || !seen.add(candidate.getId())) continue;
      CompetitionQuestionBlueprintBindingEntity binding = bindings.get(candidate.getId());
      if (binding == null || !blueprintId.equals(binding.getBlueprintId())
          || !topology.theoryTopicForPoint().containsKey(binding.getBlueprintNodeId())) {
        continue;
      }
      result.add(candidate);
    }
    return result;
  }

  private int countUnusableTheoryQuestions(
      List<CompetitionQuestionEntity> allScopeQuestions,
      BlueprintTopology topology,
      Map<Long, CompetitionQuestionBlueprintBindingEntity> bindings,
      Long blueprintId) {
    return allScopeQuestions == null ? 0 : (int) allScopeQuestions.stream()
        .filter(question -> question != null && question.getId() != null)
        .filter(question -> {
          CompetitionQuestionBlueprintBindingEntity binding = bindings.get(question.getId());
          return binding == null || !blueprintId.equals(binding.getBlueprintId())
              || !topology.theoryTopicForPoint().containsKey(binding.getBlueprintNodeId());
        })
        .map(CompetitionQuestionEntity::getId)
        .distinct()
        .count();
  }

  private CompetitionMakeExamPlanEntity copyPlanWithoutBlueprint(CompetitionMakeExamPlanEntity source) {
    CompetitionMakeExamPlanEntity copy = new CompetitionMakeExamPlanEntity();
    copy.setEventId(source.getEventId());
    copy.setEventWorkTypeId(source.getEventWorkTypeId());
    copy.setEventWorkTypeLevelId(source.getEventWorkTypeLevelId());
    copy.setContentType(source.getContentType());
    copy.setBlueprintId(null);
    return copy;
  }

  private SelectedQuestion selected(CompetitionQuestionEntity question, BigDecimal point, Long blueprintId,
      CompetitionQuestionBlueprintBindingEntity binding, Map<Long, String> pathById, Map<Long, String> fullCodeById) {
    return selected(question, point, blueprintId, binding, pathById, fullCodeById, null, null);
  }

  private SelectedQuestion selected(CompetitionQuestionEntity question, BigDecimal point, Long blueprintId,
      CompetitionQuestionBlueprintBindingEntity binding, Map<Long, String> pathById, Map<Long, String> fullCodeById,
      BigDecimal sourceTotalScore, BigDecimal scoreScale) {
    return new SelectedQuestion(question, point, blueprintId,
        binding == null ? null : binding.getBlueprintNodeId(),
        binding == null ? null : fullCodeById.get(binding.getBlueprintNodeId()),
        binding == null ? null : pathById.get(binding.getBlueprintNodeId()), sourceTotalScore, scoreScale);
  }

  private List<CompetitionQuestionBlueprintBindingEntity> safeBindings(Long tenantId, Long blueprintId) {
    if (blueprintId != null && blueprintService != null) {
      List<CompetitionQuestionBlueprintBindingEntity> effective =
          blueprintService.listEffectiveBindings(tenantId, blueprintId);
      if (effective != null) return effective;
    }
    if (blueprintBindingMapper == null) return List.of();
    List<CompetitionQuestionBlueprintBindingEntity> rows = blueprintBindingMapper.selectByBlueprint(tenantId, blueprintId);
    return rows == null ? List.of() : rows;
  }

  private Map<Long, CompetitionQuestionBlueprintBindingEntity> bindingByQuestion(Long tenantId, Long blueprintId) {
    Map<Long, CompetitionQuestionBlueprintBindingEntity> result = new HashMap<>();
    for (CompetitionQuestionBlueprintBindingEntity row : safeBindings(tenantId, blueprintId)) {
      if (row == null || row.getQuestionId() == null) continue;
      result.putIfAbsent(row.getQuestionId(), row);
    }
    return result;
  }

  private List<CompetitionMakeExamPlanTopicRuleEntity> loadTopicRules(Long tenantId, Long planId) {
    if (topicRuleMapper == null) return List.of();
    List<CompetitionMakeExamPlanTopicRuleEntity> rows = topicRuleMapper.selectByPlan(tenantId, planId);
    return rows == null ? List.of() : new ArrayList<>(rows);
  }

  private BlueprintTopology loadTopology(Long tenantId, Long blueprintId) {
    if (blueprintNodeMapper == null) throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "细目表节点服务未就绪");
    List<CompetitionQuestionBlueprintNodeEntity> nodes = blueprintNodeMapper.selectByBlueprint(tenantId, blueprintId);
    if (nodes == null || nodes.isEmpty()) throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "细目表没有节点");
    Map<Long, List<CompetitionQuestionBlueprintNodeEntity>> children = new HashMap<>();
    Map<Long, CompetitionQuestionBlueprintNodeEntity> byId = new HashMap<>();
    for (CompetitionQuestionBlueprintNodeEntity node : nodes) {
      byId.put(node.getId(), node);
      children.computeIfAbsent(node.getParentId() == null ? 0L : node.getParentId(), ignored -> new ArrayList<>()).add(node);
    }
    // Practical plans retain the historical topic discovery path. Theory plans
    // are deliberately stricter: a topic is exactly nodeLevel=3, and a
    // candidate is mapped to it only through a nodeLevel=4 assessment point.
    List<CompetitionQuestionBlueprintNodeEntity> topics = nodes.stream().filter(node -> {
      List<CompetitionQuestionBlueprintNodeEntity> childRows = children.getOrDefault(node.getId(), List.of());
      return !childRows.isEmpty() && childRows.stream().noneMatch(child -> !children.getOrDefault(child.getId(), List.of()).isEmpty());
    }).sorted(nodeOrder()).toList();
    List<CompetitionQuestionBlueprintNodeEntity> theoryTopics = nodes.stream()
        .filter(node -> Integer.valueOf(3).equals(node.getNodeLevel()))
        .sorted(nodeOrder()).toList();
    Set<Long> topicIds = topics.stream().map(CompetitionQuestionBlueprintNodeEntity::getId).collect(java.util.stream.Collectors.toSet());
    Set<Long> theoryTopicIds = theoryTopics.stream().map(CompetitionQuestionBlueprintNodeEntity::getId)
        .collect(java.util.stream.Collectors.toSet());
    Map<Long, Long> topicForPoint = new HashMap<>();
    Map<Long, Long> theoryTopicForPoint = new HashMap<>();
    Map<Long, String> paths = new HashMap<>();
    Map<Long, String> fullCodes = new HashMap<>();
    for (CompetitionQuestionBlueprintNodeEntity node : nodes) {
      List<String> parts = new ArrayList<>();
      CompetitionQuestionBlueprintNodeEntity current = node;
      Set<Long> seen = new HashSet<>();
      Long topic = null;
      while (current != null && seen.add(current.getId())) {
        parts.add(current.getName());
        if (topicIds.contains(current.getId())) topic = current.getId();
        Long parent = current.getParentId(); current = parent == null || parent == 0L ? null : byId.get(parent);
      }
      Collections.reverse(parts); paths.put(node.getId(), String.join("/", parts));
      fullCodes.put(node.getId(), node.getFullCode());
      if (topic != null && (children.getOrDefault(node.getId(), List.of()).isEmpty())) topicForPoint.put(node.getId(), topic);
      if (Integer.valueOf(4).equals(node.getNodeLevel())) {
        Long theoryTopic = null;
        CompetitionQuestionBlueprintNodeEntity currentPoint = node;
        Set<Long> theorySeen = new HashSet<>();
        while (currentPoint != null && theorySeen.add(currentPoint.getId())) {
          if (theoryTopicIds.contains(currentPoint.getId())) {
            theoryTopic = currentPoint.getId();
            break;
          }
          Long parent = currentPoint.getParentId();
          currentPoint = parent == null || parent == 0L ? null : byId.get(parent);
        }
        if (theoryTopic != null) theoryTopicForPoint.put(node.getId(), theoryTopic);
      }
    }
    return new BlueprintTopology(nodes, topics, theoryTopics, topicForPoint, theoryTopicForPoint, paths, fullCodes);
  }

  private Comparator<CompetitionQuestionBlueprintNodeEntity> nodeOrder() {
    return Comparator.comparingInt((CompetitionQuestionBlueprintNodeEntity node) ->
            node.getSortOrder() == null ? Integer.MAX_VALUE : node.getSortOrder())
        .thenComparing(node -> node.getFullCode() == null ? "" : node.getFullCode())
        .thenComparing(node -> node.getId() == null ? Long.MAX_VALUE : node.getId());
  }

  private record BlueprintTopology(List<CompetitionQuestionBlueprintNodeEntity> nodes,
      List<CompetitionQuestionBlueprintNodeEntity> topicNodes,
      List<CompetitionQuestionBlueprintNodeEntity> theoryTopicNodes,
      Map<Long, Long> topicForPoint, Map<Long, Long> theoryTopicForPoint,
      Map<Long, String> pathById, Map<Long, String> fullCodeById) {}

  private LambdaQueryWrapper<CompetitionQuestionEntity> questionScope(Long tenantId,
      CompetitionMakeExamPlanEntity plan) {
    LambdaQueryWrapper<CompetitionQuestionEntity> wrapper = new LambdaQueryWrapper<CompetitionQuestionEntity>()
        .eq(CompetitionQuestionEntity::getTenantId, tenantId)
        .eq(CompetitionQuestionEntity::getEventId, plan.getEventId())
        .eq(CompetitionQuestionEntity::getEventWorkTypeId, plan.getEventWorkTypeId())
        .eq(CompetitionQuestionEntity::getEventWorkTypeLevelId, plan.getEventWorkTypeLevelId())
        .eq(CompetitionQuestionEntity::getContentType, plan.getContentType())
        .eq(CompetitionQuestionEntity::getStatus, "enabled")
        .eq(CompetitionQuestionEntity::getDeleted, 0);
    if (plan.getBlueprintId() != null && (blueprintService != null || blueprintBindingMapper != null)) {
      List<CompetitionQuestionBlueprintBindingEntity> bindings = safeBindings(
          tenantId, plan.getBlueprintId());
      List<Long> ids = bindings == null ? List.of() : bindings.stream()
          .map(CompetitionQuestionBlueprintBindingEntity::getQuestionId)
          .filter(java.util.Objects::nonNull).distinct().toList();
      wrapper.in(CompetitionQuestionEntity::getId, ids.isEmpty() ? List.of(-1L) : ids);
    }
    return wrapper;
  }

  private List<CompetitionQuestionEntity> sortCandidateQuestions(List<CompetitionQuestionEntity> questions) {
    if (questions == null) return new ArrayList<>();
    return questions.stream()
        .filter(question -> question != null && question.getId() != null)
        .sorted(Comparator.comparing(CompetitionQuestionEntity::getId))
        .toList();
  }

  private List<SelectedQuestion> selectQuestionsForDetails(
      List<CompetitionQuestionEntity> candidates,
      List<CompetitionMakeExamPlanDetailReq> details,
      Random random) {
    if (details == null || details.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "至少需要配置一条组卷规则");
    }
    List<SelectedQuestion> selected = new ArrayList<>();
    Set<Long> usedIds = new HashSet<>();
    List<CompetitionMakeExamPlanDetailReq> orderedDetails = new ArrayList<>(details);
    orderedDetails.sort(Comparator.comparing(CompetitionMakeExamPlanDetailReq::sortOrder));
    for (CompetitionMakeExamPlanDetailReq detail : orderedDetails) {
      String type = detail.questionType().toLowerCase(Locale.ROOT);
      List<CompetitionQuestionEntity> available = new ArrayList<>();
      if (candidates != null) {
        for (CompetitionQuestionEntity candidate : candidates) {
          if (candidate == null || candidate.getId() == null || usedIds.contains(candidate.getId())
              || !type.equalsIgnoreCase(candidate.getQuestionType())
              || !matchesDifficulty(candidate, detail)) {
            continue;
          }
          available.add(candidate);
        }
      }
      Collections.shuffle(available, random);
      if (available.size() < detail.questionCount()) {
        throw new BusinessException(ErrorCode.CONFLICT,
            type + "题可用题目不足，需要 " + detail.questionCount() + " 道，当前仅有 " + available.size() + " 道");
      }
      for (int index = 0; index < detail.questionCount(); index++) {
        CompetitionQuestionEntity question = available.get(index);
        usedIds.add(question.getId());
        selected.add(new SelectedQuestion(question, detail.point()));
      }
    }
    return selected;
  }

  private List<SelectedQuestion> selectSimplePracticalQuestions(
      Long tenantId,
      List<CompetitionQuestionEntity> candidates,
      List<CompetitionMakeExamPlanDetailReq> details,
      Random random) {
    if (details == null || details.size() != 1) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "简易实操组卷只允许配置一条实操题型规则");
    }
    CompetitionMakeExamPlanDetailReq rule = details.get(0);
    List<CompetitionQuestionEntity> available = validSimplePracticalCandidates(
        tenantId, candidates, rule.point());
    Collections.shuffle(available, random);
    if (available.size() < rule.questionCount()) {
      throw new BusinessException(ErrorCode.CONFLICT,
          "实操题可用题目不足，需要 " + rule.questionCount() + " 道，当前仅有 " + available.size() + " 道");
    }
    List<SelectedQuestion> selected = new ArrayList<>();
    for (int index = 0; index < rule.questionCount(); index++) {
      CompetitionQuestionEntity question = available.get(index);
      CompetitionPracticalQuestionDetailEntity detail = practicalDetailMapper.selectByQuestion(
          tenantId, question.getId());
      BigDecimal sourceTotalScore = detail.getTotalScore();
      BigDecimal scoreScale = rule.point().divide(sourceTotalScore, 12, RoundingMode.HALF_UP);
      selected.add(new SelectedQuestion(question, rule.point(), null, null, null, null,
          sourceTotalScore, scoreScale));
    }
    return selected;
  }

  private List<CompetitionQuestionEntity> validSimplePracticalCandidates(
      Long tenantId, List<CompetitionQuestionEntity> candidates, BigDecimal targetScore) {
    List<CompetitionQuestionEntity> valid = new ArrayList<>();
    if (candidates == null) return valid;
    for (CompetitionQuestionEntity candidate : candidates) {
      if (candidate == null || candidate.getId() == null
          || !"practical".equalsIgnoreCase(candidate.getQuestionType())) {
        continue;
      }
      CompetitionPracticalQuestionDetailEntity detail = practicalDetailMapper.selectByQuestion(
          tenantId, candidate.getId());
      if (detail == null || detail.getTotalScore() == null) continue;
      try {
        validatePracticalScoringData(tenantId, candidate.getId(), detail,
            detail.getTotalScore(), targetScore);
        valid.add(candidate);
      } catch (BusinessException ex) {
        log.debug("Exclude practical question from simple make-plan candidates: questionId={}, reason={}",
            candidate.getId(), ex.getMessage());
      }
    }
    return valid;
  }

  private boolean matchesDifficulty(CompetitionQuestionEntity question,
      CompetitionMakeExamPlanDetailReq detail) {
    Integer difficulty = question.getDifficulty();
    return (detail.difficultyStart() == null
        || difficulty != null && difficulty >= detail.difficultyStart())
        && (detail.difficultyEnd() == null
        || difficulty != null && difficulty <= detail.difficultyEnd());
  }

  private BigDecimal sumAssignedScores(List<SelectedQuestion> questions) {
    BigDecimal total = BigDecimal.ZERO;
    if (questions != null) {
      for (SelectedQuestion question : questions) {
        if (question == null || question.point() == null) {
          throw new BusinessException(ErrorCode.CONFLICT, "组卷规则缺少有效单题分值");
        }
        total = total.add(question.point());
      }
    }
    return total;
  }

  private void validateSnapshotScore(Long tenantId,
      CompetitionMakeExamPaperSnapshotEntity snapshot,
      BigDecimal planTotalScore) {
    List<CompetitionMakeExamPaperQuestionEntity> questions = snapshotQuestionMapper.selectByPaper(
        tenantId, snapshot.getId());
    BigDecimal actualTotalScore = BigDecimal.ZERO;
    int questionCount = 0;
    if (questions != null) {
      questionCount = questions.size();
      for (CompetitionMakeExamPaperQuestionEntity question : questions) {
        if (question == null || question.getScore() == null) {
          throw new BusinessException(ErrorCode.CONFLICT, "组卷快照存在缺少分值的题目，无法发布");
        }
        actualTotalScore = actualTotalScore.add(question.getScore());
      }
    }
    if (questionCount == 0 || actualTotalScore.compareTo(planTotalScore) != 0
        || snapshot.getTotalScore() == null
        || snapshot.getTotalScore().compareTo(planTotalScore) != 0) {
      throw new BusinessException(ErrorCode.CONFLICT,
          "组卷快照题目总分 " + actualTotalScore + " 不等于计划总分 " + planTotalScore + "，无法发布");
    }
  }

  private long scoreCents(BigDecimal score) {
    if (score == null || score.signum() <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "组卷计划总分和单题分值必须为正数");
    }
    try {
      return score.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact();
    } catch (ArithmeticException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "组卷计划总分和单题分值最多保留两位小数");
    }
  }

  private void saveSnapshotQuestion(Long tenantId, Long userId, Long snapshotId, int order,
      SelectedQuestion selected) {
    CompetitionQuestionEntity source = selected.question();
    BigDecimal point = selected.point();
    CompetitionMakeExamPaperQuestionEntity question = new CompetitionMakeExamPaperQuestionEntity();
    question.setId(IdWorker.getId());
    question.setTenantId(tenantId);
    question.setPaperSnapshotId(snapshotId);
    question.setSourceQuestionId(source.getId());
    question.setBlueprintId(selected.blueprintId());
    question.setBlueprintNodeId(selected.blueprintNodeId());
    question.setBlueprintFullCode(selected.blueprintFullCode());
    question.setBlueprintPath(selected.blueprintPath());
    question.setSortOrder(order);
    question.setQuestionType(source.getQuestionType());
    question.setStem(source.getStem());
    question.setOptionsJson(source.getOptionsJson());
    question.setReferenceAnswer(source.getReferenceAnswer());
    question.setFillAnswer(source.getFillAnswer());
    question.setScore(point);
    question.setSourceTotalScore(selected.sourceTotalScore());
    question.setScoreScale(selected.scoreScale());
    question.setDifficulty(source.getDifficulty());
    if ("practical".equals(source.getContentType())) {
      CompetitionPracticalQuestionDetailEntity detail = practicalDetailMapper.selectByQuestion(tenantId, source.getId());
      validatePracticalScoringData(tenantId, source.getId(), detail, selected.sourceTotalScore(), point);
      if (detail != null) {
        question.setOperationContent(detail.getOperationContent());
        question.setStudentPrepare(detail.getStudentPrepare());
        question.setEquipmentPrepare(detail.getEquipmentPrepare());
        question.setScoreDifference(detail.getScoreDifference());
        question.setScoringMethod(normalizeSnapshotScoringMethod(detail.getScoringMethod()));
      }
      question.setScoringItemsJson(scoreItemsJson(tenantId, source.getId(), detail == null ? "deductive" : detail.getScoringMethod(),
          selected.scoreScale(), point));
      question.setPracticalDetailJson(practicalDetailJson(tenantId, source.getId(), detail,
          selected.scoreScale(), point));
    }
    question.setCreatedBy(userId);
    question.setUpdatedBy(userId);
    question.setDeleted(0);
    if (snapshotQuestionMapper.insert(question) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "竞赛试卷题目快照保存失败");
    }
  }

  private void saveDetails(Long tenantId, Long userId, Long planId, List<CompetitionMakeExamPlanDetailReq> requests) {
    if (requests == null || requests.isEmpty()) return;
    for (CompetitionMakeExamPlanDetailReq req : requests) {
      CompetitionMakeExamPlanDetailEntity detail = new CompetitionMakeExamPlanDetailEntity();
      detail.setId(IdWorker.getId());
      detail.setTenantId(tenantId);
      detail.setPlanId(planId);
      detail.setQuestionType(req.questionType().toLowerCase(Locale.ROOT));
      detail.setQuestionCount(req.questionCount());
      detail.setPoint(req.point());
      detail.setDifficultyStart(req.difficultyStart());
      detail.setDifficultyEnd(req.difficultyEnd());
      detail.setSortOrder(req.sortOrder());
      detail.setCreatedBy(userId);
      detail.setUpdatedBy(userId);
      detail.setDeleted(0);
      if (detailMapper.insert(detail) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "竞赛组卷明细保存失败");
      }
    }
  }

  private void validatePracticalScoringData(Long tenantId, Long questionId,
      CompetitionPracticalQuestionDetailEntity detail, BigDecimal sourceTotalScore,
      BigDecimal targetScore) {
    if (detail == null || detail.getDurationMinutes() == null || detail.getDurationMinutes() <= 0
        || detail.getTotalScore() == null || detail.getTotalScore().signum() <= 0
        || sourceTotalScore == null || sourceTotalScore.signum() <= 0
        || detail.getTotalScore().compareTo(sourceTotalScore) != 0) {
      throw new BusinessException(ErrorCode.CONFLICT, "实操题缺少合法的原始总分或参考时长配置");
    }
    if (targetScore == null || targetScore.signum() <= 0) {
      throw new BusinessException(ErrorCode.CONFLICT, "实操自动组卷单元分值必须为正数");
    }
    if (scoreItemMapper == null) {
      throw new BusinessException(ErrorCode.CONFLICT, "实操题评分项服务未就绪");
    }
    String scoringMethod = normalizeSnapshotScoringMethod(detail.getScoringMethod());
    if (scoringMethod == null) {
      throw new BusinessException(ErrorCode.CONFLICT, "实操题评分方式无效，无法生成组卷快照");
    }
    List<CompetitionPracticalQuestionScoreItemEntity> items = scoreItemMapper.selectByQuestion(tenantId, questionId);
    if (items == null || items.isEmpty()) {
      throw new BusinessException(ErrorCode.CONFLICT, "实操题缺少评分项，无法生成组卷快照");
    }
    BigDecimal ordinaryTotal = BigDecimal.ZERO;
    int ordinaryCount = 0;
    Set<Integer> itemNos = new HashSet<>();
    for (CompetitionPracticalQuestionScoreItemEntity item : items) {
      if (item == null || item.getItemNo() == null || item.getItemNo() <= 0 || !itemNos.add(item.getItemNo())) {
        throw new BusinessException(ErrorCode.CONFLICT, "实操题评分项编号无效或重复，无法生成组卷快照");
      }
      if (Integer.valueOf(0).equals(item.getAllocationEnabled())) {
        if (item.getMaxScore() != null || item.getMaxDeduction() != null || item.getDeductionStep() != null) {
          throw new BusinessException(ErrorCode.CONFLICT, "实操题不配分评分要素不能携带分值规则");
        }
        continue;
      }
      if (item.getMaxScore() == null || item.getMaxScore().signum() <= 0) {
        throw new BusinessException(ErrorCode.CONFLICT, "实操题评分项规则无效，无法生成组卷快照");
      }
      if ("deductive".equals(scoringMethod)) {
        if (item.getMaxDeduction() == null || item.getMaxDeduction().signum() < 0
            || item.getMaxDeduction().compareTo(item.getMaxScore()) > 0
            || item.getDeductionStep() == null || item.getDeductionStep().signum() <= 0
            || item.getMaxDeduction().remainder(item.getDeductionStep()).signum() != 0) {
          throw new BusinessException(ErrorCode.CONFLICT, "实操题扣分制评分项规则无效，无法生成组卷快照");
        }
      } else if (item.getMaxDeduction() != null || item.getDeductionStep() != null) {
        throw new BusinessException(ErrorCode.CONFLICT, "实操题加分制评分项不能携带扣分规则");
      }
      ordinaryTotal = ordinaryTotal.add(item.getMaxScore());
      ordinaryCount++;
    }
    if (ordinaryCount == 0 || ordinaryTotal.compareTo(sourceTotalScore) != 0) {
      throw new BusinessException(ErrorCode.CONFLICT,
          "实操题评分项普通评分分值合计" + ordinaryTotal + "不等于原始总分" + sourceTotalScore);
    }
    validateScaledPracticalItems(items, scoringMethod, sourceTotalScore, targetScore);
  }

  private void validateScaledPracticalItems(List<CompetitionPracticalQuestionScoreItemEntity> items,
      String scoringMethod, BigDecimal sourceTotalScore, BigDecimal targetScore) {
    BigDecimal scale = targetScore.divide(sourceTotalScore, 12, RoundingMode.HALF_UP);
    BigDecimal scaledTotal = BigDecimal.ZERO;
    BigDecimal lastScore = null;
    int ordinaryCount = 0;
    for (CompetitionPracticalQuestionScoreItemEntity item : items) {
      if (item == null || Integer.valueOf(0).equals(item.getAllocationEnabled())) continue;
      BigDecimal scaledScore = item.getMaxScore().multiply(scale).setScale(2, RoundingMode.HALF_UP);
      if (scaledScore.signum() <= 0) {
        throw new BusinessException(ErrorCode.CONFLICT,
            "实操题缩放后评分项分值小于0.01，无法保证快照可执行");
      }
      scaledTotal = scaledTotal.add(scaledScore);
      lastScore = scaledScore;
      ordinaryCount++;
    }
    if (ordinaryCount == 0 || lastScore == null) {
      throw new BusinessException(ErrorCode.CONFLICT, "实操题没有可配分的普通评分项");
    }
    BigDecimal adjustedLast = lastScore.add(targetScore.subtract(scaledTotal)).setScale(2, RoundingMode.HALF_UP);
    if (adjustedLast.signum() <= 0) {
      throw new BusinessException(ErrorCode.CONFLICT,
          "实操题缩放后的最后评分项分值无效，无法生成快照");
    }
    if ("deductive".equals(scoringMethod)) {
      // scaleDeductionStep() guarantees a positive cent-level step; the
      // detailed snapshot validator below checks the final normalized rows.
      if (scale.signum() <= 0) {
        throw new BusinessException(ErrorCode.CONFLICT, "实操题评分缩放比例无效");
      }
    }
  }

  /**
   * Persists only the theory distribution that was actually used by the snapshot.
   * A zero-count range is intentionally omitted because the existing table keeps
   * score positive; the authoritative complete distribution remains in the
   * snapshot/result and is never reconstructed from user input.
   */
  private void saveAutoTheoryTopicRules(Long tenantId, Long userId, Long planId, Long blueprintId,
      List<SelectedQuestion> selectedQuestions) {
    if (topicRuleMapper == null) return;
    BlueprintTopology topology = loadTopology(tenantId, blueprintId);
    Map<Long, Integer> counts = new HashMap<>();
    Map<Long, BigDecimal> scores = new HashMap<>();
    for (SelectedQuestion selected : selectedQuestions) {
      Long topicId = selected.blueprintNodeId() == null
          ? null : topology.theoryTopicForPoint().get(selected.blueprintNodeId());
      if (topicId == null) {
        throw new BusinessException(ErrorCode.CONFLICT, "理论快照题目未归属到nodeLevel=3三级考评范围");
      }
      counts.merge(topicId, 1, Integer::sum);
      scores.merge(topicId, selected.point(), BigDecimal::add);
    }
    for (int index = 0; index < topology.theoryTopicNodes().size(); index++) {
      CompetitionQuestionBlueprintNodeEntity topic = topology.theoryTopicNodes().get(index);
      int count = counts.getOrDefault(topic.getId(), 0);
      if (count <= 0) continue;
      CompetitionMakeExamPlanTopicRuleEntity rule = new CompetitionMakeExamPlanTopicRuleEntity();
      rule.setId(IdWorker.getId());
      rule.setTenantId(tenantId);
      rule.setPlanId(planId);
      rule.setBlueprintNodeId(topic.getId());
      rule.setQuestionCount(count);
      rule.setScore(scores.get(topic.getId()));
      rule.setSelected(1);
      rule.setSortOrder(index + 1);
      rule.setCreatedBy(userId);
      rule.setUpdatedBy(userId);
      rule.setDeleted(0);
      if (topicRuleMapper.insert(rule) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "理论自动主题规则保存失败");
      }
    }
  }

  /** Persists the first generated practical paper as an audit projection only. */
  private void saveAutoPracticalTopicRules(Long tenantId, Long userId, Long planId,
      CompetitionPracticalMakeExamAllocator.AllocationResult allocation) {
    if (topicRuleMapper == null || allocation == null) return;
    int order = 1;
    for (CompetitionPracticalMakeExamAllocator.UnitAllocation unit : allocation.units()) {
      if (unit.structureNodeId() == null || unit.targetScore() == null) continue;
      CompetitionMakeExamPlanTopicRuleEntity rule = new CompetitionMakeExamPlanTopicRuleEntity();
      rule.setId(IdWorker.getId());
      rule.setTenantId(tenantId);
      rule.setPlanId(planId);
      rule.setBlueprintNodeId(unit.structureNodeId());
      rule.setQuestionCount(1);
      rule.setScore(unit.targetScore());
      rule.setSelected(1);
      rule.setSortOrder(order++);
      rule.setCreatedBy(userId);
      rule.setUpdatedBy(userId);
      rule.setDeleted(0);
      if (topicRuleMapper.insert(rule) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "实操自动组卷结果保存失败");
      }
    }
  }

  private void warnIgnoredTheoryTopicRules(CompetitionMakeExamPlanUpsertReq req) {
    if (req != null && req.topicRules() != null && !req.topicRules().isEmpty()
        && "theory".equalsIgnoreCase(req.contentType())) {
      log.warn("Ignoring {} theory topicRules: theory distribution is calculated from level-3 blueprint weights",
          req.topicRules().size());
    }
  }

  private void warnIgnoredPracticalTopicRules(CompetitionMakeExamPlanUpsertReq req) {
    if (req != null && req.topicRules() != null && !req.topicRules().isEmpty()
        && "practical".equalsIgnoreCase(req.contentType())) {
      log.warn("Ignoring {} practical topicRules: blueprint selection groups or legacy direct-question compatibility are authoritative",
          req.topicRules().size());
    }
  }

  private CompetitionMakeExamPlanUpsertReq lockDraftBlueprint(
      CompetitionMakeExamPlanEntity plan, CompetitionMakeExamPlanUpsertReq req) {
    if (plan.getBlueprintId() == null) return req;
    if (req.blueprintId() != null && !plan.getBlueprintId().equals(req.blueprintId())) {
      throw new BusinessException(ErrorCode.CONFLICT, "历史草稿已锁定原细目表版本，不能切换到其他版本");
    }
    if (req.blueprintId() != null) return req;
    return new CompetitionMakeExamPlanUpsertReq(req.eventWorkTypeId(), req.eventWorkTypeLevelId(),
        req.contentType(), req.title(), req.examType(), req.durationMinutes(), req.totalScore(),
        req.passScore(), req.comments(), req.details(), req.practicalQuestionId(), plan.getBlueprintId(),
        req.topicRules(), req.paperCount(), req.randomSeed(), req.practicalQuestionIds());
  }

  private void requireBlueprintScope(Long tenantId, Long blueprintId, Long eventId,
      Long workTypeId, Long levelId, String contentType) {
    CompetitionQuestionBlueprintEntity blueprint = blueprintService.findForEvent(tenantId, eventId, blueprintId);
    if (!workTypeId.equals(blueprint.getEventWorkTypeId())
        || !levelId.equals(blueprint.getEventWorkTypeLevelId())
        || !contentType.equalsIgnoreCase(blueprint.getContentType())
        || !("active".equals(blueprint.getStatus()) || "archived".equals(blueprint.getStatus()))) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "所选细目表版本与当前赛事工种等级/内容类型不匹配或尚未完成导入");
    }
  }

  private int nextPlanVersion(
      Long tenantId,
      Long eventId,
      Long eventWorkTypeId,
      Long eventWorkTypeLevelId,
      String contentType,
      String planPurpose) {
    Integer maxVersion = PURPOSE_EXAM_PLAN.equals(planPurpose)
        ? planMapper.selectMaxVersion(tenantId, eventId, eventWorkTypeId, eventWorkTypeLevelId, contentType)
        : planMapper.selectMaxVersionByPurpose(
            tenantId, eventId, eventWorkTypeId, eventWorkTypeLevelId, contentType, planPurpose);
    return (maxVersion == null ? 0 : maxVersion) + 1;
  }

  private void validatePlan(Long tenantId, Long eventId, CompetitionMakeExamPlanUpsertReq req) {
    validatePlan(tenantId, eventId, req, false);
  }

  private void validatePlan(Long tenantId, Long eventId, CompetitionMakeExamPlanUpsertReq req,
      boolean allowLockedBlueprint) {
    requireScope(tenantId, eventId, req.eventWorkTypeId(), req.eventWorkTypeLevelId());
    String contentType = normalizeContentType(req.contentType());
    warnIgnoredTheoryTopicRules(req);
    warnIgnoredPracticalTopicRules(req);
    if (req.durationMinutes() == null || req.totalScore() == null || req.passScore() == null
        || req.durationMinutes() <= 0 || req.totalScore().signum() <= 0
        || req.passScore().signum() < 0 || req.passScore().compareTo(req.totalScore()) > 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "考试时长、及格分或总分设置无效");
    }
    scoreCents(req.totalScore());
    normalizePaperCount(req, contentType);
    if (isPracticalScoringSchemeRequest(req)) {
      validatePracticalScoringScheme(tenantId, eventId, req, contentType);
      return;
    }
    if (req.blueprintId() != null) {
      if (blueprintService == null) throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "竞赛细目表服务未就绪");
      if (allowLockedBlueprint) {
        requireBlueprintScope(tenantId, req.blueprintId(), eventId, req.eventWorkTypeId(),
            req.eventWorkTypeLevelId(), contentType);
      } else {
        blueprintService.requireActive(tenantId, req.blueprintId(), eventId, req.eventWorkTypeId(),
            req.eventWorkTypeLevelId(), contentType);
      }
      if ("practical".equals(contentType)) {
        // The practical blueprint, rather than request details/topicRules, is
        // the sole source of structure, selection mode, score weight and
        // question count. Candidate and scoring validation is performed by the
        // same allocator during precheck and snapshot generation.
        return;
      } else {
        // Theory topicRules are a legacy input only. The level-3 blueprint
        // weights are authoritative and must be the only distribution input.
        validateDefaultTheoryTopicRules(tenantId, req);
      }
    }
    if ("practical".equals(contentType) && req.blueprintId() == null
        && req.practicalQuestionId() != null) {
      CompetitionQuestionEntity selected = questionMapper.selectOne(new LambdaQueryWrapper<CompetitionQuestionEntity>()
          .eq(CompetitionQuestionEntity::getTenantId, tenantId)
          .eq(CompetitionQuestionEntity::getEventId, eventId)
          .eq(CompetitionQuestionEntity::getEventWorkTypeId, req.eventWorkTypeId())
          .eq(CompetitionQuestionEntity::getEventWorkTypeLevelId, req.eventWorkTypeLevelId())
          .eq(CompetitionQuestionEntity::getContentType, "practical")
          .eq(CompetitionQuestionEntity::getQuestionType, "practical")
          .eq(CompetitionQuestionEntity::getStatus, "enabled")
          .eq(CompetitionQuestionEntity::getId, req.practicalQuestionId())
          .eq(CompetitionQuestionEntity::getDeleted, 0));
      if (selected == null) {
        throw new BusinessException(ErrorCode.NOT_FOUND, "指定的实操题不存在、已停用或不属于当前赛事工种等级");
      }
      CompetitionPracticalQuestionDetailEntity practicalDetail = practicalDetailMapper.selectByQuestion(tenantId, selected.getId());
       if (practicalDetail == null || practicalDetail.getDurationMinutes() == null
           || practicalDetail.getDurationMinutes() <= 0 || practicalDetail.getTotalScore() == null
           || practicalDetail.getTotalScore().signum() <= 0) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "指定实操题缺少考试时间或总分配置");
      }
      if (req.durationMinutes() == null || !req.durationMinutes().equals(practicalDetail.getDurationMinutes())
          || req.totalScore().compareTo(practicalDetail.getTotalScore()) != 0) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "实操组卷的考试时间和总分必须取所选实操题配置");
      }
      if (req.details() == null || req.details().size() != 1) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "实操组卷固定只生成一条实操题快照");
      }
      CompetitionMakeExamPlanDetailReq practicalRule = req.details().get(0);
      if (!"practical".equalsIgnoreCase(practicalRule.questionType())
          || practicalRule.questionCount() == null || practicalRule.questionCount() != 1
          || practicalRule.point() == null || practicalRule.point().compareTo(practicalDetail.getTotalScore()) != 0) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "实操组卷不允许编辑题量和单题分值，必须与所选题目总分一致");
      }
    }
    if ("practical".equals(contentType) && req.blueprintId() == null
        && req.practicalQuestionId() == null
        && (req.details() == null || req.details().size() != 1)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "简易实操组卷只允许配置一条实操题型规则");
    }
    Set<Integer> orders = new HashSet<>();
    if (req.details() == null || req.details().isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "至少需要配置一条组卷规则");
    }
    BigDecimal rulesTotal = BigDecimal.ZERO;
    for (CompetitionMakeExamPlanDetailReq detail : req.details()) {
      if (detail == null || detail.sortOrder() == null || detail.questionCount() == null
          || detail.point() == null || !orders.add(detail.sortOrder())
          || detail.questionCount() <= 0 || detail.point().signum() <= 0) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "组卷明细序号不能重复，题量和单题分值必须为正数");
      }
      if (!StringUtils.hasText(detail.questionType())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "组卷明细题型不能为空");
      }
      String type = detail.questionType().toLowerCase(Locale.ROOT);
      boolean allowed = "theory".equals(contentType)
          ? Set.of("single", "multi", "judge", "fill").contains(type)
          : "practical".equals(type);
      if (!allowed) throw new BusinessException(ErrorCode.BAD_REQUEST, "组卷明细题型与理论/实操类型不匹配");
      if (detail.difficultyStart() != null && (detail.difficultyStart() < 1 || detail.difficultyStart() > 5)
          || detail.difficultyEnd() != null && (detail.difficultyEnd() < 1 || detail.difficultyEnd() > 5)
          || detail.difficultyStart() != null && detail.difficultyEnd() != null && detail.difficultyStart() > detail.difficultyEnd()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "难度范围无效");
      }
      scoreCents(detail.point());
      rulesTotal = rulesTotal.add(detail.point().multiply(BigDecimal.valueOf(detail.questionCount())));
    }
    if (rulesTotal.compareTo(req.totalScore()) != 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "组卷规则合计分值 " + rulesTotal + " 不等于计划总分 " + req.totalScore());
    }
  }

  private void validatePracticalScoringScheme(Long tenantId, Long eventId,
      CompetitionMakeExamPlanUpsertReq req, String contentType) {
    if (!"practical".equals(contentType) || req.blueprintId() != null
        || req.practicalQuestionId() != null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "实操评分方案只能选择实操题库项目，不能绑定细目表或单题组卷");
    }
    List<Long> questionIds = req.practicalQuestionIds();
    if (questionIds == null || questionIds.isEmpty() || questionIds.size() > 50) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "实操评分方案需选择1到50个实操项目");
    }
    if (req.details() == null || !req.details().isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "实操评分方案不配置随机抽题规则");
    }
    if (normalizePaperCount(req, contentType) != 1) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "实操评分方案固定生成一份评分快照");
    }
    if (req.totalScore().compareTo(new BigDecimal("100.00")) != 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "实操评分方案总分固定为100分");
    }
    List<SelectedQuestion> selected = selectedPracticalScoringSchemeQuestions(tenantId, eventId,
        req.eventWorkTypeId(), req.eventWorkTypeLevelId(), questionIds);
    BigDecimal scoreTotal = selected.stream().map(SelectedQuestion::point)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    int durationTotal = 0;
    for (SelectedQuestion item : selected) {
      CompetitionPracticalQuestionDetailEntity detail =
          practicalDetailMapper.selectByQuestion(tenantId, item.question().getId());
      durationTotal += detail.getDurationMinutes();
    }
    if (scoreTotal.compareTo(req.totalScore()) != 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "所选实操项目原始分值合计" + scoreTotal + "分，须刚好为100分；不进行分值缩放");
    }
    if (req.durationMinutes() != durationTotal) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "方案参考时长须等于所选实操项目参考时长合计" + durationTotal + "分钟");
    }
  }

  private List<SelectedQuestion> selectedPracticalScoringSchemeQuestions(Long tenantId, Long eventId,
      Long workTypeId, Long levelId, List<Long> questionIds) {
    if (questionIds == null || questionIds.isEmpty() || questionIds.size() > 50) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "实操评分方案需选择1到50个实操项目");
    }
    Set<Long> seen = new HashSet<>();
    List<SelectedQuestion> selected = new ArrayList<>();
    for (Long questionId : questionIds) {
      if (questionId == null || !seen.add(questionId)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "实操评分方案项目不能为空或重复");
      }
      CompetitionQuestionEntity question = questionMapper.selectOne(
          new LambdaQueryWrapper<CompetitionQuestionEntity>()
              .eq(CompetitionQuestionEntity::getTenantId, tenantId)
              .eq(CompetitionQuestionEntity::getEventId, eventId)
              .eq(CompetitionQuestionEntity::getEventWorkTypeId, workTypeId)
              .eq(CompetitionQuestionEntity::getEventWorkTypeLevelId, levelId)
              .eq(CompetitionQuestionEntity::getContentType, "practical")
              .eq(CompetitionQuestionEntity::getQuestionType, "practical")
              .eq(CompetitionQuestionEntity::getStatus, "enabled")
              .eq(CompetitionQuestionEntity::getId, questionId)
              .eq(CompetitionQuestionEntity::getDeleted, 0));
      if (question == null) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
            "所选实操项目不存在、已停用或不属于当前赛事工种等级");
      }
      CompetitionPracticalQuestionDetailEntity detail = practicalDetailMapper.selectByQuestion(
          tenantId, questionId);
      if (detail == null) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "所选实操项目缺少评分配置");
      }
      validatePracticalScoringData(tenantId, questionId, detail,
          detail.getTotalScore(), detail.getTotalScore());
      selected.add(new SelectedQuestion(question, detail.getTotalScore(), null, null,
          null, null, detail.getTotalScore(), BigDecimal.ONE));
    }
    return selected;
  }

  private boolean isPracticalScoringSchemeRequest(CompetitionMakeExamPlanUpsertReq req) {
    return req != null && req.practicalQuestionIds() != null;
  }

  private void validateDefaultTheoryTopicRules(Long tenantId, CompetitionMakeExamPlanUpsertReq req) {
    BlueprintTopology topology = loadTopology(tenantId, req.blueprintId());
    // Reuse the same topology and matrix validation as snapshot generation so
    // malformed weights fail before a draft row is inserted.
    try {
      allocateTheory(topology, req.details(), List.of(), new HashMap<>(), 0L);
    } catch (IllegalArgumentException ex) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          ex.getMessage() == null ? "理论细目表三级权重无效" : ex.getMessage());
    }
  }

  private CompetitionMakeExamPlanEntity findPlan(Long tenantId, Long eventId, Long planId) {
    CompetitionMakeExamPlanEntity plan = planMapper.selectOne(new LambdaQueryWrapper<CompetitionMakeExamPlanEntity>()
        .eq(CompetitionMakeExamPlanEntity::getTenantId, tenantId)
        .eq(CompetitionMakeExamPlanEntity::getEventId, eventId)
        .eq(CompetitionMakeExamPlanEntity::getId, planId)
        .eq(CompetitionMakeExamPlanEntity::getDeleted, 0));
    if (plan == null) throw new BusinessException(ErrorCode.NOT_FOUND, "竞赛组卷计划不存在");
    return plan;
  }

  private void requireEvent(Long tenantId, Long eventId) {
    if (eventMapper.selectOne(new LambdaQueryWrapper<CompetitionEventEntity>()
        .eq(CompetitionEventEntity::getTenantId, tenantId).eq(CompetitionEventEntity::getId, eventId)
        .eq(CompetitionEventEntity::getDeleted, 0)) == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "赛事不存在");
    }
  }

  private void requireScope(Long tenantId, Long eventId, Long workTypeId, Long levelId) {
    requireEvent(tenantId, eventId);
    CompetitionEventWorkTypeEntity workType = workTypeMapper.selectOne(new LambdaQueryWrapper<CompetitionEventWorkTypeEntity>()
        .eq(CompetitionEventWorkTypeEntity::getTenantId, tenantId).eq(CompetitionEventWorkTypeEntity::getEventId, eventId)
        .eq(CompetitionEventWorkTypeEntity::getId, workTypeId).eq(CompetitionEventWorkTypeEntity::getDeleted, 0));
    CompetitionEventWorkTypeLevelEntity level = levelMapper.selectOne(new LambdaQueryWrapper<CompetitionEventWorkTypeLevelEntity>()
        .eq(CompetitionEventWorkTypeLevelEntity::getTenantId, tenantId).eq(CompetitionEventWorkTypeLevelEntity::getEventId, eventId)
        .eq(CompetitionEventWorkTypeLevelEntity::getEventWorkTypeId, workTypeId).eq(CompetitionEventWorkTypeLevelEntity::getId, levelId)
        .eq(CompetitionEventWorkTypeLevelEntity::getDeleted, 0));
    if (workType == null || level == null) throw new BusinessException(ErrorCode.NOT_FOUND, "赛事工种或等级不存在");
  }

  private CompetitionMakeExamPlanVO toPlanVO(Long tenantId, CompetitionMakeExamPlanEntity plan) {
    List<CompetitionMakeExamPlanDetailEntity> details = detailMapper.selectByPlan(tenantId, plan.getId());
    List<CompetitionMakeExamPlanDetailVO> detailVOs = new ArrayList<>();
    if (details != null) for (CompetitionMakeExamPlanDetailEntity detail : details) detailVOs.add(new CompetitionMakeExamPlanDetailVO(
        String.valueOf(detail.getId()), detail.getQuestionType(), detail.getQuestionCount(), detail.getPoint(),
        detail.getDifficultyStart(), detail.getDifficultyEnd(), detail.getSortOrder()));
    List<CompetitionMakeExamPlanTopicRuleVO> topicRuleVOs = new ArrayList<>();
    for (CompetitionMakeExamPlanTopicRuleEntity rule : loadTopicRules(tenantId, plan.getId())) {
      topicRuleVOs.add(new CompetitionMakeExamPlanTopicRuleVO(String.valueOf(rule.getId()),
          String.valueOf(rule.getBlueprintNodeId()), rule.getQuestionCount(), rule.getScore(),
          Integer.valueOf(1).equals(rule.getSelected()), rule.getSortOrder()));
    }
    List<String> practicalQuestionIds = new ArrayList<>();
    if (PURPOSE_PRACTICAL_SCORING_SCHEME.equals(plan.getPlanPurpose())) {
      List<CompetitionMakeExamPaperSnapshotEntity> snapshots = snapshotMapper.selectByPlan(tenantId, plan.getId());
      CompetitionMakeExamPaperSnapshotEntity activeSnapshot = snapshots == null ? null : snapshots.stream()
          .filter(snapshot -> "P".equals(snapshot.getPaperCode())
              && ("published".equals(plan.getStatus())
                  ? "published".equals(snapshot.getStatus()) : "draft".equals(snapshot.getStatus())))
          .findFirst().orElse(null);
      if (activeSnapshot != null) {
        List<CompetitionMakeExamPaperQuestionEntity> snapshotQuestions =
            snapshotQuestionMapper.selectByPaper(tenantId, activeSnapshot.getId());
        if (snapshotQuestions != null) snapshotQuestions.forEach(question ->
            practicalQuestionIds.add(String.valueOf(question.getSourceQuestionId())));
      }
    }
    return new CompetitionMakeExamPlanVO(String.valueOf(plan.getId()), String.valueOf(plan.getEventId()),
        String.valueOf(plan.getEventWorkTypeId()), String.valueOf(plan.getEventWorkTypeLevelId()),
        plan.getPracticalQuestionId() == null ? null : String.valueOf(plan.getPracticalQuestionId()), plan.getContentType(),
        plan.getTitle(), plan.getExamType(), plan.getDurationMinutes(), plan.getTotalScore(), plan.getPassScore(),
         plan.getPaperCount(), plan.getPlanVersion(), plan.getStatus(), plan.getComments(), plan.getCreatedAt(), plan.getUpdatedAt(), detailVOs,
         plan.getBlueprintId() == null ? null : String.valueOf(plan.getBlueprintId()), topicRuleVOs,
         plan.getRandomSeed() == null ? null : String.valueOf(plan.getRandomSeed()),
         plan.getPlanPurpose() == null ? PURPOSE_EXAM_PLAN : plan.getPlanPurpose(), practicalQuestionIds);
  }

  private CompetitionMakeExamSnapshotVO toSnapshotVO(Long tenantId, CompetitionMakeExamPaperSnapshotEntity snapshot) {
    List<CompetitionMakeExamPaperQuestionEntity> questions = snapshotQuestionMapper.selectByPaper(tenantId, snapshot.getId());
    List<CompetitionMakeExamSnapshotQuestionVO> questionVOs = new ArrayList<>();
    if (questions != null) for (CompetitionMakeExamPaperQuestionEntity question : questions) questionVOs.add(new CompetitionMakeExamSnapshotQuestionVO(
        String.valueOf(question.getId()), String.valueOf(question.getSourceQuestionId()), question.getSortOrder(), question.getQuestionType(),
        question.getStem(), question.getOptionsJson(), question.getReferenceAnswer(), question.getFillAnswer(), question.getScore(), question.getDifficulty(),
        question.getOperationContent(), question.getStudentPrepare(), question.getEquipmentPrepare(), question.getScoringItemsJson(),
        question.getPracticalDetailJson(), question.getScoreDifference(), question.getScoringMethod(),
        question.getBlueprintId() == null ? null : String.valueOf(question.getBlueprintId()),
        question.getBlueprintNodeId() == null ? null : String.valueOf(question.getBlueprintNodeId()),
        question.getBlueprintFullCode(), question.getBlueprintPath(), question.getSourceTotalScore(), question.getScoreScale()));
    return new CompetitionMakeExamSnapshotVO(String.valueOf(snapshot.getId()), String.valueOf(snapshot.getPlanId()),
        snapshot.getBlueprintId() == null ? null : String.valueOf(snapshot.getBlueprintId()), String.valueOf(snapshot.getEventId()),
        String.valueOf(snapshot.getEventWorkTypeId()), String.valueOf(snapshot.getEventWorkTypeLevelId()), snapshot.getContentType(), snapshot.getPaperCode(),
        snapshot.getVersionNo(), snapshot.getTitle(), snapshot.getDurationMinutes(), snapshot.getQuestionCount(), snapshot.getTotalScore(),
        snapshot.getPassScore(), snapshot.getStatus(), questionVOs);
  }

  private String scoreItemsJson(Long tenantId, Long questionId, String scoringMethod) {
    return scoreItemsJson(tenantId, questionId, scoringMethod, null, null);
  }

  private String scoreItemsJson(Long tenantId, Long questionId, String scoringMethod,
      BigDecimal scale, BigDecimal targetScore) {
    String normalizedScoringMethod = normalizeSnapshotScoringMethod(scoringMethod);
    if (normalizedScoringMethod == null) {
      throw new BusinessException(ErrorCode.CONFLICT, "实操题评分方式无效，无法生成组卷快照");
    }
    List<CompetitionPracticalQuestionScoreItemEntity> items = scoreItemMapper.selectByQuestion(tenantId, questionId);
    if (items == null || items.isEmpty()) {
      throw new BusinessException(ErrorCode.CONFLICT, "实操题缺少评分项，无法生成组卷快照");
    }
    List<Map<String, Object>> allocated = new ArrayList<>();
    BigDecimal allocatedTotal = BigDecimal.ZERO;
    for (CompetitionPracticalQuestionScoreItemEntity item : items) {
      if (item == null || Integer.valueOf(0).equals(item.getAllocationEnabled())) continue;
      if (item.getMaxScore() == null || item.getMaxScore().signum() <= 0) {
        throw new BusinessException(ErrorCode.CONFLICT, "实操题评分项规则无效，无法生成组卷快照");
      }
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("itemNo", item.getItemNo());
      row.put("itemName", item.getItemName());
      row.put("criterion", item.getCriterion());
      row.put("elementHtml", item.getElementHtml());
      row.put("standardHtml", item.getStandardHtml());
      BigDecimal scaledScore = scale == null ? item.getMaxScore() : item.getMaxScore().multiply(scale).setScale(2, RoundingMode.HALF_UP);
      BigDecimal scaledStep = scaleDeductionStep(item.getDeductionStep(), scale);
      BigDecimal scaledDeduction = scaleDeduction(item.getMaxDeduction(), scaledStep, scale, scaledScore);
      row.put("maxScore", scaledScore);
      row.put("maxDeduction", scaledDeduction);
      row.put("deductionStep", scaledStep);
      row.put("scoringMethod", normalizedScoringMethod);
      row.put("allocationEnabled", true);
      allocated.add(row);
      allocatedTotal = allocatedTotal.add(scaledScore);
    }
    if (allocated.isEmpty()) {
      throw new BusinessException(ErrorCode.CONFLICT, "实操题评分项没有有效分值，无法生成组卷快照");
    }
    if (scale != null && targetScore != null) {
      Map<String, Object> last = allocated.get(allocated.size() - 1);
      BigDecimal adjusted = ((BigDecimal) last.get("maxScore")).add(targetScore.subtract(allocatedTotal)).setScale(2, RoundingMode.HALF_UP);
      last.put("maxScore", adjusted);
      normalizeSnapshotDeduction(last);
    }
    validateSnapshotScoringRows(allocated, normalizedScoringMethod, targetScore);
    try {
      return objectMapper.writeValueAsString(allocated);
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.CONFLICT, "实操题评分项快照序列化失败");
    }
  }

  private void validateSnapshotScoringRows(List<Map<String, Object>> rows, String scoringMethod,
      BigDecimal targetScore) {
    Set<Integer> itemNos = new HashSet<>();
    BigDecimal total = BigDecimal.ZERO;
    for (Map<String, Object> row : rows) {
      Object itemNo = row.get("itemNo");
      if (!(itemNo instanceof Number number) || number.intValue() <= 0
          || !itemNos.add(number.intValue())) {
        throw new BusinessException(ErrorCode.CONFLICT, "实操题快照评分项编号无效或重复");
      }
      Object rawScore = row.get("maxScore");
      if (!(rawScore instanceof BigDecimal maxScore) || maxScore.signum() <= 0) {
        throw new BusinessException(ErrorCode.CONFLICT, "实操题快照评分项缩放后分值无效");
      }
      total = total.add(maxScore);
      BigDecimal maxDeduction = row.get("maxDeduction") instanceof BigDecimal value ? value : null;
      BigDecimal deductionStep = row.get("deductionStep") instanceof BigDecimal value ? value : null;
      if ("additive".equals(scoringMethod)) {
        if (maxDeduction != null || deductionStep != null) {
          throw new BusinessException(ErrorCode.CONFLICT, "加分制快照评分项不能包含扣分规则");
        }
      } else if (maxDeduction == null || deductionStep == null || maxDeduction.signum() < 0
          || maxDeduction.compareTo(maxScore) > 0 || deductionStep.signum() <= 0
          || maxDeduction.remainder(deductionStep).signum() != 0) {
        throw new BusinessException(ErrorCode.CONFLICT, "扣分制快照评分项扣分规则无效");
      }
    }
    if (targetScore != null && total.compareTo(targetScore) != 0) {
      throw new BusinessException(ErrorCode.CONFLICT,
          "实操题快照评分项合计" + total + "不等于目标分值" + targetScore);
    }
  }

  private String normalizeSnapshotScoringMethod(String value) {
    if (value == null || value.isBlank()) return "deductive";
    return switch (value.trim().toLowerCase(Locale.ROOT)) {
      case "additive", "add", "加分", "加分制" -> "additive";
      case "deductive", "deduct", "扣分", "扣分制" -> "deductive";
      default -> null;
    };
  }

  private String practicalDetailJson(Long tenantId, Long questionId,
      CompetitionPracticalQuestionDetailEntity detail) {
    return practicalDetailJson(tenantId, questionId, detail, null, null);
  }

  private String practicalDetailJson(Long tenantId, Long questionId,
      CompetitionPracticalQuestionDetailEntity detail, BigDecimal scale, BigDecimal targetScore) {
    if (detail == null) return null;
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("durationMinutes", detail.getDurationMinutes());
    result.put("scoreDifference", detail.getScoreDifference());
    result.put("totalScore", targetScore == null ? detail.getTotalScore() : targetScore);
    result.put("scoringMethod", normalizeSnapshotScoringMethod(detail.getScoringMethod()));
    result.put("assessmentRequirementsHtml", detail.getAssessmentRequirementsHtml());
    result.put("examRoomPrepareHtml", detail.getExamRoomPrepareHtml());
    result.put("candidatePrepareHtml", detail.getCandidatePrepareHtml());
    result.put("negativeItemsHtml", detail.getNegativeItemsHtml());
    result.put("questionMaterialsHtml", detail.getQuestionMaterialsHtml());
    List<Map<String, Object>> contents = new ArrayList<>();
    List<Map<String, Object>> allocatedElements = new ArrayList<>();
    BigDecimal allocatedTotal = BigDecimal.ZERO;
    if (assessmentContentMapper != null) {
      List<CompetitionPracticalQuestionAssessmentContentEntity> rows = assessmentContentMapper.selectByQuestion(tenantId, questionId);
      if (rows != null) for (CompetitionPracticalQuestionAssessmentContentEntity row : rows) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("id", row.getId() == null ? null : String.valueOf(row.getId()));
        content.put("sortOrder", row.getSortOrder());
        content.put("contentHtml", row.getContentHtml());
        List<Map<String, Object>> elements = new ArrayList<>();
        if (scoreItemMapper != null) {
          List<CompetitionPracticalQuestionScoreItemEntity> items = scoreItemMapper.selectList(new LambdaQueryWrapper<CompetitionPracticalQuestionScoreItemEntity>()
              .eq(CompetitionPracticalQuestionScoreItemEntity::getTenantId, tenantId)
              .eq(CompetitionPracticalQuestionScoreItemEntity::getQuestionId, questionId)
              .eq(CompetitionPracticalQuestionScoreItemEntity::getContentId, row.getId())
              .eq(CompetitionPracticalQuestionScoreItemEntity::getDeleted, 0)
              .orderByAsc(CompetitionPracticalQuestionScoreItemEntity::getElementOrder));
          if (items != null) for (CompetitionPracticalQuestionScoreItemEntity item : items) {
            Map<String, Object> element = new LinkedHashMap<>();
            element.put("id", item.getId() == null ? null : String.valueOf(item.getId()));
            element.put("elementOrder", item.getElementOrder());
            element.put("elementHtml", item.getElementHtml());
            element.put("allocationEnabled", !Integer.valueOf(0).equals(item.getAllocationEnabled()));
            BigDecimal scaledElementScore = scale == null || item.getMaxScore() == null ? item.getMaxScore()
                : item.getMaxScore().multiply(scale).setScale(2, RoundingMode.HALF_UP);
            element.put("maxScore", scaledElementScore);
            element.put("standardHtml", item.getStandardHtml());
            BigDecimal scaledElementStep = scaleDeductionStep(item.getDeductionStep(), scale);
            element.put("maxDeduction", scaleDeduction(item.getMaxDeduction(), scaledElementStep, scale, scaledElementScore));
            element.put("deductionStep", scaledElementStep);
            elements.add(element);
            if (!Integer.valueOf(0).equals(item.getAllocationEnabled()) && element.get("maxScore") instanceof BigDecimal score) {
              allocatedElements.add(element);
              allocatedTotal = allocatedTotal.add(score);
            }
          }
        }
        content.put("elements", elements);
        contents.add(content);
      }
    }
    if (scale != null && targetScore != null && !allocatedElements.isEmpty()) {
      Map<String, Object> last = allocatedElements.get(allocatedElements.size() - 1);
      BigDecimal adjusted = ((BigDecimal) last.get("maxScore"))
          .add(targetScore.subtract(allocatedTotal)).setScale(2, RoundingMode.HALF_UP);
      last.put("maxScore", adjusted);
      normalizeSnapshotDeduction(last);
    }
    result.put("assessmentContents", contents);
    try {
      return objectMapper.writeValueAsString(result);
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.CONFLICT, "实操题结构快照序列化失败");
    }
  }

  private BigDecimal scaleDeductionStep(BigDecimal sourceStep, BigDecimal scale) {
    if (sourceStep == null || scale == null) return sourceStep;
    BigDecimal scaled = sourceStep.multiply(scale).setScale(2, RoundingMode.HALF_UP);
    return scaled.signum() <= 0 ? new BigDecimal("0.01") : scaled;
  }

  private BigDecimal scaleDeduction(BigDecimal sourceDeduction, BigDecimal scaledStep,
      BigDecimal scale, BigDecimal scaledScore) {
    if (sourceDeduction == null || scale == null) return sourceDeduction;
    BigDecimal scaled = sourceDeduction.multiply(scale).setScale(2, RoundingMode.HALF_UP);
    return normalizeDeduction(scaled, scaledStep, scaledScore);
  }

  private BigDecimal normalizeDeduction(BigDecimal deduction, BigDecimal step, BigDecimal maxScore) {
    if (deduction == null || step == null || step.signum() <= 0) return deduction;
    BigDecimal upper = maxScore == null ? deduction : deduction.min(maxScore);
    if (upper.signum() < 0) upper = BigDecimal.ZERO;
    BigDecimal units = upper.divide(step, 0, RoundingMode.FLOOR);
    return step.multiply(units).setScale(2, RoundingMode.HALF_UP);
  }

  @SuppressWarnings("unchecked")
  private void normalizeSnapshotDeduction(Map<String, Object> row) {
    Object deduction = row.get("maxDeduction");
    Object step = row.get("deductionStep");
    Object score = row.get("maxScore");
    if (deduction instanceof BigDecimal d && step instanceof BigDecimal s) {
      row.put("maxDeduction", normalizeDeduction(d, s, score instanceof BigDecimal m ? m : null));
    }
  }

  private String hashQuestionIds(List<SelectedQuestion> questions) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      for (SelectedQuestion question : questions) {
        digest.update((question.question().getId() + ":" + question.point() + "|").getBytes(StandardCharsets.UTF_8));
      }
      StringBuilder result = new StringBuilder();
      for (byte value : digest.digest()) result.append(String.format("%02x", value));
      return result.toString();
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 unavailable", ex);
    }
  }

  private String normalizeContentType(String value) {
    if (value == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "contentType 不能为空");
    String normalized = value.toLowerCase(Locale.ROOT);
    if (!Set.of("theory", "practical").contains(normalized)) throw new BusinessException(ErrorCode.BAD_REQUEST, "contentType 必须是 theory 或 practical");
    return normalized;
  }

  private boolean isBlueprintPractical(String contentType, Long blueprintId) {
    return blueprintId != null && "practical".equalsIgnoreCase(contentType);
  }

  private int normalizePaperCount(CompetitionMakeExamPlanUpsertReq req, String contentType) {
    int paperCount = req.paperCount() == null ? 1 : req.paperCount();
    if (paperCount <= 0 || paperCount > 50) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "生成试卷套数必须在1到50之间");
    }
    if (!("practical".equals(contentType) && req.blueprintId() != null) && paperCount != 1) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "当前组卷类型只允许生成一套试卷");
    }
    return paperCount;
  }

  private long practicalSeed(CompetitionMakeExamPlanUpsertReq req) {
    String value = String.join("|",
        String.valueOf(req.eventWorkTypeId()), String.valueOf(req.eventWorkTypeLevelId()),
        String.valueOf(req.blueprintId()), String.valueOf(req.contentType()), String.valueOf(req.title()),
        String.valueOf(req.durationMinutes()), String.valueOf(req.totalScore()),
        String.valueOf(req.paperCount() == null ? 1 : req.paperCount()));
    try {
      byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      long result = 0L;
      for (int index = 0; index < Math.min(8, bytes.length); index++) {
        result = (result << 8) | (bytes[index] & 0xffL);
      }
      return result;
    } catch (NoSuchAlgorithmException ex) {
      return value.hashCode();
    }
  }

  private long effectiveRequestSeed(CompetitionMakeExamPlanUpsertReq req) {
    return req.randomSeed() == null ? practicalSeed(req) : req.randomSeed();
  }

  private record SelectedQuestion(CompetitionQuestionEntity question, BigDecimal point,
      Long blueprintId, Long blueprintNodeId, String blueprintFullCode, String blueprintPath,
      BigDecimal sourceTotalScore, BigDecimal scoreScale) {
    private SelectedQuestion(CompetitionQuestionEntity question, BigDecimal point) {
      this(question, point, null, null, null, null, null, null);
    }
  }

  private String jsonEscape(String value) {
    if (value == null) return "";
    return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n");
  }

  private PlatformUserPrincipal currentUser() {
    try { return SecurityContext.currentUser(); }
    catch (IllegalStateException ex) { log.warn("Competition make plan request has no authenticated platform user"); throw ex; }
  }
}
