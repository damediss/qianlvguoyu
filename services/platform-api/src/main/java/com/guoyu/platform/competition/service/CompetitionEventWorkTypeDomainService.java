package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.converter.CompetitionEventWorkTypeConverter;
import com.guoyu.platform.competition.dto.CompetitionEventWorkTypeCreateReq;
import com.guoyu.platform.competition.dto.CompetitionEventWorkTypeUpdateReq;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class CompetitionEventWorkTypeDomainService {

  private static final String ACTIVE = "active";
  private static final String INACTIVE = "inactive";

  private final CompetitionEventWorkTypeMapper mapper;
  private final ExamWorkTypeMapper examWorkTypeMapper;

  public CompetitionEventWorkTypeDomainService(
      CompetitionEventWorkTypeMapper mapper,
      ExamWorkTypeMapper examWorkTypeMapper) {
    this.mapper = mapper;
    this.examWorkTypeMapper = examWorkTypeMapper;
  }

  @Transactional(readOnly = true)
  public List<CompetitionEventWorkTypeEntity> list(Long tenantId, Long eventId) {
    List<CompetitionEventWorkTypeEntity> records = mapper.selectList(
        new LambdaQueryWrapper<CompetitionEventWorkTypeEntity>()
            .eq(CompetitionEventWorkTypeEntity::getTenantId, tenantId)
            .eq(CompetitionEventWorkTypeEntity::getEventId, eventId)
            .eq(CompetitionEventWorkTypeEntity::getDeleted, 0)
            .orderByAsc(CompetitionEventWorkTypeEntity::getSortOrder)
            .orderByAsc(CompetitionEventWorkTypeEntity::getId));
    return records == null ? new ArrayList<>() : new ArrayList<>(records);
  }

  @Transactional(readOnly = true)
  public CompetitionEventWorkTypeEntity getById(Long tenantId, Long eventWorkTypeId) {
    CompetitionEventWorkTypeEntity entity = mapper.selectOne(
        new LambdaQueryWrapper<CompetitionEventWorkTypeEntity>()
            .eq(CompetitionEventWorkTypeEntity::getTenantId, tenantId)
            .eq(CompetitionEventWorkTypeEntity::getId, eventWorkTypeId)
            .eq(CompetitionEventWorkTypeEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "赛事工种不存在");
    }
    return entity;
  }

  @Transactional
  public CompetitionEventWorkTypeEntity create(
      Long tenantId,
      Long eventId,
      CompetitionEventWorkTypeCreateReq req) {
    ExamWorkTypeEntity workType = requireActiveExamWorkType(tenantId, req.workTypeId());
    if (existsBinding(tenantId, eventId, req.workTypeId(), null)) {
      throw new BusinessException(ErrorCode.CONFLICT, "同一赛事不能重复绑定同一工种");
    }
    CompetitionEventWorkTypeEntity entity = CompetitionEventWorkTypeConverter.toEntity(req);
    entity.setTenantId(tenantId);
    entity.setEventId(eventId);
    entity.setDisplayName(resolveDisplayName(req.displayName(), workType.getTitle()));
    entity.setStatus(validateStatus(req.status(), ACTIVE));
    entity.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
    entity.setDeleted(0);
    try {
      if (mapper.insert(entity) != 1) {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建赛事工种失败");
      }
      return entity;
    } catch (DuplicateKeyException ex) {
      throw new BusinessException(ErrorCode.CONFLICT, "同一赛事不能重复绑定同一工种");
    }
  }

  @Transactional
  public CompetitionEventWorkTypeEntity update(
      Long tenantId,
      Long eventId,
      Long eventWorkTypeId,
      CompetitionEventWorkTypeUpdateReq req) {
    CompetitionEventWorkTypeEntity entity = getById(tenantId, eventWorkTypeId);
    if (!Objects.equals(eventId, entity.getEventId())) {
      log.warn("Event work type path scope mismatch, tenantId={}, eventId={}, eventWorkTypeId={}",
          tenantId, eventId, eventWorkTypeId);
      throw new BusinessException(ErrorCode.NOT_FOUND, "赛事工种不存在");
    }
    ExamWorkTypeEntity workType = requireActiveExamWorkType(tenantId, req.workTypeId());
    boolean rebinding = !Objects.equals(entity.getWorkTypeId(), req.workTypeId());
    if (rebinding && mapper.countPublishedRuleReferences(tenantId, eventWorkTypeId) > 0) {
      throw new BusinessException(ErrorCode.CONFLICT, "赛事工种已被已发布规则引用，不能换绑");
    }
    if (rebinding && existsBinding(tenantId, eventId, req.workTypeId(), eventWorkTypeId)) {
      throw new BusinessException(ErrorCode.CONFLICT, "同一赛事不能重复绑定同一工种");
    }
    CompetitionEventWorkTypeConverter.applyUpdate(entity, req);
    entity.setDisplayName(resolveDisplayName(req.displayName(), workType.getTitle()));
    entity.setStatus(validateStatus(req.status(), entity.getStatus()));
    entity.setSortOrder(req.sortOrder() == null ? entity.getSortOrder() : req.sortOrder());
    try {
      if (mapper.updateById(entity) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "赛事工种已发生变化，请刷新后重试");
      }
      return entity;
    } catch (DuplicateKeyException ex) {
      throw new BusinessException(ErrorCode.CONFLICT, "同一赛事不能重复绑定同一工种");
    }
  }

  @Transactional(readOnly = true)
  public ExamWorkTypeEntity requireActiveExamWorkType(Long tenantId, Long workTypeId) {
    ExamWorkTypeEntity entity = findActiveExamWorkType(tenantId, workTypeId);
    if (entity == null) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "题库工种不存在、未启用或不属于当前租户");
    }
    return entity;
  }

  @Transactional(readOnly = true)
  public String findActiveSourceName(Long tenantId, Long workTypeId) {
    ExamWorkTypeEntity entity = findActiveExamWorkType(tenantId, workTypeId);
    return entity == null ? null : entity.getTitle();
  }

  private ExamWorkTypeEntity findActiveExamWorkType(Long tenantId, Long workTypeId) {
    int sourceId;
    int sourceTenantId;
    try {
      sourceId = Math.toIntExact(workTypeId);
      sourceTenantId = Math.toIntExact(tenantId);
    } catch (ArithmeticException | NullPointerException ex) {
      return null;
    }
    return examWorkTypeMapper.selectOne(
        new LambdaQueryWrapper<ExamWorkTypeEntity>()
            .eq(ExamWorkTypeEntity::getId, sourceId)
            .eq(ExamWorkTypeEntity::getTenantId, sourceTenantId)
            .eq(ExamWorkTypeEntity::getStatus, 1)
            .eq(ExamWorkTypeEntity::getDeleted, 0));
  }

  private boolean existsBinding(
      Long tenantId,
      Long eventId,
      Long workTypeId,
      Long excludedId) {
    LambdaQueryWrapper<CompetitionEventWorkTypeEntity> wrapper =
        new LambdaQueryWrapper<CompetitionEventWorkTypeEntity>()
            .eq(CompetitionEventWorkTypeEntity::getTenantId, tenantId)
            .eq(CompetitionEventWorkTypeEntity::getEventId, eventId)
            .eq(CompetitionEventWorkTypeEntity::getWorkTypeId, workTypeId)
            .eq(CompetitionEventWorkTypeEntity::getDeleted, 0);
    if (excludedId != null) {
      wrapper.ne(CompetitionEventWorkTypeEntity::getId, excludedId);
    }
    return mapper.selectCount(wrapper) > 0;
  }

  private String resolveDisplayName(String requested, String sourceTitle) {
    String displayName = StringUtils.hasText(requested) ? requested.trim() : sourceTitle;
    if (!StringUtils.hasText(displayName)) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "赛事工种显示名称不能为空");
    }
    if (displayName.length() > 255) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "赛事工种显示名称过长");
    }
    return displayName;
  }

  private String validateStatus(String requested, String defaultValue) {
    String status = StringUtils.hasText(requested) ? requested : defaultValue;
    if (!ACTIVE.equals(status) && !INACTIVE.equals(status)) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "赛事工种状态不合法");
    }
    return status;
  }
}
