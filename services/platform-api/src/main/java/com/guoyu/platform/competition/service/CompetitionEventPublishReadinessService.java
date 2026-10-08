package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.domain.CompetitionPromotionRuleDomainService;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeStageEntity;
import com.guoyu.platform.competition.entity.CompetitionStageContentEntity;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.enums.CompetitionEventScope;
import com.guoyu.platform.competition.enums.CompetitionEventStatus;
import com.guoyu.platform.competition.enums.CompetitionStageContentType;
import com.guoyu.platform.competition.enums.CompetitionStagePlanStatus;
import com.guoyu.platform.competition.enums.CompetitionStageType;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeLevelMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeStageMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageContentMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageMapper;
import com.guoyu.platform.competition.vo.CompetitionEventPublishReadinessCheckVO;
import com.guoyu.platform.competition.vo.CompetitionEventPublishReadinessVO;
import com.guoyu.platform.competition.vo.CompetitionEventPublishScopeReadinessVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Single source of truth for the event publish preflight and per-level arrangement confirmation.
 * Publish only requires event basic information so players can activate before stage planning.
 * Stage completeness is enforced when confirming a work-type/level arrangement, not at publish.
 */
@Service
@Slf4j
public class CompetitionEventPublishReadinessService {

  private static final BigDecimal HUNDRED = new BigDecimal("100.00");
  private static final Set<String> ENABLED_STATUSES = Set.of("enabled");
  private static final Set<String> DISABLED_STATUSES = Set.of("disabled");

  private final CompetitionEventWorkTypeMapper workTypeMapper;
  private final CompetitionEventWorkTypeLevelMapper levelMapper;
  private final CompetitionStageMapper stageMapper;
  private final CompetitionStageContentMapper contentMapper;
  private final CompetitionEventWorkTypeStageMapper workTypeStageMapper;
  private final CompetitionPromotionRuleDomainService ruleService =
      new CompetitionPromotionRuleDomainService();

  public CompetitionEventPublishReadinessService(
      CompetitionEventWorkTypeMapper workTypeMapper,
      CompetitionEventWorkTypeLevelMapper levelMapper,
      CompetitionStageMapper stageMapper,
      CompetitionStageContentMapper contentMapper,
      CompetitionEventWorkTypeStageMapper workTypeStageMapper) {
    this.workTypeMapper = workTypeMapper;
    this.levelMapper = levelMapper;
    this.stageMapper = stageMapper;
    this.contentMapper = contentMapper;
    this.workTypeStageMapper = workTypeStageMapper;
  }

  @Transactional(readOnly = true)
  public CompetitionEventPublishReadinessVO evaluate(
      Long tenantId,
      Long userId,
      CompetitionEventEntity event,
      PlatformUserPrincipal principal) {
    if (event == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "赛事不存在");
    }

    // Existing published/closed/archived events are historical data. Do not retroactively apply
    // the new publish gate to them; the final command itself only accepts draft events.
    if (!CompetitionEventStatus.DRAFT.getCode().equals(event.getStatus())) {
      return new CompetitionEventPublishReadinessVO(
          event.getId(), event.getCode(), event.getName(), event.getStatus(), event.getScope(),
          event.getStartAt(), event.getEndAt(), true, 0, 0, 0, 0,
          List.of(new CompetitionEventPublishReadinessCheckVO(
              "EVENT_ALREADY_FINALIZED", true, "赛事已完成发布或进入历史状态，无需重新执行发布门槛")),
          List.of());
    }

    List<CompetitionEventPublishReadinessCheckVO> eventChecks = new ArrayList<>();
    addEventChecks(event, eventChecks);

    List<CompetitionEventWorkTypeEntity> workTypes = safeList(workTypeMapper.selectList(
        new LambdaQueryWrapper<CompetitionEventWorkTypeEntity>()
            .eq(CompetitionEventWorkTypeEntity::getTenantId, tenantId)
            .eq(CompetitionEventWorkTypeEntity::getEventId, event.getId())
            .eq(CompetitionEventWorkTypeEntity::getStatus, "active")
            .eq(CompetitionEventWorkTypeEntity::getDeleted, 0)
            .orderByAsc(CompetitionEventWorkTypeEntity::getSortOrder)
            .orderByAsc(CompetitionEventWorkTypeEntity::getId)));
    addCheck(eventChecks, "ACTIVE_WORK_TYPE_MISSING", true,
        workTypes.isEmpty() ? "工种可在发布后继续配置" : "已配置启用工种");

    List<CompetitionEventPublishScopeReadinessVO> scopes = new ArrayList<>();
    int selectedLevelCount = 0;
    int confirmedScopeCount = 0;
    int stageCount = 0;
    for (CompetitionEventWorkTypeEntity workType : workTypes) {
      if (workType == null || workType.getId() == null) {
        continue;
      }
      List<CompetitionEventWorkTypeLevelEntity> levels = safeList(levelMapper.selectList(
          new LambdaQueryWrapper<CompetitionEventWorkTypeLevelEntity>()
              .eq(CompetitionEventWorkTypeLevelEntity::getTenantId, tenantId)
              .eq(CompetitionEventWorkTypeLevelEntity::getEventId, event.getId())
              .eq(CompetitionEventWorkTypeLevelEntity::getEventWorkTypeId, workType.getId())
              .eq(CompetitionEventWorkTypeLevelEntity::getDeleted, 0)
              .orderByAsc(CompetitionEventWorkTypeLevelEntity::getLevelCode)));
      addCheck(eventChecks, "WORK_TYPE_LEVEL_MISSING", true,
          levels.isEmpty()
              ? "工种“" + workTypeName(workType) + "”可在发布后选择参赛等级"
              : "工种“" + workTypeName(workType) + "”已明确选择参赛等级",
          "eventWorkType", workType.getId());
      selectedLevelCount += levels.size();
      for (CompetitionEventWorkTypeLevelEntity level : levels) {
        if (level == null) {
          continue;
        }
        CompetitionEventPublishScopeReadinessVO scope = evaluateScope(
            tenantId, event, workType, level, false);
        scopes.add(scope);
        if (CompetitionStagePlanStatus.CONFIRMED.getCode().equals(level.getStagePlanStatus())) {
          confirmedScopeCount++;
        }
        stageCount += scope.stageCount();
      }
    }

    boolean eventChecksPassed = eventChecks.stream()
        .allMatch(CompetitionEventPublishReadinessCheckVO::passed);
    return new CompetitionEventPublishReadinessVO(
        event.getId(), event.getCode(), event.getName(), event.getStatus(), event.getScope(),
        event.getStartAt(), event.getEndAt(), eventChecksPassed, workTypes.size(), selectedLevelCount,
        confirmedScopeCount, stageCount, List.copyOf(eventChecks), List.copyOf(scopes));
  }

  /**
   * Validates one level scope before its individual “确认编排” command. The confirmation flag is
   * deliberately excluded from this pass because that is the field the caller is about to write.
   */
  @Transactional(readOnly = true)
  public CompetitionEventPublishScopeReadinessVO evaluateForScopeConfirmation(
      Long tenantId,
      CompetitionEventEntity event,
      CompetitionEventWorkTypeEntity workType,
      CompetitionEventWorkTypeLevelEntity level) {
    return evaluateScope(tenantId, event, workType, level, false);
  }

  public String firstBlockingMessage(CompetitionEventPublishReadinessVO readiness) {
    if (readiness == null) {
      return "赛事发布条件校验失败，请刷新后重试";
    }
    for (CompetitionEventPublishReadinessCheckVO check : safeList(readiness.eventChecks())) {
      if (check == null) {
        continue;
      }
      if (!check.passed()) {
        return check.message();
      }
    }
    return "赛事尚未满足发布条件，请刷新后重试";
  }

  public String firstBlockingMessage(CompetitionEventPublishScopeReadinessVO scope) {
    if (scope != null) {
      for (CompetitionEventPublishReadinessCheckVO check : safeList(scope.checks())) {
        if (check == null) {
          continue;
        }
        if (!check.passed()) {
          return scopeLabel(scope) + "：" + check.message();
        }
      }
    }
    return "当前工种当前等级尚未满足确认条件";
  }

  private CompetitionEventPublishScopeReadinessVO evaluateScope(
      Long tenantId,
      CompetitionEventEntity event,
      CompetitionEventWorkTypeEntity workType,
      CompetitionEventWorkTypeLevelEntity level,
      boolean requireConfirmed) {
    List<CompetitionEventPublishReadinessCheckVO> checks = new ArrayList<>();
    if (workType == null || !"active".equals(workType.getStatus())) {
      addCheck(checks, "WORK_TYPE_NOT_ACTIVE", false, "参赛工种未启用", "eventWorkType",
          workType == null ? null : workType.getId());
    }
    if (level == null) {
      addCheck(checks, "LEVEL_NOT_SELECTED", false, "当前等级未在赛事总览中明确选择");
      return scope(workType, level, 0, checks);
    }
    boolean confirmed = CompetitionStagePlanStatus.CONFIRMED.getCode()
        .equals(level.getStagePlanStatus());
    addCheck(checks, "SCOPE_CONFIRMATION_REQUIRED", !requireConfirmed || confirmed,
        confirmed ? "当前工种当前等级已确认编排" : "当前工种当前等级尚未确认编排",
        "level", level.getId());

    List<CompetitionStageEntity> stages = safeList(stageMapper.selectList(
        new LambdaQueryWrapper<CompetitionStageEntity>()
            .eq(CompetitionStageEntity::getTenantId, tenantId)
            .eq(CompetitionStageEntity::getEventId, event.getId())
            .eq(CompetitionStageEntity::getEventWorkTypeId, workType.getId())
            .eq(CompetitionStageEntity::getEventWorkTypeLevelCode, level.getLevelCode())
            .eq(CompetitionStageEntity::getDeleted, 0)
            .orderByAsc(CompetitionStageEntity::getSortOrder)
            .orderByAsc(CompetitionStageEntity::getId)));
    addCheck(checks, "SCOPE_STAGES_MISSING", !stages.isEmpty(),
        stages.isEmpty() ? "当前工种当前等级至少需要配置一个赛段" : "已配置赛段",
        "level", level.getId());

    boolean timesValid = true;
    boolean eventHasWindow = event.getStartAt() != null && event.getEndAt() != null;
    boolean insideEvent = true;
    boolean sequenceValid = true;
    boolean hasEnabledStage;
    Set<String> stageTypes = new HashSet<>();
    List<StageWindow> windows = new ArrayList<>();
    for (CompetitionStageEntity stage : stages) {
      if (stage == null || stage.getId() == null) {
        timesValid = false;
        sequenceValid = false;
        continue;
      }
      CompetitionStageType type = CompetitionStageType.fromCode(stage.getStageType());
      if (type == null || !stageTypes.add(stage.getStageType())) {
        sequenceValid = false;
      } else if (!Objects.equals(stage.getSortOrder(), type.getSortOrder())) {
        sequenceValid = false;
      }
      if (stage.getStartAt() == null || stage.getEndAt() == null
          || !stage.getEndAt().isAfter(stage.getStartAt())) {
        timesValid = false;
      } else {
        if (eventHasWindow
            && (stage.getStartAt().isBefore(event.getStartAt())
                || stage.getEndAt().isAfter(event.getEndAt()))) {
          insideEvent = false;
        }
        if (type != null) {
          windows.add(new StageWindow(type, stage.getStartAt(), stage.getEndAt()));
        }
      }
    }
    windows.sort(Comparator.comparingInt(window -> window.type().getSortOrder()));
    for (int index = 1; index < windows.size(); index++) {
      if (windows.get(index).startAt().isBefore(windows.get(index - 1).endAt())) {
        sequenceValid = false;
        break;
      }
    }
    addCheck(checks, "STAGE_TIME_INVALID", timesValid,
        timesValid ? "所有赛段均已填写有效时间" : "每个赛段必须填写结束时间且结束时间晚于开始时间");
    addCheck(checks, "EVENT_TIME_REQUIRED_FOR_CONFIRM", eventHasWindow,
        eventHasWindow ? "赛事起止时间已填写" : "确认编排前请先补充赛事开始和结束时间");
    addCheck(checks, "STAGE_TIME_OUT_OF_EVENT", insideEvent,
        !eventHasWindow
            ? "赛事时间待补充，赛段时间暂不按赛事窗口校验"
            : insideEvent ? "所有赛段时间均在赛事时间范围内" : "赛段时间必须位于赛事起止时间范围内");
    addCheck(checks, "STAGE_SEQUENCE_INVALID", sequenceValid,
        sequenceValid ? "赛段顺序和时间窗口合法" : "赛段类型不能重复，且时间必须按赛段顺序递增且不重叠");
    boolean contentValid = true;
    boolean weightsValid = true;
    boolean simulationEnabled = false;
    for (CompetitionStageEntity stage : stages) {
      if (stage == null || stage.getId() == null) {
        contentValid = false;
        weightsValid = false;
        continue;
      }
      List<CompetitionStageContentEntity> contents = safeList(contentMapper.selectList(
          new LambdaQueryWrapper<CompetitionStageContentEntity>()
              .eq(CompetitionStageContentEntity::getTenantId, tenantId)
              .eq(CompetitionStageContentEntity::getEventId, event.getId())
              .eq(CompetitionStageContentEntity::getStageId, stage.getId())
              .eq(CompetitionStageContentEntity::getDeleted, 0)
              .orderByAsc(CompetitionStageContentEntity::getSortOrder)
              .orderByAsc(CompetitionStageContentEntity::getId)));
      if (contents.isEmpty()) {
        contentValid = false;
        weightsValid = false;
        continue;
      }
      boolean enabled = false;
      BigDecimal total = BigDecimal.ZERO;
      Set<String> contentTypes = new HashSet<>();
      for (CompetitionStageContentEntity content : contents) {
        if (content == null) {
          contentValid = false;
          weightsValid = false;
          continue;
        }
        String status = content.getStatus();
        boolean isEnabled = ENABLED_STATUSES.contains(status);
        boolean isDisabled = DISABLED_STATUSES.contains(status);
        CompetitionStageContentType contentType = CompetitionStageContentType.fromCode(
            content.getContentType());
        if (contentType == null || !contentTypes.add(content.getContentType())) {
          contentValid = false;
        }
        if (isEnabled) {
          enabled = true;
          BigDecimal weight = content.getWeight();
          if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0) {
            weightsValid = false;
          } else {
            total = total.add(weight);
          }
          if (contentType == CompetitionStageContentType.SIMULATION) {
            // Simulation scores are imported from Excel by ID card, so the content
            // can be confirmed as part of the stage plan.
            simulationEnabled = true;
          }
        } else if (!isDisabled || (content.getWeight() != null
            && content.getWeight().compareTo(BigDecimal.ZERO) != 0)) {
          weightsValid = false;
        }
      }
      if (!enabled) {
        contentValid = false;
        weightsValid = false;
      }
      if (total.compareTo(HUNDRED) != 0) {
        weightsValid = false;
      }
    }
    addCheck(checks, "STAGE_CONTENT_MISSING", contentValid,
        contentValid ? "所有赛段均已配置有效比赛内容" : "每个赛段至少需要启用一项有效比赛内容");
    addCheck(checks, "CONTENT_WEIGHT_INVALID", weightsValid,
        weightsValid ? "每个赛段启用内容权重合计为100%" : "每个赛段启用内容权重必须大于0且合计为100%，未启用内容权重必须为0");
    addCheck(checks, "SIMULATION_PROVIDER_UNAVAILABLE", true,
        simulationEnabled
            ? "仿真成绩通过 Excel 按身份证号导入，可以确认赛段编排"
            : "未启用仿真内容");

    List<CompetitionEventWorkTypeStageEntity> stageRows = safeList(workTypeStageMapper.selectList(
        new LambdaQueryWrapper<CompetitionEventWorkTypeStageEntity>()
            .eq(CompetitionEventWorkTypeStageEntity::getTenantId, tenantId)
            .eq(CompetitionEventWorkTypeStageEntity::getEventId, event.getId())
            .eq(CompetitionEventWorkTypeStageEntity::getEventWorkTypeId, workType.getId())
            .eq(CompetitionEventWorkTypeStageEntity::getEventWorkTypeLevelCode, level.getLevelCode())
            .eq(CompetitionEventWorkTypeStageEntity::getDeleted, 0)));
    Map<Long, CompetitionEventWorkTypeStageEntity> rowByStageId = new HashMap<>();
    for (CompetitionEventWorkTypeStageEntity row : stageRows) {
      if (row != null && row.getStageId() != null) {
        rowByStageId.put(row.getStageId(), row);
      }
    }
    hasEnabledStage = stages.stream().anyMatch(stage -> stage != null && stage.getId() != null
        && Objects.equals(rowByStageId.get(stage.getId()) == null
            ? null : rowByStageId.get(stage.getId()).getEnabled(), 1));
    // Keep the stage-presence check separate from the configuration row check so the readiness
    // matrix explains both “no stage” and “all stages disabled” cases.
    if (!stages.isEmpty() && !hasEnabledStage) {
      addCheck(checks, "ENABLED_STAGE_MISSING", false, "至少需要保留一个启用赛段");
    } else if (stages.isEmpty()) {
      addCheck(checks, "ENABLED_STAGE_MISSING", false, "至少需要保留一个启用赛段");
    } else {
      addCheck(checks, "ENABLED_STAGE_MISSING", true, "已保留至少一个启用赛段");
    }
    boolean promotionValid = !stages.isEmpty();
    for (CompetitionStageEntity stage : stages) {
      if (stage == null || stage.getId() == null) {
        promotionValid = false;
        continue;
      }
      CompetitionEventWorkTypeStageEntity row = rowByStageId.get(stage.getId());
      if (row == null || !Objects.equals(row.getEnabled(), 1)) {
        promotionValid = false;
        continue;
      }
      try {
        CompetitionPromotionRuleDomainService.ResolvedRule rule = ruleService.resolve(
            row.getPromotionRuleType(), row.getPromotionQuota(), row.getPromotionPercentage());
        CompetitionStageType stageType = CompetitionStageType.fromCode(stage.getStageType());
        if (rule.legacy()
            || (stageType == CompetitionStageType.FINAL
                && !CompetitionPromotionRuleDomainService.FINAL_PODIUM.equals(rule.type()))
            || (stageType != CompetitionStageType.FINAL
                && CompetitionPromotionRuleDomainService.FINAL_PODIUM.equals(rule.type()))) {
          promotionValid = false;
        }
      } catch (BusinessException ex) {
        promotionValid = false;
      }
    }
    addCheck(checks, "PROMOTION_RULE_INVALID", promotionValid,
        promotionValid ? "所有赛段晋级规则完整合法" : "每个赛段必须配置合法晋级规则，决赛必须使用前三名规则");

    return scope(workType, level, stages.size(), checks);
  }

  private CompetitionEventPublishScopeReadinessVO scope(
      CompetitionEventWorkTypeEntity workType,
      CompetitionEventWorkTypeLevelEntity level,
      int stageCount,
      List<CompetitionEventPublishReadinessCheckVO> checks) {
    boolean ready = !checks.isEmpty() && checks.stream()
        .allMatch(CompetitionEventPublishReadinessCheckVO::passed);
    return new CompetitionEventPublishScopeReadinessVO(
        workType == null ? null : workType.getId(),
        workType == null ? null : workType.getWorkTypeId(),
        workType == null ? null : workTypeName(workType),
        level == null ? null : level.getId(),
        level == null ? null : level.getLevelCode(),
        level == null ? null : level.getLevelName(),
        level == null ? null : level.getStagePlanStatus(), stageCount, ready, List.copyOf(checks));
  }

  private void addEventChecks(
      CompetitionEventEntity event,
      List<CompetitionEventPublishReadinessCheckVO> checks) {
    boolean basic = StringUtils.hasText(event.getName()) && event.getName().length() <= 128
        && StringUtils.hasText(event.getCode()) && event.getCode().length() <= 64
        && event.getRegistrationAuditRequired() != null
        && (event.getRegistrationAuditRequired() == 0 || event.getRegistrationAuditRequired() == 1)
        && (event.getDescription() == null || event.getDescription().length() <= 2000);
    addCheck(checks, "EVENT_BASIC_INFO", basic,
        basic ? "赛事基础信息完整" : "赛事名称、编码、报名审核配置或描述不合法");

    boolean time = event.getStartAt() == null && event.getEndAt() == null
        || (event.getStartAt() != null && event.getEndAt() != null
            && event.getEndAt().isAfter(event.getStartAt()));
    addCheck(checks, "EVENT_TIME_INVALID", time,
        time ? "赛事时间范围有效或待补充" : "若填写赛事时间，必须同时填写开始、结束时间且结束时间晚于开始时间");

    boolean scopeValid = CompetitionEventScope.PLATFORM.getCode().equals(event.getScope())
        || CompetitionEventScope.ENTERPRISE_INTERNAL.getCode().equals(event.getScope());
    if (CompetitionEventScope.PLATFORM.getCode().equals(event.getScope())) {
      scopeValid = scopeValid && event.getOwnerOrgId() == null;
    } else if (CompetitionEventScope.ENTERPRISE_INTERNAL.getCode().equals(event.getScope())) {
      scopeValid = scopeValid && event.getOwnerOrgId() != null;
    }
    addCheck(checks, "EVENT_SCOPE_INVALID", scopeValid,
        scopeValid ? "赛事范围和所属组织配置有效" : "赛事范围或所属组织配置不合法");
  }

  private String workTypeName(CompetitionEventWorkTypeEntity workType) {
    return StringUtils.hasText(workType.getDisplayName())
        ? workType.getDisplayName() : String.valueOf(workType.getWorkTypeId());
  }

  private String scopeLabel(CompetitionEventPublishScopeReadinessVO scope) {
    return "工种“" + scope.workTypeName() + "”·等级“" + scope.levelName() + "”";
  }

  private void addCheck(
      List<CompetitionEventPublishReadinessCheckVO> checks,
      String code,
      boolean passed,
      String message) {
    addCheck(checks, code, passed, message, null, null);
  }

  private void addCheck(
      List<CompetitionEventPublishReadinessCheckVO> checks,
      String code,
      boolean passed,
      String message,
      String targetType,
      Long targetId) {
    checks.add(new CompetitionEventPublishReadinessCheckVO(
        code, passed, message, targetType, targetId));
  }

  private <T> List<T> safeList(List<T> records) {
    return records == null ? new ArrayList<>() : new ArrayList<>(records);
  }

  private record StageWindow(CompetitionStageType type, LocalDateTime startAt, LocalDateTime endAt) {
  }
}
