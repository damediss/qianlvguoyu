package com.guoyu.platform.trainingdepartment.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
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
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpPeopleSelfMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * 用户侧 10 亿区间隔离移除回归（单库化 Q10）：合并库 platform_user id 为 int AUTO，
 * requireSaaSLocalUser 已删除——小 id（原 PHP 来源）用户编辑/删除一律放行，不再按来源拦截。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlatformUserGuardTest {

  @Mock
  private PlatformUserMapper mapper;
  @Mock
  private PlatformUserDetailMapper detailMapper;
  @Mock
  private TrainPeopleListMapper peopleListMapper;
  @Mock
  private TrainSignUpPeopleSelfMapper signUpPeopleMapper;
  @Mock
  private OrgMapper orgMapper;
  @Mock
  private UserMapper userMapper;
  @Mock
  private TrainingOperatorScopeService operatorScopeService;

  private PlatformUserApplicationService service;

  @BeforeEach
  void setUp() {
    service = new PlatformUserApplicationService(
        mapper, detailMapper, peopleListMapper, signUpPeopleMapper, orgMapper, userMapper, operatorScopeService);
    when(peopleListMapper.selectList(any())).thenReturn(List.of());
    when(signUpPeopleMapper.selectList(any())).thenReturn(List.of());
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

  private static PlatformUserEntity entity(Long id) {
    PlatformUserEntity e = new PlatformUserEntity();
    e.setId(id);
    e.setRealname("原姓名");
    e.setIdcard("450103199001011234");
    e.setPhone("13800138000");
    e.setDeleted(0);
    return e;
  }

  @Test
  void updateSmallIdUserIsAllowed() {
    // 原 PHP 来源用户（id<10 亿）：区间守卫移除后可正常编辑
    try (MockedStatic<SecurityContext> ignored = mockCurrentAdmin()) {
      when(mapper.selectCount(any())).thenReturn(0L);
      when(mapper.selectOne(any())).thenReturn(entity(4695L));
      service.save(req(4695L));
      verify(mapper).updateById(any(PlatformUserEntity.class));
    }
  }

  @Test
  void deleteSmallIdUserIsAllowed() {
    try (MockedStatic<SecurityContext> ignored = mockCurrentAdmin()) {
      when(mapper.selectOne(any())).thenReturn(entity(4695L));
      service.delete(4695L);
      verify(mapper).deleteById(4695L);
    }
  }

  @Test
  void deleteNonExistingUserStillRejected() {
    // 守卫移除≠放开存在性校验：记录不存在仍报业务异常（防误删语义不变）
    try (MockedStatic<SecurityContext> ignored = mockCurrentAdmin()) {
      when(mapper.selectOne(any())).thenReturn(null);
      assertThatThrownBy(() -> service.delete(4695L))
          .isInstanceOf(BusinessException.class);
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
