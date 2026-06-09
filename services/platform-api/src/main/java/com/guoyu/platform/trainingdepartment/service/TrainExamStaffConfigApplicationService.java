package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainExamStaffConfigCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamStaffConfigQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamStaffConfigUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainExamStaffConfigEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamStaffConfigMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainExamStaffConfigVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class TrainExamStaffConfigApplicationService {

  private final TrainExamStaffConfigMapper mapper;

  public TrainExamStaffConfigApplicationService(TrainExamStaffConfigMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainExamStaffConfigVO> page(TrainExamStaffConfigQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainExamStaffConfigEntity> wrapper = new LambdaQueryWrapper<TrainExamStaffConfigEntity>()
        .eq(TrainExamStaffConfigEntity::getTenantId, tenantId)
        .eq(TrainExamStaffConfigEntity::getDeleted, 0)
        .orderByDesc(TrainExamStaffConfigEntity::getId);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.like(TrainExamStaffConfigEntity::getId, req.getKeyword());
    }
    Page<TrainExamStaffConfigEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainExamStaffConfigVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TrainExamStaffConfigVO create(TrainExamStaffConfigCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainExamStaffConfigEntity entity = new TrainExamStaffConfigEntity();
    entity.setTenantId(tenantId);
    entity.setType(req.getType());
    entity.setRealname(req.getRealname());
    entity.setUserId(req.getUserId());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainExamStaffConfigVO update(Long id, TrainExamStaffConfigUpdateReq req) {
    TrainExamStaffConfigEntity entity = getExisting(id);
    if (StringUtils.hasText(req.getType())) { entity.setType(req.getType()); }
    if (StringUtils.hasText(req.getRealname())) { entity.setRealname(req.getRealname()); }
    if (req.getUserId() != null) { entity.setUserId(req.getUserId()); }
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainExamStaffConfigEntity entity = getExisting(id);

    entity.setDeleted(1);
    mapper.deleteById(entity.getId());
  }

  private TrainExamStaffConfigEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainExamStaffConfigEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainExamStaffConfigEntity>()

        .eq(TrainExamStaffConfigEntity::getTenantId, tenantId)
        .eq(TrainExamStaffConfigEntity::getId, id)
        .eq(TrainExamStaffConfigEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private TrainExamStaffConfigVO toVO(TrainExamStaffConfigEntity e) {
    return new TrainExamStaffConfigVO(
        e.getId(),
        e.getType(),
        e.getRealname(),
        e.getUserId(),
        e.getTenantId(),
        e.getCreateTime(),
        e.getUpdateTime()
    );
  }
}
