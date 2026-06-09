package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainExamShijuanCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamShijuanQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamShijuanUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainExamShijuanEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamShijuanMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainExamShijuanVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class TrainExamShijuanApplicationService {

  private final TrainExamShijuanMapper mapper;

  public TrainExamShijuanApplicationService(TrainExamShijuanMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainExamShijuanVO> page(TrainExamShijuanQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainExamShijuanEntity> wrapper = new LambdaQueryWrapper<TrainExamShijuanEntity>()
        .eq(TrainExamShijuanEntity::getTenantId, tenantId)
        .eq(TrainExamShijuanEntity::getDeleted, 0)
        .orderByDesc(TrainExamShijuanEntity::getId);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.like(TrainExamShijuanEntity::getId, req.getKeyword());
    }
    Page<TrainExamShijuanEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainExamShijuanVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TrainExamShijuanVO create(TrainExamShijuanCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainExamShijuanEntity entity = new TrainExamShijuanEntity();
    entity.setTenantId(tenantId);
    entity.setExamId(req.getExamId());
    entity.setPaperId(req.getPaperId());
    entity.setExamRoomId(req.getExamRoomId());
    entity.setOperatePaperId(req.getOperatePaperId());
    entity.setRoomId(req.getRoomId());
    entity.setUserId(req.getUserId());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainExamShijuanVO update(Long id, TrainExamShijuanUpdateReq req) {
    TrainExamShijuanEntity entity = getExisting(id);
    if (req.getExamId() != null) { entity.setExamId(req.getExamId()); }
    if (req.getPaperId() != null) { entity.setPaperId(req.getPaperId()); }
    if (req.getExamRoomId() != null) { entity.setExamRoomId(req.getExamRoomId()); }
    if (req.getOperatePaperId() != null) { entity.setOperatePaperId(req.getOperatePaperId()); }
    if (req.getRoomId() != null) { entity.setRoomId(req.getRoomId()); }
    if (req.getUserId() != null) { entity.setUserId(req.getUserId()); }
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainExamShijuanEntity entity = getExisting(id);

    entity.setDeleted(1);
    mapper.deleteById(entity.getId());
  }

  private TrainExamShijuanEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainExamShijuanEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainExamShijuanEntity>()

        .eq(TrainExamShijuanEntity::getTenantId, tenantId)
        .eq(TrainExamShijuanEntity::getId, id)
        .eq(TrainExamShijuanEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private TrainExamShijuanVO toVO(TrainExamShijuanEntity e) {
    return new TrainExamShijuanVO(
        e.getId(),
        e.getExamId(),
        e.getPaperId(),
        e.getExamRoomId(),
        e.getOperatePaperId(),
        e.getRoomId(),
        e.getUserId(),
        e.getTenantId(),
        e.getCreateTime(),
        e.getUpdateTime()
    );
  }
}
