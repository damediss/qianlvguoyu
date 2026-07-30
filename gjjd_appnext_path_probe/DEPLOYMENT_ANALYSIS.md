# gjjd_appnext 部署与前后端关系分析

## 1. 当前工程定位

- `gjjd_appnext/`：Vue 3 + uni-app 的移动端前端，目标运行形态包括 H5、App 和小程序。
- `D:\qianlvguoyu\qianlvguoyu_saas-git\services\platform-api/`：Spring Boot 平台接口服务，统一上下文路径为 `/api`，默认端口 `8080`。
- `D:\qianlvguoyu\qianlvguoyu_saas-git\services\mcp-server/`：TypeScript MCP Server，默认端口 `3001`，提供 `/mcp`、`/sse`、`/messages`，只通过 platform-api 调用业务能力，不直连数据库。

## 2. 前端结构

- `pages.json` 注册 94 条页面路由，实际 `pages/` 下有 110 个 Vue 页面文件。
- `pages/competition/` 已形成竞赛业务主模块：竞赛首页、资讯、通知、报名入口、签到、排名、PK、闯关、学习、错题、模拟考试、问答社区、成绩详情。
- `api/competition.js` 定义 42 个竞赛接口，前缀为 `/appcompetition/*`；`api/promote.js` 定义 4 个学习提升接口。
- `store/` 使用 Pinia，已有用户状态和竞赛状态；`utils/index.js` 与 `utils/java.js` 各自实现一套请求、上传、登录态处理逻辑。
- `utils/util.js` 目前把 `runMode` 固定为 `pro`，生产地址硬编码在前端：普通接口为 `https://gjjdp.gjjd168.com`，竞赛接口为 `https://java.app.gjjd168.com`。

## 3. 已完成的本地部署验证

```powershell
cd D:\qianlvguoyu\qianlvguoyu_saas-dev\gjjd_appnext
pnpm install
pnpm run build:h5
pnpm run dev:h5
```

- 开发态 H5：`http://localhost:5174`
- 发行产物：`dist/`
- 本地静态发行冒烟：`http://localhost:4174`
- `pnpm run build:h5` 已成功生成 `dist/index.html` 和静态资源，总产物约 2.5 MB。

为了让 HBuilderX 工程可以被 Vite 直接构建，新增了 `vite.config.js`、H5 构建脚本和 pnpm 配置；同时修复了 `main.js` 中 Vue2/Vue3 条件编译分支在 Vite 依赖扫描时产生的同名 import 冲突。

## 4. 前端与当前 platform-api 的接口关系

### 已对上的能力

当前 platform-api 已实现一组兼容旧 App 的接口：

- `/api/appauth/login`、`/api/appauth/checkcode`、`/api/appauth/setlogin`、`/api/appauth/logout`
- `/api/appsms/send`
- `/api/apphome/index`、`/api/apphome/tabs`、`/api/apphome/home`、`/api/apphome/search`、`/api/apphome/articleinfo`
- `/api/appuser/*`
- `/api/appregistration/*`
- `/api/appsmart/*` 智能考试接口
- `/api/appcoach/*` AI 教练接口

本机实际验证：`GET /api/actuator/health` 返回 `200`；`POST /api/apphome/index` 返回 `200`；错误账号调用 `/api/appauth/login` 能进入后端业务校验并返回“账号不存在”。

### 当前阻断点

1. 前端竞赛主链路调用 `/appcompetition/*`，当前 `qianlvguoyu_saas-git` 的 platform-api 源码中没有 `appcompetition` Controller 或路由，因此竞赛首页、报名、签到、PK、学习和成绩链路不能直接接入当前本地后端。
2. 前端验证码使用 `/captcha/index`、`/captcha/check`，当前 platform-api 提供的是 `/api/auth/captcha/generate`、`/api/auth/captcha/verify`，路径和响应模型不一致。
3. 前端登录后调用 `/appauth/bind`，申请设备解绑调用 `/appauth/deviceunbindapply`；当前 `AppAuthController` 没有这两个接口。
4. 前端登录请求使用旧式 `itoken/istate` 查询参数；platform-api 同时兼容部分旧 token 逻辑，但登录态续期、被踢下线、WebSocket bind 还没有形成可验证的完整契约。
5. MCP Server 当前只有 4 个认定管理只读工具：`certification.searchTasks`、`getTaskOverview`、`getCollectionProgress`、`getPracticalScoreProgress`，没有竞赛选手端工具。选手端不应直接依赖 MCP，MCP 应通过受控接口为 agent 提供只读查询或辅助能力。

## 5. 架构建议：竞赛选手端下一步

1. 先冻结选手端业务边界：竞赛发现、报名与材料、赛程/签到、学习备赛、在线练习、模拟考试、成绩/排名、通知、个人中心。
2. 在 platform-api 增加独立的 `appcompetition` 模块，优先按前端现有接口合同补齐只读首页、资讯、赛段、报名状态、排名和成绩查询，再补报名提交、签到、答题提交等写接口。
3. 前端建立统一 `api/appClient.js`，把 base URL、token 注入、错误码、重试和上传封装到一处；不要继续让 `utils/index.js` 与 `utils/java.js` 各自维护一套协议。
4. 把开发、测试、生产域名移到 `.env.*` 或 manifest 的 H5 配置，避免把线上地址写死在业务代码中。
5. 统一 token 合同：建议请求头使用 `Authorization: Bearer <token>`，保留 `istate` 只作为过渡兼容；登录、刷新、退出、被踢下线、设备绑定要由后端给出明确状态码和响应示例。
6. agent/MCP 与 App 解耦：App 调 platform-api；agent 调 MCP；MCP 通过 platform-api 做租户、用户、权限和审计校验，禁止 App 直接调用带服务身份的 MCP。
7. 第一阶段建议先做“接口适配层 + 竞赛首页/报名/赛程/成绩”四条可演示闭环，再扩展 PK、闯关、学习社区和 AI 辅助。

## 6. 本次为构建补齐的缺失项

Gitee 当前提交引用了 `utils/auth.js`，但该文件不在工作树和 Git 历史中。为完成 H5 构建，新增了一个保守的 token helper：只复用有效本地 token，过期时清理本地凭证，不静默持久化密码或伪造刷新流程。该文件只是部署解阻，不等同于最终生产认证方案，后续应按 platform-api 的正式 token 合同重构。

