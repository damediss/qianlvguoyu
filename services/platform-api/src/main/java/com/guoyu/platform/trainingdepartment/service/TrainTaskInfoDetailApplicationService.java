package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskInfoDetailCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskInfoDetailQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskInfoDetailUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainTaskInfoDetailEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainTaskInfoDetailMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainTaskInfoDetailVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 培训任务明细管理服务实现
 */
@Service
@Slf4j
public class TrainTaskInfoDetailApplicationService {

  private final TrainTaskInfoDetailMapper mapper;

  public TrainTaskInfoDetailApplicationService(TrainTaskInfoDetailMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainTaskInfoDetailVO> page(TrainTaskInfoDetailQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainTaskInfoDetailEntity> wrapper = new LambdaQueryWrapper<TrainTaskInfoDetailEntity>()
        .eq(TrainTaskInfoDetailEntity::getTenantId, tenantId)
        .eq(TrainTaskInfoDetailEntity::getDeleted, 0)
        .orderByDesc(TrainTaskInfoDetailEntity::getCreateTime);
    if (req.getTrainId() != null) {
      wrapper.eq(TrainTaskInfoDetailEntity::getTrainId, req.getTrainId());
    }
    if (req.getTaskListId() != null) {
      wrapper.eq(TrainTaskInfoDetailEntity::getTaskListId, req.getTaskListId());
    }
    Page<TrainTaskInfoDetailEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainTaskInfoDetailVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TrainTaskInfoDetailVO create(TrainTaskInfoDetailCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    TrainTaskInfoDetailEntity entity = new TrainTaskInfoDetailEntity();
    entity.setTenantId(tenantId);
    entity.setUserId(userId);
    entity.setTrainId(req.getTrainId());
    entity.setTaskListId(req.getTaskListId());
    entity.setWorkTypeId(req.getWorkTypeId());
    entity.setCourseId(req.getCourseId());
    entity.setKnowledgeId(req.getKnowledgeId());
    entity.setTitle(req.getTitle());
    entity.setType(req.getType());
    entity.setContent(req.getContent());
    entity.setGetLearnTime(req.getGetLearnTime());
    entity.setMinMinute(req.getMinMinute());
    entity.setIsRequired(req.getIsRequired());
    entity.setSortNumber(req.getSortNumber());
    entity.setScore(req.getScore());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainTaskInfoDetailVO update(Long id, TrainTaskInfoDetailUpdateReq req) {
    TrainTaskInfoDetailEntity entity = getExisting(id);
    if (req.getTitle() != null) {
      entity.setTitle(req.getTitle());
    }
    if (req.getType() != null) {
      entity.setType(req.getType());
    }
    if (req.getContent() != null) {
      entity.setContent(req.getContent());
    }
    if (req.getGetLearnTime() != null) {
      entity.setGetLearnTime(req.getGetLearnTime());
    }
    if (req.getMinMinute() != null) {
      entity.setMinMinute(req.getMinMinute());
    }
    if (req.getIsRequired() != null) {
      entity.setIsRequired(req.getIsRequired());
    }
    if (req.getSortNumber() != null) {
      entity.setSortNumber(req.getSortNumber());
    }
    if (req.getScore() != null) {
      entity.setScore(req.getScore());
    }
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainTaskInfoDetailEntity entity = getExisting(id);
    entity.setDeleted(1);
    mapper.deleteById(entity.getId());
  }

  private TrainTaskInfoDetailEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainTaskInfoDetailEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainTaskInfoDetailEntity>()
        .eq(TrainTaskInfoDetailEntity::getTenantId, tenantId)
        .eq(TrainTaskInfoDetailEntity::getId, id)
        .eq(TrainTaskInfoDetailEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private TrainTaskInfoDetailVO toVO(TrainTaskInfoDetailEntity entity) {
    return new TrainTaskInfoDetailVO(
        entity.getId(),
        entity.getTrainId(),
        entity.getTaskListId(),
        entity.getWorkTypeId(),
        entity.getCourseId(),
        entity.getKnowledgeId(),
        entity.getTitle(),
        entity.getType(),
        entity.getContent(),
        entity.getGetLearnTime(),
        entity.getMinMinute(),
        entity.getIsRequired(),
        entity.getUserId(),
        entity.getSortNumber(),
        entity.getTenantId(),
        entity.getCreateTime(),
        entity.getScore()
    );
  }
}
