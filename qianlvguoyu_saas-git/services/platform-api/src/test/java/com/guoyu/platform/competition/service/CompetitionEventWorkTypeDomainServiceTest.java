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
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
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
    when(mapper.selectCount(any())).thenReturn(0L, 0L, 1L);
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
