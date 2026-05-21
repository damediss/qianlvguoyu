package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainInfoListCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainInfoListQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainInfoListUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSolutionEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSolutionMapper;
import com.guoyu.platform.trainingdepartment.vo.TrainInfoListVO;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 培训信息列表管理服务实现
 */
@Service
@Slf4j
public class TrainInfoListApplicationService {

  private final TrainInfoListMapper mapper;
  private final TrainSolutionMapper solutionMapper;

  public TrainInfoListApplicationService(TrainInfoListMapper mapper, TrainSolutionMapper solutionMapper) {
    this.mapper = mapper;
    this.solutionMapper = solutionMapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainInfoListVO> page(TrainInfoListQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainInfoListEntity> wrapper = new LambdaQueryWrapper<TrainInfoListEntity>()
        .eq(TrainInfoListEntity::getTenantId, tenantId)
        .eq(TrainInfoListEntity::getDeleted, 0)
        .orderByDesc(TrainInfoListEntity::getCreateTime);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(w -> w
          .like(TrainInfoListEntity::getTitle, req.getKeyword())
          .or()
          .like(TrainInfoListEntity::getCompanyName, req.getKeyword()));
    }
    if (req.getStatus() != null) {
      wrapper.eq(TrainInfoListEntity::getStatus, req.getStatus());
    }
    if (req.getCompanyId() != null) {
      wrapper.eq(TrainInfoListEntity::getCompanyId, req.getCompanyId());
    }
    if (req.getWorkTypeId() != null) {
      wrapper.eq(TrainInfoListEntity::getWorkTypeId, req.getWorkTypeId());
    }
    if (req.getCourseId() != null) {
      wrapper.eq(TrainInfoListEntity::getCourseId, req.getCourseId());
    }
    if (req.getPassStatus() != null) {
      wrapper.eq(TrainInfoListEntity::getPassStatus, req.getPassStatus());
    }
    Page<TrainInfoListEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainInfoListVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TrainInfoListVO create(TrainInfoListCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    TrainInfoListEntity entity = new TrainInfoListEntity();
    entity.setTenantId(tenantId);
    entity.setUserId(userId);
    entity.setTitle(req.getTitle());
    entity.setCover(req.getCover());
    entity.setIntro(req.getIntro());
    entity.setGroupType(req.getGroupType());
    entity.setMasterType(req.getMasterType());
    entity.setType(req.getType());
    entity.setCate(req.getCate());
    entity.setCompanyId(req.getCompanyId());
    entity.setCompanyName(req.getCompanyName());
    entity.setCompanyAddress(req.getCompanyAddress());
    entity.setWorkTypeId(req.getWorkTypeId());
    entity.setWorkTypeLevel(req.getWorkTypeLevel());
    entity.setCourseId(req.getCourseId());
    entity.setNum(req.getNum());
    entity.setPosition(req.getPosition());
    entity.setCourseNum(req.getCourseNum());
    entity.setHeadConfirm(req.getHeadConfirm());
    entity.setCompanyHasConfirm(req.getCompanyHasConfirm());
    entity.setCompanyConfirmTime(req.getCompanyConfirmTime());
    entity.setCompanyConfirmUserId(req.getCompanyConfirmUserId());
    entity.setCompanyConfirmUsername(req.getCompanyConfirmUsername());
    entity.setPassStatus(req.getPassStatus());
    entity.setCheckUserId(req.getCheckUserId());
    entity.setCheckUsername(req.getCheckUsername());
    entity.setIsSortedLearn(req.getIsSortedLearn());
    entity.setMaxNormalScore(req.getMaxNormalScore());
    entity.setCourseFile(req.getCourseFile());
    entity.setStartDate(req.getStartDate());
    entity.setEndDate(req.getEndDate());
    entity.setTrainCompanyName(req.getTrainCompanyName());
    entity.setStatus(req.getStatus());
    entity.setSortNumber(req.getSortNumber());
    entity.setPoint(req.getPoint());
    entity.setAlbum(req.getAlbum());
    entity.setSummary(req.getSummary());
    entity.setProvinceId(req.getProvinceId());
    entity.setProvinceName(req.getProvinceName());
    entity.setCityId(req.getCityId());
    entity.setCityName(req.getCityName());
    entity.setAbilityScore(req.getAbilityScore());
    entity.setRegionId(req.getRegionId());
    entity.setRegionName(req.getRegionName());
    entity.setInChargeUserId(req.getInChargeUserId());
    entity.setInChargeUsername(req.getInChargeUsername());
    entity.setFiles(req.getFiles());
    entity.setTraindemandId(req.getTraindemandId());
    entity.setDeleted(0);
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainInfoListVO update(Long id, TrainInfoListUpdateReq req) {
    TrainInfoListEntity entity = getExisting(id);
    if (req.getTitle() != null) {
      entity.setTitle(req.getTitle());
    }
    if (req.getCover() != null) {
      entity.setCover(req.getCover());
    }
    if (req.getIntro() != null) {
      entity.setIntro(req.getIntro());
    }
    if (req.getGroupType() != null) {
      entity.setGroupType(req.getGroupType());
    }
    if (req.getMasterType() != null) {
      entity.setMasterType(req.getMasterType());
    }
    if (req.getType() != null) {
      entity.setType(req.getType());
    }
    if (req.getCate() != null) {
      entity.setCate(req.getCate());
    }
    if (req.getCompanyId() != null) {
      entity.setCompanyId(req.getCompanyId());
    }
    if (req.getCompanyName() != null) {
      entity.setCompanyName(req.getCompanyName());
    }
    if (req.getCompanyAddress() != null) {
      entity.setCompanyAddress(req.getCompanyAddress());
    }
    if (req.getWorkTypeId() != null) {
      entity.setWorkTypeId(req.getWorkTypeId());
    }
    if (req.getWorkTypeLevel() != null) {
      entity.setWorkTypeLevel(req.getWorkTypeLevel());
    }
    if (req.getCourseId() != null) {
      entity.setCourseId(req.getCourseId());
    }
    if (req.getNum() != null) {
      entity.setNum(req.getNum());
    }
    if (req.getPosition() != null) {
      entity.setPosition(req.getPosition());
    }
    if (req.getCourseNum() != null) {
      entity.setCourseNum(req.getCourseNum());
    }
    if (req.getHeadConfirm() != null) {
      entity.setHeadConfirm(req.getHeadConfirm());
    }
    if (req.getCompanyHasConfirm() != null) {
      entity.setCompanyHasConfirm(req.getCompanyHasConfirm());
    }
    if (req.getCompanyConfirmTime() != null) {
      entity.setCompanyConfirmTime(req.getCompanyConfirmTime());
    }
    if (req.getCompanyConfirmUserId() != null) {
      entity.setCompanyConfirmUserId(req.getCompanyConfirmUserId());
    }
    if (req.getCompanyConfirmUsername() != null) {
      entity.setCompanyConfirmUsername(req.getCompanyConfirmUsername());
    }
    if (req.getPassStatus() != null) {
      entity.setPassStatus(req.getPassStatus());
    }
    if (req.getCheckUserId() != null) {
      entity.setCheckUserId(req.getCheckUserId());
    }
    if (req.getCheckUsername() != null) {
      entity.setCheckUsername(req.getCheckUsername());
    }
    if (req.getIsSortedLearn() != null) {
      entity.setIsSortedLearn(req.getIsSortedLearn());
    }
    if (req.getMaxNormalScore() != null) {
      entity.setMaxNormalScore(req.getMaxNormalScore());
    }
    if (req.getCourseFile() != null) {
      entity.setCourseFile(req.getCourseFile());
    }
    if (req.getStartDate() != null) {
      entity.setStartDate(req.getStartDate());
    }
    if (req.getEndDate() != null) {
      entity.setEndDate(req.getEndDate());
    }
    if (req.getTrainCompanyName() != null) {
      entity.setTrainCompanyName(req.getTrainCompanyName());
    }
    if (req.getStatus() != null) {
      entity.setStatus(req.getStatus());
    }
    if (req.getSortNumber() != null) {
      entity.setSortNumber(req.getSortNumber());
    }
    if (req.getPoint() != null) {
      entity.setPoint(req.getPoint());
    }
    if (req.getAlbum() != null) {
      entity.setAlbum(req.getAlbum());
    }
    if (req.getSummary() != null) {
      entity.setSummary(req.getSummary());
    }
    if (req.getProvinceId() != null) {
      entity.setProvinceId(req.getProvinceId());
    }
    if (req.getProvinceName() != null) {
      entity.setProvinceName(req.getProvinceName());
    }
    if (req.getCityId() != null) {
      entity.setCityId(req.getCityId());
    }
    if (req.getCityName() != null) {
      entity.setCityName(req.getCityName());
    }
    if (req.getAbilityScore() != null) {
      entity.setAbilityScore(req.getAbilityScore());
    }
    if (req.getRegionId() != null) {
      entity.setRegionId(req.getRegionId());
    }
    if (req.getRegionName() != null) {
      entity.setRegionName(req.getRegionName());
    }
    if (req.getInChargeUserId() != null) {
      entity.setInChargeUserId(req.getInChargeUserId());
    }
    if (req.getInChargeUsername() != null) {
      entity.setInChargeUsername(req.getInChargeUsername());
    }
    if (req.getFiles() != null) {
      entity.setFiles(req.getFiles());
    }
    if (req.getTraindemandId() != null) {
      entity.setTraindemandId(req.getTraindemandId());
    }
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    TrainInfoListEntity entity = getExisting(id);
    entity.setDeleted(1);
    mapper.updateById(entity);
  }

  // ======== Company confirm/reject workflow (企业确认/驳回) ========

  @Transactional
  public TrainInfoListVO companyConfirm(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    String username = SecurityContext.currentUser().getUsername();
    TrainInfoListEntity entity = getExisting(id);
    entity.setCompanyHasConfirm(1);
    entity.setCompanyConfirmTime(LocalDateTime.now());
    entity.setCompanyConfirmUserId(userId);
    entity.setCompanyConfirmUsername(username);
    mapper.updateById(entity);
    // Clear reject reason on related solutions
    TrainSolutionEntity solution = new TrainSolutionEntity();
    solution.setRejectReason("");
    solutionMapper.update(solution, new LambdaQueryWrapper<TrainSolutionEntity>()
        .eq(TrainSolutionEntity::getTenantId, tenantId)
        .eq(TrainSolutionEntity::getTrainInfoId, id)
        .eq(TrainSolutionEntity::getDeleted, 0));
    return toVO(entity);
  }

  @Transactional
  public TrainInfoListVO companyReject(Long id, String reason) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    if (reason == null || reason.isBlank()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请填写驳回理由");
    }
    // Set reject reason on related solutions
    TrainSolutionEntity solution = new TrainSolutionEntity();
    solution.setRejectReason(reason);
    solutionMapper.update(solution, new LambdaQueryWrapper<TrainSolutionEntity>()
        .eq(TrainSolutionEntity::getTenantId, tenantId)
        .eq(TrainSolutionEntity::getTrainInfoId, id)
        .eq(TrainSolutionEntity::getDeleted, 0));
    return getById(id);
  }

  private TrainInfoListEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainInfoListEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainInfoListEntity>()
        .eq(TrainInfoListEntity::getTenantId, tenantId)
        .eq(TrainInfoListEntity::getId, id)
        .eq(TrainInfoListEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    return entity;
  }

  private TrainInfoListVO toVO(TrainInfoListEntity entity) {
    return new TrainInfoListVO(
        entity.getId(),
        entity.getTitle(),
        entity.getCover(),
        entity.getIntro(),
        entity.getGroupType(),
        entity.getMasterType(),
        entity.getType(),
        entity.getCate(),
        entity.getCompanyId(),
        entity.getCompanyName(),
        entity.getCompanyAddress(),
        entity.getWorkTypeId(),
        entity.getWorkTypeLevel(),
        entity.getCourseId(),
        entity.getNum(),
        entity.getPosition(),
        entity.getCourseNum(),
        entity.getHeadConfirm(),
        entity.getUserId(),
        entity.getCompanyHasConfirm(),
        entity.getCompanyConfirmTime(),
        entity.getCompanyConfirmUserId(),
        entity.getCompanyConfirmUsername(),
        entity.getPassStatus(),
        entity.getCheckUserId(),
        entity.getCheckUsername(),
        entity.getIsSortedLearn(),
        entity.getMaxNormalScore(),
        entity.getCourseFile(),
        entity.getStartDate(),
        entity.getEndDate(),
        entity.getTrainCompanyName(),
        entity.getStatus(),
        entity.getSortNumber(),
        entity.getTenantId(),
        entity.getCreateTime(),
        entity.getPoint(),
        entity.getAlbum(),
        entity.getSummary(),
        entity.getProvinceId(),
        entity.getProvinceName(),
        entity.getCityId(),
        entity.getCityName(),
        entity.getAbilityScore(),
        entity.getRegionId(),
        entity.getRegionName(),
        entity.getInChargeUserId(),
        entity.getInChargeUsername(),
        entity.getFiles(),
        entity.getTraindemandId()
    );
  }
}
