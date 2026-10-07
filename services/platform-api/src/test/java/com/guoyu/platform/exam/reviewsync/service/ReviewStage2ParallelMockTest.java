package com.guoyu.platform.exam.reviewsync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewMockPaperEntity;
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
import com.guoyu.platform.system.entity.DictEntity;
import com.guoyu.platform.system.mapper.DictMapper;
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
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 阶段 2（定向爆破）起开放并行动模考：今日视图的 mock 字段在阶段≥2 合成 available 标记
 * （App 首页据此显示「进入仿真模考」，进入后 mockStart 现场组卷，不限次、不标记当日完成）。
 * 开关 sys_dict ai_review_mock_stage2_enabled，缺省开、置 0 安静关闭。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewStage2ParallelMockTest {

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
  }

  @BeforeEach
  void setUp() {
    service = new ReviewPlanApplicationService(planMapper, dayMapper, answerMapper, wrongMapper,
        mockPaperMapper, mockItemMapper, grayWhitelistMapper, reviewQuery, examResultWriteRepository,
        llmAssistService, dictMapper, new ObjectMapper(), aiGradeService, aiGradeMockExecutor);
    service.setSelf(service);
  }

  private AiReviewPlanEntity plan() {
    AiReviewPlanEntity plan = new AiReviewPlanEntity();
    plan.setId(100L);
    plan.setTenantId(10159L);
    plan.setTotalDays(9);
    plan.setStage1Days(5);
    plan.setStage2Days(3);
    plan.setStage3Days(1);
    plan.setCurrentStage(2);
    plan.setCurrentDayIndex(8);
    plan.setStatus("active");
    return plan;
  }

  private AiReviewPlanDayEntity day(int stage, Long mockPaperId) {
    AiReviewPlanDayEntity day = new AiReviewPlanDayEntity();
    day.setId(500L);
    day.setPlanId(100L);
    day.setDayIndex(8);
    day.setStage(stage);
    day.setMockPaperId(mockPaperId);
    day.setStatus("in_progress");
    return day;
  }

  private void dictValue(String value) {
    if (value == null) {
      when(dictMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
      return;
    }
    DictEntity dict = new DictEntity();
    dict.setCode("ai_review_mock_stage2_enabled");
    dict.setValue(value);
    when(dictMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(dict));
  }

  @Test
  void stage2DayWithoutPaperGetsParallelMockAvailable() {
    dictValue(null); // 未配置字典 → 缺省开放
    Map<String, Object> view = service.mockViewWithStage2Parallel(plan(), day(2, null));
    assertThat(view).isNotNull();
    assertThat(view.get("available")).isEqualTo(true);
    assertThat(view).doesNotContainKey("mockPaperId"); // 无预生成卷，进入模考页后现场组卷
  }

  @Test
  void stage1DayHasNoMockEntry() {
    dictValue(null);
    assertThat(service.mockViewWithStage2Parallel(plan(), day(1, null))).isNull();
  }

  @Test
  void nullStageDayHasNoMockEntry() {
    dictValue(null);
    AiReviewPlanDayEntity legacyDay = day(2, null);
    legacyDay.setStage(null); // 历史行 stage 为空的防御
    assertThat(service.mockViewWithStage2Parallel(plan(), legacyDay)).isNull();
  }

  @Test
  void dictSwitchDisabledFallsBackQuietly() {
    dictValue("0");
    assertThat(service.mockViewWithStage2Parallel(plan(), day(2, null))).isNull();
  }

  @Test
  void dictMalformedValueFallsBackToEnabled() {
    dictValue("abc"); // dictInt 解析失败按缺省值 1（开放）
    Map<String, Object> view = service.mockViewWithStage2Parallel(plan(), day(2, null));
    assertThat(view).isNotNull();
    assertThat(view.get("available")).isEqualTo(true);
  }

  @Test
  void stage3RealPaperWinsOverParallelFlag() {
    dictValue(null);
    AiReviewMockPaperEntity paper = new AiReviewMockPaperEntity();
    paper.setId(900L);
    paper.setPlanId(100L);
    paper.setQuestionCount(100);
    paper.setStatus("ready");
    when(mockPaperMapper.selectById(900L)).thenReturn(paper);

    Map<String, Object> view = service.mockViewWithStage2Parallel(plan(), day(3, 900L));
    assertThat(view).isNotNull();
    assertThat(view.get("mockPaperId")).isEqualTo("900");
    assertThat(view.get("questionCount")).isEqualTo(100);
    assertThat(view.get("status")).isEqualTo("ready");
    assertThat(view).doesNotContainKey("available"); // 真实卷视图，不再合成并行标记
  }
}
