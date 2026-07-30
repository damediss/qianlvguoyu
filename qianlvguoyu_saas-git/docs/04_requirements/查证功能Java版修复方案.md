# 查证功能 Java 版修复方案

> 目标：修复 Java 版查证核心逻辑，使其与 PHP 版行为一致，正确处理第三方证书查询接口响应。PHP项目代码目录：D:\workspace\qlgt-office_chazhengback\

---

## 〇、审核修订记录

### v1.2（本次）

本次审核发现以下缺陷并修正：

| # | 缺陷/疑议 | 严重度 | 修正章节 |
|---|---|---|---|
| 1 | **isChecked 更新逻辑反了** — 原方案用 `anyAbnormal` 判断，"一轮正常一轮异常"时 isChecked 不更新，与 PHP 行为矛盾 | 🔴 重大 | §4.5 改为 `anyNormal` 判断 |
| 2 | **persistResult 自调用事务不生效** — @Transactional 标注的内部方法被同 Bean 调用时 Spring AOP 不拦截 | 🔴 重大 | §7.3 改为独立 Bean |
| 3 | **catch (Exception e) 太宽泛** — 应只捕获 RestTemplate 相关异常 | 🟡 中 | §7.2 缩小异常范围 |
| 4 | **data 数组元素缺类型检查** — 遍历时未检查元素是否为 Map | 🟡 中 | §7.2 补充 instanceof 检查 |
| 5 | **日志文件名特殊字符** — userName 含 `/` 等会创建文件失败 | 🟡 中 | §4.6 文件名安全处理 |
| 6 | **日志目录不存在** — 写日志前未创建目录 | 🟢 小 | §4.6 自动创建目录 |
| 7 | **日志含敏感信息** — 身份证号明文记录 | 🟢 小 | §4.6 补充保留策略 |
| 8 | **SSL 跳过验证的生产风险** — 临时方案，生产应配信任库 | 🟢 小 | §4.3 补充说明 |
| 9 | **CertCheckResult/CollectedCert 类未定义** — 方案中引用但未给出完整代码 | 🟢 小 | §7.6 补充类定义 |
| 10 | **jsonHeaders()/logRequestResponse() 未定义** — 方法签名缺失 | 🟢 小 | §7.7 补充方法签名 |
| 11 | **HttpServerErrorException 未捕获** — catch 只捕 ResourceAccessException，5xx 响应会抛 HttpStatusCodeException 导致崩溃 | 🔴 重大 | §7.2 改为 catch RestClientException |
| 12 | **§4.2 措辞与 anyNormal 不一致** — 仍写"标记本轮异常"，应改为"不标记 anyNormal" | 🟡 中 | §4.2 更新措辞 |
| 13 | **persistResult 双括号初始化** — `new Entity() {{ }}` 创建匿名子类，MyBatis-Plus 可能不识别 | 🟢 小 | §7.3 改常规构造 |
| 14 | **并发查证同一用户竞态** — 两个请求同时通过 exists 检查导致重复插入 | 🟢 小 | §7.3 加唯一索引兜底说明 |

### v1.1（前次）

| # | 缺陷/疑议 | 严重度 | 修正章节 |
|---|---|---|---|
| 1 | **@Transactional 包裹 HTTP 调用** — 事务内做网络 IO，第三方接口慢会长时间占 DB 连接，重现 Redis 卡住拖垮连接池问题 | 🔴 重大 | §4.1 事务边界拆分 |
| 2 | **isChecked 更新逻辑矛盾** — 原方案"异常时不更新 isChecked"与"全部轮次完成后置 isChecked=1"互相矛盾 | 🔴 重大 | §4.2 修正状态更新逻辑 |
| 3 | **业务状态判断不完整** — 只判断 errorCode，遗漏 PHP 的 statusCode 和 status | 🔴 重大 | §4.4 三重判断 |
| 4 | **HTTP 重试偏离 PHP** — PHP 无重试，原方案加 2 次重试 | 🟡 中 | §4.3 对齐为不重试 |
| 5 | **SSL 验证未处理** — PHP 跳过 SSL，Java 默认验证，https 接口可能连不上 | 🟡 中 | §4.3 SSL 配置 |
| 6 | **部分轮次异常处理不清晰** — 第一轮异常+第二轮正常时最终 isChecked 应为 1 | 🟡 中 | §4.2 |
| 7 | **hasCert 每轮查 DB 低效** | 🟢 小 | §4.5 优化 |
| 8 | **licensefile 等扩展字段未提及** | 🟢 小 | §3.3 补充说明 |
| 9 | **日志路径未明确** | 🟢 小 | §4.6 |

---

## 一、现状分析

### 1.1 PHP 版核心逻辑

```php
// 1. 从 sys_dict 表读配置
$config = CmsGetcertconfig::findFirst("id=1");
$config['datas'] = json_decode($config['datas'], true);

// 2. 组装请求体（模板 + 用户信息覆盖）
$reqData = $config['datas'][$templateIdx];  // 取模板
$reqData['idcode']   = $_user['idcode'];    // 覆盖身份证
$reqData['username'] = $_user['username'];  // 覆盖姓名

// 3. 调用接口
$res = $this->curl($config['url'], $reqData);

// 4. 解析响应（嵌套路径 result.data[]）
$result = json_decode($res, true);
$data = $result['result']['data'];  // 两层嵌套

// 5. 字段映射
$cert_no   = $cert['serialid'];           // 证书编号
$cert_date = (string)$cert['bzz002'];     // 发证日期
// cert_name = zymc_gzmc(zyjndj) 拼合

// 6. 异常分流
// curl失败/JSON解析失败 → 不置 ischecked，下轮重试
// 业务异常 → 不置 ischecked，下轮重试
// 有证 → 增量写入，iscert=1, ischecked=1
// 无证 → 仅更新日志，ischecked=1

// 7. 去重
UserCert::findFirst(user_id + cert_no)  // 存在则跳过（增量）
```

### 1.2 Java 版当前问题

| # | 问题 | 现状 | 正确行为 |
|---|---|---|---|
| 1 | 配置来源 | `@Value("chazheng.cert-api-url")` 硬编码 | 从 `sys_dict` 表 `chazheng_url`/`chazheng_datas` 读 |
| 2 | 响应路径 | `response.data`（扁平） | `result.data[]`（两层嵌套） |
| 3 | 证书编号 | `data.certNo` | `data.serialid` |
| 4 | 发证日期 | `data.certDate` | `data.bzz002`（数字转 string） |
| 5 | 证书名称 | `data.certName`（直接取值） | `metadata.zymc + "_" + metadata.gzmc + "(" + metadata.zyjndj + ")"` 拼合 |
| 6 | 业务状态判断 | `code = "0"` | errorCode 为空 + statusCode=200/不存在 + status=200/0/不存在 三重判断 |
| 7 | 异常后状态 | `code != 0` 也置 `isChecked=1` | 异常时**不置** `isChecked`，留待重试 |
| 8 | 去量策略 | `delete` 全部旧证再 `insert`（全量替换） | `findFirst` 判重，存在则跳过（增量） |
| 9 | 请求体组装 | 只传 `{idcode, username}` | 从模板取基础字段（lscode, certType 等），再覆盖 idcode/username |
| 10 | 日志 | 异常时 `log.warn` | 每次请求/响应完整记录文件日志（对账） |
| 11 | 多模板轮询 | 单接口调用 | 两轮查证（datas[0] + datas[1]），同一用户可能查多本不同类型证书 |
| 12 | **事务边界** | `@Transactional` 包裹整个 certCheck（含 HTTP 调用） | HTTP 调用移出事务，DB 写入单独事务（避免占连接） |
| 13 | **SSL 验证** | Java 默认验证 | 跳过 SSL 验证（与 PHP `CURLOPT_SSL_VERIFYPEER=false` 一致） |

---

## 二、数据库配置

用户已在 `sys_dict` 表插入两条记录：

```sql
-- 第三方接口路径
INSERT INTO sys_dict (id, tenant_id, code, name, status, value)
VALUES (2075164235886440450, 1, 'chazheng_url', '广西技能人才评价网查证路径', 'enabled',
        'https://www.gxrcosta.com/ecopf/myInfos.do');

-- 请求 JSON 模板
INSERT INTO sys_dict (id, tenant_id, code, name, status, value)
VALUES (2075164518184071170, 1, 'chazheng_datas', '广西技能人才评价网查证传值模板', 'enabled',
        '[{"idcode": "", "lscode": "6", "certType": 1, "username": ""}]');
```

### 字段说明

| 字段 | 值 | 用途 |
|---|---|---|
| `chazheng_url.value` | `https://www.gxrcosta.com/ecopf/myInfos.do` | 第三方证书查询接口地址 |
| `chazheng_datas.value` | `[{"idcode": "", "lscode": "6", "certType": 1, "username": ""}]` | 请求 JSON 模板数组 |

`chazheng_datas` 是 JSON 数组，每个元素为一轮查证模板：
- `datas[0]`：第一轮查证（如职业技能等级证书）
- `datas[1]`：第二轮查证（如特种作业操作证，可选）

模板中空字符串字段（`idcode`/`username`）在运行时替换为当前用户的真实值。

---

## 三、第三方接口协议（已确认）

### 3.1 请求格式

```http
POST https://www.gxrcosta.com/ecopf/myInfos.do
Content-Type: application/json

{
  "lscode": "6",
  "certType": 1,
  "idcode": "452624198404280738",
  "username": "黄俊规"
}
```

| 字段 | 来源 | 说明 |
|---|---|---|
| `lscode` | 模板（`chazheng_datas.value[0].lscode`） | 固定值 "6" |
| `certType` | 模板（`chazheng_datas.value[0].certType`） | 固定值 1 |
| `idcode` | 当前用户身份证 | 运行时从 `chazheng_user.id_card` 覆盖 |
| `username` | 当前用户姓名 | 运行时从 `chazheng_user.user_name` 覆盖 |

### 3.2 响应格式

```json
{
  "result": {
    "data": [
      {
        "serialid": "2024-XXXXX",
        "bzz001": "职业技能等级证书",
        "bzz002": 20230105,
        "bzz003": "...",
        "bzz004": "...",
        "aae013": "",
        "licensefile": "JVBERi0x...",
        "metadata": {
          "zymc": "起重装卸机械操作工",
          "gzmc": "桥式起重机司机",
          "zyjndj": "四级/中级工",
          "pjjg": "广西工业技师学院",
          "bfjg": "广西工业技师学院",
          "sysj": 20230105
        }
      }
    ]
  }
}
```

### 3.3 字段映射表

| Java 字段 | 响应路径 | 类型转换 | 备注 |
|---|---|---|---|
| `certNo` | `result.data[i].serialid` | 直接取字符串 | 证书编号，业务主键之一 |
| `certDate` | `result.data[i].bzz002` | `(String) bzz002`（数字→字符串）| 发证日期 |
| `certName` | 见下方拼合规则 | 拼合 | 证书名称显示用 |

### 3.3.1 chazheng_cert 唯一索引（审核修订 v1.2 #14）

并发查证同一用户时，`exists + insert` 存在竞态。应在 `chazheng_cert` 表加唯一索引兜底：

```sql
ALTER TABLE chazheng_cert
  ADD UNIQUE KEY uk_user_cert_no (user_id, cert_no);
```

Java 端 `catch (DuplicateKeyException)` 兜底跳过，不报错。

> 如果表已有此索引则无需重复添加。实施时应先检查 `SHOW INDEX FROM chazheng_cert`。

**当前不处理但保留扩展的字段**（审核修订 #8）：

| 响应字段 | 含义 | 当前处理 | 后续可扩展 |
|---|---|---|---|
| `bzz001` | 证书类型名称（如"职业技能等级证书"） | 不存储 | 可加 `certType` 列 |
| `licensefile` | base64 编码的证书 PDF | 不存储 | 可存文件 ID 关联文件服务 |
| `bzz003`/`bzz004` | 未知扩展字段 | 不存储 | 待业务确认用途 |
| `metadata.pjjg` | 评价机构 | 不存储 | 可加 `evalInstitution` 列 |
| `metadata.bfjg` | 颁发机构 | 不存储 | 可加 `issueInstitution` 列 |
| `metadata.sysj` | 发证日期（metadata 内，可能与 bzz002 重复） | 不存储 | 以 bzz002 为准 |

### 3.4 cert_name 拼合规则

```
输入：metadata.zymc / metadata.gzmc / metadata.zyjndj

规则（优先级从高到低）：
1. zymc/gzmc/zyjndj 全部非空 → "zymc_gzmc(zyjndj)"
   示例："起重装卸机械操作工_桥式起重机司机(四级/中级工)"

2. zymc 非空 + gzmc 为空/"--"/"----"（纯破折号）→ "zymc(zyjndj)"
   示例："电工(四级/中级工)"（电工类无工种细分）

3. zymc 为空 + gzmc 非空 → "gzmc(zyjndj)"
   示例：仅工种无专业时使用

4. zymc/gzmc 都为空 + zyjndj 非空 → "zyjndj"
   示例：仅有等级信息时使用

5. 全部为空 → 兜底用顶层 azf040（发证机构）
   （实际响应中可能无此字段，此时 cert_name 为空则跳过该条）

**关于 azf040 的说明**：PHP 代码兜底取 `$cert['azf040']`，但实际接口响应中没有 `azf040`
字段（metadata 中对应的是 `bfjg` 颁发机构 / `pjjg` 评价机构）。因此该兜底分支从未触发，
cert_name 为空。如果后续需要兜底，建议使用 `metadata.bfjg`，但这属于增强项，本次对齐
PHP 保持原样（cert_name 为空时跳过该条）。

特殊处理：
- gzmc 规范化：trim 后若匹配正则 `/^-+$/`（纯破折号）→ 视为空
- cert_no 为空 → 跳过该条，不写入（避免脏数据）
```

---

## 四、修复后的核心流程

### 4.1 事务边界拆分（审核修订 #1 — 重大）

**原方案缺陷**：`certCheck` 标了 `@Transactional`，整个方法（含 HTTP 调用）在一个事务内。第三方接口慢时（10s 超时），DB 连接被占 10s，高并发下连接池耗尽。

**修正方案**：拆成两层，HTTP 调用不在事务内：

```java
// 外层方法：无事务，编排 HTTP 调用 + 状态更新
public ChazhengUserVO certCheck(Integer id) {
    ChazhengUserEntity user = findExistingUser(tenantId, id);   // 非事务读

    // HTTP 调用 + 响应解析（无事务，不占 DB 连接）
    CertCheckResult result = doCertCheckWithoutTx(user);

    // DB 写入（短事务）
    chazhengPersistService.persistResult(user, result);              // @Transactional

    return buildUserVO(user);
}
```

| 层 | 事务 | 耗时操作 | 占 DB 连接时长 |
|---|---|---|---|
| 外层编排（certCheck） | 无 | 网络IO（0~10s） | 0 |
| processOneTemplate（循环内） | 无 | HTTP 调用 + 响应解析 | 0 |
| persistResult（独立 Bean） | `@Transactional` | insert/update（<100ms） | <100ms |

### 4.2 异常分流与状态更新（审核修订 #2 + #6 — 重大）

**原方案缺陷**："异常时不更新 isChecked"与"全部轮次完成后置 isChecked=1"矛盾，部分轮次异常时行为不清晰。

**修正后的状态更新逻辑**（对齐 PHP）：

```
遍历所有模板轮次，记录每轮结果：

轮次1：正常有证  → 写证 + 标记 anyNormal + anyCertFound
轮次1：正常无证  → 记日志 + 标记 anyNormal
轮次1：异常      → 记日志，不标记 anyNormal（本轮视为异常）
轮次2：...同上

全部轮次结束后：
  isCert：只要任一轮查到证 → 1，否则不变
  isChecked：
    ├─ 只要任一轮"正常"（有证或无证都算正常）→ 1
    └─ 全部轮次都异常 → 不更新（留待重试）
```

**关键**：PHP 在有证分支内**立即**更新 iscert=1, ischecked=1；在无证分支内更新 ischecked=1。Java 对齐为：只要任一轮正常，最终 ischecked=1。只有**全部轮次异常**时才不置 ischecked。

```
示例：
  轮次1（职业技能）：异常
  轮次2（特种作业）：正常有证

  最终状态：iscert=1, isChecked=1
  （轮次2 正常，所以 isChecked=1；轮次1 异常被"掩盖"，不再单独重试）

  ——这与 PHP 行为一致，PHP 也是两轮顺序执行，第二轮正常就置 ischecked=1
```

### 4.3 HTTP 调用（审核修订 #4 + #5）

**重试策略**：与 PHP 对齐，**不重试**。curl 失败直接记日志、标记异常，留待下一轮/手动重试。

> 如果后续需要重试能力，应作为独立增强项提出，不混入本次对齐修复。

**SSL 配置**：PHP 用 `CURLOPT_SSL_VERIFYPEER=false` 跳过 SSL 验证。Java 需配置 `RestTemplate` 跳过 SSL 验证（与 PHP 一致），否则 `https://www.gxrcosta.com` 证书有问题时连不上。

> ⚠️ **生产环境风险**（审核修订 v1.2 #8）：跳过 SSL 验证会使连接容易受到中间人攻击。本方案与 PHP 对齐采用跳过验证，**生产环境建议**：
> - 将 `www.gxrcosta.com` 的证书导入 Java 信任库（`keytool -importcert`）
> - 或配置 Spring `RestTemplate` 使用自定义信任库（仅信任该域名证书）
> - 仅在开发/测试环境使用全局跳过验证

```java
// RestTemplate 配置（跳过 SSL 验证，与 PHP CURLOPT_SSL_VERIFYPEER=false 对齐）
@Bean
public RestTemplate chazhengRestTemplate() throws Exception {
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, new TrustManager[]{new X509TrustManager() {
        public void checkClientTrusted(X509Certificate[] c, String s) {}
        public void checkServerTrusted(X509Certificate[] c, String s) {}
        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
    }}, new SecureRandom());

    HttpClient client = HttpClients.custom()
        .setSSLContext(sslContext)
        .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
        .setConnectTimeout(Duration.ofMillis(5000))
        .setResponseTimeout(Duration.ofMillis(10000))
        .build();

    return new RestTemplate(new HttpComponentsClientHttpRequestFactory(client));
}
```

**超时配置**：
- connectTimeout: 5000ms（与 PHP `CURLOPT_CONNECTTIMEOUT=5` 一致）
- readTimeout: 10000ms（PHP 无 read 超时，Java 建议设防卡死）

### 4.4 业务状态判断（审核修订 #3 — 重大）

**原方案缺陷**：只判断 `errorCode`，遗漏 PHP 的 `statusCode` 和 `status`。

**完整复刻 PHP `isBizResponseOk`**：

```java
/**
 * 判断对方业务响应是否成功（对齐 PHP isBizResponseOk）：
 * - errorCode 非空且非 null → 失败
 * - statusCode 存在且 != 200 → 失败
 * - status 存在且不是 "200"/"0" → 失败
 * 三个都不触发 → 成功
 */
private boolean isBizResponseOk(Map<String, Object> result) {
    // 1. errorCode 非空 → 失败
    Object errorCode = result.get("errorCode");
    if (errorCode != null && !errorCode.toString().isEmpty()) {
        return false;
    }
    // 2. statusCode 存在且 != 200 → 失败
    Object statusCode = result.get("statusCode");
    if (statusCode != null && !statusCode.toString().equals("200")) {
        return false;
    }
    // 3. status 存在且不是 "200"/"0" → 失败
    Object status = result.get("status");
    if (status != null) {
        String s = status.toString();
        if (!s.equals("200") && !s.equals("0")) {
            return false;
        }
    }
    return true;
}
```

**实际响应**：真实响应里没有 `errorCode`/`statusCode`/`status`，所以 `isBizResponseOk` 返回 `true` → 走正常分支。✓

### 4.5 状态累积与 isChecked 更新逻辑（审核修订 v1.2 #1）

**原方案缺陷**：用 `anyAbnormal` 取反判断 `isChecked`，导致"一轮正常一轮异常"时 `isChecked` 不更新，与 PHP 行为矛盾。

**修正**：改为 `anyNormal` 正向判断：

```java
CertCheckResult result = new CertCheckResult();  // 累积结果

for (int templateIdx = 0; templateIdx < templates.size(); templateIdx++) {
    ...
    // 每轮处理结果累积到 result
    // result.anyNormal：任一轮"业务正常"（有证或无证）→ true
    // result.anyCertFound：任一轮查到证 → true
    ...
}

// 状态更新：
// isChecked = 1 的条件：只要任一轮正常（PHP 也是"先入为主"，第一轮正常就置 1）
// isChecked 不变的条件：全部轮次都异常
// isCert = 1 的条件：任一轮查到证
if (result.anyNormal) user.setIsChecked(1);
if (result.anyCertFound) user.setIsCert(1);
```

**CertCheckResult 类定义**（§7.5 完整代码）：

```java
class CertCheckResult {
    boolean anyNormal = false;      // 任一轮业务正常（有证或无证）
    boolean anyCertFound = false;   // 任一轮查到证
    List<CollectedCert> certs = new ArrayList<>();  // 累积待写入的证书
}

class CollectedCert {
    String certNo;
    String certName;
    String certDate;
    ChazhengCertEntity toEntity() { ... }
}
```

### 4.6 日志记录

#### 4.6.1 路径与文件安全（审核修订 v1.2 #5 #6）

- **日志目录**：`./logs/chazheng/`（相对于应用工作目录）
- **文件名**：`certs_{safeUserName}_{idCard}.log`
- **特殊字符处理**：userName 中的文件不安全字符（`/ \ : * ? " < > |`）替换为 `_`

```java
String safeUserName = userName.replaceAll("[\\\\/:*?\"<>|]", "_");
Path logFile = Paths.get(logDir, "certs_" + safeUserName + "_" + idCard + ".log");
```

- **目录自动创建**：写日志前确保目录存在

```java
Files.createDirectories(Paths.get(logDir));
```

- **编码**：UTF-8（与 PHP `JSON_UNESCAPED_UNICODE` 对齐）

#### 4.6.2 敏感信息（审核修订 v1.2 #7）

日志中记录了**身份证号**和**姓名**，属于敏感个人信息。应满足：

| 要求 | 说明 |
|---|---|
| 访问控制 | 日志目录设置文件权限（如 `chmod 700`），仅应用运行用户可读 |
| 保留策略 | 建议保留 30 天后自动清理（可用 logrotate 或定时任务） |
| 传输加密 | 如日志需集中收集，应走加密通道（TLS） |

#### 4.6.3 记录内容

```
--- REQUEST 2026-07-13T10:00:00 templateIdx=0 ---
{"lscode":"6","certType":1,"idcode":"452624198404280738","username":"黄俊规"}

--- RESPONSE 2026-07-13T10:00:02 ---
{"parseStatus":"success","bizOk":true,"certCount":1}

--- RESULT 2026-07-13T10:00:02 ---
{"action":"inserted","certNo":"2024-XXX","certName":"起重装卸机械操作工_桥式起重机司机(四级/中级工)","certDate":"20230105"}
```

异常时：

```
--- RESPONSE 2026-07-13T10:00:05 ---
{"parseStatus":"network_error","error":"connect timed out"}
```

---

## 五、整体时序图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                  certCheck (无事务，编排层)                               │
│                                                                         │
│  1. findExistingUser (非事务读)                                         │
│                                                                         │
│  2. 从 sys_dict 读取配置（无事务）                                      │
│     ├─ chazheng_url.value    → apiUrl                                  │
│     └─ chazheng_datas.value → List<Map> templates（JSON 反序列化）      │
│                                                                         │
│  3. 遍历 templates（每轮一次查证，无事务，网络IO不占连接）              │
│     ├─ 3.1 组装请求体：模板字段 + 覆盖 idcode/username                  │
│     ├─ 3.2 HTTP POST 调用第三方接口（不重试，跳过SSL）                   │
│     ├─ 3.3 解析响应 JSON                                                │
│     ├─ 3.4 异常分流                                                     │
│     │   ├─ 网络异常 → 记日志，标记本轮异常，继续下一轮                   │
│     │   ├─ JSON 解析失败 → 记日志，标记本轮异常，继续下一轮             │
│     │   ├─ 业务异常（isBizResponseOk=false）→ 记日志，标记异常，继续    │
│     │   ├─ 业务正常 + 有证 → 写证书+ 标记本轮有证+正常                  │
│     │   └─ 业务正常 + 无证 → 记日志，标记本轮正常                       │
│     └─ 3.5 下一轮（如有 datas[1]）                                     │
│                                                                         │
│  4. chazhengPersistService.persistResult (@Transactional，短事务)       │
│     ├─ 写入新证书（增量，findFirst 判重）                               │
│     ├─ isCert：result.anyCertFound → 1                                  │
│     └─ isChecked：result.anyNormal → 1；全部异常 → 不变               │
│                                                                         │
│  5. 返回最新用户 + 证书列表                                              │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 六、异常分流详解

```
调用第三方接口（无事务）
    │
├─ 网络异常（ResourceAccessException：超时/连接失败）
│   └─ 记录请求 + 异常信息到日志文件
│   └─ 不标记 anyNormal（本轮视为异常）
│   └─ 不写证
│   └─ 继续下一轮模板
│
├─ HTTP 4xx/5xx（HttpStatusCodeException：服务端返回错误状态码）
│   └─ 记录请求 + 状态码 + 错误信息到日志文件
│   └─ 不标记 anyNormal（本轮视为异常）
│   └─ 不写证
│   └─ 继续下一轮模板
│
├─ JSON 解析失败（HttpMessageConversionException：非 JSON 响应）
│   └─ 记录请求 + 原始响应前 500 字符到日志文件
│   └─ 不标记 anyNormal（本轮视为异常）
│   └─ 不写证
│   └─ 继续下一轮模板
│
├─ JSON 解析成功 + 业务异常（isBizResponseOk = false）
│   └─ 记录请求 + 错误码 + 错误信息到日志文件
│   └─ 不标记 anyNormal（本轮视为异常）
│   └─ 不写证
│   └─ 继续下一轮模板
│
├─ JSON 解析成功 + 业务正常 + data 非空
│   └─ 标记 anyNormal = true（本轮正常）
│   └─ 遍历 data 数组
│       ├─ 元素非 Map → 跳过（审核修订 v1.2 #4）
│       ├─ cert_name 拼合
│       ├─ cert_no 为空 → 跳过（避免脏数据）
│       ├─ 增量写入（findFirst user_id + cert_no，不存在才 insert）
│       └─ 标记 anyCertFound = true
│   └─ 继续下一轮模板
│
└─ JSON 解析成功 + 业务正常 + data 为空
    └─ 标记 anyNormal = true（本轮正常）
    └─ 记录"无证书"日志
    └─ 继续下一轮模板

全部轮次结束：
    └─ isCert：result.anyCertFound → 1
    └─ isChecked：result.anyNormal → 1；全部异常 → 不更新（留待重试）
```

---

## 七、模块修复详情

### 7.1 CertCheckConfigService（新增）

**职责**：从 `sys_dict` 表读取查证配置。

```java
@Service
public class CertCheckConfigService {
    private final DictMapper dictMapper;

    /**
     * 查询第三方接口 URL
     * @return 接口地址，未配置返回 null
     */
    public String findApiUrl(Long tenantId) {
        // SELECT value FROM sys_dict WHERE tenant_id=? AND code='chazheng_url' AND deleted=0
    }

    /**
     * 查询请求模板列表（JSON 反序列化）
     * @return 模板 Map 列表，未配置返回空列表
     */
    public List<Map<String, Object>> findRequestTemplates(Long tenantId) {
        // SELECT value FROM sys_dict WHERE tenant_id=? AND code='chazheng_datas' AND deleted=0
        // → JSON 反序列化为 List<Map<String, Object>>
    }
}
```

### 7.2 ChazhengApplicationService（重构 certCheck）

> **依赖注入字段**（代码中引用但未在伪代码中声明，实施时需补充）：
> ```java
> private final CertCheckConfigService certCheckConfigService;
> private final ChazhengPersistService chazhengPersistService;
> private final RestTemplate chazhengRestTemplate;  // 来自 ChazhengRestTemplateConfig
> @Value("${chazheng.log-dir:./logs/chazheng}")
> private String chazhengLogDir;
> ```

#### 重构后结构

```java
// 外层编排：无事务
public ChazhengUserVO certCheck(Integer id) {
    ChazhengUserEntity user = findExistingUser(tenantId, id);

    // 1. 读配置
    String apiUrl = certCheckConfigService.findApiUrl(tenantId);
    List<Map<String, Object>> templates = certCheckConfigService.findRequestTemplates(tenantId);
    if (!StringUtils.hasText(apiUrl)) {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "查证接口未配置（sys_dict: chazheng_url）");
    }

    // 2. 遍历模板（无事务，HTTP 调用）
    CertCheckResult result = new CertCheckResult();
    for (int templateIdx = 0; templateIdx < templates.size(); templateIdx++) {
        if (templates.get(templateIdx) == null) continue;
        Map<String, Object> reqData = new LinkedHashMap<>(templates.get(templateIdx));
        reqData.put("idcode", user.getIdCard());
        reqData.put("username", user.getUserName());
        processOneTemplate(apiUrl, reqData, user, templateIdx, result);
    }

    // 3. DB 写入（移到独立 Bean，解决自调用事务不生效 — 审核修订 v1.2 #2）
    chazhengPersistService.persistResult(user, result);

    return buildUserVO(user);
}

// 单轮处理：无事务
private void processOneTemplate(String apiUrl, Map<String, Object> reqData,
                                ChazhengUserEntity user, int templateIdx,
                                CertCheckResult result) {
    Map<String, Object> response;
    try {
        response = chazhengRestTemplate.postForObject(apiUrl,
            new HttpEntity<>(reqData, jsonHeaders()), Map.class);
    } catch (ResourceAccessException | HttpStatusCodeException | HttpMessageConversionException e) {
        // 审核修订 v1.2 #3+#11：缩小异常范围
        // ResourceAccessException = 网络/超时；HttpStatusCodeException = 4xx/5xx（含 HTTP 500）
        // HttpMessageConversionException = JSON 解析失败
        // 三者共同父类是 RestClientException，但分开 catch 以便区分日志类型
        logRequestResponse(user, reqData,
            e instanceof HttpMessageConversionException ? "no_json_response"
            : e instanceof HttpStatusCodeException ? "http_error_" + ((HttpStatusCodeException) e).getStatusCode().value()
            : "network_error",
            e.getMessage());
        return;  // 不标记 anyNormal，本轮视为异常
    }

    if (response == null) {
        logRequestResponse(user, reqData, "no_json_response", "null response");
        return;
    }

    if (!isBizResponseOk(response)) {
        logRequestResponse(user, reqData, "api_error",
            response.get("errorCode"));
        return;
    }

    // 业务正常：标记本轮正常（审核修订 v1.2 #1：用 anyNormal）
    result.anyNormal = true;

    Object resultObj = response.get("result");
    if (!(resultObj instanceof Map)) {
        logRequestResponse(user, reqData, "no_cert", "missing 'result' or not a Map");
        return;  // result.anyNormal 已标记为 true
    }
    Map<String, Object> resultMap = (Map<String, Object>) resultObj;
    Object dataObj = resultMap.get("data");
    if (!(dataObj instanceof List) || ((List<?>) dataObj).isEmpty()) {
        logRequestResponse(user, reqData, "no_cert", "data is empty");
        return;  // result.anyNormal 已标记为 true
    }

    // 有证：遍历并收集
    List<Map<String, Object>> dataList = (List<Map<String, Object>>) dataObj;
    for (Object certObj : dataList) {
        // 审核修订 v1.2 #4：类型检查
        if (!(certObj instanceof Map)) {
            log.warn("cert item is not a Map, skip: {}", certObj);
            continue;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> cert = (Map<String, Object>) certObj;

        String certNo = getStringOrEmpty(cert, "serialid");
        if (certNo.isEmpty()) continue;  // 缺证书编号，跳过

        String certName = buildCertName(getMapOrEmpty(cert, "metadata"));
        String certDate = cert.containsKey("bzz002") ? String.valueOf(cert.get("bzz002")) : "";

        result.certs.add(new CollectedCert(certNo, certName, certDate));
        result.anyCertFound = true;
    }
}

// 构建 VO（无事务）
private ChazhengUserVO buildUserVO(ChazhengUserEntity user) {
    ChazhengUserVO vo = ChazhengUserVO.from(user);
    List<ChazhengCertEntity> certs = certMapper.selectList(
        new LambdaQueryWrapper<ChazhengCertEntity>()
            .eq(ChazhengCertEntity::getUserId, user.getId()));
    vo.setCerts(certs.stream().map(ChazhengCertVO::from).toList());
    return vo;
}

// 辅助方法（审核修订 v1.2 #10）
private static HttpHeaders jsonHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
}

private static String getStringOrEmpty(Map<String, Object> map, String key) {
    Object val = map.get(key);
    return val == null ? "" : val.toString();
}

private static Map<String, Object> getMapOrEmpty(Map<String, Object> map, String key) {
    Object val = map.get(key);
    if (val instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) val;
        return result;
    }
    return new LinkedHashMap<>();
}
```

### 7.3 ChazhengPersistService（新增 — 审核修订 v1.2 #2）

> `persistResult` 需要 `@Transactional`，但从 `certCheck`（同 Bean 内部方法）调用时 Spring AOP 不拦截。
> 解法：将 DB 写入逻辑拆到独立 Bean，通过注入调用。

```java
@Service
public class ChazhengPersistService {
    private final ChazhengUserMapper userMapper;
    private final ChazhengCertMapper certMapper;

    @Transactional
    public void persistResult(ChazhengUserEntity user, CertCheckResult result) {
        for (CollectedCert cert : result.certs) {
            // 审核修订 v1.2 #14：exists+insert 有竞态，依赖 DB 唯一索引兜底
            // chazheng_cert 表应有 UNIQUE(user_id, cert_no) 索引，并发插入时第二个抛 DuplicateKeyException
            boolean exists = certMapper.exists(new LambdaQueryWrapper<ChazhengCertEntity>()
                .eq(ChazhengCertEntity::getUserId, user.getId())
                .eq(ChazhengCertEntity::getCertNo, cert.certNo));
            if (!exists) {
                // 审核修订 v1.2 #13：不用双括号初始化（匿名子类 MyBatis-Plus 可能不识别）
                ChazhengCertEntity certEntity = new ChazhengCertEntity();
                certEntity.setUserId(user.getId());
                certEntity.setTenantId(user.getTenantId());
                certEntity.setCertNo(cert.certNo);
                certEntity.setCertName(cert.certName);
                certEntity.setCertDate(cert.certDate);
                try {
                    certMapper.insert(certEntity);
                } catch (DuplicateKeyException dup) {
                    // 并发插入兜底：另一请求已插入，跳过即可
                }
            }
        }
        if (result.anyCertFound) user.setIsCert(1);
        if (result.anyNormal) user.setIsChecked(1);  // 审核修订 v1.2 #1
        userMapper.updateById(user);
    }
}
```

### 7.4 cert_name 拼合实现

```java
private String buildCertName(Map<String, Object> metadata) {
    String zymc   = getStringOrEmpty(metadata, "zymc");
    String gzmcRaw = getStringOrEmpty(metadata, "gzmc");
    String zyjndj  = getStringOrEmpty(metadata, "zyjndj");

    // 规范化 gzmc：纯破折号视为空
    String gzmc = gzmcRaw;
    if (!gzmc.isEmpty() && gzmc.matches("^-+$")) {
        gzmc = "";
    }

    String base;
    if (zymc.isEmpty() && gzmc.isEmpty()) {
        base = "";
    } else if (gzmc.isEmpty()) {
        base = zymc;
    } else if (zymc.isEmpty()) {
        base = gzmc;
    } else {
        base = zymc + "_" + gzmc;
    }

    String certName;
    if (!base.isEmpty() && !zyjndj.isEmpty()) {
        certName = base + "(" + zyjndj + ")";
    } else if (!base.isEmpty()) {
        certName = base;
    } else if (!zyjndj.isEmpty()) {
        certName = zyjndj;
    } else {
        certName = "";
    }

    return certName;
}
```

### 7.5 ChazhengController（无改动）

Controller 方法签名不变，仅 Service 内部逻辑重构。

### 7.6 CertCheckResult / CollectedCert 类定义（审核修订 v1.2 #9）

```java
package com.guoyu.platform.chazheng.service;

import java.util.ArrayList;
import java.util.List;

/**
 * 单次查证方法的累积结果（跨多轮模板）
 */
class CertCheckResult {
    /** 任一轮业务正常（有证或无证都算正常） */
    boolean anyNormal = false;
    /** 任一轮查到证 */
    boolean anyCertFound = false;
    /** 累积待写入的证书（已去重前的原始列表） */
    List<CollectedCert> certs = new ArrayList<>();
}

/**
 * 从接口单条证响应中提取的待写入数据
 */
class CollectedCert {
    String certNo;
    String certName;
    String certDate;

    CollectedCert(String certNo, String certName, String certDate) {
        this.certNo = certNo;
        this.certName = certName;
        this.certDate = certDate;
    }
}
```

### 7.7 辅助方法签名（审核修订 v1.2 #10）

```java
/**
 * 构建 JSON 请求头
 */
private static HttpHeaders jsonHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
}

/**
 * 安全取字符串，null 返回空字符串
 */
private static String getStringOrEmpty(Map<String, Object> map, String key) {
    Object val = map.get(key);
    return val == null ? "" : val.toString();
}

/**
 * 安全取 Map，非 Map 或 null 返回空 Map
 */
private static Map<String, Object> getMapOrEmpty(Map<String, Object> map, String key) {
    Object val = map.get(key);
    if (val instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) val;
        return result;
    }
    return new LinkedHashMap<>();
}

/**
 * 记录请求/响应日志到文件
 * @param user 当前用户
 * @param reqData 请求体
 * @param parseStatus 解析状态：success|no_json_response|api_error|network_error|no_cert
 * @param detail 详细信息
 */
private void logRequestResponse(ChazhengUserEntity user, Map<String, Object> reqData,
                                 String parseStatus, Object detail) {
    // 1. 安全文件名
    String safeUserName = user.getUserName().replaceAll("[\\\\/:*?\"<>|]", "_");
    Path logDir = Paths.get(this.chazhengLogDir);
    try {
        Files.createDirectories(logDir);  // 自动创建目录（审核修订 v1.2 #6）
    } catch (IOException e) {
        log.warn("无法创建日志目录: {}", logDir);
        return;
    }
    Path logFile = logDir.resolve("certs_" + safeUserName + "_" + user.getIdCard() + ".log");

    // 2. 追加写入，格式：--- REQUEST/RESPONSE/RESULT {timestamp} ---
    String entry = String.format("--- %s %s ---%n%s%n",
        parseStatus, Instant.now(),
        JSON.toJSONString(Map.of("user", user.getIdCard(), "req", reqData, "detail", detail),
            SerializerFeature.WriteMapNullValue));
    try {
        Files.writeString(logFile, entry,
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    } catch (IOException e) {
        log.warn("写日志失败: {}", logFile, e);
    }
}
```

---

## 八、新增测试用例

### 8.1 响应解析测试

| 场景 | 输入 | 期望行为 |
|---|---|---|
| 正常单证书 | `result.data[0]={serialid: "X", bzz002: 20230105, metadata: {zymc: "电工", gzmc: "--", zyjndj: "四级"}}` | cert_name="电工(四级)" |
| 正常多证书 | `result.data` 含 2 条 | 两条都写入 |
| cert_name 全空 | `metadata={}` | cert_name="" → 跳过（如果 cert_no 也为空） |
| 无 metadata | `cert={serialid: "X"}`（无 metadata 层） | cert_name 兜底 azf040（若有），否则为空 |
| gzmc 为纯破折号 | `gzmc="----"` | 规范化为空，cert_name 用 zymc(zyjndj) |
| cert_no 为空 | `serialid=""` | 跳过该条 |
| data 含非 Map 元素 | `data: [{serialid: "X"}, "invalid", 123]` | 跳过非 Map 元素，正常处理 Map 元素 |

### 8.2 异常分流测试

| 场景 | 输入 | 期望行为 |
|---|---|---|
| HTTP 超时 | ResourceAccessException | 记日志，不更新 isChecked |
| API 返回 500 | HttpServerErrorException（HttpStatusCodeException 子类） | catch 捕获，记日志，不更新 isChecked |
| API 返回 404 | HttpClientErrorException（HttpStatusCodeException 子类） | catch 捕获，记日志，不更新 isChecked |
| JSON 非法（HTML 响应） | HttpMessageConversionException | 记录 raw 前 500 字符，不更新 isChecked |
| errorCode 非空 | `{errorCode: "E001"}` | 记录 code + msg，不更新 isChecked |
| statusCode=500 | `{statusCode: 500}` | 判失败，不更新 isChecked |
| status=999 | `{status: 999}` | 判失败，不更新 isChecked |
| BusinessException 不捕获 | 业务异常 | 只 catch RestTemplate 异常，BusinessException 向上传播 |

### 8.3 状态更新测试（审核修订 v1.2 #1）

> isChecked 逻辑：`result.anyNormal` 为 true → 1；全部异常 → 不变
> isCert 逻辑：`result.anyCertFound` 为 true → 1

| 场景 | 轮次1 | 轮次2 | 最终 isChecked | 最终 isCert |
|---|---|---|---|---|
| 全部正常有证 | 有证 | 有证 | 1 (anyNormal=true) | 1 (anyCertFound=true) |
| 部分异常 | 异常 | 有证 | 1 (anyNormal=true) | 1 (anyCertFound=true) |
| 部分无证 | 无证 | 有证 | 1 (anyNormal=true) | 1 (anyCertFound=true) |
| 全部无证 | 无证 | 无证 | 1 (anyNormal=true) | 不变 (anyCertFound=false) |
| 全部异常 | 异常 | 异常 | 不变 (anyNormal=false) | 不变 |
| 一轮正常一轮异常 | 正常无证 | 异常 | **1** (anyNormal=true) | 不变 |

> ⚠️ 全部异常场景下，isChecked 不变是为了让下一次查证可以重试——与 PHP 完全一致

### 8.4 去重测试

| 场景 | 输入 | 期望行为 |
|---|---|---|
| 首次查证 | 用户无旧证 | 全量插入 |
| 重复查证 | 已有 cert_no 相同记录 | 跳过（不重复插入） |
| 部分新增 | 已有 A 证，新查到 A+B 证 | A 跳过，B 新增 |

### 8.5 模板轮询测试

| 场景 | 输入 | 期望行为 |
|---|---|---|
| 单模板 | `chazheng_datas = [{...}]` | 只查一轮 |
| 双模板 | `chazheng_datas = [{...职业技能...}, {...特种作业...}]` | 查两轮，可能同一用户有两类证书 |
| 模板字段覆盖 | 模板填了 `lscode: "6"` | 保留模板值，只覆盖 idcode/username |

### 8.6 事务边界测试（审核新增）

| 场景 | 期望行为 |
|---|---|
| 第三方接口 10s 超时 | DB 连接占用 <100ms（HTTP 在事务外） |
| 并发 20 个用户查证 | 不耗尽连接池（HTTP 不占连接） |

---

## 九、配置项清单

| 配置位置 | key | 说明 | 当前值 |
|---|---|---|---|
| `sys_dict.value` | `chazheng_url` | 第三方接口地址 | `https://www.gxrcosta.com/ecopf/myInfos.do` |
| `sys_dict.value` | `chazheng_datas` | 请求模板 JSON 数组 | `[{"idcode":"","lscode":"6","certType":1,"username":""}]` |
| `application.yml` | `chazheng.rest.connect-timeout` | HTTP 连接超时 | 5000ms |
| `application.yml` | `chazheng.rest.read-timeout` | HTTP 读取超时 | 10000ms |
| `application.yml` | `chazheng.log-dir` | 日志目录 | `./logs/chazheng` |

---

## 十、新增/修改文件清单

| 文件 | 操作 | 改动 |
|---|---|---|
| `CertCheckConfigService.java` | **新增** | 从 sys_dict 读查证配置 |
| `ChazhengApplicationService.java` | **修改** | 重构 certCheck：模板轮询 + 正确字段映射 + 异常分流 + 增量去重 + anyNormal 状态累积 |
| `ChazhengPersistService.java` | **新增** | DB 写入短事务（独立 Bean 解决自调用事务失效） |
| `ChazhengRestTemplateConfig.java` | **新增** | 配置跳过 SSL + 超时的 RestTemplate Bean |
| `docs/04_requirements/查证功能Java版修复方案.md` | **本文件** | 方案文档 |

---

## 十一、验证清单

修复完成后，确认以下场景：

**基础功能**
- [ ] 新增字典配置 `chazheng_url` 和 `chazheng_datas` 后能正常查证
- [ ] 查询结果 cert_no / cert_name / cert_date 全部非空
- [ ] cert_name 格式为 "专业_工种(等级)" 或对应兜底格式（含 gzmc 破折号规范化）
- [ ] 网络异常时不更新 isChecked，可手动重试
- [ ] 同一用户重复查证时不产生重复证书记录
- [ ] 双模板场景下同一用户可查到多本不同类型证书

**状态逻辑（审核修订 v1.2）**
- [ ] "一轮正常一轮异常"场景下 isChecked=1（anyNormal=true）
- [ ] "全部异常"场景下 isChecked 不变（留待重试）
- [ ] persistResult 在事务内执行（独立 Bean 事务生效）
- [ ] catch 不捕获 BusinessException（仅捕获 RestClientException 体系）
- [ ] HTTP 4xx/5xx 被 HttpStatusCodeException 捕获（不崩溃）
- [ ] data 数组中非 Map 元素被跳过（不抛 ClassCastException）

**性能与安全**
- [ ] 查证日志文件记录了完整请求/响应
- [ ] 第三方接口 10s 超时时，DB 连接池未耗尽（事务边界正确）
- [ ] isBizResponseOk 正确判断 errorCode/statusCode/status 三重条件
- [ ] https 接口能连通（SSL 跳过生效）
- [ ] 日志文件名中特殊字符被替换（无文件创建失败）
- [ ] 日志目录自动创建（不存在时不出错）
- [ ] 敏感信息（身份证号）有访问控制和保留策略
- [ ] chazheng_cert 表有唯一索引（user_id + cert_no）兜底并发插入

---

*方案版本：v1.2（审核修订）| 日期：2026-07-13 | 状态：待用户确认后实施*
