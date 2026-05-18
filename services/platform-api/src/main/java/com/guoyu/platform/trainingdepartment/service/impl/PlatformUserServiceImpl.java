package com.guoyu.platform.trainingdepartment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.trainingdepartment.dto.PlatformUserCreateReq;
import com.guoyu.platform.trainingdepartment.dto.PlatformUserQueryReq;
import com.guoyu.platform.trainingdepartment.dto.PlatformUserUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.PlatformUserEntity;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserMapper;
import com.guoyu.platform.trainingdepartment.service.PlatformUserService;
import com.guoyu.platform.trainingdepartment.vo.PlatformUserVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 平台用户管理服务实现
 */
@Service
public class PlatformUserServiceImpl implements PlatformUserService {

    private final PlatformUserMapper mapper;

    public PlatformUserServiceImpl(PlatformUserMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PlatformUserVO> page(PlatformUserQueryReq req) {
        LambdaQueryWrapper<PlatformUserEntity> wrapper = new LambdaQueryWrapper<PlatformUserEntity>()
                .eq(PlatformUserEntity::getDeleted, 0)
                .orderByDesc(PlatformUserEntity::getId);
        if (StringUtils.hasText(req.getKeyword())) {
            wrapper.and(w -> w
                    .like(PlatformUserEntity::getRealname, req.getKeyword())
                    .or()
                    .like(PlatformUserEntity::getPhone, req.getKeyword()));
        }
        if (req.getCompanyId() != null) {
            wrapper.eq(PlatformUserEntity::getCompanyId, req.getCompanyId());
        }
        if (req.getOrgId() != null) {
            wrapper.eq(PlatformUserEntity::getOrgId, req.getOrgId());
        }
        if (StringUtils.hasText(req.getStat())) {
            wrapper.eq(PlatformUserEntity::getStat, req.getStat());
        }
        Page<PlatformUserEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
        return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
                page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformUserVO getById(Long id) {
        return toVO(getExisting(id));
    }

    @Override
    @Transactional
    public PlatformUserVO create(PlatformUserCreateReq req) {
        PlatformUserEntity entity = new PlatformUserEntity();
        entity.setRealname(req.getRealname());
        entity.setPhone(req.getPhone());
        entity.setPassword(req.getPassword());
        entity.setIdcard(req.getIdcard());
        entity.setOrgId(req.getOrgId());
        entity.setCompanyId(req.getCompanyId());
        entity.setWxopenid(req.getWxopenid());
        entity.setExpertId(req.getExpertId());
        entity.setTask(req.getTask());
        entity.setDeleted(0);
        mapper.insert(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public PlatformUserVO update(Long id, PlatformUserUpdateReq req) {
        PlatformUserEntity entity = getExisting(id);
        entity.setRealname(req.getRealname());
        entity.setPhone(req.getPhone());
        entity.setIdcard(req.getIdcard());
        entity.setStat(req.getStat());
        entity.setOrgId(req.getOrgId());
        entity.setCompanyId(req.getCompanyId());
        entity.setWxopenid(req.getWxopenid());
        entity.setExpertId(req.getExpertId());
        entity.setTask(req.getTask());
        mapper.updateById(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        PlatformUserEntity entity = getExisting(id);
        entity.setDeleted(1);
        mapper.updateById(entity);
    }

    private PlatformUserEntity getExisting(Long id) {
        PlatformUserEntity entity = mapper.selectOne(new LambdaQueryWrapper<PlatformUserEntity>()
                .eq(PlatformUserEntity::getId, id)
                .eq(PlatformUserEntity::getDeleted, 0));
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
        }
        return entity;
    }

    private PlatformUserVO toVO(PlatformUserEntity entity) {
        return new PlatformUserVO(
                entity.getId(),
                entity.getRealname(),
                entity.getIdcard(),
                entity.getPhone(),
                entity.getStat(),
                entity.getAddtime(),
                entity.getLogintime(),
                entity.getOrgId(),
                entity.getCompanyId(),
                entity.getWxopenid(),
                entity.getExpertId(),
                entity.getTask()
        );
    }
}
