package com.guoyu.platform.exam.reviewsync.service;

import com.guoyu.platform.exam.reviewsync.dto.ReviewQuestion;
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

  /** 答案选项（exam_exam_detail.answer JSON 元素；score 为该正确选项的分值，PHP 数据有字符串形态） */
  public record AnswerOption(String content, boolean correct, java.math.BigDecimal score) {

    public AnswerOption(String content, boolean correct) {
      this(content, correct, null);
    }
  }

  /** 判分结果 */
  public record JudgeResult(boolean correct, String correctAnswerText, java.math.BigDecimal earnedScore) {

    public JudgeResult(boolean correct, String correctAnswerText) {
      this(correct, correctAnswerText, null);
    }
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

  /**
   * 模考严格多选判分（O7，sys_dict ai_review_mock_strict_judge=1 时启用）：
   * <ul>
   *   <li>错选（选中任一 !isCorrect 选项）→ 0 分、计错；</li>
   *   <li>全选对（选齐全部正确项且无错选）→ 满分、计对；</li>
   *   <li>少选（未错选但未选齐）→ 部分分 = Σ选中正确项的 score、计错（进错题本）；
   *       若该题选项 score 数据不可用（缺失/非数值/全为 0），回退"少选不得分"。</li>
   * </ul>
   */
  public static JudgeResult judgeStrictMulti(List<AnswerOption> options, List<String> userMulti,
      java.math.BigDecimal questionScore) {
    List<String> selected = userMulti != null ? userMulti : List.of();
    // 错选即 0 分
    for (AnswerOption option : options) {
      if (!option.correct() && selected.contains(option.content())) {
        return new JudgeResult(false, correctText(options), java.math.BigDecimal.ZERO);
      }
    }
    // 统计正确项覆盖与部分分
    int correctTotal = 0;
    int correctHit = 0;
    java.math.BigDecimal earned = java.math.BigDecimal.ZERO;
    boolean scoreDataUsable = true;
    for (AnswerOption option : options) {
      if (!option.correct()) {
        continue;
      }
      correctTotal++;
      if (option.score() == null) {
        scoreDataUsable = false;
      }
      if (selected.contains(option.content())) {
        correctHit++;
        if (option.score() != null) {
          earned = earned.add(option.score());
        }
      }
    }
    if (correctHit == correctTotal && correctTotal > 0) {
      // 全对：按题分给满分（不依赖选项 score 数据质量）
      return new JudgeResult(true, correctText(options),
          questionScore != null ? questionScore : earned);
    }
    // 少选：选项 score 不可用（缺失/非数值/全 0）→ 少选不得分
    if (!scoreDataUsable || earned.compareTo(java.math.BigDecimal.ZERO) <= 0) {
      return new JudgeResult(false, correctText(options), java.math.BigDecimal.ZERO);
    }
    return new JudgeResult(false, correctText(options), earned);
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
