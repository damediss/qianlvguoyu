package com.guoyu.platform.exam.zhenti.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.exam.zhenti.dto.ZhentiAppListReq;
import com.guoyu.platform.exam.zhenti.dto.ZhentiIdReq;
import com.guoyu.platform.exam.zhenti.service.ZhentiAppService;
import com.guoyu.platform.exam.zhenti.vo.ZhentiAppListVO;
import com.guoyu.platform.exam.zhenti.vo.ZhentiPreviewVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * App 端 A1/A2。
 *
 * <p>SecurityConfig 放行本条链路，登录态由 AppTokenAuthFilter 校验（与 /appsmart/** 同模式）；
 * 未带 itoken 时 service 层抛 401。
 */
@Validated
@RestController
@RequestMapping("/appzhenti")
@Slf4j
public class ZhentiAppController {

  private final ZhentiAppService appService;

  public ZhentiAppController(ZhentiAppService appService) {
    this.appService = appService;
  }

  @PostMapping("/list")
  public ApiResponse<ZhentiAppListVO> list(@Valid @RequestBody ZhentiAppListReq req) {
    return ApiResponse.ok(appService.list(req.examId()));
  }

  @PostMapping("/preview")
  public ApiResponse<ZhentiPreviewVO> preview(@Valid @RequestBody ZhentiIdReq req) {
    return ApiResponse.ok(appService.preview(req.id()));
  }
}
