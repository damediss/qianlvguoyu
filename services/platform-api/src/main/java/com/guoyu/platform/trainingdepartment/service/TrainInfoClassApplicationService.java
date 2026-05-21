package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.trainingdepartment.dto.TrainInfoClassCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainInfoClassQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainInfoClassUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainInfoClassVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 培训信息班级管理服务实现
 */
@Service
@Slf4j
public class TrainInfoClassApplicationService {

  private final TrainInfoClassMapper mapper;

  public TrainInfoClassApplicationService(TrainInfoClassMapper mapper) {
  this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainInfoClassVO> page(TrainInfoClassQueryReq req) {
  LambdaQueryWrapper<TrainInfoClassEntity> wrapper = new LambdaQueryWrapper<TrainInfoClassEntity>()
    .eq(TrainInfoClassEntity::getDeleted, 0)
    .orderByDesc(TrainInfoClassEntity::getId);
  if (req.getTrainInfoId() != null) {
      wrapper.eq(TrainInfoClassEntity::getTrainInfoId, req.getTrainInfoId());
  }
  if (req.getCompanyId() != null) {
      wrapper.eq(TrainInfoClassEntity::getCompanyId, req.getCompanyId());
  }
  Page<TrainInfoClassEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
  return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
    page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainInfoClassVO getById(Long id) {
  return toVO(getExisting(id));
  }

  @Transactional
  public TrainInfoClassVO create(TrainInfoClassCreateReq req) {
  TrainInfoClassEntity entity = new TrainInfoClassEntity();
  entity.setTrainInfoId(req.getTrainInfoId());
  entity.setTitle(req.getTitle());
  entity.setNums(req.getNums());
  entity.setCompanyId(req.getCompanyId());
  entity.setWorkTypeId(req.getWorkTypeId());
  entity.setDateStart(req.getDateStart());
  entity.setDateEnd(req.getDateEnd());
  entity.setAddtime(req.getAddtime());
  entity.setFileTemps(req.getFileTemps());
  entity.setKcbiaoFile(req.getKcbiaoFile());
  entity.setDeleted(0);
  mapper.insert(entity);
  return toVO(entity);
  }

  @Transactional
  public TrainInfoClassVO update(Long id, TrainInfoClassUpdateReq req) {
  TrainInfoClassEntity entity = getExisting(id);
  entity.setTitle(req.getTitle());
  entity.setNums(req.getNums());
  entity.setDateStart(req.getDateStart());
  entity.setDateEnd(req.getDateEnd());
  entity.setFileTemps(req.getFileTemps());
  entity.setKcbiaoFile(req.getKcbiaoFile());
  mapper.updateById(entity);
  return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
  TrainInfoClassEntity entity = getExisting(id);
  entity.setDeleted(1);
  mapper.updateById(entity);
  }

  /**
   * 根据ID获取现有实体（未删除），不存在则抛出业务异常
   */
  private TrainInfoClassEntity getExisting(Long id) {
  TrainInfoClassEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainInfoClassEntity>()
    .eq(TrainInfoClassEntity::getId, id)
    .eq(TrainInfoClassEntity::getDeleted, 0));
  if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
  }
  return entity;
  }

  /**
   * 将实体转换为VO
   */
  private TrainInfoClassVO toVO(TrainInfoClassEntity entity) {
  return new TrainInfoClassVO(
    entity.getId(),
    entity.getTrainInfoId(),
    entity.getTitle(),
    entity.getNums(),
    entity.getCompanyId(),
    entity.getWorkTypeId(),
    entity.getDateStart(),
    entity.getDateEnd(),
    entity.getAddtime(),
    entity.getFileTemps(),
    entity.getKcbiaoFile()
  );
  }
}
