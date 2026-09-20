package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.competition.realtime.CompetitionPracticalRealtimeTicketService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/competition/practical-realtime")
public class CompetitionPracticalRealtimeController {
  private final CompetitionPracticalRealtimeTicketService service;

  public CompetitionPracticalRealtimeController(
      CompetitionPracticalRealtimeTicketService service) {
    this.service = service;
  }

  @PostMapping("/ticket")
  @PreAuthorize("hasAnyAuthority('competition:practical-judge:score', 'competition:practical-console:view')")
  public ApiResponse<String> ticket() { return ApiResponse.ok(service.issue()); }
}
