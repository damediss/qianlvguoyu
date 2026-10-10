package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.competition.entity.CompetitionPracticalAssignmentEntity;
import com.guoyu.platform.competition.mapper.CompetitionPracticalAssignmentMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CompetitionPracticalAutoFinishSchedulerTest {
  private static final Long TENANT_ID = 10159L;

  private final CompetitionPracticalAssignmentMapper assignmentMapper =
      Mockito.mock(CompetitionPracticalAssignmentMapper.class);

  @BeforeAll
  static void initMybatisTableInfo() {
    MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
    TableInfoHelper.initTableInfo(assistant, CompetitionPracticalAssignmentEntity.class);
  }

  @AfterEach
  void clearTenantContext() {
    TenantContextHolder.clear();
  }

  @Test
  void recordsExpiredPublishedDeadlineAndClearsTenantContext() {
    CompetitionPracticalAssignmentEntity due = new CompetitionPracticalAssignmentEntity();
    due.setId(100L);
    due.setTenantId(TENANT_ID);
    due.setStatus("in_progress");
    due.setDeleted(0);
    due.setDeadlineAt(LocalDateTime.now().minusSeconds(1));
    when(assignmentMapper.selectTenantIdsForExpiredPracticalRuntime(any()))
        .thenReturn(List.of(TENANT_ID));
    when(assignmentMapper.selectList(any())).thenAnswer(invocation -> {
      assertThat(TenantContextHolder.getTenantId()).isEqualTo(TENANT_ID);
      return List.of(due);
    });
    when(assignmentMapper.update(Mockito.<CompetitionPracticalAssignmentEntity>isNull(),
        Mockito.<Wrapper<CompetitionPracticalAssignmentEntity>>any())).thenReturn(1);

    new CompetitionPracticalAutoFinishScheduler(assignmentMapper).finishExpiredAssignments();

    verify(assignmentMapper).update(Mockito.<CompetitionPracticalAssignmentEntity>isNull(),
        Mockito.<Wrapper<CompetitionPracticalAssignmentEntity>>any());
    assertThat(TenantContextHolder.getTenantId()).isNull();
  }
}
