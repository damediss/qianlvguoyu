package com.guoyu.platform.competition.exam.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.competition.entity.CompetitionFormalExamOutboxEntity;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamOutboxMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class FormalExamRealtimeCatchUpServiceTest {

  private final CompetitionFormalExamOutboxMapper outbox =
      Mockito.mock(CompetitionFormalExamOutboxMapper.class);
  private final FormalExamRealtimeProperties properties = new FormalExamRealtimeProperties();
  private final FormalExamRealtimeCatchUpService service =
      new FormalExamRealtimeCatchUpService(outbox, properties, new ObjectMapper().findAndRegisterModules());

  @Test
  void playerSkipsOtherAccessEventsButAdvancesNextSeq() {
    when(outbox.selectHighWatermark(1L, 9001L)).thenReturn(12L);
    when(outbox.selectMinSeq(1L, 9001L)).thenReturn(1L);
    when(outbox.selectCatchUp(eq(1L), eq(9001L), eq(0L), eq(12L), anyInt()))
        .thenReturn(List.of(
            row(1L, FormalExamRealtimeTypes.TARGET_SESSION, null, "{\"eventId\":\"s\",\"type\":\"session.status.changed\",\"seq\":1,\"target\":\"examSession\",\"payload\":{}}"),
            row(5L, FormalExamRealtimeTypes.TARGET_ACCESS, 999L, "{\"eventId\":\"other\",\"type\":\"candidate.progress.changed\",\"seq\":5,\"target\":\"accessSession\",\"accessSessionId\":\"999\",\"payload\":{\"answeredCount\":1}}"),
            row(12L, FormalExamRealtimeTypes.TARGET_ACCESS, 100L, "{\"eventId\":\"own\",\"type\":\"candidate.progress.changed\",\"seq\":12,\"target\":\"accessSession\",\"accessSessionId\":\"100\",\"payload\":{\"answeredCount\":2}}")));

    var page = service.catchUp(1L, 9001L, "0", 50,
        FormalExamRealtimeCatchUpService.Viewer.PLAYER, 100L, 1);

    assertThat(page.events()).extracting(FormalExamRealtimeEnvelope::eventId)
        .containsExactly("s", "own");
    assertThat(page.nextSeq()).isEqualTo("12");
    assertThat(page.highWatermark()).isEqualTo("12");
    assertThat(page.hasMore()).isFalse();
    assertThat(page.snapshotRequired()).isFalse();
  }

  @Test
  void snapshotRequiredWhenRetainedPrefixIsMissing() {
    when(outbox.selectHighWatermark(1L, 9001L)).thenReturn(20L);
    when(outbox.selectMinSeq(1L, 9001L)).thenReturn(10L);
    when(outbox.selectCatchUp(eq(1L), eq(9001L), eq(1L), eq(20L), anyInt())).thenReturn(List.of());

    var page = service.catchUp(1L, 9001L, "1", 50,
        FormalExamRealtimeCatchUpService.Viewer.CONSOLE, null, null);

    assertThat(page.snapshotRequired()).isTrue();
    assertThat(page.nextSeq()).isEqualTo("1");
  }

  @Test
  void sanitizesSensitivePayloadKeys() {
    when(outbox.selectHighWatermark(1L, 9001L)).thenReturn(1L);
    when(outbox.selectMinSeq(1L, 9001L)).thenReturn(1L);
    when(outbox.selectCatchUp(eq(1L), eq(9001L), eq(0L), eq(1L), anyInt()))
        .thenReturn(List.of(row(1L, FormalExamRealtimeTypes.TARGET_SESSION, null,
            "{\"eventId\":\"s\",\"type\":\"session.status.changed\",\"seq\":1,\"target\":\"examSession\",\"payload\":{\"status\":\"running\",\"token\":\"secret\",\"phone\":\"13800000000\"}}")));

    var page = service.catchUp(1L, 9001L, "0", 10,
        FormalExamRealtimeCatchUpService.Viewer.CONSOLE, null, null);

    assertThat(page.events()).hasSize(1);
    assertThat(page.events().getFirst().payload()).containsEntry("status", "running");
    assertThat(page.events().getFirst().payload()).doesNotContainKeys("token", "phone");
  }

  private static CompetitionFormalExamOutboxEntity row(
      long seq, String target, Long accessSessionId, String json) {
    CompetitionFormalExamOutboxEntity entity = new CompetitionFormalExamOutboxEntity();
    entity.setId(seq);
    entity.setSeq(seq);
    entity.setAccessSessionId(accessSessionId);
    entity.setPayloadJson(json);
    entity.setEventType("x");
    return entity;
  }
}
