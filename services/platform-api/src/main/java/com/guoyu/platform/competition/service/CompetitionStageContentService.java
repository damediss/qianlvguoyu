package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.dto.CompetitionStageContentBatchReq;
import com.guoyu.platform.competition.dto.CompetitionStageContentItemReq;
import com.guoyu.platform.competition.entity.CompetitionStageContentEntity;
import com.guoyu.platform.competition.enums.CompetitionStageContentType;
import com.guoyu.platform.competition.enums.CompetitionStageStatus;
import com.guoyu.platform.competition.mapper.CompetitionStageContentMapper;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionStageContentService {

  private final CompetitionStageContentMapper mapper;
  private final CompetitionStageService stageService;

  public CompetitionStageContentService(
      CompetitionStageContentMapper mapper,
      CompetitionStageService stageService) {
    this.mapper = mapper;
    this.stageService = stageService;
  }

  @Transactional(readOnly = true)
  public List<CompetitionStageContentEntity> list(Long tenantId, Long stageId) {
    CompetitionStageEntitySupport.require(stageService.getById(tenantId, stageId));
    List<CompetitionStageContentEntity> records = mapper.selectList(
        new LambdaQueryWrapper<CompetitionStageContentEntity>()
            .eq(CompetitionStageContentEntity::getTenantId, tenantId)
            .eq(CompetitionStageContentEntity::getStageId, stageId)
            .eq(CompetitionStageContentEntity::getDeleted, 0)
            .orderByAsc(CompetitionStageContentEntity::getSortOrder)
            .orderByAsc(CompetitionStageContentEntity::getId));
    return records == null ? new ArrayList<>() : new ArrayList<>(records);
  }

  @Transactional
  public List<CompetitionStageContentEntity> replace(
      Long tenantId, Long stageId, CompetitionStageContentBatchReq req) {
    var stage = stageService.getById(tenantId, stageId);
    var event = stageService.lockEventForArrangementUpdate(tenantId, stage.getEventId());
    com.guoyu.platform.competition.domain.CompetitionEventArrangementGuard.requireOpen(
        event, "已关闭或已归档赛事不允许修改赛段内容");
    if (CompetitionStageStatus.fromCode(stage.getStatus()) == null
        || !CompetitionStageStatus.writableStatuses().contains(
            CompetitionStageStatus.fromCode(stage.getStatus()))) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前赛段状态不允许修改比赛内容");
    }
    if (req == null || req.contents() == null || req.contents().isEmpty()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "至少需要启用一项比赛内容");
    }
    Set<String> codes = req.contents().stream()
        .map(CompetitionStageContentItemReq::contentType)
        .collect(Collectors.toSet());
    if (codes.size() != req.contents().size() || codes.stream().anyMatch(code ->
        CompetitionStageContentType.fromCode(code) == null)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "比赛内容类型不合法或重复");
    }
    boolean hasEnabledContent = req.contents().stream().anyMatch(item ->
        item.status() == null || item.status().isBlank() || "enabled".equals(item.status()));
    if (!hasEnabledContent) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "至少需要启用一项比赛内容");
    }
    validateWeights(req);
    List<CompetitionStageContentEntity> existing = listRaw(tenantId, stageId);
    List<CompetitionStageContentEntity> result = new ArrayList<>();
    Set<Long> retainedIds = new HashSet<>();
    for (int index = 0; index < req.contents().size(); index++) {
      CompetitionStageContentItemReq item = req.contents().get(index);
      CompetitionStageContentEntity entity = existing.stream()
          .filter(current -> Objects.equals(current.getContentType(), item.contentType()))
          .findFirst().orElseGet(CompetitionStageContentEntity::new);
      entity.setTenantId(tenantId);
      entity.setEventId(stage.getEventId());
      entity.setStageId(stageId);
      entity.setContentType(item.contentType());
      entity.setStatus(item.status() == null || item.status().isBlank() ? "enabled" : item.status());
      entity.setWeight(item.weight() == null ? BigDecimal.ZERO : item.weight());
      entity.setSortOrder(index + 1);
      entity.setProviderCode(item.providerCode());
      entity.setDeleted(0);
      try {
        if (entity.getId() == null) {
          if (mapper.insert(entity) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "比赛内容保存失败");
          }
        } else if (mapper.updateById(entity) != 1) {
          throw new BusinessException(ErrorCode.CONFLICT, "比赛内容已发生变化，请刷新后重试");
        }
      } catch (DuplicateKeyException ex) {
        throw new BusinessException(ErrorCode.CONFLICT, "比赛内容已发生变化，请刷新后重试");
      }
      retainedIds.add(entity.getId());
      result.add(entity);
    }
    for (CompetitionStageContentEntity entity : existing) {
      if (!retainedIds.contains(entity.getId()) && mapper.deleteById(entity.getId()) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "比赛内容删除失败，请刷新后重试");
      }
    }
    return result;
  }

  private List<CompetitionStageContentEntity> listRaw(Long tenantId, Long stageId) {
    List<CompetitionStageContentEntity> records = mapper.selectList(
        new LambdaQueryWrapper<CompetitionStageContentEntity>()
            .eq(CompetitionStageContentEntity::getTenantId, tenantId)
            .eq(CompetitionStageContentEntity::getStageId, stageId)
            .eq(CompetitionStageContentEntity::getDeleted, 0));
    return records == null ? new ArrayList<>() : new ArrayList<>(records);
  }

  private void validateWeights(CompetitionStageContentBatchReq req) {
    boolean hasWeight = req.contents().stream().anyMatch(item -> item.weight() != null);
    if (!hasWeight) {
      return;
    }
    BigDecimal total = BigDecimal.ZERO;
    for (CompetitionStageContentItemReq item : req.contents()) {
      String status = item.status() == null || item.status().isBlank() ? "enabled" : item.status();
      BigDecimal weight = item.weight() == null ? BigDecimal.ZERO : item.weight();
      if ("enabled".equals(status) && weight.compareTo(BigDecimal.ZERO) <= 0) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "启用内容的权重必须大于0");
      }
      if ("disabled".equals(status) && weight.compareTo(BigDecimal.ZERO) != 0) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "未启用内容的权重必须为0");
      }
      if ("enabled".equals(status)) {
        total = total.add(weight);
      }
    }
    if (total.compareTo(new BigDecimal("100.00")) != 0) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "启用内容的权重合计必须为100");
    }
  }

  private static final class CompetitionStageEntitySupport {
    private static void require(Object value) {
      if (value == null) {
        throw new BusinessException(ErrorCode.NOT_FOUND, "赛段不存在");
      }
    }

    private CompetitionStageEntitySupport() {
    }
  }
}
