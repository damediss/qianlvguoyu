package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainSignUpSelfCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSignUpSelfQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSignUpSelfUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpSelfEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpSelfMapper;
import com.guoyu.platform.trainingdepartment.service.TrainingOperatorScopeService;
import com.guoyu.platform.trainingdepartment.vo.TrainSignUpSelfVO;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class TrainSignUpSelfApplicationService {

  private final TrainSignUpSelfMapper mapper;
  private final TrainingOperatorScopeService operatorScopeService;

  public TrainSignUpSelfApplicationService(
      TrainSignUpSelfMapper mapper,
      TrainingOperatorScopeService operatorScopeService) {
    this.mapper = mapper;
    this.operatorScopeService = operatorScopeService;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainSignUpSelfVO> page(TrainSignUpSelfQueryReq req) {
    LambdaQueryWrapper<TrainSignUpSelfEntity> wrapper = new LambdaQueryWrapper<TrainSignUpSelfEntity>()
        .eq(TrainSignUpSelfEntity::getDeleted, 0)
        .orderByAsc(TrainSignUpSelfEntity::getSortNumber)
        .orderByDesc(TrainSignUpSelfEntity::getCreateTime)
        .orderByDesc(TrainSignUpSelfEntity::getId);
    Long companyFilter = operatorScopeService.resolveCompanyFilter(req.getOrgId());
    if (companyFilter != null) {
      wrapper.eq(TrainSignUpSelfEntity::getOrgId, companyFilter);
    }
    if (req.getTrainInfoId() != null) {
      wrapper.eq(TrainSignUpSelfEntity::getTrainInfoId, req.getTrainInfoId());
    }
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.like(TrainSignUpSelfEntity::getTitle, req.getKeyword());
    }
    Page<TrainSignUpSelfEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainSignUpSelfVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TrainSignUpSelfVO create(TrainSignUpSelfCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainSignUpSelfEntity entity = new TrainSignUpSelfEntity();
    entity.setTenantId(tenantId);
    entity.setTitle(req.getTitle());
    entity.setCover(req.getCover());
    entity.setTrainTime(req.getTrainTime());
    entity.setExamTime(req.getExamTime());
    entity.setOrgId(req.getOrgId());
    entity.setTrainInfoId(req.getTrainInfoId());
    entity.setSortNumber(req.getSortNumber());
    entity.setDeleted(0);
    entity.setCreateTime(LocalDateTime.now());
    entity.setUpdateTime(LocalDateTime.now());
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainSignUpSelfVO update(Long id, TrainSignUpSelfUpdateReq req) {
    TrainSignUpSelfEntity entity = getExisting(id);
    if (StringUtils.hasText(req.getTitle())) { entity.setTitle(req.getTitle()); }
    if (StringUtils.hasText(req.getCover())) { entity.setCover(req.getCover()); }
    if (StringUtils.hasText(req.getTrainTime())) { entity.setTrainTime(req.getTrainTime()); }
    if (StringUtils.hasText(req.getExamTime())) { entity.setExamTime(req.getExamTime()); }
    if (req.getOrgId() != null) { entity.setOrgId(req.getOrgId()); }
    if (req.getTrainInfoId() != null) { entity.setTrainInfoId(req.getTrainInfoId()); }
    if (req.getSortNumber() != null) { entity.setSortNumber(req.getSortNumber()); }
    entity.setUpdateTime(LocalDateTime.now());
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainSignUpSelfEntity entity = getExisting(id);

    entity.setDeleted(1);
    mapper.updateById(entity);
  }

  private TrainSignUpSelfEntity getExisting(Long id) {
    TrainSignUpSelfEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainSignUpSelfEntity>()
        .eq(TrainSignUpSelfEntity::getId, id)
        .eq(TrainSignUpSelfEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    Long companyFilter = operatorScopeService.resolveCompanyFilter(entity.getOrgId());
    if (companyFilter != null && entity.getOrgId() != null && !companyFilter.equals(entity.getOrgId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该报名计划");
    }
    return entity;
  }

  private TrainSignUpSelfVO toVO(TrainSignUpSelfEntity e) {
    return new TrainSignUpSelfVO(
        e.getId(),
        e.getTitle(),
        e.getCover(),
        e.getTrainTime(),
        e.getExamTime(),
        e.getOrgId(),
        e.getTrainInfoId(),
        e.getSortNumber(),
        e.getTenantId(),
        e.getCreateTime(),
        e.getUpdateTime()
    );
  }
}
