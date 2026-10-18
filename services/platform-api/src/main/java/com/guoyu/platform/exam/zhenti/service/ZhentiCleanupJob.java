package com.guoyu.platform.exam.zhenti.service;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 软删 90 天物理清理（方案 §2.3 / §1 M5）。
 *
 * <p>MyBatis-Plus 逻辑删除会自动追加 deleted=0，这里用 JdbcTemplate 原生 SQL 跨租户扫描；
 * 文件删除失败仅告警不阻断（下一轮会重试同一条记录）。单批 200 条，避免长事务锁表。
 */
@Component
@Slf4j
public class ZhentiCleanupJob {

  private static final int BATCH_SIZE = 200;
  private static final int RETENTION_DAYS = 90;

  private final JdbcTemplate jdbcTemplate;
  private final ZhentiStorageService storageService;
  private final boolean enabled;

  public ZhentiCleanupJob(
      JdbcTemplate jdbcTemplate,
      ZhentiStorageService storageService,
      @Value("${platform.zhenti.cleanup-enabled:true}") boolean enabled) {
    this.jdbcTemplate = jdbcTemplate;
    this.storageService = storageService;
    this.enabled = enabled;
  }

  @Scheduled(cron = "${platform.zhenti.cleanup-cron:0 30 3 * * ?}")
  public void purgeExpired() {
    if (!enabled) {
      return;
    }
    List<Map<String, Object>> rows = jdbcTemplate.queryForList(
        "SELECT id, file_key FROM exam_zhenti "
            + "WHERE deleted = 1 AND updated_at < DATE_SUB(NOW(), INTERVAL ? DAY) "
            + "ORDER BY id LIMIT ?",
        RETENTION_DAYS, BATCH_SIZE);
    if (rows.isEmpty()) {
      return;
    }
    int removed = 0;
    for (Map<String, Object> row : rows) {
      Object id = row.get("id");
      Object fileKey = row.get("file_key");
      try {
        if (fileKey != null) {
          storageService.delete(String.valueOf(fileKey));
        }
        removed += jdbcTemplate.update(
            "DELETE FROM exam_zhenti WHERE id = ? AND deleted = 1", id);
      } catch (RuntimeException ex) {
        log.warn("[ZhentiCleanup] 物理清理失败 id={} err={}", id, ex.toString());
      }
    }
    log.info("[ZhentiCleanup] 软删满 {} 天物理清理完成，扫描 {} 条，删除 {} 条",
        RETENTION_DAYS, rows.size(), removed);
  }
}
