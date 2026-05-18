package com.guoyu.platform.trainingdepartment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainUserAbilityCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserAbilityQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserAbilityUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainUserAbilityEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainUserAbilityMapper;
import com.guoyu.platform.trainingdepartment.service.TrainUserAbilityService;
import com.guoyu.platform.trainingdepartment.vo.TrainUserAbilityVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户能力管理服务实现
 */
@Service
public class TrainUserAbilityServiceImpl implements TrainUserAbilityService {

    private final TrainUserAbilityMapper mapper;

    public TrainUserAbilityServiceImpl(TrainUserAbilityMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TrainUserAbilityVO> page(TrainUserAbilityQueryReq req) {
        Long tenantId = SecurityContext.currentUser().getTenantId();
        LambdaQueryWrapper<TrainUserAbilityEntity> wrapper = new LambdaQueryWrapper<TrainUserAbilityEntity>()
                .eq(TrainUserAbilityEntity::getTenantId, tenantId)
                .eq(TrainUserAbilityEntity::getDeleted, 0)
                .orderByDesc(TrainUserAbilityEntity::getCreateTime);
        if (req.getTrainId() != null) {
            wrapper.eq(TrainUserAbilityEntity::getTrainId, req.getTrainId());
        }
        if (req.getUserId() != null) {
            wrapper.eq(TrainUserAbilityEntity::getUserId, req.getUserId());
        }
        if (req.getCourseId() != null) {
            wrapper.eq(TrainUserAbilityEntity::getCourseId, req.getCourseId());
        }
        Page<TrainUserAbilityEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
        return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
                page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Override
    @Transactional(readOnly = true)
    public TrainUserAbilityVO getById(Long id) {
        return toVO(getExisting(id));
    }

    @Override
    @Transactional
    public TrainUserAbilityVO create(TrainUserAbilityCreateReq req) {
        Long tenantId = SecurityContext.currentUser().getTenantId();
        Long userId = SecurityContext.currentUser().getUserId();
        TrainUserAbilityEntity entity = new TrainUserAbilityEntity();
        entity.setTenantId(tenantId);
        entity.setUserId(userId);
        entity.setTrainId(req.getTrainId());
        entity.setCourseId(req.getCourseId());
        entity.setKnowledgeId(req.getKnowledgeId());
        entity.setResult(req.getResult());
        entity.setConfigId(req.getConfigId());
        entity.setScore(req.getScore());
        entity.setCurrentScore(req.getCurrentScore());
        entity.setPlatformUserId(req.getPlatformUserId());
        entity.setComments(req.getComments());
        entity.setStatus(req.getStatus());
        entity.setSortNumber(req.getSortNumber());
        entity.setDeleted(0);
        mapper.insert(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public TrainUserAbilityVO update(Long id, TrainUserAbilityUpdateReq req) {
        TrainUserAbilityEntity entity = getExisting(id);
        if (req.getResult() != null) {
            entity.setResult(req.getResult());
        }
        if (req.getScore() != null) {
            entity.setScore(req.getScore());
        }
        if (req.getCurrentScore() != null) {
            entity.setCurrentScore(req.getCurrentScore());
        }
        if (req.getComments() != null) {
            entity.setComments(req.getComments());
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
        TrainUserAbilityEntity entity = getExisting(id);
        entity.setDeleted(1);
        mapper.updateById(entity);
    }

    private TrainUserAbilityEntity getExisting(Long id) {
        Long tenantId = SecurityContext.currentUser().getTenantId();
        TrainUserAbilityEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainUserAbilityEntity>()
                .eq(TrainUserAbilityEntity::getTenantId, tenantId)
                .eq(TrainUserAbilityEntity::getId, id)
                .eq(TrainUserAbilityEntity::getDeleted, 0));
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
        }
        return entity;
    }

    private TrainUserAbilityVO toVO(TrainUserAbilityEntity entity) {
        return new TrainUserAbilityVO(
                entity.getId(),
                entity.getTrainId(),
                entity.getCourseId(),
                entity.getKnowledgeId(),
                entity.getResult(),
                entity.getConfigId(),
                entity.getScore(),
                entity.getCurrentScore(),
                entity.getUserId(),
                entity.getPlatformUserId(),
                entity.getComments(),
                entity.getStatus(),
                entity.getSortNumber(),
                entity.getTenantId(),
                entity.getCreateTime()
        );
    }
}
