package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskInfoCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskInfoQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskInfoUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainTaskInfoEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainTaskInfoMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainTaskInfoVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 培训任务信息管理服务实现
 */
@Service
@Slf4j
public class TrainTaskInfoApplicationService {

  private final TrainTaskInfoMapper mapper;

  public TrainTaskInfoApplicationService(TrainTaskInfoMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainTaskInfoVO> page(TrainTaskInfoQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainTaskInfoEntity> wrapper = new LambdaQueryWrapper<TrainTaskInfoEntity>()
        .eq(TrainTaskInfoEntity::getTenantId, tenantId)
        .eq(TrainTaskInfoEntity::getDeleted, 0)
        .orderByDesc(TrainTaskInfoEntity::getCreateTime);
    if (req.getTrainId() != null) {
      wrapper.eq(TrainTaskInfoEntity::getTrainId, req.getTrainId());
    }
    if (req.getCourseId() != null) {
      wrapper.eq(TrainTaskInfoEntity::getCourseId, req.getCourseId());
    }
    Page<TrainTaskInfoEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainTaskInfoVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TrainTaskInfoVO create(TrainTaskInfoCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    TrainTaskInfoEntity entity = new TrainTaskInfoEntity();
    entity.setTenantId(tenantId);
    entity.setUserId(userId);
    entity.setTrainId(req.getTrainId());
    entity.setCover(req.getCover());
    entity.setCourseId(req.getCourseId());
    entity.setTrainTaskId(req.getTrainTaskId());
    entity.setKnowledgeId(req.getKnowledgeId());
    entity.setIntro(req.getIntro());
    entity.setIsRequired(req.getIsRequired());
    entity.setScore(req.getScore());
    entity.setSortNumber(req.getSortNumber());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainTaskInfoVO update(Long id, TrainTaskInfoUpdateReq req) {
    TrainTaskInfoEntity entity = getExisting(id);
    if (req.getCover() != null) {
      entity.setCover(req.getCover());
    }
    if (req.getIntro() != null) {
      entity.setIntro(req.getIntro());
    }
    if (req.getIsRequired() != null) {
      entity.setIsRequired(req.getIsRequired());
    }
    if (req.getScore() != null) {
      entity.setScore(req.getScore());
    }
    if (req.getSortNumber() != null) {
      entity.setSortNumber(req.getSortNumber());
    }
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainTaskInfoEntity entity = getExisting(id);
    entity.setDeleted(1);
    mapper.updateById(entity);
  }

  private TrainTaskInfoEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainTaskInfoEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainTaskInfoEntity>()
        .eq(TrainTaskInfoEntity::getTenantId, tenantId)
        .eq(TrainTaskInfoEntity::getId, id)
        .eq(TrainTaskInfoEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private TrainTaskInfoVO toVO(TrainTaskInfoEntity entity) {
    return new TrainTaskInfoVO(
        entity.getId(),
        entity.getTrainId(),
        entity.getCover(),
        entity.getCourseId(),
        entity.getTrainTaskId(),
        entity.getKnowledgeId(),
        entity.getIntro(),
        entity.getIsRequired(),
        entity.getScore(),
        entity.getUserId(),
        entity.getSortNumber(),
        entity.getTenantId(),
        entity.getCreateTime()
    );
  }
}
