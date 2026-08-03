# AI 同步学（学员复习）功能接口实现方案

> 版本：v1.0　日期：2026-08-03
> 需求来源：`docs/osta-standards/AI同步学（学员复习）需求方案.txt`
> 鉴权依据：`gjjd_appnext/docs/手机端调用SaaS鉴权方案.md`（v1.2，令牌透传）
> 涉及项目：`qianlvguoyu_saas`（SaaS/platform-api，本期主体）、`gjjd_appnext`（手机端）、`gjjd_phalcon`（PHP，零改动）

## 0. 已确认决策（需求方拍板，2026-08-03）

| # | 决策点 | 结论 |
|---|---|---|
| D1 | 手机端调 SaaS 鉴权 | **令牌透传**：手机端 `Authorization: Bearer <itoken>` + `X-Tenant-Id:1`，SaaS 侧 `AppTokenAuthFilter` 查 `platform_user_token` 验票；3 表（platform_user / platform_user_token / platform_user_detail）由同步任务 PHP→SaaS 轮询同步（3s），详见鉴权方案文档 |
| D2 | AI 角色定位 | **规则引擎为主 + LLM 辅助**：排题/题量/错题淘汰/中断补救全部 Java 规则实现；LLM（DeepSeek）只做鼓励语、顽固错题讲解、周报总结 |
| D3 | 第三阶段仿真模考 | **SaaS 自组卷模考**：从 PHP 题库按规则组卷，SaaS 判分 |
| D4 | 答题数据归属 | SaaS 新表为主记录，**异步回写 PHP 库 `exam_result`**（`exam_type=5` AI同步练习），失败补偿重试 |

## 1. 背景与目标

培训机构学员在手机端刷题备考职业技能等级认定考试。本功能在 SaaS 平台新建"AI 同步学"接口能力：学员进入"提升 → 复习任务 → 复习/考试"页面，点击顶部新增"同步练习"菜单后，由系统扮演专家老师角色，按"三阶段法"自动规划每日复习题（新题 + 错题复盘 + 模考），并执行错题三级淘汰与中断补救策略，目标是提高刷题积极性、考试成绩与通过率。

### 1.1 现状关键事实（2026-08-03 实测）

| 事实 | 影响 |
|---|---|
| PHP 库（`gjjddb` @ 114.132.67.185:3308）持有全部活数据：题库、复习计划、学员关联、答题历史、token | SaaS 必须读 PHP 库 |
| SaaS 库（`gjjddb2026` @ 43.138.163.7:3308）有 `exam_*`/`train_*` 同名表，但是 2026-07-03 前快照：`exam_result`=0 行、`train_exam_people_list`=0 行、`platform_user_token`=0 行、`platform_user` 仅 33 测试号 | **本功能业务数据一律以 PHP 库为准实时查询**，不用 SaaS 快照表 |
| SaaS 已有 `FaqLlmClient`（DeepSeek/OpenAI 兼容客户端，含限流/超时/JSON mode 重试）、`exam_faq_model_config`（模型配置）、`AsyncConfig` 线程池、`/appcoach` 等 App 接口先例 | LLM 与 App 接口直接复用，不重复建设 |
| SaaS 已有 `exam_knowledge_rate`（权重表，course_id + 考核项目 + 权重） | 组卷与薄弱点加权可复用 |
| PHP 取题链路（`appreview/sortlist`）：`train_exam_list.source` → `train_exam_work_type_list`（工种+等级）→ `exam_course` → 三级知识点（level=3,type=0）→ `exam_exam_detail`（source 一致 + knowledge_id IN + final_check_status=1 + deleted=0） | 本功能"题目全集"解析算法原样复刻到 SaaS（读 PHP 库） |
| 需求文本数字矛盾：三阶段表写"第一阶段每日新题 100-120"，理论章节两处写"前 10 天每天 150 题" | **参数化**：`daily_new_cap` 默认 120，算法按"剩余题量÷剩余天数"动态计算并封顶，见 §5.2 |

### 1.2 为什么不直接用 LLM 排题（设计说明）

需求设想"AI 模型自动排题"。三阶段题量分配、艾宾浩斯复盘、错题三级淘汰、中断 ABCD 补救**全部是确定性规则**：硬约束（"红牌绝不直接移除""每日不超 200 题"）必须 100% 可执行、可单测、可复现。LLM 做算术调度慢（秒级）、贵（每学员每天调用）、输出不确定、无法单测。因此：

- **规则引擎（Java）**：计划生成、每日排题、错题状态机、中断检测与重排、组卷 —— 毫秒级、零成本、全覆盖单测；
- **LLM（DeepSeek）**：每日鼓励语、顽固错题（加星红牌）个性化讲解、周学情报告 —— 生成式任务，LLM 故障时降级为预置文案，**绝不阻塞刷题主链路**；
- LLM 生成内容按 AGENTS.md 要求留痕（来源/模型/时间/操作人）。

## 2. 总体架构

```
手机端 uni-app（新增 同步练习 页面族）
  │  saasRequest：Authorization: Bearer <itoken> + X-Tenant-Id:1
  ▼
SaaS platform-api
  ├─ security/filter/AppTokenAuthFilter（已有，验票 → PlatformUserPrincipal）
  ├─ app/reviewsync/                    【新建】App 接口层（/api/appreviewsync/**）
  │    ├─ controller  AppReviewSyncController
  │    ├─ service     ReviewPlanApplicationService（用例编排/事务）
  │    └─ dto/vo
  ├─ exam/reviewsync/                   【新建】领域层（规则引擎，纯逻辑可单测）
  │    ├─ PlanScheduleDomainService     三阶段调度/每日配额/中断ABCD重排
  │    ├─ WrongCardDomainService        错题三级淘汰状态机
  │    ├─ QuestionSelectDomainService   题目全集解析/冗余裁剪/组卷
  │    └─ LlmAssistService              鼓励语/错题讲解/周报（复用 FaqLlmClient）
  ├─ integration/php/                   【新建】PHP 库只读访问层
  │    ├─ PhpDataSourceConfig           第二数据源（独立 SqlSessionFactory，无租户拦截器）
  │    ├─ entity/mapper                 exam_exam_detail / train_exam_* / exam_result ...
  │    └─ PhpExamQueryService           题目全集/计划/学员关联/历史答题查询
  ├─ 定时任务（xxl 或 Spring @Scheduled，复用 AsyncConfig 线程池）
  │    ├─ 每日任务物化 job（04:00）
  │    └─ exam_result 回写失败补偿 job（5min）
  └─ SaaS 库新建表（Flyway）：ai_review_plan / ai_review_plan_day /
     ai_review_answer / ai_review_wrong_item / ai_review_mock_paper(+_item)
       │
       ├─ 读：integration/php → PHP 库 gjjddb（SELECT）
       └─ 写：SaaS 库新表（主记录）+ 异步回写 PHP 库 exam_result（INSERT，exam_type=5）
```

**线程边界声明**（AGENTS.md 异步陷阱）：HTTP 线程内从 `SecurityContext` 取 `platformUserId`、从 `TenantContextHolder` 取 `tenantId`（header=1，**不取 principal，App token 的 principal.tenantId 恒为 0**），之后全部参数传递；异步线程（回写/批量物化）与定时任务线程内**禁止**调用 `SecurityContext`，定时任务线程入口显式 `TenantContextHolder.setTenantId(...)` + `finally clear()`。

## 3. 数据访问设计（PHP 库只读第二数据源）

### 3.1 数据源配置

- 新增 `integration/php/PhpDataSourceConfig`：第二数据源 `phpDataSource`（Druid，最大连接 3~5，connectTimeout/socketTimeout 必配），**独立 `SqlSessionFactory`（phpSqlSessionFactory），不挂载 MyBatis-Plus 多租户拦截器**——PHP 库数据 `tenant_id=10159`（旧口径），而 App 请求 header 租户为 1，若挂拦截器会错拼 `tenant_id=1` 查不到数据。该工厂只扫描 `integration/php/mapper`。
- 主数据源（SaaS 库）及其现有配置**零改动**。
- 配置项（环境变量）：`PHP_MYSQL_URL=jdbc:mysql://114.132.67.185:3308/gjjddb`、`PHP_MYSQL_USERNAME`、`PHP_MYSQL_PASSWORD`。

### 3.2 PHP 库账号权限（运维前置）

```sql
-- 专用账号，限定来源 IP
CREATE USER 'saas_sync'@'<SaaS服务器IP>' IDENTIFIED BY '<强密码>';
GRANT SELECT ON gjjddb.platform_user          TO 'saas_sync'@'<SaaS服务器IP>';
GRANT SELECT ON gjjddb.platform_user_token    TO 'saas_sync'@'<SaaS服务器IP>';
GRANT SELECT ON gjjddb.platform_user_detail   TO 'saas_sync'@'<SaaS服务器IP>';
-- 本功能查询表
GRANT SELECT ON gjjddb.exam_exam_detail          TO 'saas_sync'@'<SaaS服务器IP>';
GRANT SELECT ON gjjddb.exam_knowledge_structure  TO 'saas_sync'@'<SaaS服务器IP>';
GRANT SELECT ON gjjddb.exam_course               TO 'saas_sync'@'<SaaS服务器IP>';
GRANT SELECT ON gjjddb.exam_work_type            TO 'saas_sync'@'<SaaS服务器IP>';
GRANT SELECT ON gjjddb.train_exam_list           TO 'saas_sync'@'<SaaS服务器IP>';
GRANT SELECT ON gjjddb.train_exam_review         TO 'saas_sync'@'<SaaS服务器IP>';
GRANT SELECT ON gjjddb.train_exam_work_type_list TO 'saas_sync'@'<SaaS服务器IP>';
GRANT SELECT ON gjjddb.train_exam_people_list    TO 'saas_sync'@'<SaaS服务器IP>';
GRANT SELECT ON gjjddb.train_exam_shijuan        TO 'saas_sync'@'<SaaS服务器IP>';
GRANT SELECT ON gjjddb.exam_paper                TO 'saas_sync'@'<SaaS服务器IP>';
GRANT SELECT ON gjjddb.exam_result               TO 'saas_sync'@'<SaaS服务器IP>';
-- 回写仅授权 INSERT（禁止 UPDATE/DELETE）
GRANT INSERT ON gjjddb.exam_result TO 'saas_sync'@'<SaaS服务器IP>';
```

> 注：鉴权方案的 3 表同步任务与本功能共用同一数据源配置；同步任务（写 SaaS 库镜像）与本功能（直读 PHP 库）互不冲突。

### 3.3 查询口径（复刻 PHP `appreview/sortlist`）

题目全集解析 `PhpExamQueryService#loadQuestionScope(examId)`：

1. `train_exam_list` 取 `source`（`deleted=0 AND id=?`）；
2. `train_exam_work_type_list` 取 `work_type_id, level, course_id`（`deleted=0 AND exam_id=?`，可多组）；为空 → 业务异常"该计划没有正确绑定工种"；
3. 有 `course_id` 直接用，否则按 `(work_type_id, level)` 查 `exam_course` 补；
4. `exam_knowledge_structure` 取三级知识点 id（`course_id IN (...) AND level=3 AND type=0`）；
5. 题目全集：`exam_exam_detail WHERE source=? AND knowledge_id IN (...) AND final_check_status=1 AND deleted=0`，列 `id, knowledge_id, type, difficulty, score`。

全部 Mapper 返回值 null 检查（AGENTS.md 防御性编程）。

### 3.4 题目内容与缓存

- 题目详情（content/answer/resolve，含 HTML）查询走"**读穿缓存**"：Caffeine 本地缓存（题 id → 详情，TTL 6h，按工种题库量级单机可控），缓存 miss 回源 PHP 库；PHP 库不可达且缓存 miss → 业务异常"题库服务暂不可用，请稍后重试"（不静默吞异常）。
- 下发手机端前**剥离答案标记**：`answer` JSON 中剔除 `isCorrect/score`，只留 `content` 列表（判分在服务端，防抓包背答案）。
- 每日任务表只存题 id 列表（JSON），不复制题内容。

## 4. 数据库设计（SaaS 库，Flyway）

> 全部新表含完整 BaseEntity 列：`tenant_id / deleted / created_by / updated_by / created_at / updated_at`，主键 `BIGINT` 雪花 ID。迁移文件：`V20260804_001__create_ai_review_sync_tables.sql`（先查 `flyway_schema_history` 最大版本再定稿）。

### 4.1 ai_review_plan — AI 复习计划主表（学员 × 考试计划 唯一）

| 列 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | 雪花 |
| platform_user_id | BIGINT | 学员（PHP platform_user.id），索引 |
| exam_id | BIGINT | 培训考试计划 train_exam_list.id，索引 |
| work_type_id / level / course_id / source | BIGINT / VARCHAR(32) / BIGINT / BIGINT | 题目范围快照（计划生成时固化，防后台改绑影响进行中计划） |
| exam_date | DATE NULL | 考试日期（解析自 train_exam_list.exam_date/exam_date_list 最近未来日） |
| start_date | DATE | 复习开始日（计划生成日） |
| total_days / stage1_days / stage2_days / stage3_days | INT | 总天数与三阶段天数（50%/30%/20% 取整，尾差归阶段3） |
| total_questions | INT | 题目全集题数 |
| target_questions | INT | 冗余裁剪后必做题数（未裁剪=total_questions） |
| trimmed | TINYINT | 是否触发裁剪 |
| daily_new_cap / daily_total_cap | INT | 参数快照（默认 120 / 200） |
| current_stage | TINYINT | 当前阶段 1/2/3 |
| current_day_index | INT | 当前第几天（从 1 起） |
| last_active_date | DATE NULL | 最后活跃日（有作答即更新） |
| interrupt_count | INT | 中断次数（零星中断判定用） |
| status | VARCHAR(16) | active / completed / abandoned |
| UNIQUE KEY | (platform_user_id, exam_id) | 幂等防重 |

### 4.2 ai_review_plan_day — 每日任务

| 列 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | |
| plan_id | BIGINT | 索引 |
| day_index / plan_date | INT / DATE | 第几天 / 计划日期 |
| stage | TINYINT | 1/2/3 |
| new_question_ids | JSON | 今日新题 id 有序列表 |
| review_question_ids | JSON | 今日复盘题 id 有序列表（红牌置顶、加星优先） |
| mock_paper_id | BIGINT NULL | 阶段3 模考卷（→ ai_review_mock_paper.id） |
| new_target / review_target | INT | 目标量 |
| new_done / review_done | INT | 实际完成 |
| correct_count / answer_count | INT | 当日判分统计 |
| status | VARCHAR(16) | pending / in_progress / done / skipped / abandoned |
| strategy_note | VARCHAR(500) NULL | 当日策略说明（如中断补救文案，展示给学员） |
| UNIQUE KEY | (plan_id, day_index) | |

### 4.3 ai_review_answer — 作答记录（主记录）

| 列 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | |
| plan_id / day_id NULL | BIGINT | 模考答完 day_id 可空 |
| platform_user_id | BIGINT | 索引 |
| question_id | BIGINT | exam_exam_detail.id |
| knowledge_id / question_type | BIGINT / VARCHAR(16) | 冗余便于统计 |
| round_type | VARCHAR(8) | new / review / mock |
| user_answer | TEXT | 作答内容（单/判：选项序号；多选：序号数组 JSON） |
| is_correct | TINYINT | 0/1 |
| use_time_sec | INT | 用时 |
| attempt_seq | INT | 该题第几次作答 |
| php_synced | TINYINT | 回写 PHP exam_result 标记：0 未回写 1 已回写 2 失败待补偿，索引 |
| php_sync_time | DATETIME NULL | |

### 4.4 ai_review_wrong_item — 错题本（三级淘汰）

| 列 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | |
| plan_id / platform_user_id | BIGINT | |
| question_id / knowledge_id | BIGINT | |
| card_level | VARCHAR(8) | red / yellow / green，索引 |
| star | TINYINT | 二次错误加星（顽固盲区） |
| consecutive_correct | INT | 连续答对计数 |
| last_correct_date | DATE NULL | 最近一次答对日期（黄牌间隔>3天判定） |
| wrong_count / correct_count | INT | 累计 |
| first_wrong_time / last_wrong_time | DATETIME | |
| removed | TINYINT | 是否已移出（绿牌=1） |
| remove_time | DATETIME NULL | |
| UNIQUE KEY | (plan_id, question_id) | |

### 4.5 ai_review_mock_paper / ai_review_mock_paper_item — 自组模考卷

- `ai_review_mock_paper`：id, plan_id, paper_no（第几套）, question_count, total_score, pass_score, time_limit_min, status(ready/done), score, correct_count, create 审计列；
- `ai_review_mock_paper_item`：id, paper_id, question_id, question_type, sort_number, score。UNIQUE(paper_id, question_id)。

### 4.6 复用与留痕

- LLM 调用留痕：复用现有 `ai_generation_draft`（来源/模型/时间/操作人，AGENTS.md AI 规则），`biz_type='ai_review_encourage' / 'ai_review_explain' / 'ai_review_weekly'`。
- 参数配置：一期用 `sys_dict`（`ai_review_daily_new_cap=120`、`ai_review_daily_total_cap=200`、`ai_review_mock_size=100`、`ai_review_default_days=20`、`ai_review_interrupt_gap=2`），不做管理界面。

### 4.7 PHP 库回写（exam_result，仅 INSERT）

异步回写字段口径（对齐 PHP `trainsave`）：`type`=题型，`exam_plan_id`=train_exam_list.id，`paper_id`=0，`detail_id`=0，`exam_id`=题 id，`platform_user_id`、`user_answer`、`get_score`（对=题分/错=0），`exam_type=5`（**新增枚举：AI 同步练习**，PHP 端既有统计按 exam_type 过滤不受影响），`tenant_id=10159`（PHP 库口径，非 SaaS 的 1），`create_time`=NOW()，`deleted`=0。

## 5. 规则引擎设计（核心，全部纯逻辑可单测）

### 5.1 计划生成（懒触发）

学员首次调 `overview` 且无进行中计划时生成：

1. 校验学员在计划中：`train_exam_people_list`（exam_id + platform_user_id + deleted=0），不在 → 业务异常"您不在该复习计划名单中"；
2. 解析题目全集（§3.3），N=题数；N=0 → 异常"该计划未绑定有效题库"；
3. 考试日：`train_exam_list.exam_date`（varchar，按 `yyyy-MM-dd` 解析）→ 无/过去 → 查 `exam_date_list`（JSON 日期数组取最近未来日）→ 仍无 → 默认 20 天（`ai_review_default_days`）并 `strategy_note` 提示"未设置考试日期，按 20 天默认规划"；
4. D = 考试日 - 今天（下限 3 天，下限保护）；阶段切分 D1=round(D×0.5)、D2=round(D×0.3)、D3=D-D1-D2；
5. 冗余判定：`N > D1 × daily_new_cap` → 触发裁剪（§5.3），target = D1 × daily_new_cap；
6. 落库 plan + 物化第 1 天任务；初始化 LLM 鼓励语（异步，失败用预置文案）。

### 5.2 每日配额算法（三阶段）

每天物化时（`remaining_new` = 必做题中尚未进过任何每日任务的题）：

- **阶段 1（地毯式扫荡）**：`new = min(daily_new_cap, ceil(remaining_new / 剩余阶段1天数))`；`review = 昨日新错题（全部红牌）+ 到期红牌/黄牌`；若 `new + review > daily_total_cap` → 新题让位：`new = max(0, daily_total_cap - review)`，让位新题顺延；
- **阶段 2（定向爆破）**：`review = 全部未移除错题（红牌优先、加星优先、按 last_wrong_time 倒序）`，封顶 300；`new = 薄弱知识点剩余题`（按学员各知识点错题率倒序取，≤50）；复盘优先，新题可让位至 0；
- **阶段 3（仿真模考）**：每日 1 套自组模考卷（默认 100 题，`ai_review_mock_size`）；考前最后 1 天可 2 套（二期开关，一期 1 套）；模考错题当日入错题本，次日穿插复盘。

### 5.3 题库冗余裁剪（"真干货"选题）

目标：从 N 题选出 target 题，**知识点覆盖率 100%**。贪心：

1. 每个三级知识点先取 1 题（优先级：重要度 X > Y > Z 的知识点优先保证；同知识点内：多选 > 单选 > 判断 > 其他，取难度中等者优先）；
2. 剩余名额按"知识点权重（exam_knowledge_rate 无则用知识点题量占比）× 题型权重（多选 3、单选 2、判断 1）"加权轮转填满；
3. 被裁掉的题标记为"扩展题"（不进必做池，不进任何每日任务）；
4. 中断补救情况 A 的"再删尾部 10%~15% 偏题"：在必做池内按 `重要度 Z 的知识点题 → cal/answer/essay 题型 → 难度 difficulty 最高` 排序删除，删除量 = `ceil(target × 15%)` 且以"每日 ≤180 题"收敛为止。

### 5.4 错题三级淘汰状态机

```
作答判错 ──→ 红牌 red（consecutive_correct=0）
红牌复盘对 → consecutive_correct+1；≥3 → 黄牌 yellow（计数清零，记 last_correct_date）
红牌复盘错 → consecutive_correct=0、star=1（加星），未来 3 天每日必出
黄牌复盘对 → consecutive_correct+1、更新 last_correct_date；
             ≥2 且 与上次答对间隔>3天 → 绿牌 green（removed=1，移出）
黄牌复盘错 → 回红牌 red、计数清零、star=1（从严，可配置）
绿牌        → 不再进每日复盘，仅考前 1 天"快速浏览"清单返回
```

- **当日新错题**：一律红牌，当日不可移除（需求硬规则）；
- **加星题**：红牌中 star=1 者，每日复盘队列置顶，连续 3 天强制出现；
- **每周日清理日**：定时任务批量复核（状态机实时已执行，清理日做兜底一致性扫描 + 生成周报）；
- **中断残酷清洗**（见 §5.5 触发）：近 14 天内移除的绿牌 → 回黄牌；全部在册红/黄牌 `consecutive_correct` 清零。

### 5.5 中断检测与 ABCD 补救（决策表）

`gap = today - last_active_date`，`gap ≥ ai_review_interrupt_gap(默认2)` 触发；每日物化时自动判定，也可在 `overview` 实时检测。

| 情况 | 判定 | 补救动作（重排后续计划） |
|---|---|---|
| A 阶段1中断 | 触发时 current_stage=1 | 恢复第 1 天：仅 80 新题且只含单选+判断（预热）；第 2 天起 `每日新题 = 剩余必做题 ÷ 剩余阶段1天数`；若 >180/天 → 按 §5.3 第 4 条删尾部偏题（≤15%），`strategy_note` 告知学员"已放弃尾部偏题，保住 80% 基础题" |
| B 阶段2中断 | current_stage=2 | 恢复第 1 天：100 题从未做过的混编摸底测验；正确率 ≥70% → 次日直接进阶段 3；<70% → 未来 3 天只出红牌题，剩余新题全部标记 abandoned（不再出现） |
| C 阶段3中断 | current_stage=3 | 不再排新题/复盘，恢复后每天 1 套模考卷，交卷后逐题强制看解析（客户端展示，服务端下发解析标记 `mustReadResolve=1`） |
| D 零星中断 | 近 7 天内 interrupt 触发 ≥2 次 | 转"3 日滚动法"：每次重启第 1 天只出"前一次任务最后 50 题"作为热车（round_type=review），第 2 天起新题固定 100/天拉长战线；超出考试日的任务按日顺延压缩阶段比例 |

中断触发同时执行 §5.4 残酷清洗。所有重排只影响 `status=pending` 的未来任务日，已完成的不改。

### 5.6 自组模考卷算法（D3）

1. 题量：`ai_review_mock_size`（默认 100）；
2. 题型比例：按必做池内题型自然分布取整（如 单选:判断:多选 ≈ 5:4:3），多选不少于 20%；
3. 知识点配额：按 `exam_knowledge_rate` 权重分配（无权重配置则按知识点题量占比），学员错题率高的知识点 ×1.5 加权；
4. 组卷快照落 `ai_review_mock_paper(+_item)`（题序随机，脱离题库顺序——需求"训练考场应变"）；
5. 判分：单选/判断全对得分；多选按 PHP 口径（answer JSON 内各选项 score 权重，错选不得分，少选按 PHP trainsave 口径取部分分——实现时以 PHP `trainsaveAction` 判分代码为准逐行对齐）；
6. 交卷：写 `ai_review_answer`（round_type=mock）→ 错题入册 → 异步回写 PHP exam_result → 返回得分/及格判定（pass_score=total×0.6）/错题清单。

## 6. LLM 辅助设计（D2）

| 场景 | 触发 | 提示词输入 | 输出 | 降级 |
|---|---|---|---|---|
| 每日鼓励语 | 计划生成 / 每日物化 | 学员姓名、工种等级、当前阶段、第几天/总天数、昨日正确率、中断后回归标记 | ≤60 字教练式鼓励 | 预置文案池（按阶段×场景 20 条） |
| 顽固错题讲解 | 学员在加星红牌题答错后点"AI 讲解" | 题干、选项、正确答案、解析、学员错答 | 通俗讲解+记忆锚点，≤200 字 | 直接展示官方 resolve 解析 |
| 周学情报告 | 每周日清理日 job | 本周各知识点正确率、错题牌级分布、题量完成度 | 周报 + 下周建议 | 仅展示统计数据，无文字报告 |

- 客户端：`FaqLlmClient`（复用），模型配置取 `exam_faq_model_config`（`model_code='deepseek-review'` 新配置一行）；
- 约束：调用前核对目标模型文档，不支持的字段一律不传（AGENTS.md 坑 #13）；超时 8s、重试 1 次；生成内容写 `ai_generation_draft` 留痕；
- 全部异步执行（`ThreadPoolTaskExecutor` 具体类型 Bean + `@Qualifier`，AGENTS.md 坑 #14），主链路不等待。

## 7. 接口设计（App 端，/api/appreviewsync/**）

> 遵循鉴权方案：**不加入** SecurityConfig / TenantContextFilter 白名单；**不用** `@PreAuthorize`；userId 取自 `PlatformUserPrincipal`，tenantId 取 `TenantContextHolder`（header）；业务响应沿用 App 风格 `{code:0, msg:"成功", data:{}}`（code=0 成功，1 业务异常，鉴权失败由 EntryPoint 返 HTTP 401）。

### 7.1 POST /appreviewsync/overview — 计划概览（懒生成入口）

- 入：`{examId}`
- 出：`{planId, examTitle, workTypeTitle, level, stage, stageName, dayIndex, totalDays, examDate, today:{dayId,newTarget,reviewTarget,newDone,reviewDone,mock:{mockPaperId,questionCount,status}|null}, progress:{doneQuestions,targetQuestions,coveragePercent,accuracy}, wrongStats:{red,yellow,green,star}, interrupt:{case:'A'|'B'|'C'|'D',strategyText}|null, encourageText}`
- 逻辑：无计划→生成（§5.1）；有计划→中断检测（§5.5）→ 当日任务未物化则物化（§5.2）。

### 7.2 POST /appreviewsync/today — 今日任务清单

- 入：`{examId}`
- 出：`{dayId, stage, newQuestions:[{questionId,type,knowledgeId,done}], wrongQuestions:[{questionId,type,cardLevel,star,wrongCount,done}]}`
- 说明：只回 id 清单（对齐 sortlist 模式）；客户端分上下两区块渲染（§9）。

### 7.3 POST /appreviewsync/question — 题目详情（批量）

- 入：`{examId, questionIds:[...]}`（≤50）
- 出：`{list:[{questionId,type,content,options:[{content}],resolve 不下发}]}` —— **剥离 isCorrect/score/resolve**；解析在提交后由 7.4 返回。

### 7.4 POST /appreviewsync/answer — 单题作答

- 入：`{examId, dayId, questionId, roundType, userAnswer, useTimeSec}`
- 出：`{correct, correctAnswer, resolve, cardChange:{from,to,star}|null, encourageText|null}`
- 逻辑：服务端判分 → 写 `ai_review_answer` → 驱动错题状态机（§5.4）→ 更新 day 计数 → 异步回写 PHP（§4.7）；加星题可带 `explainAvailable=1`。

### 7.5 POST /appreviewsync/mock/start — 开始模考

- 入：`{examId}` → 出：`{mockPaperId, questionCount, timeLimitMin, questions:[{questionId,type}]}`（内容走 7.3 分批取）

### 7.6 POST /appreviewsync/mock/submit — 模考交卷

- 入：`{examId, mockPaperId, answers:[{questionId,userAnswer,useTimeSec}], totalUseTimeSec}`
- 出：`{score, passScore, passed, correctCount, wrongCount, wrongList:[{questionId,correctAnswer,resolve}], mustReadResolve:1}`

### 7.7 POST /appreviewsync/finish — 结束今日练习

- 入：`{examId, dayId, useTimeSec}` → 出：`{summary:{newDone,reviewDone,accuracy,useTimeMin}, tomorrowPreview:{newTarget,reviewTarget}, encourageText}`

### 7.8 POST /appreviewsync/progress — 学情总览

- 入：`{examId}` → 出：`{knowledgeCoverage:[{knowledgeId,title,total,done,accuracy}], typeAccuracy:{single,multi,judge}, wrongStats, dailyTrend:[{date,newDone,reviewDone,accuracy}], weekReport|null}`

### 7.9 POST /appreviewsync/explain — 顽固错题 AI 讲解

- 入：`{examId, questionId}` → 出：`{explainText, fromCache}`（仅加星红牌题可用，LLM 生成，留痕）

## 8. 定时任务与异步设计

| 任务 | 频率 | 内容 | 线程边界要点 |
|---|---|---|---|
| 每日任务物化 | 04:00 | 遍历 `status=active` 计划：中断检测 → 物化当日任务 → 生成鼓励语（异步） | job 线程 `TenantContextHolder.setTenantId(1L)` + finally clear；planId/tenantId 参数传递，不调 SecurityContext |
| 每周日清理日 | 周日 05:00 | 错题一致性复核 + 周报生成（LLM 异步） | 同上 |
| exam_result 回写补偿 | 每 5min | 扫 `php_synced IN (0,2)` 重试，连续失败 12 次标记死信并告警日志（error 级） | 同上；批量分页，单批 ≤500 |
| 作答回写（即时） | 作答后异步 | 单条 INSERT PHP exam_result，失败置 `php_synced=2` 交补偿 job | 复用 `applicationTaskExecutor` 或新建 `reviewSyncExecutor`（AbortPolicy + 全局异常 503，AGENTS.md 坑 #12） |

## 9. 手机端改造设计（gjjd_appnext）

1. **入口**：`pages/promote/exam.vue` 顶部菜单新增"同步练习"（置于"顺序练习"上方），`navTo('/pages/exam/syncHome', {exam_id})`；
2. **新页面族**（`pages/exam/`）：
   - `syncHome.vue`：概览（7.1）——阶段进度条、今日任务卡片、鼓励语、中断提示条（strategyText）；
   - `syncExam.vue`：答题页——**上下物理隔离两区块**：上半【今日新题】、下半【错题清算】；错题题号**高饱和红/橙背景 + "错题重做"醒目字样**，加星题加 ★；单题提交（7.4）即时判分展示解析；
   - `syncMock.vue`：模考页（7.5/7.6），交卷后逐题解析（mustReadResolve）；
   - `syncProgress.vue`：学情页（7.8）——知识点覆盖率、题型正确率、错题牌级分布、周报；
3. **请求**：全部走 `saasRequest`（按鉴权方案完成 Bearer 透传 + 401 退避重试改造后），接口封装新增 `api/saas-review.js`；
4. **兼容性**：保留原有"顺序练习/随机练习/模拟考试/我的错题"菜单不变，同步练习为增量入口。

## 10. 测试方案

### 10.1 单元测试（规则引擎必须全覆盖，JUnit5 + Mockito）

- **调度算法**：1200 题/20 天（基准）；3000 题/20 天（触发裁剪，验证知识点覆盖率=100%）；300 题/30 天（稀疏）；中途加入 D=7（下限保护）；无考试日期（默认 20 天）；
- **每日配额**：复盘挤压新题（new+review>cap 时 new 让位）；阶段 2 新题可为 0；阶段 3 只出模考；
- **错题状态机**：红→黄（连续 3 对）、黄→绿（连续 2 对且间隔>3 天）、答错回退与加星、当日新错题不可移除、加星题 3 天必现、周日清理、中断清洗（绿牌回黄、计数清零）——全部状态迁移路径枚举测试；
- **中断 ABCD**：各阶段 × 不同 gap 天数 → 断言重排结果与决策表一致；D 类零星中断（7 天内 2 次）转 3 日滚动；
- **组卷**：题型比例、知识点配额加权、无重复题、快照落库；
- **判分**：单/多/判 + 多选少选部分分（与 PHP trainsave 口径对齐的用例集）。

### 10.2 集成测试（Spring Boot Test + MockMvc + Testcontainers）

- 双 MySQL 容器：一个模拟 SaaS 库（跑 Flyway）、一个模拟 PHP 库（灌 schema +  fixtures：计划/工种/课程/知识点/题目/学员关联）；
- API 契约：7.1~7.9 全接口正例 + 异常例（无 token→401、坏 token→401、学员不在计划→code=1、计划未绑工种→code=1、题库为空→code=1）；
- 租户与身份：断言 userId 只取 principal（伪造 body.userId 不生效）；PHP 数据源查询不拼 `tenant_id=1`（验证独立 SqlSessionFactory 无租户拦截器）；
- 回写：断言 PHP 容器 exam_result 收到 `exam_type=5`、`tenant_id=10159` 的行；回写失败 → `php_synced=2` → 补偿 job 重试成功；
- 异步边界：批量物化任务线程内无 SecurityContext 调用（故意在 Worker 调 SecurityContext 的回归用例应失败）。

### 10.3 LLM 测试

- Mock `FaqLlmClient`：验证提示词组装、留痕落 `ai_generation_draft`；LLM 超时/异常 → 预置文案降级、主链路响应不受影响（含响应时间断言）。

### 10.4 E2E 冒烟（真机/真库）

1. PHP 登录拿 itoken → 调 `/appreviewsync/overview`（Bearer + X-Tenant-Id:1）→ 返回计划；
2. 走通：同步练习首页 → 答题（故意答错 3 题）→ 错题入册（红牌）→ 次日复盘 → 连续答对降级；
3. 模考交卷 → PHP 管理端统计可查 exam_type=5 记录；
4. 模拟中断（改 last_active_date）→ 次日任务按 ABCD 重排；
5. PHP 侧回归：原"顺序练习/随机练习/模拟考试"不受影响。

## 11. 安全与性能

- 生产必须 HTTPS（`g.saas.pro` 当前 http，鉴权方案已列整改项）；
- 题目下发剥离答案；判分全部服务端；userId 不信任前端；
- PHP 库账号最小权限（SELECT + 单表 INSERT），限来源 IP；
- 接口限流：answer 单题提交幂等（plan_id+day_id+question_id+attempt_seq 判重，防双击/重试重复计分）；
- 每日物化批量任务分页 + 线程池 AbortPolicy；题库内容 Caffeine 缓存降低对 PHP 库压力；
- 所有认证/授权路径 warn/error 日志（AGENTS.md）。

## 12. 分期实施

| 期 | 内容 |
|---|---|
| 一期（本方案） | 第二数据源 + PHP 查询层；计划生成/每日物化/错题状态机/中断 ABCD/自组卷模考；接口 7.1~7.8；回写 exam_result + 补偿；LLM 鼓励语（含降级）；手机端同步练习三页 |
| 二期 | AI 错题讲解（7.9）、周报、考前 1 天绿牌速览清单、主动推送鼓励（对接 PHP WS/短信）、管理端参数配置页、一日两套模考开关 |

## 13. 开放问题（实现时按默认口径执行，异议即提出）

| # | 问题 | 默认口径 |
|---|---|---|
| O1 | 每日新题 100-120 还是 150？ | 参数 `daily_new_cap=120`，算法动态计算封顶 |
| O2 | 黄牌答错是否回红牌？ | 回红牌 + 加星（从严，sys_dict 可关） |
| O3 | 中断清洗"绿牌回黄"范围 | 近 14 天内移除的绿牌 |
| O4 | exam_result.exam_type=5 与 PHP 端统计口径 | 新增枚举不影响旧逻辑；PHP 端如需在统计页展示"AI 同步练习"另行提需 |
| O5 | 模考卷题量/及格线 | 100 题/卷、60% 及格，sys_dict 可调 |
| O6 | 学员同时多个复习计划 | 各计划独立 plan，互不影响；App 按 examId 维度进入 |
