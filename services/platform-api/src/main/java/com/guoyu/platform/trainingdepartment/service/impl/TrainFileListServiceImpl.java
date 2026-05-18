package com.guoyu.platform.trainingdepartment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainFileListCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainFileListQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainFileListUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainFileListEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainFileListMapper;
import com.guoyu.platform.trainingdepartment.service.TrainFileListService;
import com.guoyu.platform.trainingdepartment.vo.TrainFileListVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 文件列表管理服务实现
 */
@Service
public class TrainFileListServiceImpl implements TrainFileListService {

  private final TrainFileListMapper mapper;

  public TrainFileListServiceImpl(TrainFileListMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<TrainFileListVO> page(TrainFileListQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainFileListEntity> wrapper = new LambdaQueryWrapper<TrainFileListEntity>()
        .eq(TrainFileListEntity::getTenantId, tenantId)
        .orderByDesc(TrainFileListEntity::getCreateTime);
    if (StringUtils.hasText(req.getTableName())) {
      wrapper.eq(TrainFileListEntity::getTableName, req.getTableName());
    }
    if (StringUtils.hasText(req.getPk())) {
      wrapper.eq(TrainFileListEntity::getPk, req.getPk());
    }
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.like(TrainFileListEntity::getTitle, req.getKeyword());
    }
    Page<TrainFileListEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Override
  @Transactional(readOnly = true)
  public TrainFileListVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Override
  @Transactional
  public TrainFileListVO create(TrainFileListCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    TrainFileListEntity entity = new TrainFileListEntity();
    entity.setTenantId(tenantId);
    entity.setUserId(userId);
    entity.setTableName(req.getTableName());
    entity.setPk(req.getPk());
    entity.setTitle(req.getTitle());
    entity.setStatus(req.getStatus());
    entity.setSortNumber(req.getSortNumber());
    mapper.insert(entity);
    return toVO(entity);
  }

  @Override
  @Transactional
  public TrainFileListVO update(Long id, TrainFileListUpdateReq req) {
    TrainFileListEntity entity = getExisting(id);
    entity.setTitle(req.getTitle());
    entity.setStatus(req.getStatus());
    entity.setSortNumber(req.getSortNumber());
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Override
  @Transactional
  public void delete(Long id) {
    mapper.deleteById(id);
  }

  /**
   * 根据ID获取现有实体，不存在则抛出业务异常
   */
  private TrainFileListEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainFileListEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainFileListEntity>()
        .eq(TrainFileListEntity::getTenantId, tenantId)
        .eq(TrainFileListEntity::getId, id));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  /**
   * 将实体转换为VO
   */
  private TrainFileListVO toVO(TrainFileListEntity entity) {
    return new TrainFileListVO(
        entity.getId(),
        entity.getTableName(),
        entity.getPk(),
        entity.getTitle(),
        entity.getUserId(),
        entity.getStatus(),
        entity.getSortNumber(),
        entity.getTenantId(),
        entity.getCreateTime()
    );
  }
}
