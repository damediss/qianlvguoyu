package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.competition.dto.CompetitionTheoryPaperImportReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionScoreRuleEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryPaperQuestionEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryPaperSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryRequirementEntity;
import com.guoyu.platform.competition.mapper.CompetitionTheoryPaperQuestionMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryPaperSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryRequirementMapper;
import com.guoyu.platform.competition.service.CompetitionTheoryPaperSourceAdapter.SourcePaper;
import com.guoyu.platform.competition.service.CompetitionTheoryPaperSourceAdapter.SourceQuestion;
import com.guoyu.platform.competition.vo.CompetitionTheoryPaperSnapshotVO;
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
    when(scoreRuleDomainService.getById(1L, 4001L)).thenReturn(rule());
    when(eventWorkTypeDomainService.getById(1L, 3001L)).thenReturn(eventWorkType());
    when(requirementMapper.selectOne(any())).thenReturn(requirement());
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
        1001L, new CompetitionTheoryPaperImportReq(4001L, 601L, 701L, 702L));

    assertThat(result).hasSize(2);
    assertThat(result).extracting(CompetitionTheoryPaperSnapshotVO::paperCode)
        .containsExactly("A", "B");
    assertThat(result).allSatisfy(snapshot -> {
      assertThat(snapshot.status()).isEqualTo("published");
      assertThat(snapshot.questionCount()).isEqualTo(3);
      assertThat(snapshot.sourceHash()).hasSize(64);
    });
    verify(snapshotMapper).deprecatePublished(
        1L, 1001L, 2001L, 3001L, 4001L, "A", 9L);
    verify(snapshotMapper).deprecatePublished(
        1L, 1001L, 2001L, 3001L, 4001L, "B", 9L);
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
  void rejectsPaperWhoseQuestionCountsDoNotMatchRequirement() {
    SourcePaper invalid = new SourcePaper(
        701L,
        801L,
        601L,
        "错误A卷",
        90,
        new BigDecimal("3"),
        new BigDecimal("2"),
        List.of(singleQuestion(1L)));
    when(sourceAdapter.loadCompletedPaper(1L, 501L, 601L, 701L)).thenReturn(invalid);
    when(sourceAdapter.loadCompletedPaper(1L, 501L, 601L, 702L))
        .thenReturn(sourcePaper(702L, 802L, "B来源"));

    assertThatThrownBy(() -> service.importPair(
        1001L, new CompetitionTheoryPaperImportReq(4001L, 601L, 701L, 702L)))
        .isInstanceOf(BusinessException.class)
        .hasMessage("多选题数量不符合竞赛理论考试要求");
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

  private CompetitionTheoryRequirementEntity requirement() {
    CompetitionTheoryRequirementEntity entity = new CompetitionTheoryRequirementEntity();
    entity.setId(5001L);
    entity.setTenantId(1L);
    entity.setEventId(1001L);
    entity.setStageId(2001L);
    entity.setEventWorkTypeId(3001L);
    entity.setScoreRuleId(4001L);
    entity.setTemplateCode("CUSTOM");
    entity.setDurationMinutes(90);
    entity.setSingleCount(1);
    entity.setSingleScore(BigDecimal.ONE);
    entity.setMultiCount(1);
    entity.setMultiScore(BigDecimal.ONE);
    entity.setJudgeCount(1);
    entity.setJudgeScore(BigDecimal.ONE);
    entity.setTotalScore(new BigDecimal("3"));
    entity.setPassScore(new BigDecimal("2"));
    entity.setDeleted(0);
    return entity;
  }

  private CompetitionScoreRuleEntity rule() {
    CompetitionScoreRuleEntity entity = new CompetitionScoreRuleEntity();
    entity.setId(4001L);
    entity.setTenantId(1L);
    entity.setEventId(1001L);
    entity.setStageId(2001L);
    entity.setEventWorkTypeId(3001L);
    entity.setTheoryTemplate("CUSTOM");
    entity.setStatus("published");
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
