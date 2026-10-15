package com.guoyu.platform.exam.reviewsync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.reviewsync.dto.ReviewQuestionDetail;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewAnswerEntity;
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
import com.guoyu.platform.exam.reviewsync.service.AiGradeApplicationService.SubjectiveGradeResult;
import com.guoyu.platform.system.mapper.DictMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 模考交卷 mockSubmit 三段式（《简答题作答与AI判分方案》§3.4 Q2-A）：
 * 客观题判分不受影响；主观题并行判分（线程池桩为同步执行）；总分含 AI 比例分；
 * 单题失败重试 1 次仍失败 / AbortPolicy 拒绝 → 0 分兜底 + wrongList 诚实标注；
 * 重复交卷回放 mockResultView 不含 wrongList。Worker 线程租户上下文由参数传入并断言。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewMockSubjectiveGradeTest {

  @Mock
  private AiReviewPlanMapper planMapper;
  @Mock
  private AiReviewPlanDayMapper dayMapper;
  @Mock
  private AiReviewAnswerMapper answerMapper;
  @Mock
  private AiReviewWrongItemMapper wrongMapper;
  @Mock
  private AiReviewMockPaperMapper mockPaperMapper;
  @Mock
  private AiReviewMockPaperItemMapper mockItemMapper;
  @Mock
  private AiReviewGrayWhitelistMapper grayWhitelistMapper;
  @Mock
  private ReviewQuestionQueryService reviewQuery;
  @Mock
  private ExamResultWriteRepository examResultWriteRepository;
  @Mock
  private ReviewLlmAssistService llmAssistService;
  @Mock
  private DictMapper dictMapper;
  @Mock
  private AiGradeApplicationService aiGradeService;
  @Mock
  private ThreadPoolTaskExecutor aiGradeMockExecutor;

  private ReviewPlanApplicationService service;
  private AiReviewMockPaperEntity paper;

  private static final String SINGLE_ANSWER_JSON =
      "[{\"content\":\"对\",\"isCorrect\":true},{\"content\":\"错\",\"isCorrect\":false}]";
  private static final String REFERENCE = "参考答案：电解氧化铝得到铝";
  private static final List<Map<String, Object>> ANSWERS = List.of(
      Map.of("questionId", 901, "userAnswer", "对"),
      Map.of("questionId", 902, "userAnswer", "我的作答"));

  @BeforeEach
  void setUp() {
    service = new ReviewPlanApplicationService(planMapper, dayMapper, answerMapper, wrongMapper,
        mock(com.guoyu.platform.exam.reviewsync.mapper.AiReviewInterruptLogMapper.class),
        mockPaperMapper, mockItemMapper, grayWhitelistMapper, reviewQuery, examResultWriteRepository,
        llmAssistService, dictMapper, new ObjectMapper(), aiGradeService, aiGradeMockExecutor);
    // 纯单测无 Spring 代理：self 注入自身，短事务落库方法直调
    service.setSelf(service);

    AiReviewPlanEntity plan = new AiReviewPlanEntity();
    plan.setId(100L);
    plan.setTenantId(10159L);
    plan.setCurrentDayIndex(1);
    when(planMapper.selectOne(any())).thenReturn(plan);
    when(reviewQuery.isUserInExam(anyLong(), anyLong())).thenReturn(true);
    when(dictMapper.selectList(any())).thenReturn(List.of());

    paper = new AiReviewMockPaperEntity();
    paper.setId(700L);
    paper.setPlanId(100L);
    paper.setStatus("ready");
    paper.setQuestionCount(2);
    paper.setTotalScore(new BigDecimal("7"));
    paper.setPassScore(new BigDecimal("4.2"));
    when(mockPaperMapper.selectById(700L)).thenReturn(paper);

    AiReviewMockPaperItemEntity item1 = new AiReviewMockPaperItemEntity();
    item1.setPaperId(700L);
    item1.setQuestionId(901L);
    item1.setQuestionType("single");
    item1.setSortNumber(1);
    item1.setScore(new BigDecimal("2"));
    AiReviewMockPaperItemEntity item2 = new AiReviewMockPaperItemEntity();
    item2.setPaperId(700L);
    item2.setQuestionId(902L);
    item2.setQuestionType("answer");
    item2.setSortNumber(2);
    item2.setScore(new BigDecimal("5"));
    when(mockItemMapper.selectList(any())).thenReturn(List.of(item1, item2));

    Map<Long, ReviewQuestionDetail> details = new HashMap<>();
    details.put(901L, new ReviewQuestionDetail(
        901L, 7L, "single", 1, new BigDecimal("2"), "题干", SINGLE_ANSWER_JSON, "解析"));
    details.put(902L, new ReviewQuestionDetail(
        902L, 7L, "answer", 1, new BigDecimal("5"), "题干：简述铝电解原理", REFERENCE, "解析"));
    when(reviewQuery.loadQuestionDetails(any())).thenReturn(details);

    AiReviewPlanDayEntity day = new AiReviewPlanDayEntity();
    day.setId(500L);
    day.setPlanId(100L);
    when(dayMapper.selectOne(any())).thenReturn(day);
    when(answerMapper.selectCount(any())).thenReturn(0L);
  }

  @AfterEach
  void tearDown() {
    TenantContextHolder.clear();
  }

  /** 线程池桩：同步执行（任务在当前线程立即跑完），真实异步行为由集成环境覆盖 */
  @SuppressWarnings("unchecked")
  private void stubExecutorInline() {
    when(aiGradeMockExecutor.submit(any(Callable.class))).thenAnswer(inv -> {
      Callable<SubjectiveGradeResult> task = (Callable<SubjectiveGradeResult>) inv.getArgument(0);
      try {
        return CompletableFuture.completedFuture(task.call());
      } catch (Exception e) {
        return CompletableFuture.failedFuture(e);
      }
    });
  }

  // ------------------------------------------------------------------
  // 总分含主观题 AI 比例分（Q5）+ Worker 租户上下文断言
  // ------------------------------------------------------------------

  @Test
  void totalScoreIncludesSubjectiveEarnedScore() {
    stubExecutorInline();
    List<Long> tenantSeenInWorker = new ArrayList<>();
    when(aiGradeService.grade(eq(10159L), eq(42L), eq(902L), anyString(), eq(REFERENCE),
        eq("我的作答"), eq("answer"), eq(5)))
        .thenAnswer(inv -> {
          // Worker 线程内租户上下文由编排侧 set（禁止 SecurityContext 的配套证据）
          tenantSeenInWorker.add(TenantContextHolder.getTenantId());
          return new SubjectiveGradeResult(true, 4, true, "要点齐全", "ai");
        });

    Map<String, Object> result = service.mockSubmit(42L, 440L, 700L, ANSWERS, 120, 10159L);

    // 客观 2 分（对=题分）+ 主观 AI 比例分 4 = 6
    assertThat((BigDecimal) result.get("score")).isEqualByComparingTo("6");
    assertThat(result.get("correctCount")).isEqualTo(2);
    assertThat(result.get("wrongCount")).isEqualTo(0);
    assertThat((List<?>) result.get("wrongList")).isEmpty();
    assertThat(tenantSeenInWorker).containsExactly(10159L);

    ArgumentCaptor<AiReviewMockPaperEntity> paperCaptor =
        ArgumentCaptor.forClass(AiReviewMockPaperEntity.class);
    verify(mockPaperMapper).updateById(paperCaptor.capture());
    assertThat(paperCaptor.getValue().getScore()).isEqualByComparingTo("6");
    assertThat(paperCaptor.getValue().getCorrectCount()).isEqualTo(2);
    assertThat(paperCaptor.getValue().getStatus()).isEqualTo("done");

    // 主观题作答落库：earnedScore=4，exam_result 直写同口径
    verify(examResultWriteRepository).insertExamResult(
        "answer", 440L, 902L, 42L, "我的作答", new BigDecimal("4"));
  }

  // ------------------------------------------------------------------
  // W2.4 模考 per-question 计时落库（原恒写 0）
  // ------------------------------------------------------------------

  @Test
  void mockSubmitPersistsPerQuestionUseTimeSec() {
    stubExecutorInline();
    when(aiGradeService.grade(eq(10159L), eq(42L), eq(902L), anyString(), anyString(), anyString(),
        anyString(), eq(5)))
        .thenReturn(new SubjectiveGradeResult(true, 4, true, "要点齐全", "ai"));

    List<Map<String, Object>> answers = List.of(
        Map.of("questionId", 901, "userAnswer", "对", "useTimeSec", 45),
        Map.of("questionId", 902, "userAnswer", "我的作答", "useTimeSec", 88));

    service.mockSubmit(42L, 440L, 700L, answers, 120, 10159L);

    ArgumentCaptor<AiReviewAnswerEntity> answerCaptor =
        ArgumentCaptor.forClass(AiReviewAnswerEntity.class);
    verify(answerMapper, times(2)).insert(answerCaptor.capture());
    Map<Long, Integer> timeByQid = new HashMap<>();
    for (AiReviewAnswerEntity answer : answerCaptor.getAllValues()) {
      timeByQid.put(answer.getQuestionId(), answer.getUseTimeSec());
    }
    assertThat(timeByQid.get(901L)).isEqualTo(45);
    assertThat(timeByQid.get(902L)).isEqualTo(88);
  }

  @Test
  void mockSubmitDefaultsUseTimeSecToZeroWhenAbsent() {
    stubExecutorInline();
    when(aiGradeService.grade(eq(10159L), eq(42L), eq(902L), anyString(), anyString(), anyString(),
        anyString(), eq(5)))
        .thenReturn(new SubjectiveGradeResult(true, 4, true, "要点齐全", "ai"));

    service.mockSubmit(42L, 440L, 700L, ANSWERS, 120, 10159L);

    ArgumentCaptor<AiReviewAnswerEntity> answerCaptor =
        ArgumentCaptor.forClass(AiReviewAnswerEntity.class);
    verify(answerMapper, times(2)).insert(answerCaptor.capture());
    for (AiReviewAnswerEntity answer : answerCaptor.getAllValues()) {
      assertThat(answer.getUseTimeSec()).isEqualTo(0);
    }
  }

  // ------------------------------------------------------------------
  // 单题判分失败：重试 1 次仍失败 → 0 分 + wrongList 诚实标注
  // ------------------------------------------------------------------

  @Test
  void gradeExceptionRetriesOnceThenZeroWithAnnotation() {
    stubExecutorInline();
    when(aiGradeService.grade(eq(10159L), eq(42L), eq(902L), anyString(), anyString(), anyString(),
        anyString(), eq(5)))
        .thenThrow(new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 判分失败，请自评"));

    Map<String, Object> result = service.mockSubmit(42L, 440L, 700L, ANSWERS, 120, 10159L);

    // 失败重试 1 次（共 2 次调用）
    verify(aiGradeService, times(2)).grade(eq(10159L), eq(42L), eq(902L), anyString(), anyString(),
        anyString(), anyString(), eq(5));
    // 客观题判分不受影响：2 分计入总分
    assertThat((BigDecimal) result.get("score")).isEqualByComparingTo("2");
    assertThat(result.get("correctCount")).isEqualTo(1);

    List<Map<String, Object>> wrongList = (List<Map<String, Object>>) result.get("wrongList");
    assertThat(wrongList).hasSize(1);
    Map<String, Object> wrong = wrongList.get(0);
    assertThat(wrong.get("questionId")).isEqualTo(902L);
    assertThat(wrong.get("aiComment")).isEqualTo("AI 判分失败，本题按 0 分计");
    assertThat(wrong.get("myAnswer")).isEqualTo("我的作答");
    assertThat(wrong.get("correctAnswer")).isEqualTo(REFERENCE);
    assertThat(wrong).doesNotContainKey("partialScore");

    // 兜底 0 分落库并入错题本
    ArgumentCaptor<com.guoyu.platform.exam.reviewsync.entity.AiReviewAnswerEntity> captor =
        ArgumentCaptor.forClass(com.guoyu.platform.exam.reviewsync.entity.AiReviewAnswerEntity.class);
    verify(answerMapper, times(2)).insert(captor.capture());
    com.guoyu.platform.exam.reviewsync.entity.AiReviewAnswerEntity subjective = captor.getAllValues()
        .stream().filter(a -> a.getQuestionId() == 902L).findFirst().orElseThrow();
    assertThat(subjective.getEarnedScore()).isEqualByComparingTo("0");
    assertThat(subjective.getIsCorrect()).isEqualTo(0);
    verify(wrongMapper).insert(any(AiReviewWrongItemEntity.class));
  }

  // ------------------------------------------------------------------
  // 线程池拒绝（AbortPolicy）：0 分兜底，不调 LLM
  // ------------------------------------------------------------------

  @Test
  void poolRejectionFallsBackToZero() {
    when(aiGradeMockExecutor.submit(any(Callable.class)))
        .thenThrow(new RejectedExecutionException("pool full"));

    Map<String, Object> result = service.mockSubmit(42L, 440L, 700L, ANSWERS, 120, 10159L);

    verifyNoInteractions(aiGradeService);
    assertThat((BigDecimal) result.get("score")).isEqualByComparingTo("2");
    List<Map<String, Object>> wrongList = (List<Map<String, Object>>) result.get("wrongList");
    assertThat(wrongList).hasSize(1);
    assertThat(wrongList.get(0).get("aiComment")).isEqualTo("AI 判分失败，本题按 0 分计");
    assertThat(wrongList.get(0).get("myAnswer")).isEqualTo("我的作答");
  }

  // ------------------------------------------------------------------
  // 单题超时：取消 + 重试 1 次，采信重试结果
  // ------------------------------------------------------------------

  @Test
  @SuppressWarnings("unchecked")
  void timeoutRetriesOnceAndUsesSecondResult() throws Exception {
    Future<SubjectiveGradeResult> timeoutFuture = mock(Future.class);
    when(timeoutFuture.get(anyLong(), any(TimeUnit.class))).thenThrow(new TimeoutException());
    when(aiGradeMockExecutor.submit(any(Callable.class)))
        .thenReturn((Future) timeoutFuture)
        .thenAnswer(inv -> {
          Callable<SubjectiveGradeResult> task = (Callable<SubjectiveGradeResult>) inv.getArgument(0);
          try {
            return CompletableFuture.completedFuture(task.call());
          } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
          }
        });
    when(aiGradeService.grade(eq(10159L), eq(42L), eq(902L), anyString(), anyString(), anyString(),
        anyString(), eq(5)))
        .thenReturn(new SubjectiveGradeResult(true, 4, true, "要点齐全", "ai"));

    Map<String, Object> result = service.mockSubmit(42L, 440L, 700L, ANSWERS, 120, 10159L);

    verify(timeoutFuture).cancel(true);
    // 重试成功：总分 = 客观 2 + 主观 4
    assertThat((BigDecimal) result.get("score")).isEqualByComparingTo("6");
    assertThat((List<?>) result.get("wrongList")).isEmpty();
  }

  // ------------------------------------------------------------------
  // 重复交卷回放：mockResultView 语义不动（不含 wrongList），不触发判分
  // ------------------------------------------------------------------

  @Test
  void replayDonePaperReturnsResultViewWithoutWrongList() {
    paper.setStatus("done");
    paper.setScore(new BigDecimal("6"));
    paper.setCorrectCount(2);

    Map<String, Object> result = service.mockSubmit(42L, 440L, 700L, ANSWERS, 120, 10159L);

    assertThat(result).doesNotContainKey("wrongList");
    assertThat((BigDecimal) result.get("score")).isEqualByComparingTo("6");
    assertThat(result.get("correctCount")).isEqualTo(2);
    verifyNoInteractions(aiGradeService);
    verify(aiGradeMockExecutor, never()).submit(any(Callable.class));
    verify(mockPaperMapper, never()).updateById(any(AiReviewMockPaperEntity.class));
    verify(answerMapper, never()).insert(
        any(com.guoyu.platform.exam.reviewsync.entity.AiReviewAnswerEntity.class));
  }

  // ------------------------------------------------------------------
  // 评审 #3（mock 侧）：并发双交撞 uk → 回放已落库结果，不吃 500
  // ------------------------------------------------------------------

  @Test
  void duplicateKeyOnPersistReplaysResultViewInsteadOf500() {
    stubExecutorInline();
    when(aiGradeService.grade(eq(10159L), eq(42L), eq(902L), anyString(), eq(REFERENCE),
        eq("我的作答"), eq("answer"), eq(5)))
        .thenReturn(new SubjectiveGradeResult(true, 4, true, "要点齐全", "ai"));
    // 并发先到者已提交、本事务 RR 快照不可见 → insert 撞 uk
    when(answerMapper.insert(any(com.guoyu.platform.exam.reviewsync.entity.AiReviewAnswerEntity.class)))
        .thenThrow(new org.springframework.dao.DuplicateKeyException("uk_ai_review_answer_once"));

    Map<String, Object> result = service.mockSubmit(42L, 440L, 700L, ANSWERS, 120, 10159L);

    // 回放 mockResultView（不含 wrongList），不抛 500
    assertThat(result).doesNotContainKey("wrongList");
  }
}
