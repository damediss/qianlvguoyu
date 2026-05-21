package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainSignUpPeopleSelfCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSignUpPeopleSelfQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSignUpPeopleSelfUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpPeopleSelfMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainSignUpPeopleSelfVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class TrainSignUpPeopleSelfApplicationService {

  private final TrainSignUpPeopleSelfMapper mapper;

  public TrainSignUpPeopleSelfApplicationService(TrainSignUpPeopleSelfMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainSignUpPeopleSelfVO> page(TrainSignUpPeopleSelfQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainSignUpPeopleSelfEntity> wrapper = new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
        .eq(TrainSignUpPeopleSelfEntity::getTenantId, tenantId)
        .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0)
        .orderByDesc(TrainSignUpPeopleSelfEntity::getId);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.like(TrainSignUpPeopleSelfEntity::getId, req.getKeyword());
    }
    Page<TrainSignUpPeopleSelfEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainSignUpPeopleSelfVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TrainSignUpPeopleSelfVO create(TrainSignUpPeopleSelfCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainSignUpPeopleSelfEntity entity = new TrainSignUpPeopleSelfEntity();
    entity.setTenantId(tenantId);
    entity.setSignUpId(req.getSignUpId());
    entity.setPlatformUserId(req.getPlatformUserId());
    entity.setName(req.getName());
    entity.setPhone(req.getPhone());
    entity.setIdCardNumber(req.getIdCardNumber());
    entity.setGender(req.getGender());
    entity.setEducation(req.getEducation());
    entity.setPhoto(req.getPhoto());
    entity.setContract(req.getContract());
    entity.setCertificate(req.getCertificate());
    entity.setStatus(req.getStatus());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainSignUpPeopleSelfVO update(Long id, TrainSignUpPeopleSelfUpdateReq req) {
    TrainSignUpPeopleSelfEntity entity = getExisting(id);
    if (req.getSignUpId() != null) { entity.setSignUpId(req.getSignUpId()); }
    if (req.getPlatformUserId() != null) { entity.setPlatformUserId(req.getPlatformUserId()); }
    if (StringUtils.hasText(req.getName())) { entity.setName(req.getName()); }
    if (StringUtils.hasText(req.getPhone())) { entity.setPhone(req.getPhone()); }
    if (StringUtils.hasText(req.getIdCardNumber())) { entity.setIdCardNumber(req.getIdCardNumber()); }
    if (StringUtils.hasText(req.getGender())) { entity.setGender(req.getGender()); }
    if (StringUtils.hasText(req.getEducation())) { entity.setEducation(req.getEducation()); }
    if (StringUtils.hasText(req.getPhoto())) { entity.setPhoto(req.getPhoto()); }
    if (StringUtils.hasText(req.getContract())) { entity.setContract(req.getContract()); }
    if (StringUtils.hasText(req.getCertificate())) { entity.setCertificate(req.getCertificate()); }
    if (req.getStatus() != null) { entity.setStatus(req.getStatus()); }
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainSignUpPeopleSelfEntity entity = getExisting(id);

    entity.setDeleted(1);
    mapper.updateById(entity);
  }

  private TrainSignUpPeopleSelfEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainSignUpPeopleSelfEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()

        .eq(TrainSignUpPeopleSelfEntity::getTenantId, tenantId)
        .eq(TrainSignUpPeopleSelfEntity::getId, id)
        .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private TrainSignUpPeopleSelfVO toVO(TrainSignUpPeopleSelfEntity e) {
    return new TrainSignUpPeopleSelfVO(
        e.getId(),
        e.getSignUpId(),
        e.getPlatformUserId(),
        e.getName(),
        e.getPhone(),
        e.getIdCardNumber(),
        e.getGender(),
        e.getEducation(),
        e.getPhoto(),
        e.getContract(),
        e.getCertificate(),
        e.getStatus(),
        e.getTenantId(),
        e.getCreateTime(),
        e.getUpdateTime()
    );
  }
}
