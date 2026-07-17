# FAQ 转换 LLM 调用性能分析报告

> 日期：2026-07-17
> 状态：待补充关键信息后给出优化方案

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

### 6.2 maxTokens 配置

模型配置表中 `max_tokens` 当前设置的值是多少？
这直接影响 LLM 生成时间。

### 6.3 并发批次数量

生产环境中通常同时跑几个批次？还是一次只跑一个？

### 6.4 单次 LLM 调用实际耗时

日志中 `FAQ LLM 请求` 和 `FAQ LLM 响应` 两条日志的时间差是多少秒？
这能确认 LLM API 的真实响应时间。

### 6.5 预期的处理速度目标

希望达到什么样的处理速度？（例如：1 小时处理 1000 条？）
这决定优化方案的目标。

## 7. 潜在优化方向（待确认信息后细化）

| 方向 | 说明 | 预期收益 |
|------|------|---------|
| 批次内并行化 | 将 for 循环改为并行流/多线程，使用 `concurrencyLimit` 控制并发 | 线性提升 N 倍 |
| similar + negative 并行 | 两条 LLM 调用并行执行而非串行 | 每条省一半时间 |
| 减少 LLM 调用次数 | 合并为一次调用同时生成 similar + negative | 每条省一半时间 |
| 调整超时参数 | 根据实际 LLM 响应时间调整 read timeout | 避免无效等待 |
| 连接池复用 | 复用 HttpClient 而非每次新建 | 减少 TCP 握手开销 |
