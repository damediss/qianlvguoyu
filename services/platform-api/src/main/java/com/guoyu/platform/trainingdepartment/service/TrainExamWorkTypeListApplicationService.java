package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainExamWorkTypeListCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamWorkTypeListQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamWorkTypeListUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainExamWorkTypeListEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamWorkTypeListMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainExamWorkTypeListVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class TrainExamWorkTypeListApplicationService {

  private final TrainExamWorkTypeListMapper mapper;

  public TrainExamWorkTypeListApplicationService(TrainExamWorkTypeListMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainExamWorkTypeListVO> page(TrainExamWorkTypeListQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainExamWorkTypeListEntity> wrapper = new LambdaQueryWrapper<TrainExamWorkTypeListEntity>()
        .eq(TrainExamWorkTypeListEntity::getTenantId, tenantId)
        .eq(TrainExamWorkTypeListEntity::getDeleted, 0)
        .orderByDesc(TrainExamWorkTypeListEntity::getId);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.like(TrainExamWorkTypeListEntity::getId, req.getKeyword());
    }
    Page<TrainExamWorkTypeListEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainExamWorkTypeListVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TrainExamWorkTypeListVO create(TrainExamWorkTypeListCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainExamWorkTypeListEntity entity = new TrainExamWorkTypeListEntity();
    entity.setTenantId(tenantId);
    entity.setExamId(req.getExamId());
    entity.setWorkTypeId(req.getWorkTypeId());
    entity.setCourseId(req.getCourseId());
    entity.setLevel(req.getLevel());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainExamWorkTypeListVO update(Long id, TrainExamWorkTypeListUpdateReq req) {
    TrainExamWorkTypeListEntity entity = getExisting(id);
    if (req.getExamId() != null) { entity.setExamId(req.getExamId()); }
    if (req.getWorkTypeId() != null) { entity.setWorkTypeId(req.getWorkTypeId()); }
    if (req.getCourseId() != null) { entity.setCourseId(req.getCourseId()); }
    if (StringUtils.hasText(req.getLevel())) { entity.setLevel(req.getLevel()); }
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainExamWorkTypeListEntity entity = getExisting(id);

    entity.setDeleted(1);
    mapper.deleteById(entity.getId());
  }

  private TrainExamWorkTypeListEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainExamWorkTypeListEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainExamWorkTypeListEntity>()

        .eq(TrainExamWorkTypeListEntity::getTenantId, tenantId)
        .eq(TrainExamWorkTypeListEntity::getId, id)
        .eq(TrainExamWorkTypeListEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private TrainExamWorkTypeListVO toVO(TrainExamWorkTypeListEntity e) {
    return new TrainExamWorkTypeListVO(
        e.getId(),
        e.getExamId(),
        e.getWorkTypeId(),
        e.getCourseId(),
        e.getLevel(),
        e.getTenantId(),
        e.getCreateTime(),
        e.getUpdateTime()
    );
  }
}
