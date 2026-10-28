package com.guoyu.platform.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.guoyu.platform.auth.dto.LoginReq;
import com.guoyu.platform.auth.support.LoginFailureGuard;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.system.entity.TenantEntity;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.MenuMapper;
import com.guoyu.platform.system.mapper.RoleMenuMapper;
import com.guoyu.platform.system.mapper.TenantMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import com.guoyu.platform.system.mapper.UserRoleMapper;
import com.guoyu.platform.trainingdepartment.service.EnterpriseFeatureConfigService;
import com.guoyu.platform.trainingdepartment.service.TrainingOperatorScopeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 登录失败分级文案（方案A口径）：模糊警示不报次数/时长，锁定后报剩余分钟。
 */
class AuthApplicationServiceLoginFailureMessageTest {

  private final TenantMapper tenantMapper = mock(TenantMapper.class);
  private final UserMapper userMapper = mock(UserMapper.class);
  private final UserRoleMapper userRoleMapper = mock(UserRoleMapper.class);
  private final RoleMenuMapper roleMenuMapper = mock(RoleMenuMapper.class);
  private final MenuMapper menuMapper = mock(MenuMapper.class);
  private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
  private final TrainingOperatorScopeService trainingOperatorScopeService =
      mock(TrainingOperatorScopeService.class);
  private final EnterpriseFeatureConfigService enterpriseFeatureConfigService =
      mock(EnterpriseFeatureConfigService.class);
  private final LoginFailureGuard loginFailureGuard = mock(LoginFailureGuard.class);

  private AuthApplicationService service(int warnFailures) {
    return new AuthApplicationService(
        tenantMapper, userMapper, userRoleMapper, roleMenuMapper, menuMapper,
        passwordEncoder, trainingOperatorScopeService, enterpriseFeatureConfigService,
        loginFailureGuard, warnFailures);
  }

  private static final LoginReq REQ = new LoginReq("demo", "admin", "wrong-password");

  @Test
  @DisplayName("锁定中登录：429 报剩余分钟（600 秒 → 约 10 分钟）")
  void lockedLoginReportsRemainingMinutes() {
    when(loginFailureGuard.remainingLockSeconds("demo", "admin")).thenReturn(600L);
    BusinessException ex = assertThrows(BusinessException.class, () -> service(3).login(REQ));
    assertEquals(ErrorCode.TOO_MANY_REQUESTS, ex.getCode());
    assertEquals("账号已临时锁定，请约10分钟后再试", ex.getMessage());
  }

  @Test
  @DisplayName("达告警阈值：401 模糊警示，不报剩余次数与锁定时长")
  void warnMessageOnThresholdWithoutNumbers() {
    when(loginFailureGuard.remainingLockSeconds(any(), any())).thenReturn(0L);
    when(tenantMapper.selectOne(any(Wrapper.class))).thenReturn(null);
    when(loginFailureGuard.recordFailure("demo", "admin"))
        .thenReturn(new LoginFailureGuard.FailureOutcome(3, false));

    BusinessException ex = assertThrows(BusinessException.class, () -> service(3).login(REQ));
    assertEquals(ErrorCode.UNAUTHORIZED, ex.getCode());
    assertEquals("密码错误次数较多，再次失败账号将被临时锁定，请确认密码后重试", ex.getMessage());
  }

  @Test
  @DisplayName("未达告警阈值：401 通用文案")
  void genericMessageBelowThreshold() {
    when(loginFailureGuard.remainingLockSeconds(any(), any())).thenReturn(0L);
    when(tenantMapper.selectOne(any(Wrapper.class))).thenReturn(null);
    when(loginFailureGuard.recordFailure("demo", "admin"))
        .thenReturn(new LoginFailureGuard.FailureOutcome(1, false));

    BusinessException ex = assertThrows(BusinessException.class, () -> service(3).login(REQ));
    assertEquals(ErrorCode.UNAUTHORIZED, ex.getCode());
    assertEquals("Invalid username or password", ex.getMessage());
  }

  @Test
  @DisplayName("密码错误触发锁定：本次直接 429 报锁定分钟（900 秒 → 约 15 分钟）")
  void passwordMismatchJustLockedReturns429() {
    when(loginFailureGuard.remainingLockSeconds(any(), any())).thenReturn(0L);
    TenantEntity tenant = new TenantEntity();
    tenant.setId(10159L);
    when(tenantMapper.selectOne(any(Wrapper.class))).thenReturn(tenant);
    UserEntity user = new UserEntity();
    user.setId(1L);
    user.setPasswordHash("hash");
    when(userMapper.selectOne(any(Wrapper.class))).thenReturn(user);
    when(passwordEncoder.matches(any(), any())).thenReturn(false);
    when(loginFailureGuard.recordFailure("demo", "admin"))
        .thenReturn(new LoginFailureGuard.FailureOutcome(5, true));
    when(loginFailureGuard.lockSeconds()).thenReturn(900L);

    BusinessException ex = assertThrows(BusinessException.class, () -> service(3).login(REQ));
    assertEquals(ErrorCode.TOO_MANY_REQUESTS, ex.getCode());
    assertEquals("账号已临时锁定，请约15分钟后再试", ex.getMessage());
  }
}
