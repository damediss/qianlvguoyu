package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.competition.dto.CompetitionNoticeCreateReq;
import com.guoyu.platform.competition.dto.CompetitionNoticeQueryReq;
import com.guoyu.platform.competition.dto.CompetitionNoticeUpdateReq;
import com.guoyu.platform.competition.service.CompetitionNoticeApplicationService;
import com.guoyu.platform.competition.vo.CompetitionNoticePublishResultVO;
import com.guoyu.platform.competition.vo.CompetitionNoticeVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/competition")
public class CompetitionNoticeController {

  private final CompetitionNoticeApplicationService service;

  public CompetitionNoticeController(CompetitionNoticeApplicationService service) {
    this.service = service;
  }

  @PostMapping("/events/{eventId}/notices")
  @PreAuthorize("hasAuthority('competition:notice:publish')")
  @AuditLog(module = "competition", action = "notice:create", targetType = "competition_notice")
  public ApiResponse<CompetitionNoticeVO> create(
      @PathVariable Long eventId,
      @Valid @RequestBody CompetitionNoticeCreateReq req) {
    return ApiResponse.ok(service.create(eventId, req));
  }

  @GetMapping("/events/{eventId}/notices")
  @PreAuthorize("hasAuthority('competition:notice:view')")
  public ApiResponse<PageResponse<CompetitionNoticeVO>> page(
      @PathVariable Long eventId,
      @Valid @ModelAttribute CompetitionNoticeQueryReq req) {
    return ApiResponse.ok(service.page(eventId, req));
  }

  @GetMapping("/notices/{noticeId}")
  @PreAuthorize("hasAuthority('competition:notice:view')")
  public ApiResponse<CompetitionNoticeVO> getById(@PathVariable Long noticeId) {
    return ApiResponse.ok(service.getById(noticeId));
  }

  @PutMapping("/notices/{noticeId}")
  @PreAuthorize("hasAuthority('competition:notice:publish')")
  @AuditLog(module = "competition", action = "notice:update", targetType = "competition_notice")
  public ApiResponse<CompetitionNoticeVO> update(
      @PathVariable Long noticeId,
      @Valid @RequestBody CompetitionNoticeUpdateReq req) {
    return ApiResponse.ok(service.update(noticeId, req));
  }

  @PostMapping("/notices/{noticeId}/publish")
  @PreAuthorize("hasAuthority('competition:notice:publish')")
  @AuditLog(
      module = "competition",
      action = "notice:publish",
      targetType = "competition_notice",
      includeResult = true)
  public ApiResponse<CompetitionNoticePublishResultVO> publish(@PathVariable Long noticeId) {
    return ApiResponse.ok(service.publish(noticeId));
  }
}
