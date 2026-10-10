package com.guoyu.platform.competition.draw.service;

import com.guoyu.platform.competition.draw.vo.CompetitionPlayerDrawSlot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 两轮抽签的纯规则：第一轮打乱选手得到抽签顺序，第二轮按该顺序逐个抽取尚未占用的出场序号。
 */
public final class CompetitionPlayerDrawSession {

  private CompetitionPlayerDrawSession() {
  }

  public record DrawStep(
      List<CompetitionPlayerDrawSlot> slots,
      CompetitionPlayerDrawSlot drawn,
      boolean complete) {
  }

  public static List<CompetitionPlayerDrawSlot> createDrawOrder(
      List<CompetitionPlayerDrawSlot> contestants, Random random) {
    if (contestants == null || contestants.isEmpty()) {
      throw new IllegalArgumentException("contestants required");
    }
    if (random == null) {
      throw new IllegalArgumentException("random required");
    }
    List<CompetitionPlayerDrawSlot> shuffled = new ArrayList<>(contestants);
    Collections.shuffle(shuffled, random);
    List<CompetitionPlayerDrawSlot> ordered = new ArrayList<>(shuffled.size());
    for (int index = 0; index < shuffled.size(); index++) {
      CompetitionPlayerDrawSlot source = shuffled.get(index);
      ordered.add(copy(source, index + 1, null));
    }
    return List.copyOf(ordered);
  }

  public static DrawStep drawNext(List<CompetitionPlayerDrawSlot> slots, Random random) {
    if (slots == null || slots.isEmpty()) {
      throw new IllegalArgumentException("slots required");
    }
    if (random == null) {
      throw new IllegalArgumentException("random required");
    }
    CompetitionPlayerDrawSlot next = null;
    for (CompetitionPlayerDrawSlot slot : slots) {
      if (slot.appearanceOrder() == null
          && (next == null || slot.drawOrder() < next.drawOrder())) {
        next = slot;
      }
    }
    if (next == null) {
      throw new IllegalStateException("draw complete");
    }
    List<Integer> remaining = remainingAppearanceOrders(slots);
    if (remaining.isEmpty()) {
      throw new IllegalStateException("no appearance order left");
    }
    int picked = remaining.get(random.nextInt(remaining.size()));
    List<CompetitionPlayerDrawSlot> updated = new ArrayList<>(slots.size());
    CompetitionPlayerDrawSlot drawn = null;
    for (CompetitionPlayerDrawSlot slot : slots) {
      if (slot.contestantProfileId().equals(next.contestantProfileId())
          && slot.drawOrder() == next.drawOrder()) {
        drawn = copy(slot, slot.drawOrder(), picked);
        updated.add(drawn);
      } else {
        updated.add(slot);
      }
    }
    if (drawn == null) {
      throw new IllegalStateException("current contestant missing");
    }
    boolean complete = updated.stream().allMatch(slot -> slot.appearanceOrder() != null);
    return new DrawStep(List.copyOf(updated), drawn, complete);
  }

  private static List<Integer> remainingAppearanceOrders(List<CompetitionPlayerDrawSlot> slots) {
    Set<Integer> used = new HashSet<>();
    for (CompetitionPlayerDrawSlot slot : slots) {
      if (slot.appearanceOrder() != null) {
        used.add(slot.appearanceOrder());
      }
    }
    List<Integer> remaining = new ArrayList<>();
    for (int order = 1; order <= slots.size(); order++) {
      if (!used.contains(order)) {
        remaining.add(order);
      }
    }
    return remaining;
  }

  private static CompetitionPlayerDrawSlot copy(
      CompetitionPlayerDrawSlot source, int drawOrder, Integer appearanceOrder) {
    return new CompetitionPlayerDrawSlot(
        source.contestantProfileId(), source.contestantNo(), source.name(), source.orgName(),
        source.mobile(), drawOrder, appearanceOrder);
  }
}
