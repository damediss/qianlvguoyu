package com.guoyu.platform.exam.reviewsync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.exam.reviewsync.dto.ReviewQuestionDetail;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewAnswerEntity;
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
import java.util.List;
import java.util.Map;
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
 * 单题作答 answer() 主观题分支（《简答题作答与AI判分方案》§3.4 / §5-5）：
 * 白名单走 AI 判分路径且客观题路径不回归；ungradeable 不调 LLM 不落库；自评通道落库；
 * 短事务内幂等裁决（并发双提交只落一次）；改答案重判只加差额；correctAnswer 答对也返回。
 * 桩 AiGradeApplicationService/Mapper，不依赖真实 LLM/Redis。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewPlanSubjectiveAnswerTest {

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
  private AiReviewPlanDayEntity day;

  private static final String SINGLE_ANSWER_JSON =
      "[{\"content\":\"对\",\"isCorrect\":true},{\"content\":\"错\",\"isCorrect\":false}]";
  private static final String REFERENCE = "参考答案：电解氧化铝得到铝";

  @BeforeEach
  void setUp() {
    service = new ReviewPlanApplicationService(planMapper, dayMapper, answerMapper, wrongMapper,
        mockPaperMapper, mockItemMapper, grayWhitelistMapper, reviewQuery, examResultWriteRepository,
        llmAssistService, dictMapper, new ObjectMapper(), aiGradeService, aiGradeMockExecutor);
    // 纯单测无 Spring 代理：self 注入自身，短事务落库方法直调
    service.setSelf(service);

    AiReviewPlanEntity plan = new AiReviewPlanEntity();
    plan.setId(100L);
    plan.setTenantId(10159L);
    plan.setCurrentDayIndex(1);
    when(planMapper.selectOne(any())).thenReturn(plan);

    day = new AiReviewPlanDayEntity();
    day.setId(500L);
    day.setPlanId(100L);
    day.setStatus("pending");
    day.setDayIndex(1);
    when(dayMapper.selectById(500L)).thenReturn(day);

    when(reviewQuery.isUserInExam(anyLong(), anyLong())).thenReturn(true);
    when(dictMapper.selectList(any())).thenReturn(List.of());
    when(answerMapper.selectList(any())).thenReturn(List.of());
    when(answerMapper.selectCount(any())).thenReturn(0L);

    // 900：主观题（简答）；901：客观题（单选）；902：无参考答案主观题；903：提取失败主观题
    when(reviewQuery.loadQuestionDetail(900L)).thenReturn(new ReviewQuestionDetail(
        900L, 7L, "answer", 1, new BigDecimal("5"), "题干：简述铝电解原理", REFERENCE, "解析文本"));
    when(reviewQuery.loadQuestionDetail(901L)).thenReturn(new ReviewQuestionDetail(
        901L, 7L, "single", 1, new BigDecimal("2"), "题干", SINGLE_ANSWER_JSON, "解析"));
    when(reviewQuery.loadQuestionDetail(902L)).thenReturn(new ReviewQuestionDetail(
        902L, 7L, "answer", 1, new BigDecimal("5"), "题干：无答案题", "", "解析"));
    when(reviewQuery.loadQuestionDetail(903L)).thenReturn(new ReviewQuestionDetail(
        903L, 7L, "cal", 1, new BigDecimal("5"), "题干：提取失败题", "[{\"content\":\"\"}]", "解析"));
  }

  // ------------------------------------------------------------------
  // 白名单分流：主观题走 AI 判分，客观题不回归
  // ------------------------------------------------------------------

  @Test
  void subjectiveAiGradePathReturnsExtendedView() {
    when(aiGradeService.grade(eq(10159L), eq(42L), eq(900L), anyString(), eq(REFERENCE),
        eq("我的作答"), eq("answer"), eq(5)))
        .thenReturn(new SubjectiveGradeResult(true, 4, true, "要点齐全", "ai"));

    Map<String, Object> result = service.answer(42L, 440L, 500L, 900L, "new", "我的作答", 5, 10159L);

    assertThat(result.get("correct")).isEqualTo(1);
    // 主观题 correctAnswer 恒返回（答对也返回）= 提取的参考答案文本
    assertThat(result.get("correctAnswer")).isEqualTo(REFERENCE);
    assertThat(result.get("aiComment")).isEqualTo("要点齐全");
    assertThat(result.get("gradeSource")).isEqualTo("ai");
    assertThat(result.get("ungradeable")).isEqualTo(0);

    // 落库：isCorrect=1，earnedScore=AI 比例分 4
    ArgumentCaptor<AiReviewAnswerEntity> captor = ArgumentCaptor.forClass(AiReviewAnswerEntity.class);
    verify(answerMapper).insert(captor.capture());
    AiReviewAnswerEntity saved = captor.getValue();
    assertThat(saved.getIsCorrect()).isEqualTo(1);
    assertThat(saved.getEarnedScore()).isEqualByComparingTo("4");
    assertThat(saved.getQuestionType()).isEqualTo("answer");
    assertThat(saved.getPhpSynced()).isEqualTo(1);

    // exam_result 同事务直写：get_score=earnedScore（AI 比例分）
    verify(examResultWriteRepository).insertExamResult(
        "answer", 440L, 900L, 42L, "我的作答", new BigDecimal("4"));
  }

  @Test
  void objectivePathNeverTouchesAiGrade() {
    Map<String, Object> result = service.answer(42L, 440L, 500L, 901L, "new", "对", 5, 10159L);

    assertThat(result.get("correct")).isEqualTo(1);
    assertThat(result.get("correctAnswer")).isEqualTo("对");
    // 客观题响应不含主观题扩展字段
    assertThat(result).doesNotContainKeys("aiComment", "ungradeable", "gradeSource");
    verifyNoInteractions(aiGradeService);
    verify(examResultWriteRepository).insertExamResult(
        "single", 440L, 901L, 42L, "对", new BigDecimal("2"));
  }

  // ------------------------------------------------------------------
  // 无参考答案：ungradeable=1，不调 LLM 不落判分
  // ------------------------------------------------------------------

  @Test
  void ungradeableWhenReferenceEmpty() {
    Map<String, Object> result = service.answer(42L, 440L, 500L, 902L, "new", "我的作答", 5, 10159L);

    assertThat(result.get("ungradeable")).isEqualTo(1);
    assertThat(result.get("correctAnswer")).isNull();
    assertThat(result.get("correct")).isEqualTo(0);
    assertThat(result.get("resolve")).isEqualTo("解析");
    verifyNoInteractions(aiGradeService);
    verify(answerMapper, never()).insert(any(AiReviewAnswerEntity.class));
    verifyNoInteractions(examResultWriteRepository);
  }

  @Test
  void ungradeableWhenReferenceExtractionFails() {
    Map<String, Object> result = service.answer(42L, 440L, 500L, 903L, "new", "我的作答", 5, 10159L);

    assertThat(result.get("ungradeable")).isEqualTo(1);
    assertThat(result.get("correctAnswer")).isNull();
    verifyNoInteractions(aiGradeService);
    verify(answerMapper, never()).insert(any(AiReviewAnswerEntity.class));
  }

  // ------------------------------------------------------------------
  // AI 判分失败兜底（§6）：gradeFailed=1 + 参考答案随响应下发，不落库，前端转自评
  // ------------------------------------------------------------------

  @Test
  void gradeFailureReturnsGradeFailedWithReferenceAndPersistsNothing() {
    when(aiGradeService.grade(eq(10159L), eq(42L), eq(900L), anyString(), eq(REFERENCE),
        eq("我的作答"), eq("answer"), eq(5)))
        .thenThrow(new com.guoyu.platform.common.exception.BusinessException(
            com.guoyu.platform.common.exception.ErrorCode.TOO_MANY_REQUESTS, "AI 判分次数已达上限"));

    Map<String, Object> result = service.answer(42L, 440L, 500L, 900L, "new", "我的作答", 5, 10159L);

    assertThat(result.get("gradeFailed")).isEqualTo(1);
    assertThat(result.get("correctAnswer")).isEqualTo(REFERENCE);
    assertThat(result.get("resolve")).isEqualTo("解析文本");
    assertThat(result.get("gradeFailMsg")).isEqualTo("AI 判分次数已达上限");
    assertThat(result.get("ungradeable")).isEqualTo(0);
    // 不落库：自评结论由 selfAssessed=1 再次提交时才写入
    verify(answerMapper, never()).insert(any(AiReviewAnswerEntity.class));
    verifyNoInteractions(examResultWriteRepository);
  }

  @Test
  void gradeUnexpectedExceptionAlsoFallsBackToSelfAssess() {
    when(aiGradeService.grade(eq(10159L), eq(42L), eq(900L), anyString(), eq(REFERENCE),
        eq("我的作答"), eq("answer"), eq(5)))
        .thenThrow(new RuntimeException("boom"));

    Map<String, Object> result = service.answer(42L, 440L, 500L, 900L, "new", "我的作答", 5, 10159L);

    assertThat(result.get("gradeFailed")).isEqualTo(1);
    assertThat(result.get("correctAnswer")).isEqualTo(REFERENCE);
    verify(answerMapper, never()).insert(any(AiReviewAnswerEntity.class));
  }

  // ------------------------------------------------------------------
  // 评审 #1：无参考答案题的自评必须落库（否则当日任务永卡）
  // 评审 #3：真并发撞 uk_ai_review_answer_once → 回放先到者结果，不吃 500
  // ------------------------------------------------------------------

  @Test
  void selfAssessedWithMissingReferenceStillPersists() {
    // 902：无参考答案主观题；学员经自评 UI 以 selfAssessed=1 再次提交
    Map<String, Object> result = service.answer(42L, 440L, 500L, 902L, "new", "我的作答", 5, 10159L,
        1, 0);

    assertThat(result.get("gradeSource")).isEqualTo("self");
    assertThat(result.get("correct")).isEqualTo(0);
    assertThat(result.get("correctAnswer")).isNull();
    ArgumentCaptor<AiReviewAnswerEntity> captor = ArgumentCaptor.forClass(AiReviewAnswerEntity.class);
    verify(answerMapper).insert(captor.capture());
    assertThat(captor.getValue().getIsCorrect()).isEqualTo(0);
    assertThat(captor.getValue().getQuestionType()).isEqualTo("answer");
    // 自评判错照常入错题本
    verify(wrongMapper).insert(any(AiReviewWrongItemEntity.class));
  }

  @Test
  void duplicateKeyOnInsertReplaysWinnerInsteadOf500() {
    when(aiGradeService.grade(eq(10159L), eq(42L), eq(900L), anyString(), eq(REFERENCE),
        eq("我的作答"), eq("answer"), eq(5)))
        .thenReturn(new SubjectiveGradeResult(true, 4, true, "要点齐全", "ai"));
    // 预检（第一次 selectOne）无记录 → 走判分；insert 撞 uk → 兜底当前读（第二次 selectOne）见到先到者
    AiReviewAnswerEntity winner = new AiReviewAnswerEntity();
    winner.setIsCorrect(1);
    winner.setUserAnswer("我的作答");
    when(answerMapper.selectOne(any())).thenReturn(null, winner);
    when(answerMapper.insert(any(AiReviewAnswerEntity.class)))
        .thenThrow(new org.springframework.dao.DuplicateKeyException("uk_ai_review_answer_once"));

    Map<String, Object> result = service.answer(42L, 440L, 500L, 900L, "new", "我的作答", 5, 10159L);

    // 回放先到者结果：correct=1 + 参考答案，无 500
    assertThat(result.get("correct")).isEqualTo(1);
    assertThat(result.get("correctAnswer")).isEqualTo(REFERENCE);
  }

  // ------------------------------------------------------------------
  // 自评通道（selfAssessed=1）：采信前端结论落库，gradeSource='self'，不调 LLM
  // ------------------------------------------------------------------

  @Test
  void selfAssessedWrongPersistsSelfAndCreatesWrongCard() {
    Map<String, Object> result = service.answer(42L, 440L, 500L, 900L, "new", "我的作答", 5, 10159L,
        1, 0);

    assertThat(result.get("correct")).isEqualTo(0);
    assertThat(result.get("gradeSource")).isEqualTo("self");
    assertThat(result.get("ungradeable")).isEqualTo(0);
    // 自评不调 LLM
    verifyNoInteractions(aiGradeService);

    ArgumentCaptor<AiReviewAnswerEntity> captor = ArgumentCaptor.forClass(AiReviewAnswerEntity.class);
    verify(answerMapper).insert(captor.capture());
    assertThat(captor.getValue().getIsCorrect()).isEqualTo(0);
    // 自评 earnedScore 留空（exam_result 回退"对=题分/错=0"口径）
    assertThat(captor.getValue().getEarnedScore()).isNull();

    // 答错照常入错题本
    verify(wrongMapper).insert(any(AiReviewWrongItemEntity.class));
    verify(examResultWriteRepository).insertExamResult(
        "answer", 440L, 900L, 42L, "我的作答", BigDecimal.ZERO);
  }

  @Test
  void selfAssessedCorrectSkipsWrongCard() {
    Map<String, Object> result = service.answer(42L, 440L, 500L, 900L, "new", "我的作答", 5, 10159L,
        1, 1);

    assertThat(result.get("correct")).isEqualTo(1);
    assertThat(result.get("gradeSource")).isEqualTo("self");
    verifyNoInteractions(aiGradeService);
    verify(wrongMapper, never()).insert(any(AiReviewWrongItemEntity.class));
    // earnedScore 空 → exam_result 对=题分 5
    verify(examResultWriteRepository).insertExamResult(
        "answer", 440L, 900L, 42L, "我的作答", new BigDecimal("5"));
  }

  // ------------------------------------------------------------------
  // 幂等：并发同题同文本双提交，短事务内裁决只落一次；改答案重判只加差额
  // ------------------------------------------------------------------

  @Test
  void concurrentSameAnswerDoubleSubmitReplaysInShortTx() {
    // 预检（第 1 次查）无记录 → 走完 AI 判分；短事务内最终裁决（第 2 次查）发现并发先到者已落库
    AiReviewAnswerEntity committed = new AiReviewAnswerEntity();
    committed.setUserAnswer("我的作答");
    committed.setIsCorrect(1);
    when(answerMapper.selectOne(any())).thenReturn(null, committed);
    when(aiGradeService.grade(eq(10159L), eq(42L), eq(900L), anyString(), anyString(), anyString(),
        anyString(), eq(5)))
        .thenReturn(new SubjectiveGradeResult(true, 5, true, "很好", "ai"));

    Map<String, Object> result = service.answer(42L, 440L, 500L, 900L, "new", "我的作答", 5, 10159L);

    // 回放已存结果：判对 + 恒返回参考答案；只判分一次、零落库
    assertThat(result.get("correct")).isEqualTo(1);
    assertThat(result.get("correctAnswer")).isEqualTo(REFERENCE);
    verify(answerMapper, never()).insert(any(AiReviewAnswerEntity.class));
    verifyNoInteractions(examResultWriteRepository);
    verify(wrongMapper, never()).insert(any(AiReviewWrongItemEntity.class));
  }

  @Test
  void changedAnswerRegradesAndBumpsOnlyDelta() {
    // 此前答错 + 新答案不同 → 改答案重判（计数只加差额）
    AiReviewAnswerEntity existing = new AiReviewAnswerEntity();
    existing.setUserAnswer("旧答案");
    existing.setIsCorrect(0);
    when(answerMapper.selectOne(any())).thenReturn(existing);
    when(answerMapper.selectCount(any())).thenReturn(1L);
    day.setNewDone(1);
    day.setAnswerCount(1);
    day.setCorrectCount(0);
    when(aiGradeService.grade(eq(10159L), eq(42L), eq(900L), anyString(), anyString(),
        eq("新答案"), eq("answer"), eq(5)))
        .thenReturn(new SubjectiveGradeResult(true, 5, true, "完全正确", "ai"));

    Map<String, Object> result = service.answer(42L, 440L, 500L, 900L, "new", "新答案", 5, 10159L);

    assertThat(result.get("correct")).isEqualTo(1);
    verify(aiGradeService).grade(eq(10159L), eq(42L), eq(900L), anyString(), anyString(),
        eq("新答案"), eq("answer"), eq(5));
    ArgumentCaptor<AiReviewAnswerEntity> captor = ArgumentCaptor.forClass(AiReviewAnswerEntity.class);
    verify(answerMapper).insert(captor.capture());
    assertThat(captor.getValue().getAttemptSeq()).isEqualTo(2);
    assertThat(captor.getValue().getUserAnswer()).isEqualTo("新答案");
    // 差额计数：newDone 不重复计；answerCount+1；correctCount 由错转对 +1
    assertThat(day.getNewDone()).isEqualTo(1);
    assertThat(day.getAnswerCount()).isEqualTo(2);
    assertThat(day.getCorrectCount()).isEqualTo(1);
  }

  @Test
  void replaySameAnswerSkipsGradeAndWrites() {
    AiReviewAnswerEntity existing = new AiReviewAnswerEntity();
    existing.setUserAnswer("我的作答");
    existing.setIsCorrect(0);
    when(answerMapper.selectOne(any())).thenReturn(existing);

    Map<String, Object> result = service.answer(42L, 440L, 500L, 900L, "new", "我的作答", 5, 10159L);

    assertThat(result.get("correct")).isEqualTo(0);
    assertThat(result.get("correctAnswer")).isEqualTo(REFERENCE);
    assertThat(result.get("ungradeable")).isEqualTo(0);
    verifyNoInteractions(aiGradeService);
    verify(answerMapper, never()).insert(any(AiReviewAnswerEntity.class));
  }
}
