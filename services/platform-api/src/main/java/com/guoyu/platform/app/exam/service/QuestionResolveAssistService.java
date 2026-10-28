package com.guoyu.platform.app.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.guoyu.platform.ai.entity.AiGenerationDraftEntity;
import com.guoyu.platform.ai.infrastructure.FaqLlmClient;
import com.guoyu.platform.ai.mapper.AiGenerationDraftMapper;
import com.guoyu.platform.app.sms.AppSmsException;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.entity.ExamExamDetailEntity;
import com.guoyu.platform.exam.entity.ExamFaqModelConfigEntity;
import com.guoyu.platform.exam.mapper.ExamExamDetailMapper;
import com.guoyu.platform.exam.service.FaqModelConfigApplicationService;
import com.guoyu.platform.exam.service.OfficialResolveResolver;
import com.guoyu.platform.exam.service.OfficialResolveResolver.OfficialResolve;
import com.guoyu.platform.exam.service.QuestionResolveInputFactory;
import com.guoyu.platform.exam.service.QuestionResolveInputFactory.QuestionPromptInput;
import com.guoyu.platform.exam.service.QuestionResolvePromptPolicy;
import com.guoyu.platform.exam.service.QuestionResolveRateLimitService;
import com.guoyu.platform.exam.reviewsync.service.ReviewQuestionQueryService;
import com.guoyu.platform.exam.service.ResolveMarkdownSanitizer;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.DictEntity;
import com.guoyu.platform.system.mapper.DictMapper;
import com.guoyu.platform.trainingdepartment.dto.FollowTaskQuestionReq;
import com.guoyu.platform.trainingdepartment.support.FollowTaskBankQuestionConverter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 题目解析统一入口（《试题解析Markdown表格化_一期实施方案》§5.3~§5.4）。
 *
 * <p>一份解析资产，一条获取链：官方读取器（resolve 列 → answer JSON → empty）→
 * 同 sourceHash 的 generated/shadow draft 缓存 → 按需 LLM 生成一次 → 全员读缓存。
 * 整个公开方法不使用 {@code @Transactional}，LLM HTTP 不在事务内；draft 与 resolve
 * 两次写入各自幂等可恢复，单步失败只记 warn，不让学员主链路炸掉。
 *
 * <p>写回由 {@code question_resolve_writeback_enabled} 控制：默认 0 只写 shadow draft
 * （影子验收，§10.1）；生产 resolve 列改 text 且 shadow 验收通过前不得置 1。
 * 已删除旧 250 字截断与「≥245 字查 draft 找全文」分支（§5.8）。
 */
@Slf4j
@Service
public class QuestionResolveAssistService {

  private static final String DICT_MODEL_CODE = "ai_review_model_code";
  private static final String DEFAULT_MODEL_CODE = "deepseek-review";
  private static final String DICT_GENERATION_ENABLED = "question_resolve_generation_enabled";
  private static final String DICT_WRITEBACK_ENABLED = "question_resolve_writeback_enabled";
  private static final String DICT_OFFICIAL_ONLY_KEYWORDS = "question_resolve_official_only_keywords";
  /** v2.2 存疑出口开关：默认开（无字典行即生效）；置 0 时存疑输出退化为质量门拒绝 + 冷却，用于止血。 */
  private static final String DICT_DISPUTE_ENABLED = "question_resolve_dispute_enabled";
  /** 解析模型独立配置键：与 ai_review_model_code（同步学鼓励语共用）隔离 */
  private static final String DICT_RESOLVE_MODEL_CODE = "question_resolve_model_code";

  private static final String DRAFT_TYPE = OfficialResolveResolver.DRAFT_TYPE;
  private static final String SOURCE_TYPE = OfficialResolveResolver.SOURCE_TYPE;
  private static final String PROMPT_PREFIX = OfficialResolveResolver.PROMPT_PREFIX;
  private static final String PROMPT_PREFIX_REJECTED = OfficialResolveResolver.PROMPT_PREFIX_REJECTED;

  private static final long LOCK_TTL_MILLIS = 120_000L;
  private static final long FAIL_COOLDOWN_SECONDS = 86_400L;
  private static final DefaultRedisScript<Long> RELEASE_LOCK = new DefaultRedisScript<>("""
      if redis.call('GET', KEYS[1]) == ARGV[1] then
        return redis.call('DEL', KEYS[1])
      else
        return 0
      end
      """, Long.class);

  private final ExamExamDetailMapper examDetailMapper;
  private final FollowTaskBankQuestionConverter bankQuestionConverter;
  private final FaqLlmClient llmClient;
  private final FaqModelConfigApplicationService modelConfigService;
  private final DictMapper dictMapper;
  private final AiGenerationDraftMapper draftMapper;
  private final OfficialResolveResolver officialResolver;
  private final QuestionResolveInputFactory inputFactory;
  private final QuestionResolveRateLimitService rateLimitService;
  private final ReviewQuestionQueryService reviewQuestionQueryService;
  private final StringRedisTemplate redisTemplate;

  public QuestionResolveAssistService(
      ExamExamDetailMapper examDetailMapper,
      FollowTaskBankQuestionConverter bankQuestionConverter,
      FaqLlmClient llmClient,
      FaqModelConfigApplicationService modelConfigService,
      DictMapper dictMapper,
      AiGenerationDraftMapper draftMapper,
      OfficialResolveResolver officialResolver,
      QuestionResolveInputFactory inputFactory,
      QuestionResolveRateLimitService rateLimitService,
      ReviewQuestionQueryService reviewQuestionQueryService,
      StringRedisTemplate redisTemplate) {
    this.examDetailMapper = examDetailMapper;
    this.bankQuestionConverter = bankQuestionConverter;
    this.llmClient = llmClient;
    this.modelConfigService = modelConfigService;
    this.dictMapper = dictMapper;
    this.draftMapper = draftMapper;
    this.officialResolver = officialResolver;
    this.inputFactory = inputFactory;
    this.rateLimitService = rateLimitService;
    this.reviewQuestionQueryService = reviewQuestionQueryService;
    this.redisTemplate = redisTemplate;
  }

  /**
   * App 控制器入口（§5.3）：强制登录 + 每学员每日限流；匿名一律「请先登录」，
   * 匿名不再能读缓存或触发生成。缓存命中/锁竞争/冷却命中不计限流。
   */
  public Map<String, Object> ensureResolveForApp(Integer questionId) {
    Long userId;
    try {
      userId = SecurityContext.currentUser().getUserId();
    } catch (IllegalStateException ex) {
      throw new AppSmsException(-1, "请先登录");
    }
    if (userId == null || userId <= 0) {
      throw new AppSmsException(-1, "请先登录");
    }
    return ensureInternal(questionId, userId);
  }

  /**
   * 内部受信入口（小培讲题/后续 W8.1/批量预生成）：不强制 App 登录、不限流，
   * 与 {@link #ensureResolveForApp} 共用同一缓存/生成实现。
   */
  public Map<String, Object> ensureResolve(Integer questionId) {
    return ensureInternal(questionId, null);
  }

  /**
   * 只读解析与题干，不触发 AI 生成（§5.3）。固定顺序：官方读取器 → 与当前 sourceHash
   * 一致的 generated draft → empty；绝不读 shadow/rejected，绝不调用 LLM。
   */
  public Map<String, Object> peekResolve(Integer questionId) {
    if (questionId == null || questionId <= 0) {
      throw new AppSmsException(-1, "题目 id 无效");
    }
    ExamExamDetailEntity detail = loadDetail(questionId);
    String stem = extractStem(detail);

    OfficialResolve official = officialResolver.resolve(detail);
    if (!official.isEmpty()) {
      return result(questionId, official.text(), official.source(), false, stem, null);
    }
    QuestionPromptInput input = inputFactory.build(detail);
    if (input != null) {
      AiGenerationDraftEntity draft = findLatestDraft(questionId, "generated");
      if (draft != null && StringUtils.hasText(draft.getResultText())
          // v2.2 §6.2：只读接口与主链路同口径，旧版本 draft 不算命中
          && OfficialResolveResolver.isCurrentPrompt(draft.getPrompt())
          && OfficialResolveResolver.promptHashEquals(draft.getPrompt(), input.sourceHash())) {
        return result(questionId, draft.getResultText().trim(), "draft", false, stem, null);
      }
    }
    return result(questionId, null, "empty", false, stem, null);
  }

  /** §5.4 逐步流程；appUserId 非空表示 App 链路（真实 LLM 前做每学员每日限流）。 */
  private Map<String, Object> ensureInternal(Integer questionId, Long appUserId) {
    // 1. 校验 questionId、题目存在且未删除；读取当前有效租户（默认 10159）
    if (questionId == null || questionId <= 0) {
      throw new AppSmsException(-1, "题目 id 无效");
    }
    ExamExamDetailEntity detail = loadDetail(questionId);
    String stem = extractStem(detail);
    Long tenantId = TenantContextHolder.getTenantId();
    if (tenantId == null || tenantId <= 0) {
      tenantId = TenantContextHolder.DEFAULT_TENANT_ID;
    }

    // 2. 官方读取器非 empty → 直接返回（db/official/draft），不调 LLM、不计限流
    OfficialResolve official = officialResolver.resolve(detail);
    if (!official.isEmpty()) {
      return result(questionId, official.text(), official.source(), false, stem, null);
    }

    // 3. 输入资格（缺题干/缺选项/缺答案 → invalid_input，不调 LLM）
    QuestionPromptInput input = inputFactory.build(detail);
    if (input == null) {
      return result(questionId, null, "empty", false, stem, "invalid_input");
    }

    // 4. generated/shadow draft 缓存（同 sourceHash 才用；writeback=0 时 shadow 可直接返回）
    Map<String, Object> cached = readDraftCache(questionId, detail, input, tenantId);
    if (cached != null) {
      return cached;
    }

    // 5. 生成开关（默认 0；缓存读取不受开关影响）
    if (dictInt(DICT_GENERATION_ENABLED, 0) != 1) {
      return result(questionId, null, "empty", false, stem, "disabled");
    }

    // 6. 失败冷却：同 sourceHash 24h 内不重复；sourceHash 变了说明题目已改，可重试
    String failKey = "question_resolve:fail:" + questionId;
    try {
      String failedHash = redisTemplate.opsForValue().get(failKey);
      if (input.sourceHash().equals(failedHash)) {
        return result(questionId, null, "empty", false, stem, "cooldown");
      }
    } catch (Exception e) {
      log.warn("[题目解析] 冷却读取失败 questionId={} err={}", questionId, e.getMessage());
    }

    // 7. Redis 题级锁（跨实例防重）；拿不到锁不重复调 LLM
    String lockKey = "question_resolve:lock:" + questionId;
    String lockValue = UUID.randomUUID().toString();
    boolean locked = false;
    try {
      locked = Boolean.TRUE.equals(
          redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, java.time.Duration.ofMillis(LOCK_TTL_MILLIS)));
    } catch (Exception e) {
      log.warn("[题目解析] 获取锁失败 questionId={} err={}", questionId, e.getMessage());
    }
    if (!locked) {
      AiGenerationDraftEntity draft = findLatestDraft(questionId, "generated");
      if (draft != null && StringUtils.hasText(draft.getResultText())
          && OfficialResolveResolver.promptHashEquals(draft.getPrompt(), input.sourceHash())) {
        return result(questionId, draft.getResultText().trim(), "draft", false, stem, null);
      }
      return result(questionId, null, "empty", false, stem, "generating");
    }

    try {
      // 8. 锁内二次检查：等待期间另一实例可能已生成（重新跑 2~4 步）
      ExamExamDetailEntity freshDetail = examDetailMapper.selectById(questionId);
      if (freshDetail != null && (freshDetail.getDeleted() == null || freshDetail.getDeleted() == 0)) {
        detail = freshDetail;
        stem = extractStem(detail);
        official = officialResolver.resolve(detail);
        if (!official.isEmpty()) {
          return result(questionId, official.text(), official.source(), false, stem, null);
        }
        input = inputFactory.build(detail);
        if (input == null) {
          return result(questionId, null, "empty", false, stem, "invalid_input");
        }
        cached = readDraftCache(questionId, detail, input, tenantId);
        if (cached != null) {
          return cached;
        }
      }

      // 9. App 链路真实 LLM 前每学员每日限流（缓存命中/锁竞争/冷却命中不计次）
      if (appUserId != null) {
        try {
          rateLimitService.acquire(tenantId, appUserId);
        } catch (BusinessException ex) {
          return result(questionId, null, "empty", false, stem, "rate_limited");
        }
      }

      // 10~11. 调 LLM → 写 draft（shadow/generated）；失败写 rejected draft + 冷却
      return generateAndPersist(questionId, detail, input, tenantId, stem);
    } finally {
      releaseLock(lockKey, lockValue);
    }
  }

  /**
   * §5.4 步骤 4：generated draft 同版本同 hash 命中 → draft（writeback=1 且列空补回写）；
   * writeback=0 再查 shadow draft；最后查 v2.2 的 answer_disputed 缓存（存疑不重复调 LLM）。
   *
   * <p>注意：某条 generated 不可用（旧 prompt 版本或 sourceHash 已变）时**不能短路返回**，
   * 必须继续查 shadow / answer_disputed。否则 v2.1 遗留的 generated（hash 相同、版本不同）
   * 会永久挡住存疑缓存——同题每次点击都重新调 LLM、重复写 answer_disputed，App 链路还要计限流
   * （2026-10-28 verifier 复核发现的 P2，修复见方案 §15.6）。
   */
  private Map<String, Object> readDraftCache(Integer questionId, ExamExamDetailEntity detail,
      QuestionPromptInput input, Long tenantId) {
    AiGenerationDraftEntity generated = findLatestDraft(questionId, "generated");
    if (generated != null && StringUtils.hasText(generated.getResultText())) {
      if (OfficialResolveResolver.isCurrentPrompt(generated.getPrompt())
          && OfficialResolveResolver.promptHashEquals(generated.getPrompt(), input.sourceHash())) {
        if (dictInt(DICT_WRITEBACK_ENABLED, 0) == 1 && !StringUtils.hasText(detail.getResolve())) {
          backfillResolveColumn(detail.getId(), generated.getResultText().trim());
        }
        return result(questionId, generated.getResultText().trim(), "draft", false,
            extractStem(detail), null);
      }
      // 该 generated 不可用（旧版本 / 题面已变）：不使用它，但继续往下查 shadow 与存疑缓存
      log.debug("[题目解析] generated draft 版本或 hash 不匹配，继续查下级缓存 questionId={}",
          questionId);
    }
    if (dictInt(DICT_WRITEBACK_ENABLED, 0) != 1) {
      AiGenerationDraftEntity shadow = findLatestDraft(questionId, "shadow");
      if (shadow != null && StringUtils.hasText(shadow.getResultText())
          && OfficialResolveResolver.isCurrentPrompt(shadow.getPrompt())
          && OfficialResolveResolver.promptHashEquals(shadow.getPrompt(), input.sourceHash())) {
        return result(questionId, shadow.getResultText().trim(), "shadow", false,
            extractStem(detail), null);
      }
    }
    // v2.2 §5.2：存疑缓存命中——同版本同 hash 直接返回，学员端只拿中性文案，不展示【存疑】全文
    AiGenerationDraftEntity disputed = findLatestDraft(questionId, "answer_disputed");
    if (disputed != null && StringUtils.hasText(disputed.getResultText())
        && OfficialResolveResolver.isCurrentPrompt(disputed.getPrompt())
        && OfficialResolveResolver.promptHashEquals(disputed.getPrompt(), input.sourceHash())) {
      return result(questionId, null, "empty", false, extractStem(detail), "answer_disputed");
    }
    return null;
  }

  /** §5.4 步骤 10~11：LLM 生成 → 质量门（§5.7）→ shadow/generated draft；失败 → rejected + 冷却。 */
  private Map<String, Object> generateAndPersist(Integer questionId, ExamExamDetailEntity detail,
      QuestionPromptInput input, Long tenantId, String stem) {
    // S13：安全/法规白名单命中不调用 LLM：不写 rejected、不进冷却
    // 白名单默认 安全规程,法律法规；sys_dict question_resolve_official_only_keywords 可按环境扩展（§5.7）
    if (ResolveMarkdownSanitizer.isOfficialOnlyKnowledge(
        input.knowledgeTitle(), officialOnlyKeywords())) {
      log.info("[题目解析] 知识点命中安全/法规白名单，不生成 questionId={} knowledge={}",
          questionId, input.knowledgeTitle());
      return result(questionId, null, "empty", false, stem, "official_only");
    }

    ExamFaqModelConfigEntity config = resolveModelConfig(tenantId);
    if (config == null || !StringUtils.hasText(config.getApiKey())) {
      log.warn("[题目解析] 模型配置缺失，无法 AI 生成 questionId={}", questionId);
      onGenerationFailed(questionId, input, tenantId, null);
      return result(questionId, null, "empty", false, stem, "llm_failed");
    }

    int maxTokens = QuestionResolvePromptPolicy.maxTokensFor(config.getThinking(), config.getMaxTokens());
    String systemPrompt = QuestionResolvePromptPolicy.systemPrompt(
        QuestionResolvePromptPolicy.budgetFor(input.normalizedType()));
    String userPrompt = QuestionResolvePromptPolicy.userPrompt(input);

    String content;
    try {
      content = llmClient.chatCompletion(
          config.getBaseUrl(), config.getApiKey(), config.getModelId(),
          systemPrompt, userPrompt, maxTokens,
          config.getThinking(), config.getReasoningEffort(), false, tenantId);
    } catch (Exception e) {
      log.warn("[题目解析] LLM 失败 questionId={} err={}", questionId, e.getMessage());
      onGenerationFailed(questionId, input, tenantId, null);
      return result(questionId, null, "empty", false, stem, "llm_failed");
    }
    if (!StringUtils.hasText(content)) {
      log.warn("[题目解析] LLM 空响应 questionId={}", questionId);
      onGenerationFailed(questionId, input, tenantId, content);
      return result(questionId, null, "empty", false, stem, "llm_failed");
    }

    // 质量门：拒绝 → rejected draft + 冷却；表格整表丢弃只记日志
    ResolveMarkdownSanitizer.SanitizeResult sanitized = ResolveMarkdownSanitizer.sanitize(content, input);
    if (sanitized.rejectedReason() != null) {
      log.info("[题目解析] 质量门拒绝 questionId={} reason={}", questionId, sanitized.rejectedReason());
      onGenerationFailed(questionId, input, tenantId, content);
      return result(questionId, null, "empty", false, stem, "quality_rejected");
    }
    // v2.2 §5.1 答案存疑分支：不写 resolve 列、不写 generated/shadow、不进冷却；
    // 只落 answer_disputed draft 作为人工复核工单，学员端拿到中性文案。
    if (sanitized.disputed()) {
      if (dictInt(DICT_DISPUTE_ENABLED, 1) != 1) {
        // 存疑出口被关（止血）：按质量门拒绝处理
        log.info("[题目解析] 存疑出口已关闭，按拒绝处理 questionId={}", questionId);
        onGenerationFailed(questionId, input, tenantId, content);
        return result(questionId, null, "empty", false, stem, "quality_rejected");
      }
      writeDraft(questionId, input.sourceHash(), sanitized.text().trim(), tenantId, "answer_disputed", false);
      log.info("[题目解析] 答案存疑 questionId={} knowledge={}", questionId, input.knowledgeTitle());
      return result(questionId, null, "empty", false, stem, "answer_disputed");
    }
    if (sanitized.tableDropped()) {
      log.info("[题目解析] 表格超限整表丢弃 questionId={}", questionId);
    }

    String text = sanitized.text().trim();
    boolean writeback = dictInt(DICT_WRITEBACK_ENABLED, 0) == 1;
    if (writeback) {
      backfillResolveColumn(detail.getId(), text);
      writeDraft(questionId, input.sourceHash(), text, tenantId, "generated", false);
      return result(questionId, text, "ai", true, stem, null);
    }
    writeDraft(questionId, input.sourceHash(), text, tenantId, "shadow", false);
    return result(questionId, text, "shadow", true, stem, null);
  }

  /** 失败处理（§5.4 步骤 10）：rejected draft（失败文本永不进入读取链）+ 24h 冷却。 */
  private void onGenerationFailed(Integer questionId, QuestionPromptInput input,
      Long tenantId, String rawOutput) {
    try {
      writeDraft(questionId, input.sourceHash(), rawOutput, tenantId, "rejected", true);
    } catch (Exception e) {
      log.warn("[题目解析] rejected draft 留痕失败 questionId={} err={}", questionId, e.getMessage());
    }
    try {
      redisTemplate.opsForValue().set(
          "question_resolve:fail:" + questionId, input.sourceHash(),
          java.time.Duration.ofSeconds(FAIL_COOLDOWN_SECONDS));
    } catch (Exception e) {
      log.warn("[题目解析] 冷却写入失败 questionId={} err={}", questionId, e.getMessage());
    }
  }

  private ExamExamDetailEntity loadDetail(Integer questionId) {
    ExamExamDetailEntity detail = examDetailMapper.selectById(questionId);
    if (detail == null || (detail.getDeleted() != null && detail.getDeleted() != 0)) {
      throw new AppSmsException(-1, "题目不存在");
    }
    return detail;
  }

  private String extractStem(ExamExamDetailEntity detail) {
    if (detail == null) {
      return null;
    }
    try {
      FollowTaskQuestionReq snap = bankQuestionConverter.convert(detail, 0);
      if (snap != null && StringUtils.hasText(snap.getStem())) {
        return QuestionResolveInputFactory.stripHtml(snap.getStem());
      }
    } catch (Exception ignored) {
      // fall through：非客观题或快照失败时退回题干列
    }
    return QuestionResolveInputFactory.stripHtml(detail.getContent());
  }

  /**
   * 回写 resolve 列：全文写入、无 250 截断；条件更新「仅当列为空」——LLM 生成期间
   * 管理员手工填写时影响 0 行，AI 结果不覆盖人工内容（§5.11）。
   */
  private void backfillResolveColumn(Integer questionId, String fullText) {
    if (questionId == null || !StringUtils.hasText(fullText)) {
      return;
    }
    try {
      int updated = examDetailMapper.update(null,
          new LambdaUpdateWrapper<ExamExamDetailEntity>()
              .eq(ExamExamDetailEntity::getId, questionId)
              .and(wrapper -> wrapper.isNull(ExamExamDetailEntity::getResolve)
                  .or().eq(ExamExamDetailEntity::getResolve, ""))
              .set(ExamExamDetailEntity::getResolve, fullText.trim()));
      if (updated == 0) {
        // 未回写（已有人工内容）不 evict：缓存里的人工解析仍然有效
        log.info("[题目解析] resolve 列已有人工内容，跳过重写 questionId={}", questionId);
      } else {
        evictQuestionDetailCache(questionId);
      }
    } catch (Exception e) {
      log.warn("[题目解析] 回写 resolve 失败 questionId={} err={}", questionId, e.getMessage());
    }
  }

  /** 回写成功后失效同步学 6h 详情缓存，避免"生成了却读不到"。 */
  private void evictQuestionDetailCache(Integer questionId) {
    try {
      reviewQuestionQueryService.evictQuestionDetail(questionId.longValue());
    } catch (Exception e) {
      log.warn("[题目解析] 缓存失效失败 questionId={} err={}", questionId, e.getMessage());
    }
  }

  private AiGenerationDraftEntity findLatestDraft(Integer questionId, String status) {
    try {
      return draftMapper.selectOne(new LambdaQueryWrapper<AiGenerationDraftEntity>()
          .eq(AiGenerationDraftEntity::getDraftType, DRAFT_TYPE)
          .eq(AiGenerationDraftEntity::getSourceType, SOURCE_TYPE)
          .eq(AiGenerationDraftEntity::getSourceId, questionId.longValue())
          .eq(AiGenerationDraftEntity::getStatus, status)
          .eq(AiGenerationDraftEntity::getDeleted, 0)
          .orderByDesc(AiGenerationDraftEntity::getId)
          .last("LIMIT 1"));
    } catch (Exception e) {
      log.debug("[题目解析] draft 查询失败 questionId={} status={} err={}",
          questionId, status, e.getMessage());
      return null;
    }
  }

  /** draft 留痕（§5.10）；rejected 用独立 prompt 前缀。单步失败只记 warn。 */
  private void writeDraft(Integer questionId, String sourceHash, String resultText,
      Long tenantId, String status, boolean rejected) {
    try {
      AiGenerationDraftEntity draft = new AiGenerationDraftEntity();
      draft.setTenantId(tenantId);
      draft.setDraftType(DRAFT_TYPE);
      draft.setSourceType(SOURCE_TYPE);
      draft.setSourceId(questionId.longValue());
      draft.setPrompt((rejected ? PROMPT_PREFIX_REJECTED : PROMPT_PREFIX) + sourceHash);
      draft.setResultText(resultText);
      draft.setModelName(readModelCode(tenantId));
      draft.setStatus(status);
      draft.setDeleted(0);
      draftMapper.insert(draft);
    } catch (Exception e) {
      log.warn("[题目解析] draft 留痕失败 questionId={} status={} err={}",
          questionId, status, e.getMessage());
    }
  }

  private void releaseLock(String lockKey, String lockValue) {
    try {
      redisTemplate.execute(RELEASE_LOCK, List.of(lockKey), lockValue);
    } catch (Exception e) {
      log.warn("[题目解析] 锁释放失败 key={} err={}", lockKey, e.getMessage());
    }
  }

  private ExamFaqModelConfigEntity resolveModelConfig(Long tenantId) {
    try {
      return modelConfigService.findByModelCode(tenantId, readModelCode(tenantId));
    } catch (Exception e) {
      log.warn("[题目解析] 模型配置解析失败 tenantId={} err={}", tenantId, e.getMessage());
      return null;
    }
  }

  /**
   * 解析模型选择（模型隔离）：优先独立键 {@code question_resolve_model_code}；
   * 未配置时兼容存量环境回退 {@code ai_review_model_code}（该键同时被同步学鼓励语使用，
   * 不得改动其值）；都未配置用默认 deepseek-review。
   * 注意：只在新键【未配置】时才回退旧键——新键配置了但对应模型配置缺失时由
   * {@link #resolveModelConfig} 返回 null → llm_failed，不静默换回旧模型。
   */
  private String readModelCode(Long tenantId) {
    String resolveModelCode = dictValue(DICT_RESOLVE_MODEL_CODE);
    if (StringUtils.hasText(resolveModelCode)) {
      return resolveModelCode.trim();
    }
    String sharedModelCode = dictValue(DICT_MODEL_CODE);
    if (StringUtils.hasText(sharedModelCode)) {
      return sharedModelCode.trim();
    }
    return DEFAULT_MODEL_CODE;
  }

  private String dictValue(String code) {
    List<DictEntity> dicts = dictMapper.selectList(new LambdaQueryWrapper<DictEntity>()
        .eq(DictEntity::getCode, code)
        .eq(DictEntity::getDeleted, 0)
        .last("LIMIT 1"));
    if (!dicts.isEmpty() && StringUtils.hasText(dicts.get(0).getValue())) {
      return dicts.get(0).getValue().trim();
    }
    return null;
  }

  /** S13 白名单（sys_dict 逗号分隔）；未配置用默认常量。 */
  private java.util.List<String> officialOnlyKeywords() {
    try {
      List<DictEntity> dicts = dictMapper.selectList(new LambdaQueryWrapper<DictEntity>()
          .eq(DictEntity::getCode, DICT_OFFICIAL_ONLY_KEYWORDS)
          .eq(DictEntity::getDeleted, 0)
          .last("LIMIT 1"));
      if (!dicts.isEmpty() && StringUtils.hasText(dicts.get(0).getValue())) {
        return java.util.Arrays.stream(dicts.get(0).getValue().split("[,，]"))
            .map(String::trim).filter(s -> !s.isEmpty()).toList();
      }
    } catch (Exception e) {
      log.warn("[题目解析] 读取白名单字典失败，用默认值：{}", e.getMessage());
    }
    return null;
  }

  private int dictInt(String code, int defaultValue) {
    try {
      List<DictEntity> dicts = dictMapper.selectList(new LambdaQueryWrapper<DictEntity>()
          .eq(DictEntity::getCode, code)
          .eq(DictEntity::getDeleted, 0)
          .last("LIMIT 1"));
      if (!dicts.isEmpty() && dicts.get(0).getValue() != null) {
        return Integer.parseInt(dicts.get(0).getValue().trim());
      }
    } catch (Exception e) {
      log.warn("[题目解析] 读取字典 {} 失败，用默认值 {}：{}", code, defaultValue, e.getMessage());
    }
    return defaultValue;
  }

  private static Map<String, Object> result(
      Integer questionId, String resolve, String source, boolean generated, String stem, String reason) {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("questionId", questionId);
    out.put("resolve", resolve);
    out.put("analysis", resolve);
    out.put("source", source);
    out.put("generated", generated);
    if (StringUtils.hasText(stem)) {
      out.put("stem", stem);
    }
    if (reason != null) {
      out.put("reason", reason);
    }
    return out;
  }
}
