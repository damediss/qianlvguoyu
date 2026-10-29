package com.guoyu.platform.integration.php;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PhpPlatformUserWriterImpl implements PhpPlatformUserWriter {

  private static final Logger log = LoggerFactory.getLogger(PhpPlatformUserWriterImpl.class);

  private final JdbcTemplate phpJdbcTemplate;

  public PhpPlatformUserWriterImpl(@Qualifier("phpJdbcTemplate") JdbcTemplate phpJdbcTemplate) {
    this.phpJdbcTemplate = phpJdbcTemplate;
  }

  @Override
  public Optional<PhpUserRow> findByIdCard(String idCard) {
    if (!StringUtils.hasText(idCard)) {
      return Optional.empty();
    }
    List<Map<String, Object>> rows = phpJdbcTemplate.queryForList(
        """
            SELECT id, realname, idcard, phone, password, stat, org_id, company_id
            FROM platform_user
            WHERE deleted = 0 AND idcard = ?
            ORDER BY id ASC
            LIMIT 1
            """,
        idCard);
    return rows.isEmpty() ? Optional.empty() : Optional.of(mapRow(rows.get(0)));
  }

  @Override
  public Optional<PhpUserRow> findByPhone(String phone, Long excludeId) {
    if (!StringUtils.hasText(phone)) {
      return Optional.empty();
    }
    List<Map<String, Object>> rows;
    if (excludeId != null && excludeId > 0) {
      rows = phpJdbcTemplate.queryForList(
          """
              SELECT id, realname, idcard, phone, password, stat, org_id, company_id
              FROM platform_user
              WHERE deleted = 0 AND phone = ? AND id <> ?
              ORDER BY id ASC
              LIMIT 1
              """,
          phone, excludeId);
    } else {
      rows = phpJdbcTemplate.queryForList(
          """
              SELECT id, realname, idcard, phone, password, stat, org_id, company_id
              FROM platform_user
              WHERE deleted = 0 AND phone = ?
              ORDER BY id ASC
              LIMIT 1
              """,
          phone);
    }
    return rows.isEmpty() ? Optional.empty() : Optional.of(mapRow(rows.get(0)));
  }

  @Override
  public long insert(PhpUserWrite cmd) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    phpJdbcTemplate.update(connection -> {
      PreparedStatement ps = connection.prepareStatement(
          """
              INSERT INTO platform_user
                (realname, idcard, phone, password, stat, addtime, logintime,
                 org_id, company_id, wxopenid, deleted, expert_id, task)
              VALUES (?, ?, ?, ?, ?, ?, 0, ?, ?, NULL, 0, 0, 0)
              """,
          Statement.RETURN_GENERATED_KEYS);
      ps.setString(1, cmd.realname());
      ps.setString(2, cmd.idcard());
      ps.setString(3, cmd.phone());
      ps.setString(4, cmd.password());
      ps.setString(5, cmd.stat());
      ps.setLong(6, cmd.addtime());
      ps.setLong(7, cmd.orgId() != null ? cmd.orgId() : 0L);
      ps.setLong(8, cmd.companyId() != null ? cmd.companyId() : 0L);
      return ps;
    }, keyHolder);
    Number key = keyHolder.getKey();
    if (key == null) {
      throw new IllegalStateException("PHP platform_user insert 未返回 id");
    }
    long id = key.longValue();
    log.info("php platform_user created id={} idCard={}", id, maskId(cmd.idcard()));
    return id;
  }

  @Override
  public void updateProfile(long id, PhpUserProfilePatch patch) {
    phpJdbcTemplate.update(
        """
            UPDATE platform_user
            SET realname = ?, phone = ?, org_id = ?, company_id = ?
            WHERE id = ? AND deleted = 0
            """,
        patch.realname(),
        patch.phone(),
        patch.orgId() != null ? patch.orgId() : 0L,
        patch.companyId() != null ? patch.companyId() : 0L,
        id);
    log.info("php platform_user profile updated id={}", id);
  }

  private static PhpUserRow mapRow(Map<String, Object> row) {
    return new PhpUserRow(
        ((Number) row.get("id")).longValue(),
        asString(row.get("realname")),
        asString(row.get("idcard")),
        asString(row.get("phone")),
        asString(row.get("password")),
        asString(row.get("stat")),
        asLong(row.get("org_id")),
        asLong(row.get("company_id")));
  }

  private static String asString(Object v) {
    return v == null ? null : String.valueOf(v);
  }

  private static Long asLong(Object v) {
    if (v == null) {
      return null;
    }
    if (v instanceof Number n) {
      return n.longValue();
    }
    try {
      return Long.parseLong(String.valueOf(v));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static String maskId(String id) {
    if (id == null || id.length() < 8) {
      return "****";
    }
    return id.substring(0, 4) + "**********" + id.substring(id.length() - 4);
  }
}
