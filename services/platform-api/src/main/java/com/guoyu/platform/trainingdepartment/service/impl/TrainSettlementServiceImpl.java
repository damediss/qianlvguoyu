package com.guoyu.platform.trainingdepartment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainSettlementCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSettlementQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSettlementUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainSettlementEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainSettlementMapper;
import com.guoyu.platform.trainingdepartment.service.TrainSettlementService;
import com.guoyu.platform.trainingdepartment.vo.TrainSettlementVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TrainSettlementServiceImpl implements TrainSettlementService {

    private final TrainSettlementMapper mapper;

    public TrainSettlementServiceImpl(TrainSettlementMapper mapper) { this.mapper = mapper; }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TrainSettlementVO> page(TrainSettlementQueryReq req) {
        Long tenantId = SecurityContext.currentUser().getTenantId();
        LambdaQueryWrapper<TrainSettlementEntity> wrapper = new LambdaQueryWrapper<TrainSettlementEntity>()
                .eq(TrainSettlementEntity::getTenantId, tenantId)
                .eq(TrainSettlementEntity::getDeleted, 0)
                .orderByDesc(TrainSettlementEntity::getCreateTime);
        if (req.getProjectId() != null) wrapper.eq(TrainSettlementEntity::getProjectId, req.getProjectId());
        if (req.getApprovalStatus() != null) wrapper.eq(TrainSettlementEntity::getApprovalStatus, req.getApprovalStatus());
        if (req.getStatus() != null) wrapper.eq(TrainSettlementEntity::getStatus, req.getStatus());
        if (StringUtils.hasText(req.getKeyword())) {
            wrapper.like(TrainSettlementEntity::getShareRatio, req.getKeyword());
        }
        Page<TrainSettlementEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
        return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
                page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Override
    @Transactional(readOnly = true)
    public TrainSettlementVO getById(Long id) { return toVO(getExisting(id)); }

    @Override
    @Transactional
    public TrainSettlementVO create(TrainSettlementCreateReq req) {
        Long tenantId = SecurityContext.currentUser().getTenantId();
        Long userId = SecurityContext.currentUser().getUserId();
        TrainSettlementEntity entity = new TrainSettlementEntity();
        entity.setTenantId(tenantId);
        entity.setUserId(userId);
        entity.setProjectId(req.getProjectId());
        entity.setSettlementAmount(req.getSettlementAmount());
        entity.setInstitutionShare(req.getInstitutionShare());
        entity.setEnterpriseShare(req.getEnterpriseShare());
        entity.setThirdPartyShare(req.getThirdPartyShare());
        entity.setShareRatio(req.getShareRatio());
        entity.setApprovalStatus(req.getApprovalStatus());
        entity.setApproverId(req.getApproverId());
        entity.setApproverName(req.getApproverName());
        entity.setApproveTime(req.getApproveTime());
        entity.setRejectReason(req.getRejectReason());
        entity.setRemark(req.getRemark());
        entity.setStatus(req.getStatus());
        entity.setSortNumber(req.getSortNumber());
        entity.setDeleted(0);
        mapper.insert(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public TrainSettlementVO update(Long id, TrainSettlementUpdateReq req) {
        TrainSettlementEntity entity = getExisting(id);
        if (req.getProjectId() != null) entity.setProjectId(req.getProjectId());
        if (req.getSettlementAmount() != null) entity.setSettlementAmount(req.getSettlementAmount());
        if (req.getInstitutionShare() != null) entity.setInstitutionShare(req.getInstitutionShare());
        if (req.getEnterpriseShare() != null) entity.setEnterpriseShare(req.getEnterpriseShare());
        if (req.getThirdPartyShare() != null) entity.setThirdPartyShare(req.getThirdPartyShare());
        if (req.getShareRatio() != null) entity.setShareRatio(req.getShareRatio());
        if (req.getApprovalStatus() != null) entity.setApprovalStatus(req.getApprovalStatus());
        if (req.getApproverId() != null) entity.setApproverId(req.getApproverId());
        if (req.getApproverName() != null) entity.setApproverName(req.getApproverName());
        if (req.getApproveTime() != null) entity.setApproveTime(req.getApproveTime());
        if (req.getRejectReason() != null) entity.setRejectReason(req.getRejectReason());
        if (req.getRemark() != null) entity.setRemark(req.getRemark());
        if (req.getStatus() != null) entity.setStatus(req.getStatus());
        if (req.getSortNumber() != null) entity.setSortNumber(req.getSortNumber());
        mapper.updateById(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        TrainSettlementEntity entity = getExisting(id);
        entity.setDeleted(1);
        mapper.updateById(entity);
    }

    @Override
    @Transactional
    public TrainSettlementVO submit(Long id) {
        TrainSettlementEntity entity = getExisting(id);
        if (entity.getApprovalStatus() == null || entity.getApprovalStatus() != 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "只有草稿状态可以提交");
        }
        entity.setApprovalStatus(1);
        mapper.updateById(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public TrainSettlementVO approve(Long id) {
        TrainSettlementEntity entity = getExisting(id);
        if (entity.getApprovalStatus() == null || entity.getApprovalStatus() != 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "只有已提交状态可以审批通过");
        }
        entity.setApprovalStatus(2);
        Long userId = SecurityContext.currentUser().getUserId();
        entity.setApproverId(userId);
        mapper.updateById(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public TrainSettlementVO reject(Long id, String reason) {
        TrainSettlementEntity entity = getExisting(id);
        if (entity.getApprovalStatus() == null || entity.getApprovalStatus() != 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "只有已提交状态可以驳回");
        }
        entity.setApprovalStatus(0);
        entity.setRejectReason(reason);
        mapper.updateById(entity);
        return toVO(entity);
    }

    private TrainSettlementEntity getExisting(Long id) {
        Long tenantId = SecurityContext.currentUser().getTenantId();
        TrainSettlementEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainSettlementEntity>()
                .eq(TrainSettlementEntity::getTenantId, tenantId)
                .eq(TrainSettlementEntity::getId, id)
                .eq(TrainSettlementEntity::getDeleted, 0));
        if (entity == null) throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
        return entity;
    }

    private TrainSettlementVO toVO(TrainSettlementEntity entity) {
        return new TrainSettlementVO(entity.getId(), entity.getProjectId(), entity.getSettlementAmount(),
                entity.getInstitutionShare(), entity.getEnterpriseShare(), entity.getThirdPartyShare(),
                entity.getShareRatio(), entity.getApprovalStatus(), entity.getApproverId(),
                entity.getApproverName(), entity.getApproveTime(), entity.getRejectReason(),
                entity.getRemark(), entity.getStatus(), entity.getSortNumber(),
                entity.getTenantId(), entity.getUserId(), entity.getCreateTime(), entity.getUpdateTime());
    }
}
