package com.guoyu.platform.app.sms;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.app.entity.PlatformSmsEntity;
import com.guoyu.platform.app.mapper.PlatformSmsMapper;
import java.security.SecureRandom;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 对齐 PHP AppsmsController + AppauthController 短信校验 */
@Service
@Slf4j
public class AppSmsApplicationService {

  /** 对齐 PHP 测试万能验证码 */
  public static final String MASTER_CODE = "95274501";

  private static final int CODE_TTL_SECONDS = 300;

  private final PlatformSmsMapper smsMapper;
  private final boolean smsEnabled;

  public AppSmsApplicationService(
      PlatformSmsMapper smsMapper,
      @Value("${platform.sms.enabled:false}") boolean smsEnabled) {
    this.smsMapper = smsMapper;
    this.smsEnabled = smsEnabled;
  }

  public void sendCode(String phone) {
    if (!StringUtils.hasText(phone)) {
      throw new AppSmsException(-1, "手机号不能为空");
    }
    String normalized = phone.trim();
    String code = randomCode();
    PlatformSmsEntity entity = new PlatformSmsEntity();
    entity.setPhone(normalized);
    entity.setSms(code);
    entity.setAddtime(Instant.now().getEpochSecond());
    smsMapper.insert(entity);

    if (smsEnabled) {
      // 生产环境可接入阿里云短信；当前默认仅入库并在日志输出（便于联调）
      log.warn("platform.sms.enabled=true 但未配置阿里云 SDK，验证码已入库: phone={} code={}", normalized, code);
    } else {
      log.info("短信验证码（开发模式）phone={} code={}", normalized, code);
    }
  }

  public void verifyCode(String phone, String code) {
    if (!StringUtils.hasText(phone)) {
      throw new AppSmsException(-1, "手机号不能为空");
    }
    if (!StringUtils.hasText(code)) {
      throw new AppSmsException(-1, "短信验证码不能为空");
    }
    if (MASTER_CODE.equals(code.trim())) {
      return;
    }
    long since = Instant.now().getEpochSecond() - CODE_TTL_SECONDS;
    PlatformSmsEntity sms = smsMapper.selectOne(
        new LambdaQueryWrapper<PlatformSmsEntity>()
            .eq(PlatformSmsEntity::getPhone, phone.trim())
            .eq(PlatformSmsEntity::getSms, code.trim())
            .gt(PlatformSmsEntity::getAddtime, since)
            .orderByDesc(PlatformSmsEntity::getAddtime)
            .last("LIMIT 1"));
    if (sms == null) {
      throw new AppSmsException(-1, "短信验证码不正确或已过期");
    }
  }

  private static String randomCode() {
    SecureRandom random = new SecureRandom();
    int n = 100000 + random.nextInt(900000);
    return String.valueOf(n);
  }
}
