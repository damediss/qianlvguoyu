package com.guoyu.platform.competition.player.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.competition.player.dto.CompetitionPlayerActivationSendReq;
import com.guoyu.platform.competition.player.dto.CompetitionPlayerActivationVerifyReq;
import com.guoyu.platform.competition.player.dto.CompetitionPlayerLoginReq;
import com.guoyu.platform.competition.player.dto.CompetitionPlayerChangeMobileSendNewCodeReq;
import com.guoyu.platform.competition.player.dto.CompetitionPlayerChangeMobileVerifyReq;
import com.guoyu.platform.competition.player.service.CompetitionPlayerApplicationService;
import com.guoyu.platform.competition.player.vo.CompetitionPlayerEntryVO;
import com.guoyu.platform.competition.player.vo.CompetitionPlayerLoginResultVO;
import com.guoyu.platform.competition.player.vo.CompetitionPlayerMeVO;
import com.guoyu.platform.competition.player.vo.CompetitionPlayerSessionVO;
import com.guoyu.platform.competition.player.vo.CompetitionPlayerSmsChallengeVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/competition/player")
@Slf4j
public class CompetitionPlayerAuthController {

  private final CompetitionPlayerApplicationService service;

  public CompetitionPlayerAuthController(CompetitionPlayerApplicationService service) {
    this.service = service;
  }

  @GetMapping("/entry/resolve")
  public ApiResponse<CompetitionPlayerEntryVO> resolveEntry(@RequestParam String entry) {
    return ApiResponse.ok(service.resolveEntry(entry));
  }

  @PostMapping("/activation/send-code")
  public ApiResponse<CompetitionPlayerSmsChallengeVO> sendActivationCode(
      @Valid @RequestBody CompetitionPlayerActivationSendReq request,
      HttpServletRequest servletRequest) {
    return ApiResponse.ok(service.sendActivationCode(request, servletRequest));
  }

  @PostMapping("/activation/verify")
  public ApiResponse<CompetitionPlayerSessionVO> verifyActivation(
      @Valid @RequestBody CompetitionPlayerActivationVerifyReq request) {
    return ApiResponse.ok(service.verifyActivation(request));
  }

  @PostMapping("/auth/login")
  public ApiResponse<CompetitionPlayerLoginResultVO> login(
      @Valid @RequestBody CompetitionPlayerLoginReq request) {
    return ApiResponse.ok(service.login(request));
  }

  @GetMapping("/auth/me")
  public ApiResponse<CompetitionPlayerMeVO> me() {
    return ApiResponse.ok(service.me());
  }

  @PostMapping("/auth/logout")
  public ApiResponse<Void> logout() {
    service.logout();
    return ApiResponse.ok();
  }

  @PostMapping("/auth/change-mobile/send-old-code")
  public ApiResponse<CompetitionPlayerSmsChallengeVO> sendOldMobileChangeCode(
      HttpServletRequest servletRequest) {
    return ApiResponse.ok(service.sendOldMobileChangeCode(servletRequest));
  }

  @PostMapping("/auth/change-mobile/send-new-code")
  public ApiResponse<CompetitionPlayerSmsChallengeVO> sendNewMobileChangeCode(
      @Valid @RequestBody CompetitionPlayerChangeMobileSendNewCodeReq request,
      HttpServletRequest servletRequest) {
    return ApiResponse.ok(service.sendNewMobileChangeCode(request, servletRequest));
  }

  @PostMapping("/auth/change-mobile/verify")
  public ApiResponse<CompetitionPlayerSessionVO> verifyMobileChange(
      @Valid @RequestBody CompetitionPlayerChangeMobileVerifyReq request) {
    return ApiResponse.ok(service.verifyMobileChange(request));
  }
}
