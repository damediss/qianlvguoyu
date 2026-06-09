package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainUserPaperCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserPaperQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserPaperUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainUserPaperEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainUserPaperMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainUserPaperVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 用户论文管理服务实现
 */
@Service
@Slf4j
public class TrainUserPaperApplicationService {

  private final TrainUserPaperMapper mapper;

  public TrainUserPaperApplicationService(TrainUserPaperMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainUserPaperVO> page(TrainUserPaperQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainUserPaperEntity> wrapper = new LambdaQueryWrapper<TrainUserPaperEntity>()
        .eq(TrainUserPaperEntity::getTenantId, tenantId)
        .eq(TrainUserPaperEntity::getDeleted, 0)
        .orderByDesc(TrainUserPaperEntity::getCreateTime);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.like(TrainUserPaperEntity::getTitle, req.getKeyword());
    }
    if (StringUtils.hasText(req.getType())) {
      wrapper.eq(TrainUserPaperEntity::getType, req.getType());
    }
    if (req.getUserId() != null) {
      wrapper.eq(TrainUserPaperEntity::getUserId, req.getUserId());
    }
    Page<TrainUserPaperEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainUserPaperVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TrainUserPaperVO create(TrainUserPaperCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    TrainUserPaperEntity entity = new TrainUserPaperEntity();
    entity.setTenantId(tenantId);
    entity.setUserId(userId);
    entity.setUrl(req.getUrl());
    entity.setTitle(req.getTitle());
    entity.setType(req.getType());
    entity.setPlatformUserId(req.getPlatformUserId());
    entity.setSortNumber(req.getSortNumber());
    entity.setPk(req.getPk());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainUserPaperVO update(Long id, TrainUserPaperUpdateReq req) {
    TrainUserPaperEntity entity = getExisting(id);
    entity.setUrl(req.getUrl());
    entity.setTitle(req.getTitle());
    entity.setType(req.getType());
    entity.setSortNumber(req.getSortNumber());
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainUserPaperEntity entity = getExisting(id);
    entity.setDeleted(1);
    mapper.deleteById(entity.getId());
  }

  private TrainUserPaperEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainUserPaperEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainUserPaperEntity>()
        .eq(TrainUserPaperEntity::getTenantId, tenantId)
        .eq(TrainUserPaperEntity::getId, id)
        .eq(TrainUserPaperEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private TrainUserPaperVO toVO(TrainUserPaperEntity entity) {
    return new TrainUserPaperVO(
        entity.getId(),
        entity.getUrl(),
        entity.getTitle(),
        entity.getType(),
        entity.getUserId(),
        entity.getPlatformUserId(),
        entity.getSortNumber(),
        entity.getTenantId(),
        entity.getCreateTime(),
        entity.getPk()
    );
  }
}
