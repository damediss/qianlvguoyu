# 试题解析 prompt v2.2 与答案存疑分支实施方案

> 版本：v1.0（2026-10-27）
> 前置：《试题解析Markdown表格化_一期实施方案》v2.2（prompt v2.1 / sanitizer S1~S14 / 模型隔离 / shadow 验收均已实施）
> 本文档是**设计与任务书**，代码实施交给执行 agent。条款、方法名、校验点、测试用例以本文为准。

## 1. 背景与问题定性

生产实例：「电压负反馈调速系统对电网电压变化和负载变化（）」，题库答案 A（没有），教材口径应为 C（对前者有补偿能力，对后者无）。模型生成了「按本题给定的正确答案，它对电网电压变化也无补偿能力」「B/C/D 均与题干给出的正确答案不一致」。

根因不是模型偶发发挥，而是 prompt v2.1 的结构性缺陷，三处叠加：

1. **答案被公理化**：system 第 4 条「只依据题干、选项、正确答案……作答」+ user prompt 把「正确答案」作为事实输入，模型没有独立解题步骤，任务退化为「为答案找说辞」。
2. **硬结构逼出空话**：`branchLine` 强制「【正确】必须以"答案：X。"开头，解释该答案与题干条件的对应关系」，答案解释不通时只能写「按本题给定的正确答案……」。
3. **【干扰】允许同义反复**：「无法确定时，只写'与题干给出的正确答案不一致'」给了零信息量排除句式，正确选项被一句"与答案不一致"判掉。

后果：答案错 → 解析必然错，且错得自洽、格式标准、比没有解析更误导学员。

## 2. 总体设计

把生成链路从「单分支（按答案写解析）」改为「双分支」：

- **正常解析**：模型独立解题结论与给定答案一致 → 沿用四行结构（【考点】【正确】【干扰】【记法】），现有质量门不变。
- **答案存疑**：模型能指出题干条件与给定答案的**具体矛盾**并给出**教材级原理依据** → 输出【考点】【存疑】两行结构。存疑结果**不写回 resolve 列、不展示给学员、不进失败冷却**，落 `ai_generation_draft`（status=`answer_disputed`）作为人工复核工单；同 sourceHash 再次请求命中存疑缓存直接返回，不重复调 LLM。

三条不变红线：

- **判分口径不变**：判分仍按题库答案，存疑只影响解析生成与展示。
- **存疑全文不对学员展示**：学员端只看到中性提示，避免学员被模型质疑误导。
- **仅客观题开放存疑**：主观题答案是要点集合，模型独立作答与给定要点表述不同是常态，开放存疑会泛滥。主观题始终按给定答案要点写解析；主观题输出【存疑】按结构异常拒绝。

## 3. prompt v2.2 完整条款

文件：`services/platform-api/src/main/java/com/guoyu/platform/exam/service/QuestionResolvePromptPolicy.java`

术语统一（ubiquitous language）：输入侧「正确答案」改为「**给定答案**」，消除"答案必对"的预设立场；输出侧【正确】仍以「答案：X。」开头（S9 硬校验不变）。

### 3.1 systemPrompt(int budget) 全文（替换现有方法返回串）

```
你是职业技能等级认定考试的辅导老师，考生是一线在职工人。写解析必须遵守：
1. 短句：一句只说一件事，单句不超过 25 个字。
2. 主动语态：写“断电后才能更换熔断器”，不写“熔断器的更换应当在断电后进行”。
3. 同一事物用同一个词：题干里叫“电解槽”，解析里就叫“电解槽”，禁止换成同义词。
4. 只依据题干、选项和所给知识点名称作答。可以引用岗位操作常识，
   但禁止编造标准编号、文件名称、具体数据，禁止输出学员作答或“你选了 X”。
5. 先独立解题，再对照给定答案：先独立分析本题应选什么、依据是什么，再对照给定答案。
   - 结论一致：按第 8 条 A 结构输出解析。
   - 结论不一致：只有当你能指出题干条件与给定答案的具体矛盾，并给出教材级
     原理依据时，才按第 8 条 B 结构输出【存疑】；只是表述别扭、你不确定、
     答案看起来少见，都不算矛盾，此时仍按给定答案写解析，但禁止硬编理由。
   - 仅客观题（单选/多选/判断）可输出【存疑】；主观题不得存疑，始终按给定
     答案要点写解析。
6. 正文不超过 %d 字。除第 9 条允许的表格外，不得使用任何 markdown、emoji、
   HTML、链接或代码块。
7. 不出现“保过/必过/稳过”，不出现模型名、参数名、表名、内部文档名。
8. 输出结构（二选一），每行以【】小节名开头，不输出小节名以外的内容：
   A. 正常解析（与给定答案一致时）：
   【考点】本题考查的知识点，10 字以内。
   【正确】解释答案与题干条件的对应关系，用原理说明为什么对，禁止只写
   “符合题意”“题目给出的答案就是 X”“按本题给定的正确答案”这类空话。
   【干扰】说明错误选项与题干条件的具体冲突点，只能依据题干、选项和原理，
   禁止引入题干未提供的等级、标准、分类、历史、背景或其他参数；指不出具体
   冲突点的选项，省略不写；禁止用“与题干给出的正确答案不一致”“与答案不符”
   这类以答案为理由的排除句。
   【记法】一条现场记忆口诀或动作要点，不超过 30 字。
   B. 答案存疑（仅当满足第 5 条矛盾条件的客观题）：
   【考点】本题考查的知识点，10 字以内。
   【存疑】先写“给定答案：X。”，再写“按原理应为：Y。”，然后用 2~4 句说明
   题干条件与给定答案的具体矛盾及原理依据。只输出【考点】【存疑】两行，
   不输出【正确】【干扰】【记法】，不使用表格。
9. 表格规则（唯一允许的 markdown 语法）：仅当题干包含 2 个及以上可对比的
   数值、配比或档位，且表格确实更易读时，才在正常解析的【正确】内容之后
   插入一张表格；其余情况禁止表格。表格必须为管道语法：表头行、分隔行
   （|---|---|）、数据行各占一行；列数不超过 3，数据行不超过 4，每格不超过
   12 字；表格数值必须原样来自题干、选项或给定答案，禁止推算新数据。
   表格不占字数预算。
```

要点说明（执行 agent 不要改动这些设计意图）：

- 第 4 条删去「正确答案」作为作答依据（答案不再是公理），改为「只依据题干、选项和所给知识点名称」。
- 第 5 条是核心新增：独立解题 → 对照 → 存疑门槛（具体矛盾 + 教材级原理依据），并明确"表述别扭/不确定/少见"不算矛盾，压低存疑率。
- 第 8 条 B 规定存疑结构格式，便于 sanitizer 硬校验（必须含「给定答案：」「按原理应为：」）。
- 表格规则不变，仅补「给定答案」术语与「存疑不用表格」。

### 3.2 branchLine(QuestionPromptInput input) 全文

- single：
  `【正确】必须以“答案：{correctAnswerText}。”开头，用原理说明该选项为什么符合题干条件，禁止只写“符合题意”；【干扰】指出具体冲突点，指不出就省略，禁止用答案作排除理由。`
- multi：
  `【正确】必须以“答案：{correctAnswerText}。”开头，逐项说明入选原因；【干扰】点名最常漏选/多选项并指出具体冲突点，只依据题干与选项，不得引入题干未提供的信息，禁止用答案作排除理由。`
- judge：
  `【正确】必须以“答案：对。”或“答案：错。”开头（与给定答案一致），说明原理；【干扰】说明错误说法错在哪、正确做法是什么，不得引入题干未提供的等级或标准。`
- subjective（不开放存疑）：
  `【正确】以“答案要点：”开头，按给分要点逐条说明，禁止只写结论或复述题干；省略【干扰】行，只输出【考点】【正确】【记法】；不得输出【存疑】。`
- unknown（按客观通用）：
  `【正确】以“答案：{correctAnswerText}。”开头，用原理说明该答案与题干条件的对应关系，按通用四行输出；【干扰】禁止用答案作排除理由。`

### 3.3 userPrompt(QuestionPromptInput input) 模板

```
题型：{typeLabel}
知识点：{knowledgeTitle 或 未标注}
题干：
{stem}
选项：
{optionsText}（为空则省略此段，保持现状）
给定答案：{correctAnswerText}
{branchLine}
请先独立分析本题，再对照给定答案，按对应结构输出：
```

改动点：「正确答案：」→「给定答案：」；收尾「请按四行结构写出解析：」→「请先独立分析本题，再对照给定答案，按对应结构输出：」。

### 3.4 budgetFor / maxTokensFor

不变。存疑输出短于正常解析，无需新增预算。

## 4. ResolveMarkdownSanitizer 改动方案

文件：`services/platform-api/src/main/java/com/guoyu/platform/exam/service/ResolveMarkdownSanitizer.java`

### 4.1 SanitizeResult 增加 disputed 分量

```java
public record SanitizeResult(String text, String rejectedReason, boolean tableDropped, boolean disputed) {
  static SanitizeResult ok(String text, boolean tableDropped) {
    return new SanitizeResult(text, null, tableDropped, false);
  }
  static SanitizeResult reject(String reason) {
    return new SanitizeResult(null, reason, false, false);
  }
  static SanitizeResult disputed(String text) {
    return new SanitizeResult(text, null, false, true);
  }
}
```

约定：`disputed=true` 时 `rejectedReason==null` 且 `text` 为清洗后的【考点】【存疑】文本（供留痕与人工复核）；编排方据此走存疑分支，不进入正常写回。

### 4.2 存疑检测的管线插入点

现有顺序：S1 → S14 → S11 → 表格(S2~S5/S10) → S6 → S8 → S9 → S7 → S12。

在 **S6 之后、S8 之前**插入存疑分派：

```java
text = stripListMarkers(text);

// 客观题且含【存疑】标记 → 走存疑校验分支，不做 S8/S9
if (!QuestionTypes.isSubjective(input.normalizedType()) && text.contains("【存疑】")) {
  return sanitizeDisputed(text, tables, input);
}

// 以下走原有 S8/S9/S7/S12（主观题含【存疑】时因缺【正确】等被 S8 拒绝，符合预期）
```

设计理由：

- 放在表格处理之后：存疑文本里的非法表格已被整表丢弃，无需重复表格校验。
- 放在 S8/S9 之前：存疑结构本就不含【正确】【干扰】，不能被四行硬校验误杀。
- 主观题不进入分派：主观题输出【存疑】会因缺【正确】被 S8 以 `missing_section_正确` 拒绝，实现"主观不开放存疑"。

### 4.3 sanitizeDisputed 校验规则（新增私有静态方法）

依次校验，任一失败返回 `SanitizeResult.reject(...)`：

1. **必需小节与顺序**：解析 sections（复用 `parseSections`，注意 `sectionMarkerOf` 需识别【存疑】，见 4.5）；必须含【考点】且【考点】在【存疑】之前；缺【存疑】返回 `missing_section_存疑`，缺【考点】返回 `missing_section_考点`。
2. **存疑格式标记**：【存疑】内容必须包含「给定答案」与「按原理应为」，否则返回 `dispute_format_invalid`。（对应 prompt 第 8 条 B，确保存疑是"针对给定答案"的，不是模型泛泛发牢骚。）
3. **内容下限**：【存疑】内容去空白 < 30 字 → `content_too_short`（复用现有阈值口径）。
4. **禁止内容**：S11 已在分派前执行，无需重复。
5. **长度截断**：复用 `truncatePlainText(text, tables)`；若返回 null → `exceeds_length`；截断后需重新校验 1~3（截断可能砍掉【存疑】尾部内容）。
6. **same_as_stem**：复用现有 S12 的"与题干完全相同"判定思路，命中 → `same_as_stem`。

全部通过返回 `SanitizeResult.disputed(text)`（text 为截断后的最终文本）。

注意：存疑分支**不做 S9 答案前缀校验**（存疑就是要质疑答案），也**不要求【干扰】【记法】**。

### 4.4 S11 新增「答案同义反复句式」禁止

`forbiddenReason` 增加词条（命中返回 `forbidden_answer_tautology`）：

- `与题干给出的正确答案不一致`
- `按本题给定的正确答案`
- `题目给出的答案就是`

这是 prompt 第 8 条【干扰】禁令的双保险：即使模型不存疑，也不允许用"以答案为理由"的排除句/空话。**必须与 prompt v2.2 同步上线**，否则旧 prompt 习惯句式会导致拒绝率上升。

注意：该检测对正常解析与存疑输出都生效（分派在 S11 之后）。存疑输出里允许出现「给定答案：X」，不包含上述三句式，不冲突。

### 4.5 sectionMarkerOf 扩展

`SECTION_OBJECTIVE` 常量不动（四行结构校验仍用它）。`sectionMarkerOf` 增加对 `【存疑】` 的识别，使 `parseSections` 能解析存疑小节；`checkSections` 不变（仍按四行/三行校验正常解析）。

### 4.6 新增 rejectedReason 值汇总

| reason | 触发 |
|---|---|
| `missing_section_存疑` | 含【存疑】意图但解析后无该小节（防御，正常不易触发） |
| `dispute_format_invalid` | 【存疑】缺「给定答案」或「按原理应为」 |
| `forbidden_answer_tautology` | 出现答案同义反复句式 |
| `content_too_short` / `exceeds_length` / `same_as_stem` | 存疑内容复用现有口径 |

## 5. 编排层改动（QuestionResolveAssistService）

文件：`services/platform-api/src/main/java/com/guoyu/platform/app/exam/service/QuestionResolveAssistService.java`

### 5.1 generateAndPersist 存疑分支

在现有 `sanitized.rejectedReason() != null` 处理之后、正常写回之前插入：

```java
if (sanitized.disputed()) {
  if (dictInt(DICT_DISPUTE_ENABLED, 1) != 1) {
    // 存疑出口被关：按质量门拒绝处理（写 rejected + 冷却），止血用
    log.info("[题目解析] 存疑出口已关闭，按拒绝处理 questionId={}", questionId);
    onGenerationFailed(questionId, input, tenantId, content);
    return result(questionId, null, "empty", false, stem, "quality_rejected");
  }
  String disputedText = sanitized.text().trim();
  writeDraft(questionId, input.sourceHash(), disputedText, tenantId, "answer_disputed", false);
  log.info("[题目解析] 答案存疑 questionId={} knowledge={}", questionId, input.knowledgeTitle());
  return result(questionId, null, "empty", false, stem, "answer_disputed");
}
```

要点：

- **不写 resolve 列**（不调用 `backfillResolveColumn`）、**不写 generated/shadow**。
- `writeDraft(..., "answer_disputed", false)`：rejected=false，prompt 用正常 `PROMPT_PREFIX + sourceHash`（带 v2.2 版本号），便于缓存命中与审计。
- **不进 24h 失败冷却**（不调用 `onGenerationFailed`）：存疑不是生成失败；由 answer_disputed 缓存承担"不重复调 LLM"。
- 返回 `source=empty, reason=answer_disputed`，`generated=false`。

### 5.2 readDraftCache 增加存疑缓存命中

在 generated / shadow 检查之后追加：

```java
AiGenerationDraftEntity disputed = findLatestDraft(questionId, "answer_disputed");
if (disputed != null && StringUtils.hasText(disputed.getResultText())
    && OfficialResolveResolver.isCurrentPrompt(disputed.getPrompt())
    && OfficialResolveResolver.promptHashEquals(disputed.getPrompt(), input.sourceHash())) {
  return result(questionId, null, "empty", false, extractStem(detail), "answer_disputed");
}
```

位置在生成开关检查（步骤 5）之前 → 与 generated/shadow 一致，**缓存命中不受生成开关影响**。同 sourceHash 重复点击不再调 LLM、不计限流。

### 5.3 存疑开关 DICT_DISPUTE_ENABLED

- 常量：`private static final String DICT_DISPUTE_ENABLED = "question_resolve_dispute_enabled";`
- `dictInt(DICT_DISPUTE_ENABLED, 1)`：**默认开**，无字典行即可运行，**无需新 Flyway 迁移**。
- 止血方式：需要关闭时向 sys_dict 插入 `question_resolve_dispute_enabled=0`（tenant 10159），存疑输出即退化为 quality_rejected + 冷却。

### 5.4 peekResolve / 锁竞争回退

- `peekResolve`：不读 answer_disputed（只读 generated，保持现状），但其 generated 命中条件要加版本校验（见 §6）。
- 步骤 7 锁拿不到时的 draft 回退：保持只查 generated，不变；稍后重试会命中存疑缓存。

## 6. prompt 版本与缓存失效（关键坑，必须实施）

现状：`OfficialResolveResolver.promptHashEquals` 只取 `#` 后缀比 hash，**不校验版本前缀**（注释明确"不依赖版本做缓存失效"）。若只把 `PROMPT_PREFIX` 改成 v2.2，旧 v2.1 draft 同 sourceHash 仍命中缓存，**新 prompt 永远不生效**。

### 6.1 改动

1. `OfficialResolveResolver.PROMPT_PREFIX` → `question_resolve_v2.2#`；`PROMPT_PREFIX_REJECTED` → `question_resolve_v2.2_rejected#`。
2. 新增：
   ```java
   /** 当前 prompt 版本判定：draft 缓存命中必须版本+hash 双匹配。 */
   public static boolean isCurrentPrompt(String prompt) {
     return prompt != null && prompt.startsWith(PROMPT_PREFIX);
   }
   ```
3. `promptHashEquals` 保持只比 hash（不改签名）。

### 6.2 版本校验加在哪里（两处决策，别搞混）

| 校验点 | 是否加 `isCurrentPrompt` | 理由 |
|---|---|---|
| `readDraftCache` generated / shadow / answer_disputed 命中 | **加** | 未落列的 draft 缓存必须按新版本重新生成，否则 v2.2 不生效 |
| `peekResolve` generated 命中 | **加** | 与 readDraftCache 口径一致 |
| `isStaleAiResolve` / `sameAsDraft`（resolve 列判旧与来源标注） | **不加** | 已回写列的 AI 解析**不因 prompt 升级判旧**，避免全量重生成；判旧仍只看 sourceHash |

### 6.3 影响评估（要向发布人说清楚）

- **writeback=0 的环境（当前生产/测试）**：所有 v2.1 shadow/generated draft 缓存失效，学员首次点击会按 v2.2 重新生成一次，之后命中新缓存。这是一次性重生成成本，量取决于已缓存题数；灰度期间用日限流与生成开关控制节奏。
- **已回写 resolve 列的题**：官方读取器先命中列文本，不重新生成，不受版本升级影响。若后续想全量刷新 v2.1 列文本，走一次性治理脚本，**不在本期范围**。

## 7. 前端改动（gjjd_appnext）

文件：`utils/resolveLoader.js`

`resolveEmptyMessage` 增加存疑分支：

```js
export function resolveEmptyMessage(reason) {
  if (reason === 'generating') return '正在生成，请稍后刷新';
  if (reason === 'answer_disputed') return '本题答案存疑，已提交复核，解析暂未提供';
  if (['rate_limited', 'cooldown', 'retry_later'].includes(reason)) {
    return '今天暂时不能生成，请稍后再试';
  }
  return '暂时生成不了，请稍后再试';
}
```

- 各展示页无需改结构：存疑返回 `source=empty`，走现有 empty 提示路径；`emptyUntil` 60s 防重复点击对存疑同样生效，60s 后重试命中后端存疑缓存，不再调 LLM。
- 不展示【存疑】全文（§2 红线）。
- 顺手可做（可选）：`disabled` 单独映射「解析功能未开启」，避免与真实生成失败混淆。不影响本期主目标。

## 8. 数据与人工复核闭环

- `ai_generation_draft.status` 新增取值 `answer_disputed`，字符串列，**无 DDL**。
- 复核队列查询（本期用 SQL，不做 admin UI）：
  ```sql
  SELECT source_id AS question_id, result_text, model_name, created_at
  FROM ai_generation_draft
  WHERE draft_type='question_resolve' AND status='answer_disputed' AND deleted=0
  ORDER BY id DESC;
  ```
- **闭环**：人工确认答案错误 → 修正题库答案（exam_exam_detail.answer）→ sourceHash 变化 → 旧 answer_disputed / generated / shadow draft 因 hash 不匹配自动失效 → 下次请求按 v2.2 重新生成正常解析。无需额外清理。
- 人工确认"答案其实正确"（模型误存疑）：**首选在管理后台人工填写该题 resolve 列（人工解析）**。官方读取器优先读 resolve 列，学员立即看到人工解析，且同题之后不再调 LLM、不再走存疑缓存；人工内容永不判旧。不推荐为了触发重生成去改题干/知识点凑 hash 变化。
- 后续可在 admin UI 加"复核状态"标记位减少手工写解析的负担，列入后续迭代，本期不实现。详细操作见附录 A。

## 9. 监控与灰度

- 存疑率口径：`answer_disputed / (generated + shadow + answer_disputed)`（按 draft 表统计，建议按天）。
- 灰度建议：测试环境开 `question_resolve_generation_enabled=1` 跑 shadow 抽样 → 观察存疑率与拒绝率 → 生产再开。
- 参考阈值（上线后按实际调整）：存疑率 > 5% 复核 prompt 门槛是否过松；> 15% 或拒绝率异常升高时，插 `question_resolve_dispute_enabled=0` 止血并回查样本。
- 关注 `forbidden_answer_tautology` 拒绝数：若偏高说明模型仍在写空话，需评估 prompt 表达。

## 10. 测试清单

### 10.1 sanitizer 单测（ResolveMarkdownSanitizerTest 增补）

1. 客观题标准存疑输出（【考点】+【存疑】含「给定答案/按原理应为」且 ≥30 字）→ disputed=true，rejectedReason=null，text 保留两行。
2. 存疑缺「按原理应为」→ reject `dispute_format_invalid`。
3. 存疑内容 <30 字 → reject `content_too_short`。
4. 主观题输出【存疑】（无【正确】）→ reject `missing_section_正确`。
5. 正常四行解析（无【存疑】）→ 行为与 v2.1 完全一致（回归）。
6. 正常解析含「与题干给出的正确答案不一致」→ reject `forbidden_answer_tautology`。
7. 正常解析含「按本题给定的正确答案」→ reject `forbidden_answer_tautology`。
8. 存疑文本含禁止内容（如 HTML/链接）→ 在分派前被 S11 拒绝。
9. 存疑文本含非法表格 → 表格整表丢弃后仍按存疑校验。
10. 存疑文本超长 → 截断后重验；截断导致【存疑】内容不足 → 拒绝。
11. 含【存疑】但同时含完整四行 → 走存疑分支，仅保留【考点】【存疑】（以 `sanitizeDisputed` 输出为准）。
12. 存疑小节顺序颠倒（【存疑】在【考点】前）→ 拒绝。

### 10.2 prompt 单测

- `userPrompt` 含「给定答案：」不含「正确答案：」；收尾含「先独立分析」。
- `systemPrompt` 含【存疑】结构与「先独立解题」；不含旧句「无法确定时，只写"与题干给出的正确答案不一致"」。
- `branchLine` 主观题含「不得输出【存疑】」。
- 既有断言 v2.1 文案/前缀的测试同步更新到 v2.2。

### 10.3 编排层测试

- 存疑生成：mock LLM 返回存疑结构 → 返回 reason=answer_disputed；断言不写 resolve 列、不写 generated/shadow、写一条 answer_disputed draft、不写失败冷却 key。
- 存疑缓存：同 sourceHash 第二次调用命中 answer_disputed draft → 不再调用 LLM（计数断言）、不计限流。
- 版本失效：构造 v2.1 前缀的 generated/shadow draft + 相同 sourceHash → v2.2 下不命中，进入重新生成。
- 列文本不判旧：resolve 列文本 == v2.1 generated draft 文本且 hash 相同 → 官方读取器仍返回列文本（isStaleAiResolve 不受版本影响）。
- dispute_enabled=0：存疑输出 → quality_rejected + 冷却。
- 主观题 LLM 返回存疑 → quality_rejected。

### 10.4 前端测试

- `resolveEmptyMessage('answer_disputed')` 返回存疑文案；其它 reason 映射不回归。
- loader 对 source=empty/reason=answer_disputed 设置 emptyUntil，60s 内不重复请求（沿用现有 empty 用例模式）。

### 10.5 回归

- 后端既有解析相关测试全绿；`mvn compile -DskipTests` 0 错。
- App 既有 resolveEntryPages / resolveLoader / syncExamResolveEntry 测试全绿（文案断言如有变化同步更新）。
- 注意：sanitizer 既有用例若把「与题干给出的正确答案不一致」当合法输入，必须改为合法表述。

## 11. 验收标准

1. 上述测试全绿，无 BOM、无真实密钥。
2. 构造"答案错误"样例（电压负反馈题，题库答案 A）：v2.2 下不再出现「按本题给定的正确答案」「与题干给出的正确答案不一致」句式；预期输出存疑或正确原理解析。
3. shadow 抽样（沿用 QuestionResolveShadowSampleRunner，40 题分层）：输出存疑数、各 rejectedReason 分布，与 v2.1 结果对比归档到验收记录；runner 第二轮零 LLM 断言需覆盖 answer_disputed 缓存命中。
4. 全链路：同一存疑题连续两次请求，第二次命中存疑缓存（无 LLM 调用）。
5. 生产发布检查表更新：v2.2 上线后先保持 generation_enabled=0，灰度再开；明确一次性重生成成本与 dispute 止血开关用法。

## 12. 执行任务拆分（建议顺序）

| 任务 | 文件 | 内容 |
|---|---|---|
| T1 | QuestionResolvePromptPolicy.java + 测试 | prompt v2.2 全文替换（§3） |
| T2 | ResolveMarkdownSanitizer.java + 测试 | disputed 分量、存疑分派与校验、S11 句式、sectionMarkerOf（§4） |
| T3 | OfficialResolveResolver.java、QuestionResolveAssistService.java + 测试 | 版本前缀升级、isCurrentPrompt、存疑生成/缓存、dispute 开关（§5、§6） |
| T4 | utils/resolveLoader.js + 测试 | 存疑文案（§7） |
| T5 | shadow runner / 验收记录 | 抽样对比、阈值与发布检查表（§9、§11） |

依赖：T1/T2 可并行，T3 依赖 T2（disputed 分量）与 T1（版本号），T4 独立，T5 最后。

## 13. 明确不做（本期）

- 不做存疑人工复核 admin UI（先用 SQL 队列 + 后台题库编辑，流程见附录 A）。
- 不做"人工确认答案正确"的独立标记位（误存疑用人工 resolve 列出口，见附录 A）。
- 不改判分逻辑、不动题库答案本身（答案修正属数据治理，由复核驱动）。
- 不全量刷新已回写 resolve 列的 v2.1 AI 解析。
- 学员端不展示存疑全文。

## 14. 上线操作与止血（实施补充，2026-10-28）

> 本节按 §11.5 要求补「生产发布检查表」，随 v2.2 代码同批交付。执行人：发布人；核对人：题库负责人。

### 14.1 上线前检查

- [ ] 后端 `mvn compile -DskipTests` 0 错；`tests` 全绿（`QuestionResolvePromptPolicyTest` / `ResolveMarkdownSanitizerTest` / `OfficialResolveResolverTest` / `QuestionResolveAssistServiceTest`）。
- [ ] 手机端 `npm test` 全绿（`tests/resolveLoader.test.js` 含 `answer_disputed` 文案用例）。
- [ ] `question_resolve_generation_enabled` 当前值确认为 **0**（生产默认关）。
- [ ] `question_resolve_dispute_enabled` **不存在即为开**（默认 1），无需建行、无 Flyway 迁移。
- [ ] 确认 `question_resolve_model_code` 指向的模型配置 `api_key` 有效（存疑分支同样走该模型）。

### 14.2 上线顺序（不可颠倒）

1. **部署后端 v2.2，保持 `question_resolve_generation_enabled=0`**。此时学员点击仍只读缓存/官方解析，不产生新生成，零风险。
2. **测试环境先开生成做 shadow 抽样**：`question_resolve_generation_enabled=1` + `question_resolve_writeback_enabled=0`，用 `QuestionResolveShadowSampleRunner` 跑 40 题分层样本，记录：存疑数、各 `reason` 分布、存疑率。
3. **判读阈值**（§9）：存疑率 `answer_disputed / (shadow + disputed)` 落在 **0.5%~5%** 为正常；> 5% 说明门槛偏松，先复核样本再决定是否收紧 prompt；> 15% 或 `quality_rejected` 明显上升 → 按 14.4 止血。
4. **生产灰度**：先开 `generation_enabled=1`（writeback 仍 0，只写 shadow / answer_disputed，不改库），观察 1~2 天存疑率与学员侧文案反馈；稳定后再按原计划决定是否开 writeback。
5. **一次性重生成成本**：v2.2 版本前缀与 v2.1 不同，**所有未落列的 v2.1 generated/shadow draft 立即失效**，同题首次点击会重新生成一次后命中新缓存。量 = 已缓存题数；用 `question_resolve_daily_limit`（当前 20/人/天）与生成开关控制节奏。已回写 `resolve` 列的题不受影响（官方读取器先读列文本，`isStaleAiResolve` 不因版本升级判旧）。

### 14.3 复核闭环（每日或灰度期每天）

```sql
-- 存疑工单队列
SELECT source_id AS question_id, result_text, model_name, created_at
FROM ai_generation_draft
WHERE draft_type='question_resolve' AND status='answer_disputed' AND deleted=0
ORDER BY id DESC;
```

处置口径见附录 A（答案错 → 改答案；答案对 → 人工填 `resolve` 列；题干错 → 改题干）。三种处置都会改变 `sourceHash`，旧 draft 自动失效，无需手工清理。

### 14.4 止血开关（出问题第一时间执行）

| 症状 | 动作 | 效果 |
|---|---|---|
| 存疑率异常升高 / 学员反馈"答案存疑"过多 | 向 `sys_dict`（tenant 10159）插入或更新 `question_resolve_dispute_enabled=0` | 存疑输出按质量门拒绝处理（`quality_rejected` + 24h 冷却），不再产生新存疑工单；已存在的存疑缓存仍命中，学员继续看到中性提示 |
| 生成质量整体异常（拒绝率高、乱码） | `question_resolve_generation_enabled=0` | 停止一切新生成，只读既有缓存与官方解析 |
| 需要立即回退解析质量 | 回滚代码到 v2.1 | v2.1 draft 重新命中（版本前缀匹配），旧解析立刻恢复；v2.2 期间新生成的 draft 在 v2.1 下不命中，会被重新生成一次 |

> 止血开关都是字典行操作，**改完立即生效**（服务每次调用实时读字典，无本地缓存），无需重启。

### 14.5 上线后观察项

- `[题目解析] 答案存疑` 日志条数（按天），与 14.3 队列条数应一致。
- `[题目解析] 质量门拒绝` 中 `reason=forbidden_answer_tautology` 的条数：偏高说明模型仍在写"以答案为理由"的空话，需回看 prompt 表达，而不是直接关存疑。
- 学员端文案：出现"本题答案存疑，已提交复核，解析暂未提供"的题目，抽查其 `result_text` 是否确实指出了具体矛盾（否则属误存疑，走附录 A 情形 2）。

## 15. 实施验证记录（2026-10-28，本机对测试库 gjjddb2026 实跑）

> 代码位置：后端 `services/platform-api`（4 个主类 + 4 个测试类 + shadow runner）；手机端 `gjjd_appnext/utils/resolveLoader.js`。
> 环境：测试库 114.132.67.185:3309 / 本机 Redis / 模型 `theory-gen-glm`（glm-5.3-flash，thinking=enabled）。全程 `writeback=0`。

### 15.1 单测

| 测试类 | 用例数 | 结果 |
|---|---|---|
| `QuestionResolvePromptPolicyTest` | 11 | 全绿 |
| `ResolveMarkdownSanitizerTest` | 42（新增 12 条 v2.2 用例） | 全绿 |
| `OfficialResolveResolverTest` | 10（新增 `isCurrentPrompt` / 版本不判旧 2 条） | 全绿 |
| `QuestionResolveAssistServiceTest` | 29（新增 5 条存疑用例） | 全绿 |
| 手机端 `tests/*.test.js` | 148 passed / 2 todo（新增 4 条 `answer_disputed` 用例） | 全绿 |

### 15.2 §11.2 答案错误样例（真实链路，题目 id=990000001，答案 A）

v2.2 输出（`ai_generation_draft.status=answer_disputed`，`prompt=question_resolve_v2.2#…`）：

```
【考点】电压负反馈补偿
【存疑】给定答案：A。按原理应为：C。电压负反馈从电枢两端取反馈电压。电网电压波动会改变整流输出电压，
反馈电压随之变化，系统经调节使输出电压回稳。所以电压负反馈对电网电压变化有补偿能力。负载变化引起的
电枢电阻压降在反馈取样点之内侧，反馈电压检测不到。所以电压负反馈对负载变化无补偿能力。故应为"对前者
有补偿能力，对后者无"。
```

- ✅ 未出现「按本题给定的正确答案」「与题干给出的正确答案不一致」
- ✅ 指出具体矛盾（取样点位置）+ 原理依据（端电压反馈 vs 电枢压降不可测）
- ✅ 不进 resolve 列、不进失败冷却；同 hash 第二次请求命中 `answer_disputed` 缓存，第二轮 LLM 调用 = 0

### 15.3 §11.3 shadow 抽样（40 题 8 层，真实模型）

```
[summary] total=40 llmCalls=26 shadow=26 disputed=3 rejected=3 officialOnly=2 secondRoundLlmCalls=0
[dispute-rate] disputed=3 generatedLike=29 rate=10.34%
[reason-distribution] {draft=6, shadow=26, empty:answer_disputed=3,
                      empty:official_only=2, empty:quality_rejected=3}
prompt 版本分布：v2.2=29, v2.2-rejected=3, v2.1=6（这 6 道 resolve 列已有 v2.1 文本，官方读取器优先读列，未重新生成）
```

- ✅ 第二轮全量零 LLM（含 3 道存疑题命中 `answer_disputed` 缓存），满足 §11.3 第 3 条硬检查。
- ⚠️ 存疑率 10.34% 高于 §9 的 5% 参考阈值，但**3 条里有 2 条是占位题干**（id=22「题目1」+「答案1」选项、id=23 同类），模型给的是"题干无任何条件，无法用原理解释，建议补全"，属**输入质量问题而非答案错误**。
  剔除这 2 条后有效存疑率 = 1/29 = **3.45%**，落在正常区间。
- 待业务复核的 1 条：id=332028（多选，给定 A、C、D，模型主张含 B「崩料」，理由是炼铁教材把崩料列为常见炉况失常）。这条是真实信号，需按附录 A 情形 1/2 判定。

### 15.4 §11.3 rejectedReason 分布（3 条，均为真实质量缺陷，非误杀）

| 题号 | 原始输出特征 | 拒绝原因 |
|---|---|---|
| 57 | 「题干只显示'第二题'…不能输出解析」，无任何【】小节 | `missing_section_考点`（S8） |
| 332023 | 四行完整但只有【考点】【正确】【记法】，缺【干扰】 | `missing_section_干扰`（S8） |
| 122723 | 主观题【正确】以「答案要点一：」开头，非「答案要点：」 | `answer_prefix_mismatch`（S9） |

### 15.5 遗留观察（不在本期范围，建议下期）

占位题干（"题目1"/"答案1"）不会被输入资格拦截，会一路走到存疑或质量拒绝，向复核队列注入噪声。
建议按 §B.2 第 4 条，下期复用 JEV 的 `QuestionContentQualityInspector` + 媒体题检测，在 `inputFactory.build` 后拦成 `invalid_input`，不给坏题生成解析。

## 附录 A：存疑复核操作手册（给复核人）

### A.1 复核频率与队列

建议每个工作日查一次存疑队列（或灰度期每天、稳定后每周）：

```sql
SELECT source_id AS question_id, result_text, model_name, created_at
FROM ai_generation_draft
WHERE draft_type='question_resolve' AND status='answer_disputed' AND deleted=0
ORDER BY id DESC;
```

`result_text` 里包含【考点】与【存疑】，【存疑】给出「给定答案：X」「按原理应为：Y」及模型的矛盾理由。`question_id` 即题库题目 id。

### A.2 复核步骤

1. **看模型理由**：先读【存疑】是否指出了"题干条件与给定答案的具体矛盾 + 原理依据"。若只是泛泛质疑、没有理由，大概率为误存疑。
2. **对题核对**：到管理后台题库（公司题库/题库管理）按 question_id 找到该题，对照题干、选项、给定答案，结合教材/标准判断谁对谁错。复核人须以教材为依据，不能只信模型。
3. **按三种情形处置**（见 A.3）。
4. **验证**：处置后用测试学员号在该题答错 → 点「查看解析」，确认展示符合预期（正常解析或人工解析）。

### A.3 三种处置情形

**情形 1：题库答案确实错误（模型存疑成立）**

- 在后台题库编辑该题，**改正答案**（客观题 answer 按平台标准格式：JSON 选项数组 + isCorrect 标记，或旧格式字母；与现有同题型题保持一致）。
- 改正后 sourceHash 变化，旧存疑/生成 draft 自动失效；下次学员点击会按 v2.2 重新生成正常解析。
- **必须同时检查 resolve 列**：若该题已有旧解析（人工或 AI），它不会因改答案自动更新——旧解析可能已与新答案矛盾。请一并改写或清空 resolve 列（清空后才会触发重新生成；列非空时官方读取器永远优先用列文本）。
- 注意：改答案影响判分，属于数据治理操作，须确认教材依据后再改。

**情形 2：题库答案正确（模型误存疑）**

- 不要改答案。
- 在后台题库编辑该题，**把正确解析人工填进 resolve 列**（可按平台四行格式，也可写简洁人工解析）。
- 效果：官方读取器优先读 resolve 列，学员立即看到人工解析；该题之后不再调 LLM、不再走存疑缓存，人工内容永不判旧。这是本期误存疑的标准出口，无需等 admin UI。
- 若暂不填写解析，该题学员端会持续显示「答案存疑，已提交复核」提示（同 hash 命中存疑缓存，不重复调 LLM，无额外成本）。

**情形 3：题干或选项本身有误（不是答案问题）**

- 在后台修正题干/选项文本，sourceHash 同样变化，旧 draft 自动失效，下次点击重新生成。
- 同样检查并同步处理 resolve 列旧解析。

### A.4 复核记录建议

- 情形 1/3 的改题属于正式数据修正，建议保留修改依据（教材页码/标准条款）在题目备注或部门台账。
- 误存疑率偏高时（某工种/知识点集中出现），反馈给 prompt 维护人：可能是该领域表述让模型频繁"过度质疑"，需评估是否调高存疑门槛或补充领域约束。

## 附录 B：与《客观题答案AI检查_一期实施方案》的复用与对齐分析

> 关联文档：`docs/04_requirements/客观题答案AI检查_一期实施方案.md`（下称"JEV 检查方案"）。
> 两者是同一"题库答案质量治理"的两条链：本方案是**在线、单题、学员触发、生成式 LLM**；
> JEV 检查方案是**离线、批量、管理员触发、决策模型 JEV**。核心方法一致：AI 独立作答（盲答）→
> 与原答案比对 → 疑似错误进人工复核，AI 一律不写回题库。

### B.1 可复用清单

| 复用点 | 本方案（v2.2 存疑） | JEV 检查方案 | 复用方式与时机 |
|---|---|---|---|
| 独立作答（盲答）原则 | 先独立解题再对照给定答案（指令级软盲答） | 物理上不发送原答案/原解析（数据级硬盲答） | 统一语言与文档互相引用；实现各保留，不强行同构 |
| 疑似答案错误信号 | `answer_disputed` draft（无置信分层） | `mismatch_high` / `low_confidence`（有置信分层） | 统一映射为"疑似原答案错误"；统一复核工作台按 detail_id 聚合两个来源 |
| sourceHash 变更检测 | 五段 SHA-256，draft 失效 + 列判旧 | type/content/answer 三段长度前缀 SHA-256，快照"题库已变更"提示 | **语义复用**；口径各自保留（见 B.3 禁止改本方案算法） |
| 复核 SOP 与状态语言 | 附录 A 三情形处置 | 四态 `confirmed_correct/needs_edit/deferred/unreviewed` + 乐观锁 | **统一采用 JEV 四态语言**；本方案附录 A 的"答案正确（误存疑）"对应 `confirmed_correct`，"答案错/题干错"对应 `needs_edit`，暂不处理对应 `deferred` |
| 人工改题出口 | 后台题库编辑页改答案/题干、人工填 resolve 列 | 同样在既有题库编辑页修正，功能不写回 | 完全一致的 SOP；JEV §14.4.5 可直接引用附录 A |
| 题干/选项纯文本化 | `QuestionResolveInputFactory.stripHtml` | Jsoup 纯文本 + NBSP/空白规范化 | 下沉共用 `exam.support` 文本规范化工具，两个工厂各自调用，禁止第三份实现 |
| 媒体题拦截 | 当前未拦截（图片题干 strip 后可能丢信息仍生成） | §8.2/§8.3 明确 `MEDIA_UNSUPPORTED` 不调 AI | **本方案应复用 JEV 媒体检测规则**：含 img/data:image/audio/video → `invalid_input` 不生成解析 |
| 内容质量检查 | 无 | `QuestionContentQualityInspector`（导入预检 + AI 批次共用） | 后续增强：本方案 `inputFactory.build` 后复用同一 inspector，`contentQuality=invalid` 不生成解析，避免给坏题生成自洽假解析 |
| 标定集 | shadow 40 题抽样 | §17 200 道专家真值（150 正确 + 30 错误 + 20 边界） | **直接共用**：JEV 的 30 道"人工确认原答案错误"题就是本方案存疑功能的检出率测试集；150 道正确题测误存疑率 |
| 菜单/权限种子模式 | 若后续做存疑复核 UI | §12.5 tenant 10159 三步链、INSERT SELECT WHERE NOT EXISTS | 模式复用，挂同一"资源管理"或题库菜单 |
| 模型调用审计字段 | draft 仅 model_name | call_log 含 requested/response model、confidenceDefinition、prompt_sha256、token、耗时 | 后续增强：本方案 draft 或独立审计补齐 response model/token，口径向 JEV 看齐 |
| 防重/防晚到 | Redis 题级锁 + 失败冷却 + 学员日限流 | run_id 租约 + claimAttempt fencing | 机制不同**不共用代码**；语义一致，各自保留 |

### B.2 落地协同建议（按价值排序）

1. **复核体系统一**：存疑复核与 JEV 复核使用同一四态语言与同一 SOP（AI 只筛查，人工在题库编辑页修正）。
   后续若做统一"答案复核工作台"，按 `detail_id` 聚合 `answer_disputed` draft 与 JEV item 的
   `needsReview` 行，用查询/视图聚合，**不合并物理表**。
2. **标定集共用**：JEV §17 的 200 道标定集同时作为本方案存疑功能的质量验收集——
   30 道错误题测"存疑检出率"，150 道正确题测"误存疑率"，指标口径可与 JEV §17.2 的
   "已知错误检出率/高置信一致人工正确率"并列记录，避免另造一套标定。
3. **免费联动**：JEV 复核标记 `needs_edit` → 人工在题库编辑页改题 → sourceHash 变化 →
   本方案旧 answer_disputed/generated/shadow draft 自动失效并重新生成解析，无需额外同步代码。
   反向：已产生 `answer_disputed` 的 question_id 列表可作为 JEV 批次 `selectionMode=ids` 的优先输入，
   用 JEV 的置信度分层帮助复核人排序（在线存疑只有信号、没有 confidence）。
4. **本方案输入侧增强（后续迭代）**：复用 JEV 的媒体检测与 `QuestionContentQualityInspector`，
   在解析生成前拦截媒体题与内容质量 invalid 题，减少"坏题生成假解析"。

### B.3 明确不共用 / 禁止的"统一"

- **模型客户端不共用**：生成式 `FaqLlmClient` 与 `JevDecisionClient` 协议不同；JEV 方案禁止把
  生成式 LLM 当第二模型，本方案也不能用 JEV 输出代替解析文本。
- **结果表不合并**：JEV 三表（batch/item/call_log）是批量快照与审计，本方案在线单题无批次，
  继续使用 `ai_generation_draft`；禁止把批量初筛结果塞进 draft 表，也禁止把在线存疑塞进 JEV item 表。
- **质量门不共用**：`ResolveMarkdownSanitizer` 四行硬校验只服务生成文本；JEV 不输出文字理由，
  不得复用该 sanitizer。
- **解析器不互换**：JEV 的 `ObjectiveQuestionParser` 严格暴露异常，本方案 `QuestionResolveInputFactory`
  需兼容历史题型与主观题；二者都不得改用 `FollowTaskBankQuestionConverter` 做严格预检（其兜底会掩盖异常）。
- **不改本方案已上线 sourceHash 算法**：段定义与编码方式变更会使全部 draft 缓存失效并触发 resolve 列判旧；
  JEV 侧可复用"长度前缀防 NULL/空串碰撞"的编码思路，但不得回头改本方案算法。
- **AI 不写回题库**：两边一致禁止自动/一键写回 `exam_exam_detail.answer`；人工修正只走既有题库编辑页。
