package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.guoyu.platform.competition.dto.CompetitionMakeExamPlanDetailReq;
import com.guoyu.platform.competition.dto.CompetitionMakeExamPlanUpsertReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPaperQuestionEntity;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPaperSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPlanDetailEntity;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPlanEntity;
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
import com.guoyu.platform.competition.mapper.CompetitionQuestionMapper;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CompetitionMakeExamPlanApplicationServiceTest {

  private final CompetitionMakeExamPlanMapper planMapper = Mockito.mock(CompetitionMakeExamPlanMapper.class);
  private final CompetitionMakeExamPlanDetailMapper detailMapper = Mockito.mock(CompetitionMakeExamPlanDetailMapper.class);
  private final CompetitionMakeExamPaperSnapshotMapper snapshotMapper = Mockito.mock(CompetitionMakeExamPaperSnapshotMapper.class);
  private final CompetitionMakeExamPaperQuestionMapper snapshotQuestionMapper = Mockito.mock(CompetitionMakeExamPaperQuestionMapper.class);
  private final CompetitionQuestionMapper questionMapper = Mockito.mock(CompetitionQuestionMapper.class);
  private final CompetitionPracticalQuestionDetailMapper practicalDetailMapper = Mockito.mock(CompetitionPracticalQuestionDetailMapper.class);
  private final CompetitionPracticalQuestionScoreItemMapper scoreItemMapper = Mockito.mock(CompetitionPracticalQuestionScoreItemMapper.class);
  private final CompetitionEventMapper eventMapper = Mockito.mock(CompetitionEventMapper.class);
  private final CompetitionEventWorkTypeMapper workTypeMapper = Mockito.mock(CompetitionEventWorkTypeMapper.class);
  private final CompetitionEventWorkTypeLevelMapper levelMapper = Mockito.mock(CompetitionEventWorkTypeLevelMapper.class);
  private final CompetitionMakeExamPlanApplicationService service = new CompetitionMakeExamPlanApplicationService(
      planMapper, detailMapper, snapshotMapper, snapshotQuestionMapper, questionMapper,
      practicalDetailMapper, scoreItemMapper, eventMapper, workTypeMapper, levelMapper);

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
    when(detailMapper.insert(any(CompetitionMakeExamPlanDetailEntity.class))).thenReturn(1);
    when(snapshotMapper.insert(any(CompetitionMakeExamPaperSnapshotEntity.class))).thenReturn(1);
    when(snapshotQuestionMapper.insert(any(CompetitionMakeExamPaperQuestionEntity.class))).thenReturn(1);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void createsOneIndependentTheorySnapshotWithoutCourseQuestionSources() {
    CompetitionMakeExamPlanDetailEntity detail = new CompetitionMakeExamPlanDetailEntity();
    detail.setPlanId(5001L);
    detail.setQuestionType("single");
    detail.setQuestionCount(2);
    detail.setPoint(new BigDecimal("2"));
    detail.setSortOrder(1);
    when(detailMapper.selectByPlan(eq(1L), any())).thenReturn(List.of(detail));
    when(questionMapper.selectList(any())).thenReturn(List.of(
        question(6101L), question(6102L), question(6103L), question(6104L)));

    var result = service.create(1001L, new CompetitionMakeExamPlanUpsertReq(
        3001L, 3101L, "theory", "竞赛理论计划", "初赛理论", 90,
        new BigDecimal("4"), new BigDecimal("2"), null,
        List.of(new CompetitionMakeExamPlanDetailReq(
            "single", 2, new BigDecimal("2"), null, null, 1))));

    assertThat(result.status()).isEqualTo("draft");
    assertThat(result.paperCount()).isEqualTo(1);
    ArgumentCaptor<CompetitionMakeExamPaperSnapshotEntity> snapshotCaptor =
        ArgumentCaptor.forClass(CompetitionMakeExamPaperSnapshotEntity.class);
    Mockito.verify(snapshotMapper, Mockito.times(1)).insert(snapshotCaptor.capture());
    assertThat(snapshotCaptor.getAllValues()).extracting(CompetitionMakeExamPaperSnapshotEntity::getPaperCode)
        .containsExactly("P");
    assertThat(snapshotCaptor.getAllValues()).allSatisfy(snapshot -> {
      assertThat(snapshot.getContentType()).isEqualTo("theory");
      assertThat(snapshot.getStatus()).isEqualTo("draft");
      assertThat(snapshot.getEventWorkTypeLevelId()).isEqualTo(3101L);
    });

    ArgumentCaptor<CompetitionMakeExamPaperQuestionEntity> questionCaptor =
        ArgumentCaptor.forClass(CompetitionMakeExamPaperQuestionEntity.class);
    Mockito.verify(snapshotQuestionMapper, Mockito.times(2)).insert(questionCaptor.capture());
    Set<Long> sourceIds = questionCaptor.getAllValues().stream()
        .map(CompetitionMakeExamPaperQuestionEntity::getSourceQuestionId)
        .collect(Collectors.toSet());
    assertThat(sourceIds).hasSize(2);
    assertThat(questionCaptor.getAllValues()).allSatisfy(question -> {
      assertThat(question.getStem()).startsWith("竞赛题干");
      assertThat(question.getOptionsJson()).contains("选项");
    });
    Mockito.verify(questionMapper, Mockito.times(1)).selectList(any());
  }

  @Test
  void createsIndependentPlansWithIncrementingVersions() {
    when(planMapper.selectMaxVersion(any(), any(), any(), any(), any())).thenReturn(0, 1);
    when(questionMapper.selectList(any())).thenReturn(List.of(question(6501L, "2", "single")));

    CompetitionMakeExamPlanUpsertReq request = new CompetitionMakeExamPlanUpsertReq(
        3001L, 3101L, "theory", "独立试卷计划", "初赛理论", 90,
        new BigDecimal("2"), new BigDecimal("1"), null,
        List.of(new CompetitionMakeExamPlanDetailReq(
            "single", 1, new BigDecimal("2"), null, null, 1)));

    service.create(1001L, request);
    service.create(1001L, request);

    ArgumentCaptor<CompetitionMakeExamPlanEntity> planCaptor =
        ArgumentCaptor.forClass(CompetitionMakeExamPlanEntity.class);
    Mockito.verify(planMapper, Mockito.times(2)).insert(planCaptor.capture());
    assertThat(planCaptor.getAllValues())
        .extracting(CompetitionMakeExamPlanEntity::getPlanVersion)
        .containsExactly(1, 2);
    assertThat(planCaptor.getAllValues())
        .extracting(CompetitionMakeExamPlanEntity::getPaperCount)
        .containsExactly(1, 1);

    ArgumentCaptor<CompetitionMakeExamPaperSnapshotEntity> snapshotCaptor =
        ArgumentCaptor.forClass(CompetitionMakeExamPaperSnapshotEntity.class);
    Mockito.verify(snapshotMapper, Mockito.times(2)).insert(snapshotCaptor.capture());
    assertThat(snapshotCaptor.getAllValues())
        .extracting(CompetitionMakeExamPaperSnapshotEntity::getPaperCode)
        .containsExactly("P", "P");
  }

  @Test
  void appliesPlanPointToEverySelectedQuestion() {
    when(questionMapper.selectList(any())).thenReturn(List.of(
        question(6201L, "40", "single"), question(6202L, "35", "single"),
        question(6203L, "25", "single"), question(6204L, "1", "single")));

    var result = service.create(1001L, new CompetitionMakeExamPlanUpsertReq(
        3001L, 3101L, "theory", "按总分组卷", "初赛理论", 90,
        new BigDecimal("4"), new BigDecimal("2"), null,
        List.of(new CompetitionMakeExamPlanDetailReq(
            "single", 4, new BigDecimal("1"), null, null, 1))));

    assertThat(result.status()).isEqualTo("draft");
    ArgumentCaptor<CompetitionMakeExamPaperSnapshotEntity> snapshotCaptor =
        ArgumentCaptor.forClass(CompetitionMakeExamPaperSnapshotEntity.class);
    Mockito.verify(snapshotMapper, Mockito.times(1)).insert(snapshotCaptor.capture());
    assertThat(snapshotCaptor.getAllValues()).allSatisfy(snapshot -> {
      assertThat(snapshot.getTotalScore()).isEqualByComparingTo("4");
      assertThat(snapshot.getQuestionCount()).isEqualTo(4);
    });
    ArgumentCaptor<CompetitionMakeExamPaperQuestionEntity> questionCaptor =
        ArgumentCaptor.forClass(CompetitionMakeExamPaperQuestionEntity.class);
    Mockito.verify(snapshotQuestionMapper, Mockito.times(4)).insert(questionCaptor.capture());
    assertThat(questionCaptor.getAllValues()).extracting(CompetitionMakeExamPaperQuestionEntity::getScore)
        .containsOnly(new BigDecimal("1"));
  }

  @Test
  void allowsTheoryPlanWhenOneQuestionSetMatchesTotalScore() {
    CompetitionMakeExamPlanDetailEntity detail = new CompetitionMakeExamPlanDetailEntity();
    detail.setPlanId(5004L);
    detail.setQuestionType("single");
    detail.setQuestionCount(4);
    detail.setPoint(new BigDecimal("25"));
    detail.setSortOrder(1);
    when(detailMapper.selectByPlan(eq(1L), any())).thenReturn(List.of(detail));
    when(questionMapper.selectList(any())).thenReturn(List.of(
        question(6401L, "25", "single"), question(6402L, "25", "single"),
        question(6403L, "25", "single"), question(6404L, "25", "single")));

    var result = service.create(1001L, new CompetitionMakeExamPlanUpsertReq(
        3001L, 3101L, "theory", "四道单选题", "中级理论", 90,
        new BigDecimal("100"), new BigDecimal("60"), null,
        List.of(new CompetitionMakeExamPlanDetailReq(
            "single", 4, new BigDecimal("25"), null, null, 1))));

    assertThat(result.status()).isEqualTo("draft");
    ArgumentCaptor<CompetitionMakeExamPaperSnapshotEntity> snapshotCaptor =
        ArgumentCaptor.forClass(CompetitionMakeExamPaperSnapshotEntity.class);
    Mockito.verify(snapshotMapper, Mockito.times(1)).insert(snapshotCaptor.capture());
    assertThat(snapshotCaptor.getAllValues()).allSatisfy(snapshot -> {
      assertThat(snapshot.getQuestionCount()).isEqualTo(4);
      assertThat(snapshot.getTotalScore()).isEqualByComparingTo("100");
    });
    Mockito.verify(snapshotQuestionMapper, Mockito.times(4)).insert(any(CompetitionMakeExamPaperQuestionEntity.class));
  }

  @Test
  void rejectsPlanWhenQuestionScoresCannotComposeConfiguredTotal() {
    CompetitionMakeExamPlanDetailEntity detail = new CompetitionMakeExamPlanDetailEntity();
    detail.setPlanId(5003L);
    detail.setQuestionType("single");
    detail.setQuestionCount(4);
    detail.setPoint(new BigDecimal("1"));
    detail.setSortOrder(1);
    when(detailMapper.selectByPlan(eq(1L), any())).thenReturn(List.of(detail));
    when(questionMapper.selectList(any())).thenReturn(List.of(
        question(6301L, "30", "single"), question(6302L, "30", "multi"), question(6303L, "30", "judge"),
        question(6304L, "30", "fill")));

    assertThatThrownBy(() -> service.create(1001L, new CompetitionMakeExamPlanUpsertReq(
        3001L, 3101L, "theory", "无法组卷", "理论", 90,
        new BigDecimal("100"), new BigDecimal("60"), null,
        List.of(new CompetitionMakeExamPlanDetailReq(
            "single", 4, new BigDecimal("1"), null, null, 1)))))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("总分");
    Mockito.verify(snapshotMapper, Mockito.never()).insert(any(CompetitionMakeExamPaperSnapshotEntity.class));
  }

  private CompetitionQuestionEntity question(Long id) {
    return question(id, "2", "single");
  }

  private CompetitionQuestionEntity question(Long id, String score, String questionType) {
    CompetitionQuestionEntity question = new CompetitionQuestionEntity();
    question.setId(id);
    question.setEventId(1001L);
    question.setEventWorkTypeId(3001L);
    question.setEventWorkTypeLevelId(3101L);
    question.setContentType("theory");
    question.setQuestionType(questionType);
    question.setSortOrder(id.intValue() - 6100);
    question.setStem("竞赛题干" + id);
    question.setScore(new BigDecimal(score));
    question.setReferenceAnswer("A");
    question.setOptionsJson("{\"A\":\"选项A\",\"B\":\"选项B\"}");
    question.setStatus("enabled");
    question.setDeleted(0);
    return question;
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
