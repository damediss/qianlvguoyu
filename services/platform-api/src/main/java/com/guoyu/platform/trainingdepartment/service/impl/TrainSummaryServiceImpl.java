package com.guoyu.platform.trainingdepartment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainSummaryCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSummaryQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSummaryUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainSummaryEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainSummaryMapper;
import com.guoyu.platform.trainingdepartment.service.TrainSummaryService;
import com.guoyu.platform.trainingdepartment.vo.TrainSummaryVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TrainSummaryServiceImpl implements TrainSummaryService {

    private final TrainSummaryMapper mapper;

    public TrainSummaryServiceImpl(TrainSummaryMapper mapper) { this.mapper = mapper; }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TrainSummaryVO> page(TrainSummaryQueryReq req) {
        Long tenantId = SecurityContext.currentUser().getTenantId();
        LambdaQueryWrapper<TrainSummaryEntity> wrapper = new LambdaQueryWrapper<TrainSummaryEntity>()
                .eq(TrainSummaryEntity::getTenantId, tenantId)
                .eq(TrainSummaryEntity::getDeleted, 0)
                .orderByDesc(TrainSummaryEntity::getCreateTime);
        if (req.getProjectId() != null) wrapper.eq(TrainSummaryEntity::getProjectId, req.getProjectId());
        if (req.getApprovalStatus() != null) wrapper.eq(TrainSummaryEntity::getApprovalStatus, req.getApprovalStatus());
        if (req.getStatus() != null) wrapper.eq(TrainSummaryEntity::getStatus, req.getStatus());
        if (StringUtils.hasText(req.getKeyword())) {
            wrapper.like(TrainSummaryEntity::getContent, req.getKeyword());
        }
        Page<TrainSummaryEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
        return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
                page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Override
    @Transactional(readOnly = true)
    public TrainSummaryVO getById(Long id) { return toVO(getExisting(id)); }

    @Override
    @Transactional
    public TrainSummaryVO create(TrainSummaryCreateReq req) {
        Long tenantId = SecurityContext.currentUser().getTenantId();
        Long userId = SecurityContext.currentUser().getUserId();
        TrainSummaryEntity entity = new TrainSummaryEntity();
        entity.setTenantId(tenantId);
        entity.setUserId(userId);
        entity.setProjectId(req.getProjectId());
        entity.setCertificationRate(req.getCertificationRate());
        entity.setSubsidySuccessRate(req.getSubsidySuccessRate());
        entity.setAchievements(req.getAchievements());
        entity.setLessonsLearned(req.getLessonsLearned());
        entity.setImprovementSuggestions(req.getImprovementSuggestions());
        entity.setContent(req.getContent());
        entity.setFilePath(req.getFilePath());
        entity.setApprovalStatus(req.getApprovalStatus());
        entity.setApproverId(req.getApproverId());
        entity.setApproverName(req.getApproverName());
        entity.setApproveTime(req.getApproveTime());
        entity.setRejectReason(req.getRejectReason());
        entity.setStatus(req.getStatus());
        entity.setSortNumber(req.getSortNumber());
        entity.setDeleted(0);
        mapper.insert(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public TrainSummaryVO update(Long id, TrainSummaryUpdateReq req) {
        TrainSummaryEntity entity = getExisting(id);
        if (req.getProjectId() != null) entity.setProjectId(req.getProjectId());
        if (req.getCertificationRate() != null) entity.setCertificationRate(req.getCertificationRate());
        if (req.getSubsidySuccessRate() != null) entity.setSubsidySuccessRate(req.getSubsidySuccessRate());
        if (req.getAchievements() != null) entity.setAchievements(req.getAchievements());
        if (req.getLessonsLearned() != null) entity.setLessonsLearned(req.getLessonsLearned());
        if (req.getImprovementSuggestions() != null) entity.setImprovementSuggestions(req.getImprovementSuggestions());
        if (req.getContent() != null) entity.setContent(req.getContent());
        if (req.getFilePath() != null) entity.setFilePath(req.getFilePath());
        if (req.getApprovalStatus() != null) entity.setApprovalStatus(req.getApprovalStatus());
        if (req.getApproverId() != null) entity.setApproverId(req.getApproverId());
        if (req.getApproverName() != null) entity.setApproverName(req.getApproverName());
        if (req.getApproveTime() != null) entity.setApproveTime(req.getApproveTime());
        if (req.getRejectReason() != null) entity.setRejectReason(req.getRejectReason());
        if (req.getStatus() != null) entity.setStatus(req.getStatus());
        if (req.getSortNumber() != null) entity.setSortNumber(req.getSortNumber());
        mapper.updateById(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        TrainSummaryEntity entity = getExisting(id);
        entity.setDeleted(1);
        mapper.updateById(entity);
    }

    private TrainSummaryEntity getExisting(Long id) {
        Long tenantId = SecurityContext.currentUser().getTenantId();
        TrainSummaryEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainSummaryEntity>()
                .eq(TrainSummaryEntity::getTenantId, tenantId)
                .eq(TrainSummaryEntity::getId, id)
                .eq(TrainSummaryEntity::getDeleted, 0));
        if (entity == null) throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
        return entity;
    }

    private TrainSummaryVO toVO(TrainSummaryEntity entity) {
        return new TrainSummaryVO(entity.getId(), entity.getProjectId(), entity.getCertificationRate(),
                entity.getSubsidySuccessRate(), entity.getAchievements(), entity.getLessonsLearned(),
                entity.getImprovementSuggestions(), entity.getContent(), entity.getFilePath(),
                entity.getApprovalStatus(), entity.getApproverId(), entity.getApproverName(),
                entity.getApproveTime(), entity.getRejectReason(), entity.getStatus(),
                entity.getSortNumber(), entity.getTenantId(), entity.getUserId(),
                entity.getCreateTime(), entity.getUpdateTime());
    }
}
