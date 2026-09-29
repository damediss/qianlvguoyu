package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPaperSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalExecutionConfigEntity;
import com.guoyu.platform.competition.entity.CompetitionStageExecutionPlanEntity;
import com.guoyu.platform.competition.entity.CompetitionStagePlanResourceEntity;
import com.guoyu.platform.competition.mapper.CompetitionMakeExamPaperSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalExecutionConfigMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageExecutionPlanMapper;
import com.guoyu.platform.competition.mapper.CompetitionStagePlanResourceMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Copies editable stage rules into draft plan resources; published resources stay immutable. */
@Service
public class CompetitionPracticalStagePlanSnapshotService {
  public static final String RESOURCE_TYPE = "practical_execution_config";

  private final CompetitionPracticalExecutionConfigMapper configMapper;
  private final CompetitionMakeExamPaperSnapshotMapper schemeMapper;
  private final CompetitionStageExecutionPlanMapper planMapper;
  private final CompetitionStagePlanResourceMapper resourceMapper;
  private final ObjectMapper objectMapper;

  public CompetitionPracticalStagePlanSnapshotService(
      CompetitionPracticalExecutionConfigMapper configMapper,
      CompetitionMakeExamPaperSnapshotMapper schemeMapper,
      CompetitionStageExecutionPlanMapper planMapper,
      CompetitionStagePlanResourceMapper resourceMapper,
      ObjectMapper objectMapper) {
    this.configMapper = configMapper;
    this.schemeMapper = schemeMapper;
    this.planMapper = planMapper;
    this.resourceMapper = resourceMapper;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public void syncDraftPlansForStage(Long tenantId, Long userId, Long stageId) {
    List<CompetitionStageExecutionPlanEntity> plans = planMapper.selectList(
        new LambdaQueryWrapper<CompetitionStageExecutionPlanEntity>()
            .eq(CompetitionStageExecutionPlanEntity::getTenantId, tenantId)
            .eq(CompetitionStageExecutionPlanEntity::getStageId, stageId)
            .eq(CompetitionStageExecutionPlanEntity::getStatus, "draft")
            .eq(CompetitionStageExecutionPlanEntity::getDeleted, 0));
    if (plans != null) plans.forEach(plan -> syncDraftPlan(tenantId, userId, plan));
  }

  @Transactional
  public void syncDraftPlan(Long tenantId, Long userId, CompetitionStageExecutionPlanEntity plan) {
    if (plan == null || !"draft".equals(plan.getStatus())) return;
    CompetitionPracticalExecutionConfigEntity config = configMapper.selectOne(
        new LambdaQueryWrapper<CompetitionPracticalExecutionConfigEntity>()
            .eq(CompetitionPracticalExecutionConfigEntity::getTenantId, tenantId)
            .eq(CompetitionPracticalExecutionConfigEntity::getStageId, plan.getStageId())
            .eq(CompetitionPracticalExecutionConfigEntity::getEventWorkTypeId, plan.getEventWorkTypeId())
            .eq(CompetitionPracticalExecutionConfigEntity::getEventWorkTypeLevelId,
                plan.getEventWorkTypeLevelId())
            .eq(CompetitionPracticalExecutionConfigEntity::getDeleted, 0));
    if (config == null) return;
    CompetitionMakeExamPaperSnapshotEntity scheme = schemeMapper.selectOne(
        new LambdaQueryWrapper<CompetitionMakeExamPaperSnapshotEntity>()
            .eq(CompetitionMakeExamPaperSnapshotEntity::getTenantId, tenantId)
            .eq(CompetitionMakeExamPaperSnapshotEntity::getId, config.getScoringSchemeId())
            .eq(CompetitionMakeExamPaperSnapshotEntity::getDeleted, 0));
    if (scheme == null) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "实操评分方案快照不存在，无法发布赛段");
    }
    LocalDateTime now = LocalDateTime.now();
    String payload;
    try {
      payload = objectMapper.writeValueAsString(new RuntimeConfigSnapshot(
          config.getId(), config.getVersion(), config.getEventId(), config.getStageId(),
          config.getStageContentId(), config.getExamName(), config.getDurationSeconds(),
          config.getStartMode(), config.getAllowEarlySubmit(), config.getTimeScoringEnabled(),
          config.getOvertimeRuleType(), config.getOvertimeStepSeconds(),
          config.getOvertimeScorePerStep(), config.getOvertimeFixedDeduction(),
          config.getOvertimeMaxDeduction(), config.getEarlyFinishRuleType(),
          config.getEarlyFinishStepSeconds(), config.getEarlyFinishScorePerStep(),
          config.getEarlyFinishFixedBonus(), config.getEarlyFinishMaxBonus(),
          config.getOvertimeAction(), config.getMaxDurationSeconds(),
          config.getFinalScoreUpperLimit(), new ScoringSchemeSnapshot(scheme.getId(),
              scheme.getTitle(), scheme.getPaperCode(), scheme.getVersionNo(),
              scheme.getTotalScore(), scheme.getQuestionCount(), scheme.getDurationMinutes(),
              scheme.getSourceHash())));
    } catch (JsonProcessingException ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "实操配置发布快照生成失败");
    }

    CompetitionStagePlanResourceEntity resource = resourceMapper.selectOne(
        new LambdaQueryWrapper<CompetitionStagePlanResourceEntity>()
            .eq(CompetitionStagePlanResourceEntity::getTenantId, tenantId)
            .eq(CompetitionStagePlanResourceEntity::getPlanId, plan.getId())
            .eq(CompetitionStagePlanResourceEntity::getContentType, "practical")
            .eq(CompetitionStagePlanResourceEntity::getResourceType, RESOURCE_TYPE)
            .eq(CompetitionStagePlanResourceEntity::getDeleted, 0));
    boolean insert = resource == null;
    if (insert) {
      resource = new CompetitionStagePlanResourceEntity();
      resource.setId(IdWorker.getId());
      resource.setTenantId(tenantId);
      resource.setPlanId(plan.getId());
      resource.setEventId(plan.getEventId());
      resource.setStageId(plan.getStageId());
      resource.setContentType("practical");
      resource.setResourceType(RESOURCE_TYPE);
      resource.setResourceId(config.getId());
      resource.setStatus("enabled");
      resource.setCreatedBy(userId);
      resource.setCreatedAt(now);
      resource.setDeleted(0);
    }
    resource.setResourceVersion("config-v" + config.getVersion());
    resource.setResourcePayload(payload);
    resource.setUpdatedBy(userId);
    resource.setUpdatedAt(now);
    int changed = insert ? resourceMapper.insert(resource) : resourceMapper.updateById(resource);
    if (changed != 1) throw new BusinessException(ErrorCode.CONFLICT, "实操配置快照写入执行方案失败");
    plan.setPreflightStatus("pending");
    plan.setUpdatedBy(userId);
    plan.setUpdatedAt(now);
    planMapper.updateById(plan);
  }

  @Transactional(readOnly = true)
  public RuntimeConfigSnapshot runtimeConfigForPlan(Long tenantId, Long planId) {
    CompetitionStagePlanResourceEntity resource = resourceMapper.selectOne(
        new LambdaQueryWrapper<CompetitionStagePlanResourceEntity>()
            .eq(CompetitionStagePlanResourceEntity::getTenantId, tenantId)
            .eq(CompetitionStagePlanResourceEntity::getPlanId, planId)
            .eq(CompetitionStagePlanResourceEntity::getContentType, "practical")
            .eq(CompetitionStagePlanResourceEntity::getResourceType, RESOURCE_TYPE)
            .eq(CompetitionStagePlanResourceEntity::getStatus, "enabled")
            .eq(CompetitionStagePlanResourceEntity::getDeleted, 0));
    if (resource == null || resource.getResourcePayload() == null
        || resource.getResourcePayload().isBlank()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "赛段实操运行快照缺失，无法启动考试");
    }
    try {
      return objectMapper.readValue(resource.getResourcePayload(), RuntimeConfigSnapshot.class);
    } catch (JsonProcessingException ex) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "赛段实操运行快照无效，无法启动考试");
    }
  }

  @Transactional(readOnly = true)
  public RuntimeTimingSnapshot runtimeTimingForPlan(Long tenantId, Long planId) {
    RuntimeConfigSnapshot config = runtimeConfigForPlan(tenantId, planId);
    return new RuntimeTimingSnapshot(config.durationSeconds(), config.overtimeAction(),
        config.maxDurationSeconds());
  }

  public record RuntimeConfigSnapshot(
      Long configId,
      Integer configVersion,
      Long eventId,
      Long stageId,
      Long stageContentId,
      String examName,
      Long durationSeconds,
      String startMode,
      Integer allowEarlySubmit,
      Integer timeScoringEnabled,
      String overtimeRuleType,
      Long overtimeStepSeconds,
      BigDecimal overtimeScorePerStep,
      BigDecimal overtimeFixedDeduction,
      BigDecimal overtimeMaxDeduction,
      String earlyFinishRuleType,
      Long earlyFinishStepSeconds,
      BigDecimal earlyFinishScorePerStep,
      BigDecimal earlyFinishFixedBonus,
      BigDecimal earlyFinishMaxBonus,
      String overtimeAction,
      Long maxDurationSeconds,
      BigDecimal finalScoreUpperLimit,
      ScoringSchemeSnapshot scoringScheme) { }

  public record ScoringSchemeSnapshot(
      Long id,
      String title,
      String paperCode,
      Integer versionNo,
      BigDecimal totalScore,
      Integer questionCount,
      Integer durationMinutes,
      String sourceHash) { }

  public record RuntimeTimingSnapshot(
      Long durationSeconds,
      String overtimeAction,
      Long maxDurationSeconds) { }
}
