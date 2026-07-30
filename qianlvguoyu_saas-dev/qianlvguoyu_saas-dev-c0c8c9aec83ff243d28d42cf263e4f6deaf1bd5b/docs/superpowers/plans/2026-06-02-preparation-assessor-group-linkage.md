# Preparation Assessor Group Linkage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在“筹备期-考评员配置”中，成员/组长候选改为复用“信息管理-考评员信息”的可用考评员（已入库+空闲+未过期），并按“多选成员 → 创建考评组 → 从已选成员选组长 → 保存成功”的交互完成配置，同时后端做工作状态联动。

**Architecture:** 前端用信息管理接口 `GET /certification/info/assessors/available` 拉取候选并支持多选；保存时复用现有筹备期接口：先建组、再循环加成员（组长成员标记 `taskRole=leader`）。后端在“加成员/移除成员/发放任务”时写回信息管理表的 `workStatus`，并校验候选必须为“已入库+空闲”。

**Tech Stack:** React + antd + React Query（前端）；Spring Boot + MyBatis Plus（后端）。

---

## File map (create/modify)

**Frontend**
- Modify: `apps/admin-web/src/pages/certification/preparation/AssessorConfig.tsx`
- Modify: `apps/admin-web/src/api/certificationInfo.ts`（新增 `listAvailableAssessors` 方法）
- Modify: `apps/admin-web/src/types/certificationInfo.ts`（若需要补齐用于下拉/列表的字段，或新增轻量类型）

**Backend**
- Modify: `services/platform-api/src/main/java/com/guoyu/platform/certification/service/CertificationPreparationApplicationService.java`
- Modify: `services/platform-api/src/main/java/com/guoyu/platform/certification/info/application/CertificationInfoApplicationService.java`（如需新增“按 id 获取并校验可用”的复用方法，可加 package-private helper）
- Modify: `services/platform-api/src/main/java/com/guoyu/platform/certification/info/mapper/CertificationInfoAssessorMapper.java`（若需要新增按 id/批量更新 workStatus 的 mapper 方法）
- Modify: `services/platform-api/src/main/java/com/guoyu/platform/certification/info/entity/CertificationInfoAssessorEntity.java`（仅当缺字段/枚举不全时）

**Tests**
- Modify/Add: `services/platform-api/src/test/java/com/guoyu/platform/certification/service/CertificationPreparationApplicationServiceTest.java`

---

### Task 1: Frontend — 引入“可用考评员”候选 API

**Files:**
- Modify: `apps/admin-web/src/api/certificationInfo.ts`
- Modify: `apps/admin-web/src/types/certificationInfo.ts`（如需要）

- [ ] **Step 1: Add API function `listAvailableAssessors`**

在 `apps/admin-web/src/api/certificationInfo.ts` 追加：

```ts
export function listAvailableAssessors(keyword?: string) {
  return unwrap<AssessorInfo[]>(apiClient.get(`${base}/assessors/available`, { params: { keyword } }));
}
```

- [ ] **Step 2: Type check**

确认 `AssessorInfo` 已包含 `id/assessorName/certificateNo/workTypeScope/levelScope/validUntil/auditStatus/workStatus` 等字段；若后端 VO 缺字段导致 TS 不匹配，则新增一个轻量类型：

```ts
export interface AvailableAssessorInfo {
  id: string;
  assessorName: string;
  certificateNo: string;
  workTypeScope?: string;
  levelScope?: string;
  validUntil?: string;
  workStatus?: string;
}
```

并让 `listAvailableAssessors` 返回 `AvailableAssessorInfo[]`。

- [ ] **Step 3: Frontend build sanity**

Run (from `apps/admin-web`): `pnpm -s lint` (or `pnpm -s typecheck` if available)  
Expected: no TypeScript errors related to the new API.

---

### Task 2: Frontend — 改造“添加成员 → 创建考评组”两步弹窗

**Files:**
- Modify: `apps/admin-web/src/pages/certification/preparation/AssessorConfig.tsx`

- [ ] **Step 1: Replace old single-select member form**

移除/弃用现有 `memberForm`（单选 `assessorId`），改为弹窗内部两步状态：

- `selectedAssessorIds: string[]`
- `memberStep: 'pick_members' | 'create_group'`
- `candidateKeyword: string`

候选列表数据用 `listAvailableAssessors(keyword)` 拉取（React Query），并在弹窗展示 Table + rowSelection(multiple)。

- [ ] **Step 2: Implement Step 1 UI (multi-select)**

在“添加成员”弹窗中：

- 顶部 `Input.Search`（keyword）
- `Table` 列：姓名、证号、工种范围、等级范围、有效期至
- `rowSelection` 多选，值写入 `selectedAssessorIds`
- 底部按钮：取消 / 下一步（或“创建考评组”）

前端校验：`selectedAssessorIds.length > 0` 才能进入下一步，否则 `message.warning('请至少选择 1 名成员')`。

- [ ] **Step 3: Implement Step 2 UI (create group & leader)**

Step 2 表单字段（复用现有 `groupForm`）：

- `groupName` required
- `workTypeName` default = `task?.workTypeName`
- `leaderAssessorId` required，options **只来自已选成员**：

```ts
const selectedCandidateOptions = candidates
  .filter(x => selectedAssessorIds.includes(String(x.id)))
  .map(x => ({ label: `${x.assessorName || '-'} ${x.certificateNo || ''}`, value: String(x.id) }));
```

并增加校验：leader 必须在 `selectedAssessorIds` 内（保存前校验）。

- [ ] **Step 4: Save flow (create group then add members)**

保存按钮点击：

1) `createPreparationAssessorGroup(taskId, { ...groupValues, status:'preparing' })`
2) 循环 `selectedAssessorIds` 调用 `addPreparationGroupMember(taskId, groupId, { assessorId, taskRole, roleName })`
   - leader: `taskRole='leader'`, `roleName='组长'`
   - others: `taskRole='member'`, `roleName='成员'`

全部成功后：

- `message.success('配置成功')`
- 关闭弹窗、清空选择与步骤
- `refresh()`（现有刷新逻辑）

错误处理：任一步失败都 `message.error(err.message || '保存失败')`，并保留弹窗状态便于重试。

- [ ] **Step 5: Frontend smoke test**

Run (from `apps/admin-web`): `pnpm -s build`  
Expected: build success.

---

### Task 3: Backend — 成员来源切换到“信息管理考评员”并校验可用性

**Files:**
- Modify: `services/platform-api/src/main/java/com/guoyu/platform/certification/service/CertificationPreparationApplicationService.java`
- Modify: `services/platform-api/src/main/java/com/guoyu/platform/certification/info/mapper/CertificationInfoAssessorMapper.java`（必要时）
- Modify: `services/platform-api/src/main/java/com/guoyu/platform/certification/info/entity/CertificationInfoAssessorEntity.java`（必要时）

- [ ] **Step 1: Add dependency on info assessor mapper**

在 `CertificationPreparationApplicationService` 注入 `CertificationInfoAssessorMapper`（信息管理表）：

```java
private final CertificationInfoAssessorMapper infoAssessorMapper;
```

并在构造函数参数中加入它。

- [ ] **Step 2: Replace `requireAssessor` usage in `addGroupMember`**

将现有：

```java
CertificationAssessorEntity assessor = requireAssessor(req.assessorId());
```

替换为读取信息管理：

```java
CertificationInfoAssessorEntity assessor = requireInfoAssessor(req.assessorId());
requireInfoAssessorAvailable(assessor);
```

其中 `requireInfoAssessorAvailable` 校验：

- `auditStatus == "approved"`
- `workStatus == "idle"`
- 未过期（若 `validUntil` 不为空则 `!validUntil.isBefore(LocalDate.now())`）

校验失败抛 `BusinessException(ErrorCode.BAD_REQUEST, "...")`：
- 未入库：`"考评员未入库，不能分配"`
- 非空闲：`"考评员非空闲，不能分配"`
- 过期：`"证书已过期的考评员不得分配任务"`

- [ ] **Step 3: Map fields into task member entity**

使用信息管理字段赋值：

- `assessorName`
- `certificateNo`
- `workTypeScope`
- `levelScope`（如有）
- `validUntil`

保持现有任务校验“工种覆盖/重复分配”等逻辑。

---

### Task 4: Backend — 写回信息管理 workStatus（preparing / assigned / idle）

**Files:**
- Modify: `services/platform-api/src/main/java/com/guoyu/platform/certification/service/CertificationPreparationApplicationService.java`
- Modify: `services/platform-api/src/main/java/com/guoyu/platform/certification/info/mapper/CertificationInfoAssessorMapper.java`（必要时）

- [ ] **Step 1: On add member, set info `workStatus=preparing`**

在 `addGroupMember` 成功插入 `CertificationTaskAssessorEntity` 后：

```java
assessor.setWorkStatus("preparing");
infoAssessorMapper.updateById(assessor);
```

并考虑并发：更新前再检查仍为 `idle`，否则抛错（可用 where 条件更新，受 mapper 支持程度决定）。

- [ ] **Step 2: On dispatch, set info `workStatus=assigned` for all members**

在 `dispatchGroup` 循环成员时，除了更新任务侧 member `workStatus="assigned"`：

```java
CertificationInfoAssessorEntity info = infoAssessorMapper.selectById(member.getAssessorId());
if (info != null) { info.setWorkStatus("assigned"); infoAssessorMapper.updateById(info); }
```

- [ ] **Step 3: On remove member, release to idle if no longer used**

在 `removeGroupMember` 删除成员记录后：

- 获取被删成员的 `assessorId`（删除前先查出 member）
- 判断该 `assessorId` 是否仍存在任何 `CertificationTaskAssessorEntity`（未删除）记录：
  - 若没有：信息管理 `workStatus="idle"`

> 若系统支持“一人多任务并行”，释放条件应是“没有任何未完成/未删除的任务占用记录”；本次以“任务成员表是否仍有记录”为准。

---

### Task 5: Backend tests — 覆盖关键联动

**Files:**
- Modify/Add: `services/platform-api/src/test/java/com/guoyu/platform/certification/service/CertificationPreparationApplicationServiceTest.java`

- [ ] **Step 1: Add test for `addGroupMember` requires approved+idle**

编写测试数据：

- 插入一条 `CertificationInfoAssessorEntity`：`auditStatus=approved`, `workStatus=idle`
- 调用 `addGroupMember` → 期望成功，且信息管理 `workStatus` 变为 `preparing`

再覆盖失败路径：

- `auditStatus=pending` → BAD_REQUEST
- `workStatus=assigned` → BAD_REQUEST

- [ ] **Step 2: Add test for dispatch updates info workStatus**

在已有组+成员基础上调用 `dispatchGroup`：

- 期望信息管理 `workStatus` 变为 `assigned`

- [ ] **Step 3: Add test for remove releases idle**

移除成员后：

- 若该考评员不再在任何任务成员表出现：信息管理 `workStatus` 变回 `idle`

- [ ] **Step 4: Run backend tests**

Run (from `services/platform-api`): `mvn -q test`  
Expected: tests pass.

---

## Plan self-review (against spec)

- 覆盖 spec 的三块：候选取数（available）、两步弹窗（多选→选组长→保存）、后端联动（preparing/assigned/idle）。
- 无占位符：每个步骤给出明确文件与代码片段/命令。
- 名称一致：`auditStatus=approved`、`workStatus=idle/preparing/assigned` 与现有前端 label 映射一致。

