package com.guoyu.platform.competition.player.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.competition.player.service.CompetitionPlayerScoreApplicationService;
import com.guoyu.platform.competition.player.vo.CompetitionPlayerEventResultVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/competition/player/events")
public class CompetitionPlayerScoreController {

  private final CompetitionPlayerScoreApplicationService service;

  public CompetitionPlayerScoreController(CompetitionPlayerScoreApplicationService service) {
    this.service = service;
  }

  @GetMapping("/{eventId}/result")
  public ApiResponse<CompetitionPlayerEventResultVO> getEventResult(@PathVariable Long eventId) {
    return ApiResponse.ok(service.getEventResult(eventId));
  }
}
