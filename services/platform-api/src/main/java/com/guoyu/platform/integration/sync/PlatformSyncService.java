package com.guoyu.platform.integration.sync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * PHP → SaaS 三表轮询同步（《手机端调用SaaS鉴权方案》§3.5.3）。
 *
 * <p>双通道检测（PHP 表无 updatetime）：
 * <ol>
 *   <li>新增：id 水位线快路径 {@code WHERE id > lastMaxId}，每周期执行（主键索引，代价极低）；</li>
 *   <li>修改/软删：全表 {@code MD5(CONCAT_WS('|', 显式列))} 指纹比对，不一致的行回源拉全列 upsert；
 *       因是全表扫描，user/detail 按 {@code fingerprint-interval-ms}（默认 60s）节流，token 表除外。</li>
 * </ol>
 *
 * <p>语义约束：
 * <ul>
 *   <li>token 表每周期全量对账（含物理删除——踢人 ≤1 周期在 SaaS 生效）；</li>
 *   <li>user/detail 的物理删除由每日对账处理（PHP 侧 detail 无 deleted 列，user 软删经指纹天然覆盖）；</li>
 *   <li>本地删除只作用于 PHP 来源区间（id &lt; 10 亿），SaaS 本地新增行（id ≥ 10 亿）绝不被同步任务触碰；</li>
 *   <li>哈希比对幂等，任意时刻重跑都收敛，无脏数据风险；</li>
 *   <li>{@code platform.sync.dry-run=true} 时只读源库并记录将发生的变更，不写镜像、不推进水位线。</li>
 * </ul>
 */
@Slf4j
@Service
public class PlatformSyncService {

  static final String TABLE_USER = "platform_user";
  static final String TABLE_DETAIL = "platform_user_detail";
  static final String TABLE_TOKEN = "platform_user_token";

  /** PHP 来源 id 区间上限：本地新增行 id ≥ 10 亿（AUTO_INCREMENT=1000000000），同步任务永不触碰 */
  static final long LOCAL_ID_BOUNDARY = 1_000_000_000L;

  /** platform_user 两库共同列（PHP 专有的 device_id/device_info/device_bindtime 为 PHP 登录流程内部字段，不同步） */
  static final List<String> USER_COLUMNS = List.of(
      "id", "realname", "idcard", "phone", "password", "stat", "addtime", "logintime",
      "org_id", "company_id", "wxopenid", "deleted", "expert_id", "task");

  /** platform_user_detail 两库共同列（PHP 专有 point/point_sign/school_paper_has/work_type_id 不同步；
   *  SaaS 专有 user_id/deleted 不在同步列内，upsert 不会覆盖） */
  static final List<String> DETAIL_COLUMNS = List.of(
      "id", "phone", "realname", "idcard", "gender", "nation", "education", "school", "major", "school_no",
      "in_school_date", "graduate_date", "work_time", "title_type", "school_paper", "political", "address",
      "house_type", "idcard_header", "birthday", "idcard_front", "idcard_back", "idcard_start_date", "idcard_end_date");

  static final List<String> TOKEN_COLUMNS = List.of("id", "user_id", "token", "addtime");

  private final JdbcTemplate jdbcTemplate;
  private final JdbcTemplate phpJdbcTemplate;
  private final SyncProperties properties;
  private final SyncLockManager lockManager;
  private final SyncAlertService alertService;

  /** 上次执行 user/detail 指纹比对的时间戳（0 = 尚未执行，首周期必跑）。单实例调度，volatile 足够 */
  private volatile long lastFingerprintAt = 0L;

  public PlatformSyncService(
      JdbcTemplate jdbcTemplate,
      @Qualifier("phpJdbcTemplate") JdbcTemplate phpJdbcTemplate,
      SyncProperties properties,
      SyncLockManager lockManager,
      SyncAlertService alertService) {
    this.jdbcTemplate = jdbcTemplate;
    this.phpJdbcTemplate = phpJdbcTemplate;
    this.properties = properties;
    this.lockManager = lockManager;
    this.alertService = alertService;
  }

  /** 快周期入口（scheduler 每 3s 调用）：user/detail 水位线+（节流的）指纹，token 全量对账 */
  public void runCycleSafely() {
    if (!properties.isEnabled()) {
      return;
    }
    if (!lockManager.tryLock()) {
      log.debug("[SYNC] 上一周期未结束，跳过本周期");
      return;
    }
    try {
      // 指纹比对（全表 MD5 扫描）按 fingerprint-interval-ms 节流，水位线快路径仍每周期跑；
      // token 表小且踢人敏感，每周期全量对账不受节流影响
      boolean fingerprintDue = markFingerprintIfDue();
      syncTable(TABLE_USER, USER_COLUMNS, false, fingerprintDue);
      syncTable(TABLE_DETAIL, DETAIL_COLUMNS, false, fingerprintDue);
      syncTable(TABLE_TOKEN, TOKEN_COLUMNS, true, true);
    } catch (Exception e) {
      // 不应到达：各表内部已按表兜底；双保险防锁泄漏
      log.error("[SYNC] 同步周期未捕获异常", e);
    } finally {
      lockManager.unlock();
    }
  }

  /** 指纹节流判定：到达间隔则记录本次时间并返回 true；首周期（lastFingerprintAt=0）必返回 true */
  private boolean markFingerprintIfDue() {
    long now = System.currentTimeMillis();
    if (now - lastFingerprintAt >= properties.getFingerprintIntervalMs()) {
      lastFingerprintAt = now;
      return true;
    }
    return false;
  }

  /** 每日全量对账入口（scheduler cron 调用）：三表全量 diff + 物理删除修复 + 漂移大闸 */
  public void reconcileDailySafely() {
    if (!properties.isEnabled()) {
      return;
    }
    if (!lockManager.tryLock()) {
      log.warn("[SYNC] 快周期执行中，每日对账顺延至下次 cron");
      return;
    }
    try {
      reconcileTableFully(TABLE_USER, USER_COLUMNS);
      reconcileTableFully(TABLE_DETAIL, DETAIL_COLUMNS);
      reconcileTableFully(TABLE_TOKEN, TOKEN_COLUMNS);
    } catch (Exception e) {
      log.error("[SYNC] 每日对账未捕获异常", e);
    } finally {
      lockManager.unlock();
    }
  }

  // ------------------------------------------------------------------
  // 快周期：单表水位线 + 指纹比对
  // ------------------------------------------------------------------

  void syncTable(String table, List<String> columns, boolean processDeletes, boolean runFingerprint) {
    long startedAt = System.currentTimeMillis();
    try {
      Checkpoint checkpoint = loadCheckpoint(table);

      // ① 新增快路径：id 水位线（走主键索引，每周期执行，代价极低）
      List<Long> newIds = phpJdbcTemplate.queryForList(
          "SELECT id FROM " + table + " WHERE id > ? ORDER BY id LIMIT ?",
          Long.class, checkpoint.lastMaxId(), properties.getBatchSize());
      int inserted = 0;
      long newMaxId = checkpoint.lastMaxId();
      if (!newIds.isEmpty()) {
        List<Map<String, Object>> rows = selectRowsByIds(phpJdbcTemplate, table, columns, newIds);
        rows = filterUnsyncableRows(table, rows);
        if (!properties.isDryRun()) {
          upsertRows(table, columns, rows);
        }
        inserted = rows.size();
        newMaxId = Math.max(newMaxId, newIds.get(newIds.size() - 1));
      }

      // ② 指纹比对抓修改/软删（两侧同一条 SQL，显式列名，禁 SELECT *）；
      //    全表 MD5 扫描代价高，按 fingerprint-interval-ms 节流，未到间隔的周期整段跳过
      int updated = 0;
      int deleted = 0;
      if (runFingerprint) {
        Map<Long, String> sourceFingerprints = selectFingerprints(phpJdbcTemplate, table, columns);
        Map<Long, String> localFingerprints = selectFingerprints(jdbcTemplate, table, columns);
        SyncDiffer.Diff diff = SyncDiffer.diffByFingerprint(sourceFingerprints, localFingerprints);

        if (!diff.toUpsert().isEmpty()) {
          List<Map<String, Object>> rows = selectRowsByIds(phpJdbcTemplate, table, columns, diff.toUpsert());
          rows = filterUnsyncableRows(table, rows);
          if (!properties.isDryRun()) {
            upsertRows(table, columns, rows);
          }
          updated = rows.size();
        }

        // ③ 物理删除（仅 token 快周期处理；user/detail 交每日对账）。本地删除只作用于 PHP 来源区间
        if (processDeletes) {
          List<Long> deleteCandidates = new ArrayList<>();
          for (Long id : diff.toDelete()) {
            if (id < LOCAL_ID_BOUNDARY) {
              deleteCandidates.add(id);
            }
          }
          if (!deleteCandidates.isEmpty()) {
            if (!properties.isDryRun()) {
              deleted = deleteRowsByIds(table, deleteCandidates);
            } else {
              deleted = deleteCandidates.size();
            }
          }
        }
      }

      String stats = "new=" + inserted + ",updated=" + updated + ",deleted=" + deleted
          + (runFingerprint ? "" : ",fingerprint=skipped");
      if (!properties.isDryRun()) {
        writeCheckpointSuccess(table, newMaxId, stats);
      }
      if (inserted > 0 || updated > 0 || deleted > 0) {
        log.info("[SYNC]{} table={} {} costMs={}", properties.isDryRun() ? "[DRY-RUN]" : "", table, stats,
            System.currentTimeMillis() - startedAt);
      }
    } catch (Exception e) {
      writeCheckpointFailureAndAlert(table, e);
    }
  }

  // ------------------------------------------------------------------
  // 每日全量对账：物理删除修复 + 修复后再比对（大闸）
  // ------------------------------------------------------------------

  private void reconcileTableFully(String table, List<String> columns) {
    long startedAt = System.currentTimeMillis();
    try {
      Map<Long, String> sourceFingerprints = selectFingerprints(phpJdbcTemplate, table, columns);
      // 本地只比对 PHP 来源区间（id < 10 亿），SaaS 本地新增行不参与对账
      Map<Long, String> localFingerprints = new HashMap<>();
      for (Map.Entry<Long, String> entry : selectFingerprints(jdbcTemplate, table, columns).entrySet()) {
        if (entry.getKey() < LOCAL_ID_BOUNDARY) {
          localFingerprints.put(entry.getKey(), entry.getValue());
        }
      }

      SyncDiffer.Diff diff = SyncDiffer.diffByFingerprint(sourceFingerprints, localFingerprints);
      int repaired = 0;
      if (!diff.toUpsert().isEmpty()) {
        List<Map<String, Object>> rows = filterUnsyncableRows(table,
            selectRowsByIds(phpJdbcTemplate, table, columns, diff.toUpsert()));
        if (!properties.isDryRun()) {
          upsertRows(table, columns, rows);
        }
        repaired = rows.size();
      }
      int deleted = 0;
      if (!diff.toDelete().isEmpty() && !properties.isDryRun()) {
        deleted = deleteRowsByIds(table, diff.toDelete());
      } else if (properties.isDryRun()) {
        deleted = diff.toDelete().size();
      }

      // 大闸：修复后再比对，仍不一致 → 漂移告警
      if (!properties.isDryRun()) {
        Map<Long, String> localAfter = new HashMap<>();
        for (Map.Entry<Long, String> entry : selectFingerprints(jdbcTemplate, table, columns).entrySet()) {
          if (entry.getKey() < LOCAL_ID_BOUNDARY) {
            localAfter.put(entry.getKey(), entry.getValue());
          }
        }
        SyncDiffer.Diff residual = SyncDiffer.diffByFingerprint(sourceFingerprints, localAfter);
        if (!residual.toUpsert().isEmpty() || !residual.toDelete().isEmpty()) {
          alertService.drift("table=" + table + " residualUpsert=" + residual.toUpsert().size()
              + " residualDelete=" + residual.toDelete().size());
        }
      }

      Checkpoint checkpoint = loadCheckpoint(table);
      String stats = "daily:repaired=" + repaired + ",deleted=" + deleted;
      if (!properties.isDryRun()) {
        writeCheckpointSuccess(table, checkpoint.lastMaxId(), stats);
      }
      log.info("[SYNC]{} 每日对账 table={} {} costMs={}", properties.isDryRun() ? "[DRY-RUN]" : "", table, stats,
          System.currentTimeMillis() - startedAt);
    } catch (Exception e) {
      writeCheckpointFailureAndAlert(table, e);
    }
  }

  // ------------------------------------------------------------------
  // IO 助手（package-private 便于单测）
  // ------------------------------------------------------------------

  /** 全表 id→MD5 指纹（约 24 字节/行，5 千行 ≈ 120KB/周期，跨公网可接受） */
  Map<Long, String> selectFingerprints(JdbcTemplate template, String table, List<String> columns) {
    String hashExpr = "MD5(CONCAT_WS('|', " + String.join(", ", columns) + "))";
    List<Map<String, Object>> rows = template.queryForList("SELECT id, " + hashExpr + " AS h FROM " + table);
    Map<Long, String> fingerprints = new HashMap<>(Math.max(16, rows.size() * 2));
    for (Map<String, Object> row : rows) {
      Object id = row.get("id");
      Object hash = row.get("h");
      if (id instanceof Number number && hash != null) {
        fingerprints.put(number.longValue(), String.valueOf(hash));
      }
    }
    return fingerprints;
  }

  List<Map<String, Object>> selectRowsByIds(JdbcTemplate template, String table, List<String> columns, List<Long> ids) {
    List<Map<String, Object>> result = new ArrayList<>();
    String columnList = String.join(", ", columns);
    for (List<Long> chunk : chunked(ids, properties.getBatchSize())) {
      String placeholders = String.join(", ", chunk.stream().map(id -> "?").toList());
      result.addAll(template.queryForList(
          "SELECT " + columnList + " FROM " + table + " WHERE id IN (" + placeholders + ")", chunk.toArray()));
    }
    return result;
  }

  /** token 表特殊约束：SaaS 镜像 token 列 NOT NULL，源库 null token 行跳过并记 warn（防御性） */
  private List<Map<String, Object>> filterUnsyncableRows(String table, List<Map<String, Object>> rows) {
    if (!TABLE_TOKEN.equals(table)) {
      return rows;
    }
    List<Map<String, Object>> filtered = new ArrayList<>(rows.size());
    int skipped = 0;
    for (Map<String, Object> row : rows) {
      if (row.get("token") == null) {
        skipped++;
      } else {
        filtered.add(row);
      }
    }
    if (skipped > 0) {
      log.warn("[SYNC] table={} 源库 null token 行跳过 {} 条（镜像列 NOT NULL）", table, skipped);
    }
    return filtered;
  }

  void upsertRows(String table, List<String> columns, List<Map<String, Object>> rows) {
    if (rows.isEmpty()) {
      return;
    }
    // 注意：必须用 VALUES(col) 形式而非 MySQL 8.0.19+ 的 "AS new" 行别名形式——
    // 主库 Druid wall 过滤器（1.2.24）不支持行别名语法，会按注入拦截（2026-08-04 冒烟实测）
    StringBuilder sql = new StringBuilder("INSERT INTO ").append(table)
        .append(" (").append(String.join(", ", columns)).append(") VALUES (")
        .append(String.join(", ", columns.stream().map(c -> "?").toList()))
        .append(") ON DUPLICATE KEY UPDATE ");
    List<String> updates = new ArrayList<>();
    for (String column : columns) {
      if (!"id".equals(column)) {
        updates.add(column + " = VALUES(" + column + ")");
      }
    }
    sql.append(String.join(", ", updates));

    List<Object[]> batchArgs = new ArrayList<>(rows.size());
    for (Map<String, Object> row : rows) {
      Object[] values = new Object[columns.size()];
      for (int i = 0; i < columns.size(); i++) {
        values[i] = row.get(columns.get(i));
      }
      batchArgs.add(values);
    }
    jdbcTemplate.batchUpdate(sql.toString(), batchArgs);
  }

  int deleteRowsByIds(String table, List<Long> ids) {
    int deleted = 0;
    for (List<Long> chunk : chunked(ids, properties.getBatchSize())) {
      String placeholders = String.join(", ", chunk.stream().map(id -> "?").toList());
      deleted += jdbcTemplate.update("DELETE FROM " + table + " WHERE id IN (" + placeholders + ")", chunk.toArray());
    }
    return deleted;
  }

  private static List<List<Long>> chunked(List<Long> ids, int size) {
    List<List<Long>> chunks = new ArrayList<>();
    for (int i = 0; i < ids.size(); i += size) {
      chunks.add(ids.subList(i, Math.min(i + size, ids.size())));
    }
    return chunks;
  }

  // ------------------------------------------------------------------
  // checkpoint 持久化
  // ------------------------------------------------------------------

  record Checkpoint(long lastMaxId, int failCount) {
  }

  Checkpoint loadCheckpoint(String table) {
    List<Map<String, Object>> rows = jdbcTemplate.queryForList(
        "SELECT last_max_id, fail_count FROM sync_checkpoint WHERE table_name = ?", table);
    if (rows.isEmpty()) {
      return new Checkpoint(0L, 0);
    }
    Map<String, Object> row = rows.get(0);
    long lastMaxId = row.get("last_max_id") instanceof Number number ? number.longValue() : 0L;
    int failCount = row.get("fail_count") instanceof Number number ? number.intValue() : 0;
    return new Checkpoint(lastMaxId, failCount);
  }

  void writeCheckpointSuccess(String table, long lastMaxId, String stats) {
    upsertCheckpoint(table, lastMaxId, true, 0, null, stats);
  }

  private void writeCheckpointFailureAndAlert(String table, Exception e) {
    String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    try {
      Checkpoint checkpoint = loadCheckpoint(table);
      int consecutiveFails = checkpoint.failCount() + 1;
      upsertCheckpoint(table, checkpoint.lastMaxId(), false, consecutiveFails, truncate(message, 450), null);
      alertService.cycleFailed(table, consecutiveFails, properties.getFailAlertThreshold(), message);
    } catch (Exception checkpointError) {
      // checkpoint 都写不了（主库故障）：只能 error 日志，下个周期继续试
      log.error("[SYNC-ALERT] 同步失败且 checkpoint 写入失败 table={} error={} checkpointError={}",
          table, message, checkpointError.getMessage());
    }
  }

  private void upsertCheckpoint(String table, long lastMaxId, boolean success, int failCount,
      String lastError, String stats) {
    // VALUES(col) 形式：主库 Druid wall 过滤器不支持 "AS new" 行别名（见 upsertRows 注释）
    jdbcTemplate.update(
        "INSERT INTO sync_checkpoint (table_name, last_max_id, last_run_at, last_success_at, fail_count, last_error, stats)"
            + " VALUES (?, ?, NOW(), ?, ?, ?, ?)"
            + " ON DUPLICATE KEY UPDATE last_max_id = VALUES(last_max_id), last_run_at = VALUES(last_run_at),"
            + " last_success_at = VALUES(last_success_at), fail_count = VALUES(fail_count),"
            + " last_error = VALUES(last_error), stats = VALUES(stats)",
        table, lastMaxId, success ? new java.sql.Timestamp(System.currentTimeMillis()) : null,
        failCount, lastError, stats);
  }

  private static String truncate(String value, int maxLength) {
    return value != null && value.length() > maxLength ? value.substring(0, maxLength) : value;
  }
}
