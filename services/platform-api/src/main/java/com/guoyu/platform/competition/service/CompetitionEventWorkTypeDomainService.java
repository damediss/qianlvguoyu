package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.converter.CompetitionEventWorkTypeConverter;
import com.guoyu.platform.competition.dto.CompetitionEventWorkTypeCreateReq;
import com.guoyu.platform.competition.dto.CompetitionEventWorkTypeUpdateReq;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeStageEntity;
import com.guoyu.platform.competition.enums.CompetitionStagePlanStatus;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeLevelMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeStageMapper;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
  private final CompetitionEventWorkTypeLevelMapper levelMapper;
  private final CompetitionEventWorkTypeStageMapper stageMapper;

  public CompetitionEventWorkTypeDomainService(
      CompetitionEventWorkTypeMapper mapper,
      ExamWorkTypeMapper examWorkTypeMapper) {
    this(mapper, examWorkTypeMapper, null, null);
  }

  @Autowired
  public CompetitionEventWorkTypeDomainService(
      CompetitionEventWorkTypeMapper mapper,
      ExamWorkTypeMapper examWorkTypeMapper,
      CompetitionEventWorkTypeLevelMapper levelMapper,
      CompetitionEventWorkTypeStageMapper stageMapper) {
    this.mapper = mapper;
    this.examWorkTypeMapper = examWorkTypeMapper;
    this.levelMapper = levelMapper;
    this.stageMapper = stageMapper;
  }

  @Transactional(readOnly = true)
  public List<CompetitionEventWorkTypeEntity> list(Long tenantId, Long eventId) {
    List<CompetitionEventWorkTypeEntity> records = mapper.selectList(
        new LambdaQueryWrapper<CompetitionEventWorkTypeEntity>()
            .eq(CompetitionEventWorkTypeEntity::getTenantId, tenantId)
            .eq(CompetitionEventWorkTypeEntity::getEventId, eventId)
            .eq(CompetitionEventWorkTypeEntity::getStatus, ACTIVE)
            .eq(CompetitionEventWorkTypeEntity::getDeleted, 0)
            .orderByAsc(CompetitionEventWorkTypeEntity::getSortOrder)
            .orderByAsc(CompetitionEventWorkTypeEntity::getId));
    if (records == null) {
      return new ArrayList<>();
    }
    List<CompetitionEventWorkTypeEntity> active = new ArrayList<>();
    for (CompetitionEventWorkTypeEntity record : records) {
      if (record != null && ACTIVE.equals(record.getStatus())) {
        active.add(record);
      }
    }
    return active;
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
    String status = validateStatus(req.status(), ACTIVE);
    CompetitionEventWorkTypeEntity retired = findBinding(tenantId, eventId, req.workTypeId());
    if (retired != null) {
      if (INACTIVE.equals(retired.getStatus()) && ACTIVE.equals(status)) {
        return reactivate(retired, req, workType);
      }
      throw new BusinessException(ErrorCode.CONFLICT, "同一赛事不能重复绑定同一工种");
    }
    CompetitionEventWorkTypeEntity entity = CompetitionEventWorkTypeConverter.toEntity(req);
    entity.setTenantId(tenantId);
    entity.setEventId(eventId);
    entity.setDisplayName(resolveDisplayName(req.displayName(), workType.getTitle()));
    entity.setStatus(status);
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
    if (INACTIVE.equals(req.status())) {
      remove(tenantId, eventId, eventWorkTypeId);
      entity.setStatus(INACTIVE);
      return entity;
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

  @Transactional
  public void remove(Long tenantId, Long eventId, Long eventWorkTypeId) {
    CompetitionEventWorkTypeEntity entity = getById(tenantId, eventWorkTypeId);
    if (!Objects.equals(eventId, entity.getEventId())) {
      log.warn("Event work type removal crossed event scope, tenantId={}, eventId={}, eventWorkTypeId={}",
          tenantId, eventId, eventWorkTypeId);
      throw new BusinessException(ErrorCode.NOT_FOUND, "赛事工种不存在");
    }
    if (CompetitionStagePlanStatus.CONFIRMED.getCode().equals(entity.getStagePlanStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "已确认编排的工种不能移除");
    }
    if (levelMapper == null || stageMapper == null) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "赛事工种删除失败，请刷新后重试");
    }
    long references = mapper.countOperationalReferences(tenantId, eventWorkTypeId);
    if (references > 0) {
      log.warn("Refused to remove event work type that still has operational rows, tenantId={}, eventId={}, eventWorkTypeId={}, references={}",
          tenantId, eventId, eventWorkTypeId, references);
      throw new BusinessException(ErrorCode.CONFLICT, "该工种已有赛段、报名或试题等业务数据，不能移除");
    }
    List<CompetitionEventWorkTypeLevelEntity> levels = levelMapper.selectList(
        new LambdaQueryWrapper<CompetitionEventWorkTypeLevelEntity>()
            .eq(CompetitionEventWorkTypeLevelEntity::getTenantId, tenantId)
            .eq(CompetitionEventWorkTypeLevelEntity::getEventId, eventId)
            .eq(CompetitionEventWorkTypeLevelEntity::getEventWorkTypeId, eventWorkTypeId)
            .eq(CompetitionEventWorkTypeLevelEntity::getDeleted, 0));
    if (levels == null) {
      levels = new ArrayList<>();
    }
    for (CompetitionEventWorkTypeLevelEntity level : levels) {
      if (level != null
          && CompetitionStagePlanStatus.CONFIRMED.getCode().equals(level.getStagePlanStatus())) {
        throw new BusinessException(ErrorCode.CONFLICT, "已确认编排的工种不能移除");
      }
    }
    List<CompetitionEventWorkTypeStageEntity> stageLinks = stageMapper.selectList(
        new LambdaQueryWrapper<CompetitionEventWorkTypeStageEntity>()
            .eq(CompetitionEventWorkTypeStageEntity::getTenantId, tenantId)
            .eq(CompetitionEventWorkTypeStageEntity::getEventId, eventId)
            .eq(CompetitionEventWorkTypeStageEntity::getEventWorkTypeId, eventWorkTypeId)
            .eq(CompetitionEventWorkTypeStageEntity::getDeleted, 0));
    if (stageLinks == null) {
      stageLinks = new ArrayList<>();
    }
    for (CompetitionEventWorkTypeStageEntity stageLink : stageLinks) {
      if (stageLink != null && stageMapper.deleteById(stageLink.getId()) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "赛事工种删除失败，请刷新后重试");
      }
    }
    for (CompetitionEventWorkTypeLevelEntity level : levels) {
      if (level != null && levelMapper.deleteById(level.getId()) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "赛事工种删除失败，请刷新后重试");
      }
    }
    if (mapper.deleteById(entity.getId()) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "赛事工种删除失败，请刷新后重试");
    }
  }

  private CompetitionEventWorkTypeEntity reactivate(
      CompetitionEventWorkTypeEntity retired,
      CompetitionEventWorkTypeCreateReq req,
      ExamWorkTypeEntity workType) {
    retired.setDisplayName(resolveDisplayName(req.displayName(), workType.getTitle()));
    retired.setStatus(ACTIVE);
    retired.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
    try {
      if (mapper.updateById(retired) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "赛事工种已发生变化，请刷新后重试");
      }
      return retired;
    } catch (DuplicateKeyException ex) {
      throw new BusinessException(ErrorCode.CONFLICT, "同一赛事不能重复绑定同一工种");
    }
  }

  private CompetitionEventWorkTypeEntity findBinding(
      Long tenantId, Long eventId, Long workTypeId) {
    return mapper.selectOne(
        new LambdaQueryWrapper<CompetitionEventWorkTypeEntity>()
            .eq(CompetitionEventWorkTypeEntity::getTenantId, tenantId)
            .eq(CompetitionEventWorkTypeEntity::getEventId, eventId)
            .eq(CompetitionEventWorkTypeEntity::getWorkTypeId, workTypeId)
            .eq(CompetitionEventWorkTypeEntity::getDeleted, 0));
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
