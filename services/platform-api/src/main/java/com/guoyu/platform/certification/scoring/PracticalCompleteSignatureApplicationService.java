package com.guoyu.platform.certification.scoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.certification.entity.CertificationAssessorTaskEntity;
import com.guoyu.platform.certification.entity.CertificationPracticalCandidateResultEntity;
import com.guoyu.platform.certification.entity.CertificationPracticalCompleteSignSessionEntity;
import com.guoyu.platform.certification.entity.CertificationPracticalCompleteSignatureEntity;
import com.guoyu.platform.certification.entity.CertificationTaskAssessorEntity;
import com.guoyu.platform.certification.entity.CertificationTaskEntity;
import com.guoyu.platform.certification.info.entity.CertificationInfoAssessorEntity;
import com.guoyu.platform.certification.info.mapper.CertificationInfoAssessorMapper;
import com.guoyu.platform.certification.mapper.CertificationAssessorTaskMapper;
import com.guoyu.platform.certification.mapper.CertificationPracticalCandidateResultMapper;
import com.guoyu.platform.certification.mapper.CertificationPracticalCompleteSignSessionMapper;
import com.guoyu.platform.certification.mapper.CertificationPracticalCompleteSignatureMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskAssessorMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.entity.FileEntity;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class PracticalCompleteSignatureApplicationService {

  static final String STATUS_NONE = "none";
  static final String STATUS_PENDING = "pending";
  static final String STATUS_SIGNED = "signed";
  static final String STATUS_CONSUMED = "consumed";

  private final CertificationTaskMapper taskMapper;
  private final CertificationAssessorTaskMapper assessorTaskMapper;
  private final CertificationPracticalCandidateResultMapper candidateResultMapper;
  private final CertificationTaskAssessorMapper taskAssessorMapper;
  private final CertificationInfoAssessorMapper assessorMapper;
  private final CertificationPracticalCompleteSignSessionMapper sessionMapper;
  private final CertificationPracticalCompleteSignatureMapper signatureMapper;
  private final FileApplicationService fileApplicationService;
  private final PracticalStartEventPublisher publisher;

  public PracticalCompleteSignatureApplicationService(
      CertificationTaskMapper taskMapper,
      CertificationAssessorTaskMapper assessorTaskMapper,
      CertificationPracticalCandidateResultMapper candidateResultMapper,
      CertificationTaskAssessorMapper taskAssessorMapper,
      CertificationInfoAssessorMapper assessorMapper,
      CertificationPracticalCompleteSignSessionMapper sessionMapper,
      CertificationPracticalCompleteSignatureMapper signatureMapper,
      FileApplicationService fileApplicationService,
      PracticalStartEventPublisher publisher
  ) {
    this.taskMapper = taskMapper;
    this.assessorTaskMapper = assessorTaskMapper;
    this.candidateResultMapper = candidateResultMapper;
    this.taskAssessorMapper = taskAssessorMapper;
    this.assessorMapper = assessorMapper;
    this.sessionMapper = sessionMapper;
    this.signatureMapper = signatureMapper;
    this.fileApplicationService = fileApplicationService;
    this.publisher = publisher;
  }

  @Transactional(readOnly = true)
  public PracticalScoringModels.CompleteSignView viewForTask(Long taskId) {
    requireTask(taskId);
    return viewForTask(taskId, resolveCurrentAssessorIdOrNull());
  }

  @Transactional(readOnly = true)
  public PracticalScoringModels.CompleteSignView viewForAssessor(Long taskId, Long assessorId) {
    return viewForTask(taskId, assessorId);
  }

  @Transactional
  public PracticalScoringModels.CompleteSignView requestSignatures(Long taskId) {
    PlatformUserPrincipal user = SecurityContext.currentUser();
    requireTask(taskId);
    BoundAssessors bound = requireSettledBoundAssessors(taskId);
    CertificationPracticalCompleteSignSessionEntity session = sessionMapper.lockByTask(user.getTenantId(), taskId);
    if (session == null) {
      session = new CertificationPracticalCompleteSignSessionEntity();
      session.setTaskId(taskId);
      session.setStatus(STATUS_PENDING);
      session.setRequestedAt(LocalDateTime.now());
      session.setRequestedBy(user.getUserId());
      session.setRequiredCount(bound.assessors().size());
      session.setSignedCount(0);
      session.setDeleted(0);
      sessionMapper.insert(session);
      for (BoundAssessor assessor : bound.assessors()) {
        CertificationPracticalCompleteSignatureEntity row = new CertificationPracticalCompleteSignatureEntity();
        row.setSessionId(session.getId());
        row.setTaskId(taskId);
        row.setGroupId(assessor.groupId());
        row.setAssessorId(assessor.assessorId());
        row.setAssessorName(assessor.assessorName());
        row.setStatus(STATUS_PENDING);
        row.setDeleted(0);
        signatureMapper.insert(row);
      }
    } else if (STATUS_CONSUMED.equals(session.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "评分任务已完成，不能再次发起签字");
    }
    PracticalScoringModels.CompleteSignView view = toView(session, signatures(session.getId()), null);
    publishAfterCommit(user.getTenantId(), taskId, view);
    return view;
  }

  @Transactional
  public PracticalScoringModels.CompleteSignView submitMine(
      Long taskId,
      PracticalScoringModels.SubmitCompleteSignReq req
  ) {
    PlatformUserPrincipal user = SecurityContext.currentUser();
    CertificationInfoAssessorEntity assessor = currentAssessor();
    requireTask(taskId);
    CertificationPracticalCompleteSignSessionEntity session = sessionMapper.lockByTask(user.getTenantId(), taskId);
    if (session == null || STATUS_NONE.equals(session.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "管理端尚未发起三名考评员签字确认");
    }
    if (STATUS_CONSUMED.equals(session.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "评分任务已完成，不能再提交签字");
    }
    CertificationPracticalCompleteSignatureEntity row = signatureMapper.lockByAssessor(
        user.getTenantId(), session.getId(), assessor.getId());
    if (row == null) {
      log.warn("考评员{}尝试签署未分配的完成任务会话{}", assessor.getId(), session.getId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "当前考评员不在本任务签字名单中");
    }
    if (STATUS_SIGNED.equals(row.getStatus())) {
      return toView(session, signatures(session.getId()), assessor.getId());
    }
    FileEntity file = fileApplicationService.getExistingFile(user.getTenantId(), req.signatureFileId());
    if (StringUtils.hasText(file.getSha256())
        && StringUtils.hasText(req.signatureHash())
        && !file.getSha256().equalsIgnoreCase(req.signatureHash())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "签名文件 hash 不一致");
    }
    row.setStatus(STATUS_SIGNED);
    row.setSignatureFileId(req.signatureFileId());
    row.setSignatureHash(req.signatureHash());
    row.setSignedAt(LocalDateTime.now());
    row.setDeviceInfo(req.deviceInfo());
    row.setAssessorName(StringUtils.hasText(row.getAssessorName()) ? row.getAssessorName() : assessor.getAssessorName());
    signatureMapper.updateById(row);

    List<CertificationPracticalCompleteSignatureEntity> rows = signatures(session.getId());
    long signed = rows.stream().filter(item -> STATUS_SIGNED.equals(item.getStatus())).count();
    session.setSignedCount((int) signed);
    if (signed >= nvl(session.getRequiredCount(), rows.size()) && rows.stream().allMatch(item -> STATUS_SIGNED.equals(item.getStatus()))) {
      session.setStatus(STATUS_SIGNED);
      session.setCompletedAt(LocalDateTime.now());
    }
    sessionMapper.updateById(session);
    PracticalScoringModels.CompleteSignView view = toView(session, rows, assessor.getId());
    publishAfterCommit(user.getTenantId(), taskId, view);
    return view;
  }

  @Transactional(readOnly = true)
  public void requireAllSigned(Long taskId) {
    CertificationPracticalCompleteSignSessionEntity session = sessionMapper.selectOne(
        new LambdaQueryWrapper<CertificationPracticalCompleteSignSessionEntity>()
            .eq(CertificationPracticalCompleteSignSessionEntity::getTaskId, taskId)
            .last("LIMIT 1"));
    if (session == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请先通知三名考评员签字确认");
    }
    if (STATUS_PENDING.equals(session.getStatus())) {
      int missing = Math.max(0, nvl(session.getRequiredCount(), 3) - nvl(session.getSignedCount(), 0));
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "仍有 " + missing + " 名考评员未签字确认，不能结束评分任务");
    }
    if (!STATUS_SIGNED.equals(session.getStatus()) && !STATUS_CONSUMED.equals(session.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "三名考评员尚未全部签字确认");
    }
  }

  @Transactional
  public void markConsumed(Long taskId) {
    PlatformUserPrincipal user = SecurityContext.currentUser();
    CertificationPracticalCompleteSignSessionEntity session = sessionMapper.lockByTask(user.getTenantId(), taskId);
    if (session == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请先通知三名考评员签字确认");
    }
    if (!STATUS_SIGNED.equals(session.getStatus()) && !STATUS_CONSUMED.equals(session.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "三名考评员尚未全部签字确认");
    }
    session.setStatus(STATUS_CONSUMED);
    sessionMapper.updateById(session);
  }

  @Transactional(readOnly = true)
  public List<AssessorSignImage> loadSignedImages(Long taskId) {
    CertificationPracticalCompleteSignSessionEntity session = sessionMapper.selectOne(
        new LambdaQueryWrapper<CertificationPracticalCompleteSignSessionEntity>()
            .eq(CertificationPracticalCompleteSignSessionEntity::getTaskId, taskId)
            .last("LIMIT 1"));
    if (session == null) {
      return List.of();
    }
    List<AssessorSignImage> images = new ArrayList<>();
    for (CertificationPracticalCompleteSignatureEntity row : signatures(session.getId())) {
      if (row == null || row.getAssessorId() == null || row.getSignatureFileId() == null) {
        continue;
      }
      if (!STATUS_SIGNED.equals(row.getStatus())) {
        continue;
      }
      try {
        byte[] bytes = fileApplicationService.readFileBytesById(row.getSignatureFileId());
        if (bytes != null && bytes.length > 0) {
          images.add(new AssessorSignImage(row.getAssessorId(), row.getAssessorName(), bytes));
        }
      } catch (RuntimeException ex) {
        log.warn("读取考评员{}完成任务签名失败: taskId={}, fileId={}, error={}",
            row.getAssessorId(), taskId, row.getSignatureFileId(), ex.getMessage());
      }
    }
    return images;
  }

  public record AssessorSignImage(Long assessorId, String assessorName, byte[] imageBytes) {
  }

  private PracticalScoringModels.CompleteSignView viewForTask(Long taskId, Long currentAssessorId) {
    CertificationPracticalCompleteSignSessionEntity session = sessionMapper.selectOne(
        new LambdaQueryWrapper<CertificationPracticalCompleteSignSessionEntity>()
            .eq(CertificationPracticalCompleteSignSessionEntity::getTaskId, taskId)
            .last("LIMIT 1"));
    if (session == null) {
      return emptyView(taskId);
    }
    return toView(session, signatures(session.getId()), currentAssessorId);
  }

  private List<CertificationPracticalCompleteSignatureEntity> signatures(Long sessionId) {
    List<CertificationPracticalCompleteSignatureEntity> rows = signatureMapper.selectList(
        new LambdaQueryWrapper<CertificationPracticalCompleteSignatureEntity>()
            .eq(CertificationPracticalCompleteSignatureEntity::getSessionId, sessionId)
            .orderByAsc(CertificationPracticalCompleteSignatureEntity::getId));
    return rows == null ? List.of() : rows;
  }

  private PracticalScoringModels.CompleteSignView toView(
      CertificationPracticalCompleteSignSessionEntity session,
      List<CertificationPracticalCompleteSignatureEntity> rows,
      Long currentAssessorId
  ) {
    List<PracticalScoringModels.CompleteSignAssessorView> assessors = (rows == null ? List.<CertificationPracticalCompleteSignatureEntity>of() : rows)
        .stream()
        .sorted(Comparator.comparing(CertificationPracticalCompleteSignatureEntity::getId,
            Comparator.nullsLast(Long::compareTo)))
        .map(row -> new PracticalScoringModels.CompleteSignAssessorView(
            row.getAssessorId(),
            row.getAssessorName(),
            STATUS_SIGNED.equals(row.getStatus()),
            row.getSignedAt()))
        .toList();
    boolean currentSigned = currentAssessorId != null && assessors.stream()
        .anyMatch(item -> currentAssessorId.equals(item.assessorId()) && item.signed());
    boolean currentPending = currentAssessorId != null
        && STATUS_PENDING.equals(session.getStatus())
        && assessors.stream().anyMatch(item -> currentAssessorId.equals(item.assessorId()) && !item.signed());
    return new PracticalScoringModels.CompleteSignView(
        session.getId(),
        session.getTaskId(),
        session.getStatus(),
        nvl(session.getSignedCount(), (int) assessors.stream().filter(PracticalScoringModels.CompleteSignAssessorView::signed).count()),
        nvl(session.getRequiredCount(), assessors.size()),
        currentSigned,
        currentPending,
        new ArrayList<>(assessors));
  }

  static PracticalScoringModels.CompleteSignView emptyView(Long taskId) {
    return new PracticalScoringModels.CompleteSignView(
        null, taskId, STATUS_NONE, 0, 3, false, false, new ArrayList<>());
  }

  private BoundAssessors requireSettledBoundAssessors(Long taskId) {
    List<CertificationAssessorTaskEntity> scoringTasks = assessorTaskMapper.selectList(
        new LambdaQueryWrapper<CertificationAssessorTaskEntity>()
            .eq(CertificationAssessorTaskEntity::getTaskId, taskId)
            .isNotNull(CertificationAssessorTaskEntity::getCandidateId)
            .isNotNull(CertificationAssessorTaskEntity::getPaperQuestionId));
    if (CollectionUtils.isEmpty(scoringTasks)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "当前认定任务尚未生成实操评分任务，不能发起签字");
    }
    Set<Long> candidateIds = scoringTasks.stream()
        .map(CertificationAssessorTaskEntity::getCandidateId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    List<CertificationPracticalCandidateResultEntity> candidateResults = candidateResultMapper.selectList(
        new LambdaQueryWrapper<CertificationPracticalCandidateResultEntity>()
            .eq(CertificationPracticalCandidateResultEntity::getTaskId, taskId));
    Map<Long, String> resultStatuses = (candidateResults == null
        ? List.<CertificationPracticalCandidateResultEntity>of() : candidateResults)
        .stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(
            CertificationPracticalCandidateResultEntity::getCandidateId,
            CertificationPracticalCandidateResultEntity::getStatus,
            (left, right) -> left));
    long unsettled = candidateIds.stream()
        .filter(candidateId -> !"completed".equals(resultStatuses.get(candidateId))
            && !"absent".equals(resultStatuses.get(candidateId)))
        .count();
    if (unsettled > 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "仍有 " + unsettled + " 名考生的实操评分未完成，不能发起签字确认");
    }
    Set<Long> groupIds = scoringTasks.stream()
        .map(CertificationAssessorTaskEntity::getGroupId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    if (groupIds.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "评分任务缺少考评组绑定，不能发起签字确认");
    }
    List<CertificationTaskAssessorEntity> bindings = taskAssessorMapper.selectList(
        new LambdaQueryWrapper<CertificationTaskAssessorEntity>()
            .eq(CertificationTaskAssessorEntity::getTaskId, taskId)
            .in(CertificationTaskAssessorEntity::getGroupId, groupIds));
    if (bindings == null || bindings.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "考评组尚未绑定考评员，不能发起签字确认");
    }
    Map<Long, BoundAssessor> unique = new LinkedHashMap<>();
    for (CertificationTaskAssessorEntity binding : bindings) {
      if (binding == null || binding.getAssessorId() == null) {
        continue;
      }
      unique.putIfAbsent(binding.getAssessorId(), new BoundAssessor(
          binding.getAssessorId(),
          binding.getGroupId(),
          assessorName(binding.getAssessorId())));
    }
    if (unique.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "考评组尚未绑定考评员，不能发起签字确认");
    }
    if (unique.size() != 3) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "实操完成任务需三名考评员签字确认");
    }
    return new BoundAssessors(new ArrayList<>(unique.values()));
  }

  private String assessorName(Long assessorId) {
    CertificationInfoAssessorEntity assessor = assessorMapper.selectById(assessorId);
    return assessor == null ? ("考评员" + assessorId) : assessor.getAssessorName();
  }

  private CertificationTaskEntity requireTask(Long taskId) {
    CertificationTaskEntity task = taskMapper.selectById(taskId);
    if (task == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "认定任务不存在");
    }
    return task;
  }

  private CertificationInfoAssessorEntity currentAssessor() {
    PlatformUserPrincipal user = SecurityContext.currentUser();
    CertificationInfoAssessorEntity assessor = assessorMapper.selectOne(
        new LambdaQueryWrapper<CertificationInfoAssessorEntity>()
            .eq(CertificationInfoAssessorEntity::getUserId, user.getUserId())
            .eq(CertificationInfoAssessorEntity::getAuditStatus, "approved")
            .last("LIMIT 1"));
    if (assessor == null) {
      log.warn("用户{}没有已入库考评员身份", user.getUserId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "当前账号未绑定有效考评员档案");
    }
    return assessor;
  }

  private Long resolveCurrentAssessorIdOrNull() {
    try {
      PlatformUserPrincipal user = SecurityContext.currentUser();
      if (user == null || user.getUserId() == null) {
        return null;
      }
      CertificationInfoAssessorEntity assessor = assessorMapper.selectOne(
          new LambdaQueryWrapper<CertificationInfoAssessorEntity>()
              .eq(CertificationInfoAssessorEntity::getUserId, user.getUserId())
              .eq(CertificationInfoAssessorEntity::getAuditStatus, "approved")
              .last("LIMIT 1"));
      return assessor == null ? null : assessor.getId();
    } catch (RuntimeException ex) {
      return null;
    }
  }

  private void publishAfterCommit(Long tenantId, Long taskId, PracticalScoringModels.CompleteSignView view) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          publisher.publishCompleteSign(tenantId, taskId, view);
        }
      });
      return;
    }
    publisher.publishCompleteSign(tenantId, taskId, view);
  }

  private static int nvl(Integer value, int fallback) {
    return value == null ? fallback : value;
  }

  private record BoundAssessor(Long assessorId, Long groupId, String assessorName) {
  }

  private record BoundAssessors(List<BoundAssessor> assessors) {
  }
}
