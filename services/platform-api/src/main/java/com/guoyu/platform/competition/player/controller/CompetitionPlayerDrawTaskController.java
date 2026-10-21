package com.guoyu.platform.competition.player.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.competition.player.service.CompetitionPlayerDrawTaskApplicationService;
import com.guoyu.platform.competition.player.vo.CompetitionPlayerDrawTaskItemVO;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/competition/player/draw-tasks")
@PreAuthorize("hasRole('COMPETITION_PLAYER')")
public class CompetitionPlayerDrawTaskController {

  private final CompetitionPlayerDrawTaskApplicationService service;

  public CompetitionPlayerDrawTaskController(CompetitionPlayerDrawTaskApplicationService service) {
    this.service = service;
  }

  @GetMapping
  public ApiResponse<List<CompetitionPlayerDrawTaskItemVO>> listMine() {
    return ApiResponse.ok(service.listMine());
  }

  @PostMapping("/{taskId}/draw")
  @AuditLog(module = "competition-player", action = "draw-task:draw",
      targetType = "competition_player_draw_task")
  public ApiResponse<CompetitionPlayerDrawTaskItemVO> draw(@PathVariable Long taskId) {
    return ApiResponse.ok(service.draw(taskId));
  }
}
