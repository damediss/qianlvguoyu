package com.guoyu.platform.exam.theorygen.service;

import com.guoyu.platform.exam.theorygen.dto.TheoryQuestionItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * 客观题选项确定性洗牌。
 *
 * <p>背景（优化方案 §8.5 E2E 实测）：模型把正确答案集中放在靠前选项（A+B 占 80%、D 仅 3%），
 * prompt 禁令对 LLM 位置偏差天然无效——答案字母分布均匀属确定性规则，由后端洗牌保证
 * （AGENTS.md LLM 规范 #6：确定性规则不让位 LLM）。
 *
 * <p>约定：洗牌在 {@link TheoryQuestionValidator} 校验通过后执行；重排选项对象并按新位置
 * 重编 A/B/C/D… 标号，答案字母同步映射到新位置（多选题字母顺序保留）。
 * 零信任兜底：判断题/主观题无选项、选项不足、答案字母无法映射到选项时放弃洗牌原样返回。
 */
public final class TheoryOptionShuffler {

  private TheoryOptionShuffler() {
  }

  /** 批量洗牌（仅客观题生效；rnd 传入可复现种子便于测试） */
  public static void shuffle(List<TheoryQuestionItem> questions, RandomGenerator rnd) {
    for (TheoryQuestionItem q : questions) {
      shuffleOne(q, rnd);
    }
  }

  private static void shuffleOne(TheoryQuestionItem q, RandomGenerator rnd) {
    if (!"single".equals(q.getQuestionType()) && !"multi".equals(q.getQuestionType())) {
      return; // 判断题/主观题无选项
    }
    List<TheoryQuestionItem.Option> options = q.getOptionList();
    String answer = q.getAnswer() == null ? "" : q.getAnswer().trim().toUpperCase();
    if (options == null || options.size() < 2 || answer.isBlank()) {
      return;
    }
    // 旧字母 → 旧下标；答案字母必须全部可映射，否则放弃洗牌（防洗出孤儿答案）
    Map<String, Integer> indexByOldLabel = new HashMap<>();
    for (int i = 0; i < options.size(); i++) {
      String label = options.get(i).getLabel() == null ? "" : options.get(i).getLabel().trim().toUpperCase();
      indexByOldLabel.put(label, i);
    }
    List<Integer> answerOldIndexes = new ArrayList<>();
    for (int k = 0; k < answer.length(); k++) {
      Integer oldIdx = indexByOldLabel.get(String.valueOf(answer.charAt(k)));
      if (oldIdx == null) {
        return; // 零信任：答案字母无法映射，原样返回
      }
      answerOldIndexes.add(oldIdx);
    }
    // Fisher-Yates 洗牌
    List<TheoryQuestionItem.Option> shuffled = new ArrayList<>(options);
    for (int i = shuffled.size() - 1; i > 0; i--) {
      int j = rnd.nextInt(i + 1);
      Collections.swap(shuffled, i, j);
    }
    // 按新位置重编标号，并记录旧下标 → 新位置（Option 无 equals 重写，indexOf 为对象同一性）
    Map<Integer, Integer> oldIndexToNewPos = new HashMap<>();
    for (int newPos = 0; newPos < shuffled.size(); newPos++) {
      TheoryQuestionItem.Option option = shuffled.get(newPos);
      oldIndexToNewPos.put(options.indexOf(option), newPos);
      option.setLabel(String.valueOf((char) ('A' + newPos)));
    }
    StringBuilder newAnswer = new StringBuilder();
    for (int oldIdx : answerOldIndexes) {
      newAnswer.append((char) ('A' + oldIndexToNewPos.get(oldIdx)));
    }
    q.setOptions(shuffled);
    q.setAnswer(newAnswer.toString());
  }
}
