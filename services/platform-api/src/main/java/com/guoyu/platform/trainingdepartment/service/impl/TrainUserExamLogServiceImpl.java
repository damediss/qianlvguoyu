package com.guoyu.platform.trainingdepartment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainUserExamLogCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserExamLogQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserExamLogUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainUserExamLogEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainUserExamLogMapper;
import com.guoyu.platform.trainingdepartment.service.TrainUserExamLogService;
import com.guoyu.platform.trainingdepartment.vo.TrainUserExamLogVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户考试日志管理服务实现
 */
@Service
public class TrainUserExamLogServiceImpl implements TrainUserExamLogService {

    private final TrainUserExamLogMapper mapper;

    public TrainUserExamLogServiceImpl(TrainUserExamLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TrainUserExamLogVO> page(TrainUserExamLogQueryReq req) {
        Long tenantId = SecurityContext.currentUser().getTenantId();
        LambdaQueryWrapper<TrainUserExamLogEntity> wrapper = new LambdaQueryWrapper<TrainUserExamLogEntity>()
                .eq(TrainUserExamLogEntity::getTenantId, tenantId)
                .eq(TrainUserExamLogEntity::getDeleted, 0)
                .orderByDesc(TrainUserExamLogEntity::getCreateTime);
        if (req.getTrainId() != null) {
            wrapper.eq(TrainUserExamLogEntity::getTrainId, req.getTrainId());
        }
        if (req.getUserId() != null) {
            wrapper.eq(TrainUserExamLogEntity::getUserId, req.getUserId());
        }
        Page<TrainUserExamLogEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
        return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
                page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Override
    @Transactional(readOnly = true)
    public TrainUserExamLogVO getById(Long id) {
        return toVO(getExisting(id));
    }

    @Override
    @Transactional
    public TrainUserExamLogVO create(TrainUserExamLogCreateReq req) {
        Long tenantId = SecurityContext.currentUser().getTenantId();
        Long userId = SecurityContext.currentUser().getUserId();
        TrainUserExamLogEntity entity = new TrainUserExamLogEntity();
        entity.setTenantId(tenantId);
        entity.setUserId(userId);
        entity.setTrainId(req.getTrainId());
        entity.setKnowledgeId(req.getKnowledgeId());
        entity.setSortNumber(req.getSortNumber());
        entity.setDeleted(0);
        mapper.insert(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public TrainUserExamLogVO update(Long id, TrainUserExamLogUpdateReq req) {
        TrainUserExamLogEntity entity = getExisting(id);
        entity.setKnowledgeId(req.getKnowledgeId());
        entity.setSortNumber(req.getSortNumber());
        mapper.updateById(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        TrainUserExamLogEntity entity = getExisting(id);
        entity.setDeleted(1);
        mapper.updateById(entity);
    }

    private TrainUserExamLogEntity getExisting(Long id) {
        Long tenantId = SecurityContext.currentUser().getTenantId();
        TrainUserExamLogEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainUserExamLogEntity>()
                .eq(TrainUserExamLogEntity::getTenantId, tenantId)
                .eq(TrainUserExamLogEntity::getId, id)
                .eq(TrainUserExamLogEntity::getDeleted, 0));
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
        }
        return entity;
    }

    private TrainUserExamLogVO toVO(TrainUserExamLogEntity entity) {
        return new TrainUserExamLogVO(
                entity.getId(),
                entity.getTrainId(),
                entity.getKnowledgeId(),
                entity.getUserId(),
                entity.getSortNumber(),
                entity.getTenantId(),
                entity.getCreateTime(),
                entity.getUpdateTime()
        );
    }
}
