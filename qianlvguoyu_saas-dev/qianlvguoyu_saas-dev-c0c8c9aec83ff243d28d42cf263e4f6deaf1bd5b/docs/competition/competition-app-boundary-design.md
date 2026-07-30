# 竞赛选手 App 边界设计

## 目标

竞赛选手端在现有 uni-app 工程中以 `pages/competition-app`、`api/competition-player.js`、`store/competitionPlayer.js` 和 `utils/competition-*.js` 建立独立边界。它不是旧 `pages/competition` 的重命名或扩展。

## 边界

| 范围 | 培训 App | 竞赛选手 App |
| --- | --- | --- |
| 身份 | 培训用户及 `itoken` | player 身份及 `competition_player_token` |
| 页面 | `pages/**`、旧 `pages/competition/**` | `pages/competition-app/**` |
| API | 旧 `/appauth/*`、`/appcompetition/*` | `/api/competition/player/**` |
| Store | `useUserStore`、`useCompetitionStore` | `useCompetitionPlayerStore` |
| 未登录跳转 | `/pages/auth/login` | `/pages/competition-app/login` |

旧竞赛页面保留原状，因其仍依赖培训用户、`itoken`、`currentMatchId`、`api/competition.js` 和通用请求层。新页面禁止导入这些模块。

## 目录

```text
gjjd_appnext/
  pages/competition-app/
  api/competition-player.js
  store/competitionPlayer.js
  utils/competition-auth.js
  utils/competition-route-guard.js
```

这是适合当前 uni-app 扁平目录的隔离方案；不强行引入与现有工程不一致的 `src/modules`。
