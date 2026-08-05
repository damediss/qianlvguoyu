package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.domain.CompetitionPracticalRuleDomainService;
import com.guoyu.platform.competition.dto.CompetitionPracticalRuleCreateReq;
import com.guoyu.platform.competition.dto.CompetitionPracticalRuleItemReq;
import com.guoyu.platform.competition.entity.CompetitionPracticalRuleEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalRuleItemEntity;
import com.guoyu.platform.competition.mapper.CompetitionPracticalRuleItemMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalRuleMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompetitionPracticalRuleService {
  private final CompetitionPracticalRuleMapper ruleMapper;
  private final CompetitionPracticalRuleItemMapper itemMapper;
  private final CompetitionPracticalRuleDomainService domainService =
      new CompetitionPracticalRuleDomainService();

  public CompetitionPracticalRuleService(
      CompetitionPracticalRuleMapper ruleMapper,
      CompetitionPracticalRuleItemMapper itemMapper) {
    this.ruleMapper = ruleMapper;
    this.itemMapper = itemMapper;
  }

  @Transactional(readOnly = true)
  public List<CompetitionPracticalRuleEntity> list(Long tenantId, Long stageId, Long workTypeId, Long levelId) {
    List<CompetitionPracticalRuleEntity> rows = ruleMapper.selectList(new LambdaQueryWrapper<CompetitionPracticalRuleEntity>()
        .eq(CompetitionPracticalRuleEntity::getTenantId, tenantId)
        .eq(CompetitionPracticalRuleEntity::getStageId, stageId)
        .eq(CompetitionPracticalRuleEntity::getEventWorkTypeId, workTypeId)
        .eq(levelId != null, CompetitionPracticalRuleEntity::getEventWorkTypeLevelId, levelId)
        .eq(CompetitionPracticalRuleEntity::getDeleted, 0)
        .orderByDesc(CompetitionPracticalRuleEntity::getRuleVersion));
    return rows == null ? new ArrayList<>() : new ArrayList<>(rows);
  }

  @Transactional(readOnly = true)
  public CompetitionPracticalRuleEntity get(Long tenantId, Long ruleId) {
    CompetitionPracticalRuleEntity row = ruleMapper.selectOne(new LambdaQueryWrapper<CompetitionPracticalRuleEntity>()
        .eq(CompetitionPracticalRuleEntity::getTenantId, tenantId)
        .eq(CompetitionPracticalRuleEntity::getId, ruleId)
        .eq(CompetitionPracticalRuleEntity::getDeleted, 0));
    if (row == null) throw new BusinessException(ErrorCode.NOT_FOUND, "实操评分规则不存在");
    return row;
  }

  @Transactional(readOnly = true)
  public List<CompetitionPracticalRuleItemEntity> items(Long tenantId, Long ruleId) {
    List<CompetitionPracticalRuleItemEntity> rows = itemMapper.selectList(new LambdaQueryWrapper<CompetitionPracticalRuleItemEntity>()
        .eq(CompetitionPracticalRuleItemEntity::getTenantId, tenantId)
        .eq(CompetitionPracticalRuleItemEntity::getRuleId, ruleId)
        .eq(CompetitionPracticalRuleItemEntity::getDeleted, 0)
        .orderByAsc(CompetitionPracticalRuleItemEntity::getSortOrder));
    return rows == null ? new ArrayList<>() : new ArrayList<>(rows);
  }

  @Transactional
  public CompetitionPracticalRuleEntity create(
      Long tenantId, Long userId, Long eventId, Long stageId, CompetitionPracticalRuleCreateReq req) {
    BigDecimal total = domainService.validateAndTotal(req.items());
    int version = ruleMapper.selectList(new LambdaQueryWrapper<CompetitionPracticalRuleEntity>()
        .eq(CompetitionPracticalRuleEntity::getTenantId, tenantId)
        .eq(CompetitionPracticalRuleEntity::getStageId, stageId)
        .eq(CompetitionPracticalRuleEntity::getEventWorkTypeId, req.eventWorkTypeId())
        .eq(req.eventWorkTypeLevelId() != null, CompetitionPracticalRuleEntity::getEventWorkTypeLevelId,
            req.eventWorkTypeLevelId())
        .isNull(req.eventWorkTypeLevelId() == null, CompetitionPracticalRuleEntity::getEventWorkTypeLevelId)
        .eq(CompetitionPracticalRuleEntity::getDeleted, 0))
        .stream().map(CompetitionPracticalRuleEntity::getRuleVersion).filter(v -> v != null)
        .max(Comparator.naturalOrder()).orElse(0) + 1;
    CompetitionPracticalRuleEntity rule = new CompetitionPracticalRuleEntity();
    rule.setId(IdWorker.getId());
    rule.setTenantId(tenantId);
    rule.setEventId(eventId);
    rule.setStageId(stageId);
    rule.setEventWorkTypeId(req.eventWorkTypeId());
    rule.setEventWorkTypeLevelId(req.eventWorkTypeLevelId());
    rule.setRuleVersion(version);
    rule.setRuleName(req.ruleName() == null || req.ruleName().isBlank() ? "实操评分规则-v" + version : req.ruleName().trim());
    rule.setTotalScore(total);
    rule.setStatus("draft");
    if (ruleMapper.insert(rule) != 1) throw new BusinessException(ErrorCode.CONFLICT, "实操评分规则保存失败");
    insertItems(tenantId, userId, rule.getId(), req.items());
    return rule;
  }

  @Transactional
  public CompetitionPracticalRuleEntity publish(Long tenantId, Long userId, Long ruleId) {
    CompetitionPracticalRuleEntity rule = get(tenantId, ruleId);
    if (!"draft".equals(rule.getStatus())) throw new BusinessException(ErrorCode.CONFLICT, "仅草稿规则允许发布");
    List<CompetitionPracticalRuleItemEntity> items = items(tenantId, ruleId);
    BigDecimal total = items.stream().filter(i -> "enabled".equals(i.getStatus()))
        .map(CompetitionPracticalRuleItemEntity::getMaxScore).reduce(BigDecimal.ZERO, BigDecimal::add);
    if (total.compareTo(rule.getTotalScore()) != 0) throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "启用评分项合计必须等于规则总分");
    ruleMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<CompetitionPracticalRuleEntity>()
        .eq(CompetitionPracticalRuleEntity::getTenantId, tenantId)
        .eq(CompetitionPracticalRuleEntity::getStageId, rule.getStageId())
        .eq(CompetitionPracticalRuleEntity::getEventWorkTypeId, rule.getEventWorkTypeId())
        .eq(rule.getEventWorkTypeLevelId() != null, CompetitionPracticalRuleEntity::getEventWorkTypeLevelId, rule.getEventWorkTypeLevelId())
        .isNull(rule.getEventWorkTypeLevelId() == null, CompetitionPracticalRuleEntity::getEventWorkTypeLevelId)
        .eq(CompetitionPracticalRuleEntity::getStatus, "published")
        .eq(CompetitionPracticalRuleEntity::getDeleted, 0)
        .set(CompetitionPracticalRuleEntity::getStatus, "withdrawn"));
    rule.setStatus("published");
    rule.setPublishedAt(LocalDateTime.now());
    rule.setPublishedBy(userId);
    if (ruleMapper.updateById(rule) != 1) throw new BusinessException(ErrorCode.CONFLICT, "实操评分规则发布失败");
    return rule;
  }

  private void insertItems(Long tenantId, Long userId, Long ruleId, List<CompetitionPracticalRuleItemReq> requests) {
    for (int index = 0; index < requests.size(); index++) {
      CompetitionPracticalRuleItemReq req = requests.get(index);
      CompetitionPracticalRuleItemEntity item = new CompetitionPracticalRuleItemEntity();
      item.setId(IdWorker.getId());
      item.setTenantId(tenantId);
      item.setRuleId(ruleId);
      item.setItemCode(req.itemCode().trim());
      item.setItemName(req.itemName().trim());
      item.setSortOrder(index + 1);
      item.setMaxScore(req.maxScore());
      item.setDeductionAllowed(Boolean.FALSE.equals(req.deductionAllowed()) ? 0 : 1);
      item.setCriteriaPayload(req.criteriaPayload());
      item.setStatus("enabled");
      item.setCreatedBy(userId);
      item.setUpdatedBy(userId);
      if (itemMapper.insert(item) != 1) throw new BusinessException(ErrorCode.CONFLICT, "实操评分项保存失败");
    }
  }
}
