package com.guoyu.platform.auth.controller;

import com.guoyu.platform.auth.dto.LoginReq;
import com.guoyu.platform.auth.service.AuthApplicationService;
import com.guoyu.platform.auth.vo.CurrentUserVO;
import com.guoyu.platform.auth.vo.HomeVO;
import com.guoyu.platform.auth.vo.LoginVO;
import com.guoyu.platform.auth.vo.MenuVO;
import com.guoyu.platform.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

  private final AuthApplicationService authApplicationService;

  public AuthController(AuthApplicationService authApplicationService) {
    this.authApplicationService = authApplicationService;
  }

  @PostMapping("/login")
  public ApiResponse<LoginVO> login(@Valid @RequestBody LoginReq req) {
    return ApiResponse.ok(authApplicationService.login(req));
  }

  @PostMapping("/refresh")
  public ApiResponse<Void> refresh() {
    return ApiResponse.fail("501", "Refresh token storage is not implemented yet");
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout() {
    return ApiResponse.ok();
  }

  @GetMapping("/me")
  public ApiResponse<CurrentUserVO> me() {
    return ApiResponse.ok(authApplicationService.me());
  }

  @GetMapping("/menus")
  public ApiResponse<List<MenuVO>> menus() {
    return ApiResponse.ok(authApplicationService.menus());
  }

  @GetMapping("/home")
  public ApiResponse<HomeVO> home() {
    return ApiResponse.ok(authApplicationService.home());
  }
}
