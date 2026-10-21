package com.guoyu.platform.textbook.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 篇幅配比（设计 §2.4-④，最大余数法，纯函数）。
 *
 * <p>章字数 = total × weight ÷ Σweight 最大余数凑整，各章之和严格 = total
 * （标准权重合计 100 时与设计「÷100」口径完全一致；合计 ≠100 时按权重占比归一，
 * 仍保证合计严格守恒）。节字数 = 章字数 × 节条数 ÷ 章总条数，同法凑整；
 * 章总条数=0 时均分并记 warning。
 */
public final class TextbookWordAllocator {

  private TextbookWordAllocator() {
  }

  /**
   * 章级配比。
   *
   * @param weights 各章权重（null 按 0 计；全部 null/0 时均分并记 warning）
   * @return 各章目标字数，长度 = weights.size()，Σ 严格 = totalWords
   */
  public static int[] allocateChapters(int totalWords, List<BigDecimal> weights,
      List<String> warnings) {
    int n = weights.size();
    if (n == 0) {
      return new int[0];
    }
    if (n == 1) {
      return new int[]{totalWords};
    }
    BigDecimal[] normalized = new BigDecimal[n];
    BigDecimal sum = BigDecimal.ZERO;
    for (int i = 0; i < n; i++) {
      BigDecimal w = weights.get(i) == null ? BigDecimal.ZERO : weights.get(i);
      normalized[i] = w;
      sum = sum.add(w);
    }
    double[] shares;
    if (sum.signum() <= 0) {
      warnings.add("WARN: 章权重合计为 0，按章数均分总字数");
      shares = new double[n];
      for (int i = 0; i < n; i++) {
        shares[i] = 1.0 / n;
      }
    } else {
      shares = new double[n];
      for (int i = 0; i < n; i++) {
        shares[i] = normalized[i].divide(sum, 10, RoundingMode.HALF_UP).doubleValue();
      }
    }
    return largestRemainder(totalWords, shares);
  }

  /**
   * 节级配比。
   *
   * @param chapterWords 章目标字数
   * @param sectionCounts 各节（知识点+技能点）条数；全 0 时均分并记 warning
   * @return 各节目标字数，Σ 严格 = chapterWords
   */
  public static int[] allocateSections(int chapterWords, int[] sectionCounts,
      List<String> warnings) {
    int n = sectionCounts.length;
    if (n == 0) {
      return new int[0];
    }
    if (n == 1) {
      return new int[]{chapterWords};
    }
    int totalCount = 0;
    for (int c : sectionCounts) {
      totalCount += Math.max(c, 0);
    }
    double[] shares;
    if (totalCount <= 0) {
      warnings.add("WARN: 章总条数为 0，按节数均分章字数");
      shares = new double[n];
      for (int i = 0; i < n; i++) {
        shares[i] = 1.0 / n;
      }
    } else {
      shares = new double[n];
      for (int i = 0; i < n; i++) {
        shares[i] = (double) Math.max(sectionCounts[i], 0) / totalCount;
      }
    }
    return largestRemainder(chapterWords, shares);
  }

  /**
   * 最大余数法：floor 后按余数从大到小每份额外补 1，直到凑满 total。
   * Σshares=1 时 leftover 必然 &lt; n，一轮内补完；Σ 严格 = total。
   */
  static int[] largestRemainder(int total, double[] shares) {
    int n = shares.length;
    int[] result = new int[n];
    if (total <= 0 || n == 0) {
      return result;
    }
    record Share(int idx, double remainder) {
    }
    List<Share> remainders = new ArrayList<>();
    int allocated = 0;
    for (int i = 0; i < n; i++) {
      double exact = total * shares[i];
      result[i] = (int) Math.floor(exact);
      remainders.add(new Share(i, exact - result[i]));
      allocated += result[i];
    }
    remainders.sort(Comparator.comparingDouble(Share::remainder).reversed());
    int leftover = total - allocated;
    for (Share s : remainders) {
      if (leftover <= 0) {
        break;
      }
      result[s.idx()]++;
      leftover--;
    }
    return result;
  }
}
