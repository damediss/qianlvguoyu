# 试题解析 Markdown 表格化 — 一期实施方案

> 版本：v1.0（2026-10-26）
> 状态：**待用户审核，未实施**
> 涉及仓库：`qianlvguoyu_saas`（后端 + admin-web）、`gjjd_appnext`（学员 App）
> 设计口径来源：Karpathy「输出格式阶梯」——LLM 不画图，只产出受控数据；图形表达交给确定性渲染器。一期只放行 **Markdown 表格** 这一种图形化语法。

---

## 1. 背景与目标

`exam_exam_detail.resolve` 用于存放试题解析，由 `QuestionResolveAssistService` 在学员答错后按需调用 LLM 生成并回写。现有 prompt 只要求"首行考点/易错 + 一两句说明为什么选该答案"，存在四个缺口：

1. 只讲"为什么对"，不讲"为什么错"（学员恰恰是在错题页看解析，干扰项分析信息密度最高）；
2. 题型原码（`danxuan`/`single`/`1`）原样进 prompt，LLM 会照抄出"单选题danxuan"之类表述；
3. 无受众设定、无受控语言约束，解析常写成教材腔长难句；
4. 结构松散，且无任何图形化表达——数值对比类题目（工艺参数、材料配比、电流电压档位）纯文字讲不清。

**一期目标**：prompt v2（受控语言四原则 + 四行骨架 + 按题型分支）+ 仅放行 Markdown 表格 + 两端确定性渲染 + 全链路降级安全。

**已拍板口径**（用户 2026-10-26 确认）：

- `resolve` 列类型改 **text 由用户自行处理**，本方案**不含任何数据库迁移**；
- 只做"第一步"：Markdown 表格；chart 图表块（第二步）、Mermaid、SVG、服务端出图均不做；
- 信息量"更多但不要太多"：字数按题型分级，表格有硬性触发条件。

---

## 2. 范围与非目标

### 2.1 范围

| 仓库 | 改动 |
|---|---|
| saas 后端 | prompt v2 替换、生成结果清洗器、长度三道闸门、题型归一化收口、FAQ 链路表格扁平化 |
| appnext App | 表格转 HTML 纯函数 + 8 处解析展示点条件渲染（零新依赖） |
| admin-web | 两个编辑框 maxLength=2000（仅此一项，**无渲染改动**） |

### 2.2 非目标（已核实，勿顺手做）

| 项 | 结论 | 依据 |
|---|---|---|
| 数据库迁移 | 无。列类型用户自行改 text | 用户拍板 |
| 竞赛链路 | **零改动**。竞赛组卷时 `resolveContent` 置 null，监考/预览页 Alert 明示"不显示标准答案或解析"，App 端无任何 resolveContent 渲染点 | `CompetitionTheoryPaperApplicationService:409`、`CompetitionTheoryExamPage.tsx:573/657`、appnext 全库 grep 无 resolveContent |
| admin-web 解析渲染 | 零改动。管理端只有编辑场景（TextArea 编辑 markdown 源码），无面向学员的只读展示 | `QuestionBankPage.tsx:290`、`CompanyQuestionPage.tsx:183` |
| randExam / sortExam | 零改动。两页无 resolve 模板插值（"查看解析"是主观题 AI 判分弹窗按钮，非解析展示点） | appnext grep 实测 |
| 小培讲题 | 零改动。resolve 仅作为文本上下文喂给讲题链路，表格 markdown 当纯文本无副作用 | `QuestionResolveAssistService.peekResolve` javadoc |
| chart 图表块 / Mermaid / SVG / 服务端出图 | 不做 | 用户拍板"只做第一步" |

---

## 3. 现状事实底座（实施模型必读，防止误判）

### 3.1 数据库实测（2026-10-26）

| 库 | `resolve` 实际类型 | 数据量 |
|---|---|---|
| 测试库 gjjddb2026（114.132.67.185:3309） | **已是 text** | exam_exam_detail 共 325,179 行 |
| 生产库 gjjddbpro（43.138.163.7:3308） | **varchar(600)** | 表 353,760 行 / 158 MB；有效题 185,886 行中仅 393 行有解析（平均 64.8 字、最长 141 字） |

> `QuestionResolveAssistService:39-40` 的注释"现网多为 varchar(255)"已过时，`RESOLVE_DB_MAX = 250` 是自缚，本方案一并修正。**前置条件：用户完成两库列类型改 text 后，本方案代码才可上生产**（代码不再按 250 截断，若生产仍是 varchar(600)，800 字软截断尚未触底也不会报错，但建议先改列）。

### 3.2 解析展示点全清单（App 端，共 5 文件 8 处）

| 文件 | 行 | 现渲染方式 |
|---|---|---|
| `pages/exam/syncExam.vue` | 52, 65, 117, 129 | `<view>解析：{{ resolve }}</view>` 纯文本插值 ×4（自评解析 + 结果解析各两处） |
| `pages/exam/syncMock.vue` | 60 | `<view class="resolve-text">解析：{{ w.resolve }}</view>` |
| `pages/exam/practiceWrongBook.vue` | 26 | `<text>{{ plainStem(q.resolve \|\| q.analysis) }}</text>`（套了 plainStem 剥 HTML） |
| `pages/class/classPracticeDone.vue` | 31 | `<text class="analysis-text">{{ q.analysis }}</text>` |
| `pages/class/homeworkResult.vue` | 60 | `<view class="meta">解析：{{ row.analysis }}</view>` |

App 为 uni-app **Vue 3**（manifest `vueVersion: "3"`），题干已用内置 `<rich-text :nodes>` 渲染 HTML（`syncExam.vue:28`）——**App 已有 HTML 渲染路径，无需引入 mp-html/towxml**。

### 3.3 后端消费方

| 消费方 | 位置 | 一期处理 |
|---|---|---|
| App 出题接口 | `AppExamSmartService:1383/1806` 纯透传 | 无需改 |
| FAQ 知识生成 | `ExamFaqConverter:90-91` 把 resolve 拼进 FAQ 答案数组 | **需要扁平化**（见 §4.5） |
| JEV 分层/同步学 | `KnowledgeFixClassifyService:603/769`、`ReviewQuestionQueryService:399` 拼文本作 LLM 上下文 | 无需改（表格 markdown 当文本） |
| 企业题库手工编辑 | `CompanyQuestionApplicationService:126/153` 直写 req.resolve()，现无长度校验 | **加校验**（见 §4.3 闸门三） |

---

## 4. 设计

### 4.1 Prompt v2（替换 `QuestionResolveAssistService.generateByLlm` :222-230 两个字符串）

**System prompt（固定全文，`{预算}` 按题型填入）：**

```
你是职业技能等级认定考试的辅导老师，考生是一线在职工人。写解析必须遵守：
1. 短句：一句只说一件事，单句不超过 25 个字。
2. 主动语态：写"断电后才能更换熔断器"，不写"熔断器的更换应当在断电后进行"。
3. 同一事物用同一个词：题干里叫"电解槽"，解析里就叫"电解槽"，禁止换成同义词。
4. 只依据题干、选项和正确答案作答，可以引用岗位操作常识，但禁止编造
   标准编号、文件名称、具体数据。
5. 正文不超过 {预算} 字。除第 7 条允许的表格外，不得使用任何 markdown、
   emoji 或符号排版。
6. 严格按四行结构输出，每行以【】小节名开头，不输出小节名以外的内容：
【考点】本题考查的知识点，10 字以内。
【正确】为什么选 X：先给结论，再给原因。
【干扰】最容易错选的选项及其错误原因。
【记法】一条现场记忆口诀或动作要点，不超过 30 字。
7. 表格规则（唯一允许的 markdown 语法）：仅当题干包含 2 个及以上可对比的
   数值、配比或档位，且表格确实能让解析更易读时，才在【正确】行之后插入
   一张表格；其余情况禁止输出表格。表格要求：
   - 标准管道语法：表头行、分隔行（|---|---|）、数据行各占一行；
   - 列数不超过 3，数据行不超过 4，每个单元格不超过 12 个字；
   - 表格内容必须是题干或答案中出现的原始数值，禁止推算新数据。
   表格不占用第 5 条的字数预算。
```

**User prompt（模板，`{...}` 由服务拼装）：**

```
题型：{中文题型}
题干：
{题干，800 字内}
{选项块：仅单选/多选有，"A. xxx" 每行一个，600 字内}
正确答案：{答案，200 字内}
{分支行，按题型注入其一：}
  多选题 → 「正确答案是组合。【正确】逐项说明每个入选项为什么入选；【干扰】点名最常漏选或最常多选的项。」
  判断题 → 「本题无选项。【干扰】改为说明错误说法错在哪、正确做法是什么。」
  简答题 → 「【正确】改为按要点逐条列出答案要点（即给分口径）；省略【干扰】行。」
请按四行结构写出解析：
```

**题型预算与归一化映射**（新建 `QuestionTypes` 工具类，见 §4.4）：

| 库内码（全部历史形态） | 归一化 | 中文题型 | 字数预算 |
|---|---|---|---|
| `single` / `danxuan` / `单选` / `单选题` / `1` | single | 单选题 | 240 |
| `multi` / `duoxuan` / `多选` / `多选题` / `2` | multi | 多选题 | 300 |
| `judge` / `pandan` / `判断` / `判断题` / `3` | judge | 判断题 | 240 |
| `short` / `jianda` / `简答` / `简答题` / `4` | short | 简答题 | 400 |
| 其它/未知 | 原样 | 未标注题型 | 240（按通用四行，保留【干扰】） |

> 字数依据：生产实测现有解析平均 64.8 字，四行骨架常规题 150~200 字足够；240 起步给"为什么错"留空间，多选逐项/简答要点放宽。

**调用参数不变**：`max_tokens=800` 保持；`thinking` 继续透传模型配置（`QuestionResolveThinkingTest` 已锁定该契约，禁改回硬编码）。

### 4.2 生成结果清洗器 `ResolveMarkdownSanitizer`（后端，新建）

LLM 输出不直接落库，先过一遍确定性清洗（位置建议 `exam/service/` 或 `ai/support/`）：

| # | 规则 | 动作 |
|---|---|---|
| S1 | 首尾 ``` 代码围栏 | 剥除（LLM 常把表格包进围栏） |
| S2 | 表格识别：≥3 个连续行均以 `|` 开头且第 2 行为分隔行（只含 `|`、`-`、`:`、空格） | 认定为表格块 |
| S3 | 表格超限：列 >3 或数据行 >4 或单元格 >12 字 | **整表丢弃**，只留四行文本（宁缺毋滥，不放烂表格上屏） |
| S4 | 残行：表格块内某行 `|` 数与表头不一致 | 整表丢弃 |
| S5 | 孤立 `|`（单行出现、不构成表格块） | 原样保留（是正文不是表格） |
| S6 | 【】小节名之外的 markdown 痕迹（`#`、`*`、`-` 列表符开头的行） | 剥除标记符，保留文字 |
| S7 | 全长软截断 800 字（在表格之外的位置截，表格完整保留） | 截断 |
| S8 | 四行小节名缺失 ≥2 行（模型跑飞） | 返回 null 走"生成失败"路径（不落库、不留 draft） |

> S8 的"生成失败"复用现有 `generateByLlm` 返回 null 的语义：不写 draft、不回写 resolve、接口返回 empty 源。

### 4.3 长度三道闸门

| 闸门 | 位置 | 规则 |
|---|---|---|
| 一、prompt 预算 | §4.1 | 按题型 240/300/400 字，表格不计入 |
| 二、生成软截断 | 清洗器 S7 | 800 字硬上限（防模型失控，与列类型解耦） |
| 三、手工编辑校验 | 后端 `CompanyQuestionApplicationService` save/update + 前端两处 TextArea | resolve ≤2000 字，超长报业务错误"解析不能超过 2000 字"；前端 `QuestionBankPage.tsx:290`、`CompanyQuestionPage.tsx:183` 的 TextArea 加 `maxlength={2000}` |

**同步删除的旧截断逻辑**（列改 text 后失效）：

- `QuestionResolveAssistService:40` `RESOLVE_DB_MAX = 250` 常量及全部引用；
- `peekResolve`（:97-104）与 `ensureResolveLocked`（:122-133）里"db 文本 ≥245 字 → 查 draft 找全文"的回退分支——库列即唯一权威全文；
- `backfillResolveColumn`（:267-268）的 250 截断。

`ai_generation_draft` 留痕逻辑**保留不动**（AI 审计合规要求：来源/模型/时间）。

### 4.4 题型归一化收口（统一语言，禁止第二份实现）

现状：`AppExamSmartService.normalizeQuestionType`（:347，private static）只归一 single/multi/judge，缺 short/简答题。多处散落 switch（`ExamExamDetailApplicationService:712/753/998`、`CompanyQuestionApplicationService:339-357`）。

**收口方式**：新建 `com.guoyu.platform.exam.support.QuestionTypes`（静态工具）：

```java
public static String normalize(String raw)   // → single/multi/judge/short/原样小写
public static String toLabel(String raw)     // → 单选题/多选题/判断题/简答题/未标注题型
public static int    wordBudget(String raw)  // → 240/300/240/400/240
```

- `AppExamSmartService.normalizeQuestionType` 改为委托 `QuestionTypes.normalize`（保持 private 包装，调用点不动）；
- `QuestionResolveAssistService` 用 `toLabel` + `wordBudget` 拼 prompt；
- **不要求**一期迁移其它散落 switch（避免扩大爆炸半径），但新代码禁止再添第二份映射。

### 4.5 FAQ 链路扁平化

`ExamFaqConverter:90-91` 拼接前调用新增的 `ResolvePlainText.flatten(resolve)`（可与清洗器同包）：

- 无表格 → 原样返回；
- 有表格 → 每个数据行转成「表头1值1、表头2值2；表头1值3、表头2值4」短句（顿号连单元格、分号分行），管道符与分隔行不输出——FAQ 答案进 WeKnora 是纯文本场景；
- 转换异常 → 降级为剥掉全部 `|` 与分隔行，保证 FAQ 里永远见不到裸管道符。

### 4.6 App 端渲染（appnext，零新依赖）

**核心纯函数** `utils/resolveTable.js`（两端规则与后端清洗器同构，App 侧只做**宽松识别 + 安全渲染**，不做超限丢弃——后端落库前已保证质量）：

```js
// hasTable(text): 存在 ≥3 个连续以 | 开头的行且第 2 行为分隔行 → true
// resolveToHtml(text): 表格块 → <table class="rt"><tr><td>…；其余文本行 → escape 后以 <br/> 连接；
//                      单元格与文本一律 &<>"' 转义；解析失败/无表格 → 返回 null
```

**展示点改造模式**（8 处统一，以 syncExam.vue 为例）：

```html
<!-- 前 -->
<view v-if="q.result.resolve" class="resolve">解析：{{ q.result.resolve }}</view>
<!-- 后：hasTable 才走 rich-text，无表格保持原 <text>/<view> 不动（老数据零感知） -->
<view v-if="q.result.resolve" class="resolve">
  解析：<rich-text v-if="hasTable(q.result.resolve)" :nodes="resolveToHtml(q.result.resolve)"></rich-text>
  <text v-else>{{ q.result.resolve }}</text>
</view>
```

- 表格样式随页面既有 `.resolve` 容器（字号/行距继承），`rt` 表格内置最小样式：单元格 1px 边框 + 4px 内边距（App rich-text 支持 table/tr/td 的内联样式与部分 class，样式直接写进生成的 HTML 内联属性最稳，不依赖页面样式表穿透）；
- `practiceWrongBook.vue:26` 注意：现有 `plainStem()` 会剥 HTML——表格命中时**跳过 plainStem** 直接走 rich-text 分支；
- 降级双保险：`resolveToHtml` 返回 null / 抛错 → 自动落到 `<text>` 原文本分支，**任何情况下不白屏**。

---

## 5. 改动清单（文件级）

### saas 后端

| 文件 | 动作 |
|---|---|
| `app/exam/service/QuestionResolveAssistService.java` | 替换 generateByLlm 两个 prompt 字符串；接入 QuestionTypes/清洗器；删 RESOLVE_DB_MAX 与两处 draft 回退分支 |
| `exam/support/QuestionTypes.java` | 新建（normalize/toLabel/wordBudget） |
| `exam/service/ResolveMarkdownSanitizer.java` | 新建（S1~S8） |
| `exam/service/ResolvePlainText.java` | 新建（FAQ 扁平化） |
| `exam/service/ExamFaqConverter.java` | :90-91 接入 flatten |
| `exam/service/CompanyQuestionApplicationService.java` | save/update 加 ≤2000 校验 |
| `app/exam/service/AppExamSmartService.java` | normalizeQuestionType 改委托 QuestionTypes |
| `exam/support/QuestionTypesTest.java` 等新测试 | 见 §6 |

### appnext App

| 文件 | 动作 |
|---|---|
| `utils/resolveTable.js` | 新建（hasTable / resolveToHtml 纯函数） |
| `pages/exam/syncExam.vue` ×4 处 | §4.6 模式改造 |
| `pages/exam/syncMock.vue:60` | 同上 |
| `pages/exam/practiceWrongBook.vue:26` | 同上（跳过 plainStem） |
| `pages/class/classPracticeDone.vue:31` | 同上 |
| `pages/class/homeworkResult.vue:60` | 同上 |

### admin-web

| 文件 | 动作 |
|---|---|
| `pages/exam/QuestionBankPage.tsx:290` | TextArea 加 `maxlength={2000}` |
| `pages/exam/CompanyQuestionPage.tsx:183` | 同上 |

---

## 6. 自动化测试清单

### 后端（JUnit 5 + Mockito，随改动同步编写）

| 测试类 | 用例点 |
|---|---|
| `QuestionResolvePromptTest`（新建） | ① system prompt 含四行小节名与表格规则关键词；② `danxuan`→user prompt 出现"单选题"且预算 240；`duoxuan`→300、"多选题"；`panduan`→240；`jianda`→400、"简答题"；③ 判断/简答分支行正确注入、单选/多选不注入简答分支；④ thinking 仍来自配置（沿用 `QuestionResolveThinkingTest` 契约，该测试必须继续绿） |
| `ResolveMarkdownSanitizerTest`（新建） | S1 剥围栏；S2 正常表格保留；S3 超 3 列/超 4 行/单元格超 12 字整表丢弃；S4 残行丢表；S5 单行 `|` 保留；S6 剥 `#`/`*` 标记；S7 801 字截断、表格完整优先；S8 缺 ≥2 个小节名 → 返回 null |
| `QuestionResolveAssistServiceTest`（扩展既有风格） | ① 生成含表格文本 → 回写 resolve 全文（无 250 截断）；② db 已有全文 → 直接返回不查 draft（回退分支删除后行为）；③ 清洗器返回 null → 不写 draft、resolve 返回 empty |
| `ResolvePlainTextTest`（新建） | 无表格原样；表格→顿号/分号短句且无 `\|`；异常降级剥管道 |
| `ExamFaqConverterTest`（扩展） | resolve 含表格 → FAQ 答案数组无管道符；无表格行为不变 |
| `QuestionTypesTest`（新建） | 全部历史码形态映射表逐个断言（§4.1 表格） |
| `CompanyQuestionResolveLimitTest`（新建） | resolve 2001 字 → 业务异常；2000 字 → 通过 |

### admin-web（vitest）

- 无新组件，不新增测试；`npm run build` 全量回归必须绿（防 maxLength 改动破构建）。

### appnext App（诚实边界：无单测基建）

- `resolveTable.js` 设计为**纯函数**（无 uni API 依赖），规则与后端清洗器同构，规则正确性由后端 `ResolveMarkdownSanitizerTest` 兜底；
- App 侧验证走 §7 人工走查，**不虚构自动化测试**。

---

## 7. 验收清单

### 自动化（实施完成即勾，附证据）

- [ ] `mvn compile -DskipTests` 0 错
- [ ] `mvn test`（platform-api）全绿：上述 7 个测试类新增/扩展用例全过，`QuestionResolveThinkingTest` 契约保持
- [ ] `npm run build`（admin-web）0 错
- [ ] appnext `pnpm build`（或项目既有构建命令）0 错
- [ ] 空库迁移冒烟不受影响（本方案无迁移，跑一次既有冒烟确认无回归）

### 人工（部署后逐项走查）

- [ ] **真实 LLM 冒烟（必需，用测试库真实题目）**：
  - [ ] 1 道数值对比单选题（如工艺参数/材料配比）→ 解析含合规表格（≤3 列 ≤4 行），App 表格正常渲染、数值与题干一致；
  - [ ] 2 道无数值普通单选题 → **不得出现表格**（触发闸门验证）；
  - [ ] 判断/多选/简答各 1 道 → 分支行正确（判断题【干扰】说错误原因；多选逐项；简答无【干扰】）；
  - [ ] 1 道题故意在管理端把 resolve 清空后触发生成 → draft 留痕有记录、模型名正确（审计合规）。
- [ ] **App 五页走查**（真机或 H5）：同步学考试、模考错题、错题本、班级练习完成页、作业结果页——表格渲染无错位，无表格解析与改版前一致；
- [ ] **降级验证**：手工在库里写一条带非法表格（残行）的 resolve → App 显示原文本不白屏；
- [ ] **FAQ 链路**：含表格解析的题目触发 FAQ 生成 → WeKnora 侧答案无裸管道符；
- [ ] **管理端编辑**：题库编辑页/企业题库页粘贴 2000+ 字解析 → 前端拦截 + 后端报错文案正确；
- [ ] **存量回归**：现网 393 条短解析（平均 65 字）在 App 五页展示与改版前一致（无表格老数据零感知）。

---

## 8. 风险与降级

| 风险 | 影响 | 缓解 |
|---|---|---|
| App 发版滞后于后端 | 后端先上 → App 未更新的用户看到裸 markdown 表格（管道符文本） | 可读、不炸页面；`ExamFaqConverter` 与后端清洗器先行保证落库质量；App 发版后自动恢复 |
| LLM 表格质量不稳（推算数据/超限） | 烂数据上屏 | S3/S4 整表丢弃 fail-safe；prompt 禁止推算 |
| 列类型未改 text 就部署后端 | 软截断 800 > varchar(600) → 超长报 Data too long | 部署顺序硬约束：**先改列（用户）后发后端**；§3.1 已注明 |
| rich-text 表格在个别低版本 WebView 样式异常 | 表格错位 | 样式全部内联在生成的 HTML；走查覆盖真机；极端情况 `resolveToHtml` 返回 null 自动落回纯文本 |

## 9. 部署顺序与回滚

1. 用户完成两库 `resolve` 列改 text（先测试库后生产）；
2. saas 后端随蓝绿发布（含 §5 全部后端改动）；
3. admin-web 随前端发布（仅 maxLength）；
4. appnext App 独立发版（无硬顺序约束，滞后只影响观感）；
5. 回滚：后端回滚即恢复旧 prompt 与 250 截断逻辑（旧逻辑对 text 列无害）；App 回滚后表格裸文本可读。无数据库回滚项。
