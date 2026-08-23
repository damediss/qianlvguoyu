# 数据库合并方案：gjjddb + gjjddb2026 → gjjddbpro（决策已确认·实施细化版）

> 日期：2026-08-20（2026-08-21 细化分工；2026-08-24 你确认提供两台服务器 SSH 并授权我代执行服务器侧操作后，分工再调整）
> 状态：**§6 六项决策已由你确认（2026-08-21）；实施步骤已按"AI 能做 / 必须你做"细化。未经你最终批准，未执行任何变更**
> 分析数据来源：对两套生产库的实时只读盘点（information_schema + ID 重叠分析），原始数据在 `outputs/dbmerge-analysis/`

---

## 1. 目标与原则

- 在 43.138.163.7:3308 新建库 `gjjddbpro`，将 PHP 生产库 `gjjddb`(114.132.67.185:3308）与 SaaS 库 `gjjddb2026`(43.138.163.7:3308）合并为一个生产库
- 合并后 PHP 应用与 SaaS 应用（platform-api）同时使用 `gjjddbpro`
- 数据优先级：同名表默认以 `gjjddb`（现生产）为准；**承载 SaaS 核心功能的表以 `gjjddb2026` 为准**
- 原则：稳妥优先、停机窗口可控、可回滚；**PHP 正常运行是第一优先级（决策 3 的前提）**

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

`platform-api` 的 `integration/sync`(PlatformSyncService/Scheduler）每周期从 PHP 库同步 platform_user、platform_user_token、exam_exam_detail、exam_knowledge_structure、train_exam_list、train_exam_people_list、exam_course 等到 SaaS 库——这就是 A 类表"快照子集"的成因。**合并后该通道变成同库自同步，必须下线**（决策 6 已同意），另有学员开户双写（`user-provisioning.php-write-enabled`）需改为单写。

## 3. 合并策略（按表分类处置）

| 类别 | 表 | 处置 |
|---|---|---|
| SaaS 独有 305 张 | sys_role/sys_menu/sys_permission、knowledge、competition_*、follow_*、graduation_*、defense_* 等 SaaS 功能表 | **以 gjjddb2026 为准**，整体迁入（结构+数据） |
| PHP 独有 217 张 | sys_qlgt、cms_* 大部分、match_*、wenda_* 等 | **以 gjjddb 为准**，整体迁入 |
| A 类共有 ~95 张 | 见上表 | **以 gjjddb 为准**（全量覆盖，SaaS 侧无可丢数据） |
| B 类共有 ~15 张 | train_people_list、train_sign_up_people_self、train_teacher_bank 等 | **并集**:PHP 数据 + SaaS 独有 ID 段数据 |
| C 类共有 ~8 张 | platform_user 等 | **PHP 为准 + 追加 SaaS 独有行**（逐表核对，受 §4 保障专项约束） |
| D 类系统表 | sys_user、sys_tenant、sys_dict、sys_index_swiper | **以 gjjddb2026 为准**,PHP 侧数据导出归档（不迁入） |
| gxwebsoft_core 151 张 | 整库 | **整库原样搬到 43 实例（不改名不合并）**,PHP 配置仅改 host（决策 1 已同意） |

共有表的结构取舍：以数据归属方的表结构为准建表；归属 SaaS 的表保持 SaaS 结构（含 BaseEntity 列），归属 PHP 的表保持 PHP 结构。**例外**:B/C 类表中若 SaaS 侧有该表活跃代码依赖的新列（如 train_people_list 的 SaaS 新增列），采用并集结构（PHP 结构 + SaaS 新增列，新增列可空带默认值，对 PHP 代码无感知）——逐表核对清单见预演产物。

## 4. 实施步骤（细化分工版）

**分工标记**:
- **【AI】** 我（Agent）可直接完成：编写全部脚本、用 mysql/mysqldump 客户端直连两套库执行、改 SaaS 代码、出校验报告；**2026-08-24 起你确认提供两台服务器 SSH（root 直登）并授权我代执行服务器侧操作**，范围：MySQL root 建库授权、PHP 停写/改配置/重启、platform-api 停启/改 .env、服务器间数据传输
- **【你】** 必须你本人完成：云平台控制台（安全组/VPC）、停机公告与时段、真机/App 冒烟、Go/No-Go 决策、旧库 DROP、归档介质保管、窗口期全程在场
- **【协作】** 我准备材料（SQL/命令/配置 diff），你确认后我执行或你执行

**凭据安全约定（2026-08-24）**：SSH / MySQL root 密码由你私下提供，仅用于本机会话与运行期环境变量/交互输入，**不写入仓库、文档、脚本文件**；建议窗口期使用临时密码，合并完成后轮换；若后续确认可换密钥认证，切换后密码作废。

**前置实测事实（2026-08-21 实测，决定分工边界）**:
1. 本机可直连两套库（分析阶段已验证连通；mysql 客户端 `D:\mysql-8.4.9-winx64\bin\mysql.exe`)
2. **43 实例的 `gjjd` 账号无权创建 gjjddbpro**——实测 `SHOW GRANTS` 仅有 `gjjddb2026.*` 与 `qualification_mvp.*` 的权限，无全局 CREATE。建库、授权必须用 root（已授权我代执行，前置：你提供 MySQL root 密码）
3. ~~我没有两台服务器的 SSH~~ **已解决（2026-08-24)：你确认提供两台服务器 SSH（root 直登，密码认证；是否可换密钥待你确认）**，platform-api 的 systemctl / .env、PHP 应用配置切换均已授权我代执行
4. 本会话我没有 PHP 库（114.132.67.185:3308）的凭据（分析阶段曾使用）——正式执行前需你确认凭据可用或重新提供一次；43 实例 MySQL root 密码你说晚点提供（阶段 -1.1 的前置）
5. gjjddb2026 与目标库 gjjddbpro **同实例**,SaaS 侧数据可用 `INSERT ... SELECT` 同实例直迁（不出网，分钟级）；只有 PHP 侧两库（~555MB）需要跨公网传输，这是窗口耗时的主体

### 阶段 -1：权限与凭据准备（不停机，约 0.5 天，一切动作的前置）

| # | 步骤 | 责任 |
|---|---|---|
| -1.0 | SSH 凭据交付与连通验证：你提供两台服务器 root 密码（私下渠道）；我从本机 ssh 验证两台连通、`df -h` 查磁盘（对应 -1.4）、43 上 `mysql -uroot` 验证 root 可用、两台互 `telnet` 探内网（对应 -1.3） | 【协作】你提供，【AI】验证 |
| -1.1 | 在 43 实例用 root 执行建库+授权（SQL 由我生成、执行前贴你确认）：建 `gjjddbpro`、`gjjddbpro_rehearsal`、`gxwebsoft_core` 三个空库（utf8mb4 / utf8mb4_general_ci)；授权 `gjjd`@`%` 对三库 ALL；为 PHP 切换准备账号——建议新建 `php_gjjd`@`114.132.67.185`（随机密码我生成），授权 `gjjddbpro.*` + `gxwebsoft_core.*` ALL（备选：直接复用 gjjd@%，省一步但权限面大） | 【AI】代执行（前置：MySQL root 密码） |
| -1.2 | 腾讯云控制台确认 43 安全组放行 114.132.67.185 → 3308、放行我本机出口 IP → 两台 22；从 PHP 服务器 `telnet 43.138.163.7 3308` 验证连通（我代做） | 【你】控制台，【AI】验证 |
| -1.3 | **确认两台 CVM 是否同地域/同 VPC：若是，PHP 侧配置用 43 的内网 IP（延迟从公网几十 ms 降到 <1ms)；若跨地域，记录下来，阶段 0 必须做 PHP 页面实测（见风险表"跨网延迟"条）** | 【AI】SSH 自查（内网 IP 互探），【你】控制台确认 |
| -1.4 | 确认 43 服务器磁盘余量（新增约 1.1GB 数据 + 索引，建议余量 ≥10GB) | 【AI】 |
| -1.5 | 提供/确认 PHP 库只读导出凭据（需 SELECT / LOCK TABLES / SHOW VIEW 权限） | 【你】提供，【AI】验证连通 |
| -1.6 | 用新授权验证：能连 gjjddbpro / gjjddbpro_rehearsal 并建删测试表 | 【AI】 |

### 阶段 0：脚本编写与预演（不停机，1-2 天）

**【AI】独立完成 0.1~0.4**:

- 0.1 生成全套脚本到 `scripts/dbmerge/`:
  - `01_precheck.sql` — B/C 类唯一索引冲突预检（SaaS 独有行 vs PHP 数据，覆盖每张表全部 UNIQUE KEY);C 类 SaaS 独有行与 PHP 结构的列兼容检查；5.7 零日期（`'0000-00-00'`）扫描
  - `02_ddl.sql` — 分类建表 DDL（结构来源规则见 §3:B/C 类并集结构逐表生成）
  - `03_migrate.sh` — 数据迁移总控（SaaS 侧同实例 `INSERT ... SELECT`;PHP 侧 mysqldump；大表单表导出；B/C 类按 ID 清单增量；gxwebsoft_core 整库）
  - `04_validate.sql` — 行数比对、关键表 ID+内容 hash 抽验、auto_increment 水位检查
  - `05_cutover_checklist.md` — 窗口内操作卡（逐条勾选，含每步预期耗时）
- 0.2 在 `gjjddbpro_rehearsal` 完整预演一遍，输出报告到 `outputs/dbmerge-rehearsal/`：行数/校验和差异、冲突清单、**各步骤实测耗时（据此修正窗口时长）**，并固化预期行数清单 `expected_counts.tsv`
- 0.3 执行"C 类表 PHP 正常运行保障专项"（见下方专项小节），结论并入预演报告
- 0.4 SaaS 代码复核（只读检查，必要时才改代码）:`PLATFORM_SYNC_ENABLED` 开关路径、`user-provisioning.php-write-enabled` 开关路径确认可用；若需补开关则提交代码分支（不部署）

**【你】完成 0.5~0.8**:

- 0.5 审预演报告 + PHP 保障专项结论，**签字 Go**（任何一项不通过则不进入窗口，回到方案修订）
- 0.6 若 0.4 产生了代码改动：部署到生产（开关默认关闭，行为不变）
- 0.7 PHP 侧【AI】：我在 114 上确认 `config.php` / `configTask.php` 的 host/dbname/账号位置，直接备好待切换的新配置文件（先不启用，旧文件留 `.bak`）；**用测试入口/备用站点连接 gjjddbpro_rehearsal 实测关键页面响应（验证跨网延迟，对应决策 3），页面观感与后台抽查由你做**
- 0.8 发停机公告：时段由你定（建议业务低谷），时长以预演实测为准（预估 1-2 小时，决策 4 已接受该量级）

### C 类表 PHP 正常运行保障专项（决策 3 的前置条件，随阶段 0 预演完成）

你对决策 3 的回答是"必须优先保证 PHP 的正常运行，如果可以保证则符合预期"。以下 6 条是保证手段，全部在预演阶段完成并出报告，**任何一条不通过则 C 类处置退化（见第 6 条），绝不带病合并**:

1. **结构兼容**:C 类表以 PHP 结构建表；逐表核对 SaaS 独有行是否依赖 PHP 结构没有的列（对照 `schema_diff_report.txt`)——若依赖，采用并集结构（新列可空+默认值，PHP 代码无感知），或丢弃该列值并出丢弃清单给你确认
2. **唯一索引冲突预检**：对每张 C 类表的全部 UNIQUE KEY，检测 SaaS 独有行与 PHP 数据是否冲突（如 platform_user 的 username/phone 类索引）；冲突行出清单，逐行由你裁决（改/弃）
3. **PHP 查询可见性**：核对 PHP 代码对 C 类（及 B 类）表的列表查询是否带站点/来源过滤；统计"合并后 PHP 后台将新出现的数据行数"（例如 train_people_list 并入的 SaaS 高 ID 段学员会出现在 PHP 后台名单）——不报错但可能困惑运营，需你确认可接受
4. **自增水位**：窗口内对每张 B/C 类表 `ALTER TABLE ... AUTO_INCREMENT` 抬到 max(id)+1 以上（脚本自动计算，04_validate 复核），保证 PHP 后续自增写入不撞主键
5. **PHP 真实查询回归**：在预演库上抽取 PHP 典型 SQL(App token 校验、报名写入、名单分页、后台登录）跑通；5.7→8.4 兼容性（零日期、sql_mode、保留字）在预演中暴露并逐一消解
6. **兜底退路**：若 1~5 任一项无法通过，该表退化为"PHP 数据原样迁入，SaaS 独有行暂存旁表 `<表名>_saas_pending`",PHP 零影响，SaaS 侧数据后续人工补登

### 阶段 1：停机迁移（窗口内，顺序执行）

| # | 步骤 | 责任 |
|---|---|---|
| 1.1 | 停 PHP 写流量（维护页/停写入口，具体形式先与你确认）、`systemctl stop platform-api`、停 CLI 任务进程；确认 sync 开关已处关闭态 | 【AI】 |
| 1.2 | 冻结确认：两库 `SHOW PROCESSLIST` 无写入，截图留档；对 114 实例做最终增量快照比对（确认停机后无新写入） | 【AI】 |
| 1.3 | 窗口 Go 决策（此刻原库仍未动一个字节，可随时中止） | 【你】 |
| 1.4 | 执行 `03_migrate.sh`，顺序：① 灌 flyway_schema_history（同实例）→ ② SaaS 独有表（同实例 INSERT SELECT) → ③ PHP 独有表（mysqldump 跨网） → ④ A 类（PHP 全量覆盖） → ⑤ B/C 类（并集/追加，按预检清单） → ⑥ D 类（SaaS 为准；PHP 侧 sys_user 等先导出归档文件到 `outputs/dbmerge-archive/`) → ⑦ gxwebsoft_core 整库 → ⑧ MyISAM 7 张转 InnoDB → ⑨ B/C 类 auto_increment 抬升。每步日志落盘 | 【AI】（跨网传输方式见下注） |
| 1.5 | 执行 `04_validate.sql`，出正式校验报告，与你现场逐条核对 | 【AI】出报告，【你】核对 |

> **跨网传输注（2026-08-24 更新：已有 SSH，首选走法全链由我执行）**:PHP 侧 ~555MB 首选我在 114 服务器本机 mysqldump + scp 直传 43 再导入（不经本机中转，最快）；备选我本机 relay（mysqldump 远程导出到本机再远程导入，受本机上行带宽限制）保留为降级路径。预演会计时验证备选方案是否满足窗口。导入会话统一 `SET SESSION sql_mode` 去掉 `NO_ZERO_DATE` 以兼容 5.7 零日期。7 张 MyISAM 表导出时需锁表，但窗口内 PHP 已停写，无影响。

### 阶段 2：切换与冒烟（窗口内）

| # | 步骤 | 责任 |
|---|---|---|
| 2.1 | PHP 改配置指向 43(gjjddbpro + gxwebsoft_core 同实例；优先内网 IP，见 -1.3)→ 重启（以上我执行）→ 冒烟：App 登录（platform_user_token)、后台登录（sys_qlgt，同时作为决策 2 的最终确认）、考试报名、导出异步任务、后台名单页（核对保障专项第 3 条）（真机/App/后台页面只能你侧做） | 【AI】改配置重启，【你】冒烟 |
| 2.2 | SaaS 改 `.env`:`MYSQL_URL=.../gjjddbpro`、`PLATFORM_SYNC_ENABLED=false`、`user-provisioning.php-write-enabled=false` → `systemctl restart platform-api`（以上我执行）→ 我跑 HTTP 冒烟脚本（登录/权限/题库/考试主链路），并确认 Flyway 校验通过、无新迁移执行 | 【AI】 |
| 2.3 | **Go / No-Go 决策**:No-Go → 双方配置切回原库（原库全程未动，秒级回滚，窗口结束） | 【你】 |

### 阶段 3：观察期（1 周，决策 5 已同意）

- 旧库 gjjddb / gjjddb2026 / 旧 gxwebsoft_core **只读保留，不回写**【你】（如需主动设 read_only，我可在两台实例上代执行）
- 【AI】每日一次只读对账（新库关键表行数增长、SaaS 日志关键词、sync 确认无运行记录），出简报给你
- 【AI】观察期内 SaaS 若有新 Flyway 迁移，DDL 必须显式声明 `utf8mb4_unicode_ci`（与 SaaS 存量主体一致），避免继承库默认 `general_ci` 造成新表与存量表 collation 不一致
- 【你】观察业务反馈，重点盯 PHP 端（决策 3 的保障对象）
- 观察期内回滚 = 配置切回 + 窗口后新库增量补回旧库（补回脚本【AI】写，配置切回【AI】执行，你现场确认）

### 阶段 4：归档与清理（观察期满后）

| # | 步骤 | 责任 |
|---|---|---|
| 4.1 | 旧三库 mysqldump 全量归档 | 【AI】可代跑，【你】确认归档完整并存入 COS/离线介质 |
| 4.2 | **旧库 DROP 下线**——破坏性动作，必须你亲自执行，我不执行删库 | 【你】 |
| 4.3 | SaaS 正式移除 sync 通道代码、PHP_MYSQL_* 配置、user-provisioning 双写残留（独立变更，走正常发版） | 【AI】写代码，【你】部署 |
| 4.4 | 更新 AGENTS.md / README / deploy 文档中的库名与配置说明 | 【AI】 |

### 分工速查表

| 我（AI）独立完成 | 必须你完成 |
|---|---|
| 全部迁移/预检/校验脚本编写（`scripts/dbmerge/`) | 提供 SSH / MySQL root 凭据（私下渠道，事后轮换） |
| 预演执行 + 预演报告（`outputs/dbmerge-rehearsal/`) | 腾讯云安全组放行、VPC 控制台确认（-1.2、-1.3 控制台部分） |
| root 建库授权（-1.1)、SSH 连通/磁盘/内网自查（-1.0/-1.3/-1.4) | PHP 侧真机/App 冒烟、后台页面抽查 |
| PHP 停写/改配置/重启（1.1、2.1 前半）、platform-api 停启/.env（1.1、2.2) | 停机公告、停机时段选择、Go/No-Go 决策 |
| 服务器间数据传输全链（114 mysqldump → scp 直传 → 43 导入） | 旧库 DROP、归档介质保管、窗口期全程在场 |
| 唯一索引冲突预检、PHP 保障专项验证、行数/hash 校验报告 | |
| SaaS 侧开关/退役 sync 的代码改动与文档更新、观察期对账简报、旧库归档 dump | |

## 5. 风险与对策

| 风险 | 等级 | 对策 |
|---|---|---|
| 共有表结构差异导致 SaaS 代码读写异常（如 BaseEntity 列缺失、tinyint vs int) | **高** | 预演库全量跑 SaaS 单测/冒烟；逐表核对 schema_diff_report;SaaS 活跃表结构以 SaaS 为准 |
| **PHP 跨公网访问 43 实例的延迟**（现状 PHP 与本机 MySQL 同机，合并后每查询跨网；页面多查询场景可能明显变慢） | **高** | -1.3 确认同地域走内网 IP；阶段 0.7 用预演库实测 PHP 关键页面响应；实测不可接受则方案需重审（ gxwebsoft_core 与 gjjddbpro 必须同实例是硬约束，不能分开放） |
| gjjd 账号无权建新库（2026-08-21 实测 SHOW GRANTS 确认） | 中 | 阶段 -1.1 由你用 root 建库授权，是全部动作的前置 |
| 5.7 → 8.4 兼容：零日期、sql_mode、保留字导致导入或 PHP 运行异常 | 中 | 预演全量暴露；导入会话级放宽 sql_mode;PHP 典型 SQL 预演回归（保障专项第 5 条） |
| 停机窗口超预期 | 中 | 预演计时；SaaS 侧同实例直迁不出网；PHP 侧首选服务器间直传；窗口内只做全量 |
| gxwebsoft_core 遗漏 | 中 | 已在方案内（阶段 1 第 ⑦ 步） |
| sync 通道未关导致同库自同步 | 中 | 2.2 重启时关开关；观察期确认无 sync 日志 |
| B/C 类并入数据在 PHP 后台列表"新出现"（如 SaaS 学员出现在 PHP 名单） | 中 | 保障专项第 3 条预演出报告，你确认可接受后才进窗口 |
| SaaS sql_mode 无 STRICT_TRANS_TABLES 与 PHP 端不一致 | 低 | 合并后统一在实例/会话级明确，避免 PHP 宽松写入产生脏数据差异 |
| 字符集混用（utf8/utf8mb4/utf8mb3_bin 并存） | 低 | 本次不统一，列后续专项；注意 utf8mb3 在 8.4 已 deprecated 但可用 |
| PHP 密码含 `#`、SaaS 密码特殊字符在配置/YAML 中被截断 | 低 | YAML 引号包裹（已有踩坑记录） |

## 6. 决策记录（2026-08-21 你已全部确认）

| # | 问题 | 你的决策 | 方案落定点 |
|---|---|---|---|
| 1 | gxwebsoft_core 整库搬到 43 同实例（PHP 只改 host) | **同意** | 阶段 -1.1 建库授权；阶段 1 第 ⑦ 步整库搬运；阶段 2.1 PHP 仅改 host |
| 2 | D 类系统表以 SaaS 为准，PHP 侧数据仅归档 | **同意** | 阶段 1 第 ⑥ 步：gjjddb 的 sys_user(2533 行）/sys_tenant/sys_dict 导出到 `outputs/dbmerge-archive/` 后不入新库；阶段 2.1 的 sys_qlgt 后台登录冒烟作为最终确认 |
| 3 | C 类"PHP 为准 + 追加 SaaS 独有行" | **有条件同意：必须优先保证 PHP 的正常运行，可以保证则符合预期** | 新增"C 类表 PHP 正常运行保障专项"6 条（见 §4)；预演报告必须含保障验证结论，你签字 Go 才进窗口；任何一条不通过则该表退化处置（专项第 6 条） |
| 4 | 停机窗口 | **可接受（1-2 小时量级）** | 具体停机日期/时段由你另定（建议业务低谷）；时长以阶段 0.2 预演实测为准修正 |
| 5 | 旧库只读保留 1 周后归档下线 | **同意** | 阶段 3 / 阶段 4 |
| 6 | 合并后 sync 镜像通道整体下线，SaaS 直接同库读 | **同意** | 两步走：2.2 切换时关开关；阶段 4.3 观察期满后移除代码。注意：此后 PHP 若回滚旧库需补增量同步（阶段 3 预案） |

## 7. 分析产物清单（outputs/dbmerge-analysis/，均已落盘）

- `php_gjjddb_tables.tsv` / `saas_gjjddb2026_tables.tsv` / `php_gxwebsoft_core_tables.tsv` — 三库表清单+近似行数
- `common_tables.txt` / `php_only.txt` / `saas_only.txt` — 121 张同名表及各自独有表
- `schema_diff_report.txt` — 121 张同名表逐列结构差异
- `id_overlap_report.csv` — 115 张 id 主键表的 ID 重叠全量比对（both/only_php/only_saas)
- `php_columns.tsv` / `saas_columns.tsv` / `gx_columns.tsv` — 全量列定义快照

## 8. 待产生物（方案执行时创建）

- `scripts/dbmerge/` — 01_precheck.sql / 02_ddl.sql / 03_migrate.sh / 04_validate.sql / 05_cutover_checklist.md
- `outputs/dbmerge-rehearsal/` — 预演报告、expected_counts.tsv、耗时记录
- `outputs/dbmerge-archive/` — D 类 PHP 侧归档数据、旧库归档 dump（阶段 4)
