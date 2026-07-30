package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainPositionToWorkTypeCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainPositionToWorkTypeQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainPositionToWorkTypeUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainPositionToWorkTypeEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainPositionToWorkTypeMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainPositionToWorkTypeVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class TrainPositionToWorkTypeApplicationService {

  private final TrainPositionToWorkTypeMapper mapper;

  public TrainPositionToWorkTypeApplicationService(TrainPositionToWorkTypeMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainPositionToWorkTypeVO> page(TrainPositionToWorkTypeQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainPositionToWorkTypeEntity> wrapper = new LambdaQueryWrapper<TrainPositionToWorkTypeEntity>()
        .eq(TrainPositionToWorkTypeEntity::getTenantId, tenantId)
        .eq(TrainPositionToWorkTypeEntity::getDeleted, 0)
        .orderByDesc(TrainPositionToWorkTypeEntity::getId);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.like(TrainPositionToWorkTypeEntity::getId, req.getKeyword());
    }
    Page<TrainPositionToWorkTypeEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainPositionToWorkTypeVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TrainPositionToWorkTypeVO create(TrainPositionToWorkTypeCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainPositionToWorkTypeEntity entity = new TrainPositionToWorkTypeEntity();
    entity.setTenantId(tenantId);
    entity.setPositionName(req.getPositionName());
    entity.setWorkTypeId(req.getWorkTypeId());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainPositionToWorkTypeVO update(Long id, TrainPositionToWorkTypeUpdateReq req) {
    TrainPositionToWorkTypeEntity entity = getExisting(id);
    if (StringUtils.hasText(req.getPositionName())) { entity.setPositionName(req.getPositionName()); }
    if (req.getWorkTypeId() != null) { entity.setWorkTypeId(req.getWorkTypeId()); }
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainPositionToWorkTypeEntity entity = getExisting(id);

    entity.setDeleted(1);
    mapper.updateById(entity);
  }

  private TrainPositionToWorkTypeEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainPositionToWorkTypeEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainPositionToWorkTypeEntity>()

        .eq(TrainPositionToWorkTypeEntity::getTenantId, tenantId)
        .eq(TrainPositionToWorkTypeEntity::getId, id)
        .eq(TrainPositionToWorkTypeEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private TrainPositionToWorkTypeVO toVO(TrainPositionToWorkTypeEntity e) {
    return new TrainPositionToWorkTypeVO(
        e.getId(),
        e.getPositionName(),
        e.getWorkTypeId(),
        e.getTenantId(),
        e.getCreateTime(),
        e.getUpdateTime()
    );
  }
}
