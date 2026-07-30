package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainBaseDataCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainBaseDataQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainBaseDataUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainBaseDataEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainBaseDataMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainBaseDataVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class TrainBaseDataApplicationService {

  private final TrainBaseDataMapper mapper;

  public TrainBaseDataApplicationService(TrainBaseDataMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainBaseDataVO> page(TrainBaseDataQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainBaseDataEntity> wrapper = new LambdaQueryWrapper<TrainBaseDataEntity>()
        .eq(TrainBaseDataEntity::getTenantId, tenantId)
        .eq(TrainBaseDataEntity::getDeleted, 0)
        .orderByDesc(TrainBaseDataEntity::getId);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.like(TrainBaseDataEntity::getId, req.getKeyword());
    }
    Page<TrainBaseDataEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainBaseDataVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TrainBaseDataVO create(TrainBaseDataCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainBaseDataEntity entity = new TrainBaseDataEntity();
    entity.setTenantId(tenantId);
    entity.setDataKey(req.getDataKey());
    entity.setDataValue(req.getDataValue());
    entity.setDataType(req.getDataType());
    entity.setDescription(req.getDescription());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainBaseDataVO update(Long id, TrainBaseDataUpdateReq req) {
    TrainBaseDataEntity entity = getExisting(id);
    if (StringUtils.hasText(req.getDataKey())) { entity.setDataKey(req.getDataKey()); }
    if (StringUtils.hasText(req.getDataValue())) { entity.setDataValue(req.getDataValue()); }
    if (StringUtils.hasText(req.getDataType())) { entity.setDataType(req.getDataType()); }
    if (StringUtils.hasText(req.getDescription())) { entity.setDescription(req.getDescription()); }
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainBaseDataEntity entity = getExisting(id);

    entity.setDeleted(1);
    mapper.updateById(entity);
  }

  private TrainBaseDataEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainBaseDataEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainBaseDataEntity>()

        .eq(TrainBaseDataEntity::getTenantId, tenantId)
        .eq(TrainBaseDataEntity::getId, id)
        .eq(TrainBaseDataEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private TrainBaseDataVO toVO(TrainBaseDataEntity e) {
    return new TrainBaseDataVO(
        e.getId(),
        e.getDataKey(),
        e.getDataValue(),
        e.getDataType(),
        e.getDescription(),
        e.getTenantId(),
        e.getCreateTime(),
        e.getUpdateTime()
    );
  }
}
