package com.guoyu.platform.trainingdepartment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainRequestCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainRequestQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainRequestUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainRequestEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainRequestMapper;
import com.guoyu.platform.trainingdepartment.service.TrainRequestService;
import com.guoyu.platform.trainingdepartment.vo.TrainRequestVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 培训需求/诉求管理服务实现
 */
@Service
public class TrainRequestServiceImpl implements TrainRequestService {

    private final TrainRequestMapper mapper;

    public TrainRequestServiceImpl(TrainRequestMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TrainRequestVO> page(TrainRequestQueryReq req) {
        Long tenantId = SecurityContext.currentUser().getTenantId();
        LambdaQueryWrapper<TrainRequestEntity> wrapper = new LambdaQueryWrapper<TrainRequestEntity>()
                .eq(TrainRequestEntity::getTenantId, tenantId)
                .eq(TrainRequestEntity::getDeleted, 0)
                .orderByDesc(TrainRequestEntity::getCreateTime);
        if (StringUtils.hasText(req.getKeyword())) {
            wrapper.and(w -> w
                    .like(TrainRequestEntity::getCompanyName, req.getKeyword())
                    .or()
                    .like(TrainRequestEntity::getContact, req.getKeyword()));
        }
        if (req.getCompanyId() != null) {
            wrapper.eq(TrainRequestEntity::getCompanyId, req.getCompanyId());
        }
        Page<TrainRequestEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
        return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
                page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Override
    @Transactional(readOnly = true)
    public TrainRequestVO getById(Long id) {
        return toVO(getExisting(id));
    }

    @Override
    @Transactional
    public TrainRequestVO create(TrainRequestCreateReq req) {
        Long tenantId = SecurityContext.currentUser().getTenantId();
        Long userId = SecurityContext.currentUser().getUserId();
        TrainRequestEntity entity = new TrainRequestEntity();
        entity.setTenantId(tenantId);
        entity.setUserId(userId);
        entity.setCompanyName(req.getCompanyName());
        entity.setCompanyId(req.getCompanyId());
        entity.setPosition(req.getPosition());
        entity.setNum(req.getNum());
        entity.setType(req.getType());
        entity.setTrainDate(req.getTrainDate());
        entity.setContact(req.getContact());
        entity.setContactPhone(req.getContactPhone());
        entity.setIsOnPosition(req.getIsOnPosition());
        entity.setStudentForm(req.getStudentForm());
        entity.setSortNumber(req.getSortNumber());
        entity.setDeleted(0);
        mapper.insert(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public TrainRequestVO update(Long id, TrainRequestUpdateReq req) {
        TrainRequestEntity entity = getExisting(id);
        if (req.getCompanyName() != null) {
            entity.setCompanyName(req.getCompanyName());
        }
        if (req.getCompanyId() != null) {
            entity.setCompanyId(req.getCompanyId());
        }
        if (req.getPosition() != null) {
            entity.setPosition(req.getPosition());
        }
        if (req.getNum() != null) {
            entity.setNum(req.getNum());
        }
        if (req.getType() != null) {
            entity.setType(req.getType());
        }
        if (req.getTrainDate() != null) {
            entity.setTrainDate(req.getTrainDate());
        }
        if (req.getContact() != null) {
            entity.setContact(req.getContact());
        }
        if (req.getContactPhone() != null) {
            entity.setContactPhone(req.getContactPhone());
        }
        if (req.getIsOnPosition() != null) {
            entity.setIsOnPosition(req.getIsOnPosition());
        }
        if (req.getStudentForm() != null) {
            entity.setStudentForm(req.getStudentForm());
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
        TrainRequestEntity entity = getExisting(id);
        entity.setDeleted(1);
        mapper.updateById(entity);
    }

    private TrainRequestEntity getExisting(Long id) {
        Long tenantId = SecurityContext.currentUser().getTenantId();
        TrainRequestEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainRequestEntity>()
                .eq(TrainRequestEntity::getTenantId, tenantId)
                .eq(TrainRequestEntity::getId, id)
                .eq(TrainRequestEntity::getDeleted, 0));
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
        }
        return entity;
    }

    private TrainRequestVO toVO(TrainRequestEntity entity) {
        return new TrainRequestVO(
                entity.getId(),
                entity.getCompanyName(),
                entity.getCompanyId(),
                entity.getPosition(),
                entity.getNum(),
                entity.getType(),
                entity.getTrainDate(),
                entity.getContact(),
                entity.getContactPhone(),
                entity.getIsOnPosition(),
                entity.getStudentForm(),
                entity.getUserId(),
                entity.getSortNumber(),
                entity.getTenantId(),
                entity.getCreateTime()
        );
    }
}
