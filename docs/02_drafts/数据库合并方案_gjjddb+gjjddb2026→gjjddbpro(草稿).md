# 数据库合并方案：gjjddb + gjjddb2026 → gjjddbpro（草稿，待审核)

> 日期:2026-08-20
> 状态:**草稿，未经审核，未执行任何变更**
> 分析数据来源：对两套生产库的实时只读盘点（information_schema + ID 重叠分析），原始数据在 `outputs/dbmerge-analysis/`

---

## 1. 目标与原则

- 在 43.138.163.7:3308 新建库 `gjjddbpro`，将 PHP 生产库 `gjjddb`(114.132.67.185:3308）与 SaaS 库 `gjjddb2026`(43.138.163.7:3308）合并为一个生产库
- 合并后 PHP 应用与 SaaS 应用（platform-api）同时使用 `gjjddbpro`
- 数据优先级：同名表默认以 `gjjddb`（现生产）为准；**承载 SaaS 核心功能的表以 `gjjddb2026` 为准**
- 原则：稳妥优先、停机窗口可控、可回滚

## 2. 现状盘点（实测）

| 项 | PHP 库 gjjddb | SaaS 库 gjjddb2026 |
|---|---|---|
| MySQL 版本 | **5.7.44** | **8.4.9** |
| 表数量 | 338 | 426 |
| 数据量 | ~490 MB / ~104 万行 | ~327 MB / ~45 万行 |
| 引擎 | InnoDB 为主，**7 张 MyISAM** | 全部 InnoDB |
| 字符集 | 267 utf8mb4 + **70 张 utf8(3 字节）** + 1 utf8mb4_unicode | 317 utf8mb4_unicode + 51 utf8mb4_general + 41 utf8mb3_bin + 17 utf8mb4_0900 |
| 视图/触发器/存储过程 | 0 | 0 |
| 外键约束 | 63（含 gxwebsoft_core) | 55 |
| Flyway | 无 | **flyway_schema_history 417 条** |

**重要发现 —— 实际是三个库，不是两个**:PHP 应用是双库架构，除 `gjjddb` 外还依赖同实例的 `gxwebsoft_core`(151 张表，~65MB，含 PHP 云平台账号 `sys_user`、字典 `sys_dict/sys_dict_data`、CMS 等，PHP 模型继承 `ModelBaseSub` 走此库）。合并方案必须一并处理它，否则 PHP 上线即故障。

### 同名表（121 张）实测结论

- **结构**：121 张全部存在差异，但绝大多数是 5.7 vs 8.4 的显示性差异（`int(11)`→`int`、`timestamp`→`datetime`、`DEFAULT_GENERATED` 等），语义级差异逐表见 `outputs/dbmerge-analysis/schema_diff_report.txt`
- **数据重叠**(ID 全量比对，见 `id_overlap_report.csv`)，分四类：

| 类别 | 特征 | 表数量 | 例子 |
|---|---|---|---|
| A. SaaS 是 PHP 的快照子集（或为空） | only_saas = 0 | ~95 | exam_exam_detail(32.5万⊂37.3万）、exam_result(SaaS 空）、train_exam_people_list(SaaS 空）、train_course(SaaS 空）、platform_user_token（完全相等） |
| B. 两边各有独有行，但 ID 段不相交 | 可直接并集 | ~15 | train_people_list(PHP 153..2132 / SaaS 60001+)、train_sign_up_people_self(PHP 369..3863 / SaaS 91009+)、train_teacher_bank、course_kejian |
| C. 有交集且各有独有行 | 需逐表决策 | ~8 | platform_user(PHP 4760 + SaaS 独有 3 行 id≥1000000001)、platform_user_detail(SaaS 独有 4)、train_info_class、train_info_list、train_sign_up_self、train_solution、train_file_list、task_* 若干 |
| D. 同名不同义的系统表 | 结构语义完全不同 | 3+ | sys_user、sys_tenant、sys_dict |

- **D 类已实测化解**:PHP 代码中 `SysUser::`、`SysTenant::` 零调用（gjjddb 的 sys_user/sys_tenant 是死表）;PHP 的字典走 gxwebsoft_core 的 sys_dict;PHP 后台登录用 `sys_qlgt`(PHP 独有表，无冲突）。**结论：D 类全部以 SaaS 为准，PHP 侧数据归档不迁移。**
- 内容抽验：platform_user 4760 行重叠 ID 中仅 1 行内容有差异（快照基本未漂移）

### SaaS 侧存在活跃的 PHP→SaaS 同步通道

`platform-api` 的 `integration/sync`(PlatformSyncService/Scheduler）每周期从 PHP 库同步 platform_user、platform_user_token、exam_exam_detail、exam_knowledge_structure、train_exam_list、train_exam_people_list、exam_course 等到 SaaS 库——这就是 A 类表"快照子集"的成因。**合并后该通道变成同库自同步，必须下线**，另有学员开户双写（`user-provisioning.php-write-enabled`）需改为单写。

## 3. 合并策略（按表分类处置）

| 类别 | 表 | 处置 |
|---|---|---|
| SaaS 独有 305 张 | sys_role/sys_menu/sys_permission、knowledge、competition_*、follow_*、graduation_*、defense_* 等 SaaS 功能表 | **以 gjjddb2026 为准**，整体迁入（结构+数据） |
| PHP 独有 217 张 | sys_qlgt、cms_* 大部分、match_*、wenda_* 等 | **以 gjjddb 为准**，整体迁入 |
| A 类共有 ~95 张 | 见上表 | **以 gjjddb 为准**（全量覆盖，SaaS 侧无可丢数据） |
| B 类共有 ~15 张 | train_people_list、train_sign_up_people_self、train_teacher_bank 等 | **并集**:PHP 数据 + SaaS 独有 ID 段数据 |
| C 类共有 ~8 张 | platform_user 等 | **PHP 为准 + 追加 SaaS 独有行**（需逐表确认清单） |
| D 类系统表 | sys_user、sys_tenant、sys_dict、sys_index_swiper | **以 gjjddb2026 为准**,PHP 侧数据导出归档（不迁入） |
| gxwebsoft_core 151 张 | 整库 | **推荐：整库原样搬到 43 实例（不改名不合并）**,PHP 配置仅改 host；与 gjjddbpro 的 sys_* 无冲突（不同库名隔离）。备选：并入 gjjddbpro，需改名 ~10 张冲突表并改 PHP 代码，不推荐 |

共有表的结构取舍：以数据归属方的表结构为准建表；归属 SaaS 的表保持 SaaS 结构（含 BaseEntity 列），归属 PHP 的表保持 PHP 结构。**例外**:B/C 类表中若 SaaS 侧有该表活跃代码依赖的新列（如 train_people_list 的 SaaS 新增列），采用并集结构（PHP 结构 + SaaS 新增列，新增列对 PHP 数据置默认值）——逐表核对清单见附录生成物。

## 4. 实施步骤

### 阶段 0：准备（不停机，约 1-2 天）

1. 在 43 实例创建空库 `gjjddbpro`(utf8mb4 / utf8mb4_general_ci)
2. **先灌 `flyway_schema_history`**：从 gjjddb2026 导出 417 条记录导入 gjjddbpro，防止 SaaS 启动重跑迁移
3. 编写分类迁移脚本（按第 3 节分类生成），核心工具选择：
   - 数据量大表（exam_exam_detail 37 万、exam_result 26 万、task_exam_audit_records 8.7 万）用 mysqldump 单表导出
   - 小表用 `INSERT ... SELECT` 或 mysqldump
   - B/C 类用显式 ID 段/SaaS 独有 ID 清单做增量插入
4. 在 43 实例建预演库 `gjjddbpro_rehearsal`，完整跑一遍迁移脚本并产出校验报告（见阶段 2 校验项）
5. SaaS 侧代码准备（提前上线，开关控制）:
   - sync 通道增加总开关（现有 `platform.sync.enabled` 类配置）确认可关
   - `user-provisioning.php-write-enabled` 改为 false 后验证开户流程
   - `PHP_MYSQL_URL` 指向切换目标（合并后 PHP 库与 SaaS 库同库，该配置改指 gjjddbpro 或直接退役）
6. PHP 侧：确认 `config.php`/`configTask.php` 的 host/dbname 可配置，准备新配置
7. 通知窗口：公告停机维护（预计 1-2 小时，见阶段 1 实测后修正）

### 阶段 1：正式迁移（停机窗口）

1. 停 PHP 应用写流量（维护页/停 PHP-FPM 写入口）、停 platform-api、停 CLI 任务进程、停 sync
2. 冻结确认：`SHOW PROCESSLIST` 两库无写入
3. 执行迁移脚本（先灌 flyway_schema_history → SaaS 独有表 → PHP 独有表 → A 类 → B/C 类 → D 类 → gxwebsoft_core 整库）
4. MyISAM 7 张表转换 InnoDB 迁入（如需保持引擎则原样迁）
5. 70 张 utf8 表**保持原字符集迁入**（不在本次做字符集升级，降风险；升级列为后续专项）

### 阶段 2：校验（窗口内）

1. 行数校验：每张表 `COUNT(*)` 与预期清单比对（脚本化，预期值在预演时固化）
2. 关键表抽验：platform_user/exam_exam_detail 等按 ID+内容 hash 抽 1000 行
3. SaaS 启动自检：Flyway 校验通过（无新迁移执行）、登录、权限、题库、考试主链路冒烟
4. PHP 冒烟：App 登录（platform_user_token)、后台登录（sys_qlgt)、考试报名、导出异步任务
5. **auto_increment 修正**:B/C 类并入高 ID 数据的表，`ALTER TABLE ... AUTO_INCREMENT` 确认大于当前 MAX(id)(PHP 侧沿用自增主键，SaaS 高段数据并入后必须抬高水位，否则主键冲突）

### 阶段 3：切换与回滚

1. PHP 改配置指向 43:3308/gjjddbpro(gxwebsoft_core 同实例）→ 起服 → 冒烟
2. SaaS 改 `MYSQL_URL=jdbc:mysql://43.138.163.7:3308/gjjddbpro` → 重启 → 冒烟
3. 观察期（建议 1 周）：旧库 gjjddb / gjjddb2026 **只读保留**，不回写
4. 回滚：窗口期内任何问题 → 配置切回原库即可（原库全程未动）；观察期内回滚需补增量（尽量在窗口内把问题清零）
5. 观察期满：旧库备份归档后下线；SaaS 的 sync 代码与 PHP 数据源配置正式移除（独立变更）

## 5. 风险与对策

| 风险 | 等级 | 对策 |
|---|---|---|
| 共有表结构差异导致 SaaS 代码读写异常（如 BaseEntity 列缺失、tinyint vs int) | **高** | 预演库全量跑 SaaS 单测/冒烟；逐表核对 schema_diff_report;SaaS 活跃表结构以 SaaS 为准 |
| 停机窗口超预期 | 中 | 预演计时；大表用 mysqldump 并行（或 mydumper)；增量数据量一周漂移小，窗口内只做全量 |
| gxwebsoft_core 遗漏 | 中 | 已在方案内（阶段 1 第 3 步） |
| sync 通道未关导致同库自同步 | 中 | 切换前配置关闭；观察期确认无 sync 日志 |
| SaaS sql_mode 无 STRICT_TRANS_TABLES 与 PHP 端不一致 | 低 | 合并后统一在实例/会话级明确，避免 PHP 宽松写入产生脏数据差异 |
| 字符集混用（utf8/utf8mb4/utf8mb3_bin 并存） | 低 | 本次不统一，列后续专项；注意 utf8mb3 在 8.4 已 deprecated 但可用 |
| PHP 密码含 `#`、SaaS 密码特殊字符在配置/YAML 中被截断 | 低 | YAML 引号包裹（已有踩坑记录） |

## 6. 需要你决策的问题

1. **gxwebsoft_core 的处理**：推荐整库搬到 43 同实例（PHP 只改 host)，是否同意？（并入 gjjddbpro 需改 PHP 代码，不推荐）
2. **D 类系统表**:gjjddb 的 sys_user(2533 行）/sys_tenant/sys_dict 实测 PHP 代码无引用，计划以 SaaS 为准、PHP 侧数据仅归档——是否同意？（若 PHP 后台历史上确实用 sys_user 登录过，需确认现在确实已切换到 sys_qlgt)
3. **C 类表逐表归属**:platform_user 等 ~8 张"PHP 为准 + 追加 SaaS 独有行"是否符合预期？其中 train_info_class/train_info_list/train_solution 等需你确认业务归属（清单在 id_overlap_report.csv)
4. **停机窗口**：可接受的维护时段与时长？（预演后给出实测时长，预估 1-2 小时）
5. **旧库保留期**：建议只读保留 1 周后归档下线，是否同意？
6. **合并后 SaaS 的 sync 镜像通道（platform_user_token 镜像等）整体下线**，PHP 写入的 platform_user 数据 SaaS 直接同库读——是否同意此方向？（这意味着合并完成后 PHP 侧不能再有"独立回到旧库"的回滚，除非补同步）

## 7. 分析产物清单（outputs/dbmerge-analysis/，均已落盘）

- `php_gjjddb_tables.tsv` / `saas_gjjddb2026_tables.tsv` / `php_gxwebsoft_core_tables.tsv` — 三库表清单+近似行数
- `common_tables.txt` / `php_only.txt` / `saas_only.txt` — 121 张同名表及各自独有表
- `schema_diff_report.txt` — 121 张同名表逐列结构差异
- `id_overlap_report.csv` — 115 张 id 主键表的 ID 重叠全量比对（both/only_php/only_saas)
- `php_columns.tsv` / `saas_columns.tsv` / `gx_columns.tsv` — 全量列定义快照
