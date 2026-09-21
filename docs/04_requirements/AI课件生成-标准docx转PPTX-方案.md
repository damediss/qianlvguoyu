# AI 课件生成（标准 docx 转原生可编辑 PPTX）方案 v1.4

> 状态：v1.3 已交付（dev 430fa228）；**v1.4 知识点页改造已实施并通过全部测试与真实冒烟（2026-09-21）**；实施记录与证据见 §19
> 输入：职业标准 / 细目表类 docx；产物：原生可编辑 PPTX 培训课件（学员自学用）
> 参考项目：https://github.com/yuyuanweb/ai-ppt-generator （Python FastAPI + LangChain + LangGraph + python-pptx，源码已通读）
> 方法借鉴：WorkBuddy 课件方法文章（mp.weixin.qq.com/s/1bgHCcHMgOtG_MKGk7M3Iw）01 部分，吸收点见 §3.2

**v1.1 变更**：新增素材消化层（§6.5）；大纲升级为章→页两级（§7）；单页 prompt 吸收"结论式标题/对比优先表格/口语化"约束（§8.3）；导出页脚确定性来源标注（§10.9）；末页固定复习思考题；bullets 上限 6→5；菜单确认挂"智能课程材料管理"目录下（§13）；任务与风险表同步更新。

**v1.4.1 修正（2026-09-21 用户验收反馈）**：① 表格禁止「序号/编号」列（写入知识点与通用 prompt）;② 修复 18 页空白表格——根因是模型把 `rows` 输出为对象数组被解析成空行，修复为宽容解析（对象行按键序取值）+ 校验器新增「空白数据行」structure error 触发修复；③ EA 测试清理逻辑按标题前缀收敛，杜绝误删真实项目（本次冒烟项目曾被全表 DELETE 误删）；④ S-04 全量重生验证：196 个表格 0 序号列 0 空白行，215/215 零失败。

**v1.4 变更（知识点页，2026-09-21 用户逐条确认 AAAAA）**：基本要求/工作要求章从"概括页"改为**逐最小知识点一页**——解析层确定性枚举（基本要求按三级编号段落、工作要求按表格「相关知识要求」列 x.y.z 条目，表头/行 gridSpan 逻辑列展开）；大纲阶段替换/兜底插入知识点章（基本要求 1 章 + 工作要求按等级分章，重叠率 ≥60% 规则）；知识点页专用 prompt（结论式标题 + ≤8 行 × ≤5 列骨架表 + note 核心口诀槽位）；技能要求列融为页面"你会做什么"；页数上限 50 → 300 配置化；新增 cw_knowledge_point 表（V20260921_101）；页级派发加信号量防大课件打爆页池队列；单页 LLM 调用失败退避重试 1 次（§12 口径）。

**v1.3 变更（实施收口）**：§12 修正 thinking 口径（GLM-5.3-Flash 始终思考、不支持关闭，实测错误码 1210；改 `enabled + reasoning_effort=low`）；§15.7 DoD 逐项勾选并附证据；新增 §19 实施记录（代码清单、五层测试结果、真实冒烟数据、运维要点、I-01 环境限制说明）。

**v1.2 变更**：§15 重写为完整测试与验收清单——五层测试策略（U/I/EA/EU/S）+ 编号化清单（U-01~15、I-01~03、EA-01~11、EU-01~08、S-01~02）+ DoD 验收勾选表；§16 任务分解新增 T6（E2E 测试）、T7（冒烟收尾），各任务验收栏改为引用清单编号。

---

## 1. 背景与目标

培训业务需要将职业标准 / 细目表类 docx 文档转化为培训课件 PPT。人工制作耗时，AI 直出又常见"内容空洞、版面溢出、导出成图片不可编辑"三类失败。本方案在 platform-api 内实现一条 **「docx 解析 → 素材消化（教学化转译）→ 大纲生成（人工确认卡点）→ 页级并发生成 → 确定性质量自纠 → POI 原生 PPTX 导出」** 链路。

**一期目标**：

1. 上传标准 docx，先经"素材消化"把标准条文转译为教学化素材，再生成结构化大纲，用户确认/编辑后触发页面生成
2. 每页生成走"生成 → 确定性校验 → 有界修复（最多 1 轮）"自纠环，系统性提升单页质量
3. 导出**原生可编辑** PPTX（文本框/表格为原生对象，非整页图片），用户在 PowerPoint 中继续编辑
4. 导出前 error/warning 分级质量门禁，error 阻断、warning 透出明细

**一期不做（非目标）**：Web 画布在线编辑器、AI 局部编辑（bind_tools）、flex 自由布局树、process 流程图块（流程用有序 bullets 表达）、AI 配图/图库、多主题切换、SSE 实时推送（用轮询替代）、学员端入口。

## 2. 已确认口径（2026-09-18 用户逐条确认）

| # | 口径 | 结论 |
|---|---|---|
| 1 | 输入与产物 | 职业标准/细目表类 docx → 培训课件 PPT，单入口 |
| 2 | 页数与受众 | 页数不限定，按 docx 内容量由大纲阶段建议、用户可调；学员自学用 |
| 3 | 技术落点 | Java 全栈，落 platform-api，新建 `courseware/` 业务包；POI XSLF 渲染（poi-ooxml 已在依赖树，Tika 传递引入） |
| 4 | 能力边界 | 生成 + 质量检查 + 导出 PPTX；不做 Web 画布编辑器，用户在 PowerPoint 里编辑 |
| 5 | 自纠环深度 | 纯确定性校验 + 1 轮修复；**不加 LLM 评审节点**（符合"确定性规则不让位"） |
| 6 | 版式系统 | 固定槽位模板（封面/目录/章节/内容页型），不做 flex 布局树 |
| 7 | AI 局部编辑 | 一期不做，bind_tools 编辑随在线编辑器后置 |
| 8 | 模型与主题 | GLM 优先（glm-5.3-flash，走 exam_faq_model_config 配置化）；内置 1 套企业色主题 |
| 9 | 素材消化层 | **加**：docx 解析后、大纲生成前多 1 次 LLM 调用，产出教学化素材（核心知识点/易混淆点/关键数据），兼作 speaker_notes 底稿（v1.1 新增） |
| 10 | 大纲结构 | **章→页两级**：大纲页带所属章节，确认界面按章分组（v1.1 新增） |
| 11 | 学习时长/思考题 | 只加**末页固定复习思考题**；不做每章学习时长（自学场景意义弱）（v1.1 新增） |
| 12 | 流程/步骤表达 | 一期用有序 bullets，process 步骤条块列后续扩展（v1.1 新增） |
| 13 | 来源标注 | 导出时页脚**确定性渲染**"来源：《XX》第 X 节"（source_refs→locator 映射，不依赖模型自觉）（v1.1 新增） |
| 14 | 知识点页粒度（v1.4） | x.y.z 相关知识要求条目逐条一页（电工 184 页）；基本要求章同模式（10 页） |
| 15 | 技能要求列（v1.4） | 不出独立页，融为对应知识点页"你会做什么" |
| 16 | 页数上限（v1.4） | 50 → 300（`platform.courseware.max-pages` 配置化），接受 ≈230 页体量与成本 |
| 17 | 口诀位置（v1.4） | 页底 note 小字槽位（content_table 模板扩展） |

## 3. 源码分析结论：借鉴什么、不借鉴什么

### 3.1 ai-ppt-generator（架构借鉴）

ai-ppt-generator（527 文件）核心源码已通读，关键事实：

**它的"自纠环"实质**（`workflows/slide.py`）：4 节点状态机 `prepare → generate → check → (repair) → generate`，最多修复 1 轮。**check 节点是 100% 确定性代码，不含 LLM 调用**——结构校验（槽位契约）、容量校验（字数/条目上限）、溢出校验（FontTools 字形度量预测折行）、空话检测、过瘦检测（块数/要点数/填充率 <72%）。修复只是把问题文本回喂 prompt 重生成；溢出/容量类 warning 明确不触发重写（避免修复反而删光内容）。

**LangChain/LangGraph 的实际用量很薄**：LangChain 只用了 `with_structured_output(json_mode)`（≈prompt 约定 JSON + 校验）和 `bind_tools`（≈透传 tools + 手写分发循环）；LangGraph 只用了 StateGraph 条件回边，等价于一个 30 行的 for 循环状态机。该项目是框架教学项目（README 明示卖课），框架选型含教学动机。**质量的真实来源是确定性校验器与数据模型设计，不是图编排框架**。

**借鉴（对本方案有效的设计）**：

1. 内容（blocks）/布局（槽位模板）/主题（色值字号）三分离，单一 JSON 事实源，导出渲染同源
2. 大纲人工确认卡点：模型不越过用户直接生成最终页面，省钱防跑偏
3. 页级独立上下文：每页只带本页引用的小节（裁剪 1500/6000 字符）、相邻页标题，页面间互不依赖可并发
4. 确定性校验器 + error/warning 分级门禁 + 有界修复（1 轮、只修值得修的）
5. 槽位容量上限直接写进 prompt（与其事后裁剪，不如事前告知可用空间）
6. 失败页单独重试、软取消、导出后回读验证

**不借鉴**：LangChain/LangGraph（Java 无对应需求，普通服务类+状态循环等价实现）、flex 布局树+solver（复杂度高一档）、ARQ/Redis/SSE（用现有线程池+轮询）、python-pptx/FontTools（用 POI XSLF + 字宽估算/fontbox 替代）。

### 3.2 WorkBuddy 课件方法文章（方法借鉴，v1.1 新增）

该文章 01 部分把做课件拆为四环节：资料搜集 → 阅读整理 → 大纲策划 → PPT 整理，**AI 深入每个环节，人只在关键节点确认**。对我们的适用性分析：

| 文章方法 | 适用性判断 | 吸收方式 |
|---|---|---|
| 资料搜集（搜索技能） | **不适用**：素材是上传的标准 docx，不是散乱网络资料 | 不吸收 |
| 阅读整理（泛读→归类→冲突观点→扎实数据筛选，产出分类文档兼作讲稿底稿） | **核心吸收**：标准条文是法规式文本，转成教学课件需要"视角转换"；职业标准无"冲突观点"，对应物是**易混淆/易错点** | 新增素材消化层（§6.5），消化产物同时喂大纲与 speaker_notes |
| 大纲策划带教学参数（听众/章节/核心结论/时长/讨论环节） | **部分吸收**：章节化与核心结论吸收；讲授时长对自学场景意义弱不吸收；讨论环节转译为末页复习思考题 | §7 大纲章→页两级；末页固定 page_role=review |
| PPT 生成约束（结论式标题/正文≤5行/口语化/数据页注明来源/案例用对比表/末页讨论题） | **吸收**：标题、行数、语气、对比表进 prompt 与容量表；来源标注用确定性渲染而非模型自觉；流程图一期不做（有序 bullets 替代） | §8.3 prompt、§9.3 容量表、§10.9 页脚来源标注 |

## 4. 总体架构

```
admin-web(React)                     platform-api(Spring Boot)
┌──────────────────┐   REST/轮询    ┌─────────────────────────────────────┐
│ 课件项目列表/创建  │ ───────────▶ │ courseware/                         │
│ 大纲确认(按章分组) │              │  ├─ DocxSectionParser(POI XWPF)     │
│ 生成进度轮询       │              │  ├─ DigestService(素材消化,1次LLM)  │
│ 页明细+问题列表    │              │  ├─ OutlineService(章→页,1次LLM)    │
│ 导出下载 PPTX     │ ◀─────────── │  ├─ SlideWorkflow(生成→校验→修复1轮)│
└──────────────────┘               │  ├─ SlideValidator(确定性校验器)    │
                                   │  ├─ PptxRenderer(POI XSLF+页脚来源) │
                                   │  └─ 异步编排(线程池+AbortPolicy)    │
                                   │ ai/ 既有: LLM 代理客户端(GLM 闸口)  │
                                   │ file/ 既有: docx 上传 / pptx 存储   │
                                   └──────────────┬──────────────────────┘
                                                  ▼
                                    MySQL gjjddbpro(新表 cw_*)
                                    LLM 代理 → GLM-5.3-Flash
```

**分层约束**（按 AGENTS.md）：Controller（参数/权限入口）→ ApplicationService（用例编排、事务边界）→ DomainService（校验器、版式分配、渲染）→ Mapper。LLM 调用收敛：业务代码只依赖 `CoursewareAiService` 领域接口，接口实现内部走现有 LLM 代理客户端；禁止在业务 Service 里自建 HTTP client。

**主链路状态机**：

```
项目: draft → parsing → digesting → outline_generating → outline_ready
      →(用户确认)→ generating → ready / partial / failed
页面: pending → generating → ready / failed   (failed 可单独重试)
```

digest 失败不阻断主流程：降级为"无消化素材"直接走大纲（§6.5.5）。

## 5. 数据模型

新表 5 张（全部含完整 BaseEntity 六列：`tenant_id / deleted / created_by / updated_by / created_at / updated_at`，主键雪花 BIGINT，逻辑删除走 MyBatis-Plus）。迁移版本号实施时**同时查测试库与生产库 MAX(version) 取最大值+间隔**（踩坑 #7）。

### 5.1 cw_project（课件项目）

| 列 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | 雪花 |
| title | VARCHAR(200) | 课件标题（默认取 docx 文件名，可改） |
| source_file_id | BIGINT | file 包文件 ID（原始 docx） |
| audience | VARCHAR(100) | 受众，默认"学员自学" |
| content_density | VARCHAR(16) | concise/medium/detailed，默认 medium |
| theme_id | VARCHAR(32) | 一期固定 `enterprise` |
| status | VARCHAR(24) | 见状态机（含 digesting） |
| total_pages / ready_pages / failed_pages | INT | 进度计数（冗余自 cw_slide 聚合） |
| error | VARCHAR(500) | 项目级错误 |

### 5.2 cw_source_section（来源小节）

docx 解析产物落库，供消化/大纲/单页三级引用与追溯。

| 列 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | |
| project_id | BIGINT | |
| ref | VARCHAR(32) | 稳定引用号 `S1:2`（来源 1：小节 2；一期单文档，来源号恒为 1） |
| heading | VARCHAR(200) NULL | 小节标题（无标题小节为 NULL） |
| level | INT | 标题级别 0~6 |
| text | MEDIUMTEXT | 小节正文（表格已管道化为文本） |
| locator | VARCHAR(64) | 定位描述（"第 N 段"） |

### 5.3 cw_digest（消化素材，v1.1 新增）

每项目一份；生成失败时该表无记录，大纲/单页走降级路径。

| 列 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | |
| project_id | BIGINT | 唯一索引 |
| topics | JSON | 消化产物，结构见 §6.5.3 |
| status | VARCHAR(16) | ready / failed |
| error | VARCHAR(500) NULL | |

### 5.4 cw_outline_page（大纲页）

| 列 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | |
| project_id | BIGINT | |
| chapter_no | INT | 所属章序号（v1.1 新增） |
| chapter_title | VARCHAR(120) | 所属章标题（冗余落库，便于按章分组展示与 section 页生成） |
| position | INT | 页序（全文档连续，确认后重排连续化） |
| title | VARCHAR(120) | 页标题（可编辑；内容页应为结论句） |
| objective | VARCHAR(300) | 本页目标（可编辑） |
| key_points | JSON | 要点数组（可编辑，3~5 条） |
| page_role | VARCHAR(16) | cover/toc/section/content/summary/**review**（复习思考题，v1.1 新增） |
| layout_id | VARCHAR(32) | 槽位模板 ID（编排层分配，见 §9） |
| source_refs | JSON | 本页引用的 ref 数组 |

### 5.5 cw_slide（生成页）

| 列 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | |
| project_id / outline_page_id | BIGINT | |
| position | INT | |
| layout_id | VARCHAR(32) | 实际使用的模板 |
| blocks | JSON | 内容块数组（单一事实源，见 §9.2） |
| speaker_notes | VARCHAR(1000) | 讲稿提示（写入 PPTX 备注页；自学场景作为"学习提示"） |
| issues | JSON | 残留质量告警（修复 1 轮后仍存在的） |
| status | VARCHAR(16) | pending/generating/ready/failed |
| error | VARCHAR(500) NULL | |
| revision | INT | 乐观锁（重试/覆盖写保护） |

索引：`cw_slide(project_id, status)`、`cw_source_section(project_id)`、`cw_outline_page(project_id, position)`、`cw_digest(project_id UNIQUE)`。

LLM 调用审计复用现有 AI 调用落库（用户/租户/模型/prompt 摘要/token/耗时/请求 ID）；若现有表不含业务类型字段，补 `biz_type='courseware'` 或新建 `cw_llm_log`，实施时先盘点 ai/ 既有审计再定。

## 6. docx 解析与小节切分（T2）

参考 `ingest/docx.py` 逻辑用 POI XWPF 实现 `DocxSectionParser`：

1. 遍历 `XWPFDocument` 段落与表格（保持文档序）
2. 段落样式名匹配 `Heading N` / `标题 N` → 新小节（级别 1~6）
3. **兜底**：标准 docx 标题样式常不规范（项目既有踩坑），追加正则识别编号段落（`第X章/第X部分`、`^\d+(\.\d+)*[、.．\s]`、`^[一二三四五六七八九十]+[、.．]`）作为虚拟标题，级别按编号深度
4. 表格 → 单元格 trim 后 ` | ` 连接成行，行间换行，并入当前小节正文
5. 输出 `List<SourceSection>` 落库 `cw_source_section`
6. 全文为空/无法解析 → 项目置 failed 并给出白话提示（"这份文档没有识别出可用内容，请确认是文字版 docx"）

### 6.5 素材消化层（T2，v1.1 新增）

**定位**：标准条文是法规式文本（"应具备……能力"），课件是教学文本（"你要学会……"）。消化层做一次**视角转换**，是课件质量的关键杠杆；产物兼作 speaker_notes 素材底稿。

#### 6.5.1 输入

全部小节裁剪（单节 ≤2000 字符、总 ≤12000 字符，与大纲同预算规则；按整节追加，超长截断该节正文）。

#### 6.5.2 prompt 要点

"你是职业培训教研员。请把以下职业标准文档消化为教学素材：按知识领域归纳（3~8 个 topic），每个 topic 给出：核心知识点（学员必须掌握的结论）、易混淆/易错点（相近概念辨析、常见误解）、关键数据（温度/时间/比例等可考核数字，必须原文出现）。只能引用给定 ref。"

#### 6.5.3 输出契约（JSON，零信任校验）

```json
{ "topics": [ { "name": "...", "points": ["..."], "confusions": ["..."],
                "key_data": ["..."], "source_refs": ["S1:2"] } ] }
```

校验：Jackson 结构校验；source_refs 必须在已落库 ref 集合内（非法剔除记 warn）；topic 数钳制 [3, 8]；key_data 中数字优先回查来源文本（查不到不阻断，剔除并记 warn——防编造）。校验失败重试 1 次。

#### 6.5.4 消费方式

- **大纲生成**：digest topics 为主要输入（替代原始小节全文），小节 ref 索引仍随附供追溯
- **单页生成**：该页 source_refs 命中的 topic 片段（points/confusions/key_data）注入上下文，充实正文与 speaker_notes

#### 6.5.5 降级

digest 生成失败（LLM 异常/校验两轮不过）→ cw_digest 无记录，项目状态继续推进，大纲直接以原始小节为输入，单页 speaker_notes 只依据小节原文。**消化层失败不阻断主流程**（LLM 规范第 4 条）。

## 7. 大纲生成与人工确认（T2）

**一次 LLM 调用**，输入以 digest topics 为主（无 digest 时用原始小节，裁剪规则不变：单节 ≤2000、总 ≤12000）。

**页数决策**（口径 2：不限定）：由模型按内容量建议，系统钳制 **[6, 50]** 页；页数进入大纲确认界面可增删页。

**章节化**（口径 10）：prompt 要求 **3~6 章**，每章给 `chapter_title` 与一句 `core_conclusion`；页挂章下。章数钳制 [3,6]，章外页（cover/toc/review）chapter_no=0。

**prompt 契约**：系统提示词要求只输出 JSON：

```json
{ "chapters": [ { "chapter_title": "...", "core_conclusion": "...",
    "pages": [ { "title": "...", "objective": "...", "key_points": ["...","..."],
                 "page_role": "section|content", "source_refs": ["S1:2"] } ] } ] }
```

约束写进 prompt：章数 3~6；每章首页 page_role=section；内容页 title 用**本页核心结论句**（禁名词短语）；key_points 3~5 条；只能引用给定 ref；数字必须来自来源文本。cover/toc/review 三页**由编排层确定性生成**，不占模型输出：cover（标题=课件标题）、toc（目录=章标题列表）、**review 复习思考题固定为末页**（口径 11：题目 3~5 道，由模型在章节输出中附带 `review_questions` 字段产出，编排层组装为末页大纲；模型未产出则用各章 core_conclusion 拼"回顾本章核心结论"式兜底题）。

**输出零信任**：Jackson 校验结构；章数/页数钳制；`source_refs` 必须在已落库 ref 集合内（非法剔除记 warn）；page_role 非法兜底 content；展平后 position 连续化。校验失败重试 1 次，仍失败项目置 failed。

**人工确认卡点**：大纲落库后项目停 `outline_ready`，前端**按章分组**的表单编辑（章标题/页标题/目标/要点可改、增删页、拖拽调序、跨章移动），点击"开始生成"才把 outline 快照固化、初始化 cw_slide（pending）并触发异步生成。**用户未确认不消耗页面生成 token**。

## 8. 单页生成自纠环（T3，核心）

### 8.1 状态机（普通 Java 实现，无框架）

```java
// SlideWorkflowService.execute(input) 伪结构：
for (int round = 0; ; round++) {
    SlideDraft draft = aiService.generateSlide(input);        // 1 次 LLM 调用
    Slide slide = assemble(draft, input.getLayoutId());       // 块装配+槽位契约检查
    List<StructureIssue> issues = slideValidator.validate(slide, input);
    boolean repairWorthy = issues.stream().anyMatch(IssueRules::isRepairWorthy);
    if (!repairWorthy || round >= MAX_REPAIR_ROUNDS /*1*/) {
        return Result.of(slide, issues);                      // 残留 issues 落库透出
    }
    input = input.withIssues(describe(issues));               // 问题文本回喂
}
```

### 8.2 单页上下文（页级独立，支撑并发）

每页输入只含：课件标题、受众（学员自学）、本页 position/total、所属章标题与核心结论、title/objective/key_points、page_role、layout_id、本页 source_refs 指向的小节（单节 ≤1500、合计 ≤6000 字符）与 **digest topic 片段**（§6.5.4，有则注入）、**相邻页标题**（防内容重复/衔接断裂）、修复轮 issues。页面之间不共享可变状态。

### 8.3 生成 prompt 要点（v1.1 吸收文章约束）

- **标题=结论**："内容页 title 必须是本页核心结论句（如'淬火温度决定硬度上限'），禁止名词短语（如'淬火工艺简介'）"
- **正文口语化**：面向自学学员，语言口语化、写具体结论与事实，禁"本页介绍……"空话
- **对比/分类优先表格**："涉及对比、分类、参数列表的内容优先用 table 块，不要大段文字"
- **流程用有序要点**："操作步骤/流程用 bullets 按顺序表达，每条一个动作"（一期无 process 块，口径 12）
- 槽位容量上限直接写进 prompt（见 §9.3 表），要求"在上限内写满支撑细节，贴近容量中上沿"
- 数字必须来自给定来源；speaker_notes 2~3 句（自学场景=学习提示：本页重点、易错提醒）
- 修复轮追加："上一次生成存在以下问题，请只修正这些问题并保持其余内容稳定" + issues 列表
- 输出契约：只输出 JSON `{"blocks":[{slot_id,type,...}],"speaker_notes":"..."}`（不走 response_format——现有代理不透传，用 prompt 约定 + Jackson 校验，与 theorygen 同模式）

### 8.4 确定性校验器规则表（SlideValidator，全部纯 Java）

| code | 规则 | 级别 | 触发修复 |
|---|---|---|---|
| structure | slot_id 不在模板内 / 必填槽位缺失 / 槽位重复填充 / 块类型不被槽位接受 | **error** | ✅ |
| capacity | 超槽位字数/条目/单条字数上限 | warning | ❌（透出） |
| overflow | 估算文本高度 > 槽位可用高度（字宽估算：CJK 1.0em、拉丁 0.55em、空格 0.33em；行高×行数对比槽位矩形） | warning | ❌（透出） |
| empty_phrase | 命中空话正则（"本页介绍/相关内容/敬请期待/TODO"等，词表内置可配） | warning | ✅ |
| thin_content | 要点数 < 密度档下限 / 过半要点 < 最短字数（medium 档默认 ≥3 条、单条 ≥12 字） | warning | ✅ |

`is_repair_worthy`：severity=error，或 code ∈ {thin_content, empty_phrase}。capacity/overflow 只透出不重写（防止修复删光内容）。

标题是否结论句**不做软校验**（句式判断误报率高），只作 prompt 硬约束（口径：直接定）。

### 8.5 收口与落库

修复 1 轮后无论残留与否都收口：`blocks/speaker_notes/issues` 落库，status=ready；LLM 调用异常/JSON 校验失败 → status=failed + error，**单页失败不阻断其他页**。残留 issues 在页明细页透出（白话文案："第 3 页文字偏多，导出后建议检查版面"——不暴露 slot_id 等内部参数）。

## 9. 固定槽位模板与主题（T4 前置数据）

### 9.1 一期模板集（layout_id）

| layout_id | 用途 | 槽位 |
|---|---|---|
| cover | 封面 | title(主标) + subtitle(副标) |
| toc | 目录 | title + bullets(章条目 ≤8) |
| section | 章节页 | title(章标题) + text(章核心结论一句 ≤60 字) |
| content_bullets | 要点页（默认兜底） | title + bullets |
| content_table | 表格页 | title + table |
| content_kpi | 数据页 | title + kpi×2~3(数值+标签+备注) |
| summary | 复习思考题页（review 复用） | title + bullets(题目) |

编排层按 page_role + 大纲内容形态分配 layout_id：cover/toc/section/review 固定映射；content 页模型可在 prompt 白名单内建议，非法值兜底 `content_bullets`。

### 9.2 块模型（blocks JSON 契约）

```json
[ {"slot_id":"title","type":"text","text":"..."},
  {"slot_id":"body","type":"bullets","items":["..."]},
  {"slot_id":"body","type":"table","header":["..."],"rows":[["..."]]},
  {"slot_id":"kpi_1","type":"kpi","value":"37%","label":"...","note":"..."} ]
```

一期块类型收敛 4 种：`text / bullets / table / kpi`（不开 image/chart/cards/callout/process——无 AI 配图，流程用有序 bullets，复杂度后置）。

### 9.3 槽位容量表（写进 prompt 与校验器，同源）

| 槽位 | max_chars | max_items | max_chars_per_item |
|---|---|---|---|
| title | 40（结论句放宽，v1.1 调整） | — | — |
| subtitle | 50 | — | — |
| body(bullets) | — | **5**（文章口径"正文不超过5行"，v1.1 调整） | 45 |
| body(table) | — | 8 行 | 列 ≤5、单元格 ≤20 字 |
| kpi | value ≤12、label ≤10、note ≤30 | 3 | — |
| text(章节引言) | 60 | — | — |

### 9.4 内置主题 `enterprise`（一套，配置类内置）

蓝白企业风：主色 `#1F5C99`、辅助 `#EAF2FA`、正文 `#333333`、标题 `#1F5C99`；字体：标题/正文均"微软雅黑"（Office 环境必装）；页面 16:9（12192000×6858000 EMU）。具体色值实施时可微调，集中在一个 ThemeConfig 常量类，后续多主题只需数据化。

## 10. PPTX 渲染与导出（T4）

1. POI XSLF：`XMLSlideShow` 16:9 + 每页 `XSLFSlide`（用**空白版式**，槽位几何全由模板数据决定，避免 PowerPoint 占位符引入不可控位置）
2. 逐块渲染原生对象：`XSLFTextBox`（title/text/bullets，项目符号用 `setBullet`）、`XSLFTable`、kpi 用成组文本框（大字号数值+标签+备注）
3. **中文字体坑**：`XSLFTextRun.setFontFamily` 只设拉丁字体，必须同时写 ea 字体（底层 `CTRegularTextRun` 加 `a:ea typeface`），否则中文回落默认字体
4. `speaker_notes` 写入 `XSLFNotes` 备注页
5. **导出前门禁**：重跑 SlideValidator 全量校验 + 页边界检查，存在 error 阻断导出（返回问题清单），warning 放行并随响应透出
6. **导出后回读验证**：重新打开生成的 pptx，断言页数一致、每页至少 1 个非空文本形状；失败按导出失败处理
7. 产物存 file 包（返回 file_id），下载走现有文件通道；文件名中文（`{课件标题}.pptx`）
8. 幂等：重复导出重新渲染（内容以 cw_slide.blocks 当前值为准），不缓存旧文件
9. **页脚来源标注**（v1.1 新增，口径 13）：content/section/review 页页脚确定性渲染 `来源：《{课件标题}》{ locators }`——由该页 source_refs 映射 cw_source_section 的 heading/locator 拼接（取前 2 个，超出省略号），**不依赖模型在正文里注明**；页脚小字号灰色（主题 token），cover/toc 页不标

## 11. 异步编排与多租户（T3）

- **三段式**（踩坑 #11）：①上传 docx 秒回 projectId → 异步解析+消化+大纲；②确认大纲秒回 → 异步页级生成；③进度轮询 `GET /courseware/projects/{id}/progress`（聚合 total/ready/failed + 每页状态 + 项目阶段 parsing/digesting/outline_generating）
- 线程池：新建独立 `coursewareExecutor`（`ThreadPoolTaskExecutor`，核心 4/最大 8/队列 50，**AbortPolicy** → 全局异常返回 503"服务繁忙，请稍后重试"，踩坑 #12）；页级并发由池大小控制
- **GLM 闸口**：LLM 调用走 FaqLlmClient 体系，自动受 glm-5.3-flash 50 并发公平信号量约束，无需另建闸口
- **租户安全**（陷阱 #2）：tenantId/userId 在 Controller/HTTP 线程捕获后经参数传入异步方法，整条调用链（含下游 Service/Mapper/配置查询）禁止再调 SecurityContext；Worker 线程 `TenantContextHolder.set` + `finally clear`
- 失败页单独重试：`POST /courseware/slides/{id}/retry`（仅 failed 可重试，revision 乐观锁）
- 软取消（可选，一期可降级为"忽略剩余 pending"）：生成中项目允许取消，正在跑的页跑完，pending 页标 cancelled

## 12. LLM 接入（GLM 优先）

- 模型配置化：走 `exam_faq_model_config` 新增模型配置（如 `courseware-slide-gen`，baseUrl/模型名/key 全走配置，禁硬编码），默认 glm-5.3-flash；切模型只改配置
- 参数：temperature 0.7（起始值，配置可调）；maxTokens 消化/大纲/单页统一 8192（思维链计入输出 token，留足预算）；**thinking=`enabled` + `reasoning_effort=low`**——GLM-5.3-Flash 始终思考、不支持 `disabled`（实测 HTTP 400 错误码 1210「该模型始终思考，不支持关闭思考」，2026-09-20 冒烟修正；DeepSeek 的「关思考」口径不适用于该模型，坑 #13 同类）
- 超时与重试：连接 10s/读取 120s；HTTP 层失败重试 1 次指数退避；JSON 契约失败重试 1 次（带"必须只输出 JSON"强化提示）
- 降级：消化层失败降级直喂（§6.5.5）；LLM 不可用（闸口满载超时/连续 5xx）→ 页置 failed + 白话提示，项目可稍后整体重试，不阻塞主流程
- 审计：每次调用落库（用户/租户/模型/prompt 摘要/hash/token 用量/耗时/traceId）；生成内容落库即含来源标记（模型、时间、操作人）
- 上下文最小化：只送本项目小节与 digest，不夹带他租户数据；标准 docx 一般无敏感个人信息，如检出身份证号模式在 prompt 前脱敏
- 成本估算：20 页课件 ≈ 1（消化）+ 1（大纲）+ 20×1.3（含修复）≈ **28 次调用**，单页输入 ≤6000 字

## 13. 权限与菜单（三步链，踩坑 #8）

菜单位置已确认（v1.1）：挂在**"智能课程材料管理"目录（menu_id=2087736644369305601）**下，与"细目表管理"(sort_order=1)、"理论知识试题生成管理"(sort_order=2)、"操作技能试题生成管理"并列；新菜单名"AI课件生成管理"，sort_order 取该目录现有最大值+1（写法参照 V20260904_012 / V20260910_012 两条既有种子，实施时先查实际值）。页面：列表/创建/大纲确认/生成进度/导出。

- `sys_menu`：新增菜单 + 按钮权限码 `courseware:project:list/create/confirm/generate/retry/export`（NOT EXISTS 幂等写法，与同目录既有种子对齐）
- `sys_permission` + `sys_role_permission`：授予平台管理员（role_id=2000）
- **种子 tenant_id 一律 10159**（运行租户，勿照抄历史迁移的 1）
- 接口 `@PreAuthorize` 逐点鉴权；数据按 tenant_id 隔离（MyBatis-Plus 多租户插件自动拼，不手写）

## 14. 前端页面（admin-web，ProTable + TanStack Query）

| 页面 | 内容 |
|---|---|
| 课件项目列表 | ProTable：标题/来源文件/状态/进度(x/y)/创建时间；操作：继续、导出、删除 |
| 创建课件 | 上传 docx（现有上传组件）+ 标题/受众（默认"学员自学"）/文字量档位表单 → 提交后轮询解析/消化/大纲状态 |
| 大纲确认 | **按章分组**的页列表表单：章标题、页 title/objective/key_points 可编辑、增删页、拖拽调序（支持跨章移动）；末页复习思考题标识固定不可删；底部"开始生成" |
| 生成进度 | 进度条 + 每页状态格子（generating/ready/failed），10s 轮询（终态停止）；失败页"重试"按钮 |
| 页明细 | 单页 blocks 只读预览（文本化呈现，非画布）+ issues 白话列表 + speaker_notes |
| 导出 | 触发导出 → 有 error 展示问题清单；通过则浏览器下载 pptx（blob 模式，踩坑 #10） |

文案口径：全部业务白话，不出现"槽位/校验器/prompt/消化层"等内部词（消化层阶段对用户显示为"正在整理素材"）。

雪花 ID 一律字符串下发（踩坑 #24，@JsonSerialize ToString 或视图层 String.valueOf）。

## 15. 测试与验收

### 15.1 测试分层与打桩策略

| 层 | 工具 | LLM | 数据库 | 说明 |
|---|---|---|---|---|
| U 单元测试 | JUnit5 + Mockito | 打桩 | 不涉及 | 纯逻辑：解析/校验/状态机/渲染 |
| I 集成测试 | Spring Boot Test | 打桩 | 测试库(gjjddb2026) | 迁移/种子/多租户过滤 |
| EA API 级 E2E | 真实启动 jar + HTTP 调用 | **HttpServer 桩**（必须 `setExecutor`，并发桩否则假死） | 测试库 | 全链路真实请求，模型配置指向桩 baseUrl |
| EU UI 级 E2E | 项目 Playwright + 系统 Chrome 无头 | EA 同环境 | 测试库 | **内置浏览器连不到回环，必须用 Playwright+系统 Chrome**；滑块验证码不自动化，登录接口打桩（成功码字符串 `'0'`）+ 预置 token；长菜单断言需真实 pageMap code |
| S 真实冒烟 | 手工 | **真实 GLM key** | 测试库 | T7 手工执行，留记录 |

既有踩坑提醒：Mockito 参数不要裸值混 `eq()`；并发时序断言要确定性化（CountDownLatch/桩内计数，禁止 sleep 猜时序）。

### 15.2 单元测试清单（U）

| 编号 | 场景 | 断言要点 |
|---|---|---|
| U-01 | DocxSectionParser 标题样式识别 | `Heading N`/`标题 N` 切节、级别正确 |
| U-02 | DocxSectionParser 编号兜底 | `第X章`/`1.1`/`一、`虚拟标题切节、级别按深度 |
| U-03 | DocxSectionParser 表格与空文档 | 表格管道化并入小节；空文档 → 白话失败提示 |
| U-04 | DigestService 结构校验 | topics JSON 校验、非法 source_refs 剔除并记 warn |
| U-05 | DigestService 数字回查 | key_data 中来源文本查不到的数字被剔除 |
| U-06 | DigestService 失败降级 | 两轮校验不过 → cw_digest 无记录、大纲以原始小节为输入 |
| U-07 | OutlineService 钳制 | 章数 [3,6]、页数 [6,50]、非法 page_role 兜底 content |
| U-08 | OutlineService 三页组装 | cover/toc/review 确定性生成；review_questions 缺失时兜底拼装 |
| U-09 | SlideValidator structure | 非法 slot_id/必填缺失/重复填充/类型不接受 四类正反例 |
| U-10 | SlideValidator 其余规则 | capacity/overflow/empty_phrase/thin_content 正反例 + is_repair_worthy 分流 |
| U-11 | 自纠环 | 首轮坏→次轮好走修复；两轮都坏 1 轮上限收口；仅 capacity 告警不触发重写 |
| U-12 | PptxRenderer 回读 | 页数一致、每页非空文本、ea typeface=微软雅黑、页脚来源格式、备注页内容 |
| U-13 | 页脚来源兜底 | 无 heading 用 locator"第 N 段"；仍空该页不渲染页脚 |
| U-14 | 权限 | 无权限码 403（MockMvc + Spring Security Test）；跨租户访问 404/空 |
| U-15 | 大纲确认快照 | 确认后编辑大纲不再影响已初始化的 cw_slide |

### 15.3 集成测试清单（I）

| 编号 | 场景 | 断言要点 |
|---|---|---|
| I-01 | 空库全量 Flyway 迁移 | 成功；5 张新表 SHOW COLUMNS 六列与实体 getter 一一对应 |
| I-02 | 菜单/权限种子 | 重复执行幂等不报错；tenant_id=10159；role 2000 授权可查 |
| I-03 | 多租户过滤 | cw_* 五表查询自动拼 tenant_id；跨租户数据不可见 |

### 15.4 API 级 E2E 清单（EA，真实启动 + 测试库 + LLM 桩）

| 编号 | 场景 | 步骤与断言 |
|---|---|---|
| EA-01 | 全链路 happy path | 上传真实标准 docx → 轮询至 `outline_ready`（断言 cw_source_section>0、cw_digest 有记录、大纲章 3~6、末页 review）→ 确认 → 轮询至 `ready` → 导出 → 下载 → POI 回读页数/页脚/备注 |
| EA-02 | digest 降级链路 | 桩 digest 返回坏 JSON → 项目仍达 `outline_ready`，cw_digest 无记录，大纲以原始小节生成 |
| EA-03 | 大纲编辑后确认 | 改 1 页标题/增 1 页/删 1 页/调序 → 确认 → cw_slide 初始化数量与顺序一致 |
| EA-04 | 未确认不生成 | 确认前 cw_slide 为空、LLM 桩无单页调用记录（不消耗页面 token） |
| EA-05 | 自纠环 E2E | 桩首轮缺必填槽位、次轮正常 → 页 ready 且该页 LLM 调用=2 次 |
| EA-06 | 单页失败隔离与重试 | 某页桩 500 → 该页 failed、其余 ready、项目 partial；POST retry → ready |
| EA-07 | 双租户并发隔离 | 两租户项目同时生成，断言互不可见、互不串数据 |
| EA-08 | AbortPolicy | 占满 coursewareExecutor 后超额提交 → 503"服务繁忙" |
| EA-09 | 导出门禁 | 构造 structure error 残留页 → 导出阻断并返回问题清单；仅 warning → 放行且透出 |
| EA-10 | 幂等 | 重复确认大纲不重复初始化 slide；重复导出内容一致 |
| EA-11 | LLM 审计落库 | 每次调用有记录：用户/租户/模型/prompt 摘要/token/耗时/traceId |

### 15.5 UI 级 E2E 清单（EU，Playwright + 系统 Chrome 无头）

| 编号 | 场景 | 断言要点 |
|---|---|---|
| EU-01 | 菜单可见 | 侧栏出现"智能课程材料管理 > AI课件生成管理" |
| EU-02 | 创建课件 | 上传 docx → 进度阶段推进（"正在整理素材"）→ 大纲确认页**按章分组**展示 |
| EU-03 | 大纲编辑 | 改标题/增删页/拖拽调序 → 开始生成 |
| EU-04 | 生成进度 | 格子状态推进至全部终态，终态后轮询停止（无持续请求） |
| EU-05 | 失败重试 | 失败页点重试 → 状态恢复 ready |
| EU-06 | 导出下载 | 触发 download 事件、文件名中文 `.pptx`；有 error 时展示问题清单不下载 |
| EU-07 | 无权限账号 | 菜单不可见、按钮不展示、直接访问路由被拦 |
| EU-08 | 雪花 id 精度 | 页明细/重试请求中的 id 与列表一致（字符串下发） |

### 15.6 真实模型冒烟清单（S，T7 手工）

| 编号 | 场景 | 断言要点 |
|---|---|---|
| S-01 | 真实 GLM 全程 | 1 份真实标准 docx 跑通全链路；记录 token 用量/耗时/各页 issues 分布 |
| S-02 | 人工打开验收 | PowerPoint/WPS 打开：原生可编辑、中文不乱码、备注页有学习提示、页脚有来源标注、末页为复习思考题 |

### 15.7 验收清单（DoD，逐项勾选；证据见 §19）

**合并硬门槛**（AGENTS.md）：

- [x] `mvn compile -DskipTests` 0 错（2026-09-20 通过）；`npm run tsc --noEmit` 0 错（admin-web 通过）
- [x] 全部新增文件无 BOM（Write 工具写入）；无 `setDeleted(1)` 假删写法（软删统一 Mapper.deleteById）；未修改任何已应用 Flyway 迁移（新增 V20260920_101）
- [x] 目标模式自审：五层清单逐项跑通、无遗留 red（verifier verdict 由用户侧复核）

**功能验收**：

- [x] U-01~U-15 单测 47 个全绿；I-01/I-02/I-03 集成项通过（I-01 空库限制见 §19.4）；EA-01~EA-11 真实启动+测试库+LLM 桩 11/11 绿；EU-01~EU-08 Playwright 8/8 绿；S-01/S-02 真实 GLM 冒烟通过（§19.3）
- [x] 验收主链路：上传《井下支护工》标准 docx → 26 页大纲（6 章按章分组）→ 确认 → 26/26 页并发生成（进度可见、4 次自纠环修复）→ 导出原生 PPTX（末页复习思考题 7 题、内容页页脚来源标注、26/26 备注页）
- [x] 消化层失败降级可用（EA-02 + 冒烟首轮 1210 错误时消化自动降级未阻断）；未确认不消耗页面 token（EA-04：slide 调用=0）；残留 issues 以白话透出（导出 warnings 6 条）
- [x] LLM 调用审计完整（cw_llm_log：digest 1 + outline 1 + slide 30，含 token/耗时/stage，冒烟项目 100% 成功记录）；密钥与模型配置化（exam_faq_model_config + PENDING_USER_INPUT 占位），仓库无硬编码密钥（迁移种子仅占位符）

## 16. 任务分解（目标模式执行单元）

> 依赖序：T1 → T2 → T3 → T4 → T5 → T6(E2E) → T7(冒烟收尾)。T1 的 Flyway/权限种子为单任务独占（不与其他迁移并行）；T6 依赖 T3/T4/T5 全部完成。

| 任务 | 目标 | 主要产物 | 验收 |
|---|---|---|---|
| T1 数据模型 | 5 表迁移（含 cw_digest）+ 实体/Mapper + 菜单权限种子 | `db/migration/V202609xx_0xx__courseware.sql`（先查两库 MAX version）；`courseware/entity|mapper`；菜单种子（tenant_id=10159，role 2000） | I-01/I-02 通过；菜单可见、接口鉴权生效 |
| T2 解析消化大纲 | docx 上传解析落库 + 消化层 + 章→页大纲 + 大纲 CRUD/确认 | `DocxSectionParser`、`DigestService`、`OutlineService`、大纲 ApplicationService/Controller、进度接口雏形 | U-01~U-08、U-15 通过；确认卡点生效 |
| T3 自纠环生成 | 单页生成状态机（含 digest 注入）+ 五类校验器 + 异步编排 + 重试/取消 | `SlideWorkflowService`、`SlideValidator`、`coursewareExecutor`、页级生成 ApplicationService、retry/cancel/progress 接口 | U-09~U-11、U-14 通过；I-03 通过 |
| T4 渲染导出 | POI XSLF 渲染 + 页脚来源标注 + 导出门禁 + 回读验证 + 下载 | `PptxRenderer`、`ThemeConfig`、导出 Controller | U-12/U-13 通过；导出 pptx 回读断言全过 |
| T5 前端 | 6 个页面（大纲按章分组）+ 路由 + 权限码控制 | `apps/admin-web/src/pages/courseware/*` | npm run tsc 0 错；EU-01~EU-08 手工走查路径就绪 |
| T6 E2E 测试 | EA/EU 两清单落地执行 | LLM HttpServer 桩（setExecutor）、EA 测试类、`apps/admin-web` Playwright 脚本 | §15.4 EA-01~EA-11 全绿；§15.5 EU-01~EU-08 全绿 |
| T7 冒烟收尾 | 真实 GLM 冒烟 + 审计核对 + 文档 | S-01/S-02 记录、README/启动说明更新 | §15.7 DoD 逐项勾选完成 |

## 17. 风险与踩坑预案

| 风险 | 预案 |
|---|---|
| GLM JSON 契约不稳（长输出截断/夹 Markdown） | prompt 强约束 + Jackson 严格校验 + 1 次重试；maxTokens 留余量；thinking 走模型可用档位（GLM-5.3-Flash 必须 enabled + low 档） |
| 消化层增加 1 次调用与时延 | 消化与大纲串行异步执行，进度阶段透出（"正在整理素材"）；失败降级不阻断 |
| 消化层 key_data 被模型编造 | 数字回查来源文本剔除（§6.5.3）；大纲/单页 prompt 重申"数字必须来自来源" |
| 章节化大纲 JSON 层级加深、解析失败率上升 | 章节钳制 [3,6] + 1 次重试；仍失败项目置 failed 可重新发起，不进页面生成 |
| 标准 docx 标题样式不规范 | §6 编号正则兜底；以真实样本（osta-standards/ 下既有 docx）做解析对拍 |
| 字宽估算误差致溢出误报/漏报 | 一期估算路径 + overflow 仅 warning 不阻断；后续可换 fontbox 读 TTF 精确度量（jar 内置字体文件） |
| 页数"不限"导致大纲跑偏（太多/太少） | 钳制 [6,50] + 确认界面可增删；prompt 给密度档参考页数 |
| LLM 闸口满载（与 FAQ/试题生成共享 GLM 50 并发） | 页级池 ≤8 远小于闸口；超时按失败页处理可重试，不拖死整份 |
| POI 中文字体 ea 不落盘 | §10.3 双写 latin/ea；单测回读断言 typeface |
| 页脚来源 locator 为空（无标题小节） | 映射不到 heading 时用 locator（"第 N 段"），仍为空则该页不渲染页脚 |
| 导出并发打满内存（大文档逐页渲染） | 导出走同步接口但限单份大小（页数 ≤50）；pptx 体积 MB 级，无流式需求 |
| 历史经验：代理不透传 response_format/tools | 一期均不依赖：JSON 走 prompt 约定；bind_tools 不做 |

## 18. 后续扩展方向（一期不做，接口预留）

1. Web 画布在线编辑器 + bind_tools AI 局部编辑（需扩展 LLM 代理透传 tools 字段；编辑锁与 revision 已在模型层预留）
2. process 步骤条块（口径 12 后置）：横向步骤条渲染（圆角矩形+箭头），适配操作流程类内容
3. flex 布局树 + solver（替换固定模板层，blocks 契约可平滑演进）
4. AI 配图 / 图库（blocks 增加 image 类型 + 图片管线）
5. 多主题与课件模板市场（theme 数据化）
6. SSE 替代轮询（admin-web 引入 EventSource 封装）
7. 每章学习时长估算与学习计划（口径 11 后置项）

## 19. 实施记录（2026-09-20，目标模式执行）

### 19.1 代码清单

| 层 | 文件 |
|---|---|
| 迁移 | `V20260920_101__courseware_init.sql`（6 表 + 菜单权限三步链 + GLM 模型配置种子） |
| 后端 | `courseware/`：6 实体 + 6 Mapper + 4 Controller + 9 Service/Domain（DocxSectionParser、CoursewareAiService、DigestService、OutlineService、ProjectService、GenerationService、SlideQueryService、SlideWorkflowService、PptxRenderer、ExportService）+ 领域（CoursewareLayout/Theme/Validator/Assembler/Json/Models）+ VOs/进度与问题接口 |
| 配置 | `AsyncConfig` 增 coursewareExecutor / coursewareSlideExecutor（AbortPolicy）；`PlatformApplication` 增 mapper 扫描 |
| 前端 | `AiCoursewarePage.tsx`（六视图）+ `coursewareApi.ts` + `main.tsx` pageMap 注册 |
| 测试 | 单测 7 文件 47 例；`CoursewareE2ETest`（EA 11 例，HttpServer 桩）；`e2e/courseware.spec.ts` + `coursewareFixture.ts`（EU 8 例） |

### 19.2 五层测试结果

| 层 | 结果 | 证据 |
|---|---|---|
| U 单元测试 | **47/47 绿** | CoursewareE2ETest 之外 7 个测试类（解析/消化/大纲/校验器/自纠环/渲染回读/JSON） |
| I 集成 | I-02/I-03 通过；I-01 见 19.4 | 迁移重放幂等（0 重复 0 报错）；六表 SHOW COLUMNS 与实体对齐；Mapper 级租户过滤断言（EA-07 内） |
| EA API 级 E2E | **11/11 绿** | 真实启动 + 测试库 gjjddb2026 + HttpServer LLM 桩（setExecutor）：全链路/降级/编辑确认/未确认不生成/自纠环计数/失败隔离重试/租户隔离/AbortPolicy 503/导出门禁/幂等/审计 + U-14 权限 403 |
| EU UI 级 E2E | **8/8 绿** | 系统 Chrome（内置 shell/ffmpeg 未装）+ route 全 mock 夹具：菜单/创建阶段推进/大纲编辑与确认/进度终态停轮询/失败重试/导出中文文件名下载/无权限隐藏/雪花 id 精度 |
| 全量回归 | **1477 测 / 3 失败（均为既有）** | 3 个失败全部在竞赛（Competition）模块 schema 与阶段服务测试，与课件无关；**已在 HEAD(b166d265) 隔离 worktree（不含本次任何改动）复现同样 3 个相同断点失败**，确证非本次引入；课件相关 47U+11EA+8EU 全绿、零回归 |

> EA 套件加固（2026-09-20）：`@BeforeEach` 增加编排/页池排空等待（活动线程 0 且队列空），消除跨用例池饱和导致的偶发 503；ea08 占位时长 30s 保证容量断言确定性。

### 19.3 S 真实冒烟（真实 GLM-5.3-Flash + 真实标准 docx）

素材：`docs/osta-standards/井下支护工.docx`（502KB 真实标准文档）。

- **S-01**：上传→消化→大纲 **42s**（26 页 / 6 章，标题均结论句式）；确认→生成 **28s**（**26/26 ready、0 失败**）；导出不阻断（6 条 capacity 类 warning 透出）；下载 PPTX 114KB，总用时 72s。
- **LLM 审计**：digest 1 次（15.0s / 8588 tokens）+ outline 1 次（19.1s / 10663 tokens）+ slide **30 次**（26 页 + **4 次自纠环修复**，均值 2.7s / 合计 32869 tokens）。
- **S-02 程序化验收**：26 幻灯片 + **26 备注页**；208 处文本节点全部带 `a:ea typeface="微软雅黑"`；**0 张图片**（纯原生对象，非整页截图）、4 个原生表格；末页为复习思考题（7 题）；内容页页脚 `来源:《…》…`；LibreOffice 转 PDF 26 页、中文无乱码、字号层级 34/26/16/15/9pt 正常分档。
- 冒烟中修正：GLM-5.3-Flash **不支持关闭思考**（HTTP 400 / 1210），种子改为 `thinking=enabled + reasoning_effort=low`（与 theory-gen 同口径）。
- 产物保留：`storage/courseware-smoke/井下支护工培训课件.pptx` + `.pdf` + `s01-result.json`（可人工打开做 S-02 终检）。

### 19.4 I-01「空库全量迁移」环境限制说明

数据库账号 gjjd 无 CREATE SCHEMA 权限（仅 gjjddb2026/gjjddbpro 全权，rehearsal/PHP/mvp 库在用不可动），无法在本环境起一个真空白 schema。替代验证：(a) 迁移在测试库全链路应用成功且与既有 150+ 表共存（flyway_schema_history 20260920.101 success=1）；(b) 迁移文件重放幂等（exit 0、菜单 1/权限 6/授权 6/模型配置 1，无重复）；(c) 迁移仅含 `CREATE TABLE IF NOT EXISTS` / `NOT EXISTS` 守卫 / `ON DUPLICATE KEY UPDATE`，唯一外部依赖是更早迁移创建的父菜单（版本序保证先行）；(d) 六表 SHOW COLUMNS 与实体 getter 一一对应（含 BaseEntity 六列）。**建议**：下次生产部署走既有空库/新环境流程时顺带复核。

### 19.6 v1.4 知识点页改造实施记录（2026-09-21）

**背景**：用户验收 v1.3 电工产物后指出第四、五章（基本要求/工作要求）不能只做概括页，要求**每个最小知识点逐页详讲**（示例：1.1.2 常用低压电器 → 标题 + 分类骨架表 + 口诀）。口径经用户逐条确认（AAAAA）：粒度 x.y.z 相关知识要求逐点一页；只做知识列、技能列融为"你会做什么"；基本要求同模式；上限 300 页；口诀页底 note 槽位。

**改动清单**：

| 层 | 内容 |
|---|---|
| 迁移 | `V20260921_101__courseware_knowledge_point.sql`：cw_knowledge_point 表 + cw_outline_page.knowledge_point_id（取号依据：两库 MAX=20260921.020） |
| 解析 | `KnowledgePointEnumerator`：基本要求按三级编号段落切（10 点）；工作要求按表格「相关知识要求」列 x.y.z 条目切（**184 点**，表头/行 gridSpan 逻辑列展开，双路径复核定数；早期按 python-docx 物理列的 198 为误计）；来源 ref 与小节切分 flush 镜像同源 |
| 大纲 | 要求章替换为知识点章：基本要求 1 章 + 工作要求按等级 5 章；识别按章内页 refs 与知识点范围**重叠率 ≥60%**（模型常把章首页挂边界外 ref，全含规则会漏）；无匹配时 review 前兜底插入；页数上限 `platform.courseware.max-pages:300` |
| 生成 | 知识点专用 prompt（结论式标题 + ≤8 行 × ≤5 列骨架表 + note ≤80 字口诀）；`content_table` 模板加 note 槽位；SlideGenInput 加 KnowledgePointPayload，buildInput 按 outline_page.knowledge_point_id 装载 |
| 健壮性 | ① 页级派发加**信号量限流**（233 页打爆页池队列 50 的冒烟事故修复）；② 单页 LLM 调用失败**退避 3s 重试 1 次**（§12 口径，37 次 429 限流全部救回） |

**测试**：courseware 全部 **67/67 绿**（47 原测 + 3 枚举器 + 6 知识点流 + 11 EA）。

**S-03c 真实冒烟（真实 GLM-5.3-Flash + 真实《电工》标准 521KB）**：

- 大纲 129s / **221 页**：认识职业 3 + 等级 3 + 申报 4 + 鉴定 5 + 守则基础 3 + **基本要求 11（1 章首页+10 知识点页）+ 工作要求按等级 39/35/50/44/21（含 184 知识点页）**；重叠规则正确消除了上一轮的重复概括章
- 生成 **698s，221/221 全部 ready，0 失败**（239 次单页调用含 18 次 429 重试救回，合计 ~46 万 tokens，单页均值 11s）
- 导出 823KB 原生 PPTX：示例页「认识结构原理，才能选对低压电器」= 结论式标题 + 4 列骨架表（7 种电器逐一结构/原理/用法）+ 口诀 note + 页脚来源；与用户期望版式一致
- 产物：`storage/courseware-smoke/电工职业技能培训课件.pptx`（供人工终检）

### 19.5 运维要点（部署前）

1. 模型配置 `courseware-gen-glm` 的 `api_key` 为占位符 `PENDING_USER_INPUT`，上线前回填真实 GLM Key；`thinking` 必须保持 `enabled`（该模型不支持关闭），`reasoning_effort` 建议 `low`。
2. 文件存储走 `platform.file.storage-provider`（生产 cos）；冒烟用的 local 仅本机。
3. 权限种子已按 tenant_id=10159 + role_id=2000 下发（菜单在「智能课程材料管理」下，sort_order=4）。
4. 成本参考：一份 26 页课件 ≈ 5.2 万 tokens（3 次编排调用 + 30 次单页调用含修复）。
