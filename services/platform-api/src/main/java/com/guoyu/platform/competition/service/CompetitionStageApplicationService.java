package com.guoyu.platform.competition.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.converter.CompetitionStageConverter;
import com.guoyu.platform.competition.dto.CompetitionStageCreateReq;
import com.guoyu.platform.competition.dto.CompetitionStageQueryReq;
import com.guoyu.platform.competition.dto.CompetitionStageUpdateReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.enums.CompetitionEventScope;
import com.guoyu.platform.competition.enums.CompetitionEventStatus;
import com.guoyu.platform.competition.vo.CompetitionStageVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionStageApplicationService {

  private final CompetitionEventService eventService;
  private final CompetitionStageService stageService;
  private final UserMapper userMapper;

  public CompetitionStageApplicationService(
      CompetitionEventService eventService,
      CompetitionStageService stageService,
      UserMapper userMapper) {
    this.eventService = eventService;
    this.stageService = stageService;
    this.userMapper = userMapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<CompetitionStageVO> page(Long eventId, CompetitionStageQueryReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionEventEntity event = accessibleEvent(principal, eventId);
    var page = stageService.page(principal.getTenantId(), event.getId(), req);
    List<CompetitionStageVO> items = new ArrayList<>();
    if (page.getRecords() != null) {
      page.getRecords().forEach(stage -> items.add(CompetitionStageConverter.toVO(stage)));
    }
    return new PageResponse<>(items, page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional
  public CompetitionStageVO create(Long eventId, CompetitionStageCreateReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionEventEntity event = accessibleEvent(principal, eventId);
    requireDraftEvent(event);
    return CompetitionStageConverter.toVO(stageService.create(principal.getTenantId(), event.getId(), req));
  }

  @Transactional
  public CompetitionStageVO update(Long stageId, CompetitionStageUpdateReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionStageEntity stage = stageService.getById(principal.getTenantId(), stageId);
    CompetitionEventEntity event = accessibleEvent(principal, stage.getEventId());
    requireDraftEvent(event);
    return CompetitionStageConverter.toVO(stageService.update(principal.getTenantId(), stageId, req));
  }

  private CompetitionEventEntity accessibleEvent(PlatformUserPrincipal principal, Long eventId) {
    CompetitionEventEntity event = eventService.getById(principal.getTenantId(), eventId);
    if (CompetitionEventScope.ENTERPRISE_INTERNAL.getCode().equals(event.getScope())
        && !currentUserOrgId(principal).equals(event.getOwnerOrgId())) {
      log.warn("User attempted to access stages outside organization scope, eventId={}, userId={}, tenantId={}",
          eventId, principal.getUserId(), principal.getTenantId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该赛事");
    }
    return event;
  }

  private void requireDraftEvent(CompetitionEventEntity event) {
    if (!CompetitionEventStatus.DRAFT.getCode().equals(event.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "只有草稿状态赛事允许修改赛段");
    }
  }

  private Long currentUserOrgId(PlatformUserPrincipal principal) {
    UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
        .eq(UserEntity::getTenantId, principal.getTenantId())
        .eq(UserEntity::getId, principal.getUserId()));
    if (user == null || user.getOrgId() == null) {
      log.warn("Stage access has no user organization, userId={}, tenantId={}",
          principal.getUserId(), principal.getTenantId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户无企业数据权限");
    }
    return user.getOrgId();
  }

  private PlatformUserPrincipal currentUser() {
    try {
      return SecurityContext.currentUser();
    } catch (IllegalStateException ex) {
      log.warn("Competition stage request has no authenticated platform user");
      throw ex;
    }
  }
}
