package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainUserCoursePaperCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserCoursePaperQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserCoursePaperUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainUserCoursePaperEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainUserCoursePaperMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainUserCoursePaperVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户课程论文管理服务实现
 */
@Service
@Slf4j
public class TrainUserCoursePaperApplicationService {

  private final TrainUserCoursePaperMapper mapper;

  public TrainUserCoursePaperApplicationService(TrainUserCoursePaperMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainUserCoursePaperVO> page(TrainUserCoursePaperQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainUserCoursePaperEntity> wrapper = new LambdaQueryWrapper<TrainUserCoursePaperEntity>()
        .eq(TrainUserCoursePaperEntity::getTenantId, tenantId)
        .eq(TrainUserCoursePaperEntity::getDeleted, 0)
        .orderByDesc(TrainUserCoursePaperEntity::getCreateTime);
    if (req.getUserId() != null) {
      wrapper.eq(TrainUserCoursePaperEntity::getUserId, req.getUserId());
    }
    if (req.getCourseId() != null) {
      wrapper.eq(TrainUserCoursePaperEntity::getCourseId, req.getCourseId());
    }
    if (req.getWorkTypeId() != null) {
      wrapper.eq(TrainUserCoursePaperEntity::getWorkTypeId, req.getWorkTypeId());
    }
    Page<TrainUserCoursePaperEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainUserCoursePaperVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TrainUserCoursePaperVO create(TrainUserCoursePaperCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    TrainUserCoursePaperEntity entity = new TrainUserCoursePaperEntity();
    entity.setTenantId(tenantId);
    entity.setUserId(userId);
    entity.setPlatformUserId(req.getPlatformUserId());
    entity.setCourseId(req.getCourseId());
    entity.setWorkTypeId(req.getWorkTypeId());
    entity.setGetYear(req.getGetYear());
    entity.setStatus(req.getStatus());
    entity.setSortNumber(req.getSortNumber());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainUserCoursePaperVO update(Long id, TrainUserCoursePaperUpdateReq req) {
    TrainUserCoursePaperEntity entity = getExisting(id);
    entity.setWorkTypeId(req.getWorkTypeId());
    entity.setGetYear(req.getGetYear());
    entity.setStatus(req.getStatus());
    entity.setSortNumber(req.getSortNumber());
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainUserCoursePaperEntity entity = getExisting(id);
    entity.setDeleted(1);
    mapper.deleteById(entity.getId());
  }

  private TrainUserCoursePaperEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainUserCoursePaperEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainUserCoursePaperEntity>()
        .eq(TrainUserCoursePaperEntity::getTenantId, tenantId)
        .eq(TrainUserCoursePaperEntity::getId, id)
        .eq(TrainUserCoursePaperEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private TrainUserCoursePaperVO toVO(TrainUserCoursePaperEntity entity) {
    return new TrainUserCoursePaperVO(
        entity.getId(),
        entity.getUserId(),
        entity.getPlatformUserId(),
        entity.getCourseId(),
        entity.getWorkTypeId(),
        entity.getGetYear(),
        entity.getStatus(),
        entity.getSortNumber(),
        entity.getTenantId(),
        entity.getCreateTime()
    );
  }
}
