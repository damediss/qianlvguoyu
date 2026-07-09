package com.guoyu.platform.app.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.app.entity.PlatformUserTokenEntity;
import com.guoyu.platform.app.mapper.PlatformUserTokenMapper;
import com.guoyu.platform.app.sms.AppSmsApplicationService;
import com.guoyu.platform.app.sms.AppSmsException;
import com.guoyu.platform.trainingdepartment.entity.PlatformUserDetailEntity;
import com.guoyu.platform.trainingdepartment.entity.PlatformUserEntity;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserDetailMapper;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 对齐 PHP AppauthController（checkcode / setlogin） */
@Service
public class AppAuthApplicationService {

  private static final String TOKEN_SALT_PREFIX = "H*EGiqr_+e!i*yDs5>fjaso45256hy{96TGdr";
  private static final String TOKEN_SALT_SUFFIX = "E$[SDtRY^G&@gf<t3of412d4}{e3Wa28Hw5";

  private final AppSmsApplicationService smsService;
  private final PlatformUserMapper userMapper;
  private final PlatformUserDetailMapper detailMapper;
  private final PlatformUserTokenMapper tokenMapper;

  public AppAuthApplicationService(
      AppSmsApplicationService smsService,
      PlatformUserMapper userMapper,
      PlatformUserDetailMapper detailMapper,
      PlatformUserTokenMapper tokenMapper) {
    this.smsService = smsService;
    this.userMapper = userMapper;
    this.detailMapper = detailMapper;
    this.tokenMapper = tokenMapper;
  }

  public void checkCode(String phone, String code) {
    smsService.verifyCode(phone, code);
  }

  /** 退出登录：删除当前 token 记录（仅登出当前设备） */
  public void logout(String token) {
    if (!StringUtils.hasText(token)) {
      return;
    }
    tokenMapper.delete(new LambdaQueryWrapper<PlatformUserTokenEntity>()
        .eq(PlatformUserTokenEntity::getToken, token.trim()));
  }

  public Map<String, Object> login(String phone, String password) {
    if (!StringUtils.hasText(phone)) {
      throw new AppSmsException(-1, "手机号不能为空");
    }
    if (!StringUtils.hasText(password)) {
      throw new AppSmsException(-1, "密码不能为空");
    }

    PlatformUserEntity user = userMapper.selectOne(
        new LambdaQueryWrapper<PlatformUserEntity>()
            .eq(PlatformUserEntity::getPhone, phone.trim())
            .eq(PlatformUserEntity::getDeleted, 0)
            .last("LIMIT 1"));
    if (user == null) {
      throw new AppSmsException(-1, "账号不存在");
    }

    // 密码校验：md5(password + stat)，对齐 PHP
    String inputHash = md5(password + (user.getStat() != null ? user.getStat() : ""));
    if (!inputHash.equals(user.getPassword())) {
      throw new AppSmsException(-1, "密码不正确");
    }

    return doLogin(user, phone.trim());
  }

  private Map<String, Object> doLogin(PlatformUserEntity user, String phone) {
    // 查找或创建 detail
    PlatformUserDetailEntity detail = detailMapper.selectOne(
        new LambdaQueryWrapper<PlatformUserDetailEntity>()
            .eq(StringUtils.hasText(user.getIdcard()), PlatformUserDetailEntity::getIdcard, user.getIdcard())
            .eq(PlatformUserDetailEntity::getDeleted, 0)
            .last("LIMIT 1"));
    if (detail == null) {
      detail = new PlatformUserDetailEntity();
      detail.setPhone(phone);
      detail.setIdcard(user.getIdcard() != null ? user.getIdcard() : "");
      detail.setDeleted(0);
      detailMapper.insert(detail);
    }

    // 踢旧 token
    tokenMapper.delete(new LambdaQueryWrapper<PlatformUserTokenEntity>()
        .eq(PlatformUserTokenEntity::getUserId, user.getId()));

    // 生成新 token
    String token = sha1Secret(user.getPassword() + Instant.now().getEpochSecond());
    PlatformUserTokenEntity tokenEntity = new PlatformUserTokenEntity();
    tokenEntity.setUserId(user.getId());
    tokenEntity.setToken(token);
    tokenEntity.setAddtime(Instant.now().getEpochSecond());
    tokenMapper.insert(tokenEntity);

    // 更新登录时间
    user.setLogintime(Instant.now().getEpochSecond());
    userMapper.updateById(user);

    Map<String, Object> detailMap = new LinkedHashMap<>();
    detailMap.put("id", detail.getId());
    detailMap.put("phone", detail.getPhone());
    detailMap.put("idcard", detail.getIdcard());
    detailMap.put("realname", detail.getRealname());
    detailMap.put("age", calcAgeFromIdCard(detail.getIdcard()));

    String realname = StringUtils.hasText(user.getRealname()) ? user.getRealname() : user.getPhone();
    Map<String, Object> userMap = new LinkedHashMap<>();
    userMap.put("id", user.getId());
    userMap.put("realname", realname);
    userMap.put("expert_id", user.getExpertId() != null ? user.getExpertId() : 0);

    Map<String, Object> data = new LinkedHashMap<>();
    data.put("token", token);
    data.put("detail", detailMap);
    data.put("user", userMap);
    return data;
  }

  public Map<String, Object> setLogin(String idcard, String phone, String isAuth) {
    if (!StringUtils.hasText(idcard) || !StringUtils.hasText(phone) || !StringUtils.hasText(isAuth)) {
      throw new AppSmsException(-1, "信息不完整");
    }
    if (!"reg".equals(isAuth) && !"login".equals(isAuth)) {
      throw new AppSmsException(-1, "信息不正确");
    }
    PlatformUserEntity user = userMapper.selectOne(
        new LambdaQueryWrapper<PlatformUserEntity>()
            .eq(PlatformUserEntity::getIdcard, idcard.trim())
            .eq(PlatformUserEntity::getDeleted, 0)
            .last("LIMIT 1"));
    if (user == null) {
      throw new AppSmsException(-1, "您还没在平台创建账号");
    }
    if (!phone.trim().equals(user.getPhone())) {
      user.setPhone(phone.trim());
      userMapper.updateById(user);
    }
    PlatformUserDetailEntity detail = detailMapper.selectOne(
        new LambdaQueryWrapper<PlatformUserDetailEntity>()
            .eq(PlatformUserDetailEntity::getIdcard, idcard.trim())
            .eq(PlatformUserDetailEntity::getDeleted, 0)
            .last("LIMIT 1"));
    if (detail == null) {
      detail = new PlatformUserDetailEntity();
      detail.setPhone(phone.trim());
      detail.setIdcard(idcard.trim());
      detail.setDeleted(0);
      detailMapper.insert(detail);
    } else if (!phone.trim().equals(detail.getPhone())) {
      detail.setPhone(phone.trim());
      detailMapper.updateById(detail);
    }

    tokenMapper.delete(new LambdaQueryWrapper<PlatformUserTokenEntity>()
        .eq(PlatformUserTokenEntity::getUserId, user.getId()));

    String token = sha1Secret(user.getPassword() + Instant.now().getEpochSecond());
    PlatformUserTokenEntity tokenEntity = new PlatformUserTokenEntity();
    tokenEntity.setUserId(user.getId());
    tokenEntity.setToken(token);
    tokenEntity.setAddtime(Instant.now().getEpochSecond());
    tokenMapper.insert(tokenEntity);

    Map<String, Object> detailMap = new LinkedHashMap<>();
    detailMap.put("id", detail.getId());
    detailMap.put("phone", detail.getPhone());
    detailMap.put("idcard", detail.getIdcard());
    detailMap.put("realname", detail.getRealname());
    detailMap.put("age", calcAgeFromIdCard(detail.getIdcard()));

    String realname = StringUtils.hasText(user.getRealname()) ? user.getRealname() : user.getPhone();
    Map<String, Object> userMap = new LinkedHashMap<>();
    userMap.put("id", user.getId());
    userMap.put("realname", realname);
    userMap.put("expert_id", user.getExpertId() != null ? user.getExpertId() : 0);

    Map<String, Object> data = new LinkedHashMap<>();
    data.put("token", token);
    data.put("detail", detailMap);
    data.put("user", userMap);
    return data;
  }

  private static Integer calcAgeFromIdCard(String idcard) {
    if (!StringUtils.hasText(idcard) || idcard.length() < 14) {
      return null;
    }
    try {
      String birthRaw = idcard.length() == 18
          ? idcard.substring(6, 14)
          : "19" + idcard.substring(6, 12);
      LocalDate birth = LocalDate.parse(birthRaw, DateTimeFormatter.BASIC_ISO_DATE);
      return Period.between(birth, LocalDate.now()).getYears();
    } catch (Exception e) {
      return null;
    }
  }

  static String md5(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("MD5");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder();
      for (byte b : hash) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("MD5 not available", e);
    }
  }

  static String sha1Secret(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-1");
      byte[] hash = digest.digest((TOKEN_SALT_PREFIX + input + TOKEN_SALT_SUFFIX)
          .getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder();
      for (byte b : hash) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-1 not available", e);
    }
  }
}
