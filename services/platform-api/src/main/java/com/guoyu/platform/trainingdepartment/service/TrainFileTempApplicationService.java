package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainFileTempCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainFileTempQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainFileTempUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainFileTempEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainFileTempMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainFileTempVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class TrainFileTempApplicationService {

  private final TrainFileTempMapper mapper;

  public TrainFileTempApplicationService(TrainFileTempMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainFileTempVO> page(TrainFileTempQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainFileTempEntity> wrapper = new LambdaQueryWrapper<TrainFileTempEntity>()
        .eq(TrainFileTempEntity::getTenantId, tenantId)
        .eq(TrainFileTempEntity::getDeleted, 0)
        .orderByDesc(TrainFileTempEntity::getId);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.like(TrainFileTempEntity::getId, req.getKeyword());
    }
    Page<TrainFileTempEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainFileTempVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TrainFileTempVO create(TrainFileTempCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainFileTempEntity entity = new TrainFileTempEntity();
    entity.setTenantId(tenantId);
    entity.setType(req.getType());
    entity.setTitle(req.getTitle());
    entity.setCity(req.getCity());
    entity.setPathSource(req.getPathSource());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainFileTempVO update(Long id, TrainFileTempUpdateReq req) {
    TrainFileTempEntity entity = getExisting(id);
    if (StringUtils.hasText(req.getType())) { entity.setType(req.getType()); }
    if (StringUtils.hasText(req.getTitle())) { entity.setTitle(req.getTitle()); }
    if (StringUtils.hasText(req.getCity())) { entity.setCity(req.getCity()); }
    if (StringUtils.hasText(req.getPathSource())) { entity.setPathSource(req.getPathSource()); }
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainFileTempEntity entity = getExisting(id);

    entity.setDeleted(1);
    mapper.updateById(entity);
  }

  private TrainFileTempEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainFileTempEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainFileTempEntity>()

        .eq(TrainFileTempEntity::getTenantId, tenantId)
        .eq(TrainFileTempEntity::getId, id)
        .eq(TrainFileTempEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private TrainFileTempVO toVO(TrainFileTempEntity e) {
    return new TrainFileTempVO(
        e.getId(),
        e.getType(),
        e.getTitle(),
        e.getCity(),
        e.getPathSource(),
        e.getTenantId(),
        e.getCreateTime(),
        e.getUpdateTime()
    );
  }
}
