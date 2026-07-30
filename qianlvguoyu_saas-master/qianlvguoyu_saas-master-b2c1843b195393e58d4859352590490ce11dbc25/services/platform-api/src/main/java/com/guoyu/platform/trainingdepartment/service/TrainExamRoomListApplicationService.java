package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainExamRoomListCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamRoomListQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamRoomListUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainExamRoomListEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamRoomListMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainExamRoomListVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class TrainExamRoomListApplicationService {

  private final TrainExamRoomListMapper mapper;

  public TrainExamRoomListApplicationService(TrainExamRoomListMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainExamRoomListVO> page(TrainExamRoomListQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainExamRoomListEntity> wrapper = new LambdaQueryWrapper<TrainExamRoomListEntity>()
        .eq(TrainExamRoomListEntity::getTenantId, tenantId)
        .eq(TrainExamRoomListEntity::getDeleted, 0)
        .orderByDesc(TrainExamRoomListEntity::getId);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.like(TrainExamRoomListEntity::getId, req.getKeyword());
    }
    Page<TrainExamRoomListEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainExamRoomListVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TrainExamRoomListVO create(TrainExamRoomListCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainExamRoomListEntity entity = new TrainExamRoomListEntity();
    entity.setTenantId(tenantId);
    entity.setOrgId(req.getOrgId());
    entity.setOrgName(req.getOrgName());
    entity.setType(req.getType());
    entity.setRoomName(req.getRoomName());
    entity.setPosition(req.getPosition());
    entity.setMaxNum(req.getMaxNum());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainExamRoomListVO update(Long id, TrainExamRoomListUpdateReq req) {
    TrainExamRoomListEntity entity = getExisting(id);
    if (req.getOrgId() != null) { entity.setOrgId(req.getOrgId()); }
    if (StringUtils.hasText(req.getOrgName())) { entity.setOrgName(req.getOrgName()); }
    if (StringUtils.hasText(req.getType())) { entity.setType(req.getType()); }
    if (StringUtils.hasText(req.getRoomName())) { entity.setRoomName(req.getRoomName()); }
    if (StringUtils.hasText(req.getPosition())) { entity.setPosition(req.getPosition()); }
    if (req.getMaxNum() != null) { entity.setMaxNum(req.getMaxNum()); }
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainExamRoomListEntity entity = getExisting(id);

    entity.setDeleted(1);
    mapper.updateById(entity);
  }

  private TrainExamRoomListEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainExamRoomListEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainExamRoomListEntity>()

        .eq(TrainExamRoomListEntity::getTenantId, tenantId)
        .eq(TrainExamRoomListEntity::getId, id)
        .eq(TrainExamRoomListEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private TrainExamRoomListVO toVO(TrainExamRoomListEntity e) {
    return new TrainExamRoomListVO(
        e.getId(),
        e.getOrgId(),
        e.getOrgName(),
        e.getType(),
        e.getRoomName(),
        e.getPosition(),
        e.getMaxNum(),
        e.getTenantId(),
        e.getCreateTime(),
        e.getUpdateTime()
    );
  }
}
