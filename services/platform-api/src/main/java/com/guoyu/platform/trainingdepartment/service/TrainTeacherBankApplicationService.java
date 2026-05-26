package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainTeacherBankCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTeacherBankQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTeacherBankUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainTeacherBankEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainTeacherBankMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainTeacherBankVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 培训师资库管理服务实现
 */
@Service
@Slf4j
public class TrainTeacherBankApplicationService {

  private final TrainTeacherBankMapper mapper;

  public TrainTeacherBankApplicationService(TrainTeacherBankMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainTeacherBankVO> page(TrainTeacherBankQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainTeacherBankEntity> wrapper = new LambdaQueryWrapper<TrainTeacherBankEntity>()
        .eq(TrainTeacherBankEntity::getTenantId, tenantId)
        .eq(TrainTeacherBankEntity::getDeleted, 0)
        .orderByDesc(TrainTeacherBankEntity::getCreateTime);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(w -> w
          .like(TrainTeacherBankEntity::getName, req.getKeyword())
          .or()
          .like(TrainTeacherBankEntity::getPhone, req.getKeyword())
          .or()
          .like(TrainTeacherBankEntity::getCompany, req.getKeyword()));
    }
    if (req.getStatus() != null) {
      wrapper.eq(TrainTeacherBankEntity::getStatus, req.getStatus());
    }
    if (StringUtils.hasText(req.getEducation())) {
      wrapper.eq(TrainTeacherBankEntity::getEducation, req.getEducation());
    }
    Page<TrainTeacherBankEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainTeacherBankVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TrainTeacherBankVO create(TrainTeacherBankCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    TrainTeacherBankEntity entity = new TrainTeacherBankEntity();
    entity.setTenantId(tenantId);
    entity.setUserId(userId);
    entity.setName(req.getName());
    entity.setPhone(req.getPhone());
    entity.setIdCard(req.getIdCard());
    entity.setTeacherLicense(req.getTeacherLicense());
    entity.setEngineerLicense(req.getEngineerLicense());
    entity.setLevelLicense(req.getLevelLicense());
    entity.setEduLicense(req.getEduLicense());
    entity.setGraduatePaper(req.getGraduatePaper());
    entity.setCompany(req.getCompany());
    entity.setAddress(req.getAddress());
    entity.setIdCardNumber(req.getIdCardNumber());
    entity.setStartDate(req.getStartDate());
    entity.setEndDate(req.getEndDate());
    entity.setBirthday(req.getBirthday());
    entity.setGender(req.getGender());
    entity.setNation(req.getNation());
    entity.setEducation(req.getEducation());
    entity.setMajor(req.getMajor());
    entity.setStatus(req.getStatus());
    entity.setSortNumber(req.getSortNumber());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainTeacherBankVO update(Long id, TrainTeacherBankUpdateReq req) {
    TrainTeacherBankEntity entity = getExisting(id);
    if (req.getName() != null) {
      entity.setName(req.getName());
    }
    if (req.getPhone() != null) {
      entity.setPhone(req.getPhone());
    }
    if (req.getIdCard() != null) {
      entity.setIdCard(req.getIdCard());
    }
    if (req.getTeacherLicense() != null) {
      entity.setTeacherLicense(req.getTeacherLicense());
    }
    if (req.getEngineerLicense() != null) {
      entity.setEngineerLicense(req.getEngineerLicense());
    }
    if (req.getLevelLicense() != null) {
      entity.setLevelLicense(req.getLevelLicense());
    }
    if (req.getEduLicense() != null) {
      entity.setEduLicense(req.getEduLicense());
    }
    if (req.getGraduatePaper() != null) {
      entity.setGraduatePaper(req.getGraduatePaper());
    }
    if (req.getCompany() != null) {
      entity.setCompany(req.getCompany());
    }
    if (req.getAddress() != null) {
      entity.setAddress(req.getAddress());
    }
    if (req.getIdCardNumber() != null) {
      entity.setIdCardNumber(req.getIdCardNumber());
    }
    if (req.getStartDate() != null) {
      entity.setStartDate(req.getStartDate());
    }
    if (req.getEndDate() != null) {
      entity.setEndDate(req.getEndDate());
    }
    if (req.getBirthday() != null) {
      entity.setBirthday(req.getBirthday());
    }
    if (req.getGender() != null) {
      entity.setGender(req.getGender());
    }
    if (req.getNation() != null) {
      entity.setNation(req.getNation());
    }
    if (req.getEducation() != null) {
      entity.setEducation(req.getEducation());
    }
    if (req.getMajor() != null) {
      entity.setMajor(req.getMajor());
    }
    if (req.getStatus() != null) {
      entity.setStatus(req.getStatus());
    }
    if (req.getUserId() != null) {
      entity.setUserId(req.getUserId());
    }
    if (req.getSortNumber() != null) {
      entity.setSortNumber(req.getSortNumber());
    }
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainTeacherBankEntity entity = getExisting(id);
    entity.setDeleted(1);
    mapper.updateById(entity);
  }

  private TrainTeacherBankEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainTeacherBankEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainTeacherBankEntity>()
        .eq(TrainTeacherBankEntity::getTenantId, tenantId)
        .eq(TrainTeacherBankEntity::getId, id)
        .eq(TrainTeacherBankEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private TrainTeacherBankVO toVO(TrainTeacherBankEntity entity) {
    return new TrainTeacherBankVO(
        entity.getId(),
        entity.getName(),
        entity.getPhone(),
        entity.getIdCard(),
        entity.getTeacherLicense(),
        entity.getEngineerLicense(),
        entity.getLevelLicense(),
        entity.getEduLicense(),
        entity.getGraduatePaper(),
        entity.getCompany(),
        entity.getAddress(),
        entity.getIdCardNumber(),
        entity.getStartDate(),
        entity.getEndDate(),
        entity.getBirthday(),
        entity.getGender(),
        entity.getNation(),
        entity.getEducation(),
        entity.getMajor(),
        entity.getStatus(),
        entity.getTenantId(),
        entity.getUserId(),
        entity.getSortNumber(),
        entity.getCreateTime(),
        entity.getUpdateTime()
    );
  }
}
