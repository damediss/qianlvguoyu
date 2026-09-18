# 千铝国昱云平台 × WeKnora —— OIDC 单点登录集成方案

> 面向实施者：千铝国昱 Spring Boot 后端开发（本地源码 `D:\workspace\qianlvguoyu_saas`）。
> WeKnora 侧已在本服务器（43.138.163.7）预置完成，本文聚焦**千铝国昱后端改造**。

## 0. 目标与角色

- **目标**：用户登录千铝国昱云平台（前端 `:5173`）后，访问 WeKnora（`:8088`）时无需再次登录，自动以**同邮箱**身份进入。
- **角色**：千铝国昱 Spring Boot 后端 = **OIDC Provider（IdP）**；WeKnora = **OIDC Client（RP）**。
- WeKnora 行为（已确认，见 `WeKnora/docs/OIDC认证调用流程.md`）：
  - 用授权码 `code` 换 token，**本地解码 id_token 取 claims**（不验签），按 `email` 关联本地用户；不存在则**自动建号 + 默认租户**。
  - 真正访问 WeKnora 的凭证是 WeKnora 自己签发的 JWT，OIDC token 只用于“确认你是谁”。

## 1. SSO 调用流程（目标态）

```
1. 用户登录 千铝国昱(:5173)          → Spring 建立(共享)会话
2. 用户点“进入 WeKnora” → http://43.138.163.7:8088
3. WeKnora 无本地会话 → 后端生成 authorize_url，浏览器跳转：
   http://43.138.163.7/oauth2/authorize?response_type=code&client_id=weknora
     &redirect_uri=https://zsk.gjjd168.com/api/v1/auth/oidc/callback
     &scope=openid+profile+email&state=...
4. 千铝国昱 IdP(/oauth2/authorize) 从共享会话读到已认证身份 → 不再弹登录 → 直接发 code
5. 浏览器 302 回 https://zsk.gjjd168.com/api/v1/auth/oidc/callback?code=...&state=...
6. WeKnora 后端用 code 调 token 端点(容器内→host.docker.internal:8080)换 id_token
7. WeKnora 解码 id_token 拿 email → 按 email 自动建号/匹配 → 签发 WeKnora JWT
8. 302 回 WeKnora 前端 /#oidc_result=... → 用户已登录，进入工作台
```

## 2. 第 0 步（决定性）：判定你们现有认证模式

**这一步决定 SSO 能否直接成立，必须先做。** 打开你们的 Spring Security 配置类（通常 `SecurityConfig` / `WebSecurityConfig`）看：

| 现象 | 模式 | SSO 是否直接成立 |
|---|---|---|
| 有 `.formLogin(...)` 或未设 `SessionCreationPolicy.STATELESS` | **Session-based** | ✅ 直接成立 |
| 有 `sessionCreationPolicy(STATELESS)` 或自定义 JWT 过滤器（解析 `Authorization: Bearer`） | **JWT / 无状态** | ❌ 需桥接（见 §2.2） |

### 2.1 若是 Session-based（理想情况）
AS 的 `SecurityFilterChain`(@Order(1)) 与 app 的 `SecurityFilterChain`(@Order(2)) **默认共享同一个 HTTP Session / SecurityContext**。用户在 app 登录后 session 里有身份，访问 `/oauth2/authorize` 时 AS 链直接读到 → 免再登录。直接按 §3–§5 实现。

### 2.2 若是 JWT / 无状态（SPA + JWT 常见，需桥接）
authorize 端点找不到已认证身份 → 会强制再登录 → SSO 不成立。二选一：

- **(a) 推荐（长期）**：把登录改为 session-based，或至少为 AS 单独维护一个服务端 session。
- **(b) 短期桥接**：新增一个 `/sso/weknora` 端点，前端登录后用现有 JWT 调用它；后端校验 JWT → 建立 HTTP session（`HttpServletRequest` + `SecurityContextHolder`）→ `302` 到 `/oauth2/authorize?...`。这样 AS 链能见到 session。伪代码：
  ```java
  @GetMapping("/sso/weknora")
  public void ssoBridge(@RequestParam String redirect, HttpServletRequest req, HttpServletResponse resp, @AuthenticationPrincipal YourUser user) throws IOException {
      HttpSession session = req.getSession(true);
      // 用 app 现有方式把身份写入 SecurityContext（与 formLogin 一致）
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
      session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
          SecurityContextHolder.getContext());
      resp.sendRedirect("/oauth2/authorize?response_type=code&client_id=weknora"
          + "&redirect_uri=https://zsk.gjjd168.com/api/v1/auth/oidc/callback"
          + "&scope=openid+profile+email&state=" + URLEncoder.encode(redirect, UTF_8));
  }
  ```
  > 注意：state 在 WeKnora 流程里由 WeKnora 后端生成并携带；桥接端点不应自造 state，正确做法是前端先调 WeKnora `/api/v1/auth/oidc/url` 拿到完整 `authorization_url`，再把该 URL 交给桥接端点在建立 session 后原样重定向。即：前端 → 桥接端点(建 session) → 302 到 WeKnora 给的 authorize_url。

## 3. 版本要求与前置确认

- Spring Boot **2.7+**，推荐 **3.x**（Spring Security 6）。本文代码按 **Boot 3.x / Security 6** 写。
- Boot 2.7 用 `spring-boot-starter-oauth2-authorization-server` 0.4.x，DSL 略有差异（下文标注）。
- 先在 `pom.xml` 确认 `<parent>` 的 Boot 版本，并确认现有 `spring-boot-starter-security` 已引入。

## 4. 实现步骤

### 4.1 加依赖（`pom.xml`）
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-oauth2-authorization-server</artifactId>
</dependency>
<!-- 若还没有： -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

### 4.2 新增 `AuthorizationServerConfig`
```java
package com.qianlvguoyu.config; // 按你们包名改

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
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
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import com.nimbusds.jose.jwk.RSAKey;
import java.util.UUID;

@Configuration
public class AuthorizationServerConfig {

    @Bean @Order(1)
    public SecurityFilterChain authorizationServerFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
            .oidc(Customizer.withDefaults()); // 启用 OIDC：发 id_token + /userinfo
        // 未登录时回登录页（复用 app 现有 /login）
        http.exceptionHandling(e -> e.defaultAuthenticationEntryPointFor(
                new LoginUrlAuthenticationEntryPoint("/login"),
                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults())); // 保护 /userinfo
        return http.build();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        RegisteredClient weknora = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("weknora")
            .clientSecret("{noop}SECRET_HERE")   // 见 §4.6 生成；{noop}=明文，生产用 bcrypt
            .clientAuthenticationMethods(
                ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                ClientAuthenticationMethod.CLIENT_SECRET_POST) // WeKnora 用 POST 表单提交 secret
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("https://zsk.gjjd168.com/api/v1/auth/oidc/callback") // 必须精确匹配
            .scope(OidcScopes.OPENID)
            .scope(OidcScopes.PROFILE)
            .scope(OidcScopes.EMAIL)
            .clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).build()) // 无感 SSO
            .tokenSettings(TokenSettings.builder().idTokenSignatureAlgorithm(SignatureAlgorithm.RS256).build())
            .build();
        return new InMemoryRegisteredClientRepository(weknora);
        // 生产建议用 JdbcRegisteredClientRepository 持久化，且 client_secret 用 {bcrypt}
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        RSAKey rsaKey = RsaKeyUtil.generate(); // 见 §4.3
        JWKSet jwkSet = new JWKSet(rsaKey);
        return (jwkSelector, ctx) -> jwkSelector.select(jwkSet);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings(
        @Value("${platform.oauth2.issuer:https://java.app.gjjd168.com/api}") String issuer) {
        return AuthorizationServerSettings.builder()
            .issuer(issuer) // 与 WeKnora .env 的 OIDC_AUTH_ISSUER_URL 逐字符一致
            .build();
    }
}
```
> Boot 2.7 差异：`applyDefaultSecurity` / `OAuth2AuthorizationServerConfigurer` API 基本一致，但 lambda DSL 需改为旧式（`.and()` 链式）；`oauth2ResourceServer(jwt...)` 写法略不同。

### 4.3 RSA 签名密钥（`RsaKeyUtil`）
```java
package com.qianlvguoyu.config;

import com.nimbusds.jose.jwk.RSAKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

public final class RsaKeyUtil {
    private RsaKeyUtil() {}
    public static RSAKey generate() {
        // 简易版：每次启动生成。生产务必持久化（从 keystore/配置加载），
        // 否则重启后密钥变化，已签发的 id_token 失效（影响校验方）。
        java.security.KeyPair kp;
        try {
            var gen = java.security.KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            kp = gen.generateKeyPair();
        } catch (Exception e) { throw new IllegalStateException(e); }
        return new RSAKey.Builder((RSAPublicKey) kp.getPublic())
            .privateKey((RSAPrivateKey) kp.getPrivate())
            .keyID(UUID.randomUUID().toString())
            .build();
    }
}
```

### 4.4 给 id_token 注入 email/name（**关键，WeKnora 靠这个取邮箱**）
```java
package com.qianlvguoyu.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.oidc.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

@Configuration
public class IdTokenClaimsConfig {

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> idTokenClaimsCustomizer() {
        return context -> {
            if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
                Object principal = context.getPrincipal().getPrincipal();
                // ⚠ 适配你们的真实类型：先在 authorize 端点断点看 principal 是什么
                //    常见：你们自定义的 UserDetails 实现类，或 User 实体本身。
                String email = null, name = null;
                if (principal instanceof YourUserDetails u) {   // 改成你们的类
                    email = u.getEmail();
                    name  = u.getNickname();
                }
                // 也可在此按 username 查 user 表取 email（若 principal 只含 username）
                if (email != null) {
                    context.getClaims().claim("email", email)
                           .claim("email_verified", true);
                }
                if (name != null) context.getClaims().claim("name", name);
            }
        };
    }
}
```
> **必须验证**：联调时用 `jwt.io` 解码 WeKnora 收到的 id_token，确认 payload 里有 `email`。WeKnora 没有 email 会直接报错（“本地用户按 email 关联”）。

### 4.5 调整现有 app `SecurityConfig`（@Order(2)，放行 IdP 路径）
在你们现有 `SecurityFilterChain` 上加 `@Order(2)`（若未显式设置，默认最后；但要确保 AS 链 @Order(1) 先匹配），并确保不拦截 OIDC 路径：
```java
@Bean @Order(2)
public SecurityFilterChain appFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(a -> a
            // IdP 路径交给 @Order(1) 链，这里别再拦
            .requestMatchers("/oauth2/**","/userinfo","/.well-known/**").permitAll()
            .anyRequest().authenticated())
        .formLogin(Customizer.withDefaults())   // 你们现有登录方式保持不变
        // .sessionManagement(s -> s.sessionCreationPolicy(SessionPolicy.IF_REQUIRED)) // 若现状是 STATELESS，需按 §2.2 桥接
        ;
    return http.build();
}
```

### 4.6 生成 client_secret
```bash
openssl rand -hex 32   # 生成一个密钥
```
把该值同时填到两处（**必须一致**）：
- 千铝国昱：`AuthorizationServerConfig` 的 `.clientSecret("{noop}该密钥")`（生产用 `{bcrypt}...`）。
- WeKnora 服务器 `.env`：`OIDC_AUTH_CLIENT_SECRET=该密钥`。

### 4.7 公司现有 nginx（:80）加代理，让浏览器可达 IdP（避免公开 :8080）
在千铝国昱现有 nginx 的 `server` 块（:80）加：
```nginx
location ~ ^/(oauth2/|userinfo$|\.well-known/) {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-Host  $host;
    proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
}
```
> 目的：浏览器访问 `http://43.138.163.7/oauth2/authorize`（:80，公网可达）经 nginx 转给后端 :8080；WeKnora 容器调 token 走 `host.docker.internal:8080` 直连后端。`issuer=http://43.138.163.7`（:80）与 nginx 对外一致。

### 4.8 构建部署
```bash
# 本地（D:\workspace\qianlvguoyu_saas）
mvn clean package -DskipTests
# 把新 platform-api.jar 部署到 43.138.163.7:/opt/guoyu-platform/，按你们现有方式重启
# nginx reload
```

## 5. 必须精确匹配的值清单

| 项 | 值 |
|---|---|
| `client_id` | `weknora` |
| `redirect_uri` | `https://zsk.gjjd168.com/api/v1/auth/oidc/callback` |
| `scope` | `openid profile email` |
| `issuer` | `https://java.app.gjjd168.com/api`（生产；代码已外部化为 `platform.oauth2.issuer`） |
| `client_secret` | §4.6 生成；与 WeKnora `.env` 的 `OIDC_AUTH_CLIENT_SECRET` 一致 |
| id_token claims | 必含 `email`（推荐含 `name`） |
| Authorization Code grant | 启用 |
| consent | `requireAuthorizationConsent=false`（无感 SSO） |

## 6. WeKnora 侧配置（已在 43.138.163.7 预置，待启用）

文件：`/opt/guoyu-platform/WeKnora/.env`（已写好，`OIDC_AUTH_ENABLE=false`）。公司 IdP 就绪后：
```bash
# 1. 填真实密钥（与 §4.6 一致）
OIDC_AUTH_CLIENT_SECRET=<生成的密钥>
# 2. 开关置 true
OIDC_AUTH_ENABLE=true
# 3. 重启 app
cd /opt/guoyu-platform/WeKnora && docker compose up -d app
```
当前预置的端点（生产值，2026-09-18 起 issuer 走域名）：
```
OIDC_AUTH_ISSUER_URL=https://java.app.gjjd168.com/api
OIDC_AUTH_AUTHORIZATION_ENDPOINT=https://java.app.gjjd168.com/api/oauth2/authorize   # 浏览器
OIDC_AUTH_TOKEN_ENDPOINT=http://host.docker.internal:8080/oauth2/token  # 容器→后端
# userinfo 已注释：仅靠 id_token 取 email（简化，少一个端点）
OIDC_USER_INFO_MAPPING_EMAIL=email
OIDC_USER_INFO_MAPPING_USER_NAME=name
```

## 7. 联调验证（公司 IdP 起来后）

1. **IdP 自测**：浏览器直接访问
   `http://43.138.163.7/oauth2/authorize?response_type=code&client_id=weknora&redirect_uri=https://zsk.gjjd168.com/api/v1/auth/oidc/callback&scope=openid+profile+email`
   - 未登录 → 跳 `/login`；登录后 → 302 回 redirect_uri 带 `code`。✅
2. **id_token 校验**：用 §1 步骤 6 抓到的 id_token，在 `https://jwt.io` 解码，确认 payload 含 `email`。✅
3. **WeKnora 启用**：按 §6 填密钥+ENABLE=true+重启。
4. **登录页**：`http://43.138.163.7:8088` 应出现“**使用 千铝国昱 登录**”按钮。
5. **SSO 闭环**：先登录 `:5173`，再开 `:8088` → 点 OIDC 登录 → **不再输密码、直接进 WeKnora 工作台**，且按你的邮箱自动建号。✅

## 8. 安全注意

- 现全程 **HTTP**，授权码/token 有被截获风险。**测试可，生产务必上 HTTPS**：给 `:8088` 和 `:80` 上域名+证书，把本文所有 `http://43.138.163.7` 换成 `https://你的域名`，redirect_uri 同步换。
- `client_secret` 生产用 `{bcrypt}`，别用 `{noop}`。
- RSA 签名密钥生产**必须持久化**（§4.3 注释），别每次启动新生成。
- `requireAuthorizationConsent=false` 表示对 weknora 客户端跳过同意页——仅在你信任该客户端时这么设。

## 9. 故障排查

| 现象 | 排查 |
|---|---|
| WeKnora 登录页没有 OIDC 按钮 | `OIDC_AUTH_ENABLE` 是否 true；`docker compose logs app` 看启动报错 |
| 点 OIDC 登录跳到公司 `/login`（要再登录） | §2：你们是 JWT/无状态，没共享 session → 走 §2.2 桥接 |
| 回调报 `missing email` / 报错 | id_token 里没 `email` → §4.4 的 TokenCustomizer 没生效或 principal 类型不对，断点确认 |
| 回调 `invalid_state` | redirect_uri 在公司 IdP 未精确注册（必须一字不差，含 http、端口、路径） |
| 容器换 token 超时 | `host.docker.internal:8080` 不通：确认 app 容器有 `extra_hosts: host.docker.internal:host-gateway`（WeKnora compose 已配） |
| `iss` 不匹配 | 公司 `AuthorizationServerSettings.issuer` 与 WeKnora `OIDC_AUTH_ISSUER_URL` 不一致 |

---

## 实施分工

- **千铝国昱后端（你们本地 agent）**：§2 判定认证模式 → §3–§5 实现 → §4.8 构建 → 部署。
- **WeKnora 侧（本服务器，我来）**：公司 IdP 就绪后，按 §6 填密钥/启用/重启，并跑 §7 联调。
- **卡点沟通**：实施前请告知本服务器侧两件事——(1) §2 判定结果（session 还是 JWT）；(2) id_token 里 `email`/`name` 字段最终叫什么（决定 WeKnora `OIDC_USER_INFO_MAPPING_*` 是否要改）。
