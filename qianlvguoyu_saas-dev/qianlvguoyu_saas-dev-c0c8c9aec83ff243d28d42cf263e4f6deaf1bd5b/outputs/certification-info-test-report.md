# 认定业务-信息管理模块测试报告

测试日期：2026-06-02

## 1. 后端模块测试

执行命令：

```powershell
$env:JAVA_HOME='D:\Program Files\Java\oracleJdk-21'
& 'C:\Users\pc\AppData\Local\Programs\apache-maven-3.9.11\bin\mvn.cmd' -f services/platform-api/pom.xml '-Dtest=CertificationInfoApplicationServiceTest,CertificationInfoMigrationTest' test -s scripts/custom-maven-settings.xml
```

结果：通过。共 9 个测试，0 失败，0 错误。

覆盖内容：

- 接口业务逻辑：考评员草稿创建、提交审核、主管审核通过、主管驳回校验、删除保护。
- 数据约束：考评员证号、考场备案号、设备资产编号重复时阻断。
- 数据库迁移：信息管理表、既有主数据补列、审核记录表、菜单和权限码种子数据。
- 业务状态：`draft`、`pending`、`approved`、`rejected`、`disabled` 的核心流转。
- 异常场景：材料说明不足、驳回原因不足、已入库/已引用数据删除。
- 数据展示：手机号脱敏值生成。
- 可引用数据：仅返回已入库、可用、未过期的考评员。

## 2. 前端模块测试

执行命令：

```powershell
npm --prefix apps/admin-web run build
```

结果：通过。TypeScript 编译和 Vite 生产构建均成功。

说明：构建输出存在 Vite chunk 体积提示，这是当前应用整体包体积提醒，不影响本模块构建通过。

覆盖内容：

- 页面加载：三张页面完成 TypeScript 编译并纳入应用入口。
- 表单输入：新增/编辑弹窗包含必填字段、日期、数字、下拉和多行文本。
- 按钮操作：新增、编辑、查看、提交审核、审核、停用、删除、导出入口已配置。
- 数据展示：表格分页、关键词搜索、审核状态筛选、运行状态筛选已接入。
- 权限菜单：侧栏优先使用 `/api/auth/menus` 后端动态菜单；按钮使用现有 `PermissionGate`，权限码与后端一致。
- 接口联调准备：所有页面通过现有 `apiClient` 调用 `/api/certification/info/*` 接口。

## 3. 前后端联调检查

已完成静态和构建层联调检查：

- 前端 API 路径与后端 Controller 路径一致。
- 前端请求字段与后端 DTO 字段一致。
- 后端返回 VO 字段与前端类型字段一致。
- 鉴权入口使用 `@PreAuthorize`，前端按钮使用权限码展示控制。
- 菜单和权限由 Flyway 写入后端数据库，前端通过 `/api/auth/menus` 动态加载菜单，并注册页面组件用于路由渲染。

未执行真实浏览器登录后的端到端录制，原因：本轮未启动 MySQL/Flyway 后端运行环境。当前验证以单元测试、迁移静态测试、后端编译和前端生产构建为准。

## 4. 测试用例类型对齐

| 类型 | 覆盖情况 |
|---|---|
| 正常用例 | 创建草稿、提交审核、审核通过 |
| 异常用例 | 材料说明不足、驳回原因不足、删除已入库数据 |
| 边界用例 | 可引用列表排除过期考评员 |
| 权限用例 | Controller 使用 `@PreAuthorize`，权限码种子数据已测试存在 |
| 数据用例 | 三类唯一业务键重复阻断 |
| 安全用例 | 统一 `apiClient` 自动携带 Token 和租户头；后端权限最终裁决 |

## 5. 通过标准

- 核心功能测试通过。
- 后端返回结构沿用项目统一 `ApiResponse` 和 `PageResponse`。
- 数据库迁移包含租户字段、审计字段、逻辑删除字段和唯一索引。
- 权限控制入口有效。
- 前端构建通过，页面纳入应用入口。
- 主要业务流程已由服务层测试覆盖。
