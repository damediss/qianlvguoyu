# AI 同步学微信小程序端复刻方案

> 版本：v1.0　日期：2026-08-30
> 关联文档：《AI同步学（学员复习）实现方案》v1.6、《AI同步学统计功能设计方案》v1.3、《AI同步学功能单库化改造方案_gjjddbpro》v1.2
> 对齐基准：手机端 `gjjd_appnext` master（2026-08-30 复核，含 D7 超前学习 / D8 灰度预检 / D9 即时鼓励全部客户端能力）
> 涉及项目：`wxmini-gjjd`（微信小程序，本期主体）；后端 `qianlvguoyu_saas` **零改动**；手机端 `gjjd_appnext` **零改动**

## 0. 需求方确认记录（2026-08-30）

| # | 决策点 | 结论 |
|---|---|---|
| Q1 | 灰度预检（availability 入口显隐） | **做**——手机端最新代码已实现，小程序对齐 |
| Q2 | D7 超前学习「继续挑战」入口 | **做**——手机端最新代码已实现，小程序对齐 |
| Q3 | 复刻范围 | 以手机端最新实现为唯一基准：10 个接口全覆盖、4 个页面 + 入口 1:1 复刻，视觉与交互不改版 |

## 1. 背景与目标

AI 同步学（学员复习）已在 SaaS 后端 + 手机端 uni-app 完整落地（三阶段复习计划、错题三级淘汰、中断补救、仿真模考、学情总览、超前学习、灰度发布、科学化即时鼓励）。培训机构学员群体大量使用微信小程序，需要将同一功能复刻到小程序端 `wxmini-gjjd`（原生微信小程序），使学员在 App 与小程序两端获得一致的 AI 同步学体验。

**关键前提（已核实）**：

- 后端 `/api/appreviewsync/**` 10 个接口全部就绪，本期**后端零改动**；
- 小程序登录签发的 `itoken`（`wxmini/login`、`wxmini/openidauto`）与 App 同源（PHP `platform_user_token`），SaaS 侧 Bearer 透传鉴权**直接可用**；
- `TenantContextFilter` 对 `/api/appreviewsync/**` 强制租户 10159（单库化 Q7 第一轨），小程序端 header 取值不再构成风险；
- 同一账号（同一 platform_user）在两端共享同一份复习计划与作答进度，天然互通。

## 2. 对齐基准：手机端已实现功能清单（2026-08-30 代码复核）

| 模块 | 手机端文件 | 功能要点 |
|---|---|---|
| 入口 | `pages/promote/exam.vue` | 「同步练习」菜单（紫色 #7c6cf0），仅复习任务（result_type=1）**且灰度预检通过**时显示；登录守卫 |
| 灰度预检 D8 | 同上 `checkSyncAllowed()` | onLoad 时调 `/appreviewsync/availability`，`allowed=true` 才渲染入口；请求失败保持隐藏（**fail-closed**）；未登录（无 token）请求 401 → 同样隐藏 |
| 概览页 | `pages/exam/syncHome.vue` | 中断补救提示条、阶段进度（阶段徽标+三段进度条+考试日期）、鼓励语卡、今日任务（新题/错题清算/累计正确率）、错题牌级统计 chips、三个按钮（开始今日练习/进入仿真模考/学情总览） |
| 答题页 | `pages/exam/syncExam.vue` | 上下物理隔离两区块（今日新题蓝色区 / 错题清算红色区）；单题即时判分（单选/判断点选即提交、多选点选+「确认答案」）；多选未确认点「下一题」自动提交并停留 2s 展示结果；续答定位；做完新题才允许清算错题（顺序规则）；答错新题实时入清算队列（静默刷新保留本地状态）；错题卡文案（含 starNew 加星提示）；D9 即时鼓励条（顶部横幅 4s，连对/连错）；结束练习 summary 弹窗；D7「继续挑战」（canAdvance 时弹窗加「继续挑战」按钮，调 advance-day 原地续答明日任务）；promptFinish 防重入 |
| 模考页 | `pages/exam/syncMock.vue` | 组卷（限时 X 分钟展示、不强控倒计时）、单/多/判作答、交卷前未答题数确认、成绩卡（得分/及格线/答对数/是否通过）、逐题解析（错题正确答案+解析） |
| 学情页 | `pages/exam/syncProgress.vue` | 预估得分卡（按考试权重加权，null 隐藏）、考核项目掌握度条形、题型正确率+错题本、每日练习趋势、知识点覆盖表、空态 |
| 接口封装 | `api/saas-review.js` | 10 个接口：overview / today / question / answer / mock/start / mock/submit / finish / progress / advance-day / availability |
| 请求通道 | `utils/saasRequest.js` | `Authorization: Bearer <itoken>` + `X-Tenant-Id`（默认 10159，存量 '1' 迁移）；401 退避重试 ×3 + silentLogin + 重试 1 次；安全红线：token 只走 header、日志不打 header |
| 响应工具 | `utils/saasResponse.js` | `isSaasSuccess`（code==='0'/0/'200' 兼容）、`getSaasMsg`（message/msg）、`getSaasData`、`showSaasError/Success` |

## 3. 后端契约确认（零改动）

- 基址：`https://java.app.gjjd168.com/api`（App 生产同款，Nginx 反代 platform-api）；
- 路由：`POST /api/appreviewsync/{overview,today,question,answer,mock/start,mock/submit,finish,progress,advance-day,availability}`，请求体 JSON；
- 响应：`{code:0, msg:"成功", data:{...}}`（`LegacyAppResponse`，code 为 int，字段名 `msg`）；业务异常 `{code:1, msg}`；未登录由 EntryPoint 返 HTTP 401；
- 鉴权：`Authorization: Bearer <itoken>`（查主库 `platform_user_token`）；租户由 `TenantContextFilter` 对该前缀强制 10159，header 发 1/10159 均可；
- **ID 精度红线（2026-08-31 修订补充，已核实后端源码）**：`planId`/`dayId`/`mockPaperId` 为雪花 id，**以字符串下发**（超 JS Number 安全整数）。客户端一律按字符串原样保存与回传，**禁止 `Number()`/`parseInt` 转换**（初始占位值 `0` 除外，拿到真实值后不再参与数值运算）；
- **401 响应体形态（2026-08-31 修订补充）**：未认证由 `RestAuthenticationEntryPoint` 返 HTTP 401，body 为 `ApiResponse` 风格 `{"code":"401","message":"Authentication required"}`（code 为**字符串**、字段名 **message**），与业务体 `{code:int, msg}` 不同——**401 判定必须以 HTTP statusCode 为准，不能看 body.code**；另外 Controller 兜底 `requireUserId()` 会以 HTTP 200 + `{code:1, msg:"未登录或登录已过期"}` 返回，按业务失败处理即可；
- 关键响应字段（客户端消费面，与手机端逐字段一致）：
  - `overview`：planId/stage/stageName/dayIndex/totalDays/examDate/encourageText、today{dayId,newTarget,reviewTarget,newDone,reviewDone,mock{mockPaperId,questionCount,status}}、progress{accuracy,...}、wrongStats{red,yellow,green,star}、interrupt{strategyText}；
  - `today`：dayId、newQuestions[]/wrongQuestions[]{questionId,type,knowledgeId,done,cardLevel,star,wrongCount}；
  - `question`：list[]{questionId,type,content,options[]{content}}（已剥离答案与解析）；
  - `answer`：correct/correctAnswer/correctOptions/resolve/cardChange{from,to,star,starNew,explainAvailable}/streak{type,count}/encourageText；
  - `mock/start`：mockPaperId/timeLimitMin/questions[]{questionId,type}；`mock/submit`：score/passScore/passed/correctCount/wrongList[]{questionId,correctAnswer,resolve}/mustReadResolve；
  - `finish`：summary{newDone,reviewDone,accuracy,useTimeMin}/encourageText/**canAdvance**；
  - `advance-day`：todayTask 视图 + advanced:true；`availability`：allowed:bool；`progress`：estimatedScore/itemMastery/typeAccuracy/wrongStats/dailyTrend/knowledgeCoverage。

## 4. 小程序端现状与技术差异映射

### 4.1 小程序端现状（wxmini-gjjd，原生微信小程序）

- 页面为 js/json/wxml/wxss 四件套，`app.json` 注册；模块体系为 **CommonJS**（`require` / `module.exports`），页面经 `getApp()` 取 `util`/`global`；
- 既有 PHP 请求通道 `util.post(url, data)`（itoken 走 URL query `istate`，域名 `gjjdp.gjjd168.com`）——与本功能无关，**不动**；
- 登录体系：微信静默登录（`wxmini/auth` + `wxmini/openidauto`）与账号登录（`wxmini/login`）均落 `itoken` 至 storage；WS `type:'logout'` 被踢时清除 itoken 并跳首页；
- 富文本渲染：既有 `utils/wxParse`（`sortExam` 等页面在用）：`WxParse.wxParse(bindName,'html',content,this,5)` + `<import src="...wxParse">` + `<template is="wxParse" data="{{wxParseData:xxx.nodes}}"/>`；
- **从未引用过 SaaS 域名 `java.app.gjjd168.com`**——微信小程序后台 request 合法域名需新增（见 §7 运维前置）。

### 4.2 uni-app → 小程序技术差异映射（复刻适配决策）

| # | uni-app 实现 | 小程序适配 | 理由 |
|---|---|---|---|
| A1 | 模块 ES import/export | CommonJS require/module.exports | 项目既有约定 |
| A2 | 传参 `{item: JSON}`（decodeURIComponent 解析） | 扁平 query（`exam_id=123`），`onLoad(options)` 直取 | 项目既有约定（promote/exam 等同款） |
| A3 | `uv-parse` 组件渲染题干 HTML | 既有 `wxParse`，**仅解析当前题**（切题/刷新时重新 parse），非 HTML 题干仍走纯文本绑定 | 全量解析每题浪费性能；与 sortExam 既有模式一致 |
| A4 | 对象直接突变（Vue 响应式） | 更新数据源后 `setData`；高频路径用**精准路径 setData**（如 `newList[3].result`、`newQ` 快照），避免整列表重刷 | 小程序性能约束 |
| A5 | 401 退避重试 300ms×3（覆盖 PHP→SaaS token 3s 同步窗口） | **不做退避**：401 且有 token → 微信静默登录（`util.wxLogin`，Promise 包装）刷新 itoken → 重试 1 次 → 仍 401 原样返回交页面提示 | 单库化后 token 直查主库，同步窗口已消失，无需退避；行为更简单确定 |
| A6 | `X-Tenant-Id` storage 化（10159 默认 + 存量迁移） | **常量 10159**（无 storage 读写） | 小程序无企业调研等其它 SaaS 链路，无存量设备问题；服务端另有强制覆盖兜底 |
| A7 | `uni.showModal/showToast/showLoading` | `wx.showModal/wx.showToast/wx.showLoading`（`util.showToast` 已有封装） | API 平移 |
| A8 | flex `gap` 布局 | 改用 `margin` 实现同等间距 | 规避旧基础库 flex gap 兼容风险 |
| A9 | 图标 `gjjd-iconfont` | 直接复用（小程序 `promote/exam.wxml` 同款 class 已在用） | 同一套图标字体 |
| A10 | 登录守卫 `isLogin() + checkLogin()` | `util.isLogin()` 判定 + `wx.navigateTo` 登录页（小程序 util 无 checkLogin） | 等价行为 |

## 5. 总体设计与文件清单

```
微信小程序 wxmini-gjjd
  ├─ pages/promote/exam        【改】+「同步练习」菜单（result_type && syncAllowed）+ checkSyncAllowed 预检 + toSyncHome
  ├─ pages/exam/syncHome       【新】计划概览（onShow 拉取，返回刷新）
  ├─ pages/exam/syncExam       【新】今日练习（新题/错题双区、判分、D9 鼓励条、D7 继续挑战）
  ├─ pages/exam/syncMock       【新】仿真模考（作答 → 交卷 → 成绩+逐题解析）
  ├─ pages/exam/syncProgress   【新】学情总览
  ├─ utils/saasRequest.js      【新】SaaS 请求通道（Bearer itoken + X-Tenant-Id:10159 + 401 静默自愈）
  ├─ utils/saasResponse.js     【新】isSaasSuccess/getSaasMsg/getSaasData/showSaasError/showSaasSuccess
  ├─ utils/saas-review.js      【新】10 个接口封装
  └─ app.json                  【改】注册 4 个新页面
```

- 页面标题（.json）：同步练习 / 同步练习 / 仿真模考 / 学情总览（对齐手机端 pages.json）；
- 页面导航：入口 → syncHome → syncExam/syncMock/syncProgress 均为 `wx.navigateTo` 压栈，完成/返回即 `wx.navigateBack`，与手机端栈行为一致；
- 与 PHP 既有链路（顺序练习/随机练习/模拟考试/我的错题）**零交叉**：不改动 `util.post`、不动既有页面逻辑（promote/exam 仅增量）。

## 6. 详细设计

### 6.1 SaaS 请求通道 `utils/saasRequest.js`

```
saasRequest({ url, data, method='POST', timeout=15000 }) → Promise
```

- 基址：文件内定义 `SAAS_BASE = { dev: 'http://192.168.1.110:8080/api', pro: 'https://java.app.gjjd168.com/api' }`（与 App 的 `g.saas` 配置对齐——**2026-08-31 修订：原方案误写 192.168.1.98，已核实 App `utils/util.js` 实为 192.168.1.110**）；选择键为文件内常量 `SAAS_MODE`（默认 `'pro'`，对齐小程序 `util.runMode`；联调时改 `'dev'`）。**注意 App 当前 `saasMode:'dev'` 指向局域网，小程序与其解耦、默认直连生产**；
- header：`Content-Type: application/json` + `X-Tenant-Id: '10159'`（常量）+ 有 itoken 时 `Authorization: Bearer <itoken>`；每次调用实时读 storage（与 App 同策略，自愈后拿新 token）；
- POST body：对象直接交给 `wx.request`（header 为 json 时自动序列化）；
- **契约对齐 App**：永远 resolve 不 reject；网络失败 toast「网络请求失败，请检查网络」并 `resolve(false)`（调用方按 falsy 处理）；4xx/5xx 也进 success 分支按 **HTTP statusCode** 判定（body 可能是 ApiResponse 风格，见 §3）；
- **401/403 处理链**（简化自 App，理由见 A5）：
  1. 无 itoken（匿名）→ 原样返回响应体（入口预检依赖此行为实现 fail-closed，不跳登录）；
  2. 有 itoken → 调**本文件内实现的 `silentRefreshToken()`** 静默刷新 → 成功且 storage 已拿到新 itoken 才用新 token 重试原请求 1 次；openid 未绑定用户（openidauto 无 token）则不重试；
  3. 重试后仍 401/403 → 原样返回响应体（页面 showError 展示 msg/message）；被踢下线场景由 app.js 既有 WS logout 逻辑处理（itoken 已清，走分支 1）；
  4. 并发守卫：多个并发请求同时 401 时共享同一个刷新 Promise，不重复触发登录流程；
- **`silentRefreshToken()` 不在 util.wxLogin 上包装（2026-08-31 修订，原方案不可行）**：已核实 `util.wxLogin(succ)` 中 `succ(true)` 在 `wxmini/openidauto` 的异步回调**之前**执行，且 openidauto 未返回 token 时同样 `succ(true)`——Promise 包装后会在新 token 尚未落 storage 时就重试，必然仍 401。故在 saasRequest.js 内自行实现全程 await 的静默登录链：`wx.login` 取 code → `util.post('wxmini/auth', {pid:0, v_id:0, code})` 取 openid 落 storage → `util.post('wxmini/openidauto', {openid})` 取 token 落 storage，任一环失败/无 token 返回 false；
- 安全红线对齐：console 仅打 method+url，绝不打 header/token。

### 6.2 响应工具与接口封装

- `utils/saasResponse.js`：`isSaasSuccess(res)`（`code===0||'0'||'200'`）、`getSaasMsg`（`message||msg`）、`getSaasData(res, def)`、`showSaasError(res, defMsg)`（wx.showToast icon:none）、`showSaasSuccess`；
- `utils/saas-review.js`：10 个函数与手机端逐一对应（同名语义）：`getReviewSyncOverview(examId)`、`getReviewSyncToday(examId)`、`getReviewSyncQuestions(examId, questionIds)`、`submitReviewSyncAnswer(examId, dayId, questionId, roundType, userAnswer, useTimeSec)`、`startReviewSyncMock(examId)`、`submitReviewSyncMock(examId, mockPaperId, answers, totalUseTimeSec)`、`finishReviewSyncDay(examId, dayId, useTimeSec)`、`getReviewSyncProgress(examId)`、`advanceReviewSyncDay(examId)`、`getReviewSyncAvailability(examId)`。

### 6.3 入口改造 `pages/promote/exam`

- **wxml**：在「去考试」区块之后、「顺序练习」之前插入菜单（与手机端位置一致）：

```xml
<view class="nav-item bg-sync color-ff mar-B10" bindtap="toSyncHome"
      wx:if="{{result_type && syncAllowed}}">
  <view class="mar-L10 mar-R10"><i class="gjjd-iconfont icon-renwu"></i></view>
  <view class="w-full">同步练习</view>
  <view class="mar-R10"><i class="gjjd-iconfont icon-xiangyou"></i></view>
</view>
```

- **js**：data 增加 `syncAllowed: false`（默认隐藏）；onLoad 中 `exam_id` 存在时在 `getData()` 旁调用 `checkSyncAllowed()`；`checkSyncAllowed()`：`result_type` 为假直接 return，否则调 availability，成功且 `data.allowed` 为真才 `setData({syncAllowed:true})`——请求失败/401 保持隐藏（**fail-closed**，未登录学员入口不出现，与手机端一致）；
- **js**：`toSyncHome()`：`util.isLogin()` 为假 → `wx.navigateTo` 登录页（`/pages/auth/login?from=/pages/exam/syncHome&params=` + `JSON.stringify({exam_id})`，与 util.navTo 内部跳登录的 URL 格式一致）并 return；否则 `util.navTo("/pages/exam/syncHome", false, {exam_id})`。**不能用 `util.navTo(path, true, param)` 做守卫**（2026-08-31 修订补充）：已核实该函数未登录跳登录页后**不 return**，仍会继续 navigateTo 目标页（既有缺陷），必须手动守卫；
- **checkSyncAllowed 调用时机**：onLoad 的 setData 回调中 `exam_id` 存在即调（不依赖 paper_id，与 getData 的 `paper_id && exam_id` 条件解耦）（2026-08-31 修订补充）；
- **wxss**：`.bg-sync{background-color:#7c6cf0;}`。

### 6.4 syncHome 计划概览

1:1 复刻手机端布局与配色（中断提示条 / 计划卡（阶段徽标+三段 stage-bar+考试日期）/ 鼓励语渐变卡 / 今日任务三列 / 错题牌级 chips / 主按钮「开始今日练习」/ 次按钮「进入仿真模考」（today.mock 存在时）/ 描边按钮「学情总览」）：

- onLoad 接收 `exam_id`；**onShow** 拉取 overview（从答题/模考/学情页返回时自动刷新，对齐手机端）；
- `toExam/toMock/toProgress` → navTo 携带 exam_id；
- 空数据防御：`overview.today`、`overview.wrongStats`、`overview.progress` 等按手机端同条件渲染；失败 toast `msg`。

### 6.5 syncExam 今日练习（核心页面）

**结构**（wxml）：进度头（第 N/M 题 + 完成后显示「结束今日练习」链接）→ D9 鼓励横幅（fixed 顶部）→ 区块一「今日新题」（蓝色标题条）→ 区块二「错题清算」（红色标题条，点击=tapWrongSection）→ 上一题/下一题 → 空态。

- **渲染模型（A3/A4 适配）**：两区块标题条按列表非空常显；题目卡片仅渲染**当前题**（`currentSection==='new'` 时新题区渲染 `newQ`，否则错题区渲染 `wrongQ`）——与手机端 `v-for + v-show` 的视觉效果等价、DOM 更轻；`newQ/wrongQ` 为按 `currentIndex` 取出的**视图模型快照**（2026-08-31 修订补充：小程序 wxml 不能调用函数，`optClass/typeText/typeTagClass/cardText/hasHtml` 的判定结果须预计算进快照——`optionsView[]{content,cls}`、`typeText`、`typeTagCls`、`cardChangeText`、`hasHtml`），切题/提交/刷新后经统一 `syncView()` 随列表一并 setData；题干 HTML 用 wxParse 单绑定 `parsedContent`（仅 parse 当前题，按 questionId 去重避免重复 parse），纯文本题干走 `{{currentQText}}` 绑定（hasHtml 判定同手机端正则）；
- **数据模型**：`dayId/newList/wrongList/currentSection/currentIndex/encourage{banner,text,type}` 等与手机端逐字段对应；**定时器与内部标志不进 data（2026-08-31 修订）**：`encourageTimer` 挂页面实例 `this.`（定时器 id 进 data 会被 setData 序列化，无意义且易误用）；`startTime/autoSubmitting/finishPromptShowing` 为不渲染的内部状态，挂 `this.` 或留 data 但不 setData（直接 `this.data.xxx` 赋值，与项目既有风格一致）；`dayId` 为字符串（§3 红线），仅原样回传；
- **作答交互**：单选/判断点选即提交；多选点选切换选中、「确认答案」提交；`userAnswer`：多选 `JSON.stringify(selected)`、其余取首个选项 content；`useTimeSec = max(1, round((now-startTime)/1000))`，提交后重置 startTime；错题区 roundType 传 `review`；
- **选项高亮**：选中 opt-selected；判分后按 `correctOptions` 数组精确匹配正确项（无该字段回退 `correctAnswer.split('、')`）、错选标 opt-wrong——逻辑与手机端 optClass 一致；
- **多选自动提交**：已选未确认点「下一题」→ 提交 → 停留 2s 展示判分 → 再跳（提交失败留原地；停留期间位置被改/全部完成则不跳）；「上一题」不触发；
- **顺序规则**：新题未清完不允许进清算区（提示文案一致）；tapWrongSection 同规则；
- **续答定位 locateResume**、**afterFirstDone 自动引导**（新题清完→进清算区→全清完→promptFinish）与手机端一致；promptFinish 加防重入（finishPromptShowing）；
- **refreshWrongList**：新题答错且 cardChange 非空时静默刷新清算队列——拉 today，按 questionId 保留本地 done/selected/result/detail，清算区答题中按 questionId 重定位当前题；随后增量 loadDetails；
- **loadDetails**：仅拉详情缺失的题，50 题/批循环，失败批次跳过（不阻塞）；
- **D9 即时鼓励条**：answer 响应含 `streak` 且 `encourageText` 时，顶部 fixed 横幅滑入展示 4s 自动消失（correct/wrong 双配色渐变 + slide 动画，wxss animation 实现）；展示期间 firstPass 引导与 refreshWrongList 延迟 4s 执行（followUp 机制，防止鼓励条被 toast/modal 顶掉，对齐手机端实测缺陷修复）；onUnload 清理 encourageTimer；
- **错题卡文案 cardText**：新入册（from 空）「已加入X错题本」，`starNew==1` 追加「，加星标记（顽固盲区）」；迁移动画「红牌 → 黄牌」等；对齐手机端（注意用 **starNew** 而非 star）；
- **结束练习 doFinish**：调 finish → summary 弹窗「新题 X 道，复盘 X 道，正确率 X%」+ encourageText；响应 `canAdvance` 为真 → 弹窗改为「继续挑战 / 结束返回」双按钮（content 追加「学有余力？可以提前做明天的任务」）；
- **D7 doAdvance**：调 advance-day → toast「已解锁明天的任务，继续加油！」→ 重置 currentSection='new'、currentIndex=0 → 重新 loadToday 原地续答；
- **空态**：加载完成且总题数 0 → 展示 emptyText（默认「今日任务已完成，明天再来吧」；加载失败展示后端 msg）。

### 6.6 syncMock 仿真模考

- 答题态：进度头（第 N/M 题 + 限时 X 分钟，不强控倒计时）、当前题卡（题型文案 + 题干 wxParse + 选项）、上一题/下一题/末题「交卷」；多选点选切换、单选点选即选中（**不自动提交**，交卷统一判分，对齐手机端）；
- 交卷：未答题数 > 0 → showModal「还有 X 道题未作答，确定交卷吗？」；answers 数组 `{questionId, userAnswer, useTimeSec:0}`；`totalUseTimeSec` 页面级计时；
- 结果态：成绩卡（通过绿渐变/未过红渐变、得分、及格线 passScore、答对 correctCount、通过/未过文案）、逐题解析列表（wrongList 的正确答案 + 解析）、「返回同步练习」按钮 navigateBack；
- 题目详情同样走 loadDetails 50/批。

### 6.7 syncProgress 学情总览

1:1 复刻手机端五块：预估得分卡（`estimatedScore != null` 才渲染，≥60 与 <60 文案区分）、考核项目掌握度（名称 + 进度条 + 百分比）、题型正确率 + 错题本双卡、每日练习趋势（日期/新+复/正确率）、知识点覆盖表（表头 + 已做/总题 + 覆盖率，coveredCount 统计 done>0）；全部数据块为空 → 空态「还没有学习数据，先去做今天的同步练习吧」。onShow 拉取。

## 7. 运维前置项（发布阻塞项）

| # | 事项 | 说明 |
|---|---|---|
| 1 | **微信小程序后台新增 request 合法域名** `https://java.app.gjjd168.com` | 小程序现仅使用 `gjjdp.gjjd168.com` / `cdn.app.gjjd168.com`；未加白则正式版所有 `/appreviewsync/**` 请求失败。开发/体验阶段可在开发者工具勾选「不校验合法域名」先行联调 |
| 2 | 灰度白名单确认 | 上线默认 `ai_review_gray_enabled=true`（仅白名单可见入口）；如需小程序端同步开放全量，运维将 `sys_dict` 改 false 即可（两端共用同一配置，无需发版） |

## 8. 测试与验收清单

### 8.1 功能用例（微信开发者工具 + 真机）

> 2026-08-31 验收：标注 ✅ 的条目已由 `_tests/` 离线单测（187 条断言全绿）+ 代码走查覆盖；标注【联调项】的需在开发者工具/真机连后端复核。

- [x] 入口显隐：白名单内复习任务显示入口 ✅；非白名单（allowed=false）/未登录（401）/availability 请求失败均隐藏（fail-closed）✅；考试任务（result_type=0）不显示 ✅（灰度白名单真实配置生效【联调项】）
- [x] 登录守卫：未登录点入口跳登录页（from/params 带回跳参数）✅
- [x] 概览页：阶段进度、鼓励语、中断提示条、今日任务计数、错题 chips、三按钮跳转 ✅；懒生成计划与中断补救策略为后端行为【联调项】；从答题页返回后计数刷新（onShow）✅
- [x] 答题页-新题：单选/判断点选即判分 ✅、正确/错误样式与解析展示 ✅、多选提示与「确认答案」✅、多选未确认点下一题自动提交停留 2s ✅
- [x] 答题页-错题清算：新题未清完点清算提示 ✅、判分后错题卡迁移文案（含 starNew 加星提示）✅；红/黄牌+★标签、已错次数、重做改答案（后端幂等重判）【联调项】
- [x] 实时入队：新题答错后「错题清算 (N/M)」实时 +1 ✅，正在清算的题不被推走（按 questionId 重定位）✅
- [x] D9 鼓励条：连对触发横幅 4s 消失 ✅、展示期间引导延后不被顶掉 ✅、onUnload 无定时器泄漏 ✅
- [x] D7 继续挑战：finish 弹窗出现「继续挑战」，确认后原地加载明日任务续答 ✅；额度用尽/计划完结时弹窗无该按钮 ✅（canAdvance 由后端额度控制【联调项】）
- [x] 结束练习：summary 数据正确 ✅、返回概览页数据刷新 ✅
- [x] 模考：组卷 ✅、限时展示 ✅、单/多/判作答 ✅、未答题交卷确认 ✅、成绩卡与及格判定 ✅、逐题解析 ✅、返回 ✅
- [x] 学情页：预估得分 ✅、掌握度条、题型正确率、趋势、知识点覆盖、空态 ✅（无权重配置时 estimatedScore=null 隐藏【联调项】）
- [x] 401 自愈：token 失效时静默刷新并重试成功 ✅（mock 全链路）；openid 未绑定时不死循环 ✅；真实过期 token【联调项】
- [ ] 两端互通：App 侧已有的计划在小程序端打开进度一致（同 platform_user）【联调项，需真机/开发者工具连后端验证】

### 8.2 回归

- [x] PHP 链路不受影响：顺序练习/随机练习/模拟考试/我的错题/去考试（promote/exam 仅增量改动：新增 data 字段 + 2 个方法 + onLoad 追加一次调用 + wxml 插入 1 个菜单块，既有逻辑零改动；开发者工具编译复核【联调项】）
- [x] WS 被踢下线：app.js 既有 logout 逻辑未改动；itoken 已清后同步练习请求走匿名分支原样返回、不触发静默登录循环 ✅（单测覆盖匿名 401 分支）

### 8.3 工程

- [x] 编译无报错：全部新增/改动 JS 通过 `node --check`，全部 JSON 通过解析校验，页面四件套齐全、wxParse import 路径有效；无 flex gap（已用 margin 替代）✅（开发者工具编译 + CSS 兼容告警复核【联调项】）
- [x] 主包体积增量 < 100KB：实测 4 页面 + 3 utils 共 56.3KB（未压缩，minify 后更小）✅
- [ ] 题干含图片/表格的富文本题在真机渲染正常（wxParse 既有能力域）【联调项】

### 8.4 自动化自测（2026-08-31 修订新增）

- [x] `_tests/` Node 离线单测（mock wx/Page/getApp/定时器）：saasResponse 判定、saasRequest 401 自愈链与并发守卫、10 接口 URL/参数/雪花 ID 字符串红线、入口 fail-closed 与登录守卫、syncExam 判分/翻页/多选自动提交/顺序规则/续答定位/实时入队/D9/D7/防重入、syncMock 交卷、syncProgress 空态——`node _tests/run.js` **187 条断言全绿** ✅
- [x] `_tests/` 已加入 `project.config.json` 的 `packOptions.ignore`，不进主包 ✅

## 9. 实施顺序与风险

**实施顺序**（每步可独立编译验证）：

1. `utils/saasRequest.js` + `utils/saasResponse.js` + `utils/saas-review.js`（通道层，可用 availability 接口先行连通验证）；
2. 入口改造（promote/exam + app.json 注册）；
3. `syncHome` → 4. `syncExam` → 5. `syncMock` → 6. `syncProgress`；
7. 开发者工具全链路冒烟（§8.1）→ 真机预览 → 交付验收。

**风险与对策**：

| 风险 | 等级 | 对策 |
|---|---|---|
| 合法域名未加白导致正式版请求全挂 | 高 | §7 前置项，发布前在 MP 后台核验；开发期勾选「不校验合法域名」 |
| wxParse 对个别富文本（嵌套表格/超长图片）渲染差异 | 中 | 题干渲染失败兜底显示纯文本；与现网 sortExam 同一渲染器，风险域一致 |
| 高频 setData 卡顿（大题量日 120 新题 + 错题） | 中 | 仅渲染当前题、详情批量 50/批、精准路径 setData（§6.5） |
| openid 未绑定用户时静默登录拿不到 token | 低 | 401 链路不循环重试；入口 fail-closed 隐藏，登录后正常 |
| 两端交互细节漂移（后续手机端迭代） | 低 | 本方案锁定对齐基准为 2026-08-30 手机端 master；后续手机端变更另行同步 |

## 10. 审核修订记录（2026-08-31）

对照三方源码（`gjjd_appnext` / `wxmini-gjjd` / `qianlvguoyu_saas`）逐条核实，修订以下缺陷：

| # | 缺陷 | 修订 |
|---|---|---|
| R1 | §6.1 dev 基址误写 `192.168.1.98`，且 App 实际用独立 `saasMode`（当前 dev）选基址而非 runMode | 更正为 `192.168.1.110`；小程序 saasRequest 内定义 `SAAS_MODE` 常量（默认 pro），与 App 解耦 |
| R2 | §6.1「`util.wxLogin` Promise 包装」不可行：`wxLogin` 的 `succ(true)` 先于 `openidauto` 异步回调执行，且无 token 也回 true | 改为 saasRequest 内自实现全程 await 的 `silentRefreshToken()`（wx.login → wxmini/auth → wxmini/openidauto），并加并发刷新守卫 |
| R3 | §3 未提雪花 ID（planId/dayId/mockPaperId）字符串下发，JS Number 转换会丢精度 | §3 增加 ID 精度红线：字符串原样保存回传，禁止数值转换 |
| R4 | §3 未写 401 响应体形态（`{"code":"401","message":...}`，与业务体不同） | §3 补充，明确 401 判定以 HTTP statusCode 为准 |
| R5 | §6.5 数据模型把 `encourageTimer` 列入 data（会被 setData 序列化） | 定时器/内部标志挂页面实例；wxml 不可调用函数，选项高亮等判定预计算进视图模型快照 |
| R6 | §6.3 登录守卫未给登录页 URL，且未警示 `util.navTo(path,true)` 未登录不 return 的既有缺陷 | 补 URL 格式与手动守卫要求；checkSyncAllowed 调用时机与 getData 条件解耦 |
| R7 | 验收清单无可执行的自动化验证手段，真机条目无法离线勾选 | §8.4 新增 `_tests/` Node 离线单测 + packOptions.ignore 忽略；真机/联调条目标注「联调项」 |
