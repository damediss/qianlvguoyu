package com.guoyu.platform.exam.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.exam.service.QuestionBankTreeApplicationService;
import com.guoyu.platform.exam.vo.QuestionBankTreeNodeVO;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 题库分类导航聚合树接口。
 *
 * <p>替代前端 QuestionBankPage 现在用的三次串行 lazy-load (workType → course → knowledge) ,
 * 一次性返回完整 6 级树,每个节点带 questionCount 递归题数,便于在分类导航上
 * 直接渲染题数 badge,不用二次请求。
 */
@Validated
@RestController
@RequestMapping("/exam")
@Slf4j
public class QuestionBankTreeController {

  private final QuestionBankTreeApplicationService service;

  public QuestionBankTreeController(QuestionBankTreeApplicationService service) {
    this.service = service;
  }

  /**
   * 获取题库分类导航全树 (工种 → 课程 → 知识点 L0-L3)。
   * 每个节点带 {@code questionCount} (递归汇总),便于前端渲染 badge。
   * 空租户返回 {@code []}。
   */
  @GetMapping("/question-bank-tree")
  @PreAuthorize("hasAuthority('exam:question:view')")
  public ApiResponse<List<QuestionBankTreeNodeVO>> getQuestionBankTree() {
    return ApiResponse.ok(service.buildTree());
  }
}
