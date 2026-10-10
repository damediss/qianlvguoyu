package com.guoyu.platform.certification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.guoyu.platform.certification.dto.CertificationCandidateExamSessionSaveReq;
import com.guoyu.platform.certification.dto.CertificationExamSessionCandidateAssignReq;
import com.guoyu.platform.certification.dto.CertificationExamSessionSaveReq;
import com.guoyu.platform.certification.entity.CertificationCandidateEntity;
import com.guoyu.platform.certification.entity.CertificationCandidateExamSessionEntity;
import com.guoyu.platform.certification.entity.CertificationExamSessionEntity;
import com.guoyu.platform.certification.entity.CertificationTaskEntity;
import com.guoyu.platform.certification.entity.CertificationTaskScopeEntity;
import com.guoyu.platform.certification.mapper.CertificationCandidateExamSessionMapper;
import com.guoyu.platform.certification.mapper.CertificationCandidateMapper;
import com.guoyu.platform.certification.mapper.CertificationExamSessionMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskScopeMapper;
import com.guoyu.platform.certification.vo.CertificationCandidateExamSessionVO;
import com.guoyu.platform.certification.vo.CertificationExamSessionCandidateVO;
import com.guoyu.platform.certification.vo.CertificationExamSessionVO;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CertificationExamSessionApplicationService {

  private static final String THEORY = "THEORY";
  private static final String PRACTICE = "PRACTICE";
  private static final String ASSIGNED = "ASSIGNED";
  private static final String EXEMPT = "EXEMPT";
  private static final String CANCELLED = "CANCELLED";
  private static final String PENDING = "PENDING";
  private static final String CONFIGURED = "CONFIGURED";

  private final CertificationTaskMapper taskMapper;
  private final CertificationTaskScopeMapper scopeMapper;
  private final CertificationCandidateMapper candidateMapper;
  private final CertificationExamSessionMapper sessionMapper;
  private final CertificationCandidateExamSessionMapper relationMapper;

  public CertificationExamSessionApplicationService(
      CertificationTaskMapper taskMapper,
      CertificationTaskScopeMapper scopeMapper,
      CertificationCandidateMapper candidateMapper,
      CertificationExamSessionMapper sessionMapper,
      CertificationCandidateExamSessionMapper relationMapper
  ) {
    this.taskMapper = taskMapper;
    this.scopeMapper = scopeMapper;
    this.candidateMapper = candidateMapper;
    this.sessionMapper = sessionMapper;
    this.relationMapper = relationMapper;
  }

  @Transactional
  public CertificationExamSessionVO create(Long taskId, CertificationExamSessionSaveReq req) {
    CertificationTaskEntity task = requireTask(taskId);
    validateRequest(taskId, req);
    CertificationExamSessionEntity entity = new CertificationExamSessionEntity();
    entity.setId(IdWorker.getId());
    entity.setTaskId(taskId);
    apply(entity, req);
    entity.setStatus(normalizeStatus(req.status()));
    entity.setDeleted(0);
    sessionMapper.insert(entity);
    return CertificationExamSessionVO.from(entity);
  }

  @Transactional(readOnly = true)
  public List<CertificationExamSessionVO> list(Long taskId) {
    requireTask(taskId);
    List<CertificationExamSessionEntity> sessions = sessionMapper.selectList(new LambdaQueryWrapper<CertificationExamSessionEntity>()
        .eq(CertificationExamSessionEntity::getTaskId, taskId)
        .orderByAsc(CertificationExamSessionEntity::getExamDate)
        .orderByAsc(CertificationExamSessionEntity::getStartTime)
        .orderByAsc(CertificationExamSessionEntity::getSessionName));
    return sessions == null ? List.of() : sessions.stream().map(CertificationExamSessionVO::from).toList();
  }

  @Transactional(readOnly = true)
  public List<CertificationExamSessionCandidateVO> listSessionCandidates(Long sessionId) {
    CertificationExamSessionEntity session = requireSession(sessionId);
    List<CertificationCandidateExamSessionEntity> relations = relationMapper.selectBySession(
        tenantId(), session.getTaskId(), sessionId);
    List<CertificationCandidateEntity> candidates = candidateMapper.selectList(new LambdaQueryWrapper<CertificationCandidateEntity>()
        .eq(CertificationCandidateEntity::getTaskId, session.getTaskId())
        .eq(CertificationCandidateEntity::getDeleted, 0)
        .orderByAsc(CertificationCandidateEntity::getId));
    if (candidates == null || candidates.isEmpty()) {
      return List.of();
    }
    Map<Long, CertificationCandidateExamSessionEntity> relationMap = (relations == null ? List.<CertificationCandidateExamSessionEntity>of() : relations)
        .stream().collect(Collectors.toMap(CertificationCandidateExamSessionEntity::getCandidateId, Function.identity(), (left, right) -> left));
    return candidates.stream()
        .map(candidate -> {
          CertificationCandidateExamSessionEntity relation = relationMap.get(candidate.getId());
          return new CertificationExamSessionCandidateVO(
              candidate.getId(), candidate.getCandidateName(), candidate.getIdCardNo(),
              candidate.getAdmissionTicketNo(),
              relation == null ? "UNASSIGNED" : relation.getParticipationStatus(),
              relation == null ? PENDING : relation.getCollectionStatus());
        })
        .toList();
  }

  @Transactional
  public List<CertificationExamSessionCandidateVO> assignSessionCandidates(
      Long sessionId, CertificationExamSessionCandidateAssignReq req
  ) {
    CertificationExamSessionEntity session = requireSession(sessionId);
    List<Long> requestedIds = (req.candidateIds() == null ? List.<Long>of() : req.candidateIds()).stream()
        .filter(java.util.Objects::nonNull).distinct().toList();
    List<CertificationCandidateEntity> candidates = requestedIds.isEmpty()
        ? List.of()
        : candidateMapper.selectList(new LambdaQueryWrapper<CertificationCandidateEntity>()
            .eq(CertificationCandidateEntity::getTaskId, session.getTaskId())
            .in(CertificationCandidateEntity::getId, requestedIds)
            .eq(CertificationCandidateEntity::getDeleted, 0));
    List<CertificationCandidateEntity> safeCandidates = candidates == null ? List.of() : candidates;
    if (safeCandidates.size() != requestedIds.size()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "存在不属于当前认定任务的考生");
    }
    Map<Long, Boolean> requested = new HashMap<>();
    requestedIds.forEach(id -> requested.put(id, Boolean.TRUE));
    List<CertificationCandidateExamSessionEntity> existing = relationMapper.selectBySession(
        tenantId(), session.getTaskId(), sessionId);
    if (existing != null) {
      existing.stream().filter(relation -> !requested.containsKey(relation.getCandidateId())).forEach(relation -> {
        relation.setParticipationStatus(CANCELLED);
        relation.setCollectionStatus(defaultCollectionStatus(relation));
        relationMapper.updateById(relation);
      });
    }
    for (Long candidateId : requestedIds) {
      upsertCandidateSession(candidateId, sessionId, new CertificationCandidateExamSessionSaveReq(
          ASSIGNED, PENDING, null, null, null));
    }
    return listSessionCandidates(sessionId);
  }

  @Transactional
  public CertificationExamSessionVO completeCollectionConfig(Long sessionId) {
    CertificationExamSessionEntity session = requireSession(sessionId);
    List<CertificationCandidateExamSessionEntity> relations = relationMapper.selectBySession(
        tenantId(), session.getTaskId(), sessionId);
    long assignedCount = relations == null ? 0 : relations.stream()
        .filter(relation -> ASSIGNED.equalsIgnoreCase(relation.getParticipationStatus()))
        .count();
    if (assignedCount == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请先为该场次设置至少一名考生");
    }
    session.setStatus(CONFIGURED);
    sessionMapper.updateById(session);
    return CertificationExamSessionVO.from(session);
  }

  @Transactional
  public CertificationExamSessionVO update(Long sessionId, CertificationExamSessionSaveReq req) {
    CertificationExamSessionEntity entity = requireSession(sessionId);
    validateRequest(entity.getTaskId(), req);
    apply(entity, req);
    entity.setStatus(normalizeStatus(req.status()));
    sessionMapper.updateById(entity);
    return CertificationExamSessionVO.from(entity);
  }

  @Transactional
  public void delete(Long sessionId) {
    CertificationExamSessionEntity entity = requireSession(sessionId);
    long participantCount = relationMapper.selectCount(new LambdaQueryWrapper<CertificationCandidateExamSessionEntity>()
        .eq(CertificationCandidateExamSessionEntity::getExamSessionId, sessionId));
    if (participantCount > 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "考试场次已有考生参加记录，不能删除");
    }
    sessionMapper.deleteById(entity.getId());
  }

  @Transactional(readOnly = true)
  public List<CertificationCandidateExamSessionVO> listCandidateSessions(Long candidateId) {
    CertificationCandidateEntity candidate = requireCandidate(candidateId);
    List<CertificationCandidateExamSessionEntity> relations = relationMapper.selectByCandidate(
        tenantId(), candidate.getTaskId(), candidateId);
    if (relations == null || relations.isEmpty()) {
      return List.of();
    }
    List<CertificationExamSessionEntity> sessionRecords = sessionMapper.selectBatchIds(
        relations.stream().map(CertificationCandidateExamSessionEntity::getExamSessionId).toList());
    if (sessionRecords == null) {
      sessionRecords = List.of();
    }
    Map<Long, CertificationExamSessionEntity> sessions = sessionRecords
        .stream().collect(Collectors.toMap(CertificationExamSessionEntity::getId, Function.identity(), (left, right) -> left));
    return relations.stream()
        .map(relation -> {
          CertificationExamSessionEntity session = sessions.get(relation.getExamSessionId());
          return session == null ? null : CertificationCandidateExamSessionVO.from(relation, CertificationExamSessionVO.from(session));
        })
        .filter(java.util.Objects::nonNull)
        .sorted(Comparator.comparing(CertificationCandidateExamSessionVO::examType)
            .thenComparing(CertificationCandidateExamSessionVO::sessionName))
        .toList();
  }

  @Transactional
  public CertificationCandidateExamSessionVO upsertCandidateSession(
      Long candidateId, Long sessionId, CertificationCandidateExamSessionSaveReq req
  ) {
    CertificationCandidateEntity candidate = requireCandidate(candidateId);
    CertificationExamSessionEntity session = requireSession(sessionId);
    if (!candidate.getTaskId().equals(session.getTaskId())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "考生与考试场次不属于同一认定任务");
    }
    validateParticipation(req);
    CertificationCandidateExamSessionEntity relation = relationMapper.selectIncludingDeleted(
        tenantId(), candidate.getTaskId(), candidateId, sessionId);
    if (relation == null) {
      relation = new CertificationCandidateExamSessionEntity();
      relation.setId(IdWorker.getId());
      relation.setTenantId(tenantId());
      relation.setTaskId(candidate.getTaskId());
      relation.setCandidateId(candidateId);
      relation.setExamSessionId(sessionId);
      relation.setCollectionStatus(PENDING);
      relation.setDeleted(0);
      relation.setParticipationStatus(req.participationStatus().trim().toUpperCase(Locale.ROOT));
      relation.setCollectionSourceSessionId(req.collectionSourceSessionId());
      relation.setExemptReason(EXEMPT.equals(relation.getParticipationStatus()) ? req.exemptReason() : null);
      relation.setRemark(req.remark());
      relationMapper.insert(relation);
    } else {
      relation.setDeleted(0);
      relation.setParticipationStatus(req.participationStatus().trim().toUpperCase(Locale.ROOT));
      relation.setCollectionStatus(StringUtils.hasText(req.collectionStatus())
          ? req.collectionStatus().trim().toUpperCase(Locale.ROOT) : defaultCollectionStatus(relation));
      relation.setCollectionSourceSessionId(req.collectionSourceSessionId());
      relation.setExemptReason(EXEMPT.equals(relation.getParticipationStatus()) ? req.exemptReason() : null);
      relation.setRemark(req.remark());
      relationMapper.updateById(relation);
    }
    return CertificationCandidateExamSessionVO.from(relation, CertificationExamSessionVO.from(session));
  }

  @Transactional
  public void assignCandidateSessions(
      Long taskId,
      Long candidateId,
      List<Long> sessionIds,
      String participation,
      String exemptReason
  ) {
    if (sessionIds == null || sessionIds.isEmpty()) {
      return;
    }
    CertificationCandidateEntity candidate = requireCandidate(candidateId);
    if (!taskId.equals(candidate.getTaskId())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "考生不属于当前认定任务");
    }
    validateSessionIds(taskId, sessionIds);
    String normalized = StringUtils.hasText(participation)
        ? participation.trim().toUpperCase(Locale.ROOT) : ASSIGNED;
    CertificationCandidateExamSessionSaveReq req = new CertificationCandidateExamSessionSaveReq(
        normalized, PENDING, null, exemptReason, null);
    for (Long sessionId : sessionIds) {
      upsertCandidateSession(candidateId, sessionId, req);
    }
  }

  @Transactional
  public void assignBatchCandidates(Long taskId, Long importBatchId, List<Long> sessionIds, String defaultParticipation) {
    if (sessionIds == null || sessionIds.isEmpty()) {
      return;
    }
    requireTask(taskId);
    List<CertificationExamSessionEntity> sessions = sessionMapper.selectBatchIds(sessionIds);
    if (sessions == null) {
      sessions = List.of();
    }
    if (sessions == null || sessions.size() != sessionIds.stream().distinct().count()
        || sessions.stream().anyMatch(session -> !taskId.equals(session.getTaskId()))) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "导入所选考试场次不属于当前认定任务");
    }
    String participation = StringUtils.hasText(defaultParticipation)
        ? defaultParticipation.trim().toUpperCase(Locale.ROOT) : ASSIGNED;
    if (!ASSIGNED.equals(participation) && !CANCELLED.equals(participation)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的默认参加状态");
    }
    List<CertificationCandidateEntity> candidates = candidateMapper.selectList(new LambdaQueryWrapper<CertificationCandidateEntity>()
        .eq(CertificationCandidateEntity::getTaskId, taskId)
        .eq(CertificationCandidateEntity::getImportBatchId, importBatchId)
        .eq(CertificationCandidateEntity::getStatus, "active"));
    if (candidates == null) {
      return;
    }
    for (CertificationCandidateEntity candidate : candidates) {
      for (CertificationExamSessionEntity session : sessions) {
        CertificationCandidateExamSessionSaveReq req = new CertificationCandidateExamSessionSaveReq(
            participation, PENDING, null, null, null);
        upsertCandidateSession(candidate.getId(), session.getId(), req);
      }
    }
  }

  @Transactional(readOnly = true)
  public List<CertificationExamSessionEntity> validateSessionIds(Long taskId, List<Long> sessionIds) {
    if (sessionIds == null || sessionIds.isEmpty()) {
      return List.of();
    }
    requireTask(taskId);
    List<CertificationExamSessionEntity> sessions = sessionMapper.selectBatchIds(sessionIds);
    if (sessions == null) {
      sessions = List.of();
    }
    if (sessions.size() != sessionIds.stream().distinct().count()
        || sessions.stream().anyMatch(session -> !taskId.equals(session.getTaskId()))) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "所选考试场次不属于当前认定任务");
    }
    return sessions;
  }

  private void validateRequest(Long taskId, CertificationExamSessionSaveReq req) {
    String examType = normalizeExamType(req.examType());
    if (!THEORY.equals(examType) && !PRACTICE.equals(examType)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "考试类型必须为 THEORY 或 PRACTICE");
    }
    if (req.startTime() != null && req.endTime() != null && !req.startTime().isBefore(req.endTime())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "考试开始时间必须早于结束时间");
    }
    if (StringUtils.hasText(req.examVenue()) && req.examVenue().trim().length() > 200) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "考场场地不能超过200个字符");
    }
    if (req.taskScopeId() != null) {
      CertificationTaskScopeEntity scope = scopeMapper.selectById(req.taskScopeId());
      if (scope == null || !taskId.equals(scope.getTaskId())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "工种等级范围不属于当前认定任务");
      }
    }
  }

  private void apply(CertificationExamSessionEntity entity, CertificationExamSessionSaveReq req) {
    entity.setTaskScopeId(req.taskScopeId());
    entity.setExamType(normalizeExamType(req.examType()));
    entity.setSessionName(req.sessionName().trim());
    entity.setExamDate(req.examDate());
    entity.setStartTime(req.startTime());
    entity.setEndTime(req.endTime());
    entity.setExamSiteId(req.examSiteId());
    entity.setExamVenue(StringUtils.hasText(req.examVenue()) ? req.examVenue().trim() : null);
    // 认定规则固定：理论和实操均需完成现场信息采集，具体采集方式由现场采集流程统一处理。
    entity.setIdentityCheckRequired(true);
    entity.setFingerprintRequired(true);
    entity.setSignatureRequired(true);
    entity.setRemark(req.remark());
  }

  private String normalizeExamType(String value) {
    return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
  }

  private String normalizeStatus(String value) {
    if (!StringUtils.hasText(value)) {
      return "DRAFT";
    }
    String normalized = value.trim().toUpperCase(Locale.ROOT);
    if (!List.of("DRAFT", "CONFIGURED", "IN_PROGRESS", "ENDED").contains(normalized)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的考试场次状态");
    }
    return normalized;
  }

  private void validateParticipation(CertificationCandidateExamSessionSaveReq req) {
    String status = req.participationStatus().trim().toUpperCase(Locale.ROOT);
    if (!List.of(ASSIGNED, EXEMPT, CANCELLED).contains(status)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的考生参加状态");
    }
    if (EXEMPT.equals(status) && !StringUtils.hasText(req.exemptReason())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "免考状态必须填写免考原因");
    }
  }

  private String defaultCollectionStatus(CertificationCandidateExamSessionEntity relation) {
    return StringUtils.hasText(relation.getCollectionStatus()) ? relation.getCollectionStatus() : PENDING;
  }

  private CertificationTaskEntity requireTask(Long taskId) {
    CertificationTaskEntity task = taskMapper.selectById(taskId);
    if (task == null || !tenantId().equals(task.getTenantId()) || Integer.valueOf(1).equals(task.getDeleted())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "认定任务不存在");
    }
    return task;
  }

  private CertificationExamSessionEntity requireSession(Long sessionId) {
    CertificationExamSessionEntity session = sessionMapper.selectById(sessionId);
    if (session == null || !tenantId().equals(session.getTenantId()) || Integer.valueOf(1).equals(session.getDeleted())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考试场次不存在");
    }
    return session;
  }

  private CertificationCandidateEntity requireCandidate(Long candidateId) {
    CertificationCandidateEntity candidate = candidateMapper.selectById(candidateId);
    if (candidate == null || !tenantId().equals(candidate.getTenantId()) || Integer.valueOf(1).equals(candidate.getDeleted())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考生不存在");
    }
    return candidate;
  }

  private Long tenantId() {
    return SecurityContext.currentUser().getTenantId();
  }
}
