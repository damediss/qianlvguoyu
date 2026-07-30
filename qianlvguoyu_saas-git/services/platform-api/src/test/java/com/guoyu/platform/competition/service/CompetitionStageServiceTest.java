package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.competition.dto.CompetitionStageCreateReq;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.mapper.CompetitionStageMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CompetitionStageServiceTest {

  private final CompetitionStageMapper mapper = Mockito.mock(CompetitionStageMapper.class);
  private final CompetitionStageService service = new CompetitionStageService(mapper);

  @Test
  void rejectsEndBeforeStart() {
    assertThatThrownBy(() -> service.create(1L, 1001L, new CompetitionStageCreateReq(
        "Preparation", "preparation", 1,
        LocalDateTime.of(2026, 8, 2, 9, 0),
        LocalDateTime.of(2026, 8, 1, 9, 0), null)))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .hasMessage("赛段结束时间不能早于开始时间");
    verify(mapper, never()).insert(any(CompetitionStageEntity.class));
  }

  @Test
  void missingStageReturnsNotFound() {
    when(mapper.selectOne(any())).thenReturn(null);

    assertThatThrownBy(() -> service.getById(1L, 9001L))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .hasMessage("赛段不存在");
  }
}
