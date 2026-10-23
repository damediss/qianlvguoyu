package com.guoyu.platform.exam.reviewsync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.exam.mapper.ExamExamDetailMapper;
import com.guoyu.platform.exam.reviewsync.entity.AiReviewPlanEntity;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewAnswerMapper;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewMockPaperMapper;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewPlanMapper;
import com.guoyu.platform.exam.reviewsync.mapper.AiReviewWrongItemMapper;
import com.guoyu.platform.exam.reviewsync.mapper.ReviewSyncStatsMapper;
import com.guoyu.platform.trainingdepartment.entity.PlatformUserEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainExamListEntity;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserMapper;
import com.guoyu.platform.trainingdepartment.entity.TrainExamPeopleListEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamPeopleListMapper;
import java.sql.Date;
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

/**
 * 学员明细"最近活跃"展示口径（方案1）：输出真实最后作答日（作答流水 MAX(DATE(created_at))），
 * 不是 plan.lastActiveDate——后者语义为"任务覆盖到哪天"（D7 §15.1-②），超前完成时是未来日期，
 * 直接下发会出现"明天活跃"的穿越展示。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewSyncStatsLastActiveDisplayTest {

  @Mock ReviewSyncStatsMapper statsMapper;
  @Mock AiReviewPlanMapper planMapper;
  @Mock AiReviewWrongItemMapper wrongMapper;
  @Mock AiReviewMockPaperMapper mockPaperMapper;
  @Mock AiReviewAnswerMapper answerMapper;
  @Mock ExamExamDetailMapper examDetailMapper;
  @Mock PlatformUserMapper platformUserMapper;
  @Mock TrainExamListMapper trainExamListMapper;
  @Mock TrainExamPeopleListMapper examPeopleListMapper;
  @Mock ReviewQuestionQueryService reviewQuery;

  ReviewSyncStatsApplicationService service;

  @BeforeAll
  static void initTableInfo() {
    // 分页/包装器需要实体的 TableInfo（与既有 reviewsync 服务测试同款前置）
    MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
    assistant.setCurrentNamespace(AiReviewPlanEntity.class.getName());
    TableInfoHelper.initTableInfo(assistant, AiReviewPlanEntity.class);
  }

  @BeforeEach
  void setUp() {
    service = new ReviewSyncStatsApplicationService(statsMapper, planMapper, wrongMapper,
        mockPaperMapper, answerMapper, examDetailMapper, platformUserMapper, trainExamListMapper,
        examPeopleListMapper, reviewQuery, new ObjectMapper());
  }

  @Test
  void detailShowsRealAnswerDateInsteadOfFuturePlanCoverageDate() {
    AiReviewPlanEntity plan = new AiReviewPlanEntity();
    plan.setId(1L);
    plan.setPlatformUserId(100L);
    plan.setExamId(200L);
    plan.setCurrentStage(2);
    plan.setCurrentDayIndex(6);
    plan.setTotalDays(10);
    plan.setStatus("active");
    // 内部口径：超前完成 → 任务覆盖到明天
    plan.setLastActiveDate(LocalDate.now().plusDays(1));

    Page<AiReviewPlanEntity> page = new Page<>(1, 20);
    page.setRecords(List.of(plan));
    when(planMapper.selectPage(any(), any())).thenReturn(page);
    when(statsMapper.planAnswerAgg(anyList())).thenReturn(List.of(Map.of(
        "planId", 1L, "answers", 569L, "corrects", 510L,
        "newAnswers", 300L, "reviewAnswers", 200L, "mockAnswers", 69L,
        // 真实最后作答日=今天
        "lastAnswerDate", Date.valueOf(LocalDate.now()))));
    when(statsMapper.planWrongAgg(anyList())).thenReturn(List.of());
    when(platformUserMapper.selectList(any())).thenReturn(List.of());
    TrainExamListEntity exam = new TrainExamListEntity();
    exam.setId(200L);
    exam.setTitle("2026年10月10日百色竞赛电工");
    when(trainExamListMapper.selectList(any())).thenReturn(List.of(exam));

    List<Map<String, Object>> rows = service.students(1, 20, null, null).items();

    assertThat(rows).hasSize(1);
    // 输出真实作答日，而不是 plan 上的未来覆盖日
    assertThat(rows.get(0).get("lastActiveDate")).isEqualTo(LocalDate.now().toString());
  }

  @Test
  void blankProfileNameFallsBackToExamRosterByPhone() {
    // 生产实证（2026-10-23）：App 注册学员 platform_user.realname 为空串，统计页学员列空白；
    // 考生名册（考试计划导入）有姓名 → 按手机号回退
    AiReviewPlanEntity plan = new AiReviewPlanEntity();
    plan.setId(3L);
    plan.setPlatformUserId(102L);
    plan.setExamId(200L);
    plan.setStatus("active");

    Page<AiReviewPlanEntity> page = new Page<>(1, 20);
    page.setRecords(List.of(plan));
    when(planMapper.selectPage(any(), any())).thenReturn(page);
    when(statsMapper.planAnswerAgg(anyList())).thenReturn(List.of());
    when(statsMapper.planWrongAgg(anyList())).thenReturn(List.of());
    when(trainExamListMapper.selectList(any())).thenReturn(List.of());

    PlatformUserEntity user = new PlatformUserEntity();
    user.setId(102L);
    user.setRealname(""); // 空串（非 null）——原实现直接透传导致列空白
    user.setPhone("15277075096");
    when(platformUserMapper.selectList(any())).thenReturn(List.of(user));

    TrainExamPeopleListEntity roster = new TrainExamPeopleListEntity();
    roster.setId(17405L);
    roster.setExamId(200L);
    roster.setRealname("覃金冠");
    roster.setPhone("15277075096");
    when(examPeopleListMapper.selectList(any())).thenReturn(List.of(roster));

    List<Map<String, Object>> rows = service.students(1, 20, null, null).items();

    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).get("realname")).isEqualTo("覃金冠");
  }

  @Test
  void blankProfileNameWithoutRosterUsesFallbackLabel() {
    AiReviewPlanEntity plan = new AiReviewPlanEntity();
    plan.setId(4L);
    plan.setPlatformUserId(103L);
    plan.setExamId(200L);
    plan.setStatus("active");

    Page<AiReviewPlanEntity> page = new Page<>(1, 20);
    page.setRecords(List.of(plan));
    when(planMapper.selectPage(any(), any())).thenReturn(page);
    when(statsMapper.planAnswerAgg(anyList())).thenReturn(List.of());
    when(statsMapper.planWrongAgg(anyList())).thenReturn(List.of());
    when(trainExamListMapper.selectList(any())).thenReturn(List.of());

    PlatformUserEntity user = new PlatformUserEntity();
    user.setId(103L);
    user.setRealname("  ");
    user.setPhone("13000000000");
    when(platformUserMapper.selectList(any())).thenReturn(List.of(user));
    when(examPeopleListMapper.selectList(any())).thenReturn(List.of());

    List<Map<String, Object>> rows = service.students(1, 20, null, null).items();

    assertThat(rows.get(0).get("realname")).isEqualTo("学员#103");
  }

  @Test
  void planWithoutAnswerFlowShowsNullLastActive() {
    AiReviewPlanEntity plan = new AiReviewPlanEntity();
    plan.setId(2L);
    plan.setPlatformUserId(101L);
    plan.setExamId(200L);
    plan.setStatus("active");
    plan.setLastActiveDate(LocalDate.now().plusDays(1));

    Page<AiReviewPlanEntity> page = new Page<>(1, 20);
    page.setRecords(List.of(plan));
    when(planMapper.selectPage(any(), any())).thenReturn(page);
    // 无作答流水：聚合无行 → 最近活跃为 null，前端展示 '-'
    when(statsMapper.planAnswerAgg(anyList())).thenReturn(List.of());
    when(statsMapper.planWrongAgg(anyList())).thenReturn(List.of());
    when(platformUserMapper.selectList(any())).thenReturn(List.of());
    when(trainExamListMapper.selectList(any())).thenReturn(List.of());

    List<Map<String, Object>> rows = service.students(1, 20, null, null).items();

    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).get("lastActiveDate")).isNull();
  }
}
