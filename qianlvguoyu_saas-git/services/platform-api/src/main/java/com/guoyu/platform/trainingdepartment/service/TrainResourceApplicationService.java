package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainResourceCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainResourceQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainResourceUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainResourceEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainResourceMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainResourceVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 培训资源管理服务实现
 */
@Service
@Slf4j
public class TrainResourceApplicationService {

  private final TrainResourceMapper mapper;

  public TrainResourceApplicationService(TrainResourceMapper mapper) {
  this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainResourceVO> page(TrainResourceQueryReq req) {
  Long tenantId = SecurityContext.currentUser().getTenantId();
  LambdaQueryWrapper<TrainResourceEntity> wrapper = new LambdaQueryWrapper<TrainResourceEntity>()
    .eq(TrainResourceEntity::getTenantId, tenantId)
    .eq(TrainResourceEntity::getDeleted, 0)
    .orderByDesc(TrainResourceEntity::getCreateTime);
  if (StringUtils.hasText(req.getKeyword())) {
      wrapper.like(TrainResourceEntity::getTitle, req.getKeyword());
  }
  if (StringUtils.hasText(req.getType())) {
      wrapper.eq(TrainResourceEntity::getType, req.getType());
  }
  if (req.getWorkTypeId() != null) {
      wrapper.eq(TrainResourceEntity::getWorkTypeId, req.getWorkTypeId());
  }
  Page<TrainResourceEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
  return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
    page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainResourceVO getById(Long id) {
  return toVO(getExisting(id));
  }

  @Transactional
  public TrainResourceVO create(TrainResourceCreateReq req) {
  Long tenantId = SecurityContext.currentUser().getTenantId();
  Long userId = SecurityContext.currentUser().getUserId();
  TrainResourceEntity entity = new TrainResourceEntity();
  entity.setTenantId(tenantId);
  entity.setUserId(userId);
  entity.setWorkTypeId(req.getWorkTypeId());
  entity.setTitle(req.getTitle());
  entity.setType(req.getType());
  entity.setPath(req.getPath());
  entity.setSortNumber(req.getSortNumber());
  entity.setDeleted(0);
  mapper.insert(entity);
  return toVO(entity);
  }

  @Transactional
  public TrainResourceVO update(Long id, TrainResourceUpdateReq req) {
  TrainResourceEntity entity = getExisting(id);
  entity.setTitle(req.getTitle());
  entity.setType(req.getType());
  entity.setPath(req.getPath());
  entity.setSortNumber(req.getSortNumber());
  mapper.updateById(entity);
  return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
  TrainResourceEntity entity = getExisting(id);
  entity.setDeleted(1);
  mapper.deleteById(entity.getId());
  }

  /**
   * 根据ID获取现有实体（未删除），不存在则抛出业务异常
   */
  private TrainResourceEntity getExisting(Long id) {
  Long tenantId = SecurityContext.currentUser().getTenantId();
  TrainResourceEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainResourceEntity>()
    .eq(TrainResourceEntity::getTenantId, tenantId)
    .eq(TrainResourceEntity::getId, id)
    .eq(TrainResourceEntity::getDeleted, 0));
  if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
  }
  return entity;
  }

  /**
   * 将实体转换为VO
   */
  private TrainResourceVO toVO(TrainResourceEntity entity) {
  return new TrainResourceVO(
    entity.getId(),
    entity.getWorkTypeId(),
    entity.getTitle(),
    entity.getType(),
    entity.getPath(),
    entity.getUserId(),
    entity.getSortNumber(),
    entity.getTenantId(),
    entity.getCreateTime()
  );
  }
}
