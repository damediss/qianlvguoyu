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
      "/api/actuator/info"
  );
  private static final Set<String> WHITE_PATH_PREFIXES = Set.of(
      "/api/appregistration/"
  );
  private static final Set<String> WHITE_METHOD_PATHS = Set.of(
      "POST:/api/auth/login",
      "POST:/api/auth/refresh",
      "GET:/api/auth/tenants"
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
