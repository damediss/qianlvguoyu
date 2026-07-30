package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.competition.dto.CompetitionEventOrgCreateReq;
import com.guoyu.platform.competition.entity.CompetitionEventOrgEntity;
import com.guoyu.platform.competition.mapper.CompetitionEventOrgMapper;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CompetitionEventOrgServiceTest {

  private final CompetitionEventOrgMapper mapper = Mockito.mock(CompetitionEventOrgMapper.class);
  private final CompetitionEventOrgService service = new CompetitionEventOrgService(mapper);

  @Test
  void createsInvitedRelationWithTenantAndEventBoundary() {
    when(mapper.selectOne(any())).thenReturn(null);
    AtomicReference<CompetitionEventOrgEntity> inserted = new AtomicReference<>();
    when(mapper.insert(any(CompetitionEventOrgEntity.class))).thenAnswer(invocation -> {
      inserted.set(invocation.getArgument(0));
      return 1;
    });

    CompetitionEventOrgEntity result = service.create(
        1L, 1001L, new CompetitionEventOrgCreateReq(3001L, 20, 1));

    assertThat(result).isSameAs(inserted.get());
    assertThat(result.getTenantId()).isEqualTo(1L);
    assertThat(result.getEventId()).isEqualTo(1001L);
    assertThat(result.getOrgId()).isEqualTo(3001L);
    assertThat(result.getStatus()).isEqualTo("invited");
    verify(mapper).insert(any(CompetitionEventOrgEntity.class));
  }

  @Test
  void rejectsDuplicateActiveRelation() {
    CompetitionEventOrgEntity existing = new CompetitionEventOrgEntity();
    existing.setStatus("active");
    when(mapper.selectOne(any())).thenReturn(existing);

    assertThatThrownBy(() -> service.create(
        1L, 1001L, new CompetitionEventOrgCreateReq(3001L, null, null)))
        .isInstanceOf(com.guoyu.platform.common.exception.BusinessException.class)
        .hasMessage("赛事参与企业已存在");
  }
}
