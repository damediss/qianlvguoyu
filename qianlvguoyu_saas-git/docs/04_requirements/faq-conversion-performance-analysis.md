# FAQ 转换 LLM 调用性能分析报告

> 日期：2026-07-17（分析） / 2026-07-19（方案定稿 + 修订） / 2026-07-20（缺陷审阅修订 → 实施前终审）
> 状态：✅ 方案终审通过（代码核验 + 3 项决策确认 + 12 处缺陷修复），待实施
>
> **修订记录（2026-07-19）**：（略，同前）
>
> **修订记录（2026-07-20，缺陷审阅）**：（略，同前 10 处）
>
> **修订记录（2026-07-20，实施前终审）**：
> - 🔴 修复缺陷 1：熔断器 success 路径补充 `circuitBreaker.reset()`，消除非连续 429 误触发
> - 🔴 修复缺陷 2：统一 `processSingleRecordMerged` 为 4 参数签名 `(record, tenantId, modelConfig, batchId)`
> - 🔴 修复缺陷 3：7.4 伪代码补充编排线程加载 `modelConfig` 后传入记录任务
> - 🟡 修复缺陷 4：明确 `CircuitBreaker` 为批次级局部变量，附完整实现
> - 🟡 修复缺陷 5：确认批次表有 `error_message` 字段，`pauseBatchWithReason` 直接写入
> - 🟡 修复缺陷 6：异常继承体系明确为 `FaqLlmException extends BusinessException`，7 子类继承它
> - 🟡 修复缺陷 7：明确 `FaqLlmPartialResultException` 由应用层抛出（JSON 解析后判空数组），非 `FaqLlmClient`
> - 🟡 修复缺陷 8：`finish_reason='stop'` 但 content 为空/null → `FaqLlmServerException`（瞬时重试 3 次）
> - 🟡 修复缺陷 9：补充 `loadPendingRecords` SQL 定义
> - 🟡 修复缺陷 10：连接池补充 `validateAfterInactivity=5000ms` 防 stale connection
> - 🟡 修复缺陷 11：明确 `FaqLlmClient` 改造后保留现有 request/response 详细日志
> - 🟡 修复缺陷 12：`faqRecordExecutor` 线程名前缀设为 `faq-record-`

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
| 每条记录耗时 | 2 次串行 LLM × 等效 ~72s ≈ **144 秒** |
| 每小时处理 | 3600 / 144 = **25 条** |
| 24 小时处理 | 25 × 24 = **600 条** |

与观测值（24h / 600 条）精确吻合。

> **"等效 ~72s"与实测 10~50s 的关系**：10~50s 是**成功调用**的日志区间；等效值还包含失败路径开销——超时（70s 读超时）后重试最多 3 次 + 重试间隔 sleep、429/5xx 重试、DB 写入等。失败路径把单条等效耗时推高到成功耗时的 1.5~2 倍。

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
3. LLM API 成功响应 10~50 秒，叠加超时重试等失败路径后等效单次 ~72 秒 → 单条 ~144 秒
4. 三者叠加 → 吞吐量被锁死在 25 条/小时

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
现状:      2次串行 × 单线程   = 144s/条(含失败路径等效) → 25条/小时
只做合并:  1次调用 × 单线程   = ~80s/条(40s成功+失败等效) → 45条/小时   (1.8倍, 不够)
只做并行:  2次串行 × 12并发   = 144÷12=12s/条            → 300条/小时  (12倍, 不够)
合并+并行: 1次调用 × 12并发   = 40÷12=3.3s/条            → 1080条/小时 (43倍, 达标 ✓ 有余量)
```

> 口径说明：「只做并行」仍沿用旧客户端（无连接池、无异常分类），失败路径等效开销不变；「合并+并行」含方案三/四全部改造（连接池 + 瞬时/确定性分类 + 熔断），失败路径开销大幅收敛，按成功路径 40s/次估算（同 §7.1）。

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
| `response_format` | 当前代码未传 | **保持不传** | 见下方注意（踩坑 #13） |

> **注意**（AGENTS.md 踩坑 #13）：**LongCat 不支持 `response_format`**，该字段一律不传，改为在 prompt 中强制约束输出 JSON 格式，解析端做容错（提取第一个 `{` 到最后一个 `}`）。当前代码已确认未传此字段，本次仅复核确认，无改动。

**部分结果处理策略（瞬时失败，可重试）：**

合并调用可能产生部分字段为空的情况（如 LLM 只生成了 similar_questions 但 negative_questions 为空数组）。处理规则：
- **两个字段均非空** → 正常入库，`conversion_status='success'`
- **任一字段为空数组** → 判定为**瞬时失败**（模型采样方差导致，换次采样大概率完整），抛 `FaqLlmPartialResultException`，按 §7.5 指数退避重试 3 次；仍失败标 `failed`
- **两个字段均为空** → 同上，按瞬时失败重试

**JSON 解析容错策略（区分瞬时 / 确定性）：**

`FaqLlmResponseParser` 已有二阶 fallback（`indexOf('{')` → `lastIndexOf('}')`），合并后继续使用此策略。解析失败按 `finish_reason` 区分两类（详见 §7.5 异常分类）：

| 场景 | 判定 | 策略 |
|------|------|------|
| `finish_reason='length'`（输出被 max_tokens 截断） | **瞬时**（采样随机性，重试输出可能更短） | 退避重试 3 次 |
| `finish_reason='stop'` 但 JSON 解析失败（如题干含 `{`/`}` 干扰边界） | **确定性**（同一 prompt 重试大概率同样失败） | **不重试**，直接标 `failed`，`error_message` 标注"确定性失败：JSON 解析失败（完整响应但格式异常）" |

**抽样验证计划（上线前必做）：**

1. 从 single / multi / judge 三种题型各抽 7 条，共 **21 条**样本
2. 分别用「合并调用」和「原两次调用」生成，人工对比质量
3. 验收标准：合并版可用率 ≥ 90%（两个字段均非空且无需人工改写即可入库）
4. 若质量不达标 → 回退到「两次调用并行」模式（用 `CompletableFuture` 并行发两个请求，收益折半但仍可配合并发提升达标：单条等效 ~72s，需并发 25 才能达到 20 分钟 300 条目标；`faqRecordExecutor` core=30 已按此预留，仅需将 `concurrency_limit` 上调至 25 并回归重测）

### 7.4 方案二：批次内并行化（concurrencyLimit 生效）

改造 `processLlmEnhancementAsync()`：单线程 for 循环 → 信号量限流的并行执行。

**⚠️ 现状修正**：当前代码 `processLlmEnhancementAsync()` **未设置 `TenantContextHolder`**（代码核验确认），导致 MyBatis-Plus 多租户拦截器在 Worker 线程中无法自动拼 `tenant_id`。这是 bug，方案二必须修复。

**⚠️ 关键约束**：`TenantContextHolder` 基于 ThreadLocal，**不跨线程传播**。仅在编排线程（`faqConversionExecutor`）设置对 `faqRecordExecutor` 的记录任务线程**无效**——必须在**每条记录任务内部** set/clear。

```java
private void processLlmEnhancementAsync(Long batchId, Long tenantId,
                                        Long userId, int concurrencyLimit) {
    // 编排线程：加载 pending 记录 + 模型配置（各一次，传入记录任务复用）
    TenantContextHolder.set(tenantId);
    List<ExamFaqConversionEntity> pending;
    ExamFaqModelConfigEntity modelConfig;
    try {
        pending = loadPendingRecords(batchId, tenantId);
        modelConfig = modelConfigService.findByModelCode(tenantId, batchModelCode(tenantId, batchId));
        if (modelConfig == null) {
            log.error("模型配置不存在：tenantId={}, batchId={}", tenantId, batchId);
            return;
        }
    } finally {
        TenantContextHolder.clear();
    }

    // 熔断器：批次级局部变量（每个批次一个实例，12 并发线程共享）
    CircuitBreaker circuitBreaker = new CircuitBreaker(5);
    Semaphore semaphore = new Semaphore(concurrencyLimit);
    int chunkSize = 50;
    for (int offset = 0; offset < pending.size(); offset += chunkSize) {
        if (!batchService.isRunning(tenantId, batchId)) {
            log.info("批次已暂停，停止提交新任务：batchId={}, offset={}", batchId, offset);
            break;
        }
        List<ExamFaqConversionEntity> chunk = pending.subList(offset, Math.min(offset + chunkSize, pending.size()));
        List<CompletableFuture<Void>> futures = chunk.stream()
            .map(record -> CompletableFuture.runAsync(() -> {
                // ⚠️ 关键：每条记录任务内部必须独立设置租户上下文
                TenantContextHolder.set(tenantId);
                try {
                    // 秒级暂停响应：每条记录开头再检查一次
                    if (!batchService.isRunning(tenantId, batchId)) {
                        return;  // 记录保持 pending，resume 时续跑
                    }
                    processRecordWithPermit(record, semaphore, modelConfig, tenantId, batchId, circuitBreaker);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    TenantContextHolder.clear();
                }
            }, faqRecordExecutor))
            .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }
    // 批次完成流转：pendingCount<=0 → completeBatch（沿用现有方法）
    checkBatchCompletion(batchId, tenantId);
}

/** 加载待处理记录：conversion_status='pending' 按 id 排序 */
private List<ExamFaqConversionEntity> loadPendingRecords(Long batchId, Long tenantId) {
    return faqConversionMapper.selectList(new LambdaQueryWrapper<ExamFaqConversionEntity>()
        .eq(ExamFaqConversionEntity::getTenantId, tenantId)
        .eq(ExamFaqConversionEntity::getBatchId, batchId)
        .eq(ExamFaqConversionEntity::getConversionStatus, "pending")
        .orderByAsc(ExamFaqConversionEntity::getId));
}

/** 从批次实体读取 modelCode（用于 resume/retry 路径） */
private String batchModelCode(Long tenantId, Long batchId) {
    ExamFaqConversionBatchEntity batch = batchService.getBatchEntity(tenantId, batchId);
    return batch != null ? batch.getModelCode() : null;
}
```

**线程池配置调整（`AsyncConfig`）：**

| 配置 | 原值 | 新值 | 说明 |
|------|------|------|------|
| `faqConversionExecutor` | core=3, max=6, queue=500 | **core=2, max=4, queue=50** | 批次编排池。生产只跑 1 个批次，`allOf().join()` 阻塞 1 线程 ~20min，core=2 留 1 线程给 resume/retry 等操作 |
| 新增 `faqRecordExecutor` | — | **core=30, max=30, queue=2000, prefix=`faq-record-`** | 批次内记录级并行池。core 必须 ≥ 最大预期并发（见下方说明），默认并发 12 由 Semaphore 限制，其余线程 park 等待 |
| 拒绝策略 | 未显式配置（Spring 默认 AbortPolicy） | **显式声明 AbortPolicy** | AGENTS.md 踩坑 #12：禁止 CallerRunsPolicy，满了抛异常由全局处理器返回 503 |
| `concurrency_limit` 字段 | 未被使用（默认值 5） | **生效，默认值 5 → 12，上限 30** | 批次创建时读取，Semaphore 以此初始化 |

> **为什么 core=30 而不是 core=12/max=24**：Spring `ThreadPoolTaskExecutor` 的线程增长规则是"core 满 → 入队 → **队列满**才扩到 max"。queue=2000 几乎不会满，core=12 时线程池永远只有 12 个线程——回退场景需要 25 并发（§7.3 抽样验证第 4 点）将永远达不到。因此 core 直接设为最大预期并发 30，实际并发由 Semaphore（= concurrency_limit，默认 12）控制，空闲线程 park 的开销可忽略。

**进度统计的线程安全（现状确认）：**

代码核验确认：`ExamFaqConversionBatchMapper` 的 `incrementSuccess/Failed/Skipped/Pending` **已是 SQL 级别原子操作**（`SET success_count = success_count + 1`），当前无线程安全问题，**无需改造**。

**多租户与异步陷阱（AGENTS.md 强制约束）：**
- `tenantId` / `userId` 在 HTTP 线程捕获后通过参数传入，`faqRecordExecutor` Worker 线程内**全链路禁止调 `SecurityContext`**
- `TenantContextHolder` 基于 ThreadLocal **不跨线程传播**：编排线程仅在 `loadPendingRecords` 期间 set/clear；**每条记录任务内部**必须独立 `TenantContextHolder.set(tenantId)` / `clear()`（当前代码完全缺失，必须修复）
- `loadPendingRecords` 及记录任务内的 Mapper 调用，MyBatis-Plus 多租户拦截器依赖 `TenantContextHolder` 自动拼 `tenant_id`

### 7.5 方案三：指数退避重试 + 熔断器（429/5xx）

**异常分类体系（新增，区分瞬时/确定性）：**

当前 `FaqLlmClient` 统一抛出 `BusinessException(ErrorCode.BAD_GATEWAY)`，无法区分 429/5xx/超时。方案三必须先建立异常分类。

**异常继承体系**：所有新增异常继承自 `FaqLlmException extends BusinessException`，保持与现有全局异常处理器兼容（`GlobalExceptionHandler` 已覆盖 `BusinessException`），7 个具体子类继承 `FaqLlmException`：

```java
// 基类
public class FaqLlmException extends BusinessException {
    public FaqLlmException(ErrorCode errorCode, String message) { super(errorCode, message); }
}

// 7 个子类（瞬时）
public class FaqLlmRateLimitException extends FaqLlmException { ... }   // 429
public class FaqLlmServerException extends FaqLlmException { ... }     // 5xx + content为空
public class FaqLlmTimeoutException extends FaqLlmException { ... }    // 超时/网络
public class FaqLlmTruncatedException extends FaqLlmException { ... }  // finish_reason=length
public class FaqLlmPartialResultException extends FaqLlmException { ... } // 字段空数组

// 7 个子类（确定性）
public class FaqLlmParseException extends FaqLlmException { ... }      // stop但解析失败
public class FaqLlmClientException extends FaqLlmException { ... }     // 4xx非429
```

| 新异常类 | 触发条件 | 分类 | 重试策略 |
|---------|---------|------|---------|
| `FaqLlmRateLimitException` | HTTP 429 | 瞬时 | ✅ 退避重试 3 次 |
| `FaqLlmServerException` | HTTP 5xx；或 `finish_reason='stop'` 但 content 为空/null | 瞬时 | ✅ 退避重试 3 次 |
| `FaqLlmTimeoutException` | SocketTimeoutException / 网络抖动 | 瞬时 | ✅ 退避重试 3 次 |
| `FaqLlmTruncatedException` | `finish_reason='length'`（max_tokens 截断） | 瞬时 | ✅ 退避重试 3 次 |
| `FaqLlmPartialResultException` | JSON 合法但 similar/negative 任一为空数组（**应用层抛出**） | 瞬时 | ✅ 退避重试 3 次 |
| `FaqLlmParseException` | `finish_reason='stop'` 但 JSON 解析失败（如题干含 `{}` 干扰边界） | **确定性** | ❌ 不重试，直接 failed |
| `FaqLlmClientException` | HTTP 4xx（非 429，如 401/400） | **确定性** | ❌ 不重试，直接 failed |

> `FaqLlmClient` 改造：
> 1. 根据 `RestClientResponseException.getStatusCode()` 抛出对应子类，替代当前统一 `BusinessException`
> 2. **新增 `choices[0].finish_reason` 检测**：`length` → 抛 `FaqLlmTruncatedException`（在解析 JSON 之前判定）；`stop` 但 content 为空/null → 抛 `FaqLlmServerException`
> 3. 解析失败且 `finish_reason='stop'` → 抛 `FaqLlmParseException`
>
> **⚠️ 层级分工**：`FaqLlmPartialResultException` 由**应用层**（`processSingleRecordMerged`）在 JSON 解析完成后判空数组抛出，不是 `FaqLlmClient` 抛出。

**退避策略（持有许可 sleep）：**

```java
// 许可获取/释放与重试在同一方法内闭环，杜绝跨方法许可管理
private void processRecordWithPermit(ExamFaqConversionEntity record, Semaphore semaphore,
                                     ExamFaqModelConfigEntity modelConfig,
                                     Long tenantId, Long batchId, CircuitBreaker circuitBreaker)
        throws InterruptedException {
    semaphore.acquire();
    try {
        processSingleRecordMerged(record, tenantId, modelConfig, batchId, circuitBreaker);
    } finally {
        semaphore.release();
    }
}

private String callLlmWithRetry(String prompt, ExamFaqModelConfigEntity modelConfig,
                                Long tenantId, CircuitBreaker circuitBreaker) {
    long[] backoffMs = {2000, 4000, 8000};
    for (int attempt = 0; attempt <= backoffMs.length; attempt++) {
        try {
            String result = faqLlmClient.chatCompletion(
                modelConfig.getBaseUrl(), modelConfig.getApiKey(),
                modelConfig.getModelId(), prompt, modelConfig.getMaxTokens(), tenantId);
            circuitBreaker.onSuccess();           // ✅ 成功时复位连续 429 计数
            return result;
        } catch (FaqLlmRateLimitException e) {
            circuitBreaker.onHttp429();           // 熔断计数（见下）
            attemptOrThrow(e, attempt, backoffMs);
        } catch (FaqLlmServerException | FaqLlmTimeoutException
               | FaqLlmTruncatedException | FaqLlmPartialResultException e) {
            circuitBreaker.onSuccess();           // 非 429 结果，复位连续 429 计数
            attemptOrThrow(e, attempt, backoffMs);
        }
        // FaqLlmParseException / FaqLlmClientException：确定性，不重试直接向上抛
    }
    throw new IllegalStateException("不应到达此处");
}

private void attemptOrThrow(RuntimeException e, int attempt, long[] backoffMs) {
    if (attempt == backoffMs.length) throw e;
    try {
        Thread.sleep(backoffMs[attempt]);   // 持有许可 sleep
    } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        throw e;
    }
}
```

> **设计变更说明（2026-07-20）**：2026-07-19 版采用"释放许可 → sleep → 重新获取"，但 sleep 被中断时许可未重新获取、外层 finally 又 release，**许可净增导致并发控制缓慢失效**；且许可状态跨方法传递过于脆弱。改为持有许可 sleep 后：
> - 许可获取/释放在 `processRecordWithPermit` 一个方法内闭环，无泄漏路径
> - 停滞风险有界：单条最多 3 次退避共 14s，且熔断器在连续 5 次 429 时暂停整个批次，最坏情况全批次停滞 ≤ 14s

**熔断器（Circuit Breaker）— 批次级局部变量：**

```java
/**
 * 熔断器 — 每个批次一个实例（processLlmEnhancementAsync 的局部变量）。
 * 连续 N 次 429 → 触发 isOpen() → 调用方暂停批次。
 */
private static class CircuitBreaker {
    private final int threshold;
    private final AtomicInteger consecutive429 = new AtomicInteger(0);

    CircuitBreaker(int threshold) { this.threshold = threshold; }

    void onHttp429() { consecutive429.incrementAndGet(); }
    void onSuccess()  { consecutive429.set(0); }
    boolean isOpen()  { return consecutive429.get() >= threshold; }
}
```

> ⚠️ `CircuitBreaker` 必须是**批次级局部变量**（在 `processLlmEnhancementAsync` 内 `new CircuitBreaker(5)`），不能是类字段或 Bean，否则多批次共享计数器会互相干扰。12 并发线程共享同一个实例，AtomicInteger 保证线程安全。

连续 **5 次** LLM 调用返回 429（全局计数，不限具体记录），说明 LLM 服务降级或区域性故障，继续重试只会空耗资源：

```
实现要点：
  - 批次级 CircuitBreaker 实例（见上），threshold=5
  - LLM 调用结果 onSuccess() 复位；onHttp429() 累加
  - 12 并发下"连续"语义为近似值（按结果到达顺序计数），可接受

circuitBreaker.isOpen() == true → 触发熔断：
  1. 调用 batchService.pauseBatchWithReason(tenantId, batchId, reason) 暂停批次
     （批次表已有 error_message 字段，直接写入
      "LLM 服务限流熔断，已自动暂停，请确认服务恢复后手动恢复批次"）
  2. 编排循环在 chunk 边界 isRunning()=false 自然停止提交新任务；
     排队中的记录任务在开头 isRunning() 检查处快速退出；执行中的 LLM 调用自然结束
     （⚠️ 不能"释放所有已持有许可"——许可由持有线程自己释放，外部无法代操作）
  3. 记录 ERROR 日志（含连续 429 计数、时间窗口、批次已处理数）
  4. 前端轮询发现 paused + error_message 后展示告警，管理员确认 LLM 恢复后手动恢复批次
  5. 恢复后从 pending 记录续跑（已有 resumeBatch 机制可用）
```

**单条失败处理：**

| 规则 | 说明 |
|------|------|
| 瞬时失败重试 3 次后仍失败 | 标 `conversion_status='failed'` + `error_message`，**批次不中断** |
| 确定性失败（`finish_reason='stop'` 解析失败 / 4xx 非 429） | **不重试**，直接标 `failed`，`error_message` 以"确定性失败："前缀标注原因（便于运营筛出题干含 `{}` 等问题题目人工处理） |

### 7.6 方案四：HTTP 连接池复用（**核心，非辅助**）

代码核验确认：`FaqLlmClient` **每次调用都 new `HttpClient` + `RestClient`**（line 73-80），无连接池、无 keep-alive。10+ 并发下问题严重：

- **TLS 握手**：每次调用 ~200-500ms 额外开销
- **TCP 端口压力**：大量 socket 快速进入 TIME_WAIT，高并发下可能端口耗尽
- **无连接复用**：同一 LLM 端点每次重新建连

**依赖核验（2026-07-20）**：`httpclient5` 已在 `pom.xml:55`，Spring Boot 3.3.13（Spring Framework 6.1+）自带 `HttpComponentsClientHttpRequestFactory`，**无需新增依赖**。

| 配置 | 值 |
|------|---|
| 实现 | Apache HttpClient 5 `PoolingHttpClientConnectionManager` + `HttpComponentsClientHttpRequestFactory`，`FaqLlmClient` 改造为持有单例 `RestClient` |
| `maxTotal` | 60（= `faqRecordExecutor` 最大并发 30 的 2 倍，留余量） |
| `defaultMaxPerRoute` | 30（= 最大并发上限，确保并发线程不受连接池限制） |
| keepAlive | 60s |
| `connectTimeout` | 70s（不变） |
| `responseTimeout`（读超时） | 90s（方案一同步调整） |
| 收益 | 每次调用省 1 次 TLS 握手 + TCP 端口复用；12 并发持续运行时避免端口耗尽 |

**改造方式**：`FaqLlmClient` 不再在 `chatCompletion()` 方法内 new HttpClient，改为类级别单例（构造一次，应用生命周期内复用）：

```java
@Component
public class FaqLlmClient {

  private final ObjectMapper objectMapper;
  private final RestClient client;   // 单例，内部含连接池

  public FaqLlmClient(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;

    PoolingHttpClientConnectionManager connectionManager =
        PoolingHttpClientConnectionManagerBuilder.create()
            .setMaxConnTotal(60)
            .setMaxConnPerRoute(30)
            .setValidateAfterInactivity(TimeValue.ofSeconds(5))  // 防 stale connection
            .build();

    CloseableHttpClient httpClient = HttpClients.custom()
        .setConnectionManager(connectionManager)
        .setDefaultRequestConfig(RequestConfig.custom()
            .setConnectTimeout(Timeout.ofSeconds(70))
            .setResponseTimeout(Timeout.ofSeconds(90))
            .build())
        .setKeepAliveStrategy((response, context) -> TimeValue.ofSeconds(60))
        .build();

    this.client = RestClient.builder()
        .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
        .build();
  }

  // chatCompletion() 内直接使用 this.client，不再 new；保留现有 request/response 详细日志
}
```

> 注意：JDK `java.net.http.HttpClient` 不支持自定义连接池管理器，必须从 JDK HttpClient 切换到 Apache HttpClient 5（`JdkClientHttpRequestFactory` → `HttpComponentsClientHttpRequestFactory`），`RestClient` 调用方代码不变。

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
| 1 | 合并 prompt，删除两次调用逻辑，改单次调用解析两个数组；JSON 解析后应用层判空数组 → 抛 `FaqLlmPartialResultException` 走退避重试 | `FaqLlmPromptBuilder` / `FaqConversionApplicationService` | 核心 |
| 2 | `response_format` 保持不传（当前代码已未传，仅复核确认） | `FaqLlmClient` | 确认（踩坑 #13） |
| 3 | 新增异常继承体系：`FaqLlmException extends BusinessException` + 7 子类（瞬时 5：`FaqLlmRateLimit/Server/Timeout/Truncated/PartialResult`；确定性 2：`FaqLlmParse/Client`） | `ai/infrastructure/` 新增 8 个异常类（1 基类 + 7 子类） | 核心 |
| 4 | `FaqLlmClient` 根据 HTTP status code 抛出对应异常子类；新增 `finish_reason` 检测（`length` → Truncated；`stop` + content 为空/null → Server）；解析失败 + `stop` → Parse；保留现有 request/response 详细日志 | `FaqLlmClient` | 核心 |
| 5 | 批次内并行：编排线程一次性加载 `pending` 记录 + `modelConfig`，分块提交 + Semaphore + `faqRecordExecutor` + 每条记录任务开头暂停检查 + 熔断器（批次级局部变量） | `FaqConversionApplicationService.processLlmEnhancementAsync()` | 核心 |
| 6 | 新增 `faqRecordExecutor` Bean（core=30/max=30/queue=2000/prefix=`faq-record-`/显式 AbortPolicy）；调整 `faqConversionExecutor`（core=2/max=4） | `AsyncConfig` | 配置 |
| 7 | `concurrency_limit` 从批次表读取并生效（默认值 5 → **12**，上限 **30**） | `FaqConversionBatchService` / `FaqConversionApplicationService` | 核心 |
| 8 | 指数退避重试（2s/4s/8s × 3 次，持有许可 sleep；`callLlmWithRetry` 成功时复位熔断计数）；确定性 2 类不重试直接 failed 并标注"确定性失败：" | `FaqConversionApplicationService` | 核心 |
| 9 | 熔断器：批次级 `CircuitBreaker` 连续 429 计数 ≥ 5 → `pauseBatchWithReason(tenantId, batchId, reason)` 暂停批次（写入 error_message）+ ERROR 日志 | `FaqConversionApplicationService` / `FaqConversionBatchService` | 核心 |
| 10 | Worker 线程 TenantContextHolder 设置/清除（每条记录任务内部 + 编排线程加载期，修复当前缺失 bug） | `processLlmEnhancementAsync()` | 强制约束 |
| 11 | read timeout 70s → 90s | `FaqLlmClient` | 配置 |
| 12 | HttpClient 连接池化：JDK HttpClient → Apache HttpClient 5（maxTotal=60, perRoute=30, keepAlive=60s, validateAfterInactivity=5s），`FaqLlmClient` 持有单例 `RestClient`；依赖已具备（`pom.xml:55`） | `FaqLlmClient` 改造 | **核心** |
| 13 | 新增 `loadPendingRecords(batchId, tenantId)` 方法（按 batch_id + conversion_status='pending' 排序查询） | `FaqConversionApplicationService` | 辅助 |
| 14 | 21 条样本 A/B 质量验证（合并 vs 原两次，含部分结果判定标准） | 测试任务 | 验收门槛 |
| 15 | 应用启动时扫描孤立 pending 记录（可选，见 §10） | 新增 `FaqConversionRecoveryService` | 运维保障 |

## 9. 回滚预案

| 情况 | 回滚动作 |
|------|---------|
| 抽样验证质量不达标（可用率 < 90%） | 切回两次调用 + `CompletableFuture` 并行，并发调至 **25**（`faqRecordExecutor` core=30 已预留，无需改代码，仅需调 `concurrency_limit` 并回归重测） |
| 上线后 LLM 频繁 429 | 熔断器自动暂停批次；恢复后 `concurrency_limit` 下调至 6，退避间隔翻倍（4s/8s/16s） |
| 并行改造引入数据错乱 | 保留开关：`faq.conversion.parallel-enabled=false` 时退回单线程循环（配置项，application.yml） |
| 连接池化引入连接泄漏 | 保留开关：`faq.conversion.connection-pool-enabled=false` 时退回每次 new HttpClient（性能回退但功能正确） |
| 合并调用上线后发现质量问题 | ⚠️ 合并调用**无运行时开关**（prompt 结构不同，无法配置切换），质量门槛依赖上线前 21 条抽样验证拦截；若漏网，需发版回退代码。接受此风险的前提：抽样验证严格执行 |

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
