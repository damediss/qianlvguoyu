package com.guoyu.platform.competition.realtime;

import com.guoyu.platform.security.SecurityContext;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class CompetitionPracticalRealtimeTicketService {
  private final ConcurrentHashMap<String, Ticket> tickets = new ConcurrentHashMap<>();

  public String issue() {
    var principal = SecurityContext.currentUser();
    String value = UUID.randomUUID().toString();
    tickets.put(value, new Ticket(principal.getTenantId(), principal.getUserId(),
        principal.getPermissionCodes(), Instant.now().plusSeconds(120)));
    return value;
  }

  public Ticket consume(String value) {
    Ticket ticket = value == null ? null : tickets.remove(value);
    return ticket == null || ticket.expiresAt().isBefore(Instant.now()) ? null : ticket;
  }

  public record Ticket(Long tenantId, Long userId, List<String> permissions, Instant expiresAt) { }
}
