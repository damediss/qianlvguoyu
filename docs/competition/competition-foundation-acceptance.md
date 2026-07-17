# 竞赛基础链路 FND-06 验收

## 1. 验收范围

本轮覆盖后台基础链路、权限契约、多租户数据范围、文件上传与名单预检、批次确认、资格审核、通知发布，以及真实云端 Flyway/API 启动验收。

不包含选手端、Player Token、短信/身份证、任务实例、成果文件、H5、题库、考试、成绩排名、晋级、专家工作台和 AI/MCP。

## 2. 已完成修正

- 参与企业查询使用 `competition:event:view + competition:event-org:view`；写入使用 `competition:event-org:manage`。
- 赛段查询使用 `competition:event:view + competition:stage:view`；写入使用 `competition:stage:manage`。
- 赛事创建统一使用 `competition:event:create`，停用旧的 `competition:enterprise-event:create`。
- 通知页面统一使用后端契约 `competition:notice:publish`。
- 新增批次分页接口 `GET /api/competition/events/{eventId}/roster-imports`，支持 `orgId/status/batchNo/page/pageSize`。
- 名单页面改为“Excel 上传 → `/api/files` 获取 `FileVO.id` → 选择组织 → precheck → 批次列表/详情 → pending_confirm 确认”。
- 名单预检在读取文件前校验 `.xlsx/.xls`；不接受 CSV、TXT 等扩展名。
- 平台管理员按租户范围操作；企业用户只访问所属组织；企业内部赛事额外限制 `ownerOrgId`；参与企业列表和名单批次均执行组织范围过滤。
- VO 补齐赛事、参与企业、通知、名单批次所需的审计字段和组织字段；批次详情 `batchId` 已补齐。

## 3. Flyway 验收

新增迁移：

`services/platform-api/src/main/resources/db/migration/V20260716_005__complete_competition_permission_contract.sql`

内容包括：

- `competition:event-org:view`
- `competition:stage:view`
- `competition:stage:manage`
- 为平台管理员角色 `role_id=2000` 授权上述权限
- 停用旧的 `competition:enterprise-event:create`

真实云端数据库连接成功，数据库为 `gjjddb2026`，MySQL 版本 `8.4.9`。以干净构建产物检查，迁移资源包含 `V20260716_005`，不包含误命名的旧 `V20260715_007__complete_competition_permission_contract.sql`。

注意：首次非 clean 启动曾因 `target/classes` 残留旧资源触发 Flyway orphan history repair，并在历史表中留下同描述的旧版本记录；源代码未修改已提交历史迁移。之后已停止实例、执行 `mvn clean package`，并使用 `spring.flyway.enabled=false` 的干净产物验证 API，避免再次改写云端 Flyway history。

## 4. 真实 API 验收记录

临时验收实例：`platform-api`，本机端口 `18080`；干净产物复核实例端口 `18081`。

测试租户为 `demo`，测试账号为平台管理员；测试赛事编码以 `FND06-20260716-*` 标识，名单使用无敏感信息的测试员工编号。

| 场景 | 结果 | 关键结果 |
|---|---|---|
| `/api/actuator/health` | PASS | `200`，`UP` |
| 登录 `/api/auth/login` | PASS | `code=0`，返回有效登录态；权限包含新增权限 |
| 创建平台赛事 | PASS | 赛事状态 `draft` |
| 绑定组织并激活 | PASS | 组织关系 `invited → active` |
| 创建赛段/任务模板 | PASS | 均返回 `code=0` |
| 发布赛事 | PASS | `draft → published` |
| `/api/files` multipart 上传 Excel | PASS | 返回 `FileVO.id` |
| 名单 precheck | PASS | `total=1，valid=1，invalid=0，pending_confirm` |
| 批次分页/详情 | PASS | 返回 `batchId/batchNo`；干净产物复核详情 `batchId=id` |
| 批次确认 | PASS | `pending_confirm → confirmed` |
| 资格分页 | PASS | 生成 1 条 `pending_audit` 资格 |
| 资格审核 approve | PASS | `pending_audit → eligible_unactivated` |
| 通知创建/发布 | PASS | 草稿创建和发布均返回 `code=0` |
| 未登录访问赛事列表 | PASS | HTTP `401`，`Authentication required` |
| 非法赛事 scope | PASS | HTTP `400`，`赛事范围不合法` |
| 非 Excel 名单预检 | PASS | HTTP `400`，`名单文件仅支持 .xlsx 或 .xls` |

## 5. 本地构建与测试

| 命令 | 结果 |
|---|---|
| `mvn -Dtest=com.guoyu.platform.competition.** test -f services/platform-api/pom.xml -s scripts/custom-maven-settings.xml` | PASS，102 tests，0 failures，0 errors，0 skipped |
| `mvn clean package -DskipTests -f services/platform-api/pom.xml -s scripts/custom-maven-settings.xml` | PASS，`BUILD SUCCESS` |
| `npm run build`（`apps/admin-web`） | PASS，TypeScript/Vite 构建成功 |
| `git diff --check` | PASS |
| BOM 扫描 | PASS |

## 6. 验收结论

FND-06 后台基础链路、权限契约、安全数据范围和可运营性要求已通过静态测试、MockMvc、真实云端 Flyway 启动和真实 HTTP API 链路验收。真实验收产生的测试赛事、文件、批次、资格和公告保留在云端租户 `demo` 中，均带有 `FND06` 标识，便于后续运营侧核查。
