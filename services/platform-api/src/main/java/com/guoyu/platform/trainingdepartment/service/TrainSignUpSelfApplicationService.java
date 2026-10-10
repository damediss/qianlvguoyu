package com.guoyu.platform.trainingdepartment.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainSignUpSelfCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSignUpSelfQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSignUpSelfUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpSelfEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpSelfMapper;
import com.guoyu.platform.trainingdepartment.service.TrainingOperatorScopeService;
import com.guoyu.platform.trainingdepartment.vo.TrainSignUpSelfVO;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class TrainSignUpSelfApplicationService {

  private final TrainSignUpSelfMapper mapper;
  private final TrainingOperatorScopeService operatorScopeService;

  public TrainSignUpSelfApplicationService(
      TrainSignUpSelfMapper mapper,
      TrainingOperatorScopeService operatorScopeService) {
    this.mapper = mapper;
    this.operatorScopeService = operatorScopeService;
  }

  @Transactional(readOnly = true)
  public PageResponse<TrainSignUpSelfVO> page(TrainSignUpSelfQueryReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    LambdaQueryWrapper<TrainSignUpSelfEntity> wrapper = new LambdaQueryWrapper<TrainSignUpSelfEntity>()
        .eq(TrainSignUpSelfEntity::getDeleted, 0)
        .eq(TrainSignUpSelfEntity::getTenantId, tenantId)
        .orderByAsc(TrainSignUpSelfEntity::getSortNumber)
        .orderByDesc(TrainSignUpSelfEntity::getCreateTime)
        .orderByDesc(TrainSignUpSelfEntity::getId);
    Long companyFilter = operatorScopeService.resolveCompanyFilter(req.getOrgId());
    if (companyFilter != null) {
      wrapper.eq(TrainSignUpSelfEntity::getOrgId, companyFilter);
    }
    if (req.getTrainInfoId() != null) {
      wrapper.eq(TrainSignUpSelfEntity::getTrainInfoId, req.getTrainInfoId());
    }
    if (req.getClassId() != null) {
      wrapper.eq(TrainSignUpSelfEntity::getTrainClassId, req.getClassId())
          .eq(TrainSignUpSelfEntity::getCollectionSource, ClassCollectionApplicationService.SOURCE_CLASS_PREP);
    } else if (StringUtils.hasText(req.getCollectionSource())) {
      wrapper.eq(TrainSignUpSelfEntity::getCollectionSource, req.getCollectionSource().trim());
    } else {
      // 旧报名计划 CRUD：排除调研/按班收集
      wrapper.and(w -> w
          .eq(TrainSignUpSelfEntity::getCollectionSource, "train_info")
          .or()
          .isNull(TrainSignUpSelfEntity::getCollectionSource));
    }
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.like(TrainSignUpSelfEntity::getTitle, req.getKeyword());
    }
    Page<TrainSignUpSelfEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
    return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
        page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public TrainSignUpSelfVO getById(Long id) {
    return toVO(getExisting(id));
  }

  @Transactional
  public TrainSignUpSelfVO create(TrainSignUpSelfCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    assertTitleUnique(tenantId, req.getTitle(), null);
    assertTrainInfoAvailable(tenantId, req.getTrainInfoId(), null);
    TrainSignUpSelfEntity entity = new TrainSignUpSelfEntity();
    entity.setTenantId(tenantId);
    entity.setTitle(req.getTitle().trim());
    entity.setCover(trimToNull(req.getCover()));
    entity.setTrainTime(req.getTrainTime().trim());
    entity.setExamTime(req.getExamTime().trim());
    entity.setOrgId(req.getOrgId());
    entity.setTrainInfoId(normalizeTrainInfoId(req.getTrainInfoId()));
    entity.setCollectionSource("train_info");
    entity.setSortNumber(req.getSortNumber() != null ? req.getSortNumber() : 0);
    entity.setDeleted(0);
    entity.setCreateTime(LocalDateTime.now());
    entity.setUpdateTime(LocalDateTime.now());
    mapper.insert(entity);
    return toVO(entity);
  }

  @Transactional
  public TrainSignUpSelfVO update(Long id, TrainSignUpSelfUpdateReq req) {
    TrainSignUpSelfEntity entity = getExisting(id);
    if (StringUtils.hasText(req.getTitle())) {
      assertTitleUnique(entity.getTenantId(), req.getTitle().trim(), id);
      entity.setTitle(req.getTitle().trim());
    }
    if (req.getCover() != null) {
      entity.setCover(trimToNull(req.getCover()));
    }
    if (StringUtils.hasText(req.getTrainTime())) {
      entity.setTrainTime(req.getTrainTime().trim());
    }
    if (StringUtils.hasText(req.getExamTime())) {
      entity.setExamTime(req.getExamTime().trim());
    }
    if (req.getOrgId() != null) {
      entity.setOrgId(req.getOrgId());
    }
    if (req.getTrainInfoId() != null) {
      assertTrainInfoAvailable(entity.getTenantId(), req.getTrainInfoId(), id);
      entity.setTrainInfoId(normalizeTrainInfoId(req.getTrainInfoId()));
    }
    if (req.getSortNumber() != null) {
      entity.setSortNumber(req.getSortNumber());
    }
    entity.setUpdateTime(LocalDateTime.now());
    mapper.updateById(entity);
    return toVO(entity);
  }

  @Transactional
  public void delete(Long id) {
    getExisting(id);
    mapper.deleteById(id);
  }

  private TrainSignUpSelfEntity getExisting(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    TrainSignUpSelfEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainSignUpSelfEntity>()
        .eq(TrainSignUpSelfEntity::getId, id)
        .eq(TrainSignUpSelfEntity::getTenantId, tenantId)
        .eq(TrainSignUpSelfEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
    }
    Long companyFilter = operatorScopeService.resolveCompanyFilter(entity.getOrgId());
    if (companyFilter != null && entity.getOrgId() != null && !companyFilter.equals(entity.getOrgId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该报名计划");
    }
    return entity;
  }

  private void assertTitleUnique(Long tenantId, String title, Long excludeId) {
    LambdaQueryWrapper<TrainSignUpSelfEntity> wrapper = new LambdaQueryWrapper<TrainSignUpSelfEntity>()
        .eq(TrainSignUpSelfEntity::getDeleted, 0)
        .eq(TrainSignUpSelfEntity::getTenantId, tenantId)
        .eq(TrainSignUpSelfEntity::getTitle, title);
    if (excludeId != null) {
      wrapper.ne(TrainSignUpSelfEntity::getId, excludeId);
    }
    if (mapper.selectCount(wrapper) > 0) {
      throw new BusinessException(ErrorCode.CONFLICT, "该标题已存在");
    }
  }

  private void assertTrainInfoAvailable(Long tenantId, Long trainInfoId, Long excludeId) {
    Long normalized = normalizeTrainInfoId(trainInfoId);
    if (normalized == null) {
      return;
    }
    // 仅约束旧 train_info 信息收集；class_prep / research 可共享同一 train_info
    LambdaQueryWrapper<TrainSignUpSelfEntity> wrapper = new LambdaQueryWrapper<TrainSignUpSelfEntity>()
        .eq(TrainSignUpSelfEntity::getDeleted, 0)
        .eq(TrainSignUpSelfEntity::getTenantId, tenantId)
        .eq(TrainSignUpSelfEntity::getTrainInfoId, normalized)
        .and(w -> w
            .eq(TrainSignUpSelfEntity::getCollectionSource, "train_info")
            .or()
            .isNull(TrainSignUpSelfEntity::getCollectionSource));
    if (excludeId != null) {
      wrapper.ne(TrainSignUpSelfEntity::getId, excludeId);
    }
    if (mapper.selectCount(wrapper) > 0) {
      throw new BusinessException(ErrorCode.CONFLICT, "该培训信息已被其他报名计划绑定");
    }
  }

  private Long normalizeTrainInfoId(Long trainInfoId) {
    return trainInfoId != null && trainInfoId > 0 ? trainInfoId : null;
  }

  private String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }

  private TrainSignUpSelfVO toVO(TrainSignUpSelfEntity e) {
    return new TrainSignUpSelfVO(
        e.getId(),
        e.getTitle(),
        e.getCover(),
        e.getTrainTime(),
        e.getExamTime(),
        e.getOrgId(),
        e.getTrainInfoId(),
        e.getTrainClassId(),
        e.getCollectionSource(),
        e.getSortNumber(),
        e.getTenantId(),
        e.getCreateTime(),
        e.getUpdateTime()
    );
  }
}
