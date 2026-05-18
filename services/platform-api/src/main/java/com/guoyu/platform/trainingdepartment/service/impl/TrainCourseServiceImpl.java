package com.guoyu.platform.trainingdepartment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainCourseCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainCourseQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainCourseUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainCourseEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainCourseMapper;
import com.guoyu.platform.trainingdepartment.service.TrainCourseService;
import com.guoyu.platform.trainingdepartment.vo.TrainCourseVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 培训课程管理服务实现
 */
@Service
public class TrainCourseServiceImpl implements TrainCourseService {

  private final TrainCourseMapper mapper;

  public TrainCourseServiceImpl(TrainCourseMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<TrainCourseVO> page(TrainCourseQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainCourseEntity> wrapper = new LambdaQueryWrapper<TrainCourseEntity>()
        .eq(TrainCourseEntity::getTenantId, tenantId)
        .eq(TrainCourseEntity::getDeleted, 0)
        .orderByDesc(TrainCourseEntity::getCreateTime);
    if (req.getTrainId() != null) {
      wrapper.eq(TrainCourseEntity::getTrainId, req.getTrainId());
    }
    if (req.getType() != null) {
      wrapper.eq(TrainCourseEntity::getType, req.getType());
    }
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(w -> w
          .like(TrainCourseEntity::getTitle, req.getKeyword())
          .or()
          .like(TrainCourseEntity::getTeacherName, req.getKeyword()));
    }
    Page<TrainCourseEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Override
  @Transactional(readOnly = true)
  public TrainCourseVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Override
  @Transactional
  public TrainCourseVO create(TrainCourseCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainCourseEntity entity = new TrainCourseEntity();
    entity.setTenantId(tenantId);
    entity.setTrainId(req.getTrainId());
    entity.setTeacherIdCard(req.getTeacherIdCard());
    entity.setTeacherName(req.getTeacherName());
    entity.setTeacherUserId(req.getTeacherUserId());
    entity.setType(req.getType());
    entity.setCourseDate(req.getCourseDate());
    entity.setTitle(req.getTitle());
    entity.setStartTime(req.getStartTime());
    entity.setEndTime(req.getEndTime());
    entity.setTheoryTime(req.getTheoryTime());
    entity.setOperationTime(req.getOperationTime());
    entity.setSortNumber(req.getSortNumber());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Override
  @Transactional
  public TrainCourseVO update(Long id, TrainCourseUpdateReq req) {
    TrainCourseEntity entity = getExisting(id);
    entity.setTeacherIdCard(req.getTeacherIdCard());
    entity.setTeacherName(req.getTeacherName());
    entity.setTeacherUserId(req.getTeacherUserId());
    entity.setType(req.getType());
    entity.setCourseDate(req.getCourseDate());
    entity.setTitle(req.getTitle());
    entity.setStartTime(req.getStartTime());
    entity.setEndTime(req.getEndTime());
    entity.setTheoryTime(req.getTheoryTime());
    entity.setOperationTime(req.getOperationTime());
    entity.setSortNumber(req.getSortNumber());
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Override
  @Transactional
  public void delete(Long id) {
    TrainCourseEntity entity = getExisting(id);
    entity.setDeleted(1);
    mapper.updateById(entity);
  }

  /**
   * 根据ID获取现有实体（未删除），不存在则抛出业务异常
   */
  private TrainCourseEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainCourseEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainCourseEntity>()
        .eq(TrainCourseEntity::getTenantId, tenantId)
        .eq(TrainCourseEntity::getId, id)
        .eq(TrainCourseEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  /**
   * 将实体转换为VO
   */
  private TrainCourseVO toVO(TrainCourseEntity entity) {
    return new TrainCourseVO(
        entity.getId(),
        entity.getTrainId(),
        entity.getTeacherIdCard(),
        entity.getTeacherName(),
        entity.getTeacherUserId(),
        entity.getType(),
        entity.getCourseDate(),
        entity.getTitle(),
        entity.getStartTime(),
        entity.getEndTime(),
        entity.getTheoryTime(),
        entity.getOperationTime(),
        entity.getSortNumber(),
        entity.getTenantId(),
        entity.getCreateTime()
    );
  }
}
