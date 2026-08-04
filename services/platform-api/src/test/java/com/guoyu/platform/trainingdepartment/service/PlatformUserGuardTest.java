package com.guoyu.platform.trainingdepartment.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.system.mapper.UserMapper;
import com.guoyu.platform.trainingdepartment.dto.PlatformUserSaveReq;
import com.guoyu.platform.trainingdepartment.entity.PlatformUserEntity;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserDetailMapper;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** PHP 来源用户保护（鉴权方案 §3.5.4）：编辑/删除 id<10 亿的用户一律拒绝 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlatformUserGuardTest {

  @Mock
  private PlatformUserMapper mapper;
  @Mock
  private PlatformUserDetailMapper detailMapper;
  @Mock
  private OrgMapper orgMapper;
  @Mock
  private UserMapper userMapper;
  @Mock
  private TrainingOperatorScopeService operatorScopeService;

  private PlatformUserApplicationService service;

  @BeforeEach
  void setUp() {
    service = new PlatformUserApplicationService(mapper, detailMapper, orgMapper, userMapper, operatorScopeService);
  }

  private MockedStatic<SecurityContext> mockCurrentAdmin() {
    PlatformUserPrincipal principal = mock(PlatformUserPrincipal.class);
    when(principal.getUserId()).thenReturn(1L);
    MockedStatic<SecurityContext> mocked = mockStatic(SecurityContext.class);
    mocked.when(SecurityContext::currentUser).thenReturn(principal);
    UserEntity admin = new UserEntity();
    admin.setOrgId(7L);
    when(userMapper.selectById(1L)).thenReturn(admin);
    return mocked;
  }

  private static PlatformUserSaveReq req(Long id) {
    PlatformUserSaveReq req = new PlatformUserSaveReq();
    req.setId(id);
    req.setRealname("测试学员");
    req.setIdcard("450103199001011234");
    req.setPhone("13800138000");
    return req;
  }

  @Test
  void updatePhpSourcedUserIsRejected() {
    try (MockedStatic<SecurityContext> ignored = mockCurrentAdmin()) {
      when(mapper.selectCount(any())).thenReturn(0L);
      assertThatThrownBy(() -> service.save(req(4695L)))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("该用户来源于工匠基地平台，请前往原平台维护");
      verify(mapper, never()).updateById(any(PlatformUserEntity.class));
    }
  }

  @Test
  void deletePhpSourcedUserIsRejected() {
    assertThatThrownBy(() -> service.delete(4695L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("该用户来源于工匠基地平台，请前往原平台维护");
    verify(mapper, never()).deleteById(any(Long.class));
  }

  @Test
  void deleteSaaSLocalUserIsAllowed() {
    try (MockedStatic<SecurityContext> ignored = mockCurrentAdmin()) {
      PlatformUserEntity local = new PlatformUserEntity();
      local.setId(2_000_000_000L);
      when(mapper.selectOne(any())).thenReturn(local);
      service.delete(2_000_000_000L);
      verify(mapper).deleteById(2_000_000_000L);
    }
  }

  @Test
  void insertNewUserIsAllowed() {
    try (MockedStatic<SecurityContext> ignored = mockCurrentAdmin()) {
      when(mapper.selectCount(any())).thenReturn(0L);
      when(mapper.insert(any(PlatformUserEntity.class))).thenReturn(1);
      service.save(req(null));
      verify(mapper).insert(any(PlatformUserEntity.class));
    }
  }
}
