package com.guoyu.platform.exam.reviewsync.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewAnswerEntity;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewGrayWhitelistEntity;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewMockPaperEntity;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewMockPaperItemEntity;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewPlanDayEntity;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewPlanEntity;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewWrongItemEntity;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewAnswerMapper;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewGrayWhitelistMapper;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewMockPaperItemMapper;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewMockPaperMapper;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewPlanDayMapper;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewPlanMapper;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewWrongItemMapper;
import com.guoyu.platform.integration.php.PhpExamQueryService;
import com.guoyu.platform.integration.php.dto.PhpCourseWeights;
import com.guoyu.platform.integration.php.dto.PhpExamPlanInfo;
import com.guoyu.platform.integration.php.dto.PhpKnowledgeNode;
import com.guoyu.platform.integration.php.dto.PhpQuestion;
import com.guoyu.platform.integration.php.dto.PhpQuestionDetail;
import com.guoyu.platform.system.entity.DictEntity;
import com.guoyu.platform.system.mapper.DictMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 同步学用例编排（§5 / §7）。
 *
 * <p>线程边界：所有公开方法以 platformUserId/tenantId 参数驱动，不调 SecurityContext；
 * 定时任务线程入口由 ReviewSyncScheduler 负责 TenantContextHolder.setTenantId(1L) + clear。
 */
@Slf4j
@Service
public class ReviewPlanApplicationService {

  private final AiReviewPlanMapper planMapper;
  private final AiReviewPlanDayMapper dayMapper;
  private final AiReviewAnswerMapper answerMapper;
  private final AiReviewWrongItemMapper wrongMapper;
  private final AiReviewMockPaperMapper mockPaperMapper;
  private final AiReviewMockPaperItemMapper mockItemMapper;
  private final AiReviewGrayWhitelistMapper grayWhitelistMapper;
  private final PhpExamQueryService phpQuery;
  private final PhpResultWritebackService writebackService;
  private final ReviewLlmAssistService llmAssistService;
  private final DictMapper dictMapper;
  private final ObjectMapper objectMapper;

  public ReviewPlanApplicationService(
      AiReviewPlanMapper planMapper,
      AiReviewPlanDayMapper dayMapper,
      AiReviewAnswerMapper answerMapper,
      AiReviewWrongItemMapper wrongMapper,
      AiReviewMockPaperMapper mockPaperMapper,
      AiReviewMockPaperItemMapper mockItemMapper,
      AiReviewGrayWhitelistMapper grayWhitelistMapper,
      PhpExamQueryService phpQuery,
      PhpResultWritebackService writebackService,
      ReviewLlmAssistService llmAssistService,
      DictMapper dictMapper,
      ObjectMapper objectMapper) {
    this.planMapper = planMapper;
    this.dayMapper = dayMapper;
    this.answerMapper = answerMapper;
    this.wrongMapper = wrongMapper;
    this.mockPaperMapper = mockPaperMapper;
    this.mockItemMapper = mockItemMapper;
    this.grayWhitelistMapper = grayWhitelistMapper;
    this.phpQuery = phpQuery;
    this.writebackService = writebackService;
    this.llmAssistService = llmAssistService;
    this.dictMapper = dictMapper;
    this.objectMapper = objectMapper;
  }

  // ==================================================================
  // D8 灰度发布（方案 §15.2）：总开关 ai_review_gray_enabled + exam/user 双粒度白名单
  // ==================================================================

  /** 灰度拦截：开关开启时，examId 或 userId 任一命中白名单才放行；开关关闭/缺失=全员开放 */
  private void gateGrayRelease(long examId, long platformUserId, Long tenantId) {
    if (!dictBool("ai_review_gray_enabled", false, tenantId)) {
      return;
    }
    boolean allowed = grayWhitelistMapper.selectCount(new LambdaQueryWrapper<AiReviewGrayWhitelistEntity>()
            .eq(AiReviewGrayWhitelistEntity::getScopeType, "exam")
            .eq(AiReviewGrayWhitelistEntity::getScopeId, examId)) > 0
        || grayWhitelistMapper.selectCount(new LambdaQueryWrapper<AiReviewGrayWhitelistEntity>()
            .eq(AiReviewGrayWhitelistEntity::getScopeType, "user")
            .eq(AiReviewGrayWhitelistEntity::getScopeId, platformUserId)) > 0;
    if (!allowed) {
      log.warn("[AI复习] 灰度拦截：user={} exam={} 不在白名单", platformUserId, examId);
      throw new BusinessException(ErrorCode.BAD_REQUEST, "同步练习正在小范围试点中，敬请期待");
    }
  }

  // ==================================================================
  // 7.1 计划概览（懒生成入口）
  // ==================================================================

  @Transactional
  public Map<String, Object> overview(long platformUserId, long examId, Long tenantId) {
    gateGrayRelease(examId, platformUserId, tenantId);
    AiReviewPlanEntity plan = getOrCreatePlan(platformUserId, examId, tenantId);
    LocalDate today = LocalDate.now();

    InterruptInfo interrupt = applyInterruptIfNeeded(plan, today);
    AiReviewPlanDayEntity day = getOrMaterializeDay(plan, plan.getCurrentDayIndex(), today, interrupt);

    Map<String, Object> result = new LinkedHashMap<>();
    // 雪花 id 超出 JS Number 安全整数（2^53），App 端一律以字符串下发（前端原样回传即可）
    result.put("planId", String.valueOf(plan.getId()));
    result.put("examId", examId);
    result.put("stage", plan.getCurrentStage());
    result.put("stageName", stageName(plan.getCurrentStage()));
    result.put("dayIndex", plan.getCurrentDayIndex());
    result.put("totalDays", plan.getTotalDays());
    result.put("examDate", plan.getExamDate() != null ? plan.getExamDate().toString() : null);
    result.put("today", todayView(plan, day));
    result.put("progress", progressView(plan));
    result.put("wrongStats", wrongStatsView(plan.getId()));
    if (interrupt != null) {
      Map<String, Object> interruptView = new LinkedHashMap<>();
      interruptView.put("case", interrupt.caseName());
      interruptView.put("strategyText", interrupt.strategyText());
      result.put("interrupt", interruptView);
    } else {
      result.put("interrupt", null);
    }
    result.put("encourageText", llmAssistService.getEncouragement(
        plan.getId(), plan.getCurrentDayIndex(), plan.getCurrentStage(), interrupt != null));
    return result;
  }

  // ==================================================================
  // 7.2 今日任务清单
  // ==================================================================

  @Transactional
  public Map<String, Object> todayTask(long platformUserId, long examId, Long tenantId) {
    gateGrayRelease(examId, platformUserId, tenantId);
    AiReviewPlanEntity plan = requirePlan(platformUserId, examId);
    LocalDate today = LocalDate.now();
    InterruptInfo interrupt = applyInterruptIfNeeded(plan, today);
    AiReviewPlanDayEntity day = getOrMaterializeDay(plan, plan.getCurrentDayIndex(), today, interrupt);

    // done 标记按轮次独立：新题区只认 round=new，清算区只认 round=review
    // （同一题可能先在「新题」答过又进入清算队列，不能互相算作完成）
    Set<Long> doneNewIds = new HashSet<>();
    Set<Long> doneReviewIds = new HashSet<>();
    for (AiReviewAnswerEntity a : answerMapper.selectList(new LambdaQueryWrapper<AiReviewAnswerEntity>()
        .eq(AiReviewAnswerEntity::getDayId, day.getId()))) {
      if ("new".equals(a.getRoundType())) {
        doneNewIds.add(a.getQuestionId());
      } else if ("review".equals(a.getRoundType())) {
        doneReviewIds.add(a.getQuestionId());
      }
    }
    List<Map<String, Object>> newQuestions = new ArrayList<>();
    for (Long qid : parseIds(day.getNewQuestionIds())) {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("questionId", qid);
      item.put("done", doneNewIds.contains(qid));
      newQuestions.add(item);
    }
    // 复盘队列动态取当前在册错题：当天新产生的错题实时进入清算，不依赖物化时的快照
    List<AiReviewWrongItemEntity> reviewList = reviewListForDay(plan, day);
    List<Map<String, Object>> wrongQuestions = new ArrayList<>();
    for (AiReviewWrongItemEntity wrong : reviewList) {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("questionId", wrong.getQuestionId());
      item.put("cardLevel", wrong.getCardLevel());
      item.put("star", wrong.getStar());
      item.put("wrongCount", wrong.getWrongCount());
      item.put("done", doneReviewIds.contains(wrong.getQuestionId()));
      wrongQuestions.add(item);
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("dayId", String.valueOf(day.getId()));
    result.put("stage", day.getStage());
    result.put("newQuestions", newQuestions);
    result.put("wrongQuestions", wrongQuestions);
    result.put("mock", mockView(day));
    result.put("strategyNote", day.getStrategyNote());
    return result;
  }

  // ==================================================================
  // 7.3 题目详情（批量，剥离答案标记与解析）
  // ==================================================================

  public Map<String, Object> questions(long platformUserId, long examId, List<Long> questionIds) {
    gateGrayRelease(examId, platformUserId, null);
    requirePlan(platformUserId, examId);
    if (questionIds == null || questionIds.isEmpty() || questionIds.size() > 50) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "questionIds 数量须为 1~50");
    }
    Map<Long, PhpQuestionDetail> details = phpQuery.loadQuestionDetails(questionIds);
    List<Map<String, Object>> list = new ArrayList<>();
    for (Long qid : questionIds) {
      PhpQuestionDetail detail = details.get(qid);
      if (detail == null) {
        continue;
      }
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("questionId", detail.id());
      item.put("type", detail.type());
      item.put("content", detail.content());
      item.put("options", stripAnswerOptions(detail.answerJson()));
      list.add(item);
    }
    return Map.of("list", list);
  }

  // ==================================================================
  // 7.4 单题作答
  // ==================================================================

  @Transactional
  public Map<String, Object> answer(long platformUserId, long examId, long dayId, long questionId,
      String roundType, String userAnswer, Integer useTimeSec, Long tenantId) {
    gateGrayRelease(examId, platformUserId, tenantId);
    AiReviewPlanEntity plan = requirePlan(platformUserId, examId);
    AiReviewPlanDayEntity day = requireDay(plan.getId(), dayId);

    // 幂等与改答案语义：
    //  - 答案相同 或 此前已答对 → 纯回放（防双击/倒退刷分，不写库不计数）
    //  - 此前答错 + 新答案不同 → 改答案（新尝试：重判分、驱动错题卡、计数只加差额）
    AiReviewAnswerEntity existing = answerMapper.selectOne(new LambdaQueryWrapper<AiReviewAnswerEntity>()
        .eq(AiReviewAnswerEntity::getPlanId, plan.getId())
        .eq(AiReviewAnswerEntity::getDayId, dayId)
        .eq(AiReviewAnswerEntity::getQuestionId, questionId)
        .eq(AiReviewAnswerEntity::getRoundType, roundType)
        .orderByDesc(AiReviewAnswerEntity::getAttemptSeq)
        .last("LIMIT 1"));
    PhpQuestionDetail detail = phpQuery.loadQuestionDetail(questionId);
    if (detail == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "题目不存在");
    }
    List<AnswerJudgeDomainService.AnswerOption> options = parseOptions(detail.answerJson());
    boolean correction = false;
    if (existing != null) {
      boolean sameAnswer = java.util.Objects.equals(existing.getUserAnswer(), userAnswer);
      boolean wasCorrect = existing.getIsCorrect() != null && existing.getIsCorrect() == 1;
      if (sameAnswer || wasCorrect) {
        return answerView(existing.getIsCorrect() == 1, options, detail.resolve(), null);
      }
      correction = true;
    }

    // 判分
    AnswerJudgeDomainService.JudgeResult judged = AnswerJudgeDomainService.judge(
        detail.type(), options, userAnswer, parseUserMultiAnswer(userAnswer));

    // 写作答记录
    int attemptSeq = countAttempts(plan.getId(), questionId) + 1;
    AiReviewAnswerEntity answer = new AiReviewAnswerEntity();
    answer.setPlanId(plan.getId());
    answer.setDayId(dayId);
    answer.setPlatformUserId(platformUserId);
    answer.setExamPlanId(examId);
    answer.setQuestionId(questionId);
    answer.setQuestionScore(detail.score());
    answer.setKnowledgeId(detail.knowledgeId());
    answer.setQuestionType(detail.type());
    answer.setRoundType(roundType);
    answer.setUserAnswer(userAnswer);
    answer.setIsCorrect(judged.correct() ? 1 : 0);
    answer.setUseTimeSec(useTimeSec != null ? useTimeSec : 0);
    answer.setAttemptSeq(attemptSeq);
    answer.setPhpSynced(0);
    answerMapper.insert(answer);

    // 错题状态机
    Map<String, Object> cardChange = applyWrongCard(plan, platformUserId, detail, judged.correct());

    // 计数：改答案不重复计 done，只补作答次数与对错差额
    bumpDayCounters(day, roundType, judged.correct(), correction);
    touchLastActiveDate(plan, day);
    planMapper.updateById(plan);

    // 异步回写 PHP
    writebackService.writebackAsync(answer.getId());

    Map<String, Object> result = answerView(judged.correct(), options, detail.resolve(), cardChange);
    if (cardChange != null && Boolean.TRUE.equals(cardChange.get("explainAvailable"))) {
      result.put("explainAvailable", 1);
    }

    // D9 连对/连错即时鼓励（§15.3）：回放路径不触发（上方已 return），仅真实作答/改错触发
    StreakEncourageDomainService.Streak streak = computeDayStreak(day.getId());
    int tier = StreakEncourageDomainService.tierOf(streak);
    if (StreakEncourageDomainService.shouldFire(streak, tier)) {
      String text = llmAssistService.streakEncouragement(plan.getId(), day.getDayIndex(),
          streak.type(), tier, day.getAnswerCount() != null ? day.getAnswerCount() : 0);
      if (text != null) {
        Map<String, Object> streakView = new LinkedHashMap<>();
        streakView.put("type", streak.type());
        streakView.put("count", streak.count());
        result.put("streak", streakView);
        result.put("encourageText", text);
      }
    }
    return result;
  }

  /** 当日作答流水（含改错重答，按时间序）计算当前连对/连错（D9） */
  private StreakEncourageDomainService.Streak computeDayStreak(long dayId) {
    List<AiReviewAnswerEntity> dayAnswers = answerMapper.selectList(new LambdaQueryWrapper<AiReviewAnswerEntity>()
        .eq(AiReviewAnswerEntity::getDayId, dayId)
        .orderByAsc(AiReviewAnswerEntity::getCreatedAt)
        .orderByAsc(AiReviewAnswerEntity::getId));
    List<Integer> flags = new ArrayList<>(dayAnswers.size());
    for (AiReviewAnswerEntity a : dayAnswers) {
      flags.add(a.getIsCorrect() != null && a.getIsCorrect() == 1 ? 1 : 0);
    }
    return StreakEncourageDomainService.computeStreak(flags);
  }

  /** D7 §15.1-②：lastActiveDate 语义为"任务已覆盖到哪天"——超前完成的天按 plan_date 计，
   * 超前完成换来的休息天数不算中断 */
  private void touchLastActiveDate(AiReviewPlanEntity plan, AiReviewPlanDayEntity day) {
    LocalDate now = LocalDate.now();
    LocalDate covered = day.getPlanDate() != null && day.getPlanDate().isAfter(now) ? day.getPlanDate() : now;
    plan.setLastActiveDate(plan.getLastActiveDate() != null && plan.getLastActiveDate().isAfter(covered)
        ? plan.getLastActiveDate() : covered);
  }

  // ==================================================================
  // 7.5 开始模考
  // ==================================================================

  @Transactional
  public Map<String, Object> mockStart(long platformUserId, long examId, Long tenantId) {
    gateGrayRelease(examId, platformUserId, tenantId);
    AiReviewPlanEntity plan = requirePlan(platformUserId, examId);
    AiReviewMockPaperEntity paper = composeMockPaper(plan, tenantId);
    List<AiReviewMockPaperItemEntity> items = mockItemMapper.selectList(
        new LambdaQueryWrapper<AiReviewMockPaperItemEntity>()
            .eq(AiReviewMockPaperItemEntity::getPaperId, paper.getId())
            .orderByAsc(AiReviewMockPaperItemEntity::getSortNumber));
    List<Map<String, Object>> questions = new ArrayList<>();
    for (AiReviewMockPaperItemEntity item : items) {
      questions.add(Map.of("questionId", item.getQuestionId(),
          "type", item.getQuestionType() != null ? item.getQuestionType() : ""));
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("mockPaperId", String.valueOf(paper.getId()));
    result.put("questionCount", paper.getQuestionCount());
    result.put("timeLimitMin", paper.getTimeLimitMin());
    result.put("questions", questions);
    return result;
  }

  // ==================================================================
  // 7.6 模考交卷
  // ==================================================================

  @Transactional
  public Map<String, Object> mockSubmit(long platformUserId, long examId, long mockPaperId,
      List<Map<String, Object>> answers, Integer useTimeSec, Long tenantId) {
    gateGrayRelease(examId, platformUserId, tenantId);
    AiReviewPlanEntity plan = requirePlan(platformUserId, examId);
    AiReviewMockPaperEntity paper = mockPaperMapper.selectById(mockPaperId);
    if (paper == null || !paper.getPlanId().equals(plan.getId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "模考卷不存在");
    }
    if ("done".equals(paper.getStatus())) {
      return mockResultView(plan, paper);
    }
    List<AiReviewMockPaperItemEntity> items = mockItemMapper.selectList(
        new LambdaQueryWrapper<AiReviewMockPaperItemEntity>()
            .eq(AiReviewMockPaperItemEntity::getPaperId, mockPaperId));
    Map<Long, BigDecimal> scoreByQid = new HashMap<>();
    Map<Long, String> typeByQid = new HashMap<>();
    for (AiReviewMockPaperItemEntity item : items) {
      scoreByQid.put(item.getQuestionId(), item.getScore());
      typeByQid.put(item.getQuestionId(), item.getQuestionType());
    }
    Map<Long, String> userAnswerByQid = new LinkedHashMap<>();
    if (answers != null) {
      for (Map<String, Object> a : answers) {
        Object qid = a.get("questionId");
        if (qid != null) {
          userAnswerByQid.put(Long.parseLong(String.valueOf(qid)),
              a.get("userAnswer") != null ? String.valueOf(a.get("userAnswer")) : "");
        }
      }
    }
    Map<Long, PhpQuestionDetail> details = phpQuery.loadQuestionDetails(new ArrayList<>(scoreByQid.keySet()));

    // O7：模考多选严格判分开关（错选 0 分、少选按选项 score 部分给分）；练习轮仍与 PHP 宽松口径一致
    boolean strictJudge = dictInt("ai_review_mock_strict_judge", 1, tenantId) == 1;

    AiReviewPlanDayEntity todayDay = findDay(plan.getId(), plan.getCurrentDayIndex());
    BigDecimal score = BigDecimal.ZERO;
    int correctCount = 0;
    List<Map<String, Object>> wrongList = new ArrayList<>();
    LocalDate today = LocalDate.now();
    for (AiReviewMockPaperItemEntity item : items) {
      PhpQuestionDetail detail = details.get(item.getQuestionId());
      if (detail == null) {
        continue;
      }
      String userAnswer = userAnswerByQid.getOrDefault(item.getQuestionId(), "");
      List<AnswerJudgeDomainService.AnswerOption> options = parseOptions(detail.answerJson());
      AnswerJudgeDomainService.JudgeResult judged;
      if (strictJudge && "multi".equals(detail.type())) {
        judged = AnswerJudgeDomainService.judgeStrictMulti(options, parseUserMultiAnswer(userAnswer),
            item.getScore());
      } else {
        judged = AnswerJudgeDomainService.judge(detail.type(), options, userAnswer,
            parseUserMultiAnswer(userAnswer));
      }
      // 实得分：严格多选判分给 earnedScore；其余 对=题分/错=0
      BigDecimal earned;
      if (judged.earnedScore() != null) {
        earned = judged.earnedScore();
      } else {
        earned = judged.correct() ? (item.getScore() != null ? item.getScore() : BigDecimal.ZERO)
            : BigDecimal.ZERO;
      }
      if (judged.correct()) {
        correctCount++;
      }
      score = score.add(earned);
      if (!judged.correct()) {
        Map<String, Object> wrong = new LinkedHashMap<>();
        wrong.put("questionId", item.getQuestionId());
        wrong.put("correctAnswer", judged.correctAnswerText());
        wrong.put("resolve", detail.resolve());
        if (earned.compareTo(BigDecimal.ZERO) > 0) {
          wrong.put("partialScore", earned); // 少选部分分（展示用）
        }
        wrongList.add(wrong);
      }
      // 写作答（幂等键：计划+题+卷——同卷重复提交跳过，跨卷重答是合法新尝试）
      AiReviewAnswerEntity existing = answerMapper.selectOne(new LambdaQueryWrapper<AiReviewAnswerEntity>()
          .eq(AiReviewAnswerEntity::getPlanId, plan.getId())
          .eq(AiReviewAnswerEntity::getQuestionId, item.getQuestionId())
          .eq(AiReviewAnswerEntity::getRoundType, "mock")
          .eq(AiReviewAnswerEntity::getMockPaperId, mockPaperId)
          .last("LIMIT 1"));
      if (existing == null) {
        AiReviewAnswerEntity answer = new AiReviewAnswerEntity();
        answer.setPlanId(plan.getId());
        answer.setDayId(todayDay != null ? todayDay.getId() : null);
        answer.setMockPaperId(mockPaperId);
        answer.setPlatformUserId(platformUserId);
        answer.setExamPlanId(examId);
        answer.setQuestionId(item.getQuestionId());
        answer.setQuestionScore(item.getScore());
        answer.setEarnedScore(earned);
        answer.setKnowledgeId(detail.knowledgeId());
        answer.setQuestionType(detail.type());
        answer.setRoundType("mock");
        answer.setUserAnswer(userAnswer);
        answer.setIsCorrect(judged.correct() ? 1 : 0);
        answer.setUseTimeSec(0);
        answer.setAttemptSeq(countAttempts(plan.getId(), item.getQuestionId()) + 1);
        answer.setPhpSynced(0);
        answerMapper.insert(answer);
        applyWrongCard(plan, platformUserId, detail, judged.correct());
        writebackService.writebackAsync(answer.getId());
      }
    }
    paper.setScore(score);
    paper.setCorrectCount(correctCount);
    paper.setStatus("done");
    mockPaperMapper.updateById(paper);
    plan.setLastActiveDate(today);
    planMapper.updateById(plan);

    Map<String, Object> result = mockResultView(plan, paper);
    result.put("wrongList", wrongList);
    result.put("mustReadResolve", 1);
    return result;
  }

  // ==================================================================
  // 7.7 结束今日练习
  // ==================================================================

  @Transactional
  public Map<String, Object> finish(long platformUserId, long examId, long dayId,
      Integer useTimeSec, Long tenantId) {
    gateGrayRelease(examId, platformUserId, tenantId);
    AiReviewPlanEntity plan = requirePlan(platformUserId, examId);
    AiReviewPlanDayEntity day = requireDay(plan.getId(), dayId);

    // 同一天多次练习允许中途退出；只有全部完成才允许真正「结束」
    int newDone = day.getNewDone() != null ? day.getNewDone() : 0;
    int reviewDone = day.getReviewDone() != null ? day.getReviewDone() : 0;
    int newTarget = day.getNewTarget() != null ? day.getNewTarget() : 0;
    int reviewTarget = day.getReviewTarget() != null ? day.getReviewTarget() : 0;
    if (newDone < newTarget || reviewDone < reviewTarget) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          String.format("今日任务未完成（新题 %d/%d，错题复盘 %d/%d），请全部完成后再结束",
              newDone, newTarget, reviewDone, reviewTarget));
    }

    day.setStatus("done");
    dayMapper.updateById(day);
    // D7 §15.1-②：超前完成的天按 plan_date 覆盖，换来的休息天数不算中断
    touchLastActiveDate(plan, day);
    // 不再在这里推进 current_day_index；跨天进入时由 getOrMaterializeDay 按日期自动推进
    if (plan.getCurrentDayIndex() >= plan.getTotalDays()) {
      plan.setStatus("completed");
    }
    planMapper.updateById(plan);

    Map<String, Object> summary = new LinkedHashMap<>();
    summary.put("newDone", day.getNewDone());
    summary.put("reviewDone", day.getReviewDone());
    summary.put("accuracy", accuracy(day.getCorrectCount(), day.getAnswerCount()));
    summary.put("useTimeMin", useTimeSec != null ? Math.round(useTimeSec / 60.0) : 0);

    Map<String, Object> tomorrow = new LinkedHashMap<>();
    tomorrow.put("newTarget", plan.getStatus().equals("completed") ? 0 : estimateTomorrowNew(plan));
    tomorrow.put("reviewTarget", plan.getStatus().equals("completed") ? 0 : wrongMapper.selectCount(
        new LambdaQueryWrapper<AiReviewWrongItemEntity>()
            .eq(AiReviewWrongItemEntity::getPlanId, plan.getId())
            .eq(AiReviewWrongItemEntity::getRemoved, 0)));

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("summary", summary);
    result.put("tomorrowPreview", tomorrow);
    result.put("planStatus", plan.getStatus());
    // D7：前端据此显示「继续挑战：提前做明天的任务」按钮
    result.put("canAdvance", canAdvanceToday(plan, LocalDate.now()));
    result.put("encourageText", llmAssistService.getEncouragement(
        plan.getId(), day.getDayIndex(), day.getStage(), false));
    return result;
  }

  // ==================================================================
  // D7 超前学习：提前解锁明日任务（方案 §15.1）
  // ==================================================================

  /**
   * 提前解锁下一天任务。前置：当前日已 done、计划未完结、当日提前额度未满。
   * 下一天的 plan_date 仍写真实明天日期（日历语义不变），次日进入时自然复用。
   *
   * @return 新一天的任务清单（todayTask 同构视图）+ advanced=true
   */
  @Transactional
  public Map<String, Object> advanceDay(long platformUserId, long examId, Long tenantId) {
    gateGrayRelease(examId, platformUserId, tenantId);
    AiReviewPlanEntity plan = requirePlan(platformUserId, examId);
    LocalDate today = LocalDate.now();
    AiReviewPlanDayEntity current = findDay(plan.getId(), plan.getCurrentDayIndex());
    if (current == null || !"done".equals(current.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "请先完成今日任务，再挑战明天的任务");
    }
    if (!canAdvanceToday(plan, today)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          plan.getCurrentDayIndex() >= plan.getTotalDays()
              ? "计划已全部完成，每天回来清算错题保持手感就好"
              : "今日挑战次数已用完，明天再来吧，错题清算随时可以做");
    }
    int nextIndex = plan.getCurrentDayIndex() + 1;
    plan.setCurrentDayIndex(nextIndex);
    plan.setCurrentStage(stageOfDay(plan, nextIndex));
    planMapper.updateById(plan);
    AiReviewPlanDayEntity day = materializeDay(plan, nextIndex, today.plusDays(1), null);
    log.info("[AI复习] 超前解锁：planId={} user={} dayIndex={} planDate={}",
        plan.getId(), platformUserId, nextIndex, day.getPlanDate());

    Map<String, Object> result = todayTask(platformUserId, examId, tenantId);
    result.put("advanced", true);
    return result;
  }

  /** 当日是否还能提前解锁：计划未完结 + 当日提前次数 < ai_review_advance_max_per_day（默认 1） */
  private boolean canAdvanceToday(AiReviewPlanEntity plan, LocalDate today) {
    if (!"active".equals(plan.getStatus()) || plan.getCurrentDayIndex() >= plan.getTotalDays()) {
      return false;
    }
    int maxAdvance = dictInt("ai_review_advance_max_per_day", 1, plan.getTenantId());
    long advancedToday = dayMapper.selectCount(new LambdaQueryWrapper<AiReviewPlanDayEntity>()
        .eq(AiReviewPlanDayEntity::getPlanId, plan.getId())
        .apply("DATE(created_at) = CURDATE()")
        .gt(AiReviewPlanDayEntity::getPlanDate, today));
    return advancedToday < maxAdvance;
  }

  // ==================================================================
  // 7.8 学情总览
  // ==================================================================

  public Map<String, Object> progress(long platformUserId, long examId, Long tenantId) {
    gateGrayRelease(examId, platformUserId, tenantId);
    AiReviewPlanEntity plan = requirePlan(platformUserId, examId);
    List<AiReviewAnswerEntity> answers = answerMapper.selectList(new LambdaQueryWrapper<AiReviewAnswerEntity>()
        .eq(AiReviewAnswerEntity::getPlanId, plan.getId()));

    // 按 L3 汇总
    Map<Long, int[]> byL3 = new HashMap<>(); // knowledgeId -> [answers, correct]
    Map<String, int[]> byType = new HashMap<>();
    Set<Long> doneQuestionIds = new HashSet<>();
    for (AiReviewAnswerEntity a : answers) {
      if (a.getKnowledgeId() != null) {
        int[] counter = byL3.computeIfAbsent(a.getKnowledgeId(), k -> new int[2]);
        counter[0]++;
        if (a.getIsCorrect() == 1) {
          counter[1]++;
        }
      }
      String type = a.getQuestionType() != null ? a.getQuestionType() : "";
      int[] typeCounter = byType.computeIfAbsent(type, k -> new int[2]);
      typeCounter[0]++;
      if (a.getIsCorrect() == 1) {
        typeCounter[1]++;
      }
      doneQuestionIds.add(a.getQuestionId());
    }

    // knowledgeCoverage（必做池维度）
    List<Long> mustDo = parseIds(plan.getMustDoIds());
    List<Map<String, Object>> coverage = new ArrayList<>();
    if (!mustDo.isEmpty()) {
      Map<Long, PhpQuestion> metaById = questionMetaByIds(plan, mustDo);
      // 知识点标题：知识结构表（exam_knowledge_structure）title 字段
      Map<Long, String> titleById = new HashMap<>();
      for (PhpKnowledgeNode node : phpQuery.loadKnowledgeTree(plan.getCourseId())) {
        titleById.put(node.id(), node.title());
      }
      Map<Long, int[]> l3Totals = new HashMap<>();
      for (Long qid : mustDo) {
        PhpQuestion meta = metaById.get(qid);
        if (meta != null && meta.knowledgeId() != null) {
          int[] counter = l3Totals.computeIfAbsent(meta.knowledgeId(), k -> new int[2]);
          counter[0]++;
          if (doneQuestionIds.contains(qid)) {
            counter[1]++;
          }
        }
      }
      List<Map.Entry<Long, int[]>> entries = new ArrayList<>(l3Totals.entrySet());
      entries.sort(Map.Entry.comparingByKey());
      for (Map.Entry<Long, int[]> entry : entries) {
        int[] answered = byL3.getOrDefault(entry.getKey(), new int[2]);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("knowledgeId", entry.getKey());
        item.put("title", titleById.getOrDefault(entry.getKey(), "知识点 " + entry.getKey()));
        item.put("total", entry.getValue()[0]);
        item.put("done", entry.getValue()[1]);
        // 覆盖率 = 已做题数 / 该知识点总题数；accuracy 为正确率（含改错前的首次作答）
        item.put("coverage", accuracy(entry.getValue()[1], entry.getValue()[0]));
        item.put("accuracy", accuracy(answered[1], answered[0]));
        coverage.add(item);
      }
    }

    // 考核项目掌握度（权重快照）+ 预估得分
    List<Map<String, Object>> itemMastery = new ArrayList<>();
    Double estimatedScore = null;
    Map<Long, Long> l1ByL3 = l1ByL3OfPlan(plan);
    if (plan.getWeightSnapshot() != null && !"count".equals(plan.getWeightSource())) {
      double score = 0;
      for (Map<String, Object> item : parseSnapshot(plan.getWeightSnapshot())) {
        long l1 = ((Number) item.get("knowledgeId")).longValue();
        double weight = ((Number) item.get("rate")).doubleValue() / 100.0;
        int answeredCount = 0;
        int correctCount = 0;
        for (Map.Entry<Long, int[]> entry : byL3.entrySet()) {
          if (l1 == l1ByL3.getOrDefault(entry.getKey(), -1L)) {
            answeredCount += entry.getValue()[0];
            correctCount += entry.getValue()[1];
          }
        }
        double mastery = answeredCount == 0 ? 0 : correctCount / (double) answeredCount;
        score += weight * mastery;
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("item", item.get("title"));
        view.put("rate", item.get("rate"));
        view.put("mastery", Math.round(mastery * 1000) / 10.0);
        itemMastery.add(view);
      }
      estimatedScore = Math.round(score * 1000) / 10.0;
    }

    Map<String, Object> typeAccuracy = new LinkedHashMap<>();
    for (String type : List.of("single", "multi", "judge")) {
      int[] counter = byType.getOrDefault(type, new int[2]);
      typeAccuracy.put(type, accuracy(counter[1], counter[0]));
    }

    List<AiReviewPlanDayEntity> days = dayMapper.selectList(new LambdaQueryWrapper<AiReviewPlanDayEntity>()
        .eq(AiReviewPlanDayEntity::getPlanId, plan.getId())
        .orderByAsc(AiReviewPlanDayEntity::getDayIndex));
    List<Map<String, Object>> dailyTrend = new ArrayList<>();
    for (AiReviewPlanDayEntity day : days) {
      if (day.getAnswerCount() != null && day.getAnswerCount() > 0) {
        dailyTrend.add(Map.of(
            "date", day.getPlanDate().toString(),
            "newDone", day.getNewDone(),
            "reviewDone", day.getReviewDone(),
            "accuracy", accuracy(day.getCorrectCount(), day.getAnswerCount())));
      }
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("knowledgeCoverage", coverage);
    result.put("typeAccuracy", typeAccuracy);
    result.put("wrongStats", wrongStatsView(plan.getId()));
    result.put("dailyTrend", dailyTrend);
    result.put("itemMastery", itemMastery);
    result.put("estimatedScore", estimatedScore);
    result.put("weekReport", null);
    return result;
  }

  // ==================================================================
  // 计划生成（§5.1）
  // ==================================================================

  private AiReviewPlanEntity getOrCreatePlan(long platformUserId, long examId, Long tenantId) {
    AiReviewPlanEntity existing = planMapper.selectOne(new LambdaQueryWrapper<AiReviewPlanEntity>()
        .eq(AiReviewPlanEntity::getPlatformUserId, platformUserId)
        .eq(AiReviewPlanEntity::getExamId, examId)
        .last("LIMIT 1"));
    if (existing != null) {
      if ("abandoned".equals(existing.getStatus())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "该复习计划已放弃");
      }
      return existing;
    }
    return createPlan(platformUserId, examId, tenantId);
  }

  private AiReviewPlanEntity createPlan(long platformUserId, long examId, Long tenantId) {
    if (!phpQuery.isUserInExam(examId, platformUserId)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "您不在该复习计划名单中");
    }
    PhpExamPlanInfo planInfo = phpQuery.loadExamPlanInfo(examId);
    if (planInfo == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "复习计划不存在");
    }
    if (planInfo.bindings().isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "该计划没有正确绑定工种");
    }
    List<Long> courseIds = planInfo.bindings().stream()
        .map(PhpExamPlanInfo.WorkTypeBinding::courseId)
        .filter(id -> id != null && id > 0)
        .distinct()
        .toList();
    if (courseIds.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "该计划没有正确绑定工种（未匹配到科目）");
    }
    List<Long> l3Ids = phpQuery.loadL3KnowledgeIds(courseIds);
    int source = planInfo.source() != null ? planInfo.source() : 0;
    List<PhpQuestion> questions = phpQuery.loadQuestionScope(source, l3Ids);
    if (questions.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "该计划未绑定有效题库");
    }

    LocalDate today = LocalDate.now();
    int defaultDays = dictInt("ai_review_default_days", 20, tenantId);
    int dailyNewCap = dictInt("ai_review_daily_new_cap", 120, tenantId);
    int dailyTotalCap = dictInt("ai_review_daily_total_cap", 200, tenantId);
    LocalDate examDate = PlanScheduleDomainService.resolveExamDate(
        planInfo.examDate(), parseDateList(planInfo.examDateList()), today);
    int totalDays = PlanScheduleDomainService.computeTotalDays(examDate, today, defaultDays);
    int[] stages = PlanScheduleDomainService.splitStages(totalDays);

    long primaryCourseId = courseIds.get(0);
    PhpCourseWeights courseWeights = phpQuery.loadCourseWeights(primaryCourseId);
    List<PhpKnowledgeNode> tree = phpQuery.loadKnowledgeTree(primaryCourseId);
    WeightResolver.ResolvedWeights weights = WeightResolver.resolve(courseWeights, tree, questions);
    if ("count".equals(weights.weightSource())) {
      log.warn("[AI复习] course={} 无权重配置，按题量占比兜底", primaryCourseId);
    }

    int target = Math.min(questions.size(), stages[0] * dailyNewCap);
    boolean trimmed = questions.size() > target;
    List<Long> mustDoIds = trimmed
        ? QuestionSelectDomainService.trimQuestionBank(questions, tree, weights, target)
        : questions.stream().map(PhpQuestion::id).toList();

    AiReviewPlanEntity plan = new AiReviewPlanEntity();
    plan.setPlatformUserId(platformUserId);
    plan.setExamId(examId);
    PhpExamPlanInfo.WorkTypeBinding firstBinding = planInfo.bindings().get(0);
    plan.setWorkTypeId(firstBinding.workTypeId());
    plan.setLevel(firstBinding.level() != null ? String.valueOf(firstBinding.level()) : null);
    plan.setCourseId(primaryCourseId);
    plan.setSource(source);
    plan.setWeightSnapshot(writeJson(WeightResolver.toSnapshot(weights)));
    plan.setWeightSource(weights.weightSource());
    plan.setExamDate(examDate);
    plan.setStartDate(today);
    plan.setTotalDays(totalDays);
    plan.setStage1Days(stages[0]);
    plan.setStage2Days(stages[1]);
    plan.setStage3Days(stages[2]);
    plan.setTotalQuestions(questions.size());
    plan.setTargetQuestions(mustDoIds.size());
    plan.setMustDoIds(writeJson(mustDoIds));
    plan.setTrimmed(trimmed ? 1 : 0);
    plan.setDailyNewCap(dailyNewCap);
    plan.setDailyTotalCap(dailyTotalCap);
    plan.setCurrentStage(1);
    plan.setCurrentDayIndex(1);
    plan.setInterruptCount(0);
    plan.setStatus("active");
    plan.setTenantId(tenantId);
    planMapper.insert(plan);
    log.info("[AI复习] 计划生成：planId={} user={} exam={} 总题={} 必做={} 裁剪={} 天数={}({}/{}/{}) 权重={}",
        plan.getId(), platformUserId, examId, questions.size(), mustDoIds.size(), trimmed,
        totalDays, stages[0], stages[1], stages[2], weights.weightSource());
    return plan;
  }

  // ==================================================================
  // 每日物化（§5.2）
  // ==================================================================

  private AiReviewPlanDayEntity getOrMaterializeDay(AiReviewPlanEntity plan, int dayIndex,
      LocalDate date, InterruptInfo interrupt) {
    AiReviewPlanDayEntity existing = findDay(plan.getId(), dayIndex);
    if (existing != null) {
      // 同一天多次进入：直接复用当天记录，碎片时间练习累计到同一天
      if (existing.getPlanDate().equals(date)) {
        if (interrupt != null && interrupt.strategyText() != null
            && (existing.getStrategyNote() == null || existing.getStrategyNote().isBlank())) {
          existing.setStrategyNote(interrupt.strategyText());
          dayMapper.updateById(existing);
        }
        return existing;
      }
      // 跨天进入：把指针推进到下一天，再物化新一天
      if (existing.getPlanDate().isBefore(date)) {
        int nextIndex = dayIndex + 1;
        if (nextIndex <= plan.getTotalDays()) {
          plan.setCurrentDayIndex(nextIndex);
          plan.setCurrentStage(stageOfDay(plan, nextIndex));
          planMapper.updateById(plan);
          return getOrMaterializeDay(plan, nextIndex, date, interrupt);
        }
      }
      return existing;
    }
    return materializeDay(plan, dayIndex, date, interrupt);
  }

  /** 物化指定天的任务（每日 04:00 job 与 overview 懒物化共用） */
  @Transactional
  public AiReviewPlanDayEntity materializeDay(AiReviewPlanEntity plan, int dayIndex,
      LocalDate date, InterruptInfo interrupt) {
    int stage = stageOfDay(plan, dayIndex);
    List<Long> mustDo = parseIds(plan.getMustDoIds());
    Set<Long> assignedNew = assignedNewIds(plan.getId());
    List<Long> remainingNew = new ArrayList<>();
    for (Long id : mustDo) {
      if (!assignedNew.contains(id)) {
        remainingNew.add(id);
      }
    }

    AiReviewPlanDayEntity day = new AiReviewPlanDayEntity();
    day.setPlanId(plan.getId());
    day.setDayIndex(dayIndex);
    day.setPlanDate(date);
    day.setStage(stage);
    day.setStatus("pending");
    day.setNewDone(0);
    day.setReviewDone(0);
    day.setCorrectCount(0);
    day.setAnswerCount(0);

    if (interrupt != null && interrupt.caseName() == PlanScheduleDomainService.InterruptCase.A) {
      // 情况 A 恢复第 1 天：预热 80 题，仅单选+判断
      materializeStage1(day, plan, remainingNew, PlanScheduleDomainService.CASE_A_PREHEAT_NEW, true);
      day.setStrategyNote(interrupt.strategyText());
    } else if (stage == 1) {
      int remainingDays = plan.getStage1Days() - dayIndex + 1;
      int newTarget = PlanScheduleDomainService.stage1NewTarget(
          remainingNew.size(), remainingDays, plan.getDailyNewCap());
      materializeStage1(day, plan, remainingNew, newTarget, false);
      if (interrupt != null) {
        day.setStrategyNote(interrupt.strategyText());
      }
    } else if (stage == 2) {
      materializeStage2(day, plan, remainingNew);
      if (interrupt != null) {
        day.setStrategyNote(interrupt.strategyText());
      }
    } else {
      // 阶段 3：组模考卷
      AiReviewMockPaperEntity paper = composeMockPaper(plan, plan.getTenantId());
      day.setMockPaperId(paper.getId());
      day.setNewTarget(0);
      day.setReviewTarget(paper.getQuestionCount());
      day.setNewQuestionIds("[]");
      day.setReviewQuestionIds("[]");
      if (interrupt != null) {
        day.setStrategyNote(interrupt.strategyText());
      }
    }
    day.setTenantId(plan.getTenantId());
    dayMapper.insert(day);

    // 异步生成鼓励语
    llmAssistService.generateAsync(plan.getId(), dayIndex, stage, null, null,
        plan.getTotalDays(), null, interrupt != null, plan.getTenantId());
    return day;
  }

  /** 阶段 1 物化（含 D6 权重双轨构成与总量封顶） */
  private void materializeStage1(AiReviewPlanDayEntity day, AiReviewPlanEntity plan,
      List<Long> remainingNew, int newTargetRaw, boolean preheatSingleJudgeOnly) {
    WeightResolver.ResolvedWeights weights = weightsOfPlan(plan);
    Map<Long, PhpQuestion> metaById = questionMetaByIds(plan, remainingNew);

    List<Long> candidateIds = remainingNew;
    if (preheatSingleJudgeOnly) {
      candidateIds = new ArrayList<>();
      for (Long id : remainingNew) {
        PhpQuestion meta = metaById.get(id);
        if (meta != null && ("single".equals(meta.type()) || "judge".equals(meta.type()))) {
          candidateIds.add(id);
        }
      }
    }

    // 复盘队列：红牌（加星置顶）→ 黄牌，按 lastWrongTime 倒序
    List<AiReviewWrongItemEntity> activeWrong = activeWrongItems(plan.getId());
    int reviewTarget = activeWrong.size();
    int[] capped = PlanScheduleDomainService.applyTotalCap(newTargetRaw, reviewTarget, plan.getDailyTotalCap());
    int newTarget = Math.min(capped[0], candidateIds.size());
    int reviewCap = capped[1];

    // 双轨构成
    Map<Long, Integer> remainingByL1 = new LinkedHashMap<>();
    for (Long id : candidateIds) {
      PhpQuestion meta = metaById.get(id);
      Long l1 = meta != null ? weights.l1ByL3().get(meta.knowledgeId()) : null;
      if (l1 != null) {
        remainingByL1.merge(l1, 1, Integer::sum);
      }
    }
    int remainingDays = Math.max(1, plan.getStage1Days() - day.getDayIndex() + 1);
    Map<Long, Integer> composition = PlanScheduleDomainService.stage1Composition(
        remainingByL1, weights.l1Weights(), newTarget, remainingDays);

    // 按构成从各 L1 取题（池顺序优先重要度高的 L3）
    Map<Long, List<Long>> idsByL1 = new LinkedHashMap<>();
    for (Long id : candidateIds) {
      PhpQuestion meta = metaById.get(id);
      Long l1 = meta != null ? weights.l1ByL3().get(meta.knowledgeId()) : null;
      if (l1 != null) {
        idsByL1.computeIfAbsent(l1, k -> new ArrayList<>()).add(id);
      }
    }
    List<Long> newIds = new ArrayList<>();
    for (Map.Entry<Long, Integer> entry : composition.entrySet()) {
      List<Long> group = idsByL1.getOrDefault(entry.getKey(), List.of());
      for (int i = 0; i < Math.min(entry.getValue(), group.size()); i++) {
        newIds.add(group.get(i));
      }
    }
    // 兜底（D6）：权重无法映射（配置错位/树缺失）导致构成为空 → 覆盖优先、不配比
    if (newIds.isEmpty() && newTarget > 0) {
      for (int i = 0; i < Math.min(newTarget, candidateIds.size()); i++) {
        newIds.add(candidateIds.get(i));
      }
      log.warn("[AI复习] plan={} 权重映射失败，本日新题按池顺序兜底 {} 题", plan.getId(), newIds.size());
    }
    // 阶段1最后一天强制清空剩余必做（覆盖优先硬约束）
    if (day.getDayIndex() >= plan.getStage1Days() && !preheatSingleJudgeOnly) {
      Set<Long> chosen = new HashSet<>(newIds);
      for (Long id : candidateIds) {
        if (!chosen.contains(id)) {
          newIds.add(id);
        }
      }
    }

    List<Long> reviewIds = new ArrayList<>();
    for (int i = 0; i < Math.min(reviewCap, activeWrong.size()); i++) {
      reviewIds.add(activeWrong.get(i).getQuestionId());
    }

    day.setNewQuestionIds(writeJson(newIds));
    day.setReviewQuestionIds(writeJson(reviewIds));
    day.setNewTarget(newIds.size());
    day.setReviewTarget(reviewIds.size());
  }

  /** 阶段 2 物化：复盘全部未移除错题（≤300）+ 薄弱 L3 新题（≤50） */
  private void materializeStage2(AiReviewPlanDayEntity day, AiReviewPlanEntity plan,
      List<Long> remainingNew) {
    List<AiReviewWrongItemEntity> activeWrong = activeWrongItems(plan.getId());
    List<Long> reviewIds = new ArrayList<>();
    for (int i = 0; i < Math.min(PlanScheduleDomainService.STAGE2_REVIEW_CAP, activeWrong.size()); i++) {
      reviewIds.add(activeWrong.get(i).getQuestionId());
    }

    // 薄弱 L3：按错题率倒序（错题本 wrong/(wrong+correct)）
    Map<Long, Double> wrongRateByL3 = wrongRateByL3(plan.getId());
    WeightResolver.ResolvedWeights weights = weightsOfPlan(plan);
    Map<Long, PhpQuestion> metaById = questionMetaByIds(plan, remainingNew);
    List<Long> sortedRemaining = new ArrayList<>(remainingNew);
    sortedRemaining.sort(Comparator.comparingDouble((Long id) -> {
      PhpQuestion meta = metaById.get(id);
      double rate = meta != null ? wrongRateByL3.getOrDefault(meta.knowledgeId(), 0.0) : 0.0;
      Long l1 = meta != null ? weights.l1ByL3().get(meta.knowledgeId()) : null;
      double weight = l1 != null ? weights.l1Weights().getOrDefault(l1, 0.0) : 0.0;
      return -(rate * 10 + weight); // 错题率为主，权重 tie-breaker
    }));
    List<Long> newIds = new ArrayList<>();
    for (int i = 0; i < Math.min(PlanScheduleDomainService.STAGE2_NEW_CAP, sortedRemaining.size()); i++) {
      newIds.add(sortedRemaining.get(i));
    }

    day.setNewQuestionIds(writeJson(newIds));
    day.setReviewQuestionIds(writeJson(reviewIds));
    day.setNewTarget(newIds.size());
    day.setReviewTarget(reviewIds.size());
  }

  // ==================================================================
  // 模考组卷（§5.6）
  // ==================================================================

  private AiReviewMockPaperEntity composeMockPaper(AiReviewPlanEntity plan, Long tenantId) {
    List<Long> mustDo = parseIds(plan.getMustDoIds());
    Map<Long, PhpQuestion> metaById = questionMetaByIds(plan, mustDo);
    List<PhpQuestion> pool = new ArrayList<>();
    for (Long id : mustDo) {
      PhpQuestion meta = metaById.get(id);
      if (meta != null) {
        pool.add(meta);
      }
    }
    WeightResolver.ResolvedWeights weights = weightsOfPlan(plan);
    Map<Long, Double> wrongRateByL3 = wrongRateByL3(plan.getId());
    int mockSize = dictInt("ai_review_mock_size", 100, tenantId);
    List<Long> questionIds = QuestionSelectDomainService.composeMockPaper(
        pool, weights, mockSize, wrongRateByL3, new Random());

    int paperNo = mockPaperMapper.selectCount(new LambdaQueryWrapper<AiReviewMockPaperEntity>()
        .eq(AiReviewMockPaperEntity::getPlanId, plan.getId())).intValue() + 1;
    BigDecimal total = BigDecimal.ZERO;
    for (Long qid : questionIds) {
      PhpQuestion meta = metaById.get(qid);
      if (meta != null && meta.score() != null) {
        total = total.add(meta.score());
      }
    }
    AiReviewMockPaperEntity paper = new AiReviewMockPaperEntity();
    paper.setPlanId(plan.getId());
    paper.setPaperNo(paperNo);
    paper.setQuestionCount(questionIds.size());
    paper.setTotalScore(total);
    paper.setPassScore(total.multiply(new BigDecimal("0.6")).setScale(1, RoundingMode.HALF_UP));
    paper.setTimeLimitMin(Math.max(30, questionIds.size()));
    paper.setStatus("ready");
    paper.setTenantId(tenantId);
    mockPaperMapper.insert(paper);

    int sort = 1;
    for (Long qid : questionIds) {
      PhpQuestion meta = metaById.get(qid);
      AiReviewMockPaperItemEntity item = new AiReviewMockPaperItemEntity();
      item.setPaperId(paper.getId());
      item.setQuestionId(qid);
      item.setQuestionType(meta != null ? meta.type() : null);
      item.setSortNumber(sort++);
      item.setScore(meta != null && meta.score() != null ? meta.score() : BigDecimal.ZERO);
      item.setTenantId(tenantId);
      mockItemMapper.insert(item);
    }
    return paper;
  }

  // ==================================================================
  // 中断检测与补救（§5.5）
  // ==================================================================

  /** 中断信息（case + 展示文案） */
  public record InterruptInfo(PlanScheduleDomainService.InterruptCase caseName, String strategyText) {
  }

  private InterruptInfo applyInterruptIfNeeded(AiReviewPlanEntity plan, LocalDate today) {
    int gapDays = dictInt("ai_review_interrupt_gap", 2, plan.getTenantId());
    PlanScheduleDomainService.InterruptCase interruptCase = PlanScheduleDomainService.detectInterruptCase(
        plan.getCurrentStage(), plan.getLastActiveDate(), today, gapDays,
        plan.getInterruptCount() != null ? plan.getInterruptCount() : 0);
    if (interruptCase == null) {
      return null;
    }
    plan.setInterruptCount((plan.getInterruptCount() != null ? plan.getInterruptCount() : 0) + 1);
    planMapper.updateById(plan);
    washWrongItems(plan.getId(), today);
    String strategyText = switch (interruptCase) {
      case A -> "检测到您中断了几天。别慌：今天先只做 80 道单选和判断题热热身，明天起我们把剩余题量平摊到剩余天数。"
          + "实在刷不完的偏题怪题会帮您放弃，保住 80% 基础题就赢了。";
      case B -> "检测到您中断了几天。今天先做一次 100 题摸底测验：正确率 ≥70% 就直接进模考冲刺；"
          + "不达标的话，未来 3 天只磕红牌错题，剩余新题全部放弃，不再占用大脑内存。";
      case C -> "冲刺期中断不用怕：从今天起每天 1 套全真模拟卷，交卷后请逐题看解析，"
          + "把每道题的四个选项为什么对、为什么错想明白，比乱刷 300 题有效 3 倍。";
      case D -> "最近学习节奏有些断续，已为您切换 3 日滚动记忆法：今天先重做上次最后 50 题热车，"
          + "明天起每天固定 100 新题拉长战线。做过的题正确率稳住，及格没问题。";
    };
    log.info("[AI复习] 中断补救触发：planId={} case={} stage={} count={}",
        plan.getId(), interruptCase, plan.getCurrentStage(), plan.getInterruptCount());
    return new InterruptInfo(interruptCase, strategyText);
  }

  /** 残酷清洗：近 14 天移除的绿牌回黄；在册红/黄牌连续计数清零 */
  private void washWrongItems(long planId, LocalDate today) {
    List<AiReviewWrongItemEntity> items = wrongMapper.selectList(new LambdaQueryWrapper<AiReviewWrongItemEntity>()
        .eq(AiReviewWrongItemEntity::getPlanId, planId));
    for (AiReviewWrongItemEntity item : items) {
      long removedDaysAgo = -1;
      if (item.getRemoved() == 1 && item.getRemoveTime() != null) {
        removedDaysAgo = java.time.temporal.ChronoUnit.DAYS.between(
            item.getRemoveTime().toLocalDate(), today);
      }
      WrongCardDomainService.CardState state = toCardState(item);
      WrongCardDomainService.CardTransition transition =
          WrongCardDomainService.onInterruptWash(state, today, removedDaysAgo);
      if (transition.levelChanged() || transition.consecutiveCorrect() != item.getConsecutiveCorrect()) {
        item.setCardLevel(transition.toLevel());
        item.setConsecutiveCorrect(transition.consecutiveCorrect());
        if (WrongCardDomainService.YELLOW.equals(transition.toLevel()) && item.getRemoved() == 1) {
          item.setRemoved(0);
          item.setRemoveTime(null);
        }
        wrongMapper.updateById(item);
      }
    }
  }

  // ==================================================================
  // 错题状态机应用
  // ==================================================================

  private Map<String, Object> applyWrongCard(AiReviewPlanEntity plan, long platformUserId,
      PhpQuestionDetail detail, boolean correct) {
    AiReviewWrongItemEntity item = wrongMapper.selectOne(new LambdaQueryWrapper<AiReviewWrongItemEntity>()
        .eq(AiReviewWrongItemEntity::getPlanId, plan.getId())
        .eq(AiReviewWrongItemEntity::getQuestionId, detail.id())
        .last("LIMIT 1"));
    WrongCardDomainService.CardState state = toCardState(item);
    LocalDate today = LocalDate.now();
    WrongCardDomainService.CardTransition transition = correct
        ? WrongCardDomainService.onCorrect(state, today)
        : WrongCardDomainService.onWrong(state, today);

    if (item == null) {
      if (correct) {
        return null; // 答对且无错题记录：不入册
      }
      item = new AiReviewWrongItemEntity();
      item.setPlanId(plan.getId());
      item.setPlatformUserId(platformUserId);
      item.setQuestionId(detail.id());
      item.setKnowledgeId(detail.knowledgeId());
      item.setCardLevel(transition.toLevel());
      item.setStar(transition.star() ? 1 : 0);
      item.setConsecutiveCorrect(transition.consecutiveCorrect());
      item.setLastCorrectDate(transition.lastCorrectDate());
      item.setWrongCount(1);
      item.setCorrectCount(0);
      item.setFirstWrongTime(LocalDateTime.now());
      item.setLastWrongTime(LocalDateTime.now());
      item.setRemoved(0);
      item.setTenantId(plan.getTenantId());
      wrongMapper.insert(item);
    } else {
      item.setCardLevel(transition.toLevel());
      item.setStar(transition.star() ? 1 : 0);
      item.setConsecutiveCorrect(transition.consecutiveCorrect());
      if (transition.lastCorrectDate() != null) {
        item.setLastCorrectDate(transition.lastCorrectDate());
      }
      if (correct) {
        item.setCorrectCount((item.getCorrectCount() != null ? item.getCorrectCount() : 0) + 1);
      } else {
        item.setWrongCount((item.getWrongCount() != null ? item.getWrongCount() : 0) + 1);
        item.setLastWrongTime(LocalDateTime.now());
      }
      if (transition.removed()) {
        item.setRemoved(1);
        item.setRemoveTime(LocalDateTime.now());
      }
      wrongMapper.updateById(item);
    }

    if (transition.levelChanged() || transition.star()) {
      Map<String, Object> change = new LinkedHashMap<>();
      change.put("from", transition.fromLevel());
      change.put("to", transition.toLevel());
      change.put("star", transition.star() ? 1 : 0);
      change.put("explainAvailable", transition.star() && !correct);
      return change;
    }
    return null;
  }

  private static WrongCardDomainService.CardState toCardState(AiReviewWrongItemEntity item) {
    if (item == null) {
      return WrongCardDomainService.CardState.none();
    }
    return new WrongCardDomainService.CardState(
        true,
        item.getCardLevel(),
        item.getStar() != null && item.getStar() == 1,
        item.getConsecutiveCorrect() != null ? item.getConsecutiveCorrect() : 0,
        item.getLastCorrectDate(),
        item.getRemoved() != null && item.getRemoved() == 1);
  }

  // ==================================================================
  // 定时任务入口（每日物化 / 周日清理）
  // ==================================================================

  /** 每日 04:00：遍历 active 计划，中断检测 + 物化当日任务 */
  public int materializeAllActivePlans(LocalDate today) {
    List<AiReviewPlanEntity> plans = planMapper.selectList(new LambdaQueryWrapper<AiReviewPlanEntity>()
        .eq(AiReviewPlanEntity::getStatus, "active"));
    int done = 0;
    for (AiReviewPlanEntity plan : plans) {
      try {
        InterruptInfo interrupt = applyInterruptIfNeeded(plan, today);
        if (findDay(plan.getId(), plan.getCurrentDayIndex()) == null
            && plan.getCurrentDayIndex() <= plan.getTotalDays()) {
          materializeDay(plan, plan.getCurrentDayIndex(), today, interrupt);
        }
        done++;
      } catch (Exception e) {
        log.error("[AI复习] 每日物化失败：planId={} error={}", plan.getId(), e.getMessage(), e);
      }
    }
    return done;
  }

  /** 周日清理日：错题一致性兜底扫描（状态机实时已执行，此处仅复核统计） */
  public Map<String, Integer> weeklyCleanup() {
    Integer red = wrongMapper.selectCount(new LambdaQueryWrapper<AiReviewWrongItemEntity>()
        .eq(AiReviewWrongItemEntity::getCardLevel, "red")
        .eq(AiReviewWrongItemEntity::getRemoved, 0)).intValue();
    Integer yellow = wrongMapper.selectCount(new LambdaQueryWrapper<AiReviewWrongItemEntity>()
        .eq(AiReviewWrongItemEntity::getCardLevel, "yellow")
        .eq(AiReviewWrongItemEntity::getRemoved, 0)).intValue();
    log.info("[AI复习] 周日清理日复核：在册红牌={} 黄牌={}", red, yellow);
    return Map.of("red", red, "yellow", yellow);
  }

  // ==================================================================
  // 内部工具
  // ==================================================================

  private AiReviewPlanEntity requirePlan(long platformUserId, long examId) {
    AiReviewPlanEntity plan = planMapper.selectOne(new LambdaQueryWrapper<AiReviewPlanEntity>()
        .eq(AiReviewPlanEntity::getPlatformUserId, platformUserId)
        .eq(AiReviewPlanEntity::getExamId, examId)
        .last("LIMIT 1"));
    if (plan == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "复习计划不存在，请先进入同步练习");
    }
    return plan;
  }

  private AiReviewPlanDayEntity requireDay(long planId, long dayId) {
    AiReviewPlanDayEntity day = dayMapper.selectById(dayId);
    if (day == null || !day.getPlanId().equals(planId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "每日任务不存在");
    }
    return day;
  }

  private AiReviewPlanDayEntity findDay(long planId, int dayIndex) {
    return dayMapper.selectOne(new LambdaQueryWrapper<AiReviewPlanDayEntity>()
        .eq(AiReviewPlanDayEntity::getPlanId, planId)
        .eq(AiReviewPlanDayEntity::getDayIndex, dayIndex)
        .last("LIMIT 1"));
  }

  private int stageOfDay(AiReviewPlanEntity plan, int dayIndex) {
    if (dayIndex <= plan.getStage1Days()) {
      return 1;
    }
    if (dayIndex <= plan.getStage1Days() + plan.getStage2Days()) {
      return 2;
    }
    return 3;
  }

  private Set<Long> assignedNewIds(long planId) {
    List<AiReviewPlanDayEntity> days = dayMapper.selectList(new LambdaQueryWrapper<AiReviewPlanDayEntity>()
        .eq(AiReviewPlanDayEntity::getPlanId, planId));
    Set<Long> assigned = new HashSet<>();
    for (AiReviewPlanDayEntity day : days) {
      assigned.addAll(parseIds(day.getNewQuestionIds()));
    }
    return assigned;
  }

  private Set<Long> answeredIdsOfDay(long dayId) {
    List<AiReviewAnswerEntity> answers = answerMapper.selectList(new LambdaQueryWrapper<AiReviewAnswerEntity>()
        .eq(AiReviewAnswerEntity::getDayId, dayId));
    Set<Long> ids = new HashSet<>();
    for (AiReviewAnswerEntity a : answers) {
      ids.add(a.getQuestionId());
    }
    return ids;
  }

  private int countAttempts(long planId, long questionId) {
    return answerMapper.selectCount(new LambdaQueryWrapper<AiReviewAnswerEntity>()
        .eq(AiReviewAnswerEntity::getPlanId, planId)
        .eq(AiReviewAnswerEntity::getQuestionId, questionId)).intValue();
  }

  private void bumpDayCounters(AiReviewPlanDayEntity day, String roundType, boolean correct, boolean correction) {
    if (!correction) {
      if ("new".equals(roundType)) {
        day.setNewDone((day.getNewDone() != null ? day.getNewDone() : 0) + 1);
      } else if ("review".equals(roundType)) {
        day.setReviewDone((day.getReviewDone() != null ? day.getReviewDone() : 0) + 1);
      }
    }
    day.setAnswerCount((day.getAnswerCount() != null ? day.getAnswerCount() : 0) + 1);
    if (correct) {
      // 改答案前次必为答错，差额恒 +1；首答按实际
      day.setCorrectCount((day.getCorrectCount() != null ? day.getCorrectCount() : 0) + 1);
    }
    if ("pending".equals(day.getStatus())) {
      day.setStatus("in_progress");
    }
    dayMapper.updateById(day);
  }

  private List<AiReviewWrongItemEntity> activeWrongItems(long planId) {
    List<AiReviewWrongItemEntity> items = wrongMapper.selectList(new LambdaQueryWrapper<AiReviewWrongItemEntity>()
        .eq(AiReviewWrongItemEntity::getPlanId, planId)
        .eq(AiReviewWrongItemEntity::getRemoved, 0));
    items.sort(Comparator
        .comparingInt((AiReviewWrongItemEntity w) -> w.getStar() != null && w.getStar() == 1 ? 0 : 1)
        .thenComparingInt(w -> "red".equals(w.getCardLevel()) ? 0 : 1)
        .thenComparing(AiReviewWrongItemEntity::getLastWrongTime,
            Comparator.nullsLast(Comparator.reverseOrder())));
    return items;
  }

  /** 当日复盘队列（动态）：按当前在册错题实时计算，当天新错题立即进入清算 */
  private List<AiReviewWrongItemEntity> reviewListForDay(AiReviewPlanEntity plan, AiReviewPlanDayEntity day) {
    List<AiReviewWrongItemEntity> activeWrong = activeWrongItems(plan.getId());
    int stage = day.getStage() != null ? day.getStage() : 1;
    int cap;
    if (stage == 1) {
      int[] capped = PlanScheduleDomainService.applyTotalCap(
          day.getNewTarget() != null ? day.getNewTarget() : 0,
          activeWrong.size(),
          plan.getDailyTotalCap() != null ? plan.getDailyTotalCap() : 200);
      cap = capped[1];
    } else if (stage == 2) {
      cap = PlanScheduleDomainService.STAGE2_REVIEW_CAP;
    } else {
      cap = 0;
    }
    return activeWrong.subList(0, Math.min(cap, activeWrong.size()));
  }

  private Map<Long, Double> wrongRateByL3(long planId) {
    List<AiReviewWrongItemEntity> items = wrongMapper.selectList(new LambdaQueryWrapper<AiReviewWrongItemEntity>()
        .eq(AiReviewWrongItemEntity::getPlanId, planId));
    Map<Long, int[]> counters = new HashMap<>();
    for (AiReviewWrongItemEntity item : items) {
      if (item.getKnowledgeId() == null) {
        continue;
      }
      int[] counter = counters.computeIfAbsent(item.getKnowledgeId(), k -> new int[2]);
      counter[0] += item.getWrongCount() != null ? item.getWrongCount() : 0;
      counter[1] += item.getCorrectCount() != null ? item.getCorrectCount() : 0;
    }
    Map<Long, Double> rates = new HashMap<>();
    for (Map.Entry<Long, int[]> entry : counters.entrySet()) {
      int total = entry.getValue()[0] + entry.getValue()[1];
      rates.put(entry.getKey(), total == 0 ? 0.0 : entry.getValue()[0] / (double) total);
    }
    return rates;
  }

  /** 题目轻量元数据（按 id 批量从 PHP 查，排题用） */
  private Map<Long, PhpQuestion> questionMetaByIds(AiReviewPlanEntity plan, List<Long> questionIds) {
    if (questionIds.isEmpty()) {
      return Map.of();
    }
    return phpQuery.loadQuestionMetaByIds(questionIds);
  }

  private WeightResolver.ResolvedWeights weightsOfPlan(AiReviewPlanEntity plan) {
    // 权重快照 + PHP 树重建 L3 权重（快照只有 L1 粒度）
    long courseId = plan.getCourseId();
    List<PhpKnowledgeNode> tree = phpQuery.loadKnowledgeTree(courseId);
    PhpCourseWeights snapshotWeights = snapshotToCourseWeights(plan);
    List<PhpQuestion> pool = new ArrayList<>(questionMetaByIds(plan, parseIds(plan.getMustDoIds())).values());
    return WeightResolver.resolve(snapshotWeights, tree, pool);
  }

  private PhpCourseWeights snapshotToCourseWeights(AiReviewPlanEntity plan) {
    if (plan.getWeightSnapshot() == null || plan.getWeightSnapshot().isBlank()) {
      return new PhpCourseWeights(plan.getWeightSource(), List.of());
    }
    List<PhpCourseWeights.WeightItem> items = new ArrayList<>();
    for (Map<String, Object> item : parseSnapshot(plan.getWeightSnapshot())) {
      long knowledgeId = ((Number) item.get("knowledgeId")).longValue();
      int rate = (int) Math.round(((Number) item.get("rate")).doubleValue());
      items.add(new PhpCourseWeights.WeightItem(knowledgeId, (String) item.get("title"), rate));
    }
    return new PhpCourseWeights(plan.getWeightSource(), items);
  }

  private Map<Long, Long> l1ByL3OfPlan(AiReviewPlanEntity plan) {
    return weightsOfPlan(plan).l1ByL3();
  }

  private List<Map<String, Object>> parseSnapshot(String snapshotJson) {
    try {
      return objectMapper.readValue(snapshotJson, new TypeReference<>() {
      });
    } catch (Exception e) {
      return List.of();
    }
  }

  private List<Long> parseIds(String idsJson) {
    if (idsJson == null || idsJson.isBlank()) {
      return new ArrayList<>();
    }
    try {
      return new ArrayList<>(objectMapper.readValue(idsJson, new TypeReference<List<Long>>() {
      }));
    } catch (Exception e) {
      log.warn("[AI复习] 解析题 id 列表失败：{}", e.getMessage());
      return new ArrayList<>();
    }
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception e) {
      return "[]";
    }
  }

  private List<AnswerJudgeDomainService.AnswerOption> parseOptions(String answerJson) {
    List<AnswerJudgeDomainService.AnswerOption> options = new ArrayList<>();
    if (answerJson == null || answerJson.isBlank()) {
      return options;
    }
    try {
      List<Map<String, Object>> raw = objectMapper.readValue(answerJson, new TypeReference<>() {
      });
      for (Map<String, Object> item : raw) {
        options.add(new AnswerJudgeDomainService.AnswerOption(
            item.get("content") != null ? String.valueOf(item.get("content")) : "",
            Boolean.TRUE.equals(item.get("isCorrect")) || "true".equals(String.valueOf(item.get("isCorrect"))),
            parseOptionScore(item.get("score"))));
      }
    } catch (Exception e) {
      log.warn("[AI复习] 答案 JSON 解析失败：{}", e.getMessage());
    }
    return options;
  }

  /** 选项 score 兼容解析：PHP 数据有数值/字符串两种形态，非数值按 null（该题回退少选不得分） */
  private static java.math.BigDecimal parseOptionScore(Object score) {
    if (score == null) {
      return null;
    }
    try {
      String text = String.valueOf(score).trim();
      if (text.isEmpty()) {
        return null;
      }
      return new java.math.BigDecimal(text);
    } catch (Exception e) {
      return null;
    }
  }

  /** 剥离答案标记：只留 content 列表（§3.4 防抓包背答案） */
  private List<Map<String, String>> stripAnswerOptions(String answerJson) {
    List<Map<String, String>> stripped = new ArrayList<>();
    for (AnswerJudgeDomainService.AnswerOption option : parseOptions(answerJson)) {
      stripped.add(Map.of("content", option.content()));
    }
    return stripped;
  }

  private List<String> parseUserMultiAnswer(String userAnswer) {
    if (userAnswer == null || !userAnswer.trim().startsWith("[")) {
      return List.of();
    }
    try {
      return objectMapper.readValue(userAnswer, new TypeReference<List<String>>() {
      });
    } catch (Exception e) {
      return List.of();
    }
  }

  private List<String> parseDateList(String examDateListJson) {
    if (examDateListJson == null || examDateListJson.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(examDateListJson, new TypeReference<List<String>>() {
      });
    } catch (Exception e) {
      return List.of();
    }
  }

  /** 字典布尔值：value ∈ {true,1,yes} 视为开；缺失/异常回默认值 */
  private boolean dictBool(String code, boolean defaultValue, Long tenantId) {
    try {
      List<DictEntity> dicts = dictMapper.selectList(new LambdaQueryWrapper<DictEntity>()
          .eq(DictEntity::getCode, code)
          .eq(DictEntity::getDeleted, 0)
          .last("LIMIT 1"));
      if (!dicts.isEmpty() && dicts.get(0).getValue() != null) {
        String v = dicts.get(0).getValue().trim().toLowerCase();
        return "true".equals(v) || "1".equals(v) || "yes".equals(v);
      }
    } catch (Exception e) {
      log.warn("[AI复习] 读取字典 {} 失败，用默认值 {}：{}", code, defaultValue, e.getMessage());
    }
    return defaultValue;
  }

  private int dictInt(String code, int defaultValue, Long tenantId) {
    try {
      List<DictEntity> dicts = dictMapper.selectList(new LambdaQueryWrapper<DictEntity>()
          .eq(DictEntity::getCode, code)
          .eq(DictEntity::getDeleted, 0)
          .last("LIMIT 1"));
      if (!dicts.isEmpty() && dicts.get(0).getValue() != null) {
        return Integer.parseInt(dicts.get(0).getValue().trim());
      }
    } catch (Exception e) {
      log.warn("[AI复习] 读取字典 {} 失败，用默认值 {}：{}", code, defaultValue, e.getMessage());
    }
    return defaultValue;
  }

  // ------------------------------------------------------------------
  // 视图组装
  // ------------------------------------------------------------------

  private Map<String, Object> todayView(AiReviewPlanEntity plan, AiReviewPlanDayEntity day) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("dayId", String.valueOf(day.getId()));
    view.put("newTarget", day.getNewTarget());
    view.put("reviewTarget", reviewListForDay(plan, day).size());
    view.put("newDone", day.getNewDone());
    view.put("reviewDone", day.getReviewDone());
    view.put("mock", mockView(day));
    return view;
  }

  private Map<String, Object> mockView(AiReviewPlanDayEntity day) {
    if (day.getMockPaperId() == null) {
      return null;
    }
    AiReviewMockPaperEntity paper = mockPaperMapper.selectById(day.getMockPaperId());
    if (paper == null) {
      return null;
    }
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("mockPaperId", String.valueOf(paper.getId()));
    view.put("questionCount", paper.getQuestionCount());
    view.put("status", paper.getStatus());
    return view;
  }

  private Map<String, Object> progressView(AiReviewPlanEntity plan) {
    int done = answerMapper.selectCount(new LambdaQueryWrapper<AiReviewAnswerEntity>()
        .eq(AiReviewAnswerEntity::getPlanId, plan.getId())).intValue();
    int correct = answerMapper.selectCount(new LambdaQueryWrapper<AiReviewAnswerEntity>()
        .eq(AiReviewAnswerEntity::getPlanId, plan.getId())
        .eq(AiReviewAnswerEntity::getIsCorrect, 1)).intValue();
    Set<Long> doneIds = new HashSet<>();
    for (AiReviewAnswerEntity a : answerMapper.selectList(new LambdaQueryWrapper<AiReviewAnswerEntity>()
        .eq(AiReviewAnswerEntity::getPlanId, plan.getId())
        .select(AiReviewAnswerEntity::getQuestionId))) {
      doneIds.add(a.getQuestionId());
    }
    int target = plan.getTargetQuestions() != null ? plan.getTargetQuestions() : 0;
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("doneQuestions", doneIds.size());
    view.put("targetQuestions", target);
    view.put("coveragePercent", target == 0 ? 0 : Math.round(doneIds.size() * 1000.0 / target) / 10.0);
    view.put("accuracy", accuracy(correct, done));
    return view;
  }

  private Map<String, Object> wrongStatsView(long planId) {
    List<AiReviewWrongItemEntity> items = wrongMapper.selectList(new LambdaQueryWrapper<AiReviewWrongItemEntity>()
        .eq(AiReviewWrongItemEntity::getPlanId, planId));
    int red = 0, yellow = 0, green = 0, star = 0;
    for (AiReviewWrongItemEntity item : items) {
      if (item.getRemoved() != null && item.getRemoved() == 1) {
        green++;
      } else if ("red".equals(item.getCardLevel())) {
        red++;
        if (item.getStar() != null && item.getStar() == 1) {
          star++;
        }
      } else if ("yellow".equals(item.getCardLevel())) {
        yellow++;
      }
    }
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("red", red);
    view.put("yellow", yellow);
    view.put("green", green);
    view.put("star", star);
    return view;
  }

  private Map<String, Object> answerView(boolean correct,
      List<AnswerJudgeDomainService.AnswerOption> options, String resolve, Map<String, Object> cardChange) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("correct", correct ? 1 : 0);
    StringBuilder correctAnswer = new StringBuilder();
    List<String> correctOptions = new ArrayList<>();
    for (AnswerJudgeDomainService.AnswerOption option : options) {
      if (option.correct()) {
        if (correctAnswer.length() > 0) {
          correctAnswer.append("、");
        }
        correctAnswer.append(option.content());
        correctOptions.add(option.content());
      }
    }
    result.put("correctAnswer", correctAnswer.toString());
    // 正确选项原文数组：选项文本可能含"、"等分隔符，前端高亮必须用它而非 split（2026-08-05 含顿号选项不高亮问题）
    result.put("correctOptions", correctOptions);
    result.put("resolve", resolve);
    result.put("cardChange", cardChange);
    result.put("encourageText", null);
    return result;
  }

  private Map<String, Object> mockResultView(AiReviewPlanEntity plan, AiReviewMockPaperEntity paper) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("score", paper.getScore());
    result.put("passScore", paper.getPassScore());
    result.put("passed", paper.getScore() != null && paper.getPassScore() != null
        && paper.getScore().compareTo(paper.getPassScore()) >= 0 ? 1 : 0);
    result.put("correctCount", paper.getCorrectCount());
    result.put("wrongCount", paper.getQuestionCount() - (paper.getCorrectCount() != null ? paper.getCorrectCount() : 0));
    return result;
  }

  private int estimateTomorrowNew(AiReviewPlanEntity plan) {
    List<Long> mustDo = parseIds(plan.getMustDoIds());
    Set<Long> assigned = assignedNewIds(plan.getId());
    long remaining = mustDo.stream().filter(id -> !assigned.contains(id)).count();
    int remainingDays = Math.max(1, plan.getStage1Days() - plan.getCurrentDayIndex() + 1);
    return PlanScheduleDomainService.stage1NewTarget((int) remaining, remainingDays, plan.getDailyNewCap());
  }

  private static double accuracy(int correct, int total) {
    return total == 0 ? 0 : Math.round(correct * 1000.0 / total) / 10.0;
  }

  private static String stageName(int stage) {
    return switch (stage) {
      case 1 -> "地毯式扫荡";
      case 2 -> "定向爆破";
      default -> "仿真模考";
    };
  }
}
