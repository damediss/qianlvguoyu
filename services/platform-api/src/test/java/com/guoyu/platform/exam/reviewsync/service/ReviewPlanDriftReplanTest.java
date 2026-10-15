package com.guoyu.platform.exam.reviewsync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

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
import java.util.ArrayList;
import java.util.List;
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
 * 考试计划中途变更的入口软重排（方案 §5.8）：
 * overview/today/advanceDay 入口重读考试计划头部（日期+来源），漂移即重排——
 * 日期变化重算天数/阶段；来源变化重建必做题池；未开始的今日及未来日任务重置。
 * 必做题池之外的学员进度（作答/错题本/已完成的天）一律不动。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewPlanDriftReplanTest {

  private static final long PLAN_ID = 100L;
  private static final long EXAM_ID = 440L;
  private static final long USER_ID = 42L;
  private static final long COURSE_ID = 7L;

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
        mock(com.guoyu.platform.exam.reviewsync.mapper.AiReviewInterruptLogMapper.class),
        mockPaperMapper, mockItemMapper, grayWhitelistMapper, reviewQuery, examResultWriteRepository,
        llmAssistService, dictMapper, new ObjectMapper(), aiGradeService, aiGradeMockExecutor);
    // 纯单测无 Spring 代理：self 注入自身
    service.setSelf(service);
  }

  /** 建计划时的旧口径：考试日在 66 天后（阶段 33/20/13）、source=0，学员停在第 1 天 */
  private static AiReviewPlanEntity planAtDay(int currentDayIndex) {
    AiReviewPlanEntity plan = new AiReviewPlanEntity();
    plan.setId(PLAN_ID);
    plan.setPlatformUserId(USER_ID);
    plan.setExamId(EXAM_ID);
    plan.setTenantId(10159L);
    plan.setCourseId(COURSE_ID);
    plan.setSource(0);
    plan.setExamDate(LocalDate.now().plusDays(66));
    plan.setStartDate(LocalDate.now());
    plan.setTotalDays(66);
    plan.setStage1Days(33);
    plan.setStage2Days(20);
    plan.setStage3Days(13);
    plan.setCurrentStage(1);
    plan.setCurrentDayIndex(currentDayIndex);
    plan.setDailyNewCap(120);
    plan.setDailyTotalCap(200);
    plan.setTotalQuestions(950);
    plan.setTargetQuestions(924);
    plan.setMustDoIds("[900,901]");
    plan.setWeightSnapshot("{}");
    plan.setStatus("active");
    return plan;
  }

  private static AiReviewPlanDayEntity unstartedDay(int dayIndex, LocalDate planDate) {
    AiReviewPlanDayEntity day = new AiReviewPlanDayEntity();
    day.setId(500L + dayIndex);
    day.setPlanId(PLAN_ID);
    day.setDayIndex(dayIndex);
    day.setPlanDate(planDate);
    day.setStatus("pending");
    day.setNewDone(0);
    day.setReviewDone(0);
    return day;
  }

  private static ReviewQuestion question(long id) {
    return new ReviewQuestion(id, 70L, "single", 1, new BigDecimal("2"));
  }

  /** 仅考试日期漂移（source 与计划快照一致） */
  private void stubDateDriftTo(int daysFromNow) {
    when(reviewQuery.loadExamPlanHead(EXAM_ID)).thenReturn(new ReviewExamPlanInfo.ExamPlanHead(
        LocalDate.now().plusDays(daysFromNow).toString(), null, 0));
  }

  /** 仅来源漂移（考试日期与计划快照一致），并搭好重取题池的桩 */
  private void stubSourceDriftTo(int source, List<ReviewQuestion> pool) {
    when(reviewQuery.loadExamPlanHead(EXAM_ID)).thenReturn(new ReviewExamPlanInfo.ExamPlanHead(
        LocalDate.now().plusDays(66).toString(), null, source));
    when(reviewQuery.loadL3KnowledgeIds(any())).thenReturn(List.of(70L));
    when(reviewQuery.loadQuestionScope(eq(source), any())).thenReturn(pool);
    when(reviewQuery.loadCourseWeights(COURSE_ID))
        .thenReturn(new ReviewCourseWeights("standard", List.of()));
    when(reviewQuery.loadKnowledgeTree(COURSE_ID)).thenReturn(List.of());
  }

  // ------------------------------------------------------------------
  // 考试日期漂移
  // ------------------------------------------------------------------

  @Test
  void examMovedEarlierResetsUnstartedDaysAndKeepsFields() {
    AiReviewPlanEntity planView = planAtDay(1);
    AiReviewPlanEntity locked = planAtDay(1);
    when(planMapper.selectByIdForUpdate(PLAN_ID)).thenReturn(locked);
    // 今天未开始 + 提前物化的明天（超前学习）都未开始 → 重置
    when(dayMapper.selectUnstartedFromForUpdate(eq(PLAN_ID), any()))
        .thenReturn(List.of(unstartedDay(1, LocalDate.now()),
            unstartedDay(2, LocalDate.now().plusDays(1))));
    stubDateDriftTo(25);

    AiReviewPlanEntity result = service.replanIfExamPlanDrifted(planView);

    // 25 天重切：round(12.5)=13 / round(7.5)=8 / 尾差 4
    assertThat(result.getExamDate()).isEqualTo(LocalDate.now().plusDays(25));
    assertThat(result.getTotalDays()).isEqualTo(25);
    assertThat(result.getStage1Days()).isEqualTo(13);
    assertThat(result.getStage2Days()).isEqualTo(8);
    assertThat(result.getStage3Days()).isEqualTo(4);
    assertThat(result.getCurrentStage()).isEqualTo(1);
    // 进度字段不动
    assertThat(result.getMustDoIds()).isEqualTo("[900,901]");
    assertThat(result.getCurrentDayIndex()).isEqualTo(1);
    verify(dayMapper).deleteUnstartedFrom(PLAN_ID, LocalDate.now());
    verify(planMapper).updateById(locked);
    verify(llmAssistService).evictEncouragement(PLAN_ID, List.of(1, 2));
  }

  @Test
  void examMovedLaterGrowsDaysAndReDerivesStage() {
    // 学员已进行到第 14 天（旧口径下处于阶段 2）
    AiReviewPlanEntity planView = planAtDay(14);
    AiReviewPlanEntity locked = planAtDay(14);
    when(planMapper.selectByIdForUpdate(PLAN_ID)).thenReturn(locked);
    when(dayMapper.selectUnstartedFromForUpdate(eq(PLAN_ID), any())).thenReturn(List.of());
    stubDateDriftTo(80);

    AiReviewPlanEntity result = service.replanIfExamPlanDrifted(planView);

    // 80 天重切 40/24/16；第 14 天按新边界回到阶段 1（剩余新题继续按天均摊）
    assertThat(result.getTotalDays()).isEqualTo(80);
    assertThat(result.getStage1Days()).isEqualTo(40);
    assertThat(result.getStage2Days()).isEqualTo(24);
    assertThat(result.getStage3Days()).isEqualTo(16);
    assertThat(result.getCurrentStage()).isEqualTo(1);
    verify(dayMapper).deleteUnstartedFrom(PLAN_ID, LocalDate.now());
    verify(llmAssistService).evictEncouragement(eq(PLAN_ID), anyList());
  }

  @Test
  void totalDaysNeverShrinksBelowCurrentDayIndex() {
    // 考试日提前到只剩 2 天，但学员已进行到第 10 天：保底按已进行天数收尾
    AiReviewPlanEntity planView = planAtDay(10);
    AiReviewPlanEntity locked = planAtDay(10);
    when(planMapper.selectByIdForUpdate(PLAN_ID)).thenReturn(locked);
    when(dayMapper.selectUnstartedFromForUpdate(eq(PLAN_ID), any())).thenReturn(List.of());
    stubDateDriftTo(2);

    AiReviewPlanEntity result = service.replanIfExamPlanDrifted(planView);

    // totalDays=10 重切 5/3/2；第 10 天 > 5+3 → 冲刺期
    assertThat(result.getTotalDays()).isEqualTo(10);
    assertThat(result.getCurrentDayIndex()).isEqualTo(10);
    assertThat(result.getCurrentStage()).isEqualTo(3);
  }

  @Test
  void noDriftSkipsEverything() {
    AiReviewPlanEntity planView = planAtDay(1);
    when(reviewQuery.loadExamPlanHead(EXAM_ID)).thenReturn(new ReviewExamPlanInfo.ExamPlanHead(
        planView.getExamDate().toString(), null, 0));

    AiReviewPlanEntity result = service.replanIfExamPlanDrifted(planView);

    assertThat(result).isSameAs(planView);
    verify(planMapper, never()).selectByIdForUpdate(anyLong());
    verify(dayMapper, never()).deleteUnstartedFrom(anyLong(), any());
    verify(planMapper, never()).updateById(any(AiReviewPlanEntity.class));
  }

  @Test
  void examDatePassedKeepsPlan() {
    AiReviewPlanEntity planView = planAtDay(1);
    when(reviewQuery.loadExamPlanHead(EXAM_ID)).thenReturn(new ReviewExamPlanInfo.ExamPlanHead(
        LocalDate.now().minusDays(3).toString(), null, 0));

    AiReviewPlanEntity result = service.replanIfExamPlanDrifted(planView);

    assertThat(result).isSameAs(planView);
    verify(planMapper, never()).selectByIdForUpdate(anyLong());
    verify(planMapper, never()).updateById(any(AiReviewPlanEntity.class));
  }

  @Test
  void examPlanDeletedKeepsPlan() {
    AiReviewPlanEntity planView = planAtDay(1);
    when(reviewQuery.loadExamPlanHead(EXAM_ID)).thenReturn(null);

    AiReviewPlanEntity result = service.replanIfExamPlanDrifted(planView);

    assertThat(result).isSameAs(planView);
    verify(planMapper, never()).updateById(any(AiReviewPlanEntity.class));
  }

  @Test
  void concurrentEntryAlreadyReplanReturnsFreshRow() {
    AiReviewPlanEntity planView = planAtDay(1);
    // 锁内当前读：并发请求已完成重排（考试日期已是新值）→ 不再写，返回最新行
    AiReviewPlanEntity replanned = planAtDay(1);
    replanned.setExamDate(LocalDate.now().plusDays(25));
    replanned.setTotalDays(25);
    when(planMapper.selectByIdForUpdate(PLAN_ID)).thenReturn(replanned);
    stubDateDriftTo(25);

    AiReviewPlanEntity result = service.replanIfExamPlanDrifted(planView);

    assertThat(result).isSameAs(replanned);
    verify(dayMapper, never()).deleteUnstartedFrom(anyLong(), any());
    verify(planMapper, never()).updateById(any(AiReviewPlanEntity.class));
  }

  @Test
  void finishedPlanSkipsDriftCheck() {
    AiReviewPlanEntity planView = planAtDay(1);
    planView.setStatus("completed");

    AiReviewPlanEntity result = service.replanIfExamPlanDrifted(planView);

    assertThat(result).isSameAs(planView);
    verify(reviewQuery, never()).loadExamPlanHead(anyLong());
  }

  // ------------------------------------------------------------------
  // 题目来源（source）漂移
  // ------------------------------------------------------------------

  @Test
  void sourceChangedRebuildsPoolAndResetsUnstartedDays() {
    AiReviewPlanEntity planView = planAtDay(1);
    AiReviewPlanEntity locked = planAtDay(1);
    when(planMapper.selectByIdForUpdate(PLAN_ID)).thenReturn(locked);
    when(dayMapper.selectUnstartedFromForUpdate(eq(PLAN_ID), any()))
        .thenReturn(List.of(unstartedDay(1, LocalDate.now())));
    stubSourceDriftTo(1, List.of(question(910L), question(911L)));

    AiReviewPlanEntity result = service.replanIfExamPlanDrifted(planView);

    // 题池快照按新来源刷新；2 题 < 阶段1容量(33×120) → 不裁剪
    assertThat(result.getSource()).isEqualTo(1);
    assertThat(result.getTotalQuestions()).isEqualTo(2);
    assertThat(result.getTargetQuestions()).isEqualTo(2);
    assertThat(result.getTrimmed()).isZero();
    assertThat(result.getMustDoIds()).contains("910", "911");
    // 日期维不动
    assertThat(result.getTotalDays()).isEqualTo(66);
    assertThat(result.getExamDate()).isEqualTo(LocalDate.now().plusDays(66));
    // 未开始日任务重置 + 鼓励语失效
    verify(dayMapper).deleteUnstartedFrom(PLAN_ID, LocalDate.now());
    verify(llmAssistService).evictEncouragement(PLAN_ID, List.of(1));
    verify(planMapper).updateById(locked);
  }

  @Test
  void sourceChangedTrimsToStage1Capacity() {
    AiReviewPlanEntity planView = planAtDay(1);
    AiReviewPlanEntity locked = planAtDay(1);
    locked.setStage1Days(1);
    when(planMapper.selectByIdForUpdate(PLAN_ID)).thenReturn(locked);
    when(dayMapper.selectUnstartedFromForUpdate(eq(PLAN_ID), any())).thenReturn(List.of());
    List<ReviewQuestion> pool = new ArrayList<>();
    for (long id = 1000; id < 1130; id++) {
      pool.add(question(id));
    }
    stubSourceDriftTo(2, pool);

    AiReviewPlanEntity result = service.replanIfExamPlanDrifted(planView);

    // 130 题 > 阶段1容量(1×120) → 裁剪到 120
    assertThat(result.getSource()).isEqualTo(2);
    assertThat(result.getTotalQuestions()).isEqualTo(130);
    assertThat(result.getTargetQuestions()).isEqualTo(120);
    assertThat(result.getTrimmed()).isEqualTo(1);
    verify(planMapper).updateById(locked);
  }

  @Test
  void sourceChangedButNewPoolEmptyKeepsPlanUntouched() {
    AiReviewPlanEntity planView = planAtDay(1);
    AiReviewPlanEntity locked = planAtDay(1);
    when(planMapper.selectByIdForUpdate(PLAN_ID)).thenReturn(locked);
    stubSourceDriftTo(1, List.of());

    AiReviewPlanEntity result = service.replanIfExamPlanDrifted(planView);

    // 新来源暂无题：原计划保持不动（source 快照不写，下次进入继续检测），日任务也不重置
    assertThat(result).isSameAs(planView);
    assertThat(planView.getSource()).isZero();
    verify(dayMapper, never()).selectUnstartedFromForUpdate(anyLong(), any());
    verify(dayMapper, never()).deleteUnstartedFrom(anyLong(), any());
    verify(planMapper, never()).updateById(any(AiReviewPlanEntity.class));
  }

  @Test
  void bothDriftsApplyDateAndPool() {
    AiReviewPlanEntity planView = planAtDay(1);
    AiReviewPlanEntity locked = planAtDay(1);
    when(planMapper.selectByIdForUpdate(PLAN_ID)).thenReturn(locked);
    when(dayMapper.selectUnstartedFromForUpdate(eq(PLAN_ID), any())).thenReturn(List.of());
    // 日期提前到 25 天 + 换来源；2 题 < 新阶段1容量(13×120)
    when(reviewQuery.loadExamPlanHead(EXAM_ID)).thenReturn(new ReviewExamPlanInfo.ExamPlanHead(
        LocalDate.now().plusDays(25).toString(), null, 1));
    when(reviewQuery.loadL3KnowledgeIds(any())).thenReturn(List.of(70L));
    when(reviewQuery.loadQuestionScope(eq(1), any()))
        .thenReturn(List.of(question(910L), question(911L)));
    when(reviewQuery.loadCourseWeights(COURSE_ID))
        .thenReturn(new ReviewCourseWeights("standard", List.of()));
    when(reviewQuery.loadKnowledgeTree(COURSE_ID)).thenReturn(List.of());

    AiReviewPlanEntity result = service.replanIfExamPlanDrifted(planView);

    assertThat(result.getExamDate()).isEqualTo(LocalDate.now().plusDays(25));
    assertThat(result.getTotalDays()).isEqualTo(25);
    assertThat(result.getStage1Days()).isEqualTo(13);
    assertThat(result.getSource()).isEqualTo(1);
    assertThat(result.getTotalQuestions()).isEqualTo(2);
    assertThat(result.getTargetQuestions()).isEqualTo(2);
    verify(dayMapper).deleteUnstartedFrom(PLAN_ID, LocalDate.now());
    verify(planMapper).updateById(locked);
  }
}
