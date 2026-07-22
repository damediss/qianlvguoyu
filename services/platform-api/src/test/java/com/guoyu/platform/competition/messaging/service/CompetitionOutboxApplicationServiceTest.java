package com.guoyu.platform.competition.messaging.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.guoyu.platform.competition.entity.CompetitionNoticeEntity;
import com.guoyu.platform.competition.messaging.entity.CompetitionOutboxEventEntity;
import com.guoyu.platform.competition.messaging.mapper.CompetitionOutboxEventMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class CompetitionOutboxApplicationServiceTest {

  private final CompetitionOutboxEventMapper mapper = Mockito.mock(CompetitionOutboxEventMapper.class);
  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
  private final CompetitionOutboxApplicationService service =
      new CompetitionOutboxApplicationService(mapper, objectMapper);

  @Test
  void persistsIdentifierOnlyNoticePublishedOutboxPayload() throws Exception {
    CompetitionNoticeEntity notice = new CompetitionNoticeEntity();
    notice.setId(9001L);
    notice.setTenantId(1L);
    notice.setEventId(1001L);
    notice.setPublishedAt(LocalDateTime.of(2026, 7, 21, 14, 0));
    notice.setTitle("不应进入消息载荷的标题");
    notice.setContent("不应进入消息载荷的公告正文");
    when(mapper.insert(any(CompetitionOutboxEventEntity.class))).thenReturn(1);

    service.enqueueNoticePublished(notice, 1000L);

    ArgumentCaptor<CompetitionOutboxEventEntity> captor =
        ArgumentCaptor.forClass(CompetitionOutboxEventEntity.class);
    verify(mapper).insert(captor.capture());
    CompetitionOutboxEventEntity event = captor.getValue();
    JsonNode payload = objectMapper.readTree(event.getPayloadJson());
    assertThat(event.getEventType())
        .isEqualTo(CompetitionOutboxApplicationService.NOTICE_PUBLISHED_EVENT_TYPE);
    assertThat(event.getStatus()).isEqualTo("pending");
    assertThat(payload.get("tenantId").asLong()).isEqualTo(1L);
    assertThat(payload.get("eventId").asLong()).isEqualTo(1001L);
    assertThat(payload.get("noticeId").asLong()).isEqualTo(9001L);
    assertThat(event.getPayloadJson()).doesNotContain("标题", "公告正文");
  }
}
