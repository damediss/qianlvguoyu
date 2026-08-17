package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.integration.php.PhpPlatformUserWriter;
import com.guoyu.platform.integration.php.PhpPlatformUserWriter.PhpUserProfilePatch;
import com.guoyu.platform.integration.php.PhpPlatformUserWriter.PhpUserRow;
import com.guoyu.platform.integration.php.PhpPlatformUserWriter.PhpUserWrite;
import com.guoyu.platform.integration.php.UserProvisioningProperties;
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
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 分班确认 / 开班导入：按身份证确保 App 学员账号存在。
 *
 * <p>当 {@code platform.user-provisioning.php-write-enabled=true} 时：先写 PHP（登录权威），
 * 再同 id 镜像 SaaS；否则仅写 SaaS（开发降级，App 不可登新建号）。
 *
 * @see docs/04_requirements/phase2-training/学员账号同步_实现方案.md
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
    private int phpWritten;
    private final List<Map<String, Object>> warnings = new ArrayList<>();

    public void accept(EnsureResult result, String name, String idCard) {
      if (result == null) {
        return;
      }
      switch (result.outcome()) {
        case CREATED -> {
          created++;
          phpWritten++;
        }
        case REUSED -> reused++;
        case UPDATED -> {
          updated++;
          phpWritten++;
        }
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
      out.put("phpWritten", phpWritten);
      out.put("loginReady", created + reused + updated);
      out.put("warnings", warnings);
      return out;
    }

    public int getCreated() {
      return created;
    }
  }

  private final PlatformUserMapper mapper;
  private final PlatformUserDetailMapper detailMapper;
  private final PhpPlatformUserWriter phpWriter;
  private final UserProvisioningProperties provisioningProperties;
  private final JdbcTemplate jdbcTemplate;

  public PlatformUserEnsureService(
      PlatformUserMapper mapper,
      PlatformUserDetailMapper detailMapper,
      PhpPlatformUserWriter phpWriter,
      UserProvisioningProperties provisioningProperties,
      JdbcTemplate jdbcTemplate) {
    this.mapper = mapper;
    this.detailMapper = detailMapper;
    this.phpWriter = phpWriter;
    this.provisioningProperties = provisioningProperties;
    this.jdbcTemplate = jdbcTemplate;
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

    if (!provisioningProperties.isPhpWriteEnabled()) {
      return legacySaasOnlyEnsure(name, normalizedId, normalizedPhone, companyId, orgId);
    }

    try {
      return ensureWithPhpDualWrite(name, normalizedId, normalizedPhone, companyId, orgId);
    } catch (Exception ex) {
      log.error("division ensure php dual-write failed idCard={}", maskId(normalizedId), ex);
      if (provisioningProperties.isFailSoft()) {
        return EnsureResult.skipped("PHP 开户失败: " + rootMessage(ex));
      }
      throw ex;
    }
  }

  private static String rootMessage(Throwable ex) {
    String best = ex.getMessage();
    Throwable cur = ex;
    int depth = 0;
    while (cur != null && depth < 8) {
      String m = cur.getMessage();
      if (m != null && !m.isBlank()) {
        best = m;
        if (m.contains("Access denied")
            || m.contains("Communications link failure")
            || m.contains("Unknown database")
            || m.contains("doesn't have a default value")) {
          break;
        }
      }
      cur = cur.getCause();
      depth++;
    }
    return best == null ? ex.getClass().getSimpleName() : best;
  }

  private EnsureResult ensureWithPhpDualWrite(
      String name, String normalizedId, String normalizedPhone, Long companyId, Long orgId) {
    Optional<PhpUserRow> phpExisting = phpWriter.findByIdCard(normalizedId);
    Outcome outcome;
    long phpId;
    String password = null;
    String stat = null;
    long addtime = System.currentTimeMillis() / 1000;
    List<String> notes = new ArrayList<>();
    String finalName = StringUtils.hasText(name) ? name : normalizedPhone;
    String finalPhone = normalizedPhone;
    Long finalOrgId = orgId != null && orgId > 0 ? orgId : 0L;
    Long finalCompanyId = companyId != null && companyId > 0 ? companyId : null;
    boolean profileChanged = false;

    if (phpExisting.isEmpty()) {
      if (phpWriter.findByPhone(normalizedPhone, null).isPresent()) {
        return EnsureResult.skipped("手机号已被其他学员占用");
      }
      stat = randomStat();
      password = md5(DEFAULT_PASSWORD, stat);
      phpId = phpWriter.insert(new PhpUserWrite(
          finalName, normalizedId, normalizedPhone, password, stat, addtime, finalOrgId, finalCompanyId));
      outcome = Outcome.CREATED;
    } else {
      PhpUserRow row = phpExisting.get();
      phpId = row.id();
      password = row.password();
      stat = row.stat();
      addtime = System.currentTimeMillis() / 1000;

      String currentName = trim(row.realname());
      if (!StringUtils.hasText(currentName) && StringUtils.hasText(name)) {
        finalName = name;
        profileChanged = true;
      } else if (StringUtils.hasText(name)
          && StringUtils.hasText(currentName)
          && currentName.equals(trim(row.phone()))) {
        finalName = name;
        profileChanged = true;
      } else {
        finalName = StringUtils.hasText(currentName) ? currentName : finalName;
      }

      Long rowOrg = row.orgId();
      if ((rowOrg == null || rowOrg == 0L) && orgId != null && orgId > 0) {
        finalOrgId = orgId;
        profileChanged = true;
      } else {
        finalOrgId = rowOrg != null ? rowOrg : 0L;
      }

      Long rowCompany = row.companyId();
      if (rowCompany == null && companyId != null && companyId > 0) {
        finalCompanyId = companyId;
        profileChanged = true;
      } else {
        finalCompanyId = rowCompany;
      }

      String currentPhone = trim(row.phone());
      if (!normalizedPhone.equals(currentPhone)) {
        if (phpWriter.findByPhone(normalizedPhone, phpId).isPresent()) {
          notes.add("手机冲突，保留原手机");
          finalPhone = currentPhone;
        } else {
          log.info(
              "division ensure php phone update id={} old={} new={}",
              phpId,
              maskPhone(currentPhone),
              maskPhone(normalizedPhone));
          finalPhone = normalizedPhone;
          profileChanged = true;
        }
      } else {
        finalPhone = currentPhone;
      }

      if (profileChanged) {
        phpWriter.updateProfile(phpId, new PhpUserProfilePatch(finalName, finalPhone, finalOrgId, finalCompanyId));
      }
      outcome = profileChanged || !notes.isEmpty() ? Outcome.UPDATED : Outcome.REUSED;
    }

    String mirrorWarning = null;
    try {
      mirrorToSaas(
          phpId,
          finalName,
          normalizedId,
          finalPhone,
          password,
          stat,
          finalCompanyId,
          finalOrgId,
          addtime,
          outcome == Outcome.CREATED);
      syncDetailFillEmpty(loadSaasUser(phpId, normalizedId), outcome == Outcome.CREATED);
    } catch (Exception mirrorEx) {
      log.error("saas mirror failed phpId={} idCard={}", phpId, maskId(normalizedId), mirrorEx);
      mirrorWarning = "PHP 已开户，SaaS 镜像失败，将依赖同步任务补齐: " + mirrorEx.getMessage();
    }

    String warning = joinWarnings(notes, mirrorWarning);
    if (outcome == Outcome.REUSED && warning == null) {
      return new EnsureResult(Outcome.REUSED, phpId, null);
    }
    if (outcome == Outcome.CREATED) {
      log.info("division ensure dual-write created phpId={} idCard={}", phpId, maskId(normalizedId));
      return new EnsureResult(Outcome.CREATED, phpId, warning);
    }
    return new EnsureResult(
        warning != null || outcome == Outcome.UPDATED ? Outcome.UPDATED : Outcome.REUSED,
        phpId,
        warning);
  }

  /** 开发降级：仅写 SaaS（可能产生高位 id，App 不可登）。 */
  private EnsureResult legacySaasOnlyEnsure(
      String name, String normalizedId, String normalizedPhone, Long companyId, Long orgId) {
    PlatformUserEntity existing = mapper.selectOne(new LambdaQueryWrapper<PlatformUserEntity>()
        .eq(PlatformUserEntity::getDeleted, 0)
        .eq(PlatformUserEntity::getIdcard, normalizedId)
        .last("LIMIT 1"));

    if (existing == null) {
      PlatformUserEntity phoneOwner = findByPhone(normalizedPhone, null);
      if (phoneOwner != null) {
        return EnsureResult.skipped("手机号已被其他学员占用");
      }
      PlatformUserEntity created = insertUserSaasAuto(name, normalizedId, normalizedPhone, companyId, orgId);
      syncDetailFillEmpty(created, true);
      log.info("division ensure saas-only created id={} idCard={}", created.getId(), maskId(normalizedId));
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
        existing.setPhone(normalizedPhone);
        changed = true;
      }
    }

    if (changed) {
      mapper.updateById(existing);
    }
    syncDetailFillEmpty(existing, false);

    if (changed || !notes.isEmpty()) {
      return new EnsureResult(Outcome.UPDATED, existing.getId(), notes.isEmpty() ? null : String.join("；", notes));
    }
    return new EnsureResult(Outcome.REUSED, existing.getId(), null);
  }

  private void mirrorToSaas(
      long phpId,
      String realname,
      String idCard,
      String phone,
      String password,
      String stat,
      Long companyId,
      Long orgId,
      long addtime,
      boolean writePassword) {
    // 同证其它 SaaS 行一律软删（含历史 PHP 号镜像与高位孤儿），只保留当前权威 phpId
    List<PlatformUserEntity> sameCard = mapper.selectList(new LambdaQueryWrapper<PlatformUserEntity>()
        .eq(PlatformUserEntity::getDeleted, 0)
        .eq(PlatformUserEntity::getIdcard, idCard));
    for (PlatformUserEntity row : sameCard) {
      if (row.getId() != null && !row.getId().equals(phpId)) {
        mapper.deleteById(row.getId());
        log.info("soft-delete saas duplicate id={} (keep phpId={}) for idCard={}",
            row.getId(), phpId, maskId(idCard));
      }
    }

    PlatformUserEntity byId = mapper.selectById(phpId);
    if (byId == null) {
      jdbcTemplate.update(
          """
              INSERT INTO platform_user
                (id, realname, idcard, phone, password, stat, addtime, logintime,
                 org_id, company_id, wxopenid, deleted, expert_id, task)
              VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, ?, NULL, 0, 0, 0)
              """,
          phpId,
          realname,
          idCard,
          phone,
          password,
          stat,
          addtime,
          orgId != null ? orgId : 0L,
          companyId != null ? companyId : 0L);
      return;
    }

    byId.setRealname(realname);
    byId.setPhone(phone);
    byId.setIdcard(idCard);
    byId.setOrgId(orgId != null ? orgId : 0L);
    byId.setCompanyId(companyId);
    byId.setDeleted(0);
    if (writePassword && StringUtils.hasText(password) && StringUtils.hasText(stat)) {
      byId.setPassword(password);
      byId.setStat(stat);
    }
    mapper.updateById(byId);
  }

  private PlatformUserEntity loadSaasUser(long phpId, String idCard) {
    PlatformUserEntity byId = mapper.selectById(phpId);
    if (byId != null) {
      return byId;
    }
    PlatformUserEntity shell = new PlatformUserEntity();
    shell.setId(phpId);
    shell.setIdcard(idCard);
    return shell;
  }

  private PlatformUserEntity insertUserSaasAuto(
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
    if (user == null || !StringUtils.hasText(user.getIdcard())) {
      return;
    }
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
    if (detail.getUserId() == null && user.getId() != null) {
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

  private static String joinWarnings(List<String> notes, String extra) {
    List<String> all = new ArrayList<>();
    if (notes != null) {
      for (String n : notes) {
        if (StringUtils.hasText(n)) {
          all.add(n);
        }
      }
    }
    if (StringUtils.hasText(extra)) {
      all.add(extra);
    }
    return all.isEmpty() ? null : String.join("；", all);
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

  /** 与 PHP md5(password.stat) 一致；单测可调用。 */
  public static String hashPassword(String password, String stat) {
    return md5(password, stat);
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
