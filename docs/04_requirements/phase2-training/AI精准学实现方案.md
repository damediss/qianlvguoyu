# AI 精准学 — 实现方案

> 目标项目：`qianlvguoyu_saas`（`platform-api` + `admin-web`）+ App：`gjjd_appnext`  
> 关联文档：  
> - [App同步学刷题作业_实现方案.md](./App同步学刷题作业_实现方案.md)（通道 A/B/C 隔离）  
> - [AI同步学（学员复习）实现方案.md](../AI同步学（学员复习）实现方案.md)（通道 C 引擎）  
> - [跟班_班级同步学配置_实现方案.md](./跟班_班级同步学配置_实现方案.md)（班级 ↔ exam 绑定）  
> - [班级刷题学情方案.md](./班级刷题学情方案.md) / [学员刷题已做未做覆盖_实现方案.md](./学员刷题已做未做覆盖_实现方案.md)（通道 A）  
> - [学员AI助教实现方案.md](./学员AI助教实现方案.md)（编排层；「该刷哪」主推 C）  
> 状态：**P0 已落地**；**P1 Step A–E 代码已落地**（弱项加权抽题 / 解析错因首行 / 练后效果 / Hub 优先强化）；访谈冻结 2026-08-31  
> 编写日期：2026-08-31  
> 修订：同日一问一答冻结首屏/路由/API/无 C 行为；2026-09-01 落地 follow dashboard 与 preciseHome；同日 P1 提通过率顺序实现 

---

## 〇、一句话

**AI 精准学 = 学员侧 Hub（编排层）**：**页内只保留一个答题入口**——根据学员错题推送同考点+同题型练习（通道 A `similar-wrong`）；薄弱考点仅展示来源，不另开同步学/专项/班级刷题入口；答卷与指标永不混库。

---

## 一、已冻结决策（P0）

| # | 项 | 结论 |
|---|----|------|
| D1 | 首期用户 | **仅学员**（App Hub）；教务看板后置 |
| D2 | 首屏主价值 | **薄弱强化**（哪里弱 → 立刻去练） |
| D3 | 唯一答题路由 | **仅通道 A 同型推题**（`similar-wrong` → `classPracticeExam` instant）；页内**不进** syncExam / 班级刷题 |
| D4 | 薄弱数据源 | **A + C 都可展示**，标明 `source=sync\|special`；列表**只读**，不作多入口 |
| D5 | 入口 | **独立「AI 精准学」Hub**；同步练习 / 班级刷题仍可从「我的班级」直达，**不挂在精准学页内** |
| D6 | 无同步学时 | 不展示 C 来源薄弱即可；不影响唯一推题入口 |
| D7 | 「今日路径」区块 | **不做** |
| D8 | 能力诊断双卡 | **P0 不做在精准学页**（学情仍可由班级/同步学页查看） |
| D9 | Dashboard API | **`GET /training/follow/classes/{classId}/precise/dashboard`**（薄弱列表供展示；开练只调 similar-wrong） |

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
| 中枢页第一眼？ | **一个「开始练习」+ 薄弱只读列表** |
| 去练写哪套答卷？ | 仅通道 A `exam_*`（similar-wrong → classPracticeExam） |
| 无同步学？ | 列表可不含 C 来源；推题入口不变 |
| 诊断放哪？ | 不在精准学页做双卡入口 |
| API？ | dashboard 展示薄弱；开练 `similar-wrong` |
| 引擎改不改？ | **不改** C Scheduler / A 组卷核心 |

**一句话：**  
Hub 只开一个同型推题入口 → 写 A → 不混库。

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
│  ① 唯一 CTA：按错题开始练习（similar-wrong）     │
│  ② 错题情况统计（同步卡 + 专项错题汇总）         │
└─────────────────────┬────────────────────────┘
                      │
               ┌──────▼──────┐
               │ 通道 A 专项  │
               │ classPracticeExam │
               └─────────────┘
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
| 1 | **唯一「开始练习」** | 按本班错题综合推同型题；无错题则空态提示，不跳转其它练题页 |
| 2 | **错题情况统计** | 同步学卡（红/黄/星）+ 专项错题本/错误率/薄弱考点数；**不再列逐条考点** |

### 6.3 唯一答题路由

```text
开始练习
  → POST /appsmart/paper/similar-wrong（classId；不按单点收窄，综合错题桶）
  → classPracticeExam（instant，答错出解析）
  → 禁止页内进 syncExam / classPractice
```

产品口径：根据**错题** → 推送**同考点+同题型** → 开练 → **答错即时解析**。  
API：`POST /appsmart/paper/similar-wrong`（mode=`drill`）。  
解析：`POST /appsmart/question/resolve`（先库后 AI 回写）。

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
| **P0** | Hub 单入口 + similar-wrong + 错题分项统计（题型/知识点/近期错题） | 能按错题开练、答错看解析、看清弱在哪 |
| **P1** | **提通过率主迭代**（见 §10.2）：弱项加权抽题 + 练后效果回写 + 解析可用 | 顽固弱项优先被练到；周错误率可降 |
| **P2** | 考前冲刺包、教务分源看板、覆盖空洞补练 | 考前按考纲比例；班级可盯通过率 |

### 10.1 P0 验收（已落地口径）

1. 页内仅「开始练习」→ `similar-wrong` → `classPracticeExam` instant。  
2. 错题情况含题型错误率、未掌握知识点、同步待消卡、近期错题。  
3. 不混 A/C 答卷；同步学/班级刷题不挂在精准学页内。  

### 10.2 P1 怎么做（提通过率落地清单）

目标一句话：**测准顽固弱项 → 只推该练的题 → 练完能量化变好。**

#### Step A — 弱项打分（改 `similar-wrong` 选种，不动新引擎）

在 `AppExamSmartService.similarWrongPractice` 内，对错题种子/考点桶加权，再按权重抽同型题：

| 信号 | 权重建议 | 数据来源 |
|------|----------|----------|
| 错题本仍在本（最近一次仍错） | 高 | `findWrongQuestionIds` |
| 同考点错误率 ≥60%（薄弱） | 高 | `wrongStatsByKnowledge` |
| 同步学红卡/星标对应考点 | 高 | `ai_review_wrong_item`（只作加权，答卷仍写 A） |
| 题型错误率明显高于学员均值 | 中 | `wrongStatsByQuestionType` |
| 班级 practice-scope 内 | 必须 | 现有 `applyPracticeScopeFilter` / pool |
| 偶发错、已出本 | 低/剔除 | 最新作答已对则不进种子 |

实现要点：
1. 先算 `knowledgeId|type` 桶优先级（Top 桶占本包 60%+ 题量）。  
2. 池内同型题按「未做过 > 做过仍错 > 做过已对」排序，避免无效重复。  
3. 每包固定题量（建议 15–20），前端 CTA 文案可写「今日强化包」。

验收：同一学员连续开练，优先出现高错误率考点/题型，而不是均匀乱抽。

#### Step B — 练后闭环（交卷回写效果）

| 项 | 做法 |
|----|------|
| 本包正确率 | `classPracticeExam` 交卷后返回 `right/total`，Hub `onShow` 拉 dashboard 刷新统计 |
| 考点进步 | dashboard `byKnowledge` 增加 `deltaWrongRate`（相对 7 日前，可先做「相对上次开练」） |
| 出本感知 | 答对出本后 `wrongBookCount` 下降，页脚提示「本周已消灭 x 道错题」 |

最小接口：dashboard 的 `wrongOverview` 增加：

```text
effect?: {
  weekWrongRate, weekWrongRateDelta,
  clearedWrongCount,   // 近 7 日出本数（可选，P1.1）
  lastPackAccuracy     // 最近一次 drill 正确率（纸卷 mode=drill）
}
```

验收：学员练完返回 Hub，能看到错误率/错题本数字变化，而不是「练了像没练」。

#### Step C — 解析可用（学会，而不是刷完）

| 项 | 做法 |
|----|------|
| 答错必出解析 | 保持 instant；无 `resolve` 走 `/appsmart/question/resolve` |
| 截断治理 | `resolve` 列过短则改 TEXT/或只存摘要+draft 全文 |
| 错因一句 | resolve 回写时要求首行「考点：…；易错：…」（提示词约束） |

验收：抽 20 道错题，解析非空率 ≥95%，手机端可读完。

#### Step D — Hub 展示对齐（不增答题入口）

在现有「错题情况」上增强，**仍只有一个开始练习**：

1. 「优先强化」三行：Top3 知识点（错误率）+ Top1 弱题型。  
2. 按钮副文案带本包策略：「本包侧重：安全意识培养 · 多选题」。  
3. 可选：练后条「上次正确率 70%」。

#### Step E — 度量（证明通过率在涨）

班级维度（P1 末或 P2 初，跟班只读即可）：

- 学员周错误率、薄弱知识点数、drill 包完成次数  
- 有模考/作业的班：模考分前后对比（通道 B，只读关联，不混库）

没有度量就不算「精准学做成了」。

### 10.3 建议排期（人天量级）

| 顺序 | 项 | 约 | 依赖 |
|------|----|----|------|
| 1 | Step A 弱项加权抽题 | 2–3d | 已有 similar-wrong / predict |
| 2 | Step C 解析非空与首行错因 | 1–2d | resolve 服务已有 |
| 3 | Step B + D 练后效果 + Hub 文案 | 2d | dashboard / 交卷 |
| 4 | Step E 周报指标（先学员后教务） | 2d | answer_log |

**先做 1→2→3**，就能体感「更准、更能学会」；4 用来对内证明提分。

### 10.4 明确不做（避免跑偏）

- 不新建第四套答卷库、不混写 A/C  
- 不在精准学页加同步学/班级刷题第二入口  
- 不靠「综合能力分」替代错题/错误率真源  
- 不重做 C Scheduler；同步进度仍归同步学  

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
| O3 | 是否砍直达入口 | **已拍板：保留（班级页直达；精准学页内不挂）** |
| O4 | predict / 抽题 class·scope 过滤 | **P1 Step A 一并做** |
| O5 | 「有可练 C 任务」精确判定 | 实现时在 Service 写死规则并单测 |
| O6 | 提通过率主路径 | **§10.2：加权抽题 → 解析 → 练后闭环 → 度量** |

---

## 十三、结论

P0：Hub **单入口同型推题 + 错题分项可见**。  
P1：在 `similar-wrong` 上做 **弱项加权**，保证 **解析学会**，交卷后 **效果回写**，用周错误率证明提分。  
全程 **编排不混库、不重做 C 引擎**。