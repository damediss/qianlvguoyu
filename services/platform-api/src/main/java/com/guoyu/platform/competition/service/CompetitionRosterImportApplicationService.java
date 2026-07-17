package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.converter.CompetitionRosterImportConverter;
import com.guoyu.platform.competition.dto.CompetitionRosterImportQueryReq;
import com.guoyu.platform.competition.dto.CompetitionRosterImportListQueryReq;
import com.guoyu.platform.competition.dto.CompetitionRosterPrecheckReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventOrgEntity;
import com.guoyu.platform.competition.entity.CompetitionRosterImportEntity;
import com.guoyu.platform.competition.entity.CompetitionRosterItemEntity;
import com.guoyu.platform.competition.enums.CompetitionEventScope;
import com.guoyu.platform.competition.enums.CompetitionEventOrgStatus;
import com.guoyu.platform.competition.vo.CompetitionRosterImportDetailVO;
import com.guoyu.platform.competition.vo.CompetitionRosterImportVO;
import com.guoyu.platform.competition.vo.CompetitionRosterItemVO;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.entity.FileEntity;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.PlatformAdminSupport;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventOrgMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionRosterImportApplicationService {

  private final CompetitionEventService eventService;
  private final CompetitionEventOrgMapper eventOrgMapper;
  private final CompetitionRosterImportService importService;
  private final CompetitionRegistrationService registrationService;
  private final FileApplicationService fileApplicationService;
  private final OrgMapper orgMapper;
  private final UserMapper userMapper;

  public CompetitionRosterImportApplicationService(
      CompetitionEventService eventService,
      CompetitionEventOrgMapper eventOrgMapper,
      CompetitionRosterImportService importService,
      CompetitionRegistrationService registrationService,
      FileApplicationService fileApplicationService,
      OrgMapper orgMapper,
      UserMapper userMapper) {
    this.eventService = eventService;
    this.eventOrgMapper = eventOrgMapper;
    this.importService = importService;
    this.registrationService = registrationService;
    this.fileApplicationService = fileApplicationService;
    this.orgMapper = orgMapper;
    this.userMapper = userMapper;
  }

  @Transactional
  public CompetitionRosterImportDetailVO precheck(Long eventId, CompetitionRosterPrecheckReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionEventEntity event = assertCanImport(principal, eventId, req.orgId());
    FileEntity file = fileApplicationService.getExistingFile(principal.getTenantId(), req.fileId());
    validateExcelFile(file);
    byte[] fileBytes = fileApplicationService.readFileBytesById(req.fileId());
    CompetitionRosterImportEntity batch = importService.precheck(
        principal.getTenantId(), event.getId(), req.orgId(), req.fileId(), fileBytes);
    return detail(principal.getTenantId(), batch, new CompetitionRosterImportQueryReq());
  }

  @Transactional(readOnly = true)
  public CompetitionRosterImportDetailVO getBatch(Long batchId, CompetitionRosterImportQueryReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionRosterImportEntity batch = importService.getById(principal.getTenantId(), batchId);
    assertCanImport(principal, batch.getEventId(), batch.getOrgId());
    return detail(principal.getTenantId(), batch, req);
  }

  @Transactional(readOnly = true)
  public PageResponse<CompetitionRosterImportVO> page(
      Long eventId, CompetitionRosterImportListQueryReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionEventEntity event = eventService.getById(principal.getTenantId(), eventId);
    applyListScope(principal, event, req);
    var page = importService.pageBatches(principal.getTenantId(), eventId, req);
    List<CompetitionRosterImportVO> items = new ArrayList<>();
    if (page.getRecords() != null && !page.getRecords().isEmpty()) {
      List<Long> orgIds = page.getRecords().stream().map(CompetitionRosterImportEntity::getOrgId).distinct().toList();
      Map<Long, OrgEntity> organizations = new HashMap<>();
      List<OrgEntity> orgs = orgMapper.selectList(new LambdaQueryWrapper<OrgEntity>()
          .eq(OrgEntity::getTenantId, principal.getTenantId())
          .in(OrgEntity::getId, orgIds)
          .eq(OrgEntity::getDeleted, 0));
      if (orgs != null) {
        orgs.forEach(org -> organizations.put(org.getId(), org));
      }
      for (CompetitionRosterImportEntity batch : page.getRecords()) {
        OrgEntity org = organizations.get(batch.getOrgId());
        items.add(CompetitionRosterImportConverter.toVO(batch, org == null ? null : org.getName()));
      }
    }
    return new PageResponse<>(items, page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional
  public CompetitionRosterImportDetailVO confirm(Long batchId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionRosterImportEntity batch = importService.getById(principal.getTenantId(), batchId);
    assertCanImport(principal, batch.getEventId(), batch.getOrgId());
    CompetitionRosterImportEntity confirmed = registrationService.confirmAndGenerate(
        principal.getTenantId(), batchId, principal.getUserId());
    return detail(principal.getTenantId(), confirmed, new CompetitionRosterImportQueryReq());
  }

  private CompetitionRosterImportDetailVO detail(
      Long tenantId, CompetitionRosterImportEntity batch, CompetitionRosterImportQueryReq req) {
    var page = importService.pageItems(tenantId, batch.getId(), req);
    List<CompetitionRosterItemVO> items = new ArrayList<>();
    if (page.getRecords() != null) {
      for (CompetitionRosterItemEntity item : page.getRecords()) {
        items.add(CompetitionRosterImportConverter.toItemVO(item));
      }
    }
    CompetitionRosterImportDetailVO detail = CompetitionRosterImportConverter.toDetailVO(batch);
    OrgEntity organization = orgMapper.selectOne(new LambdaQueryWrapper<OrgEntity>()
        .eq(OrgEntity::getTenantId, tenantId)
        .eq(OrgEntity::getId, batch.getOrgId())
        .eq(OrgEntity::getDeleted, 0));
    if (organization != null) {
      detail.setOrgName(organization.getName());
    }
    detail.setItems(new PageResponse<>(items, page.getCurrent(), page.getSize(), page.getTotal()));
    return detail;
  }

  private CompetitionEventEntity assertCanImport(
      PlatformUserPrincipal principal, Long eventId, Long orgId) {
    if (orgId == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "缺少企业组织");
    }
    boolean superAdmin = PlatformAdminSupport.isSuperAdmin(principal);
    Long currentOrgId = superAdmin ? null : currentUserOrgId(principal);
    if (!superAdmin && !currentOrgId.equals(orgId)) {
      log.warn("User attempted roster import for another organization, userId={}, tenantId={}, orgId={}",
          principal.getUserId(), principal.getTenantId(), orgId);
      throw new BusinessException(ErrorCode.FORBIDDEN, "仅当前所属企业可以导入名单");
    }
    ensureTenantOrganization(principal.getTenantId(), orgId);

    CompetitionEventEntity event = eventService.getById(principal.getTenantId(), eventId);
    if (CompetitionEventScope.ENTERPRISE_INTERNAL.getCode().equals(event.getScope())
        && !superAdmin
        && !orgId.equals(event.getOwnerOrgId())) {
      log.warn("User attempted roster import outside enterprise event scope, eventId={}, userId={}, tenantId={}",
          eventId, principal.getUserId(), principal.getTenantId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该赛事");
    }
    if (CompetitionEventScope.PLATFORM.getCode().equals(event.getScope())
        && !superAdmin
        && !hasParticipatingOrganization(principal.getTenantId(), eventId, orgId)) {
      log.warn("Organization is not a participating organization for event, eventId={}, tenantId={}, orgId={}",
          eventId, principal.getTenantId(), orgId);
      throw new BusinessException(ErrorCode.FORBIDDEN, "当前企业不是赛事参与企业");
    }
    return event;
  }

  private void applyListScope(
      PlatformUserPrincipal principal,
      CompetitionEventEntity event,
      CompetitionRosterImportListQueryReq req) {
    if (PlatformAdminSupport.isSuperAdmin(principal)) {
      if (req.getOrgId() != null) ensureTenantOrganization(principal.getTenantId(), req.getOrgId());
      return;
    }
    Long currentOrgId = currentUserOrgId(principal);
    if (req.getOrgId() != null && !currentOrgId.equals(req.getOrgId())) {
      log.warn("User attempted to list roster imports for another organization, userId={}, tenantId={}, orgId={}",
          principal.getUserId(), principal.getTenantId(), req.getOrgId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权查询其他企业的名单批次");
    }
    ensureTenantOrganization(principal.getTenantId(), currentOrgId);
    if (CompetitionEventScope.ENTERPRISE_INTERNAL.getCode().equals(event.getScope())) {
      if (!currentOrgId.equals(event.getOwnerOrgId())) {
        throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该赛事");
      }
    } else if (!hasParticipatingOrganization(principal.getTenantId(), event.getId(), currentOrgId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "当前企业不是赛事参与企业");
    }
    req.setOrgId(currentOrgId);
  }

  private boolean hasParticipatingOrganization(Long tenantId, Long eventId, Long orgId) {
    CompetitionEventOrgEntity relation = eventOrgMapper.selectOne(new LambdaQueryWrapper<CompetitionEventOrgEntity>()
        .eq(CompetitionEventOrgEntity::getTenantId, tenantId)
        .eq(CompetitionEventOrgEntity::getEventId, eventId)
        .eq(CompetitionEventOrgEntity::getOrgId, orgId)
        .ne(CompetitionEventOrgEntity::getStatus, CompetitionEventOrgStatus.CLOSED.getCode())
        .eq(CompetitionEventOrgEntity::getDeleted, 0));
    return relation != null;
  }

  private void ensureTenantOrganization(Long tenantId, Long orgId) {
    OrgEntity org = orgMapper.selectOne(new LambdaQueryWrapper<OrgEntity>()
        .eq(OrgEntity::getTenantId, tenantId)
        .eq(OrgEntity::getId, orgId)
        .eq(OrgEntity::getDeleted, 0));
    if (org == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "企业不存在或不属于当前租户");
    }
  }

  private Long currentUserOrgId(PlatformUserPrincipal principal) {
    UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
        .eq(UserEntity::getTenantId, principal.getTenantId())
        .eq(UserEntity::getId, principal.getUserId()));
    if (user == null || user.getOrgId() == null) {
      log.warn("Roster import user has no organization, userId={}, tenantId={}",
          principal.getUserId(), principal.getTenantId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户无企业数据权限");
    }
    return user.getOrgId();
  }

  private PlatformUserPrincipal currentUser() {
    try {
      return SecurityContext.currentUser();
    } catch (IllegalStateException ex) {
      log.warn("Competition roster import request has no authenticated platform user");
      throw ex;
    }
  }

  private void validateExcelFile(FileEntity file) {
    String name = file == null ? null : file.getOriginalName();
    if (name == null || !name.toLowerCase(java.util.Locale.ROOT).matches(".+\\.(xlsx|xls)$")) {
      log.warn("Competition roster precheck rejected non-Excel file, fileId={}", file == null ? null : file.getId());
      throw new BusinessException(ErrorCode.BAD_REQUEST, "名单文件仅支持 .xlsx 或 .xls");
    }
  }
}
