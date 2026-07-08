# 试题库 → FAQ 知识库转换方案

> 目标：将 `exam_exam_detail` 中的试题转换为 `faq_example.json` 格式的标准问/相似问/反例结构，导入 WeKnora 知识库，提升 FAQ 问答精准匹配能力。

---

## 一、总体流程

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           转换流水线                                      │
│                                                                         │
│  exam_exam_detail 表                                                    │
│       │                                                                 │
│       ▼                                                                 │
│  ① 筛选与清洗 ──► 过滤有效试题（single/judge/multi），去除 HTML 标签     │
│       │                                                                 │
│       ▼                                                                 │
│  ② 知识点关联 ──► 通过 knowledge_id 查知识点表，获取层级路径            │
│       │                                                                 │
│       ▼                                                                 │
│  ③ 规则生成 ──► 按题型生成 standard_question / answers / tag_name       │
│       │                                                                 │
│       ▼                                                                 │
│  ④ LLM 增强 ──► 调大模型生成 similar_questions / negative_questions     │
│       │                                                                 │
│       ▼                                                                 │
│  ⑤ 输出 ──► faq_example.json（按 tag_name 分组的可导入文件）            │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 二、输入筛选规则

只转换以下题型，其余跳过：

| type 值 | 含义 | 是否转换 |
|---|---|---|
| `single` | 单选题 | ✅ |
| `multi` | 多选题 | ✅ |
| `judge` | 判断题 | ✅ |
| `fill` | 填空题 | ❌ 留空值，不适配 FAQ |
| `essay` / `jianda` | 简答/论述题 | ❌ 答案过长，不适配 FAQ |

**数据库过滤条件：**

```sql
WHERE deleted = 0
  AND check_status = 1
  AND status = 1
  AND type IN ('single', 'multi', 'judge')
```

---

## 三、知识点关联逻辑（tag_name 知识层级来源）

### 3.1 关联路径

```
exam_exam_detail.knowledge_id
        │
        ▼
exam_knowledge_point.id  ──►  exam_knowledge_point.parent_id
        │                            │
        │                            ▼
        │                    递归向上查父级知识点
        │                            │
        │                            ▼
        │                    拼接路径: 父级名称-子级名称-当前名称
        │
        ▼
最终 tag_name 知识层级 = 知识点路径的末三级（或按 depth 截断）
```

### 3.2 SQL 示意

```sql
-- 递归查知识点层级路径（MySQL 8+ CTE）
WITH RECURSIVE kp_path AS (
  SELECT id, parent_id, name, 1 AS depth,
         CAST(name AS CHAR(500)) AS path
  FROM exam_knowledge_point
  WHERE parent_id = 0
  UNION ALL
  SELECT c.id, c.parent_id, c.name, p.depth + 1,
         CONCAT(p.path, '-', c.name)
  FROM exam_knowledge_point c
  JOIN kp_path p ON c.parent_id = p.id
)
SELECT id, name, path, depth FROM kp_path;
```

### 3.3 截断规则

| 知识点深度 | tag_name 知识层级取值 |
|---|---|
| depth ≤ 1 | 取当前名称 |
| depth = 2 | 取 `父-当前` |
| depth ≥ 3 | 取末三级 `祖父-父-当前` |
| 无关联知识点 | 回退到 keyword 字段（按 `\|` 分割取第一个） |

---

## 四、输入 → 输出字段映射

### 4.1 输入结构（exam_exam_detail 单条）

```json
{
  "id": 10063,
  "type": "single",
  "content": "<p>常用的顺流式罐式煅烧炉有多少组？（  ）</p>",
  "answer": "[{\"content\":5,\"isCorrect\":false,\"score\":0},{\"content\":6,\"isCorrect\":true,\"score\":1},{\"content\":7,\"isCorrect\":false,\"score\":0},{\"content\":8,\"isCorrect\":false,\"score\":0}]",
  "keyword": "煅烧炉|碳素工艺",
  "difficulty": 2,
  "knowledge_id": 203,
  "score": 1.0,
  "resolve": "顺流式罐式煅烧炉由6个罐组成"
}
```

### 4.2 输出结构（FAQ 条目）

```json
{
  "standard_question": "常用的顺流式罐式煅烧炉有多少组？",
  "answers": [
    "常用的顺流式罐式煅烧炉有6组。",
    "解析：顺流式罐式煅烧炉由6个罐组成。"
  ],
  "similar_questions": [
    "顺流式罐式煅烧炉一共有几组？",
    "一台顺流式罐式煅烧炉包含多少组？",
    "顺流式罐式煅烧炉的罐组数是多少？"
  ],
  "negative_questions": [
    "逆流式罐式煅烧炉有多少组？（不同炉型，不能混淆）",
    "回转窑由多少个罐组成？（设备选型错误）",
    "顺流式罐式煅烧炉的煅烧温度是多少？（答非所问）"
  ],
  "tag_name": "单选题|碳素工艺-煅烧炉-顺流罐式|中等"
}
```

---

## 五、分题型转换逻辑

### 5.1 单选题（type = 'single'）

**输入示例：**

```
content: "“工匠精神”的基础是"
answer:  [{"content":"精益求精","isCorrect":false},
          {"content":"爱岗敬业","isCorrect":true},
          {"content":"脚踏实地","isCorrect":false},
          {"content":"推陈出新","isCorrect":false}]
```

**字段生成规则：**

| FAQ 字段 | 生成逻辑 |
|---|---|
| standard_question | 题干转问句：陈述句补"是什么？"，已是问句则保留 |
| answers | ① 正确答案转陈述："爱岗敬业是工匠精神的基础。" ② 干扰项否定："精益求精、脚踏实地、推陈出新均不是工匠精神的基础。" ③ 拼接 resolve（若有）：`resolve` 原文 |
| similar_questions | **走 LLM 生成 3 条**，要求：同义改写，保留主题词+属性词两个锚点 |
| negative_questions | **走 LLM 生成 3 条**，要求：保留主题词，替换属性词为同概念的其他维度 |
| tag_name | `{题型}|{知识点路径末三级}|{难度}` |

**输出示例：**

```json
{
  "standard_question": "\"工匠精神\"的基础是什么？",
  "answers": [
    "爱岗敬业是工匠精神的基础。",
    "精益求精是最高追求，脚踏实地和推陈出新是实践延伸，均非基础。"
  ],
  "similar_questions": [
    "工匠精神的基础是指什么？",
    "以下哪项是工匠精神的基础？",
    "工匠精神的基础在于什么？"
  ],
  "negative_questions": [
    "工匠精神的最高境界是什么？（答案应为精益求精）",
    "工匠精神的创新要求是什么？（答案应为推陈出新）",
    "工匠精神的实践体现是什么？（答案应包含脚踏实地）"
  ],
  "tag_name": "单选题|职业道德-工匠精神-基础"
}
```

---

### 5.2 判断题（type = 'judge'）

**输入示例：**

```
content: "（   ）碳素企业目前采用的煅烧炉是顺流式罐式煅烧炉。"
answer:  [{"content":"对","isCorrect":true,"score":1},
          {"content":"错","isCorrect":false,"score":0}]
```

**字段生成规则：**

| FAQ 字段 | 生成逻辑 |
|---|---|
| standard_question | 题干补问号："碳素企业目前采用的煅烧炉是顺流式罐式煅烧炉吗？" |
| answers | 根据 `isCorrect=true` 的选项决定：若"对"答肯定句 + resolve；若"错"答事实纠正 + resolve |
| similar_questions | **走 LLM 生成 3 条**：替换主语、替换句式（"是否"/"对吗"/"正确吗"）、替换范围词 |
| negative_questions | **走 LLM 生成 3 条**：替换关键实体为相近炉型（逆流式/回转窑/艾奇逊炉）、替换限定条件 |
| tag_name | `{题型}|{知识点路径末三级}|{难度}` |

**输出示例：**

```json
{
  "standard_question": "碳素企业目前采用的煅烧炉是顺流式罐式煅烧炉吗？",
  "answers": [
    "是的，我国碳素企业目前普遍采用顺流式罐式煅烧炉。",
    "解析：因罐式煅烧炉环评达标、投资适中而被广泛采用。"
  ],
  "similar_questions": [
    "我国碳素行业主要使用顺流式罐式煅烧炉对吗？",
    "目前大多数碳素企业的煅烧炉是否为顺流式罐式？",
    "顺流式罐式煅烧炉是碳素企业的主要选择吗？"
  ],
  "negative_questions": [
    "碳素企业目前采用的是逆流式罐式煅烧炉吗？（目前我国普遍是顺流式）",
    "艾奇逊炉是碳素企业目前的主流煅烧设备吗？（主流仍为顺流式罐式）",
    "回转窑是主流煅烧设备吗？（回转窑用于其他工段）"
  ],
  "tag_name": "判断题|碳素工艺-煅烧炉-炉型识别"
}
```

---

### 5.3 多选题（type = 'multi'）

**输入示例：**

```
content: "目前的煅烧炉类型主要有（ ?）。"
answer:  [{"content":"罐式煅烧炉","isCorrect":true,"score":0.2},
          {"content":"回转窑煅烧炉","isCorrect":true,"score":0.2},
          {"content":"电热煅烧炉","isCorrect":true,"score":0.2},
          {"content":"焦炉","isCorrect":true,"score":0.2},
          {"content":"回转床煅烧炉","isCorrect":true,"score":0.2}]
```

**字段生成规则：**

| FAQ 字段 | 生成逻辑 |
|---|---|
| standard_question | 题干改疑问句："目前的煅烧炉类型主要有哪些？" |
| answers | 枚举所有 isCorrect=true 的选项名称，补充每个选项的适用场景，拼接 resolve |
| similar_questions | **走 LLM 生成 3 条**：替换枚举词（"主要有哪些"→"包括哪些"→"常见类型有"），保留"煅烧炉类型"锚点 |
| negative_questions | **走 LLM 生成 3 条**：替换主体概念为相近热源设备（"锅炉"/"熔炼炉"）、替换工段（"原料制备"/"冷却"） |
| tag_name | `{题型}|{知识点路径末三级}|{难度}` |

**输出示例：**

```json
{
  "standard_question": "目前的煅烧炉类型主要有哪些？",
  "answers": [
    "主要有五种：罐式煅烧炉、回转窑煅烧炉、电热煅烧炉、焦炉和回转床煅烧炉。",
    "罐式煅烧炉适用于大规模连续生产；回转窑多见于新型项目；电热煅烧炉用于高纯材料。"
  ],
  "similar_questions": [
    "常见的煅烧炉类型包括哪些？",
    "目前工业上主要使用哪些类型的煅烧炉？",
    "煅烧炉可以分为哪几类？"
  ],
  "negative_questions": [
    "常见的原料破碎机类型有哪些？（工段不同，非煅烧炉）",
    "碳素企业的主要冷却设备有哪些？（冷却工段设备，非煅烧）",
    "锅炉的常见类型有哪些？（相近热源设备，非煅烧炉）"
  ],
  "tag_name": "多选题|碳素工艺-煅烧炉-炉型分类"
}
```

---

## 六、LLM 生成 similar/negative_questions 的 Prompt 设计

### 6.1 生成 similar_questions 的 Prompt

```text
你是一个试题转 FAQ 的助手。根据以下题目信息，生成 3 条「相似问」。

## 题目信息
- 题型：{single|multi|judge}
- 题干：{content_stripped}
- 正确选项：{correct_options}
- 干扰项：{wrong_options}

## 生成要求
1. 相似问必须与原题答案一致，语义等价
2. 保留题干中的核心概念（名词、限定词）不变
3. 通过替换疑问词、同义改写、调整句式来生成多样性问法
4. 不要生成需要额外推理或引入新知识才能回答的问法
5. 输出为 JSON 数组，每条为纯文本字符串，不要带引号以外的标记

## 输出格式
{"similar_questions": ["问法1", "问法2", "问法3"]}
```

### 6.2 生成 negative_questions 的 Prompt

```text
你是一个试题转 FAQ 的助手。根据以下题目信息，生成 3 条「反例」。

## 题目信息
- 题型：{single|multi|judge}
- 题干：{content_stripped}
- 正确选项：{correct_options}
- 干扰项：{wrong_options}
- 题目考查的关键概念：{key_concepts_from_keyword}

## 生成要求
1. 反例必须与原题「看起来相似但答案不同」
2. 用以下策略生成反例（每条使用不同策略）：
   - 属性替换：保留主题词，把属性词替换为同主题的其他属性（如把"基础"换成"最高要求"/"创新体现"）
   - 实体替换：保留句式结构，把核心实体换成同领域的其他实体
   - 工段/场景替换：把题干中的工段/场景换成相近但不同的工段/场景
   - 答非所问：保留相关词，但答案需要完全不同的知识点
3. 每条反例后用括号标注「应匹配的正确答案」，格式：（答案应为 XXX）
4. 输出为 JSON 数组，每条为纯文本字符串

## 输出格式
{"negative_questions": ["反例1（答案应为XXX）", "反例2（答案应为XXX）", "反例3（答案应为XXX）"]}
```

### 6.3 LLM 调用参数

| 参数 | 取值 |
|---|---|
| base_url | **待定**（用户选择后填入） |
| api_key | **待定**（用户选择后填入） |
| model | **待定** |
| temperature | 0.7 |
| response_format | `{"type": "json_object"}` |
| max_tokens | 2000 |

### 6.4 调用策略

- **并发控制**：每批最多 20 条试题，调 2 次 LLM（一次 similar、一次 negative）
- **重试机制**：解析失败最多重试 2 次，仍失败则记录错误日志并跳过该条
- **超时**：单次调用 30s 超时
- **限流**：如果 LLM 有 RPM 限制，加 `time.sleep` 控制请求间隔（可配置）

---

## 七、tag_name 命名规范

格式：`{题型}|{知识点路径}|{难度}`

| 字段 | 来源 | 映射规则 |
|---|---|---|
| 题型 | `type` 字段 | `single`→`单选题`，`multi`→`多选题`，`judge`→`判断题` |
| 知识点路径 | `knowledge_id` → 知识点表递归查父级 | 取末三级，用 `-` 连接 |
| 难度 | `difficulty` 字段 | 0-1→`简单`，2→`中等`，3-4→`困难` |

**示例：**

```
单选题|碳素工艺-煅烧炉-顺流罐式|中等
多选题|职业道德-工匠精神-表现形式|简单
判断题|安全规范-高空作业-基本要求|困难
```

---

## 八、边界与特殊处理

| 情况 | 处理方式 |
|---|---|
| content 含 HTML 标签 | 用 Jsoup 或正则去除 `<p><br><strong>` 等标签，保留纯文本 |
| answer 字段解析失败 | 跳过该条，记入 `error_log.txt` |
| isCorrect=true 的选项为空 | 跳过该条，记入 `error_log.txt` |
| isCorrect=true 的选项不止一个 | 按多选题处理 |
| resolve 为空 | answers 不拼接解析，仅输出选项说明 |
| 题干含填空下划线 | 判断题保留；选择题保留括号；填空题直接跳过 |
| knowledge_id 无对应知识点 | 回退到 keyword 字段（按 `\|` 分割取第一个）作为 tag 知识层级 |
| keyword 也为空 | tag 知识层级填 `未分类` |
| 多选题单选项分值为 0 | 可能是全选，按全选处理 |
| LLM 返回非合法 JSON | 重试 2 次，仍失败则记录错误并跳过 |

---

## 九、输出文件组织

按 `tag_name` 一级分类输出，方便分批次导入 WeKnora：

```
output/
  faq_单选题-碳素工艺-煅烧炉.json       ← 同一 tag 的 FAQ 集合
  faq_判断题-职业道德-工匠精神.json
  faq_多选题-安全规范-高空作业.json
  error_log.txt                        ← 解析失败/跳过的试题清单
  stats.json                           ← 转换统计（成功/失败/各题型数量）
```

**stats.json 示例：**

```json
{
  "total_input": 1200,
  "success": 1150,
  "skipped_fill": 30,
  "skipped_essay": 15,
  "parse_error": 5,
  "by_type": {
    "single": 600,
    "multi": 300,
    "judge": 250
  },
  "by_tag": {
    "单选题|碳素工艺-煅烧炉": 200,
    "判断题|职业道德-工匠精神": 150
  }
}
```

---

## 十、模块划分（待实现时参考）

| 层级 | 模块 | 职责 |
|---|---|---|
| Controller | `FaqGenerationController` | REST 入口（`POST /exam/faq/generate`），`@PreAuthorize` 鉴权 |
| Application | `FaqGenerationApplicationService` | 编排：读题 → 调 LLM → 组装 FAQ → 输出 JSON，记审计日志 |
| Domain | `ExamFaqConverter` | 按题型生成 standard_question / answers / tag_name |
| Infrastructure | `LlmClient` | 通用 OpenAI 兼容 API 调用（RestClient） |
| Infrastructure | `KnowledgePointPathResolver` | 通过 knowledge_id 递归查知识点路径 |
| Config | `application.yml` | `ai.llm.base-url` / `ai.llm.api-key` / `ai.llm.model` |

---

## 十一、待确认项

| 项 | 状态 |
|---|---|
| LLM 服务商选择 | **待定**（DeepSeek / OpenAI / 通义 / 其他） |
| LLM base_url | **待定** |
| LLM api_key | **待定** |
| LLM model 名称 | **待定时可给推荐值** |
| 是否支持并发批处理 | 默认支持，并发数可配置 |
| FAQ 导入 WeKnora 方式 | 通过接口写入 or 文件批量导入 WeKnora 后台 |

---

*方案版本：v1.0 | 日期：2026-07-06 | 状态：待用户确认后落地实现*
