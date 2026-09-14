# exam_exam_detail 知识归属（knowledge_id）修正方案（含数据去重）

> 目标库：gjjddbpro（生产）。目标表：`exam_exam_detail`（题库试题明细，约 32 万行）。
> 依据：2026-09-12 生产库只读探查（全部为 SELECT，未做任何写入）。
> 状态：**v1.2——路径 B 定稿（源数据已灭失）；取消人工抽验，以三采样自一致投票等工程手段最大化准确率（§4.4）；决策记录见 §8**。
> 关联背景：2026-05-14 起的题库批量导入存在知识点错误挂接 + 大规模重复导入。

### 修订记录

| 版本 | 内容 |
|---|---|
| v1.0 | 初版：问题实证、路径选择、LLM 分类设计、验收与写回框架 |
| v1.1 | 需求方确认：①采纳方案 1（每题保留最早一份副本，幸存集合整体走知识修正）；②去重作为独立阶段并入本方案（Phase 1）；③删除一律**软删除**（deleted=1，不物理删）。量化数据与阶段表同步更新 |
| v1.2 | 需求方确认：①源数据已灭失，**路径 B 定稿**；②**取消人工抽验与验收集**，LLM 结果直接生效（不要求 100% 正确）；③准确率最大化改为工程手段——三采样自一致投票 + 低置信升级旗舰模型 + 置信分层落库（§4.4）。阶段表与写回策略同步更新。**新增 §7 前后端设计**（入口：课程管理 → 资源管理 → 知识归属修正；含菜单权限种子/工作表/后端 API/前端三页签） |
| v1.3 | 新增 **§10 测试与验收清单**（KF-01~24 单测 + 回归门槛 + 演练库彩排 + 生产执行验收），覆盖新增前后端代码与生产数据操作的安全边界 |

---

## 1. 问题实证（先看清问题本身）

### 1.1 知识点错挂（knowledge_id）

| 事实 | 数值 | 来源 |
|---|---|---|
| 2026-05-01 以来导入行数（deleted=0） | **118,205 行** | create_time 范围统计 |
| 其中命中"跨课程同名节点"的行 | **72,682 行（61%）**，62 个节点、62 个课程 | 与同名节点集合联查 |
| 5-14 单日导入 | 71,408 行；4192 节点的 1,179 行写入于 **7 秒内** | SQL 脚本特征 |
| "工匠精神培养/工匠精神"系错挂 | 4 个课程同名节点共 **~5,700 行** | 190/283/180/211 |

根因推断（高置信）：迁移脚本按知识点 **title 查 id 未限定 course_id**。"工匠精神培养"类素养节点名存在于 40~56 个课程的知识树中，同名碰撞导致题目塌缩到本课程的同名节点上。

**课程归属可信（方案核心支点）**：课程名与错挂题干逐一吻合——190 炭极生产工高级↔炭素题、211 母液蒸发工技师↔蒸发题、283 焊接设备操作工初级↔氩弧焊题。错挂在哪个课程的同名节点，题就属于那个课程；错的只是课程内鉴定点。LLM 分类因此是"单课程树内 231 选 1"的封闭候选集问题。

### 1.2 重复导入（v1.1 新增量化）

判重键（已验证可靠）：`题型 + 规范化题干（去 HTML 标签/去空白/去 &nbsp;）+ 规范化答案 JSON（去空白/去空选项）`。
**判重陷阱备忘：不能只用题干**——"下列说法不正确的是()。"这类通用题干首测误报 146 份"重复"，实为 146 道不同题；纳入选项答案后最大真重复为 53 份。

| 层级 | 数量 |
|---|---|
| 5 月以来导入总行数 | 118,205 |
| 规范化后唯一题（type+题干+答案） | **51,120 道** |
| 5 月内部组内冗余（保留一份可删） | 67,085 行（最大真重复 53 份） |
| 与 5 月前历史重复的唯一题 | 26,604 道 / 39,586 行 |
| 5 月新增内容（历史没有的） | 24,516 道 / 78,619 行 |
| **软删除候选合计（§5 Phase 1 口径）** | **39,586 + 54,103 = 93,689 行（79%）** |

副本抽样核验：同一题干 4 份历史副本（含括号空格差异）规范化后归一，答案 JSON 完全一致——删冗余副本无信息损失。

### 1.3 引用面（写回/删除风险，已核实为低）

- 错挂行与 5 月导入行的 `exam_id` 全部为 0 → 未挂接任何试卷/考试
- `exam_user_answer_log` 按 question_id 反查 → 0 条答题记录
- 执行当天仍需重跑 §6.1 复核 SQL（引用面随时间变化）

---

## 2. 目标与非目标

**目标**：
1. **去重（Phase 1）**：对 5 月以来导入行按 §1.2 判重键识别冗余副本，**软删除**（deleted=1）93,689 行，保留每道唯一题最早一份副本；历史已有同题的组保留历史原件、删除全部 5 月副本
2. **知识归属修正（Phase 2~5）**：去重后幸存集合中"内容与当前节点不符"的行，将 knowledge_id 重挂到其所属课程知识树内的正确鉴定点（level 3）

**成功标准**：软删除/修正均清单化、带备份、可整体回滚；自动质量信号达标（试跑自一致率 ≥80%，§4.4）；"属于当前节点"的素养题保留不动；置信不足行保持原节点并输出 flag 清单；全程审计留痕。

**非目标**：不物理删除任何行；不动 2026-05-14 之前存量行的删除与修正（5 月前同组的冗余副本处理为可选延伸，见 §5 Phase 1 备注）；不修改题干内容；不处理其它表。

---

## 3. 路径选择（为什么不是"纯 LLM 全量跑"）

| 路径 | 前提 | 结论 |
|---|---|---|
| A. 确定性重推导 | 5-14 迁移的**源数据**可找到且每题带知识点名称（+课程/工种） | **已关闭——需求方确认源数据已灭失（2026-09-12）** |
| B. LLM 分类（**本方案定稿**） | 源数据不可得或无知识点信息 | 去重后幸存集合中命中同名塌缩节点的行：题干 → 本课程树内两阶段选鉴定点，三采样自一致投票提准（§4.4） |
| C. 全量 11.8 万行无差别复检 | — | 不推荐：39% 未命中同名节点的行无错误证据；去重后更无必要 |

范围界定的原则：**先修"有确定性证据的错"**。同名节点行内部再分两类：内容与节点相符的素养题保留；不符的才重分类（防过度修正）。

---

## 4. LLM 分类设计

### 4.1 数据预处理（确定性，无 LLM）

1. **去重先行**（Phase 1）：幸存集合本身已无重复，**原 v1.0 的"多副本归并"步骤取消**（归并需求已由去重阶段前置解决）
2. **内容规范化**：与判重键同口径——去 HTML 标签、压缩空白，截断至分类所需长度
3. **候选集快照**：从 `exam_knowledge_structure`（course_id 匹配、deleted=0）导出每课程的 L2/L3 节点（id、title、上级路径、important），按课程缓存
4. **分类范围**：幸存集合中命中"跨课程同名塌缩节点"的行（Phase 1 完成后重新量化；预计为 72,682 行的去重幸存子集）

### 4.2 两阶段分类（控制候选集与成本）

- **阶段 1（粗分）**：题干 + 本课程 L2 节点列表（~22 项）→ 选 L2
- **阶段 2（细分）**：题干 + 所选 L2 下的 L3 鉴定点列表（~10 项，含 important 档位）→ 选 L3 + 置信度 + 一句理由
- 相比一次注入全部 231 个 L3 节点（单次 ~4,000+ tokens），两阶段把单次输入压到 ~1,000 tokens 以内
- **成本估算式**：行数 ×（两阶段输入+输出）≈ 1,500 tokens/行；以 Phase 1 后实际行数为准，100 行试跑实测
- **防过度修正内置**：候选集包含"当前节点（工匠精神培养）"，模型判定题干确实属于素养范畴时选它 → 标记 `keep`，不改

### 4.3 调用规范（AGENTS.md LLM 规范落点）

输出 JSON `{"node_id": <int>, "confidence": <0~1>, "reason": "<=50字"}`，解析失败/越界 id 按失败处理（#5 零信任）；置信分层 ≥0.8 / 0.5~0.8 / <0.5；批量执行复用"提交秒回 → 异步分块 → 进度轮询 → 失败自动补跑"架构；LLM 走平台统一入口（#1）、模型配置化（#2）、限流退避（#4）、全量落库审计（#10）。

### 4.4 准确率最大化设计（需求方确认：无人工抽验，LLM 结果直接生效，不要求 100% 正确）

人工验收取消后，准确率只能靠工程手段堆出来。五层防线，全部自动化：

1. **候选集封闭**：只在可信课程树内选（§1.3 课程归属可信），且候选集包含"当前节点"——同时防跨课程漂移与过度修正
2. **上下文增强**：输入 = 题干 + 选项 + 答案 + 解析（去 HTML 规范化后），不只题干；候选节点带上级路径与 important 档位
3. **三采样自一致投票**：同一 prompt、候选顺序随机化、temperature=0.7 采样 3 次——3/3 一致 → 高置信；2/3 → 中置信；三方各不同 → 升级旗舰模型复判
4. **低置信升级旗舰**：自一致不足的行升级 GLM-5.3（非 Flash，thinking=enabled）单次复判；复判后置信仍 <0.5 → **保持当前节点不动**，列入 flag 清单（宁可不改，不改错）
5. **确定性卫兵**：题干与所选节点 title 的关键词重合度作为校验位——选中节点与题干零词重叠且非素养节点 → 降级入 flag 清单

**自动质量信号（替代人工抽检）**：试跑 100 行输出**自一致率**（3/3 一致占比）与旗舰升级率——自一致率 <80% 视为 prompt/候选集缺陷信号，强制调优后才上量；全量跑完输出 apply/keep/flag 三类占比报告。

---

## 5. 执行阶段与工作量

| 阶段 | 内容 | 工作量 | 产出 |
|---|---|---|---|
| Phase 0 | **修脚本匹配逻辑防再脏**（脚本仍在则必须先修，不再阻塞路径）；引用面复核（§6.1 SQL）；分类范围重新量化 | 0.5 天 | 嫌疑行精确数 |
| **Phase 1** | **重复数据软删除**（§6.3）：清单表 → 备份 → 分批置 deleted=1 → 复核 | 0.5~1 天 | 软删除 93,689 行，幸存 51,120 唯一题 |
| Phase 2 | 修正基础设施：修正建议表、内容规范化、课程树快照、分类范围重新量化 | 1 天 | 可执行的基础设施 |
| Phase 3 | 100 行试跑（看自一致率/升级率，<80% 强制调优）→ 全量分批执行（异步+进度+失败自动补跑） | 1~1.5 天 | apply/keep/flag 三类全量清单 |
| Phase 4 | 自动质量报告（自一致率/置信分布/flag 规模）→ 备份 → 按置信分层分批写回 → 复核 → 归档 | 0.5 天 | 修正落地+审计记录 |

Phase 1 备注（可选延伸，默认不做）：5 月前同组的冗余副本（同题在 2024/2025 年的多份历史副本）按同一"保留最早"逻辑顺带软删除，数量执行时统计；默认保持本确认范围（仅 5 月以来行）。

---

## 6. 验收与生产写回

### 6.1 执行当天引用面复核（每次写库前重跑）

```sql
-- 1) 待软删除行/待修正行的试卷挂接（当前为 0）
SELECT SUM(exam_id<>0) FROM exam_exam_detail
WHERE id IN (SELECT detail_id FROM exam_detail_dedup_list WHERE status='approved');
-- 2) 答题日志引用（当前为 0）
SELECT COUNT(*) FROM exam_user_answer_log
WHERE question_id IN (SELECT detail_id FROM exam_detail_dedup_list WHERE status='approved');
-- 3) 知识修正清单同法复核；另用 information_schema 搜含 knowledge_id 列的表逐一核对
```

### 6.2 两张清单表（工作底表 + 审计载体，均建在 gjjddbpro）

`exam_detail_dedup_list`（去重清单）：`detail_id`、`group_key`（MD5）、`is_keeper`（保留/删除）、`keeper_id`（本组保留行）、`create_wave`（2026-05-14 / 07-30 / 07-31 / other）、`status`（pending/approved/done/rolled_back）、`created_at`、`executed_at`。

`exam_detail_knowledge_fix`（知识修正清单）：`detail_id`、`old_knowledge_id`、`course_id`、`new_knowledge_id`、`confidence`、`basis`（llm/manual/rule）、`llm_reason`、`status`（pending/classified/approved/applied/rejected）、`batch_no`、`created_at`、`applied_at`。

### 6.3 Phase 1 软删除执行（需求方确认：只软删，不物理删）

```sql
-- 1) 备份（整行，可完整还原）
CREATE TABLE exam_exam_detail_bak_dedup_2026MMDD AS
SELECT * FROM exam_exam_detail WHERE id IN (SELECT detail_id FROM exam_detail_dedup_list WHERE status='approved' AND is_keeper=0);
-- 2) 分批软删除（每批 1,000 行，核对影响行数与清单一致）
UPDATE exam_exam_detail SET deleted = 1, update_time = NOW()
WHERE id IN (SELECT detail_id FROM exam_detail_dedup_list WHERE status='approved' AND is_keeper=0)
  AND deleted = 0;   -- 防御：已被其它流程删除的行不重复计
-- 3) 复核：幸存集合每组恰一行
SELECT group_key, COUNT(*) FROM exam_exam_detail e JOIN exam_detail_dedup_list l ON l.detail_id=e.id
WHERE l.is_keeper=1 AND e.deleted=0 GROUP BY group_key HAVING COUNT(*)<>1;
```

**回滚**（一条 SQL 整体还原）：
```sql
UPDATE exam_exam_detail e JOIN exam_exam_detail_bak_dedup_2026MMDD b ON b.id=e.id
SET e.deleted = b.deleted, e.update_time = NOW() WHERE e.deleted=1 AND b.deleted=0;
```

清单生成要点（Phase 1 前置）：
- 判重键与 §1.2 完全一致（防通用题干陷阱：**必须含选项答案**）
- 防御性过滤：`is_keeper=0` 的行必须满足 `exam_id=0` 且无答题日志引用（引用面非零的行移入人工复核，不自动删）
- 保留规则：历史已有同题的组 → 保留历史最早行；仅 5 月的组 → 保留 5 月最早行（`ORDER BY create_time, id` 取首行）
- 平台侧语义：deleted=1 后 MyBatis-Plus/@TableLogic 自动过滤，应用层不可见；**不物理 DELETE**（需求方确认）

### 6.4 Phase 4 知识修正写回（无人工验收，按置信分层自动落库）

**落库规则**（§4.4 自一致投票结果直接决定）：
- `apply`：自一致 ≥2/3 且升级复判后置信 ≥0.7 → UPDATE knowledge_id
- `keep`：模型选中当前节点（题干确实属于该素养节点）→ 不动
- `flag`：升级复判后置信仍 <0.5，或确定性卫兵拦截 → **保持当前节点不动**，仅输出 flag 清单（Excel/临时表）供日后处理——宁可不改，不改错

执行步骤：
1. 备份：`CREATE TABLE exam_exam_detail_bak_kfix_2026MMDD AS SELECT * FROM exam_exam_detail WHERE id IN (SELECT detail_id FROM exam_detail_knowledge_fix WHERE status='approved' AND basis='apply');`
2. 分批 UPDATE knowledge_id（仅 apply 行，每批 1,000 行，逐批核对行数）
3. 复核：修正后每题 knowledge_id 的 course_id 未变（课程可信假设不破坏）+ keep/flag 行保持原值
4. 回滚：按备份表反向 UPDATE
5. 派生同步：引用面当前为 0，预计不涉及；执行当天复核结果非零时再评估

---

## 7. 实施载体与前后端设计（v1.2 新增，需求方确认入口：课程管理 → 资源管理）

### 7.1 菜单与权限（Flyway 种子，挂载点已在生产 sys_menu 核实）

- **挂载点**：课程管理（id=4700，directory，/question-bank）下**新建**"资源管理"目录（当前不存在）；其下挂"知识归属修正"页面。与既有理论知识/操作技能生成页面（支持保障 → 智能课程材料管理下）平级独立，不混用
- **菜单 ID 沿用日期序号模式**：资源管理 `202609120101`、知识归属修正 `202609120102`（对齐 2026090400101/2026091000101 先例）
- **权限码 8 条**（sys_permission，挂 knowledge_fix 菜单）：`exam:knowledgefix:view`（查看）、`...:dedup:generate`（生成去重清单）、`...:dedup:apply`（执行软删除）、`...:classify:submit`（提交分类批次）、`...:classify:export`（导出修正清单）、`...:fix:apply`（执行知识写回）、`...:rollback`（回滚，高危独立权限）、`...:report:view`（质量报告）
- **三步链全套**（坑 #8）：sys_menu → sys_permission → sys_role_menu/sys_role_permission（role_id=2000 平台管理员），幂等 NOT EXISTS 守卫，**tenant_id 一律 10159**
- **Flyway 版本号**：执行时双库（gjjddbpro + gjjddb2026）实时 `SELECT MAX(version)` 取号留间隔（踩坑 #7），建议文件名 `V20260912_0XX__knowledge_fix_menu_and_tables.sql`

### 7.2 数据库新增表（同一迁移内，均含 BaseEntity 6 列 + tenant_id=10159 语义）

| 表 | 用途 | 关键列 |
|---|---|---|
| `exam_detail_dedup_list` | 去重清单（工作底表+审计） | detail_id、group_key(MD5)、is_keeper、keeper_id、create_wave、status(pending/approved/done/rolled_back) |
| `exam_detail_knowledge_fix` | 知识修正清单 | detail_id、old/new_knowledge_id、course_id、confidence、basis(llm/manual/rule)、llm_reason、status(pending/classified/approved/applied/rejected)、batch_no |
| `exam_knowledge_fix_batch` | LLM 分类批次 | batch_no、total/done/failed、apply_cnt/keep_cnt/flag_cnt、self_consistency_rate、status、fail_reason |

另加 `exam_faq_model_config` 种子 1 行：`knowledge-fix-pro`（model_id=glm-5.3、thinking=enabled，api_key=PENDING_USER_INPUT 部署回填，与既有 theory-gen-glm 行同 key）；主采样直接复用 theory-gen-glm 行（glm-5.3-flash，key 已配置）。

### 7.3 后端设计（新包 `exam/knowledgefix/`，分层对齐 theorygen）

```
exam/knowledgefix/
  controller/KnowledgeFixController.java
  dto/（DedupStatsVO、FixBatchVO、FixItemVO、QualityReportVO、ApplyResultVO）
  entity/ + mapper/（上表 3 张表）
  service/
    KnowledgeFixDedupService.java      —— 去重清单生成（幂等）+ 软删除执行
    KnowledgeFixClassifyService.java   —— LLM 分类批次编排（三采样自一致 + 旗舰升级 + 补跑）
    KnowledgeFixWriteBackService.java  —— 备份 / 分批写回 / 复核 / 回滚
```

API（`/api/exam/knowledgefix/**`，@PreAuthorize 对应权限码）：

| 方法 | 路径 | 权限码 | 说明 |
|---|---|---|---|
| POST | /dedup/generate | dedup:generate | 生成/刷新去重清单（幂等；判重键 §1.2；防御过滤 exam_id=0 且无日志引用） |
| GET | /dedup/stats、/dedup/list | view | 统计（组数/行数/涉及课程）与清单分页（is_keeper 筛选） |
| POST | /dedup/apply | dedup:apply | 软删除执行：备份表 → 分批 deleted=1 → 复核（§6.3） |
| POST | /classify/submit | classify:submit | 提交 LLM 分类批次（秒回 batchNo；异步分块 + 失败自动补跑，复用理论生成模式） |
| GET | /classify/progress | view | 批次进度（done/total、apply/keep/flag 计数、自一致率） |
| GET | /fix/list、/fix/export | view / classify:export | 修正清单分页（apply/keep/flag 筛选）与 xlsx 导出 |
| POST | /fix/apply | fix:apply | 知识写回执行：备份表 → 分批 UPDATE（仅 apply 行）→ 复核（§6.4） |
| POST | /dedup/rollback、/fix/rollback | rollback | 按备份表整体回滚 |
| GET | /report | report:view | 自一致率/升级率/置信分布/flag 规模 |

实现要点：去重清单生成的规范化 SQL 与 §1.2 判重键**逐字符一致**（防通用题干陷阱回归）；分类 Worker 复用 theorygen 模式（独立线程池 knowledgeFixExecutor、AbortPolicy、TenantContextHolder 参数传递、连续失败熔断、失败自动补跑 3 轮）；三采样=同 prompt 调 3 次（候选顺序随机化）多数投票，升级走 knowledge-fix-pro 配置；LLM 统一走 FaqLlmClient（规范 #1/#2/#4/#5/#10/#11 落点）；软删除与写回操作全量审计（操作人、清单版本、备份表名、影响行数）。

### 7.4 前端设计（apps/admin-web）

```
src/pages/question_bank/
  ResourceCenterPage.tsx      —— 资源管理目录落地页（卡片入口：知识归属修正；预留后续资源类功能）
  KnowledgeFixPage.tsx        —— 三页签工作台
  knowledgeFixApi.ts + types  —— 对齐 theoryGenApi.ts 模式
```

**KnowledgeFixPage 三页签**：

1. **去重清理**：统计卡（总行/唯一题/冗余行/涉及课程）→ [生成清单] → 清单表（is_keeper 徽标、重复份数、题干预览）→ [执行软删除]（Popconfirm 二次确认 + "已自动备份可回滚"提示 + 执行进度）
2. **知识修正**：模型配置说明卡（主采样 glm-5.3-flash / 升级 glm-5.3）→ [提交分类批次] → 进度条（轮询 + 自动补跑状态展示，复用生成页模式）→ 修正清单表（apply/keep/flag Tag 筛选、置信列、reason Tooltip）→ [导出 xlsx] → [执行写回]（Popconfirm + 备份提示）
3. **质量报告**：自一致率、旗舰升级率、置信分布（ECharts）、flag 清单表

交互红线：两个"执行"按钮必须在**清单已生成且备份成功**后才可用；执行中按钮 loading 并轮询进度；回滚入口置于页头下拉（红色、独立权限）；菜单从后端动态加载，前端仅注册路由与组件（既有规范）。

---

## 8. 决策记录（2026-09-12 需求方确认）

| # | 决策点 | 结论 |
|---|---|---|
| 1 | 5-14 迁移源数据下落（决定路径 A 是否可行） | **已确认：源数据已灭失**——路径 A 关闭，路径 B 定稿 |
| 2 | 修正范围 | **已确认：同名碰撞类先行**（5 月以来行 × 跨课程同名节点，软删后幸存集合重新量化）；其余 39% 行先抽检无恙再议 |
| 3 | 验收与写回 | **已确认（v1.2 修订）：取消人工抽验与验收集**，LLM 结果按置信分层直接生效（§4.4/§6.4）；清单→备份→分批→可回滚不变，全程软删除 |
| 4 | 去重保留策略 | **已确认：方案 1**——每题保留最早一份副本，幸存集合整体走知识归属修正 |
| 5 | 删除方式 | **已确认：一律软删除**（deleted=1），不物理 DELETE |
| 6 | 准确率要求 | **已确认：不要求 100% 正确**，以工程手段尽量提高（三采样自一致投票 + 低置信升级旗舰模型 + 置信分层落库 + flag 行保持原节点，§4.4） |

---

## 9. 风险清单

| 风险 | 应对 |
|---|---|
| 无人工验收，LLM 错误修正直接生效 | 三采样自一致投票 + 低置信升级旗舰模型 + flag 行保持原节点（宁可不改不改错）+ 整体备份可回滚；自动质量门：试跑自一致率 <80% 强制调优 |
| 素养题被误改 | 候选集含当前节点 + "属于则保留"判定 + keep 行单独统计报告 |
| 通用题干误判重（已踩） | 判重键必须含选项答案 JSON；上线前用"下列说法不正确的是"类样本回归 |
| 生产 UPDATE/软删误伤 | 清单驱动 + 防御过滤（exam_id=0、无日志引用）+ 前置备份 + 分批核对 + 整体可回滚 |
| 多副本修正不一致 | 去重先行，幸存集合无重复，问题消除 |
| 迁移脚本仍在跑同一映射逻辑持续导入新错行 | Phase 0 必须先修脚本匹配逻辑（按课程限定 title 查询），否则修完还会再脏 |
| 软删除行被 PHP 老系统可见性差异影响 | 同表同字段（deleted）语义一致，两侧同步隐藏；执行后抽验 PHP 侧列表 |

---

## 10. 测试与验收清单（v1.3 新增）

### 10.1 单元测试清单（JUnit 5 + Mockito，编号 KF；LLM 全部 mock 桩响应，不依赖真实模型）

**去重（KnowledgeFixDedupService）**

| 用例 ID | 覆盖项 | 场景与断言要点 |
|---|---|---|
| KF-01 | 判重键规范化 | 4 份已知副本（"（   ）/（ ）/（  ）"空格差异 + HTML 标签 + &nbsp;）归一同一 group_key |
| KF-02 | 通用题干防陷阱 | 题干"下列说法不正确的是()。"、选项答案不同 → **不同组**（不误判重） |
| KF-03 | keeper 选择-历史优先 | 组内含 2024/2025/2026 三份副本 → keeper=最早历史行，5 月副本 is_keeper=0 |
| KF-04 | keeper 选择-仅 5 月组 | 组内仅 5 月副本 → keeper=最早（ORDER BY create_time, id 首行） |
| KF-05 | 防御过滤 | exam_id<>0 或存在答题日志引用的行 → status=manual_review，**不进入自动删除清单** |
| KF-06 | 清单幂等 | generate 连续两次 → 清单行数不变、detail_id 无重复 |
| KF-07 | 软删除执行 | 先建备份表且行数=删除行数 → 分批置 deleted=1 → keeper 行与无关行不受影响 |
| KF-08 | 软删除回滚 | 回滚后 deleted 还原为 0，字段与备份表一致 |

**LLM 分类（KnowledgeFixClassifyService，含升级链路）**

| 用例 ID | 覆盖项 | 场景与断言要点 |
|---|---|---|
| KF-09 | 候选集封闭 | 模型返回非本课程树的 node_id → 该行计失败，**不写入修正清单** |
| KF-10 | 当前节点在候选集 | 候选列表包含 old_knowledge_id 对应节点；模型选中它 → status=keep |
| KF-11 | 自一致 3/3 | 三采样同选 → apply（高置信），仅 1 次 LLM 语义判定计数 3 次采样 |
| KF-12 | 自一致 2/3 | 多数投票 → apply（中置信=2/3） |
| KF-13 | 三方不同 → 升级旗舰 | flash 三次互不相同 → 调 knowledge-fix-pro 复判；复判置信 ≥0.7 → apply；<0.5 → flag |
| KF-14 | 确定性卫兵 | 题干与所选节点 title 零关键词重叠且节点非素养类 → 强制 flag（拦截模型结果） |
| KF-15 | 输出零信任 | 非法 JSON/缺 node_id/越界 difficulty → 该行失败计数，批次不中断 |
| KF-16 | 失败自动补跑 | 首轮部分行失败 → 补跑轮只跑失败行；3 轮耗尽 → 批次 FAILED + 未完成行入 flag |
| KF-17 | 熔断 | 连续失败达阈值（如 key 无效）→ 批次终止，不浪费后续调用 |
| KF-18 | 多租户安全 | Worker 线程 TenantContextHolder set/clear 成对，无泄漏（线程池复用场景二次校验） |
| KF-19 | 审计落库 | 每次 LLM 调用记录 batch_no/prompt 摘要/token/耗时（LLM 规范 #10） |
| KF-20 | 菜单种子幂等 | 迁移重放：sys_menu/sys_permission/sys_role_menu/sys_role_permission 行数不变；tenant_id 全 10159；role 2000 三步链完整 |

**写回与权限（WriteBackService + Controller）**

| 用例 ID | 覆盖项 | 场景与断言要点 |
|---|---|---|
| KF-21 | 写回仅 apply 行 | keep/flag 行 knowledge_id 保持原值；备份表行数=apply 行数；逐批行数核对 |
| KF-22 | 写回复核 | 修正后每行 course_id 未变；keep/flag 行原值未动 |
| KF-23 | 写回回滚 | 备份表反向 UPDATE 后 knowledge_id 全部还原 |
| KF-24 | 权限拦截 | 11 个端点逐一验证：无对应权限码 → 403（@PreAuthorize 生效） |

### 10.2 回归门槛（合并前必须全绿）

1. `mvn compile -DskipTests` 0 错（JDK 21，`-s scripts/custom-maven-settings.xml`）
2. 全量既有测试回归（theorygen/skillgen/exam/certification 至少不新增失败；当前已知 3 个预存在失败见优化方案 §8.2）
3. 新增迁移在 gjjddb2026 重放成功且**重放幂等**（KF-20）；生产 gjjddbpro 仅在发布窗口执行
4. 前端 `npx tsc -b` 0 错；菜单动态加载后新菜单出现在"课程管理→资源管理"下
5. 全部新增文件 UTF-8 无 BOM；不修改任何已应用迁移

### 10.3 真实执行验收（三段式：先演练库彩排，后生产）

**演练（gjjddbpro_rehearsal 库，全流程彩排一次）**：
1. 同一迁移在演练库执行 → 建表+菜单成功
2. 从生产复制嫌疑行子集（如按 batch 抽 2,000 行）到演练库 → 生成去重清单 → 软删除 → 复核幸存唯一性
3. 真实 LLM 分类 100 行（主采样+升级全链路）→ 输出自一致率/升级率 → 写回 → 复核 → 回滚
4. 演练发现的问题全部修复后，才允许上生产

**生产执行（每步前置 §6.1 复核 + 审批窗口）**：

| 步骤 | 操作 | 通过标准 |
|---|---|---|
| E2E-1 | Phase 1 软删除执行 | 备份表行数=删除行数；`deleted=1` 行数与清单一致；应用侧列表行数相应减少 |
| E2E-2 | Phase 3 试跑 100 行 | 自一致率 ≥80%（<80% 禁止上量，回炉调优）；apply/keep/flag 分布合理 |
| E2E-3 | Phase 3 全量分类 | 批次 DONE；失败行经自动补跑后归零或已入 flag |
| E2E-4 | Phase 4 写回 | 仅 apply 行更新；course_id 全部未变；flag/keep 行原值未动 |
| E2E-5 | 归档报告 | apply/keep/flag 占比、自一致率、涉及课程分布落档；备份表与清单表保留 |

### 10.4 最终验收门槛

- [ ] §7 前后端代码全部落地：knowledgefix 包 + 11 个端点 + 3 张工作表迁移 + 菜单/权限种子 + 前端三页签
- [ ] KF-01~24 全绿（10.1）；10.2 回归门槛全过；演练（10.3）问题清零
- [ ] 生产 E2E-1~5 全部通过，flag 清单与质量报告归档
- [ ] 全程：未物理 DELETE、未修改已应用迁移、未提交真实密钥、无 BOM、verifier 每阶段 PASS 证据（编译/测试/DESCRIBE 核对）

### 10.5 实施证据（2026-09-13 执行记录，全部真实退出码核实）

**代码落地**：knowledgefix 包 21 个 Java 文件（3 实体 / 4 Mapper / 6 DTO / 3 服务 / 1 控制器 / KnowledgeNormUtil）+ 迁移 V20260912_001（4 表含 LLM 审计表 + 菜单/权限/角色种子 + pro 模型配置）+ 前端 ResourceCenterPage / KnowledgeFixPage / knowledgeFixApi + main.tsx pageMap 注册 + application.yml knowledgefix 配置段。编译 BUILD SUCCESS。

**单测（KF-01~24 → 25 个测试方法全绿）**：
- KnowledgeNormUtilTest 4 例（KF-01/02 + 空选项剥离 + 卫兵口径）
- KnowledgeFixDedupServiceTest 5 例（KF-05/06/07/08：生成顺序、防御过滤排除、备份先行+分批游标、回滚还原）
- KnowledgeFixClassifyServiceTest 11 例（KF-09 候选封闭 / KF-10 keep / KF-11 3/3 一致自一致率 1.0 / KF-12 2/3 多数 / KF-13 升级 glm-5.3 / KF-14 卫兵拦截 / KF-15 非法 JSON 容错 / KF-16 失败补跑 / KF-17 熔断停派发 / KF-18 三线程租户 12 次调用全 10159 / KF-19 审计 4 行落库）
- KnowledgeFixWriteBackServiceTest 4 例（KF-21/22/23 + 非法备份表名拒绝）
- KnowledgeFixControllerPermissionTest 1 例（KF-24：11 端点权限反射断言）
- 开发期修掉的测试缺陷：① LLM 桩未按阶段路由（stage1 被返回 node_id JSON → 全变 flag）；② 空选项规范化过弱（带/不带空选项的副本未归一，Java 与 SQL 双侧强化为"剥离整个空选项对象 + 收敛悬挂逗号"）；③ MySQL ICU 拒绝 `\{` 转义（改字符类 `[{]`…`[}]`）；④ 夹具层级错标（L2 标成 L3 导致候选集越界）；⑤ 断言计数口径（12 = 3 行 × 4 次调用）

**全量回归**：1323 测试。本次触达模块（knowledgefix / theorygen / skillgen / ai / 前端 tsc）0 失败；5 个失败全部位于未触碰模块（certification DB/工作流 2、competition 窗口/schema 3——docx 测试资源缺失与窗口校验类，与本次改动无关，另行处置）。

**KF-20 迁移幂等**：gjjddb2026 连续执行两遍（退出码均 0），计数稳定：菜单 2 / 权限 8 / 角色菜单 2 / 角色权限 8 / pro 模型 1。开发期踩坑：角色授权固定 ID `202609120301` 与库内既有行撞车（已改 `2026091211xx` 段 + ROW_NUMBER）；首次版本缺 `IF NOT EXISTS` 导致重放报"表已存在"（已补）。

**Phase 1 演练彩排**（gjjddbpro_rehearsal，数据与生产同构 118,205 行）：迁移建表+种子 ✓ → 去重清单生成 14,517 行（演练库为"历史重复组内冗余"子集口径，生产全口径预计 93,689）→ 备份表 14,517 → 软删除 14,517 → 幸存唯一性校验 0 异常 → 回滚还原 14,517 → 5 月行数复原 118,205 ✓。全程零人工干预、可整体回滚。

**无 BOM 终检**：全部新增/修改文件 ✓。

### 10.6 遗留待部署项（生产执行，需审批窗口）

1. V20260912_001 迁移随发布在 gjjddbpro 执行
2. `knowledge-fix-pro` 模型行回填真实 api_key（与 theory-gen-glm 同 key）
3. 页面操作 E2E-1~5：生成清单 → 软删除 → 提交分类（100 行试跑看自一致率 ≥80%）→ 全量 → 写回 → 报告归档
4. 迁移脚本源头修复（防再脏）：脚本已灭失则记录结论关闭；若后续仍会跑同类导入，必须先按课程限定 title 匹配
