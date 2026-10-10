package com.guoyu.platform.app.exam.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** O2 精准学：弱项桶加权（同步学红卡/星标 boost） */
class PreciseLearnBucketScoreTest {

  @Test
  void syncBoostRaisesBucketAbovePlainPrefer() {
    Map<String, Integer> seeds = Map.of("10|single", 1);
    Map<Integer, Double> kr = Map.of(10, 40.0);
    Map<String, Double> tr = Map.of("single", 20.0);
    double withSync = AppExamSmartService.scoreBucket(
        "10|single", seeds, kr, tr, Set.of(10), Set.of(10));
    double withoutSync = AppExamSmartService.scoreBucket(
        "10|single", seeds, kr, tr, Set.of(10), Set.of());
    assertTrue(withSync > withoutSync, "sync red/star boost must raise score");
  }

  @Test
  void highWrongRateBeatsLowRateSameSeeds() {
    Map<String, Integer> seeds = Map.of("1|single", 2, "2|single", 2);
    Map<Integer, Double> kr = Map.of(1, 80.0, 2, 20.0);
    Map<String, Double> tr = Map.of("single", 30.0);
    double weak = AppExamSmartService.scoreBucket(
        "1|single", seeds, kr, tr, Set.of(), Set.of());
    double mild = AppExamSmartService.scoreBucket(
        "2|single", seeds, kr, tr, Set.of(), Set.of());
    assertTrue(weak > mild);
  }
}
