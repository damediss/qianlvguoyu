# 客观题答案 AI 检查（一期）实施方案

> 文档版本：v1.5（JEV 连接配置集中入库与共享配置适用范围）  
> 编写日期：2026-10-26  
> 最近修订：2026-10-28  
> 状态：一期编码与自动化验证已完成（2026-10-28）；v1.5 配置集中入库已实施并通过目标测试（2026-10-29）；200 道标定、真实 JEV 探针与空库迁移待执行，标定未过不得放量  
> 菜单位置：课程管理 → 支持保障 → 资源管理 → **客观题答案AI检查**  
> 数据主表：`exam_exam_detail`  
> 决策模型：`ateve-jev-v1`（TokenDance TypeSafe SystemOne，非 OpenAI 协议）  
> 实施读者：本文件按“可交给另一个编码模型直接实施”的粒度编写。文中的“必须/禁止”均为约束，不是建议。

---

## 0. 已确认决策与文档使用方式

### 0.1 用户已确认的五项口径

| 决策项 | 已确认口径 |
|---|---|
| 菜单名称 | **客观题答案AI检查**；一期仅单选题、多选题、判断题 |
| 结果闭环 | **AI 检查 + 人工复核 + Excel 导出**；记录复核状态，但不修改原题库答案 |
| 低置信与解释 | 仅转人工；展示选项概率；**不接第二个生成式模型，不生成文字理由** |
| 首轮放量 | 先用 **100～200 道人工标定样本**校准；本方案按 200 道设计，达标后再放开 |
| 单批上限 | **2,000 道**；超出时引导缩小筛选范围或改为逐题勾选 |

### 0.2 一期不可被实施者擅自改变的边界

1. AI 只负责筛查，不作最终事实裁决；页面不得出现“AI 已证明答案正确/错误”的文案。
2. **禁止自动或人工一键写回 `exam_exam_detail.answer`**。一期的人工复核只写检查结果表的复核字段。
3. 不接入 DeepSeek、GLM 等第二模型兜底；任何 JEV 失败、低置信、结构异常均不得悄悄改走生成式 LLM。
4. 不输出 AI 文字解析。JEV 的选项、概率和置信度就是一期全部 AI 产出。
5. 不支持图片、音频、视频依赖题；不做主观题；不做填空、简答、计算、案例分析。
6. 单批最多 2,000 道是服务端硬限制，前端隐藏入口或改请求参数都不能绕过。
7. 首批 200 道标定未通过 §17 的质量门槛时，只能保持试点，不得以“接口已经调通”为由验收放量。
8. 内容质量与 AI 置信度是两个独立维度；suspect/invalid 必须进入人工复核，不能用高置信一致覆盖格式警告，也不能把格式疑似直接伪造成模型低置信。
9. `suggestedText` 只供人工参考，禁止自动替换题库、禁止把科学计数法转换候选悄悄送入 AI 代替原存储文本。

### 0.3 实施时必须先核对的仓库事实

- 后端规则：根 `AGENTS.md` + `services/platform-api/AGENTS.md`。
- 前端规则：`apps/admin-web/AGENTS.md`，尤其是标签页轮询、构建和无 MSW 约束。
- 当前 `PageResponse` 的真实 JSON 是 **`{items,page,pageSize,total}`**，不是 `records`。
- `exam_exam_detail.id` 是 `INT`，可安全按 number 下发；本方案新建批次/明细/审计表使用雪花 `BIGINT`，前端必须按字符串接收。
- 雪花 ID 字符串化**不是全局配置**（`JacksonConfig` 只处理日期时间）：VO 中所有 Long 型雪花字段（batchId/itemId/runId/createdBy 等）必须显式加 `@JsonSerialize(using = ToStringSerializer.class)`（certification/competition 既有 VO 的惯例），禁止裸 Long 输出，否则前端精度丢失（AGENTS #24）。
- `exam_course.level` 是 0～4：0=五级、1=四级、2=三级、3=二级、4=一级。对外统一 `levelCode=5-level`，即 5～1。
- `exam_exam_detail.source`：0=内部、1=外部、2=AI 生成、3=赛前复习。本页面必须展示全部四种，不得沿用某些页面刻意隐藏 source=2 的局部口径。
- 多租户过滤依赖 MyBatis-Plus 插件；普通业务代码不得手拼租户条件。Worker 必须自行设置并清理 `TenantContextHolder`。
- 禁止 MP 静态 `Db.saveBatch`；批量插入使用自定义 Mapper 分批 SQL。
- 迁移版本实施当天必须重新查测试、生产两库 `flyway_schema_history`，不得只照抄本文取号。
- HTTP 业务写入可使用既有 MP 审计填充；但 `AuditMetaObjectHandler` 会调用 `SecurityContext`，所以本功能的异步写库必须使用 §11.6 的显式审计参数 SQL，不得在 Worker 中依赖 BaseEntity 自动填充。

### 0.4 v1.1 独立复核修订记录

本版在初稿后做了一轮独立静态评审，已针对性补齐：

- `run_id + owner_id + lease_until` 编排租约，以及 `attempts` 明细认领代次，防蓝绿/崩溃接管时旧 Worker 晚到覆盖和重复计数；
- 熔断锁存、`pausing → failed`、failed 在飞行行恢复、所有线程池拒绝点的持久化补偿；
- 多选逐块成功/失败审计回调，choice 与概率分布一致性校验；
- 一期仅平台管理员检查本租问题库的服务端硬约束，不能只靠分配权限码扩大数据范围；
- 复核显式版本号、token 精确/估算分列、预览与创建超限差异、标定指标零分母规则。

### 0.5 v1.2 XLSX 场景补充（owner 已确认纳入）

针对“xlsx 数值格式导致科学计数法、精度或显示错误”的场景，本版将方案从单纯答案检查扩展为三段闭环：

1. **导入链防错**：统一单元格读取器，增加导入前文件预检，普通数值禁止强转 int，日期/百分比/公式按类型处理；硬错误不入库，软警告必须可见。
2. **存量内容质量识别**：新增独立于 AI 置信结论的 `contentQuality/contentWarnings`，用确定性规则识别 Excel 错误值、科学计数法疑似、异常精度和占位符。
3. **AI 答案检查**：沿用独立作答与置信分层，但任何内容质量 suspect/invalid 都进入人工复核，不允许“高置信一致”掩盖显示错误。

明确边界：格式识别不能还原已经丢失的信息；`0.5 → 0` 这类已落库截断无法靠正则逆推。没有原始 xlsx 时只能标记和人工修正，禁止自动恢复数值或批量改题库。

### 0.6 v1.3 关联方案协同：解析存疑（标定集共用与复核四态对齐）

关联文档：`docs/04_requirements/试题解析prompt_v2.2与答案存疑_实施方案.md`（其附录 B 为完整复用分析）。该方案在学员点击「查看解析」的在线链路中，由生成式 LLM 先独立解题再对照给定答案，能指出具体矛盾并给出原理依据时落 `ai_generation_draft`（status=`answer_disputed`）形成存疑工单。本方案与其是同一"题库答案质量治理"的离线/在线两条链，统一以下两条口径：

1. **标定集共用**：§17 的 200 道专家真值标定集同时作为解析存疑功能的质量验收集，用法见 §17.5。这不改变本方案任何放量门槛；存疑侧指标单独记录，不得以存疑结果替代本方案标定，也不得以本方案 AI 结论直接充当存疑侧真值。
2. **复核四态对齐**：§5.6 的 `confirmed_correct/needs_edit/deferred/unreviewed` 四态语言同样适用于解析存疑工单的人工复核；两边 SOP 一致——AI 只筛查不写回，人工在既有题库编辑页修正（解析存疑侧另有"人工填写 resolve 列"出口，见其附录 A）。后续若做统一"答案复核工作台"，按 `detail_id` 聚合本方案 item 与存疑 draft，用查询/视图聚合，**不合并物理表**。

协同边界（与 §0.2/§20 同等约束）：不因此引入第二模型或 AI 文字理由；不让生成式 LLM 参与本方案判定；不改本方案的 sourceHash、阈值、写回边界与权限角色；解析存疑侧亦不得改动其已上线 sourceHash 算法。

### 0.7 v1.4 实施前复核修订（2026-10-27，对照代码与生产库逐项核查）

1. §4.1 删除 `zi_yuan_guan_li` 的 pageMap 注册要求（生产该 code 是 directory 纯目录），并标注 ResourceCenterPage 卡片仅测试环境可达、生产唯一入口为侧栏新菜单；
2. §12.1 候选迁移号改为实施当天按双库 MAX(version)+1 取号（原候选 `V20261026_001` 已过时，仓库已存在 `V20261027_006`）；
3. §0.3 补充雪花 ID 的 `@JsonSerialize(using = ToStringSerializer.class)` 强制要求（无全局 Long→String 配置）；
4. §7.2 澄清 JOIN 租户语义（插件过滤、禁止当前租户字面量、允许列间等值）。

本次复核已验证与方案一致、实施可直接照做的仓库事实：菜单 `zi_yuan_guan_li` 现存（支持保障下）、`sort_order=6` 为空位、role 2000=PLATFORM_ADMIN（tenant 10159）、`exam_exam_detail` 及关联表结构与 §1.3 快照一致（题型合计 166,155 行精确吻合）、答案 JSON 主流形状为 `[{content,isCorrect}]`、导入接口权限 `exam:question:create` 与返回 `ApiResponse<Integer>`、两处 `getCellString` 的 `(int)` 强转位置（`ExamExamDetailApplicationService:1202`、`CompanyQuestionApplicationService:431`）、`JevDecisionClient` 尚未禁用内容压缩（§9.4 新增必要）、`AuditMetaObjectHandler` 调用 SecurityContext（§11.6 异步写库约束成立）、`@InterceptorIgnore`/jsoup/`FollowTaskBankQuestionConverter` 均有先例。导入返回契约变更影响面封闭：前端仅 `QuestionBankPage`/`CompanyQuestionPage` 两个消费方，旧 PHP 端不调用这两个接口。另：生产有 175 行无 `isCorrect` 的存量题，按方案落 `CORRECT_OPTION_MISSING` 结构异常属预期行为，不是缺陷。

### 0.8 v1.5 JEV 连接配置集中入库（owner 已确认）

1. `OBJECTIVE_ANSWER_CHECK_JEV_MODEL` 的**非敏感连接配置**集中放入 `exam_faq_model_config`：`status/base_url/api_format/model_id/jev_concurrency/jev_read_timeout_seconds`。
2. `api_key` 字段固定写入占位值 **`模型API Key走配置文件`**；它只是说明性占位，**永远不得作为请求密钥发送**。真实密钥只从服务器部署配置/环境变量 `OBJECTIVE_ANSWER_CHECK_JEV_API_KEY` 读取。
3. `exam_faq_model_config` 是共享配置表。新增 `feature_scope` 区分适用范围，避免本行启用后污染 FAQ 转换、理论生成、技能生成等通用模型下拉。
4. 原功能迁移若已应用，本变更必须写成**新的后续迁移**，禁止修改已应用迁移；取号实施当天查两库 `MAX(version)+1`。

---

## 1. 背景、可行性与实测数据

### 1.1 目标

帮助题库管理员按 **工种、等级、来源、题型** 找到一组试题，使用 `ateve-jev-v1` 独立作答，再与题库原参考答案比对，快速得到：

- 原答案与 AI 答案是否一致；
- 模型对原答案的支持程度；
- 模型对自身答案的置信度；
- 题干和选项是否存在 XLSX 导入/显示异常；
- 哪些题应优先进入人工复核。

一期交付的是**筛查工作台**，不是自动判卷专家，也不是答案修改工具。

### 1.2 技术可行性

现有 `JevDecisionClient` 已具备 SystemOne 的单子问题调用、超时、429/529/5xx 重试、401/422 分类和响应零信任解析骨架。协议文档及既有探针表明：

- `choice` 可以返回候选键、概率分布和 confidence；
- 一次请求可包含多个共享 `state` 的问题；
- `criteria` 至少 2 项、最多 255 项；
- `ateve-jev-v1` 与既有 `bocha-jev-v1` API 契约同构；
- 输入还受 `expanded_input_tokens <= 32,768` 限制；
- 响应可能无 `metadata`，因此 `confidence_definition` 必须允许为空；
- `usage.input_tokens` 是展开后的口径，不能与生成式模型 token 简单横向比较。

因此，单选/判断可直接映射为一个 `choice`；多选可映射为每个选项一个“应选/不应选”的二元 `choice`，再合并。**客户端需要扩展多子问题请求，不是零代码复用。**

### 1.3 2026-10-26 生产库只读核查

| 指标 | 实测结果 | 设计影响 |
|---|---:|---|
| 单选/多选/判断存活题总数 | 166,155 | 必须异步、限量、分批，禁止 HTTP 同步跑完 |
| 三种题型答案为 JSON 数组 | 全部 | 可以做严格结构化预检 |
| 无有效课程关联 | 46,127（约 27.8%） | 不能按工种/等级归类；但未限定这两个筛选时仍可检查 |
| 单选选项数 | 0、2～9、11 均有；主体为 4 | 需要结构预检，不能只假定四选项 |
| 多选选项数 | 2～9；主体为 5 | 采用选项分块，避免子问题展开超预算 |
| 判断题选项 | 对/错、正确/错误，以及空占位和异常内容 | 必须做语义归一，不能机械取前两项 |
| 零正确项、单选多正确项等 | 少量存在 | 确定性标为结构异常，不调用 AI |
| 音频非空 / 视频非空 | 各 31 行 | 保守标为“暂不支持”，不得删掉媒体后硬送模型 |
| 题干最大字符 / 答案最大字符 | 1,132 / 610 | 当前文本长度可控；仍须保留超长与 422 防线 |

说明：上述是设计时点的数据快照，不是迁移要修改的数据，也不要求本需求先治理这 46,127 道无课程题。

### 1.4 业务可行性的限制

1. JEV 是决策模型，不是带检索证据的专家系统；不会查职业标准原文，也不会给出推理过程。
2. 中文职业技能题的作答准确率尚未由本功能验证；知识归属修正场景的历史精度不能替代本场景标定。
3. 模型可能对坏题仍强行给一个高置信选项，因此“高置信”不能等于“正确”。
4. 多选逐项判断不能完美表达选项间的组合约束，必须单独统计多选题标定指标。
5. 置信度可能随上游路由和 checkpoint 改变；响应模型或 confidence 口径变化后必须重新标定。

**结论：接口与工程上可行；业务上只能在“人工复核兜底 + 首轮标定通过”的前提下放量。**

### 1.5 XLSX 导入失真对本功能的影响

模型只能看到落库后的题干和选项，无法看到 Excel 原始单元格及其格式。如果 `0.5` 被写成 `0`、科学计数法被当成普通文本，AI 检查的是一道内容已改变的题：

- AI 选择其他选项时，可能形成高置信不一致，但根因是内容失真而非答案键错误；
- AI 仍选同一选项序号时，可能形成高置信一致，但学员看到的依然是错误内容；
- 所以“答案是否正确”和“题目内容是否可信”必须分别判定。

当前 dev 代码中，`ExamExamDetailApplicationService.getCellString` 与 `CompanyQuestionApplicationService.getCellString` 都存在 NUMERIC 直接 `(int)` 强转，小数选项会被截断；这是必须在 AI 批次上线前修复的导入缺陷，不能寄希望于事后模型发现。

2026-10-27 只读模式扫描生产三种题型：ASCII `E/e` 科学计数法与常见 Excel 错误值未命中，但发现 133 个长小数选项记录，其中不少像合法循环小数。该结果说明两件事：全角符号和其他文本变体不能靠一次扫描排除；长小数只能作弱疑似，不能批量判错或自动格式化。

---

## 2. 统一语言与结果口径

| 名词 | 本方案唯一含义 |
|---|---|
| 原答案 | 批次创建时，从 `exam_exam_detail.answer` 快照解析出的正确选项标记 |
| AI 答案 | 模型在看不到原答案和解析时独立作出的选项判断 |
| 原答案支持度 `referenceConfidence` | 模型概率对原答案各选项标记的支持程度，按最弱选项取最小值；不是正确率 |
| AI 答案置信度 `aiConfidence` | 模型对自身判断的 confidence；多选取各选项 confidence 的最小值 |
| 最小概率差 `minMargin` | 最优候选概率与次优候选概率之差；多选取各选项中的最小值 |
| 高置信 | 同时满足 AI 置信度阈值和概率差阈值；高置信一致还要求原答案支持度过阈值 |
| 疑似原答案错误 | 原答案与 AI 答案不同，且 AI 判断为高置信；只是优先复核信号 |
| 结构异常 | 确定性预检已能判定无法可靠检查，不调用模型 |
| 调用失败 | 已尝试模型但未得到可接受的完整结论；区分可重试与不可重试 |
| 内容质量 `contentQuality` | `clean/suspect/invalid`；描述题干/选项是否存在格式或导入异常，与 AI 答案是否一致无关 |
| 内容警告 `contentWarnings` | 确定性规则产生的结构化明细：规则、题干/选项位置、匹配文本、转换候选和说明 |
| 待人工复核 `needsReview` | 结果已落定（resultCode 非空）且 `reviewStatus=unreviewed`，并满足结果属于 mismatch_high/low_confidence/invalid/failed 或 contentQuality≠clean；pending/processing 不算 |

### 2.1 五类展示结果

| `resultCode` | 页面文案 | 进入条件 |
|---|---|---|
| `consistent_high` | 高置信一致 | 原答案=AI 答案；AI 置信度、概率差、原答案支持度均过阈值 |
| `mismatch_high` | 高置信不一致 | 原答案≠AI 答案；AI 置信度和概率差过阈值 |
| `low_confidence` | 低置信复核 | 已完成 AI 判断，但不满足上述高置信条件；保留一致/不一致事实 |
| `invalid` | 答案结构异常 | 预检失败或题型/媒体暂不支持；未调用模型 |
| `failed` | 检查失败 | 模型或响应处理失败；记录 `retryable` |

`consistent_high` 和 `mismatch_high` 之外还必须分别保存 `consistent=true/false`，避免低置信行丢失“是否一致”信息。

**内容质量不新增第六个 AI resultCode，也不改写 AI confidence。** `contentQuality=invalid` 时按确定性预检进入 `resultCode=invalid` 且零模型调用；`suspect` 时仍可执行 AI，结果码照常计算，但 `needsReview=true`，页面同时显示“AI 检查结论”和“格式疑似异常”两个标签。人工复核完成后，该行离开默认复核队列，但 contentQuality 和 AI 原始结论均保留作历史。

---

## 3. 端到端流程与线程边界

```text
HTTP 线程
  1. 查询筛选项 / 试题分页
  2. 选择逐题 IDs 或“当前筛选全部”
  3. selection/preview：后端重新查询，依次做结构预检 + 内容质量识别，返回数量、格式警告与预计调用量
  4. POST batches：校验平台管理员并捕获 tenantId/userId
  5. 短事务：重新选题 → 写批次（新 run_id/租约 + modelId/JEV baseUrl/并发/读超时快照）+ 快照明细（含 invalid 行）→ 提交事务
  6. 事务提交后投递编排池，立即返回批次 ID；拒绝时持久化 failed 并释放活动槽
                         │
                         ▼
objective-answer-check- 编排线程
  7. try/finally 设置并清理租户；每 15 秒按 run_id 心跳续租 60 秒
  8. 每行认领前以“status=running + run_id 匹配 + 租约有效”条件更新，派生 claimAttempt
                         │
                         ▼
objective-answer-check-worker- Worker
  9. 每行 try/finally 设置并清理租户；contentQuality=invalid 直接落异常，suspect/clean 才调 AI
 10. 调 ObjectiveAnswerCheckAiService（ai 包）；单选/判断一次逻辑调用，多选按每 5 个选项分块
 11. 每个逻辑块结束都通过 auditSink 回调，独立短事务写成功或失败审计及 token
 12. 整题聚合 AI 结论但保留独立 contentQuality/warnings，以“processing + run_id + claimAttempt”条件写结果
 13. 只有当前认领写入成功才更新结果与内容质量计数；pending=processing=0 时当前读收尾
```

硬性要求：

- HTTP 线程只捕获一次 `tenantId/userId`，之后全部经参数传递；异步链禁止再调 `SecurityContext`。
- 批次和明细必须先提交可见，再投递 Worker，禁止事务内写库后立即投递造成抢跑。
- **编排线程和每个 Worker 都必须在 `finally` 中 `TenantContextHolder.clear()`**。
- 每行及每个审计回调各用一个短事务；禁止整批 2,000 行包在一个长事务里。
- 暂停只阻止新派发，不强杀已开始的 HTTP 调用；失败一旦锁存，后来的成功不得撤销。
- 批次收尾、暂停与重试的状态复核必须使用当前读/条件更新，不能依赖 RR 快照中的旧计数。
- 过期租约才能被其他节点接管；旧 `run_id` 的派发和旧 `claimAttempt` 的结果写入必须全部失效。晚到调用可保留实际用量审计，但不得改变明细结论或结果计数。

---

## 4. 菜单、权限与前端文件

### 4.1 菜单与路由

- 菜单 `code`：`objective_answer_check`
- 菜单名称：`客观题答案AI检查`
- 菜单类型：`menu`
- 路径：`/question-bank/resources/objective-answer-check`
- component：`question_bank/ObjectiveAnswerCheckPage`
- 页面主组件：`apps/admin-web/src/pages/question_bank/ObjectiveAnswerCheckPage.tsx`
- API：`apps/admin-web/src/pages/question_bank/objectiveAnswerCheckApi.ts`
- 页面类型可同目录建 `objectiveAnswerCheckTypes.ts`，不要塞进全局 `types/exam.ts` 造成既有文件冲突。

在 `main.tsx`：

1. 懒加载 `ObjectiveAnswerCheckPage`；
2. `pageMap.objective_answer_check = <ObjectiveAnswerCheckPage />`；
3. 保留 `question_resource: <ResourceCenterPage />`；
4. **不得把 `zi_yuan_guan_li` 注册进 pageMap**：2026-10-27 生产库核实该 code 是 `menu_type=directory` 的纯目录，不是页面；注册会改变目录点击与深链恢复行为。

在 `ResourceCenterPage.tsx` 平铺新增“客观题答案AI检查”卡片，点击 `setSelectedKey('objective_answer_check')`。不要把入口收进下拉菜单。**入口边界（生产库核实）**：生产没有任何菜单指向 ResourceCenterPage，该卡片仅在测试环境（`question_resource` 菜单存在时）可达；**生产的唯一入口是侧栏新菜单“客观题答案AI检查”**，验收不得以“生产也有卡片入口”为准。

### 4.2 权限码

| 权限码 | 用途 |
|---|---|
| `exam:objective-answer-check:view` | 筛选项、试题分页、预览、批次/结果/详情查询 |
| `exam:objective-answer-check:submit` | 创建批次、暂停、继续、失败重试 |
| `exam:objective-answer-check:review` | 单行人工复核与撤销复核 |
| `exam:objective-answer-check:export` | 导出批次结果 |

一期只给平台管理员 `role_id=2000` 授权，且**一期数据范围固定为“平台管理员检查当前租户全部存活题库”**。除 `@PreAuthorize` 权限码外，所有 HTTP ApplicationService 入口还必须调用统一的 `requirePlatformAdmin()`：通过 `PlatformAdminSupport.isPlatformAdmin(principal.getRoleCodes())` 判定，非平台管理员即使有同名权限码也返回 403。异步 Worker 不做角色判断，也不在异步链读取 SecurityContext；授权只在 HTTP 入口完成。

这是一期的数据权限边界，不是可有可无的临时限制。后续若要开放机构/企业角色，必须先另案定义 `orgId/isPrivate/userId` 数据范围谓词、外发范围和越权测试，并修订本文；**禁止只在权限管理页面给其他角色分配这四个权限码就上线**。不得因为用户拥有题库查看权限就隐式允许其发起外部 AI 批次。

---

## 5. 页面交互设计

### 5.1 总体结构

一个页面、三个页签：**选择试题 / 检查结果 / 检查批次**。不新增隐藏路由。

```text
┌ 客观题答案AI检查 ────────────────────────────────────────────────┐
│ AI 检查用于快速筛查，不能替代专家审定；检查不会修改题库答案。     │
├ 选择试题 | 检查结果 | 检查批次 ──────────────────────────────────┤
│ 工种 [可搜索选择] 等级 [选择] 来源 [选择] 题型 [多选] [查询][重置]│
│ 已逐题勾选 36 道 / 已按筛选选择 1,280 道   [选择筛选全部][清空]  │
│ ┌ 试题表格：勾选 | 题干 | 题型 | 来源 | 工种/等级 | 原答案 | 预检 ┐│
│ │ ...server pagination...                                      ││
│ └──────────────────────────────────────────────────────────────┘│
│ [预览并开始 AI 检查]                                             │
└─────────────────────────────────────────────────────────────────┘
```

### 5.2 筛选项

1. **工种**
   - 可搜索 TreeSelect/Select，展示完整父路径，解决同名职业节点与同名子工种歧义。
   - 只返回经“知识点→课程”关联后能查到存活客观题的工种，避免只有课程、实际无题或选中后必然为空的工种；无课程关联的题不用于反推工种。
2. **等级**
   - 可单独使用，也可与工种联动；选项为五级/初级工、四级/中级工、三级/高级工、二级/技师、一级/高级技师。
   - 选择工种后只显示该工种实际存在的等级；清空工种恢复全部等级。
3. **来源**：内部、外部、AI 生成、赛前复习；单选、可清空。
4. **题型**：多选，只含单选/多选/判断；默认三项全选。
5. 不额外增加审核状态过滤；本功能本来就是检查答案，不能只查“已审核正确”的题。

### 5.3 两种选择模式，互斥且可见

**A. 逐题勾选（默认）**

- 服务端分页，支持 20/50/100 条每页。
- 跨页保留勾选；切页不清空。
- 使用 `preserveSelectedRowKeys`，但筛选条件变化时必须显式清空全部勾选与预览缓存。
- 切换工种、等级、来源、题型前若已有选择，提示“筛选条件变化将清空已选试题”。

**B. 当前筛选全部**

- 点击后先查询总数；总数 >2,000 时拒绝并提示缩小范围。
- 成功后页面显示“已按当前筛选选择 N 道”，进入 `filter` 模式；此时禁用逐行勾选，提供“改为逐题勾选”按钮。
- 创建批次时由后端按同一 scope 重新取数，不信任前端传来的 ID 列表。
- 筛选条件变化会退出该模式并清空选择。

两种模式的预览和创建使用同一个 `SelectionReq`，禁止前端分别拼两套筛选逻辑。

### 5.4 预览与开始确认

点击“预览并开始 AI 检查”后，后端返回：选择题数、各题型数量、可检查数、结构异常数、内容格式 suspect/invalid 数、预计逻辑调用数、单批上限、当前模型、置信阈值和概率差阈值。内容 suspect 与 AI 可检查数不是互斥口径，弹窗必须分别展示，不能简单相加。

确认弹窗必须平铺展示：

```text
本次选择：1,200 道（单选 600 / 多选 400 / 判断 200）
可直接检查：1,176 道；结构异常：24 道（仍会留在结果中）
内容格式提醒：疑似 18 道（仍参与 AI 但必复核）；确定异常 6 道（不调 AI）
预计逻辑调用：1,236 次（多选按每 5 个选项分块，失败重试另计）
模型：ateve-jev-v1
高置信门槛：AI 置信度 ≥80%，概率差 ≥20%
说明：AI 不读取原答案和解析；结果不会自动修改题库。
```

- “预计逻辑调用数”不是 token 或费用承诺。
- 创建按钮在请求期间禁用，防双击重复批次。
- 创建成功跳到“检查结果”并选中该批次；不显示技术参数名。

### 5.5 检查结果页

顶部是当前批次摘要与进度，以下为可点击统计卡：

`高置信不一致 / 低置信复核 / 高置信一致 / 答案结构异常 / 检查失败 / 内容格式待核 / 全部`

“内容格式待核”是独立内容质量过滤，不与五类 AI 结果互斥；点击后按 `contentQuality IN (suspect,invalid)` 查询。

- 批次完成后默认过滤为“需人工复核”视图：`resultCode IS NOT NULL AND reviewStatus=unreviewed AND (resultCode IN(mismatch_high,low_confidence,invalid,failed) OR contentQuality<>clean)`，排序为 `mismatch_high` 优先、contentQuality=invalid/suspect 次之，再按 AI 置信度倒序、明细 ID 升序。pending/processing 即使为 suspect 也不进入可执行复核队列。
- 用户可单独点击任一统计卡切换精确过滤；人工复核完成后该行离开默认队列，但可从“全部”或复核状态过滤重新找到。
- 进行中每 3 秒刷新进度；使用 `usePaneActive()` 驱动 `refetchInterval: active && pollingStatus ? 3000 : false`，只有标签页 active 且批次为 pending/running/pausing 时轮询。
- 只要状态离开 pending/running/pausing，无论进入 **paused、failed 还是 done**，都主动失效批次、结果列表和统计查询；不能只停轮询而不刷新尾部数据。paused/failed 可继续执行，不把它们误当无需刷新的“终态”。
- 手动“刷新”按钮必须有 `batchId` 守卫；react-query 的 `refetch()` 会绕过 `enabled:false`，禁止裸调用。

表格列：

| 列 | 展示要求 |
|---|---|
| 题干 | 纯文本、最多两行，完整内容进详情；禁止渲染原始 HTML |
| 题型/来源 | 中文标签 |
| 工种/等级 | 无关联时显示“-”，不是结构异常 |
| 原答案 | 字母 + 选项文本；判断显示正确/错误 |
| AI 答案 | 失败/异常显示“-”；多选按原选项顺序展示 |
| 原答案支持度 | 百分比；tooltip 说明“模型对原答案标记的支持程度，不是正确率” |
| AI 置信度 | 百分比，并显示概率差是否过线 |
| 检查结果 | 文字 + 颜色，不能只靠颜色区分 |
| 内容质量 | 正常/格式疑似异常/格式确定异常；与 AI 结果分开展示，suspect 不得藏进 confidence tooltip |
| 人工复核 | 未复核/确认原答案无误/需要修改/暂缓处理 |
| 操作 | 详情、复核；操作平铺，不收进下拉 |

### 5.6 详情抽屉与人工复核

详情抽屉宽度建议 760px，依次展示：

1. 题干、工种、课程、等级、知识点路径、来源、题型；
2. 原解析（仅供人工看，明确“未发送给 AI”）；
3. 逐选项对照表：选项、原答案标记、AI 判断、AI confidence、原标记支持概率、最优/次优概率、概率差；
4. 内容质量明细：高亮具体题干/选项、ruleCode、matchedText、严重度、白话说明；科学计数法可展示 suggestedText 转换候选，并明确“仅供人工核对，未替换原值、未发送给 AI”；
5. 原答案、AI 答案、两个置信度、检查结论和 `decisionWarning`；
6. 模型、请求模型与响应模型、检查时间、操作人、逻辑调用 ID、token、耗时、失败原因；
7. “题库已变更/已删除”提示：由当前 `type+content+answer` 的规范哈希与快照比较得出（NULL 与空串使用不同长度前缀编码）；仅题型变化也必须检出。复核仍针对本次快照。

人工复核只允许单行操作：

- `confirmed_correct`：确认原答案无误；
- `needs_edit`：标记需要修改；
- `deferred`：暂缓处理；
- `unreviewed`：撤销复核。

这四态同时是解析存疑工单（`answer_disputed`）的复核语言，见 §0.6；跨功能统一复核时按 `detail_id` 识别同一道题，不合并存储。

可填最多 500 字备注。复核弹窗再次说明“这里只记录处理意见，不会修改题库”。`needs_edit` 不产生答案编辑入口，也不跳题库编辑页，避免一期边界被悄悄扩大。

复核请求携带整数 `expectedVersion`，后端以 `id + version + 终态` 条件更新并令 `version=version+1`；不得使用秒级 `updatedAt` 充当乐观锁。明细每次认领、结果写入、重试复位和复核都递增 version。并发复核冲突返回“该记录已被其他人更新，请刷新后重试”。

### 5.7 检查批次页

列表字段：批次 ID、创建时间、创建人、筛选摘要、模型、阈值、总数、进度、五类结果数、内容格式 suspect/invalid 数、状态、模型调用数、精确/估算输入 token、耗时。

操作：

- 查看结果；
- 暂停（running/pending）；
- 继续（paused，或 failed 且仍有 pending）；
- 重试失败项（存在 retryable failed）；
- 导出。

不设置“删除批次”。历史永久保留，便于审计和前后对照。

### 5.8 文案与空状态

统一使用业务白话：

- “AI 检查用于快速筛查，不能替代专家审定；检查不会修改题库答案。”
- 未开通：“答案 AI 检查暂未开通，请联系管理员。”
- 超上限：“本次最多检查 2,000 道，请缩小筛选范围或改为逐题勾选。”
- 部分失败：“部分试题未完成检查，可重试失败项；失败不影响已完成结果。”
- 无关联课程：“该题没有课程归属，不影响 AI 检查，但不能按工种/等级筛选到它。”

禁止在 UI 出现 `criteria`、`top1Prob`、`theta`、`JevValidationException`、内部文档名或堆栈。

---

## 6. 后端模块与类职责

新包：`com.guoyu.platform.exam.answercheck`

| 类 | 职责 |
|---|---|
| `ObjectiveAnswerCheckController` | 唯一 HTTP 入口；参数校验、权限、审计注解；不写 SQL |
| `ObjectiveAnswerCheckApplicationService` | 筛选项、试题分页、选择预览、批次/结果查询、人工复核、导出编排 |
| `ObjectiveAnswerCheckBatchService` | 创建批次、异步编排、租约/心跳、认领 fencing、暂停/继续/重试、熔断锁存、过期接管 |
| `ObjectiveAnswerCheckDomainService` | 唯一结果判定入口；计算一致性、高置信与 `resultCode` |
| `ObjectiveQuestionParser` | 严格解析题干/答案、归一选项、确定性预检；禁止兜底编造答案 |
| `exam.support.ExcelCellTextReader` | XLSX 单元格统一读取：普通数值 plain decimal、日期/百分比/公式分派、格式与坐标告警；两条试题导入共用 |
| `exam.support.QuestionContentQualityInspector` | 题干/选项内容质量唯一实现；导入预检与 AI 批次共用规则，禁止两处各写一套正则 |
| `ObjectiveAnswerCheckScopeMapper` | 统一 scope 查询、IDs 查询、批量快照插入；分页和创建共用 |
| `ObjectiveAnswerCheckExportService` | 基于快照生成 xlsx，不重新组题、不改写 AI 结果 |
| `ObjectiveAnswerCheckProperties` | 业务开关、并发、阈值、批量上限和文本上限；不含 JEV 密钥配置 |
| 三个 Entity + Mapper | `exam_objective_answer_check_batch/item/call_log` |

AI 包：

| 类 | 职责 |
|---|---|
| `ai.service.ObjectiveAnswerCheckAiService` | 业务依赖的窄接口；descriptor 只暴露模型编码/模型名/凭据就绪/可快照并发超时；输入归一题目与调用上下文，输出逐选项判断；每个逻辑块都发审计事件 |
| `ai.infrastructure.ObjectiveAnswerCheckJevConfigResolver` | 仅 ai 包内解析 `exam_faq_model_config` 专用行 + `OBJECTIVE_ANSWER_CHECK_JEV_API_KEY` 环境变量，生成含 baseUrl/apiKey 的内存运行时配置；不把真实密钥交给 exam 业务层 |
| `ai.infrastructure.JevObjectiveAnswerCheckAiService` | 构造 SystemOne 请求、分块、调用与聚合；不知道 Controller，不返回密钥 |
| `ai.dto.ObjectiveAnswerCallOutcome` | 单个逻辑块成功或失败的完整审计：callId/chunk/runId/claimAttempt/模型/请求摘要/token/耗时/error |
| `ai.infrastructure.JevDecisionClient` | 增加通用多子问题 `decideBatch`；保留原 `decide` 给 knowledgefix |
| `ai.dto.JevQuestion` | `questionKey/instructions/criteria` |
| `ai.dto.JevBatchDecisionResult` | 子问题结果 Map + usage + 响应 model + confidenceDefinition + duration |
| `ai.dto.ObjectiveAnswerDecision` | 归一后的选项判断、概率、confidence、调用审计信息 |

接口签名统一为：

```java
ObjectiveAnswerCheckAiDescriptor descriptor();
ObjectiveAnswerDecision check(
    ObjectiveAnswerCheckAiRequest request,
    ObjectiveAnswerCheckCallContext context,
    Consumer<ObjectiveAnswerCallOutcome> auditSink);
```

`descriptor()` 返回 `modelCode/modelId/baseUrl/credentialsReady/jevConcurrency/jevReadTimeoutSeconds`，用于预览、创建/继续前校验和批次非敏感运行时快照，**绝不返回 apiKey**。业务 enabled 与 credentialsReady 必须同时满足才可发起新 run；baseUrl/并发/超时快照一旦写入批次，本次运行不随 DB 修改变化。

`context` 携带 tenantId/userId/batchId/itemId/runId/claimAttempt 以及批次固化的 modelId/baseUrl/jevReadTimeoutSeconds，不含 SecurityContext。AI 调用必须使用 context 中的批次快照值，禁止在 Worker 内重新读取 DB 连接配置。AI infrastructure 在每个逻辑块成功或失败后都先发 `auditSink`；批次服务的回调用独立短事务写审计。这样 6/9 选项题即使第 2 块失败，第 1 块成功和第 2 块失败也都有审计，AI 层仍不访问数据库。审计写入失败必须使当前题 `failed/retryable=1` 并计系统性失败，禁止在无审计情况下静默标成功。

`exam.answercheck` 不得自行 new HTTP client、不得直拼 URL、不得读取/打印 API Key。它只依赖 `ObjectiveAnswerCheckAiService`；baseUrl/apiKey 的组装只允许发生在 ai 包 `ObjectiveAnswerCheckJevConfigResolver` 内。

> 不要复用 `FollowTaskBankQuestionConverter` 作为本功能预检器：它对缺失判断答案存在默认兜底，会掩盖本功能需要暴露的结构异常。本方案的严格解析器是独立职责，且在本功能内只有一份实现。

---

## 7. 筛选、选题与预览规则

### 7.1 Scope DTO

```json
{
  "workTypeId": 143,
  "levelCode": 5,
  "source": 0,
  "types": ["single", "multi", "judge"]
}
```

校验：

- `workTypeId` 可空；非空必须 >0。
- `levelCode` 可空；非空只能 1～5，SQL 条件为 `course.level = 5 - levelCode`。
- `source` 可空；非空只能 0/1/2/3。
- `types` 为空按三种题型全集；存在未知值直接 400，不得静默忽略。
- 所有查询只取存活题；不额外限制 `status/checkStatus/finalCheckStatus`。

### 7.2 关联关系

```text
exam_exam_detail d
  LEFT JOIN exam_knowledge_structure k ON d.knowledge_id = k.id
  LEFT JOIN exam_course c             ON k.course_id = c.id
  LEFT JOIN exam_work_type w          ON c.work_type_id = w.id
```

- 关联表均要求 `deleted=0`。JOIN ON 只写 `deleted=0` 与主外键等值；租户过滤一律交给 MyBatis-Plus 插件，**禁止在 SQL 中手拼当前租户字面量**。如需防历史脏数据跨租户错配，只允许列间等值（如 `d.tenant_id = k.tenant_id`），这不属于手拼过滤条件。
- `workTypeId/levelCode` 非空时使用 INNER 语义过滤，未关联题自然被排除。
- 两个条件都为空时，未关联题仍可被 source/type/IDs 选中。
- `d.knowledge_id` 为单值，上述链不会把一道题放大成多行；实现测试必须固化这一点。

### 7.3 SelectionReq

```json
{
  "selectionMode": "ids",
  "detailIds": [101, 102, 103],
  "scope": {
    "workTypeId": null,
    "levelCode": null,
    "source": null,
    "types": ["single", "multi", "judge"]
  }
}
```

- `selectionMode=ids`：`detailIds` 必填并去重；创建时数量须为 1～`effectiveMaxBatchSize`，预览时超出只返回 overLimit。scope 可为空；若使用 scope，后端必须同时套用，防止直接 API 选到范围外试题。
- `selectionMode=filter`：`detailIds` 必须为空，scope 必填；后端重新查询。
- IDs 模式在创建时若存在已删除、跨租户、非支持题型或超范围 ID，整批 409，不静默丢弃。
- `effectiveMaxBatchSize=min(2000, 配置 batchMaxSize)`；配置启动校验必须在 1～2,000。预览、IDs 校验、filter COUNT、前端展示全部使用同一个 effective 值，试点 200 时绝不允许创建 201 道。
- filter 先 `COUNT`，避免一次加载 16 万题。**预览超限返回 200 + `overLimit=true`，其余计数字段可为空；创建超限才返回 413。** IDs 预览超限同样返回 overLimit。
- 预览与创建共用 scope 校验、选题查询和预检方法，但超限处理按上一条区分；创建时的快照以数据库当前值为准。

### 7.4 预览返回

```json
{
  "selectionCount": 1200,
  "overLimit": false,
  "maxBatchSize": 2000,
  "validCount": 1176,
  "invalidCount": 24,
  "suspectContentCount": 18,
  "invalidContentCount": 6,
  "expectedCalls": 1236,
  "typeCounts": {"single": 600, "multi": 400, "judge": 200},
  "invalidReasonCounts": [{"code":"JUDGE_OPTIONS_INVALID","count":8}],
  "modelId": "ateve-jev-v1",
  "confidenceThreshold": 0.8,
  "marginThreshold": 0.2
}
```

`invalidCount` 按题去重，包含结构预检 invalid 与 contentQuality=invalid；`invalidContentCount` 是其中内容质量子集，二者不能相加。`suspectContentCount` 可落在 validCount 内，是“仍参与 AI 但需复核”的独立计数。

`expectedCalls = Σ（单选/判断有效行 1 次）+ Σ（多选有效行 ceil(有效选项数/5)）`。resultCode=invalid 的行不计调用；contentQuality=invalid 已包含在 invalid 行内，contentQuality=suspect 仍计调用，不能与 validCount 再重复相加。

---

## 8. 严格解析与确定性预检

### 8.1 归一结构

```java
record NormalizedObjectiveQuestion(
    Integer detailId,
    String type,
    String stemText,
    String contextPath,
    List<NormalizedOption> options,
    String sourceHash) {}

record NormalizedOption(
    String optionId,       // opt_0、opt_1……按原数组下标生成，永不随展示顺序变化
    int originalIndex,
    String label,          // A、B……；判断题展示时可映射为 T/F
    String text,
    boolean originalCorrect) {}
```

- `optionId` 是 JEV criteria 的内部 key，不使用原答案字母作为数据库关联依据。
- 单选可用 `detailId % optionCount` 做确定性轮换展示顺序，降低位置偏置；响应仍按 `optionId` 映射回原选项。
- 判断题可按 `detailId % 2` 交换 T/F criteria 顺序；多选保持原顺序便于分块。
- 所有 Map 使用 `LinkedHashMap`，保证请求可重复、测试可断言。

### 8.2 文本处理

- 题干和选项使用 Jsoup 提取纯文本，规范化 NBSP、连续空白和大小写仅用于重复检测；展示文本不做大小写转换。
- 先检测 `<img>`、`data:image`、audio/video，再 strip；不得把媒体静默删除后继续检查。
- 不静默截断题干或选项。超过配置上限直接 `TEXT_TOO_LONG`，避免截掉关键条件。
- `resolve` 只用于人工详情，绝不进入 `state/instructions/criteria`。
- 原始 `answer` JSON 也绝不整体发送给 AI，只发送解析后的题干和选项文本。
- `sourceHash` 对 type/content/answer 三段分别做长度前缀后计算 SHA-256：NULL 用 `-1:`，空串用 `0:`，其余用 UTF-8 字节长度；禁止直接字符串拼接造成 NULL/空串或边界碰撞。

### 8.3 预检规则

| 规则 | 结果码 | 是否调用 AI |
|---|---|---|
| type 非 single/multi/judge | `TYPE_UNSUPPORTED` | 否 |
| 题干纯文本为空 | `STEM_BLANK` | 否 |
| content/answer 含图片，或 audio/video 非空 | `MEDIA_UNSUPPORTED` | 否 |
| answer 非 JSON 数组、空数组 | `ANSWER_JSON_INVALID` / `OPTIONS_EMPTY` | 否 |
| 单选/多选选项 <2 或 >16 | `OPTION_COUNT_INVALID` | 否 |
| 单选/多选任一选项文本为空 | `OPTION_TEXT_BLANK` | 否 |
| 归一后选项文本重复 | `OPTION_TEXT_DUPLICATE` | 否 |
| `isCorrect` 只接受 boolean true，或字符串/数值 `true/1/yes`（大小写不敏感） | 解析规则，不是异常 | - |
| 原答案正确项为 0 | `CORRECT_OPTION_MISSING` | 否 |
| 单选正确项 ≠1 | `SINGLE_CORRECT_COUNT_INVALID` | 否 |
| 多选正确项 <2 | `MULTI_CORRECT_COUNT_INVALID` | 否 |
| 多选全部选项均正确 | 允许 | 是 |
| 判断题归一后不能得到“一正一反且仅一个原正确项” | `JUDGE_OPTIONS_INVALID` | 否 |
| 文本超配置上限 | `TEXT_TOO_LONG` | 否 |
| 无课程/工种/等级关联 | 不是异常 | 是 |

判断题归一：

1. 接受 `对/正确/是/T/true/√` 为正向，`错/错误/否/F/false/×` 为反向，忽略大小写与空白。
2. 先去掉空占位项；仅对判断题，可去掉第二个有效正反项之后、文本恰好等于自身字母的 C/D 等占位项。
3. 必须恰好一个正向、一个反向，且原正确项恰为一个。
4. `答案1/答案2`、普通专业文本等无法识别的内容不得猜测成对错。

预检结果写入明细 `process_status=invalid, result_code=invalid, retryable=0`，无调用审计行。

### 8.4 内容质量识别（独立于结构预检和 AI 置信度）

调用顺序：先严格解析；结构已 invalid 的题不再调 AI，内容检查只对题干和可解析选项尽力补充警告。结构正常的题必须对 `stemText + 全部 NormalizedOption.text` 执行 `QuestionContentQualityInspector`，规则在导入预检和 AI 批次共用。

检测前只对**检测副本**做 Unicode NFKC，覆盖全角 `Ｅ/ｅ/＋/－/数字`；原始题干、选项、sourceHash 和发送给 AI 的文本均不因该归一被修改。

| ruleCode | 识别口径 | contentQuality / AI 处理 |
|---|---|---|
| `EXCEL_ERROR_LITERAL` | 独立错误文本：`#DIV/0!`、`#VALUE!`、`#REF!`、`#N/A`、`#NAME?`、`#NUM!`、`#NULL!`，以及 `NaN/Infinity/-Infinity` | **invalid**；不调 AI |
| `EXCEL_SCIENTIFIC_NOTATION_SUSPECT` | NFKC 后匹配 `(?<![A-Za-z0-9_])[+-]?(?:\d+(?:\.\d+)?|\.\d+)[Ee][+-]?\d+(?![A-Za-z0-9_])`，如 `1.2E+3`、`1.0E+00`、`1.2ｅ＋3` | **suspect**；可调用 AI，但必须复核 |
| `EXCEL_HASH_MARKS_SUSPECT` | 连续 3 个及以上 `#`，如列宽不足形成的 `####` | **suspect**；不直接当 Markdown 标题或确定错误 |
| `EXCESSIVE_DECIMAL_PRECISION_SUSPECT` | 数值 token 小数位 ≥16，或含连续 ≥12 个 0/9 的疑似二进制浮点尾巴，如 `0.30000000000000004` | **suspect**；长循环小数也只能提示，不能自动截断 |
| `TEXT_REPLACEMENT_CHAR_SUSPECT` | 含 U+FFFD `�` 替换字符 | **suspect**；提示可能存在编码问题 |
| 正常科学/专业表达 | `1.6×10^-19`、公式、单位、职业编码、普通长循环小数 | 不按 Excel 科学计数法命中；长小数若命中弱规则也只 suspect |

补充约束：

1. 多个 warning 取最高严重度；invalid > suspect > clean。warning 结构固定为 `{ruleCode,severity,location,optionId,matchedText,suggestedText,message}`，题干时 `location=stem,optionId=null`，选项时 `location=option`。
2. `suggestedText` 仅对完整科学计数法 token 生成，例如 `1.2E+3 → 1200`。必须先在 token.length>128 时直接置空；再用 `new BigDecimal(token)` 解析，并以 long 计算 `integerDigits=max(1, precision-scale)`、`estimatedChars=sign + integerDigits + (scale>0 ? 1+scale : 0)`，预计超过 **4096 字符**时直接置空，**不得先调用 toPlainString 再截断**。解析/scale 溢出、非有限值同样置空，正常范围才调用 `new BigDecimal(token).toPlainString()`。它只供详情和导出人工参考。
3. **不得自动改文本、自动替换答案、自动截断长小数，也不得把 suggestedText 送入 AI。** suspect 的 AI 请求仍使用原存储文本，使结果与快照可解释。
4. contentQuality=invalid 时 `resultCode=invalid, retryable=0`。若结构预检已产生 invalidReason，则**保留先产生的结构原因**，只在结构正常的题上写 `invalidReason=CONTENT_QUALITY_INVALID`；contentWarnings 仍完整保存，不因原因优先级丢字段。suspect 不改变 resultCode/confidence，但使 needsReview=true。
5. 不设置“选项是 0/1 就可疑”或“所有长小数都错误”等不可靠规则。`0.5→0` 已落库的信息损失无法从结果文本反推；没有原始文件时只能依靠人工或源文件对拍，不得宣称系统能全部识别。
6. 批次保存 `content_quality` 与 `content_warnings_json` 快照；重试 AI 不重新解释或清除这些字段，题库内容变化必须另建批次。

---

## 9. JEV 请求与响应契约

### 9.1 配置

v1.5 起，JEV 连接配置分两层管理：

**A. `application.yml` 只保留本功能运行参数：**

```yaml
platform:
  objectiveanswercheck:
    enabled: ${OBJECTIVE_ANSWER_CHECK_ENABLED:false}
    batch-max-size: ${OBJECTIVE_ANSWER_CHECK_BATCH_MAX_SIZE:2000}
    concurrency: ${OBJECTIVE_ANSWER_CHECK_CONCURRENCY:20}   # Worker 池上限
    confidence-threshold: ${OBJECTIVE_ANSWER_CHECK_CONFIDENCE_THRESHOLD:0.80}
    margin-threshold: ${OBJECTIVE_ANSWER_CHECK_MARGIN_THRESHOLD:0.20}
    max-stem-chars: ${OBJECTIVE_ANSWER_CHECK_MAX_STEM_CHARS:12000}
    max-option-chars: ${OBJECTIVE_ANSWER_CHECK_MAX_OPTION_CHARS:2000}
```

真实密钥不在 `application.yml` 中配置，由部署配置文件/环境变量提供：

```bash
OBJECTIVE_ANSWER_CHECK_JEV_API_KEY=真实TokenDance Key
```

**B. `exam_faq_model_config` 集中管理模型连接行：**

| 字段 | 值 |
|---|---|
| `model_code` | `OBJECTIVE_ANSWER_CHECK_JEV_MODEL` |
| `model_name` | `客观题答案检查 JEV` |
| `api_format` | `custom` |
| `feature_scope` | `objective-answer-check` |
| `base_url` | `https://tokendance.space/gateway` |
| `model_id` | `ateve-jev-v1` |
| `api_key` | `模型API Key走配置文件`（固定占位，绝不发送） |
| `status` | 种子默认 `disabled`，部署密钥后由管理员启用 |
| `jev_concurrency` | 5 |
| `jev_read_timeout_seconds` | 30 |
| `jev_theta1/jev_theta2` | NULL（本功能不用知识修正双门） |
| `temperature/max_tokens` | 0.70 / 2000（保留表默认；JEV 非生成式，不参与输出） |

运行时解析规则：

1. 功能开关 `enabled=false` 时直接拒绝创建/继续，历史结果仍可查看。
2. 按当前租户读取 `model_code=OBJECTIVE_ANSWER_CHECK_JEV_MODEL` 的最新未删行；不存在、`status!=enabled`、`base_url/model_id` 为空均返回“答案 AI 检查未开通/配置不可用”。
3. 只从环境变量读取真实 `OBJECTIVE_ANSWER_CHECK_JEV_API_KEY`；`api_key='模型API Key走配置文件'`、`PENDING_USER_INPUT`、空串都视为未配置，禁止发送。
4. DB 中的 `jev_concurrency/jev_read_timeout_seconds` 校验为 1～Worker池上限、5～120 秒；作为批次运行时快照，改配置对下一批次生效。
5. 应用启动只校验 yml 运行参数；DB 行在提交/继续/重试时实时解析，不要求为了改模型状态而重启服务。
6. **16 个选项上限、每块 5 个多选子问题仍是固定常量**，不随 DB 配置改变。
7. API Key 只在 ai 包内存运行时对象中传递，**不得写入批次 JSON、调用审计、异常信息、日志或返回给前端**；exam 业务侧只通过 descriptor 获取 modelId/baseUrl/credentialsReady/非敏感并发超时快照。
8. 现有 client 的 `platform.knowledgefix.jev-*` 技术默认值不改名，避免影响知识归属修正。

### 9.2 单选请求示例

```json
{
  "model": "ateve-jev-v1",
  "state": "【工种】铝电解工\n【课程】铝电解工初级\n【知识点】基本要求/安全知识\n【题型】单选题\n【题干】……\n【选项】\nA. ……\nB. ……\nC. ……\nD. ……",
  "questions": {
    "answer": {
      "type": "choice",
      "instructions": "根据题干和选项独立作答，选择唯一最符合题意的选项。",
      "criteria": {
        "opt_0": "第一个选项纯文本",
        "opt_1": "第二个选项纯文本",
        "opt_2": "第三个选项纯文本",
        "opt_3": "第四个选项纯文本"
      }
    }
  }
}
```

判断题相同，但 `instructions=判断题干的陈述是否正确。`，criteria 为 `{"T":"正确","F":"错误"}`。

内容质量 suspect 不触发提示词改写：请求中不额外加入“该选项可能错误”的引导，也不使用 suggestedText，避免把系统猜测变成模型偏见。contentQuality=invalid 则在进入本节前已终止，不产生请求。

### 9.3 多选请求示例与分块

每个选项一个二元子问题；每块最多 5 个子问题。所有块共享完全相同的题干与全部选项上下文。

```json
{
  "model": "ateve-jev-v1",
  "state": "【题型】多选题，至少有两个正确选项\n【题干】……\n【全部选项】\nA. ……\nB. ……\nC. ……\nD. ……\nE. ……",
  "questions": {
    "option_0": {
      "type": "choice",
      "instructions": "结合题干和全部选项，判断选项 A 是否属于本题正确答案。",
      "criteria": {"Y": "应选", "N": "不应选"}
    },
    "option_1": {
      "type": "choice",
      "instructions": "结合题干和全部选项，判断选项 B 是否属于本题正确答案。",
      "criteria": {"Y": "应选", "N": "不应选"}
    }
  }
}
```

- 2～5 个选项：1 块；6～10 个：2 块；11～16 个：最多 4 块。
- 每个逻辑块生成一个本地 `callId=UUID`，`chunkIndex/chunkCount` 落审计。
- 每个块结束立即通过 §6 的 `auditSink` 上报完整 outcome；成功块和失败块都独立落审计，不能只等整题成功。
- 所有块都成功且完整后才生成明细结论；任何一块失败，该题本轮失败，不展示半份 AI 答案。
- 重试该题时重新执行全部块并生成新 callId，避免把不同轮次的残缺结果拼起来；各轮 `runId/claimAttempt` 和成功/失败历史审计全部保留。
- 不使用 `2^n-1` 组合枚举，不把各选项概率相乘。

### 9.4 `decideBatch` 响应校验

新增通用客户端方法时保留既有 `decide` 行为；可让旧方法委托新方法，但不得改变 knowledgefix 既有异常分类。

对 200 响应逐字段验证：

1. 根 `answers` 必须是对象，且本次每个 `questionKey` 都存在；缺一个即整批响应失败。
2. 每个 answer 的 `choice` 非空且必须属于该子问题 criteria key。
3. `confidence` 必须是有限数值且在 [0,1]。
4. `probabilities` 的 key 集必须与该子问题 criteria key 集**完全一致**，不得缺少或额外带 key；值为有限数且在 [0,1]，概率和允许浮点误差 `|sum-1|<=0.01`，超出不自行归一化。
5. `choice` 必须位于最高概率候选集合（浮点容差 1e-9）。若 choice=B 但 probabilities 明显最高为 A，属于响应内部矛盾，按解析失败处理，禁止据此产生 mismatch_high。并列最高允许，但 margin=0，自然落低置信。
6. `margin` 统一计算为 `p(choice) - max(p(其他候选))`，不再脱离 choice 单取一个无关 top1/top2 差；`top1Probability/top2Probability` 仅作展示，最高并列时 margin=0。
7. usage 存在时 input/output tokens 必须是非负整数；缺失时按**完整序列化请求体**（state + 全部 questions/instructions/criteria）字符数 `/3` 估算并置 `usageEstimated=true`，不得只按 state 估算，也不得伪造精确 token。
8. `model`、`metadata.confidence_definition` 可空；空值不阻断。
9. 上游 request id 如存在则记录；不存在不猜字段，使用本地 callId 作为审计主键外的追踪键。

在 `JevDecisionClient` 的 Apache HttpClient 构建处增加 `.disableContentCompression()`，防止既有 Brotli 单字节读取缺陷在其他端点复现；补请求头不包含 `Accept-Encoding` 的回归测试。

### 9.5 错误分类与熔断

| 场景 | 明细处理 | 批次处理 |
|---|---|---|
| 配置 disabled/key 为空 | 创建或继续前直接 503，不发请求 | 不创建新批次 |
| 401 | 当前行 `failed/retryable=1`，修复配置后可重试 | 立即停止派发，批次 `failed` |
| 422 `token_budget_exceeded` | `failed/retryable=0`，错误文案“内容超出模型输入上限” | 不计系统性连续失败，继续其他行 |
| 其他 422 | `failed/retryable=0`，表示请求构造问题 | 连续 5 次后批次 `failed` |
| 429/529/5xx/网络/超时 | client 内限次退避后仍失败，行 `failed/retryable=1` | 连续 5 次后停止派发，批次 `failed` |
| 200 缺子答案/choice 越界/概率非法 | 整行失败，不用部分结果；`retryable=1` | 连续 5 次后批次 `failed` |
| 结构预检 invalid | 无 HTTP 调用 | 不计失败、不熔断 |
| 人工暂停 | 在飞行调用正常收尾 | 不再派发，最后置 `paused` |

连续成功一次逻辑调用可将“实时连续失败计数”清零，但**401 或达到 5 次阈值时会以 run_id 条件把 running/pausing 原子更新为 failed，数据库状态即失败锁存，后续成功绝不能清除**。`pausing → failed` 是合法转换且失败优先于暂停收尾；paused/done 的条件更新不得覆盖 failed。熔断后 pending 保留，已完成结果保留；修复问题后“继续”处理 pending，“重试失败项”只处理 retryable=1 的 failed 行。

**任何失败都不得降级到另一个生成式模型。**

---

## 10. 置信度与结果判定算法

### 10.1 题目级指标与逐选项指标分层保存

**单选/判断只有一个题目级全局 choice**，不得把全局 confidence/margin 错当成每个选项各自独立的模型判断。`option_results_json` 采用两种明确形态：

```json
{
  "scope": "question",
  "choice": "opt_1",
  "confidence": 0.91,
  "top1Probability": 0.88,
  "top2Probability": 0.07,
  "margin": 0.81,
  "options": [
    {"optionId":"opt_0","label":"A","text":"……","originalCorrect":false,"aiCorrect":false,"referenceProbability":0.95},
    {"optionId":"opt_1","label":"B","text":"……","originalCorrect":true,"aiCorrect":true,"referenceProbability":0.88}
  ]
}
```

```json
{
  "scope": "option",
  "options": [
    {"optionId":"opt_0","label":"A","text":"……","originalCorrect":true,"aiCorrect":true,
     "choice":"Y","confidence":0.91,"referenceProbability":0.88,
     "top1Probability":0.88,"top2Probability":0.12,"margin":0.76},
    {"optionId":"opt_1","label":"B","text":"……","originalCorrect":false,"aiCorrect":false,
     "choice":"N","confidence":0.86,"referenceProbability":0.83,
     "top1Probability":0.83,"top2Probability":0.17,"margin":0.66}
  ]
}
```

定义：

- `confidence_i`：对应子问题的 `answers.<key>.confidence`。
- `p_i(c)`：对应子问题 probabilities 中候选 c 的概率。
- `margin_i=p_i(choice)-max(p_i(其他候选))`；top1/top2 仅作展示。choice 必须已通过 §9.4 的最高概率一致性校验。
- 单选/判断：AI 正确项为全局 choice 对应选项，其余为 false。
- 多选：每个二元子问题 `choice=Y` 则该选项 `aiCorrect=true`，否则 false。

### 10.2 原答案支持度

对每个选项计算模型对“原标记”的支持概率：

- 若 `originalCorrect=true`：单选/多选取该选项被判定为正确的概率；判断取原正/反候选概率。
- 若 `originalCorrect=false`：取 1 减去该选项被判定为正确的概率；多选即 `p_i(N)`（当原标记为不应选时）。

```text
referenceProbability_i = P(模型认可该选项的原正确/错误标记)
referenceConfidence   = min(referenceProbability_i)，i 覆盖全部选项
```

单选示例：原答案 B，概率 A=0.05、B=0.85、C=0.07、D=0.03，则四个原标记支持概率为 0.95、0.85、0.93、0.97，故 `referenceConfidence=0.85`。

由于单选/判断的 probabilities 完整且和为 1、原正确项恰为一个，上述 min 在数学上等于**原正确候选自己的概率**。实现可直接用该简化公式，并用两候选和多候选数值测试证明等价：

- 判断原答案为反候选 F：`referenceConfidence=p(F)`，不是 `1-p(F)`。
- 多选原标记为不应选：该选项使用 `p_i(N)`，最后对所有选项取 min。

### 10.3 AI 置信度与概率差

```text
单选/判断：aiConfidence = 全局 choice 的 confidence；minMargin = 全局 margin
多选：      aiConfidence = min(confidence_i)；minMargin = min(margin_i)
```

禁止把多选概率连乘：连乘需要独立性假设，会让 5 个 0.9 变成 0.59，且没有业务可解释性。取最小值是“最弱选项”保守口径。

### 10.4 最终结果

设批次阈值为 `θ=0.80`、`δ=0.20`，计算时先比较原始值，再保留 5 位小数落库，禁止先四舍五入再卡边界。

```text
baseHigh = aiConfidence >= θ AND minMargin >= δ
consistent = (AI 正确选项 ID 集合 == 原正确选项 ID 集合)

if consistent:
    if baseHigh AND referenceConfidence >= θ:
        result = consistent_high
    else:
        result = low_confidence
else:
    if baseHigh:
        result = mismatch_high
    else:
        result = low_confidence
```

额外卫兵：多选 AI 最终正确项 <2 时，即便 `baseHigh=true`，也只能 `low_confidence`，并写 `decisionWarning=AI_COMBINATION_INVALID`；不得把无效的 AI 组合标成高置信修改建议。

### 10.5 为什么同时保留两个置信度

- 高置信不一致时，AI 对自己的新答案很确定，原答案支持度通常很低；它是“优先复核”信号。
- 高置信一致不仅要求 AI 自信，还要求概率分布对原答案的全部标记都有足够支持。
- 页面不能把 `mismatch_high` 展示成“原答案置信度 90%”，那会颠倒含义。

### 10.6 内容质量与 AI 结果的组合

```text
needsReview = resultCode != null
              AND reviewStatus == unreviewed
              AND (resultCode IN (mismatch_high, low_confidence, invalid, failed)
                   OR contentQuality != clean)
```

- `resultCode=null` 的 pending/processing 不进入可执行复核队列；retry-failed 刚复位的 suspect 行同样等待结果落定，避免“需要复核但禁止复核”的矛盾。
- `consistent_high + suspect` 是合法组合：AI 答案一致，但格式仍需人工核对，不得进入“可放心略过”集合。
- `mismatch_high + suspect` 仍展示高置信不一致，但页面必须同时提示“可能是内容格式问题”，人工需区分答案键错误与导入失真。
- 内容质量不参与 aiConfidence/referenceConfidence 数值计算，避免把确定性文本问题伪装成模型不确定性。
- 标定指标按 clean 与 suspect 分组报告；不能用 suspect 样本拉高或压低 clean 集的 AI 准确率。

---

## 11. 批次、租约与明细状态机

### 11.1 批次状态与失败锁存

| 状态 | 含义 | active_tenant_id / 租约 |
|---|---|---|
| `pending` | 已创建待编排 | tenantId；run_id/lease 有效 |
| `running` | 正在派发/执行 | tenantId；run_id/lease 有效 |
| `pausing` | 已请求暂停，等待在飞行行收尾 | tenantId；继续心跳 |
| `paused` | 可继续；pending 保留且无 processing | NULL；清空 run_id/owner/lease |
| `done` | pending=processing=0；允许存在 failed 明细 | NULL；清空 run_id/owner/lease |
| `failed` | 批次级失败锁存，仍有 pending 可继续 | **processing>0 时保留活动槽并继续心跳；归零后释放** |

`active_tenant_id` 建唯一索引：同一租户最多一个持有活动槽的批次。paused/done 不阻塞新批次；failed 只有在 processing=0 后才能继续或允许其他批次运行，避免旧在飞行调用与新批次并发。

合法转换：

```text
pending → running → done
pending/running → pausing → paused → running
running/pausing → failed → running（继续或重试后）
done → running（仅 retry-failed 确实复位了行）
```

其他转换用条件 UPDATE 拒绝，不在 Java 里先读后无条件覆盖。401 或熔断触发时，`markFailed` 以 `id + run_id + status IN(running,pausing)` 更新为 failed；**failed 是锁存事实，后续成功只完成自己的在飞行行，不得改回 running/paused/done**。只有用户显式继续/重试才能生成新 run_id 离开 failed。

### 11.2 编排所有权：run_id + owner_id + lease

每次创建、继续或有效重试都生成：

- `runId=IdWorker.getId()`：本次编排代次；
- `ownerId=hostname:pid:UUID`：本次编排所有者；
- `leaseUntil=数据库当前时间 + 60 秒`。

编排线程每 15 秒续租一次：

```sql
UPDATE exam_objective_answer_check_batch b
SET b.lease_until=DATE_ADD(NOW(), INTERVAL 60 SECOND)
WHERE b.id=? AND b.run_id=? AND b.owner_id=? AND b.lease_until>=NOW()
  AND (b.status IN ('pending','running','pausing')
       OR (b.status='failed' AND EXISTS (
         SELECT 1 FROM exam_objective_answer_check_item i
         WHERE i.batch_id=b.id AND i.process_status='processing'
       )))
```

影响 0 行表示所有权已失效，编排器必须停止派发和收尾。时间比较统一用数据库时间，避免多节点时钟差。每次派发前及等待信号量后都要复核 `status=running + run_id 匹配 + lease_until>=NOW()`。

paused/done，以及 processing=0 的 failed 在收尾时清空 active 槽、run_id、owner_id、lease_until。重启或蓝绿新节点**不得直接重置租约仍有效的批次**。

### 11.3 明细认领与晚到写入 fencing

明细状态：`pending / processing / success / invalid / failed`。认领和版本递增必须与批次所有权在同一条件 UPDATE 中完成：

```sql
UPDATE exam_objective_answer_check_item i
SET i.process_status='processing', i.attempts=i.attempts+1, i.version=i.version+1
WHERE i.id=? AND i.process_status='pending'
  AND EXISTS (
    SELECT 1 FROM exam_objective_answer_check_batch b
    WHERE b.id=i.batch_id AND b.run_id=? AND b.status='running' AND b.lease_until>=NOW()
  )
```

影响 1 行后读取的 `attempts` 即本次 `claimAttempt`。整题最终结果更新必须同时满足：

```text
item.id 匹配
AND process_status='processing'
AND attempts=claimAttempt
AND 所属 batch.run_id=claimRunId
```

- 影响 1 行：当前认领有效，在同一短事务中更新明细及批次 `done/failed/resultCode` 计数。
- 影响 0 行：说明批次已接管或明细已重新认领；旧 Worker 不得覆盖新结果，也不得重复增加结果计数。
- 晚到 Worker 已实际发生的模型调用仍按 `runId/claimAttempt` 保留审计并计实际用量，但绝不能改变明细结论。
- 暂停与认领之间允许出现“最后一个在飞行行”；其 run_id 尚未清理时可正常收尾。接管后 run_id 改变则旧行被 fencing。

### 11.4 暂停、失败与收尾竞态

- 暂停：`pending/running → pausing` 必须条件更新；编排器停止新认领但继续心跳，等在飞行行归零。
- 失败：允许 `running/pausing → failed`，失败优先于 paused/done 收尾；第 5 次失败后又成功的竞态不能撤销锁存。
- paused/done 收尾必须带 `原状态 + run_id` 条件，且用当前读统计 pending/processing；pausing 在 processing=0 后若仍有 pending 置 paused，否则置 done；任何收尾都不得覆盖 failed。
- failed 时停止新认领，其他在飞行行允许收尾并计 done；processing 归零后释放活动槽和租约。
- 编排器因 paused/failed 停止派发后，必须继续 join 本 run 已提交的任务并执行 processing 归零收尾，不得一发现 failed 就直接 return 留下无人释放的活动槽；只有 run_id 被接管时才立即退出，由接管方收尾。
- `resume` 只允许 paused，或 processing=0 的 failed；它只处理 pending，不自动复位 failed 明细。
- `retry-failed` 只允许 paused/done，或 processing=0 的 failed，且实际复位行数必须 >0。

### 11.5 过期接管、启动恢复与拒绝补偿

恢复入口为 `ApplicationReadyEvent` + 每 30 秒一次的恢复扫描（多节点可同时执行，数据库条件保证只有一个接管成功）：

1. 只选 `lease_until<NOW()` 的 pending/running/pausing，以及仍有 processing 且租约过期的 failed；**租约有效的批次绝不重置**。
2. 短事务 `SELECT ... FOR UPDATE` 当前读并再次确认过期；把遗留 processing→pending，version 递增但 attempts 不清零。
3. 原活动批次置 paused；failed 保持 failed；清空 active 槽、run_id、owner_id、lease_until。pending=0 的原活动批次可置 done。
4. 不自动发起新外部调用，由用户点继续。failed 的遗留 processing 因此也有明确出口。

跨租户恢复扫描只能使用专用系统恢复 Mapper 的受限 `@InterceptorIgnore(tenantLine="true")` 方法，返回 `id/tenant_id/status/run_id/lease_until`，不得暴露给 Controller；更新每一行前先设置对应租户上下文并 finally clear。普通查询禁止复用该绕过方法。补两个租户及“旧节点仍持有效租约”的恢复测试。

所有 `AbortPolicy` 拒绝都必须有持久化补偿：

- **创建/继续/重试后的编排池拒绝**：批次已提交则置 failed，若 processing=0 立即释放活动槽和租约；返回 503，之后可继续，不得永久 pending 占槽。
- **Worker 投递拒绝**：以 `runId+claimAttempt` 条件把刚认领军行恢复 pending（version+1），再锁存批次 failed；其他在飞行行按 §11.4 收尾。
- 恢复/重试/收尾 SQL 同样必须使用当前状态条件，不能因补偿把新 run 覆盖掉。
- 拒绝恢复以及过期/失败后的实际调用审计，必须与批次 `callCount/token/usageEstimatedCalls/modelDurationMs` 在同一短事务落库；`done/failed/resultCode` 仍只由当前 `runId+claimAttempt` 的有效认领推进，结果写入影响 0 行时不得回减或重复增加结果计数。

### 11.6 失败重试的事务与计数守恒

`retry-failed` 在一个短事务中：

1. 锁定批次并确认 processing=0、状态允许；
2. 选出 `retryable=1` 的 failed 行，实际为 0 时不改状态、不投递；
3. 将这些行恢复 pending，同时清空 `result_code/consistent/ai_answer_keys/三个置信字段/option_results_json/decision_warning/error_* /checked_at` 和旧复核字段；`retryable=0`，`version=version+1`，attempts 保留用于 fencing；`content_quality/content_warnings_json` 是确定性快照，必须保留；
4. 按实际行数 n 原子执行 `done=done-n, failed=failed-n`；
5. 批次置 running，写入新 active 槽/runId/ownerId/lease，清 failReason/finishedAt；**保留原批次 modelId/JEV baseUrl/并发/读超时快照，不重新读取当前 DB 配置**；提交后再投递。

重试只检查原快照；题库若已修改，必须另建批次。重试会清除这些失败行的旧复核标记，前端按钮需明确提示。编排池拒绝时按 §11.5 置 failed，pending 和回减后的计数仍保持一致。

### 11.7 计数口径

- `total`：全部选择题数，含 invalid。
- `done = success + invalid + failed`，且 `total=done+pending+processing`。
- `pending/processing` 可从明细实时统计；批次表中冗余计数只由同一服务原子更新。
- `consistent_high + mismatch_high + low_confidence = success`。
- `content_invalid` 是 resultCode=invalid 的子集；`content_suspect` 跨成功/失败等结果计数，二者均不参与 success 三类结果的加和，重试时不清零。
- `expectedCalls` 是创建时预估值；`callCount` 是实际逻辑块数，不含 client 内部 HTTP 重试次数。
- 过期接管造成的晚到重复调用仍计 callCount/token 实际用量，但结果计数只接受当前 claim，因此 callCount 可以高于 expectedCalls，不能用 callCount 反推 done。
- token 分为 `inputTokensExact` 与 `inputTokensEstimated`，并记录 `usageEstimatedCalls`；禁止把含估算的总量展示成纯精确值。
- `done` 批次可含 failed；页面必须显示“已完成（含 N 道失败）”，不能假装全部成功。

### 11.8 异步写库与线程池

`AuditMetaObjectHandler` 会调用 `SecurityContext.currentUser()`，因此本功能异步链不能只靠“异常被吞掉”来规避规则：

- HTTP 创建批次/人工复核可使用普通 MP 填充。
- Worker、编排线程、启动恢复的 insert/update 统一走本模块自定义 Mapper SQL，显式传入 tenantId、userId、createdAt/updatedAt、createdBy/updatedBy；异步审计操作人使用**当前 run 的触发 userId**（创建/继续/重试各自在 HTTP 线程捕获），批次 createdBy 保留首次创建人，系统恢复 updatedBy=0。
- 不得在这些异步路径调用 BaseEntity 自动填充，不得依赖 SecurityContext 抛异常后返回 null。测试须验证 SecurityContext 零调用且写入的 tenantId/createdBy/updatedBy 非空正确。

线程池：

- `objectiveAnswerCheckExecutor`：编排池 core=1/max=2/queue=10；
- `objectiveAnswerCheckWorkerExecutor`：core=max=`platform.objectiveanswercheck.concurrency`（v1.5 默认 20，仅作 Worker 池上限）；每个批次实际派发并发取 `min(exam_faq_model_config.jev_concurrency, Worker池上限)`，队列与 Worker 池上限同源；
- 两者均 `AbortPolicy`、`waitForTasksToCompleteOnShutdown=true`、终止等待 120 秒；
- 禁止 `CallerRunsPolicy`，禁止裸 `setInterval` 式后端无限调度。恢复扫描用固定 30 秒调度，单测直接调用恢复方法保证确定性。

---

## 12. 数据库与 Flyway 迁移

### 12.1 取号与文件

设计时双库 `MAX(version)` 均为 `20261024.002`；2026-10-27 复核时仓库已存在 `V20261027_006`、生产已应用至 `20261027.001`，原候选号 `V20261026_001` 已过时。**文件名不得照抄本文任何示例**，实施当天以两库 `MAX(version)` 的较大者 +1 取号：

`services/platform-api/src/main/resources/db/migration/V<实施当天取号>__objective_answer_check.sql`

**实施当天必须重新查两库；如已有更大版本则顺延，不得修改已应用迁移，不新增分叉目录。** 新迁移必须 `mvn compile` 后才会进入 `target/classes`。

一个迁移包含三表 + 菜单/权限/角色授权，全部幂等。表实体继承 `BaseEntity`，雪花 ID 由 `IdWorker.getId()` 显式设置；DDL 的 id/tenant_id/deleted/created_by/updated_by/created_at/updated_at 必须与 getter 一一对应。

### 12.2 批次表

```sql
CREATE TABLE IF NOT EXISTS exam_objective_answer_check_batch (
  id BIGINT NOT NULL COMMENT '批次ID，雪花；同时作为对外批次号',
  tenant_id BIGINT NOT NULL,
  active_tenant_id BIGINT NULL COMMENT '活动批次唯一槽；paused/done及无processing的failed置NULL',
  run_id BIGINT NULL COMMENT '编排代次；旧run禁止派发和收尾',
  owner_id VARCHAR(128) NULL COMMENT '编排所有者hostname:pid:UUID',
  lease_until DATETIME NULL COMMENT '编排租约，数据库时间；过期才可接管',
  selection_mode VARCHAR(8) NOT NULL COMMENT 'ids/filter',
  filter_json LONGTEXT NOT NULL COMMENT '筛选快照，不含API Key和逐题IDs',
  status VARCHAR(16) NOT NULL,
  total INT NOT NULL DEFAULT 0,
  done INT NOT NULL DEFAULT 0,
  failed INT NOT NULL DEFAULT 0,
  invalid INT NOT NULL DEFAULT 0,
  consistent_high INT NOT NULL DEFAULT 0,
  mismatch_high INT NOT NULL DEFAULT 0,
  low_confidence INT NOT NULL DEFAULT 0,
  content_suspect INT NOT NULL DEFAULT 0 COMMENT '内容质量suspect行数，跨AI结果分类',
  content_invalid INT NOT NULL DEFAULT 0 COMMENT '内容质量invalid行数，是result invalid子集',
  expected_calls INT NOT NULL DEFAULT 0,
  call_count INT NOT NULL DEFAULT 0,
  input_tokens_exact BIGINT NOT NULL DEFAULT 0,
  input_tokens_estimated BIGINT NOT NULL DEFAULT 0,
  output_tokens BIGINT NOT NULL DEFAULT 0,
  usage_estimated_calls INT NOT NULL DEFAULT 0,
  model_duration_ms BIGINT NOT NULL DEFAULT 0,
  model_id VARCHAR(64) NOT NULL,
  prompt_version VARCHAR(32) NOT NULL,
  confidence_threshold DECIMAL(4,3) NOT NULL,
  margin_threshold DECIMAL(4,3) NOT NULL,
  fail_reason VARCHAR(500) NULL,
  started_at DATETIME NULL,
  finished_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_objective_answer_check_active (active_tenant_id),
  KEY idx_oacb_tenant_status (tenant_id, status, id),
  KEY idx_oacb_lease (status, lease_until),
  KEY idx_oacb_created (tenant_id, created_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='客观题答案AI检查批次';
```

### 12.3 明细表

```sql
CREATE TABLE IF NOT EXISTS exam_objective_answer_check_item (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  batch_id BIGINT NOT NULL,
  detail_id INT NOT NULL COMMENT 'exam_exam_detail.id',
  question_type VARCHAR(16) NOT NULL,
  source INT NULL,
  work_type_id INT NULL,
  work_type_title VARCHAR(512) NULL,
  level_code INT NULL COMMENT '对外等级1~5；由5-exam_course.level转换',
  course_id INT NULL,
  course_title VARCHAR(512) NULL,
  knowledge_id INT NULL,
  knowledge_path VARCHAR(1024) NULL,
  content_snapshot TEXT NULL,
  stem_text TEXT NOT NULL,
  answer_snapshot LONGTEXT NULL COMMENT '原始answer；SQL NULL也保留为NULL并由预检标记异常',
  resolve_snapshot TEXT NULL,
  option_snapshot_json LONGTEXT NOT NULL COMMENT '归一选项及原正确标记',
  source_hash CHAR(64) NOT NULL COMMENT 'type+content+answer 的SHA-256',
  content_quality VARCHAR(16) NOT NULL DEFAULT 'clean' COMMENT 'clean/suspect/invalid',
  content_warnings_json LONGTEXT NULL COMMENT '结构化内容警告，原值不被suggestedText覆盖',
  process_status VARCHAR(16) NOT NULL,
  result_code VARCHAR(24) NULL,
  consistent TINYINT NULL,
  original_answer_keys VARCHAR(128) NULL,
  ai_answer_keys VARCHAR(128) NULL,
  reference_confidence DECIMAL(6,5) NULL,
  ai_confidence DECIMAL(6,5) NULL,
  min_margin DECIMAL(6,5) NULL,
  option_results_json LONGTEXT NULL COMMENT '逐选项概率与判断；仅成功后完整写入',
  decision_warning VARCHAR(64) NULL,
  invalid_reason VARCHAR(64) NULL,
  error_code VARCHAR(64) NULL,
  error_message VARCHAR(500) NULL,
  retryable TINYINT NOT NULL DEFAULT 0,
  attempts INT NOT NULL DEFAULT 0 COMMENT '认领代次；结果写入fencing，不在重试时清零',
  version INT NOT NULL DEFAULT 0 COMMENT '全行变更版本；人工复核乐观锁',
  review_status VARCHAR(24) NOT NULL DEFAULT 'unreviewed',
  review_note VARCHAR(500) NULL,
  reviewed_by BIGINT NULL,
  reviewed_at DATETIME NULL,
  checked_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_oaci_batch_detail (batch_id, detail_id),
  KEY idx_oaci_batch_process (batch_id, process_status, id),
  KEY idx_oaci_batch_result (batch_id, result_code, id),
  KEY idx_oaci_batch_content (batch_id, content_quality, id),
  KEY idx_oaci_batch_review (batch_id, review_status, id),
  KEY idx_oaci_detail (tenant_id, detail_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='客观题答案AI检查明细快照';
```

不加与 legacy 表的物理外键，与现有工程风格一致；不提供明细删除 API。JSON 文本列由应用层 ObjectMapper 读写，插入时显式给 `[]/{}`，不得依赖数据库默认值。

### 12.4 调用审计表

```sql
CREATE TABLE IF NOT EXISTS exam_objective_answer_check_call_log (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  batch_id BIGINT NOT NULL,
  item_id BIGINT NOT NULL,
  detail_id INT NOT NULL,
  run_id BIGINT NOT NULL COMMENT '发起该调用的编排代次',
  claim_attempt INT NOT NULL COMMENT '发起该调用的明细认领代次',
  call_id VARCHAR(36) NOT NULL COMMENT '每个逻辑块的本地UUID，重试同一轮可关联',
  upstream_request_id VARCHAR(128) NULL,
  tool_name VARCHAR(64) NOT NULL DEFAULT 'objective_answer_check' COMMENT '受控AI工具名',
  prompt_version VARCHAR(32) NOT NULL,
  chunk_index INT NOT NULL DEFAULT 1,
  chunk_count INT NOT NULL DEFAULT 1,
  question_type VARCHAR(16) NOT NULL,
  requested_model VARCHAR(64) NOT NULL,
  response_model VARCHAR(64) NULL,
  confidence_definition VARCHAR(128) NULL,
  prompt_sha256 CHAR(64) NOT NULL,
  prompt_preview VARCHAR(200) NULL COMMENT '题干末尾摘要，不含API Key和原答案标记',
  input_tokens BIGINT NULL,
  output_tokens BIGINT NULL,
  usage_estimated TINYINT NOT NULL DEFAULT 0,
  duration_ms INT NULL,
  status VARCHAR(16) NOT NULL COMMENT 'success/failed',
  error_code VARCHAR(64) NULL,
  error_message VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_oacll_call (call_id),
  KEY idx_oacll_item (item_id, id),
  KEY idx_oacll_batch (batch_id, status, id),
  KEY idx_oacll_created (tenant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='客观题答案AI检查模型调用审计';
```

审计按“逻辑块”记录最终成功/失败；client 内部 HTTP 重试不新增多行，错误信息保留重试次数。`auditSink` 保证多选前序成功块和后续失败块都能落库，runId/claimAttempt 用来识别晚到旧调用。批次 callCount、精确/估算 token 和 usageEstimatedCalls 由审计聚合核对；估算只按完整请求体计算，禁止把含估算的总量当精确值隐藏。审计写失败不得让明细静默成功。

聚合口径固定为：每个实际发出的逻辑块 outcome 与批次用量在同一短事务成功落库即 `callCount+1`，无论该块模型成功或失败；token/duration 的 NULL 按 0 累加。仅 `usage_estimated=1` 的块计 `usageEstimatedCalls+1` 且输入 token 进入 estimated 桶，usage 精确的输入 token 进入 exact 桶，output token 单独累计；网络/HTTP 失败且无 usage 时保持 token NULL、usage_estimated=0，不把请求估算伪装成上游已消耗 token。晚到或被 fencing 的调用也按实际发生计入上述用量，但不推进结果计数。

### 12.5 菜单与权限种子

1. `tenant_id=10159`。
2. 父菜单只查现存“资源管理”：优先 `code='zi_yuan_guan_li'`，不存在时回退 `code='question_resource'`；**不得再建一个支持保障或资源管理目录，不得 parent_id=0 兜底**。
3. 菜单 ID 可用 `202610260101`；权限 ID `202610260201～204`；角色关系 ID 使用同日期不冲突段。实施时若撞 ID 必须换，不覆盖旧行。
4. `sort_order=6`，`visible=1,status=enabled`，parent 为上述资源管理 ID。
5. `sys_permission` 四条挂新菜单，权限码见 §4.2。
6. `sys_role_menu` 给 `role_id=2000` 挂新菜单；`sys_role_permission` 授四条权限。两类授权 SELECT 都必须显式连接 `sys_role r ON r.id=2000 AND r.tenant_id=10159 AND r.deleted=0`，不得把 tenant_id=1 的历史角色当成 10159 角色。
7. 所有 INSERT 使用 `INSERT ... SELECT ... WHERE NOT EXISTS`，角色不存在时不产生无效授权；迁移后验收必须核对菜单/权限/授权实际行数，不能把“脚本没报错”当成种子成功。
8. 空库跑迁移时，既有 `V20260912_001` 的 `question_resource` 是回退父菜单；测试/生产现存 `zi_yuan_guan_li` 时则挂到真实的“课程管理→支持保障→资源管理”链。补“zi_yuan 优先”和“question_resource 回退”两种种子测试。
9. **空库与克隆库的验收口径分开**：空库只验证三表、父菜单实际 ID、菜单和四条权限可幂等创建；既有迁移并不保证存在 `(2000,10159)` 角色，所以空库不得伪造角色授权。含真实 10159/2000 角色基线的测试库结构克隆再验证 1 条角色菜单和 4 条角色权限。不得为通过本功能测试修改已应用旧迁移或插入跨租户角色关系。

### 12.6 v1.5 模型连接配置种子（后续迁移，禁止改已应用迁移）

原功能迁移可能已应用，本小节必须落在**新的后续迁移**，文件名实施当天按两库 `MAX(version)` 较大者 +1 取号，例如：

`services/platform-api/src/main/resources/db/migration/V<新取号>__objective_answer_jev_model_config.sql`

1. `exam_faq_model_config` 增加适用范围列（更新 Entity/VO/DTO，老数据默认 common）：

```sql
ALTER TABLE exam_faq_model_config
  ADD COLUMN feature_scope VARCHAR(32) NOT NULL DEFAULT 'common'
    COMMENT '配置适用范围：common/faq-conversion/theory-gen/skill-gen/objective-answer-check/knowledge-fix' AFTER api_format,
  ADD KEY idx_efmc_scope_status (tenant_id, feature_scope, status);
```

2. 本功能批次表固化 v1.5 JEV 运行时快照（不含密钥，重启续跑仍遵守“改配置只影响下一批次”）：

```sql
ALTER TABLE exam_objective_answer_check_batch
  ADD COLUMN jev_base_url VARCHAR(512) NULL COMMENT '批次创建时从模型配置固化的JEV Base URL（非密钥）',
  ADD COLUMN jev_concurrency INT NULL COMMENT '批次创建时从模型配置固化的JEV派发并发',
  ADD COLUMN jev_read_timeout_seconds INT NULL COMMENT '批次创建时从模型配置固化的JEV读超时';
```

3. 插入客观题答案检查专用 JEV 行（`api_key` 为 owner 确认的固定占位文案，不是密钥）：

```sql
INSERT INTO exam_faq_model_config (
  id, tenant_id, model_code, model_name, base_url, api_key, api_format, feature_scope,
  model_id, temperature, max_tokens, thinking, reasoning_effort, extra_config, status,
  jev_theta1, jev_theta2, jev_concurrency, jev_read_timeout_seconds,
  jev_prune_stage2, jev_prune_stage1,
  created_at, updated_at, created_by, updated_by, deleted
)
SELECT
  202610280101, 10159,
  'OBJECTIVE_ANSWER_CHECK_JEV_MODEL', '客观题答案检查 JEV',
  'https://tokendance.space/gateway', '模型API Key走配置文件', 'custom', 'objective-answer-check',
  'ateve-jev-v1', 0.70, 2000, NULL, NULL, NULL, 'disabled',
  NULL, NULL, 5, 30,
  0, 0,
  NOW(), NOW(), 1, 1, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM exam_faq_model_config c
  WHERE c.tenant_id = 10159
    AND c.model_code = 'OBJECTIVE_ANSWER_CHECK_JEV_MODEL'
    AND c.deleted = 0
);
```

实施时若 ID 撞号必须换号，不覆盖旧行；`status='disabled'` 是安全默认，部署环境密钥后由管理员在模型配置页启用。

4. 共享模型下拉的适用范围过滤（防止本行出现在无关功能）：

- `FaqModelConfigApplicationService.listEnabled()`（FAQ 转换）：`feature_scope IN ('common','faq-conversion')`；
- `TheoryGenBatchService.listEnabledModelConfigs()`：`feature_scope IN ('common','theory-gen')`；
- `SkillGenBatchService.listEnabledModelConfigs()`：`feature_scope IN ('common','skill-gen')`；
- 本功能按精确 `model_code` 读取，不受上述下拉过滤影响；模型配置管理页的 `page` 仍展示全部范围，便于管理本行。

5. 模型配置管理面同步：

- `FaqModelConfigVO` 增加 `featureScope/jevConcurrency/jevReadTimeoutSeconds`；列表增加“适用范围”，本行 API Key 列显示“配置文件提供”。
- `FaqModelConfigCreateReq/UpdateReq` 允许维护 `jevConcurrency/jevReadTimeoutSeconds`；`FaqConfigDialog` 仅在编辑 `OBJECTIVE_ANSWER_CHECK_JEV_MODEL` 时显示这两个字段，普通 LLM 行界面不变。
- `FaqModelConfigApplicationService.update` 对本行增加密钥保护：请求未传 `apiKey` 时保留占位值；传入占位值视为不修改；传入其他值直接 409“该模型 API Key 由服务器配置文件提供，请不要在此填写”。普通模型编辑时空 Key 也必须忽略，不能清空旧密钥。
- 本行禁止删除（`delete` 返回 409），避免系统托管配置被删后只能手工 INSERT 恢复；前端隐藏该行删除按钮。
- `FaqModelConfigVO.maskApiKey` 对 `模型API Key走配置文件` 返回“配置文件提供”，不得把占位值当普通密钥脱敏成前后片段。

6. 存量种子适用范围必须用**独立后续迁移**回填：`V20261029_003__model_config_feature_scope_backfill.sql`（实施时 002 已被并发竞赛功能占用）。按 model_code 幂等更新 `theory-gen-glm→theory-gen`、`skill-gen-glm→skill-gen`、`knowledge-fix-pro/knowledge-fix-jev→knowledge-fix`、`courseware-gen-glm→courseware-gen`、`deepseek-review→review-sync`；禁止修改 V20261029_001/002。

---

## 13. API 契约

统一前缀：`/exam/objective-answer-check`。统一响应 `{code,message,data}`；分页响应为 `{items,page,pageSize,total}`。所有 batchId/itemId 均为字符串。

| 方法/路径 | 权限 | 说明 |
|---|---|---|
| `GET /filter-options?workTypeId=` | view | 返回实际有题的工种、可用等级、四来源、三题型 |
| `POST /questions/page` | view | Scope + page/pageSize，返回试题分页与预检摘要 |
| `POST /selection/preview` | view | SelectionReq → 数量、异常、预计调用和当前阈值 |
| `POST /batches` | submit + AuditLog | 创建并秒回批次；事务提交后才异步 |
| `GET /batches/current` | view | 当前持有活动槽的批次（含尚有 processing 的 failed），无则 data=null |
| `GET /batches` | view | 历史批次分页，按创建时间/ID倒序 |
| `GET /batches/{batchId}` | view | 批次进度、统计、配置快照与可用操作 |
| `POST /batches/{batchId}/pause` | submit + AuditLog | 请求暂停 |
| `POST /batches/{batchId}/resume` | submit + AuditLog | 继续 pending；可处理 failed 批次的剩余行 |
| `POST /batches/{batchId}/retry-failed` | submit + AuditLog | 仅 retryable failed 复位并运行，返回复位数量 |
| `GET /batches/{batchId}/items` | view | 按结果/内容质量/needsReview/题型/复核状态/关键词分页 |
| `GET /batches/{batchId}/items/{itemId}` | view | 快照、逐选项、调用审计、sourceChanged |
| `POST /batches/{batchId}/items/{itemId}/review` | review + AuditLog | 单行复核/撤销，expectedVersion 乐观校验 |
| `GET /batches/{batchId}/export` | export + AuditLog | 按当前过滤导出 xlsx |

### 13.1 创建批次请求

```json
{
  "selectionMode": "filter",
  "detailIds": null,
  "scope": {
    "workTypeId": 143,
    "levelCode": 5,
    "source": 0,
    "types": ["single", "multi", "judge"]
  }
}
```

不接受 `modelId/confidenceThreshold/marginThreshold/concurrency/apiKey` 等客户端字段；若请求出现也不得生效。

### 13.2 批次 VO 关键字段

```json
{
  "id": "2100000000000000001",
  "status": "running",
  "selectionMode": "filter",
  "filterSummary": "铝电解工 / 五级 / 内部 / 三种题型",
  "total": 1200,
  "done": 300,
  "pending": 850,
  "processing": 50,
  "consistentHigh": 210,
  "mismatchHigh": 20,
  "lowConfidence": 60,
  "invalid": 8,
  "failed": 2,
  "contentSuspect": 18,
  "contentInvalid": 6,
  "expectedCalls": 1236,
  "callCount": 310,
  "inputTokensExact": 240000,
  "inputTokensEstimated": 10000,
  "usageEstimatedCalls": 2,
  "outputTokens": 0,
  "modelId": "ateve-jev-v1",
  "promptVersion": "objective-answer-v1",
  "confidenceThreshold": 0.8,
  "marginThreshold": 0.2,
  "createdBy": "10001",
  "createdByName": "管理员",
  "createdAt": "2026-10-26T10:00:00",
  "startedAt": "2026-10-26T10:00:01",
  "finishedAt": null,
  "failReason": null,
  "canPause": true,
  "canResume": false,
  "canRetryFailed": false
}
```

### 13.3 结果明细 VO 关键字段

```json
{
  "id": "2100000000000000002",
  "batchId": "2100000000000000001",
  "detailId": 12345,
  "questionType": "single",
  "stemPreview": "……",
  "originalAnswer": "B. ……",
  "aiAnswer": "C. ……",
  "resultCode": "mismatch_high",
  "contentQuality": "suspect",
  "contentWarnings": [{"ruleCode":"EXCEL_SCIENTIFIC_NOTATION_SUSPECT","severity":"suspect","location":"option","optionId":"opt_2","matchedText":"1.2E+3","suggestedText":"1200","message":"选项可能存在表格数值格式问题，请人工核对"}],
  "needsReview": true,
  "consistent": false,
  "referenceConfidence": 0.06,
  "aiConfidence": 0.91,
  "minMargin": 0.78,
  "decisionWarning": null,
  "retryable": false,
  "attempts": 1,
  "version": 3,
  "reviewStatus": "unreviewed",
  "reviewNote": null,
  "checkedAt": "2026-10-26T10:01:00",
  "updatedAt": "2026-10-26T10:01:00"
}
```

### 13.4 复核请求

```json
{
  "reviewStatus": "needs_edit",
  "note": "已核对，需由题库负责人修订选项。",
  "expectedVersion": 3
}
```

错误口径：不存在/跨租户统一 404；状态冲突 409；参数错误 400；创建时超过 effectiveMaxBatchSize 返回 413（预览则 200+overLimit）；模型未开通/线程池拒绝返回 503。错误 message 用白话，不暴露 SQL、密钥或上游完整响应。

---

## 14. 导出、审计与安全

### 14.1 Excel 导出

两个 Sheet：

1. **批次信息**：批次 ID、筛选快照、模型、promptVersion、阈值、五类结果数、内容格式 suspect/invalid 数、JEV baseUrl/并发/读超时快照、调用数、精确输入 token、估算输入 token、含估算调用数、输出 token、创建人、起止时间。
2. **检查结果**：批次 ID、试题 ID、工种、等级、课程、知识点路径、来源、题型、题干、选项明细、原解析、原答案、AI 答案、原答案支持度、AI 置信度、最小概率差、一致性、检查结果、内容质量、格式规则/位置/匹配文本/suggestedText、异常/失败原因、needsReview、人工复核、复核备注、模型、检查时间、逻辑调用 ID。

- 置信度单元格为数值 + 百分比格式，不写成带 `%` 的字符串。
- 使用批次快照，不因为导出时题库已改而换内容。
- 支持当前结果/题型/复核状态过滤；空结果也生成带表头文件。
- 中文文件名：`客观题答案AI检查_{batchId后8位}_{yyyyMMddHHmmss}.xlsx`；Sheet 内保留完整批次 ID。
- 前端使用 `responseType:'blob'`，取 `response.data`，复用 Content-Disposition 中文文件名解析。
- 最多 2,000 行，同步导出可接受；导出不调用 AI。

### 14.2 审计要求

- 创建、暂停、继续、失败重试、人工复核、导出全部写操作有 `@AuditLog`。
- AI 结果可通过批次定位操作人、模型、时间、promptVersion；通过 callId 定位每个逻辑块；调用审计固定 `tool_name=objective_answer_check`，满足用户、租户、工具名、请求 ID 全链路追踪。
- 审计不得保存 API Key，不得在异常 message 中带上游完整 body；只保留截断且不含密钥的错误摘要。
- 生成内容来源为 `ateve-jev-v1`/实际 responseModel；响应模型可能不同，二者都保留。

### 14.3 数据与外发最小化

发送给第三方仅：题干、选项文本，以及可选的工种/课程/知识点路径。禁止发送：

- 原正确标记、原解析；
- `orgId/orgName/userId/checkUserId`；
- 学员、身份证、手机号、企业敏感信息；
- 其他批次或其他租户数据。

页面不渲染原始 HTML，使用纯文本，避免题库内容引入 XSS。后端是所有权限的最终裁决点，前端隐藏按钮不能替代鉴权。一期所有 HTTP 用例除权限码外还必须通过 §4.2 的平台管理员检查；数据范围是当前租户全部题库。机构/企业角色即使被误授权限码也必须 403，避免在缺少 orgId/isPrivate/userId 数据谓词时扩大外发范围。

### 14.4 XLSX 导入链防错（AI 批次上线前的前置改造）

#### 14.4.1 总体顺序

```text
原始 xlsx
  → 选择文件后“检查文件格式”（只读预检，不写库）
  → ExcelCellTextReader 生成与最终入库一致的文本
  → QuestionContentQualityInspector 对题干/选项做硬错误和软疑似检查
  → 硬错误：整批拒绝，展示行列；软疑似：用户确认后可继续
  → 正式导入时服务端重新读取并复检，不能信任预检结果
  → 入库后 AI 答案检查再次执行同一内容质量规则
  → 格式问题与答案问题分别复核；人工在既有题库编辑页修正
```

导入侧修复必须先于正式 AI 批量检查上线，否则新坏题仍会持续产生。两条现有路径必须同时改：`ExamExamDetailApplicationService` 和 `CompanyQuestionApplicationService`；不得只修其中一条。其他知识结构/竞赛导入不在本需求内批量替换，避免无测试扩散。

#### 14.4.2 共用 `ExcelCellTextReader`

建议返回结构：

```java
record ExcelCellValue(
    String text,
    String rawDisplayText,
    String numberFormat,
    List<QuestionImportIssue> issues) {}

record QuestionImportIssue(
    String severity,       // error / warning
    String ruleCode,
    String sheet,
    int row,
    int column,
    String field,
    String rawValue,
    String matchedText,
    String message) {}
```

读取规则：

| 单元格情况 | 转换规则 |
|---|---|
| STRING | 返回原字符串并 trim；科学计数法文本不擅自转换，交给内容质量规则判 suspect |
| NUMERIC，General | `BigDecimal.valueOf(d).stripTrailingZeros().toPlainString()`；禁止 `(int)`、`Double` 强转和默认科学计数输出 |
| NUMERIC，科学计数格式 | 同样按原始 double 转 plain decimal；`1.2E+3` 显示格式可还原为数值等价的 `1200`，记录转换日志而非判错 |
| NUMERIC，小数/千分位等自定义格式 | 用 `DataFormatter` 按格式取显示文本，例如 `0.50`、`12,345`，保留业务展示含义 |
| NUMERIC，日期格式 | 用 `DataFormatter` 按工作簿日期体系与单元格格式输出，禁止输出 Excel 日期序列数 |
| NUMERIC，百分比格式 | 用 `DataFormatter` 输出如 `25%`，不得把原始 0.25 与 25% 混淆 |
| FORMULA | 按缓存结果类型重新走以上分派；缓存错误/缺失/不可读取为 error，不猜公式结果 |
| BOOLEAN | 输出 `true/false`；题型、答案标记等字段再由各自业务解析器校验 |
| BLANK/NULL | 输出空串；是否允许为空由题干/选项规则决定 |
| 非有限 double、错误字面量 | error，整批拒绝 |

精度边界：Excel 数值本身只有双精度。整数字段超过 15 位有效数字或疑似标识符（身份证、长编码）时必须给出 warning，要求源文件改为文本；**不得承诺恢复第 16 位以后已经丢失的数字**。普通选项数值若出现精度 warning，也只提示，不自动四舍五入。

`QuestionContentQualityInspector` 必须检查“按上述规则转换后、真正准备入库的文本”，而不是只检查 Excel 显示文本。这样预检报告与最终落库/AI 检查看到的内容一致。reader 与 inspector 产生的 issues 按 `sheet+row+column+ruleCode+matchedText` 去重，避免同一个 Excel 错误值被两层重复计数；error 优先于 warning。inspector 必须保留实际匹配 token，reader 的单元格级问题以 rawValue 回填 matchedText，禁止去重键出现非预期 null。

#### 14.4.3 导入前只读预检

新增主题库接口：

`POST /exam/exam-details/import/validate`，权限沿用 `exam:question:create`，参数 `file/courseId`，不写数据库、不调用 AI。该接口属于既有试题导入链，**不套用 AI 工作台新增的 PLATFORM_ADMIN 限制**，以免无依据改变原导入角色；其数据权限仍按原导入逻辑执行。返回：

```json
{
  "fileSha256": "……",
  "totalRows": 400,
  "validRows": 398,
  "hardErrorCount": 2,
  "warningCount": 6,
  "errors": [{"sheet":"Sheet1","row":12,"column":4,"field":"选项A","ruleCode":"EXCEL_ERROR_LITERAL","rawValue":"#REF!","message":"该单元格包含表格错误值"}],
  "warnings": [{"sheet":"Sheet1","row":20,"column":5,"field":"选项B","ruleCode":"EXCEL_SCIENTIFIC_NOTATION_SUSPECT","rawValue":"1.2E+3","message":"请核对科学计数法显示"}],
  "truncated": false
}
```

- `validRows` 指无 hard error、可进入正式导入的行，允许其中有 warning；一行可有多个 issues，因此 totalRows-validRows 不等于 errors.size，hardErrorCount 也按 issue 计数。
- 复用与正式导入完全相同的表头识别、模板判断、单元格读取和内容质量规则；错误/警告最多返回 50 条，超出 `truncated=true`，总数不截断。
- 主题库导入弹窗增加平铺按钮“检查文件格式”。hardErrorCount>0 时禁止确认导入；warningCount>0 时展示前 20 条，用户勾选“已查看格式提醒”后才可继续。
- 预检返回 fileSha256；正式导入可携带 `validationFileSha256`，服务端重算文件哈希，不一致则 400，防止预检 A 文件却导入 B 文件。未做预检也可直接导入，但服务端仍必须执行同样校验。

#### 14.4.4 正式导入与返回契约

- 正式 `POST /exam/exam-details/import` 必须在**第一次 mapper.insert 之前**完成全文件读取与校验。存在 hard error 时整批 400、0 行入库，错误消息含总数及前 10 个行列位置；不得边导边产生脏数据。
- 软 warning 可以导入，但响应不再只返回数量，改为：

```json
{
  "fileSha256": "……",
  "importedCount": 398,
  "warningCount": 6,
  "warnings": [{"sheet":"Sheet1","row":20,"column":5,"field":"选项B","ruleCode":"EXCEL_SCIENTIFIC_NOTATION_SUSPECT","rawValue":"1.2E+3","message":"请核对科学计数法显示"}],
  "truncated": false
}
```

- `ExamExamDetailController` 和前端 `importExamDetails` 的返回类型由 `ApiResponse<Integer>` 改为上述 VO；`QuestionBankPage` 展示“已导入 N 道，另有 M 条格式提醒”及行列明细。
- `CompanyQuestionController` 与 `importCompanyQuestions` 同步修改共用读取器和返回 VO，避免企业题路径继续强转 int；其页面至少展示 warningCount 和前 20 条。企业路径复用 reader/inspector/报告 VO 和它自身的模板解析，不在本期新增企业预检接口或页面，不得硬套主题库表头。
- 两条导入均保持原租户、权限、审核状态和知识点挂载逻辑；本改造只统一单元格读取、预检和返回报告，不改变“导入即两级审核”等既有业务裁定。
- 既有命题模板导出继续使用文本单元格，补“导出→再导入”数值回归，防止往返后出现 `.0`、科学计数或小数截断。

#### 14.4.5 存量题处置步骤

1. **规则扫描**：AI 批次创建时按 §8.4 生成 contentQuality/warnings；确定异常不调 AI，疑似异常保留 AI 结论但全部进入复核。
2. **答案检查**：继续按原方案得到一致性/置信度，不用 AI 代替格式识别，也不把 suggestedText 当正确答案。
3. **人工分类**：复核人区分“答案键错误”“选项内容错误”“正常科学记号/合法长小数”，分别标记 confirmed_correct/needs_edit/deferred，并填写备注。
4. **有原始 xlsx 时做对拍**：使用同一表头映射和 ExcelCellTextReader 读取源选项，与数据库按试题 ID/原行位置逐格比较，输出源文本、库存文本、行列位置。源映射不明确时禁止按题干相似度猜配。
5. **人工修复**：在既有题库编辑功能中修正；本功能仍不写回。修复后创建新批次复查，历史批次快照不变。
6. **无原始 xlsx 时**：只能依据警告和人工专业知识修正；`0.5→0` 等已丢失精度不得自动恢复，导出中明确“原值无法由系统确定”。

---

## 15. 实施顺序（tracer bullet，禁止按层横切）

### W0：基线与合成契约探针

1. 核对两库迁移版本、当前菜单 code、构建基线。
2. 用**合成题，不用生产题**验证：单选四选项、判断、多选 5 子问题、多选 9 子问题、401、422、预算用量、响应 model/confidenceDefinition/requestId。
3. 确认多子问题响应结构与 §9 一致；若实际字段有差异，先修订本文契约再编码，不得默默兼容猜测。
4. 固化两条试题导入的当前数值缺陷复现用例：0.5、1.5、科学计数格式、日期、百分比、公式错误。
5. 在本文 §18 回填探针证据。

### W1：XLSX 导入防错纵向切片

先实现共用的 ExcelCellTextReader 与 QuestionContentQualityInspector，打通“数值单元格 → 文本转换 → 内容质量规则 → 只读预检 → 行列错误/警告展示 → 正式导入 → 数据库文本正确 → 导出再导入”一条链。两条试题导入同步改造并跑绿数值测试，先停止新增坏题。

### W2：存量内容质量纵向切片

复用 W1 的 QuestionContentQualityInspector，用一条含科学计数法疑似的存量题打通“规则检测 → 批次 contentQuality/warnings 快照 → 预览计数 → 结果标签 → 详情高亮 → Excel 导出”。同步完成三表字段与内容质量计数，不与 AI 置信度混用，也不再写第二份格式正则。

### W3：最窄 AI 端到端切片

打通“单选筛选分页 → 逐题选择 → 预览 → 创建批次 → AI 桩返回 → 结果表 → 详情 → 复核 → 导出 → 菜单权限”一条路径。先用 AI 桩，不等全部多选功能完成再组装页面。

### W4：严格预检与真实 JEV 单选/判断

完成 §8 全部规则、`decideBatch`、压缩禁用、响应零信任、审计；单选/判断结果算法和单测全绿。contentQuality=invalid 零调用，suspect 请求仍使用原存储文本。

### W5：多选分块与完整置信算法

完成每 5 选项分块、整题原子聚合、两个置信度、概率差、AI 组合卫兵；补齐多选边界测试。

### W6：异步控制与历史

完成限量、活动批次唯一槽、run_id 租约/心跳、claimAttempt fencing、暂停/继续/失败重试、熔断锁存、过期接管、拒绝补偿、计数一致性、标签页轮询和三类收尾刷新。

### W7：交互收口

完成两种选择模式、五类结果卡 + 内容格式卡、详情警告高亮、复核乐观锁、Excel、长文本/空态/错误态/权限态和 ResourceCenter 卡片；导入预检按钮与警告确认同步可用。

### W7.1：v1.5 模型配置集中入库收口

完成后续迁移、`feature_scope`、专用 JEV 配置行、AI Service 配置解析、通用模型下拉过滤、FAQ 配置弹层 JEV 字段和 API Key 占位保护。迁移已应用的环境必须用新迁移，禁止修改旧迁移；完成本节后重新跑 W8/W9 的相关回归。

### W8：自动化全验证与 verifier 独立复核

执行 §16；修复缺陷后由 verifier 按本文逐项复核，不能由实施者自报替代。

### W9：200 道标定与受控放量

按 §17 先保持生产批量上限 200，完成人工标定和内容格式样本验收；达标后才把上限恢复 2,000。未达标则保持试点或关闭，不做全量。

---

## 16. 自动化测试方案

所有模型测试使用桩，不依赖真实 API Key。前端无 MSW，使用 `vi.mock` mock API 模块；本机 jsdom 重交互用例显式 30s/60s 超时。HttpServer 桩必须 `setExecutor`。

### 16.0 验证口径（2026-10-28 回填）

本节复选项只代表对应版本代码已实现，且至少有一个自动化测试或集成测试覆盖该行为。
v1.4 已勾选项的证据见 §18.2；**v1.5 配置集中入库新增项（§16.1.1、§19.6）已实施并通过目标测试，证据见 §18.2 的 v1.5 行**。
其余未勾选项为环境/人工依赖（导入页 e2e、空库全量迁移、200 道标定），不代表 v1.4 代码缺失。

### 16.1 后端单元测试

**A. `ExcelCellTextReaderTest` / `QuestionImportValidationTest`**

- [x] NUMERIC General 的 0.5、1.5、0、负数均保留数值含义，不发生 int 截断；科学计数格式输出 plain decimal。
- [x] 自定义 `0.00`/千分位、日期、百分比按 DataFormatter 保留显示语义；BOOLEAN/BLANK 正确分派。
- [x] FORMULA 按缓存类型读取；缓存错误/缺失、NaN/Infinity、Excel 错误字面量均为 hard error。
- [x] 超过 15 位的标识/整数给精度警告，不宣称恢复已丢失位数。
- [x] 预检与正式导入使用同一文本转换和表头映射；预检不写库、不调 AI，hard error 禁止导入。
- [x] 同一单元格被 reader 与 inspector 命中同一规则时按行列+规则+matchedText 去重，只计一次；reader 以 rawValue 回填 matchedText。
- [x] 预检 fileSha256 与正式文件不一致时拒绝；warning 需用户确认，warning 超过 50 条只截断列表不截断总数。
- [x] 两条试题导入的 hard error 都在首次 insert 前整批失败；成功响应 importedCount/warnings 正确，原租户/审核/知识点逻辑不变。
- [x] 导出→再导入不产生 `.0`、科学计数或小数截断；既有导入测试全部保持绿色。

**B. `QuestionContentQualityInspectorTest`**

- [x] ASCII/全角科学计数法均命中 suspect；`1.2E+3` 的 suggestedText=1200，但原题干/选项和 AI 输入完全不变。
- [x] `1E+1000000000`、`1E-1000000000`、超 128 字符 token 均快速返回 suggestedText=null；预计输出 >4096 字符时不得先调用 toPlainString，原文保持不变。
- [x] `#REF!/#DIV/0!/NaN/Infinity` 为 invalid 且零 AI 调用；`####`、U+FFFD 为 suspect，不误当确定错误。
- [x] `1.6×10^-19`、公式、单位、职业编码不命中科学计数规则；普通循环小数不自动判错或截断。
- [x] 小数位 ≥16、连续 0/9 浮点尾巴只标 suspect；规则不设置“0/1 即错误”。
- [x] 多 warning 严重度按 invalid>suspect>clean；题干 location/optionId、matchedText、suggestedText、message 完整。
- [x] suspect 的 AI resultCode/confidence 照常但 needsReview=true；结构正常且内容 invalid 时映射 CONTENT_QUALITY_INVALID。
- [x] 结构与内容双重 invalid 时保留先产生的结构 invalidReason，warnings 完整，invalidCount=1、invalidContentCount=1、零 AI 调用。
- [x] 初始 pending 和 retry-failed 刚复位的 suspect 行 needsReview=false，不进入可执行复核队列；结果落定后再按规则入队。
- [x] `consistent_high+suspect` 和 `mismatch_high+suspect` 都进入默认复核队列，复核后离开但历史警告保留。
- [x] 重试保留 contentQuality/warnings；批次 content_suspect/content_invalid 计数与五类 AI 结果守恒不冲突。

**C. `ObjectiveQuestionParserTest`**

- [x] 标准 single/multi/judge JSON 数组解析，boolean/string/numeric 正确标记。
- [x] 对/错、正确/错误、T/F、√/× 的判断归一。
- [x] 判断空占位、纯字母 C/D 占位清理；答案1/答案2 和专业文本不误判。
- [x] 非 JSON、空数组、空题干、空选项、重复选项。
- [x] 单选 0/2 个正确项，多选 0/1 个正确项，多选全正确允许。
- [x] 单选 2/4/11 选项，多选 2/5/9 选项；>16 拦截。
- [x] 图片、data:image、audio、video 拦截；无课程关联不拦截。
- [x] HTML 纯文本化；长文本不静默截断。
- [x] optionId 与原下标稳定，sourceHash 对 type/content/answer 单独改变均敏感；SQL NULL answer 可落快照且标 invalid，NULL 与空串哈希不同。

**D. `JevObjectiveAnswerCheckAiServiceTest`**

- [x] 对实际请求 JSON 和 state/instructions/criteria 断言不含原答案、originalCorrect、isCorrect、原答案 key 集合、answerSnapshot、resolve、org/user 信息；防止直接序列化归一对象泄密。
- [x] criteria key 与 optionId 映射可逆；确定性轮换不改变最终答案映射。
- [x] 多选 5 项一块、6 项两块、9 项两块、16 项四块；每块 state 含全部选项。
- [x] 任意一块失败则整题无部分 AI 答案；重试重新执行全部块。
- [x] 6/9 选项在第 2 块失败时，auditSink 仍收到前序成功块和当前失败块的完整 outcome；审计写失败使整题可重试失败而非静默成功。
- [x] 多选 AI 少于两个正确项时产生 `AI_COMBINATION_INVALID`。

**E. `ObjectiveAnswerCheckDomainServiceTest`**

- [x] 单选原答案支持度按全部原标记 min 计算，示例 §10.2 得 0.85；两候选/多候选均验证其等于原正确候选概率。
- [x] 判断原答案分别为 T、F 时支持度分别取 p(T)、p(F)；多选原正确取 p(Y)、原不正确取 p(N)，再取 min。
- [x] 单选/判断全局指标与多选逐选项指标分组反序列化，不把全局 confidence 错写到每个选项。
- [x] 高置信一致、不同、高置信不一致、低置信四种组合。
- [x] 高置信一致还必须 referenceConfidence≥θ；不一致不能误用该条件。
- [x] θ=0.80、δ=0.20 的等于边界通过，低一点即不通过；先判定后落库舍入。
- [x] 多选取 confidence/margin 最小值，不做概率连乘。
- [x] 多选答案集合比较与顺序无关；optionId 而非展示字母决定一致性。
- [x] `AI_COMBINATION_INVALID` 强制 low_confidence。
- [x] invalid/failed 不伪造 consistent/confidence。

**F. `JevDecisionClientTest` 增量**

- [x] 多子问题请求 JSON 与响应 Map 全解析，usage/model/confidenceDefinition 完整。
- [x] 缺一个子答案、choice 越界、confidence 非数/越界、缺/多概率 key、概率和异常均失败。
- [x] choice=B 但 A 概率更高时按响应矛盾失败；并列最高 margin=0；不得错误产生 mismatch_high。
- [x] metadata/responseModel 缺失不失败；usage 缺失按完整请求体估算并标记 estimated；负数/非整数 token 拒绝。
- [x] 401/422 不重试，429/5xx 退避，预算 422 可分类。
- [x] 请求头无 `Accept-Encoding`，覆盖 Brotli 回归。
- [x] 原 knowledgefix 单问题测试全部保持绿色。

**G. `ObjectiveAnswerCheckBatchServiceTest`**

- [x] IDs/filter 两种选题共用 scope；空选择、未知 type/source/level、超过 effectiveMaxBatchSize 均拒绝；预览超限返回 overLimit，创建才 413。
- [x] 试点上限 200 时 201 道不能创建；batchMaxSize>2,000、阈值越界、并发/超时越界均启动校验失败；选项 16 和分块 5 不可被配置覆盖。
- [x] 跨租户/已删除/范围外 IDs 不静默丢弃。
- [x] 批次和明细提交前 executor 不被调用；提交后才投递。
- [x] 批量插入不用 `Db.saveBatch`；2,000 行按 200 行分批。
- [x] 同租户并发创建只有一个成功；不同租户不互堵；paused/done/无 processing 的 failed 释放活动槽。
- [x] invalid 行零 AI 调用；全部 invalid 时直接 done。
- [x] 编排和 Worker 均 try/finally 清理租户；静态 mock SecurityContext 验证异步链零调用，自定义 SQL 写入的 tenant/user/审计时间正确。
- [x] 暂停不再派发，在飞行行结束后 paused；继续只处理 pending。
- [x] 条件认领防两个 Worker 处理同一行；旧 run_id 不能派发，旧 claimAttempt 晚到结果影响 0 行且不重复计数。
- [x] 有效租约批次在蓝绿新节点启动时不被重置；过期租约才可接管，旧编排器随后自行退出。
- [x] 401 立即锁存 failed；pausing→failed 合法；第 5 次失败后再成功不能撤销 failed。
- [x] failed 尚有 processing 时保留活动槽和心跳，归零才释放；崩溃恢复也扫描这种 failed，遗留 processing→pending。
- [x] 系统性连续 5 次熔断；预算 422 不可重试且不熔断；普通 422 连续 5 次熔断。
- [x] 创建/继续/重试编排池拒绝均持久化 failed 并释放无 processing 的活动槽；Worker 拒绝把刚认领军行恢复 pending。
- [x] retry-failed 只复位 retryable=1；同事务回减 done/failed、清旧 AI/复核字段、占用新 run；复位 0 不运行，重试重跑多选全部块。
- [x] 收尾当前读/条件更新覆盖“暂停与最后一行同时完成”竞态。
- [x] `total=done+pending+processing`，三种 success 计数之和等于 success；晚到重复调用只增实际用量不增结果计数。
- [x] 配置 disabled/key 空禁止创建和继续，但历史查询不受影响。

**H. `ObjectiveAnswerCheckReviewServiceTest` / `ExportServiceTest`**

- [x] 四种复核状态、撤销复核、500 字限制、pending/processing 禁止复核。
- [x] expectedVersion 冲突返回 409；同秒两个复核请求也只有一个成功；复核只更新 item 并递增 version，verify `examDetailMapper` 从未 update。
- [x] 两个 Sheet、中文文件名、百分比格式、精确/估算 token 与 usageEstimatedCalls、空结果表头、当前过滤、快照不随题库变化。
- [x] 多选/失败/invalid/null 字段导出无 NPE；contentQuality/warnings/suggestedText/needsReview 完整，2,000 行导出通过。

#### 16.1.1 v1.5 模型配置集中入库增量测试

- [x] 新迁移幂等：`feature_scope` 默认 `common`、索引存在；专用行只插入一次，`api_key` 精确等于 `模型API Key走配置文件`，默认 `status=disabled`。
- [x] `ObjectiveAnswerCheckAiService` 对配置行缺失、disabled、baseUrl/modelId 为空、DB 占位值、环境密钥为空均返回未就绪；环境密钥有效后使用 DB 的 baseUrl/modelId/readTimeout/concurrency。
- [x] 环境变量真实密钥绝不写入 VO、批次、审计、日志；descriptor 只暴露非敏感连接配置与就绪布尔值。
- [x] DB 改 baseUrl/并发/超时/状态只对下一批次生效；并发超过 Worker 池上限被防御性钳制并告警；批次表 `jev_base_url/jev_concurrency/jev_read_timeout_seconds` 快照在续跑/重启后仍保持创建时值。
- [x] FAQ 转换、理论生成、技能生成的 enabled 模型列表均不出现 `OBJECTIVE_ANSWER_CHECK_JEV_MODEL`；模型配置管理页仍能看到该行及适用范围。
- [x] 模型配置更新：本行不传 apiKey 保留占位；传占位值不修改；传真实密钥 409；VO 显示“配置文件提供”。
- [x] `FaqConfigDialog` 编辑本行显示适用范围与 JEV 并发/读超时，API Key 输入禁用并提示环境变量名；普通 LLM 行不显示 JEV 字段。

### 16.2 后端集成测试

`ObjectiveAnswerCheckControllerIT` + 可控 AI 桩；导入侧增加 `ExamExamDetailImportNumericCellIT` / `CompanyQuestionImportNumericCellIT`：

- [x] 两条导入路径使用真实 xlsx 数值/公式单元格验证 0.5、1.5、科学计数、日期、百分比、hard error 整批回滚与 warning 返回；不只使用字符串单元格测试。
- [x] 主题库 `/import/validate` 验证成功/失败/权限/哈希不一致路径；正式导入与预检使用同一报告口径。
- [x] 未登录 401；无 view/submit/review/export 分别 403；直接调 API 也受控。
- [x] 机构/企业角色即使被赋予四个权限码，因无 PLATFORM_ADMIN 仍全部 403；平台管理员按一期口径可查当前租户全部题库。
- [x] 工种×等级×来源×题型组合筛选准确；levelCode 与 0～4 存储转换正确。
- [x] 无筛选时无课程题可检查；限定工种/等级时无课程题被排除；查询无重复行。
- [x] 主链路：预览→创建→轮询至 done→结果→详情→复核→导出。
- [x] 创建请求中的伪造 model/threshold/apiKey 不生效。
- [x] AI 桩捕获请求，断言没有原答案、解析和 suggestedText，只有当前租户题目；contentQuality=invalid 零调用，suspect 请求保持原存储文本。
- [x] contentQuality/warnings 快照、批次内容计数、needsReview 队列与复核后出队正确。
- [x] 跨租户批次/明细/复核/导出均 404，不泄露存在性。
- [x] 暂停、继续、失败重试、过期接管与活动批次冲突的集成路径；旧节点有效租约不被新节点启动恢复误清。
- [x] 6/9 选项中途失败时成功/失败块审计齐全，重试后 token 与实际逻辑调用汇总一致。
- [x] 复核前后 `exam_exam_detail.content/answer/update_time` 不变。
- [x] 审计行包含用户、租户、模型、promptVersion、callId、token、耗时；无 API Key。
- [x] 导出成功与无权限/不存在批次失败路径。

### 16.3 前端 Vitest / React Testing Library

**`questionImportValidation.test.tsx`**

- [x] 选择文件后可主动预检；hard error 显示行列并禁用导入，warning 展示前 20 条且须勾选已知悉。
- [x] 预检 A 文件后改选 B 文件会清空预检状态并触发哈希校验；直接导入也有服务端错误提示。
- [x] 两条导入页面均正确消费 `{importedCount,warningCount,warnings}`，不再按 number 读取；没有漏改前端 API 类型。

**`objectiveAnswerCheckSelection.test.tsx`**

- [x] 工种路径展示、等级联动、来源含 AI 生成、题型默认三项。
- [x] 跨页勾选保留；筛选变化清空；filter 全部与逐题模式互斥。
- [x] >2,000 禁止“筛选全部”，可改逐题；预览数与确认弹窗一致。
- [x] 创建防双击，成功切到结果页；后端 409/413/503 显示白话错误。

**`objectiveAnswerCheckResults.test.tsx`**

- [x] 五类结果卡 + 内容格式卡与“需人工复核”默认视图，低置信行仍保留一致/不一致；contentQuality 与 resultCode 可组合过滤。
- [x] `consistent_high+suspect` 仍显示两个标签并进入复核；复核后出队但可从全部找到。
- [x] 原答案支持度和 AI 置信度不混列；AI 组合告警可见。
- [x] 详情逐选项展示；格式 warning 高亮位置/匹配文本/suggestedText，明确未替换未发送；原始 HTML 不被渲染；题库已变更提示。
- [x] 单行复核、撤销、并发冲突后刷新；无任何“采纳 AI 答案”按钮。
- [x] active=false 不轮询；离开活动状态进入 paused/failed/done 三种情况都停止轮询并失效刷新尾部计数；手动刷新有 batchId 守卫。
- [x] 服务端分页 current/pageSize/onChange 同源更新，不写死 pageSize。
- [x] batchId/itemId 全程字符串，不经 `Number()` 转换。

**`objectiveAnswerCheckExport.test.tsx`**

- [x] Blob 取 `.data`、中文文件名、导出过滤参数、空结果与失败提示。

### 16.4 Playwright 主链路

使用测试后端 AI 桩，不依赖真实模型：

- [ ] 在主题库导入页对含数值小数的 xlsx 执行预检和正式导入，确认选项值未截断；含 hard error 的文件 0 行入库。
- [x] 管理员从“课程管理→支持保障→资源管理”进入新菜单。
- [x] 筛选并跨页勾选单选/多选/判断各至少一道，预览、创建、看到进度终态。
- [x] 打开高置信不一致详情，看到逐选项概率，标记“需要修改”，题库答案未改变；含科学计数法疑似的题同时展示格式警告并进入复核队列。
- [x] 导出并验证下载文件名；历史批次可重新进入且快照一致。
- [x] 无 submit 权限角色看不到创建按钮，直接 POST 仍 403。

### 16.5 构建、迁移与全量回归

- [x] `mvn compile -DskipTests` 0 错，新迁移进入 `target/classes`。
- [x] 后端目标测试、新增 IT、既有 JEV/knowledgefix 与两条试题导入测试全绿。
- [x] 后端全量回归无新增失败；既有基线必须逐项隔离证明，不得笼统写“无关”。
- [x] `npm run build` 0 错（包含 `tsc -b`）；新增 Vitest 全绿；Playwright 主链路通过。
- [ ] 新迁移在空库全量链执行成功，种子幂等，三表 BaseEntity 列与实体 getter 对齐；空库核对父菜单/菜单/权限，克隆库另核对 10159/2000 的 1 条角色菜单和 4 条角色权限。
- [ ] 若空库被既有踩坑 #34 的旧迁移断链阻塞，必须如实标为未验证并记录具体旧迁移；同时完成“测试库结构克隆 + 本迁移增量执行”验证，不得宣称空库通过，也不得修改已应用旧迁移。
- [x] 所有新文件 UTF-8 without BOM；无真实密钥、生产题目样本或客户数据入库。

---

## 17. 200 道标定、上线与回退

### 17.1 标定集构成

首轮固定 200 道，由题库专家在查看 AI 结果前先独立给出真值：

- 150 道人工确认原答案正确：单选/多选/判断各 50；
- 30 道人工确认原答案错误：单选/多选/判断各 10；**至少 10 道必须是自然存在且经专家确认的错误**，其余可为人工翻转构造样本，两类分开报告；若自然错误不足 10 道，本轮质量验收为“未验证”，不能用构造样本完全替代；
- 20 道边界/结构样本：空正确项、单选多正确项、判断异常、无课程、超长/媒体，以及 ASCII/全角科学计数法、Excel 错误值、正常科学记号、合法长小数、异常浮点尾巴等，不计入 clean 集答题准确率。

覆盖至少 3 个工种、内部/外部两种来源；若 AI 生成或赛前复习未来纳入首批，再单列统计，不用少量样本混算。

### 17.2 指标与放量门槛

| 指标 | 门槛 | 说明 |
|---|---:|---|
| 高置信一致的人工正确率（总体） | ≥95% | 防止给用户错误“安心感”，最严格 |
| 高置信一致的人工正确率（单题型） | 各 ≥90% | 不允许多选短板被总体掩盖 |
| 高置信不一致的有效疑点率 | ≥80% | 人工认同原答案确需修改/存在明确问题的比例 |
| 已知错误的检出率（30 道总体及至少 10 道自然错误） | 各 ≥70% | 高置信不一致 + 被专家认可的低置信疑点；构造样本另报，不替代自然错误门槛 |
| 结构预检准确率 | 100% | 20 道边界样本不得误送 AI 或漏标 |
| 内容格式规则准确率 | 100% | hard 必须拦截、suspect 必须提示；正常科学记号不得误报，合法长小数不得自动改值 |
| XLSX 数值保真 | 100% | 0.5/1.5/日期/百分比/公式用例无截断，hard error 零入库 |
| 技术失败处理 | 100% 可解释 | 无静默成功、无半份多选结论、无跨租户数据 |

**零分母规则**：总体/某题型若没有高置信一致样本，或没有高置信不一致样本，相应指标为“未验证/未达标”，不得记 100%、不得跳过、不得因全部低置信而放量。自然错误不足 10 道同样属于未验证。构造样本只作补充观察，总体 30 道和至少 10 道自然错误必须分别满足 70% 检出率。

AI 准确率指标只使用 `contentQuality=clean` 的标定题；suspect 单独报告格式识别命中和人工结论，不能混入 clean 集抬高或压低模型准确率。正常科学记号/合法长小数被误标时，按内容格式规则缺陷处理，不得通过放宽规则到完全失去检测能力来“消误报”。

同时报告高置信覆盖率、低置信占比、每题型 token、P50/P95 单行耗时；覆盖率只作实用性观察，不得为提高覆盖率而降低阈值。

若未达任一质量门槛：

1. 保持 `OBJECTIVE_ANSWER_CHECK_BATCH_MAX_SIZE=200`，不得放量；
2. 优先提高阈值、修订 instructions、检查位置偏置与多选分块，不得降低标准；
3. 若 responseModel/confidenceDefinition 变化，重新标定；
4. 仍不达标则保持 `enabled=false` 或仅保留测试环境试点，功能不得宣称验收通过。

### 17.3 受控上线步骤

1. 先完成两条 XLSX 导入防错、预检和数值保真验收；未通过时不得开启正式 AI 批量检查。
2. 部署 v1.5 后续迁移、后端 jar、前端 dist；`OBJECTIVE_ANSWER_CHECK_JEV_MODEL` 配置行保持 `disabled`，功能 `enabled=false`，不在应用配置中写密钥。
3. 在服务器部署配置/环境变量写入真实 `OBJECTIVE_ANSWER_CHECK_JEV_API_KEY`，确认数据库 `api_key` 仍固定为 `模型API Key走配置文件`。
4. 核对菜单→权限→角色授权三步链和页面无权限表现。
5. 试点期在模型配置管理页将 `OBJECTIVE_ANSWER_CHECK_JEV_MODEL` 置为启用，再设功能 `enabled=true`、`BATCH_MAX_SIZE=200`，仅平台管理员使用。
6. 跑合成冒烟、200 道标定及内容格式样本，回填 §18。
7. 达标后把上限调回 2,000；yml/DB 配置只影响新批次，历史批次快照不变。
8. 正式批次优先复核全部高置信不一致与内容格式警告；每批至少抽查 50 条高置信结果，记录人工结论。

### 17.4 回退

- 立即回退：暂停活动批次，在模型配置页将 `OBJECTIVE_ANSWER_CHECK_JEV_MODEL` 置为禁用；如需关闭整个功能再设 `enabled=false` 并重启。历史结果和审计保留，数据库占位值不回填真实密钥。
- 代码/菜单回退不影响题库，因为一期从未写 `exam_exam_detail`。
- 模型异常时只切回关闭状态，**不得改成另一个模型名称继续跑而不重新标定**。

### 17.5 标定集复用于解析存疑验收

同一 200 道标定集（含专家真值）复用于解析存疑功能（§0.6），无需另造样本：

- **30 道人工确认原答案错误**（自然/构造分开）：测存疑检出率 = 生成【存疑】且经专家认可的比例；自然错误与构造样本分别报告，口径同 §17.2。
- **150 道人工确认原答案正确**：测误存疑率 = 被误标【存疑】的比例；误存疑集中的工种/知识点需反馈 prompt 维护人，用于调整存疑门槛。
- **20 道边界/结构样本**：观察存疑功能在媒体、结构异常、内容质量 suspect/invalid 题上的拦截行为（解析存疑侧后续接入媒体与内容质量规则后复测）。

存疑侧指标只用于解析存疑方案的灰度与门槛调整，**不计入本方案 §17.2 的任何指标**；本方案的 AI 作答结论也不直接当作存疑侧真值（模型不同、输出形态不同）。

---

## 18. 实施验证记录（2026-10-28 回填）

### 18.1 合成探针

| 项目 | 结果/证据 | 日期与执行人 |
|---|---|---|
| 单选 4 选项 | 未执行真实 JEV 探针；请求结构与响应零信任由 `JevDecisionClientBatchTest` / `JevObjectiveAnswerCheckAiServiceTest` 以 HttpServer/桩覆盖 | 2026-10-28 实施者 |
| 判断 T/F | 同上；`ObjectiveQuestionParserTest` 覆盖对/错、正确/错误、T/F、√/× 归一 | 2026-10-28 实施者 |
| 多选 5 子问题 | `JevObjectiveAnswerCheckAiServiceTest` 验证每块 ≤5、16 选项 4 块 | 2026-10-28 实施者 |
| 多选 9 子问题与 expanded tokens | 9 选项分 2 块、块数与失败审计已测；真实 expanded tokens 未探针 | 2026-10-28 实施者 |
| 401/422/预算/重试 | `JevDecisionClientTest` + `JevDecisionClientBatchTest`：401/422/预算 422/429 退避 | 2026-10-28 实施者 |
| responseModel/confidenceDefinition/requestId | `JevDecisionClientBatchTest` 覆盖可空与记录 | 2026-10-28 实施者 |

> 未执行真实生产 JEV 调用的原因：仓库不保存生产 API Key，且方案要求先用合成题探针；
> 本次以本地 HttpServer 桩验证协议与零信任解析，真实模型探针需在配置密钥的环境执行后再回填本节。

### 18.2 自动化与构建

| 检查 | 命令/证据 | 结论 |
|---|---|---|
| XLSX 数值/预检/双导入路径 | `mvn -o test -Dtest=QuestionImportNumericCellIT`（5/5） | 通过 |
| 内容质量规则与批次组合 | `QuestionContentQualityInspectorTest`（7/7）+ `ObjectiveAnswerCheckApiIT` 的 suspect/invalid 断言 | 通过 |
| v1.5 模型配置集中入库 | `V20261029_001` + `V20261029_003` 已在测试库应用并核对种子/列/存量范围；v1.5 单测 34/34；答案检查 IT 11/11（Api/Limit/RetryFromDone）；理论/技能相关回归通过；前端套件 21/21 + `npm run build` 通过；stash 冲突按交接说明仅恢复安全集前端，未覆盖活跃 backend 文件 | 通过（真实 JEV 密钥与生产部署未执行） |
| 后端目标测试 | 9 个新增单测类，`Tests run: 69, Failures: 0` | 通过 |
| 后端集成测试 | `ObjectiveAnswerCheckApiIT` 7/7、`QuestionImportNumericCellIT` 5/5、`ObjectiveAnswerCheckLimitIT` 2/2 | 通过（14/14） |
| 后端全量回归 | `mvn -o test -Dtest='*Test'`（2026-10-29 v1.5 后）：2489 例，9F+4E | 既有基线（competition 7F+3E、defense 1F、knowledgefix 1F），无 v1.5 新增失败 |
| 前端 Vitest/build | `npm run test:objective-answer-check`（19/19）+ `npm run build`（tsc -b + vite） | 通过 |
| Playwright | `STAGE_E2E_MANAGE_SERVER=1 npx playwright test --config playwright.chrome.config.ts e2e/objective-answer-check.spec.ts`（2/2） | 通过 |
| 空库迁移/克隆增量迁移 | 测试库 `gjjddb2026` Flyway 已应用 `20261028.005`（success=1），三表/菜单/4 权限/角色授权已核对；**空库全量链未执行**（受既有历史断链限制，未修改旧迁移） | 部分通过，空库项未验证 |
| verifier verdict | 2026-10-29 独立复核：红线静态审查（不写回题库/异步零 SecurityContext/AbortPolicy/密钥不出仓/迁移幂等）通过；后端 89 例目标测试 + 前端 19 例 Vitest + tsc/vite build 全绿；Playwright 2/2 以实施者报告为据未复跑。复核中**发现并由 verifier 修复 2 处缺陷**：①`acquire` SQL 状态白名单漏 `done`，导致 done 批次“重试失败项”必 409（方案 §11.1 done→running）——已补 `done` 并新增真实库 `ObjectiveAnswerCheckRetryFromDoneIT`（1/1）验证；②判定使用运行期 properties 阈值而非批次固化快照（§9.1/§17.3 历史批次阈值不变）——已改为取批次 `confidence_threshold/margin_threshold`，新增 `processClaimedUsesBatchSnapshotThresholds` 单测（BatchServiceTest 9/9）。另记录不阻塞项：菜单 `menu_type='page'` 与方案/兄弟菜单 `menu` 不一致（功能无碍，建议后续统一）；创建批次初始 lease 用 JVM 时间（acquire 立即以 DB 时间覆盖，影响可忽略）。空库全量迁移与 200 道标定仍未验证，**放量门槛不变** | **PASS（代码层）**；标定/空库项仍未验证 |

> v1.5 配置集中入库变更发生在上述 verifier 复核之后，不在该 PASS 范围内；§16.1.1/§19.6 已由实施侧补证据，但仍需独立 verifier 对 v1.5 做一轮单独复核，当前不得把 v1.4 的 PASS 当作 v1.5 结论。

**未勾选项说明（v1.5 待实施项除外）**：

- §16.4 第 1 条：主题库导入页的“预检+正式导入”未做 Playwright（导入链已由 `QuestionImportNumericCellIT` 以真实 xlsx 覆盖）。
- §16.5 / §19.5 空库全量迁移：受既有历史迁移断链限制，只验证了测试库 Flyway 增量（success=1），未宣称空库通过，也未修改旧迁移。
- §19.2 200 道标定：需真实 JEV 密钥、200 道专家真值样本与人工判定，编码环境无法执行。
- §19.5 verifier 独立复核：本次由实施者自检，未冒充独立 verifier。

**全量回归结果**：`mvn -o test -Dtest='*Test'` → **2462 run / 9 failures / 5 errors / 15 skipped / BUILD FAILURE**。
失败集中在 `competition`（schema 契约、player/practical/outbox）、`DefenseManagement`、`CoursewareE2ETest`（登录 500）、`KnowledgeFixControllerPermissionTest`，
均与本功能无文件交集；本功能 9 个新增单测类（69 例）全绿。按“逐项隔离”要求如实记录：**未宣称全量零失败，也未修改上述模块测试**。

### 18.3 200 道标定

| 指标 | 实测 | 是否达标 |
|---|---|---|
| 高置信一致人工正确率（总体/各题型） | 未执行（需 200 道专家真值样本） | 未验证 |
| 高置信不一致有效疑点率 | 未执行 | 未验证 |
| 已知错误检出率（自然/构造分开） | 未执行 | 未验证 |
| 结构预检准确率 | 未执行 200 道标定；单测覆盖 20 类结构规则 | 未验证（标定口径） |
| 内容格式规则准确率/正常科学记号误报 | 未执行标定；单测覆盖正常科学记号不误报 | 未验证（标定口径） |
| XLSX 数值保真与 hard error 零入库 | 自动化用例覆盖 0.5/1.5/#REF!/哈希不一致 | 通过（自动化口径） |
| 高置信覆盖率/低置信占比 | 未执行 | 观察项 |
| token 与 P50/P95 耗时 | 未执行 | 观察项 |

> 结论：**标定未完成，生产必须保持 `OBJECTIVE_ANSWER_CHECK_BATCH_MAX_SIZE=200` 且不得以“接口已调通”验收放量。**

### 18.4 本次新增/修改文件

- 迁移：`V20261028_005__objective_answer_check.sql`
- 后端：`exam/answercheck/**`（controller/dto/entity/mapper/service/vo/config/parser）、`exam/support/{ExcelCellTextReader,QuestionContentQualityInspector,QuestionImportIssue,ExcelCellValue,QuestionContentQualityWarning}`、`ai/{dto,config,service,infrastructure}` 下相关类、两条导入链与控制器、`PlatformApplication`、`application.yml`、测试配置
- 前端：`pages/question_bank/ObjectiveAnswerCheck*`、`pages/exam/QuestionImportModal.tsx`、`api/exam.ts`、`types/questionImport.ts`、`main.tsx`、`ResourceCenterPage.tsx`、相关单测与 e2e
- 文档：本文件

## 19. 验收清单

### 19.1 业务范围

- [x] 菜单名、层级、三题型、四来源、工种和等级筛选完全符合已确认口径。
- [x] 逐题跨页勾选和当前筛选全部均可用；单批 2,000 硬上限不可绕过。
- [x] 无课程题在未限定工种/等级时可检查，限定后正确排除。
- [x] 结果同时有一致性、原答案支持度、AI 置信度，不是只有一个“通过率”。
- [x] 低置信保留一致/不一致事实；高置信不一致只叫“疑似”。
- [x] 内容质量 suspect/invalid 与 AI 结果独立展示并进入复核，复核后出队但历史快照保留。
- [x] 一期无第二模型、无文字理由、无自动/一键写答案、无批量复核。

### 19.2 AI、内容质量与数据正确性

- [x] AI 请求不含原答案、originalCorrect/isCorrect、原答案 key 集合、answerSnapshot、suggestedText、解析、组织或用户信息。
- [x] ASCII/全角科学计数、Excel 错误值、异常精度、正常科学记号按 §8.4 分级，suggestedText 只供人工不替换文本。
- [x] 两条 XLSX 导入不再强转 int；预检/正式导入同规则，日期/百分比/公式正确，hard error 零入库，返回契约和前端调用同步。
- [x] 已落库的信息丢失不自动恢复；有源文件按明确映射对拍，无源文件只提示人工，不宣称全部可识别。
- [x] 单选/判断/多选映射正确，多选每 5 项分块且整题原子聚合。
- [x] 原答案支持度、AI 置信度、最小概率差严格按 §10 计算，不连乘、不提前舍入。
- [x] 所有结构异常按 §8 处理，判断占位不错误兜底，媒体不静默剥离。
- [x] 多选 AI 少于两个正确项不会成为高置信建议。
- [ ] 200 道标定全部达标并留有证据；未达标没有放量。

### 19.3 异步、权限与审计

- [x] 快速返回 + 异步 + 进度轮询；线程池 AbortPolicy，无 SecurityContext 异步调用。
- [x] 编排与 Worker 租户设置/清理、提交后投递、run_id 租约、claimAttempt fencing、当前读收尾均有测试；异步写库不触发 SecurityContext 审计填充。
- [x] 暂停/继续/失败重试/熔断锁存/过期接管/拒绝补偿状态正确，计数守恒，旧 Worker 晚到写入不能覆盖或重复计数。
- [x] view/submit/review/export 后端鉴权、PLATFORM_ADMIN 数据范围和跨租户测试通过；非管理员有权限码也不能使用。
- [x] 所有写操作有操作日志；模型调用有用户、租户、模型、时间、callId、token、耗时。
- [x] API Key 只在环境配置和内存，不出现在仓库、数据库快照、日志、导出。
- [x] 人工复核有乐观锁，且 `exam_exam_detail` 未发生任何更新。

### 19.4 页面与导出

- [x] 三页签、统计卡、详情抽屉、批次历史、复核、导出主链路顺畅。
- [x] 标签页不可见停止轮询，进入 paused/failed/done 都主动刷新；手动刷新不绕过大 BatchId 守卫。
- [x] 长题干、空关联、失败、异常、无权限、未开通、超限都有清晰白话反馈。
- [x] 操作平铺；雪花 ID 无精度丢失；原始 HTML 不渲染。
- [x] Excel 两 Sheet、快照、百分比、中文名、当前过滤、空结果均正确；contentQuality/warnings/suggestedText/needsReview 与精确/估算 token 完整。

### 19.5 工程门槛

- [x] Flyway 取号经双库核查，未改历史迁移；三表和菜单权限种子幂等。
- [ ] 空库迁移通过；若受既有断链影响已如实标红并完成克隆增量验证，没有虚报。
- [x] 后端编译、目标测试、IT、全量回归无新增失败；两条既有导入链及前端返回类型没有漏改中间层。
- [x] 前端 build、Vitest、Playwright 全绿。
- [x] 无 BOM、无真实密钥/客户数据入库、无 `Db.saveBatch`、无 `CallerRunsPolicy`。
- [ ] verifier 独立复核 verdict=PASS，本文验证记录与验收证据已回填。

### 19.6 v1.5 模型配置集中入库

- [x] 新后续迁移未修改已应用迁移；`feature_scope` 与老数据 common 默认值正确；专用 JEV 行幂等且 `api_key` 精确为 `模型API Key走配置文件`。
- [x] 真实密钥只从 `OBJECTIVE_ANSWER_CHECK_JEV_API_KEY` 环境变量读取；DB 占位值、空串、`PENDING_USER_INPUT` 都不会发送给上游。
- [x] 本功能按 `model_code` 读取 DB 的 baseUrl/modelId/并发/超时/启停；配置变更只对下一批次生效，历史批次快照不变。
- [x] FAQ 转换、理论生成、技能生成的启用模型列表不出现本行；模型配置管理页可见、可启停、可维护 JEV 并发和读超时。
- [x] 本行 API Key 在列表/编辑页显示“配置文件提供”，输入框禁用；后端拒绝通过模型配置接口写入真实密钥。
- [x] 普通模型编辑时空 API Key 不覆盖旧密钥；系统托管的 JEV 行禁止删除，前端隐藏删除入口。
- [x] 旧批次缺少 JEV 快照时继续/重试返回“请新建批次”，不误报“AI 检查未开通”。
- [x] `V20261029_003` 已回填 theory-gen/skill-gen/knowledge-fix/courseware-gen/review-sync 存量种子范围。
- [x] §16.1.1 增量测试与相关回归全部通过。

---

## 20. 风险登记与变更控制

| 风险 | 控制 |
|---|---|
| 高置信但答错 | 双阈值 + 原答案支持度 + 200 道标定 + 人工复核；禁止自动写回 |
| XLSX 数值截断/科学显示污染 AI | 两条导入共用 ExcelCellTextReader + 只读预检 + 正式复检 + contentQuality 独立复核 |
| 正常科学记号/长小数被误改 | NFKC 仅用于检测、规则分级、suggestedText 只读、不自动替换/截断 |
| 已落库信息丢失无法还原 | 明示不可恢复边界；源文件明确映射对拍 + 人工修复，禁止猜测原值 |
| 多选组合判断不稳定 | 每 5 项分块、最弱项口径、组合卫兵、单题型独立质量门槛 |
| 模型路由/置信口径漂移 | requested/response model 与 confidenceDefinition 全审计；变化重新标定 |
| 429/5xx/超时 | 独立并发 5、限次退避、连续失败熔断、暂停与重试 |
| 多子问题 expanded token 超限 | 每块 5 项、16 项上限、不静默截断、预算 422 单列 |
| 批次并发/蓝绿接管竞态 | 活动槽唯一索引、run_id 租约/心跳、claimAttempt fencing、失败锁存、过期接管、拒绝补偿 |
| 历史结果被题库编辑污染 | 创建时完整快照 + type/content/answer sourceHash + 题库已变更提示 |
| 权限或租户泄露 | 后端权限码 + PLATFORM_ADMIN 硬约束、插件租户过滤、Worker 上下文、跨租户/非管理员 IT |
| 明文密钥或 Brotli 隐患复现 | 环境密钥、禁用内容压缩、请求头回归测试 |
| 共享模型配置行污染其他功能 | `feature_scope` + FAQ/理论/技能通用下拉过滤；管理页保留全量可见 |
| 占位值被误当真实密钥 | 固定文案 `模型API Key走配置文件` 永不发送；descriptor 仅认环境变量；配置接口拒绝改密钥 |

实施过程中若需要改变以下任一内容，必须先回到本文修订并交 owner 确认：菜单范围、写回答案、第二模型、阈值语义、多选算法、单批上限、质量门槛、外发字段、权限角色、内容质量分级、自动数值转换、导入返回契约、JEV 配置存放位置、api_key 占位文案、模型配置适用范围。除此之外的实现命名微调也应保持 `objectiveAnswerCheck` 统一语言，不得再出现 `subjectiveAnswerCheck` 或另一套同义模块。
