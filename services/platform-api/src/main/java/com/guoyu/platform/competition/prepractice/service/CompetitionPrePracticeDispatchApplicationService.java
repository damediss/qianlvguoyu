package com.guoyu.platform.competition.prepractice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.prepractice.dto.CompetitionPrePracticeContestantQueryReq;
import com.guoyu.platform.competition.prepractice.dto.CompetitionPrePracticeDispatchCreateReq;
import com.guoyu.platform.competition.prepractice.dto.CompetitionPrePracticeDispatchQueryReq;
import com.guoyu.platform.competition.prepractice.entity.CompetitionPrePracticeDispatchBatchEntity;
import com.guoyu.platform.competition.prepractice.entity.CompetitionPrePracticeDispatchRecipientEntity;
import com.guoyu.platform.competition.prepractice.entity.CompetitionPrePracticeTaskEntity;
import com.guoyu.platform.competition.prepractice.mapper.CompetitionPrePracticeDispatchBatchMapper;
import com.guoyu.platform.competition.prepractice.mapper.CompetitionPrePracticeDispatchRecipientMapper;
import com.guoyu.platform.competition.prepractice.mapper.CompetitionPrePracticeTaskMapper;
import com.guoyu.platform.competition.prepractice.mapper.row.CompetitionPrePracticeContestantRow;
import com.guoyu.platform.competition.prepractice.mapper.row.CompetitionPrePracticeOrganizationRow;
import com.guoyu.platform.competition.prepractice.vo.CompetitionPrePracticeContestantVO;
import com.guoyu.platform.competition.prepractice.vo.CompetitionPrePracticeDispatchBatchVO;
import com.guoyu.platform.competition.prepractice.vo.CompetitionPrePracticeOrganizationVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class CompetitionPrePracticeDispatchApplicationService {

  private static final int MAX_RECIPIENTS_PER_BATCH = 5000;

  private final CompetitionPrePracticeDispatchBatchMapper batchMapper;
  private final CompetitionPrePracticeDispatchRecipientMapper recipientMapper;
  private final CompetitionPrePracticeTaskMapper taskMapper;

  public CompetitionPrePracticeDispatchApplicationService(
      CompetitionPrePracticeDispatchBatchMapper batchMapper,
      CompetitionPrePracticeDispatchRecipientMapper recipientMapper,
      CompetitionPrePracticeTaskMapper taskMapper) {
    this.batchMapper = batchMapper;
    this.recipientMapper = recipientMapper;
    this.taskMapper = taskMapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<CompetitionPrePracticeContestantVO> pageContestants(
      CompetitionPrePracticeContestantQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Page<CompetitionPrePracticeContestantRow> page = batchMapper.pageEligibleContestants(
        new Page<>(req.getPage(), req.getPageSize()), tenantId,
        req.getEventId(), req.getEventWorkTypeId(), req.getLevelCode(), req.getOrgId(), null);
    List<CompetitionPrePracticeContestantVO> items = page == null || page.getRecords() == null
        ? List.of() : page.getRecords().stream().map(this::toContestantVO).toList();
    if (page == null) {
      log.warn("Pre-practice contestant query returned null page, tenantId={}, eventId={}",
          tenantId, req.getEventId());
      return PageResponse.empty(req.getPage(), req.getPageSize());
    }
    return new PageResponse<>(items, page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public List<CompetitionPrePracticeOrganizationVO> organizations(
      CompetitionPrePracticeContestantQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    List<CompetitionPrePracticeOrganizationRow> rows = batchMapper.selectEligibleOrganizations(
        tenantId, req.getEventId(), req.getEventWorkTypeId(), req.getLevelCode());
    if (rows == null || rows.isEmpty()) return List.of();
    return rows.stream()
        .filter(Objects::nonNull)
        .map(row -> new CompetitionPrePracticeOrganizationVO(
            row.getOrgId() == null ? null : row.getOrgId().toString(), row.getOrgName()))
        .toList();
  }

  @Transactional(readOnly = true)
  public PageResponse<CompetitionPrePracticeDispatchBatchVO> pageDispatches(
      CompetitionPrePracticeDispatchQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Page<CompetitionPrePracticeDispatchBatchEntity> page = batchMapper.selectPage(
        new Page<>(req.getPage(), req.getPageSize()),
        new LambdaQueryWrapper<CompetitionPrePracticeDispatchBatchEntity>()
            .eq(CompetitionPrePracticeDispatchBatchEntity::getTenantId, tenantId)
            .eq(req.getEventId() != null, CompetitionPrePracticeDispatchBatchEntity::getEventId,
                req.getEventId())
            .eq(req.getEventWorkTypeId() != null,
                CompetitionPrePracticeDispatchBatchEntity::getEventWorkTypeId,
                req.getEventWorkTypeId())
            .eq(StringUtils.hasText(req.getLevelCode()),
                CompetitionPrePracticeDispatchBatchEntity::getEventWorkTypeLevelCode,
                req.getLevelCode())
            .orderByDesc(CompetitionPrePracticeDispatchBatchEntity::getPushedAt)
            .orderByDesc(CompetitionPrePracticeDispatchBatchEntity::getId));
    List<CompetitionPrePracticeDispatchBatchVO> items = page == null || page.getRecords() == null
        ? List.of() : page.getRecords().stream().map(this::toBatchVO).toList();
    if (page == null) return PageResponse.empty(req.getPage(), req.getPageSize());
    return new PageResponse<>(items, page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional
  public CompetitionPrePracticeDispatchBatchVO dispatch(
      CompetitionPrePracticeDispatchCreateReq req) {
    PlatformUserPrincipal actor = SecurityContext.currentUser();
    Long tenantId = actor.getTenantId();
    CompetitionPrePracticeTaskEntity task = taskMapper.selectOne(
        new LambdaQueryWrapper<CompetitionPrePracticeTaskEntity>()
            .eq(CompetitionPrePracticeTaskEntity::getTenantId, tenantId)
            .eq(CompetitionPrePracticeTaskEntity::getId, req.taskId()));
    if (task == null || !"configured".equals(task.getStatus())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "已生成任务不存在或当前不可推送");
    }
    if (!Objects.equals(task.getEventId(), req.eventId())
        || !Objects.equals(task.getEventWorkTypeId(), req.eventWorkTypeId())
        || !Objects.equals(task.getEventWorkTypeLevelCode(), req.levelCode())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "所选任务与赛事、工种或等级不匹配");
    }

    boolean selectAll = Boolean.TRUE.equals(req.selectAllMatching());
    List<Long> profileIds = req.contestantProfileIds() == null
        ? List.of() : req.contestantProfileIds();
    if (selectAll && !profileIds.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "全选筛选结果时不能同时提交单选名单");
    }
    if (!selectAll && profileIds.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请至少选择一名选手");
    }
    if (!selectAll && profileIds.stream().distinct().count() != profileIds.size()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "选手名单中存在重复记录");
    }

    long requestedPageSize = selectAll
        ? MAX_RECIPIENTS_PER_BATCH + 1L : Math.max(profileIds.size(), 1);
    Page<CompetitionPrePracticeContestantRow> selectedPage = batchMapper.pageEligibleContestants(
        new Page<>(1, requestedPageSize), tenantId, req.eventId(), req.eventWorkTypeId(),
        req.levelCode(), req.orgId(), selectAll ? null : profileIds);
    if (selectedPage == null || selectedPage.getRecords() == null || selectedPage.getRecords().isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "当前筛选范围内没有可接收任务的已激活选手");
    }
    if (!selectAll && selectedPage.getTotal() != profileIds.size()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "部分选手已不属于当前赛事、工种、等级或单位范围，请刷新名单");
    }
    if (selectedPage.getTotal() > MAX_RECIPIENTS_PER_BATCH) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "单次推送不能超过 5000 名选手，请按单位缩小筛选范围");
    }

    Map<Long, CompetitionPrePracticeContestantRow> uniquePlayers = new LinkedHashMap<>();
    for (CompetitionPrePracticeContestantRow row : selectedPage.getRecords()) {
      if (row != null && row.getPlayerId() != null) uniquePlayers.putIfAbsent(row.getPlayerId(), row);
    }
    if (uniquePlayers.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "所选选手没有有效的竞赛账号");
    }

    LocalDateTime pushedAt = LocalDateTime.now();
    CompetitionPrePracticeDispatchBatchEntity batch = new CompetitionPrePracticeDispatchBatchEntity();
    batch.setTenantId(tenantId);
    batch.setTaskId(task.getId());
    batch.setTaskTitleSnapshot(task.getTitle());
    batch.setEventId(task.getEventId());
    batch.setEventNameSnapshot(task.getEventNameSnapshot());
    batch.setEventWorkTypeId(task.getEventWorkTypeId());
    batch.setEventWorkTypeNameSnapshot(task.getEventWorkTypeNameSnapshot());
    batch.setEventWorkTypeLevelCode(task.getEventWorkTypeLevelCode());
    batch.setEventWorkTypeLevelNameSnapshot(task.getEventWorkTypeLevelNameSnapshot());
    batch.setStageId(task.getStageId());
    batch.setStageNameSnapshot(task.getStageNameSnapshot());
    batch.setRecipientCount(uniquePlayers.size());
    batch.setPushedAt(pushedAt);
    batch.setCreatedBy(actor.getUserId());
    batch.setUpdatedBy(actor.getUserId());
    if (batchMapper.insert(batch) != 1 || batch.getId() == null) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "推送批次创建失败");
    }

    for (CompetitionPrePracticeContestantRow candidate : uniquePlayers.values()) {
      CompetitionPrePracticeDispatchRecipientEntity recipient = new CompetitionPrePracticeDispatchRecipientEntity();
      recipient.setTenantId(tenantId);
      recipient.setBatchId(batch.getId());
      recipient.setTaskId(task.getId());
      recipient.setPlayerId(candidate.getPlayerId());
      recipient.setContestantProfileId(candidate.getProfileId());
      recipient.setRegistrationId(candidate.getRegistrationId());
      recipient.setOrgId(candidate.getOrgId());
      recipient.setOrgNameSnapshot(candidate.getOrgName());
      recipient.setStatus("delivered");
      recipient.setDeliveredAt(pushedAt);
      recipient.setCreatedBy(actor.getUserId());
      recipient.setUpdatedBy(actor.getUserId());
      if (recipientMapper.insert(recipient) != 1) {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "选手任务写入失败");
      }
    }

    task.setDeliveryStatus("pushed");
    task.setUpdatedBy(actor.getUserId());
    if (taskMapper.updateById(task) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "任务推送状态更新失败");
    }
    log.info("Competition pre-practice task pushed, tenantId={}, userId={}, taskId={}, batchId={}, recipientCount={}",
        tenantId, actor.getUserId(), task.getId(), batch.getId(), uniquePlayers.size());
    return toBatchVO(batch);
  }

  private CompetitionPrePracticeContestantVO toContestantVO(CompetitionPrePracticeContestantRow row) {
    return new CompetitionPrePracticeContestantVO(
        value(row.getProfileId()), value(row.getRegistrationId()), value(row.getPlayerId()),
        row.getContestantNo(), row.getContestantName(), value(row.getOrgId()), row.getOrgName());
  }

  private CompetitionPrePracticeDispatchBatchVO toBatchVO(CompetitionPrePracticeDispatchBatchEntity batch) {
    return new CompetitionPrePracticeDispatchBatchVO(
        value(batch.getId()), value(batch.getTaskId()), batch.getTaskTitleSnapshot(),
        value(batch.getEventId()), batch.getEventNameSnapshot(), value(batch.getEventWorkTypeId()),
        batch.getEventWorkTypeNameSnapshot(), batch.getEventWorkTypeLevelCode(),
        batch.getEventWorkTypeLevelNameSnapshot(), value(batch.getStageId()),
        batch.getStageNameSnapshot(), batch.getRecipientCount(), batch.getPushedAt());
  }

  private String value(Long value) {
    return value == null ? null : value.toString();
  }
}
