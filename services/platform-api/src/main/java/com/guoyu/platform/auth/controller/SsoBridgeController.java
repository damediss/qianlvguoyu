package com.guoyu.platform.auth.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.guoyu.platform.security.PlatformUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/**
 * SSO 桥接控制器。
 * <p>
 * 前端登录后用 Bearer Token 调本端点：
 * <ol>
 *   <li>SaTokenBridgeFilter 已完成认证，SecurityContext 里有 PlatformUserPrincipal</li>
 *   <li>创建 HTTP session 并写入 SecurityContext（app 链是 STATELESS，必须手动 stash）</li>
 *   <li>服务端调 WeKnora /oidc/url 拿 authorize_url（含 WeKnora 生成的 state）</li>
 *   <li>返回 {authorizeUrl}；前端拿到后 window.location 跳转，浏览器带 JSESSIONID 命中 /oauth2/authorize</li>
 * </ol>
 */
@Slf4j
@RestController
public class SsoBridgeController {

  /** WeKnora 后端 /oidc/url 地址（WeKnora 发布在宿主 :8088，本服务走 localhost 即可） */
  private static final String WEKNORA_OIDC_URL =
      "http://localhost:8088/api/v1/auth/oidc/url"
          + "?redirect_uri=http://43.138.163.7:8088/api/v1/auth/oidc/callback";

  @GetMapping("/sso/weknora")
  public Map<String, String> bridge(HttpServletRequest req) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    // --- 诊断日志：检查 SecurityContext 状态 ---
    if (auth == null) {
      log.warn("SSO bridge: SecurityContext is empty — no authentication in context. "
          + "Request URI={}, headers=[Authorization={}]",
          req.getRequestURI(),
          req.getHeader("Authorization") != null ? "present" : "MISSING");
      throw new ResponseStatusException(UNAUTHORIZED, "no valid token");
    }
    log.info("SSO bridge: authenticated principal type={}, name={}",
        auth.getPrincipal().getClass().getSimpleName(), auth.getName());

    if (!(auth.getPrincipal() instanceof PlatformUserPrincipal)) {
      log.warn("SSO bridge: principal is not PlatformUserPrincipal, got {}",
          auth.getPrincipal().getClass().getName());
      throw new ResponseStatusException(UNAUTHORIZED, "no valid token");
    }
    PlatformUserPrincipal principal = (PlatformUserPrincipal) auth.getPrincipal();
    log.info("SSO bridge: user={}, tenantId={}, email={}",
        principal.getUsername(), principal.getTenantId(), principal.getEmail());

    // 关键：把身份写入 session，供 AS 链(@Order 1)在 /oauth2/authorize 读取
    HttpSession session = req.getSession(true);
    SecurityContext sc = SecurityContextHolder.createEmptyContext();
    sc.setAuthentication(new UsernamePasswordAuthenticationToken(
        auth.getPrincipal(), null, auth.getAuthorities()));
    session.setAttribute(
        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);
    log.info("SSO bridge: session created, JSESSIONID={}, SPRING_SECURITY_CONTEXT written",
        session.getId());

    // 服务端取 authorize_url（含 state），避免前端跨域调 WeKnora
    try {
      JsonNode body = RestClient.create().get()
          .uri(WEKNORA_OIDC_URL)
          .retrieve()
          .body(JsonNode.class);
      String authorizeUrl = body != null ? body.get("authorization_url").asText() : null;
      if (authorizeUrl == null || authorizeUrl.isBlank()) {
        log.error("SSO bridge: WeKnora returned empty authorization_url, body={}", body);
        throw new ResponseStatusException(UNAUTHORIZED, "weknora authorize url is empty");
      }
      log.info("SSO bridge: obtained authorizeUrl (len={})", authorizeUrl.length());
      return Map.of("authorizeUrl", authorizeUrl);
    } catch (Exception e) {
      log.error("SSO bridge: failed to call WeKnora /oidc/url: {}", e.getMessage(), e);
      throw new ResponseStatusException(UNAUTHORIZED, "weknora unreachable");
    }
  }
}
