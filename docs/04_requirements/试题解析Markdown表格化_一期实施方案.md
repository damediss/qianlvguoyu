# 试题解析 Markdown 表格化 — 一期实施方案

> 版本：v2.2（2026-10-26，M2 修订：质量门接入、输入/prompt 收口、字典种子迁移口径更正）
> 状态：**M1/M2 已实施（后端单选最小闭环 + 质量门与防重护栏），第三批待做 App 单页端到端**
> 涉及仓库：`qianlvguoyu_saas`（后端 + admin-web）、`gjjd_appnext`（学员 App）
> 关联文档：《AI同步学方法论优化实施方案》W8.1/W8.2/O-7（已同步修订）
> 设计口径来源：Karpathy「输出格式阶梯」——LLM 不画图，只产出受控数据；图形表达交给确定性渲染器。一期只放行 **Markdown 表格** 这一种图形化语法。
> 实施提示：本文按“另一个模型可直接照做”的粒度编写。文件路径均以各仓库根目录为基准；行号是 2026-10-26 的定位参考，若代码漂移，以方法名/字段名为准。

---

## 0. 修订记录与拍板口径

### 0.0 v2.2 修订（2026-10-26，M2 批次）

- 质量门 `ResolveMarkdownSanitizer`（S1~S14）已接入 `generateAndPersist`：LLM 输出先清洗校验，拒绝写 rejected draft + 冷却（quality_rejected）；表格超限只整表丢弃。
- 输入清洗收口：选项块与主观 JSON 拼接答案统一再剥 HTML；`optionsText` 为空判 invalid_input；可解析出合法题干/选项/答案的未知题型走通用四行分支。
- prompt 单选/多选分支删除“（X 替换为真实答案）”括号说明，直接以实际答案开头。
- **迁移口径更正**：无表结构变更，仅新增 sys_dict 种子迁移 `V20261026_011`（generation/writeback 开关默认 0、每日上限 20，tenant 10159，已应用测试库）。按 Flyway 铁律保留不改动；生产未应用前不得手工补写开关为 1。
- 失败路径收口：模型配置缺失统一走 rejected draft + 冷却；`backfillResolveColumn` 改条件更新（列为空才写），AI 不覆盖生成期间的人工解析。

### 0.1 v2.1 补充拍板：解析资产“使用即建设”

**需求方 2026-10-26 补充确认：**

⑤ **解析资产采用“使用即建设”模式**：不先批量建设再开放。统一生成链路与小规模 shadow 质量验收通过后，立即向学员开放“AI讲解/生成解析”；学员点击时已有解析直接展示，没有则生成一次并回写 `resolve`，后续学员直接读缓存。
⑥ **W8.1 默认门禁放宽为 `wrong`**（首次答错即可讲），`star` 仅作为成本收紧档；让更多真实作答题能参与资产建设。

因此，本方案中的顺序统一解释为：

```text
统一能力 + 30~50 题内部 shadow 质量验收
  → 学员立即可用（首次点击生成，之后全员读缓存）
  → 高价值作答题的解析覆盖随真实使用增长
  → 按“作答题覆盖率/生成成功率”评估 W8.2
```

**shadow 验收只是 prompt/质量门的小样准入，不是全库资产建设前置，也不让学员参与 prompt 试验。**全量批量预生成只允许作为后续高频题补充，不是学员功能开放条件。

覆盖率统计改为双口径：全库覆盖率仅作背景；准入判断使用近 30 天学员作答题/同步学计划题的覆盖率、生成成功率与缓存命中情况，详见 §10.4。

### 0.2 v2.0 修订说明与四项拍板

v1.0 的表格清洗、App 渲染和降级思路保留；本次重点补齐 **解析质量、缓存复用、生成触发、上线准入** 四条链路，并修正若干现状误判。

**需求方 2026-10-26 拍板：**

1. **W8.1 与标准解析共用同一份内容**：同步学「AI 讲解」不再单独生成三段式文本；统一调用本方案的 `ensureResolve`。有 `resolve` 直接展示，没有才按本方案生成一次。
2. **主链路只读，按需生成**：作答/交卷接口不嵌 LLM。只有学员进入统一获取解析入口（按钮/后续 W8.1 入口）时才触发首次生成；首学员只在该次等待，之后全员读缓存。
3. **仅 AI 解析随题目变更失效**：已有 `resolve` 一律先直接用；题干/选项/正确答案变化时，仅当 `resolve` 与最近一次 AI 成功 draft 完全一致，才判旧并重新生成。人工解析、历史解析不被自动覆盖。
4. **严格校验 + 先影子验收**：结构、答案一致性、表格数字白名单等硬校验之外，先对 30~50 道真实题生成 shadow draft；人工验收通过后才允许在生产回写 `resolve`。

另有两条不可突破的工程约束：

- `exam_exam_detail.resolve` 列类型改 **text 由用户自行处理**；本方案**无表结构变更，仅新增 sys_dict 种子迁移 V20261026_011**；生产列未改前不得开放写回。
- 任何场景都不得新建第二套“查解析/生成解析”实现。统一入口是 `QuestionResolveAssistService`，统一官方解析读取器见 §5.1。

---

## 1. 背景与目标

`exam_exam_detail.resolve` 是题库级解析资产。现有 `QuestionResolveAssistService` 已具备“查库 → draft → LLM → 回写”的雏形，但仍有以下缺口：

1. prompt 只讲“为什么对”，不讲“为什么错”，且题型原码、HTML、答案序列化未受控；
2. 没有确定性质量门，LLM 输出只要非空就可能成为长期缓存；
3. 官方解析读取分散：`resolve` 列、answer JSON、同步学各自为政；
4. 生成失败后立即重试、公开接口可匿名触发、单 JVM 锁不能覆盖蓝绿双实例，均可能浪费 token；
5. 表格类数值题纯文字难讲清，需要受控 Markdown 表格 + 确定性渲染。

**一期目标：**

- 所有学员、所有页面按同一顺序获得解析：**`resolve` 列 → answer JSON 官方解析 → 统一生成一次 → 全员读缓存**；
- prompt v2（受控语言 + 四行骨架 + 题型分支 + 条件表格）；
- 生成前后均有质量门，shadow 验收通过才开放生产写回；
- App 两端统一渲染与统一按需获取，无解析时不白屏、不重复点击烧 token。

**非目标：** chart 图表块、Mermaid、SVG、服务端出图、把全量批量预生成作为上线前置、W8.1 接口本身的提前实施、W8.2 漏洞提示提前上线。

---

## 2. 范围与非目标

### 2.1 范围

| 仓库 | 改动 |
|---|---|
| saas 后端 | 统一官方解析读取器；`QuestionResolveAssistService` 缓存/锁/冷却/限流/质量门/prompt v2；`AppExamSmartService` 与同步学读取链接入；FAQ 表格扁平化；手工编辑长度校验 |
| appnext App | `resolveTable.js` 纯函数渲染；统一 `ensureQuestionResolve` 请求封装与按需加载器；8 个解析展示点 + 班级刷题现有自动生成点改造 |
| admin-web | 两个编辑框 `maxLength={2000}`，后端同步校验 |
| 同步学方案文档 | W8.1 改为复用标准解析；O-7 写入实测结论；测试/验收条目同步 |

### 2.2 非目标（已核实，勿顺手做）

| 项 | 结论 | 依据 |
|---|---|---|
| 数据库迁移 | 无表结构变更，仅新增 sys_dict 种子迁移 V20261026_011（generation/writeback 开关默认 0、每日上限 20，tenant 10159）；列类型用户自行改 text；不新增解析版本列、不新增缓存表 | 用户拍板；版本信息写 `ai_generation_draft.prompt` |
| 竞赛链路 | 零改动。竞赛组卷时 `resolveContent` 置 null，监考/预览页 Alert 明示不显示答案解析 | `CompetitionTheoryPaperApplicationService:409` 等 |
| admin-web 解析渲染 | 零改动。管理端只有编辑场景，无学员只读展示 | `QuestionBankPage.tsx`、`CompanyQuestionPage.tsx` |
| randExam / sortExam | 零改动。两页无 `resolve` 展示点 | appnext grep 实测 |
| 小培讲题 | 不新增展示。只把其解析读取/生成统一到新链路 | `ClassAssistantApplicationService` 已调用 `peekResolve/ensureResolve` |
| W8.1 `/appreviewsync/explain` 实施 | 仍属同步学二期。本期只定复用契约，不提前做该端点 | 与方法论方案排期一致 |
| 批量预生成任务 | 只做服务可复用设计，不建定时任务、不跑全量；后续若做只补高频题，不是上线前置 | “使用即建设”之外的补充工作包 |

---

## 3. 现状事实底座（实施模型必读）

### 3.1 数据库与覆盖率

| 库 | `resolve` 实际类型 | 备注 |
|---|---|---|
| 测试库 gjjddb2026（114.132.67.185:3309） | 已是 text | 2026-10-26 实测 |
| 生产库 gjjddbpro（43.138.163.7:3308） | varchar(600) | **开放写回前必须由用户改为 text** |

2026-10-26 生产只读实测（`deleted=0 AND final_check_status=1`）：

- 有效题 138,981 行；`resolve` 非空 353 行；answer JSON 中 `resolve/analysis/jiexi/explain/解析` 命中 0 行；两者皆空 138,628 行。
- 该统计与 v1.0 的“185,886/393”口径不同。实施复核时必须在文档中写明过滤条件，不得只引用行数。

```sql
SELECT COUNT(*) total,
 SUM(resolve IS NOT NULL AND TRIM(resolve)<>'') resolve_col,
 SUM(answer REGEXP '"(resolve|analysis|jiexi|explain|解析)"[[:space:]]*:') answer_json
FROM exam_exam_detail
WHERE deleted=0 AND final_check_status=1;
```

结论：全库官方解析覆盖率仅作背景，**不作为学员功能开放前置**。本方案采用“使用即建设”：先开放统一按需生成，学员点击即生成并共享；覆盖率重点看真实作答题，而非要求全库先达到某一比例。

### 3.2 当前生成与读取链（v1.0 误判修正）

- **已有生成入口只有班级刷题**：`api/class-practice.js:50` 调 `/appsmart/question/resolve`；`pages/class/classPracticeExam.vue:337` 在揭晓后异步触发 `ensureAiResolve`。其余展示点目前只读，不触发生成。
- 同步学答错响应直接返回 `detail.resolve()`，不调用 LLM：`ReviewPlanApplicationService:410`。
- 精准学题目视图有 answer JSON 回退：`AppExamSmartService.toQuestionView:1806`；同步学 `ReviewQuestionQueryService.toDetail` 没有该回退。
- `QuestionResolveAssistService.ensureResolve` 已有 draft 与题级 JVM 锁，但：
  - 不看 answer JSON 官方解析；
  - 整个方法标 `@Transactional`，LLM HTTP 在事务内；
  - 生成失败无冷却，下次请求立即重试；
  - 锁仅当前 JVM 有效；
  - `/appsmart/question/resolve` 位于 `/appsmart/** permitAll` 下，当前控制器不校验学员身份。
- 已存在 `exam/reviewsync/support/QuestionTypes.java`，其注释明确为“题型口径唯一来源”。**不得另建同名同职责类。**

### 3.3 App 展示点（原 5 文件 8 处，另有 1 个生成触发文件）

| 文件 | 展示字段/位置 | 一期处理 |
|---|---|---|
| `pages/exam/syncExam.vue` | `q.selfData.resolve`、`q.result.resolve`（共 4 处） | 表格条件渲染 + 无解析时按需获取 |
| `pages/exam/syncMock.vue:60` | `w.resolve` | 同上 |
| `pages/exam/practiceWrongBook.vue:26` | `q.resolve \|\| q.analysis`，现套 `plainStem` | 有表格时跳过 `plainStem`，无表格保持原行为 |
| `pages/class/classPracticeDone.vue:31` | `q.analysis` | 同上 |
| `pages/class/homeworkResult.vue:60` | `row.analysis` | 同上 |
| `pages/class/classPracticeExam.vue:337` | 现有 `ensureAiResolve` 自动生成点 | 改为统一按需入口；不再揭晓后逐题自动生成 |

App 为 uni-app Vue 3，题干已有 `<rich-text :nodes>` 渲染路径；不引入 mp-html/towxml。App 已有 Vitest 基建，构建命令是 **`npm run build:h5`**，不是 `pnpm build`。

---

## 4. 总体设计：一份解析资产，一条获取链

### 4.1 统一读取顺序

```text
OfficialResolveResolver.resolve(detail)
  1. resolve 列非空，且不是“已失效的 AI 解析”
       → 人工/历史文本返回 source=db；与有效 generated draft 一致的 AI 文本返回 source=draft
  2. answer JSON 根字段 resolve/analysis/jiexi/explain/解析 → 返回 source=official
  3. 否则 → source=empty
```

以下调用方必须使用同一个读取器，不得再各写一份：

- `QuestionResolveAssistService.peekResolve/ensureResolve`；
- `AppExamSmartService.toQuestionView`；
- `ReviewQuestionQueryService.toDetail`（由 private static 改为实例方法并注入读取器）；
- `ExamFaqConverter` 取解析后先经该读取器，再做 §5.12 扁平化。

answer JSON 官方解析只读使用，**不回写 `resolve`**；只有 AI 验收通过的文本才回写。这样不改变题库原始数据，又能保证所有读取方顺序一致。

### 4.2 统一生成顺序

`ensureResolve` 只有在上述读取为空时才继续：

```text
校验生成开关/输入资格
  → 失败冷却检查（同 sourceHash 24h 内不重复）
  → Redis 题级锁（跨实例防重）
  → 锁内再次读 resolve/official/generated draft
  → 每学员每日限流（仅真实 LLM 前计次）
  → prompt v2 调 LLM
  → 清洗 + 质量门
  → 合格：写 draft；按 writeback 开关决定 shadow 或回写 resolve
  → 不合格/异常：不写 resolve，记失败原因并进入冷却
```

同一题同一 sourceHash，成功生成至多一次；失败在冷却期内不重复调用。后续学员、后续页面、W8.1、未来批量预生成都复用同一结果。

### 4.3 与 W8.1 的关系（已拍板）

- W8.1 只保留自己的业务门禁：登录、plan/question 归属、`ai_review_explain_gate=wrong|star`、每日上限。**默认 `wrong`（首次答错即可讲），`star` 为成本收紧档**。
- 门禁通过后直接调用 `ensureResolve`，返回同一份标准解析；**取消独立三段式 prompt、独立 `ai_review_explain` draft 和独立 Caffeine 缓存**。
- App 结果区已有解析就直接展示；没有时才显示「让 AI 讲讲这题」。点击后展示同一 `resolve`，底部在 `source=ai/draft` 时固定标注“AI 生成，以官方教材为准”。
- W8.2 后续若需补充高频题预生成，也只能调用 `ensureResolve`，不得写独立脚本直连 LLM 后自行回写；预生成不作为学员功能开放前置。

---

## 5. 后端详细设计

### 5.1 官方解析读取器 `OfficialResolveResolver`（新建）

建议位置：`com.guoyu.platform.exam.service.OfficialResolveResolver`，`@Component`。

```java
public record OfficialResolve(String text, String source) {
  // source ∈ db | official | draft | empty
  // db=人工/历史列文本；draft=与有效 generated draft 一致的 AI 缓存文本
}

public OfficialResolve resolve(ExamExamDetailEntity detail);
```

规则：

1. `detail == null` 或 `deleted != 0` → empty。
2. `resolve` 去首尾空白后非空：
   - 调 §5.5 的 `isStaleAiResolve(detail, columnText)`；是失效 AI 解析 → 不返回该文本，继续查 answer JSON。
   - 未失效时，若该列与最近一条有效 `generated` draft 的 resultText 完全一致且 sourceHash 相同 → 返回 `draft`（用于 AI 来源标注）；否则返回 `db`（人工/历史）。两个判断应共用一次 draft 查询与一次 sourceHash 计算，不得在调用链中重复查库。
3. answer JSON 仅当根节点为对象时读取；按 `resolve → analysis → jiexi → explain → 解析` 顺序取第一个非空字符串，返回 `official`。解析异常按无官方解析处理并记 debug，不抛错。
4. 都没有 → `empty`。
5. 返回文本统一 `trim()`；不再按 250 字截断。

依赖：`AiGenerationDraftMapper`（失效判断）+ §5.5 的输入/sourceHash 构建器。JSON 解析使用 Jackson `ObjectMapper`（与 `AppExamSmartService` 当前一致）。

`AppExamSmartService` 删除私有 `extractResolveFromAnswerJson` 或改为委托；`ReviewQuestionQueryService.toDetail` 注入该读取器。禁止保留第三份 JSON key 列表。

### 5.2 题型归一：扩展现有 `QuestionTypes`

文件：`exam/reviewsync/support/QuestionTypes.java`。保留现有 `OBJECTIVE_TYPES/SUBJECTIVE_TYPES/SCOPE_TYPES/isSubjective`，新增：

```java
public static String normalize(String raw);  // null/blank → null；未知 → trim 后小写原样
public static String toLabel(String raw);    // 中文题型；未知/空 → 未标注题型
```

映射（大小写、首尾空白均容错）：

| 归一码 | 历史形态 | 中文标签 |
|---|---|---|
| single | single/danxuan/单选/单选题/1 | 单选题 |
| multi | multi/duoxuan/多选/多选题/2 | 多选题 |
| judge | judge/pandan/panduan/判断/判断题/3 | 判断题 |
| short | short/jianda/简答/简答题/4 | 简答题 |
| answer | answer/问答/问答题 | 问答题 |
| cal | cal/计算/计算题 | 计算题 |
| essay | essay/论述/论述题 | 论述题 |
| analysis | analysis/分析/分析题 | 分析题 |
| explain | explain/解释/解释题 | 解释题 |
| fill | fill/填空/填空题 | 填空题 |
| drafting | drafting/绘图/绘图题 | 绘图题 |

`AppExamSmartService.normalizeQuestionType` 改为委托 `QuestionTypes.normalize`，并保持 `null` 返回语义与客观三型结果不变。`QuestionResolveAssistService` 使用 `normalize/toLabel`。

prompt 侧专属规则放 `QuestionResolvePromptPolicy`（见 §5.6），不要把字数预算塞进通用 `QuestionTypes`。

### 5.3 `QuestionResolveAssistService` 对外契约

保留并扩展，不新建第二套服务：

```java
Map<String, Object> peekResolve(Integer questionId);       // 只读，不生成
Map<String, Object> ensureResolve(Integer questionId);     // 内部受信调用（小培/未来批量/W8.1）
Map<String, Object> ensureResolveForApp(Integer questionId); // App 控制器：强制登录 + 每学员限流
```

返回字段（保持现有兼容，新增字段只允许增加）：

| 字段 | 含义 |
|---|---|
| questionId | 题目 id |
| resolve / analysis | 同一解析文本；为空时均为 null |
| source | `db`：列直读；`official`：answer JSON；`draft`：历史 AI draft；`ai`：本次生成并正式写回；`shadow`：本次生成但未写回；`empty`：无可用解析 |
| generated | 仅本次实际调用 LLM 且质量门通过时为 true |
| reason | 仅 empty 时可有：`disabled / invalid_input / cooldown / generating / rate_limited / llm_failed / quality_rejected / official_only / shadow` |
| stem | 剥 HTML 后的题干（沿用现状） |

`ensureResolveForApp` 通过 `SecurityContext.currentUser()` 强制登录；无有效 userId 时抛 `AppSmsException(-1, "请先登录")`，**匿名不再能读缓存或触发生成**。内部 `ensureResolve` 不强制 App 登录，供后端受信链路使用。

`peekResolve` 的只读顺序固定为：`OfficialResolveResolver` → 与当前 sourceHash 一致的 `generated` draft → empty；绝不读 shadow/rejected，绝不调用 LLM。`ensureResolveForApp` 与内部 `ensureResolve` 共用同一缓存/生成实现，仅鉴权和每学员限流不同。

### 5.4 `ensureResolve` 逐步流程（实现顺序不可颠倒）

1. 校验 questionId、题目存在且未删除；读取当前有效租户（沿用现有 `TenantContextHolder`，默认 10159）。
2. `OfficialResolveResolver.resolve` 非 empty → 直接返回其 source（db/official/draft），不调 LLM、不计限流。
3. 计算 §5.5 的 `QuestionPromptInput` 与 sourceHash。输入资格不合格 → 返回 `empty/invalid_input`，不调 LLM。
4. 查最近一次 `draft_type='question_resolve'`、`source_type='exam_exam_detail'`、`source_id=questionId`、`status='generated'` 的 draft：
   - resultText 非空且 sourceHash 与当前一致 → 若 writeback=1 且 `resolve` 仍空则补回写；返回 `draft`。
   - sourceHash 不一致 → 视为旧 draft，不使用。
   - writeback=0 时，再查同 sourceHash 的最近 `shadow` draft，有则直接返回 `shadow`，避免影子样本重复生成；writeback=1 时 shadow 不得当作正式缓存。
5. 若 `question_resolve_generation_enabled != 1` → 返回 `empty/disabled`。缓存读取不受该开关影响。
6. 失败冷却：Redis `GET question_resolve:fail:{questionId}`，值等于当前 sourceHash 且未过期 → 返回 `empty/cooldown`。sourceHash 变了说明题目已改，可重新尝试。
7. 获取 Redis 题级锁：`SET question_resolve:lock:{questionId} {uuid} NX PX 120000`。
   - 未获得 → 返回当前官方/生成 draft（若有），否则 `empty/generating`；**不得直接再调 LLM**。
   - 获得后进入下一步；finally 用“值比对后删除”的 Lua 释放锁。
8. 锁内再次执行第 2~4 步，防止等待期间另一实例已生成。
9. App 调用且将真实调用 LLM 时，执行每学员每日限流；缓存命中、锁竞争、冷却命中不计次。
10. 调 LLM（§5.6~§5.8）。异常、空响应、`finish_reason=length` 或清洗质量门拒绝：
    - 不写 `resolve`；
    - 写一条失败 draft（`status='rejected'`，resultText 可为原始输出，便于审计；resultText 不进入任何读取链）；
    - `SET question_resolve:fail:{questionId} {sourceHash} EX 86400`；
    - 返回对应 empty/reason。
11. 合格文本：
    - `question_resolve_writeback_enabled=1`：先回写 `resolve`，再写 `status='generated'` draft，返回 `ai`；
    - 否则：只写 `status='shadow'` draft，不回写，返回 `shadow`。
12. 整个公开方法 **不使用 `@Transactional`**。draft 与 resolve 两次写入均幂等可恢复；任一步失败只记 warn，不让学员主链路炸掉。

`question_resolve_generation_enabled` 默认 0，`question_resolve_writeback_enabled` 默认 0。生产在 shadow 验收完成前不得把 generation 置 1。

### 5.5 输入组装、sourceHash 与失效判定

新建包级辅助 `QuestionResolveInputFactory`（可与服务同包），产出：

```java
record QuestionPromptInput(
    String normalizedType, String typeLabel, String stem, String optionsText,
    String correctAnswerText, String knowledgeTitle, String sourceHash) {}
```

**输入清洗：**

- 题干、选项、答案全部先执行现有 `stripHtml` 等价规则：`<br>`/`</p>` 转换行、剥标签、`&nbsp;` 等实体解码、压缩空白。
- 客观题优先复用 `FollowTaskBankQuestionConverter.convert(detail, 0)` 获取选项与正确选项键；缺选项、缺答案、答案不属于选项 → `invalid_input`。
- 单选答案格式 `A`；多选按字母升序 `A、C`；判断把 `true/false/正确/错误/对/错` 及正确选项文本归一为 `对/错`，无法归一则 invalid。
- 主观题（`QuestionTypes.isSubjective`）不走客观转换器：题干用剥 HTML 后的 `detail.content`，答案用剥 HTML 后的 `detail.answer`。answer 若为 JSON 数组，支持字符串元素，或对象元素的 `content/text/answer/value` 字段，按原顺序以 `；` 连接；无法解析才用原始文本。答案为空 → invalid。
- 知识点名：`ExamKnowledgeStructureMapper.selectById(detail.knowledgeId)`，取 `title`；查不到则为空字符串，不阻断。
- 截断：题干 800 字、选项块 600 字、客观答案 200 字、主观答案 1200 字。**先剥 HTML 再截断**。

**sourceHash（无新表、无新列）：**

```text
canonical = normalizedType + \u0001 + stem + \u0001 + optionsText
          + \u0001 + correctAnswerText + \u0001 + knowledgeTitle
sourceHash = SHA-256(canonical 的 UTF-8 字节) 的 64 位小写 hex
```

成功 draft 的 `prompt` 写 `question_resolve_v2#` + sourceHash。失败 draft 写 `question_resolve_v2_rejected#` + sourceHash。

**失效判定 `isStaleAiResolve(detail, columnText)`：**

1. 找最近一条 `status='generated'` 的同类 draft；没有 → false（人工/历史解析永久可信）。
2. `columnText` 与 `draft.resultText.trim()` 不完全一致 → false（人工已改写过，不覆盖）。
3. 当前 sourceHash 与 draft.prompt 中 hash 不一致，或当前题面已无法构建合法输入 → true（题面/答案/知识点已变）。
4. 否则 false。

这样无需依赖 `updated_at`，也不会把“回写 resolve 自身造成的 updated_at 变化”误判为题目编辑。

### 5.6 Prompt v2（`QuestionResolvePromptPolicy`）

**System prompt 固定全文：**

```text
你是职业技能等级认定考试的辅导老师，考生是一线在职工人。写解析必须遵守：
1. 短句：一句只说一件事，单句不超过 25 个字。
2. 主动语态：写“断电后才能更换熔断器”，不写“熔断器的更换应当在断电后进行”。
3. 同一事物用同一个词：题干里叫“电解槽”，解析里就叫“电解槽”，禁止换成同义词。
4. 只依据题干、选项、正确答案和所给知识点名称作答。可以引用岗位操作常识，
   但禁止编造标准编号、文件名称、具体数据，禁止输出学员作答或“你选了 X”。
5. 正文不超过 {预算} 字。除第 8 条允许的表格外，不得使用任何 markdown、emoji、
   HTML、链接或代码块。
6. 不出现“保过/必过/稳过”，不出现模型名、参数名、表名、内部文档名。
7. 严格按下列结构输出，每行以【】小节名开头，不输出小节名以外的内容：
【考点】本题考查的知识点，10 字以内。
【正确】先按用户给定格式写答案，再说明为什么。
【干扰】最容易错选的选项及其错误原因。
【记法】一条现场记忆口诀或动作要点，不超过 30 字。
8. 表格规则（唯一允许的 markdown 语法）：仅当题干包含 2 个及以上可对比的
   数值、配比或档位，且表格确实更易读时，才在【正确】内容之后插入一张表格；
   其余情况禁止表格。表格必须为管道语法：表头行、分隔行（|---|---|）、
   数据行各占一行；列数不超过 3，数据行不超过 4，每格不超过 12 字；
   表格数值必须原样来自题干、选项或正确答案，禁止推算新数据。表格不占字数预算。
```

**User prompt：**

```text
题型：{typeLabel}
知识点：{knowledgeTitle，空则写“未标注”}
题干：
{stem}
{optionsText 非空时：选项：\nA. …\nB. …}
正确答案：{correctAnswerText}
{题型分支行}
请按四行结构写出解析：
```

题型分支与答案硬结构：

| 类型 | 预算 | 分支行与【正确】硬结构 |
|---|---|---|
| 单选 | 240 | `【正确】必须以“答案：A。”开头（A 替换为真实答案），再说明原因。` |
| 多选 | 300 | `【正确】必须以“答案：A、C。”开头（替换为真实组合），逐项说明入选原因；【干扰】点名最常漏选/多选项。` |
| 判断 | 240 | `【正确】必须以“答案：对。”或“答案：错。”开头；【干扰】说明错误说法错在哪、正确做法是什么。` |
| 主观 8 类 | 400 | `【正确】以“答案要点：”开头，按给分要点逐条说明；省略【干扰】行，只输出【考点】【正确】【记法】。` |
| 未知 | 240 | 按通用四行；【正确】以“答案：{原文}。”开头 |

**模型参数：**

- `thinking` 继续透传模型配置，保留 `QuestionResolveThinkingTest` 契约，禁止改回硬编码 null。
- 关闭思考时 `max_tokens=800`；配置开启思考时 `max_tokens=2400`（思维链计入输出预算）。若配置无法判断是否开启，按开启处理，取 2400。
- 空响应或 `finish_reason=length` 一律视为失败，不得把半截解析写回。
- 不为兼容而传目标平台不支持的参数；沿用 `FaqLlmClient` 既有入参。

### 5.7 清洗与质量门 `ResolveMarkdownSanitizer`

返回 `SanitizeResult(text, rejectedReason, tableDropped)`；拒绝时 text=null。

| 规则 | 动作 |
|---|---|
| S1 首尾代码围栏 | 剥除 |
| S2 表格识别 | ≥3 个连续行均以 `|` 开头，且第 2 行只含 `|`、`-`、`:`、空格，认定为表格块 |
| S3 表格超限 | 列 >3、数据行 >4、单元格 >12 字 → 整表丢弃，保留文本 |
| S4 表格残行 | 同一表格块内各行列数不一致 → 整表丢弃 |
| S5 孤立 `|` | 不构成表格块则保留正文 |
| S6 其他 markdown | 仅剥 `# `、`* `、`- ` 等带空格的标记；不得误伤负数/负号 |
| S7 总长度 | 纯文本部分 >800 字时按完整行截断；表格只能完整保留或整表丢弃，不得截半张表 |
| S8 必需小节 | 按题型检查集合与顺序：客观/未知=【考点】【正确】【干扰】【记法】；主观=【考点】【正确】【记法】。缺任一必需项或顺序错 → 拒绝 |
| S9 答案一致性 | 单选/多选【正确】必须以 `答案：{与题库完全一致的组合}。` 开头；判断必须 `答案：对/错。` 且与题库一致；主观必须 `答案要点：`；不一致 → 拒绝 |
| S10 表格数字白名单 | 从题干/选项/答案提取数字（含小数、百分数、区间）；表格中 ≥2 位的数字、带小数/百分号的数字必须能在该集合中找到，否则整表丢弃。单个 0~9 序号不纳入，避免误伤 |
| S11 禁止内容 | 含“保过/必过/稳过”、学员作答表述（“你选了/你答了”）、模型名/表名/参数名、HTML、http 链接 → 拒绝 |
| S12 内容下限 | 【正确】与【干扰】合并正文（主观只看【正确】）<30 字 → 拒绝；去除空白后与题干完全相同 → 拒绝 |
| S13 安全类 | 知识点名命中“安全规程/法律法规”等配置白名单时，不调用 LLM；直接返回官方/empty。工艺参数类允许生成，但强制执行 S10 |
| S14 思考链/异常形态 | 含 `<think>`、JSON 外壳、明显 prompt 复述 → 拒绝 |

S13 的白名单写为服务内常量或 sys_dict `question_resolve_official_only_keywords`（默认 `安全规程,法律法规`）；**不要再造第二份关键词库**，若 W8.1 后续需要同名单，直接复用本常量。

### 5.8 长度三道闸门

1. prompt 按题型 240/300/400 字；
2. 清洗器 S7 纯文本 800 字上限；
3. 手工编辑前端/后端均 ≤2000 字，后端 `CompanyQuestionApplicationService` save/update 及其他写 `resolve` 的管理端入口统一校验，超长报“解析不能超过 2000 字”。

删除 `RESOLVE_DB_MAX=250` 及全部引用、`peekResolve/ensureResolveLocked` 的“≥245 字查 draft 找全文”分支、`backfillResolveColumn` 的 250 截断。draft 不再是 varchar 截断时的全文备胎，只承担审计、失败恢复与失效判断。

### 5.9 鉴权、限流、锁、冷却

- **鉴权**：`AppExamSmartController.ensureQuestionResolve` 改调 `ensureResolveForApp`；匿名一律“请先登录”。内部服务调用不需要 App 身份，但仍走租户过滤。
- **限流**：新建 `QuestionResolveRateLimitService`，仿 `AiGradeRateLimitService` 的 Lua INCR+惰性 EXPIRE；key=`resolve:rate:day:{tenantId}:{userId}:{yyyy-MM-dd}`，sys_dict `question_resolve_daily_limit` 默认 20。只在真实 LLM 前计一次；Redis 异常时返回 `empty/rate_limited` 并记 warn，不直接放行无限生成。
- **跨实例锁**：Redis `SET NX PX`，锁值 UUID，120 秒；释放锁必须比对值。拿不到锁返回 `generating`，不重复调用。
- **失败冷却**：`question_resolve:fail:{questionId}` 存 sourceHash，TTL 24h。题目变更后 hash 不同可立即重试；LLM 故障与质量拒绝共用冷却，避免坏题反复烧 token。

### 5.10 draft 留痕与 shadow

`ai_generation_draft` 字段沿用，不迁移：

| 字段 | 写法 |
|---|---|
| draftType | `question_resolve` |
| sourceType / sourceId | `exam_exam_detail` / questionId |
| prompt | 成功/影子：`question_resolve_v2#{sourceHash}`；失败：`question_resolve_v2_rejected#{sourceHash}` |
| resultText | 合格文本或失败原始输出；失败文本永不进入读取链 |
| modelName | 实际模型配置码 |
| status | `generated`（正式）/ `shadow`（影子）/ `rejected`（失败） |
| tenantId / created_by / created_at | 按现有上下文与 MetaObjectHandler；触发来源在日志记录 `app / class-assistant / reviewsync / batch` |

读取缓存只认 `generated`；`shadow/rejected` 不得回写或展示给普通学员。重复 shadow 验收时，同 sourceHash 的 shadow draft 可直接返回，避免重复生成样本。

### 5.11 手工编辑与缓存失效

- 人工编辑框与后端校验上限 2000 字；前端属性是 **`maxLength={2000}`**。
- 人工修改过的 `resolve` 与最近 AI draft 不一致，失效判定返回 false，永不自动重生成。
- 题目内容/答案变更时无需逐个编辑入口加钩子：读取时通过 sourceHash 判旧，覆盖所有编辑路径。
- 旧 353 条及历史 resolve 全部直接沿用，不因 prompt v2 自动重写。

### 5.12 FAQ 链路扁平化

`ExamFaqConverter` 先经 `OfficialResolveResolver`，再调 `ResolvePlainText.flatten`：

- 无表格原样；
- 表格数据行转「表头1值1、表头2值2；表头1值3、表头2值4」；
- 转换异常降级为剥掉 `|` 与分隔行，保证 WeKnora 纯文本中无裸管道符。

---

## 6. App 详细设计

### 6.1 表格渲染纯函数 `utils/resolveTable.js`

```js
export function hasTable(text)
// ≥3 个连续以 | 开头的行，且第 2 行为分隔行

export function resolveToHtml(text)
// 表格块 → <table><tr><td style="...">；其余文本 → escape 后 <br/>
// 所有文本/单元格统一转义 & < > " '
// 无表格或解析异常 → null（调用方落回原文本）
```

表格样式全部内联：1px 边框、4px 内边距、单元格左对齐；不依赖页面样式穿透。`resolveToHtml` 必须对任何输入都不抛出异常。

`resolveTable.test.js` 用 Vitest 覆盖：正常表格、残行、无表格、孤立管道、XSS 字符串、null 返回。

### 6.2 统一请求与按需加载

- 将 `ensureQuestionResolve` 的唯一定义移到新文件 `api/question-resolve.js`；`api/class-practice.js` 仅 re-export，避免破坏现有 import。请求仍为 `saasPost('/appsmart/question/resolve', { questionId })`。
- 新建 `utils/resolveLoader.js`：
  - `pending: Map<questionId, Promise>`：同题并发点击共享同一请求；
  - `emptyUntil: Map<questionId, timestamp>`：返回 empty 后 60 秒内不重复请求，后端冷却仍是最终约束；
  - `loadQuestionResolve(questionId)`：返回后端 data；不自行写题库、不直接调 LLM。
- 页面加载状态：按钮点击后禁用并显示“解析生成中…”；成功把 `resolve` 与 `analysis` 同时写入当前题对象；empty 按 reason 显示：
  - `generating`：正在生成，请稍后刷新；
  - `rate_limited/cooldown`：今天暂时不能生成，请稍后再试；
  - 其他：暂时生成不了，请稍后再试。
- `classPracticeExam.vue` 现有 `ensureAiResolve` 改为复用 `loadQuestionResolve`，并取消揭晓后自动逐题调用；改为学员点击「生成解析」才调用。`resolveTried/resolveLoading` 旧实现删除。

### 6.3 展示点统一模式

每个展示点都遵循：

```html
<view v-if="解析文本">
  解析：<rich-text v-if="hasTable(解析文本)" :nodes="resolveToHtml(解析文本)"></rich-text>
  <text v-else>{{ 解析文本 }}</text>
</view>
<view v-else-if="已展示答案/结果">
  <text class="resolve-generate" @click="按需加载">生成解析</text>
</view>
```

- `syncExam.vue`：客观题只在 `q.result` 已出现时允许生成；自评区只在 `q.needSelf` 时出现；生成结果同时回填 `q.result.resolve` 或 `q.selfData.resolve`。
- `syncMock.vue`：回填 `w.resolve`。
- `practiceWrongBook.vue`：保留 `q.resolve || q.analysis` 回退；仅当有表格时跳过 `plainStem`，无表格继续原 `plainStem` 行为。
- `classPracticeDone.vue` / `homeworkResult.vue`：分别回填 `q.analysis`、`row.analysis`。
- 任何 `resolveToHtml=null`、接口 false、超时或异常都落回原文/“暂时生成不了”，不白屏、不阻断答题。

为 8 个展示点写源码契约测试：含 `hasTable` 条件渲染、空解析按需入口、原字段回退未丢；`classPracticeExam` 不得再有揭晓后自动 `ensureAiResolve(q)` 调用。

---

## 7. admin-web 改动

| 文件 | 改动 |
|---|---|
| `apps/admin-web/src/pages/exam/QuestionBankPage.tsx` | 解析 TextArea 加 `maxLength={2000}` |
| `apps/admin-web/src/pages/exam/CompanyQuestionPage.tsx` | 同上 |

不新增渲染器、不新增组件测试；`npm run build` 必须绿。

---

## 8. 文件级改动清单

### saas 后端

| 文件 | 动作 |
|---|---|
| `app/exam/service/QuestionResolveAssistService.java` | 按 §5.3~§5.10 重构；删 250 截断与旧 draft 回退；去事务 |
| `exam/service/OfficialResolveResolver.java` | 新建，统一 db/official/empty 与失效判断 |
| `exam/service/QuestionResolveInputFactory.java` | 新建，输入清洗、答案格式化、sourceHash |
| `exam/service/QuestionResolvePromptPolicy.java` | 新建，prompt、题型分支、预算 |
| `exam/service/ResolveMarkdownSanitizer.java` | 新建，S1~S14 |
| `exam/service/ResolvePlainText.java` | 新建，FAQ 扁平化 |
| `exam/service/QuestionResolveRateLimitService.java` | 新建，每学员每日限流 |
| `exam/reviewsync/support/QuestionTypes.java` | 扩展 normalize/toLabel，不新建第二套 |
| `app/exam/service/AppExamSmartService.java` | normalize 委托；toQuestionView 用统一读取器；删私有 JSON 回退 |
| `exam/reviewsync/service/ReviewQuestionQueryService.java` | toDetail 改实例方法并注入统一读取器 |
| `app/exam/controller/AppExamSmartController.java` | `/question/resolve` 改调 ensureResolveForApp |
| `exam/service/ExamFaqConverter.java` | 统一读取 + flatten |
| `exam/service/CompanyQuestionApplicationService.java` | resolve ≤2000 校验 |

### appnext

| 文件 | 动作 |
|---|---|
| `api/question-resolve.js` | 新建唯一 `ensureQuestionResolve` 定义 |
| `api/class-practice.js` | 改为 re-export，保持兼容 |
| `utils/resolveTable.js` + `utils/resolveLoader.js` | 新建渲染与按需加载 |
| 上述 6 个页面文件 | 按 §6.3 改造；班级刷题取消自动生成 |

---

## 9. 自动化测试清单

### 后端（JUnit 5 + Mockito）

| 测试类 | 必测点 |
|---|---|
| `OfficialResolveResolverTest` | 列优先；answer JSON 五 key 顺序；无解析；失效 AI 列不返回；人工改写不判旧；hash 不变不判旧 |
| `QuestionTypesTest` | §5.2 全映射；pandan/panduan 均通过；blank/null；现有客观三型与 isSubjective 不回归 |
| `QuestionResolveInputFactoryTest` | HTML 剥离；单/多/判答案格式；主观 JSON 数组与对象数组；缺答案/缺选项 invalid；sourceHash 稳定且随题干/答案变化 |
| `QuestionResolvePromptTest` | 中文题型与预算；知识点名；答案硬结构；判断/主观分支；无学员作答；thinking 关闭=800、开启=2400；原 thinking 契约保持绿 |
| `ResolveMarkdownSanitizerTest` | S1~S14 全规则；重点：单选答案错配拒绝、多选组合顺序归一后校验、判断对错错配拒绝、简答无【干扰】可通过、表格数字编造整表丢弃、负数行不被误剥 |
| `QuestionResolveAssistServiceTest` | db/official/draft 命中零 LLM；首次生成一次；第二次零 LLM；质量拒绝不写 resolve；rejected 不进入读取链；shadow 不回写；writeback 后才 generated；失败冷却；锁内二次缓存；失效 AI 重生成；人工 resolve 不重生成 |
| `QuestionResolveRateLimitServiceTest` | 缓存命中不计次；真实 LLM 前计一次；第 21 次拒绝；Redis 异常不无限放行 |
| `ResolvePlainTextTest` | 无表格原样；表格转顿号/分号短句且无管道符；异常降级剥管道 |
| `ExamFaqConverterTest` | 含表格无裸管道；无表格行为不变 |
| `CompanyQuestionResolveLimitTest` | 2000 通过、2001 业务异常 |

MockRedis 可用 Mockito 仿现有 `AiGradeRateLimitService` 测试风格；锁与冷却不必连接真实 Redis。

### appnext（Vitest）

- `resolveTable.test.js`：渲染、转义、异常降级；
- `resolveLoader.test.js`：同题并发只发一次、empty 60 秒防重、成功写回字段；
- 8 展示点源码契约：条件渲染与按需入口存在；`plainStem` 仅无表格走；`classPracticeExam` 无自动逐题生成调用。

### 构建门禁

- `mvn compile -DskipTests` 0 错；`mvn test` 目标测试与全量回归无新增失败；
- admin-web `npm run build` 0 错；
- appnext `npm test` 全绿、`npm run build:h5` 0 错；
- 无 BOM；本机绿盾环境按 `AGENT-BUILD-GUIDE.md` 的 DLP 构建流程执行。

---

## 10. 影子验收（生产写回前的硬门槛）

### 10.1 开关与样本

- 测试/预发：`question_resolve_generation_enabled=1`、`question_resolve_writeback_enabled=0`；只写 shadow draft。
- 生产：验收前两个开关均 0，既有缓存可读，禁止新生成。
- 样本 30~50 道真实题，必须覆盖：单选 ≥10、多选 ≥6、判断 ≥6、主观 ≥4、数值表格候选 ≥6、安全/法规 ≥2、无知识点名/长 HTML 题干 ≥2。

### 10.2 验收指标

| 指标 | 门槛 |
|---|---|
| 必需小节与答案硬结构 | 清洗后 100% 合格（不合格不得落 resolve） |
| 单/多/判答案与题库一致 | 100% |
| 表格数字与题面一致 | 100% |
| 知识性错误、与正确答案冲突 | 0 条 |
| 人工评审“有助于学员理解” | ≥90% |
| 表格滥用 | 无数值触发条件的题不得出现表格 |
| 安全/法规白名单 | 0 次 LLM 调用 |

评审记录至少包含：questionId、模型、promptVersion、输入摘要、原始输出、清洗结果、表格是否丢弃、人工 verdict、问题备注。抽样通过后才允许 production `generation=1 + writeback=1`。

### 10.3 复测与回滚

prompt/清洗器任何后续变更都要复跑同一 sourceHash 样本集；若线上发现坏解析：

1. `question_resolve_writeback_enabled=0` 停止新写回；
2. 必要时 `question_resolve_generation_enabled=0` 停止新生成；
3. 已有人工/历史 resolve 不受影响；坏 AI 行必须**同时**将对应 generated draft 标记 `rejected` 并清空该题 `resolve`（draft 原文保留审计），此后读取器才会走官方/重新生成流程。**不得只改 draft 状态而保留坏文本，也不得批量 DELETE 题库数据**。

### 10.4 “使用即建设”的运行指标（O-7 使用侧口径）

全库覆盖率只做背景，不作为开放学员功能的前置。上线后按以下指标评估资产建设效果：

| 指标 | 含义与用途 |
|---|---|
| 近 30 天作答题解析覆盖率 | `ai_review_answer` 与 `exam_user_answer_log` 的去重题目中，已有 `resolve` 或 answer JSON 官方解析的比例；W8.2 的主要准入指标 |
| 点击后生成成功率 | 同期 `generated / (generated + rejected)`；低成功率先查模型/输入/质量门，不盲目扩量 |
| 缓存复用 | 同题第二次及以后读取不再产生 LLM 调用；通过服务日志按 questionId/source 抽查，不要求新建统计表 |
| 质量拒绝与表格丢弃 | 观察 rejected 原因、表格丢弃率，作为 prompt/清洗器复盘输入 |

近 30 天使用侧覆盖率参考 SQL（生产只读执行，列名已按实体核对）：

```sql
WITH used AS (
  SELECT DISTINCT question_id AS qid
  FROM ai_review_answer
  WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
  UNION
  SELECT DISTINCT question_id AS qid
  FROM exam_user_answer_log
  WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
)
SELECT COUNT(*) AS used_questions,
 SUM(CASE WHEN d.resolve IS NOT NULL AND TRIM(d.resolve)<>''
           OR d.answer REGEXP '"(resolve|analysis|jiexi|explain|解析)"[[:space:]]*:'
          THEN 1 ELSE 0 END) AS covered_questions,
 SUM(d.resolve IS NOT NULL AND TRIM(d.resolve)<>'') AS with_resolve
FROM used u
JOIN exam_exam_detail d ON d.id = u.qid
WHERE d.deleted = 0;
```

`covered_questions / used_questions` 即使用侧解析覆盖率（同一题多来源只计一次）。

**首次基线（2026-10-26，生产只读执行成功）**：近 30 天去重作答题 13,986 道，已有解析 166 道，覆盖率约 **1.19%**；其中 `resolve` 非空 166 道，answer JSON 官方解析 0 道。该基线用于验证“使用即建设”后的增长，不作为开放学员功能的前置。

W8.2 评估沿用原阈值但**改到使用侧**：≥70% 可考虑全量；50%~70% 只做漏洞点+错因、不主动引导看解析；<50% 继续暂缓 W8.2 的泛化引导。该门槛不影响 W8.1/标准解析按需使用。

---

## 11. 验收清单

### 自动化

- [ ] §9 所有测试完成且全绿；全量回归无新增失败
- [ ] 两个前端构建通过；无 BOM；无第二套 QuestionTypes/官方 JSON 解析实现
- [ ] `/appsmart/question/resolve` 匿名不能生成；缓存命中不耗 LLM、不计限流
- [ ] 同题并发生成只有一次 LLM；失败后 24h 内不重复；题目变更后 hash 变化可重新生成
- [ ] 既有 `QuestionResolveThinkingTest` 继续绿

### 人工

- [ ] shadow 样本指标全部达标并留评审记录
- [ ] “使用即建设”链路：首位学员点击后生成并回写，后续学员同题直接读缓存；无需先做全量预生成
- [ ] W8.1 默认 `wrong`（首次答错即可讲），`star` 可切换为收紧档
- [ ] 使用侧统计：§10.4 SQL 可执行并记录基线；上线后覆盖率、生成成功率有复盘记录
- [ ] 真实 LLM：数值表格题表格合规，普通题无表格，判断/多选/主观分支正确
- [ ] App 五页 + 班级刷题：表格不炸、无表格老数据零感知、空解析按钮可用、失败有文案
- [ ] 降级：手工写入残表/非法 markdown 不白屏；LLM 失败不写 resolve；锁竞争提示生成中
- [ ] FAQ：含表格解析进 WeKnora 后无裸管道符
- [ ] 管理端 2000 字前后端拦截一致；人工编辑的解析不被后续 AI 覆盖
- [ ] 同步学 W8.1 实施时只复用 ensureResolve，无独立三段式/draft/第二套生成链

---

## 12. 实施顺序（tracer bullet，禁止按层横切）

1. **最窄端到端**：统一官方读取器 + sourceHash → 一道单选题 ensureResolve 生成/清洗/回写 → syncExam 一个展示点读取并渲染 → 缓存第二次零 LLM。
2. 补齐质量门、失败冷却、Redis 锁、限流与 shadow 开关。
3. 扩展多选/判断/主观、表格规则与 FAQ 扁平化。
4. 接入 App 其余展示点与班级刷题按需入口。
5. 全量自动化回归 → shadow 样本验收 → 用户签字后开放生产写回。

每一步都必须可独立验证、可回滚；不得先写完全部后端再发现 App 数据契约不符。

---

## 13. 部署顺序与风险

1. 用户先把两库 `resolve` 改 text（先测试后生产）。
2. 后端带两个开关默认 0 发布：此时只统一读取与渲染，不开放新生成；公开匿名生成风险已关闭。
3. 测试/预发开 generation=1、writeback=0，执行 shadow 验收。
4. 验收通过，生产 generation=1、writeback=1，App 随版本发布并**立即开放学员按需使用**：首次点击生成，后续全员读缓存，解析资产随真实做题持续积累。
5. 上线后按 §10.4 跟踪使用侧覆盖率、生成成功率与缓存复用；达到使用侧门槛后再评估 W8.2，不要求先做全量预生成。
6. App 发版滞后：旧 App 看到表格 markdown 原文，仍可读、不炸；后端质量门先行保证无烂表。
7. 回滚优先后端开关；代码回滚可恢复旧 prompt，但**不得依赖旧 250 截断逻辑**。

| 风险 | 缓解 |
|---|---|
| 首学员等待 LLM | 只在主动点击时发生；按钮 loading；主作答接口不调用 |
| 生成质量不合格成为长期缓存 | S1~S14 + shadow 验收 + rejected 不读取 |
| 蓝绿重复生成 | Redis 锁 + 锁内二次检查 + 失败冷却 |
| 匿名刷接口 | 强制登录 + 每学员每日限流 |
| 题目更新后旧解析 | sourceHash 判旧；人工解析除外 |
| App 老版本 | 表格裸文本可读；不阻断作答 |

---

## 14. 与同步学后续工作的接口

- **W8.1**：只做门禁与入口，内容完全复用 `ensureResolve`；不再三段式、不再独立缓存/留痕类型。
- **W8.2**：全库覆盖率低不构成学员使用解析的障碍；其泛化“去看解析”提示是否开放，按 §10.4 的近 30 天作答题覆盖率与生成成功率评估，未达标前继续暂缓。
- **批量预生成**：不是上线前置。后续若针对高频题补做，必须调用 `ensureResolve`，复用输入资格、质量门、冷却、锁与 draft；不得绕过服务直连 LLM。
- **质量复盘**： shadow 样本集与 sourceHash 可重复利用；prompt 版本升级只允许通过新 promptVersion 的显式工作包，不在学员请求里自动重写已有解析。
