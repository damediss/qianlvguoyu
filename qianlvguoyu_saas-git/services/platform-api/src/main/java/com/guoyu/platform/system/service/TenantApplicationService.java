package com.guoyu.platform.system.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.system.dto.TenantCreateReq;
import com.guoyu.platform.system.dto.TenantQueryReq;
import com.guoyu.platform.system.dto.TenantUpdateReq;
import com.guoyu.platform.system.entity.TenantEntity;
import com.guoyu.platform.system.mapper.TenantMapper;
import com.guoyu.platform.system.vo.TenantVO;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class TenantApplicationService {

  private final TenantMapper tenantMapper;

  public TenantApplicationService(TenantMapper tenantMapper) {
    this.tenantMapper = tenantMapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TenantVO> page(TenantQueryReq req) {
    LambdaQueryWrapper<TenantEntity> wrapper = new LambdaQueryWrapper<TenantEntity>()
        .eq(TenantEntity::getDeleted, 0)
        .orderByDesc(TenantEntity::getCreatedAt);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(item -> item
          .like(TenantEntity::getCode, req.getKeyword())
          .or()
          .like(TenantEntity::getName, req.getKeyword()));
    }
    if (StringUtils.hasText(req.getStatus())) {
      wrapper.eq(TenantEntity::getStatus, req.getStatus());
    }
    Page<TenantEntity> page = tenantMapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(
        page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(),
        page.getSize(),
        page.getTotal()
    );
  }

  @Transactional(readOnly = true)
  public TenantVO detail(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TenantVO create(TenantCreateReq req) {
    boolean exists = tenantMapper.exists(new LambdaQueryWrapper<TenantEntity>()
        .eq(TenantEntity::getCode, req.code())
        .eq(TenantEntity::getDeleted, 0));
    if (exists) {
      throw new BusinessException(ErrorCode.CONFLICT, "Tenant code already exists");
    }

    LocalDateTime now = LocalDateTime.now();
    TenantEntity entity = new TenantEntity();
    entity.setCode(req.code());
    entity.setName(req.name());
    entity.setStatus(StringUtils.hasText(req.status()) ? req.status() : "enabled");
    entity.setContactName(req.contactName());
    entity.setContactPhone(req.contactPhone());
    entity.setRemark(req.remark());
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);
    entity.setDeleted(0);
    tenantMapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TenantVO update(Long id, TenantUpdateReq req) {
    TenantEntity entity = getExisting(id);
    entity.setName(req.name());
    entity.setStatus(req.status());
    entity.setContactName(req.contactName());
    entity.setContactPhone(req.contactPhone());
    entity.setRemark(req.remark());
    entity.setUpdatedAt(LocalDateTime.now());
    tenantMapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TenantEntity entity = getExisting(id);
    entity.setDeleted(1);
    entity.setUpdatedAt(LocalDateTime.now());
    tenantMapper.deleteById(entity.getId());
  }

  private TenantEntity getExisting(Long id) {
    TenantEntity entity = tenantMapper.selectOne(new LambdaQueryWrapper<TenantEntity>()
        .eq(TenantEntity::getId, id)
        .eq(TenantEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Tenant not found");
    }
    return entity;
  }

  private TenantVO toVO(TenantEntity entity) {
    return new TenantVO(
        entity.getId(),
        entity.getCode(),
        entity.getName(),
        entity.getStatus(),
        entity.getContactName(),
        entity.getContactPhone(),
        entity.getRemark(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
