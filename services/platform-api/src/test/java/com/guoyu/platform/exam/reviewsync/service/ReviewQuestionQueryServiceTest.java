package com.guoyu.platform.exam.reviewsync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.exam.entity.ExamCourseEntity;
import com.guoyu.platform.exam.entity.ExamExamDetailEntity;
import com.guoyu.platform.exam.entity.ExamKnowledgeRateDetailEntity;
import com.guoyu.platform.exam.entity.ExamKnowledgeRateEntity;
import com.guoyu.platform.exam.entity.ExamKnowledgeStructureEntity;
import com.guoyu.platform.exam.mapper.ExamCourseMapper;
import com.guoyu.platform.exam.mapper.ExamExamDetailMapper;
import com.guoyu.platform.exam.mapper.ExamKnowledgeRateDetailMapper;
import com.guoyu.platform.exam.mapper.ExamKnowledgeRateMapper;
import com.guoyu.platform.exam.mapper.ExamKnowledgeStructureMapper;
import com.guoyu.platform.exam.reviewsync.dto.ReviewCourseWeights;
import com.guoyu.platform.exam.reviewsync.dto.ReviewExamPlanInfo;
import com.guoyu.platform.exam.reviewsync.dto.ReviewQuestion;
import com.guoyu.platform.exam.reviewsync.dto.ReviewQuestionDetail;
import com.guoyu.platform.trainingdepartment.entity.TrainExamListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainExamWorkTypeListEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamPeopleListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainExamWorkTypeListMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * 主数据源查询层（单库化后替代 PhpExamQueryService）的取题链路：
 * 计划绑定 → findCourseId 兜底 → 三级知识点 → 题目全集 → 权重 type=3/0/无 三态。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewQuestionQueryServiceTest {

  @Mock
  private TrainExamListMapper trainExamListMapper;
  @Mock
  private TrainExamWorkTypeListMapper workTypeListMapper;
  @Mock
  private TrainExamPeopleListMapper peopleListMapper;
  @Mock
  private ExamCourseMapper examCourseMapper;
  @Mock
  private ExamKnowledgeStructureMapper knowledgeStructureMapper;
  @Mock
  private ExamKnowledgeRateMapper knowledgeRateMapper;
  @Mock
  private ExamKnowledgeRateDetailMapper knowledgeRateDetailMapper;
  @Mock
  private ExamExamDetailMapper examDetailMapper;

  private ReviewQuestionQueryService service;

  /** 纯 Mockito 环境下 LambdaQueryWrapper.select(SFunction) 是即时解析，需要手工初始化实体 TableInfo */
  @BeforeAll
  static void initTableInfos() {
    MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
    TableInfoHelper.initTableInfo(assistant, ExamCourseEntity.class);
    TableInfoHelper.initTableInfo(assistant, ExamKnowledgeStructureEntity.class);
    TableInfoHelper.initTableInfo(assistant, ExamKnowledgeRateEntity.class);
    TableInfoHelper.initTableInfo(assistant, ExamExamDetailEntity.class);
  }

  @BeforeEach
  void setUp() {
    service = new ReviewQuestionQueryService(trainExamListMapper, workTypeListMapper, peopleListMapper,
        examCourseMapper, knowledgeStructureMapper, knowledgeRateMapper, knowledgeRateDetailMapper,
        examDetailMapper);
  }

  private static TrainExamListEntity plan(long id) {
    TrainExamListEntity e = new TrainExamListEntity();
    e.setId(id);
    e.setTitle("电工三级考前培训");
    e.setSource(1);
    e.setExamDate("2026-09-01");
    return e;
  }

  private static TrainExamWorkTypeListEntity binding(long workTypeId, Long courseId, String level) {
    TrainExamWorkTypeListEntity e = new TrainExamWorkTypeListEntity();
    e.setWorkTypeId(workTypeId);
    e.setCourseId(courseId);
    e.setLevel(level);
    return e;
  }

  // ------------------------------------------------------------------
  // loadExamPlanInfo
  // ------------------------------------------------------------------

  @Test
  void loadExamPlanInfoResolvesBindingsWithCourseFallback() {
    when(trainExamListMapper.selectById(440L)).thenReturn(plan(440L));
    // 第一条绑定自带 courseId；第二条 courseId 为空，走 findCourseId 按（工种, 等级）补查
    when(workTypeListMapper.selectList(any())).thenReturn(List.of(
        binding(10L, 55L, "3"),
        binding(11L, null, "2")));
    ExamCourseEntity course = new ExamCourseEntity();
    course.setId(77);
    when(examCourseMapper.selectList(any())).thenReturn(List.of(course));

    ReviewExamPlanInfo info = service.loadExamPlanInfo(440L);

    assertThat(info).isNotNull();
    assertThat(info.id()).isEqualTo(440L);
    assertThat(info.source()).isEqualTo(1);
    assertThat(info.bindings()).hasSize(2);
    assertThat(info.bindings().get(0).workTypeId()).isEqualTo(10L);
    assertThat(info.bindings().get(0).level()).isEqualTo(3);
    assertThat(info.bindings().get(0).courseId()).isEqualTo(55L);
    assertThat(info.bindings().get(1).workTypeId()).isEqualTo(11L);
    assertThat(info.bindings().get(1).level()).isEqualTo(2);
    // findCourseId 兜底命中
    assertThat(info.bindings().get(1).courseId()).isEqualTo(77L);
  }

  @Test
  void loadExamPlanInfoReturnsNullWhenPlanMissing() {
    when(trainExamListMapper.selectById(999L)).thenReturn(null);
    assertThat(service.loadExamPlanInfo(999L)).isNull();
  }

  @Test
  void loadExamPlanInfoSkipsCourseFallbackWhenLevelBlank() {
    when(trainExamListMapper.selectById(440L)).thenReturn(plan(440L));
    // 合并库 level 为 tinyint，偶发空串/非数字——容错解析为 null 后不补查科目
    when(workTypeListMapper.selectList(any())).thenReturn(List.of(binding(10L, null, "")));

    ReviewExamPlanInfo info = service.loadExamPlanInfo(440L);

    assertThat(info.bindings()).hasSize(1);
    assertThat(info.bindings().get(0).level()).isNull();
    assertThat(info.bindings().get(0).courseId()).isNull();
    verify(examCourseMapper, never()).selectList(any());
  }

  // ------------------------------------------------------------------
  // 三级知识点与题目全集
  // ------------------------------------------------------------------

  @Test
  void loadL3KnowledgeIdsReturnsIds() {
    ExamKnowledgeStructureEntity n1 = new ExamKnowledgeStructureEntity();
    n1.setId(101);
    ExamKnowledgeStructureEntity n2 = new ExamKnowledgeStructureEntity();
    n2.setId(102);
    when(knowledgeStructureMapper.selectList(any())).thenReturn(List.of(n1, n2));

    List<Long> ids = service.loadL3KnowledgeIds(List.of(55L));

    assertThat(ids).containsExactly(101L, 102L);
  }

  @Test
  void loadL3KnowledgeIdsEmptyInputSkipsQuery() {
    assertThat(service.loadL3KnowledgeIds(List.of())).isEmpty();
    verify(knowledgeStructureMapper, never()).selectList(any());
  }

  @Test
  void loadQuestionScopeMapsRows() {
    ExamExamDetailEntity row = new ExamExamDetailEntity();
    row.setId(9001);
    row.setKnowledgeId(100);
    row.setType("single");
    row.setDifficulty(2);
    row.setScore(new BigDecimal("2.5"));
    when(examDetailMapper.selectList(any())).thenReturn(List.of(row));

    List<ReviewQuestion> questions = service.loadQuestionScope(1, List.of(100L));

    assertThat(questions).hasSize(1);
    assertThat(questions.get(0).id()).isEqualTo(9001L);
    assertThat(questions.get(0).knowledgeId()).isEqualTo(100L);
    assertThat(questions.get(0).type()).isEqualTo("single");
    assertThat(questions.get(0).score()).isEqualByComparingTo("2.5");
  }

  @Test
  void loadQuestionScopeEmptyKnowledgeIdsSkipsQuery() {
    assertThat(service.loadQuestionScope(1, List.of())).isEmpty();
    verify(examDetailMapper, never()).selectList(any());
  }

  /** 选题池硬过滤（方案 §3.4）：SQL 只出客观三型 + 主观题 8 白名单，空 type/未知题型不入池 */
  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void loadQuestionScopeFiltersToWhitelistTypes() {
    when(examDetailMapper.selectList(any())).thenReturn(List.of());

    service.loadQuestionScope(1, List.of(100L));

    ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
    verify(examDetailMapper).selectList(captor.capture());
    assertThat(captor.getValue().getSqlSegment()).contains("type IN");
    assertThat(captor.getValue().getParamNameValuePairs().values())
        .contains("single", "multi", "judge", "answer", "cal", "essay", "analysis", "explain",
            "fill", "short", "drafting");
  }

  // ------------------------------------------------------------------
  // 题目详情读穿缓存
  // ------------------------------------------------------------------

  @Test
  void loadQuestionDetailCachesSecondCall() {
    ExamExamDetailEntity row = new ExamExamDetailEntity();
    row.setId(9001);
    row.setKnowledgeId(100);
    row.setType("single");
    row.setContent("题干");
    row.setAnswer("[{\"content\":\"对\",\"isCorrect\":true}]");
    row.setResolve("解析");
    when(examDetailMapper.selectOne(any())).thenReturn(row);

    ReviewQuestionDetail first = service.loadQuestionDetail(9001L);
    ReviewQuestionDetail second = service.loadQuestionDetail(9001L);

    assertThat(first).isNotNull();
    assertThat(first.content()).isEqualTo("题干");
    assertThat(second).isSameAs(first);
    // 第二次命中 Caffeine 缓存，不再回源
    verify(examDetailMapper, times(1)).selectOne(any());
  }

  // ------------------------------------------------------------------
  // 权重：type=3 国标 → type=0 人工 → 空
  // ------------------------------------------------------------------

  private static ExamKnowledgeRateEntity rate(int id) {
    ExamKnowledgeRateEntity e = new ExamKnowledgeRateEntity();
    e.setId(id);
    return e;
  }

  private static ExamKnowledgeRateDetailEntity rateDetail(int knowledgeId, int rateValue) {
    ExamKnowledgeRateDetailEntity e = new ExamKnowledgeRateDetailEntity();
    e.setKnowledgeRateId(5);
    e.setKnowledgeId(knowledgeId);
    e.setRate(rateValue);
    e.setSortNumber(1);
    return e;
  }

  private static ExamKnowledgeStructureEntity titledNode(int id, String title) {
    ExamKnowledgeStructureEntity e = new ExamKnowledgeStructureEntity();
    e.setId(id);
    e.setTitle(title);
    return e;
  }

  @Test
  void loadCourseWeightsPrefersStandardAndPicksLatestWhenDuplicated() {
    // type=3 存在 2 条配置：警告并取 id 最大者（LIMIT 1 已由 wrapper 表达，此处验证多条不阻断）
    when(knowledgeRateMapper.selectCount(any())).thenReturn(2L);
    when(knowledgeRateMapper.selectList(any())).thenReturn(List.of(rate(5)));
    when(knowledgeRateDetailMapper.selectList(any())).thenReturn(List.of(
        rateDetail(100, 30),
        rateDetail(101, 0))); // rate<=0 被过滤
    when(knowledgeStructureMapper.selectList(any())).thenReturn(List.of(titledNode(100, "节点A")));

    ReviewCourseWeights weights = service.loadCourseWeights(55L);

    assertThat(weights.sourceType()).isEqualTo("standard");
    assertThat(weights.items()).hasSize(1);
    assertThat(weights.items().get(0).knowledgeId()).isEqualTo(100L);
    assertThat(weights.items().get(0).title()).isEqualTo("节点A");
    assertThat(weights.items().get(0).rate()).isEqualTo(30);
  }

  @Test
  void loadCourseWeightsFallsBackToCustomWhenNoStandard() {
    when(knowledgeRateMapper.selectCount(any())).thenReturn(0L, 1L);
    when(knowledgeRateMapper.selectList(any()))
        .thenReturn(List.of(), List.of(rate(9)));
    when(knowledgeRateDetailMapper.selectList(any())).thenReturn(List.of(rateDetail(101, 50)));
    when(knowledgeStructureMapper.selectList(any())).thenReturn(List.of(titledNode(101, "人工节点")));

    ReviewCourseWeights weights = service.loadCourseWeights(55L);

    assertThat(weights.sourceType()).isEqualTo("custom");
    assertThat(weights.items()).hasSize(1);
    assertThat(weights.items().get(0).knowledgeId()).isEqualTo(101L);
  }

  @Test
  void loadCourseWeightsEmptyWhenNeitherConfigured() {
    when(knowledgeRateMapper.selectCount(any())).thenReturn(0L, 0L);
    when(knowledgeRateMapper.selectList(any())).thenReturn(List.of(), List.of());

    ReviewCourseWeights weights = service.loadCourseWeights(55L);

    assertThat(weights.sourceType()).isNull();
    assertThat(weights.isEmpty()).isTrue();
  }
}
