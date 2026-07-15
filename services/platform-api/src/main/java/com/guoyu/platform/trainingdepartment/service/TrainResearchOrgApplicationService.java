package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainResearchOrgCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainResearchOrgQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainResearchOrgUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainResearchOrgEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpSelfEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainResearchOrgMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpSelfMapper;
import com.guoyu.platform.trainingdepartment.support.ResearchCollectionFieldOptionsSupport;
import com.guoyu.platform.trainingdepartment.support.ResearchCollectionFieldOptionsSupport.ResearchCollectionFieldOptions;
import com.guoyu.platform.trainingdepartment.vo.TrainResearchOrgVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TrainResearchOrgApplicationService {

  private final TrainResearchOrgMapper trainResearchOrgMapper;
  private final OrgMapper orgMapper;
  private final TrainSignUpSelfMapper signUpSelfMapper;
  private final ResearchCollectionFieldOptionsSupport fieldOptionsSupport;

  public TrainResearchOrgApplicationService(
      TrainResearchOrgMapper trainResearchOrgMapper,
      OrgMapper orgMapper,
      TrainSignUpSelfMapper signUpSelfMapper,
      ResearchCollectionFieldOptionsSupport fieldOptionsSupport) {
    this.trainResearchOrgMapper = trainResearchOrgMapper;
    this.orgMapper = orgMapper;
    this.signUpSelfMapper = signUpSelfMapper;
    this.fieldOptionsSupport = fieldOptionsSupport;
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
    Map<Long, String> orgNames = resolveOrgNames(result.getRecords());
    List<TrainResearchOrgVO> list = result.getRecords().stream()
        .map(e -> toVO(e, orgNames.get(e.getTargetOrgId())))
        .toList();
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
    String orgName = resolveOrgName(entity.getTargetOrgId());
    return toVO(entity, orgName);
  }

  public TrainResearchOrgVO create(TrainResearchOrgCreateReq req) {
    TrainResearchOrgEntity entity = new TrainResearchOrgEntity();
    entity.setTitle(req.title());
    entity.setTargetOrgId(req.targetOrgId());
    entity.setPersonnelCount(0);
    entity.setType(req.type());
    entity.setProvinceId(req.provinceId());
    entity.setProvinceName(req.provinceName());
    entity.setCityId(req.cityId());
    entity.setCityName(req.cityName());
    entity.setAreaId(req.areaId());
    entity.setAreaName(req.areaName());
    entity.setSortNumber(req.sortNumber() != null ? req.sortNumber() : 100);
    entity.setStatus(req.status() != null ? req.status() : 1);
    entity.setCollectionFieldOptions(fieldOptionsSupport.serialize(
        fieldOptionsSupport.mergeIncoming(req.collectionFieldOptions())));
    entity.setTenantId(SecurityContext.currentUser().getTenantId());
    entity.setUserId(SecurityContext.currentUser().getUserId());
    trainResearchOrgMapper.insert(entity);
    return toVO(entity, resolveOrgName(entity.getTargetOrgId()));
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
    if (req.targetOrgId() != null) entity.setTargetOrgId(req.targetOrgId());
    if (req.type() != null) entity.setType(req.type());
    if (req.provinceId() != null) entity.setProvinceId(req.provinceId());
    if (req.provinceName() != null) entity.setProvinceName(req.provinceName());
    if (req.cityId() != null) entity.setCityId(req.cityId());
    if (req.cityName() != null) entity.setCityName(req.cityName());
    if (req.areaId() != null) entity.setAreaId(req.areaId());
    if (req.areaName() != null) entity.setAreaName(req.areaName());
    if (req.sortNumber() != null) entity.setSortNumber(req.sortNumber());
    if (req.status() != null) entity.setStatus(req.status());
    if (req.collectionFieldOptions() != null) {
      String serialized = fieldOptionsSupport.serialize(
          fieldOptionsSupport.mergeIncoming(req.collectionFieldOptions()));
      entity.setCollectionFieldOptions(serialized);
      syncActiveSignUpFieldOptions(entity.getActiveSignUpId(), serialized);
    } else {
      entity.setCollectionFieldOptions(null);
      syncActiveSignUpFieldOptions(entity.getActiveSignUpId(), null);
    }
    entity.setUpdateTime(LocalDateTime.now());
    trainResearchOrgMapper.updateById(entity);
    return toVO(entity, resolveOrgName(entity.getTargetOrgId()));
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

  private TrainResearchOrgVO toVO(TrainResearchOrgEntity e, String targetOrgName) {
    return new TrainResearchOrgVO(
        e.getId(),
        e.getTitle(),
        e.getTargetOrgId(),
        targetOrgName,
        e.getActiveSignUpId(),
        e.getPersonnelCount(),
        fieldOptionsSupport.parse(e.getCollectionFieldOptions()),
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

  private Map<Long, String> resolveOrgNames(List<TrainResearchOrgEntity> records) {
    List<Long> orgIds = records.stream()
        .map(TrainResearchOrgEntity::getTargetOrgId)
        .filter(id -> id != null && id > 0)
        .distinct()
        .toList();
    if (orgIds.isEmpty()) {
      return Map.of();
    }
    return orgMapper.selectBatchIds(orgIds).stream()
        .collect(Collectors.toMap(OrgEntity::getId, OrgEntity::getName, (a, b) -> a));
  }

  private String resolveOrgName(Long orgId) {
    if (orgId == null || orgId <= 0) {
      return null;
    }
    OrgEntity org = orgMapper.selectById(orgId);
    return org != null ? org.getName() : null;
  }

  /** 编辑调研企业字段配置时，同步到已开启的采集批次，H5 扫码才能读到最新选项 */
  private void syncActiveSignUpFieldOptions(Long activeSignUpId, String fieldOptionsJson) {
    if (activeSignUpId == null) {
      return;
    }
    TrainSignUpSelfEntity signUp = signUpSelfMapper.selectById(activeSignUpId);
    if (signUp == null || Integer.valueOf(1).equals(signUp.getDeleted())) {
      return;
    }
    if (!"research".equals(signUp.getCollectionSource())) {
      return;
    }
    signUp.setCollectionFieldOptions(fieldOptionsJson);
    signUp.setUpdateTime(LocalDateTime.now());
    signUpSelfMapper.updateById(signUp);
  }
}
