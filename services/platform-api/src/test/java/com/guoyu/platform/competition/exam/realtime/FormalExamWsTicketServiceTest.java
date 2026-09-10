package com.guoyu.platform.competition.exam.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.guoyu.platform.common.exception.BusinessException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class FormalExamWsTicketServiceTest {

  private final StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
  private final ValueOperations<String, String> values = Mockito.mock(ValueOperations.class);
  private final FormalExamRealtimeProperties properties = new FormalExamRealtimeProperties();
  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
  private FormalExamWsTicketService service;

  @BeforeEach
  void setUp() {
    when(redis.opsForValue()).thenReturn(values);
    service = new FormalExamWsTicketService(redis, objectMapper, properties);
  }

  @Test
  void consumeDeletesTicketAndRejectsReuse() throws Exception {
    FormalExamWsTicketService.Ticket ticket = new FormalExamWsTicketService.Ticket(
        1L, 77L, 100L, 2, 9001L, 9101L, Instant.now().plusSeconds(45));
    String json = objectMapper.writeValueAsString(ticket);
    when(values.getAndDelete(anyString())).thenReturn(json).thenReturn(null);

    FormalExamWsTicketService.Ticket consumed = service.consume("ticket-1");
    assertThat(consumed).isNotNull();
    assertThat(consumed.accessSessionId()).isEqualTo(100L);
    assertThat(consumed.generation()).isEqualTo(2);
    assertThat(consumed.playerId()).isEqualTo(77L);
    assertThat(service.consume("ticket-1")).isNull();
  }

  @Test
  void expiredTicketIsRejected() throws Exception {
    FormalExamWsTicketService.Ticket ticket = new FormalExamWsTicketService.Ticket(
        1L, 77L, 100L, 2, 9001L, 9101L, Instant.now().minusSeconds(5));
    when(values.getAndDelete(anyString())).thenReturn(objectMapper.writeValueAsString(ticket));

    assertThat(service.consume("stale")).isNull();
  }

  @Test
  void issueWithoutRedisReturnsUnavailable() {
    FormalExamWsTicketService missingRedis =
        new FormalExamWsTicketService(null, objectMapper, properties);

    assertThatThrownBy(() -> missingRedis.issue(new FormalExamWsTicketService.Ticket(
        1L, 77L, 100L, 2, 9001L, 9101L, Instant.now().plusSeconds(45))))
        .isInstanceOf(BusinessException.class)
        .hasMessage("实时通道暂时不可用")
        .extracting("code")
        .isEqualTo("503");
  }

  @Test
  void issueStoresOnceWithTtl() {
    when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

    String ticket = service.issue(new FormalExamWsTicketService.Ticket(
        1L, 77L, 100L, 2, 9001L, 9101L, Instant.now().plusSeconds(45)));

    assertThat(ticket).isNotBlank();
    verify(values).setIfAbsent(
        eq(FormalExamRealtimeTypes.TICKET_KEY_PREFIX + ticket),
        anyString(),
        eq(Duration.ofSeconds(45)));
    verify(values, never()).getAndDelete(anyString());
  }

  @Test
  void consoleTicketIsConsumedOnceAndNotInterchangeableWithPlayerTicket() throws Exception {
    FormalExamWsTicketService.ConsoleTicket ticket = new FormalExamWsTicketService.ConsoleTicket(
        1L, 1000L, 9001L, Instant.now().plusSeconds(45));
    String json = objectMapper.writeValueAsString(ticket);
    when(values.getAndDelete(anyString())).thenReturn(json).thenReturn(null);

    FormalExamWsTicketService.ConsoleTicket consumed = service.consumeConsole("console-1");
    assertThat(consumed).isNotNull();
    assertThat(consumed.examSessionId()).isEqualTo(9001L);
    assertThat(consumed.userId()).isEqualTo(1000L);
    assertThat(service.consumeConsole("console-1")).isNull();
    verify(values, times(2)).getAndDelete(eq(FormalExamRealtimeTypes.CONSOLE_TICKET_KEY_PREFIX + "console-1"));

    FormalExamWsTicketService.Ticket player = new FormalExamWsTicketService.Ticket(
        1L, 77L, 100L, 2, 9001L, 9101L, Instant.now().plusSeconds(45));
    when(values.getAndDelete(eq(FormalExamRealtimeTypes.CONSOLE_TICKET_KEY_PREFIX + "player-1")))
        .thenReturn(objectMapper.writeValueAsString(player));
    assertThat(service.consumeConsole("player-1")).isNull();
  }

  @Test
  void issueConsoleStoresOnceWithDistinctPrefix() {
    when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

    String ticket = service.issueConsole(new FormalExamWsTicketService.ConsoleTicket(
        1L, 1000L, 9001L, Instant.now().plusSeconds(45)));

    assertThat(ticket).isNotBlank();
    verify(values).setIfAbsent(
        eq(FormalExamRealtimeTypes.CONSOLE_TICKET_KEY_PREFIX + ticket),
        anyString(),
        eq(Duration.ofSeconds(45)));
  }
}
