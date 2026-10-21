package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.domain.CompetitionScoreRuleReferenceValidator;
import com.guoyu.platform.competition.dto.CompetitionCalculatedRankingPublishReq;
import com.guoyu.platform.competition.dto.CompetitionRankingSnapshotGenerateReq;
import com.guoyu.platform.competition.dto.CompetitionScoreSourceRevisionReq;
import com.guoyu.platform.competition.dto.CompetitionTotalScoreCalculateReq;
import com.guoyu.platform.competition.entity.CompetitionRankingEntryEntity;
import com.guoyu.platform.competition.entity.CompetitionRankingSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionScoreSourceEntity;
import com.guoyu.platform.competition.entity.CompetitionTotalScoreSnapshotEntity;
import com.guoyu.platform.competition.mapper.CompetitionRankingEntryMapper;
import com.guoyu.platform.competition.mapper.CompetitionRankingSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionScoreSourceMapper;
import com.guoyu.platform.competition.mapper.CompetitionTotalScoreSnapshotMapper;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CompetitionScoringApplicationServiceTest {

  private static final Long EVENT_ID = 1001L;
  private static final Long STAGE_A_ID = 2001L;
  private static final Long EVENT_WORK_TYPE_ID = 3001L;
  private static final Long CONTESTANT_MEMBER_ID = 5001L;
  private static final Long SCORE_RULE_ID = 4001L;

  private CompetitionScoreSourceMapper scoreSourceMapper;
  private CompetitionTotalScoreSnapshotMapper totalScoreSnapshotMapper;
  private CompetitionRankingSnapshotMapper rankingSnapshotMapper;
  private CompetitionRankingEntryMapper rankingEntryMapper;
  private CompetitionContestantScopeValidator contestantScopeValidator;
  private CompetitionScoreRuleDomainService scoreRuleDomainService;
  private CompetitionScoreRuleReferenceValidator scoreRuleReferenceValidator;
  private CompetitionScoreRuleApplicationService scoreRuleApplicationService;
  private CompetitionScoringApplicationService service;

  @BeforeEach
  void setUp() {
    scoreSourceMapper = Mockito.mock(CompetitionScoreSourceMapper.class);
    totalScoreSnapshotMapper = Mockito.mock(CompetitionTotalScoreSnapshotMapper.class);
    rankingSnapshotMapper = Mockito.mock(CompetitionRankingSnapshotMapper.class);
    rankingEntryMapper = Mockito.mock(CompetitionRankingEntryMapper.class);
    contestantScopeValidator = Mockito.mock(CompetitionContestantScopeValidator.class);
    scoreRuleDomainService = Mockito.mock(CompetitionScoreRuleDomainService.class);
    scoreRuleReferenceValidator = Mockito.mock(CompetitionScoreRuleReferenceValidator.class);
    scoreRuleApplicationService = Mockito.mock(CompetitionScoreRuleApplicationService.class);
    service = new CompetitionScoringApplicationService(
        scoreSourceMapper,
        totalScoreSnapshotMapper,
        rankingSnapshotMapper,
        rankingEntryMapper,
        contestantScopeValidator,
        scoreRuleDomainService,
        scoreRuleReferenceValidator,
        scoreRuleApplicationService);
    authenticate();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void sourceRevisionRejectsContestantFromAnotherStageBeforeWriting() {
    rejectCurrentScope();

    assertThatThrownBy(() -> service.reviseScoreSource(EVENT_ID, revisionRequest()))
        .isInstanceOf(BusinessException.class);

    verify(scoreRuleReferenceValidator, never()).validatePublishedRule(
        any(), any(), any(), any(), any());
    verify(scoreSourceMapper, never()).selectList(any());
    verify(scoreSourceMapper, never()).insert(any(CompetitionScoreSourceEntity.class));
  }

  @Test
  void totalScoreCalculationRejectsContestantFromAnotherStageBeforeReadingScores() {
    rejectCurrentScope();

    assertThatThrownBy(() -> service.calculateTotalScore(EVENT_ID, totalScoreRequest()))
        .isInstanceOf(BusinessException.class);

    verify(scoreRuleReferenceValidator, never()).validatePublishedRule(
        any(), any(), any(), any(), any());
    verifyNoInteractions(scoreRuleDomainService, scoreSourceMapper, totalScoreSnapshotMapper);
  }

  @Test
  void rankingGenerationRejectsContestantFromAnotherStageBeforeCreatingSnapshot() {
    rejectCurrentScope();

    assertThatThrownBy(() -> service.generateRankingSnapshot(EVENT_ID, rankingRequest()))
        .isInstanceOf(BusinessException.class);

    verify(scoreRuleReferenceValidator, never()).validatePublishedRule(
        any(), any(), any(), any(), any());
    verifyNoInteractions(totalScoreSnapshotMapper, rankingSnapshotMapper, rankingEntryMapper);
  }

  @Test
  void contestantWithoutStageAssignmentRemainsCompatible() {
    when(scoreSourceMapper.selectList(any())).thenReturn(List.of());
    when(scoreSourceMapper.insert(any(CompetitionScoreSourceEntity.class))).thenReturn(1);

    assertThat(service.reviseScoreSource(EVENT_ID, revisionRequest()).stageId())
        .isEqualTo(STAGE_A_ID);

    verify(scoreSourceMapper).insert(any(CompetitionScoreSourceEntity.class));
  }

  @Test
  void genericPublishRejectsUnreviewedRankingDraft() {
    CompetitionRankingSnapshotEntity draft = rankingSnapshot("draft");
    when(rankingSnapshotMapper.selectById(draft.getId())).thenReturn(draft);

    assertThatThrownBy(() -> service.publishRankingSnapshot(draft.getId(), null))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("必须先经复核确认");

    verify(rankingSnapshotMapper, never()).selectOne(any());
    verify(rankingSnapshotMapper, never())
        .updateById(any(CompetitionRankingSnapshotEntity.class));
  }

  @Test
  void calculatedPublishConfirmsAndPublishesCurrentDraft() {
    CompetitionRankingSnapshotEntity draft = rankingSnapshot("draft");
    CompetitionAutomaticScorePipelineService pipeline =
        Mockito.mock(CompetitionAutomaticScorePipelineService.class);
    service.setAutomaticScorePipelineService(pipeline);
    when(pipeline.ensureCurrentContentRankingDraft(
        1L, EVENT_ID, STAGE_A_ID, EVENT_WORK_TYPE_ID, null, 9001L)).thenReturn(draft);
    when(rankingSnapshotMapper.selectById(draft.getId())).thenReturn(draft);
    CompetitionRankingEntryEntity entry = new CompetitionRankingEntryEntity();
    entry.setRankingSnapshotId(draft.getId());
    entry.setContestantMemberId(CONTESTANT_MEMBER_ID);
    entry.setTotalScoreSnapshotId(8001L);
    entry.setTotalScore(new BigDecimal("88"));
    entry.setContestantStatus("completed");
    when(rankingEntryMapper.selectList(any())).thenReturn(List.of(entry));
    CompetitionTotalScoreSnapshotEntity total = new CompetitionTotalScoreSnapshotEntity();
    total.setEventId(EVENT_ID);
    total.setStageId(STAGE_A_ID);
    total.setEventWorkTypeId(EVENT_WORK_TYPE_ID);
    total.setContestantMemberId(CONTESTANT_MEMBER_ID);
    total.setTotalScore(new BigDecimal("88"));
    total.setConfirmationStatus("confirmed");
    total.setCurrentFlag(1);
    when(totalScoreSnapshotMapper.selectById(8001L)).thenReturn(total);
    when(rankingSnapshotMapper.updateById(draft)).thenReturn(1);

    var result = service.publishCalculatedRanking(
        EVENT_ID, new CompetitionCalculatedRankingPublishReq(
            STAGE_A_ID, EVENT_WORK_TYPE_ID, null, null));

    assertThat(result.status()).isEqualTo("published");
    assertThat(draft.getConfirmedBy()).isEqualTo(9001L);
    assertThat(draft.getPublishedBy()).isEqualTo(9001L);
    verify(rankingSnapshotMapper).updateById(draft);
  }

  private void rejectCurrentScope() {
    doThrow(new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
        "contestant is assigned to another stage and cannot write current stage scores"))
        .when(contestantScopeValidator)
        .validate(1L, EVENT_ID, STAGE_A_ID, EVENT_WORK_TYPE_ID, CONTESTANT_MEMBER_ID);
  }

  private void authenticate() {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        9001L,
        1L,
        "platform-admin",
        "Platform Admin",
        "business",
        List.of("PLATFORM_ADMIN"),
        List.of("competition:score:manage", "competition:score:calculate",
            "competition:ranking:generate"),
        "test@example.com");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
  }

  private CompetitionScoreSourceRevisionReq revisionRequest() {
    return new CompetitionScoreSourceRevisionReq(
        STAGE_A_ID,
        EVENT_WORK_TYPE_ID,
        CONTESTANT_MEMBER_ID,
        "theory",
        new BigDecimal("88"),
        "confirmed",
        "manual",
        "fix-01",
        SCORE_RULE_ID,
        LocalDateTime.of(2026, 8, 3, 10, 0));
  }

  private CompetitionTotalScoreCalculateReq totalScoreRequest() {
    return new CompetitionTotalScoreCalculateReq(
        STAGE_A_ID, EVENT_WORK_TYPE_ID, CONTESTANT_MEMBER_ID, SCORE_RULE_ID);
  }

  private CompetitionRankingSnapshotGenerateReq rankingRequest() {
    return new CompetitionRankingSnapshotGenerateReq(
        STAGE_A_ID,
        EVENT_WORK_TYPE_ID,
        SCORE_RULE_ID,
        "fix-01",
        List.of(new CompetitionRankingSnapshotGenerateReq.Candidate(
            CONTESTANT_MEMBER_ID, 8001L, "88", "completed")));
  }

  private CompetitionRankingSnapshotEntity rankingSnapshot(String status) {
    CompetitionRankingSnapshotEntity snapshot = new CompetitionRankingSnapshotEntity();
    snapshot.setId(7001L);
    snapshot.setTenantId(1L);
    snapshot.setEventId(EVENT_ID);
    snapshot.setStageId(STAGE_A_ID);
    snapshot.setEventWorkTypeId(EVENT_WORK_TYPE_ID);
    snapshot.setStatus(status);
    return snapshot;
  }
}
