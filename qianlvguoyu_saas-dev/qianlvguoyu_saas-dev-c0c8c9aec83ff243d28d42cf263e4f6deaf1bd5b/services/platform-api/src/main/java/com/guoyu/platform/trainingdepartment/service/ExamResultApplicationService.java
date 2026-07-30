package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.ExamResultCreateReq;
import com.guoyu.platform.trainingdepartment.dto.ExamResultQueryReq;
import com.guoyu.platform.trainingdepartment.dto.ExamResultUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.ExamResultEntity;
import com.guoyu.platform.trainingdepartment.mapper.ExamResultMapper;
import com.guoyu.platform.trainingdepartment.vo.ExamResultVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 考试结果管理服务实现
 */
@Service
@Slf4j
public class ExamResultApplicationService {

  private final ExamResultMapper mapper;

  public ExamResultApplicationService(ExamResultMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<ExamResultVO> page(ExamResultQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<ExamResultEntity> wrapper = new LambdaQueryWrapper<ExamResultEntity>()
        .eq(ExamResultEntity::getTenantId, tenantId)
        .eq(ExamResultEntity::getDeleted, 0)
        .orderByDesc(ExamResultEntity::getCreateTime);
    if (req.getExamPlanId() != null) {
      wrapper.eq(ExamResultEntity::getExamPlanId, req.getExamPlanId());
    }
    if (req.getPaperId() != null) {
      wrapper.eq(ExamResultEntity::getPaperId, req.getPaperId());
    }
    if (req.getExamType() != null) {
      wrapper.eq(ExamResultEntity::getExamType, req.getExamType());
    }
    if (req.getUserId() != null) {
      wrapper.eq(ExamResultEntity::getUserId, req.getUserId());
    }
    if (req.getPlatformUserId() != null) {
      wrapper.eq(ExamResultEntity::getPlatformUserId, req.getPlatformUserId());
    }
    Page<ExamResultEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public ExamResultVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public ExamResultVO create(ExamResultCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    ExamResultEntity entity = new ExamResultEntity();
    entity.setTenantId(tenantId);
    entity.setUserId(userId);
    entity.setType(req.getType());
    entity.setExamPlanId(req.getExamPlanId());
    entity.setPaperId(req.getPaperId());
    entity.setDetailId(req.getDetailId());
    entity.setGetScore(req.getGetScore());
    entity.setExamType(req.getExamType());
    entity.setExamId(req.getExamId());
    entity.setPlatformUserId(req.getPlatformUserId());
    entity.setUserAnswer(req.getUserAnswer());
    entity.setSortNumber(req.getSortNumber());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public ExamResultVO update(Long id, ExamResultUpdateReq req) {
    ExamResultEntity entity = getExisting(id);
    entity.setType(req.getType());
    entity.setGetScore(req.getGetScore());
    entity.setExamType(req.getExamType());
    entity.setUserAnswer(req.getUserAnswer());
    entity.setSortNumber(req.getSortNumber());
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    ExamResultEntity entity = getExisting(id);
    entity.setDeleted(1);
    mapper.updateById(entity);
  }

  private ExamResultEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    ExamResultEntity entity = mapper.selectOne(new LambdaQueryWrapper<ExamResultEntity>()
        .eq(ExamResultEntity::getTenantId, tenantId)
        .eq(ExamResultEntity::getId, id)
        .eq(ExamResultEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private ExamResultVO toVO(ExamResultEntity entity) {
    return new ExamResultVO(
        entity.getId(),
        entity.getType(),
        entity.getExamPlanId(),
        entity.getPaperId(),
        entity.getDetailId(),
        entity.getGetScore(),
        entity.getExamType(),
        entity.getExamId(),
        entity.getUserId(),
        entity.getPlatformUserId(),
        entity.getUserAnswer(),
        entity.getSortNumber(),
        entity.getTenantId(),
        entity.getCreateTime()
    );
  }
}
