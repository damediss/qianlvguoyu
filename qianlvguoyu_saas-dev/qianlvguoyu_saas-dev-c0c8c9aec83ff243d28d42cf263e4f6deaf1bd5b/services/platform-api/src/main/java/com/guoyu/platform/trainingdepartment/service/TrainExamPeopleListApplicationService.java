package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainExamPeopleListCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamPeopleListQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainExamPeopleListUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainExamPeopleListEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamPeopleListMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainExamPeopleListVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class TrainExamPeopleListApplicationService {

  private final TrainExamPeopleListMapper mapper;

  public TrainExamPeopleListApplicationService(TrainExamPeopleListMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainExamPeopleListVO> page(TrainExamPeopleListQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainExamPeopleListEntity> wrapper = new LambdaQueryWrapper<TrainExamPeopleListEntity>()
        .eq(TrainExamPeopleListEntity::getTenantId, tenantId)
        .eq(TrainExamPeopleListEntity::getDeleted, 0)
        .orderByDesc(TrainExamPeopleListEntity::getId);
    if (req.getExamId() != null) {
      wrapper.eq(TrainExamPeopleListEntity::getExamId, req.getExamId());
    }
    if (req.getPlatformUserId() != null) {
      wrapper.eq(TrainExamPeopleListEntity::getPlatformUserId, req.getPlatformUserId());
    }
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(w -> w
          .like(TrainExamPeopleListEntity::getRealname, req.getKeyword())
          .or()
          .like(TrainExamPeopleListEntity::getPhone, req.getKeyword()));
    }
    Page<TrainExamPeopleListEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainExamPeopleListVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TrainExamPeopleListVO create(TrainExamPeopleListCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainExamPeopleListEntity entity = new TrainExamPeopleListEntity();
    entity.setTenantId(tenantId);
    entity.setExamId(req.getExamId());
    entity.setPlatformUserId(req.getPlatformUserId());
    entity.setWorkTypeId(req.getWorkTypeId());
    entity.setWorkTypeLevel(req.getWorkTypeLevel());
    entity.setRealname(req.getRealname());
    entity.setIdCardNum(req.getIdCardNum());
    entity.setPhone(req.getPhone());
    entity.setEducation(req.getEducation());
    entity.setRoomId(req.getRoomId());
    entity.setInWhiteList(req.getInWhiteList() != null ? req.getInWhiteList() : 0);
    entity.setSortNumber(req.getSortNumber() != null ? req.getSortNumber() : 1);
    entity.setLearnTime(0);
    entity.setLearnUseTime(0);
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainExamPeopleListVO update(Long id, TrainExamPeopleListUpdateReq req) {
    TrainExamPeopleListEntity entity = getExisting(id);
    if (req.getWorkTypeId() != null) { entity.setWorkTypeId(req.getWorkTypeId()); }
    if (req.getWorkTypeLevel() != null) { entity.setWorkTypeLevel(req.getWorkTypeLevel()); }
    if (req.getRealname() != null) { entity.setRealname(req.getRealname()); }
    if (req.getIdCardNum() != null) { entity.setIdCardNum(req.getIdCardNum()); }
    if (req.getPhone() != null) { entity.setPhone(req.getPhone()); }
    if (req.getEducation() != null) { entity.setEducation(req.getEducation()); }
    if (req.getRoomId() != null) { entity.setRoomId(req.getRoomId()); }
    if (req.getInWhiteList() != null) { entity.setInWhiteList(req.getInWhiteList()); }
    if (req.getNormalScore() != null) { entity.setNormalScore(req.getNormalScore()); }
    if (req.getScore() != null) { entity.setScore(req.getScore()); }
    if (req.getTotalScore() != null) { entity.setTotalScore(req.getTotalScore()); }
    if (req.getLearnTime() != null) { entity.setLearnTime(req.getLearnTime()); }
    if (req.getLearnUseTime() != null) { entity.setLearnUseTime(req.getLearnUseTime()); }
    if (req.getSortNumber() != null) { entity.setSortNumber(req.getSortNumber()); }
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainExamPeopleListEntity entity = getExisting(id);
    entity.setDeleted(1);
    mapper.updateById(entity);
  }

  @Transactional
  public void batchDelete(Long examId, java.util.List<Long> ids) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    for (Long id : ids) {
      TrainExamPeopleListEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainExamPeopleListEntity>()
          .eq(TrainExamPeopleListEntity::getTenantId, tenantId)
          .eq(TrainExamPeopleListEntity::getId, id)
          .eq(TrainExamPeopleListEntity::getDeleted, 0));
      if (entity != null) {
        entity.setDeleted(1);
        mapper.updateById(entity);
      }
    }
  }

  private TrainExamPeopleListEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainExamPeopleListEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainExamPeopleListEntity>()
        .eq(TrainExamPeopleListEntity::getTenantId, tenantId)
        .eq(TrainExamPeopleListEntity::getId, id)
        .eq(TrainExamPeopleListEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private TrainExamPeopleListVO toVO(TrainExamPeopleListEntity entity) {
    return new TrainExamPeopleListVO(
        entity.getId(),
        entity.getExamId(),
        entity.getPlatformUserId(),
        entity.getWorkTypeId(),
        entity.getWorkTypeLevel(),
        entity.getRealname(),
        entity.getIdCardNum(),
        entity.getPhone(),
        entity.getEducation(),
        entity.getRoomId(),
        entity.getInWhiteList(),
        entity.getNormalScore(),
        entity.getScore(),
        entity.getTotalScore(),
        entity.getLearnTime(),
        entity.getLearnUseTime(),
        entity.getUserId(),
        entity.getSortNumber(),
        entity.getTenantId(),
        entity.getCreateTime(),
        entity.getUpdateTime()
    );
  }
}
