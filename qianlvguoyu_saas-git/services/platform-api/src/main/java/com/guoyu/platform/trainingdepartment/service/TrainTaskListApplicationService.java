package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskListCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskListQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskListUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainTaskListEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainTaskListMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainTaskListVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 培训任务列表管理服务实现
 */
@Service
@Slf4j
public class TrainTaskListApplicationService {

  private final TrainTaskListMapper mapper;

  public TrainTaskListApplicationService(TrainTaskListMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainTaskListVO> page(TrainTaskListQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainTaskListEntity> wrapper = new LambdaQueryWrapper<TrainTaskListEntity>()
        .eq(TrainTaskListEntity::getTenantId, tenantId)
        .eq(TrainTaskListEntity::getDeleted, 0)
        .orderByDesc(TrainTaskListEntity::getCreateTime);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.like(TrainTaskListEntity::getTitle, req.getKeyword());
    }
    if (req.getWorkTypeId() != null) {
      wrapper.eq(TrainTaskListEntity::getWorkTypeId, req.getWorkTypeId());
    }
    if (req.getCourseId() != null) {
      wrapper.eq(TrainTaskListEntity::getCourseId, req.getCourseId());
    }
    if (req.getKnowledgeId() != null) {
      wrapper.eq(TrainTaskListEntity::getKnowledgeId, req.getKnowledgeId());
    }
    if (StringUtils.hasText(req.getType())) {
      wrapper.eq(TrainTaskListEntity::getType, req.getType());
    }
    if (req.getCheckStatus() != null) {
      wrapper.eq(TrainTaskListEntity::getCheckStatus, req.getCheckStatus());
    }
    Page<TrainTaskListEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainTaskListVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TrainTaskListVO create(TrainTaskListCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    TrainTaskListEntity entity = new TrainTaskListEntity();
    entity.setTenantId(tenantId);
    entity.setUserId(userId);
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
    entity.setGetPoint(req.getGetPoint());
    entity.setCheckStatus(req.getCheckStatus());
    entity.setFinalCheckStatus(req.getFinalCheckStatus());
    entity.setCheckTime(req.getCheckTime());
    entity.setCheckUserId(req.getCheckUserId());
    entity.setCheckUsername(req.getCheckUsername());
    entity.setFinalCheckTime(req.getFinalCheckTime());
    entity.setFinalCheckUserId(req.getFinalCheckUserId());
    entity.setFinalCheckUsername(req.getFinalCheckUsername());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainTaskListVO update(Long id, TrainTaskListUpdateReq req) {
    TrainTaskListEntity entity = getExisting(id);
    if (req.getWorkTypeId() != null) {
      entity.setWorkTypeId(req.getWorkTypeId());
    }
    if (req.getCourseId() != null) {
      entity.setCourseId(req.getCourseId());
    }
    if (req.getKnowledgeId() != null) {
      entity.setKnowledgeId(req.getKnowledgeId());
    }
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
    if (req.getGetPoint() != null) {
      entity.setGetPoint(req.getGetPoint());
    }
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainTaskListEntity entity = getExisting(id);
    entity.setDeleted(1);
    mapper.deleteById(entity.getId());
  }

  private TrainTaskListEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainTaskListEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainTaskListEntity>()
        .eq(TrainTaskListEntity::getTenantId, tenantId)
        .eq(TrainTaskListEntity::getId, id)
        .eq(TrainTaskListEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private TrainTaskListVO toVO(TrainTaskListEntity entity) {
    return new TrainTaskListVO(
        entity.getId(),
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
        entity.getScore(),
        entity.getGetPoint(),
        entity.getCheckStatus(),
        entity.getFinalCheckStatus(),
        entity.getCheckTime(),
        entity.getCheckUserId(),
        entity.getCheckUsername(),
        entity.getFinalCheckTime(),
        entity.getFinalCheckUserId(),
        entity.getFinalCheckUsername()
    );
  }
}
