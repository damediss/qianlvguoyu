package com.guoyu.platform.trainingdepartment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainProjectCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainProjectQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainProjectUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainProjectEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainProjectMapper;
import com.guoyu.platform.trainingdepartment.service.TrainProjectService;
import com.guoyu.platform.trainingdepartment.vo.TrainProjectVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TrainProjectServiceImpl implements TrainProjectService {

    private final TrainProjectMapper mapper;

    public TrainProjectServiceImpl(TrainProjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TrainProjectVO> page(TrainProjectQueryReq req) {
        Long tenantId = SecurityContext.currentUser().getTenantId();
        LambdaQueryWrapper<TrainProjectEntity> wrapper = new LambdaQueryWrapper<TrainProjectEntity>()
                .eq(TrainProjectEntity::getTenantId, tenantId)
                .eq(TrainProjectEntity::getDeleted, 0)
                .orderByDesc(TrainProjectEntity::getCreatedAt);
        if (StringUtils.hasText(req.getKeyword())) {
            wrapper.and(w -> w
                    .like(TrainProjectEntity::getProjectName, req.getKeyword())
                    .or()
                    .like(TrainProjectEntity::getCompanyName, req.getKeyword()));
        }
        if (req.getCompanyId() != null) {
            wrapper.eq(TrainProjectEntity::getCompanyId, req.getCompanyId());
        }
        if (req.getStatus() != null) {
            wrapper.eq(TrainProjectEntity::getStatus, req.getStatus());
        }
        Page<TrainProjectEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
        return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
                page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Override
    @Transactional(readOnly = true)
    public TrainProjectVO getById(Long id) {
        return toVO(getExisting(id));
    }

    @Override
    @Transactional
    public TrainProjectVO create(TrainProjectCreateReq req) {
        TrainProjectEntity entity = new TrainProjectEntity();
        entity.setProjectName(req.getProjectName());
        entity.setCompanyId(req.getCompanyId());
        entity.setCompanyName(req.getCompanyName());
        entity.setManagerId(req.getManagerId());
        entity.setManagerName(req.getManagerName());
        entity.setPlanStart(req.getPlanStart());
        entity.setPlanEnd(req.getPlanEnd());
        entity.setCurrentStage(req.getCurrentStage());
        entity.setProgress(req.getProgress());
        entity.setStatus(req.getStatus());
        entity.setDescription(req.getDescription());
        entity.setSortNumber(req.getSortNumber());
        entity.setDeleted(0);
        mapper.insert(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public TrainProjectVO update(Long id, TrainProjectUpdateReq req) {
        TrainProjectEntity entity = getExisting(id);
        if (req.getProjectName() != null) {
            entity.setProjectName(req.getProjectName());
        }
        if (req.getCompanyId() != null) {
            entity.setCompanyId(req.getCompanyId());
        }
        if (req.getCompanyName() != null) {
            entity.setCompanyName(req.getCompanyName());
        }
        if (req.getManagerId() != null) {
            entity.setManagerId(req.getManagerId());
        }
        if (req.getManagerName() != null) {
            entity.setManagerName(req.getManagerName());
        }
        if (req.getPlanStart() != null) {
            entity.setPlanStart(req.getPlanStart());
        }
        if (req.getPlanEnd() != null) {
            entity.setPlanEnd(req.getPlanEnd());
        }
        if (req.getCurrentStage() != null) {
            entity.setCurrentStage(req.getCurrentStage());
        }
        if (req.getProgress() != null) {
            entity.setProgress(req.getProgress());
        }
        if (req.getStatus() != null) {
            entity.setStatus(req.getStatus());
        }
        if (req.getDescription() != null) {
            entity.setDescription(req.getDescription());
        }
        if (req.getSortNumber() != null) {
            entity.setSortNumber(req.getSortNumber());
        }
        mapper.updateById(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        TrainProjectEntity entity = getExisting(id);
        entity.setDeleted(1);
        mapper.updateById(entity);
    }

    private TrainProjectEntity getExisting(Long id) {
        Long tenantId = SecurityContext.currentUser().getTenantId();
        TrainProjectEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainProjectEntity>()
                .eq(TrainProjectEntity::getTenantId, tenantId)
                .eq(TrainProjectEntity::getId, id)
                .eq(TrainProjectEntity::getDeleted, 0));
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
        }
        return entity;
    }

    private TrainProjectVO toVO(TrainProjectEntity entity) {
        return new TrainProjectVO(
                entity.getId(),
                entity.getProjectName(),
                entity.getCompanyId(),
                entity.getCompanyName(),
                entity.getManagerId(),
                entity.getManagerName(),
                entity.getPlanStart(),
                entity.getPlanEnd(),
                entity.getCurrentStage(),
                entity.getProgress(),
                entity.getStatus(),
                entity.getDescription(),
                entity.getSortNumber(),
                entity.getTenantId(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
