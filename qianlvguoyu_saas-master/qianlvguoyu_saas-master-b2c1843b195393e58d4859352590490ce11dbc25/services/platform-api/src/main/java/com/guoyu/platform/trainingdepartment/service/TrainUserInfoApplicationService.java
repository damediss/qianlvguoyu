package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainUserInfoCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserInfoQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserInfoUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainUserInfoEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainUserInfoMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainUserInfoVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 用户信息管理服务实现
 */
@Service
@Slf4j
public class TrainUserInfoApplicationService {

  private final TrainUserInfoMapper mapper;

  public TrainUserInfoApplicationService(TrainUserInfoMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainUserInfoVO> page(TrainUserInfoQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainUserInfoEntity> wrapper = new LambdaQueryWrapper<TrainUserInfoEntity>()
        .eq(TrainUserInfoEntity::getTenantId, tenantId)
        .eq(TrainUserInfoEntity::getDeleted, 0)
        .orderByDesc(TrainUserInfoEntity::getCreateTime);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(w -> w
          .like(TrainUserInfoEntity::getPersonType, req.getKeyword())
          .or()
          .like(TrainUserInfoEntity::getNationality, req.getKeyword())
          .or()
          .like(TrainUserInfoEntity::getEducation, req.getKeyword()));
    }
    if (req.getCompanyId() != null) {
      wrapper.eq(TrainUserInfoEntity::getCompanyId, req.getCompanyId());
    }
    Page<TrainUserInfoEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainUserInfoVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TrainUserInfoVO create(TrainUserInfoCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    TrainUserInfoEntity entity = new TrainUserInfoEntity();
    entity.setTenantId(tenantId);
    entity.setUserId(userId);
    entity.setCompanyId(req.getCompanyId());
    entity.setPersonType(req.getPersonType());
    entity.setPersonSupplementary(req.getPersonSupplementary());
    entity.setHouseType(req.getHouseType());
    entity.setNationality(req.getNationality());
    entity.setEducation(req.getEducation());
    entity.setJobIntention(req.getJobIntention());
    entity.setIsPartyMember(req.getIsPartyMember());
    entity.setAddress(req.getAddress());
    entity.setIdCardFront(req.getIdCardFront());
    entity.setIdCardBack(req.getIdCardBack());
    entity.setPhoto(req.getPhoto());
    entity.setEduPhoto(req.getEduPhoto());
    entity.setWorkContract(req.getWorkContract());
    entity.setSortNumber(req.getSortNumber());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainUserInfoVO update(Long id, TrainUserInfoUpdateReq req) {
    TrainUserInfoEntity entity = getExisting(id);
    entity.setCompanyId(req.getCompanyId());
    entity.setPersonType(req.getPersonType());
    entity.setPersonSupplementary(req.getPersonSupplementary());
    entity.setHouseType(req.getHouseType());
    entity.setNationality(req.getNationality());
    entity.setEducation(req.getEducation());
    entity.setJobIntention(req.getJobIntention());
    entity.setIsPartyMember(req.getIsPartyMember());
    entity.setAddress(req.getAddress());
    entity.setIdCardFront(req.getIdCardFront());
    entity.setIdCardBack(req.getIdCardBack());
    entity.setPhoto(req.getPhoto());
    entity.setEduPhoto(req.getEduPhoto());
    entity.setWorkContract(req.getWorkContract());
    entity.setSortNumber(req.getSortNumber());
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainUserInfoEntity entity = getExisting(id);
    entity.setDeleted(1);
    mapper.updateById(entity);
  }

  private TrainUserInfoEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainUserInfoEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainUserInfoEntity>()
        .eq(TrainUserInfoEntity::getTenantId, tenantId)
        .eq(TrainUserInfoEntity::getId, id)
        .eq(TrainUserInfoEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private TrainUserInfoVO toVO(TrainUserInfoEntity entity) {
    return new TrainUserInfoVO(
        entity.getId(),
        entity.getUserId(),
        entity.getCompanyId(),
        entity.getPersonType(),
        entity.getPersonSupplementary(),
        entity.getHouseType(),
        entity.getNationality(),
        entity.getEducation(),
        entity.getJobIntention(),
        entity.getIsPartyMember(),
        entity.getAddress(),
        entity.getIdCardFront(),
        entity.getIdCardBack(),
        entity.getPhoto(),
        entity.getEduPhoto(),
        entity.getWorkContract(),
        entity.getSortNumber(),
        entity.getTenantId(),
        entity.getCreateTime()
    );
  }
}
