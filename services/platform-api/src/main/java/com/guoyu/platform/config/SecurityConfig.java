package com.guoyu.platform.config;

import lombok.extern.slf4j.Slf4j;

import com.guoyu.platform.common.filter.TenantContextFilter;
import com.guoyu.platform.security.RestAccessDeniedHandler;
import com.guoyu.platform.security.RestAuthenticationEntryPoint;
import com.guoyu.platform.security.filter.AppTokenAuthFilter;
import com.guoyu.platform.security.filter.SaTokenBridgeFilter;
import com.guoyu.platform.competition.player.security.CompetitionPlayerTokenBridgeFilter;
import com.guoyu.platform.file.config.FileStorageProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({FileStorageProperties.class})
@Slf4j
public class SecurityConfig {

  /**
   * 测试便利白名单开关（默认关 = 生产安全态）：开启时 /training/sign-ups/** 全路径匿名放行，
   * 仅用于本机联调（env: PERMIT_TEST_APIS=true）。生产必须保持关闭——此时管理端 sign-ups
   * 端点（CRUD/二维码/导出）走 Sa-Token 认证 + @PreAuthorize 权限码，
   * 学员扫码报名匿名端点不受此开关影响（业务本身即匿名，精确放行见下方白名单）。
   */
  @Value("${platform.security.permit-test-apis:false}")
  private boolean permitTestApis;

  @Bean
  public AppTokenAuthFilter appTokenAuthFilter(
      com.guoyu.platform.app.mapper.PlatformUserTokenMapper tokenMapper,
      com.guoyu.platform.trainingdepartment.mapper.PlatformUserMapper userMapper) {
    return new AppTokenAuthFilter(tokenMapper, userMapper);
  }

  @Bean
  public org.springframework.boot.web.servlet.FilterRegistrationBean<AppTokenAuthFilter> appTokenAuthFilterRegistration(
      AppTokenAuthFilter filter) {
    org.springframework.boot.web.servlet.FilterRegistrationBean<AppTokenAuthFilter> registration =
        new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }

  @Bean
  @Order(0)
  public SecurityFilterChain ssoBridgeChain(
      HttpSecurity http,
      SaTokenBridgeFilter saTokenBridgeFilter,
      TenantContextFilter tenantContextFilter
  ) throws Exception {
    http
        .securityMatcher("/sso/weknora")
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .addFilterBefore(saTokenBridgeFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(tenantContextFilter, SaTokenBridgeFilter.class)
        .authorizeHttpRequests(a -> a.anyRequest().permitAll());
    return http.build();
  }

  @Bean
  @Order(2)
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      RestAuthenticationEntryPoint authenticationEntryPoint,
      RestAccessDeniedHandler accessDeniedHandler,
      AppTokenAuthFilter appTokenAuthFilter,
      CompetitionPlayerTokenBridgeFilter competitionPlayerTokenBridgeFilter,
      SaTokenBridgeFilter saTokenBridgeFilter,
      TenantContextFilter tenantContextFilter,
      CorsConfigurationSource corsConfigurationSource
  ) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .cors(c -> c.configurationSource(corsConfigurationSource))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(exception -> exception
            .authenticationEntryPoint(authenticationEntryPoint)
            .accessDeniedHandler(accessDeniedHandler)
        )
        .authorizeHttpRequests(auth -> {
            // CORS 预检请求直接放行（否则浏览器 OPTIONS 会先被 Security 拦截返回 405）
            auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
            auth.requestMatchers("/actuator/health", "/actuator/info").permitAll();
            auth.requestMatchers(HttpMethod.POST, "/auth/login", "/auth/refresh", "/auth/logout").permitAll();
            auth.requestMatchers(HttpMethod.GET, "/competition/player/entry/resolve").permitAll();
            auth.requestMatchers(HttpMethod.POST,
                "/competition/player/activation/send-code",
                "/competition/player/activation/verify",
                "/competition/player/auth/login").permitAll();
            auth.requestMatchers(HttpMethod.GET, "/auth/tenants").permitAll();
            auth.requestMatchers(HttpMethod.GET, "/auth/captcha/generate").permitAll();
            auth.requestMatchers(HttpMethod.POST, "/auth/captcha/verify").permitAll();
            auth.requestMatchers(HttpMethod.GET, "/exam/files/**").permitAll();
            auth.requestMatchers("/files/**").permitAll();
            auth.requestMatchers(HttpMethod.GET, "/ws/practical-scoring").permitAll();
            auth.requestMatchers(HttpMethod.POST, "/appregistration/**").permitAll();
            auth.requestMatchers(HttpMethod.POST, "/appsms/**", "/appauth/**", "/appuser/**", "/appuserv4/**", "/apphome/**", "/appsmart/**", "/appcoach/**").permitAll();
            // App 端接口（身份证 OCR 等），由 AppTokenAuthFilter 验证登录态即可
            auth.requestMatchers("/training/app/**").permitAll();
            // 培训管理·扫码学员报名 —— 学员匿名链路（生产功能：学员扫码填报名表，未登录，
            // 以身份证号(+部分场景手机号)做弱身份校验；管理端端点不在此列，见下方开关）
            auth.requestMatchers(HttpMethod.GET,
                "/training/sign-ups/{id}/detail",
                "/training/sign-ups/{id}/roster-check",
                "/training/sign-ups/{id}/mine").permitAll();
            auth.requestMatchers(HttpMethod.POST,
                "/training/sign-ups/{id}/register",
                "/training/sign-ups/upload").permitAll();
            auth.requestMatchers(HttpMethod.PUT, "/training/sign-ups/{id}/mine").permitAll();
            if (permitTestApis) {
                // 本地联调便利（env PERMIT_TEST_APIS=true，默认关）：管理端 sign-ups
                // CRUD/二维码/导出也匿名放行；生产默认关闭 → 落入 authenticated()，
                // 由 Sa-Token + @PreAuthorize(train:sign-up:*) 保护
                log.info("[Security] permit-test-apis=true: /training/sign-ups/** 匿名放行（仅限本地联调）");
                auth.requestMatchers("/training/sign-ups/**").permitAll();
            }
            auth.requestMatchers("/training/public/research-collection/**").permitAll();
            auth.requestMatchers(HttpMethod.GET, "/collect-test.html", "/favicon.ico").permitAll();
            // OIDC 端点由 @Order(1) AS 链处理，此处放行防止 app 链拦截
            auth.requestMatchers("/oauth2/**", "/userinfo", "/.well-known/**").permitAll();
            auth.anyRequest().authenticated();
        })
        .addFilterBefore(saTokenBridgeFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(appTokenAuthFilter, SaTokenBridgeFilter.class)
        .addFilterBefore(competitionPlayerTokenBridgeFilter, AppTokenAuthFilter.class)
        .addFilterBefore(tenantContextFilter, CompetitionPlayerTokenBridgeFilter.class);
    return http.build();
  }

  /**
   * 兼容编码器：{noop} 明文匹配（OIDC client_secret）+ 无前缀自动 BCrypt（现有用户密码）。
   * <p>
   * 不能用 {@code PasswordEncoderFactories.createDelegatingPasswordEncoder()}，
   * 因为它的 defaultPasswordEncoderForMatches 是 UnmappedIdPasswordEncoder，
   * 遇到无前缀的旧密码（$2a$...）会直接抛异常而非 fallback 到 BCrypt。
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    Map<String, PasswordEncoder> encoders = new HashMap<>();
    encoders.put("bcrypt", new BCryptPasswordEncoder());
    encoders.put("noop", org.springframework.security.crypto.password.NoOpPasswordEncoder.getInstance());
    DelegatingPasswordEncoder delegating = new DelegatingPasswordEncoder("bcrypt", encoders);
    // 无前缀密码（旧 bcrypt 哈希）→ 用 BCrypt 验证
    delegating.setDefaultPasswordEncoderForMatches(new BCryptPasswordEncoder());
    return delegating;
  }
}
