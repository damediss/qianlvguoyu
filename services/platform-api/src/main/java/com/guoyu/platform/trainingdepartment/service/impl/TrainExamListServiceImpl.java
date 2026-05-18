package com.guoyu.platform.trainingdepartment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainExamListCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamListQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamListUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainExamListEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamListMapper;
import com.guoyu.platform.trainingdepartment.service.TrainExamListService;
import com.guoyu.platform.trainingdepartment.vo.TrainExamListVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 考试计划管理服务实现
 */
@Service
public class TrainExamListServiceImpl implements TrainExamListService {

  private final TrainExamListMapper mapper;

  public TrainExamListServiceImpl(TrainExamListMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<TrainExamListVO> page(TrainExamListQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainExamListEntity> wrapper = new LambdaQueryWrapper<TrainExamListEntity>()
        .eq(TrainExamListEntity::getTenantId, tenantId)
        .eq(TrainExamListEntity::getDeleted, 0)
        .orderByDesc(TrainExamListEntity::getCreateTime);
    if (req.getTrainId() != null) {
      wrapper.eq(TrainExamListEntity::getTrainId, req.getTrainId());
    }
    if (req.getOrgId() != null) {
      wrapper.eq(TrainExamListEntity::getOrgId, req.getOrgId());
    }
    if (req.getType() != null) {
      wrapper.eq(TrainExamListEntity::getType, req.getType());
    }
    if (req.getSource() != null) {
      wrapper.eq(TrainExamListEntity::getSource, req.getSource());
    }
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.like(TrainExamListEntity::getTitle, req.getKeyword());
    }
    Page<TrainExamListEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Override
  @Transactional(readOnly = true)
  public TrainExamListVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Override
  @Transactional
  public TrainExamListVO create(TrainExamListCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    TrainExamListEntity entity = new TrainExamListEntity();
    entity.setTenantId(tenantId);
    entity.setUserId(userId);
    entity.setTitle(req.getTitle());
    entity.setLevel(req.getLevel());
    entity.setTrainId(req.getTrainId());
    entity.setOrgId(req.getOrgId());
    entity.setOrgName(req.getOrgName());
    entity.setType(req.getType());
    entity.setSource(req.getSource());
    entity.setInvigilateUserId(req.getInvigilateUserId());
    entity.setInvigilateUsername(req.getInvigilateUsername());
    entity.setEvaluateUserId(req.getEvaluateUserId());
    entity.setEvaluateUsername(req.getEvaluateUsername());
    entity.setWatcherUserId(req.getWatcherUserId());
    entity.setWatcherUsername(req.getWatcherUsername());
    entity.setStaffUserId(req.getStaffUserId());
    entity.setStaffUsername(req.getStaffUsername());
    entity.setPropositionUserId(req.getPropositionUserId());
    entity.setPropositionUsername(req.getPropositionUsername());
    entity.setMarkingUserId(req.getMarkingUserId());
    entity.setMarkingUsername(req.getMarkingUsername());
    entity.setExamId(req.getExamId());
    entity.setExamDate(req.getExamDate());
    entity.setExamDateMode(req.getExamDateMode());
    entity.setExamDateList(req.getExamDateList());
    entity.setExamTime(req.getExamTime());
    entity.setNeedFinishAdventure(req.getNeedFinishAdventure());
    entity.setNeedFinishNormal(req.getNeedFinishNormal());
    entity.setJigouId(req.getJigouId());
    entity.setSortNumber(req.getSortNumber() != null ? req.getSortNumber() : 1);
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Override
  @Transactional
  public TrainExamListVO update(Long id, TrainExamListUpdateReq req) {
    TrainExamListEntity entity = getExisting(id);
    if (StringUtils.hasText(req.getTitle())) { entity.setTitle(req.getTitle()); }
    if (StringUtils.hasText(req.getLevel())) { entity.setLevel(req.getLevel()); }
    if (req.getOrgId() != null) { entity.setOrgId(req.getOrgId()); }
    if (StringUtils.hasText(req.getOrgName())) { entity.setOrgName(req.getOrgName()); }
    if (req.getType() != null) { entity.setType(req.getType()); }
    if (req.getSource() != null) { entity.setSource(req.getSource()); }
    if (req.getInvigilateUserId() != null) { entity.setInvigilateUserId(req.getInvigilateUserId()); }
    if (StringUtils.hasText(req.getInvigilateUsername())) { entity.setInvigilateUsername(req.getInvigilateUsername()); }
    if (req.getEvaluateUserId() != null) { entity.setEvaluateUserId(req.getEvaluateUserId()); }
    if (StringUtils.hasText(req.getEvaluateUsername())) { entity.setEvaluateUsername(req.getEvaluateUsername()); }
    if (req.getWatcherUserId() != null) { entity.setWatcherUserId(req.getWatcherUserId()); }
    if (StringUtils.hasText(req.getWatcherUsername())) { entity.setWatcherUsername(req.getWatcherUsername()); }
    if (req.getStaffUserId() != null) { entity.setStaffUserId(req.getStaffUserId()); }
    if (StringUtils.hasText(req.getStaffUsername())) { entity.setStaffUsername(req.getStaffUsername()); }
    if (req.getPropositionUserId() != null) { entity.setPropositionUserId(req.getPropositionUserId()); }
    if (StringUtils.hasText(req.getPropositionUsername())) { entity.setPropositionUsername(req.getPropositionUsername()); }
    if (req.getMarkingUserId() != null) { entity.setMarkingUserId(req.getMarkingUserId()); }
    if (StringUtils.hasText(req.getMarkingUsername())) { entity.setMarkingUsername(req.getMarkingUsername()); }
    if (req.getExamId() != null) { entity.setExamId(req.getExamId()); }
    if (StringUtils.hasText(req.getExamDate())) { entity.setExamDate(req.getExamDate()); }
    if (req.getExamDateMode() != null) { entity.setExamDateMode(req.getExamDateMode()); }
    if (StringUtils.hasText(req.getExamDateList())) { entity.setExamDateList(req.getExamDateList()); }
    if (req.getExamTime() != null) { entity.setExamTime(req.getExamTime()); }
    if (req.getNeedFinishAdventure() != null) { entity.setNeedFinishAdventure(req.getNeedFinishAdventure()); }
    if (req.getNeedFinishNormal() != null) { entity.setNeedFinishNormal(req.getNeedFinishNormal()); }
    if (req.getSortNumber() != null) { entity.setSortNumber(req.getSortNumber()); }
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Override
  @Transactional
  public void delete(Long id) {
    TrainExamListEntity entity = getExisting(id);
    entity.setDeleted(1);
    mapper.updateById(entity);
  }

  /**
   * 根据ID获取现有实体（未删除），不存在则抛出业务异常
   */
  private TrainExamListEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainExamListEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainExamListEntity>()
        .eq(TrainExamListEntity::getTenantId, tenantId)
        .eq(TrainExamListEntity::getId, id)
        .eq(TrainExamListEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  /**
   * 将实体转换为VO
   */
  private TrainExamListVO toVO(TrainExamListEntity entity) {
    return new TrainExamListVO(
        entity.getId(),
        entity.getTitle(),
        entity.getLevel(),
        entity.getTrainId(),
        entity.getOrgId(),
        entity.getOrgName(),
        entity.getType(),
        entity.getSource(),
        entity.getInvigilateUserId(),
        entity.getInvigilateUsername(),
        entity.getEvaluateUserId(),
        entity.getEvaluateUsername(),
        entity.getWatcherUserId(),
        entity.getWatcherUsername(),
        entity.getStaffUserId(),
        entity.getStaffUsername(),
        entity.getPropositionUserId(),
        entity.getPropositionUsername(),
        entity.getMarkingUserId(),
        entity.getMarkingUsername(),
        entity.getExamId(),
        entity.getExamDate(),
        entity.getExamDateMode(),
        entity.getExamDateList(),
        entity.getExamTime(),
        entity.getNeedFinishAdventure(),
        entity.getNeedFinishNormal(),
        entity.getUserId(),
        entity.getSortNumber(),
        entity.getTenantId(),
        entity.getJigouId(),
        entity.getCreateTime()
    );
  }
}
