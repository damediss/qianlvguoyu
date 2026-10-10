package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.guoyu.platform.competition.dto.CompetitionMakeExamPlanUpsertReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPaperQuestionEntity;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPaperSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPlanEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalQuestionDetailEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalQuestionScoreItemEntity;
import com.guoyu.platform.competition.entity.CompetitionQuestionBlueprintBindingEntity;
import com.guoyu.platform.competition.entity.CompetitionQuestionBlueprintNodeEntity;
import com.guoyu.platform.competition.entity.CompetitionQuestionEntity;
import com.guoyu.platform.competition.mapper.CompetitionEventMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeLevelMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
import com.guoyu.platform.competition.mapper.CompetitionMakeExamPaperQuestionMapper;
import com.guoyu.platform.competition.mapper.CompetitionMakeExamPaperSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionMakeExamPlanDetailMapper;
import com.guoyu.platform.competition.mapper.CompetitionMakeExamPlanMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalQuestionDetailMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalQuestionScoreItemMapper;
import com.guoyu.platform.competition.mapper.CompetitionQuestionBlueprintBindingMapper;
import com.guoyu.platform.competition.mapper.CompetitionQuestionBlueprintNodeMapper;
import com.guoyu.platform.competition.mapper.CompetitionQuestionMapper;
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

class CompetitionPracticalMakeExamPlanServiceTest {

  private final CompetitionMakeExamPlanMapper planMapper = mock(CompetitionMakeExamPlanMapper.class);
  private final CompetitionMakeExamPlanDetailMapper detailMapper = mock(CompetitionMakeExamPlanDetailMapper.class);
  private final CompetitionMakeExamPaperSnapshotMapper snapshotMapper = mock(CompetitionMakeExamPaperSnapshotMapper.class);
  private final CompetitionMakeExamPaperQuestionMapper snapshotQuestionMapper = mock(CompetitionMakeExamPaperQuestionMapper.class);
  private final CompetitionQuestionMapper questionMapper = mock(CompetitionQuestionMapper.class);
  private final CompetitionPracticalQuestionDetailMapper practicalDetailMapper = mock(CompetitionPracticalQuestionDetailMapper.class);
  private final CompetitionPracticalQuestionScoreItemMapper scoreItemMapper = mock(CompetitionPracticalQuestionScoreItemMapper.class);
  private final CompetitionEventMapper eventMapper = mock(CompetitionEventMapper.class);
  private final CompetitionEventWorkTypeMapper workTypeMapper = mock(CompetitionEventWorkTypeMapper.class);
  private final CompetitionEventWorkTypeLevelMapper levelMapper = mock(CompetitionEventWorkTypeLevelMapper.class);
  private final CompetitionQuestionBlueprintApplicationService blueprintService = mock(CompetitionQuestionBlueprintApplicationService.class);
  private final CompetitionQuestionBlueprintNodeMapper blueprintNodeMapper = mock(CompetitionQuestionBlueprintNodeMapper.class);
  private final CompetitionQuestionBlueprintBindingMapper blueprintBindingMapper = mock(CompetitionQuestionBlueprintBindingMapper.class);
  private final CompetitionMakeExamPlanApplicationService service = new CompetitionMakeExamPlanApplicationService(
      planMapper, detailMapper, snapshotMapper, snapshotQuestionMapper, questionMapper,
      practicalDetailMapper, scoreItemMapper, null, eventMapper, workTypeMapper, levelMapper,
      blueprintService, blueprintNodeMapper, blueprintBindingMapper, null);

  @BeforeEach
  void setUp() {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        9L, 1L, "admin", "admin", "business", List.of("PLATFORM_ADMIN"),
        List.of("competition:make-plan:view", "competition:make-plan:create"), "admin@example.com");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
    when(eventMapper.selectOne(any())).thenReturn(event());
    when(workTypeMapper.selectOne(any())).thenReturn(workType());
    when(levelMapper.selectOne(any())).thenReturn(level());
    when(planMapper.insert(any(CompetitionMakeExamPlanEntity.class))).thenReturn(1);
    when(snapshotMapper.insert(any(CompetitionMakeExamPaperSnapshotEntity.class))).thenReturn(1);
    when(snapshotQuestionMapper.insert(any(CompetitionMakeExamPaperQuestionEntity.class))).thenReturn(1);
    when(detailMapper.selectByPlan(any(), any())).thenReturn(List.of());
    when(blueprintNodeMapper.selectByBlueprint(1L, 9901L)).thenReturn(List.of(
        node(99011L, 0L, "范围", BigDecimal.ONE, null, 1),
        node(99012L, 99011L, "必考结构", BigDecimal.ONE, "必考", 1),
        node(99013L, 99012L, "考评点", null, null, 1)));
    when(blueprintBindingMapper.selectByBlueprint(1L, 9901L)).thenReturn(List.of(
        binding(99021L, 9901L, 99013L), binding(99022L, 9901L, 99013L)));
    when(questionMapper.selectList(any())).thenReturn(List.of(question(99021L), question(99022L)));
    when(practicalDetailMapper.selectByQuestion(1L, 99021L)).thenReturn(detail(99021L));
    when(practicalDetailMapper.selectByQuestion(1L, 99022L)).thenReturn(detail(99022L));
    when(scoreItemMapper.selectByQuestion(any(), any())).thenReturn(List.of(scoreItem()));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void generatesOneSnapshotPerRequestedPaperAndKeepsEachPaperQuestionUnique() {
    var result = service.create(1001L, new CompetitionMakeExamPlanUpsertReq(
        3001L, 3101L, "practical", "实操自动组卷", "实操", 60,
        new BigDecimal("100"), new BigDecimal("60"), null, List.of(), null,
        9901L, null, 2, 12345L));

    assertThat(result.paperCount()).isEqualTo(2);
    assertThat(result.randomSeed()).isEqualTo("12345");
    ArgumentCaptor<CompetitionMakeExamPaperSnapshotEntity> snapshots =
        ArgumentCaptor.forClass(CompetitionMakeExamPaperSnapshotEntity.class);
    Mockito.verify(snapshotMapper, Mockito.times(2)).insert(snapshots.capture());
    assertThat(snapshots.getAllValues()).extracting(CompetitionMakeExamPaperSnapshotEntity::getPaperCode)
        .containsExactly("P1", "P2");
    ArgumentCaptor<CompetitionMakeExamPaperQuestionEntity> questions =
        ArgumentCaptor.forClass(CompetitionMakeExamPaperQuestionEntity.class);
    Mockito.verify(snapshotQuestionMapper, Mockito.times(2)).insert(questions.capture());
    assertThat(questions.getAllValues()).extracting(CompetitionMakeExamPaperQuestionEntity::getSourceQuestionId)
        .containsExactlyInAnyOrder(99021L, 99022L);
    assertThat(questions.getAllValues()).allSatisfy(question -> {
      assertThat(question.getScore()).isEqualByComparingTo("100.00");
      assertThat(question.getSourceTotalScore()).isEqualByComparingTo("100");
      assertThat(question.getScoreScale()).isEqualByComparingTo("1.00000000");
    });
  }

  private CompetitionQuestionEntity question(Long id) {
    CompetitionQuestionEntity question = new CompetitionQuestionEntity();
    question.setId(id);
    question.setEventId(1001L);
    question.setEventWorkTypeId(3001L);
    question.setEventWorkTypeLevelId(3101L);
    question.setContentType("practical");
    question.setQuestionType("practical");
    question.setSortOrder(id.intValue());
    question.setStem("实操题" + id);
    question.setStatus("enabled");
    question.setDeleted(0);
    return question;
  }

  private CompetitionPracticalQuestionDetailEntity detail(Long questionId) {
    CompetitionPracticalQuestionDetailEntity detail = new CompetitionPracticalQuestionDetailEntity();
    detail.setQuestionId(questionId);
    detail.setDurationMinutes(60);
    detail.setTotalScore(new BigDecimal("100"));
    detail.setScoringMethod("deductive");
    return detail;
  }

  private CompetitionPracticalQuestionScoreItemEntity scoreItem() {
    CompetitionPracticalQuestionScoreItemEntity item = new CompetitionPracticalQuestionScoreItemEntity();
    item.setItemNo(1);
    item.setAllocationEnabled(1);
    item.setMaxScore(new BigDecimal("100"));
    item.setMaxDeduction(new BigDecimal("100"));
    item.setDeductionStep(new BigDecimal("1"));
    return item;
  }

  private CompetitionQuestionBlueprintNodeEntity node(Long id, Long parentId, String name,
      BigDecimal weight, String selectMode, int sortOrder) {
    CompetitionQuestionBlueprintNodeEntity node = new CompetitionQuestionBlueprintNodeEntity();
    node.setId(id);
    node.setParentId(parentId);
    node.setName(name);
    node.setWeight(weight);
    node.setSelectMode(selectMode);
    node.setNodeLevel(sortOrder == 1 ? (parentId == 0L ? 1 : 2) : 4);
    node.setCode(String.valueOf(id));
    node.setFullCode(String.valueOf(id));
    node.setSortOrder(sortOrder);
    return node;
  }

  private CompetitionQuestionBlueprintBindingEntity binding(Long questionId, Long blueprintId, Long nodeId) {
    CompetitionQuestionBlueprintBindingEntity binding = new CompetitionQuestionBlueprintBindingEntity();
    binding.setQuestionId(questionId);
    binding.setBlueprintId(blueprintId);
    binding.setBlueprintNodeId(nodeId);
    binding.setDeleted(0);
    return binding;
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
