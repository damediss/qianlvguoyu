package com.guoyu.platform.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

  public static final String HEADER_NAME = "X-Request-Id";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    String traceId = resolveTraceId(request);
    try {
      TraceIdHolder.setTraceId(traceId);
      MDC.put("traceId", traceId);
      response.setHeader(HEADER_NAME, traceId);
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove("traceId");
      TraceIdHolder.clear();
    }
  }

  private String resolveTraceId(HttpServletRequest request) {
    String requestId = request.getHeader(HEADER_NAME);
    if (requestId == null || requestId.isBlank()) {
      return "req-" + UUID.randomUUID();
    }
    return requestId;
  }
}
