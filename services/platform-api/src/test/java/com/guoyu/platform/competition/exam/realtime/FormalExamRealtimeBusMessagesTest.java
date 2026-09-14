package com.guoyu.platform.competition.exam.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FormalExamRealtimeBusMessagesTest {

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @Test
  void wrappedMessageSkipsOnSameNodeAndDeliversOnOtherNode() throws Exception {
    FormalExamRealtimeEnvelope envelope = new FormalExamRealtimeEnvelope(
        "e1", null, FormalExamRealtimeTypes.SESSION_STATUS_CHANGED,
        "9001", "9001", null, null, null, 1L, null, null,
        FormalExamRealtimeTypes.TARGET_SESSION, Map.of("status", "running"));
    String payload = objectMapper.writeValueAsString(envelope);
    String wrapped = FormalExamRealtimeBusMessages.wrap(objectMapper, "api-1", payload);
    FormalExamRealtimeBusMessages.Parsed parsed =
        FormalExamRealtimeBusMessages.parse(objectMapper, wrapped);
    assertThat(parsed.publisherNodeId()).isEqualTo("api-1");
    assertThat(parsed.envelope().eventId()).isEqualTo("e1");
    assertThat(FormalExamRealtimeBusMessages.shouldSkipLocalEcho("api-1", "api-1")).isTrue();
    assertThat(FormalExamRealtimeBusMessages.shouldSkipLocalEcho("api-1", "api-2")).isFalse();
    assertThat(FormalExamRealtimeBusMessages.shouldSkipLocalEcho(null, "api-1")).isFalse();
  }

  @Test
  void legacyPayloadWithoutPublisherNodeIdRemainsDeliverable() throws Exception {
    FormalExamRealtimeBusMessages.Parsed parsed = FormalExamRealtimeBusMessages.parse(
        objectMapper,
        "{\"eventId\":\"e1\",\"type\":\"session.status.changed\",\"examSessionId\":\"9001\",\"seq\":\"3\"}");
    assertThat(parsed.publisherNodeId()).isNull();
    assertThat(parsed.envelope().eventId()).isEqualTo("e1");
    assertThat(parsed.envelope().seq()).isEqualTo(3L);
  }
}
