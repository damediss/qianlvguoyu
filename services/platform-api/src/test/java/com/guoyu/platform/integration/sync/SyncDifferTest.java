package com.guoyu.platform.integration.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SyncDifferTest {

  @Test
  void newSourceRowsGoToUpsert() {
    SyncDiffer.Diff diff = SyncDiffer.diffByFingerprint(
        Map.of(1L, "h1", 2L, "h2"),
        Map.of(1L, "h1"));
    assertThat(diff.toUpsert()).containsExactly(2L);
    assertThat(diff.toDelete()).isEmpty();
  }

  @Test
  void changedFingerprintGoesToUpsert() {
    SyncDiffer.Diff diff = SyncDiffer.diffByFingerprint(
        Map.of(1L, "h1", 2L, "h2-new"),
        Map.of(1L, "h1", 2L, "h2-old"));
    assertThat(diff.toUpsert()).containsExactly(2L);
    assertThat(diff.toDelete()).isEmpty();
  }

  @Test
  void identicalFingerprintsProduceEmptyDiff() {
    SyncDiffer.Diff diff = SyncDiffer.diffByFingerprint(
        Map.of(1L, "h1", 2L, "h2"),
        Map.of(1L, "h1", 2L, "h2"));
    assertThat(diff.toUpsert()).isEmpty();
    assertThat(diff.toDelete()).isEmpty();
  }

  @Test
  void localExtraRowsGoToDelete() {
    SyncDiffer.Diff diff = SyncDiffer.diffByFingerprint(
        Map.of(1L, "h1"),
        Map.of(1L, "h1", 9L, "h9"));
    assertThat(diff.toUpsert()).isEmpty();
    assertThat(diff.toDelete()).containsExactly(9L);
  }

  @Test
  void mixedChangesAreSplitCorrectly() {
    SyncDiffer.Diff diff = SyncDiffer.diffByFingerprint(
        Map.of(1L, "h1", 2L, "h2-changed", 3L, "h3"),
        Map.of(1L, "h1", 2L, "h2", 4L, "h4"));
    assertThat(diff.toUpsert()).containsExactlyInAnyOrder(2L, 3L);
    assertThat(diff.toDelete()).containsExactly(4L);
  }

  @Test
  void emptySourceMeansEverythingLocalIsDeleted() {
    SyncDiffer.Diff diff = SyncDiffer.diffByFingerprint(Map.of(), Map.of(1L, "h1"));
    assertThat(diff.toUpsert()).isEmpty();
    assertThat(diff.toDelete()).containsExactly(1L);
  }

  @Test
  void fingerprintOfHandlesNullsAndOrder() {
    assertThat(SyncDiffer.fingerprintOf(java.util.Arrays.asList(1, null, "abc"))).isEqualTo("1||abc|");
    assertThat(SyncDiffer.fingerprintOf(List.of())).isEqualTo("");
  }
}
