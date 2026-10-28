package com.guoyu.platform.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.ai.entity.AiGenerationDraftEntity;
import com.guoyu.platform.ai.mapper.AiGenerationDraftMapper;
import com.guoyu.platform.exam.entity.ExamExamDetailEntity;
import com.guoyu.platform.exam.service.QuestionResolveInputFactory.QuestionPromptInput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 官方解析统一读取器（《试题解析Markdown表格化_一期实施方案》§5.1）。
 *
 * <p>所有「查解析」消费方共用同一份读取顺序，禁止再各写一份 JSON key 列表：
 * resolve 列（人工/历史，或判旧后让位）→ answer JSON 官方解析 → empty。
 * 返回文本统一 trim，不按 250 字截断（§5.8）。answer JSON 只读使用，不回写 resolve 列。
 *
 * <p>resolve 列与「最近一条有效 generated draft 完全一致且 sourceHash 相同」时返回
 * source=draft，用于 AI 来源标注；人工改写过的列文本永不判旧（§5.5 isStaleAiResolve）。
 */
@Slf4j
@Component
public class OfficialResolveResolver {

  public static final String DRAFT_TYPE = "question_resolve";
  public static final String SOURCE_TYPE = "exam_exam_detail";
  /**
   * 成功/影子 draft 的 prompt 前缀（v2.2 §6.1）。版本号参与缓存命中判定：
   * {@link #isCurrentPrompt(String)} 与 {@link #promptHashEquals(String, String)} 双匹配才算命中，
   * 否则 prompt 升级后旧 draft 会一直命中、新 prompt 永远不生效。
   */
  public static final String PROMPT_PREFIX = "question_resolve_v2.2#";
  /** 失败 draft 的 prompt 前缀（§5.10）；失败文本永不进入读取链。 */
  public static final String PROMPT_PREFIX_REJECTED = "question_resolve_v2.2_rejected#";

  /** source ∈ db | official | draft | empty */
  public record OfficialResolve(String text, String source) {
    public boolean isEmpty() {
      return text == null;
    }
  }

  private static final OfficialResolve EMPTY = new OfficialResolve(null, "empty");
  /** answer JSON 官方解析 key 顺序（§5.1，唯一一份列表）。 */
  private static final String[] ANSWER_JSON_KEYS = {"resolve", "analysis", "jiexi", "explain", "解析"};

  private final AiGenerationDraftMapper draftMapper;
  private final QuestionResolveInputFactory inputFactory;
  private final ObjectMapper objectMapper;

  public OfficialResolveResolver(
      AiGenerationDraftMapper draftMapper,
      QuestionResolveInputFactory inputFactory,
      ObjectMapper objectMapper) {
    this.draftMapper = draftMapper;
    this.inputFactory = inputFactory;
    this.objectMapper = objectMapper;
  }

  public OfficialResolve resolve(ExamExamDetailEntity detail) {
    if (detail == null || (detail.getDeleted() != null && detail.getDeleted() != 0)) {
      return EMPTY;
    }
    String columnText = detail.getResolve() == null ? null : detail.getResolve().trim();
    if (StringUtils.hasText(columnText)) {
      // 共用一次 draft 查询与一次 sourceHash 计算（§5.1 规则 2）
      AiGenerationDraftEntity draft = findLatestGeneratedDraft(detail.getId());
      QuestionPromptInput input = inputFactory.build(detail);
      if (!isStaleAiResolve(columnText, draft, input)) {
        boolean sameAsDraft = draft != null && StringUtils.hasText(draft.getResultText())
            && columnText.equals(draft.getResultText().trim())
            && input != null && promptHashEquals(draft.getPrompt(), input.sourceHash());
        return new OfficialResolve(columnText, sameAsDraft ? "draft" : "db");
      }
      // 已失效的 AI 解析：不返回列文本，继续查 answer JSON
    }
    String official = extractOfficialFromAnswerJson(detail.getAnswer());
    if (official != null) {
      return new OfficialResolve(official, "official");
    }
    return EMPTY;
  }

  /**
   * 失效判定（§5.5）：列文本与最近成功 draft 完全一致、但 sourceHash 不一致（题面/答案/
   * 知识点已变）或当前已无法构建合法输入 → 判旧。无 generated draft（人工/历史解析）
   * 或人工改写过的列文本 → 永久可信，不判旧。不依赖 updated_at。
   */
  static boolean isStaleAiResolve(String columnText, AiGenerationDraftEntity latestGenerated,
      QuestionPromptInput input) {
    if (latestGenerated == null || !StringUtils.hasText(latestGenerated.getResultText())) {
      return false;
    }
    if (!columnText.equals(latestGenerated.getResultText().trim())) {
      return false;
    }
    if (input == null) {
      return true;
    }
    return !promptHashEquals(latestGenerated.getPrompt(), input.sourceHash());
  }

  private AiGenerationDraftEntity findLatestGeneratedDraft(Integer questionId) {
    if (questionId == null) {
      return null;
    }
    try {
      return draftMapper.selectOne(new LambdaQueryWrapper<AiGenerationDraftEntity>()
          .eq(AiGenerationDraftEntity::getDraftType, DRAFT_TYPE)
          .eq(AiGenerationDraftEntity::getSourceType, SOURCE_TYPE)
          .eq(AiGenerationDraftEntity::getSourceId, questionId.longValue())
          .eq(AiGenerationDraftEntity::getStatus, "generated")
          .eq(AiGenerationDraftEntity::getDeleted, 0)
          .orderByDesc(AiGenerationDraftEntity::getId)
          .last("LIMIT 1"));
    } catch (Exception e) {
      log.debug("[题目解析] 查询 generated draft 失败 questionId={} err={}", questionId, e.getMessage());
      return null;
    }
  }

  /** draft.prompt = {@code question_resolve_v2.2#{sourceHash}}；取 '#' 后缀比对。 */
  public static boolean promptHashEquals(String prompt, String sourceHash) {
    if (prompt == null || sourceHash == null) {
      return false;
    }
    int idx = prompt.indexOf('#');
    if (idx < 0 || idx + 1 >= prompt.length()) {
      return false;
    }
    return sourceHash.equals(prompt.substring(idx + 1).trim());
  }

  /**
   * 当前 prompt 版本判定（v2.2 §6.1）：draft 缓存命中必须版本 + hash 双匹配。
   *
   * <p>只用于未落列的 draft 缓存（generated / shadow / answer_disputed）与 peekResolve；
   * 已回写 resolve 列的 AI 解析**不因 prompt 升级判旧**（{@link #isStaleAiResolve} 仍只看 hash），
   * 避免升级触发全量重生成。
   */
  public static boolean isCurrentPrompt(String prompt) {
    return prompt != null && prompt.startsWith(PROMPT_PREFIX);
  }

  /** answer JSON 根节点为对象时按 key 顺序取第一个非空字符串；异常按无官方解析处理。 */
  private String extractOfficialFromAnswerJson(String answerJson) {
    if (!StringUtils.hasText(answerJson)) {
      return null;
    }
    String raw = answerJson.trim();
    if (!raw.startsWith("{")) {
      return null;
    }
    try {
      JsonNode root = objectMapper.readTree(raw);
      if (!root.isObject()) {
        return null;
      }
      for (String key : ANSWER_JSON_KEYS) {
        JsonNode node = root.get(key);
        if (node != null && node.isTextual() && StringUtils.hasText(node.asText())) {
          return node.asText().trim();
        }
      }
    } catch (Exception e) {
      log.debug("[题目解析] answer JSON 官方解析读取失败 err={}", e.getMessage());
    }
    return null;
  }
}
