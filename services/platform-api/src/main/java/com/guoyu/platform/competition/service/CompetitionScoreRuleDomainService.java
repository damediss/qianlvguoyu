package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.dto.CompetitionScoreRuleCreateReq;
import com.guoyu.platform.competition.dto.CompetitionScoreRuleUpdateReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionScoreRuleEntity;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.enums.CompetitionEventStatus;
import com.guoyu.platform.competition.enums.CompetitionScoreRuleStatus;
import com.guoyu.platform.competition.enums.CompetitionTheoryTemplate;
import com.guoyu.platform.competition.mapper.CompetitionEventMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
import com.guoyu.platform.competition.mapper.CompetitionScoreRuleMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionScoreRuleDomainService {

  private static final BigDecimal TOTAL_WEIGHT = new BigDecimal("100.00");
  private static final String SCORING_SYSTEM_B = "B";

  private final CompetitionScoreRuleMapper scoreRuleMapper;
  private final CompetitionEventMapper eventMapper;
  private final CompetitionStageMapper stageMapper;
  private final CompetitionEventWorkTypeMapper eventWorkTypeMapper;

  public CompetitionScoreRuleDomainService(
      CompetitionScoreRuleMapper scoreRuleMapper,
      CompetitionEventMapper eventMapper,
      CompetitionStageMapper stageMapper,
      CompetitionEventWorkTypeMapper eventWorkTypeMapper) {
    this.scoreRuleMapper = scoreRuleMapper;
    this.eventMapper = eventMapper;
    this.stageMapper = stageMapper;
    this.eventWorkTypeMapper = eventWorkTypeMapper;
  }

  @Transactional(readOnly = true)
  public List<CompetitionScoreRuleEntity> list(
      Long tenantId, Long eventId, Long stageId, Long eventWorkTypeId) {
    LambdaQueryWrapper<CompetitionScoreRuleEntity> wrapper =
        new LambdaQueryWrapper<CompetitionScoreRuleEntity>()
            .eq(CompetitionScoreRuleEntity::getTenantId, tenantId)
            .eq(CompetitionScoreRuleEntity::getEventId, eventId)
            .orderByAsc(CompetitionScoreRuleEntity::getStageId)
            .orderByAsc(CompetitionScoreRuleEntity::getEventWorkTypeId)
            .orderByDesc(CompetitionScoreRuleEntity::getRuleVersion);
    if (stageId != null) {
      wrapper.eq(CompetitionScoreRuleEntity::getStageId, stageId);
    }
    if (eventWorkTypeId != null) {
      wrapper.eq(CompetitionScoreRuleEntity::getEventWorkTypeId, eventWorkTypeId);
    }
    List<CompetitionScoreRuleEntity> rules = scoreRuleMapper.selectList(wrapper);
    if (rules == null) {
      log.warn("Competition score rule mapper returned null list, tenantId={}, eventId={}",
          tenantId, eventId);
      return Collections.emptyList();
    }
    return rules;
  }

  @Transactional
  public CompetitionScoreRuleEntity create(
      Long tenantId, Long userId, Long eventId, CompetitionScoreRuleCreateReq req) {
    requireManageableEvent(tenantId, eventId);
    requireScopeReferences(tenantId, eventId, req.stageId(), req.eventWorkTypeId());
    validateDraftRule(
        req.scoringSystem(), req.theoryTemplate(),
        req.theoryWeight(), req.simulationPracticalWeight());

    Integer maxVersion = scoreRuleMapper.selectMaxRuleVersion(
        tenantId, eventId, req.stageId(), req.eventWorkTypeId());
    CompetitionScoreRuleEntity entity = new CompetitionScoreRuleEntity();
    entity.setTenantId(tenantId);
    entity.setEventId(eventId);
    entity.setStageId(req.stageId());
    entity.setEventWorkTypeId(req.eventWorkTypeId());
    entity.setScoringSystem(normalizeScoringSystem(req.scoringSystem()));
    entity.setTheoryTemplate(normalizeTemplate(req.theoryTemplate()));
    entity.setTheoryWeight(req.theoryWeight());
    entity.setSimulationPracticalWeight(req.simulationPracticalWeight());
    entity.setStatus(CompetitionScoreRuleStatus.DRAFT.getCode());
    entity.setRuleVersion(maxVersion == null ? 1 : maxVersion + 1);
    entity.setCreatedBy(userId);
    entity.setUpdatedBy(userId);
    entity.setDeleted(0);
    try {
      if (scoreRuleMapper.insert(entity) != 1) {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建评分规则失败");
      }
    } catch (DuplicateKeyException ex) {
      log.warn("Competition score rule version allocation conflict, tenantId={}, eventId={}, stageId={}, "
              + "eventWorkTypeId={}", tenantId, eventId, req.stageId(), req.eventWorkTypeId());
      throw new BusinessException(ErrorCode.CONFLICT, "评分规则版本已变化，请重试");
    }
    return entity;
  }

  @Transactional
  public CompetitionScoreRuleEntity update(
      Long tenantId, Long userId, Long ruleId, CompetitionScoreRuleUpdateReq req) {
    CompetitionScoreRuleEntity entity = findForUpdate(tenantId, ruleId);
    requireManageableEvent(tenantId, entity.getEventId());
    if (!CompetitionScoreRuleStatus.DRAFT.getCode().equals(entity.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "已发布或已废止的评分规则不可修改，请创建新版本");
    }
    validateDraftRule(
        req.scoringSystem(), req.theoryTemplate(),
        req.theoryWeight(), req.simulationPracticalWeight());
    entity.setScoringSystem(normalizeScoringSystem(req.scoringSystem()));
    entity.setTheoryTemplate(normalizeTemplate(req.theoryTemplate()));
    entity.setTheoryWeight(req.theoryWeight());
    entity.setSimulationPracticalWeight(req.simulationPracticalWeight());
    entity.setUpdatedBy(userId);
    if (scoreRuleMapper.updateById(entity) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "评分规则状态已变化，请刷新后重试");
    }
    return entity;
  }

  @Transactional
  public CompetitionScoreRuleEntity publish(Long tenantId, Long userId, Long ruleId) {
    CompetitionScoreRuleEntity entity = findForUpdate(tenantId, ruleId);
    requireManageableEvent(tenantId, entity.getEventId());
    requireScopeReferences(
        tenantId, entity.getEventId(), entity.getStageId(), entity.getEventWorkTypeId());
    if (!CompetitionScoreRuleStatus.DRAFT.getCode().equals(entity.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "只有草稿评分规则可以发布");
    }
    validatePublishedRule(
        entity.getScoringSystem(), entity.getTheoryTemplate(),
        entity.getTheoryWeight(), entity.getSimulationPracticalWeight());

    List<CompetitionScoreRuleEntity> published = scoreRuleMapper.selectPublishedForUpdate(
        tenantId, entity.getEventId(), entity.getStageId(), entity.getEventWorkTypeId());
    if (published == null) {
      log.warn("Competition score rule mapper returned null published list, tenantId={}, ruleId={}",
          tenantId, ruleId);
      published = Collections.emptyList();
    }
    LocalDateTime publishedAt = LocalDateTime.now();
    scoreRuleMapper.deprecatePublishedInScope(
        tenantId, entity.getEventId(), entity.getStageId(), entity.getEventWorkTypeId(),
        entity.getId(), userId, publishedAt);

    entity.setStatus(CompetitionScoreRuleStatus.PUBLISHED.getCode());
    entity.setPublishedAt(publishedAt);
    entity.setPublishedBy(userId);
    entity.setUpdatedBy(userId);
    if (scoreRuleMapper.updateById(entity) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "评分规则状态已变化，请刷新后重试");
    }
    return entity;
  }

  @Transactional(readOnly = true)
  public CompetitionScoreRuleEntity getById(Long tenantId, Long ruleId) {
    CompetitionScoreRuleEntity entity = scoreRuleMapper.selectOne(
        new LambdaQueryWrapper<CompetitionScoreRuleEntity>()
            .eq(CompetitionScoreRuleEntity::getTenantId, tenantId)
            .eq(CompetitionScoreRuleEntity::getId, ruleId));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "评分规则不存在");
    }
    return entity;
  }

  public boolean isValidRule(CompetitionScoreRuleEntity entity) {
    if (entity == null) {
      return false;
    }
    try {
      validatePublishedRule(
          entity.getScoringSystem(), entity.getTheoryTemplate(),
          entity.getTheoryWeight(), entity.getSimulationPracticalWeight());
      return true;
    } catch (BusinessException ex) {
      return false;
    }
  }

  private CompetitionScoreRuleEntity findForUpdate(Long tenantId, Long ruleId) {
    CompetitionScoreRuleEntity entity = scoreRuleMapper.selectForUpdate(tenantId, ruleId);
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "评分规则不存在");
    }
    return entity;
  }

  private CompetitionEventEntity requireManageableEvent(Long tenantId, Long eventId) {
    CompetitionEventEntity event = eventMapper.selectOne(
        new LambdaQueryWrapper<CompetitionEventEntity>()
            .eq(CompetitionEventEntity::getTenantId, tenantId)
            .eq(CompetitionEventEntity::getId, eventId));
    if (event == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "赛事不存在");
    }
    if (!CompetitionEventStatus.DRAFT.getCode().equals(event.getStatus())
        && !CompetitionEventStatus.PUBLISHED.getCode().equals(event.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "已关闭或已归档赛事不可管理评分规则");
    }
    return event;
  }

  private void requireScopeReferences(
      Long tenantId, Long eventId, Long stageId, Long eventWorkTypeId) {
    CompetitionStageEntity stage = stageMapper.selectOne(
        new LambdaQueryWrapper<CompetitionStageEntity>()
            .eq(CompetitionStageEntity::getTenantId, tenantId)
            .eq(CompetitionStageEntity::getId, stageId)
            .eq(CompetitionStageEntity::getEventId, eventId));
    if (stage == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "赛段不存在或不属于当前赛事");
    }
    CompetitionEventWorkTypeEntity eventWorkType = eventWorkTypeMapper.selectOne(
        new LambdaQueryWrapper<CompetitionEventWorkTypeEntity>()
            .eq(CompetitionEventWorkTypeEntity::getTenantId, tenantId)
            .eq(CompetitionEventWorkTypeEntity::getId, eventWorkTypeId)
            .eq(CompetitionEventWorkTypeEntity::getEventId, eventId));
    if (eventWorkType == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "赛事工种不存在或不属于当前赛事");
    }
  }

  private void validateDraftRule(
      String scoringSystem,
      String theoryTemplate,
      BigDecimal theoryWeight,
      BigDecimal simulationPracticalWeight) {
    if (!SCORING_SYSTEM_B.equals(normalizeScoringSystem(scoringSystem))) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "本期仅支持赛制B");
    }
    String normalizedTemplate = normalizeTemplate(theoryTemplate);
    if (normalizedTemplate != null && !CompetitionTheoryTemplate.supports(normalizedTemplate)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "理论模板仅支持THEORY_90或THEORY_120");
    }
    requireOptionalPercentage(theoryWeight, "理论权重", ErrorCode.BAD_REQUEST);
    requireOptionalPercentage(
        simulationPracticalWeight, "仿真实操组合权重", ErrorCode.BAD_REQUEST);
    if (theoryWeight != null && simulationPracticalWeight != null
        && theoryWeight.add(simulationPracticalWeight).compareTo(TOTAL_WEIGHT) != 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "理论权重与仿真实操组合权重之和必须为100");
    }
  }

  private void validatePublishedRule(
      String scoringSystem,
      String theoryTemplate,
      BigDecimal theoryWeight,
      BigDecimal simulationPracticalWeight) {
    if (!SCORING_SYSTEM_B.equals(normalizeScoringSystem(scoringSystem))) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "本期仅支持赛制B");
    }
    if (!CompetitionTheoryTemplate.supports(normalizeTemplate(theoryTemplate))) {
      throw new BusinessException(
          ErrorCode.UNPROCESSABLE_ENTITY, "发布前必须选择THEORY_90或THEORY_120理论模板");
    }
    if (theoryWeight == null || simulationPracticalWeight == null) {
      throw new BusinessException(
          ErrorCode.UNPROCESSABLE_ENTITY, "发布前必须填写理论权重和仿真实操组合权重");
    }
    requireOptionalPercentage(theoryWeight, "理论权重", ErrorCode.UNPROCESSABLE_ENTITY);
    requireOptionalPercentage(
        simulationPracticalWeight, "仿真实操组合权重", ErrorCode.UNPROCESSABLE_ENTITY);
    if (theoryWeight.add(simulationPracticalWeight).compareTo(TOTAL_WEIGHT) != 0) {
      throw new BusinessException(
          ErrorCode.UNPROCESSABLE_ENTITY, "理论权重与仿真实操组合权重之和必须为100");
    }
  }

  private void requireOptionalPercentage(BigDecimal value, String name, String errorCode) {
    if (value != null && (value.compareTo(BigDecimal.ZERO) < 0
        || value.compareTo(TOTAL_WEIGHT) > 0)) {
      throw new BusinessException(errorCode, name + "必须在0到100之间");
    }
  }

  private String normalizeScoringSystem(String scoringSystem) {
    return scoringSystem == null || scoringSystem.isBlank() ? SCORING_SYSTEM_B : scoringSystem;
  }

  private String normalizeTemplate(String theoryTemplate) {
    return theoryTemplate == null || theoryTemplate.isBlank() ? null : theoryTemplate;
  }
}
