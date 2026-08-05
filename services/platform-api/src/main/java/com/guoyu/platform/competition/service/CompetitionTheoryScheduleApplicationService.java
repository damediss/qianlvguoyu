package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.domain.CompetitionTheoryScheduleAllocator;
import com.guoyu.platform.competition.domain.CompetitionTheoryScheduleAllocator.Allocation;
import com.guoyu.platform.competition.domain.CompetitionTheoryScheduleAllocator.Candidate;
import com.guoyu.platform.competition.domain.CompetitionTheoryScheduleAllocator.RoomSlot;
import com.guoyu.platform.competition.domain.CompetitionTheoryScheduleAllocator.SessionSlot;
import com.guoyu.platform.competition.dto.CompetitionTheoryAssignmentAdjustReq;
import com.guoyu.platform.competition.dto.CompetitionTheoryRoomUpsertReq;
import com.guoyu.platform.competition.dto.CompetitionTheoryScheduleCreateReq;
import com.guoyu.platform.competition.dto.CompetitionTheorySessionUpsertReq;
import com.guoyu.platform.competition.entity.CompetitionScoreRuleEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryAssignmentAdjustmentEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryCandidateAssignmentEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryExamRoomEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryExamSessionEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryPaperSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryScheduleVersionEntity;
import com.guoyu.platform.competition.entity.CompetitionTheorySessionRoomEntity;
import com.guoyu.platform.competition.enums.CompetitionScoreRuleStatus;
import com.guoyu.platform.competition.mapper.CompetitionTheoryAssignmentAdjustmentMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryCandidateAssignmentMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryCandidateRow;
import com.guoyu.platform.competition.mapper.CompetitionTheoryExamRoomMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryExamSessionMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryPaperSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryRosterRow;
import com.guoyu.platform.competition.mapper.CompetitionTheoryScheduleVersionMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheorySessionRoomMapper;
import com.guoyu.platform.competition.messaging.entity.CompetitionPlayerMessageEntity;
import com.guoyu.platform.competition.messaging.mapper.CompetitionPlayerMessageMapper;
import com.guoyu.platform.competition.vo.CompetitionPlayerTheoryScheduleVO;
import com.guoyu.platform.competition.vo.CompetitionTheoryAssignmentVO;
import com.guoyu.platform.competition.vo.CompetitionTheoryAssignmentAdjustmentVO;
import com.guoyu.platform.competition.vo.CompetitionTheoryRoomVO;
import com.guoyu.platform.competition.vo.CompetitionTheoryScheduleVersionVO;
import com.guoyu.platform.competition.vo.CompetitionTheorySchedulePreflightVO;
import com.guoyu.platform.competition.vo.CompetitionTheorySessionRoomVO;
import com.guoyu.platform.competition.vo.CompetitionTheorySessionVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class CompetitionTheoryScheduleApplicationService {

  private static final Set<String> ROOM_STATUSES = Set.of("enabled", "disabled");
  private static final SecureRandom RANDOM = new SecureRandom();

  private final CompetitionScoreRuleApplicationService scoreRuleApplicationService;
  private final CompetitionScoreRuleDomainService scoreRuleDomainService;
  private final CompetitionTheoryScheduleVersionMapper versionMapper;
  private final CompetitionTheoryExamRoomMapper roomMapper;
  private final CompetitionTheoryExamSessionMapper sessionMapper;
  private final CompetitionTheorySessionRoomMapper sessionRoomMapper;
  private final CompetitionTheoryCandidateAssignmentMapper assignmentMapper;
  private final CompetitionTheoryAssignmentAdjustmentMapper adjustmentMapper;
  private final CompetitionTheoryPaperSnapshotMapper paperSnapshotMapper;
  private final CompetitionTheoryScheduleAllocator allocator;
  private final CompetitionPlayerMessageMapper playerMessageMapper;
  private final CompetitionContestantScopeValidator contestantScopeValidator;

  public CompetitionTheoryScheduleApplicationService(
      CompetitionScoreRuleApplicationService scoreRuleApplicationService,
      CompetitionScoreRuleDomainService scoreRuleDomainService,
      CompetitionTheoryScheduleVersionMapper versionMapper,
      CompetitionTheoryExamRoomMapper roomMapper,
      CompetitionTheoryExamSessionMapper sessionMapper,
      CompetitionTheorySessionRoomMapper sessionRoomMapper,
      CompetitionTheoryCandidateAssignmentMapper assignmentMapper,
      CompetitionTheoryAssignmentAdjustmentMapper adjustmentMapper,
      CompetitionTheoryPaperSnapshotMapper paperSnapshotMapper,
      CompetitionTheoryScheduleAllocator allocator,
      CompetitionPlayerMessageMapper playerMessageMapper) {
    this(scoreRuleApplicationService, scoreRuleDomainService, versionMapper, roomMapper, sessionMapper,
        sessionRoomMapper, assignmentMapper, adjustmentMapper, paperSnapshotMapper, allocator,
        playerMessageMapper, null);
  }

  @org.springframework.beans.factory.annotation.Autowired
  public CompetitionTheoryScheduleApplicationService(
      CompetitionScoreRuleApplicationService scoreRuleApplicationService,
      CompetitionScoreRuleDomainService scoreRuleDomainService,
      CompetitionTheoryScheduleVersionMapper versionMapper,
      CompetitionTheoryExamRoomMapper roomMapper,
      CompetitionTheoryExamSessionMapper sessionMapper,
      CompetitionTheorySessionRoomMapper sessionRoomMapper,
      CompetitionTheoryCandidateAssignmentMapper assignmentMapper,
      CompetitionTheoryAssignmentAdjustmentMapper adjustmentMapper,
      CompetitionTheoryPaperSnapshotMapper paperSnapshotMapper,
      CompetitionTheoryScheduleAllocator allocator,
      CompetitionPlayerMessageMapper playerMessageMapper,
      CompetitionContestantScopeValidator contestantScopeValidator) {
    this.scoreRuleApplicationService = scoreRuleApplicationService;
    this.scoreRuleDomainService = scoreRuleDomainService;
    this.versionMapper = versionMapper;
    this.roomMapper = roomMapper;
    this.sessionMapper = sessionMapper;
    this.sessionRoomMapper = sessionRoomMapper;
    this.assignmentMapper = assignmentMapper;
    this.adjustmentMapper = adjustmentMapper;
    this.paperSnapshotMapper = paperSnapshotMapper;
    this.allocator = allocator;
    this.playerMessageMapper = playerMessageMapper;
    this.contestantScopeValidator = contestantScopeValidator;
  }

  @Transactional
  public CompetitionTheoryScheduleVersionVO createVersion(
      Long eventId, CompetitionTheoryScheduleCreateReq req) {
    PlatformUserPrincipal principal = currentUser();
    scoreRuleApplicationService.manageableEvent(principal, eventId);
    CompetitionScoreRuleEntity rule =
        scoreRuleDomainService.getById(principal.getTenantId(), req.scoreRuleId());
    if (!Objects.equals(rule.getEventId(), eventId)
        || !Objects.equals(rule.getStageId(), req.stageId())
        || !Objects.equals(rule.getEventWorkTypeId(), req.eventWorkTypeId())
        || !CompetitionScoreRuleStatus.PUBLISHED.getCode().equals(rule.getStatus())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "只能为当前范围的已发布评分规则创建安排");
    }
    long active = versionMapper.selectCount(
        new LambdaQueryWrapper<CompetitionTheoryScheduleVersionEntity>()
            .eq(CompetitionTheoryScheduleVersionEntity::getTenantId, principal.getTenantId())
            .eq(CompetitionTheoryScheduleVersionEntity::getEventId, eventId)
            .eq(CompetitionTheoryScheduleVersionEntity::getStageId, req.stageId())
            .eq(CompetitionTheoryScheduleVersionEntity::getEventWorkTypeId, req.eventWorkTypeId())
            .in(CompetitionTheoryScheduleVersionEntity::getStatus, "draft", "published")
            .eq(CompetitionTheoryScheduleVersionEntity::getDeleted, 0));
    if (active > 0) {
      throw new BusinessException(ErrorCode.CONFLICT, "当前范围已有草稿或已发布安排");
    }
    LocalDateTime now = LocalDateTime.now();
    CompetitionTheoryScheduleVersionEntity entity =
        new CompetitionTheoryScheduleVersionEntity();
    entity.setTenantId(principal.getTenantId());
    entity.setEventId(eventId);
    entity.setStageId(req.stageId());
    entity.setEventWorkTypeId(req.eventWorkTypeId());
    entity.setScoreRuleId(req.scoreRuleId());
    entity.setVersionNo(versionMapper.selectMaxVersion(
        principal.getTenantId(), eventId, req.stageId(), req.eventWorkTypeId()) + 1);
    entity.setStatus("draft");
    auditCreate(entity, principal.getUserId(), now);
    requireInsert(versionMapper.insert(entity), "考试安排草稿创建失败");
    return detailInternal(principal.getTenantId(), entity);
  }

  @Transactional(readOnly = true)
  public List<CompetitionTheoryScheduleVersionVO> listVersions(
      Long eventId, Long stageId, Long eventWorkTypeId) {
    PlatformUserPrincipal principal = currentUser();
    scoreRuleApplicationService.accessibleEvent(principal, eventId);
    List<CompetitionTheoryScheduleVersionEntity> versions = safe(versionMapper.selectList(
        new LambdaQueryWrapper<CompetitionTheoryScheduleVersionEntity>()
            .eq(CompetitionTheoryScheduleVersionEntity::getTenantId, principal.getTenantId())
            .eq(CompetitionTheoryScheduleVersionEntity::getEventId, eventId)
            .eq(stageId != null, CompetitionTheoryScheduleVersionEntity::getStageId, stageId)
            .eq(eventWorkTypeId != null,
                CompetitionTheoryScheduleVersionEntity::getEventWorkTypeId, eventWorkTypeId)
            .eq(CompetitionTheoryScheduleVersionEntity::getDeleted, 0)
            .orderByDesc(CompetitionTheoryScheduleVersionEntity::getVersionNo)));
    return versions.stream().map(v -> detailInternal(principal.getTenantId(), v)).toList();
  }

  @Transactional(readOnly = true)
  public CompetitionTheoryScheduleVersionVO detail(Long versionId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionTheoryScheduleVersionEntity version =
        requireVersion(principal.getTenantId(), versionId);
    scoreRuleApplicationService.accessibleEvent(principal, version.getEventId());
    return detailInternal(principal.getTenantId(), version);
  }

  @Transactional(readOnly = true)
  public CompetitionTheorySchedulePreflightVO preflight(Long versionId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionTheoryScheduleVersionEntity version =
        requireVersion(principal.getTenantId(), versionId);
    scoreRuleApplicationService.accessibleEvent(principal, version.getEventId());
    List<CompetitionTheoryExamSessionEntity> sessions =
        sessions(principal.getTenantId(), versionId);
    List<CompetitionTheoryCandidateRow> candidates = safe(
        assignmentMapper.selectEligibleCandidates(
            principal.getTenantId(), version.getEventId(), version.getStageId(),
            version.getEventWorkTypeId()));
    List<CompetitionTheoryCandidateAssignmentEntity> assignments =
        assignmentEntities(principal.getTenantId(), versionId);
    int totalCapacity = sessions.stream()
        .mapToInt(session -> session.getTotalCapacity() == null ? 0 : session.getTotalCapacity())
        .sum();
    List<String> issues = new ArrayList<>();
    if (!"draft".equals(version.getStatus())) {
      issues.add("仅草稿安排可发布");
    }
    try {
      validateSessionSet(principal.getTenantId(), version, sessions);
    } catch (BusinessException ex) {
      issues.add(ex.getMessage());
    }
    if (candidates.size() > totalCapacity) {
      issues.add("A/B场次总容量不足，缺少" + (candidates.size() - totalCapacity) + "个座位");
    }
    try {
      validatePublishAssignments(
          principal.getTenantId(), version, sessions, candidates, assignments);
    } catch (BusinessException ex) {
      if (!issues.contains(ex.getMessage())) {
        issues.add(ex.getMessage());
      }
    }
    return new CompetitionTheorySchedulePreflightVO(
        versionId, issues.isEmpty(), candidates.size(), assignments.size(), totalCapacity,
        Math.max(0, candidates.size() - totalCapacity), List.copyOf(issues));
  }

  @Transactional(readOnly = true)
  public List<CompetitionTheoryAssignmentAdjustmentVO> listAdjustments(Long versionId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionTheoryScheduleVersionEntity version =
        requireVersion(principal.getTenantId(), versionId);
    scoreRuleApplicationService.accessibleEvent(principal, version.getEventId());
    return safe(adjustmentMapper.selectList(
        new LambdaQueryWrapper<CompetitionTheoryAssignmentAdjustmentEntity>()
            .eq(CompetitionTheoryAssignmentAdjustmentEntity::getTenantId, principal.getTenantId())
            .eq(CompetitionTheoryAssignmentAdjustmentEntity::getScheduleVersionId, versionId)
            .eq(CompetitionTheoryAssignmentAdjustmentEntity::getDeleted, 0)
            .orderByDesc(CompetitionTheoryAssignmentAdjustmentEntity::getAdjustedAt)
            .orderByDesc(CompetitionTheoryAssignmentAdjustmentEntity::getId)))
        .stream()
        .map(item -> new CompetitionTheoryAssignmentAdjustmentVO(
            item.getId(), item.getAssignmentId(), item.getContestantProfileId(),
            item.getFromSessionId(), item.getFromSessionRoomId(), item.getFromSeatNo(),
            item.getToSessionId(), item.getToSessionRoomId(), item.getToSeatNo(),
            item.getReason(), item.getAdjustedBy(), item.getAdjustedAt()))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<CompetitionTheoryRoomVO> listRooms(Long eventId) {
    PlatformUserPrincipal principal = currentUser();
    scoreRuleApplicationService.accessibleEvent(principal, eventId);
    return safe(roomMapper.selectList(
        new LambdaQueryWrapper<CompetitionTheoryExamRoomEntity>()
            .eq(CompetitionTheoryExamRoomEntity::getTenantId, principal.getTenantId())
            .eq(CompetitionTheoryExamRoomEntity::getEventId, eventId)
            .eq(CompetitionTheoryExamRoomEntity::getDeleted, 0)
            .orderByAsc(CompetitionTheoryExamRoomEntity::getName)
            .orderByAsc(CompetitionTheoryExamRoomEntity::getId)))
        .stream().map(this::toRoomVO).toList();
  }

  @Transactional
  public CompetitionTheoryRoomVO createRoom(
      Long eventId, CompetitionTheoryRoomUpsertReq req) {
    PlatformUserPrincipal principal = currentUser();
    scoreRuleApplicationService.manageableEvent(principal, eventId);
    CompetitionTheoryExamRoomEntity room = new CompetitionTheoryExamRoomEntity();
    room.setTenantId(principal.getTenantId());
    room.setEventId(eventId);
    applyRoom(room, req);
    auditCreate(room, principal.getUserId(), LocalDateTime.now());
    requireInsert(roomMapper.insert(room), "考场创建失败");
    return toRoomVO(room);
  }

  @Transactional
  public CompetitionTheoryRoomVO updateRoom(
      Long roomId, CompetitionTheoryRoomUpsertReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionTheoryExamRoomEntity room = requireRoom(principal.getTenantId(), roomId);
    scoreRuleApplicationService.manageableEvent(principal, room.getEventId());
    if (roomMapper.countPublishedReferences(principal.getTenantId(), roomId) > 0) {
      throw new BusinessException(ErrorCode.CONFLICT, "已发布安排引用的考场不可修改");
    }
    applyRoom(room, req);
    auditUpdate(room, principal.getUserId(), LocalDateTime.now());
    roomMapper.updateById(room);
    return toRoomVO(room);
  }

  @Transactional
  public void deleteRoom(Long roomId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionTheoryExamRoomEntity room = requireRoom(principal.getTenantId(), roomId);
    scoreRuleApplicationService.manageableEvent(principal, room.getEventId());
    long references = sessionRoomMapper.selectCount(
        new LambdaQueryWrapper<CompetitionTheorySessionRoomEntity>()
            .eq(CompetitionTheorySessionRoomEntity::getTenantId, principal.getTenantId())
            .eq(CompetitionTheorySessionRoomEntity::getRoomId, roomId)
            .eq(CompetitionTheorySessionRoomEntity::getDeleted, 0));
    if (references > 0) {
      throw new BusinessException(ErrorCode.CONFLICT, "考场已被考试场次引用，不能删除");
    }
    roomMapper.deleteById(roomId);
  }

  @Transactional
  public CompetitionTheorySessionVO createSession(
      Long versionId, CompetitionTheorySessionUpsertReq req) {
    return saveSession(versionId, null, req);
  }

  @Transactional
  public CompetitionTheorySessionVO updateSession(
      Long versionId, Long sessionId, CompetitionTheorySessionUpsertReq req) {
    return saveSession(versionId, sessionId, req);
  }

  @Transactional
  public void deleteSession(Long versionId, Long sessionId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionTheoryScheduleVersionEntity version =
        requireDraftForManage(principal, versionId);
    CompetitionTheoryExamSessionEntity session =
        requireSession(principal.getTenantId(), versionId, sessionId);
    clearAssignments(principal.getTenantId(), versionId);
    deleteSessionRooms(principal.getTenantId(), sessionId);
    sessionMapper.deleteById(session.getId());
    touchVersion(version, principal.getUserId());
  }

  @Transactional
  public CompetitionTheoryScheduleVersionVO randomAllocate(Long versionId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionTheoryScheduleVersionEntity version =
        requireDraftForManage(principal, versionId);
    List<CompetitionTheoryExamSessionEntity> sessions =
        sessions(principal.getTenantId(), versionId);
    validateSessionSet(principal.getTenantId(), version, sessions);
    List<CompetitionTheoryCandidateRow> candidates = safe(
        assignmentMapper.selectEligibleCandidates(
            principal.getTenantId(), version.getEventId(), version.getStageId(),
            version.getEventWorkTypeId()));
    if (candidates.isEmpty()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "当前范围没有可排场的合格选手");
    }
    lockCandidatesForWrite(principal.getTenantId(), version.getEventId(), version.getStageId(),
        version.getEventWorkTypeId(), candidates.stream()
            .map(CompetitionTheoryCandidateRow::getContestantMemberId)
            .sorted()
            .toList());
    List<SessionSlot> slots = sessionSlots(principal.getTenantId(), sessions);
    byte[] seedBytes = new byte[16];
    RANDOM.nextBytes(seedBytes);
    long seed = ByteBuffer.wrap(seedBytes).getLong();
    String randomBatchId = UUID.randomUUID().toString();
    List<Allocation> allocations = allocator.allocate(
        candidates.stream().map(row -> new Candidate(
            row.getContestantProfileId(), row.getContestantMemberId(),
            row.getRegistrationId(), row.getPlayerId())).toList(),
        slots, seed);

    clearAssignments(principal.getTenantId(), versionId);
    LocalDateTime now = LocalDateTime.now();
    for (Allocation allocation : allocations) {
      CompetitionTheoryCandidateAssignmentEntity entity =
          new CompetitionTheoryCandidateAssignmentEntity();
      entity.setTenantId(principal.getTenantId());
      entity.setScheduleVersionId(versionId);
      entity.setEventId(version.getEventId());
      entity.setStageId(version.getStageId());
      entity.setEventWorkTypeId(version.getEventWorkTypeId());
      entity.setSessionId(allocation.sessionId());
      entity.setSessionRoomId(allocation.sessionRoomId());
      entity.setRoomId(allocation.roomId());
      entity.setContestantProfileId(allocation.candidate().contestantProfileId());
      entity.setContestantMemberId(allocation.candidate().contestantMemberId());
      entity.setRegistrationId(allocation.candidate().registrationId());
      entity.setPlayerId(allocation.candidate().playerId());
      entity.setSeatNo(allocation.seatNo());
      entity.setRandomBatchId(randomBatchId);
      entity.setAllocationSource("random");
      entity.setStatus("draft");
      auditCreate(entity, principal.getUserId(), now);
      requireInsert(assignmentMapper.insert(entity), "选手随机分配保存失败");
    }
    version.setRandomBatchId(randomBatchId);
    version.setRandomSeed(Base64.getEncoder().encodeToString(seedBytes));
    version.setGeneratedAt(now);
    version.setGeneratedBy(principal.getUserId());
    auditUpdate(version, principal.getUserId(), now);
    versionMapper.updateById(version);
    return detailInternal(principal.getTenantId(), version);
  }

  @Transactional
  public CompetitionTheoryScheduleVersionVO adjust(
      Long assignmentId, CompetitionTheoryAssignmentAdjustReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionTheoryCandidateAssignmentEntity assignment =
        requireAssignment(principal.getTenantId(), assignmentId);
    CompetitionTheoryScheduleVersionEntity version =
        requireDraftForManage(principal, assignment.getScheduleVersionId());
    lockCandidateForWrite(principal.getTenantId(), version.getEventId(), version.getStageId(),
        version.getEventWorkTypeId(), assignment.getContestantMemberId());
    CompetitionTheorySessionRoomEntity targetRoom =
        requireSessionRoom(principal.getTenantId(), version.getId(), req.targetSessionRoomId());
    if (Objects.equals(targetRoom.getId(), assignment.getSessionRoomId())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "目标考场与当前考场相同");
    }
    CompetitionTheoryExamSessionEntity targetSession =
        requireSession(principal.getTenantId(), version.getId(), targetRoom.getSessionId());
    long occupied = assignmentMapper.selectCount(
        new LambdaQueryWrapper<CompetitionTheoryCandidateAssignmentEntity>()
            .eq(CompetitionTheoryCandidateAssignmentEntity::getTenantId, principal.getTenantId())
            .eq(CompetitionTheoryCandidateAssignmentEntity::getSessionRoomId, targetRoom.getId())
            .eq(CompetitionTheoryCandidateAssignmentEntity::getDeleted, 0));
    if (occupied >= targetRoom.getCapacitySnapshot()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "目标考场已满");
    }
    if (sessionMapper.countCandidateTimeConflicts(
        principal.getTenantId(), assignment.getContestantProfileId(), version.getId(),
        targetSession.getStartAt(), targetSession.getEndAt()) > 0) {
      throw new BusinessException(ErrorCode.CONFLICT, "该选手在目标时段已有其他考试安排");
    }
    String newSeat = firstAvailableSeat(
        principal.getTenantId(), targetRoom.getId(), targetRoom.getCapacitySnapshot());
    LocalDateTime now = LocalDateTime.now();
    CompetitionTheoryAssignmentAdjustmentEntity audit =
        new CompetitionTheoryAssignmentAdjustmentEntity();
    audit.setTenantId(principal.getTenantId());
    audit.setScheduleVersionId(version.getId());
    audit.setAssignmentId(assignment.getId());
    audit.setContestantProfileId(assignment.getContestantProfileId());
    audit.setFromSessionId(assignment.getSessionId());
    audit.setFromSessionRoomId(assignment.getSessionRoomId());
    audit.setFromSeatNo(assignment.getSeatNo());
    audit.setToSessionId(targetSession.getId());
    audit.setToSessionRoomId(targetRoom.getId());
    audit.setToSeatNo(newSeat);
    audit.setReason(req.reason().trim());
    audit.setAdjustedBy(principal.getUserId());
    audit.setAdjustedAt(now);
    auditCreate(audit, principal.getUserId(), now);
    requireInsert(adjustmentMapper.insert(audit), "特殊选手调整审计保存失败");

    assignment.setSessionId(targetSession.getId());
    assignment.setSessionRoomId(targetRoom.getId());
    assignment.setRoomId(targetRoom.getRoomId());
    assignment.setSeatNo(newSeat);
    assignment.setAllocationSource("manual");
    auditUpdate(assignment, principal.getUserId(), now);
    assignmentMapper.updateById(assignment);
    return detailInternal(principal.getTenantId(), version);
  }

  @Transactional
  public CompetitionTheoryScheduleVersionVO publish(Long versionId) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionTheoryScheduleVersionEntity version =
        requireDraftForManage(principal, versionId);
    List<CompetitionTheoryExamSessionEntity> sessions =
        sessions(principal.getTenantId(), versionId);
    validateSessionSet(principal.getTenantId(), version, sessions);
    List<CompetitionTheoryCandidateRow> candidates = safe(
        assignmentMapper.selectEligibleCandidates(
            principal.getTenantId(), version.getEventId(), version.getStageId(),
            version.getEventWorkTypeId()));
    List<CompetitionTheoryCandidateAssignmentEntity> assignments =
        assignmentEntities(principal.getTenantId(), versionId);
    lockCandidatesForWrite(principal.getTenantId(), version.getEventId(), version.getStageId(),
        version.getEventWorkTypeId(), assignments.stream()
            .map(CompetitionTheoryCandidateAssignmentEntity::getContestantMemberId)
            .sorted()
            .toList());
    validatePublishAssignments(principal.getTenantId(), version, sessions, candidates, assignments);

    LocalDateTime now = LocalDateTime.now();
    version.setStatus("published");
    version.setPublishedAt(now);
    version.setPublishedBy(principal.getUserId());
    auditUpdate(version, principal.getUserId(), now);
    versionMapper.updateById(version);
    for (CompetitionTheoryExamSessionEntity session : sessions) {
      session.setStatus("published");
      auditUpdate(session, principal.getUserId(), now);
      sessionMapper.updateById(session);
    }
    for (CompetitionTheoryCandidateAssignmentEntity assignment : assignments) {
      assignment.setStatus("published");
      auditUpdate(assignment, principal.getUserId(), now);
      assignmentMapper.updateById(assignment);
    }
    createScheduleMessages(principal.getTenantId(), principal.getUserId(), version,
        assignments, sessions, "theory_schedule_published", "理论考试安排已发布", now);
    return detailInternal(principal.getTenantId(), version);
  }

  @Transactional
  public CompetitionTheoryScheduleVersionVO withdraw(Long versionId, String reason) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionTheoryScheduleVersionEntity version =
        versionMapper.selectForUpdate(principal.getTenantId(), versionId);
    if (version == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考试安排不存在");
    }
    scoreRuleApplicationService.manageableEvent(principal, version.getEventId());
    if (!"published".equals(version.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "仅已发布安排可以撤销");
    }
    if (!StringUtils.hasText(reason)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "撤销原因不能为空");
    }
    LocalDateTime now = LocalDateTime.now();
    List<CompetitionTheoryExamSessionEntity> sessions =
        sessions(principal.getTenantId(), versionId);
    List<CompetitionTheoryCandidateAssignmentEntity> assignments =
        assignmentEntities(principal.getTenantId(), versionId);
    version.setStatus("withdrawn");
    version.setWithdrawnAt(now);
    version.setWithdrawnBy(principal.getUserId());
    version.setWithdrawReason(reason.trim());
    auditUpdate(version, principal.getUserId(), now);
    versionMapper.updateById(version);
    for (CompetitionTheoryExamSessionEntity session : sessions) {
      session.setStatus("withdrawn");
      auditUpdate(session, principal.getUserId(), now);
      sessionMapper.updateById(session);
    }
    for (CompetitionTheoryCandidateAssignmentEntity assignment : assignments) {
      assignment.setStatus("withdrawn");
      auditUpdate(assignment, principal.getUserId(), now);
      assignmentMapper.updateById(assignment);
    }
    createScheduleMessages(principal.getTenantId(), principal.getUserId(), version,
        assignments, sessions, "theory_schedule_withdrawn", "理论考试安排已变更", now);
    return detailInternal(principal.getTenantId(), version);
  }

  @Transactional(readOnly = true)
  public List<CompetitionTheoryRosterRow> rosterRows(Long versionId, boolean requirePublished) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionTheoryScheduleVersionEntity version =
        requireVersion(principal.getTenantId(), versionId);
    scoreRuleApplicationService.accessibleEvent(principal, version.getEventId());
    if (requirePublished && !"published".equals(version.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "仅已发布安排可以导出名单");
    }
    return safe(assignmentMapper.selectRosterRows(principal.getTenantId(), versionId));
  }

  @Transactional(readOnly = true)
  public List<CompetitionPlayerTheoryScheduleVO> currentPlayerSchedules(
      Long tenantId, Long playerId) {
    return safe(assignmentMapper.selectPublishedByPlayer(tenantId, playerId)).stream()
        .map(row -> new CompetitionPlayerTheoryScheduleVO(
            row.getScheduleVersionId(), row.getEventId(), row.getEventName(), row.getWorkTypeName(),
            row.getSessionName(), row.getStartAt(), row.getEndAt(), row.getRoomName(),
            row.getAddress(), row.getSeatNo(), row.getContestantNo()))
        .toList();
  }

  private void lockCandidatesForWrite(
      Long tenantId, Long eventId, Long stageId, Long eventWorkTypeId, List<Long> memberIds) {
    if (contestantScopeValidator == null || memberIds == null) {
      return;
    }
    memberIds.stream()
        .filter(Objects::nonNull)
        .distinct()
        .sorted()
        .forEach(memberId -> lockCandidateForWrite(
            tenantId, eventId, stageId, eventWorkTypeId, memberId));
  }

  private void lockCandidateForWrite(
      Long tenantId, Long eventId, Long stageId, Long eventWorkTypeId, Long memberId) {
    if (contestantScopeValidator == null || memberId == null) {
      return;
    }
    contestantScopeValidator.validateForUpdate(
        tenantId, eventId, stageId, eventWorkTypeId, memberId);
  }

  private CompetitionTheorySessionVO saveSession(
      Long versionId, Long sessionId, CompetitionTheorySessionUpsertReq req) {
    PlatformUserPrincipal principal = currentUser();
    CompetitionTheoryScheduleVersionEntity version =
        requireDraftForManage(principal, versionId);
    validateTime(req.startAt(), req.endAt());
    CompetitionTheoryPaperSnapshotEntity paper =
        requirePaperSnapshot(principal.getTenantId(), version, req.paperSnapshotId());
    if (!req.sessionCode().equalsIgnoreCase(paper.getPaperCode())) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "场次卷别必须与试卷快照卷别一致");
    }
    long actualMinutes = Duration.between(req.startAt(), req.endAt()).toMinutes();
    if (Duration.between(req.startAt(), req.endAt()).getSeconds() % 60 != 0
        || actualMinutes != paper.getDurationMinutes()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "场次时长必须与试卷时长一致");
    }
    List<Long> roomIds = new ArrayList<>(new LinkedHashSet<>(req.roomIds()));
    if (roomIds.size() != req.roomIds().size()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "同一场次不能重复选择考场");
    }
    List<CompetitionTheoryExamRoomEntity> rooms = requireEnabledRooms(
        principal.getTenantId(), version.getEventId(), roomIds);
    Long excluded = sessionId == null ? 0L : sessionId;
    for (CompetitionTheoryExamRoomEntity room : rooms) {
      if (sessionMapper.countRoomTimeConflicts(
          principal.getTenantId(), room.getId(), excluded, req.startAt(), req.endAt()) > 0) {
        throw new BusinessException(
            ErrorCode.CONFLICT, "考场“" + room.getName() + "”在该时段已被占用");
      }
    }
    LocalDateTime now = LocalDateTime.now();
    CompetitionTheoryExamSessionEntity session;
    if (sessionId == null) {
      session = new CompetitionTheoryExamSessionEntity();
      session.setTenantId(principal.getTenantId());
      session.setScheduleVersionId(versionId);
      session.setEventId(version.getEventId());
      session.setStageId(version.getStageId());
      session.setEventWorkTypeId(version.getEventWorkTypeId());
      session.setStatus("draft");
      auditCreate(session, principal.getUserId(), now);
    } else {
      session = requireSession(principal.getTenantId(), versionId, sessionId);
      clearAssignments(principal.getTenantId(), versionId);
      deleteSessionRooms(principal.getTenantId(), sessionId);
      auditUpdate(session, principal.getUserId(), now);
    }
    session.setPaperSnapshotId(paper.getId());
    session.setSessionCode(req.sessionCode().toUpperCase());
    session.setSessionName(req.sessionName().trim());
    session.setStartAt(req.startAt());
    session.setEndAt(req.endAt());
    session.setTotalCapacity(rooms.stream().mapToInt(CompetitionTheoryExamRoomEntity::getCapacity).sum());
    if (sessionId == null) {
      requireInsert(sessionMapper.insert(session), "考试场次创建失败");
    } else {
      sessionMapper.updateById(session);
    }
    int sortOrder = 1;
    for (CompetitionTheoryExamRoomEntity room : rooms) {
      CompetitionTheorySessionRoomEntity relation = new CompetitionTheorySessionRoomEntity();
      relation.setTenantId(principal.getTenantId());
      relation.setScheduleVersionId(versionId);
      relation.setSessionId(session.getId());
      relation.setRoomId(room.getId());
      relation.setRoomNameSnapshot(room.getName());
      relation.setAddressSnapshot(room.getAddress());
      relation.setCapacitySnapshot(room.getCapacity());
      relation.setSortOrder(sortOrder++);
      auditCreate(relation, principal.getUserId(), now);
      requireInsert(sessionRoomMapper.insert(relation), "场次考场保存失败");
    }
    touchVersion(version, principal.getUserId());
    return toSessionVO(principal.getTenantId(), session);
  }

  private void validatePublishAssignments(
      Long tenantId,
      CompetitionTheoryScheduleVersionEntity version,
      List<CompetitionTheoryExamSessionEntity> sessions,
      List<CompetitionTheoryCandidateRow> candidates,
      List<CompetitionTheoryCandidateAssignmentEntity> assignments) {
    if (candidates.isEmpty()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "当前范围没有可发布的合格选手");
    }
    Set<Long> candidateIds = new HashSet<>();
    for (CompetitionTheoryCandidateRow candidate : candidates) {
      candidateIds.add(candidate.getContestantProfileId());
    }
    Set<Long> assignedIds = new HashSet<>();
    Map<Long, CompetitionTheoryExamSessionEntity> sessionsById = new HashMap<>();
    sessions.forEach(session -> sessionsById.put(session.getId(), session));
    for (CompetitionTheoryCandidateAssignmentEntity assignment : assignments) {
      if (!assignedIds.add(assignment.getContestantProfileId())
          || !candidateIds.contains(assignment.getContestantProfileId())) {
        throw new BusinessException(ErrorCode.CONFLICT, "分配名单存在重复或已失效选手");
      }
      CompetitionTheoryExamSessionEntity session = sessionsById.get(assignment.getSessionId());
      if (session == null) {
        throw new BusinessException(ErrorCode.CONFLICT, "分配名单引用了无效场次");
      }
      if (sessionMapper.countCandidateTimeConflicts(
          tenantId, assignment.getContestantProfileId(), version.getId(),
          session.getStartAt(), session.getEndAt()) > 0) {
        throw new BusinessException(ErrorCode.CONFLICT, "存在选手考试时间冲突");
      }
    }
    if (candidateIds.size() != assignedIds.size() || !assignedIds.containsAll(candidateIds)) {
      throw new BusinessException(
          ErrorCode.UNPROCESSABLE_ENTITY,
          "分配名单与当前合格选手不一致，请重新随机分配");
    }
  }

  private void validateSessionSet(
      Long tenantId,
      CompetitionTheoryScheduleVersionEntity version,
      List<CompetitionTheoryExamSessionEntity> sessions) {
    if (sessions.isEmpty() || sessions.size() > 2) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "必须配置A场，可选配置B场");
    }
    Map<String, CompetitionTheoryExamSessionEntity> byCode = new HashMap<>();
    for (CompetitionTheoryExamSessionEntity session : sessions) {
      if (byCode.put(session.getSessionCode(), session) != null) {
        throw new BusinessException(ErrorCode.CONFLICT, "同一安排存在重复场次卷别");
      }
      requirePaperSnapshot(tenantId, version, session.getPaperSnapshotId());
      List<CompetitionTheorySessionRoomEntity> rooms = sessionRooms(tenantId, session.getId());
      int capacity = rooms.stream()
          .mapToInt(CompetitionTheorySessionRoomEntity::getCapacitySnapshot).sum();
      if (rooms.isEmpty() || capacity != session.getTotalCapacity()) {
        throw new BusinessException(ErrorCode.CONFLICT, "场次考场或容量快照不完整");
      }
      for (CompetitionTheorySessionRoomEntity room : rooms) {
        if (sessionMapper.countRoomTimeConflicts(
            tenantId, room.getRoomId(), session.getId(),
            session.getStartAt(), session.getEndAt()) > 0) {
          throw new BusinessException(ErrorCode.CONFLICT, "存在考场时间冲突");
        }
      }
    }
    if (!byCode.containsKey("A")) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "必须配置A场");
    }
    if (byCode.containsKey("B")) {
      CompetitionTheoryExamSessionEntity a = byCode.get("A");
      CompetitionTheoryExamSessionEntity b = byCode.get("B");
      boolean overlap = a.getStartAt().isBefore(b.getEndAt())
          && a.getEndAt().isAfter(b.getStartAt());
      if (overlap) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "A/B场次时间不得重叠");
      }
    }
  }

  private CompetitionTheoryScheduleVersionVO detailInternal(
      Long tenantId, CompetitionTheoryScheduleVersionEntity version) {
    List<CompetitionTheoryExamSessionEntity> sessionEntities =
        sessions(tenantId, version.getId());
    List<CompetitionTheorySessionVO> sessionVOs =
        sessionEntities.stream().map(session -> toSessionVO(tenantId, session)).toList();
    List<CompetitionTheoryRosterRow> rows =
        safe(assignmentMapper.selectRosterRows(tenantId, version.getId()));
    int eligibleCount = safe(assignmentMapper.selectEligibleCandidates(
        tenantId, version.getEventId(), version.getStageId(), version.getEventWorkTypeId())).size();
    int totalCapacity = sessionEntities.stream()
        .mapToInt(session -> session.getTotalCapacity() == null ? 0 : session.getTotalCapacity())
        .sum();
    return new CompetitionTheoryScheduleVersionVO(
        version.getId(), version.getEventId(), version.getStageId(),
        version.getEventWorkTypeId(), version.getScoreRuleId(), version.getVersionNo(),
        version.getStatus(), version.getRandomBatchId(), version.getGeneratedAt(),
        version.getPublishedAt(), version.getWithdrawnAt(), version.getWithdrawReason(),
        eligibleCount, rows.size(), totalCapacity, Math.max(0, eligibleCount - totalCapacity),
        sessionVOs, rows.stream().map(this::toAssignmentVO).toList());
  }

  private CompetitionTheorySessionVO toSessionVO(
      Long tenantId, CompetitionTheoryExamSessionEntity session) {
    List<CompetitionTheorySessionRoomVO> roomVOs = sessionRooms(tenantId, session.getId())
        .stream()
        .map(room -> new CompetitionTheorySessionRoomVO(
            room.getId(), room.getRoomId(), room.getRoomNameSnapshot(),
            room.getAddressSnapshot(), room.getCapacitySnapshot(), room.getSortOrder()))
        .toList();
    return new CompetitionTheorySessionVO(
        session.getId(), session.getPaperSnapshotId(), session.getSessionCode(),
        session.getSessionName(), session.getStartAt(), session.getEndAt(),
        session.getTotalCapacity(), session.getStatus(), roomVOs);
  }

  private CompetitionTheoryAssignmentVO toAssignmentVO(CompetitionTheoryRosterRow row) {
    return new CompetitionTheoryAssignmentVO(
        row.getAssignmentId(), row.getPlayerId(), row.getSessionId(), row.getEventName(),
        row.getWorkTypeName(), row.getSessionName(), row.getStartAt(), row.getEndAt(),
        row.getRoomName(), row.getAddress(), row.getSeatNo(), row.getContestantNo(),
        row.getContestantName(), row.getOrgName());
  }

  private CompetitionTheoryRoomVO toRoomVO(CompetitionTheoryExamRoomEntity room) {
    return new CompetitionTheoryRoomVO(
        room.getId(), room.getEventId(), room.getName(), room.getAddress(),
        room.getCapacity(), room.getStatus(), room.getRemark());
  }

  private CompetitionTheoryScheduleVersionEntity requireDraftForManage(
      PlatformUserPrincipal principal, Long versionId) {
    CompetitionTheoryScheduleVersionEntity version =
        versionMapper.selectForUpdate(principal.getTenantId(), versionId);
    if (version == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考试安排不存在");
    }
    scoreRuleApplicationService.manageableEvent(principal, version.getEventId());
    if (!"draft".equals(version.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "已发布或已撤销安排不可修改");
    }
    return version;
  }

  private CompetitionTheoryScheduleVersionEntity requireVersion(Long tenantId, Long id) {
    CompetitionTheoryScheduleVersionEntity version = versionMapper.selectOne(
        new LambdaQueryWrapper<CompetitionTheoryScheduleVersionEntity>()
            .eq(CompetitionTheoryScheduleVersionEntity::getTenantId, tenantId)
            .eq(CompetitionTheoryScheduleVersionEntity::getId, id)
            .eq(CompetitionTheoryScheduleVersionEntity::getDeleted, 0));
    if (version == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考试安排不存在");
    }
    return version;
  }

  private CompetitionTheoryExamRoomEntity requireRoom(Long tenantId, Long id) {
    CompetitionTheoryExamRoomEntity room = roomMapper.selectOne(
        new LambdaQueryWrapper<CompetitionTheoryExamRoomEntity>()
            .eq(CompetitionTheoryExamRoomEntity::getTenantId, tenantId)
            .eq(CompetitionTheoryExamRoomEntity::getId, id)
            .eq(CompetitionTheoryExamRoomEntity::getDeleted, 0));
    if (room == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考场不存在");
    }
    return room;
  }

  private CompetitionTheoryExamSessionEntity requireSession(
      Long tenantId, Long versionId, Long sessionId) {
    CompetitionTheoryExamSessionEntity session = sessionMapper.selectOne(
        new LambdaQueryWrapper<CompetitionTheoryExamSessionEntity>()
            .eq(CompetitionTheoryExamSessionEntity::getTenantId, tenantId)
            .eq(CompetitionTheoryExamSessionEntity::getScheduleVersionId, versionId)
            .eq(CompetitionTheoryExamSessionEntity::getId, sessionId)
            .eq(CompetitionTheoryExamSessionEntity::getDeleted, 0));
    if (session == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考试场次不存在");
    }
    return session;
  }

  private CompetitionTheorySessionRoomEntity requireSessionRoom(
      Long tenantId, Long versionId, Long sessionRoomId) {
    CompetitionTheorySessionRoomEntity room = sessionRoomMapper.selectOne(
        new LambdaQueryWrapper<CompetitionTheorySessionRoomEntity>()
            .eq(CompetitionTheorySessionRoomEntity::getTenantId, tenantId)
            .eq(CompetitionTheorySessionRoomEntity::getScheduleVersionId, versionId)
            .eq(CompetitionTheorySessionRoomEntity::getId, sessionRoomId)
            .eq(CompetitionTheorySessionRoomEntity::getDeleted, 0));
    if (room == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "目标场次考场不存在");
    }
    return room;
  }

  private CompetitionTheoryCandidateAssignmentEntity requireAssignment(
      Long tenantId, Long assignmentId) {
    CompetitionTheoryCandidateAssignmentEntity assignment = assignmentMapper.selectOne(
        new LambdaQueryWrapper<CompetitionTheoryCandidateAssignmentEntity>()
            .eq(CompetitionTheoryCandidateAssignmentEntity::getTenantId, tenantId)
            .eq(CompetitionTheoryCandidateAssignmentEntity::getId, assignmentId)
            .eq(CompetitionTheoryCandidateAssignmentEntity::getDeleted, 0));
    if (assignment == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "选手分配不存在");
    }
    return assignment;
  }

  private CompetitionTheoryPaperSnapshotEntity requirePaperSnapshot(
      Long tenantId,
      CompetitionTheoryScheduleVersionEntity version,
      Long paperSnapshotId) {
    CompetitionTheoryPaperSnapshotEntity paper = paperSnapshotMapper.selectOne(
        new LambdaQueryWrapper<CompetitionTheoryPaperSnapshotEntity>()
            .eq(CompetitionTheoryPaperSnapshotEntity::getTenantId, tenantId)
            .eq(CompetitionTheoryPaperSnapshotEntity::getId, paperSnapshotId)
            .eq(CompetitionTheoryPaperSnapshotEntity::getEventId, version.getEventId())
            .eq(CompetitionTheoryPaperSnapshotEntity::getStageId, version.getStageId())
            .eq(CompetitionTheoryPaperSnapshotEntity::getEventWorkTypeId,
                version.getEventWorkTypeId())
            .eq(CompetitionTheoryPaperSnapshotEntity::getScoreRuleId, version.getScoreRuleId())
            .eq(CompetitionTheoryPaperSnapshotEntity::getStatus, "published")
            .eq(CompetitionTheoryPaperSnapshotEntity::getDeleted, 0));
    if (paper == null) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "试卷快照未发布或与安排范围不匹配");
    }
    return paper;
  }

  private List<CompetitionTheoryExamRoomEntity> requireEnabledRooms(
      Long tenantId, Long eventId, List<Long> roomIds) {
    List<CompetitionTheoryExamRoomEntity> rooms = safe(roomMapper.selectList(
        new LambdaQueryWrapper<CompetitionTheoryExamRoomEntity>()
            .eq(CompetitionTheoryExamRoomEntity::getTenantId, tenantId)
            .eq(CompetitionTheoryExamRoomEntity::getEventId, eventId)
            .in(CompetitionTheoryExamRoomEntity::getId, roomIds)
            .eq(CompetitionTheoryExamRoomEntity::getStatus, "enabled")
            .eq(CompetitionTheoryExamRoomEntity::getDeleted, 0)));
    Map<Long, CompetitionTheoryExamRoomEntity> byId = new HashMap<>();
    rooms.forEach(room -> byId.put(room.getId(), room));
    List<CompetitionTheoryExamRoomEntity> ordered = new ArrayList<>();
    for (Long roomId : roomIds) {
      CompetitionTheoryExamRoomEntity room = byId.get(roomId);
      if (room == null) {
        throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "所选考场不存在、已停用或不属于当前赛事");
      }
      ordered.add(room);
    }
    return ordered;
  }

  private List<CompetitionTheoryExamSessionEntity> sessions(Long tenantId, Long versionId) {
    return safe(sessionMapper.selectList(
        new LambdaQueryWrapper<CompetitionTheoryExamSessionEntity>()
            .eq(CompetitionTheoryExamSessionEntity::getTenantId, tenantId)
            .eq(CompetitionTheoryExamSessionEntity::getScheduleVersionId, versionId)
            .eq(CompetitionTheoryExamSessionEntity::getDeleted, 0)
            .orderByAsc(CompetitionTheoryExamSessionEntity::getSessionCode)));
  }

  private List<CompetitionTheorySessionRoomEntity> sessionRooms(
      Long tenantId, Long sessionId) {
    return safe(sessionRoomMapper.selectList(
        new LambdaQueryWrapper<CompetitionTheorySessionRoomEntity>()
            .eq(CompetitionTheorySessionRoomEntity::getTenantId, tenantId)
            .eq(CompetitionTheorySessionRoomEntity::getSessionId, sessionId)
            .eq(CompetitionTheorySessionRoomEntity::getDeleted, 0)
            .orderByAsc(CompetitionTheorySessionRoomEntity::getSortOrder)));
  }

  private List<CompetitionTheoryCandidateAssignmentEntity> assignmentEntities(
      Long tenantId, Long versionId) {
    return safe(assignmentMapper.selectList(
        new LambdaQueryWrapper<CompetitionTheoryCandidateAssignmentEntity>()
            .eq(CompetitionTheoryCandidateAssignmentEntity::getTenantId, tenantId)
            .eq(CompetitionTheoryCandidateAssignmentEntity::getScheduleVersionId, versionId)
            .eq(CompetitionTheoryCandidateAssignmentEntity::getDeleted, 0)));
  }

  private List<SessionSlot> sessionSlots(
      Long tenantId, List<CompetitionTheoryExamSessionEntity> sessions) {
    List<SessionSlot> result = new ArrayList<>();
    for (CompetitionTheoryExamSessionEntity session : sessions) {
      List<RoomSlot> rooms = sessionRooms(tenantId, session.getId()).stream()
          .map(room -> new RoomSlot(
              room.getId(), room.getRoomId(), room.getCapacitySnapshot(), room.getSortOrder()))
          .toList();
      result.add(new SessionSlot(
          session.getId(), session.getSessionCode(), session.getTotalCapacity(), rooms));
    }
    return result;
  }

  private void clearAssignments(Long tenantId, Long versionId) {
    List<Long> ids = assignmentEntities(tenantId, versionId).stream()
        .map(CompetitionTheoryCandidateAssignmentEntity::getId).toList();
    if (!ids.isEmpty()) {
      assignmentMapper.deleteBatchIds(ids);
    }
  }

  private void deleteSessionRooms(Long tenantId, Long sessionId) {
    List<Long> ids = sessionRooms(tenantId, sessionId).stream()
        .map(CompetitionTheorySessionRoomEntity::getId).toList();
    if (!ids.isEmpty()) {
      sessionRoomMapper.deleteBatchIds(ids);
    }
  }

  private String firstAvailableSeat(Long tenantId, Long sessionRoomId, int capacity) {
    Set<String> occupied = new HashSet<>();
    assignmentMapper.selectList(
            new LambdaQueryWrapper<CompetitionTheoryCandidateAssignmentEntity>()
                .eq(CompetitionTheoryCandidateAssignmentEntity::getTenantId, tenantId)
                .eq(CompetitionTheoryCandidateAssignmentEntity::getSessionRoomId, sessionRoomId)
                .eq(CompetitionTheoryCandidateAssignmentEntity::getDeleted, 0))
        .forEach(assignment -> occupied.add(assignment.getSeatNo()));
    for (int i = 1; i <= capacity; i++) {
      String seat = String.format("%03d", i);
      if (!occupied.contains(seat)) {
        return seat;
      }
    }
    throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "目标考场已满");
  }

  private void createScheduleMessages(
      Long tenantId,
      Long operatorId,
      CompetitionTheoryScheduleVersionEntity version,
      List<CompetitionTheoryCandidateAssignmentEntity> assignments,
      List<CompetitionTheoryExamSessionEntity> sessions,
      String type,
      String title,
      LocalDateTime now) {
    Map<Long, CompetitionTheoryExamSessionEntity> sessionsById = new HashMap<>();
    sessions.forEach(session -> sessionsById.put(session.getId(), session));
    for (CompetitionTheoryCandidateAssignmentEntity assignment : assignments) {
      if (assignment.getPlayerId() == null) {
        continue;
      }
      CompetitionTheoryExamSessionEntity session = sessionsById.get(assignment.getSessionId());
      String content = "published".equals(version.getStatus()) && session != null
          ? "您的理论考试安排已发布，请前往个人中心查看考场、座位和考试时间。"
          : "原理论考试安排已撤销，请留意后续重新发布的考试安排。";
      CompetitionPlayerMessageEntity message = new CompetitionPlayerMessageEntity();
      message.setId(IdWorker.getId());
      message.setTenantId(tenantId);
      message.setPlayerId(assignment.getPlayerId());
      message.setEventId(version.getEventId());
      message.setScheduleVersionId(version.getId());
      message.setType(type);
      message.setTitle(title);
      message.setContent(content);
      message.setPublishedAt(now);
      message.setReceivedAt(now);
      auditCreate(message, operatorId, now);
      playerMessageMapper.insertIgnoreDuplicate(message);
    }
  }

  private void applyRoom(
      CompetitionTheoryExamRoomEntity room, CompetitionTheoryRoomUpsertReq req) {
    String status = StringUtils.hasText(req.status()) ? req.status().trim() : "enabled";
    if (!ROOM_STATUSES.contains(status)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "考场状态不合法");
    }
    if (req.capacity() == null || req.capacity() <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "考场容量必须大于0");
    }
    room.setName(req.name().trim());
    room.setAddress(req.address().trim());
    room.setCapacity(req.capacity());
    room.setStatus(status);
    room.setRemark(StringUtils.hasText(req.remark()) ? req.remark().trim() : null);
  }

  private void validateTime(LocalDateTime startAt, LocalDateTime endAt) {
    if (startAt == null || endAt == null || !endAt.isAfter(startAt)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "场次结束时间必须晚于开始时间");
    }
  }

  private void touchVersion(
      CompetitionTheoryScheduleVersionEntity version, Long operatorId) {
    version.setRandomBatchId(null);
    version.setRandomSeed(null);
    version.setGeneratedAt(null);
    version.setGeneratedBy(null);
    auditUpdate(version, operatorId, LocalDateTime.now());
    versionMapper.updateById(version);
  }

  private void requireInsert(int affected, String message) {
    if (affected != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, message);
    }
  }

  private void auditCreate(
      com.guoyu.platform.common.domain.BaseEntity entity,
      Long operatorId,
      LocalDateTime now) {
    entity.setCreatedBy(operatorId);
    entity.setUpdatedBy(operatorId);
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);
    entity.setDeleted(0);
  }

  private void auditUpdate(
      com.guoyu.platform.common.domain.BaseEntity entity,
      Long operatorId,
      LocalDateTime now) {
    entity.setUpdatedBy(operatorId);
    entity.setUpdatedAt(now);
  }

  private <T> List<T> safe(List<T> values) {
    return values == null ? List.of() : values;
  }

  private PlatformUserPrincipal currentUser() {
    try {
      return SecurityContext.currentUser();
    } catch (IllegalStateException ex) {
      log.warn("Competition theory schedule request has no authenticated platform user");
      throw ex;
    }
  }
}
