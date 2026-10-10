# AI 同步学统计功能设计方案（培训机构管理者）

> 版本：v1.2　日期：2026-08-12
> 关联文档：《AI同步学（学员复习）实现方案》v1.5（学员侧功能）
> 需求方拍板记录（2026-08-11 第一轮）：① 数据范围=全部学员，不做机构隔离；② 列表 + 单个学员详情钻取；③ 错题趋势用近似口径（不建日级快照表）；④ 需要 Excel 导出
> 需求方拍板记录（2026-08-11 第二轮，入口与考试计划数据源）：⑤ `train_exam_list` 由 PHP 库**一次性导入** SaaS 库同名表（需求方知晓并接受"PHP 新建复习计划不会自动出现，需人肉重导"）；⑥ 同 id 冲突行**以 PHP 内容为准覆盖**；⑦ SaaS 考试管理页面对 **PHP 来源行只读**（编辑/删除/考生操作拦截）；⑧ 统计入口**仅放考试管理列表页行内按钮**，不开全局菜单
> 需求方拍板记录（2026-08-12 第三轮）：⑨ 错题清单题目详情数据源——**SaaS 库 `exam_exam_detail` 优先**（运维已将 PHP 题库同步，同 id），PHP 库仅作缺失回退
>
> 修订记录：
> - v1.1（2026-08-11）：按第二轮拍板改写——新增 §3「train_exam_list 一次性导入与只读约束」；统计数据源由"PHP 库实时查 train_exam_list"改为"SaaS 库本地表"；入口由"独立菜单"改为"考试管理列表页行内按钮"；权限收敛为按钮级权限码，不新增可见菜单
> - v1.2（2026-08-12，实施落地后修正）：**权限码挂接菜单修正**——`train_exam-list`(4904) 已 deleted=1，V20260811_008 按其查找插入 0 行导致按钮不显示，V20260812_002 改挂生效菜单 `kao_shi_ji_hua`（§5.4）；**错题清单升级**——题目/选项/正确答案/学员答案完整展示，数据源改 SaaS 本地 `exam_exam_detail` 优先（拍板⑨，§2.1/§5.3/§6.3）；图表 legend/网格重叠修正（§6.2/§6.3）
> - v1.3（2026-08-27）：**单库化改造实施完成（指针式修订）**——gjjddb+gjjddb2026 已合并为 gjjddbpro（租户统一 10159），本文 §2/§3/§5 中"PHP 库回退查题目详情""train_exam_list 一次性导入 + PHP 来源行只读"等双库口径全部废止；现行口径以《AI同步学功能单库化改造方案_gjjddbpro》v1.2 为准（统计单库直查、题目详情缺失降级显示题号、考试计划全量可编辑）

## 1. 背景与目标

培训机构管理者需要回答四个问题：

1. **每天的使用情况**：AI 同步练习每天有多少学员在用、练了多少题、完成率如何；
2. **学员练题情况**：每个学员练了多少题、正确率如何；
3. **错题情况**：错题存量分布（红/黄/绿/加星）、每日新增与消灭趋势；
4. **知识掌握与学习趋势**：各考核项目/知识点掌握度，学员个人学习曲线。

产物：管理端 2 个页面（统计总览 + 学员详情，**入口为「培训管理 > 考试管理」列表页行内按钮**，见 §6.1）+ 7 个统计 API + 1 个 Excel 导出。

## 2. 数据来源盘点（已核实）

### 2.1 SaaS 库 `gjjddb2026`（AI 同步学业务数据，全部已存在，无需新建业务表）

| 表 | 角色 | 支撑的统计指标 | 满足度 |
|---|---|---|---|
| `ai_review_plan` | 计划主表 | 计划学员数、阶段分布（current_stage 1/2/3）、状态分布（active/completed/abandoned）、中断次数、必做池题数 | ✅ |
| `ai_review_plan_day` | 每日任务 | **每日使用情况的天然聚合源**：按 `plan_date` 聚合活跃计划数、作答数（answer_count）、答对数（correct_count）、完成数（new_done+review_done）、任务状态 | ✅ |
| `ai_review_answer` | 作答流水 | 练题量、正确率、题型维度、**知识点维度**（knowledge_id）、用时 | ✅ |
| `ai_review_wrong_item` | 错题本 | 牌级分布（card_level）、加星顽固错题、累计对错；`first_wrong_time`/`remove_time` 支撑新增/消灭趋势（近似口径） | ✅ 快照 + 近似趋势 |
| `ai_review_mock_paper` | 模考卷 | 模考次数、得分（score）、及格率 | ✅ |
| `platform_user`（PHP 镜像表） | 学员信息 | realname / idcard / phone / org_id | ✅ |
| `train_exam_list`（PHP 一次性导入，见 §3） | 考试计划 | 统计页计划下拉/标题、列表页行内入口载体 | ✅ 导入后本地读 |
| `exam_exam_detail`（PHP 题库同步，同 id，32 万+ 行） | 题目详情 | 错题清单的题型（type）/题目（content）/选项与正确答案（answer JSON）/解析（resolve） | ✅ 本地优先读（拍板⑨） |

### 2.2 PHP 库 `gjjddb`（只读第二数据源，复用 `PhpDataSourceHolder`，仅 SELECT）

| 表 | 用途 |
|---|---|
| `exam_knowledge_structure` | 知识树标题与层级（level 0~3，parent_id），掌握度按 L1 聚合 |
| `train_exam_work_type_list` | 工种/等级名称（exam_id → work_type_id、level） |
| `exam_exam_detail` | **仅作回退**：SaaS 本地缺失的题号（运维尚未同步的新题）才实时查 PHP 侧 |
| ~~`train_exam_list`~~ | ~~考试计划下拉~~ **已改为一次性导入 SaaS 库（§3），统计功能不再实时查 PHP 侧该表** |

> PHP 库访问沿用 `PhpDataSourceHolder` 持有器模式（AGENTS.md 坑 #19/#20：不注册 DataSource Bean、主 JdbcTemplate 已 @Primary）；JDBC URL 已带 `tinyInt1isBit=false`（坑 #23）。

### 2.3 结论

**现有表完全满足需求，不新建业务表**。两个已知限制：

1. **错题历史趋势为近似口径**：按 `first_wrong_time`（每日新增错题）与 `remove_time`（每日消灭/移出错题）聚合，无法精确还原"历史上某一天的在册红牌数"。若未来需要精确曲线，再建 `ai_review_wrong_snapshot` 日快照表（二期，本方案预留扩展位）。
2. **直查实时聚合**：当前作答仅数百行，实时 GROUP BY 毫秒级。数据量到百万级后再上预聚合报表（二期，`review_sync_stats_daily` 预聚合表 + 每日定时任务）。

## 3. train_exam_list 一次性导入与只读约束（第二轮拍板⑤⑥⑦）

### 3.1 现状核查（2026-08-11 实测）

| 事实 | 数据 |
|---|---|
| SaaS `train_exam_list` | 438 行（id 1~441，214 行有效，tenant_id 全为 1）——含历史一次性导入 + 本地创建 |
| PHP `train_exam_list` | 458 行（id 1~461，224 行有效） |
| 同 id 内容漂移 | 存在。如 `id=440`：SaaS="测试20260622"，PHP="2026年来宾银海铝铝电解槽操作工-技师" |
| SaaS 侧已过时 | AI 同步学引用的 12 个 exam_id 中 443/444/445/446/448/449/451/454 在 SaaS 表**不存在** |
| 关联数据 | SaaS `train_exam_people_list` 0 行（考生功能未实际使用）；`train_exam_review` 163 行 |

### 3.2 导入规则

- **一次性导入**（拍板⑤）：运维脚本 `infra/db/train-exam-list-import.sql`（或 mysqldump 管道），**全量含 `deleted=1` 行**（保持 id 全集一致，已删除的历史计划仍可能被旧统计数据引用；列表页查询侧已按 `deleted=0` 过滤）。
- **PHP 为准覆盖**（拍板⑥）：同 id 行整行覆盖为 PHP 内容（`INSERT ... ON DUPLICATE KEY UPDATE 列=VALUES(列)` 逐列覆盖；坑 #21：不用 `AS new` 行别名）。SaaS 有而 PHP 没有的 id（本地创建行）**保留不动**。
- **列差异映射**：SaaS 表比 PHP 多 `tenant_id`（NOT NULL，导入行**恒填 1**，否则多租户插件查不到）、`jigou_id`（填 NULL）、`create_time/update_time`（填 NOW()）、`sort_number`（沿用 SaaS 现值/默认 0）；类型差（int→bigint、tinyint→int）隐式兼容。
- **id 区间隔离**：导入完成后执行 `ALTER TABLE train_exam_list AUTO_INCREMENT = 100000;`——此后 SaaS 本地新建计划 id ≥ 100000，**id < 100000 即 PHP 来源行**（与鉴权方案 §3.5.3③ platform_user 同款区间隔离思路）。

### 3.3 PHP 来源行只读约束（拍板⑦）

- 后端：`TrainExamListApplicationService` 的 `update` / `delete` / 考生写操作（import-people、import-from-train、people-grades 更新、batch-delete）统一加 `requireSaaSLocalExam(examId)` 守卫——`examId < 100000` 抛业务异常「该考试计划来源于工匠基地平台，请前往原平台维护」（参照 `requireSaaSLocalUser` 既有模式）。
- 前端：`TrainExamListPage` 操作列对 PHP 来源行（id < 100000）置灰「编辑/删除/考生」按钮并 tooltip 说明；后端拦截为最终裁决。
- **已知接受的代价**：PHP 侧后续新建/修改复习计划**不会自动出现**在 SaaS 侧，需人肉重跑导入脚本（拍板⑤已确认接受）。重跑脚本幂等（同规则再覆盖一遍），无脏数据风险。

## 4. 指标口径定义（验收以此为准）

| 指标 | 口径 | 来源 |
|---|---|---|
| 计划学员数 | `COUNT(DISTINCT platform_user_id)`，status != abandoned 由筛选决定 | ai_review_plan |
| 每日活跃学员 | 当日有作答的 `COUNT(DISTINCT platform_user_id)`（按 answer.created_at 日期） | ai_review_answer |
| 每日作答数 | `COUNT(*)`（含改答案重答，如实反映练习强度） | ai_review_answer |
| 每日完成题数 | `SUM(new_done + review_done)`（任务完成口径，不含重复作答） | ai_review_plan_day |
| 每日正确率 | `SUM(is_correct) / COUNT(*)` | ai_review_answer |
| 任务完成率 | 当日 `status='done'` 的天任务数 / 当日应做任务数（plan_date 当天且 status != skipped） | ai_review_plan_day |
| 学员累计练题 | 该学员 answer 总行数（可拆分 new/review/mock 三轮次） | ai_review_answer |
| 学员正确率 | `SUM(is_correct)/COUNT(*)` | ai_review_answer |
| 错题牌级分布 | 当前快照：`removed=0` 按 card_level 分组计数；加星 = star=1 且 removed=0 | ai_review_wrong_item |
| 每日新增错题 | `COUNT(*)` 按 `DATE(first_wrong_time)` | ai_review_wrong_item |
| 每日消灭错题 | `COUNT(*)` 按 `DATE(remove_time)`（removed=1） | ai_review_wrong_item |
| 知识点掌握度 | 某知识点（L3）正确率 = 该 knowledge_id 下 `SUM(is_correct)/COUNT(*)`；L1 考核项目掌握度 = 其下所有 L3 的作答加权正确率 | ai_review_answer ⨝ exam_knowledge_structure |
| 知识点覆盖率 | 已作答过的 L3 知识点数 / 该计划必做池涉及的 L3 知识点数（must_do_ids 快照解析） | ai_review_answer ⨝ ai_review_plan |
| 模考成绩 | `status='done'` 的 score / total_score、及格 = score >= pass_score | ai_review_mock_paper |

> 所有 SQL 走 MyBatis-Plus 多租户插件自动拼 `tenant_id`（SaaS 表），不在业务代码手写（AGENTS.md 规范）。PHP 库表无 tenant_id，直接查。

## 5. 后端设计

### 5.1 包与分层

```
exam/reviewsync/
  controller/ReviewSyncStatsController.java        // 管理端 /api 路径，@PreAuthorize 鉴权
  service/ReviewSyncStatsApplicationService.java   // 用例编排
  vo/ReviewSyncStatsVO.java                        // 响应对象（内部静态类）
```

- 管理端接口（非 App 接口），走 sys_user 权限体系，`@PreAuthorize("hasAuthority('exam:review-sync:stats:view')")`；导出单独权限码 `exam:review-sync:stats:export`。
- Controller 不直接写 SQL；统计 SQL 放 Mapper（`ReviewSyncStatsMapper`，@Select 注解或 XML），Entity 不暴露前端。
- 考试计划标题/下拉读 **SaaS 库本地 `train_exam_list`**（§3 一次性导入后）；知识树标题仍从 PHP 库 `exam_knowledge_structure` 只读查询，复用/扩展 `PhpExamQueryService`。

### 5.2 API 清单

| # | 方法与路径 | 说明 | 主要入参 |
|---|---|---|---|
| 1 | GET `/api/exam/review-sync/stats/overview` | 顶部概览卡片：计划学员数、今日活跃、累计/今日作答、整体正确率、在册错题四档计数 | from, to, examId? |
| 2 | GET `/api/exam/review-sync/stats/daily-trend` | 每日使用趋势：活跃学员数、作答数、正确率、任务完成率（按天数组） | from, to, examId? |
| 3 | GET `/api/exam/review-sync/stats/wrong-trend` | 错题：牌级分布快照 + 每日新增/消灭两个序列 | from, to, examId? |
| 4 | GET `/api/exam/review-sync/stats/knowledge-mastery` | L1 考核项目掌握度（正确率、作答数、覆盖率），可按学员过滤 | examId, platformUserId? |
| 5 | GET `/api/exam/review-sync/stats/students` | 学员明细分页表（统一 PageQuery/PageResponse） | page, size, examId?, keyword?（姓名/手机号）, sort? |
| 6 | GET `/api/exam/review-sync/stats/students/{planId}` | 学员详情：计划信息、个人每日趋势、知识点掌握（L1+L3）、**错题清单（含题型/题目/选项/正确答案/学员最近答案/最近对错）**、模考成绩 | planId |
| 7 | GET `/api/exam/review-sync/stats/students/export` | 学员明细 Excel 导出（同 #5 筛选，不分页，上限 5000 行） | 同 #5 |

响应统一 `{code, message, data}`；#5 分页结构 `{total, page, size, records}`。

### 5.3 关键 SQL 要点

```sql
-- 每日使用趋势（#2 主体）
SELECT DATE(created_at) AS day,
       COUNT(DISTINCT platform_user_id) AS activeUsers,
       COUNT(*) AS answers,
       SUM(is_correct) AS corrects
FROM ai_review_answer
WHERE created_at >= :from AND created_at < :to + 1
GROUP BY DATE(created_at);

-- 学员明细（#5 主体）：plan ⨝ answer 聚合 ⨝ wrong 聚合 ⨝ platform_user
SELECT p.id AS planId, p.platform_user_id, u.realname, u.phone,
       p.exam_id, p.current_stage, p.current_day_index, p.total_days, p.status,
       a.answers, a.corrects,
       w.redCnt, w.yellowCnt, w.starCnt,
       p.last_active_date
FROM ai_review_plan p
LEFT JOIN platform_user u ON u.id = p.platform_user_id AND u.deleted = 0
LEFT JOIN (SELECT plan_id, COUNT(*) answers, SUM(is_correct) corrects
           FROM ai_review_answer WHERE deleted=0 GROUP BY plan_id) a ON a.plan_id = p.id
LEFT JOIN (SELECT plan_id,
                  SUM(card_level='red' AND removed=0) redCnt,
                  SUM(card_level='yellow' AND removed=0) yellowCnt,
                  SUM(star=1 AND removed=0) starCnt
           FROM ai_review_wrong_item WHERE deleted=0 GROUP BY plan_id) w ON w.plan_id = p.id
ORDER BY p.last_active_date DESC;
```

- 知识点掌握（#4/#6）：`ai_review_answer` 按 knowledge_id 聚合 → PHP `exam_knowledge_structure` 查 parent 链聚合到 L1；知识树一次性加载到内存复用（AGENTS.md 坑 #18：避免逐条查库）。
- L3 覆盖率：解析 `ai_review_plan.must_do_ids` JSON 得必做池，join answer distinct question_id → knowledge_id 集合比对。
- 错题清单题目详情（#6，拍板⑨）：**SaaS 本地 `exam_exam_detail` 优先**（运维已同步 PHP 题库，同 id，`answer` 列即 `[{content,isCorrect,score}]` JSON）；本地缺失的 id 回退 PHP 库；两级都失败降级只显示题号。学员答案取该计划内每题最近一次作答（`plan_id + question_id IN (...)` 按 id 倒序取首条，含改错重答）。

### 5.4 权限与菜单（AGENTS.md 坑 #8 三步链，Flyway 迁移一并落地）

**仅列表页入口（拍板⑧），不新增可见菜单**：

1. 权限码：`exam:review-sync:stats:view`、`exam:review-sync:stats:export`，挂在既有「培训管理 > 考试」菜单（**生效菜单 code=`kao_shi_ji_hua`**，id=2067179901380399106）下作为**按钮级功能权限**；
   > ⚠️ 实施踩坑（2026-08-12）：初版迁移 V20260811_008 按 `code='train_exam-list'` 挂接，但该菜单（4904）已 `deleted=1`，INSERT 命中 0 行 → 前端按钮不显示。已由 **V20260812_002** 修复改挂 `kao_shi_ji_hua`。**教训：挂接菜单必须核实 `deleted=0` 的生效记录**；
2. `sys_role_permission` 授予平台管理员角色（role_id=2000）+ 已有 `train:exam-list:view` 权限的角色同步授予 view（export 仅管理员）；
3. 前端 `TrainExamListPage` 行内「AI同步学统计」按钮按 `exam:review-sync:stats:view` 控制显隐；导出按钮按 `exam:review-sync:stats:export` 控制显隐；
4. 统计页/学员详情页为**隐藏路由**（不进菜单树），仅能经列表页按钮跳转进入。

> 新迁移版本号前先查 `SELECT MAX(version) FROM flyway_schema_history`（坑 #7）。

### 5.5 防御性与边界

- Mapper 返回值 null 检查；聚合子查询无数据时 `COALESCE(...,0)`；
- 日期范围强制上限（from/to 跨度 ≤ 92 天），防全表扫；
- 导出行数上限 5000，超出提示先筛选；
- 知识树标题从 PHP 库读，连接失败时降级为只显示 id（统计主链路不挂）；考试计划标题、题目详情读 SaaS 本地表（题目详情本地缺失再回退 PHP，拍板⑨），跨库依赖最小化；
- 所有统计接口为 GET 只读，无写操作，不记操作日志（项目约定写操作才记）。

## 6. 前端页面设计

技术：ProTable + TanStack Query + ECharts（echarts-for-react，参照 `TrainingDashboardPage.tsx` 现有模式）。目录：

```
apps/admin-web/src/pages/exam/reviewSyncStats/
  StatsOverviewPage.tsx      // 统计总览（/training/exam-lists/:examId/review-sync-stats，隐藏路由）
  StudentDetailPage.tsx      // 学员详情（/training/exam-lists/:examId/review-sync-stats/student/:planId）
  api.ts                     // 接口封装与类型
```

### 6.1 入口：考试管理列表页行内按钮（拍板⑧）

`apps/admin-web/src/pages/training/TrainExamListPage.tsx` 操作列追加：

```
| 编辑 | 考生 | AI同步学统计 | 删除 |
              └── 仅当该行有 AI 复习数据时高亮；无数据也可进入（空态页）
```

- 点击跳 `/training/exam-lists/{examId}/review-sync-stats`，统计页以该计划为预选筛选（页内仍可切换其它计划——SaaS 本地 `train_exam_list` 下拉）；
- PHP 来源行（id < 100000，见 §3.3）：「编辑/删除/考生」置灰 + tooltip「来源于工匠基地平台，请前往原平台维护」；「AI同步学统计」**不受影响**（统计是只读功能）；
- 按钮显隐受权限码 `exam:review-sync:stats:view` 控制。

### 6.2 统计总览页布局

```
┌──────────────────────────────────────────────────────────────────┐
│ ← 返回考试管理 | 当前计划：百色皓海炭极生产工（高级）复习 [切换计划▾]  │
│ 筛选栏： [日期范围 近14天▾] [学员姓名/手机号____] [查询]              │
├──────────────────────────────────────────────────────────────────┤
│ 卡片×6： 计划学员  今日活跃  今日作答  累计作答  整体正确率  在册错题 │
├───────────────────────────────┬──────────────────────────────────┤
│ 图1 每日使用趋势               │ 图2 错题牌级分布(环图)+新增/消灭柱 │
│  折线:活跃学员/作答数/正确率   │                                  │
├───────────────────────────────┴──────────────────────────────────┤
│ 图3 考核项目掌握度（横向条形图，L1，按权重排序，<60% 红色警示）       │
├──────────────────────────────────────────────────────────────────┤
│ 学员明细表（ProTable，服务端分页）                      [导出 Excel] │
│ 学员|手机号|考试计划|阶段/进度|累计练题|正确率|红/黄/加星|最近活跃|详情│
└──────────────────────────────────────────────────────────────────┘
```

- **顶部计划头**：从列表页带入的 examId 为当前计划；「切换计划」下拉读 SaaS 本地 `train_exam_list`（deleted=0，按考试日期倒序），切换后整页数据联动；「返回考试管理」回列表页。
- **图1 每日使用趋势**：双 Y 轴——左轴柱状=作答数、折线=活跃学员数；右轴折线=正确率%。悬停 tooltip 显示任务完成率。
- **图2 错题**：左半环图=当前在册红/黄/绿/加星占比；右半堆叠柱=每日新增错题 vs 消灭错题（近似口径，见 §4）。
- **图3 掌握度**：横向条形图按 L1 考核项目展示正确率，<60% 标红、60~80% 标黄、≥80% 标绿。
- **学员明细表**：默认按最近活跃倒序；「阶段/进度」列显示 `阶段N · 第x/y天` 迷你进度条；操作列「详情」跳学员详情页。
- **图表布局约束（v1.2 实测修正）**：ECharts legend 必须显式定位（折线/柱状 `legend.top=0` + `grid.top≥44`；环图 `legend.bottom=0` + `radius≈['38%','60%']` + `center y≈42%`），y 轴不写 `name`（与刻度文字抢占区域导致重叠），改在右轴刻度加 `%` 后缀；小卡片图表高度 ≥190px。未按此约束会出现"legend/轴标题与图形重叠"（2026-08-12 实测缺陷）。

### 6.3 学员详情页布局

```
┌──────────────────────────────────────────────────────────────────┐
│ 学员头： 张三  138****0000 | 考试计划：铝电解工四级 | 阶段2 · 第8/20天 │
│ 进度条： 必做池 1200 题，已练 640（53%），正确率 78%                 │
├───────────────────────────────┬──────────────────────────────────┤
│ 个人每日趋势（折线：作答数/正确率）│ 错题牌级迷你环图                  │
├───────────────────────────────┴──────────────────────────────────┤
│ 知识点掌握度：L1 条形图 + L3 明细表（知识点|作答数|正确率|覆盖）      │
├──────────────────────────────────────────────────────────────────┤
│ 错题清单（点击行展开完整题目，拍板⑨/v1.2）：                        │
│  题号|题型|题目(摘要)|正确答案|学员最近答案|牌级|已错/已对|最近答错   │
│  展开行：完整题干(HTML) + 全部选项逐条标注 ✓正确答案/✗学员选择        │
│ 模考成绩（时间|得分/总分|正确数|是否及格）                            │
└──────────────────────────────────────────────────────────────────┘
```

- **错题清单数据源链（拍板⑨）**：题目详情 SaaS 本地 `exam_exam_detail` 优先 → 本地缺失回退 PHP 库 → 仍缺失降级只显示题号（行内提示"题目详情加载失败"，不阻塞页面）；
- 学员最近答案：多选为 JSON 数组字符串，前端解析逐条与选项比对；单选/判断为选项内容原文；
- 题干/选项可能含 HTML（题库富文本），折叠态用纯文本摘要，展开行渲染 HTML（管理端内部可信数据）。

### 6.4 交互与异常态

- 「切换计划」下拉：读 **SaaS 本地 `train_exam_list`**（deleted=0，按考试日期倒序）；PHP 新建计划未重导时不出现在下拉（拍板⑤已接受）；
- 全部图表空态：「所选范围内暂无 AI 同步练习数据」；
- 日期范围默认近 14 天，快捷选项 7/14/30 天；
- 导出按钮带 loading，后端 5000 行上限拦截时 toast 提示先缩小范围；
- 权限不足（403）按 admin-web 统一拦截处理，不自动跳转。

## 7. Excel 导出

- 复用项目现有 Excel 导出模式（参照 `chazheng` 模块）；
- 列：学员、手机号、身份证、考试计划、工种/等级、当前阶段、进度（第x/共y天）、累计练题、新题/复盘/模考分项、正确率、在册红牌、黄牌、加星数、最近活跃时间；
- 文件名：`AI同步学学员明细_yyyyMMdd_HHmmss.xlsx`；
- 流式写出（SXSSF），5000 行上限。

## 8. 性能与演进

| 阶段 | 策略 |
|---|---|
| 一期（本方案） | 直查业务表实时聚合；日期跨度 ≤92 天强制；知识树一次性加载内存复用；导出 5000 行上限；`train_exam_list` 一次性导入 + 人肉重导 |
| 二期（数据量触发） | `review_sync_stats_daily` 预聚合表 + 每日定时任务；错题日快照表 `ai_review_wrong_snapshot` 支撑精确在册曲线；机构隔离（platform_user.org_id）若开机构账号再加；`train_exam_list` 若嫌人肉重导频繁，可并入 PlatformSyncService 轮询同步 |

## 9. 验收清单

- [ ] 7 个 API 全部按 §4 口径返回，Postman/单元测试覆盖
- [ ] Mapper 聚合 SQL 单测（H2/Testcontainers）：每日趋势、学员明细、牌级分布、知识点掌握
- [ ] 空数据/跨租户数据不漏（tenant_id 隔离）/超 92 天范围拦截 均有测试
- [ ] `infra/db/train-exam-list-import.sql` 在测试库执行验证：同 id 行被 PHP 内容覆盖、SaaS 独有 id 保留、导入行 tenant_id=1、`AUTO_INCREMENT=100000` 生效（§3）
- [ ] PHP 来源行只读拦截：编辑/删除/考生写操作对 id<100000 抛业务异常，前端按钮置灰（§3.3）
- [ ] 权限码+角色授权迁移在空库可执行；**权限码挂在 `deleted=0` 的生效菜单（`kao_shi_ji_hua`）下**，无 `exam:review-sync:stats:view` 的角色看不到行内按钮（坑 #7/#8 + §5.4 踩坑）
- [ ] 列表页行内按钮跳转统计页并预选当前计划；切换计划联动；学员详情页钻取正确
- [ ] 全部图表 legend/轴刻度/图形无重叠（§6.2 布局约束）
- [ ] 错题清单完整展示题型/题目/正确答案/学员最近答案，展开行选项标注正确与学员选择；**SaaS 本地 `exam_exam_detail` 命中**（不依赖 PHP 库可达），本地缺失题号回退 PHP，两级失败降级显示题号不 500（§6.3）
- [ ] 导出 Excel 列齐全、5000 行上限生效
- [ ] PHP 库不可达时知识点标题降级（显示 id，不 500）
- [ ] `mvn compile` 0 错、`npm run tsc` 0 错
