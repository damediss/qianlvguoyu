package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainSubsidyCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSubsidyQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSubsidyUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainSubsidyEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainSubsidyMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainSubsidyVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class TrainSubsidyApplicationService {

  private final TrainSubsidyMapper mapper;

  public TrainSubsidyApplicationService(TrainSubsidyMapper mapper) { this.mapper = mapper; }

  @Transactional(readOnly = true)
  public PageResponse<TrainSubsidyVO> page(TrainSubsidyQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainSubsidyEntity> wrapper = new LambdaQueryWrapper<TrainSubsidyEntity>()
        .eq(TrainSubsidyEntity::getTenantId, tenantId)
        .eq(TrainSubsidyEntity::getDeleted, 0)
        .orderByDesc(TrainSubsidyEntity::getCreateTime);
    if (req.getProjectId() != null) wrapper.eq(TrainSubsidyEntity::getProjectId, req.getProjectId());
    if (req.getMaterialStatus() != null) wrapper.eq(TrainSubsidyEntity::getMaterialStatus, req.getMaterialStatus());
    if (req.getRssbStatus() != null) wrapper.eq(TrainSubsidyEntity::getRssbStatus, req.getRssbStatus());
    if (req.getStatus() != null) wrapper.eq(TrainSubsidyEntity::getStatus, req.getStatus());
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.like(TrainSubsidyEntity::getPolicyTemplate, req.getKeyword());
    }
    Page<TrainSubsidyEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainSubsidyVO getById(Long id) { return toVO(getExisting(id)); }

  @Transactional
  public TrainSubsidyVO create(TrainSubsidyCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    TrainSubsidyEntity entity = new TrainSubsidyEntity();
    entity.setTenantId(tenantId);
    entity.setUserId(userId);
    entity.setProjectId(req.getProjectId());
    entity.setPolicyTemplate(req.getPolicyTemplate());
    entity.setSubsidyAmount(req.getSubsidyAmount());
    entity.setStudentCount(req.getStudentCount());
    entity.setMaterialPackagePath(req.getMaterialPackagePath());
    entity.setMaterialStatus(req.getMaterialStatus());
    entity.setRssbStatus(req.getRssbStatus());
    entity.setRssbSubmitTime(req.getRssbSubmitTime());
    entity.setRssbReceiptNo(req.getRssbReceiptNo());
    entity.setRssbRejectReason(req.getRssbRejectReason());
    entity.setGrantedAmount(req.getGrantedAmount());
    entity.setGrantTime(req.getGrantTime());
    entity.setRemark(req.getRemark());
    entity.setStatus(req.getStatus());
    entity.setSortNumber(req.getSortNumber());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainSubsidyVO update(Long id, TrainSubsidyUpdateReq req) {
    TrainSubsidyEntity entity = getExisting(id);
    if (req.getProjectId() != null) entity.setProjectId(req.getProjectId());
    if (req.getPolicyTemplate() != null) entity.setPolicyTemplate(req.getPolicyTemplate());
    if (req.getSubsidyAmount() != null) entity.setSubsidyAmount(req.getSubsidyAmount());
    if (req.getStudentCount() != null) entity.setStudentCount(req.getStudentCount());
    if (req.getMaterialPackagePath() != null) entity.setMaterialPackagePath(req.getMaterialPackagePath());
    if (req.getMaterialStatus() != null) entity.setMaterialStatus(req.getMaterialStatus());
    if (req.getRssbStatus() != null) entity.setRssbStatus(req.getRssbStatus());
    if (req.getRssbSubmitTime() != null) entity.setRssbSubmitTime(req.getRssbSubmitTime());
    if (req.getRssbReceiptNo() != null) entity.setRssbReceiptNo(req.getRssbReceiptNo());
    if (req.getRssbRejectReason() != null) entity.setRssbRejectReason(req.getRssbRejectReason());
    if (req.getGrantedAmount() != null) entity.setGrantedAmount(req.getGrantedAmount());
    if (req.getGrantTime() != null) entity.setGrantTime(req.getGrantTime());
    if (req.getRemark() != null) entity.setRemark(req.getRemark());
    if (req.getStatus() != null) entity.setStatus(req.getStatus());
    if (req.getSortNumber() != null) entity.setSortNumber(req.getSortNumber());
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainSubsidyEntity entity = getExisting(id);
    entity.setDeleted(1);
    mapper.updateById(entity);
  }

  @Transactional
  public TrainSubsidyVO submit(Long id) {
    TrainSubsidyEntity entity = getExisting(id);
    if (entity.getStatus() == null || entity.getStatus() != 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "只有草稿状态可以提交");
    }
    entity.setStatus(1);
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainSubsidyVO approve(Long id) {
    TrainSubsidyEntity entity = getExisting(id);
    if (entity.getStatus() == null || entity.getStatus() != 1) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "只有已提交状态可以审批通过");
    }
    entity.setStatus(2);
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainSubsidyVO reject(Long id, String reason) {
    TrainSubsidyEntity entity = getExisting(id);
    if (entity.getStatus() == null || entity.getStatus() != 1) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "只有已提交状态可以驳回");
    }
    entity.setStatus(0);
    entity.setRssbRejectReason(reason);
    mapper.updateById(entity);
    return toVO(entity);
  }

  private TrainSubsidyEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainSubsidyEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainSubsidyEntity>()
        .eq(TrainSubsidyEntity::getTenantId, tenantId)
        .eq(TrainSubsidyEntity::getId, id)
        .eq(TrainSubsidyEntity::getDeleted, 0));
    if (entity == null) throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    return entity;
  }

  private TrainSubsidyVO toVO(TrainSubsidyEntity entity) {
    return new TrainSubsidyVO(entity.getId(), entity.getProjectId(), entity.getPolicyTemplate(),
        entity.getSubsidyAmount(), entity.getStudentCount(), entity.getMaterialPackagePath(),
        entity.getMaterialStatus(), entity.getRssbStatus(), entity.getRssbSubmitTime(),
        entity.getRssbReceiptNo(), entity.getRssbRejectReason(), entity.getGrantedAmount(),
        entity.getGrantTime(), entity.getRemark(), entity.getStatus(), entity.getSortNumber(),
        entity.getTenantId(), entity.getUserId(), entity.getCreateTime(), entity.getUpdateTime());
  }
}
