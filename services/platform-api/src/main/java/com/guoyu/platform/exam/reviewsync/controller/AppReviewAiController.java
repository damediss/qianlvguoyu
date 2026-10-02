package com.guoyu.platform.exam.reviewsync.controller;

import com.guoyu.platform.app.api.LegacyAppResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.reviewsync.service.AiGradeApplicationService;
import com.guoyu.platform.exam.reviewsync.service.AiGradeApplicationService.SubjectiveGradeResult;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.util.LinkedHashMap;
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
 * 主观题 AI 判分 App 接口（/appreviewai/**）—— 《简答题作答与AI判分方案》§3.2 / §5-4。
 *
 * <p>供 PHP 练习链路前端直连（saasPost Bearer 鉴权）；SaaS 链 answer/mock 的内部判分
 * 直接调 {@link AiGradeApplicationService}，不经本接口。鉴权与响应约定同
 * {@code AppReviewSyncController}：不加 SecurityConfig 白名单（anyRequest 兜底 401）、
 * 不用 @PreAuthorize、userId 只取 principal、tenantId 取 TenantContextHolder（本前缀被
 * TenantContextFilter 强制 10159）；响应 LegacyAppResponse（int code，0=成功），业务异常 code=1。
 */
@Slf4j
@RestController
@RequestMapping("/appreviewai")
public class AppReviewAiController {

  private static final int MAX_TEXT_LENGTH = 2000;

  private final AiGradeApplicationService gradeService;

  public AppReviewAiController(AiGradeApplicationService gradeService) {
    this.gradeService = gradeService;
  }

  /** 单题 AI 判分（同步，前端 timeout 30s）；无参考答案/AI 失败/限流 → code=1，前端转自评兜底 */
  @PostMapping("/grade")
  public LegacyAppResponse grade(@RequestBody Map<String, Object> body) {
    long userId = requireUserId();
    long questionId = longValue(body.get("questionId"), 0);
    String stem = str(body.get("stem"));
    String referenceAnswer = str(body.get("referenceAnswer"));
    String userAnswer = str(body.get("userAnswer"));
    String questionType = str(body.get("questionType")).trim();
    int fullScore = intValue(body.get("score"));

    if (questionId <= 0) {
      throw new BusinessException("400", "questionId 不能为空");
    }
    if (!AiGradeApplicationService.isSubjective(questionType)) {
      throw new BusinessException("400", "不支持判分的题型");
    }
    if (stem.isBlank() || referenceAnswer.isBlank() || userAnswer.isBlank()) {
      throw new BusinessException("400", "题干、参考答案、作答内容不能为空");
    }
    if (stem.length() > MAX_TEXT_LENGTH
        || referenceAnswer.length() > MAX_TEXT_LENGTH
        || userAnswer.length() > MAX_TEXT_LENGTH) {
      throw new BusinessException("400", "题干、参考答案、作答内容各不能超过 2000 字");
    }

    SubjectiveGradeResult result = gradeService.grade(
        TenantContextHolder.getTenantId(), userId, questionId, stem, referenceAnswer, userAnswer,
        questionType, fullScore);
    if (!result.gradable()) {
      // 无参考答案：跳过 AI 判分，前端走自评兜底（Q4-A）
      return LegacyAppResponse.fail(1, "该题暂无参考答案，请自评");
    }
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("correct", result.correct() ? 1 : 0);
    data.put("score", result.score());
    data.put("comment", result.comment());
    data.put("gradeSource", result.gradeSource());
    return LegacyAppResponse.ok("成功", data);
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

  private static String str(Object value) {
    return value != null ? String.valueOf(value) : "";
  }

  @ExceptionHandler(BusinessException.class)
  public LegacyAppResponse handleBusiness(BusinessException ex) {
    return LegacyAppResponse.fail(1, ex.getMessage());
  }

  @ExceptionHandler(Exception.class)
  public LegacyAppResponse handleOther(Exception ex) {
    log.error("[AI判分] 接口异常", ex);
    return LegacyAppResponse.fail(1, "服务繁忙，请稍后重试");
  }
}
