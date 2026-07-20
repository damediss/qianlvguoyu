package com.guoyu.platform.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.dto.FaqConversionQueryReq;
import com.guoyu.platform.exam.entity.ExamFaqConversionBatchEntity;
import com.guoyu.platform.exam.mapper.ExamFaqConversionBatchMapper;
import com.guoyu.platform.exam.vo.FaqBatchVO;
import com.guoyu.platform.security.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** FAQ 转换批次管理 — 创建/进度计算/暂停/恢复 */
@Service
@Slf4j
public class FaqConversionBatchService {

  public static final String STATUS_RUNNING = "running";
  public static final String STATUS_PAUSED = "paused";
  public static final String STATUS_COMPLETED = "completed";
  public static final String STATUS_CANCELED = "canceled";

  private final ExamFaqConversionBatchMapper batchMapper;

  public FaqConversionBatchService(ExamFaqConversionBatchMapper batchMapper) {
    this.batchMapper = batchMapper;
  }

  /** 查批次列表 */
  @Transactional(readOnly = true)
  public PageResponse<FaqBatchVO> page(Long tenantId, String status, Long page, long pageSize) {
    LambdaQueryWrapper<ExamFaqConversionBatchEntity> wrapper = new LambdaQueryWrapper<ExamFaqConversionBatchEntity>()
        .eq(ExamFaqConversionBatchEntity::getTenantId, tenantId)
        .eq(status != null && !status.isBlank(), ExamFaqConversionBatchEntity::getStatus, status)
        .orderByDesc(ExamFaqConversionBatchEntity::getId);
    Page<ExamFaqConversionBatchEntity> p = batchMapper.selectPage(new Page<>(page, pageSize), wrapper);
    return new PageResponse<>(
        p.getRecords().stream().map(FaqBatchVO::from).toList(),
        p.getCurrent(), p.getSize(), p.getTotal());
  }

  /** 查单条批次 */
  @Transactional(readOnly = true)
  public FaqBatchVO getById(Long tenantId, Long batchId) {
    ExamFaqConversionBatchEntity entity = batchMapper.selectById(batchId);
    if (entity == null || !entity.getTenantId().equals(tenantId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "批次不存在");
    }
    return FaqBatchVO.from(entity);
  }

  /** 查单条批次实体（供下游服务使用） */
  @Transactional(readOnly = true)
  public ExamFaqConversionBatchEntity getBatchEntity(Long tenantId, Long batchId) {
    ExamFaqConversionBatchEntity entity = batchMapper.selectById(batchId);
    if (entity == null || !entity.getTenantId().equals(tenantId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "批次不存在");
    }
    return entity;
  }

  /** 创建批次 */
  @Transactional
  public ExamFaqConversionBatchEntity createBatch(Long tenantId, Long userId, Integer workTypeId,
                                                   String workTypeName, String modelCode, String modelName,
                                                   Integer totalExams, Integer concurrencyLimit) {
    LocalDateTime now = LocalDateTime.now();
    ExamFaqConversionBatchEntity batch = new ExamFaqConversionBatchEntity();
    batch.setId(IdWorker.getId());
    batch.setTenantId(tenantId);
    batch.setWorkTypeId(workTypeId);
    batch.setWorkTypeName(workTypeName);
    batch.setModelCode(modelCode);
    batch.setModelName(modelName);
    batch.setTotalExams(totalExams);
    batch.setSuccessCount(0);
    batch.setFailedCount(0);
    batch.setSkippedCount(0);
    batch.setPendingCount(totalExams);
    batch.setStatus(STATUS_RUNNING);
    batch.setStartedAt(now);
    batch.setConcurrencyLimit(concurrencyLimit != null ? concurrencyLimit : 5);
    batch.setCreatedBy(userId);
    batch.setUpdatedBy(userId);
    batch.setCreatedAt(now);
    batch.setUpdatedAt(now);
    batch.setDeleted(0);

    batchMapper.insert(batch);
    log.info("FAQ 转换批次已创建：batchId={}, workTypeId={}, modelCode={}", batch.getId(), workTypeId, modelCode);
    return batch;
  }

  /** 暂停批次 */
  @Transactional
  public void pauseBatch(Long tenantId, Long batchId) {
    ExamFaqConversionBatchEntity entity = getBatchEntity(tenantId, batchId);
    if (!STATUS_RUNNING.equals(entity.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "批次状态不是运行中，无法暂停");
    }
    entity.setStatus(STATUS_PAUSED);
    entity.setUpdatedAt(LocalDateTime.now());
    batchMapper.updateById(entity);
    log.info("FAQ 转换批次已暂停：batchId={}", batchId);
  }

  /** 暂停批次（带原因写入 error_message） */
  @Transactional
  public void pauseBatchWithReason(Long tenantId, Long batchId, String reason) {
    ExamFaqConversionBatchEntity entity = getBatchEntity(tenantId, batchId);
    if (!STATUS_RUNNING.equals(entity.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "批次状态不是运行中，无法暂停");
    }
    entity.setStatus(STATUS_PAUSED);
    entity.setErrorMessage(reason);
    entity.setUpdatedAt(LocalDateTime.now());
    batchMapper.updateById(entity);
    log.info("FAQ 转换批次已暂停（带原因）：batchId={}, reason={}", batchId, reason);
  }

  /** 恢复批次 */
  @Transactional
  public void resumeBatch(Long tenantId, Long batchId) {
    ExamFaqConversionBatchEntity entity = getBatchEntity(tenantId, batchId);
    if (!STATUS_PAUSED.equals(entity.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "批次状态不是暂停，无法恢复");
    }
    entity.setStatus(STATUS_RUNNING);
    entity.setUpdatedAt(LocalDateTime.now());
    batchMapper.updateById(entity);
    log.info("FAQ 转换批次已恢复：batchId={}", batchId);
  }

  /** 标记批次完成 */
  @Transactional
  public void completeBatch(Long tenantId, Long batchId) {
    ExamFaqConversionBatchEntity entity = getBatchEntity(tenantId, batchId);
    entity.setStatus(STATUS_COMPLETED);
    entity.setCompletedAt(LocalDateTime.now());
    entity.setPendingCount(0);
    entity.setUpdatedAt(entity.getCompletedAt());
    batchMapper.updateById(entity);
    log.info("FAQ 转换批次已完成：batchId={}", batchId);
  }

  /** 原子递增成功计数 */
  @Transactional
  public void incrementSuccess(Long batchId) {
    batchMapper.incrementSuccess(batchId);
  }

  /** 原子递增失败计数 */
  @Transactional
  public void incrementFailed(Long batchId) {
    batchMapper.incrementFailed(batchId);
  }

  /** 原子递增跳过计数 */
  @Transactional
  public void incrementSkipped(Long batchId) {
    batchMapper.incrementSkipped(batchId);
  }

  /** 原子递减待转换计数 */
  @Transactional
  public void decrementPending(Long batchId) {
    batchMapper.decrementPending(batchId);
  }

  /** 原子递增待转换计数（retryFailed 场景：failed → pending） */
  @Transactional
  public void incrementPending(Long batchId) {
    batchMapper.incrementPending(batchId);
  }

  /** 原子递减失败计数（retryFailed 场景：failed → pending） */
  @Transactional
  public void decrementFailed(Long batchId) {
    batchMapper.decrementFailed(batchId);
  }

  /** 检查批次是否为运行中 */
  @Transactional(readOnly = true)
  public boolean isRunning(Long tenantId, Long batchId) {
    ExamFaqConversionBatchEntity entity = batchMapper.selectById(batchId);
    return entity != null && entity.getTenantId().equals(tenantId)
        && STATUS_RUNNING.equals(entity.getStatus());
  }

}
