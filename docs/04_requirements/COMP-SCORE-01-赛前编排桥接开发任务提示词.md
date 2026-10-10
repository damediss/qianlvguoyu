# COMP-SCORE-01 赛前编排与成绩域桥接开发任务提示词

以下内容可直接复制到一个全新的 Codex 任务中执行。本任务是当前应优先执行的任务；不要先执行 `COMP-SCORE-02`。

---

你正在仓库 `D:\qianlvguoyu\qianlvguoyu_saas-git` 中工作。请直接完成 `COMP-SCORE-01：竞赛赛前编排与成绩域桥接`，不要只输出方案。完成后给出可供另一名审查者复核的实施报告。

## 一、必须先读和核对

1. 完整阅读根目录 `AGENTS.md`。
2. 按顺序阅读：
   - `README.md`
   - `docs/README.md`
   - `docs/05_decisions/开发框架选型建议.md`
   - `docs/05_decisions/云平台技术路线选型建议.md`
   - `docs/04_requirements/competition-end-to-end-chain-gap-assessment.md`
   - `docs/04_requirements/competition-scoring-rule-contract.md`
   - `docs/05_decisions/competition-event-member-role-model.md`
   - `docs/competition-foundation-design.md`
3. 执行 `git status --short`。已有修改均视为用户资产，不得覆盖、回滚、清理或顺带格式化无关文件。
4. 检查以下现有实现，不得重复造一套：
   - `competition_event`、`competition_stage`、`competition_registration`
   - `competition_event_member`、`competition_contestant_profile`
   - `CompetitionStageApplicationService`
   - `CompetitionRegistrationApplicationService`
   - 现有 `exam_work_type` 实体、表和查询能力
   - 现有竞赛权限迁移、Controller API 和测试风格
5. 新建 Flyway 前查询目标数据库 `flyway_schema_history` 的实际最大版本，严格遵守 `AGENTS.md`。数据库不可访问时不得猜迁移版本号，应完成其余可独立工作并在报告中明确阻塞。

## 二、任务背景和目标

当前仓库已经有赛事、参与企业、名单导入、选手身份和赛事成员底座，但成绩专项缺少稳定的上游业务键：

- 工种仍是名单里的自由文本。
- 选手扩展没有正式赛事工种归属。
- 赛段 ApplicationService 没有 Controller。
- 资格审核 ApplicationService 没有 Controller。
- 赛事没有可发布、可锁定的赛制 B 规则版本。
- 成绩域没有统一的“是否允许开始评分”就绪校验。

本任务要补齐从“合格选手”进入“理论/实操成绩”的桥梁，使下一任务能够稳定取得：

```text
tenantId + eventId + stageId + eventWorkTypeId + contestantMemberId + publishedScoreRuleVersionId
```

本任务完成后，不产生正式成绩、不计算排名；只保证成绩专项的输入范围真实、稳定、可审计。

## 三、必须实现的范围

### 1. 赛事工种/赛项

新增规范化的“赛事工种”业务实体，关联现有题库工种 `exam_work_type`，不要复制一套工种主数据。

至少包含：

- `event_id`
- `work_type_id`，关联现有题库工种 ID
- 赛事内显示名称或显示快照
- 状态
- 排序号
- 完整 BaseEntity 字段

核心规则：

- 同一赛事不得重复绑定同一工种。
- 只允许绑定当前租户内有效、未删除的题库工种。
- 排名、规则、考试和裁判后续都使用赛事工种 ID，禁止使用名单 `work_type` 文本作为主键。
- 已被已发布规则引用的赛事工种不得直接删除或换绑，应使用状态和新版本表达。

提供最小后台 API：

```text
GET  /api/competition/events/{eventId}/work-types
POST /api/competition/events/{eventId}/work-types
PUT  /api/competition/events/{eventId}/work-types/{eventWorkTypeId}
```

使用项目统一响应、分页/列表规范、权限和赛事数据范围校验。

### 2. 选手赛事工种归属与名单文本对账

为 `competition_contestant_profile` 增加稳定的赛事工种关联，或采用等价且更符合现有模型的关联表。必须满足：

- 一名选手在当前业务合同下只归属一个赛事工种。
- 归属的赛事工种必须属于该选手所在赛事。
- 名单中的 `work_type` 文本保留为原始导入值，不删除、不改写为事实主键。
- 系统提供“已匹配、未匹配、冲突”的对账结果。
- 文本完全匹配可以作为候选自动匹配，但不得在多个工种同名或无法唯一命中时猜测；此时必须进入待人工确认。
- 不允许未匹配工种的选手进入后续评分就绪状态。

实现服务端对账和人工确认所需的 DTO/ApplicationService/API；本任务不做管理端页面。

### 3. 补齐赛段 API

复用现有 `CompetitionStageApplicationService`，新增薄 Controller，不复制业务逻辑。

建议路径：

```text
GET  /api/competition/events/{eventId}/stages
POST /api/competition/events/{eventId}/stages
PUT  /api/competition/stages/{stageId}
```

要求：

- 读取使用赛事查看权限。
- 新增和修改只能作用于有权管理的草稿赛事。
- 后端校验跨租户、跨企业、跨赛事访问。
- 写操作记录操作日志。
- 增加 API 与权限拒绝测试。

基础任务模板 Controller 不是成绩链路的硬前置，本任务不要顺带实现。

### 4. 补齐资格审核 API

复用现有 `CompetitionRegistrationApplicationService`，新增薄 Controller：

```text
GET  /api/competition/registrations
POST /api/competition/registrations/{registrationId}/audit
```

要求：

- 使用现有 `competition:registration:audit` 等权限语义；先核对历史迁移是否退役或未授予，不得只写 `@PreAuthorize` 而不给目标管理员角色权限。
- 平台管理员和企业用户的数据范围必须符合现有 ApplicationService 约束。
- 只有业务状态机允许的资格可审核。
- 资格只有在有效且已绑定 `contestant` 赛事成员后，才能成为评分候选。
- 增加查询、审核、401/403、跨组织和非法状态测试。

### 5. 赛制 B 规则版本

新增可版本化的竞赛评分规则实体，作用域至少包含：

```text
eventId + stageId + eventWorkTypeId + ruleVersion
```

规则至少保存：

- 赛制类型：本期仅支持 B。
- 理论模板：`THEORY_90` 或 `THEORY_120`。
- 理论权重。
- 仿真实操组合权重。
- 状态：草稿、已发布/锁定、已废止（可按项目枚举命名）。
- 版本号、发布时间、发布人。
- 完整 BaseEntity 字段。

冻结规则：

- 理论、仿真、实操均为 0–100 分。
- `仿真实操组合分 = (仿真分 + 实操分) ÷ 2`。
- 理论权重 + 仿真实操组合权重 = 100%。
- 权重使用 `DECIMAL`/`BigDecimal`，禁止 `float/double`。
- 90 分钟模板固定为单选 60×1、多选 20×1、判断 40×0.5。
- 120 分钟模板固定为单选 120×0.5、多选 20×1、判断 40×0.5。
- 同一工种 A/B 卷后续必须使用同一理论模板。
- 已发布规则不可原地修改；修改必须创建新版本。
- 本任务只保存规则和模板类型，不创建具体试卷、题目快照或考试场次。

提供最小 API：

```text
GET  /api/competition/events/{eventId}/score-rules
POST /api/competition/events/{eventId}/score-rules
PUT  /api/competition/score-rules/{ruleId}
POST /api/competition/score-rules/{ruleId}/publish
```

所有写操作必须校验赛事、赛段和赛事工种属于同一租户与同一赛事。

### 6. 成绩域就绪检查

实现独立的 `CompetitionScoringReadinessService` 或等价领域服务，输入赛事/赛段/赛事工种，返回结构化检查结果，而不是只返回布尔值。

至少检查：

- 赛事存在且状态允许进入后续编排。
- 赛段属于赛事且有效。
- 赛事工种属于赛事且有效。
- 存在唯一已发布的当前规则版本。
- 权重和理论模板合法。
- 当前评分候选选手均具有有效资格、`contestant` 赛事成员和明确赛事工种归属。
- 不把缺考当作赛前不就绪；缺考是比赛执行后的选手状态。

未就绪结果应包含机器可识别的错误码和可读说明，例如：

```text
STAGE_MISSING
EVENT_WORK_TYPE_MISSING
SCORE_RULE_UNPUBLISHED
CONTESTANT_WORK_TYPE_UNRESOLVED
CONTESTANT_INELIGIBLE
```

提供只读检查 API：

```text
GET /api/competition/events/{eventId}/scoring-readiness
```

支持按 `stageId`、`eventWorkTypeId` 查询。下一任务创建成绩来源前必须调用同一领域服务，不能只依赖前端检查。

### 7. 赛事发布兼容边界

当前赛事发布已经用于生成选手入口码，历史赛事可能已发布但缺少新的工种和评分规则。不要直接让历史赛事无法读取或登录。

本任务应：

- 保持历史已发布赛事可读取。
- 对新的评分链路使用就绪检查硬阻断。
- 评估是否适合在“赛事发布”时增加赛段/工种/规则校验。
- 如果无法在不破坏现有赛事和激活链路的情况下安全增加发布校验，本任务不要擅自改变现有发布语义；在实施报告中给出兼容方案，并保证任何成绩或考试执行入口必须被 `CompetitionScoringReadinessService` 阻断。

## 四、数据库和代码约束

- 仅新增 Flyway，不修改已应用迁移。
- 所有新业务表完整包含 BaseEntity 的 6 个公共列以及主键。
- Entity 必须继承 `BaseEntity`，DDL 与 getter 一一对应。
- MySQL 8.4，主键 `BIGINT`，分值/权重使用 `DECIMAL`。
- 建立必要唯一键和查询索引。
- 多租户依赖 MyBatis-Plus 拦截器，不在普通业务代码手拼租户 SQL。
- Controller 只做入口、参数和返回；业务编排在 ApplicationService；核心一致性检查在 DomainService。
- Entity 不直接暴露给前端。
- Mapper 返回值使用前进行 null 检查。
- 写操作记录操作日志；权限失败路径打印 warn/error。
- 新权限必须完成“权限码 + 菜单/能力入口 + 平台管理员角色授权”的闭环。
- 不引入新微服务、工作流引擎或新前端工程。

## 五、测试和验收

至少覆盖：

1. 同一赛事绑定两个不同工种成功，重复绑定同一工种失败。
2. 跨租户或无效题库工种绑定失败。
3. 名单文本唯一匹配时可形成候选归属；零匹配和多匹配进入待确认，不允许评分就绪。
4. 选手不能关联其他赛事的赛事工种。
5. 赛段 Controller 的正常、无权限、跨企业和非草稿修改路径。
6. 资格审核 Controller 的正常、无权限、跨组织和非法状态路径。
7. 规则权重 40/60 发布成功；40/50、负权重、超过 100、无模板发布失败。
8. 已发布规则不可原地修改，新规则只能生成新版本。
9. 规则中的赛事、赛段、赛事工种交叉引用不一致时失败。
10. 就绪检查能分别识别缺赛段、缺工种、规则未发布、选手工种未匹配和资格无效。
11. 完整准备数据时返回就绪，并给出唯一规则版本 ID。
12. 新表 BaseEntity 字段、索引和实体映射合同测试。
13. 新权限已授予平台管理员，未授权角色访问返回 403。

必须执行：

```powershell
$env:JAVA_HOME = "D:\Program Files\Java\oracleJdk-21"
& "C:\Program Files\Java\apache-maven-3.9.11\bin\mvn.cmd" -f services/platform-api/pom.xml -s scripts/custom-maven-settings.xml -DskipTests compile
```

运行本任务新增定向测试和受影响的竞赛既有测试。环境允许时验证空库 Flyway；不能验证时不得声称已通过。

最后执行：

```powershell
git diff --check
```

验证所有新增/修改 Java、SQL、Markdown 均为 UTF-8 without BOM。

## 六、禁止项

本任务不得实现：

- 成绩来源、实操评分单、总分计算或排名快照；这些属于 `COMP-SCORE-02`。
- 具体试卷、题目快照、考试场次、答题、交卷或自动判分。
- 裁判评分工作台、认定实操评分适配、解锁重评。
- 管理端 React 页面。
- 选手 H5。
- 腾讯云/阿里云短信修改。
- 仿真回调。
- 奖项、公示、仲裁、撤销重排。
- 并发优化、Redis 限流、压测。
- 赛制 A。
- 已应用 Flyway 修改。
- 无关重构、依赖升级、全仓格式化。
- Git 提交、推送或 PR，除非用户另行明确要求。

## 七、完成定义和最终回复

只有同时满足以下条件才可声明完成：

- 赛事工种使用稳定 ID 并关联现有题库工种。
- 选手具备可审计的赛事工种归属和未匹配状态。
- 赛段和资格审核已有受权限保护的 API。
- 赛制 B 规则可创建、校验、发布锁定和版本化。
- 成绩域就绪检查可返回结构化缺口并阻断不完整范围。
- Flyway 版本已从实际历史核对，迁移和实体符合 BaseEntity。
- 编译、定向测试和 `git diff --check` 通过。
- 未进入禁止范围。

最终回复必须包含：

1. 完成/部分完成/阻塞的明确结论。
2. 实际实现范围。
3. 新增和修改文件清单。
4. Flyway 版本核对证据。
5. API 清单和权限码。
6. 数据迁移与历史赛事兼容策略。
7. 编译、测试命令、退出码和通过数量。
8. 未通过项及原因。
9. 与本提示词逐项对应的验收结果。
10. `git status --short` 与本任务相关 `git diff --stat`。
11. 下一步是否已具备执行 `COMP-SCORE-02` 的条件，但不要自行开始下一任务。

完整返回以上结果，供原任务中的审查者检查是否发生业务、架构、权限、迁移或范围偏离。
