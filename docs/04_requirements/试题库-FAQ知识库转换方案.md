# 试题库 → FAQ 知识库转换方案

> 目标：按工种维度将 `exam_exam_detail` 中的试题转换为结构化 FAQ（标准问/相似问/反例），入库可追踪转换状态，导入 WeKnora 知识库，提升 FAQ 问答精准匹配能力。
>
> 模型角色：以「国家职业标准」专家视角执行转换，能结合国家标准中的职业定义、技能要求、鉴定规范来生成高质量 FAQ。

---

## 一、表关系澄清（必读）

现有系统中不存在 `exam_knowledge_point` 和 `exam_exam_subject` 两张表，正确的关系链如下：

```
exam_work_type（工种表）
    │  id (INT)
    │  parent_id（树形，支持多级工种分类）
    │  title / series_no（职业编码） / levels_no（级别）
    │
    │  work_type_id
    ▼
exam_course（科目表）
    │  id (INT)
    │  work_type_id → exam_work_type.id
    │  title / level（0=初级~4=高级技师）/ series_no（科目编码）
    │
    │  course_id
    ▼
exam_knowledge_structure（知识点/鉴定范围 4级树）
    │  id (INT)
    │  course_id → exam_course.id
    │  parent_id（自引用，0=根节点(level=0一级鉴定范围)）
    │  level（0=一级鉴定范围 1=二级 2=三级 3=鉴定点）
    │  type（0=理论 1=实操）/ title / important
    │
    │  knowledge_id
    ▼
exam_exam_detail（试题表）
    │  id (INT)
    │  knowledge_id → exam_knowledge_structure.id
    │  type（single/multi/judge/essay/...）/ content / answer(JSON)
    │  difficulty(0~4) / keyword / resolve / score
    │  check_status / final_check_status
```

**关键纠正：**
- `exam_knowledge_point` 不存在 → 实际知识点表 = `exam_knowledge_structure`
- `exam_exam_subject` 不存在 → 实际科目表 = `exam_course`
- 知识点层级路径通过 `parent_id` 递归拼接（level 0~3），不需要 `knowledge_id` 以外的字段

---

## 二、新增表设计

### 2.1 `exam_faq_conversion`（FAQ 转换记录表）

> 用途：记录每条试题的结构化 FAQ 转换结果，跟踪转换状态。

```sql
CREATE TABLE exam_faq_conversion (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,

  -- 溯源关联
  exam_id INT NULL COMMENT '来源试题 exam_exam_detail.id',
  work_type_id INT NOT NULL COMMENT '工种ID exam_work_type.id',
  course_id INT NULL COMMENT '科目ID exam_course.id',
  knowledge_id INT NULL COMMENT '知识点ID exam_knowledge_structure.id',

  -- 原始试题快照（脱敏快照，防止题目删除后无法回溯）
  source_type VARCHAR(50) NULL COMMENT '题型: single/multi/judge',
  source_difficulty INT NULL COMMENT '难度',
  source_content TEXT NULL COMMENT '原始题干',
  source_answer TEXT NULL COMMENT '原始答案JSON',

  -- 结构化 FAQ 字段
  standard_question TEXT NOT NULL COMMENT '标准问',
  answers JSON NOT NULL COMMENT '答案数组（含正确答案、干扰项说明、解析）',
  similar_questions JSON NULL COMMENT '相似问数组（LLM生成）',
  negative_questions JSON NULL COMMENT '反例数组（LLM生成）',
  tag_name VARCHAR(500) NULL COMMENT '标签路径：题型|知识点末三级|难度',

  -- 转换元数据
  conversion_status VARCHAR(32) NOT NULL DEFAULT 'pending'
    COMMENT '转换状态: pending=待转换 / success=成功 / failed=失败 / skipped=跳过',
  model_code VARCHAR(64) NULL COMMENT '使用的LLM模型编码(关联sys_dict_item)',
  error_message VARCHAR(1024) NULL COMMENT '转换失败原因',
  retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',

  -- 导入状态
  weknora_imported TINYINT NOT NULL DEFAULT 0 COMMENT '是否已导入WeKnora: 0=否 1=是',
  weknora_imported_at DATETIME NULL COMMENT '导入WeKnora时间',

  -- 审计字段
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,

  -- 索引
  UNIQUE KEY uk_exam_faq_conversion_exam (tenant_id, exam_id, deleted),
  KEY idx_efc_work_type_status (tenant_id, work_type_id, conversion_status),
  KEY idx_efc_status (tenant_id, conversion_status),
  KEY KEY idx_efc_knowledge (tenant_id, knowledge_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='试题FAQ结构化转换记录表';
```

### 2.2 `exam_faq_conversion_batch`（转换批次表）

> 用途：记录一次按工种发起的转换任务（一个工种可能有成千上万道题）。

```sql
CREATE TABLE exam_faq_conversion_batch (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,

  -- 转换范围
  work_type_id INT NOT NULL COMMENT '工种ID',
  work_type_name VARCHAR(255) NULL COMMENT '工种名称（快照）',
  course_ids JSON NULL COMMENT '选中的科目ID列表（空=全部科目）',

  -- 模型配置（快照，防止字典表变更后不可追溯）
  model_code VARCHAR(64) NULL COMMENT '本次使用的模型编码',
  model_name VARCHAR(128) NULL COMMENT '模型名称快照',

  -- 统计
  total_exams INT NOT NULL DEFAULT 0 COMMENT '该工种下参与转换的试题总数',
  success_count INT NOT NULL DEFAULT 0 COMMENT '转换成功数',
  failed_count INT NOT NULL DEFAULT 0 COMMENT '转换失败数',
  skipped_count INT NOT NULL DEFAULT 0 COMMENT '跳过数（非FAQ适配题型）',
  pending_count INT NOT NULL DEFAULT 0 COMMENT '待转换数',

  -- 批次状态
  status VARCHAR(32) NOT NULL DEFAULT 'running'
    COMMENT '状态: running=执行中 / paused=暂停 / completed=完成 / canceled=取消',
  started_at DATETIME NULL COMMENT '开始时间',
  completed_at DATETIME NULL COMMENT '完成时间',
  error_message VARCHAR(1024) NULL,

  -- 并发控制
  concurrency_limit INT NOT NULL DEFAULT 5 COMMENT '并发批次数',

  -- 审计字段
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,

  KEY idx_efcb_work_type (tenant_id, work_type_id),
  KEY idx_efcb_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='试题FAQ转换批次表';
```

---

## 三、LLM 模型配置（字典表方式）

### 3.1 字典项编码

在 `sys_dict` 中新建字典，编码 `llm_model_provider`：

| 字段 | 值 |
|------|------|
| tenant_id | 各租户独立 |
| code | `llm_model_provider` |
| name | `FAQ转换LLM模型供应商` |

### 3.2 字典项字段（`sys_dict_item`）

每条字典项代表一个可用的 LLM 模型：

| 字段 | 含义 | 示例值 |
|------|------|--------|
| item_value | 模型编码（程序引用 key） | `deepseek-v3` |
| item_label | 显示名称（页面展示） | `DeepSeek V3` |
| sort_order | 排序 | 1 |
| status | enabled/disabled | enabled |

### 3.3 扩展字段 — 模型连接参数存储

> 由于 `sys_dict_item` 只有 `item_value` / `item_label` 两个业务字段，不足以存放 base URL、API Key 等配置。新增一张独立配置表来存储模型连接参数。

```sql
CREATE TABLE exam_faq_model_config (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  model_code VARCHAR(64) NOT NULL COMMENT '模型编码(唯一标识，如 deepseek-v3)',
  model_name VARCHAR(128) NOT NULL COMMENT '模型显示名称',
  base_url VARCHAR(512) NOT NULL COMMENT 'API Base URL(如 https://api.deepseek.com/v1)',
  api_key VARCHAR(512) NOT NULL COMMENT 'API密钥',
  api_format VARCHAR(32) NOT NULL DEFAULT 'openai_compatible'
    COMMENT 'API格式: openai_compatible / anthropic / custom',
  model_id VARCHAR(128) NOT NULL COMMENT '模型ID(如 deepseek-chat)',
  temperature DECIMAL(3,2) NOT NULL DEFAULT 0.7 COMMENT '采样温度',
  max_tokens INT NOT NULL DEFAULT 2000 COMMENT '最大输出token',
  extra_config JSON NULL COMMENT '扩展配置(可选，存储自定义headers等)',
  status VARCHAR(32) NOT NULL DEFAULT 'enabled',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,

  UNIQUE KEY uk_model_config_code (tenant_id, model_code, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='FAQ转换LLM模型连接参数配置表';
```

### 3.4 初始化记录（首条默认配置）

系统首次部署时，通过 Flyway 迁移自动写入一条默认模型配置记录：

```sql
-- Flyway 迁移: V20260712_002__seed_faq_model_config_defaults.sql
INSERT INTO exam_faq_model_config (
  id, tenant_id, model_code, model_name,
  base_url, api_key, api_format, model_id,
  temperature, max_tokens, extra_config,
  status, created_at, updated_at, created_by, updated_by, deleted
) VALUES (
  1,  -- 雪花 ID（实际由 ID 生成器产生）
  0,  -- tenant_id=0 表示全局默认，各租户可覆盖
  'longcat-2.0',
  'LongCat-2.0',
  'https://api.longcat.chat/openai/v1/chat/completions',
  'PENDING_USER_INPUT',  -- 占位符，提醒用户在页面填入真实 API Key
  'openai_compatible',
  'LongCat-2.0',
  0.70,
  2000,
  NULL,
  'enabled',
  NOW(), NOW(),
  NULL, NULL, 0
);
```

**字段取值依据：**

| 字段 | 取值 | 来源 |
|------|------|------|
| `model_code` | `longcat-2.0` | 程序引用 key，小写短横线 |
| `model_name` | `LongCat-2.0` | 页面展示名 |
| `base_url` | `https://api.longcat.chat/openai/v1/chat/completions` | LongCat 文档：`POST /openai/v1/chat/completions` |
| `api_key` | `PENDING_USER_INPUT` | **占位符**，必须通过页面设置真实密钥 |
| `api_format` | `openai_compatible` | 走 OpenAI 兼容端点 |
| `model_id` | `LongCat-2.0` | 请求 body 中的 `model` 字段值 |
| `temperature` | `0.70` | 平衡生成多样性与准确性 |
| `max_tokens` | `2000` | 含 similar/negative questions 足够输出 |

> **部署注意**：`api_key` 字段初始为占位符字符串，系统检测到该值时应在前端标红提示「请配置有效 API Key」。用户必须在页面「WeKnora & LLM 配置」弹层中填入真实密钥后，模型方可用于转换。

### 3.5 页面交互

```
工种题库结构化问答管理页面
├── 模型下拉选择 (从 exam_faq_model_config 查 enabled 列表)
├── 工种下拉选择 (从 exam_work_type 查 enabled 树)
├── 科目多选 (根据工种联动 exam_course)
├── 题型复选 (single / multi / judge)
├── 转换按钮
├── 导出按钮 ─► 生成 faq_example.json 文件下载（按 tag_name 分组）
└── 进度展示:
    ├── 总数 / 已转换 / 待转换 / 失败 / 跳过
    ├── 进度条（轮询刷新 GET /exam/faq/conversion/progress?batchId=X）
    └── 批次列表（可展开看每条试题状态）
```

### JSON 文件导出

- 文件格式与第九章输出结构一致：`faq_example.json`
- 按 `tag_name` 分组输出为多个 JSON 文件，打包为 zip 提供下载
- 仅导出 `conversion_status='success'` 且 `weknora_imported=0` 的记录
- 导出后不改 `weknora_imported` 状态（因用户选择文件手动上传，由用户上传后手动标记或下次导入时自动识别）
- >5000 题的工艺异步生成文件，完成后通知用户下载

---

## 四、WeKnora Webhook 回调配置

### 4.1 回调方向说明

> **Webhook 是 WeKnora 调我们的接口，不是我们调 WeKnora。**

流向：

```
WeKnora 知识库（AI 助手 + 访客交互）
    │
    │  ① 访客发消息 → WeKnora 触发 message_sent
    │  ② AI 回复完成 → WeKnora 触发 message_received
    │
    │  WeKnora 主动 POST 到我们的回调地址
    ▼
我们的 SaaS 系统（platform-api）
    │
    │  POST /api/v1/exam/faq/webhook/weknora
    │
    │  读取签名头 → 用 webhook_secret 校验 → 处理事件
    ▼
记录问答日志 / 分析未匹配问题 / 反哺优化 FAQ
```

**我们的回调地址**由系统统一暴露，**不需要在表中存储 URL**。
管理员在 WeKnora 后台的 Webhook 配置页面填入此地址即可。

### 4.2 回调地址约定

```
https://<saas-domain>/api/v1/exam/faq/webhook/weknora
```

该路径固定，不随租户变化（租户信息从签名 body 中解析）。

### 4.3 配置项（不需要存 URL，只需存签名密钥和事件偏好）

```sql
CREATE TABLE exam_faq_weknora_config (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,

  -- 签名与事件配置（我们接收 WeKnora 回调时用到）
  webhook_secret VARCHAR(256) NULL COMMENT '签名密钥(共享给WeKnora,用于sha256校验)',
  enabled_events JSON NULL COMMENT '启用的事件列表: ["message_sent","message_received"]',

  -- FAQ 导出目标（我们主动推送 FAQ 到 WeKnora 时用到）
  knowledge_base_id VARCHAR(128) NULL COMMENT 'WeKnora知识库ID',
  faq_set_id VARCHAR(128) NULL COMMENT 'FAQ数据集ID',

  -- 功能开关
  webhook_enabled TINYINT NOT NULL DEFAULT 0 COMMENT 'Webhook接收开关: 0=关 1=开',
  auto_export TINYINT NOT NULL DEFAULT 0 COMMENT '转换成功后自动导出到WeKnora',

  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,

  UNIQUE KEY uk_weknora_config_tenant (tenant_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='WeKnora集成配置表(回调签名校验+FAQ导出目标)';
```

### 4.4 Webhook 事件说明

| 事件名 | 触发方 | 触发时机 | POST body 关键字段 |
|--------|--------|----------|-------------------|
| `message_sent` | WeKnora → 我们 | 访客发送消息 | `{event, message_id, conversation_id, content, timestamp}` |
| `message_received` | WeKnora → 我们 | AI 助手回复完成 | `{event, message_id, conversation_id, reply_content, timestamp}` |

### 4.5 签名校验流程

```
WeKnora 管理员配置:
  │  在我们系统的设置页面获取 webhook_secret
  │  将 secret 填入 WeKnora 后台的 Webhook 签名密钥栏位
  │  将回调 URL 填为 https://<domain>/api/v1/exam/faq/webhook/weknora
  ▼

我们服务端接收 Webhook:
  │
  ├─ 读取请求头 X-Webhook-Signature (sha256=...)
  │
  ├─ 用 exam_faq_weknora_config.webhook_secret 对 raw body 做 HMAC-SHA256
  │
  ├─ 比对：签名不通过 → 返回 401
  │
  └─ 签名通过 → 按 event 类型处理:
       ├─ message_sent → 记录访客问题日志（可用于后续分析未匹配问题）
       └─ message_received → 记录AI回复日志 + 触发质量评估
```

> 注：WeKnora 侧的 Webhook 配置（URL + 密钥录入）由管理员在 WeKnora 后台操作，我们的设置页面只展示「回调地址（只读）」+ 「签名密钥（可修改）」给管理员复制。

---

## 五、总体流程（按工种）

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              转换流水线（按工种）                              │
│                                                                             │
│  ① 用户选择工种 → exam_course → exam_knowledge_structure → exam_exam_detail │
│       筛选: status=1, check_status=1, final_check_status=1,                  │
│             type IN ('single','multi','judge'), deleted=0                     │
│       写入 exam_faq_conversion_batch (批次) + exam_faq_conversion (明细)       │
│                                                                             │
│  ② 规则生成 ──► 按题型生成 standard_question / answers / tag_name            │
│       (确定性逻辑，不调 LLM)                                                   │
│                                                                             │
│  ③ LLM 增强 ──► 调用 exam_faq_model_config 选择的模型                        │
│       调大模型生成 similar_questions / negative_questions                      │
│       SYSTEM PROMPT 注入「国家职业标准」专家角色                                │
│       更新 exam_faq_conversion.conversion_status = success/failed             │
│                                                                             │
│  ④ 更新批次进度 ──► exam_faq_conversion_batch 统计字段实时刷新                │
│                                                                             │
│  ⑤ 导出 ──► 用户点击「导出」→ 前端按 tag_name 分组生成 JSON 文件打包 zip 下载  │
│       用户手动上传 zip/JSON 到 WeKnora 后台 → 上传成功后手动标记已导入          │
│                                                                             │
│  ⑥ Webhook ──► WeKnora 侧访客问答事件 → 我们的服务端接收                   │
│       可用于：监测哪些问题高频触发但未匹配 → 反哺优化 FAQ                      │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 六、输入筛选规则

只转换以下题型，其余跳过：

| type 值 | 含义 | 是否转换 |
|---|---|---|
| `single` | 单选题 | ✅ |
| `multi` | 多选题 | ✅ |
| `judge` | 判断题 | ✅ |
| `fill` | 填空题 | ❌ 留空值，不适配 FAQ |
| `essay` / `jianda` | 简答/论述题 | ❌ 答案过长，不适配 FAQ |
| `answer` | 问答题 | ❌ 答案过长 |
| `explain` | 名词解释 | ❌ 答案过长 |
| `cal` | 计算题 | ❌ 需要多步推理 |
| `simple_fill` | 简答填空 | ❌ 不适配 |

**数据库过滤条件：**

```sql
WHERE deleted = 0
  AND status = 1
  AND check_status = 1
  AND final_check_status = 1
  AND type IN ('single', 'multi', 'judge')
```

> 注意：与初版方案不同，新增 `final_check_status = 1`（终审通过），确保只有双审通过的试题才进入 FAQ。

---

## 七、知识点关联逻辑（tag_name 知识层级来源）

### 7.1 关联路径（修正版）

```
exam_exam_detail.knowledge_id
        │
        ▼
exam_knowledge_structure.id  ──►  exam_knowledge_structure.parent_id
        │                                  │
        │                                  ▼
        │                          递归向上查父级
        │                                  │
        │                                  ▼
        │                          拼接路径: 一级-二级-三级-鉴定点
        │
        ▼
tag_name 知识层级 = 知识点路径的末三级（level=2~3 拼接）
```

### 7.2 SQL 示意（表已修正）

```sql
-- 递归查知识点层级路径（MySQL 8+ CTE）
WITH RECURSIVE ks_path AS (
  SELECT id, parent_id, title AS name, level, 1 AS depth,
         CAST(title AS CHAR(500)) AS path
  FROM exam_knowledge_structure
  WHERE parent_id = 0 AND deleted = 0
  UNION ALL
  SELECT c.id, c.parent_id, c.title AS name, c.level, p.depth + 1,
         CONCAT(p.path, '-', c.title)
  FROM exam_knowledge_structure c
  JOIN ks_path p ON c.parent_id = p.id
  WHERE c.deleted = 0
)
SELECT id, name, path, level, depth FROM ks_path;
```

### 7.3 截断规则

| 知识点深度 | tag_name 知识层级取值 |
|---|---|
| depth ≤ 1 | 取当前名称 |
| depth = 2 | 取 `父-当前` |
| depth ≥ 3 | 取末三级 `祖父-父-当前` |
| 无关联知识点 (knowledge_id=0) | 回退到 keyword 字段（按 `\|` 分割取第一个） |
| keyword 也为空 | tag 知识层级填 `未分类` |

---

## 八、输入 → 输出字段映射

### 8.1 输入结构（exam_exam_detail 单条）

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

### 8.2 输出结构（FAQ 条目）

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

## 九、分题型转换逻辑

### 9.1 单选题（type = 'single'）

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

### 9.2 判断题（type = 'judge'）

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

### 9.3 多选题（type = 'multi'）

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
| answers | 枚举所有 `isCorrect=true` 的选项名称，补充每个选项的适用场景，拼接 resolve |
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

## 十、LLM 生成 Prompt 设计（国家职业标准专家角色）

### 10.1 System Prompt（共用前缀）

```text
你是一位「国家职业标准」专家。你熟知中华人民共和国人力资源和社会保障部颁布的各职业国家职业标准（Occupational Standard of the People's Republic of China），包括但不限于：

- 职业定义、职业等级（五级/初级工、四级/中级工、三级/高级工、二级/技师、一级/高级技师）
- 职业技能要求的知识体系和技能权重
- 鉴定要求（申报条件、鉴定方式、考核时长）
- 各行业标准术语表和行业规范

你正在协助将职业技能鉴定试题转换为 FAQ 知识库条目。生成 FAQ 时请严格遵守以下原则：

1. **国家标准优先**：使用国家职业标准中的规范术语和表述，不使用口语化或地方性用语
2. **鉴定点导向**：生成的相似问/反例应覆盖同一鉴定点的不同表述方式
3. **干扰项准确性**：negative_questions 应体现真实考试中常见的错误理解和国家标准中的区分边界
4. **分级意识**：初级工侧重"是什么"，中高级工侧重"怎么做"和"为什么"，编制时要符合对应鉴定层级的认知要求

每次收到题目，你都将当前题目涵盖的国家职业标准上下文（工种名称、职业等级、鉴定范围），再按要求生成输出。
```

### 10.2 生成 similar_questions 的 Prompt

```text
## 题目上下文
- 工种：{work_type_name} ({work_type_series_no})
- 职业等级：{work_type_level}
- 科目{subject_title}
- 鉴定范围：{knowledge_path}
- 题型：{题型}
- 难度：{难度}
- 涉及的国家职业标准鉴定点：{鉴定点描述}

## 题目信息
- 题型：{single|multi|judge}
- 题干：{content_stripped}
- 正确选项：{correct_options}
- 干扰项：{wrong_options}

## 生成要求
1. 相似问必须与原题答案一致，语义等价
2. 使用国家职业标准中的规范术语，避免口语化表述
3. 保留题干中的核心概念（名词、限定词）不变，这些通常是标准中的关键词
4. 生成的问法应覆盖学员在实际作答或质检中可能出现的真实提问方式
5. 通过替换疑问词、同义改写、调整句式来生成多样性问法
6. 不要生成需要额外推理或引入新知识才能回答的问法
7. 输出为 JSON 数组，每条为纯文本字符串，不要带引号以外的标记

## 输出格式
{"similar_questions": ["问法1", "问法2", "问法3"]}
```

### 10.3 生成 negative_questions 的 Prompt

```text
## 题目上下文
- 工种：{work_type_name} ({work_type_series_no})
- 职业等级：{work_type_level}
- 科目{subject_title}
- 鉴定范围：{knowledge_path}
- 题型：{题型}
- 难度：{难度}
- 涉及的国家职业标准鉴定点：{鉴定点描述}

## 题目信息
- 题型：{single|multi|judge}
- 题干：{content_stripped}
- 正确选项：{correct_options}
- 干扰项：{wrong_options}
- 题目考查的关键概念：{key_concepts_from_keyword}

## 生成要求
1. 反例必须与原题「看起来相似但答案不同」，用于培训模型区分能力
2. 使用以下策略生成反例（每条使用不同策略）：
   - 属性替换：保留主题词，把属性词替换为同职业标准中该主题的其他属性
   - 实体替换：保留句式结构，把核心实体换成同工种的其他标准实体
   - 等级替换：将职业等级要求混淆（如把"初级工要求"换成"高级工要求"）
   - 工段/场景替换：把题干中的工段/场景换成工种内相近但不同的工段
   - 答非所问：保留相关词，但答案需要完全不同的标准知识点
3. 每条反例后用括号标注「应匹配的正确答案」
4. 反例应体现国家职业标准中容易混淆的邻近概念
5. 输出为 JSON 数组，每条为纯文本字符串

## 输出格式
{"negative_questions": ["反例1（答案应为XXX）", "反例2（答案应为XXX）", "反例3（答案应为XXX）"]}
```

### 10.4 LLM 调用参数（由 exam_faq_model_config 动态配置）

| 参数 | 取值 |
|---|---|
| base_url | 从 `exam_faq_model_config.base_url` 取 |
| api_key | 从 `exam_faq_model_config.api_key` 取 |
| model | 从 `exam_faq_model_config.model_id` 取 |
| temperature | 从 `exam_faq_model_config.temperature` 取（默认 0.7） |
| response_format | `{"type": "json_object"}` |
| max_tokens | 从 `exam_faq_model_config.max_tokens` 取（默认 2000） |
| api_format | 由 `exam_faq_model_config.api_format` 决定请求适配层 |

### 10.5 调用策略

- **并发控制**：每批最多 20 条试题，调 2 次 LLM（一次 similar、一次 negative）；>5000 题的工艺整体任务异步执行（后台线程池/消息队列消费），前端轮询进度
- **重试机制**：解析失败最多重试 2 次，仍失败则记录 `conversion_status='failed'` + 错误信息
- **超时**：单次调用 30s 超时
- **限流**：按 `exam_faq_conversion_batch.concurrency_limit` 控制请求间隔
- **失败隔离**：单条失败不影响批次其他试题继续执行

---

## 十一、tag_name 命名规范

格式：`{题型}|{知识点路径}|{难度}`

| 字段 | 来源 | 映射规则 |
|---|---|---|
| 题型 | `type` 字段 | `single`→`单选题`，`multi`→`多选题`，`judge`→`判断题` |
| 知识点路径 | `knowledge_id` → `exam_knowledge_structure` 递归查父级 | 取末三级，用 `-` 连接 |
| 难度 | `difficulty` 字段 | 0-1→`简单`，2→`中等`，3-4→`困难` |

**示例：**

```
单选题|碳素工艺-煅烧炉-顺流罐式|中等
多选题|职业道德-工匠精神-表现形式|简单
判断题|安全规范-高空作业-基本要求|困难
```

---

## 十二、边界与特殊处理

| 情况 | 处理方式 |
|---|---|
| content 含 HTML 标签 | 用 Jsoup 或正则去除 `<p><br><strong>` 等标签，保留纯文本 |
| answer 字段解析失败 | 跳过该条，标记 `conversion_status='failed'` + 记录错误 |
| isCorrect=true 的选项为空 | 跳过该条，标记 `conversion_status='failed'` |
| isCorrect=true 的选项不止一个 | 按多选题处理 |
| resolve 为空 | answers 不拼接解析，仅输出选项说明 |
| 题干含填空下划线 | 判断题保留选择题保留；填空题直接跳过 |
| knowledge_id=0（无关联知识点） | 回退到 keyword 字段（按 `\|` 分割取第一个）作为 tag 知识层级 |
| keyword 也为空 | tag 知识层级填 `未分类` |
| 多选题单选项分值为 0 | 可能是全选，按全选处理 |
| LLM 返回非合法 JSON | 重试 2 次，仍失败则 `conversion_status='failed'` |
| LLM API 不可用（429/500） | 当前批次暂停，记录错误到 `exam_faq_conversion_batch.error_message` |
| 单批次失败率 > 30% | 自动暂停，等用户确认后继续 |

---

## 十三、页面设计：工种题库结构化问答管理

### 13.1 页面布局

```
┌────────────────────────────────────────────────────────────────┐
│  工种题库结构化问答管理                                    设置 ──┤
│                                                                │
│  ┌────────────────── 筛选条件 ──────────────────┐               │
│  │ 工种: [下拉树 ▼]  科目: [多选 ▼]              │               │
│  │ 题型: [☑ 单选] [☑ 多选] [☑ 判断]             │               │
│  │ LLM模型: [DeepSeek V3 ▼]                     │               │
│  │ 状态: [全部 ▼]                                │               │
│  └──────────────────────────────────────────────┘               │
│                                                                │
│  ┌────────────────── 进度概览 ──────────────────┐               │
│  │  总 1200  │ 已转换 856  │ 待转换 290  │       │               │
│  │  失败 12  │ 跳过 42     │              │       │               │
│  │  ████████████████████░░░░ 71.3%              │               │
│  │  [开始转换] [暂停] [重试失败] [导出]          │               │
│  └──────────────────────────────────────────────┘               │
│                                                                │
│  ┌─── 试题转换明细表 ──────────────────────────────┐            │
│  │ 序号  │ 题型  │ 题干摘要    │ 知识点路径    │     │            │
│  │       │       │             │               │ 状态 │            │
│  │ ──────┼───────┼─────────────┼──────────────┼──── │            │
│  │ 10063 │ 单选题│ 常用的顺流式…│ 碳素工艺-…  │ ✅  │            │
│  │ 10064 │ 判断题│ 碳素企业目前…│ 碳素工艺-…  │ ⏳  │            │
│  │ 10065 │ 单选题│ 以下关于……  │ 煅烧炉-…    │ ❌  │            │
│  │ 10066 │ 填空题│ 碳素煅烧温… │ —           │ ⏭  │            │
│  └──────────────────────────────────────────────┘               │
│  ◀ 1  2  3  ...  120 ▶                                         │
└────────────────────────────────────────────────────────────────┘
```

### 13.2 状态标识

| 状态 | 图标 | 含义 | 后台值 |
|------|------|------|--------|
| 待转换 | ⏳ | 已写入转换记录但未执行 | `pending` |
| 转换中 | 🔄 | LLM 正在生成 | `processing`（新增） |
| 转换成功 | ✅ | FAQ 生成完毕 | `success` |
| 转换失败 | ❌ | LLM/解析失败 | `failed` |
| 跳过 | ⏭ | 题型不适配 | `skipped` |

### 13.3 操作按钮

| 按钮 | 权限 | 行为 |
|------|------|------|
| **开始转换** | exam:faq:generate | 为未转换的试题发起批量 LLM 调用 |
| **暂停** | exam:faq:pause | 暂停当前运行中的批次 |
| **继续** | exam:faq:resume | 恢复暂停的批次 |
| **重试失败** | exam:faq:retry | 仅对 `failed` 状态的记录重新生成 |
| **导出 JSON** | exam:faq:export | 导出 `success` 状态的 FAQ 为 `faq_example.json` |
| **导入 WeKnora** | exam:faq:export | 直接将 success 状态的 FAQ 推送至 WeKnora |
| **查看明细** | — | 弹出抽屉看 standard_question / similar / negative |

### 13.4 设置弹层

```
┌────────────────────────────────────────────────────┐
│  WeKnora & LLM 配置                                │
│                                                    │
│  ── Webhook 回调 (WeKnora → 我们) ─────────────    │
│  回调地址(只读，填入WeKnora后台):                   │
│  [https://xxx/api/v1/exam/faq/webhook/weknora] [复制] │
│  签名密钥(双方共享):  [____________]  [👁 显示]     │
│  启用事件:  [☑] message_sent                       │
│            [☑] message_received                    │
│  接收开关:   [ ● ] 开   [ ○ ] 关                   │
│                                                    │
│  ── FAQ 导出目标 (我们 → WeKnora) ───────────────  │
│  知识库 ID:        [____________]                  │
│  数据集 ID:        [____________]                  │
│  自动导出:   [ ● ] 是  [ ○ ] 否                    │
│                                                    │
│  ── 模型管理 ──────────────────────────────────   │
│  [+ 新建模型]                                      │
│  ┌─────┬──────────┬──────────┬────────┐            │
│  │ 名称 │ base_url  │ 模型ID   │ 状态   │            │
│  ├─────┼──────────┼──────────┼────────┤            │
│  │ DS  │ https://…│ deepseek │ 启用   │            │
│  │ GPT │ https://…│ gpt-4o   │ 启用   │            │
│  └─────┴──────────┴──────────┴────────┘            │
│                                                    │
│  [取消]                              [保存]        │
└────────────────────────────────────────────────────┘
```

> Webhook 由 WeKnora 管理员在其后台填入回调地址和密钥，我们这边的设置页面只做「展示」+「密钥管理」+「事件开关」，不存 URL。

---

## 十四、模块划分（新增代码参考）

| 层级 | 模块 | 职责 |
|---|---|---|
| Page | `apps/admin-web/src/pages/exam/FaqConversionPage.tsx` | 工种转换管理主页面 |
| Page | `apps/admin-web/src/pages/exam/FaqModelConfigDialog.tsx` | LLM 模型配置弹层 |
| Page | `apps/admin-web/src/pages/exam/FaqWeKnoraConfigDialog.tsx` | WeKnora 配置弹层 |
| Controller | `ExamFaqConversionController` | FAQ 转换 REST API |
| Controller | `ExamFaqModelConfigController` | 模型配置 CRUD |
| Controller | `ExamFaqWeKnoraConfigController` | WeKnora 配置 CRUD |
| Controller | `ExamFaqWebhookController` | WeKnora 回调端点 `POST /api/v1/exam/faq/webhook/weknora`（WeKnora → 我们） |
| Application | `FaqConversionApplicationService` | 编排：读题 → 调 LLM → 入库 → 更新进度 |
| Application | `FaqConversionBatchService` | 批次创建/进度计算/暂停恢复 |
| Application | `FaqModelConfigApplicationService` | 模型配置管理 |
| Application | `FaqWeKnoraConfigApplicationService` | WeKnora 配置管理 |
| Application | `FaqWeKnoraWebhookService` | Webhook 接收/签名校验/事件处理 |
| Domain | `ExamFaqConverter` | 按题型生成 standard_question / answers / tag_name |
| Domain | `KnowledgeStructurePathResolver` | 通过 knowledge_id 递归查 `exam_knowledge_structure` 路径 |
| Domain | `FaqLlmPromptBuilder` | 组装含国家职业标准上下文的 LLM Prompt |
| Infrastructure | `FaqLlmClientAdapter` | 统一适配 openai_compatible / anthropic / custom |
| Infrastructure | `FaqWeKnoraExportClient` | WeKnora FAQ 接口导出 |
| Flyway | `V20260712_001__create_faq_conversion_tables.sql` | 4 张新表 |

---

## 十五、输出文件组织

按 `tag_name` 一级分类输出，方便分批次导入 WeKnora：

```
output/
  batch_{work_type_id}_{timestamp}/
    faq_单选题-碳素工艺-煅烧炉.json       ← 同一 tag 的 FAQ 集合
    faq_判断题-职业道德-工匠精神.json
    faq_多选题-安全规范-高空作业.json
    error_log.txt                        ← 解析失败/跳过的试题清单
    stats.json                           ← 转换统计
```

---

## 十六、待确认项

| 项 | 状态 |
|---|---|
| LLM 服务商选择 | ✅ 采用「页面配置多个，下拉选择」方案，已有默认 LongCat-2.0 初始化记录 |
| WeKnora FAQ 导入方式 | ✅ 文件手动上传；页面提供 `faq_example.json`（按 tag_name 分组，打包 zip）导出功能 |
| `message_received` 事件处理 | ✅ 仅记录日志，不做自动质量评估 |
| 国家职业标准知识注入 | ✅ 暂不需要从 WeKnora 知识库实时 pull 标准文本拼入 prompt；prompt 中的国家职业标准上下文由 system prompt 内置定义（工种名、职业编码、鉴定范围） |
| Webhook 回调安全 | ✅ 仅使用 HMAC-SHA256 签名校验，不需要 IP 白名单或 mTLS |
| Webhook 回调地址 | ✅ SaaS 域名/反代地址已就绪且支持 HTTPS，回调路径固定为 `/api/v1/exam/faq/webhook/weknora` |
| 转换进度 WebSocket or 轮询 | ✅ 采用轮询方式，前端定时调用 `GET /exam/faq/conversion/progress?batchId=X` |
| 单工种大数据量性能 | ✅ >5000 题的工艺采用异步处理：批次写入 + 后台任务消费 + 完成后通知用户 |

---

*方案版本：v2.0 | 日期：2026-07-12 | 状态：待用户确认后落地实现*
