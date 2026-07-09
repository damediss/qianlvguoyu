package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainResearchOrgCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainResearchOrgQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainResearchOrgUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainResearchOrgEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainResearchOrgMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainResearchOrgVO;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TrainResearchOrgApplicationService {

  private final TrainResearchOrgMapper trainResearchOrgMapper;

  public TrainResearchOrgApplicationService(TrainResearchOrgMapper trainResearchOrgMapper) {
    this.trainResearchOrgMapper = trainResearchOrgMapper;
  }

  public PageResponse<TrainResearchOrgVO> page(TrainResearchOrgQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainResearchOrgEntity> wrapper = new LambdaQueryWrapper<TrainResearchOrgEntity>()
        .eq(TrainResearchOrgEntity::getTenantId, tenantId)
        .eq(TrainResearchOrgEntity::getStatus, req.status() != null ? req.status() : 1)
        .orderByDesc(TrainResearchOrgEntity::getCreateTime);
    if (StringUtils.hasText(req.title())) {
      wrapper.like(TrainResearchOrgEntity::getTitle, req.title());
    }
    if (StringUtils.hasText(req.type())) {
      wrapper.eq(TrainResearchOrgEntity::getType, req.type());
    }

    Page<TrainResearchOrgEntity> page = new Page<>(req.page(), req.pageSize());
    Page<TrainResearchOrgEntity> result = trainResearchOrgMapper.selectPage(page, wrapper);
    List<TrainResearchOrgVO> list = result.getRecords().stream().map(this::toVO).toList();
    return new PageResponse<>(list, result.getCurrent(), result.getSize(), result.getTotal());
  }

  public TrainResearchOrgVO getById(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainResearchOrgEntity entity = trainResearchOrgMapper.selectOne(
        new LambdaQueryWrapper<TrainResearchOrgEntity>()
            .eq(TrainResearchOrgEntity::getId, id)
            .eq(TrainResearchOrgEntity::getTenantId, tenantId));
    if (entity == null) {
      throw new BusinessException("NOT_FOUND", "记录不存在");
    }
    return toVO(entity);
  }

  public TrainResearchOrgVO create(TrainResearchOrgCreateReq req) {
    TrainResearchOrgEntity entity = new TrainResearchOrgEntity();
    entity.setTitle(req.title());
    entity.setType(req.type());
    entity.setProvinceId(req.provinceId());
    entity.setProvinceName(req.provinceName());
    entity.setCityId(req.cityId());
    entity.setCityName(req.cityName());
    entity.setAreaId(req.areaId());
    entity.setAreaName(req.areaName());
    entity.setSortNumber(req.sortNumber() != null ? req.sortNumber() : 100);
    entity.setStatus(req.status() != null ? req.status() : 1);
    entity.setTenantId(SecurityContext.currentUser().getTenantId());
    entity.setUserId(SecurityContext.currentUser().getUserId());
    trainResearchOrgMapper.insert(entity);
    return toVO(entity);
  }

  public TrainResearchOrgVO update(Long id, TrainResearchOrgUpdateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainResearchOrgEntity entity = trainResearchOrgMapper.selectOne(
        new LambdaQueryWrapper<TrainResearchOrgEntity>()
            .eq(TrainResearchOrgEntity::getId, id)
            .eq(TrainResearchOrgEntity::getTenantId, tenantId));
    if (entity == null) {
      throw new BusinessException("NOT_FOUND", "记录不存在");
    }
    if (req.title() != null) entity.setTitle(req.title());
    if (req.type() != null) entity.setType(req.type());
    if (req.provinceId() != null) entity.setProvinceId(req.provinceId());
    if (req.provinceName() != null) entity.setProvinceName(req.provinceName());
    if (req.cityId() != null) entity.setCityId(req.cityId());
    if (req.cityName() != null) entity.setCityName(req.cityName());
    if (req.areaId() != null) entity.setAreaId(req.areaId());
    if (req.areaName() != null) entity.setAreaName(req.areaName());
    if (req.sortNumber() != null) entity.setSortNumber(req.sortNumber());
    if (req.status() != null) entity.setStatus(req.status());
    trainResearchOrgMapper.updateById(entity);
    return toVO(entity);
  }

  public void delete(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainResearchOrgEntity entity = trainResearchOrgMapper.selectOne(
        new LambdaQueryWrapper<TrainResearchOrgEntity>()
            .eq(TrainResearchOrgEntity::getId, id)
            .eq(TrainResearchOrgEntity::getTenantId, tenantId));
    if (entity == null) {
      throw new BusinessException("NOT_FOUND", "记录不存在");
    }
    trainResearchOrgMapper.deleteById(id);
  }

  private TrainResearchOrgVO toVO(TrainResearchOrgEntity e) {
    return new TrainResearchOrgVO(
        e.getId(),
        e.getTitle(),
        e.getType(),
        e.getProvinceId(),
        e.getProvinceName(),
        e.getCityId(),
        e.getCityName(),
        e.getAreaId(),
        e.getAreaName(),
        e.getUserId(),
        e.getSortNumber(),
        e.getStatus(),
        e.getCreateTime(),
        e.getUpdateTime());
  }
}
