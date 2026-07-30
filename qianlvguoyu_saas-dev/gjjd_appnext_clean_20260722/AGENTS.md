# AGENTS.md — 工匠基地平台 (gjjd_appnext)

面向 ZCode agent 的工作区说明。仅记录从仓库中不易一眼看出的项目级事实。

## 项目概览

uni-app (Vue 3) 跨端 App，主要面向 Android/iOS 原生 App（HBuilderX 5+App），并配置了微信/支付宝/百度/头条小程序与 H5。业务为"工匠基地"竞赛/培训/团队/积分商城平台。
- AppID：`__UNI__06C03D0`，版本 `3.3.8`（`manifest.json` 与 `utils/util.js` 的 `g.ver` 必须同步）。
- Vue 版本由 `manifest.json` 的 `vueVersion: "3"` 决定；`main.js` 用条件编译 `#ifdef VUE3` / `#ifndef VUE3` 同时保留两套入口，**当前生效的是 VUE3 分支**。

## 构建与运行

- **没有 npm 构建链**：`package.json` 只声明 `pinia` 一个依赖，无 `scripts`、无 eslint/prettier/babel/vite/webpack/tsconfig/test 配置。不要假设可以 `npm run build/lint/test`。
- 本项目由 **HBuilderX** 管理：运行/发行通过 HBuilderX 的"运行 → 运行到手机或模拟器/浏览器/小程序"或"发行 → 原生App-云打包/本地打包"菜单完成，而非命令行。
- `node_modules/` 实际只含 pinia；不要期待其它 npm 包可用。
- 平台原生能力依赖 `plus.*` API，必须包在 `#ifdef APP-PLUS` 条件编译内，否则小程序/H5 端会报错。

## 目录结构

```
App.vue          入口：Pinia 装配、全局属性、WebSocket 心跳/踢人/设备绑定
main.js          createApp（VUE3 分支生效）
manifest.json    uni-app 应用配置（权限、图标、各端设置）
pages.json       页面路由 + tabBar
pages/           页面模块：auth competition exam index plan promote public registration shop task team user
api/             接口模块（competition.js / promote.js），index.js 统一 re-export
store/           Pinia：user.js / competition.js，index.js 统一 re-export；index.js.bak 是旧 Vuex 备份，勿用
utils/           见下"两套 request"
styles/          全局 CSS：colors / layout / typography / utilities（在 App.vue 全局 @import）
static/          iconfont / images / tabbar
uni_modules/     uni-app 插件市场组件（qiun-data-charts、uni-popup、uni-icons、uni-calendar 等）
docs/            设计文档（注意：被 .gitignore 排除，本地存在但不入库）
```

## 两套后端与两套 request（关键）

`utils/util.js` 导出全局配置对象 `g`，含 `runMode`（`'dev'`/`'pro'`）与各端域名：

| 后端 | dev | pro | request 实现 | 用法 |
|------|-----|-----|--------------|------|
| PHP 主后端 | `http://192.168.1.110:8080` | `https://gjjdp.gjjd168.com` | `utils/index.js` 的 `request` | 旧业务、`/appauth/*`、`/apphome/*`、`/apppromote/*` |
| Java 竞赛后端 | `http://192.168.1.110:8081` | `https://java.app.gjjd168.com` | `utils/java.js` 的 `request` | `api/competition.js` 所有 `/appcompetition/*` |

- 切换环境只改 `utils/util.js` 的 `g.runMode`，**不要**在接口里硬编码域名。
- `utils/index.js` 与 `utils/java.js` 是**近重复**文件（uploads/chooseFile/navTo 等都复制了一份）。新增竞赛接口走 `@/utils/java.js`，其余走 `@/utils`（即 `utils/index.js`）。
- 切换 `runMode` 后 dev 指向局域网 `192.168.1.110`，需在对应内网才能联调。

## 鉴权与设备绑定

- 登录态：token 存于 `uni.getStorageSync('itoken')`，请求时以 `&istate=<token>` 拼到 URL；`userId`、`clientId` 同样存 storage。Pinia `useUserStore` 镜像这些值。
- WebSocket（`App.vue`）：连接 `g.wssUrl()`，服务端下发 `client_id` → 调 `/appauth/bind` 绑定连接；收到 `type:'logout'` 即踢下线。心跳 15s。
- **`clientId`（连接级，临时）≠ `deviceId`（设备级，持久）**。`deviceId` 由 `utils/device.js` 的 `getDeviceId()`/`getDeviceInfo()` 提供，App 端用 `plus.device.uuid`，小程序/H5 退化为 storage UUID。登录时随 `/appauth/login` 上送。
- 设备绑定/换绑/灰度白名单方案见 `docs/设备绑定功能方案.md`（改登录、`utils/index.js` 的 `code=2` 拦截前必读）。

## 响应码约定（注意不一致）

- 后端统一返回 `{code, msg, data}`。
- **Java 竞赛接口**：`code === 1` 为成功（见 `store/competition.js` 与 `utils/response.js` 的 `RESPONSE_CODE.SUCCESS = 1`，提供 `isSuccess/isError/getData/showError/showSuccess`）。
- **旧 PHP 登录接口**（设备绑定方案文档）：`code === 0` 成功，`code === 2` 设备不匹配需弹窗联系管理员。两套成功码不同，处理响应前先确认是哪条链路。

## 导航约定

- 跳转统一用 `@/utils` 导出的 `navTo`（压栈）/ `rectTo`（替换）/ `showMsg`（Toast 后跳转），不要直接散用 `uni.navigateTo`。
- **tabBar 页面**（`pages/index/index`、`pages/team/index`、`pages/promote/index`、`pages/shop/index`、`pages/user/index`）必须走 `uni.switchTab` 且**不能带参数**。上述辅助函数用 `indexOf` 硬编码了这份 tabBar 名单。
  - 注意：`pages/shop/index` 在辅助函数里被当作 tabBar，但 `pages.json` 的 `tabBar.list` 实际只有 4 项（发现/团队/提升/我的，**不含 shop**）。新增或调整 tabBar 页面时，需同步修改 `utils/util.js`、`utils/index.js`、`utils/java.js` 三处重复的名单，以及 `pages.json`。
- 简单参数走 `?id=`；复杂对象走 `?item=` + `encodeURIComponent(JSON.stringify(obj))`，目标页 `onLoad` 里 `JSON.parse(decodeURIComponent(options.item))` 解析。

## 编码规范

- 注释与 UI 文案为中文，沿用既有风格。
- 页面推荐 Options API + `setup()` 返回 store 的写法（见 `README.md` 与 `pages/auth/login.vue`）：`setup() { const userStore = useUserStore(); return { userStore } }`，方法中 `this.userStore.xxx`。
- Store 优先用 Pinia（`useUserStore` / `useCompetitionStore`），不要再用 `utils/user.js` 的 `getUserId()` 旧工具（README 有迁移说明）。
- 全局样式类来自 `styles/*.css`（如 `mar-B20`、`bg-f9d52b`、`color-62`），新增样式优先复用这些工具类。
- `console.log` 调试日志在代码中普遍保留，不必特意清理。
- 每个请求 URL 自动追加 `?ver=<g.ver>` 做缓存击穿；改版本号要同时改 `manifest.json` 的 `versionName`/`versionCode`。

## 改动前必读

- **`utils/index.js` / `utils\java.js` 的 `request` 函数是该项目的 HTTP 基础函数，被 50+ 个页面/组件调用。其隐式契约是"永远 resolve（成功返回 data，网络失败返回 false），不会 reject。"** 改造 `request` 时：
  - **不要新增 `Promise.reject()` 路径**。如需中断请求请返回 `false` 或 `null`。如业务需要区分错误类型，可返回 `{error:'NO_TOKEN'}` 等方式。
  - 如确需 reject，必须同步排查所有 `await request()` 调用方，确保它们有 try/catch 或能正确处理 rejection。
  - **`showLoading` / `hideLoading` 必须成对调用**。推荐用 `try {...} finally { uni.hideLoading() }` 确保 `hideLoading` 一定被执行。退出登录后 loading 无限卡死的 bug，根因就是 `await request()` 抛异常跳过了 `hideLoading()`。排查 loading 卡死时，优先检查所有 `showLoading` 之后是否有 `await` 调用可能抛异常。
- `docs/设备绑定功能方案.md` — 登录/设备绑定/灰度全流程（改 `pages/auth/login.vue`、`utils/index.js`、`utils/device.js` 前）。
- `README.md` — Pinia store 用法与从旧 `utils/user.js` 迁移的方式。
- `.gitignore` 排除了 `/docs/`、`/unpackage/`、`/.hbuilderx/`、`node_modules/`：文档本地有但不入库，不要把对 `docs/` 的依赖写进被跟踪的构建逻辑。
