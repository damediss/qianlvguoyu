package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainArchiveCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainArchiveQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainArchiveUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainArchiveEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainArchiveMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainArchiveVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class TrainArchiveApplicationService {

  private final TrainArchiveMapper mapper;

  public TrainArchiveApplicationService(TrainArchiveMapper mapper) { this.mapper = mapper; }

  @Transactional(readOnly = true)
  public PageResponse<TrainArchiveVO> page(TrainArchiveQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainArchiveEntity> wrapper = new LambdaQueryWrapper<TrainArchiveEntity>()
        .eq(TrainArchiveEntity::getTenantId, tenantId)
        .eq(TrainArchiveEntity::getDeleted, 0)
        .orderByDesc(TrainArchiveEntity::getCreateTime);
    if (req.getProjectId() != null) wrapper.eq(TrainArchiveEntity::getProjectId, req.getProjectId());
    if (req.getStatus() != null) wrapper.eq(TrainArchiveEntity::getStatus, req.getStatus());
    if (StringUtils.hasText(req.getArchiveType())) wrapper.eq(TrainArchiveEntity::getArchiveType, req.getArchiveType());
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.like(TrainArchiveEntity::getArchiveType, req.getKeyword());
    }
    Page<TrainArchiveEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainArchiveVO getById(Long id) { return toVO(getExisting(id)); }

  @Transactional
  public TrainArchiveVO create(TrainArchiveCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    TrainArchiveEntity entity = new TrainArchiveEntity();
    entity.setTenantId(tenantId);
    entity.setUserId(userId);
    entity.setProjectId(req.getProjectId());
    entity.setArchiveType(req.getArchiveType());
    entity.setFileCount(req.getFileCount());
    entity.setArchiveTime(req.getArchiveTime());
    entity.setFilePaths(req.getFilePaths());
    entity.setStatus(req.getStatus());
    entity.setApproverId(req.getApproverId());
    entity.setApproverName(req.getApproverName());
    entity.setApproveTime(req.getApproveTime());
    entity.setRejectReason(req.getRejectReason());
    entity.setRemark(req.getRemark());
    entity.setSortNumber(req.getSortNumber());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainArchiveVO update(Long id, TrainArchiveUpdateReq req) {
    TrainArchiveEntity entity = getExisting(id);
    if (req.getProjectId() != null) entity.setProjectId(req.getProjectId());
    if (req.getArchiveType() != null) entity.setArchiveType(req.getArchiveType());
    if (req.getFileCount() != null) entity.setFileCount(req.getFileCount());
    if (req.getArchiveTime() != null) entity.setArchiveTime(req.getArchiveTime());
    if (req.getFilePaths() != null) entity.setFilePaths(req.getFilePaths());
    if (req.getStatus() != null) entity.setStatus(req.getStatus());
    if (req.getApproverId() != null) entity.setApproverId(req.getApproverId());
    if (req.getApproverName() != null) entity.setApproverName(req.getApproverName());
    if (req.getApproveTime() != null) entity.setApproveTime(req.getApproveTime());
    if (req.getRejectReason() != null) entity.setRejectReason(req.getRejectReason());
    if (req.getRemark() != null) entity.setRemark(req.getRemark());
    if (req.getSortNumber() != null) entity.setSortNumber(req.getSortNumber());
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainArchiveEntity entity = getExisting(id);
    entity.setDeleted(1);
    mapper.deleteById(entity.getId());
  }

  private TrainArchiveEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainArchiveEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainArchiveEntity>()
        .eq(TrainArchiveEntity::getTenantId, tenantId)
        .eq(TrainArchiveEntity::getId, id)
        .eq(TrainArchiveEntity::getDeleted, 0));
    if (entity == null) throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    return entity;
  }

  private TrainArchiveVO toVO(TrainArchiveEntity entity) {
    return new TrainArchiveVO(entity.getId(), entity.getProjectId(), entity.getArchiveType(),
        entity.getFileCount(), entity.getArchiveTime(), entity.getFilePaths(), entity.getStatus(),
        entity.getApproverId(), entity.getApproverName(), entity.getApproveTime(),
        entity.getRejectReason(), entity.getRemark(), entity.getSortNumber(),
        entity.getTenantId(), entity.getUserId(), entity.getCreateTime(), entity.getUpdateTime());
  }
}
