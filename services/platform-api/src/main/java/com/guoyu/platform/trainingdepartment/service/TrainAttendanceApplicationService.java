package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainAttendanceCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainAttendanceQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainAttendanceUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainAttendanceEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainAttendanceMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainAttendanceVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class TrainAttendanceApplicationService {

  private final TrainAttendanceMapper mapper;

  public TrainAttendanceApplicationService(TrainAttendanceMapper mapper) { this.mapper = mapper; }

  @Transactional(readOnly = true)
  public PageResponse<TrainAttendanceVO> page(TrainAttendanceQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainAttendanceEntity> wrapper = new LambdaQueryWrapper<TrainAttendanceEntity>()
        .eq(TrainAttendanceEntity::getTenantId, tenantId)
        .eq(TrainAttendanceEntity::getDeleted, 0)
        .orderByDesc(TrainAttendanceEntity::getCreateTime);
    if (req.getProjectId() != null) wrapper.eq(TrainAttendanceEntity::getProjectId, req.getProjectId());
    if (req.getClassId() != null) wrapper.eq(TrainAttendanceEntity::getClassId, req.getClassId());
    if (req.getUserId() != null) wrapper.eq(TrainAttendanceEntity::getUserId, req.getUserId());
    if (req.getStatus() != null) wrapper.eq(TrainAttendanceEntity::getStatus, req.getStatus());
    if (StringUtils.hasText(req.getAttendanceDate())) wrapper.eq(TrainAttendanceEntity::getAttendanceDate, req.getAttendanceDate());
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.like(TrainAttendanceEntity::getUserName, req.getKeyword());
    }
    Page<TrainAttendanceEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainAttendanceVO getById(Long id) { return toVO(getExisting(id)); }

  @Transactional
  public TrainAttendanceVO create(TrainAttendanceCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    TrainAttendanceEntity entity = new TrainAttendanceEntity();
    entity.setTenantId(tenantId);
    entity.setCreatedBy(userId);
    entity.setProjectId(req.getProjectId());
    entity.setClassId(req.getClassId());
    entity.setUserId(req.getUserId());
    entity.setUserName(req.getUserName());
    entity.setAttendanceDate(req.getAttendanceDate());
    entity.setStatus(req.getStatus());
    entity.setCheckInTime(req.getCheckInTime());
    entity.setCheckOutTime(req.getCheckOutTime());
    entity.setPhotoPath(req.getPhotoPath());
    entity.setVideoPath(req.getVideoPath());
    entity.setRemark(req.getRemark());
    entity.setSortNumber(req.getSortNumber());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainAttendanceVO update(Long id, TrainAttendanceUpdateReq req) {
    TrainAttendanceEntity entity = getExisting(id);
    if (req.getProjectId() != null) entity.setProjectId(req.getProjectId());
    if (req.getClassId() != null) entity.setClassId(req.getClassId());
    if (req.getUserId() != null) entity.setUserId(req.getUserId());
    if (req.getUserName() != null) entity.setUserName(req.getUserName());
    if (req.getAttendanceDate() != null) entity.setAttendanceDate(req.getAttendanceDate());
    if (req.getStatus() != null) entity.setStatus(req.getStatus());
    if (req.getCheckInTime() != null) entity.setCheckInTime(req.getCheckInTime());
    if (req.getCheckOutTime() != null) entity.setCheckOutTime(req.getCheckOutTime());
    if (req.getPhotoPath() != null) entity.setPhotoPath(req.getPhotoPath());
    if (req.getVideoPath() != null) entity.setVideoPath(req.getVideoPath());
    if (req.getRemark() != null) entity.setRemark(req.getRemark());
    if (req.getSortNumber() != null) entity.setSortNumber(req.getSortNumber());
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainAttendanceEntity entity = getExisting(id);
    entity.setDeleted(1);
    mapper.deleteById(entity.getId());
  }

  @Transactional
  public TrainAttendanceVO signIn(TrainAttendanceCreateReq req) {
    return create(req);
  }

  private TrainAttendanceEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainAttendanceEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainAttendanceEntity>()
        .eq(TrainAttendanceEntity::getTenantId, tenantId)
        .eq(TrainAttendanceEntity::getId, id)
        .eq(TrainAttendanceEntity::getDeleted, 0));
    if (entity == null) throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    return entity;
  }

  private TrainAttendanceVO toVO(TrainAttendanceEntity entity) {
    return new TrainAttendanceVO(entity.getId(), entity.getProjectId(), entity.getClassId(),
        entity.getUserId(), entity.getUserName(), entity.getAttendanceDate(), entity.getStatus(),
        entity.getCheckInTime(), entity.getCheckOutTime(), entity.getPhotoPath(),
        entity.getVideoPath(), entity.getRemark(), entity.getSortNumber(), entity.getTenantId(),
        entity.getCreatedBy(), entity.getCreateTime(), entity.getUpdateTime());
  }
}
