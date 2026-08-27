package com.guoyu.platform.common.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.guoyu.platform.common.tenant.TenantContextHolder;
import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * 单库化 Q7 第一轨：/api/appreviewsync/** 强制租户 10159。
 * 旧版 App 仍发 X-Tenant-Id=1，若按 header 过滤会查空/写错/撞 uk_ai_review_plan_user_exam 唯一键；
 * header 缺失也不再 400（服务端兜底，App 发版前即可上线）。
 */
class TenantContextFilterTest {

  private final TenantContextFilter filter = new TenantContextFilter();

  @AfterEach
  void tearDown() {
    TenantContextHolder.clear();
  }

  /** 执行请求并返回（链内所见租户, 响应状态, 链是否被调用） */
  private record RunResult(Long tenantInChain, int status, boolean chainCalled) {
  }

  private RunResult run(String method, String uri, String tenantHeader) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
    if (tenantHeader != null) {
      request.addHeader("X-Tenant-Id", tenantHeader);
    }
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<Long> seen = new AtomicReference<>();
    AtomicBoolean called = new AtomicBoolean(false);
    FilterChain chain = (req, res) -> {
      called.set(true);
      seen.set(TenantContextHolder.getTenantId());
    };
    filter.doFilter(request, response, chain);
    return new RunResult(seen.get(), response.getStatus(), called.get());
  }

  @Test
  void reviewSyncPathIgnoresLegacyHeaderAndForcesDefault() throws Exception {
    RunResult r = run("POST", "/api/appreviewsync/overview", "1");
    assertThat(r.chainCalled()).isTrue();
    assertThat(r.tenantInChain()).isEqualTo(10159L);
    assertThat(r.status()).isEqualTo(200);
    // 出链后必须清理，不污染后续复用线程
    assertThat(TenantContextHolder.getTenantId()).isNull();
  }

  @Test
  void reviewSyncPathWithNewHeaderStillForcedToDefault() throws Exception {
    RunResult r = run("POST", "/api/appreviewsync/overview", "10159");
    assertThat(r.chainCalled()).isTrue();
    assertThat(r.tenantInChain()).isEqualTo(10159L);
  }

  @Test
  void reviewSyncPathWithoutHeaderNoLonger400() throws Exception {
    RunResult r = run("POST", "/api/appreviewsync/answer", null);
    assertThat(r.chainCalled()).isTrue();
    assertThat(r.tenantInChain()).isEqualTo(10159L);
    assertThat(r.status()).isEqualTo(200);
  }

  @Test
  void reviewSyncExactPrefixAlsoForced() throws Exception {
    RunResult r = run("POST", "/api/appreviewsync", null);
    assertThat(r.chainCalled()).isTrue();
    assertThat(r.tenantInChain()).isEqualTo(10159L);
  }

  @Test
  void normalPathWithoutHeaderStill400() throws Exception {
    RunResult r = run("GET", "/api/system/user/page", null);
    assertThat(r.chainCalled()).isFalse();
    assertThat(r.status()).isEqualTo(400);
  }

  @Test
  void normalPathWithHeaderUsesHeaderTenant() throws Exception {
    RunResult r = run("GET", "/api/system/user/page", "10159");
    assertThat(r.chainCalled()).isTrue();
    assertThat(r.tenantInChain()).isEqualTo(10159L);
  }

  @Test
  void whitePrefixWithoutHeaderFallsBackToDefault() throws Exception {
    RunResult r = run("POST", "/api/appauth/login", null);
    assertThat(r.chainCalled()).isTrue();
    assertThat(r.tenantInChain()).isEqualTo(10159L);
  }
}
