package com.guoyu.platform.certification.collection.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.certification.collection.converter.PracticalCollectionConverter;
import com.guoyu.platform.certification.collection.domain.CollectionEligibilityResult;
import com.guoyu.platform.certification.collection.domain.PracticalCollectionDomainService;
import com.guoyu.platform.certification.collection.domain.PracticalCollectionEligibilityService;
import com.guoyu.platform.certification.collection.dto.CancelCurrentCollectionRequest;
import com.guoyu.platform.certification.collection.dto.CollectionExceptionQueryRequest;
import com.guoyu.platform.certification.collection.dto.BatchJoinQueueRequest;
import com.guoyu.platform.certification.collection.dto.CollectionFormExportRequest;
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
import com.guoyu.platform.certification.collection.vo.BatchJoinQueueResultVO;
import com.guoyu.platform.certification.collection.vo.CollectionFormExportVO;
import com.guoyu.platform.certification.collection.vo.CollectionStatsVO;
import com.guoyu.platform.certification.collection.vo.CollectionTaskVO;
import com.guoyu.platform.certification.collection.vo.CollectionWorkbenchVO;
import com.guoyu.platform.certification.collection.vo.FingerprintTraceVO;
import com.guoyu.platform.certification.collection.vo.QrCodeExportVO;
import com.guoyu.platform.certification.collection.vo.QueueItemVO;
import com.guoyu.platform.certification.collection.vo.ScanMatchResultVO;
import com.guoyu.platform.certification.collection.vo.SignatureRecordVO;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.certification.entity.CertificationCandidateEntity;
import com.guoyu.platform.certification.entity.CertificationCandidateExamSessionEntity;
import com.guoyu.platform.certification.entity.CertificationExamSessionEntity;
import com.guoyu.platform.certification.entity.CertificationTaskEntity;
import com.guoyu.platform.certification.entity.CertificationTaskExamSiteEntity;
import com.guoyu.platform.certification.entity.CertificationTaskScopeEntity;
import com.guoyu.platform.certification.entity.PracticalCollectionExceptionEntity;
import com.guoyu.platform.certification.entity.PracticalCollectionQueueEntity;
import com.guoyu.platform.certification.entity.PracticalCollectionStatusEntity;
import com.guoyu.platform.certification.entity.PracticalFingerprintTraceEntity;
import com.guoyu.platform.certification.entity.PracticalSignatureRecordEntity;
import com.guoyu.platform.certification.mapper.CertificationCandidateMapper;
import com.guoyu.platform.certification.mapper.CertificationCandidateExamSessionMapper;
import com.guoyu.platform.certification.mapper.CertificationExamSessionMapper;
import com.guoyu.platform.certification.mapper.CertificationCandidateDocumentMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskExamSiteMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskScopeMapper;
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
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.PictureData;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.MDC;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class PracticalCollectionApplicationService {

  private static final Pattern LAST4_PATTERN = Pattern.compile("^\\d{4}$");
  private static final Pattern NAME_LAST4_PATTERN = Pattern.compile("^(.+?)[\\s,，:：-]*(\\d{4})$");
  private static final int TOKEN_BYTES = 32;
  private static final int JOIN_QUEUE_RETRY = 2;
  private static final int QR_EXPORT_IMAGE_SIZE = 160;
  private static final Pattern TOKEN_QUERY_PATTERN =
      Pattern.compile("(?:collectionToken|collectToken|token)=([^&\\s]+)", Pattern.CASE_INSENSITIVE);
  private static final DateTimeFormatter FILE_TS_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
  private static final DateTimeFormatter SESSION_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DateTimeFormatter SESSION_TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm");
  private static final int COLLECTION_TEMPLATE_COLUMN_COUNT = 15;
  private static final int COLLECTION_TEMPLATE_ROW_COUNT = 30;
  private static final int COLLECTION_FIRST_BLOCK_ROW = 2;
  private static final int COLLECTION_BLOCK_ROW_STEP = 7;
  private static final int COLLECTION_BLOCK_COUNT = 4;
  private static final int COLLECTION_CANDIDATES_PER_ROW = 5;
  private static final int COLLECTION_CANDIDATES_PER_PAGE =
      COLLECTION_BLOCK_COUNT * COLLECTION_CANDIDATES_PER_ROW;
  private static final int[] COLLECTION_BLOCK_START_COLUMNS = {0, 3, 6, 9, 12};
  private static final byte[] COLLECTION_HEADER_FILL = new byte[] {(byte) 0xF9, (byte) 0xFA, (byte) 0xFB};
  private static final byte[] COLLECTION_LABEL_FILL = new byte[] {(byte) 0xE5, (byte) 0xE7, (byte) 0xEB};
  private static final String LEGACY_COLLECTION_SESSION_NAME = "历史实操考试";
  /**
   * Excel 单元格屏幕像素约为 96DPI；按 4x 生成嵌入图，打印/缩放后笔画不易发虚断点。
   */
  private static final int COLLECTION_IMAGE_EXPORT_SCALE = 4;
  private static final int COLLECTION_IMAGE_CONTENT_PADDING_PX = 8;
  private static final int COLLECTION_IMAGE_WHITE_THRESHOLD = 245;

  private final CertificationTaskMapper taskMapper;
  private final CertificationCandidateMapper candidateMapper;
  private final CertificationExamSessionMapper examSessionMapper;
  private final CertificationCandidateExamSessionMapper candidateExamSessionMapper;
  private final CertificationCandidateDocumentMapper candidateDocumentMapper;
  private final CertificationTaskExamSiteMapper taskExamSiteMapper;
  private final CertificationTaskScopeMapper taskScopeMapper;
  private final PracticalCollectionStatusMapper statusMapper;
  private final PracticalCollectionQueueMapper queueMapper;
  private final PracticalFingerprintTraceMapper fingerprintTraceMapper;
  private final PracticalSignatureRecordMapper signatureRecordMapper;
  private final PracticalCollectionExceptionMapper exceptionMapper;
  private final FileApplicationService fileApplicationService;
  private final PracticalCollectionDomainService domainService;
  private final PracticalCollectionEligibilityService eligibilityService;
  private final PracticalCollectionConverter converter;
  private final SecureRandom secureRandom = new SecureRandom();

  /**
   * 采集链接模板，需包含占位符 {token}，例如
   * {@code https://xxx/practical-collection/scan?token={token}}。
   * 为空时二维码内容退化为 {@code collectionToken=<token>}。
   */
  private final String scanUrlTemplate;

  @Autowired
  public PracticalCollectionApplicationService(
      CertificationTaskMapper taskMapper,
      CertificationCandidateMapper candidateMapper,
      CertificationExamSessionMapper examSessionMapper,
      CertificationCandidateExamSessionMapper candidateExamSessionMapper,
      CertificationCandidateDocumentMapper candidateDocumentMapper,
      CertificationTaskExamSiteMapper taskExamSiteMapper,
      CertificationTaskScopeMapper taskScopeMapper,
      PracticalCollectionStatusMapper statusMapper,
      PracticalCollectionQueueMapper queueMapper,
      PracticalFingerprintTraceMapper fingerprintTraceMapper,
      PracticalSignatureRecordMapper signatureRecordMapper,
      PracticalCollectionExceptionMapper exceptionMapper,
      FileApplicationService fileApplicationService,
      PracticalCollectionDomainService domainService,
      PracticalCollectionEligibilityService eligibilityService,
      PracticalCollectionConverter converter,
      @Value("${platform.practical-collection.scan-url-template:}") String scanUrlTemplate
  ) {
    this.taskMapper = taskMapper;
    this.candidateMapper = candidateMapper;
    this.examSessionMapper = examSessionMapper;
    this.candidateExamSessionMapper = candidateExamSessionMapper;
    this.candidateDocumentMapper = candidateDocumentMapper;
    this.taskExamSiteMapper = taskExamSiteMapper;
    this.taskScopeMapper = taskScopeMapper;
    this.statusMapper = statusMapper;
    this.queueMapper = queueMapper;
    this.fingerprintTraceMapper = fingerprintTraceMapper;
    this.signatureRecordMapper = signatureRecordMapper;
    this.exceptionMapper = exceptionMapper;
    this.fileApplicationService = fileApplicationService;
    this.domainService = domainService;
    this.eligibilityService = eligibilityService;
    this.converter = converter;
    this.scanUrlTemplate = scanUrlTemplate == null ? "" : scanUrlTemplate.trim();
  }

  /** Backward-compatible constructor for template/export-only callers that do not use session-based tasks. */
  public PracticalCollectionApplicationService(
      CertificationTaskMapper taskMapper,
      CertificationCandidateMapper candidateMapper,
      CertificationCandidateDocumentMapper candidateDocumentMapper,
      CertificationTaskExamSiteMapper taskExamSiteMapper,
      CertificationTaskScopeMapper taskScopeMapper,
      PracticalCollectionStatusMapper statusMapper,
      PracticalCollectionQueueMapper queueMapper,
      PracticalFingerprintTraceMapper fingerprintTraceMapper,
      PracticalSignatureRecordMapper signatureRecordMapper,
      PracticalCollectionExceptionMapper exceptionMapper,
      FileApplicationService fileApplicationService,
      PracticalCollectionDomainService domainService,
      PracticalCollectionEligibilityService eligibilityService,
      PracticalCollectionConverter converter,
      String scanUrlTemplate
  ) {
    this(taskMapper, candidateMapper, null, null, candidateDocumentMapper, taskExamSiteMapper, taskScopeMapper,
        statusMapper, queueMapper, fingerprintTraceMapper, signatureRecordMapper, exceptionMapper,
        fileApplicationService, domainService, eligibilityService, converter, scanUrlTemplate);
  }

  public PageResponse<CollectionTaskVO> pageTasks(CollectionTaskQueryRequest request) {
    Long tenantId = current().getTenantId();
    List<CertificationExamSessionEntity> configuredSessions = examSessionMapper.selectList(
        new LambdaQueryWrapper<CertificationExamSessionEntity>()
            .eq(CertificationExamSessionEntity::getTenantId, tenantId)
            .in(CertificationExamSessionEntity::getStatus, List.of("CONFIGURED", "IN_PROGRESS", "ENDED"))
            .eq(CertificationExamSessionEntity::getDeleted, 0)
            .orderByAsc(CertificationExamSessionEntity::getExamDate)
            .orderByAsc(CertificationExamSessionEntity::getStartTime));
    List<CertificationExamSessionEntity> safeSessions = configuredSessions == null
        ? List.of() : configuredSessions;
    List<CollectionTaskVO> taskRows = new ArrayList<>();
    Set<Long> sessionTaskIds = safeSessions.stream()
        .map(CertificationExamSessionEntity::getTaskId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    taskRows.addAll(safeSessions.stream()
        .map(session -> {
          CertificationTaskEntity task = taskMapper.selectById(session.getTaskId());
          if (task == null || (StringUtils.hasText(request.getStatus())
              && !Objects.equals(request.getStatus(), task.getStatus()))) {
            return null;
          }
          String keyword = request.getKeyword();
          if (StringUtils.hasText(keyword)
              && !containsIgnoreCase(task.getTaskName(), keyword)
              && !containsIgnoreCase(task.getTaskNo(), keyword)
              && !containsIgnoreCase(task.getCompanyName(), keyword)
              && !containsIgnoreCase(session.getSessionName(), keyword)) {
            return null;
          }
          return converter.toTaskVO(task, statsForSession(tenantId, session),
              scopesForTask(tenantId, task.getId()), session.getId(), session.getSessionName(), session.getExamType());
        })
        .filter(Objects::nonNull)
        .toList());

    // 兼容旧认定任务：历史任务没有考试场次关系，统一以固定场次名称展示，
    // 并与新场次任务合并返回，避免新增场次后旧任务被整体隐藏。
    taskRows.addAll(listLegacyTaskRows(tenantId, request, sessionTaskIds));
    return paginateTaskRows(taskRows, request);
  }

  private List<CollectionTaskVO> listLegacyTaskRows(
      Long tenantId,
      CollectionTaskQueryRequest request,
      Set<Long> sessionTaskIds
  ) {
    String keyword = request.getKeyword();
    boolean matchesLegacySessionName = StringUtils.hasText(keyword)
        && containsIgnoreCase(LEGACY_COLLECTION_SESSION_NAME, keyword);
    LambdaQueryWrapper<CertificationTaskEntity> wrapper = new LambdaQueryWrapper<CertificationTaskEntity>()
        .eq(CertificationTaskEntity::getTenantId, tenantId)
        .eq(CertificationTaskEntity::getDeleted, 0)
        .and(StringUtils.hasText(keyword) && !matchesLegacySessionName, nested -> nested
            .like(CertificationTaskEntity::getTaskName, request.getKeyword())
            .or()
            .like(CertificationTaskEntity::getTaskNo, request.getKeyword())
            .or()
            .like(CertificationTaskEntity::getCompanyName, request.getKeyword()))
        .eq(StringUtils.hasText(request.getStatus()), CertificationTaskEntity::getStatus, request.getStatus())
        .orderByDesc(CertificationTaskEntity::getExamDate)
        .orderByDesc(CertificationTaskEntity::getCreatedAt);
    if (!sessionTaskIds.isEmpty()) {
      wrapper.notIn(CertificationTaskEntity::getId, sessionTaskIds);
    }
    applyCandidateTaskFilter(wrapper, request);
    List<CertificationTaskEntity> legacyTasks = taskMapper.selectList(wrapper);
    return legacyTasks.stream()
        .map(task -> converter.toTaskVO(task, statsForTask(tenantId, task.getId()),
            scopesForTask(tenantId, task.getId()), null, LEGACY_COLLECTION_SESSION_NAME, "PRACTICE"))
        .toList();
  }

  private PageResponse<CollectionTaskVO> paginateTaskRows(
      List<CollectionTaskVO> taskRows,
      CollectionTaskQueryRequest request
  ) {
    long page = request.getPage();
    long pageSize = request.getPageSize();
    int from = (int) Math.min(Math.max(page - 1, 0) * pageSize, taskRows.size());
    int to = (int) Math.min(from + pageSize, taskRows.size());
    return new PageResponse<>(taskRows.subList(from, to), page, pageSize, taskRows.size());
  }

  private boolean containsIgnoreCase(String value, String keyword) {
    return value != null && keyword != null && value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
  }

  private CollectionStatsVO statsForSession(Long tenantId, CertificationExamSessionEntity session) {
    List<CertificationCandidateExamSessionEntity> relations = candidateExamSessionMapper.selectBySession(
        tenantId, session.getTaskId(), session.getId());
    List<Long> candidateIds = (relations == null ? List.<CertificationCandidateExamSessionEntity>of() : relations).stream()
        .filter(relation -> "ASSIGNED".equalsIgnoreCase(relation.getParticipationStatus()))
        .map(CertificationCandidateExamSessionEntity::getCandidateId)
        .toList();
    List<PracticalCollectionStatusEntity> statuses = statusMapper.selectList(
        new LambdaQueryWrapper<PracticalCollectionStatusEntity>()
            .eq(PracticalCollectionStatusEntity::getTenantId, tenantId)
            .eq(PracticalCollectionStatusEntity::getTaskId, session.getTaskId())
            .eq(PracticalCollectionStatusEntity::getExamSessionId, session.getId())
            .eq(PracticalCollectionStatusEntity::getDeleted, 0));
    if (statuses == null) {
      statuses = List.of();
    }
    int qrIssued = countStatuses(statuses, PracticalCollectionStatusEntity::getQrIssueStatus, QrIssueStatusEnum.ISSUED.getCode());
    int arrived = (int) statuses.stream().filter(status -> status.getArrivedAt() != null).count();
    int queued = countStatuses(statuses, PracticalCollectionStatusEntity::getQueueStatus, CollectionQueueStatusEnum.QUEUED.getCode());
    int collecting = countStatuses(statuses, PracticalCollectionStatusEntity::getOverallStatus, CollectionOverallStatusEnum.COLLECTING.getCode());
    int completed = countStatuses(statuses, PracticalCollectionStatusEntity::getOverallStatus, CollectionOverallStatusEnum.COMPLETED.getCode());
    int pendingRetry = countStatuses(statuses, PracticalCollectionStatusEntity::getOverallStatus, CollectionOverallStatusEnum.PENDING_RETRY.getCode());
    int infoError = countStatuses(statuses, PracticalCollectionStatusEntity::getOverallStatus, CollectionOverallStatusEnum.INFO_ERROR.getCode());
    int exceptionReleased = countStatuses(statuses, PracticalCollectionStatusEntity::getOverallStatus, CollectionOverallStatusEnum.EXCEPTION_RELEASED.getCode());
    return new CollectionStatsVO(candidateIds.size(), qrIssued, arrived, queued, collecting, completed,
        pendingRetry, infoError, exceptionReleased, infoError + exceptionReleased);
  }

  private List<CertificationTaskScopeEntity> scopesForTask(Long tenantId, Long taskId) {
    if (tenantId == null || taskId == null) {
      return List.of();
    }
    List<CertificationTaskScopeEntity> scopes = taskScopeMapper.selectList(
        new LambdaQueryWrapper<CertificationTaskScopeEntity>()
            .eq(CertificationTaskScopeEntity::getTenantId, tenantId)
            .eq(CertificationTaskScopeEntity::getTaskId, taskId)
            .eq(CertificationTaskScopeEntity::getDeleted, 0)
            .orderByAsc(CertificationTaskScopeEntity::getSortOrder)
            .orderByAsc(CertificationTaskScopeEntity::getId));
    return scopes == null ? List.of() : scopes;
  }

  @Transactional
  public CollectionWorkbenchVO workbench(Long taskId) {
    Long tenantId = current().getTenantId();
    initializeCollectionStatus(taskId);
    CertificationTaskEntity task = requireTask(tenantId, taskId);
    CollectionStatsVO stats = statsForTask(tenantId, taskId);
    List<CertificationTaskScopeEntity> scopes = scopesForTask(tenantId, taskId);
    List<CertificationCandidateEntity> candidates = candidateMapper.selectList(
        new LambdaQueryWrapper<CertificationCandidateEntity>()
            .eq(CertificationCandidateEntity::getTenantId, tenantId)
            .eq(CertificationCandidateEntity::getTaskId, taskId)
            .eq(CertificationCandidateEntity::getDeleted, 0)
            .orderByAsc(CertificationCandidateEntity::getId)
            .last("LIMIT 500"));
    Map<Long, CollectionEligibilityResult> eligibilities = eligibilityService.checkCandidates(tenantId, taskId, candidates);
    List<CertificationCandidateEntity> eligibleCandidates = eligibleCandidates(candidates, eligibilities);
    Map<Long, PracticalCollectionStatusEntity> statuses = statusesByCandidateIds(
        tenantId,
        taskId,
        eligibleCandidates.stream().map(CertificationCandidateEntity::getId).toList()
    );
    List<CandidateCollectionItemVO> candidateItems = eligibleCandidates.stream()
        .map(candidate -> converter.toCandidateItemVO(candidate, statuses.get(candidate.getId()), eligibilities.get(candidate.getId())))
        .toList();
    Set<Long> eligibleCandidateIds = eligibleCandidates.stream()
        .map(CertificationCandidateEntity::getId)
        .collect(Collectors.toSet());
    List<PracticalCollectionQueueEntity> queueRows = queueMapper.selectList(
        new LambdaQueryWrapper<PracticalCollectionQueueEntity>()
            .eq(PracticalCollectionQueueEntity::getTenantId, tenantId)
            .eq(PracticalCollectionQueueEntity::getTaskId, taskId)
            .in(PracticalCollectionQueueEntity::getQueueStatus,
                List.of(CollectionQueueStatusEnum.QUEUED.getCode(), CollectionQueueStatusEnum.COLLECTING.getCode()))
            .eq(PracticalCollectionQueueEntity::getDeleted, 0)
            .orderByAsc(PracticalCollectionQueueEntity::getQueueNo)
            .orderByAsc(PracticalCollectionQueueEntity::getJoinedAt))
        .stream()
        .filter(queue -> eligibleCandidateIds.contains(queue.getCandidateId()))
        .toList();
    Map<Long, CertificationCandidateEntity> candidateMap = eligibleCandidates.stream()
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
        converter.toTaskVO(task, stats, scopes),
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
    CollectionEligibilityResult eligibility = eligibilityService.check(user.getTenantId(), taskId, candidateId);
    return converter.toCandidateDetailVO(
        candidate,
        status,
        null,
        queueItem,
        converter.toFingerprintTraceVO(fingerprintTrace),
        converter.toSignatureRecordVO(signatureRecord),
        exceptions,
        eligibility
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
    Long tenantId = current().getTenantId();
    int generated = generateQrTokens(taskId);
    List<PracticalCollectionStatusEntity> statuses = statusMapper.selectList(
        new LambdaQueryWrapper<PracticalCollectionStatusEntity>()
            .eq(PracticalCollectionStatusEntity::getTenantId, tenantId)
            .eq(PracticalCollectionStatusEntity::getTaskId, taskId)
            .eq(PracticalCollectionStatusEntity::getDeleted, 0));
    int total = statuses.size();
    long issued = statuses.stream().filter(status -> StringUtils.hasText(status.getCollectToken())).count();
    long existing = Math.max(issued - generated, 0);
    String message = String.format(
        "二维码 token 生成完成：本次新增 %d 个，已有 %d 个，共 %d/%d 名考生已发放",
        generated, existing, issued, total);
    return actionResult(taskId, message);
  }

  /**
   * 导出当前任务考生的采集二维码。生成真实二维码图片并嵌入 Excel，
   * 同时包含姓名、准考证号、token、二维码内容与采集链接。
   */
  @Transactional
  public QrCodeExportVO exportQrCodes(Long taskId) {
    Long tenantId = current().getTenantId();
    CertificationTaskEntity task = requireTask(tenantId, taskId);
    generateQrTokens(taskId);
    List<CertificationCandidateEntity> candidates = candidateMapper.selectList(
        new LambdaQueryWrapper<CertificationCandidateEntity>()
            .eq(CertificationCandidateEntity::getTenantId, tenantId)
            .eq(CertificationCandidateEntity::getTaskId, taskId)
            .eq(CertificationCandidateEntity::getDeleted, 0)
            .orderByAsc(CertificationCandidateEntity::getId));
    Map<Long, PracticalCollectionStatusEntity> statuses = statusesByCandidateIds(
        tenantId,
        taskId,
        candidates.stream().map(CertificationCandidateEntity::getId).toList());
    byte[] data = buildQrCodeWorkbook(candidates, statuses);
    String filename = String.format(
        "practical-collection-qrcodes-%s-%s.xlsx",
        taskId,
        LocalDateTime.now().format(FILE_TS_FORMAT));
    LocalDateTime now = LocalDateTime.now();
    statuses.values().forEach(status -> {
      if (StringUtils.hasText(status.getCollectToken())) {
        status.setQrExportedAt(now);
        statusMapper.updateById(status);
      }
    });
    return new QrCodeExportVO(data, filename);
  }

  /** Strict archive export: every selected candidate must pass the current eligibility gate and have all capture images readable. */
  @Transactional(readOnly = true)
  public CollectionFormExportVO exportCollectionForms(Long taskId, CollectionFormExportRequest request) {
    Long tenantId = current().getTenantId();
    CertificationTaskEntity task = requireTask(tenantId, taskId);
    List<Long> candidateIds = request.candidateIds().stream().filter(Objects::nonNull).distinct().toList();
    List<CertificationCandidateEntity> candidates = candidateMapper.selectList(
        new LambdaQueryWrapper<CertificationCandidateEntity>()
            .eq(CertificationCandidateEntity::getTenantId, tenantId)
            .eq(CertificationCandidateEntity::getTaskId, taskId)
            .in(CertificationCandidateEntity::getId, candidateIds)
            .eq(CertificationCandidateEntity::getDeleted, 0)
            .orderByAsc(CertificationCandidateEntity::getId));
    Map<Long, CertificationCandidateEntity> candidateMap = (candidates == null ? List.<CertificationCandidateEntity>of() : candidates).stream()
        .collect(Collectors.toMap(CertificationCandidateEntity::getId, Function.identity(), (left, right) -> left));
    Map<Long, CollectionEligibilityResult> eligibilities = eligibilityService.checkCandidates(tenantId, taskId, candidates);
    Map<Long, PracticalCollectionStatusEntity> statuses = statusesByCandidateIds(tenantId, taskId, candidateIds);
    List<String> errors = new ArrayList<>();
    List<CollectionFormCandidate> exportCandidates = new ArrayList<>();
    for (Long candidateId : candidateIds) {
      CertificationCandidateEntity candidate = candidateMap.get(candidateId);
      if (candidate == null) {
        errors.add("考生ID" + candidateId + "：不属于当前任务或已被删除");
        continue;
      }
      CollectionEligibilityResult eligibility = eligibilities.get(candidateId);
      PracticalCollectionStatusEntity status = statuses.get(candidateId);
      if (eligibility == null || !eligibility.eligible()) {
        errors.add(candidateLabel(candidate) + "：" + (eligibility == null ? "资料状态无法确认" : eligibility.reasonMessage()));
        continue;
      }
      if (status == null || !CollectionOverallStatusEnum.canExportCollectionForm(
          status.getOverallStatus(), status.getExceptionStatus())) {
        errors.add(candidateLabel(candidate) + "：仅已完成或已放行的考生可生成采集表");
        continue;
      }
      boolean released = CollectionOverallStatusEnum.isExemptReleased(
          status.getOverallStatus(), status.getExceptionStatus());
      try {
        byte[] avatar = readImageFile(tenantId, eligibility.avatarFileId(), "证件照");
        PracticalFingerprintTraceEntity fingerprint = latestFingerprintTrace(tenantId, taskId, candidateId);
        PracticalSignatureRecordEntity signature = latestSignatureRecord(tenantId, taskId, candidateId);
        byte[] fingerprintImage;
        byte[] signatureImage;
        if (released) {
          fingerprintImage = fingerprint == null
              ? new byte[0]
              : readImageFileOrEmpty(tenantId, fingerprint.getFingerprintImageFileId(), "指纹图片");
          signatureImage = signature == null
              ? new byte[0]
              : readImageFileOrEmpty(tenantId, signature.getSignatureFileId(), "电子签名图片");
        } else {
          if (fingerprint == null || !FingerprintCollectStatusEnum.SUCCESS.getCode().equals(fingerprint.getCollectStatus())
              || fingerprint.getFingerprintImageFileId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "缺少有效指纹图片");
          }
          fingerprintImage = readImageFile(tenantId, fingerprint.getFingerprintImageFileId(), "指纹图片");
          if (signature == null || signature.getSignatureFileId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "缺少有效电子签名图片");
          }
          signatureImage = readImageFile(tenantId, signature.getSignatureFileId(), "电子签名图片");
        }
        exportCandidates.add(new CollectionFormCandidate(candidate, avatar, fingerprintImage, signatureImage));
      } catch (BusinessException ex) {
        errors.add(candidateLabel(candidate) + "：" + ex.getMessage());
      }
    }
    if (!errors.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "本次选择" + candidateIds.size() + "名考生，其中" + errors.size()
              + "名不满足采集表生成条件：\n" + String.join("\n", errors) + "\n请补充资料后重新生成。");
    }
    if (exportCandidates.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请先选择需要生成采集表的考生");
    }
    CertificationTaskExamSiteEntity site = taskExamSiteMapper.selectOne(new LambdaQueryWrapper<CertificationTaskExamSiteEntity>()
        .eq(CertificationTaskExamSiteEntity::getTenantId, tenantId)
        .eq(CertificationTaskExamSiteEntity::getTaskId, taskId)
        .eq(CertificationTaskExamSiteEntity::getDeleted, 0)
        .orderByAsc(CertificationTaskExamSiteEntity::getSortOrder)
        .last("LIMIT 1"));
    CertificationTaskScopeEntity scope = taskScopeMapper.selectOne(new LambdaQueryWrapper<CertificationTaskScopeEntity>()
        .eq(CertificationTaskScopeEntity::getTenantId, tenantId)
        .eq(CertificationTaskScopeEntity::getTaskId, taskId)
        .eq(CertificationTaskScopeEntity::getDeleted, 0)
        .orderByAsc(CertificationTaskScopeEntity::getSortOrder)
        .last("LIMIT 1"));
    CertificationExamSessionEntity session = resolveExportExamSession(taskId, statuses);
    byte[] data = buildCollectionFormWorkbook(task, site, scope, session, exportCandidates);
    String filename = "职业技能等级认定座次签到表_" + safeFilename(task.getTaskName()) + "_"
        + LocalDateTime.now().format(FILE_TS_FORMAT) + ".xlsx";
    return new CollectionFormExportVO(data, filename);
  }

  private byte[] buildCollectionFormWorkbook(
      CertificationTaskEntity task,
      CertificationTaskExamSiteEntity site,
      CertificationTaskScopeEntity scope,
      List<CollectionFormCandidate> candidates
  ) {
    return buildCollectionFormWorkbook(task, site, scope, null, candidates);
  }

  private byte[] buildCollectionFormWorkbook(
      CertificationTaskEntity task,
      CertificationTaskExamSiteEntity site,
      CertificationTaskScopeEntity scope,
      CertificationExamSessionEntity session,
      List<CollectionFormCandidate> candidates
  ) {
    try (Workbook workbook = openCollectionTemplateWorkbook();
         ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      int pageCount = (candidates.size() + COLLECTION_CANDIDATES_PER_PAGE - 1)
          / COLLECTION_CANDIDATES_PER_PAGE;
      Sheet template = workbook.getSheetAt(0);
      List<Sheet> pages = new ArrayList<>();
      for (int page = 0; page < pageCount; page++) {
        pages.add(copyCollectionTemplateSheet(workbook, template, page + 1));
      }
      workbook.removeSheetAt(workbook.getSheetIndex(template));
      CollectionFormHeader header = collectionFormHeader(task, site, session);
      for (int page = 0; page < pageCount; page++) {
        fillCollectionSheet(pages.get(page), header,
            candidates.subList(page * COLLECTION_CANDIDATES_PER_PAGE,
                Math.min((page + 1) * COLLECTION_CANDIDATES_PER_PAGE, candidates.size())), workbook);
      }
      workbook.write(output);
      return output.toByteArray();
    } catch (IOException ex) {
      log.error("Practical collection form export failed", ex);
      throw new BusinessException(ErrorCode.BAD_REQUEST, "采集表模板读取或生成失败，请检查模板文件");
    }
  }

  private Workbook openCollectionTemplateWorkbook() {
    try (java.io.InputStream input = getClass().getResourceAsStream("/templates/职业技能等级认定座次签到表.xlsx")) {
      if (input != null) {
        Workbook workbook = createTemplateWorkbook(input);
        if (isCurrentCollectionTemplate(workbook)) {
          return workbook;
        }
        log.warn("Classpath seat-chart template is the legacy 9-column layout; generating the current 5x4 template");
        workbook.close();
      }
    } catch (IOException ex) {
      log.warn("Classpath seat-chart template could not be read; generating the current 5x4 template", ex);
    }
    return buildNormalizedCollectionTemplate();
  }

  private Workbook createTemplateWorkbook(java.io.InputStream input) {
    try {
      return WorkbookFactory.create(input);
    } catch (Exception ex) {
      log.warn("Provided WPS/DISPIMG template is not readable by Apache POI; using a POI-normalized equivalent", ex);
      return buildNormalizedCollectionTemplate();
    }
  }

  private boolean isCurrentCollectionTemplate(Workbook workbook) {
    if (workbook == null || workbook.getNumberOfSheets() < 1) {
      return false;
    }
    Sheet sheet = workbook.getSheetAt(0);
    boolean headerFifteenCols = false;
    boolean fourthRowPhoto = false;
    boolean fifthColumnPhoto = false;
    for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
      org.apache.poi.ss.util.CellRangeAddress region = sheet.getMergedRegion(i);
      if (region.getFirstRow() == 0 && region.getFirstColumn() == 0 && region.getLastColumn() >= 14) {
        headerFifteenCols = true;
      }
      if (region.getFirstRow() == COLLECTION_FIRST_BLOCK_ROW + 3 * COLLECTION_BLOCK_ROW_STEP
          && region.getFirstColumn() == 0
          && region.getLastRow() >= COLLECTION_FIRST_BLOCK_ROW + 3 * COLLECTION_BLOCK_ROW_STEP + 3) {
        fourthRowPhoto = true;
      }
      if (region.getFirstRow() == COLLECTION_FIRST_BLOCK_ROW && region.getFirstColumn() == 12) {
        fifthColumnPhoto = true;
      }
    }
    return headerFifteenCols && fourthRowPhoto && fifthColumnPhoto;
  }

  private Workbook buildNormalizedCollectionTemplate() {
    XSSFWorkbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet("座次签到表_优化");
    XSSFFont titleFont = workbook.createFont();
    titleFont.setFontName("Microsoft YaHei");
    titleFont.setFontHeightInPoints((short) 18);
    titleFont.setBold(true);
    XSSFFont infoFont = workbook.createFont();
    infoFont.setFontName("Microsoft YaHei");
    infoFont.setFontHeightInPoints((short) 13);
    XSSFFont labelFont = workbook.createFont();
    labelFont.setFontName("Microsoft YaHei");
    labelFont.setFontHeightInPoints((short) 10);
    labelFont.setBold(true);
    XSSFFont bodyFont = workbook.createFont();
    bodyFont.setFontName("Microsoft YaHei");
    bodyFont.setFontHeightInPoints((short) 10);
    CellStyle titleStyle = rgbStyle(workbook, titleFont, null, HorizontalAlignment.CENTER,
        VerticalAlignment.CENTER, true, false);
    CellStyle infoStyle = rgbStyle(workbook, infoFont, COLLECTION_HEADER_FILL, HorizontalAlignment.LEFT,
        VerticalAlignment.CENTER, true, false);
    CellStyle labelStyle = rgbStyle(workbook, labelFont, COLLECTION_LABEL_FILL, HorizontalAlignment.CENTER,
        VerticalAlignment.CENTER, true, true);
    CellStyle bodyStyle = rgbStyle(workbook, bodyFont, COLLECTION_HEADER_FILL, HorizontalAlignment.CENTER,
        VerticalAlignment.CENTER, true, true);

    sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, COLLECTION_TEMPLATE_COLUMN_COUNT - 1));
    sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, COLLECTION_TEMPLATE_COLUMN_COUNT - 1));
    for (int col = 0; col < COLLECTION_TEMPLATE_COLUMN_COUNT; col++) {
      sheet.setColumnWidth(col, col % 3 == 0 ? 3651 : 2304);
    }
    Row title = sheet.createRow(0);
    title.setHeightInPoints(36);
    Cell titleCell = title.createCell(0);
    titleCell.setCellValue("职业技能等级认定座次签到表");
    titleCell.setCellStyle(titleStyle);
    for (int col = 1; col < COLLECTION_TEMPLATE_COLUMN_COUNT; col++) {
      title.createCell(col).setCellStyle(titleStyle);
    }
    Row info = sheet.createRow(1);
    info.setHeightInPoints(32);
    Cell infoCell = info.createCell(0);
    infoCell.setCellValue("考场地址：                                      考试时间：");
    infoCell.setCellStyle(infoStyle);
    for (int col = 1; col < COLLECTION_TEMPLATE_COLUMN_COUNT; col++) {
      info.createCell(col).setCellStyle(infoStyle);
    }
    for (int row = COLLECTION_FIRST_BLOCK_ROW; row < COLLECTION_TEMPLATE_ROW_COUNT; row++) {
      Row target = sheet.createRow(row);
      target.setHeightInPoints(40);
      for (int col = 0; col < COLLECTION_TEMPLATE_COLUMN_COUNT; col++) {
        target.createCell(col).setCellStyle(bodyStyle);
      }
    }
    for (int block = 0; block < COLLECTION_BLOCK_COUNT; block++) {
      int blockStartRow = COLLECTION_FIRST_BLOCK_ROW + block * COLLECTION_BLOCK_ROW_STEP;
      for (int start : COLLECTION_BLOCK_START_COLUMNS) {
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
            blockStartRow, blockStartRow + 3, start, start));
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
            blockStartRow, blockStartRow + 3, start + 1, start + 1));
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
            blockStartRow + 4, blockStartRow + 4, start + 1, start + 2));
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
            blockStartRow + 5, blockStartRow + 5, start + 1, start + 2));
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
            blockStartRow + 6, blockStartRow + 6, start + 1, start + 2));
        Cell checkIn = sheet.getRow(blockStartRow).getCell(start + 2);
        checkIn.setCellValue("考场签到");
        checkIn.setCellStyle(labelStyle);
        Cell submit = sheet.getRow(blockStartRow + 2).getCell(start + 2);
        submit.setCellValue("交卷确认");
        submit.setCellStyle(labelStyle);
        sheet.getRow(blockStartRow + 5).getCell(start).setCellValue("证件号");
        sheet.getRow(blockStartRow + 6).getCell(start).setCellValue("准考证");
      }
    }
    sheet.setDisplayGridlines(false);
    sheet.setPrintGridlines(false);
    sheet.setFitToPage(true);
    sheet.setAutobreaks(true);
    sheet.setHorizontallyCenter(true);
    sheet.setVerticallyCenter(false);
    PrintSetup printSetup = sheet.getPrintSetup();
    printSetup.setLandscape(false);
    printSetup.setPaperSize(PrintSetup.A4_PAPERSIZE);
    printSetup.setScale((short) 100);
    printSetup.setFitWidth((short) 1);
    printSetup.setFitHeight((short) 1);
    sheet.setMargin(Sheet.LeftMargin, 0.4);
    sheet.setMargin(Sheet.RightMargin, 0.4);
    sheet.setMargin(Sheet.TopMargin, 0.5);
    sheet.setMargin(Sheet.BottomMargin, 0.5);
    workbook.setPrintArea(workbook.getSheetIndex(sheet), 0, COLLECTION_TEMPLATE_COLUMN_COUNT - 1,
        0, COLLECTION_TEMPLATE_ROW_COUNT - 1);
    return workbook;
  }

  private Sheet copyCollectionTemplateSheet(Workbook workbook, Sheet source, int pageNo) {
    Sheet target = workbook.createSheet("座次签到表_" + pageNo);
    target.setDefaultColumnWidth(source.getDefaultColumnWidth());
    target.setDefaultRowHeight(source.getDefaultRowHeight());
    target.setDefaultRowHeightInPoints(source.getDefaultRowHeightInPoints());
    target.setDisplayGridlines(source.isDisplayGridlines());
    target.setPrintGridlines(source.isPrintGridlines());
    target.setFitToPage(source.getFitToPage());
    target.setAutobreaks(source.getAutobreaks());
    target.setHorizontallyCenter(source.getHorizontallyCenter());
    target.setVerticallyCenter(source.getVerticallyCenter());
    for (int col = 0; col < COLLECTION_TEMPLATE_COLUMN_COUNT; col++) {
      target.setColumnWidth(col, source.getColumnWidth(col));
      target.setColumnHidden(col, source.isColumnHidden(col));
    }
    for (int rowNo = 0; rowNo < COLLECTION_TEMPLATE_ROW_COUNT; rowNo++) {
      Row sourceRow = source.getRow(rowNo);
      Row targetRow = target.createRow(rowNo);
      if (sourceRow != null) {
        targetRow.setHeight(sourceRow.getHeight());
        targetRow.setZeroHeight(sourceRow.getZeroHeight());
      }
      if (sourceRow == null) continue;
      for (int col = 0; col < COLLECTION_TEMPLATE_COLUMN_COUNT; col++) {
        Cell sourceCell = sourceRow.getCell(col);
        if (sourceCell == null) continue;
        Cell targetCell = targetRow.createCell(col);
        targetCell.setCellStyle(sourceCell.getCellStyle());
        switch (sourceCell.getCellType()) {
          case STRING -> targetCell.setCellValue(sourceCell.getStringCellValue());
          case NUMERIC -> targetCell.setCellValue(sourceCell.getNumericCellValue());
          case BOOLEAN -> targetCell.setCellValue(sourceCell.getBooleanCellValue());
          case FORMULA -> targetCell.setCellFormula(sourceCell.getCellFormula());
          default -> targetCell.setBlank();
        }
      }
    }
    for (int i = 0; i < source.getNumMergedRegions(); i++) {
      org.apache.poi.ss.util.CellRangeAddress region = source.getMergedRegion(i);
      if (region.getFirstRow() < COLLECTION_TEMPLATE_ROW_COUNT
          && region.getLastRow() < COLLECTION_TEMPLATE_ROW_COUNT
          && region.getFirstColumn() < COLLECTION_TEMPLATE_COLUMN_COUNT
          && region.getLastColumn() < COLLECTION_TEMPLATE_COLUMN_COUNT) {
        target.addMergedRegion(region.copy());
      }
    }
    workbook.setPrintArea(workbook.getSheetIndex(target), 0,
        COLLECTION_TEMPLATE_COLUMN_COUNT - 1, 0, COLLECTION_TEMPLATE_ROW_COUNT - 1);
    target.getPrintSetup().setLandscape(source.getPrintSetup().getLandscape());
    target.getPrintSetup().setPaperSize(source.getPrintSetup().getPaperSize());
    target.getPrintSetup().setScale(source.getPrintSetup().getScale());
    target.getPrintSetup().setFitWidth(source.getPrintSetup().getFitWidth());
    target.getPrintSetup().setFitHeight(source.getPrintSetup().getFitHeight());
    target.setMargin(Sheet.LeftMargin, source.getMargin(Sheet.LeftMargin));
    target.setMargin(Sheet.RightMargin, source.getMargin(Sheet.RightMargin));
    target.setMargin(Sheet.TopMargin, source.getMargin(Sheet.TopMargin));
    target.setMargin(Sheet.BottomMargin, source.getMargin(Sheet.BottomMargin));
    return target;
  }

  private void fillCollectionSheet(
      Sheet sheet,
      CollectionFormHeader header,
      List<CollectionFormCandidate> candidates,
      Workbook workbook
  ) {
    setCellValue(sheet, 0, 0, "职业技能等级认定座次签到表");
    setCellValue(sheet, 1, 0, header.headerLine());
    int candidateIndex = 0;
    for (int block = 0; block < COLLECTION_BLOCK_COUNT; block++) {
      int blockStartRow = COLLECTION_FIRST_BLOCK_ROW + block * COLLECTION_BLOCK_ROW_STEP;
      for (int start : COLLECTION_BLOCK_START_COLUMNS) {
        CollectionFormCandidate candidate = candidateIndex < candidates.size()
            ? candidates.get(candidateIndex) : null;
        candidateIndex++;
        clearPictureCells(sheet, start, blockStartRow);
        clearCandidateCells(sheet, start, blockStartRow);
        if (candidate == null) {
          continue;
        }
        setCellValue(sheet, blockStartRow + 4, start, candidate.candidate().getCandidateName());
        setCellValue(sheet, blockStartRow + 4, start + 1, header.secondColumnText());
        setCellValue(sheet, blockStartRow + 5, start + 1, candidate.candidate().getIdCardNo());
        setCellValue(sheet, blockStartRow + 6, start + 1, candidate.candidate().getAdmissionTicketNo());
        addImage(workbook, sheet, candidate.avatar(), start, blockStartRow, 1, 4);
        addImage(workbook, sheet, candidate.fingerprint(), start + 1, blockStartRow, 1, 4);
        addImage(workbook, sheet, candidate.signature(), start + 2, blockStartRow + 1, 1, 1);
        addImage(workbook, sheet, candidate.signature(), start + 2, blockStartRow + 3, 1, 1);
      }
    }
  }

  private CertificationExamSessionEntity resolveExportExamSession(
      Long taskId,
      Map<Long, PracticalCollectionStatusEntity> statuses
  ) {
    if (examSessionMapper == null) {
      return null;
    }
    if (statuses != null && !statuses.isEmpty()) {
      List<Long> sessionIds = statuses.values().stream()
          .map(PracticalCollectionStatusEntity::getExamSessionId)
          .filter(Objects::nonNull)
          .distinct()
          .toList();
      if (!sessionIds.isEmpty()) {
        CertificationExamSessionEntity linked = examSessionMapper.selectById(sessionIds.get(0));
        if (linked != null) {
          return linked;
        }
      }
    }
    if (taskId == null) {
      return null;
    }
    List<CertificationExamSessionEntity> sessions = examSessionMapper.selectList(
        new LambdaQueryWrapper<CertificationExamSessionEntity>()
            .eq(CertificationExamSessionEntity::getTaskId, taskId)
            .eq(CertificationExamSessionEntity::getDeleted, 0)
            .orderByAsc(CertificationExamSessionEntity::getExamDate)
            .orderByAsc(CertificationExamSessionEntity::getStartTime)
            .orderByAsc(CertificationExamSessionEntity::getId));
    return sessions == null || sessions.isEmpty() ? null : sessions.get(0);
  }

  private CollectionFormHeader collectionFormHeader(
      CertificationTaskEntity task,
      CertificationTaskExamSiteEntity site,
      CertificationExamSessionEntity session
  ) {
    String venue = resolveExamVenue(session, site);
    String examTime = resolveExamTimeText(task, site, session);
    String examType = examTypeLabel(resolveExamType(session, site));
    String headerLine = "考场地址：" + venue
        + "                                                                           考试时间：" + examTime;
    return new CollectionFormHeader(headerLine, examType, venue, examTime);
  }

  private String resolveExamType(
      CertificationExamSessionEntity session,
      CertificationTaskExamSiteEntity site
  ) {
    if (session != null && StringUtils.hasText(session.getExamType())) {
      return session.getExamType();
    }
    return site == null ? null : site.getExamType();
  }

  private String resolveExamVenue(
      CertificationExamSessionEntity session,
      CertificationTaskExamSiteEntity site
  ) {
    if (session != null && StringUtils.hasText(session.getExamVenue())) {
      return session.getExamVenue().trim();
    }
    if (site != null && StringUtils.hasText(site.getSiteName())) {
      return site.getSiteName().trim();
    }
    if (site != null && StringUtils.hasText(site.getAddress())) {
      return site.getAddress().trim();
    }
    return "";
  }

  private String resolveExamTimeText(
      CertificationTaskEntity task,
      CertificationTaskExamSiteEntity site,
      CertificationExamSessionEntity session
  ) {
    if (session != null) {
      return formatExamTime(
          session.getExamType(),
          session.getExamDate(),
          session.getStartTime(),
          session.getEndTime(),
          null,
          null);
    }
    LocalDate examDate = site != null && site.getExamDate() != null
        ? site.getExamDate()
        : task == null ? null : task.getExamDate();
    return formatExamTime(
        site == null ? null : site.getExamType(),
        examDate,
        null,
        null,
        site == null ? null : site.getStartTime(),
        site == null ? null : site.getEndTime());
  }

  private String formatExamTime(
      String examType,
      LocalDate examDate,
      LocalDateTime startTime,
      LocalDateTime endTime,
      String startText,
      String endText
  ) {
    String typeLabel = examTypeLabel(examType);
    String startPart = formatDateTimePart(examDate, startTime, startText);
    LocalDate endDate = endTime != null ? endTime.toLocalDate() : examDate;
    String endPart = formatDateTimePart(endDate, endTime, endText);
    String range;
    if (StringUtils.hasText(startPart) && StringUtils.hasText(endPart) && !startPart.equals(endPart)) {
      range = startPart + "~" + endPart;
    } else {
      range = StringUtils.hasText(startPart) ? startPart : endPart;
    }
    if (StringUtils.hasText(typeLabel) && StringUtils.hasText(range)) {
      return typeLabel + " " + range;
    }
    return StringUtils.hasText(typeLabel) ? typeLabel : nullToEmpty(range);
  }

  private String formatDateTimePart(LocalDate examDate, LocalDateTime dateTime, String clockText) {
    if (dateTime != null) {
      return dateTime.toLocalDate().format(SESSION_DATE_FORMAT) + " " + dateTime.format(SESSION_TIME_FORMAT);
    }
    String dateText = examDate == null ? "" : examDate.format(SESSION_DATE_FORMAT);
    String timeText = nullToEmpty(clockText).trim();
    if (StringUtils.hasText(dateText) && StringUtils.hasText(timeText)) {
      return dateText + " " + timeText;
    }
    return StringUtils.hasText(dateText) ? dateText : timeText;
  }

  private String examTypeLabel(String examType) {
    if (!StringUtils.hasText(examType)) {
      return "";
    }
    String value = examType.trim();
    String upper = value.toUpperCase(java.util.Locale.ROOT);
    if ("PRACTICE".equals(upper) || "PRACTICAL".equals(upper) || "实操".equals(value)) {
      return "实操";
    }
    if ("DEFENSE".equals(upper) || "答辩".equals(value)) {
      return "答辩";
    }
    if ("THEORY".equals(upper) || "理论".equals(value)) {
      return "理论";
    }
    return value;
  }

  private CellStyle rgbStyle(
      XSSFWorkbook workbook,
      XSSFFont font,
      byte[] rgb,
      HorizontalAlignment horizontal,
      VerticalAlignment vertical,
      boolean wrap,
      boolean border
  ) {
    XSSFCellStyle style = workbook.createCellStyle();
    style.setFont(font);
    style.setAlignment(horizontal);
    style.setVerticalAlignment(vertical);
    style.setWrapText(wrap);
    if (rgb != null) {
      style.setFillForegroundColor(new XSSFColor(rgb, new DefaultIndexedColorMap()));
      style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    }
    if (border) {
      style.setBorderTop(BorderStyle.THIN);
      style.setBorderBottom(BorderStyle.THIN);
      style.setBorderLeft(BorderStyle.THIN);
      style.setBorderRight(BorderStyle.THIN);
    }
    return style;
  }

  private void clearPictureCells(Sheet sheet, int start, int blockStartRow) {
    setCellValue(sheet, blockStartRow, start, null);
    setCellValue(sheet, blockStartRow, start + 1, null);
    setCellValue(sheet, blockStartRow + 1, start + 2, null);
    setCellValue(sheet, blockStartRow + 3, start + 2, null);
  }

  private void clearCandidateCells(Sheet sheet, int start, int blockStartRow) {
    setCellValue(sheet, blockStartRow + 4, start, null);
    setCellValue(sheet, blockStartRow + 4, start + 1, null);
    setCellValue(sheet, blockStartRow + 4, start + 2, null);
    setCellValue(sheet, blockStartRow + 5, start + 1, null);
    setCellValue(sheet, blockStartRow + 6, start + 1, null);
    setCellValue(sheet, blockStartRow + 5, start + 2, null);
    setCellValue(sheet, blockStartRow + 6, start + 2, null);
  }

  private void setCellValue(Sheet sheet, int row, int col, String value) {
    Row targetRow = sheet.getRow(row);
    if (targetRow == null) targetRow = sheet.createRow(row);
    Cell cell = targetRow.getCell(col);
    if (cell == null) cell = targetRow.createCell(col);
    cell.setBlank();
    if (value != null) cell.setCellValue(value);
  }

  private byte[] readImageFileOrEmpty(Long tenantId, Long fileId, String label) {
    if (fileId == null) {
      return new byte[0];
    }
    try {
      return readImageFile(tenantId, fileId, label);
    } catch (BusinessException ex) {
      log.warn("Optional collection export image skipped: label={}, fileId={}, reason={}", label, fileId, ex.getMessage());
      return new byte[0];
    }
  }

  private void addImage(Workbook workbook, Sheet sheet, byte[] source, int col, int row, int colSpan, int rowSpan) {
    if (source == null || source.length == 0) {
      return;
    }
    try {
      BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(source));
      if (image == null) throw new IOException("not an image");
      int displayWidth = 0;
      for (int column = col; column < col + colSpan; column++) {
        displayWidth += Math.max(1, Math.round(sheet.getColumnWidthInPixels(column)));
      }
      int displayHeight = 0;
      for (int targetRow = row; targetRow < row + rowSpan; targetRow++) {
        Row target = sheet.getRow(targetRow);
        float heightInPoints = target == null || target.getHeightInPoints() <= 0
            ? sheet.getDefaultRowHeightInPoints() : target.getHeightInPoints();
        displayHeight += Math.max(1, Math.round(heightInPoints * 96f / 72f));
      }
      // 嵌入图按打印级分辨率生成，单元格锚点仍占原区域，避免先压成屏幕像素再被 Excel/打印机二次缩放。
      int canvasWidth = Math.max(1, displayWidth * COLLECTION_IMAGE_EXPORT_SCALE);
      int canvasHeight = Math.max(1, displayHeight * COLLECTION_IMAGE_EXPORT_SCALE);
      BufferedImage content = cropWhitespace(image);
      BufferedImage normalized = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_RGB);
      Graphics2D graphics = normalized.createGraphics();
      try {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
            RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        graphics.setColor(java.awt.Color.WHITE);
        graphics.fillRect(0, 0, canvasWidth, canvasHeight);
        double scale = Math.min((double) canvasWidth / content.getWidth(),
            (double) canvasHeight / content.getHeight());
        int drawWidth = Math.max(1, (int) Math.round(content.getWidth() * scale));
        int drawHeight = Math.max(1, (int) Math.round(content.getHeight() * scale));
        int drawX = (canvasWidth - drawWidth) / 2;
        int drawY = (canvasHeight - drawHeight) / 2;
        graphics.drawImage(content, drawX, drawY, drawWidth, drawHeight, null);
      } finally {
        graphics.dispose();
      }
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      ImageIO.write(normalized, "png", bytes);
      int pictureIndex = workbook.addPicture(bytes.toByteArray(), Workbook.PICTURE_TYPE_PNG);
      XSSFDrawing drawing = ((XSSFSheet) sheet).createDrawingPatriarch();
      XSSFClientAnchor anchor = new XSSFClientAnchor();
      anchor.setCol1(col);
      anchor.setRow1(row);
      anchor.setCol2(col + colSpan);
      anchor.setRow2(row + rowSpan);
      anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE);
      drawing.createPicture(anchor, pictureIndex);
    } catch (IOException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "图片无法读取，正式导出已阻止");
    }
  }

  /**
   * 裁掉大块留白，让签名笔画在单元格内更大更清晰；无有效内容时退回原图。
   */
  private BufferedImage cropWhitespace(BufferedImage image) {
    int width = image.getWidth();
    int height = image.getHeight();
    if (width <= 2 || height <= 2) {
      return image;
    }
    int minX = width;
    int minY = height;
    int maxX = -1;
    int maxY = -1;
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int rgb = image.getRGB(x, y);
        int alpha = (rgb >>> 24) & 0xFF;
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        boolean transparent = alpha < 16;
        boolean nearWhite = red >= COLLECTION_IMAGE_WHITE_THRESHOLD
            && green >= COLLECTION_IMAGE_WHITE_THRESHOLD
            && blue >= COLLECTION_IMAGE_WHITE_THRESHOLD;
        if (transparent || nearWhite) {
          continue;
        }
        if (x < minX) {
          minX = x;
        }
        if (y < minY) {
          minY = y;
        }
        if (x > maxX) {
          maxX = x;
        }
        if (y > maxY) {
          maxY = y;
        }
      }
    }
    if (maxX < minX || maxY < minY) {
      return image;
    }
    int pad = COLLECTION_IMAGE_CONTENT_PADDING_PX;
    int cropX = Math.max(0, minX - pad);
    int cropY = Math.max(0, minY - pad);
    int cropW = Math.min(width - cropX, maxX - minX + 1 + pad * 2);
    int cropH = Math.min(height - cropY, maxY - minY + 1 + pad * 2);
    if (cropW <= 0 || cropH <= 0 || (cropW == width && cropH == height)) {
      return image;
    }
    BufferedImage cropped = new BufferedImage(cropW, cropH, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = cropped.createGraphics();
    try {
      graphics.setColor(java.awt.Color.WHITE);
      graphics.fillRect(0, 0, cropW, cropH);
      graphics.drawImage(image, 0, 0, cropW, cropH, cropX, cropY, cropX + cropW, cropY + cropH, null);
    } finally {
      graphics.dispose();
    }
    return cropped;
  }

  private byte[] readImageFile(Long tenantId, Long fileId, String label) {
    if (fileId == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "缺少" + label);
    FileEntity file = fileApplicationService.getExistingFile(tenantId, fileId);
    try {
      byte[] bytes = fileApplicationService.readBytes(fileId);
      if (bytes == null || bytes.length == 0 || ImageIO.read(new java.io.ByteArrayInputStream(bytes)) == null) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, label + "文件不可读取");
      }
      return bytes;
    } catch (BusinessException ex) {
      throw ex;
    } catch (IOException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, label + "文件不可读取");
    }
  }

  private String candidateLabel(CertificationCandidateEntity candidate) {
    return nullToEmpty(candidate.getCandidateName()) + "，准考证号" + nullToEmpty(candidate.getAdmissionTicketNo());
  }

  private String safeFilename(String value) {
    return StringUtils.hasText(value) ? value.replaceAll("[\\\\/:*?\"<>|]", "_") : "任务";
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private record CollectionFormCandidate(
      CertificationCandidateEntity candidate,
      byte[] avatar,
      byte[] fingerprint,
      byte[] signature
  ) {
  }

  private record CollectionFormHeader(
      String headerLine,
      String secondColumnText,
      String examVenue,
      String examTimeText
  ) {
  }

  private byte[] buildQrCodeWorkbook(
      List<CertificationCandidateEntity> candidates,
      Map<Long, PracticalCollectionStatusEntity> statuses
  ) {
    String[] headers = {
        "序号", "姓名", "准考证号", "身份证后四位", "单位", "工种", "等级",
        "二维码token", "二维码内容", "采集链接", "二维码图片"
    };
    try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("采集二维码");
      Drawing<?> drawing = sheet.createDrawingPatriarch();
      Row header = sheet.createRow(0);
      for (int col = 0; col < headers.length; col++) {
        header.createCell(col).setCellValue(headers[col]);
      }
      int[] widths = {1600, 3200, 5000, 3600, 5200, 4200, 3000, 9000, 9000, 9000, 6000};
      for (int col = 0; col < widths.length; col++) {
        sheet.setColumnWidth(col, widths[col]);
      }
      int rowIdx = 1;
      int seq = 1;
      for (CertificationCandidateEntity candidate : candidates) {
        PracticalCollectionStatusEntity status = statuses.get(candidate.getId());
        String token = status == null ? null : status.getCollectToken();
        String qrContent = buildQrContent(token);
        String scanLink = buildScanLink(token);
        Row row = sheet.createRow(rowIdx);
        row.setHeightInPoints(120f);
        row.createCell(0).setCellValue(seq);
        row.createCell(1).setCellValue(nullToDash(candidate.getCandidateName()));
        row.createCell(2).setCellValue(nullToDash(candidate.getAdmissionTicketNo()));
        row.createCell(3).setCellValue(status == null ? "-" : nullToDash(status.getIdCardLast4()));
        row.createCell(4).setCellValue(nullToDash(candidate.getOrgName()));
        row.createCell(5).setCellValue(nullToDash(candidate.getWorkTypeName()));
        row.createCell(6).setCellValue(nullToDash(candidate.getLevelName()));
        row.createCell(7).setCellValue(nullToDash(token));
        row.createCell(8).setCellValue(nullToDash(qrContent));
        row.createCell(9).setCellValue(nullToDash(scanLink));
        if (StringUtils.hasText(qrContent)) {
          byte[] png = qrPngBytes(qrContent);
          if (png != null) {
            int pictureIdx = workbook.addPicture(png, Workbook.PICTURE_TYPE_PNG);
            ClientAnchor anchor = workbook.getCreationHelper().createClientAnchor();
            anchor.setCol1(10);
            anchor.setRow1(rowIdx);
            anchor.setCol2(11);
            anchor.setRow2(rowIdx + 1);
            drawing.createPicture(anchor, pictureIdx);
          }
        }
        rowIdx++;
        seq++;
      }
      workbook.write(output);
      return output.toByteArray();
    } catch (IOException ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "生成二维码导出文件失败: " + ex.getMessage());
    }
  }

  private byte[] qrPngBytes(String content) {
    try {
      QRCodeWriter writer = new QRCodeWriter();
      Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
      hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
      hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
      hints.put(EncodeHintType.MARGIN, 1);
      BitMatrix matrix = writer.encode(
          content, BarcodeFormat.QR_CODE, QR_EXPORT_IMAGE_SIZE, QR_EXPORT_IMAGE_SIZE, hints);
      BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      ImageIO.write(image, "PNG", out);
      return out.toByteArray();
    } catch (WriterException ex) {
      return null;
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  private String buildQrContent(String token) {
    if (!StringUtils.hasText(token)) {
      return "";
    }
    if (scanUrlTemplate.contains("{token}")) {
      return scanUrlTemplate.replace("{token}", token);
    }
    return "collectionToken=" + token;
  }

  private String buildScanLink(String token) {
    if (!StringUtils.hasText(token) || !scanUrlTemplate.contains("{token}")) {
      return "";
    }
    return scanUrlTemplate.replace("{token}", token);
  }

  private String nullToDash(String value) {
    return StringUtils.hasText(value) ? value : "-";
  }

  /**
   * 扫码匹配当前考生并推进到采集中状态。token 允许是纯 token、二维码内容或采集链接。
   */
  @Transactional
  public ScanMatchResultVO scanMatchCurrent(Long taskId, String rawToken) {
    PlatformUserPrincipal user = current();
    requireTask(user.getTenantId(), taskId);
    String token = normalizeToken(rawToken);
    if (!StringUtils.hasText(token)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "二维码内容为空，无法匹配考生");
    }
    PracticalCollectionStatusEntity status =
        statusMapper.lockByTaskAndToken(user.getTenantId(), taskId, token);
    if (status == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "采集二维码无效或不属于当前任务");
    }
    CertificationCandidateEntity candidate =
        requireCandidate(user.getTenantId(), taskId, status.getCandidateId());
    if (CollectionOverallStatusEnum.COMPLETED.getCode().equals(status.getOverallStatus())) {
      return new ScanMatchResultVO(
          true,
          collectionDetail(taskId, candidate.getId()),
          "该考生已完成采集");
    }
    eligibilityService.requireEligible(user.getTenantId(), taskId, candidate.getId());
    CandidateCollectionDetailVO detail = openCurrentCandidate(
        user,
        taskId,
        candidate,
        status,
        CollectionMatchMethodEnum.QR_CODE
    );
    return new ScanMatchResultVO(false, detail, "匹配成功");
  }

  @Transactional
  public CandidateCollectionDetailVO openCandidateCollection(Long taskId, Long candidateId) {
    PlatformUserPrincipal user = current();
    requireTask(user.getTenantId(), taskId);
    CertificationCandidateEntity candidate = requireCandidate(user.getTenantId(), taskId, candidateId);
    PracticalCollectionStatusEntity existingStatus = statusMapper.selectByTaskAndCandidate(user.getTenantId(), taskId, candidateId);
    if (existingStatus != null && CollectionOverallStatusEnum.COMPLETED.getCode().equals(existingStatus.getOverallStatus())) {
      return collectionDetail(taskId, candidate.getId());
    }
    eligibilityService.requireEligible(user.getTenantId(), taskId, candidateId);
    PracticalCollectionStatusEntity status = lockStatusForCandidate(user.getTenantId(), candidate);
    return openCurrentCandidate(
        user,
        taskId,
        candidate,
        status,
        CollectionMatchMethodEnum.MANUAL
    );
  }

  /**
   * 取消当前采集：释放采集中占用，不清空已提交的指纹/签名记录。
   * 取消后考生回到已到场，可再次开采集；不再阻塞其他考生进入采集中。
   */
  @Transactional
  public CollectionActionResultVO cancelCurrentCollection(CancelCurrentCollectionRequest request) {
    PlatformUserPrincipal user = current();
    requireTask(user.getTenantId(), request.taskId());
    CertificationCandidateEntity candidate =
        requireCandidate(user.getTenantId(), request.taskId(), request.candidateId());
    PracticalCollectionStatusEntity status = lockStatusForCandidate(user.getTenantId(), candidate);
    boolean collecting =
        CollectionOverallStatusEnum.COLLECTING.getCode().equals(status.getOverallStatus())
            || CollectionQueueStatusEnum.COLLECTING.getCode().equals(status.getQueueStatus());
    if (!collecting) {
      PracticalCollectionQueueEntity activeQueue =
          queueMapper.selectActiveByCandidate(user.getTenantId(), request.taskId(), request.candidateId());
      if (activeQueue == null
          || !CollectionQueueStatusEnum.COLLECTING.getCode().equals(activeQueue.getQueueStatus())) {
        return converter.toActionResultVO(status, activeQueueItem(user.getTenantId(), candidate, status), "当前考生未在采集中");
      }
    }
    removeActiveQueue(
        user.getTenantId(),
        request.taskId(),
        request.candidateId(),
        user.getUserId(),
        "取消当前采集"
    );
    if (CollectionOverallStatusEnum.COLLECTING.getCode().equals(status.getOverallStatus())) {
      status.setOverallStatus(CollectionOverallStatusEnum.ARRIVED.getCode());
    }
    status.setQueueStatus(CollectionQueueStatusEnum.REMOVED.getCode());
    status.setCollectingStartedAt(null);
    status.setLastOperatorId(user.getUserId());
    statusMapper.updateById(status);
    return converter.toActionResultVO(status, null, "已取消当前采集");
  }

  private CandidateCollectionDetailVO openCurrentCandidate(
      PlatformUserPrincipal user,
      Long taskId,
      CertificationCandidateEntity candidate,
      PracticalCollectionStatusEntity status,
      CollectionMatchMethodEnum matchMethod
  ) {
    eligibilityService.requireEligible(user.getTenantId(), taskId, candidate.getId());
    PracticalCollectionQueueEntity collecting = queueMapper.selectList(
            new LambdaQueryWrapper<PracticalCollectionQueueEntity>()
                .eq(PracticalCollectionQueueEntity::getTenantId, user.getTenantId())
                .eq(PracticalCollectionQueueEntity::getTaskId, taskId)
                .eq(PracticalCollectionQueueEntity::getQueueStatus, CollectionQueueStatusEnum.COLLECTING.getCode())
                .eq(PracticalCollectionQueueEntity::getDeleted, 0)
                .last("LIMIT 1"))
        .stream()
        .findFirst()
        .orElse(null);
    if (collecting != null && !Objects.equals(collecting.getCandidateId(), candidate.getId())) {
      CertificationCandidateEntity currentCandidate =
          candidateById(user.getTenantId(), taskId, collecting.getCandidateId());
      String currentName = currentCandidate == null
          ? String.valueOf(collecting.getCandidateId())
          : currentCandidate.getCandidateName();
      throw new BusinessException(
          ErrorCode.CONFLICT,
          "当前已有正在采集的考生：" + currentName + "，请先完成或处理后再切换");
    }
    LocalDateTime now = LocalDateTime.now();
    status.setMatchMethod(matchMethod.getCode());
    if (status.getArrivedAt() == null) {
      status.setArrivedAt(now);
    }
    status.setLastOperatorId(user.getUserId());
    statusMapper.updateById(status);
    PracticalCollectionQueueEntity queue =
        queueMapper.selectActiveByCandidate(user.getTenantId(), taskId, candidate.getId());
    if (queue == null) {
      joinQueue(new JoinQueueRequest(taskId, candidate.getId(), matchMethod.getCode()));
      queue = queueMapper.selectActiveByCandidate(user.getTenantId(), taskId, candidate.getId());
    }
    if (queue == null) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "加入采集队列失败");
    }
    PracticalCollectionStatusEntity locked = lockStatusForCandidate(user.getTenantId(), candidate);
    if (!CollectionOverallStatusEnum.COLLECTING.getCode().equals(locked.getOverallStatus())) {
      if (!CollectionQueueStatusEnum.COLLECTING.getCode().equals(queue.getQueueStatus())) {
        queue.setQueueStatus(CollectionQueueStatusEnum.COLLECTING.getCode());
        queue.setStartedAt(now);
        queue.setOperatorId(user.getUserId());
        queueMapper.updateById(queue);
      }
      locked.setOverallStatus(CollectionOverallStatusEnum.COLLECTING.getCode());
      locked.setQueueStatus(CollectionQueueStatusEnum.COLLECTING.getCode());
      if (locked.getCollectingStartedAt() == null) {
        locked.setCollectingStartedAt(now);
      }
      locked.setLastOperatorId(user.getUserId());
      statusMapper.updateById(locked);
    }
    return collectionDetail(taskId, candidate.getId());
  }

  private String normalizeToken(String rawToken) {
    if (!StringUtils.hasText(rawToken)) {
      return null;
    }
    String value = rawToken.trim();
    Matcher matcher = TOKEN_QUERY_PATTERN.matcher(value);
    if (matcher.find()) {
      return matcher.group(1).trim();
    }
    return value;
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
    Map<Long, CollectionEligibilityResult> eligibilities = eligibilityService.checkCandidates(tenantId, taskId, candidates);
    int created = 0;
    for (CertificationCandidateEntity candidate : candidates) {
      PracticalCollectionStatusEntity existing = statusMapper.selectByTaskAndCandidate(
          tenantId, taskId, candidate.getId());
      if (existing != null) {
        continue;
      }
      CollectionEligibilityResult eligibility = eligibilities.get(candidate.getId());
      if (eligibility == null || !eligibility.eligible()) {
        log.warn("Skip practical collection status initialization for ineligible candidate: taskId={}, candidateId={}, missing={}, invalid={}",
            taskId, candidate.getId(),
            eligibility == null ? List.of() : eligibility.missingDocumentCodes(),
            eligibility == null ? List.of() : eligibility.invalidDocumentCodes());
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
    if (!CollectionOverallStatusEnum.COMPLETED.getCode().equals(status.getOverallStatus())) {
      eligibilityService.requireEligible(user.getTenantId(), request.taskId(), candidate.getId());
    }
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
    Map<Long, CollectionEligibilityResult> eligibilities = eligibilityService.checkCandidates(tenantId, request.taskId(), candidates);
    candidates = eligibleCandidates(candidates, eligibilities);
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
            inferMatchMethod(keyword, candidate, statuses.get(candidate.getId())),
            eligibilities.get(candidate.getId())
        ))
        .toList();
  }

  private List<CertificationCandidateEntity> eligibleCandidates(
      List<CertificationCandidateEntity> candidates,
      Map<Long, CollectionEligibilityResult> eligibilities
  ) {
    if (candidates == null || candidates.isEmpty() || eligibilities == null || eligibilities.isEmpty()) {
      return List.of();
    }
    return candidates.stream()
        .filter(candidate -> {
          if (candidate == null || candidate.getId() == null) {
            return false;
          }
          CollectionEligibilityResult eligibility = eligibilities.get(candidate.getId());
          return eligibility != null && eligibility.eligible();
        })
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
    eligibilityService.requireEligible(user.getTenantId(), request.taskId(), request.candidateId());
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
  public BatchJoinQueueResultVO joinQueueBatch(BatchJoinQueueRequest request) {
    String matchMethod = StringUtils.hasText(request.matchMethod())
        ? request.matchMethod() : CollectionMatchMethodEnum.MANUAL.getCode();
    List<Long> candidateIds = request.candidateIds() == null ? List.of() : request.candidateIds().stream()
        .filter(Objects::nonNull).distinct().toList();
    int success = 0;
    List<BatchJoinQueueResultVO.BatchJoinQueueFailureVO> failures = new ArrayList<>();
    for (Long candidateId : candidateIds) {
      try {
        joinQueue(new JoinQueueRequest(request.taskId(), candidateId, matchMethod));
        success++;
      } catch (BusinessException ex) {
        CollectionEligibilityResult eligibility = eligibilityService.check(
            current().getTenantId(), request.taskId(), candidateId);
        failures.add(new BatchJoinQueueResultVO.BatchJoinQueueFailureVO(
            candidateId,
            eligibility.candidateName(),
            eligibility.admissionTicketNo(),
            eligibility.missingDocumentCodes(),
            eligibility.invalidDocumentCodes(),
            ex.getMessage()));
      }
    }
    return new BatchJoinQueueResultVO(success, failures.size(), failures);
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
    eligibilityService.requireEligible(user.getTenantId(), request.taskId(), candidate.getId());
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
    eligibilityService.requireEligible(user.getTenantId(), request.taskId(), request.candidateId());
    PracticalCollectionStatusEntity status = lockStatusForCandidate(user.getTenantId(), candidate);
    if (request.examSessionId() != null) {
      status.setExamSessionId(request.examSessionId());
    }
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
    trace.setExamSessionId(request.examSessionId());
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
    eligibilityService.requireEligible(user.getTenantId(), request.taskId(), request.candidateId());
    PracticalCollectionStatusEntity status = lockStatusForCandidate(user.getTenantId(), candidate);
    if (request.examSessionId() != null) {
      status.setExamSessionId(request.examSessionId());
    }
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
    record.setExamSessionId(request.examSessionId());
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
    if (request.examSessionId() != null) {
      status.setExamSessionId(request.examSessionId());
    }
    if (CollectionOverallStatusEnum.COMPLETED.getCode().equals(status.getOverallStatus())) {
      return converter.toActionResultVO(status, activeQueueItem(user.getTenantId(), candidate, status), "采集已完成");
    }
    eligibilityService.requireEligible(user.getTenantId(), request.taskId(), request.candidateId());
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
    domainService.assertNotCompleted(status.getOverallStatus(), "标记异常");
    removeActiveQueue(user.getTenantId(), request.taskId(), request.candidateId(), user.getUserId(), request.reason());
    status.setOverallStatus(CollectionOverallStatusEnum.INFO_ERROR.getCode());
    status.setFingerprintStatus(FingerprintStatusEnum.FAILED.getCode());
    status.setSignatureStatus(SignatureStatusEnum.FAILED.getCode());
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
        CollectionReleaseScopeEnum.ALL.getCode(),
        request.reason(),
        null,
        null
    );
    return converter.toActionResultVO(status, null, "已标记异常");
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
    removeActiveQueue(user.getTenantId(), request.taskId(), request.candidateId(), user.getUserId(), request.reason());
    status.setOverallStatus(CollectionOverallStatusEnum.EXCEPTION_RELEASED.getCode());
    status.setQueueStatus(CollectionQueueStatusEnum.REMOVED.getCode());
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
    return converter.toActionResultVO(status, activeQueueItem(user.getTenantId(), candidate, status), "已记录免考放行");
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
