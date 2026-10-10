package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainFileDetailCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainFileDetailQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainFileDetailUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainFileDetailEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainFileDetailMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainFileDetailVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 文件详情管理服务实现
 */
@Service
@Slf4j
public class TrainFileDetailApplicationService {

  private final TrainFileDetailMapper mapper;

  public TrainFileDetailApplicationService(TrainFileDetailMapper mapper) {
  this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainFileDetailVO> page(TrainFileDetailQueryReq req) {
  Long tenantId = SecurityContext.currentUser().getTenantId();
  LambdaQueryWrapper<TrainFileDetailEntity> wrapper = new LambdaQueryWrapper<TrainFileDetailEntity>()
    .eq(TrainFileDetailEntity::getTenantId, tenantId)
    .orderByDesc(TrainFileDetailEntity::getCreateTime);
  if (req.getFileId() != null) {
      wrapper.eq(TrainFileDetailEntity::getFileId, req.getFileId());
  }
  Page<TrainFileDetailEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
  return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
    page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainFileDetailVO getById(Long id) {
  return toVO(getExisting(id));
  }

  @Transactional
  public TrainFileDetailVO create(TrainFileDetailCreateReq req) {
  Long tenantId = SecurityContext.currentUser().getTenantId();
  Long userId = SecurityContext.currentUser().getUserId();
  TrainFileDetailEntity entity = new TrainFileDetailEntity();
  entity.setTenantId(tenantId);
  entity.setUserId(userId);
  entity.setFileId(req.getFileId());
  entity.setTitle(req.getTitle());
  entity.setPath(req.getPath());
  entity.setStatus(req.getStatus());
  entity.setSortNumber(req.getSortNumber());
  mapper.insert(entity);
  return toVO(entity);
  }

  @Transactional
  public TrainFileDetailVO update(Long id, TrainFileDetailUpdateReq req) {
  TrainFileDetailEntity entity = getExisting(id);
  entity.setTitle(req.getTitle());
  entity.setPath(req.getPath());
  entity.setStatus(req.getStatus());
  entity.setSortNumber(req.getSortNumber());
  mapper.updateById(entity);
  return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
  mapper.deleteById(id);
  }

  /**
   * 根据ID获取现有实体，不存在则抛出业务异常
   */
  private TrainFileDetailEntity getExisting(Long id) {
  Long tenantId = SecurityContext.currentUser().getTenantId();
  TrainFileDetailEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainFileDetailEntity>()
    .eq(TrainFileDetailEntity::getTenantId, tenantId)
    .eq(TrainFileDetailEntity::getId, id));
  if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
  }
  return entity;
  }

  /**
   * 将实体转换为VO
   */
  private TrainFileDetailVO toVO(TrainFileDetailEntity entity) {
  return new TrainFileDetailVO(
    entity.getId(),
    entity.getFileId(),
    entity.getTitle(),
    entity.getPath(),
    entity.getUserId(),
    entity.getStatus(),
    entity.getSortNumber(),
    entity.getTenantId(),
    entity.getCreateTime()
  );
  }
}
