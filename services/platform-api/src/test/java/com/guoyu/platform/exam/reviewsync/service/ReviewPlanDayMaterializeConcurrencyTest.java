package com.guoyu.platform.exam.reviewsync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.exam.reviewsync.dto.ReviewCourseWeights;
import com.guoyu.platform.exam.reviewsync.dto.ReviewExamPlanInfo;
import com.guoyu.platform.exam.reviewsync.dto.ReviewQuestion;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewAnswerEntity;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewMockPaperEntity;
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
import com.guoyu.platform.system.mapper.DictMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DuplicateKeyException;

/**
 * 每日任务物化的并发幂等（线上事故：App 并发调 overview/today，
 * 两个请求同时物化同一天 → uk_ai_review_day_plan_day 重复键 500）。
 *
 * <p>防线：①计划行锁 + 当前读复核（并发只落一行）；②重复键兜底（当前读取回已提交行）；
 * ③计划创建走同一套兜底（uk_ai_review_plan_user_exam）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewPlanDayMaterializeConcurrencyTest {

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

  private ReviewPlanApplicationService service;

  /** 单测无 Spring 扫描，LambdaQueryWrapper 需要手动初始化实体的 TableInfo 缓存 */
  @BeforeAll
  static void initMpTableInfo() {
    MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
    TableInfoHelper.initTableInfo(assistant, AiReviewPlanEntity.class);
    TableInfoHelper.initTableInfo(assistant, AiReviewPlanDayEntity.class);
    TableInfoHelper.initTableInfo(assistant, AiReviewAnswerEntity.class);
    TableInfoHelper.initTableInfo(assistant, AiReviewWrongItemEntity.class);
    TableInfoHelper.initTableInfo(assistant, AiReviewMockPaperEntity.class);
  }

  @BeforeEach
  void setUp() {
    service = new ReviewPlanApplicationService(planMapper, dayMapper, answerMapper, wrongMapper,
        mockPaperMapper, mockItemMapper, grayWhitelistMapper, reviewQuery, examResultWriteRepository,
        llmAssistService, dictMapper, new ObjectMapper());
  }

  private static AiReviewPlanEntity activePlan() {
    AiReviewPlanEntity plan = new AiReviewPlanEntity();
    plan.setId(100L);
    plan.setTenantId(10159L);
    plan.setCourseId(7L);
    plan.setStartDate(LocalDate.of(2026, 9, 1));
    plan.setTotalDays(20);
    plan.setStage1Days(10);
    plan.setStage2Days(6);
    plan.setStage3Days(4);
    plan.setCurrentStage(1);
    plan.setCurrentDayIndex(1);
    plan.setDailyNewCap(120);
    plan.setDailyTotalCap(200);
    plan.setMustDoIds("[]");
    plan.setStatus("active");
    return plan;
  }

  private static AiReviewPlanDayEntity existingDay(long planId, int dayIndex) {
    AiReviewPlanDayEntity day = new AiReviewPlanDayEntity();
    day.setId(500L);
    day.setPlanId(planId);
    day.setDayIndex(dayIndex);
    day.setStatus("pending");
    day.setNewQuestionIds("[]");
    day.setReviewQuestionIds("[]");
    return day;
  }

  @Test
  void materializeReusesRowCommittedByConcurrentRequest() {
    AiReviewPlanEntity plan = activePlan();
    AiReviewPlanDayEntity concurrentRow = existingDay(100L, 1);

    // 快照读看不到（并发事务刚提交），当前读看得到 → 复用，不再插入
    when(dayMapper.selectOne(any())).thenReturn(null);
    when(dayMapper.selectByPlanAndDayForUpdate(100L, 1)).thenReturn(concurrentRow);

    AiReviewPlanDayEntity day = service.materializeDay(plan, 1, LocalDate.of(2026, 9, 1), null);

    assertThat(day).isSameAs(concurrentRow);
    verify(dayMapper, never()).insert(any(AiReviewPlanDayEntity.class));
    verify(planMapper).selectByIdForUpdate(100L);
  }

  @Test
  void materializeFallsBackToCommittedRowWhenInsertHitsUniqueKey() {
    AiReviewPlanEntity plan = activePlan();
    AiReviewPlanDayEntity concurrentRow = existingDay(100L, 1);

    when(dayMapper.selectOne(any())).thenReturn(null);
    // 第一次当前读仍为空（并发事务尚未提交），重复键兜底时第二次看得到
    when(dayMapper.selectByPlanAndDayForUpdate(100L, 1)).thenReturn(null, concurrentRow);
    when(dayMapper.insert(any(AiReviewPlanDayEntity.class))).thenThrow(new DuplicateKeyException(
        "Duplicate entry '100-1' for key 'ai_review_plan_day.uk_ai_review_day_plan_day'"));
    when(reviewQuery.loadKnowledgeTree(anyLong())).thenReturn(List.of());

    AiReviewPlanDayEntity day = service.materializeDay(plan, 1, LocalDate.of(2026, 9, 1), null);

    assertThat(day).isSameAs(concurrentRow);
    verify(dayMapper).insert(any(AiReviewPlanDayEntity.class));
    verify(llmAssistService, never()).generateAsync(anyLong(), anyInt(), anyInt(), any(), any(),
        anyInt(), any(), anyBoolean(), anyLong());
  }

  @Test
  void materializeRethrowsWhenDuplicateIsNotConcurrency() {
    AiReviewPlanEntity plan = activePlan();

    when(dayMapper.selectOne(any())).thenReturn(null);
    when(dayMapper.selectByPlanAndDayForUpdate(100L, 1)).thenReturn(null);
    when(dayMapper.insert(any(AiReviewPlanDayEntity.class))).thenThrow(new DuplicateKeyException("dup"));
    when(reviewQuery.loadKnowledgeTree(anyLong())).thenReturn(List.of());

    assertThatThrownBy(() -> service.materializeDay(plan, 1, LocalDate.of(2026, 9, 1), null))
        .isInstanceOf(DuplicateKeyException.class);
  }

  @Test
  void materializeReturnsExistingDayWithoutLockingWhenAlreadyMaterialized() {
    AiReviewPlanEntity plan = activePlan();
    AiReviewPlanDayEntity existing = existingDay(100L, 1);

    when(dayMapper.selectOne(any())).thenReturn(existing);

    AiReviewPlanDayEntity day = service.materializeDay(plan, 1, LocalDate.of(2026, 9, 1), null);

    assertThat(day).isSameAs(existing);
    verify(dayMapper, never()).insert(any(AiReviewPlanDayEntity.class));
    // 快路径不加锁，避免和 answer() 的「当天行锁 → 计划行锁」形成交叉加锁
    verify(planMapper, never()).selectByIdForUpdate(anyLong());
  }

  @Test
  void overviewReusesPlanWhenConcurrentCreateHitsUniqueKey() {
    AiReviewPlanEntity concurrentPlan = activePlan();

    // 快照读没有计划 → 建计划时撞唯一键 → 当前读取回并发已提交的计划
    when(planMapper.selectOne(any())).thenReturn(null);
    when(reviewQuery.isUserInExam(440L, 42L)).thenReturn(true);
    when(reviewQuery.loadExamPlanInfo(440L)).thenReturn(new ReviewExamPlanInfo(
        440L, "计划", 0, "2026-10-01", null,
        List.of(new ReviewExamPlanInfo.WorkTypeBinding(11L, 2, 7L))));
    when(reviewQuery.loadL3KnowledgeIds(any())).thenReturn(List.of(70L));
    when(reviewQuery.loadQuestionScope(anyInt(), any())).thenReturn(
        List.of(new ReviewQuestion(900L, 70L, "single", 1, new BigDecimal("2"))));
    when(reviewQuery.loadCourseWeights(7L)).thenReturn(new ReviewCourseWeights("standard", List.of()));
    when(reviewQuery.loadKnowledgeTree(7L)).thenReturn(List.of());
    when(planMapper.insert(any(AiReviewPlanEntity.class))).thenThrow(new DuplicateKeyException(
        "Duplicate entry '42-440' for key 'ai_review_plan.uk_ai_review_plan_user_exam'"));
    when(planMapper.selectByUserAndExamForUpdate(42L, 440L)).thenReturn(concurrentPlan);
    AiReviewPlanDayEntity todayRow = existingDay(100L, 1);
    todayRow.setPlanDate(LocalDate.now());
    when(dayMapper.selectOne(any())).thenReturn(todayRow);

    Map<String, Object> result = service.overview(42L, 440L, 10159L);

    assertThat(result.get("planId")).isEqualTo("100");
    assertThat(result.get("dayIndex")).isEqualTo(1);
    verify(planMapper).selectByUserAndExamForUpdate(42L, 440L);
  }
}
