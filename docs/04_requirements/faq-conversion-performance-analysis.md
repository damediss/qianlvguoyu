# FAQ 转换 LLM 调用性能分析报告

> 日期：2026-07-17（分析） / 2026-07-19（方案定稿）
> 状态：✅ 全部信息已确认，优化方案已定稿，待实施

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
| LLM 客户端 | `ai/infrastructure/FaqLlmClient.java` | HTTP 调用，读超时 50s |
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

- `FaqLlmClient` 读超时设置为 **50 秒**
- 公有云 LLM API（DeepSeek / LongCat 等）生成 JSON 数组通常需要 30~70 秒
- 如果 prompt 较长或 maxTokens 较大，耗时更久

### 4.4 吞吐量计算

| 项目 | 值 |
|------|---|
| 每条记录耗时 | 2 次 LLM × ~72s ≈ **144 秒** |
| 每小时处理 | 3600 / 144 ≈ **25 条** |
| 24 小时处理 | 25 × 24 = **600 条** |

与实际观测值（24h / 600 条）吻合。

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
| 结论 | 合并调用（调用次数减半）+ 批次内 10 并发，恰好达标；并发 12 可留余量 |

### 7.2 优化组合（两项必须同时做，缺一不可）

```
现状:  2次串行调用 × 单线程       = 60s+/条  → 25条/小时
只做合并: 1次调用 × 单线程        = 40s/条   → 90条/小时   (3.6倍, 不够)
只做并行: 2次串行 × 10并发        = 6s/条    → 600条/小时  (24倍, 不够)
合并+并行: 1次调用 × 10并发       = 4s/条    → 900条/小时  (36倍, 达标 ✓)
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
| read timeout | 50s | **90s** | 合并后输出变长，实测单次最长 50s，留 1.8 倍余量 |
| `response_format` | `{"type": "json_object"}` | 不变 | LongCat OpenAI 兼容端点支持 |

> **注意**（AGENTS.md 踩坑 #13）：合并前需确认 LongCat 支持 `response_format` 字段。已知 **LongCat 不支持 `response_format`**，因此该字段一律不传，改为在 prompt 中强制约束输出 JSON 格式，解析端做容错（提取第一个 `{` 到最后一个 `}`）。

**抽样验证计划（上线前必做）：**

1. 从 single / multi / judge 三种题型各抽 7 条，共 **21 条**样本
2. 分别用「合并调用」和「原两次调用」生成，人工对比质量
3. 验收标准：合并版可用率 ≥ 90%（无需人工改写即可入库）
4. 若质量不达标 → 回退到「两次调用并行」模式（用 `CompletableFuture` 并行发两个请求，收益折半但仍可配合 10 并发达到 450 条/小时，此时需将并发上调至 20 才能达标，需重测）

### 7.4 方案二：批次内并行化（concurrencyLimit 生效）

改造 `processLlmEnhancementAsync()`：单线程 for 循环 → 信号量限流的并行执行。

```java
private void processLlmEnhancementAsync(Long batchId, Long tenantId,
                                        Long userId, int concurrencyLimit) {
    List<ExamFaqConversionEntity> pending = loadPendingRecords(batchId, tenantId);
    Semaphore semaphore = new Semaphore(concurrencyLimit);
    List<CompletableFuture<Void>> futures = pending.stream()
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
    refreshBatchCounters(batchId, tenantId);
}
```

**线程池配置调整（`AsyncConfig`）：**

| 配置 | 原值 | 新值 | 说明 |
|------|------|------|------|
| `faqConversionExecutor` | core=3, max=6, queue=500 | **core=4, max=8, queue=100** | 批次编排池（同时跑的批次数），生产只跑 1 个批次也够用 |
| 新增 `faqRecordExecutor` | — | **core=10, max=20, queue=2000** | 批次内记录级并行池，core 对应默认并发 10 |
| 拒绝策略 | 需确认 | **AbortPolicy** | AGENTS.md 踩坑 #12：禁止 CallerRunsPolicy，满了抛异常由全局处理器返回 503 |
| `concurrency_limit` 字段 | 未被使用 | **生效** | 批次创建时读取（默认 10，上限 20），Semaphore 以此初始化 |

**进度统计的线程安全**：多线程并发更新批次计数时，禁止 `read → +1 → update` 的竞态写法，改为：
- 每条记录完成后执行 `UPDATE exam_faq_conversion_batch SET success_count = success_count + 1 WHERE id = ?`（数据库原子自增）
- 或内存 `AtomicInteger` 累积 + 每 10 条批量落库一次（减少写放大，推荐）

**多租户与异步陷阱（AGENTS.md 强制约束）：**
- `tenantId` / `userId` 在 HTTP 线程捕获后通过参数传入，`faqRecordExecutor` Worker 线程内**全链路禁止调 `SecurityContext`**
- 每条记录处理前手动设置 `TenantContextHolder.set(tenantId)`，`finally` 中清除（Worker 线程复用会串租户）

### 7.5 方案三：指数退避重试（429/5xx）

```java
private String callLlmWithRetry(FaqLlmRequest req) {
    long[] backoffMs = {2000, 4000, 8000};
    for (int attempt = 0; attempt <= backoffMs.length; attempt++) {
        try {
            return faqLlmClient.chat(req);
        } catch (FaqLlmRateLimitException | FaqLlmServerException e) {
            if (attempt == backoffMs.length) throw e;
            Thread.sleep(backoffMs[attempt]);   // 2s → 4s → 8s
        }
        // 4xx（非429）不重试，直接抛
    }
}
```

| 规则 | 说明 |
|------|------|
| 可重试异常 | HTTP 429（限流）、5xx（服务端错误）、SocketTimeoutException |
| 不可重试 | 4xx 参数错误、JSON 解析失败（解析失败沿用原方案重试 2 次） |
| 单条最终失败 | 标 `conversion_status='failed'` + `error_message`，**批次不中断** |
| 退避时持信号量 | 退避 sleep 期间持有 semaphore 许可（防止瞬时对 LLM 打满） |

### 7.6 方案四：HTTP 连接池复用（辅助）

`FaqLlmClient` 当前若每次新建连接，10 并发下 TCP/TLS 握手开销明显。改为池化：

| 配置 | 值 |
|------|---|
| 实现 | Apache HttpClient 5 `PoolingHttpClientConnectionManager`（或复用现有 RestClient 配置的连接池） |
| `maxTotal` | 32 |
| `defaultMaxPerRoute` | 16（≥ 记录池 max=20 的常用水位） |
| keepAlive | 60s |
| 收益 | 每次调用省 1 次 TLS 握手（公网约 200~500ms），10 并发下累计可观 |

### 7.7 优化后吞吐量验证

| 场景 | 单次耗时 | 并发 | 吞吐 | 是否达标 |
|------|---------|------|------|---------|
| 悲观（50s/次） | 50s | 10 | 720 条/小时 → 20分钟 240 条 | ⚠️ 略低，并发调至 12 即达标 |
| 均值（40s/次） | 40s | 10 | **900 条/小时 → 20分钟 300 条** | ✅ 达标 |
| 乐观（20s/次） | 20s | 10 | 1800 条/小时 → 20分钟 600 条 | ✅ 富余 |

**上线后调节手段**：`exam_faq_conversion_batch.concurrency_limit` 为运行参数，无需改代码即可按实测 LLM 耗时下拉/上调。

## 8. 实施清单

| # | 改动点 | 文件 | 类型 |
|---|--------|------|------|
| 1 | 合并 prompt，删除两次调用逻辑，改单次调用解析两个数组 | `FaqLlmPromptBuilder` / `FaqConversionApplicationService.processSingleLlmEnhancement()` | 核心 |
| 2 | 移除 `response_format` 请求字段，解析端加 JSON 容错提取 | `FaqLlmClient` | 核心（踩坑 #13） |
| 3 | 批次内并行：Semaphore + `faqRecordExecutor` | `FaqConversionApplicationService.processLlmEnhancementAsync()` | 核心 |
| 4 | 新增 `faqRecordExecutor` Bean（core=10/max=20/AbortPolicy） | `AsyncConfig` | 配置 |
| 5 | `concurrency_limit` 从批次表读取并生效（默认 10，上限 20） | `FaqConversionBatchService` | 核心 |
| 6 | 指数退避重试（2s/4s/8s，区分可重试异常） | `FaqConversionApplicationService` | 核心 |
| 7 | 批次计数器原子更新（DB 自增或 AtomicInteger 批量落库） | `FaqConversionBatchService` / Mapper | 核心 |
| 8 | Worker 线程 TenantContextHolder 设置/清除 | `processLlmEnhancementAsync()` | 强制约束 |
| 9 | read timeout 50s → 90s | `FaqLlmClient` | 配置 |
| 10 | HttpClient 连接池化（maxTotal=32, perRoute=16） | `FaqLlmClient` 配置 | 辅助 |
| 11 | 21 条样本 A/B 质量验证（合并 vs 原两次） | 测试任务 | 验收门槛 |

## 9. 回滚预案

| 情况 | 回滚动作 |
|------|---------|
| 抽样验证质量不达标（可用率 < 90%） | 切回两次调用 + `CompletableFuture` 并行，并发调至 20 |
| 上线后 LLM 频繁 429 | `concurrency_limit` 下调至 5，退避间隔翻倍（4s/8s/16s） |
| 并行改造引入数据错乱 | 保留开关：`faq.conversion.parallel-enabled=false` 时退回单线程循环（配置项，application.yml） |
