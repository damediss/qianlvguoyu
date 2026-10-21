package com.guoyu.platform.textbook;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guoyu.platform.textbook.domain.TextbookWordAllocator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 篇幅配比（设计 §4.1-TextbookWordAllocatorTest，最大余数法）。
 */
class TextbookWordAllocatorTest {

  /** ④ 基准用例：40 学时 × 2500 = 100000，五级槽操作工五章权重 */
  @Test
  void baseCase100000() {
    List<String> warnings = new ArrayList<>();
    int[] words = TextbookWordAllocator.allocateChapters(100000,
        List.of(bd("5"), bd("30"), bd("10"), bd("50"), bd("5")), warnings);
    assertArrayEquals(new int[]{5000, 30000, 10000, 50000, 5000}, words);
    assertEquals(100000, Arrays.stream(words).sum());
    assertTrue(warnings.isEmpty());
  }

  /** ① 除不尽：999 按 1:1:1 → 和严格 999，各份差 ≤1 */
  @Test
  void indivisibleTotalStillConserves() {
    List<String> warnings = new ArrayList<>();
    int[] words = TextbookWordAllocator.allocateChapters(999,
        List.of(bd("1"), bd("1"), bd("1")), warnings);
    assertEquals(999, Arrays.stream(words).sum());
    for (int w : words) {
      assertTrue(w == 333 || w == 334, "各份应 333/334：" + Arrays.toString(words));
    }

    int[] awkward = TextbookWordAllocator.allocateChapters(77777,
        List.of(bd("33"), bd("33"), bd("34")), warnings);
    assertEquals(77777, Arrays.stream(awkward).sum());
  }

  /** ① 权重 0：0 权重章不占字数 */
  @Test
  void zeroWeightGetsNothing() {
    List<String> warnings = new ArrayList<>();
    int[] words = TextbookWordAllocator.allocateChapters(100,
        List.of(bd("0"), bd("0"), bd("10")), warnings);
    assertArrayEquals(new int[]{0, 0, 100}, words);

    int[] withNull = TextbookWordAllocator.allocateChapters(100,
        Arrays.asList(null, bd("10")), warnings);
    assertArrayEquals(new int[]{0, 100}, withNull);
  }

  /** ① 单章：全部给唯一章 */
  @Test
  void singleChapterGetsAll() {
    List<String> warnings = new ArrayList<>();
    int[] words = TextbookWordAllocator.allocateChapters(50000, List.of(bd("5")), warnings);
    assertArrayEquals(new int[]{50000}, words);

    // 全 0 权重：均分兜底 + warning
    int[] zero = TextbookWordAllocator.allocateChapters(99,
        List.of(bd("0"), bd("0"), bd("0")), warnings);
    assertEquals(99, Arrays.stream(zero).sum());
    assertTrue(warnings.stream().anyMatch(w -> w.contains("权重合计为 0")),
        "全 0 权重应记均分 warning：" + warnings);
  }

  /** ② 节之和 = 章字数（条数占比 3:7:2:2:2） */
  @Test
  void sectionsSumToChapterWords() {
    List<String> warnings = new ArrayList<>();
    int[] words = TextbookWordAllocator.allocateSections(16000,
        new int[]{3, 7, 2, 2, 2}, warnings);
    assertEquals(16000, Arrays.stream(words).sum());
    assertTrue(words[1] > words[0] && words[0] > words[2],
        "条数多的节字数应多：" + Arrays.toString(words));

    int[] prime = TextbookWordAllocator.allocateSections(997, new int[]{1, 1, 1}, warnings);
    assertEquals(997, Arrays.stream(prime).sum());
  }

  /** ③ 章总条数=0 → 均分 + warning */
  @Test
  void zeroCountsSplitEvenlyWithWarning() {
    List<String> warnings = new ArrayList<>();
    int[] words = TextbookWordAllocator.allocateSections(100, new int[]{0, 0}, warnings);
    assertArrayEquals(new int[]{50, 50}, words);
    assertTrue(warnings.stream().anyMatch(w -> w.contains("章总条数为 0")),
        "0 条数应记均分 warning：" + warnings);
  }

  private static BigDecimal bd(String v) {
    return new BigDecimal(v);
  }
}
