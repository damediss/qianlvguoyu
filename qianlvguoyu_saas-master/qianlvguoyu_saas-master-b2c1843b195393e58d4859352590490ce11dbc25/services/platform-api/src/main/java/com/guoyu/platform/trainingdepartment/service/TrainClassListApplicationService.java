package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.AssignClassReq;
import com.guoyu.platform.trainingdepartment.dto.TrainClassListCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainClassListQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainClassListUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainClassListEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainClassListMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainClassListVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 培训班级列表管理服务实现
 */
@Service
@Slf4j
public class TrainClassListApplicationService {

  private final TrainClassListMapper mapper;

  public TrainClassListApplicationService(TrainClassListMapper mapper) {
  this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainClassListVO> page(TrainClassListQueryReq req) {
  Long tenantId = SecurityContext.currentUser().getTenantId();
  LambdaQueryWrapper<TrainClassListEntity> wrapper = new LambdaQueryWrapper<TrainClassListEntity>()
    .eq(TrainClassListEntity::getTenantId, tenantId)
    .eq(TrainClassListEntity::getDeleted, 0)
    .orderByDesc(TrainClassListEntity::getCreateTime);
  if (req.getTrainId() != null) {
      wrapper.eq(TrainClassListEntity::getTrainId, req.getTrainId());
  }
  if (StringUtils.hasText(req.getKeyword())) {
      wrapper.like(TrainClassListEntity::getClassName, req.getKeyword());
  }
  Page<TrainClassListEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
  return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
    page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainClassListVO getById(Long id) {
  return toVO(getExisting(id));
  }

  @Transactional
  public TrainClassListVO create(TrainClassListCreateReq req) {
  Long tenantId = SecurityContext.currentUser().getTenantId();
  Long userId = SecurityContext.currentUser().getUserId();
  TrainClassListEntity entity = new TrainClassListEntity();
  entity.setTenantId(tenantId);
  entity.setUserId(userId);
  entity.setTrainId(req.getTrainId());
  entity.setIsLeader(req.getIsLeader());
  entity.setIsCommittee(req.getIsCommittee());
  entity.setClassName(req.getClassName());
  entity.setSortNumber(req.getSortNumber());
  entity.setDeleted(0);
  mapper.insert(entity);
  return toVO(entity);
  }

  @Transactional
  public TrainClassListVO update(Long id, TrainClassListUpdateReq req) {
  TrainClassListEntity entity = getExisting(id);
  entity.setIsLeader(req.getIsLeader());
  entity.setIsCommittee(req.getIsCommittee());
  entity.setClassName(req.getClassName());
  entity.setSortNumber(req.getSortNumber());
  mapper.updateById(entity);
  return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
  TrainClassListEntity entity = getExisting(id);
  entity.setDeleted(1);
  mapper.updateById(entity);
  }

  @Transactional
  public void batchAssign(AssignClassReq req) {
  Long tenantId = SecurityContext.currentUser().getTenantId();
  for (Long targetUserId : req.getUserIds()) {
      TrainClassListEntity entity = new TrainClassListEntity();
      entity.setTenantId(tenantId);
      entity.setUserId(targetUserId);
      entity.setTrainId(req.getClassId());
      entity.setClassName(null);
      entity.setIsLeader(0);
      entity.setIsCommittee(0);
      entity.setSortNumber(1);
      entity.setDeleted(0);
      mapper.insert(entity);
  }
  }

  @Transactional
  public TrainClassListVO openClass(Long id) {
  TrainClassListEntity entity = getExisting(id);
  if (entity.getStatus() != null && entity.getStatus() == 2) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "已结课的班级不能重新开班");
  }
  if (entity.getStatus() != null && entity.getStatus() == 1) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "班级已处于开班状态");
  }
  entity.setStatus(1);
  mapper.updateById(entity);
  return toVO(entity);
  }

  @Transactional
  public TrainClassListVO closeClass(Long id) {
  TrainClassListEntity entity = getExisting(id);
  if (entity.getStatus() == null || entity.getStatus() != 1) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "只有开班状态的班级才能结课");
  }
  entity.setStatus(2);
  mapper.updateById(entity);
  return toVO(entity);
  }

  /**
   * 根据ID获取现有实体（未删除），不存在则抛出业务异常
   */
  private TrainClassListEntity getExisting(Long id) {
  Long tenantId = SecurityContext.currentUser().getTenantId();
  TrainClassListEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainClassListEntity>()
    .eq(TrainClassListEntity::getTenantId, tenantId)
    .eq(TrainClassListEntity::getId, id)
    .eq(TrainClassListEntity::getDeleted, 0));
  if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
  }
  return entity;
  }

  /**
   * 将实体转换为VO
   */
  private TrainClassListVO toVO(TrainClassListEntity entity) {
  return new TrainClassListVO(
    entity.getId(),
    entity.getTrainId(),
    entity.getUserId(),
    entity.getIsLeader(),
    entity.getIsCommittee(),
    entity.getClassName(),
    entity.getSortNumber(),
    entity.getStatus(),
    entity.getTenantId(),
    entity.getCreateTime()
  );
  }
}
