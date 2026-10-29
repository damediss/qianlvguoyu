package com.guoyu.platform.ai.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.ai.dto.ObjectiveAnswerCheckAiDescriptor;
import com.guoyu.platform.ai.dto.ObjectiveAnswerCheckCallContext;
import com.guoyu.platform.exam.answercheck.config.ObjectiveAnswerCheckProperties;
import com.guoyu.platform.exam.entity.ExamFaqModelConfigEntity;
import com.guoyu.platform.exam.mapper.ExamFaqModelConfigMapper;
import com.guoyu.platform.exam.service.FaqModelConfigApplicationService;
import com.guoyu.platform.security.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 客观题答案检查 JEV 配置解析（方案 v1.5 §9.1）。
 *
 * <p>非敏感连接配置集中读 `exam_faq_model_config` 的 `OBJECTIVE_ANSWER_CHECK_JEV_MODEL` 行；
 * 真实 API Key 只读环境变量 `OBJECTIVE_ANSWER_CHECK_JEV_API_KEY`。
 * `descriptor()` 只允许 HTTP 线程调用；Worker 必须使用批次快照，不得重新读 DB。
 */
@Slf4j
@Service
public class ObjectiveAnswerCheckJevConfigResolver {

  public static final String MODEL_CODE =
      FaqModelConfigApplicationService.OBJECTIVE_ANSWER_CHECK_JEV_MODEL_CODE;
  private static final String PENDING_USER_INPUT = "PENDING_USER_INPUT";

  private final ExamFaqModelConfigMapper mapper;
  private final ObjectiveAnswerCheckProperties properties;
  private final String envApiKey;

  public ObjectiveAnswerCheckJevConfigResolver(ExamFaqModelConfigMapper mapper,
      ObjectiveAnswerCheckProperties properties,
      @Value("${OBJECTIVE_ANSWER_CHECK_JEV_API_KEY:}") String envApiKey) {
    this.mapper = mapper;
    this.properties = properties;
    this.envApiKey = envApiKey;
  }

  /** HTTP 入口使用：读取当前租户配置，只返回非敏感字段和凭据就绪布尔值。 */
  public ObjectiveAnswerCheckAiDescriptor descriptor() {
    return descriptorFor(SecurityContext.currentUser().getTenantId());
  }

  /** 测试与显式租户解析使用。 */
  public ObjectiveAnswerCheckAiDescriptor descriptorFor(Long tenantId) {
    ExamFaqModelConfigEntity cfg = findLatest(tenantId);
    if (cfg == null) {
      return ObjectiveAnswerCheckAiDescriptor.unavailable();
    }
    int concurrency = resolveConcurrency(cfg);
    int readTimeout = resolveReadTimeout(cfg);
    boolean enabled = "enabled".equals(cfg.getStatus());
    boolean connectionValid = hasText(cfg.getBaseUrl()) && hasText(cfg.getModelId())
        && readTimeout >= 5 && readTimeout <= 120;
    boolean ready = enabled && connectionValid && envApiKeyUsable();
    return new ObjectiveAnswerCheckAiDescriptor(MODEL_CODE, trimToNull(cfg.getModelId()),
        trimToNull(cfg.getBaseUrl()), ready, concurrency, readTimeout);
  }

  /**
   * Worker 调用：必须使用 context 中批次固化的 baseUrl/model/readTimeout；
   * 只从环境变量补真实密钥。DB 占位值、空串、PENDING_USER_INPUT 均不可用。
   */
  public Runtime runtimeFor(ObjectiveAnswerCheckCallContext context) {
    String apiKey = usableEnvApiKey();
    if (apiKey == null) {
      throw new IllegalStateException("OBJECTIVE_ANSWER_CHECK_JEV_API_KEY 未配置");
    }
    if (!hasText(context.jevBaseUrl()) || !hasText(context.modelId())
        || context.jevReadTimeoutSeconds() < 5 || context.jevReadTimeoutSeconds() > 120) {
      throw new IllegalStateException("批次 JEV 配置快照不完整");
    }
    return new Runtime(context.jevBaseUrl().trim(), apiKey, context.modelId().trim(),
        context.jevReadTimeoutSeconds());
  }

  private ExamFaqModelConfigEntity findLatest(Long tenantId) {
    if (tenantId == null) {
      return null;
    }
    return mapper.selectOne(new LambdaQueryWrapper<ExamFaqModelConfigEntity>()
        .eq(ExamFaqModelConfigEntity::getTenantId, tenantId)
        .eq(ExamFaqModelConfigEntity::getModelCode, MODEL_CODE)
        .eq(ExamFaqModelConfigEntity::getDeleted, 0)
        .orderByDesc(ExamFaqModelConfigEntity::getId)
        .last("LIMIT 1"));
  }

  private int resolveConcurrency(ExamFaqModelConfigEntity cfg) {
    int configured = cfg.getJevConcurrency() == null ? 5 : cfg.getJevConcurrency();
    int cap = Math.max(1, properties.getConcurrency());
    if (configured < 1 || configured > cap) {
      log.warn("[OAC-JEV] 配置并发 {} 超出 1~{}，防御性钳制为 {}", configured, cap,
          Math.min(cap, Math.max(1, configured)));
    }
    return Math.min(cap, Math.max(1, configured));
  }

  private int resolveReadTimeout(ExamFaqModelConfigEntity cfg) {
    return cfg.getJevReadTimeoutSeconds() == null ? 30 : cfg.getJevReadTimeoutSeconds();
  }

  private boolean envApiKeyUsable() {
    return usableEnvApiKey() != null;
  }

  private String usableEnvApiKey() {
    String key = trimToNull(envApiKey);
    if (key == null || PENDING_USER_INPUT.equals(key)
        || FaqModelConfigApplicationService.MODEL_API_KEY_CONFIG_FILE_PLACEHOLDER.equals(key)) {
      return null;
    }
    return key;
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private String trimToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  /** 含密钥的内存运行时对象，只允许传给 JevDecisionClient，禁止落库/返回前端。 */
  public record Runtime(String baseUrl, String apiKey, String modelId, int readTimeoutSeconds) {
  }
}
