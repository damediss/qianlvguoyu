package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.trainingdepartment.dto.CourseKejianCreateReq;
import com.guoyu.platform.trainingdepartment.dto.CourseKejianQueryReq;
import com.guoyu.platform.trainingdepartment.dto.CourseKejianUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.CourseKejianEntity;
import com.guoyu.platform.trainingdepartment.mapper.CourseKejianMapper;
import com.guoyu.platform.trainingdepartment.vo.CourseKejianVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 课程课件管理服务实现
 */
@Service
@Slf4j
public class CourseKejianApplicationService {

  private final CourseKejianMapper mapper;

  public CourseKejianApplicationService(CourseKejianMapper mapper) {
  this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<CourseKejianVO> page(CourseKejianQueryReq req) {
  LambdaQueryWrapper<CourseKejianEntity> wrapper = new LambdaQueryWrapper<CourseKejianEntity>()
    .eq(CourseKejianEntity::getDeleted, 0)
    .orderByDesc(CourseKejianEntity::getId);
  if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(w -> w
          .like(CourseKejianEntity::getTitle, req.getKeyword())
          .or()
          .like(CourseKejianEntity::getSno, req.getKeyword()));
  }
  if (req.getWorkTypeId() != null) {
      wrapper.eq(CourseKejianEntity::getWorkTypeId, req.getWorkTypeId());
  }
  if (req.getType() != null) {
      wrapper.eq(CourseKejianEntity::getType, req.getType());
  }
  if (req.getTag() != null) {
      wrapper.eq(CourseKejianEntity::getTag, req.getTag());
  }
  Page<CourseKejianEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
  return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
    page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public CourseKejianVO getById(Long id) {
  return toVO(getExisting(id));
  }

  @Transactional
  public CourseKejianVO create(CourseKejianCreateReq req) {
  CourseKejianEntity entity = new CourseKejianEntity();
  entity.setSno(req.getSno());
  entity.setTitle(req.getTitle());
  entity.setKeshi(req.getKeshi());
  entity.setCover(req.getCover());
  entity.setType(req.getType());
  entity.setPrice(req.getPrice());
  entity.setDescription(req.getDescription());
  entity.setContent(req.getContent());
  entity.setFilePath(req.getFilePath());
  entity.setWorkTypeId(req.getWorkTypeId());
  entity.setTag(req.getTag());
  entity.setJigouId(req.getJigouId());
  entity.setOwnerId(req.getOwnerId());
  entity.setAddtime(System.currentTimeMillis() / 1000);
  entity.setDeleted(0);
  mapper.insert(entity);
  return toVO(entity);
  }

  @Transactional
  public CourseKejianVO update(Long id, CourseKejianUpdateReq req) {
  CourseKejianEntity entity = getExisting(id);
  if (req.getTitle() != null) {
      entity.setTitle(req.getTitle());
  }
  if (req.getKeshi() != null) {
      entity.setKeshi(req.getKeshi());
  }
  if (req.getCover() != null) {
      entity.setCover(req.getCover());
  }
  if (req.getType() != null) {
      entity.setType(req.getType());
  }
  if (req.getPrice() != null) {
      entity.setPrice(req.getPrice());
  }
  if (req.getDescription() != null) {
      entity.setDescription(req.getDescription());
  }
  if (req.getContent() != null) {
      entity.setContent(req.getContent());
  }
  if (req.getFilePath() != null) {
      entity.setFilePath(req.getFilePath());
  }
  if (req.getWorkTypeId() != null) {
      entity.setWorkTypeId(req.getWorkTypeId());
  }
  if (req.getTag() != null) {
      entity.setTag(req.getTag());
  }
  mapper.updateById(entity);
  return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
  CourseKejianEntity entity = getExisting(id);
  entity.setDeleted(1);
  mapper.deleteById(entity.getId());
  }

  /**
   * 根据ID获取现有实体（未删除），不存在则抛出业务异常
   */
  private CourseKejianEntity getExisting(Long id) {
  CourseKejianEntity entity = mapper.selectOne(new LambdaQueryWrapper<CourseKejianEntity>()
    .eq(CourseKejianEntity::getId, id)
    .eq(CourseKejianEntity::getDeleted, 0));
  if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
  }
  return entity;
  }

  /**
   * 将实体转换为VO
   */
  private CourseKejianVO toVO(CourseKejianEntity entity) {
  return new CourseKejianVO(
    entity.getId(),
    entity.getSno(),
    entity.getTitle(),
    entity.getKeshi(),
    entity.getCover(),
    entity.getType(),
    entity.getPrice(),
    entity.getDescription(),
    entity.getContent(),
    entity.getFilePath(),
    entity.getWorkTypeId(),
    entity.getTag(),
    entity.getJigouId(),
    entity.getOwnerId(),
    entity.getAddtime()
  );
  }
}
