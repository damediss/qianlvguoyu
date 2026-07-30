# 竞赛选手 App MVP 路由设计

| 业务路由 | uni-app 页面 | 认证 |
| --- | --- | --- |
| `/competition/login` | `pages/competition-app/login` | 否 |
| `/competition/home` | `pages/competition-app/home` | player token + player 身份 |
| `/competition/events` | `pages/competition-app/events` | 同上 |
| `/competition/event-detail` | `pages/competition-app/event-detail` | 同上 |
| `/competition/register` | `pages/competition-app/register` | 同上 |
| `/competition/tasks` | `pages/competition-app/tasks` | 同上 |
| `/competition/profile` | `pages/competition-app/profile` | 同上 |

流程：登录 -> 选手首页 -> 赛事列表 -> 赛事详情 -> 报名 -> 任务；通知由首页入口进入。每个受保护页面调用 `guardCompetitionPlayerRoute`，验证独立 token、player identity，并在已选择当前赛事时验证 `eligibleEventIds`。失败统一重启到选手登录页。

本轮仅创建页面骨架，不加载不存在的后端接口、不提供 mock，也不实现在线考试、交卷、排名、晋级、PK 或 AI。
