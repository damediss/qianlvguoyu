package com.guoyu.platform.config;

import lombok.extern.slf4j.Slf4j;

import com.guoyu.platform.common.filter.TenantContextFilter;
import com.guoyu.platform.security.RestAccessDeniedHandler;
import com.guoyu.platform.security.RestAuthenticationEntryPoint;
import com.guoyu.platform.security.filter.AppTokenAuthFilter;
import com.guoyu.platform.security.filter.SaTokenBridgeFilter;
import com.guoyu.platform.file.config.FileStorageProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      RestAuthenticationEntryPoint authenticationEntryPoint,
      RestAccessDeniedHandler accessDeniedHandler,
      AppTokenAuthFilter appTokenAuthFilter,
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
            .requestMatchers(HttpMethod.GET, "/auth/tenants").permitAll()
            .requestMatchers(HttpMethod.GET, "/auth/captcha/generate").permitAll()
            .requestMatchers(HttpMethod.POST, "/auth/captcha/verify").permitAll()
            .requestMatchers(HttpMethod.GET, "/exam/files/**").permitAll()
            .requestMatchers("/files/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/ws/practical-scoring").permitAll()
            .requestMatchers(HttpMethod.POST, "/appregistration/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/appsms/**", "/appauth/**", "/appuser/**", "/apphome/**").permitAll()
            // App 端接口（身份证 OCR 等），由 AppTokenAuthFilter 验证登录态即可
            .requestMatchers("/training/app/**").permitAll()
            // 培训管理·扫码信息收集 - 测试专用接口（生成环境请移除并接入鉴权）
            .requestMatchers("/training/sign-ups/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/collect-test.html", "/favicon.ico").permitAll()
            .anyRequest().authenticated()
        )
        .addFilterBefore(tenantContextFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(saTokenBridgeFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(appTokenAuthFilter, SaTokenBridgeFilter.class);
    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
