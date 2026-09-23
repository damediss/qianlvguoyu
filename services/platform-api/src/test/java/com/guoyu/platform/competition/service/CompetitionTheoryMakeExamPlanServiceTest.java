package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.competition.dto.CompetitionMakeExamPlanDetailReq;
import com.guoyu.platform.competition.dto.CompetitionMakeExamPlanTopicRuleReq;
import com.guoyu.platform.competition.dto.CompetitionMakeExamPlanUpsertReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPaperQuestionEntity;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPaperSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPlanDetailEntity;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPlanEntity;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPlanTopicRuleEntity;
import com.guoyu.platform.competition.entity.CompetitionQuestionBlueprintBindingEntity;
import com.guoyu.platform.competition.entity.CompetitionQuestionBlueprintEntity;
import com.guoyu.platform.competition.entity.CompetitionQuestionBlueprintNodeEntity;
import com.guoyu.platform.competition.entity.CompetitionQuestionEntity;
import com.guoyu.platform.competition.mapper.CompetitionEventMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeLevelMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
import com.guoyu.platform.competition.mapper.CompetitionMakeExamPaperQuestionMapper;
import com.guoyu.platform.competition.mapper.CompetitionMakeExamPaperSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionMakeExamPlanDetailMapper;
import com.guoyu.platform.competition.mapper.CompetitionMakeExamPlanMapper;
import com.guoyu.platform.competition.mapper.CompetitionMakeExamPlanTopicRuleMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalQuestionDetailMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalQuestionScoreItemMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalQuestionAssessmentContentMapper;
import com.guoyu.platform.competition.mapper.CompetitionQuestionBlueprintBindingMapper;
import com.guoyu.platform.competition.mapper.CompetitionQuestionBlueprintNodeMapper;
import com.guoyu.platform.competition.mapper.CompetitionQuestionMapper;
import com.guoyu.platform.competition.entity.CompetitionPracticalQuestionDetailEntity;
import com.guoyu.platform.competition.service.CompetitionQuestionBlueprintApplicationService;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CompetitionTheoryMakeExamPlanServiceTest {

  private final CompetitionMakeExamPlanMapper planMapper = Mockito.mock(CompetitionMakeExamPlanMapper.class);
  private final CompetitionMakeExamPlanDetailMapper detailMapper = Mockito.mock(CompetitionMakeExamPlanDetailMapper.class);
  private final CompetitionMakeExamPaperSnapshotMapper snapshotMapper = Mockito.mock(CompetitionMakeExamPaperSnapshotMapper.class);
  private final CompetitionMakeExamPaperQuestionMapper snapshotQuestionMapper = Mockito.mock(CompetitionMakeExamPaperQuestionMapper.class);
  private final CompetitionQuestionMapper questionMapper = Mockito.mock(CompetitionQuestionMapper.class);
  private final CompetitionPracticalQuestionDetailMapper practicalDetailMapper = Mockito.mock(CompetitionPracticalQuestionDetailMapper.class);
  private final CompetitionPracticalQuestionScoreItemMapper scoreItemMapper = Mockito.mock(CompetitionPracticalQuestionScoreItemMapper.class);
  private final CompetitionPracticalQuestionAssessmentContentMapper assessmentContentMapper = Mockito.mock(CompetitionPracticalQuestionAssessmentContentMapper.class);
  private final CompetitionEventMapper eventMapper = Mockito.mock(CompetitionEventMapper.class);
  private final CompetitionEventWorkTypeMapper workTypeMapper = Mockito.mock(CompetitionEventWorkTypeMapper.class);
  private final CompetitionEventWorkTypeLevelMapper levelMapper = Mockito.mock(CompetitionEventWorkTypeLevelMapper.class);
  private final CompetitionQuestionBlueprintApplicationService blueprintService = Mockito.mock(CompetitionQuestionBlueprintApplicationService.class);
  private final CompetitionQuestionBlueprintNodeMapper blueprintNodeMapper = Mockito.mock(CompetitionQuestionBlueprintNodeMapper.class);
  private final CompetitionQuestionBlueprintBindingMapper bindingMapper = Mockito.mock(CompetitionQuestionBlueprintBindingMapper.class);
  private final CompetitionMakeExamPlanTopicRuleMapper topicRuleMapper = Mockito.mock(CompetitionMakeExamPlanTopicRuleMapper.class);

  private final CompetitionMakeExamPlanApplicationService service =
      new CompetitionMakeExamPlanApplicationService(
          planMapper, detailMapper, snapshotMapper, snapshotQuestionMapper, questionMapper,
          practicalDetailMapper, scoreItemMapper, assessmentContentMapper, eventMapper,
          workTypeMapper, levelMapper, blueprintService, blueprintNodeMapper, bindingMapper,
          topicRuleMapper);

  @BeforeEach
  void setUp() throws Exception {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        9L, 1L, "admin", "admin", "business", List.of("PLATFORM_ADMIN"),
        List.of("competition:make-plan:view", "competition:make-plan:create"), "admin@example.com");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
    when(eventMapper.selectOne(any())).thenReturn(event());
    when(workTypeMapper.selectOne(any())).thenReturn(workType());
    when(levelMapper.selectOne(any())).thenReturn(level());
    when(blueprintService.requireActive(any(), eq(700L), any(), any(), any(), eq("theory")))
        .thenReturn(blueprint());
    when(blueprintService.findForEvent(any(), any(), eq(700L))).thenReturn(blueprint());
    when(blueprintNodeMapper.selectByBlueprint(1L, 700L)).thenReturn(nodes());
    when(bindingMapper.selectByBlueprint(1L, 700L)).thenReturn(bindings());
    when(blueprintService.listEffectiveBindings(anyLong(), anyLong())).thenReturn(null);
    when(questionMapper.selectList(any())).thenReturn(List.of(question(101L), question(102L)));
    when(planMapper.insert(any(CompetitionMakeExamPlanEntity.class))).thenReturn(1);
    when(detailMapper.insert(any(CompetitionMakeExamPlanDetailEntity.class))).thenReturn(1);
    when(detailMapper.selectByPlan(any(), any())).thenReturn(List.of(detailEntity()));
    when(snapshotMapper.insert(any(CompetitionMakeExamPaperSnapshotEntity.class))).thenReturn(1);
    when(snapshotQuestionMapper.insert(any(CompetitionMakeExamPaperQuestionEntity.class))).thenReturn(1);
    when(topicRuleMapper.insert(any(CompetitionMakeExamPlanTopicRuleEntity.class))).thenReturn(1);
    when(topicRuleMapper.selectByPlan(any(), any())).thenReturn(List.of());
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void precheckAndFormalGenerationUseBlueprintWeightsAndIgnoreLegacyTheoryRules() {
    CompetitionMakeExamPlanUpsertReq request = requestWithLegacyTopicRule();

    var precheck = service.precheck(1001L, request);

    assertThat(precheck.ready()).isTrue();
    assertThat(precheck.theoryAllocations()).hasSize(1);
    assertThat(precheck.theoryAllocations().get(0).questionCount()).isEqualTo(2);
    assertThat(precheck.theoryAllocations().get(0).assigned()).isEqualTo(2);
    assertThat(precheck.theoryAllocations().get(0).details().get(0).required()).isEqualTo(2);

    var result = service.create(1001L, request);

    assertThat(result.status()).isEqualTo("draft");
    ArgumentCaptor<CompetitionMakeExamPaperSnapshotEntity> snapshotCaptor =
        ArgumentCaptor.forClass(CompetitionMakeExamPaperSnapshotEntity.class);
    verify(snapshotMapper).insert(snapshotCaptor.capture());
    CompetitionMakeExamPaperSnapshotEntity snapshot = snapshotCaptor.getValue();
    assertThat(snapshot.getRandomSeed()).isNotBlank();
    assertThat(snapshot.getQuestionCount()).isEqualTo(2);
    assertThat(snapshot.getTotalScore()).isEqualByComparingTo("4");

    ArgumentCaptor<CompetitionMakeExamPaperQuestionEntity> paperQuestionCaptor =
        ArgumentCaptor.forClass(CompetitionMakeExamPaperQuestionEntity.class);
    verify(snapshotQuestionMapper, times(2)).insert(paperQuestionCaptor.capture());
    assertThat(paperQuestionCaptor.getAllValues())
        .extracting(CompetitionMakeExamPaperQuestionEntity::getScore)
        .containsOnly(new BigDecimal("2"));

    ArgumentCaptor<CompetitionMakeExamPlanTopicRuleEntity> ruleCaptor =
        ArgumentCaptor.forClass(CompetitionMakeExamPlanTopicRuleEntity.class);
    verify(topicRuleMapper, times(1)).insert(ruleCaptor.capture());
    CompetitionMakeExamPlanTopicRuleEntity rule = ruleCaptor.getValue();
    assertThat(rule.getBlueprintNodeId()).isEqualTo(3L);
    assertThat(rule.getQuestionCount()).isEqualTo(2);
    assertThat(rule.getScore()).isEqualByComparingTo("4");
    assertThat(rule.getSelected()).isEqualTo(1);
    assertThat(rule.getSortOrder()).isEqualTo(1);
  }

  @Test
  void precheckExcludesUnboundWrongBlueprintAndNonLeafBindings() {
    CompetitionQuestionBlueprintBindingEntity wrongBlueprint = binding(103L, 4L);
    wrongBlueprint.setBlueprintId(999L);
    CompetitionQuestionBlueprintBindingEntity nonLeafBinding = binding(104L, 3L);
    List<CompetitionQuestionEntity> scopeQuestions = List.of(
        question(101L), question(102L), question(103L), question(104L), question(105L));
    when(bindingMapper.selectByBlueprint(1L, 700L)).thenReturn(
        List.of(binding(101L, 4L), binding(102L, 5L), wrongBlueprint, nonLeafBinding));
    when(questionMapper.selectList(any())).thenReturn(scopeQuestions, scopeQuestions);

    var precheck = service.precheck(1001L, requestWithLegacyTopicRule());

    assertThat(precheck.ready()).isTrue();
    assertThat(precheck.candidateCount()).isEqualTo(2);
    assertThat(precheck.unboundQuestionCount()).isEqualTo(3);
    assertThat(precheck.warnings()).anyMatch(item -> item.contains("3道题未绑定"));
  }

  @Test
  void precheckUsesEffectiveBindingsInheritedFromArchivedBlueprint() {
    when(bindingMapper.selectByBlueprint(1L, 700L)).thenReturn(List.of());
    when(blueprintService.listEffectiveBindings(1L, 700L)).thenReturn(bindings());
    List<CompetitionQuestionEntity> scopeQuestions = List.of(question(101L), question(102L));
    when(questionMapper.selectList(any())).thenReturn(scopeQuestions, scopeQuestions);

    var precheck = service.precheck(1001L, requestWithLegacyTopicRule());

    assertThat(precheck.ready()).isTrue();
    assertThat(precheck.candidateCount()).isEqualTo(2);
    assertThat(precheck.unboundQuestionCount()).isZero();
  }

  @Test
  void publishedPlanCannotEnterDraftRegenerationPath() {
    CompetitionMakeExamPlanEntity published = new CompetitionMakeExamPlanEntity();
    published.setId(9001L);
    published.setEventId(1001L);
    published.setStatus("published");
    published.setDeleted(0);
    when(planMapper.selectOne(any())).thenReturn(published);

    org.assertj.core.api.Assertions.assertThatThrownBy(() ->
        service.update(1001L, 9001L, requestWithLegacyTopicRule()))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("已发布");
    verify(snapshotMapper, Mockito.never()).insert(any(CompetitionMakeExamPaperSnapshotEntity.class));
  }

  private CompetitionMakeExamPlanUpsertReq requestWithLegacyTopicRule() {
    return new CompetitionMakeExamPlanUpsertReq(
        3001L, 3101L, "theory", "理论自动组卷", "初赛理论", 90,
        new BigDecimal("4"), new BigDecimal("2"), null,
        List.of(new CompetitionMakeExamPlanDetailReq(
            "single", 2, new BigDecimal("2"), null, null, 1)),
        null, 700L,
        List.of(new CompetitionMakeExamPlanTopicRuleReq(999L, 99, new BigDecimal("999"), true, 1)));
  }

  private CompetitionQuestionBlueprintEntity blueprint() {
    CompetitionQuestionBlueprintEntity entity = new CompetitionQuestionBlueprintEntity();
    entity.setId(700L);
    entity.setEventId(1001L);
    entity.setEventWorkTypeId(3001L);
    entity.setEventWorkTypeLevelId(3101L);
    entity.setContentType("theory");
    entity.setStatus("active");
    entity.setDeleted(0);
    return entity;
  }

  private List<CompetitionQuestionBlueprintNodeEntity> nodes() {
    return List.of(node(1L, 0L, 1, "A", "一级范围", null),
        node(2L, 1L, 2, "A-1", "一级考核范围", null),
        node(3L, 2L, 3, "A-1-1", "设备知识", BigDecimal.ONE),
        node(4L, 3L, 4, "A-1-1-1", "末级考评点一", null),
        node(5L, 3L, 4, "A-1-1-2", "末级考评点二", null));
  }

  private CompetitionQuestionBlueprintNodeEntity node(
      Long id, Long parentId, int level, String code, String name, BigDecimal weight) {
    CompetitionQuestionBlueprintNodeEntity node = new CompetitionQuestionBlueprintNodeEntity();
    node.setId(id);
    node.setBlueprintId(700L);
    node.setParentId(parentId);
    node.setNodeLevel(level);
    node.setCode(code);
    node.setFullCode(code);
    node.setName(name);
    node.setWeight(weight);
    node.setSortOrder(id.intValue());
    return node;
  }

  private List<CompetitionQuestionBlueprintBindingEntity> bindings() {
    return List.of(binding(101L, 4L), binding(102L, 5L));
  }

  private CompetitionQuestionBlueprintBindingEntity binding(Long questionId, Long nodeId) {
    CompetitionQuestionBlueprintBindingEntity binding = new CompetitionQuestionBlueprintBindingEntity();
    binding.setQuestionId(questionId);
    binding.setBlueprintId(700L);
    binding.setBlueprintNodeId(nodeId);
    return binding;
  }

  private CompetitionQuestionEntity question(Long id) {
    CompetitionQuestionEntity question = new CompetitionQuestionEntity();
    question.setId(id);
    question.setEventId(1001L);
    question.setEventWorkTypeId(3001L);
    question.setEventWorkTypeLevelId(3101L);
    question.setContentType("theory");
    question.setQuestionType("single");
    question.setDifficulty(2);
    question.setSortOrder(id.intValue());
    question.setStem("理论题干" + id);
    question.setReferenceAnswer("A");
    question.setOptionsJson("{\"A\":\"选项A\"}");
    question.setStatus("enabled");
    question.setDeleted(0);
    return question;
  }

  private CompetitionMakeExamPlanDetailEntity detailEntity() {
    CompetitionMakeExamPlanDetailEntity detail = new CompetitionMakeExamPlanDetailEntity();
    detail.setQuestionType("single");
    detail.setQuestionCount(2);
    detail.setPoint(new BigDecimal("2"));
    detail.setSortOrder(1);
    return detail;
  }

  private CompetitionEventEntity event() {
    CompetitionEventEntity entity = new CompetitionEventEntity();
    entity.setId(1001L);
    entity.setTenantId(1L);
    entity.setDeleted(0);
    return entity;
  }

  private CompetitionEventWorkTypeEntity workType() {
    CompetitionEventWorkTypeEntity entity = new CompetitionEventWorkTypeEntity();
    entity.setId(3001L);
    entity.setEventId(1001L);
    entity.setTenantId(1L);
    entity.setDeleted(0);
    return entity;
  }

  private CompetitionEventWorkTypeLevelEntity level() {
    CompetitionEventWorkTypeLevelEntity entity = new CompetitionEventWorkTypeLevelEntity();
    entity.setId(3101L);
    entity.setEventId(1001L);
    entity.setEventWorkTypeId(3001L);
    entity.setTenantId(1L);
    entity.setDeleted(0);
    return entity;
  }
}
