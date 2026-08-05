package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.converter.CompetitionEventWorkTypeLevelConverter;
import com.guoyu.platform.competition.dto.CompetitionEventWorkTypeLevelBatchReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventOrgEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.enums.CompetitionEventOrgStatus;
import com.guoyu.platform.competition.enums.CompetitionEventScope;
import com.guoyu.platform.competition.enums.CompetitionEventStatus;
import com.guoyu.platform.competition.mapper.CompetitionEventOrgMapper;
import com.guoyu.platform.competition.vo.CompetitionEventWorkTypeLevelVO;
import com.guoyu.platform.security.PlatformAdminSupport;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.UserMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionEventWorkTypeLevelApplicationService {

  private final CompetitionEventService eventService;
  private final CompetitionEventWorkTypeLevelService levelService;
  private final CompetitionEventOrgMapper eventOrgMapper;
  private final UserMapper userMapper;

  public CompetitionEventWorkTypeLevelApplicationService(
      CompetitionEventService eventService,
      CompetitionEventWorkTypeLevelService levelService,
      CompetitionEventOrgMapper eventOrgMapper,
      UserMapper userMapper) {
    this.eventService = eventService;
    this.levelService = levelService;
    this.eventOrgMapper = eventOrgMapper;
    this.userMapper = userMapper;
  }

  @Transactional(readOnly = true)
  public List<CompetitionEventWorkTypeLevelVO> list(Long eventId, Long eventWorkTypeId) {
    PlatformUserPrincipal principal = currentUser();
    accessibleEvent(principal, eventId, false);
    List<CompetitionEventWorkTypeLevelVO> result = new ArrayList<>();
    for (CompetitionEventWorkTypeLevelEntity entity : levelService.list(
        principal.getTenantId(), eventId, eventWorkTypeId)) {
      result.add(CompetitionEventWorkTypeLevelConverter.toVO(entity));
    }
    return result;
  }

  @Transactional
  public List<CompetitionEventWorkTypeLevelVO> replace(
      Long eventId,
      Long eventWorkTypeId,
      CompetitionEventWorkTypeLevelBatchReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionEventEntity event = accessibleEvent(principal, eventId, true);
    if (!CompetitionEventStatus.DRAFT.getCode().equals(event.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "赛事发布后不能修改开放等级");
    }
    List<CompetitionEventWorkTypeLevelVO> result = new ArrayList<>();
    for (CompetitionEventWorkTypeLevelEntity entity : levelService.replaceAll(
        principal.getTenantId(), eventId, eventWorkTypeId, req, principal.getUserId())) {
      result.add(CompetitionEventWorkTypeLevelConverter.toVO(entity));
    }
    return result;
  }

  private CompetitionEventEntity accessibleEvent(
      PlatformUserPrincipal principal, Long eventId, boolean manage) {
    CompetitionEventEntity event = eventService.getById(principal.getTenantId(), eventId);
    if (PlatformAdminSupport.isSuperAdmin(principal)) {
      return event;
    }
    UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
        .eq(UserEntity::getTenantId, principal.getTenantId())
        .eq(UserEntity::getId, principal.getUserId())
        .eq(UserEntity::getDeleted, 0));
    if (user == null || user.getOrgId() == null) {
      log.warn("Competition level access has no user organization, eventId={}, userId={}, tenantId={}",
          eventId, principal.getUserId(), principal.getTenantId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户无企业数据权限");
    }
    if (CompetitionEventScope.ENTERPRISE_INTERNAL.getCode().equals(event.getScope())) {
      if (Objects.equals(user.getOrgId(), event.getOwnerOrgId())) {
        return event;
      }
      log.warn("Competition level access outside owner organization, eventId={}, userId={}, tenantId={}",
          eventId, principal.getUserId(), principal.getTenantId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该赛事");
    }
    if (manage) {
      log.warn("Enterprise participant attempted to manage platform event levels, eventId={}, userId={}, tenantId={}",
          eventId, principal.getUserId(), principal.getTenantId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "平台赛事开放等级仅平台管理员可管理");
    }
    CompetitionEventOrgEntity relation = eventOrgMapper.selectOne(
        new LambdaQueryWrapper<CompetitionEventOrgEntity>()
            .eq(CompetitionEventOrgEntity::getTenantId, principal.getTenantId())
            .eq(CompetitionEventOrgEntity::getEventId, eventId)
            .eq(CompetitionEventOrgEntity::getOrgId, user.getOrgId())
            .eq(CompetitionEventOrgEntity::getStatus, CompetitionEventOrgStatus.ACTIVE.getCode())
            .eq(CompetitionEventOrgEntity::getDeleted, 0));
    if (relation == null) {
      log.warn("Competition level access without active event organization, eventId={}, userId={}, tenantId={}",
          eventId, principal.getUserId(), principal.getTenantId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "当前企业不是有效赛事参与企业");
    }
    return event;
  }

  private PlatformUserPrincipal currentUser() {
    try {
      return SecurityContext.currentUser();
    } catch (IllegalStateException ex) {
      log.warn("Competition level request has no authenticated platform user");
      throw ex;
    }
  }
}
