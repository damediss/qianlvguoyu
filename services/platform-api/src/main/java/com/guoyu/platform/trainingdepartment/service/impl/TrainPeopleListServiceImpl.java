package com.guoyu.platform.trainingdepartment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.SignupAuditReq;
import com.guoyu.platform.trainingdepartment.dto.TrainPeopleListCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainPeopleListQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainPeopleListUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import com.guoyu.platform.trainingdepartment.service.TrainPeopleListService;
import com.guoyu.platform.trainingdepartment.vo.TrainPeopleListVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 培训人员列表管理服务实现
 */
@Service
public class TrainPeopleListServiceImpl implements TrainPeopleListService {

  private final TrainPeopleListMapper mapper;

  public TrainPeopleListServiceImpl(TrainPeopleListMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<TrainPeopleListVO> page(TrainPeopleListQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainPeopleListEntity> wrapper = new LambdaQueryWrapper<TrainPeopleListEntity>()
        .eq(TrainPeopleListEntity::getTenantId, tenantId)
        .eq(TrainPeopleListEntity::getDeleted, 0)
        .orderByDesc(TrainPeopleListEntity::getCreateTime);
    if (req.getTrainId() != null) {
      wrapper.eq(TrainPeopleListEntity::getTrainId, req.getTrainId());
    }
    if (req.getTrainClassId() != null) {
      wrapper.eq(TrainPeopleListEntity::getTrainClassId, req.getTrainClassId());
    }
    if (StringUtils.hasText(req.getType())) {
      wrapper.eq(TrainPeopleListEntity::getType, req.getType());
    }
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(w -> w
          .like(TrainPeopleListEntity::getRealname, req.getKeyword())
          .or()
          .like(TrainPeopleListEntity::getPhone, req.getKeyword()));
    }
    Page<TrainPeopleListEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Override
  @Transactional(readOnly = true)
  public TrainPeopleListVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Override
  @Transactional
  public TrainPeopleListVO create(TrainPeopleListCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    TrainPeopleListEntity entity = new TrainPeopleListEntity();
    entity.setTenantId(tenantId);
    entity.setUserId(userId);
    entity.setTrainId(req.getTrainId());
    entity.setTrainClassId(req.getTrainClassId());
    entity.setType(req.getType());
    entity.setPlatformUserId(req.getPlatformUserId());
    entity.setRealname(req.getRealname());
    entity.setPhone(req.getPhone());
    entity.setIsLeader(req.getIsLeader());
    entity.setIsCommittee(req.getIsCommittee());
    entity.setExamScore(req.getExamScore());
    entity.setRealScore(req.getRealScore());
    entity.setPaper(req.getPaper());
    entity.setNormalScore(req.getNormalScore());
    entity.setNationalPaper(req.getNationalPaper());
    entity.setCurrentLevel(req.getCurrentLevel());
    entity.setSortNumber(req.getSortNumber());
    entity.setIdCard(req.getIdCard());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Override
  @Transactional
  public TrainPeopleListVO update(Long id, TrainPeopleListUpdateReq req) {
    TrainPeopleListEntity entity = getExisting(id);
    entity.setRealname(req.getRealname());
    entity.setPhone(req.getPhone());
    entity.setIsLeader(req.getIsLeader());
    entity.setIsCommittee(req.getIsCommittee());
    entity.setExamScore(req.getExamScore());
    entity.setRealScore(req.getRealScore());
    entity.setPaper(req.getPaper());
    entity.setNormalScore(req.getNormalScore());
    entity.setNationalPaper(req.getNationalPaper());
    entity.setCurrentLevel(req.getCurrentLevel());
    entity.setSortNumber(req.getSortNumber());
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Override
  @Transactional
  public void delete(Long id) {
    TrainPeopleListEntity entity = getExisting(id);
    entity.setDeleted(1);
    mapper.updateById(entity);
  }

  @Override
  @Transactional
  public void batchAudit(SignupAuditReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    mapper.update(null, new LambdaUpdateWrapper<TrainPeopleListEntity>()
        .set(TrainPeopleListEntity::getStatus, req.getStatus())
        .in(TrainPeopleListEntity::getId, req.getIds())
        .eq(TrainPeopleListEntity::getTenantId, tenantId)
        .eq(TrainPeopleListEntity::getDeleted, 0));
  }

  /**
   * 根据ID获取现有实体（未删除），不存在则抛出业务异常
   */
  private TrainPeopleListEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainPeopleListEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainPeopleListEntity>()
        .eq(TrainPeopleListEntity::getTenantId, tenantId)
        .eq(TrainPeopleListEntity::getId, id)
        .eq(TrainPeopleListEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  /**
   * 将实体转换为VO
   */
  private TrainPeopleListVO toVO(TrainPeopleListEntity entity) {
    return new TrainPeopleListVO(
        entity.getId(),
        entity.getTrainId(),
        entity.getTrainClassId(),
        entity.getType(),
        entity.getUserId(),
        entity.getPlatformUserId(),
        entity.getRealname(),
        entity.getPhone(),
        entity.getIsLeader(),
        entity.getIsCommittee(),
        entity.getExamScore(),
        entity.getRealScore(),
        entity.getPaper(),
        entity.getNormalScore(),
        entity.getNationalPaper(),
        entity.getCurrentLevel(),
        entity.getSortNumber(),
        entity.getTenantId(),
        entity.getCreateTime(),
        entity.getIdCard(),
        entity.getStatus()
    );
  }
}
