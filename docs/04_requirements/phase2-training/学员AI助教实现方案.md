# 学员 AI 助教 — 实现方案

> 目标项目：`qianlvguoyu_saas`（`platform-api`）+ App：`gjjd_appnext`  
> 关联文档：  
> - [App同步学刷题作业_实现方案.md](./App同步学刷题作业_实现方案.md)（通道 A/B/C 边界）  
> - [培训模块_App端实现方案.md](./培训模块_App端实现方案.md)（我的班级 / 五角色）  
> - [AI同步学（学员复习）实现方案.md](../AI同步学（学员复习）实现方案.md)（排题 / overview / explain）  
> - [方案A_App登录PHP签发与SaaS令牌镜像验票实现方案.md](./方案A_App登录PHP签发与SaaS令牌镜像验票实现方案.md)  
> 状态：**P0 范围已冻结（访谈 2026-08-25）**；待开发  
> 编写日期：2026-08-25  
> 说明：替代已删除的《AI班级助理实现方案.md》；产品重心从「班务助理」改为「学员学习助教」。

---

## 〇、一句话

在 **「我的班级」悬浮弹窗** 内，为学员提供 **「该刷哪（同步学）」+「讲题（解析→同步学→WeKnora）」**；规则 Tool 取真数，LLM 只润色话术。

---

## 一、已冻结决策（P0）

| # | 项 | 结论 |
|---|----|------|
| D1 | 首期用户 | **仅学员**（老师催交/抽题布置不做） |
| D2 | 主价值 | **C 该刷哪 + D 讲题** |
| D3 | 「该刷哪」主通道 | **AI 同步学（通道 C）**；不主打精准学/班级刷题 |
| D4 | 讲题降级链 | **官方解析 → 同步学讲解 → WeKnora**（**落地分档**：O3-P0 仅保证官方/题库解析或「暂无法讲解」；同步学讲解档与 WeKnora 讲题档见优化方案 P1） |
| D5 | 入口 | **我的班级悬浮弹窗（FAB）**；后期可迁提升 |
| D6 | 旧入口 | **删除**各角色台「AI 班级助理」按钮 |
| D7 | 首屏 | **同步学摘要卡 + chips + 可自由输入** |
| D8 | 智能策略 | **规则意图 + Tool；LLM 仅润色**；失败回退规则文案（**润色默认关**，开关见 [AI小培与双轨智能体优化方案.md](./AI小培与双轨智能体优化方案.md) §3.5；P0 未接线视为关） |
| D9 | 讲题题目来源 | **当前同步学题为主**；粘贴/拍照后补 |
| D10 | 无同步学计划 | **自动匹配本班 exam**；无计划则 **隐藏「该刷哪」**，保留讲题与引导 |

### 1.1 非目标（P0）

| 不做 | 原因 |
|------|------|
| 老师侧催交 / 抽题布置 / 未交名单 | 首期仅学员 |
| 聊天内直接答题 / 新答卷域 | 助教只编排；答卷仍落 C（及讲题只读） |
| 合并 A/B/C 指标或错题主库 | 通道隔离已锁 |
| 提升 Tab 独立入口 / 全站 FAB | 后期再迁 |
| 粘贴题干、拍照搜题 | P1+ |
| 数字人 / 语音 | 另立需求 |
| 主动推送 / 群发 | 不做 |

---

## 二、与现网关系

```text
通道 A  班级刷题     /appsmart + practice-scope     （助教 P0 不主推）
通道 B  作业/模拟考  /training/follow/tasks*        （助教 P0 不主推）
通道 C  AI 同步学    /appreviewsync/**              ←「该刷哪」真源
通道 D' 学员 AI 助教  本方案：班级 FAB 弹窗           ← 编排层
陪练    /appcoach → WeKnora                         ← 讲题链第三档
```

| 原则 | 说明 |
|------|------|
| 助教是编排层 | 不新建答题域；推荐练跳同步学；讲题只读展示 |
| 班务数字仍走 Tool | 本 P0 几乎不暴露班务；若保留「今日课表」等辅佐，禁止 LLM 口算 |
| 与旧「班级助理」 | 产品替换：删入口；后端可演进现有 `assistant/*` 或新 `tutor/*`（见 §五） |

### 2.1 可复用资产

| 资产 | 用途 |
|------|------|
| `ClassAssistantApplicationService` + `/training/follow/classes/{classId}/assistant/*` | 可改造为学员助教，或旁路新服务 |
| `/appreviewsync/overview|today|progress|availability` | 摘要卡、「该刷哪」 |
| `ReviewLlmAssistService`（同步学讲解/鼓励） | 讲题链第二档 |
| 题目官方解析字段 / resolve 类接口 | 讲题链第一档 |
| `/appcoach` + `WeKnoraClient` | 讲题链第三档 |
| `FaqLlmClient` + `exam_faq_model_config` | 话术润色 |
| 班级 → `train_info_id` → `train_exam_list` | 自动匹配 `examId`（与跟班考试通过率同路径） |

---

## 三、信息架构与交互

### 3.1 入口

```text
我的班级 → 某班工作台（学员）
              └─ 悬浮按钮「AI 助教」→ 半屏/弹层
```

- **删除**：`roles/student.vue`（及 monitor/leadTeacher/headTeacher/followStaff）上的「AI 班级助理」按钮。  
- P0 **仅学员角色**展示 FAB；其他角色不展示（与 D1 一致）。  
- `pages/class/assistant.vue`：可改为弹窗内容组件的宿主，或废弃全屏路由、仅保留组件被 FAB 引用。

### 3.2 弹窗首屏

| 区域 | 内容 |
|------|------|
| 摘要卡 | 匹配到的同步学：阶段/今日进度/是否可练；无计划则说明原因 |
| chips | 有计划：`该刷哪` `继续练` `讲题`；无计划：隐藏前两者，保留 `讲题`（若有题上下文）+ `如何开通同步学` |
| 输入框 | 自由文本 → 规则意图 → Tool → 可选 LLM 润色 |
| 结果区 | replies + cards + actions（navigate 深链） |

### 3.3 「该刷哪 / 继续练」

1. Tool：`availability` + `overview`（或 `today`）  
2. 卡片展示：今日新学/复习完成度、阶段、建议动作  
3. action：深链 `pages/exam/syncHome` 或直接 `syncExam`（带 `examId`）  
4. **不在弹窗内拉题作答**

### 3.4 「讲题」

| 优先级 | 来源 | 条件 |
|--------|------|------|
| 1 | 官方解析 | 当前题有解析则展示 |
| 2 | 同步学讲解 | `ReviewLlmAssist` / 已有 explain；可降级预设 |
| 3 | WeKnora | 调 `/appcoach` 或内嵌同客户端；需 KB 已配置 |

题目上下文（P0）：

- 若从同步学答题页打开弹窗（后期同组件复用）：页面注入 `questionId` / 题干摘要。  
- P0 仅在「我的班级」：无当前题时，讲题 chip 提示「请在同步练习作答页打开助教问本课」或允许输入题号（可选，不强制）。  
- **粘贴 / 拍照：明确后置。**

---

## 四、exam 匹配与降级

```text
classId
  → train_info_class.train_info_id
  → train_exam_list（同 train_id；优先工种/等级与班级一致）
  → 学员 platform_user_id × exam_id → 同步学计划 / availability
```

| 情况 | 行为 |
|------|------|
| 唯一匹配 exam + 在灰度/有资格 | 摘要卡正常；展示「该刷哪」「继续练」 |
| 多个 exam | P0：按班级工种/等级择优 1 个；歧义大时在卡上展示所选标题，允许「换计划」为 P1 |
| 无 exam / 无资格 / 灰度外 | 隐藏「该刷哪」「继续练」；文案引导；讲题仍可用（有题上下文时） |
| 无 platform_user_id | 提示账号未开通，不编造进度 |

---

## 五、技术设计

### 5.1 API（建议）

**方案甲（推荐，改动量小）：** 扩展现有

```text
GET  /training/follow/classes/{classId}/assistant/bootstrap
POST /training/follow/classes/{classId}/assistant/chat
```

- `bootstrap` 增加：`examId`、`syncSummary`、`chips`（按 D10 裁剪）、`welcome` 改为学员学习助教文案。  
- `chat` 增加意图：`go_sync` / `sync_today` / `explain`；老师专用意图对学员直接拒绝。  
- 可选 body：`questionId`、`examId`、`text`、`intent`。

**方案乙：** 新前缀 `/training/follow/classes/{classId}/tutor/**`，旧 assistant 标记废弃。  
P0 用甲即可，减少 App 双轨。

### 5.2 处理流水线

```text
POST .../assistant/chat
  → 鉴权：本班学员角色
  → 解析 examId（body 或自动匹配）
  → 意图（规则；chips 直达）
  → Tool（sync overview/today/progress | 解析 | review explain | appcoach）
  → 可选 FaqLlmClient 润色（禁止改写数字与事实字段）
  → { replies, cards, actions }
```

模型配置建议：`ai_student_tutor_model_code`（可与 FAQ 共用一条配置，单独 code 便于限流）。

### 5.3 会话（P0 建议做轻量）

| 项 | P0 | 说明 |
|----|----|------|
| 会话落库 | 建议做 | `train_class_assistant_session` / `_message`（可复用原助理表设计） |
| 维度 | `classId + platformUserId`（+ 可选 examId） | 租户隔离 |
| 无表降级 | 允许 | 仅当排期紧：端侧只保留当次弹窗消息 |

### 5.4 App 改造清单（`gjjd_appnext`）

| 项 | 说明 |
|----|------|
| 删除五角色台「AI 班级助理」按钮 | `roles/*.vue` |
| `myClasses` / `workspace` 学员态加 FAB | 仅 `primaryRole=student`（或 roleCodes 含 student） |
| 弹窗组件 | 摘要卡 + chips + 消息列表 + 输入；调 bootstrap/chat |
| 深链 | 同步学 `syncHome` / `syncExam` |
| （P1）syncExam 注入当前题 | 打开同一弹窗组件并带 `questionId` |

### 5.5 后端改造清单（`platform-api`）

| 项 | 说明 |
|----|------|
| `ClassAssistantApplicationService` | 学员芯片重做；接 sync Tool；explain 链；拒老师意图 |
| exam 解析工具方法 | 与 `TrainFollowMetricsApplicationService` 找 exam 逻辑对齐 |
| `FaqLlmClient` 润色 | 开关可关；超时回退 |
| WeKnora | 仅 explain 第三档；KB 未配则跳过并提示 |
| （建议）Flyway 会话表 | 见上 |

---

## 六、分期与验收

| 阶段 | 交付 | 验收 |
|------|------|------|
| **P0** | 删旧入口；班级 FAB 弹窗；bootstrap 摘要；该刷哪/继续练深链同步学；讲题 **有解析必出 / 无则明确无法讲**；LLM 润色可关（默认关）；无计划隐藏推荐 chips | 学员可从班级打开弹窗；有计划能跳到今日练；无计划不出现假进度；数字与 sync overview 一致；**O3-P0 验收档**通过（完整三档讲题与润色接线见优化方案 P1） |
| **P1** | 同步学答题页唤起弹窗 + 当前题讲题；粘贴题干；会话落库完善；多 exam 切换 | 做题中可问「这题怎么讲」且优先官方解析 |
| **P2** | 拍照讲题（WeKnora/vision）；入口迁提升可选 | 与陪练能力对齐 |
| **P3** | 老师侧能力（若需要）另开需求 | 不在本 P0 |

### 6.1 P0 验收场景

1. 学员进班 → 见 FAB → 打开有同步学摘要（或无计划说明）。  
2. 点「继续练」→ 进入同步学对应页，`examId` 正确。  
3. 点「该刷哪」→ 文案与 overview/today 一致，并带深链。  
4. 无同步学资格 → 无「该刷哪/继续练」，有引导，不报错。  
5. 讲题（有解析的题）→ 先出官方解析；无则同步学讲解；再无则 WeKnora 或明确「暂无法讲解」。  
6. 关闭 LLM 配置 → 规则回复仍正确。  
7. 所有角色台不再出现「AI 班级助理」；非学员不见 FAB。

---

## 七、风险与红线

| 风险 | 应对 |
|------|------|
| 班级无 class 维同步学 FK | exam 间接匹配；失败降级隐藏推荐 |
| 灰度/资格导致「有班无练」 | 文案说清，不显示 0% 假数据 |
| WeKnora 未配 KB | 第三档跳过，不阻塞 P0 |
| 与旧助理行为差异 | 产品已定替换；发布说明一句 |
| LLM 编造进度 | 润色 prompt 禁止改数字；结构化字段不经模型 |

**红线：** 不混 A/B/C 答卷；不替代认定判分；不静默写作业/改课表；学员不可看他人学情。

---

## 八、开放问题（不阻塞 P0）

| # | 问题 | 默认 |
|---|------|------|
| O1 | 多 exam 歧义时是否 P0 就做切换器 | 否，择优 1 个 |
| O2 | 班级页无当前题时讲题 chip 是否隐藏 | 显示但引导去同步学页（P1 注入题后再强化） |
| O3 | 会话表是否必须进 P0 | 建议做；排期不够可端侧降级 |
| O4 | 弹窗组件是否同时干掉全屏 `assistant` 路由 | 可保留路由重定向到提示「请用悬浮窗」，避免旧链接 404 |

---

## 九、现网对照（2026-08-25 核对）

> 对照 `platform-api` + `gjjd_appnext` 实码；用于修正「可复用」表述与实施顺序。

### 9.1 分块评级

| 方案块 | 评级 | 现网事实 |
|--------|------|----------|
| 删旧入口 | Ready | `roles/{student,monitor,leadTeacher,headTeacher,followStaff}.vue` 均有「AI 班级助理」 |
| FAB 弹窗 | Partial | 宜挂 `workspace.vue`（学员）；`myClasses` 为列表页，不适合作主 FAB |
| bootstrap 同步摘要 | Partial | `/appreviewsync/overview|availability` 可用，但必须先有 `examId` |
| class→exam 匹配 | **Missing** | `TrainFollowMetricsApplicationService` 仅按 `train_info_id` **聚合**考试通过率，无工种/等级择优器 |
| 讲题链 | Partial | 官方解析字段有；同步学 **无** `explain` API；WeKnora 要 `trade/chapter` |
| LLM 润色 | Ready | `FaqLlmClient` 可直接用 |
| 会话表 | **Missing** | 无 Flyway；现服务注释写明「不落会话表」 |

### 9.2 必须修正的假设

| 方案原文 | 现网 | 修正 |
|----------|------|------|
| 「该刷哪」接同步学 | 现 `go_precise` → `preciseHome`（通道 A） | 改为 `go_sync` / `sync_today` |
| `ReviewLlmAssist` 可讲解 | 仅鼓励语；`/appreviewsync/explain` **未落地** | P0：解析 →（补 explain 或跳过）→ WeKnora |
| 深链 `examId` | App 用 `exam_id` + `item` JSON | actions 按 `syncHome`/`syncExam` 现约编码 |
| `wrong/resolve`≈解析 | `/appsmart/wrong/resolve` = 答对移出错题本 | 讲题用 `PhpQuestionDetail.resolve` 字段 |
| 会话表可复用 | 表不存在 | P0 端侧消息；表可选后补 |

### 9.3 通道隔离

助教只深链同步学、只读讲题、不写 A/B 答卷 → **与现网隔离一致**。勿把 A 覆盖率塞进同步学摘要卡。

### 9.4 校正后实施顺序

1. **class→exam matcher**（`workTypeId`/`level` + `availability`）— 阻塞 bootstrap  
2. 删旧入口 + `workspace` 学员 FAB + 抽弹窗（复用 `assistant.vue`）  
3. 扩展 bootstrap/chat：sync 摘要 + `go_sync` 深链（`exam_id`）  
4. 讲题档1：有 `questionId` 时内读官方解析；班级页无题则引导  
5. 补同步学 explain **或** 直接上 WeKnora 第三档  
6. `FaqLlmClient` 润色（可关）  
7. 会话 Flyway（可选）

---

## 十、结论

方案**产品方向正确、可落地**；但文档中多处「现成可复用」偏乐观。按 §九 修正后实施：先做 **exam 匹配器**，再改入口与 sync 编排，讲题链不要假设已有 `explain` API。P0 仍建议：**班级学员 FAB + 同步学推荐 + 解析/WeKnora 讲题 + 删旧助理入口**。
