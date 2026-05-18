package com.guoyu.platform.trainingdepartment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskLogCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskLogQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainTaskLogUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainTaskLogEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainTaskLogMapper;
import com.guoyu.platform.trainingdepartment.service.TrainTaskLogService;
import com.guoyu.platform.trainingdepartment.vo.TrainTaskLogVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 培训任务日志管理服务实现
 */
@Service
public class TrainTaskLogServiceImpl implements TrainTaskLogService {

    private final TrainTaskLogMapper mapper;

    public TrainTaskLogServiceImpl(TrainTaskLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TrainTaskLogVO> page(TrainTaskLogQueryReq req) {
        LambdaQueryWrapper<TrainTaskLogEntity> wrapper = new LambdaQueryWrapper<TrainTaskLogEntity>()
                .orderByDesc(TrainTaskLogEntity::getId);
        if (req.getTrainClassId() != null) {
            wrapper.eq(TrainTaskLogEntity::getTrainClassId, req.getTrainClassId());
        }
        Page<TrainTaskLogEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
        return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
                page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Override
    @Transactional(readOnly = true)
    public TrainTaskLogVO getById(Long id) {
        TrainTaskLogEntity entity = mapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
        }
        return toVO(entity);
    }

    @Override
    @Transactional
    public TrainTaskLogVO create(TrainTaskLogCreateReq req) {
        TrainTaskLogEntity entity = new TrainTaskLogEntity();
        entity.setTrainClassId(req.getTrainClassId());
        entity.setCkId(req.getCkId());
        entity.setDate(req.getDate());
        entity.setAddtime(req.getAddtime());
        entity.setStatus(req.getStatus());
        entity.setTimes(req.getTimes());
        mapper.insert(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public TrainTaskLogVO update(Long id, TrainTaskLogUpdateReq req) {
        TrainTaskLogEntity entity = mapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
        }
        if (req.getStatus() != null) {
            entity.setStatus(req.getStatus());
        }
        if (req.getTimes() != null) {
            entity.setTimes(req.getTimes());
        }
        mapper.updateById(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    private TrainTaskLogVO toVO(TrainTaskLogEntity entity) {
        return new TrainTaskLogVO(
                entity.getId(),
                entity.getTrainClassId(),
                entity.getCkId(),
                entity.getDate(),
                entity.getAddtime(),
                entity.getStatus(),
                entity.getTimes()
        );
    }
}
