package com.guoyu.platform.app.exam.controller;

import com.guoyu.platform.app.api.LegacyAppResponse;
import com.guoyu.platform.app.exam.service.AppExamSmartService;
import com.guoyu.platform.app.sms.AppSmsException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
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

  public AppExamSmartController(AppExamSmartService smartService) {
    this.smartService = smartService;
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
  public LegacyAppResponse handle(AppSmsException ex) {
    return LegacyAppResponse.fail(ex.getCode(), ex.getMessage());
  }

  /** 兜底：把 NPE/RuntimeException 等也转成 App 友好响应，并在日志打印堆栈 */
  @ExceptionHandler(Exception.class)
  public LegacyAppResponse handleAny(Exception ex) {
    log.error("[appsmart] 未处理异常", ex);
    return LegacyAppResponse.fail(500, "服务异常：" + ex.getMessage());
  }
}
