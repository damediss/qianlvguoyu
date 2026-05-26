package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainSignUpOrcSelfCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSignUpOrcSelfQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSignUpOrcSelfUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpOrcSelfEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpOrcSelfMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainSignUpOrcSelfVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class TrainSignUpOrcSelfApplicationService {

  private final TrainSignUpOrcSelfMapper mapper;

  public TrainSignUpOrcSelfApplicationService(TrainSignUpOrcSelfMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainSignUpOrcSelfVO> page(TrainSignUpOrcSelfQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainSignUpOrcSelfEntity> wrapper = new LambdaQueryWrapper<TrainSignUpOrcSelfEntity>()
        .eq(TrainSignUpOrcSelfEntity::getTenantId, tenantId)
        .eq(TrainSignUpOrcSelfEntity::getDeleted, 0)
        .orderByDesc(TrainSignUpOrcSelfEntity::getId);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.like(TrainSignUpOrcSelfEntity::getId, req.getKeyword());
    }
    Page<TrainSignUpOrcSelfEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainSignUpOrcSelfVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TrainSignUpOrcSelfVO create(TrainSignUpOrcSelfCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainSignUpOrcSelfEntity entity = new TrainSignUpOrcSelfEntity();
    entity.setTenantId(tenantId);
    entity.setSignUpId(req.getSignUpId());
    entity.setPlatformUserId(req.getPlatformUserId());
    entity.setOrcData(req.getOrcData());
    entity.setStatus(req.getStatus());
    entity.setFailReason(req.getFailReason());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainSignUpOrcSelfVO update(Long id, TrainSignUpOrcSelfUpdateReq req) {
    TrainSignUpOrcSelfEntity entity = getExisting(id);
    if (req.getSignUpId() != null) { entity.setSignUpId(req.getSignUpId()); }
    if (req.getPlatformUserId() != null) { entity.setPlatformUserId(req.getPlatformUserId()); }
    if (StringUtils.hasText(req.getOrcData())) { entity.setOrcData(req.getOrcData()); }
    if (req.getStatus() != null) { entity.setStatus(req.getStatus()); }
    if (StringUtils.hasText(req.getFailReason())) { entity.setFailReason(req.getFailReason()); }
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainSignUpOrcSelfEntity entity = getExisting(id);

    entity.setDeleted(1);
    mapper.updateById(entity);
  }

  private TrainSignUpOrcSelfEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainSignUpOrcSelfEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainSignUpOrcSelfEntity>()

        .eq(TrainSignUpOrcSelfEntity::getTenantId, tenantId)
        .eq(TrainSignUpOrcSelfEntity::getId, id)
        .eq(TrainSignUpOrcSelfEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private TrainSignUpOrcSelfVO toVO(TrainSignUpOrcSelfEntity e) {
    return new TrainSignUpOrcSelfVO(
        e.getId(),
        e.getSignUpId(),
        e.getPlatformUserId(),
        e.getOrcData(),
        e.getStatus(),
        e.getFailReason(),
        e.getTenantId(),
        e.getCreateTime(),
        e.getUpdateTime()
    );
  }
}
