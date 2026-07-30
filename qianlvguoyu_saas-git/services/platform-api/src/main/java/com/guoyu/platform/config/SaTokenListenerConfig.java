package com.guoyu.platform.config;

import cn.dev33.satoken.listener.SaTokenListener;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class SaTokenListenerConfig implements SaTokenListener {

  @Override
  public void doLogin(String loginType, Object loginId, String tokenValue, SaLoginParameter params) {
    log.info("Sa-Token login: loginType={}, loginId={}", loginType, loginId);
  }

  @Override
  public void doLogout(String loginType, Object loginId, String tokenValue) {
    log.info("Sa-Token logout: loginType={}, loginId={}", loginType, loginId);
  }

  @Override
  public void doKickout(String loginType, Object loginId, String tokenValue) {
    log.info("Sa-Token kickout: loginType={}, loginId={}", loginType, loginId);
  }

  @Override
  public void doReplaced(String loginType, Object loginId, String tokenValue) {
    log.info("Sa-Token replaced: loginType={}, loginId={}", loginType, loginId);
  }

  @Override
  public void doDisable(String loginType, Object loginId, String service, int level, long disableTime) {
    log.info("Sa-Token disable: loginType={}, loginId={}, service={}, level={}", loginType, loginId, service, level);
  }

  @Override
  public void doUntieDisable(String loginType, Object loginId, String service) {
    log.info("Sa-Token untie disable: loginType={}, loginId={}, service={}", loginType, loginId, service);
  }

  @Override
  public void doOpenSafe(String loginType, String tokenValue, String service, long safeTime) {
  }

  @Override
  public void doCloseSafe(String loginType, String tokenValue, String service) {
  }

  @Override
  public void doCreateSession(String id) {
  }

  @Override
  public void doLogoutSession(String id) {
  }

  @Override
  public void doRenewTimeout(String tokenValue, Object loginId, long timeout) {
  }
}