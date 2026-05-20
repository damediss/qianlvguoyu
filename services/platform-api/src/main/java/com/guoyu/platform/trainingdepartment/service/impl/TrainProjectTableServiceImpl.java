package com.guoyu.platform.trainingdepartment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainProjectTableCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainProjectTableQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainProjectTableUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainProjectTableEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainProjectTableMapper;
import com.guoyu.platform.trainingdepartment.service.TrainProjectTableService;
import com.guoyu.platform.trainingdepartment.vo.TrainProjectTableVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TrainProjectTableServiceImpl implements TrainProjectTableService {

    private final TrainProjectTableMapper mapper;

    public TrainProjectTableServiceImpl(TrainProjectTableMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TrainProjectTableVO> page(TrainProjectTableQueryReq req) {
        Long tenantId = SecurityContext.currentUser().getTenantId();
        LambdaQueryWrapper<TrainProjectTableEntity> wrapper = new LambdaQueryWrapper<TrainProjectTableEntity>()
                .eq(TrainProjectTableEntity::getTenantId, tenantId)
                .eq(TrainProjectTableEntity::getDeleted, 0)
                .orderByDesc(TrainProjectTableEntity::getCreateTime);
        if (StringUtils.hasText(req.getKeyword())) {
            wrapper.and(w -> w
                    .like(TrainProjectTableEntity::getTableName, req.getKeyword())
                    .or()
                    .like(TrainProjectTableEntity::getContent, req.getKeyword()));
        }
        if (req.getProjectId() != null) {
            wrapper.eq(TrainProjectTableEntity::getProjectId, req.getProjectId());
        }
        if (req.getTableType() != null) {
            wrapper.eq(TrainProjectTableEntity::getTableType, req.getTableType());
        }
        if (req.getStatus() != null) {
            wrapper.eq(TrainProjectTableEntity::getStatus, req.getStatus());
        }
        Page<TrainProjectTableEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
        return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
                page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Override
    @Transactional(readOnly = true)
    public TrainProjectTableVO getById(Long id) {
        return toVO(getExisting(id));
    }

    @Override
    @Transactional
    public TrainProjectTableVO create(TrainProjectTableCreateReq req) {
        Long tenantId = SecurityContext.currentUser().getTenantId();
        Long userId = SecurityContext.currentUser().getUserId();
        TrainProjectTableEntity entity = new TrainProjectTableEntity();
        entity.setTenantId(tenantId);
        entity.setUserId(userId);
        entity.setProjectId(req.getProjectId());
        entity.setTableType(req.getTableType());
        entity.setTableName(req.getTableName());
        entity.setContent(req.getContent());
        entity.setVersion(req.getVersion());
        entity.setStatus(req.getStatus());
        entity.setOwnerId(req.getOwnerId());
        entity.setOwnerName(req.getOwnerName());
        entity.setSortNumber(req.getSortNumber());
        entity.setDeleted(0);
        mapper.insert(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public TrainProjectTableVO update(Long id, TrainProjectTableUpdateReq req) {
        TrainProjectTableEntity entity = getExisting(id);
        if (req.getProjectId() != null) {
            entity.setProjectId(req.getProjectId());
        }
        if (req.getTableType() != null) {
            entity.setTableType(req.getTableType());
        }
        if (req.getTableName() != null) {
            entity.setTableName(req.getTableName());
        }
        if (req.getContent() != null) {
            entity.setContent(req.getContent());
        }
        if (req.getVersion() != null) {
            entity.setVersion(req.getVersion());
        }
        if (req.getStatus() != null) {
            entity.setStatus(req.getStatus());
        }
        if (req.getOwnerId() != null) {
            entity.setOwnerId(req.getOwnerId());
        }
        if (req.getOwnerName() != null) {
            entity.setOwnerName(req.getOwnerName());
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
        TrainProjectTableEntity entity = getExisting(id);
        entity.setDeleted(1);
        mapper.updateById(entity);
    }

    private TrainProjectTableEntity getExisting(Long id) {
        Long tenantId = SecurityContext.currentUser().getTenantId();
        TrainProjectTableEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainProjectTableEntity>()
                .eq(TrainProjectTableEntity::getTenantId, tenantId)
                .eq(TrainProjectTableEntity::getId, id)
                .eq(TrainProjectTableEntity::getDeleted, 0));
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
        }
        return entity;
    }

    private TrainProjectTableVO toVO(TrainProjectTableEntity entity) {
        return new TrainProjectTableVO(
                entity.getId(),
                entity.getProjectId(),
                entity.getTableType(),
                entity.getTableName(),
                entity.getContent(),
                entity.getVersion(),
                entity.getStatus(),
                entity.getFileId(),
                entity.getOwnerId(),
                entity.getOwnerName(),
                entity.getSortNumber(),
                entity.getTenantId(),
                entity.getUserId(),
                entity.getCreateTime(),
                entity.getUpdateTime()
        );
    }
}
