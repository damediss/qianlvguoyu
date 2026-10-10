package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainUserLearnLogCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserLearnLogQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserLearnLogUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainUserLearnLogEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainUserLearnLogMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainUserLearnLogVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户学习日志管理服务实现
 */
@Service
@Slf4j
public class TrainUserLearnLogApplicationService {

  private final TrainUserLearnLogMapper mapper;

  public TrainUserLearnLogApplicationService(TrainUserLearnLogMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainUserLearnLogVO> page(TrainUserLearnLogQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainUserLearnLogEntity> wrapper = new LambdaQueryWrapper<TrainUserLearnLogEntity>()
        .eq(TrainUserLearnLogEntity::getTenantId, tenantId)
        .eq(TrainUserLearnLogEntity::getDeleted, 0)
        .orderByDesc(TrainUserLearnLogEntity::getCreateTime);
    if (req.getTrainId() != null) {
      wrapper.eq(TrainUserLearnLogEntity::getTrainId, req.getTrainId());
    }
    if (req.getUserId() != null) {
      wrapper.eq(TrainUserLearnLogEntity::getUserId, req.getUserId());
    }
    Page<TrainUserLearnLogEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainUserLearnLogVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TrainUserLearnLogVO create(TrainUserLearnLogCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    TrainUserLearnLogEntity entity = new TrainUserLearnLogEntity();
    entity.setTenantId(tenantId);
    entity.setUserId(userId);
    entity.setTrainId(req.getTrainId());
    entity.setMatchId(req.getMatchId());
    entity.setTrainTaskId(req.getTrainTaskId());
    entity.setLearnTime(req.getLearnTime());
    entity.setScore(req.getScore());
    entity.setSortNumber(req.getSortNumber());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainUserLearnLogVO update(Long id, TrainUserLearnLogUpdateReq req) {
    TrainUserLearnLogEntity entity = getExisting(id);
    entity.setLearnTime(req.getLearnTime());
    entity.setScore(req.getScore());
    entity.setSortNumber(req.getSortNumber());
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainUserLearnLogEntity entity = getExisting(id);
    entity.setDeleted(1);
    mapper.deleteById(entity.getId());
  }

  private TrainUserLearnLogEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainUserLearnLogEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainUserLearnLogEntity>()
        .eq(TrainUserLearnLogEntity::getTenantId, tenantId)
        .eq(TrainUserLearnLogEntity::getId, id)
        .eq(TrainUserLearnLogEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private TrainUserLearnLogVO toVO(TrainUserLearnLogEntity entity) {
    return new TrainUserLearnLogVO(
        entity.getId(),
        entity.getTrainId(),
        entity.getMatchId(),
        entity.getTrainTaskId(),
        entity.getLearnTime(),
        entity.getScore(),
        entity.getUserId(),
        entity.getSortNumber(),
        entity.getTenantId(),
        entity.getCreateTime()
    );
  }
}
