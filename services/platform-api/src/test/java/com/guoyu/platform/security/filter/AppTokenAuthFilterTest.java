package com.guoyu.platform.security.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.app.entity.PlatformUserTokenEntity;
import com.guoyu.platform.app.mapper.PlatformUserTokenMapper;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.trainingdepartment.entity.PlatformUserEntity;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserMapper;
import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 单库化 App 令牌鉴权：platform_user_token 是合并库唯一权威表，主库查不到即按未认证放行
 * （下游 EntryPoint 返 401），不再有任何 PHP 回退源（方案 Q4）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppTokenAuthFilterTest {

  @Mock
  private PlatformUserTokenMapper tokenMapper;
  @Mock
  private PlatformUserMapper userMapper;

  private AppTokenAuthFilter filter;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
    filter = new AppTokenAuthFilter(tokenMapper, userMapper);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private AtomicBoolean runFilter(MockHttpServletRequest request) throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean chainCalled = new AtomicBoolean(false);
    FilterChain chain = (req, res) -> chainCalled.set(true);
    filter.doFilter(request, response, chain);
    return chainCalled;
  }

  private static MockHttpServletRequest bearerRequest(String token) {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/appreviewsync/overview");
    if (token != null) {
      request.addHeader("Authorization", "Bearer " + token);
    }
    return request;
  }

  private static PlatformUserTokenEntity tokenRow(long userId) {
    PlatformUserTokenEntity e = new PlatformUserTokenEntity();
    e.setId(1L);
    e.setUserId(userId);
    e.setToken("tok123");
    return e;
  }

  private static PlatformUserEntity userRow(long id, int deleted) {
    PlatformUserEntity e = new PlatformUserEntity();
    e.setId(id);
    e.setPhone("13800138000");
    e.setRealname("张三");
    e.setDeleted(deleted);
    return e;
  }

  @Test
  void validTokenAuthenticatesFromMainDb() throws Exception {
    when(tokenMapper.selectOne(any())).thenReturn(tokenRow(42L));
    when(userMapper.selectById(42L)).thenReturn(userRow(42L, 0));

    AtomicBoolean chainCalled = runFilter(bearerRequest("tok123"));

    assertThat(chainCalled).isTrue();
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    assertThat(auth).isNotNull();
    PlatformUserPrincipal principal = (PlatformUserPrincipal) auth.getPrincipal();
    assertThat(principal.getUserId()).isEqualTo(42L);
    // 无 X-Tenant-Id header 时主体租户回落默认 10159
    assertThat(principal.getTenantId()).isEqualTo(10159L);
  }

  @Test
  void tokenMissPassesThroughUnauthenticated() throws Exception {
    // 主库查不到即放行（401 由下游 EntryPoint 决定），没有任何回退源可查
    when(tokenMapper.selectOne(any())).thenReturn(null);

    AtomicBoolean chainCalled = runFilter(bearerRequest("unknown"));

    assertThat(chainCalled).isTrue();
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(userMapper, never()).selectById(any(Long.class));
  }

  @Test
  void deletedUserIsNotAuthenticated() throws Exception {
    when(tokenMapper.selectOne(any())).thenReturn(tokenRow(42L));
    when(userMapper.selectById(42L)).thenReturn(userRow(42L, 1));

    AtomicBoolean chainCalled = runFilter(bearerRequest("tok123"));

    assertThat(chainCalled).isTrue();
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void noAuthorizationHeaderSkipsTokenLookup() throws Exception {
    AtomicBoolean chainCalled = runFilter(bearerRequest(null));

    assertThat(chainCalled).isTrue();
    verify(tokenMapper, never()).selectOne(any());
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }
}
