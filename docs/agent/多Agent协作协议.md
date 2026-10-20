# 多 Agent 协作协议（Mavis Team / Claude Code Team 适用）

> 从根目录 `AGENTS.md` 拆出（2026-09-20 拆分）。团队阶段（owner + producer + verifier）必读；单 agent 阶段可跳过。

## 角色

| 角色 | 干啥的 | 不要干啥 |
|---|---|---|
| **owner** | 拆 task、派活、看 CycleReport、决断 PASS/FAIL | 不要亲自写生产代码 |
| **producer** | 按 task 写代码 / 跑构建 / 写 deliverable | 不要自己决定 PASS；每写完一个文件就 append 一行到 deliverable.md，给引擎心跳 |
| **verifier** | 独立审查产物；跑 `mvn compile` / `npm run build` / `DESCRIBE table`；给 PASS/FAIL | 不要顺手改 producer 代码；不要靠"看着对"判 PASS |
| **tester** | 跑单测 / 集成测 / 端到端 | 不要跳过手动冒烟 |

## 数据事实复核（存在性断言，2026-10-20 起）

- verifier 职责加一条：生产者每一条**存在性断言**（"不存在/只有/没有"，以及"在 X 表 / 由 X 提供"类肯定断言），必须按 `docs/agent/数据关系速查.md` 30 秒红线（第 0 步 `node scripts/agent/schema-find.mjs <关键词>` 或手工五步）**独立重跑并附结果**，不得由生产者自证。
- **单 Agent 阶段口径**（本协议自述单 Agent 阶段可跳过 verifier，而 2026-10-20 真题事故恰恰发生在单 Agent 模式——规则不能在默认工作模式下空转）：无 verifier 时，producer 的存在性断言必须在方案/交付文档中附五步或 `schema-find` 输出原文；owner 评审只查证据、不查人。与 `services/platform-api/AGENTS.md` 第 6 条呼应。
- "数据事实"要求落到 `docs/05_decisions/方案数据事实评审清单.md`：现状表每一行必须有 SQL 或 `file:line`；没有证据路径的只能标"未核实"，不得进入结论。

## 硬上限

- **producer 阶段：15min（系统硬限，撞了必被 kill）**
- verifier 阶段：30min（可申请延长一次）
- **生产者别在 thinking 里反复权衡方案** —— 先写代码 → 跑构建 → 错再改
- 全链路 E2E 单 task 装不下，**拆成"接口代码 + 编译"和"单测 + 真实启动"两个 sub-task**

> 口径说明（2026-09-20 统一）：历史文档中"producer 25min"的说法作废，以系统硬限 15min 为准（踩坑库 #6 已同步标注）。

## 交棒 / 上报

- producer 完成后：在 deliverable.md 写**已完成文件清单 + 编译/测试日志关键行 + 已知遗留**
- verifier 完成后：在 verdict 里写 **PASS / FAIL + 一句话证据**（不能只说"看起来对"）
- 失败 owner 重派时：steer 一次，不要反复 abort + spawn（浪费 session）

## 并行约束

- 同一 controller / 同一 page 文件，**同一时间只允许 1 个 agent 改**
- 改 `BaseEntity` / `pom.xml` / `tsconfig.json` / Flyway 主干迁移 —— **必须先在 plan 里 mark 为单 task**
- producer A 完成后，producer B 才能开始读 A 的产物 —— 不要并发读未提交的中间状态
- 文件所有权默认按目录划分：`services/platform-api/exam/` 归题库模块 owner；`apps/admin-web/src/pages/exam/` 归前端对应模块 owner；跨模块改动必须 owner 显式授权
