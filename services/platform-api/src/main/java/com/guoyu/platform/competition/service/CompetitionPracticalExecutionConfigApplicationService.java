package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.domain.CompetitionPracticalExecutionConfigValidator;
import com.guoyu.platform.competition.domain.PracticalTimeScoreCalculator;
import com.guoyu.platform.competition.domain.PracticalTimeScoreCalculation;
import com.guoyu.platform.competition.domain.PracticalTimeScoreRule;
import com.guoyu.platform.competition.dto.CompetitionPracticalExecutionConfigSaveReq;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPaperSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPlanEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalExecutionConfigEntity;
import com.guoyu.platform.competition.entity.CompetitionStageContentEntity;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeLevelMapper;
import com.guoyu.platform.competition.mapper.CompetitionMakeExamPaperSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionMakeExamPlanMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalExecutionConfigMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageContentMapper;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.competition.vo.CompetitionPracticalExecutionConfigValidationVO;
import com.guoyu.platform.competition.vo.CompetitionPracticalExecutionConfigVO;
import com.guoyu.platform.competition.vo.CompetitionPracticalScoringSchemeVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompetitionPracticalExecutionConfigApplicationService {
  private final CompetitionStageApplicationService stageService;
  private final CompetitionStageService stageEntityService;
  private final CompetitionStageContentMapper contentMapper;
  private final CompetitionEventWorkTypeLevelMapper workTypeLevelMapper;
  private final CompetitionPracticalExecutionConfigMapper configMapper;
  private final CompetitionMakeExamPaperSnapshotMapper schemeMapper;
  private final CompetitionPracticalExecutionConfigValidator validator;
  private final PracticalTimeScoreCalculator timeScoreCalculator;

  @Autowired(required = false)
  private CompetitionPracticalStagePlanSnapshotService planSnapshotService;

  @Autowired(required = false)
  private CompetitionMakeExamPlanMapper makeExamPlanMapper;

  public CompetitionPracticalExecutionConfigApplicationService(
      CompetitionStageApplicationService stageService,
      CompetitionStageService stageEntityService,
      CompetitionStageContentMapper contentMapper,
      CompetitionEventWorkTypeLevelMapper workTypeLevelMapper,
      CompetitionPracticalExecutionConfigMapper configMapper,
      CompetitionMakeExamPaperSnapshotMapper schemeMapper,
      CompetitionPracticalExecutionConfigValidator validator,
      PracticalTimeScoreCalculator timeScoreCalculator) {
    this.stageService = stageService;
    this.stageEntityService = stageEntityService;
    this.contentMapper = contentMapper;
    this.workTypeLevelMapper = workTypeLevelMapper;
    this.configMapper = configMapper;
    this.schemeMapper = schemeMapper;
    this.validator = validator;
    this.timeScoreCalculator = timeScoreCalculator;
  }

  @Transactional(readOnly = true)
  public CompetitionPracticalExecutionConfigVO get(Long stageId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionStageEntity stage = getStageEntity(stageId, principal.getTenantId());
    CompetitionStageContentEntity practicalContent = practicalContent(principal.getTenantId(), stageId);
    if (practicalContent == null) return null;
    CompetitionPracticalExecutionConfigEntity config = findConfig(principal.getTenantId(), stageId);
    if (config == null) return null;
    CompetitionMakeExamPaperSnapshotEntity scheme = findSchemeById(
        principal.getTenantId(), config.getScoringSchemeId());
    return toVO(config, scheme, stage);
  }

  @Transactional(readOnly = true)
  public List<CompetitionPracticalScoringSchemeVO> scoringSchemes(
      Long eventId, Long eventWorkTypeId, Long eventWorkTypeLevelId) {
    PlatformUserPrincipal principal = currentUser();
    if (makeExamPlanMapper == null || eventId == null || eventWorkTypeId == null
        || eventWorkTypeLevelId == null) return List.of();
    CompetitionEventWorkTypeLevelEntity level = workTypeLevelMapper.selectOne(
        new LambdaQueryWrapper<CompetitionEventWorkTypeLevelEntity>()
            .eq(CompetitionEventWorkTypeLevelEntity::getTenantId, principal.getTenantId())
            .eq(CompetitionEventWorkTypeLevelEntity::getEventId, eventId)
            .eq(CompetitionEventWorkTypeLevelEntity::getEventWorkTypeId, eventWorkTypeId)
            .eq(CompetitionEventWorkTypeLevelEntity::getId, eventWorkTypeLevelId)
            .eq(CompetitionEventWorkTypeLevelEntity::getDeleted, 0));
    if (level == null) return List.of();
    List<CompetitionMakeExamPaperSnapshotEntity> rows = schemeMapper.selectList(
        new LambdaQueryWrapper<CompetitionMakeExamPaperSnapshotEntity>()
            .eq(CompetitionMakeExamPaperSnapshotEntity::getTenantId, principal.getTenantId())
            .eq(CompetitionMakeExamPaperSnapshotEntity::getEventId, eventId)
            .eq(CompetitionMakeExamPaperSnapshotEntity::getEventWorkTypeId, eventWorkTypeId)
            .eq(CompetitionMakeExamPaperSnapshotEntity::getEventWorkTypeLevelId, eventWorkTypeLevelId)
            .eq(CompetitionMakeExamPaperSnapshotEntity::getContentType, "practical")
            .eq(CompetitionMakeExamPaperSnapshotEntity::getPaperCode, "P")
            .eq(CompetitionMakeExamPaperSnapshotEntity::getStatus, "published")
            .gt(CompetitionMakeExamPaperSnapshotEntity::getQuestionCount, 0)
            .eq(CompetitionMakeExamPaperSnapshotEntity::getDeleted, 0)
            .orderByDesc(CompetitionMakeExamPaperSnapshotEntity::getUpdatedAt)
            .orderByDesc(CompetitionMakeExamPaperSnapshotEntity::getId));
    List<CompetitionPracticalScoringSchemeVO> result = new ArrayList<>();
    if (rows != null) for (CompetitionMakeExamPaperSnapshotEntity row : rows) {
      CompetitionMakeExamPlanEntity plan = makeExamPlanMapper.selectOne(
          new LambdaQueryWrapper<CompetitionMakeExamPlanEntity>()
              .eq(CompetitionMakeExamPlanEntity::getTenantId, principal.getTenantId())
              .eq(CompetitionMakeExamPlanEntity::getId, row.getPlanId())
              .eq(CompetitionMakeExamPlanEntity::getPlanPurpose, "PRACTICAL_SCORING_SCHEME")
              .eq(CompetitionMakeExamPlanEntity::getStatus, "published")
              .eq(CompetitionMakeExamPlanEntity::getDeleted, 0));
      if (plan != null) result.add(schemeVO(row));
    }
    return result;
  }

  @Transactional(readOnly = true)
  public CompetitionPracticalExecutionConfigValidationVO validate(
      Long stageId, CompetitionPracticalExecutionConfigSaveReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionStageEntity stage = getStageEntity(stageId, principal.getTenantId());
    List<String> scopeErrors = scopeErrors(principal.getTenantId(), stage, req);
    CompetitionMakeExamPaperSnapshotEntity scheme = req == null || req.scoringSchemeId() == null
        ? null : findPublishedScheme(principal.getTenantId(), req.scoringSchemeId(),
            stage.getEventId(), req.eventWorkTypeId(), req.eventWorkTypeLevelId());
    List<String> errors = new ArrayList<>(scopeErrors);
    errors.addAll(validator.validate(req, scheme,
        practicalContent(principal.getTenantId(), stageId) != null));
    return new CompetitionPracticalExecutionConfigValidationVO(errors.isEmpty(),
        List.copyOf(errors), errors.isEmpty() ? "实操配置校验通过" : String.join("；", errors));
  }

  @Transactional
  public CompetitionPracticalExecutionConfigVO save(
      Long stageId, CompetitionPracticalExecutionConfigSaveReq req) {
    PlatformUserPrincipal principal = currentUser();
    stageService.assertCanManageStagePlan(stageId);
    CompetitionStageEntity stage = getStageEntity(stageId, principal.getTenantId());
    CompetitionStageContentEntity practicalContent = practicalContent(principal.getTenantId(), stageId);
    CompetitionMakeExamPaperSnapshotEntity scheme = req == null || req.scoringSchemeId() == null
        ? null : findPublishedScheme(principal.getTenantId(), req.scoringSchemeId(),
            stage.getEventId(), req.eventWorkTypeId(), req.eventWorkTypeLevelId());
    List<String> errors = new ArrayList<>(scopeErrors(principal.getTenantId(), stage, req));
    errors.addAll(validator.validate(req, scheme, practicalContent != null));
    if (!errors.isEmpty()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, String.join("；", errors));
    }

    LocalDateTime now = LocalDateTime.now();
    CompetitionPracticalExecutionConfigEntity config = findConfig(principal.getTenantId(), stageId);
    boolean insert = config == null;
    if (insert) {
      config = new CompetitionPracticalExecutionConfigEntity();
      config.setId(IdWorker.getId());
      config.setTenantId(principal.getTenantId());
      config.setCreatedBy(principal.getUserId());
      config.setCreatedAt(now);
      config.setDeleted(0);
      config.setVersion(1);
    } else {
      config.setVersion(config.getVersion() == null ? 1 : config.getVersion() + 1);
    }
    config.setEventId(stage.getEventId());
    config.setStageId(stageId);
    config.setStageContentId(practicalContent.getId());
    config.setEventWorkTypeId(req.eventWorkTypeId());
    config.setEventWorkTypeLevelId(req.eventWorkTypeLevelId());
    config.setScoringSchemeId(req.scoringSchemeId());
    config.setExamName(req.examName().trim());
    config.setDurationSeconds(req.durationSeconds());
    config.setStartMode(req.startMode());
    config.setAllowEarlySubmit(req.allowEarlySubmit() ? 1 : 0);
    config.setTimeScoringEnabled(req.timeScoringEnabled() ? 1 : 0);
    config.setOvertimeRuleType(req.overtimeRuleType());
    config.setOvertimeStepSeconds(req.overtimeStepSeconds());
    config.setOvertimeScorePerStep(req.overtimeScorePerStep());
    config.setOvertimeFixedDeduction(req.overtimeFixedDeduction());
    config.setOvertimeMaxDeduction(req.overtimeMaxDeduction());
    config.setEarlyFinishRuleType(req.earlyFinishRuleType());
    config.setEarlyFinishStepSeconds(req.earlyFinishStepSeconds());
    config.setEarlyFinishScorePerStep(req.earlyFinishScorePerStep());
    config.setEarlyFinishFixedBonus(req.earlyFinishFixedBonus());
    config.setEarlyFinishMaxBonus(req.earlyFinishMaxBonus());
    config.setOvertimeAction(req.overtimeAction());
    config.setMaxDurationSeconds(req.maxDurationSeconds());
    config.setFinalScoreUpperLimit(req.finalScoreUpperLimit());
    config.setUpdatedBy(principal.getUserId());
    config.setUpdatedAt(now);
    int affected = insert ? configMapper.insert(config) : configMapper.updateById(config);
    if (affected != 1) throw new BusinessException(ErrorCode.CONFLICT, "实操配置保存失败，请刷新后重试");

    if (planSnapshotService != null) {
      planSnapshotService.syncDraftPlansForStage(principal.getTenantId(), principal.getUserId(), stageId);
    }
    return toVO(config, scheme, stage);
  }

  @Transactional(readOnly = true)
  public PracticalTimeScoreCalculation calculateResult(Long planId, Instant startTime,
      Instant finishTime, Duration pausedDuration, BigDecimal practicalScore) {
    PlatformUserPrincipal principal = currentUser();
    if (planSnapshotService == null) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "赛段实操运行快照服务不可用");
    }
    CompetitionPracticalStagePlanSnapshotService.RuntimeConfigSnapshot snapshot =
        planSnapshotService.runtimeConfigForPlan(principal.getTenantId(), planId);
    return timeScoreCalculator.calculate(toTimeScoreRule(snapshot), startTime, finishTime,
        pausedDuration, practicalScore);
  }

  private PracticalTimeScoreRule toTimeScoreRule(
      CompetitionPracticalStagePlanSnapshotService.RuntimeConfigSnapshot config) {
    return new PracticalTimeScoreRule(config.durationSeconds(),
        Integer.valueOf(1).equals(config.timeScoringEnabled()),
        config.overtimeRuleType(), config.overtimeStepSeconds(),
        config.overtimeScorePerStep(), config.overtimeFixedDeduction(),
        config.overtimeMaxDeduction(), config.earlyFinishRuleType(),
        config.earlyFinishStepSeconds(), config.earlyFinishScorePerStep(),
        config.earlyFinishFixedBonus(), config.earlyFinishMaxBonus(),
        config.overtimeAction(), config.maxDurationSeconds(),
        config.finalScoreUpperLimit());
  }

  private List<String> scopeErrors(Long tenantId, CompetitionStageEntity stage,
      CompetitionPracticalExecutionConfigSaveReq req) {
    if (req == null) return List.of("实操配置内容不能为空");
    List<String> errors = new ArrayList<>();
    CompetitionEventWorkTypeLevelEntity level = req.eventWorkTypeLevelId() == null ? null
        : workTypeLevelMapper.selectOne(new LambdaQueryWrapper<CompetitionEventWorkTypeLevelEntity>()
            .eq(CompetitionEventWorkTypeLevelEntity::getTenantId, tenantId)
            .eq(CompetitionEventWorkTypeLevelEntity::getId, req.eventWorkTypeLevelId())
            .eq(CompetitionEventWorkTypeLevelEntity::getDeleted, 0));
    if (level == null || !java.util.Objects.equals(level.getEventId(), stage.getEventId())
        || !java.util.Objects.equals(level.getEventWorkTypeId(), req.eventWorkTypeId())) {
      errors.add("工种等级不属于当前赛事范围");
      return List.copyOf(errors);
    }
    if (stage.getEventWorkTypeId() != null
        && !java.util.Objects.equals(stage.getEventWorkTypeId(), req.eventWorkTypeId())) {
      errors.add("工种与当前赛段范围不匹配");
    }
    if (stage.getEventWorkTypeLevelCode() != null
        && !java.util.Objects.equals(stage.getEventWorkTypeLevelCode(), level.getLevelCode())) {
      errors.add("等级与当前赛段范围不匹配");
    }
    return List.copyOf(errors);
  }

  private CompetitionStageContentEntity practicalContent(Long tenantId, Long stageId) {
    return contentMapper.selectOne(new LambdaQueryWrapper<CompetitionStageContentEntity>()
        .eq(CompetitionStageContentEntity::getTenantId, tenantId)
        .eq(CompetitionStageContentEntity::getStageId, stageId)
        .eq(CompetitionStageContentEntity::getContentType, "practical")
        .eq(CompetitionStageContentEntity::getStatus, "enabled")
        .eq(CompetitionStageContentEntity::getDeleted, 0));
  }

  private CompetitionPracticalExecutionConfigEntity findConfig(Long tenantId, Long stageId) {
    return configMapper.selectOne(new LambdaQueryWrapper<CompetitionPracticalExecutionConfigEntity>()
        .eq(CompetitionPracticalExecutionConfigEntity::getTenantId, tenantId)
        .eq(CompetitionPracticalExecutionConfigEntity::getStageId, stageId)
        .eq(CompetitionPracticalExecutionConfigEntity::getDeleted, 0));
  }

  private CompetitionMakeExamPaperSnapshotEntity findPublishedScheme(Long tenantId, Long schemeId,
      Long eventId, Long eventWorkTypeId, Long eventWorkTypeLevelId) {
    if (makeExamPlanMapper == null || eventId == null || eventWorkTypeId == null
        || eventWorkTypeLevelId == null) return null;
    CompetitionMakeExamPaperSnapshotEntity scheme = schemeMapper.selectOne(
        new LambdaQueryWrapper<CompetitionMakeExamPaperSnapshotEntity>()
        .eq(CompetitionMakeExamPaperSnapshotEntity::getTenantId, tenantId)
        .eq(CompetitionMakeExamPaperSnapshotEntity::getId, schemeId)
        .eq(CompetitionMakeExamPaperSnapshotEntity::getEventId, eventId)
        .eq(CompetitionMakeExamPaperSnapshotEntity::getEventWorkTypeId, eventWorkTypeId)
        .eq(CompetitionMakeExamPaperSnapshotEntity::getEventWorkTypeLevelId, eventWorkTypeLevelId)
        .eq(CompetitionMakeExamPaperSnapshotEntity::getContentType, "practical")
        .eq(CompetitionMakeExamPaperSnapshotEntity::getPaperCode, "P")
        .eq(CompetitionMakeExamPaperSnapshotEntity::getStatus, "published")
        .gt(CompetitionMakeExamPaperSnapshotEntity::getQuestionCount, 0)
        .eq(CompetitionMakeExamPaperSnapshotEntity::getDeleted, 0));
    if (scheme == null) return null;
    CompetitionMakeExamPlanEntity plan = makeExamPlanMapper.selectOne(
        new LambdaQueryWrapper<CompetitionMakeExamPlanEntity>()
            .eq(CompetitionMakeExamPlanEntity::getTenantId, tenantId)
            .eq(CompetitionMakeExamPlanEntity::getId, scheme.getPlanId())
            .eq(CompetitionMakeExamPlanEntity::getPlanPurpose, "PRACTICAL_SCORING_SCHEME")
            .eq(CompetitionMakeExamPlanEntity::getStatus, "published")
            .eq(CompetitionMakeExamPlanEntity::getDeleted, 0));
    return plan == null ? null : scheme;
  }

  private CompetitionMakeExamPaperSnapshotEntity findSchemeById(Long tenantId, Long schemeId) {
    return schemeMapper.selectOne(new LambdaQueryWrapper<CompetitionMakeExamPaperSnapshotEntity>()
        .eq(CompetitionMakeExamPaperSnapshotEntity::getTenantId, tenantId)
        .eq(CompetitionMakeExamPaperSnapshotEntity::getId, schemeId)
        .eq(CompetitionMakeExamPaperSnapshotEntity::getDeleted, 0));
  }

  private CompetitionPracticalExecutionConfigVO toVO(
      CompetitionPracticalExecutionConfigEntity config,
      CompetitionMakeExamPaperSnapshotEntity scheme,
      CompetitionStageEntity stage) {
    return new CompetitionPracticalExecutionConfigVO(
        String.valueOf(config.getId()), String.valueOf(stage.getEventId()),
        String.valueOf(config.getStageId()), String.valueOf(config.getStageContentId()),
        String.valueOf(config.getEventWorkTypeId()), String.valueOf(config.getEventWorkTypeLevelId()),
        String.valueOf(config.getScoringSchemeId()), config.getExamName(), config.getDurationSeconds(),
        "SECONDS", config.getStartMode(), Integer.valueOf(1).equals(config.getAllowEarlySubmit()),
        Integer.valueOf(1).equals(config.getTimeScoringEnabled()), config.getOvertimeRuleType(),
        config.getOvertimeStepSeconds(), config.getOvertimeScorePerStep(),
        config.getOvertimeFixedDeduction(), config.getOvertimeMaxDeduction(),
        config.getEarlyFinishRuleType(), config.getEarlyFinishStepSeconds(),
        config.getEarlyFinishScorePerStep(), config.getEarlyFinishFixedBonus(),
        config.getEarlyFinishMaxBonus(), config.getOvertimeAction(), config.getMaxDurationSeconds(),
        config.getFinalScoreUpperLimit(), scheme == null ? null : scheme.getTotalScore(),
        config.getVersion(), scheme == null ? null : schemeVO(scheme));
  }

  private CompetitionPracticalScoringSchemeVO schemeVO(CompetitionMakeExamPaperSnapshotEntity scheme) {
    return new CompetitionPracticalScoringSchemeVO(String.valueOf(scheme.getId()),
        String.valueOf(scheme.getEventId()), String.valueOf(scheme.getEventWorkTypeId()),
        String.valueOf(scheme.getEventWorkTypeLevelId()), scheme.getTitle(), scheme.getPaperCode(),
        scheme.getVersionNo(), scheme.getDurationMinutes(), scheme.getQuestionCount(),
        scheme.getTotalScore(), scheme.getSourceHash());
  }

  private PlatformUserPrincipal currentUser() {
    return SecurityContext.currentUser();
  }

  private CompetitionStageEntity getStageEntity(Long stageId, Long tenantId) {
    stageService.get(stageId);
    return stageEntityService.getById(tenantId, stageId);
  }
}
