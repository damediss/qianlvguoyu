package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainExamStaffCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamStaffQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamStaffUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainExamStaffEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamStaffMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainExamStaffVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class TrainExamStaffApplicationService {

  private final TrainExamStaffMapper mapper;

  public TrainExamStaffApplicationService(TrainExamStaffMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainExamStaffVO> page(TrainExamStaffQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainExamStaffEntity> wrapper = new LambdaQueryWrapper<TrainExamStaffEntity>()
        .eq(TrainExamStaffEntity::getTenantId, tenantId)
        .eq(TrainExamStaffEntity::getDeleted, 0)
        .orderByDesc(TrainExamStaffEntity::getId);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.like(TrainExamStaffEntity::getId, req.getKeyword());
    }
    Page<TrainExamStaffEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainExamStaffVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TrainExamStaffVO create(TrainExamStaffCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainExamStaffEntity entity = new TrainExamStaffEntity();
    entity.setTenantId(tenantId);
    entity.setExamId(req.getExamId());
    entity.setExamRoomId(req.getExamRoomId());
    entity.setRoomId(req.getRoomId());
    entity.setUserId(req.getUserId());
    entity.setRealname(req.getRealname());
    entity.setType(req.getType());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainExamStaffVO update(Long id, TrainExamStaffUpdateReq req) {
    TrainExamStaffEntity entity = getExisting(id);
    if (req.getExamId() != null) { entity.setExamId(req.getExamId()); }
    if (req.getExamRoomId() != null) { entity.setExamRoomId(req.getExamRoomId()); }
    if (req.getRoomId() != null) { entity.setRoomId(req.getRoomId()); }
    if (req.getUserId() != null) { entity.setUserId(req.getUserId()); }
    if (StringUtils.hasText(req.getRealname())) { entity.setRealname(req.getRealname()); }
    if (StringUtils.hasText(req.getType())) { entity.setType(req.getType()); }
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainExamStaffEntity entity = getExisting(id);

    entity.setDeleted(1);
    mapper.deleteById(entity.getId());
  }

  private TrainExamStaffEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainExamStaffEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainExamStaffEntity>()

        .eq(TrainExamStaffEntity::getTenantId, tenantId)
        .eq(TrainExamStaffEntity::getId, id)
        .eq(TrainExamStaffEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private TrainExamStaffVO toVO(TrainExamStaffEntity e) {
    return new TrainExamStaffVO(
        e.getId(),
        e.getExamId(),
        e.getExamRoomId(),
        e.getRoomId(),
        e.getUserId(),
        e.getRealname(),
        e.getType(),
        e.getTenantId(),
        e.getCreateTime(),
        e.getUpdateTime()
    );
  }
}
