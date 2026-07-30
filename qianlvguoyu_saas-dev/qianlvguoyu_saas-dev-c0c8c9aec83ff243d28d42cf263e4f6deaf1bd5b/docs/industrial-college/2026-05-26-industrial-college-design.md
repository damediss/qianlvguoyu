# 园区学院功能迁移设计

## 背景

本次功能基于旧 PHP 项目 `D:\older_php_proj\gjjd_phalcon-master\gjjd_phalcon-master\app\controllers\api\IndustrialController.php` 中的 `collegeAction` 原型能力迁移到 `qianlvguoyu_saas` Java SaaS 系统。

旧系统中园区学院仍处于原型演示阶段：接口返回模拟数据，旧 Vue 页面在前端补齐 `tag`、`fanwei`、`desc`、`train`、`rending` 等展示字段。新系统需要将其升级为正式后台维护模块，支持菜单权限、租户隔离、数据入库、列表统计、详情、新增、编辑和删除。

## 目标范围

新增一级目录菜单“工业互联网平台”，并在其下新增子菜单“园区学院”。

园区学院第一版提供以下能力：

- 分页列表，展示培训院校数据。
- 顶部统计数据：总院校数、按类型统计、最近新增数。
- 关键字筛选，按机构名称、地址、电话等常用文本字段检索。
- 类型筛选，类型值包含：本科院校、大专、技工学校、中专、职高。
- 状态筛选，支持启用和停用。
- 详情查看。
- 新增、编辑、逻辑删除。
- 所有数据按当前登录用户的 `tenant_id` 隔离。

不包含以下能力：

- Excel 导入导出。
- 批量删除。
- 文件上传或资质附件管理。
- 复杂审批流程。
- 与培训班级、考试、证书等业务建立强关联。

## 后端设计

新增后端包：

```text
services/platform-api/src/main/java/com/guoyu/platform/industrial/
  controller/
  dto/
  entity/
  mapper/
  service/
  vo/
```

后端接口统一挂在当前服务的 `/api` context path 下，业务路径保持需求指定的 `/industrial/college`。

接口列表：

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/industrial/college` | `industrial:college:view` | 分页查询园区学院 |
| GET | `/industrial/college/stats` | `industrial:college:view` | 查询统计数据 |
| GET | `/industrial/college/{id}` | `industrial:college:view` | 查询详情 |
| POST | `/industrial/college` | `industrial:college:create` | 新增园区学院 |
| PUT | `/industrial/college/{id}` | `industrial:college:update` | 编辑园区学院 |
| DELETE | `/industrial/college/{id}` | `industrial:college:delete` | 逻辑删除园区学院 |

接口统一返回 `ApiResponse<T>`，分页使用现有 `PageResponse<T>`。

写操作增加 `@AuditLog`，模块名使用 `industrial`，目标类型使用 `industrial_college`。

## 数据库设计

新增 Flyway 迁移创建 `industrial_college` 表。

字段设计：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | BIGINT | 主键，雪花 ID |
| `tenant_id` | BIGINT | 租户 ID |
| `name` | VARCHAR(128) | 机构名称 |
| `tag` | VARCHAR(32) | 类型 |
| `business_scope` | VARCHAR(512) | 经营范围，对应旧字段 `fanwei` |
| `description` | TEXT | 简介，对应旧字段 `desc` |
| `training_qualification` | VARCHAR(512) | 培训资质，对应旧字段 `train` |
| `certification_qualification` | VARCHAR(512) | 认定资质，对应旧字段 `rending` |
| `address` | VARCHAR(255) | 地址 |
| `phone` | VARCHAR(64) | 电话 |
| `status` | VARCHAR(32) | 状态，`enabled` 或 `disabled` |
| `sort_order` | INT | 排序号 |
| `created_at` | DATETIME | 创建时间 |
| `updated_at` | DATETIME | 更新时间 |
| `created_by` | BIGINT | 创建人 |
| `updated_by` | BIGINT | 更新人 |
| `deleted` | TINYINT | 逻辑删除标记 |

索引：

- `KEY idx_industrial_college_tenant_status (tenant_id, status)`
- `KEY idx_industrial_college_tenant_tag (tenant_id, tag)`
- `KEY idx_industrial_college_created_at (tenant_id, created_at)`

数据唯一性：

- 第一版不强制 `name` 唯一，避免同租户下存在同名分校或历史数据导入冲突。

## 校验规则

新增和编辑使用同一套核心校验：

- `name` 必填，最大 128 字符。
- `tag` 必填，只允许：本科院校、大专、技工学校、中专、职高。
- `phone` 可选，最大 64 字符。
- `address` 可选，最大 255 字符。
- `businessScope`、`trainingQualification`、`certificationQualification` 最大 512 字符。
- `status` 为空时默认 `enabled`。
- `sortOrder` 为空时默认 1，最小 1。

详情、编辑、删除必须按当前租户查询；查不到或已删除时返回业务异常。

## 菜单与权限

新增菜单种子数据：

- 目录：工业互联网平台
  - code: `industrial`
  - path: `/industrial`
  - permission_code: `industrial:college:view`
- 页面：园区学院
  - code: `industrial_college`
  - path: `/industrial/college`
  - component: `industrial/College`
  - permission_code: `industrial:college:view`

新增权限：

- `industrial:college:view`
- `industrial:college:create`
- `industrial:college:update`
- `industrial:college:delete`

Demo 租户的平台管理员角色默认授予上述菜单和权限。

## 前端设计

新增前端文件：

```text
apps/admin-web/src/types/industrial.ts
apps/admin-web/src/api/industrial.ts
apps/admin-web/src/pages/industrial/CollegePage.tsx
```

页面布局沿用当前 React 管理端风格：

- 页面标题区：标题“园区学院”，描述该页面用于维护工业互联网平台下的培训院校资源。
- 统计区：三个统计卡，展示总院校数、类型数量、最近新增数。
- 筛选区：关键字、类型、状态、搜索、重置。
- 表格区：展示机构名称、类型、经营范围、培训资质、认定资质、地址、电话、状态、更新时间、操作。
- 操作区：详情、编辑、删除。
- 新增和编辑使用同一个 Modal 表单。
- 详情使用 Drawer 或只读 Modal 展示完整字段。

前端请求使用现有 `apiClient` 和 `unwrap`，认证、租户 Header 和错误处理沿用全局拦截器。

路由接入现有 `AppShell` 菜单解析逻辑；如果菜单组件名到页面组件的映射表需要补充，则添加 `industrial/College` 到页面组件映射。

## 测试策略

后端按 TDD 执行：

1. 先为 `IndustrialCollegeApplicationService` 编写服务层测试，覆盖新增、分页、详情、编辑、删除、租户隔离和统计。
2. 运行测试，确认因功能缺失失败。
3. 实现实体、Mapper、Service、Controller 和迁移脚本。
4. 运行目标测试和后端编译。

前端验证：

- 新增类型和 API 封装后，执行 `npm run build`。
- 当前 React 管理端已有构建错误会阻塞新增页面验证，因此本任务实施时先修复现有构建错误，再验证园区学院页面。

最终验收命令：

```powershell
C:\Users\pc\.m2\wrapper\dists\apache-maven-3.6.3-bin\1iopthnavndlasol9gbrbg6bf2\apache-maven-3.6.3\bin\mvn.cmd -f services/platform-api/pom.xml test
npm run build
```

前端命令在 `apps/admin-web` 目录下执行。

## 实施顺序

1. 修复当前 React 管理端构建阻塞错误。
2. 编写后端服务层失败测试。
3. 新增数据库迁移和菜单权限迁移。
4. 新增 `industrial` 后端模块。
5. 新增前端类型、API 和页面。
6. 接入菜单组件映射。
7. 运行后端测试、后端编译和前端构建。
