package com.guoyu.platform.competition.exam.realtime;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.competition.entity.CompetitionFormalExamSessionEntity;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamSessionMapper;
import com.guoyu.platform.competition.vo.CompetitionFormalExamConsoleRealtimeTicketVO;
import com.guoyu.platform.competition.vo.FormalExamRealtimeCatchUpVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FormalExamConsoleRealtimeApplicationService {

  public record BoundConsole(
      Long tenantId,
      Long userId,
      Long examSessionId) {
  }

  private final FormalExamWsTicketService ticketService;
  private final FormalExamRealtimeProperties properties;
  private final CompetitionFormalExamSessionMapper sessionMapper;
  private final FormalExamRealtimeCatchUpService catchUpService;
  private final Clock clock;

  public FormalExamConsoleRealtimeApplicationService(
      FormalExamWsTicketService ticketService,
      FormalExamRealtimeProperties properties,
      CompetitionFormalExamSessionMapper sessionMapper,
      FormalExamRealtimeCatchUpService catchUpService,
      Clock clock) {
    this.ticketService = ticketService;
    this.properties = properties;
    this.sessionMapper = sessionMapper;
    this.catchUpService = catchUpService;
    this.clock = clock;
  }

  public CompetitionFormalExamConsoleRealtimeTicketVO issueTicket(Long examSessionId) {
    if (!properties.isEnabled()) {
      throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "实时通道暂时不可用");
    }
    PlatformUserPrincipal user = SecurityContext.currentUser();
    CompetitionFormalExamSessionEntity session = requireSession(user.getTenantId(), examSessionId);
    Instant expiresAt = Instant.now(clock).plusSeconds(ticketService.ttlSeconds());
    String ticket = ticketService.issueConsole(new FormalExamWsTicketService.ConsoleTicket(
        user.getTenantId(),
        user.getUserId(),
        session.getId(),
        expiresAt));
    return new CompetitionFormalExamConsoleRealtimeTicketVO(
        ticket,
        ticketService.ttlSeconds(),
        properties.getConsoleWsPath(),
        properties.getHeartbeatSeconds());
  }

  public FormalExamRealtimeCatchUpVO catchUp(Long examSessionId, String afterSeq, Integer limit) {
    PlatformUserPrincipal user = SecurityContext.currentUser();
    CompetitionFormalExamSessionEntity session = requireSession(user.getTenantId(), examSessionId);
    return catchUpService.catchUp(
        session.getTenantId(),
        session.getId(),
        afterSeq,
        limit,
        FormalExamRealtimeCatchUpService.Viewer.CONSOLE,
        null,
        null);
  }

  public BoundConsole bind(String ticketValue) {
    FormalExamWsTicketService.ConsoleTicket ticket = ticketService.consumeConsole(ticketValue);
    if (ticket == null) {
      return null;
    }
    Long previous = TenantContextHolder.getTenantId();
    try {
      TenantContextHolder.setTenantId(ticket.tenantId());
      CompetitionFormalExamSessionEntity session = sessionMapper.selectById(ticket.examSessionId());
      if (session == null
          || !Objects.equals(session.getId(), ticket.examSessionId())
          || !Objects.equals(session.getTenantId(), ticket.tenantId())) {
        log.warn("Formal exam console WS ticket rejected: session mismatch, userId={}", ticket.userId());
        return null;
      }
      return new BoundConsole(ticket.tenantId(), ticket.userId(), session.getId());
    } finally {
      if (previous == null) {
        TenantContextHolder.clear();
      } else {
        TenantContextHolder.setTenantId(previous);
      }
    }
  }

  private CompetitionFormalExamSessionEntity requireSession(Long tenantId, Long examSessionId) {
    if (examSessionId == null || tenantId == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "正式比赛运行实例不存在");
    }
    CompetitionFormalExamSessionEntity session = sessionMapper.selectById(examSessionId);
    if (session == null || !Objects.equals(session.getTenantId(), tenantId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "正式比赛运行实例不存在");
    }
    return session;
  }
}
