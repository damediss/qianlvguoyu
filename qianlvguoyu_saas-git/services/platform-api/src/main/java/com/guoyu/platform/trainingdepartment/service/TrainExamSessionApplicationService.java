package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainExamSessionCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamSessionQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamSessionUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainExamSessionEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamSessionMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainExamSessionVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class TrainExamSessionApplicationService {

  private final TrainExamSessionMapper mapper;

  public TrainExamSessionApplicationService(TrainExamSessionMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainExamSessionVO> page(TrainExamSessionQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainExamSessionEntity> wrapper = new LambdaQueryWrapper<TrainExamSessionEntity>()
        .eq(TrainExamSessionEntity::getTenantId, tenantId)
        .eq(TrainExamSessionEntity::getDeleted, 0)
        .orderByDesc(TrainExamSessionEntity::getId);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.like(TrainExamSessionEntity::getId, req.getKeyword());
    }
    Page<TrainExamSessionEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainExamSessionVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TrainExamSessionVO create(TrainExamSessionCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainExamSessionEntity entity = new TrainExamSessionEntity();
    entity.setTenantId(tenantId);
    entity.setExamId(req.getExamId());
    entity.setCourseId(req.getCourseId());
    entity.setUserId(req.getUserId());
    entity.setStartTime(req.getStartTime());
    entity.setEndTime(req.getEndTime());
    entity.setCourseName(req.getCourseName());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainExamSessionVO update(Long id, TrainExamSessionUpdateReq req) {
    TrainExamSessionEntity entity = getExisting(id);
    if (req.getExamId() != null) { entity.setExamId(req.getExamId()); }
    if (req.getCourseId() != null) { entity.setCourseId(req.getCourseId()); }
    if (req.getUserId() != null) { entity.setUserId(req.getUserId()); }
    if (StringUtils.hasText(req.getStartTime())) { entity.setStartTime(req.getStartTime()); }
    if (StringUtils.hasText(req.getEndTime())) { entity.setEndTime(req.getEndTime()); }
    if (StringUtils.hasText(req.getCourseName())) { entity.setCourseName(req.getCourseName()); }
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainExamSessionEntity entity = getExisting(id);

    entity.setDeleted(1);
    mapper.deleteById(entity.getId());
  }

  private TrainExamSessionEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainExamSessionEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainExamSessionEntity>()

        .eq(TrainExamSessionEntity::getTenantId, tenantId)
        .eq(TrainExamSessionEntity::getId, id)
        .eq(TrainExamSessionEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private TrainExamSessionVO toVO(TrainExamSessionEntity e) {
    return new TrainExamSessionVO(
        e.getId(),
        e.getExamId(),
        e.getCourseId(),
        e.getUserId(),
        e.getStartTime(),
        e.getEndTime(),
        e.getCourseName(),
        e.getTenantId(),
        e.getCreateTime(),
        e.getUpdateTime()
    );
  }
}
