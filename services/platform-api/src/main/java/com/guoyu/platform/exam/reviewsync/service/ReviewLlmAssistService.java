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

  /** 预置文案池（按阶段 × 场景）。
   * 文案规则（D9 §15.4）：只夸努力/方法/坚持（过程性表扬，Mueller & Dweck 1998），
   * 禁特质表扬（"你真聪明"）、禁排名攀比（掌握目标定向）、禁控制性语言（自我决定理论）。 */
  private static final Map<String, List<String>> PRESETS = Map.of(
      "1", List.of(
          "地基不牢，地动山摇。今天的每一道新题，都是考场上的 1 分，稳住节奏刷完它！",
          "地毯式扫荡期，别纠结对错，先把所有题见一遍。完成比完美重要！",
          "刷题就像炼铝，温度到了自然成。今天的任务量刚刚好，拿下它！",
          "第一遍不求全对，只求全见过。见过的题越多，考场上的生面孔越少。",
          "今天刷的每道题，都在给知识地图点灯。灯点得越多，路越好走。",
          "起步阶段比的不是速度，是谁更能坚持。你今天坐住了，就已经赢过昨天的自己。",
          "遇到不会的题别慌，先标记再往下走。第一遍的目标是见识，不是满分。",
          "每天这点题量，是专家按遗忘曲线算好的。跟着节奏走，别加戏也别偷懒。"),
      "2", List.of(
          "定向爆破期，错题就是你的矿脉。今天把这些硬骨头啃下来，分数立刻见涨！",
          "能错的题都见过了，接下来就是把它们一个个变成得分点。今天的复盘很关键！",
          "错题不过夜，红牌天天见。坚持清理，你离及格线又近了一步！",
          "这个阶段涨分最快：每消灭一张红牌，考场上就少一个丢分坑。",
          "错题肯露面是好事——它现在不暴露，就会在考场上暴露。今天继续清算！",
          "别嫌同一道题反复出现，它出现一次，你的印象就深一层。这正是设计的用意。",
          "昨天的错题今天还对，说明方法对了；还错，就再看一眼解析。两种结果都是进步。",
          "盯住错题本别松手。这个阶段你和自己较的劲，都会变成成绩单上的分。"),
      "3", List.of(
          "仿真模考期，把每一次模考都当成真考。稳住心态，你已经准备得很好了！",
          "考场比的不只是会不会，更是稳不稳。今天这套卷，练的就是临场应变！",
          "最后冲刺，保持手感就是胜利。相信自己，这套题你一定能拿下！",
          "模考分数起落很正常，看趋势别盯单点。你的曲线一直在往上走。",
          "把每次模考的错题当情报：它们在告诉你，最后几天该往哪儿使劲。",
          "到今天这步，拼的是心态。按平时的节奏答题，你准备得比自己以为的更好。",
          "考前不做偏题怪题，把基础分拿稳就是高分。今天这套卷就是这个思路。",
          "一路刷到这里不容易。最后几天保持节奏，考场正常发挥就是胜利。"));

  private static final List<String> INTERRUPT_RETURN_PRESETS = List.of(
      "中断几天没关系，最怕的是放弃。今天先热热身，我们重新出发！",
      "学习路上谁都有断档的时候，重要的是你现在回来了。节奏放慢一点，稳住！",
      "回来就好。前面学过的内容还在你脑子里，今天先找回手感，不追求速度。",
      "计划已经按你的中断重新排好了，不用补旧账。从今天这页开始，一步一步来。");

  /** 连对鼓励池（D9 §15.4）：过程性表扬 + 自我参照 + 信息性反馈 */
  private static final Map<Integer, List<String>> STREAK_CORRECT_PRESETS = Map.of(
      3, List.of(
          "连对 3 题！读题节奏找对了，保持这个方法。",
          "3 连对！刚才这几题的读题方法很稳，继续这样拆题。",
          "连对 3 题，开局不错。是你一步步刷出来的手感。",
          "3 连对！圈关键词、看否定词，你这几题的做法就是标准动作。"),
      5, List.of(
          "5 连对！这几题说明这个知识点已经吃透了，是你一道道刷出来的实力。",
          "连对 5 题！这个正确率不是运气，是你前面积累在兑现。",
          "5 连对，状态起来了。保持当前读题速度，别急着提速。",
          "连对 5 题！你对题干的处理越来越熟练，继续保持。"),
      10, List.of(
          "10 连对，手感火热！保持这个读题节奏，别急着提速。",
          "连对 10 题！稳定输出比偶尔爆发更值钱，这就是考场要的状态。",
          "10 连对！这一串对题背后，是你这些天一天没落下的练习。",
          "连对 10 题，非常强。记住现在这种读题的感觉，考场上就照这个来。"),
      15, List.of(
          "15 连对！稳定输出比偶尔爆发更值钱，你现在的状态就是考场要的状态。",
          "连对 15 题！这已经不是手感了，是扎实的基本功在说话。",
          "15 连对，太稳了。你比自己想象的准备得更充分。"),
      20, List.of(
          "20 连对！这是日复一日刷题攒出来的底气，考场照这个节奏来就行。",
          "连对 20 题，满分节奏！把这份沉稳带到考场上，没问题的。"));

  /** 连错鼓励池（D9 §15.4）：归因方法（可控）+ 错题是提分点（成长型思维/纠错增强记忆），
   * 绝不暗示能力问题（归因理论） */
  private static final Map<Integer, List<String>> STREAK_WRONG_PRESETS = Map.of(
      2, List.of(
          "这两题错得正是时候——现在暴露的盲区，都是考场上能捡回的分。看一眼解析再往下走。",
          "连错 2 题别在意，错题现在出现比考场出现强一百倍。看看解析，下一题找回来。",
          "这两题没拿下，多半是题干里有坑。回看一眼关键词，你会发现自己其实会。",
          "错了 2 题，正好把这两处漏洞补上。补一题是一题，都是分数。"),
      3, List.of(
          "碰到硬骨头了。放慢一点，先圈题干关键词再选。错题现在出现，是帮你考前堵漏，不是坏事。",
          "连错 3 题，说明撞上了薄弱知识点。这是好事：它现在暴露，你就有机会在考前补上。",
          "这几题有点绕。别急着往下刷，把这题的解析读透，比再做十题都值。",
          "连续几题没选对，不是不会，是这类题的出题套路还没摸熟。看解析找规律，马上就能翻身。"),
      4, List.of(
          "连着几题没拿下，先停一停：回头看看这几题的解析，弄懂一道比刷过十道值。准备好了再继续，不急。"));

  private final FaqLlmClient llmClient;
  private final FaqModelConfigApplicationService modelConfigService;
  private final DictMapper dictMapper;
  private final AiGenerationDraftMapper draftMapper;
  private final ThreadPoolTaskExecutor executor;
  private final Cache<String, String> encourageCache;
  /** 连对/连错防打扰（D9）：planId:dayIndex:type:tier → 已发放标记（每档每日一次） */
  private final Cache<String, Boolean> streakFiredCache;
  /** 连对/连错防打扰（D9）：planId:dayIndex → 上次发放时的当日作答数（每 5 题最多 1 条） */
  private final Cache<String, Integer> streakLastShownCache;
  private final Random random = new Random();

  public ReviewLlmAssistService(
      FaqLlmClient llmClient,
      FaqModelConfigApplicationService modelConfigService,
      DictMapper dictMapper,
      AiGenerationDraftMapper draftMapper,
      @Qualifier("reviewLlmAssistExecutor") ThreadPoolTaskExecutor executor) {
    this.llmClient = llmClient;
    this.modelConfigService = modelConfigService;
    this.dictMapper = dictMapper;
    this.draftMapper = draftMapper;
    this.executor = executor;
    this.encourageCache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofHours(24))
        .build();
    this.streakFiredCache = Caffeine.newBuilder()
        .maximumSize(100_000)
        .expireAfterWrite(Duration.ofHours(24))
        .build();
    this.streakLastShownCache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofHours(24))
        .build();
  }

  /**
   * 连对/连错即时鼓励（D9 §15.3）：命中档位时下发预置文案。
   *
   * <p>防打扰：每档每日每计划仅一次；距上一条鼓励不足 5 题则压制。
   * 进程重启计数清零，降级为"可能多给一条"，无副作用。
   *
   * @return 文案；本次不发放（未命中/已发过/间隔不足）返回 null
   */
  public String streakEncouragement(long planId, int dayIndex, String streakType, int tier,
      int answerCountOfDay) {
    if (tier <= 0) {
      return null;
    }
    String dayKey = planId + ":" + dayIndex;
    if (Boolean.TRUE.equals(streakFiredCache.getIfPresent(dayKey + ":" + streakType + ":" + tier))) {
      log.debug("[AI复习] 即时鼓励压制（本档今日已发）：planId={} dayIndex={} type={} tier={}",
          planId, dayIndex, streakType, tier);
      return null;
    }
    Integer lastShown = streakLastShownCache.getIfPresent(dayKey);
    if (lastShown != null && answerCountOfDay - lastShown < 5) {
      log.debug("[AI复习] 即时鼓励压制（距上条不足 5 题）：planId={} dayIndex={} type={} tier={} lastShown={} now={}",
          planId, dayIndex, streakType, tier, lastShown, answerCountOfDay);
      return null;
    }
    Map<Integer, List<String>> pool = StreakEncourageDomainService.TYPE_CORRECT.equals(streakType)
        ? STREAK_CORRECT_PRESETS : STREAK_WRONG_PRESETS;
    List<String> candidates = pool.get(tier);
    if (candidates == null || candidates.isEmpty()) {
      return null;
    }
    streakFiredCache.put(dayKey + ":" + streakType + ":" + tier, Boolean.TRUE);
    streakLastShownCache.put(dayKey, answerCountOfDay);
    log.info("[AI复习] 即时鼓励发放：planId={} dayIndex={} type={} tier={} answerCount={}",
        planId, dayIndex, streakType, tier, answerCountOfDay);
    return candidates.get(random.nextInt(candidates.size()));
  }

  /** 读缓存/预置文案（同步，绝不阻塞主链路） */
  public String getEncouragement(long planId, int dayIndex, int stage, boolean interruptReturn) {
    String cached = encourageCache.getIfPresent(planId + ":" + dayIndex);
    if (cached != null) {
      return cached;
    }
    return preset(stage, interruptReturn);
  }

  /**
   * 失效指定天的鼓励语缓存（考试日期变更重排专用）：
   * 旧文案按旧总天数生成（如"别想那八十六天"），重排后不再适用，须重新生成。
   */
  public void evictEncouragement(long planId, List<Integer> dayIndexes) {
    if (dayIndexes == null) {
      return;
    }
    for (Integer dayIndex : dayIndexes) {
      if (dayIndex != null) {
        encourageCache.invalidate(planId + ":" + dayIndex);
      }
    }
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
          systemPrompt, prompt, 500, config.getThinking(), config.getReasoningEffort(), false, tenantId);
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
