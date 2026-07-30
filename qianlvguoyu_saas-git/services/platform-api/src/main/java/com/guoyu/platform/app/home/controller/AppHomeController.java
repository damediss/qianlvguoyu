package com.guoyu.platform.app.home.controller;

import com.guoyu.platform.app.api.LegacyAppResponse;
import com.guoyu.platform.app.home.service.AppHomeApplicationService;
import com.guoyu.platform.app.sms.AppSmsException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * App 首页接口（对齐 PHP ApphomeController）。
 *
 * <p>5 个接口：index / tabs / home / search / articleinfo
 */
@RestController
@RequestMapping("/apphome")
@Slf4j
public class AppHomeController {

  private final AppHomeApplicationService homeService;

  public AppHomeController(AppHomeApplicationService homeService) {
    this.homeService = homeService;
  }

  /** 1. 首页聚合：轮播图 + 置顶资讯 + 文章列表 */
  @PostMapping("/index")
  public LegacyAppResponse index(@RequestBody(required = false) Map<String, Object> body) {
    return LegacyAppResponse.ok("成功", homeService.index());
  }

  /** 2. 首页 tabs 配置 */
  @PostMapping("/tabs")
  public LegacyAppResponse tabs(@RequestBody(required = false) Map<String, Object> body) {
    return LegacyAppResponse.ok("成功", homeService.tabs());
  }

  /** 3. 按 tag 返回栏目内容 */
  @PostMapping("/home")
  public LegacyAppResponse home(@RequestBody(required = false) Map<String, Object> body) {
    String tag = body != null && body.get("tag") != null
        ? String.valueOf(body.get("tag")) : "tuijian";
    return LegacyAppResponse.ok("成功", homeService.home(tag));
  }

  /** 4. 搜索文章 */
  @PostMapping("/search")
  public LegacyAppResponse search(@RequestBody(required = false) Map<String, Object> body) {
    String keyword = body != null && body.get("keyword") != null
        ? String.valueOf(body.get("keyword")) : "";
    int page = 1;
    if (body != null && body.get("page") != null) {
      try { page = Integer.parseInt(String.valueOf(body.get("page"))); }
      catch (NumberFormatException ignore) { page = 1; }
    }
    return LegacyAppResponse.ok("成功", homeService.search(keyword, page));
  }

  /** 5. 文章详情 */
  @PostMapping("/articleinfo")
  public LegacyAppResponse articleInfo(@RequestBody(required = false) Map<String, Object> body) {
    Integer articleId = null;
    if (body != null && body.get("article_id") != null) {
      try { articleId = Integer.parseInt(String.valueOf(body.get("article_id"))); }
      catch (NumberFormatException ignore) { }
    }
    return LegacyAppResponse.ok("成功", homeService.articleInfo(articleId));
  }

  @ExceptionHandler(AppSmsException.class)
  public LegacyAppResponse handle(AppSmsException ex) {
    return LegacyAppResponse.fail(ex.getCode(), ex.getMessage());
  }

  @ExceptionHandler(Exception.class)
  public LegacyAppResponse handleAny(Exception ex) {
    log.error("[apphome] 未处理异常", ex);
    return LegacyAppResponse.fail(500, "服务异常：" + ex.getMessage());
  }
}
