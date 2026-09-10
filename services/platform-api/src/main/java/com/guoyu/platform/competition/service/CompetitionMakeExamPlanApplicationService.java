package com.guoyu.platform.competition.service;

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
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPaperQuestionEntity;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPaperSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPlanDetailEntity;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPlanEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalQuestionDetailEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalQuestionScoreItemEntity;
import com.guoyu.platform.competition.entity.CompetitionQuestionEntity;
import com.guoyu.platform.competition.mapper.CompetitionEventMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeLevelMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
import com.guoyu.platform.competition.mapper.CompetitionMakeExamPaperQuestionMapper;
import com.guoyu.platform.competition.mapper.CompetitionMakeExamPaperSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionMakeExamPlanDetailMapper;
import com.guoyu.platform.competition.mapper.CompetitionMakeExamPlanMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalQuestionDetailMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalQuestionScoreItemMapper;
import com.guoyu.platform.competition.mapper.CompetitionQuestionMapper;
import com.guoyu.platform.competition.vo.CompetitionMakeExamPlanDetailVO;
import com.guoyu.platform.competition.vo.CompetitionMakeExamPlanVO;
import com.guoyu.platform.competition.vo.CompetitionMakeExamSnapshotQuestionVO;
import com.guoyu.platform.competition.vo.CompetitionMakeExamSnapshotVO;
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
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class CompetitionMakeExamPlanApplicationService {

  private final CompetitionMakeExamPlanMapper planMapper;
  private final CompetitionMakeExamPlanDetailMapper detailMapper;
  private final CompetitionMakeExamPaperSnapshotMapper snapshotMapper;
  private final CompetitionMakeExamPaperQuestionMapper snapshotQuestionMapper;
  private final CompetitionQuestionMapper questionMapper;
  private final CompetitionPracticalQuestionDetailMapper practicalDetailMapper;
  private final CompetitionPracticalQuestionScoreItemMapper scoreItemMapper;
  private final CompetitionEventMapper eventMapper;
  private final CompetitionEventWorkTypeMapper workTypeMapper;
  private final CompetitionEventWorkTypeLevelMapper levelMapper;

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
    this.planMapper = planMapper;
    this.detailMapper = detailMapper;
    this.snapshotMapper = snapshotMapper;
    this.snapshotQuestionMapper = snapshotQuestionMapper;
    this.questionMapper = questionMapper;
    this.practicalDetailMapper = practicalDetailMapper;
    this.scoreItemMapper = scoreItemMapper;
    this.eventMapper = eventMapper;
    this.workTypeMapper = workTypeMapper;
    this.levelMapper = levelMapper;
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

  @Transactional
  public CompetitionMakeExamPlanVO create(Long eventId, CompetitionMakeExamPlanUpsertReq req) {
    PlatformUserPrincipal principal = currentUser();
    validatePlan(principal.getTenantId(), eventId, req);
    CompetitionMakeExamPlanEntity plan = new CompetitionMakeExamPlanEntity();
    plan.setId(IdWorker.getId());
    plan.setTenantId(principal.getTenantId());
    plan.setEventId(eventId);
    plan.setEventWorkTypeId(req.eventWorkTypeId());
    plan.setEventWorkTypeLevelId(req.eventWorkTypeLevelId());
    String contentType = normalizeContentType(req.contentType());
    plan.setContentType(contentType);
    plan.setTitle(req.title());
    plan.setExamType(req.examType());
    plan.setDurationMinutes(req.durationMinutes());
    plan.setTotalScore(req.totalScore());
    plan.setPassScore(req.passScore());
    // A make-exam plan owns exactly one paper. Paper variants such as A/B are
    // created by separate plans and selected later in stage arrangement.
    plan.setPaperCount(1);
    plan.setPlanVersion(nextPlanVersion(principal.getTenantId(), eventId,
        req.eventWorkTypeId(), req.eventWorkTypeLevelId(), contentType));
    plan.setStatus("draft");
    plan.setComments(req.comments());
    plan.setCreatedBy(principal.getUserId());
    plan.setUpdatedBy(principal.getUserId());
    plan.setDeleted(0);
    if (planMapper.insert(plan) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "竞赛组卷计划保存失败");
    }
    saveDetails(principal.getTenantId(), principal.getUserId(), plan.getId(), req.details());
    generateDraftSnapshots(principal.getTenantId(), principal.getUserId(), plan, req.details());
    return toPlanVO(principal.getTenantId(), plan);
  }

  @Transactional
  public CompetitionMakeExamPlanVO update(Long eventId, Long planId, CompetitionMakeExamPlanUpsertReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionMakeExamPlanEntity plan = findPlan(principal.getTenantId(), eventId, planId);
    if (!"draft".equals(plan.getStatus())) throw new BusinessException(ErrorCode.CONFLICT, "已发布组卷计划不可直接修改，请新建版本");
    validatePlan(principal.getTenantId(), eventId, req);
    plan.setEventWorkTypeId(req.eventWorkTypeId());
    plan.setEventWorkTypeLevelId(req.eventWorkTypeLevelId());
    String contentType = normalizeContentType(req.contentType());
    plan.setContentType(contentType);
    plan.setTitle(req.title());
    plan.setExamType(req.examType());
    plan.setDurationMinutes(req.durationMinutes());
    plan.setTotalScore(req.totalScore());
    plan.setPassScore(req.passScore());
    plan.setPaperCount(1);
    plan.setPlanVersion(nextPlanVersion(principal.getTenantId(), eventId,
        req.eventWorkTypeId(), req.eventWorkTypeLevelId(), contentType));
    plan.setComments(req.comments());
    plan.setUpdatedBy(principal.getUserId());
    if (planMapper.updateById(plan) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "竞赛组卷计划已发生变化，请刷新后重试");
    }
    detailMapper.delete(new LambdaQueryWrapper<CompetitionMakeExamPlanDetailEntity>()
        .eq(CompetitionMakeExamPlanDetailEntity::getTenantId, principal.getTenantId())
        .eq(CompetitionMakeExamPlanDetailEntity::getPlanId, planId));
    snapshotMapper.update(null, new LambdaUpdateWrapper<CompetitionMakeExamPaperSnapshotEntity>()
        .eq(CompetitionMakeExamPaperSnapshotEntity::getTenantId, principal.getTenantId())
        .eq(CompetitionMakeExamPaperSnapshotEntity::getPlanId, planId)
        .eq(CompetitionMakeExamPaperSnapshotEntity::getStatus, "draft")
        .set(CompetitionMakeExamPaperSnapshotEntity::getStatus, "deprecated")
        .set(CompetitionMakeExamPaperSnapshotEntity::getUpdatedBy, principal.getUserId())
        .set(CompetitionMakeExamPaperSnapshotEntity::getUpdatedAt, LocalDateTime.now()));
    saveDetails(principal.getTenantId(), principal.getUserId(), plan.getId(), req.details());
    generateDraftSnapshots(principal.getTenantId(), principal.getUserId(), plan, req.details());
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
    if (snapshots == null || plan.getPaperCount() == null || plan.getPaperCount() != 1
        || snapshots.stream().filter(s -> "draft".equals(s.getStatus())).count() != 1) {
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
        result.add(toSnapshotVO(principal.getTenantId(), snapshot));
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
    return toSnapshotVO(principal.getTenantId(), snapshot);
  }

  private void generateDraftSnapshots(Long tenantId, Long userId, CompetitionMakeExamPlanEntity plan,
      List<CompetitionMakeExamPlanDetailReq> details) {
    List<CompetitionQuestionEntity> candidates = questionMapper.selectList(questionScope(tenantId, plan));
    candidates = candidates == null ? new ArrayList<>() : new ArrayList<>(candidates);
    Random random = new Random(UUID.randomUUID().getMostSignificantBits());
    List<SelectedQuestion> selectedQuestions = selectQuestionsForDetails(candidates, details, random);
    BigDecimal actualTotalScore = sumAssignedScores(selectedQuestions);
    if (actualTotalScore.compareTo(plan.getTotalScore()) != 0) {
      throw new BusinessException(ErrorCode.CONFLICT,
          "组卷规则合计分值 " + actualTotalScore + " 不等于计划总分 " + plan.getTotalScore());
    }
    CompetitionMakeExamPaperSnapshotEntity snapshot = new CompetitionMakeExamPaperSnapshotEntity();
    snapshot.setId(IdWorker.getId());
    snapshot.setTenantId(tenantId);
    snapshot.setPlanId(plan.getId());
    snapshot.setEventId(plan.getEventId());
    snapshot.setEventWorkTypeId(plan.getEventWorkTypeId());
    snapshot.setEventWorkTypeLevelId(plan.getEventWorkTypeLevelId());
    snapshot.setContentType(plan.getContentType());
    snapshot.setPaperCode("P");
    snapshot.setVersionNo(plan.getPlanVersion());
    snapshot.setTitle(plan.getTitle() + "（试卷）");
    snapshot.setDurationMinutes(plan.getDurationMinutes());
    snapshot.setQuestionCount(selectedQuestions.size());
    snapshot.setTotalScore(actualTotalScore);
    snapshot.setPassScore(plan.getPassScore());
    snapshot.setStatus("draft");
    snapshot.setRandomSeed(UUID.randomUUID().toString());
    snapshot.setSourceHash(hashQuestionIds(selectedQuestions));
    snapshot.setCreatedBy(userId);
    snapshot.setUpdatedBy(userId);
    snapshot.setDeleted(0);
    if (snapshotMapper.insert(snapshot) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "竞赛试卷快照保存失败");
    }
    int order = 1;
    for (SelectedQuestion selected : selectedQuestions) {
      saveSnapshotQuestion(tenantId, userId, snapshot.getId(), order++, selected.question(), selected.point());
    }
  }

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
    return wrapper;
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
      CompetitionQuestionEntity source, BigDecimal point) {
    CompetitionMakeExamPaperQuestionEntity question = new CompetitionMakeExamPaperQuestionEntity();
    question.setId(IdWorker.getId());
    question.setTenantId(tenantId);
    question.setPaperSnapshotId(snapshotId);
    question.setSourceQuestionId(source.getId());
    question.setSortOrder(order);
    question.setQuestionType(source.getQuestionType());
    question.setStem(source.getStem());
    question.setOptionsJson(source.getOptionsJson());
    question.setReferenceAnswer(source.getReferenceAnswer());
    question.setFillAnswer(source.getFillAnswer());
    question.setScore(point);
    question.setDifficulty(source.getDifficulty());
    if ("practical".equals(source.getContentType())) {
      CompetitionPracticalQuestionDetailEntity detail = practicalDetailMapper.selectByQuestion(tenantId, source.getId());
      if (detail != null) {
        question.setOperationContent(detail.getOperationContent());
        question.setStudentPrepare(detail.getStudentPrepare());
        question.setEquipmentPrepare(detail.getEquipmentPrepare());
      }
      question.setScoringItemsJson(scoreItemsJson(tenantId, source.getId(), point));
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

  private int nextPlanVersion(
      Long tenantId,
      Long eventId,
      Long eventWorkTypeId,
      Long eventWorkTypeLevelId,
      String contentType) {
    Integer maxVersion = planMapper.selectMaxVersion(
        tenantId, eventId, eventWorkTypeId, eventWorkTypeLevelId, contentType);
    return (maxVersion == null ? 0 : maxVersion) + 1;
  }

  private void validatePlan(Long tenantId, Long eventId, CompetitionMakeExamPlanUpsertReq req) {
    requireScope(tenantId, eventId, req.eventWorkTypeId(), req.eventWorkTypeLevelId());
    String contentType = normalizeContentType(req.contentType());
    if (req.durationMinutes() == null || req.totalScore() == null || req.passScore() == null
        || req.durationMinutes() <= 0 || req.totalScore().signum() <= 0
        || req.passScore().signum() < 0 || req.passScore().compareTo(req.totalScore()) > 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "考试时长、及格分或总分设置无效");
    }
    scoreCents(req.totalScore());
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
    return new CompetitionMakeExamPlanVO(String.valueOf(plan.getId()), String.valueOf(plan.getEventId()),
        String.valueOf(plan.getEventWorkTypeId()), String.valueOf(plan.getEventWorkTypeLevelId()), plan.getContentType(),
        plan.getTitle(), plan.getExamType(), plan.getDurationMinutes(), plan.getTotalScore(), plan.getPassScore(),
        plan.getPaperCount(), plan.getPlanVersion(), plan.getStatus(), plan.getComments(), plan.getCreatedAt(), plan.getUpdatedAt(), detailVOs);
  }

  private CompetitionMakeExamSnapshotVO toSnapshotVO(Long tenantId, CompetitionMakeExamPaperSnapshotEntity snapshot) {
    List<CompetitionMakeExamPaperQuestionEntity> questions = snapshotQuestionMapper.selectByPaper(tenantId, snapshot.getId());
    List<CompetitionMakeExamSnapshotQuestionVO> questionVOs = new ArrayList<>();
    if (questions != null) for (CompetitionMakeExamPaperQuestionEntity question : questions) questionVOs.add(new CompetitionMakeExamSnapshotQuestionVO(
        String.valueOf(question.getId()), String.valueOf(question.getSourceQuestionId()), question.getSortOrder(), question.getQuestionType(),
        question.getStem(), question.getOptionsJson(), question.getReferenceAnswer(), question.getFillAnswer(), question.getScore(), question.getDifficulty(),
        question.getOperationContent(), question.getStudentPrepare(), question.getEquipmentPrepare(), question.getScoringItemsJson()));
    return new CompetitionMakeExamSnapshotVO(String.valueOf(snapshot.getId()), String.valueOf(snapshot.getPlanId()), String.valueOf(snapshot.getEventId()),
        String.valueOf(snapshot.getEventWorkTypeId()), String.valueOf(snapshot.getEventWorkTypeLevelId()), snapshot.getContentType(), snapshot.getPaperCode(),
        snapshot.getVersionNo(), snapshot.getTitle(), snapshot.getDurationMinutes(), snapshot.getQuestionCount(), snapshot.getTotalScore(),
        snapshot.getPassScore(), snapshot.getStatus(), questionVOs);
  }

  private String scoreItemsJson(Long tenantId, Long questionId, BigDecimal targetScore) {
    List<CompetitionPracticalQuestionScoreItemEntity> items = scoreItemMapper.selectByQuestion(tenantId, questionId);
    if (items == null || items.isEmpty()) {
      throw new BusinessException(ErrorCode.CONFLICT, "实操题缺少评分项，无法生成组卷快照");
    }
    BigDecimal sourceTotal = BigDecimal.ZERO;
    for (CompetitionPracticalQuestionScoreItemEntity item : items) {
      if (item == null || item.getMaxScore() == null || item.getMaxScore().signum() <= 0
          || item.getMaxDeduction() == null || item.getMaxDeduction().signum() < 0
          || item.getDeductionStep() == null || item.getDeductionStep().signum() <= 0) {
        throw new BusinessException(ErrorCode.CONFLICT, "实操题评分项规则无效，无法生成组卷快照");
      }
      sourceTotal = sourceTotal.add(item.getMaxScore());
    }
    if (sourceTotal.signum() <= 0) {
      throw new BusinessException(ErrorCode.CONFLICT, "实操题评分项没有有效分值，无法生成组卷快照");
    }
    BigDecimal remainingScore = targetScore.setScale(2, RoundingMode.UNNECESSARY);
    StringBuilder json = new StringBuilder("[");
    int index = 0;
    if (items != null) for (CompetitionPracticalQuestionScoreItemEntity item : items) {
      if (index++ > 0) json.append(',');
      BigDecimal itemScore = index == items.size()
          ? remainingScore
          : item.getMaxScore().multiply(targetScore).divide(sourceTotal, 2, RoundingMode.HALF_UP);
      if (itemScore.signum() <= 0) {
        throw new BusinessException(ErrorCode.CONFLICT, "实操题单题分值过小，无法按评分项保留两位小数");
      }
      remainingScore = remainingScore.subtract(itemScore);
      BigDecimal ratio = targetScore.divide(sourceTotal, 8, RoundingMode.HALF_UP);
      BigDecimal maxDeduction = item.getMaxDeduction().multiply(ratio).setScale(2, RoundingMode.HALF_UP);
      if (maxDeduction.compareTo(itemScore) > 0) maxDeduction = itemScore;
      BigDecimal deductionStep = item.getDeductionStep().multiply(ratio).setScale(2, RoundingMode.HALF_UP);
      if (deductionStep.signum() <= 0) deductionStep = new BigDecimal("0.01");
      json.append("{\"itemNo\":").append(item.getItemNo()).append(",\"itemName\":\"")
           .append(jsonEscape(item.getItemName())).append("\",\"criterion\":\"")
           .append(jsonEscape(item.getCriterion())).append("\",\"maxScore\":").append(itemScore)
           .append(",\"maxDeduction\":").append(maxDeduction).append(",\"deductionStep\":")
           .append(deductionStep).append('}');
    }
    return json.append(']').toString();
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

  private record SelectedQuestion(CompetitionQuestionEntity question, BigDecimal point) {
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
