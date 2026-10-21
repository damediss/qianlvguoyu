package com.guoyu.platform.competition.draw.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** 单名选手从 1..N 的剩余号码中随机抽取一个号码。 */
public final class CompetitionPlayerSelfDraw {

  private CompetitionPlayerSelfDraw() {
  }

  public static int pick(int playerCount, Collection<Integer> usedNumbers, Random random) {
    if (playerCount < 1) throw new IllegalArgumentException("playerCount must be positive");
    if (random == null) throw new IllegalArgumentException("random required");
    Set<Integer> used = new HashSet<>();
    if (usedNumbers != null) {
      for (Integer number : usedNumbers) {
        if (number == null || number < 1 || number > playerCount || !used.add(number)) {
          throw new IllegalArgumentException("used number is invalid or duplicated");
        }
      }
    }
    List<Integer> remaining = new ArrayList<>();
    for (int number = 1; number <= playerCount; number++) {
      if (!used.contains(number)) remaining.add(number);
    }
    if (remaining.isEmpty()) throw new IllegalStateException("draw complete");
    return remaining.get(random.nextInt(remaining.size()));
  }
}
