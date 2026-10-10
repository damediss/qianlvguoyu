package com.guoyu.platform.competition.player.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.competition.player.service.CompetitionPlayerTheoryScheduleApplicationService;
import com.guoyu.platform.competition.vo.CompetitionPlayerTheoryScheduleVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/competition/player/theory-schedules")
public class CompetitionPlayerTheoryScheduleController {

  private final CompetitionPlayerTheoryScheduleApplicationService service;

  public CompetitionPlayerTheoryScheduleController(
      CompetitionPlayerTheoryScheduleApplicationService service) {
    this.service = service;
  }

  @GetMapping
  public ApiResponse<List<CompetitionPlayerTheoryScheduleVO>> listMine() {
    return ApiResponse.ok(service.listMine());
  }
}
