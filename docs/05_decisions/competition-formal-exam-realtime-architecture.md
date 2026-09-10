# 竞赛理论正式考试：实时与数据面架构（已落地）

状态：Phase 0–5 已在工作区实现；Phase 6 不把 3000 台容量当作已达标。  
范围：新 formal-exam 链路。不迁移旧 `CompetitionTheoryAttempt` / theory monitor。

## 1. 边界

| 面 | 职责 | 非职责 |
| --- | --- | --- |
| HTTP + MySQL | 登录、试卷、答案批量同步、提交、强交、清退、场次命令 | 不把答案/交卷改成只走 WS |
| WebSocket | 状态、控制、ACK、控制台增量 | 不推 3000 人全量数组 |
| Redis | 一次性 ticket、presence TTL、Pub/Sub 路由 | 不是成绩/答案/清退的事实源 |
| Android 薄壳 | 打开 H5、URL allowlist、生命周期 | 不开第二路 socket、无 JSBridge |

H5 是唯一 WebSocket owner。Force-submit / force-offline **先 HTTP**，WS 只回传结果。

## 2. 状态真相

- 业务终态：MySQL `competition_formal_candidate_exam` + 答案表，行锁/CAS/幂等。
- AccessSession：绑定 examSession、candidateExam、player、device、generation。
- Presence：Redis `formal-exam:presence:{accessSessionId}`，心跳约 20s，不写 MySQL。
- 连接展示：`FormalExamConnectionStates.resolve(candidateStatus, accessSessionStatus, lastSeenAt, now)`，不是 `lastActiveTime`。
- 服务端时间与 `deadline_at` 权威；客户端倒计时只改善体验。

## 3. 消息与失败恢复

1. HTTP 签发 ticket（player 前缀 `formal-exam:ws-ticket:`，console 前缀 `formal-exam:console-ticket:`），TTL 默认 45s，Redis `GETDEL` 一次性。
2. 浏览器连 `/api/ws/competition/formal-exam/player?ticket=` 或 `/console?ticket=`，**URL 不带 Bearer**。
3. 业务写与 outbox 同行事务；提交后 Redis channel `formal-exam:realtime`；每节点只投递本机 socket。
4. Redis 不可用：ticket/WS 返回 503「实时通道暂时不可用」；HTTP 考试仍走 MySQL。
5. 客户端：WS 通期间 status 轮询 60–120s + jitter；断开回 ~4s jitter；重连全抖动退避；`eventId` 去重；`seq` 缺口则 HTTP 快照。Presence envelope 的 `seq` 为 null，避免误 invalidate。

## 4. 明确不做

- 不为容量引入 Kafka、独立 exam 微服务、第二 DataSource、CQRS。
- 不复制实操模块 JVM 本地 ticket 集合当多实例方案。
- 不声称支持 3000 台，除非有与生产拓扑一致的压测报告。
