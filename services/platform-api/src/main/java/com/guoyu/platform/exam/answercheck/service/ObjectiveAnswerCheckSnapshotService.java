package com.guoyu.platform.exam.answercheck.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.ai.dto.ObjectiveAnswerCheckAiDescriptor;
import com.guoyu.platform.ai.service.ObjectiveAnswerCheckAiService;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.answercheck.config.ObjectiveAnswerCheckProperties;
import com.guoyu.platform.exam.answercheck.dto.KnowledgeNodeRow;
import com.guoyu.platform.exam.answercheck.dto.ObjectiveAnswerScope;
import com.guoyu.platform.exam.answercheck.dto.ObjectiveAnswerScopeQuery;
import com.guoyu.platform.exam.answercheck.dto.ObjectiveAnswerScopeRow;
import com.guoyu.platform.exam.answercheck.dto.ObjectiveAnswerSelectionReq;
import com.guoyu.platform.exam.answercheck.entity.ObjectiveAnswerCheckBatchEntity;
import com.guoyu.platform.exam.answercheck.entity.ObjectiveAnswerCheckItemEntity;
import com.guoyu.platform.exam.answercheck.mapper.ObjectiveAnswerCheckBatchMapper;
import com.guoyu.platform.exam.answercheck.mapper.ObjectiveAnswerCheckItemMapper;
import com.guoyu.platform.exam.answercheck.mapper.ObjectiveAnswerCheckScopeMapper;
import com.guoyu.platform.exam.answercheck.parser.NormalizedObjectiveQuestion;
import com.guoyu.platform.exam.answercheck.parser.NormalizedOption;
import com.guoyu.platform.exam.answercheck.parser.ObjectiveQuestionParser;
import com.guoyu.platform.exam.answercheck.parser.ObjectiveQuestionSource;
import com.guoyu.platform.exam.answercheck.parser.ParseOutcome;
import com.guoyu.platform.exam.answercheck.vo.ObjectiveAnswerPreviewVO;
import com.guoyu.platform.exam.support.QuestionContentQualityInspector;
import com.guoyu.platform.exam.support.QuestionContentQualityWarning;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 预览与批次快照（方案 §5.4 / §7.3 / §8）。
 *
 * <p>预览和创建共用同一套解析、预检、内容质量与预计调用计算，区别只在超限处理：
 * 预览 200 + overLimit，创建 413。
 */
@Slf4j
@Service
public class ObjectiveAnswerCheckSnapshotService {

  public static final String PROMPT_VERSION = "objective-answer-v1";
  public static final String CONTENT_QUALITY_INVALID = "CONTENT_QUALITY_INVALID";
  private static final int INSERT_BATCH_SIZE = 200;

  private final ObjectiveAnswerCheckScopeMapper scopeMapper;
  private final ObjectiveAnswerCheckBatchMapper batchMapper;
  private final ObjectiveAnswerCheckItemMapper itemMapper;
  private final ObjectiveQuestionParser parser;
  private final ObjectiveAnswerCheckProperties properties;
  private final ObjectiveAnswerCheckAiService aiService;
  private final ObjectMapper objectMapper;

  public ObjectiveAnswerCheckSnapshotService(ObjectiveAnswerCheckScopeMapper scopeMapper,
      ObjectiveAnswerCheckBatchMapper batchMapper, ObjectiveAnswerCheckItemMapper itemMapper,
      ObjectiveQuestionParser parser, ObjectiveAnswerCheckProperties properties,
      ObjectiveAnswerCheckAiService aiService, ObjectMapper objectMapper) {
    this.scopeMapper = scopeMapper;
    this.batchMapper = batchMapper;
    this.itemMapper = itemMapper;
    this.parser = parser;
    this.properties = properties;
    this.aiService = aiService;
    this.objectMapper = objectMapper;
  }

  /** 预览：超限返回 overLimit 而不是异常。 */
  public ObjectiveAnswerPreviewVO preview(ObjectiveAnswerSelectionReq req) {
    String modelId = aiService.descriptor().modelId();
    Plan plan = buildPlan(req, false);
    if (plan.overLimit()) {
      return ObjectiveAnswerPreviewVO.overLimit(plan.selectionCount(),
          properties.effectiveMaxBatchSize(), modelId);
    }
    return new ObjectiveAnswerPreviewVO(plan.selectionCount(), false,
        properties.effectiveMaxBatchSize(), plan.validCount(), plan.invalidCount(),
        plan.suspectContentCount(), plan.invalidContentCount(), plan.expectedCalls(),
        plan.typeCounts(), plan.invalidReasonCounts(), modelId,
        BigDecimal.valueOf(properties.getConfidenceThreshold()),
        BigDecimal.valueOf(properties.getMarginThreshold()));
  }

  /**
   * 创建批次与快照（短事务）：批次和明细必须先提交可见，再由调用方投递编排（方案 §3）。
   */
  @Transactional
  public Long createBatch(ObjectiveAnswerSelectionReq req, Long tenantId, Long userId) {
    if (!properties.isEnabled() || !aiService.descriptor().credentialsReady()) {
      throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE,
          "答案 AI 检查暂未开通，请联系管理员。");
    }
    Plan plan = buildPlan(req, true);
    if (plan.rows().isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "没有符合条件的试题，请调整筛选条件");
    }

    ObjectiveAnswerCheckBatchEntity active = batchMapper.findActive(tenantId);
    if (active != null) {
      throw new BusinessException(ErrorCode.CONFLICT,
          "已有正在执行的检查批次，请等待完成或先暂停");
    }

    long batchId = IdWorker.getId();
    long runId = IdWorker.getId();
    ObjectiveAnswerCheckBatchEntity batch = new ObjectiveAnswerCheckBatchEntity();
    batch.setId(batchId);
    batch.setTenantId(tenantId);
    batch.setActiveTenantId(tenantId);
    batch.setRunId(runId);
    batch.setOwnerId(ownerId());
    batch.setLeaseUntil(LocalDateTime.now().plusSeconds(60));
    batch.setSelectionMode(plan.selectionMode());
    batch.setFilterJson(plan.filterJson());
    batch.setStatus("pending");
    batch.setTotal(plan.rows().size());
    // invalid 行创建即已是终态，done 从 invalid 起算（done=success+invalid+failed）
    batch.setDone(plan.invalidCount());
    batch.setFailed(0);
    batch.setInvalid(plan.invalidCount());
    batch.setConsistentHigh(0);
    batch.setMismatchHigh(0);
    batch.setLowConfidence(0);
    batch.setContentSuspect(plan.suspectContentCount());
    batch.setContentInvalid(plan.invalidContentCount());
    batch.setExpectedCalls(plan.expectedCalls());
    batch.setCallCount(0);
    batch.setInputTokensExact(0L);
    batch.setInputTokensEstimated(0L);
    batch.setOutputTokens(0L);
    batch.setUsageEstimatedCalls(0);
    batch.setModelDurationMs(0L);
    batch.setModelId(aiService.descriptor().modelId());
    batch.setPromptVersion(PROMPT_VERSION);
    batch.setConfidenceThreshold(BigDecimal.valueOf(properties.getConfidenceThreshold()));
    batch.setMarginThreshold(BigDecimal.valueOf(properties.getMarginThreshold()));
    batch.setCreatedBy(userId);
    batch.setUpdatedBy(userId);
    batch.setDeleted(0);
    try {
      batchMapper.insert(batch);
    } catch (DuplicateKeyException e) {
      // active_tenant_id 唯一索引：并发创建时只有一个成功
      throw new BusinessException(ErrorCode.CONFLICT,
          "已有正在执行的检查批次，请等待完成或先暂停");
    }

    List<ObjectiveAnswerCheckItemEntity> buffer = new ArrayList<>(INSERT_BATCH_SIZE);
    for (SnapshotRow row : plan.rows()) {
      buffer.add(toEntity(batchId, tenantId, userId, row));
      if (buffer.size() >= INSERT_BATCH_SIZE) {
        itemMapper.insertBatch(List.copyOf(buffer), tenantId, userId);
        buffer.clear();
      }
    }
    if (!buffer.isEmpty()) {
      itemMapper.insertBatch(List.copyOf(buffer), tenantId, userId);
    }
    log.info("Objective answer check batch created: id={}, total={}, invalid={}, expectedCalls={}",
        batchId, plan.rows().size(), plan.invalidCount(), plan.expectedCalls());
    return batchId;
  }

  /** 编排派发所需的最小信息。 */
  public record BatchStart(Long batchId, Long tenantId, Long runId, String ownerId) {
  }

  public BatchStart startInfo(Long batchId, Long tenantId) {
    ObjectiveAnswerCheckBatchEntity batch = batchMapper.findById(batchId);
    if (batch == null || batch.getRunId() == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "批次不存在或已结束");
    }
    return new BatchStart(batchId, tenantId, batch.getRunId(), batch.getOwnerId());
  }

  private ObjectiveAnswerCheckItemEntity toEntity(long batchId, Long tenantId, Long userId,
      SnapshotRow row) {
    ObjectiveAnswerCheckItemEntity item = new ObjectiveAnswerCheckItemEntity();
    item.setId(IdWorker.getId());
    item.setTenantId(tenantId);
    item.setBatchId(batchId);
    item.setDetailId(row.detailId());
    item.setQuestionType(row.questionType());
    item.setSource(row.source());
    item.setWorkTypeId(row.workTypeId());
    item.setWorkTypeTitle(truncate(row.workTypeTitle(), 512));
    item.setLevelCode(row.courseLevel() == null ? null : 5 - row.courseLevel());
    item.setCourseId(row.courseId());
    item.setCourseTitle(truncate(row.courseTitle(), 512));
    item.setKnowledgeId(row.knowledgeId());
    item.setKnowledgePath(truncate(row.knowledgePath(), 1024));
    item.setContentSnapshot(row.content());
    item.setStemText(row.stemText() == null ? "" : row.stemText());
    item.setAnswerSnapshot(row.answer());
    item.setResolveSnapshot(row.resolveText());
    item.setOptionSnapshotJson(row.optionSnapshotJson());
    item.setSourceHash(row.sourceHash());
    item.setContentQuality(row.contentQuality());
    item.setContentWarningsJson(row.contentWarningsJson());
    item.setProcessStatus(row.processStatus());
    item.setResultCode(row.resultCode());
    item.setInvalidReason(row.invalidReason());
    item.setRetryable(0);
    item.setAttempts(0);
    item.setVersion(0);
    item.setReviewStatus("unreviewed");
    item.setCreatedBy(userId);
    item.setUpdatedBy(userId);
    item.setDeleted(0);
    return item;
  }

  private Plan buildPlan(ObjectiveAnswerSelectionReq req, boolean forCreate) {
    if (req == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择要检查的试题");
    }
    String mode = req.selectionMode();
    if (!ObjectiveAnswerSelectionReq.MODE_IDS.equals(mode)
        && !ObjectiveAnswerSelectionReq.MODE_FILTER.equals(mode)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "选择方式不正确");
    }
    ObjectiveAnswerScope scope = normalizeScope(req.scope());
    int max = properties.effectiveMaxBatchSize();
    List<Integer> ids = normalizeIds(req.detailIds());
    ObjectiveAnswerScopeQuery query = toQuery(scope);

    long selectionCount;
    List<ObjectiveAnswerScopeRow> rows;
    if (ObjectiveAnswerSelectionReq.MODE_IDS.equals(mode)) {
      if (ids.isEmpty()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "请至少选择一道试题");
      }
      if (ids.size() > max) {
        if (forCreate) {
          throw new BusinessException(ErrorCode.PAYLOAD_TOO_LARGE,
              "本次最多检查 " + max + " 道，请缩小筛选范围或改为逐题勾选。");
        }
        return Plan.overLimit(ids.size(), mode, "{}");
      }
      query.setDetailIds(ids);
      rows = scopeMapper.listQuestions(query, ids.size(), 0);
      if (rows.size() != ids.size()) {
        throw new BusinessException(ErrorCode.CONFLICT,
            "部分试题已删除、已变更或不在当前筛选范围内，请刷新后重试");
      }
      selectionCount = rows.size();
    } else {
      if (ids != null && !ids.isEmpty()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "按筛选选择时不能同时提交逐题清单");
      }
      selectionCount = scopeMapper.countQuestions(query);
      if (selectionCount == 0) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "没有符合条件的试题，请调整筛选条件");
      }
      if (selectionCount > max) {
        if (forCreate) {
          throw new BusinessException(ErrorCode.PAYLOAD_TOO_LARGE,
              "本次最多检查 " + max + " 道，请缩小筛选范围或改为逐题勾选。");
        }
        return Plan.overLimit((int) selectionCount, mode, "{}");
      }
      rows = scopeMapper.listQuestions(query, max, 0);
    }

    Map<Integer, String> knowledgePaths = knowledgePaths(rows);
    List<SnapshotRow> snapshotRows = new ArrayList<>();
    Map<String, Integer> typeCounts = new LinkedHashMap<>();
    Map<String, Integer> reasonCounts = new LinkedHashMap<>();
    int valid = 0;
    int invalid = 0;
    int suspect = 0;
    int contentInvalid = 0;
    int expectedCalls = 0;

    for (ObjectiveAnswerScopeRow row : rows) {
      String knowledgePath = knowledgePaths.get(row.getKnowledgeId());
      SnapshotRow snapshot = toSnapshotRow(row, knowledgePath);
      snapshotRows.add(snapshot);
      typeCounts.merge(row.getQuestionType(), 1, Integer::sum);
      if (snapshot.invalid()) {
        invalid++;
        reasonCounts.merge(snapshot.invalidReason(), 1, Integer::sum);
      } else {
        valid++;
        expectedCalls += "multi".equals(row.getQuestionType())
            ? (snapshot.optionCount() + 4) / 5 : 1;
      }
      if ("suspect".equals(snapshot.contentQuality())) {
        suspect++;
      }
      if ("invalid".equals(snapshot.contentQuality())) {
        contentInvalid++;
      }
    }

    List<ObjectiveAnswerPreviewVO.InvalidReasonCount> reasons = reasonCounts.entrySet().stream()
        .map(e -> new ObjectiveAnswerPreviewVO.InvalidReasonCount(e.getKey(), e.getValue()))
        .toList();
    return new Plan(false, (int) selectionCount, mode, filterJson(scope, rows), snapshotRows,
        valid, invalid, suspect, contentInvalid, expectedCalls, typeCounts, reasons);
  }

  private SnapshotRow toSnapshotRow(ObjectiveAnswerScopeRow row, String knowledgePath) {
    List<QuestionContentQualityWarning> warnings = new ArrayList<>();
    ParseOutcome outcome = parser.parse(new ObjectiveQuestionSource(row.getDetailId(),
        row.getQuestionType(), row.getContent(), row.getAnswer(), row.getAudio(), row.getVideo(),
        knowledgePath),
        properties.getMaxStemChars(), properties.getMaxOptionChars());
    warnings.addAll(QuestionContentQualityInspector.inspect(
        parser.toPlainText(row.getContent()), "stem", null));
    List<NormalizedOption> options = outcome.valid() ? outcome.question().options() : List.of();
    for (NormalizedOption option : options) {
      warnings.addAll(
          QuestionContentQualityInspector.inspect(option.text(), "option", option.optionId()));
    }
    String quality = QuestionContentQualityInspector.summarizeSeverity(warnings);
    String stemText = outcome.valid() ? outcome.question().stemText()
        : parser.toPlainText(row.getContent());

    String invalidReason = outcome.valid() ? null : outcome.invalidReason();
    if (invalidReason == null && "invalid".equals(quality)) {
      invalidReason = CONTENT_QUALITY_INVALID;
    }
    String processStatus = invalidReason == null ? "pending" : "invalid";
    String resultCode = invalidReason == null ? null : "invalid";

    return new SnapshotRow(row.getDetailId(), row.getQuestionType(), row.getSource(),
        row.getWorkTypeId(), row.getWorkTypeTitle(), row.getCourseId(), row.getCourseTitle(),
        row.getCourseLevel(), row.getKnowledgeId(), knowledgePath, row.getContent(), stemText,
        row.getAnswer(), row.getResolveText(), optionSnapshotJson(options),
        outcome.valid() ? outcome.question().sourceHash()
            : parser.sourceHash(row.getQuestionType(), row.getContent(), row.getAnswer()),
        quality, warningsJson(warnings), processStatus, resultCode, invalidReason,
        options.size());
  }

  private String optionSnapshotJson(List<NormalizedOption> options) {
    List<Map<String, Object>> list = new ArrayList<>();
    for (NormalizedOption option : options) {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("optionId", option.optionId());
      map.put("originalIndex", option.originalIndex());
      map.put("label", option.label());
      map.put("text", option.text());
      map.put("originalCorrect", option.originalCorrect());
      list.add(map);
    }
    try {
      return objectMapper.writeValueAsString(list);
    } catch (Exception e) {
      throw new IllegalStateException("序列化选项快照失败", e);
    }
  }

  private String warningsJson(List<QuestionContentQualityWarning> warnings) {
    if (warnings.isEmpty()) {
      return "[]";
    }
    try {
      return objectMapper.writeValueAsString(warnings);
    } catch (Exception e) {
      throw new IllegalStateException("序列化内容警告失败", e);
    }
  }

  private ObjectiveAnswerScope normalizeScope(ObjectiveAnswerScope scope) {
    ObjectiveAnswerScope normalized = scope == null ? ObjectiveAnswerScope.empty() : scope;
    if (normalized.getWorkTypeId() != null && normalized.getWorkTypeId() <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "工种参数不正确");
    }
    if (normalized.getLevelCode() != null
        && (normalized.getLevelCode() < 1 || normalized.getLevelCode() > 5)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "等级参数不正确");
    }
    if (normalized.getSource() != null
        && (normalized.getSource() < 0 || normalized.getSource() > 3)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "来源参数不正确");
    }
    List<String> types = normalized.getTypes();
    if (types == null || types.isEmpty()) {
      normalized.setTypes(List.of("single", "multi", "judge"));
    } else {
      Set<String> distinct = new LinkedHashSet<>(types);
      for (String type : distinct) {
        if (!"single".equals(type) && !"multi".equals(type) && !"judge".equals(type)) {
          throw new BusinessException(ErrorCode.BAD_REQUEST, "题型参数不正确");
        }
      }
      normalized.setTypes(List.copyOf(distinct));
    }
    return normalized;
  }

  private List<Integer> normalizeIds(List<Integer> ids) {
    if (ids == null) {
      return List.of();
    }
    return List.copyOf(new LinkedHashSet<>(ids));
  }

  private ObjectiveAnswerScopeQuery toQuery(ObjectiveAnswerScope scope) {
    ObjectiveAnswerScopeQuery query = new ObjectiveAnswerScopeQuery();
    query.setWorkTypeId(scope.getWorkTypeId());
    query.setLevelCode(scope.getLevelCode());
    query.setSource(scope.getSource());
    query.setTypes(scope.getTypes());
    return query;
  }

  private String filterJson(ObjectiveAnswerScope scope, List<ObjectiveAnswerScopeRow> rows) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("workTypeId", scope.getWorkTypeId());
    map.put("levelCode", scope.getLevelCode());
    map.put("source", scope.getSource());
    map.put("types", scope.getTypes());
    if (!rows.isEmpty()) {
      ObjectiveAnswerScopeRow first = rows.get(0);
      map.put("workTypeTitle", first.getWorkTypeTitle());
      map.put("courseTitle", first.getCourseTitle());
    }
    try {
      return objectMapper.writeValueAsString(map);
    } catch (Exception e) {
      return "{}";
    }
  }

  /** 知识点路径：按父链走内存（d.knowledge_id 单值，不会放大行数）。 */
  private Map<Integer, String> knowledgePaths(Collection<ObjectiveAnswerScopeRow> rows) {
    Set<Integer> courseIds = new LinkedHashSet<>();
    for (ObjectiveAnswerScopeRow row : rows) {
      if (row.getCourseId() != null) {
        courseIds.add(row.getCourseId());
      }
    }
    if (courseIds.isEmpty()) {
      return Map.of();
    }
    List<KnowledgeNodeRow> nodes = scopeMapper.listKnowledgeNodes(courseIds);
    Map<Integer, KnowledgeNodeRow> byId = new LinkedHashMap<>();
    for (KnowledgeNodeRow node : nodes) {
      byId.put(node.getId(), node);
    }
    Map<Integer, String> cache = new LinkedHashMap<>();
    for (KnowledgeNodeRow node : nodes) {
      cache.put(node.getId(), pathOf(node, byId));
    }
    return cache;
  }

  private String pathOf(KnowledgeNodeRow node, Map<Integer, KnowledgeNodeRow> byId) {
    List<String> chain = new ArrayList<>();
    KnowledgeNodeRow current = node;
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
    return String.join("/", chain);
  }

  private String ownerId() {
    return System.getenv().getOrDefault("HOSTNAME", "local") + ":" + ProcessHandle.current().pid()
        + ":" + java.util.UUID.randomUUID();
  }

  private String truncate(String text, int max) {
    if (text == null) {
      return null;
    }
    return text.length() <= max ? text : text.substring(0, max);
  }

  private record SnapshotRow(
      Integer detailId,
      String questionType,
      Integer source,
      Integer workTypeId,
      String workTypeTitle,
      Integer courseId,
      String courseTitle,
      Integer courseLevel,
      Integer knowledgeId,
      String knowledgePath,
      String content,
      String stemText,
      String answer,
      String resolveText,
      String optionSnapshotJson,
      String sourceHash,
      String contentQuality,
      String contentWarningsJson,
      String processStatus,
      String resultCode,
      String invalidReason,
      int optionCount) {

    boolean invalid() {
      return invalidReason != null;
    }
  }

  private record Plan(
      boolean overLimit,
      int selectionCount,
      String selectionMode,
      String filterJson,
      List<SnapshotRow> rows,
      int validCount,
      int invalidCount,
      int suspectContentCount,
      int invalidContentCount,
      int expectedCalls,
      Map<String, Integer> typeCounts,
      List<ObjectiveAnswerPreviewVO.InvalidReasonCount> invalidReasonCounts) {

    static Plan overLimit(int count, String mode, String filterJson) {
      return new Plan(true, count, mode, filterJson, List.of(), 0, 0, 0, 0, 0, Map.of(),
          List.of());
    }
  }
}
