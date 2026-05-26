package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainExamPeopleInRoomCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamPeopleInRoomQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamPeopleInRoomUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainExamPeopleInRoomEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamPeopleInRoomMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainExamPeopleInRoomVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class TrainExamPeopleInRoomApplicationService {

  private final TrainExamPeopleInRoomMapper mapper;

  public TrainExamPeopleInRoomApplicationService(TrainExamPeopleInRoomMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainExamPeopleInRoomVO> page(TrainExamPeopleInRoomQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainExamPeopleInRoomEntity> wrapper = new LambdaQueryWrapper<TrainExamPeopleInRoomEntity>()
        .eq(TrainExamPeopleInRoomEntity::getTenantId, tenantId)
        .eq(TrainExamPeopleInRoomEntity::getDeleted, 0)
        .orderByDesc(TrainExamPeopleInRoomEntity::getId);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.like(TrainExamPeopleInRoomEntity::getId, req.getKeyword());
    }
    Page<TrainExamPeopleInRoomEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainExamPeopleInRoomVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TrainExamPeopleInRoomVO create(TrainExamPeopleInRoomCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainExamPeopleInRoomEntity entity = new TrainExamPeopleInRoomEntity();
    entity.setTenantId(tenantId);
    entity.setExamId(req.getExamId());
    entity.setCourseId(req.getCourseId());
    entity.setSessionId(req.getSessionId());
    entity.setRoomId(req.getRoomId());
    entity.setExamRoomId(req.getExamRoomId());
    entity.setUserId(req.getUserId());
    entity.setRealname(req.getRealname());
    entity.setPeopleId(req.getPeopleId());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainExamPeopleInRoomVO update(Long id, TrainExamPeopleInRoomUpdateReq req) {
    TrainExamPeopleInRoomEntity entity = getExisting(id);
    if (req.getExamId() != null) { entity.setExamId(req.getExamId()); }
    if (req.getCourseId() != null) { entity.setCourseId(req.getCourseId()); }
    if (req.getSessionId() != null) { entity.setSessionId(req.getSessionId()); }
    if (req.getRoomId() != null) { entity.setRoomId(req.getRoomId()); }
    if (req.getExamRoomId() != null) { entity.setExamRoomId(req.getExamRoomId()); }
    if (req.getUserId() != null) { entity.setUserId(req.getUserId()); }
    if (StringUtils.hasText(req.getRealname())) { entity.setRealname(req.getRealname()); }
    if (req.getPeopleId() != null) { entity.setPeopleId(req.getPeopleId()); }
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainExamPeopleInRoomEntity entity = getExisting(id);

    entity.setDeleted(1);
    mapper.updateById(entity);
  }

  private TrainExamPeopleInRoomEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainExamPeopleInRoomEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainExamPeopleInRoomEntity>()

        .eq(TrainExamPeopleInRoomEntity::getTenantId, tenantId)
        .eq(TrainExamPeopleInRoomEntity::getId, id)
        .eq(TrainExamPeopleInRoomEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private TrainExamPeopleInRoomVO toVO(TrainExamPeopleInRoomEntity e) {
    return new TrainExamPeopleInRoomVO(
        e.getId(),
        e.getExamId(),
        e.getCourseId(),
        e.getSessionId(),
        e.getRoomId(),
        e.getExamRoomId(),
        e.getUserId(),
        e.getRealname(),
        e.getPeopleId(),
        e.getTenantId(),
        e.getCreateTime(),
        e.getUpdateTime()
    );
  }
}
