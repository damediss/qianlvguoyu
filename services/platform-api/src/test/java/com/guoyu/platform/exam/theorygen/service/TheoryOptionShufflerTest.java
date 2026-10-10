package com.guoyu.platform.exam.theorygen.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guoyu.platform.exam.theorygen.dto.TheoryQuestionItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 客观题选项洗牌单测：答案-内容映射保持、字母分布均匀、多选重映射、零信任兜底 */
class TheoryOptionShufflerTest {

  private TheoryQuestionItem single(String answer) {
    TheoryQuestionItem q = new TheoryQuestionItem();
    q.setQuestionType("single");
    q.setContent("测试题干（ ）。");
    q.setAnswer(answer);
    List<TheoryQuestionItem.Option> options = new ArrayList<>();
    for (char label = 'A'; label <= 'D'; label++) {
      TheoryQuestionItem.Option o = new TheoryQuestionItem.Option();
      o.setLabel(String.valueOf(label));
      o.setContent("选项内容" + label);
      options.add(o);
    }
    q.setOptions(options);
    return q;
  }

  private String contentOfCorrectOption(TheoryQuestionItem q) {
    for (TheoryQuestionItem.Option o : q.getOptionList()) {
      if (o.getLabel().equalsIgnoreCase(q.getAnswer())) {
        return o.getContent();
      }
    }
    throw new IllegalStateException("答案未命中任何选项");
  }

  /** 洗牌后答案字母仍指向同一内容，且标号恢复为 A~D 顺序 */
  @Test
  void answerStillPointsToSameContentAfterShuffle() {
    for (long seed = 0; seed < 200; seed++) {
      TheoryQuestionItem q = single("C");
      String correctContent = contentOfCorrectOption(q);
      TheoryOptionShuffler.shuffle(List.of(q), new Random(seed));
      String newAnswer = q.getAnswer();
      assertTrue(newAnswer.matches("^[A-D]$"), "新答案应为单字母: " + newAnswer);
      String mappedContent = contentOfCorrectOption(q);
      assertEquals(correctContent, mappedContent, "seed=" + seed + " 答案-内容映射被破坏");
      for (int i = 0; i < q.getOptionList().size(); i++) {
        assertEquals(String.valueOf((char) ('A' + i)), q.getOptionList().get(i).getLabel(),
            "seed=" + seed + " 标号未按新位置顺序重编");
      }
    }
  }

  /**
   * 分布均匀性：单个 Random 流连续洗 4000 次（与生产 ThreadLocalRandom 行为同构）。
   * 不用 new Random(seed) 顺序小种子——LCG 首轮输出有结构性规律，会制造伪偏差。
   */
  @Test
  void answerLetterDistributionIsUniform() {
    Map<String, Integer> counter = new HashMap<>();
    int runs = 4000;
    Random rnd = new Random(20260906L);
    for (int i = 0; i < runs; i++) {
      TheoryQuestionItem q = single("A");
      TheoryOptionShuffler.shuffle(List.of(q), rnd);
      counter.merge(q.getAnswer(), 1, Integer::sum);
    }
    for (String letter : List.of("A", "B", "C", "D")) {
      int count = counter.getOrDefault(letter, 0);
      assertTrue(count >= runs / 4 - runs / 20 && count <= runs / 4 + runs / 20,
          letter + " 档分布偏离 25%±5%: " + count + "/" + runs);
    }
  }

  /** 多选题：3 个正确答案字母整体重映射，正确内容集合保持一致 */
  @Test
  void multiAnswerLettersRemappedAsSet() {
    for (long seed = 0; seed < 200; seed++) {
      TheoryQuestionItem q = new TheoryQuestionItem();
      q.setQuestionType("multi");
      q.setContent("测试多选题干（ ）。");
      q.setAnswer("ABD");
      List<TheoryQuestionItem.Option> options = new ArrayList<>();
      for (char label = 'A'; label <= 'E'; label++) {
        TheoryQuestionItem.Option o = new TheoryQuestionItem.Option();
        o.setLabel(String.valueOf(label));
        o.setContent("选项内容" + label);
        options.add(o);
      }
      q.setOptions(options);
      Set<String> correctContents = new HashSet<>(Set.of("选项内容A", "选项内容B", "选项内容D"));

      TheoryOptionShuffler.shuffle(List.of(q), new Random(seed));

      String newAnswer = q.getAnswer();
      assertEquals(3, newAnswer.length(), "seed=" + seed + " 正确答案字母数改变");
      Set<String> mapped = new HashSet<>();
      for (char c : newAnswer.toCharArray()) {
        for (TheoryQuestionItem.Option o : q.getOptionList()) {
          if (o.getLabel().equals(String.valueOf(c))) {
            mapped.add(o.getContent());
          }
        }
      }
      assertEquals(correctContents, mapped, "seed=" + seed + " 多选正确内容集合被破坏");
    }
  }

  /** 判断题无选项：原样返回 */
  @Test
  void judgeUntouched() {
    TheoryQuestionItem q = new TheoryQuestionItem();
    q.setQuestionType("judge");
    q.setContent("判断题干。");
    q.setAnswer("√");
    TheoryOptionShuffler.shuffle(List.of(q), new Random(1));
    assertEquals("√", q.getAnswer());
  }

  /** 零信任兜底：答案字母无法映射到选项（E 越界）→ 原样返回不洗 */
  @Test
  void unmappableAnswerSkipsShuffle() {
    TheoryQuestionItem q = single("E");
    List<TheoryQuestionItem.Option> before = q.getOptionList();
    TheoryOptionShuffler.shuffle(List.of(q), new Random(1));
    assertSame(before, q.getOptionList());
    assertEquals("E", q.getAnswer());
  }
}
