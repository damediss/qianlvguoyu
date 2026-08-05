package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.trainingdepartment.entity.PlatformUserDetailEntity;
import com.guoyu.platform.trainingdepartment.entity.PlatformUserEntity;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserDetailMapper;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 分班确认等场景：按身份证确保 platform_user 账号存在（开 App / 学员信息主档）。
 * 规则见 docs/04_requirements/phase2-training/企业确认分班后同步学员信息方案.md
 */
@Service
public class PlatformUserEnsureService {

  private static final Logger log = LoggerFactory.getLogger(PlatformUserEnsureService.class);
  private static final String DEFAULT_PASSWORD = "123123";

  public enum Outcome {
    CREATED, REUSED, UPDATED, SKIPPED
  }

  public record EnsureResult(Outcome outcome, Long platformUserId, String warning) {
    public static EnsureResult skipped(String warning) {
      return new EnsureResult(Outcome.SKIPPED, null, warning);
    }
  }

  public static class SyncStats {
    private int created;
    private int reused;
    private int updated;
    private int skipped;
    private final List<Map<String, Object>> warnings = new ArrayList<>();

    public void accept(EnsureResult result, String name, String idCard) {
      if (result == null) {
        return;
      }
      switch (result.outcome()) {
        case CREATED -> created++;
        case REUSED -> reused++;
        case UPDATED -> updated++;
        case SKIPPED -> skipped++;
      }
      if (StringUtils.hasText(result.warning())) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("name", name == null ? "" : name);
        row.put("idCard", idCard == null ? "" : idCard);
        row.put("reason", result.warning());
        warnings.add(row);
      }
    }

    public Map<String, Object> toMap() {
      Map<String, Object> out = new LinkedHashMap<>();
      out.put("created", created);
      out.put("reused", reused);
      out.put("updated", updated);
      out.put("skipped", skipped);
      out.put("warnings", warnings);
      return out;
    }

    public int getCreated() {
      return created;
    }
  }

  private final PlatformUserMapper mapper;
  private final PlatformUserDetailMapper detailMapper;

  public PlatformUserEnsureService(PlatformUserMapper mapper, PlatformUserDetailMapper detailMapper) {
    this.mapper = mapper;
    this.detailMapper = detailMapper;
  }

  /**
   * 按身份证 upsert 学员账号。可跳过场景返回 SKIPPED + warning，不抛业务异常。
   */
  @Transactional
  public EnsureResult ensureFromDivisionPerson(
      String realname,
      String idCard,
      String phone,
      Long companyId,
      Long orgId) {
    String normalizedId = normalizeIdCard(idCard);
    String normalizedPhone = normalizePhone(phone);
    String name = trim(realname);

    if (!StringUtils.hasText(normalizedId)) {
      return EnsureResult.skipped("缺少身份证，无法开 App 账号");
    }
    if (normalizedId.length() != 18) {
      return EnsureResult.skipped("身份证号位数不正确");
    }
    if (!StringUtils.hasText(normalizedPhone) || normalizedPhone.length() != 11) {
      return EnsureResult.skipped("缺少有效手机号，无法开 App 账号");
    }

    PlatformUserEntity existing = mapper.selectOne(new LambdaQueryWrapper<PlatformUserEntity>()
        .eq(PlatformUserEntity::getDeleted, 0)
        .eq(PlatformUserEntity::getIdcard, normalizedId)
        .last("LIMIT 1"));

    if (existing == null) {
      PlatformUserEntity phoneOwner = findByPhone(normalizedPhone, null);
      if (phoneOwner != null) {
        return EnsureResult.skipped("手机号已被其他学员占用");
      }
      PlatformUserEntity created = insertUser(name, normalizedId, normalizedPhone, companyId, orgId);
      syncDetailFillEmpty(created, true);
      log.info("division ensure platform_user created id={} idCard={}", created.getId(), maskId(normalizedId));
      return new EnsureResult(Outcome.CREATED, created.getId(), null);
    }

    boolean changed = false;
    List<String> notes = new ArrayList<>();

    if (!StringUtils.hasText(existing.getRealname()) && StringUtils.hasText(name)) {
      existing.setRealname(name);
      changed = true;
    } else if (StringUtils.hasText(name)
        && StringUtils.hasText(existing.getRealname())
        && existing.getRealname().equals(trim(existing.getPhone()))) {
      // 旧档用手机号占位姓名时，用花名册/报名真名覆盖
      existing.setRealname(name);
      changed = true;
    }

    if (existing.getCompanyId() == null && companyId != null && companyId > 0) {
      existing.setCompanyId(companyId);
      changed = true;
    }

    if (orgId != null && orgId > 0
        && (existing.getOrgId() == null || existing.getOrgId() == 0L)) {
      existing.setOrgId(orgId);
      changed = true;
    }

    String currentPhone = trim(existing.getPhone());
    if (!normalizedPhone.equals(currentPhone)) {
      PlatformUserEntity phoneOwner = findByPhone(normalizedPhone, existing.getId());
      if (phoneOwner != null) {
        notes.add("手机冲突，保留原手机");
      } else {
        log.info(
            "division ensure platform_user phone update id={} old={} new={}",
            existing.getId(),
            maskPhone(currentPhone),
            maskPhone(normalizedPhone));
        existing.setPhone(normalizedPhone);
        changed = true;
      }
    }

    if (changed) {
      mapper.updateById(existing);
    }
    syncDetailFillEmpty(existing, false);

    if (changed || !notes.isEmpty()) {
      String warning = notes.isEmpty() ? null : String.join("；", notes);
      return new EnsureResult(Outcome.UPDATED, existing.getId(), warning);
    }
    return new EnsureResult(Outcome.REUSED, existing.getId(), null);
  }

  private PlatformUserEntity insertUser(
      String realname, String idCard, String phone, Long companyId, Long orgId) {
    PlatformUserEntity entity = new PlatformUserEntity();
    entity.setRealname(StringUtils.hasText(realname) ? realname : phone);
    entity.setIdcard(idCard);
    entity.setPhone(phone);
    entity.setCompanyId(companyId);
    entity.setOrgId(orgId != null ? orgId : 0L);
    entity.setAddtime(System.currentTimeMillis() / 1000);
    entity.setDeleted(0);
    String stat = randomStat();
    entity.setStat(stat);
    entity.setPassword(md5(DEFAULT_PASSWORD, stat));
    mapper.insert(entity);
    return entity;
  }

  private PlatformUserEntity findByPhone(String phone, Long excludeId) {
    LambdaQueryWrapper<PlatformUserEntity> wrapper = new LambdaQueryWrapper<PlatformUserEntity>()
        .eq(PlatformUserEntity::getDeleted, 0)
        .eq(PlatformUserEntity::getPhone, phone);
    if (excludeId != null && excludeId > 0) {
      wrapper.ne(PlatformUserEntity::getId, excludeId);
    }
    return mapper.selectOne(wrapper.last("LIMIT 1"));
  }

  private void syncDetailFillEmpty(PlatformUserEntity user, boolean createShell) {
    PlatformUserDetailEntity detail = detailMapper.selectOne(new LambdaQueryWrapper<PlatformUserDetailEntity>()
        .eq(PlatformUserDetailEntity::getIdcard, user.getIdcard())
        .eq(PlatformUserDetailEntity::getDeleted, 0)
        .last("LIMIT 1"));
    if (detail == null) {
      if (!createShell) {
        return;
      }
      detail = new PlatformUserDetailEntity();
      detail.setUserId(user.getId());
      detail.setIdcard(user.getIdcard());
      detail.setRealname(user.getRealname());
      detail.setPhone(user.getPhone());
      detail.setDeleted(0);
      if (user.getIdcard() != null && user.getIdcard().length() >= 14) {
        detail.setBirthday(user.getIdcard().substring(6, 14));
        detail.setGender(genderFromIdCard(user.getIdcard()));
      }
      detailMapper.insert(detail);
      return;
    }
    boolean changed = false;
    if (detail.getUserId() == null) {
      detail.setUserId(user.getId());
      changed = true;
    }
    if (!StringUtils.hasText(detail.getRealname()) && StringUtils.hasText(user.getRealname())) {
      detail.setRealname(user.getRealname());
      changed = true;
    }
    if (!StringUtils.hasText(detail.getPhone()) && StringUtils.hasText(user.getPhone())) {
      detail.setPhone(user.getPhone());
      changed = true;
    }
    if (changed) {
      detailMapper.updateById(detail);
    }
  }

  private static Integer genderFromIdCard(String idCard) {
    try {
      int code = Integer.parseInt(idCard.substring(16, 17));
      return code % 2 == 0 ? 2 : 1;
    } catch (Exception e) {
      return null;
    }
  }

  static String normalizeIdCard(String idCard) {
    if (!StringUtils.hasText(idCard)) {
      return "";
    }
    return idCard.replace(" ", "").toUpperCase(Locale.ROOT);
  }

  private static String normalizePhone(String phone) {
    if (!StringUtils.hasText(phone)) {
      return "";
    }
    return phone.replace(" ", "").trim();
  }

  private static String trim(String value) {
    return value == null ? "" : value.replace(" ", "").trim();
  }

  private static String randomStat() {
    return UUID.randomUUID().toString().replace("-", "").substring(0, 4);
  }

  private static String md5(String password, String stat) {
    try {
      MessageDigest digest = MessageDigest.getInstance("MD5");
      byte[] bytes = digest.digest((password + stat).getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(bytes);
    } catch (Exception e) {
      throw new IllegalStateException("密码加密失败", e);
    }
  }

  private static String maskId(String id) {
    if (id == null || id.length() < 8) {
      return "****";
    }
    return id.substring(0, 4) + "**********" + id.substring(id.length() - 4);
  }

  private static String maskPhone(String phone) {
    if (phone == null || phone.length() < 7) {
      return "****";
    }
    return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
  }
}
