# admin-web 主窗口标签页化改造方案

- 日期：2026-10-04（**v1.4，顶栏合并与 Chrome 风格视觉修订**；v1.3 清单化、v1.2 二期前挪、v1.1 源码复核、v1.0 一版）
- 状态：**口径全部拍板（Q1-Q10）并已实施（T1~T8 代码落地，2026-10-04）**，剩余项见 §5 实施勾选与 §5.6 人工抽查
- 涉及范围：仅前端 `apps/admin-web`（外壳与导航层），后端零改动
- 关联记忆/口径：管理端外壳滚动模型（b127a42，外壳锁 100vh、内容区独立滚动）
- v1.1 修订来源：逐条核对 `src/main.tsx`、`src/layouts/AppShell.tsx`、`src/stores/navStore.ts`、`src/hooks/useUnsavedChangesBlocker.tsx` 及全站轮询点后的评审结论
- v1.2 修订来源：① 缺陷修订——**D1** §3.7 覆盖态"不进标签列表"与"激活态=override 标签"自相矛盾（改为覆盖态也 openTab 并携带 `overridePath`，回退能力随之一期化）；**D2** §3.4 pushState 不派发 popstate 后 `App()` pathname 状态滞后的说明缺失；**D3** §3.8 守卫 scope 来源依赖 D1 解决；**D4** §1.1 守卫注册方文件名修正（实测消费方为 CompetitionEventManagementPage / CompetitionEventModulePage）。② 范围重估——二期 6 项中 5 项挪入一期（Q10）：覆盖态回退、标签刷新按钮、>10 软提示、乙组剩余 6 处 setInterval、（低优先可裁剪）拖拽排序；**全站 dirty 确认不全量挪入**（一期交付框架 + 高风险页试点，全量滚动留二期）。承重断言抽样复核通过：api/client.ts 401 clearAuth、GenWorkbenchPage.tsx:50 为真轮询点、pageMap 实测 369 键/49 连字符、单槽 guardRef、赛段编排除文案硬编码。
- 实施记录（2026-10-04，T1~T8 全量落地）：新增 `src/stores/tabStore.ts`、`src/layouts/TabKeepAlive.tsx`、`src/hooks/useTabDirtyGuard.ts`、`src/routes/hiddenRoutes.ts`、`src/config/tabsFeature.ts`、`tests/adminTabsStore.test.ts`（20 条）、`tests/adminTabsUrl.test.tsx`（12 条）、`e2e/adminTabs.spec.ts`；改造 `AppShell.tsx`（Tabs/保活/URL 联动/恢复/下钻守卫/滚动存取/title/软提示/拖拽排序/关闭守卫/租户切换清空/灰度开关）、`navStore.ts`（委托 tabStore）、`main.tsx`（5 个隐藏路由 shell 覆盖态接线）、`useUnsavedChangesBlocker.tsx`（Map 注册表化+文案入参化）、`competitionRoute.ts`（findMenuCodeByPath 泛化）、`global.css`（.app-tabbar）、`AGENTS.md`（四条约定）、`package.json`（test:admin-tabs / test:e2e:admin-tabs + @dnd-kit 依赖）；轮询接入 25 处（甲组 refetchInterval 16 处 + 乙组 setInterval 9 处，含附录 B 之外实测多出的 XiMuBiao:705/GenWorkbench:66/CompetitionQuestionBank 竞赛题库导入）。`npm run build` 0 error；`npm run test:admin-tabs` 32/32 绿；全量 vitest 198 过（= 既有 166 + 新 32），失败集与改造前基线完全一致（2 条既有失败 + 并发 flaky 3 文件单跑全过 + 3 个 node-test 风格文件本就不属 vitest）——**零新增回归**。e2e/量化门禁/附录 C SQL 待环境执行（见 §5 实施勾选）。
- v1.4 修订（2026-10-06，UI 拍板 Q1A~Q4A）：见 §3.10 顶栏合并与 Chrome 风格视觉。依赖修复 `7c98d9ae`（@dnd-kit/utilities 显式声明，三 lockfile 同步）；评审修复轮 `aefe07e7`（openOverrideTab 原子入口修 D1 覆盖态 URL 改写、visitedKeys 收敛修 D3 全量挂载、openTab 同步 title/path、恢复用 visibleMenuTree、竞赛 shell menuTree 守卫，+6 回归用例 38/38）。
- v1.4 新增 §3.10 顶栏合并与 Chrome 风格视觉（Q1-Q4 owner 已拍板 2026-10-06）：
  - **Q1-A 搜索栏删除**：Header 内的搜索 Input 无任何交互逻辑（无 onSearch/onPressEnter，纯占位），连同 `.top-search` 样式一并移除。
  - **Q2-A Chrome 同款单行**：标签条与"平台管理员"下拉合并为唯一头部行（高 44px）——标签占左侧弹性宽度，下拉固定右侧（功能完整保留：平板考评端/综合答辩端/退出登录）。比旧两行（58+40）省 54px 纵向空间，内容区自动吃回。
  - **Q3-A Chrome 浅色原味**：标签栏灰底（#dee1e6 系）；**激活标签=白色卡片浮起**（顶部圆角 8px、与底部分隔断开、轻阴影），**未激活标签与栏同色、hover 提亮**（rgba 白 60%）——选中/未选中颜色区分即此。
  - **Q4-A 关闭按钮仿 Chrome**：未激活标签 hover 才显示 ×（opacity 过渡），激活标签常显 ×。
  - 默认口径（未另问）：不加"+"新建按钮（标签只能由菜单打开）；溢出沿用 antd 左右箭头；固定"管理概览"不可关；灰度 off 的旧分支头部仅保留用户下拉（右对齐）。
  - 影响面：纯布局+样式（`AppShell.tsx` Header 结构、`global.css` 标签条样式重写、`.app-tabbar` 类更名为 `.app-tabbar-strip`），不动 tabStore 任何逻辑；测试/ e2e 选择器同步更名。
- v1.3 修订来源：owner 指令——软提示阈值 >15 改为 **>10**（第 11 个标签触发；Q10 行、§3.9、§5、§6、§8、v1.2 修订行同步）；§5 测试与验收重写为**清单化结构**（5.1 单测 UT-01~20 / 5.2 组件 RTL-01~12 / 5.3 e2e E2E-01~08 / 5.4 量化门禁 / 5.5 手工回归 / 5.6 验收流程），验收时逐项勾选；§5.6 补**角色分工**（owner 指令：手工回归执行主力=实施者，owner 只抽查两类体感项）。

---

## 1. 背景与现状

### 1.1 现状架构（**已于 2026-10-04 复核源码，本表对 v1.0 的数字做了更正**）

| 组件 | 现状 |
|---|---|
| `src/layouts/AppShell.tsx` | 侧栏 antd Menu + 内容区。内容区渲染 `contentOverride \|\| pageMap[selectedKey] \|\| pageMap['knowledge']`，一次只能看一个页面，无任何"开过哪些页"的记忆 |
| `src/stores/navStore.ts` | `selectedKey`（zustand）驱动内容区，持久化 localStorage（`guoyu_admin_selected_menu`，默认 `knowledge`） |
| `src/main.tsx` | `pageMap` **369 个键**（v1.0 写"约 200+"，已更正）→ ReactNode；多个码共享同一组件（如 `train_people` / `train_people-list` → TrainingClassPeoplePage）。其中**含连字符的键 49 个**：真正为页面内跳转而设的 kebab-case 别名 **39 个**（`certification-dashboard`、`zhixun-*` 系列等），另有 `train_info-list`、`train_exam-list` 等 **10 个 `train_xxx-yyy` 形态是真实菜单码**，二者不可混为一谈 |
| 隐藏路由 | **5 个独立 shell**，各自渲染自己的 `<AppShell>`，通过 `contentOverride + selectedMenuKeyOverride` 覆盖内容区：`CompetitionEventShell`、`ReviewSyncStatsShell`、`GraduationClassDetailShell`、`CollectionManageShell`（采集管理），外加 `CompetitionModuleShell`（作为 `pageMap` 的 value 存在）。URL 直达有效 |
| **页面内跳转（三类，v1.0 只认出第一类）** | ① **经 navStore**：26 个页面文件消费 `setSelectedKey` / `navigateTo` / `navigateMenu`，实际调用点 **70+ 处**（v1.0 写"7 处、31 文件"，已更正）；其中大量是 kebab 别名（如 `zhixunplatform/ProjectArchivesPage.tsx:50-109` 一次性列出 20 个 `zhixun-*` 跳转目标）<br>② **直 pushState**：6 个文件自定义本地 `navigateTo(path)` 直接 `history.pushState` 跳隐藏路由——`exam/reviewSyncStats/StatsOverviewPage.tsx:55`、`exam/reviewSyncStats/StudentDetailPage.tsx:72`、`training/TrainExamListPage.tsx:33`、`training/ClassReviewSyncModal.tsx:33`、`training/TrainingGraduationPage.tsx:86`、`training/TrainingGraduationClassDetailPage.tsx:32`。**不经过 navStore，委托机制覆盖不到**<br>③ **容器内切换**：`training/subsidy/components/MaterialWorkspacePanel.tsx` 用本地 `setSelectedKey` 做面板切换，同名但不同源，不参与标签化 |
| 菜单树 | `MenuVO`（`src/types/api.ts:57`）每项自带 `path` 字段（目前只有竞赛模块在用），是 URL 联动的现成数据源 |
| 顶部滚动 | 切菜单时 `contentRef.scrollTo(top:0)`（内容区是独立滚动容器，浏览器不会自动复位）。`AppShell.tsx:210-212` 该 effect 依赖 `[activeMenuKey]`，即 `selectedMenuKeyOverride \|\| selectedKey` |
| **目录自动下钻（v1.0 未识别）** | `AppShell.tsx:246-254`：当 `selectedKey` 命中一个「无 `pageMap` 且非 external」的目录节点时，自动 `setSelectedKey(首个可导航子节点)`。该 effect 依赖异步到达的 `visibleMenuTree`，**与刷新恢复存在执行顺序冲突**（见 §3.5） |
| **未保存拦截（v1.0 描述有误）** | `src/hooks/useUnsavedChangesBlocker.tsx`：`NavigationBlockerProvider` 用**单个 `guardRef`** 保存唯一 guard，`registerGuard` 为覆盖写；`confirmNavigation()` 调用的是"当前挂在槽上的那一个"，与"哪个页面发起"无关。目前仅竞赛侧两个文件（`CompetitionEventManagementPage.tsx` / `CompetitionEventModulePage.tsx`，v1.2 D4 修正）经 `useUnsavedChangesBlocker` 注册，故单页场景无害；保活后多页共存会串台（见 §3.8）。另 line 52 确认文案硬编码为"赛段编排" |

### 1.2 问题

多任务并行操作（如一边跑试题生成批次一边查题库）只能靠浏览器级反复导航，切走再切回，筛选/表单/分页状态全丢。

---

## 2. 目标与已确认决策

目标体验：点菜单 = 打开一个浏览器式标签页；多页并存、随时切换、状态保留。

| 决策点 | 结论（owner 已拍板，2026-10-04） |
|---|---|
| **Q1 保活策略** | **A 全保活**：已打开的标签页组件始终挂载，切换仅隐藏（`display:none`）；配套"不可见标签暂停轮询"（§3.6） |
| **Q2 URL 联动** | **A 联动**：激活标签 → URL 变为该菜单的 `path`；刷新恢复；带 path 链接可直达开标签。**并新增约束：路径若命中隐藏路由（附录 A）则不写 URL，见 §3.4** |
| **Q3 恢复已开标签** | **A 恢复**：标签列表 + 激活位置 + 各标签滚动位置（localStorage） |
| **Q4 常驻标签** | **A**："管理概览"（`knowledge`）固定为第一个标签，不可关闭 |
| **Q5 标签栏能力** | **B**：点击切换 + 关闭按钮 + 右键菜单（关闭其他/关闭右侧/全部关闭）+ 中键关闭 |
| **Q6 关闭守卫实现（v1.1 新增）** | **改注册表**：`NavigationBlockerContext` 由单 ref 改为 `Map<scope, Guard>`，`confirmNavigation(scope)` 按 tabKey 取用；顺带把硬编码的"赛段编排"文案改为通用文案。**不完成此项则 §3.8 不上线** |
| **Q7 中键关闭事件（v1.1 新增）** | 采用 `onAuxClick`（`button===1`）判定中键，并在 `onMouseDown` 中 `preventDefault()` 抑制 Chrome 自动滚动；不使用 `onMouseDown button===1`（触控板/部分浏览器取不到） |
| **Q8 旧持久化 key（v1.1 新增）** | **不物理删除** `guoyu_admin_selected_menu`。该 key 是灰度开关 off 时的降级读源（见 §7），保留它等于保留一条零成本回滚路径 |
| **Q9 轮询暂停覆盖面（v1.2 修订）** | 两条腿：`refetchInterval` 型改 query options（甲组 15 处一期全改）；`setInterval` 型（附录 B 乙组 9 处）**v1.2 起一期全量改造**（每处仅几行门禁，机制与甲组共用；导出/上传类短定时器不可见时跳过 setState、不中断进行中任务）+ AGENTS.md 禁令保留 |
| **Q10 二期项前挪（v1.2 新增，owner 已拍板 2026-10-04）** | **挪入一期**：覆盖态回退（`overridePath`，标签化自身引入的回归缺口）、标签刷新按钮（全保活后"切走再切回"的天然刷新途径消失，属体验补洞）、>10 软提示（对冲全保活内存风险，几行）、乙组剩余 6 处 setInterval；**有条件挪入（最低优先、工期紧整项裁回）**：拖拽排序（新增 `@dnd-kit/core` + `@dnd-kit/sortable` 依赖）；**不全量挪入**：全站页面 dirty 关闭确认——一期交付 `useTabDirtyGuard` 框架 + 高风险表单页试点（≤5 个），全量接入按风险滚动留二期 |

直接采用的默认口径（未另提问）：标签栏放顶部 Header 下方横向一行（`flex: 0 0 40px`）；不设硬性数量上限（antd Tabs 自带横向滚动）；每标签记忆自己的滚动位置；登出/切换端清空标签；同名组件不同菜单码各开各的标签（状态隔离）；`document.title` 随激活标签联动。

---

## 3. 总体设计

### 3.1 布局与信息结构

```
┌──────────┬──────────────────────────────────────────────┐
│  Sider   │  Header（搜索 + 用户区，不变）                  │
│  (不变)   ├──────────────────────────────────────────────┤
│          │  TabBar：[管理概览 ✕不可关][知识归属修正 ✕][题库…✕] ← 新增，flex: 0 0 40px
│  菜单     ├──────────────────────────────────────────────┤
│  (高亮    │  Content（独立滚动容器，不变）                  │
│  联动     │  ├ 面包屑（保留现状）                          │
│  激活tab) │  └ 各标签页内容（保活面板，激活者可见）          │
└──────────┴──────────────────────────────────────────────┘
```

- 标签页三元组：`{ key: 菜单码, title: 菜单名(menuDisplayName), path: 菜单path, closable }`。
- **一个菜单码一个标签**：点已打开的菜单 → 激活既有标签（不重复开）；点新菜单 → 追加新标签并激活。
- 面包屑保留在内容区（提供层级上下文，与标签叶名互补）。
- **布局口径（v1.4 修订）**：`.app-main` 是 `height:100vh; overflow:hidden` 的 antd Layout（默认 flex column）；标签条并入头部行（`.app-header-tabs`，高 44px）——标签 `.app-tabbar-strip` 占左侧弹性宽度、用户下拉固定右侧；`.app-content` 维持 `flex: 1 1 auto; min-height: 0; overflow-y: auto`（global.css）继续吃掉剩余高度。灰度 off 时头部为 `.app-header-plain`（仅用户下拉，右对齐）。

### 3.2 数据层：新增 `tabStore`（zustand + persist）

```ts
interface TabItem { key: string; title: string; path: string; closable: boolean; overridePath?: string; /* 覆盖态标签专用，见 §3.7 */ }
interface TabState {
  tabs: TabItem[];                 // 有序，tabs[0] 固定为 knowledge
  activeKey: string;
  scrollPositions: Record<string, number>;   // 每 tab 滚动位置
  openTab(key, title, path?): void;          // 已存在则激活，否则追加+激活（必须幂等）
  activateTab(key): void;          // 激活 + URL pushState（§3.4 校验后）+ 恢复滚动（§3.3）
  closeTab(key): void;             // 关闭相邻激活位 fallback：左邻 → 右邻 → 固定 tab
  closeOthers(key) / closeRight(key) / closeAll(): void;  // 均保留固定 tab
  saveScroll(key, top): void;
  clearTabs(): void;               // 仅由「显式登出 / 切换端」调用，见下方注意点
  restoreFromStorage(menuTree): void;       // 见 §3.5 校验规则
}
```

- persist key：`guoyu_admin_tabs`（tabs + activeKey + scrollPositions）。**key 中并入 tenantId**：形如 `guoyu_admin_tabs:${tenantId}`（tenantId 取自 `authStore`，与 `guoyu_admin_tenant_id` 同源）。理由：`update: tenantId` 可在同一浏览器切换，未隔离的 tab key 会让甲租户标签被乙租户菜单树静默改写。切换租户时同步 `clearTabs()`。
- **navStore 兼容层（关键设计）**：`navStore.setSelectedKey(code)` 改为委托 `tabStore.openTab(code)`——26 个页面文件、70+ 处调用点**零改动**自动获得"激活/新建标签"语义；`navStore.selectedKey` 保留为 `activeKey` 的镜像（AppShell 及隐藏路由 shell 的高亮逻辑继续可用）。`editingTaskId` / `currentProjectId` 不动。
  - **语义变化须在评审稿中写明**：现状是"替换内容区"，改造后是"增开标签"。受影响最大的是批量跳转型页面（`zhixunplatform/ProjectArchivesPage.tsx:50-109`、`TaskArchivesPage.tsx:206-258`），其"返回上级"按钮会从"回到上一页"变成"叠一个新标签"，历史回退链靠另一侧的回退按钮承担。
  - **第 ② 类跳转（直 pushState）不受委托影响**，由 §3.4 的 popstate 排除清单兜住。
- **别名归一（改为动态规则，不建静态表）**：静态 `ALIAS_TO_MENU_CODE` 会随菜单增删腐化（现有 39 个别名与 10 个 `train_xxx-yyy` 真码混在同一份 pageMap 里，靠手工维护易错）。改为运行期判定：
  1. `menuTree` 中存在该 code → 直接使用（保证真实菜单码不被误并）；
  2. 否则取 `code.replace(/-/g, '_')`，若存在于 `menuTree` → 归一到该主菜单码；
  3. 都不存在 → 仍以原 code 开标签，title 回落 `pageMap` 键名，并在 console 打一条 warn（为后续清理名录提供线索）。
- **清退时机（重要修正）**：`clearTabs()` **不挂** `authStore.clearAuth`。`clearAuth` 有 6 个调用点，其中 `api/client.ts:67` 是 **401 响应拦截器**——一次偶发 401（并发请求、token 临界过期、网关抖动）就会清空用户全部标签，而现状 `selectedKey` 不受影响。改为：
  - 在 AppShell 的三处**显式**入口调用：`handleLogout`、`handleOpenAssessor`、`handleOpenDefense`（AppShell.tsx:256-286）；
  - 401 被动失效路径**不清 tabs**，让 §3.5 的权限校验在下次 restore 时自然剔除失效标签。

### 3.3 渲染层：AppShell 改造（保活模型）

- 用 **antd Tabs（`type="editable-card"`、`hideAdd`）** 承载标签栏：默认 `destroyInactiveTabPane=false` 天然满足 Q1 全保活（antd ^5.22.6 行为确认）；`forceRender` 不开（首次激活才挂载，控制初始成本）。
- 标签栏 items：每个 tab 的 `label` 包一层自定义节点，承载 **右键菜单**（antd Dropdown `trigger={['contextMenu']}`，包裹节点不得 `stopPropagation`，点击仍需冒泡到 tab 完成切换）与 **中键关闭**（`onAuxClick` + `onMouseDown preventDefault`，见 Q7）；关闭图标用 editable-card 自带。
- tab 面板 children = `pageMap[code]`；pageMap 同一元素描述符渲染多份 = 独立组件实例（React element 本身不可变，状态天然隔离）。
- 滚动模型不变：外壳仍锁 100vh，Content 仍是唯一独立滚动容器。**切走时保存，`激活时按下述时序恢复`**：
  ```
  1) closeTab/activateTab 切换前：scrollPositions[oldKey] = contentRef.scrollTop
  2) setActiveKey 触发渲染后，在 useLayoutEffect + 双 requestAnimationFrame 后尝试恢复：
     - 若 contentRef.scrollHeight >= pos + clientHeight → scrollTo(pos)，结束
     - 否则最多重试 3 次（每次一帧），仍不满足则放弃本次恢复
       （重要：失败时**不写回 0**，保留 scrollPositions 原值，供下次切回重试）
  3) 首次打开的标签（无记录）→ scrollTo(0)
  ```
  理由：隐藏 pane 是 `display:none`，切换瞬间尚未重排；且多数页面内容依赖请求返回才撑高，直接在 effect 里 `scrollTo` 会被 `scrollHeight` 钳到 0，Q3-A 的滚动还原会在长页面上静默失效。
- 现有"切菜单无条件回顶" effect（AppShell.tsx:210-212）删除，职责上移到上述算法。
- 菜单高亮联动：`selectedKeys=[effectiveActiveKey]`（定义见 §3.7）+ 祖先自动展开逻辑沿用现状。
- `document.title` = `${激活标签 title} - 千铝国昱云平台`；标签全部关闭（仅剩固定 tab）时同步复位。

### 3.4 URL 联动（Q2-A）

- **正向（含 v1.1 新增的排除规则）**：激活标签 → 先过隐藏路由匹配器（附录 A）：
  - **命中** → **不写 URL**，该标签退化为"仅 localStorage 身份"（与 path 为空同口径）。这是 D3 的根解：否则该菜单的 path（如 `/competition/events`）会在 **F5 刷新或被分享给同事**时，让 `App()`（main.tsx:706-751）改判到 `CompetitionEventShell` / `ReviewSyncStatsShell` / `GraduationClassDetailShell`，标签栏整条消失。
  - 未命中且 `tab.path` 非空 → `history.pushState({}, '', tab.path)`。
  - **只 pushState，不手动 dispatch popstate**：`useBrowserPathname`（main.tsx:564）只监听 popstate，不派发即可保证 `App()` 不在三种 shell 之间抖动。
  - **状态滞后说明（v1.2 D2 补充）**：pushState 不派发 popstate 意味着 `App()` 里 `useBrowserPathname` 的状态在下一次 popstate/刷新前是**陈旧的**，shell 选择随之滞后——因所有 shell 共用同一 AppShell + 标签体系，期间渲染等价、无可见差异（高亮由 effectiveActiveKey 兜底）。**实施时不要把滞后当成 bug 去"补 dispatch"**；唯一允许显式派发 popstate 的路径是 §3.7 的覆盖态回退。
- **反向（浏览器前进/后退）**：现有 `useBrowserPathname` 已监听 popstate——path 变化时：
  1. 命中附录 A 隐藏路由 → **直接交给既有 shell 逻辑，tabStore 不介入**；
  2. 否则按 path→菜单码反查（`findMenuCodeByPath`，由 `routes/competitionRoute.ts:40` 的 `findCompetitionMenuCode` 泛化：精确优先、其次最长前缀）→ 已开则激活，未开则开新标签；
  3. 找不到 → 保持现状不动（不改 hash、不开标签）。
- **直达/刷新**：进入系统时若 URL path 能解析到菜单码且未命中附录 A → 打开并激活该标签（优先于 localStorage 的 activeKey）；否则按存储的 activeKey 恢复。
- 深链可分享：把带 path 的链接发给同事 → 对方登录后直达同一页面（**排除附录 A 命中的菜单，这类链接一期不承诺直达开标签**）。

### 3.5 刷新恢复（Q3-A）与校验规则

- localStorage 恢复 tabs + activeKey + scrollPositions，**在 menuTree 异步到达后执行校验**：逐个标签按 code 在菜单树中确认仍存在且 pageMap 可渲染；被删菜单/无权限的码静默剔除；全部失效则回落固定 tab。
- **执行顺序约束（v1.1 新增，对应 D8）**：restore **必须早于** AppShell 的"目录自动下钻" effect（AppShell.tsx:246-254）产生副作用。做法是给下钻 effect 增加守卫——仅当 `selectedKey` 来自**用户点击**（`navigateToNode` 链路写入的标志位）时才重定向；restore 写入的 activeKey 不带该标志位，即便命中空目录也只做"菜单高亮"，不再自动拉起子页标签。
- 旧存储兼容：`guoyu_admin_selected_menu` 的值作为首次显示的初始 active 标签读取；**不删除该 key**（Q8 / §7 回滚依赖）。

### 3.6 不可见标签暂停轮询（Q1-A 配套）

- 风险面：保活后，生成工作台、统计页等轮询在隐藏标签里继续打接口。
- 机制：新增 `TabKeepAliveProvider` + `useIsTabActive()`（context 提供 activeKey），页面显式接入 `refetchInterval: isActive ? N : false`。
- **一期实际覆盖面（v1.1：经全站 grep 复核，更正 v1.0 的"三处"）**——完整清单见**附录 B**：
  - **甲组（改 query options 即生效，一期全改）**：15 处 `refetchInterval`。注意 v1.0 点名的 `TheoryQuestionGenPage.tsx` **不含** `refetchInterval`（line 90 是模拟进度条的 `setInterval`），真正的批次轮询在它渲染的子组件 **`workbench/GenWorkbenchPage.tsx:50`**；`SkillQuestionGenPage.tsx:385`、`KnowledgeFixPage.tsx:134` 定位正确。
  - **乙组（`setInterval` 型，改 query options 无效）**：9 处（v1.2 **全量挪入一期**——每处仅几行 `useIsTabActive` 门禁，与甲组机制共用）。其中 `TrainingDashboardPage.tsx:96` 的 1s 时钟 tick 只产生渲染无请求，同样门禁（隐藏时省 CPU）；导出/上传类短定时器（`TrainingInformationPage.tsx:590/634`、`ClassSignUpPeoplePanel.tsx:320`、`XiMuBiaoGuanLiPage.tsx:189`）门禁时**不得中断进行中的任务**——不可见期间让计时继续走但跳过 setState，可见后再同步一次。AGENTS.md 禁令保留（新页面不得裸写 `setInterval` 轮询）。
  - 丙组（竞赛/平板/正式考试模块）：不在标签化范围，`display:none` 场景不可达，一期不动。
- 其余非轮询副作用（WebSocket、定时器）集中在竞赛/考试模块，不在标签化范围；方案不引入全局定时器熔断（避免误伤）。

### 3.7 隐藏路由与并存策略（覆盖态优先级链）

- **适用范围**：5 个 shell（`CompetitionEventShell`、`ReviewSyncStatsShell`、`GraduationClassDetailShell`、`CollectionManageShell`，以及作为 pageMap value 的 `CompetitionModuleShell`）。它们各自渲染一个 `<AppShell>`。
- **覆盖态标签进列表（v1.2 D1 修正）**：v1.1"不进标签列表"与"覆盖态下 TabBar 激活=override 对应标签"自相矛盾——标签不在列表里就无标签可激活。定稿：**进入覆盖态时同样 `openTab(override 菜单码)`**，该标签携带可选字段 `overridePath`（当前隐藏路由完整 URL 含 query，如 `/competition/theory-exams?eventId=…&stageId=…`）；覆盖期间内容仍由 `contentOverride` 渲染（机制不变），仅标签栏与高亮按该标签显示。
- **优先级链（不变）**：
  ```
  effectiveActiveKey = contentOverride ? selectedMenuKeyOverride : tabStore.activeKey
  ```
  菜单高亮、面包屑、TabBar 激活态统一取 `effectiveActiveKey`。**每个 shell 必须传入一个真实的菜单码作为 `selectedMenuKeyOverride`**（现状 5 处均已传），避免"点了 A 标签、菜单瞬时跳 B"的错位。
- **退出覆盖**：从覆盖态点击其他菜单/标签 = `onMenuNavigate` 触发 `setXxxActive(false)`（现有流转不变）+ 同步 `tabStore.activateTab(target)`，保证退出后 tabStore.activeKey 与界面一致。
- **覆盖态回退（v1.2 一期化，替代 v1.1 的"已知限制不修"）**：点击携带 `overridePath` 的标签 → `pushState(overridePath)` **并手动 dispatch popstate**（唯一允许派发 popstate 的路径——需要 `App()` 重新按 pathname 选 shell）→ 各 shell 现有的"pathname 变化 → setXxxActive(true)" effect 自动回到覆盖态，URL 级状态（eventId/stageId 等 query 参数）完整恢复；URL 之外的页内状态（未保存表单、内部分页）不承诺恢复（与浏览器书签行为一致）。激活/退出覆盖时同步把 `overridePath` 刷新为当前 URL。
- 竞赛内部跳转（pushState + popstate、未保存拦截）完全不动。
- 好处：改造范围收敛在 AppShell + 导航层，高风险的竞赛工作台零触碰。

### 3.8 标签关闭守卫（**按 Q6 重写**）

- **前置依赖**：必须先把 `src/hooks/useUnsavedChangesBlocker.tsx` 的单槽 `guardRef` 改为 `Map<scope, Guard>` 注册表，`registerGuard(scope, guard)` / `confirmNavigation(scope)` 按 scope 取用；`useNavigationBlocker()` 返回的 API 保持兼容，新增 scope 参数。**这是 Q1-A 全保活的硬前置**：多页共存时单槽会被后挂载的页面覆盖写，且 cleanup 的 `guardRef.current === guard` 判定让被挤掉的一方连正确注销的机会都没有。
- 注册 scope 取**当前 tab 的 key**——来源与 `useIsTabActive()` 同根的 `effectiveActiveKey`（v1.2 D3：覆盖态页面依赖 D1"覆盖态标签进列表"才有稳定 scope），从而"关哪个标签问哪个页面"。
- 一期体态：仅对**已注册 guard 的页面**（现为竞赛工作台）在关闭其标签时用 `confirmNavigation(tabKey)` 弹确认；其余页面直接关闭（对齐浏览器习惯）。
- 顺带修掉 line 52 硬编码的"当前页面有未保存的赛段编排"文案，改为入参化的通用文案。
- 其余页面的"未保存离开确认"列为二期增强（需建立页面级 dirty 注册机制，届时天然具备 scope）。

### 3.9 改动文件清单（v1.1 更正）

| 文件 | 改动 |
|---|---|
| `src/stores/tabStore.ts`（新增） | 标签数据层（§3.2 全部职责）+ persist（tenantId 并入 key）+ 动态别名归一 + pushState 排除逻辑 |
| `src/layouts/AppShell.tsx` | 内容区改 Tabs 渲染；滚动保存/恢复时序；URL 联动；effectiveActiveKey 优先级链；下钻 effect 加"用户点击"守卫；删除原回顶 effect；外部门菜单（WeKnora）逻辑不动 |
| `src/stores/navStore.ts` | `setSelectedKey` 委托 tabStore（签名不变） |
| `src/main.tsx` | 挂 `TabKeepAliveProvider`；5 个隐藏路由 shell 传入 tabStore.activeKey 联动（小改）；pageMap 不动 |
| `src/layouts/TabKeepAlive.tsx`（新增） | Provider + `useIsTabActive` hook |
| `src/hooks/useUnsavedChangesBlocker.tsx` | 单 ref → `Map<scope, Guard>` 注册表 + scope 化 API + 通用文案（**§3.8 硬前置**） |
| `src/hooks/useTabDirtyGuard.ts`（新增，v1.2） | 页面级 dirty 关闭确认框架（消费 Q6 注册表）；一期试点 ≤5 个高风险表单页（候选：组卷计划、赛事创建、赛段编排，名单实施时与 owner 确认） |
| `src/layouts/AppShell.tsx`（T8 追加，v1.2） | 标签刷新按钮（重挂载单页：pane key bump；**不清 react-query 全局缓存**，见 §8 风险）+ 打开数 >10 非阻塞软提示 |
| `src/main.tsx`（T8 追加，v1.2） | 覆盖态标签 `openTab` + `overridePath` 维护；覆盖态回退的 pushState+popstate 派发 |
| `package.json`（T8 追加，v1.2） | 新增依赖 `@dnd-kit/core` + `@dnd-kit/sortable`（拖拽排序，最低优先、整项可裁剪、独立 commit） |
| `workbench/GenWorkbenchPage.tsx`（**替代 v1.0 误列的 TheoryQuestionGenPage**）、`SkillQuestionGenPage.tsx`、`pages/question_bank/KnowledgeFixPage.tsx` | 轮询接 `useIsTabActive`。SkillQuestionGenPage 另需收 300ms 导入定时器 |
| 其余甲组轮询点（附录 B，12 处） | `refetchInterval: isActive ? N : false` |
| `pages/zhi_neng_ke_cheng_cai_liao_guan_li/XiMuBiaoGuanLiPage.tsx` | 乙组唯一随一期改造项：`refetch()` 轮询接 `useIsTabActive` |
| `styles/global.css` | 标签条样式重写为 Chrome 风格（v1.4：`.app-tabbar-strip` 灰底、激活白卡片浮起、未激活 hover 提亮、× 按 Q4-A 显隐）；`.app-header-tabs`/`.app-header-plain` 头部行；移除 `.top-search`。`.app-content` 取值不变（§3.1 布局口径） |
| `package.json` | 新增 `test:admin-tabs` script（`vitest run tests/adminTabs*.test.ts(x)`），与主构建解耦 |
| `apps/admin-web/AGENTS.md` | 新增约定：① 新页面轮询必须接 `useIsTabActive`，且**禁止裸写 `setInterval` 轮询**；② 页面内跳转继续用 `setSelectedKey`（语义已变为开标签）；③ 隐藏路由 shell 必须传 `selectedMenuKeyOverride`；④ 新增隐藏路由须同步登记附录 A |

---

## 4. 实施步骤（v1.1 新增）

按可独立验证、可独立 revert 拆分，**每步结束都可单独跑通，不允许跨步的半成品态入库**。

| 步骤 | 内容 | 独立验证产出 |
|---|---|---|
| **T1** | `tabStore` + 动态别名归一 + persist（含租户隔离）+ 单测；**不接任何 UI** | 新增单测全绿；`npm run build` 通过；界面零行为变化 |
| **T2** | `AppShell` 改 antd Tabs 渲染 + TabBar + 保活 + 固定 tab + 右键/中键 + document.title + global.css + 有效 ActiveKey 优先级链 | 手工：竞赛全流程、5 条隐藏路由直达、26 个跳转文件抽样回归 |
| **T3** | URL 联动（pushState 排除 + popstate 排除 + `findMenuCodeByPath` 泛化）+ 刷新恢复 + 恢复校验 + 下钻 effect 守卫 | 沿用 playwright 的四条主流程 e2e 全绿 |
| **T4** | `useUnsavedChangesBlocker` 注册表改造 + §3.8 关闭守卫 | 竞赛 stage planning 既有测试全绿；多标签 dirty 隔离用例新增并全绿 |
| **T5** | `useIsTabActive` 接入：甲组 15 处 + 3 项乙组 | 隐藏标签静默 60s 请求数门禁（§5）通过 |
| **T6** | navStore→tabStore 语义变更的文案/交互收口（"返回上级"链路复核）、AGENTS.md 约定落地 | 评审走查：批量跳转型页面 6 个文件 100% 覆盖 |
| **T7** | 灰度开关 `VITE_ADMIN_TABS` 上线验证（先 1 个租户），全量放开后保留开关一个迭代 | 上线后 24h 观察窗无事端 → 全量 |
| **T8（v1.2 前挪项）** | 覆盖态回退（`overridePath` 维护 + 派发）+ 标签刷新按钮 + >10 软提示 + 乙组剩余 6 处 setInterval + `useTabDirtyGuard` 框架与高风险页试点 + （可裁剪）拖拽排序（dnd-kit） | 新增 RTL/e2e 用例全绿（§5 增补）；拖拽如裁剪则整体不进 T8，独立 commit 便于剔除 |

---

## 5. 测试与验收（v1.3：清单化，验收时逐项勾选）

> **实施勾选（2026-10-04，T1~T8 代码全量落地后）**
> - **5.1 单测 UT-01~20：✅ 全部实现并执行通过**（`npm run test:admin-tabs` = `tests/adminTabsStore.test.ts` 20 条 + `tests/adminTabsUrl.test.tsx` 12 条，32/32 绿；UT-14/15 在 store 测试以守卫注册表口径覆盖，UT-18 以 AppShell 集成口径覆盖，UT-20 覆盖 reorder+钳制）
> - **5.2 组件级 RTL：✅ 核心项已覆盖**（菜单开标签/保活不重挂载/URL 联动/深链/恢复校验/popstate 反查/隐藏路由排除/下钻守卫/title/软提示/固定标签 closable/灰度 off 旧分支）；RTL-03 滚动像素误差、RTL-05 中键真实指针事件、RTL-09/10 计时器断言 → 转入 §5.5 抽查
> - **5.3 e2e E2E-01~08：⏳ spec 已交付未执行**（`e2e/adminTabs.spec.ts` + `npm run test:e2e:admin-tabs`；执行需 dev server + 登录态，本机无后端连接凭据；E2E-06 拖拽/E2E-08 登出按 §5.6 归人工）
> - **5.4 量化门禁：类型检查 ✅**（`npm run build` 0 error）；**单测 ✅**（新增 32 全绿 + 既有用例零新增回归：全量 198 过，失败集与改造前 stash 基线完全一致）；**其余门限 ⏳**（LCP/切换 P95/隐藏静默 60s/内存/滚动 ≤4px/存储 32KB/权限过滤——需真实浏览器 + 后端环境，度量方式见表）
> - **5.5 手工回归：⏳** 按 §5.6 分工执行
> - **附录 C 数据核对 SQL：⏳** 待库连接执行（本机凭据文件已失失，SQL 就绪）
> - **useTabDirtyGuard 试点名单：⏳ 待 owner 确认**（框架已交付；竞赛工作台两文件经既有 `useUnsavedChangesBlocker` 已自动获得 scope 化守卫 = 首批实际受益页）

### 5.1 单元测试清单（Vitest，`tests/adminTabs*.test.ts(x)`，目标 ≥ 20 条全绿；全仓既有用例零回归）

| # | 用例 | 对应设计 |
|---|---|---|
| UT-01 | openTab 新码追加标签并激活 | §3.2 |
| UT-02 | openTab 已存在码仅激活不重复追加（幂等，含 StrictMode 双调用） | §3.2 / §8 |
| UT-03 | 固定首标签 knowledge 恒在、`closable=false`；closeTab 对之为 no-op | Q4 |
| UT-04 | closeTab 激活位 fallback：左邻 → 右邻 → 固定 tab | §3.2 |
| UT-05 | closeOthers / closeRight / closeAll 均保留固定标签与指定标签 | Q5 |
| UT-06 | 别名归一：menuTree 命中直接用；kebab→snake 归一；`train_xxx-yyy` 真码不被误并；两不命中按原 code 开标签 + console.warn | §3.2 |
| UT-07 | restoreFromStorage：失效码剔除、全部失效回落固定 tab、activeKey 越界回退 | §3.5 |
| UT-08 | scrollPositions 存取；恢复失败**不回写 0**，二次切回可还原 | §3.3 |
| UT-09 | openTab 携带/更新 `overridePath`（覆盖态标签） | §3.7 |
| UT-10 | **>10 软提示**：打开第 11 个标签时触发一次、同会话不重复 | Q10 |
| UT-11 | URL 正向：activateTab → pushState；path 为空 / 附录 A 命中不写 | §3.4 |
| UT-12 | URL 反向：popstate → findMenuCodeByPath（精确/最长前缀）→ 激活或开标签；附录 A 命中不反查 | §3.4 |
| UT-13 | 覆盖态回退：`overridePath` 标签激活 → pushState + popstate（全站唯一派发点） | §3.7 |
| UT-14 | 守卫注册表 `Map<scope, Guard>`：registerGuard / confirmNavigation 按 scope 取用、注销不误删他人 | §3.8 / Q6 |
| UT-15 | useTabDirtyGuard 注册/注销/scope 隔离 | §3.8 |
| UT-16 | clearTabs 后 tabs 复位为固定 tab；401 被动失效路径**不清** tabs | §3.2 |
| UT-17 | persist key 并入 tenantId：甲租户 key 不被乙租户读写 | §3.2 |
| UT-18 | 下钻守卫：非用户点击来源的 activeKey 命中空目录不重定向 | §3.5 |
| UT-19 | navStore.setSelectedKey 委托 tabStore（签名与行为兼容） | §3.2 |
| UT-20 | 拖拽排序 reorder 并持久化（若未裁剪） | Q10 |

### 5.2 组件级清单（React Testing Library）

| # | 用例 |
|---|---|
| RTL-01 | 菜单点击 → 新标签出现并激活、菜单高亮与面包屑同步 |
| RTL-02 | 保活：表单输入/筛选/分页切走再切回完整保留 |
| RTL-03 | 滚动还原：>2 屏页面中部切走切回误差 ≤4px；内容未撑高失败不写 0、二次切回可还原 |
| RTL-04 | 关闭标签 → 组件卸载（该页轮询/定时器停止）；关闭激活标签后激活位正确 |
| RTL-05 | 右键菜单三项（关其他/关右侧/全关）行为正确；中键 onAuxClick 关闭且不触发 Chrome 自动滚动 |
| RTL-06 | 覆盖态 effectiveActiveKey=overrideKey；退出覆盖后 activeKey 与界面一致 |
| RTL-07 | 覆盖态回退：overridePath 点击 → pushState+popstate → 覆盖态恢复、eventId/stageId 正确 |
| RTL-08 | 标签刷新按钮：该页本地状态重置、URL 不变、其他标签的 react-query 缓存不受影响 |
| RTL-09 | dirty 守卫多标签互不串台：试点页关闭弹确认、非试点页直接关 |
| RTL-10 | 乙组 tick：隐藏期间 setState 调用数 = 0；导出类计时隐藏不中断、可见后同步一次 |
| RTL-11 | >10 软提示 UI 出现且非阻塞（不中断当前操作流） |
| RTL-12 | 灰度开关 off 时走旧渲染分支（无 TabBar、无持久化读写） |

### 5.3 e2e 清单（Playwright，沿用现有 playwright.config）

| # | 场景 |
|---|---|
| E2E-01 | 主流程：点 3 个菜单 → 3 标签 → 中键关 1 个 → 右键"关闭其他"→ 剩固定+当前 |
| E2E-02 | 刷新恢复：开标签 → F5 → 标签列表、激活位、滚动位置还原 |
| E2E-03 | 直达：粘贴菜单 path URL → 自动开对应标签 |
| E2E-04 | 竞赛并存：竞赛 URL 直达 → 覆盖态正常 → 点其他菜单出标签 |
| E2E-05 | 覆盖态回退：竞赛工作台（带 eventId）→ 点知识库菜单出标签 → 点回竞赛标签 → URL 级状态恢复 |
| E2E-06 | 拖拽排序 → F5 后顺序保持（若未裁剪） |
| E2E-07 | 浏览器前进/后退在标签间切换；附录 A 路径正确落到对应 shell（不开标签） |
| E2E-08 | 登出后标签清空；重登只剩固定 tab；模拟 401 后标签不丢失 |

### 5.4 量化门禁（必须全部达标方可进入 T7 全量）

| 项 | 门限 | 度量方式 |
|---|---|---|
| 类型检查 | `npm run build`（内含 `tsc -b`）**0 error**，警告数不高于改造前 | CI |
| 单元测试 | 新增 tabs 用例 **≥ 20** 条且全绿（5.1 清单）；全仓既有 vitest 用例零回归 | CI |
| e2e | E2E-01 ~ E2E-08 全绿 | CI |
| 首屏性能 | 登录到首屏 LCP 相对改造前劣化 **≤ 5%** | Performance API，3 次取中位数 |
| 切换耗时 | 10 标签下点击切换到内容可见 **P95 ≤ 150ms** | Performance API，3 轮 |
| 隐藏标签静默 | 隐藏标签静默 **60s**，**全部轮询点（甲组 15 + 乙组 9，v1.2 全量）**新增请求数 **= 0**；时钟/进度类 tick 隐藏期间 setState 调用数 **= 0** | DevTools Network 计数 + RTL-10 |
| 滚动还原 | 内容高度 > 2 屏的页面滚到中部 → 切走 → 切回，`scrollTop` 误差 **≤ 4px** | 手工 + e2e 断言 |
| 滚动还原失败处理 | 内容未撑高时不回写 0，二次切回仍能还原 | RTL-03 |
| 内存 | 开 10 个标签后 `performance.memory.usedJSHeapSize` 较基线增长 **≤ 150MB** | DevTools Memory 快照 |
| 内存泄漏 | 连续切换标签 20 次后，detached DOM 节点增长 **= 0** | DevTools Memory 快照对比 |
| 存储体积 | 单条 `guoyu_admin_tabs` **≤ 32KB**（localStorage 5MB 配额） | DevTools Application |
| 权限过滤 | 用无某菜单权限的账号复用同一浏览器登录，失效标签被剔除且**不白屏**，回落固定 tab | 手工 |
| 软提示阈值 | 打开第 **11** 个标签时触发一次非阻塞提示 | RTL-11 / 手工 |
| 数据核对 | 上线前一次性核对：重复 path 组数、**路径命中附录 A 的菜单数**（对应 Q3） | 附录 C SQL |

### 5.5 手工回归清单

- 竞赛模块全流程（含赛段编排 dirty 关闭确认不串台）
- 4 条隐藏路由直达：AI 同步学统计 / 毕业典礼班级详情 / 采集管理 / 竞赛各入口
- 6 个直 pushState 文件逐个走查（附录 1.1 表第 ② 类名单）
- 26 个经 navStore 跳转文件抽样 ≥ 60%；批量跳转 hub 页面（`ProjectArchivesPage`、`TaskArchivesPage`）100%
- 批量跳转型页面"返回上级"体感走查（T6 收口）
- 别名页面（`zhixun-*` kebab 跳转）不出现同页双标签
- 旧数据迁移：含 `guoyu_admin_selected_menu` 的老浏览器首次进入 → 初始激活正确
- 401 模拟：token 临界过期并发请求 → 标签不丢失
- 登出清空、租户切换清空
- 隐藏标签中的全局弹窗（批次完成 toast 等）无遮蔽问题
- 内容区可用高度减少 40px 后，无固定高度写法的页面出现挤压
- `npm run build` 0 错、警告数不高于改造前

### 5.6 验收流程

- 每步 T1–T8 独立 commit、独立验证产出（§4 表），任一可单独 revert。
- 全量前：T7 灰度单租户 24h 观察窗，指标达标再全量；开关在全量后再保留一个迭代。
- 回滚触发条件（量化）与新旧分支并行策略见 §7。

**角色分工（v1.3，owner 指令）**：手工回归清单（§5.5）归验收流程使用，**执行主力是实施者（AI Agent），owner 只做抽查**——

| 层级 | 承担方 | 范围 | 产出 |
|---|---|---|---|
| 自动化 + 可操作项 | 实施者（AI Agent） | 5.1 / 5.2 / 5.3 全部；§5.5 中可操作项：4 条隐藏路由直达、6 个直 pushState 文件、26 个跳转文件抽样、别名双标签、旧 key 迁移、401 模拟、登出/租户清空、`npm run build`；5.4 量化门禁全项 | 测试报告 + 关键页面截图/录屏证据 |
| 体感抽查 | owner | 仅两类：① 批量跳转型页面"返回上级"体感走查（T6）；② 视觉类——隐藏标签全局弹窗有无遮蔽、内容区少 40px 后有无页面挤压变形 | 实施者提供对应截图/录屏，owner 过一眼，预计 ≤ 15 分钟 |
| 真机协调 | owner | 涉及真实设备的回归（如平板端，若本期触及） | 按需要提供设备 |

---

## 6. 分期

- **一期（本方案全部）**：tabStore + TabBar + 保活 + URL 联动 + 恢复 + 右键/中键 + 固定 tab + 轮询暂停（甲组 15 + 乙组 9 全量，v1.2）+ 守卫注册表化 + 隐藏路由并存 + **覆盖态回退（overridePath）+ 标签刷新按钮 + >10 软提示 + useTabDirtyGuard 框架与高风险页试点 +（低优先可裁剪）拖拽排序（dnd-kit）**（v1.2 前挪）。对应 T1–T8。
- **二期（滚动增强，不阻塞上线）**：全站页面 dirty 确认全量接入（框架已在一期交付，按"表单量大、误关损失高"排序滚动采纳，不设死线）、页面级 scroll anchor 兜底、（若一期裁剪）标签拖拽排序。

---

## 7. 回滚方案（v1.1 新增）

- **灰度开关**：编译期常量 + localStorage 覆盖双通道的 `VITE_ADMIN_TABS`。off 时 `AppShell` 走**旧渲染分支**（`contentOverride || pageMap[selectedKey] || pageMap['knowledge']`，无 TabBar、无持久化读写），新旧两条渲染路径在开关存活期内**并行保留**。这正好用得上 Q8 决定保留的 `guoyu_admin_selected_menu`。
- **提交粒度**：T1–T4 各一个独立 commit，任一可单独 revert 后系统仍可运行（T1 不接 UI；T2 revert 即回到无 TabBar；T3 revert 即回到无 URL 联动；T4 revert 即回到单槽 guard 的既有行为）。
- **服务端零改动**：本改造无后端改动、无数据迁移、无 Flyway 脚本，前端回滚只需重新部署上一版 `dist`（Vite 静态产物），**分钟级**。
- **向前兼容**：新增的 `guoyu_admin_tabs` 对旧版本是不可识别 key，旧版直接忽略；这是为何不清 Rwanda 除该 key 也无害。
- **回滚触发条件（量化）**：满足任一即关开关回滚——① 上线后 24h 内标签相关 P1 缺陷 **≥ 3 起**；② 出现跨标签状态串（如 `currentProjectId` / `editingTaskId` 被另一标签改写）**≥ 1 起**；③ 首屏或切换性能门禁不达标；④ 用户在同一浏览器换账号后出现错位而非回落固定 tab。
- **观察窗**：T7 灰度先在单一租户开 24h，指标达标再全量；开关在全量后再保留一个迭代（约两周）才删除旧分支代码。

---

## 8. 风险与注意点（原 §6）

- **内存**：全保活下极端用户开几十个标签，DOM/组件树常驻。一期不设硬上限，观察真实使用；必要时二期加软提示（>10）。门槛值见 §5 量化表。
- **浏览器返回键语义变化**：URL 联动后"返回"= 切回上一个标签（pushState 栈），与现状不同——预期内的体验升级，发版说明提一句。**注意**：现在多了"两类 path 不写 URL"（空 path + 附录 A 命中），这部分标签不进历史栈，需在发版说明里一并交代，避免用户困惑。
- **隐藏标签中的弹窗**：antd Modal/message 是全局 portal，隐藏标签页里的异步提示（如批次完成 toast）仍会全局弹出——与浏览器多标签行为不同，属可接受差异，验收时确认无遮蔽问题。
- **StrictMode 双渲染**：`openTab` 必须幂等（同码不重复追加），持久化写操作容忍重复；注意 dev 下 `pushState` 会被调两次，e2e 断言不要依赖历史栈长度。
- **滚动口径**：TabBar 不参与滚动（`flex: 0 0 40px`），Content 滚动模型延续 b127a42 口径；页面内 sticky/100vh 取值不受影响（容器未变），但内容区可用高度**减少 40px**，需在 §5 走查里确认无固定高度写法的页面出现挤压。
- **菜单 path 数据质量**：若部分存量菜单 path 为空/重复，空者按 §3.4 降级（不写 URL）；重复 path 的两个码在 popstate 反查时取先匹配者。**上线前必须按附录 C 拿到确切分布**，把"path 唯一性"从定性要求变成数字门禁。
- **跳转语义变更**：navStore 委托使 70+ 处调用从"替换内容"变为"开标签"，批量跳转型页面的返回链体感会变；这是设计取舍，已写入 §3.2 与 §5 手工回归清单。
- **刷新按钮与缓存语义（v1.2）**：标签"刷新"= 重挂载组件（pane key bump），**不清 react-query 全局缓存**——缓存由 staleTime 治理，一刀切全清会误伤其他标签共享的查询；若试点页面确需"强制最新"，只对**该页自己的 queryKey** 调 `invalidateQueries`，实施时按页登记。
- **拖拽排序引入新依赖（v1.2）**：`@dnd-kit/core` + `@dnd-kit/sortable` 为运行时依赖（无 DOM 侵入、与 antd 无冲突，antd 拖拽排序场景的事实标准）；排序结果随 tabStore persist。工期紧时整项裁回二期（独立 commit，剔除零残留）。
- **全站 dirty 确认不全量（v1.2 明确）**：Q6 注册表 + `useTabDirtyGuard(tabKey, isDirty, message?)` 一期只交付框架与 ≤5 个高风险页试点；全量接入靠 AGENTS.md 新页面约定 + 存量滚动采纳，不设死线——避免一期范围失控。

---

## 附录 A：隐藏路由清单（URL 联动双向排除）

`App()`（main.tsx:706-751）按 pathname 在这些 shell 与 AppShell 之间二选一，因此**任一标签路径命中下表，均不得写入 URL、popstate 亦不反查**：

| 匹配器 | 来源 | 对应 shell |
|---|---|---|
| `/^\/competition(?:\/\|$)/` | `routes/competitionRoute.ts:9` `isCompetitionPath` | `CompetitionEventShell` |
| `/^\/training\/exam-lists\/\d+\/review-sync-stats/` | main.tsx:645 | `ReviewSyncStatsShell` |
| `/^\/training\/graduation\/classes\/\d+\/?$/` | main.tsx:677 | `GraduationClassDetailShell` |
| `/^\/certification\/practical-collection\/([^/]+)\/manage\/?$/` | main.tsx:713 | `CollectionManageShell` |
| `/^\/formal-exam(?:\/\|$)/`、`/^\/assessor-scoring/`、`/^\/assessor-defense/`、`/^\/practical-collection/`、`/^\/competition-practical-judge/` | main.tsx:708-717 | 非 AppShell 端，天然排除 |

**注**：`AppShell.tsx:25` 的 `hiddenMenuCodes = {'competition_theory_exam'}` 说明竞赛确有菜单 path 落在上表第一类，D3 是真实可达缺陷，不是理论风险。

## 附录 B：轮询点全量清单（2026-10-04 grep 复核）

**甲组 · `refetchInterval`（一期全改，15 处）**

| 位置 | 周期 | 备注 |
|---|---|---|
| `zhi_neng_ke_cheng_cai_liao_guan_li/workbench/GenWorkbenchPage.tsx:50` | 3s | **理论生成工作台真正的轮询点**（v1.0 误列宿主页） |
| `zhi_neng_ke_cheng_cai_liao_guan_li/SkillQuestionGenPage.tsx:385` | 3s | |
| `question_bank/KnowledgeFixPage.tsx:134` | 3s | |
| `zhi_neng_ke_cheng_cai_liao_guan_li/AiCoursewarePage.tsx:333 / 648` | 3s / 条件 | AI 课件生成 |
| `certification/DefenseManagementPage.tsx:82` | 5s | **无条件常驻**，隐藏后仍打 |
| `training/TrainingDashboardPage.tsx:85`（`liveQueryOptions` 复用多处） | 10s | 另 `refetchOnWindowFocus: true` |
| `training/ReviewSyncPanel.tsx:64` | `POLL_INTERVAL_MS` | |
| `training/SolutionPlanningPage.tsx:217 / 237` | 条件 | |
| `training/follow/FollowMockExamTab.tsx:135 / 153 / 166 / 186` | `pollMs` | 4 处 |
| `competition/CompetitionQuestionBankPage.tsx:239` | 1.5s | 丙组边缘：竞赛菜单可达，一期一并接入成本低 |
| `certification/info/AssessorLedgerImportButton.tsx:17` | 1.5s | |
| `certification/preparation/PracticalScoringConfig.tsx:109` | 5s | 条件 |

**乙组 · `setInterval`（query options 管不到，9 处）**

| 位置 | 周期 | 一期处置 |
|---|---|---|
| `zhi_neng_ke_cheng_cai_liao_guan_li/XiMuBiaoGuanLiPage.tsx:155` | 5s `setInterval(() => refetch(), 5000)` | **随一期改造**（最典型） |
| `zhi_neng_ke_cheng_cai_liao_guan_li/TheoryQuestionGenPage.tsx:90` | 300ms 导入进度 | **随一期改造**（隐藏时纯浪费） |
| `zhi_neng_ke_cheng_cai_liao_guan_li/SkillQuestionGenPage.tsx:128` | 300ms 导入进度 | **随一期改造** |
| `chazheng/CertQueryPage.tsx:102 / 232` | 2s 查证轮询 | **随一期改造（v1.2）** |
| `exam/FaqConversionPage.tsx:106` | FAQ 转换轮询 | **随一期改造（v1.2）** |
| `training/TrainingDashboardPage.tsx:96` | 1s 时钟 tick | **随一期改造（v1.2）**——渲染类 tick，隐藏时跳过 setState 省 CPU |
| `training/TrainingInformationPage.tsx:590 / 634` | 导出计时 / 轮询 | **随一期改造（v1.2）**——短定时器不中断任务：隐藏时继续计时、跳过 setState，可见后同步 |
| `training/ClassSignUpPeoplePanel.tsx:320` | 导出 | **随一期改造（v1.2）**——同上口径 |
| `zhi_neng_ke_cheng_cai_liao_guan_li/XiMuBiaoGuanLiPage.tsx:189` | 300ms 上传假进度 | **随一期改造（v1.2）**——同上口径 |

**丙组 · 不在标签化范围**（competition 其余、assessor / formalExam / practicalCollection 端）：`display:none` 场景不可达，一期不动。

## 附录 C：菜单 path 数据核对口径（上线前执行一次）

```sql
-- 只读，租户 10159
SELECT COUNT(*)                                    AS total,
       SUM(path IS NOT NULL AND path <> '')        AS path_nonempty,
       SUM(path IS NULL OR path = '')              AS path_empty
FROM   sys_menu WHERE tenant_id = 10159 AND deleted = 0;

-- 重复 path 分组（取前若干组即可）
SELECT path, COUNT(*) c, GROUP_CONCAT(code) codes
FROM   sys_menu WHERE tenant_id = 10159 AND deleted = 0 AND path <> ''
GROUP BY path HAVING c > 1;

-- 命中附录 A 隐藏路由前缀的菜单数（决定 Q2/Q3 承诺的覆盖缺口）
SELECT COUNT(*) FROM sys_menu
WHERE tenant_id = 10159 AND deleted = 0 AND (
      path LIKE '/competition/%' OR path LIKE '/training/exam-lists/%/review-sync-stats%'
   OR path LIKE '/training/graduation/classes/%' OR path LIKE '/certification/practical-collection/%/manage%');
```

门禁建议：`path_empty` 与"重复 path 组数"如实进方案（不接受 ≥ 0 的模糊表述）；第三项数字 > 0 时，必须在发版说明中明确"这批菜单不支持深链直达开标签"。
