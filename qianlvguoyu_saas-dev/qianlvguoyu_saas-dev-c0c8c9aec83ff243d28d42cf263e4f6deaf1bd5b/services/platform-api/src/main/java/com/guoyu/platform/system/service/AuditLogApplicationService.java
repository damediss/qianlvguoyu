package com.guoyu.platform.system.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.dto.AuditLogQueryReq;
import com.guoyu.platform.system.entity.AuditLogEntity;
import com.guoyu.platform.system.mapper.AuditLogMapper;
import com.guoyu.platform.system.vo.AuditLogVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class AuditLogApplicationService {

  private final AuditLogMapper auditLogMapper;

  public AuditLogApplicationService(AuditLogMapper auditLogMapper) {
    this.auditLogMapper = auditLogMapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<AuditLogVO> page(AuditLogQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<AuditLogEntity> wrapper = new LambdaQueryWrapper<AuditLogEntity>()
        .eq(AuditLogEntity::getTenantId, tenantId)
        .eq(AuditLogEntity::getDeleted, 0)
        .orderByDesc(AuditLogEntity::getCreatedAt);
    if (StringUtils.hasText(req.getModule())) {
      wrapper.eq(AuditLogEntity::getModule, req.getModule());
    }
    if (StringUtils.hasText(req.getAction())) {
      wrapper.eq(AuditLogEntity::getAction, req.getAction());
    }
    if (StringUtils.hasText(req.getTargetType())) {
      wrapper.eq(AuditLogEntity::getTargetType, req.getTargetType());
    }
    if (req.getUserId() != null) {
      wrapper.eq(AuditLogEntity::getUserId, req.getUserId());
    }

    Page<AuditLogEntity> page = auditLogMapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(
        page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(),
        page.getSize(),
        page.getTotal()
    );
  }

  private AuditLogVO toVO(AuditLogEntity entity) {
    return new AuditLogVO(
        entity.getId(),
        entity.getTenantId(),
        entity.getUserId(),
        entity.getModule(),
        entity.getAction(),
        entity.getTargetType(),
        entity.getTargetId(),
        entity.getRequestId(),
        entity.getIp(),
        entity.getUserAgent(),
        entity.getDetailJson(),
        entity.getCreatedAt()
    );
  }
}
