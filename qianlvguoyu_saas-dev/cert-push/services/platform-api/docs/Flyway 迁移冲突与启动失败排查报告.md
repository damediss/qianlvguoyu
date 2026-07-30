# Flyway 迁移冲突与启动失败排查报告

> 日期：2026-06-05  
> 项目：`platform-api`  
> 数据库：`gjjd_test`（192.168.1.200:3308）

---

## 1. 问题概述

应用在启动阶段因 Flyway 迁移失败而无法启动，同时伴随 Java 编译错误。共涉及三类问题：

| 类型 | 现象 | 影响 |
|------|------|------|
| Flyway 版本号冲突 | `Found more than one migration with version 20260605.001` | 无法启动 |
| Flyway 失败记录 | `Schema gjjd_test contains a failed migration to version 20260605.010` | 无法启动 |
| Java 编译错误 | `找不到符号 buildDownloadResponse` / `ResponseEntity` | 无法编译 |

---

## 2. 已实施的修复

### 2.1 Flyway 版本号冲突（001 / 002 / 003）

**原因：** 认定模块与题库模块在同一天各自创建了同名版本迁移脚本。

| 原冲突版本 | 认定模块脚本 | 题库模块脚本 | 处理方式 |
|-----------|-------------|-------------|----------|
| `20260605.001` | `create_certification_project.sql` | `add_3_import_permissions.sql` | 保留认定 `001`，题库改为 `029` |
| `20260605.002` | `create_certification_preparation_tables.sql` | `rename_examsub_detail_isJisuan_to_snake.sql` | 保留认定 `002`，题库改为 `030` |
| `20260605.003` | `add_certification_task_scope.sql` | `extend_exam_task_examshicao_detail_pingfen_to_6col.sql` | 保留认定 `003`，题库改为 `031` |

重命名后的文件：

- `V20260605_029__add_3_import_permissions.sql`
- `V20260605_030__rename_examsub_detail_isJisuan_to_snake.sql`
- `V20260605_031__extend_exam_task_examshicao_detail_pingfen_to_6col.sql`

### 2.2 Flyway 010 失败（重复 DDL）

**原因：** 数据库中已通过旧版本号（如 `20260602.006`）执行过相同 DDL，新版 `V20260605_010` 再次 `ADD COLUMN` 导致失败，并在 `flyway_schema_history` 中留下 `success=0` 记录。

**修复：**

1. 重写 `V20260605_010__create_certification_info_management.sql`：
   - 列变更改为基于 `information_schema` 的幂等 `ALTER`
   - 移除与 `6310`/`6930` 冲突的菜单/权限插入（由 `V20260605_012` 使用 `6330`/`6960+` 正确写入）

2. 提供一次性修复脚本 `tools/repair_flyway.py`：
   - 删除 `flyway_schema_history` 中 `success=0` 的失败记录
   - 将 `20260605.011`–`.030` 标记为已成功（库中结构已存在）

### 2.3 Java 编译错误

**原因：** `CertificationPreparationApplicationService` 调用了 `fileApplicationService.buildDownloadResponse()`，但 `FileApplicationService` 中未实现该方法，且缺少 Spring HTTP 相关 import。

**修复：** 在 `FileApplicationService.java` 中补充：

- 方法 `buildDownloadResponse(Long id, boolean inline)`
- import：`ResponseEntity`、`HttpHeaders`、`ContentDisposition`、`MediaType`

---

## 3. 下次还会不会报错？

### 3.1 Flyway

| 场景 | 是否还会报错 |
|------|-------------|
| **全新数据库**（从零跑迁移） | 一般不会。版本号已理顺，`010` 已幂等，029–031 不再冲突 |
| **现有 `gjjd_test` 等旧库** | 需**执行一次** `repair_flyway.py`；之后正常 |
| **以后再次出现同版本号迁移** | **会**。多人未协调 Flyway 版本号时，同类问题会复发 |

### 3.2 Java 编译

当前代码已修复，**不会**再因 `buildDownloadResponse` / `ResponseEntity` 报错，除非新增调用但未实现对应方法。

---

## 4. 根源分析：是否由 damediss push 导致？

**不完全是。** 根因是**多人并行开发 + Flyway 版本号未统一协调**，叠加历史库版本重编号。

### 4.1 Git 提交归因

```
damediss (b693f25) feat(certification): 提交认定管理模块到 dev
  ├─ 认定模块迁移 V20260605_001 ~ V20260605_028
  └─ 调用 buildDownloadResponse()，但 FileApplicationService 未实现

qinjian (2f84cb1 / 426965d) 题库管理更新
  ├─ 题库迁移也使用 V20260605_001 ~ V20260605_003
  └─ → 与认定模块产生 Flyway 同版本冲突

gjjd168 (2ed3a40) fix(db): 完善认定信息库迁移并重新编号 Flyway 脚本
  ├─ 题库脚本重编号为 029 ~ 031
  └─ 扩展 V20260605_010 为幂等 ALTER

历史库 gjjd_test
  └─ 旧版 20260602.x 已执行相同 DDL
  └─ 新版 20260605.010 再次执行 → 失败记录
```

### 4.2 责任划分

| 问题 | 主要来源 |
|------|----------|
| 同版本号冲突（001/002/003） | **qinjian** 与 **damediss** 同一天各写一套迁移 |
| `010` 重复执行失败 | 库中已有旧版 `20260602.006` 记录，新版又跑一遍 |
| `buildDownloadResponse` 找不到 | **damediss** 认定模块调用了未实现的 API |

**结论：** damediss 的 push 是原因之一（认定模块 + 未完成文件下载 API），但不是唯一原因；qinjian 的题库迁移版本撞车、以及 Flyway 重编号后未同步历史库，也各占一部分。

---

## 5. 操作指南

### 5.1 旧库一次性修复（已跑过 20260602.x 的数据库）

```powershell
cd D:\phpApp\qianlvguoyu_saas\services\platform-api
python tools/repair_flyway.py
```

然后重新启动 `platform-api`。

### 5.2 新环境 / 全新数据库

直接启动即可，Flyway 会按顺序执行全部迁移，无需运行 repair 脚本。

### 5.3 若仍报 `failed migration to version 20260605.010`

1. 再次执行 `python tools/repair_flyway.py`
2. 确认 `target/classes/db/migration/` 无旧版冲突文件副本（重新编译）
3. 重启应用

---

## 6. 预防建议

1. **新增迁移前**：查看 `src/main/resources/db/migration/` 中当天最大序号，使用递增版本号，不要复用已有号。
2. **合并前自测**：本地启动一次，确认 Flyway 全部通过。
3. **团队约定**：同一日期版本由一人分配序号，或按模块分段（如认定 `05xx`、题库 `06xx`）。
4. **新同事拉代码**：若连接的是旧开发库，先询问是否需要运行 `repair_flyway.py`。

---

## 7. 相关文件清单

| 文件 | 说明 |
|------|------|
| `src/main/resources/db/migration/V20260605_010__create_certification_info_management.sql` | 幂等 DDL，已移除冲突菜单/权限 |
| `src/main/resources/db/migration/V20260605_029~031__*.sql` | 题库迁移（由原 001~003 重编号） |
| `tools/repair_flyway.py` | 旧库 Flyway 历史一次性修复脚本 |
| `src/main/java/.../file/service/FileApplicationService.java` | 补充 `buildDownloadResponse` 及 import |
| `src/main/java/.../certification/service/CertificationPreparationApplicationService.java` | 考生资料下载调用方 |

---

## 8. 当前 Flyway 版本状态（修复后）

认定模块保留 `20260605.001`–`20260605.028` 连续编号；题库相关脚本位于 `029`–`031`。

旧库经 `repair_flyway.py` 处理后，待 Flyway 自动执行的仅剩：

- `20260605.010`（幂等，对已存在列/表为 no-op）
- `20260605.031`（实操题评分数据 UPDATE，可重复执行）
