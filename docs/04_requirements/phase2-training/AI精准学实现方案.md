# AI 精准学 — 实现方案

> 目标项目：`qianlvguoyu_saas`（`platform-api` + `admin-web`）+ App：`gjjd_appnext`  
> 关联文档：  
> - [App同步学刷题作业_实现方案.md](./App同步学刷题作业_实现方案.md)（通道 A/B/C 隔离）  
> - [AI同步学（学员复习）实现方案.md](../AI同步学（学员复习）实现方案.md)（通道 C 引擎）  
> - [跟班_班级同步学配置_实现方案.md](./跟班_班级同步学配置_实现方案.md)（班级 ↔ exam 绑定）  
> - [班级刷题学情方案.md](./班级刷题学情方案.md) / [学员刷题已做未做覆盖_实现方案.md](./学员刷题已做未做覆盖_实现方案.md)（通道 A）  
> - [学员AI助教实现方案.md](./学员AI助教实现方案.md)（编排层；「该刷哪」主推 C）  
> 状态：**P0 已冻结（访谈 2026-08-31）**；待开发（本文不改引擎核心）  
> 编写日期：2026-08-31  
> 修订：同日一问一答冻结首屏/路由/API/无 C 行为  

---

## 〇、一句话

**AI 精准学 = 学员侧 Hub（编排层）**：首屏主做 **薄弱强化（A/C 分列带来源）**；点「去练」按 **有同步学任务→C，否则 A**；诊断双卡放列表下方；**同步练复用 C、专项练复用 A**；答卷与指标永不混库。

---

## 一、已冻结决策（P0）

| # | 项 | 结论 |
|---|----|------|
| D1 | 首期用户 | **仅学员**（App Hub）；教务看板后置 |
| D2 | 首屏主价值 | **薄弱强化**（哪里弱 → 立刻去练） |
| D3 | 点薄弱默认路由 | **有同步学可练任务 → 通道 C；否则 → 通道 A**（系统判定，少弹选择） |
| D4 | 薄弱数据源 | **A + C 都列**，每条标明 `source=sync\|special`；**不合成一个分** |
| D5 | 入口 | **保留独立「AI 精准学」Hub**；与「同步练习」「班级刷题」直达并存 |
| D6 | 无同步学时 | **整块隐藏 C**（不展示开通空态）；只留专项薄弱与去专项练 |
| D7 | 「今日路径」区块 | **不做**；路径感落在每条薄弱的「去练」 |
| D8 | 能力诊断双卡 | **要做**，放在 **薄弱列表下方** |
| D9 | Dashboard API | **`GET /training/follow/classes/{classId}/precise/dashboard`** |

### 1.1 非目标（P0）

| 不做 | 原因 |
|------|------|
| 教务/跟班「精准学概况」必做 | D1 |
| 单独「今日路径」模块 | D7 |
| 无 C 时引导开通文案区 | D6（整块隐藏） |
| 新建排题调度器 / 混 A·B·C 答卷 | 通道隔离 |
| 跨通道加权「综合能力分」 | 不作认定依据 |
| 数字人 / 语音 | 另立 |

---

## 二、产品定义与能力映射

### 2.1 四项能力 ↔ 现网资产

| 产品能力 | 现网真源 | 精准学怎么用（冻结后） |
|----------|----------|------------------------|
| **学员能力诊断** | C：`progress` / `overview`；A：`knowledge/predict` + `wrong/book` | Hub **下方双卡**并排，不合成总分 |
| **个性化学习路径** | C：三阶段 + Scheduler（勿重做） | **不单独展示路径卡**；由薄弱「去练」路由体现（优先 C） |
| **薄弱知识点强化** | C 错题卡；A 预测 + wrong-replay/generate | **首屏主列表**；点项按 D3 路由 |
| **同步练习** | `/appreviewsync` + `train_class_review_sync` | Hub 次级入口 + 薄弱去 C |
| **专项练习** | practice-scope + `/appsmart` + `classPractice*` | Hub 次级入口 + 薄弱去 A |

### 2.2 命名对齐

| 称谓 | 含义 |
|------|------|
| **AI 精准学** | 产品总称 + App Hub，**不是**第四套答卷引擎 |
| **通道 A** | 专项 / 班级刷题 |
| **通道 B** | 作业 / 模拟考（Hub 不吞并） |
| **通道 C** | 同步学 |
| App「通道 E」 | 历史壳标签；**废弃作答卷域**，仅指 Hub 页 |

---

## 三、结论（先看）

| 问题 | 答案 |
|------|------|
| 中枢页第一眼？ | **薄弱列表 + 去练** |
| 去练写哪套答卷？ | 进 C 写 `ai_review_*`；进 A 写 `exam_*`；禁止双写 |
| 无同步学？ | 隐藏一切 C UI；只做专项薄弱 |
| 诊断放哪？ | 薄弱下方完整双卡 |
| API？ | 跟班前缀 dashboard，服务内聚合 |
| 引擎改不改？ | **不改** C Scheduler / A 组卷核心 |

**一句话：**  
Hub 主打薄弱行动 → 路由 C 或 A → 双卡辅诊 → 不混库。

---

## 四、现状与缺口

### 4.1 可复用

| 层 | 资产 |
|----|------|
| C | `ReviewPlanApplicationService`、`/appreviewsync/**`、`ClassReviewSyncApplicationService`、`goReviewSync.js` |
| A | `AppExamSmartService`、practice-scope、`classPractice*` |
| Hub 壳 | `preciseHome.vue`、`POST /appsmart/precise/overview`（可兼容保留） |

### 4.2 P0 必须补

| 缺口 | 动作 |
|------|------|
| 错题重练死弹窗 | 打通 `classPracticeExam` |
| Hub 未读 review-sync / C 薄弱 | dashboard 聚合 + 列表 `source=sync` |
| 无统一薄弱行动路由 | 服务端给出 `action.channel` + 深链参数 |
| 无 follow dashboard | 新增薄 Service + Controller |

---

## 五、目标架构

```text
┌──────────────────────────────────────────────┐
│  AI 精准学 Hub（preciseHome）                  │
│  ① 薄弱列表（主，含去练）                        │
│  ② 诊断双卡（次）                               │
│  ③ 同步练习 / 专项练习入口（次；无 C 则无同步入口） │
└───────────┬──────────────────┬───────────────┘
            │                  │
     ┌──────▼──────┐    ┌──────▼──────┐
     │ 通道 C 同步学 │    │ 通道 A 专项  │
     └─────────────┘    └─────────────┘
```

### 5.1 原则

| 原则 | 说明 |
|------|------|
| 编排不吞并 | 不新建答卷表 |
| 双源标注 | 列表与双卡均带来源；禁止合成认定分 |
| 路径 = 路由 | 不靠独立「今日路径」模块 |
| 无 C 静默降级 | 隐藏 C，不打断专项 |

---

## 六、学员信息架构

### 6.1 入口

| 入口 | 行为 |
|------|------|
| 我的班级 → **AI 精准学** | Hub（必带 `classId`） |
| 我的班级 → 同步练习 / 班级刷题 | **保留直达** |
| 提升 → 同步练习 | 保留 |
| 学员 AI 助教 | 可深链 Hub 或 C（与助教方案对齐） |

### 6.2 Hub 首屏顺序（冻结）

| 顺序 | 区块 | 说明 |
|------|------|------|
| 1 | **薄弱强化列表** | 项含：名称、来源标签、弱程度摘要、「去练」 |
| 2 | **能力诊断双卡** | 上有 C 且可见才渲染 sync 卡；special 卡在 scope 可用时渲染 |
| 3 | **练习入口** | 同步练习（仅 `entryVisible`）；专项练习（scope 开启时） |

### 6.3 薄弱「去练」路由（D3）

```text
点击弱项 (knowledgeId, source)
  → 若 review-sync.entryVisible
       且（source=sync 或 今日 C 仍有可练任务）
       → 深链 syncHome / syncExam（exam_id 约定）
  → 否则
       → A：wrong-replay 或 generate（知识点 + classId + scope）
         → classPracticeExam（真实答题，禁止仅弹窗）
```

「今日 C 仍有可练任务」判定（实现约定）：`overview/today` 存在未完成的新学或复习量，或弱项来自 sync 源；细则实现时写进 Service 注释，避免前端猜。

### 6.4 无同步学（D6）

| 表现 | 说明 |
|------|------|
| 不渲染 | sync 薄弱、sync 诊断卡、同步练习入口 |
| 仍渲染 | special 薄弱、专项诊断卡（若有数据）、专项入口 |
| 不展示 | 「请联系老师开通同步学」大空态（P1 若运营需要再加） |

---

## 七、能力设计细则

### 7.1 薄弱列表（主）

| 字段建议 | 说明 |
|----------|------|
| `knowledgeId` / `title` | 展示名 |
| `source` | `sync` \| `special` |
| `signal` | 如红卡数 / 错误率档 / mastery |
| `action` | `{ channel: 'sync'|'special', url|navPayload }` |

排序建议：先 sync 红卡/星标，再 special 薄弱档；P0 可简单按信号强度。

### 7.2 诊断双卡（次）

| 卡 | 字段 |
|----|------|
| 考前同步学 | 阶段、今日完成度、`estimatedScore`、错题卡摘要 |
| 本班专项 | 薄弱考点 TopN、错题本数量、（可选）覆盖提示 |

无 C：不渲染第一张卡。

### 7.3 个性化路径

- 引擎仍仅 C。  
- Hub **不展示**独立路径模块；路径通过「去练→C」完成。  
- 无 C 时不出现「考前计划」文案。

### 7.4 同步 / 专项

配置仍在跟班：`review-sync` + `practice-scope`；精准学不新增第三套配置。

---

## 八、API（冻结）

```text
GET /training/follow/classes/{classId}/precise/dashboard
Authorization: Bearer（方案 A）
→ ApiResponse<{
    weakItems: [{
      knowledgeId, title, source: 'sync'|'special',
      signal?, action: { channel, ...深链参数 }
    }],
    diagnosis: {
      sync: null | { examId, stage, estimatedScore, wrongStats, todayHint, ... },
      special: null | { wrongBookCount, weakKnowledges[], ... }
    },
    entries: {
      sync: null | { examId, entryVisible: true },
      special: null | { scopeEnabled: true }
    }
  }>
```

| 规则 | 说明 |
|------|------|
| 鉴权 | 本班学员（或本班成员只读本人数据） |
| 聚合 | 服务内调 `ClassReviewSyncApplicationService`、`ReviewPlanApplicationService`、`AppExamSmartService` 等 |
| `sync` 全 null | `entryVisible=false` 时 weakItems 不含 sync，diagnosis.sync=null，entries.sync=null |
| 兼容 | `POST /appsmart/precise/overview` 可保留，Hub 主读本接口 |

---

## 九、技术要点

### 9.1 后端

| 项 | 说明 |
|----|------|
| `PreciseLearnDashboardApplicationService`（名可调） | 薄编排 |
| Controller | `TrainFollowAppController` 增加 dashboard |
| 禁止 | 改 Scheduler；合并 wrong 表；返回跨通道综合分 |

### 9.2 App

| 项 | 说明 |
|----|------|
| `preciseHome.vue` | 按 §6.2 重排；接 follow dashboard |
| 去练 | 执行 `action`；A 必须进 `classPracticeExam` |
| `api/precise-learn.js` | 增加 follow dashboard 调用；废弃「通道 E」表述 |
| 深链 | C 用 `exam_id`/`item`；A 用现有 classPractice 参数 |

### 9.3 管理端

P0 无新页；P1 可选分源只读概况。

---

## 十、分期与验收

| 阶段 | 交付 | 验收 |
|------|------|------|
| **P0** | follow dashboard；Hub 薄弱主列表 + 下方双卡 + 入口；D3 路由；D6 隐藏 C；错题重练真答题 | 见下 |
| **P1** | predict 可选 class 过滤；助教与 Hub 同源；可选 explain/周报（仅 C） | 弱项更准；助教一致 |
| **P2** | 教务分源看板；专题包；入口扩展 | 隔离仍在 |

### 10.1 P0 验收场景

1. 有同步学开通 → 列表可见 sync 弱项；点去练进 C；下方有 sync 诊断卡。  
2. 无同步学 → **无任何 C UI**；仅 special 弱项与专项入口/卡。  
3. 点 special 弱项 → 进 A 真实答题并可交卷。  
4. **无**单独「今日路径」区块。  
5. 双卡在薄弱下方；A/C 数据分开展示、不混算。  
6. 「同步练习」「班级刷题」直达入口仍在。  

---

## 十一、红线

- 不混 A/B/C 答卷与主指标  
- 不替代认定判分  
- 编排层不写答卷  
- 无 C 时不编造同步学进度  

---

## 十二、开放问题（已收敛 / 残留）

| # | 问题 | 状态 |
|---|------|------|
| O1 | Dashboard 挂哪 | **已拍板：follow** |
| O2 | 无 C 路径文案 | **已拍板：隐藏 C，不强调开通** |
| O3 | 是否砍直达入口 | **已拍板：保留** |
| O4 | predict class 过滤 | P1；不阻塞 P0 |
| O5 | 「有可练 C 任务」精确判定 | 实现时在 Service 写死规则并单测 |

---

## 十三、结论

按冻结访谈：精准学 Hub **以薄弱强化为主**，诊断为辅，路径不单列；练习仍分流 **C 同步 / A 专项**；无同步学则静默只做专项。落地以 **`precise/dashboard` + Hub 改版 + 真答题深链** 为主，**不动排题引擎**。
