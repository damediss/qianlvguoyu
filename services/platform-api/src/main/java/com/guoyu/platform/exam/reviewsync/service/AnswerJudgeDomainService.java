package com.guoyu.platform.exam.reviewsync.service;

import com.guoyu.platform.integration.php.dto.PhpQuestion;
import java.util.List;

/**
 * 判分（§5.6.5，与 PHP trainsave 前端判分口径逐行对齐 —— PHP 端无判分逻辑，
 * get_score 由 sortExam.vue doSave 计算，规则如下）。
 *
 * <pre>
 * 单选/判断：正确选项 content == 用户答案 → 全对得分
 * 多选：错选（选中任一 !isCorrect 选项）→ 0 分；未错选（含少选）→ 满分
 *      （PHP 训练口径无"少选部分分"，实现方案开放问题 O7 记录此差异）
 * </pre>
 *
 * user_answer 存储口径（与 PHP exam_result 一致）：单/判 = 选项内容字符串；多选 = 内容数组 JSON。
 */
public final class AnswerJudgeDomainService {

  private AnswerJudgeDomainService() {
  }

  /** 答案选项（exam_exam_detail.answer JSON 元素） */
  public record AnswerOption(String content, boolean correct) {
  }

  /** 判分结果 */
  public record JudgeResult(boolean correct, String correctAnswerText) {
  }

  /**
   * @param questionType single/multi/judge/其他
   * @param options      题目答案选项（含 isCorrect 标记）
   * @param userSingle   单/判：用户选中的选项内容（trim 后）
   * @param userMulti    多选：用户选中的选项内容列表
   */
  public static JudgeResult judge(String questionType, List<AnswerOption> options,
      String userSingle, List<String> userMulti) {
    if ("multi".equals(questionType)) {
      boolean wrong = false;
      for (AnswerOption option : options) {
        if (!option.correct() && userMulti != null && userMulti.contains(option.content())) {
          wrong = true;
          break;
        }
      }
      return new JudgeResult(!wrong, correctText(options));
    }
    // single / judge：正确选项内容必须与用户答案一致
    String correctContent = null;
    for (AnswerOption option : options) {
      if (option.correct()) {
        correctContent = option.content();
        break;
      }
    }
    boolean correct = correctContent != null && correctContent.equals(userSingle);
    return new JudgeResult(correct, correctText(options));
  }

  private static String correctText(List<AnswerOption> options) {
    StringBuilder sb = new StringBuilder();
    for (AnswerOption option : options) {
      if (option.correct()) {
        if (sb.length() > 0) {
          sb.append("、");
        }
        sb.append(option.content());
      }
    }
    return sb.toString();
  }
}
