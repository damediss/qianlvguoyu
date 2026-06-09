package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainUserPatentCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserPatentQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainUserPatentUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainUserPatentEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainUserPatentMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainUserPatentVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 用户专利管理服务实现
 */
@Service
@Slf4j
public class TrainUserPatentApplicationService {

  private final TrainUserPatentMapper mapper;

  public TrainUserPatentApplicationService(TrainUserPatentMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainUserPatentVO> page(TrainUserPatentQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainUserPatentEntity> wrapper = new LambdaQueryWrapper<TrainUserPatentEntity>()
        .eq(TrainUserPatentEntity::getTenantId, tenantId)
        .eq(TrainUserPatentEntity::getDeleted, 0)
        .orderByDesc(TrainUserPatentEntity::getCreateTime);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.like(TrainUserPatentEntity::getTitle, req.getKeyword());
    }
    Page<TrainUserPatentEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainUserPatentVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TrainUserPatentVO create(TrainUserPatentCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    TrainUserPatentEntity entity = new TrainUserPatentEntity();
    entity.setTenantId(tenantId);
    entity.setUserId(userId);
    entity.setTitle(req.getTitle());
    entity.setPaper(req.getPaper());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainUserPatentVO update(Long id, TrainUserPatentUpdateReq req) {
    TrainUserPatentEntity entity = getExisting(id);
    entity.setTitle(req.getTitle());
    entity.setPaper(req.getPaper());
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainUserPatentEntity entity = getExisting(id);
    entity.setDeleted(1);
    mapper.deleteById(entity.getId());
  }

  private TrainUserPatentEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainUserPatentEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainUserPatentEntity>()
        .eq(TrainUserPatentEntity::getTenantId, tenantId)
        .eq(TrainUserPatentEntity::getId, id)
        .eq(TrainUserPatentEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private TrainUserPatentVO toVO(TrainUserPatentEntity entity) {
    return new TrainUserPatentVO(
        entity.getId(),
        entity.getTitle(),
        entity.getPaper(),
        entity.getUserId(),
        entity.getTenantId(),
        entity.getCreateTime(),
        entity.getUpdateTime()
    );
  }
}
