package com.guoyu.platform.exam.reviewsync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.exam.reviewsync.dto.ReviewQuestionDetail;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewAnswerEntity;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewPlanDayEntity;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewPlanEntity;
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
 * 主观题作答链路的拦截落库与「查看解析」通道（拦截方案 §4.3）：
 * rule/skip 落库 grade_source、giveUp 不调 LLM、拦截后可重答、回放回显来源。
 * AI 判分服务为桩（闸口本身的零调用断言在 AiGradeApplicationServiceGuardTest）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewPlanSubjectiveGuardTest {

  private static final String REFERENCE =
      "方向是低碳、零排放、循环经济，实施路径含节能、环保技术及循环经济举措";

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

  @BeforeEach
  void setUp() {
    service = new ReviewPlanApplicationService(planMapper, dayMapper, answerMapper, wrongMapper,
        mock(com.guoyu.platform.exam.reviewsync.mapper.AiReviewInterruptLogMapper.class),
        mockPaperMapper, mockItemMapper, grayWhitelistMapper, reviewQuery, examResultWriteRepository,
        llmAssistService, dictMapper, new ObjectMapper(), aiGradeService, aiGradeMockExecutor);
    service.setSelf(service);

    AiReviewPlanEntity plan = new AiReviewPlanEntity();
    plan.setId(100L);
    plan.setTenantId(10159L);
    when(planMapper.selectOne(any())).thenReturn(plan);

    AiReviewPlanDayEntity day = new AiReviewPlanDayEntity();
    day.setId(500L);
    day.setPlanId(100L);
    day.setStatus("pending");
    when(dayMapper.selectById(500L)).thenReturn(day);

    when(reviewQuery.loadQuestionDetail(902L)).thenReturn(new ReviewQuestionDetail(
        902L, 7L, "essay", 1, new BigDecimal("1"), "题干：论述绿色制造", REFERENCE, "解析"));
    when(reviewQuery.isUserInExam(anyLong(), anyLong())).thenReturn(true);
    when(dictMapper.selectList(any())).thenReturn(List.of());
    when(answerMapper.selectOne(any())).thenReturn(null);
    when(answerMapper.selectList(any())).thenReturn(List.of());
    when(answerMapper.selectCount(any())).thenReturn(0L);
  }

  private static SubjectiveGradeResult ruleResult() {
    return new SubjectiveGradeResult(true, 0, false,
        "系统未识别到有效作答，本题记 0 分。想看答案可以点下方「查看解析」；愿意动手可以重新作答。",
        "rule");
  }

  @Test
  void ruleResultPersistsWithGradeSource() {
    when(aiGradeService.grade(any(), any(), any(), anyString(), anyString(), anyString(), any(),
        anyInt())).thenReturn(ruleResult());

    Map<String, Object> view = service.answer(42L, 440L, 500L, 902L, "new", "2", 5, 10159L);

    assertThat(view.get("correct")).isEqualTo(0);
    assertThat(view.get("gradeSource")).isEqualTo("rule");
    assertThat(view.get("correctAnswer")).isEqualTo(REFERENCE);

    ArgumentCaptor<AiReviewAnswerEntity> captor = ArgumentCaptor.forClass(AiReviewAnswerEntity.class);
    verify(answerMapper).insert(captor.capture());
    assertThat(captor.getValue().getGradeSource()).isEqualTo("rule");
    assertThat(captor.getValue().getIsCorrect()).isEqualTo(0);
    assertThat(captor.getValue().getEarnedScore()).isNull();
    // rule 判 0 同样直写 exam_result（与 AI 判 0 同口径）
    verify(examResultWriteRepository).insertExamResult(
        "essay", 440L, 902L, 42L, "2", BigDecimal.ZERO);
  }

  @Test
  void giveUpPersistsSkipWithoutLlm() {
    Map<String, Object> view =
        service.answer(42L, 440L, 500L, 902L, "new", "", 5, 10159L, null, null, 1);

    verifyNoInteractions(aiGradeService);
    assertThat(view.get("correct")).isEqualTo(0);
    assertThat(view.get("gradeSource")).isEqualTo("skip");
    assertThat(view.get("correctAnswer")).isEqualTo(REFERENCE);

    ArgumentCaptor<AiReviewAnswerEntity> captor = ArgumentCaptor.forClass(AiReviewAnswerEntity.class);
    verify(answerMapper).insert(captor.capture());
    assertThat(captor.getValue().getGradeSource()).isEqualTo("skip");
    assertThat(captor.getValue().getIsCorrect()).isEqualTo(0);
  }

  @Test
  void reAnswerAfterRuleRegrades() {
    when(aiGradeService.grade(any(), any(), any(), anyString(), anyString(), anyString(), any(),
        anyInt())).thenReturn(ruleResult(),
        new SubjectiveGradeResult(true, 1, true, "覆盖要点", "ai"));

    service.answer(42L, 440L, 500L, 902L, "new", "2", 5, 10159L);
    Map<String, Object> second =
        service.answer(42L, 440L, 500L, 902L, "new", "低碳零排放循环经济", 5, 10159L);

    // 不同答案不受幂等回放阻挡，重新走 AI 判分
    verify(aiGradeService, times(2)).grade(any(), any(), any(), anyString(), anyString(),
        anyString(), any(), anyInt());
    assertThat(second.get("gradeSource")).isEqualTo("ai");
    assertThat(second.get("correct")).isEqualTo(1);
    verify(answerMapper, times(2)).insert(any(AiReviewAnswerEntity.class));
  }

  @Test
  void replayEchoesGradeSourceFromColumn() {
    service.answer(42L, 440L, 500L, 902L, "new", "", 5, 10159L, null, null, 1);
    ArgumentCaptor<AiReviewAnswerEntity> captor = ArgumentCaptor.forClass(AiReviewAnswerEntity.class);
    verify(answerMapper).insert(captor.capture());
    AiReviewAnswerEntity persisted = captor.getValue();

    // 再次进入（刷新页面）：幂等回放从 grade_source 列回显，skip 不丢
    when(answerMapper.selectOne(any())).thenReturn(persisted);
    Map<String, Object> replay = service.answer(42L, 440L, 500L, 902L, "new", "", 5, 10159L);
    assertThat(replay.get("gradeSource")).isEqualTo("skip");
    assertThat(replay.get("correctAnswer")).isEqualTo(REFERENCE);

    // 存量 NULL 行（V20261021_004 之前未落库）回放按 'ai' 默认
    persisted.setGradeSource(null);
    Map<String, Object> legacyReplay = service.answer(42L, 440L, 500L, 902L, "new", "", 5, 10159L);
    assertThat(legacyReplay.get("gradeSource")).isEqualTo("ai");
  }

  @Test
  void aiResultPersistsEarnedScoreAndGradeSource() {
    when(aiGradeService.grade(any(), any(), any(), anyString(), anyString(), anyString(), any(),
        anyInt())).thenReturn(new SubjectiveGradeResult(true, 1, true, "好", "ai"));

    service.answer(42L, 440L, 500L, 902L, "new", "低碳零排放循环经济", 5, 10159L);

    ArgumentCaptor<AiReviewAnswerEntity> captor = ArgumentCaptor.forClass(AiReviewAnswerEntity.class);
    verify(answerMapper).insert(captor.capture());
    assertThat(captor.getValue().getGradeSource()).isEqualTo("ai");
    assertThat(captor.getValue().getEarnedScore()).isEqualByComparingTo("1");
  }
}
