# admin-web 前端规则

> 根目录 `AGENTS.md` 的子文件，承载前端局部规则。Codex / Claude Code 进入本目录会自动加载；其他工具请先手动阅读。
> 通用硬规则（统一响应结构、多租户、软删、Git 规范等）见根目录 `AGENTS.md`，本文件不重复。

## 技术栈与命令

- React 18 + TypeScript 5.7 + Vite 6 + antd + TanStack Query + Zustand + ECharts；**Node 20 LTS+**（18 已 EOL）
- 开发服务器：`npm run dev`（:5173）
- 构建 + 类型检查：`npm run build`（内部即 `tsc -b && vite build`）——**没有 `tsc` script，也没有通用 `test` script**，合并前类型检查走 build
- 专项测试 script 见 `package.json`（`test:competition-score` / `test:competition-routes` / `test:competition-stage-planning` / `test:e2e:stage-planning`）
- 测试栈：Vitest / React Testing Library / Playwright（**无 MSW**，不要假定存在）

## 目录结构

```
src/
  app/  routes/  layouts/  components/  services/
  stores/  hooks/  utils/  types/
  pages/
```

`pages/` 实际目录（15 个，以真实目录为准，最新清单见 `docs/agent/项目现状.md`）：
`assessor` / `certification` / `chazheng` / `competition` / `exam` / `expert` / `formalExam` / `knowledge` / `practicalCollection` / `product` / `question_bank` / `system` / `training` / `zhi_neng_ke_cheng_cai_liao_guan_li` / `zhixunplatform`

历史文档中的 `dashboard / course / question / workflow / ai` 页面目录**不存在**，不要按旧名建目录。

## HTTP 错误处理

apiClient 响应拦截器必须覆盖三种错误码：

- 401：清除 store 中 token + currentUser，让 AuthGate 展示登录表单
- 403：展示权限不足提示，不自动跳转
- 500：区分代理错误和业务错误

## 登录态管理

- AuthGate 使用 store 中 currentUser（同步）作为主判定条件，避免仅依赖异步 `/api/auth/me`
- 页面刷新后：先取 localStorage token → 同步展示 UI → 后台异步验证 `/api/auth/me`
- `/api/auth/me` 返回 401 时静默清除本地 token，展示登录表单
- 登录成功后同时写入 token、tenantId、currentUser 到 store

## 多租户

- 请求拦截器在 Authorization header 之外附加 `X-Tenant-Id` header
- tenantId 持久化到 localStorage，页面刷新后恢复

## 执行规则

1. 页面级数据请求使用 TanStack Query
2. 用户信息、菜单、主题、折叠状态使用 Zustand
3. 表格优先 ProTable，表单优先 ProForm，图表统一 ECharts
4. 菜单从后端动态加载，不在前端写死
5. HTML 原型仅作参考，不在原型文件上堆正式功能
6. 接口类型放入 `types/`（后续可用 OpenAPI 生成）
7. 需兼容局域网 HTTP 访问
8. 下载文件：`apiClient.get(url, { responseType: 'blob' })` 返回 `AxiosResponse`，blob 数据在 `.data` 里；API 函数签名应 `return (await apiClient.get(...)).data` 并声明 `Promise<Blob>`（踩坑 #10）
9. 外壳滚动模型（2026-09-14 起）：外壳锁 100vh、侧栏与内容区各自独立滚动、切菜单内容区回顶；页面内 sticky/100vh 取值按新容器口径
10. **标签页外壳（2026-10-04 起，《admin-web标签页外壳改造方案》）**，四条约定：
    - 新页面轮询必须接 `usePaneActive()`（`layouts/TabKeepAlive`）：`refetchInterval: active ? N : false`；**禁止裸写 `setInterval` 轮询**；确需 setInterval 的短定时器，隐藏时跳过 setState、不得中断进行中任务
    - 页面内跨页跳转继续用 `navStore.setSelectedKey(code)`（语义已是"激活/新建标签页"，勿自行 pushState）
    - 隐藏路由 shell（contentOverride 覆盖态）必须传真实菜单码作 `selectedMenuKeyOverride`，并在进入覆盖态时 `tabStore.openTab(code)` + `setOverridePath(code, 当前URL)`（覆盖态回退载体）
    - 新增隐藏路由必须同步登记改造方案附录 A（`src/routes/hiddenRoutes.ts`）
    - 专项测试：`npm run test:admin-tabs`（标签层单测）/ `npm run test:e2e:admin-tabs`（标签页 e2e）
