package com.guoyu.platform.config;

import lombok.extern.slf4j.Slf4j;

import cn.dev33.satoken.stp.StpLogic;
import com.guoyu.platform.common.filter.TenantContextFilter;
import com.guoyu.platform.security.RestAccessDeniedHandler;
import com.guoyu.platform.security.RestAuthenticationEntryPoint;
import com.guoyu.platform.security.filter.AppTokenAuthFilter;
import com.guoyu.platform.security.filter.SaTokenBridgeFilter;
import com.guoyu.platform.competition.player.security.CompetitionPlayerTokenBridgeFilter;
import com.guoyu.platform.file.config.FileStorageProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
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
        .authorizeHttpRequests(auth -> auth
            // CORS 预检请求直接放行（否则浏览器 OPTIONS 会先被 Security 拦截返回 405）
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers("/actuator/health", "/actuator/info").permitAll()
            .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/refresh", "/auth/logout").permitAll()
            .requestMatchers(HttpMethod.GET, "/competition/player/entry/resolve").permitAll()
            .requestMatchers(HttpMethod.POST,
                "/competition/player/activation/send-code",
                "/competition/player/activation/verify",
                "/competition/player/auth/login").permitAll()
            .requestMatchers(HttpMethod.GET, "/auth/tenants").permitAll()
            .requestMatchers(HttpMethod.GET, "/auth/captcha/generate").permitAll()
            .requestMatchers(HttpMethod.POST, "/auth/captcha/verify").permitAll()
            .requestMatchers(HttpMethod.GET, "/exam/files/**").permitAll()
            .requestMatchers("/files/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/ws/practical-scoring").permitAll()
            .requestMatchers(HttpMethod.POST, "/appregistration/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/appsms/**", "/appauth/**", "/appuser/**", "/appuserv4/**", "/apphome/**", "/appsmart/**", "/appcoach/**").permitAll()
            // App 端接口（身份证 OCR 等），由 AppTokenAuthFilter 验证登录态即可
            .requestMatchers("/training/app/**").permitAll()
            // 培训管理·扫码信息收集 - 测试专用接口（生成环境请移除并接入鉴权）
            .requestMatchers("/training/sign-ups/**").permitAll()
            .requestMatchers("/training/public/research-collection/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/collect-test.html", "/favicon.ico").permitAll()
            // OIDC 端点由 @Order(1) AS 链处理，此处放行防止 app 链拦截
            .requestMatchers("/oauth2/**", "/userinfo", "/.well-known/**").permitAll()
            .anyRequest().authenticated()
        )
        .addFilterBefore(saTokenBridgeFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(appTokenAuthFilter, SaTokenBridgeFilter.class)
        .addFilterBefore(competitionPlayerTokenBridgeFilter, AppTokenAuthFilter.class)
        .addFilterBefore(tenantContextFilter, CompetitionPlayerTokenBridgeFilter.class);
    return http.build();
  }

  /**
   * Sa-Token 默认 StpLogic（login type = login）。
   * 项目中另有 competitionExamStpLogic / competitionPlayerStpLogic 两个自定义 StpLogic，
   * 必须存在名为 stpLogic 的默认 bean，且编译保留参数名，否则 SaBeanInject.setStpLogic
   * 按类型注入会因候选 bean 过多而启动失败。
   */
  @Bean
  @Primary
  public StpLogic stpLogic() {
    return new StpLogic("login");
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
