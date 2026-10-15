package com.guoyu.platform.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.filter.TenantContextFilter;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.security.TenantScopedPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 租户一致性校验（P0-1，用户口径：零例外 403）。
 *
 * <p>背景：`X-Tenant-Id` 请求头由客户端任意指定，{@code TenantContextFilter} 直接采信它写入
 * {@link TenantContextHolder}，而 MyBatis-Plus 多租户拦截器正是按该值拼 `WHERE tenant_id`；
 * 认证主体的租户却来自数据库（token → user 记录）。两者此前从不比对，导致「A 租户的登录用户
 * 改 header 为 B，即可以 A 的角色读写 B 的数据」。
 *
 * <p>本过滤器在认证过滤器之后执行：存在带租户的主体时，要求上下文租户与主体租户一致，
 * 不一致直接 403 并记 WARN（认证、授权、权限校验路径必须打日志，见 AGENTS.md 编码规范）。
 *
 * <p>放行情形（不得误伤）：
 * <ul>
 *   <li>无认证主体 / 匿名主体 —— 匿名白名单由 SecurityConfig 与 TenantContextFilter 负责；</li>
 *   <li>主体类型未实现 {@link TenantScopedPrincipal}（未知主体）—— 只记 DEBUG 不阻断，
 *       避免新增主体类型时被本过滤器静默拒绝；</li>
 *   <li>主体租户为 null —— 未知，放行。</li>
 * </ul>
 *
 * <p>竞赛选手/考评主体（CompetitionPlayerPrincipal / CompetitionExamPrincipal）同样实现该接口，
 * 链路内已先做租户比对，此处不会重复拒绝，仅作为兜底。
 */
@Component
@Slf4j
public class TenantConsistencyFilter extends OncePerRequestFilter {

  private final ObjectMapper objectMapper;

  public TenantConsistencyFilter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    Long principalTenantId = resolvePrincipalTenantId();
    if (principalTenantId == null) {
      filterChain.doFilter(request, response);
      return;
    }

    Long contextTenantId = TenantContextHolder.getTenantId();
    if (contextTenantId == null) {
      // 上下文缺失（极端情况）：以主体租户为准补齐，避免后续 SQL 落到默认租户
      try {
        TenantContextHolder.setTenantId(principalTenantId);
        filterChain.doFilter(request, response);
      } finally {
        TenantContextHolder.clear();
      }
      return;
    }

    if (request.getAttribute(TenantContextFilter.TENANT_FORCED_ATTRIBUTE) != null) {
      // 租户由 TenantContextFilter 按路径强制（/appreviewsync、/appreviewai、/training/follow），
      // 非来自请求头；与按请求头构建的主体租户比对必然失真，跳过（生产实证旧 App header=1 被误伤 403）
      filterChain.doFilter(request, response);
      return;
    }

    if (!Objects.equals(contextTenantId, principalTenantId)) {
      log.warn(
          "Tenant consistency violation rejected: uri={}, method={}, headerTenantId={}, principalTenantId={}",
          request.getRequestURI(), request.getMethod(), contextTenantId, principalTenantId);
      writeForbidden(response);
      return;
    }

    filterChain.doFilter(request, response);
  }

  /** 取出当前主体租户；无主体 / 匿名 / 未知主体类型 / 租户未知时返回 null（表示放行）。 */
  private Long resolvePrincipalTenantId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      return null;
    }
    Object principal = authentication.getPrincipal();
    if (!(principal instanceof TenantScopedPrincipal tenantScoped)) {
      log.debug("Tenant consistency check skipped: principal type={}",
          principal == null ? "null" : principal.getClass().getName());
      return null;
    }
    return tenantScoped.getTenantId();
  }

  private void writeForbidden(HttpServletResponse response) throws IOException {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(
        response.getWriter(),
        ApiResponse.fail(ErrorCode.FORBIDDEN, "租户与登录身份不一致，拒绝访问")
    );
  }
}
