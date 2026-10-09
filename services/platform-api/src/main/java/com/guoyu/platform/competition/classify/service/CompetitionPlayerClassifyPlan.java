package com.guoyu.platform.competition.classify.service;

import java.util.ArrayList;
import java.util.List;

/** 按类型数量把选手尽量均分。不能整除时，靠前的类型各多 1 人。 */
final class CompetitionPlayerClassifyPlan {

  private CompetitionPlayerClassifyPlan() {
  }

  static List<Integer> balancedSizes(int playerCount, int typeCount) {
    if (playerCount < 1 || typeCount < 1 || typeCount > playerCount) {
      throw new IllegalArgumentException("type count must be between 1 and the player count");
    }
    int base = playerCount / typeCount;
    int extra = playerCount % typeCount;
    List<Integer> sizes = new ArrayList<>(typeCount);
    for (int index = 0; index < typeCount; index++) {
      sizes.add(base + (index < extra ? 1 : 0));
    }
    return List.copyOf(sizes);
  }
}
