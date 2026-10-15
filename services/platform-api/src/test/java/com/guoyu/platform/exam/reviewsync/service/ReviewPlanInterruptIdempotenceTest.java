package com.guoyu.platform.exam.reviewsync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.guoyu.platform.system.entity.DictEntity;
import com.guoyu.platform.system.mapper.DictMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 中断补救当日幂等（生产实证：学员回归后 50 秒内 overview/today 重复触发 case=D 9 次，
 * 每次重复执行 washWrongItems 清零连对、interruptCount 虚涨 3→11）。
 * 修复：触发补救时把 lastActiveDate 推进到当天，同日内不再命中 gap≥2。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewPlanInterruptIdempotenceTest {

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

  @BeforeAll
  static void initMpTableInfo() {
    MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
    TableInfoHelper.initTableInfo(assistant, DictEntity.class);
    TableInfoHelper.initTableInfo(assistant, AiReviewPlanEntity.class);
    TableInfoHelper.initTableInfo(assistant, AiReviewPlanDayEntity.class);
    TableInfoHelper.initTableInfo(assistant, AiReviewAnswerEntity.class);
    TableInfoHelper.initTableInfo(assistant, AiReviewWrongItemEntity.class);
    TableInfoHelper.initTableInfo(assistant, AiReviewMockPaperEntity.class);
  }

  @BeforeEach
  void setUp() {
    service = new ReviewPlanApplicationService(planMapper, dayMapper, answerMapper, wrongMapper,
        mock(com.guoyu.platform.exam.reviewsync.mapper.AiReviewInterruptLogMapper.class),
        mockPaperMapper, mockItemMapper, grayWhitelistMapper, reviewQuery, examResultWriteRepository,
        llmAssistService, dictMapper, new ObjectMapper(), aiGradeService, aiGradeMockExecutor);
    service.setSelf(service);

    when(dictMapper.selectList(any())).thenReturn(List.of());
    when(reviewQuery.isUserInExam(anyLong(), anyLong())).thenReturn(true);
    when(reviewQuery.loadExamPlanHead(anyLong())).thenReturn(null);
    // 可变列表：activeWrongItems 等路径会对结果 sort（List.of 不可变会炸）
    when(wrongMapper.selectList(any())).thenReturn(new java.util.ArrayList<>());
    when(answerMapper.selectCount(any())).thenReturn(0L);
    when(answerMapper.selectList(any())).thenReturn(new java.util.ArrayList<>());
  }

  /** 中断 5 天、阶段 3 的活跃计划；当日任务行已物化（materializeDay 快路径直接返回）。 */
  private static AiReviewPlanEntity interruptedPlan() {
    AiReviewPlanEntity plan = new AiReviewPlanEntity();
    plan.setId(100L);
    plan.setTenantId(10159L);
    plan.setPlatformUserId(1430L);
    plan.setExamId(498L);
    plan.setStatus("active");
    plan.setCurrentStage(3);
    plan.setCurrentDayIndex(1);
    plan.setTotalDays(20);
    plan.setStage1Days(10);
    plan.setStage2Days(6);
    plan.setStage3Days(4);
    plan.setMustDoIds("[]");
    plan.setLastActiveDate(LocalDate.now().minusDays(5));
    plan.setInterruptCount(0);
    return plan;
  }

  private static AiReviewPlanDayEntity existingDay(long planId, int dayIndex) {
    AiReviewPlanDayEntity day = new AiReviewPlanDayEntity();
    day.setId(500L);
    day.setPlanId(planId);
    day.setDayIndex(dayIndex);
    day.setPlanDate(LocalDate.now());
    day.setStatus("in_progress");
    day.setNewQuestionIds("[]");
    day.setReviewQuestionIds("[]");
    return day;
  }

  @Test
  @DisplayName("中断回归当日首次 overview 触发补救一次，同日再次 overview 不再重复触发")
  void interruptHandledOncePerDay() {
    AiReviewPlanEntity plan = interruptedPlan();
    when(planMapper.selectOne(any())).thenReturn(plan);
    when(dayMapper.selectOne(any())).thenReturn(existingDay(100L, 1));

    Map<String, Object> first = service.overview(1430L, 498L, 10159L);
    assertThat(first.get("interrupt")).isNotNull();
    assertThat(plan.getInterruptCount()).isEqualTo(1);
    assertThat(plan.getLastActiveDate()).isEqualTo(LocalDate.now());

    Map<String, Object> second = service.overview(1430L, 498L, 10159L);
    assertThat(second.get("interrupt")).isNull();
    assertThat(plan.getInterruptCount()).isEqualTo(1);
  }
}
