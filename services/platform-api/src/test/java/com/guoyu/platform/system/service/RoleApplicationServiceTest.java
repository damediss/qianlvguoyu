package com.guoyu.platform.system.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.system.entity.RoleEntity;
import com.guoyu.platform.system.mapper.MenuMapper;
import com.guoyu.platform.system.mapper.PermissionMapper;
import com.guoyu.platform.system.mapper.RoleMapper;
import com.guoyu.platform.system.mapper.RoleMenuMapper;
import com.guoyu.platform.system.mapper.RoleMenuRelationMapper;
import com.guoyu.platform.system.mapper.RolePermissionRelationMapper;
import com.guoyu.platform.system.mapper.UserRoleRelationMapper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class RoleApplicationServiceTest {

  private final RoleMapper roleMapper = Mockito.mock(RoleMapper.class);
  private final MenuMapper menuMapper = Mockito.mock(MenuMapper.class);
  private final PermissionMapper permissionMapper = Mockito.mock(PermissionMapper.class);
  private final RoleMenuMapper roleMenuMapper = Mockito.mock(RoleMenuMapper.class);
  private final RoleMenuRelationMapper roleMenuRelationMapper = Mockito.mock(RoleMenuRelationMapper.class);
  private final RolePermissionRelationMapper rolePermissionRelationMapper = Mockito.mock(RolePermissionRelationMapper.class);
  private final UserRoleRelationMapper userRoleRelationMapper = Mockito.mock(UserRoleRelationMapper.class);
  private final RoleApplicationService service = new RoleApplicationService(
      roleMapper,
      menuMapper,
      permissionMapper,
      roleMenuMapper,
      roleMenuRelationMapper,
      rolePermissionRelationMapper,
      userRoleRelationMapper
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
        List.of("role:delete")
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
  void deleteRejectsRoleUsedByUsers() {
    RoleEntity role = new RoleEntity();
    role.setId(2000L);
    role.setTenantId(1L);
    role.setCode("PLATFORM_ADMIN");
    role.setName("平台管理员");
    role.setRoleType("platform");
    role.setStatus("enabled");
    role.setDeleted(0);

    when(roleMapper.selectOne(any(Wrapper.class))).thenReturn(role);
    when(userRoleRelationMapper.exists(any(Wrapper.class))).thenReturn(true);

    assertThatThrownBy(() -> service.delete(2000L))
        .isInstanceOf(BusinessException.class)
        .hasMessage("Role is used by users");
  }
}
