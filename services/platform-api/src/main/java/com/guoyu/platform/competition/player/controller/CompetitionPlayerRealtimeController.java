package com.guoyu.platform.competition.player.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.competition.player.realtime.CompetitionPlayerRealtimeTicketService;
import com.guoyu.platform.competition.player.security.CompetitionPlayerPrincipal;
import com.guoyu.platform.competition.player.security.CompetitionPlayerSecurityContext;
import com.guoyu.platform.competition.player.vo.CompetitionPlayerRealtimeTicketVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/competition/player/realtime")
public class CompetitionPlayerRealtimeController {

  private final CompetitionPlayerRealtimeTicketService ticketService;

  public CompetitionPlayerRealtimeController(CompetitionPlayerRealtimeTicketService ticketService) {
    this.ticketService = ticketService;
  }

  @PostMapping("/ticket")
  public ApiResponse<CompetitionPlayerRealtimeTicketVO> issueTicket() {
    CompetitionPlayerPrincipal player = CompetitionPlayerSecurityContext.currentPlayer();
    String ticket = ticketService.issue(player.getTenantId(), player.getPlayerId());
    return ApiResponse.ok(new CompetitionPlayerRealtimeTicketVO(ticket, ticketService.ttlSeconds()));
  }
}
