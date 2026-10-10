package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.competition.dto.CompetitionEventWorkTypeCreateReq;
import com.guoyu.platform.competition.dto.CompetitionEventWorkTypeUpdateReq;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeStageEntity;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeLevelMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeStageMapper;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CompetitionEventWorkTypeDomainServiceTest {

  private final CompetitionEventWorkTypeMapper mapper =
      Mockito.mock(CompetitionEventWorkTypeMapper.class);
  private final ExamWorkTypeMapper examWorkTypeMapper =
      Mockito.mock(ExamWorkTypeMapper.class);
  private final CompetitionEventWorkTypeDomainService service =
      new CompetitionEventWorkTypeDomainService(mapper, examWorkTypeMapper);

  @Test
  void bindsDifferentWorkTypesAndRejectsDuplicateInSameEvent() {
    when(examWorkTypeMapper.selectOne(any())).thenReturn(source(10, "焊工"), source(11, "电工"));
    CompetitionEventWorkTypeEntity occupied = new CompetitionEventWorkTypeEntity();
    occupied.setId(100L);
    occupied.setStatus("active");
    when(mapper.selectOne(any())).thenReturn(null, null, occupied);
    AtomicLong ids = new AtomicLong(100);
    doAnswer(invocation -> {
      CompetitionEventWorkTypeEntity entity = invocation.getArgument(0);
      entity.setId(ids.incrementAndGet());
      return 1;
    }).when(mapper).insert(any(CompetitionEventWorkTypeEntity.class));

    CompetitionEventWorkTypeEntity welding = service.create(
        1L, 200L, new CompetitionEventWorkTypeCreateReq(10L, null, null, 1));
    CompetitionEventWorkTypeEntity electrician = service.create(
        1L, 200L, new CompetitionEventWorkTypeCreateReq(11L, "电工赛项", "active", 2));

    assertThat(welding.getDisplayName()).isEqualTo("焊工");
    assertThat(electrician.getDisplayName()).isEqualTo("电工赛项");
    assertThatThrownBy(() -> service.create(
        1L, 200L, new CompetitionEventWorkTypeCreateReq(10L, null, null, 3)))
        .isInstanceOf(BusinessException.class)
        .hasMessage("同一赛事不能重复绑定同一工种");
  }

  @Test
  void rejectsMissingInactiveOrCrossTenantSourceWorkType() {
    when(examWorkTypeMapper.selectOne(any())).thenReturn(null);

    assertThatThrownBy(() -> service.create(
        1L, 200L, new CompetitionEventWorkTypeCreateReq(99L, null, null, 0)))
        .isInstanceOf(BusinessException.class)
        .hasMessage("题库工种不存在、未启用或不属于当前租户");
    verify(mapper, never()).insert(any(CompetitionEventWorkTypeEntity.class));
  }

  @Test
  void publishedRulePreventsRebinding() {
    CompetitionEventWorkTypeEntity existing = new CompetitionEventWorkTypeEntity();
    existing.setId(300L);
    existing.setTenantId(1L);
    existing.setEventId(200L);
    existing.setWorkTypeId(10L);
    existing.setDisplayName("焊工");
    existing.setStatus("active");
    existing.setSortOrder(1);
    existing.setDeleted(0);
    when(mapper.selectOne(any())).thenReturn(existing);
    when(examWorkTypeMapper.selectOne(any())).thenReturn(source(11, "电工"));
    when(mapper.countPublishedRuleReferences(1L, 300L)).thenReturn(1L);

    assertThatThrownBy(() -> service.update(
        1L, 200L, 300L,
        new CompetitionEventWorkTypeUpdateReq(11L, null, "active", 1)))
        .isInstanceOf(BusinessException.class)
        .hasMessage("赛事工种已被已发布规则引用，不能换绑");
    verify(mapper, never()).updateById(any(CompetitionEventWorkTypeEntity.class));
  }

  @Test
  void updatePreservesOptionalStatusAndSortOrderWhenTheyAreOmitted() {
    CompetitionEventWorkTypeEntity existing = new CompetitionEventWorkTypeEntity();
    existing.setId(300L);
    existing.setTenantId(1L);
    existing.setEventId(200L);
    existing.setWorkTypeId(10L);
    existing.setDisplayName("旧显示名称");
    existing.setStatus("active");
    existing.setSortOrder(7);
    existing.setDeleted(0);
    when(mapper.selectOne(any())).thenReturn(existing);
    when(examWorkTypeMapper.selectOne(any())).thenReturn(source(10, "焊工"));
    when(mapper.updateById(any(CompetitionEventWorkTypeEntity.class))).thenReturn(1);

    CompetitionEventWorkTypeEntity updated = service.update(
        1L, 200L, 300L,
        new CompetitionEventWorkTypeUpdateReq(10L, null, null, null));

    assertThat(updated.getDisplayName()).isEqualTo("焊工");
    assertThat(updated.getStatus()).isEqualTo("active");
    assertThat(updated.getSortOrder()).isEqualTo(7);
    verify(mapper).updateById(existing);
  }

  @Test
  void listHidesInactiveWorkTypes() {
    CompetitionEventWorkTypeEntity active = binding(1L, "active", "铝电解槽操作工");
    CompetitionEventWorkTypeEntity inactive = binding(2L, "inactive", "电解槽操作工");
    when(mapper.selectList(any())).thenReturn(List.of(active, inactive));

    assertThat(service.list(1L, 200L))
        .extracting(CompetitionEventWorkTypeEntity::getDisplayName)
        .containsExactly("铝电解槽操作工");
  }

  @Test
  void createReactivatesInactiveBinding() {
    CompetitionEventWorkTypeEntity retired = binding(300L, "inactive", "电解槽操作工");
    retired.setWorkTypeId(59L);
    when(examWorkTypeMapper.selectOne(any())).thenReturn(source(59, "电解槽操作工"));
    when(mapper.selectOne(any())).thenReturn(retired);
    when(mapper.updateById(any(CompetitionEventWorkTypeEntity.class))).thenReturn(1);

    CompetitionEventWorkTypeEntity revived = service.create(
        1L, 200L, new CompetitionEventWorkTypeCreateReq(59L, null, "active", 2));

    assertThat(revived.getId()).isEqualTo(300L);
    assertThat(revived.getStatus()).isEqualTo("active");
    assertThat(revived.getSortOrder()).isEqualTo(2);
    verify(mapper, never()).insert(any(CompetitionEventWorkTypeEntity.class));
  }

  @Test
  void removingUnusedWorkTypeDeletesLevelsAndStageLinks() {
    CompetitionEventWorkTypeLevelMapper levelMapper = Mockito.mock(CompetitionEventWorkTypeLevelMapper.class);
    CompetitionEventWorkTypeStageMapper stageMapper = Mockito.mock(CompetitionEventWorkTypeStageMapper.class);
    CompetitionEventWorkTypeDomainService removing = new CompetitionEventWorkTypeDomainService(
        mapper, examWorkTypeMapper, levelMapper, stageMapper);
    when(mapper.selectOne(any())).thenReturn(binding(300L, "inactive", "电解槽操作工"));
    when(mapper.countOperationalReferences(1L, 300L)).thenReturn(0L);
    CompetitionEventWorkTypeLevelEntity level = new CompetitionEventWorkTypeLevelEntity();
    level.setId(400L);
    level.setStagePlanStatus("draft");
    CompetitionEventWorkTypeStageEntity stageLink = new CompetitionEventWorkTypeStageEntity();
    stageLink.setId(500L);
    when(levelMapper.selectList(any())).thenReturn(List.of(level));
    when(stageMapper.selectList(any())).thenReturn(List.of(stageLink));
    when(levelMapper.deleteById(400L)).thenReturn(1);
    when(stageMapper.deleteById(500L)).thenReturn(1);
    when(mapper.deleteById(300L)).thenReturn(1);

    removing.remove(1L, 200L, 300L);

    verify(stageMapper).deleteById(500L);
    verify(levelMapper).deleteById(400L);
    verify(mapper).deleteById(300L);
  }

  @Test
  void removingWorkTypeWithOperationalRowsKeepsTheBinding() {
    CompetitionEventWorkTypeLevelMapper levelMapper = Mockito.mock(CompetitionEventWorkTypeLevelMapper.class);
    CompetitionEventWorkTypeStageMapper stageMapper = Mockito.mock(CompetitionEventWorkTypeStageMapper.class);
    CompetitionEventWorkTypeDomainService removing = new CompetitionEventWorkTypeDomainService(
        mapper, examWorkTypeMapper, levelMapper, stageMapper);
    when(mapper.selectOne(any())).thenReturn(binding(300L, "active", "电解槽操作工"));
    when(mapper.countOperationalReferences(1L, 300L)).thenReturn(2L);

    assertThatThrownBy(() -> removing.remove(1L, 200L, 300L))
        .isInstanceOf(BusinessException.class)
        .hasMessage("该工种已有赛段、报名或试题等业务数据，不能移除");
    verify(mapper, never()).deleteById(any(java.io.Serializable.class));
    verify(levelMapper, never()).deleteById(any(java.io.Serializable.class));
  }

  @Test
  void markingWorkTypeInactiveRemovesItInsteadOfLeavingASelectableRow() {
    CompetitionEventWorkTypeLevelMapper levelMapper = Mockito.mock(CompetitionEventWorkTypeLevelMapper.class);
    CompetitionEventWorkTypeStageMapper stageMapper = Mockito.mock(CompetitionEventWorkTypeStageMapper.class);
    CompetitionEventWorkTypeDomainService removing = new CompetitionEventWorkTypeDomainService(
        mapper, examWorkTypeMapper, levelMapper, stageMapper);
    CompetitionEventWorkTypeEntity existing = binding(300L, "active", "电解槽操作工");
    when(mapper.selectOne(any())).thenReturn(existing);
    when(mapper.countOperationalReferences(1L, 300L)).thenReturn(0L);
    when(levelMapper.selectList(any())).thenReturn(List.of());
    when(stageMapper.selectList(any())).thenReturn(List.of());
    when(mapper.deleteById(300L)).thenReturn(1);

    CompetitionEventWorkTypeEntity updated = removing.update(
        1L, 200L, 300L,
        new CompetitionEventWorkTypeUpdateReq(59L, "电解槽操作工", "inactive", 0));

    assertThat(updated.getStatus()).isEqualTo("inactive");
    verify(mapper).deleteById(300L);
    verify(mapper, never()).updateById(any(CompetitionEventWorkTypeEntity.class));
  }

  private CompetitionEventWorkTypeEntity binding(Long id, String status, String displayName) {
    CompetitionEventWorkTypeEntity entity = new CompetitionEventWorkTypeEntity();
    entity.setId(id);
    entity.setTenantId(1L);
    entity.setEventId(200L);
    entity.setWorkTypeId(59L);
    entity.setDisplayName(displayName);
    entity.setStatus(status);
    entity.setSortOrder(0);
    entity.setStagePlanStatus("draft");
    entity.setDeleted(0);
    return entity;
  }

  private ExamWorkTypeEntity source(int id, String title) {
    ExamWorkTypeEntity entity = new ExamWorkTypeEntity();
    entity.setId(id);
    entity.setTenantId(1);
    entity.setTitle(title);
    entity.setStatus(1);
    entity.setDeleted(0);
    return entity;
  }
}
