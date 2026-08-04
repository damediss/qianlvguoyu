package com.guoyu.platform.integration.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 同步 diff 纯逻辑（可单测）：比较"源库 id→指纹"与"本地 id→指纹"，得出待 upsert / 待删除的 id 集合。
 *
 * <p>指纹由两侧同一条 SQL（{@code MD5(CONCAT_WS('|', 显式列...))}）或行内容拼接生成，
 * 本类只关心 Map 语义，不关心指纹来源。
 */
public final class SyncDiffer {

  private SyncDiffer() {
  }

  /**
   * @param toUpsert 源有本地无、或指纹不一致的 id（需要回源拉全列 upsert）
   * @param toDelete 本地有源无的 id（源已物理删除，对账时本地补删）
   */
  public record Diff(List<Long> toUpsert, List<Long> toDelete) {
  }

  /**
   * @param sourceFingerprints 源库 id → 指纹（非 null，空表给空 Map）
   * @param localFingerprints  本地 id → 指纹（非 null，空表给空 Map）
   */
  public static Diff diffByFingerprint(Map<Long, String> sourceFingerprints, Map<Long, String> localFingerprints) {
    List<Long> toUpsert = new ArrayList<>();
    for (Map.Entry<Long, String> entry : sourceFingerprints.entrySet()) {
      String localFingerprint = localFingerprints.get(entry.getKey());
      if (!entry.getValue().equals(localFingerprint)) {
        toUpsert.add(entry.getKey());
      }
    }
    List<Long> toDelete = new ArrayList<>();
    for (Long localId : localFingerprints.keySet()) {
      if (!sourceFingerprints.containsKey(localId)) {
        toDelete.add(localId);
      }
    }
    return new Diff(toUpsert, toDelete);
  }

  /** 行内容 → 指纹字符串（token 表全量对账用；列值按固定顺序拼接，null 以空串占位） */
  public static String fingerprintOf(List<Object> orderedValues) {
    StringBuilder sb = new StringBuilder();
    for (Object value : orderedValues) {
      sb.append(value == null ? "" : String.valueOf(value)).append('|');
    }
    return sb.toString();
  }
}
