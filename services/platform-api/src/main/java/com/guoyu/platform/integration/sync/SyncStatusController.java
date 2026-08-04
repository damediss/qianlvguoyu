package com.guoyu.platform.integration.sync;

import com.guoyu.platform.common.api.ApiResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 同步状态监控只读接口（鉴权方案 §3.5.5：暴露 sync_checkpoint + 行数对比）。
 *
 * <p>管理端接口，权限码 {@code system:sync:view}（V20260804_003 种子已授予平台管理员 role 2000）。
 * PHP 侧行数不可达时 sourceCount=-1 降级展示，不抛异常。
 */
@Slf4j
@RestController
@RequestMapping("/system/sync")
public class SyncStatusController {

  private final JdbcTemplate jdbcTemplate;
  private final JdbcTemplate phpJdbcTemplate;

  public SyncStatusController(
      JdbcTemplate jdbcTemplate,
      @Qualifier("phpJdbcTemplate") JdbcTemplate phpJdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    this.phpJdbcTemplate = phpJdbcTemplate;
  }

  @GetMapping("/status")
  @PreAuthorize("hasAuthority('system:sync:view')")
  public ApiResponse<List<Map<String, Object>>> status() {
    List<Map<String, Object>> checkpoints = jdbcTemplate.queryForList(
        "SELECT table_name, last_max_id, last_run_at, last_success_at, fail_count, last_error, stats, updated_at"
            + " FROM sync_checkpoint ORDER BY table_name");
    List<Map<String, Object>> result = new ArrayList<>();
    for (String table : List.of("platform_user", "platform_user_token", "platform_user_detail")) {
      Map<String, Object> item = new HashMap<>();
      item.put("tableName", table);
      item.put("localCount", countSafely(jdbcTemplate, table));
      item.put("sourceCount", countSafely(phpJdbcTemplate, table));
      Map<String, Object> checkpoint = checkpoints.stream()
          .filter(row -> table.equals(row.get("table_name")))
          .findFirst()
          .orElse(null);
      item.put("checkpoint", checkpoint);
      result.add(item);
    }
    return ApiResponse.ok(result);
  }

  private static long countSafely(JdbcTemplate template, String table) {
    try {
      Long count = template.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
      return count != null ? count : -1L;
    } catch (Exception e) {
      log.warn("[SYNC] 监控接口计数失败 table={} error={}", table, e.getMessage());
      return -1L;
    }
  }
}
