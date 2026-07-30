package com.guoyu.platform.system.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.dto.DictCreateReq;
import com.guoyu.platform.system.dto.DictItemCreateReq;
import com.guoyu.platform.system.dto.DictItemUpdateReq;
import com.guoyu.platform.system.dto.DictQueryReq;
import com.guoyu.platform.system.dto.DictUpdateReq;
import com.guoyu.platform.system.entity.DictEntity;
import com.guoyu.platform.system.entity.DictItemEntity;
import com.guoyu.platform.system.mapper.DictItemMapper;
import com.guoyu.platform.system.mapper.DictMapper;
import com.guoyu.platform.system.vo.DictItemVO;
import com.guoyu.platform.system.vo.DictVO;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class DictApplicationService {

  private final DictMapper dictMapper;
  private final DictItemMapper dictItemMapper;

  public DictApplicationService(DictMapper dictMapper, DictItemMapper dictItemMapper) {
    this.dictMapper = dictMapper;
    this.dictItemMapper = dictItemMapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<DictVO> page(DictQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<DictEntity> wrapper = new LambdaQueryWrapper<DictEntity>()
        .eq(DictEntity::getTenantId, tenantId)
        .eq(DictEntity::getDeleted, 0)
        .orderByDesc(DictEntity::getCreatedAt);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(item -> item
          .like(DictEntity::getCode, req.getKeyword())
          .or()
          .like(DictEntity::getName, req.getKeyword()));
    }
    if (StringUtils.hasText(req.getStatus())) {
      wrapper.eq(DictEntity::getStatus, req.getStatus());
    }
    Page<DictEntity> page = dictMapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(
        page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(),
        page.getSize(),
        page.getTotal()
    );
  }

  @Transactional
  public DictVO create(DictCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    ensureDictCodeAvailable(tenantId, req.code());

    DictEntity entity = new DictEntity();
    entity.setTenantId(tenantId);
    entity.setCode(req.code());
    entity.setName(req.name());
    entity.setValue(StringUtils.hasText(req.value()) ? req.value() : null);
    entity.setStatus(StringUtils.hasText(req.status()) ? req.status() : "enabled");
    entity.setDeleted(0);
    dictMapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public DictVO update(Long id, DictUpdateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    DictEntity entity = getExistingDict(tenantId, id);
    entity.setName(req.name());
    entity.setValue(StringUtils.hasText(req.value()) ? req.value() : entity.getValue());
    entity.setStatus(req.status());
    dictMapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional(readOnly = true)
  public List<DictItemVO> itemsByCode(String code) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    DictEntity dict = getExistingDictByCode(tenantId, code);
    return dictItemMapper.selectList(new LambdaQueryWrapper<DictItemEntity>()
            .eq(DictItemEntity::getTenantId, tenantId)
            .eq(DictItemEntity::getDictId, dict.getId())
            .eq(DictItemEntity::getDeleted, 0)
            .eq(DictItemEntity::getStatus, "enabled")
            .orderByAsc(DictItemEntity::getSortOrder)
            .orderByAsc(DictItemEntity::getId))
        .stream()
        .map(this::toItemVO)
        .toList();
  }

  @Transactional
  public DictItemVO createItem(Long dictId, DictItemCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    DictEntity dict = getExistingDict(tenantId, dictId);
    ensureItemValueAvailable(tenantId, dict.getId(), req.itemValue());

    DictItemEntity entity = new DictItemEntity();
    entity.setTenantId(tenantId);
    entity.setDictId(dict.getId());
    entity.setItemValue(req.itemValue());
    entity.setItemLabel(req.itemLabel());
    entity.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
    entity.setStatus(StringUtils.hasText(req.status()) ? req.status() : "enabled");
    entity.setDeleted(0);
    dictItemMapper.insert(entity);
    return toItemVO(entity);
  }

  @Transactional
  public DictItemVO updateItem(Long itemId, DictItemUpdateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    DictItemEntity entity = getExistingItem(tenantId, itemId);
    entity.setItemLabel(req.itemLabel());
    entity.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
    entity.setStatus(req.status());
    dictItemMapper.updateById(entity);
    return toItemVO(entity);
  }

  private void ensureDictCodeAvailable(Long tenantId, String code) {
    boolean exists = dictMapper.exists(new LambdaQueryWrapper<DictEntity>()
        .eq(DictEntity::getTenantId, tenantId)
        .eq(DictEntity::getCode, code)
        .eq(DictEntity::getDeleted, 0));
    if (exists) {
      throw new BusinessException(ErrorCode.CONFLICT, "Dict code already exists");
    }
  }

  private void ensureItemValueAvailable(Long tenantId, Long dictId, String itemValue) {
    boolean exists = dictItemMapper.exists(new LambdaQueryWrapper<DictItemEntity>()
        .eq(DictItemEntity::getTenantId, tenantId)
        .eq(DictItemEntity::getDictId, dictId)
        .eq(DictItemEntity::getItemValue, itemValue)
        .eq(DictItemEntity::getDeleted, 0));
    if (exists) {
      throw new BusinessException(ErrorCode.CONFLICT, "Dict item value already exists");
    }
  }

  private DictEntity getExistingDict(Long tenantId, Long id) {
    DictEntity entity = dictMapper.selectOne(new LambdaQueryWrapper<DictEntity>()
        .eq(DictEntity::getTenantId, tenantId)
        .eq(DictEntity::getId, id)
        .eq(DictEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Dict not found");
    }
    return entity;
  }

  private DictEntity getExistingDictByCode(Long tenantId, String code) {
    DictEntity entity = dictMapper.selectOne(new LambdaQueryWrapper<DictEntity>()
        .eq(DictEntity::getTenantId, tenantId)
        .eq(DictEntity::getCode, code)
        .eq(DictEntity::getDeleted, 0)
        .eq(DictEntity::getStatus, "enabled"));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Dict not found");
    }
    return entity;
  }

  private DictItemEntity getExistingItem(Long tenantId, Long itemId) {
    DictItemEntity entity = dictItemMapper.selectOne(new LambdaQueryWrapper<DictItemEntity>()
        .eq(DictItemEntity::getTenantId, tenantId)
        .eq(DictItemEntity::getId, itemId)
        .eq(DictItemEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Dict item not found");
    }
    return entity;
  }

  private DictVO toVO(DictEntity entity) {
    return new DictVO(
        entity.getId(),
        entity.getTenantId(),
        entity.getCode(),
        entity.getName(),
        entity.getValue(),
        entity.getStatus(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }

  private DictItemVO toItemVO(DictItemEntity entity) {
    return new DictItemVO(
        entity.getId(),
        entity.getTenantId(),
        entity.getDictId(),
        entity.getItemValue(),
        entity.getItemLabel(),
        entity.getSortOrder(),
        entity.getStatus(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
