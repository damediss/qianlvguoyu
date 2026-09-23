package com.guoyu.platform.security.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.security.PlatformUserPrincipal;
import jakarta.servlet.FilterChain;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 修订方案 §2.1（P0-1）：租户一致性校验。
 * 覆盖：跨租户 403 / 同租户放行 / 匿名放行 / 上下文缺失补齐。
 */
class TenantConsistencyFilterTest {

  private final TenantConsistencyFilter filter = new TenantConsistencyFilter(new ObjectMapper());

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    TenantContextHolder.clear();
  }

  private static PlatformUserPrincipal principal(long tenantId) {
    return new PlatformUserPrincipal(
        1L, tenantId, "tester", "测试用户", "admin", List.of(), List.of(), null);
  }

  private void authenticate(Object principal) {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", List.of()));
  }

  @Test
  @DisplayName("★ A 租户主体 + B 租户 header → 403 且不继续过滤链")
  void rejectsMismatchedTenant() throws Exception {
    authenticate(principal(10159L));
    TenantContextHolder.setTenantId(1L);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/system/users");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    assertEquals(403, response.getStatus());
    verify(chain, never()).doFilter(any(), any());
  }

  @Test
  @DisplayName("主体租户与 header 一致 → 放行")
  void allowsMatchingTenant() throws Exception {
    authenticate(principal(10159L));
    TenantContextHolder.setTenantId(10159L);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/system/users");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
  }

  @Test
  @DisplayName("匿名主体（白名单路径）→ 放行，不阻断")
  void allowsAnonymous() throws Exception {
    SecurityContextHolder.getContext().setAuthentication(
        new AnonymousAuthenticationToken(
            "key", "anonymous", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
    TenantContextHolder.setTenantId(10159L);

    FilterChain chain = mock(FilterChain.class);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/training/sign-ups/1/detail");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
  }

  @Test
  @DisplayName("未知主体类型（未实现 TenantScopedPrincipal）→ 放行，避免误伤新增链路")
  void allowsUnknownPrincipalType() throws Exception {
    authenticate("some-string-principal");
    TenantContextHolder.setTenantId(10159L);

    FilterChain chain = mock(FilterChain.class);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/anything");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
  }

  @Test
  @DisplayName("上下文租户缺失 → 以主体租户补齐后放行")
  void fillsContextFromPrincipalWhenMissing() throws Exception {
    authenticate(principal(10159L));
    TenantContextHolder.clear();

    FilterChain chain = mock(FilterChain.class);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/anything");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
  }
}
