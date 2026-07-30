package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainNeedCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainNeedQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainNeedUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainNeedEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainNeedMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainNeedVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 培训需求管理服务实现
 */
@Service
@Slf4j
public class TrainNeedApplicationService {

  private final TrainNeedMapper mapper;

  public TrainNeedApplicationService(TrainNeedMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainNeedVO> page(TrainNeedQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainNeedEntity> wrapper = new LambdaQueryWrapper<TrainNeedEntity>()
        .eq(TrainNeedEntity::getTenantId, tenantId)
        .eq(TrainNeedEntity::getDeleted, 0)
        .orderByDesc(TrainNeedEntity::getCreateTime);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(w -> w
          .like(TrainNeedEntity::getTrainObject, req.getKeyword())
          .or()
          .like(TrainNeedEntity::getPosition, req.getKeyword())
          .or()
          .like(TrainNeedEntity::getOrgName, req.getKeyword()));
    }
    if (req.getOrgId() != null) {
      wrapper.eq(TrainNeedEntity::getOrgId, req.getOrgId());
    }
    Page<TrainNeedEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainNeedVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TrainNeedVO create(TrainNeedCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    TrainNeedEntity entity = new TrainNeedEntity();
    entity.setTenantId(tenantId);
    entity.setUserId(userId);
    entity.setOrgId(req.getOrgId());
    entity.setOrgName(req.getOrgName());
    entity.setTrainObject(req.getTrainObject());
    entity.setPosition(req.getPosition());
    entity.setNum(req.getNum());
    entity.setWorkType(req.getWorkType());
    entity.setLevel(req.getLevel());
    entity.setTarget(req.getTarget());
    entity.setMethod(req.getMethod());
    entity.setAddress(req.getAddress());
    entity.setTrainDate(req.getTrainDate());
    entity.setBudget(req.getBudget());
    entity.setTeacherRequire(req.getTeacherRequire());
    entity.setNeedExam(req.getNeedExam());
    entity.setNeedMatch(req.getNeedMatch());
    entity.setNeedPaper(req.getNeedPaper());
    entity.setSortNumber(req.getSortNumber());
    entity.setJsonData(req.getJsonData());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainNeedVO update(Long id, TrainNeedUpdateReq req) {
    TrainNeedEntity entity = getExisting(id);
    if (req.getOrgId() != null) {
      entity.setOrgId(req.getOrgId());
    }
    if (req.getOrgName() != null) {
      entity.setOrgName(req.getOrgName());
    }
    if (req.getTrainObject() != null) {
      entity.setTrainObject(req.getTrainObject());
    }
    if (req.getPosition() != null) {
      entity.setPosition(req.getPosition());
    }
    if (req.getNum() != null) {
      entity.setNum(req.getNum());
    }
    if (req.getWorkType() != null) {
      entity.setWorkType(req.getWorkType());
    }
    if (req.getLevel() != null) {
      entity.setLevel(req.getLevel());
    }
    if (req.getTarget() != null) {
      entity.setTarget(req.getTarget());
    }
    if (req.getMethod() != null) {
      entity.setMethod(req.getMethod());
    }
    if (req.getAddress() != null) {
      entity.setAddress(req.getAddress());
    }
    if (req.getTrainDate() != null) {
      entity.setTrainDate(req.getTrainDate());
    }
    if (req.getBudget() != null) {
      entity.setBudget(req.getBudget());
    }
    if (req.getTeacherRequire() != null) {
      entity.setTeacherRequire(req.getTeacherRequire());
    }
    if (req.getNeedExam() != null) {
      entity.setNeedExam(req.getNeedExam());
    }
    if (req.getNeedMatch() != null) {
      entity.setNeedMatch(req.getNeedMatch());
    }
    if (req.getNeedPaper() != null) {
      entity.setNeedPaper(req.getNeedPaper());
    }
    if (req.getSortNumber() != null) {
      entity.setSortNumber(req.getSortNumber());
    }
    if (req.getJsonData() != null) {
      entity.setJsonData(req.getJsonData());
    }
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainNeedEntity entity = getExisting(id);
    entity.setDeleted(1);
    mapper.deleteById(entity.getId());
  }

  private TrainNeedEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainNeedEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainNeedEntity>()
        .eq(TrainNeedEntity::getTenantId, tenantId)
        .eq(TrainNeedEntity::getId, id)
        .eq(TrainNeedEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private TrainNeedVO toVO(TrainNeedEntity entity) {
    return new TrainNeedVO(
        entity.getId(),
        entity.getOrgId(),
        entity.getOrgName(),
        entity.getTrainObject(),
        entity.getPosition(),
        entity.getNum(),
        entity.getWorkType(),
        entity.getLevel(),
        entity.getTarget(),
        entity.getMethod(),
        entity.getAddress(),
        entity.getTrainDate(),
        entity.getBudget(),
        entity.getTeacherRequire(),
        entity.getNeedExam(),
        entity.getNeedMatch(),
        entity.getNeedPaper(),
        entity.getUserId(),
        entity.getSortNumber(),
        entity.getTenantId(),
        entity.getCreateTime(),
        entity.getJsonData()
    );
  }
}
