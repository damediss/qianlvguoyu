package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.DemandEmployeeCreateReq;
import com.guoyu.platform.trainingdepartment.dto.DemandEmployeeQueryReq;
import com.guoyu.platform.trainingdepartment.dto.DemandEmployeeUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.DemandEmployeeEntity;
import com.guoyu.platform.trainingdepartment.mapper.DemandEmployeeMapper;
import com.guoyu.platform.trainingdepartment.vo.DemandEmployeeVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class DemandEmployeeApplicationService {

  private final DemandEmployeeMapper mapper;

  public DemandEmployeeApplicationService(DemandEmployeeMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<DemandEmployeeVO> page(DemandEmployeeQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<DemandEmployeeEntity> wrapper = new LambdaQueryWrapper<DemandEmployeeEntity>()
        .eq(DemandEmployeeEntity::getTenantId, tenantId)
        .eq(DemandEmployeeEntity::getDeleted, 0)
        .orderByDesc(DemandEmployeeEntity::getId);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.like(DemandEmployeeEntity::getId, req.getKeyword());
    }
    Page<DemandEmployeeEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public DemandEmployeeVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public DemandEmployeeVO create(DemandEmployeeCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    DemandEmployeeEntity entity = new DemandEmployeeEntity();
    entity.setTenantId(tenantId);
    entity.setOrgId(req.getOrgId());
    entity.setWork(req.getWork());
    entity.setEducation(req.getEducation());
    entity.setMajor(req.getMajor());
    entity.setAddtime(req.getAddtime());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public DemandEmployeeVO update(Long id, DemandEmployeeUpdateReq req) {
    DemandEmployeeEntity entity = getExisting(id);
    if (req.getOrgId() != null) { entity.setOrgId(req.getOrgId()); }
    if (StringUtils.hasText(req.getWork())) { entity.setWork(req.getWork()); }
    if (StringUtils.hasText(req.getEducation())) { entity.setEducation(req.getEducation()); }
    if (StringUtils.hasText(req.getMajor())) { entity.setMajor(req.getMajor()); }
    if (req.getAddtime() != null) { entity.setAddtime(req.getAddtime()); }
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    DemandEmployeeEntity entity = getExisting(id);

    entity.setDeleted(1);
    mapper.updateById(entity);
  }

  private DemandEmployeeEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    DemandEmployeeEntity entity = mapper.selectOne(new LambdaQueryWrapper<DemandEmployeeEntity>()

        .eq(DemandEmployeeEntity::getTenantId, tenantId)
        .eq(DemandEmployeeEntity::getId, id)
        .eq(DemandEmployeeEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private DemandEmployeeVO toVO(DemandEmployeeEntity e) {
    return new DemandEmployeeVO(
        e.getId(),
        e.getOrgId(),
        e.getWork(),
        e.getEducation(),
        e.getMajor(),
        e.getAddtime(),
        e.getTenantId(),
        e.getCreateTime(),
        e.getUpdateTime()
    );
  }
}
