# FAQ 转换 LLM 调用性能分析报告

> 日期：2026-07-17（分析） / 2026-07-19（方案定稿 + 修订）
> 状态：✅ 方案已修订（基于代码核验 + 4 项决策确认），待实施
> 
> **修订记录（2026-07-19）**：
> - 并发数 10 → 12（留 20% 余量），线程池 core=12/max=24
> - 退避策略改为"释放许可 → sleep → 重新获取"，消除持许可 sleep 导致的批次停滞风险
> - 新增熔断器：连续 5 条 429 → 暂停批次并发告警
> - 方案四从"辅助"提升为"核心"（代码核验确认无连接池）
> - 补充异常分类体系（4 类新异常）
> - 补充孤立 pending 记录恢复策略
> - 修正文档错误：读超时实际为 70s（非 50s），计数器已是 SQL 原子操作（无需改造）
> - 明确取消功能不在本次范围

---

## 1. 问题描述

FAQ 转换接口（`ExamFaqConversionController`）调用 LLM 服务执行非常慢，24 小时仅成功约 600 条记录。

## 2. 调用链路

```
前端提交批次
  → ExamFaqConversionController.submit()
    → FaqConversionApplicationService.submitConversion()
      → 创建批次 + 明细记录（同步，秒回 batchId）
      → CompletableFuture.runAsync( → faqConversionExecutor 线程池 )
        → processLlmEnhancementAsync()  ← Worker 线程
          → for 循环逐条处理：
            → processSingleLlmEnhancement()
              → 第 1 次 LLM 调用：生成 similar_questions
              → 第 2 次 LLM 调用：生成 negative_questions
```

## 3. 关键代码位置

| 组件 | 文件 | 说明 |
|------|------|------|
| Controller | `exam/controller/ExamFaqConversionController.java` | REST 入口 |
| 异步处理 | `exam/service/FaqConversionApplicationService.java:185-214` | 单线程 for 循环 |
| 单条处理 | `exam/service/FaqConversionApplicationService.java:469-547` | 2 次串行 LLM 调用 |
| LLM 客户端 | `ai/infrastructure/FaqLlmClient.java` | HTTP 调用，读超时 **70s**；每次 new HttpClient，无连接池 |
| 线程池配置 | `config/AsyncConfig.java:24-35` | core=3, max=6, queue=500 |

## 4. 性能瓶颈分析

### 4.1 批次内单线程串行

`processLlmEnhancementAsync()` 使用 **单线程 for 循环** 逐条处理记录。一个批次内无论有多少条数据，都是一条一条串行执行的。

```
记录1: LLM调用 → LLM调用 → 写库
记录2: LLM调用 → LLM调用 → 写库     ← 等记录1完成才开始
记录3: LLM调用 → LLM调用 → 写库     ← 等记录2完成才开始
...
```

### 4.2 每条记录 2 次串行 LLM 调用

`processSingleLlmEnhancement()` 中：
1. 先调 LLM 生成 `similar_questions`（等待返回）
2. 再调 LLM 生成 `negative_questions`（等待返回）

两次调用是串行的，不能并行。

### 4.3 LLM API 本身耗时

- `FaqLlmClient` 读超时当前设置为 **70 秒**（代码核验：`FaqLlmClient.java:77` `setReadTimeout(Duration.ofSeconds(70))`）
- 公有云 LLM API（LongCat）生成 JSON 数组实测 10~50 秒
- 如果 prompt 较长或 maxTokens 较大，耗时更久
- **额外开销**：当前每次调用 new HttpClient（`FaqLlmClient.java:73-80`），无连接池，10 并发下 TLS 握手 + TCP 端口压力显著

### 4.4 吞吐量计算

| 项目 | 值 |
|------|---|
| 每条记录耗时 | 2 次 LLM × ~60s ≈ **120 秒**（单次实测 10~50s，均值约 30s） |
| 每小时处理 | 3600 / 120 ≈ **30 条** |
| 24 小时处理 | 30 × 24 = **720 条** |

与实际观测值（24h / 600 条）基本吻合（实际因网络抖动、超时重试略低）。

### 4.5 线程池未充分利用

- 线程池配置：core=3, max=6
- 但这是 **批次间** 并行——只有同时提交多个批次才会用到多线程
- 如果只跑一个批次，无论线程池多大，都只有 1 个线程在工作

### 4.6 批次实体上的 `concurrencyLimit` 未被使用

批次表有 `concurrency_limit` 字段（默认 5），但当前代码 **完全没有读取或使用** 这个字段。批次内并发控制能力缺失。

## 5. 根因总结

**不是代码 bug，而是架构设计问题：**

1. 批次内单线程串行 → 无法并行处理多条记录
2. 每条记录 2 次串行 LLM 调用 → 耗时翻倍
3. LLM API 响应时间 30~70 秒 → 单次调用就慢
4. 三者叠加 → 吞吐量被锁死在 ~25 条/小时

## 6. 待确认信息

以下信息对制定优化方案至关重要，请补充：

### 6.1 LLM 服务商

调用的是哪家 LLM？（DeepSeek / LongCat / 其他？）
不同服务商的响应速度和并发限制差异很大。
答：“LongCat”

### 6.2 maxTokens 配置

模型配置表中 `max_tokens` 当前设置的值是多少？
这直接影响 LLM 生成时间。
答：“原本设置的值是4000，已经改成2000”

### 6.3 并发批次数量

生产环境中通常同时跑几个批次？还是一次只跑一个？
答：“只跑一个批次”

### 6.4 单次 LLM 调用实际耗时

日志中 `FAQ LLM 请求` 和 `FAQ LLM 响应` 两条日志的时间差是多少秒？
这能确认 LLM API 的真实响应时间。
答：“10-50秒”

### 6.5 预期的处理速度目标

希望达到什么样的处理速度？（例如：1 小时处理 1000 条？）
这决定优化方案的目标。
答：“20分钟处理300条”

### 6.6 补充确认（2026-07-19）

| 问题 | 答复 | 对方案的影响 |
|------|------|-------------|
| LongCat RPM 限制 | **官方无 RPM 限制**；响应速度取决于请求长度和复杂度、服务器负载和地理位置、是否使用流式响应 | 并发上限不受服务商约束，可按吞吐目标自由设定线程数；瓶颈只在本机线程池与单次调用耗时 |
| 是否接受合并 similar + negative 为一次调用 | **接受合并，先抽样验证** | 采用「一次调用生成 3 相似 + 3 反例」方案；上线前抽 20 条对比质量 |
| 单批次中途失败（429/5xx）策略 | **指数退避后继续** | 单条按 2s→4s→8s 退避重试 3 次，仍失败标 `failed`，批次不中断 |

## 7. 优化方案（已定稿）

### 7.1 目标吞吐测算

| 项 | 值 |
|---|---|
| 目标 | **20 分钟处理 300 条 = 900 条/小时 = 0.25 条/秒** |
| 合并后单次 LLM 调用耗时 | 10~50s（实测区间），按保守均值 **40s** 估算（输出含 6 条问题，比原来单次略长） |
| 所需并发数 | 40s × 0.25 条/秒 = **10 并发** |
| 结论 | 合并调用（调用次数减半）+ 批次内并发 **12**（留 20% 余量），达标且有冗余 |

### 7.2 优化组合（两项必须同时做，缺一不可）

```
现状:  2次串行调用 × 单线程       = 60s+/条  → ~30条/小时
只做合并: 1次调用 × 单线程        = 40s/条   → 90条/小时   (3倍, 不够)
只做并行: 2次串行 × 12并发        = 5s/条    → 720条/小时  (24倍, 不够)
合并+并行: 1次调用 × 12并发       = 3.3s/条  → 1080条/小时 (36倍, 达标 ✓ 有余量)
```

### 7.3 方案一：合并 LLM 调用（similar + negative 一次生成）

将原来两次独立调用合并为一次，prompt 共享题目上下文，一次返回完整 JSON。

**合并后 Prompt（在原 prompt 基础上合并，system prompt 不变）：**

```text
## 题目上下文
- 工种：{work_type_name} ({work_type_series_no})
- 职业等级：{work_type_level}
- 科目：{subject_title}
- 鉴定范围：{knowledge_path}
- 题型：{题型}
- 难度：{难度}

## 题目信息
- 题型：{single|multi|judge}
- 题干：{content_stripped}
- 正确选项：{correct_options}
- 干扰项：{wrong_options}
- 题目考查的关键概念：{key_concepts_from_keyword}

## 生成要求
一次性生成 3 条「相似问」和 3 条「反例」。

### 相似问要求
1. 与原题答案一致，语义等价
2. 使用国家职业标准中的规范术语，避免口语化
3. 保留题干核心概念（名词、限定词）不变
4. 通过替换疑问词、同义改写、调整句式生成多样性问法
5. 不生成需要额外推理或引入新知识才能回答的问法

### 反例要求
1. 与原题「看起来相似但答案不同」
2. 每条使用不同策略：属性替换 / 实体替换 / 等级混淆 / 工段场景替换 / 答非所问
3. 每条反例后用括号标注「应匹配的正确答案」，格式：（答案应为 XXX）
4. 体现国家职业标准中容易混淆的邻近概念

## 输出格式
{
  "similar_questions": ["问法1", "问法2", "问法3"],
  "negative_questions": ["反例1（答案应为XXX）", "反例2（答案应为XXX）", "反例3（答案应为XXX）"]
}
```

**参数调整：**

| 参数 | 原值 | 新值 | 理由 |
|------|------|------|------|
| `max_tokens` | 2000 | **2000（不变）** | 6 条问题 × 约 60 token + JSON 开销 ≈ 500 token，余量充足 |
| read timeout | 70s | **90s** | 合并后输出变长（6 条问题），实测单次最长 50s，留余量；仅修改 `FaqLlmClient` 一处即可覆盖所有调用 |
| `response_format` | `{"type": "json_object"}` | 不变 | LongCat OpenAI 兼容端点支持 |

> **注意**（AGENTS.md 踩坑 #13）：合并前需确认 LongCat 支持 `response_format` 字段。已知 **LongCat 不支持 `response_format`**，因此该字段一律不传，改为在 prompt 中强制约束输出 JSON 格式，解析端做容错（提取第一个 `{` 到最后一个 `}`）。

**部分结果处理策略：**

合并调用可能产生部分字段为空的情况（如 LLM 只生成了 similar_questions 但 negative_questions 为空数组）。处理规则：
- **两个字段均非空** → 正常入库，`conversion_status='success'`
- **任一字段为空数组** → 不视为成功，整条按失败重试（空字段说明 LLM 未完整响应，质量不可靠）
- **两个字段均为空** → 判定为 JSON 解析失败或 LLM 完全未生成，按失败重试

**JSON 解析容错策略：**

`FaqLlmResponseParser` 已有二阶 fallback（`indexOf('{')` → `lastIndexOf('}')`），合并后继续使用此策略。已知风险：题干内容若含 `{` 或 `}`（数学/化学题目），可能命中错误边界。处理方式：**解析失败 = 重试该条**，不引入更激进的修复策略（保持 Simple is Better）。

**抽样验证计划（上线前必做）：**

1. 从 single / multi / judge 三种题型各抽 7 条，共 **21 条**样本
2. 分别用「合并调用」和「原两次调用」生成，人工对比质量
3. 验收标准：合并版可用率 ≥ 90%（两个字段均非空且无需人工改写即可入库）
4. 若质量不达标 → 回退到「两次调用并行」模式（用 `CompletableFuture` 并行发两个请求，收益折半但仍可配合并发 12 达到 450 条/小时，此时需将并发上调至 **25** 才能达标，`faqRecordExecutor` max 需 ≥ 25，需重测）

### 7.4 方案二：批次内并行化（concurrencyLimit 生效）

改造 `processLlmEnhancementAsync()`：单线程 for 循环 → 信号量限流的并行执行。

**⚠️ 现状修正**：当前代码 `processLlmEnhancementAsync()` **未设置 `TenantContextHolder`**（代码核验确认），导致 MyBatis-Plus 多租户拦截器在 Worker 线程中无法自动拼 `tenant_id`。这是 bug，方案二必须修复。

```java
private void processLlmEnhancementAsync(Long batchId, Long tenantId,
                                        Long userId, int concurrencyLimit) {
    // ⚠️ 修复缺失：Worker 线程必须手动设置 TenantContextHolder
    TenantContextHolder.set(tenantId);
    try {
        List<ExamFaqConversionEntity> pending = loadPendingRecords(batchId, tenantId);
        Semaphore semaphore = new Semaphore(concurrencyLimit);
        // 分块提交，避免一次性创建全部 Future（批次规模增长时防 OOM）
        int chunkSize = 50;
        for (int offset = 0; offset < pending.size(); offset += chunkSize) {
            // 检查批次是否被暂停
            if (!batchService.isRunning(tenantId, batchId)) {
                log.info("批次已暂停，停止提交新任务：batchId={}, offset={}", batchId, offset);
                break;
            }
            List<ExamFaqConversionEntity> chunk = pending.subList(offset, Math.min(offset + chunkSize, pending.size()));
            List<CompletableFuture<Void>> futures = chunk.stream()
                .map(record -> CompletableFuture.runAsync(() -> {
                    try {
                        semaphore.acquire();
                        processSingleRecordMerged(record, tenantId);  // 合并调用 + 退避重试
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        semaphore.release();
                    }
                }, faqRecordExecutor))
                .toList();
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }
        refreshBatchCounters(batchId, tenantId);
    } finally {
        TenantContextHolder.clear();
    }
}
```

**线程池配置调整（`AsyncConfig`）：**

| 配置 | 原值 | 新值 | 说明 |
|------|------|------|------|
| `faqConversionExecutor` | core=3, max=6, queue=500 | **core=2, max=4, queue=50** | 批次编排池。生产只跑 1 个批次，`allOf().join()` 阻塞 1 线程 ~20min，core=2 留 1 线程给 resume/retry 等操作 |
| 新增 `faqRecordExecutor` | — | **core=12, max=24, queue=2000** | 批次内记录级并行池。core=12 对应默认并发（留 20% 余量）；max=24 容纳重试 sleep 线程 + 回退场景（需 25 并发时够用） |
| 拒绝策略 | 需确认 | **AbortPolicy** | AGENTS.md 踩坑 #12：禁止 CallerRunsPolicy，满了抛异常由全局处理器返回 503 |
| `concurrency_limit` 字段 | 未被使用 | **生效** | 批次创建时读取（默认 **12**，上限 **24**），Semaphore 以此初始化 |

> **max > core 的设计意图**：`faqRecordExecutor` max=24 但 semaphore 默认=12。多出的 12 个线程用于容纳**退避 sleep 中的重试线程**（sleep 时释放 semaphore 许可，线程本身仍占用线程池槽位）。实际并发数始终由 semaphore 控制，max 只决定"最多允许多少个线程同时存在于池中"。

**进度统计的线程安全（现状确认）：**

代码核验确认：`ExamFaqConversionBatchMapper` 的 `incrementSuccess/Failed/Skipped/Pending` **已是 SQL 级别原子操作**（`SET success_count = success_count + 1`），当前无线程安全问题，**无需改造**。

**多租户与异步陷阱（AGENTS.md 强制约束）：**
- `tenantId` / `userId` 在 HTTP 线程捕获后通过参数传入，`faqRecordExecutor` Worker 线程内**全链路禁止调 `SecurityContext`**
- `processLlmEnhancementAsync()` 入口处手动 `TenantContextHolder.set(tenantId)`，`finally` 中 `TenantContextHolder.clear()`（当前代码缺失，必须修复）
- `loadPendingRecords` 通过 Mapper 查询时，MyBatis-Plus 多租户拦截器依赖 `TenantContextHolder` 自动拼 `tenant_id`

### 7.5 方案三：指数退避重试 + 熔断器（429/5xx）

**异常分类体系（新增）：**

当前 `FaqLlmClient` 统一抛出 `BusinessException(ErrorCode.BAD_GATEWAY)`，无法区分 429/5xx/超时。方案三必须先建立异常分类：

| 新异常类 | 触发条件 | 是否可重试 |
|---------|---------|-----------|
| `FaqLlmRateLimitException` | HTTP 429 | ✅ 是 |
| `FaqLlmServerException` | HTTP 5xx | ✅ 是 |
| `FaqLlmTimeoutException` | SocketTimeoutException | ⚠️ 最多重试 1 次（超时通常是全局问题，重试成功率低；避免单条阻塞 semaphore 许可过久） |
| `FaqLlmClientException` | HTTP 4xx（非 429）、JSON 解析失败 | ❌ 否，直接标 failed |

> `FaqLlmClient` 改造：根据 `RestClientResponseException.getStatusCode()` 抛出对应子类，替代当前统一 `BusinessException`。

**退避策略（释放许可 → sleep → 重新获取）：**

```java
private String callLlmWithRetry(FaqLlmRequest req, Semaphore semaphore, Long tenantId) {
    long[] backoffMs = {2000, 4000, 8000};
    for (int attempt = 0; attempt <= backoffMs.length; attempt++) {
        try {
            return faqLlmClient.chat(req);
        } catch (FaqLlmRateLimitException | FaqLlmServerException e) {
            if (attempt == backoffMs.length) throw e;
            // ⚠️ 释放许可 → sleep → 重新获取（防止持许可 sleep 导致全批次停滞）
            semaphore.release();
            try { Thread.sleep(backoffMs[attempt]); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); throw e; }
            semaphore.acquire();  // 重新获取许可（可能被其他线程抢占，自然的竞争公平性）
        } catch (FaqLlmTimeoutException e) {
            // 超时只重试 1 次
            if (attempt >= 1) throw e;
            semaphore.release();
            try { Thread.sleep(2000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); throw e; }
            semaphore.acquire();
        }
        // FaqLlmClientException（4xx / JSON 解析失败）不重试，直接抛
    }
    throw new IllegalStateException("不应到达此处");  // 编译兜底
}
```

**熔断器（Circuit Breaker）：**

连续 **5 条**记录返回 429（不限具体记录，全局计数），说明 LLM 可能出现服务降级或区域性故障，此时继续重试只会空耗资源：

```
连续 429 计数 ≥ 5 → 触发熔断：
  1. 暂停当前批次（status='paused'，写 error_message='LLM 服务限流熔断，已暂停，请稍后恢复'）
  2. 释放所有已持有的 semaphore 许可
  3. 记录 ERROR 日志（含连续 429 的时间窗口和总处理数）
  4. 前端轮询发现 paused 状态后展示告警，管理员确认 LLM 恢复后手动恢复批次
  5. 恢复后从 pending 记录续跑（已有 resumeBatch 机制可用）
```

**单条失败处理：**

| 规则 | 说明 |
|------|------|
| 单条最终失败 | 标 `conversion_status='failed'` + `error_message`，**批次不中断** |
| JSON 解析失败 | 归类 `FaqLlmClientException`，不重试直接标 failed（题干含 `{` `}` 等边界情况，重试大概率同样失败） |

### 7.6 方案四：HTTP 连接池复用（**核心，非辅助**）

代码核验确认：`FaqLlmClient` **每次调用都 new `HttpClient` + `RestClient`**（line 73-80），无连接池、无 keep-alive。10+ 并发下问题严重：

- **TLS 握手**：每次调用 ~200-500ms 额外开销
- **TCP 端口压力**：大量 socket 快速进入 TIME_WAIT，高并发下可能端口耗尽
- **无连接复用**：同一 LLM 端点每次重新建连

| 配置 | 值 |
|------|---|
| 实现 | `FaqLlmClient` 改为注入单例 `HttpClient`（`@Bean` 或构造函数内一次性创建），配置 `PoolingHttpClientConnectionManager` |
| `maxTotal` | 48（覆盖 `faqRecordExecutor` max=24 的 2 倍，留余量） |
| `defaultMaxPerRoute` | 24（= 记录池 max，确保并发线程不受连接池限制） |
| keepAlive | 60s |
| `connectTimeout` | 70s（不变） |
| `readTimeout` | 90s（方案一同步调整） |
| 收益 | 每次调用省 1 次 TLS 握手 + TCP 端口复用；10 并发持续运行时避免端口耗尽 |

**改造方式**：`FaqLlmClient` 不再在 `chatCompletion()` 方法内 new HttpClient，改为类级别单例：

```java
// 类级别：应用生命周期内只创建一次
private final RestClient client;

public FaqLlmClient(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
    connectionManager.setMaxTotal(48);
    connectionManager.setDefaultMaxPerRoute(24);
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(70))
        .connectionManager(connectionManager)  // JDK HttpClient 不直接支持，需用 HttpClient 5 或 OkHttp
        .build();
    // 实际实现：推荐用 Apache HttpClient 5 的 PoolingHttpClientConnectionManager + Spring 6.1+ 的 HttpComponentsClientHttpRequestFactory
}
```

### 7.7 优化后吞吐量验证

| 场景 | 单次耗时 | 并发 | 吞吐 | 是否达标 |
|------|---------|------|------|---------|
| 悲观（50s/次） | 50s | 12 | **864 条/小时 → 20分钟 288 条** | ✅ 接近目标，上调至 14 即可 |
| 均值（40s/次） | 40s | 12 | **1080 条/小时 → 20分钟 360 条** | ✅ 达标且有盈余 |
| 乐观（20s/次） | 20s | 12 | 2160 条/小时 → 20分钟 720 条 | ✅ 大幅富余 |

**上线后调节手段**：`exam_faq_conversion_batch.concurrency_limit` 为运行参数，无需改代码即可按实测 LLM 耗时下拉/上调。

## 8. 实施清单

| # | 改动点 | 文件 | 类型 |
|---|--------|------|------|
| 1 | 合并 prompt，删除两次调用逻辑，改单次调用解析两个数组；部分字段为空时整条重试 | `FaqLlmPromptBuilder` / `FaqConversionApplicationService.processSingleLlmEnhancement()` | 核心 |
| 2 | 移除 `response_format` 请求字段（已确认当前代码未传，仅确认） | `FaqLlmClient` | 确认（踩坑 #13） |
| 3 | 新增异常分类体系：`FaqLlmRateLimitException` / `FaqLlmServerException` / `FaqLlmTimeoutException` / `FaqLlmClientException` | `ai/infrastructure/` 新增 4 个异常类 | 核心 |
| 4 | `FaqLlmClient` 根据 HTTP status code 抛出对应异常子类 | `FaqLlmClient` | 核心 |
| 5 | 批次内并行：分块提交 + Semaphore + `faqRecordExecutor` + 熔断器 | `FaqConversionApplicationService.processLlmEnhancementAsync()` | 核心 |
| 6 | 新增 `faqRecordExecutor` Bean（core=12/max=24/AbortPolicy）；调整 `faqConversionExecutor`（core=2/max=4） | `AsyncConfig` | 配置 |
| 7 | `concurrency_limit` 从批次表读取并生效（默认 **12**，上限 **24**） | `FaqConversionBatchService` / `FaqConversionApplicationService` | 核心 |
| 8 | 指数退避重试（2s/4s/8s，释放许可 → sleep → 重新获取；超时只重试 1 次） | `FaqConversionApplicationService` | 核心 |
| 9 | 熔断器：连续 5 条 429 → 暂停批次 + 释放许可 + ERROR 日志 | `FaqConversionApplicationService` | 核心 |
| 10 | Worker 线程 TenantContextHolder 设置/清除（**修复当前缺失 bug**） | `processLlmEnhancementAsync()` | 强制约束 |
| 11 | read timeout 70s → 90s | `FaqLlmClient` | 配置 |
| 12 | HttpClient 连接池化（maxTotal=48, perRoute=24, keepAlive=60s） | `FaqLlmClient` 改造为单例 HttpClient | **核心** |
| 13 | 21 条样本 A/B 质量验证（合并 vs 原两次，含部分结果判定标准） | 测试任务 | 验收门槛 |
| 14 | 应用启动时扫描孤立 pending 记录（可选，见 §10） | 新增 `FaqConversionRecoveryService` | 运维保障 |

## 9. 回滚预案

| 情况 | 回滚动作 |
|------|---------|
| 抽样验证质量不达标（可用率 < 90%） | 切回两次调用 + `CompletableFuture` 并行，并发调至 **25**（`faqRecordExecutor` max=24 已预留，需确认） |
| 上线后 LLM 频繁 429 | 熔断器自动暂停批次；恢复后 `concurrency_limit` 下调至 6，退避间隔翻倍（4s/8s/16s） |
| 并行改造引入数据错乱 | 保留开关：`faq.conversion.parallel-enabled=false` 时退回单线程循环（配置项，application.yml） |
| 连接池化引入连接泄漏 | 保留开关：`faq.conversion.connection-pool-enabled=false` 时退回每次 new HttpClient（性能回退但功能正确） |

## 10. 孤立 pending 记录恢复（运维保障）

**问题**：应用重启或崩溃时，正在处理的批次中已标记 `processing` 但未完成（success/failed）的记录会成为"孤儿"，永远停在 `processing` 状态。

**恢复策略**：

1. **应用启动时**：扫描所有 `status='running'` 的批次，将其 `processing` 记录重置为 `pending`（SQL：`UPDATE exam_faq_conversion SET conversion_status='pending' WHERE batch_id IN (SELECT id FROM exam_faq_conversion_batch WHERE status='running') AND conversion_status='processing'`）
2. **批次状态重置**：将 `status='running'` 的批次改为 `status='paused'`，管理员确认后手动恢复
3. **实现方式**：新增 `FaqConversionRecoveryService` 实现 `ApplicationRunner`，启动时执行上述扫描

> 此改动为运维保障性质，不影响核心功能，可在主体方案上线后补充。

## 11. 不在本次范围

以下需求明确不在本次优化范围内，后续按需评估：

- **批次取消（Cancel）**：当前仅支持暂停（Pause）+ 恢复（Resume）。并行模式下 Worker 线程无法通过 `isRunning()` 检查中断正在执行的 LLM 调用。取消功能需引入 `Future.cancel()` + HttpClient 中断机制，复杂度较高，留待后续。
- **更激进的 JSON 修复策略**：当前按"解析失败 = 重试该条"处理，不引入正则修复或 JSON 修复库。
