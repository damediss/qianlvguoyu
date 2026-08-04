package com.guoyu.platform.integration.sync;

import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

/** 同步周期编排逻辑测试（JdbcTemplate 全 mock，不连真实库） */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlatformSyncServiceTest {

  @Mock
  private JdbcTemplate primary;
  @Mock
  private JdbcTemplate php;
  @Mock
  private SyncLockManager lockManager;
  @Mock
  private SyncAlertService alertService;

  private SyncProperties properties;
  private PlatformSyncService service;

  @BeforeEach
  void setUp() {
    properties = new SyncProperties();
    service = new PlatformSyncService(primary, php, properties, lockManager, alertService);
    when(lockManager.tryLock()).thenReturn(true);
    // 默认：三表 checkpoint 初始（水位线 0、连续失败 0）
    stubCheckpoint(PlatformSyncService.TABLE_USER, 0L, 0);
    stubCheckpoint(PlatformSyncService.TABLE_DETAIL, 0L, 0);
    stubCheckpoint(PlatformSyncService.TABLE_TOKEN, 0L, 0);
    // 默认：三表水位线无新增、指纹两侧皆空（无差异）
    for (String table : List.of(PlatformSyncService.TABLE_USER, PlatformSyncService.TABLE_DETAIL,
        PlatformSyncService.TABLE_TOKEN)) {
      stubWatermark(table, List.of());
      stubFingerprints(php, table, Map.of());
      stubFingerprints(primary, table, Map.of());
    }
  }

  private void stubCheckpoint(String table, long lastMaxId, int failCount) {
    when(primary.queryForList(contains("FROM sync_checkpoint"), eq(table)))
        .thenReturn(List.of(Map.of("last_max_id", lastMaxId, "fail_count", failCount)));
  }

  private void stubWatermark(String table, List<Long> ids) {
    when(php.queryForList(contains("FROM " + table + " WHERE id > ?"), eq(Long.class), any(), any()))
        .thenReturn(ids);
  }

  private void stubFingerprints(JdbcTemplate template, String table, Map<Long, String> fingerprints) {
    List<Map<String, Object>> rows = fingerprints.entrySet().stream()
        .map(e -> Map.<String, Object>of("id", e.getKey(), "h", e.getValue()))
        .toList();
    when(template.queryForList(contains("AS h FROM " + table))).thenReturn(rows);
  }

  private void stubRowsByIds(String table, List<Map<String, Object>> rows) {
    when(php.queryForList(contains("FROM " + table + " WHERE id IN"), any(Object[].class)))
        .thenReturn(rows);
  }

  @Test
  void watermarkNewRowsAreUpsertedAndCheckpointAdvanced() {
    stubCheckpoint(PlatformSyncService.TABLE_USER, 100L, 0);
    stubWatermark(PlatformSyncService.TABLE_USER, List.of(101L, 102L));
    stubRowsByIds(PlatformSyncService.TABLE_USER, List.of(
        Map.of("id", 101L, "realname", "张三"),
        Map.of("id", 102L, "realname", "李四")));

    service.runCycleSafely();

    verify(primary).batchUpdate(contains("INSERT INTO platform_user "),
        argThat((List<Object[]> batch) -> batch.size() == 2));
    // 三表各写一次成功 checkpoint
    verify(primary, times(3)).update(contains("INSERT INTO sync_checkpoint"), any(Object[].class));
    verify(alertService, never()).cycleFailed(anyString(), eq(1), eq(20), anyString());
  }

  @Test
  void hashMismatchRowsAreRepairedFromSource() {
    stubFingerprints(php, PlatformSyncService.TABLE_USER, Map.of(1L, "h1", 2L, "h2-new"));
    stubFingerprints(primary, PlatformSyncService.TABLE_USER, Map.of(1L, "h1", 2L, "h2-old"));
    stubRowsByIds(PlatformSyncService.TABLE_USER, List.of(Map.of("id", 2L, "realname", "王五")));

    service.runCycleSafely();

    verify(primary).batchUpdate(contains("INSERT INTO platform_user "),
        argThat((List<Object[]> batch) -> batch.size() == 1));
  }

  @Test
  void tokenKickDeletionsOnlyApplyWithinPhpSourceRange() {
    stubFingerprints(php, PlatformSyncService.TABLE_TOKEN, Map.of(1L, "a"));
    stubFingerprints(primary, PlatformSyncService.TABLE_TOKEN,
        Map.of(1L, "a", 5L, "b", 2_000_000_000L, "c"));

    service.runCycleSafely();

    // id=5（PHP 来源区间）被删；id=20 亿（SaaS 本地区间）绝不被同步任务触碰
    verify(primary).update(contains("DELETE FROM platform_user_token"), aryEq(new Object[]{5L}));
    verify(primary, never()).update(contains("DELETE"), aryEq(new Object[]{2_000_000_000L}));
  }

  @Test
  void nullTokenRowsAreSkippedBeforeUpsert() {
    stubWatermark(PlatformSyncService.TABLE_TOKEN, List.of(7L, 8L));
    stubRowsByIds(PlatformSyncService.TABLE_TOKEN, List.of(
        Map.of("id", 7L, "user_id", 1L, "addtime", 100L), // token=null
        Map.of("id", 8L, "user_id", 2L, "token", "abc", "addtime", 101L)));

    service.runCycleSafely();

    verify(primary).batchUpdate(contains("INSERT INTO platform_user_token "),
        argThat((List<Object[]> batch) -> batch.size() == 1 && "abc".equals(batch.get(0)[2])));
  }

  @Test
  void phpFailureIncrementsFailCountAndAlerts() {
    stubCheckpoint(PlatformSyncService.TABLE_USER, 0L, 3);
    when(php.queryForList(contains("FROM platform_user WHERE id > ?"), eq(Long.class), any(), any()))
        .thenThrow(new DataAccessResourceFailureException("connect timeout"));

    service.runCycleSafely();

    verify(alertService).cycleFailed(eq("platform_user"), eq(4), eq(20), anyString());
    // 失败分支写 checkpoint（fail_count=4），另两表成功也各写一次
    verify(primary, times(3)).update(contains("INSERT INTO sync_checkpoint"), any(Object[].class));
  }

  @Test
  void dryRunReadsSourceButWritesNothing() {
    properties.setDryRun(true);
    stubWatermark(PlatformSyncService.TABLE_USER, List.of(1L, 2L));
    stubRowsByIds(PlatformSyncService.TABLE_USER, List.of(
        Map.of("id", 1L, "realname", "张三"), Map.of("id", 2L, "realname", "李四")));
    stubFingerprints(php, PlatformSyncService.TABLE_USER, Map.of(3L, "x"));
    stubFingerprints(php, PlatformSyncService.TABLE_TOKEN, Map.of());
    stubFingerprints(primary, PlatformSyncService.TABLE_TOKEN, Map.of(9L, "y"));

    service.runCycleSafely();

    verify(primary, never()).batchUpdate(anyString(), anyList());
    verify(primary, never()).update(contains("INSERT INTO sync_checkpoint"), any(Object[].class));
    verify(primary, never()).update(contains("DELETE"), any(Object[].class));
  }

  @Test
  void cycleIsSkippedWhenLockNotAcquired() {
    when(lockManager.tryLock()).thenReturn(false);

    service.runCycleSafely();

    verifyNoInteractions(php);
    verify(primary, never()).batchUpdate(anyString(), anyList());
  }
}
