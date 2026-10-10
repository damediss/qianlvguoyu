package com.guoyu.platform.competition.exam.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Debug-only reverse proxy so a Pad on the Windows hotspot can load Vite H5.
 *
 * <p>Hotspot clients can reach Java {@code :8080} but not Node {@code :5173}. Enable with
 * {@code formal-exam.hotspot-vite-proxy-target=http://127.0.0.1:5173} and start the API with
 * {@code --server.servlet.context-path=/ --spring.mvc.servlet.path=/api} so {@code /formal-exam}
 * is visible at the HTTP root. {@code /api/**} is never proxied.
 */
@Configuration
@ConditionalOnProperty(name = "formal-exam.hotspot-vite-proxy-target")
public class FormalExamHotspotViteProxyConfiguration {

  private static final Logger log = LoggerFactory.getLogger(FormalExamHotspotViteProxyConfiguration.class);
  private static final Set<String> HOP_BY_HOP = Set.of(
      "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
      "te", "trailers", "transfer-encoding", "upgrade", "host", "content-length");

  @Bean
  FilterRegistrationBean<FormalExamHotspotViteProxyFilter> formalExamHotspotViteProxyFilter(
      Environment environment) {
    String target = environment.getProperty("formal-exam.hotspot-vite-proxy-target", "").trim();
    FormalExamHotspotViteProxyFilter filter = new FormalExamHotspotViteProxyFilter(target);
    FilterRegistrationBean<FormalExamHotspotViteProxyFilter> registration =
        new FilterRegistrationBean<>(filter);
    registration.addUrlPatterns("/*");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    log.warn("Formal exam hotspot Vite proxy enabled, non-/api traffic -> {}", target);
    return registration;
  }

  static final class FormalExamHotspotViteProxyFilter extends OncePerRequestFilter {

    private final String target;

    FormalExamHotspotViteProxyFilter(String target) {
      this.target = target.endsWith("/") ? target.substring(0, target.length() - 1) : target;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
      String uri = request.getRequestURI();
      return uri == null || uri.equals("/api") || uri.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
      StringBuilder loc = new StringBuilder(target)
          .append(request.getRequestURI());
      if (request.getQueryString() != null && !request.getQueryString().isBlank()) {
        loc.append('?').append(request.getQueryString());
      }
      HttpURLConnection conn = (HttpURLConnection) URI.create(loc.toString()).toURL().openConnection();
      conn.setInstanceFollowRedirects(false);
      conn.setConnectTimeout(3_000);
      conn.setReadTimeout(30_000);
      conn.setRequestMethod(request.getMethod());
      conn.setDoInput(true);
      copyRequestHeaders(request, conn);
      if (mayHaveBody(request.getMethod())) {
        conn.setDoOutput(true);
        try (InputStream in = request.getInputStream(); OutputStream out = conn.getOutputStream()) {
          in.transferTo(out);
        }
      }
      int status = conn.getResponseCode();
      response.setStatus(status);
      copyResponseHeaders(conn, response);
      InputStream body = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
      if (body != null) {
        try (InputStream in = body; OutputStream out = response.getOutputStream()) {
          in.transferTo(out);
        }
      }
      conn.disconnect();
    }

    private static boolean mayHaveBody(String method) {
      return !"GET".equalsIgnoreCase(method)
          && !"HEAD".equalsIgnoreCase(method)
          && !"DELETE".equalsIgnoreCase(method);
    }

    private static void copyRequestHeaders(HttpServletRequest request, HttpURLConnection conn) {
      List<String> names = java.util.Collections.list(request.getHeaderNames());
      for (String name : names) {
        if (name == null || HOP_BY_HOP.contains(name.toLowerCase())) {
          continue;
        }
        List<String> values = java.util.Collections.list(request.getHeaders(name));
        for (String value : values) {
          conn.addRequestProperty(name, value);
        }
      }
    }

    private static void copyResponseHeaders(HttpURLConnection conn, HttpServletResponse response) {
      conn.getHeaderFields().forEach((name, values) -> {
        if (name == null || values == null || HOP_BY_HOP.contains(name.toLowerCase())) {
          return;
        }
        if (HttpHeaders.SET_COOKIE.equalsIgnoreCase(name) || HttpHeaders.SET_COOKIE2.equalsIgnoreCase(name)) {
          return;
        }
        for (String value : values) {
          response.addHeader(name, value);
        }
      });
    }
  }
}
