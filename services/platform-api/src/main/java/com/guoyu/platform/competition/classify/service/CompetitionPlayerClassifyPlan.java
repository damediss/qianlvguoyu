package com.guoyu.platform.competition.classify.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 出场顺序是 1..选手人数。各类型人数尽量相等，再把类型名额打乱后铺到这条顺序上。
 * 人数不能整除时，靠前的类型各多 1 人。
 */
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

  /** 0-based 类型下标。名额先按人数均分，再打乱，下标即出场位置。 */
  static List<Integer> shuffledTypeIndexes(int playerCount, int typeCount, Random random) {
    if (random == null) {
      throw new IllegalArgumentException("random is required");
    }
    List<Integer> sizes = balancedSizes(playerCount, typeCount);
    List<Integer> indexes = new ArrayList<>(playerCount);
    for (int typeIndex = 0; typeIndex < sizes.size(); typeIndex++) {
      for (int count = 0; count < sizes.get(typeIndex); count++) {
        indexes.add(typeIndex);
      }
    }
    Collections.shuffle(indexes, random);
    return List.copyOf(indexes);
  }
}
