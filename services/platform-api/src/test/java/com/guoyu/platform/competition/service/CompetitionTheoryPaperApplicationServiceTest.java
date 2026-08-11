package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.competition.dto.CompetitionTheoryPaperImportReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryPaperQuestionEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryPaperSnapshotEntity;
import com.guoyu.platform.competition.mapper.CompetitionTheoryPaperQuestionMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryPaperSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryRequirementMapper;
import com.guoyu.platform.competition.service.CompetitionTheoryPaperSourceAdapter.SourcePaper;
import com.guoyu.platform.competition.service.CompetitionTheoryPaperSourceAdapter.SourceQuestion;
import com.guoyu.platform.competition.vo.CompetitionTheoryPaperSnapshotVO;
import com.guoyu.platform.competition.vo.CompetitionTheoryPaperPreviewVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CompetitionTheoryPaperApplicationServiceTest {

  private final CompetitionScoreRuleDomainService scoreRuleDomainService =
      Mockito.mock(CompetitionScoreRuleDomainService.class);
  private final CompetitionScoreRuleApplicationService scoreRuleApplicationService =
      Mockito.mock(CompetitionScoreRuleApplicationService.class);
  private final CompetitionStageService stageService =
      Mockito.mock(CompetitionStageService.class);
  private final CompetitionEventWorkTypeDomainService eventWorkTypeDomainService =
      Mockito.mock(CompetitionEventWorkTypeDomainService.class);
  private final CompetitionTheoryRequirementMapper requirementMapper =
      Mockito.mock(CompetitionTheoryRequirementMapper.class);
  private final CompetitionTheoryPaperSnapshotMapper snapshotMapper =
      Mockito.mock(CompetitionTheoryPaperSnapshotMapper.class);
  private final CompetitionTheoryPaperQuestionMapper questionMapper =
      Mockito.mock(CompetitionTheoryPaperQuestionMapper.class);
  private final CompetitionTheoryPaperSourceAdapter sourceAdapter =
      Mockito.mock(CompetitionTheoryPaperSourceAdapter.class);
  private final CompetitionTheoryAnswerSnapshotAdapter answerAdapter =
      new CompetitionTheoryAnswerSnapshotAdapter(new ObjectMapper());
  private final CompetitionTheoryPaperApplicationService service =
      new CompetitionTheoryPaperApplicationService(
          scoreRuleDomainService,
          scoreRuleApplicationService,
          stageService,
          eventWorkTypeDomainService,
          requirementMapper,
          snapshotMapper,
          questionMapper,
          sourceAdapter,
          answerAdapter);

  @BeforeEach
  void setUp() {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        9L, 1L, "admin", "admin", "business",
        List.of("PLATFORM_ADMIN"),
        List.of("competition:theory-paper:view", "competition:theory-paper:manage"),
        "admin@example.com");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
    when(scoreRuleApplicationService.manageableEvent(any(), eq(1001L)))
        .thenReturn(event());
    when(stageService.getById(1L, 2001L)).thenReturn(stage());
    when(eventWorkTypeDomainService.getById(1L, 3001L)).thenReturn(eventWorkType());
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void importsIndependentCoursePlansAsImmutableABSnapshots() {
    when(sourceAdapter.loadCompletedPaper(1L, 501L, 601L, 701L))
        .thenReturn(sourcePaper(701L, 801L, "A来源"));
    when(sourceAdapter.loadCompletedPaper(1L, 501L, 601L, 702L))
        .thenReturn(sourcePaper(702L, 802L, "B来源"));
    AtomicLong ids = new AtomicLong(9000L);
    when(snapshotMapper.insert(any(CompetitionTheoryPaperSnapshotEntity.class)))
        .thenAnswer(invocation -> {
          CompetitionTheoryPaperSnapshotEntity entity = invocation.getArgument(0);
          entity.setId(ids.incrementAndGet());
          return 1;
        });
    when(questionMapper.insert(any(CompetitionTheoryPaperQuestionEntity.class))).thenReturn(1);

    List<CompetitionTheoryPaperSnapshotVO> result = service.importPair(
        1001L, new CompetitionTheoryPaperImportReq(2001L, 3001L, 601L, 701L, 702L));

    assertThat(result).hasSize(2);
    assertThat(result).extracting(CompetitionTheoryPaperSnapshotVO::paperCode)
        .containsExactly("A", "B");
    assertThat(result).allSatisfy(snapshot -> {
      assertThat(snapshot.status()).isEqualTo("published");
      assertThat(snapshot.questionCount()).isEqualTo(3);
      assertThat(snapshot.sourceHash()).hasSize(64);
    });
    ArgumentCaptor<CompetitionTheoryPaperSnapshotEntity> snapshotCaptor =
        ArgumentCaptor.forClass(CompetitionTheoryPaperSnapshotEntity.class);
    verify(snapshotMapper, Mockito.times(2)).insert(snapshotCaptor.capture());
    assertThat(snapshotCaptor.getAllValues())
        .allSatisfy(snapshot -> assertThat(snapshot.getScoreRuleId()).isNull());
    verify(snapshotMapper).deprecatePublished(
        1L, 1001L, 2001L, 3001L, null, "A", 9L);
    verify(snapshotMapper).deprecatePublished(
        1L, 1001L, 2001L, 3001L, null, "B", 9L);
    ArgumentCaptor<CompetitionTheoryPaperQuestionEntity> questionCaptor =
        ArgumentCaptor.forClass(CompetitionTheoryPaperQuestionEntity.class);
    verify(questionMapper, Mockito.times(6)).insert(questionCaptor.capture());
    assertThat(questionCaptor.getAllValues())
        .allSatisfy(question -> {
          assertThat(question.getOptionsJson()).doesNotContain("isCorrect");
          assertThat(question.getCorrectAnswersJson()).startsWith("[");
          assertThat(question.getScoringRuleCode()).isEqualTo("exact_match");
        });
  }

  @Test
  void importsCoursePaperWithoutCompetitionTheoryRequirementValidation() {
    SourcePaper invalid = new SourcePaper(
        701L,
        801L,
        601L,
        "错误A卷",
        90,
        new BigDecimal("3"),
        new BigDecimal("2"),
        List.of(singleQuestion(1L), singleQuestion(2L), singleQuestion(3L)));
    when(sourceAdapter.loadCompletedPaper(1L, 501L, 601L, 701L)).thenReturn(invalid);
    when(sourceAdapter.loadCompletedPaper(1L, 501L, 601L, 702L))
        .thenReturn(sourcePaper(702L, 802L, "B来源"));
    AtomicLong ids = new AtomicLong(9000L);
    when(snapshotMapper.insert(any(CompetitionTheoryPaperSnapshotEntity.class)))
        .thenAnswer(invocation -> {
          CompetitionTheoryPaperSnapshotEntity entity = invocation.getArgument(0);
          entity.setId(ids.incrementAndGet());
          return 1;
        });
    when(questionMapper.insert(any(CompetitionTheoryPaperQuestionEntity.class))).thenReturn(1);

    List<CompetitionTheoryPaperSnapshotVO> result = service.importPair(
        1001L, new CompetitionTheoryPaperImportReq(2001L, 3001L, 601L, 701L, 702L));

    assertThat(result).hasSize(2);
    verify(requirementMapper, Mockito.never()).selectOne(any());
  }

  @Test
  void previewOnlyExposesQuestionTypeAndScore() throws Exception {
    CompetitionTheoryPaperSnapshotEntity snapshot = new CompetitionTheoryPaperSnapshotEntity();
    snapshot.setId(4001L);
    snapshot.setTenantId(1L);
    snapshot.setEventId(1001L);
    snapshot.setPaperCode("A");
    snapshot.setVersionNo(1);
    snapshot.setSourceTitle("初级理论试卷");
    snapshot.setDurationMinutes(90);
    snapshot.setQuestionCount(1);
    snapshot.setTotalScore(new BigDecimal("100"));
    snapshot.setDeleted(0);
    when(snapshotMapper.selectOne(any())).thenReturn(snapshot);

    CompetitionTheoryPaperQuestionEntity question = new CompetitionTheoryPaperQuestionEntity();
    question.setSortOrder(1);
    question.setQuestionType("single");
    question.setContent("不能出现在预览中的题干");
    question.setOptionsJson("[{\"content\":\"A\"}]");
    question.setCorrectAnswersJson("[\"A\"]");
    question.setResolveContent("不能出现在预览中的解析");
    question.setScore(new BigDecimal("1"));
    when(questionMapper.selectByPaper(4001L)).thenReturn(List.of(question));

    CompetitionTheoryPaperPreviewVO preview = service.previewSnapshot(1001L, 4001L);

    assertThat(preview.questions()).hasSize(1);
    assertThat(preview.questions().get(0).content()).isEqualTo("不能出现在预览中的题干");
    assertThat(preview.questions().get(0).questionType()).isEqualTo("single");
    assertThat(preview.questions().get(0).score()).isEqualByComparingTo("1");
    String serialized = new ObjectMapper().writeValueAsString(preview);
    assertThat(serialized).contains("不能出现在预览中的题干");
    assertThat(serialized).doesNotContain("optionsJson");
    assertThat(serialized).doesNotContain("correctAnswersJson");
    assertThat(serialized).doesNotContain("resolveContent");
  }

  private SourcePaper sourcePaper(Long paperId, Long planId, String title) {
    return new SourcePaper(
        paperId,
        planId,
        601L,
        title,
        90,
        new BigDecimal("3"),
        new BigDecimal("2"),
        List.of(
            singleQuestion(paperId * 10 + 1),
            multiQuestion(paperId * 10 + 2),
            judgeQuestion(paperId * 10 + 3)));
  }

  private SourceQuestion singleQuestion(Long id) {
    return question(id, "single", """
        [
          {"content":"甲","isCorrect":true},
          {"content":"乙","isCorrect":false}
        ]
        """);
  }

  private SourceQuestion multiQuestion(Long id) {
    return question(id, "multiple", """
        [
          {"content":"甲","isCorrect":true},
          {"content":"乙","isCorrect":true},
          {"content":"丙","isCorrect":false}
        ]
        """);
  }

  private SourceQuestion judgeQuestion(Long id) {
    return question(id, "judge", """
        [
          {"content":"正确","isCorrect":true},
          {"content":"错误","isCorrect":false}
        ]
        """);
  }

  private SourceQuestion question(Long id, String type, String answers) {
    return new SourceQuestion(
        id,
        id,
        type,
        "题干" + id,
        answers,
        BigDecimal.ONE,
        "解析",
        2,
        100L,
        "来源组织",
        200L,
        null);
  }

  private CompetitionStageEntity stage() {
    CompetitionStageEntity entity = new CompetitionStageEntity();
    entity.setId(2001L);
    entity.setTenantId(1L);
    entity.setEventId(1001L);
    entity.setDeleted(0);
    return entity;
  }

  private CompetitionEventWorkTypeEntity eventWorkType() {
    CompetitionEventWorkTypeEntity entity = new CompetitionEventWorkTypeEntity();
    entity.setId(3001L);
    entity.setTenantId(1L);
    entity.setEventId(1001L);
    entity.setWorkTypeId(501L);
    entity.setDeleted(0);
    return entity;
  }

  private CompetitionEventEntity event() {
    CompetitionEventEntity entity = new CompetitionEventEntity();
    entity.setId(1001L);
    entity.setTenantId(1L);
    entity.setStatus("published");
    return entity;
  }
}
