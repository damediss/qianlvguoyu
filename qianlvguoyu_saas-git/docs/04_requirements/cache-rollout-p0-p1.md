Agent A的计划
# Redis 缓存改造 — 最小实施计划

> 范围:**仅 P0(Sa-Token 多 Pod 修复)+ P1 Top 3(`@Cacheable` 改造)**
> 写代码前请逐项确认,通过后再动手。

## 总体思路

| 阶段 | 改什么 | 风险 |
|---|---|---|
| P0-A | `pom.xml` 加 `sa-token-redis-jackson` | 需所有用户重新登录一次 |
| P0-B | `application.yml` 改 `token-style: tik → uuid` | 同上,token 长度变化 |
| P1-A | 加 `spring-boot-starter-cache` 依赖 + 新建 `config/CacheConfig.java` | 框架引入,无破坏 |
| P1-B | `MenuApplicationService.tree()` 加 `@Cacheable` + 写时 `@CacheEvict` | 5 处变更 |
| P1-C | `ExamWorkTypeApplicationService.tree()` 加 `@Cacheable` + 写时 `@CacheEvict` | 5 处变更 |
| P1-D | `AuthApplicationService.menus()` + `SaTokenBridgeFilter` 加 `@Cacheable` | 改 1 个方法、改 1 个 Filter |

> **不在范围内**:
> - Dashboard `CertificationDashboardApplicationService` / `TrainDashboardApplicationService` 全表扫描改造(P2 任务)
> - 限流 / 验证码 / 分布式锁 / chazheng 外部 API 缓存(后续 sprint)
> - Caffeine 本地一级缓存(后续)

---

## 步骤 0:前置准备

启动后端前确认两件事:

1. **Redis 已通** — 上一轮已验证 `43.138.163.7:6379` PING/SET/GET 全过,继续用。
2. **环境变量已设** — `REDIS_HOST / REDIS_PORT / REDIS_PASSWORD` 三个值你已设好。Spring Boot 启动时会自动注入到 `spring.data.redis.*`。

---

## 步骤 1:P0 — Sa-Token 切到 Redis(2 个文件)

### 1.1 `services/platform-api/pom.xml`

**位置**:`<dependencies>` 块,sa-token 现有声明的下一行。

**新增**:
```xml
<dependency>
  <groupId>cn.dev33</groupId>
  <artifactId>sa-token-redis-jackson</artifactId>
  <version>${sa-token.version}</version>
</dependency>
```

> 不需要额外配 Lettuce / Jedis — `spring-boot-starter-data-redis` 已在 pom,且 `application.yml` 已配 `spring.data.redis.*`,sa-token-redis-jackson 会自动用。

### 1.2 `services/platform-api/src/main/resources/application.yml`

**位置**:`sa-token:` 段。

**改动**:
```yaml
sa-token:
  token-name: Authorization
  token-prefix: Bearer
  timeout: 259200
  active-timeout: -1
  is-concurrent: true
  is-share: true
  is-log: true
  token-style: uuid          # ← 原值是 tik
  is-read-cookie: false
```

**无需**额外加 `sa-token.redis.*` 段 — `sa-token-redis-jackson` 会自动从 `spring.data.redis.*` 读连接。

### 1.3 P0 验收

1. 启动后端,日志找 `Redis connected` 或 `LettuceConnectionFactory` 成功 init
2. 登录拿一个 token
3. `redis-cli -h 43.138.163.7 keys '*satoken*'` 能看到 key(说明 token 确实写到 Redis 了)
4. **不需要重新部署验证多 Pod** — 改造完就支持,部署时配两个 Pod 自动生效

---

## 步骤 2:P1-A — 加 Spring Cache 基础设施(2 个文件)

### 2.1 `services/platform-api/pom.xml`

**位置**:`<dependencies>` 块。

**新增**:
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

### 2.2 新建 `services/platform-api/src/main/java/com/guoyu/platform/config/CacheConfig.java`

```java
package com.guoyu.platform.config;

import java.time.Duration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class CacheConfig {

  @Bean
  public RedisCacheManager cacheManager(RedisConnectionFactory cf) {
    RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(10))
        .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
            new GenericJackson2JsonRedisSerializer()))
        .disableCachingNullValues()
        .prefixCacheNameWith("saas:");

    return RedisCacheManager.builder(cf)
        .cacheDefaults(base)
        .withCacheConfiguration("menuTree", base.entryTtl(Duration.ofMinutes(30)))
        .withCacheConfiguration("workTypeTree", base.entryTtl(Duration.ofHours(1)))
        .withCacheConfiguration("userMenus", base.entryTtl(Duration.ofMinutes(10)))
        .withCacheConfiguration("userRoles", base.entryTtl(Duration.ofMinutes(10)))
        .withCacheConfiguration("userPerms", base.entryTtl(Duration.ofMinutes(10)))
        .build();
  }
}
```

**键约定**:`saas:menuTree::{tenantId}#ROOT`、`saas:userMenus::{userId}` 等(Spring Cache 内部用 `::` 分隔 cacheName 和 key)。

### 2.3 P1-A 验收

- 启动后端,日志没 `@EnableCaching` 相关 warning
- 暂未加 `@Cacheable`,所以 cache 里没东西,纯框架就绪

---

## 步骤 3:P1-B — 缓存 `MenuApplicationService.tree()`(2 个文件)

### 3.1 改动 `MenuApplicationService.java`

**位置**:`tree()` 方法。

**在方法签名加 `@Cacheable`,在所有写方法(`create` / `update` / `delete` / `deleteWithChildren` / `batchUpdateVisible`)加 `@CacheEvict`**。

**改动示意**(实际改动会逐方法贴):

```java
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

// 1) 读方法
@Cacheable(value = "menuTree", key = "#root.target.tenantId() + ':#ROOT'", unless = "#result == null")
@Transactional(readOnly = true)
public List<MenuTreeVO> tree() { ... }   // 不改实现

// 2) 写方法(5 个:create / update / batchUpdateVisible / delete / deleteWithChildren)
// 关键:写操作要 evict 该租户的整棵 cache。Menu 改动会同时影响 userMenus。
@Caching(evict = {
    @CacheEvict(value = "menuTree", allEntries = true),       // 同租户所有 tree 缓存
    @CacheEvict(value = "userMenus", allEntries = true),      // 所有用户的菜单
    @CacheEvict(value = "userPerms", allEntries = true)       // 权限也可能跟 menu 变更有关
})
@Transactional
public MenuTreeVO create(MenuCreateReq req) { ... }   // 不改实现
```

**问题**:`menuTree` 和 `userMenus` 用 `allEntries=true` 会**清空整个 cache**,跨租户也会被清。租户隔离方案:

- **方案 A(推荐,简单粗暴)**:`allEntries=true` + 接受这个开销
  - 菜单变更不频繁,管理员操作,接受几秒到几十分钟内的全 cache miss
  - 实现简单,零额外代码
- **方案 B(精准)**:用 `key = "#tenantId + ':#ROOT'"`,写时按 `tenantId` 单独 evict
  - 需要把 `tenantId` 作为参数传入或从 `SecurityContext` 抽
  - Spring Cache 的 `key` 写 SPEL 拼租户,写方法加 `@CacheEvict(value="menuTree", key="#tenantId + ':#ROOT'")`
  - 复杂度高一点点

**默认采用方案 A**。若你坚持方案 B,审核时说一声。

**待办:确认你偏好哪种方案。**

### 3.2 P1-B 验收

1. 调一次 `GET /system/menu/tree`,redis 里出现 `saas:menuTree::*` key
2. 再调一次,日志没新 SQL(`mapper.selectList` 没再触发)
3. 调 `POST /system/menu`(新增一个菜单)
4. 再调 `GET /system/menu/tree`,SQL 又触发了(说明 evict 生效)
5. 新调一次又被缓存

---

## 步骤 4:P1-C — 缓存 `ExamWorkTypeApplicationService.tree()`(1 个文件)

### 4.1 改动 `ExamWorkTypeApplicationService.java`

**位置**:`tree()` 方法和写方法(`create` / `update` / `delete`)。

**注意**:`tenantId` 是 `Long`,方法内部转成 `int` 查的。cache key 走 `String` 即可。

**改动示意**:

```java
@Cacheable(value = "workTypeTree", key = "#root.target.tenantId() + ':#ROOT'", unless = "#result == null")
@Transactional(readOnly = true)
public List<ExamWorkTypeVO> tree() { ... }

// 写方法 3 个(create / update / delete)加 evict
@CacheEvict(value = "workTypeTree", allEntries = true)
@Transactional
public ExamWorkTypeVO create(ExamWorkTypeCreateReq req) { ... }
```

### 4.2 P1-C 验收

同 3.2,但只针对 `/exam/work-type/tree` 端点 + `exam-work-type` CRUD。

---

## 步骤 5:P1-D — 缓存 `AuthApplicationService.menus()` + 改 `SaTokenBridgeFilter`(2 个文件)

> 这是**最有价值**的一步,直接砍每个鉴权请求 3 次 SQL。但**最复杂**的一步。

### 5.1 改动 `SaTokenBridgeFilter.java`

**核心改造**:把 3 次 SQL 查询(`userMapper` / `userRoleMapper` / `roleMenuMapper`)的结果塞进 `StpUtil.getTokenSession()`,下次请求从 session 直接拿。

**改动示意**:

```java
private void authenticate(String token) {
  Object loginIdObj = StpUtil.getLoginIdByToken(token);
  if (loginIdObj == null) return;
  Long userId = Long.valueOf(loginIdObj.toString());

  // 1) 优先从 Sa-Token session(Redis)拿 principal,跳过 3 次 SQL
  Object cached = StpUtil.getTokenSession().get("principal");
  PlatformUserPrincipal principal;
  if (cached instanceof PlatformUserPrincipal p) {
    principal = p;
  } else {
    // 2) 冷路径:查 DB,登录时 buildPrincipal 逻辑跟 AuthApplicationService 一样
    UserEntity user = userMapper.selectOne(...);
    if (user == null) return;
    List<String> roleCodes = userRoleMapper.selectRoleCodes(...);
    List<String> permissionCodes = roleMenuMapper.selectPermissionCodesByUser(...);
    if (roleCodes == null) roleCodes = List.of();
    if (permissionCodes == null) permissionCodes = List.of();
    principal = new PlatformUserPrincipal(
        user.getId(), user.getTenantId(), user.getUsername(),
        user.getDisplayName(), user.getUserType(), roleCodes, permissionCodes);
    StpUtil.getTokenSession().set("principal", principal);  // 写回 Redis
  }

  UsernamePasswordAuthenticationToken authentication =
      new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
  SecurityContextHolder.getContext().setAuthentication(authentication);
}
```

**注意**:
- `PlatformUserPrincipal` 已经有 `UserDetails` 实现,Jackson 默认支持反序列化(只要不引 Jackson 模块限制)。
- `getTokenSession()` 在 P0 改造后会**自动走 Redis**(sa-token-redis-jackson)。
- 如果 principal 失效(用户被禁用、改密码),仍需走冷路径查 DB 验证 — 当前代码已经有 `userMapper.selectOne(... eq status=enabled)`,保持原样。

### 5.2 改动 `AuthApplicationService.menus()`

**目标**:把"按用户查菜单"的结果缓存到 `userMenus` cache。

**改动示意**:

```java
@Cacheable(value = "userMenus", key = "#userId", unless = "#result == null")
public List<MenuVO> menusForUser(Long userId) { ... }   // 把 menus() 的查询逻辑挪过来

// 原 menus() 改成从 principal 拿 userId,转发
public List<MenuVO> menus() {
  Long userId = SecurityContext.currentUser().getUserId();
  return menusForUser(userId);
}
```

**问题**:`menus()` 当前依赖 `SecurityContext.currentUser()` 拿 `tenantId`,而 `@Cacheable` 拦截时 SPEL 不能直接拿这个。两种解法:

- **方案 A**:把 `menusForUser(Long userId)` 拆出来,内部用 mapper 查 `user` 拿 tenantId
  - 多 1 次 SQL,砍了 2 次(原本每鉴权 3 次 → 缓存命中 0 次,缓存未命中 4 次)
- **方案 B**:依赖 P1-D-1(SaTokenBridgeFilter 缓存 principal),直接拿 principal.getTenantId()
  - 缓存命中 0 次 SQL,未命中 1 次 user + 2 次 role/perm,3 次**降为 0**(但 menu 自身查还要算)

**默认采用方案 B**,与 P1-D-1 强耦合。如果 P1-D-1 暂时不通过,P1-D-2 单独走 A 也能跑(只是收益打折)。

### 5.3 P1-D 验收

1. 启动后端,登录拿 token
2. 第一次调任意鉴权接口 → 日志有 3 次 SQL
3. 第二次调 → **0 次 SQL**(principal 从 Redis 拿)
4. 调 `GET /auth/menus` → 日志 0 SQL(从 userMenus 缓存拿)
5. 调 `POST /system/role/bind-permissions` → 失效缓存后再调 → SQL 又出现
6. 重启后端服务,token 仍能鉴权(证明 Sa-Token session 真的在 Redis 里)

---

## 改动文件清单(总览)

| # | 文件 | 类型 | 行数估 |
|---|---|---|---|
| 1 | `services/platform-api/pom.xml` | 改 | +7 行(2 个 dependency) |
| 2 | `services/platform-api/src/main/resources/application.yml` | 改 | 1 行(token-style) |
| 3 | `services/platform-api/src/main/java/com/guoyu/platform/config/CacheConfig.java` | 新建 | 40 行 |
| 4 | `services/platform-api/src/main/java/com/guoyu/.../system/service/MenuApplicationService.java` | 改 | +15 行(imports + 注解) |
| 5 | `services/platform-api/src/main/java/com/guoyu/.../exam/service/ExamWorkTypeApplicationService.java` | 改 | +12 行 |
| 6 | `services/platform-api/src/main/java/com/guoyu/.../auth/service/AuthApplicationService.java` | 改 | +10 行 |
| 7 | `services/platform-api/src/main/java/com/guoyu/.../security/filter/SaTokenBridgeFilter.java` | 改 | +20 行 |

**总计:7 个文件,5 改 + 1 新 + 1 改。** 预计 1.5-2 小时完成。

---

## 风险点(实施时关注)

1. **跨租户 cache 隔离**:用 `allEntries=true` 简单但会"误伤"其他租户。可接受,因为 menu/workType 写操作极少。
2. **`@Cacheable` SPEL 里取 `tenantId`**:`#root.target.tenantId()` 这种写法 — `MenuApplicationService` 没有 `tenantId()` 方法,要么加一个 private helper,要么改 `key` 用 `@Cacheable(value=..., key="...")`,用 `SecurityContextHolder` 静态调用。更稳的做法:**写一个 `TenantIds.currentTenantId()` 工具类**(`com/guoyu/platform/common/tenant/TenantContextHolder` 已有,直接复用),然后 `@Cacheable(value="menuTree", key="T(com.guoyu...).TenantContextHolder.get().getTenantId() + ':#ROOT'")`。
3. **`@Cacheable` + `@Transactional` 顺序**:Spring 拦截器是 cache 在外、tx 在内,意味着 cache 命中就**完全不会进事务**,没问题。冷路径会进事务,也没问题。
4. **测试环境 `application-test.yml` 的 Redis 配置**:目前指向 `localhost:6379`(没用),加了 `@Cacheable` 后,如果跑 `@SpringBootTest` 没起 testcontainer,会**连不上 redis 报错**。本轮先不动 — 但你后面要加测试的话,记得处理。
5. **`sa-token-redis-jackson` 1.42.0 是否兼容** `spring-boot 3.3.x` + `spring-data-redis` 当前版本:这是社区主流组合,绝大多数情况直接能跑。如果启动报错,再单独排查。

---

## 实施步骤(执行顺序)

1. 先做 P0(1.1 + 1.2)— 启动后端验证 token 进 Redis
2. 再做 P1-A(2.1 + 2.2)— 启动后端验证 @EnableCaching 生效
3. 再做 P1-B(3.1)— 验证 menu 缓存
4. 再做 P1-C(4.1)— 验证 workType 缓存
5. 最后做 P1-D(5.1 + 5.2)— 验证最热路径(每个鉴权请求 3 SQL → 0 SQL)

每步独立验收,通过再做下一步。任何一步失败就停。

---

## 待你确认的 3 个选择

1. **菜单/工种 cache 失效方案 A vs B?**
   - A(简单,跨租户全清):推荐
   - B(精准,按租户 evict):复杂度高

2. **P1-D 强依赖 P1-D-1 一起做,还是只做 P1-D-2(menus 缓存)?**
   - 一起做:收益最大,改动量也最大
   - 只做 P1-D-2:收益打折但简单

3. **`@Cacheable` 的 `key` 里取 `tenantId` 用 SPEL `T(全限定类名)` 还是新建一个工具方法?**
   - 工具方法:更易读,推荐
   - SPEL 直引:行数少一点

审核后告诉我这三个选择,我再按确认的方案开始改代码。


Agent B的复核结果
我已读完改造计划、4 份 wiki、以及计划里涉及的全部 7 个源文件 + 测试配置。下面是核实结果，**有几个致命问题会让计划直接跑不起来**，先看这些。

## 总体结论

**P0（Sa-Token → Redis）基本可行**，但有 1 个隐藏的地雷。
**P1-B / P1-C（@Cacheable 改 tree）方向对，但计划里的 SPEL 写法是错的**，照抄编译不过 / 运行空指针。
**P1-D（principal 缓存）有 2 个致命问题 + 1 个安全回归**，不建议按当前设计做。

---

## 🔴 致命问题（不解决就跑不起来）

### 1. P1-D：`PlatformUserPrincipal` 无法被 Jackson 反序列化

计划步骤 5.1"注意"第 1 条说：*"PlatformUserPrincipal 已经有 UserDetails 实现，Jackson 默认支持反序列化"*。

**核实结论：这个说法是错的。** `PlatformUserPrincipal.java:9-39` 的实际情况：

- 类**没有 `implements Serializable`**（`UserDetails` 不继承 `Serializable`）
- 类**只有一个 7 参数全参构造函数**（第 20-39 行），**没有无参构造、没有 `@JsonCreator`、字段全是 `final`**
- `authorities` 字段类型是 `Collection<? extends GrantedAuthority>`，`SimpleGrantedAuthority` 也没无参构造

切到 `sa-token-redis-jackson` 后，`login()` 写入 session 的 principal（`AuthApplicationService.java:90`），下次请求 `getTokenSession().get("principal")` 反序列化时**会抛异常**。

而且我发现 `AuthApplicationService.login()` **本来就已经在写 `StpUtil.getTokenSession().set("principal", principal)` 了** —— 所以 P1-D-1 根本不是"新增写入"，而是"修复反序列化"。计划对现状的描述本身就有偏差。

要修就得给 `PlatformUserPrincipal` 加无参构造 + setter（破坏 immutability），或换 Sa-Token 的序列化器（如 `sa-token-redis-jackson` 改 `sa-token-redis-jackson` 配 `SaTokenContextRegister`），或干脆**不在 session 里放 principal，改成只放 userId，filter 每次查 DB + 自己加缓存**。

### 2. P1-D：被禁用/改密码的用户 3 天内仍可访问（安全回归）

计划步骤 5.1"注意"第 3 条说：*"如果 principal 失效，仍需走冷路径查 DB 验证 — 当前代码已经有 status=enabled 检查"*。

**核实结论：计划给的代码（步骤 5.1 第 244-260 行）一旦命中 cache 就直接 `return`，根本不查 DB。**

`SaTokenBridgeFilter.java:65-68` 现在每次都查 `status='enabled'`，管理员禁用用户 → 下一个请求立即失效。改造后命中 cache → 禁用用户在 token timeout（3 天）内继续放行。`active-timeout: -1`（永不过期）让这个窗口无法缩短。**这是权限降级**，计划完全没评估风险。

### 3. P1-B / P1-C：`@Cacheable` 的 SPEL key 写法不成立

计划多处写 `key = "#root.target.tenantId() + ':#ROOT'"`：

**核实结论：`MenuApplicationService` 和 `ExamWorkTypeApplicationService` 都没有 `tenantId()` 方法。**
- `MenuApplicationService.java:30-41` 只有 3 个 mapper 字段
- `ExamWorkTypeApplicationService.java:28-34` 只有 1 个 mapper 字段
- 两个类的 `tree()` 都是内部调 `SecurityContext.currentUser().getTenantId()`（第 45 / 72 行）

计划自己在"风险点 #2"提了一嘴，但又给了第二套**同样错误**的写法：`T(com.guoyu...).TenantContextHolder.get().getTenantId()`。

**核实 `TenantContextHolder.java:14-16`：是静态方法 `getTenantId()`，不是 `get().getTenantId()`。** 计划的两套 SPEL 都跑不起来。

可行的写法是：`key = "T(com.guoyu.platform.common.tenant.TenantContextHolder).getTenantId()"`。但要注意 `TenantContextFilter` 和 `SaTokenBridgeFilter` 的执行顺序（`SecurityConfig.java:56-57` 两个都 `addFilterBefore`，实际 tenant 在前、sa-token 在后），到 `tree()` 执行时 ThreadLocal 是有值的，可以用。

### 4. P1-D-2：`userMenus` 在角色变更时不会被 evict

计划步骤 5.3 验收第 5 条说"调 `/system/role/bind-permissions` → 失效缓存后再调 → SQL 又出现"。

**核实结论：改动文件清单（步骤 6）里没有 `RoleApplicationService`、`UserApplicationService`。** 给用户加/减角色、给角色绑权限，都不会清 `userMenus` cache。用户看到的菜单/权限视图会脏，直到 TTL（10 分钟）过期。

`menus()` 的逻辑（`AuthApplicationService.java:111-128`）依赖 `currentUser.getRoleCodes()` 做 `isPlatformAdmin` 分支判断 —— 角色 cache 脏了不只是菜单错，连走哪个分支都可能错。

---

## 🟡 计划内部不一致 / 小瑕疵

| # | 位置 | 问题 |
|---|---|---|
| 5 | CacheConfig.java 第 128-129 行 | 定义了 `userRoles`、`userPerms` 两个 cache，但整个计划里**没有任何 @Cacheable 用它们**。P1-D-1 把权限缓存塞进了 principal（在 Sa-Token session 里），跟 Spring Cache 的 `userPerms` 是两套机制。配置多余。 |
| 6 | "总体思路"P1-B/P1-C 说"5 处变更" | `MenuApplicationService` 确有 5 个写方法（create/update/batchUpdateVisible/delete/deleteWithChildren），但 `ExamWorkTypeApplicationService` 只有 3 个写方法（create/update/delete）。计划步骤 4.1 自己又写"3 个"，前后矛盾。 |
| 7 | 步骤 5.1 冷路径代码 | 完整复制了现有 filter 的 3 次 SQL，没简化。说好的"省 SQL"只在热路径（session 命中）成立。 |
| 8 | 步骤 1.3 验收 `redis-cli keys '*satoken*'` | sa-token-redis-jackson 1.42 的实际 key 前缀是 `satoken:login:token:*` / `satoken:login:session:*`，`*satoken*` 能匹配但验收时最好确认具体 key 形态。 |

---

## ⚪ 计划没核实、我也还没读的点（不阻塞，但实施前要确认）

1. `MenuTreeVO` / `ExamWorkTypeVO` 是否有无参构造 —— 用 `GenericJackson2JsonRedisSerializer` 序列化 `List<MenuTreeVO>`（带嵌套 children）反序列化时需要。我没读这两个 VO。
2. `application-test.yml:18-21` Redis 指向 `localhost:6379` 无密码。目前只有 2 个 `@SpringBootTest`（`CertificationProjectMapperDatabaseTest`、`CertificationDocumentWorkflowServiceTest`），且都不走 cache，短期不受影响。但加 `@EnableCaching` 后如果集成测试要扩展，得处理。计划"风险点 #4"提到了，承认了不处理。
3. `MenuApplicationServiceTest.java:16` 是纯 `new` 不走 Spring 代理，`@Cacheable` 不生效 —— 这个测试既不受影响也测不到 cache。计划没提 cache 的单测怎么写。

---

## 我的建议（等你拍板）

**P0**：可以做，但 P1-D-1 的反序列化地雷在 P0 切到 redis-jackson 后就会**立刻**引爆（因为 `login()` 已经在写 session principal 了）。建议 P0 必须和"修 PlatformUserPrincipal 反序列化"一起做，不能拆。

**P1-B / P1-C**：方向对，但需要把 SPEL key 改成正确的 `T(...TenantContextHolder).getTenantId()`，并接受 `allEntries=true` 跨租户清缓存。

**P1-D**：当前设计有安全回归（禁用用户不立即生效）+ 缓存脏数据（role 变更不清 cache）。建议**暂缓**，或改成更简单的方案——filter 里用 Spring Cache 缓存 `(userId → principal)` 并设短 TTL（如 30s），既省 SQL 又保证禁用用户 30s 内失效。

---

## 需要你回答的几个问题

1. **P1-D 这一步要不要做？** 鉴于有安全回归 + role 变更不 evict 两个硬伤，我倾向于建议**本轮只做 P0 + P1-A + P1-B + P1-C**，P1-D 单独立项重设计。你怎么看？
2. **`PlatformUserPrincipal` 反序列化**：这是 P0 的前置。可以接受给这个类加无参构造 + setter（破坏 immutability），还是要换 Sa-Token 序列化方案（改用 `sa-token-redis-jackson` 的同时配自定义 `SaTokenSerializer`）？
3. **菜单/工种 cache 跨租户 `allEntries=true`**：你确认接受"一个租户改菜单，全租户 menuTree cache 失效"这个代价吗？还是必须按租户精准 evict（方案 B）？
4. **`tree()` 的 cache key**：确认用 `TenantContextHolder.getTenantId()`（静态 SPEL）作为 key？还是要我加一个实例方法 `tenantId()` 让 `#root.target.tenantId()` 能用？

回答完这 4 个我再开始动代码。