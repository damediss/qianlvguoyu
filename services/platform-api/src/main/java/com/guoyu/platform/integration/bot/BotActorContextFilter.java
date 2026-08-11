package com.guoyu.platform.integration.bot;

import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.security.PlatformUserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Verifies hidden BotActorContext headers before application controllers see them.
 * Invalid context is never silently ignored; an unsigned context cannot affect audit or delivery.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
@Slf4j
public class BotActorContextFilter extends OncePerRequestFilter {

  private final BotActorContextCodec codec;

  public BotActorContextFilter(BotActorContextCodec codec) {
    this.codec = codec;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    String encoded = request.getHeader(BotActorContextCodec.CONTEXT_HEADER);
    String signature = request.getHeader(BotActorContextCodec.SIGNATURE_HEADER);
    if ((encoded == null || encoded.isBlank()) && (signature == null || signature.isBlank())) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      BotActorContext context = codec.decodeAndVerify(encoded, signature);
      verifyTenant(request, context);
      verifyRequestId(request, context);
      BotActorContextHolder.set(context);
      filterChain.doFilter(request, response);
    } catch (com.guoyu.platform.common.exception.BusinessException ex) {
      log.warn("Rejected invalid bot actor context: path={}, code={}, message={}",
          request.getRequestURI(), ex.getCode(), ex.getMessage());
      if (!response.isCommitted()) {
        response.setStatus(Integer.parseInt(ex.getCode()));
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"" + ex.getCode() + "\",\"message\":\"Bot actor context rejected\"}");
      }
    } finally {
      BotActorContextHolder.clear();
    }
  }

  private void verifyTenant(HttpServletRequest request, BotActorContext context) {
    String tenantHeader = request.getHeader("X-Tenant-Id");
    if (tenantHeader != null && !tenantHeader.isBlank()) {
      try {
        if (!context.tenantId().equals(Long.valueOf(tenantHeader.trim()))) {
          throw new com.guoyu.platform.common.exception.BusinessException(
              com.guoyu.platform.common.exception.ErrorCode.FORBIDDEN,
              "Bot actor tenant does not match request tenant");
        }
      } catch (NumberFormatException ex) {
        throw new com.guoyu.platform.common.exception.BusinessException(
            com.guoyu.platform.common.exception.ErrorCode.BAD_REQUEST,
            "Invalid request tenant");
      }
    }
    Long currentTenant = TenantContextHolder.getTenantId();
    if (currentTenant != null && !context.tenantId().equals(currentTenant)) {
      throw new com.guoyu.platform.common.exception.BusinessException(
          com.guoyu.platform.common.exception.ErrorCode.FORBIDDEN,
          "Bot actor tenant does not match tenant context");
    }
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof PlatformUserPrincipal principal
        && !context.tenantId().equals(principal.getTenantId())) {
      throw new com.guoyu.platform.common.exception.BusinessException(
          com.guoyu.platform.common.exception.ErrorCode.FORBIDDEN,
          "Bot actor tenant does not match authenticated service account");
    }
  }

  private void verifyRequestId(HttpServletRequest request, BotActorContext context) {
    String requestId = request.getHeader("X-Request-Id");
    if (requestId != null && !requestId.isBlank() && !requestId.equals(context.requestId())) {
      throw new com.guoyu.platform.common.exception.BusinessException(
          com.guoyu.platform.common.exception.ErrorCode.FORBIDDEN,
          "Bot actor request_id does not match request trace");
    }
  }
}
