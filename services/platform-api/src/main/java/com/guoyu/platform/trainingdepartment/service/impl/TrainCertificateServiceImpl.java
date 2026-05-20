package com.guoyu.platform.trainingdepartment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainCertificateCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainCertificateQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainCertificateUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainCertificateEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainCertificateMapper;
import com.guoyu.platform.trainingdepartment.service.TrainCertificateService;
import com.guoyu.platform.trainingdepartment.vo.TrainCertificateVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class TrainCertificateServiceImpl implements TrainCertificateService {

    private final TrainCertificateMapper mapper;

    public TrainCertificateServiceImpl(TrainCertificateMapper mapper) { this.mapper = mapper; }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TrainCertificateVO> page(TrainCertificateQueryReq req) {
        Long tenantId = SecurityContext.currentUser().getTenantId();
        LambdaQueryWrapper<TrainCertificateEntity> wrapper = new LambdaQueryWrapper<TrainCertificateEntity>()
                .eq(TrainCertificateEntity::getTenantId, tenantId)
                .eq(TrainCertificateEntity::getDeleted, 0)
                .orderByDesc(TrainCertificateEntity::getCreateTime);
        if (req.getProjectId() != null) wrapper.eq(TrainCertificateEntity::getProjectId, req.getProjectId());
        if (req.getUserId() != null) wrapper.eq(TrainCertificateEntity::getUserId, req.getUserId());
        if (req.getStatus() != null) wrapper.eq(TrainCertificateEntity::getStatus, req.getStatus());
        if (StringUtils.hasText(req.getCertificateType())) wrapper.eq(TrainCertificateEntity::getCertificateType, req.getCertificateType());
        if (StringUtils.hasText(req.getWorkType())) wrapper.eq(TrainCertificateEntity::getWorkType, req.getWorkType());
        if (StringUtils.hasText(req.getKeyword())) {
            wrapper.and(w -> w.like(TrainCertificateEntity::getUserName, req.getKeyword())
                    .or().like(TrainCertificateEntity::getCertificateNo, req.getKeyword()));
        }
        Page<TrainCertificateEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
        return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
                page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Override
    @Transactional(readOnly = true)
    public TrainCertificateVO getById(Long id) { return toVO(getExisting(id)); }

    @Override
    @Transactional
    public TrainCertificateVO create(TrainCertificateCreateReq req) {
        Long tenantId = SecurityContext.currentUser().getTenantId();
        Long userId = SecurityContext.currentUser().getUserId();
        TrainCertificateEntity entity = new TrainCertificateEntity();
        entity.setTenantId(tenantId);
        entity.setCreatedBy(userId);
        entity.setProjectId(req.getProjectId());
        entity.setUserId(req.getUserId());
        entity.setUserName(req.getUserName());
        entity.setCertificateNo(req.getCertificateNo());
        entity.setCertificateType(req.getCertificateType());
        entity.setWorkType(req.getWorkType());
        entity.setLevel(req.getLevel());
        entity.setIssueDate(req.getIssueDate());
        entity.setExpireDate(req.getExpireDate());
        entity.setIssuingAuthority(req.getIssuingAuthority());
        entity.setFilePath(req.getFilePath());
        entity.setStatus(req.getStatus());
        entity.setSortNumber(req.getSortNumber());
        entity.setDeleted(0);
        mapper.insert(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public TrainCertificateVO update(Long id, TrainCertificateUpdateReq req) {
        TrainCertificateEntity entity = getExisting(id);
        if (req.getProjectId() != null) entity.setProjectId(req.getProjectId());
        if (req.getUserId() != null) entity.setUserId(req.getUserId());
        if (req.getUserName() != null) entity.setUserName(req.getUserName());
        if (req.getCertificateNo() != null) entity.setCertificateNo(req.getCertificateNo());
        if (req.getCertificateType() != null) entity.setCertificateType(req.getCertificateType());
        if (req.getWorkType() != null) entity.setWorkType(req.getWorkType());
        if (req.getLevel() != null) entity.setLevel(req.getLevel());
        if (req.getIssueDate() != null) entity.setIssueDate(req.getIssueDate());
        if (req.getExpireDate() != null) entity.setExpireDate(req.getExpireDate());
        if (req.getIssuingAuthority() != null) entity.setIssuingAuthority(req.getIssuingAuthority());
        if (req.getFilePath() != null) entity.setFilePath(req.getFilePath());
        if (req.getStatus() != null) entity.setStatus(req.getStatus());
        if (req.getSortNumber() != null) entity.setSortNumber(req.getSortNumber());
        mapper.updateById(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        TrainCertificateEntity entity = getExisting(id);
        entity.setDeleted(1);
        mapper.updateById(entity);
    }

    @Override
    @Transactional
    public List<TrainCertificateVO> batchIssue(List<Long> studentIds, Long projectId) {
        Long tenantId = SecurityContext.currentUser().getTenantId();
        Long userId = SecurityContext.currentUser().getUserId();
        long timestamp = System.currentTimeMillis();
        List<TrainCertificateVO> results = new ArrayList<>();
        for (Long studentId : studentIds) {
            TrainCertificateEntity entity = new TrainCertificateEntity();
            entity.setTenantId(tenantId);
            entity.setCreatedBy(userId);
            entity.setProjectId(projectId);
            entity.setUserId(studentId);
            entity.setCertificateNo("CERT-" + projectId + "-" + studentId + "-" + timestamp);
            entity.setStatus(1);
            entity.setDeleted(0);
            mapper.insert(entity);
            results.add(toVO(entity));
        }
        return results;
    }

    private TrainCertificateEntity getExisting(Long id) {
        Long tenantId = SecurityContext.currentUser().getTenantId();
        TrainCertificateEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainCertificateEntity>()
                .eq(TrainCertificateEntity::getTenantId, tenantId)
                .eq(TrainCertificateEntity::getId, id)
                .eq(TrainCertificateEntity::getDeleted, 0));
        if (entity == null) throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
        return entity;
    }

    private TrainCertificateVO toVO(TrainCertificateEntity entity) {
        return new TrainCertificateVO(entity.getId(), entity.getProjectId(), entity.getUserId(),
                entity.getUserName(), entity.getCertificateNo(), entity.getCertificateType(),
                entity.getWorkType(), entity.getLevel(), entity.getIssueDate(), entity.getExpireDate(),
                entity.getIssuingAuthority(), entity.getFilePath(), entity.getStatus(),
                entity.getSortNumber(), entity.getTenantId(), entity.getCreatedBy(),
                entity.getCreateTime(), entity.getUpdateTime());
    }
}
