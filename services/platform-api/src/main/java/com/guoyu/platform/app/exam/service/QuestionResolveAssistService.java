package com.guoyu.platform.app.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.ai.entity.AiGenerationDraftEntity;
import com.guoyu.platform.ai.infrastructure.FaqLlmClient;
import com.guoyu.platform.ai.mapper.AiGenerationDraftMapper;
import com.guoyu.platform.app.sms.AppSmsException;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.entity.ExamExamDetailEntity;
import com.guoyu.platform.exam.entity.ExamFaqModelConfigEntity;
import com.guoyu.platform.exam.mapper.ExamExamDetailMapper;
import com.guoyu.platform.exam.service.FaqModelConfigApplicationService;
import com.guoyu.platform.system.entity.DictEntity;
import com.guoyu.platform.system.mapper.DictMapper;
import com.guoyu.platform.trainingdepartment.dto.FollowTaskQuestionReq;
import com.guoyu.platform.trainingdepartment.support.FollowTaskBankQuestionConverter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 题目解析：先查库 → 无则 LLM 生成 → 回写题库（并留痕 draft）。
 *
 * <p>用于精准学/班级练习答错后补解析；不阻塞组卷主链路，按题按需拉取。
 */
@Slf4j
@Service
public class QuestionResolveAssistService {

  private static final String DICT_MODEL_CODE = "ai_review_model_code";
  private static final String DEFAULT_MODEL_CODE = "deepseek-review";
  private static final String DRAFT_TYPE = "question_resolve";
  private static final String SOURCE_TYPE = "exam_exam_detail";
  /** exam_exam_detail.resolve 现网多为 varchar(255) */
  private static final int RESOLVE_DB_MAX = 250;

  private final ExamExamDetailMapper examDetailMapper;
  private final FollowTaskBankQuestionConverter bankQuestionConverter;
  private final FaqLlmClient llmClient;
  private final FaqModelConfigApplicationService modelConfigService;
  private final DictMapper dictMapper;
  private final AiGenerationDraftMapper draftMapper;
  private final ConcurrentHashMap<Integer, Object> locks = new ConcurrentHashMap<>();

  public QuestionResolveAssistService(
      ExamExamDetailMapper examDetailMapper,
      FollowTaskBankQuestionConverter bankQuestionConverter,
      FaqLlmClient llmClient,
      FaqModelConfigApplicationService modelConfigService,
      DictMapper dictMapper,
      AiGenerationDraftMapper draftMapper) {
    this.examDetailMapper = examDetailMapper;
    this.bankQuestionConverter = bankQuestionConverter;
    this.llmClient = llmClient;
    this.modelConfigService = modelConfigService;
    this.dictMapper = dictMapper;
    this.draftMapper = draftMapper;
  }

  /**
   * 获取解析：db → draft 缓存 → AI 生成并落库。
   *
   * @return questionId / resolve / source(db|draft|ai|empty) / generated
   */
  @Transactional
  public Map<String, Object> ensureResolve(Integer questionId) {
    if (questionId == null || questionId <= 0) {
      throw new AppSmsException(-1, "题目 id 无效");
    }
    Object lock = locks.computeIfAbsent(questionId, k -> new Object());
    synchronized (lock) {
      try {
        return ensureResolveLocked(questionId);
      } finally {
        locks.remove(questionId, lock);
      }
    }
  }

  private Map<String, Object> ensureResolveLocked(Integer questionId) {
    ExamExamDetailEntity detail = examDetailMapper.selectById(questionId);
    if (detail == null || (detail.getDeleted() != null && detail.getDeleted() != 0)) {
      throw new AppSmsException(-1, "题目不存在");
    }

    if (StringUtils.hasText(detail.getResolve())) {
      String dbText = detail.getResolve().trim();
      // varchar(255) 截断时优先返回 draft 全文
      if (dbText.length() >= RESOLVE_DB_MAX - 5) {
        AiGenerationDraftEntity draft = findDraft(questionId);
        if (draft != null && StringUtils.hasText(draft.getResultText())
            && draft.getResultText().trim().length() > dbText.length()) {
          return result(questionId, draft.getResultText().trim(), "draft", false);
        }
      }
      return result(questionId, dbText, "db", false);
    }

    AiGenerationDraftEntity draft = findDraft(questionId);
    if (draft != null && StringUtils.hasText(draft.getResultText())) {
      String full = draft.getResultText().trim();
      backfillResolveColumn(detail, full);
      return result(questionId, full, "draft", false);
    }

    Long tenantId = TenantContextHolder.getTenantId();
    if (tenantId == null || tenantId <= 0) {
      tenantId = TenantContextHolder.DEFAULT_TENANT_ID;
    }

    String generated = generateByLlm(detail, tenantId);
    if (!StringUtils.hasText(generated)) {
      return result(questionId, null, "empty", false);
    }
    String text = generated.trim();
    writeDraft(questionId, text, tenantId);
    backfillResolveColumn(detail, text);
    return result(questionId, text, "ai", true);
  }

  private String generateByLlm(ExamExamDetailEntity detail, Long tenantId) {
    ExamFaqModelConfigEntity config = resolveModelConfig(tenantId);
    if (config == null || !StringUtils.hasText(config.getApiKey())) {
      log.warn("[题目解析] 模型配置缺失，无法 AI 生成 questionId={}", detail.getId());
      return null;
    }

    String stem;
    String optionsText;
    String correct;
    try {
      FollowTaskQuestionReq snap = bankQuestionConverter.convert(detail, 0);
      stem = snap.getStem() == null ? "" : snap.getStem();
      StringBuilder optSb = new StringBuilder();
      if (snap.getOptions() != null) {
        for (Object raw : snap.getOptions()) {
          if (raw instanceof Map<?, ?> om) {
            optSb.append(om.get("key")).append(". ").append(om.get("text")).append('\n');
          }
        }
      }
      optionsText = optSb.toString().trim();
      correct = snap.getAnswer() == null ? "" : String.valueOf(snap.getAnswer());
    } catch (Exception e) {
      stem = detail.getContent() == null ? "" : detail.getContent();
      optionsText = "";
      correct = detail.getAnswer() == null ? "" : detail.getAnswer();
    }

    String systemPrompt = "你是职业技能等级认定考试的辅导老师。"
        + "请用简洁中文写解析。第一行必须是：考点：xxx；易错：xxx。"
        + "随后用一两句说明为什么选该答案。"
        + "要求：总长不超过 180 字，不要 markdown，不要 emoji，不要复述整道题干。";
    String userPrompt = "题型：" + (detail.getType() == null ? "" : detail.getType()) + "\n"
        + "题干：\n" + truncate(stem, 800) + "\n"
        + (StringUtils.hasText(optionsText) ? "选项：\n" + truncate(optionsText, 600) + "\n" : "")
        + "正确答案：" + truncate(correct, 200) + "\n"
        + "请按格式写出解析（首行含考点与易错）：";

    try {
      String content = llmClient.chatCompletion(
          config.getBaseUrl(),
          config.getApiKey(),
          config.getModelId(),
          systemPrompt,
          userPrompt,
          400,
          null,
          config.getReasoningEffort(),
          false,
          tenantId);
      if (!StringUtils.hasText(content)) {
        return null;
      }
      String text = content.trim();
      if (text.length() > 400) {
        text = text.substring(0, 400);
      }
      return text;
    } catch (Exception e) {
      log.warn("[题目解析] LLM 失败 questionId={} err={}", detail.getId(), e.getMessage());
      return null;
    }
  }

  private void backfillResolveColumn(ExamExamDetailEntity detail, String fullText) {
    if (detail == null || detail.getId() == null || !StringUtils.hasText(fullText)) {
      return;
    }
    if (StringUtils.hasText(detail.getResolve())) {
      return;
    }
    try {
      String store = fullText.trim();
      if (store.length() > RESOLVE_DB_MAX) {
        store = store.substring(0, RESOLVE_DB_MAX);
      }
      ExamExamDetailEntity patch = new ExamExamDetailEntity();
      patch.setId(detail.getId());
      patch.setResolve(store);
      examDetailMapper.updateById(patch);
      detail.setResolve(store);
    } catch (Exception e) {
      log.warn("[题目解析] 回写 resolve 失败 questionId={} err={}", detail.getId(), e.getMessage());
    }
  }

  private AiGenerationDraftEntity findDraft(Integer questionId) {
    return draftMapper.selectOne(new LambdaQueryWrapper<AiGenerationDraftEntity>()
        .eq(AiGenerationDraftEntity::getDraftType, DRAFT_TYPE)
        .eq(AiGenerationDraftEntity::getSourceType, SOURCE_TYPE)
        .eq(AiGenerationDraftEntity::getSourceId, questionId.longValue())
        .eq(AiGenerationDraftEntity::getDeleted, 0)
        .orderByDesc(AiGenerationDraftEntity::getId)
        .last("LIMIT 1"));
  }

  private void writeDraft(Integer questionId, String resultText, Long tenantId) {
    try {
      AiGenerationDraftEntity draft = new AiGenerationDraftEntity();
      draft.setTenantId(tenantId);
      draft.setDraftType(DRAFT_TYPE);
      draft.setSourceType(SOURCE_TYPE);
      draft.setSourceId(questionId.longValue());
      draft.setPrompt("question_resolve#" + questionId);
      draft.setResultText(resultText);
      draft.setModelName(readModelCode(tenantId));
      draft.setStatus("generated");
      draft.setDeleted(0);
      draftMapper.insert(draft);
    } catch (Exception e) {
      log.warn("[题目解析] 留痕失败 questionId={} err={}", questionId, e.getMessage());
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

  private String readModelCode(Long tenantId) {
    List<DictEntity> dicts = dictMapper.selectList(new LambdaQueryWrapper<DictEntity>()
        .eq(DictEntity::getCode, DICT_MODEL_CODE)
        .eq(DictEntity::getDeleted, 0)
        .last("LIMIT 1"));
    if (!dicts.isEmpty() && StringUtils.hasText(dicts.get(0).getValue())) {
      return dicts.get(0).getValue().trim();
    }
    return DEFAULT_MODEL_CODE;
  }

  private static Map<String, Object> result(Integer questionId, String resolve, String source, boolean generated) {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("questionId", questionId);
    out.put("resolve", resolve);
    out.put("analysis", resolve);
    out.put("source", source);
    out.put("generated", generated);
    return out;
  }

  private static String truncate(String s, int max) {
    if (s == null) {
      return "";
    }
    String t = s.trim();
    return t.length() <= max ? t : t.substring(0, max);
  }
}
