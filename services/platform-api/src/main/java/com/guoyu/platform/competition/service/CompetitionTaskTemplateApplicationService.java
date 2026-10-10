package com.guoyu.platform.competition.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.competition.converter.CompetitionTaskTemplateConverter;
import com.guoyu.platform.competition.dto.CompetitionTaskTemplateCreateReq;
import com.guoyu.platform.competition.dto.CompetitionTaskTemplateQueryReq;
import com.guoyu.platform.competition.dto.CompetitionTaskTemplateUpdateReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.entity.CompetitionTaskTemplateEntity;
import com.guoyu.platform.competition.enums.CompetitionEventScope;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.vo.CompetitionTaskTemplateVO;
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
public class CompetitionTaskTemplateApplicationService {

  private final CompetitionEventService eventService;
  private final CompetitionStageService stageService;
  private final CompetitionTaskTemplateService templateService;
  private final UserMapper userMapper;

  public CompetitionTaskTemplateApplicationService(
      CompetitionEventService eventService,
      CompetitionStageService stageService,
      CompetitionTaskTemplateService templateService,
      UserMapper userMapper) {
    this.eventService = eventService;
    this.stageService = stageService;
    this.templateService = templateService;
    this.userMapper = userMapper;
  }

  @Transactional(readOnly = true)
  public PageResponse<CompetitionTaskTemplateVO> page(Long stageId, CompetitionTaskTemplateQueryReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionStageEntity stage = accessibleStage(principal, stageId);
    var page = templateService.page(principal.getTenantId(), stage.getId(), req);
    List<CompetitionTaskTemplateVO> items = new ArrayList<>();
    if (page.getRecords() != null) {
      page.getRecords().forEach(template -> items.add(CompetitionTaskTemplateConverter.toVO(template)));
    }
    return new PageResponse<>(items, page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional
  public CompetitionTaskTemplateVO create(Long stageId, CompetitionTaskTemplateCreateReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionStageEntity stage = accessibleDraftStage(principal, stageId);
    return CompetitionTaskTemplateConverter.toVO(
        templateService.create(principal.getTenantId(), stage.getId(), req));
  }

  @Transactional
  public CompetitionTaskTemplateVO update(Long templateId, CompetitionTaskTemplateUpdateReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionTaskTemplateEntity template = templateService.getById(principal.getTenantId(), templateId);
    accessibleDraftStage(principal, template.getStageId());
    return CompetitionTaskTemplateConverter.toVO(
        templateService.update(principal.getTenantId(), templateId, req));
  }

  private CompetitionStageEntity accessibleDraftStage(
      PlatformUserPrincipal principal, Long stageId) {
    CompetitionStageEntity stage = stageService.getById(principal.getTenantId(), stageId);
    CompetitionEventEntity event = accessibleEvent(principal, stage.getEventId());
    if (!"draft".equals(event.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "只有草稿状态赛事允许修改任务模板");
    }
    return stage;
  }

  private CompetitionStageEntity accessibleStage(PlatformUserPrincipal principal, Long stageId) {
    CompetitionStageEntity stage = stageService.getById(principal.getTenantId(), stageId);
    accessibleEvent(principal, stage.getEventId());
    return stage;
  }

  private CompetitionEventEntity accessibleEvent(PlatformUserPrincipal principal, Long eventId) {
    CompetitionEventEntity event = eventService.getById(principal.getTenantId(), eventId);
    if (CompetitionEventScope.ENTERPRISE_INTERNAL.getCode().equals(event.getScope())
        && !currentUserOrgId(principal).equals(event.getOwnerOrgId())) {
      log.warn("User attempted to access task templates outside organization scope, eventId={}, userId={}, tenantId={}",
          eventId, principal.getUserId(), principal.getTenantId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该赛事");
    }
    return event;
  }

  private Long currentUserOrgId(PlatformUserPrincipal principal) {
    UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
        .eq(UserEntity::getTenantId, principal.getTenantId())
        .eq(UserEntity::getId, principal.getUserId()));
    if (user == null || user.getOrgId() == null) {
      log.warn("Task template access has no user organization, userId={}, tenantId={}",
          principal.getUserId(), principal.getTenantId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户无企业数据权限");
    }
    return user.getOrgId();
  }

  private PlatformUserPrincipal currentUser() {
    try {
      return SecurityContext.currentUser();
    } catch (IllegalStateException ex) {
      log.warn("Competition task template request has no authenticated platform user");
      throw ex;
    }
  }
}
