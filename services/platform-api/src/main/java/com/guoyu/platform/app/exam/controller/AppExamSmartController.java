package com.guoyu.platform.app.exam.controller;

import com.guoyu.platform.app.api.LegacyAppResponse;
import com.guoyu.platform.app.exam.service.AppExamSmartService;
import com.guoyu.platform.app.exam.service.QuestionResolveAssistService;
import com.guoyu.platform.app.sms.AppSmsException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * App 端智能学习接口：AI 组卷 / 模拟考 / 考点预测 / 错题重练。
 *
 * <p>对齐 PHP 旧 App 的 promote/* 学习接口风格，但走 Sa-Token 鉴权。
 */
@RestController
@RequestMapping("/appsmart")
@Slf4j
public class AppExamSmartController {

  private final AppExamSmartService smartService;
  private final QuestionResolveAssistService resolveAssistService;

  public AppExamSmartController(
      AppExamSmartService smartService,
      QuestionResolveAssistService resolveAssistService) {
    this.smartService = smartService;
    this.resolveAssistService = resolveAssistService;
  }

  /** 1. AI 智能组卷（按考点×难度分层抽题，生成一张模拟卷） */
  @PostMapping("/paper/generate")
  public LegacyAppResponse generatePaper(@RequestBody(required = false) Map<String, Object> body) {
    return LegacyAppResponse.ok("成功", smartService.generatePaper(body == null ? Map.of() : body));
  }

  /** 2. 随机练习（不强制及格分，即时反馈） */
  @PostMapping("/paper/random")
  public LegacyAppResponse randomPractice(@RequestBody(required = false) Map<String, Object> body) {
    return LegacyAppResponse.ok("成功", smartService.randomPractice(body == null ? Map.of() : body));
  }

  /** 3. 错题重练（把历史错题组成一张卷） */
  @PostMapping("/paper/wrong-replay")
  public LegacyAppResponse wrongReplay(@RequestBody(required = false) Map<String, Object> body) {
    return LegacyAppResponse.ok("成功", smartService.wrongReplay(body == null ? Map.of() : body));
  }

  /**
   * 3b. 精准学：按错题推送同考点+同题型相似题（mode=drill，答错即时看解析）
   */
  @PostMapping("/paper/similar-wrong")
  public LegacyAppResponse similarWrong(@RequestBody(required = false) Map<String, Object> body) {
    return LegacyAppResponse.ok("成功", smartService.similarWrongPractice(body == null ? Map.of() : body));
  }

  /** 4. 考点预测（基于答题历史分析薄弱考点） */
  @PostMapping("/knowledge/predict")
  public LegacyAppResponse knowledgePredict() {
    return LegacyAppResponse.ok("成功", smartService.knowledgePredict());
  }

  /** 5. 错题本（按考点/分页列出历史错题） */
  @PostMapping("/wrong/book")
  public LegacyAppResponse wrongBook(@RequestBody(required = false) Map<String, Object> body) {
    return LegacyAppResponse.ok("成功", smartService.wrongBook(body == null ? Map.of() : body));
  }

  /** 5b. 错题重练答对后移出错题本（内部复用单题进度） */
  @PostMapping("/wrong/resolve")
  public LegacyAppResponse resolveWrong(@RequestBody(required = false) Map<String, Object> body) {
    return LegacyAppResponse.ok("成功", smartService.resolveWrongOnCorrect(body == null ? Map.of() : body));
  }

  /** 5c. 练习单题进度：揭晓后即时写入，驱动本班覆盖实时更新 */
  @PostMapping("/paper/progress")
  public LegacyAppResponse paperProgress(@RequestBody(required = false) Map<String, Object> body) {
    return LegacyAppResponse.ok("成功", smartService.recordPaperProgress(body == null ? Map.of() : body));
  }

  /**
   * 5d. 题目解析：官方读取 → 缓存 → 按需 AI 生成（精准学/班级练习答错补解析）。
   * App 入口强制登录 + 每学员每日限流；匿名一律「请先登录」（401）。
   * body: { questionId }
   */
  @PostMapping("/question/resolve")
  public LegacyAppResponse ensureQuestionResolve(@RequestBody(required = false) Map<String, Object> body) {
    Object raw = body == null ? null : body.get("questionId");
    Integer questionId = null;
    if (raw instanceof Number n) {
      questionId = n.intValue();
    } else if (raw != null) {
      try {
        questionId = Integer.parseInt(String.valueOf(raw));
      } catch (NumberFormatException ignored) {
        // leave null
      }
    }
    return LegacyAppResponse.ok("成功", resolveAssistService.ensureResolveForApp(questionId));
  }

  /** AI 精准学 E-P0：首页四宫格摘要（聚合层，不改原错题/预测契约） */
  @PostMapping("/precise/overview")
  public LegacyAppResponse preciseOverview(@RequestBody(required = false) Map<String, Object> body) {
    return LegacyAppResponse.ok("成功", smartService.preciseOverview(body == null ? Map.of() : body));
  }

  /** 6. 试卷详情（含题目列表，未交卷不返回答案） */
  @PostMapping("/paper/detail/{paperId}")
  public LegacyAppResponse paperDetail(@PathVariable Integer paperId) {
    return LegacyAppResponse.ok("成功", smartService.paperDetail(paperId));
  }

  /** 7. 交卷判分（写答题明细 + 更新试卷状态 + 返回对错详情） */
  @PostMapping("/paper/submit/{paperId}")
  public LegacyAppResponse submitPaper(@PathVariable Integer paperId,
                                       @RequestBody(required = false) Map<String, Object> body) {
    return LegacyAppResponse.ok("成功", smartService.submitPaper(paperId, body == null ? Map.of() : body));
  }

  /** 8. 模拟考历史（按 mode 过滤，分页） */
  @PostMapping("/paper/history")
  public LegacyAppResponse paperHistory(@RequestBody(required = false) Map<String, Object> body) {
    return LegacyAppResponse.ok("成功", smartService.paperHistory(body == null ? Map.of() : body));
  }

  @ExceptionHandler(AppSmsException.class)
  public ResponseEntity<LegacyAppResponse> handle(AppSmsException ex) {
    LegacyAppResponse body = LegacyAppResponse.fail(ex.getCode(), ex.getMessage());
    // /appsmart 本身 permitAll，业务「请先登录」若仍返回 200，App saasRequest 不会走 401 退避/静默重登
    if (isLoginRequired(ex)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }
    return ResponseEntity.ok(body);
  }

  /** 兜底：把 NPE/RuntimeException 等也转成 App 友好响应，并在日志打印堆栈 */
  @ExceptionHandler(Exception.class)
  public LegacyAppResponse handleAny(Exception ex) {
    log.error("[appsmart] 未处理异常", ex);
    return LegacyAppResponse.fail(500, "服务异常：" + ex.getMessage());
  }

  private static boolean isLoginRequired(AppSmsException ex) {
    String msg = ex.getMessage();
    if (msg == null || msg.isBlank()) {
      return false;
    }
    return msg.contains("请先登录") || msg.contains("未登录") || msg.contains("登录已过期");
  }
}
