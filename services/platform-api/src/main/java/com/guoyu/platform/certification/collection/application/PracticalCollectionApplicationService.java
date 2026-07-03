package com.guoyu.platform.certification.collection.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.certification.collection.converter.PracticalCollectionConverter;
import com.guoyu.platform.certification.collection.domain.PracticalCollectionDomainService;
import com.guoyu.platform.certification.collection.dto.CollectionExceptionQueryRequest;
import com.guoyu.platform.certification.collection.dto.CollectionTaskQueryRequest;
import com.guoyu.platform.certification.collection.dto.CompleteCollectionRequest;
import com.guoyu.platform.certification.collection.dto.ExceptionReleaseRequest;
import com.guoyu.platform.certification.collection.dto.GenerateQrCodeRequest;
import com.guoyu.platform.certification.collection.dto.JoinQueueRequest;
import com.guoyu.platform.certification.collection.dto.MarkInfoErrorRequest;
import com.guoyu.platform.certification.collection.dto.MarkPendingRetryRequest;
import com.guoyu.platform.certification.collection.dto.MatchByTokenRequest;
import com.guoyu.platform.certification.collection.dto.RemoveQueueRequest;
import com.guoyu.platform.certification.collection.dto.SearchCandidateRequest;
import com.guoyu.platform.certification.collection.dto.StartNextRequest;
import com.guoyu.platform.certification.collection.dto.SubmitFingerprintRequest;
import com.guoyu.platform.certification.collection.dto.SubmitSignatureRequest;
import com.guoyu.platform.certification.collection.enums.CollectionExceptionStatusEnum;
import com.guoyu.platform.certification.collection.enums.CollectionExceptionTypeEnum;
import com.guoyu.platform.certification.collection.enums.CollectionMatchMethodEnum;
import com.guoyu.platform.certification.collection.enums.CollectionOverallStatusEnum;
import com.guoyu.platform.certification.collection.enums.CollectionQueueStatusEnum;
import com.guoyu.platform.certification.collection.enums.CollectionReleaseScopeEnum;
import com.guoyu.platform.certification.collection.enums.FingerprintCollectStatusEnum;
import com.guoyu.platform.certification.collection.enums.FingerprintStatusEnum;
import com.guoyu.platform.certification.collection.enums.QrIssueStatusEnum;
import com.guoyu.platform.certification.collection.enums.SignatureRecordStatusEnum;
import com.guoyu.platform.certification.collection.enums.SignatureStatusEnum;
import com.guoyu.platform.certification.collection.enums.SignerTypeEnum;
import com.guoyu.platform.certification.collection.vo.CandidateCollectionDetailVO;
import com.guoyu.platform.certification.collection.vo.CandidateCollectionItemVO;
import com.guoyu.platform.certification.collection.vo.CandidateMatchVO;
import com.guoyu.platform.certification.collection.vo.CollectionActionResultVO;
import com.guoyu.platform.certification.collection.vo.CollectionExceptionVO;
import com.guoyu.platform.certification.collection.vo.CollectionStatsVO;
import com.guoyu.platform.certification.collection.vo.CollectionTaskVO;
import com.guoyu.platform.certification.collection.vo.CollectionWorkbenchVO;
import com.guoyu.platform.certification.collection.vo.FingerprintTraceVO;
import com.guoyu.platform.certification.collection.vo.QueueItemVO;
import com.guoyu.platform.certification.collection.vo.SignatureRecordVO;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.certification.entity.CertificationCandidateEntity;
import com.guoyu.platform.certification.entity.CertificationTaskEntity;
import com.guoyu.platform.certification.entity.PracticalCollectionExceptionEntity;
import com.guoyu.platform.certification.entity.PracticalCollectionQueueEntity;
import com.guoyu.platform.certification.entity.PracticalCollectionStatusEntity;
import com.guoyu.platform.certification.entity.PracticalFingerprintTraceEntity;
import com.guoyu.platform.certification.entity.PracticalSignatureRecordEntity;
import com.guoyu.platform.certification.mapper.CertificationCandidateMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskMapper;
import com.guoyu.platform.certification.mapper.PracticalCollectionExceptionMapper;
import com.guoyu.platform.certification.mapper.PracticalCollectionQueueMapper;
import com.guoyu.platform.certification.mapper.PracticalCollectionStatusMapper;
import com.guoyu.platform.certification.mapper.PracticalFingerprintTraceMapper;
import com.guoyu.platform.certification.mapper.PracticalSignatureRecordMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.entity.FileEntity;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.MDC;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PracticalCollectionApplicationService {

  private static final Pattern LAST4_PATTERN = Pattern.compile("^\\d{4}$");
  private static final Pattern NAME_LAST4_PATTERN = Pattern.compile("^(.+?)[\\s,，:：-]*(\\d{4})$");
  private static final int TOKEN_BYTES = 32;
  private static final int JOIN_QUEUE_RETRY = 2;

  private final CertificationTaskMapper taskMapper;
  private final CertificationCandidateMapper candidateMapper;
  private final PracticalCollectionStatusMapper statusMapper;
  private final PracticalCollectionQueueMapper queueMapper;
  private final PracticalFingerprintTraceMapper fingerprintTraceMapper;
  private final PracticalSignatureRecordMapper signatureRecordMapper;
  private final PracticalCollectionExceptionMapper exceptionMapper;
  private final FileApplicationService fileApplicationService;
  private final PracticalCollectionDomainService domainService;
  private final PracticalCollectionConverter converter;
  private final SecureRandom secureRandom = new SecureRandom();

  public PracticalCollectionApplicationService(
      CertificationTaskMapper taskMapper,
      CertificationCandidateMapper candidateMapper,
      PracticalCollectionStatusMapper statusMapper,
      PracticalCollectionQueueMapper queueMapper,
      PracticalFingerprintTraceMapper fingerprintTraceMapper,
      PracticalSignatureRecordMapper signatureRecordMapper,
      PracticalCollectionExceptionMapper exceptionMapper,
      FileApplicationService fileApplicationService,
      PracticalCollectionDomainService domainService,
      PracticalCollectionConverter converter
  ) {
    this.taskMapper = taskMapper;
    this.candidateMapper = candidateMapper;
    this.statusMapper = statusMapper;
    this.queueMapper = queueMapper;
    this.fingerprintTraceMapper = fingerprintTraceMapper;
    this.signatureRecordMapper = signatureRecordMapper;
    this.exceptionMapper = exceptionMapper;
    this.fileApplicationService = fileApplicationService;
    this.domainService = domainService;
    this.converter = converter;
  }

  public PageResponse<CollectionTaskVO> pageTasks(CollectionTaskQueryRequest request) {
    Long tenantId = current().getTenantId();
    LambdaQueryWrapper<CertificationTaskEntity> wrapper = new LambdaQueryWrapper<CertificationTaskEntity>()
        .eq(CertificationTaskEntity::getTenantId, tenantId)
        .eq(CertificationTaskEntity::getDeleted, 0)
        .and(StringUtils.hasText(request.getKeyword()), nested -> nested
            .like(CertificationTaskEntity::getTaskName, request.getKeyword())
            .or()
            .like(CertificationTaskEntity::getTaskNo, request.getKeyword())
            .or()
            .like(CertificationTaskEntity::getCompanyName, request.getKeyword()))
        .eq(StringUtils.hasText(request.getStatus()), CertificationTaskEntity::getStatus, request.getStatus())
        .orderByDesc(CertificationTaskEntity::getExamDate)
        .orderByDesc(CertificationTaskEntity::getCreatedAt);
    applyCandidateTaskFilter(wrapper, request);
    Page<CertificationTaskEntity> page = taskMapper.selectPage(
        new Page<>(request.getPage(), request.getPageSize()),
        wrapper
    );
    List<CollectionTaskVO> items = page.getRecords().stream()
        .map(task -> converter.toTaskVO(task, statsForTask(tenantId, task.getId())))
        .toList();
    return new PageResponse<>(items, page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional
  public CollectionWorkbenchVO workbench(Long taskId) {
    Long tenantId = current().getTenantId();
    initializeCollectionStatus(taskId);
    CertificationTaskEntity task = requireTask(tenantId, taskId);
    CollectionStatsVO stats = statsForTask(tenantId, taskId);
    List<CertificationCandidateEntity> candidates = candidateMapper.selectList(
        new LambdaQueryWrapper<CertificationCandidateEntity>()
            .eq(CertificationCandidateEntity::getTenantId, tenantId)
            .eq(CertificationCandidateEntity::getTaskId, taskId)
            .eq(CertificationCandidateEntity::getDeleted, 0)
            .orderByAsc(CertificationCandidateEntity::getId)
            .last("LIMIT 500"));
    Map<Long, PracticalCollectionStatusEntity> statuses = statusesByCandidateIds(
        tenantId,
        taskId,
        candidates.stream().map(CertificationCandidateEntity::getId).toList()
    );
    List<CandidateCollectionItemVO> candidateItems = candidates.stream()
        .map(candidate -> converter.toCandidateItemVO(candidate, statuses.get(candidate.getId())))
        .toList();
    List<PracticalCollectionQueueEntity> queueRows = queueMapper.selectList(
        new LambdaQueryWrapper<PracticalCollectionQueueEntity>()
            .eq(PracticalCollectionQueueEntity::getTenantId, tenantId)
            .eq(PracticalCollectionQueueEntity::getTaskId, taskId)
            .in(PracticalCollectionQueueEntity::getQueueStatus,
                List.of(CollectionQueueStatusEnum.QUEUED.getCode(), CollectionQueueStatusEnum.COLLECTING.getCode()))
            .eq(PracticalCollectionQueueEntity::getDeleted, 0)
            .orderByAsc(PracticalCollectionQueueEntity::getQueueNo)
            .orderByAsc(PracticalCollectionQueueEntity::getJoinedAt));
    Map<Long, CertificationCandidateEntity> candidateMap = candidates.stream()
        .collect(Collectors.toMap(CertificationCandidateEntity::getId, Function.identity(), (left, right) -> left));
    List<QueueItemVO> queueItems = queueRows.stream()
        .map(queue -> converter.toQueueItemVO(
            queue,
            candidateMap.get(queue.getCandidateId()),
            statuses.get(queue.getCandidateId())
        ))
        .toList();
    CandidateCollectionDetailVO currentCandidate = queueRows.stream()
        .filter(queue -> CollectionQueueStatusEnum.COLLECTING.getCode().equals(queue.getQueueStatus()))
        .findFirst()
        .map(queue -> collectionDetail(taskId, queue.getCandidateId()))
        .orElse(null);
    return new CollectionWorkbenchVO(
        converter.toTaskVO(task, stats),
        stats,
        candidateItems,
        queueItems,
        currentCandidate
    );
  }

  public CandidateCollectionDetailVO collectionDetail(Long taskId, Long candidateId) {
    PlatformUserPrincipal user = current();
    CertificationCandidateEntity candidate = requireCandidate(user.getTenantId(), taskId, candidateId);
    PracticalCollectionStatusEntity status = ensureStatusForCandidate(user.getTenantId(), candidate);
    PracticalCollectionQueueEntity queue =
        queueMapper.selectActiveByCandidate(user.getTenantId(), taskId, candidateId);
    PracticalFingerprintTraceEntity fingerprintTrace = latestFingerprintTrace(user.getTenantId(), taskId, candidateId);
    PracticalSignatureRecordEntity signatureRecord = latestSignatureRecord(user.getTenantId(), taskId, candidateId);
    List<CollectionExceptionVO> exceptions = exceptionMapper.selectList(
            new LambdaQueryWrapper<PracticalCollectionExceptionEntity>()
                .eq(PracticalCollectionExceptionEntity::getTenantId, user.getTenantId())
                .eq(PracticalCollectionExceptionEntity::getTaskId, taskId)
                .eq(PracticalCollectionExceptionEntity::getCandidateId, candidateId)
                .eq(PracticalCollectionExceptionEntity::getDeleted, 0)
                .orderByDesc(PracticalCollectionExceptionEntity::getHandledAt)
                .orderByDesc(PracticalCollectionExceptionEntity::getCreatedAt)
                .last("LIMIT 20"))
        .stream()
        .map(entity -> converter.toExceptionVO(entity, candidate, status))
        .toList();
    QueueItemVO queueItem = queue == null ? null : converter.toQueueItemVO(queue, candidate, status);
    return converter.toCandidateDetailVO(
        candidate,
        status,
        null,
        queueItem,
        converter.toFingerprintTraceVO(fingerprintTrace),
        converter.toSignatureRecordVO(signatureRecord),
        exceptions
    );
  }

  public PageResponse<CollectionExceptionVO> pageExceptions(CollectionExceptionQueryRequest request) {
    Long tenantId = current().getTenantId();
    Set<Long> candidateIds = exceptionCandidateFilter(tenantId, request);
    if (StringUtils.hasText(request.getKeyword()) && candidateIds.isEmpty()) {
      return PageResponse.empty(request.getPage(), request.getPageSize());
    }
    LambdaQueryWrapper<PracticalCollectionExceptionEntity> wrapper =
        new LambdaQueryWrapper<PracticalCollectionExceptionEntity>()
            .eq(PracticalCollectionExceptionEntity::getTenantId, tenantId)
            .eq(request.getTaskId() != null, PracticalCollectionExceptionEntity::getTaskId, request.getTaskId())
            .eq(request.getCandidateId() != null, PracticalCollectionExceptionEntity::getCandidateId, request.getCandidateId())
            .eq(StringUtils.hasText(request.getExceptionType()),
                PracticalCollectionExceptionEntity::getExceptionType,
                request.getExceptionType())
            .eq(StringUtils.hasText(request.getExceptionStatus()),
                PracticalCollectionExceptionEntity::getExceptionStatus,
                request.getExceptionStatus())
            .in(!candidateIds.isEmpty(), PracticalCollectionExceptionEntity::getCandidateId, candidateIds)
            .eq(PracticalCollectionExceptionEntity::getDeleted, 0)
            .orderByDesc(PracticalCollectionExceptionEntity::getHandledAt)
            .orderByDesc(PracticalCollectionExceptionEntity::getCreatedAt);
    Page<PracticalCollectionExceptionEntity> page = exceptionMapper.selectPage(
        new Page<>(request.getPage(), request.getPageSize()),
        wrapper
    );
    List<CollectionExceptionVO> items = page.getRecords().stream()
        .map(entity -> converter.toExceptionVO(
            entity,
            candidateById(tenantId, entity.getTaskId(), entity.getCandidateId()),
            statusMapper.selectByTaskAndCandidate(tenantId, entity.getTaskId(), entity.getCandidateId())
        ))
        .toList();
    return new PageResponse<>(items, page.getCurrent(), page.getSize(), page.getTotal());
  }

  @Transactional
  public CollectionActionResultVO initializeCollectionStatusAction(Long taskId) {
    int created = initializeCollectionStatus(taskId);
    return actionResult(taskId, "Initialized collection statuses: " + created);
  }

  @Transactional
  public CollectionActionResultVO generateQrTokensAction(Long taskId) {
    int generated = generateQrTokens(taskId);
    return actionResult(taskId, "Generated collection QR tokens: " + generated);
  }

  public CollectionActionResultVO exportQrCodesPlaceholder(Long taskId) {
    requireTask(current().getTenantId(), taskId);
    return actionResult(taskId, "QR code export endpoint is reserved for the platform export implementation.");
  }

  @Transactional
  public int initializeCollectionStatus(Long taskId) {
    Long tenantId = current().getTenantId();
    requireTask(tenantId, taskId);
    List<CertificationCandidateEntity> candidates = candidateMapper.selectList(
        new LambdaQueryWrapper<CertificationCandidateEntity>()
            .eq(CertificationCandidateEntity::getTenantId, tenantId)
            .eq(CertificationCandidateEntity::getTaskId, taskId)
            .eq(CertificationCandidateEntity::getDeleted, 0));
    int created = 0;
    for (CertificationCandidateEntity candidate : candidates) {
      PracticalCollectionStatusEntity existing = statusMapper.selectByTaskAndCandidate(
          tenantId, taskId, candidate.getId());
      if (existing != null) {
        continue;
      }
      try {
        statusMapper.insert(newStatus(tenantId, candidate));
        created++;
      } catch (DuplicateKeyException ex) {
        // Another collector initialized this candidate concurrently. The unique key keeps it idempotent.
      }
    }
    return created;
  }

  @Transactional
  public int generateQrTokens(Long taskId) {
    Long tenantId = current().getTenantId();
    initializeCollectionStatus(taskId);
    List<PracticalCollectionStatusEntity> rows = statusMapper.selectList(
        new LambdaQueryWrapper<PracticalCollectionStatusEntity>()
            .eq(PracticalCollectionStatusEntity::getTenantId, tenantId)
            .eq(PracticalCollectionStatusEntity::getTaskId, taskId)
            .eq(PracticalCollectionStatusEntity::getDeleted, 0)
            .and(wrapper -> wrapper
                .isNull(PracticalCollectionStatusEntity::getCollectToken)
                .or()
                .eq(PracticalCollectionStatusEntity::getCollectToken, "")));
    int generated = 0;
    for (PracticalCollectionStatusEntity row : rows) {
      if (generateTokenFor(row)) {
        generated++;
      }
    }
    return generated;
  }

  @Transactional
  public int generateQrTokens(GenerateQrCodeRequest request) {
    if (request.candidateIds() == null || request.candidateIds().isEmpty()) {
      return generateQrTokens(request.taskId());
    }
    Long tenantId = current().getTenantId();
    requireTask(tenantId, request.taskId());
    int generated = 0;
    for (Long candidateId : request.candidateIds()) {
      CertificationCandidateEntity candidate = requireCandidate(tenantId, request.taskId(), candidateId);
      PracticalCollectionStatusEntity status = lockStatusForCandidate(tenantId, candidate);
      boolean regenerate = Boolean.TRUE.equals(request.regenerateExisting());
      if (!regenerate && StringUtils.hasText(status.getCollectToken())) {
        continue;
      }
      if (generateTokenFor(status)) {
        generated++;
      }
    }
    return generated;
  }

  @Transactional
  public CandidateMatchVO matchByToken(MatchByTokenRequest request) {
    PlatformUserPrincipal user = current();
    PracticalCollectionStatusEntity status = statusMapper.lockByTaskAndToken(
        user.getTenantId(), request.taskId(), request.collectToken());
    if (status == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "采集二维码不存在或不属于当前任务");
    }
    CertificationCandidateEntity candidate = requireCandidate(user.getTenantId(), request.taskId(), status.getCandidateId());
    LocalDateTime now = LocalDateTime.now();
    if (CollectionOverallStatusEnum.NOT_ARRIVED.getCode().equals(status.getOverallStatus())
        || CollectionOverallStatusEnum.ISSUED.getCode().equals(status.getOverallStatus())) {
      status.setOverallStatus(CollectionOverallStatusEnum.ARRIVED.getCode());
      status.setArrivedAt(status.getArrivedAt() == null ? now : status.getArrivedAt());
    }
    status.setMatchMethod(CollectionMatchMethodEnum.QR_CODE.getCode());
    status.setLastOperatorId(user.getUserId());
    statusMapper.updateById(status);
    PracticalCollectionQueueEntity activeQueue =
        queueMapper.selectActiveByCandidate(user.getTenantId(), request.taskId(), candidate.getId());
    return converter.toCandidateMatchVO(
        candidate,
        status,
        null,
        activeQueue != null,
        CollectionMatchMethodEnum.QR_CODE.getCode()
    );
  }

  @Transactional
  public List<CandidateMatchVO> searchCandidate(SearchCandidateRequest request) {
    Long tenantId = current().getTenantId();
    requireTask(tenantId, request.taskId());
    initializeCollectionStatus(request.taskId());
    String keyword = request.keyword() == null ? "" : request.keyword().trim();
    if (!StringUtils.hasText(keyword)) {
      return List.of();
    }
    List<CertificationCandidateEntity> candidates = searchCandidates(tenantId, request.taskId(), keyword);
    if (candidates.isEmpty()) {
      return List.of();
    }
    Map<Long, PracticalCollectionStatusEntity> statuses = statusesByCandidateIds(
        tenantId,
        request.taskId(),
        candidates.stream().map(CertificationCandidateEntity::getId).toList()
    );
    return candidates.stream()
        .map(candidate -> converter.toCandidateMatchVO(
            candidate,
            statuses.get(candidate.getId()),
            null,
            isActiveQueued(statuses.get(candidate.getId())),
            inferMatchMethod(keyword, candidate, statuses.get(candidate.getId()))
        ))
        .toList();
  }

  @Transactional
  public QueueItemVO joinQueue(JoinQueueRequest request) {
    PlatformUserPrincipal user = current();
    CollectionMatchMethodEnum matchMethod = CollectionMatchMethodEnum.fromCode(request.matchMethod());
    if (matchMethod == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "匹配方式不正确");
    }
    CertificationCandidateEntity candidate = requireCandidate(user.getTenantId(), request.taskId(), request.candidateId());
    PracticalCollectionStatusEntity status = lockStatusForCandidate(user.getTenantId(), candidate);
    if (CollectionOverallStatusEnum.COMPLETED.getCode().equals(status.getOverallStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "已完成采集的考生不能重新入队");
    }
    PracticalCollectionQueueEntity existing =
        queueMapper.selectActiveByCandidate(user.getTenantId(), request.taskId(), request.candidateId());
    if (existing != null) {
      return converter.toQueueItemVO(existing, candidate, status);
    }
    domainService.assertCanJoinQueue(status.getOverallStatus(), status.getQueueStatus());
    for (int attempt = 0; attempt < JOIN_QUEUE_RETRY; attempt++) {
      try {
        PracticalCollectionQueueEntity queue = new PracticalCollectionQueueEntity();
        queue.setTenantId(user.getTenantId());
        queue.setTaskId(request.taskId());
        queue.setCandidateId(request.candidateId());
        queue.setStatusId(status.getId());
        queue.setQueueNo(nextQueueNo(user.getTenantId(), request.taskId()));
        queue.setQueueStatus(CollectionQueueStatusEnum.QUEUED.getCode());
        queue.setMatchMethod(matchMethod.getCode());
        queue.setJoinedAt(LocalDateTime.now());
        queue.setOperatorId(user.getUserId());
        queue.setDeleted(0);
        queueMapper.insert(queue);

        status.setOverallStatus(CollectionOverallStatusEnum.QUEUED.getCode());
        status.setQueueStatus(CollectionQueueStatusEnum.QUEUED.getCode());
        status.setMatchMethod(matchMethod.getCode());
        status.setQueueNo(queue.getQueueNo());
        status.setLastOperatorId(user.getUserId());
        if (status.getArrivedAt() == null) {
          status.setArrivedAt(queue.getJoinedAt());
        }
        statusMapper.updateById(status);
        return converter.toQueueItemVO(queue, candidate, status);
      } catch (DuplicateKeyException ex) {
        PracticalCollectionQueueEntity active =
            queueMapper.selectActiveByCandidate(user.getTenantId(), request.taskId(), request.candidateId());
        if (active != null) {
          return converter.toQueueItemVO(active, candidate, status);
        }
        if (attempt + 1 >= JOIN_QUEUE_RETRY) {
          throw new BusinessException(ErrorCode.CONFLICT, "队列号生成冲突，请重试");
        }
      }
    }
    throw new BusinessException(ErrorCode.CONFLICT, "加入队列失败");
  }

  @Transactional
  public QueueItemVO removeQueue(RemoveQueueRequest request) {
    PlatformUserPrincipal user = current();
    PracticalCollectionQueueEntity queue = queueMapper.selectById(request.queueId());
    if (queue == null
        || !user.getTenantId().equals(queue.getTenantId())
        || !request.taskId().equals(queue.getTaskId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "队列项不存在");
    }
    CertificationCandidateEntity candidate = requireCandidate(user.getTenantId(), request.taskId(), queue.getCandidateId());
    PracticalCollectionStatusEntity status = lockStatusForCandidate(user.getTenantId(), candidate);
    if (CollectionQueueStatusEnum.REMOVED.getCode().equals(queue.getQueueStatus())) {
      return converter.toQueueItemVO(queue, candidate, status);
    }
    if (CollectionQueueStatusEnum.COLLECTING.getCode().equals(queue.getQueueStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "采集中考生不能直接移出队列");
    }
    if (!CollectionQueueStatusEnum.QUEUED.getCode().equals(queue.getQueueStatus())) {
      return converter.toQueueItemVO(queue, candidate, status);
    }
    queue.setQueueStatus(CollectionQueueStatusEnum.REMOVED.getCode());
    queue.setRemovedAt(LocalDateTime.now());
    queue.setRemoveReason(request.removeReason());
    queue.setOperatorId(user.getUserId());
    queueMapper.updateById(queue);

    status.setQueueStatus(CollectionQueueStatusEnum.REMOVED.getCode());
    if (!CollectionOverallStatusEnum.PENDING_RETRY.getCode().equals(status.getOverallStatus())) {
      status.setOverallStatus(CollectionOverallStatusEnum.ARRIVED.getCode());
    }
    status.setLastOperatorId(user.getUserId());
    statusMapper.updateById(status);
    return converter.toQueueItemVO(queue, candidate, status);
  }

  @Transactional
  public CandidateCollectionDetailVO startNext(StartNextRequest request) {
    PlatformUserPrincipal user = current();
    requireTask(user.getTenantId(), request.taskId());
    PracticalCollectionQueueEntity queue = queueMapper.lockNextQueued(user.getTenantId(), request.taskId());
    if (queue == null) {
      return null;
    }
    CertificationCandidateEntity candidate = requireCandidate(user.getTenantId(), request.taskId(), queue.getCandidateId());
    PracticalCollectionStatusEntity status = lockStatusForCandidate(user.getTenantId(), candidate);
    domainService.assertCanStartCollecting(status.getOverallStatus(), status.getQueueStatus());
    LocalDateTime now = LocalDateTime.now();
    queue.setQueueStatus(CollectionQueueStatusEnum.COLLECTING.getCode());
    queue.setStartedAt(now);
    queue.setOperatorId(user.getUserId());
    queueMapper.updateById(queue);

    status.setOverallStatus(CollectionOverallStatusEnum.COLLECTING.getCode());
    status.setQueueStatus(CollectionQueueStatusEnum.COLLECTING.getCode());
    status.setCollectingStartedAt(now);
    status.setLastOperatorId(user.getUserId());
    statusMapper.updateById(status);
    return converter.toCandidateDetailVO(candidate, status, null);
  }

  @Transactional
  public FingerprintTraceVO submitFingerprint(SubmitFingerprintRequest request) {
    PlatformUserPrincipal user = current();
    CertificationCandidateEntity candidate = requireCandidate(user.getTenantId(), request.taskId(), request.candidateId());
    PracticalCollectionStatusEntity status = lockStatusForCandidate(user.getTenantId(), candidate);
    domainService.assertCanSubmitFingerprint(status.getOverallStatus());
    FingerprintCollectStatusEnum collectStatus = FingerprintCollectStatusEnum.fromCode(request.collectStatus());
    if (collectStatus == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "指纹采集状态不正确");
    }
    if (collectStatus == FingerprintCollectStatusEnum.SUCCESS && request.fingerprintImageFileId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "指纹采集成功必须上传指纹图片并返回文件ID");
    }
    String requestId = resolveRequestId(request.requestId());
    if (StringUtils.hasText(requestId)) {
      PracticalFingerprintTraceEntity existing = fingerprintTraceMapper.selectByRequestId(
          user.getTenantId(), request.taskId(), request.candidateId(), requestId);
      if (existing != null) {
        return converter.toFingerprintTraceVO(existing);
      }
    }
    String fingerprintImageHash = normalizeSha256Hex(request.fingerprintImageHash());
    if (request.fingerprintImageFileId() != null) {
      FileEntity file = fileApplicationService.getExistingFile(user.getTenantId(), request.fingerprintImageFileId());
      if (StringUtils.hasText(file.getSha256())) {
        if (StringUtils.hasText(fingerprintImageHash)
            && !file.getSha256().equalsIgnoreCase(fingerprintImageHash)) {
          throw new BusinessException(ErrorCode.BAD_REQUEST, "指纹图片文件 hash 不一致");
        }
        if (!StringUtils.hasText(fingerprintImageHash)) {
          fingerprintImageHash = file.getSha256();
        }
      }
    }
    PracticalFingerprintTraceEntity trace = new PracticalFingerprintTraceEntity();
    trace.setTenantId(user.getTenantId());
    trace.setTaskId(request.taskId());
    trace.setCandidateId(request.candidateId());
    trace.setSessionId(request.sessionId());
    trace.setDeviceSn(request.deviceSn());
    trace.setDeviceModel(request.deviceModel());
    trace.setDeviceVendorId(request.deviceVendorId());
    trace.setDeviceProductId(request.deviceProductId());
    trace.setSdkDeviceType(request.sdkDeviceType());
    trace.setAdapterType(StringUtils.hasText(request.adapterType()) ? request.adapterType() : "MOCK");
    trace.setCollectStatus(collectStatus.getCode());
    trace.setQualityScore(request.qualityScore());
    trace.setDurationMs(request.durationMs());
    trace.setErrorCode(request.errorCode());
    trace.setExceptionReason(request.exceptionReason());
    trace.setCollectedBy(user.getUserId());
    trace.setCollectedAt(request.collectedAt());
    trace.setFingerprintImageFileId(request.fingerprintImageFileId());
    trace.setFingerprintImageHash(fingerprintImageHash);
    trace.setFingerprintImageFormat(StringUtils.hasText(request.fingerprintImageFormat())
        ? request.fingerprintImageFormat().trim()
        : (request.fingerprintImageFileId() == null ? null : "PNG"));
    trace.setFingerprintImageCapturedAt(request.fingerprintImageCapturedAt() == null
        ? request.collectedAt()
        : request.fingerprintImageCapturedAt());
    trace.setRequestId(requestId);
    trace.setDeleted(0);
    fingerprintTraceMapper.insert(trace);

    if (collectStatus == FingerprintCollectStatusEnum.SUCCESS) {
      status.setFingerprintStatus(FingerprintStatusEnum.DONE.getCode());
    } else {
      status.setFingerprintStatus(FingerprintStatusEnum.FAILED.getCode());
    }
    status.setLastOperatorId(user.getUserId());
    statusMapper.updateById(status);
    return converter.toFingerprintTraceVO(trace);
  }

  @Transactional
  public SignatureRecordVO submitSignature(SubmitSignatureRequest request) {
    PlatformUserPrincipal user = current();
    if (SignerTypeEnum.fromCode(request.signerType()) == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "签署人类型不正确");
    }
    CertificationCandidateEntity candidate = requireCandidate(user.getTenantId(), request.taskId(), request.candidateId());
    PracticalCollectionStatusEntity status = lockStatusForCandidate(user.getTenantId(), candidate);
    domainService.assertCanSubmitSignature(status.getOverallStatus());
    FileEntity file = fileApplicationService.getExistingFile(user.getTenantId(), request.signatureFileId());
    if (StringUtils.hasText(file.getSha256())
        && StringUtils.hasText(request.signatureHash())
        && !file.getSha256().equalsIgnoreCase(request.signatureHash())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "签名文件 hash 不一致");
    }
    PracticalSignatureRecordEntity record = new PracticalSignatureRecordEntity();
    record.setTenantId(user.getTenantId());
    record.setTaskId(request.taskId());
    record.setCandidateId(request.candidateId());
    record.setSessionId(request.sessionId());
    record.setSignatureFileId(request.signatureFileId());
    record.setSignatureHash(request.signatureHash());
    record.setSignerType(request.signerType());
    record.setSignerName(request.signerName());
    record.setSignedAt(request.signedAt());
    record.setDeviceInfo(request.deviceInfo());
    record.setRecordStatus(SignatureRecordStatusEnum.ACTIVE.getCode());
    record.setDeleted(0);
    signatureRecordMapper.insert(record);

    status.setSignatureStatus(SignatureStatusEnum.DONE.getCode());
    status.setLastOperatorId(user.getUserId());
    statusMapper.updateById(status);
    return converter.toSignatureRecordVO(record);
  }

  @Transactional
  public CollectionActionResultVO completeCollection(CompleteCollectionRequest request) {
    PlatformUserPrincipal user = current();
    CertificationCandidateEntity candidate = requireCandidate(user.getTenantId(), request.taskId(), request.candidateId());
    PracticalCollectionStatusEntity status = lockStatusForCandidate(user.getTenantId(), candidate);
    if (CollectionOverallStatusEnum.COMPLETED.getCode().equals(status.getOverallStatus())) {
      return converter.toActionResultVO(status, activeQueueItem(user.getTenantId(), candidate, status), "采集已完成");
    }
    domainService.assertCanComplete(status.getFingerprintStatus(), status.getSignatureStatus());
    LocalDateTime now = LocalDateTime.now();
    status.setOverallStatus(CollectionOverallStatusEnum.COMPLETED.getCode());
    status.setQueueStatus(CollectionQueueStatusEnum.COMPLETED.getCode());
    status.setCompletedAt(now);
    status.setLastOperatorId(user.getUserId());
    if (StringUtils.hasText(request.remark())) {
      status.setRemark(request.remark());
    }
    statusMapper.updateById(status);

    PracticalCollectionQueueEntity queue =
        queueMapper.selectActiveByCandidate(user.getTenantId(), request.taskId(), request.candidateId());
    if (queue != null) {
      queue.setQueueStatus(CollectionQueueStatusEnum.COMPLETED.getCode());
      queue.setCompletedAt(now);
      queue.setOperatorId(user.getUserId());
      queueMapper.updateById(queue);
    }
    return converter.toActionResultVO(
        status,
        queue == null ? null : converter.toQueueItemVO(queue, candidate, status),
        "采集完成"
    );
  }

  @Transactional
  public CollectionActionResultVO markPendingRetry(MarkPendingRetryRequest request) {
    PlatformUserPrincipal user = current();
    CertificationCandidateEntity candidate = requireCandidate(user.getTenantId(), request.taskId(), request.candidateId());
    PracticalCollectionStatusEntity status = lockStatusForCandidate(user.getTenantId(), candidate);
    domainService.assertNotCompleted(status.getOverallStatus(), "标记待补采");
    removeActiveQueue(user.getTenantId(), request.taskId(), request.candidateId(), user.getUserId(), request.reason());
    status.setOverallStatus(CollectionOverallStatusEnum.PENDING_RETRY.getCode());
    status.setQueueStatus(CollectionQueueStatusEnum.REMOVED.getCode());
    status.setExceptionStatus(CollectionExceptionStatusEnum.PENDING_RETRY.getCode());
    status.setLastOperatorId(user.getUserId());
    statusMapper.updateById(status);
    writeException(
        user,
        request.taskId(),
        request.candidateId(),
        CollectionExceptionTypeEnum.OTHER.getCode(),
        CollectionExceptionStatusEnum.PENDING_RETRY.getCode(),
        null,
        request.reason(),
        null,
        null
    );
    return converter.toActionResultVO(status, null, "已标记待补采");
  }

  @Transactional
  public CollectionActionResultVO markInfoError(MarkInfoErrorRequest request) {
    PlatformUserPrincipal user = current();
    CertificationCandidateEntity candidate = requireCandidate(user.getTenantId(), request.taskId(), request.candidateId());
    PracticalCollectionStatusEntity status = lockStatusForCandidate(user.getTenantId(), candidate);
    domainService.assertNotCompleted(status.getOverallStatus(), "标记信息异常");
    removeActiveQueue(user.getTenantId(), request.taskId(), request.candidateId(), user.getUserId(), request.reason());
    status.setOverallStatus(CollectionOverallStatusEnum.INFO_ERROR.getCode());
    status.setQueueStatus(CollectionQueueStatusEnum.REMOVED.getCode());
    status.setExceptionStatus(CollectionExceptionStatusEnum.INFO_ERROR.getCode());
    status.setLastOperatorId(user.getUserId());
    statusMapper.updateById(status);
    writeException(
        user,
        request.taskId(),
        request.candidateId(),
        CollectionExceptionTypeEnum.INFO_ERROR.getCode(),
        CollectionExceptionStatusEnum.INFO_ERROR.getCode(),
        CollectionReleaseScopeEnum.INFO.getCode(),
        request.reason(),
        null,
        null
    );
    return converter.toActionResultVO(status, null, "已标记信息异常");
  }

  @Transactional
  public CollectionActionResultVO exceptionRelease(ExceptionReleaseRequest request) {
    PlatformUserPrincipal user = current();
    CertificationCandidateEntity candidate = requireCandidate(user.getTenantId(), request.taskId(), request.candidateId());
    PracticalCollectionStatusEntity status = lockStatusForCandidate(user.getTenantId(), candidate);
    CollectionExceptionTypeEnum exceptionType = CollectionExceptionTypeEnum.fromCode(request.exceptionType());
    if (exceptionType == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "异常类型不正确");
    }
    domainService.assertCanExceptionRelease(status.getOverallStatus(), request.releaseScope(), request.reason());
    CollectionReleaseScopeEnum scope = CollectionReleaseScopeEnum.fromCode(request.releaseScope());
    if (scope == CollectionReleaseScopeEnum.FINGERPRINT || scope == CollectionReleaseScopeEnum.ALL) {
      status.setFingerprintStatus(FingerprintStatusEnum.EXCEPTION_RELEASED.getCode());
    }
    if (scope == CollectionReleaseScopeEnum.SIGNATURE || scope == CollectionReleaseScopeEnum.ALL) {
      status.setSignatureStatus(SignatureStatusEnum.EXCEPTION_RELEASED.getCode());
    }
    if (scope == CollectionReleaseScopeEnum.ALL
        || scope == CollectionReleaseScopeEnum.INFO
        || scope == CollectionReleaseScopeEnum.OTHER) {
      removeActiveQueue(user.getTenantId(), request.taskId(), request.candidateId(), user.getUserId(), request.reason());
      status.setOverallStatus(CollectionOverallStatusEnum.EXCEPTION_RELEASED.getCode());
      status.setQueueStatus(CollectionQueueStatusEnum.REMOVED.getCode());
    }
    status.setExceptionStatus(CollectionExceptionStatusEnum.RELEASED.getCode());
    status.setLastOperatorId(user.getUserId());
    statusMapper.updateById(status);
    writeException(
        user,
        request.taskId(),
        request.candidateId(),
        exceptionType.getCode(),
        CollectionExceptionStatusEnum.RELEASED.getCode(),
        scope.getCode(),
        request.reason(),
        request.deviceSn(),
        request.deviceStatus()
    );
    return converter.toActionResultVO(status, activeQueueItem(user.getTenantId(), candidate, status), "已记录异常放行");
  }

  private void applyCandidateTaskFilter(
      LambdaQueryWrapper<CertificationTaskEntity> wrapper,
      CollectionTaskQueryRequest request
  ) {
    if (StringUtils.hasText(request.getWorkTypeName())) {
      wrapper.exists(
          """
          SELECT 1
          FROM certification_candidate cc
          WHERE cc.tenant_id = certification_task.tenant_id
            AND cc.task_id = certification_task.id
            AND cc.deleted = 0
            AND cc.work_type_name LIKE CONCAT('%', {0}, '%')
          """,
          request.getWorkTypeName()
      );
    }
    if (StringUtils.hasText(request.getLevelCode())) {
      wrapper.exists(
          """
          SELECT 1
          FROM certification_candidate cc
          WHERE cc.tenant_id = certification_task.tenant_id
            AND cc.task_id = certification_task.id
            AND cc.deleted = 0
            AND cc.level_code = {0}
          """,
          request.getLevelCode()
      );
    }
  }

  private CollectionStatsVO statsForTask(Long tenantId, Long taskId) {
    int candidateTotal = toInt(candidateMapper.selectCount(
        new LambdaQueryWrapper<CertificationCandidateEntity>()
            .eq(CertificationCandidateEntity::getTenantId, tenantId)
            .eq(CertificationCandidateEntity::getTaskId, taskId)
            .eq(CertificationCandidateEntity::getDeleted, 0)));
    List<PracticalCollectionStatusEntity> statuses = statusMapper.selectList(
        new LambdaQueryWrapper<PracticalCollectionStatusEntity>()
            .eq(PracticalCollectionStatusEntity::getTenantId, tenantId)
            .eq(PracticalCollectionStatusEntity::getTaskId, taskId)
            .eq(PracticalCollectionStatusEntity::getDeleted, 0));
    int qrIssued = countStatuses(statuses, PracticalCollectionStatusEntity::getQrIssueStatus, QrIssueStatusEnum.ISSUED.getCode());
    int arrived = (int) statuses.stream().filter(status -> status.getArrivedAt() != null).count();
    int queued = countStatuses(statuses, PracticalCollectionStatusEntity::getQueueStatus, CollectionQueueStatusEnum.QUEUED.getCode());
    int collecting = countStatuses(statuses, PracticalCollectionStatusEntity::getOverallStatus, CollectionOverallStatusEnum.COLLECTING.getCode());
    int completed = countStatuses(statuses, PracticalCollectionStatusEntity::getOverallStatus, CollectionOverallStatusEnum.COMPLETED.getCode());
    int pendingRetry = countStatuses(statuses, PracticalCollectionStatusEntity::getOverallStatus, CollectionOverallStatusEnum.PENDING_RETRY.getCode());
    int infoError = countStatuses(statuses, PracticalCollectionStatusEntity::getOverallStatus, CollectionOverallStatusEnum.INFO_ERROR.getCode());
    int exceptionReleased = countStatuses(
        statuses,
        PracticalCollectionStatusEntity::getOverallStatus,
        CollectionOverallStatusEnum.EXCEPTION_RELEASED.getCode()
    );
    return new CollectionStatsVO(
        candidateTotal,
        qrIssued,
        arrived,
        queued,
        collecting,
        completed,
        pendingRetry,
        infoError,
        exceptionReleased,
        infoError + exceptionReleased
    );
  }

  private int countStatuses(
      List<PracticalCollectionStatusEntity> statuses,
      Function<PracticalCollectionStatusEntity, String> getter,
      String code
  ) {
    return (int) statuses.stream().filter(status -> Objects.equals(getter.apply(status), code)).count();
  }

  private PracticalFingerprintTraceEntity latestFingerprintTrace(Long tenantId, Long taskId, Long candidateId) {
    return fingerprintTraceMapper.selectOne(
        new LambdaQueryWrapper<PracticalFingerprintTraceEntity>()
            .eq(PracticalFingerprintTraceEntity::getTenantId, tenantId)
            .eq(PracticalFingerprintTraceEntity::getTaskId, taskId)
            .eq(PracticalFingerprintTraceEntity::getCandidateId, candidateId)
            .eq(PracticalFingerprintTraceEntity::getDeleted, 0)
            .orderByDesc(PracticalFingerprintTraceEntity::getCollectedAt)
            .orderByDesc(PracticalFingerprintTraceEntity::getCreatedAt)
            .last("LIMIT 1"));
  }

  private PracticalSignatureRecordEntity latestSignatureRecord(Long tenantId, Long taskId, Long candidateId) {
    return signatureRecordMapper.selectOne(
        new LambdaQueryWrapper<PracticalSignatureRecordEntity>()
            .eq(PracticalSignatureRecordEntity::getTenantId, tenantId)
            .eq(PracticalSignatureRecordEntity::getTaskId, taskId)
            .eq(PracticalSignatureRecordEntity::getCandidateId, candidateId)
            .eq(PracticalSignatureRecordEntity::getRecordStatus, SignatureRecordStatusEnum.ACTIVE.getCode())
            .eq(PracticalSignatureRecordEntity::getDeleted, 0)
            .orderByDesc(PracticalSignatureRecordEntity::getSignedAt)
            .orderByDesc(PracticalSignatureRecordEntity::getCreatedAt)
            .last("LIMIT 1"));
  }

  private Set<Long> exceptionCandidateFilter(Long tenantId, CollectionExceptionQueryRequest request) {
    if (!StringUtils.hasText(request.getKeyword())) {
      return Set.of();
    }
    String keyword = request.getKeyword().trim();
    Set<Long> candidateIds = new HashSet<>();
    candidateIds.addAll(candidateMapper.selectList(
            new LambdaQueryWrapper<CertificationCandidateEntity>()
                .eq(CertificationCandidateEntity::getTenantId, tenantId)
                .eq(request.getTaskId() != null, CertificationCandidateEntity::getTaskId, request.getTaskId())
                .eq(CertificationCandidateEntity::getDeleted, 0)
                .and(wrapper -> wrapper
                    .like(CertificationCandidateEntity::getCandidateName, keyword)
                    .or()
                    .eq(CertificationCandidateEntity::getAdmissionTicketNo, keyword)
                    .or()
                    .eq(CertificationCandidateEntity::getCandidateNo, keyword))
                .last("LIMIT 200"))
        .stream()
        .map(CertificationCandidateEntity::getId)
        .toList());
    if (LAST4_PATTERN.matcher(keyword).matches()) {
      candidateIds.addAll(statusMapper.selectList(
              new LambdaQueryWrapper<PracticalCollectionStatusEntity>()
                  .eq(PracticalCollectionStatusEntity::getTenantId, tenantId)
                  .eq(request.getTaskId() != null, PracticalCollectionStatusEntity::getTaskId, request.getTaskId())
                  .eq(PracticalCollectionStatusEntity::getIdCardLast4, keyword)
                  .eq(PracticalCollectionStatusEntity::getDeleted, 0)
                  .last("LIMIT 200"))
          .stream()
          .map(PracticalCollectionStatusEntity::getCandidateId)
          .toList());
    }
    return candidateIds;
  }

  private CertificationCandidateEntity candidateById(Long tenantId, Long taskId, Long candidateId) {
    return candidateMapper.selectOne(
        new LambdaQueryWrapper<CertificationCandidateEntity>()
            .eq(CertificationCandidateEntity::getTenantId, tenantId)
            .eq(CertificationCandidateEntity::getTaskId, taskId)
            .eq(CertificationCandidateEntity::getId, candidateId)
            .eq(CertificationCandidateEntity::getDeleted, 0)
            .last("LIMIT 1"));
  }

  private CollectionActionResultVO actionResult(Long taskId, String message) {
    return new CollectionActionResultVO(
        taskId,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        message
    );
  }

  private int toInt(Long value) {
    if (value == null) {
      return 0;
    }
    return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : value.intValue();
  }

  private List<CertificationCandidateEntity> searchCandidates(Long tenantId, Long taskId, String keyword) {
    if (LAST4_PATTERN.matcher(keyword).matches()) {
      return candidatesByLast4(tenantId, taskId, keyword);
    }
    List<CertificationCandidateEntity> exactIdentifierMatches =
        candidatesByAdmissionOrCandidateNo(tenantId, taskId, keyword);
    if (!exactIdentifierMatches.isEmpty()) {
      return exactIdentifierMatches;
    }
    Matcher nameLast4 = NAME_LAST4_PATTERN.matcher(keyword);
    if (nameLast4.matches()) {
      String name = nameLast4.group(1).trim();
      String last4 = nameLast4.group(2);
      Set<Long> idsByLast4 = statusesByLast4(tenantId, taskId, last4).stream()
          .map(PracticalCollectionStatusEntity::getCandidateId)
          .collect(Collectors.toCollection(LinkedHashSet::new));
      if (idsByLast4.isEmpty()) {
        return List.of();
      }
      return candidateMapper.selectList(new LambdaQueryWrapper<CertificationCandidateEntity>()
          .eq(CertificationCandidateEntity::getTenantId, tenantId)
          .eq(CertificationCandidateEntity::getTaskId, taskId)
          .in(CertificationCandidateEntity::getId, idsByLast4)
          .like(StringUtils.hasText(name), CertificationCandidateEntity::getCandidateName, name)
          .eq(CertificationCandidateEntity::getDeleted, 0)
          .last("LIMIT 30"));
    }
    return candidateMapper.selectList(new LambdaQueryWrapper<CertificationCandidateEntity>()
        .eq(CertificationCandidateEntity::getTenantId, tenantId)
        .eq(CertificationCandidateEntity::getTaskId, taskId)
        .eq(CertificationCandidateEntity::getDeleted, 0)
        .and(wrapper -> wrapper
            .like(CertificationCandidateEntity::getCandidateName, keyword)
            .or()
            .eq(CertificationCandidateEntity::getAdmissionTicketNo, keyword)
            .or()
            .eq(CertificationCandidateEntity::getCandidateNo, keyword))
        .last("LIMIT 30"));
  }

  private List<CertificationCandidateEntity> candidatesByAdmissionOrCandidateNo(
      Long tenantId,
      Long taskId,
      String keyword
  ) {
    return candidateMapper.selectList(new LambdaQueryWrapper<CertificationCandidateEntity>()
        .eq(CertificationCandidateEntity::getTenantId, tenantId)
        .eq(CertificationCandidateEntity::getTaskId, taskId)
        .eq(CertificationCandidateEntity::getDeleted, 0)
        .and(wrapper -> wrapper
            .eq(CertificationCandidateEntity::getAdmissionTicketNo, keyword)
            .or()
            .eq(CertificationCandidateEntity::getCandidateNo, keyword))
        .last("LIMIT 30"));
  }

  private List<CertificationCandidateEntity> candidatesByLast4(Long tenantId, Long taskId, String last4) {
    List<Long> candidateIds = statusesByLast4(tenantId, taskId, last4).stream()
        .map(PracticalCollectionStatusEntity::getCandidateId)
        .toList();
    if (candidateIds.isEmpty()) {
      return List.of();
    }
    return candidateMapper.selectList(new LambdaQueryWrapper<CertificationCandidateEntity>()
        .eq(CertificationCandidateEntity::getTenantId, tenantId)
        .eq(CertificationCandidateEntity::getTaskId, taskId)
        .in(CertificationCandidateEntity::getId, candidateIds)
        .eq(CertificationCandidateEntity::getDeleted, 0)
        .last("LIMIT 30"));
  }

  private List<PracticalCollectionStatusEntity> statusesByLast4(Long tenantId, Long taskId, String last4) {
    return statusMapper.selectList(new LambdaQueryWrapper<PracticalCollectionStatusEntity>()
        .eq(PracticalCollectionStatusEntity::getTenantId, tenantId)
        .eq(PracticalCollectionStatusEntity::getTaskId, taskId)
        .eq(PracticalCollectionStatusEntity::getIdCardLast4, last4)
        .eq(PracticalCollectionStatusEntity::getDeleted, 0)
        .last("LIMIT 50"));
  }

  private Map<Long, PracticalCollectionStatusEntity> statusesByCandidateIds(
      Long tenantId,
      Long taskId,
      List<Long> candidateIds
  ) {
    if (candidateIds == null || candidateIds.isEmpty()) {
      return Map.of();
    }
    return statusMapper.selectList(new LambdaQueryWrapper<PracticalCollectionStatusEntity>()
            .eq(PracticalCollectionStatusEntity::getTenantId, tenantId)
            .eq(PracticalCollectionStatusEntity::getTaskId, taskId)
            .in(PracticalCollectionStatusEntity::getCandidateId, candidateIds)
            .eq(PracticalCollectionStatusEntity::getDeleted, 0))
        .stream()
        .collect(Collectors.toMap(
            PracticalCollectionStatusEntity::getCandidateId,
            Function.identity(),
            (left, right) -> left
        ));
  }

  private PracticalCollectionStatusEntity lockStatusForCandidate(
      Long tenantId,
      CertificationCandidateEntity candidate
  ) {
    PracticalCollectionStatusEntity status =
        statusMapper.lockByTaskAndCandidate(tenantId, candidate.getTaskId(), candidate.getId());
    if (status != null) {
      return status;
    }
    ensureStatusForCandidate(tenantId, candidate);
    status = statusMapper.lockByTaskAndCandidate(tenantId, candidate.getTaskId(), candidate.getId());
    if (status == null) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "采集状态初始化失败");
    }
    return status;
  }

  private PracticalCollectionStatusEntity ensureStatusForCandidate(
      Long tenantId,
      CertificationCandidateEntity candidate
  ) {
    PracticalCollectionStatusEntity existing =
        statusMapper.selectByTaskAndCandidate(tenantId, candidate.getTaskId(), candidate.getId());
    if (existing != null) {
      return existing;
    }
    try {
      PracticalCollectionStatusEntity status = newStatus(tenantId, candidate);
      statusMapper.insert(status);
      return status;
    } catch (DuplicateKeyException ex) {
      return statusMapper.selectByTaskAndCandidate(tenantId, candidate.getTaskId(), candidate.getId());
    }
  }

  private PracticalCollectionStatusEntity newStatus(Long tenantId, CertificationCandidateEntity candidate) {
    PracticalCollectionStatusEntity status = new PracticalCollectionStatusEntity();
    status.setTenantId(tenantId);
    status.setTaskId(candidate.getTaskId());
    status.setCandidateId(candidate.getId());
    status.setQrIssueStatus(QrIssueStatusEnum.NOT_ISSUED.getCode());
    status.setOverallStatus(CollectionOverallStatusEnum.NOT_ARRIVED.getCode());
    status.setQueueStatus(CollectionQueueStatusEnum.NOT_QUEUED.getCode());
    status.setFingerprintStatus(FingerprintStatusEnum.NOT_STARTED.getCode());
    status.setSignatureStatus(SignatureStatusEnum.NOT_STARTED.getCode());
    status.setExceptionStatus(CollectionExceptionStatusEnum.NONE.getCode());
    status.setIdCardLast4(extractLast4(candidate.getIdCardNo()));
    status.setDeleted(0);
    return status;
  }

  private CertificationTaskEntity requireTask(Long tenantId, Long taskId) {
    CertificationTaskEntity task = taskMapper.selectOne(new LambdaQueryWrapper<CertificationTaskEntity>()
        .eq(CertificationTaskEntity::getTenantId, tenantId)
        .eq(CertificationTaskEntity::getId, taskId)
        .eq(CertificationTaskEntity::getDeleted, 0)
        .last("LIMIT 1"));
    if (task == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "认定任务不存在");
    }
    return task;
  }

  private CertificationCandidateEntity requireCandidate(Long tenantId, Long taskId, Long candidateId) {
    CertificationCandidateEntity candidate = candidateMapper.selectOne(
        new LambdaQueryWrapper<CertificationCandidateEntity>()
            .eq(CertificationCandidateEntity::getTenantId, tenantId)
            .eq(CertificationCandidateEntity::getTaskId, taskId)
            .eq(CertificationCandidateEntity::getId, candidateId)
            .eq(CertificationCandidateEntity::getDeleted, 0)
            .last("LIMIT 1"));
    if (candidate == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考生不存在或不属于当前任务");
    }
    return candidate;
  }

  private boolean generateTokenFor(PracticalCollectionStatusEntity status) {
    for (int i = 0; i < 3; i++) {
      status.setCollectToken(nextToken());
      status.setQrIssueStatus(QrIssueStatusEnum.ISSUED.getCode());
      status.setTokenGeneratedAt(LocalDateTime.now());
      try {
        statusMapper.updateById(status);
        return true;
      } catch (DuplicateKeyException ex) {
        status.setCollectToken(null);
      }
    }
    throw new BusinessException(ErrorCode.CONFLICT, "二维码 token 生成冲突，请重试");
  }

  private String nextToken() {
    byte[] bytes = new byte[TOKEN_BYTES];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private Integer nextQueueNo(Long tenantId, Long taskId) {
    Integer currentMax = queueMapper.selectMaxQueueNo(tenantId, taskId);
    return (currentMax == null ? 0 : currentMax) + 1;
  }

  private boolean isActiveQueued(PracticalCollectionStatusEntity status) {
    if (status == null) {
      return false;
    }
    return CollectionQueueStatusEnum.QUEUED.getCode().equals(status.getQueueStatus())
        || CollectionQueueStatusEnum.COLLECTING.getCode().equals(status.getQueueStatus());
  }

  private String inferMatchMethod(
      String keyword,
      CertificationCandidateEntity candidate,
      PracticalCollectionStatusEntity status
  ) {
    if (LAST4_PATTERN.matcher(keyword).matches()
        || (status != null && Objects.equals(keyword, status.getIdCardLast4()))) {
      return CollectionMatchMethodEnum.ID_CARD_LAST4.getCode();
    }
    if (Objects.equals(keyword, candidate.getAdmissionTicketNo())) {
      return CollectionMatchMethodEnum.ADMISSION_NO.getCode();
    }
    if (Objects.equals(keyword, candidate.getCandidateNo())) {
      return CollectionMatchMethodEnum.MANUAL.getCode();
    }
    return CollectionMatchMethodEnum.NAME.getCode();
  }

  private QueueItemVO activeQueueItem(
      Long tenantId,
      CertificationCandidateEntity candidate,
      PracticalCollectionStatusEntity status
  ) {
    PracticalCollectionQueueEntity queue =
        queueMapper.selectActiveByCandidate(tenantId, candidate.getTaskId(), candidate.getId());
    return queue == null ? null : converter.toQueueItemVO(queue, candidate, status);
  }

  private void removeActiveQueue(
      Long tenantId,
      Long taskId,
      Long candidateId,
      Long operatorId,
      String reason
  ) {
    queueMapper.removeActiveByCandidate(tenantId, taskId, candidateId, operatorId, reason);
  }

  private void writeException(
      PlatformUserPrincipal user,
      Long taskId,
      Long candidateId,
      String exceptionType,
      String exceptionStatus,
      String releaseScope,
      String reason,
      String deviceSn,
      String deviceStatus
  ) {
    PracticalCollectionExceptionEntity entity = new PracticalCollectionExceptionEntity();
    entity.setTenantId(user.getTenantId());
    entity.setTaskId(taskId);
    entity.setCandidateId(candidateId);
    entity.setExceptionType(exceptionType);
    entity.setExceptionStatus(exceptionStatus);
    entity.setReleaseScope(releaseScope);
    entity.setReason(reason);
    entity.setDeviceSn(deviceSn);
    entity.setDeviceStatus(deviceStatus);
    entity.setHandledBy(user.getUserId());
    entity.setHandledAt(LocalDateTime.now());
    entity.setRequestId(resolveRequestId(null));
    entity.setDeleted(0);
    exceptionMapper.insert(entity);
  }

  private String extractLast4(String idCardNo) {
    if (!StringUtils.hasText(idCardNo) || idCardNo.length() < 4) {
      return null;
    }
    return idCardNo.substring(idCardNo.length() - 4);
  }

  private String resolveRequestId(String requestId) {
    if (StringUtils.hasText(requestId)) {
      return requestId;
    }
    return MDC.get("requestId");
  }

  private String normalizeSha256Hex(String hash) {
    if (!StringUtils.hasText(hash)) {
      return null;
    }
    String value = hash.trim();
    if (value.regionMatches(true, 0, "sha256:", 0, "sha256:".length())) {
      value = value.substring("sha256:".length()).trim();
    }
    return value;
  }

  private PlatformUserPrincipal current() {
    return SecurityContext.currentUser();
  }
}
