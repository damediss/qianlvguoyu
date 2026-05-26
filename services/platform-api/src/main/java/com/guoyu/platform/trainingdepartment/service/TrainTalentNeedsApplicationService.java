package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainTalentNeedsCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTalentNeedsQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTalentNeedsUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainTalentNeedsEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainTalentNeedsMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainTalentNeedsVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 人才需求管理服务实现
 */
@Service
@Slf4j
public class TrainTalentNeedsApplicationService {

  private final TrainTalentNeedsMapper mapper;

  public TrainTalentNeedsApplicationService(TrainTalentNeedsMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainTalentNeedsVO> page(TrainTalentNeedsQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainTalentNeedsEntity> wrapper = new LambdaQueryWrapper<TrainTalentNeedsEntity>()
        .eq(TrainTalentNeedsEntity::getTenantId, tenantId)
        .eq(TrainTalentNeedsEntity::getDeleted, 0)
        .orderByDesc(TrainTalentNeedsEntity::getCreateTime);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(w -> w
          .like(TrainTalentNeedsEntity::getTitle, req.getKeyword())
          .or()
          .like(TrainTalentNeedsEntity::getOrgName, req.getKeyword())
          .or()
          .like(TrainTalentNeedsEntity::getJob, req.getKeyword())
          .or()
          .like(TrainTalentNeedsEntity::getWorkType, req.getKeyword()));
    }
    if (req.getOrgId() != null) {
      wrapper.eq(TrainTalentNeedsEntity::getOrgId, req.getOrgId());
    }
    if (req.getGender() != null) {
      wrapper.eq(TrainTalentNeedsEntity::getGender, req.getGender());
    }
    if (StringUtils.hasText(req.getEdu())) {
      wrapper.eq(TrainTalentNeedsEntity::getEdu, req.getEdu());
    }
    Page<TrainTalentNeedsEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainTalentNeedsVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TrainTalentNeedsVO create(TrainTalentNeedsCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    TrainTalentNeedsEntity entity = new TrainTalentNeedsEntity();
    entity.setTenantId(tenantId);
    entity.setUserId(userId);
    entity.setTitle(req.getTitle());
    entity.setOrgId(req.getOrgId());
    entity.setOrgName(req.getOrgName());
    entity.setJob(req.getJob());
    entity.setWorkType(req.getWorkType());
    entity.setMajor(req.getMajor());
    entity.setEdu(req.getEdu());
    entity.setGender(req.getGender());
    entity.setNum(req.getNum());
    entity.setAddress(req.getAddress());
    entity.setWorkYear(req.getWorkYear());
    entity.setInPositionDate(req.getInPositionDate());
    entity.setPaperRequire(req.getPaperRequire());
    entity.setSortNumber(req.getSortNumber());
    entity.setJsonData(req.getJsonData());
    entity.setDuty(req.getDuty());
    entity.setSchoolNeed(req.getSchoolNeed());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainTalentNeedsVO update(Long id, TrainTalentNeedsUpdateReq req) {
    TrainTalentNeedsEntity entity = getExisting(id);
    if (StringUtils.hasText(req.getTitle())) {
      entity.setTitle(req.getTitle());
    }
    if (req.getOrgId() != null) {
      entity.setOrgId(req.getOrgId());
    }
    if (StringUtils.hasText(req.getOrgName())) {
      entity.setOrgName(req.getOrgName());
    }
    if (StringUtils.hasText(req.getJob())) {
      entity.setJob(req.getJob());
    }
    if (StringUtils.hasText(req.getWorkType())) {
      entity.setWorkType(req.getWorkType());
    }
    if (StringUtils.hasText(req.getMajor())) {
      entity.setMajor(req.getMajor());
    }
    if (StringUtils.hasText(req.getEdu())) {
      entity.setEdu(req.getEdu());
    }
    if (req.getGender() != null) {
      entity.setGender(req.getGender());
    }
    if (req.getNum() != null) {
      entity.setNum(req.getNum());
    }
    if (StringUtils.hasText(req.getAddress())) {
      entity.setAddress(req.getAddress());
    }
    if (StringUtils.hasText(req.getWorkYear())) {
      entity.setWorkYear(req.getWorkYear());
    }
    if (StringUtils.hasText(req.getInPositionDate())) {
      entity.setInPositionDate(req.getInPositionDate());
    }
    if (StringUtils.hasText(req.getPaperRequire())) {
      entity.setPaperRequire(req.getPaperRequire());
    }
    if (req.getSortNumber() != null) {
      entity.setSortNumber(req.getSortNumber());
    }
    if (StringUtils.hasText(req.getJsonData())) {
      entity.setJsonData(req.getJsonData());
    }
    if (StringUtils.hasText(req.getDuty())) {
      entity.setDuty(req.getDuty());
    }
    if (StringUtils.hasText(req.getSchoolNeed())) {
      entity.setSchoolNeed(req.getSchoolNeed());
    }
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainTalentNeedsEntity entity = getExisting(id);
    entity.setDeleted(1);
    mapper.updateById(entity);
  }

  private TrainTalentNeedsEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainTalentNeedsEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainTalentNeedsEntity>()
        .eq(TrainTalentNeedsEntity::getTenantId, tenantId)
        .eq(TrainTalentNeedsEntity::getId, id)
        .eq(TrainTalentNeedsEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private TrainTalentNeedsVO toVO(TrainTalentNeedsEntity entity) {
    return new TrainTalentNeedsVO(
        entity.getId(),
        entity.getTitle(),
        entity.getOrgId(),
        entity.getOrgName(),
        entity.getJob(),
        entity.getWorkType(),
        entity.getMajor(),
        entity.getEdu(),
        entity.getGender(),
        entity.getNum(),
        entity.getAddress(),
        entity.getWorkYear(),
        entity.getInPositionDate(),
        entity.getPaperRequire(),
        entity.getUserId(),
        entity.getSortNumber(),
        entity.getTenantId(),
        entity.getCreateTime(),
        entity.getJsonData(),
        entity.getDuty(),
        entity.getSchoolNeed()
    );
  }
}
