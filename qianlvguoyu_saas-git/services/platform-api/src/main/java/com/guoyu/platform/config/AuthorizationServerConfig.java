package com.guoyu.platform.config;

import com.guoyu.platform.security.RestAuthenticationEntryPoint;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.config.Customizer;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Configuration
public class AuthorizationServerConfig {

  /**
   * OIDC Authorization Server 过滤器链，仅处理 OIDC 端点。
   * @Order(1) 确保在 app 链(@Order(2))之前匹配。
   */
  @Bean
  @Order(1)
  public SecurityFilterChain authorizationServerFilterChain(
      HttpSecurity http,
      RestAuthenticationEntryPoint authenticationEntryPoint) throws Exception {
    OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
    http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
        .oidc(Customizer.withDefaults());

    http
        .securityMatcher("/oauth2/**", "/userinfo", "/.well-known/**")
        // 关键：在 AS 过滤器之前，手动从 session 加载 SecurityContext
        .addFilterBefore(new OncePerRequestFilter() {
          @Override
          protected void doFilterInternal(HttpServletRequest request,
              HttpServletResponse response, FilterChain filterChain)
              throws ServletException, IOException {
            HttpSession session = request.getSession(false);
            if (session != null) {
              Object ctx = session.getAttribute(
                  HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
              if (ctx instanceof org.springframework.security.core.context.SecurityContext sc) {
                SecurityContextHolder.setContext(sc);
                log.debug("SSO-AS: loaded SecurityContext from session {} (auth={})",
                    session.getId(),
                    sc.getAuthentication() != null ? sc.getAuthentication().getName() : "null");
              } else {
                log.debug("SSO-AS: session {} exists but no SecurityContext found", session.getId());
              }
            } else {
              log.debug("SSO-AS: no session for request {}", request.getRequestURI());
            }
            filterChain.doFilter(request, response);
          }
        }, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
        .exceptionHandling(e -> e
            .authenticationEntryPoint(authenticationEntryPoint));
    return http.build();
  }

  @Bean
  public RegisteredClientRepository registeredClientRepository() {
    // TODO: 实施时用 openssl rand -hex 32 生成，替换 {noop}SECRET_HERE
    RegisteredClient weknora = RegisteredClient.withId(UUID.randomUUID().toString())
        .clientId("weknora")
        .clientSecret("{noop}454f0a63386e667a5a0e3437ce12b508106db9934de01845d853c3829afb968a") // 生产用 {bcrypt}
        .clientAuthenticationMethods(c -> {
          c.add(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
          c.add(ClientAuthenticationMethod.CLIENT_SECRET_POST);
        })
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("http://43.138.163.7:8088/api/v1/auth/oidc/callback")
        .scope(OidcScopes.OPENID)
        .scope(OidcScopes.PROFILE)
        .scope(OidcScopes.EMAIL)
        .clientSettings(ClientSettings.builder()
            .requireAuthorizationConsent(false) // 无感 SSO
            .build())
        .tokenSettings(TokenSettings.builder()
            .idTokenSignatureAlgorithm(SignatureAlgorithm.RS256)
            .build())
        .build();
    return new InMemoryRegisteredClientRepository(weknora);
  }

  @Bean
  public JWKSource<SecurityContext> jwkSource() {
    RSAKey rsaKey = generateRsa();
    JWKSet jwkSet = new JWKSet(rsaKey);
    return (jwkSelector, ctx) -> jwkSelector.select(jwkSet);
  }

  @Bean
  public AuthorizationServerSettings authorizationServerSettings() {
    return AuthorizationServerSettings.builder()
        .issuer("http://43.138.163.7/api")
        .build();
  }

  /**
   * 开发期：每次启动生成新 RSA 密钥对。
   * 生产务必从 keystore/配置加载持久化密钥，否则重启后旧 id_token 失效。
   */
  private static RSAKey generateRsa() {
    try {
      KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
      gen.initialize(2048);
      KeyPair kp = gen.generateKeyPair();
      return new RSAKey.Builder((RSAPublicKey) kp.getPublic())
          .privateKey((RSAPrivateKey) kp.getPrivate())
          .keyID(UUID.randomUUID().toString())
          .build();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to generate RSA key pair", e);
    }
  }
}
