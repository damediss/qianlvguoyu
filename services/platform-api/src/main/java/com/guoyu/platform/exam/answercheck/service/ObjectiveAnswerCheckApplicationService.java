package com.guoyu.platform.exam.answercheck.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.ai.dto.ObjectiveAnswerCheckAiDescriptor;
import com.guoyu.platform.ai.service.ObjectiveAnswerCheckAiService;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.answercheck.dto.ObjectiveAnswerItemFilter;
import com.guoyu.platform.exam.answercheck.dto.ObjectiveAnswerScope;
import com.guoyu.platform.exam.answercheck.dto.ObjectiveAnswerScopeQuery;
import com.guoyu.platform.exam.answercheck.dto.ObjectiveAnswerScopeRow;
import com.guoyu.platform.exam.answercheck.dto.ObjectiveAnswerSelectionReq;
import com.guoyu.platform.exam.answercheck.dto.WorkTypeOptionRow;
import com.guoyu.platform.exam.answercheck.entity.ObjectiveAnswerCheckBatchEntity;
import com.guoyu.platform.exam.answercheck.entity.ObjectiveAnswerCheckCallLogEntity;
import com.guoyu.platform.exam.answercheck.entity.ObjectiveAnswerCheckItemEntity;
import com.guoyu.platform.exam.answercheck.mapper.ObjectiveAnswerCheckBatchMapper;
import com.guoyu.platform.exam.answercheck.mapper.ObjectiveAnswerCheckCallLogMapper;
import com.guoyu.platform.exam.answercheck.mapper.ObjectiveAnswerCheckItemMapper;
import com.guoyu.platform.exam.answercheck.mapper.ObjectiveAnswerCheckScopeMapper;
import com.guoyu.platform.exam.answercheck.parser.NormalizedOption;
import com.guoyu.platform.exam.answercheck.parser.ObjectiveQuestionParser;
import com.guoyu.platform.exam.answercheck.parser.ObjectiveQuestionSource;
import com.guoyu.platform.exam.answercheck.parser.ParseOutcome;
import com.guoyu.platform.exam.answercheck.vo.ObjectiveAnswerBatchVO;
import com.guoyu.platform.exam.answercheck.vo.ObjectiveAnswerFilterOptionsVO;
import com.guoyu.platform.exam.answercheck.vo.ObjectiveAnswerItemDetailVO;
import com.guoyu.platform.exam.answercheck.vo.ObjectiveAnswerItemVO;
import com.guoyu.platform.exam.answercheck.vo.ObjectiveAnswerPreviewVO;
import com.guoyu.platform.exam.answercheck.vo.ObjectiveAnswerQuestionPageVO;
import com.guoyu.platform.exam.answercheck.vo.ObjectiveAnswerReviewReq;
import com.guoyu.platform.exam.entity.ExamExamDetailEntity;
import com.guoyu.platform.exam.mapper.ExamExamDetailMapper;
import com.guoyu.platform.exam.support.QuestionContentQualityInspector;
import com.guoyu.platform.exam.support.QuestionContentQualityWarning;
import com.guoyu.platform.exam.vo.ExportResultDTO;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 管理端应用服务（方案 §5 / §13）：筛选项、试题分页、预览、批次与明细查询、人工复核、导出编排。
 *
 * <p>本类不写 SQL，不做结果判定；所有写操作只落在检查结果表，绝不修改 exam_exam_detail。
 */
@Slf4j
@Service
public class ObjectiveAnswerCheckApplicationService {

  private static final List<String> TYPES = List.of("single", "multi", "judge");
  private static final Set<String> REVIEW_STATUSES = Set.of("confirmed_correct", "needs_edit",
      "deferred", "unreviewed");
  private static final Set<String> REVIEW_RESULT_CODES = Set.of("mismatch_high", "low_confidence",
      "invalid", "failed");

  private final ObjectiveAnswerCheckScopeMapper scopeMapper;
  private final ObjectiveAnswerCheckBatchMapper batchMapper;
  private final ObjectiveAnswerCheckItemMapper itemMapper;
  private final ObjectiveAnswerCheckCallLogMapper callLogMapper;
  private final ObjectiveAnswerCheckSnapshotService snapshotService;
  private final ObjectiveAnswerCheckBatchService batchService;
  private final ObjectiveAnswerCheckExportService exportService;
  private final ObjectiveAnswerCheckAiService aiService;
  private final ObjectiveQuestionParser parser;
  private final ExamExamDetailMapper detailMapper;
  private final UserMapper userMapper;
  private final ObjectMapper objectMapper;

  public ObjectiveAnswerCheckApplicationService(ObjectiveAnswerCheckScopeMapper scopeMapper,
      ObjectiveAnswerCheckBatchMapper batchMapper, ObjectiveAnswerCheckItemMapper itemMapper,
      ObjectiveAnswerCheckCallLogMapper callLogMapper,
      ObjectiveAnswerCheckSnapshotService snapshotService,
      ObjectiveAnswerCheckBatchService batchService,
      ObjectiveAnswerCheckExportService exportService,
      ObjectiveAnswerCheckAiService aiService, ObjectiveQuestionParser parser,
      ExamExamDetailMapper detailMapper, UserMapper userMapper, ObjectMapper objectMapper) {
    this.scopeMapper = scopeMapper;
    this.batchMapper = batchMapper;
    this.itemMapper = itemMapper;
    this.callLogMapper = callLogMapper;
    this.snapshotService = snapshotService;
    this.batchService = batchService;
    this.exportService = exportService;
    this.aiService = aiService;
    this.parser = parser;
    this.detailMapper = detailMapper;
    this.userMapper = userMapper;
    this.objectMapper = objectMapper;
  }

  // ────────────── 筛选项 ──────────────

  public ObjectiveAnswerFilterOptionsVO filterOptions(Integer workTypeId) {
    List<WorkTypeOptionRow> all = scopeMapper.listAllWorkTypes();
    Map<Integer, WorkTypeOptionRow> byId = all.stream()
        .collect(Collectors.toMap(WorkTypeOptionRow::getId, Function.identity(), (a, b) -> a,
            LinkedHashMap::new));
    List<ObjectiveAnswerFilterOptionsVO.WorkTypeOption> workTypes =
        scopeMapper.listWorkTypesWithQuestions().stream()
            .map(row -> new ObjectiveAnswerFilterOptionsVO.WorkTypeOption(row.getId().longValue(),
                row.getTitle(), workTypePath(row, byId)))
            .toList();

    List<Integer> levelValues = scopeMapper.listLevels(workTypeId);
    List<ObjectiveAnswerFilterOptionsVO.LevelOption> levels = levelValues.stream()
        .filter(java.util.Objects::nonNull)
        .map(level -> 5 - level)
        .distinct()
        .sorted((a, b) -> Integer.compare(b, a))
        .map(code -> new ObjectiveAnswerFilterOptionsVO.LevelOption(code, levelLabel(code)))
        .toList();

    List<ObjectiveAnswerFilterOptionsVO.SourceOption> sources = List.of(
        new ObjectiveAnswerFilterOptionsVO.SourceOption(0, "内部"),
        new ObjectiveAnswerFilterOptionsVO.SourceOption(1, "外部"),
        new ObjectiveAnswerFilterOptionsVO.SourceOption(2, "AI 生成"),
        new ObjectiveAnswerFilterOptionsVO.SourceOption(3, "赛前复习"));
    List<ObjectiveAnswerFilterOptionsVO.TypeOption> types = List.of(
        new ObjectiveAnswerFilterOptionsVO.TypeOption("single", "单选题"),
        new ObjectiveAnswerFilterOptionsVO.TypeOption("multi", "多选题"),
        new ObjectiveAnswerFilterOptionsVO.TypeOption("judge", "判断题"));
    return new ObjectiveAnswerFilterOptionsVO(workTypes, levels, sources, types);
  }

  private String workTypePath(WorkTypeOptionRow row, Map<Integer, WorkTypeOptionRow> byId) {
    List<String> chain = new ArrayList<>();
    WorkTypeOptionRow current = row;
    int guard = 0;
    while (current != null && guard++ < 20) {
      if (StringUtils.hasText(current.getTitle())) {
        chain.add(current.getTitle());
      }
      Integer parentId = current.getParentId();
      if (parentId == null || parentId <= 0) {
        break;
      }
      current = byId.get(parentId);
    }
    java.util.Collections.reverse(chain);
    return String.join(" / ", chain);
  }

  // ────────────── 试题分页 ──────────────

  public ObjectiveAnswerQuestionPageVO pageQuestions(ObjectiveAnswerScope scope, long page,
      long pageSize) {
    long safePage = Math.max(1, page);
    long safeSize = pageSize == 20 || pageSize == 50 || pageSize == 100 ? pageSize : 20;
    ObjectiveAnswerScope normalized = normalizeScope(scope);
    ObjectiveAnswerScopeQuery query = toQuery(normalized);
    Page<ObjectiveAnswerScopeRow> mpPage = new Page<>(safePage, safeSize);
    var result = scopeMapper.pageQuestions(mpPage, query);
    List<ObjectiveAnswerQuestionPageVO.QuestionRow> rows = result.getRecords().stream()
        .map(this::toQuestionRow).toList();
    return new ObjectiveAnswerQuestionPageVO(rows, safePage, safeSize, result.getTotal());
  }

  private ObjectiveAnswerQuestionPageVO.QuestionRow toQuestionRow(ObjectiveAnswerScopeRow row) {
    ParseOutcome outcome = parser.parse(new ObjectiveQuestionSource(row.getDetailId(),
        row.getQuestionType(), row.getContent(), row.getAnswer(), row.getAudio(), row.getVideo(),
        null), 12000, 2000);
    List<QuestionContentQualityWarning> warnings = new ArrayList<>(
        QuestionContentQualityInspector.inspect(parser.toPlainText(row.getContent()), "stem", null));
    if (outcome.valid()) {
      for (NormalizedOption option : outcome.question().options()) {
        warnings.addAll(
            QuestionContentQualityInspector.inspect(option.text(), "option", option.optionId()));
      }
    }
    String quality = QuestionContentQualityInspector.summarizeSeverity(warnings);
    String invalidReason = outcome.valid() ? null : outcome.invalidReason();
    if (invalidReason == null && "invalid".equals(quality)) {
      invalidReason = ObjectiveAnswerCheckSnapshotService.CONTENT_QUALITY_INVALID;
    }
    String stem = outcome.valid() ? outcome.question().stemText()
        : parser.toPlainText(row.getContent());
    return new ObjectiveAnswerQuestionPageVO.QuestionRow(row.getDetailId(), row.getQuestionType(),
        typeLabel(row.getQuestionType()), preview(stem), originalAnswerText(row, outcome),
        row.getSource(), sourceLabel(row.getSource()), row.getWorkTypeId() == null ? null
            : row.getWorkTypeId().longValue(),
        row.getWorkTypeTitle(), row.getCourseLevel() == null ? null : 5 - row.getCourseLevel(),
        row.getCourseLevel() == null ? null : levelLabel(5 - row.getCourseLevel()),
        row.getCourseId(), row.getCourseTitle(), invalidReason, quality);
  }

  /** 分页表“原答案”列：字母 + 选项文本；判断题显示正确/错误；解析失败显示 “-”。 */
  private String originalAnswerText(ObjectiveAnswerScopeRow row, ParseOutcome outcome) {
    if (!outcome.valid()) {
      return "-";
    }
    List<String> parts = new ArrayList<>();
    for (NormalizedOption option : outcome.question().options()) {
      if (option.originalCorrect()) {
        parts.add("judge".equals(row.getQuestionType()) ? option.text()
            : option.label() + ". " + option.text());
      }
    }
    return parts.isEmpty() ? "-" : String.join("；", parts);
  }

  // ────────────── 预览 / 创建 ──────────────

  public ObjectiveAnswerPreviewVO preview(ObjectiveAnswerSelectionReq req) {
    return snapshotService.preview(req);
  }

  /**
   * 创建批次：快照事务提交后才投递编排，避免事务内写库后立即派发造成抢跑。
   */
  public Long create(ObjectiveAnswerSelectionReq req, Long tenantId, Long userId) {
    Long batchId = snapshotService.createBatch(req, tenantId, userId);
    batchService.dispatch(batchId, tenantId, userId);
    return batchId;
  }

  public void pause(Long batchId) {
    batchService.pause(batchId);
  }

  public void resume(Long batchId, Long tenantId, Long userId) {
    batchService.resume(batchId, tenantId, userId);
    batchService.dispatch(batchId, tenantId, userId);
  }

  public int retryFailed(Long batchId, Long tenantId, Long userId) {
    int reset = batchService.retryFailed(batchId, tenantId, userId);
    batchService.dispatch(batchId, tenantId, userId);
    return reset;
  }

  // ────────────── 批次查询 ──────────────

  public ObjectiveAnswerBatchVO current(Long tenantId) {
    ObjectiveAnswerCheckBatchEntity batch = batchMapper.findActive(tenantId);
    return batch == null ? null : toBatchVO(batch);
  }

  public PageResponse<ObjectiveAnswerBatchVO> pageBatches(long page, long pageSize) {
    long safePage = Math.max(1, page);
    long safeSize = pageSize <= 0 || pageSize > 100 ? 10 : pageSize;
    Page<ObjectiveAnswerCheckBatchEntity> mpPage = new Page<>(safePage, safeSize);
    var result = batchMapper.selectPage(mpPage,
        new LambdaQueryWrapper<ObjectiveAnswerCheckBatchEntity>()
            .orderByDesc(ObjectiveAnswerCheckBatchEntity::getId));
    List<ObjectiveAnswerBatchVO> items = result.getRecords().stream().map(this::toBatchVO).toList();
    return new PageResponse<>(items, safePage, safeSize, result.getTotal());
  }

  public ObjectiveAnswerBatchVO getBatch(Long batchId) {
    return toBatchVO(batchService.requireBatch(batchId));
  }

  private ObjectiveAnswerBatchVO toBatchVO(ObjectiveAnswerCheckBatchEntity batch) {
    int pending = itemMapper.countByProcessStatus(batch.getId(), "pending");
    int processing = itemMapper.countByProcessStatus(batch.getId(), "processing");
    int retryableFailed = countRetryableFailed(batch.getId());
    boolean canPause = "pending".equals(batch.getStatus()) || "running".equals(batch.getStatus());
    boolean canResume = "paused".equals(batch.getStatus())
        || ("failed".equals(batch.getStatus()) && processing == 0 && pending > 0);
    boolean canRetryFailed = ("paused".equals(batch.getStatus()) || "done".equals(batch.getStatus())
        || ("failed".equals(batch.getStatus()) && processing == 0)) && retryableFailed > 0;
    return new ObjectiveAnswerBatchVO(batch.getId(), batch.getStatus(), batch.getSelectionMode(),
        filterSummary(batch.getFilterJson()), nz(batch.getTotal()), nz(batch.getDone()), pending,
        processing, nz(batch.getConsistentHigh()), nz(batch.getMismatchHigh()),
        nz(batch.getLowConfidence()), nz(batch.getInvalid()), nz(batch.getFailed()),
        nz(batch.getContentSuspect()), nz(batch.getContentInvalid()), nz(batch.getExpectedCalls()),
        nz(batch.getCallCount()), nz(batch.getInputTokensExact()),
        nz(batch.getInputTokensEstimated()), nz(batch.getUsageEstimatedCalls()),
        nz(batch.getOutputTokens()), batch.getModelId(), batch.getJevBaseUrl(),
        batch.getJevConcurrency(), batch.getJevReadTimeoutSeconds(), batch.getPromptVersion(),
        batch.getConfidenceThreshold(), batch.getMarginThreshold(), batch.getCreatedBy(),
        displayName(batch.getCreatedBy()), batch.getCreatedAt(), batch.getStartedAt(),
        batch.getFinishedAt(), batch.getFailReason(), canPause, canResume, canRetryFailed);
  }

  private int countRetryableFailed(Long batchId) {
    return Math.toIntExact(itemMapper.selectCount(
        new LambdaQueryWrapper<ObjectiveAnswerCheckItemEntity>()
            .eq(ObjectiveAnswerCheckItemEntity::getBatchId, batchId)
            .eq(ObjectiveAnswerCheckItemEntity::getProcessStatus, "failed")
            .eq(ObjectiveAnswerCheckItemEntity::getRetryable, 1)));
  }

  private String displayName(Long userId) {
    if (userId == null) {
      return null;
    }
    UserEntity user = userMapper.selectById(userId);
    return user == null ? null : user.getDisplayName();
  }

  private String filterSummary(String filterJson) {
    if (!StringUtils.hasText(filterJson)) {
      return "-";
    }
    try {
      Map<String, Object> map = objectMapper.readValue(filterJson, new TypeReference<>() {
      });
      List<String> parts = new ArrayList<>();
      if (map.get("workTypeTitle") != null) {
        parts.add(String.valueOf(map.get("workTypeTitle")));
      }
      Object levelCode = map.get("levelCode");
      if (levelCode instanceof Number number) {
        parts.add(levelLabel(number.intValue()));
      }
      Object source = map.get("source");
      if (source instanceof Number number) {
        parts.add(sourceLabel(number.intValue()));
      }
      Object types = map.get("types");
      if (types instanceof List<?> list && !list.isEmpty()) {
        parts.add(list.size() >= 3 ? "三种题型"
            : list.stream().map(t -> typeLabel(String.valueOf(t))).collect(Collectors.joining("、")));
      }
      return parts.isEmpty() ? "-" : String.join(" / ", parts);
    } catch (Exception e) {
      return "-";
    }
  }

  // ────────────── 结果明细 ──────────────

  public PageResponse<ObjectiveAnswerItemVO> pageItems(Long batchId, ObjectiveAnswerItemFilter filter,
      long page, long pageSize) {
    batchService.requireBatch(batchId);
    long safePage = Math.max(1, page);
    long safeSize = pageSize <= 0 || pageSize > 200 ? 20 : pageSize;
    LambdaQueryWrapper<ObjectiveAnswerCheckItemEntity> wrapper = itemWrapper(batchId, filter);
    if (Boolean.TRUE.equals(filter == null ? null : filter.getNeedsReview())) {
      wrapper.last("ORDER BY FIELD(result_code,'mismatch_high','low_confidence','invalid','failed',"
          + "'consistent_high'), FIELD(content_quality,'invalid','suspect','clean'), "
          + "ai_confidence DESC, id ASC");
    } else {
      wrapper.orderByAsc(ObjectiveAnswerCheckItemEntity::getId);
    }
    Page<ObjectiveAnswerCheckItemEntity> mpPage = new Page<>(safePage, safeSize);
    var result = itemMapper.selectPage(mpPage, wrapper);
    List<ObjectiveAnswerItemVO> items = result.getRecords().stream().map(this::toItemVO).toList();
    return new PageResponse<>(items, safePage, safeSize, result.getTotal());
  }

  List<ObjectiveAnswerCheckItemEntity> listItems(Long batchId, ObjectiveAnswerItemFilter filter) {
    return itemMapper.selectList(itemWrapper(batchId, filter)
        .orderByAsc(ObjectiveAnswerCheckItemEntity::getId));
  }

  private LambdaQueryWrapper<ObjectiveAnswerCheckItemEntity> itemWrapper(Long batchId,
      ObjectiveAnswerItemFilter filter) {
    LambdaQueryWrapper<ObjectiveAnswerCheckItemEntity> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(ObjectiveAnswerCheckItemEntity::getBatchId, batchId);
    if (filter != null) {
      if (StringUtils.hasText(filter.getResultCode())) {
        wrapper.eq(ObjectiveAnswerCheckItemEntity::getResultCode, filter.getResultCode());
      }
      if (StringUtils.hasText(filter.getContentQuality())) {
        if ("not_clean".equals(filter.getContentQuality())) {
          wrapper.ne(ObjectiveAnswerCheckItemEntity::getContentQuality, "clean");
        } else {
          wrapper.eq(ObjectiveAnswerCheckItemEntity::getContentQuality,
              filter.getContentQuality());
        }
      }
      if (StringUtils.hasText(filter.getReviewStatus())) {
        wrapper.eq(ObjectiveAnswerCheckItemEntity::getReviewStatus, filter.getReviewStatus());
      }
      if (StringUtils.hasText(filter.getQuestionType())) {
        wrapper.eq(ObjectiveAnswerCheckItemEntity::getQuestionType, filter.getQuestionType());
      }
      if (StringUtils.hasText(filter.getKeyword())) {
        wrapper.like(ObjectiveAnswerCheckItemEntity::getStemText, filter.getKeyword().trim());
      }
      if (Boolean.TRUE.equals(filter.getNeedsReview())) {
        wrapper.isNotNull(ObjectiveAnswerCheckItemEntity::getResultCode)
            .eq(ObjectiveAnswerCheckItemEntity::getReviewStatus, "unreviewed")
            .and(w -> w.in(ObjectiveAnswerCheckItemEntity::getResultCode, REVIEW_RESULT_CODES)
                .or().ne(ObjectiveAnswerCheckItemEntity::getContentQuality, "clean"));
      }
    }
    return wrapper;
  }

  public ObjectiveAnswerItemDetailVO getItem(Long batchId, Long itemId) {
    ObjectiveAnswerCheckItemEntity item = requireItem(batchId, itemId);
    ObjectiveAnswerCheckBatchEntity batch = batchService.requireBatch(batchId);
    List<ObjectiveAnswerCheckCallLogEntity> calls = callLogMapper.listByItem(itemId);
    List<ObjectiveAnswerItemDetailVO.OptionLine> options = optionLines(item);
    boolean sourceChanged = sourceChanged(item);
    String requestedModel = calls.stream()
        .map(ObjectiveAnswerCheckCallLogEntity::getRequestedModel).filter(StringUtils::hasText)
        .findFirst().orElse(batch.getModelId());
    String responseModel = calls.stream()
        .map(ObjectiveAnswerCheckCallLogEntity::getResponseModel).filter(StringUtils::hasText)
        .findFirst().orElse(null);
    String confidenceDefinition = calls.stream()
        .map(ObjectiveAnswerCheckCallLogEntity::getConfidenceDefinition)
        .filter(StringUtils::hasText).findFirst().orElse(null);
    List<ObjectiveAnswerItemDetailVO.CallLog> callVOs = calls.stream()
        .map(c -> new ObjectiveAnswerItemDetailVO.CallLog(c.getCallId(), nz(c.getChunkIndex()),
            nz(c.getChunkCount()), c.getStatus(), c.getRequestedModel(), c.getResponseModel(),
            c.getConfidenceDefinition(), c.getInputTokens(), c.getOutputTokens(),
            nz(c.getUsageEstimated()) == 1, c.getDurationMs(), c.getErrorCode(),
            c.getErrorMessage()))
        .toList();
    return new ObjectiveAnswerItemDetailVO(toItemVO(item), sourceLabel(item.getSource()),
        item.getWorkTypeTitle(), item.getCourseTitle(), item.getLevelCode(),
        item.getKnowledgePath(), item.getContentSnapshot(), item.getStemText(),
        item.getResolveSnapshot(), options, callVOs, sourceChanged, batch.getModelId(),
        batch.getPromptVersion(), requestedModel, responseModel, confidenceDefinition,
        item.getErrorMessage());
  }

  private ObjectiveAnswerCheckItemEntity requireItem(Long batchId, Long itemId) {
    ObjectiveAnswerCheckItemEntity item = itemMapper.findById(itemId);
    if (item == null || !batchId.equals(item.getBatchId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "检查记录不存在");
    }
    return item;
  }

  private boolean sourceChanged(ObjectiveAnswerCheckItemEntity item) {
    ExamExamDetailEntity current = detailMapper.selectById(item.getDetailId());
    if (current == null) {
      return true;
    }
    String hash = parser.sourceHash(current.getType(), current.getContent(), current.getAnswer());
    return !hash.equals(item.getSourceHash());
  }

  private List<ObjectiveAnswerItemDetailVO.OptionLine> optionLines(
      ObjectiveAnswerCheckItemEntity item) {
    if (!StringUtils.hasText(item.getOptionResultsJson())) {
      return List.of();
    }
    try {
      ObjectiveAnswerCheckResult.OptionResults result = objectMapper.readValue(
          item.getOptionResultsJson(), ObjectiveAnswerCheckResult.OptionResults.class);
      if (result.options() == null) {
        return List.of();
      }
      return result.options().stream()
          .map(o -> new ObjectiveAnswerItemDetailVO.OptionLine(o.optionId(), o.label(), o.text(),
              o.originalCorrect(), o.aiCorrect(), o.choice(), o.confidence(),
              o.referenceProbability(), o.top1Probability(), o.top2Probability(), o.margin()))
          .toList();
    } catch (Exception e) {
      log.warn("[OAC] 选项结果解析失败 itemId={}: {}", item.getId(), e.getMessage());
      return List.of();
    }
  }

  private ObjectiveAnswerItemVO toItemVO(ObjectiveAnswerCheckItemEntity item) {
    List<QuestionContentQualityWarning> warnings = new ArrayList<>();
    if (StringUtils.hasText(item.getContentWarningsJson())) {
      try {
        warnings = objectMapper.readValue(item.getContentWarningsJson(),
            new TypeReference<List<QuestionContentQualityWarning>>() {
            });
      } catch (Exception e) {
        log.warn("[OAC] 内容警告解析失败 itemId={}: {}", item.getId(), e.getMessage());
      }
    }
    return new ObjectiveAnswerItemVO(item.getId(), item.getBatchId(), item.getDetailId(),
        item.getQuestionType(), typeLabel(item.getQuestionType()), preview(item.getStemText()),
        answerText(item, item.getOriginalAnswerKeys()), answerText(item, item.getAiAnswerKeys()),
        item.getResultCode(), item.getContentQuality(), warnings, needsReview(item),
        item.getConsistent() == null ? null : item.getConsistent() == 1,
        item.getReferenceConfidence(), item.getAiConfidence(), item.getMinMargin(),
        item.getDecisionWarning(), nz(item.getRetryable()) == 1, nz(item.getAttempts()),
        nz(item.getVersion()), item.getReviewStatus(), item.getReviewNote(), item.getCheckedAt(),
        item.getUpdatedAt());
  }

  private boolean needsReview(ObjectiveAnswerCheckItemEntity item) {
    if (item.getResultCode() == null || !"unreviewed".equals(item.getReviewStatus())) {
      return false;
    }
    return REVIEW_RESULT_CODES.contains(item.getResultCode())
        || !"clean".equals(item.getContentQuality());
  }

  /** 答案展示：字母 + 选项文本；判断题显示正确/错误；无关联时返回 “-”。 */
  private String answerText(ObjectiveAnswerCheckItemEntity item, String keys) {
    if (!StringUtils.hasText(keys)) {
      return "-";
    }
    Set<String> keySet = new LinkedHashSet<>(List.of(keys.split(",")));
    List<Map<String, Object>> options = snapshotOptions(item);
    List<String> parts = new ArrayList<>();
    for (String key : keySet) {
      for (Map<String, Object> option : options) {
        if (key.equals(String.valueOf(option.get("optionId")))) {
          String label = String.valueOf(option.get("label"));
          String text = String.valueOf(option.get("text"));
          if ("judge".equals(item.getQuestionType())) {
            parts.add(text);
          } else {
            parts.add(label + ". " + text);
          }
          break;
        }
      }
    }
    return parts.isEmpty() ? "-" : String.join("；", parts);
  }

  private List<Map<String, Object>> snapshotOptions(ObjectiveAnswerCheckItemEntity item) {
    try {
      return objectMapper.readValue(item.getOptionSnapshotJson(), new TypeReference<>() {
      });
    } catch (Exception e) {
      return List.of();
    }
  }

  // ────────────── 人工复核 ──────────────

  @Transactional
  public void review(Long batchId, Long itemId, ObjectiveAnswerReviewReq req, Long userId) {
    String status = req.reviewStatus() == null ? "" : req.reviewStatus().trim();
    if (!REVIEW_STATUSES.contains(status)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "复核状态不正确");
    }
    requireItem(batchId, itemId);
    int updated = itemMapper.updateReview(itemId, batchId, req.expectedVersion(), status,
        req.note(), userId);
    if (updated == 0) {
      throw new BusinessException(ErrorCode.CONFLICT, "该记录已被其他人更新，请刷新后重试");
    }
  }

  // ────────────── 导出 ──────────────

  public ExportResultDTO export(Long batchId, ObjectiveAnswerItemFilter filter) {
    ObjectiveAnswerCheckBatchEntity batch = batchService.requireBatch(batchId);
    return exportService.export(batch, listItems(batchId, filter));
  }

  // ────────────── 工具 ──────────────

  private ObjectiveAnswerScope normalizeScope(ObjectiveAnswerScope scope) {
    ObjectiveAnswerScope result = scope == null ? ObjectiveAnswerScope.empty() : scope;
    if (result.getTypes() == null || result.getTypes().isEmpty()) {
      result.setTypes(TYPES);
    }
    return result;
  }

  private ObjectiveAnswerScopeQuery toQuery(ObjectiveAnswerScope scope) {
    ObjectiveAnswerScopeQuery query = new ObjectiveAnswerScopeQuery();
    query.setWorkTypeId(scope.getWorkTypeId());
    query.setLevelCode(scope.getLevelCode());
    query.setSource(scope.getSource());
    query.setTypes(scope.getTypes());
    return query;
  }

  static String typeLabel(String type) {
    if (type == null) {
      return "-";
    }
    return switch (type) {
      case "single" -> "单选题";
      case "multi" -> "多选题";
      case "judge" -> "判断题";
      default -> type;
    };
  }

  static String sourceLabel(Integer source) {
    if (source == null) {
      return "-";
    }
    return switch (source) {
      case 0 -> "内部";
      case 1 -> "外部";
      case 2 -> "AI 生成";
      case 3 -> "赛前复习";
      default -> String.valueOf(source);
    };
  }

  static String levelLabel(int levelCode) {
    return switch (levelCode) {
      case 5 -> "五级/初级工";
      case 4 -> "四级/中级工";
      case 3 -> "三级/高级工";
      case 2 -> "二级/技师";
      case 1 -> "一级/高级技师";
      default -> String.valueOf(levelCode);
    };
  }

  private String preview(String text) {
    if (text == null) {
      return "";
    }
    String flat = text.replaceAll("\\s+", " ").trim();
    return flat.length() <= 120 ? flat : flat.substring(0, 120);
  }

  private int nz(Integer value) {
    return value == null ? 0 : value;
  }

  private long nz(Long value) {
    return value == null ? 0L : value;
  }
}
