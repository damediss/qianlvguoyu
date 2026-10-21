package com.guoyu.platform.app.reviewsync.controller;

import com.guoyu.platform.app.api.LegacyAppResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.reviewsync.service.ReviewPlanApplicationService;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 同步学 App 接口（/appreviewsync/**）—— 《AI同步学（学员复习）实现方案》§7。
 *
 * <p>鉴权约定（鉴权方案 v1.3 §3.3；单库化 Q7）：
 * <ul>
 *   <li>不加 SecurityConfig 白名单——坏 token 由 EntryPoint 返 401；</li>
 *   <li>不用 {@code @PreAuthorize}（App token 的 authorities 为空）；</li>
 *   <li>userId 只取 {@link PlatformUserPrincipal}（不信任前端传的任何 userId）；</li>
 *   <li>tenantId 取 {@link TenantContextHolder}——TenantContextFilter 对本前缀强制 10159，
 *       忽略 X-Tenant-Id header（旧版 App 仍发 1，服务端兜底，App 发版改默认值前即可上线）。</li>
 * </ul>
 * 响应：App 风格 {@code {code:0, msg:"成功", data:{}}}；业务异常 code=1。
 */
@Slf4j
@RestController
@RequestMapping("/appreviewsync")
public class AppReviewSyncController {

  private final ReviewPlanApplicationService reviewService;

  public AppReviewSyncController(ReviewPlanApplicationService reviewService) {
    this.reviewService = reviewService;
  }

  /** 7.1 计划概览（懒生成入口） */
  @PostMapping("/overview")
  public LegacyAppResponse overview(@RequestBody Map<String, Object> body) {
    long userId = requireUserId();
    return LegacyAppResponse.ok("成功",
        reviewService.overview(userId, examId(body), TenantContextHolder.getTenantId()));
  }

  /** 7.2 今日任务清单 */
  @PostMapping("/today")
  public LegacyAppResponse today(@RequestBody Map<String, Object> body) {
    long userId = requireUserId();
    return LegacyAppResponse.ok("成功",
        reviewService.todayTask(userId, examId(body), TenantContextHolder.getTenantId()));
  }

  /** 7.3 题目详情（批量，≤50，剥离答案标记） */
  @PostMapping("/question")
  public LegacyAppResponse question(@RequestBody Map<String, Object> body) {
    long userId = requireUserId();
    List<Long> questionIds = longList(body.get("questionIds"));
    return LegacyAppResponse.ok("成功", reviewService.questions(userId, examId(body), questionIds));
  }

  /** 7.4 单题作答（主观题支持自评兜底入参 selfAssessed/selfCorrect、giveUp 主动放弃看解析，可选） */
  @PostMapping("/answer")
  public LegacyAppResponse answer(@RequestBody Map<String, Object> body) {
    long userId = requireUserId();
    return LegacyAppResponse.ok("成功", reviewService.answer(
        userId,
        examId(body),
        longValue(body.get("dayId"), 0),
        longValue(body.get("questionId"), 0),
        str(body.get("roundType")),
        str(body.get("userAnswer")),
        intValue(body.get("useTimeSec")),
        TenantContextHolder.getTenantId(),
        intValueOrNull(body.get("selfAssessed")),
        intValueOrNull(body.get("selfCorrect")),
        intValueOrNull(body.get("giveUp"))));
  }

  /** 7.5 开始模考 */
  @PostMapping("/mock/start")
  public LegacyAppResponse mockStart(@RequestBody Map<String, Object> body) {
    long userId = requireUserId();
    return LegacyAppResponse.ok("成功",
        reviewService.mockStart(userId, examId(body), TenantContextHolder.getTenantId()));
  }

  /** 7.6 模考交卷 */
  @PostMapping("/mock/submit")
  @SuppressWarnings("unchecked")
  public LegacyAppResponse mockSubmit(@RequestBody Map<String, Object> body) {
    long userId = requireUserId();
    List<Map<String, Object>> answers = body.get("answers") instanceof List<?> list
        ? (List<Map<String, Object>>) list : List.of();
    return LegacyAppResponse.ok("成功", reviewService.mockSubmit(
        userId,
        examId(body),
        longValue(body.get("mockPaperId"), 0),
        answers,
        intValue(body.get("totalUseTimeSec")),
        TenantContextHolder.getTenantId()));
  }

  /** 7.7 结束今日练习 */
  @PostMapping("/finish")
  public LegacyAppResponse finish(@RequestBody Map<String, Object> body) {
    long userId = requireUserId();
    return LegacyAppResponse.ok("成功", reviewService.finish(
        userId,
        examId(body),
        longValue(body.get("dayId"), 0),
        intValue(body.get("useTimeSec")),
        TenantContextHolder.getTenantId()));
  }

  /** 7.8 学情总览 */
  @PostMapping("/progress")
  public LegacyAppResponse progress(@RequestBody Map<String, Object> body) {
    long userId = requireUserId();
    return LegacyAppResponse.ok("成功",
        reviewService.progress(userId, examId(body), TenantContextHolder.getTenantId()));
  }

  /** D7 超前学习：今日任务完成后提前解锁明日任务（§15.1） */
  @PostMapping("/advance-day")
  public LegacyAppResponse advanceDay(@RequestBody Map<String, Object> body) {
    long userId = requireUserId();
    return LegacyAppResponse.ok("成功",
        reviewService.advanceDay(userId, examId(body), TenantContextHolder.getTenantId()));
  }

  /** D8 灰度准入预检（§15.2 配套）：App 菜单页据此决定是否渲染「同步练习」入口。本接口不做灰度拦截 */
  @PostMapping("/availability")
  public LegacyAppResponse availability(@RequestBody Map<String, Object> body) {
    long userId = requireUserId();
    return LegacyAppResponse.ok("成功",
        reviewService.grayAvailability(userId, examId(body), TenantContextHolder.getTenantId()));
  }

  // ------------------------------------------------------------------
  // 内部
  // ------------------------------------------------------------------

  /** 只信任 AppTokenAuthFilter 设置的 principal（前端传 userId 一律忽略） */
  private static long requireUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.getPrincipal() instanceof PlatformUserPrincipal principal
        && principal.getUserId() != null) {
      return principal.getUserId();
    }
    throw new BusinessException("401", "未登录或登录已过期");
  }

  private static long examId(Map<String, Object> body) {
    long examId = longValue(body.get("examId"), 0);
    if (examId <= 0) {
      throw new BusinessException("400", "examId 不能为空");
    }
    return examId;
  }

  private static long longValue(Object value, long defaultValue) {
    if (value == null) {
      return defaultValue;
    }
    try {
      return Long.parseLong(String.valueOf(value));
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  private static int intValue(Object value) {
    if (value == null) {
      return 0;
    }
    try {
      return Integer.parseInt(String.valueOf(value));
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  /** 可选 int 入参：缺失/非数值返回 null（调用方按"未传"处理） */
  private static Integer intValueOrNull(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return Integer.parseInt(String.valueOf(value));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static String str(Object value) {
    return value != null ? String.valueOf(value) : "";
  }

  private static List<Long> longList(Object value) {
    if (!(value instanceof List<?> list)) {
      return List.of();
    }
    return list.stream().map(item -> longValue(item, 0)).filter(id -> id > 0).toList();
  }

  @ExceptionHandler(BusinessException.class)
  public LegacyAppResponse handleBusiness(BusinessException ex) {
    return LegacyAppResponse.fail(1, ex.getMessage());
  }

  @ExceptionHandler(Exception.class)
  public LegacyAppResponse handleOther(Exception ex) {
    log.error("[AI复习] 接口异常", ex);
    return LegacyAppResponse.fail(1, "服务繁忙，请稍后重试");
  }
}
