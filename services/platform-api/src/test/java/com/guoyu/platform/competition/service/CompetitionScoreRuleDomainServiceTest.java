package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.competition.dto.CompetitionScoreRuleCreateReq;
import com.guoyu.platform.competition.dto.CompetitionScoreRuleUpdateReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionScoreRuleEntity;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.mapper.CompetitionEventMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
import com.guoyu.platform.competition.mapper.CompetitionScoreRuleMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class CompetitionScoreRuleDomainServiceTest {

  private final CompetitionScoreRuleMapper ruleMapper =
      Mockito.mock(CompetitionScoreRuleMapper.class);
  private final CompetitionEventMapper eventMapper = Mockito.mock(CompetitionEventMapper.class);
  private final CompetitionStageMapper stageMapper = Mockito.mock(CompetitionStageMapper.class);
  private final CompetitionEventWorkTypeMapper workTypeMapper =
      Mockito.mock(CompetitionEventWorkTypeMapper.class);
  private final CompetitionScoreRuleDomainService service =
      new CompetitionScoreRuleDomainService(
          ruleMapper, eventMapper, stageMapper, workTypeMapper);

  @BeforeEach
  void setUpValidScope() {
    when(eventMapper.selectOne(any())).thenReturn(event("draft"));
    when(stageMapper.selectOne(any())).thenReturn(stage(1001L));
    when(workTypeMapper.selectOne(any())).thenReturn(workType(1001L, "active"));
  }

  @Test
  void createsFortySixtyRuleAsFirstDraftVersion() {
    when(ruleMapper.selectMaxRuleVersion(1L, 1001L, 2001L, 3001L)).thenReturn(null);
    when(ruleMapper.insert(any(CompetitionScoreRuleEntity.class))).thenAnswer(invocation -> {
      CompetitionScoreRuleEntity entity = invocation.getArgument(0);
      entity.setId(4001L);
      return 1;
    });

    CompetitionScoreRuleEntity created =
        service.create(1L, 9L, 1001L, createReq("THEORY_90", "40", "60"));

    assertThat(created.getId()).isEqualTo(4001L);
    assertThat(created.getScoringSystem()).isEqualTo("B");
    assertThat(created.getRuleVersion()).isEqualTo(1);
    assertThat(created.getStatus()).isEqualTo("draft");
    assertThat(created.getTheoryWeight()).isEqualByComparingTo("40");
    assertThat(created.getSimulationPracticalWeight()).isEqualByComparingTo("60");
  }

  @Test
  void rejectsInvalidWeightCombinationsAndRanges() {
    assertThatThrownBy(() ->
        service.create(1L, 9L, 1001L, createReq("THEORY_90", "40", "50")))
        .isInstanceOf(BusinessException.class)
        .hasMessage("理论权重与仿真实操组合权重之和必须为100");
    assertThatThrownBy(() ->
        service.create(1L, 9L, 1001L, createReq("THEORY_90", "-1", "101")))
        .isInstanceOf(BusinessException.class)
        .hasMessage("理论权重必须在0到100之间");
    assertThatThrownBy(() ->
        service.create(1L, 9L, 1001L, createReq("THEORY_90", "101", "-1")))
        .isInstanceOf(BusinessException.class)
        .hasMessage("理论权重必须在0到100之间");
    verify(ruleMapper, never()).insert(any(CompetitionScoreRuleEntity.class));
  }

  @Test
  void allowsIncompleteDraftButRejectsPublishingWithoutTemplate() {
    when(ruleMapper.selectMaxRuleVersion(anyLong(), anyLong(), anyLong(), anyLong())).thenReturn(0);
    when(ruleMapper.insert(any(CompetitionScoreRuleEntity.class))).thenReturn(1);

    CompetitionScoreRuleEntity draft = service.create(
        1L, 9L, 1001L,
        new CompetitionScoreRuleCreateReq(2001L, 3001L, "B", null, null, null));
    assertThat(draft.getTheoryTemplate()).isNull();

    draft.setId(4001L);
    when(ruleMapper.selectForUpdate(1L, 4001L)).thenReturn(draft);
    assertThatThrownBy(() -> service.publish(1L, 9L, 4001L))
        .isInstanceOfSatisfying(BusinessException.class,
            ex -> assertThat(ex.getCode()).isEqualTo("422"))
        .hasMessage("发布前必须选择THEORY_90或THEORY_120理论模板");
    verify(ruleMapper, never()).deprecatePublishedInScope(
        anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), any());
  }

  @Test
  void rejectsCrossEventStageReference() {
    when(stageMapper.selectOne(any())).thenReturn(null);

    assertThatThrownBy(() ->
        service.create(1L, 9L, 1001L, createReq("THEORY_120", "40", "60")))
        .isInstanceOfSatisfying(BusinessException.class,
            ex -> assertThat(ex.getCode()).isEqualTo("400"))
        .hasMessage("赛段不存在或不属于当前赛事");
    verify(ruleMapper, never()).insert(any(CompetitionScoreRuleEntity.class));
  }

  @Test
  void publishedRuleIsLockedAndNewVersionPublishesWhileDeprecatingOldVersion() {
    CompetitionScoreRuleEntity published = completeRule(4001L, 1, "published");
    when(ruleMapper.selectForUpdate(1L, 4001L)).thenReturn(published);

    assertThatThrownBy(() -> service.update(
        1L, 9L, 4001L, updateReq("THEORY_120", "40", "60")))
        .isInstanceOf(BusinessException.class)
        .hasMessage("已发布或已废止的评分规则不可修改，请创建新版本");

    when(ruleMapper.selectMaxRuleVersion(1L, 1001L, 2001L, 3001L)).thenReturn(1);
    when(ruleMapper.insert(any(CompetitionScoreRuleEntity.class))).thenAnswer(invocation -> {
      CompetitionScoreRuleEntity entity = invocation.getArgument(0);
      entity.setId(4002L);
      return 1;
    });
    CompetitionScoreRuleEntity versionTwo =
        service.create(1L, 9L, 1001L, createReq("THEORY_120", "40", "60"));
    assertThat(versionTwo.getRuleVersion()).isEqualTo(2);

    when(ruleMapper.selectForUpdate(1L, 4002L)).thenReturn(versionTwo);
    when(ruleMapper.selectPublishedForUpdate(1L, 1001L, 2001L, 3001L))
        .thenReturn(List.of(published));
    when(ruleMapper.updateById(versionTwo)).thenReturn(1);

    CompetitionScoreRuleEntity result = service.publish(1L, 9L, 4002L);

    assertThat(result.getStatus()).isEqualTo("published");
    assertThat(result.getPublishedAt()).isNotNull();
    assertThat(result.getPublishedBy()).isEqualTo(9L);
    ArgumentCaptor<Long> currentRuleId = ArgumentCaptor.forClass(Long.class);
    verify(ruleMapper).deprecatePublishedInScope(
        Mockito.eq(1L), Mockito.eq(1001L), Mockito.eq(2001L), Mockito.eq(3001L),
        currentRuleId.capture(), Mockito.eq(9L), any());
    assertThat(currentRuleId.getValue()).isEqualTo(4002L);
  }

  private CompetitionScoreRuleCreateReq createReq(
      String template, String theoryWeight, String combinationWeight) {
    return new CompetitionScoreRuleCreateReq(
        2001L, 3001L, "B", template,
        new BigDecimal(theoryWeight), new BigDecimal(combinationWeight));
  }

  private CompetitionScoreRuleUpdateReq updateReq(
      String template, String theoryWeight, String combinationWeight) {
    return new CompetitionScoreRuleUpdateReq(
        "B", template, new BigDecimal(theoryWeight), new BigDecimal(combinationWeight));
  }

  private CompetitionScoreRuleEntity completeRule(Long id, int version, String status) {
    CompetitionScoreRuleEntity entity = new CompetitionScoreRuleEntity();
    entity.setId(id);
    entity.setTenantId(1L);
    entity.setEventId(1001L);
    entity.setStageId(2001L);
    entity.setEventWorkTypeId(3001L);
    entity.setScoringSystem("B");
    entity.setTheoryTemplate("THEORY_90");
    entity.setTheoryWeight(new BigDecimal("40"));
    entity.setSimulationPracticalWeight(new BigDecimal("60"));
    entity.setStatus(status);
    entity.setRuleVersion(version);
    entity.setDeleted(0);
    return entity;
  }

  private CompetitionEventEntity event(String status) {
    CompetitionEventEntity entity = new CompetitionEventEntity();
    entity.setId(1001L);
    entity.setTenantId(1L);
    entity.setStatus(status);
    return entity;
  }

  private CompetitionStageEntity stage(Long eventId) {
    CompetitionStageEntity entity = new CompetitionStageEntity();
    entity.setId(2001L);
    entity.setTenantId(1L);
    entity.setEventId(eventId);
    entity.setStatus("active");
    return entity;
  }

  private CompetitionEventWorkTypeEntity workType(Long eventId, String status) {
    CompetitionEventWorkTypeEntity entity = new CompetitionEventWorkTypeEntity();
    entity.setId(3001L);
    entity.setTenantId(1L);
    entity.setEventId(eventId);
    entity.setStatus(status);
    return entity;
  }
}
