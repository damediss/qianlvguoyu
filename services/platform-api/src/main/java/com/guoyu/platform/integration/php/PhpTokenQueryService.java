package com.guoyu.platform.integration.php;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * PHP platform_user_token 直查（镜像 miss 兜底）。
 *
 * <p>背景：App 端鉴权依赖 SaaS 镜像表 platform_user_token，镜像由快周期任务
 * （默认 3s，platform.sync.interval-ms）从 PHP 源库同步。PHP 登录为单点制——
 * re-login 会删旧 token 发新 token，新 token 在同步窗口内不在镜像中，
 * 期间 App 请求一律 401，前端退避重试（300ms×3）覆盖不了该窗口，
 * 学员被误判"登录已过期"踢到登录页。
 *
 * <p>本服务在镜像 miss 时直查 PHP 源库（只读），命中即回种镜像：
 * 鉴权结果与 PHP 实时一致，同步窗口对 App 完全透明。
 */
@Slf4j
@Service
public class PhpTokenQueryService {

  /** PHP token 为 40 位 SHA-1 hex；管理端 Sa-Token uuid 等其他格式不触发 PHP 直查 */
  private static final Pattern PHP_TOKEN_PATTERN = Pattern.compile("^[0-9a-f]{40}$");

  private final JdbcTemplate phpJdbcTemplate;
  private final JdbcTemplate jdbcTemplate;

  public PhpTokenQueryService(
      @Qualifier("phpJdbcTemplate") JdbcTemplate phpJdbcTemplate,
      JdbcTemplate jdbcTemplate) {
    this.phpJdbcTemplate = phpJdbcTemplate;
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * 直查 PHP 源库按 token 找用户（对齐 PHP findFirst 语义：id 升序首行），命中回种镜像。
   *
   * @return userId；未命中、格式不符或 PHP 不可达返回 null（调用方按未认证处理）
   */
  public Long findUserIdByToken(String token) {
    if (token == null || !PHP_TOKEN_PATTERN.matcher(token).matches()) {
      return null;
    }
    try {
      List<Map<String, Object>> rows = phpJdbcTemplate.queryForList(
          "SELECT id, user_id, addtime FROM platform_user_token WHERE token = ? ORDER BY id ASC LIMIT 1",
          token);
      if (rows.isEmpty()) {
        return null;
      }
      Map<String, Object> row = rows.get(0);
      if (!(row.get("id") instanceof Number id) || !(row.get("user_id") instanceof Number userId)) {
        return null;
      }
      Long addtime = row.get("addtime") instanceof Number t ? t.longValue() : null;
      seedMirror(id.longValue(), userId.longValue(), token, addtime);
      return userId.longValue();
    } catch (Exception e) {
      // PHP 不可达不阻断请求链：按未认证放行给下游（EntryPoint 返 401），日志留痕
      log.warn("[App鉴权] PHP token 直查失败：{}", e.getMessage());
      return null;
    }
  }

  /**
   * 回种 SaaS 镜像（保持 PHP 源行 id，同步任务的 upsert/对账语义不受影响）。
   * upsert 用旧式 {@code VALUES()} 语法——主库 Druid wall 过滤器拦截 8.0.19+ 的 {@code AS new} 行别名。
   */
  private void seedMirror(long id, long userId, String token, Long addtime) {
    try {
      jdbcTemplate.update(
          "INSERT INTO platform_user_token (id, user_id, token, addtime) VALUES (?,?,?,?)"
              + " ON DUPLICATE KEY UPDATE user_id = VALUES(user_id),"
              + " token = VALUES(token), addtime = VALUES(addtime)",
          id, userId, token, addtime);
      log.info("[App鉴权] token 镜像 miss，已直查 PHP 命中并回种：userId={}", userId);
    } catch (Exception e) {
      // 回种失败不阻断鉴权（下一同步周期自愈）
      log.warn("[App鉴权] token 回种镜像失败：{}", e.getMessage());
    }
  }
}
