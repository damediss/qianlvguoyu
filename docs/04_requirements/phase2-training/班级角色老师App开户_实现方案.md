# 班级角色老师 App 开户 · 实现方案

> **状态**：P0 已落地（指派 ensure + 硬失败 + 单行/批量补开 API + 角色面板开通按钮）；历史批量清零可继续用 ensure-app-accounts  
> **日期**：2026-10-28（P0 落地同日）  
> **范围**：`platform-api`（班级角色指派）+ `admin-web`（开班准备 / 跟班「班级角色」）  
> **对齐**：  
> - [培训班级五角色落地实现方案.md](./培训班级五角色落地实现方案.md) §七（账号开通策略，原定未落地）  
> - [培训模块_App端实现方案.md](./培训模块_App端实现方案.md) §5.1（非学员角色依赖 `platform_user`）  
> - [跟班作业与随堂练实现方案.md](./跟班作业与随堂练实现方案.md)（主讲/助教才可布置）  
> - [学员账号同步_实现方案.md](./学员账号同步_实现方案.md)（复用 `PlatformUserEnsureService`）  
> **原则**：不新建「老师专用账号体系」；不改 App 登录协议；师资库仍只作选人源  

---

## 0. 结论（先看）

**产品一句话**：从师资库（或手工）把人指成理论老师 / 实操老师 / 教务人员时，**必须 ensure 出可登录的 App 账号并回写 `train_class_role.platform_user_id`**，否则指派失败或必须补开成功后才能履职。

| 问题 | 定稿 |
|------|------|
| 账号权威 | 与学员相同：`platform_user`（合并库直写；App 登录仍认该表） |
| 开户入口 | **指派时 ensure**（主路径）+ **未绑定补开**（历史脏数据） |
| 复用组件 | `PlatformUserEnsureService.ensureFromDivisionPerson` |
| 必填 | **18 位身份证 + 11 位手机号 + 姓名**；缺一不可指派需履职的角色 |
| 默认密码 | `123123`（与学员 ensure 一致；`md5(password+stat)`） |
| 失败策略 | **硬失败**：资料不全 / 手机号被他人占用 / ensure SKIPPED → 指派不落库（或事务回滚） |
| 师资库 | **不**在入库时强制开户；选人指派时才开 |
| 班长 | 不在本方案；仍走学员花名册账号 |
| 管理端代布置作业 | 不在本方案（作业方案已定首期否） |

---

## 1. 背景与现状缺口

### 1.1 目标链路（产品期望）

```text
师资库选人 → 指派 lead_teacher / assistant / follow_staff
  → ensure platform_user
  → 回写 train_class_role.platform_user_id
  → 老师用手机号登录 App
  →「我的班级」见主讲/助教台 → 布置作业
```

### 1.2 现网实际

| 环节 | 现状 | 后果 |
|------|------|------|
| `assign` | 仅 `findPlatformUserId(phone, idCard)`，**不 create** | 无账号仍可指派成功，`platform_user_id` 为空 |
| 师资库 | 无 App 账号生命周期；`user_id`=录入员 | 入库 ≠ 可登录 |
| 管理端 | `unboundWarning` 告警文案 | 只提示、无补开、不阻断 |
| 作业 API | `assertTeacher` 要求角色上有该 `platform_user_id` | 未绑定老师登不进 / 布置 403 |
| 五角色方案 §七 | 已写「指派时 ensure」 | **未编码落地** |

### 1.3 数据侧（测试库抽样，2026-10-28）

| 范围 | 现象 |
|------|------|
| `train_teacher_bank` 在册 | 约 113 人；能按手机/身份证对上已有 `platform_user` 的约 40 人 |
| 活跃 `follow_staff` | 12 人中约 11 人 `platform_user_id` 为空 |
| 活跃 `head_teacher` | 7 人中约 6 人未绑 |

说明「配置了老师 ≠ 能登 App」已是常态，不是个例。

---

## 2. 已锁定产品决策

1. **账号模型不变**：老师与学员共用 `platform_user`；不做独立师资登录端。  
2. **开户时机**：以**班级角色指派**为准，不以师资库入库为准（避免未排班老师刷一堆闲置账号；需要时再开）。  
3. **适用角色**：`lead_teacher`、`assistant`、`follow_staff`（含历史码 `head_teacher` 若仍可指派/补开）。`monitor` 不走本方案。  
4. **硬保证**：上述角色指派成功 ⇒ 必有非空 `platform_user_id`；否则不算指派成功。  
5. **资料门槛**：身份证、手机号与学员 ensure 同规；师资库缺项须先补资料再指派。  
6. **冲突处理**：手机号已被**另一身份证**占用 → 指派失败，提示「手机号已被占用，请核对师资库或联系管理员」，**不**静默绑到别人账号。  
7. **已有账号**：身份证命中则 REUSE，只回写绑定，不改密码。  
8. **历史数据**：提供「补开 / 重新绑定」接口与按钮，按班或按角色行补齐。

---

## 3. 非目标

- 改 App 登录 / 方案 A 验票。  
- 师资库批量「一键给全库开户」。  
- 管理端代老师布置作业。  
- 课表 `teacher_name` 字符串自动开户或自动指派主讲。  
- 机构后台 `sys_user` 与 App 账号打通。

---

## 4. 技术方案

### 4.1 指派主路径（改 `TrainClassRoleApplicationService.assign`）

```text
解析 subject（teacher_bank / people / 手工）
  → 汇总 displayName / phone / idCard
  → 若已有 platformUserId（如 people 自带）则校验有效
  → 否则 findPlatformUserId(phone, idCard)
  → 若仍空：ensureFromDivisionPerson(name, idCard, phone, companyId?, orgId?)
       · CREATED / REUSED / UPDATED → 取 platformUserId
       · SKIPPED → throw BusinessException（文案用 warning）
  → 写入/更新 train_class_role.platform_user_id（禁止成功路径留空）
  → VO 增加 bindStatus: bound | ensured | reused（展示用）
```

要点：

| 项 | 约定 |
|----|------|
| `bindPlatformUser=false` | **废弃或忽略**（履职角色不允许「只挂名不开户」）；若保留仅超管调试，默认生产禁止 |
| `companyId` / `orgId` | 优先班级所属企业；师资库 `companyId` 可作回退；与学员 ensure 字段对齐即可 |
| 来源标记 | 日志 / 操作流水 detail 带 `source=class_role`；不必改 `platform_user` 表结构 |
| 事务 | assign 与 ensure 同事务；ensure 失败则角色行不落库 |

### 4.2 补开接口（历史 unbound）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/training/classes/{classId}/roles/{roleId}/ensure-app-account` | 单行补开并回写 |
| POST | `/training/classes/{classId}/roles/ensure-app-accounts` | 本班所有未绑定的履职角色批量补开 |

返回与学员补开类似：`created` / `reused` / `updated` / `skipped` + `warnings[]`。  
`skipped` 行保持 unbound，前端继续标红。

### 4.3 师资库选人校验（前端 + 后端双卡）

选人弹窗 / assign 前：

- 无 11 位手机号 → 不可提交，提示先完善师资库  
- 无 18 位身份证 → 不可提交  

后端再次校验，防止绕过 UI。

### 4.4 管理端 UI（`ClassRolesPanel`）

| 现状 | 改后 |
|------|------|
| 「未绑定」文案 | 保留；并提供 **开通账号** 按钮（调单行 ensure） |
| 仅 info Alert | unboundCount>0 时升为 **warning**，引导补资料或点开通 |
| 指派成功 toast | 若本次 ensured：提示「已开通 App 账号，默认密码 123123，请通知老师首次登录后修改」 |

跟班侧同一面板复用，不另做第二套。

### 4.5 与作业 / App 的衔接（只消费，不改）

- App 登录成功后靠 `platform_user_id` 查 `train_class_role` →「我的班级」。  
- 布置作业继续 `assertTeacher(lead_teacher|assistant)`。  
- 本方案保证：**能指派成功的主讲/助教，一定具备进台前提**。

---

## 5. API 与 VO 变更摘要

### 5.1 调整现有

| API | 变更 |
|-----|------|
| `POST .../roles/assign` | 内嵌 ensure；成功必带 `platformUserId`；失败 4xx + 明确原因 |
| `GET .../roles` | 已有 `unboundWarning`；可增 `bindStatus`（可选） |

### 5.2 新增

| API | 说明 |
|-----|------|
| `POST .../roles/{roleId}/ensure-app-account` | 单行补开 |
| `POST .../roles/ensure-app-accounts` | 本班批量补开 |

### 5.3 错误文案（建议固定）

| 场景 | 文案 |
|------|------|
| 缺身份证 | 请先在师资库补全 18 位身份证后再指派 |
| 缺手机号 | 请先在师资库补全 11 位手机号后再指派 |
| 手机号占用 | 该手机号已绑定其他身份证账号，请核对后重试 |
| ensure 其他 skip | 透出 `EnsureResult.warning` |

---

## 6. 分期与工作量

| 阶段 | 内容 | 验收 |
|------|------|------|
| **P0** | assign 接入 ensure + 硬失败；单测；角色面板开通按钮 + 成功提示 | 新指派主讲必有 `platform_user_id`；手机号可 App 登录 |
| **P1** | 本班批量补开；可选导出本班 unbound 清单 | 历史 follow_staff/head_teacher 可一键清零 unbound（资料齐全者） |
| **P2**（可选） | 师资库详情页「预开 App 账号」（仍写入 `platform_user`，指派时只绑定） | 教务可提前开好再排班 |

建议先只做 **P0**，即可堵住「配了却登不进」的新增量；历史用 P1 清。

---

## 7. 测试要点

### 7.1 单测（platform-api）

- 无账号 + 齐全资料 → CREATED，角色上有 id  
- 已有同身份证账号 → REUSED，不改密码  
- 缺身份证 / 缺手机 → 抛错，库无新角色行  
- 手机号被其他身份证占用 → 抛错  
- `lead_teacher` 指派后 `assertTeacher` 同 id 可通过（可集成轻测）

### 7.2 手工验收

1. 师资库新建老师（姓名+手机+身份证，确保库中无该账号）→ 指派理论老师 → 角色「已绑定」。  
2. App 用该手机号 + `123123` 登录 →「我的班级」见该班 → 主讲台可进布置页。  
3. 故意缺身份证指派 → 失败，列表无脏数据。  
4. 对历史 unbound 点「开通账号」→ 变为已绑定或给出 skip 原因。

### 7.3 回归

- 学员分班 / 花名册导入 ensure 行为不变。  
- 班长仍只从花名册同步。  
- 卸任不删 `platform_user`（账号保留，仅角色结束）。

---

## 8. 风险与对策

| 风险 | 对策 |
|------|------|
| 师资库历史缺身份证，指派变严后教务抱怨 | UI 明确引导先补资料；P1 批量补开时列出 skip 原因 |
| 老师手机号与学员相同但身份证不同 | 硬失败 + 人工改号；禁止绑错人 |
| 默认密码弱 | 与学员策略一致；提示改密；后续若有统一强制改密再复用 |
| 误以为入库师资库即开户 | 文档与 Alert 写清：以指派/补开为准 |
| App 登录库与 SaaS 分裂 | **2026-10-28**：已恢复 `PlatformUserEnsureService` PHP 双写。**踩坑**：默认曾写废弃库 `114:3308/gjjddb`，而 `gjjdp` App 登录读的是 `43:3308/gjjddbpro` → 后台有号、App「账号不存在」。已改 `PHP_MYSQL_*` 默认指向 `gjjddbpro`；`13100412557`/`18076617968` 已手工补入 pro 并可 `123123` 登录。老师指派复用同一 ensure；历史高位孤儿用班级「补开 App 账号」或角色「开通账号」改绑 |

---

## 9. 文档与代码落点

| 落点 | 说明 |
|------|------|
| `TrainClassRoleApplicationService.assign` | P0 主改 |
| `PlatformUserEnsureService` | 只调用，原则上不改；若需 `source` 日志可加可选参数 |
| `ClassRolesPanel.tsx` | 开通按钮 + toast |
| 五角色方案 §七 | 实现后把状态改为「已按《班级角色老师App开户》落地」 |
| 本文件 | 拍板与验收依据 |

---

## 10. 待拍板（默认已按上文取舍）

若无异议，按下列默认执行：

| # | 项 | 默认 |
|---|----|------|
| a | 资料不全时 | **拒绝指派**（硬失败） |
| b | `bindPlatformUser=false` | **生产忽略，强制绑定** |
| c | 是否师资库入库即开户 | **否**（P2 可选预开） |
| d | 默认密码 | **123123**（同学员） |
| e | 首期范围 | **P0 only** |

---

**一句话**：把五角色方案里已经写过、但没做的「指派时 ensure」落地，并加上历史补开；用硬失败保证「配上的老师一定有 App 账号」。
