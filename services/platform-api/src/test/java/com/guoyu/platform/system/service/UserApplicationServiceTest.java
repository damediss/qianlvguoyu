package com.guoyu.platform.system.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.system.dto.UserCreateReq;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.system.mapper.RoleMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import com.guoyu.platform.system.mapper.UserRoleMapper;
import com.guoyu.platform.system.mapper.UserRoleRelationMapper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserApplicationServiceTest {

  private final UserMapper userMapper = Mockito.mock(UserMapper.class);
  private final OrgMapper orgMapper = Mockito.mock(OrgMapper.class);
  private final RoleMapper roleMapper = Mockito.mock(RoleMapper.class);
  private final UserRoleMapper userRoleMapper = Mockito.mock(UserRoleMapper.class);
  private final UserRoleRelationMapper userRoleRelationMapper = Mockito.mock(UserRoleRelationMapper.class);
  private final PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
  private final UserApplicationService service = new UserApplicationService(
      userMapper,
      orgMapper,
      roleMapper,
      userRoleMapper,
      userRoleRelationMapper,
      passwordEncoder
  );

  @BeforeEach
  void setUp() {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        1000L,
        1L,
        "admin",
        "平台管理员",
        "admin",
        List.of("PLATFORM_ADMIN"),
        List.of("user:create")
    );
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities())
    );
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void createRejectsDuplicateUsername() {
    when(orgMapper.exists(any(Wrapper.class))).thenReturn(true);
    when(roleMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
    when(userMapper.exists(any(Wrapper.class))).thenReturn(true);

    UserCreateReq req = new UserCreateReq(
        100L,
        "admin",
        "Admin@123456",
        "平台管理员",
        "admin",
        null,
        null,
        "enabled",
        List.of(2000L)
    );

    assertThatThrownBy(() -> service.create(req))
        .isInstanceOf(BusinessException.class)
        .hasMessage("Username already exists");
  }
}
