package com.guoyu.platform.competition.service;

import com.guoyu.platform.common.util.SensitiveMaskUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.dto.CompetitionRefereeAssignmentReplaceReq;
import com.guoyu.platform.competition.dto.CompetitionRefereeBatchAssignmentReplaceReq;
import com.guoyu.platform.competition.dto.CompetitionRefereeCreateReq;
import com.guoyu.platform.competition.dto.CompetitionRefereeGroupBindingReq;
import com.guoyu.platform.competition.dto.CompetitionRefereeGroupUpsertReq;
import com.guoyu.platform.competition.dto.CompetitionRefereeQualificationReq;
import com.guoyu.platform.competition.dto.CompetitionRefereeQueryReq;
import com.guoyu.platform.competition.dto.CompetitionRefereeUpdateReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventMemberEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.entity.CompetitionPlayerIdentityEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalAssignmentEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalSessionEntity;
import com.guoyu.platform.competition.entity.CompetitionRefereeAssignmentEntity;
import com.guoyu.platform.competition.entity.CompetitionRefereeProfileEntity;
import com.guoyu.platform.competition.entity.CompetitionRefereeQualificationEntity;
import com.guoyu.platform.competition.domain.CompetitionRefereeGroupScheduleGuard;
import com.guoyu.platform.competition.entity.CompetitionRefereeGroupEntity;
import com.guoyu.platform.competition.entity.CompetitionRefereeGroupMemberEntity;
import com.guoyu.platform.competition.entity.CompetitionScoreSourceEntity;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.enums.CompetitionScoreSourceType;
import com.guoyu.platform.competition.mapper.CompetitionEventMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventMemberMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalAssignmentMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalSessionMapper;
import com.guoyu.platform.competition.mapper.CompetitionRefereeAssignmentMapper;
import com.guoyu.platform.competition.mapper.CompetitionRefereeProfileMapper;
import com.guoyu.platform.competition.mapper.CompetitionRefereeQualificationMapper;
import com.guoyu.platform.competition.mapper.CompetitionRefereeQualificationRow;
import com.guoyu.platform.competition.mapper.CompetitionRefereeQueryRow;
import com.guoyu.platform.competition.mapper.CompetitionRefereeGroupMapper;
import com.guoyu.platform.competition.mapper.CompetitionRefereeGroupMemberMapper;
import com.guoyu.platform.competition.mapper.CompetitionScoreSourceMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageMapper;
import com.guoyu.platform.competition.mapper.CompetitionStagePaperAssignmentMapper;
import com.guoyu.platform.competition.mapper.CompetitionStagePaperAssignmentPlayerRow;
import com.guoyu.platform.competition.player.domain.CompetitionPlayerIdentityDomainService;
import com.guoyu.platform.competition.vo.CompetitionRefereeAssignmentVO;
import com.guoyu.platform.competition.vo.CompetitionRefereeAssignmentWorkspaceVO;
import com.guoyu.platform.competition.vo.CompetitionRefereeBatchAssignmentResultVO;
import com.guoyu.platform.competition.vo.CompetitionRefereeCandidateVO;
import com.guoyu.platform.competition.vo.CompetitionRefereeGroupBoundTaskVO;
import com.guoyu.platform.competition.vo.CompetitionRefereeGroupMemberVO;
import com.guoyu.platform.competition.vo.CompetitionRefereeGroupVO;
import com.guoyu.platform.competition.vo.CompetitionRefereeQualificationVO;
import com.guoyu.platform.competition.vo.CompetitionRefereeVO;
import com.guoyu.platform.competition.vo.CompetitionStageVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import com.guoyu.platform.system.service.UserApplicationService;
import com.guoyu.platform.system.mapper.RoleMapper;
import com.guoyu.platform.system.entity.RoleEntity;
import com.guoyu.platform.system.dto.UserCreateReq;

@Service
@Slf4j
public class CompetitionRefereeManagementService {

  private static final String REFEREE = "referee";
  private static final String ACTIVE = "active";
  private static final String WITHDRAWN = "withdrawn";
  private static final String OFFICIAL = "official";
  private static final String PRACTICAL = "practical";
  private static final String GROUP_DRAFT = "draft";
  private static final String GROUP_CONFIRMED = "confirmed";
  private static final String GROUP_CLOSED = "closed";
  private static final String GROUP_MEMBER = "member";
  private static final String GROUP_LEADER = "leader";
  private static final String TASK_PENDING = "pending";
  private static final String TASK_IN_PROGRESS = "in_progress";
  private static final String TASK_COMPLETED = "completed";
  private static final String INITIAL_PASSWORD = "12345678";
  private static final Set<String> STANDARD_LEVEL_CODES = Set.of("1", "2", "3", "4", "5");

  private final CompetitionEventWorkTypeDomainService eventWorkTypeService;
  private final CompetitionEventWorkTypeLevelService eventWorkTypeLevelService;
  private final CompetitionStageApplicationService stageApplicationService;
  private final CompetitionRefereeProfileMapper profileMapper;
  private final CompetitionRefereeQualificationMapper qualificationMapper;
  private final CompetitionEventMemberMapper memberMapper;
  private final CompetitionRefereeAssignmentMapper assignmentMapper;
  private final CompetitionRefereeGroupMapper groupMapper;
  private final CompetitionRefereeGroupMemberMapper groupMemberMapper;
  private final CompetitionStageMapper stageMapper;
  private final CompetitionStagePaperAssignmentMapper stagePaperAssignmentMapper;
  private final CompetitionEventMapper eventMapper;
  private final CompetitionPracticalAssignmentMapper practicalAssignmentMapper;
  private final CompetitionPracticalSessionMapper practicalSessionMapper;
  private final CompetitionScoreSourceMapper scoreSourceMapper;
  private final CompetitionPlayerIdentityDomainService identityService;
  private UserApplicationService userApplicationService;
  private RoleMapper roleMapper;

  @Autowired(required = false)
  public void setAccountDependencies(
      UserApplicationService userApplicationService, RoleMapper roleMapper) {
    this.userApplicationService = userApplicationService;
    this.roleMapper = roleMapper;
  }

  public CompetitionRefereeManagementService(
      CompetitionEventWorkTypeDomainService eventWorkTypeService,
      CompetitionEventWorkTypeLevelService eventWorkTypeLevelService,
      CompetitionStageApplicationService stageApplicationService,
      CompetitionRefereeProfileMapper profileMapper,
      CompetitionRefereeQualificationMapper qualificationMapper,
      CompetitionEventMemberMapper memberMapper,
      CompetitionRefereeAssignmentMapper assignmentMapper,
      CompetitionRefereeGroupMapper groupMapper,
      CompetitionRefereeGroupMemberMapper groupMemberMapper,
      CompetitionStageMapper stageMapper,
      CompetitionStagePaperAssignmentMapper stagePaperAssignmentMapper,
      CompetitionEventMapper eventMapper,
      CompetitionPracticalAssignmentMapper practicalAssignmentMapper,
      CompetitionPracticalSessionMapper practicalSessionMapper,
      CompetitionScoreSourceMapper scoreSourceMapper,
      CompetitionPlayerIdentityDomainService identityService) {
    this.eventWorkTypeService = eventWorkTypeService;
    this.eventWorkTypeLevelService = eventWorkTypeLevelService;
    this.stageApplicationService = stageApplicationService;
    this.profileMapper = profileMapper;
    this.qualificationMapper = qualificationMapper;
    this.memberMapper = memberMapper;
    this.assignmentMapper = assignmentMapper;
    this.groupMapper = groupMapper;
    this.groupMemberMapper = groupMemberMapper;
    this.stageMapper = stageMapper;
    this.stagePaperAssignmentMapper = stagePaperAssignmentMapper;
    this.eventMapper = eventMapper;
    this.practicalAssignmentMapper = practicalAssignmentMapper;
    this.practicalSessionMapper = practicalSessionMapper;
    this.scoreSourceMapper = scoreSourceMapper;
    this.identityService = identityService;
  }

  @Transactional(readOnly = true)
  public PageResponse<CompetitionRefereeVO> page(CompetitionRefereeQueryReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionRefereeQueryReq query = req == null ? new CompetitionRefereeQueryReq() : req;
    Page<CompetitionRefereeQueryRow> page = profileMapper.selectPage(
        new Page<>(query.getPage(), query.getPageSize()), principal.getTenantId(), null,
        query.getEventId(), query.getWorkTypeId(), normalizeOptional(query.getStatus()),
        normalizeOptional(query.getKeyword()));
    if (page == null) {
      log.warn("Competition referee mapper returned null page, tenantId={}", principal.getTenantId());
      return PageResponse.empty(query.getPage(), query.getPageSize());
    }
    List<CompetitionRefereeQueryRow> rows = safe(page.getRecords()).stream()
        .filter(Objects::nonNull).toList();
    Map<Long, List<CompetitionRefereeQualificationVO>> qualifications = qualificationVOs(
        principal.getTenantId(), rows.stream().map(CompetitionRefereeQueryRow::getProfileId).toList());
    List<CompetitionRefereeVO> items = rows.stream()
        .map(row -> toVO(row, false, qualifications.get(row.getProfileId())))
        .toList();
    return new PageResponse<>(items, page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional(readOnly = true)
  public CompetitionRefereeVO getForEdit(Long profileId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionRefereeQueryRow row = findRow(principal.getTenantId(), profileId);
    return toVO(row, true, qualificationVOs(
        principal.getTenantId(), List.of(profileId)).get(profileId));
  }

  @Transactional
  public CompetitionRefereeVO create(CompetitionRefereeCreateReq req) {
    PlatformUserPrincipal principal = currentUser();
    List<QualificationInput> qualifications = normalizeQualifications(
        principal.getTenantId(), req == null ? null : req.qualifications());
    CompetitionPlayerIdentityEntity identity = identityService.resolveOrCreate(
        principal.getTenantId(), req.idCardNo(), req.mobile());
    if (!findProfilesByPerson(principal.getTenantId(), identity.getId()).isEmpty()) {
      throw new BusinessException(ErrorCode.CONFLICT, "该身份证号已创建裁判，无需重复创建");
    }

    CompetitionRefereeProfileEntity profile = new CompetitionRefereeProfileEntity();
    profile.setId(IdWorker.getId());
    profile.setTenantId(principal.getTenantId());
    profile.setPersonId(identity.getId());
    profile.setMemberId(null);
    profile.setRefereeName(req.name().trim());
    profile.setStatus(ACTIVE);
    profile.setDeleted(0);
    profile.setCreatedBy(principal.getUserId());
    profile.setUpdatedBy(principal.getUserId());
    try {
      if (profileMapper.insert(profile) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "裁判信息创建失败，请刷新后重试");
      }
    } catch (DuplicateKeyException ex) {
      throw new BusinessException(ErrorCode.CONFLICT, "该身份证号已创建裁判，无需重复创建");
    }
    createAccountForProfile(principal.getTenantId(), principal.getUserId(), profile,
        req.mobile().trim());
    replaceQualifications(principal.getTenantId(), profile.getId(), qualifications,
        principal.getUserId());
    return getForEdit(profile.getId());
  }

  @Transactional
  public CompetitionRefereeVO update(Long profileId, CompetitionRefereeUpdateReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionRefereeQueryRow row = findRow(principal.getTenantId(), profileId);
    List<QualificationInput> qualifications = normalizeQualifications(
        principal.getTenantId(), req == null ? null : req.qualifications());
    List<CompetitionRefereeQualificationEntity> existing = qualificationMapper.selectList(
        new LambdaQueryWrapper<CompetitionRefereeQualificationEntity>()
            .eq(CompetitionRefereeQualificationEntity::getTenantId, principal.getTenantId())
            .eq(CompetitionRefereeQualificationEntity::getRefereeProfileId, profileId)
            .eq(CompetitionRefereeQualificationEntity::getDeleted, 0));
    if (!sameQualifications(existing, qualifications)) {
      assertNoActiveAssignments(principal.getTenantId(), row.getPersonId());
    }
    identityService.updateSensitiveData(
        principal.getTenantId(), row.getPersonId(), req.idCardNo(), req.mobile());
    CompetitionRefereeProfileEntity profile = findProfile(principal.getTenantId(), profileId);
    profile.setPersonId(row.getPersonId());
    profile.setRefereeName(req.name().trim());
    profile.setStatus(ACTIVE);
    profile.setUpdatedBy(principal.getUserId());
    if (profileMapper.updateById(profile) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "裁判信息已发生变化，请刷新后重试");
    }
    replaceQualifications(principal.getTenantId(), profileId, qualifications, principal.getUserId());
    return getForEdit(profileId);
  }

  private void createAccountForProfile(
      Long tenantId, Long operatorId, CompetitionRefereeProfileEntity profile, String mobile) {
    if (userApplicationService == null || roleMapper == null) {
      throw new BusinessException(ErrorCode.CONFLICT, "裁判账号服务未就绪");
    }
    RoleEntity role = roleMapper.selectOne(new LambdaQueryWrapper<RoleEntity>()
        .eq(RoleEntity::getTenantId, tenantId)
        .eq(RoleEntity::getCode, "COMPETITION_PRACTICAL_REFEREE")
        .eq(RoleEntity::getStatus, "enabled")
        .eq(RoleEntity::getDeleted, 0));
    if (role == null) {
      throw new BusinessException(ErrorCode.CONFLICT, "竞赛实操裁判角色尚未初始化");
    }
    var user = userApplicationService.create(new UserCreateReq(
        null, null, mobile, INITIAL_PASSWORD, profile.getRefereeName(),
        "competition_referee", mobile, null, "enabled", List.of(role.getId())));
    profile.setUserId(user.id());
    profile.setAccountUsername(user.username());
    profile.setUpdatedBy(operatorId);
    if (profileMapper.updateById(profile) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "裁判账号关联失败");
    }
  }

  @Transactional
  public void delete(Long profileId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionRefereeQueryRow row = findRow(principal.getTenantId(), profileId);
    assertNoActiveAssignments(principal.getTenantId(), row.getPersonId());
    assertNoActiveGroupMembership(principal.getTenantId(), profileId);
    if (profileMapper.deleteById(profileId) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "裁判信息已发生变化，请刷新后重试");
    }
    List<CompetitionEventMemberEntity> members = findMembersByPerson(
        principal.getTenantId(), row.getPersonId());
    for (CompetitionEventMemberEntity member : members) {
      member.setStatus(WITHDRAWN);
      member.setUpdatedBy(principal.getUserId());
      if (memberMapper.updateById(member) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "裁判赛事角色已发生变化，请刷新后重试");
      }
    }
  }

  @Transactional(readOnly = true)
  public CompetitionRefereeAssignmentWorkspaceVO assignmentWorkspace(Long stageId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionStageVO stage = stageApplicationService.get(stageId);
    Scope scope = requireScopeByLevelCode(principal.getTenantId(), stage.getEventId(),
        stage.getEventWorkTypeId(), stage.getEventWorkTypeLevelCode());
    List<CompetitionRefereeQueryRow> refereeRows = profileMapper.selectQualifiedForStage(
        principal.getTenantId(), stage.getEventId(), stage.getEventWorkTypeId(),
        scope.level().getLevelCode());
    Map<Long, CompetitionRefereeQueryRow> refereeByMemberId = safe(refereeRows).stream()
        .filter(row -> row != null && row.getMemberId() != null)
        .collect(Collectors.toMap(CompetitionRefereeQueryRow::getMemberId, Function.identity(),
            (left, right) -> left, LinkedHashMap::new));

    List<CompetitionStagePaperAssignmentPlayerRow> candidateRows = eligibleCandidates(
        principal.getTenantId(), stage, scope);
    List<CompetitionRefereeAssignmentEntity> assignments = activeAssignments(
        principal.getTenantId(), stage, scope.level().getId());
    Map<Long, List<CompetitionRefereeAssignmentEntity>> assignmentsByContestant = assignments.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.groupingBy(CompetitionRefereeAssignmentEntity::getContestantMemberId,
            LinkedHashMap::new, Collectors.toList()));
    Map<Long, CompetitionRefereeQueryRow> refereeByProfileId = safe(refereeRows).stream()
        .filter(row -> row != null && row.getProfileId() != null)
        .collect(Collectors.toMap(CompetitionRefereeQueryRow::getProfileId, Function.identity(),
            (left, right) -> left, LinkedHashMap::new));
    List<CompetitionRefereeGroupVO> groups = annotateBindability(
        principal.getTenantId(), stage, scope, toGroupVOs(
            confirmedTenantGroups(principal.getTenantId()), refereeByMemberId, refereeByProfileId));
    Map<Long, CompetitionRefereeGroupVO> groupById = groups.stream()
        .filter(item -> item.getId() != null)
        .collect(Collectors.toMap(CompetitionRefereeGroupVO::getId, Function.identity(),
            (left, right) -> left, LinkedHashMap::new));

    CompetitionRefereeAssignmentWorkspaceVO result = new CompetitionRefereeAssignmentWorkspaceVO();
    result.setStageId(stage.getId());
    result.setEventId(stage.getEventId());
    result.setEventWorkTypeId(stage.getEventWorkTypeId());
    result.setEventWorkTypeLevelId(scope.level().getId());
    result.setStageName(stage.getName());
    result.setWorkTypeName(scope.workType().getDisplayName());
    result.setLevelName(displayLevel(scope.level()));
    result.setReferees(safe(refereeRows).stream()
        .filter(Objects::nonNull)
        .map(row -> toVO(row, false, List.of()))
        .toList());
    result.setGroups(groups);
    List<CompetitionRefereeCandidateVO> candidates = new ArrayList<>();
    for (CompetitionStagePaperAssignmentPlayerRow row : candidateRows) {
      if (row == null) continue;
      candidates.add(toCandidate(row, assignmentsByContestant.get(row.getContestantMemberId()),
          refereeByMemberId, groupById));
    }
    result.setCandidates(candidates);
    return result;
  }

  @Transactional(readOnly = true)
  public List<CompetitionRefereeGroupVO> listGroups() {
    PlatformUserPrincipal principal = currentUser();
    Map<Long, CompetitionRefereeQueryRow> refereeByProfileId = tenantRefereesByProfile(
        principal.getTenantId());
    return toGroupVOs(tenantGroups(principal.getTenantId()), Map.of(), refereeByProfileId);
  }

  @Transactional(readOnly = true)
  public List<CompetitionRefereeGroupVO> groups(Long stageId) {
    return assignmentWorkspace(stageId).getGroups();
  }

  @Transactional
  public CompetitionRefereeGroupVO createGroup(CompetitionRefereeGroupUpsertReq req) {
    PlatformUserPrincipal principal = currentUser();
    ResolvedGroupInput input = resolveGroupInput(principal, req);
    int nextGroupNo = tenantGroups(principal.getTenantId()).stream()
        .map(CompetitionRefereeGroupEntity::getGroupNo)
        .filter(Objects::nonNull)
        .max(Integer::compareTo)
        .orElse(0) + 1;
    CompetitionRefereeGroupEntity group = new CompetitionRefereeGroupEntity();
    group.setId(IdWorker.getId());
    group.setTenantId(principal.getTenantId());
    group.setGroupNo(nextGroupNo);
    group.setGroupName(input.groupName());
    group.setLeaderRefereeProfileId(input.leaderProfileId());
    group.setStatus(GROUP_DRAFT);
    group.setCreatedBy(principal.getUserId());
    group.setUpdatedBy(principal.getUserId());
    group.setDeleted(0);
    if (groupMapper.insert(group) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "裁判小组创建失败，请刷新后重试");
    }
    replaceGroupMembers(principal, group, input.profileIds());
    return annotatedGroupVO(group, Map.of(), input.refereeByProfileId());
  }

  @Transactional
  public CompetitionRefereeGroupVO updateGroup(
      Long groupId, CompetitionRefereeGroupUpsertReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionRefereeGroupEntity group = findGroup(principal.getTenantId(), groupId);
    if (GROUP_CLOSED.equals(group.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "已解散的裁判小组不能修改");
    }
    if (!GROUP_DRAFT.equals(group.getStatus()) && hasActiveGroupBindings(principal.getTenantId(), groupId)) {
      throw new BusinessException(ErrorCode.CONFLICT, "已绑定赛段的裁判小组不能直接修改，请先解除选手绑定");
    }
    ResolvedGroupInput input = resolveGroupInput(principal, req);
    group.setGroupName(input.groupName());
    group.setLeaderRefereeProfileId(input.leaderProfileId());
    group.setLeaderRefereeMemberId(null);
    group.setUpdatedBy(principal.getUserId());
    if (groupMapper.updateById(group) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "裁判小组已发生变化，请刷新后重试");
    }
    replaceGroupMembers(principal, group, input.profileIds());
    return annotatedGroupVO(group, Map.of(), input.refereeByProfileId());
  }

  @Transactional
  public CompetitionRefereeGroupVO confirmGroup(Long groupId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionRefereeGroupEntity group = findGroup(principal.getTenantId(), groupId);
    Map<Long, CompetitionRefereeQueryRow> refereeByProfileId = tenantRefereesByProfile(
        principal.getTenantId());
    if (GROUP_CONFIRMED.equals(group.getStatus())) {
      return annotatedGroupVO(group, Map.of(), refereeByProfileId);
    }
    if (GROUP_CLOSED.equals(group.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "已解散的裁判小组不能确定");
    }
    if (!GROUP_DRAFT.equals(group.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前裁判小组不能确定");
    }
    List<CompetitionRefereeGroupMemberEntity> members = activeGroupMembers(
        principal.getTenantId(), group.getId());
    if (members.size() != 3) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "每个裁判小组必须配置3名裁判");
    }
    if (group.getLeaderRefereeProfileId() == null
        || members.stream().noneMatch(item -> Objects.equals(
            item.getRefereeProfileId(), group.getLeaderRefereeProfileId()))) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "请选择一名已配置的裁判作为组长");
    }
    group.setStatus(GROUP_CONFIRMED);
    group.setUpdatedBy(principal.getUserId());
    if (groupMapper.updateById(group) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "裁判小组已发生变化，请刷新后重试");
    }
    return annotatedGroupVO(group, Map.of(), refereeByProfileId);
  }

  @Transactional
  public void deleteGroup(Long groupId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionRefereeGroupEntity group = findGroup(principal.getTenantId(), groupId);
    if (hasActiveGroupBindings(principal.getTenantId(), groupId)) {
      throw new BusinessException(ErrorCode.CONFLICT,
          "已绑定赛段的裁判小组不能删除，请先解除选手绑定或在任务完成后解散小组");
    }
    List<CompetitionRefereeGroupMemberEntity> members = activeGroupMembers(
        principal.getTenantId(), group.getId());
    for (CompetitionRefereeGroupMemberEntity member : members) {
      if (groupMemberMapper.deleteById(member.getId()) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "裁判小组成员已发生变化，请刷新后重试");
      }
    }
    if (groupMapper.deleteById(group.getId()) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "裁判小组已发生变化，请刷新后重试");
    }
  }

  @Transactional
  public CompetitionRefereeGroupVO releaseGroup(Long groupId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionRefereeGroupEntity group = findGroup(principal.getTenantId(), groupId);
    if (GROUP_CLOSED.equals(group.getStatus())) {
      return annotatedGroupVO(group, Map.of(), tenantRefereesByProfile(principal.getTenantId()));
    }
    if (!GROUP_CONFIRMED.equals(group.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "只有已确定的裁判小组才能解散");
    }
    CompetitionRefereeGroupVO preview = annotatedGroupVO(
        group, Map.of(), tenantRefereesByProfile(principal.getTenantId()));
    if (preview.getBoundTasks() == null || preview.getBoundTasks().isEmpty()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "当前裁判小组尚未绑定赛段任务，请直接删除");
    }
    if (!Boolean.TRUE.equals(preview.getReleasable())) {
      throw new BusinessException(ErrorCode.CONFLICT,
          "分配到的赛段任务尚未全部完成，不能解散裁判小组");
    }
    LocalDateTime now = LocalDateTime.now();
    for (CompetitionRefereeAssignmentEntity assignment : groupAssignments(
        principal.getTenantId(), group.getId(), true)) {
      if (assignment == null || !ACTIVE.equals(assignment.getStatus())) continue;
      assignment.setStatus(WITHDRAWN);
      assignment.setEndedAt(now);
      assignment.setUpdatedBy(principal.getUserId());
      if (assignmentMapper.updateById(assignment) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "裁判组绑定已发生变化，请刷新后重试");
      }
    }
    List<CompetitionRefereeGroupMemberEntity> members = activeGroupMembers(
        principal.getTenantId(), group.getId());
    for (CompetitionRefereeGroupMemberEntity member : members) {
      if (groupMemberMapper.deleteById(member.getId()) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "裁判小组成员已发生变化，请刷新后重试");
      }
    }
    group.setStatus(GROUP_CLOSED);
    group.setUpdatedBy(principal.getUserId());
    if (groupMapper.updateById(group) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "裁判小组已发生变化，请刷新后重试");
    }
    return annotatedGroupVO(group, Map.of(), tenantRefereesByProfile(principal.getTenantId()));
  }

  @Transactional
  public CompetitionRefereeCandidateVO bindGroup(
      Long stageId, Long contestantMemberId, CompetitionRefereeGroupBindingReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionStageVO stage = stageApplicationService.assertCanManage(stageId);
    Scope scope = requireScopeByLevelCode(principal.getTenantId(), stage.getEventId(),
        stage.getEventWorkTypeId(), stage.getEventWorkTypeLevelCode());
    requireEligibleCandidate(principal.getTenantId(), stage, scope, contestantMemberId);
    Long groupId = req == null ? null : req.refereeGroupId();
    if (groupId == null) {
      replaceAssignmentsInternal(principal, stage, scope, contestantMemberId,
          Set.of(), Map.of(), null);
    } else {
      CompetitionRefereeGroupEntity group = findGroup(principal.getTenantId(), groupId);
      Map<Long, Long> memberIdByProfileId = resolveBindableGroupMembers(principal, stage, scope, group);
      Set<Long> profileIds = new LinkedHashSet<>(memberIdByProfileId.keySet());
      replaceAssignmentsInternal(principal, stage, scope, contestantMemberId,
          profileIds, memberIdByProfileId, group.getId());
    }
    return assignmentWorkspace(stageId).getCandidates().stream()
        .filter(item -> Objects.equals(item.getContestantMemberId(), contestantMemberId))
        .findFirst()
        .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "裁判组绑定结果读取失败"));
  }

  private ResolvedGroupInput resolveGroupInput(
      PlatformUserPrincipal principal, CompetitionRefereeGroupUpsertReq req) {
    if (req == null || !StringUtils.hasText(req.groupName())
        || req.refereeProfileIds() == null || req.refereeProfileIds().isEmpty()
        || req.leaderRefereeProfileId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "裁判小组名称、裁判和组长不能为空");
    }
    List<Long> profileIds = req.refereeProfileIds();
    if (profileIds.stream().anyMatch(Objects::isNull)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "裁判不能为空");
    }
    LinkedHashSet<Long> uniqueProfileIds = new LinkedHashSet<>(profileIds);
    if (uniqueProfileIds.size() != profileIds.size()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "裁判不能重复选择");
    }
    if (uniqueProfileIds.size() != 3) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "每个裁判小组必须配置3名裁判");
    }
    if (!uniqueProfileIds.contains(req.leaderRefereeProfileId())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "组长必须从已配置的裁判中选择");
    }
    Map<Long, CompetitionRefereeQueryRow> rowByProfileId = tenantRefereesByProfile(
        principal.getTenantId());
    for (Long profileId : uniqueProfileIds) {
      CompetitionRefereeQueryRow row = rowByProfileId.get(profileId);
      if (row == null || !ACTIVE.equals(row.getStatus())) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "所选裁判不存在或已停用");
      }
    }
    return new ResolvedGroupInput(req.groupName().trim(),
        new ArrayList<>(uniqueProfileIds), req.leaderRefereeProfileId(), rowByProfileId);
  }

  private void replaceGroupMembers(
      PlatformUserPrincipal principal,
      CompetitionRefereeGroupEntity group,
      List<Long> profileIds) {
    assertProfilesAvailable(principal.getTenantId(), group.getId(), profileIds);
    List<CompetitionRefereeGroupMemberEntity> existing = groupMemberMapper.selectList(
        new LambdaQueryWrapper<CompetitionRefereeGroupMemberEntity>()
            .eq(CompetitionRefereeGroupMemberEntity::getTenantId, principal.getTenantId())
            .eq(CompetitionRefereeGroupMemberEntity::getGroupId, group.getId())
            .eq(CompetitionRefereeGroupMemberEntity::getDeleted, 0));
    for (CompetitionRefereeGroupMemberEntity item : safe(existing)) {
      if (item != null && groupMemberMapper.deleteById(item.getId()) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "裁判小组成员已发生变化，请刷新后重试");
      }
    }
    Set<Long> uniqueProfileIds = new LinkedHashSet<>(profileIds == null ? List.of() : profileIds);
    for (Long profileId : uniqueProfileIds) {
      CompetitionRefereeGroupMemberEntity item = new CompetitionRefereeGroupMemberEntity();
      item.setId(IdWorker.getId());
      item.setTenantId(principal.getTenantId());
      item.setGroupId(group.getId());
      item.setRefereeProfileId(profileId);
      item.setMemberRole(Objects.equals(profileId, group.getLeaderRefereeProfileId())
          ? GROUP_LEADER : GROUP_MEMBER);
      item.setStatus(ACTIVE);
      item.setCreatedBy(principal.getUserId());
      item.setUpdatedBy(principal.getUserId());
      item.setDeleted(0);
      try {
        if (groupMemberMapper.insert(item) != 1) {
          throw new BusinessException(ErrorCode.CONFLICT, "裁判小组成员保存失败，请刷新后重试");
        }
      } catch (DuplicateKeyException ex) {
        throw new BusinessException(ErrorCode.CONFLICT, "一名裁判只能进入一个裁判小组");
      }
    }
  }

  private void assertProfilesAvailable(Long tenantId, Long currentGroupId, Collection<Long> profileIds) {
    if (profileIds == null || profileIds.isEmpty()) return;
    List<CompetitionRefereeGroupMemberEntity> existing = groupMemberMapper.selectList(
        new LambdaQueryWrapper<CompetitionRefereeGroupMemberEntity>()
            .eq(CompetitionRefereeGroupMemberEntity::getTenantId, tenantId)
            .in(CompetitionRefereeGroupMemberEntity::getRefereeProfileId, profileIds)
            .eq(CompetitionRefereeGroupMemberEntity::getStatus, ACTIVE)
            .eq(CompetitionRefereeGroupMemberEntity::getDeleted, 0));
    Map<Long, CompetitionRefereeGroupEntity> groupById = new LinkedHashMap<>();
    for (CompetitionRefereeGroupMemberEntity member : safe(existing)) {
      if (member == null || member.getRefereeProfileId() == null) continue;
      if (currentGroupId != null && Objects.equals(currentGroupId, member.getGroupId())) continue;
      CompetitionRefereeGroupEntity other = groupById.computeIfAbsent(member.getGroupId(),
          id -> groupMapper.selectById(id));
      if (other == null || !Objects.equals(other.getTenantId(), tenantId)
          || !Objects.equals(other.getDeleted(), 0) || GROUP_CLOSED.equals(other.getStatus())) {
        continue;
      }
      throw new BusinessException(ErrorCode.CONFLICT,
          "所选裁判已加入裁判小组「" + other.getGroupName() + "」，一名裁判只能进入一个裁判小组");
    }
  }

  private CompetitionRefereeGroupEntity findGroup(Long tenantId, Long groupId) {
    if (groupId == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "裁判组不存在");
    }
    CompetitionRefereeGroupEntity group = groupMapper.selectById(groupId);
    if (group == null || !Objects.equals(group.getTenantId(), tenantId)
        || !Objects.equals(group.getDeleted(), 0)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "裁判组不存在");
    }
    return group;
  }

  private void assertNoActiveGroupMembership(Long tenantId, Long profileId) {
    if (profileId == null) return;
    Long count = groupMemberMapper.selectCount(new LambdaQueryWrapper<CompetitionRefereeGroupMemberEntity>()
        .eq(CompetitionRefereeGroupMemberEntity::getTenantId, tenantId)
        .eq(CompetitionRefereeGroupMemberEntity::getRefereeProfileId, profileId)
        .eq(CompetitionRefereeGroupMemberEntity::getStatus, ACTIVE)
        .eq(CompetitionRefereeGroupMemberEntity::getDeleted, 0));
    if (count != null && count > 0) {
      throw new BusinessException(ErrorCode.CONFLICT, "该裁判已加入裁判小组，不能删除");
    }
  }

  private boolean hasActiveGroupBindings(Long tenantId, Long groupId) {
    Long count = assignmentMapper.selectCount(new LambdaQueryWrapper<CompetitionRefereeAssignmentEntity>()
        .eq(CompetitionRefereeAssignmentEntity::getTenantId, tenantId)
        .eq(CompetitionRefereeAssignmentEntity::getRefereeGroupId, groupId)
        .eq(CompetitionRefereeAssignmentEntity::getStatus, ACTIVE)
        .eq(CompetitionRefereeAssignmentEntity::getDeleted, 0));
    return count != null && count > 0;
  }

  private Map<Long, Long> resolveBindableGroupMembers(
      PlatformUserPrincipal principal,
      CompetitionStageVO stage,
      Scope scope,
      CompetitionRefereeGroupEntity group) {
    if (!GROUP_CONFIRMED.equals(group.getStatus())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "只能绑定已确定的裁判小组");
    }
    List<CompetitionRefereeGroupMemberEntity> members = activeGroupMembers(
        principal.getTenantId(), group.getId());
    if (members.size() != 3) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "裁判小组必须配置3名裁判后才能绑定选手");
    }
    if (GROUP_CLOSED.equals(group.getStatus())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "已解散的裁判小组不能绑定选手");
    }
    String occupiedEventName = occupiedDifferentEventName(
        principal.getTenantId(), group.getId(), stage.getEventId());
    if (occupiedEventName != null) {
      throw new BusinessException(ErrorCode.CONFLICT,
          "该裁判小组已承担赛事「" + occupiedEventName + "」的裁判任务，不能同时参加其他赛事");
    }
    Map<Long, CompetitionRefereeQueryRow> qualifiedByProfileId = safe(profileMapper.selectQualifiedForStage(
        principal.getTenantId(), stage.getEventId(), stage.getEventWorkTypeId(),
        scope.level().getLevelCode())).stream()
        .filter(item -> item != null && item.getProfileId() != null)
        .collect(Collectors.toMap(CompetitionRefereeQueryRow::getProfileId, Function.identity(),
            (left, right) -> left, LinkedHashMap::new));
    Map<Long, Long> memberIdByProfileId = new LinkedHashMap<>();
    for (CompetitionRefereeGroupMemberEntity member : members) {
      Long profileId = member.getRefereeProfileId();
      CompetitionRefereeQueryRow row = profileId == null ? null : qualifiedByProfileId.get(profileId);
      if (row == null) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
            "裁判小组成员未具备当前赛段工种等级资质或已停用");
      }
      Long eventMemberId = ensureEventRefereeMember(principal.getTenantId(), stage.getEventId(),
          row.getPersonId(), principal.getUserId()).getId();
      memberIdByProfileId.put(profileId, eventMemberId);
    }
    return memberIdByProfileId;
  }

  private List<CompetitionRefereeGroupVO> annotateBindability(
      Long tenantId,
      CompetitionStageVO stage,
      Scope scope,
      List<CompetitionRefereeGroupVO> groups) {
    Set<Long> qualifiedProfileIds = safe(profileMapper.selectQualifiedForStage(
        tenantId, stage.getEventId(), stage.getEventWorkTypeId(),
        scope.level().getLevelCode())).stream()
        .filter(item -> item != null && item.getProfileId() != null)
        .map(CompetitionRefereeQueryRow::getProfileId)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    for (CompetitionRefereeGroupVO group : groups) {
      if (!GROUP_CONFIRMED.equals(group.getStatus())) {
        group.setBindable(false);
        group.setUnavailableReason("请先在裁判小组中确定该组");
        continue;
      }
      List<Long> memberProfileIds = safe(group.getMembers()).stream()
          .map(CompetitionRefereeGroupMemberVO::getRefereeProfileId)
          .filter(Objects::nonNull)
          .toList();
      if (memberProfileIds.size() != 3
          || memberProfileIds.stream().anyMatch(id -> !qualifiedProfileIds.contains(id))) {
        group.setBindable(false);
        group.setUnavailableReason("组内裁判不具备当前赛段工种等级资质");
        continue;
      }
      String occupiedEventName = occupiedDifferentEventName(tenantId, group.getId(), stage.getEventId());
      if (occupiedEventName != null) {
        group.setBindable(false);
        group.setOccupiedEventName(occupiedEventName);
        group.setUnavailableReason("已承担赛事「" + occupiedEventName + "」的裁判任务，不能同时参加其他赛事");
        continue;
      }
      group.setBindable(true);
    }
    return groups;
  }

  private String occupiedDifferentEventName(Long tenantId, Long groupId, Long currentEventId) {
    Set<Long> occupiedEventIds = new LinkedHashSet<>();
    Map<Long, String> eventNames = new LinkedHashMap<>();
    for (CompetitionRefereeGroupBoundTaskVO task : boundTasksOf(tenantId, groupId, true)) {
      if (task.getEventId() == null) continue;
      occupiedEventIds.add(task.getEventId());
      if (StringUtils.hasText(task.getEventName())) {
        eventNames.putIfAbsent(task.getEventId(), task.getEventName());
      }
    }
    if (!CompetitionRefereeGroupScheduleGuard.occupiesDifferentEvent(currentEventId, occupiedEventIds)) {
      return null;
    }
    return occupiedEventIds.stream()
        .filter(eventId -> !Objects.equals(eventId, currentEventId))
        .map(eventId -> eventNames.getOrDefault(eventId, String.valueOf(eventId)))
        .findFirst()
        .orElse("其他赛事");
  }

  private Map<Long, CompetitionRefereeQueryRow> tenantRefereesByProfile(Long tenantId) {
    return safe(profileMapper.selectRows(tenantId, null, null, null, null, null)).stream()
        .filter(item -> item != null && item.getProfileId() != null)
        .collect(Collectors.toMap(CompetitionRefereeQueryRow::getProfileId, Function.identity(),
            (left, right) -> left, LinkedHashMap::new));
  }

  private List<CompetitionRefereeGroupEntity> tenantGroups(Long tenantId) {
    return safe(groupMapper.selectList(
        new LambdaQueryWrapper<CompetitionRefereeGroupEntity>()
            .eq(CompetitionRefereeGroupEntity::getTenantId, tenantId)
            .eq(CompetitionRefereeGroupEntity::getDeleted, 0)
            .orderByAsc(CompetitionRefereeGroupEntity::getGroupNo)
            .orderByAsc(CompetitionRefereeGroupEntity::getId)));
  }

  private List<CompetitionRefereeGroupEntity> confirmedTenantGroups(Long tenantId) {
    return tenantGroups(tenantId).stream()
        .filter(item -> GROUP_CONFIRMED.equals(item.getStatus()))
        .toList();
  }

  private List<CompetitionRefereeGroupMemberEntity> activeGroupMembers(
      Long tenantId, Long groupId) {
    return safe(groupMemberMapper.selectList(
        new LambdaQueryWrapper<CompetitionRefereeGroupMemberEntity>()
            .eq(CompetitionRefereeGroupMemberEntity::getTenantId, tenantId)
            .eq(CompetitionRefereeGroupMemberEntity::getGroupId, groupId)
            .eq(CompetitionRefereeGroupMemberEntity::getStatus, ACTIVE)
            .eq(CompetitionRefereeGroupMemberEntity::getDeleted, 0)
            .orderByAsc(CompetitionRefereeGroupMemberEntity::getId)));
  }

  private List<CompetitionRefereeGroupVO> toGroupVOs(
      List<CompetitionRefereeGroupEntity> groups,
      Map<Long, CompetitionRefereeQueryRow> refereeByMemberId,
      Map<Long, CompetitionRefereeQueryRow> refereeByProfileId) {
    List<CompetitionRefereeGroupVO> vos = safe(groups).stream().filter(Objects::nonNull)
        .map(group -> groupVO(group, refereeByMemberId, refereeByProfileId)).toList();
    Long tenantId = safe(groups).stream().filter(Objects::nonNull)
        .map(CompetitionRefereeGroupEntity::getTenantId).filter(Objects::nonNull).findFirst()
        .orElse(null);
    return annotateGroupTasks(tenantId, vos);
  }

  private CompetitionRefereeGroupVO annotatedGroupVO(
      CompetitionRefereeGroupEntity group,
      Map<Long, CompetitionRefereeQueryRow> refereeByMemberId,
      Map<Long, CompetitionRefereeQueryRow> refereeByProfileId) {
    return annotateGroupTasks(group.getTenantId(),
        List.of(groupVO(group, refereeByMemberId, refereeByProfileId))).getFirst();
  }

  private CompetitionRefereeGroupVO groupVO(
      CompetitionRefereeGroupEntity group,
      Map<Long, CompetitionRefereeQueryRow> refereeByMemberId,
      Map<Long, CompetitionRefereeQueryRow> refereeByProfileId) {
    CompetitionRefereeGroupVO result = new CompetitionRefereeGroupVO();
    result.setId(group.getId());
    result.setStageId(group.getStageId());
    result.setGroupNo(group.getGroupNo());
    result.setGroupName(group.getGroupName());
    result.setStatus(group.getStatus());
    result.setReleasable(false);
    result.setLeaderRefereeProfileId(group.getLeaderRefereeProfileId());
    List<CompetitionRefereeGroupMemberVO> members = new ArrayList<>();
    for (CompetitionRefereeGroupMemberEntity member : activeGroupMembers(
        group.getTenantId(), group.getId())) {
      CompetitionRefereeGroupMemberVO item = new CompetitionRefereeGroupMemberVO();
      item.setId(member.getId());
      item.setRefereeMemberId(member.getRefereeMemberId());
      item.setRefereeProfileId(member.getRefereeProfileId());
      item.setMemberRole(member.getMemberRole());
      item.setStatus(member.getStatus());
      CompetitionRefereeQueryRow referee = member.getRefereeProfileId() == null
          ? null : refereeByProfileId.get(member.getRefereeProfileId());
      if (referee == null && member.getRefereeMemberId() != null) {
        referee = refereeByMemberId.get(member.getRefereeMemberId());
      }
      if (referee != null) {
        item.setRefereeProfileId(referee.getProfileId());
        item.setRefereeName(referee.getName());
        if (Objects.equals(member.getRefereeProfileId(), group.getLeaderRefereeProfileId())
            || Objects.equals(member.getRefereeMemberId(), group.getLeaderRefereeMemberId())) {
          result.setLeaderRefereeProfileId(referee.getProfileId());
        }
      }
      members.add(item);
    }
    result.setMembers(members);
    result.setBoundTasks(new ArrayList<>());
    return result;
  }

  private List<CompetitionRefereeGroupVO> annotateGroupTasks(
      Long tenantId, List<CompetitionRefereeGroupVO> groups) {
    if (tenantId == null || groups == null || groups.isEmpty()) {
      return groups == null ? List.of() : groups;
    }
    Map<Long, List<CompetitionRefereeGroupBoundTaskVO>> tasksByGroup = boundTasksByGroup(
        tenantId,
        groups.stream().map(CompetitionRefereeGroupVO::getId).filter(Objects::nonNull).toList(),
        false,
        groups.stream().filter(item -> item.getId() != null)
            .collect(Collectors.toMap(CompetitionRefereeGroupVO::getId,
                item -> item.getStatus() == null ? "" : item.getStatus(),
                (left, right) -> left, LinkedHashMap::new)));
    for (CompetitionRefereeGroupVO group : groups) {
      List<CompetitionRefereeGroupBoundTaskVO> tasks = new ArrayList<>(
          tasksByGroup.getOrDefault(group.getId(), List.of()));
      group.setBoundTasks(tasks);
      boolean allCompleted = !tasks.isEmpty() && tasks.stream()
          .allMatch(item -> TASK_COMPLETED.equals(item.getStatus()));
      group.setReleasable(GROUP_CONFIRMED.equals(group.getStatus()) && allCompleted);
      tasks.stream().map(CompetitionRefereeGroupBoundTaskVO::getEventName)
          .filter(StringUtils::hasText).findFirst()
          .ifPresent(group::setOccupiedEventName);
    }
    return groups;
  }

  private List<CompetitionRefereeGroupBoundTaskVO> boundTasksOf(
      Long tenantId, Long groupId, boolean activeOnly) {
    return boundTasksByGroup(tenantId, groupId == null ? List.of() : List.of(groupId), activeOnly, Map.of())
        .getOrDefault(groupId, List.of());
  }

  private Map<Long, List<CompetitionRefereeGroupBoundTaskVO>> boundTasksByGroup(
      Long tenantId, List<Long> groupIds, boolean activeOnly, Map<Long, String> groupStatusById) {
    if (groupIds == null || groupIds.isEmpty()) return Map.of();
    List<CompetitionRefereeAssignmentEntity> assignments = assignmentMapper.selectList(
        new LambdaQueryWrapper<CompetitionRefereeAssignmentEntity>()
            .eq(CompetitionRefereeAssignmentEntity::getTenantId, tenantId)
            .in(CompetitionRefereeAssignmentEntity::getRefereeGroupId, groupIds)
            .eq(activeOnly, CompetitionRefereeAssignmentEntity::getStatus, ACTIVE)
            .eq(CompetitionRefereeAssignmentEntity::getDeleted, 0));
    List<CompetitionRefereeAssignmentEntity> rows = safe(assignments).stream()
        .filter(Objects::nonNull)
        .filter(item -> item.getRefereeGroupId() != null && item.getStageId() != null)
        .filter(item -> {
          if (ACTIVE.equals(item.getStatus())) return true;
          if (activeOnly) return false;
          return WITHDRAWN.equals(item.getStatus())
              && GROUP_CLOSED.equals(groupStatusById.get(item.getRefereeGroupId()));
        })
        .toList();
    if (rows.isEmpty()) return Map.of();

    Set<Long> stageIds = rows.stream().map(CompetitionRefereeAssignmentEntity::getStageId)
        .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
    Set<Long> eventIds = rows.stream().map(CompetitionRefereeAssignmentEntity::getEventId)
        .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
    Map<Long, CompetitionStageEntity> stages = stageIds.isEmpty() ? Map.of()
        : safe(stageMapper.selectList(new LambdaQueryWrapper<CompetitionStageEntity>()
            .eq(CompetitionStageEntity::getTenantId, tenantId)
            .in(CompetitionStageEntity::getId, stageIds)
            .eq(CompetitionStageEntity::getDeleted, 0))).stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(CompetitionStageEntity::getId, Function.identity(),
                (left, right) -> left, LinkedHashMap::new));
    Map<Long, CompetitionEventEntity> events = eventIds.isEmpty() ? Map.of()
        : safe(eventMapper.selectList(new LambdaQueryWrapper<CompetitionEventEntity>()
            .eq(CompetitionEventEntity::getTenantId, tenantId)
            .in(CompetitionEventEntity::getId, eventIds)
            .eq(CompetitionEventEntity::getDeleted, 0))).stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(CompetitionEventEntity::getId, Function.identity(),
                (left, right) -> left, LinkedHashMap::new));
    List<CompetitionPracticalSessionEntity> sessions = stageIds.isEmpty() ? List.of()
        : safe(practicalSessionMapper.selectList(
            new LambdaQueryWrapper<CompetitionPracticalSessionEntity>()
                .eq(CompetitionPracticalSessionEntity::getTenantId, tenantId)
                .in(CompetitionPracticalSessionEntity::getStageId, stageIds)
                .eq(CompetitionPracticalSessionEntity::getDeleted, 0)));
    Map<Long, Long> stageIdBySession = sessions.stream()
        .filter(item -> item.getId() != null && item.getStageId() != null)
        .collect(Collectors.toMap(CompetitionPracticalSessionEntity::getId,
            CompetitionPracticalSessionEntity::getStageId, (left, right) -> left, LinkedHashMap::new));
    Set<Long> sessionIds = stageIdBySession.keySet();
    Set<Long> contestantIds = rows.stream()
        .map(CompetitionRefereeAssignmentEntity::getContestantMemberId)
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    List<CompetitionPracticalAssignmentEntity> practicals = sessionIds.isEmpty() || contestantIds.isEmpty()
        ? List.of()
        : safe(practicalAssignmentMapper.selectList(
            new LambdaQueryWrapper<CompetitionPracticalAssignmentEntity>()
                .eq(CompetitionPracticalAssignmentEntity::getTenantId, tenantId)
                .in(CompetitionPracticalAssignmentEntity::getSessionId, sessionIds)
                .in(CompetitionPracticalAssignmentEntity::getContestantMemberId, contestantIds)
                .ne(CompetitionPracticalAssignmentEntity::getStatus, WITHDRAWN)
                .eq(CompetitionPracticalAssignmentEntity::getDeleted, 0)));
    Map<String, Set<Long>> startedContestants = new HashMap<>();
    for (CompetitionPracticalAssignmentEntity practical : practicals) {
      if (practical == null) continue;
      Long stageId = stageIdBySession.get(practical.getSessionId());
      if (stageId == null || practical.getContestantMemberId() == null) continue;
      startedContestants.computeIfAbsent(stageId + ":" + practical.getContestantMemberId(),
          ignored -> new LinkedHashSet<>()).add(practical.getContestantMemberId());
    }
    List<CompetitionScoreSourceEntity> scores = contestantIds.isEmpty() || stageIds.isEmpty()
        ? List.of()
        : safe(scoreSourceMapper.selectList(
            new LambdaQueryWrapper<CompetitionScoreSourceEntity>()
                .eq(CompetitionScoreSourceEntity::getTenantId, tenantId)
                .in(CompetitionScoreSourceEntity::getStageId, stageIds)
                .in(CompetitionScoreSourceEntity::getContestantMemberId, contestantIds)
                .eq(CompetitionScoreSourceEntity::getSourceType,
                    CompetitionScoreSourceType.PRACTICAL.getCode())
                .eq(CompetitionScoreSourceEntity::getConfirmationStatus, "confirmed")
                .eq(CompetitionScoreSourceEntity::getCurrentFlag, 1)
                .eq(CompetitionScoreSourceEntity::getDeleted, 0)));
    Set<String> completedKeys = scores.stream()
        .filter(Objects::nonNull)
        .filter(item -> item.getStageId() != null && item.getContestantMemberId() != null)
        .map(item -> item.getStageId() + ":" + item.getContestantMemberId())
        .collect(Collectors.toCollection(LinkedHashSet::new));

    Map<Long, Map<String, List<CompetitionRefereeAssignmentEntity>>> grouped = new LinkedHashMap<>();
    for (CompetitionRefereeAssignmentEntity row : rows) {
      String key = row.getEventId() + ":" + row.getStageId() + ":"
          + row.getEventWorkTypeId() + ":" + row.getEventWorkTypeLevelId();
      grouped.computeIfAbsent(row.getRefereeGroupId(), ignored -> new LinkedHashMap<>())
          .computeIfAbsent(key, ignored -> new ArrayList<>()).add(row);
    }
    Map<Long, List<CompetitionRefereeGroupBoundTaskVO>> result = new LinkedHashMap<>();
    Map<Long, String> workTypeNames = new HashMap<>();
    Map<Long, String> levelNames = new HashMap<>();
    for (Map.Entry<Long, Map<String, List<CompetitionRefereeAssignmentEntity>>> groupEntry : grouped.entrySet()) {
      List<CompetitionRefereeGroupBoundTaskVO> tasks = new ArrayList<>();
      for (List<CompetitionRefereeAssignmentEntity> stageRows : groupEntry.getValue().values()) {
        CompetitionRefereeAssignmentEntity first = stageRows.getFirst();
        Set<Long> contestants = stageRows.stream()
            .map(CompetitionRefereeAssignmentEntity::getContestantMemberId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        int completedCount = 0;
        int startedCount = 0;
        for (Long contestantId : contestants) {
          String contestantKey = first.getStageId() + ":" + contestantId;
          if (completedKeys.contains(contestantKey)) {
            completedCount++;
            startedCount++;
          } else if (startedContestants.containsKey(contestantKey)) {
            startedCount++;
          }
        }
        String status = TASK_PENDING;
        if (!contestants.isEmpty() && completedCount == contestants.size()) {
          status = TASK_COMPLETED;
        } else if (startedCount > 0) {
          status = TASK_IN_PROGRESS;
        }
        CompetitionRefereeGroupBoundTaskVO task = new CompetitionRefereeGroupBoundTaskVO();
        task.setEventId(first.getEventId());
        CompetitionEventEntity event = first.getEventId() == null ? null : events.get(first.getEventId());
        task.setEventName(event == null ? null : event.getName());
        task.setStageId(first.getStageId());
        CompetitionStageEntity stage = first.getStageId() == null ? null : stages.get(first.getStageId());
        task.setStageName(stage == null ? null : stage.getName());
        task.setEventWorkTypeId(first.getEventWorkTypeId());
        task.setWorkTypeName(workTypeName(tenantId, first.getEventWorkTypeId(), workTypeNames));
        task.setEventWorkTypeLevelId(first.getEventWorkTypeLevelId());
        task.setLevelName(levelName(tenantId, first.getEventId(), first.getEventWorkTypeId(),
            first.getEventWorkTypeLevelId(), levelNames));
        task.setContestantCount(contestants.size());
        task.setCompletedCount(completedCount);
        task.setStatus(status);
        tasks.add(task);
      }
      result.put(groupEntry.getKey(), tasks);
    }
    return result;
  }

  private String workTypeName(Long tenantId, Long workTypeId, Map<Long, String> cache) {
    if (workTypeId == null) return null;
    if (cache.containsKey(workTypeId)) return cache.get(workTypeId);
    try {
      CompetitionEventWorkTypeEntity workType = eventWorkTypeService.getById(tenantId, workTypeId);
      String name = workType == null ? null : workType.getDisplayName();
      cache.put(workTypeId, name);
      return name;
    } catch (BusinessException ex) {
      cache.put(workTypeId, null);
      return null;
    }
  }

  private String levelName(
      Long tenantId, Long eventId, Long workTypeId, Long levelId, Map<Long, String> cache) {
    if (levelId == null) return null;
    if (cache.containsKey(levelId)) return cache.get(levelId);
    if (eventId == null || workTypeId == null) {
      cache.put(levelId, null);
      return null;
    }
    try {
      String name = eventWorkTypeLevelService.list(tenantId, eventId, workTypeId).stream()
          .filter(item -> item != null && Objects.equals(item.getId(), levelId))
          .map(this::displayLevel)
          .findFirst().orElse(null);
      cache.put(levelId, name);
      return name;
    } catch (BusinessException ex) {
      cache.put(levelId, null);
      return null;
    }
  }

  private List<CompetitionRefereeAssignmentEntity> groupAssignments(
      Long tenantId, Long groupId, boolean activeOnly) {
    return safe(assignmentMapper.selectList(
        new LambdaQueryWrapper<CompetitionRefereeAssignmentEntity>()
            .eq(CompetitionRefereeAssignmentEntity::getTenantId, tenantId)
            .eq(CompetitionRefereeAssignmentEntity::getRefereeGroupId, groupId)
            .eq(activeOnly, CompetitionRefereeAssignmentEntity::getStatus, ACTIVE)
            .eq(CompetitionRefereeAssignmentEntity::getDeleted, 0)));
  }

  /** Kept for compatibility with older clients; the stage UI uses group binding APIs. */
  @Transactional
  @Deprecated
  public CompetitionRefereeCandidateVO replaceAssignments(
      Long stageId, Long contestantMemberId, CompetitionRefereeAssignmentReplaceReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionStageVO stage = stageApplicationService.assertCanManage(stageId);
    Scope scope = requireScopeByLevelCode(principal.getTenantId(), stage.getEventId(),
        stage.getEventWorkTypeId(), stage.getEventWorkTypeLevelCode());
    CompetitionStagePaperAssignmentPlayerRow candidate = requireEligibleCandidate(
        principal.getTenantId(), stage, scope, contestantMemberId);
    Set<Long> requestedProfiles = normalizeRequestedProfileIds(
        req == null ? null : req.refereeProfileIds());
    Map<Long, Long> memberIdByProfileId = resolveRefereeMembers(
        principal, stage, scope, requestedProfiles);
    replaceAssignmentsInternal(principal, stage, scope, contestantMemberId,
        requestedProfiles, memberIdByProfileId);
    return assignmentWorkspace(stageId).getCandidates().stream()
        .filter(item -> Objects.equals(item.getContestantMemberId(), contestantMemberId))
        .findFirst()
        .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "裁判绑定结果读取失败"));
  }

  /**
   * Replaces one referee set for all selected contestants in one transaction.
   * Every selected contestant must already have a practical paper; otherwise
   * the whole batch is rejected and no partial binding is committed.
   */
  /** Kept for compatibility with older clients; the stage UI uses group binding APIs. */
  @Transactional
  @Deprecated
  public CompetitionRefereeBatchAssignmentResultVO replaceAssignmentsBatch(
      Long stageId, CompetitionRefereeBatchAssignmentReplaceReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionStageVO stage = stageApplicationService.assertCanManage(stageId);
    Scope scope = requireScopeByLevelCode(principal.getTenantId(), stage.getEventId(),
        stage.getEventWorkTypeId(), stage.getEventWorkTypeLevelCode());
    List<Long> requestedContestantIds = req == null || req.contestantMemberIds() == null
        ? List.of() : req.contestantMemberIds();
    Set<Long> contestantIds = new LinkedHashSet<>(requestedContestantIds);
    if (contestantIds.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "至少选择一名已绑定实操试卷的选手");
    }
    if (contestantIds.size() != requestedContestantIds.size()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "选手不能重复选择");
    }

    Map<Long, CompetitionStagePaperAssignmentPlayerRow> candidateById = eligibleCandidates(
        principal.getTenantId(), stage, scope).stream()
        .filter(Objects::nonNull)
        .filter(row -> row.getContestantMemberId() != null)
        .collect(Collectors.toMap(CompetitionStagePaperAssignmentPlayerRow::getContestantMemberId,
            Function.identity(), (left, right) -> left, LinkedHashMap::new));
    for (Long contestantId : contestantIds) {
      CompetitionStagePaperAssignmentPlayerRow candidate = candidateById.get(contestantId);
      if (candidate == null) {
        throw new BusinessException(ErrorCode.NOT_FOUND, "当前选手不属于该赛段工种等级");
      }
      if (candidate.getPaperSnapshotId() == null) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
            "选手「" + candidate.getContestantName() + "」尚未绑定实操试卷，不能关联裁判");
      }
    }

    Set<Long> requestedProfiles = normalizeRequestedProfileIds(
        req == null ? null : req.refereeProfileIds());
    Map<Long, Long> memberIdByProfileId = resolveRefereeMembers(
        principal, stage, scope, requestedProfiles);
    for (Long contestantId : contestantIds) {
      replaceAssignmentsInternal(principal, stage, scope, contestantId,
          requestedProfiles, memberIdByProfileId);
    }

    CompetitionRefereeBatchAssignmentResultVO result = new CompetitionRefereeBatchAssignmentResultVO();
    result.setStageId(stageId);
    result.setContestantMemberIds(new ArrayList<>(contestantIds));
    result.setUpdatedCount(contestantIds.size());
    return result;
  }

  private CompetitionStagePaperAssignmentPlayerRow requireEligibleCandidate(
      Long tenantId, CompetitionStageVO stage, Scope scope, Long contestantMemberId) {
    CompetitionStagePaperAssignmentPlayerRow candidate = eligibleCandidates(tenantId, stage, scope).stream()
        .filter(row -> row != null && Objects.equals(row.getContestantMemberId(), contestantMemberId))
        .findFirst()
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "当前选手不属于该赛段工种等级"));
    if (candidate.getPaperSnapshotId() == null) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
          "该选手尚未绑定实操试卷，不能关联裁判");
    }
    return candidate;
  }

  private Set<Long> normalizeRequestedProfileIds(List<Long> requestedProfileIds) {
    List<Long> ids = requestedProfileIds == null ? List.of() : requestedProfileIds;
    if (ids.stream().anyMatch(Objects::isNull)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "裁判不能为空");
    }
    Set<Long> requestedProfiles = new LinkedHashSet<>(ids);
    if (requestedProfiles.size() != ids.size()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "裁判不能重复选择");
    }
    return requestedProfiles;
  }

  private Map<Long, Long> resolveRefereeMembers(
      PlatformUserPrincipal principal,
      CompetitionStageVO stage,
      Scope scope,
      Set<Long> requestedProfiles) {
    List<CompetitionRefereeQueryRow> refereeRows = profileMapper.selectQualifiedForStage(
        principal.getTenantId(), stage.getEventId(), stage.getEventWorkTypeId(),
        scope.level().getLevelCode());
    Map<Long, CompetitionRefereeQueryRow> refereeByProfileId = safe(refereeRows).stream()
        .filter(row -> row != null && row.getProfileId() != null)
        .collect(Collectors.toMap(CompetitionRefereeQueryRow::getProfileId, Function.identity(),
            (left, right) -> left, LinkedHashMap::new));
    Map<Long, Long> memberIdByProfileId = new LinkedHashMap<>();
    for (Long profileId : requestedProfiles) {
      CompetitionRefereeQueryRow referee = refereeByProfileId.get(profileId);
      if (referee == null) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
            "所选裁判未具备当前赛事工种等级资质或已停用");
      }
      memberIdByProfileId.put(profileId, ensureEventRefereeMember(
          principal.getTenantId(), stage.getEventId(), referee.getPersonId(), principal.getUserId()).getId());
    }
    return memberIdByProfileId;
  }

  private void replaceAssignmentsInternal(
      PlatformUserPrincipal principal,
      CompetitionStageVO stage,
      Scope scope,
      Long contestantMemberId,
      Set<Long> requestedProfiles,
      Map<Long, Long> memberIdByProfileId) {
    replaceAssignmentsInternal(principal, stage, scope, contestantMemberId, requestedProfiles,
        memberIdByProfileId, null);
  }

  private void replaceAssignmentsInternal(
      PlatformUserPrincipal principal,
      CompetitionStageVO stage,
      Scope scope,
      Long contestantMemberId,
      Set<Long> requestedProfiles,
      Map<Long, Long> memberIdByProfileId,
      Long refereeGroupId) {
    Set<Long> requestedMemberIds = new LinkedHashSet<>(memberIdByProfileId.values());
    List<CompetitionRefereeAssignmentEntity> existing = assignmentMapper.selectList(
        assignmentWrapper(principal.getTenantId(), stage, scope.level().getId(), contestantMemberId));
    if (existing == null) existing = new ArrayList<>();
    Map<Long, CompetitionRefereeAssignmentEntity> activeByReferee = existing.stream()
        .filter(Objects::nonNull)
        .filter(item -> ACTIVE.equals(item.getStatus()))
        .collect(Collectors.toMap(CompetitionRefereeAssignmentEntity::getRefereeMemberId,
            Function.identity(), (left, right) -> left, LinkedHashMap::new));
    LocalDateTime now = LocalDateTime.now();
    for (CompetitionRefereeAssignmentEntity item : existing) {
      if (item == null || !ACTIVE.equals(item.getStatus())
          || requestedMemberIds.contains(item.getRefereeMemberId())) continue;
      item.setStatus(WITHDRAWN);
      item.setEndedAt(now);
      item.setUpdatedBy(principal.getUserId());
      if (assignmentMapper.updateById(item) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "裁判绑定已发生变化，请刷新后重试");
      }
    }
    for (Long profileId : requestedProfiles) {
      Long refereeMemberId = memberIdByProfileId.get(profileId);
      CompetitionRefereeAssignmentEntity existingAssignment = activeByReferee.get(refereeMemberId);
      if (existingAssignment != null) {
        if (!Objects.equals(existingAssignment.getRefereeGroupId(), refereeGroupId)) {
          existingAssignment.setRefereeGroupId(refereeGroupId);
          existingAssignment.setUpdatedBy(principal.getUserId());
          if (assignmentMapper.updateById(existingAssignment) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "裁判组绑定已发生变化，请刷新后重试");
          }
        }
        continue;
      }
      CompetitionRefereeAssignmentEntity item = new CompetitionRefereeAssignmentEntity();
      item.setId(IdWorker.getId());
      item.setTenantId(principal.getTenantId());
      item.setEventId(stage.getEventId());
      item.setStageId(stage.getId());
      item.setEventWorkTypeId(stage.getEventWorkTypeId());
      item.setEventWorkTypeLevelId(scope.level().getId());
      item.setRefereeGroupId(refereeGroupId);
      item.setRefereeMemberId(refereeMemberId);
      item.setAssignmentRole(OFFICIAL);
      item.setContestantMemberId(contestantMemberId);
      item.setStatus(ACTIVE);
      item.setAssignedAt(now);
      item.setCreatedBy(principal.getUserId());
      item.setUpdatedBy(principal.getUserId());
      item.setDeleted(0);
      try {
        if (assignmentMapper.insert(item) != 1) {
          throw new BusinessException(ErrorCode.CONFLICT, "裁判绑定失败，请刷新后重试");
        }
      } catch (DuplicateKeyException ex) {
        throw new BusinessException(ErrorCode.CONFLICT, "裁判绑定已发生变化，请刷新后重试");
      }
    }
  }

  private List<CompetitionStagePaperAssignmentPlayerRow> eligibleCandidates(
      Long tenantId, CompetitionStageVO stage, Scope scope) {
    List<CompetitionStagePaperAssignmentPlayerRow> rows = stagePaperAssignmentMapper.selectEligiblePlayers(
        tenantId, stage.getEventId(), stage.getId(), stage.getEventWorkTypeId(), scope.level().getId(),
        levelTexts(scope.level()), null, null, null, null, PRACTICAL);
    return rows == null ? new ArrayList<>() : new ArrayList<>(rows);
  }

  private List<CompetitionRefereeAssignmentEntity> activeAssignments(
      Long tenantId, CompetitionStageVO stage, Long levelId) {
    List<CompetitionRefereeAssignmentEntity> rows = assignmentMapper.selectList(
        assignmentWrapper(tenantId, stage, levelId, null));
    if (rows == null) return new ArrayList<>();
    return rows.stream().filter(Objects::nonNull).filter(item -> ACTIVE.equals(item.getStatus())).toList();
  }

  private LambdaQueryWrapper<CompetitionRefereeAssignmentEntity> assignmentWrapper(
      Long tenantId, CompetitionStageVO stage, Long levelId, Long contestantMemberId) {
    LambdaQueryWrapper<CompetitionRefereeAssignmentEntity> wrapper =
        new LambdaQueryWrapper<CompetitionRefereeAssignmentEntity>()
            .eq(CompetitionRefereeAssignmentEntity::getTenantId, tenantId)
            .eq(CompetitionRefereeAssignmentEntity::getEventId, stage.getEventId())
            .eq(CompetitionRefereeAssignmentEntity::getStageId, stage.getId())
            .eq(CompetitionRefereeAssignmentEntity::getEventWorkTypeId, stage.getEventWorkTypeId())
            .eq(CompetitionRefereeAssignmentEntity::getEventWorkTypeLevelId, levelId)
            .eq(CompetitionRefereeAssignmentEntity::getDeleted, 0);
    if (contestantMemberId != null) {
      wrapper.eq(CompetitionRefereeAssignmentEntity::getContestantMemberId, contestantMemberId);
    }
    return wrapper;
  }

  private CompetitionRefereeCandidateVO toCandidate(
      CompetitionStagePaperAssignmentPlayerRow row,
      List<CompetitionRefereeAssignmentEntity> assignments,
      Map<Long, CompetitionRefereeQueryRow> refereeByMemberId,
      Map<Long, CompetitionRefereeGroupVO> groupById) {
    CompetitionRefereeCandidateVO result = new CompetitionRefereeCandidateVO();
    result.setContestantProfileId(row.getContestantProfileId());
    result.setContestantMemberId(row.getContestantMemberId());
    result.setContestantNo(row.getContestantNo());
    result.setName(row.getContestantName());
    result.setOrgName(row.getOrgName());
    result.setWorkTypeName(row.getWorkTypeName());
    result.setLevelCode(row.getLevelCode());
    result.setLevelName(row.getLevelName());
    result.setPaperSnapshotId(row.getPaperSnapshotId());
    result.setPaperCode(row.getPaperCode());
    result.setPaperTitle(row.getPaperTitle());
    result.setAssignmentStatus(row.getPaperSnapshotId() == null ? "unassigned" : "assigned");
    CompetitionRefereeAssignmentEntity groupAssignment = safe(assignments).stream()
        .filter(Objects::nonNull)
        .filter(item -> item.getRefereeGroupId() != null)
        .findFirst().orElse(null);
    if (groupAssignment != null) {
      result.setRefereeGroupId(groupAssignment.getRefereeGroupId());
      CompetitionRefereeGroupVO group = groupById.get(groupAssignment.getRefereeGroupId());
      if (group != null) {
        result.setRefereeGroupName(group.getGroupName());
        result.setRefereeGroupStatus(group.getStatus());
      }
    }
    List<CompetitionRefereeAssignmentVO> assignmentVOs = new ArrayList<>();
    for (CompetitionRefereeAssignmentEntity assignment : assignments == null
        ? Collections.<CompetitionRefereeAssignmentEntity>emptyList() : assignments) {
      if (assignment == null) continue;
      CompetitionRefereeAssignmentVO item = new CompetitionRefereeAssignmentVO();
      item.setId(assignment.getId());
      item.setRefereeMemberId(assignment.getRefereeMemberId());
      item.setRefereeGroupId(assignment.getRefereeGroupId());
      CompetitionRefereeQueryRow referee = refereeByMemberId.get(assignment.getRefereeMemberId());
      if (referee != null) {
        item.setRefereeProfileId(referee.getProfileId());
        item.setRefereeName(referee.getName());
      }
      item.setAssignmentRole(assignment.getAssignmentRole());
      item.setStatus(assignment.getStatus());
      assignmentVOs.add(item);
    }
    result.setAssignments(assignmentVOs);
    return result;
  }

  private CompetitionRefereeVO toVO(
      CompetitionRefereeQueryRow row,
      boolean includeSensitive,
      List<CompetitionRefereeQualificationVO> qualifications) {
    CompetitionRefereeVO result = new CompetitionRefereeVO();
    result.setId(row.getProfileId());
    result.setEventId(row.getEventId());
    result.setMemberId(row.getMemberId());
    result.setPersonId(row.getPersonId());
    result.setUserId(row.getUserId());
    result.setAccountUsername(row.getAccountUsername());
    result.setEventName(row.getEventName());
    result.setEventWorkTypeId(row.getEventWorkTypeId());
    result.setEventWorkTypeName(row.getEventWorkTypeName());
    result.setEventWorkTypeLevelId(row.getEventWorkTypeLevelId());
    result.setEventWorkTypeLevelCode(row.getEventWorkTypeLevelCode());
    result.setEventWorkTypeLevelName(row.getEventWorkTypeLevelName());
    result.setName(row.getName());
    result.setIdCardNo(includeSensitive ? row.getIdCardNo() : SensitiveMaskUtils.maskIdCard(row.getIdCardNo()));
    result.setMobile(includeSensitive ? row.getMobile() : SensitiveMaskUtils.maskPhone(row.getMobile()));
    result.setStatus(row.getStatus());
    result.setCreatedAt(row.getCreatedAt());
    result.setQualifications(qualifications == null ? List.of() : qualifications);
    return result;
  }

  private CompetitionRefereeQueryRow findRow(Long tenantId, Long profileId) {
    if (profileId == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "裁判信息不存在");
    }
    List<CompetitionRefereeQueryRow> rows = profileMapper.selectRows(
        tenantId, profileId, null, null, null, null);
    if (rows == null || rows.isEmpty() || rows.getFirst() == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "裁判信息不存在");
    }
    return rows.getFirst();
  }

  private CompetitionRefereeProfileEntity findProfile(Long tenantId, Long profileId) {
    CompetitionRefereeProfileEntity profile = profileMapper.selectById(profileId);
    if (profile == null || !Objects.equals(profile.getTenantId(), tenantId)
        || !Objects.equals(profile.getDeleted(), 0)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "裁判信息不存在");
    }
    return profile;
  }

  private List<CompetitionRefereeProfileEntity> findProfilesByPerson(Long tenantId, Long personId) {
    List<CompetitionRefereeProfileEntity> rows = profileMapper.selectList(
        new LambdaQueryWrapper<CompetitionRefereeProfileEntity>()
            .eq(CompetitionRefereeProfileEntity::getTenantId, tenantId)
            .eq(CompetitionRefereeProfileEntity::getPersonId, personId)
            .eq(CompetitionRefereeProfileEntity::getDeleted, 0));
    return rows == null ? new ArrayList<>() : new ArrayList<>(rows);
  }

  private List<CompetitionEventMemberEntity> findMembersByPerson(Long tenantId, Long personId) {
    List<CompetitionEventMemberEntity> rows = memberMapper.selectList(
        new LambdaQueryWrapper<CompetitionEventMemberEntity>()
            .eq(CompetitionEventMemberEntity::getTenantId, tenantId)
            .eq(CompetitionEventMemberEntity::getPersonId, personId)
            .eq(CompetitionEventMemberEntity::getRoleType, REFEREE)
            .eq(CompetitionEventMemberEntity::getDeleted, 0));
    return rows == null ? new ArrayList<>() : new ArrayList<>(rows);
  }

  private CompetitionEventMemberEntity findMember(Long tenantId, Long eventId, Long personId) {
    return memberMapper.selectOne(new LambdaQueryWrapper<CompetitionEventMemberEntity>()
        .eq(CompetitionEventMemberEntity::getTenantId, tenantId)
        .eq(CompetitionEventMemberEntity::getEventId, eventId)
        .eq(CompetitionEventMemberEntity::getPersonId, personId)
        .eq(CompetitionEventMemberEntity::getRoleType, REFEREE)
        .eq(CompetitionEventMemberEntity::getDeleted, 0));
  }

  private CompetitionEventMemberEntity ensureEventRefereeMember(
      Long tenantId, Long eventId, Long personId, Long operatorId) {
    CompetitionEventMemberEntity member = findMember(tenantId, eventId, personId);
    if (member == null) {
      member = new CompetitionEventMemberEntity();
      member.setId(IdWorker.getId());
      member.setTenantId(tenantId);
      member.setEventId(eventId);
      member.setPersonId(personId);
      member.setRoleType(REFEREE);
      member.setStatus(ACTIVE);
      member.setJoinedAt(LocalDateTime.now());
      member.setCreatedBy(operatorId);
      member.setUpdatedBy(operatorId);
      member.setDeleted(0);
      try {
        if (memberMapper.insert(member) != 1) {
          throw new BusinessException(ErrorCode.CONFLICT, "裁判赛事角色创建失败，请刷新后重试");
        }
      } catch (DuplicateKeyException ex) {
        member = findMember(tenantId, eventId, personId);
        if (member == null) {
          throw new BusinessException(ErrorCode.CONFLICT, "裁判赛事角色已发生变化，请刷新后重试");
        }
      }
    } else if (!ACTIVE.equals(member.getStatus())) {
      member.setStatus(ACTIVE);
      member.setUpdatedBy(operatorId);
      if (memberMapper.updateById(member) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "裁判赛事角色已发生变化，请刷新后重试");
      }
    }
    return member;
  }

  private void assertNoActiveAssignments(Long tenantId, Long personId) {
    List<Long> memberIds = findMembersByPerson(tenantId, personId).stream()
        .map(CompetitionEventMemberEntity::getId).filter(Objects::nonNull).toList();
    if (!memberIds.isEmpty()) {
      Long count = assignmentMapper.selectCount(new LambdaQueryWrapper<CompetitionRefereeAssignmentEntity>()
          .eq(CompetitionRefereeAssignmentEntity::getTenantId, tenantId)
          .in(CompetitionRefereeAssignmentEntity::getRefereeMemberId, memberIds)
          .eq(CompetitionRefereeAssignmentEntity::getStatus, ACTIVE)
          .eq(CompetitionRefereeAssignmentEntity::getDeleted, 0));
      if (count != null && count > 0) {
        throw new BusinessException(ErrorCode.CONFLICT, "该裁判已绑定选手，不能删除或修改工种等级");
      }
    }
  }

  private Scope requireScopeByLevelCode(
      Long tenantId, Long eventId, Long workTypeId, String levelCode) {
    if (!StringUtils.hasText(levelCode)) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "当前赛段未配置工种等级");
    }
    CompetitionEventWorkTypeEntity workType = eventWorkTypeService.getById(tenantId, workTypeId);
    if (!Objects.equals(eventId, workType.getEventId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "赛事工种不存在");
    }
    CompetitionEventWorkTypeLevelEntity level = eventWorkTypeLevelService.list(
        tenantId, eventId, workTypeId).stream()
        .filter(item -> item != null && levelCode.trim().equals(item.getLevelCode()))
        .findFirst()
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "赛事工种等级不存在"));
    return new Scope(workType, level);
  }

  private List<QualificationInput> normalizeQualifications(
      Long tenantId, List<CompetitionRefereeQualificationReq> requested) {
    if (requested == null || requested.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "至少需要配置一项裁判工种等级资质");
    }
    Map<Long, QualificationInput> unique = new LinkedHashMap<>();
    for (CompetitionRefereeQualificationReq item : requested) {
      if (item == null || item.workTypeId() == null
          || !StringUtils.hasText(item.levelFromCode())
          || !StringUtils.hasText(item.levelToCode())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "裁判工种和等级范围不能为空");
      }
      eventWorkTypeService.requireActiveExamWorkType(tenantId, item.workTypeId());
      String levelFromCode = item.levelFromCode().trim();
      String levelToCode = item.levelToCode().trim();
      if (!STANDARD_LEVEL_CODES.contains(levelFromCode)
          || !STANDARD_LEVEL_CODES.contains(levelToCode)) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "工种等级范围必须是1至5");
      }
      // The displayed range is always the intuitive low-to-high skill order:
      // 初级工(5) -> 中级工(4) -> 高级工(3) -> 技师(2) -> 高级技师(1).
      if (Integer.parseInt(levelFromCode) < Integer.parseInt(levelToCode)) {
        String swapped = levelFromCode;
        levelFromCode = levelToCode;
        levelToCode = swapped;
      }
      QualificationInput input = new QualificationInput(
          item.workTypeId(), levelFromCode, levelToCode);
      if (unique.put(input.workTypeId(), input) != null) {
        throw new BusinessException(ErrorCode.CONFLICT, "同一裁判的工种资质不能重复");
      }
    }
    return new ArrayList<>(unique.values());
  }

  private void replaceQualifications(
      Long tenantId, Long profileId, List<QualificationInput> requested, Long operatorId) {
    List<CompetitionRefereeQualificationEntity> existing = qualificationMapper.selectList(
        new LambdaQueryWrapper<CompetitionRefereeQualificationEntity>()
            .eq(CompetitionRefereeQualificationEntity::getTenantId, tenantId)
            .eq(CompetitionRefereeQualificationEntity::getRefereeProfileId, profileId)
            .eq(CompetitionRefereeQualificationEntity::getDeleted, 0));
    Map<String, CompetitionRefereeQualificationEntity> existingByKey = safe(existing).stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(this::qualificationKey,
            Function.identity(), (left, right) -> left, LinkedHashMap::new));
    Set<String> requestedKeys = requested.stream().map(QualificationInput::key)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    for (CompetitionRefereeQualificationEntity item : safe(existing)) {
      if (item != null && !requestedKeys.contains(qualificationKey(item))) {
        if (qualificationMapper.deleteById(item.getId()) != 1) {
          throw new BusinessException(ErrorCode.CONFLICT, "裁判工种等级范围删除失败，请刷新后重试");
        }
      }
    }
    for (QualificationInput item : requested) {
      if (existingByKey.containsKey(item.key())) continue;
      CompetitionRefereeQualificationEntity entity = new CompetitionRefereeQualificationEntity();
      entity.setId(IdWorker.getId());
      entity.setTenantId(tenantId);
      entity.setRefereeProfileId(profileId);
      entity.setWorkTypeId(item.workTypeId());
      entity.setLevelFromCode(item.levelFromCode());
      entity.setLevelToCode(item.levelToCode());
      entity.setLevelCode(item.levelFromCode());
      entity.setCreatedBy(operatorId);
      entity.setUpdatedBy(operatorId);
      entity.setDeleted(0);
      try {
        if (qualificationMapper.insert(entity) != 1) {
          throw new BusinessException(ErrorCode.CONFLICT, "裁判工种等级范围保存失败，请刷新后重试");
        }
      } catch (DuplicateKeyException ex) {
        throw new BusinessException(ErrorCode.CONFLICT, "裁判工种等级范围已发生变化，请刷新后重试");
      }
    }
  }

  private boolean sameQualifications(
      List<CompetitionRefereeQualificationEntity> existing, List<QualificationInput> requested) {
    Set<String> existingKeys = safe(existing).stream().filter(Objects::nonNull)
        .map(this::qualificationKey)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    Set<String> requestedKeys = requested.stream().map(QualificationInput::key)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    return existingKeys.equals(requestedKeys);
  }

  private Map<Long, List<CompetitionRefereeQualificationVO>> qualificationVOs(
      Long tenantId, List<Long> profileIds) {
    if (profileIds == null || profileIds.isEmpty()) return Map.of();
    List<CompetitionRefereeQualificationRow> rows = profileMapper.selectQualifications(tenantId, profileIds);
    Map<Long, List<CompetitionRefereeQualificationVO>> result = new LinkedHashMap<>();
    for (CompetitionRefereeQualificationRow row : safe(rows)) {
      if (row == null) continue;
      CompetitionRefereeQualificationVO item = new CompetitionRefereeQualificationVO();
      item.setWorkTypeId(row.getWorkTypeId());
      item.setWorkTypeName(row.getWorkTypeName());
      String levelFromCode = StringUtils.hasText(row.getLevelFromCode())
          ? row.getLevelFromCode() : row.getLevelCode();
      String levelToCode = StringUtils.hasText(row.getLevelToCode())
          ? row.getLevelToCode() : row.getLevelCode();
      String levelFromName = displayLevel(levelFromCode);
      String levelToName = displayLevel(levelToCode);
      item.setLevelFromCode(levelFromCode);
      item.setLevelToCode(levelToCode);
      item.setLevelFromName(levelFromName);
      item.setLevelToName(levelToName);
      item.setLevelRange(levelFromName + "-" + levelToName);
      item.setLevelCode(levelFromCode);
      item.setLevelName(item.getLevelRange());
      result.computeIfAbsent(row.getProfileId(), ignored -> new ArrayList<>()).add(item);
    }
    return result;
  }

  private String qualificationKey(CompetitionRefereeQualificationEntity item) {
    String fromCode = StringUtils.hasText(item.getLevelFromCode())
        ? item.getLevelFromCode() : item.getLevelCode();
    String toCode = StringUtils.hasText(item.getLevelToCode())
        ? item.getLevelToCode() : item.getLevelCode();
    return qualificationKey(item.getWorkTypeId(), fromCode, toCode);
  }

  private String qualificationKey(Long workTypeId, String levelFromCode, String levelToCode) {
    return String.valueOf(workTypeId) + ':' + levelFromCode + ':' + levelToCode;
  }

  private String displayLevel(String levelCode) {
    if (!StringUtils.hasText(levelCode)) return "";
    return switch (levelCode) {
      case "1" -> "高级技师";
      case "2" -> "技师";
      case "3" -> "高级工";
      case "4" -> "中级工";
      case "5" -> "初级工";
      default -> levelCode;
    };
  }

  private String displayLevel(CompetitionEventWorkTypeLevelEntity level) {
    return StringUtils.hasText(level.getLevelName()) ? level.getLevelName() : level.getLevelCode();
  }

  private List<String> levelTexts(CompetitionEventWorkTypeLevelEntity level) {
    LinkedHashSet<String> values = new LinkedHashSet<>();
    addText(values, level.getLevelCode());
    addText(values, level.getLevelName());
    addText(values, level.getSourceLevelCode());
    addText(values, level.getSourceLevelText());
    return new ArrayList<>(values);
  }

  private void addText(Set<String> values, String value) {
    if (StringUtils.hasText(value)) values.add(value.trim());
  }

  private String normalizeOptional(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }


  private <T> List<T> safe(List<T> values) {
    return values == null ? List.of() : values;
  }

  private PlatformUserPrincipal currentUser() {
    try {
      return SecurityContext.currentUser();
    } catch (IllegalStateException ex) {
      log.warn("Competition referee request has no authenticated platform user");
      throw ex;
    }
  }

  private record QualificationInput(Long workTypeId, String levelFromCode, String levelToCode) {
    private String key() {
      return String.valueOf(workTypeId) + ':' + levelFromCode + ':' + levelToCode;
    }
  }

  private record Scope(
      CompetitionEventWorkTypeEntity workType,
      CompetitionEventWorkTypeLevelEntity level) {
  }

  private record ResolvedGroupInput(
      String groupName,
      List<Long> profileIds,
      Long leaderProfileId,
      Map<Long, CompetitionRefereeQueryRow> refereeByProfileId) {
  }
}
