# 千铝国昱云平台 × WeKnora —— OIDC 单点登录集成方案（v2，已锁定决策）

> 面向实施者：千铝国昱 Spring Boot 后端开发（本地源码 `D:\workspace\qianlvguoyu_saas`）。
> WeKnora 侧已在本服务器（43.138.163.7）预置完成。本文聚焦**千铝国昱后端改造**。

## 已确认决策（本次锁定）

| 项 | 结论 |
|---|---|
| 认证模式 | **JWT / 无状态**（`SecurityConfig.java:48` `SessionCreationPolicy.STATELESS`，前端 Sa-Token + Bearer） |
| SSO 路径 | **必须走桥接**（§2）：新增 `/sso/weknora`，前端用 Bearer 调它，后端建 session 写 SecurityContext → 浏览器跳 `/oauth2/authorize` |
| Principal 类型 | `com.guoyu.platform.security.PlatformUserPrincipal`（自定义 UserDetails） |
| email 现状 | `PlatformUserPrincipal` **未暴露 email**；`UserEntity:17` 有，构造时未传入 |
| email 方案 | **(a) 给 `PlatformUserPrincipal` 加 email 字段 + getEmail()**（推荐，改动小）；3 处构造点补传 |
| id_token claims | `email`、`email_verified=true`、`name`=displayName |
| WeKnora `.env` 映射 | **不改**：`OIDC_USER_INFO_MAPPING_EMAIL=email`、`..._USER_NAME=name` 与默认一致 |

## 1. SSO 调用流程（目标态，含桥接）

```
A. 用户登录 千铝国昱(:5173) → Sa-Token 发 Bearer Token（无服务端 session）
B. 用户点“进入 WeKnora”
C. 前端 fetch('/sso/weknora', {Authorization: Bearer <token>})
   → 后端 AppTokenAuthFilter 校验 token → SecurityContext 里有 PlatformUserPrincipal
   → 桥接控制器：req.getSession(true) 创建 session，把 SecurityContext 写入 session
   → 服务端调 WeKnora /api/v1/auth/oidc/url 取 authorize_url（含 WeKnora 生成的 state）
   → 返回 JSON {authorizeUrl}（响应带 Set-Cookie: JSESSIONID）
D. 前端 window.location = authorizeUrl
   → 浏览器导航到 http://43.138.163.7/oauth2/authorize?...（同域，自动带 JSESSIONID）
   → AS 链(@Order 1, session-capable) 从 session 读到已认证身份 → 不弹登录 → 发 code
E. 浏览器 302 回 http://43.138.163.7:8088/api/v1/auth/oidc/callback?code=...&state=...
F. WeKnora 后端用 code 调 token 端点(容器→host.docker.internal:8080)换 id_token
G. WeKnora 解码 id_token 拿 email → 按 email 自动建号/匹配 → 签发 WeKnora JWT
H. 302 回 WeKnora 前端 /#oidc_result=... → 用户已登录，进入工作台
```

## 2. 桥接实现（必做，针对 JWT/无状态）

### 2.1 桥接控制器 `SsoBridgeController`
```java
package com.guoyu.platform.controller; // 按你们包名改

import com.fasterxml.jackson.databind.JsonNode;
import com.guoyu.platform.security.PlatformUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
public class SsoBridgeController {

    /** WeKnora 后端 /oidc/url 地址（WeKnora 发布在宿主 :8088，公司后端走 localhost 即可） */
    private static final String WEKNORA_OIDC_URL =
        "http://localhost:8088/api/v1/auth/oidc/url"
        + "?redirect_uri=http://43.138.163.7:8088/api/v1/auth/oidc/callback";

    /**
     * 前端登录后用 Bearer Token 调本端点：
     * 1) AppTokenAuthFilter 已完成认证，SecurityContext 里有 PlatformUserPrincipal
     * 2) 创建 HTTP session 并写入 SecurityContext（app 链是 STATELESS 不会自动存，必须手动 stash）
     * 3) 服务端调 WeKnora /oidc/url 拿 authorize_url（含 WeKnora 生成的 state，避免手搓 state）
     * 4) 返回 {authorizeUrl}；前端拿到后 window.location 跳转，浏览器带 JSESSIONID 命中 /oauth2/authorize
     */
    @GetMapping("/sso/weknora")
    public Map<String, String> bridge(HttpServletRequest req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof PlatformUserPrincipal)) {
            throw new ResponseStatusException(UNAUTHORIZED, "no valid token");
        }
        // 关键：把身份写入 session，供 AS 链(@Order 1)在 /oauth2/authorize 读取
        HttpSession session = req.getSession(true);
        SecurityContext sc = SecurityContextHolder.createEmptyContext();
        sc.setAuthentication(new UsernamePasswordAuthenticationToken(
                auth.getPrincipal(), null, auth.getAuthorities()));
        session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);

        // 服务端取 authorize_url（含 state），避免前端跨域调 WeKnora
        JsonNode body = RestClient.create().get()
            .uri(WEKNORA_OIDC_URL).retrieve().body(JsonNode.class);
        String authorizeUrl = body != null ? body.get("authorization_url").asText() : null;

        return Map.of("authorizeUrl", authorizeUrl);
    }
}
```

### 2.2 前端调用（千铝国昱 :5173）
```js
// 用户点“进入 WeKnora”时
const r = await fetch('/sso/weknora', {
  headers: { Authorization: 'Bearer ' + saToken }   // Sa-Token Bearer
});
const { authorizeUrl } = await r.json();
window.location.href = authorizeUrl;   // 浏览器导航，带 JSESSIONID → /oauth2/authorize 免登录
```

### 2.3 STATELESS 与 session 的命门（必读 + 必测）
app 链是 `STATELESS`（用 `NullSecurityContextRepository`，不会自动把身份存进 session）。上面代码**手动** `setAttribute(SPRING_SECURITY_CONTEXT)` 绕过它。**必须实测**：

> 联调时，用合法 Bearer 调 `/sso/weknora`，看响应是否 `Set-Cookie: JSESSIONID=...`。然后浏览器访问 `/oauth2/authorize?...`，**应当直接 302 回 WeKnora callback 带 code，而不是跳 `/login`**。
> - 若仍跳 `/login` → 说明 session 没建起来或 AS 链没读到。改用 §2.4 的独立链变体。

### 2.4 兜底变体：给 /sso/weknora 单独一条 session-capable 链
若 §2.3 实测 STATELESS 阻止了 session 创建，新增一条优先级更高的链（非 stateless，并挂你们的 JWT 过滤器）：
```java
@Bean @Order(0)   // 比 AS(@Order1) 和 app(@Order2) 都优先
public SecurityFilterChain ssoBridgeChain(HttpSecurity http,
        AppTokenAuthFilter appTokenAuthFilter) throws Exception {   // 注入你们的 JWT 过滤器 bean
    http.securityMatcher("/sso/weknora")
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .addFilterBefore(appTokenAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .authorizeHttpRequests(a -> a.anyRequest().permitAll()); // 过滤器做真正鉴权
    return http.build();
}
```
> 前提：`AppTokenAuthFilter`（或 `SaTokenBridgeFilter`）是可注入的 bean，不是 `new` 出来的。若不是，先抽成 bean。这条链 session-capable，`HttpSessionSecurityContextRepository` 会自动存身份，§2.1 的手动 stash 也可保留（双保险）。

## 3. 版本要求
Spring Boot **2.7+**，推荐 **3.x**（Spring Security 6）。本文代码按 **Boot 3 / Security 6** 写（`jakarta.*`）。Boot 2.7 用 `javax.*` + authorization-server 0.4.x，DSL 改旧式 `.and()` 链。

## 4. Spring Authorization Server 实现

### 4.1 依赖（`pom.xml`）
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-oauth2-authorization-server</artifactId>
</dependency>
```

### 4.2 `AuthorizationServerConfig`（RegisteredClient + JWK + issuer + AS 链）
```java
package com.guoyu.platform.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
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

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

@Configuration
public class AuthorizationServerConfig {

    @Bean @Order(1)
    public SecurityFilterChain authorizationServerFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
            .oidc(Customizer.withDefaults()); // 启用 OIDC：发 id_token + /userinfo
        http.exceptionHandling(e -> e.defaultAuthenticationEntryPointFor(
                new LoginUrlAuthenticationEntryPoint("/login"),
                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        RegisteredClient weknora = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("weknora")
            .clientSecret("{noop}SECRET_HERE")   // §4.5 生成；生产 {bcrypt}
            .clientAuthenticationMethods(
                ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                ClientAuthenticationMethod.CLIENT_SECRET_POST) // WeKnora 用 POST 表单提交
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://43.138.163.7:8088/api/v1/auth/oidc/callback") // 精确匹配
            .scope(OidcScopes.OPENID).scope(OidcScopes.PROFILE).scope(OidcScopes.EMAIL)
            .clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).build()) // 无感 SSO
            .tokenSettings(TokenSettings.builder().idTokenSignatureAlgorithm(SignatureAlgorithm.RS256).build())
            .build();
        return new InMemoryRegisteredClientRepository(weknora); // 生产建议 JdbcRegisteredClientRepository
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        RSAKey rsaKey = generateRsa(); // 见下
        JWKSet jwkSet = new JWKSet(rsaKey);
        return (jwkSelector, ctx) -> jwkSelector.select(jwkSet);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().issuer("http://43.138.163.7").build();
    }

    /** 生产务必改为从 keystore/配置加载持久化密钥，否则重启后旧 id_token 失效 */
    private static RSAKey generateRsa() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair kp = gen.generateKeyPair();
            return new RSAKey.Builder((RSAPublicKey) kp.getPublic())
                .privateKey((RSAPrivateKey) kp.getPrivate())
                .keyID(UUID.randomUUID().toString()).build();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}
```

### 4.3 给 `PlatformUserPrincipal` 补 email（必做，3 处构造点）
**(a) 加字段 + getter +构造参数**（`PlatformUserPrincipal.java`）：
```java
public class PlatformUserPrincipal implements UserDetails {
    private final String username;
    private final String displayName;
    private final String email;          // 新增
    private final Long userId;
    private final String tenantId;
    private final Collection<? extends GrantedAuthority> roles;
    // ... 其它现有字段

    public PlatformUserPrincipal(String username, String displayName, String email /*新增*/,
                                 Long userId, String tenantId,
                                 Collection<? extends GrantedAuthority> roles /*按你们现有签名*/) {
        this.username = username; this.displayName = displayName; this.email = email;
        this.userId = userId; this.tenantId = tenantId; this.roles = roles;
    }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    // ... 现有 getUsername() 等
}
```
**(b) 3 处构造点补传 email**（从 `UserEntity.getEmail()`）：
- `AuthApplicationService.java:178-186`
- `AppTokenAuthFilter.java:82`
- `SaTokenBridgeFilter.java:91`

每处把 `new PlatformUserPrincipal(...)` 加上 `userEntity.getEmail()`（或你们现有的取 email 方式）。

### 4.4 `IdTokenClaimsConfig`（给 id_token 注 email/name —— 关键）
```java
package com.guoyu.platform.config;

import com.guoyu.platform.security.PlatformUserPrincipal;
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
                if (principal instanceof PlatformUserPrincipal u) {
                    String email = u.getEmail();
                    String name  = u.getDisplayName();   // name 用 displayName
                    if (email != null && !email.isBlank()) {
                        context.getClaims().claim("email", email)
                               .claim("email_verified", true);
                    }
                    if (name != null) context.getClaims().claim("name", name);
                }
                // 兜底：若 principal 不是 PlatformUserPrincipal（理论上不会），
                // 可在此按 u.getUsername() 查 user 表取 email（即原方案 b）。
            }
        };
    }
}
```
> **必须验证**：联调时用 jwt.io 解码 WeKnora 收到的 id_token，确认 payload 有 `email`。WeKnora 没有 email 会直接报错。

### 4.5 生成 client_secret
```bash
openssl rand -hex 32
```
两处必须一致：
- 千铝国昱：`AuthorizationServerConfig` 的 `.clientSecret("{noop}该密钥")`（生产用 `{bcrypt}`）。
- WeKnora 服务器 `.env`：`OIDC_AUTH_CLIENT_SECRET=该密钥`。

### 4.6 app `SecurityConfig` 调整（@Order(2)，放行 IdP 路径）
现有 `SecurityConfig.java` 的链加 `@Order(2)`（确保 AS 链 @Order(1) 先匹配），并放行 OIDC 路径：
```java
@Bean @Order(2)
public SecurityFilterChain appFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(a -> a
            .requestMatchers("/sso/weknora","/oauth2/**","/userinfo","/.well-known/**").permitAll()
            .anyRequest().authenticated())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 现状保留
        // 你们现有的 AppTokenAuthFilter / SaTokenBridgeFilter 注册方式保持不变
        ;
    return http.build();
}
```
> `/sso/weknora` 在 app 链上由 JWT 过滤器完成鉴权（permitAll 只是放行权限检查，过滤器仍跑）。若 §2.3 实测 session 建不起来，把 `/sso/weknora` 从本链移除，改由 §2.4 的独立链处理。

### 4.7 nginx（:80）代理 IdP 端点给浏览器
在千铝国昱现有 nginx `server`（:80）加：
```nginx
location ~ ^/(oauth2/|userinfo$|\.well-known/) {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-Host  $host;
}
```
> 浏览器经 `:80` 访问 `/oauth2/authorize`；WeKnora 容器调 token 走 `host.docker.internal:8080` 直连后端。`issuer=http://43.138.163.7`（:80）与 nginx 对外一致。

### 4.8 构建/部署
```bash
# 本地 D:\workspace\qianlvguoyu_saas
mvn clean package -DskipTests
# 部署新 platform-api.jar 到 43.138.163.7:/opt/guoyu-platform/，按现有方式重启；nginx -s reload
```

## 5. 必须精确匹配的值

| 项 | 值 |
|---|---|
| client_id | `weknora` |
| redirect_uri | `http://43.138.163.7:8088/api/v1/auth/oidc/callback` |
| scope | `openid profile email` |
| issuer | `http://43.138.163.7` |
| client_secret | §4.5 生成；与 WeKnora `.env` `OIDC_AUTH_CLIENT_SECRET` 一致 |
| id_token claims | 必含 `email`（推荐含 `name`） |
| consent | `requireAuthorizationConsent=false` |

## 6. WeKnora 侧（已在 43.138.163.7 预置，待启用）

文件 `/opt/guoyu-platform/WeKnora/.env`（OIDC 块已写好，`OIDC_AUTH_ENABLE=false`）。公司 IdP 就绪后：
```bash
OIDC_AUTH_CLIENT_SECRET=<§4.5 的密钥>
OIDC_AUTH_ENABLE=true
cd /opt/guoyu-platform/WeKnora && docker compose up -d app
```
预置端点（无需改）：
```
OIDC_AUTH_ISSUER_URL=http://43.138.163.7
OIDC_AUTH_AUTHORIZATION_ENDPOINT=http://43.138.163.7/oauth2/authorize   # 浏览器
OIDC_AUTH_TOKEN_ENDPOINT=http://host.docker.internal:8080/oauth2/token  # 容器→后端
# userinfo 已注释：仅靠 id_token 取 email
OIDC_USER_INFO_MAPPING_EMAIL=email        # 与你们发的 claim 名一致，无需改
OIDC_USER_INFO_MAPPING_USER_NAME=name     # = displayName，无需改
```

## 7. 联调验证

1. **桥接自测**：用合法 Bearer 调 `http://43.138.163.7/sso/weknora` → 应 200 返回 `authorizeUrl` 且响应带 `Set-Cookie: JSESSIONID`。
2. **AS 自测**：浏览器访问上一步返回的 `authorizeUrl` → 应 **直接 302 回 WeKnora callback 带 code**（不跳 `/login`）。若跳 `/login` → §2.3/§2.4 的 session 问题。
3. **id_token 校验**：jwt.io 解码 id_token，确认 payload 有 `email`。
4. **WeKnora 启用**：按 §6 填密钥+ENABLE=true+重启。
5. **SSO 闭环**：登录 `:5173` → 点“进入 WeKnora” → 应直接进 WeKnora 工作台，不再输密码，按你的邮箱自动建号。

## 8. 安全注意
- 现全程 HTTP，code/token 有截获风险。**生产上 HTTPS**：给 `:8088` 和 `:80` 上域名+证书，所有 `http://43.138.163.7` 换 `https://你的域名`，redirect_uri 同步换。
- `client_secret` 生产用 `{bcrypt}`。
- RSA 密钥生产**必须持久化**（§4.2 注释）。

## 9. 故障排查
| 现象 | 排查 |
|---|---|
| 点 OIDC 登录跳到公司 `/login`（要再登录） | session 没建/没共享：先看 `/sso/weknora` 响应有无 `Set-Cookie: JSESSIONID`；有但仍跳登录 → 用 §2.4 独立链 |
| 回调报 `missing email` | id_token 里没 `email`：§4.3 是否给 Principal 补了 email、§4.4 TokenCustomizer 是否生效（断点确认 principal 是 PlatformUserPrincipal） |
| 回调 `invalid_state` | redirect_uri 在公司 IdP 未精确注册（一字不差） |
| 容器换 token 超时 | `host.docker.internal:8080` 不通：确认 WeKnora app 容器有 `extra_hosts: host.docker.internal:host-gateway`（已配） |
| `/sso/weknora` 401 | Bearer token 没带上或已过期；AppTokenAuthFilter 未在该请求跑（检查 filter 是否注册到匹配 /sso/weknora 的链） |

## 实施分工
- **千铝国昱后端（本地 agent）**：§2 桥接 + §4.3 补 email + §4.2/§4.4 AS + §4.6/§4.7 → §4.8 构建部署。
- **WeKnora 侧（本服务器，我来）**：公司 IdP 就绪后按 §6 启用，跑 §7 联调。
- **回传确认**：实施完告诉我两件事——(1) §2.3 实测 session 是否建起来（决定要不要 §2.4）；(2) id_token 里 `email`/`name` 最终 claim 名（决定 WeKnora 映射要不要改，目前预置 email/name）。
