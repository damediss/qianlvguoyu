package com.guoyu.platform.common.filter;

import com.guoyu.platform.common.tenant.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Slf4j
public class TenantContextFilter extends OncePerRequestFilter {

  private static final String TENANT_HEADER = "X-Tenant-Id";
  private static final Set<String> WHITE_PATHS = Set.of(
      "/api/actuator/health",
      "/api/actuator/info",
      // OIDC: AS 链未认证时 LoginUrlAuthenticationEntryPoint 302 → /api/login，
      // 浏览器跳转不带 X-Tenant-Id，此处放行避免被拦截返回 400
      "/api/login"
  );
  private static final Set<String> WHITE_PATH_PREFIXES = Set.of(
      "/api/appregistration/",
      "/api/appsms/",
      "/api/appauth/",
      "/api/appuser/",
      "/api/appuserv4/",
      "/api/appsmart/",
      "/api/appcoach/",
      "/api/apphome/",
      "/api/exam/files/",
      // 培训管理·扫码信息收集 - 测试专用
      "/api/training/sign-ups/",
      // App 身份证 OCR / 证件照上传（扫码采集页调用，无管理端登录态）
      "/api/training/app/",
      "/api/training/public/research-collection/",
      "/api/collect-test.html"
  );
  private static final Set<String> WHITE_METHOD_PATHS = Set.of(
      "POST:/api/auth/login",
      "POST:/api/auth/refresh",
      "GET:/api/auth/tenants",
      "GET:/api/auth/captcha/generate",
      "POST:/api/auth/captcha/verify",
      "GET:/api/competition/player/entry/resolve",
      "POST:/api/competition/player/auth/login"
  );

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    String path = request.getRequestURI();
    String method = request.getMethod();
    String tenantIdHeader = request.getHeader(TENANT_HEADER);
    if ((tenantIdHeader == null || tenantIdHeader.isBlank())
        && path.equals("/api/ws/practical-scoring")) {
      tenantIdHeader = request.getParameter("tenantId");
    }

    boolean isWhite = WHITE_PATHS.contains(path)
        || WHITE_METHOD_PATHS.contains(method + ":" + path)
        || WHITE_PATH_PREFIXES.stream().anyMatch(path::startsWith);

    if (isWhite) {
      if (tenantIdHeader == null || tenantIdHeader.isBlank()) {
        TenantContextHolder.setTenantId(1L);
      } else {
        TenantContextHolder.setTenantId(Long.valueOf(tenantIdHeader.trim()));
      }
      try {
        filterChain.doFilter(request, response);
      } finally {
        TenantContextHolder.clear();
      }
      return;
    }

    if (tenantIdHeader == null || tenantIdHeader.isBlank()) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.setContentType("application/json;charset=UTF-8");
      response.getWriter().write("{\"code\":\"400\",\"message\":\"Missing X-Tenant-Id header\"}");
      return;
    }

    try {
      Long tenantId = Long.valueOf(tenantIdHeader.trim());
      TenantContextHolder.setTenantId(tenantId);
      filterChain.doFilter(request, response);
    } catch (NumberFormatException e) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.setContentType("application/json;charset=UTF-8");
      response.getWriter().write("{\"code\":\"400\",\"message\":\"Invalid X-Tenant-Id format\"}");
    } finally {
      TenantContextHolder.clear();
    }
  }
}
