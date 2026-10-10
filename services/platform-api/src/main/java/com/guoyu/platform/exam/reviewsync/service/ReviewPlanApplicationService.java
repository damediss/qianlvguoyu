package com.guoyu.platform.exam.reviewsync.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
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
import com.guoyu.platform.exam.reviewsync.mapper.ExamResultWriteRepository;
import com.guoyu.platform.exam.reviewsync.dto.ReviewCourseWeights;
import com.guoyu.platform.exam.reviewsync.dto.ReviewExamPlanInfo;
import com.guoyu.platform.exam.reviewsync.dto.ReviewKnowledgeNode;
import com.guoyu.platform.exam.reviewsync.dto.ReviewQuestion;
import com.guoyu.platform.exam.reviewsync.dto.ReviewQuestionDetail;
import com.guoyu.platform.exam.reviewsync.service.AiGradeApplicationService.SubjectiveGradeResult;
import com.guoyu.platform.exam.reviewsync.support.SubjectiveAnswerExtractor;
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
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 同步学用例编排（§5 / §7）。
 *
 * <p>线程边界：所有公开方法以 platformUserId/tenantId 参数驱动，不调 SecurityContext；
 * 懒生成发生在学员请求线程（overview/today），不经后台线程池。
 *
 * <p>原每日/每周定时物化（ReviewSyncScheduler）已随蓝绿方案 §5.2 移除：计划与当日任务的
 * 物化由 overview/today 的 {@link #getOrMaterializeDay} 懒生成路径覆盖，中断检测同在请求链路。
 */
@Slf4j
@Service
public class ReviewPlanApplicationService {

  /** 模考单题 LLM 判分超时（秒）：超时取消 + 失败重试 1 次，仍失败该题 0 分兜底（方案 §3.4） */
  private static final int MOCK_GRADE_TIMEOUT_SECONDS = 20;

  private final AiReviewPlanMapper planMapper;
  private final AiReviewPlanDayMapper dayMapper;
  private final AiReviewAnswerMapper answerMapper;
  private final AiReviewWrongItemMapper wrongMapper;
  private final AiReviewMockPaperMapper mockPaperMapper;
  private final AiReviewMockPaperItemMapper mockItemMapper;
  private final AiReviewGrayWhitelistMapper grayWhitelistMapper;
  private final ReviewQuestionQueryService reviewQuery;
  private final ExamResultWriteRepository examResultWriteRepository;
  private final ReviewLlmAssistService llmAssistService;
  private final DictMapper dictMapper;
  private final ObjectMapper objectMapper;
  private final AiGradeApplicationService aiGradeService;
  /** 模考主观题并行判分专用池（踩坑 #14：注入端 @Qualifier 对齐 @Bean 名） */
  private final ThreadPoolTaskExecutor aiGradeMockExecutor;

  /**
   * 自注入代理：主观题 AI 判分（秒级 LLM 调用）禁止持有 DB 事务——判分在无事务段完成后，
   * 落库走代理开启短事务（参照 TrainFollowEvidenceApplicationService 的 self 拆分惯例）。
   */
  private ReviewPlanApplicationService self;

  public ReviewPlanApplicationService(
      AiReviewPlanMapper planMapper,
      AiReviewPlanDayMapper dayMapper,
      AiReviewAnswerMapper answerMapper,
      AiReviewWrongItemMapper wrongMapper,
      AiReviewMockPaperMapper mockPaperMapper,
      AiReviewMockPaperItemMapper mockItemMapper,
      AiReviewGrayWhitelistMapper grayWhitelistMapper,
      ReviewQuestionQueryService reviewQuery,
      ExamResultWriteRepository examResultWriteRepository,
      ReviewLlmAssistService llmAssistService,
      DictMapper dictMapper,
      ObjectMapper objectMapper,
      AiGradeApplicationService aiGradeService,
      @Qualifier("aiGradeMockExecutor") ThreadPoolTaskExecutor aiGradeMockExecutor) {
    this.planMapper = planMapper;
    this.dayMapper = dayMapper;
    this.answerMapper = answerMapper;
    this.wrongMapper = wrongMapper;
    this.mockPaperMapper = mockPaperMapper;
    this.mockItemMapper = mockItemMapper;
    this.grayWhitelistMapper = grayWhitelistMapper;
    this.reviewQuery = reviewQuery;
    this.examResultWriteRepository = examResultWriteRepository;
    this.llmAssistService = llmAssistService;
    this.dictMapper = dictMapper;
    this.objectMapper = objectMapper;
    this.aiGradeService = aiGradeService;
    this.aiGradeMockExecutor = aiGradeMockExecutor;
  }

  @Autowired
  public void setSelf(@Lazy ReviewPlanApplicationService self) {
    this.self = self;
  }

  // ==================================================================
  // D8 灰度发布（方案 §15.2）：总开关 ai_review_gray_enabled + exam/user 双粒度白名单
  // ==================================================================

  /** 灰度拦截：开关开启时，examId 或 userId 任一命中白名单才放行；开关关闭/缺失=全员开放 */
  private void gateGrayRelease(long examId, long platformUserId, Long tenantId) {
    if (!isGrayAllowed(examId, platformUserId, tenantId)) {
      log.warn("[AI复习] 灰度拦截：user={} exam={} 不在白名单", platformUserId, examId);
      throw new BusinessException(ErrorCode.BAD_REQUEST, "同步练习正在小范围试点中，敬请期待");
    }
  }

  /** 灰度准入判定（不抛异常）：菜单预检与各用例拦截共用 */
  private boolean isGrayAllowed(long examId, long platformUserId, Long tenantId) {
    if (!dictBool("ai_review_gray_enabled", false, tenantId)) {
      return true;
    }
    return grayWhitelistMapper.selectCount(new LambdaQueryWrapper<AiReviewGrayWhitelistEntity>()
            .eq(AiReviewGrayWhitelistEntity::getScopeType, "exam")
            .eq(AiReviewGrayWhitelistEntity::getScopeId, examId)) > 0
        || grayWhitelistMapper.selectCount(new LambdaQueryWrapper<AiReviewGrayWhitelistEntity>()
            .eq(AiReviewGrayWhitelistEntity::getScopeType, "user")
            .eq(AiReviewGrayWhitelistEntity::getScopeId, platformUserId)) > 0;
  }

  /** 灰度准入预检（D8 配套）：App 菜单页据此决定是否渲染「同步练习」入口，本接口不做灰度拦截 */
  public Map<String, Object> grayAvailability(long platformUserId, long examId, Long tenantId) {
    return Map.of("allowed", isGrayAllowed(examId, platformUserId, tenantId));
  }

  // ==================================================================
  // 7.1 计划概览（懒生成入口）
  // ==================================================================

  @Transactional
  public Map<String, Object> overview(long platformUserId, long examId, Long tenantId) {
    gateGrayRelease(examId, platformUserId, tenantId);
    AiReviewPlanEntity plan = replanIfExamPlanDrifted(getOrCreatePlan(platformUserId, examId, tenantId));
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
    AiReviewPlanEntity plan = replanIfExamPlanDrifted(requirePlan(platformUserId, examId));
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
    Map<Long, ReviewQuestionDetail> details = reviewQuery.loadQuestionDetails(questionIds);
    List<Map<String, Object>> list = new ArrayList<>();
    for (Long qid : questionIds) {
      ReviewQuestionDetail detail = details.get(qid);
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

  /**
   * 单题作答入口（本身不开事务）：查题后按题型分流——
   * 客观题走原大事务路径（{@link #answerObjectiveInTx}，自注入代理开启，判分/落库逻辑一行未动）；
   * 白名单主观题走 {@link #answerSubjective}（AI 判分在事务外完成，落库另开短事务，
   * LLM 调用期间禁止持有 DB 事务——《简答题作答与AI判分方案》§3.4）。
   */
  public Map<String, Object> answer(long platformUserId, long examId, long dayId, long questionId,
      String roundType, String userAnswer, Integer useTimeSec, Long tenantId) {
    return answer(platformUserId, examId, dayId, questionId, roundType, userAnswer, useTimeSec,
        tenantId, null, null);
  }

  public Map<String, Object> answer(long platformUserId, long examId, long dayId, long questionId,
      String roundType, String userAnswer, Integer useTimeSec, Long tenantId,
      Integer selfAssessed, Integer selfCorrect) {
    gateGrayRelease(examId, platformUserId, tenantId);
    AiReviewPlanEntity plan = requirePlan(platformUserId, examId);
    AiReviewPlanDayEntity day = requireDay(plan.getId(), dayId);
    ReviewQuestionDetail detail = reviewQuery.loadQuestionDetail(questionId);
    if (detail == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "题目不存在");
    }
    if (AiGradeApplicationService.isSubjective(detail.type())) {
      return answerSubjective(plan, day, detail, platformUserId, examId, dayId, questionId,
          roundType, userAnswer, useTimeSec, tenantId, selfAssessed, selfCorrect);
    }
    return self.answerObjectiveInTx(plan, day, detail, platformUserId, examId, dayId, questionId,
        roundType, userAnswer, useTimeSec, tenantId);
  }

  /** 客观题作答（原 answer() 大事务路径，自注入代理开启事务；判分/落库/计数逻辑与原实现一致） */
  @Transactional
  public Map<String, Object> answerObjectiveInTx(AiReviewPlanEntity plan, AiReviewPlanDayEntity day,
      ReviewQuestionDetail detail, long platformUserId, long examId, long dayId, long questionId,
      String roundType, String userAnswer, Integer useTimeSec, Long tenantId) {
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
    // 单库化：exam_result 同事务同步直写成功即置 1（语义=已落 exam_result；回滚旧代码时补偿 job 不会重写）
    answer.setPhpSynced(1);
    answerMapper.insert(answer);

    // 错题状态机
    Map<String, Object> cardChange = applyWrongCard(plan, platformUserId, detail, judged.correct());

    // 计数：改答案不重复计 done，只补作答次数与对错差额
    bumpDayCounters(day, roundType, judged.correct(), correction);
    touchLastActiveDate(plan, day);
    planMapper.updateById(plan);

    // exam_result 事务内同步直写（exam_type=5，对齐 PHP trainsave 口径）；
    // INSERT 失败整个作答事务回滚，学员收到错误可重试（方案 §4-C 明示的行为变化）
    examResultWriteRepository.insertExamResult(
        answer.getQuestionType(),
        answer.getExamPlanId() == null ? 0 : answer.getExamPlanId(),
        answer.getQuestionId(),
        answer.getPlatformUserId(),
        answer.getUserAnswer(),
        examResultScoreOf(answer));

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

  // ==================================================================
  // 7.4-主观题 单题作答（AI 判分在事务外，落库短事务；方案 §3.4 / §5-5）
  // ==================================================================

  /**
   * 主观题作答编排（无事务段）：提取参考答案 → 幂等预检 → 判分（自评/AI）→ 短事务落库。
   *
   * <ul>
   *   <li>无参考答案（exam_exam_detail.answer 兼容提取为空）→ 直接回 ungradeable=1，
   *       不调 LLM 不落判分，前端走自评（Q4-A）；</li>
   *   <li>selfAssessed=1 → 采信前端自评结论（correct=selfCorrect==1，gradeSource='self'），
   *       不调 LLM、不进判分缓存，照常落库+答错入错题本（Q6）；</li>
   *   <li>否则调 {@link AiGradeApplicationService#grade}（同步秒级；限流/失败抛业务异常，
   *       前端按约定转自评兜底，§6）。</li>
   * </ul>
   * 幂等语义与客观题对齐：同答案或此前已答对 → 纯回放；改答案 → 重判（判分缓存键含答案
   * md5，自然 miss）。最终幂等裁决留在短事务内（防并发同题同文本双提交双落库）。
   */
  private Map<String, Object> answerSubjective(AiReviewPlanEntity plan, AiReviewPlanDayEntity day,
      ReviewQuestionDetail detail, long platformUserId, long examId, long dayId, long questionId,
      String roundType, String userAnswer, Integer useTimeSec, Long tenantId,
      Integer selfAssessed, Integer selfCorrect) {
    String reference = SubjectiveAnswerExtractor.extract(detail.answerJson());

    // 幂等预检（无事务）：同答案或此前已答对 → 纯回放，不调 LLM
    AiReviewAnswerEntity existing = findLatestAnswer(plan.getId(), dayId, questionId, roundType);
    if (existing != null && (sameAnswer(existing, userAnswer) || wasCorrect(existing))) {
      return subjectiveReplayView(existing, reference, detail.resolve());
    }

    // 自评通道优先于"无参考答案"早退：自评不需要参考答案且必须落库——否则无参考答案题
    // 永卡当日任务（评审 #1；Q4 口径"自评结果照常落库"）
    if (selfAssessed != null && selfAssessed == 1) {
      boolean selfRight = selfCorrect != null && selfCorrect == 1;
      SubjectiveGradeResult graded = new SubjectiveGradeResult(true, selfRight ? fullScoreOf(detail) : 0,
          selfRight, null, "self");
      return persistSubjectiveWithDupReplay(plan, day, detail, platformUserId, examId, dayId,
          questionId, roundType, userAnswer, useTimeSec, graded, reference);
    }

    if (reference == null) {
      log.info("[AI判分] 参考答案提取为空，返回不可判分（前端走自评）：questionId={}", questionId);
      return subjectiveAnswerView(false, null, detail.resolve(), null, null, null, 1);
    }

    SubjectiveGradeResult graded;
    try {
      graded = aiGradeService.grade(tenantId, platformUserId, questionId, detail.content(),
          reference, userAnswer, detail.type(), fullScoreOf(detail));
    } catch (BusinessException e) {
      // AI 判分失败（限流/超时/解析失败/模型配置缺失）：不中断练习、不落库，
      // 返回 gradeFailed=1 + 参考答案，前端转自评兜底（§6）——自评经 selfAssessed=1 再次提交落库
      log.warn("[AI判分] 判分失败转自评兜底：questionId={}, err={}", questionId, e.getMessage());
      Map<String, Object> view = subjectiveAnswerView(false, reference, detail.resolve(),
          null, null, null, 0);
      view.put("gradeFailed", 1);
      view.put("gradeFailMsg", e.getMessage());
      return view;
    } catch (Exception e) {
      log.error("[AI判分] 判分异常转自评兜底：questionId={}", questionId, e);
      Map<String, Object> view = subjectiveAnswerView(false, reference, detail.resolve(),
          null, null, null, 0);
      view.put("gradeFailed", 1);
      view.put("gradeFailMsg", "AI 判分暂不可用，请自评");
      return view;
    }

    return persistSubjectiveWithDupReplay(plan, day, detail, platformUserId, examId, dayId,
        questionId, roundType, userAnswer, useTimeSec, graded, reference);
  }

  /**
   * 短事务落库 + 真并发撞 uk 兜底：RR 快照读防不住未提交行，后到者 insert 撞
   * uk_ai_review_answer_once → 本方法捕获后当前读回放先到者结果（评审 #3，不再吃 500）
   */
  private Map<String, Object> persistSubjectiveWithDupReplay(AiReviewPlanEntity plan,
      AiReviewPlanDayEntity day, ReviewQuestionDetail detail, long platformUserId, long examId,
      long dayId, long questionId, String roundType, String userAnswer, Integer useTimeSec,
      SubjectiveGradeResult graded, String reference) {
    try {
      return self.persistSubjectiveAnswer(plan, day, detail, platformUserId, examId, dayId,
          questionId, roundType, userAnswer, useTimeSec, graded, reference);
    } catch (DuplicateKeyException e) {
      log.warn("[AI判分] 并发双提交撞唯一键，回放先到者结果：planId={} questionId={}",
          plan.getId(), questionId);
      AiReviewAnswerEntity winner = findLatestAnswer(plan.getId(), dayId, questionId, roundType);
      if (winner != null) {
        return subjectiveReplayView(winner, reference, detail.resolve());
      }
      throw e;
    }
  }

  /**
   * 主观题判分结果落库（短事务，自注入代理开启）：
   * 最终幂等裁决（事务内当前读）→ ai_review_answer（correct 按得分率≥60%口径；AI 判分
   * earnedScore=AI 比例分，自评 earnedScore 留空回退"对=题分/错=0"）→ 答错走错题卡 →
   * 计数差额（与客观题 :315 同款）→ exam_result 同事务直写。
   */
  @Transactional
  public Map<String, Object> persistSubjectiveAnswer(AiReviewPlanEntity plan, AiReviewPlanDayEntity day,
      ReviewQuestionDetail detail, long platformUserId, long examId, long dayId, long questionId,
      String roundType, String userAnswer, Integer useTimeSec, SubjectiveGradeResult graded,
      String reference) {
    // 最终幂等裁决：并发同题同文本双提交，后到者回放先到者已落库结果（防双落库）
    AiReviewAnswerEntity existing = findLatestAnswer(plan.getId(), dayId, questionId, roundType);
    boolean correction = false;
    if (existing != null) {
      if (sameAnswer(existing, userAnswer) || wasCorrect(existing)) {
        return subjectiveReplayView(existing, reference, detail.resolve());
      }
      correction = true;
    }

    int attemptSeq = countAttempts(plan.getId(), questionId) + 1;
    AiReviewAnswerEntity answer = new AiReviewAnswerEntity();
    answer.setPlanId(plan.getId());
    answer.setDayId(dayId);
    answer.setPlatformUserId(platformUserId);
    answer.setExamPlanId(examId);
    answer.setQuestionId(questionId);
    answer.setQuestionScore(detail.score());
    if ("ai".equals(graded.gradeSource())) {
      answer.setEarnedScore(BigDecimal.valueOf(graded.score()));
    }
    answer.setKnowledgeId(detail.knowledgeId());
    answer.setQuestionType(detail.type());
    answer.setRoundType(roundType);
    answer.setUserAnswer(userAnswer);
    answer.setIsCorrect(graded.correct() ? 1 : 0);
    answer.setUseTimeSec(useTimeSec != null ? useTimeSec : 0);
    answer.setAttemptSeq(attemptSeq);
    // 单库化：exam_result 同事务同步直写成功即置 1（与客观题同口径）
    answer.setPhpSynced(1);
    answerMapper.insert(answer);

    // 错题状态机（答错入册，与客观题同一机制）
    Map<String, Object> cardChange = applyWrongCard(plan, platformUserId, detail, graded.correct());

    // 计数：改答案不重复计 done，只补作答次数与对错差额（与客观题同款）
    bumpDayCounters(day, roundType, graded.correct(), correction);
    touchLastActiveDate(plan, day);
    planMapper.updateById(plan);

    // exam_result 事务内同步直写（exam_type=5，沿用客观题机位；失败回滚整个作答事务）
    examResultWriteRepository.insertExamResult(
        answer.getQuestionType(),
        answer.getExamPlanId() == null ? 0 : answer.getExamPlanId(),
        answer.getQuestionId(),
        answer.getPlatformUserId(),
        answer.getUserAnswer(),
        examResultScoreOf(answer));

    Map<String, Object> result = subjectiveAnswerView(graded.correct(), reference, detail.resolve(),
        cardChange, graded.comment(), graded.gradeSource(), 0);
    if (cardChange != null && Boolean.TRUE.equals(cardChange.get("explainAvailable"))) {
      result.put("explainAvailable", 1);
    }

    // D9 连对/连错即时鼓励（与客观题 answerObjectiveInTx 同款口径，回放路径不触发）
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

  /** 该（计划+天+题+轮次）最近一次作答（主观题幂等预检/裁决共用） */
  private AiReviewAnswerEntity findLatestAnswer(long planId, long dayId, long questionId, String roundType) {
    return answerMapper.selectOne(new LambdaQueryWrapper<AiReviewAnswerEntity>()
        .eq(AiReviewAnswerEntity::getPlanId, planId)
        .eq(AiReviewAnswerEntity::getDayId, dayId)
        .eq(AiReviewAnswerEntity::getQuestionId, questionId)
        .eq(AiReviewAnswerEntity::getRoundType, roundType)
        .orderByDesc(AiReviewAnswerEntity::getAttemptSeq)
        .last("LIMIT 1"));
  }

  private static boolean sameAnswer(AiReviewAnswerEntity existing, String userAnswer) {
    return java.util.Objects.equals(existing.getUserAnswer(), userAnswer);
  }

  private static boolean wasCorrect(AiReviewAnswerEntity existing) {
    return existing.getIsCorrect() != null && existing.getIsCorrect() == 1;
  }

  private static int fullScoreOf(ReviewQuestionDetail detail) {
    return detail.score() != null ? detail.score().intValue() : 0;
  }

  private static int fullScoreOf(AiReviewMockPaperItemEntity item) {
    return item.getScore() != null ? item.getScore().intValue() : 0;
  }

  /** 主观题作答回放视图：已存结果 + 恒返回参考答案；aiComment/gradeSource 未落库，回放为 null（口径见交棒说明） */
  private Map<String, Object> subjectiveReplayView(AiReviewAnswerEntity existing, String reference,
      String resolve) {
    return subjectiveAnswerView(existing.getIsCorrect() != null && existing.getIsCorrect() == 1,
        reference, resolve, null, null, null, 0);
  }

  /**
   * 主观题作答响应视图（在现有结构上扩展，前端已按此对齐）：
   * correctAnswer 恒返回（答对也返回，不可判分为 null）；aiComment=AI 点评；
   * gradeSource=ai/self；ungradeable=1 表示无参考答案不可判分（前端走自评）。
   */
  private Map<String, Object> subjectiveAnswerView(boolean correct, String reference, String resolve,
      Map<String, Object> cardChange, String aiComment, String gradeSource, int ungradeable) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("correct", correct ? 1 : 0);
    result.put("correctAnswer", reference);
    result.put("correctOptions", List.of());
    result.put("resolve", resolve);
    result.put("cardChange", cardChange);
    result.put("encourageText", null);
    result.put("aiComment", aiComment);
    result.put("gradeSource", gradeSource);
    result.put("ungradeable", ungradeable);
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

  /**
   * 模考交卷入口（本身不开事务），拆三段（《简答题作答与AI判分方案》§3.4 Q2-A + v1.2 事务边界）：
   * ① 客观题判分（现状口径，纯 CPU）→ ② 主观题并行判分（事务外专用池，题干+参考答案由
   * HTTP 线程随卷一次性预载，Worker 只调 LLM）→ ③ 短事务落库
   * （{@link #persistMockSubmit}，自注入代理开启；并发双交在事务内裁决回放）。
   * LLM 判分期间禁止持有 DB 事务。
   */
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
    for (AiReviewMockPaperItemEntity item : items) {
      scoreByQid.put(item.getQuestionId(), item.getScore());
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
    // HTTP 线程随卷一次性预载全部题的题干+参考答案（主观题判分不再回库查题）
    Map<Long, ReviewQuestionDetail> details = reviewQuery.loadQuestionDetails(new ArrayList<>(scoreByQid.keySet()));

    // O7：模考多选严格判分开关（错选 0 分、少选按选项 score 部分给分）；练习轮仍与 PHP 宽松口径一致
    boolean strictJudge = dictInt("ai_review_mock_strict_judge", 1, tenantId) == 1;

    // ① 客观题判分（现状口径，纯 CPU 无 LLM）；主观题收集待判
    Map<Long, MockJudgedItem> judgedByQid = new HashMap<>();
    List<AiReviewMockPaperItemEntity> subjectiveItems = new ArrayList<>();
    for (AiReviewMockPaperItemEntity item : items) {
      ReviewQuestionDetail detail = details.get(item.getQuestionId());
      if (detail == null) {
        continue;
      }
      if (AiGradeApplicationService.isSubjective(detail.type())) {
        subjectiveItems.add(item);
        continue;
      }
      String userAnswer = userAnswerByQid.getOrDefault(item.getQuestionId(), "");
      judgedByQid.put(item.getQuestionId(), judgeObjectiveMockItem(item, detail, userAnswer, strictJudge));
    }

    // ② 主观题并行判分（事务外）：先一次性全部提交专用池，再逐个收结果（单题超时 20s + 失败重试 1 次）
    if (!subjectiveItems.isEmpty()) {
      Map<Long, Future<SubjectiveGradeResult>> firstAttemptByQid = new LinkedHashMap<>();
      for (AiReviewMockPaperItemEntity item : subjectiveItems) {
        ReviewQuestionDetail detail = details.get(item.getQuestionId());
        firstAttemptByQid.put(item.getQuestionId(), submitMockGrade(tenantId, platformUserId,
            item.getQuestionId(), detail.content(), detail.answerJson(),
            userAnswerByQid.getOrDefault(item.getQuestionId(), ""), detail.type(), fullScoreOf(item)));
      }
      for (AiReviewMockPaperItemEntity item : subjectiveItems) {
        ReviewQuestionDetail detail = details.get(item.getQuestionId());
        String userAnswer = userAnswerByQid.getOrDefault(item.getQuestionId(), "");
        SubjectiveGradeResult graded = awaitMockGrade(firstAttemptByQid.get(item.getQuestionId()),
            () -> submitMockGrade(tenantId, platformUserId, item.getQuestionId(), detail.content(),
                detail.answerJson(), userAnswer, detail.type(), fullScoreOf(item)));
        judgedByQid.put(item.getQuestionId(), toJudgedSubjective(item, detail, userAnswer, graded));
      }
    }

    // ③ 短事务落库（答案/错题/成绩/exam_result 沿用现有机位）；
    // 并发双交撞 uk → 回放已落库结果（评审 #3，与既有 DuplicateKeyException 兜底同口径）
    try {
      return self.persistMockSubmit(plan, examId, mockPaperId, platformUserId, items, details, judgedByQid);
    } catch (DuplicateKeyException e) {
      log.warn("[AI复习] 模考并发双交撞唯一键，回放结果：mockPaperId={}", mockPaperId);
      return mockResultView(plan, paper);
    }
  }

  /** 模考单题判分结果（①②段产出，③段只负责落库与视图组装） */
  private record MockJudgedItem(
      boolean subjective,
      boolean correct,
      BigDecimal earned,
      String correctAnswerText,
      String userAnswer,
      /** 主观题：AI 点评 / 失败标注；客观题 null */
      String aiComment) {
  }

  /** ① 客观题判分（原交卷循环内的判分口径原样抽取：严格多选给 earnedScore，其余 对=题分/错=0） */
  private MockJudgedItem judgeObjectiveMockItem(AiReviewMockPaperItemEntity item,
      ReviewQuestionDetail detail, String userAnswer, boolean strictJudge) {
    List<AnswerJudgeDomainService.AnswerOption> options = parseOptions(detail.answerJson());
    AnswerJudgeDomainService.JudgeResult judged;
    if (strictJudge && "multi".equals(detail.type())) {
      judged = AnswerJudgeDomainService.judgeStrictMulti(options, parseUserMultiAnswer(userAnswer),
          item.getScore());
    } else {
      judged = AnswerJudgeDomainService.judge(detail.type(), options, userAnswer,
          parseUserMultiAnswer(userAnswer));
    }
    BigDecimal earned;
    if (judged.earnedScore() != null) {
      earned = judged.earnedScore();
    } else {
      earned = judged.correct() ? (item.getScore() != null ? item.getScore() : BigDecimal.ZERO)
          : BigDecimal.ZERO;
    }
    return new MockJudgedItem(false, judged.correct(), earned, judged.correctAnswerText(),
        userAnswer, null);
  }

  /** ② 主观题判分结果映射：失败/池拒绝/超时 → 0 分兜底并诚实标注；无参考答案（Q4）模考无自评交互按 0 分计 */
  private MockJudgedItem toJudgedSubjective(AiReviewMockPaperItemEntity item,
      ReviewQuestionDetail detail, String userAnswer, SubjectiveGradeResult graded) {
    String reference = SubjectiveAnswerExtractor.extract(detail.answerJson());
    if (graded == null) {
      return new MockJudgedItem(true, false, BigDecimal.ZERO, reference, userAnswer,
          "AI 判分失败，本题按 0 分计");
    }
    if (!graded.gradable()) {
      return new MockJudgedItem(true, false, BigDecimal.ZERO, null, userAnswer,
          "本题无参考答案，按 0 分计");
    }
    // 模考口径（Q5）：earned = AI 比例分计入总分；correct 按得分率≥60%
    return new MockJudgedItem(true, graded.correct(), BigDecimal.valueOf(graded.score()),
        reference, userAnswer, graded.comment());
  }

  /** 提交一题判分任务到专用池；AbortPolicy 拒绝返回 null（池满立即重试只会再撞，按失败兜底） */
  private Future<SubjectiveGradeResult> submitMockGrade(Long tenantId, long platformUserId,
      long questionId, String stem, String rawAnswer, String userAnswer, String type, int fullScore) {
    try {
      return aiGradeMockExecutor.submit(() -> {
        // Worker 线程：tenantId/userId 由 HTTP 线程预捕获传参，整条调用链禁止 SecurityContext；
        // 租户上下文每 Worker 线程独立 set + finally clear（异步红线）
        TenantContextHolder.setTenantId(tenantId);
        try {
          return aiGradeService.grade(tenantId, platformUserId, questionId, stem, rawAnswer,
              userAnswer, type, fullScore);
        } finally {
          TenantContextHolder.clear();
        }
      });
    } catch (RejectedExecutionException e) {
      log.warn("[AI判分] 模考判分线程池拒绝（AbortPolicy），本题按 0 分计：questionId={}", questionId);
      return null;
    }
  }

  /** 收取判分结果：单题超时 20s；失败/超时重试 1 次；仍失败返回 null（调用方 0 分兜底） */
  private SubjectiveGradeResult awaitMockGrade(Future<SubjectiveGradeResult> firstAttempt,
      java.util.function.Supplier<Future<SubjectiveGradeResult>> retrySubmit) {
    Future<SubjectiveGradeResult> future = firstAttempt;
    for (int attempt = 1; attempt <= 2 && future != null; attempt++) {
      try {
        return future.get(MOCK_GRADE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
      } catch (TimeoutException e) {
        future.cancel(true);
        log.warn("[AI判分] 模考单题判分超时（第 {}/2 次）", attempt);
      } catch (Exception e) {
        log.warn("[AI判分] 模考单题判分失败（第 {}/2 次）：{}", attempt, e.getMessage());
      }
      if (attempt < 2) {
        future = retrySubmit.get();
      }
    }
    return null;
  }

  /**
   * 模考交卷落库（短事务，自注入代理开启）：并发双交最终裁决（事务内当前读卷状态）→
   * 逐题写作答（同卷幂等跳过）+ 错题卡 + exam_result 同事务直写 → 卷成绩/计划活跃日落库。
   */
  @Transactional
  public Map<String, Object> persistMockSubmit(AiReviewPlanEntity plan, long examId, long mockPaperId,
      long platformUserId, List<AiReviewMockPaperItemEntity> items,
      Map<Long, ReviewQuestionDetail> details, Map<Long, MockJudgedItem> judgedByQid) {
    AiReviewMockPaperEntity paper = mockPaperMapper.selectById(mockPaperId);
    if (paper == null || !paper.getPlanId().equals(plan.getId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "模考卷不存在");
    }
    if ("done".equals(paper.getStatus())) {
      // 并发/重复交卷：回放（不含 wrongList，Q-E 接受现状）
      return mockResultView(plan, paper);
    }

    AiReviewPlanDayEntity todayDay = findDay(plan.getId(), plan.getCurrentDayIndex());
    BigDecimal score = BigDecimal.ZERO;
    int correctCount = 0;
    List<Map<String, Object>> wrongList = new ArrayList<>();
    LocalDate today = LocalDate.now();
    for (AiReviewMockPaperItemEntity item : items) {
      ReviewQuestionDetail detail = details.get(item.getQuestionId());
      MockJudgedItem judged = judgedByQid.get(item.getQuestionId());
      if (detail == null || judged == null) {
        continue;
      }
      if (judged.correct()) {
        correctCount++;
      }
      score = score.add(judged.earned());
      if (!judged.correct()) {
        Map<String, Object> wrong = new LinkedHashMap<>();
        wrong.put("questionId", item.getQuestionId());
        wrong.put("correctAnswer", judged.correctAnswerText());
        wrong.put("resolve", detail.resolve());
        if (judged.earned().compareTo(BigDecimal.ZERO) > 0) {
          wrong.put("partialScore", judged.earned()); // 少选/主观题比例分（展示用）
        }
        if (judged.subjective()) {
          wrong.put("myAnswer", judged.userAnswer());
          wrong.put("aiComment", judged.aiComment());
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
        answer.setEarnedScore(judged.earned());
        answer.setKnowledgeId(detail.knowledgeId());
        answer.setQuestionType(detail.type());
        answer.setRoundType("mock");
        answer.setUserAnswer(judged.userAnswer());
        answer.setIsCorrect(judged.correct() ? 1 : 0);
        answer.setUseTimeSec(0);
        answer.setAttemptSeq(countAttempts(plan.getId(), item.getQuestionId()) + 1);
        answer.setPhpSynced(1);
        answerMapper.insert(answer);
        applyWrongCard(plan, platformUserId, detail, judged.correct());
        // exam_result 事务内同步直写（同 answer() 口径；失败回滚整卷提交）
        examResultWriteRepository.insertExamResult(
            answer.getQuestionType(),
            answer.getExamPlanId() == null ? 0 : answer.getExamPlanId(),
            answer.getQuestionId(),
            answer.getPlatformUserId(),
            answer.getUserAnswer(),
            examResultScoreOf(answer));
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
    AiReviewPlanEntity plan = replanIfExamPlanDrifted(requirePlan(platformUserId, examId));
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
      Map<Long, ReviewQuestion> metaById = questionMetaByIds(plan, mustDo);
      // 知识点标题：知识结构表（exam_knowledge_structure）title 字段
      Map<Long, String> titleById = new HashMap<>();
      for (ReviewKnowledgeNode node : reviewQuery.loadKnowledgeTree(plan.getCourseId())) {
        titleById.put(node.id(), node.title());
      }
      Map<Long, int[]> l3Totals = new HashMap<>();
      for (Long qid : mustDo) {
        ReviewQuestion meta = metaById.get(qid);
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
      if (!reviewQuery.isUserInExam(examId, platformUserId)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "您不在该复习计划名单中");
      }
      if ("abandoned".equals(existing.getStatus())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "该复习计划已放弃");
      }
      return existing;
    }
    try {
      return createPlan(platformUserId, examId, tenantId);
    } catch (DuplicateKeyException ex) {
      // 同一学员并发首次进入（overview/today 同时发起）撞 uk_ai_review_plan_user_exam：当前读取回已提交行
      AiReviewPlanEntity concurrent = planMapper.selectByUserAndExamForUpdate(platformUserId, examId);
      if (concurrent == null) {
        throw ex;
      }
      log.info("[AI复习] 并发建计划命中唯一键，复用已存在计划：user={} exam={} planId={}",
          platformUserId, examId, concurrent.getId());
      if ("abandoned".equals(concurrent.getStatus())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "该复习计划已放弃");
      }
      return concurrent;
    }
  }

  private AiReviewPlanEntity createPlan(long platformUserId, long examId, Long tenantId) {
    if (!reviewQuery.isUserInExam(examId, platformUserId)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "您不在该复习计划名单中");
    }
    ReviewExamPlanInfo planInfo = reviewQuery.loadExamPlanInfo(examId);
    if (planInfo == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "复习计划不存在");
    }
    if (planInfo.bindings().isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "该计划没有正确绑定工种");
    }
    List<Long> courseIds = planInfo.bindings().stream()
        .map(ReviewExamPlanInfo.WorkTypeBinding::courseId)
        .filter(id -> id != null && id > 0)
        .distinct()
        .toList();
    if (courseIds.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "该计划没有正确绑定工种（未匹配到科目）");
    }
    List<Long> l3Ids = reviewQuery.loadL3KnowledgeIds(courseIds);
    int source = planInfo.source() != null ? planInfo.source() : 0;
    List<ReviewQuestion> questions = reviewQuery.loadQuestionScope(source, l3Ids);
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
    ReviewCourseWeights courseWeights = reviewQuery.loadCourseWeights(primaryCourseId);
    List<ReviewKnowledgeNode> tree = reviewQuery.loadKnowledgeTree(primaryCourseId);
    WeightResolver.ResolvedWeights weights = WeightResolver.resolve(courseWeights, tree, questions);
    if ("count".equals(weights.weightSource())) {
      log.warn("[AI复习] course={} 无权重配置，按题量占比兜底", primaryCourseId);
    }

    int target = Math.min(questions.size(), stages[0] * dailyNewCap);
    boolean trimmed = questions.size() > target;
    List<Long> mustDoIds = trimmed
        ? QuestionSelectDomainService.trimQuestionBank(questions, tree, weights, target)
        : questions.stream().map(ReviewQuestion::id).toList();

    AiReviewPlanEntity plan = new AiReviewPlanEntity();
    plan.setPlatformUserId(platformUserId);
    plan.setExamId(examId);
    ReviewExamPlanInfo.WorkTypeBinding firstBinding = planInfo.bindings().get(0);
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
  // 考试计划变更软重排（入口漂移检测：考试日期 + 题目来源）
  // ==================================================================

  /**
   * 考试计划中途变更后的入口重排（软重排，学员进度全保留）。重读 train_exam_list 头部字段，
   * 与计划快照比对两维漂移：
   *
   * <ul>
   *   <li>考试日期漂移：重算总天数与阶段切分（不回退天指针），重置未开始的今日及未来日任务；</li>
   *   <li>题目来源（source）漂移：按新来源重建必做题池——重取题、权重重新解析并快照、
   *       按阶段1容量重裁剪，刷新 source/total_questions/target_questions/must_do_ids/trimmed；
   *       新来源暂无符合口径的题时保持原题池并记 warn，待题库补齐后下次进入自动完成重排。</li>
   * </ul>
   *
   * <p>进度保全：作答记录、错题本（旧来源错题保留，由三级淘汰状态机自然洗出）、
   * 已开始/已完成的天、当前天指针一律不动。任一维漂移都会重置未开始的今日及未来日任务，
   * 下次物化按新节奏/新题池重算配额；受影响天的鼓励语缓存一并失效（旧文案含旧总天数）。
   * 被删阶段3日任务挂着的未开考模考卷成为无引用行，无害。
   *
   * <p>边界：解析不出未来考试日（考试已过/日期清空）只跳过日期维，来源维独立判定；
   * 考试日大幅提前导致新总天数小于已进行天数时按已进行天数保底，学员按每日上限自然收尾。
   *
   * <p>并发：与 materializeDay 同序（先计划行锁、后当天行）；锁后以当前读复核两维快照，
   * App 并发调 overview/today 时只有第一个请求真正重排，其余读到已更新快照直接跳过。
   * 仅 overview/today/advanceDay 三个物化入口调用；progress/finish 不物化无需调。
   *
   * @return 重排后的最新计划（无漂移时原样返回入参）
   */
  public AiReviewPlanEntity replanIfExamPlanDrifted(AiReviewPlanEntity planView) {
    if (planView == null || !"active".equals(planView.getStatus()) || planView.getExamId() == null) {
      // examId 兜底：并发建计划撞唯一键回退的并发行可能未填充
      return planView;
    }
    LocalDate today = LocalDate.now();
    ReviewExamPlanInfo.ExamPlanHead head = reviewQuery.loadExamPlanHead(planView.getExamId());
    if (head == null) {
      return planView;
    }
    LocalDate newExamDate = PlanScheduleDomainService.resolveExamDate(
        head.examDate(), parseDateList(head.examDateList()), today);
    int newSource = head.source() != null ? head.source() : 0;
    boolean viewDateDrift = newExamDate != null && !newExamDate.equals(planView.getExamDate());
    boolean viewSourceDrift = newSource != sourceOf(planView);
    if (!viewDateDrift && !viewSourceDrift) {
      return planView;
    }
    AiReviewPlanEntity plan = planMapper.selectByIdForUpdate(planView.getId());
    if (plan == null) {
      return planView;
    }
    if (!"active".equals(plan.getStatus())) {
      return plan;
    }
    boolean dateDrift = viewDateDrift && !newExamDate.equals(plan.getExamDate());
    boolean sourceDrift = viewSourceDrift && newSource != sourceOf(plan);
    if (!dateDrift && !sourceDrift) {
      // 并发入口已重排过：返回锁内当前读的最新行
      return plan;
    }

    LocalDate oldExamDate = plan.getExamDate();
    int oldTotalDays = plan.getTotalDays();
    String oldStages = plan.getStage1Days() + "/" + plan.getStage2Days() + "/" + plan.getStage3Days();
    int oldSource = sourceOf(plan);
    int oldTotalQuestions = plan.getTotalQuestions() != null ? plan.getTotalQuestions() : 0;
    int oldTargetQuestions = plan.getTargetQuestions() != null ? plan.getTargetQuestions() : 0;

    if (dateDrift) {
      int computedDays = PlanScheduleDomainService.computeTotalDays(newExamDate, today, 0);
      int totalDays = Math.max(computedDays, plan.getCurrentDayIndex());
      int[] stages = PlanScheduleDomainService.splitStages(totalDays);
      plan.setExamDate(newExamDate);
      plan.setTotalDays(totalDays);
      plan.setStage1Days(stages[0]);
      plan.setStage2Days(stages[1]);
      plan.setStage3Days(stages[2]);
      plan.setCurrentStage(stageOfDay(plan, plan.getCurrentDayIndex()));
    }

    boolean poolRebuilt = false;
    if (sourceDrift) {
      // 先日期后题池：阶段1天数变化会影响必做池容量上限
      poolRebuilt = rebuildPoolForSource(plan, newSource);
    }
    if (!dateDrift && !poolRebuilt) {
      // 仅来源漂移且新来源暂无题：原计划保持不动，下次进入再检测
      return planView;
    }

    // 任一维漂移：重置未开始的今日及未来日任务，下次物化按新节奏/新题池重算
    List<AiReviewPlanDayEntity> unstarted =
        dayMapper.selectUnstartedFromForUpdate(plan.getId(), today);
    List<Integer> resetIndexes = unstarted.stream()
        .map(AiReviewPlanDayEntity::getDayIndex)
        .toList();
    int removed = dayMapper.deleteUnstartedFrom(plan.getId(), today);
    planMapper.updateById(plan);
    llmAssistService.evictEncouragement(plan.getId(), resetIndexes);
    log.info("[AI复习] 计划漂移重排：planId={} examDate {}→{} source {}→{} 题量 {}→{} 必做 {}→{} "
            + "totalDays {}→{} 阶段 {}→{}/{}/{} 重置未开始日任务 {} 行 index={}",
        plan.getId(), oldExamDate, plan.getExamDate(), oldSource, plan.getSource(),
        oldTotalQuestions, plan.getTotalQuestions(), oldTargetQuestions, plan.getTargetQuestions(),
        oldTotalDays, plan.getTotalDays(), oldStages,
        plan.getStage1Days(), plan.getStage2Days(), plan.getStage3Days(), removed, resetIndexes);
    return plan;
  }

  /** source 快照归一（建计划口径：null 记 0） */
  private static int sourceOf(AiReviewPlanEntity plan) {
    return plan.getSource() != null ? plan.getSource() : 0;
  }

  /**
   * 来源漂移：按新 source 重建必做题池（同科目，权重重新解析并快照）。裁剪口径与建计划一致：
   * min(新池, 阶段1天数×dailyNewCap)，超出按权重裁剪（§5.3）。
   *
   * @return true=已重建；false=新来源暂无题/主科目缺失，保持原题池不动
   */
  private boolean rebuildPoolForSource(AiReviewPlanEntity plan, int newSource) {
    if (plan.getCourseId() == null) {
      log.warn("[AI复习] 来源变更重排中止：计划无主科目，保留原题池 planId={}", plan.getId());
      return false;
    }
    List<Long> l3Ids = reviewQuery.loadL3KnowledgeIds(List.of(plan.getCourseId()));
    List<ReviewQuestion> questions = reviewQuery.loadQuestionScope(newSource, l3Ids);
    if (questions.isEmpty()) {
      log.warn("[AI复习] 来源变更重排中止：新来源暂无符合口径的题，保留原题池 planId={} source={}",
          plan.getId(), newSource);
      return false;
    }
    ReviewCourseWeights courseWeights = reviewQuery.loadCourseWeights(plan.getCourseId());
    List<ReviewKnowledgeNode> tree = reviewQuery.loadKnowledgeTree(plan.getCourseId());
    WeightResolver.ResolvedWeights weights = WeightResolver.resolve(courseWeights, tree, questions);
    if ("count".equals(weights.weightSource())) {
      log.warn("[AI复习] course={} 无权重配置，来源重排按题量占比兜底", plan.getCourseId());
    }
    int dailyNewCap = plan.getDailyNewCap() != null ? plan.getDailyNewCap() : 120;
    int target = Math.min(questions.size(), Math.max(1, plan.getStage1Days()) * dailyNewCap);
    boolean trimmed = questions.size() > target;
    List<Long> mustDoIds = trimmed
        ? QuestionSelectDomainService.trimQuestionBank(questions, tree, weights, target)
        : questions.stream().map(ReviewQuestion::id).toList();

    plan.setSource(newSource);
    plan.setWeightSnapshot(writeJson(WeightResolver.toSnapshot(weights)));
    plan.setWeightSource(weights.weightSource());
    plan.setTotalQuestions(questions.size());
    plan.setTargetQuestions(mustDoIds.size());
    plan.setMustDoIds(writeJson(mustDoIds));
    plan.setTrimmed(trimmed ? 1 : 0);
    return true;
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
          // 行锁 + 当前读：与 materializeDay 同序（先计划行、后当天行），并发请求不会把指针推进两次
          AiReviewPlanEntity locked = planMapper.selectByIdForUpdate(plan.getId());
          int targetIndex = locked != null
              ? Math.max(nextIndex, locked.getCurrentDayIndex())
              : nextIndex;
          if (targetIndex <= plan.getTotalDays()) {
            plan.setCurrentDayIndex(targetIndex);
            plan.setCurrentStage(stageOfDay(plan, targetIndex));
            planMapper.updateById(plan);
            return getOrMaterializeDay(plan, targetIndex, date, interrupt);
          }
        }
      }
      return existing;
    }
    return materializeDay(plan, dayIndex, date, interrupt);
  }

  /**
   * 物化指定天的任务（overview/today 懒物化与 advanceDay 共用）。
   *
   * <p>幂等：App 进复习页会并发调 overview 与 today，两个请求可能同时决定物化同一天。
   * 这里先锁计划行再以当前读复核，并发落库只会有一行；万一仍撞唯一键（其他来源），
   * 捕获后当前读取回已提交行，不把 500 抛给学员。
   */
  @Transactional
  public AiReviewPlanDayEntity materializeDay(AiReviewPlanEntity plan, int dayIndex,
      LocalDate date, InterruptInfo interrupt) {
    AiReviewPlanDayEntity snapshot = findDay(plan.getId(), dayIndex);
    if (snapshot != null) {
      // 快路径：已物化，无需加锁
      return snapshot;
    }
    planMapper.selectByIdForUpdate(plan.getId());
    AiReviewPlanDayEntity materialized = dayMapper.selectByPlanAndDayForUpdate(plan.getId(), dayIndex);
    if (materialized != null) {
      return materialized;
    }

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
    try {
      dayMapper.insert(day);
    } catch (DuplicateKeyException ex) {
      // 兜底：并发来源抢先落库，当前读取回；仍取不到说明是真冲突，原样抛出
      AiReviewPlanDayEntity concurrent = dayMapper.selectByPlanAndDayForUpdate(plan.getId(), dayIndex);
      if (concurrent == null) {
        throw ex;
      }
      log.info("[AI复习] 并发物化命中唯一键，复用已存在当天任务：planId={} dayIndex={}",
          plan.getId(), dayIndex);
      return concurrent;
    }

    // 异步生成鼓励语
    llmAssistService.generateAsync(plan.getId(), dayIndex, stage, null, null,
        plan.getTotalDays(), null, interrupt != null, plan.getTenantId());
    return day;
  }

  /** 阶段 1 物化（含 D6 权重双轨构成与总量封顶） */
  private void materializeStage1(AiReviewPlanDayEntity day, AiReviewPlanEntity plan,
      List<Long> remainingNew, int newTargetRaw, boolean preheatSingleJudgeOnly) {
    WeightResolver.ResolvedWeights weights = weightsOfPlan(plan);
    Map<Long, ReviewQuestion> metaById = questionMetaByIds(plan, remainingNew);

    List<Long> candidateIds = remainingNew;
    if (preheatSingleJudgeOnly) {
      candidateIds = new ArrayList<>();
      for (Long id : remainingNew) {
        ReviewQuestion meta = metaById.get(id);
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
      ReviewQuestion meta = metaById.get(id);
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
      ReviewQuestion meta = metaById.get(id);
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
    Map<Long, ReviewQuestion> metaById = questionMetaByIds(plan, remainingNew);
    List<Long> sortedRemaining = new ArrayList<>(remainingNew);
    sortedRemaining.sort(Comparator.comparingDouble((Long id) -> {
      ReviewQuestion meta = metaById.get(id);
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
    Map<Long, ReviewQuestion> metaById = questionMetaByIds(plan, mustDo);
    List<ReviewQuestion> pool = new ArrayList<>();
    for (Long id : mustDo) {
      ReviewQuestion meta = metaById.get(id);
      if (meta != null) {
        pool.add(meta);
      }
    }
    WeightResolver.ResolvedWeights weights = weightsOfPlan(plan);
    Map<Long, Double> wrongRateByL3 = wrongRateByL3(plan.getId());
    int mockSize = dictInt("ai_review_mock_size", 100, tenantId);
    List<Long> questionIds = QuestionSelectDomainService.composeMockPaper(
        pool, weights, mockSize, wrongRateByL3, new Random());
    // 主观题数量上限（Q2-A，sys_dict ai_review_mock_subjective_max 默认 5）：同步交卷的墙钟
    // 预算约束（单题 20s + 重试 1 次）。超过按题型优先级裁（cal/answer/essay 复杂主观题优先，
    // 详见 capSubjectiveCount 注释）；被裁名额不回补客观题，总分/及格分按实际入卷题重算。
    int subjectiveMax = dictInt("ai_review_mock_subjective_max", 5, tenantId);
    questionIds = QuestionSelectDomainService.capSubjectiveCount(questionIds, metaById, subjectiveMax);

    int paperNo = mockPaperMapper.selectCount(new LambdaQueryWrapper<AiReviewMockPaperEntity>()
        .eq(AiReviewMockPaperEntity::getPlanId, plan.getId())).intValue() + 1;
    BigDecimal total = BigDecimal.ZERO;
    for (Long qid : questionIds) {
      ReviewQuestion meta = metaById.get(qid);
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
      ReviewQuestion meta = metaById.get(qid);
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
      ReviewQuestionDetail detail, boolean correct) {
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

    // starNew = 本次作答新打上星（此前无星 → 现有星）；已加星红牌答对/再答错时 star 仍为 1，
    // 但那不是"新变化"，前端只在 starNew=1 时提示"加星标记（顽固盲区）"
    // （2026-08-11 实测缺陷：已加星红牌在清算区答对，旧 star 状态被当成新变化下发，误报加星）
    boolean starNew = transition.star() && !state.star();
    return buildCardChange(transition, starNew, correct);
  }

  /**
   * cardChange 视图组装（§7.4 出参）。
   *
   * <p>答错：恒回（新入册/降级/加星/已错次数刷新 + AI 讲解入口 explainAvailable）；
   * 答对：仅牌级变化（红→黄、黄→绿移除）才回，无变化回 null。
   */
  public static Map<String, Object> buildCardChange(WrongCardDomainService.CardTransition transition,
      boolean starNew, boolean correct) {
    if (correct && !transition.levelChanged()) {
      return null;
    }
    Map<String, Object> change = new LinkedHashMap<>();
    change.put("from", transition.fromLevel());
    change.put("to", transition.toLevel());
    change.put("star", transition.star() ? 1 : 0);
    change.put("starNew", starNew ? 1 : 0);
    change.put("explainAvailable", transition.star() && !correct);
    return change;
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
    if (!reviewQuery.isUserInExam(examId, platformUserId)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "您不在该复习计划名单中");
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

  /** exam_result get_score 口径：优先实得分（严格判分部分分）；无实得分则 对=题分/错=0 */
  private static BigDecimal examResultScoreOf(AiReviewAnswerEntity answer) {
    if (answer.getEarnedScore() != null) {
      return answer.getEarnedScore();
    }
    return answer.getIsCorrect() != null && answer.getIsCorrect() == 1
        && answer.getQuestionScore() != null ? answer.getQuestionScore() : BigDecimal.ZERO;
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

  /** 题目轻量元数据（按 id 批量从主库查，排题用） */
  private Map<Long, ReviewQuestion> questionMetaByIds(AiReviewPlanEntity plan, List<Long> questionIds) {
    if (questionIds.isEmpty()) {
      return Map.of();
    }
    return reviewQuery.loadQuestionMetaByIds(questionIds);
  }

  private WeightResolver.ResolvedWeights weightsOfPlan(AiReviewPlanEntity plan) {
    // 权重快照 + 主库知识树重建 L3 权重（快照只有 L1 粒度）
    long courseId = plan.getCourseId();
    List<ReviewKnowledgeNode> tree = reviewQuery.loadKnowledgeTree(courseId);
    ReviewCourseWeights snapshotWeights = snapshotToCourseWeights(plan);
    List<ReviewQuestion> pool = new ArrayList<>(questionMetaByIds(plan, parseIds(plan.getMustDoIds())).values());
    return WeightResolver.resolve(snapshotWeights, tree, pool);
  }

  private ReviewCourseWeights snapshotToCourseWeights(AiReviewPlanEntity plan) {
    if (plan.getWeightSnapshot() == null || plan.getWeightSnapshot().isBlank()) {
      return new ReviewCourseWeights(plan.getWeightSource(), List.of());
    }
    List<ReviewCourseWeights.WeightItem> items = new ArrayList<>();
    for (Map<String, Object> item : parseSnapshot(plan.getWeightSnapshot())) {
      long knowledgeId = ((Number) item.get("knowledgeId")).longValue();
      int rate = (int) Math.round(((Number) item.get("rate")).doubleValue());
      items.add(new ReviewCourseWeights.WeightItem(knowledgeId, (String) item.get("title"), rate));
    }
    return new ReviewCourseWeights(plan.getWeightSource(), items);
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
    view.put("mock", mockViewWithStage2Parallel(plan, day));
    return view;
  }

  /**
   * 今日模考视图：阶段 3 的天有物化时预组的卷（mockPaperId 驱动，原口径）；
   * 阶段 2（定向爆破）起开放并行动模考——不预组卷、交卷不标记当日完成（清算照常），
   * 入口进入仿真模考页后由 mockStart 现场组卷，不限次（现状口径）。
   * 开关：sys_dict ai_review_mock_stage2_enabled（缺省开；置 0 关闭，安静回退到仅阶段 3 开放）。
   */
  Map<String, Object> mockViewWithStage2Parallel(AiReviewPlanEntity plan, AiReviewPlanDayEntity day) {
    Map<String, Object> paperView = mockView(day);
    if (paperView != null) {
      return paperView;
    }
    Integer stage = day.getStage();
    if (stage == null || stage < 2) {
      return null;
    }
    if (dictInt("ai_review_mock_stage2_enabled", 1, plan.getTenantId()) != 1) {
      return null;
    }
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("available", true);
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
