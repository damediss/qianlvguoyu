# 竞赛选手 H5 与现有培训移动端的边界分析

## 核查结果

仓库根目录和 `apps/` 下未找到用户指定的 `gjjd_appnext` 目录，因此无法对该工程的源码、路由、依赖或页面进行逐文件复核。本结论以当前仓库中可见的培训 App 后端兼容域 `com.guoyu.platform.app` 为参考，并等待补充 `gjjd_appnext` 的实际路径或独立仓库地址后更新。

## 为什么不能直接复用

1. 用户已明确竞赛是独立赛事应用，不能以培训 App 的 `pages/competition` 为基础；培训的学习、考试和学员生命周期不等于报名、赛段、晋级和赛事公开生命周期。
2. 现有历史移动端兼容接口使用 `/appauth`、`LegacyAppResponse`、`platform_user` / `platform_user_token`，代码中可见 MD5 密码校验和 SHA-1 令牌生成，且相关表被多租户拦截器忽略；它不能作为新竞赛 H5 的认证、授权或租户隔离实现。
3. 竞赛需以“报名审核 + 赛事/赛段授权 + 本人任务范围”为权限边界；仅凭培训用户 token 或前端页面隐藏无法满足要求。
4. 竞赛原型还定义企业、专家和公开端，这些均不属于选手 H5，直接复用培训页面将模糊角色与数据边界。

## 可以参考的能力

| 可参考 | 使用方式 |
| --- | --- |
| 手机号登录、短信验证码的交互经验 | 企业导入名单身份证匹配成功后，重新按竞赛 player 认证域实现；使用现代令牌策略、频控、配额和成本监控 |
| 移动端接口封装、网络异常、文件上传体验 | 在 `competition-h5` 新建独立 API client 与本地存储 key |
| 学习/考试的产品交互经验 | 仅作为后续赛前学习、线上考试专项的输入，不能复用业务页面和数据表 |
| Android WebView 壳 | 后续可将独立 H5 通过 URL 接入；不把 WebView 壳变成业务逻辑承载层 |

## 必须重新建设

- `apps/competition-h5` 独立工程、独立路由、移动样式、发布物和会话存储。
- `competition_player`、报名资格、赛事/赛段、任务、成果、通知、成绩发布投影等竞赛领域模型。
- 企业导入名单、身份证匹配与脱敏/加密保存、手机号验证码 Provider Adapter、成本/频控控制，以及 player 认证主体、令牌续期/注销、赛事范围校验、隐私授权、审计和多租户控制。
- 后续线上考试的服务端会话、计时、作答、自动交卷和反作弊审计；不得使用客户端倒计时替代。

详见 [competition-architecture-analysis.md](competition-architecture-analysis.md) 与 [competition-prototype-analysis.md](competition-prototype-analysis.md)。
