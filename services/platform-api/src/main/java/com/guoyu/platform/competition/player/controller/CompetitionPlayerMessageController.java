package com.guoyu.platform.competition.player.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageQuery;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.competition.player.service.CompetitionPlayerMessageApplicationService;
import com.guoyu.platform.competition.player.vo.CompetitionPlayerMessageVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/competition/player/messages")
public class CompetitionPlayerMessageController {

  private final CompetitionPlayerMessageApplicationService service;

  public CompetitionPlayerMessageController(CompetitionPlayerMessageApplicationService service) {
    this.service = service;
  }

  @GetMapping
  public ApiResponse<PageResponse<CompetitionPlayerMessageVO>> page(@Valid PageQuery query) {
    return ApiResponse.ok(service.page(query));
  }

  @GetMapping("/unread-count")
  public ApiResponse<Long> unreadCount() {
    return ApiResponse.ok(service.unreadCount());
  }

  @PostMapping("/{messageId}/read")
  public ApiResponse<Void> markRead(@PathVariable Long messageId) {
    service.markRead(messageId);
    return ApiResponse.ok();
  }

  @GetMapping("/{messageId}/assets/{assetId}")
  public ResponseEntity<Resource> getAsset(
      @PathVariable Long messageId, @PathVariable Long assetId) {
    return service.downloadAsset(messageId, assetId);
  }
}
