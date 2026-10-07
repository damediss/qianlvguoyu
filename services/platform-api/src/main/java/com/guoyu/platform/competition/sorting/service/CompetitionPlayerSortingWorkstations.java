package com.guoyu.platform.competition.sorting.service;

import java.util.ArrayList;
import java.util.List;

/**
 * 工位数量是每组可用工位数，必须小于等于每组人数上限。
 * 组内按出场顺序轮转：第 n 位选手进入第 ((n - 1) % 工位数 + 1) 个工位。
 * 人数不能被工位数整除时，靠前的工位多 1 人，任意两个工位的人数相差不超过 1。
 */
final class CompetitionPlayerSortingWorkstations {

  private static final String[] DIGITS = {"零", "一", "二", "三", "四", "五", "六", "七", "八", "九"};

  private CompetitionPlayerSortingWorkstations() {
  }

  static String assign(int groupOrder, int workstationCount) {
    if (groupOrder < 1 || workstationCount < 1) {
      throw new IllegalArgumentException("group order and workstation count must be positive");
    }
    int stationNo = (groupOrder - 1) % workstationCount + 1;
    return "第" + chinese(stationNo) + "工位";
  }

  static List<String> assignGroup(int groupSize, int workstationCount) {
    if (groupSize < 1) {
      throw new IllegalArgumentException("group size must be positive");
    }
    List<String> stations = new ArrayList<>(groupSize);
    for (int groupOrder = 1; groupOrder <= groupSize; groupOrder++) {
      stations.add(assign(groupOrder, workstationCount));
    }
    return List.copyOf(stations);
  }

  static String chinese(int number) {
    if (number < 1 || number > 5000) {
      throw new IllegalArgumentException("workstation number must be between 1 and 5000");
    }
    int thousands = number / 1000;
    int hundreds = (number % 1000) / 100;
    int tens = (number % 100) / 10;
    int ones = number % 10;
    StringBuilder text = new StringBuilder();
    if (thousands > 0) {
      text.append(DIGITS[thousands]).append("千");
    }
    if (hundreds > 0) {
      text.append(DIGITS[hundreds]).append("百");
    } else if (thousands > 0 && (tens > 0 || ones > 0)) {
      text.append("零");
    }
    if (tens > 0) {
      if (!(tens == 1 && thousands == 0 && hundreds == 0)) {
        text.append(DIGITS[tens]);
      }
      text.append("十");
    } else if (hundreds > 0 && ones > 0) {
      text.append("零");
    }
    if (ones > 0) {
      text.append(DIGITS[ones]);
    }
    return text.toString();
  }
}
