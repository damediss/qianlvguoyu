package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainUserCoursePlanCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserCoursePlanQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserCoursePlanUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainUserCoursePlanEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainUserCoursePlanMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainUserCoursePlanVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户课程计划管理服务实现
 */
@Service
@Slf4j
public class TrainUserCoursePlanApplicationService {

  private final TrainUserCoursePlanMapper mapper;

  public TrainUserCoursePlanApplicationService(TrainUserCoursePlanMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainUserCoursePlanVO> page(TrainUserCoursePlanQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainUserCoursePlanEntity> wrapper = new LambdaQueryWrapper<TrainUserCoursePlanEntity>()
        .eq(TrainUserCoursePlanEntity::getTenantId, tenantId)
        .eq(TrainUserCoursePlanEntity::getDeleted, 0)
        .orderByDesc(TrainUserCoursePlanEntity::getCreateTime);
    if (req.getUserId() != null) {
      wrapper.eq(TrainUserCoursePlanEntity::getUserId, req.getUserId());
    }
    if (req.getCourseId() != null) {
      wrapper.eq(TrainUserCoursePlanEntity::getCourseId, req.getCourseId());
    }
    if (req.getStatus() != null) {
      wrapper.eq(TrainUserCoursePlanEntity::getStatus, req.getStatus());
    }
    Page<TrainUserCoursePlanEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainUserCoursePlanVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TrainUserCoursePlanVO create(TrainUserCoursePlanCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    TrainUserCoursePlanEntity entity = new TrainUserCoursePlanEntity();
    entity.setTenantId(tenantId);
    entity.setUserId(userId);
    entity.setPlatformUserId(req.getPlatformUserId());
    entity.setRealname(req.getRealname());
    entity.setAddFrom(req.getAddFrom());
    entity.setCourseId(req.getCourseId());
    entity.setDate(req.getDate());
    entity.setType(req.getType());
    entity.setStatus(req.getStatus());
    entity.setSortNumber(req.getSortNumber());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainUserCoursePlanVO update(Long id, TrainUserCoursePlanUpdateReq req) {
    TrainUserCoursePlanEntity entity = getExisting(id);
    entity.setRealname(req.getRealname());
    entity.setDate(req.getDate());
    entity.setType(req.getType());
    entity.setStatus(req.getStatus());
    entity.setSortNumber(req.getSortNumber());
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainUserCoursePlanEntity entity = getExisting(id);
    entity.setDeleted(1);
    mapper.updateById(entity);
  }

  private TrainUserCoursePlanEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainUserCoursePlanEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainUserCoursePlanEntity>()
        .eq(TrainUserCoursePlanEntity::getTenantId, tenantId)
        .eq(TrainUserCoursePlanEntity::getId, id)
        .eq(TrainUserCoursePlanEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private TrainUserCoursePlanVO toVO(TrainUserCoursePlanEntity entity) {
    return new TrainUserCoursePlanVO(
        entity.getId(),
        entity.getUserId(),
        entity.getPlatformUserId(),
        entity.getRealname(),
        entity.getAddFrom(),
        entity.getCourseId(),
        entity.getDate(),
        entity.getType(),
        entity.getStatus(),
        entity.getSortNumber(),
        entity.getTenantId(),
        entity.getCreateTime()
    );
  }
}
