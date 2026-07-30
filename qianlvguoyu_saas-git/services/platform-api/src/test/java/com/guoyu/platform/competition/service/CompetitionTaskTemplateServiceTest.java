package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.guoyu.platform.competition.dto.CompetitionTaskTemplateCreateReq;
import com.guoyu.platform.competition.entity.CompetitionTaskTemplateEntity;
import com.guoyu.platform.competition.mapper.CompetitionTaskTemplateMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CompetitionTaskTemplateServiceTest {

  private final CompetitionTaskTemplateMapper mapper = Mockito.mock(CompetitionTaskTemplateMapper.class);
  private final CompetitionTaskTemplateService service = new CompetitionTaskTemplateService(mapper);

  @Test
  void rejectsUnsupportedTaskType() {
    assertThatThrownBy(() -> service.create(1L, 5001L, new CompetitionTaskTemplateCreateReq(
        "exam", "Exam", null, null, 0, null, null, 1)))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .hasMessage("任务模板类型不合法");
    verify(mapper, never()).insert(any(CompetitionTaskTemplateEntity.class));
  }
}
