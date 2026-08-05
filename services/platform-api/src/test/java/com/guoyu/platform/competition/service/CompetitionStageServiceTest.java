package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.competition.dto.CompetitionStageCreateReq;
import com.guoyu.platform.competition.dto.CompetitionStageUpdateReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.mapper.CompetitionEventMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CompetitionStageServiceTest {

  private final CompetitionStageMapper mapper = Mockito.mock(CompetitionStageMapper.class);
  private final CompetitionEventMapper eventMapper = Mockito.mock(CompetitionEventMapper.class);
  private final CompetitionStageService service = new CompetitionStageService(mapper);

  @Test
  void rejectsEndBeforeStart() {
    assertThatThrownBy(() -> service.create(1L, 1001L, new CompetitionStageCreateReq(
        "Preparation", "preparation", 1,
        LocalDateTime.of(2026, 8, 2, 9, 0),
        LocalDateTime.of(2026, 8, 1, 9, 0), null, null)))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .hasMessage("赛段结束时间不能早于开始时间");
    verify(mapper, never()).insert(any(CompetitionStageEntity.class));
  }

  @Test
  void rejectsStageOutsideCompetitionWindow() {
    CompetitionEventEntity event = new CompetitionEventEntity();
    event.setId(1001L);
    event.setTenantId(1L);
    event.setStartAt(LocalDateTime.of(2026, 8, 1, 9, 0));
    event.setEndAt(LocalDateTime.of(2026, 8, 10, 18, 0));
    when(eventMapper.selectForUpdate(1L, 1001L)).thenReturn(event);
    when(mapper.selectList(any())).thenReturn(java.util.List.of());

    CompetitionStageService boundedService = new CompetitionStageService(
        mapper,
        new com.guoyu.platform.competition.domain.CompetitionStageLifecycleDomainService(),
        null,
        null,
        eventMapper);

    assertThatThrownBy(() -> boundedService.create(1L, 1001L, new CompetitionStageCreateReq(
        "preliminary", "preliminary", 20,
        LocalDateTime.of(2026, 8, 1, 8, 59),
        LocalDateTime.of(2026, 8, 1, 10, 0), null, null)))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .hasMessage("赛段开始时间不能早于赛事开始时间");
    verify(mapper, never()).insert(any(CompetitionStageEntity.class));
  }

  @Test
  void missingStageReturnsNotFound() {
    when(mapper.selectOne(any())).thenReturn(null);

    assertThatThrownBy(() -> service.getById(1L, 9001L))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .hasMessage("赛段不存在");
  }

  @Test
  void updateCannotChangeLifecycleStatusThroughDeprecatedRequestField() {
    CompetitionStageEntity existing = new CompetitionStageEntity();
    existing.setId(9001L);
    existing.setTenantId(1L);
    existing.setEventId(1001L);
    existing.setStageType("qualification");
    existing.setStatus("configuring");
    when(mapper.selectOne(any())).thenReturn(existing);
    when(mapper.selectList(any())).thenReturn(java.util.List.of());
    when(mapper.updateById(any(CompetitionStageEntity.class))).thenReturn(1);

    CompetitionStageEntity updated = service.update(1L, 9001L,
        new CompetitionStageUpdateReq(null, "qualification", null, null, null, "draft", null));

    assertThat(updated.getStatus()).isEqualTo("configuring");
  }
}
