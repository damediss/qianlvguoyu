# AI 同步学功能单库化改造方案（gjjddbpro）

> 版本：v1.2　日期：2026-08-27
> 前置事实：SaaS 库 `gjjddb2026` 与 PHP 库 `gjjddb` 已于 2026-08-25 合并为 `gjjddbpro`（单实例 43.138.163.7:3308，双端已切换运行，租户 id 统一为 **10159**，PHP 第二数据源当前为过渡指向 gjjddbpro，sync 通道已关）。
> 目标：将《AI同步学（学员复习）实现方案》v1.5 与《AI同步学统计功能设计方案》v1.2 中的**双库互动功能全部改造为单库使用**（主数据源 gjjddbpro）。**v1.2：代码实施完成（后端 + 管理端前端 + 手机端 gjjd_appnext），本地编译/测试/冒烟全绿，待部署窗口执行 §9.1 步骤 3-6。**
> 需求方确认（2026-08-25 第一轮）：Q1 全拆第二数据源与同步通道；Q2 移除 train_exam_list 只读约束/导入机制；Q3 取 (a) exam_result 事务内同步直写；Q4 删除 App token PHP 回退；Q5 观察期压缩（半天后实施，改造后另测观察）；Q6 同意 tenant 行为逐表核验。
> 需求方确认（2026-08-25 第二轮，代码走查 + 合并库实测后的缺陷修订）：Q7 App 租户切换取**双轨**（服务端对 /appreviewsync 强制覆盖 10159 + App 发版同步改默认值）；Q8 遗留 tenant_id=1 数据 **UPDATE 归一 10159 并补写 exam_result**；Q9 exam_result 直写机制取**主库 JdbcTemplate 复用原 SQL**（10159 硬编码）；Q10 用户侧 10 亿区间隔离（LOCAL_ID_BOUNDARY / requireSaaSLocalUser）**本期一并移除**。
>
> 修订记录：
> - v1.1（2026-08-25）：按代码走查与合并库实测结果修订 12 处缺陷——① 新增 §4-G「App 租户切换双轨」（原方案误将 App header=10159 当既成事实，实测 App 仍发 1 且 08-25 当日仍有 tenant_id=1 新数据产生）；② 新增 §4-H「数据修复」（tenant_id=1 遗留行归一 + 58 行 php_synced=0 补写 exam_result + knowledge_structure 13910 行修复；uk 唯一键不含 tenant_id，不能靠重新生成）；③ 更正 §2 AUTO_INCREMENT 事实（实测 466，非 100000）；④ §4-B 新增「新 Mapper 实体不得继承 BaseEntity」硬约束（合并表只有 create_time/update_time，无 created_at/updated_at/created_by/updated_by，id 为 int AUTO）；⑤ §4-C 明确 exam_result 直写机制为 JdbcTemplate 复用原 SQL；⑥ 回滚预案补 php_synced=1 防重写步骤；⑦ 拆除面补全（UserProvisioningProperties.LOCAL_ID_BOUNDARY 引用点、ClassPrepRosterImportService/TrainSignUpPeopleImportService 警告分支、前端 php-write 文案、application-prod.yml、AsyncConfig reviewSyncExecutor、.run xml、auth-token-sync-ops.sql、06_daily_check.py 口径、测试类）；⑧ §4-E 明确开户单写保留路径与 dedup 语义；⑨ 明示同步直写失败使作答事务回滚的行为变化；⑩ §6 核验清单补充遗留数据与唯一键事实；⑪ 测试/验收补充 header=1 场景、数据修复验证、回滚演练；⑫ 主 URL tinyInt1isBit 风险核实结论（合并表相关列为普通 tinyint，无需改主 URL，保留 asInt/asLong Boolean 防御）。
> - v1.2（2026-08-27）：**实施完成**。按 v1.1 全量落地（拆除层/查询层迁移/同步直写/统计折叠/开户单写/区间隔离移除/租户双轨第一轨/配置清理/手机端），实施中修正方案 5 处偏差（详见 §12 实施记录）：① `reviewSyncExecutor` 仍有消费方 `ReviewLlmAssistService`（v1.1 误称可删）→ Bean 改名 `reviewLlmAssistExecutor` 保留；② 作答 INSERT 成功即置 `php_synced=1`（v1.1 只在回滚预案提及，未明确直写路径的置位点，缺了会在回滚时被补偿 job 重写）；③ `train_exam_work_type_list.level` 实体 String/库 tinyint → `parseLevel` 容错解析；④ 权重多条告警改回 `selectCount` 判重（v1.1 写法在 LIMIT 1 后 size 恒 ≤1，是死代码）；⑤ 拆除面补 `src/test/resources/application.yml` sync 段、`SyncStats.phpWritten` 字段、`.run/PlatformApplication.run.xml` env 行。构建与测试结论见 §12。

---

## 1. 背景与目标

### 1.1 为什么现在改造

合并前，AI 同步学（学员复习）与 AI 同步学统计依赖"双库互动"架构：

- **只读跨库**：SaaS 经第二数据源（`integration/php`，`PhpDataSourceHolder`）实时读 PHP 库 `gjjddb` 的题库、知识树、考试计划、学员关联；
- **异步回写**：作答经 `PhpResultWritebackService` 异步回写 PHP 库 `exam_result`（`exam_type=5`），含失败补偿状态机；
- **镜像同步**：鉴权 3 表（platform_user / platform_user_token / platform_user_detail）由 `PlatformSyncService` 轮询 PHP→SaaS 镜像；
- **一次性导入**：`train_exam_list` 由 PHP 一次性导入 SaaS，并靠"id<100000 即 PHP 来源行"做只读隔离。

合并后两张表/两个库变成**同一张表/同一个库**：以上全部机制失去存在意义，且保留会带来三害：① 第二数据源指向旧库地址则读到 stale 数据；② 指向新库则与主数据源重复读同一库（空转连接池、双份缓存、双份判分/回写路径易分叉）；③ 回写补偿/镜像同步/只读隔离等复杂状态机在单库下纯属负担。

### 1.2 目标

1. **拆除** `integration/php` 第二数据源整层与 `integration/sync` 镜像同步通道（与数据库合并方案决策 6 / 阶段 4.3 一致，一次到位）；
2. 学员复习、统计、App 鉴权、开户全部读写**只走主数据源 `gjjddbpro`**；
3. `exam_result` 回写改**事务内同步直写**（Q3a/Q9）；
4. 移除 `train_exam_list` 一次性导入/只读约束（Q2）与用户侧 10 亿区间隔离（Q10）；
5. App 请求租户**双轨切换到 10159**（服务端强制覆盖 + App 发版，Q7），并**一次性修复遗留 tenant_id=1 数据**（Q8）；
6. 改造后功能行为与合并前**等价或更优**，测试与验收清单覆盖全链路。

---

## 2. 合并后关键事实（改造依据，2026-08-25 实测复核）

| 项 | 事实 |
|---|---|
| 主数据源 | `localhost:3308/gjjddbpro`（43），MyBatis-Plus 多租户拦截器按 `TenantContextHolder` 自动拼 `tenant_id`；`DEFAULT_TENANT_ID=10159`（TenantContextHolder.java:11，决策 7 已落地） |
| App 端租户现状 | **App 仍发 `X-Tenant-Id: 1`**（`gjjd_appnext/utils/saasRequest.js:22` `DEFAULT_SAAS_TENANT_ID=1`；`getSaasTenantId()` 优先读 storage `saas_tenant_id`，老设备存 1；全仓仅企业调研页调用 `setSaasTenantId`）。`/api/appreviewsync/**` 不在 TenantContextFilter 白名单，**强制解析 header 作为租户**；`AppTokenAuthFilter` 从不调用 `TenantContextHolder.setTenantId`。**实测 08-25 当日仍有 tenant_id=1 新数据产生**（ai_review_plan 2 行 created_at=08-25 05:05:50） |
| 主数据源 JDBC URL | 不含 `tinyInt1isBit=false`。**实测合并表中 level/difficulty/type 均为普通 `tinyint`（非 tinyint(1)），不触发 Boolean 映射**；相关查询只用于 WHERE 过滤，主 URL 无需改动；新查询层保留 `asInt/asLong` 的 Boolean 防御即可 |
| 合并表审计列 | 这些 PHP 表**只有 `create_time/update_time`（或无 update_time），无 `created_at/updated_at/created_by/updated_by`；主键全部 `int AUTO_INCREMENT`**（含 exam_result、train_exam_list、platform_user）。**新 Mapper 实体一律不继承 BaseEntity** |
| PHP 第二数据源 | 已过渡指向 `localhost:3308/gjjddbpro`（`PHP_MYSQL_URL`），独立 SqlSessionFactory/JdbcTemplate **无租户拦截器** |
| sync 通道 | `PLATFORM_SYNC_ENABLED=false`、`PLATFORM_USER_PHP_WRITE_ENABLED`（默认 true，yml 未显式关）——通道已关，代码未拆 |
| 关键表在合并库 | `exam_exam_detail` 372870（全 tenant_id=10159）、`exam_knowledge_structure` 21936@10159 **+1 行 tenant_id=1**（id=13910，course 205「职业道德」L1 节点）、`train_exam_list` 462 行@10159（**AUTO_INCREMENT=466，非 100000**）、`exam_result` 262963@10159、`exam_knowledge_rate` type=3 有 82 行 + type=0 有 34 行（全 10159，国标权重已随合并保留）、`platform_user(_token/_detail)`（PHP 为准，id 为 int AUTO） |
| ai_review_* 遗留数据 | 主流数据全 10159（answer 2792、plan 25、wrong 1054、mock 6、gray 4）；**遗留 tenant_id=1**：plan 2、plan_day 2、answer 58（且 `php_synced=0` 从未回写）、wrong_item 26。`ai_review_plan` 唯一键 `uk_ai_review_plan_user_exam=(platform_user_id, exam_id)` **不含 tenant_id** → 遗留行不能靠"重新生成"消化，否则撞唯一键 |
| ignoreTable 白名单 | `platform_user`/`platform_user_token`/`platform_user_detail` 等 19 张无 `tenant_id` 列的表不走租户过滤（MybatisPlusConfig.java:40-61，已含鉴权三表，维持现状） |
| 用户侧区间隔离 | `platform_user.id` 为 `int AUTO`（PHP 口径自增续），SaaS 新开户将拿小 id，**10 亿区间隔离（LOCAL_ID_BOUNDARY）在单库下已失效**（Q10 移除） |
| sys_user 脏值 | 存在 1 行 `tenant_id=2062723418189152258`（雪花样脏值，归一化遗留），随改造窗口顺带确认处置 |

---

## 3. 双库互动点总表（处置映射）

| # | 位置（方案文档） | 现状机制 | 改造处置 |
|---|---|---|---|
| 1 | 复习方案 §3 数据访问设计 | 第二数据源 `PhpDataSourceConfig/Holder` + `phpJdbcTemplate` | **拆除**（Q1） |
| 2 | 复习方案 §3.3 查询口径 | `PhpExamQueryService`（题目全集/计划/工种课程/知识树/权重） | 改主数据源（新 Mapper 或主 JdbcTemplate，§4-B） |
| 3 | 复习方案 §3.4 题目详情缓存 | Caffeine miss 回源 PHP 库 | 回源主库（缓存保留） |
| 4 | 复习方案 §4.7/D4 exam_result 回写 | `PhpResultWritebackService` 异步 INSERT + `php_synced` 补偿 job | **同步直写主库**（Q3a/Q9），删除补偿状态机 |
| 5 | 复习方案 §3.1 租户错配说明 | "PHP 库 10159 vs App header 1" 需独立工厂绕拦截器 | 主数据源直用；App 请求租户经**服务端强制覆盖**统一为 10159（Q7） |
| 6 | 复习方案 §13.3 鉴权依赖 | 3 表同步任务（鉴权方案） | 拆除 `integration/sync`（Q1，决策 6） |
| 7 | 统计方案 §2.2 只读第二数据源 | 知识树/工种/题目详情回退/train_exam_list 读 PHP | 全部改主数据源 |
| 8 | 统计方案 §3 一次性导入+只读 | 导入脚本、id<100000 判定、AUTO_INCREMENT=100000、只读拦截 | **全部移除**（Q2） |
| 9 | 统计方案 §5.3 题目三重链 | SaaS 本地优先→回退 PHP→降级 | 折叠为单次主库读 |
| 10 | 复习方案 D1/鉴权 App 登录 | `AppTokenAuthFilter` PHP token 回退（`PhpTokenQueryService`） | **删除回退**，主库查不到即 401（Q4） |
| 11 | 复习方案 §3.2 saas_sync 账号 | PHP 库专用只读+INSERT 账号 | 观察期后回收授权 |
| 12 | 开户双写 | `PhpPlatformUserWriterImpl` + `user-provisioning.php-write-enabled` | 改单写主库（Q1，§4-E 保留 MP 直写路径） |
| 13 | 用户侧 10 亿区间隔离 | `LOCAL_ID_BOUNDARY` + `requireSaaSLocalUser` + TrainPeopleList/TrainDivision 来源过滤 | **本期一并移除**（Q10，单库下已失效） |
| 14 | 鉴权方案 v1.3 D1 | App 端 `X-Tenant-Id: 1` 硬编码默认值 | **双轨切换 10159**（Q7，§4-G） |

---

## 4. 后端代码改造清单（精确到文件）

### A. 拆除层（整文件删除）

| 文件 | 说明 |
|---|---|
| `integration/php/PhpDataSourceConfig.java` | 第二数据源 + 独立 SqlSessionFactory 注册 |
| `integration/php/PhpDataSourceHolder.java` | 持有器（坑 #19/#20 遗留） |
| `integration/php/PhpTokenQueryService.java` | App token PHP 回退（Q4） |
| `integration/php/PhpPlatformUserWriter.java` / `PhpPlatformUserWriterImpl.java` | 开户双写 |
| `integration/php/UserProvisioningProperties.java` | `php-write-enabled` 配置载体 + `LOCAL_ID_BOUNDARY` 常量（**删除前先迁移/移除引用点**，见 §4-E-3） |
| `integration/php/dto/*.java`（PhpCourseWeights/PhpExamPlanInfo/PhpKnowledgeNode/PhpQuestion/PhpQuestionDetail） | 随查询层一并迁移（DTO 被新实现复用则移包保留） |
| `integration/sync/*.java`（PlatformSyncService/Scheduler/Differ/LockManager/Properties/AlertService/StatusController） | 3 表镜像同步通道（Q1） |
| `exam/reviewsync/service/PhpResultWritebackService.java` | 异步回写 + 补偿状态机（Q3a，写入逻辑迁至 §4-C） |
| `config/AsyncConfig` 中 `reviewSyncExecutor` 线程池 Bean | 同步直写后无消费方，删除（Q9） |
| `integration/sync/` 下 3 个测试类 | 同步通道测试 |

### B. 迁移到主数据源的类（改造不删除）

| 文件 | 现状 | 改造要点 |
|---|---|---|
| `integration/php/PhpExamQueryService.java` | `phpJdbcTemplate` 查 PHP 库 | 改为主数据源查询：**新 MyBatis-Plus Mapper**（推荐，享租户拦截器+null 检查规范）或主 `JdbcTemplate`（@Primary）。查询表：`train_exam_list`/`train_exam_work_type_list`/`exam_course`/`exam_knowledge_structure`/`exam_exam_detail`/`exam_knowledge_rate(_detail)`/`train_exam_people_list`。**硬约束（v1.1 新增）**：这些表无 `created_at/updated_at/created_by/updated_by`、id 为 `int AUTO`，**新实体一律不继承 BaseEntity**、主键 `IdType.AUTO`、只映射实际存在的列；数值解析保留 `asInt/asLong` 的 Boolean 防御（实测相关列为普通 tinyint，无需改主数据源 URL）。类迁出 `integration/php` 包 → 落 `exam/reviewsync/service/ReviewQuestionQueryService`（统计与复习两消费者共享），避免残留"php"语义 |
| `security/filter/AppTokenAuthFilter.java` | 构造注入 `PhpTokenQueryService`，镜像 miss 后回退 | 删除回退分支与构造参数；`SecurityConfig` 对应构造调用同步改（Q4） |
| `config/SecurityConfig.java` | 注入 `phpTokenQueryService` 传给 filter | 删除该参数 |

### C. exam_result 同步直写（Q3a/Q9）

| 文件 | 现状 | 改造 |
|---|---|---|
| 写库机制 | `PhpExamQueryService.insertExamResult`：`phpJdbcTemplate` 执行 `INSERT INTO exam_result (type, exam_plan_id, paper_id, detail_id, get_score, exam_type, exam_id, platform_user_id, user_answer, tenant_id, create_time, deleted) VALUES (?, ?, 0, 0, ?, 5, ?, ?, ?, 10159, NOW(), 0)` | **SQL 一字不改，仅把数据源换为主库 `JdbcTemplate`（@Primary）**：`tenant_id` 继续硬编码 10159（不依赖请求 header、不经租户拦截器）；`id` 走合并表 `int AUTO_INCREMENT`（实测合并表与 PHP 表同构，INSERT 已在其上验证可行）；Druid wall 过滤器对旧式 VALUES SQL 无拦截（坑 #21）。落点：迁到主数据源后的 `ReviewQuestionQueryService`（或独立 `ExamResultWriteRepository`，由作答/模考事务内调用） |
| `ReviewPlanApplicationService.answer()/mockSubmit()` | `@Transactional` 内调用 `writebackService.writebackAsync(...)` | 改为**事务内同步调用**主库 INSERT；**行为变化（明示）**：exam_result INSERT 失败将导致整个作答/模考事务回滚，学员收到错误并可重试（原异步 best-effort 失败不影响主链路）——验收清单补该场景测试 |
| `exam/reviewsync/entity/AiReviewAnswerEntity.java` | `php_synced`/`php_sync_time` 列 | 列保留（历史数据）但**不再写入/使用**；可选 Flyway 后续清理（不建议本期动数据） |
| `exam/reviewsync/service/ReviewSyncScheduler.java` | 回写补偿 job（5min）+ 每日物化 + 周日清理 | 删 `writebackCompensate` job；保留物化/清理 job（其内 `TenantContextHolder.setTenantId(DEFAULT_TENANT_ID=10159)` 逻辑保留，线程边界规则不变） |
| 幂等 | 防重复写（作答幂等 attemptSeq 判重已有） | 同步写天然事务一致，同事务内防重即可；`exam_result` 无唯一键时依赖既有 `attemptSeq`/回放判重逻辑（不变） |

### D. 统计功能单库化（统计方案）

| 文件 | 改造 |
|---|---|
| `exam/reviewsync/service/ReviewSyncStatsApplicationService.java` | 移除对 `PhpDataSourceHolder`/`PhpExamQueryService` 的依赖：知识树标题、工种/等级、题目详情全部改主数据源查询；`train_exam_list` 主库读（已如此） |
| `exam/reviewsync/mapper/ReviewSyncStatsMapper.java` | 原跨库 SQL 改单库：题目详情链（本地优先→回退→降级）折叠为**一次本地查**（缺失即降级显示题号，不 500） |
| `TrainExamListApplicationService`（培训模块） | 删除 `requireSaaSLocalExam(examId)` 只读守卫（`examId<100000` 抛异常）——Q2；编辑/删除/考生操作恢复放行。**注意（v1.1）**：合并表 AUTO_INCREMENT=466，实施前窗口期内 SaaS 新建计划（id 466+ <100000）仍会被守卫误拦截，属已知过渡异常，本改造即修复 |
| 前端 `TrainExamListPage.tsx` | 删除"PHP 来源行"置灰/tooltip 逻辑（id<100000 判断）——Q2 |
| `infra/db/train-exam-list-import.sql` | 停用/归档（合并库已是同一张表，无需导入） |
| AUTO_INCREMENT | **更正（v1.1）**：合并表实测 `AUTO_INCREMENT=466`（并非"100000 已保留"），不再用于来源判定；无需回退（若需恢复普通自增另议，非本期必须） |

### E. 开户单写 + 用户侧区间隔离移除（Q1/Q10）

1. **开户单写路径（明确保留哪条）**：`trainingdepartment/service/PlatformUserEnsureService` 删除"先写 PHP 再写 SaaS"双写分支（`ensureWithPhpDualWrite`）与 `phpWriteEnabled` 判断，**统一走既有 MP 直写路径**（`PlatformUserEntity` 经主库 Mapper insert，`IdType.AUTO`，删除 `@TableLogic` 外的双写痕迹）。已实测合并表 `platform_user`（PHP schema：id int AUTO、无 tenant_id、无 created_at，addtime/logintime INT）与 `PlatformUserEntity` 字段一一对齐，MP 直写可行；dedup 查询（`idcard/phone ... ORDER BY id ASC LIMIT 1`）在单库下语义不变，保留。
2. **移除用户侧 10 亿区间隔离（Q10，合并表 id 为 int AUTO，区间隔离已失效）**：
   - `integration/php/UserProvisioningProperties.LOCAL_ID_BOUNDARY` 常量**直接删除**，同时删除其全部引用：`TrainDivisionApplicationService.java:741`、`TrainPeopleListApplicationService.java:14,511`（全限定名引用，拆包后编译会失败）——来源过滤逻辑一并移除（单库下不再区分来源，列表行为以测试回归确认）；
   - `PlatformUserApplicationService` 的 `requireSaaSLocalUser`（10 亿区间守卫）删除，调用点放行。
3. **php-write-enabled 残留清理**：`ClassPrepRosterImportService.java:8,129-131`、`TrainSignUpPeopleImportService.java:21,160-168,351` 的 `UserProvisioningProperties.isPhpWriteEnabled()` 警告分支删除（直接走主库 ensure）；前端 `ClassSignUpPeoplePanel.tsx:368,401,431`、`TrainingInformationPage.tsx:408,460` 的 "php-write-enabled=false" 错误探测文案删除。
4. `application.yml` 删除 `platform.user-provisioning.php-write-enabled` 配置段（保留配置无害，但代码已无读取点）。

### F. 配置与 Flyway

| 项 | 动作 |
|---|---|
| `application.yml` php 数据源段（约 279 行起） | **删除**（`PHP_MYSQL_URL/USERNAME/PASSWORD/MAX_ACTIVE/MAX_WAIT_MS/QUERY_TIMEOUT`、`tinyInt1isBit` 注释段） |
| `application.yml` `platform.sync.*` 段（约 265-278 行） | 删除；`platform.review-sync.jobs-enabled` 保留（物化/清理 job 仍用） |
| `infra/deploy/application-prod.yml:70-90` | 删除 sync 段 + PHP 数据源段（注意：该文件 prod 默认 `sync.enabled=true`，一并清理） |
| 生产 env `platform-api.env` | 删除 `PHP_MYSQL_*` 三行与 `PLATFORM_SYNC_ENABLED`、`PLATFORM_USER_PHP_WRITE_ENABLED`（过渡指向 gjjddbpro 的配置随之移除；主数据源已是 gjjddbpro） |
| `.run/PlatformApplication.run.xml` | 删除 `PLATFORM_SYNC_ENABLED=false` 行 |
| `infra/db/auth-token-sync-ops.sql` | 停用/归档（sync 通道已拆） |
| `scripts/dbmerge/06_daily_check.py` | **改口径**：其检查项 3 专查"sync 通道未运行"，sync 拆除后该检查项删除或改为"代码无 sync 引用"；对账基准表 KEY_TABLES 维持 |
| `sys_dict` | `ai_review_model_code` 等参数不变（已实测全部 `ai_review_*` 字典 tenant_id=10159/enabled；LLM 配置 `exam_faq_model_config` 的 `deepseek-review` 行 10159/enabled，与数据源无关） |
| Flyway | **本期无新表**；如需清理 `php_synced` 列、或加 `exam_result` 唯一键（防重写），另立新迁移（先查最大版本，坑 #7）——建议本期不加，保持最小改动 |
| `saas_sync` 账号（114） | 观察期后回收（阶段 4 一并处理，不在本期代码改动内） |

### G. App 租户切换双轨（Q7，v1.1 新增——原方案将此视为既成事实，实测不成立）

**问题**：`gjjd_appnext` 现发 `X-Tenant-Id: 1`（`utils/saasRequest.js:22`），08-25 当日仍以 tenant_id=1 产生新数据；`/api/appreviewsync/**` 不在 TenantContextFilter 白名单，header 即租户，若只改后端则上线即查空/写错/撞唯一键。

**第一轨（服务端强制覆盖，本期必须，保证零 App 发版上线）**：

- `common/filter/TenantContextFilter.java` 增加前缀特例：`/api/appreviewsync/**` **忽略 header，强制 `TenantContextHolder.setTenantId(DEFAULT_TENANT_ID=10159)`**（该前缀全部业务数据均为 10159，强制覆盖语义正确；不满足"缺失 header 即 400"的旧规则，白名单注释同步更新）。
- 可选实现（二选一，建议前者）：① TenantContextFilter 前缀特例；② `AppTokenAuthFilter` 鉴权通过后 `TenantContextHolder.setTenantId(10159)` 覆盖（链序 TenantContextFilter→AppTokenAuthFilter，可行但仅覆盖鉴权通过请求，未带 token 的 permitAll 请求仍按 header）。
- 配套：`AppReviewSyncController.java:27` 陈旧注释（"header=1、principal 恒为 0"）同步更正。

**第二轨（App 发版，后续版本，长期兜底）**：

- `gjjd_appnext/utils/saasRequest.js`：`DEFAULT_SAAS_TENANT_ID` 改为 10159；`getSaasTenantId()` 的 storage 读取做迁移（存量设备 `saas_tenant_id='1'` 时视为旧值，改用新 key `saas_tenant_id_v2` 或直接清除）；企业调研页 `Saasdetail.vue` 的 `setSaasTenantId(data.tenantId)` 写入口径一并核对（防止其回写 1 污染 storage）。
- App 发版**不阻塞**本期上线（服务端第一轨已兜底），列为观察期并行事项。

### H. 数据修复（Q8，一次性运维 SQL，v1.1 新增——非 Flyway）

> 依据：`uk_ai_review_plan_user_exam=(platform_user_id, exam_id)` 不含 tenant_id，遗留行**不能靠切租户后重新生成**（会撞唯一键）；58 行 `php_synced=0` 在拆除补偿 job 后永不回写。修复在**部署窗口内**执行（先服务端租户强制覆盖生效，后跑 SQL，避免双轨窗口再产生 tenant=1 行）。脚本落 `infra/db/merge-single-db-repair.sql`，执行前 `SELECT` 复核行数与本文一致。

| 步骤 | SQL 要点 | 说明 |
|---|---|---|
| 1. ai_review_* 归一 | `UPDATE ai_review_plan / plan_day / answer / wrong_item SET tenant_id=10159 WHERE tenant_id=1`（实测 2/2/58/26 行；gray/mock 无 1 租户行，跳过） | 唯一键不涉 tenant，UPDATE 不撞键 |
| 2. 知识树修复 | `UPDATE exam_knowledge_structure SET tenant_id=10159 WHERE tenant_id=1`（仅 id=13910 一行，course 205「职业道德」L1） | **先人工确认该节点应归属 10159**（若是误建垃圾行则改删除，需认定部确认） |
| 3. 补写 exam_result | 对步骤 1 归一后 `ai_review_answer WHERE php_synced=0` 的 58 行，按 §4-C 字段口径生成 `INSERT INTO exam_result (type, exam_plan_id, paper_id, detail_id, get_score, exam_type, exam_id, platform_user_id, user_answer, tenant_id, create_time, deleted)`：`type=question_type`、`exam_plan_id`、`paper_id=0`、`detail_id=0`、`get_score=COALESCE(earned_score, IF(is_correct=1, question_score, 0))`、`exam_type=5`、`exam_id=question_id`、`user_answer`、`tenant_id=10159`、`create_time`、`deleted=0`；完成后置 `php_synced=1` | 一次性补齐 PHP 管理端统计缺口 |
| 4. sys_user 脏值确认 | `sys_user` 中 `tenant_id=2062723418189152258` 的 1 行人工确认后修正 | 归一化遗留，随窗口顺带处理 |

---

## 5. 改造后架构

```
手机端 uni-app（saasReview：Bearer <itoken>；X-Tenant-Id 旧端=1/新端=10159）
  │
  ▼
SaaS platform-api（仅主数据源 gjjddbpro）
  ├─ TenantContextFilter：/appreviewsync/** 强制租户 10159（Q7 第一轨，忽略 header）
  ├─ security/filter/AppTokenAuthFilter（主库 platform_user_token 验票，无 PHP 回退）
  ├─ app/reviewsync/controller + exam/reviewsync/*（规则引擎不变）
  │    ├─ 题目/知识树/权重/计划/工种课程：主库 Mapper 直查（租户拦截器=10159；实体不继承 BaseEntity）
  │    ├─ 作答/模考/错题/鼓励：主库（既有 ai_review_* 表，不变）
  │    └─ exam_result：作答事务内同步 INSERT（主库 JdbcTemplate，exam_type=5, tenant_id=10159 硬编码）
  ├─ exam/reviewsync/stats：主库聚合（知识树/题目详情单库直查）
  └─ 定时任务：每日物化 04:00 / 周日清理 05:00（tenant=10159 手动 set/clear，线程边界规则不变）
gjjd_phalcon（PHP）：继续读/写 gjjddbpro（与 SaaS 同库，无跨库语义）
```

**关键不变项**：规则引擎（调度/错题状态机/中断 ABCD/组卷/鼓励文案）纯逻辑零改动；`ai_review_*` 表结构零改动；App 接口契约零改动；统计 7 个 API 契约零改动。

---

## 6. 数据库与租户行为核验清单（Q6，逐表——已实测，标注结果）

> 执行方式：合并库只读 SQL + 代码走查。**下表"实测"列已按 2026-08-25 真实数据填写**；上线前重新跑一遍确认无新增漂移。

### 6.1 有 tenant_id 列的表（拦截器自动拼 =10159）

| 表 | 实测结果（2026-08-25） | 处置 |
|---|---|---|
| `exam_exam_detail` | 372870 行全 10159 ✅ | 无 |
| `exam_knowledge_structure` | 21936@10159 + **1 行@1**（id=13910） | §4-H 步骤 2 修复 |
| `train_exam_work_type_list` | 1067 全 10159 ✅ | 无 |
| `train_exam_list` | 462 全 10159 ✅（AUTO_INCREMENT=466） | 无 |
| `exam_knowledge_rate(_detail)` | 82+34 / 1051 全 10159 ✅ | 无 |
| `exam_course` | 92 全 10159 ✅ | 无 |
| `exam_result` | 262963 全 10159 ✅ | 无 |
| `train_exam_people_list` | 15454 全 10159 ✅ | 无 |
| `ai_review_answer` | 2792@10159 + **58@1（php_synced=0）** | §4-H 步骤 1/3 修复 |
| `ai_review_plan` | 25@10159 + **2@1**（08-25 05:05 产生） | §4-H 步骤 1 修复 |
| `ai_review_plan_day` | 52@10159 + **2@1** | §4-H 步骤 1 修复 |
| `ai_review_wrong_item` | 1054@10159 + **26@1** | §4-H 步骤 1 修复 |
| `ai_review_mock_paper` / `gray_whitelist` | 6 / 4 全 10159 ✅ | 无 |
| `train_exam_review` / `train_exam_shijuan` / `exam_paper` | 本功能链路无查询代码（走查确认），无需核验 | 无 |

### 6.2 无 tenant_id 列（ignoreTable 白名单，拦截器不拼，须在 `MybatisPlusConfig.ignoreTable` 内）

| 表 | 核验 |
|---|---|
| `platform_user` / `platform_user_token` / `platform_user_detail` | 已在 ignoreTable（当前配置，19 张）；改造后走主库直查，行为不变。`platform_user.id` 为 int AUTO（PHP 口径），App 登录验票走 `platform_user_token ORDER BY id ASC LIMIT 1`（对齐 PHP findFirst，坑 #22 语义保留） |
| 其它 ignoreTable 表（train_people_list 等） | 维持现状，走查确认无新增依赖表漏出白名单 |

### 6.3 走查点

- 新 Mapper 若用 MyBatis-Plus `LambdaQueryWrapper`：全部隐式带 tenant 过滤 → 与强制租户 10159 一致；**新实体不继承 BaseEntity、主键 IdType.AUTO**（§2 实测列约束）；
- 若个别统计 SQL 需要"跨租户/不租户过滤"（如平台级导出），必须显式走 `@InterceptorIgnore(tenantLine="true")` 或白名单表，**不得**再引入第二数据源；
- `platform_user_token` 验票在主库（ignoreTable），无租户过滤——与合并前镜像行为等价；
- `/appreviewsync/**` 的租户由 TenantContextFilter 强制 10159（Q7），不再信任 App header。

---

## 7. 测试方案

### 7.1 单元测试（规则引擎零改动，重点补新查询/写路径）

- **主库查询层**：题目全集解析（复刻 `appreview/sortlist` 口径）、权重回退链（type=3/0/无配置三态）、知识树 L1 聚合——断言 SQL 拼出的条件含 `tenant_id=10159`（MyBatis-Plus 拦截器行为单测）；
- **exam_result 同步写**：作答→事务内 INSERT 断言字段（`exam_type=5`、`tenant_id=10159`、`get_score=earned_score`、`deleted=0`）；幂等（同 attemptSeq 回放不重复 INSERT）；**INSERT 失败→作答事务回滚、无孤儿 ai_review_answer 行（v1.1 新增）**；
- **AppTokenAuthFilter**：主库 token 命中/未命中两态（无 PHP 回退分支——删除后编译期即验证）；
- **TenantContextFilter（v1.1 新增）**：`/api/appreviewsync/**` 无论 header=1 / 10159 / 缺失，TenantContextHolder 恒为 10159；
- **train_exam_list**：删除只读守卫后，编辑/删除/考生操作对全部 id 放行（原 id<100000 用例改为通过用例）；
- **用户侧区间隔离移除（Q10，v1.1 新增）**：删除 `requireSaaSLocalUser`/LOCAL_ID_BOUNDARY 过滤后，`PlatformUserApplicationService`/`TrainPeopleListApplicationService`/`TrainDivisionApplicationService` 相关用例改为"不再按来源过滤"的通过用例。

### 7.2 集成测试（单容器化，Testcontainers）

- 原"双 MySQL 容器"改为**单容器**：灌 `gjjddbpro` 合并后 schema + 关键 fixtures（计划/工种/课程/知识点/题目/学员/错题/作答/考试计划含 id<100000 与 ≥100000 各若干）；
- API 契约：`/appreviewsync/overview|today|question|answer|mock/start|mock/submit|finish|progress|explain|advance-day|availability` 全正例 + 异常例（无 token 401、坏 token 401、学员不在计划、计划未绑工种、题库为空）；
- 统计 7 API：指标与 §4 口径一致（含错题清单题目详情单库直查）；
- 租户断言：主数据源查询自动拼 `tenant_id=10159`（删除"独立工厂无拦截器"旧断言，替换为"主库查询带 10159"新断言）；**header=1 时 /appreviewsync 仍按 10159 读写（v1.1 新增）**；
- 异步边界：物化/清理 job 线程内不调 SecurityContext（既有回归用例保留）；
- 数据修复 SQL（§4-H）在 fixtures 上演练：归一 UPDATE 不撞唯一键、58 行口径补写 exam_result 结果与 PHP trainsave 对拍一致（v1.1 新增）。

### 7.3 E2E 冒烟（真机/真库，改造后）

1. App 登录（itoken 透传，旧端发 X-Tenant-Id:1 / 新端发 10159 均可用）→ overview 生成计划（落 tenant_id=10159）；
2. 答题（答错 3 题）→ 错题入册红牌 → 次日复盘 → 连续答对降级；
3. 模考交卷 → **主库 `exam_result` 出现 `exam_type=5` 新行**（同步写，无延迟）；PHP 管理端统计可见；
4. 中断模拟（改 last_active_date）→ 次日 ABCD 重排；
5. 管理端统计：7 API 数据与合并前一致（对照迁移前快照）；
6. PHP 侧回归：顺序练习/随机练习/模拟考试不受影响；
7. 改造前产生的 2 个 tenant=1 活跃计划（用户 1286/4530）修复后学员端可正常续用（§4-H 验证）。

---

## 8. 验收清单（逐项达成方可上线）

> v1.2 标注：[x]=本地已验证（代码/单测/冒烟）；[ ]=待部署窗口（§9.1 步骤 3-6）执行后勾选。

- [x] `integration/php` 与 `integration/sync` 包全部删除，代码库无 `PhpDataSourceHolder`/`PhpTokenQueryService`/`PlatformSync*`/`LOCAL_ID_BOUNDARY`/`PhpPlatformUserWriter` 引用（编译即证，2026-08-27 `mvn compile` 0 错 + 全仓 grep 复核）；
- [x] `application.yml`/`application-prod.yml` 删除 php 数据源段与 sync 段；`PLATFORM_SYNC_ENABLED`/`PLATFORM_USER_PHP_WRITE_ENABLED` 开关配置随之清理；`reviewSyncExecutor` 按偏差①改名 `reviewLlmAssistExecutor` 保留（仍有 ReviewLlmAssistService 消费）。**生产 env 删除 `PHP_MYSQL_*` 属部署窗口事项，未勾；**
- [x] `AppTokenAuthFilter` 无 PHP 回退：主库 token 缺失 → 401；token 命中 → 正常放行（`AppTokenAuthFilterTest` 4 用例：命中/未命中/用户已删/无 header）；
- [x] `/appreviewsync/**` 租户强制 10159：header=1/10159/缺失三态均按 10159 读写（`TenantContextFilterTest` 7 用例 + 2026-08-27 真启动 HTTP 冒烟：header=1/无 header/坏 token 均 401 而非 400/查空，对照普通接口无 header 仍 400）；
- [x] 题目全集/知识树/权重/工种课程查询全部走主数据源，SQL 自动拼 `tenant_id=10159`，新实体不继承 BaseEntity（`ReviewQuestionQueryServiceTest` 11 用例：findCourseId 兜底/level 容错/缓存读穿/权重三态）；
- [x] `exam_result` 同步直写：作答事务内 INSERT 成功、字段口径正确（`ExamResultWriteRepositoryTest` SQL 与参数序逐字断言）、php_synced=1 置位、失败传播回滚、幂等回放不写（`ReviewAnswerExamResultSyncTest` 4 用例）；**§7.3-3 真机无重复行核验待部署窗口；**
- [x] 错题清单题目详情单库直查，缺失降级显示题号不 500（ReviewSyncStatsApplicationService 已折叠为 examDetailMapper 单次主库读）；**统计 7 API 与合并前数据一致（§7.3-5）待部署窗口真库对比；**
- [x] `train_exam_list` 只读约束移除：id<100000 行编辑/删除/考生操作放行（`TrainExamListGuardTest` 6 用例改写为放行回归）；前端无置灰逻辑（TrainExamListPage 已删 isPhpSource，`tsc -b` 0 错）；
- [x] 用户侧 10 亿区间隔离移除：`requireSaaSLocalUser`/LOCAL_ID_BOUNDARY 全部删除，列表不再按来源过滤（`PlatformUserGuardTest` 4 用例改写为放行回归）；
- [x] 开户单写：新建账号仅落主库 `platform_user`（MP 直写路径），无 PHP 双写，dedup 语义不变（`PlatformUserEnsureServiceTest` 7 用例重写为单写语义）；
- [ ] §4-H 数据修复执行并核验（**部署窗口事项**：`infra/db/merge-single-db-repair.sql` 已备好，含步骤 0 SELECT 复核 → 归一/补写 → 验收全 0 查询 + §9.2 回滚防重写注释）；
- [ ] §6.1 逐表核验复跑全绿（**部署窗口事项**：生产真库执行）；
- [ ] §10.5 量化验收表（复习方案）除"回写 exam_result 成功率/补偿"改为"同步写成功率 100% 且无死信"外，其余逐项仍通过（**部署窗口事项**）；
- [x] `mvn compile` 0 错、`npx tsc -b` 0 错、本期相关单测全绿（12 个测试类 63/63；全量 791 中 18 个失败经 HEAD 基线 worktree 对照逐一确认均为改造前既有遗留：认证/竞赛模块迁移契约测试读不存在的迁移文件、@SpringBootTest Flowable 引擎需真库环境——非本期引入，见 §12.3）；
- [ ] 观察期对账（06_daily_check.py 改口径后）在改造后连续 2 日 issues=0；PHP 端业务无异常反馈（**部署后事项**）。

---

## 9. 实施顺序与回滚

### 9.1 实施顺序（Q5：观察期压缩）

| 步骤 | 内容 | 说明 |
|---|---|---|
| 0 | 当前观察期压缩至半天（旧库只读保留） | 需求方已确认；期间 06_daily_check 继续跑 |
| 1 | 分支开发（不改生产）：按 §4 清单拆包/删包/迁移查询/同步直写/守卫移除/租户强制覆盖（Q7 第一轨） | 遵守 AGENTS.md 并行约束（integration/php 单 task；UserProvisioningProperties 删除与 training 模块引用清理同一 task） |
| 2 | `mvn compile` + §7.1/§7.2 测试全绿（含 §4-H 修复 SQL 演练） | 单容器集成测试改造一并提交 |
| 3 | 部署生产（guoyu-platform-api 重启）+ 生产 env 清理 PHP_MYSQL_* | 停窗口内执行（预计 <5min）；PHP 端零改动 |
| 4 | **部署窗口内立即执行 §4-H 数据修复 SQL**（先跑 SELECT 复核行数，再 UPDATE/补写） | 与步骤 3 同窗口：服务端租户强制覆盖已生效，此后不再产生 tenant=1 新行 |
| 5 | E2E 冒烟（§7.3）+ 验收清单逐项 | 需求方真机/管理端抽查；重点 §7.3-7（遗留计划续用） |
| 6 | 观察 2 日（对账 + 业务反馈）→ 通过后进入正常运营；App 发版（Q7 第二轨）并行推进，不阻塞 | 回滚窗口同步 |

### 9.2 回滚

- **代码回滚**：git revert 本次改造分支，重建并部署旧 jar；生产 env 恢复 `PHP_MYSQL_URL=localhost:3308/gjjddbpro`（过渡值仍在）与 `PLATFORM_SYNC_ENABLED=false`；
- **数据防重写（v1.1 新增，必须）**：回滚前记录部署时刻 `ai_review_answer` 的 `MAX(id)`，回滚后先执行 `UPDATE ai_review_answer SET php_synced=1 WHERE php_synced=0 AND id > :maxIdBeforeDeploy`——防止旧代码的补偿 job 把改造窗口内已同步直写的作答**再写一遍 exam_result 造成重复行**；
- **数据安全**：本次改造不改任何表结构（§4-H 的 UPDATE 归一不可逆但语义正确，回滚后旧代码以 10159 过滤仍可见）；exam_result 写入路径变同步为行为等价；
- **旧库回退预案**（若 PHP 需回滚旧库场景）：按数据库合并方案阶段 3 预案执行（配置切回 + 增量补回），与本次改造无耦合。

---

## 10. 遗留清理（观察期/后续阶段）

- [ ] `saas_sync` 账号（114 旧库）授权回收（阶段 4）；
- [ ] 旧三库归档 dump + owner 亲自 DROP（阶段 4.1/4.2）；
- [ ] 114→43 临时 SSH 密钥移除；
- [ ] App 发版（Q7 第二轨）：`DEFAULT_SAAS_TENANT_ID=10159` + storage 迁移 + Saasdetail.vue 写入口径核对；
- [ ] `sys_user` 脏 tenant_id 行（2062723418189152258）确认处置；
- [ ] 本方案涉及文档同步修订：《AI同步学（学员复习）实现方案》§3/§4.7/§13、《AI同步学统计功能设计方案》§2/§3/§5、鉴权方案 v1.3（App 端 header 口径）、数据库合并方案 §4.3 实施记录。

---

## 11. 风险与对策

| 风险 | 等级 | 对策 |
|---|---|---|
| App 端 header 仍为 1，主数据源查询按 1 过滤导致查空/写错/撞唯一键 | **高（已证实正在发生）** | Q7 双轨：服务端 /appreviewsync 强制 10159 本期上线；App 发版改默认值兜底（§4-G） |
| 遗留 tenant_id=1 行（2 plan/2 day/58 answer/26 wrong + 知识树 1 行）漏修 | 高 | §4-H 部署窗口内修复；uk 唯一键不含 tenant_id，修复后才能避免重新生成撞键 |
| 58 行 php_synced=0 作答丢失（拆补偿 job 后永不回写） | 中 | §4-H 步骤 3 一次性补写 exam_result |
| exam_result 同步写在事务内增加主库写压力 / INSERT 失败使作答回滚 | 低 | 单条 INSERT + 索引已存在；压测确认 P95 ≤ 既有回写口径；失败回滚行为已明示并纳入测试 |
| 回滚后旧补偿 job 重写窗口内数据造成 exam_result 重复 | 中 | §9.2 回滚前标记 php_synced=1（v1.1 新增步骤） |
| 新 Mapper 实体误继承 BaseEntity → Unknown column（合并表无 created_at 等列） | 中 | §4-B 硬约束 + 编译/单测断言 |
| 主数据源查询带租户过滤导致个别统计漏行（表 tenant_id 非 10159 的脏行） | 中 | §6.1 实测复核已完成；上线前复跑全绿才上线 |
| 拆除 sync 后旧 3 表镜像的存量依赖（如其它模块读镜像表） | 中 | 全仓引用扫描（§4 文件清单 + `grep -rln` 复核无遗漏）；观察期对账盯 401 率 |
| 用户侧区间隔离移除后列表行为变化 | 低 | Q10 已确认；TrainPeopleList/TrainDivision 相关用例改为通过用例回归 |
| 题目详情缓存回源改主库后，Caffeine 命中率/并发行为变化 | 低 | 缓存逻辑不动，仅换数据源；压测 P95 对比 |
| PHP 端仍按"自己的库"假设（如直连 114 配置残留） | 低 | PHP 已切 172.16.16.2:gjjddbpro；运维复核 4 个 vhost 配置无回退 |

---

## 12. 实施记录（v1.2，2026-08-27）

### 12.1 改动清单

**后端（services/platform-api）**
- 删除：`integration/php/` 整包（12 文件含 dto）、`integration/sync/` 整包（7 文件）、`exam/reviewsync/service/PhpResultWritebackService.java`、3 个 sync 测试类。
- 新建：`exam/reviewsync/dto/` 5 个 record（ReviewQuestion/ReviewQuestionDetail/ReviewKnowledgeNode/ReviewCourseWeights/ReviewExamPlanInfo）；`ReviewQuestionQueryService`（主数据源查询层，注入 8 个既有 Mapper，题目详情 Caffeine 读穿缓存保留仅换数据源）；`ExamResultWriteRepository`（主库 JdbcTemplate，SQL 与双库时代一字不改）。
- 改造：`ReviewPlanApplicationService`（answer/mockSubmit 事务内直写 exam_result + php_synced=1 置位 + examResultScoreOf 口径）；`ReviewSyncScheduler`（删 writebackCompensate job）；`ReviewSyncStatsApplicationService`（统计单库折叠，题目详情单次主库读、缺失降级不 500）；`AppTokenAuthFilter`（删 PHP 回退）；`TenantContextFilter`（/appreviewsync/** 强制 10159）；`PlatformUserEnsureService`（单写重写）；`TrainExamListApplicationService`/`PlatformUserApplicationService`（守卫与区间常量删除）；`TrainPeopleListApplicationService`/`TrainDivisionApplicationService`（列表来源过滤移除）；`WeightResolver`/`QuestionSelectDomainService`/`AnswerJudgeDomainService`（DTO 改名）；`AsyncConfig`（Bean 改名 reviewLlmAssistExecutor）；`ReviewLlmAssistService`（@Qualifier 同步）。
- 配置：`application.yml`/`application-prod.yml`/`src/test/resources/application.yml` 删 sync 与 user-provisioning 段；`.run/PlatformApplication.run.xml`、`infra/env/platform-api.env.example`、`scripts/dbmerge/06_daily_check.py`（检查口径改"无残留"）同步清理。

**前端（apps/admin-web）**：`TrainExamListPage.tsx` 删 PHP 来源置灰；`ClassSignUpPeoplePanel.tsx`（3 处）/`TrainingInformationPage.tsx`（2 处）删 php-write-enabled 探测提示。

**手机端（gjjd_appnext）**：`utils/saasRequest.js` `DEFAULT_SAAS_TENANT_ID=10159`；storage 旧值 '1' 读取时清除回落新默认；`setSaasTenantId('1')` 不写入（防 Saasdetail.vue 回写污染）。

**数据修复脚本**：`infra/db/merge-single-db-repair.sql`（§4-H，步骤 0 复核 → 归一/13910 修复/58 行补写 → 验收全 0 + §9.2 回滚防重写注释）；`infra/db/archive/README.md` 归档说明。

### 12.2 实施中修正的方案偏差（5 处）

| # | v1.1 方案表述 | 实施修正 |
|---|---|---|
| 1 | `reviewSyncExecutor` 线程池删除（称无消费方） | 实测 `ReviewLlmAssistService` 仍注入消费；Bean 改名 `reviewLlmAssistExecutor` 保留池子，消灭"sync"语义残留 |
| 2 | php_synced=1 仅出现在回滚预案 | answer()/mockSubmit() 直写路径同步置位——否则回滚旧代码时补偿 job 会把窗口内已直写作答重写一遍 |
| 3 | （未覆盖）train_exam_work_type_list.level 类型 | 实体 String、合并库实为 tinyint；`parseLevel` 对数字串/数值/空串容错解析，空值不触发 findCourseId 兜底 |
| 4 | 权重多条告警用查询结果 size 判断 | LIMIT 1 后 size 恒 ≤1 是死代码；改回 `selectCount` 独立判重后再取最大 id 行 |
| 5 | 拆除面清单 | 补 `src/test/resources/application.yml` sync 段、`SyncStats.phpWritten` 字段、`.run` xml 的 `PLATFORM_SYNC_ENABLED=false` 行 |

### 12.3 构建 / 测试 / 冒烟结论

- `mvn compile` 0 错；`mvn clean package -DskipTests` 通过；`apps/admin-web` `npx tsc -b` 0 错。
- 本期相关测试 **12 类 63/63 全绿**：改写 PlatformUserGuardTest(4)/TrainExamListGuardTest(6)/PlatformUserEnsureServiceTest(7)/ClassPrepRosterImportServiceTest(3)/ReviewGrayReleaseGateTest(4)/WeightResolverTest(6)/QuestionSelectDomainServiceTest(5)；新增 TenantContextFilterTest(7)/AppTokenAuthFilterTest(4)/ReviewQuestionQueryServiceTest(11)/ExamResultWriteRepositoryTest(2)/ReviewAnswerExamResultSyncTest(4)。
- 全量 `mvn test`：791 个中 11 失败 + 7 错误，**全部位于 certification/competition 模块（本期零改动）**；在 HEAD 建干净 worktree 跑同 8 个测试类得到**完全相同的 11F+7E**，确认为改造前既有遗留（迁移契约测试读已被重命名的迁移文件、@SpringBootTest Flowable 引擎需真库），非本期引入。
- 启动冒烟（clean jar，SERVER_PORT=18080）：30s 启动，actuator/health UP。HTTP 级验证：`POST /api/appreviewsync/overview` 在 header=1 / 无 header / 坏 token 三态均 **401**（强制 10159 生效、不再 400）；对照普通接口无 header 仍 **400**。
- **部署注意**：必须 `mvn clean package`——增量编译会把已删除包的旧 class 残留在 `target/classes`（本次冒烟抓到 `PhpResultWritebackService.class` 残留导致 `@Qualifier("reviewSyncExecutor")` NoSuchBeanDefinition），直接进 jar 会启动失败。
- 单测环境注意：纯 Mockito 下 `LambdaQueryWrapper.select(SFunction)` 是即时解析，需 `TableInfoHelper.initTableInfo` 手工初始化合并表实体（`ReviewQuestionQueryServiceTest` 已示范）。

### 12.4 部署窗口待办（§9.1 步骤 3-6，需需求方发令）

1. 生产部署新 jar（clean 构建）+ 生产 env 清理 `PHP_MYSQL_*`/`PLATFORM_SYNC_ENABLED`/`PLATFORM_USER_PHP_WRITE_ENABLED`；
2. 同窗口执行 `infra/db/merge-single-db-repair.sql`（先步骤 0 SELECT 复核 2/2/58/26/1，再执行修复）；
3. §7.3 E2E 冒烟（重点：遗留计划续用、作答后 exam_result 无重复行、统计 7 API 对比）；
4. 改造后连续 2 日 06_daily_check issues=0 后进入正常运营；App 发版（Q7 第二轨）并行推进。
