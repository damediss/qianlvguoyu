package com.guoyu.platform.competition.exam.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 20)
public class FormalExamObservabilityFilter extends OncePerRequestFilter {

  private static final List<PrefixOp> OPS = List.of(
      new PrefixOp("GET:/api/competition/formal-exam/player/", "/context", "player.context"),
      new PrefixOp("GET:/api/competition/formal-exam/player/", "/status", "player.status"),
      new PrefixOp("POST:/api/competition/formal-exam/player/", "/answers/sync", "player.sync"),
      new PrefixOp("GET:/api/competition/formal-exam-sessions/", "/candidates", "admin.candidates"),
      new PrefixOp("GET:/api/competition/formal-exam-sessions/", "", "admin.session"));

  private final FormalExamMetrics metrics;

  public FormalExamObservabilityFilter(FormalExamMetrics metrics) {
    this.metrics = metrics;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return operationOf(request) == null;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String operation = operationOf(request);
    FormalExamSqlProbe.enter();
    Instant started = Instant.now();
    try {
      filterChain.doFilter(request, response);
    } finally {
      int sqlCount = FormalExamSqlProbe.exit();
      metrics.recordHttp(
          operation,
          outcomeOf(response.getStatus()),
          sqlCount,
          Duration.between(started, Instant.now()));
    }
  }

  private static String operationOf(HttpServletRequest request) {
    String method = request.getMethod();
    String uri = request.getRequestURI();
    if (method == null || uri == null) {
      return null;
    }
    String key = method + ":" + uri;
    for (PrefixOp op : OPS) {
      if (key.startsWith(op.methodPrefix()) && (op.suffix().isEmpty() || uri.endsWith(op.suffix()))) {
        if ("admin.session".equals(op.operation()) && uri.contains("/candidates")) {
          continue;
        }
        return op.operation();
      }
    }
    return null;
  }

  private static String outcomeOf(int status) {
    if (status < 400) {
      return "ok";
    }
    if (status == 401) {
      return "unauthorized";
    }
    if (status == 403) {
      return "forbidden";
    }
    if (status == 409) {
      return "conflict";
    }
    if (status >= 500) {
      return "server_error";
    }
    return "client_error";
  }

  private record PrefixOp(String methodPrefix, String suffix, String operation) {
  }
}
