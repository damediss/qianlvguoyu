package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.guoyu.platform.competition.entity.CompetitionRankingSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionScoreSourceEntity;
import com.guoyu.platform.competition.entity.CompetitionStageContentEntity;
import com.guoyu.platform.competition.entity.CompetitionTotalScoreSnapshotEntity;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateExamMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamSessionMapper;
import com.guoyu.platform.competition.mapper.CompetitionRankingEntryMapper;
import com.guoyu.platform.competition.mapper.CompetitionRankingSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionScoreRuleMapper;
import com.guoyu.platform.competition.mapper.CompetitionScoreSourceMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageContentMapper;
import com.guoyu.platform.competition.mapper.CompetitionTotalScoreSnapshotMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class CompetitionAutomaticScorePipelineServiceTest {

  private final CompetitionScoreSourceMapper sourceMapper =
      Mockito.mock(CompetitionScoreSourceMapper.class);
  private final CompetitionScoreRuleMapper ruleMapper =
      Mockito.mock(CompetitionScoreRuleMapper.class);
  private final CompetitionStageContentMapper stageContentMapper =
      Mockito.mock(CompetitionStageContentMapper.class);
  private final CompetitionTotalScoreSnapshotMapper totalMapper =
      Mockito.mock(CompetitionTotalScoreSnapshotMapper.class);
  private final CompetitionFormalExamSessionMapper sessionMapper =
      Mockito.mock(CompetitionFormalExamSessionMapper.class);
  private final CompetitionFormalCandidateExamMapper candidateMapper =
      Mockito.mock(CompetitionFormalCandidateExamMapper.class);
  private final CompetitionRankingSnapshotMapper rankingMapper =
      Mockito.mock(CompetitionRankingSnapshotMapper.class);
  private final CompetitionRankingEntryMapper entryMapper =
      Mockito.mock(CompetitionRankingEntryMapper.class);
  private final CompetitionAutomaticScorePipelineService service =
      new CompetitionAutomaticScorePipelineService(
          sourceMapper, ruleMapper, stageContentMapper, totalMapper,
          sessionMapper, candidateMapper, rankingMapper, entryMapper);

  @Test
  void practicalOnlyStageContentTurnsConfirmedPracticalScoreIntoTotal() {
    CompetitionScoreSourceEntity practical = confirmedPractical("99.83");
    when(sourceMapper.selectList(any())).thenReturn(List.of(practical));
    CompetitionStageContentEntity content = new CompetitionStageContentEntity();
    content.setContentType("practical");
    content.setStatus("enabled");
    content.setWeight(new BigDecimal("100"));
    when(stageContentMapper.selectList(any())).thenReturn(List.of(content));
    when(totalMapper.selectOne(any())).thenReturn(null);
    when(totalMapper.insert(any(CompetitionTotalScoreSnapshotEntity.class))).thenReturn(1);
    when(sessionMapper.selectOne(any())).thenReturn(null);
    when(totalMapper.selectList(any())).thenReturn(List.of());
    when(rankingMapper.selectOne(any())).thenReturn(null);
    when(rankingMapper.insert(any(CompetitionRankingSnapshotEntity.class))).thenReturn(1);

    CompetitionTotalScoreSnapshotEntity total = service.onConfirmedSource(practical, 1000L);

    ArgumentCaptor<CompetitionTotalScoreSnapshotEntity> captor =
        ArgumentCaptor.forClass(CompetitionTotalScoreSnapshotEntity.class);
    Mockito.verify(totalMapper).insert(captor.capture());
    assertThat(total.getTotalScore()).isEqualByComparingTo("99.83");
    assertThat(total.getPracticalScore()).isEqualByComparingTo("99.83");
    assertThat(total.getPracticalWeight()).isEqualByComparingTo("100");
    assertThat(total.getTheoryWeight()).isEqualByComparingTo("0");
    assertThat(total.getScoreRuleId()).isNull();
    assertThat(captor.getValue().getConfirmationStatus()).isEqualTo("confirmed");
  }

  private CompetitionScoreSourceEntity confirmedPractical(String score) {
    CompetitionScoreSourceEntity source = new CompetitionScoreSourceEntity();
    source.setId(11L);
    source.setTenantId(10159L);
    source.setEventId(20L);
    source.setStageId(10L);
    source.setEventWorkTypeId(30L);
    source.setEventWorkTypeLevelId(40L);
    source.setContestantMemberId(50L);
    source.setSourceType("practical");
    source.setRawScore(new BigDecimal(score));
    source.setConfirmationStatus("confirmed");
    source.setCurrentFlag(1);
    return source;
  }
}
