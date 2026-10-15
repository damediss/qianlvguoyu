package com.guoyu.platform.exam.reviewsync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewAnswerEntity;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewInterruptLogEntity;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewMockPaperEntity;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewPlanDayEntity;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewPlanEntity;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewWrongItemEntity;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewAnswerMapper;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewGrayWhitelistMapper;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewInterruptLogMapper;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewMockPaperItemMapper;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewMockPaperMapper;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewPlanDayMapper;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewPlanMapper;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewWrongItemMapper;
import com.guoyu.platform.exam.reviewsync.mapper.ExamResultWriteRepository;
import com.guoyu.platform.system.entity.DictEntity;
import com.guoyu.platform.system.mapper.DictMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * AI 同步学方法论优化一期（W2）机制单测（方案 §12.1）：
 * 清洗开关默认关/开两态、interrupt_log 落行、参数区间 clamp、mockStart 阶段校验。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewPhase1MechanismTest {

  @Mock
  private AiReviewPlanMapper planMapper;
  @Mock
  private AiReviewPlanDayMapper dayMapper;
  @Mock
  private AiReviewAnswerMapper answerMapper;
  @Mock
  private AiReviewWrongItemMapper wrongMapper;
  @Mock
  private AiReviewInterruptLogMapper interruptLogMapper;
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
    TableInfoHelper.initTableInfo(assistant, AiReviewInterruptLogEntity.class);
  }

  @BeforeEach
  void setUp() {
    service = new ReviewPlanApplicationService(planMapper, dayMapper, answerMapper, wrongMapper,
        interruptLogMapper, mockPaperMapper, mockItemMapper, grayWhitelistMapper, reviewQuery,
        examResultWriteRepository, llmAssistService, dictMapper, new ObjectMapper(), aiGradeService,
        aiGradeMockExecutor);
    service.setSelf(service);
    when(reviewQuery.isUserInExam(anyLong(), anyLong())).thenReturn(true);
    when(reviewQuery.loadExamPlanHead(anyLong())).thenReturn(null);
    when(wrongMapper.selectList(any())).thenReturn(new ArrayList<>());
    when(answerMapper.selectCount(any())).thenReturn(0L);
    when(answerMapper.selectList(any())).thenReturn(new ArrayList<>());
  }

  /** 字典桩：任意键都返回同一行（测试内只关心一个键的取值；灰度门读取同桩需注意取值） */
  private void dictValue(String value) {
    if (value == null) {
      when(dictMapper.selectList(any())).thenReturn(List.of());
      return;
    }
    DictEntity dict = new DictEntity();
    dict.setCode("any");
    dict.setValue(value);
    when(dictMapper.selectList(any())).thenReturn(List.of(dict));
  }

  /**
   * 按 key 精准桩：只应答 ai_review_interrupt_clean_enabled，其余键（含灰度门 ai_review_gray_enabled）
   * 视为未配置——避免"全局一行"桩把灰度门意外打开。
   */
  /** 中断 3 天、阶段 1 的活跃计划（A 案） */
  private static AiReviewPlanEntity interruptedPlanStage1() {
    AiReviewPlanEntity plan = new AiReviewPlanEntity();
    plan.setId(100L);
    plan.setTenantId(10159L);
    plan.setPlatformUserId(1430L);
    plan.setExamId(498L);
    plan.setStatus("active");
    plan.setCurrentStage(1);
    plan.setCurrentDayIndex(1);
    plan.setTotalDays(20);
    plan.setStage1Days(10);
    plan.setStage2Days(6);
    plan.setStage3Days(4);
    plan.setMustDoIds("[]");
    plan.setLastActiveDate(LocalDate.now().minusDays(3));
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
  @DisplayName("W2.1 清洗开关=0（默认）：中断只重排+策略文案，不清零连对，interrupt_log 写一行")
  void interruptCleanDisabledByDefault() {
    service = spy(service);
    doReturn(false).when(service).interruptCleanEnabled(any());
    AiReviewPlanEntity plan = interruptedPlanStage1();
    // 在册红牌连对=3：若清洗被执行会清零并 update
    AiReviewWrongItemEntity item = new AiReviewWrongItemEntity();
    item.setId(9L);
    item.setPlanId(100L);
    item.setQuestionId(1L);
    item.setCardLevel("red");
    item.setConsecutiveCorrect(3);
    item.setRemoved(0);
    when(wrongMapper.selectList(any())).thenReturn(new ArrayList<>(List.of(item)));
    when(planMapper.selectOne(any())).thenReturn(plan);
    when(dayMapper.selectOne(any())).thenReturn(existingDay(100L, 1));

    Map<String, Object> overview = service.overview(1430L, 498L, 10159L);

    assertThat(overview.get("interrupt")).isNotNull();
    String strategyText = (String) ((Map<?, ?>) overview.get("interrupt")).get("strategyText");
    assertThat(strategyText).doesNotContain("部分错题的连对进度需要重新积累");
    verify(wrongMapper, never()).updateById(any(AiReviewWrongItemEntity.class));
    // 日志照常落行（开关无关）
    ArgumentCaptor<AiReviewInterruptLogEntity> logCaptor =
        ArgumentCaptor.forClass(AiReviewInterruptLogEntity.class);
    verify(interruptLogMapper, times(1)).insert(logCaptor.capture());
    assertThat(logCaptor.getValue().getInterruptCase()).isEqualTo("A");
    assertThat(logCaptor.getValue().getGapDays()).isEqualTo(3);
    assertThat(logCaptor.getValue().getStageAtInterrupt()).isEqualTo(1);
  }

  @Test
  @DisplayName("W2.1 清洗开关=1：维持清洗语义且策略文案追加披露句")
  void interruptCleanEnabledWashesAndDiscloses() {
    service = spy(service);
    doReturn(true).when(service).interruptCleanEnabled(any());
    AiReviewPlanEntity plan = interruptedPlanStage1();
    AiReviewWrongItemEntity item = new AiReviewWrongItemEntity();
    item.setId(9L);
    item.setPlanId(100L);
    item.setQuestionId(1L);
    item.setCardLevel("red");
    item.setConsecutiveCorrect(3);
    item.setRemoved(0);
    when(wrongMapper.selectList(any())).thenReturn(new ArrayList<>(List.of(item)));
    when(planMapper.selectOne(any())).thenReturn(plan);
    when(dayMapper.selectOne(any())).thenReturn(existingDay(100L, 1));

    Map<String, Object> overview = service.overview(1430L, 498L, 10159L);

    assertThat(overview.get("interrupt")).isNotNull();
    String strategyText = (String) ((Map<?, ?>) overview.get("interrupt")).get("strategyText");
    assertThat(strategyText).contains("部分错题的连对进度需要重新积累");
    verify(wrongMapper, times(1)).updateById(any(AiReviewWrongItemEntity.class));
  }

  @Test
  @DisplayName("W2.2 参数越界 clamp 到边界（500→300），区间内的值不动")
  void clampToRangeBounds() {
    assertThat(ReviewPlanApplicationService.clampToRange(500, 20, 300)).isEqualTo(300);
    assertThat(ReviewPlanApplicationService.clampToRange(5, 20, 300)).isEqualTo(20);
    assertThat(ReviewPlanApplicationService.clampToRange(120, 20, 300)).isEqualTo(120);
    assertThat(service.dictIntInRange("ai_review_daily_new_cap", 120, 20, 300, 10159L))
        .isEqualTo(120); // 无字典 → 默认且在区间内
  }

  @Test
  @DisplayName("W2.2 参数值为非数字时回默认值（既有 dictInt 解析失败兜底）")
  void dictIntFallsBackToDefaultOnGarbage() {
    dictValue("abc");
    assertThat(service.dictIntInRange("ai_review_daily_new_cap", 120, 20, 300, 10159L))
        .isEqualTo(120);
  }

  @Test
  @DisplayName("W2.3 阶段 1 调 mockStart 被业务异常拦截")
  void mockStartRejectedInStage1() {
    AiReviewPlanEntity plan = interruptedPlanStage1();
    plan.setCurrentStage(1);
    dictValue(null);
    assertThatThrownBy(() -> service.assertMockStartAllowed(plan, 10159L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("当前阶段以新题为主");
  }

  @Test
  @DisplayName("W2.3 阶段 2 需并行动模考开关=1 才放行；阶段 3 直接放行")
  void mockStartStage2GateAndStage3Open() {
    AiReviewPlanEntity plan = interruptedPlanStage1();
    dictValue(null); // 开关缺省=1
    plan.setCurrentStage(2);
    service.assertMockStartAllowed(plan, 10159L); // 不抛

    dictValue("0"); // 开关=0
    assertThatThrownBy(() -> service.assertMockStartAllowed(plan, 10159L))
        .isInstanceOf(BusinessException.class);

    dictValue(null);
    plan.setCurrentStage(3);
    service.assertMockStartAllowed(plan, 10159L); // 不抛
  }
}
