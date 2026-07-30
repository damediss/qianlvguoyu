package com.guoyu.platform.certification.scoring;

import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class PracticalWebSocketTicketService {

  private final ConcurrentHashMap<String, Ticket> tickets = new ConcurrentHashMap<>();

  public String issue() {
    PlatformUserPrincipal user = SecurityContext.currentUser();
    String value = UUID.randomUUID().toString();
    tickets.put(value, new Ticket(
        user.getUserId(), user.getTenantId(), user.getPermissionCodes(), Instant.now().plusSeconds(60)));
    return value;
  }

  public Ticket consume(String value) {
    Ticket ticket = value == null ? null : tickets.remove(value);
    return ticket == null || ticket.expiresAt().isBefore(Instant.now()) ? null : ticket;
  }

  public record Ticket(Long userId, Long tenantId, List<String> permissions, Instant expiresAt) {
  }
}
