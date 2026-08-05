package com.guoyu.platform.exam.reviewsync.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.ai.entity.AiGenerationDraftEntity;
import com.guoyu.platform.ai.infrastructure.FaqLlmClient;
import com.guoyu.platform.ai.mapper.AiGenerationDraftMapper;
import com.guoyu.platform.exam.entity.ExamFaqModelConfigEntity;
import com.guoyu.platform.exam.service.FaqModelConfigApplicationService;
import com.guoyu.platform.system.entity.DictEntity;
import com.guoyu.platform.system.mapper.DictMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * LLM 鼓励语（§6，一期唯一 LLM 场景）。
 *
 * <p>选型：sys_dict {@code ai_review_model_code} → {@code exam_faq_model_config} 按 code 解析（§6 机制）。
 * 配置缺失/调用失败 → 预置文案池降级，绝不阻塞刷题主链路。
 * 留痕：ai_generation_draft（draftType='ai_review_encourage'，AGENTS.md AI 规则）。
 * 缓存：planId+dayIndex → 文案（每日一条，物化时异步生成，Caffeine TTL 24h）。
 */
@Slf4j
@Service
public class ReviewLlmAssistService {

  private static final String DICT_MODEL_CODE = "ai_review_model_code";
  private static final String DEFAULT_MODEL_CODE = "deepseek-review";
  private static final String DRAFT_TYPE = "ai_review_encourage";

  /** 预置文案池（按阶段 × 场景） */
  private static final Map<String, List<String>> PRESETS = Map.of(
      "1", List.of(
          "地基不牢，地动山摇。今天的每一道新题，都是考场上的 1 分，稳住节奏刷完它！",
          "地毯式扫荡期，别纠结对错，先把所有题见一遍。完成比完美重要！",
          "刷题就像炼铝，温度到了自然成。今天的任务量刚刚好，拿下它！"),
      "2", List.of(
          "定向爆破期，错题就是你的矿脉。今天把这些硬骨头啃下来，分数立刻见涨！",
          "能错的题都见过了，接下来就是把它们一个个变成得分点。今天的复盘很关键！",
          "错题不过夜，红牌天天见。坚持清理，你离及格线又近了一步！"),
      "3", List.of(
          "仿真模考期，把每一次模考都当成真考。稳住心态，你已经准备得很好了！",
          "考场比的不只是会不会，更是稳不稳。今天这套卷，练的就是临场应变！",
          "最后冲刺，保持手感就是胜利。相信自己，这套题你一定能拿下！"));

  private static final List<String> INTERRUPT_RETURN_PRESETS = List.of(
      "中断几天没关系，最怕的是放弃。今天先热热身，我们重新出发！",
      "学习路上谁都有断档的时候，重要的是你现在回来了。节奏放慢一点，稳住！");

  private final FaqLlmClient llmClient;
  private final FaqModelConfigApplicationService modelConfigService;
  private final DictMapper dictMapper;
  private final AiGenerationDraftMapper draftMapper;
  private final ThreadPoolTaskExecutor executor;
  private final Cache<String, String> encourageCache;
  private final Random random = new Random();

  public ReviewLlmAssistService(
      FaqLlmClient llmClient,
      FaqModelConfigApplicationService modelConfigService,
      DictMapper dictMapper,
      AiGenerationDraftMapper draftMapper,
      @Qualifier("reviewSyncExecutor") ThreadPoolTaskExecutor executor) {
    this.llmClient = llmClient;
    this.modelConfigService = modelConfigService;
    this.dictMapper = dictMapper;
    this.draftMapper = draftMapper;
    this.executor = executor;
    this.encourageCache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofHours(24))
        .build();
  }

  /** 读缓存/预置文案（同步，绝不阻塞主链路） */
  public String getEncouragement(long planId, int dayIndex, int stage, boolean interruptReturn) {
    String cached = encourageCache.getIfPresent(planId + ":" + dayIndex);
    if (cached != null) {
      return cached;
    }
    return preset(stage, interruptReturn);
  }

  /** 物化时异步生成（失败自动降级预置文案） */
  public void generateAsync(long planId, int dayIndex, int stage, String workTypeTitle, String levelName,
      int totalDays, Double yesterdayAccuracy, boolean interruptReturn, Long tenantId) {
    try {
      executor.execute(() -> generateAndCache(planId, dayIndex, stage, workTypeTitle, levelName,
          totalDays, yesterdayAccuracy, interruptReturn, tenantId));
    } catch (Exception e) {
      log.warn("[AI复习] 鼓励语线程池拒绝，使用预置文案：planId={}", planId);
    }
  }

  /** 同步生成（测试与内部用）：LLM 失败/配置缺失 → null（调用方回落预置文案） */
  public String generateAndCache(long planId, int dayIndex, int stage, String workTypeTitle,
      String levelName, int totalDays, Double yesterdayAccuracy, boolean interruptReturn, Long tenantId) {
    String prompt = buildPrompt(stage, workTypeTitle, levelName, dayIndex, totalDays, yesterdayAccuracy,
        interruptReturn);
    try {
      ExamFaqModelConfigEntity config = resolveModelConfig(tenantId);
      if (config == null || config.getApiKey() == null || config.getApiKey().isBlank()) {
        log.warn("[AI复习] 模型配置缺失或未填 apiKey（code={}），鼓励语走预置文案", readModelCode(tenantId));
        return null;
      }
      String systemPrompt = "你是一位职业技能等级认定考试的教练老师，负责给学员写每日鼓励语。"
          + "要求：中文、不超过 60 字、口语化、有力量感、结合学员当前阶段，不要 emoji。";
      String content = llmClient.chatCompletion(config.getBaseUrl(), config.getApiKey(), config.getModelId(),
          systemPrompt, prompt, 200, null, false, tenantId);
      if (content == null || content.isBlank()) {
        return null;
      }
      String text = content.trim();
      if (text.length() > 80) {
        text = text.substring(0, 80);
      }
      encourageCache.put(planId + ":" + dayIndex, text);
      writeDraft(planId, systemPrompt + "\n" + prompt, text, config.getModelId(), tenantId);
      return text;
    } catch (Exception e) {
      log.warn("[AI复习] LLM 鼓励语生成失败，走预置文案：planId={} error={}", planId, e.getMessage());
      return null;
    }
  }

  private String buildPrompt(int stage, String workTypeTitle, String levelName, int dayIndex,
      int totalDays, Double yesterdayAccuracy, boolean interruptReturn) {
    StringBuilder sb = new StringBuilder();
    sb.append("学员正在备考").append(workTypeTitle == null ? "职业技能等级认定" : workTypeTitle);
    if (levelName != null) {
      sb.append("（").append(levelName).append("）");
    }
    sb.append("，当前处于复习第 ").append(dayIndex).append(" 天/共 ").append(totalDays).append(" 天");
    sb.append("，阶段：").append(switch (stage) {
      case 1 -> "地毯式扫荡（过遍所有题）";
      case 2 -> "定向爆破（专攻错题）";
      default -> "仿真模考（冲刺）";
    });
    if (yesterdayAccuracy != null) {
      sb.append("，昨日正确率 ").append(Math.round(yesterdayAccuracy * 100)).append("%");
    }
    if (interruptReturn) {
      sb.append("，学员中断几天后今天刚回归");
    }
    sb.append("。请写一句鼓励语。");
    return sb.toString();
  }

  private String preset(int stage, boolean interruptReturn) {
    if (interruptReturn) {
      return INTERRUPT_RETURN_PRESETS.get(random.nextInt(INTERRUPT_RETURN_PRESETS.size()));
    }
    List<String> pool = PRESETS.getOrDefault(String.valueOf(stage), PRESETS.get("1"));
    return pool.get(random.nextInt(pool.size()));
  }

  private String readModelCode(Long tenantId) {
    List<DictEntity> dicts = dictMapper.selectList(new LambdaQueryWrapper<DictEntity>()
        .eq(DictEntity::getCode, DICT_MODEL_CODE)
        .eq(DictEntity::getDeleted, 0)
        .last("LIMIT 1"));
    if (!dicts.isEmpty() && dicts.get(0).getValue() != null && !dicts.get(0).getValue().isBlank()) {
      return dicts.get(0).getValue().trim();
    }
    return DEFAULT_MODEL_CODE;
  }

  private ExamFaqModelConfigEntity resolveModelConfig(Long tenantId) {
    try {
      return modelConfigService.findByModelCode(tenantId, readModelCode(tenantId));
    } catch (Exception e) {
      log.warn("[AI复习] 模型配置解析失败：tenantId={} error={}", tenantId, e.getMessage());
      return null;
    }
  }

  private void writeDraft(long planId, String prompt, String resultText, String modelName, Long tenantId) {
    try {
      AiGenerationDraftEntity draft = new AiGenerationDraftEntity();
      draft.setTenantId(tenantId);
      draft.setDraftType(DRAFT_TYPE);
      draft.setSourceType("ai_review_plan");
      draft.setSourceId(planId);
      draft.setPrompt(prompt);
      draft.setResultText(resultText);
      draft.setModelName(modelName);
      draft.setStatus("generated");
      draft.setDeleted(0);
      draftMapper.insert(draft);
    } catch (Exception e) {
      log.warn("[AI复习] 鼓励语留痕失败（不影响主链路）：planId={} error={}", planId, e.getMessage());
    }
  }
}
