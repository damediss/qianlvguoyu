package com.guoyu.platform.trainingdepartment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainDeclareCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainDeclareQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainDeclareUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainDeclareEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainDeclareMapper;
import com.guoyu.platform.trainingdepartment.service.TrainDeclareService;
import com.guoyu.platform.trainingdepartment.vo.TrainDeclareVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 申报任务管理服务实现
 */
@Service
public class TrainDeclareServiceImpl implements TrainDeclareService {

    private final TrainDeclareMapper mapper;

    public TrainDeclareServiceImpl(TrainDeclareMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TrainDeclareVO> page(TrainDeclareQueryReq req) {
        Long tenantId = SecurityContext.currentUser().getTenantId();
        LambdaQueryWrapper<TrainDeclareEntity> wrapper = new LambdaQueryWrapper<TrainDeclareEntity>()
                .eq(TrainDeclareEntity::getTenantId, tenantId)
                .eq(TrainDeclareEntity::getDeleted, 0)
                .orderByDesc(TrainDeclareEntity::getCreateTime);
        if (StringUtils.hasText(req.getKeyword())) {
            wrapper.and(w -> w
                    .like(TrainDeclareEntity::getTitle, req.getKeyword())
                    .or()
                    .like(TrainDeclareEntity::getCityName, req.getKeyword()));
        }
        if (req.getTrainId() != null) {
            wrapper.eq(TrainDeclareEntity::getTrainId, req.getTrainId());
        }
        if (req.getStatus() != null) {
            wrapper.eq(TrainDeclareEntity::getStatus, req.getStatus());
        }
        Page<TrainDeclareEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
        return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
                page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Override
    @Transactional(readOnly = true)
    public TrainDeclareVO getById(Long id) {
        return toVO(getExisting(id));
    }

    @Override
    @Transactional
    public TrainDeclareVO create(TrainDeclareCreateReq req) {
        Long tenantId = SecurityContext.currentUser().getTenantId();
        Long userId = SecurityContext.currentUser().getUserId();
        TrainDeclareEntity entity = new TrainDeclareEntity();
        entity.setTenantId(tenantId);
        entity.setUserId(userId);
        entity.setTrainId(req.getTrainId());
        entity.setTitle(req.getTitle());
        entity.setCityId(req.getCityId());
        entity.setCityName(req.getCityName());
        entity.setStatus(req.getStatus());
        entity.setSortNumber(req.getSortNumber());
        entity.setDeleted(0);
        mapper.insert(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public TrainDeclareVO update(Long id, TrainDeclareUpdateReq req) {
        TrainDeclareEntity entity = getExisting(id);
        if (req.getTrainId() != null) {
            entity.setTrainId(req.getTrainId());
        }
        if (StringUtils.hasText(req.getTitle())) {
            entity.setTitle(req.getTitle());
        }
        if (req.getCityId() != null) {
            entity.setCityId(req.getCityId());
        }
        if (StringUtils.hasText(req.getCityName())) {
            entity.setCityName(req.getCityName());
        }
        if (req.getStatus() != null) {
            entity.setStatus(req.getStatus());
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
        TrainDeclareEntity entity = getExisting(id);
        entity.setDeleted(1);
        mapper.updateById(entity);
    }

    private TrainDeclareEntity getExisting(Long id) {
        Long tenantId = SecurityContext.currentUser().getTenantId();
        TrainDeclareEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainDeclareEntity>()
                .eq(TrainDeclareEntity::getTenantId, tenantId)
                .eq(TrainDeclareEntity::getId, id)
                .eq(TrainDeclareEntity::getDeleted, 0));
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
        }
        return entity;
    }

    private TrainDeclareVO toVO(TrainDeclareEntity entity) {
        return new TrainDeclareVO(
                entity.getId(),
                entity.getTrainId(),
                entity.getTitle(),
                entity.getCityId(),
                entity.getCityName(),
                entity.getUserId(),
                entity.getStatus(),
                entity.getSortNumber(),
                entity.getTenantId(),
                entity.getCreateTime()
        );
    }
}
