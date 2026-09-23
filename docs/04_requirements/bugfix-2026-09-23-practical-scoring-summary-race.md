# 实操评分题级汇总并发竞态修复（B 方案移交文档）

> 日期：2026-09-23
> 状态：**代码已改，待测试与生产验证**（`mvn -DskipTests compile` 通过；未运行测试；生产验证待部署后执行）
> 模块：认定考评 · 实操评分（`certification` / `scoring` 包）
> 严重度：P1 —— 特定时序下考生永久无法完成考试，且界面无报错入口、后台无错误日志，现场极难自助排查

---

## 1. 问题现象（生产实录）

认定考评实操评分场景中，三名考评员对某考生**全部试题**均已提交、界面合计分正常显示，组长点击「完成考试」时弹出红色提示：

> **考生成绩尚未汇总完成**

- 重复点击重复弹（每次都是一个正常的业务拒绝，不是异常）；
- **后台日志没有任何 ERROR/WARN 堆栈**——该提示走的是 `BusinessException(BAD_REQUEST)` 正常返回路径，属预期行为，勿按"日志没报错=没触发"排查；
- 已发生实例：考生**周家增**（`C-20260818152454`，认定任务 `2085264292171644930` / 考生 `2089614500382470146`），该考生共 **6 道题**（前端卡片可能被详情面板挡住只见 5 道）。

## 2. 根因（已定位，证据闭环）

### 2.1 生产数据证据

`certification_practical_question_result`（题级汇总表）该考生 6 行中前 5 行 `completed`，**第 54 题**：

```
paper_question_id=54  status=pending  submitted_count=2  average_score=NULL
```

而 `certification_assessor_task`（评分任务表）第 54 题的 3 条任务**全部 `submitted`**，提交时间线：

| 考评员（尾号） | submitted_at |
|---|---|
| …4418 | 13:07:12 |
| …4274 | 13:07:**19** |
| …5222 | 13:07:**19** |

即：**最后两名考评员同一秒并发提交**，先提交者的事务提交时机落在后提交者事务的 RR 快照建立之后。

### 2.2 代码机制

`PracticalScoringApplicationService`（行号为 dev `bfd4b4b2` 快照，可能漂移）：

- 两个提交入口（主提交 `submitScores` 约 :2640-2654；调整重提约 :730-752）都会在同事务内调用 **`calculateResults(task)`**；
- `calculateResults`（约 :2655-2706）的逻辑顺序：
  1. `questionResultMapper.lockResult(...)` —— `FOR UPDATE` 锁题级汇总行；
  2. **普通 `selectList` 统计**该题 `status='submitted'` 的任务数（:2662-2667）；
  3. 满 3 人 → 写 `completed` + 均分；否则写 `pending` + 计数。
- 问题在步骤 2：**MySQL 默认 REPEATABLE READ 下，一致性快照在事务首次读时建立**。后到事务（…5222）请求早期的读（加载任务详情等）建立了快照 → 随后虽拿到行锁，这条计数仍是**快照读**，看不到先到事务（…4274）刚提交的 `submitted` → 数出 2 → 写回 `pending/submitted_count=2`。
- **永不自愈的原因**：「完成考试」入口 `completeCandidate`（约 :783-811）只调 `calculateCandidateResult` 重算**人级**行，**从不重算题级行**；题级行卡在 `pending` → 人级 `completed_question_count=5/6` → 永远抛"考生成绩尚未汇总完成"（:807）。

**这与踩坑库 #27（AI复习物化竞态）是同一类 bug：锁了汇总行，但任务统计仍使用过期快照。** 修复需要让同一事务中的每次统计都能看到语句开始时已提交的任务状态；仅增加行锁不会刷新 REPEATABLE READ 快照。

### 2.3 正常逻辑参考（第 54 题的正确值）

三名考评员分数 5.00 / 2.00 / 2.00 → 均分 3.0（`HALF_UP` 保留 1 位），分差 3 ≤ `MAX_ASSESSOR_SCORE_DEVIATION`（10）→ 应 `completed`，人级总分 84.0。

## 3. 影响面排查（修复前后都建议跑）

找出所有"卡住的题级行"（pending 但 3 任务已提交）：

```sql
SELECT qr.task_id, qr.candidate_id, qr.paper_question_id
FROM certification_practical_question_result qr
WHERE qr.deleted = 0 AND qr.status = 'pending'
  AND (SELECT COUNT(*) FROM certification_assessor_task t
       WHERE t.task_id = qr.task_id AND t.candidate_id = qr.candidate_id
         AND t.paper_question_id = qr.paper_question_id
         AND t.deleted = 0 AND t.status = 'submitted') = 3;
```

注意：此 SQL 仅能在**与库同环境的会话**判断（生产只读查询可跑；写操作仅部署方/用户在服务器执行）。

## 4. 修复方案与实施情况

### 4.1 B1 根治：汇总事务使用 READ COMMITTED

提交、异常分数调整重提、退回以及完成考试入口使用 `Isolation.READ_COMMITTED`。在题级行 `FOR UPDATE` 串行化同题汇总后，submitted 任务统计仍使用普通查询；READ COMMITTED 使每条查询读取该语句开始时已提交的数据，并包含本事务自己的任务状态更新。

此处不再对所有 submitted 任务行追加 `FOR UPDATE`：提交链路先锁考评员自己的任务行、后锁题级汇总行；反向锁住全部任务行会引入交叉等待风险。

`calculateResults` 与完成入口自愈共用 `submittedTasks(...)` 和 `recomputeQuestionResult(...)`，避免两条路径的计数、偏差阈值和均分口径分叉。

### 4.2 B2 加固：完成入口自愈（已实施）

`completeCandidate` 校验该考生所有考评任务已提交后，按 `paper_question_id` 升序遍历非 `completed` 的题级行并锁定行，读取已提交任务后调用共享计算逻辑，再重算人级行。这样：

- 存量卡住的考生（含周家增这类已按旧逻辑写入的 pending 行）**部署后重试点「完成考试」即自动修复**，无需手工改库；
- 未来任何原因（含 B1 未覆盖到的路径）造成的同类卡态，完成入口兜底。

实施要点：

1. 共用计算已抽取为 `recomputeQuestionResult(questionResult, submittedTasks)`，提交路径和修复路径调用同一实现。
2. `completeCandidate` 在重新计算人级结果之前修复所有非完成题级行：

   ```java
   repairIncompleteQuestionResults(taskId, candidateId);
   calculateCandidateResult(taskId, candidateId);
   CertificationPracticalCandidateResultEntity result = selectCandidateResult(...);
   if (result == null || !"completed".equals(result.getStatus())) {
     if (result != null && "review_required".equals(result.getStatus())) {
       throw new BusinessException(BAD_REQUEST, "三名考评员评分偏差超过10分，请审核并退回修正后再完成考试"); // 语义不变
     }
     throw new BusinessException(BAD_REQUEST, "考生成绩尚未汇总完成"); // 文案不变
   }
   ```

3. `repairIncompleteQuestionResults` 语义：
   - 遍历该考生题级行（含 `pending`，以及防御性处理 `review_required`——重算后若偏差仍 >10 会自然回到 `review_required`，偏差提示文案不变）；
   - 对每题先按固定顺序锁题级汇总行，再以 READ COMMITTED 查询已提交任务集；
   - **任务行缺失（汇总行存在但无任何任务行）时不建任务行**（任务行是考务生成的，不属于本修复范围），该题保持原状并在日志 WARN 留痕（含 taskId/candidateId/paperQuestionId）；
   - 汇总行本身不存在的情况：不在本次修复中生成考评任务或汇总行；人级汇总继续按现有逻辑落 `pending`。
4. 修复路径在 `completeCandidate` 的 READ COMMITTED 事务内执行，与正常提交共享同题汇总行锁。偏差仍超限时用专属业务异常保留原提示，同时允许刚重算出的 `review_required` 题级与人级状态提交，避免每次点击都只在事务内临时看到审核状态。

### 4.3 不做的事（范围边界）

- 不改表结构、不加 Flyway 迁移；
- 不改「三名考评员全部提交后才能完成考试」「评分偏差超过10分」两条既有业务文案与分支语义；
- 不动任务生成链路（`ensure*` 系列保持原样）；
- 生产已手工修复的行不需要回滚或订正。

## 5. 测试要求

1. **并发回归（核心）**：模拟两名考评员同一题**并发提交**（两个线程各提交一个不同考评员任务，可用 `CountDownLatch` 对齐提交时机；真实库用例参照 `CoursewareE2ETest` 的 `@SpringBootTest + courseware-e2e profile` 模式）。断言：题级行最终 `completed`、`submitted_count=3`、均分正确；人级行 `completed`、总分正确。
   - 注意时序确定性：若用例不稳定，宁可做成"先提交线程落库后第二个线程才开始"的确定性版本 + 单独标注并发用例，不要留随机挂的用例（本仓库惯例，见 knowledgefix 并发用例确定性化经验）。
2. **自愈用例（Mockito 单测，确定性）**：种数据——某题题级行 `pending/submitted_count=2`，3 条任务 `submitted` → 调 `completeCandidate` → 断言题级行被重算为 `completed`、方法返回正常视图；以及偏差 >10 的题重算后仍走"偏差超过10分"文案。
3. **回归**：`mvn compile -DskipTests` 0 错；`mvn test` 全量不新增失败（当前基线：1622 用例中 4 个竞赛模块既有契约失败为已知项）；`npm run build` 与本修复无关可跳过；无 BOM。
4. **生产验证（部署后）**：用第 3 节排查 SQL 确认存量卡态清零；对历史卡住考生重试点「完成考试」可自愈即验收通过。

## 6. 临时缓解手段（已在生产用过，留档备查）

任选其一即可解封单个考生，均不需发版：

- **UI 重提**：实操管理页展开考生行 → 在卡住那道题（3 任务已提交但汇总未完成的题）任一考评员任务上「修改分数」→ 分数不动 → 「保存并重新提交」。新事务当前计数=3，立即 completed。
  - ⚠️ 不要点「退回」——退回会 `resetScoresToDefault` 清空该题分数，需重新录入。
- **单行长 SQL**（仅题级行；人级行勿动，点完成考试时代码会自算）：

  ```sql
  UPDATE certification_practical_question_result
  SET status='completed', submitted_count=3,
      average_score=<三人均分>, final_score=<三人均分>,
      calculated_at=NOW(), updated_at=NOW(), updated_by=1
  WHERE id=<题级行id> AND status='pending' AND deleted=0;  -- 保护条件：仍 pending 才生效
  ```

  周家增案例的参考值：第 54 题（行 id `2101558414025056257`，tenant 10159），均分 3.0。

## 7. 关键位置速查（dev `bfd4b4b2` 快照，行号可能漂移）

| 内容 | 位置 |
|---|---|
| 报错抛出点 | `PracticalScoringApplicationService.completeCandidate` :807 |
| 完成入口整体 | 同文件 :783-811 |
| 题级计算（B1 已改） | 同文件 `calculateResults`（`submittedTasks` + `recomputeQuestionResult`） |
| 人级计算 | 同文件 `calculateCandidateResult` :2708-2743 |
| 两个提交入口 | 同文件 :2640-2654（主提交）、:730-752（调整重提） |
| 汇总行生成（ensure*） | 同文件 :2953-2956（调用点）、:3590-3650 |
| 题级行锁参考写法 | `CertificationPracticalQuestionResultMapper.lockResult`（FOR UPDATE + 租户条件） |
| 同类事故 | 踩坑库 #27（RR 快照读 vs 当前读，含修法与约定） |

## 8. 交接备注

- 修复分支命名建议：`fix/<task-id>-practical-scoring-summary-race`；提交格式 `fix(backend): 实操评分题级汇总并发竞态修复（READ COMMITTED+完成入口自愈）`，body 关联本文档。
- 本修复上线需**重新打包部署 platform-api**；部署后历史卡态由 B2 自愈，无需数据订正脚本。
- 修复完成后请同步把本类事故沉淀为踩坑库条目（参照 #27 格式：现象/根因/修法/约定）。
